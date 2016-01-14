package com.blinq.service.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.blinq.BlinqApplication;
import com.blinq.MeCardHolder;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.SettingsManager;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.models.MemberContact;
import com.blinq.models.NotificationData;
import com.blinq.models.Platform;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.service.DotManager;
import com.blinq.service.FloatingDotService;
import com.blinq.ui.activities.PopupCheckboxFinalMsg;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Log;
import com.blinq.utils.PackageUtils;
import com.blinq.utils.ServerUtils;
import com.blinq.utils.StringUtils;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A service that get trigger from the system when new notifications are
 * posted or removed.
 * <p/>
 * For Android Version 4.3 and higher.
 * <p/>
 * {@link ://developer.android.com/reference/android/service/notification/
 * NotificationListenerService.html}
 *
 * @author Johan Hansson.
 */
@SuppressLint({"NewApi", "Override"})
public class HeadboxNotificationListenerService extends
        NotificationListenerService implements ServerUtils.OnGetContactInformationListener {

    private String TAG = this.getClass().getSimpleName();
    private SettingsManager settingsManager;
    private PreferencesManager preferencesManager;
    private SharedPreferences sharedPreferences;

    private StatusBarNotification statusBarNotification;
    private Uri feedUri;
    Platform notificationPlatform;

    private NotificationParser notificationParser;

    private String contactName;

    private long feedId;

//    private static List<Platform> INBOUND_PLATFORMS = new ArrayList<Platform>() {
//        private static final long serialVersionUID = 4707177610685408281L;
//        {
//            add(Platform.SMS);
//            add(Platform.EMAIL);
//            add(Platform.SKYPE);
//            add(Platform.WHATSAPP);
//            add(Platform.FACEBOOK);
//            add(Platform.HANGOUTS);
//        }
//    };

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(TAG, "Enable Blinq Notification Listener Service");

        preferencesManager = new PreferencesManager(this);
        settingsManager = new SettingsManager(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        settingsManager.setAsDefaultNotificationApp(true);

        if (!preferencesManager.getOnboardingFinalMessageAppearedOnce()) {
            startPopupCheckboxFinalMsgActivity(HeadboxNotificationListenerService.this);
            sharedPreferences.edit().putString(getString(R.string.enabled_apps_key),
                    getString(R.string.enabled_apps_default_all)).apply();
            sendNotificationChangeEvent(true);
            showWelcomeOnboardNotification();
        }
        notificationParser = new NotificationParser(
                getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Disable Blinq Notification Listener Service");

        settingsManager = new SettingsManager(getApplicationContext());
        settingsManager.setAsDefaultNotificationApp(false);
        sendNotificationChangeEvent(false);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

    }

    /**
     * We are listening to notification removal - it means that eithr:
     * User clicked on it OR user dismiss it - This is our trigger
     */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

        synchronized (this) {

            if (sbn == null) {
                Log.d(TAG, "Null notification detected");
                return;
            }
            try {
                parseNotification(sbn);
            } catch (Exception e) {
                Crashlytics.logException(e);
                Log.e(TAG, e.getMessage());
            }

        }

    }

    long start;

    /**
     * Parse and manage the appearance of the posted notification.
     */
    private void parseNotification(StatusBarNotification sbn) {



        start = System.currentTimeMillis();
        statusBarNotification = sbn;

        final Notification notification = sbn.getNotification();

        if (notification == null)
            return;

        String packageName = sbn.getPackageName();

        if (StringUtils.isBlank(packageName)) {
            Log.d(TAG, "package name is black");
            return;
        }

        notificationPlatform = notificationParser.mapPackageToPlatform(packageName);

        if (notificationPlatform == null) {
            if(!PackageUtils.isIgnorePackageName(packageName)) {
                BlinqApplication.analyticsSender.sendUnknownNotification(packageName);
            }
            return;
        }

        sendNotificationsAnalytics(notificationPlatform);

        notificationParser = new NotificationParser(getApplicationContext());
        List<NotificationData> notificationData = notificationParser
                .parseNotification(notification, packageName,
                        sbn.getId(), sbn.getTag());

        List<Platform> enabledPlatforms = getEnabledPlatforms();

        if (!enabledPlatforms.contains(notificationPlatform) || notificationData.size() == 0)
            return;

        closeDot(notificationPlatform);

        NotificationData data = getNotification(notificationData, notificationPlatform);

        getContactName(data, notificationPlatform);

        if (isFacebookGroupChat()) {
            return;
        }

        Log.d(TAG, "time to decide " + AppUtils.findTime(start));

        feedUri = FeedProviderImpl.getInstance(getApplicationContext()).insertNotification(data);

        if (feedUri == null)
            return;

        Log.d(TAG, "time to get feed id " + AppUtils.findTime(start));

        startDotService(feedUri, statusBarNotification.getPackageName(), notificationPlatform);

        feedId = Integer.parseInt(feedUri.getPathSegments().get(1));

        HashMap<Platform, List<MemberContact>> contacts =
                FeedProviderImpl.getInstance(getApplicationContext()).getContacts(feedId);

        setPreferencesLastContactName(contacts);

        ServerUtils.getInstance(this).getContactInformation(contacts, feedId, this);
    }

    private boolean isFacebookGroupChat() {
        return notificationPlatform == Platform.FACEBOOK && contactName.contains(", ");
    }

    private void setPreferencesLastContactName(HashMap<Platform, List<MemberContact>> contacts) {
        if(contacts.get(Platform.FACEBOOK) != null) {
            String name = contacts.get(Platform.FACEBOOK).get(0).getContactName();
            preferencesManager.setLastNotificationContactName(name);
        }
        for(Platform p: contacts.keySet()) {
            String name = contacts.get(p).get(0).getContactName();
            preferencesManager.setLastNotificationContactName(name);
        }
    }

    private NotificationData getNotification(List<NotificationData> list, Platform platform) {
        NotificationData data = list.get(0);
        if (list.size() == 1) {
            return data;
        }
        if (platform == Platform.FACEBOOK || platform == Platform.WHATSAPP) {
            data.setText(list.get(1).getContent());
        }
        return data;
    }

    private List<Platform> getEnabledPlatforms() {
        List<Platform> result = new ArrayList<Platform>();
        String platformsFromPreferences = sharedPreferences.getString(getString(R.string.enabled_apps_key), "");
        if (StringUtils.isBlank(platformsFromPreferences)) {
            return result;
        }
        for (String platform : platformsFromPreferences.split("=")) {
            result.add(Platform.fromId(Integer.valueOf(platform)));
        }
        return result;
    }

    private void getContactName(NotificationData data, Platform platform) {
        if (platform == Platform.SMS || platform == Platform.MMS) {
            String title = data.getTitle().toString();
            if (title.contains(":")) {
                data.setTitle(title.substring(0, title.indexOf(":")));
            }
        }
        contactName = data.getTitle().toString();
        preferencesManager.setLastNotificationContactName(contactName);
    }

    private void closeDot(Platform platform) {
        Intent intent = new Intent(this, FloatingDotService.class);
        intent.setAction(FloatingDotService.CLOSE_DOT_ACTION);
        intent.putExtra(FloatingDotService.PLATFORM_EXTRA_TAG, platform.getId());
        startService(intent);
    }

    private void startDotService(Uri uri, String packageName, final Platform platform) {
        MeCardHolder.getInstance().clearData();
        DotManager dotManager = DotManager.getInstance();
        int feedId = Integer.parseInt(uri.getPathSegments().get(1));
        dotManager.startNotificationDotServiceIfShould(this, feedId, platform, packageName);
    }

    private void sendNotificationsAnalytics(Platform platform) {
        BlinqApplication.analyticsSender.sendNotificationsAnalytics(platform);
    }

    /**
     * Send Mixpanel event for changed stream notification
     */
    private void sendNotificationChangeEvent(boolean isEnabled) {

        AnalyticsSender sender = BlinqApplication.analyticsSender;
        sender.sendNotificationChangeEvent(isEnabled);

    }

    private void showWelcomeOnboardNotification() {

        // Here to show the notification welcome only one time.
        if (!preferencesManager.getProperty(
                PreferencesManager.NOTIFICATION_WELCOME_SHOWN, false)) {
            HeadboxNotificationManager
                    .showWelcomeOnboardNotification(getApplicationContext());
            preferencesManager.setProperty(
                    PreferencesManager.NOTIFICATION_WELCOME_SHOWN, true);
        }
    }

    public void startPopupCheckboxFinalMsgActivity(Context context) {
        Intent intent = new Intent(context, PopupCheckboxFinalMsg.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.d(TAG, "Failed");
        }
    }

    @Override
    public void onGetContactInformation(boolean success) {
        Log.d(TAG, "time for server " + AppUtils.findTime(start));
        if (feedUri != null && statusBarNotification != null && notificationPlatform != null) {
            //startDotService(feedUri, statusBarNotification.getPackageName(), notificationPlatform);
        }
    }

}
