package com.blinq.ui.fragments.utils;

import android.content.Context;
import android.view.View;
import android.widget.TabHost.TabContentFactory;

/**
 * @author Johan Hansson
 */
public class TabFactory implements TabContentFactory {

    private final Context context;

    public TabFactory(Context context) {
        this.context = context;
    }

    /**
     * @param tag tab tag.
     * @return return a view for tab.
     */
    public View createTabContent(String tag) {
        View v = new View(context);
        v.setMinimumWidth(0);
        v.setMinimumHeight(0);

        return v;
    }
}
