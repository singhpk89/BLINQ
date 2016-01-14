package com.blinq.animation;

import android.view.View;

public class SetInvisibleWhenFinishedAnimationListener extends ChangeVisibilityWhenFinishedAnimationListener {

    public SetInvisibleWhenFinishedAnimationListener(View view) {
        super(view, View.INVISIBLE);
    }

}
