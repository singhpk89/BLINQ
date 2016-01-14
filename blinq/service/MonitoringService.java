package com.blinq.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.blinq.provider.FeedProvider;
import com.blinq.receivers.ContactsMonitor;
import com.blinq.utils.Log;
import com.nu.art.software.core.utils.SmarterHandler;

public class MonitoringService extends Service {

    private ContactsMonitor contactsMonitor;

    private static final String TAG = MonitoringService.class.getSimpleName();

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    public void onCreate() {
        super.onCreate();
        startMonitoring();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand OnStart");
        stopMonitoring();
        startMonitoring();

        // Start sticky to be restarted when the service closed by system.
        return Service.START_STICKY;
    }

    /**
     * Register an observers that get callbacks when data identified by a given content URI changes.
     */
    void startMonitoring() {

        contactsMonitor = new ContactsMonitor(this, getApplicationContext());

        contactsMonitor.startMonitoring();

    }

    private void recoverMissedLogs() {

        SmarterHandler handler = new SmarterHandler(TAG);
        handler.removeAndPost(new Runnable() {
            @Override
            public void run() {
                try {
                    MonitoringService.this.getContentResolver().bulkInsert(
                            FeedProvider.FEED_REFRESH_HISTORY_URI, null);
                } catch (Exception e) {
                }
            }
        });
    }

    /**
     * Unregister a change observers.
     */
    void stopMonitoring() {

        try {
            contactsMonitor.stopMonitoring();
        } catch (Exception e) {
            Log.d(TAG, "stopMonitoring exception");
        }

    }
}
