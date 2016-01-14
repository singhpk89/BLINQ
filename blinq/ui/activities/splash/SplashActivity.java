package com.blinq.ui.activities.splash;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.SettingsManager;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.AuthUtils.AuthAction;
import com.blinq.authentication.impl.AuthUtils.LoginCallBack;
import com.blinq.authentication.impl.AuthUtils.RequestStatus;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.service.notification.HeadboxNotificationManager;
import com.blinq.ui.activities.HeadboxBaseActivity;
import com.blinq.ui.activities.Redirector;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.crashlytics.android.Crashlytics;

import java.util.HashMap;
import java.util.List;

/**
 * Splash and main activity entering point
 */
public class SplashActivity extends HeadboxBaseActivity implements AuthAction {

    public static final String TAG = SplashActivity.class.getSimpleName();

    private boolean isLoaded;
    private boolean isDBUpgraded;

    private Redirector redirector;
    private HashMap<Platform, Boolean> platforms = new HashMap<Platform, Boolean>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //boolean notificationStreamEnabled = settingsManager.isDefaultNotificationApp();

        sendEnteredAppAnalytics();

        // Initialize activities redirection instance.
        redirector = new Redirector(this);

        if (!isTaskRoot()) {
            // Android launched another instance of the splash activity into an
            // existing task
            // finish and go away, dropping the user back into the activity
            sendAnalytics();
            finish();
            return;
        }

        fillScreen();
        startTheApplication();

        sendAnalytics();

    }

    private void sendAnalytics() {

        if (getIntent() != null && getIntent().getBooleanExtra(
                HeadboxNotificationManager.FROM_WELCOME_NOTIFICATION, false)) {
            // send open welcome on board notification event.
            sendEvent(
                    AnalyticsConstants.OPENED_WELCOME_NOTIFICATION_EVENT,
                    true, AnalyticsConstants.ONBOARDING_CATEGORY);
        } else {
            // send open splash event.
            sendEvent(
                    AnalyticsConstants.OPENED_SPLASH_SCREEN_EVENT, false,
                    AnalyticsConstants.ONBOARDING_CATEGORY);
        }

    }

    @Override
    protected void onDestroy() {
        complete();
        super.onDestroy();
    }

    /**
     * Send analytics event when entering the app.
     */
    private void sendEnteredAppAnalytics() {

        sendEvent(AnalyticsConstants.ENTERING_APP_EVENT,
                AnalyticsConstants.FROM_PROPERTY,
                AnalyticsConstants.DIRECT_LAUNCH_VALUE, true,
                AnalyticsConstants.ACTION_CATEGORY);

    }

    private void sendABNotificationProperty() {

        String abNotificationValue = new SettingsManager(this).getABNotificationValue();

        logDebug("AB notification:" + abNotificationValue);

        if (!StringUtils.isBlank(abNotificationValue)) {

            setUserProfileProperty(
                    AnalyticsConstants.PROFILE_AB_NOTIFICATION_PROPERTY,
                    abNotificationValue);
        }

    }

    private void startTheApplication() {

        //updateApp();
        AppUtils.upgradeApp(this);

        // Flag saved in the Preferences for first shown of splash screen.
        isLoaded = getPreferencesManager().isLoaded();
        isDBUpgraded = getPreferencesManager().isDatabaseUpgraded();
        // Manage the appearance of the splash screen.
        // Make sure that also after an upgrade we see the stream notifications
        // screens
        if (!isLoaded) {

            setContentView(R.layout.activity_splash);
            // configure the AB notification
            settingsManager
                    .setupABNotificationGroup();
            preferencesManager.setupABFeedHistory();
            sendFirstInstallAnalytics();
            init();

        } else {
            redirector.Redirect();
        }

        if(!AppUtils.isDebug(this)) {
            Crashlytics.start(this);
        }
    }

    /**
     * Send the first time install event
     */
    private void sendFirstInstallAnalytics() {

        if (getPreferencesManager().getProperty(PreferencesManager.FIRST_INSTALL,
                true)) {
            getPreferencesManager().setProperty(PreferencesManager.FIRST_INSTALL,
                    false);

            String currentTime = AppUtils
                    .getCurrentDateTime(AnalyticsConstants.DATE_FORMAT);
            sendEvent(AnalyticsConstants.FIRST_INSTALL_EVENT,
                    AnalyticsConstants.DATE_FIRST_SEEN_PROPERTY, currentTime,
                    true, AnalyticsConstants.ONBOARDING_CATEGORY);

            //Send AB notification event.
            sendABNotificationProperty();

            //Send AB Feed History mode event.
            String abFeedHistoryValue = getPreferencesManager().
                    getProperty(PreferencesManager.AB_FEED_HISTORY, AnalyticsConstants.AB_FEED_HISTORY_FULL);

            Log.d(TAG, "Feed history mode = " + abFeedHistoryValue);

            setUserProfileProperty(AnalyticsConstants.AB_FEED_HISTORY,
                    abFeedHistoryValue);

            //Send first install time event.
            setUserProfileProperty(
                    AnalyticsConstants.FIRST_INSTALL_EVENT, currentTime);
        }
    }

    /**
     * Reload the platform contacts after upgrading if was connected
     */
    private void reloadDataAfterUpgrade() {

        if (isDBUpgraded) {
            synchronized (platforms) {
                for (Platform platform : platforms.keySet()) {
                    reLogin(AuthUtils.getAuthInstance(platform, this), platform);
                }
            }
        }
    }

    /**
     * Initialize activity's required data.
     */
    private void init() {
        getPreferencesManager().initializeAppSettings();

        initAnimation();

        platforms.put(Platform.FACEBOOK, false);
        platforms.put(Platform.HANGOUTS, false);
        platforms.put(Platform.TWITTER, false);
        platforms.put(Platform.INSTAGRAM, false);

        // Starts the asynchronous task for App initialization.
        new AppInitializer().execute(null, null, null);
    }

    private void initAnimation() {
        final Animation splashAnimation = AnimationUtils.loadAnimation(this, R.anim.loading);
        final View squareImageView = findViewById(R.id.splash_square);
        squareImageView.startAnimation(splashAnimation);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // Prevent the application from closing in splash screen when the user
        // clicks on back or home buttons.

        if (keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_HOME) {

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * ReLogin to specific Platform.
     *
     * @param authenticator - authentication instance.
     * @param platform      - Authentication platform.
     */
    private void reLogin(Authenticator authenticator, Platform platform) {

        if (authenticator.isConnected()) {
            synchronized (platforms) {
                platforms.put(platform, true);

            }
            LoginCallBack loginCallCallback = new AuthUtils().new LoginCallBack(
                    platform, SplashActivity.this, SplashActivity.this, false,
                    TAG);
            authenticator.login(loginCallCallback);
            logDebug("Start reloading contacts after upgrading " + platform);
        }

    }

    @Override
    public void onLoginCompleted(Platform platform) {

        Authenticator authenticator = AuthUtils.getAuthInstance(platform, this);
        boolean platformFinished = false;
        switch (platform) {
            case FACEBOOK:
                if (authenticator.isConnected()) {
                    AuthUtils.UpdateUserProfile(this, authenticator, this,
                            Platform.FACEBOOK);
                } else {
                    platformFinished = true;
                }

                break;
            case HANGOUTS:
                if (authenticator.isConnected()) {
                    AuthUtils.UpdateUserProfile(this, authenticator, this,
                            Platform.HANGOUTS);
                } else {
                    platformFinished = true;
                }

                break;

            default:
                platformFinished = true;
        }

        synchronized (platforms) {
            if (platformFinished)
                platforms.put(platform, false);

            isAllPlatformsLoadingFinished();
        }
        logDebug(" Finished ReLoading contacts after upgrade for " + platform);
    }

    @Override
    public void onUserProfileLoaded(Platform platform,
                                    Contact profile, boolean success) {

        synchronized (platforms) {
            platforms.put(platform, false);
            isAllPlatformsLoadingFinished();
        }

        logDebug("Finish Updated profile after upgrade for " + platform);
    }

    @Override
    public void onContactsUpdated(List<Contact> contacts) {
    }

    @Override
    public void onInboxUpdated(RequestStatus status) {
    }

    /**
     * Wait all platforms to load their data
     */
    private void isAllPlatformsLoadingFinished() {
        if (!platforms.values().contains(true)) {
            redirector = new Redirector(SplashActivity.this);
            getPreferencesManager().setDatabaseUpgraded(false);
            //FeedProviderImpl.getInstance(getApplicationContext()).updateTopFriendsNotifications();
            redirector.Redirect();
        }
    }

    /**
     * Asynchronous task to initialize required data for application in
     * background.
     */
    class AppInitializer extends AsyncTask<String, String, String> {

        private FeedProviderImpl feedProvider = (FeedProviderImpl) FeedProviderImpl
                .getInstance(SplashActivity.this);

        @Override
        protected String doInBackground(String... params) {

            AppUtils.loadHeadboxConfiguration(SplashActivity.this);

            feedProvider.deleteAllMessages(Platform.CALL);
            feedProvider.deleteAllMessages(Platform.SMS);

            // If this is the first time we load the application
            int contactsCount = feedProvider.buildContacts();

            String feedHistoryMode = getPreferencesManager().getProperty(PreferencesManager.AB_FEED_HISTORY,
                    AnalyticsConstants.AB_FEED_HISTORY_FULL);

            if (feedHistoryMode.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_FULL)) {
                feedProvider.insertLogsHistory(0);
            } else if (feedHistoryMode.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_FIVE)) {
                feedProvider.insertLogsHistory(getPreferencesManager().getABFeedHistoryValue());
            } else if (feedHistoryMode.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_NONE)) {
                //Do nothing.
            }

            redirector.getPreferencesManager().setAppLoaded(true);
            // send the success login and loading contact event
            setUserProfileProperty(
                    AnalyticsConstants.CONTACT_COUNT_PROPERTY, contactsCount);

            logDebug("Loading History and contacts");

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (!isDBUpgraded) {
                redirector = new Redirector(SplashActivity.this);
                redirector.Redirect();
            } else {
                reloadDataAfterUpgrade();
            }
        }
    }

}
