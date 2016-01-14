package com.blinq.authentication.impl.facebook.Mappers;

import android.os.Bundle;

import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.models.Location;
import com.blinq.models.Platform;
import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.PostTypeTag;
import com.blinq.utils.StringUtils;
import com.facebook.model.GraphObject;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible to convert the response of the events graph path
 * initialized by {@link com.blinq.authentication.impl.facebook.Actions.EventsAction} to
 * a headbox {@link com.blinq.models.social.window.SocialWindowPost} post of an event content type..
 */
public class EventsMapper {

    private static final String TAG = PostsMapper.class.getSimpleName();

    private final FacebookPost event;

    private String EVENT_URL = "https://www.facebook.com/events/%s";

    private EventsMapper(GraphObject graphObject) {
        Date now = new Date();
        event = new FacebookPost();

        event.setId(FacebookUtils.getPropertyString(graphObject, Properties.ID));

        FacebookUtils utils = new FacebookUtils();
        Date date = utils.getDateFromString(FacebookUtils.getPropertyString(graphObject, Properties.UPDATED_TIME));
        event.setPublishTime(date);

        if (!StringUtils.isBlank(event.getId())) {
            event.setLink(String.format(EVENT_URL, event.getId()));
            event.setHasLink(true);
        }

        String eventName = FacebookUtils.getPropertyString(graphObject, Properties.NAME);
        String description = FacebookUtils.getPropertyString(graphObject, Properties.DESCRIPTION);

        /* TODO: I think we need to design a new card for events.
        as we have more details like the event's start time,end time and description
        we should fill on the social window. */

        String body = eventName;
        Date startDate = utils.getDateFromString(FacebookUtils.getPropertyString(graphObject, Properties.START_TIME));

        if(startDate == null) {
            event.setStartTime(event.getPublishTime());
        }else {
            event.setStartTime(startDate);
            if (startDate.after(now)) {
                String days = StringUtils.normalizeDifferenceDateFuture(startDate);
                body = "Going in " + days;
            } else if (startDate.before(now)) {
                String days = StringUtils.normalizeDifferenceDate(startDate);
                body = "Was " + days + " ago";
            } else {
                body = "Is now";
            }
        }
        event.setStatusBody(body);
        event.setHasMessage(true);

        //first check if we have a cover for this event then get image source.
        //if not get the picture property.
        GraphObject coverData = FacebookUtils.getPropertyGraphObject(graphObject, Properties.COVER);
        if (coverData != null) {

            String objectId = FacebookUtils.getPropertyString(coverData, Properties.COVER_ID);
            String picture = FacebookUtils.getPropertyString(coverData, Properties.COVER_SOURCE);
            event.setObjectId(objectId);
            event.setPictureUrl(picture);

        } else {

            GraphObject pictureData = FacebookUtils.getPropertyGraphObject(graphObject, Properties.PICTURE);
            String picture = FacebookUtils.getPropertyInsideProperty(pictureData, "data", "url");
            event.setPictureUrl(picture);
        }

        if (!StringUtils.isBlank(event.getPictureUrl())) {
            event.setHasPicture(true);
        }


        String locationName = FacebookUtils.getPropertyString(graphObject, Properties.LOCATION);
        Location location = new Location();
        location.setName(locationName);
        location.setCity(locationName);

        event.setLocation(location);
        event.setStory(body);
        event.setStatusBody("<b>" + eventName + "</b>");
        event.setHasLocation(true);
        event.setTag(PostTypeTag.EVENT);
        event.setCoverPageStatusPlatform(Platform.FACEBOOK);
    }

    public static FacebookPost create(GraphObject graphObject) {
        return new EventsMapper(graphObject).getEvent();
    }

    public static class Properties {

        /**
         * Graph API Path.
         */
        public static final String EVENTS_PATH = "events";
        public static final String FIELDS = "fields";

        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String END_TIME = "end_time";
        public static final String LOCATION = "location";
        public static final String OWNER = "owner";
        public static final String PICTURE = "picture";
        public static final String COVER = "cover";
        public static final String COVER_ID = "cover_id";
        public static final String COVER_SOURCE = "source";
        public static final String START_TIME = "start_time";
        public static final String UPDATED_TIME = "updated_time";

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
             * @param property
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

    /**
     * get converted event to social window event.
     */
    public FacebookPost getEvent() {
        return event;
    }

}
