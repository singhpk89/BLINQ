package com.blinq.authentication.impl.Google;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;

import com.blinq.analytics.BlinqAnalytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusClient.OnPeopleLoadedListener;
import com.google.android.gms.plus.PlusShare;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.blinq.HeadboxAccountsManager;
import com.blinq.HeadboxAccountsManager.AccountType;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.ContactsMapper;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.settings.GoogleSettings;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide methods and callbacks to authenticate Headbox user via google play
 * service. Also provide a way to handle connection/disconnection cases.
 *
 * @author Johan Hansson
 */
public class GooglePlusAuthenticator implements Authenticator {

    /**
     * Single GooglePlusAuthenticator instance.
     */
    private static GooglePlusAuthenticator instance;

    private static GoogleSettings settings = new GoogleSettings();
    private static PlusClient plusClient;
    private static Activity mActivity;
    private static HeadboxAccountsManager headboxAccountsManager;
    private List<Contact> friends = new ArrayList<Contact>();

    private static GoogleConnectionCallbacks connectionListener = null;
    private static GoogleConnectionFailedListener connectionFailedListener = null;
    private static PeopleLoadedListener peopleLoadedListener = null;

    private ConnectionResult mConnectionResult;

    public static final String[] SCOPES = {Scopes.PLUS_LOGIN,
            Scopes.PLUS_PROFILE};

    /**
     * Signed in successfully connection state.
     */
    private static final ConnectionResult CONNECTION_RESULT_SUCCESS = new ConnectionResult(
            ConnectionResult.SUCCESS, null);

    /**
     * An invalid request code to use to indicate that
     * {@link #login(com.blinq.authentication.Authenticator.LoginCallBack)} hasn't been called.
     */
    private static final int INVALID_REQUEST_CODE = -1;

    public static final String TAG = GooglePlusAuthenticator.class
            .getSimpleName();

    /**
     * The last result from onConnectionFailed.
     */
    private ConnectionResult mLastConnectionResult;

    /**
     * The request specified in signIn or INVALID_REQUEST_CODE if not signing in
     */

    private int mRequestCode;

    /**
     * Tracks if {@link PlusClient#connect()} has been called.
     */
    private boolean mIsConnecting;

    /**
     * Id of G+ contact to get.
     */
    private String userId;

    /**
     * Listener for profile/contact request.
     */
    private ProfileRequestCallback friendRequest;

    private static PreferencesManager preferencesManager;

    private GooglePlusAuthenticator(Activity activity) {
        initialize(activity);
    }

    /**
     * Returns the current Google Authenticator.
     *
     * @param activity the Activity to use.
     * @return GoogleAuthenticator Instance
     */
    public static GooglePlusAuthenticator getInstance(Activity activity) {

        mActivity = activity;

        if (instance == null) {
            instance = new GooglePlusAuthenticator(activity);
        }
        preferencesManager = new PreferencesManager(activity.getApplicationContext());
        headboxAccountsManager = HeadboxAccountsManager.getInstance();

        return instance;
    }

    /**
     * Initialize PlusClient Instance and register the connection/disconnection
     * callbacks.
     */
    private void initialize(Activity activity) {

        connectionListener = new GoogleConnectionCallbacks();
        connectionFailedListener = new GoogleConnectionFailedListener();
        peopleLoadedListener = new PeopleLoadedListener();
        plusClient = new PlusClient.Builder(activity.getApplicationContext(),
                connectionListener, connectionFailedListener).setScopes(SCOPES)
                .build();
    }

    /**
     * Called to sign out the user from account manager.
     */
    @Override
    public void logout(LogoutCallBack logoutCallback) {

        if (plusClient.isConnected()) {
            plusClient.clearDefaultAccount();
        }

        if (mIsConnecting || plusClient.isConnected()) {

            plusClient.disconnect();
            // Reconnect to get a new mPlusClient.
            mLastConnectionResult = null;
            // Cancel sign in.
            mRequestCode = INVALID_REQUEST_CODE;

            // Reconnect to fetch the sign-in (account chooser) intent from the
            // plus client.
            plusClient.connect();
        }
    }

    @Override
    public Action search(OnActionListener listener, String queryString, int limit) {
        return null;
    }

    @Override
    public String refreshToken(Context context) {

        // Refresh access token
        AccountManager accountManager = AccountManager.get(context);

        accountManager.invalidateAuthToken(GoogleSettings.ACCOUNT_TYPE,
                this.getAccessToken());

        return this.getAccessToken(context);
    }

    /**
     * Checks if the client is currently connected to the google play service,
     * so that requests to other methods will succeed.
     */
    public boolean isConnected() {
        return !StringUtils.isBlank(headboxAccountsManager.getAccountsByType(AccountType.GOOGLE).name);
    }

    @Override
    public void login(LoginCallBack loginCallBack) {

        connectionListener.loginCallBack = loginCallBack;
        connectionFailedListener.loginCallBack = loginCallBack;

        int available = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(mActivity
                        .getApplicationContext());
        if (available != ConnectionResult.SUCCESS) {
            mActivity
                    .showDialog(GoogleSettings.DIALOG_GET_GOOGLE_PLAY_SERVICES);
            return;
        }
        try {
            if (mConnectionResult != null)
                mConnectionResult.startResolutionForResult(mActivity,
                        GoogleSettings.REQUEST_CODE_SIGN_IN);
            else {
                if (!plusClient.isConnected() && !plusClient.isConnecting())
                    plusClient.connect();
            }
        } catch (IntentSender.SendIntentException e) {
            // Fetch a new result to start.
            if (!plusClient.isConnected() && !plusClient.isConnecting())
                plusClient.connect();
        }
    }

    @Override
    public void getProfile(final ProfileRequestCallback profileRequestCallback) {

        profileRequestCallback.onComplete(ContactsMapper.createContact(plusClient
                .getCurrentPerson()));
    }

    /**
     * Persist signed in account.
     *
     * @param accountName - google account name.
     */
    private void saveLoggedInAccount(String accountName) {

        Log.i(TAG, "you are connected with " + accountName);
        headboxAccountsManager.addAccount(AccountType.GOOGLE, accountName, "");
        String accessToken = getAccessToken();
        headboxAccountsManager.saveAccessToken(AccountType.GOOGLE, accessToken);
    }

    /**
     * Call this inside the activity
     */
    public boolean onActivityResult(Activity activity, int requestCode,
                                    int resultCode, Intent data) {

        if (requestCode == 0
                || requestCode == GoogleSettings.REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES) {
            if (resultCode == Activity.RESULT_OK && !plusClient.isConnected()
                    && !plusClient.isConnecting()) {
                // This time, connect should succeed.
                plusClient.connect();
            }
        } else if (requestCode == GoogleSettings.REQUEST_CODE_INTERACTIVE_POST)
            if (resultCode == Activity.RESULT_OK) {
                new BlinqAnalytics(activity.getApplicationContext()).sendEvent(
                        AnalyticsConstants.DRAWER_SPREAD_LOVE_SUCCESS_EVENT,
                        AnalyticsConstants.TYPE_PROPERTY,
                        Platform.HANGOUTS.getName(), true,
                        AnalyticsConstants.ACTION_CATEGORY);
            } else {
                Log.e(TAG, "Failed to create interactive post");
            }


        return false;
    }

    /**
     * Returns an OAuth 2.0 access token.
     */
    public String getAccessToken() {
        return getAccessToken(mActivity.getApplicationContext());
    }

    @Override
    public boolean isLoginCompleted() {

        return preferencesManager.isContactsLoaded(Platform.HANGOUTS);
    }

    @Override
    public void setLoginCompleted(boolean contactsLoaded) {

        preferencesManager.setContactsLoaded(Platform.HANGOUTS, contactsLoaded);
    }

    @Override
    public String getAccessToken(Context context) {

        String accessToken = "";
        String googleAccount = getAccountName(context);

        if (googleAccount != null) {
            Account account = new Account(googleAccount,
                    GoogleSettings.ACCOUNT_TYPE);
            AccountManagerFuture<Bundle> accountManagerResult = AccountManager
                    .get(context.getApplicationContext()).getAuthToken(account,
                            GoogleSettings.AUTH_TOKEN_TYPE, null, mActivity,
                            null, null);
            try {
                Bundle authTokenBundle = accountManagerResult.getResult();
                accessToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN)
                        .toString();
            } catch (OperationCanceledException e) {
                Log.d(TAG, e.getMessage() + "");

            } catch (AuthenticatorException e) {
                Log.d(TAG, e.getMessage() + "");

            } catch (IOException e) {
                Log.d(TAG, e.getMessage() + "");
            } catch (IllegalStateException e) {
                Log.d(TAG, e.getMessage() + "");
            }
        }
        return accessToken;

    }

    public static GoogleSettings getSettings() {
        return settings;
    }

    public static void setSettings(GoogleSettings settings) {
        GooglePlusAuthenticator.settings = settings;
    }

    /**
     * Provides callbacks that are called when the client is connected or
     * disconnected from Google.
     */
    private class GoogleConnectionCallbacks implements ConnectionCallbacks {

        public LoginCallBack loginCallBack;

        @Override
        public void onConnected(Bundle connectionHint) {

            // Successful connection!
            mLastConnectionResult = CONNECTION_RESULT_SUCCESS;
            mRequestCode = INVALID_REQUEST_CODE;
            mIsConnecting = false;

            if (loginCallBack != null) {

                saveLoggedInAccount(plusClient.getAccountName());
                loginCallBack.onLoggedIn();

            }

            if (userId != null && friendRequest != null) {

                getPeople(userId, friendRequest);

            }
        }

        @Override
        public void onDisconnected() {
            mIsConnecting = false;
        }


    }

    /**
     * Provides callbacks for scenarios that result in a failed attempt to
     * connect the client to the service.
     */
    private class GoogleConnectionFailedListener implements
            OnConnectionFailedListener {

        public LoginCallBack loginCallBack;

        @Override
        public void onConnectionFailed(ConnectionResult result) {

            mLastConnectionResult = result;
            mIsConnecting = false;

            // On a failed connection try again.
            if (mRequestCode != INVALID_REQUEST_CODE) {
                resolveLastResult();
            } else {
                Log.e(TAG, "connection with google+ failed!");
                if (loginCallBack != null) {
                    saveLoggedInAccount(null);
                    loginCallBack.onException("Connection Failed", null);
                }
            }
        }

        private void resolveLastResult() {
            if (GooglePlayServicesUtil
                    .isUserRecoverableError(mLastConnectionResult
                            .getErrorCode())) {
                mActivity
                        .showDialog(GoogleSettings.DIALOG_GET_GOOGLE_PLAY_SERVICES);
                return;
            }

            if (mLastConnectionResult.hasResolution()) {
                startResolution();
            }
        }

        /**
         * Resolves an error by starting any intents requiring user interaction.
         */
        private void startResolution() {
            try {
                mLastConnectionResult.startResolutionForResult(mActivity,
                        mRequestCode);
            } catch (SendIntentException e) {
                // The intent we had is not valid right now, perhaps the remote
                // process died.
                // Try to reconnect to get a new resolution intent.
                mLastConnectionResult = null;
                // TODO show progress dialog.
                plusClient.connect();
                mIsConnecting = true;
            }
        }
    }

    /**
     * Responsible for loading user friends. Called when requesting specific
     * contact by plusClient.loadVisiblePeople();
     *
     * @return list of contacts using friends Request CallBack.
     */
    private class PeopleLoadedListener implements OnPeopleLoadedListener {

        public FriendsRequestCallBack friendsRequestCallBack;
        /**
         * Returned people count.
         */
        private int count;

        @Override
        public void onPeopleLoaded(ConnectionResult status,
                                   PersonBuffer personBuffer, String nextPageToken) {

            switch (status.getErrorCode()) {
                case ConnectionResult.SUCCESS:
                    try {

                        count = personBuffer.getCount();
                        Log.i(TAG, "You have " + count + " G+ Friends");

                        for (int i = 0; i < count; i++) {
                            PeopleListener listener = new PeopleListener(this, i,
                                    personBuffer.get(i).getDisplayName());
                            plusClient.loadPeople(listener, personBuffer.get(i)
                                    .getId());
                        }

                    } finally {
                        personBuffer.close();
                    }
                    break;

                case ConnectionResult.SIGN_IN_REQUIRED:
                    plusClient.disconnect();
                    plusClient.connect();
                    break;
                default:
                    friendsRequestCallBack.onFail(mActivity
                            .getString(R.string.error_when_listing_g_contacts)
                            + ",Error Status = " + status);
                    break;
            }
        }

        public void addFriend(int id, Contact friend) {

            //Add to the list of friends.
            if (friend != null)
                friends.add(friend);

            //Check if whether we reached the last one or not.
            if (id == count - 1) {
                if (friends != null && friends.size() > 0)
                    friendsRequestCallBack.onComplete(friends);
                else
                    friendsRequestCallBack.onFail(mActivity
                            .getString(R.string.error_when_listing_g_contacts));
            }
        }
    }

    /**
     * Responsible for loading g+ contact. Called when requesting
     * certain contact by plusClient.loadPeople();
     */
    private class PeopleListener implements OnPeopleLoadedListener {

        public PeopleLoadedListener listener;
        private String name;
        /**
         * Requested people id.
         */
        private int id;

        public PeopleListener(PeopleLoadedListener listener, int id, String name) {
            this.listener = listener;
            this.name = name;
            this.id = id;
        }

        @Override
        public void onPeopleLoaded(ConnectionResult status,
                                   PersonBuffer personBuffer, String nextPageToken) {
            switch (status.getErrorCode()) {
                case ConnectionResult.SUCCESS:
                    try {

                        Contact friend = ContactsMapper.createFriend(personBuffer.get(0));
                        listener.addFriend(id, friend);
                        Log.i(TAG, friend.getName() + " ");

                    } catch (Exception e) {
                    } finally {
                        personBuffer.close();
                    }
                    break;

                case ConnectionResult.SIGN_IN_REQUIRED:
                    plusClient.disconnect();
                    plusClient.connect();
                    break;
                default:
                    Log.i("Connection Error", "Error when getting people: " + name);
                    listener.addFriend(id, null);
                    break;
            }
        }
    }

    public boolean isConnecting() {
        return mIsConnecting;
    }

    /**
     * Internal functionality to get certain g+ user details.
     *
     * @param userId - Google plus id.
     */
    private void getPeople(final String userId,
                           final ProfileRequestCallback friendRequest) {

        plusClient.loadPeople(new OnPeopleLoadedListener() {

            @Override
            public void onPeopleLoaded(ConnectionResult status,
                                       PersonBuffer personBuffer, String nextPageToken) {

                switch (status.getErrorCode()) {
                    case ConnectionResult.SUCCESS:
                        try {
                            //Convert to Headbox contact.
                            Contact friend = ContactsMapper
                                    .createFriend(personBuffer.get(0));
                            friendRequest.onComplete(friend);

                        } catch (Exception e) {

                            String message = mActivity
                                    .getString(R.string.error_when_listing_g_contacts);
                            friendRequest.onException(message, e);

                        } finally {
                            if (personBuffer != null)
                                personBuffer.close();
                        }
                        break;
                    case ConnectionResult.SIGN_IN_REQUIRED:
                        plusClient.disconnect();
                        plusClient.connect();
                        break;
                    default:
                        Log.i("Connection Error", "Error when getting people: "
                                + userId);
                        friendRequest.onFail();
                        break;
                }

            }
        }, userId);
    }

    @Override
    public void getFriends(FriendsRequestCallBack friendsRequestCallBack) {

        peopleLoadedListener.friendsRequestCallBack = friendsRequestCallBack;
        plusClient.loadVisiblePeople(peopleLoadedListener, null);
    }

    @Override
    public void getProfile(final String userId,
                           final ProfileRequestCallback profileRequestCallback) {

        if (profileRequestCallback == null)
            return;

        if (!isConnected()) {
            profileRequestCallback.onFail();
        }


        this.userId = userId;
        this.friendRequest = profileRequestCallback;

        if (!plusClient.isConnected())
            plusClient.connect();
        else {
            getPeople(userId, friendRequest);
        }

    }

    @Override
    public void getPosts(PostsRequestCallback postsRequestCallBack,
                         String userId) {
    }

    @Override
    public String getAccountName(Context context) {

        return headboxAccountsManager.getAccountsByType(AccountType.GOOGLE).name;
    }

    /**
     * Build and initialize Post intent.
     *
     * @param text - message to be shared.
     */
    private Intent getInteractivePostIntent(String text) {

        // Create an interactive post builder.
        PlusShare.Builder builder = new PlusShare.Builder(mActivity);

        String title = mActivity.getString(R.string.app_name);
        Uri photo = Uri.parse(mActivity.getString(R.string.share_picture));
        String link = mActivity.getString(R.string.share_link);
        builder.setContentDeepLinkId(link, title, text, photo);

        // Set the pre-filled message.
        builder.setText(text);
        return builder.getIntent();
    }

    /**
     * Share status on G+.
     */
    public void shareStatus(String status) {
        //Open an interactive post intent
        mActivity.startActivityForResult(getInteractivePostIntent(status),
                GoogleSettings.REQUEST_CODE_INTERACTIVE_POST);
    }

}