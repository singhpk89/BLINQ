package com.blinq.authentication.impl.facebook;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import com.facebook.Response;
import com.facebook.model.GraphMultiResult;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;
import com.blinq.authentication.impl.facebook.Mappers.EventsMapper;
import com.blinq.authentication.impl.facebook.Mappers.InboxMapper;
import com.blinq.authentication.impl.facebook.Mappers.PostsMapper;
import com.blinq.authentication.impl.facebook.Mappers.ProfileMapper;
import com.blinq.authentication.impl.facebook.Mappers.ProfileMapper.PictureAttributes;
import com.blinq.authentication.impl.facebook.Mappers.ProfileMapper.PictureType;
import com.blinq.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Provide utilities to process,manipulate,and to map facebook response for
 * different requests.
 */
@SuppressLint("NewApi")
public class FacebookUtils {

    /**
     * Facebook Date format encapsulated within the JSONObject
     * Facebook date format (ISO 8601). Example: 2010-02-28T16:11:08+0000
     */
    public static final String FACEBOOK_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    /**
     * Time notion of facebook date field into JSONObject.
     */
    public static final String FACEBOOK_TIME = "T";

    /**
     * length of the milli seconds of the time variable.
     */
    private static final int FACEBOOK_MS_TIME_LENGTH = 3;
    private static final int FACEBOOK_STRING_START = 0;
    /**
     * Default date
     */
    public static final String FACEBOOK_DEFALULT_DATE = "0";

    private static final String TAG = FacebookUtils.class.getSimpleName();
    /**
     * Normal Image tag
     */
    public static final String FACEBOOK_NORMAL_IMAGE_TAG = "_n.";
    /**
     * Small Image tag
     */
    public static final String FACEBOOK_SMALL_IMAGE_TAG = "_s.";
    /**
     * Thumbnail Image tag
     */
    public static final String FACEBOOK_THUMBNAIL_IMAGE_TAG = "_t.";

    private static final String SMALL_IMAGE_SIZE = "s130x130";
    private static final String SMALL_IMAGE_SECOND_SIZE = "p130x130";
    private static final String NORMAL_IMAGE_SIZE = "n130x130";
    public final static long hoursInMillis = 60L * 60L * 1000L;

    /**
     * Inbox fields.
     */
    private static final String fieldsContent = "to,unread,comments.fields(message,from)";
    private static final String FIELDS = "fields";
    private static final String since = "since";
    private static final String sinceContent = "-%s day";
    private static final String messageSince = ".since(-%s day)";
    private static final String inboxPath = "me/inbox";
    private static final int TIME_OUT_IN_MILLI_SECONDS = 5 * 1000;

    private Context context;
    //private long timeZoneOffsetInMillis;

    //TODO: check why do we need this timezone
    public FacebookUtils() {
//        this.timeZoneOffsetInMillis = (long) (HeadboxAccountsManager.getInstance().
//                getUserTimeZone(HeadboxAccountsManager.AccountType.FACEBOOK) * hoursInMillis);
    }

    /**
     * Converts the JSONObject's date string into Date Object.
     *
     * @param stringDate String of the date.
     * @return Date Object.
     */
    public Date getDateFromString(String stringDate) {
        DateFormat dateFormat = new SimpleDateFormat(FACEBOOK_DATE_FORMAT);
        Date date = null;

        try {
            date = dateFormat.parse(stringDate);
            //date = new Date(date.getTime() + timeZoneOffsetInMillis);
        } catch (ParseException e) {
        }
        return date;
    }

    /**
     * Normalizes the facebook date string to adapt the DateFormat type.
     */
    public String normalizeFacebookDateString(String stringDate) {

        return stringDate.replace(FACEBOOK_TIME, " ");

    }

    public String normalizeFacebookTimeString(String lastTime) {
        if (lastTime != null && lastTime.length() > FACEBOOK_MS_TIME_LENGTH) {
            return lastTime.substring(FACEBOOK_STRING_START, lastTime.length()
                    - FACEBOOK_MS_TIME_LENGTH);
        }
        return FACEBOOK_DEFALULT_DATE;
    }

    /**
     * Get the normal size facebook image URL by replacing the "_s." to "_n."
     */
    public static String getNormalSizeImage(String imageUrl) {

        if (StringUtils.isBlank(imageUrl))
            return imageUrl;

        int lastIndexOfTag, tagLength;
        StringBuilder stringBuilder = new StringBuilder(imageUrl);

        // Adjust manually image URL to get a larger image
        if (imageUrl.contains(FacebookUtils.FACEBOOK_SMALL_IMAGE_TAG)) {
            lastIndexOfTag = imageUrl
                    .lastIndexOf(FacebookUtils.FACEBOOK_SMALL_IMAGE_TAG);
            tagLength = FacebookUtils.FACEBOOK_SMALL_IMAGE_TAG.length();
            stringBuilder.replace(lastIndexOfTag, lastIndexOfTag + tagLength,
                    FacebookUtils.FACEBOOK_NORMAL_IMAGE_TAG);
        }

        if (imageUrl.contains(FacebookUtils.FACEBOOK_THUMBNAIL_IMAGE_TAG)) {
            lastIndexOfTag = imageUrl
                    .lastIndexOf(FacebookUtils.FACEBOOK_THUMBNAIL_IMAGE_TAG);
            tagLength = FacebookUtils.FACEBOOK_THUMBNAIL_IMAGE_TAG.length();
            stringBuilder.replace(lastIndexOfTag, lastIndexOfTag + tagLength,
                    FacebookUtils.FACEBOOK_NORMAL_IMAGE_TAG);
        }

        if (imageUrl.contains(FacebookUtils.SMALL_IMAGE_SIZE)) {
            lastIndexOfTag = imageUrl
                    .lastIndexOf(FacebookUtils.SMALL_IMAGE_SIZE);
            tagLength = FacebookUtils.SMALL_IMAGE_SIZE.length();
            stringBuilder.replace(lastIndexOfTag, lastIndexOfTag + tagLength,
                    NORMAL_IMAGE_SIZE);
        }

        if (imageUrl.contains(FacebookUtils.SMALL_IMAGE_SECOND_SIZE)) {
            lastIndexOfTag = imageUrl
                    .lastIndexOf(FacebookUtils.SMALL_IMAGE_SECOND_SIZE);
            tagLength = FacebookUtils.SMALL_IMAGE_SECOND_SIZE.length();
            stringBuilder.replace(lastIndexOfTag, lastIndexOfTag + tagLength,
                    NORMAL_IMAGE_SIZE);

        }

        return stringBuilder.toString();

    }

    public static int getGraphObjectsCount(GraphObject graphObject, String property, String rootCollectionJsonProperty) {


        GraphObject collectionGraph = getPropertyGraphObject(graphObject, property);
        if (collectionGraph == null) {
            return 0;
        }

        GraphObjectList<GraphObject> graphObjects = collectionGraph.getPropertyAsList(rootCollectionJsonProperty, GraphObject.class);
        if (graphObjects == null || graphObjects.size() == 0) {
            return 0;
        }

        return graphObjects.size();
    }

    /**
     * Joins the elements of the provided {@code Iterator} into a single String
     * containing the provided elements.
     *
     * @param iterator  the {@code Iterator} of values to join together, may be null
     * @param separator the separator character to use
     * @return the joined String, {@code null} if null iterator input
     */
    public static String join(Iterator<?> iterator, String separator) {
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return StringUtils.EMPTY_STRING;
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return first == null ? StringUtils.EMPTY_STRING : first.toString();
        }
        StringBuilder buf = new StringBuilder(256);
        if (first != null) {
            buf.append(first);
        }
        while (iterator.hasNext()) {
            buf.append(separator);
            Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();

    }

    public static String getPropertyString(GraphObject graphObject,
                                           String property) {

        if (graphObject == null) {
            return StringUtils.EMPTY_STRING;
        }

        Object object = graphObject.getProperty(property);
        if (object == null) {
            return StringUtils.EMPTY_STRING;
        }
        return String.valueOf(object);
    }

    public static String join(Map<?, ?> map, char separator,
                              char valueStartChar, char valueEndChar) {

        if (map == null) {
            return null;
        }
        if (map.size() == 0) {
            return StringUtils.EMPTY_STRING;
        }
        StringBuilder buf = new StringBuilder(256);
        boolean isFirst = true;
        for (Entry<?, ?> entry : map.entrySet()) {
            if (isFirst) {
                buf.append(entry.getKey());
                buf.append(valueStartChar);
                buf.append(entry.getValue());
                buf.append(valueEndChar);
                isFirst = false;
            } else {
                buf.append(separator);
                buf.append(entry.getKey());
                buf.append(valueStartChar);
                buf.append(entry.getValue());
                buf.append(valueEndChar);
            }
        }

        return buf.toString();
    }

    public static Long getPropertyLong(GraphObject graphObject, String property) {
        if (graphObject == null) {
            return null;
        }
        Object value = graphObject.getProperty(property);
        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return null;
        }

        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(Response response, Type type) {
        try {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                if (rawType.getName().equals(List.class.getName())) {
                    // if the T is of List type
                    List<GraphObject> graphObjects = mappedListFromResponse(
                            response, GraphObject.class);
                    Class<?> actualType = (Class<?>) parameterizedType
                            .getActualTypeArguments()[0];
                    Method method = actualType.getMethod("create",
                            GraphObject.class);
                    List<Object> list = ArrayList.class.newInstance();
                    for (GraphObject graphObject : graphObjects) {
                        Object object = method.invoke(null, graphObject);
                        list.add(object);
                    }
                    return (T) list;
                }
            } else {
                Class<?> rawType = (Class<?>) type;
                GraphObject graphObject = response.getGraphObject();
                Method method = rawType.getMethod("create", GraphObject.class);
                Object object = method.invoke(null, graphObject);
                return (T) object;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Facebook Helper method list representing the same underlying data,
     * exposed as the new GraphObject-derived type
     */
    public static <T extends GraphObject> List<T> mappedListFromResponse(
            Response response, Class<T> classT) {
        GraphMultiResult multiResult = response
                .getGraphObjectAs(GraphMultiResult.class);
        if (multiResult != null && multiResult.getData() != null) {
            return multiResult.getData().castToListOf(classT);
        }
        return null;
    }

    public static Boolean getPropertyBoolean(GraphObject graphObject,
                                             String property) {
        if (graphObject == null) {
            return null;
        }
        Object value = graphObject.getProperty(property);
        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return null;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    public static Integer getPropertyInteger(GraphObject graphObject,
                                             String property) {
        if (graphObject == null) {
            return 0;
        }
        Object value = graphObject.getProperty(property);
        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return 0;
        }

        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }

    }

    public static Double getPropertyDouble(GraphObject graphObject,
                                           String property) {
        if (graphObject == null) {
            return null;
        }
        Object value = graphObject.getProperty(property);
        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return null;
        }
        return Double.valueOf(String.valueOf(value));
    }

    public static JSONArray getPropertyJsonArray(GraphObject graphObject,
                                                 String property) {
        if (graphObject == null) {
            return null;
        }
        Object value = graphObject.getProperty(property);
        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return null;
        }

        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(value);
            return jsonArray;
        } catch (JSONException e) {
            try {
                return (JSONArray) value;
            } catch (Exception e1) {
            }
        }
        return null;
    }

    public static String getPropertyInsideProperty(GraphObject graphObject,
                                                   String parent, String child) {
        if (graphObject == null) {
            return null;
        }

        JSONObject jsonObject = (JSONObject) graphObject.getProperty(parent);
        if (jsonObject != null) {
            return String.valueOf(jsonObject.opt(child));
        }
        return null;
    }

    public static GraphObject getPropertyGraphObject(GraphObject graphObject,
                                                     String property) {
        if (graphObject == null) {
            return null;
        }
        return graphObject.getPropertyAs(property, GraphObject.class);
    }

    public static User createUser(GraphObject graphObject, String parent) {
        if (graphObject == null) {
            return null;
        }
        GraphObject userGraphObject = getPropertyGraphObject(graphObject,
                parent);
        if (userGraphObject == null) {
            return null;
        }
        return createUser(userGraphObject);
    }

    public static User createUser(GraphObject graphObject) {

        final String id = String.valueOf(graphObject
                .getProperty(ProfileMapper.Properties.ID));
        final String name = String.valueOf(graphObject
                .getProperty(ProfileMapper.Properties.NAME));

        User user = new User() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getId() {
                return id;
            }
        };

        return user;
    }

    public static User createUser(JSONObject graphObject) {

        User user = null;

        try {
            final String id = graphObject.getString(ProfileMapper.Properties.ID);
            final String name = graphObject.getString(ProfileMapper.Properties.NAME);

            user = new User() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getId() {
                    return id;
                }
            };
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return user;
    }


    public static GraphObjectList<GraphObject> getGraphObjectList(JSONObject object, String property) throws JSONException {

        if (object == null) {
            return null;
        }
        return GraphObject.Factory.createList(object.getJSONArray(property), GraphObject.class);
    }

    public static List<PostsMapper.StoryTag> createStoryTagsList(GraphObject graphObject, String property, Converter converter) {

        List<PostsMapper.StoryTag> result = new ArrayList<PostsMapper.StoryTag>();
        if (graphObject == null) {
            return result;
        }

        GraphObject mapGraph = graphObject.getPropertyAs(property, GraphObject.class);
        if (mapGraph == null) {
            return result;
        }

        // get the map of objects and have them in ordered way
        Map<String, Object> map = mapGraph.asMap();
        Set<String> keySet = map.keySet();
        SortedSet<String> keys = new TreeSet<String>(new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return Integer.valueOf(lhs) - Integer.valueOf(rhs);
            }
        });
        keys.addAll(keySet);

        for (String key : keys) {
            GraphObjectList<GraphObject> graphObjects = mapGraph.getPropertyAsList(key, GraphObject.class);
            if (graphObjects == null || graphObjects.size() == 0) {
                continue;
            }

            ListIterator<GraphObject> iterator = graphObjects.listIterator();
            while (iterator.hasNext()) {
                GraphObject graphObjectItr = iterator.next();
                PostsMapper.StoryTag t = converter.convert(graphObjectItr);
                result.add(t);
            }
        }

        return result;
    }

    public interface Converter {
        PostsMapper.StoryTag convert(GraphObject e);
    }

    /**
     * Build parameters to pass along with the Graph API request.
     */
    public static Bundle buildProfileBundle() {

        // define the friend picture we want to get.
        PictureAttributes pictureAttributes = ProfileMapper.getPictureAttributes();
        pictureAttributes.setType(PictureType.SQUARE);
        pictureAttributes.setHeight(PictureAttributes.PICTURE_DEFAULT_HEIGHT);
        pictureAttributes.setWidth(PictureAttributes.PICTURE_DEFAULT_WIDTH);

        ProfileMapper.Properties properties = new ProfileMapper.Properties.Builder()
                .add(ProfileMapper.Properties.ID)
                .add(ProfileMapper.Properties.USER_NAME)
                .add(ProfileMapper.Properties.NAME)
                .add(ProfileMapper.Properties.EMAIL)
                .add(ProfileMapper.Properties.FIRST_NAME)
                .add(ProfileMapper.Properties.LAST_NAME)
                .add(ProfileMapper.Properties.LINK)
                .add(ProfileMapper.Properties.TIMEZONE)
                .add(ProfileMapper.Properties.COVER)
                .add(ProfileMapper.Properties.PICTURE, pictureAttributes).build();

        return properties.getBundle();
    }

    /**
     * Build parameters to pass along with the Graph API request.
     */
    public static Bundle buildSearchBundle(String queryString, String type, int limit) {

        // Build profile bundle.
        Bundle bundle = FacebookUtils.buildProfileBundle();
        // Add search properties.
        bundle.putString(ProfileMapper.Properties.Search.LIMIT, String.valueOf(limit));
        //bundle.putString(Facebook.Search.OFFSET, String.valueOf(offset));
        bundle.putString(ProfileMapper.Properties.Search.TYPE, type);
        bundle.putString(ProfileMapper.Properties.Search.QUERY_STRING, queryString);

        return bundle;
    }

    /**
     * Build parameters to pass along with the Graph API request.
     */
    public static Bundle buildInboxBundle() {

        List<String> comments = new ArrayList<String>();
        comments.add(InboxMapper.Properties.ID);
        comments.add(InboxMapper.Properties.FROM);
        comments.add(InboxMapper.Properties.MESSAGE);
        comments.add(InboxMapper.Properties.CREATED_TIME);

        List<String> profile = new ArrayList<String>();
        profile.add(ProfileMapper.Properties.ID);
        profile.add(ProfileMapper.Properties.NAME);
        profile.add(ProfileMapper.Properties.COVER);
        profile.add(ProfileMapper.Properties.PICTURE);
        profile.add(ProfileMapper.Properties.TIMEZONE);
        profile.add(ProfileMapper.Properties.FIRST_NAME);
        profile.add(ProfileMapper.Properties.LAST_NAME);
        profile.add(ProfileMapper.Properties.USER_NAME);
        profile.add(ProfileMapper.Properties.LINK);

        InboxMapper.Properties properties = new InboxMapper.Properties.Builder()
                .add(InboxMapper.Properties.ID).add(InboxMapper.Properties.UNREAD)
                .add(InboxMapper.Properties.UPDATED_TIME)
                .add(InboxMapper.Properties.TO, InboxMapper.Properties.FIELDS, profile)
                .add(InboxMapper.Properties.COMMENTS, InboxMapper.Properties.FIELDS, comments)
                .build();

        return properties.getBundle();
    }

    public static Bundle buildPostsBundle() {

        PostsMapper.Properties properties = new PostsMapper.Properties.Builder()
                .add(PostsMapper.Properties.ID)
                .add(PostsMapper.Properties.OBJECT_ID)
                .add(PostsMapper.Properties.MESSAGE)
                .add(PostsMapper.Properties.PICTURE)
                .add(PostsMapper.Properties.COMMENTS)
                .add(PostsMapper.Properties.PLACE)
                .add(PostsMapper.Properties.LIKES)
                .add(PostsMapper.Properties.FULL_PICTURE)
                .add(PostsMapper.Properties.LINK)
                .add(PostsMapper.Properties.STORY)
                .add(PostsMapper.Properties.STORY_TAGS)
                .add(PostsMapper.Properties.DESCRIPTION)
                .add(PostsMapper.Properties.SOURCE)
                .add(PostsMapper.Properties.LINK)
                .add(PostsMapper.Properties.TYPE)
                .add(PostsMapper.Properties.STATUS_TYPE).build();
        return properties.getBundle();
    }

    public static Bundle buildEventsBundle() {

        EventsMapper.Properties properties = new EventsMapper.Properties.Builder()
                .add(EventsMapper.Properties.ID)
                .add(EventsMapper.Properties.NAME)
                .add(EventsMapper.Properties.PICTURE)
                .add(EventsMapper.Properties.COVER)
                .add(EventsMapper.Properties.START_TIME)
                .add(EventsMapper.Properties.END_TIME)
                .add(EventsMapper.Properties.UPDATED_TIME)
                .add(EventsMapper.Properties.DESCRIPTION)
                .add(EventsMapper.Properties.LOCATION)
                .add(EventsMapper.Properties.OWNER).build();
        return properties.getBundle();
    }

    /**
     * Receives the 1st biggest high res image from images array
     * We can Use the 2nd to save memory and internet bandwidth
     * method used to get high resolution images from facebook
     *
     * @param jsonObject "images" arrays returned from object_id
     */
    public static String getHighResImageFromImages(JSONObject jsonObject) {
        try {
            int imageResolutionIndexToUse = 0;
            JSONArray imagesArray = jsonObject.getJSONArray("images");
            JSONObject image;
            if (imagesArray.length() > imageResolutionIndexToUse) {
                image = imagesArray.getJSONObject(imageResolutionIndexToUse);
            } else {
                image = imagesArray.getJSONObject(0);
            }
            return image.getString("source");
        } catch (JSONException e) {
            return null;
        }
    }


    public interface User {

        String getId();

        String getName();
    }

}