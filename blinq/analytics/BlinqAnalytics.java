package com.blinq.analytics;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AppEventsLogger;
import com.facebook.Settings;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.blinq.PreferencesManager;
import com.blinq.utils.AppUtils;
import com.blinq.utils.StringUtils;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Responsible for sending analytics to mixpanel, google, facebook and more
 */
public class BlinqAnalytics extends Analytics {

    public static final String TAG = BlinqAnalytics.class.getSimpleName();

    private static final int MAX_EVENT_LENGTH = 39;

    private AppEventsLogger facebookLogger;
    private MixpanelAPI mixpanel;
    private EasyTracker googleTracker;

    public BlinqAnalytics(Context context) {
        this.context = context;
        this.isAnalyticsEnabled = !AppUtils.isDebug(context);
        loadAnalyticsPreferences();
        configureMixpanel();
        specifyUserIdentify();
        configureFacebookEvents();
        configureGoogleEvents();
    }

    private void loadAnalyticsPreferences() {
        this.isAnalyticsEnabled = true;
    }

    private void configureGoogleEvents() {
        googleTracker = EasyTracker.getInstance(context);
    }

    private void configureMixpanel() {
        this.mixpanel = MixpanelAPI.getInstance(context,
                AnalyticsConstants.MIXPANEL_API_TOKEN);
    }

    private void configureFacebookEvents() {
        facebookLogger = AppEventsLogger.newLogger(context);
        Settings.setAppVersion(AppUtils.getVersionName(context));
    }

    private void specifyUserIdentify() {
        String distinctUserId = getTrackingUserDistinctId();
        specifyMixpanelUserIdentity(distinctUserId);
    }

    /**
     * Set the user identity in mixpanel
     */
    private void specifyMixpanelUserIdentity(String distinctUserId) {
        mixpanel.identify(distinctUserId);
        mixpanel.getPeople().identify(distinctUserId);
        mixpanel.getPeople().initPushHandling(
                AnalyticsConstants.ANDROID_PUSH_SENDER_ID);
    }

    /**
     * Get the user's distinct ID
     */
    private String getTrackingUserDistinctId() {
        SharedPreferences preferences = context.getSharedPreferences(
                AnalyticsConstants.MIXAPNEL_PREFERENCES_NAME,
                Context.MODE_PRIVATE);

        String userId = preferences.getString(
                AnalyticsConstants.MIXPANEL_DISTINCT_ID_NAME, null);

        if (userId == null) {
            // generate new user Id
            userId = AppUtils.generateUserDistinctId(context);
            final SharedPreferences.Editor preferencesEditor = preferences
                    .edit();
            preferencesEditor.putString(
                    AnalyticsConstants.MIXPANEL_DISTINCT_ID_NAME, userId);
            preferencesEditor.commit();
        }

        return userId;
    }

    @Override
    public void sendEvent(String eventName) {
        HashMap<String, Object> properties = new HashMap<String, Object>();
        sendEvent(eventName, properties);
    }

    private String fixEventName(String eventName) {
        if (StringUtils.isBlank(eventName))
            return StringUtils.EMPTY_STRING;

        eventName = eventName.replaceAll("[^a-zA-Z0-9\\s]",
                StringUtils.EMPTY_STRING).trim();

        if (eventName.length() > MAX_EVENT_LENGTH) {
            return eventName.substring(0, MAX_EVENT_LENGTH);
        }

        return eventName;
    }

    @Override
    public void sendEvent(String eventName, String propertyName,
                          Object propertyValue) {
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put(propertyName, propertyValue);
        sendEvent(eventName, properties);
    }

    @Override
    public void complete() {
        mixpanel.flush();
    }

    @Override
    public void sendEvent(String eventName,
                          HashMap<String, Object> propertiesHash) {
        if (isAnalyticsEnabled) {
            Log.i(TAG, "Analytics is not enabled");
            return;
        }
        try {
            String propertiesString = convertHashMapToString(propertiesHash);
            Log.i(TAG, "Sent event:" + eventName + " with "
                    + propertiesString);

            JSONObject properties = new JSONObject(propertiesHash);
            mixpanel.track(eventName, properties);

            Bundle parameters = convertHashMapToBundle(propertiesHash);
            facebookLogger.logEvent(fixEventName(eventName), parameters);

            googleTracker.send(MapBuilder.createEvent(
                    AnalyticsConstants.DEFAULT_CATEGORY_VALUE, eventName,
                    propertiesString, null).build());
        } catch (OutOfMemoryError outOfMemoryError) {
            Log.e(TAG, "Analytics OutOfMemoryError:" + outOfMemoryError.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Analytics Error:" + e.getMessage());
        }

    }

    /**
     * Convert the hashmap properties to a "key1:valu1e key2:value2 ...." label.
     */
    private String convertHashMapToString(HashMap<String, Object> propertiesHash) {
        String result = StringUtils.EMPTY_STRING;
        for (String parameter : propertiesHash.keySet()) {
            result += getLabel(fixEventName(parameter),
                    String.valueOf(propertiesHash.get(parameter)))
                    + StringUtils.SPACE;
        }

        return result;
    }

    /**
     * Convert the hash of properties to bundle to be send by
     * com.facebook.AppEventsLogger
     */
    private Bundle convertHashMapToBundle(HashMap<String, Object> propertiesHash) {
        Bundle parameters = new Bundle();
        for (String parameter : propertiesHash.keySet()) {
            parameters.putString(fixEventName(parameter),
                    String.valueOf(propertiesHash.get(parameter)));
        }
        return parameters;
    }

    @Override
    public void setUserProfileProperty(String propertyName, Object propertyValue) {

        if (!isAnalyticsEnabled) {
            Log.i(TAG, "Analytics is not enabled");
            return;
        }
        try {
            String propertyValueStr = String.valueOf(propertyValue);
            String eventName = AnalyticsConstants.SET_USER_PROFILE_EVENT;
            mixpanel.getPeople().set(propertyName, propertyValue);

            // only mixpanel truly support profiles. the rest are just
            // logging an event
            Log.i(TAG, "Sent event - Set user profile:" + propertyName
                    + " " + propertyValueStr);

            Bundle parameters = new Bundle();
            parameters.putString(fixEventName(propertyName),
                    propertyValueStr);

            facebookLogger.logEvent(fixEventName(eventName), parameters);
            String propertiesLabel = getLabel(propertyName,
                    propertyValueStr);
            googleTracker.send(MapBuilder.createEvent(
                    AnalyticsConstants.USER_INFO_CATEGORY, eventName,
                    propertiesLabel, null).build());
        } catch (OutOfMemoryError outOfMemoryError) {
            Log.e(TAG, "Analytics OutOfMemoryError:" + outOfMemoryError.getMessage()
            );
        } catch (Exception e) {

            Log.e(TAG, "Analytics Error:" + e.getMessage());
        }
    }

    /**
     * connect the property name and value in one label as
     * 'propertyName:propertyValue'
     */
    private String getLabel(String propertyName, String propertyValue) {
        return propertyName + AnalyticsConstants.PROPERITES_SEPARATOR
                + propertyValue;
    }

    /**
     * Show Mixpanel's Survey or Notification if available for the user
     */
    public static void showAllMixpanelUpdates(Activity activity) {
        showSurveyIfAvailable(activity);
        showNotificationIfAvailable(activity);
    }

    /**
     * Show Mixpanel's Survey if available for the user
     */
    public static void showSurveyIfAvailable(Activity activity) {
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(
                activity.getApplicationContext(),
                AnalyticsConstants.MIXPANEL_API_TOKEN);
        mixpanel.getPeople().showSurveyIfAvailable(activity);
    }

    /**
     * Show Mixpanel's Notification if available for the user
     */
    public static void showNotificationIfAvailable(Activity activity) {
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(
                activity.getApplicationContext(),
                AnalyticsConstants.MIXPANEL_API_TOKEN);
        mixpanel.getPeople().showNotificationIfAvailable(activity);
    }

    @Override
    public void sendEvent(String eventName, boolean sendToMixpanel,
                          String category) {
        HashMap<String, Object> properties = new HashMap<String, Object>();
        sendEvent(eventName, properties, sendToMixpanel, category);
    }

    @Override
    public void sendEvent(String eventName, String propertyName,
                          Object propertyValue, boolean sendToMixpanel, String category) {
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put(propertyName, propertyValue);
        sendEvent(eventName, properties, sendToMixpanel, category);
    }

    @Override
    public void sendEvent(String eventName, HashMap<String, Object> propertiesHash,
                          boolean sendToMixpanel, String category) {
        if (!isAnalyticsEnabled) {
            Log.i(TAG, "Analytics is not enabled");
        }
        try {
            String propertiesString = convertHashMapToString(propertiesHash);
            Log.i(TAG, "Sent event:" + eventName + " with "
                    + propertiesString + " category:" + category);

            if (sendToMixpanel) {
                JSONObject properties = new JSONObject(propertiesHash);
                mixpanel.track(eventName, properties);
            }

            Bundle parameters = convertHashMapToBundle(propertiesHash);
            facebookLogger.logEvent(fixEventName(eventName), parameters);

            googleTracker.send(MapBuilder.createEvent(category, eventName,
                    propertiesString, null).build());
        } catch (OutOfMemoryError outOfMemoryError) {
            Log.e(TAG, "Analytics OutOfMemoryError:" + outOfMemoryError.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Analytics Error:" + e.getMessage());
        }
    }

}
