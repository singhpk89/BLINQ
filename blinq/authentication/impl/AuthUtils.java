package com.blinq.authentication.impl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.Analytics;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.Authenticator.FriendsRequestCallBack;
import com.blinq.authentication.impl.Google.GooglePlusAuthenticator;
import com.blinq.authentication.impl.Instagram.InstagramAuthenticator;
import com.blinq.authentication.impl.Twitter.TwitterAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.models.Contact;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.provider.FeedProvider;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.server.UserProfileCreator;
import com.blinq.service.platform.PlatformServiceBase;
import com.blinq.ui.activities.splash.SplashActivity;
import com.blinq.utils.AppUtils;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.DialogUtils.DialogType;
import com.blinq.utils.Log;
import com.blinq.utils.UIUtils;
import com.crashlytics.android.Crashlytics;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralize and manage login process to different platforms.
 */
@SuppressLint("DefaultLocale")
public class AuthUtils {

    private static final String TAG = AuthUtils.class.getSimpleName();

    /**
     * Any Activity or fragment should implement this interface in order to handle different actions
     * through our supported platforms.
     *
     * @author Johan Hansson.
     */
    public interface AuthAction {
        /**
         * Action to be executed after completing the login process.
         */
        public void onLoginCompleted(Platform platform);

        /**
         * Action to be executed after completing the loading user profile process.
         */
        public void onUserProfileLoaded(Platform platform, Contact profile, boolean success);

        /**
         * Responds to the updating user profiles action.
         */
        public void onContactsUpdated(List<Contact> contacts);

        /**
         * Responds to the updating facebook inbox action.
         *
         * @param status - loading status: COMPLETED,FAILED
         */
        public void onInboxUpdated(RequestStatus status);
    }

    /**
     * Implement {@link Authenticator.LoginCallBack} to authenticate headbox
     * user.
     */
    public class LoginCallBack implements Authenticator.LoginCallBack {

        /**
         * Authentication platform.
         */
        private Platform platform;
        private Context context;
        /**
         * Activity to start login from.
         */
        private Activity activity;
        /**
         * Action to execute after finishing the login.
         */
        private AuthAction loginCompleteAction;
        private LoginAnalytics loginAnalytics;
        private boolean showDialogs;
        private String loginViewName;

        public LoginCallBack(Platform platform, Activity activity,
                             AuthAction loginCompleteAction, boolean showDialogs,
                             String loginViewName) {
            this.platform = platform;
            this.context = activity.getApplicationContext();
            this.activity = activity;
            this.loginCompleteAction = loginCompleteAction;
            this.loginViewName = loginViewName;
            this.loginAnalytics = new LoginAnalytics(context, platform, loginViewName);
            this.showDialogs = showDialogs;
            loginAnalytics.sendLoginAnalytics();
        }

        @Override
        public void onLoggedIn() {

            loginAnalytics.sendSuccessLogin();

            if (platform == Platform.FACEBOOK && !loginViewName.equals(SplashActivity.TAG)) {

                loginCompleteAction.onLoginCompleted(platform);

            } else if (loginViewName.equals(AnalyticsConstants.LOGIN_FROM_SOCIAL_WINDOW) || loginViewName.equals(SplashActivity.TAG)) {

                LoadContacts(platform, loginCompleteAction, activity, showDialogs, loginViewName);

            } else {

                Log.e(TAG, "Login to " + platform.name() + " completed.");

                if (platform == Platform.TWITTER || platform == Platform.INSTAGRAM) {

                    PlatformServiceBase.startContactsFullSync(platform, context);
                    loginCompleteAction.onLoginCompleted(platform);
                    UserProfileCreator.getInstance(context).createProfile();

                } else if (platform == Platform.HANGOUTS) {
                    //TODO: Implement a service for hangouts contacts.
                    LoadContacts(platform, loginCompleteAction, activity, showDialogs, loginViewName);
                }

            }
        }

        @Override
        public void onException(String msg, Exception e) {
            Log.e(TAG, "Login to " + platform + " error :" + e);
            if (showDialogs) {
                UIUtils.showMessage(activity, msg);
            } else {

                if (loginCompleteAction != null)
                    loginCompleteAction.onLoginCompleted(platform);
            }
            loginAnalytics.sendFailedLoginAnalytics(platform, msg);

        }

        @Override
        public void onCancel() {
            if (showDialogs) {
                UIUtils.showMessage(activity,
                        activity.getString(R.string.retro_dialer_cancel_login));
                if (loginViewName.equals(AnalyticsConstants.LOGIN_FROM_SOCIAL_WINDOW)) { // notifiy that didnt login
                    loginCompleteAction.onLoginCompleted(null);
                }
            } else {

                if (loginCompleteAction != null)
                    loginCompleteAction.onLoginCompleted(platform);
            }
        }

        @Override
        public void doWhileLogin() {
        }

    }

    /**
     * Implement {@link Authenticator.FriendsRequestCallBack} to fetch
     * authenticated user's friends from different platforms.
     */
    public class FriendsCallBack implements FriendsRequestCallBack {

        private Platform platform;
        private Context context;
        private Activity activity;
        protected AlertDialog customWaitingDialog;
        private AuthAction loginCompleteAction;
        private boolean showDialogs;
        private String loginViewName;
        private Analytics analyticsManager;

        public FriendsCallBack(Platform platform, Activity activity,
                               AuthAction loginCompleteAction, boolean showDialogs,
                               String loginViewName) {

            this.platform = platform;
            this.context = activity.getApplicationContext();
            this.activity = activity;
            this.loginCompleteAction = loginCompleteAction;
            this.customWaitingDialog = DialogUtils.createCustomDialog(activity,
                    DialogType.CUSTOM);
            this.showDialogs = showDialogs;
            this.loginViewName = loginViewName;
            this.analyticsManager = new BlinqAnalytics(context);

            sendContactSyncEvent(AnalyticsConstants.STARTED_VALUE);

        }

        private void sendContactSyncEvent(String syncState) {
            String eventName = AnalyticsConstants.CONTACTS_SYNC_EVENT
                    + syncState;
            analyticsManager.sendEvent(eventName,
                    AnalyticsConstants.TYPE_PROPERTY,
                    platform.getName(), false,
                    AnalyticsConstants.ONBOARDING_CATEGORY);

        }

        @Override
        public void onGettingFriends() {
            if (showDialogs)
                DialogUtils.showDialog(activity, customWaitingDialog);

        }

        @Override
        public void onFail(String msg) {
            sendContactSyncEvent(AnalyticsConstants.FAILED_VALUE);
            if (showDialogs) {
                DialogUtils.hideDialog(activity, customWaitingDialog);
                AuthUtils.showRetryLoadingDialog(activity, platform,
                        loadContactsDialog);
                Log.e(TAG, "load contacts fail for platform " + platform + " with " + msg);
            } else {

                if (loginCompleteAction != null)
                    loginCompleteAction.onLoginCompleted(platform);
            }
        }

        @Override
        public void onException(String msg, Exception exception) {

            Crashlytics.logException(exception);
            sendContactSyncEvent(AnalyticsConstants.FAILED_VALUE);
            getAuthInstance(platform, activity).setLoginCompleted(false);
            if (showDialogs) {
                DialogUtils.hideDialog(activity, customWaitingDialog);
                reloadContacts(platform, activity);
                Log.e(TAG, "load contacts exception for " + platform
                        + exception.toString());
            } else {

                if (loginCompleteAction != null)
                    loginCompleteAction.onLoginCompleted(platform);
            }
        }

        @Override
        public void onComplete(List<Contact> friends) {

            long startTime = System.currentTimeMillis();
            Provider provider = FeedProviderImpl.getInstance(context);
            provider.buildContacts(friends, platform);
            Log.i(TAG, "---Time taken to build " + platform + "'s contacts =" + AppUtils.findTime(startTime));
            sendContactSyncEvent(AnalyticsConstants.COMPLETED_VALUE);

            if (showDialogs)
                DialogUtils.hideDialog(activity, customWaitingDialog);
            LoginAnalytics loginAnalytics = new LoginAnalytics(context,
                    platform, loginViewName);
            loginAnalytics
                    .updateUserProfilePlatformFriendsCount(friends.size());

            if (loginCompleteAction != null)
                loginCompleteAction.onLoginCompleted(platform);
        }

        /**
         * retry click handler that reload the contacts .
         */
        DialogInterface.OnClickListener loadContactsDialog = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                LoadContacts(platform, loginCompleteAction, activity, true,
                        loginViewName);
            }

        };

        public void reloadContacts(Platform platform, Activity activity) {

            AuthUtils.showRetryLoadingDialog(activity, platform,
                    loadContactsDialog);

        }

    }

    private static int count;
    private static ImageView refreshButton;
    private static ProgressBar refreshBar;

    /**
     * Load certain platform Contacts.
     *
     * @param showDialogs
     */
    public static void LoadContacts(Platform platform,
                                    AuthAction loginCompleteAction, Activity activity,
                                    boolean showDialogs, String loginViewName) {
        FriendsCallBack friendsCallback = new AuthUtils().new FriendsCallBack(
                platform, activity, loginCompleteAction, showDialogs,
                loginViewName);

        PreferencesManager preferencesManager = new PreferencesManager(activity);

        switch (platform) {

            case FACEBOOK:

                FacebookAuthenticator.getInstance(activity).getFriends(
                        friendsCallback);
                break;
            case INSTAGRAM:
                InstagramAuthenticator.getInstance(activity).getFriends(
                        friendsCallback);
                break;
            case TWITTER:
                TwitterAuthenticator.getInstance(activity).getFriends(
                        friendsCallback);
                break;
            default:
                break;
        }

    }

    private static List<Platform> ignoredPlatforms = new ArrayList<Platform>() {
        private static final long serialVersionUID = 4707177610685408281L;

        {
            add(Platform.EMAIL);
            add(Platform.SKYPE);
            add(Platform.CALL);
            add(Platform.WHATSAPP);
        }
    };

    /**
     * Update list of contacts for each platform.
     *
     * @param activity - activity to start update from.
     * @param action   - action to be executed after completing updating process.
     * @
     */
    public static void updateContacts(final Activity activity,
                                      HashMap<Platform, List<MemberContact>> contacts, AuthAction action
    ) {


        // Initialize list of Callback to to use while updating multiple
        // contacts.
        final List<AuthUtils.UpdateFriendRequestCallback> calls = new ArrayList<AuthUtils.UpdateFriendRequestCallback>();

        int index = 0;
        for (Platform platform : contacts.keySet()) {

            // We don't need to update some types contacts.
            if (!ignoredPlatforms.contains(platform)) {
                for (MemberContact contact : contacts.get(platform)) {

                    String userId = contact.getId();

                    if (!StringUtils.isBlank(userId)) {
                        // Create a call back to use while getting profile from
                        // certain platform.
                        AuthUtils.UpdateFriendRequestCallback callBack = new AuthUtils().new UpdateFriendRequestCallback(
                                index, platform, activity, action, userId);
                        index++;
                        calls.add(callBack);
                    }
                }
            }
        }

        count = calls.size();
        // Check if we have contacts to update.
        if (count > 0) {

            for (UpdateFriendRequestCallback callBack : calls) {
                // Start updating contact.
                getAuthInstance(callBack.getPlatform(), activity).getProfile(
                        callBack.getContactId(), callBack);
            }
        }
    }

    /**
     * Implement {@link Authenticator.ProfileRequestCallback} to fetch and
     * update certain contact from different platforms.
     */
    public class UpdateFriendRequestCallback implements
            Authenticator.ProfileRequestCallback {

        private Context context;
        /**
         * Updated contact index on the list.
         */
        private int id;
        /**
         * The real if of the contact on Facebook,Google,Twitter,etc..
         */
        private String contactId;
        /**
         * Activity to start loading process from.
         */
        private Activity activity;
        /**
         * Contact paltform.
         */
        private Platform platform;
        /**
         * Action to be execute after finishing the update
         */
        private AuthAction action;

        public UpdateFriendRequestCallback(int id, Platform platform,
                                           Activity activity, AuthAction action, String contactId) {

            this.id = id;
            this.context = activity.getApplicationContext();
            this.activity = activity;
            this.platform = platform;
            this.contactId = contactId;
            this.action = action;
        }

        @Override
        public void onException(String msg, Exception e) {

            Log.d(TAG, "Such exception happened while updating friend with Id="
                    + contactId);
            completeLoading();

        }

        @Override
        public void onFail() {

            Log.d(TAG, "Connection error while fetching friend with Id="
                    + contactId);
            completeLoading();
        }

        @Override
        public void onGettingProfile() {
            Log.d(TAG, "fetching user with id = " + contactId + " on "
                    + platform.getName());
        }

        @Override
        public void onComplete(Contact contact) {

            Log.d(TAG, platform.getName() + " Friend with Id = " + contactId
                    + " has been successfully updated.");
            completeLoading();
        }

        /**
         * Check if the loading is completed to call onLoadingCompeleted Action.
         */
        private void completeLoading() {

            Log.d(TAG, "Complete updating contacts.");

            if (id == count - 1) {
                if (AppUtils.isActivityActive(getActivity())) {
                    action.onContactsUpdated(null);
                }
            }

        }

        public Activity getActivity() {
            return activity;
        }

        public String getContactId() {
            return contactId;
        }

        public Platform getPlatform() {
            return platform;
        }

    }

    public enum RequestStatus {
        COMPLETED, FAILED
    }

    ;

    /**
     * Implement {@link Authenticator.ProfileRequestCallback} to fetch
     * authenticated user's profile for different platforms.
     */
    public class ProfileRequestCallback implements
            Authenticator.ProfileRequestCallback {

        private String TAG = ProfileRequestCallback.class.getSimpleName();
        private Context context;
        private Activity activity;
        private PreferencesManager preferences;
        private Platform platform;
        private AuthAction authAction;
        private LoginAnalytics loginAnalytics;

        public ProfileRequestCallback(Platform platform, Activity activity,
                                      AuthAction authAction) {

            this.activity = activity;
            this.context = activity.getApplicationContext();
            this.platform = platform;
            this.authAction = authAction;
            this.preferences = new PreferencesManager(context);
            this.loginAnalytics = new LoginAnalytics(context, platform);
        }

        @Override
        public void onGettingProfile() {

        }

        @Override
        public void onFail() {

            if (platform == Platform.FACEBOOK)
                preferences.setFacebookProfileLoadingStatus(false);

            if (authAction != null)
                authAction.onUserProfileLoaded(platform, null, false);
        }

        @Override
        public void onException(String msg, Exception exception) {
            if (platform == Platform.FACEBOOK)
                preferences.setFacebookProfileLoadingStatus(false);

            if (authAction != null)
                authAction.onUserProfileLoaded(platform, null, false);
        }

        @Override
        public void onComplete(Contact profile) {

            if (platform == Platform.FACEBOOK) {

                preferences.setFacebookProfileLoadingStatus(true);
                loginAnalytics.updateUserProfileNameAnalytics(profile);
            }

            Log.d(TAG, platform.getName() + " Name: " + profile.getName());

            loginAnalytics.setUserProfileProperties(profile);
            if (authAction != null)
                authAction.onUserProfileLoaded(platform, profile,
                        true);

        }

        public Activity getActivity() {
            return activity;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }
    }

    ;

    /**
     * Check whether the platform is connected or not.
     */
    public static boolean isConnected(Platform platform, Activity activity) {
        return getAuthInstance(platform, activity).isConnected();
    }

    /**
     * Check whether the login process is completed.
     *
     * @param platform - authentication platform
     * @return
     */
    public static boolean isLoginCompleted(Platform platform, Activity activity) {

        return getAuthInstance(platform, activity).isLoginCompleted();
    }

    public static Authenticator getAuthInstance(Platform platform,
                                                Activity activity) {

        switch (platform) {
            case FACEBOOK:
                return FacebookAuthenticator.getInstance(activity);
            case HANGOUTS:
                return GooglePlusAuthenticator.getInstance(activity);
            case INSTAGRAM:
                return InstagramAuthenticator.getInstance(activity);
            case TWITTER:
                return TwitterAuthenticator.getInstance(activity);
            default:
                return null;
        }
    }

    public static boolean isLoginCompleted(Platform platform, Context context) {

        return getAuthInstance(platform, context).isLoginCompleted();
    }

    public static Authenticator getAuthInstance(Platform platform,
                                                Context context) {

        switch (platform) {
            case FACEBOOK:
                return FacebookAuthenticator.getInstance(context);
            case INSTAGRAM:
                return InstagramAuthenticator.getInstance(context);
            case TWITTER:
                return TwitterAuthenticator.getInstance(context);
            default:
                return null;
        }
    }

    /**
     * Update authenticated user profile in certain platform.
     */
    public static void UpdateUserProfile(final Activity activity,
                                         final Authenticator auth, final AuthAction authAction,
                                         final Platform platform) {

        if (!AppUtils.isActivityActive(activity))
            return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                auth.getProfile(new AuthUtils().new ProfileRequestCallback(
                        platform, activity, authAction));
            }
        });

    }

    /**
     * Show Retry Load Dialog.
     *
     */
    public static void showRetryLoadingDialog(final Activity activity,
                                              final Platform platform, final OnClickListener retryClickListener) {

        if (!AppUtils.isActivityActive(activity))
            return;

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                String message = activity
                        .getString(R.string.loading_contacts_failed);
                DialogUtils
                        .createConfirmAlertDialog(
                                activity,
                                platform.getName(),
                                message,
                                retryClickListener,
                                activity.getString(R.string.retry_load_contacts),
                                activity.getString(R.string.cancel_load_contacts),
                                null).show();

            }
        });

    }

    /**
     * Responsible for sending the analytics for all authentication operations.
     */
    public class LoginAnalytics {

        private Analytics analyticsManager;
        private Platform platform;
        private String loginViewName;

        public LoginAnalytics(Context context, Platform platform) {
            analyticsManager = new BlinqAnalytics(context);
            this.platform = platform;
        }

        public LoginAnalytics(Context context, Platform platform,
                              String loginViewName) {
            this(context, platform);
            this.loginViewName = loginViewName;
        }

        /**
         * Update the platform contacts count.
         */
        public void updateUserProfilePlatformFriendsCount(int friendsCount) {

            String contactsCountProperty = String.format(
                    AnalyticsConstants.CONTACTS_COUNT_PROPERTY, platform
                            .getName().toLowerCase()
            );
            analyticsManager.setUserProfileProperty(contactsCountProperty,
                    friendsCount);
        }

        /**
         * Update the user profile name.
         */
        public void updateUserProfileNameAnalytics(Contact profile) {

            String name = profile.getName();

            if (StringUtils.isBlank(name))
                return;

            analyticsManager.setUserProfileProperty(
                    AnalyticsConstants.FISRT_NAME_PROFILE_PROPERTY,
                    com.blinq.utils.StringUtils.getFirstName(name));
            analyticsManager.setUserProfileProperty(
                    AnalyticsConstants.LAST_NAME_PROFILE_PROPERTY,
                    com.blinq.utils.StringUtils.getLastName(name));
            analyticsManager.setUserProfileProperty(
                    AnalyticsConstants.EMAIL_PROFILE_PROPERTY, profile.getEmail());
        }

        /**
         * Send success login event to certain platform.
         */
        public void sendSuccessLogin() {

            if (platform == Platform.FACEBOOK) {
                String eventName = AnalyticsConstants.FACEBOOK_LOGIN_EVENT
                        + AnalyticsConstants.SUCCESS_VALUE;
                analyticsManager.sendEvent(eventName, false,
                        AnalyticsConstants.ONBOARDING_CATEGORY);

            }

        }

        /**
         * Send login event to certain platform.
         */
        public void sendLoginAnalytics() {

            String platformName = platform.name().toLowerCase();

            if (!StringUtils.isBlank(loginViewName) && !loginViewName.equals(SplashActivity.TAG)) {

                String category = getEventCategoryByView(loginViewName);

                Log.d(TAG, "Login with " + platformName + " From "
                        + loginViewName);
                String loggedInEvent = String.format(
                        AnalyticsConstants.LOGGED_IN_EVENT, platformName,
                        loginViewName);
                analyticsManager.sendEvent(loggedInEvent, true, category);
            }
        }

        private String getEventCategoryByView(String viewName) {
            if (viewName == AnalyticsConstants.LOGIN_FROM_LOGIN_SCREEN)
                return AnalyticsConstants.ONBOARDING_CATEGORY;

            return AnalyticsConstants.ACTION_CATEGORY;
        }

        /**
         * Update user platform name and ID properties like (Facebook Name and
         * Facebook ID).
         */
        public void setUserProfileProperties(Contact profile) {

            if (profile == null)
                return;

            String idProperty = String.format(
                    AnalyticsConstants.PROFILE_ID_PROPERTY,
                    platform.getName());
            String nameProperty = String.format(
                    AnalyticsConstants.PROFILE_NAME_PROPERTY,
                    platform.getName());

            analyticsManager.setUserProfileProperty(nameProperty,
                    profile.getName());
            analyticsManager.setUserProfileProperty(idProperty,
                    profile.getContactId());
        }

        /**
         * Send failure login event to certain platform.
         */
        public void sendFailedLoginAnalytics(Platform platform, String msg) {

            if (!StringUtils.isBlank(loginViewName) && !loginViewName.equals(SplashActivity.TAG)) {
                if (platform == Platform.FACEBOOK) {
                    String eventName = AnalyticsConstants.FACEBOOK_LOGIN_EVENT
                            + AnalyticsConstants.FAILED_VALUE;
                    analyticsManager.sendEvent(eventName, false,
                            AnalyticsConstants.ONBOARDING_CATEGORY);
                } else {
                    HashMap<String, Object> properties = new HashMap<String, Object>();
                    properties.put(AnalyticsConstants.STATUS_PROPERTY,
                            AnalyticsConstants.FAILURE_VALUE);
                    properties.put(AnalyticsConstants.TYPE_PROPERTY,
                            platform.getName());
                    properties.put(AnalyticsConstants.FROM_PROPERTY,
                            loginViewName);
                    properties.put(
                            AnalyticsConstants.EXCEPTION_MESSAGE_PROPERTY, msg);

                    analyticsManager.sendEvent(AnalyticsConstants.LOGIN_EVENT,
                            properties, false,
                            AnalyticsConstants.ERRORS_CATEGORY);
                }
            }
        }

    }

    public class MessagesRequestCallback implements
            Authenticator.MessagesRequestCallback {

        private Activity activity;
        private AlertDialog dialog;
        private PreferencesManager preferences;
        private AuthAction action;

        public MessagesRequestCallback(Activity activity, AuthAction action) {
            this.activity = activity;
            this.preferences = new PreferencesManager(activity);
            this.action = action;
        }

        @Override
        public void onException(String msg, Exception e) {
            DialogUtils.hideDialog(getActivity(), dialog);
            action.onInboxUpdated(RequestStatus.FAILED);
        }

        @Override
        public void onFail(String msg) {
            DialogUtils.hideDialog(getActivity(), getDialog());
            action.onInboxUpdated(RequestStatus.FAILED);
        }

        @Override
        public void onGettingMessages() {
            DialogUtils.showDialog(getActivity(), getDialog());
        }

        @Override
        public void onComplete(Map<String, ContentValues[]> messages) {

            long now = System.currentTimeMillis();
            preferences.setFacebookHistoryLastLoadingTime(now);
            FeedProvider.setMessages(messages);
            getActivity().getContentResolver().query(FeedProvider.FACEBOOK_URI,
                    null, null, null, null);
            Log.i(TAG, "---Time taken to save facebook's messages  =" + AppUtils.findTime(now));
            DialogUtils.hideDialog(getActivity(), getDialog());
            action.onInboxUpdated(RequestStatus.COMPLETED);

        }

        public Activity getActivity() {
            return activity;
        }

        public void setDialog(AlertDialog dialog) {
            this.dialog = dialog;
        }

        public AlertDialog getDialog() {
            return dialog;
        }

        public void createDialog() {
            dialog = DialogUtils.createCustomDialog(getActivity(),
                    DialogType.CUSTOM);
        }
    }

    /**
     * TO check whether a certain platform is completely logged in/authenticated or not.
     *
     * @param platform -  platform to get status for.
     * @return
     */
    public static boolean getAuthenticationStatus(Activity activity, Platform platform) {

        switch (platform) {

            case FACEBOOK:
            case HANGOUTS:
            case TWITTER:
            case INSTAGRAM:
                return AuthUtils.isLoginCompleted(
                        platform, activity);
            case SKYPE:
            case WHATSAPP:
            case EMAIL:
                return true;
        }

        return false;
    }

    /**
     * TO check whether a certain platform is authenticated/connected or not.
     *
     * @param platform -  platform to get status for.
     * @return
     */
    public static boolean getConnectivityStatus(Activity activity, Platform platform) {

        switch (platform) {

            case FACEBOOK:
            case HANGOUTS:
            case TWITTER:
            case INSTAGRAM:
                return AuthUtils.isConnected(platform,
                        activity);
            case LINKEDIN:
                //Temporary.
                return false;
            case SKYPE:
            case WHATSAPP:
            case EMAIL:
                return true;
        }
        return false;
    }

    /**
     * To establish new connection with platform if it's not connected.
     * Or to enable the stream notifications for other platforms.
     */
    public static void connect(Activity activity, AuthAction authAction, Platform platform, String screenName) {

        switch (platform) {

            case FACEBOOK:
            case HANGOUTS:
            case TWITTER:
            case INSTAGRAM:
                if (!AuthUtils.isConnected(platform, activity)) {
                    LoginCallBack loginCallCallback = new AuthUtils().new LoginCallBack(
                            platform, activity, authAction, true, screenName);
                    AuthUtils.getAuthInstance(platform, activity).login(
                            loginCallCallback);
                }
            case SKYPE:
            case WHATSAPP:
            case EMAIL:
                return;
        }
    }


    /**
     * Disconnect a connected platform
     * Or to disable the stream notifications for other platforms.
     */
    public static void disconnect(Activity activity, AuthAction authAction, Platform platform) {

        switch (platform) {

            case FACEBOOK:
            case HANGOUTS:
            case TWITTER:
            case INSTAGRAM:
                break;
            case SKYPE:
            case WHATSAPP:
            case EMAIL:
                return;
        }
    }

}
