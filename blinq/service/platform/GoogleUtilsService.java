package com.blinq.service.platform;

import android.content.Intent;

import com.blinq.utils.Log;

/**
 * Responsible to handle different updates for Google+.
 *
 * @author Johan Hansson.
 */
public class GoogleUtilsService extends PlatformServiceBase {

    private static final String TAG = GoogleUtilsService.class.getSimpleName();

    public GoogleUtilsService() {
        super(TAG);
        Log.d(TAG, "Starting GoogleUtilsService..");
    }

    @Override
    public void onCreate() {

        super.onCreate();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
    }

    /**
     * Fetch a certain Google contact.
     */
    @Override
    protected void syncContact(Intent intent) {

    }

    @Override
    protected void syncContacts(Intent intent) {
        //TODO: To be implemented later.
    }

    @Override
    protected void fullSyncContacts() {
        //TODO: To be implemented later.
    }
}