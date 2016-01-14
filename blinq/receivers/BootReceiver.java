package com.blinq.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blinq.utils.AppUtils;

/**
 * Allows the application's main service to start on boot.
 *
 * @author Johan Hansson.
 */

public class BootReceiver extends BroadcastReceiver {

    private final static String TAG = BootReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {

        // Checks for Boot completed Action.
        AppUtils.startMainService(context);
    }

}
