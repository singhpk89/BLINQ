package com.blinq.ui.animations;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class FixedSpeedScroller extends Scroller {

    private int mDuration;
    private boolean isAnimationMode;

    public FixedSpeedScroller(Context context, int mDuration,
                              boolean isAnimationMode) {
        super(context);

        this.mDuration = mDuration;
        this.isAnimationMode = isAnimationMode;
    }

    public FixedSpeedScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    public FixedSpeedScroller(Context context, Interpolator interpolator,
                              boolean flywheel) {
        super(context, interpolator, flywheel);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {

        // Ignore received duration, use fixed one instead
        if (isAnimationMode) {
            super.startScroll(startX, startY, dx, dy, mDuration);

        } else {
            super.startScroll(startX, startY, dx, dy, duration);

        }

    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy) {

        // Ignore received duration, use fixed one instead
        if (isAnimationMode) {
            super.startScroll(startX, startY, dx, dy, mDuration);

        } else {
            super.startScroll(startX, startY, dx, dy);
        }
    }
}