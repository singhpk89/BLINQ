package com.blinq.service.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony.Sms;

import com.blinq.SettingsManager;
import com.blinq.models.NotificationData;
import com.blinq.models.Platform;
import com.blinq.utils.ExternalAppsUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressLint({"NewApi", "Override"})
public class NotificationParser {

    private final Context context;
    private List<NotificationData> notifications;
    private SettingsManager settingsManager;
    private Platform platform;

    public NotificationParser(Context context) {
        this.context = context;
        this.settingsManager = new SettingsManager(context);
    }

    public List<NotificationData> parseNotification(Notification n,
                                                    String packageName, int notificationId, String tag) {

        notifications = new ArrayList<NotificationData>();

        if (n != null) {

            // build notification data object
            NotificationData nd = new NotificationData();

            // extract notification & app icons
            Resources res;
            PackageInfo info;
            ApplicationInfo ai;
            try {
                res = context.getPackageManager().getResourcesForApplication(
                        packageName);
                info = context.getPackageManager().getPackageInfo(packageName,
                        0);
                ai = context.getPackageManager().getApplicationInfo(
                        packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                info = null;
                res = null;
                ai = null;
            }

            platform = mapPackageToPlatform(packageName);
            nd.setPlatform(platform);

            if (!StringUtils.isBlank(n.tickerText)) {
                if (platform == Platform.WHATSAPP &&  n.tickerText.toString().contains("@")) {
                    // This is a group message
                    return notifications;
                }
            }

            // get time of the event
            if (n.when != 0)
                nd.setReceived(n.when);
            else
                nd.setReceived(System.currentTimeMillis());

            nd.setCount(n.number);
            nd.setPackageName(packageName);

            Bundle extras = n.extras;

            String notificationTitle = "";
            if (extras.containsKey(Notification.EXTRA_TITLE)) {
                notificationTitle = com.blinq.utils.StringUtils.removeGeneralPunctuation(extras.getCharSequence(Notification.EXTRA_TITLE));
                nd.setTitle(notificationTitle);
            }

            if (extras.containsKey(Notification.EXTRA_TEXT))
                nd.setText(extras.getCharSequence(Notification.EXTRA_TEXT));

            // Removed notification from whatsapp where we have multiple messages from multiple people
            if ("Whatsapp".equals(nd.getTitle())) {
                nd.setTitle(n.tickerText.toString().substring("Message from ".length()));
            }

            // Use default notification text & title - if no info found on
            // expanded notification
            if (nd.getText() == null) {
                nd.setText(n.tickerText);
            }
            if (nd.getTitle() == null) {
                if (nd.getText() == null) {
                    // if both text and title are null - that's non
                    // Informative notification - ignore it

                    return notifications;
                }
                if (info != null)
                    nd.setTitle(context.getPackageManager()
                            .getApplicationLabel(ai));
                else
                    nd.setTitle(packageName);
            }

            nd.setId(notificationId);
            nd.setTag(tag);

            notifications = new ArrayList<NotificationData>();
            notifications.add(nd);

            if (extras.containsKey(Notification.EXTRA_TEXT_LINES)) {
                CharSequence[] lines = extras
                        .getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                if (lines != null)
                    for (CharSequence line : lines) {
                        if (StringUtils.isBlank(line)) continue;
                        // Ignore the group messages, or messages from different person
                        if (platform == Platform.WHATSAPP) {
                            String lineString = line.toString();
                            if (!lineString.contains(nd.getTitle())) {
                                continue;
                            }
                            if (lineString.contains("@")) {
                                continue;
                            }
                            if (lineString.contains(nd.getTitle())) {
                                line = lineString.substring(nd.getTitle().length() + 2);
                            }
                        }

                        NotificationData subNotification = new NotificationData(
                                nd);
                        parseExtendedText(subNotification, packageName, line);
                        subNotification.setContent(line);
                        notifications.add(subNotification);
                    }
            }

            return notifications;
        }
        return notifications;
    }

    /**
     * Try to parse the expanded notification line.
     *
     * @param notification - notification parsed model.
     * @param line         - notification expanded line.
     */
    private void parseExtendedText(NotificationData notification,
                                   String packageName, CharSequence line) {

        String title = null;
        String text = null;

        if (platform == Platform.EMAIL) {

            if (packageName.equals(ExternalAppsUtils.GMAIL_PACKAGE_NAME)
                    || packageName.equals(ExternalAppsUtils.GOOGLE_INBOX_APP_PACKAGE_NAME)) {

                title = StringUtils.substringBefore(line.toString(), "  ");
                text = StringUtils.substringAfter(line.toString(), "   ");

            } else if (packageName
                    .equals(ExternalAppsUtils.EMAIL_APP_PACKAGE_NAME)) {
                title = StringUtils.substringBefore(line.toString(), ": ");
                text = StringUtils.substringAfter(line.toString(), ": ");
            }
        } else if (platform == Platform.SKYPE) {

            int separatorIndex = StringUtils.ordinalIndexOf(line, " ", 2);
            title = StringUtils.substring(line.toString(), 0, separatorIndex);
            text = StringUtils.substring(line.toString(), separatorIndex + 1,
                    line.length());

        } else if (platform == Platform.WHATSAPP) {

            title = StringUtils.substringBefore(line.toString(), ": ");
            text = StringUtils.substringAfter(line.toString(), ": ");
        } else if (platform == Platform.SMS) {

            title = notification.getTitle().toString();
            text = notification.getText().toString();
        }

        notification.setTitle(title);
        notification.setText(text);

    }

    /**
     * Check if the notification is dismissible or not according to the user
     * preferences.
     */
    public boolean isCancelable() {

        if (notifications == null || platform == null)
            return false;

        switch (platform) {

            case CALL:
            case SMS:
            case FACEBOOK:
                return settingsManager.isNotificationEnabled(platform);
            case SKYPE:
            case WHATSAPP:
            case EMAIL:
                return settingsManager.isInboundMessagesEnabled(platform);
            case HANGOUTS:
                return settingsManager.isNotificationEnabled(platform)
                        && isASingleHangoutsChatNotification(notifications.get(0)
                        .getTitle());
            default:
                break;
        }
        return false;

    }

    /**
     * Map application package name to platform.
     */
    public Platform mapPackageToPlatform(String packageName) {

        Platform platform = null;

        if (packageName == null) {
            return null;
        }

        if (packageName.equals(ExternalAppsUtils.DEFAULT_CALL_APP_PACKAGE)) {
            return Platform.CALL;
        } else if (packageName.equals(ExternalAppsUtils.HANGOUTS_PACKAGE_NAME)) {
            return Platform.HANGOUTS;
        } else if (packageName
                .equals(ExternalAppsUtils.DEFAULT_SMS_APP_PACKAGE)) {
            return Platform.SMS;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && packageName.equals(Sms.getDefaultSmsPackage(context))) {
            return Platform.SMS;
        } else if (packageName
                .equals(ExternalAppsUtils.FACEBOOK_MESSENGER_APP_PACKAGE_NAME)) {
            return Platform.FACEBOOK;
        } else if (packageName.equals(ExternalAppsUtils.SKYPE_APP_PACKAGE_NAME)) {
            return Platform.SKYPE;
        } else if (packageName.equals(ExternalAppsUtils.WHATSAPP_PACKAGE_NAME)) {
            return Platform.WHATSAPP;
        } else if (packageName.equals(ExternalAppsUtils.GMAIL_PACKAGE_NAME)
                || packageName.equals(ExternalAppsUtils.EMAIL_APP_PACKAGE_NAME)
                || packageName.equals(ExternalAppsUtils.GOOGLE_INBOX_APP_PACKAGE_NAME)) {
            return Platform.EMAIL;
        }
        return platform;
    }

    /**
     * Check if the notification's title pointed to a single chat notification.
     */
    private boolean isASingleHangoutsChatNotification(CharSequence contentTitle) {

        if (contentTitle == null)
            return true;

        // If the content title contains ", " then its a group chat
        // notification.
        if (!StringUtils.contains(contentTitle, ", ")) {
            return true;
        }

        return false;
    }

    public Platform getPlatform() {
        return platform;
    }
}