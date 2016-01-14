package com.blinq.authentication.impl.facebook.Mappers;

import android.net.Uri;
import android.os.Bundle;

import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.authentication.impl.facebook.FacebookUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Responsible to convert the response of "friends graph path"
 * initialized by {@link com.blinq.authentication.impl.facebook.Actions.FriendsAction} to
 * a headbox {@link com.blinq.models.Contact} model.
 */
public class ProfileMapper {

    private final GraphObject mGraphObject;
    private final Contact mContact;

    private ProfileMapper(GraphObject graphObject) {

        mGraphObject = graphObject;
        mContact = new Contact();

        // id
        mContact.setContactId(FacebookUtils.getPropertyString(mGraphObject,
                Properties.ID));
        // name
        mContact.setName(FacebookUtils.getPropertyString(mGraphObject,
                Properties.NAME));

        // first name
        mContact.setFirstName(FacebookUtils.getPropertyString(mGraphObject,
                Properties.FIRST_NAME));

        // last name
        mContact.setLastName(FacebookUtils.getPropertyString(mGraphObject,
                Properties.LAST_NAME));

        // link
        mContact.setLink(FacebookUtils.getPropertyString(mGraphObject,
                Properties.LINK));

        mContact.setEmail(FacebookUtils.getPropertyString(mGraphObject,
                Properties.EMAIL));
        // username
        mContact.setUserName(FacebookUtils.getPropertyString(mGraphObject,
                Properties.USER_NAME));

        mContact.setTimeZone(FacebookUtils.getPropertyInteger(mGraphObject,
                Properties.TIMEZONE));

        // cover
        GraphObject coverData = FacebookUtils.getPropertyGraphObject(
                mGraphObject, Properties.COVER);
        mContact.setCoverUri(Uri.parse(FacebookUtils.getPropertyString(
                coverData, Properties.COVER_SOURCE)));

        // picture
        GraphObject pictureData = FacebookUtils.getPropertyGraphObject(
                mGraphObject, Properties.PICTURE);

        mContact.setPhotoUri(Uri.parse(FacebookUtils.getPropertyInsideProperty(
                pictureData, "data", "url")));

        // type
        mContact.setContactType(Platform.FACEBOOK);
    }

    public ProfileMapper() {
        mGraphObject = null;
        mContact = null;
    }

    /**
     * Create new Contact based on {@link GraphUser} instance.
     *
     * @param graphObject The {@link GraphObject} instance
     * @return {@link Contact} of the user
     */
    public static Contact create(GraphObject graphObject) {
        return new ProfileMapper(graphObject).getContact();
    }

    /**
     * Return the graph object
     *
     * @return The graph object
     */
    public GraphObject getGraphObject() {
        return mGraphObject;
    }

    public static PictureAttributes getPictureAttributes() {
        return new ProfileMapper().new PictureAttributes();
    }

    /**
     * Return the headbox contact object
     *
     * @return The headbox contact object.
     */
    public Contact getContact() {
        return mContact;
    }

    /**
     * Profile properties.
     */
    public static class Properties {

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
         * User's friends API path.
         */
        public static final String FRIENDS_PATH = "me/friends";


        public static String FIELDS = "fields";

        /**
         * <b>Description:</b><br>
         * The user's Facebook ID<br>
         */
        public static final String ID = "id";

        /**
         * <b>Description:</b><br>
         * The user's full name<br>
         */
        public static final String NAME = "name";

        /**
         * <b>Description:</b><br>
         * The user's first name<br>
         */
        public static final String FIRST_NAME = "first_name";

        /**
         * <b>Description:</b><br>
         * The user's middle name<br>
         */
        public static final String MIDDLE_NAME = "middle_name";

        /**
         * <b>Description:</b><br>
         * The user's last name<br>
         */
        public static final String LAST_NAME = "last_name";

        /**
         * <b>Description:</b><br>
         * The URL of the profile for the user on Facebook<br>
         */
        public static final String LINK = "link";

        /**
         * <b>Description:</b><br>
         * The email address of the user<br>
         */
        public static final String EMAIL = "email";

        /**
         * <b>Description:</b><br>
         * The user's Facebook username<br>
         */
        public static final String USER_NAME = "username";

        /**
         * <b>Description:</b><br>
         * The user's timezone offset from UTC<br>
         */
        public static final String TIMEZONE = "timezone";

        /**
         * <b>Description:</b><br>
         * The user's cover photo<br>
         */
        public static final String COVER = "cover";
        public static final String COVER_SOURCE = "source";

        /**
         * <b>Description:</b><br>
         * The user's current city<br>
         */
        public static final String LOCATION = "location";

        /**
         * <b>Description:</b><br>
         * The user's profile pic<br>
         */
        public static final String PICTURE = "picture";


        public interface Search {

            /**
             * <b>Description:</b><br>
             * Search Graph API path<br>
             * <br>
             */
            public static final String PATH = "search";
            /**
             * <b>Description:</b><br>
             * Search type property<br>
             * <br>
             */
            public static final String TYPE = "type";
            /**
             * <b>Description:</b><br>
             * User's search type<br>
             * <br>
             */
            public static final String TYPE_USER = "user";
            /**
             * <b>Description:</b><br>
             * Query String property<br>
             * <br>
             */
            public static final String QUERY_STRING = "q";
            /**
             * <b>Description:</b><br>
             * Max results<br>
             * <br>
             */
            public static final String LIMIT = "limit";
            /**
             * <b>Description:</b><br>
             * Search offset<br>
             * <br>
             */
            public static final String OFFSET = "offset";
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
             * @param property For example: {@link Properties#FIRST_NAME}
             * @return {@link Builder}
             */
            public Builder add(String property) {
                properties.add(property);
                return this;
            }

            /**
             * Add property and attribute you need
             *
             * @param property   For example: {@link Properties#PICTURE}
             * @param attributes For example: picture can have type,width and height<br>
             * @return {@link Builder}
             */
            public Builder add(String property, PictureAttributes attributes) {
                Map<String, String> map = attributes.getAttributes();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(property);
                stringBuilder.append('.');
                stringBuilder.append(FacebookUtils.join(map, '.', '(', ')'));
                properties.add(stringBuilder.toString());
                return this;
            }

            public Properties build() {
                return new Properties(this);
            }

        }
    }

    public class PictureAttributes {

        private final static String HEIGHT = "height";
        private final static String WIDTH = "width";
        public static final int PICTURE_DEFAULT_WIDTH = 200;
        public static final int PICTURE_DEFAULT_HEIGHT = 200;
        private final static String TYPE = "type";

        protected Map<String, String> attributes = new HashMap<String, String>();

        PictureAttributes() {
        }

        public void setHeight(int pixels) {
            attributes.put(HEIGHT, String.valueOf(pixels));
        }

        public void setWidth(int pixels) {
            attributes.put(WIDTH, String.valueOf(pixels));
        }

        public void setType(PictureType type) {
            attributes.put(TYPE, type.getValue());
        }

        /**
         * Create picture attributes
         *
         * @return {@link PictureAttributes}
         */
        public PictureAttributes createPictureAttributes() {
            return new PictureAttributes();
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }
    }

    public enum PictureType {

        SMALL("small"), NORMAL("normal"), LARGE("large"), SQUARE("square");

        private String mValue;

        private PictureType(String value) {
            mValue = value;
        }

        public String getValue() {
            return mValue;
        }
    }

}
