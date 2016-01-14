package com.blinq.models.social.window;

/**
 * Extends the social window post to represent tweets posted by twitter application.
 *
 * @author Johan Hansson
 */
public class TwitterPost extends SocialWindowPost {

    private int retweetsCount;
    private int favoritesCount;

    public int getRetweetsCount() {
        return retweetsCount;
    }

    public void setRetweetsCount(int retweetsCount) {
        this.retweetsCount = retweetsCount;
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public void setFavoritesCount(int favoritesCount) {
        this.favoritesCount = favoritesCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TwitterPost tweet = (TwitterPost) o;

        if (favoritesCount != tweet.favoritesCount) return false;
        if (retweetsCount != tweet.retweetsCount) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + retweetsCount;
        result = 31 * result + favoritesCount;
        return result;
    }

    @Override
    public String toString() {
        return "Tweet{" +
                "retweetsCount=" + retweetsCount +
                ", favoritesCount=" + favoritesCount +
                '}';
    }
}
