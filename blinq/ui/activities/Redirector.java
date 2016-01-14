package com.blinq.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.blinq.HeadboxAccountsManager;
import com.blinq.HeadboxAccountsManager.AccountType;
import com.blinq.PreferencesManager;
import com.blinq.SettingsManager;
import com.blinq.authentication.impl.Google.GoogleAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.service.notification.HeadboxNotificationManager;
import com.blinq.ui.activities.feed.FeedActivity;
import com.blinq.ui.activities.notificationsetup.NotificationSetupActivity;
import com.blinq.utils.Constants;

/**
 * Redirect the user from one activity to another based on connectivity status.
 */
public class Redirector {

    private static final String TAG = Redirector.class.getSimpleName();

    private final HeadboxAccountsManager headboxAccountsManager;
    private FacebookAuthenticator facebookAuthenticator;
    private GoogleAuthenticator googleAuthenticator;
    private PreferencesManager preferencesManager;
    private Activity activity;

    public Redirector(final Activity activity) {

        this.activity = activity;
        facebookAuthenticator = FacebookAuthenticator.getInstance(activity);
        googleAuthenticator = GoogleAuthenticator.getInstance(activity, activity);
        preferencesManager = new PreferencesManager(activity);
        headboxAccountsManager = HeadboxAccountsManager.getInstance();
    }

    public PreferencesManager getPreferencesManager() {
        return preferencesManager;
    }

    /**
     * Redirect the user to a suitable activity based on connectivity status [connected/disconnected].
     */
    public void Redirect() {

        Context context = activity;

        boolean isFacebookConnected = (facebookAuthenticator.isConnected() && headboxAccountsManager
                .getAccountsByType(AccountType.FACEBOOK).name != null);

        boolean isGoogleConnected = (googleAuthenticator.isConnected() && headboxAccountsManager
                .getAccountsByType(AccountType.GOOGLE).name != null);

        boolean isTokenExist = (isFacebookConnected
                || isGoogleConnected || activity
                .getIntent().getBooleanExtra(
                        HeadboxNotificationManager.FROM_WELCOME_NOTIFICATION,
                        false));


        boolean isStreamNotificationOpenedBefore = preferencesManager
                .getProperty(PreferencesManager.SETUP_SCREEN_OPENED, false);

        boolean isPreviouslyChecked = new SettingsManager(context)
                .isDefaultNotificationApp();

        boolean isAndroidSupportNotificationStream = (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2);

        if (!isFacebookConnected) {
            startLoginActivity(context);
        } else if (isTokenExist && !isStreamNotificationOpenedBefore
                && !isPreviouslyChecked && isAndroidSupportNotificationStream) {
            preferencesManager.setProperty(
                    PreferencesManager.SETUP_SCREEN_OPENED, true);
            startNotificationSetupActivity(context);
        } else if (isTokenExist) {
            startFeedActivity(context);
        } else {
            startLoginActivity(context);
        }
        activity.finish();
    }

    private void startNotificationSetupActivity(Context context) {
        Log.d(TAG, "Staring NotificationSetupActivity");
        Intent notificationSetupIntent = new Intent(context,
                NotificationSetupActivity.class);
        notificationSetupIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(notificationSetupIntent);
    }

    private void startFeedActivity(Context context) {
        Log.d(TAG, "Starting FeedActivity");
        Intent feedViewIntent = new Intent(context, FeedActivity.class);
        feedViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        feedViewIntent.putExtra(Constants.SINGLE_NOTIFICATION, false);
        activity.startActivity(feedViewIntent);
    }

    private void startLoginActivity(Context context) {
        Log.d(TAG, "Starting OnBoardActivity");
        Intent loginViewIntent = new Intent(context, OnBoardActivity.class);
        loginViewIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(loginViewIntent);
    }
}


