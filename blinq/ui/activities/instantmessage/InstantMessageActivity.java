package com.xxxx.ui.activities.instantmessage;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.xxxx.R;
import com.xxxx.xxxxApplication;
import com.xxxx.ImageLoaderManager;
import com.xxxx.PreferencesManager;
import com.xxxx.analytics.AnalyticsConstants;
import com.xxxx.analytics.AnalyticsSender;
import com.xxxx.analytics.xxxxAnalytics;
import com.xxxx.models.FeedModel;
import com.xxxx.models.MemberContact;
import com.xxxx.models.Platform;
import com.xxxx.authentication.impl.facebook.FacebookAuthenticator;
import com.xxxx.authentication.impl.Google.GooglePlusAuthenticator;
import com.xxxx.provider.FeedProviderImpl;
import com.xxxx.provider.Provider;
import com.xxxx.ui.activities.search.SearchHandler;
import com.xxxx.ui.adapters.SocialWindowAdapter;
import com.xxxx.ui.animations.ResizeAnimation;
import com.xxxx.ui.animations.SlideAnimation;
import com.xxxx.ui.fragments.InstantMessageFragment;
import com.xxxx.ui.fragments.MergeFragment;
import com.xxxx.ui.fragments.SocialListFragment;
import com.xxxx.utils.AppUtils;
import com.xxxx.utils.Constants;
import com.xxxx.utils.ExternalAppsUtils;
import com.xxxx.utils.Log;
import com.xxxx.utils.StringUtils;
import com.xxxx.utils.UIUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import me.imid.swipebacklayout.lib.SwipeBackLayout;
import me.imid.swipebacklayout.lib.app.SwipeBackActivity;

/**
 * @Created by Johan Hansson.
 */
public class InstantMessageActivity extends SwipeBackActivity implements SwipeBackLayout.SwipeListener,
        SocialListFragment.OnFragmentInteractionListener, InstantMessageFragment.OnSocialWindowAffected,
        View.OnClickListener, InstantMessageFragment.OnTypingMessageChanged, InstantMessageFragment.OnUiUpdate,
        MergeFragment.OnMergeDone {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    public static final String TAG = InstantMessageActivity.class
            .getSimpleName();

    public static final String INSTANT_MESSAGE_FRAGMENT_TAG = "instantMessageFragment";
    public static final String UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG = "unifiedSocialWindowFragment";
    public static final String MERGE_FRAGMENT_TAG = "mergeFragment";


    // Social window slide down/up animation duration in millis.
    public static final int SOCIAL_WINDOW_SLIDE_DOWN_DURATION = 400;
    public static final int SOCIAL_WINDOW_SLIDE_DOWN_FAST_DURATION = 20;
    public static final int SOCIAL_WINDOW_SLIDE_UP_DURATION = 400;
    public static final int SOCIAL_WINDOW_SLIDE_UP_FAST_DURATION = 20;


    // Enum for different status of social window.
    public static enum SocialWindowStatus {
        DEFAULT, PROCESSING, HIDDEN, FILLING_SCREEN
    }

    // -------------------------------------------------------------------------
    // Views
    // -------------------------------------------------------------------------
    private View unifiedSocialWindowView;
    private TextView contactNameTextView;
    private TextView lastMessageSentDateTextView;
    private TextView typingStatusTextView;
    private ImageView contactImageView;

    // -------------------------------------------------------------------------
    // Flags
    // -------------------------------------------------------------------------
    private boolean redirectToHomeEnabled = true;
    private boolean fromNotification = true;
    private boolean sendBackPressEvent = true;

    private FragmentManager fragmentManager;

    private xxxxAnalytics analyticsManager;
    private AnalyticsSender analyticsSender;

    private Provider provider;

    private SocialWindowStatus socialWindowStatus = SocialWindowStatus.DEFAULT;

    private FeedModel friendFeed;

    private HashMap<Platform, List<MemberContact>> memberContacts;

    private int feedId;


    // For enlarge social window by sliding pull arrow.
    float socialWindowOriginalHeight = 0, diff, previousY = 0;

    // Number of diffs to cache, used to smooth touch behavior.
    private final int NUMBER_OF_DIFFS_TO_CACHE = 2;
    private LinkedBlockingDeque<Integer> lastDiffs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        AppUtils.unRegisterActivityGoingIntoBackground(this);
        setContentView(R.layout.activity_instant_message);

        Log.d(TAG, "onCreate");

        init();

        customizeComlogActionBar();

        initHeader();

        handleNotification();

        analyticsSender.sendOpenComlogEvent();

        loadInstantMessageFragment();
        loadUnifiedSocialWindowFragment();

        setSwipeBackEnable(false);

        if (isSocialWindowActive()) {
            unifiedSocialWindowView.setVisibility(View.VISIBLE);
            slideSocialWindowDown(SOCIAL_WINDOW_SLIDE_DOWN_DURATION);
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update social window height when orientation changed.
        if (socialWindowStatus == SocialWindowStatus.FILLING_SCREEN) {

            enlargeSocialWindowWithoutAnimation();
        }

    }


    /**
     * @return true if social window should appear, false otherwise.
     */
    private boolean isSocialWindowActive() {
        return isFriend() && isContactableUser();
    }


    /**
     * Check if the contact is exist on headbox contacts.
     * for example: conversation with unfriend. [contact not exist on phone contacts].
     */
    private boolean isFriend() {

        return isContactableUser() && !StringUtils.isBlank(friendFeed.getContact().getContactId());
    }


    /**
     * Check if the contact is not private or unknown.
     * for example conversation with a private or unknown user.
     */
    private boolean isContactableUser() {

        return (friendFeed == null || friendFeed.getContact() == null) ? false
                : friendFeed.getContact().isContactable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }


    /**
     * Restart the instant message fragment. Used to start normal mode of the
     * instant message fragment after animation mode done.
     */
    public void restartInstantMessageFragment() {

        restartFragment(INSTANT_MESSAGE_FRAGMENT_TAG);
    }


    /**
     * Restart fragment with given tag.
     *
     * @param fragmentTag tag for fragment to be restarted.
     */
    public void restartFragment(String fragmentTag) {

        fragmentManager = getSupportFragmentManager();

        Fragment fragment = fragmentManager
                .findFragmentByTag(fragmentTag);

        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();
        fragmentTransaction.detach(fragment);
        fragmentTransaction.attach(fragment);

        try {

            fragmentTransaction.commit();

        } catch (IllegalStateException e) {

            try {
                fragmentTransaction.commitAllowingStateLoss();
            } catch (IllegalStateException ex) {
                Log.d(TAG,
                        "Fatal Exception: java.lang.IllegalStateException commit already called"
                                + ex.getMessage()
                );
            }
            Log.d(TAG, "Failed to commit fragment.");
        }
    }


    /**
     * handle Headbox notifications to send the analytics
     */
    private void handleNotification() {

        Intent intent = getIntent();

        if (intent == null)
            return;

        fromNotification = intent.getBooleanExtra(Constants.FROM_NOTIFICATION, false);

        if (isFromNotification())
            sendNotificationAnalytics(feedId);

    }


    private boolean isFromNotification() {
        return fromNotification;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        switch (requestCode) {

            case AppUtils.CONTACT_SAVE_INTENT_REQUEST:

                if (resultCode == RESULT_OK) {
                    AppUtils.addContact(getApplicationContext(), intent);
                    xxxxApplication.refresh = true;
                }

                break;
            case ExternalAppsUtils.OPEN_EXTERNAL_APP_REQUEST_CODE:
                onBackPressed();
                break;
            default:
                GooglePlusAuthenticator.getInstance(this).onActivityResult(this,
                        requestCode, resultCode, intent);
                FacebookAuthenticator.getInstance(this).onActivityResult(this,
                        requestCode, resultCode, intent);
                break;
        }

        Log.d(TAG, "onActivityResult");

        super.onActivityResult(requestCode, resultCode, intent);

    }


    /**
     * Send Entered app event and clicked on notification event
     */
    private void sendNotificationAnalytics(int feedId) {

        FeedModel lastFeed = provider.getFeed(feedId);

        if (lastFeed != null && lastFeed.getLastMessagePlatform() != null) {

            Platform notificationPlatform = lastFeed.getLastMessagePlatform();

            analyticsSender.sendEnteringAppEvent();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }


    @Override
    protected void onStop() {
        super.onStop();
    }


    /**
     * Initializes the instant message fragment and hide ActionBar for instant
     * message activity.
     */
    private void init() {

        analyticsManager = new xxxxAnalytics(getApplicationContext());

        analyticsSender = new AnalyticsSender(this);

        provider = FeedProviderImpl.getInstance(this);

        feedId = getIntent().getExtras().getInt(Constants.FEED_ID);
        if (provider != null) {
            friendFeed = provider.getFeed(feedId);
        }

        unifiedSocialWindowView = findViewById(R.id.linearLayoutForUnifiedSocialWindow);


        // Get member contacts.
        memberContacts = provider.getContacts(feedId);

        lastDiffs = new LinkedBlockingDeque<Integer>();
    }


    /**
     * Initializes header view excluding cover page and status view.
     */
    private void initHeader() {

        Typeface friendNameTypeface = UIUtils.getFontTypeface(this,
                UIUtils.Fonts.ROBOTO_CONDENSED);
        contactNameTextView.setTypeface(friendNameTypeface);
        contactNameTextView.setText(friendFeed.getContact().toString());

        Typeface lastMessageSentDateTypeface = UIUtils.getFontTypeface(
                this, UIUtils.Fonts.ROBOTO_LIGHT);
        lastMessageSentDateTextView.setTypeface(lastMessageSentDateTypeface);
        lastMessageSentDateTextView.setText(StringUtils
                .normalizeDifferenceDate(friendFeed.getLastMessageTime()));

        typingStatusTextView.setTypeface(lastMessageSentDateTypeface);

        ImageLoaderManager imageLoaderManager = new ImageLoaderManager(getApplicationContext());
        imageLoaderManager.loadContactAvatarImage(contactImageView, friendFeed.getContact(), false);
    }


    /**
     * Customize instant message action bar.
     */
    private void customizeComlogActionBar() {

        ActionBar actionBar = getActionBar();

        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_background));
        actionBar.setDisplayHomeAsUpEnabled(true);

        //disable application icon from ActionBar
        actionBar.setIcon(R.drawable.conversation_action_bar_custom_icon);

        //disable application name from ActionBar
        actionBar.setDisplayShowTitleEnabled(false);

        // Set custom view for the action bar to display contact icon as circle.
        LayoutInflater mInflater = LayoutInflater.from(this);
        View customView = mInflater.inflate(R.layout.conversation_custom_action_bar, null);

        contactNameTextView = (TextView) customView.findViewById(R.id.conversation_contact_name);
        contactNameTextView.setOnClickListener(this);

        lastMessageSentDateTextView = (TextView) customView.findViewById(R.id.conversation_last_message_sent_date);
        typingStatusTextView = (TextView) customView.findViewById(R.id.conversation_typing_status);

        contactImageView = (ImageView) customView.findViewById(R.id.conversation_contact_image);
        contactImageView.setOnClickListener(this);

        actionBar.setCustomView(customView);
        actionBar.setDisplayShowCustomEnabled(true);

        UIUtils.showActionbar(this);
    }


    /**
     * Slide down social window smoothly.
     */
    private void slideSocialWindowDown(int duration) {

        if (isSocialWindowActive()) {
            // Reset the position for social window after it's hidden by sliding up.
            unifiedSocialWindowView.setY(0);

            SlideAnimation.slideVertically(unifiedSocialWindowView,
                    -unifiedSocialWindowView.getHeight(), unifiedSocialWindowView.getY(), duration);

            unifiedSocialWindowView.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                    socialWindowStatus = SocialWindowStatus.PROCESSING;
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                    if (unifiedSocialWindowView.getHeight() == UIUtils.getScreenHeightWithoutUpperBars(InstantMessageActivity.this)) {

                        // Show social window to filling screen state.
                        socialWindowStatus = SocialWindowStatus.FILLING_SCREEN;

                    } else {

                        // Show social window to normal size state.
                        socialWindowStatus = SocialWindowStatus.DEFAULT;
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    }


    /**
     * Slide up social window smoothly.
     */
    private void slideSocialWindowUp(int duration) {

        SlideAnimation.slideVertically(unifiedSocialWindowView,
                unifiedSocialWindowView.getY(), -unifiedSocialWindowView.getHeight(), duration);

        unifiedSocialWindowView.getAnimation().setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

                socialWindowStatus = SocialWindowStatus.PROCESSING;
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                socialWindowStatus = SocialWindowStatus.HIDDEN;

                // Move social window out of screen after animation done, if not moved it's area will handle
                // touches events.
                unifiedSocialWindowView
                        .setY(-unifiedSocialWindowView.getHeight());
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


    /**
     * Load instant message fragment into Activity.
     */
    private void loadInstantMessageFragment() {

        InstantMessageFragment instantMessageFragment = new InstantMessageFragment(this);

        loadFragment(instantMessageFragment, INSTANT_MESSAGE_FRAGMENT_TAG, R.id.linearLayoutForComlogContents);
    }


    /**
     * Load unified social window fragment into Activity.
     */
    private void loadUnifiedSocialWindowFragment() {

        SocialListFragment socialListFragment = SocialListFragment.newInstance(memberContacts, friendFeed);

        loadFragment(socialListFragment, UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG, R.id.linearLayoutForUnifiedSocialWindow);
    }


    /**
     * Load given fragment to the giving layout.
     *
     * @param fragment    fragment to load.
     * @param tag         fragment tag to be attached with fragment as an id, used to
     *                    get the fragment and communicate with it.
     * @param containerId layout id to load fragment in.
     */
    private void loadFragment(Fragment fragment, String tag, int containerId) {

        fragmentManager = getSupportFragmentManager();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // delete previous instant message fragment if exist, before adding new
        // one
        Fragment previousInstanceOfTheFragment = fragmentManager
                .findFragmentByTag(tag);

        if (previousInstanceOfTheFragment != null) {
            fragmentTransaction.remove(previousInstanceOfTheFragment);
        }

        fragmentTransaction.add(containerId, fragment, tag);
        fragmentTransaction.commit();
    }


    /**
     * Refreshes last seen date view.
     */
    private void refreshLastSeenDate() {

        if (friendFeed == null)
            return;

        Date lastMessageDate = friendFeed.getLastMessageTime();
        lastMessageSentDateTextView.setText(StringUtils
                .normalizeDifferenceDate(lastMessageDate));
    }


    @Override
    public void onBackPressed() {

        if (sendBackPressEvent) {

            analyticsSender.sendComlogBackPressedEvent();
        }

        if (fromNotification) {

            finish();

        } else {

            MergeFragment mergeFragment = (MergeFragment) fragmentManager
                    .findFragmentByTag(MERGE_FRAGMENT_TAG);

            if (mergeFragment != null) {

                displayInstantMessageView(Platform.NOTHING);

            } else {

                scrollToFinishActivity();
            }
        }

    }


    /**
     * Displays instant message view.
     */
    public void displayInstantMessageView(Platform mergePlatform) {

        InstantMessageFragment instantMessageFragment = new InstantMessageFragment(
                this);
        instantMessageFragment.setMergePlatform(mergePlatform);
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.linearLayoutForComlogContents,
                instantMessageFragment,
                INSTANT_MESSAGE_FRAGMENT_TAG);
        transaction.commit();

    }


    /**
     * Display merge view.
     *
     * @param feedId    current feed id.
     * @param platform  platform to merge with.
     * @param mergeType type used in centralize search handler to know how to use the search list view depends
     *                  on the type.
     */
    public void displayMergeFragment(int feedId, Platform platform, SearchHandler.SearchViewMode mergeType) {

        try {

            MergeFragment mergeFragment = new MergeFragment();
            mergeFragment.setPlatform(platform);
            mergeFragment.setMergeType(mergeType);
            mergeFragment.setFeedId(feedId);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.linearLayoutForComlogContents,
                    mergeFragment, InstantMessageActivity.MERGE_FRAGMENT_TAG);
            transaction.commit();

            new PreferencesManager(getApplicationContext())
                    .setProperty(Constants.MERGE_TYPE, mergeType.getId());

        } catch (IllegalStateException illegalStateException) {

            Log.e(TAG, "IllegalStateException while displaying merge view:"
                    + illegalStateException);

        } catch (Exception exception) {

            Log.e(TAG, "Exception while displaying merge view :" + exception);
        }

    }


    /**
     * Invoked when an intent receives.
     */
    @Override
    protected void onNewIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            MergeFragment mergeFragment = (MergeFragment) fragmentManager
                    .findFragmentByTag(MERGE_FRAGMENT_TAG);

            if (mergeFragment != null) {
                mergeFragment.handleIntent(intent);
            }
        }
    }


    @Override
    protected void onPause() {

        if (getRedirectToHomeEnabled()) {
            AppUtils.registerActivityGoingIntoBackground(this);
        }
        super.onPause();
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
        analyticsManager.complete();

    }


    @Override
    public void onScrollStateChange(int i, float v) {

    }


    @Override
    public void onEdgeTouch(int i) {

    }


    @Override
    public void onScrollOverThreshold() {
        analyticsManager.sendEvent(AnalyticsConstants.SWIPE_BACK_TO_FEED_EVENT, false, AnalyticsConstants.ACTION_CATEGORY);
    }


    @Override
    public void hideSocialWindow() {

        slideSocialWindowUp(SOCIAL_WINDOW_SLIDE_UP_DURATION);
    }


    @Override
    public void showSocialWindow() {

        slideSocialWindowDown(SOCIAL_WINDOW_SLIDE_DOWN_DURATION);
    }


    @Override
    public void hideWithoutAnimation() {

        unifiedSocialWindowView.setVisibility(View.GONE);
    }


    @Override
    public void showWithoutAnimation() {

        if (isSocialWindowActive()) {
            unifiedSocialWindowView.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.conversation_contact_name:
                onClickTopBarContactName();
                break;


            case R.id.conversation_contact_image:

                analyticsSender.sendInstantMessageActionBarAvatarClickedEvent();
                onBackPressed();
                break;
        }

    }


    /**
     * Hide / Show social window
     */
    private void onClickTopBarContactName() {

        analyticsSender.sendInstantMessageActionBarUserNameClickedEvent();

        if (friendFeed.getContact().isContactable()) {

            if (socialWindowStatus != SocialWindowStatus.PROCESSING) {

                if (socialWindowStatus == SocialWindowStatus.DEFAULT
                        || socialWindowStatus == SocialWindowStatus.FILLING_SCREEN) {
                    hideSocialWindow();
                } else {
                    showSocialWindow();
                }
            }

            // Check keyboard status (visible/not visible).
            if (((InstantMessageFragment) getFragmentByTag(INSTANT_MESSAGE_FRAGMENT_TAG)).isKeyboardVisible()) {
                AppUtils.hideKeyboard(this);
            }

        }
    }


    @Override
    public void showTypingMessage() {

        typingStatusTextView.setVisibility(View.VISIBLE);
        lastMessageSentDateTextView.setVisibility(View.GONE);
    }


    @Override
    public void hideTypingMessage() {

        typingStatusTextView.setVisibility(View.GONE);
        lastMessageSentDateTextView.setVisibility(View.VISIBLE);
    }


    @Override
    public void refresh() {

        refreshLastSeenDate();
    }


    @Override
    public void refreshOnResume() {
        initHeader();
    }


    @Override
    public void displayMergeView(int feedId, Platform platform, SearchHandler.SearchViewMode mergeType) {

        hideSocialWindow();

        displayMergeFragment(feedId, platform, mergeType);
    }


    @Override
    public void updateActionBar(Fragment fragment) {
        if (fragment instanceof InstantMessageFragment) {
            customizeComlogActionBar();
            initHeader();
        }
    }


    @Override
    public void enlargeSocialWindowFullScreen() {
        enlargeSocialWindow(UIUtils.getScreenHeightWithoutUpperBars(this), true);
    }

    @Override
    public void enlargeSocialWindow(int newHeight, boolean isRotateArrow) {

        if (socialWindowStatus == SocialWindowStatus.DEFAULT && isSocialWindowActive()) {

            // Resizing time for social window in millis.
            final int RESIZE_SOCIAL_WINDOW_DURATION = 500;

            ResizeAnimation resizeAnimation = new ResizeAnimation(unifiedSocialWindowView,
                    unifiedSocialWindowView.getHeight(),
                    newHeight,
                    RESIZE_SOCIAL_WINDOW_DURATION);


            setCoversImagesLoadingStatus(false);

            //actually means new screen. todo - fix the tangle - one place should set all things
            if (isRotateArrow) {
                socialWindowStatus = SocialWindowStatus.FILLING_SCREEN;
            }

            resizeAnimation.setAnimationListener(rotateArrowOnAnimationEnd);

            // Disable message input while social window enlarging
            ((InstantMessageFragment) getFragmentByTag(INSTANT_MESSAGE_FRAGMENT_TAG)).changeMessageInputStatus(false);

            unifiedSocialWindowView.startAnimation(resizeAnimation);
        }
    }


    @Override
    public void shrinkSocialWindow() {

        socialWindowStatus = SocialWindowStatus.DEFAULT;

        // Resizing time for social window in millis.
        final int RESIZE_SOCIAL_WINDOW_DURATION = 500;

        setCoversImagesLoadingStatus(false);

        ResizeAnimation resizeAnimation = new ResizeAnimation(unifiedSocialWindowView,
                unifiedSocialWindowView.getHeight(), socialWindowOriginalHeight,
                RESIZE_SOCIAL_WINDOW_DURATION);

        resizeAnimation.setAnimationListener(rotateArrowOnAnimationEnd);

        unifiedSocialWindowView.startAnimation(resizeAnimation);
    }


    @Override
    public void enlargeSocialWindowWithoutAnimation() {

        socialWindowStatus = SocialWindowStatus.FILLING_SCREEN;
        changeSocialWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        updatePullArrowDirection();
    }


    /**
     * Change social window height without animation.
     *
     * @param newHeight new height for social window.
     */
    @Override
    public void changeSocialWindowHeight(int newHeight) {

        ViewGroup.LayoutParams layoutParams = unifiedSocialWindowView.getLayoutParams();
        layoutParams.height = newHeight;
        unifiedSocialWindowView.setLayoutParams(layoutParams);
    }

    @Override
    public boolean onArrowTouched(MotionEvent motionEvent) {

        // Only handle touch if it's in normal mode (visible & not filling screen).
        if (socialWindowStatus == SocialWindowStatus.DEFAULT) {

            switch (motionEvent.getAction()) {

                // Action down.
                case MotionEvent.ACTION_DOWN:

                    socialWindowOriginalHeight = unifiedSocialWindowView.getHeight();
                    diff = 0;
                    previousY = motionEvent.getY();

                    // Disable swipe back on start touching pull arrow.
                    setSwipeBackEnable(false);

                    setCoversImagesLoadingStatus(false);
                    break;


                // Action move.
                case MotionEvent.ACTION_MOVE:

                    float y = motionEvent.getY();
                    diff = y - previousY;
                    previousY = y;

                    int roundedHeight = Math.round(unifiedSocialWindowView.getHeight() + diff);

                    // Cache the number of diffs depends on defined constant in order to smoothing
                    // touches accuracy (Remove effect of frequent changes between up & down).
                    if (lastDiffs.size() >= NUMBER_OF_DIFFS_TO_CACHE) {
                        lastDiffs.removeFirst();
                    }

                    lastDiffs.add((int) diff);

                    int sum = 0;
                    for (Integer currentDiff : lastDiffs) {
                        sum += currentDiff;
                    }

                    // Change height only if last diffs in the same direction (up/down).
                    if (((sum > 0 && diff > 0) || (sum < 0 && diff < 0))
                            && roundedHeight > socialWindowOriginalHeight
                            && roundedHeight < UIUtils.getScreenHeightWithoutUpperBars(this)) {

                        changeSocialWindowHeight(roundedHeight);
                    }
                    break;


                // Action up.
                case MotionEvent.ACTION_UP:

                    // Enable swipe back after touch done.
                    setSwipeBackEnable(false);

                    // Enlarge social window to fill screen if touch increase it's height.
                    if (unifiedSocialWindowView.getHeight() > socialWindowOriginalHeight) {
                        enlargeSocialWindowFullScreen();
                        analyticsSender.sendSocialWindowArrowSwiped();
                    }

                    break;
            }
        }

        return false;
    }


    @Override
    public boolean onSearchRequested() {

        return false;
    }


    @Override
    public void onMergeDone() {

        SocialListFragment socialListFragment = (SocialListFragment) getFragmentByTag(UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG);

        // Update member contacts in the social window fragment.
        memberContacts = provider.getContacts(feedId);
        socialListFragment.setMemberContacts(memberContacts);

        // Reload social window data.
        socialListFragment.reload();

        slideSocialWindowDown(SOCIAL_WINDOW_SLIDE_DOWN_FAST_DURATION);
    }


    /**
     * Listener to rotate pull arrow after the enlarge/shrink animation of social window finish.
     */
    private Animation.AnimationListener rotateArrowOnAnimationEnd = new Animation.AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {

            updatePullArrowDirection();

            setCoversImagesLoadingStatus(true);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };


    /**
     * Change pull arrow direction (up/down) depends on the social window status (filling screen / not filling).
     */
    private void updatePullArrowDirection() {

        SocialListFragment socialListFragment = (SocialListFragment) getFragmentByTag(UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG);

        if (socialWindowStatus == SocialWindowStatus.FILLING_SCREEN) {

            socialListFragment.rotatePullArrow(180);

            // Enable message input after enlarging done.
            ((InstantMessageFragment) getFragmentByTag(INSTANT_MESSAGE_FRAGMENT_TAG)).changeMessageInputStatus(true);

        } else {
            socialListFragment.rotatePullArrow(0);
        }
    }


    /**
     * Return fragment from fragment manager depends on the given tag.
     *
     * @param tag fragment tag.
     * @return fragment with the given tag.
     */
    private Fragment getFragmentByTag(String tag) {

        if (!StringUtils.isBlank(tag)) {
            return getSupportFragmentManager().findFragmentByTag(tag);
        }

        return null;
    }


    /**
     * Used to control posts cover images loading while changing social window status, in order to remove blinking
     * in social list.
     *
     * @param status true to enable cover images loading, false to disable cover images loading.
     */
    private void setCoversImagesLoadingStatus(boolean status) {

        SocialListFragment socialListFragment = (SocialListFragment) getFragmentByTag(UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG);

        if (socialListFragment != null) {

            ((SocialWindowAdapter) socialListFragment.getListAdapter()).setCoverImageLoadingEnabled(status);
        }
    }


    // -----------------------------------------------------------------------------------------
    // Getters & Setters
    // -----------------------------------------------------------------------------------------

    /**
     * Return current visibility status of social window.
     *
     * @return current visibility status of social window.
     */
    public SocialWindowStatus getSocialWindowStatus() {
        return socialWindowStatus;
    }

    /**
     * Set the visibility status of social window.
     *
     * @param socialWindowStatus visibility status of social window.
     */
    public void setSocialWindowStatus(SocialWindowStatus socialWindowStatus) {
        this.socialWindowStatus = socialWindowStatus;
    }


    public boolean getRedirectToHomeEnabled() {
        return redirectToHomeEnabled;
    }


    public void setRedirectToHomeEnabled(boolean enableRedirectToHome) {
        this.redirectToHomeEnabled = enableRedirectToHome;
    }


    public void setSendBackPressEvent(boolean sendBackPressEvent) {
        this.sendBackPressEvent = sendBackPressEvent;
    }
}