package com.blinq.animation;

import android.view.View;

public class SetGoneWhenFinishedAnimationListener extends ChangeVisibilityWhenFinishedAnimationListener {

    public SetGoneWhenFinishedAnimationListener(View view) {
        super(view, View.GONE);
    }

}
