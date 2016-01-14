package com.blinq.models.social.window;

/**
 * Extends the general SocialWindowPost model to add list of properties related to the instagram post.
 *
 * @author Johan Hansson
 */

public class InstagramPost extends SocialWindowPost {

    /**
     * Number of People who comment on this post.
     */
    private int commentsCount;

    /**
     * Number of People who like this post.
     */
    private int likesCount;

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        InstagramPost instagramPost = (InstagramPost) o;

        if (commentsCount != instagramPost.commentsCount) return false;
        if (likesCount != instagramPost.likesCount) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + commentsCount;
        result = 31 * result + likesCount;
        return result;
    }

    @Override
    public String toString() {
        return "Post{" +
                "commentsCount=" + commentsCount +
                ", likesCount=" + likesCount +
                '}';
    }
}
