package com.blinq.authentication.impl.Google;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.blinq.R;
import com.blinq.HeadboxAccountsManager;
import com.blinq.HeadboxAccountsManager.AccountType;
import com.blinq.PreferencesManager;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.ContactsMapper;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.settings.GoogleSettings;
import com.blinq.utils.Log;
import com.blinq.utils.NetworkUtils;
import com.blinq.utils.StringUtils;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Johan Hansson
 *         <p/>
 *         NOTE : not completed.
 */
public final class GoogleAuthenticator implements Authenticator {

    /**
     * Status code to check for Google service availability.
     */
    private static final int NOT_AVAILABLE = 0;

    private static final String TAG = GoogleAuthenticator.class.getSimpleName();

    /**
     * single GoogleAuthenticator object.
     */
    private static GoogleAuthenticator instance;

    /**
     * Manages account selection and authorization for Google accounts.
     */
    private static GoogleAccountCredential credential;

    private static GoogleSettings settings = new GoogleSettings();

    private static Activity mActivity;
    private static HeadboxAccountsManager headboxAccountsManager;

    private ProfileRequestCallback profileRequestCallback;

    private LoginCallBack loginCallBack;

    private static PreferencesManager preferencesManager;

    private static Context context;

    /**
     * Returns the current Google Authenticator, or null if there is none.
     *
     * @param activity the Activity to use.
     * @return GoogleAuthenticator Instance
     */
    public static GoogleAuthenticator getInstance(Activity activity,
                                                  Context context) {
        if (instance == null) {
            instance = new GoogleAuthenticator();
        }
        // TODO handle it by another way.
        try {
            credential = GoogleAccountCredential.usingAudience(
                    activity.getApplicationContext(), GoogleSettings.AUDIENCE);
        } catch (Exception e) {
        }

        mActivity = activity;
        GoogleAuthenticator.context = context;
        preferencesManager = new PreferencesManager(context);
        headboxAccountsManager = HeadboxAccountsManager.getInstance();
        return instance;
    }


    public GoogleAccountCredential getCredential() {
        return credential;
    }

    /**
     * Called to sign out the user from account manager.
     */
    @Override
    public void logout(LogoutCallBack logoutCallback) {
        credential.setSelectedAccountName("");
    }

    @Override
    public Action search(OnActionListener listener, String queryString, int limit) {
        return null;
    }

    /**
     * Refresh Google Authentication token.
     */
    @Override
    public String refreshToken(Context context) {

        // Refresh access token
        AccountManager manager = AccountManager.get(context);

        manager.invalidateAuthToken(GoogleSettings.ACCOUNT_TYPE,
                this.getAccessToken());

        return this.getAccessToken(context);
    }

    /**
     * Retrieves the used account name and checks if the credential object can
     * be set to this account.
     */
    @Override
    public boolean isConnected() {

        return !StringUtils.isBlank(headboxAccountsManager.getAccountsByType(AccountType.GOOGLE).name);
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
    public void login(LoginCallBack loginCallBack) {
        this.loginCallBack = loginCallBack;
        mActivity.startActivityForResult(getCredential()
                        .newChooseAccountIntent(),
                GoogleSettings.REQUEST_ACCOUNT_PICKER
        );
    }

    @Override
    public void getProfile(final ProfileRequestCallback profileRequestCallback) {

        this.profileRequestCallback = profileRequestCallback;
        profileAsyncTask profileAsyncTask = new profileAsyncTask(mActivity,
                getAccountName(mActivity.getApplicationContext()),
                GoogleSettings.SCOPE,
                GoogleSettings.REQUEST_CODE_RECOVER_FROM_AUTH_ERROR);
        profileAsyncTask.execute();

    }

    /**
     * Call this inside the activity
     */
    public boolean onActivityResult(Activity activity, int requestCode,
                                    int resultCode, Intent data) {
        switch (requestCode) {
            case GoogleSettings.REQUEST_ACCOUNT_PICKER:
                if (data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        saveLoggedInAccount(accountName);
                        Log.d("Account Name: ", credential.getSelectedAccountName()
                                + "");
                        loginCallBack.onLoggedIn();
                    }
                }
                break;
            case GoogleSettings.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != Activity.RESULT_OK) {
                    int statusCode = NetworkUtils
                            .checkGooglePlayAvailability(mActivity.getBaseContext());
                    if (statusCode != NOT_AVAILABLE) {
                        loginCallBack
                                .onException(
                                        activity.getString(R.string.google_play_service_not_available_),
                                        null);
                    }
                }
                break;
            default:
                break;
        }
        return false;
    }

    private void saveLoggedInAccount(String accountName) {

        Log.i(TAG, "you are connected now with " + accountName);

        String accessToken = getAccessToken();
        headboxAccountsManager.addAccount(AccountType.GOOGLE, accountName, accessToken);

        credential.setSelectedAccountName(accountName);
    }

    /**
     * Returns an OAuth 2.0 access token.
     */
    public String getAccessToken() {

        return getAccessToken(context);

    }

    @Override
    public String getAccessToken(Context context) {

        String accessToken = "";
        String googleAccount = getAccountName(context);

        if (googleAccount == null || googleAccount.equals("") || googleAccount.equals("null"))
            return  null;

            Account account = new Account(googleAccount,
                    GoogleSettings.ACCOUNT_TYPE);

            AccountManagerFuture<Bundle> accountManagerResult = AccountManager
                    .get(context.getApplicationContext()).getAuthToken(account,
                            GoogleSettings.AUTH_TOKEN_TYPE, null, true, null,
                            null);

            try {
                Bundle authTokenBundle = accountManagerResult.getResult();
                accessToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN)
                        .toString();
            } catch (Exception e) {
                Log.d(TAG,
                        "Error while getting access token : " + e.getMessage());
            }

        return accessToken;
    }

    @Override
    public String getAccountName(Context context) {

        return headboxAccountsManager.getAccountsByType(AccountType.GOOGLE).name;

    }

    /**
     * Returns the selected Google account name (e-mail address), for example
     * example@gmail.com.
     */
    public String getAccountName() {
        return credential.getSelectedAccountName();
    }

    /**
     * Set selected Google account name (e-mail address), for example
     * example@gmail.com.
     */
    public void setAccountName(String accountName) {
        credential.setSelectedAccountName(accountName);
    }

    public static GoogleSettings getSettings() {
        return settings;
    }

    public static void setSettings(GoogleSettings settings) {
        GoogleAuthenticator.settings = settings;
    }

    public class profileAsyncTask extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "profileAsyncTask";

        // Response code types returned by the remote HTTP server.
        private static final int OK = 200;
        private static final int UNAUTHORIZED = 401;

        private String scope;
        private String email;
        private int requestCode;
        private Contact contact;

        profileAsyncTask(Activity activity, String email, String scope,
                         int requestCode) {
            this.scope = GoogleSettings.SCOPE;
            this.email = email;
            this.requestCode = requestCode;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                fetchNameFromProfileServer();
            } catch (IOException ex) {
                onError(mActivity.getString(R.string.following_error_occured_please_try_again_)
                        + ex.getMessage(), ex);
            } catch (JSONException e) {
                onError("Bad Response: " + e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            if (contact != null) {
                profileRequestCallback.onComplete(contact);
            }
            super.onPostExecute(result);
        }

        protected void onError(String msg, Exception e) {
            if (e != null) {
                saveLoggedInAccount(null);
                profileRequestCallback.onException(msg, e);
            }
        }

        /**
         * Get a authentication token if one is not available. If the error is
         * not recoverable then it displays the error message on parent activity
         * right away.
         */
        protected String fetchToken() throws IOException {
            try {
                return GoogleAuthUtil.getToken(mActivity, email, scope);
            } catch (GooglePlayServicesAvailabilityException playEx) {
                // GooglePlayServices.apk is either old, disabled, or not
                // present.
                onError(mActivity.getString(R.string.google_play_service_error_)
                        + playEx.getConnectionStatusCode(), null);

            } catch (UserRecoverableAuthException userRecoverableException) {
                // Unable to authenticate, but the user can fix this.
                // Forward the user to the appropriate activity.
                mActivity.startActivityForResult(
                        userRecoverableException.getIntent(), requestCode);
            } catch (GoogleAuthException fatalException) {
                onError(mActivity.getString(R.string.unrecoverable_error_)
                        + fatalException.getMessage(), fatalException);
            }
            return null;
        }

        /**
         * Contacts the user info server to get the profile of the user and
         * extracts the first name of the user from the profile. In order to
         * authenticate with the user info server the method first fetches an
         * access token from Google Play services.
         *
         * @throws IOException   if communication with user info server failed.
         * @throws JSONException if the response from the server could not be parsed.
         */
        private void fetchNameFromProfileServer() throws IOException,
                JSONException {
            String token = fetchToken();
            Log.e(token, "token : " + token);
            if (token == null) {
                // error has already been handled in fetchToken()
                return;
            }
            URL url = new URL(
                    "https://www.googleapis.com/oauth2/v1/userinfo?access_token="
                            + token
            );
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode == OK) {
                InputStream is = connection.getInputStream();
                String response = readResponse(is);
                contact = ContactsMapper.createProfile(response);
                is.close();
                return;
            } else if (responseCode == UNAUTHORIZED) {
                GoogleAuthUtil.invalidateToken(mActivity, token);
                onError(mActivity.getString(R.string.server_auth_error_please_try_again_)
                        + readResponse(connection.getErrorStream()), null);
                return;
            } else {
                onError(mActivity.getString(R.string.server_returned_the_following_error_code_)
                        + responseCode, null);
                return;
            }
        }

        /**
         * Reads the response from the input stream and returns it as a string.
         */
        private String readResponse(InputStream is) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] data = new byte[2048];
            int len = 0;
            while ((len = is.read(data, 0, data.length)) >= 0) {
                bos.write(data, 0, len);
            }
            return new String(bos.toByteArray(), "UTF-8");
        }
    }

    @Override
    public void getFriends(FriendsRequestCallBack friendsRequestCallBack) {
        // Later.
    }

    @Override
    public void getPosts(PostsRequestCallback postsRequestCallBack,
                         String userId) {
    }

    @Override
    public void getProfile(String userId,
                           ProfileRequestCallback profileRequestCallback) {

    }

}