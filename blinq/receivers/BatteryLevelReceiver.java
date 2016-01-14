package com.blinq.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.blinq.utils.Log;

/**
 * Monitor Changes in Battery level.
 */
public class BatteryLevelReceiver extends BroadcastReceiver {

    private static final String TAG = BatteryLevelReceiver.class
            .getSimpleName();

    @Override
    public void onReceive(Context context, Intent batteryStatus) {

        // current battery level
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        // Maximum battery level.
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = level / (float) scale;

        Log.i(TAG, "Battery info : level :" + level + " scale :" + scale
                + " battery Percentage :" + batteryPercentage);
        // TODO: Disable any background updates when the battery is critically.

    }

}
