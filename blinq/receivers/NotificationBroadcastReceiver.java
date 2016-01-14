package com.blinq.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.blinq.BlinqApplication;
import com.blinq.models.NotificationContentIntent;
import com.blinq.models.Platform;
import com.blinq.service.FloatingDotService;
import com.blinq.service.notification.HeadboxNotificationManager;
import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.ui.fragments.InstantMessageFragment;
import com.blinq.utils.Constants;
import com.blinq.utils.Log;

/**
 * Accepts broadcast Intents which will be prepared by
 * {@link HeadboxNotificationManager}.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = NotificationBroadcastReceiver.class
            .getSimpleName();

    /**
     * Intent Action used for making a phone call from Notification bar. This is
     * for missed call notifications.
     */
    public static final String ACTION_CALL_BACK_FROM_NOTIFICATION = "com.headbox.phone.ACTION_CALL_BACK_FROM_NOTIFICATION";

    /**
     * Intent Action used for sending a SMS from notification bar. This is for
     * missed call notifications.
     */
    public static final String ACTION_SEND_SMS_FROM_NOTIFICATION = "com.headbox.phone.ACTION_SEND_SMS_FROM_NOTIFICATION";

    public static final String ACTION_NOTIFICATION_PRESSED = "com.headbox.notification";

    public static final String ACTION_SEND_SMS_FROM_HEADBOX_NOTIFICATION = "com.headbox.phone.ACTION_SEND_SMS_FROM_HEADBOX_NOTIFICATION";
    public static final String ACTION_SEND_SMS_EXTRA_FEED_ID = "feedId";
    public static final String ACTION_SEND_SMS_EXTRA_PLATFORM = "platform";


    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "Broadcast from Notification: " + action);

        if (action.equals(ACTION_CALL_BACK_FROM_NOTIFICATION)) {

            closeSystemDialogs(context);
            clearMissedCallNotification(context);

            Intent callIntent = new Intent(Intent.ACTION_CALL, intent.getData());
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivity(callIntent);

        } else if (action.equals(ACTION_SEND_SMS_FROM_NOTIFICATION)) {

            closeSystemDialogs(context);
            clearMissedCallNotification(context);

            Intent smsIntent = new Intent(Intent.ACTION_SENDTO,
                    intent.getData());
            smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(smsIntent);

        } else if (action.equals(ACTION_SEND_SMS_FROM_HEADBOX_NOTIFICATION)) {

            closeSystemDialogs(context);
            clearMissedCallNotification(context);

            int feedId = intent.getIntExtra(ACTION_SEND_SMS_EXTRA_FEED_ID, 0);
            if (feedId == 0)
                return;

            openConversation(context, feedId);

        } else if (action.equals(ACTION_NOTIFICATION_PRESSED)) {

            closeSystemDialogs(context);

            Platform platform = BlinqApplication.notification_platform;

            try {
                final NotificationContentIntent intentsMap = HeadboxNotificationManager.getNotificationsIntents().get(platform);

                intentsMap.getContentIntent().send(context, 0, new Intent(), new PendingIntent.OnFinished() {
                    @Override
                    public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String s, Bundle bundle) {
                        Intent dotServiceIntent = new Intent(context, FloatingDotService.class);
                        dotServiceIntent.putExtra(FloatingDotService.FEED_ID_EXTRA_TAG, intentsMap.getFeedId());
                        dotServiceIntent.putExtra(FloatingDotService.PLATFORM_EXTRA_TAG, intentsMap.getPlatform().getId());
                        context.startService(dotServiceIntent);
                    }
                }, new Handler());


            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Received a request from notification manager.");
        }
    }


    /**
     * Open the instant message view for a specific contact when pressing the notification send message action.
     *
     * @param context -
     * @param feedId  - conversation to be opened.
     */
    private Intent openConversation(Context context, int feedId) {

        Intent intent = new Intent(context, PopupSocialWindow.class);
        intent.putExtra(Constants.FEED_ID, feedId);
        intent.putExtra(InstantMessageFragment.SHOW_KEYBOARD, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(Constants.FROM_NOTIFICATION, true);
        return intent;
    }

    /**
     * Clear headbox notifications history.
     */
    private void clearMissedCallNotification(Context context) {

        HeadboxNotificationManager.cancelNotifications();
        HeadboxNotificationManager.clearNotificationHistory();
    }

    /**
     * Send request to close the notification window-shade and the recent
     * tasks dialog.
     */
    private void closeSystemDialogs(Context context) {

        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeDialog);

    }
}
