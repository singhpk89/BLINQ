package com.blinq.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;

import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.provider.HeadboxFeed.Feeds;
import com.blinq.provider.HeadboxFeed.Messages;
import com.blinq.utils.HeadboxPhoneUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;

import org.apache.commons.collections.ListUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Contact provider to deal with device local contacts.
 *
 * @author Johan Hansson.
 */
public class ContactsManager {

    public static final String TAG = ContactsManager.class.getSimpleName();
    public static final String MIMI_TYPE_SKYPE = "vnd.android.cursor.item/com.skype.android.chat.action";
    public static final String MIMI_TYPE_WHATSAPP = "vnd.android.cursor.item/vnd.com.whatsapp.profile";
    public static final String MIMI_TYPE_EMAIL = "vnd.android.cursor.item/email_v2";
    public static final String MIMI_TYPE_PHONE = "vnd.android.cursor.item/phone_v2";
    public static final String CONTACT_DATA_URI = "content://com.android.contacts/data/%s";

    /**
     * Returns list of content values contains merged Sms,Mms,Call logs.
     *
     * @param context - From which to call this method.
     * @param period  - in days
     * @return - List of content values
     */
    public static List<ContentValues> getLogsHistory(Context context, int period) {

        List<ContentValues> smsHistory = SMSManager.getSMSHistory(context,
                period);
        List<ContentValues> mmsHistory = SMSManager.getMMSHistory(context,
                period);
        List<ContentValues> callsHistory = CallsManager.getCallsHistory(
                context, period);

        Log.d(TAG, "Headbox loaded " + smsHistory.size() + " SMS.");
        Log.d(TAG, "Headbox loaded " + mmsHistory.size() + " MMS.");
        Log.d(TAG, "Headbox loaded " + callsHistory.size() + " CALLS.");

        return mergeTwoListsByDate(callsHistory,
                mergeTwoListsByDate(smsHistory, mmsHistory));
    }

    public static List<ContentValues> getLastLogsHistory(Context context,
                                                         int logsCount) {

        List<ContentValues> SMSs = SMSManager.getLatestSMS(context, logsCount);
        List<ContentValues> calls = CallsManager.getLatestCalls(context,
                logsCount);

        return mergeTwoListsByDate(SMSs, calls);
    }

    /**
     * Splits the contacts into hash map with phone number as a key for the map.
     *
     * @param context - app context
     * @param period  - in days
     * @return Hash map with phone number as a key and messages as a list .
     */
    public static Map<String, ContentValues[]> getContactsLogsHistory(
            Context context, int period) {
        Map<String, List<ContentValues>> map = new HashMap<String, List<ContentValues>>();
        Map<String, ContentValues[]> logs = new HashMap<String, ContentValues[]>();
//        List<ContentValues> merged = getLogsHistory(context, period);
//        int index;
//        for (index = 0; index < merged.size(); index++) {
//            List<ContentValues> messages = map.get(HeadboxPhoneUtils
//                    .getPhoneNumber((String) merged.get(index).get(
//                            Feeds.FEED_IDENTIFIER)));
//            if (messages == null)
//                messages = new ArrayList<ContentValues>();
//            messages.add(merged.get(index));
//            map.put(HeadboxPhoneUtils.getPhoneNumber((String) merged.get(index).get(
//                    Feeds.FEED_IDENTIFIER)), messages);
//        }
//        for (String phoneNumber : map.keySet()) {
//            logs.put(
//                    phoneNumber,
//                    map.get(phoneNumber).toArray(
//                            new ContentValues[map.get(phoneNumber).size()])
//            );
//
//        }
        return logs;
    }

    public static Map<String, ContentValues[]> getRecentLogsHistory(
            Context context, int count) {

        Map<String, List<ContentValues>> map = new HashMap<String, List<ContentValues>>();
        Map<String, ContentValues[]> logs = new HashMap<String, ContentValues[]>();
        List<ContentValues> merged = getLastLogsHistory(context, count);
        int index;
        for (index = 0; index < merged.size(); index++) {
            List<ContentValues> messages = map.get(HeadboxPhoneUtils
                    .getPhoneNumber((String) merged.get(index).get(
                            Feeds.FEED_IDENTIFIER)));
            if (messages == null)
                messages = new ArrayList<ContentValues>();
            messages.add(merged.get(index));
            map.put(HeadboxPhoneUtils.getPhoneNumber((String) merged.get(index).get(
                    Feeds.FEED_IDENTIFIER)), messages);
        }
        for (String phonenumber : map.keySet()) {
            logs.put(
                    phonenumber,
                    map.get(phonenumber).toArray(
                            new ContentValues[map.get(phonenumber).size()])
            );
        }
        return logs;
    }

    /**
     * Returns URI for the contact photo.
     */
    private static Uri getContactPhoto(Long contactId, Context context) {

        Uri contactUri = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri,
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

        try {
            // Check if we have photoStream for given photoUri.
            // If Not raise FileNotFoundException and return null.
            InputStream inputStream = context.getContentResolver()
                    .openInputStream(photoUri);
        } catch (FileNotFoundException e) {
            return Uri.parse("");
        } catch (Exception e) {
            return Uri.parse("");
        }
        return photoUri;
    }

    /**
     * Returns the contact with the phone raw according to entered contact Id.
     */
    public static Contact getContact(Context context, String contactId) {

        String name = null;
        String number = null;
        String normalized = null;
        Contact contact = null;

        // New tables projection criteria to get contact for the contact
        // Id.
        String[] columnNames = new String[]{Phone._ID, Phone.CONTACT_ID,
                Phone.DISPLAY_NAME, Phone.NUMBER};

        Uri uri = Phone.CONTENT_URI;

        // If Facebook account Id is defined in contacts , contact is to
        // be got.
        if (contactId != null && !contactId.equals("")) {

            Cursor cursor = context.getContentResolver()
                    .query(uri, columnNames,
                            Phone.CONTACT_ID + "=" + contactId, null, null);

            if (cursor.moveToFirst()) {

                name = cursor.getString(cursor
                        .getColumnIndex(Phone.DISPLAY_NAME));
                number = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                normalized = HeadboxPhoneUtils.getPhoneNumber(number);
                contact = new Contact(contactId, name, number, normalized);
                contact.setContactType(Platform.CALL);
                contact.setPhotoUri(getContactPhoto(Long.valueOf(contactId),
                        context));

            }

        }

        return contact;
    }

    /**
     * Get list of contacts to the user who owns the Phone.
     *
     * @param context .
     */
    public static List<Contact> getOwnerContacts(Context context) {

        // projection.
        String[] columnNames = new String[]{Profile._ID, Profile.DISPLAY_NAME,
                Profile.PHOTO_URI, Profile.DISPLAY_NAME_PRIMARY};

        Cursor c = null;
        List<Contact> contacts = null;

        try {

            c = context.getContentResolver().query(Profile.CONTENT_URI,
                    columnNames, null, null, null);

            if (c.moveToFirst()) {

                contacts = new ArrayList<Contact>();

                do {
                    String contactId = c.getString(c
                            .getColumnIndex(Profile._ID));
                    String contactName = c.getString(c
                            .getColumnIndex(Profile.DISPLAY_NAME));
                    String alternativeName = c.getString(c
                            .getColumnIndex(Profile.DISPLAY_NAME_PRIMARY));
                    String photoURI = c.getString(c
                            .getColumnIndex(Profile.PHOTO_URI));

                    Contact contact = new Contact();
                    if (photoURI != null) {
                        contact.setPhotoUri(getContactPhoto(Long.valueOf(contactId),
                                context));
                        contact.setHasPhoto(true);
                    } else {
                        contact.setHasPhoto(false);
                    }

                    if (!StringUtils.isBlank(contactName)) {
                        contact.setName(contactName);
                    } else if (!StringUtils.isBlank(alternativeName)) {
                        contact.setName(alternativeName);
                    } else {
                        contact.setName("");
                    }
                    contacts.add(contact);
                } while (c.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "Unable to get contact..");
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return contacts;
    }

    /**
     * Merges two sorted lists according to date .
     *
     * @return merged single sorted list with ascending date.
     */
    private static List<ContentValues> mergeTwoListsByDate(
            List<ContentValues> list1, List<ContentValues> list2) {

        List<ContentValues> merged = ListUtils.union(list1, list2);
        sortMessagesByDate(merged);
        return merged;
    }

    /**
     * Sort the list of messages according to date . An implementation to the
     * <code>Comparator</code> is done to decide the comparison criteria.
     *
     * @param mergedLists - link to the list to be sorted
     */
    private static void sortMessagesByDate(List<ContentValues> mergedLists) {

        Collections.sort(mergedLists, new Comparator<ContentValues>() {

            @Override
            public int compare(ContentValues contentValues1,
                               ContentValues contentValues2) {
                return contentValues1.getAsLong(Messages.DATE).compareTo(
                        contentValues2.getAsLong(Messages.DATE));
            }

        });
    }

    /**
     * Returns list of contact mapped to the hash key using contact id.
     */
    public static HashMap<String, Contact> getAllContacts(Context context) {

        final Uri contactsContentUri = ContactsContract.Data.CONTENT_URI;
        final String contactRawId = ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID;
        final String contactDisplayName = ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME;

        final String whereName = ContactsContract.Data.MIMETYPE + " = ?";
        final String[] whereNameParams = new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
        final String[] contactsProjection = new String[]{contactRawId,
                contactDisplayName};

        HashMap<String, Contact> contacts = new HashMap<String, Contact>();

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(contactsContentUri,
                contactsProjection, whereName, whereNameParams, null);

        try {

            // Query and loop for every contact to save contact's names.
            while (cursor.moveToNext()) {

                try {

                    String contactId = cursor.getString(cursor
                            .getColumnIndex(contactRawId));

                    String displayName = "";
                    if (!cursor.isNull(cursor
                            .getColumnIndex(contactDisplayName))) {
                        displayName = cursor.getString(cursor
                                .getColumnIndex(contactDisplayName));
                    }

                    Uri contactUri = ContentUris.withAppendedId(
                            ContactsContract.Contacts.CONTENT_URI,
                            Long.valueOf(contactId));
                    Uri photoUri = Uri.withAppendedPath(contactUri,
                            ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

                    Contact contact = new Contact();
                    contact.setContactId(contactId);
                    contact.setName(displayName);
                    contact.setPhotoUri(photoUri);
                    contacts.put(contactId, contact);

                } catch (Exception e) {
                    Log.e(TAG, "Unable to parse contact.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to get contacts.");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contacts;
    }

    /**
     * Map platform to a phone list's mimi type value.
     */
    public static String getMimiType(Platform platform) {

        switch (platform) {
            case CALL:
                return MIMI_TYPE_PHONE;
            case SKYPE:
                return MIMI_TYPE_SKYPE;
            case WHATSAPP:
                return MIMI_TYPE_WHATSAPP;
            case EMAIL:
                return MIMI_TYPE_EMAIL;
        }
        return null;
    }

    /**
     * Map phone list's mimi type value to a headbox platform.
     */
    private static Platform getPlatform(String mimiType) {

        if (mimiType.equals(MIMI_TYPE_PHONE))
            return Platform.CALL;
        else if (mimiType.equals(MIMI_TYPE_SKYPE))
            return Platform.SKYPE;
        else if (mimiType.equals(MIMI_TYPE_WHATSAPP))
            return Platform.WHATSAPP;
        else if (mimiType.equals(MIMI_TYPE_EMAIL))
            return Platform.EMAIL;

        return Platform.NOTHING;
    }

    /**
     * Get list of Mimi types given a list of headbox platforms.
     */
    public static List<String> getMimiTypes(List<Platform> platforms) {
        List<String> mimiTypes = new ArrayList<String>();
        for (Platform platform : platforms)
            mimiTypes.add(getMimiType(platform));

        return mimiTypes;
    }

    /**
     * Returns list of contacts with a data kind representing an IM address.
     * You should provide a list of platforms to be returned from the phone contacts list.
     */
    public static List<Contact> getContacts(Context context, List<Platform> platforms) {

        if (platforms == null || platforms.size() == 0)
            return null;

        final Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;
        final String IM_CONTACT_ID = ContactsContract.CommonDataKinds.Im.CONTACT_ID;
        final String IM_DATA_KIND = ContactsContract.CommonDataKinds.Im.DATA;
        final String IM_PROTOCOL = ContactsContract.CommonDataKinds.Im.PROTOCOL;
        final String IM_MIMI_TYPE = Data.MIMETYPE;
        final String IM_DISPLAY_NAME = ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME;

        final String SELECTION = IM_MIMI_TYPE + " IN "
                + "(" + StringUtils.convertStringsForINQuery(getMimiTypes(platforms)) + ")";

        final String[] PROJECTION = new String[]{IM_CONTACT_ID, IM_DATA_KIND,
                IM_PROTOCOL, IM_MIMI_TYPE, IM_DISPLAY_NAME};

        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(CONTENT_URI, PROJECTION,
                SELECTION, null, IM_CONTACT_ID);

        List<Contact> contacts = new ArrayList<Contact>();

        try {

            // Query and loop through list of email addresses.
            if (cursor != null)

                while (cursor.moveToNext()) {

                    try {

                        String contactId = cursor.getString(cursor
                                .getColumnIndex(IM_CONTACT_ID));
                        String displayName = cursor.getString(cursor
                                .getColumnIndex(IM_DISPLAY_NAME));
                        String imAddress = cursor.getString(cursor
                                .getColumnIndex(IM_DATA_KIND));

                        Platform platform = Platform.NOTHING;

                        if (contactId != null && imAddress != null) {

                            String type = cursor.getString(cursor
                                    .getColumnIndex(IM_MIMI_TYPE));

                            platform = getPlatform(type);

                            if (platform != Platform.NOTHING) {

                                Contact contact = new Contact();
                                contact.setContactId(contactId);
                                contact.setIdentifier(imAddress);

                                if (platform == Platform.CALL) {
                                    contact.setNormalizedIdentifier(HeadboxPhoneUtils
                                            .getPhoneNumber(imAddress));
                                } else {
                                    contact.setNormalizedIdentifier(imAddress);
                                }

                                contact.setContactType(platform);
                                contact.setName(displayName);
                                contacts.add(contact);
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Unable to parse an im contact.");
                    }
                }

        } catch (Exception e) {
            Log.e(TAG, "Unable to get im contacts.");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contacts;

    }


}