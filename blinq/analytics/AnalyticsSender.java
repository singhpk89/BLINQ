package com.blinq.analytics;

import android.content.Context;

import com.blinq.PreferencesManager;
import com.blinq.models.Platform;
import com.blinq.models.social.window.StatusContent;
import com.blinq.utils.AppUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Johan Hansson on 9/17/2014.
 * <p/>
 * Used to centralize all application events to be sent.
 */
public class AnalyticsSender {

    private final Context context;
    private final BlinqAnalytics analyticsManager;
    private final PreferencesManager preferencesManager;

    public AnalyticsSender(Context context) {

        this.context = context;
        this.analyticsManager = new BlinqAnalytics(context);
        this.preferencesManager = new PreferencesManager(context);
    }


    /**
     * Send event when social window item scrolled.
     *
     * @param direction scroll direction up/down.
     */
    public void sendSocialWindowScrollEvent(String direction) {

        analyticsManager.sendEvent(
                AnalyticsConstants.SOCIAL_WINDOW_SCROLL_EVENT,
                AnalyticsConstants.SOCIAL_WINDOW_DIRECTION_PROPERTY, direction,
                false, AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send event when social item clicked.
     *
     * @param platform platform of the clicked item.
     * @param type     the type of the content - video, image, event
     */
    public void sendSocialWindowClickEvent(Platform platform, StatusContent type) {

        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put(AnalyticsConstants.PLATFORM_PROPERTY, platform.getName());

        analyticsManager.sendEvent(
                AnalyticsConstants.SOCIAL_WINDOW_CLICKED_EVENT,
                properties, true,
                AnalyticsConstants.ACTION_CATEGORY);

        if (type != null) {
            analyticsManager.sendEvent(
                    AnalyticsConstants.SOCIAL_WINDOW_TYPE_CLICKED_EVENT,
                    properties, true,
                    AnalyticsConstants.ACTION_CATEGORY);
        }
    }

    /**
     * Send event when click on the user name in instant message activity action bar.
     */
    public void sendInstantMessageActionBarUserNameClickedEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.SOCIAL_WINDOW_CLICKED_SHOW_EVENT,
                false, AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send event when user avatar in instant message activity clicked.
     */
    public void sendInstantMessageActionBarAvatarClickedEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.COMLOG_AVATAR_EVENT, false,
                AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send event when user call contact from instant message action bar.
     */
    public void setInstantMessageActionBarCallContactEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.COMLOG_CALL_EVENT, true,
                AnalyticsConstants.COMMUNICATION_CATEGORY);

    }

    /**
     * Send event when instant message refreshed.
     */
    public void setInstantMessageRefreshEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.COMLOG_REFRESH_EVENT, false,
                AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send event when user long click instant message list item to show contextual action bar.
     */
    public void setInstantMessageLongClickOnListItem() {

        analyticsManager.sendEvent(
                AnalyticsConstants.LONG_CLICK_ON_MESSAGE_EVENT, false,
                AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send event when message from instant message list has delete.
     */
    public void setDeleteMessageFromInstantMessageListEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.DELETE_MESSAGE_EVENT, false,
                AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send event when message from instant message list has copied.
     */
    public void setCopyMessageFromInstantMessageListEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.COPY_MESSAGE_EVENT, false,
                AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send event when retro dialer opened from account flipper.
     */
    public void setOpenRetroDialerEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.RETRO_DIALER_CLICKED_EVENT, false,
                AnalyticsConstants.COMMUNICATION_CATEGORY);
    }

    /**
     * Send event when send message button pressed in instant message.
     *
     * @param platform platform to send message on.
     */
    public void sendSendMessageButtonClickedEvent(Platform platform) {

        analyticsManager.sendEvent(
                AnalyticsConstants.SEND_BUTTON_CLICKED_EVENT,
                AnalyticsConstants.TYPE_PROPERTY, platform.name(),
                true, AnalyticsConstants.COMMUNICATION_CATEGORY);
    }

    /**
     * Send event when platform switched in instant message.
     *
     * @param properties analytics properties.
     */
    public void sendPlatformSwitchEvent(HashMap<String, Object> properties) {

        analyticsManager.sendEvent(
                AnalyticsConstants.SWITCH_PLATFORM_EVENT, properties,
                false, AnalyticsConstants.COMMUNICATION_CATEGORY);
    }

    /**
     * Send event when back button pressed in instant message activity.
     */
    public void sendInstantMessageBackButtonPressedEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.BACK_BUTTON_COMLOG_CLICKED_PROPERTY,
                false, AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send event when th user click on the text box in the comlog
     */
    public void sendMessageInputClickedEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.CLICKED_ON_CONVERSION_TEXT_EVENT,
                true, AnalyticsConstants.COMMUNICATION_CATEGORY);
    }

    /**
     * Send event when the contacts Sync Started for a certain platform.
     */
    public void sendContactSyncEvent(String syncState, Platform platform) {

        String eventName = AnalyticsConstants.CONTACTS_SYNC_EVENT + syncState;
        analyticsManager.sendEvent(eventName,
                AnalyticsConstants.TYPE_PROPERTY,
                platform.getName(), false,
                AnalyticsConstants.ONBOARDING_CATEGORY);

    }

    public void sendSuccessConnectionEvent(Platform platform) {
        String platformName = platform.name().toLowerCase();

        String successConnectEvent = String.format(
                AnalyticsConstants.SUCCESS_CONNECT_EVENT, platformName,
                AnalyticsConstants.LOGIN_FROM_LOGIN_SCREEN);
        analyticsManager.sendEvent(successConnectEvent, true, AnalyticsConstants.ONBOARDING_CATEGORY);
    }

    public void sendLinkedInButtonClickedEvent() {
        String loggedInEvent = String.format(
                AnalyticsConstants.LOGGED_IN_EVENT, Platform.LINKEDIN.name().toLowerCase(),
                AnalyticsConstants.LOGIN_FROM_LOGIN_SCREEN);
        analyticsManager.sendEvent(loggedInEvent, true, AnalyticsConstants.ONBOARDING_CATEGORY);
    }


    /**
     * Send event when com-log screen opened.
     */
    public void sendOpenComlogEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.OPENED_COMLOG_SCREEN_EVENT, false,
                AnalyticsConstants.ACTION_CATEGORY);
    }


    /**
     * Send event when com-log back pressed.
     */
    public void sendComlogBackPressedEvent() {

        analyticsManager.sendEvent(
                AnalyticsConstants.BACK_COMLOG_CLICKED_PROPERTY,
                false, AnalyticsConstants.ACTION_CATEGORY);
    }


    /**
     * Send event when app started.
     */
    public void sendEnteringAppEvent() {

        analyticsManager.sendEvent(AnalyticsConstants.ENTERING_APP_EVENT,
                AnalyticsConstants.FROM_PROPERTY,
                AnalyticsConstants.NOTIFICATION_VALUE, true,
                AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send Daily used Mixpanel event
     */
    public void sendDailyUsedEvent() {

        if (!AppUtils.isNewDay(context))
            return;

        int dayUsed = preferencesManager.getProperty(
                PreferencesManager.DAY_USED_COUNT, 0) + 1;

        analyticsManager.setUserProfileProperty(
                AnalyticsConstants.PROFILE_DAY_USED_COUNT_PROPERTY,
                Integer.valueOf(dayUsed));

        if (dayUsed < AnalyticsConstants.DAY_USED_COUNT_THRESHOLD
                || dayUsed > AnalyticsConstants.DAY_USED_COUNT_MAX_THRESHOLD) {

            analyticsManager.sendEvent(
                    AnalyticsConstants.PROFILE_DAY_USED_COUNT_PROPERTY,
                    AnalyticsConstants.PROFILE_DAY_USED_COUNT_PROPERTY,
                    String.valueOf(dayUsed), false,
                    AnalyticsConstants.ONBOARDING_CATEGORY);
        } else {
            // For day 1 to 7 flatten the event
            String dayUsedEvent = String.format(
                    AnalyticsConstants.DAY_USED_EVENT, dayUsed);
            analyticsManager.sendEvent(dayUsedEvent, true,
                    AnalyticsConstants.ONBOARDING_CATEGORY);
        }

        preferencesManager.setProperty(PreferencesManager.DAY_USED_COUNT,
                dayUsed);

        saveNewDay();
    }

    /**
     * Save new day entry of user to the application
     */
    private void saveNewDay() {

        final String SIMPLE_DATE_FORMAT = "dd/MM/yyyy";
        SimpleDateFormat dateFormater = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
        String nowDate = dateFormater.format(new Date());
        preferencesManager.setProperty(PreferencesManager.USER_ENTERED_LAST_DAY, nowDate);

    }

    /**
     * Send event if the socialize action on the notification item clicked.
     *
     * @param platform platform to send the event for.
     */
    public void sendNotificationSocializeActionClicked(Platform platform) {

        analyticsManager.sendEvent(
                AnalyticsConstants.NOTIFICATION_CLICKED_ON_SOCIALIZE,
                AnalyticsConstants.TYPE_PROPERTY, platform.name(),
                true, AnalyticsConstants.COMMUNICATION_CATEGORY);
    }

    /**
     * Send Mixpanel event for changed stream notification
     */
    public void sendNotificationChangeEvent(boolean isEnabled) {

        String status = isEnabled ? AnalyticsConstants.ENABLED_VALUE
                : AnalyticsConstants.DISABLED_VALUE;

        String event = String.format(
                AnalyticsConstants.NOTIFICATION_CHANGE_EVENT, status);
        analyticsManager.sendEvent(event, true,
                AnalyticsConstants.ONBOARDING_CATEGORY);

        analyticsManager.setUserProfileProperty(
                AnalyticsConstants.STREAM_NOTIFICATIONS_PROPERTY, isEnabled);
    }

    public void sendNotificationsAnalytics(Platform platform) {

        if (platform == null)
            return;

        analyticsManager.sendEvent(AnalyticsConstants.RECEIVE_ANY_NOTIFICATION_EVENT,
                AnalyticsConstants.TYPE_PROPERTY, platform.name().toLowerCase()
                , true, AnalyticsConstants.COMMUNICATION_CATEGORY);

    }

    /**
     * Send event with how many people were replaced after updating the top friends list.
     *
     * @param peopleNumber - number of the replaced friends.
     */
    public void sendTopFriendsReplacedEvent(int peopleNumber) {

        analyticsManager.sendEvent(
                AnalyticsConstants.UPDATE_TOP_FRIENDS_EVENT,
                AnalyticsConstants.UPDATE_TOP_FRIEND_PROPERTY_REPLACED, peopleNumber,
                true, AnalyticsConstants.ONBOARDING_CATEGORY);
    }

    public void sendSocialWindowArrowClicked() {

        analyticsManager.sendEvent(
                AnalyticsConstants.SOCIAL_WINDOW_ARROW_CLICKED_EVENT,
                true, AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Send a click event on user profile in the drawer
     */
    public void sendDrawerProfileClicked() {
        analyticsManager.sendEvent(
                AnalyticsConstants.DRAWER_PROFILE_CLICKED_EVENT,
                true, AnalyticsConstants.ACTION_CATEGORY);
    }

    public void sendSocialWindowArrowSwiped() {
        analyticsManager.sendEvent(
                AnalyticsConstants.SOCIAL_WINDOW_ARROW_SWIPPED_EVENT,
                true, AnalyticsConstants.ACTION_CATEGORY);
    }

    public void sendShowTheDot(Platform platform) {
        analyticsManager.sendEvent(
                AnalyticsConstants.SHOW_THE_DOT_EVENT,
                AnalyticsConstants.TYPE_PROPERTY, platform.name().toLowerCase(),
                true, AnalyticsConstants.ACTION_CATEGORY);
    }

    public void sendClickedTheDot(Platform platform) {
        if(platform == null) {
            platform = Platform.NOTHING;
        }
        analyticsManager.sendEvent(
                AnalyticsConstants.CLICKED_THE_DOT_EVENT,
                AnalyticsConstants.TYPE_PROPERTY, platform.name().toLowerCase(),
                true, AnalyticsConstants.ACTION_CATEGORY);
    }

    public void sendUnknownNotification(String platformPackageName) {
        analyticsManager.sendEvent(
                AnalyticsConstants.RECEIVE_ANY_NOTIFICATION_UNKNOWEN_PLATFORM_EVENT,
                AnalyticsConstants.TYPE_PROPERTY, platformPackageName,
                true, AnalyticsConstants.ACTION_CATEGORY);
    }

    public void sendShowFinalMessagePopup() {
        analyticsManager.sendEvent(
                AnalyticsConstants.RECEIVE_CONGRATS_POPUP_EVENT,
                true, AnalyticsConstants.ACTION_CATEGORY);
    }

    public void sendMergedAPersonSuccessfully() {
        analyticsManager.sendEvent(
                AnalyticsConstants.MERGED_A_PERSON_SUCCESSFULLY,
                true, AnalyticsConstants.ACTION_CATEGORY);
    }
}