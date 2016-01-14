package com.blinq.service.platform;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Handler;

import com.blinq.HeadboxAccountsManager;
import com.blinq.PreferencesManager;
import com.blinq.models.Contact;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.Platform;
import com.blinq.module.message.utils.MessageConverter;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.impl.facebook.Actions.FriendsAction;
import com.blinq.authentication.impl.facebook.Actions.InboxAction;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.provider.FeedProvider;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.nu.art.software.core.utils.SmarterHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Responsible to handle different updates for facebook.
 * <ul>
 * <li>Update facebook Contacts/friends.</li>
 * <li>Update FACEBOOK Inbox.</li>
 * </ul>
 *
 * @author Johan Hansson.
 */
public class FacebookUtilsService extends PlatformServiceBase {

    private static final String TAG = FacebookUtilsService.class.getSimpleName();

    private static final long UPDATING_WAITING_TIME = 4000;

    private static Timer timer = new Timer();

    public FacebookUtilsService() {
        super(TAG);
        Log.d(TAG, "Starting FacebookUtilsService..");
    }

    @Override
    public void onCreate() {

        super.onCreate();
        authenticator = FacebookAuthenticator.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        super.onHandleIntent(intent);
        if (ACTION_RELOAD_FACEBOOK_INBOX.equals(action)) {
            updateFacebookInbox();
        }
    }

    /**
     * Fetch a certain contact from facebook API.
     */
    @Override
    protected void syncContact(Intent intent) {

        final String contactId = intent.getStringExtra(EXTRA_CONTACT_ID);

        Handler mHandler = new Handler(getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                authenticator.getProfile(contactId,
                        new ProfileRequestCallback(Platform.FACEBOOK));

            }
        });
    }

    /**
     * Fetch list of contacts.
     *
     * @param intent - updated contacts sent by intent.
     */
    @Override
    protected void syncContacts(Intent intent) {

        // Extract the ids of the contacts to be updated.
        final ArrayList<String> contacts = (ArrayList<String>) intent
                .getSerializableExtra(EXTRA_CONTACT_ID);

        Handler mHandler = new Handler(getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (String contact : contacts) {
                    // Getting facebook contact/s.
                    String facebookId = contact.substring(0,
                            contact.indexOf(StringUtils.FACEBOOK_SUFFIX));
                    authenticator.getProfile(facebookId,
                            new ProfileRequestCallback(Platform.FACEBOOK));
                }
            }
        });

    }

    @Override
    protected void fullSyncContacts() {

        SmarterHandler handler = new SmarterHandler(TAG);
        handler.removeAndPost(new Runnable() {
            @Override
            public void run() {

                FriendsAction action = new FriendsAction((FacebookAuthenticator) authenticator);
                action.setRequestType(Action.RequestType.SYNC);
                action.execute();

                if (action.getResult() == null)
                    return;

                provider.buildContacts(action.getResult(), Platform.FACEBOOK);
                FeedProviderImpl.getInstance(getApplicationContext()).updateTopFriendsNotifications();

            }
        });

    }

    private void updateFacebookInbox() {

        final String facebookId = HeadboxAccountsManager.getInstance()
                .getAccountsByType(HeadboxAccountsManager.AccountType.FACEBOOK).name;

        if (facebookId == null)
            return;

        timer.cancel();
        this.timer = new Timer();

        TimerTask action = new TimerTask() {

            public void run() {

                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        loadFacebookInbox();
                    }
                });
                thread.start();
            }
        };

        this.timer.schedule(action, UPDATING_WAITING_TIME); // Start the task

    }


    private void loadFacebookInbox() {

        InboxAction action = new InboxAction(FacebookAuthenticator.getInstance(this));
        action.setLimit(PreferencesManager.FACEBOOK_INBOX_FIRST_LOAD_LIMIT);
        action.setRequestType(Action.RequestType.SYNC);
        action.setActionListener(new OnActionListener<Map<Contact, List<HeadboxMessage>>>() {
            @Override
            public void onComplete(Map<Contact, List<HeadboxMessage>> response) {

                HashMap<String, ContentValues[]> values = new HashMap<String, ContentValues[]>();

                for (Contact contact : response.keySet()) {
                    List<HeadboxMessage> messages = response.get(contact);
                    ContentValues[] content = MessageConverter.convertToContentValues(messages);
                    String id = contact.getContactId().concat(StringUtils.FACEBOOK_SUFFIX);
                    values.put(id, content);
                }

                FeedProvider.setMessages(values);
                getApplicationContext().getContentResolver().bulkInsert(
                        FeedProvider.FACEBOOK_URI, null);
            }

            @Override
            public void onFail(String reason) {
                Log.d(TAG, "onFail , failed while reloading facebook inbox = " + reason);
                super.onFail(reason);
            }

            @Override
            public void onException(Throwable throwable) {
                Log.d(TAG, "onException , exception while reloading facebook inbox = " + throwable.getMessage());
                super.onException(throwable);
            }
        });

        action.execute();
    }

    /**
     * Respond to {@see Authenticator.getProfile()} action to update certain
     * contact.
     *
     * @author Johan Hansson.
     */
    private class ProfileRequestCallback implements
            Authenticator.ProfileRequestCallback {

        /**
         * Authentication platform.
         */
        private Platform platform;

        public ProfileRequestCallback(Platform platform) {
            this.platform = platform;
        }

        @Override
        public void onException(String msg, Exception e) {
        }

        @Override
        public void onFail() {
        }

        @Override
        public void onGettingProfile() {
        }

        @Override
        public void onComplete(Contact contact) {

            provider.insertContact(contact);
        }

    }

}