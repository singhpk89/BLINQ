package com.blinq.models;

import java.util.Date;

import com.blinq.utils.StringUtils;

/**
 * General model to hold messages from different platforms.
 * <p/>
 * Used when:
 * <ul>
 * <li>Querying for messages.
 * <li>Tracking delivery and sent notifications for messages.
 * </ul>
 *
 * @author Johan Hansson.
 */
public class HeadboxMessage {

    private Contact contact;

    private String id;
    /**
     * Feed Id if exist.
     */
    private long feedId;

    /**
     * Message ID on its platform.
     */
    private String sourceId;

    private String body;
    private MessageType type;
    private Platform platform;
    private Date date;
    private int read;
    /**
     * The duration of the call in seconds, for CALL type.
     */
    private long duration;

    /**
     * This constructor gets called when querying from data provider.
     */
    public HeadboxMessage(Contact contact, String messageId, String body,
                          MessageType type, Platform platform, Date date) {
        super();
        this.contact = contact;
        this.id = messageId;
        this.body = body;
        this.type = type;
        this.platform = platform;
        this.date = date;
    }

    /**
     * This constructor gets called when receiving or sending headbox message.
     */
    public HeadboxMessage(String sourceId, Contact contact, String body,
                          MessageType type, Platform platform, Date date) {
        super();
        this.sourceId = sourceId;
        this.contact = contact;
        this.body = body;
        this.type = type;
        this.platform = platform;
        this.date = date;
    }

    public HeadboxMessage(Contact contact, String body, MessageType type,
                          Platform platform, Date date) {
        super();
        this.contact = contact;
        this.body = body;
        this.type = type;
        this.platform = platform;
        this.date = date;
    }

    public HeadboxMessage() {
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getMessageId() {
        return id;
    }

    public void setMessageId(String messageId) {
        this.id = messageId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getNormalizedDate() {
        return StringUtils.normalizeDifferenceDate(date);
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public long getFeedId() {
        return feedId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        result = prime * result + ((contact == null) ? 0 : contact.hashCode());
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + (int) (feedId ^ (feedId >>> 32));
        result = prime * result
                + ((sourceId == null) ? 0 : sourceId.hashCode());
        result = prime * result
                + ((platform == null) ? 0 : platform.hashCode());
        result = prime * result + read;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HeadboxMessage other = (HeadboxMessage) obj;
        if (body == null) {
            if (other.body != null)
                return false;
        } else if (!body.equals(other.body))
            return false;
        if (contact == null) {
            if (other.contact != null)
                return false;
        } else if (!contact.equals(other.contact))
            return false;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (feedId != other.feedId)
            return false;
        if (sourceId == null) {
            if (other.sourceId != null)
                return false;
        } else if (!sourceId.equals(other.sourceId))
            return false;
        if (platform != other.platform)
            return false;
        if (read != other.read)
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HeadboxMessage [contact=" + contact + ", id=" + id
                + ", feedId=" + feedId + ", sourceId=" + sourceId + ", body="
                + body + ", type=" + type + ", platform=" + platform
                + ", date=" + date + ", read=" + read + "]";
    }

}