package com.blinq.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.manuelpeinado.imagelayout.ImageLayout;

import java.lang.ref.WeakReference;

public class WalkthroughActivity extends HeadboxBaseActivity {

    private View overlayView;
    private View dotContainer;
    private View dotView;
    private View whiteCircle;
    private View redCircle;
    private ScrollView socialContentContainerView;
    private View socialContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fillScreen();
        setContentView(R.layout.activity_walkthrough);
        initializeComponents();
    }

    @Override
    public void onBackPressed() {
        sendEvent(AnalyticsConstants.BACK_WALKTHROUGH_EVENT, true,
                AnalyticsConstants.ONBOARDING_CATEGORY);
        super.onBackPressed();
    }

    private void initializeComponents() {
        final View phoneView = findViewById(R.id.walkthrough_phone);
        phoneView.setOnClickListener(startSocialAnimationClick());

        overlayView = findViewById(R.id.walkthrough_overlay);

        socialContentContainerView = (ScrollView) findViewById(R.id.walkthrough_social_content_container);
        socialContentView = findViewById(R.id.walkthrough_social_content);
        socialContentView.setOnClickListener(startSocialAnimationClick());

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.walkthrough_main_layout);
        mainLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendEvent(AnalyticsConstants.CLICKED_RANDOMLY_WALKTHROUGH_SCREEN_EVENT,
                        false, AnalyticsConstants.ONBOARDING_CATEGORY);
            }
        });

        dotContainer = findViewById(R.id.walkthrough_dot_container);

        whiteCircle = findViewById(R.id.walkthrough_white_circle);

        dotView = findViewById(R.id.walkthrough_dot);
        dotView.setOnClickListener(startSocialAnimationClick());
        final Animation dotFadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        dotFadeInAnimation.setStartOffset(400);
        final Animation dotBreatheAnimation = AnimationUtils.loadAnimation(this, R.anim.walkthrough_dot_breathe);
        dotBreatheAnimation.setAnimationListener(new RepeatWhenFinishedAnimationListener(dotView));
        dotFadeInAnimation.setAnimationListener(new StartAnimationWhenFinishedAnimationListener(dotView, dotBreatheAnimation));
        dotView.startAnimation(dotFadeInAnimation);

        final Animation redSquareAnimation = AnimationUtils.loadAnimation(this, R.anim.walkthrough_red_square);
        final View redSquare = findViewById(R.id.red_square);
        redSquareAnimation.setAnimationListener(new SetGoneWhenFinishedAnimationListener(redSquare));
        redSquare.startAnimation(redSquareAnimation);

        final Animation redCircleAnimation = AnimationUtils.loadAnimation(this, R.anim.walkthrough_red_circle_show);
        redCircle = findViewById(R.id.red_circle);
        redCircle.startAnimation(redCircleAnimation);
    }

    private static final class RepeatWhenFinishedAnimationListener implements Animation.AnimationListener {

        private final WeakReference<View> viewWeakReference;

        public RepeatWhenFinishedAnimationListener(View view) {
            viewWeakReference = new WeakReference<View>(view);
        }

        @Override
        public void onAnimationStart(Animation animation) {
            // do nothing
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            final View view = viewWeakReference.get();
            if (view != null) {
                view.startAnimation(animation);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // do nothing
        }
    }

    private static final class StartAnimationWhenFinishedAnimationListener implements Animation.AnimationListener {

        private final WeakReference<View> viewWeakReference;
        private final Animation animationToStartWhenFinished;

        private StartAnimationWhenFinishedAnimationListener(View view, Animation animationToStartWhenFinished) {
            viewWeakReference = new WeakReference<View>(view);
            this.animationToStartWhenFinished = animationToStartWhenFinished;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            // do nothing
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            final View view = viewWeakReference.get();
            if (view != null) {
                view.startAnimation(animationToStartWhenFinished);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // do nothing
        }
    }

    private static final class SetGoneWhenFinishedAnimationListener implements Animation.AnimationListener {

        private final WeakReference<View> viewWeakReference;

        public SetGoneWhenFinishedAnimationListener(View view) {
            viewWeakReference = new WeakReference<View>(view);
        }

        @Override
        public void onAnimationStart(Animation animation) {
            // do nothing
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            final View view = viewWeakReference.get();
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // do nothing
        }
    }

    private View.OnClickListener startSocialAnimationClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (overlayView.getVisibility() != View.VISIBLE) {
                    startSocialAnimation();
                } else {
                    startConnectMoreAnimation();
                }
            }
        };
    }

    private void startSocialAnimation() {
        if (redCircle.getAnimation() != null && !redCircle.getAnimation().hasEnded()) {
            redCircle.getAnimation().cancel();
        }
        if (redCircle.getVisibility() == View.VISIBLE) {
            final Animation hideRedCircleAnimation =
                    AnimationUtils.loadAnimation(WalkthroughActivity.this, R.anim.walkthrough_red_circle_hide);
            hideRedCircleAnimation.setAnimationListener(new SetGoneWhenFinishedAnimationListener(redCircle));
            redCircle.startAnimation(hideRedCircleAnimation);
        }

        overlayView.setVisibility(View.VISIBLE);
        final Animation overlayAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        overlayView.startAnimation(overlayAnimation);

        final Animation whiteCircleAnimation = AnimationUtils.loadAnimation(this, R.anim.walkthrough_white_circle);
        whiteCircleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // do nothing
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                socialContentContainerView.setVisibility(View.VISIBLE);
                socialContentContainerView.post(new Runnable() {
                    @Override
                    public void run() {
                        final int scrollAmount = socialContentView.getHeight()- socialContentContainerView.getHeight();
                        final ObjectAnimator scrollDownAnimator =
                                ObjectAnimator.ofInt(socialContentContainerView, "scrollY", 0, scrollAmount);
                        scrollDownAnimator.setDuration(20 * socialContentContainerView.getMaxScrollAmount());
                        scrollDownAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animator) {
                                final ObjectAnimator scrollUpAnimator =
                                        ObjectAnimator.ofInt(socialContentContainerView, "scrollY", scrollAmount, 0);
                                scrollUpAnimator.setDuration(4 * socialContentContainerView.getMaxScrollAmount());
                                scrollUpAnimator.setStartDelay(500);
                                scrollUpAnimator.start();
                            }
                        });
                        scrollDownAnimator.start();
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // do nothing
            }
        });
        whiteCircle.setVisibility(View.VISIBLE);
        whiteCircle.startAnimation(whiteCircleAnimation);

        final ImageLayout.LayoutParams dotContainerLayoutParams = (ImageLayout.LayoutParams) dotContainer.getLayoutParams();
        final ValueAnimator dotAnimator = ValueAnimator.ofInt(dotContainerLayoutParams.centerY, dotContainerLayoutParams.centerY - 110);
        dotAnimator.setInterpolator(new OvershootInterpolator());
        dotAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                dotContainerLayoutParams.centerY = (Integer) valueAnimator.getAnimatedValue();
                dotContainer.requestLayout();
            }
        });
        dotAnimator.start();
    }

    private void startConnectMoreAnimation() {
        forwardToNextView();
        //title.setText("See what your friends are doing on other platforms");
        //TODO: animate this
        //connectMoreLayout.setVisibility(View.VISIBLE);
    }

    /**
     * To Forward the user to the next view according to the connectivity status.
     */
    private void forwardToNextView() {

        startConnectMoreView();
    }

    /**
     * Redirect the user to Connect More platforms view.
     */
    private void startConnectMoreView() {
        Intent intent = new Intent(this, ConnectMoreActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

}
