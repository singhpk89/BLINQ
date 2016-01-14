package com.blinq.models;

import java.util.Date;

import android.net.Uri;

/**
 * Represent notification components.
 *
 * @author Johan Hansson.
 */
public class HeadboxNotification {

    /**
     * User name for the notification message.
     */
    private String name;

    /**
     * Message body for the notification.
     */
    private String body;

    /**
     * Account id.
     */
    private int accountId;

    /**
     * Notification date.
     */
    private Date date;

    /**
     * Notification title.
     */
    private String title;

    /**
     * Notification image Uri.
     */
    private Uri imageUri;

    public HeadboxNotification() {
        super();
    }

    public HeadboxNotification(String name, String body, int index, Date date) {

        super();

        this.name = name;
        this.body = body;
        this.accountId = index;
        this.date = date;

    }

    public String getName() {

        return name;

    }

    public void setName(String name) {

        this.name = name;

    }

    public String getBody() {

        return body;

    }

    public void setBody(String body) {

        this.body = body;

    }

    public int getIndex() {

        return accountId;

    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setIndex(int index) {

        this.accountId = index;

    }

    /**
     * @return notification image URI.
     */
    public Uri getImageUri() {
        return imageUri;
    }

    /**
     * @param imageUri notification image URI.
     */
    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    /**
     * @return notification title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title notification title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

}
