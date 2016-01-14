package com.blinq.models.social.window;

import com.blinq.models.Platform;

/**
 * Created by Johan Hansson on 9/28/2014.
 * <p/>
 * Model for Card item (Merge/Connect) displayed in social window.
 * It can be twitter, instagram card
 */
public class SocialWindowCard implements SocialWindowItem {

    public enum CardType {CONNECT, MERGE}

    private Platform platform;
    private String friendName;
    private CardType type;


    /**
     * Return the platform of the card.
     *
     * @return card platform.
     */
    public Platform getPlatform() {
        return platform;
    }


    /**
     * Set the platform of the card.
     *
     * @param platform card platform.
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }


    /**
     * Return friend name to apply action (merge/connect) with.
     *
     * @return friend name.
     */
    public String getFriendName() {
        return friendName;
    }


    /**
     * Set friend name to apply action (merge/connect) with.
     *
     * @param friendName friend name.
     */
    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }


    /**
     * Return card type (connect/merge).
     *
     * @return card type.
     */
    public CardType getType() {
        return type;
    }


    /**
     * Set the type (connect/merge) for card.
     *
     * @param type card type.
     */
    public void setType(CardType type) {
        this.type = type;
    }


    @Override
    public SocialItemType getItemType() {
        return SocialItemType.CARD;
    }
}
