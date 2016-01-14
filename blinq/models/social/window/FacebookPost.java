package com.blinq.models.social.window;

import com.blinq.authentication.impl.facebook.FacebookStatusType;
import com.blinq.authentication.impl.facebook.Mappers.PostsMapper;

import java.util.Date;
import java.util.List;

/**
 * Facebook post/status is an individual entry in a profile's feed.
 * The profile could be a user, page, app, or group.
 * Extends the SocialWindowPost model.
 *
 * @author Johan Hansson
 */
public class FacebookPost extends SocialWindowPost {

    /**
     * The ID of any uploaded photo or video attached to the post.
     */
    private String objectId;

    /**
     * Number of People who comment on this post.
     */
    private int commentsCount;

    /**
     * Number of People who like this post.
     */
    private int likesCount;

    /**
     * Represents the status type.
     */
    private FacebookStatusType statusType;

    private String story;
    private List<PostsMapper.StoryTag> taggedUsers;

    /**
     * Used for Event posts - the actual start time of the event
     */
    private Date startTime;


    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public void setStatusType(FacebookStatusType statusType) {
        this.statusType = statusType;
    }

    public FacebookStatusType getStatusType() {
        return statusType;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public String getStory() {
        return story;
    }

    public boolean isEventInFuture() {
        if(this.getStartTime() == null)
            return false;
        return(this.getStartTime().after(new Date()));
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setTaggedUsers(List<PostsMapper.StoryTag> taggedUsers) {
        this.taggedUsers = taggedUsers;
    }

    public boolean isEvent() {
        return startTime != null;
    }

    public List<PostsMapper.StoryTag> getTaggedUsers() {
        return taggedUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FacebookPost that = (FacebookPost) o;

        if (commentsCount != that.commentsCount) return false;
        if (likesCount != that.likesCount) return false;
        if (objectId != null ? !objectId.equals(that.objectId) : that.objectId != null)
            return false;
        if (statusType != that.statusType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (objectId != null ? objectId.hashCode() : 0);
        result = 31 * result + commentsCount;
        result = 31 * result + likesCount;
        result = 31 * result + (statusType != null ? statusType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FacebookPost{" +
                "objectId='" + objectId + '\'' +
                ", commentsCount=" + commentsCount +
                ", likesCount=" + likesCount +
                ", statusType=" + statusType +
                '}';
    }

}
