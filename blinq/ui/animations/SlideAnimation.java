package com.blinq.ui.animations;

import android.view.View;
import android.view.animation.TranslateAnimation;

/**
 * Handle slide animation.
 *
 * @author Johan Hansson.
 */
public class SlideAnimation {

    /**
     * Slide given view vertically.
     *
     * @param view     view to slide.
     * @param fromY    start point.
     * @param toY      end point.
     * @param duration sliding period in milliseconds.
     */
    public static void slideVertically(View view, float fromY, float toY,
                                       int duration) {

        TranslateAnimation slide = new TranslateAnimation(0, 0, fromY, toY);
        slide.setDuration(duration);
        slide.setFillAfter(true);
        view.startAnimation(slide);
    }

    /**
     * Slide given view horizontally.
     *
     * @param view     view to be animated.
     * @param fromX    start point.
     * @param toX      end point.
     * @param duration sliding period in milliseconds.
     */
    public static void slideHorizontally(View view, float fromX, float toX,
                                         int duration) {

        TranslateAnimation slide = new TranslateAnimation(fromX, toX, 0, 0);
        slide.setDuration(duration);
        slide.setFillAfter(true);
        view.startAnimation(slide);
    }

}
