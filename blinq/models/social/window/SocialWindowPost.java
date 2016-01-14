package com.blinq.models.social.window;

import com.blinq.models.Contact;
import com.blinq.models.Location;
import com.blinq.models.Platform;
import com.blinq.utils.StringUtils;

import java.util.Date;

/**
 * Holds cover page and status for each user account.
 *
 * @author Johan Hansson.
 */
public class SocialWindowPost implements SocialWindowItem {

    /**
     * The post id.
     */
    private String id;

    /**
     * The message written in the post
     */
    private String statusBody;

    /**
     * The time the post was published, expressed as UNIX timestamp
     */
    private String createdDate;

    private Date publishTime;
    /**
     * Picture url.
     */
    private String pictureUrl;
    /**
     * Status
     */
    private Platform coverPageStatusPlatform;

    /**
     * Indicates whether the post has photo or not.
     */
    private boolean hasPicture;
    /**
     * Indicates whether the post has message.
     */
    private boolean hasMessage;
    /**
     * The URL of the post
     */
    private String link;

    private boolean hasLink;

    /**
     * A URL to a Flash movie or video file to be embedded within the post.
     */
    private String source;

    /**
     * Post location.
     */
    private Location location;

    private boolean hasLocation;

    /**
     * Friend,mutual friend name.
     */
    private String entityName;

    private Contact user;

    /**
     * Represents the status content:link,photo,etc..
     */
    private StatusContent type;

    private PostTypeTag tag;

    public SocialWindowPost() {
        super();
    }

    /**
     * @param link                    The URL in the post.
     * @param coverPageStatusPlatform the platform type of cover page and status.
     * @param statusBody              the status body for specific platform.
     */

    public SocialWindowPost(String link,
                            Platform coverPageStatusPlatform, String statusBody) {
        super();
        this.link = link;
        if (!StringUtils.isBlank(link))
            setHasLink(true);

        this.coverPageStatusPlatform = coverPageStatusPlatform;
        this.statusBody = statusBody;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The URL for user cover page.
     */
    public String getPictureUrl() {
        return pictureUrl;
    }

    /**
     * @param pictureUrl URL for user cover page.
     */
    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    /**
     * @return The platform type of cover page and status.
     */
    public Platform getCoverPageStatusPlatform() {
        return coverPageStatusPlatform;
    }

    /**
     * @param coverPageStatusPlatform platform type of cover page and status.
     */
    public void setCoverPageStatusPlatform(
            Platform coverPageStatusPlatform) {
        this.coverPageStatusPlatform = coverPageStatusPlatform;
    }

    /**
     * @return The status body for specific platform.
     */
    public String getStatusBody() {
        return statusBody;
    }

    /**
     * @param statusBody status body for specific platform.
     */
    public void setStatusBody(String statusBody) {
        this.statusBody = statusBody;
    }

    public String getCreatedDate() {
        return StringUtils.normalizeDifferenceDate(publishTime);
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public boolean hasPicture() {
        return hasPicture;
    }

    public void setHasPicture(boolean hasPicture) {
        this.hasPicture = hasPicture;
    }

    public boolean hasMessage() {
        return hasMessage;
    }

    public void setHasMessage(boolean hasMessage) {
        this.hasMessage = hasMessage;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean HasLink() {
        return hasLink;
    }

    public void setHasLink(boolean hasLink) {
        this.hasLink = hasLink;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setHasLocation(boolean hasLocation) {
        this.hasLocation = hasLocation;
    }

    public boolean hasLocation() {
        return hasLocation;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public StatusContent getContentType() {
        return type;
    }

    public void setContentType(StatusContent type) {
        this.type = type;
    }

    public Contact getUser() {
        return user;
    }

    public void setUser(Contact user) {
        this.user = user;
    }

    public void setTag(PostTypeTag tag) {
        this.tag = tag;
    }

    public PostTypeTag getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocialWindowPost that = (SocialWindowPost) o;

        if (hasLink != that.hasLink) return false;
        if (hasMessage != that.hasMessage) return false;
        if (hasPicture != that.hasPicture) return false;
        if (coverPageStatusPlatform != that.coverPageStatusPlatform) return false;
        if (createdDate != null ? !createdDate.equals(that.createdDate) : that.createdDate != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (link != null ? !link.equals(that.link) : that.link != null) return false;
        if (pictureUrl != null ? !pictureUrl.equals(that.pictureUrl) : that.pictureUrl != null)
            return false;
        if (location != null ? !location.equals(that.location) : that.location != null)
            return false;
        if (statusBody != null ? !statusBody.equals(that.statusBody) : that.statusBody != null)
            return false;

        return true;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityName() {
        return entityName;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (statusBody != null ? statusBody.hashCode() : 0);
        result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
        result = 31 * result + (pictureUrl != null ? pictureUrl.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (coverPageStatusPlatform != null ? coverPageStatusPlatform.hashCode() : 0);
        result = 31 * result + (hasPicture ? 1 : 0);
        result = 31 * result + (hasMessage ? 1 : 0);
        result = 31 * result + (link != null ? link.hashCode() : 0);
        result = 31 * result + (hasLink ? 1 : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SocialWindowPost{" +
                "id='" + id + '\'' +
                ", statusBody='" + statusBody + '\'' +
                ", createdDate='" + createdDate + '\'' +
                ", pictureUrl='" + pictureUrl + '\'' +
                ", coverPageStatusPlatform=" + coverPageStatusPlatform +
                ", hasPicture=" + hasPicture +
                ", hasMessage=" + hasMessage +
                ", link='" + link + '\'' +
                ", hasLink=" + hasLink +
                ", place=" + location +
                ", hasLocation=" + hasLocation +
                ", source=" + source +
                '}';
    }


    @Override
    public SocialItemType getItemType() {
        return SocialItemType.POST;
    }
}
