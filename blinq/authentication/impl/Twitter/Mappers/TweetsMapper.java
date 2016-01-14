package com.blinq.authentication.impl.Twitter.Mappers;

import com.blinq.authentication.settings.TwitterSettings;
import com.blinq.models.Location;
import com.blinq.models.Platform;
import com.blinq.models.social.window.PostTypeTag;
import com.blinq.models.social.window.StatusContent;
import com.blinq.models.social.window.TwitterPost;

import twitter4j.MediaEntity;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.URLEntity;

/**
 * Created by Johan Hansson on 9/18/2014.
 */
public class TweetsMapper {


    private static final String TAG = TweetsMapper.class.getSimpleName();
    private final Status status;
    private final TwitterPost tweet;

    private TweetsMapper(Status status) {

        this.status = status;
        tweet = convertStatus(status);
    }

    public TweetsMapper() {
        status = null;
        tweet = null;
    }

    /**
     * Create tweet based on {@link twitter4j.Status} instance.
     *
     * @param status The {@link Status} instance
     * @return {@link com.blinq.models.social.window.TwitterPost} model.
     */
    public static TwitterPost create(Status status) {
        return new TweetsMapper(status).getTweet();
    }

    /**
     * Convert  the twitter's status object
     * to a headbox social window - {@link com.blinq.models.social.window.TwitterPost} object.
     *
     * @param status - {@link twitter4j.Status} object
     */
    private TwitterPost convertStatus(
            Status status) {

        TwitterPost tweet = new TwitterPost();

        tweet.setId(String.valueOf(status.getId()));

        if (!status.getText().isEmpty()) {

            tweet.setStatusBody(status.getText());
            tweet.setHasMessage(true);
        }

        if (status.getMediaEntities() != null && status.getMediaEntities().length > 0) {

            for (MediaEntity entity : status.getMediaEntities()) {

                if (entity.getType().equals(TwitterSettings.TWITTER_MEDIA_PHOTO_TYPE)) {
                    tweet.setPictureUrl(entity.getMediaURL());
                    tweet.setContentType(StatusContent.PHOTO);
                    tweet.setHasPicture(true);
                    tweet.setLink(entity.getExpandedURL());
                    tweet.setHasLink(true);

                    break;
                }
            }
        }

        int urlEntityIndex = 0;
        if (status.getURLEntities() != null && status.getURLEntities().length > 0) {

            URLEntity urlEntity = status.getURLEntities()[urlEntityIndex];
            tweet.setLink(urlEntity.getURL());
            tweet.setHasLink(true);
        }

        try {
            if (status.getRetweetedStatus() != null && status.getRetweetedStatus().getUser() != null) {
                tweet.setUser(UsersMapper.create(status.getRetweetedStatus().getUser()));
            }
        } catch (Exception e) {
        }

        tweet.setRetweetsCount(status.getRetweetCount());
        tweet.setFavoritesCount(status.getFavoriteCount());
        tweet.setPublishTime(status.getCreatedAt());
        tweet.setCoverPageStatusPlatform(Platform.TWITTER);

        setLocation(status.getPlace());

        if (status.isRetweet()) {
            tweet.setTag(PostTypeTag.RETWEET);
        }

        return tweet;
    }

    private void setLocation(Place place) {

        if (place == null)
            return;

        Location location = new Location(place.getId(),
                null, place.getStreetAddress(), 0, 0, null, place.getFullName());
    }

    /**
     * Return a headbox tweet object
     */
    public TwitterPost getTweet() {
        return tweet;
    }

}
