package com.blinq.authentication.settings;

/**
 * Holds Google settings such as: google Application Id,Scopes and permissions.
 */
public class GoogleSettings {

    public static final int DIALOG_GET_GOOGLE_PLAY_SERVICES = 1;
    public static final int REQUEST_CODE_GOOGLE_PLUS_SIGN_IN = 0;
    public static final int REQUEST_CODE_SIGN_IN = 1;
    public static final int REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES = 2;

    public static final String AUDIENCE = "server:client_id:997120912524-qi55ma8230au870un6homqfl8dqfpenm.apps.googleusercontent.com";
    public static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
    public static final String CirclesScope = "https://www.googleapis.com/auth/plus.circles.read";
    public static final String EXTRA_ACCOUNTNAME = "extra_accountname";
    public static final int REQUEST_ACCOUNT_PICKER = 1;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 2;
    public static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
    public static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
    public static final String GOOGLE_ACCOUNT_NAME_SETTING = "accountName";
    public static final String SHARED_PREF_NAME = "Account";
    public static final String AUTH_TOKEN_TYPE = "mail";
    public static final String ACCOUNT_TYPE = "com.google";
    public static final int REQUEST_CODE_INTERACTIVE_POST = 6;
}
