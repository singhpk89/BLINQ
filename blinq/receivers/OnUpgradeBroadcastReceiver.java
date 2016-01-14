package com.blinq.receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blinq.utils.Log;

/**
 * Receive intent when the application is updated or Reinstalled.
 *
 * @author Johan Hansson.
 */
public class OnUpgradeBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = OnUpgradeBroadcastReceiver.class
            .getSimpleName();

    public OnUpgradeBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null)
            return;

        Log.d(TAG, TAG + "Received upgrading action : " + intent.getAction());

        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())
                || Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {

            // Check whether the upgraded application is Headbox or not.
            if (intent.getData().getSchemeSpecificPart()
                    .equals(context.getPackageName())) {

                // Here we need to restart the services and register the
                // observers and scheduling the alarm manager.
                //Disabled temporary.
                //AppUtils.scheduleAlarm(context);

            }
        }
    }

}
