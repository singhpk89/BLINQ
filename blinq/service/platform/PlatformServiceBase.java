package com.blinq.service.platform;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.blinq.PreferencesManager;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.utils.Constants;
import com.blinq.utils.ServerUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A Base service class to initialize and configure common functionalities
 * for the supported platforms.
 */
public abstract class PlatformServiceBase extends IntentService {


    // Intent Actions
    protected static final String PREFIX = "com.blinq.service.";

    protected static final String ACTION_SYNC_CONTACT = PREFIX
            + "ACTION_SYNC_CONTACT_NAMES";
    protected static final String ACTION_SYNC_CONTACTS = PREFIX
            + "ACTION_SYNC_CONTACTS";
    protected static final String ACTION_SYNC_FULL_CONTACTS = PREFIX
            + "ACTION_SYNC_FULL_CONTACTS";
    protected static final String ACTION_RELOAD_FACEBOOK_INBOX = PREFIX
            + "ACTION_RELOAD_FB_INBOX";
    protected static final String ACTION_RELOAD_PLATFORM_CONTACTS = PREFIX
            + "ACTION_RELOAD_PLATFORM_CONTACTS";

    protected static final String EXTRA_CONTACT_ID = PREFIX + "CONTACT_ID";

    protected Authenticator authenticator;
    protected PreferencesManager preferences;
    protected Provider provider;
    protected String action;

    public PlatformServiceBase(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        preferences = new PreferencesManager(getApplicationContext());
        provider = FeedProviderImpl.getInstance(getApplicationContext());
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        action = intent.getAction();
        if (action == null)
            return;

        if (ACTION_SYNC_CONTACT.equals(action)) {
            syncContact(intent);
        } else if (ACTION_SYNC_CONTACTS.equals(action)) {
            syncContacts(intent);
        } else if (ACTION_SYNC_FULL_CONTACTS.equals(action)) {
            fullSyncContacts();
        }

        // Finish the execution of the previous startWakefulService.
    }

    protected abstract void syncContact(Intent intent);

    protected abstract void syncContacts(Intent intent);

    protected abstract void fullSyncContacts();

    private static Intent getServiceIntent(Context context, Platform platform) {

        switch (platform) {
            case FACEBOOK:
                return new Intent(context, FacebookUtilsService.class);
            case HANGOUTS:
                return new Intent(context, GoogleUtilsService.class);
            case TWITTER:
                return new Intent(context, TwitterUtilsService.class);
            case INSTAGRAM:
                return new Intent(context, InstagramUtilsService.class);
        }
        return null;
    }

    /**
     * Update certain contact details.
     */
    public static void startSyncContact(Context context, Platform platform,
                                        String contactId) {

        boolean found = FeedProviderImpl.getInstance(context).contactExists(
                contactId);

        if (!found) {
            Intent intent = getServiceIntent(context, platform);
            intent.setAction(PlatformServiceBase.ACTION_SYNC_CONTACT);
            intent.putExtra(PlatformServiceBase.EXTRA_CONTACT_ID, contactId);
            WakefulBroadcastReceiver.startWakefulService(context, intent);
        }
    }

    /**
     * Update list of contacts for a given platform.
     */
    public static void startSyncContacts(Context context, Platform platform,
                                         ArrayList<String> contacts) {

        Intent intent = getServiceIntent(context, platform);
        intent.setAction(PlatformServiceBase.ACTION_SYNC_CONTACTS);
        intent.putExtra(PlatformServiceBase.EXTRA_CONTACT_ID, contacts);
        WakefulBroadcastReceiver.startWakefulService(context, intent);
    }

    public static void startContactsFullSync(Platform platform, Context context) {

        Intent intent = getServiceIntent(context, platform);
        intent.setAction(PlatformServiceBase.ACTION_SYNC_FULL_CONTACTS);
        WakefulBroadcastReceiver.startWakefulService(context, intent);
    }

    /**
     * Update FACEBOOK chat inbox.
     */
    public static void updateFacebookInbox(Context context) {

        Intent intent = getServiceIntent(context, Platform.FACEBOOK);
        intent.setAction(PlatformServiceBase.ACTION_RELOAD_FACEBOOK_INBOX);
        WakefulBroadcastReceiver.startWakefulService(context, intent);
    }

    /**
     * Implement {@link Authenticator.FriendsRequestCallBack} to fetch
     * authenticated user's friends from different platforms.
     */
    protected class FriendsCallBack implements Authenticator.FriendsRequestCallBack {

        private Platform platform;
        private String loginViewName;
        private AnalyticsSender analyticsSender;

        public FriendsCallBack(Platform platform, String loginViewName) {

            this.platform = platform;
            this.loginViewName = loginViewName;
            this.analyticsSender = new AnalyticsSender(getApplicationContext());
            this.analyticsSender.sendContactSyncEvent(AnalyticsConstants.STARTED_VALUE, platform);
        }


        @Override
        public void onGettingFriends() {

        }

        @Override
        public void onFail(String msg) {

            analyticsSender.sendContactSyncEvent(AnalyticsConstants.FAILED_VALUE, platform);
            AuthUtils.getAuthInstance(platform, getApplicationContext()).setLoginCompleted(false);

        }

        @Override
        public void onException(String msg, Exception exception) {

            analyticsSender.sendContactSyncEvent(AnalyticsConstants.FAILED_VALUE, platform);
            AuthUtils.getAuthInstance(platform, getApplicationContext()).setLoginCompleted(false);

        }

        @Override
        public void onComplete(List<Contact> friends) {

            Provider provider = FeedProviderImpl.getInstance(getApplicationContext());
            provider.buildContacts(friends, platform);
            setContactsSyncEnded(platform);
            analyticsSender.sendContactSyncEvent(AnalyticsConstants.COMPLETED_VALUE, platform);

            AuthUtils.LoginAnalytics loginAnalytics = new AuthUtils().new LoginAnalytics(getApplicationContext(), platform, loginViewName);
            loginAnalytics.updateUserProfilePlatformFriendsCount(friends.size());

        }

        private void setContactsSyncEnded(Platform platform) {
            switch (platform) {
                case INSTAGRAM:
                    preferences.setInstagramContactsSyncStatus(Constants.SYNC_ENDED);
                    break;
                case TWITTER:
                    preferences.setTwitterContactsSyncStatus(Constants.SYNC_ENDED);
                    break;
                default:
                    return;
            }
            ServerUtils.getInstance(getApplicationContext()).sendContactsDatabase();
        }
    }
}
