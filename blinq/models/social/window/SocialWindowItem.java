package com.blinq.models.social.window;

/**
 * Created by Johan Hansson on 9/28/2014.
 * <p/>
 * Used to be implemented by items displayed in social window (Post & Card) in order to
 * know what is the current item to be displayed (Post or Card).
 */
public interface SocialWindowItem {


    public enum SocialItemType {LOADING, POST, CARD, ME_CARD}

    /**
     * @return true if the item is post, false if it's card.
     */
    public SocialItemType getItemType();
}
