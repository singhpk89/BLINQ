package com.blinq.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;

import com.blinq.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johan Hansson.
 */
public class EmailUtils {

    public static final String REPORT_BUG_TITLE = "Feedback";

    private static final String EMAIL_INTENT_TYPE = "message/rfc822";

    // Constants to discriminate Gmail application.
    private static final String GMAIL_PACKAGE_INDICATOR = ".gm";
    private static final String GMAIL_ACTIVITY_INDICATOR = "gmail";
    public static final String DATABASE_LOG_EMAIL_SUBJECT = "Blinq Log File - Support";
    public static final String LOW_RATE_EMAIL_SUBJECT = "My feedback for version ";

    /**
     * Send email using intent.
     *
     * @param to      array of recipients.
     * @param subject email subject.
     * @param body    email body.
     * @param context application context.
     */
    public static void sendEmail(String[] to, String subject, String body,
                                 Context context) {

        // Build email intent.
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        emailIntent.setType(EMAIL_INTENT_TYPE);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        checkGmailApp(context, emailIntent);

        // Gmail not found, show all Apps which can handle this intent.
        context.startActivity(Intent.createChooser(emailIntent,
                context.getString(R.string.app_chooser_title)).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK));

    }

    /**
     * Send email using intent.
     *
     * @param to              array of recipients.
     * @param subject         email subject.
     * @param body            email body.
     * @param attachmentsUris list of attachments URIs.
     * @param context         application context.
     */
    public static void sendEmail(String[] to, String subject, String body,
                                 ArrayList<Uri> attachmentsUris, Context context) {

        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY);
        emailIntent.setType(EMAIL_INTENT_TYPE);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        checkGmailApp(context, emailIntent);

        // Check if the status of external storage available to read/write.
        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {

            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                    attachmentsUris);

            context.startActivity(Intent.createChooser(emailIntent,
                    context.getString(R.string.app_chooser_title)));
        }

    }

    /**
     * Check if the Gmail application found on the device & update the given
     * intent depends on it.
     *
     * @param context application context.
     * @param intent  email intent.
     * @return updated intent if needed.
     */
    private static Intent checkGmailApp(Context context, Intent intent) {

        // Get all packages that can handle this intent.
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> matches = packageManager.queryIntentActivities(
                intent, 0);
        ResolveInfo targetActivityInfo = null;

        for (final ResolveInfo info : matches) {

            // Check if the Gmail package is one of this packages.
            if (info.activityInfo.packageName.endsWith(GMAIL_PACKAGE_INDICATOR)
                    || info.activityInfo.name.toLowerCase().contains(
                    GMAIL_ACTIVITY_INDICATOR)) {

                targetActivityInfo = info;
                break;
            }
        }

        // if Gmail found use it.
        if (targetActivityInfo != null) {

            intent.setClassName(targetActivityInfo.activityInfo.packageName,
                    targetActivityInfo.activityInfo.name);
        }

        return intent;

    }

    /**
     * Send user Database & Log via email.
     */
    public static void sendDatabaseLog(Activity activity) {

        sendEmail(
                new String[]{activity.getResources().getString(R.string.headbox_support_email)},
                EmailUtils.DATABASE_LOG_EMAIL_SUBJECT,
                "",
                FileUtils
                        .getUrisOfFolderContents(FileUtils.HEADBOX_FOLDER_PATH),
                activity
        );

    }

    public static void sendLowRateEmail(Activity activity) {
        sendEmail(new String[]{activity.getResources().getString(R.string.headbox_support_email)},
                LOW_RATE_EMAIL_SUBJECT + AppUtils.getVersionName(activity),
                AppUtils.getDeviceInfo(activity), activity);
    }

}
