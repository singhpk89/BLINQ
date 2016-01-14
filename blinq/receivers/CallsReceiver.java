package com.blinq.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telephony.TelephonyManager;

import com.blinq.PreferencesManager;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.HeadboxAnalyst;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.Platform;
import com.blinq.provider.CallsManager;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.utils.Log;

import java.util.List;

/**
 * Calls Receiver detect and process an incoming/Outgoing call.
 *
 * @author Johan Hansson.
 */

public class CallsReceiver extends BroadcastReceiver {

    /**
     * The delay (in milliseconds) until the call handler will be executed.
     */
    public static final String TAG = CallsReceiver.class.getSimpleName();

    private static final long HANDLER_DELAY = 2500;
    private static final int CALLS_TO_LOAD = 3;

    private Handler handler = new Handler();
    private CallsManager callsProvider;
    private PreferencesManager preferencesManager;

    public CallsReceiver() {

    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        if (intent == null || intent.getExtras() == null) {
            return;
        }
        String phoneState = intent.getExtras().getString(
                TelephonyManager.EXTRA_STATE);

        // An idle state means the call has just been ended. We should proceed
        // to extract calling information from call log.
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(phoneState)) {

            Log.i(TAG, "a new call detected.");

            final Context mContext = context;

            // To ensure the data is fully updated & stable to be correctly
            // read, we might need to defer the processing a little bit...
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    callsProvider = new CallsManager(mContext);
                    List<HeadboxMessage> calls = callsProvider.getLastCalls(CALLS_TO_LOAD);

                    if (calls == null || calls.size() == 0) {
                        Log.d(TAG, "Unable to get last call..");
                        return;
                    }

                    Provider provider = FeedProviderImpl.getInstance(mContext);
                    preferencesManager = new PreferencesManager(mContext);

                    String feedHistoryMode = preferencesManager.getProperty(PreferencesManager.AB_FEED_HISTORY, AnalyticsConstants.AB_FEED_HISTORY_FULL);

                    if (feedHistoryMode.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_FULL)) {
                        for (HeadboxMessage call : calls) {
                            provider.insertMessage(call);
                        }
                    } else {
                        provider.insertMessage(calls.get(0));
                    }

                    sendCallAnalytics(mContext, calls.get(0));

                }
            }, HANDLER_DELAY);
        }
    }

    /**
     * Send incoming/outgoing events for Call platform
     */
    private void sendCallAnalytics(Context context, HeadboxMessage call) {

        HeadboxAnalyst headboxAnalyst = new HeadboxAnalyst(context);
        PreferencesManager preferencesManager = new PreferencesManager(context);

        switch (call.getType()) {

            case INCOMING:
                headboxAnalyst.sendIncomingMessageEvent(Platform.CALL, false);
                break;
            case OUTGOING:
                boolean sentFromApp = preferencesManager.getProperty(PreferencesManager.CALLED_FROM_APP, false);
                headboxAnalyst.sendOutgoingMessageEvent(Platform.CALL, sentFromApp);
                preferencesManager.setProperty(PreferencesManager.CALLED_FROM_APP, false);
                break;
            default:
                break;
        }
    }

}
