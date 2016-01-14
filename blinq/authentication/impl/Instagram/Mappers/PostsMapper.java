package com.blinq.authentication.impl.Instagram.Mappers;

import com.blinq.models.Location;
import com.blinq.models.Platform;
import com.blinq.models.social.window.InstagramPost;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.authentication.settings.InstagramSettings;
import com.blinq.utils.JsonUtils;
import com.blinq.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Johan Hansson on 10/9/2014.
 */
public class PostsMapper {

    /**
     * Map the JSON Instagram post to Headbox social window post.
     *
     * @param object
     * @throws org.json.JSONException
     */
    public static SocialWindowPost create(JSONObject object) throws JSONException {

        if (object == null)
            return null;

        InstagramPost instagramPost = new InstagramPost();

        String id = JsonUtils.getPropertyString(object, Properties.INSTAGRAM_ID);
        String link = JsonUtils.getPropertyString(object, Properties.INSTAGRAM_LINK);
        String message = JsonUtils.getPropertyStringInsideProperty(object, Properties.INSTAGRAM_CAPTION, Properties.INSTAGRAM_TEXT);
        long time = JsonUtils.getPropertyLong(object, Properties.INSTAGRAM_CREATED_TIME);
        int likes = JsonUtils.getPropertyIntegerInsideProperty(object, Properties.INSTAGRAM_LIKES, InstagramSettings.INSTAGRAM_COUNT);
        int comments = JsonUtils.getPropertyIntegerInsideProperty(object, Properties.INSTAGRAM_COMMENTS, InstagramSettings.INSTAGRAM_COUNT);

        instagramPost.setId(id);

        instagramPost.setLink(link);
        if (!org.apache.commons.lang3.StringUtils.isBlank(link))
            instagramPost.setHasLink(true);

        instagramPost.setStatusBody(message);

        if (!org.apache.commons.lang3.StringUtils.isBlank(message))
            instagramPost.setHasMessage(true);

        instagramPost.setPublishTime(new Date(time * 1000));
        instagramPost.setCommentsCount(comments);
        instagramPost.setLikesCount(likes);


        if (!object.isNull(Properties.INSTAGRAM_IMAGES)) {

            JSONObject images = object.getJSONObject(Properties.INSTAGRAM_IMAGES);

            if (!images.isNull(Properties.INSTAGRAM_STANDARD_RESOLUTION)) {

                JSONObject standardResolution = images.getJSONObject(Properties.INSTAGRAM_STANDARD_RESOLUTION);
                instagramPost.setPictureUrl(standardResolution.getString(Properties.INSTAGRAM_URL));
                instagramPost.setHasPicture(true);
            }
        }

        if (!object.isNull(Properties.INSTAGRAM_LOCATION)) {

            JSONObject locationObject = object.getJSONObject(Properties.INSTAGRAM_LOCATION);

            String lid = JsonUtils.getPropertyString(locationObject, Properties.INSTAGRAM_LOCATION_ID);
            String name = JsonUtils.getPropertyString(locationObject, Properties.INSTAGRAM_LOCATION_NAME);
            double latitude = JsonUtils.getPropertyDouble(locationObject, Properties.INSTAGRAM_LOCATION_LATITUDE);
            double longitude = JsonUtils.getPropertyDouble(locationObject, Properties.INSTAGRAM_LOCATION_LONGITUDE);

            Location location = new Location(lid, null, null, longitude, latitude, null, name);

            instagramPost.setLocation(location);
            if (name.equals(StringUtils.EMPTY_STRING)) {
                instagramPost.setHasLocation(false);
            }
        }

        instagramPost.setCoverPageStatusPlatform(Platform.INSTAGRAM);

        return instagramPost;

    }

    public static class Properties {

        public static final String INSTAGRAM_ID = "id";
        public static final String INSTAGRAM_TEXT = "text";
        public static final String INSTAGRAM_LINK = "link";
        public static final String INSTAGRAM_IMAGES = "images";
        public static final String INSTAGRAM_STANDARD_RESOLUTION = "standard_resolution";
        public static final String INSTAGRAM_URL = "url";
        public static final String INSTAGRAM_CREATED_TIME = "created_time";
        public static final String INSTAGRAM_COMMENTS = "comments";
        public static final String INSTAGRAM_LIKES = "likes";
        public static final String INSTAGRAM_CAPTION = "caption";

        public static final String INSTAGRAM_LOCATION = "location";
        public static final String INSTAGRAM_LOCATION_ID = "id";
        public static final String INSTAGRAM_LOCATION_NAME = "name";
        public static final String INSTAGRAM_LOCATION_LATITUDE = "latitude";
        public static final String INSTAGRAM_LOCATION_LONGITUDE = "longitude";
    }

}
