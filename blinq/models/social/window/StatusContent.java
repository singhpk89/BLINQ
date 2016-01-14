package com.blinq.models.social.window;

/**
 * Represents the content of the social window post.
 * Created by Johan Hansson on 9/29/2014.
 */
public enum StatusContent {

    NOTHING(""),
    LINK("link"),
    PHOTO("photo"),
    VIDEO("video"),
    STATUS("status");

    private String name;

    private StatusContent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static StatusContent fromName(String name) {
        for (StatusContent e : StatusContent.values()) {
            if (e.getName().equals(name))
                return e;
        }
        return StatusContent.NOTHING;
    }
}