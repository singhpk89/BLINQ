package com.blinq.analytics;


public final class AnalyticsConstants {

    public static final String MIXPANEL_API_TOKEN = "44d1275827a578d0d6dc814952b7f894";
    public static final String ANDROID_PUSH_SENDER_ID = "286424613906";

    public static final String MIXAPNEL_PREFERENCES_NAME = "mixpanelPreferences";
    public static final String MIXPANEL_DISTINCT_ID_NAME = "mixapnelUserId";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    /* Event Name */
    public static final String LOGIN_EVENT = "Login";
    public static final String OUTGOING_EVENT = "OUT";
    public static final String SEND_OUTGOING_EVENT = "Send Outgoing";
    public static final String INCOMING_EVENT = "IN";
    public static final String OPENED_SPLASH_SCREEN_EVENT = "Opened Splash Screen";
    public static final String OPENED_FEEDBACK_SCREEN_EVENT = "Opened Feedback Screen";
    public static final String OPENED_FEED_SCREEN_EVENT = "Opened Feed Screen";
    public static final String OPENED_SEARCH_SCREEN_EVENT = "Opened Search Screen";
    public static final String OPENED_COMLOG_SCREEN_EVENT = "Opened Comlog Screen";
    public static final String OPENED_CONNECT_MORE_SCREEN_EVENT = "Opened connect more platform screen";
    public static final String CLICK_TO_OPEN_COMLOG_EVENT = "Clicked to open comlog";
    public static final String OPENED_LOGIN_SCREEN_EVENT = "Opened Sign in Screen";
    public static final String OPENED_WELCOME_NOTIFICATION_EVENT = "press on the welcome notification";
    public static final String RECEIVE_ANY_NOTIFICATION_EVENT = "Receive a notification";
    public static final String RECEIVE_ANY_NOTIFICATION_UNKNOWEN_PLATFORM_EVENT = "Receive a notification from unknown platform";
    public static final String RECEIVE_HEADBOX_NOTIFICATION_EVENT = "Receive Headbox notification";
    public static final String CONTACTS_WITH_PHOTO_EVENT = "Contacts with photos in the feed";
    public static final String CONTACTS_WITHOUT_PHOTO_EVENT = "Contacts without a photo in the feed";
    public static final String SWIPE_BACK_TO_FEED_EVENT = "Swipe to go back to feed";
    public static final String RECEIVE_CONGRATS_POPUP_EVENT = "Receive the congrats pop up";
    public static final String CREATE_USER_PROFILE_EVENT = "Create user profile";

    public static final String RECEIVE_WELCOME_NOTIFICATION_EVENT = "Receive the welcome notification";
    public static final String OPENED_STREAM_NOTIFICATIONS_SCREEN_EVENT = "Opened Stream Notifications Screen";
    public static final String OPENED_SETTINGS_SCREEN_EVENT = "Opened Settings Screen";
    public static final String SWITCH_PLATFORM_EVENT = "Switched Platform";
    public static final String FILTER_EVENT = "Switched Tab";
    public static final String CLICKED_CANCEL_SEARCH_EVENT = "Clicked on Cancel in search";
    public static final String CLICKED_RESULT_SEARCH_EVENT = "Clicked on Search result item";
    public static final String LONG_CLICKED_RESULT_SEARCH_EVENT = "Long clicked on Search result item";
    public static final String ENTERING_APP_EVENT = "Entered App";
    public static final String NOTIFICATION_IS_CLICKED_EVENT = "Clicked on Socialize";
    public static final String RETRO_DIALER_CLICKED_EVENT = "Clicked on Retro Dialer";
    public static final String SOCIAL_WINDOW_SCROLL_EVENT = "Scroll in Social Window";
    public static final String SOCIAL_WINDOW_CLICKED_EVENT = "Clicked in Social Window by platform";
    public static final String SOCIAL_WINDOW_TYPE_CLICKED_EVENT = "Clicked in Social Window by type";
    public static final String SOCIAL_WINDOW_CLICKED_SHOW_EVENT = "Clicked on top to show social window";
    public static final String PERSON_CLICKED_EVENT = "Clicked a Person";
    public static final String PERSON_MERGED_START_EVENT = "Opened Merge screen";
    public static final String PERSON_SUGGESTED_MERGED_EVENT = "Merge screen person was suggested";
    public static final String MERGED_A_PERSON_SUCCESSFULLY = "Merge a person successfully";
    public static final String SETTINGS_CHANGED_EVENT = "Settings changed";
    public static final String CLICKED_ON_SETTINGS_EVENT = "Clicked on %s";
    public static final String NOTIFICATION_CHANGE_EVENT = "%s Stream Notifications";
    public static final String EULA_CLICKED_EVENT = "Clicked on Terms of Service";
    public static final String PRIVACY_CLICKED_EVENT = "Clicked on Privacy";
    public static final String SKIPPED_LOGIN_EVENT = "Skipped on login";
    public static final String SKIPPED_STREAM_NOTIFICATIONS_EVENT = "Skipped on Stream Notifications";


    /**
     * Drawer view events.
     */
    public static final String DRAWER_OPENED_EVENT = "Opened Drawer";
    public static final String DRAWER_SPREAD_LOVE_CLICKED_EVENT = "Clicked on Spread the love";
    public static final String DRAWER_SPREAD_LOVE_SUCCESS_EVENT = "Spread the love successfully";

    public static final String CANCELED_SEARCH_EVENT = "Canceled Search";
    public static final String SEND_BUTTON_CLICKED_EVENT = "Clicked on Send Button";
    public static final String FIRST_INSTALL_EVENT = "First Install";
    public static final String AUTO_MERGED_CONTACTS_EVENT = " Automatic merged contacts";
    public static final String DAY_USED_EVENT = "Day used for %d Days";
    public static final String LOGGED_IN_EVENT = "Login with %s From %s";
    public static final String SUCCESS_CONNECT_EVENT = "Successfully Connect to %s from %s";
    public static final String FEED_ANDROID_OPTION = "Android option";
    public static final String FEED_ANDROID_OPTION_ADD_CONTACT = "Android option add contact";
    public static final String FEED_ANDROID_OPTION_SETTINGS = "Android option Settings";
    public static final String CLICK_ON_SEARCH_ICON = "Click on Search icon";

    public static final String FACEBOOK_LOGIN_EVENT = "Facebook Login ";
    public static final String LINKEDIN_EVENT = "Clicked on Linkedin Button";
    public static final String CONTACTS_SYNC_EVENT = "Contacts Sync ";
    public static final String CLICKED_RANDOMLY_WALKTHROUGH_SCREEN_EVENT = "Clicked randomly in walkthrough screen";
    public static final String CLICKED_RANDOMLY_SIGNIN_SCREEN_EVENT = "Clicked randomly in Facebook sign in screen";
    public static final String CLICKED_RANDOMLY_SOCIAL_NETWORK_SCREEN_EVENT = "Clicked randomly in social network sign in screen";
    public static final String CLICKED_RANDOMLY_NOTIFICATION_SCREEN_EVENT = "Clicked randomly in notification screen";
    public static final String CLICKED_ON_RETRO_CENTER_EVENT = "Clicked on retro Dialer center";
    public static final String BACK_FACEBOOK_EVENT = "Press back on facebook login";
    public static final String BACK_WALKTHROUGH_EVENT = "Press back on walkthrogh screen";
    public static final String COMLOG_CALL_EVENT = "Clicked on make a call";
    public static final String COMLOG_REFRESH_EVENT = "Clicked on refresh";
    public static final String COMLOG_AVATAR_EVENT = "Clicked on avatar";
    public static final String CHANGE_THEME_EVENT = "Changed Theme";
    public static final String SOUND_EVENT = "Sound";
    public static final String NOTIFICATION_SETTINGS_CHANGE_EVENT = "Settings notification changed";
    public static final String PRESSED_BACK_IN_STREAM_NOTIFICATION_EVENT = "Press back after stream notification";
    public static final String PRESSED_BACK_IN_STREAM_NOTIFICATION_SCREEN_EVENT = "Press back on Notifications Screen";
    public static final String DELETE_MESSAGE_EVENT = "Delete a message";
    public static final String COPY_MESSAGE_EVENT = "Copy a message";
    public static final String LONG_CLICK_ON_MESSAGE_EVENT = "Long clicked on message";
    public static final String PERSON_MERGED_EVENT = "Merged a Person Succcess";
    public static final String CLICKED_ON_CONVERSION_TEXT_EVENT = "Click on the text box";
    public static final String CLICKED_ON_NOTIFICATION_STREAM_BUTTON_EVENT = "Press Button in Stream Notifications Screen";
    public static final String OPENED_TUTORIAL_EVENT = "Opened tutorial";
    public static final String SKIPPED_IN_SOCIAL_EVENT = "Skipped in social screen";
    public static final String PRESS_BACK_SOCIAL_EVENT = "Press back on social screen";
    public static final String CREATED_HEADBOX_NOTIFICATION = "Created headbox notification";
    public static final String UPDATE_TOP_FRIENDS_EVENT = "Updated top friends";
    public static final String SOCIAL_WINDOW_ARROW_CLICKED_EVENT = "Clicked on social window arrow";

    /**
     * Connect More view.
     */

    /**
     * In the connect more social network, how many user saw the pop up "connect more platform".
     */
    public static final String CONNECT_MORE_PLATFORMS_POPUP_OPENED = "Saw connect more socials pop-up";
    /**
     * When user get pop up connect more platform how of them press skip.
     */
    public static final String CONNECT_MORE_PLATFORMS_POPUP_SKIP_CLICKED = "Connect more social networks popup clicked SKIP";
    /**
     * When user get pop up connect more platform how of them press OK.
     */
    public static final String CONNECT_MORE_PLATFORMS_POPUP_OK_CLICKED = "Connect more social networks popup clicked OK";

    /**
     * Stream Notification view.
     */

    /**
     * In the stream notification, how many user saw the pop up "stream your notification
     */
    public static final String SKIP_STREAM_NOTIFICATION_POPUP_OPENED = "Saw Skip Stream notification pop-up";
    public static final String SKIP_STREAM_NOTIFICATION_POPUP_OPENED_TWICE = "Saw Skip Stream notification pop-up twice";
    /**
     * When user get pop up stream notification how of them press skip
     */
    public static final String STREAM_NOTIFICATION_POPUP_SKIP_CLICKED = "Stream notification popup clicked SKIP";
    /**
     * When user get pop up stream notification how of them press OK
     */
    public static final String STREAM_NOTIFICATION_POPUP_OK_CLICKED = "Stream notification popup clicked OK";


    public static final String AB_FEED_HISTORY_FULL = "Full";
    public static final String AB_FEED_HISTORY_NONE = "None";
    public static final String AB_FEED_HISTORY_FIVE = "Only 5";

    /**
     * In the feed, User long pressed on conversation,
     * open the option & clicked on the garbage to remove the conversation
     */
    public static final String FEED_DELETE_CONVERSATION_EVENT = "Delete a Feed conversation";
    /**
     * In the feed, User long pressed on conversation,
     * open the option & clicked on the phone  to call the contact
     */
    public static final String FEED_CALL_EVENT = "Feed Call";

    public static final String NOTIFICATION_CLICKED_ON_SOCIALIZE = "Clicked on socialize";

    public static final String DRAWER_PROFILE_CLICKED_EVENT = "Clicked on user profile in the drawer";

    public static final String SOCIAL_WINDOW_ARROW_SWIPPED_EVENT = "Swiped social window arrow";

    public static final String SHOW_THE_DOT_EVENT = "Show the dot";
    public static final String CLICKED_THE_DOT_EVENT = "Tap the dot";


    /* Events Categories */
    public static final String ONBOARDING_CATEGORY = "On boarding";
    public static final String ACTION_CATEGORY = "Action";
    public static final String COMMUNICATION_CATEGORY = "Communication";
    public static final String SETTINGS_CATEGORY = "Settings";
    public static final String ERRORS_CATEGORY = "Errors";
    public static final String USER_INFO_CATEGORY = "User Info";
    public static final String AB_FEED_HISTORY = "AB Feed History";

    /* Errors events */
    public static final String FAILED_TO_SHARE_EVENT = "Failed To Spread The Love";
    public static final String FAILED_TO_SHARE_EXCEPTION_EVENT = "Failed To Spread The Love Exception";
    public static final String REASON_PROPERTY = "reason";

    /* Events properties's names */
    public static final String CONTACT_COUNT_PROPERTY = "# of mobile friends";
    public static final String CONTACTS_COUNT_PROPERTY = "# of %s friends";
    public static final String STATUS_PROPERTY = "Status";
    public static final String TYPE_PROPERTY = "Type";
    public static final String EXCEPTION_MESSAGE_PROPERTY = "Exception";
    public static final String FROM_PROPERTY = "From";
    public static final String SWITCHED_TO_PROPERTY = "Switched to";
    public static final String BACK_BUTTON_NOTIFICATION_CLICKED_PROPERTY = "Back Button Clicked in notifications";
    public static final String BACK_BUTTON_COMLOG_CLICKED_PROPERTY = "Back Button Clicked in comlog";
    public static final String BACK_COMLOG_CLICKED_PROPERTY = "Press back in the comlog";
    public static final String SOCIAL_WINDOW_DIRECTION_PROPERTY = "Swipe";

    public static final String PLATFORM_NOTIFICATION_PROPERTY = "%s Notification";
    public static final String STREAM_NOTIFICATIONS_PROPERTY = "Is Stream Notifications";
    public static final String DATE_FIRST_SEEN_PROPERTY = "Date First Seen";
    public static final String COUNT_SUGGESTIONS = "# of suggestions";
    public static final String COUNT_PROPERTY = "count";
    public static final String TOTAL_CONTACTS_PROPERTY = "Total contacts";
    public static final String MERGED_CONTACTS_FROM_PROPERTY = "From %s";
    public static final String PLATFORM_MESSAGES_PROPERTY = "%s messages";
    public static final String TOTAL_MESSAGES_PROPERTY = "Total messages";
    public static final String UPDATE_TOP_FRIEND_PROPERTY_REPLACED = "Replaced";


    /* Events properties's possible values */

    public static final String FAILURE_VALUE = "Failure";

    public static final String NOTIFICATION_VALUE = "Notication";
    public static final String DIRECT_LAUNCH_VALUE = "Direct launch";
    public static final String SCROLLING_UP_VALUE = "Up";
    public static final String SCROLLING_DOWN_VALUE = "Down";
    public static final String SCROLLING_LEFT_VALUE = "Left to Right";
    public static final String SCROLLING_RIGHT_VALUE = "Right to Left";


    public static final String ENABLED_VALUE = "Enabled";
    public static final String DISABLED_VALUE = "Disabled";

    public static final String LOGIN_FROM_SOCIAL_WINDOW = "Social Window";
    public static final String LOGIN_FROM_LOGIN_SCREEN = "Signin";
    public static final String LOGIN_FROM_DRAWER = "Drawer";
    public static final String LOGIN_FROM_RETRO = "Retro Dialer";
    public static final String ONLY_CALL_AND_SMS_NOTFIFICATION = "only calls and sms";
    public static final String ALL_NOTIFICATION = "all";


    /* User's profile properties */

    public static final String FISRT_NAME_PROFILE_PROPERTY = "$first_name";
    public static final String LAST_NAME_PROFILE_PROPERTY = "$last_name";
    public static final String EMAIL_PROFILE_PROPERTY = "$email";
    public static final String PROFILE_ID_PROPERTY = "%s ID";
    public static final String PROFILE_NAME_PROPERTY = "%s Name";
    public static final String PROFILE_DAY_USED_COUNT_PROPERTY = "Day used";
    public static final String PROFILE_AB_NOTIFICATION_PROPERTY = "AB Notification";
    public static final String SET_USER_PROFILE_EVENT = "Set user profile";

    public static final int DAY_USED_COUNT_THRESHOLD = 1;
    public static final int DAY_USED_COUNT_MAX_THRESHOLD = 7;
    public static final String PROPERITES_SEPARATOR = ":";
    public static final String DEFAULT_CATEGORY_VALUE = "Category";

    public static final String FAILED_VALUE = "Failed";
    public static final String SUCCESS_VALUE = "Success";
    public static final String STARTED_VALUE = "Started";
    public static final String COMPLETED_VALUE = "completed ";
    public static final String SWIPE_VALUE = "Swipe";
    public static final String ICON_VALUE = "Icon";
    public static final String TUTORIAL_VALUE = "Tuto";
    public static final String RIGHT_VALUE = "right";
    public static final String LEFT_VALUE = "left";
    public static final String PLATFORM_PROPERTY = "Platform";


    private AnalyticsConstants() {
        throw new AssertionError();
    }

}
