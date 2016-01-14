package com.blinq.models;

import android.graphics.Typeface;

/**
 * Singleton model contains the design of fields which changes depends on the
 * application mode.
 *
 * @author Johan Hansson
 */
public final class FeedDesign {

    private static FeedDesign modeDesign;

    private int feedActivityBackgroundColor;
    private int senderImageId;
    private int feedBodySelector;
    private int feedBodySelectorUnread;
    private int feedSeparatorColor;
    private int contactNameTextSelectorRead;
    private int contactNameTextSelectorUnread;
    private int messageTextSelectorRead;
    private int messageTextSelectorUnread;
    private int messageTimeAgoSelector;
    private Typeface senderTypefaceRead;
    private Typeface senderTypefaceUnread;
    private int mergeViewBackgroundColor;
    private int mergeViewSuggestionTitleColor;
    private int mergeViewSuggestionHintColor;
    private int mergeViewSuggestionSeparatorColor;
    private int feedLongpressBackgroundColor;

    private FeedDesign() {
        super();
    }

    public static FeedDesign getInstance() {

        // Apply singleton design pattern.
        if (modeDesign == null) {

            modeDesign = new FeedDesign();

        }

        return modeDesign;

    }

    /**
     * @return the color for feed activity background.
     */
    public int getFeedActivityBackgroundColor() {
        return feedActivityBackgroundColor;
    }

    /**
     * @param feedActivityBackgroundColor the color for feed activity background.
     */
    public void setFeedActivityBackgroundColor(int feedActivityBackgroundColor) {
        this.feedActivityBackgroundColor = feedActivityBackgroundColor;
    }

    /**
     * @return image id for feed sender.
     */
    public int getSenderImageId() {
        return senderImageId;
    }

    /**
     * @param senderImageId image id for feed sender.
     */
    public void setSenderImageId(int senderImageId) {
        this.senderImageId = senderImageId;
    }

    /**
     * @return the selector id of feed sender.
     */
    public int getContactNameTextSelectorRead() {
        return contactNameTextSelectorRead;
    }

    /**
     * @param contactNameTextSelectorRead the selector id of feed sender.
     */
    public void setContactNameTextSelectorRead(int contactNameTextSelectorRead) {
        this.contactNameTextSelectorRead = contactNameTextSelectorRead;
    }

    /**
     * @return the selector for unread feed contact name.
     */
    public int getContactNameTextSelectorUnread() {
        return contactNameTextSelectorUnread;
    }

    /**
     * @param contactNameTextSelectorUnread the selector for unread feed contact name.
     */
    public void setContactNameTextSelectorUnread(int contactNameTextSelectorUnread) {
        this.contactNameTextSelectorUnread = contactNameTextSelectorUnread;
    }


    /**
     * @return the selector for read feed message text.
     */
    public int getMessageTextSelectorRead() {
        return messageTextSelectorRead;
    }

    /**
     * @param messageTextSelectorRead the selector for read feed message text.
     */
    public void setMessageTextSelectorRead(int messageTextSelectorRead) {
        this.messageTextSelectorRead = messageTextSelectorRead;
    }

    /**
     * @return the selector for un-read feed message text.
     */
    public int getMessageTextSelectorUnread() {
        return messageTextSelectorUnread;
    }

    /**
     * @param messageTextSelectorUnread the selector for un-read feed message text.
     */
    public void setMessageTextSelectorUnread(int messageTextSelectorUnread) {
        this.messageTextSelectorUnread = messageTextSelectorUnread;
    }

    /**
     * @return typeface for feed sender when the status of feed is read.
     */
    public Typeface getSenderTypefaceRead() {
        return senderTypefaceRead;
    }

    /**
     * @param senderTypefaceRead typeface for feed sender when the status of feed is read.
     */
    public void setSenderTypefaceRead(Typeface senderTypefaceRead) {
        this.senderTypefaceRead = senderTypefaceRead;
    }

    /**
     * @return typeface for feed sender when the status of feed is unread.
     */
    public Typeface getSenderTypefaceUnread() {
        return senderTypefaceUnread;
    }

    /**
     * @param senderTypefaceUnread typeface for feed sender when the status of feed is unread.
     */
    public void setSenderTypefaceUnread(Typeface senderTypefaceUnread) {
        this.senderTypefaceUnread = senderTypefaceUnread;
    }

    /**
     * @return the color for separator line between feeds.
     */
    public int getFeedSeparatorColor() {
        return feedSeparatorColor;
    }

    /**
     * @param feedSeparatorColor the color for separator line between feeds.
     */
    public void setFeedSeparatorColor(int feedSeparatorColor) {
        this.feedSeparatorColor = feedSeparatorColor;
    }

    /**
     * @return the color of Merge View Background.
     */
    public int getMergeViewBackgroundColor() {
        return mergeViewBackgroundColor;
    }

    /**
     * @param mergeViewBackgroundColor the color of Merge View Background.
     */
    public void setMergeViewBackgroundColor(int mergeViewBackgroundColor) {
        this.mergeViewBackgroundColor = mergeViewBackgroundColor;
    }

    /**
     * @return the color of Merge View Suggestion Title.
     */
    public int getMergeViewSuggestionTitleColor() {
        return mergeViewSuggestionTitleColor;
    }

    /**
     * @param mergeViewSuggestionTitleColor the color of merge view suggestion title.
     */
    public void setMergeViewSuggestionTitleColor(
            int mergeViewSuggestionTitleColor) {
        this.mergeViewSuggestionTitleColor = mergeViewSuggestionTitleColor;
    }

    /**
     * @return the color of merge view suggestion hint.
     */
    public int getMergeViewSuggestionHintColor() {
        return mergeViewSuggestionHintColor;
    }

    /**
     * @param mergeViewSuggestionHintColor the color of merge view suggestion hint.
     */
    public void setMergeViewSuggestionHintColor(int mergeViewSuggestionHintColor) {
        this.mergeViewSuggestionHintColor = mergeViewSuggestionHintColor;
    }

    /**
     * @return the color of merge view suggestion separator.
     */
    public int getMergeViewSuggestionSeparatorColor() {
        return mergeViewSuggestionSeparatorColor;
    }

    /**
     * @param mergeViewSuggestionSeparatorColor the color of merge view suggestion separator.
     */
    public void setMergeViewSuggestionSeparatorColor(
            int mergeViewSuggestionSeparatorColor) {
        this.mergeViewSuggestionSeparatorColor = mergeViewSuggestionSeparatorColor;
    }

    /**
     * @return the selector id of feed body.
     */
    public int getFeedBodySelector() {
        return feedBodySelector;
    }

    /**
     * @param feedBodySelector the selector id of feed body.
     */
    public void setFeedBodySelector(int feedBodySelector) {
        this.feedBodySelector = feedBodySelector;
    }

    /**
     * @return the selector id of unread feed body.
     */
    public int getFeedBodySelectorUnread() {
        return feedBodySelectorUnread;
    }

    /**
     * @param feedBodySelectorUnread the selector id of unread feed body.
     */
    public void setFeedBodySelectorUnread(int feedBodySelectorUnread) {
        this.feedBodySelectorUnread = feedBodySelectorUnread;
    }


    /**
     * @return selector id for feed message time ago.
     */
    public int getMessageTimeAgoSelector() {
        return messageTimeAgoSelector;
    }

    /**
     * @param messageTimeAgoSelector selector id for feed message time ago.
     */
    public void setMessageTimeAgoSelector(int messageTimeAgoSelector) {
        this.messageTimeAgoSelector = messageTimeAgoSelector;
    }

    public void setFeedLongPressBackground(int feedLongpressBackgroundColor) {
        this.feedLongpressBackgroundColor = feedLongpressBackgroundColor;
    }

    public int getFeedLongPressBackground() { return feedLongpressBackgroundColor; }
}
