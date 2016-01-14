package com.blinq.models;


public class NotificationData {

    /**
     * The id supplied to notify(int, Notification).
     */
    private int id;
    /**
     * The main text payload.
     */
    private CharSequence text;
    /**
     * The title of the notification.
     */
    private CharSequence title;
    /**
     * The content of the notification.
     */
    private CharSequence content;
    /**
     * A timestamp related to this notification, in milliseconds.
     */
    private long received;
    /**
     * The package of the app that posted the notification.
     */
    private String packageName;
    /**
     * The platform of the app that posted the notification.
     */
    private Platform platform;
    /**
     * The number of events that this notification represents.
     */
    private int count;
    /**
     * The tag supplied to notify(int, Notification), or null if no tag was
     * specified.
     */
    private String tag;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setTitle(CharSequence title) {
        this.title = title;
    }

    public CharSequence getContent() {
        return content;
    }

    public void setContent(CharSequence content) {
        this.content = content;
    }

    public long getReceived() {
        return received;
    }

    public void setReceived(long received) {
        this.received = received;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isEqual(NotificationData nd) {
        CharSequence title1 = nd.title;
        CharSequence title2 = this.title;
        CharSequence text1 = nd.text;
        CharSequence text2 = this.text;
        CharSequence content1 = nd.content;
        CharSequence content2 = this.content;
        if (title1 == null)
            title1 = "";
        if (title2 == null)
            title2 = "";
        if (text1 == null)
            text1 = "";
        if (text2 == null)
            text2 = "";
        if (content1 == null)
            content1 = "";
        if (content2 == null)
            content2 = "";
        return title1.equals(title2) && text1.equals(text2)
                && content1.equals(content2);
    }

    public NotificationData() {
    }

    public NotificationData(NotificationData nd) {
        super();
        this.id = nd.id;
        this.text = nd.text;
        this.title = nd.title;
        this.content = nd.content;
        this.received = nd.received;
        this.packageName = nd.packageName;
        this.platform = nd.platform;
        this.count = nd.count;
        this.tag = nd.tag;
    }

    @Override
    public String toString() {
        return "NotificationData [id=" + id + ", text=" + text + ", title="
                + title + ", content=" + content + ", received=" + received
                + ", packageName=" + packageName + ", count=" + count
                + ", tag=" + tag + "]";
    }

}