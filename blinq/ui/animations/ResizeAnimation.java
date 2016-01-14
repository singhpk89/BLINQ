package com.blinq.ui.animations;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by Johan Hansson on 9/24/2014.
 * <p/>
 * Custom animation to resize view smoothly.
 */
public class ResizeAnimation extends Animation {

    private final String TAG = ResizeAnimation.class.getSimpleName();

    private View view;

    private float fromHeight;
    private float toHeight;


    /**
     * @param view       view to be resize.
     * @param fromHeight start height of the view.
     * @param toHeight   end height of the view.
     * @param duration   resizing period.
     */
    public ResizeAnimation(View view, float fromHeight, float toHeight, int duration) {

        this.view = view;
        this.fromHeight = fromHeight;
        this.toHeight = toHeight;


        setDuration(duration);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {

        float height =
                (toHeight - fromHeight) * interpolatedTime + fromHeight;

        ViewGroup.LayoutParams p = view.getLayoutParams();
        p.height = (int) height;

        view.requestLayout();
    }
}
