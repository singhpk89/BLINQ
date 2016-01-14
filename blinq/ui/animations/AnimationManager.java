package com.blinq.ui.animations;

import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.blinq.R;
import com.blinq.PreferencesManager;
import com.blinq.models.Platform;
import com.blinq.ui.adapters.PlatformCoverListAdapter;
import com.blinq.ui.fragments.InstantMessageFragment;
import com.blinq.utils.Log;
import com.blinq.utils.UIUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manage first time animation.
 *
 * @author Johan Hansson
 */
public class AnimationManager {

    private static final String TAG = AnimationManager.class.getSimpleName();
    private final String PAGER_SCROLLER_FIELD = "mScroller";

    // Animation constants.
    private final int SOCIAL_WINDOW_ANIMATION_START_AFTER = 1400;
    private final int SHOW_SOCIAL_WINDOW_TIME_ANIMATION_MODE = 1500;
    private final int HIDE_SOCIAL_WINDOW_TIME_ANIMATION_MODE = 2000;
    private final int WAIT_ONE_SECOND = 1000;
    private final int SCROLL_HORIZONTALLY_TIME = 2000;
    private final int SHOW_STATUS_BAR_TIME = 500;
    private final int SCROLL_VERTICALLY_TIME = 1500;

    private final int VERTICALL_SCROLL_PERIOD = 1500;
    private final int START_RETRO_DIALER_ANIMATION_DELAY = 100;
    private final int RETRO_DIALER_ANIMATION_DELAY = 100;
    private final int RETRO_DIALER_ANIMATION_PERIOD = 2000;
    private final int STATUS_TRANSLATION_Y = 100;

    private ImageView retroDialerAnimationImageView;
    private ImageView platformIconAnimationImageView;
    private LinearLayout retroDialerLinearLayout;

    private TimerTask startRetroDialerTimerTask;
    private TimerTask retroDialerTimerTask;

    // Animation data
    private ListView coverList;
    private int index;
    private float socialWindowHeight;

    private PreferencesManager preferences;

    private enum AnimationType {
        SHOW_SOCIAL_WINDOW, SCROLL_HORIZONTALLY, SHOW_STATUS, SCROLL_VERTICALLY, HIDE_SOCIAL_WINDOW, START_RETRO_DIALER, FADE_RETRO_DIALER, FADE_ACCOUNT_FLIPPER
    }

    ;

    private static final HashMap<Platform, List<Integer>> dummyCoverResource = new HashMap<Platform, List<Integer>>() {

        /**
         * Auto generated serial version ID used in serializable processes.
         */
        private static final long serialVersionUID = -8357370681593937798L;

        {
            put(Platform.FACEBOOK, Arrays.asList(R.drawable.white_bitmap,
                    R.drawable.white_bitmap));
            put(Platform.INSTAGRAM, Arrays.asList(R.drawable.white_bitmap));
        }
    };

    private Activity activity;
    private InstantMessageFragment instantMessageFragment;
    private ViewPager coverStatusPagerView;
    private Timer swipeTimer;

    public AnimationManager(InstantMessageFragment instantMessageFragment,
                            View root) {

        this.instantMessageFragment = instantMessageFragment;
        this.coverStatusPagerView = (ViewPager) root
                .findViewById(R.id.cover_status_pager_view);
        this.activity = instantMessageFragment.getActivity();
        this.retroDialerAnimationImageView = (ImageView) root
                .findViewById(R.id.imageViewForRetroDialerAnimation);
        this.platformIconAnimationImageView = (ImageView) root
                .findViewById(R.id.imageViewForPlatformAnimation);
        this.retroDialerLinearLayout = (LinearLayout) root
                .findViewById(R.id.linearLayoutForRetroDialer);

        preferences = new PreferencesManager(root.getContext());

    }

    /**
     * Apply first time animation on comlog view.
     */
    public void applyFirstTimeAnimation() {

        prepareAnimation();

        retroDialerLinearLayout.setVisibility(View.VISIBLE);

        swipeTimer = new Timer();

        initializeStartRetroDialerTimerTask();

        initializeRetroDialerTimerTask();
        try {
            swipeTimer.schedule(startRetroDialerTimerTask,
                    START_RETRO_DIALER_ANIMATION_DELAY);

        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG, "Illegal State Exception while schedule swipe timer ");
        }
    }

    /**
     * Stop first time animation on comlog view.
     */
    public void stopAnimation() {

        swipeTimer.cancel();
        swipeTimer.purge();
        retroDialerTimerTask.cancel();
    }

    /**
     * Prepare animation data.
     */
    private void prepareAnimation() {

        socialWindowHeight = activity.getResources().getDimension(
                R.dimen.social_window_text_only_item_height);

        // Hide social window when animation start.
        slideSocialWindowUp(0);
    }

    /**
     * Start first use social window animation.
     */
    private void startSocialWindowAnimation() {

        TimerTask startSocialWindowAnimationTimerTask = new TimerTask() {

            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        resetAnimationChanges();

                    }
                });

            }
        };

        Timer showSocialWindowTimer = new Timer();
        try {
            showSocialWindowTimer.schedule(startSocialWindowAnimationTimerTask,
                    SOCIAL_WINDOW_ANIMATION_START_AFTER);

        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG,
                    "Illegal State Exception while schedule show Social Window Timer.");
        }

    }

    /**
     * Slide social window down with given time.
     *
     * @param duration animation duration.
     */
    public void slideSocialWindowDown(int duration) {

        SlideAnimation.slideVertically(coverStatusPagerView,
                -socialWindowHeight, coverStatusPagerView.getY(), duration);

    }

    /**
     * Slide social window up with given duration.
     *
     * @param duration animation duration.
     */
    public void slideSocialWindowUp(int duration) {

        SlideAnimation.slideVertically(coverStatusPagerView,
                coverStatusPagerView.getY(), -socialWindowHeight, duration);
    }

    /**
     * Show social window.
     */
    private void showSocialWindow() {

        coverStatusPagerView.setVisibility(View.VISIBLE);

        slideSocialWindowDown(SHOW_SOCIAL_WINDOW_TIME_ANIMATION_MODE);

//        TimerTask showSocialWindowTimerTask = new TimerTask() {
//
//            @Override
//            public void run() {
//
//                activity.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//
//                        scrollHorizontally();
//
//                    }
//                });
//
//            }
//        };
//
//        Timer showSocialWindowTimer = new Timer();
//        try {
//            showSocialWindowTimer.schedule(showSocialWindowTimerTask,
//                    SHOW_SOCIAL_WINDOW_TIME_ANIMATION_MODE + WAIT_ONE_SECOND);
//        } catch (IllegalStateException illegalStateException) {
//            Log.e(TAG,
//                    "Illegal State Exception while schedule show Social Window Timer.");
//        }

    }

    /**
     * Scroll social window horizontally.
     */
    private void scrollHorizontally() {

        UIUtils.alertUser(
                activity.getApplicationContext(),
                activity.getApplicationContext().getString(
                        R.string.first_social_animation)
        );

        // Set animation mode scroller.
        FixedSpeedScroller animationModeScroller = new FixedSpeedScroller(
                activity.getApplicationContext(), SCROLL_HORIZONTALLY_TIME,
                true);

        setCustomScrollerForSocialWindowPager(animationModeScroller);

        coverStatusPagerView.setCurrentItem(--index, true);

        if (index == 0) {
            coverList = (ListView) coverStatusPagerView.getChildAt(0)
                    .findViewById(R.id.cover_list);
            coverList.setDivider(null);
        }

        TimerTask scrollHorizontallyTimerTask = new TimerTask() {

            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        showStatusBar();

                    }
                });

            }
        };

        Timer scrollHorizontallyTimer = new Timer();
        try {
            scrollHorizontallyTimer.schedule(scrollHorizontallyTimerTask,
                    SCROLL_HORIZONTALLY_TIME);
        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG,
                    "Illegal State Exception while schedule scroll hirizontallt  Timer.");
        }

    }

    /**
     * Set custom scroller for social window pager with 1.5 second period.
     */
    private void setCustomScrollerForSocialWindowPager(
            FixedSpeedScroller scroller) {

        try {

            Field scrollerField;
            scrollerField = ViewPager.class
                    .getDeclaredField(PAGER_SCROLLER_FIELD);
            scrollerField.setAccessible(true);

            scrollerField.set(coverStatusPagerView, scroller);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show status bar.
     */
    private void showStatusBar() {

        LinearLayout accountStatusLayout = (LinearLayout) coverList
                .findViewById(R.id.account_status_layout);
        accountStatusLayout.setTranslationY(STATUS_TRANSLATION_Y);
        accountStatusLayout.setVisibility(View.VISIBLE);

        accountStatusLayout.animate().setDuration(SHOW_STATUS_BAR_TIME)
                .translationY(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                PlatformCoverListAdapter listAdapter = (PlatformCoverListAdapter) coverList
                        .getAdapter();
                listAdapter.setShowStatus(true);
                listAdapter.notifyDataSetChanged();
            }
        });

        TimerTask showStatusBarTimerTask = new TimerTask() {

            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        scrollVertically();

                    }
                });

            }
        };

        Timer showStatusBarTimer = new Timer();
        try {
            showStatusBarTimer.schedule(showStatusBarTimerTask,
                    SHOW_STATUS_BAR_TIME + WAIT_ONE_SECOND);
        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG,
                    "Illegal State Exception while schedule show status bar Window Timer.");
        }

    }

    /**
     * Scroll social window pager vertically.
     */
    private void scrollVertically() {

        coverList.smoothScrollToPositionFromTop(++index, 0,
                VERTICALL_SCROLL_PERIOD);

        TimerTask scrollVerticallyTimerTask = new TimerTask() {

            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        hideSocialWindow();

                    }
                });

            }
        };

        Timer scrollVerticallyTimer = new Timer();
        try {
            scrollVerticallyTimer.schedule(scrollVerticallyTimerTask,
                    SCROLL_VERTICALLY_TIME + WAIT_ONE_SECOND);
        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG,
                    "Illegal State Exception while schedule scroll vertically Timer.");
        }

    }

    /**
     * Hide social window - removed for now
     */
    private void hideSocialWindow() {

        slideSocialWindowUp(HIDE_SOCIAL_WINDOW_TIME_ANIMATION_MODE);

        TimerTask hideSocialWindowTimerTask = new TimerTask() {

            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        resetAnimationChanges();

                    }
                });

            }
        };

        Timer hideSocialWindowTimer = new Timer();
        try {
            hideSocialWindowTimer.schedule(hideSocialWindowTimerTask,
                    HIDE_SOCIAL_WINDOW_TIME_ANIMATION_MODE);
        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG,
                    "Illegal State Exception while schedule hide Social Window Timer.");
        }

    }


    /**
     * Reset all attributes related to animation.
     */
    private void resetAnimationChanges() {

        preferences.setProperty(PreferencesManager.FIRST_COMLOG_USE, false);

        // Set default mode scroller.
        FixedSpeedScroller defaultModeScroller = new FixedSpeedScroller(
                activity.getApplicationContext(), 0, false);
        setCustomScrollerForSocialWindowPager(defaultModeScroller);

        Activity activity = instantMessageFragment.getActivity();
        instantMessageFragment.restartInstantMessage(Platform.FACEBOOK);

        // Remove animation layout.
        retroDialerLinearLayout.setVisibility(View.GONE);
    }

    /**
     * Initialize the start of retro dialer animation.
     */
    private void initializeStartRetroDialerTimerTask() {

        startRetroDialerTimerTask = new TimerTask() {

            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        retroDialerAnimationImageView
                                .setVisibility(View.VISIBLE);

                        applyFadeInAnimation();

                    }

                });

            }
        };

    }

    /**
     * Show retro dialer with fade in animation.
     */
    private void applyFadeInAnimation() {

        Animation fadeInAnimation = AnimationUtils.loadAnimation(activity,
                R.anim.fade_in);
        retroDialerAnimationImageView.startAnimation(fadeInAnimation);

        fadeInAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                swipeTimer.cancel();
                swipeTimer.purge();
                startRetroDialerTimerTask.cancel();
                swipeTimer = new Timer();

                try {
                    swipeTimer.schedule(retroDialerTimerTask,
                            RETRO_DIALER_ANIMATION_DELAY,
                            RETRO_DIALER_ANIMATION_PERIOD);

                } catch (IllegalStateException illegalStateException) {
                    Log.e(TAG,
                            "Illegal State Exception while schedule swipe timer ");
                }

            }
        });

    }

    /**
     * Initialize the timer task for retro dialer first use animation.
     */
    private void initializeRetroDialerTimerTask() {

        retroDialerTimerTask = new TimerTask() {

            AnimationType currentAnimationType = AnimationType.FADE_RETRO_DIALER;

            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        switch (currentAnimationType) {

                            case FADE_RETRO_DIALER:

                                retroDialerAnimationImageView
                                        .startAnimation(new WheelExitAnimation(
                                                WheelExitAnimation.RETRO_DIALER_ANIMATION_DURATION,
                                                WheelExitAnimation.RETRO_DIALER_ANIMATION_START_ANGLE,
                                                WheelExitAnimation.RETRO_DIALER_ANIMATION_ARCANGLE,
                                                WheelExitAnimation.COUNTER_CLOCKWISE,
                                                retroDialerAnimationImageView));
                                currentAnimationType = AnimationType.FADE_ACCOUNT_FLIPPER;
                                break;

                            case FADE_ACCOUNT_FLIPPER:
                                platformIconAnimationImageView
                                        .setVisibility(View.VISIBLE);
                                platformIconAnimationImageView
                                        .startAnimation(new WheelExitAnimation(
                                                WheelExitAnimation.ACCOUNT_FLIPEER_ANIMATION_DURATION,
                                                WheelExitAnimation.ACCOUNT_FLIPPER_ANIMATION_START_ANGLE,
                                                WheelExitAnimation.ACCOUNT_FLIPEER_ANIMATION_ARCANGLE,
                                                WheelExitAnimation.CLOCKWISE,
                                                platformIconAnimationImageView));

                                swipeTimer.cancel();
                                swipeTimer.purge();
                                retroDialerTimerTask.cancel();
                                swipeTimer = new Timer();

                                startSocialWindowAnimation();

                                break;

                        }

                    }

                });

            }
        };

    }
}
