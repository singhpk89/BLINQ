package com.blinq.service.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;

import com.blinq.BlinqApplication;
import com.blinq.ImageLoaderManager;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.SettingsManager;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.models.Contact;
import com.blinq.models.FeedModel;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.HeadboxNotification;
import com.blinq.models.MessageType;
import com.blinq.models.NotificationContentIntent;
import com.blinq.models.Platform;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.receivers.NotificationBroadcastReceiver;
import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.ui.activities.splash.SplashActivity;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.ExternalAppsUtils;
import com.blinq.utils.ImageUtils;
import com.blinq.utils.Log;
import com.blinq.utils.ManageKeyguard;
import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible to notify the user of headbox events that happen.
 *
 * @author Johan Hansson.
 */
@SuppressLint("UseSparseArrays")
public class HeadboxNotificationManager {

    private static final String TAG = HeadboxNotificationManager.class
            .getSimpleName();

    private static final List<Platform> INBOUND_MESSAGES_PLATFORMS =
            Arrays.asList(Platform.WHATSAPP,
                    Platform.SKYPE,
                    Platform.EMAIL,
                    Platform.FACEBOOK,
                    Platform.HANGOUTS);

    /**
     * Responsible to notify the user of events that happen.
     */
    private static NotificationManager notificationManager;

    /**
     * Map of List of headbox notifications.
     */
    private static HashMap<Platform, List<HeadboxNotification>> notificationsMap = new HashMap<Platform, List<HeadboxNotification>>();

    private static HashMap<Platform, Integer> notificationsCount = new HashMap<Platform, Integer>();

    /**
     * Map of Builders for NotificationCompat objects.
     */
    private static HashMap<Platform, NotificationCompat.Builder> notificationsBuilder = new HashMap<Platform, NotificationCompat.Builder>();

    private static Context context;

    public static final int WELCOME_NOTIFICATION_ID = 54321;
    public static final String FROM_WELCOME_NOTIFICATION = "WelcomeNotification";
    private final static int FACEBOOK_NOTIFICATION_ID = 2466;
    private final static int HANGOUTS_NOTIFICATION_ID = 2467;
    private final static int MISSED_CALL_NOTIFICATION_ID = 2468;
    private final static int SMS_NOTIFICATION_ID = 2469;
    private final static int EMAIL_NOTIFICATION_ID = 2470;
    private final static int WHATSAPP_NOTIFICATION_ID = 2471;
    private final static int SKYPE_NOTIFICATION_ID = 2472;

    private static final int MAX_NUMBER_OF_NOTIFICATION_BITMAPS = 3;
    private static final String NOTIFICATION_BIG_TITLE_SEPERATER = " ";
    /**
     * Splitter between notification title and body , Separate the text that is
     * displayed in the status bar when the notification first arrives.
     */
    private static final String ALERT_SPLITTER = ": ";

    /**
     * Conversation object to the received message.
     */
    private static FeedModel feed;
    private static String lastContactId = null;
    private static SettingsManager settingsManager;
    private static PreferencesManager preferencesManager;
    private static StatusBarNotification sbn;


    /**
     * Show Notification.
     *
     * @param context - from which to call this method.
     * @param message - Headbox Message that contain message values.
     * @param feedId  - user conversation id.
     */
    public static void displayNotification(final Context context,
                                           HeadboxMessage message, StatusBarNotification sbn, int feedId) {

        HeadboxNotificationManager.context = context;
        settingsManager = new SettingsManager(context);
        preferencesManager = new PreferencesManager(context);
        HeadboxNotificationManager.sbn = sbn;
        boolean showNotification = isNotificationEnabled(message.getPlatform());

        if (!showNotification)
            return;

        if (settingsManager.isNotificationEnabled(message.getPlatform())) {

            if (message.getType() != MessageType.OUTGOING) {

                feed = FeedProviderImpl.getInstance(context).getFeed(feedId);

                if (feed == null) {
                    Log.e(TAG, "Error while displaying notification");
                    return;
                }

                Contact contact = feed.getContact();

                if (contact == null) {
                    Log.e(TAG, "Error while displaying notification");
                    return;
                }

                //NOTE: For now let's accept any notification no matter if it's from a top friend or not
                // .. for testing...
               /* if (!contact.isNotificationEnabled()) {
                    return;
                } else {
                    Log.e(TAG, "Receiving message from a top friend contact.");
                }*/

                sendCreatedNotificationAnalytics();

                Uri pictureUri = contact.getPhotoUri();

                if (lastContactId == null) {
                    lastContactId = contact.getIdentifier();
                }

                lastContactId = contact.getIdentifier();

                if (contact.getName() != null) {
                    message.getContact().setName(contact.getName());
                }

                if (INBOUND_MESSAGES_PLATFORMS.contains(message.getPlatform())) {
                    overrideNotification(message, feedId, pictureUri);
                }

            }
        } else {
            Log.d(TAG, "Notifications for " + message.getPlatform().name()
                    + " disabled.");
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void overrideNotification(HeadboxMessage message, int feedId, Uri pictureUri) {

        String body = feed.getLastMessageBody();
        String title = handleNotificationTitle(feed.getContact().toString(), message.getPlatform());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        //Set notification properties.
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setAutoCancel(true);

        Notification notification = sbn.getNotification();

        if (notification == null)
            return;

        storeNotificationIntent(notification, feedId, message.getPlatform());
        BlinqApplication.notification_platform = message.getPlatform();
        builder.setSmallIcon(getNotificationSmallIcon(message.getPlatform()));

        if (notification.tickerText != null)
            builder.setTicker(notification.tickerText);

        if (notification.contentIntent != null) {

            PendingIntent contentIntent = getNotificationContentIntent(feedId, message.getPlatform());
            builder.setContentIntent(contentIntent);

        }
        if (notification.fullScreenIntent != null)
            builder.setFullScreenIntent(notification.fullScreenIntent, true);

        if (notification.deleteIntent != null)
            builder.setDeleteIntent(notification.deleteIntent);

        if (message.getPlatform() != Platform.FACEBOOK)
            //Discard facebook actions.
            //Overriding those actions will crash the app while posting our notification.. will leave it for now.
            if (notification.actions != null && notification.actions.length > 0)
                for (Notification.Action action : notification.actions) {
                    //set the action's icon empty for now.
                    int icon = 0;
                    if (message.getPlatform() == Platform.EMAIL)
                        icon = getEmailActionIconByTitle(String.valueOf(action.title));

                    builder.addAction(icon, action.title, action.actionIntent);
                }

        //Add the notification builder to the builders map.
        notificationsBuilder.put(message.getPlatform(), builder);


        //Download the notification photo and then notify.
        DownloadImagesTask task = new DownloadImagesTask();
        task.setPlatform(message.getPlatform());
        String photo = pictureUri == null ? "" : pictureUri.toString();
        String[] imagesUris = new String[]{photo};
        task.execute(imagesUris);

    }

    /**
     * Build pending intent responsible to redirect the pressing action to the NotificationBroadcastReceiver,
     * In order to make a specific action.
     *
     * @param feedId   - the feed id that should be added to the intent extras.
     * @param platform - notification platform that should be added to the intent extras.
     * @return PendingIntent object.
     */
    private static PendingIntent getNotificationContentIntent(int feedId, Platform platform) {

        Intent notificationIntent = new Intent(NotificationBroadcastReceiver.ACTION_NOTIFICATION_PRESSED, null, context, NotificationBroadcastReceiver.class);
        notificationIntent.putExtra(NotificationBroadcastReceiver.ACTION_SEND_SMS_EXTRA_FEED_ID, feedId);
        notificationIntent.putExtra(NotificationBroadcastReceiver.ACTION_SEND_SMS_EXTRA_PLATFORM, platform.getId());
        PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);

        return contentIntent;
    }

    /**
     * Copy the original apps notification [content,fullscreen,and delete] intents to a static map
     * in order to use those intents.
     *
     * @param notification - status bar notification object.
     * @param feedId       - conversation id.
     * @param platform     - Notification type.
     */
    private static void storeNotificationIntent(Notification notification, int feedId, Platform platform) {

        if (BlinqApplication.notificationsIntents == null)
            BlinqApplication.notificationsIntents = new HashMap<Platform, NotificationContentIntent>();

        NotificationContentIntent notificationContent = new NotificationContentIntent(feedId, notification.contentIntent,
                notification.fullScreenIntent, notification.deleteIntent, platform);

        BlinqApplication.notificationsIntents.put(platform, notificationContent);
    }

    /**
     * Get resource ID of a drawable that represents the email action.
     *
     * @param title Text describing the action.
     */
    private static int getEmailActionIconByTitle(String title) {

        if (title.equalsIgnoreCase(context.getString(R.string.notification_email_action_reply)))
            return R.drawable.ic_action_reply;
        else if (title.equalsIgnoreCase(context.getString(R.string.notification_email_action_archive)))
            return R.drawable.ic_archive;

        return 0;
    }

    /**
     * Build a pending intent to open the social window when pressing the
     * socialize action.
     *
     * @param feedId   - conversation id.
     * @param platform - notification platform.
     * @return - PendingIntent object.
     */
    private static PendingIntent buildSocializeActionPendingIntent(int feedId, Platform platform) {

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        Intent intent = createNotificationPressIntent(context, platform, feedId);
        intent.putExtra(Constants.OPEN_SOCIAL_WINDOW, true);
        intent.putExtra(Constants.PLATFORM, platform.getId());
        stackBuilder.addParentStack(PopupSocialWindow.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }


    /**
     * Here to handle notification's title when receiving a message from unknown
     * contact.
     */
    private static String handleNotificationTitle(String title,
                                                  Platform platform) {

        if (platform == Platform.FACEBOOK
                && title.contains(StringUtils.FACEBOOK_SUFFIX)) {
            return StringUtils.NEW_MESSAGE_RECEIVED;
        }
        return title;
    }

    /**
     * Display notification.
     *
     * @param context - from which to call this method.
     */
    private static void show(Context context, Platform platform) {

        NotificationCompat.Builder notificationBuilder = notificationsBuilder.get(platform);

        if (notificationBuilder == null)
            return;

        Notification notification = null;

        if (INBOUND_MESSAGES_PLATFORMS.contains(platform)) {
            notification = buildNotificationForInboundPlatforms(platform);
        } else {
            notification = buildNotificationForOtherPlatforms(platform);
        }

        /**
         *
         A notification's big view appears only when the notification is
         * expanded, which happens when the notification is at the top of the
         * notification drawer, or when the user expands the notification with a
         * gesture. so we need to set the highest priority for headbox
         * notifications.
         */
        notification.priority = Notification.PRIORITY_MAX;

        notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = getNotificationId(platform);
        notificationManager.notify(notificationId, notification);
        sendNotificationsAnalytics(context, platform);
    }

    private static Notification buildNotificationForOtherPlatforms(Platform platform) {

        // Show notification, notificationID allows you to update
        // the notification later on.
        Notification notification = notificationsBuilder.get(platform).build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_LIGHTS;

        configureNotificationSound(notification, platform);

        return notification;
    }

    private static Notification buildNotificationForInboundPlatforms(Platform platform) {

        Notification notification = notificationsBuilder.get(platform).build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_LIGHTS;

        return notification;
    }

    private static void sendNotificationsAnalytics(Context context, Platform platform) {
        if (platform == null)
            return;
        BlinqAnalytics analyticsManager = new BlinqAnalytics(context);
        analyticsManager.sendEvent(AnalyticsConstants.RECEIVE_HEADBOX_NOTIFICATION_EVENT, AnalyticsConstants.TYPE_PROPERTY, platform.name().toLowerCase()
                , false, AnalyticsConstants.COMMUNICATION_CATEGORY);

        SettingsManager settingsManager = new SettingsManager(context);


        if (!settingsManager.isDefaultNotificationApp()) {
            analyticsManager.sendEvent(AnalyticsConstants.RECEIVE_ANY_NOTIFICATION_EVENT, AnalyticsConstants.TYPE_PROPERTY, platform.name().toLowerCase()
                    , false, AnalyticsConstants.COMMUNICATION_CATEGORY);
        }
    }

    /**
     * Get a unique identifier for a given platform to use for
     * creating/canceling the notification.
     */
    private static int getNotificationId(Platform platform) {

        switch (platform) {
            case FACEBOOK:
                return FACEBOOK_NOTIFICATION_ID;
            case HANGOUTS:
                return HANGOUTS_NOTIFICATION_ID;
            case CALL:
                return MISSED_CALL_NOTIFICATION_ID;
            case SMS:
            case MMS:
                return SMS_NOTIFICATION_ID;
            case WHATSAPP:
                return WHATSAPP_NOTIFICATION_ID;
            case SKYPE:
                return SKYPE_NOTIFICATION_ID;
            case EMAIL:
                return EMAIL_NOTIFICATION_ID;
            default:
                break;
        }

        return 0;
    }

    /**
     * Configure the sound of given notification.
     * <ul>
     * <li>- If Facebook messenger installed fire Facebook notifications without
     * sound.
     * <li>If missed call then fire the notification without sound.
     * <li>- Otherwise change sound depends on the contact type (Known/Unknown).
     * <ul>
     * <br>
     *
     * @param notification notification to be fired.
     * @param platform     notification platform.
     * @return configured notification object.
     */
    private static Notification configureNotificationSound(
            Notification notification, Platform platform) {

        // If facebook is installed and the message type is facebook or the
        // message type is missed call then don't play sound.
        if ((platform == Platform.FACEBOOK && ExternalAppsUtils
                .isFacebookMessengerAppInstalled(context))
                || platform == Platform.CALL) {
            return notification;
        }

        if (feed.getContact().getContactId() != null) {

            // Known contact.
            notification.sound = Uri.parse(Constants.RESOURCES_PATH
                    + context.getPackageName() + "/"
                    + R.raw.notification_known_contact);

        } else {

            // Unknown contact.
            notification.sound = Uri.parse(Constants.RESOURCES_PATH
                    + context.getPackageName() + "/"
                    + R.raw.notification_unknown_contact);

        }

        return notification;

    }

    /**
     * Get array of unique URIs from notification's images URIs (Remove
     * redundant).
     *
     * @return string array of unique URIs.
     */
    private static String[] getUniqueUris(
            List<HeadboxNotification> notifications) {

        ArrayList<String> uniqeUris = new ArrayList<String>();

        // Put URIs in hash map to remove redundant.
        HashMap<String, String> map = new HashMap<String, String>();

        for (int index = notifications.size() - 1; index >= 0
                && map.size() < MAX_NUMBER_OF_NOTIFICATION_BITMAPS; index--) {

            Uri imageUri = notifications.get(index).getImageUri();

            if (imageUri != null) {

                if (map.get(imageUri.toString()) == null) {

                    // new URI
                    map.put(imageUri.toString(), imageUri.toString());
                    uniqeUris.add(imageUri.toString());
                }

            } else {

                // Add null to set default image for the contact.
                uniqeUris.add(null);
            }
        }

        // Convert array list to array and return it.
        return uniqeUris.toArray(new String[uniqeUris.size()]);
    }

    /**
     * get notification ContentTitle in the big form of the group notifications
     * template.
     *
     * @param count    - notifications count.
     * @param platform - notification platform.
     */
    private static CharSequence getBigNotificationTitle(int count,
                                                        Platform platform) {

        String notificationTitle = count + NOTIFICATION_BIG_TITLE_SEPERATER;

        switch (platform) {

            case CALL:
                return notificationTitle
                        + context
                        .getString(R.string.notification_group_calls_title);
            default:
                return notificationTitle
                        + context
                        .getString(R.string.notification_group_messages_title);
        }
    }

    /**
     * Get a small icon for a given platform to use in the notification layout.
     */
    private static int getNotificationSmallIcon(Platform platform) {

        switch (platform) {
            case CALL:
                return R.drawable.statusbar_notifications_missed_call;
            case SMS:
            case MMS:
                return R.drawable.core_notification_sendmsg;
            case FACEBOOK:
                return R.drawable.fbmes_notif;
            case HANGOUTS:
                return R.drawable.hangouts_notif;
            case EMAIL:
                return R.drawable.gmail_notif;
            case SKYPE:
                return R.drawable.skype_notif;
            case WHATSAPP:
                return R.drawable.whatsapp_notif;
            default:
                break;
        }
        return 0;
    }

    /**
     * Create pending intent to call back from the notification bar.
     */
    public static PendingIntent getCallBackPendingIntent(Context context,
                                                         String number) {

        Intent intent = new Intent(
                NotificationBroadcastReceiver.ACTION_CALL_BACK_FROM_NOTIFICATION,
                Uri.fromParts(Constants.SCHEME_TEL, number, null), context,
                NotificationBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Create pending intent to send sms from the notification bar.
     */
    public static PendingIntent getSendSmsFromNotificationPendingIntent(
            Context context, String number) {

        Intent intent = new Intent(
                NotificationBroadcastReceiver.ACTION_SEND_SMS_FROM_NOTIFICATION,
                Uri.fromParts(Constants.SCHEME_SMSTO, number, null), context,
                NotificationBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static PendingIntent getSendSmsFromHeadboxNotificationPendingIntent(
            Context context, int feedId) {

        Intent intent = new Intent(
                NotificationBroadcastReceiver.ACTION_SEND_SMS_FROM_HEADBOX_NOTIFICATION, null, context,
                NotificationBroadcastReceiver.class);
        intent.putExtra(NotificationBroadcastReceiver.ACTION_SEND_SMS_EXTRA_FEED_ID, feedId);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Create Intent to open headbox when pressing the notification.
     */
    private static Intent getLaunchHeadboxIntent(Context context) {

        Intent intent = new Intent(context, SplashActivity.class);
        intent.putExtra(FROM_WELCOME_NOTIFICATION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return intent;
    }

    /**
     * Create intent to open a proper activity after pressing the notification item.
     *
     * @param feedId - conversation id.
     * @return - intent.
     */
    private static Intent createNotificationPressIntent(Context context, Platform platform, int feedId) {

        int notificationsCount = notificationsMap.get(platform) == null ? 1 : notificationsMap.get(platform).size();
        Intent intent = new Intent(context, PopupSocialWindow.class);
        intent.putExtra(Constants.FEED_ID, feedId);
        intent.putExtra(Constants.FROM_NOTIFICATION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(Constants.SINGLE_NOTIFICATION, notificationsCount > 1 ? false : true);

        return intent;
    }

    /**
     * Check whether the user is authenticated or not in order to control the appearance of headbox notifications.
     *
     * @return - boolean
     */
    private static boolean isAuthenticated() {
        return FacebookAuthenticator.getInstance(context).isConnected();
    }

    /**
     * Clear notification history and reset all values to its initial values
     */
    public static void clearNotificationHistory() {

        notificationsMap.clear();
        notificationsBuilder.clear();
        lastContactId = null;
    }

    /**
     * Cancel notifications from the notification bar.
     */
    public static void cancelNotifications() {

        if (notificationsBuilder != null && notificationManager != null) {
            for (Platform platform : notificationsBuilder.keySet()) {
                int notificationId = getNotificationId(platform);
                notificationManager.cancel(notificationId);
            }
        }
    }

    /**
     * Scale given bitmap to the device notification icon size.
     *
     * @param bitmap bitmap to scale.
     * @return scaled bitmap.
     */
    private static Bitmap scaleNotificationImage(Bitmap bitmap) {

        // Get notification image dimensions.
        int notificationIconWidth = (int) context.getResources().getDimension(
                android.R.dimen.notification_large_icon_width);

        int notificationIconHeight = (int) context.getResources().getDimension(
                android.R.dimen.notification_large_icon_height);

        return ImageUtils.scaleBitmap(bitmap, notificationIconWidth,
                notificationIconHeight);

    }

    /**
     * Get notification activation status.
     */
    public static boolean isNotificationEnabled(Platform platform) {

        //SHOW THE NOTIFICATION:
        //1-IF App ON background OR ON foreground AND the keyguard on the restricted input mode or the its an inbound platform.
        //2-AND set as a default notification stream app.
        //3-AND the user is Authenticated.

        boolean enabled = (AppUtils.isAppOnBackground(context)
                || (AppUtils.isAppOnForeground(context) &&
                (ManageKeyguard.inKeyguardRestrictedInputMode() || INBOUND_MESSAGES_PLATFORMS.contains(platform))))
                && settingsManager.isDefaultNotificationApp()
                && isAuthenticated();

        return enabled;
    }

    public static Map<Platform, NotificationContentIntent> getNotificationsIntents() {
        return BlinqApplication.notificationsIntents;
    }

    /**
     * Get picture form its URI in the background.
     *
     * @author exalt
     */
    private static class DownloadImagesTask extends
            AsyncTask<String, Void, List<Bitmap>> {

        private Platform platform;

        @Override
        protected List<Bitmap> doInBackground(String... urls) {

            List<Bitmap> bitmaps = new ArrayList<Bitmap>();

            Bitmap contactBitmap;

            for (String url : urls) {

                if (!StringUtils.isBlank(url)) {

                    contactBitmap = loadContactBitmap(url);

                    if (contactBitmap == null) {

                        bitmaps.add(getDefaultNotificationImage(urls.length));

                    } else {

                        bitmaps.add(scaleNotificationImage(contactBitmap));

                    }

                } else {

                    bitmaps.add(getDefaultNotificationImage(urls.length));

                }
            }
            return bitmaps;
        }

        public void setPlatform(Platform platform) {
            this.platform = platform;
        }

        // Sets the Bitmap returned by doInBackground
        @Override
        protected void onPostExecute(List<Bitmap> bitmaps) {

            try {

                if (bitmaps != null && bitmaps.size() > 0) {

                    Bitmap notificationBitmap;

                    // If there is only one bitmap get it, if there are more
                    // bitmaps combine them as needed.
                    if (bitmaps.size() > 1) {
                        notificationBitmap = ImageUtils.combineImages(bitmaps);
                        notificationBitmap = scaleNotificationImage(notificationBitmap);
                    } else {
                        notificationBitmap = bitmaps.get(0);
                    }

                    if (notificationsBuilder.get(platform) != null) {

                        notificationsBuilder.get(platform).setLargeIcon(
                                notificationBitmap);
                    }

                } else {

                    if (notificationsBuilder.get(platform) != null) {

                        notificationsBuilder.get(platform).setLargeIcon(
                                getDefaultNotificationImage(bitmaps.size()));
                    }
                }

                HeadboxNotificationManager.show(context, platform);

            } catch (Exception e) {

                Log.d(TAG, "Unexpected crash while showing a notification");

            }
        }

        /**
         * Get the default notification image from resources;
         *
         * @param numberOfImages number of images to combine in the notification icon.
         * @return default notification image.
         */
        private Bitmap getDefaultNotificationImage(int numberOfImages) {

            Bitmap defaultNotificationBitmap = BitmapFactory.decodeResource(
                    context.getResources(),
                    R.drawable.statusbar_notifications_body_headbox);

            if (numberOfImages == 1) {

                // only the default notification icon, don't scale.
                return defaultNotificationBitmap;

            }

            return scaleNotificationImage(defaultNotificationBitmap);

        }

        /**
         * Load bitmap from WEB or local contacts depends on URL type.
         *
         * @param url image URL.
         * @return loaded bitmap.
         */
        private Bitmap loadContactBitmap(String url) {


            if (url == null) {
                return null;
            }

            ImageLoaderManager imageLoaderManager = new ImageLoaderManager(context);

            return imageLoaderManager.loadImageSync(Uri.parse(url));

        }

    }

    /**
     * Build and post Headbox Welcome Notification.
     */
    public static void showWelcomeOnboardNotification(Context context) {

        // Build welcome notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context);

        builder.setTicker(context
                .getString(R.string.notification_enable_message));
        builder.setContentTitle(context.getString(R.string.notification_enable_name));
        builder.setContentText(context
                .getString(R.string.notification_enable_message));
        builder.setSmallIcon(R.drawable.blinq_splash_logo);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.welcome_notification));

        Intent notificationIntent = getLaunchHeadboxIntent(context);
        int requestId = (int) System.currentTimeMillis();
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                requestId, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(true);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Show the notification
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(WELCOME_NOTIFICATION_ID, notification);

        // send receive welcome on board notification event
        BlinqAnalytics analyticsManager = new BlinqAnalytics(context);

        analyticsManager.sendEvent(
                AnalyticsConstants.RECEIVE_WELCOME_NOTIFICATION_EVENT, true,
                AnalyticsConstants.ONBOARDING_CATEGORY);

    }

    private static void sendCreatedNotificationAnalytics() {
        BlinqAnalytics analyticsManager = new BlinqAnalytics(context);
        analyticsManager.sendEvent(
                AnalyticsConstants.CREATED_HEADBOX_NOTIFICATION, true,
                AnalyticsConstants.ACTION_CATEGORY);
    }

}