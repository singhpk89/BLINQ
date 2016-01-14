package com.blinq.animation;

import android.view.View;
import android.view.animation.Animation;

import java.lang.ref.WeakReference;

public class ChangeVisibilityWhenFinishedAnimationListener extends AnimationListenerAdapter {

    private final WeakReference<View> viewWeakReference;
    private final int visibility;

    public ChangeVisibilityWhenFinishedAnimationListener(View view, int visibility) {
        viewWeakReference = new WeakReference<View>(view);
        this.visibility = visibility;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        final View view = viewWeakReference.get();
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

}
