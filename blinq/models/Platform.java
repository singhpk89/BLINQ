package com.blinq.models;

import java.util.List;

/**
 * Represent Platforms type in our system Maps an integer to platform type
 * additional platforms will be added in the future to this enum
 *
 * @author Gal Bracha
 */
public enum Platform {

    NOTHING(0, ""),
    CALL(1, "Mobile"),
    SMS(2, "Mobile"),
    MMS(7, "MMS"),
    FACEBOOK(3, "Facebook"),
    HANGOUTS(4, "Hangouts"),
    TWITTER(5, "Twitter"),
    INSTAGRAM(6, "Instagram"),
    SKYPE(8, "Skype"),
    OTHER(9, ""),
    WHATSAPP(10, "Whatsapp"),
    EMAIL(11, "Emails"),
    LINKEDIN(12, "LinkedIn"),
    GOOGLEPLUS(13, "GooglePlus"),
    PICASA(14, "Picasa"),
    ALL(15, "ALL"),
    ANGELLIST(16, "Angellist"),
    PINTEREST(17, "Pinterest"),
    KLOUT(18, "Klout"),
    ABOUTME(19, "Aboutme"),
    GRAVATAR(20, "Gravatar"),
    GITHUB(21, "Github"),
    FOURSQUARE(22, "Foursquare"),
    VIMEO(23, "Vimeo"),
    STATICINFO(90, "Staticinfo"); //Used to map server to client contact


    private int id;
    private String displayName;

    private Platform(int id, String name) {
        this.id = id;
        this.displayName = name;
    }

    public int getId() {
        return id;
    }

    public String getIdAsString() {
        return String.valueOf(id);
    }

    public String getName() {
        return displayName;
    }

    public static Platform fromId(int id) {
        for (Platform e : Platform.values()) {
            if (e.getId() == id)
                return e;
        }
        return Platform.NOTHING;
    }

    /**
     * Convert platform list to string separated with commas.
     */
    public static String convertToString(List<Platform> platforms) {

        String platformsString = "";
        for (Platform platform : platforms) {
            platformsString += platform.getId() + ",";
        }

        return platformsString.substring(0, platformsString.length() - 1);
    }
}