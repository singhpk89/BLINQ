package com.blinq.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Holds the application network,google play service and other utilities.
 *
 * @author Johan Hansson
 */
public class NetworkUtils {

    public static final String TAG = NetworkUtils.class.getSimpleName();
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;

    private NetworkUtils() {
    }

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static String getConnectivityStatusString(Context context) {
        int connection = getConnectivityStatus(context);
        String status = null;
        if (connection == TYPE_WIFI) {
            status = "Wifi enabled";
        } else if (connection == TYPE_MOBILE) {
            status = "Mobile data enabled";
        } else if (connection == TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet";
        }
        return status;
    }

    /**
     * Check for the presence of any Internet connection.
     *
     * @param context
     * @return boolean indicate whether we have connection or not.
     */
    public static boolean isConnectedToInternet(Context context) {

        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
        }
        return false;
    }

    /**
     * Checks if Google Play Services are installed and if not it initialises
     */
    public static int checkGooglePlayAvailability(Context context) {
        final int connectionStatusCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(context);

        Log.d(TAG, String.valueOf(connectionStatusCode));
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            return connectionStatusCode;
        }
        return 0;
    }

}
