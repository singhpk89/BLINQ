package com.blinq.server;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;

import com.blinq.PreferencesManager;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.models.server.ContactProfile;
import com.blinq.provider.FeedProvider;
import com.blinq.provider.HeadboxDatabaseHelper;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.FileUtils;
import com.blinq.utils.HeadboxPhoneUtils;
import com.blinq.utils.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Musab on 11/5/2014.
 */
public class BlinqRestClientUsage {

    public interface ServerRequestCallbacks{
        void getServerContactCompleted(ContactProfile contactProfile, Map<String,String> paramsMap, long feedId);
        void getServerContactFailed(int statusCode, JSONObject errorResponse, Map<String, String> paramsMap);
    }


    public final static String TAG = "BLINQ_REST_CLIENT_USAGE";
    public final static String PERSON_RELATIVE_URL = "person";
    public final static String DBS_RELATIVE_URL = "dbs";

    private ServerRequestCallbacks serverRequestCallbacks;

    private Context context;
    private PreferencesManager preferencesManager;

    public BlinqRestClientUsage(Context context, ServerRequestCallbacks serverRequestCallbacks) {
        this.serverRequestCallbacks = serverRequestCallbacks;
        this.context = context;
        preferencesManager = new PreferencesManager(context);
    }

    /*
        This method performs a get request and parse the result to an object of ContactProfile.
         Input: Hashmap (params) containing available info about notification client.
         Output: ClientProfile object
     */
    public void getContactInformation(final Map<Platform , List<MemberContact>> map, final long feedId) {

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final Map<String, String> paramsMap = getParamsForServer(map);
        RequestParams params = new RequestParams(paramsMap);

        StaticHTTPClient.get(PERSON_RELATIVE_URL, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                ObjectMapper mapper = new ObjectMapper();
                String jsonString = response.toString();
                try {
                    ContactProfile contactProfile = mapper.readValue(jsonString,ContactProfile.class);
                    Log.d(TAG,"Contact Profile parsing succeeded.");
                    if (serverRequestCallbacks != null){
                        serverRequestCallbacks.getServerContactCompleted(contactProfile, paramsMap, feedId);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                // When 202 TODO also for 404
                onFail(statusCode, errorResponse);
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                // When bad request
                onFail(statusCode, null);
                super.onFailure(statusCode, headers, responseString, throwable);
            }

            private void onFail(int statusCode, JSONObject errorResponse) {
                if (serverRequestCallbacks != null){
                    serverRequestCallbacks.getServerContactFailed(statusCode, errorResponse, paramsMap);
                }
                //Fix a bug where no name returned from server so we use the contact name as name
                for(Platform p : map.keySet()) {
                    String name = map.get(p).get(0).getContactName();
                    preferencesManager.setLastPersonName(name);
                    break;
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                super.onSuccess(statusCode, headers, responseString);
            }
        });
    }

    private Map getParamsForServer(Map<Platform, List<MemberContact>> contacts) {
        Map<String, String> queryMap = new HashMap<String, String>();

        if (contacts.get(Platform.EMAIL) != null) {
            MemberContact contact = contacts.get(Platform.EMAIL).get(0);
            String identifier = contact.getNormalizedIdentifier();
            queryMap.put(Constants.EMAIL_PARAM, identifier);
        }
        if (contacts.get(Platform.FACEBOOK) != null) {
            MemberContact contact = contacts.get(Platform.FACEBOOK).get(0);
            String identifier = contact.getNormalizedIdentifier();
            String[] parts = identifier.split("@");
            identifier = parts[0];
            if (isLong(identifier))
                queryMap.put(Constants.FACEBOOK_ID_PARAM, identifier);
            else
                queryMap.put(Constants.FACEBOOK_PARAM, identifier);
        }
        if (contacts.get(Platform.TWITTER) != null) {
            MemberContact contact = contacts.get(Platform.TWITTER).get(0);
            String identifier = contact.getNormalizedIdentifier();
            String[] parts = identifier.split("@");
            identifier = parts[0];
            queryMap.put(Constants.TWITTER_PARAM, identifier);
        }
        if (contacts.get(Platform.CALL) != null) {
            String identifier = getPhoneNumberParam(contacts.get(Platform.CALL));
            queryMap.put(Constants.MOBILE_NUM_PARAM, identifier);
        }
        if (contacts.get(Platform.WHATSAPP) != null) {
            String identifier = getPhoneNumberParam(contacts.get(Platform.WHATSAPP));
            queryMap.put(Constants.MOBILE_NUM_PARAM, identifier);
        }
        if (contacts.get(Platform.HANGOUTS) != null) {
            String identifier = getPhoneNumberParam(contacts.get(Platform.HANGOUTS));
            queryMap.put(Constants.MOBILE_NUM_PARAM, identifier);
        }
        if (contacts.get(Platform.SMS) != null) {
            String identifier = getPhoneNumberParam(contacts.get(Platform.SMS));
            queryMap.put(Constants.MOBILE_NUM_PARAM, identifier);
        }
        Log.d(TAG, "request: " + queryMap.toString());
        return queryMap;
    }

    /**
     * In server we store phone numbers with the country prefix.
     * We want to be sure that we send the contacts phone number to the server with prefix
     * If the number doesnt start with the current country prefix - we add it before sending
     *
     * Source:
     * http://stackoverflow.com/questions/5402253/getting-telephone-country-code-with-android
     */
    private String getPhoneNumberParam(List<MemberContact> contactAsList) {
        MemberContact contact = contactAsList.get(0);
        String phoneNumber = contact.getNormalizedIdentifier();
        String myCountry = preferencesManager.getNetworkCountyISO();
        String prefix = HeadboxPhoneUtils.getInstance(context).getCountryPrefix(myCountry);
        if (!phoneNumber.startsWith(prefix)) {
            phoneNumber = prefix + phoneNumber;
        }
        return phoneNumber;

    }

    private static boolean isLong(String s) {
        try {
            Long.parseLong(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    class DatabaseSender extends AsyncTask<Void, Void, String> {

        private final String TAG = DatabaseSender.class.getSimpleName();

        final String source;
        final String destination;
        final Activity activity;

        public DatabaseSender(String source, String destination, Activity activity) {
            this.source = source;
            this.destination = destination;
            this.activity = activity;
        }

        @Override
        protected String doInBackground(Void... args) {

            Log.d(TAG, "Starts copying contacts DB");

            //prepareDBToSend();
            String zipFile = destination.replace(".db", ".zip");
            FileUtils.zipFiles(new String[]{destination}, zipFile);

            Log.d(TAG, "Done copying contacts DB");
            return zipFile;
        }

        @Override
        protected void onPostExecute(String zipFile) {
            RequestParams params = null;
            int count = getContactsCount(activity);
            try {
                params = new RequestParams();
                params.put(Constants.DB_FILE_PARAM, new File(zipFile));
                params.put(Constants.DB_CONTACTS_COUNT, count);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Failed to upload contacts: " + e.getMessage());
                return;
            }

            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            Log.d(TAG, "Uploading contacts DB");

            StaticHTTPClient.post(DBS_RELATIVE_URL, params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    // TODO
                    Log.d(TAG, "Uploaded contacts to server");
                    super.onSuccess(statusCode, headers, response);
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    Log.d(TAG, "Uploaded contacts to server");
                    super.onSuccess(statusCode, headers, response);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    // When 202 TODO also for 404
                    Log.e(TAG, throwable.getMessage());
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    Log.e(TAG, throwable.getMessage());
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.e(TAG, "Failed to upload contacts: " + throwable.getMessage());
                    super.onFailure(statusCode, headers, responseString, throwable);
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    Log.d(TAG, "Uploaded contacts to server");
                    super.onSuccess(statusCode, headers, responseString);
                }
            });
        }

    }

    public void sendContactsDatabase(final Context context) {

        if (!contactsSyncEnded()) {
            return;
        }
        if (AppUtils.isDebug(context)) {
            return;
        }

        String facebookId = FacebookAuthenticator.getInstance(context).getFacebookId();
        final String source = context.getDatabasePath(
                HeadboxDatabaseHelper.DATABASE_NAME).getAbsolutePath();

        final String destination = String.format("%s/%s.db",
                Environment.getExternalStorageDirectory().getAbsolutePath(), facebookId);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Starts copying contacts DB");

                prepareDBToSend(source, destination, context);
                String zipFile = destination.replace(".db", ".zip");
                FileUtils.zipFiles(new String[]{destination}, zipFile);

                Log.d(TAG, "Done copying contacts DB");

                RequestParams params = null;
                int count = getContactsCount(context);
                try {
                    params = new RequestParams();
                    params.put(Constants.DB_FILE_PARAM, new File(zipFile));
                    params.put(Constants.DB_CONTACTS_COUNT, count);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Failed to upload contacts: " + e.getMessage());
                    return;
                }
                uploadContactsToServer(params);
            }
        }).start();
    }

    private boolean contactsSyncEnded() {
        int syncSkipped = Constants.SYNC_SKIPPED;
        int syncEnded = Constants.SYNC_ENDED;

        int facebookContactsSyncStatus = preferencesManager.getFacebookContactsSyncSatus();
        int twitterContactsSyncStatus = preferencesManager.getTwitterContactsSyncStatus();
        int instagramContactsSyncStatus = preferencesManager.getInstagramContactsSyncStatus();

        Log.d(TAG, "facebook sync status " + facebookContactsSyncStatus);
        Log.d(TAG, "twitter sync status " + twitterContactsSyncStatus);
        Log.d(TAG, "instagram sync status " + instagramContactsSyncStatus);

        return preferencesManager.getContactsSyncApprovedByUser()
                && preferencesManager.getFacebookContactsSyncSatus() == syncEnded
                && (twitterContactsSyncStatus == syncEnded || twitterContactsSyncStatus == syncSkipped)
                && (instagramContactsSyncStatus == syncEnded || instagramContactsSyncStatus == syncSkipped);
    }

    private void prepareDBToSend(String source, String destination, Context context) {
        FileUtils.copyFile(source, destination, context);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(destination, null, SQLiteDatabase.OPEN_READWRITE);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_FEEDS, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_DELETED_MESSAGES, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_FACEBOOK_CONTACTS, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_FEED_PLATFORMS, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_GOOGLE_PLUS_CONTACTS, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_GOOGLE_CONTACTS, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS_LOOKUP, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_MERGE_LINKS, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_MESSAGES, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_PHONE_CONTACTS, null);
        db.rawQuery("DROP TABLE " + HeadboxDatabaseHelper.TABLE_PLATFORMS_CONTACTS, null);
    }

    private int getContactsCount(Context context) {
        int result = -1;
        Cursor c = context.getContentResolver().query(FeedProvider.CONTACTS_COUNT, null, null, null, null);
        if (c != null) {
            try {
                c.moveToFirst();
                result = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return result;
    }

    private void uploadContactsToServer(RequestParams params) {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Log.d(TAG, "Uploading contacts DB");

        StaticHTTPClient.post(DBS_RELATIVE_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // TODO
                Log.d(TAG, "Uploaded contacts to server");
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d(TAG, "Uploaded contacts to server");
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                // When 202 TODO also for 404
                Log.e(TAG, throwable.getMessage());
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e(TAG, throwable.getMessage());
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e(TAG, "Failed to upload contacts: " + throwable.getMessage());
                super.onFailure(statusCode, headers, responseString, throwable);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Log.d(TAG, "Uploaded contacts to server");
                super.onSuccess(statusCode, headers, responseString);
            }
        });
    }

}


