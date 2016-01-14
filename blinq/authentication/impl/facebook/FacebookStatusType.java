package com.blinq.authentication.impl.facebook;

/**
 * Holds the Types of Facebook status.
 */
public enum FacebookStatusType {

    NOTHING(""),
    MOBILE_STATUS_UPDATE("mobile_status_update"),
    CREATED_NOTE("created_note"),
    ADDED_PHOTOS("added_photos"),
    ADDED_VIDEO("added_video"),
    SHARED_STORY("shared_story"),
    CREATED_GROUP("created_group"),
    CREATED_EVENT("created_event"),
    WALL_POST("wall_post"),
    APP_CREATED_STORY("app_created_story"),
    PUBLISHED_STORY("published_story"),
    APPROVED_FRIEND("approved_friend"),
    TAGGED_IN_PHOTO("tagged_in_photo");

    private String name;

    private FacebookStatusType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static FacebookStatusType fromName(String name) {
        for (FacebookStatusType e : FacebookStatusType.values()) {
            if (e.getName().equals(name))
                return e;
        }
        return FacebookStatusType.NOTHING;
    }

}
