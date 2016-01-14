package com.blinq.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * A ScrollView that doesn't allow manual scrolling (only programmatic).
 * Implementation based on:
 * http://stackoverflow.com/questions/18893198/how-to-disable-and-enable-the-scrolling-on-android-scrollview
 */
public class LockedScrollView extends ScrollView {

    public LockedScrollView(Context context) {
        super(context);
    }

    public LockedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockedScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
}
