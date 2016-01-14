package com.blinq.provider;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;

import com.blinq.PreferencesManager;
import com.blinq.models.Contact;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.module.message.utils.MessageConverter;
import com.blinq.provider.HeadboxFeed.Messages;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provide utilities to deal with android calls content provider.
 * <p/>
 * <li>Querying calls history.
 * <li>Querying last incoming/outgoing/missed call.
 * <li>Delete/Update call record.
 * <li>Make Call/Dial.
 * <p/>
 *
 * @author Johan Hansson.
 */
public class CallsManager {

    private static final String TAG = CallsManager.class.getSimpleName();

    public static final int START_DIAL_ACTION_REQUEST_CODE = 6;


    /**
     * A list of which columns to return from call log.
     */
    private static final String[] CALL_COLUMNS = new String[]{Calls._ID,
            Calls.NUMBER, Calls.DATE, Calls.TYPE, Calls.IS_READ, Calls.DURATION};
    /**
     * The URI for call content to retrieve.
     */
    public static final String CALL_URI = Calls.CONTENT_URI.toString();
    public static final String CALL_TYPE_INCOMING_TEXT = "Incoming Call";
    public static final String CALL_TYPE_OUTGOING_TEXT = "Outgoing Call";
    public static final String CALL_TYPE_MISSEDCALL_TEXT = "Missed Call";
    private static final String DEFAULT_SORT_ORDER = CallLog.Calls._ID
            + " DESC ";

    /**
     * Prepended to the phone number when making a call using intent.
     */
    private static final String CALL_INTENT_PREFEX = "tel:";

    private Context context;

    public CallsManager(Context context) {
        this.context = context;
    }

    /**
     * Get the calls stored in mobile database for last period of days
     *
     * @param period - in days
     */
    public static List<ContentValues> getCallsHistory(Context context,
                                                      int period) {

        Log.i(TAG, "Loading Calls History from the phone's database.");

        List<ContentValues> messages;

        String whereClause = Calls.DATE + " > ?";
        String[] whereArgs = new String[]{""
                + (new Date().getTime() - period * Constants.DAY_IN_MILLISECOND)};

        Cursor cursor = context.getContentResolver().query(Uri.parse(CALL_URI),
                CALL_COLUMNS, whereClause, whereArgs, Calls.DATE);

        messages = convertCursor(cursor);
        return messages;

    }

    /**
     * Get last calls from android calls db.
     *
     * @param latestCount - count to be returned.
     */
    public static List<ContentValues> getLatestCalls(Context context,
                                                     int latestCount) {

        Log.i(TAG, "Getting last " + latestCount + " calls.");

        List<ContentValues> messages = new ArrayList<ContentValues>();

        if (latestCount == 0)
            return messages;

        String limit = "LIMIT " + latestCount;
        Cursor cursor = context.getContentResolver().query(Uri.parse(CALL_URI),
                CALL_COLUMNS, null, null, Calls._ID + " DESC " + limit);

        messages = convertCursor(cursor);

        return messages;
    }

    private static List<ContentValues> convertCursor(Cursor cursor) {

        List<ContentValues> messages = new ArrayList<ContentValues>();

        try {

            if (cursor.moveToFirst()) {

                do {
                    HeadboxMessage message = getCallFromCursor(cursor);
                    messages.add(MessageConverter
                            .convertToContentValues(message));
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in call cursor conversion :" + e.getMessage() + "");

        } finally {
            if (cursor != null)
                cursor.close();
        }
        return messages;
    }

    /**
     * Get last call from calls provider.
     */
    public List<HeadboxMessage> getLastCalls(int count) {

        Log.i(TAG, "Loading last " + count + " calls.");

        String limit = " LIMIT " + count;

        Cursor cursor = null;

        List<HeadboxMessage> messages = null;
        try {
            cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, CALL_COLUMNS, null, null,
                    DEFAULT_SORT_ORDER + limit);

            if (cursor.moveToFirst()) {

                messages = new ArrayList<HeadboxMessage>();
                do {
                    HeadboxMessage call = getCallFromCursor(cursor);
                    call.setDate(new Date());
                    messages.add(call);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error in getting last call: " + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return messages;
    }

    /**
     * Maps call cursor to the message object.
     */
    private static HeadboxMessage getCallFromCursor(Cursor cursor) {

        int numberColumn = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int typeColumn = cursor.getColumnIndex(CallLog.Calls.TYPE);
        int dateColumn = cursor.getColumnIndex(CallLog.Calls.DATE);
        int idColumn = cursor.getColumnIndex(CallLog.Calls._ID);
        int readColumn = cursor.getColumnIndex(CallLog.Calls.IS_READ);
        int durationColumn = cursor.getColumnIndex(CallLog.Calls.DURATION);

        String phoneNumber = cursor.getString(numberColumn);
        String handledPhoneNumber = handlePhoneNumber(phoneNumber);

        HeadboxMessage call = new HeadboxMessage();
        Contact contact = new Contact(handledPhoneNumber);
        call.setContact(contact);

        MessageType type = getCallType(cursor.getInt(typeColumn));
        call.setType(type);
        call.setPlatform(Platform.CALL);
        call.setMessageId(cursor.getString(idColumn));
        call.setSourceId(cursor.getString(idColumn));
        call.setBody(getCallMessageBody(cursor.getInt(typeColumn)));
        call.setDate(AppUtils.convertToDate(cursor.getLong(dateColumn)));
        call.setRead(cursor.getInt(readColumn));
        call.setDuration(cursor.getInt(durationColumn));

        if (type == MessageType.INCOMING) {
            call.setRead(Messages.MSG_READ);
        }

        return call;
    }

    /**
     * Handle contact number to deal with unknown/private numbers.
     */
    private static String handlePhoneNumber(String number) {

        String phoneNumber = null;

        if (number == null)
            phoneNumber = StringUtils.UNKNOWN_NUMBER;
        else if (number.equals(StringUtils.UNKNOWN_NUMBER))
            phoneNumber = StringUtils.UNKNOWN_NUMBER;
        else if (number.equals(StringUtils.PRIVATE_NUMBER))
            phoneNumber = StringUtils.PRIVATE_NUMBER;
        else if (number.equals(""))
            phoneNumber = StringUtils.UNKNOWN_NUMBER;
        else {
            phoneNumber = number;
        }

        return phoneNumber;
    }

    /**
     * Get call text according to its type.
     */
    private static String getCallMessageBody(int type) {

        String body = CALL_TYPE_INCOMING_TEXT;
        if (type == Calls.INCOMING_TYPE) {
            return CALL_TYPE_INCOMING_TEXT;
        } else if (type == Calls.OUTGOING_TYPE) {
            return CALL_TYPE_OUTGOING_TEXT;
        } else if (type == Calls.MISSED_TYPE) {
            return CALL_TYPE_MISSEDCALL_TEXT;
        }
        return body;
    }

    /**
     * Map call type to headbox Message type.
     */
    private static MessageType getCallType(int type) {

        MessageType callType = MessageType.INCOMING;
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                callType = MessageType.INCOMING;
                break;

            case CallLog.Calls.OUTGOING_TYPE:
                callType = MessageType.OUTGOING;
                break;

            case CallLog.Calls.MISSED_TYPE:
                callType = MessageType.MISSED;
                break;
        }
        return callType;
    }

    /**
     * Mark list of Calls as read.
     *
     * @param callsIDs - list of calls ids.
     */
    public static void markAsRead(Context context, String[] callsIDs) {

        String selection = Calls._ID + " IN ("
                + StringUtils.convertToString(callsIDs) + " )";

        ContentValues values = new ContentValues();
        values.put(Calls.IS_READ, true);
        context.getContentResolver().update(Calls.CONTENT_URI, values,
                selection, null);
    }

    /**
     * Delete the call from the device log.
     *
     * @param callId - the call id on the device.
     */
    public static void deleteCall(Context context, String callId) {

        Log.i(TAG, "Delete call for Id :" + callId);

        String selection = Calls._ID + " =" + callId;
        context.getContentResolver().delete(Calls.CONTENT_URI, selection, null);

    }

    /**
     * @param activity    - activity used to start the call intent.
     * @param phoneNumber - contact phone number.
     */
    public static void makeACall(Activity activity, String phoneNumber) {

        Log.i(TAG, "Make a call for #:" + phoneNumber);

        String phoneNumberURI = CallsManager.CALL_INTENT_PREFEX + phoneNumber;
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(phoneNumberURI));

        //TODO: Build a callListener service to listen for the call pending intent.
        //instead of setting the preference here. note that: NOT 100% ACCURATE.
        PreferencesManager preferencesManager = new PreferencesManager(activity);
        preferencesManager.setProperty(PreferencesManager.CALLED_FROM_APP, true);

        activity.startActivityForResult(callIntent, START_DIAL_ACTION_REQUEST_CODE);
    }

    /**
     * @param activity    - activity used to start the call intent.
     * @param phoneNumber - contact phone number.
     */
    public static void makeADial(Activity activity, String phoneNumber) {

        Log.i(TAG, "Make dial call for #:" + phoneNumber);
        String phoneNumberURI = CallsManager.CALL_INTENT_PREFEX + phoneNumber;
        Intent callIntent = new Intent(Intent.ACTION_DIAL,
                Uri.parse(phoneNumberURI));

        activity.startActivityForResult(callIntent,
                START_DIAL_ACTION_REQUEST_CODE);
    }

}