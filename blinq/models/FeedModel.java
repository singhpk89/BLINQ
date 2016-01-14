package com.blinq.models;

import com.blinq.utils.StringUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Holds data related to the Feed item.
 *
 * @author Johan Hansson
 */
public class FeedModel {

    private int id;
    /**
     * Other contact associated with this feed.
     */
    private Contact contact;

    /**
     * List of platform's ids for this feed.
     */
    private List<Platform> platforms;

    /**
     * Contains the number of unread messages for each platform
     */
    private Map<Platform, Integer> unreadMessagesCount;


    /**
     * Feed creation date.
     */
    private Date addedDate;

    /**
     * Snippet text of the last message.
     */
    private String lastMessageText;

    /**
     * Define last message type (Incoming/Outgoing).
     */
    private MessageType lastMessageType;

    /**
     * Define last message platform (CALL/SMS/FACEBOOK,etc..).
     */
    private Platform lastMessagePlatform;
    /**
     * Latest Message Date.
     */
    private Date lastMessageTime;

    private Date updatedTime;

    /**
     * Last Call Type (Incoming,Missed,Outgoing)
     */
    private MessageType lastCallType;

    /**
     * Phone Number if exist.
     */
    private String phoneNumber;
    /**
     * Indicates whether the feed has phone number.
     */
    private boolean hasPhoneNumber;

    /**
     * Has the message been read (1: read , 0 not read)
     */
    private boolean read;
    /**
     * Indicates whether the feed has been updated.
     */
    private boolean modified;

    /**
     * Messages count
     */
    private int messagesCount;

    private boolean deleted;


    public int getFeedId() {
        return id;
    }

    public void setFeedId(int feed_id) {
        this.id = feed_id;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getLastMessageBody() {
        return lastMessageText;
    }

    public void setLastMessageBody(String Body) {
        this.lastMessageText = Body;
    }

    /**
     * @return List of platform ids associated with feed.
     */
    public List<Platform> getPlatforms() {
        return platforms;
    }

    /**
     * Set the List of platform ids associated with feed.
     */
    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    public Date getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Date date) {
        this.addedDate = date;
    }

    public MessageType getLastMessageType() {
        return lastMessageType;
    }

    public void setLastMessageType(MessageType direction) {
        this.lastMessageType = direction;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date messageTime) {
        this.lastMessageTime = messageTime;
    }

    public String getLastMessageTimeString() {
        return StringUtils.normalizeDifferenceDate(lastMessageTime);
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setAsDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public MessageType getLastCallType() {
        return lastCallType;
    }

    public void setLastCallType(MessageType lastCallType) {
        this.lastCallType = lastCallType;
    }

    public Platform getLastMessagePlatform() {
        return lastMessagePlatform;
    }

    public void setLastMessagePlatform(Platform platform) {
        this.lastMessagePlatform = platform;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean hasPhoneNumber() {
        return hasPhoneNumber;
    }

    public void setHasPhoneNumber(boolean hasPhoneNumber) {
        this.hasPhoneNumber = hasPhoneNumber;
    }

    public void setMessagesCount(int messagesCount) {
        this.messagesCount = messagesCount;
    }

    public int getMessagesCount() {
        return messagesCount;
    }

    public void setUnreadMessagesCount(Map<Platform, Integer> unreadMessagesCount) {
        this.unreadMessagesCount = unreadMessagesCount;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public Map<Platform, Integer> getUnreadMessagesCount() {
        return unreadMessagesCount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedModel feedModel = (FeedModel) o;

        if (deleted != feedModel.deleted) return false;
        if (hasPhoneNumber != feedModel.hasPhoneNumber) return false;
        if (id != feedModel.id) return false;
        if (messagesCount != feedModel.messagesCount) return false;
        if (modified != feedModel.modified) return false;
        if (read != feedModel.read) return false;
        if (addedDate != null ? !addedDate.equals(feedModel.addedDate) : feedModel.addedDate != null)
            return false;
        if (contact != null ? !contact.equals(feedModel.contact) : feedModel.contact != null)
            return false;
        if (lastCallType != feedModel.lastCallType) return false;
        if (lastMessagePlatform != feedModel.lastMessagePlatform) return false;
        if (lastMessageText != null ? !lastMessageText.equals(feedModel.lastMessageText) : feedModel.lastMessageText != null)
            return false;
        if (lastMessageTime != null ? !lastMessageTime.equals(feedModel.lastMessageTime) : feedModel.lastMessageTime != null)
            return false;
        if (lastMessageType != feedModel.lastMessageType) return false;
        if (phoneNumber != null ? !phoneNumber.equals(feedModel.phoneNumber) : feedModel.phoneNumber != null)
            return false;
        if (platforms != null ? !platforms.equals(feedModel.platforms) : feedModel.platforms != null)
            return false;
        if (unreadMessagesCount != null ? !unreadMessagesCount.equals(feedModel.unreadMessagesCount) : feedModel.unreadMessagesCount != null)
            return false;
        if (updatedTime != null ? !updatedTime.equals(feedModel.updatedTime) : feedModel.updatedTime != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (contact != null ? contact.hashCode() : 0);
        result = 31 * result + (platforms != null ? platforms.hashCode() : 0);
        result = 31 * result + (unreadMessagesCount != null ? unreadMessagesCount.hashCode() : 0);
        result = 31 * result + (addedDate != null ? addedDate.hashCode() : 0);
        result = 31 * result + (lastMessageText != null ? lastMessageText.hashCode() : 0);
        result = 31 * result + (lastMessageType != null ? lastMessageType.hashCode() : 0);
        result = 31 * result + (lastMessagePlatform != null ? lastMessagePlatform.hashCode() : 0);
        result = 31 * result + (lastMessageTime != null ? lastMessageTime.hashCode() : 0);
        result = 31 * result + (updatedTime != null ? updatedTime.hashCode() : 0);
        result = 31 * result + (lastCallType != null ? lastCallType.hashCode() : 0);
        result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0);
        result = 31 * result + (hasPhoneNumber ? 1 : 0);
        result = 31 * result + (read ? 1 : 0);
        result = 31 * result + (modified ? 1 : 0);
        result = 31 * result + messagesCount;
        result = 31 * result + (deleted ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FeedModel{" +
                "id=" + id +
                ", contact=" + contact +
                ", platforms=" + platforms +
                ", unreadMessagesCount=" + unreadMessagesCount +
                ", addedDate=" + addedDate +
                ", lastMessageText='" + lastMessageText + '\'' +
                ", lastMessageType=" + lastMessageType +
                ", lastMessagePlatform=" + lastMessagePlatform +
                ", lastMessageTime=" + lastMessageTime +
                ", updatedTime=" + updatedTime +
                ", lastCallType=" + lastCallType +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", hasPhoneNumber=" + hasPhoneNumber +
                ", read=" + read +
                ", modified=" + modified +
                ", messagesCount=" + messagesCount +
                ", deleted=" + deleted +
                '}';
    }

    public static Comparator<FeedModel> dateComparator = new Comparator<FeedModel>() {
        @Override
        public int compare(FeedModel o1, FeedModel o2) {
            return o2.lastMessageTime.toString().compareTo(
                    o1.lastMessageTime.toString());
        }
    };
}