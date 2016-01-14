package com.blinq.authentication.impl.facebook.Mappers;

import android.os.Bundle;
import android.util.Log;

import com.blinq.authentication.impl.facebook.FacebookStatusType;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.models.Location;
import com.blinq.models.Platform;
import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.PostTypeTag;
import com.blinq.models.social.window.StatusContent;
import com.facebook.model.GraphObject;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible to convert the response of the posts graph path
 * initialized by {@link com.blinq.authentication.impl.facebook.Actions.PostsAction} to
 * a headbox {@link com.blinq.models.social.window.SocialWindowPost} model.
 */
public class PostsMapper {

    private static final String TAG = PostsMapper.class.getSimpleName();

    private final FacebookPost status;

    private String entityName;
    private final String story;

    private PostsMapper(GraphObject graphObject, String entityName) {

        status = new FacebookPost();
        this.entityName = entityName;

        status.setId(FacebookUtils.getPropertyString(graphObject, Properties.ID));
        status.setObjectId(FacebookUtils.getPropertyString(graphObject, Properties.OBJECT_ID));

        FacebookUtils utils = new FacebookUtils();
        Date date = utils.getDateFromString(FacebookUtils.getPropertyString(graphObject, Properties.CREATED_TIME));
        status.setPublishTime(date);

        status.setLink(FacebookUtils.getPropertyString(graphObject, Properties.LINK));
        if (!StringUtils.isBlank(status.getLink())) {
            status.setHasLink(true);
        }

        status.setSource(FacebookUtils.getPropertyString(graphObject, Properties.SOURCE));

        String message = FacebookUtils.getPropertyString(graphObject, Properties.MESSAGE);
        story = FacebookUtils.getPropertyString(graphObject, Properties.STORY);
        String description = FacebookUtils.getPropertyString(graphObject, Properties.DESCRIPTION);

        FacebookStatusType type = FacebookStatusType.fromName(FacebookUtils.getPropertyString(graphObject, Properties.STATUS_TYPE));
        status.setStatusType(type);

        StatusContent content = StatusContent.fromName(FacebookUtils.getPropertyString(graphObject, Properties.TYPE));
        status.setContentType(content);

        if (!StringUtils.isBlank(message)) {
            status.setStatusBody(message);
        } else if (!StringUtils.isBlank(description)) {
            status.setStatusBody(description);
        }
        if (!StringUtils.isBlank(status.getStatusBody())) {
            status.setHasMessage(true);
        }

        int likesCount = FacebookUtils.getGraphObjectsCount(graphObject, Properties.LIKES, "data");
        int commentsCount = FacebookUtils.getGraphObjectsCount(graphObject, Properties.COMMENTS, "data");
        status.setLikesCount(likesCount);
        status.setCommentsCount(commentsCount);

        String picture = FacebookUtils.getPropertyString(graphObject, Properties.FULL_PICTURE);
        status.setPictureUrl(picture);
        if (!StringUtils.isBlank(status.getPictureUrl())) {
            status.setHasPicture(true);
        }

        try {
            setLocation(graphObject);
        } catch (Exception e) {
            Log.d(TAG, "Error while converting the location object.");
        }
        switch (content) {
            case PHOTO:
                handlePhoto();
                break;
            case VIDEO:
                handleVideo();
                break;
            case STATUS:
                handleStatus();
                break;
            case LINK:
                handleLink();
                break;
        }

        if (status.getStatusType() == FacebookStatusType.TAGGED_IN_PHOTO) {
            List<StoryTag> mStoryTags;
            mStoryTags = FacebookUtils.createStoryTagsList(graphObject, Properties.STORY_TAGS, new FacebookUtils.Converter() {
                @Override
                public StoryTag convert(GraphObject graphObject) {
                    return StoryTag.create(graphObject);
                }
            });
            status.setTaggedUsers(mStoryTags);
        }

        if (status.getStory() == null) {
            status.setStory("");
        }
        if (status.getStatusBody() == null) {
            status.setStatusBody("");
        }
        status.setCoverPageStatusPlatform(Platform.FACEBOOK);
    }

    private void handlePhoto() {
        FacebookStatusType type = status.getStatusType();
        String other;
        switch (type) {
            case ADDED_PHOTOS:
                status.setTag(PostTypeTag.POSTED_IMAGE_VIDEO);
                status.setStory("<b>" + entityName + "</b> posted a photo");
                return;
            case SHARED_STORY:
                status.setTag(PostTypeTag.SHARED_IMAGE_VIDEO);
                other = getPhotoFriendName(story, "shared");
                status.setStory("<b>" + entityName + "</b> shared <b>" + other + "</b> photo");
                return;
            case TAGGED_IN_PHOTO:
                other = getPhotoFriendName(story, "in");
                if ("his".equals(other) || "her".equals(other) ||
                        story.contains("himself") || story.contains("herself")) {
                    status.setTag(PostTypeTag.SELF_TAG);
                } else {
                    status.setTag(PostTypeTag.TAGGED_IN);
                }
                status.setStory("<b>" + entityName + "</b> was tagged in <b>" + other + "</b> photo");
                return;
            case NOTHING:
                if (story.contains("profile picture")) {
                    status.setTag(PostTypeTag.CHANGED_PROFILE_PICTURE);
                    entityName = story.substring(0, story.indexOf("changed"));
                    status.setStory("<b>" + entityName + "</b> " + story.substring(story.indexOf("changed")));
                } else if (story.contains("cover photo")) {
                    status.setTag(PostTypeTag.CHANGED_COVER_PICTURE);
                    entityName = story.substring(0, story.indexOf("updated"));
                    status.setStory("<b>" + entityName + "</b> " + story.substring(story.indexOf("updated")));
                }
        }
    }

    private String getPhotoFriendName(String story, String start) {
        String name = StringUtils.substringBetween(story, start, "photo");
        if (name != null) {
            return name;
        }
        if (story.contains("your")) {
            return "your";
        }
        return "a";
    }


    private void handleVideo() {
        FacebookStatusType type = status.getStatusType();
        switch (type) {
            case ADDED_VIDEO:
                status.setTag(PostTypeTag.POSTED_IMAGE_VIDEO);
                status.setStory("<b>" + entityName + "</b> posted a video");
                return;
            case SHARED_STORY:
                status.setTag(PostTypeTag.SHARED_IMAGE_VIDEO);
                status.setStory("<b>" + entityName + "</b> shared a video");
                return;
        }
    }

    private void handleStatus() {
        FacebookStatusType type = status.getStatusType();
        switch (type) {
            case WALL_POST:
                return;
            case MOBILE_STATUS_UPDATE:
                status.setTag(PostTypeTag.STATUS_POST);
                status.setStory("");
                return;
            case NOTHING:
                if (story.contains("commented")) {
                    status.setTag(PostTypeTag.COMMENTED_ON);
                } else if (story.contains("likes")) {
                    status.setTag(PostTypeTag.LIKES_A);
                } else if (story.contains("posted")) {
                    status.setTag(PostTypeTag.POSTED_ON_OTHER);
                }
        }
    }

    private void handleLink() {
        String other;
        if (story.contains("likes")) {
            entityName = story.substring(0, story.indexOf("likes"));
            other = StringUtils.substringBetween(story, "likes", ".");
            status.setStory("<b>" + entityName + "</b> likes <b>" + other + "</b>");
            status.setTag(PostTypeTag.LIKES_LINK);
        }

    }

    /**
     * Maps graph api location object to headbox location..
     *
     * @param graphObject
     */
    private void setLocation(GraphObject graphObject) {

        GraphObject placeObject = FacebookUtils.getPropertyGraphObject(graphObject, Properties.PLACE);

        if (graphObject == null || placeObject == null)
            return;

        Location location = null;

        String id = FacebookUtils.getPropertyString(placeObject, Properties.Place.ID);
        String name = FacebookUtils.getPropertyString(placeObject, Properties.Place.NAME);

        GraphObject locationObj = FacebookUtils.getPropertyGraphObject(placeObject, Properties.Place.LOCATION);
        String street = FacebookUtils.getPropertyString(locationObj, Properties.Place.STREET);
        String city = FacebookUtils.getPropertyString(locationObj, Properties.Place.CITY);
        String state = FacebookUtils.getPropertyString(locationObj, Properties.Place.STATE);
        String country = FacebookUtils.getPropertyString(locationObj, Properties.Place.COUNTRY);
        double latitude = FacebookUtils.getPropertyDouble(locationObj, Properties.Place.LATITUDE);
        double longitude = FacebookUtils.getPropertyDouble(locationObj, Properties.Place.LONGITUDE);

        location = new Location(id, city, street, longitude, latitude, state, name);
        status.setLocation(location);
        status.setHasLocation(true);
    }

    public static FacebookPost create(GraphObject graphObject, String entityName) {
        return new PostsMapper(graphObject, entityName).getStatus();
    }

    public static class Properties {

        /**
         * Graph API Path.
         */
        public static final String POSTS_PATH = "posts";
        public static final String FIELDS = "fields";
        public static final String MY_POSTS = "me/posts";
        public static final String MUTUAL_FRIENDS_PATH = "me/mutualfriends/";

        /**
         * Properties
         */

        public static final String ID = "id";

        /**
         * The Facebook object id for an uploaded photo or video.
         */
        public static final String OBJECT_ID = "object_id";
        /**
         * The time the status was initially published.
         */
        public static final String CREATED_TIME = "created_time";

        /**
         * A description of the link (appears beneath the link caption).
         */
        public static final String DESCRIPTION = "description";
        /**
         * The link attached to this status.
         */
        public static final String LINK = "link";
        /**
         * The message.
         */
        public static final String MESSAGE = "message";
        /**
         * The name of the link.
         */
        public static final String NAME = "name";
        /**
         * If available, a link to the picture included with this status.
         */
        public static final String PICTURE = "picture";
        public static final String FULL_PICTURE = "full_picture";
        /**
         * A URL to a Flash movie or video file to be embedded within the status.
         */
        public static final String SOURCE = "source";
        /**
         * The caption of the link (appears beneath the link name).
         */
        public static final String CAPTION = "caption";

        public static final String STATUS_TYPE = "status_type";
        /**
         * A string indicating the type for this status (including link, photo,
         * video).
         */
        public static final String TYPE = "type";

        public static final String DATA = "data";
        /**
         * Information about the user who posted the message.
         */
        public static final String IMAGES = "images";
        public static final String FROM = "from";
        public static final String STORY = "story";
        public static final String STORY_TAGS = "story_tags";
        public static final String COMMENTS = "comments";
        public static final String LIKES = "likes";
        public static final String PLACE = "place";

        public interface Place {

            public static final String ID = "id";
            public static final String NAME = "name";
            public static final String LOCATION = "location";
            public static final String STREET = "street";
            public static final String CITY = "city";
            public static final String STATE = "state";
            public static final String COUNTRY = "country";
            public static final String ZIP = "zip";
            public static final String LATITUDE = "latitude";
            public static final String LONGITUDE = "longitude";

        }

        private final Bundle mBundle;

        private Properties(Builder builder) {

            mBundle = new Bundle();
            Iterator<String> iterator = builder.properties.iterator();
            String fields = FacebookUtils.join(iterator, ",");
            mBundle.putString(Properties.FIELDS, fields);
        }

        public Bundle getBundle() {
            return mBundle;
        }

        /**
         * Properties and attributes builder.
         */
        public static class Builder {

            Set<String> properties;

            public Builder() {
                properties = new HashSet<String>();
            }

            /**
             * Add property you need
             *
             * @param property For example: {@link Properties#COMMENTS}
             * @return {@link Builder}
             */
            public Builder add(String property) {
                properties.add(property);
                return this;
            }

            /**
             * Add property and attribute you need
             *
             * @param property
             * @param map
             * @return {@link Builder}
             */
            public Builder add(String property, Map<String, String> map) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(property);
                stringBuilder.append('.');
                stringBuilder.append(FacebookUtils.join(map, '.', '(', ')'));
                properties.add(stringBuilder.toString());
                return this;
            }

            public Builder add(String property, String fields, List<String> map) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(property);
                stringBuilder.append('.');
                stringBuilder.append(fields);
                stringBuilder.append('(');
                stringBuilder.append(FacebookUtils.join(map.iterator(), ","));
                stringBuilder.append(')');
                properties.add(stringBuilder.toString());
                return this;
            }

            public Properties build() {
                return new Properties(this);
            }

        }

    }

    public static class StoryTag {

        private String mId;
        private String mName;

        private StoryTag(GraphObject graphObject) {
            mId = FacebookUtils.getPropertyString(graphObject, ProfileMapper.Properties.ID);
            mName = FacebookUtils.getPropertyString(graphObject, ProfileMapper.Properties.NAME);
        }

        public static StoryTag create(GraphObject graphObject) {
            return new StoryTag(graphObject);
        }

        @Override
        public String toString() {
            return "InlineTag{" +
                    "mId='" + mId + '\'' +
                    ", mName='" + mName + '\'' +
                    '}';
        }

        public String getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }

    }
    /**
     * get converted status to social window status.
     */
    public FacebookPost getStatus() {
        return status;
    }

}
