package com.blinq.receivers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;

import com.blinq.PreferencesManager;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.service.MonitoringService;
import com.blinq.utils.Log;

/**
 * Keep tracking of the changes happen on android local contacts.
 */
public class ContactsMonitor {

    private static final String TAG = ContactsMonitor.class.getSimpleName();
    private MonitoringService service;
    private PreferencesManager preferences;
    private ContentResolver contentResolver = null;
    private Handler contactshandler = null;
    private ContentObserver contactsMonitor = null;
    public boolean monitorStatus = false;

    public ContactsMonitor(final MonitoringService service,
                           final Context mainContext) {
        this.service = service;
        contentResolver = service.getContentResolver();
        contactshandler = new ContactsHandler();
        contactsMonitor = new ContactsObserver(contactshandler);
        preferences = new PreferencesManager(mainContext);
    }

    public void startMonitoring() {

        Log.i(TAG, "Start contacts monitoring.");

        try {
            monitorStatus = false;
            if (!monitorStatus) {
                contentResolver.registerContentObserver(
                        ContactsContract.Contacts.CONTENT_URI, false,
                        contactsMonitor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Start contacts monitoring exception :" + e.getMessage());
        }
    }

    public void stopMonitoring() {
        try {
            monitorStatus = false;
            if (!monitorStatus) {
                contentResolver.unregisterContentObserver(contactsMonitor);
                Log.d(TAG, "Stop ContactsMonitor ######");
            }
        } catch (Exception e) {
            Log.e(TAG, "Stop contacts monitoring exception:" + e.getMessage());
        }
    }

    class ContactsHandler extends Handler {
        public void handleMessage(final Message msg) {
        }
    }

    class ContactsObserver extends ContentObserver {

        public ContactsObserver(final Handler handler) {
            super(handler);
        }

        public void onChange(final boolean bSelfChange) {
            super.onChange(bSelfChange);

            Thread thread = new Thread() {
                public void run() {
                    try {

                        monitorStatus = true;
                        synchronized (this) {
                            onContactsChange();
                        }

                    } catch (Exception e) {
                        Log.e(TAG,
                                "onchange contact monitoring exception:"
                                        + e.getMessage()
                        );
                    }
                }
            };
            thread.start();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
        }

        protected void onContactsChange() {

            long currentTime = System.currentTimeMillis();

            if (preferences.canLoadContacts(currentTime)) {

                preferences.setContactsLastLoadingTime(currentTime);
                Log.i(TAG, "Start loading contacts..");

                FeedProviderImpl feedProvider = (FeedProviderImpl) FeedProviderImpl
                        .getInstance(service);
                feedProvider.updateLocalContacts();
            }
        }

    }
}