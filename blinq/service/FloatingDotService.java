package com.blinq.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.blinq.BlinqApplication;
import com.blinq.R;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.authentication.impl.provider.SocialWindowProvider;
import com.blinq.models.Platform;
import com.blinq.service.TasksObserver.AppState;
import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.ui.adapters.SocialWindowAdapter;
import com.blinq.ui.fragments.BlinqSocialListFragment;
import com.blinq.ui.fragments.InstantMessageFragment;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.ImageUtils;
import com.crashlytics.android.Crashlytics;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class FloatingDotService extends Service implements TasksObserver.OnTaskMoveToBackgroundListener,SpringListener {

    private final String TAG = this.getClass().getSimpleName();

    public static String FEED_ID_EXTRA_TAG = "FEED_ID";
    public static String PLATFORM_EXTRA_TAG = "PLATFORM";
    public static String PACKAGE_NAME_EXTRA_TAG = "PACKAGE_NAME";
    public static String IS_OPEN_SOCIAL_WINDOW_TAG = "IS_OPEN_SOCIAL_WINDOW";
    public static String START_WITH_ANIMATION = "START_WITH_ANIMATION";
    public static String START_WITH_SOCIAL_WINDOW = "START_WITH_SOCIAL_WINDOW";
    public static int FEED_ID_DEFAULT_TAG = 0;

    public static int FADE_AWAY_DELAY = 180000; //3 min
    private static int DEFAULT_MARGIN_RIGHT_DP = 12;
    private static int DEFAULT_AFTER_PRESS_MARGIN_TOP_DP = 5;
    private static int DEFAULT_MARGIN_TOP_DP = 40;
    private static int DEFAULT_FACEBOOK_MARGIN_TOP_DP = 110;

    private static int CLOSE_X_BOUNDARY_MARGIN = 40;

    private static int DEFAULT_CLOSE_MARGIN_BOTTOM_DP = 80;

    private int MUTE_DOT_RESOURCE = R.drawable.mute_clock;
    private int MUTE_DOT_RESOURCE_HOVER = R.drawable.mute_clock_hover;

    private WindowManager windowManager;
    private LinearLayout blinqDot;
    private ImageView dotImageView;
    private ImageView closeDotView;

    private int feedId;
    private Platform platform;
    private String packageName;

    private WindowManager.LayoutParams dotParams = null;
    private WindowManager.LayoutParams closeDotParams = null;

    public static boolean isCloseDotAnimated = false;

    //We need this because we calculate dimensions from the
    //resource itself before we load the actual bitmap
    private ImageUtils.Dimensions dotDimensions;
    private ImageUtils.Dimensions closeDotDimensions;

    private int serviceStartId = 0;

    private DotManager dotManager;

    public static String SOCIAL_WINDOW_CLOSED_ACTION = "Social Window Closed";
    public static String CLOSE_DOT_ACTION = "Close Dot";
    public static String CONNECT_NEW_PLATFROM_STARTED = "connect_platform_started";
    public static String CONNECT_NEW_PLATFROM_ENDED = "connect_platform_ended";

    private WindowManager.LayoutParams paramsF;

    Point screenSize;

    private boolean isFromFeedActivity;

    //variables for drag_drop_animation
    private static double TENSION = 1000;
    private static double DAMPER = 30; //friction
    private SpringSystem springSystem;
    private Spring spring;
    private boolean isHandling;

    //variables for kill_dot_animation
    private Spring springX;
    private Spring springY;
    private SpringConfig CONVERGING = SpringConfig.fromOrigamiTensionAndFriction(40, 3);
    private boolean isKilling;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dotDimensions = ImageUtils.getResourceImageDimensions(this, R.drawable.dot);
        closeDotDimensions = ImageUtils.getResourceImageDimensions(this, MUTE_DOT_RESOURCE);

        if (intent == null)
            return super.onStartCommand(intent, flags, startId);

        initializeFloatingDot();
        initializeCloseDotView();
        initSpringSystem();

        if (isActionInIntent(intent, SOCIAL_WINDOW_CLOSED_ACTION)) {
            floatingDotHandler.postDelayed(fadeAwayTask, FADE_AWAY_DELAY);
            if (!isFromFeedActivity) {
                moveDotToDefaultLocation();
            } else {
                closeDotAndService();
                isFromFeedActivity = false;
            }
        } else if (isActionInIntent(intent,CLOSE_DOT_ACTION)) {
            Platform platformToClose = Platform.fromId(intent.getIntExtra(PLATFORM_EXTRA_TAG, Platform.NOTHING.getId()));
            // Check if the requested closed came from the closing of the right platform
            if (this.platform == platformToClose && !isFromFeedActivity) {
                closeDotAndService();
            }
            return super.onStartCommand(intent, flags, startId);
        } else if (isActionInIntent(intent, CONNECT_NEW_PLATFROM_STARTED)) {
            closeTheDot();
        } else if (isActionInIntent(intent, CONNECT_NEW_PLATFROM_ENDED)) {
            updateDotLocation(DEFAULT_AFTER_PRESS_MARGIN_TOP_DP);
            TasksObserver.getInstance(this).startListen(this, packageName, AppState.BACKGROUND, this);
        } else {
            this.feedId = intent.getIntExtra(FEED_ID_EXTRA_TAG, FEED_ID_DEFAULT_TAG);
            this.platform = Platform.fromId(intent.getIntExtra(PLATFORM_EXTRA_TAG, Platform.NOTHING.getId()));
            this.packageName = intent.getStringExtra(PACKAGE_NAME_EXTRA_TAG);
            startTaskObserver();

            dotManager = DotManager.getInstance();
            if (intent.getBooleanExtra(START_WITH_ANIMATION, false)) {
                animateDotForNewPost();
            } else {
                SocialWindowProvider.setNewPostsListener(newPostsListener);
            }

            AnalyticsSender sender = BlinqApplication.analyticsSender;
            sender.sendShowTheDot(platform);

            if (intent.getBooleanExtra(IS_OPEN_SOCIAL_WINDOW_TAG, false)) {
                isFromFeedActivity = true;
                updateDotLocation(DEFAULT_AFTER_PRESS_MARGIN_TOP_DP);
                openPopupSocialWindow(FloatingDotService.this, feedId, false);
                floatingDotHandler.removeCallbacks(fadeAwayTask);
            } else if (intent.getBooleanExtra(START_WITH_SOCIAL_WINDOW, false)) {
                //updateDotLocation(DEFAULT_AFTER_PRESS_MARGIN_TOP_DP);
                openPopupSocialWindow(FloatingDotService.this, feedId, false);
            } else {
                updateDotLocation(getDotDefaultTopMargin());
            }
        }
        addListenerOnFloatingDot();
        this.serviceStartId = startId;

        return super.onStartCommand(intent, flags, startId);
    }

    private void initSpringSystem() {
        springSystem = SpringSystem.create();

        spring = springSystem.createSpring();
        spring.addListener(new SpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                float value = (float) spring.getCurrentValue();
                float scale = 1f + value;
                dotImageView.setScaleX(scale);
                dotImageView.setScaleY(scale);
            }

            @Override
            public void onSpringAtRest(Spring spring) {
                if (isHandling == true) {
                    handleDotClick();
                }
            }

            @Override
            public void onSpringActivate(Spring spring) {

            }

            @Override
            public void onSpringEndStateChange(Spring spring) {

            }
        });

        SpringConfig config = new SpringConfig(TENSION, DAMPER);
        spring.setSpringConfig(config);

        springX = springSystem.createSpring();
        springX.addListener(this);

        springY = springSystem.createSpring();
        springY.addListener(this);
    }

    private void startTaskObserver() {
        TasksObserver.getInstance(this).startListen(this, packageName, AppState.BACKGROUND ,this);
    }


    private SocialWindowProvider.NewPostsListener newPostsListener = new SocialWindowProvider.NewPostsListener() {
        @Override
        public void onNewPosts() {
            animateDotForNewPost();
        }
    };

    private void closeTheDot() {
        if (blinqDot != null && blinqDot.isShown()) {
            floatingDotHandler.removeCallbacks(fadeAwayTask);
            windowManager.removeView(blinqDot);
        }
        if(closeDotView != null && closeDotView.isShown()) {
            windowManager.removeView(closeDotView);
        }
        TasksObserver.getInstance(this).cancel();
        springSystem.removeAllListeners();
        spring.removeAllListeners();
        springX.removeAllListeners();
        springY.removeAllListeners();
    }

    private void initializeFloatingDot() {
        if(windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        if(blinqDot == null) {
            dotImageView = new ImageView(this);
            dotImageView.setImageResource(R.drawable.dot);
            blinqDot = new LinearLayout(this);
            blinqDot.addView(dotImageView);
            dotParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            dotParams.gravity = Gravity.TOP | Gravity.LEFT;

            paramsF = dotParams;
        }

        // Add timer to floating dot.

        floatingDotHandler.postDelayed(fadeAwayTask, FADE_AWAY_DELAY);
    }


    private void initializeCloseDotView() {
        if(closeDotView == null) {
            closeDotView = new ImageView(this);
            closeDotView.setImageResource(MUTE_DOT_RESOURCE);
            closeDotParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            closeDotParams.gravity = Gravity.TOP | Gravity.LEFT;
        }

        updateDotCloseLocation();
    }

    private int getDotDefaultTopMargin() {
        int topMargin;
        if (platform != null && platform == Platform.FACEBOOK) {
            topMargin = DEFAULT_FACEBOOK_MARGIN_TOP_DP;
        }else{
            topMargin = DEFAULT_MARGIN_TOP_DP;
        }
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                topMargin, getResources().getDisplayMetrics());
    }

    private int getDotDefaultRightMargin() {
        screenSize = new Point();
        getScreenSize(screenSize);
        return screenSize.x - dotDimensions.width - (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_MARGIN_RIGHT_DP, getResources().getDisplayMetrics());
    }

    private void updateDotLocation(int topMargin) {
        if (dotParams == null)
            return;

        int rightMargin = getDotDefaultRightMargin();

        dotParams.x = rightMargin;
        dotParams.y = topMargin;

        if (blinqDot != null && blinqDot.isShown()) {
            windowManager.removeView(blinqDot);
        }
        if (blinqDot == null)
        {
            return;
        }
        try {
            windowManager.addView(blinqDot, dotParams);
        } catch (IllegalStateException e) {
            Log.e(TAG, "add dot failed " + e.getMessage());
            Crashlytics.logException(e);
        }
    }

    private void getScreenSize(final Point screenSize) {
        if(windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        getDisplaySize(windowManager.getDefaultDisplay(), screenSize);
    }

    private static Point getDisplaySize(final Display display,final Point point) {
        try {
            display.getSize(point);
        } catch (java.lang.NoSuchMethodError ignore) { // Older device
            point.x = display.getWidth();
            point.y = display.getHeight();
        }
        return point;
    }


    private void updateDotCloseLocation() {
        if(closeDotParams == null)
            return;

        Point screenSize = new Point();
        getScreenSize(screenSize);

        int topMarginClose = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CLOSE_MARGIN_BOTTOM_DP, getResources().getDisplayMetrics());
        closeDotParams.x = (screenSize.x - closeDotDimensions.width) / 2;
        closeDotParams.y = screenSize.y - closeDotDimensions.height - topMarginClose;

        if (closeDotView != null && closeDotView.isShown()) {
            windowManager.removeView(closeDotView);
        }
    }

    private void stopDotAnimationsIfNeeded() {
        if(schedulerDotAnimation != null && !schedulerDotAnimation.isShutdown()) {
            schedulerDotAnimation.shutdownNow();
        }
    }

    private void handleDotClick() {

        showCloseDot(false);
        if (blinqDot == null)
            return;

        floatingDotHandler.removeCallbacks(fadeAwayTask);
        if (PopupSocialWindow.isWindowCurrentlyDisplayed()) {
            closePopupSocialWindow();
        } else {
            openPopupSocialWindow(FloatingDotService.this, feedId, true );
        }
    }

    private void addListenerOnFloatingDot() {
        paramsF = dotParams;

        if (blinqDot == null) {
            initializeFloatingDot();
        }

        // Set touch listener to move the dot.
        blinqDot.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            private long startTouchTime;

            private final int MAX_CLICK_DURATION = 200;
            private final int X_DELAY = 200;

            private long timeSinceActionDown() {
                return System.currentTimeMillis() - startTouchTime;
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isKilling = false;
                        isHandling = false;
                        spring.setEndValue(1f);
                        startTouchTime = System.currentTimeMillis();
                        initialX = paramsF.x;
                        initialY = paramsF.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        floatingDotHandler.removeCallbacks(fadeAwayTask);
                        stopDotAnimationsIfNeeded();

                        break;
                    case MotionEvent.ACTION_UP:
                        isHandling = false;
                        isKilling = false;
                        spring.setEndValue(0f);
                        if (timeSinceActionDown() < MAX_CLICK_DURATION) {
                            isHandling = true;
//                            handleDotClick();
                            break;
                        }
                        floatingDotHandler.postDelayed(fadeAwayTask, FADE_AWAY_DELAY);
                        if (isDotHitX(paramsF.x, paramsF.y)) {
                            isKilling = true;

                            springX.setSpringConfig(CONVERGING);
                            springY.setSpringConfig(CONVERGING);
                            springX.setEndValue(closeDotParams.x - 5);
                            springY.setEndValue(closeDotParams.y - 5);

                            muteFeed();
                            return true;
                        }
                        showCloseDot(false);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        isHandling = false;
                        isKilling = false;
                        paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                        paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(blinqDot, paramsF);

                        springX.setEndValue(paramsF.x);
                        springY.setEndValue(paramsF.y);

                        if (!closeDotView.isShown() && timeSinceActionDown() >= X_DELAY) {
                            showCloseDot(true);
                        }

                        if (isDotHitX(paramsF.x, paramsF.y)) {
                            HighlightCloseDot(true);
                        } else {
                            HighlightCloseDot(false);
                        }
                        break;
                }
                return false;
            }

        });
    }


    private boolean isDotHitX(int dotX, int dotY) {
        if (closeDotParams == null) {
            return true;
        }

        if ((dotX >= (closeDotParams.x - CLOSE_X_BOUNDARY_MARGIN - dotDimensions.width)) &&
                (dotX <= (closeDotParams.x + closeDotDimensions.width + CLOSE_X_BOUNDARY_MARGIN)) &&
                (dotY >= (closeDotParams.y - CLOSE_X_BOUNDARY_MARGIN - dotDimensions.height)) &&
                (dotY <= (closeDotParams.y + closeDotDimensions.height + CLOSE_X_BOUNDARY_MARGIN))) {
            return true;
        }
        return false;
    }

    private void HighlightCloseDot(boolean isTrue) {
        if (closeDotView == null)
            return;

        if (isTrue) {
            if (!isCloseDotAnimated) {
                isCloseDotAnimated = true;
                closeDotView.setImageResource(MUTE_DOT_RESOURCE_HOVER);
            }
        } else {
            if (isCloseDotAnimated) {
                isCloseDotAnimated = false;
                closeDotView.setImageResource(MUTE_DOT_RESOURCE);
            }
        }
    }

    private void showCloseDot(boolean isShow) {
        try {
            if (closeDotView == null)
                return;

            if (isShow && !closeDotView.isShown()) {
                windowManager.addView(closeDotView, closeDotParams);
            } else if (closeDotView.isShown()) {
                windowManager.removeView(closeDotView);
            }
        }catch (IllegalStateException e) {
            //ignore as the close dot is already shown on screen
        }
    }

    final android.os.Handler floatingDotHandler = new android.os.Handler();
    Runnable fadeAwayTask = new Runnable() {
        public void run() {
            closeDotAndService();
        }
    };

    private void muteFeed() {
        dotManager.muteFeed(FloatingDotService.this, feedId, closeDotParams);
    }

    private void closeDotAndService() {
        springSystem.removeAllListeners();
        this.spring.removeAllListeners();
        springX.removeAllListeners();
        springY.removeAllListeners();
        if (blinqDot != null) {
            if (blinqDot.isShown()) {
                windowManager.removeView(blinqDot);
            }
            blinqDot = null;
            showCloseDot(false);
            closeDotView = null;
        }
        if (!BlinqSocialListFragment.socialWindowItemClicked) {
            TasksObserver.getInstance(this).cancel();
        }
        closePopupSocialWindow();
        stopSelf(serviceStartId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void openPopupSocialWindow(Context context, int feedId, boolean moveDotUp) {
        if (moveDotUp)
            moveDotUp();
        BlinqApplication.analyticsSender.sendClickedTheDot(platform);
        Intent intent = new Intent(context, PopupSocialWindow.class);
        intent.putExtra(Constants.FEED_ID, feedId);
        intent.putExtra(Constants.PLATFORM, platform.getId());
        intent.putExtra(InstantMessageFragment.SHOW_KEYBOARD, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.FROM_NOTIFICATION, true);
        intent.putExtra(Constants.FROM_FEED, isFromFeedActivity);
        startActivity(intent);
        dotManager.unmuteFeed(feedId);
        dotImageView.setScaleX(1);
        dotImageView.setScaleY(1);
    }

    ScheduledExecutorService schedulerDotAnimation;
    ScheduledFuture scheduledFuture;
    private void moveDotUp() {
        moveDotOnCorrectThread(true);
    }

    private void moveDotToDefaultLocation() {
        moveDotOnCorrectThread(false);
    }

    // TODO: REFACTOR!
    private void moveDotOnCorrectThread(final boolean toTop) {
        final int delay = toTop ? 400 : 200;

        if (schedulerDotAnimation != null) {
            schedulerDotAnimation.shutdown();
        }

        schedulerDotAnimation = Executors.newScheduledThreadPool(1);

        scheduledFuture = schedulerDotAnimation.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (blinqDot == null)
                    schedulerDotAnimation.shutdown();
                else
                    handlerFadeOut.sendEmptyMessage(0);
            }

            private final Handler handlerFadeOut = new Handler() {

                public void handleMessage(Message msg) {
                    dotParams.alpha -= 0.1f;

                    if (blinqDot != null && blinqDot.isShown()) {
                        windowManager.updateViewLayout(blinqDot, dotParams);
                    }
                    if (dotParams.alpha < 0f) {
                        dotParams.alpha = 0;
                        dotParams.y = toTop ? DEFAULT_AFTER_PRESS_MARGIN_TOP_DP : getDotDefaultTopMargin();
                        scheduledFuture.cancel(false);
                        schedulerDotAnimation.scheduleAtFixedRate(new Runnable() {
                            @Override
                            public void run() {
                                if (blinqDot == null)
                                    schedulerDotAnimation.shutdown();
                                else
                                    handlerFadeIn.sendEmptyMessage(0);
                            }

                            private Handler handlerFadeIn = new Handler() {
                                public void handleMessage(Message msg) {
                                    dotParams.alpha += 0.1f;

                                    if (blinqDot != null && blinqDot.isShown()) {
                                        windowManager.updateViewLayout(blinqDot, dotParams);
                                    }
                                    if (dotParams.alpha > 0.9f) {
                                        schedulerDotAnimation.shutdown();
                                        dotParams.alpha = 1f;
                                        if (blinqDot != null && blinqDot.isShown()) {
                                            windowManager.updateViewLayout(blinqDot, dotParams);
                                        }
                                    }
                                }
                            };

                        }, delay, 10, TimeUnit.MILLISECONDS);
                    }
                }
            };
        }, delay, 10, TimeUnit.MILLISECONDS);
    }

    private void closePopupSocialWindow() {
        if (!PopupSocialWindow.isWindowCurrentlyDisplayed()) {
            return;
        }
        Intent intent = new Intent(this, PopupSocialWindow.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.IS_CLOSE, true);
        this.startActivity(intent);
    }

    private int lastOrientation = Configuration.ORIENTATION_PORTRAIT;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (lastOrientation == newConfig.orientation)
            return;

        lastOrientation = newConfig.orientation;

        if (blinqDot == null || !blinqDot.isShown())
            return;

        updateDotLocation(getDotDefaultTopMargin());
        updateDotCloseLocation();
    }

    public void animateDotForNewPost() {
        new CountDownTimer(750, 10) {

            final float originX = dotParams.x;

            public void onTick(long millisUntilFinished) {
                dotParams.x -= 1;
                if(blinqDot == null || !blinqDot.isShown()) {
                    return;
                }
                try {
                    windowManager.updateViewLayout(blinqDot, dotParams);
                }catch(IllegalArgumentException e) {
                    //Ignore in case the view is no longer on screen
                }
            }

            public void onFinish() {
                new CountDownTimer(750, 10) {

                    public void onTick(long millisUntilFinished) {
                        if (blinqDot == null || !blinqDot.isShown() || dotParams.x >= originX) {
                            return;
                        }
                        dotParams.x += 1;
                        try {
                            windowManager.updateViewLayout(blinqDot, dotParams);
                        } catch(IllegalArgumentException e) {
                            //Ignore in case the view is no longer on screen
                        }
                    }

                    public void onFinish() {
                    }

                }.start();
            }
        }.start();
    }

    private boolean isActionInIntent(Intent intent, String action) {
        return (intent != null && intent.getAction() != null && intent.getAction().equals(action));
    }

    long movedToBackgroundTime = 0;
    static int DEFAULT_IDLE_TIME_TO_WAIT_FOR_DOT = 60000;

    @Override
    public void OnAppChangedState(String packageName, AppState state) {

        if (state == AppState.BACKGROUND) {
            if (BlinqSocialListFragment.socialWindowItemClicked || SocialWindowAdapter.profileClicked) {
                movedToBackgroundTime = System.currentTimeMillis();
                TasksObserver.getInstance(this).startListen(FloatingDotService.this, packageName,
                        AppState.FOREGROUND, this);
            } else {
                closeDotAndService();
            }
        }
        else if (AppUtils.findTime(movedToBackgroundTime) < DEFAULT_IDLE_TIME_TO_WAIT_FOR_DOT) {
            restartSelf();
        }
        if (BlinqSocialListFragment.socialWindowItemClicked )
            BlinqSocialListFragment.socialWindowItemClicked = false;
        if (SocialWindowAdapter.profileClicked)
            SocialWindowAdapter.profileClicked = false;
    }

    private void restartSelf() {
        Intent intent = new Intent(FloatingDotService.this, FloatingDotService.class);
        intent.putExtra(FloatingDotService.FEED_ID_EXTRA_TAG, feedId);
        intent.putExtra(FloatingDotService.PLATFORM_EXTRA_TAG, platform.getId());
        intent.putExtra(FloatingDotService.PACKAGE_NAME_EXTRA_TAG, this.packageName);
        intent.putExtra(FloatingDotService.START_WITH_SOCIAL_WINDOW, true);
        startService(intent);
    }

    @Override
    public void onSpringUpdate(Spring spring) {
        if(isKilling == true) {
            int x = (int) springX.getCurrentValue();
            int y = (int) springY.getCurrentValue();
            paramsF.x = x;
            paramsF.y = y;
            windowManager.updateViewLayout(blinqDot, paramsF);
        }
    }

    @Override
    public void onSpringAtRest(Spring spring) {
        if (isKilling == true) {
            closeDotAndService();
        }
    }

    @Override
    public void onSpringActivate(Spring spring) {

    }

    @Override
    public void onSpringEndStateChange(Spring spring) {

    }
}
