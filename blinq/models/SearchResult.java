package com.blinq.models;

import java.util.Date;

/**
 * Holds data as a result of searching contact.
 * <ul>
 * <li>Contact details</li>
 * <li>The Feed related to the contact if exist</li>
 * <ul>
 *
 * @author Johan Hansson.
 */
public class SearchResult {

    /**
     * Indicates if the contact we want to search has feed or not.
     */
    private boolean hasFeed;
    /**
     * Id of the feed record
     */
    private String feedId;

    /**
     * Contains information about the contact related to the search result.
     */
    private Contact contact;

    /**
     * Define last message platform (CALL/SMS/FACEBOOK,etc..).
     */
    private Platform lastMessagePlatform;
    /**
     * Last Call Type (Incoming,Missed,Outgoing)
     */
    private MessageType lastCallType;
    /**
     * Has the message been read (1: read , 0 not read)
     */
    private boolean read;
    /**
     * Latest Message Date.
     */
    private Date lastMessageTime;

    /**
     * Phone Number if exist.
     */
    private String phoneNumber;
    /**
     * Indicates whether the contact has phone number.
     */
    private boolean hasPhoneNumber;

    /**
     * For search result ranking issues.
     */
    private Double rank;

    /**
     * Search result type: Local/Global.
     */
    public SearchType searchType;

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Platform getLastMessagePlatform() {
        return lastMessagePlatform;
    }

    public void setLastMessagePlatform(Platform lastMessagePlatform) {
        this.lastMessagePlatform = lastMessagePlatform;
    }

    public MessageType getLastCallType() {
        return lastCallType;
    }

    public void setLastCallType(MessageType lastCallType) {
        this.lastCallType = lastCallType;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean HasFeed() {
        return hasFeed;
    }

    public void setHasFeed(boolean hasFeed) {
        this.hasFeed = hasFeed;
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

    public Double getRank() {
        return rank;
    }

    public void setRank(Double rank) {
        this.rank = rank;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public enum SearchType {

        LOCAL(1), GLOBAL(2);

        private int id;

        private SearchType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((contact == null) ? 0 : contact.hashCode());
        result = prime * result + ((feedId == null) ? 0 : feedId.hashCode());
        result = prime * result + (hasFeed ? 1231 : 1237);
        result = prime * result + (hasPhoneNumber ? 1231 : 1237);
        result = prime * result
                + ((lastCallType == null) ? 0 : lastCallType.hashCode());
        result = prime
                * result
                + ((lastMessagePlatform == null) ? 0 : lastMessagePlatform
                .hashCode());
        result = prime * result
                + ((lastMessageTime == null) ? 0 : lastMessageTime.hashCode());
        result = prime * result
                + ((phoneNumber == null) ? 0 : phoneNumber.hashCode());
        result = prime * result + ((rank == null) ? 0 : rank.hashCode());
        result = prime * result + (read ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SearchResult))
            return false;
        SearchResult other = (SearchResult) obj;
        if (contact == null) {
            if (other.contact != null)
                return false;
        } else if (!contact.equals(other.contact))
            return false;
        if (feedId == null) {
            if (other.feedId != null)
                return false;
        } else if (!feedId.equals(other.feedId))
            return false;
        if (hasFeed != other.hasFeed)
            return false;
        if (hasPhoneNumber != other.hasPhoneNumber)
            return false;
        if (lastCallType != other.lastCallType)
            return false;
        if (lastMessagePlatform != other.lastMessagePlatform)
            return false;
        if (lastMessageTime == null) {
            if (other.lastMessageTime != null)
                return false;
        } else if (!lastMessageTime.equals(other.lastMessageTime))
            return false;
        if (phoneNumber == null) {
            if (other.phoneNumber != null)
                return false;
        } else if (!phoneNumber.equals(other.phoneNumber))
            return false;
        if (rank == null) {
            if (other.rank != null)
                return false;
        } else if (!rank.equals(other.rank))
            return false;
        if (read != other.read)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "hasFeed=" + hasFeed +
                ", feedId='" + feedId + '\'' +
                ", contact=" + contact +
                ", lastMessagePlatform=" + lastMessagePlatform +
                ", lastCallType=" + lastCallType +
                ", read=" + read +
                ", lastMessageTime=" + lastMessageTime +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", hasPhoneNumber=" + hasPhoneNumber +
                ", rank=" + rank +
                ", searchType=" + searchType +
                '}';
    }
}
