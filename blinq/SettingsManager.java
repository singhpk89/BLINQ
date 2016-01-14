package com.blinq;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.blinq.analytics.AnalyticsConstants;
import com.blinq.models.Platform;
import com.blinq.utils.AppUtils;

/**
 * Manages and encapsulates headbox settings.
 * TODO : Connect with AppPreference.
 *
 * @author Johan Hansson.
 */
public class SettingsManager {

    public static final boolean ENABLE_NOTIFICATION_DEFAULT_VALUE = true;
    private Context context;
    private SharedPreferences settingsPreferences;

    public SettingsManager(Context context) {
        this.context = context;
        settingsPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

    }

    public boolean isNotificationEnabled(Platform platform) {

        switch (platform) {
            case CALL:
                return settingsPreferences.getBoolean(context
                                .getString(R.string.settings_calls_notifications_key),
                        ENABLE_NOTIFICATION_DEFAULT_VALUE
                );

            case FACEBOOK:
                return settingsPreferences.getBoolean(context
                                .getString(R.string.settings_facebook_notifications_key),
                        ENABLE_NOTIFICATION_DEFAULT_VALUE
                );

            case HANGOUTS:
                return settingsPreferences.getBoolean(context
                                .getString(R.string.settings_hangouts_notifications_key),
                        ENABLE_NOTIFICATION_DEFAULT_VALUE
                );

            case SMS:
            case MMS:
                return settingsPreferences.getBoolean(
                        context.getString(R.string.settings_sms_notifications_key),
                        ENABLE_NOTIFICATION_DEFAULT_VALUE);
            case EMAIL:
            case SKYPE:
            case WHATSAPP:
                return isInboundMessagesEnabled(platform);
            default:
                return ENABLE_NOTIFICATION_DEFAULT_VALUE;

        }
    }

    public boolean isInboundMessagesEnabled(Platform platform) {

        switch (platform) {
            case EMAIL:
                return settingsPreferences.getBoolean(context
                                .getString(R.string.settings_email_notifications_key),
                        !ENABLE_NOTIFICATION_DEFAULT_VALUE
                );

            case SKYPE:
                return settingsPreferences.getBoolean(context
                                .getString(R.string.settings_skype_notifications_key),
                        !ENABLE_NOTIFICATION_DEFAULT_VALUE
                );

            case WHATSAPP:
                return settingsPreferences.getBoolean(context
                                .getString(R.string.settings_whatsapp_notifications_key),
                        !ENABLE_NOTIFICATION_DEFAULT_VALUE
                );
            default:
                return !ENABLE_NOTIFICATION_DEFAULT_VALUE;

        }
    }

    public void setInboundPlatformEnabled(Platform platform, boolean enabled) {

        Editor editor = settingsPreferences.edit();

        switch (platform) {
            case EMAIL:
                editor.putBoolean(context
                                .getString(R.string.settings_email_notifications_key),
                        enabled
                );
                break;

            case SKYPE:

                editor.putBoolean(context
                                .getString(R.string.settings_skype_notifications_key),
                        enabled
                );
                break;
            case WHATSAPP:

                editor.putBoolean(context
                                .getString(R.string.settings_whatsapp_notifications_key),
                        enabled
                );
                break;
        }
        editor.commit();

    }

    /**
     * Set the value of notification
     */
    public void setNotificationEnabled(Platform platform, boolean enabled) {

        Editor editor = settingsPreferences.edit();

        switch (platform) {
            case CALL:
                editor.putBoolean(context
                                .getString(R.string.settings_calls_notifications_key),
                        enabled
                );
                break;

            case FACEBOOK:

                editor.putBoolean(context
                                .getString(R.string.settings_facebook_notifications_key),
                        enabled
                );
                break;
            case HANGOUTS:

                editor.putBoolean(context
                                .getString(R.string.settings_hangouts_notifications_key),
                        enabled
                );
                break;

            case SMS:
                editor.putBoolean(
                        context.getString(R.string.settings_sms_notifications_key),
                        enabled);
                break;

        }
        editor.commit();
    }

    /**
     * Return the selected application theme from settings.
     *
     * @return the application theme.
     */
    public String getTheme() {

        return settingsPreferences.getString(
                context.getString(R.string.settings_theme_key), null);

    }


    /**
     * Set the default notification APP setting as enabled or disabled.
     */
    public void setAsDefaultNotificationApp(boolean enabled) {

        Editor editor = settingsPreferences.edit();
        editor.putBoolean(
                context.getString(R.string.settings_default_notification_app_internal_key),
                enabled);
        editor.commit();
    }

    /**
     * Return whether the default notification app setting is enabled or not.
     */
    public boolean isDefaultNotificationApp() {

        return settingsPreferences
                .getBoolean(
                        context.getString(R.string.settings_default_notification_app_internal_key),
                        false);
    }

    /**
     * @return true if sounds source from Headbox resources, false if not.
     */
    public boolean isSoundsSourceFromHeadbox() {

        return settingsPreferences.getBoolean(
                context.getString(R.string.settings_sound_source_key), false);
    }

    /**
     * Get the value of AB Notification value based on the user selection.
     */
    public String getABNotificationValue() {
        String abNotificationValue = null;

        if (isNotificationEnabled(Platform.CALL)
                && isNotificationEnabled(Platform.SMS)
                && isNotificationEnabled(Platform.FACEBOOK)
                && isNotificationEnabled(Platform.HANGOUTS)) {
            abNotificationValue = AnalyticsConstants.ALL_NOTIFICATION;
        } else if (isNotificationEnabled(Platform.CALL)
                && isNotificationEnabled(Platform.SMS)
                && !isNotificationEnabled(Platform.FACEBOOK)
                && !isNotificationEnabled(Platform.HANGOUTS)) {
            abNotificationValue = AnalyticsConstants.ONLY_CALL_AND_SMS_NOTFIFICATION;
        }
        return abNotificationValue;
    }

    /**
     * Setup the random default AB notification
     */
    public void setupABNotificationGroup() {

        setNotificationEnabled(Platform.CALL, false);
        setNotificationEnabled(Platform.SMS, false);

        if (AppUtils.getRandomString(context, R.array.ab_notification)
                .equalsIgnoreCase(AnalyticsConstants.ALL_NOTIFICATION)) {
            setNotificationEnabled(Platform.FACEBOOK, true);
            setNotificationEnabled(Platform.HANGOUTS, true);

        } else {
            setNotificationEnabled(Platform.FACEBOOK, false);
            setNotificationEnabled(Platform.HANGOUTS, false);
        }

    }
}
