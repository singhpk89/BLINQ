package com.blinq;

import android.app.Application;

import com.blinq.models.Account;
import com.nu.art.software.TacB0sS.samples.genericPreferences.AppSpecificPreferenceStorage;
import com.nu.art.software.TacB0sS.samples.genericPreferences.PreferencesStorage;

/**
 * This class provides access to a centralized registry of the user's
 * accounts.
 * <p/>
 * <ol>
 * <li>Store Facebook profile's details and access token.
 * <li>Store Google Plus profile's details and access token.
 * <li>Store Instagram,Twitter profile's details</li>
 * </ol>
 *
 * @author Johan Hansson.
 */

public class HeadboxAccountsManager {

    private final static String TAG = HeadboxAccountsManager.class.getSimpleName();
    private static AppSpecificPreferenceStorage preferences = null;
    private static final String PREF_NAME = "headbox_accounts_preferences";

    private static HeadboxAccountsManager instance = null;

    private static PreferencesStorage.SharedPreferencesDetails sharedPreferencesDetails = new PreferencesStorage.SharedPreferencesDetails() {
        @Override
        public String getPreferencesName() {
            return PREF_NAME;
        }

        @Override
        public int getMode() {
            return Application.MODE_PRIVATE;
        }
    };

    private HeadboxAccountsManager() {

        preferences = BlinqApplication.preferences;

    }

    /**
     * Return a singleton for the <code>AccountsManager</code>.
     *
     * @return AccountsManager instance.
     */
    public static HeadboxAccountsManager getInstance() {
        if (instance == null) {
            instance = new HeadboxAccountsManager();
        }
        return instance;

    }

    /*
  * Add an account of a specified type.
  */
    public void addAccount(AccountType type, String userName, String accessToken) {
        addAccount(userName, accessToken, 0, type);
    }


    public void addAccount(String userName, String accessToken,
                           float timeZone, AccountType type) {

        preferences.putValue(getPreferenceKey(Accounts.USER_NAME, type), userName);
        preferences.putValue(getPreferenceKey(Accounts.TIME_ZONE, type), timeZone);
        preferences.putValue(getPreferenceKey(Accounts.ACCESS_TOKEN, type), accessToken);

    }

    public void saveAccessToken(AccountType type, String accessToken) {

        preferences.putValue(getPreferenceKey(Accounts.ACCESS_TOKEN, type), accessToken);

    }

    /**
     * Gets an auth token of the specified type for a particular account.
     */
    public String getAuthToken(AccountType type) {

        if (type == null) throw new IllegalArgumentException("account type is null");
        return String.valueOf(preferences.getValue(getPreferenceKey(Accounts.ACCESS_TOKEN, type), null));
    }

    /**
     * Gets a time zone of the specified type for a authenticated user.
     * <p> Mainly for {@link HeadboxAccountsManager.AccountType.FACEBOOK}.
     */
    public float getUserTimeZone(AccountType type) {

        if (type == null) throw new IllegalArgumentException("account type is null");
        return (Float) preferences.getValue(getPreferenceKey(Accounts.TIME_ZONE, type), (float) 0);
    }

    private <Type> PreferencesStorage.PreferenceKey<Type> getPreferenceKey(String preferenceKey, AccountType type) {

        String propertyKey = String.format(preferenceKey, type.getName());
        PreferencesStorage.PreferenceKey<Type> key = new PreferencesStorage.PreferenceKey<Type>(propertyKey, sharedPreferencesDetails);
        return key;
    }


    public Account getAccountsByType(AccountType type) {

        String accountName = String.valueOf(preferences.getValue(getPreferenceKey(Accounts.USER_NAME, type), null));
        String displayName = String.valueOf(preferences.getValue(getPreferenceKey(Accounts.DISPLAY_NAME, type), null));
        String accessToken = (String) preferences.getValue(getPreferenceKey(Accounts.ACCESS_TOKEN, type), null);

        return new Account(accountName, type, accessToken);
    }

    /**
     * Holds the names of the preferences to modify.
     */
    public final class Accounts {

        /**
         * Bundle key used for the {@link String} account type in results
         * from methods which return information about a particular account.
         */
        public static final String ACCOUNT_TYPE = "%s_accountType";
        public static final String USER_NAME = "%s_user_name";
        public static final String DISPLAY_NAME = "%s_display_name";
        public static final String TIME_ZONE = "%s_time_zone";
        /**
         * Bundle key used for the auth token value in results
         * from {@link #getAuthToken} and friends.
         */
        public static final String ACCESS_TOKEN = "%s_access_token";

    }

    /**
     * Enumeration holds the supported authentication's accounts.
     */
    public enum AccountType {

        GOOGLE("google"), FACEBOOK("facebook"), TWITTER("twitter"), INSTAGRAM("instagram");
        private String name;

        private AccountType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
