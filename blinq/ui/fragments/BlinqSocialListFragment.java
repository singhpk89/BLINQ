package com.blinq.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.blinq.ImageLoaderManager;
import com.blinq.MeCardHolder;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.facebook.Actions.PostsAction;
import com.blinq.authentication.impl.provider.SocialWindowProvider;
import com.blinq.mappers.SocialProfileMapper;
import com.blinq.models.Contact;
import com.blinq.models.FeedModel;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.MeCard;
import com.blinq.models.social.window.SocialWindowCard;
import com.blinq.models.social.window.SocialWindowCard.CardType;
import com.blinq.models.social.window.SocialWindowItem;
import com.blinq.models.social.window.SocialWindowLoadingItem;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.models.social.window.StatusContent;
import com.blinq.service.FloatingDotService;
import com.blinq.sorters.SocialWindowSorter;
import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.ui.activities.search.BlinqSearchHandler;
import com.blinq.ui.adapters.SocialWindowAdapter;
import com.blinq.ui.views.UndoActionView;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.ExternalAppsUtils;
import com.blinq.utils.HeadboxPhoneUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Johan Hansson on 9/16/2014.
 * <p/>
 * A fragment representing a list of social posts.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class BlinqSocialListFragment extends ListFragment implements SocialWindowProvider.SocialWindowPostsListener,
        View.OnClickListener, SocialWindowAdapter.OnCardItemButtonClicked, AuthUtils.AuthAction, PostsAction.OnHighResolutionImageLoaded, View.OnTouchListener {

    public static final int DEFAULT_ROW_TO_SHOW_CARDS_IN = 10;
    private static final int NUMBER_OF_TIMES_ME_CARD_TUTORIAL_SHOULD_BE_SHOWN = 7;
    private final String TAG = BlinqSocialListFragment.class.getSimpleName();

    /*
    * If vertical scroll on visible social window item above the threshold
    * move to next/previous item of the social window depends on the scroll
    * direction up/down.
    */
    private final int COVER_PAGE_VERTICAL_SCROLL_THRESHOLD = 100;

    private Context context;

    private List<SocialWindowItem> posts;

    private AnalyticsSender analyticsSender;

    private HashMap<Platform, List<MemberContact>> memberContacts;

    private OnFragmentInteractionListener onFragmentInteractionListener;
    private OnSocialWindowAffected onSocialWindowAffected;

    private PopupSocialWindow activity;

    private ImageView pullArrowImageView;

    private FeedModel friendFeed;

    public static boolean socialWindowItemClicked;

    public boolean userStartMerge;

    private MeCardHolder meCardHolder;

    /**
     * Those will be created a special merge cards if needed.
     * Order is important
     */
    private List<Platform> PLATFORMS_TO_SUGGEST_MERGE = Arrays.asList(
            Platform.FACEBOOK, Platform.INSTAGRAM, Platform.TWITTER);

    private PreferencesManager preferencesManager;

    private Platform mergePlatform;

    public static BlinqSocialListFragment newInstance(HashMap<Platform, List<MemberContact>> memberContacts, FeedModel friendFeed) {

        BlinqSocialListFragment fragment = new BlinqSocialListFragment();
        fragment.memberContacts = memberContacts;
        fragment.friendFeed = friendFeed;
        fragment.posts = new ArrayList<SocialWindowItem>();

        return fragment;
    }


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BlinqSocialListFragment() {

    }

    public BlinqSocialListFragment(PopupSocialWindow activity) {
        this.activity = activity;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        addLoadingItem();

        setListAdapter(new SocialWindowAdapter(activity, posts, this));
        onFragmentInteractionListener.enlargeSocialWindowWithoutAnimation();
    }


    /**
     * Add loading item to social window list.
     */
    private void addLoadingItem() {

        if (posts != null) {

            SocialWindowLoadingItem loadingItem = new SocialWindowLoadingItem();
            posts.add(loadingItem);
        }
    }

    View root;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.unified_social_window, container, false);

        pullArrowImageView = (ImageView) root.findViewById(R.id.pullDownSocialWindowArrow);
        pullArrowImageView.setOnClickListener(this);
        pullArrowImageView.setOnTouchListener(this);

        return root;
    }


    @Override
    public void onResume() {

        setTouchListenerOnListView();

        onSocialWindowAffected.showWithoutAnimation();

        showUndoMergeView();

        super.onResume();
    }


    /**
     * Show the undo merge view after the merge operation
     */
    private void showUndoMergeView() {
        if (preferencesManager.getProperty(PreferencesManager.IS_SHOW_UNDO_MERGE,
                false) && userStartMerge) {
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    UndoActionView undoActionView = new UndoActionView(
                            root.findViewById(R.id.undobar), activity, 5000
                    );
                    preferencesManager.setProperty(
                            PreferencesManager.IS_SHOW_UNDO_MERGE, false);
                    undoActionView.showUndoBar(false,
                            activity.getString(R.string.merge_undo_message), null);

                }
            });

        }
        userStartMerge = false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (PopupSocialWindow) activity;
        try {
            onFragmentInteractionListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        // Get instance of the listener implemented by given activity.
        try {
            onSocialWindowAffected = (OnSocialWindowAffected) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnSocialWindowAffected.class.getSimpleName());
        }


    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


    /**
     * Initialize fragment components.
     */
    private void init() {

        context = activity.getApplicationContext();
        preferencesManager = new PreferencesManager(context);
        meCardHolder = MeCardHolder.getInstance();
        analyticsSender = new AnalyticsSender(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        getPosts();
    }

    private boolean addMeCard(List<SocialWindowPost> loadedPosts) {
        String lastPersonBio = meCardHolder.getBio();
        String lastPersonTitle = meCardHolder.getTitle();
        String lastPersonOrganization = meCardHolder.getOrganization();
        String lastPersonImagePath = meCardHolder.getImagePath();
        String lastPersonName = meCardHolder.getName();

        if (lastPersonBio == null && lastPersonTitle == null &&
                lastPersonOrganization == null && lastPersonImagePath == null)
            return false;

        MeCard meCard = new MeCard();
        meCard.setName(lastPersonName);
        meCard.setBio(lastPersonBio);
        List<MeCard.SocialProfile> socialProfiles = SocialProfileMapper.getInstance().map();
        meCard.setSocialProfiles(socialProfiles);
        String fullTitle = "";
        if (!StringUtils.isBlank(lastPersonTitle) &&
                !StringUtils.isBlank(lastPersonOrganization)) {
            fullTitle = lastPersonTitle + " @ " + lastPersonOrganization;
        }
        meCard.setTitle(fullTitle);
        meCard.setPictureUrl(lastPersonImagePath);
        meCard.setLastLocation(getLastLocation(loadedPosts));
        meCard.setMutualFriends(meCardHolder.getMutualFriends());
        posts.add(meCard);
        return true;
    }

    private String getLastLocation(List<SocialWindowPost> loadedPosts) {
        Date now = new Date();

        if (loadedPosts.isEmpty()) {
            return "";
        }
        List<SocialWindowPost> loadedPostsByLocation = new ArrayList<SocialWindowPost>(loadedPosts);
        SocialWindowSorter sws = new SocialWindowSorter();
        sws.sortOnlyByRecentLocation(loadedPostsByLocation);

        for (SocialWindowPost post : loadedPostsByLocation) {
            if (post.getLocation() == null || StringUtils.isBlank(post.getLocation().getName())) {
                continue;
            }

            String result = "";
            Date when = post.getPublishTime();
            if (post instanceof FacebookPost &&
                    ((FacebookPost) post).isEvent()) {
                when = ((FacebookPost) post).getStartTime();
            }

            if (when.after(now)) {
                String days = StringUtils.normalizeDifferenceDateFuture(when);
                result = "Going in " + days + " to ";
            } else {
                String days = StringUtils.normalizeDifferenceDate(when);
                result = "Was " + days + " ago @ ";
            }

            result += post.getLocation().getName();
            if (!StringUtils.isBlank(post.getLocation().getCity()) &&
                    !post.getLocation().getCity().equals(post.getLocation().getName())) {
                result += ", " + post.getLocation().getCity();
            }
            return result;
        }
        return null;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Only handle posts items.
        if (posts.get(position).getItemType() == SocialWindowItem.SocialItemType.POST) {

            handleClickOnSocialWindowItem(position);
        }
    }


    /**
     * Set touch listener on unified social window list-view to detect sliding up/down on
     * the visible item.
     */
    private void setTouchListenerOnListView() {

        final ListView listView = getListView();

        listView.setOnTouchListener(new View.OnTouchListener() {

            float previousY;
            float diffY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        previousY = event.getY();
                        break;

                    case MotionEvent.ACTION_UP: {

                        diffY = event.getY() - previousY;

                        // If diff larger than given threshold apply scroll behavior.
                        if (Math.abs(diffY) > COVER_PAGE_VERTICAL_SCROLL_THRESHOLD
                                && activity.getSocialWindowStatus() != PopupSocialWindow.SocialWindowStatus.FILLING_SCREEN) {

                            onFragmentInteractionListener.enlargeSocialWindowFullScreen();
                        }

                        break;
                    }
                }

                return v.onTouchEvent(event);
            }

        });
    }


    /**
     * Handle social window click action.
     *
     * @param position position of the selected item.
     */
    private void handleClickOnSocialWindowItem(int position) {

        socialWindowItemClicked = true;

        SocialWindowPost selectedPost = (SocialWindowPost) getListAdapter().getItem(position);

        Platform platform = selectedPost.getCoverPageStatusPlatform();
        String socialWindowId = selectedPost.getId();

        /*
         * Check if the application for given platform installed on the device to open
         * the post in, otherwise open it web page.
         */
        //If the url video open in external browser and no in app for given platform
        if (selectedPost.getContentType() == StatusContent.VIDEO) {

            openPostInExternalBrowser(selectedPost);

        } else if (ExternalAppsUtils.isAppInstalledFor(platform, context)
                && !StringUtils.isBlank(socialWindowId)) {

            ExternalAppsUtils.launchPlatformPostIntent(selectedPost, context);

        } else {
            openPostInExternalBrowser(selectedPost);
        }

        analyticsSender.sendSocialWindowClickEvent(platform, selectedPost.getContentType());
    }


    /**
     * Open given post in device browser.
     *
     * @param post post to open in browser.
     */
    private void openPostInExternalBrowser(SocialWindowPost post) {
        if (!post.HasLink())
            return;

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(post.getLink()));
        startActivity(browserIntent);
    }


    /**
     * Refresh unified social window list-view
     */
    private void refresh() {

        ((SocialWindowAdapter) getListAdapter()).notifyDataSetChanged();
    }


    /**
     * Reload social window data.
     */
    public void reload() {

        posts.clear();
        refresh();

        getPosts();
    }


    /**
     * Get post data from provider and save them to public list.
     */
    private void getPosts() {
        SocialWindowProvider.getInstance().setListener(this);
    }


    /**
     * Rotate pull arrow to the given degree.
     *
     * @param toDegree degree to rotate view to.
     */
    public void rotatePullArrow(int toDegree) {

        pullArrowImageView.setRotation(toDegree);
    }


    /**
     * Check if user connected to given platform.
     *
     * @param platform platform to check connection with.
     * @return true if user connected to given platform, false otherwise.
     */
    private boolean isConnectedToPlatform(Platform platform) {

        Authenticator authenticator = AuthUtils.getAuthInstance(
                platform, activity);

        return authenticator.isConnected();
    }


    /**
     * Check if friend merged with given platform.
     *
     * @param platform platform to check merge with.
     * @return true if contact merged with given platform, false otherwise.
     */
    private boolean isMergedTo(Platform platform) {

        for (Platform memberPlatform : memberContacts.keySet()) {

            if (memberPlatform == platform) {
                return true;
            }
        }

        return !StringUtils.isBlank(meCardHolder.getSocialProfile(platform));
    }


    @Override
    public void onComplete(List<SocialWindowPost> loadedPosts, boolean isNew, boolean meCardOnly) {

        if (!AppUtils.isActivityActive(activity))
            return;

        // Used to remove loading item.
        posts.clear();

        int meCardExist = addMeCard(loadedPosts) ? 1 : 0;

        int highResImageIndex = meCardExist;
        for (int index = 0; index < loadedPosts.size(); index++, highResImageIndex++) {

            SocialWindowPost post = loadedPosts.get(index);

            if (post instanceof FacebookPost) {
                PostsAction.setHighResolutionImageUrl(((FacebookPost) post).getObjectId(), highResImageIndex, this);
            }

            posts.add(post);
        }

        if (!meCardOnly) {
            addCards();
        }

        updateUIDependsOnFirstSocialItem();

        refresh();

        if (meCardExist != 0 && meCardOnly) {
            getListView().smoothScrollToPositionFromTop(0, 0);
        }
    }

    private void scrollBeyondMeCard(int cardsAddedCount) {
        String contactIdentifier = friendFeed.getContact().getIdentifier();
        if (!preferencesManager.getShowedMeCardOf(contactIdentifier)
                || cardsAddedCount == PLATFORMS_TO_SUGGEST_MERGE.size() || friendFeed.getFeedId() == 1) {
            getListView().smoothScrollToPosition(0);
            preferencesManager.setShowedMeCardOf(contactIdentifier);
        } else
            getListView().setSelection(1);
    }


    /**
     * Change social window height & slide message list down depends on the first social item height.
     */
    private void updateUIDependsOnFirstSocialItem() {

        onFragmentInteractionListener.enlargeSocialWindow(getFirstSocialItemHeight(), false);
    }


    /**
     * Used to change social window height depends on the first loaded item.
     */
    private int getFirstSocialItemHeight() {

        if (posts == null || posts.size() <= 0) {
            return -1;
        }
        SocialWindowItem socialItem = posts.get(0);

        // if opened from notification - don't resize
        if (getActivity().getIntent().getBooleanExtra(Constants.OPEN_SOCIAL_WINDOW, false)) {
            return -1;
        }

        // if full screen - don't resize
        if (activity.getSocialWindowStatus() == PopupSocialWindow.SocialWindowStatus.FILLING_SCREEN) {
            return -1;
        }

        if (socialItem.getItemType() == SocialWindowItem.SocialItemType.POST) {

            SocialWindowPost post = (SocialWindowPost) socialItem;

            if (post.getCoverPageStatusPlatform() == Platform.INSTAGRAM) {

                return (int) context.getResources().getDimension(R.dimen.social_window_instagram_item_height);

            } else {

                // Change the height for social window depends on the photo status.
                if (post.hasPicture()) {
                    return (int) context.getResources().getDimension(R.dimen.social_window_text_with_image_item_height);
                } else {
                    return (int) context.getResources().getDimension(R.dimen.social_window_text_only_item_height);
                }
            }

        } else {

            return (int) context.getResources().getDimension(R.dimen.social_window_text_only_item_height);
        }
    }


    /**
     * Add Connect/Merge cards to social list.
     * return the number of added cards
     */
    private int addCards() {
        int addedCards = 0;
        for (Platform platform : PLATFORMS_TO_SUGGEST_MERGE) {

            SocialWindowCard socialWindowCard = getCard(platform);

            if (socialWindowCard != null) {
                addedCards++;
                posts.add(socialWindowCard);
            }
        }
        return addedCards;
    }


    /**
     * Get card object depends on the given platform and user connection status.
     *
     * @param platform platform to get card for.
     * @return card object.
     */
    private SocialWindowCard getCard(Platform platform) {

        if (!ExternalAppsUtils.isAppInstalledFor(platform, context)) {
            return null;
        }
        if (!isConnectedToPlatform(platform)) {
            return createSocialCard(platform, CardType.CONNECT);
        } else if (!isMergedTo(platform)) {
            return createSocialCard(platform, CardType.MERGE);
        }

        return null;
    }


    /**
     * Create card depends on the type and platform.
     *
     * @param platform card platform.
     * @param type     card type.
     * @return card item.
     */
    private SocialWindowCard createSocialCard(Platform platform, CardType type) {
        SocialWindowCard socialWindowCard = new SocialWindowCard();
        socialWindowCard.setPlatform(platform);
        socialWindowCard.setFriendName(getSocialWindowCardName());
        socialWindowCard.setType(type);
        return socialWindowCard;
    }

    private String getSocialWindowCardName() {
        if (friendFeed == null || friendFeed.getContact() == null || HeadboxPhoneUtils.isPhoneNumber(friendFeed.getContact().getName())) {
            String meCardName = MeCardHolder.getInstance().getName();
            return StringUtils.isBlank(meCardName) ? "" : meCardName;
        } else {
            return friendFeed.getContact().getName();
        }
    }


    @Override
    public void onException(Throwable throwable) {

    }


    @Override
    public void onFail(String reason) {

    }


    @Override
    public void onLoading() {

    }


    @Override
    public void onHighResolutionImageLoaded(String highResImageUrl, int position) {

        if (posts.get(position) instanceof SocialWindowPost) {

            SocialWindowPost post = (SocialWindowPost) posts.get(position);
            post.setPictureUrl(highResImageUrl);
            post.setHasPicture(true);

            // To update post cover image if it's visible.
            try {

                View listItemView = UIUtils.getTheViewOfListItemAtSpecificPosition(position, getListView());

                if (listItemView != null) {

                    ImageView imageView = (ImageView) listItemView.findViewById(R.id.imageViewForSocialWindowCover);
                    ImageLoaderManager imageLoaderManager = new ImageLoaderManager(activity);
                    imageLoaderManager.loadImage(imageView, Uri.parse(post.getPictureUrl()));
                }

            } catch (Exception e) {
                // Fragment view not loaded yet.
            }
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.pullDownSocialWindowArrow:

                if (activity.getSocialWindowStatus() != PopupSocialWindow.SocialWindowStatus.PROCESSING
                        && activity.getSocialWindowStatus() != PopupSocialWindow.SocialWindowStatus.FILLING_SCREEN) {

                    onFragmentInteractionListener.enlargeSocialWindowFullScreen();

                } else if (activity.getSocialWindowStatus() == PopupSocialWindow.SocialWindowStatus.FILLING_SCREEN) {

                    onFragmentInteractionListener.shrinkSocialWindow();
                }

                analyticsSender.sendSocialWindowArrowClicked();

                break;
        }
    }


    @Override
    public void onCardItemButtonClicked(int position) {

        if (posts.get(position) instanceof SocialWindowCard) {

            SocialWindowCard card = (SocialWindowCard) posts.get(position);
            if (card != null) {
                handleClickOnCardButton(card.getPlatform());
            }

        } else {
            Log.i(TAG, "Wrong card index");
        }
    }


    /**
     * Handle click on the card button (Connect/Merge).
     *
     * @param platform card platform.
     */
    private void handleClickOnCardButton(Platform platform) {

        // If platform NOTHING, open add contact intent from contact application.
        if (platform == Platform.NOTHING) {
            return;
            //openAddContactIntent();
        } else {

            if (activity == null)
                return;

            // Get authenticator with given platform to check what action to apply (Merge/Connect),
            // depends on the connection status.
            Authenticator authenticator = AuthUtils.getAuthInstance(
                    platform, activity);

            if (authenticator.isConnected() && !authenticator.isLoginCompleted()) {
                AuthUtils.LoadContacts(platform, this, activity, true,
                        AnalyticsConstants.LOGIN_FROM_SOCIAL_WINDOW);
                return;
            }

            // If not connected to given platform, connect. If connected, open merge screen.
            if (!authenticator.isConnected()) {

                AuthUtils.LoginCallBack loginCallCallback = new AuthUtils().new LoginCallBack(
                        platform, activity, this, true,
                        AnalyticsConstants.LOGIN_FROM_SOCIAL_WINDOW);
                authenticator.login(loginCallCallback);
                closeTheDot();

            } else {

                // Open merge fragment.
                activity.displayMergeView(friendFeed.getFeedId(), platform, BlinqSearchHandler.SearchViewMode.MERGE);
                userStartMerge = true;
                return;
            }

            /*if (activity instanceof InstantMessageActivity) {
                activity.setRedirectToHomeEnabled(false);
            }*/
        }
    }


    /**
     * Open device contact application to add new contact.
     */
    private void openAddContactIntent() {

        try {
            AppUtils.openAddContactIntent(activity,
                    AppUtils.CONTACT_SAVE_INTENT_REQUEST,
                    friendFeed.getContact().getIdentifier());

        } catch (android.content.ActivityNotFoundException e) {

            UIUtils.alertUser(context,
                    context.getString(R.string.unknown_error_));
        }
    }


    /**
     * Replace connect card for given platform with merge card. Used after
     * login done from social window.
     *
     * @param platform platform to replace card for.
     */
    private void replaceConnectCardWithMergeCard(Platform platform) {

        for (int index = 0; index < posts.size(); index++) {

            SocialWindowItem item = posts.get(index);

            if (item instanceof SocialWindowCard) {

                //if (((SocialWindowCard) item).getPlatform() == platform) {

                    posts.set(index, getCard(platform));

                //}
            }
        }
        refresh();
    }

    private void showTheDot() {
        Intent i = new Intent(context, FloatingDotService.class);
        i.setAction(FloatingDotService.CONNECT_NEW_PLATFROM_ENDED);
        context.startService(i);
    }

    @Override
    public void onLoginCompleted(final Platform platform) {

        showTheDot();
        if (platform == null) {     // didn't logged
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!memberContacts.containsKey(platform) && StringUtils.isBlank(meCardHolder.getSocialProfile(platform))) {
                    replaceConnectCardWithMergeCard(platform);
                } else {
                    activity.onMergeDone();
                }
            }
        });
    }


    @Override
    public void onUserProfileLoaded(Platform platform, Contact profile, boolean success) {

    }


    @Override
    public void onContactsUpdated(List<Contact> contacts) {

    }


    @Override
    public void onInboxUpdated(AuthUtils.RequestStatus status) {

    }


    // ------------------------  Getters & Setters --------------------------------------

    public void setMemberContacts(HashMap<Platform, List<MemberContact>> memberContacts) {
        this.memberContacts = memberContacts;
    }

    public void setMergePlatform(Platform mergePlatform) {
        this.mergePlatform = mergePlatform;
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        return onFragmentInteractionListener.onArrowTouched(motionEvent);
    }


    // ---------------------------- Call backs ------------------------------------------

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {


        /**
         * Pull it fully animated
         */
        public void enlargeSocialWindowFullScreen();

        /**
         * Called on scrolling social window list to resize
         * social window view to fill activity content.
         */
        public void enlargeSocialWindow(int newHeight, boolean isRotateArrow);


        /**
         * Called when up arrow pressed to resize social window
         * to it's normal size.
         */
        public void shrinkSocialWindow();


        /**
         * Called to make social window filling screen fast (without animation).
         */
        public void enlargeSocialWindowWithoutAnimation();


        public void changeSocialWindowHeight(int newHeight);


        /**
         * Called when touch listener applied on pull arrow. Used to send touch event to
         * container activity.
         *
         * @param motionEvent touch listener motion event.
         * @return true if touch handled, false otherwise.
         */
        public boolean onArrowTouched(MotionEvent motionEvent);

    }

    /**
     * Work as call back to be sent to the container activity when action (show/hide) should apply on
     * social window.
     */
    public interface OnSocialWindowAffected {

        /**
         * Called when hide social window action should applied.
         */
        public void hideSocialWindow();

        /**
         * Called when show social window action should applied.
         */
        public void showSocialWindow();

        /**
         * Called when animation start to hide the unified social window fast without notice.
         */
        public void hideWithoutAnimation();

        /**
         * Called when activity started to show the unified social window fast without notice
         * after hidden by animation.
         */
        public void showWithoutAnimation();
    }

    /**
     * Interface to communicate with container activity when UI update.
     */
    public interface OnUiUpdate {

        /**
         * Called to refresh some components in the container activity.
         */
        public void refresh();


        /**
         * Called to refresh some components in the container activity in on resume.
         */
        public void refreshOnResume();


        /**
         * Called to display merge fragment in place of messages list fragment.
         */
        public void displayMergeView(int feedId, Platform platform, BlinqSearchHandler.SearchViewMode mode);

        /**
         * Update the action bar based on the attached fragment.
         */
        public void updateActionBar(Fragment fragment);
    }

    private void closeTheDot() {
        Intent i = new Intent(context, FloatingDotService.class);
        i.setAction(FloatingDotService.CONNECT_NEW_PLATFROM_STARTED);
        context.startService(i);
    }



}
