package com.blinq.utils;

/**
 * @author Johan Hansson
 *         <p/>
 *         Holds the application Constants.
 */
public class Constants {


    private Constants() {
    }

    /**
     * feed id Extra
     */
    public static final String FEED_ID = "FeedId";

    /**
     * Platform extra for the app intent.
     */
    public static final String PLATFORM = "platform" ;

    public static final String IS_CLOSE = "IS_CLOSE" ;


    /**
     * Day in millisecond 24 * 60 * 60 *1000
     */
    public static final long DAY_IN_MILLISECOND = (long) 864e5;

    /**
     * extra data alias, to use when starting service.
     */
    public static final String EXTRA_RESULT = "result";

    /**
     * Indicates if the google contacts loaded or not.
     */
    public static final String GOOGLE_CONTACTS_LOADED = "Loaded";

    /**
     * Flag indicator for notification pressed on, true single notification,
     * else multi notifications.
     */
    public static final String SINGLE_NOTIFICATION = "SingleNotification";

    /**
     * Flag indicator for Instants Message Activity to check if the intent
     * created from Feed Activity or from Notification.
     */
    public static final String FROM_NOTIFICATION = "From Notification";

    public static String OPEN_SOCIAL_WINDOW= "openSocialWindow";

    /**
     * The name of the extra data, to use when sending data between activities
     * and receivers.
     */
    public static final String MESSAGE_EXTRA = "messageID";
    public static final String FEED_EXTRA = "feedID";
    public static final String MESSAGE_TYPE_EXTRA = "messageType";
    public static final String OLD_MESSAGE_EXTRA = "oldMessageID";

    /**
     * The Intent action name, to use when send broadcast to refresh our views.
     */
    public static final String ACTION_REFRESH_INSERT = "android.intent.action.refresh.onInsert";
    public static final String ACTION_REFRESH_UPDATE = "android.intent.action.refresh.onUpdate";
    public static final String ACTION_REFRESH_MESSAGE_TYPE_CHANGE = "android.intent.action.refresh.onTypeChange";

    /**
     * Used to check if the image URI from web.
     */
    public static final String WEB_URL_INDICATOR = "http";

    /**
     * Number of feeds to be loaded every request.
     */
    public static final int NUMBER_OF_FEEDS_TO_LOAD_FIRST = 10;

    public static final int NUMBER_OF_FEEDS_TO_LOAD = 20;

    public static final int MAX_NUMBER_OF_PLATFORMS = 6;

    // /////////////////////////////////////////////////////////////////
    // For Facebook connection
    // /////////////////////////////////////////////////////////////////
    public static final String FACEBOOK_LOGIN_MECHANISIM = "X-FACEBOOK-PLATFORM";
    /**
     * Facebook application key.
     */
    public static final String FACEBOOK_API_KEY = "370719043029103";

    // /////////////////////////////////////////////////////////////////
    // For calculating application mode
    // /////////////////////////////////////////////////////////////////

    /**
     * Shared preferences flag for application mode (day/night).
     */
    public static final String IS_DAY_MODE = "IsDayMode";

    /**
     * AM period
     */
    public static final int AM = 0;

    /**
     * PM period
     */
    public static final int PM = 1;

    /**
     * 6 AM.
     */
    public static final int START_HOUR_OF_AM_DAY_MODE = 6;

    /**
     * 11 AM
     */
    public static final int END_HOUR_OF_AM_DAY_MODE = 12;

    /**
     * 12 PM , separate between AM & PM periods.
     */
    public static final int START_HOUR_OF_PM_DAY_MODE = 0;

    /**
     * 6 PM.
     */
    public static final int END_HOUR_OF_PM_DAY_MODE = 6;

    /**
     * Web page URL extra data alias.
     */
    public static final String WEB_PAGE_LINK = "WebPageLink";

    public static final String DEAFAULT_SHARED_PREFERENCES_PATH = "/shared_prefs/";
    public static final String APPLICATION_PATH = "/data/data/";

    public static final String RESOURCES_PATH = "android.resource://";
    /**
     * URI schemes.
     */
    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_SMSTO = "smsto";


    // Search view components hierarchy indices
    // TODD : build customize Headbox search view widget that extend the original searchview with customization to be used in feed and merge screens.
    public static final int SEARCH_PARENT_LAYOUT = 0;
    public static final int SEARCH_EDIT_FRAME = 2;
    public static final int SEARCH_APP_ICON = 0;
    public static final int SEARCH_PLATE_FRAME = 0;
    public static int SEARCH_TEXT_VIEW = 0;
    public static final int SEARCH_CLOSE_BUTTON = 1;
    public static final String SEARCH_VIEW_TEXT_ID = "android:id/search_src_text";


    /**
     * Merge view.
     */
    public static final String MERGE_TYPE = "merge_type";

    /*
    * Server request parameters constants
     */
    public static final String FACEBOOK_PARAM = "facebook";
    public static final String FACEBOOK_ID_PARAM = "facebookId";
    public static final String TWITTER_PARAM = "twitter";
    public static final String EMAIL_PARAM = "email";
    public static final String MOBILE_NUM_PARAM = "phone";
    public static final String DB_FILE_PARAM = "filefield";
    public static final String DB_CONTACTS_COUNT = "count";

    public static final String CONTACT_NAME = "contact_name";

    public static final String FRIEND_PARAM = "friend";
    public static final String ACCESS_TOKEN_PARAM = "accessToken";
    public static final String LIKES_COUNT = "likesCount";

    public static final String FROM_FEED = "from_feed";

    public static final int SYNC_SKIPPED = 0;
    public static final int SYNC_STARTED = 1;
    public static final int SYNC_ENDED = 2;
    public static final int SYNC_UNKNOWN = 3;
}
