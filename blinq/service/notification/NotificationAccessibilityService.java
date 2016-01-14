package com.blinq.service.notification;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.blinq.utils.Log;

import android.view.accessibility.AccessibilityEvent;

/**
 * Responsible to Intercept device notifications.
 * <p/>
 * Works with versions under 4.3.
 * <p/>
 * {@see http
 * ://developer.android.com/guide/topics/ui/accessibility/services.html}
 *
 * @author Johan Hansson.
 */
public class NotificationAccessibilityService extends AccessibilityService {

    private String TAG = this.getClass().getSimpleName();
    private int notificationId = 0;
    private NotificationManager notificationManager;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        final int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:

                if (event.getParcelableData() != null
                        && event.getParcelableData() instanceof Notification) {

                    Notification notification = (Notification) event
                            .getParcelableData();
                    String packageName = event.getPackageName().toString();
                    int id = notificationId++;
                    cancelNotification(id, packageName, notification);
                    Log.d(TAG,
                            "NotificationAccessibilityService:onNotificationPosted #"
                                    + id
                    );
                }

                break;
        }
    }

    private void cancelNotification(int id, String packageName, Notification notification) {

        // TODO: Parse notification object to get notification id.
    }

    @Override
    protected void onServiceConnected() {

        Log.d(TAG, "NotificationAccessibilityService:onServiceConnected");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG,
                    "Running Android 4.3 and above. ignoring accessibility service");
            return;
        }

        notificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        // called when it successfully connects to accessibility
        // service.
        super.onServiceConnected();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "NotificationAccessibilityService:onDestroy");
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {
    }
}
