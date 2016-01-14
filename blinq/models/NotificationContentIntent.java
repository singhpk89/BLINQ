package com.blinq.models;

import android.app.PendingIntent;

/**
 * Created by Osama on 11/3/2014.
 */
public class NotificationContentIntent {

    private PendingIntent contentIntent;
    private PendingIntent fullScreenIntent;
    private PendingIntent deleteIntent;
    private Platform platform;
    private int feedId;

    public NotificationContentIntent(int feedId,PendingIntent contentIntent, PendingIntent fullScreenIntent, PendingIntent deleteIntent, Platform platform) {
        this.contentIntent = contentIntent;
        this.fullScreenIntent = fullScreenIntent;
        this.deleteIntent = deleteIntent;
        this.platform = platform;
        this.feedId = feedId;
    }

    public PendingIntent getContentIntent() {
        return contentIntent;
    }

    public void setContentIntent(PendingIntent contentIntent) {
        this.contentIntent = contentIntent;
    }

    public PendingIntent getFullScreenIntent() {
        return fullScreenIntent;
    }

    public void setFullScreenIntent(PendingIntent fullScreenIntent) {
        this.fullScreenIntent = fullScreenIntent;
    }

    public PendingIntent getDeleteIntent() {
        return deleteIntent;
    }

    public void setDeleteIntent(PendingIntent deleteIntent) {
        this.deleteIntent = deleteIntent;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public int getFeedId() {
        return feedId;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }
}
