package com.blinq.models.social.window;

/**
 * Created by Johan Hansson.
 */
public class SocialWindowLoadingItem implements SocialWindowItem {

    @Override
    public SocialItemType getItemType() {
        return SocialItemType.LOADING;
    }
}

