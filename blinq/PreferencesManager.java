package com.blinq;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.blinq.analytics.AnalyticsConstants;
import com.blinq.models.Platform;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.nu.art.software.TacB0sS.samples.genericPreferences.AppSpecificPreferenceStorage;
import com.nu.art.software.TacB0sS.samples.genericPreferences.PreferencesStorage;


/**
 * Manages and encapsulates headbox development preferences.
 *
 * @author Johan Hansson.
 */
public class PreferencesManager {

    /**
     * Shared Preferences
     */
    public static AppSpecificPreferenceStorage preferences;

    private Context context;

    /**
     * Shared preferences file name
     */
    private static final String PREF_NAME = "headbox_preferences";

    private static final String APP_LOADED = "loaded";
    private static final String APP_VERSION_CODE = "versionCode";
    private static final String APP_VERSION_NAME = "versionName";

    private static final String DB_UPGRADE_STATUS = "database_upgraded";

    private static final String CONTACTS_LOADING = "%s_CONTACTS_LOADED";

    private static final String COVERS_LOADING = "CoversLoading";

    private static final String FACEBOOK_PROFILE_LOADED = "facebook_Loaded";
    public static final String ANALYTICS_STATUS = "isAnalyticsEnabled";

    private static final String TWITTER_ACCESS_TOKEN = "twitter_oauth_token";
    private static final String TWITTER_ACCESS_TOKEN_SECRET = "twitter__oauth_token_secret";
    private static final String TWITTER_LOGIN_STATUS = "isTwitterLogedIn";

    private static final String FACEBOOK_HISTORY_LOADING = "facebook_history_last_loading";
    private static final long FACEBOOK_HISTORY_LOADING_ELAPSED_TIME = 120000;

    private static final String CONTACTS_LAST_LOADING_TIME = "last_loading_time";

    /**
     * Used to track user usage and to make ui decision based on that
     */
    private static final String NUMBER_OF_CLICKED_ON_DOT = "number_of_clicked_on_dot";

    private static final String LAST_PERSON_BIO = "last_person_bio";
    private static final String LAST_PERSON_TITLE = "last_person_title";
    private static final String LAST_PERSON_ORGANIZATION = "last_person_organization";
    private static final String LAST_PERSON_IMAGE_PATH = "last_person_image_path";
    private static final String LAST_PERSON_NAME = "last_person_name";

    private static final String LAST_PERSON_SOCIAL_ABOUT_ME = "last_person_social_about_me";
    private static final String LAST_PERSON_SOCIAL_ANGELLIST = "last_person_social_angellist";
    private static final String LAST_PERSON_SOCIAL_FOURSQUARE = "last_person_social_foursquare";
    private static final String LAST_PERSON_SOCIAL_GITHUB = "last_person_social_github";
    private static final String LAST_PERSON_SOCIAL_GOOGLE_PLUS = "last_person_social_googleplus";
    private static final String LAST_PERSON_SOCIAL_GRAVATAR = "last_person_social_gravatar";
    private static final String LAST_PERSON_SOCIAL_KLOUT = "last_person_social_klout";
    private static final String LAST_PERSON_SOCIAL_LINKEDIN = "last_person_social_linkedin";
    private static final String LAST_PERSON_SOCIAL_PICASA = "last_person_social_picasa";
    private static final String LAST_PERSON_SOCIAL_PINTEREST = "last_person_social_pinterest";
    private static final String LAST_PERSON_SOCIAL_TWITTER = "last_person_social_twitter";
    private static final String LAST_PERSON_SOCIAL_INSTAGRAM = "last_person_social_instagram";
    private static final String LAST_PERSON_SOCIAL_FACEBOOK = "last_person_social_facebook";
    private static final String LAST_PERSON_SOCIAL_VIMEO = "last_person_social_vimeo";

    private static final String SHOWED_ME_CARD_OF = "showed_me_card_of_%s";
    private static final String MERGED_SOCIAL_PROFILE = "merged_social_profile_%d_%d";
    private static final String LIKES_COUNT = "likes_count";
    private static final String LAST_NOTIFICATION_CONTACT_NAME = "last_notification_contact_name";

    private static final String NETWORK_COUNTRY_ISO = "network_country_iso";

    private static final String RATED_THE_APP = "rated_the_app";

    /**
     * Indicates whether certain platform's contacts has been reloaded or not.
     */
    public static final String CONTACTS_RELOADING_COMPLETED = "%s_contacts_reloading_completed";

    private static final long CONTACTS_LOADING_ELAPSED_TIME = 12 * 60 * 60 * 1000;

    private static final String INCOMING_FACEBOOK_MESSAGES_COUNTER = "facebook_incoming_messages_counter";
    private static final int UPDATE_FACEBOOK_INBOX_AFTER_INCOMING_MESSAGES_COUNT = 10;
    private static final String FACEBOOK_CONTACT_LOADING = "facebook_contacts_last_loading";
    private static final long FACEBOOK_CONTACTS_LOADING_ELAPSED_TIME = 60 * 60 * 1000;
    public static final int FACEBOOK_INBOX_FIRST_LOAD_LIMIT = 25;

    public static final long ALLOWED_IDLE_TIME_In_MILLIS = 1 * 60 * 1000;

    public static final String IS_THEME_CHANGED = "ThemeChanged";
    public static final String FIRST_COMLOG_USE = "FirstComlogUse";
    public static final String FIRST_APP_USE = "FirstAppUse";
    public static final int DEVICE_LOGS_LOADING_PERIOD = 21;
    public static final int RECENT_LOG_COUNT = 5;

    /**
     * Alarm manager triggering time.
     */
    public static final String ALARM_SET = "alram_set";
    public static final long ALARM_TRIGGERING_TIME = 12 * 60 * 60 * 1000;

    public static final String SETUP_SCREEN_OPENED = "SETUP_SCREEN_OPENED";

    public static final String NOTIFICATION_WELCOME_SHOWN = "default_notification_app_welcome_shown";

    public static final String FIRST_INSTALL = "first_time_install";
    public static final String USER_ENTERED_LAST_DAY = "app_entered_last_date";
    public static final String DAY_USED_COUNT = "day_used";

    public static final String MERGED_CONTACT_ID = "merged_contact_id";
    public static final String FEED_CONTACT_ID = "contact_id";
    public static final String MERGED_CONTACT_FEED = "merged_feed_id";
    public static final String CONTACT_FEED = "feed_id";
    public static final String MERGED_FEEDS = "feeds_hashmap";

    public static final String IS_SHOW_UNDO_MERGE = "is_show_undo_merged";

    public static final String SEARCH_SOURCE = "headbox_search_source";

    public static final String TIMES_USED_IN_DAY_COUNT = "times_used_count";

    public static final String AB_FEED_HISTORY = "ab_feed_history";

    public static final String RECORDS_INSERTED = "record_inserted";
    public static String APP_AUTHENTICATED = "app_authenticated";
    public static final String COMPONENTS_DISABLED = "components_disabled";

    // First time feed animation.
    public static final String SHOULD_APPLY_FIRST_TIME_FEED_ANIMATION = "should_apply_first_time_feed_animation";

    public static final String ONBOARDING_FINAL_MSG_APPEARED = "on_board_final_msg_appeared" ;

    /**
     * Indicates whether the last CALL is initiated from the app or not.
     */
    public static final String CALLED_FROM_APP = "called_from_app";

    public static final String USER_APPROVED_SYNC_CONTACTS = "user_approved_sync_contacts";
    public static final String INSTAGRAM_CONTACTS_SYNC_STATUS = "instagram_contacts_sync_status";
    public static final String TWITTER_CONTACTS_SYNC_STATUS = "twitter_contacts_sync_status";
    public static final String FACEBOOK_CONTACTS_SYNC_STATUS = "facebook_contacts_sync_status";


    private PreferencesStorage.SharedPreferencesDetails sharedPreferencesDetails = new PreferencesStorage.SharedPreferencesDetails() {
        @Override
        public String getPreferencesName() {
            return PREF_NAME;
        }

        @Override
        public int getMode() {
            return Application.MODE_PRIVATE;
        }
    };

    public PreferencesManager(Context context) {
        this.context = context;
        preferences = BlinqApplication.preferences;
    }

    /**
     * Initialize the application settings to there default values.
     */
    public void initializeAppSettings() {

        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    }

    public void setAppLoaded(boolean loaded) {

        setProperty(APP_LOADED, loaded);
    }

    public void setContactsLastLoadingTime(long lastLoadingTime) {

        setProperty(CONTACTS_LAST_LOADING_TIME, lastLoadingTime);

    }

    public void setNetworkCountyISO(String value) {
        if (value == null)
            value = "";
        setProperty(NETWORK_COUNTRY_ISO, value);
    }

    public String getNetworkCountyISO() {
        return getProperty(NETWORK_COUNTRY_ISO, null);
    }

    public void setUserRatedTheApp() { setProperty(RATED_THE_APP, true);}

    public boolean getUserRatedTheApp() {return getProperty(RATED_THE_APP, false);}

    /**
     * @return true if the difference between the current time and last loading
     * time is greater than the value of
     * {@see CONTACTS_LOADING_ELAPSED_TIME}
     */
    public boolean canLoadContacts(long currentTime) {

        long lastloading = getProperty(CONTACTS_LAST_LOADING_TIME, (long) 0);
        if (Math.abs(currentTime - lastloading) > CONTACTS_LOADING_ELAPSED_TIME)
            return true;

        return false;
    }

    /**
     * Return true if the app has been loaded before.
     */
    public boolean isLoaded() {
        return getProperty(APP_LOADED, false);
    }

    public void setVersionCode(int value) {

        setProperty(APP_VERSION_CODE, value);

    }

    /**
     * Return installed app version code.
     */
    public int getVersionCode() {
        return getProperty(APP_VERSION_CODE, 0);
    }

    public void setVersionName(String value) {

        setProperty(APP_VERSION_NAME, value);
    }

    /**
     * Return installed app version name.
     */
    public String getVersionName() {
        return getProperty(APP_VERSION_NAME, null);
    }

    /**
     * Set Database upgrade status.
     */
    public void setDatabaseUpgraded(boolean upgraded) {

        setProperty(DB_UPGRADE_STATUS, upgraded);
    }

    /**
     * Return the Database upgrade status.
     */
    public boolean isDatabaseUpgraded() {

        return getProperty(DB_UPGRADE_STATUS, false);
    }

    /**
     * Get the platform contact loading status.
     *
     * @param platform - platform from where to get the loading status.
     * @return the status of loading platform contacts.
     */
    public boolean isContactsLoaded(Platform platform) {

        return getProperty(String.format(CONTACTS_LOADING, platform.name()),
                false);
    }

    /**
     * Set certain platform's contacts loading status.
     */
    public void setContactsLoaded(Platform platform, boolean loaded) {
        setProperty(String.format(CONTACTS_LOADING, platform.name()), loaded);

    }

    /**
     * @param loaded the status of loading contacts details.
     */
    public void setContactsDetailsLoadingStatus(boolean loaded) {

        setProperty(COVERS_LOADING, loaded);

    }

    /**
     * @return the status of loading contacts details.
     */
    public boolean isContactsDetailsLoaded() {

        return getProperty(COVERS_LOADING, false);
    }

    public void setFacebookProfileLoadingStatus(boolean loaded) {

        setProperty(FACEBOOK_PROFILE_LOADED, loaded);
    }

    /**
     * @return true if the difference between the current time and last loading
     * time is greater than the value of
     * {@link PreferencesManager#FACEBOOK_HISTORY_LOADING_ELAPSED_TIME}
     */
    public boolean canLoadFacebookHistory(long currentTime) {

        long lastloading = getProperty(FACEBOOK_HISTORY_LOADING, (long) 0);
        if (Math.abs(currentTime - lastloading) > FACEBOOK_HISTORY_LOADING_ELAPSED_TIME)
            return true;

        return false;
    }

    /**
     * Get the number of use times in a day.
     */
    public int getTimesUsedInDayCount() {
        if (AppUtils.isNewDay(context)) {

            setProperty(TIMES_USED_IN_DAY_COUNT, 0);
        }
        int timesUsedCount = getProperty(TIMES_USED_IN_DAY_COUNT, 0) + 1;
        setProperty(TIMES_USED_IN_DAY_COUNT, timesUsedCount);
        return timesUsedCount;
    }

    /**
     * Set last loading's time to the facebook messages history.
     */
    public void setFacebookHistoryLastLoadingTime(long time) {

        setProperty(FACEBOOK_HISTORY_LOADING, time);
    }

    /**
     * @return true if the difference between the current time and last loading
     * time is greater than the value of
     * {@link PreferencesManager#FACEBOOK_CONTACTS_LOADING_ELAPSED_TIME}
     */
    public boolean canLoadFacebookContacts() {

        long lastLoading = getProperty(FACEBOOK_CONTACT_LOADING, (long) 0);

        if (Math.abs(System.currentTimeMillis() - lastLoading) > FACEBOOK_CONTACTS_LOADING_ELAPSED_TIME) {
            return true;
        }

        return false;
    }

    /**
     * Set last loading/reloading time to the facebook contact.
     */
    public void setFacebookContactsLastLoadingTime() {
        setProperty(FACEBOOK_CONTACT_LOADING, System.currentTimeMillis());
    }

    /**
     * @return true if an internal condition satisfied.
     */
    public boolean canLoadFacebookHistory() {

        int incomingMessagesCount = getProperty(
                INCOMING_FACEBOOK_MESSAGES_COUNTER, 0);
        if (incomingMessagesCount > UPDATE_FACEBOOK_INBOX_AFTER_INCOMING_MESSAGES_COUNT) {
            // Reset the counter
            setFacebookIncomingMessagesCounter(0);
            return true;
        } else {
            // Increment the counter.
            setFacebookIncomingMessagesCounter(incomingMessagesCount + 1);
        }

        return false;
    }

    public void setLastPersonBio(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_BIO, value);
    }



    public boolean getShowedMeCardOf(String value) {
        return getProperty(String.format(SHOWED_ME_CARD_OF, value), false);
    }

    public void setShowedMeCardOf(String value) {
        setProperty(String.format(SHOWED_ME_CARD_OF, value), true);
    }

    public String getLastPersonBio() {
        return getProperty (LAST_PERSON_BIO, null);
    }

    public void setLastPersonTitle(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_TITLE, value);
    }

    public String getLastPersonTitle() {
        return getProperty (LAST_PERSON_TITLE, null);
    }

    public void setLastPersonOrganization(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_ORGANIZATION, value);
    }

    public String getLastPersonOrganization() {
        return getProperty (LAST_PERSON_ORGANIZATION, null);
    }

    public void setLastPersonName(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_NAME, value);
    }

    public String getLastPersonName() {
        return getProperty (LAST_PERSON_NAME, null);
    }

    public void setLastPersonImagePath(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_IMAGE_PATH, value);
    }

    public String getLastPersonImagePath() {
        return getProperty (LAST_PERSON_IMAGE_PATH, null);
    }

    public String getLastNotificationContactName() {
        return getProperty(LAST_NOTIFICATION_CONTACT_NAME, null);
    }

    public void setLastNotificationContactName(String value) {
        if (value == null)
            value = "";
        setProperty(LAST_NOTIFICATION_CONTACT_NAME, value);
    }

    public void setLastPersonSocialProfileAboutMe(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_ABOUT_ME, value);
    }

    public String getLastPersonSocialProfileAboutMe() {
        return getProperty (LAST_PERSON_SOCIAL_ABOUT_ME, null);
    }

    public void setLastPersonSocialProfileAngellist(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_ANGELLIST, value);
    }

    public String getLastPersonSocialProfileAngellist() {
        return getProperty (LAST_PERSON_SOCIAL_ANGELLIST, null);
    }

    public void setLastPersonSocialProfileFoursquare(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_FOURSQUARE, value);
    }

    public String getLastPersonSocialProfileFoursquare() {
        return getProperty (LAST_PERSON_SOCIAL_FOURSQUARE, null);
    }

    public void setLastPersonSocialProfileGithub(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_GITHUB, value);
    }

    public String getLastPersonSocialProfileGithub() {
        return getProperty (LAST_PERSON_SOCIAL_GITHUB, null);
    }

    public void setLastPersonSocialProfileGoogleplus(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_GOOGLE_PLUS, value);
    }

    public String getLastPersonSocialProfileGoogleplus() {
        return getProperty (LAST_PERSON_SOCIAL_GOOGLE_PLUS, null);
    }

    public void setLastPersonSocialProfileGravatar(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_GRAVATAR, value);
    }

    public String getLastPersonSocialProfileGravatar() {
        return getProperty (LAST_PERSON_SOCIAL_GRAVATAR, null);
    }

    public void setLastPersonSocialProfileKlout(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_KLOUT, value);
    }

    public String getLastPersonSocialProfileKlout() {
        return getProperty (LAST_PERSON_SOCIAL_KLOUT, null);
    }

    public void setLastPersonSocialProfileLinkedin(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_LINKEDIN, value);
    }

    public String getLastPersonSocialProfileLinkedin() {
        return getProperty (LAST_PERSON_SOCIAL_LINKEDIN, null);
    }

    public void setLastPersonSocialProfilePicasa(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_PICASA, value);
    }

    public String getLastPersonSocialProfilePicasa() {
        return getProperty (LAST_PERSON_SOCIAL_PICASA, null);
    }

    public void setLastPersonSocialProfilePinterest(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_PINTEREST, value);
    }

    public String getLastPersonSocialProfilePinterest() {
        return getProperty (LAST_PERSON_SOCIAL_PINTEREST, null);
    }


    public void setLastPersonSocialProfileTwitter(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_TWITTER, value);
    }

    public String getLastPersonSocialProfileTwitter() {
        return getProperty (LAST_PERSON_SOCIAL_TWITTER, null);
    }

    public void setLastPersonSocialProfileInstagram(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_INSTAGRAM, value);
    }

    public String getLastPersonSocialProfileInstagram() {
        return getProperty (LAST_PERSON_SOCIAL_INSTAGRAM, null);
    }

    public void setLastPersonSocialProfileFacebook(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_FACEBOOK, value);
    }

    public String getLastPersonSocialProfileFacebook() {
        return getProperty (LAST_PERSON_SOCIAL_FACEBOOK, null);
    }

    public void setLastPersonSocialProfileVimeo(String value) {
        if(value == null)
            value = "";
        setProperty(LAST_PERSON_SOCIAL_VIMEO, value);
    }

    public String getLastPersonSocialProfileVimeo() {
        return getProperty (LAST_PERSON_SOCIAL_VIMEO, null);
    }

    public void setLastPersonSocialProfileFromMerge(Platform platform, long feeId, String userName) {
        setProperty(String.format(MERGED_SOCIAL_PROFILE, platform.getId(), feeId), userName);
    }

    public String getLastPersonSocialProfileFromMerge(Platform platform, long feeId) {
        return getProperty(String.format(MERGED_SOCIAL_PROFILE, platform.getId(), feeId), null);
    }

    public void setLastPersonLikesCount(String count) {
        setProperty(LIKES_COUNT, count);
    }

    public String getLastPersonLikesCount() {
        return getProperty(LIKES_COUNT, null);
    }

    /**
     * Increment or reset the counter of the incoming facebook's messages.
     */
    private void setFacebookIncomingMessagesCounter(int value) {

        setProperty(INCOMING_FACEBOOK_MESSAGES_COUNTER, value);
    }

    /**
     * @return Facebook profile loading status.
     */
    public boolean isFacebookProfileLoaded() {

        return getProperty(FACEBOOK_PROFILE_LOADED, false);
    }

    /**
     * Persist twitter access tokens
     *
     * @param accessToken       - twitter active access token.
     * @param accessTokenSecret - twitter secret token.
     */
    public void setTwitterTokens(String accessToken, String accessTokenSecret) {

        setProperty(TWITTER_ACCESS_TOKEN, accessToken);
        setProperty(TWITTER_ACCESS_TOKEN_SECRET, accessTokenSecret);
        setTWitterLoggedIn(true);
    }

    /**
     * Checks the status of twitter's login
     */
    public Boolean isTWitterLoggedIn() {
        return getProperty(TWITTER_LOGIN_STATUS, false);
    }

    /**
     * Change the the status of twitter's login
     *
     * @param loggedIn - login status.
     */
    public void setTWitterLoggedIn(Boolean loggedIn) {

        setProperty(TWITTER_LOGIN_STATUS, loggedIn);
    }

    /**
     * Get the Twitter access token.
     */
    public String getTwitterAccessToken() {
        return getProperty(TWITTER_ACCESS_TOKEN, null);
    }

    /**
     * Get the Twitter access token secret.
     */
    public String getTwitterAccessTokenSecret() {
        return getProperty(TWITTER_ACCESS_TOKEN_SECRET, null);
    }

    /**
     * @return whether the comlog activity was opened previously
     */
    public boolean isComlogFirstUse() {

        return getProperty(FIRST_COMLOG_USE, true);

    }


    /**
     * @return whether the feed activity was opened previously
     */
    public boolean isAppFirstUse() {

        return getProperty(FIRST_APP_USE, true);

    }

    /**
     * Set a random AB scenario for the feed view.
     */
    public void setupABFeedHistory() {

        //TODO: Set always to full mode - till fixed
        //String scenario = AppUtils.getRandomString(context, R.array.ab_feed_scenarios);
        String scenario = "Full";
        setProperty(AB_FEED_HISTORY, scenario);
    }

    /**
     * Get the result to be displayed when opening the feed view for the first time.
     */
    public int getABFeedHistoryValue() {

        String scenario = getProperty(AB_FEED_HISTORY, AnalyticsConstants.AB_FEED_HISTORY_FULL);

        if (scenario.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_FULL)) {
            return Constants.NUMBER_OF_FEEDS_TO_LOAD_FIRST;
        } else if (scenario.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_FIVE)) {
            return 5;
        } else if (scenario.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_NONE)) {
            return 0;
        }
        return Constants.NUMBER_OF_FEEDS_TO_LOAD_FIRST;
    }

    public boolean calledFromApp() {
        return getProperty(CALLED_FROM_APP, false);
    }


    /**
     * Get specific property with a key from Preferences
     */
    public String getProperty(String propertyKey) {

        PreferencesStorage.PreferenceKey<String> key = new PreferencesStorage.PreferenceKey<String>(propertyKey, sharedPreferencesDetails);
        return preferences.getValue(key, null);
    }

    /*
    *   Set if the onboarding final msg appeared once
     */
    public void setOnboardingFinalMessageAppearedOnce(Boolean appeared) {

        setProperty(ONBOARDING_FINAL_MSG_APPEARED, appeared);
    }

    public void setInstagramContactsSyncStatus(int value) {
        setProperty(INSTAGRAM_CONTACTS_SYNC_STATUS, value);
    }

    public int getInstagramContactsSyncStatus() {
        return getProperty(INSTAGRAM_CONTACTS_SYNC_STATUS, Constants.SYNC_UNKNOWN);
    }

    public void setTwitterContactsSyncStatus(int value) {
        setProperty(TWITTER_CONTACTS_SYNC_STATUS, value);
    }

    public int getTwitterContactsSyncStatus() {
        return getProperty(TWITTER_CONTACTS_SYNC_STATUS, Constants.SYNC_UNKNOWN);
    }

    public void setFacebookContactsSyncSatus(int value) {
        setProperty(FACEBOOK_CONTACTS_SYNC_STATUS, value);
    }

    public int getFacebookContactsSyncSatus() {
        return getProperty(FACEBOOK_CONTACTS_SYNC_STATUS, Constants.SYNC_UNKNOWN);
    }

    public void setContactsSyncApprovedByUser() {
        setProperty(USER_APPROVED_SYNC_CONTACTS, true);
    }

    public boolean getContactsSyncApprovedByUser() {
        return getProperty(USER_APPROVED_SYNC_CONTACTS, false);
    }


    /*
    * Get a boolean that indicates if the onboarding final msg appeared once
     */
    public boolean getOnboardingFinalMessageAppearedOnce(){
        return getProperty(ONBOARDING_FINAL_MSG_APPEARED, false);
    }


    /**
     * Get a general property from Preferences
     *
     * @param propertyKey - preference key.
     * @param defValue    - value to return if this preference does not exist.
     * @return preference value
     */
    public <Type> Type getProperty(String propertyKey, Type defValue) {
        PreferencesStorage.PreferenceKey<Type> key = new PreferencesStorage.PreferenceKey<Type>(propertyKey, sharedPreferencesDetails);
        return preferences.getValue(key, defValue);
    }

    /**
     * Set a general preferences property.
     *
     * @param propertyKey - preference key.
     * @param value       - value to be set.
     */
    public <Type> void setProperty(String propertyKey, Type value) {

        PreferencesStorage.PreferenceKey<Type> key = new PreferencesStorage.PreferenceKey<Type>(propertyKey, sharedPreferencesDetails);
        preferences.putValue(key, value);
    }

}
