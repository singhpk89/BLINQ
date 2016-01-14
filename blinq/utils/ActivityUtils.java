package com.blinq.utils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

public class ActivityUtils {

    //http://stackoverflow.com/questions/1147172/what-android-tools-and-methods-work-best-to-find-memory-resource-leaks

    public static void unbindDrawables(View view) {
        unbindDrawables(view, false);
    }

    public static void unbindDrawables(View view, boolean skipGarbageCollection) {

        try {

            if (view.getBackground() != null)
                view.getBackground().setCallback(null);

            if (view instanceof ImageView) {
                ImageView imageView = (ImageView) view;
                imageView.setImageBitmap(null);
            } else if (view instanceof ViewGroup) {

                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++)
                    unbindDrawables(viewGroup.getChildAt(i));

                if (!(view instanceof AdapterView))
                    viewGroup.removeAllViews();
            }

            if (!skipGarbageCollection) System.gc();

        } catch (Exception e) {
        }
    }
}