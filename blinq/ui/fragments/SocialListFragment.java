package com.blinq.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.blinq.ImageLoaderManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.facebook.Actions.PostsAction;
import com.blinq.authentication.impl.provider.SocialWindowProvider;
import com.blinq.models.Contact;
import com.blinq.models.FeedModel;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.SocialWindowCard;
import com.blinq.models.social.window.SocialWindowCard.CardType;
import com.blinq.models.social.window.SocialWindowItem;
import com.blinq.models.social.window.SocialWindowLoadingItem;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.models.social.window.StatusContent;
import com.blinq.ui.activities.instantmessage.InstantMessageActivity;
import com.blinq.ui.activities.instantmessage.InstantMessageActivity.SocialWindowStatus;
import com.blinq.ui.activities.search.SearchHandler;
import com.blinq.ui.adapters.SocialWindowAdapter;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.ExternalAppsUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;
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
public class SocialListFragment extends ListFragment implements SocialWindowProvider.SocialWindowPostsListener,
        View.OnClickListener, SocialWindowAdapter.OnCardItemButtonClicked, AuthUtils.AuthAction, PostsAction.OnHighResolutionImageLoaded, View.OnTouchListener {

    public static final int DEFAULT_ROW_TO_SHOW_CARDS_IN = 10;
    private final String TAG = SocialListFragment.class.getSimpleName();

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

    private InstantMessageActivity activity;

    private ImageView pullArrowImageView;

    private FeedModel friendFeed;


    /**
     * Those will be created a special merge cards if needed.
     * Order is importent
     */
    private List<Platform> PLATFORMS_TO_SUGGEST_MERGE = Arrays.asList(
            Platform.FACEBOOK, Platform.INSTAGRAM, Platform.TWITTER);

    public static SocialListFragment newInstance(HashMap<Platform, List<MemberContact>> memberContacts, FeedModel friendFeed) {

        SocialListFragment fragment = new SocialListFragment();
        fragment.memberContacts = memberContacts;
        fragment.friendFeed = friendFeed;
        fragment.posts = new ArrayList<SocialWindowItem>();

        return fragment;
    }


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SocialListFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        addLoadingItem();

        setListAdapter(new SocialWindowAdapter(activity, posts, this));
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.unified_social_window, container, false);

        pullArrowImageView = (ImageView) root.findViewById(R.id.pullDownSocialWindowArrow);
        pullArrowImageView.setOnClickListener(this);
        pullArrowImageView.setOnTouchListener(this);

        return root;
    }


    @Override
    public void onResume() {

        setTouchListenerOnListView();

        if (getActivity().getIntent().getBooleanExtra(Constants.OPEN_SOCIAL_WINDOW, false)) {
            onFragmentInteractionListener.enlargeSocialWindowWithoutAnimation();
        }

        super.onResume();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (InstantMessageActivity) activity;

        try {
            onFragmentInteractionListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
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

        analyticsSender = new AnalyticsSender(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        getPosts();
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
                                && activity.getSocialWindowStatus() != InstantMessageActivity.SocialWindowStatus.FILLING_SCREEN) {

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

        return false;
    }


    @Override
    public void onComplete(List<SocialWindowPost> loadedPosts, boolean isNew, boolean meCardOnly) {

        if (!AppUtils.isActivityActive(activity))
            return;

        // Used to remove loading item.
        posts.clear();

        int rowToShowCardsIn = 0;

        // Check where to add cards in the social list.
        if (loadedPosts.size() == 0) {

            // No posts, just add cards.
            addCards();

        } else if (loadedPosts.size() <= DEFAULT_ROW_TO_SHOW_CARDS_IN) {

            // Add cards after all posts.
            rowToShowCardsIn = loadedPosts.size() - 1;

        } else {

            // Add cards between posts.
            rowToShowCardsIn = DEFAULT_ROW_TO_SHOW_CARDS_IN;
        }

        for (int index = 0; index < loadedPosts.size(); index++) {

            SocialWindowPost post = loadedPosts.get(index);

            if (post instanceof FacebookPost) {
                PostsAction.setHighResolutionImageUrl(((FacebookPost) post).getObjectId(), index, this);
            }

            posts.add(post);

            if (index == rowToShowCardsIn) {
                addCards();
            }
        }

        updateUIDependsOnFirstSocialItem();

        refresh();
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

        SocialWindowItem socialItem = null;

        if (posts.size() == 0) {
            addCards();
        }

        socialItem = posts.get(0);

        // if opened from notification - don't resize
        if (getActivity().getIntent().getBooleanExtra(Constants.OPEN_SOCIAL_WINDOW, false)) {
            return -1;
        }

        // if full screen - don't resize
        if (activity.getSocialWindowStatus() == SocialWindowStatus.FILLING_SCREEN) {
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
     */
    private void addCards() {

        if (posts == null)
            posts = new ArrayList<SocialWindowItem>();

        for (Platform platform : PLATFORMS_TO_SUGGEST_MERGE) {

            SocialWindowCard socialWindowCard = getCard(platform);

            if (socialWindowCard != null) {
                posts.add(getCard(platform));
            }
        }
    }


    /**
     * Get card object depends on the given platform and user connection status.
     *
     * @param platform platform to get card for.
     * @return card object.
     */
    private SocialWindowCard getCard(Platform platform) {

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
        socialWindowCard.setFriendName(friendFeed.getContact().toString());
        socialWindowCard.setType(type);
        return socialWindowCard;
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

                if (activity.getSocialWindowStatus() != InstantMessageActivity.SocialWindowStatus.PROCESSING
                        && activity.getSocialWindowStatus() != InstantMessageActivity.SocialWindowStatus.FILLING_SCREEN) {

                    onFragmentInteractionListener.enlargeSocialWindowFullScreen();

                } else if (activity.getSocialWindowStatus() == InstantMessageActivity.SocialWindowStatus.FILLING_SCREEN) {

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
            handleClickOnCardButton(card.getPlatform());

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

            openAddContactIntent();

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

            } else {

                // Open merge fragment.
                activity.displayMergeView(friendFeed.getFeedId(), platform, SearchHandler.SearchViewMode.MERGE);
                return;
            }

            if (activity instanceof InstantMessageActivity) {
                activity.setRedirectToHomeEnabled(false);
            }
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

                if (((SocialWindowCard) item).getPlatform() == platform) {

                    posts.set(index, getCard(platform));
                    refresh();
                    break;
                }
            }
        }
    }


    @Override
    public void onLoginCompleted(final Platform platform) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                replaceConnectCardWithMergeCard(platform);
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

}
