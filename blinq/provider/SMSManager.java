package com.blinq.provider;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Part;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;

import com.blinq.models.Contact;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MessageType;
import com.blinq.models.MmsMessage;
import com.blinq.models.Platform;
import com.blinq.module.message.utils.MessageConverter;
import com.blinq.provider.HeadboxFeed.Messages;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Retrieves SMS information from SMS content provider.
 *
 * @author Johan Hansson.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class SMSManager {

    private Context context;
    private static final String TAG = SMSManager.class.getSimpleName();

    public static final String MESSAGING_STATUS_RECEIVED_ACTION = "com.android.mms.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED";
    public static final String MESSAGING_PACKAGE_NAME = "com.android.mms";
    public static final String MESSAGING_STATUS_CLASS_NAME = MESSAGING_PACKAGE_NAME
            + ".transaction.MessageStatusReceiver";

    /**
     * projection for SMS table.
     */
    private static final String[] SMS_COLUMNS = new String[]{Sms._ID,
            Sms.THREAD_ID, Sms.DATE, Sms.ADDRESS, Sms.PERSON, Sms.TYPE,
            Sms.SUBJECT, Sms.BODY, Sms.READ};

    private static final String[] MMS_COLUMNS = new String[]{Mms._ID,
            Mms.THREAD_ID, Mms.DATE, Mms.SUBJECT, Mms.MESSAGE_TYPE,
            Mms.SUBJECT_CHARSET, Mms.READ};

    //
    // SMS/MMS URIs for inbox/outbox/sent/address/etc...
    //
    public static final Uri CONTENT_URI = Uri.parse("content://sms");
    public static final Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");
    public static final Uri SMS_OUTBOX_URI = Uri.parse("content://sms/outbox");
    public static final Uri SMS_SENT_URI = Uri.parse("content://sms/sent");
    public static final Uri MMS_SMS_CONTENT_URI = Uri
            .parse("content://mms-sms/");
    public static final Uri MMS_INBOX_CONTENT_URI = Mms.Inbox.CONTENT_URI;
    private static final Uri MMS_PART_URI = Uri.parse("content://mms/part/");
    ;
    public static final Uri CONVERSATION_CONTENT_URI = Uri.withAppendedPath(
            MMS_SMS_CONTENT_URI, "conversations");
    public static final Uri THREAD_ID_CONTENT_URI = Uri.withAppendedPath(
            MMS_SMS_CONTENT_URI, "threadID");
    public static final Uri MMS_CONTENT_URI = Mms.CONTENT_URI;
    private static final String MMS_ADDRESS = "content://mms/{0}/addr";

    /**
     * Values for Mms/Sms READ column.
     */
    private static final int READ_VALUE = 1;
    private static final int UNREAD_VALUE = 0;

    /**
     * SMS Messages UNREAD Condition.
     */
    private static final String UNREAD_CONDITION = Sms.READ + "="
            + UNREAD_VALUE;

    /**
     * Default sort order clause when querying SMS table.
     */
    private static final String SMS_DEFAULT_SORT_ORDER = Sms._ID + " DESC ";
    /**
     * Default sort order clause when querying MMS table.
     */
    public static final String MMS_DEFUALT_SORT_ORDER = Mms._ID + " DESC ";

    private static final String SMS_TYPE_SELECTION = Sms.TYPE + " IN ( "
            + MessageType.INCOMING.getId() + "," + MessageType.OUTGOING.getId()
            + ")";

    private static final String MMS_TYPE_SELECTION = Mms.MESSAGE_BOX + " IN ("
            + Mms.MESSAGE_BOX_INBOX + "," + Mms.MESSAGE_BOX_SENT + ","
            + Mms.MESSAGE_BOX_OUTBOX + ")";

    /**
     * MMS From [Address] field type components.
     */
    public static final String FROM_INSERT_ADDRESS_TOKEN_STR = "insert-address-token";

    /**
     * pdu fields.
     * <p/>
     * X-Mms-Message-Type field types.
     */
    private static final int MESSAGE_TYPE_SEND_REQ = 128;
    private static final int MESSAGE_TYPE_FORWARD_REQ = 137;
    private static final int MESSAGE_TYPE_CANCEL_CONF = 151;

    /**
     * Displaying text for an empty body Mms.
     */
    private static final String MMS_BLANK_SUBJECT = "(No subject)";

    private final String SENT = "SMS_SENT";
    private final String DELIVERED = "SMS_DELIVERED";

    public SMSManager(Context context) {
        super();
        Log.d(TAG, "create SMSManager.");
        this.context = context;
    }

    /**
     * Returns list of Sms messages from Sms table.
     *
     * @param period - in days.
     */
    public static List<ContentValues> getSMSHistory(Context context, int period) {

        Log.i(TAG, "getting SMS history");
        List<ContentValues> messages = new ArrayList<ContentValues>();

        String whereClause = Sms.DATE + " > ? ";
        whereClause = DatabaseUtils.concatenateWhere(whereClause,
                SMS_TYPE_SELECTION);

        Date now = new Date();
        long fromDate = (now.getTime() - period * Constants.DAY_IN_MILLISECOND);
        String[] whereArgs = new String[]{"" + fromDate};

        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                SMS_COLUMNS, whereClause, whereArgs, Sms.DATE);

        messages = convertSmsCursor(context, cursor);
        return messages;

    }

    /**
     * Get last x messages.
     */
    public static List<ContentValues> getLatestSMS(Context context,
                                                   int messagesCount) {
        Log.i(TAG, "getting latest SMS.");
        List<ContentValues> messages = new ArrayList<ContentValues>();

        if (messagesCount == 0)
            return messages;

        String whereClause = Sms.TYPE + "=?";
        String[] whereArgs = new String[]{MessageType.OUTGOING.getId() + ""};
        String limit = " LIMIT " + messagesCount;
        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                SMS_COLUMNS, whereClause, whereArgs,
                SMS_DEFAULT_SORT_ORDER + limit);

        messages = convertSmsCursor(context, cursor);
        return messages;
    }

    /**
     * Convert the Sms query result to the list of content values.
     */
    private static List<ContentValues> convertSmsCursor(Context context,
                                                        Cursor cursor) {

        List<ContentValues> messages = new ArrayList<ContentValues>();

        try {
            if (cursor != null && cursor.moveToFirst()) {

                do {
                    HeadboxMessage message = getSms(context, cursor);
                    messages.add(MessageConverter
                            .convertToContentValues(message));
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG,
                    "Error in cursor to message conversion:" + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return messages;

    }

    /**
     * Convert the Mms query result to the list of content values.
     */
    private static List<ContentValues> convertMmsCursor(Context context,
                                                        Cursor cursor) {

        List<ContentValues> messages = new ArrayList<ContentValues>();

        try {
            if (cursor != null && cursor.moveToFirst()) {

                do {
                    HeadboxMessage message = getMms(context, cursor);
                    messages.add(MessageConverter
                            .convertToContentValues(message));
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG,
                    "Error in cursor to message conversion:" + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return messages;

    }

    /**
     * Convert retrieved SMS cursor from Sms table to Headbox message.
     */
    public static HeadboxMessage getSms(Context context, Cursor cursor) {

        int idColumn = cursor.getColumnIndex(Sms._ID);
        int dateColumn = cursor.getColumnIndex(Sms.DATE);
        int addressColumn = cursor.getColumnIndex(Sms.ADDRESS);
        int bodyColumn = cursor.getColumnIndex(Sms.BODY);
        int typeColumn = cursor.getColumnIndex(Sms.TYPE);
        int readColumn = cursor.getColumnIndex(Sms.READ);

        String id = cursor.getString(idColumn);
        int type = cursor.getInt(typeColumn);
        MessageType messageType = getSmsType(type);

        String message = cursor.getString(bodyColumn);
        String address = cursor.getString(addressColumn);
        Date date = AppUtils.convertToDate(cursor.getLong(dateColumn));
        int read = cursor.getInt(readColumn);

        if (address == null || address.length() <= 0) {
            address = StringUtils.UNKNOWN_NUMBER;
        }

        HeadboxMessage sms = new HeadboxMessage();

        Contact contact = new Contact(address);
        sms.setContact(contact);

        sms.setMessageId(id);
        sms.setSourceId(id);
        sms.setType(messageType);
        sms.setPlatform(Platform.SMS);
        sms.setBody(message);
        sms.setDate(date);

        if (messageType == MessageType.INCOMING) {
            sms.setRead(read);
        } else {
            sms.setRead(Messages.MSG_READ);
        }

        return sms;
    }

    /**
     * Map Sms [android-type] to headbox message-type.
     */
    public static MessageType getSmsType(int type) {

        switch (type) {
            case Sms.MESSAGE_TYPE_INBOX:
                return MessageType.INCOMING;
            case Sms.MESSAGE_TYPE_SENT:
                return MessageType.OUTGOING;
            case Sms.MESSAGE_TYPE_QUEUED:
                return MessageType.QUEUED;
            case Sms.MESSAGE_TYPE_FAILED:
                return MessageType.FAILED;
            case Sms.MESSAGE_TYPE_DRAFT:
                return MessageType.DRAFT;
        }
        return null;
    }

    /**
     * Mark list of SMSs as read.
     *
     * @param context
     * @param smsMessagesID - list of SMS Ids.
     */
    public static void markAsRead(Context context, String[] smsMessagesID) {

        String selection = Sms._ID + " IN ("
                + StringUtils.convertToString(smsMessagesID) + " )";

        ContentValues values = new ContentValues();
        values.put(Sms.READ, true);
        context.getContentResolver().update(CONTENT_URI, values, selection,
                null);
    }


    /**
     * Read the PDUs out of an {@see #SMS_RECEIVED_ACTION} or a
     * {@see #DATA_SMS_RECEIVED_ACTION} intent.
     *
     * @param intent the intent to read from
     * @return an array of SmsMessages for the PDUs
     */
    public static final SmsMessage[] getMessagesFromIntent(Intent intent) {
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        if (messages == null) {
            return null;
        }
        if (messages.length == 0) {
            return null;
        }

        byte[][] pduObjs = new byte[messages.length][];

        for (int i = 0; i < messages.length; i++) {
            pduObjs[i] = (byte[]) messages[i];
        }
        byte[][] pdus = new byte[pduObjs.length][];
        int pduCount = pdus.length;
        SmsMessage[] msgs = new SmsMessage[pduCount];
        for (int i = 0; i < pduCount; i++) {
            pdus[i] = pduObjs[i];
            msgs[i] = SmsMessage.createFromPdu(pdus[i]);
        }
        return msgs;
    }

    /**
     * Tries to locate the message id (from the system database), given the
     * message thread id, the timestamp of the message.
     */
    public static long findMessageId(Context context, long threadId,
                                     long timestamp, String body) {

        Log.d(TAG, "Find SMS ID for thread:" + threadId);

        long id = 0;
        String selection = Sms.BODY + " = "
                + DatabaseUtils.sqlEscapeString(body != null ? body : "");
        selection += " and " + UNREAD_CONDITION;
        final String sortOrder = Sms.DATE + " DESC";
        final String[] projection = new String[]{Sms._ID, Sms.DATE,
                Sms.THREAD_ID, Sms.BODY};

        Cursor cursor = null;
        if (threadId > 0) {
            Log.i(TAG, "Trying to find message for thread:" + threadId);

            try {
                cursor = context.getContentResolver().query(
                        ContentUris.withAppendedId(CONVERSATION_CONTENT_URI,
                                threadId), projection, selection, null,
                        sortOrder
                );

                if (cursor != null && cursor.moveToFirst()) {
                    id = cursor.getLong(0);
                    Log.i(TAG, "Message id found = " + id);
                }
            } catch (SQLiteException sqLiteException) {
                Log.e(TAG,
                        "Find SMS Id Sql lite Exception:"
                                + sqLiteException.toString() + "\n for thread:"
                                + threadId
                );
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e.toString() + "\n for thread:"
                        + threadId);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (id == 0) {
            Log.d(TAG, "Message id could not be found for thread:" + threadId);

        }

        return id;
    }

    /**
     * Tries to locate the message thread id given the address (phone or email)
     * of the message sender.
     *
     * @param context a context to use
     * @param address phone number or email address of sender
     * @return the thread id (or 0 if there was a problem)
     */
    public static long findThreadIdFromAddress(Context context, String address) {

        if (address == null)
            return 0;

        Log.d(TAG, "Trying find thread id from address :" + address);
        String THREAD_RECIPIENT_QUERY = "recipient";

        Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter(THREAD_RECIPIENT_QUERY, address);

        long threadId = 0;

        Cursor cursor = null;
        try {

            cursor = context.getContentResolver().query(uriBuilder.build(),
                    new String[]{Contacts._ID}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                threadId = cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to get thread for address :" + address);
            return threadId;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return threadId;
    }


    /**
     * Add an SMS to the given URI.
     *
     * @param context  the context to use
     * @param uri      the URI to add the message to
     * @param address  the address of the sender
     * @param body     the body of the message
     * @param date     the timestamp for the message
     * @param read     true if the message has been read, false if not
     * @return the URI for the new message
     */
    public static Uri addMessageToUri(Context context, Uri uri, String address,
                                      String body, Long date, boolean read) {

        ContentValues values = new ContentValues();
        if (date != null) {
            values.put(Sms.DATE, date);
        }
        values.put(Sms.ADDRESS, address);
        values.put(Sms.READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
        values.put(Sms.BODY, body);

        long threadId = findThreadIdFromAddress(context, address);
        if (threadId != 0) {
            values.put(Sms.THREAD_ID, threadId);
        }
        return context.getContentResolver().insert(uri, values);
    }

    /**
     * Move a message to the given folder.
     *
     * @param context the context to use
     * @param uri     the message to move
     * @param folder  the folder to move to
     * @return true if the operation succeeded
     */
    public static boolean moveMessageToFolder(Context context, Uri uri,
                                              int folder) {
        if (uri == null) {
            return false;
        }

        boolean markAsUnread = false;
        boolean markAsRead = false;
        switch (folder) {
            case Sms.MESSAGE_TYPE_INBOX:
            case Sms.MESSAGE_TYPE_DRAFT:
                break;
            case Sms.MESSAGE_TYPE_OUTBOX:
            case Sms.MESSAGE_TYPE_SENT:
                markAsRead = true;
                break;
            case Sms.MESSAGE_TYPE_FAILED:
            case Sms.MESSAGE_TYPE_QUEUED:
                markAsUnread = true;
                break;
            default:
                return false;
        }

        ContentValues values = new ContentValues(2);

        values.put(Sms.TYPE, folder);
        if (markAsUnread) {
            values.put(Sms.READ, Integer.valueOf(0));
        } else if (markAsRead) {
            values.put(Sms.READ, Integer.valueOf(1));
        }

        int result = 0;

        try {
            result = context.getContentResolver().update(uri, values, null,
                    null);
        } catch (Exception e) {
        }

        if (result == 1) {
            Log.d(TAG, "message " + uri.toString() + " updated.");
        } else {
            Log.d(TAG, "Unable to update message " + uri.toString());
        }

        return 1 == result;

    }

    /**
     * Delete certain message from SMS table.
     *
     * @param messageId - message id in the android SMS table.
     * @return # of affected records.
     */
    public static int deleteSms(Context context, String messageId) {

        if (messageId == null)
            return 0;

        long id = 0;
        try {
            id = Long.valueOf(messageId);
        } catch (NumberFormatException e) {
            return 0;
        }

        int result = 0;

        try {
            Uri uri = ContentUris.withAppendedId(CONTENT_URI, id);
            result = context.getContentResolver().delete(uri, null, null);
        } catch (Exception e) {
            Log.e(TAG,
                    "Unknown exception while trying to delete sms "
                            + e.getMessage()
            );
        }

        if (result > 0) {
            Log.d(TAG, "Sms has been successfully deleted.");
        } else {
            Log.d(TAG, "Unable to delete Sms.");
        }

        return 0;
    }

    /**
     * Get last Mms message from Mms table.
     */
    public static HeadboxMessage getLastMms(Context context) {

        String selection = UNREAD_CONDITION;
        String[] selectionArgs = null;
        final String sortOrder = Mms._ID + " DESC ";

        Cursor cursor = context.getContentResolver().query(
                MMS_INBOX_CONTENT_URI, MMS_COLUMNS, selection, selectionArgs,
                sortOrder);

        MmsMessage message = getMms(context, cursor);
        return message;

    }

    /**
     * Convert mms cursor to Mms Model.
     */
    public static MmsMessage getMms(Context context, Cursor cursor) {

        int count = 0;

        MmsMessage mmsMessage = null;
        if (cursor != null) {
            try {
                count = cursor.getCount();
                if (count > 0) {

                    cursor.moveToFirst();

                    String messageId = cursor.getString(cursor
                            .getColumnIndex(Mms._ID));
                    long timestamp = cursor.getLong(cursor
                            .getColumnIndex(Mms.DATE)) * 1000;
                    int type = Integer.parseInt(cursor.getString(cursor
                            .getColumnIndex(Mms.MESSAGE_TYPE)));
                    String subject = cursor.getString(cursor
                            .getColumnIndex(Mms.SUBJECT));
                    int read = cursor.getInt(cursor.getColumnIndex(Mms.READ));
                    String body = getMmsPlainText(context, messageId);

                    MessageType messageType = getMMSDirection(type);
                    Contact contact = getMmsAddress(context, messageId);

                    mmsMessage = new MmsMessage(contact, messageId, subject,
                            messageType, Platform.MMS, new Date(timestamp));
                    mmsMessage.setSourceId(messageId);

                    if (body == null || body.equals("")) {
                        mmsMessage.setBody(MMS_BLANK_SUBJECT);
                    } else {
                        mmsMessage.setBody(body);
                    }

                    if (messageType == MessageType.INCOMING) {
                        mmsMessage.setRead(read);
                    } else {
                        mmsMessage.setRead(Messages.MSG_READ);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return mmsMessage;

    }

    /**
     * Returns list of Mms messages from Mms table.
     *
     * @param period - in days.
     */
    public static List<ContentValues> getMMSHistory(Context context, int period) {

        List<ContentValues> messages = new ArrayList<ContentValues>();
        Cursor cursor = context.getContentResolver().query(Mms.CONTENT_URI,
                null, MMS_TYPE_SELECTION, null, Mms.DATE);
        messages = convertMmsCursor(context, cursor);
        return messages;
    }

    /**
     * Delete certain Mms from Mms table.
     *
     * @param messageId - id in the android Mms table.
     * @return # of affected records.
     */
    public static int deleteMms(Context context, String messageId) {

        long id = 0;

        try {
            id = Long.valueOf(messageId);
        } catch (NumberFormatException e) {
            return 0;
        }

        int result = 0;
        try {

            Uri msgUri = ContentUris.withAppendedId(Mms.Inbox.CONTENT_URI, id);
            result = context.getContentResolver().delete(msgUri, null, null);

        } catch (Exception e) {
            Log.e(TAG,
                    "Unknown exception while trying to delete mms "
                            + e.getMessage()
            );
        }

        if (result > 0) {
            Log.d(TAG, "MMS has been successfully deleted.");
        } else {
            Log.e(TAG, "Unable to delete MMS.");
        }
        return result;
    }

    /**
     * Returns the uri of a certain Mms part.
     */
    private static Uri getMmsPartUri(String partId) {
        return ContentUris.withAppendedId(MMS_PART_URI, Long.valueOf(partId));
    }

    /**
     * Returns list of images attached with the Mms.
     * <p/>
     * TODO: update this method to return an attachment uri mapped to the
     * attachment type.
     */
    public static List<Uri> getMmsImages(Context mContext, String messageId) {

        List<Uri> images = new ArrayList<Uri>();

        try {

            String selectionPart = Mms.Part.MSG_ID + "=" + messageId;

            Cursor cursor = mContext.getContentResolver().query(MMS_PART_URI,
                    null, selectionPart, null, null);

            if (cursor.moveToFirst()) {
                do {
                    String partId = cursor.getString(cursor
                            .getColumnIndex(Part._ID));
                    String type = cursor.getString(cursor
                            .getColumnIndex(Part.CONTENT_TYPE));

                    if (ContentType.isSupportedImageType(type)) {
                        Uri imageUri = getMmsPartUri(partId);
                        images.add(imageUri);
                    }

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception happened while getting mms images:"
                    + e.getMessage());
        }
        return images;
    }

    public static String getMmsPlainText(Context mContext, String messageId) {

        String body = "";
        String selectionPart = Mms.Part.MSG_ID + "=" + messageId;

        Cursor cursor = null;
        try {

            cursor = mContext.getContentResolver().query(MMS_PART_URI, null,
                    selectionPart, null, null);

            if (cursor.moveToFirst()) {
                do {
                    String partId = cursor.getString(cursor
                            .getColumnIndex(Part._ID));
                    String type = cursor.getString(cursor
                            .getColumnIndex(Part.CONTENT_TYPE));

                    if (type.equalsIgnoreCase(ContentType.TEXT_PLAIN)) {

                        String data = cursor.getString(cursor
                                .getColumnIndex(Part._DATA));
                        if (data != null) {
                            body = getMmsText(mContext, partId);
                        } else {
                            body = cursor.getString(cursor
                                    .getColumnIndex(Part.TEXT));
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Unknown exception happened while getting mms text:"
                            + e.getMessage()
            );
        }
        return body;
    }

    /**
     * Get the message text from the mms part.
     */
    private static String getMmsText(Context context, String partId) {

        Uri partURI = getMmsPartUri(partId);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = context.getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return sb.toString();
    }

    /**
     * Returns an address information for Mms message.
     */
    public static Contact getMmsAddress(Context context, String messageId) {

        Contact contact = null;

        String address = null;
        String uriString = MessageFormat.format(MMS_ADDRESS, messageId);
        Uri uriAddress = Uri.parse(uriString);

        String selection = Mms.Addr.TYPE + "=" + MESSAGE_TYPE_FORWARD_REQ;
        String selectionAdd = Mms.Addr.MSG_ID + "=" + messageId;
        selection = DatabaseUtils.concatenateWhere(selection, selectionAdd);

        Cursor cursorAddress = null;

        try {

            cursorAddress = context.getContentResolver().query(uriAddress,
                    null, selection, null, null);

            if (cursorAddress.moveToNext()) {

                address = cursorAddress.getString(cursorAddress
                        .getColumnIndex(Mms.Addr.ADDRESS));

                if (address.contentEquals(FROM_INSERT_ADDRESS_TOKEN_STR)) {

                    selection = Mms.Addr.TYPE + "=" + MESSAGE_TYPE_CANCEL_CONF;
                    Cursor cursorAddress2 = context.getContentResolver().query(
                            uriAddress, null, selection, null, null);

                    if (cursorAddress2 != null) {

                        while (cursorAddress2.moveToNext()) {
                            address = cursorAddress2.getString(cursorAddress2
                                    .getColumnIndex(Mms.Addr.ADDRESS));

                            if (!address
                                    .contentEquals(FROM_INSERT_ADDRESS_TOKEN_STR)) {
                                cursorAddress2.moveToLast();
                            }
                        }

                        cursorAddress2.close();
                    }
                }

                Log.v("MMS", "Mms address: " + address.toString());

            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Unknown exception while getting MMS address:"
                            + e.getMessage()
            );
        } finally {
            if (cursorAddress != null) {
                cursorAddress.close();
            }
        }
        contact = new Contact(address);

        return contact;
    }

    /**
     * Return Mms direction[incoming/outgoing]
     */
    public static MessageType getMMSDirection(int type) {
        MessageType direction;
        if (type == MESSAGE_TYPE_SEND_REQ) {
            direction = MessageType.OUTGOING;
        } else {
            direction = MessageType.INCOMING;
        }
        return direction;
    }

    /**
     * Utilities for the Mms supported content values.
     */
    public static class ContentType {

        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_HTML = "text/html";
        public static final String TEXT_VCALENDAR = "text/x-vCalendar";
        public static final String TEXT_VCARD = "text/x-vCard";

        public static final String IMAGE_UNSPECIFIED = "image/*";
        public static final String IMAGE_JPEG = "image/jpeg";
        public static final String IMAGE_JPG = "image/jpg";
        public static final String IMAGE_GIF = "image/gif";
        public static final String IMAGE_WBMP = "image/vnd.wap.wbmp";
        public static final String IMAGE_PNG = "image/png";
        public static final String IMAGE_X_MS_BMP = "image/x-ms-bmp";

        public static final String AUDIO_UNSPECIFIED = "audio/*";
        public static final String AUDIO_AAC = "audio/aac";
        public static final String AUDIO_AMR = "audio/amr";
        public static final String AUDIO_IMELODY = "audio/imelody";
        public static final String AUDIO_MID = "audio/mid";
        public static final String AUDIO_MIDI = "audio/midi";
        public static final String AUDIO_MP3 = "audio/mp3";
        public static final String AUDIO_MPEG3 = "audio/mpeg3";
        public static final String AUDIO_MPEG = "audio/mpeg";
        public static final String AUDIO_MPG = "audio/mpg";
        public static final String AUDIO_MP4 = "audio/mp4";
        public static final String AUDIO_X_MID = "audio/x-mid";
        public static final String AUDIO_X_MIDI = "audio/x-midi";
        public static final String AUDIO_X_MP3 = "audio/x-mp3";
        public static final String AUDIO_X_MPEG3 = "audio/x-mpeg3";
        public static final String AUDIO_X_MPEG = "audio/x-mpeg";
        public static final String AUDIO_X_MPG = "audio/x-mpg";
        public static final String AUDIO_3GPP = "audio/3gpp";
        public static final String AUDIO_X_WAV = "audio/x-wav";
        public static final String AUDIO_OGG = "application/ogg";

        public static final String VIDEO_UNSPECIFIED = "video/*";
        public static final String VIDEO_3GPP = "video/3gpp";
        public static final String VIDEO_3G2 = "video/3gpp2";
        public static final String VIDEO_H263 = "video/h263";
        public static final String VIDEO_MP4 = "video/mp4";

        private static final ArrayList<String> contentTypes = new ArrayList<String>();
        private static final ArrayList<String> imageTypes = new ArrayList<String>();
        private static final ArrayList<String> audioTypes = new ArrayList<String>();
        private static final ArrayList<String> videoTypes = new ArrayList<String>();

        static {

            contentTypes.add(TEXT_PLAIN);
            contentTypes.add(TEXT_HTML);
            contentTypes.add(TEXT_VCALENDAR);
            contentTypes.add(TEXT_VCARD);
            contentTypes.add(IMAGE_JPEG);
            contentTypes.add(IMAGE_GIF);
            contentTypes.add(IMAGE_WBMP);
            contentTypes.add(IMAGE_PNG);
            contentTypes.add(IMAGE_JPG);
            contentTypes.add(IMAGE_X_MS_BMP);
            contentTypes.add(AUDIO_AAC);
            contentTypes.add(AUDIO_AMR);
            contentTypes.add(AUDIO_IMELODY);
            contentTypes.add(AUDIO_MID);
            contentTypes.add(AUDIO_MIDI);
            contentTypes.add(AUDIO_MP3);
            contentTypes.add(AUDIO_MPEG3);
            contentTypes.add(AUDIO_MPEG);
            contentTypes.add(AUDIO_MPG);
            contentTypes.add(AUDIO_X_MID);
            contentTypes.add(AUDIO_X_MIDI);
            contentTypes.add(AUDIO_X_MP3);
            contentTypes.add(AUDIO_X_MPEG3);
            contentTypes.add(AUDIO_X_MPEG);
            contentTypes.add(AUDIO_X_MPG);
            contentTypes.add(AUDIO_X_WAV);
            contentTypes.add(AUDIO_3GPP);
            contentTypes.add(AUDIO_OGG);
            contentTypes.add(VIDEO_3GPP);
            contentTypes.add(VIDEO_3G2);
            contentTypes.add(VIDEO_H263);
            contentTypes.add(VIDEO_MP4);

            // add supported image types
            imageTypes.add(IMAGE_JPEG);
            imageTypes.add(IMAGE_GIF);
            imageTypes.add(IMAGE_WBMP);
            imageTypes.add(IMAGE_PNG);
            imageTypes.add(IMAGE_JPG);
            imageTypes.add(IMAGE_X_MS_BMP);

            // add supported audio types
            audioTypes.add(AUDIO_AAC);
            audioTypes.add(AUDIO_AMR);
            audioTypes.add(AUDIO_IMELODY);
            audioTypes.add(AUDIO_MID);
            audioTypes.add(AUDIO_MIDI);
            audioTypes.add(AUDIO_MP3);
            audioTypes.add(AUDIO_MPEG3);
            audioTypes.add(AUDIO_MPEG);
            audioTypes.add(AUDIO_MPG);
            audioTypes.add(AUDIO_MP4);
            audioTypes.add(AUDIO_X_MID);
            audioTypes.add(AUDIO_X_MIDI);
            audioTypes.add(AUDIO_X_MP3);
            audioTypes.add(AUDIO_X_MPEG3);
            audioTypes.add(AUDIO_X_MPEG);
            audioTypes.add(AUDIO_X_MPG);
            audioTypes.add(AUDIO_X_WAV);
            audioTypes.add(AUDIO_3GPP);
            audioTypes.add(AUDIO_OGG);

            // add supported video types
            videoTypes.add(VIDEO_3GPP);
            videoTypes.add(VIDEO_3G2);
            videoTypes.add(VIDEO_H263);
            videoTypes.add(VIDEO_MP4);

        }

        // This class should never be instantiated.
        private ContentType() {
        }

        public static boolean isSupportedType(String contentType) {
            return (null != contentType) && contentTypes.contains(contentType);
        }

        public static boolean isSupportedImageType(String contentType) {
            return isImageType(contentType) && isSupportedType(contentType);
        }

        public static boolean isSupportedAudioType(String contentType) {
            return isAudioType(contentType) && isSupportedType(contentType);
        }

        public static boolean isSupportedVideoType(String contentType) {
            return isVideoType(contentType) && isSupportedType(contentType);
        }

        public static boolean isTextType(String contentType) {
            return (null != contentType) && contentType.startsWith("text/");
        }

        public static boolean isImageType(String contentType) {
            return (null != contentType) && contentType.startsWith("image/");
        }

        public static boolean isAudioType(String contentType) {
            return (null != contentType) && contentType.startsWith("audio/");
        }

        public static boolean isVideoType(String contentType) {
            return (null != contentType) && contentType.startsWith("video/");
        }
    }

}