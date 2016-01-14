package com.blinq.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;

import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.authentication.impl.provider.SocialWindowProvider;
import com.blinq.models.FeedModel;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.service.FloatingDotService;
import com.blinq.service.TasksObserver;
import com.blinq.ui.activities.search.BlinqSearchHandler;
import com.blinq.ui.adapters.SocialWindowAdapter;
import com.blinq.ui.animations.ResizeAnimation;
import com.blinq.ui.animations.SlideAnimation;
import com.blinq.ui.fragments.BlinqMergeFragment;
import com.blinq.ui.fragments.BlinqSocialListFragment;
import com.blinq.ui.fragments.InstantMessageFragment;
import com.blinq.ui.views.UndoActionView;
import com.blinq.utils.Constants;
import com.blinq.utils.Log;
import com.blinq.utils.ManageKeyguard;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class PopupSocialWindow extends Activity implements
        BlinqSocialListFragment.OnFragmentInteractionListener,
        BlinqMergeFragment.OnMergeDone,
        BlinqSocialListFragment.OnUiUpdate,
        BlinqSocialListFragment.OnSocialWindowAffected, UndoActionView.UndoListener {


    private static final String TAG = PopupSocialWindow.class.getSimpleName();
    private View unifiedSocialWindowView;
    private Provider provider;
    private int feedId;
    private FeedModel friendFeed;
    private HashMap<Platform, List<MemberContact>> memberContacts;

    // Social window slide down/up animation duration in millis.
    public static final int SOCIAL_WINDOW_SLIDE_DOWN_DURATION = 400;
    public static final int SOCIAL_WINDOW_SLIDE_DOWN_FAST_DURATION = 20;
    public static final int SOCIAL_WINDOW_SLIDE_UP_DURATION = 400;
    public static final int SOCIAL_WINDOW_SLIDE_UP_FAST_DURATION = 20;
    private View mergeWindowView;
    private BlinqSocialListFragment socialListFragment;
    private Platform platform;
    private boolean isFromMerge;

    // When set to true. and the activity calls onStop
    // bring back the dot.
    PreferencesManager preferencesManager;


    // Enum for different status of social window.
    public static enum SocialWindowStatus {
        DEFAULT, PROCESSING, HIDDEN, SocialWindowStatus, FILLING_SCREEN
    }

    // For enlarge social window by sliding pull arrow.
    float socialWindowOriginalHeight = 0, diff, previousY = 0;

    // Number of diffs to cache, used to smooth touch behavior.
    private final int NUMBER_OF_DIFFS_TO_CACHE = 2;

    private LinkedBlockingDeque<Integer> lastDiffs;
    private static final String UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG = "unifiedSocialWindowFragment";
    public static final String INSTANT_MESSAGE_FRAGMENT_TAG = "instantMessageFragment";
    public static final String MERGE_FRAGMENT_TAG = "mergeFragment";
    private FragmentManager fragmentManager;
    private SocialWindowStatus socialWindowStatus = SocialWindowStatus.DEFAULT;

    private static boolean isCurrentlyDisplayed;
    public static boolean forcingMessengerHide;

    private boolean isFromFeedActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isFromFeedActivity = getIntent().getBooleanExtra(Constants.FROM_FEED, false);
        platform = Platform.fromId(getIntent().getExtras().getInt(Constants.PLATFORM,0));

        int layoutId = showAsPopup();

        setContentView(layoutId);
        setOnTouchListenerIfNeeded(layoutId);
        preferencesManager = new PreferencesManager(getApplicationContext());
        //this.setFinishOnTouchOutside(true);
        lastDiffs = new LinkedBlockingDeque<Integer>();

        provider = FeedProviderImpl.getInstance(this);

        feedId = getIntent().getExtras().getInt(Constants.FEED_ID);

        if (provider != null) {
            friendFeed = provider.getFeed(feedId);
        }
        if (feedId == 0 && friendFeed == null) {
            isCurrentlyDisplayed = false;
            finish();
            return;
        }
        if (getIntent() != null && getIntent().getExtras() != null &&
                getIntent().getExtras().getBoolean(Constants.IS_CLOSE, false)) {
            isCurrentlyDisplayed = false;
            finish();
            return;
        }

        unifiedSocialWindowView = findViewById(R.id.container);
        mergeWindowView = findViewById(R.id.container1);
        mergeWindowView.setVisibility(View.GONE);
        // Get member contacts.
        memberContacts = provider.getContacts(feedId);

        getActionBar().hide();
        if (savedInstanceState == null) {

            socialListFragment = BlinqSocialListFragment.newInstance(memberContacts, friendFeed);
            loadFragment(socialListFragment, UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG, R.id.container);
        }

        isCurrentlyDisplayed = true;
    }

    /**
     * If we display the social window in full screen, we need to set this listener, so if the user click outside of the
     * visible window - the activity ends.
     */
    private void setOnTouchListenerIfNeeded(int layoutId) {
        if (layoutId != R.layout.activity_new_social_window_padded) {
            return;
        }
        findViewById(R.id.social_window_pad).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();
                    if(isClickedOutside(view, x, y)) {
                        finish();
                    }
                return true;
            }
        });
    }

    private boolean isClickedOutside(View view, int x, int y) {
        int height = view.getHeight();
        int width = view.getWidth();
        return (x <= R.dimen.social_window_full_screen_padding_left ||
                y <= R.dimen.social_window_cover_photo_margin_top ||
                x >= width - R.dimen.social_window_full_screen_padding_right ||
                y >= height - R.dimen.social_window_full_screen_padding_bottom);
    }


    /**
     * Check if the window is currently displayed. Used when clicking the dot - if the window is open, close it
     */
    public static boolean isWindowCurrentlyDisplayed() {
        return isCurrentlyDisplayed;
    }

    /**
     * To show activity as dialog and dim the background with a specific layout configurations.
     *
     * @return the layout that will be used
     */
    public int showAsPopup() {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        if (platform == Platform.FACEBOOK && !isFromFeedActivity) {
            forcingMessengerHide = true;
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            return R.layout.activity_new_social_window_padded;
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        params.height = (int) (dpHeight * displayMetrics.density * (8.5 / 12));
        params.width = (int) (dpWidth * displayMetrics.density * 4.5 / 5);
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        getWindow().setAttributes(params);
        return R.layout.activity_new_social_window;
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

        fragmentManager = getFragmentManager();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // delete previous instant message fragment if exist, before adding new
        // one
        Fragment previousInstanceOfTheFragment = fragmentManager
                .findFragmentByTag(tag);

        if (previousInstanceOfTheFragment != null) {
            fragmentTransaction.remove(previousInstanceOfTheFragment);
        }

        fragmentTransaction.replace(containerId, fragment, tag);
        fragmentTransaction.commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_social_window, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void enlargeSocialWindowFullScreen() {
        enlargeSocialWindow(UIUtils.getScreenHeightWithActionBar(this), true);
    }

    @Override
    public void enlargeSocialWindow(int newHeight, boolean isRotateArrow) {

        if (socialWindowStatus == SocialWindowStatus.DEFAULT) {

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

            unifiedSocialWindowView.startAnimation(resizeAnimation);
        }
    }

    /**
     * Used to control posts cover images loading while changing social window status, in order to remove blinking
     * in social list.
     *
     * @param status true to enable cover images loading, false to disable cover images loading.
     */
    private void setCoversImagesLoadingStatus(boolean status) {

        BlinqSocialListFragment socialListFragment = (BlinqSocialListFragment) getFragmentByTag(UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG);

        if (socialListFragment != null) {

            ((SocialWindowAdapter) socialListFragment.getListAdapter()).setCoverImageLoadingEnabled(status);
        }
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

    public SocialWindowStatus getSocialWindowStatus() {
        return socialWindowStatus;
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
        //updatePullArrowDirection();
    }

    /**
     * Change pull arrow direction (up/down) depends on the social window status (filling screen / not filling).
     */
    private void updatePullArrowDirection() {

        BlinqSocialListFragment socialListFragment = (BlinqSocialListFragment) getFragmentByTag(UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG);

        if (socialWindowStatus == SocialWindowStatus.FILLING_SCREEN) {

            socialListFragment.rotatePullArrow(180);

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
            return getFragmentManager().findFragmentByTag(tag);
        }

        return null;
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
                            && roundedHeight < UIUtils.getScreenHeightWithActionBar(this)) {

                        changeSocialWindowHeight(roundedHeight);
                    }
                    break;
                // Action up.
                case MotionEvent.ACTION_UP:

                    // Enlarge social window to fill screen if touch increase it's height.
                    if (unifiedSocialWindowView.getHeight() > socialWindowOriginalHeight) {
                        enlargeSocialWindowFullScreen();
                    }

                    break;
            }
        }

        return false;
    }

    @Override
    public void onBackPressed() {

        BlinqMergeFragment mergeFragment = (BlinqMergeFragment) fragmentManager
                .findFragmentByTag(MERGE_FRAGMENT_TAG);

        if (mergeFragment != null) {

            fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(mergeFragment);
            displaySocialWindowView();

        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {

        //Check the cause of this
        // If sleep then do nothing

        super.onStop();

        if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
            return;
        }

        PowerManager pm = (PowerManager) getSystemService(getApplicationContext().POWER_SERVICE);

        if (!pm.isScreenOn()) {
            return;
        }

        if (platform == Platform.FACEBOOK &&
                !BlinqSocialListFragment.socialWindowItemClicked && !SocialWindowAdapter.profileClicked) {
            // User clicked home button
            forcingMessengerHide = false;
            finish();
        }
    }

    private void restoreFacebookMessengerChats() {
        if (!TasksObserver.getInstance(this).isFacebookMessengerServiceRunning(TasksObserver.FACEBOOK_CHAT_HEAD)) {
            return;
        }
        if (!forcingMessengerHide) {
            return;
        }
        if (memberContacts.containsKey(Platform.FACEBOOK)) {
            for (MemberContact memberContact : memberContacts.get(Platform.FACEBOOK)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://messaging/" +
                        memberContact.getId())));
            }
            forcingMessengerHide = false;
        }
    }

    /*
    * Handle touch outside for normal activity (When not opened from facebook)
    * */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If we've received a touch notification that the user has touched
        // outside the app, finish the activity.
        if (MotionEvent.ACTION_OUTSIDE == event.getAction()) {
            finish();
            return true;
        }

        // Delegate everything else to Activity.
        return super.onTouchEvent(event);
    }

    private void onSocialWindowClosed() {
        startService(new Intent(FloatingDotService.SOCIAL_WINDOW_CLOSED_ACTION,
                null, this, FloatingDotService.class));
    }


    /**
     * Invoked when an intent receives.
     */
    @Override
    protected void onNewIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            BlinqMergeFragment mergeFragment = (BlinqMergeFragment) fragmentManager
                    .findFragmentByTag(MERGE_FRAGMENT_TAG);

            if (mergeFragment != null) {
                mergeFragment.handleIntent(intent);
            }
        } else if (intent != null && intent.getBooleanExtra(Constants.IS_CLOSE, false)) {
            finish();
        }
    }

    @Override
    public void onMergeDone() {
        BlinqApplication.analyticsSender.sendMergedAPersonSuccessfully();
        this.isFromMerge = true;
        startPopupSocialWindowActivity(feedId);
    }

    /**
     * Start instants message activity with specific feed id.
     *
     * @param selectedFeedId feed id of the selected item in the search list.
     */
    private void startPopupSocialWindowActivity(int selectedFeedId) {
        finish();
        Intent intent = new Intent(this,
                PopupSocialWindow.class);
        intent.putExtra(Constants.FEED_ID, selectedFeedId);
        intent.putExtra(InstantMessageFragment.SHOW_KEYBOARD,
                true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.PLATFORM, platform.getId());
        intent.putExtra(Constants.FROM_FEED, isFromFeedActivity);
        startActivity(intent);
        SocialWindowProvider.getNewInstance(this, selectedFeedId).fetchSocialPosts(true, true);
    }

    @Override
    public void refresh() {

    }

    @Override
    public void refreshOnResume() {

    }


    /**
     * First time it is called with the right platformToMerge - from outside
     * next time it will use the same platform.
     */
    @Override
    public void displayMergeView(int feedId, Platform platform, BlinqSearchHandler.SearchViewMode mode) {
        hideSocialWindow();
        displayMergeFragment(feedId, platform, mode);

    }

    /**
     * Display merge view.
     *
     * @param feedId    current feed id.
     * @param platform  platform to merge with.
     * @param mergeType type used in centralize search handler to know how to use the search list view depends
     *                  on the type.
     */
    public void displayMergeFragment(int feedId, Platform platform, BlinqSearchHandler.SearchViewMode mergeType) {

        try {

            unifiedSocialWindowView.setVisibility(View.GONE);
            mergeWindowView.setVisibility(View.VISIBLE);
            BlinqMergeFragment mergeFragment = new BlinqMergeFragment();
            mergeFragment.setPlatform(platform);
            mergeFragment.setMergeType(mergeType);
            mergeFragment.setFeedId(feedId);
            loadFragment(mergeFragment, MERGE_FRAGMENT_TAG, R.id.container1);


            preferencesManager.setProperty(Constants.MERGE_TYPE, mergeType.getId());

        } catch (IllegalStateException illegalStateException) {

            Log.e(TAG, "IllegalStateException while displaying merge view:"
                    + illegalStateException);

        } catch (Exception exception) {

            Log.e(TAG, "Exception while displaying merge view :" + exception);
        }

    }

    @Override
    public void updateActionBar(Fragment fragment) {

    }

    @Override
    public void hideSocialWindow() {
        slideSocialWindowUp(SOCIAL_WINDOW_SLIDE_UP_DURATION);

    }

    @Override
    public void showSocialWindow() {
        slideSocialWindowDown(SOCIAL_WINDOW_SLIDE_DOWN_DURATION);

    }

    /**
     * Slide down social window smoothly.
     */
    private void slideSocialWindowDown(int duration) {

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

                if (unifiedSocialWindowView.getHeight() == UIUtils.getScreenHeightWithActionBar(PopupSocialWindow.this)) {

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

    public void displaySocialWindowView() {
        unifiedSocialWindowView.setVisibility(View.VISIBLE);
        mergeWindowView.setVisibility(View.VISIBLE);
        socialListFragment = BlinqSocialListFragment.newInstance(memberContacts, friendFeed);
        loadFragment(socialListFragment, UNIFIED_SOCIAL_WINDOW_FRAGMENT_TAG, R.id.container1);
    }

    @Override
    public void onUndo(Parcelable token) {
        provider.undoLastMerge();
        preferencesManager.setProperty(
                BlinqMergeFragment.DISPLAY_LAST_SEARCH_TEXT, true);

        displayMergeView(friendFeed.getFeedId(), BlinqApplication.searchSource, BlinqSearchHandler.SearchViewMode.MERGE);

        preferencesManager.setProperty(PreferencesManager.IS_SHOW_UNDO_MERGE,
                false);
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

    @Override
    public void hideWithoutAnimation() {

        unifiedSocialWindowView.setVisibility(View.GONE);
    }


    @Override
    public void showWithoutAnimation() {

        unifiedSocialWindowView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        if (!isFromMerge) {
            isCurrentlyDisplayed = false;
            if (platform == Platform.FACEBOOK && !isFromFeedActivity) {
                restoreFacebookMessengerChats();
            }
            onSocialWindowClosed();
            isFromMerge = false;
        }
        super.onDestroy();
    }

}
