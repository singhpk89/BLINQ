package com.blinq.server;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Patterns;

import com.blinq.BlinqApplication;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.authentication.impl.Instagram.InstagramAuthenticator;
import com.blinq.authentication.impl.Twitter.TwitterAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Create profile on the server.
 * Gather the user IDs on the platforms, phone number and email, and send the data to the server.
 * Called after the Connect more activity, and after connecting platforms in the social window
 *
 * Created by Roi on 12/26/14.
 */
public class UserProfileCreator {

    public static final String TAG = UserProfileCreator.class.getSimpleName();

    public final static String PROFILES_RELATIVE_URL = "profiles";

    public static final String EMAIL = "email";
    public static final String PHONE_NUMBER = "phone";
    public static final String FACEBOOK_ID = "facebookId";
    public static final String NAME = "name";
    public static final String INSTAGRAM = "instagram";
    public static final String TWITTER = "twitter";

    private final Set<String> phoneNumbers = new HashSet<String>();
    private final Set<String> emails = new HashSet<String>();
    private String facebookId;
    private String name;
    private String instagram;
    private String twitter ;

    private final Context context;

    private FacebookAuthenticator facebookAuthenticator;
    private TwitterAuthenticator twitterAuthenticator;
    private InstagramAuthenticator instagramAuthenticator;

    private static UserProfileCreator instance;

    public static UserProfileCreator getInstance(Context context) {
        if (instance == null) {
            instance = new UserProfileCreator(context);
        }
        return instance;
    }

    private UserProfileCreator(Context context) {
        this.context = context;
        initializeAuthenticators();
        initializeEmailsAndPhoneNumbers();
        initializeFacebookIdAndName();
    }

    private void initializeFacebookIdAndName() {
        facebookId = facebookAuthenticator.getFacebookId();
        name = facebookAuthenticator.getFacebookName();
        if (name != null) {
            name = name.replace(" ", "%20");
        }
    }

    private void initializeAuthenticators() {
        facebookAuthenticator = FacebookAuthenticator.getInstance(context);
        instagramAuthenticator = InstagramAuthenticator.getInstance(context);
        twitterAuthenticator = TwitterAuthenticator.getInstance(context);
    }

    private void initializeEmailsAndPhoneNumbers() {
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            String accountName = account.name;
            if (accountName.isEmpty()) {
                continue;
            }
            if (account.type.equals("com.google") && Patterns.EMAIL_ADDRESS.matcher(accountName).matches()) {
                emails.add(accountName);
            }
            else if (Patterns.PHONE.matcher(accountName).matches()) {
                if (accountName.startsWith("+")) {
                    accountName = accountName.substring(1);
                }
                phoneNumbers.add(accountName);
            }
        }
        String phoneNumber = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            phoneNumbers.add(phoneNumber);
        }
    }

    public void createProfile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (instagramAuthenticator.isConnected()) {
                    instagram = instagramAuthenticator.getInstagramId();
                }
                if (twitterAuthenticator.isConnected()) {
                    twitter = twitterAuthenticator.getTwitterId();
                }
                sendData();
            }
        }).start();
    }

    private void sendData() {
        Map<String, String> params = prepareParamsMap();
        String paramsString = prepareParamsString(params);
        StaticHTTPClient.post(PROFILES_RELATIVE_URL + paramsString.toString(), null, jsonHttpResponseHandler);
    }

    private Map<String, String> prepareParamsMap() {
        Map<String, String> map = new HashMap<String, String>();
        if (!phoneNumbers.isEmpty()) {
            map.put(PHONE_NUMBER, StringUtils.join(phoneNumbers, ","));
        }
        if (!emails.isEmpty()) {
            map.put(EMAIL, StringUtils.join(emails, ","));
        }
        if (facebookId != null) {
            map.put(FACEBOOK_ID, facebookId);
        }
        if (name != null) {
            map.put(NAME, name);
        }
        if (twitter != null) {
            map.put(TWITTER, twitter);
        }
        if (instagram != null) {
            map.put(INSTAGRAM, instagram);
        }
        Log.d(TAG, "Params of the profile: " + map.toString());
        return map;
    }

    private String prepareParamsString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        for (String key : params.keySet()) {
            sb.append(key + "=" + params.get(key));
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private JsonHttpResponseHandler jsonHttpResponseHandler = new JsonHttpResponseHandler() {

        private void sendFailAnalytics() {
            Log.e(TAG, "Failed to create user profile on server");
            BlinqApplication.analyticsManager.sendEvent(AnalyticsConstants.CREATE_USER_PROFILE_EVENT, false, AnalyticsConstants.ERRORS_CATEGORY);
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            Log.d(TAG, "Created use profile on server successfully");
            super.onSuccess(statusCode, headers, response);
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
            Log.d(TAG, "Created use profile on server successfully");
            super.onSuccess(statusCode, headers, response);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
            sendFailAnalytics();
            super.onFailure(statusCode, headers, throwable, errorResponse);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
            sendFailAnalytics();
            super.onFailure(statusCode, headers, throwable, errorResponse);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            sendFailAnalytics();
            super.onFailure(statusCode, headers, responseString, throwable);
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, String responseString) {
            Log.d(TAG, "Created use profile on server successfully");
            super.onSuccess(statusCode, headers, responseString);
        }
    };
}
