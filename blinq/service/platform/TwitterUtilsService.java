package com.blinq.service.platform;

import android.content.Intent;

import com.blinq.models.Platform;
import com.blinq.authentication.impl.Twitter.TwitterAuthenticator;
import com.blinq.utils.Log;

/**
 * Responsible to manage the long-running requests to the Twitter API.
 * Extends the Base Platform Service.
 * <p/>
 * Created by Johan Hansson on 9/24/2014.
 */
public class TwitterUtilsService extends PlatformServiceBase {

    private static final String TAG = TwitterUtilsService.class.getSimpleName();

    public TwitterUtilsService() {
        super(TAG);
        Log.d(TAG, "Starting TwitterUtilsService..");
    }

    public TwitterUtilsService(String name) {
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


        TwitterAuthenticator.getInstance(getApplicationContext()).getFriends(
                new FriendsCallBack(Platform.TWITTER, TAG));
    }
}
