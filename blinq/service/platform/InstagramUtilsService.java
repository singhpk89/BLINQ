package com.blinq.service.platform;

import android.content.Intent;

import com.blinq.models.Platform;
import com.blinq.authentication.impl.Instagram.InstagramAuthenticator;
import com.blinq.utils.Log;

/**
 * Responsible to manage the long-running requests to the Instagram API.
 * Extends the Base Platform Service.
 * <p/>
 * Created by Johan Hansson on 9/24/2014.
 */
public class InstagramUtilsService extends PlatformServiceBase {

    private static final String TAG = InstagramUtilsService.class.getSimpleName();

    public InstagramUtilsService() {
        super(TAG);
        Log.d(TAG, "Starting InstagramUtilsService..");
    }

    public InstagramUtilsService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
    }

    @Override
    protected void syncContact(Intent intent) {

    }

    @Override
    protected void syncContacts(Intent intent) {

    }

    @Override
    protected void fullSyncContacts() {
        InstagramAuthenticator.getInstance(getApplicationContext()).getFriends(
                new FriendsCallBack(Platform.INSTAGRAM, TAG));
    }
}
