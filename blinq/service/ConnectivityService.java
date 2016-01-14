package com.blinq.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.blinq.PreferencesManager;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;

/**
 * Handle connecting & receiving packets listeners.
 *
 * @author Johan Hansson.
 */
public class ConnectivityService extends Service {

    private static final String TAG = ConnectivityService.class.getSimpleName();

    // Binder given to clients
    private final IBinder binder = new ServiceBinder();
    private Provider provider;
    private PreferencesManager preferencesManager;

    @Override
    public IBinder onBind(Intent intent) {

        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        init();

        // Start sticky to be restarted when the service closed by system.
        return Service.START_STICKY;
    }

    /**
     * Initialize required components.
     */
    private void init() {

        provider = FeedProviderImpl.getInstance(getApplicationContext());
        preferencesManager = new PreferencesManager(getApplicationContext());
    }

    public class ServiceBinder extends Binder {

        public ConnectivityService getService() {

            // Return this instance of ConnectivityService so clients can call
            // public methods
            return ConnectivityService.this;
        }
    }

}
