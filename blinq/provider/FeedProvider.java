package com.blinq.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.facebook.Actions.FriendsAction;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.models.Contact;
import com.blinq.models.FeedModel;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.provider.HeadboxFeed.FacebookContacts;
import com.blinq.provider.HeadboxFeed.FeedColumns;
import com.blinq.provider.HeadboxFeed.FeedPlatformsColumns;
import com.blinq.provider.HeadboxFeed.Feeds;
import com.blinq.provider.HeadboxFeed.GoogleContacts;
import com.blinq.provider.HeadboxFeed.HeadboxContacts;
import com.blinq.provider.HeadboxFeed.MergeLinks;
import com.blinq.provider.HeadboxFeed.Messages;
import com.blinq.provider.HeadboxFeed.MessagesColumns;
import com.blinq.provider.HeadboxFeed.PhoneContacts;
import com.blinq.provider.utils.HeadboxDBQueries;
import com.blinq.provider.utils.ModelConverter;
import com.blinq.service.notification.HeadboxNotificationListenerService;
import com.blinq.service.platform.FacebookUtilsService;
import com.blinq.utils.AppUtils;
import com.blinq.utils.HeadboxDBUtils;
import com.blinq.utils.HeadboxPhoneUtils;
import com.blinq.utils.ImageUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.google.gson.Gson;

import org.apache.commons.collections.ListUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides the ability to query the Feeds database , also this class provides a
 * way to insert, delete or update all messages in a Feed.
 *
 * @author Johan Hansson.
 */

public class FeedProvider extends ContentProvider {

    private static final String TAG = FeedProvider.class.getSimpleName();

    private static final UriMatcher URIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    private static final String PROVIDER_NAME = "com.blinq.provider.feeds";
    private static final String URL = "content://" + PROVIDER_NAME;
    private static final int URI_CONTACTS_FILTERED = 121;
    private static List<String> contactsForMerge;
    private SQLiteOpenHelper openHelper;
    // Reference to an object to block simultaneous writing when merging
    // contacts
    private Object synchronizeRef = new Object();
    private PreferencesManager preferenceManager;
    private boolean isDuplicate;

    public SQLiteOpenHelper getOpenHelper() {
        return openHelper;
    }

    public void setOpenHelper(SQLiteOpenHelper openHelper) {
        this.openHelper = openHelper;
    }

    // Defined to be matched with URI paths.
    private static final int URI_FEEDS = 0;
    private static final int URI_FEED_MESSAGES = 1;
    private static final int URI_FEED_MEMBER_CONTACTS = 2;
    private static final int URI_FEED_ID = 3;
    private static final int URI_COMPLETE_FEEDS = 4;
    private static final int URI_FEEDS_SUBJECT = 5;
    private static final int URI_FEED_BY_PLATFORMS = 7;
    private static final int URI_FEED_PLATFORMS = 8;
    private static final int URI_MESSAGE_ID = 9;
    private static final int URI_FEEDS_HISTORY = 10;
    private static final int URI_REFRESH_HISTORY = 34;
    private static final int URI_CONTACT = 11;
    private static final int URI_MESSAGE = 13;
    private static final int URI_SEARCH = 14;
    private static final int URI_FEED_UNREAD_PLATFORMS = 15;
    private static final int URI_CONTACT_PHONES_EMAILS = 16;
    private static final int URI_FACEBOOK_HISTORY = 17;
    private static final int URI_GOOGLE_PLUS_CONTACTS = 19;
    private static final int URI_CONTACTS_MERGE = 22;
    private static final int URI_FEEDS_MERGE = 23;
    private static final int URI_UPDATE_GOOGLE_CONTACTS = 24;
    private static final int URI_PLATFORM_CONTACTS = 25;
    private static final int URI_UPDATE_GOOGLE_CONTACTS_COVERS = 26;
    private static final int URI_GENERAL_CONTACTS = 27;
    private static final int URI_CONTACTS_UNMERGE = 28;
    private static final int URI_MESSAGE_SOURCE_ID = 30;
    private static final int URI_ALL_MESSAGES_DELETE = 31;
    private static final int URI_FEED_LAST_MESSAGE_BY_PLATFORM = 32;
    private static final int URI_DELETE_MERGE_LINK = 33;
    private static final int URI_UNDO_FEED_MERGE = 34;
    private static final int URI_UPDATE_CONTACTS = 120;
    private static final int URI_UPDATE_TOP_FRIENDS_NOTIFICATIONS = 35;
    private static final int URI_UPDATE_NOTIFICATION_FOR_CONTACT = 36;
    private static final int URI_INSERT_WELCOME_FEED = 37;
    private static final int URI_UPDATE_CONTACT_NAME_FROM_SERVER = 38;
    private static final int URI_CONTACTS_COUNT = 39;

    static HashMap<String, String> sContactsProjection = new HashMap<String, String>();
    static HashMap<String, String> sFeedsProjection = new HashMap<String, String>();

    /**
     * A list of which columns to return when querying
     * {@link HeadboxDatabaseHelper.TABLE_MESSAGES}.
     */
    private static String[] MESSAGES_COLUMNS = {MessagesColumns._ID,
            MessagesColumns.Feed_ID, MessagesColumns.PLATFORM_ID,
            MessagesColumns.CONTACT, MessagesColumns.CONTACT_COMPLETE,
            MessagesColumns.READ, MessagesColumns.SEEN, MessagesColumns.DATE,
            MessagesColumns.BODY, MessagesColumns.SUBJECT,
            MessagesColumns.TYPE, MessagesColumns.SOURCE_ID};

    /**
     * A list of which columns to return when querying
     * {@link HeadboxDatabaseHelper.TABLE_FEEDS}.
     */
    public static String[] FEEDS_COLUMNS = {Feeds._ID, Feeds.MESSAGE_COUNT,
            Feeds.DATE, Feeds.LAST_MSG_DATE, Feeds.SNIPPET_TEXT,
            Feeds.MEMBERS_IDS, Feeds.FEED_IDENTIFIER, Feeds.READ,
            Feeds.LAST_MSG_TYPE, Feeds.LAST_CALL_TYPE, Feeds.MODIFIED};

    /**
     * A list of which columns to return when querying
     * {@link HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS}.
     */
    private String[] CONTACTS_COLUMNS = new String[]{
            HeadboxContacts.CONTACT_ID, HeadboxContacts.PRIMARY_INDENTIFIER,
            HeadboxContacts.COMPLETE_INDENTIFIER, HeadboxContacts.NAME,
            HeadboxContacts.ALTER_INDENTIFIER, HeadboxContacts.PICTURE_URL,
            HeadboxContacts.COVER_URL, HeadboxContacts.CONTACT_TYPE};
    ;

    public static Map<String, ContentValues[]> messages;

    private static final String GENERAL_SEARCH_DEFAULT_ORDER = " ORDER BY msg_date desc,headbox_contacts.headbox_contact_name asc LIMIT 25";
    private static final String GENERAL_SEARCH_DEFAULT_ORDER2 = " ORDER BY msg_date desc,contacts.headbox_contact_name asc LIMIT 25";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        final Context context = getContext();
        openHelper = HeadboxDatabaseHelper.getInstance(context);
        preferenceManager = new PreferencesManager(context);
        return openHelper == null ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;

        switch (URIMatcher.match(uri)) {

            case URI_COMPLETE_FEEDS:
                cursor = getFeeds(projection, selection, null, sortOrder);
                break;
            case URI_FEED_MESSAGES:
                // feed id
                cursor = getFeedMessages(uri.getPathSegments().get(1), projection,
                        selection, sortOrder);
                break;
            case URI_FEED_ID:
                // feed id
                cursor = getFeedById(uri.getPathSegments().get(1));
                break;

            case URI_FEED_LAST_MESSAGE_BY_PLATFORM:
                cursor = getFeedLastMessage(uri.getPathSegments().get(1));
                break;
            case URI_FEED_BY_PLATFORMS:
                Platform platform = Platform.fromId(Integer.parseInt(uri
                        .getPathSegments().get(1)));
                cursor = getFilteredFeeds(projection, selection, sortOrder,
                        platform);
                break;
            case URI_FEED_PLATFORMS:
                // feed id
                cursor = getFeedPlatforms(uri.getPathSegments().get(1), projection,
                        selection, sortOrder);
                break;
            case URI_FEED_UNREAD_PLATFORMS:
                cursor = getFeedUnReadPlatforms(uri.getPathSegments().get(1));
                break;
            case URI_FEED_MEMBER_CONTACTS:
                // Feed Id.
                cursor = getContactsByFeedId(uri.getPathSegments().get(1),
                        selection);
                break;
            case URI_CONTACT_PHONES_EMAILS:
                // Contact Id.
                cursor = getContacts(uri.getPathSegments().get(1), selection);
                break;
            case URI_MESSAGE_ID:
                // Message Id
                cursor = getMessage(uri.getPathSegments().get(1), projection,
                        selection, selectionArgs, sortOrder);
                break;
            case URI_SEARCH:

                Platform source = Platform.fromId(Integer.parseInt(uri
                        .getPathSegments().get(1)));
                String searchQuery = uri.getPathSegments().get(2);
                cursor = searchContact(searchQuery, source, selection);

                break;

            case URI_CONTACT:
                cursor = query(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                        selection, selectionArgs);
                break;
            case URI_GENERAL_CONTACTS:
                source = Platform.fromId(Integer.parseInt(uri
                        .getPathSegments().get(1)));
                cursor = getContactsByPlatform(source);
                break;
            case URI_PLATFORM_CONTACTS:
                Platform contactsSource = Platform.fromId(Integer.parseInt(uri
                        .getPathSegments().get(1)));
                cursor = getContacts(contactsSource, selection);
                break;
            case URI_FACEBOOK_HISTORY:
                updateHistory(Platform.FACEBOOK, messages);
                updateUnknownContacts();
                break;
            case URI_CONTACTS_COUNT:
                cursor = getContactsCount();
            default:
                break;
        }

        return cursor;
    }

    private Cursor getContactsCount() {

        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor result = db.rawQuery("SELECT COUNT(*) FROM " + HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, null);
        return result;
    }

    /**
     * Query the given table, returning a Cursor over the result set.
     */
    private Cursor query(String table, String selection, String[] selectionArgs) {

        SQLiteDatabase db = openHelper.getReadableDatabase();
        return query(db, table, selection, selectionArgs);

    }

    private Cursor query(SQLiteDatabase db, String table, String selection,
                         String[] selectionArgs) {

        return db
                .query(table, null, selection, selectionArgs, null, null, null);
    }

    /**
     * Get merged contacts by the contact id.
     */
    private Cursor getContacts(String contactId, String selection) {

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        String contactSelection = HeadboxContacts.CONTACT_ID + " = '"
                + contactId + "'";
        String concatSelection = DatabaseUtils.concatenateWhere(
                contactSelection, selection);

        builder.setTables(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS);
        builder.setDistinct(true);
        Cursor cursor = builder.query(openHelper.getReadableDatabase(),
                CONTACTS_COLUMNS, concatSelection, null, null, null, null);

        return cursor;
    }

    /**
     * Return cursor contains the unread platforms. (platforms have unread
     * messages.) for a certain feed.
     */
    private Cursor getFeedUnReadPlatforms(String feedId) {

        String query = null;
        Cursor cursor = null;
        if (feedId.equals("0")) {
            //TODO: DELETE THIS.
            query = "SELECT DISTINCT feed_id,platform FROM messages WHERE read = 0 AND platform "
                    + " NOT IN ( SELECT platform FROM (SELECT platform,type,max(date) "
                    + " FROM messages  GROUP BY platform HAVING type ="
                    + MessageType.OUTGOING.getId() + " ORDER BY platform ))";
            cursor = openHelper.getReadableDatabase().rawQuery(query, null);
        } else {
            //Return the last 4 platforms have unread messages.
            query = "SELECT DISTINCT platform,unread_messages_count FROM feed_platforms WHERE feed_id = ? " +
                    " AND unread_messages_count > 0 ORDER BY msg_date DESC LIMIT 4";
            cursor = openHelper.getReadableDatabase().rawQuery(query,
                    new String[]{feedId});
        }

        return cursor;
    }

    /**
     * Update Headbox contacts by local contacts from the device.
     */
    private void updateLocalContacts() {

        long startTime = System.currentTimeMillis();

        SQLiteDatabase db = openHelper.getWritableDatabase();

        db.beginTransaction();
        try {

            String contactsTypeCondition = PhoneContacts.PHONE_CONTACT_TYPE
                    + " IN (" + Platform.CALL.getId() + ","
                    + Platform.EMAIL.getId() + "," + Platform.SKYPE.getId()
                    + "," + Platform.WHATSAPP.getId() + "," + Platform.SMS.getId()
                    + "," +Platform.MMS.getId() + ")";

            // Insert added contacts.
            String insertQuery = "INSERT INTO headbox_contacts "
                    + "(phone_contact_id,headbox_contact_name,"
                    + "headbox_contact_first_name,headbox_contact_last_name, "
                    + "headbox_picture_url,headbox_cover_url,"
                    + "headbox_contact_complete_identifier, headbox_contact_primary_identifier, "
                    + "headbox_contact_alternative_identifier, headbox_contact_type) "
                    + "SELECT DISTINCT phone_contact_id,phone_contact_name,"
                    + "phone_contact_first_name,phone_contact_last_name,"
                    + "phone_picture_url,'', "
                    + "phone_contact_complete_identifier,phone_contact_identifier,"
                    + "'',phone_contact_type "
                    + "FROM headbox_phone_contacts WHERE "
                    + contactsTypeCondition
                    + " AND phone_contact_identifier NOT IN "
                    + "( SELECT headbox_contact_primary_identifier FROM headbox_contacts ) "
                    + "ORDER BY phone_contact_id,phone_contact_identifier";

            // Get and delete the deleted contacts from phone.
            String whereClause = "headbox_contact_primary_identifier IN "
                    + "(SELECT DISTINCT headbox_contact_primary_identifier FROM headbox_contacts "
                    + "WHERE headbox_contact_type =  "
                    + Platform.CALL.getId()
                    + " AND headbox_contact_primary_identifier NOT IN "
                    + "(SELECT phone_contact_identifier FROM headbox_phone_contacts "
                    + "WHERE phone_contact_type =" + Platform.CALL.getId()
                    + " ))";

            // Get updated contacts.
            String query = "SELECT * FROM headbox_contacts JOIN headbox_phone_contacts "
                    + "ON headbox_contact_primary_identifier = phone_contact_identifier "
                    + "AND headbox_contact_type = phone_Contact_type "
                    + "AND (headbox_contact_name != phone_contact_name "
                    + "OR headbox_picture_url != phone_picture_url ) ";

            db.execSQL(insertQuery);

            int deletedRows = db.delete(
                    HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, whereClause,
                    null);

            Log.d(TAG, "Deleted contacts,count = " + deletedRows);

            // Now we need to do updating process.
            Cursor cursor = db.rawQuery(query, null);

            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {

                        Platform platform = Platform
                                .fromId(cursor.getInt(cursor
                                        .getColumnIndex(PhoneContacts.PHONE_CONTACT_TYPE)));
                        String name = cursor
                                .getString(cursor
                                        .getColumnIndex(PhoneContacts.PHONE_CONTACT_NAME));
                        String picture = cursor
                                .getString(cursor
                                        .getColumnIndex(PhoneContacts.PHONE_PICTURE_URI));
                        String identifier = cursor
                                .getString(cursor
                                        .getColumnIndex(HeadboxContacts.PRIMARY_INDENTIFIER));

                        if (platform == Platform.CALL) {

                            ContentValues values = new ContentValues(2);

                            values.put(HeadboxContacts.NAME, name);
                            values.put(HeadboxContacts.PICTURE_URL, picture);

                            String where = HeadboxContacts.PRIMARY_INDENTIFIER
                                    + "=?";
                            String[] whereArgs = new String[]{identifier};

                            db.update(
                                    HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                                    values, where, whereArgs);
                        }

                        Log.d(TAG,
                                "Updated contacts,count = " + cursor.getCount());
                    }
                } catch (Exception e) {
                    Log.e(TAG,
                            "Exception while updating contacts : "
                                    + e.getMessage()
                    );
                } finally {
                    cursor.close();
                }
            }

            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

        long timeTaken = AppUtils.findTime(startTime);
        Log.v(TAG, "time taken to update headbox with the local contacts was "
                + timeTaken + " millisecond");
    }

    /**
     * Get a list of contacts for a given feed.
     */
    private Cursor getContactsByFeedId(String feedId, String selection) {

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS);
        builder.setDistinct(true);

        String where = HeadboxContacts.CONTACT_ID
                + "="
                + "(SELECT phone_contact_id FROM headbox_contacts JOIN feeds ON "
                + "headbox_contacts.headbox_contact_primary_identifier = feeds.members_ids AND feeds._id = ? ) ";

        selection = DatabaseUtils.concatenateWhere(where, selection);
        String[] selectionArgs = new String[]{feedId};
        String groupBy = HeadboxContacts.PRIMARY_INDENTIFIER;
        String sortOrder = HeadboxContacts.CONTACT_TYPE;

        Cursor cursor = builder.query(openHelper.getReadableDatabase(),
                CONTACTS_COLUMNS, selection, selectionArgs, groupBy, null,
                sortOrder, null, null);

        return cursor;
    }

    /**
     * Search contacts and return cursor contains the result.
     */
    private Cursor searchContact(String filterParam, Platform platform,
                                 String selection) {

        StringBuilder queryBuilder = null;

        //Here to support platform.
        switch (platform) {
            case CALL:
            case SMS:
                queryBuilder = buildContactsLookupForMergeQuery(
                        Platform.CALL.getId() + "", filterParam);
                queryBuilder.append(" AND " + selection);
/*                queryBuilder
                        .append(" AND headbox_contacts.phone_contact_id NOT IN "
                                + "(SELECT phone_contact_id FROM headbox_contacts "
                                + " WHERE headbox_contact_type !="
                                + Platform.CALL.getId() + ") ");*/
                queryBuilder.append(" GROUP BY headbox_contacts.phone_contact_id ");
                queryBuilder.append(GENERAL_SEARCH_DEFAULT_ORDER);
                break;
            case FACEBOOK:
            case HANGOUTS:
            case TWITTER:
            case WHATSAPP:
            case EMAIL:
            case INSTAGRAM:
            case SKYPE:
                queryBuilder = buildContactsLookupForMergeQuery(platform.getId()
                        + "", filterParam);
                queryBuilder.append(" AND " + selection);
/*                queryBuilder
                        .append(" AND headbox_contacts.phone_contact_id NOT IN "
                                + "(SELECT phone_contact_id FROM headbox_contacts "
                                + " WHERE headbox_contact_type ="
                                + Platform.CALL.getId() + ") ");*/
                queryBuilder.append(" GROUP BY headbox_contacts.phone_contact_id ");
                queryBuilder.append(GENERAL_SEARCH_DEFAULT_ORDER);
                break;
            case OTHER:
                // Build ContactsLookup query.
                queryBuilder = buildContactsLookupQuery(filterParam);
                queryBuilder.append(GENERAL_SEARCH_DEFAULT_ORDER2);
                break;
            default:
                break;

        }
        // Log.d(TAG, queryBuilder.toString());
        Cursor cursor = openHelper.getReadableDatabase().rawQuery(
                queryBuilder.toString(), null);

        return cursor;
    }


    private Cursor getContactsByPlatform(Platform platform) {

        String selection = null;
        if (platform != Platform.ALL)
            selection = HeadboxContacts.CONTACT_TYPE + "=" + platform.getId();

        Cursor c = openHelper.getReadableDatabase()
                .query(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, null, selection, null, null, null, null);
        return c;
    }

    private Cursor getContacts(Platform platform, String selection) {

        String extendedSelection = null;
        String condition = null;
        String groupBy = "phone_contact_id";
        String orderBy = "msg_date desc,headbox_contact_name asc ";
        String limit = "25";

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (platform) {

            case CALL:
            case SMS:
                //Here to get contacts for a certain platform.
                qb.setTables(HeadboxDBQueries.CONTACTS_JOIN_FEEDS);
                qb.setProjectionMap(sContactsProjection);
                condition = " phone_contact_id NOT IN "
                        + "(SELECT phone_contact_id FROM headbox_contacts "
                        + " WHERE headbox_contact_type !=" + Platform.CALL.getId()
                        + ") ";
                extendedSelection = DatabaseUtils.concatenateWhere(selection,
                        condition);

                break;
            case FACEBOOK:
            case HANGOUTS:
            case TWITTER:
            case INSTAGRAM:
            case SKYPE:
            case WHATSAPP:
            case EMAIL:
                qb.setTables(HeadboxDBQueries.CONTACTS_JOIN_FEEDS);
                qb.setProjectionMap(sContactsProjection);
                condition = " phone_contact_id NOT IN "
                        + "(SELECT phone_contact_id FROM headbox_contacts "
                        + " WHERE headbox_contact_type = " + Platform.CALL.getId()
                        + ") ";
                extendedSelection = DatabaseUtils.concatenateWhere(selection,
                        condition);
                qb.appendWhere(extendedSelection);
                break;
            default:
                break;
        }

        Cursor cursor = qb.query(openHelper.getReadableDatabase(), null, null,
                null, groupBy, null, orderBy, limit);

        return cursor;
    }

    /**
     * Parse query string and build query to scan headbox_contacts lookup table.
     * //TODO: Need Refactoring.
     */
    private StringBuilder buildContactsLookupQuery(String filterParam) {

        // parse the actual string to know that
        // it can use the headbox_contacts lookup index
        // to do a prefix search.

        String queryString1 = DatabaseUtils.getHexCollationKey(filterParam);
        String queryString2 = DatabaseUtils.getHexCollationKey(" "
                + filterParam);

        StringBuilder filter = new StringBuilder(
                HeadboxDBQueries.GENERAL_SEARCH_QUERY2
                        + " WHERE contacts.id IN "
                        + "(SELECT source FROM headbox_contacts_Lookup WHERE token GLOB "
        );

        DatabaseUtils.appendEscapedSQLString(filter, queryString1 + "*");
        DatabaseUtils.appendEscapedSQLString(filter.append(" OR token GLOB "),
                "*" + queryString2 + "*");
        filter.append(")");
        filter.append(" GROUP BY contacts.phone_contact_id ");

        return filter;
    }

    // TODO: Need Refactoring.
    private StringBuilder buildContactsLookupForMergeQuery(String platform,
                                                           String filterParam) {

        String queryString1 = DatabaseUtils.getHexCollationKey(filterParam);
        String queryString2 = DatabaseUtils.getHexCollationKey(" "
                + filterParam);

        StringBuilder filter = new StringBuilder(
                HeadboxDBQueries.GENERAL_SEARCH_QUERY
                        + "WHERE headbox_contacts.id IN "
                        + "(SELECT source FROM headbox_contacts_Lookup WHERE token GLOB "
        );

        DatabaseUtils.appendEscapedSQLString(filter, queryString1 + "*");
        DatabaseUtils.appendEscapedSQLString(filter.append(" OR token GLOB "),
                "*" + queryString2 + "*");
        filter.append(")");
        filter.append(" AND headbox_contacts.headbox_contact_type = "
                + platform);

        return filter;

    }

    /**
     * Get Message using id.
     */
    private Cursor getMessage(String messageId, String[] projection,
                              String selection, String[] selectionArgs, String sortOrder) {

        String finalQuery = buildMessageTableQuery(projection, selection,
                sortOrder);
        return openHelper.getReadableDatabase().rawQuery(finalQuery,
                selectionArgs);
    }

    /**
     * Get list of feeds. each feed will show the last message text/body of a
     * certain platform.
     */
    private Cursor getFilteredFeeds(String[] projection, String selection,
                                    String sortOrder, Platform platform) {

        // TODO Temporary Implementation.
        // To be enhanced and moved.

        String platformCondition = platform.getIdAsString();
        if (platform == Platform.SMS) {
            // We want to join SMS,MMS feeds within one tab.
            platformCondition = Platform.SMS.getId() + ","
                    + Platform.MMS.getId();
        }

        String query = null;

        if (selection != null
                && selection.equals(Feeds.MODIFIED + "=" + Feeds.FEED_MODIFIED)) {
            query = HeadboxDBQueries.FILTERED_MODIFIED_FEEDS_QUERY;
        } else {
            query = HeadboxDBQueries.FILTERED_FEEDS_QUERY;
        }

        // Append where statement:
        if (selection != null)
            query += " WHERE " + selection;
        if (sortOrder != null)
            query += " ORDER BY " + sortOrder;

        query = String.format(query, platformCondition);
        Cursor cursor = openHelper.getReadableDatabase().rawQuery(query, null);

        return cursor;
    }

    /**
     * Get last message for each platform joined with a contact from
     * headbox_contact for a certain feed.
     */
    private Cursor getFeedLastMessage(String feedId) {

        String query = "SELECT DISTINCT * FROM (SELECT * FROM messages WHERE feed_id = ? "
                + " GROUP BY messages.platform HAVING MAX(date)) AS messages "
                + " LEFT JOIN headbox_contacts ON "
                + " messages.contact = headbox_contacts.headbox_contact_primary_identifier";

        Cursor cursor = openHelper.getReadableDatabase().rawQuery(query,
                new String[]{feedId});

        return cursor;
    }

    /**
     * Get a list of platforms associated with a given feed.
     *
     * @param feedId
     * @param projection - A list of which columns to return. Passing null will return
     *                   all columns.
     * @param selection  - A filter declaring which rows to return.
     * @param sortOrder  - How to order the rows, formatted as an "SQL ORDER BY".
     *                   Passing null will return default sort order.
     */
    private Cursor getFeedPlatforms(String feedId, String[] projection,
                                    String selection, String sortOrder) {

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        String filterSelection = FeedPlatformsColumns.Feed_ID + " = " + feedId;
        String querySelection = DatabaseUtils.concatenateWhere(selection,
                filterSelection);

        String[] columns = new String[]{FeedPlatformsColumns.PLATFORM_ID};
        builder.setTables(HeadboxDatabaseHelper.TABLE_FEED_PLATFORMS);
        builder.setDistinct(true);

        Cursor cursor = builder.query(openHelper.getReadableDatabase(),
                columns, querySelection, null, null, null,
                FeedPlatformsColumns.PLATFORM_ID);

        return cursor;
    }


    private int updateNotificationForContact(SQLiteDatabase db, String contactId) {

        //Get current state of notification - on / off
        String[] columns = new String[]{HeadboxContacts.ID, HeadboxContacts.NOTIFICATION_ENABLED};
        String selection = HeadboxContacts.CONTACT_ID + "=" + contactId;
        Cursor cursor = null;

        long notificationEnabledValue = 0;
        try {
            cursor = db.query(false, HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                    columns, selection, null, null, null, null, null);
            cursor.moveToFirst();
            notificationEnabledValue = cursor.getLong(cursor.getColumnIndex(HeadboxContacts.NOTIFICATION_ENABLED));
        } catch (Exception e) {
            Log.d(TAG, e.getMessage() + "");
        } finally {
            if (cursor != null)
                cursor.close();
        }

        //Update notification status to the second state.
        notificationEnabledValue = (notificationEnabledValue == HeadboxContacts.NOTIFICATION_DISABLED_VALUE
                ? HeadboxContacts.NOTIFICATION_ENABLED_VALUE : HeadboxContacts.NOTIFICATION_DISABLED_VALUE);
        ContentValues values = new ContentValues();
        values.put(HeadboxContacts.NOTIFICATION_ENABLED, notificationEnabledValue);

        int result = 0;
        try {
            result = db
                    .update(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                            values, HeadboxContacts.CONTACT_ID + "=" + contactId, null
                    );
        } catch (Exception e) {
            Log.d(TAG, e.getMessage() + "");
        }
        return result;
    }

    private int updateTopFriendsNotifications(SQLiteDatabase db) {

        //This would set the notifications of the friend

        String NUM_TOP_FRIEND_TO_GET = "20"; //TODO

        String[] columns = new String[]{Feeds._ID,
                Feeds.MEMBERS_IDS,
                Feeds.MESSAGE_COUNT,
                HeadboxContacts.CONTACT_ID,
                HeadboxContacts.NAME};

        String orderBy = Feeds.MESSAGE_COUNT + " DESC";

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(HeadboxDBQueries.FEEDS_INNER_JOIN_CONTACTS);
        qb.setDistinct(true);
        Cursor cursor = null;

        List<String> contactsToUpdate = new ArrayList<String>();
        List<String> currentTopFriends = new ArrayList<String>();

        try {

            String[] topFriendsColumns = new String[]{HeadboxContacts.CONTACT_ID};
            String topFriendsSelection = HeadboxContacts.NOTIFICATION_ENABLED + "="
                    + HeadboxContacts.NOTIFICATION_ENABLED_VALUE;

            Cursor topFriendsCursor = db.query(true, HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                    topFriendsColumns, topFriendsSelection, null, null, null, null, null);

            if (topFriendsCursor.moveToFirst()) {
                do {
                    int contactId = topFriendsCursor.getInt(topFriendsCursor.getColumnIndex(HeadboxContacts.CONTACT_ID));
                    currentTopFriends.add(String.valueOf(contactId));
                } while (topFriendsCursor.moveToNext());
            }

            //Reset the notification enabled column.
            ContentValues resetTopFriends = new ContentValues();
            resetTopFriends.put(HeadboxContacts.NOTIFICATION_ENABLED,
                    HeadboxContacts.NOTIFICATION_DISABLED_VALUE);
            db.update(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, resetTopFriends, topFriendsSelection, null);

            //get a list of top friends.
            cursor = qb.query(db, columns, null, null, null, null, orderBy, NUM_TOP_FRIEND_TO_GET);
            if (cursor.moveToFirst()) {
                do {
                    long feedId = cursor.getLong(cursor.getColumnIndex(Feeds._ID));
                    int messageCount = cursor.getInt(cursor.getColumnIndex(Feeds.MESSAGE_COUNT));
                    int contactId = cursor.getInt(cursor.getColumnIndex(HeadboxContacts.CONTACT_ID));
                    String contactName = cursor.getString(cursor.getColumnIndex(HeadboxContacts.NAME));
                    Log.d(TAG, "Top friend contact: " + contactName + " with " + messageCount + " Events with id " + feedId);
                    contactsToUpdate.add(String.valueOf(contactId));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage() + "");
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (contactsToUpdate.size() <= 0) {
            Log.d(TAG, "Can't find top friends");
            return 0;
        }

        List<String> difference = ListUtils.subtract(contactsToUpdate, currentTopFriends);

        //Update notifications to be enabled for the found list of top friends.
        ContentValues values = new ContentValues();
        values.put(HeadboxContacts.NOTIFICATION_ENABLED, HeadboxContacts.NOTIFICATION_ENABLED_VALUE);

        //We should also enable the notification for the merged contacts...
        String where = HeadboxContacts.CONTACT_ID
                + " IN (" + StringUtils.convertNumbersForINQuery(contactsToUpdate) + ")";
        int result = db.update(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                values, where, null
        );
        return difference.size();
    }

    /**
     * Inserts new message and then returns content:// URI of the insertion
     * message.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {

        SQLiteDatabase db = openHelper.getWritableDatabase();

        switch (URIMatcher.match(uri)) {

            case URI_MESSAGE:

                Uri messageId = insertMessage(uri, db, contentValues);
                return messageId;

            case URI_GENERAL_CONTACTS:

                long id = insertContact(uri, contentValues);
                Uri contactUri = Uri.withAppendedPath(CONTACTS_URI, id + "");

                return contactUri;

            case URI_INSERT_WELCOME_FEED:
                long feedId = insertWelcomeFeed();
                return null;
            default:
                break;
        }

        return null;
    }

    private ContentValues insertNewFacebookFriendContact(Uri uri, String friendName) {

        Context context = getContext();
        FriendsAction action = new FriendsAction(FacebookAuthenticator.getInstance(context));
        action.setRequestType(Action.RequestType.SYNC);

        long start = System.currentTimeMillis();

        action.execute();

        if (action.getResult() == null) {
            return null;
        }

        Log.d(TAG, "Time to fetch friends list " + Long.valueOf(System.currentTimeMillis() - start));

        for (Contact contact : action.getResult()) {
            if (contact.getName().equals(friendName)) {
                ContentValues values = ModelConverter.convert(contact);
                insertContact(Uri.withAppendedPath(uri, Platform.FACEBOOK.getIdAsString()), values);
                return values;
            }
        }
        return null;
    }

    private ContentValues insertNewNumberContact(SQLiteDatabase db, ContentValues values, Platform platform) {
        ContentValues result = new ContentValues(5);

        long id = getLastIntInColumn(db, HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, PhoneContacts.ID) + 1;
        String phoneNumber = values.getAsString(HeadboxContacts.NAME);

        result.put(PhoneContacts.ID, Long.toString(id));
        result.put(HeadboxContacts.NAME, phoneNumber);
        result.put(HeadboxContacts.PICTURE_URL, "");
        result.put(HeadboxContacts.COMPLETE_INDENTIFIER, phoneNumber);
        result.put(HeadboxContacts.PRIMARY_INDENTIFIER, phoneNumber);
        result.put(HeadboxContacts.CONTACT_TYPE, platform.getIdAsString());
        result.put(HeadboxContacts.NOTIFICATION_ENABLED, "0");

        long inserted = db.insert(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, null, result);
        return result;

    }

    /**
     * Responsible to manipulate,manage and then insert messages from different platforms.
     */
    private Uri insertMessage(Uri uri, SQLiteDatabase db, ContentValues contentValues) {

        ContentValues values = null;

        if (contentValues == null) {
            values = new ContentValues(1);
        } else {
            values = new ContentValues(contentValues);
        }

        // Handle the case when inserting record with no date.
        if (!values.containsKey(Messages.DATE)) {
            values.put(Messages.DATE, Long.valueOf(System.currentTimeMillis()));
        }

        Long feedId = values.getAsLong(Messages.Feed_ID);
        String contact = values.getAsString(Messages.CONTACT);
        String phoneNumber = values.getAsString(Feeds.FEED_IDENTIFIER);
        Platform platform = Platform.fromId(values
                .getAsInteger(Messages.PLATFORM_ID));
        MessageType type = MessageType.fromId(values
                .getAsInteger(Messages.TYPE));
        String sourceId = values.getAsString(Messages.SOURCE_ID);
        String body = values.getAsString(Messages.BODY);
        String name = values.getAsString(HeadboxContacts.NAME);

        // Check if we have a feed if not then search for exist feed or
        // create new one.
        if (platform == Platform.SMS || platform == Platform.HANGOUTS) {

        }
        if (feedId == null || feedId == 0) {
            if (contact == null) {
                feedId = getFeedId(db, platform, values);
            } else {
                feedId = getFeedId(db, contact, phoneNumber);
            }
            if (feedId == 0) {
                ContentValues newValues = null;
                if (platform == Platform.FACEBOOK) {
                    newValues = insertNewFacebookFriendContact(uri, name);
                } else if (platform == Platform.SMS || platform == Platform.HANGOUTS
                        || platform == Platform.WHATSAPP) {
                    newValues = insertNewNumberContact(db, values, platform);
                }
                feedId = getFeedId(db, platform, newValues);
            }

            values.put(Messages.Feed_ID, feedId);
        }

        if (feedId == 0 || isFeedMarkDeleted(db, feedId)) {
            return null;
        }

        // Delete any draft message if the inserted message is not incoming
        // message or a call.
        try {
            if (type != MessageType.INCOMING && platform != Platform.CALL)
                db.delete(
                        HeadboxDatabaseHelper.TABLE_MESSAGES,
                        "feed_id=? AND type=?",
                        new String[]{feedId + "",
                                Integer.toString(MessageType.DRAFT.getId())}
                );
        } catch (Exception e) {
        }

        // TODO: Temporary. Should applied from @Provider.insertMessage
        // method.
        if (platform == Platform.CALL || platform == Platform.SMS
                || platform == Platform.MMS || platform == Platform.HANGOUTS || platform == Platform.WHATSAPP) {
            values.put(Messages.CONTACT_COMPLETE,
                    values.getAsString(Feeds.FEED_IDENTIFIER));
        } else {
            values.put(Messages.CONTACT_COMPLETE,
                    values.getAsString(Messages.CONTACT));
        }

        // Remove values not associated with the messages table.
        values.remove(Feeds.FEED_IDENTIFIER);
        values.remove(HeadboxContacts.NAME);

        long msgId = 0;
        boolean isDuplicateMessage = (platform != Platform.NOTHING)
                && isDuplicateMessage(db, platform, type, sourceId, body,
                feedId);
        if (!isDuplicateMessage) {
            msgId = db.insert(HeadboxDatabaseHelper.TABLE_MESSAGES, null,
                    values);
        }

        /**
         * If record is added successfully
         */
        if (msgId > 0) {
            Uri _uri = null;
            try {

                Uri mUri = Uri.parse(URL + "/"
                        + HeadboxDatabaseHelper.TABLE_MESSAGES);
                Uri feed = Uri.withAppendedPath(mUri, feedId + "");
                _uri = Uri.withAppendedPath(feed, msgId + "");
                Log.d(TAG, "Insert " + _uri + " succeeded");

                return _uri;

            } catch (Exception e) {
                Log.d(TAG, "An exception happened while concat the URI");
                return _uri;
            }


        } else {
            Log.e(TAG, "Insert failed: already exist.");
        }

        return null;
    }


    private boolean isFeedMarkDeleted(SQLiteDatabase db, long feedId) {
        boolean markDeleted = false;
        Cursor c = db.query(HeadboxDatabaseHelper.TABLE_FEEDS, new String[] {Feeds.DELETED}, Feeds._ID + "=" + feedId, null, null, null, null);
        if (c != null) {
            try {
                c.moveToFirst();
                markDeleted = c.getInt(0) == 1;
            } finally {
                c.close();
            }
        }
        if (markDeleted) {
            Log.d(TAG, "feed " + feedId + " marked as deleted. ignoring");
        }
        return markDeleted;
    }
    /**
     * Check whether we have a previous record for this message depending on its
     * platform,type and its id on the real platform.
     */
    private boolean isDuplicateMessage(SQLiteDatabase db, Platform platform,
                                       MessageType type, String sourceId, String body, long feedId) {

        boolean isDuplicate = false;
        Cursor cursor = null;

        try {
            // For now this possible for sms,calls and mms types.

            if ((platform == Platform.SMS && type == MessageType.OUTGOING)
                    || platform == Platform.CALL || platform == Platform.MMS) {

                String selection = Messages.SOURCE_ID + "=?" + " AND "
                        + Messages.PLATFORM_ID + "=?";
                String[] selectionArgs = new String[]{sourceId,
                        platform.getIdAsString()};

                cursor = query(db, HeadboxDatabaseHelper.TABLE_MESSAGES,
                        selection, selectionArgs);

                if (cursor != null && cursor.getCount() > 0) {
                    isDuplicate = true;
                }

            } else if (platform == Platform.SKYPE
                    || platform == Platform.FACEBOOK
                    || platform == Platform.HANGOUTS
                    || platform == Platform.WHATSAPP
                    || platform == Platform.EMAIL) {

                isDuplicate = false;

            }

        } catch (Exception e) {
            Log.d(TAG, "Exception on isDuplicateMessage() " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return isDuplicate;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {

        SQLiteDatabase db = openHelper.getWritableDatabase();

        int result = 0;
        switch (URIMatcher.match(uri)) {
            case URI_FEEDS_HISTORY:
                result = doBulkInsert(uri, values);
                // We need to group/link all logs belong to the same contact.
                // This will be effective if we have a contact
                // with multiple phone numbers.
                //linkCommunicationLogs(db);
                break;
            case URI_REFRESH_HISTORY:
                result = insertLatestLogs(uri, values);
                break;
            case URI_FACEBOOK_HISTORY:
                updateHistory(Platform.FACEBOOK, messages);
                updateUnknownContacts();
                break;
            case URI_UPDATE_CONTACTS:
                result = buildPhoneContactsTable();
                updateLocalContacts();
                break;
            case URI_CONTACT:

                long startTime = System.currentTimeMillis();

                result = buildPhoneContactsTable();
                mergeContacts(Platform.CALL, false);
                mergeContacts(Platform.SKYPE, false);
                mergeContacts(Platform.FACEBOOK, false);
                mergeContacts(Platform.HANGOUTS, false);
                mergeContacts(Platform.TWITTER, false);
                mergeContacts(Platform.INSTAGRAM, false);
                mergeContacts(Platform.WHATSAPP, false);
                mergeContacts(Platform.EMAIL, false);
                linkContacts();

                long timeTaken = AppUtils.findTime(startTime);
                Log.v(TAG, "time taken to insert " + result
                        + " contacts and merge them was " + timeTaken
                        + " milliseconds");

                break;
            case URI_GENERAL_CONTACTS:

                int platform = Integer.valueOf(uri.getPathSegments().get(1));
                String table = getTableName(Platform.fromId(platform));
                result = HeadboxDBUtils.bulkInsert(db, table, values);
                mergeContacts(Platform.fromId(platform), true);
                break;
            case URI_GOOGLE_PLUS_CONTACTS:
                result = HeadboxDBUtils.bulkInsert(db,
                        HeadboxDatabaseHelper.TABLE_GOOGLE_PLUS_CONTACTS, values);
                mergeContacts(Platform.HANGOUTS, true);
                break;
            case URI_UPDATE_GOOGLE_CONTACTS:
                String whereClause = HeadboxContacts.PRIMARY_INDENTIFIER + "=?"
                        + " AND " + HeadboxContacts.CONTACT_TYPE + "="
                        + Platform.HANGOUTS.getId();
                updateContacts(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                        values, whereClause, HeadboxContacts.PRIMARY_INDENTIFIER);
                break;
            case URI_UPDATE_GOOGLE_CONTACTS_COVERS:
                whereClause = HeadboxContacts.ALTER_INDENTIFIER + "=?" + " AND "
                        + HeadboxContacts.CONTACT_TYPE + "="
                        + Platform.HANGOUTS.getId();
                updateContacts(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                        values, whereClause, HeadboxContacts.ALTER_INDENTIFIER);
                db.execSQL(HeadboxDBQueries.TRIGGER_UPDATE_GOOGLE_CONTACT_ON_HEADBOX_CONTACTS_TABLE);
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * Merge all communication logs from different platforms that belong to the
     * same contact.
     */
    private void linkCommunicationLogs(SQLiteDatabase db) {

        // Here to get all contacts have multiple feeds.
        String query = "SELECT DISTINCT _id,phone_contact_id,headbox_contact_primary_identifier "
                + "FROM feeds JOIN headbox_contacts ON members_ids = headbox_contact_primary_identifier "
                + "WHERE headbox_contact_type = "
                + Platform.CALL.getId()
                + " AND phone_contact_id NOT IN "
                + "( SELECT DISTINCT phone_contact_id FROM feeds JOIN headbox_contacts "
                + "ON members_ids = headbox_contact_primary_identifier "
                + "GROUP BY phone_contact_id HAVING COUNT(phone_contact_id) = 1 ) "
                + "ORDER BY phone_contact_id ";

        Cursor c = db.rawQuery(query, null);
        if (c != null) {
            try {

                String contactId = null;
                String feedId = "";

                int CONTACT_ID_INDEX = c
                        .getColumnIndex(HeadboxContacts.CONTACT_ID);
                int FEED_ID_INDEX = c.getColumnIndex(Feeds._ID);

                while (c.moveToNext()) {

                    String currentContact = c.getString(CONTACT_ID_INDEX);
                    String currentFeed = c.getString(FEED_ID_INDEX);

                    if (contactId == null) {

                        contactId = c.getString(CONTACT_ID_INDEX);
                        feedId = c.getString(FEED_ID_INDEX);

                    } else if (currentContact.equals(contactId)) {

                        String update = "UPDATE messages SET feed_id = ? WHERE feed_id = ?";
                        String delete = "DELETE FROM feeds WHERE _id = ?";
                        db.execSQL(update, new String[]{feedId, currentFeed});
                        db.execSQL(delete, new String[]{currentFeed});
                        updateFeed(db, Long.valueOf(feedId));

                    } else if (!currentContact.equals(contactId)) {

                        contactId = currentContact;
                        feedId = currentFeed;
                    }

                }
            } finally {
                c.close();
            }
        }

    }

    /**
     * Find db table name for a certain platform's contacts.
     *
     * @param platform
     * @return
     */
    private String getTableName(Platform platform) {

        String table = "";
        switch (platform) {
            case CALL:
                table = HeadboxDatabaseHelper.TABLE_PHONE_CONTACTS;
                break;
            case FACEBOOK:
                table = HeadboxDatabaseHelper.TABLE_FACEBOOK_CONTACTS;
                break;
            case HANGOUTS:
                table = HeadboxDatabaseHelper.TABLE_GOOGLE_CONTACTS;
                break;
            case INSTAGRAM:
            case TWITTER:
            case STATICINFO:
                table = HeadboxDatabaseHelper.TABLE_PLATFORMS_CONTACTS;
                break;
            default:
                break;
        }
        return table;

    }

    private void updateContacts(String table, ContentValues[] values,
                                String whereClause, String column) {

        SQLiteDatabase db = openHelper.getWritableDatabase();

        db.beginTransaction();
        try {

            for (int i = 0; i < values.length; i++) {

                String[] args = new String[]{values[i].getAsString(column)};
                db.update(table, values[i], whereClause, args);

                if (column == HeadboxContacts.PRIMARY_INDENTIFIER) {

                    ContentValues value = new ContentValues();
                    value.put(GoogleContacts.PICTURE_URL,
                            values[i].getAsString(HeadboxContacts.PICTURE_URL));
                    value.put(GoogleContacts.PLUS_ID, values[i]
                            .getAsString(HeadboxContacts.ALTER_INDENTIFIER));

                    String[] whereArgs = new String[]{values[i]
                            .getAsString(HeadboxContacts.PRIMARY_INDENTIFIER)};

                    db.update(HeadboxDatabaseHelper.TABLE_GOOGLE_CONTACTS,
                            value, GoogleContacts.GOOGLE_ID + "=?", whereArgs);

                } else if (column == HeadboxContacts.ALTER_INDENTIFIER) {

                    ContentValues value = new ContentValues();
                    value.put(GoogleContacts.COVER_PICTURE_URL,
                            values[i].getAsString(HeadboxContacts.COVER_URL));
                    value.put(GoogleContacts.PICTURE_URL,
                            values[i].getAsString(HeadboxContacts.PICTURE_URL));

                    String[] whereArgs = new String[]{values[i]
                            .getAsString(HeadboxContacts.ALTER_INDENTIFIER)};

                    db.update(HeadboxDatabaseHelper.TABLE_GOOGLE_CONTACTS,
                            value, GoogleContacts.PLUS_ID + "=?", whereArgs);
                }
            }
            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

    }

    /**
     * Generate random id for empty id contacts.
     */
    private void updateEmptyIDContacts(SQLiteDatabase db) {

        db.execSQL("UPDATE headbox_contacts SET phone_contact_id = rowid + 1000000 "
                + "WHERE phone_contact_id IS NULL");

    }

    /**
     * General transaction to do an auto merging functionality..
     */
    private void mergeContacts(Platform platform, boolean sendAnalytics) {

        //synchronized (synchronizeRef) {

            Log.d(TAG, "Merging " + platform.name() + " Contacts");

            SQLiteDatabase db = openHelper.getWritableDatabase();

            db.beginTransaction();

            try {

                String where = HeadboxContacts.CONTACT_TYPE + " = "
                        + platform.getId();
                db.delete(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, where,
                        null);
                if (sendAnalytics) {
                    sendAutomaticMerge(db, platform);
                }
                switch (platform) {
                    case CALL:
                    case SKYPE:
                    case EMAIL:
                    case WHATSAPP:
                        db.rawQuery(HeadboxDBQueries.MERGE_CONTACTS_QUERY,
                                new String[]{platform.getIdAsString()});
                        break;
                    case HANGOUTS:
                        db.rawQuery(HeadboxDBQueries.GOOGLE_CONTACTS_MATCHING_ON_ID, null);
                        db.rawQuery(HeadboxDBQueries.GOOGLE_CONTACTS_MATCHING_ON_FULL_NAME, null);
                        break;
                    case FACEBOOK:
                        db.rawQuery(HeadboxDBQueries.FACEBOOK_CONTACTS_MATCHING_ON_ID, null);
                        db.rawQuery(HeadboxDBQueries.FACEBOOK_CONTACTS_MATCHING_ON_FULL_NAME, null);
                        break;
                    case TWITTER:
                    case INSTAGRAM:
                    case STATICINFO:
                        db.rawQuery(
                                HeadboxDBQueries.CONTACTS_MATCHING_ON_FULL_NAME,
                                new String[]{platform.getIdAsString(),
                                        platform.getIdAsString()}
                        );
                        break;
                    default:
                        break;
                }

                updateEmptyIDContacts(db);
                db.setTransactionSuccessful();

            } catch (Throwable ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } finally {
                db.endTransaction();
            }

            linkContacts();
        //}
    }

    /**
     * Automatic merged contacts event
     */
    private void sendAutomaticMerge(SQLiteDatabase db, Platform platform) {

        int mergedContactsCount = 0;

        db.beginTransaction();

        switch (platform) {
            case CALL:
            case SKYPE:
            case EMAIL:
            case WHATSAPP:
                return;
            case HANGOUTS:

                mergedContactsCount += getQueryRawCount(
                        db,
                        HeadboxDBQueries.GET_AUTO_MERGED_GOOGLE_CONTACTS_MATCHING_ON_ID_COUNT);
                mergedContactsCount += getQueryRawCount(
                        db,
                        HeadboxDBQueries.GET_AUTO_MERGED_GOOGLE_CONTACTS_MATCHING_ON_FULL_NAME_COUNT);
                break;
            case FACEBOOK:
                mergedContactsCount += getQueryRawCount(
                        db,
                        HeadboxDBQueries.GET_AUTO_MERGED_FACEBOOK_CONTACTS_MATCHING_ON_FULL_NAME_COUNT);
                mergedContactsCount += getQueryRawCount(
                        db,
                        HeadboxDBQueries.GET_AUTO_MERGED_FACEBOOK_CONTACTS_MATCHING_ON_ID_COUNT);
                break;
            case TWITTER:
            case INSTAGRAM:
                mergedContactsCount += getQueryRawCount(
                        db,
                        HeadboxDBQueries.GET_AUTO_MERGED_CONTACTS_MATCHING_ON_FULL_NAME_COUNT
                                + platform.getId()
                );
                break;
            default:
                break;
        }

        int totalContacts = getQueryRawCount(db,
                HeadboxDBQueries.GET_HEADBOX_CONTACTS_COUNT);

        db.setTransactionSuccessful();
        db.endTransaction();

        Log.d(TAG, "Auto Merged Contact =" + mergedContactsCount + " for"
                + platform.name() + " of Total contacts =" + totalContacts);

        // Sending automatic merge contacts event.
        HashMap<String, Object> properties = new HashMap<String, Object>();

        String from = String.format(
                AnalyticsConstants.MERGED_CONTACTS_FROM_PROPERTY,
                platform.getName());
        properties.put(from, mergedContactsCount);
        properties.put(AnalyticsConstants.TOTAL_CONTACTS_PROPERTY,
                totalContacts);
        new BlinqAnalytics(getContext()).sendEvent(
                AnalyticsConstants.AUTO_MERGED_CONTACTS_EVENT, properties,
                true, AnalyticsConstants.ACTION_CATEGORY);

    }

    /**
     * Get the number of records
     */
    private int getQueryRawCount(SQLiteDatabase db, String query) {
        Cursor countCursor = db.rawQuery(query, null);
        countCursor.moveToFirst();
        int count = countCursor.getInt(0);
        countCursor.close();
        return count;
    }

    /**
     * Insert history of platforms messages using one transaction.
     */
    private int doBulkInsert(Uri uri, ContentValues bulkValues[]) {

        Map<String, ContentValues[]> logs = ContactsManager
                .getContactsLogsHistory(getContext(),
                        PreferencesManager.DEVICE_LOGS_LOADING_PERIOD);

        if (logs.size() == 0)
            return 0;

        int count = Integer.parseInt(uri.getLastPathSegment());

        long startTime = System.currentTimeMillis();

        SQLiteDatabase db = null;
        try {
            db = openHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            return 0;
        }
        int insertedRows = 0;

        // 1st transaction to insert feeds.
        db.beginTransaction();

        for (String contact : logs.keySet()) {
            ContentValues[] valuesArray = logs.get(contact);
            String completeIdentifer = valuesArray[valuesArray.length - 1]
                    .getAsString(MessagesColumns.CONTACT_COMPLETE);
            insertFeed(db, contact, completeIdentifer);
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        // 2nd transaction to insert messages for each inserted feed.
        db.beginTransaction();

        int id = 0;

        for (String contact : logs.keySet()) {

            ContentValues[] valuesArray = logs.get(contact);

            try {
                int length = valuesArray.length;
                id++;
                for (ContentValues values : valuesArray) {

                    ContentValues contentValues = new ContentValues(values);
                    contentValues.remove(Feeds.FEED_IDENTIFIER);
                    contentValues.put(Messages.Feed_ID, id);
                    db.insert(HeadboxDatabaseHelper.TABLE_MESSAGES, null,
                            contentValues);
                }
                insertedRows = insertedRows + length;
            } catch (Exception e) {
                Log.e(TAG, "exception3: " + e.getMessage() + "");
            } finally {
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        long timeTaken = AppUtils.findTime(startTime);
        Log.v(TAG, "Time taken to insert " + insertedRows + " records was "
                + timeTaken + " milliseconds");


        String feedHistoryMode = preferenceManager.getProperty(PreferencesManager.AB_FEED_HISTORY,
                AnalyticsConstants.AB_FEED_HISTORY_FULL);

        boolean isFiveRecordsFeedHistory = feedHistoryMode.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_FIVE);

        if (isFiveRecordsFeedHistory && count != 0 && logs.size() > count) {
            db.delete(HeadboxDatabaseHelper.TABLE_FEEDS, "_id NOT IN (SELECT _id FROM FEEDS where msg_date NOT NULL ORDER BY msg_date DESC LIMIT 5)", null);
            db.delete(HeadboxDatabaseHelper.TABLE_MESSAGES, "feed_id NOT IN (SELECT _id FROM FEEDS where msg_date NOT NULL ORDER BY msg_date DESC LIMIT 5)", null);
        }

        db.rawQuery("UPDATE feeds SET " + Feeds.MODIFIED + "="
                + Feeds.FEED_UNMODIFIED, null);

        preferenceManager.setProperty(PreferencesManager.RECORDS_INSERTED, true);


        return insertedRows;
    }

    /**
     * Insert history of platforms messages using one transaction.
     */
    private int insertLatestLogs(Uri uri, ContentValues bulkValues[]) {

        Map<String, ContentValues[]> logs = ContactsManager
                .getRecentLogsHistory(getContext(),
                        PreferencesManager.RECENT_LOG_COUNT);

        long startTime = System.currentTimeMillis();

        SQLiteDatabase db = null;
        try {
            db = openHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            return 0;
        }
        int insertedRows = 0;

        db.beginTransaction();

        for (String contact : logs.keySet()) {

            ContentValues[] valuesArray = logs.get(contact);
            String identifier = valuesArray[valuesArray.length - 1]
                    .getAsString(Messages.CONTACT_COMPLETE);

            long feedId = getFeedId(db, contact, identifier);

            try {

                String query = "INSERT INTO messages "
                        + "(feed_id,body,date,platform,type,read,id_source,contact,contact_complete) "
                        + " SELECT ?,?,?,?,?,?,?,?,?"
                        + " WHERE NOT EXISTS (SELECT 1 FROM messages WHERE id_source=? AND platform =? )"
                        + " AND NOT EXISTS (SELECT 1 FROM deleted_messages WHERE id_source=? AND platform =? )";

                for (ContentValues values : valuesArray) {

                    Object[] parameters = new Object[]{feedId,
                            values.getAsString(Messages.BODY),
                            values.getAsLong(Messages.DATE),
                            values.getAsInteger(Messages.PLATFORM_ID),
                            values.getAsInteger(Messages.TYPE),
                            values.getAsInteger(Messages.READ),
                            values.getAsString(Messages.SOURCE_ID),
                            values.getAsString(Messages.CONTACT),
                            values.getAsString(Messages.CONTACT_COMPLETE),
                            values.getAsString(Messages.SOURCE_ID),
                            values.getAsString(Messages.PLATFORM_ID),
                            values.getAsString(Messages.SOURCE_ID),
                            values.getAsString(Messages.PLATFORM_ID)};

                    db.execSQL(query, parameters);
                    insertedRows++;
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage() + "");
            } finally {
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        long timeTaken = AppUtils.findTime(startTime);
        Log.v(TAG, "Time taken to insert " + insertedRows + " records was "
                + timeTaken + " milliseconds");

        return insertedRows;
    }

    /**
     * Insert history of certain platform's messages using one transaction.
     */
    private int bulkHistoryInsert(Platform platform,
                                  Map<String, ContentValues[]> messages) {

        long startTime = System.currentTimeMillis();

        // Exist feeds.
        Map<String, FeedModel> feeds = findPlatformFeeds(platform, messages
                .keySet().toArray());

        SQLiteDatabase db = null;
        try {
            db = openHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            return 0;
        }
        int insertedRows = 0;

        long feedId = 0;

        db.beginTransaction();

        try {

            for (String contactId : messages.keySet()) {

                ContentValues[] valuesArray = messages.get(contactId);

                // Insert new feed if we don't have a feed for this
                // conversation.
                if (!feeds.containsKey(contactId)) {
                    feedId = insertFeed(db, contactId);

                } else {
                    feedId = feeds.get(contactId).getFeedId();
                }

                int length = valuesArray.length;
                long lastMessageTime = valuesArray[length - 1].getAsLong(Messages.DATE);
                if (feeds.get(contactId).getUpdatedTime().getTime() == lastMessageTime) {
                    Log.d(TAG, "Already updated feed.");
                    continue;
                }

                for (ContentValues values : valuesArray) {

                    ContentValues contentValues = new ContentValues(values);
                    contentValues.remove(Feeds.FEED_IDENTIFIER);
                    contentValues.put(Messages.Feed_ID, feedId);
                    contentValues.put(Messages.STATUS, Messages.NEW_RECORD);
                    db.insert(HeadboxDatabaseHelper.TABLE_MESSAGES, null,
                            contentValues);
                }

                // Delete old messages.
                String selection = Messages.STATUS + "=? AND "
                        + Messages.PLATFORM_ID + "=? AND " + Messages.Feed_ID
                        + "=?";
                db.delete(
                        HeadboxDatabaseHelper.TABLE_MESSAGES,
                        selection,
                        new String[]{Messages.OLD_RECORD + "",
                                platform.getIdAsString(),
                                String.valueOf(feedId)}
                );

                // Change added messages's status from new to old.
                updateMessageStatus(db, feedId, platform, Messages.OLD_RECORD);
                markFeedAsModified(db, feedId, Feeds.FEED_UNMODIFIED);

                ContentValues feedValues = new ContentValues();
                feedValues.put(Feeds.UPDATED_TIME, lastMessageTime);
                db.update(HeadboxDatabaseHelper.TABLE_FEEDS, feedValues, Feeds._ID + "=" + feedId, null);


                insertedRows = insertedRows + length;

            }
            // Update time stamps and snippet texts to the feeds contain certain
            // platform messages.
            updateAllFeeds(db, Messages.PLATFORM_ID + "=?",
                    new String[]{platform.getId() + ""});

            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

        long timeTaken = AppUtils.findTime(startTime);
        Log.v(TAG, "Time taken to insert " + insertedRows
                + " records of Facebook History was " + timeTaken
                + " milliseconds");
        return insertedRows;
    }

    /**
     * Insert history of certain platform's messages using one transaction.
     */
    private int updateHistory(Platform platform,
                              Map<String, ContentValues[]> messages) {

        SQLiteDatabase db = null;
        try {
            db = openHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            return 0;
        }

        // Exist feeds.
        Map<String, FeedModel> feeds = findPlatformFeeds(platform, messages
                .keySet().toArray());

        List<String> deletedMessages = getPlatformDeletedMessages(db, platform);

        int insertedRows = 0;

        long feedId = 0;

        for (String contactId : messages.keySet()) {

            // Insert new feed if we don't have a feed for this
            // conversation.
            if (!feeds.containsKey(contactId)) {

                feedId = insertFeed(db, contactId);
                FeedModel model = new FeedModel();
                model.setFeedId((int) feedId);
                model.setUpdatedTime(new Date(0));
                feeds.put(contactId, model);
            }
        }

        db.beginTransaction();

        try {

            for (String contactId : messages.keySet()) {

                ContentValues[] valuesArray = messages.get(contactId);

                feedId = feeds.get(contactId).getFeedId();

                int length = valuesArray.length;
                if (length == 0)
                    continue;

                long lastMessageTime = valuesArray[length - 1].getAsLong(Messages.DATE);
                if (feeds.get(contactId).getUpdatedTime().getTime() == lastMessageTime) {
                    continue;
                }

                String query = "INSERT INTO messages "
                        + "(feed_id,body,date,platform,type,read,id_source,contact,contact_complete) "
                        + " SELECT ?,?,?,?,?,?,?,?,?"
                        + " WHERE NOT EXISTS (SELECT 1 FROM messages WHERE id_source=? AND platform ="
                        + platform.getId() + ")";

                for (ContentValues values : valuesArray) {

                    String sourceId = values.getAsString(Messages.SOURCE_ID);

                    if (sourceId != null && !deletedMessages.contains(sourceId)) {

                        Object[] parameters = new Object[]{feedId,
                                values.getAsString(Messages.BODY),
                                values.getAsLong(Messages.DATE),
                                values.getAsInteger(Messages.PLATFORM_ID),
                                values.getAsInteger(Messages.TYPE),
                                values.getAsInteger(Messages.READ),
                                values.getAsString(Messages.SOURCE_ID),
                                values.getAsString(Messages.CONTACT),
                                values.getAsString(Messages.CONTACT_COMPLETE),
                                values.getAsString(Messages.SOURCE_ID)};

                        db.execSQL(query, parameters);

                    }
                }

                updateFeed(db, feedId);

                ContentValues feedValues = new ContentValues();
                feedValues.put(Feeds.UPDATED_TIME, lastMessageTime);
                db.update(HeadboxDatabaseHelper.TABLE_FEEDS, feedValues, Feeds._ID + "=" + feedId, null);

                insertedRows = insertedRows + length;
            }
            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

        db.beginTransaction();

        try {
            String condition = Messages.PLATFORM_ID + "=" + platform.getId()
                    + " AND " + Messages.SOURCE_ID + " IS NULL OR ''";
            db.delete(HeadboxDatabaseHelper.TABLE_MESSAGES, condition, null);
            db.setTransactionSuccessful();
        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

        return insertedRows;
    }

    /**
     * Get list of deleted messages for a certain platform.
     */
    private List<String> getPlatformDeletedMessages(SQLiteDatabase db,
                                                    Platform platform) {

        String selection = Messages.PLATFORM_ID + "=" + platform.getId();
        String[] columns = new String[]{Messages.SOURCE_ID};
        Cursor c = null;

        List<String> deletedMessages = new ArrayList<String>();

        try {
            c = db.query(true, HeadboxDatabaseHelper.TABLE_DELETED_MESSAGES,
                    columns, selection, null, null, null, null, null);

            if (c != null) {

                while (c.moveToNext()) {
                    deletedMessages.add(c.getString(0));
                }
            }

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return deletedMessages;
    }

    /**
     * Change specific feed's message status to old or new.
     */
    private void updateMessageStatus(SQLiteDatabase db, long feedId,
                                     Platform platform, int status) {

        ContentValues values = new ContentValues();
        values.put(Messages.STATUS, status);

        String selection = Messages.Feed_ID + " =? " + " AND "
                + Messages.PLATFORM_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(feedId),
                platform.getIdAsString(),};

        db.update(HeadboxDatabaseHelper.TABLE_MESSAGES, values, selection,
                selectionArgs);
    }

    /**
     * Delete any feed has empty messages.
     */
    private void deleteEmptyFeeds(SQLiteDatabase db) {

        db.delete(
                HeadboxDatabaseHelper.TABLE_FEEDS,
                "_id NOT IN (SELECT DISTINCT feed_id FROM messages where feed_id NOT NULL)",
                null);
    }

    /**
     * Get and sync unknown contacts.
     */
    protected void updateUnknownContacts() {

		/* TODO: It should be generic. */
        String query = "SELECT members_ids FROM feeds WHERE members_ids LIKE '%@facebook.com' "
                + " AND members_ids NOT IN (SELECT headbox_contact_primary_identifier "
                + "FROM headbox_contacts WHERE headbox_contact_type = "
                + Platform.FACEBOOK.getId() + " )";

        Cursor c = openHelper.getReadableDatabase().rawQuery(query, null);
        if (c != null) {
            try {
                ArrayList<String> contacts = new ArrayList<String>(c.getCount());
                while (c.moveToNext()) {
                    contacts.add(c.getString(0));
                }
                if (contacts.size() > 0) {
                    FacebookUtilsService.startSyncContacts(getContext(),
                            Platform.FACEBOOK, contacts);
                }

            } finally {
                c.close();
            }
        }

    }

    /**
     * Fetch device contacts and build headbox phone contacts table.
     */
    private int buildPhoneContactsTable() {

        SQLiteDatabase db = null;

        try {
            db = openHelper.getWritableDatabase();
            // Delete old records.
            db.delete(HeadboxDatabaseHelper.TABLE_PHONE_CONTACTS, null, null);
        } catch (SQLiteException e) {
            return 0;
        }

        int insertedRows = 0;

        // Return a general list of contacts.
        HashMap<String, Contact> contacts = ContactsManager.getAllContacts(getContext());
        final List<Platform> SUPPORTED_PLATFORMS = Arrays.asList(Platform.CALL, Platform.SKYPE, Platform.EMAIL, Platform.WHATSAPP);
        List<Contact> imContacts = ContactsManager.getContacts(getContext(), SUPPORTED_PLATFORMS);

        db.beginTransaction();

        if (imContacts.size() > 0)
            for (Contact contact : imContacts) {

                try {
                    String suffix = "";
                    if (contact.getContactType() == Platform.SKYPE) {
                        suffix = StringUtils.SKYPE_SUFFIX;
                    }

                    ContentValues values = new ContentValues();
                    values.put(PhoneContacts.ID, contact.getContactId());
                    values.put(PhoneContacts.PHONE_CONTACT_COMPLETE_IDENTIFIER,
                            contact.getIdentifier() + suffix);
                    values.put(PhoneContacts.PHONE_CONTACT_IDENTIFIER,
                            contact.getNormalizedIdentifier() + suffix);

                    if (contacts.get(contact.getContactId()) != null) {
                        values.put(PhoneContacts.PHONE_CONTACT_NAME, contacts
                                .get(contact.getContactId()).getName());
                    } else {
                        values.put(PhoneContacts.PHONE_CONTACT_NAME,
                                contact.getName());
                    }

                    if (contacts.get(contact.getContactId()) != null)
                        values.put(PhoneContacts.PHONE_PICTURE_URI, contacts
                                .get(contact.getContactId()).getPhotoUri()
                                .toString());

                    values.put(PhoneContacts.PHONE_CONTACT_TYPE, contact
                            .getContactType().getId());
                    db.insert(HeadboxDatabaseHelper.TABLE_PHONE_CONTACTS, null,
                            values);

                    insertedRows++;

                } catch (Exception e) {
                    Log.d(TAG, "Error while getting contact");
                }
            }

        db.setTransactionSuccessful();
        db.endTransaction();

        // Handle and delete duplicate records.
        db.execSQL(HeadboxDBQueries.DELETE_DUPLICATE_CONTACTS);
        db.execSQL(HeadboxDBQueries.DELETE_DUPLICATE_CONTACTS2);

        return insertedRows;
    }

    /**
     * Get certain feed messages.
     */
    private Cursor getFeedMessages(String feedId, String[] projection,
                                   String selection, String sortOrder) {

        try {
            Long.parseLong(feedId);
        } catch (NumberFormatException e) {
            Log.d(TAG, "getFeedMessages: feed id must be long.");
            return null;
        }
        String filterSelection = MessagesColumns.Feed_ID + " = " + feedId
                + " AND " + MessagesColumns.STATUS + "="
                + MessagesColumns.OLD_RECORD;
        String finalSelection = DatabaseUtils.concatenateWhere(selection,
                filterSelection);

        String finalQuery = buildMessageTableQuery(projection, finalSelection,
                sortOrder);
        return openHelper.getReadableDatabase().rawQuery(finalQuery,
                new String[0]);
    }

    /**
     * Return feed details using the feed id.
     */
    private Cursor getFeedById(String feedId) {

        try {
            Long.parseLong(feedId);
        } catch (NumberFormatException e) {
            Log.d(TAG, "getFeedById: feed id must be long.");
            return null;
        }

        SQLiteDatabase db = null;
        try {
            db = openHelper.getReadableDatabase();
        } catch (SQLiteException e) {
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(HeadboxDBQueries.FEEDS_JOIN_CONTACTS);
        qb.setProjectionMap(sFeedsProjection);
        qb.appendWhere(Feeds._ID + "=" + feedId);
        Cursor cursor = qb.query(db, null, null, null, null, null, null);

        return cursor;
    }

    private Cursor getFeeds(String[] projection, String selection,
                            String[] selectionArgs, String sortOrder) {

        SQLiteDatabase db = null;
        try {
            db = openHelper.getReadableDatabase();
        } catch (SQLiteException e) {
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(HeadboxDBQueries.FEEDS_JOIN_CONTACTS);
        qb.setProjectionMap(sFeedsProjection);
        String groupBy = Feeds._ID;
        String where = DatabaseUtils.concatenateWhere(selection,
                Feeds.DELETED + "!=" + Feeds.FEED_DELETED);

        Cursor cursor = qb
                .query(db, null, where, null, groupBy, null, sortOrder);

        return cursor;
    }

    /**
     * Return handled and normalized query for fetching messages from messages
     * table.
     */
    private static String buildMessageTableQuery(String[] projection,
                                                 String selection, String sortOrder) {

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setDistinct(true);
        builder.setTables(HeadboxDatabaseHelper.TABLE_MESSAGES);
        String[] messageColumns = handleNullProjectionForMessages(projection);
        String messageSortOrder = handleNullSortOrderMessage(sortOrder);
        selection = DatabaseUtils.concatenateWhere(selection,
                HeadboxDBQueries.IGNORE_DELETED_MESSAGE_CONDITION);
        String query = builder.buildQuery(messageColumns, selection, null,
                null, messageSortOrder, null);

        return query;
    }

    private static String handleNullSortOrderMessage(String sortOrder) {
        return sortOrder == null ? Messages.DATE + " DESC " : sortOrder;
    }

    /**
     * To apply Default projection if we have null projection for the Message
     * table.
     */
    private static String[] handleNullProjectionForMessages(String[] projection) {
        return projection == null ? MESSAGES_COLUMNS : projection;
    }

    /**
     * Get the feed id or generate new in case of there is no feed associated
     * with this contact.
     */
    private long getFeedId(SQLiteDatabase db, String contact, String phoneNumber) {

        long feedId = findFeedId(db, contact);

        if (feedId != -1) {
            return feedId;
        }
        feedId = insertFeed(db, contact, phoneNumber);
        return feedId;
    }

    private long getFeedId(SQLiteDatabase db, Platform platform,
                           ContentValues values) {

        if (values == null) {
            return 0;
        }

        String contactName = null;
        if (values.containsKey(HeadboxContacts.NAME)) {
            contactName = values.getAsString(HeadboxContacts.NAME);
        } else if (values.containsKey(FacebookContacts.NAME)) {
            contactName = values.getAsString(FacebookContacts.NAME);
        }

        if (StringUtils.isBlank(contactName)) {
            return 0;
        }

        Log.d(TAG, "Finding feed for contact name= " + contactName);

        String query = "SELECT feeds._id,headbox_contacts.headbox_contact_primary_identifier FROM feeds JOIN headbox_contacts ON "
                + "Headbox_contacts.headbox_contact_primary_identifier = feeds.members_ids"
                + " WHERE "
                + "(phone_contact_id IN "
                + "(SELECT phone_contact_id FROM headbox_contacts "
                + "WHERE (headbox_contact_name LIKE %s OR headbox_contact_primary_identifier LIKE %s) AND headbox_contact_type = %s ))"
                + " OR "
                + "( headbox_contacts.headbox_contact_name LIKE %s "
                + " AND "
                + "feeds._id IN (SELECT feed_id FROM messages WHERE platform = %s ))";

        contactName = contactName.replaceAll("'", "''");

        String filter = "'" + contactName.trim() + "'";
        String findFeedQuery = String.format(query, filter, filter,
                platform.getIdAsString(), filter, platform.getIdAsString());
        Cursor cursor = db.rawQuery(findFeedQuery, null);

        long feedId = 0;
        String contact = null;

        if (cursor != null) {
            try {
                cursor.moveToFirst();
                feedId = cursor.getLong(cursor.getColumnIndex(Feeds._ID));
                Log.d(TAG, "* Found Feed id = " + feedId);
                contact = cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.PRIMARY_INDENTIFIER));
                values.put(Messages.CONTACT, contact);
            } catch (Exception e) {
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        if (feedId == 0) {

            String[] columns = new String[]{
                    HeadboxContacts.COMPLETE_INDENTIFIER,
                    HeadboxContacts.PRIMARY_INDENTIFIER,
                    HeadboxContacts.CONTACT_TYPE};
            String selection = HeadboxContacts.NAME + " LIKE " + filter;
            cursor = db.query(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                    columns, selection, null, null, null, null);

            if (cursor != null) {

                try {

                    String contactIdentifier = null;
                    boolean foundContactPlatform = false;
                    while (cursor.moveToNext()) {

                        Platform contactType = Platform
                                .fromId(cursor.getInt(cursor
                                        .getColumnIndex(HeadboxContacts.CONTACT_TYPE)));

                        contactIdentifier = cursor
                                .getString(cursor
                                        .getColumnIndex(HeadboxContacts.PRIMARY_INDENTIFIER));

                        if (contactType == platform || (
                                platform == Platform.SMS && contactType == Platform.CALL)) {
                            foundContactPlatform = true;
                            break;
                        }
                    }

                    if (!foundContactPlatform) {
                        return 0;
                    }
                    values.put(Messages.CONTACT, contactIdentifier);
                    feedId = insertFeed(db, contactIdentifier);
                    if (feedId != 0) {
                        Log.d(TAG, "*** Found Feed id = " + feedId);
                    }
                } catch (Exception e) {
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            }

        }

        return feedId;
    }

    /**
     * Add a record for a new Feed.
     *
     * @param memberId - list contains Ids of the feed members.
     */
    private long insertFeed(SQLiteDatabase db, String memberId,
                            String identifier) {

        if (memberId == null) {
            return 0;
        }

        ContentValues values = new ContentValues();
        long date = System.currentTimeMillis();
        values.put(Feeds.DATE, date - date % 1000);
        values.put(Feeds.MEMBERS_IDS, memberId);
        values.put(Feeds.MESSAGE_COUNT, 0);
        values.put(Feeds.FEED_IDENTIFIER, identifier);
        values.put(Feeds.TYPE, Feeds.TYPE_SINGLE);

        long result = db
                .insert(HeadboxDatabaseHelper.TABLE_FEEDS, null, values);
        return result;
    }

    private long insertFeed(SQLiteDatabase db, String identifier) {

        if (identifier == null) {
            return 0;
        }

        ContentValues values = new ContentValues();
        long date = System.currentTimeMillis();
        values.put(Feeds.DATE, date - date % 1000);
        values.put(Feeds.MEMBERS_IDS, identifier);
        values.put(Feeds.MESSAGE_COUNT, 0);
        values.put(Feeds.FEED_IDENTIFIER, identifier);
        values.put(Feeds.TYPE, Feeds.TYPE_SINGLE);

        long result = db
                .insert(HeadboxDatabaseHelper.TABLE_FEEDS, null, values);
        Log.d(TAG, "created feed feed id=" + result);

        return result;
    }

    /**
     * Return an id for feed record associated with contact identifier or -1 if
     * not exist.
     * TODO: Needs refactoring.
     */
    private long findFeedId(SQLiteDatabase db, String contactIdentifier) {
        long feedId = -1;


        // Method - 1
        // Check if a feed already exists by querying messages table using
        // contact_identifier:
        // could be Facebook ID,Google Id, or Phone Number.

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        String selection = Messages.CONTACT + " LIKE ? ";
        String[] selectionArgs = new String[]{contactIdentifier};
        String limit = "1";
        String[] projection = new String[]{Messages.Feed_ID};
        queryBuilder.setTables(HeadboxDatabaseHelper.TABLE_MESSAGES);
        queryBuilder.setDistinct(true);

        Cursor cursor = null;
        try {

            cursor = queryBuilder.query(openHelper.getReadableDatabase(),
                    projection, selection, selectionArgs, null, null, null, limit);

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                feedId = cursor.getLong(cursor.getColumnIndex(Messages.Feed_ID));
                cursor.close();
                return feedId;
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        // Method - 2

        // check if there is a feed record associated with contact
        // identifier.
        queryBuilder = new SQLiteQueryBuilder();
        selection = Feeds.MEMBERS_IDS + "  LIKE ? ";
        selectionArgs = new String[]{contactIdentifier};
        projection = new String[]{Feeds._ID};
        queryBuilder.setTables(HeadboxDatabaseHelper.TABLE_FEEDS);
        queryBuilder.setDistinct(true);

        try {

            cursor = queryBuilder.query(db, projection, selection,
                    selectionArgs, null, null, null, null);

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                feedId = cursor.getLong(cursor.getColumnIndex(Feeds._ID));
                cursor.close();
                return feedId;
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        // Method - 3

        // If a feed doesn't exist then look for contact ID from the feed
        // and bring the contact ID for this user and then Check if there is
        // a feed with this ID.

        try {

            String query = String.format(
                    HeadboxDBQueries.GET_FEED_FOR_INCOMING_MESSAGE_QUERY,
                    contactIdentifier);
            cursor = db.rawQuery(query, null);

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                feedId = cursor.getLong(cursor.getColumnIndex(Feeds._ID));
                cursor.close();
                return feedId;
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        return feedId;
    }

    /**
     * Correct and merge separated feeds by any unknown reason.
     */
    private void correctDuplicateFeeds() {

        // Get duplicate feeds
        String query = "SELECT DISTINCT M1.feed_id, M2.feed_id, M1.contact FROM messages M1 JOIN messages M2 "
                + "ON M1.contact=M2.contact AND M1.feed_id > M2.feed_id";

        SQLiteDatabase db = openHelper.getWritableDatabase();

        try {

            Cursor c = db.rawQuery(query, null);
            db.beginTransaction();

            if (c != null && c.getCount() > 0) {

                Log.d(TAG, "Correct duplicate feeds");
                try {
                    while (c.moveToNext()) {

                        // Move messages from newest[duplicate] feed to the
                        // oldest one.
                        ContentValues values = new ContentValues();
                        values.put(Messages.Feed_ID, c.getInt(1));
                        String where = Messages.Feed_ID + "=?";
                        String[] whereArgs = new String[]{c.getString(0)};
                        db.update(HeadboxDatabaseHelper.TABLE_MESSAGES, values,
                                where, whereArgs);
                        updateFeed(db, c.getInt(0));
                        updateFeed(db, c.getInt(1));
                    }
                } finally {
                    c.close();
                }
            }
            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            try {
                db.endTransaction();
            } catch (Throwable ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
        }
    }

    /**
     * Returns a Map with a contactId and the related feed.
     */
    private Map<String, FeedModel> findPlatformFeeds(Platform platform,
                                                     Object[] contactIdentifiers) {

        String query = null;
        if (platform == Platform.FACEBOOK)
            // TODO: Generalize this query.
            query = HeadboxDBQueries.GET_FEEDS_FOR_FACEBOOK_MESSAGE_QUERY;
        else {
            return null;
        }

        // The key of the map is contact id ,
        // the content of the map is the feed id.
        Map<String, FeedModel> feeds = new HashMap<String, FeedModel>();
        // query = "select feeds._id from feeds";
        Cursor cursor = openHelper.getReadableDatabase().rawQuery(query, null);

        if (cursor.getCount() > 0) {

            int identifierIndex = cursor
                    .getColumnIndex(HeadboxContacts.PRIMARY_INDENTIFIER);
            int feedId = cursor.getColumnIndex(Feeds._ID);
            int updatedTime = cursor.getColumnIndex(Feeds.UPDATED_TIME);
            cursor.moveToFirst();

            do {

                String identifier = cursor.getString(identifierIndex);
                FeedModel model = new FeedModel();
                model.setFeedId(cursor.getInt(feedId));
                model.setUpdatedTime(new Date(cursor.getLong(updatedTime)));
                feeds.put(identifier, model);

            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
        return feeds;
    }

    /**
     * Returns the id of the last Headbox contacts.
     */
    private int getLastContactID(SQLiteDatabase db) {

        String query = "SELECT MAX(phone_contact_id) AS phone_contact_id FROM headbox_contacts";
        Cursor cursor = db.rawQuery(query, null);
        int id = 0;

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int contactIdColumn = cursor
                    .getColumnIndex(HeadboxContacts.CONTACT_ID);
            id = cursor.getInt(contactIdColumn);
        }
        if (cursor != null)
            cursor.close();

        return id;
    }

    private long insertContact(Uri uri, ContentValues values) {


        Platform platform = Platform.fromId(Integer.parseInt(uri.getPathSegments().get(1)));
        String table = "";

        if (platform == Platform.CALL)
            table = HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS;
        else {
            table = getTableName(platform);
        }

        SQLiteDatabase db = openHelper.getWritableDatabase();
        long id = 0;
        String query = "";
        String selection = "";
        db.beginTransaction();
        try {

            long contactId = db.insert(table, null, values);

            // Need more improvements.
            switch (platform) {
                case FACEBOOK:
                    query = HeadboxDBQueries.FACEBOOK_CONTACTS_MATCHING_ON_FULL_NAME;
                    selection = " WHERE " + FacebookContacts.ID + "=" + contactId;
                    db.execSQL(query + selection);
                    break;
                case HANGOUTS:
                    query = HeadboxDBQueries.GOOGLE_CONTACTS_MATCHING_ON_FULL_NAME;
                    selection = " WHERE "
                            + HeadboxDatabaseHelper.TABLE_GOOGLE_CONTACTS + "."
                            + GoogleContacts.ID + "=" + contactId;
                    db.execSQL(query + selection);
                    break;
                case TWITTER:
                case INSTAGRAM:
                case STATICINFO:
                    db.insert(HeadboxDatabaseHelper.TABLE_PLATFORMS_CONTACTS, null, values);
                    query = HeadboxDBQueries.CONTACTS_MATCHING_ON_FULL_NAME;
                    selection = " AND "
                            + HeadboxDatabaseHelper.TABLE_PLATFORMS_CONTACTS + "."
                            + HeadboxFeed.PlatformContacts.ID + "=" + contactId;
                    db.execSQL(query + selection, new String[]{platform.getIdAsString(),
                            platform.getIdAsString()});
                    break;
                default:
                    break;
            }
            updateEmptyIDContacts(db);
            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

        id = getLastInsertedRowId(db, HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, HeadboxContacts.CONTACT_ID);

        return id;
    }

    private long getLastInsertedRowId(SQLiteDatabase db, String table, String column) {

        int id = 0;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT " + column + " FROM " + table + " ORDER BY ROWID DESC LIMIT 1", null);
            c.moveToNext();
            id = c.getInt(0);
        } finally {
            c.close();
        }
        return id;
    }

    /**
     * Update certain contact.
     */
    private int updateContact(SQLiteDatabase db, Platform contactPlatform,
                              ContentValues values, String selection, String[] selectionArgs) {

        String table = getTableName(contactPlatform);
        int affectedRows = db.update(table, values, selection, selectionArgs);
        Log.d(TAG, "# of updated " + contactPlatform.getName() + " contacts ="
                + affectedRows);
        return affectedRows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int affectedRows = 0;
        int update = 1;
        String table = HeadboxDatabaseHelper.TABLE_MESSAGES;
        String extendedSelection = null;
        SQLiteDatabase db = openHelper.getWritableDatabase();

        int j = URIMatcher.match(uri);
        switch (URIMatcher.match(uri)) {
            case URI_FEED_MESSAGES:

                String feedId = uri.getPathSegments().get(1);

                try {
                    Integer.parseInt(feedId);
                } catch (Exception ex) {
                    Log.e(TAG, "Error in conversion " + feedId);
                    break;
                }
                extendedSelection = Messages.Feed_ID + "=" + feedId;
                break;
            case URI_MESSAGE_ID:
                String messageId = uri.getPathSegments().get(1);
                extendedSelection = null;
                break;
            case URI_FEEDS:
                table = HeadboxDatabaseHelper.TABLE_FEEDS;
                break;
            case URI_FEEDS_MERGE:
                String feed = uri.getPathSegments().get(1);
                //synchronized (synchronizeRef) {
                    mergeFeeds(db, values, feed);
                //}
                update = 0;
                break;
            case URI_CONTACTS_MERGE:
                String contactId = uri.getPathSegments().get(1);
                // feed to be updated after doing contact merging.
                String mergedFeed = uri.getPathSegments().get(2);
                Log.d(TAG, "FEED = " + mergedFeed + " contact = " + contactId);
                //synchronized (synchronizeRef) {
                    mergeContacts(db, values, contactId);
                    markFeedAsModified(db, Long.valueOf(mergedFeed),
                            Feeds.FEED_MODIFIED);
                //}
                update = 0;
                break;
            case URI_CONTACTS_UNMERGE:
                long updatedFeed = Long.valueOf(uri.getPathSegments().get(1));
                String[] contactIdentifiers = selectionArgs;
                update = 0;
                updateMergeLinks(db, updatedFeed, contactIdentifiers);
                break;
            case URI_GENERAL_CONTACTS:
                Platform platform = Platform.fromId(Integer.parseInt(uri
                        .getPathSegments().get(1)));
                affectedRows = updateContact(db, platform, values, selection, selectionArgs);
                update = 0;
                break;
            case URI_UNDO_FEED_MERGE:
                affectedRows = db.update(HeadboxDatabaseHelper.TABLE_FEEDS, values,
                        selection, null);
                update = 0;
                break;
            case URI_UPDATE_TOP_FRIENDS_NOTIFICATIONS:
                affectedRows = updateTopFriendsNotifications(db);
                update = 0;
                break;
            case URI_UPDATE_NOTIFICATION_FOR_CONTACT:
                String contactIdentifier = String.valueOf(uri.getPathSegments().get(1));
                affectedRows = updateNotificationForContact(db, contactIdentifier);
                update = 0;
                break;
            case URI_UPDATE_CONTACT_NAME_FROM_SERVER:
                update = updateContactNameFromServerResponse(db, uri.getPathSegments().get(1), selectionArgs);
                break;
            default:
                break;

        }

        if (update == 1) {
            // Concatenate two WHERE clauses.
            selection = DatabaseUtils.concatenateWhere(selection,
                    extendedSelection);

            affectedRows = db.update(table, values, selection, selectionArgs);
            if (affectedRows > 0) {
                Log.d(TAG, "Update " + uri + " succeeded");
            }
        }
        return affectedRows;
    }

    private int updateContactNameFromServerResponse(SQLiteDatabase db, String feedId, String[] nameAndPhoto) {
        ContentValues values = new ContentValues();
        String name = nameAndPhoto[0];
        String photo = nameAndPhoto[1];
        if (photo == null && name == null) {
            return 0;
        }
        if (name != null) {
            values.put(HeadboxContacts.NAME, name);
        }
        if (photo != null) {
            values.put(HeadboxContacts.PICTURE_URL, photo);
        }

        Cursor c = db.rawQuery("SELECT " + Feeds.MEMBERS_IDS + " FROM " +
                HeadboxDatabaseHelper.TABLE_FEEDS + " WHERE " + Feeds._ID + "=" + feedId, null);
        if (c != null) {
            try {
                c.moveToFirst();
                String identifier = c.getString(0);
                if (!HeadboxPhoneUtils.isPhoneNumber(identifier)) {
                    return 0;
                }
                db.update(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, values,
                        HeadboxContacts.PRIMARY_INDENTIFIER + "=" + identifier, null);
                Log.d(TAG, "Updated feed " + feedId + " contact name from " + identifier + " to " + name);
            } finally {
                c.close();
            }
        }
        return 0;
    }

    private void mergeContacts(SQLiteDatabase db, ContentValues values,
                               String contactId) {

        PreferencesManager preferencesManager = new PreferencesManager(
                getContext());
        // save the contacts of merge
        String mergedContactId = values.getAsString(HeadboxContacts.CONTACT_ID);

        preferencesManager.setProperty(PreferencesManager.MERGED_CONTACT_ID,
                contactId);
        preferencesManager.setProperty(PreferencesManager.FEED_CONTACT_ID,
                mergedContactId);

        HashMap<String, String> feedHashMap = new HashMap<String, String>();
        db.execSQL(HeadboxDBQueries.ADD_MERGE_LINKS_QUERY,
                new String[]{mergedContactId, contactId,
                        BlinqApplication.searchSource.getId() + ""}
        );

        Cursor c = db
                .rawQuery(
                        "SELECT _id,members_ids FROM feeds WHERE members_ids "
                                + "IN (SELECT headbox_contact_primary_identifier FROM headbox_contacts "
                                + "WHERE phone_contact_id =? AND headbox_contact_type = ?)",
                        new String[]{contactId,
                                BlinqApplication.searchSource.getId() + ""}
                );

        if (c != null) {
            try {
                while (c.moveToNext()) {

                    String query = "UPDATE feeds "
                            + "SET members_ids = "
                            + "(SELECT headbox_contact_primary_identifier FROM headbox_contacts "
                            + "WHERE phone_contact_id =? and headbox_contact_type != ? LIMIT 1) "
                            + "WHERE _id = ?";

                    db.execSQL(query,
                            new String[]{
                                    contactId,
                                    BlinqApplication.searchSource.getId()
                                            + "", c.getString(0)}
                    );

                    feedHashMap.put(c.getString(0), c.getString(1));
                }
            } finally {
                c.close();
            }
        }

        db.update(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, values,
                HeadboxContacts.CONTACT_ID + "=" + contactId + " AND "
                        + HeadboxContacts.CONTACT_TYPE + "="
                        + BlinqApplication.searchSource.getId(), null
        );

        // save the Hashmap of updated feeds
        preferencesManager.setProperty(PreferencesManager.MERGED_FEEDS,
                new Gson().toJson(feedHashMap));
        preferencesManager.setProperty(PreferencesManager.SEARCH_SOURCE,
                new Gson().toJson(BlinqApplication.searchSource));
        preferencesManager.setProperty(PreferencesManager.IS_SHOW_UNDO_MERGE, true);

    }

    private void updateMergeLinks(SQLiteDatabase db, long updatedFeedId,
                                  String[] contactIdentifiers) {

        db.beginTransaction();
        try {

            // Unlink contact - Update contact ID.

            ContentValues values = new ContentValues();
            values.put(HeadboxContacts.CONTACT_ID, getLastContactID(db) + 1);
            int result = db
                    .update(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS,
                            values,
                            HeadboxContacts.PRIMARY_INDENTIFIER
                                    + " IN ("
                                    + StringUtils
                                    .convertStringsForINQuery(contactIdentifiers)
                                    + ")", null
                    );

            // Insert new feed.
            long feedId = insertFeed(db, contactIdentifiers[0]);

            // Move unlinked contact's messages to new feed.
            ContentValues feedValues = new ContentValues();
            feedValues.put(Messages.Feed_ID, feedId);

            String selection = null;
            String[] selectionArgs = null;

            if (BlinqApplication.searchSource == Platform.CALL
                    || BlinqApplication.searchSource == Platform.SMS) {

                selection = Messages.Feed_ID + "=?" + " AND ("
                        + Messages.PLATFORM_ID + "=?" + " OR "
                        + Messages.PLATFORM_ID + "=? )";
                selectionArgs = new String[]{updatedFeedId + "",
                        Platform.CALL.getId() + "", Platform.SMS.getId() + ""};

            } else {

                selection = Messages.Feed_ID + "=?" + " AND "
                        + Messages.PLATFORM_ID + "=?";
                selectionArgs = new String[]{updatedFeedId + "",
                        BlinqApplication.searchSource.getId() + ""};

            }

            db.update(HeadboxDatabaseHelper.TABLE_MESSAGES, feedValues,
                    selection, selectionArgs);

            // Update feed's snippet text,time,last platform..
            updateFeed(db, feedId);
            updateFeed(db, updatedFeedId);
            updateFeedIdentifier(db, updatedFeedId, contactsForMerge.get(0));

            // remove empty feeds.
            db.delete(
                    HeadboxDatabaseHelper.TABLE_FEEDS,
                    "_id NOT IN (SELECT DISTINCT feed_id FROM messages where feed_id NOT NULL) AND _id !=?",
                    new String[]{updatedFeedId + ""});

            for (String identifier : contactIdentifiers) {

                // Delete old merge links.

                db.delete(HeadboxDatabaseHelper.TABLE_MERGE_LINKS,
                        MergeLinks.FIRST_CONTACT_INDENTIFIER + "=?" + " OR "
                                + MergeLinks.SECOND_CONTACT_INDENTIFIER + "=?",
                        new String[]{identifier, identifier}
                );

                // Add old merge to the broken list.

                ContentValues val = new ContentValues();
                val.put(MergeLinks.FIRST_CONTACT_INDENTIFIER, identifier);
                val.put(MergeLinks.FIRST_CONTACT_TYPE,
                        BlinqApplication.searchSource.getId());
                val.put(MergeLinks.TYPE, MergeLinks.TYPE_BREAK);

                db.insert(HeadboxDatabaseHelper.TABLE_MERGE_LINKS, null, val);

            }

            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }
    }

    private void updateFeedIdentifier(SQLiteDatabase db, long feedId,
                                      String identifier) {
        ContentValues values = new ContentValues();
        values.put(Feeds.MEMBERS_IDS, identifier);
        String where = Feeds._ID + "=?";
        String[] whereArgs = new String[]{feedId + ""};
        db.update(HeadboxDatabaseHelper.TABLE_FEEDS, values, where, whereArgs);
    }

    /**
     * To rebuild merge links after updating contacts.
     */
    private void linkContacts() {

        SQLiteDatabase db = openHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            breakOldMergingLinks(db);

            Cursor c = db.query(HeadboxDatabaseHelper.TABLE_MERGE_LINKS, null,
                    MergeLinks.TYPE + "=?", new String[]{MergeLinks.TYPE_LINK
                            + ""}, null, null,
                    MergeLinks.FIRST_CONTACT_INDENTIFIER, null
            );

            if (c != null) {
                try {
                    while (c.moveToNext()) {

                        String query = "UPDATE headbox_contacts "
                                + "SET phone_contact_id = "
                                + "(SELECT phone_contact_id FROM headbox_contacts WHERE headbox_contact_primary_identifier = ? ) "
                                + "WHERE headbox_contact_primary_identifier = ?";

                        db.execSQL(query,
                                new String[]{c.getString(1), c.getString(3)});
                    }
                } finally {
                    c.close();
                }
            }

            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

    }

    private void breakOldMergingLinks(SQLiteDatabase db) {

        db.beginTransaction();
        try {

            Cursor c = db.query(HeadboxDatabaseHelper.TABLE_MERGE_LINKS, null,
                    MergeLinks.TYPE + "=" + MergeLinks.TYPE_BREAK, null, null,
                    null, MergeLinks.FIRST_CONTACT_INDENTIFIER, null);

            int lastContactId = getLastContactID(db);

            if (c != null) {
                try {
                    int id = 1;
                    while (c.moveToNext()) {

                        String query = "UPDATE headbox_contacts "
                                + "SET phone_contact_id =?  "
                                + "WHERE headbox_contact_primary_identifier = ?";

                        db.execSQL(query, new String[]{
                                (lastContactId + id++) + "", c.getString(1)});
                    }
                } finally {
                    c.close();
                }
            }

            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

    }

    private void mergeFeeds(SQLiteDatabase db, ContentValues values,
                            String feedId) {

        PreferencesManager preferencesManager = new PreferencesManager(
                getContext());
        preferencesManager.setProperty(PreferencesManager.MERGED_CONTACT_FEED,
                feedId);

        if (feedId == null || feedId == "") {
            return;
        }
        db.beginTransaction();
        try {

            // Set feed id to the new feed.
            int mergedFeedID = values.getAsInteger(Messages.Feed_ID);
            preferencesManager.setProperty(PreferencesManager.CONTACT_FEED,
                    String.valueOf(mergedFeedID));
            String moveMessagesQuery = "UPDATE "
                    + HeadboxDatabaseHelper.TABLE_MESSAGES + " SET "
                    + Messages.Feed_ID + "=" + mergedFeedID + " WHERE "
                    + Messages.Feed_ID + "=" + feedId + " AND "
                    + Messages.PLATFORM_ID + "="
                    + BlinqApplication.searchSource.getId();

            if (BlinqApplication.searchSource == Platform.CALL) {

                moveMessagesQuery = "UPDATE "
                        + HeadboxDatabaseHelper.TABLE_MESSAGES + " SET "
                        + Messages.Feed_ID + "=" + mergedFeedID + " WHERE "
                        + Messages.Feed_ID + "=" + feedId + " AND ("
                        + Messages.PLATFORM_ID + "=" + Platform.CALL.getId()
                        + " OR " + Messages.PLATFORM_ID + "="
                        + Platform.SMS.getId() + ")";

            }
            db.execSQL(moveMessagesQuery);

            // Do update process to the "merged" feed.
            updateFeed(db, mergedFeedID);
            updateFeed(db, Long.valueOf(feedId));
            markFeedAsModified(db, Long.valueOf(feedId), Feeds.FEED_MODIFIED);
            markFeedAsModified(db, mergedFeedID, Feeds.FEED_MODIFIED);
            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Update feed details
     *
     * @param db - database instance.
     */
    public static void updateFeed(SQLiteDatabase db, long feedId) {

        if (feedId < 0) {
            return;
        }

        db.beginTransaction();
        try {

            // Update the message count in feeds table as the count
            // of all messages on messages table.
            db.execSQL(" UPDATE feeds SET message_count = "
                    + " (SELECT COUNT(messages._id) FROM messages LEFT JOIN feeds "
                    + " ON feeds._id = " + Messages.Feed_ID + " WHERE "
                    + Messages.Feed_ID + " = " + feedId + " AND messages."
                    + Messages.TYPE + " !=" + MessageType.DRAFT.getId() + ")"
                    + " WHERE feeds._id = " + feedId + ";");

            // Update feed's date,snippet,and last platform in
            // feeds table to be that of the most recent message in
            // the feed.
            db.execSQL(" UPDATE feeds" + " SET" + " msg_date ="
                    + " (SELECT date FROM"
                    + " (SELECT date, feed_id FROM messages)"
                    + " WHERE feed_id = "
                    + feedId
                    + " ORDER BY date DESC LIMIT 1),"
                    + " snippet ="
                    + " (SELECT snippet FROM"
                    + " (SELECT date, body AS snippet, feed_id FROM messages)"
                    + " WHERE feed_id = "
                    + feedId
                    + " ORDER BY date DESC LIMIT 1),"
                    + " last_platform_id ="
                    + " (SELECT platform FROM"
                    + " (SELECT date, platform, feed_id FROM messages)"
                    + " WHERE feed_id = "
                    + feedId
                    + " ORDER BY date DESC LIMIT 1),"
                    + " last_call_type ="
                    + " (SELECT type FROM"
                    + " (SELECT date, platform,type, feed_id FROM messages)"
                    + " WHERE feed_id = "
                    + feedId
                    + " AND platform = "
                    + Platform.CALL.getId()
                    + " ORDER BY date DESC LIMIT 1)"
                    + " WHERE feeds._id = " + feedId + ";");

            // Update feed_platforms table.
            db.delete(HeadboxDatabaseHelper.TABLE_FEED_PLATFORMS,
                    FeedPlatformsColumns.Feed_ID + "=?", new String[]{feedId
                            + ""}
            );

            db.execSQL(HeadboxDBQueries.UPDATE_FEED_PLATFORM_TABLE,
                    new String[]{feedId + "", feedId + ""});

            db.setTransactionSuccessful();

        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Update all feeds
     *
     * @param db        - database instance
     * @param where     - selection statement.
     * @param whereArgs - selection arguments.
     */
    private void updateAllFeeds(SQLiteDatabase db, String where,
                                String[] whereArgs) {

        db.beginTransaction();
        try {
            if (where == null) {
                where = "";
            } else {
                where = "WHERE (" + where + ")";
            }
            String query = "SELECT _id FROM feeds WHERE _id IN "
                    + "(SELECT DISTINCT feed_id FROM messages " + where + ")";
            Cursor c = db.rawQuery(query, whereArgs);
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        updateFeed(db, c.getInt(0));
                    }
                    deleteEmptyFeeds(db);
                } finally {
                    c.close();
                }
            }

            db.setTransactionSuccessful();
        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SQLiteDatabase db = openHelper.getWritableDatabase();
        int affectedRows = 0;
        String extendedSelection = null;
        long feedId = 0;

        switch (URIMatcher.match(uri)) {
            case URI_FEED_MESSAGES:

                try {
                    feedId = Long.parseLong(uri.getPathSegments().get(1));
                } catch (Exception ex) {
                    Log.e(TAG, "error in conversion " + feedId);
                    break;
                }

                extendedSelection = Messages.Feed_ID + "=" + feedId;
                selection = DatabaseUtils.concatenateWhere(selection,
                        extendedSelection);
                affectedRows = db.delete(HeadboxDatabaseHelper.TABLE_MESSAGES,
                        selection, selectionArgs);
                updateFeedAfterDelete(db, feedId);

                break;

            case URI_CONTACT:
                affectedRows = db
                        .delete(HeadboxDatabaseHelper.TABLE_PHONE_CONTACTS,
                                selection, null);
                affectedRows = db.delete(
                        HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, selection,
                        null);
                break;
            case URI_MESSAGE_SOURCE_ID:

                affectedRows = deleteMesssage(db, uri, selection, selectionArgs);
                break;
            case URI_ALL_MESSAGES_DELETE:

                affectedRows = deleteAllPlatformMessages(db, uri, selection,
                        selectionArgs);
                break;
            case URI_DELETE_MERGE_LINK:

                affectedRows = deleteContactsMergeLink(db, uri);
                break;
            case URI_FEED_ID:
                try {
                    feedId = Long.parseLong(uri.getPathSegments().get(1));
                } catch (Exception ex) {
                    Log.e(TAG, "error in conversion " + feedId);
                    break;
                }
                affectedRows = markFeedAsDeleted(db, feedId);
                break;
            default:
                break;
        }
        return affectedRows;

    }

    private int markFeedAsDeleted(SQLiteDatabase db, long feedId) {

        ContentValues values = new ContentValues();
        values.put(Feeds.DELETED, Feeds.FEED_DELETED);
        int result = db.update(HeadboxDatabaseHelper.TABLE_FEEDS, values, Feeds._ID + "=" + feedId, null);
        return result;
    }

    private int deleteFeedFromDatabase(SQLiteDatabase db, long feedId) {
        int result = db.delete(HeadboxDatabaseHelper.TABLE_FEEDS, Feeds._ID + "=" + feedId, null);
        return result;
    }

    private int deleteContactsMergeLink(SQLiteDatabase db, Uri uri) {

        int affectedRows = 0;

        String feedContactId = uri.getPathSegments().get(1);
        String mergedContactId = uri.getPathSegments().get(2);
        affectedRows = db.delete(HeadboxDatabaseHelper.TABLE_MERGE_LINKS,
                MergeLinks.FIRST_CONTACT_INDENTIFIER + "  LIKE "
                        + feedContactId + "%'  AND "
                        + MergeLinks.SECOND_CONTACT_INDENTIFIER + " LIKE  "
                        + mergedContactId + "%'", null
        );

        ContentValues contentValues = new ContentValues();
        contentValues.put(HeadboxContacts.CONTACT_ID, mergedContactId);
        db.update(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, contentValues,
                HeadboxContacts.CONTACT_ID + "=" + feedContactId + " AND "
                        + HeadboxContacts.CONTACT_TYPE + "="
                        + BlinqApplication.searchSource.getId(), null
        );

        return affectedRows;
    }

    /**
     * Update the feed after deleting message
     */
    private void updateFeedAfterDelete(SQLiteDatabase db, long feedId) {
        try {

            updateFeed(db, feedId);
            markFeedAsModified(db, feedId, Feeds.FEED_MODIFIED);

        } catch (Exception ex) {
            Log.e(TAG,
                    "Update feed After Deleting records from: error in conversion "
                            + feedId
            );
        }

    }

    /**
     * Delete Message from Headbox db.
     */
    private int deleteMesssage(SQLiteDatabase db, Uri uri, String selection,
                               String[] selectionArgs) {

        db.execSQL(HeadboxDBQueries.CREATE_TRIGGER_DELETE_FROM_MESSAGE_TABLE);

        String messageSourceId = uri.getPathSegments().get(3);
        Platform platform = Platform.fromId(Integer.valueOf(uri
                .getPathSegments().get(2)));

        long feedId = Long.valueOf(uri.getPathSegments().get(4));
        if (feedId == 0 || feedId == -1) {
            feedId = getFeed(db, platform, messageSourceId);
        }

        String extendedSelection = Messages.SOURCE_ID + "=\"" + messageSourceId
                + "\"";
        selection = DatabaseUtils
                .concatenateWhere(selection, extendedSelection);
        extendedSelection = Messages.PLATFORM_ID + "=" + platform.getId();
        selection = DatabaseUtils
                .concatenateWhere(selection, extendedSelection);
        int affectedRows = db.delete(HeadboxDatabaseHelper.TABLE_MESSAGES,
                selection, selectionArgs);

        db.execSQL(HeadboxDBQueries.DROP_TRIGGER_DELETE_FROM_MESSAGE_TABLE);

        updateFeedAfterDelete(db, feedId);

        return affectedRows;
    }

    /**
     * Returns the feed id for a certain message by its platform and source id.
     */
    private long getFeed(SQLiteDatabase db, Platform platform,
                         String messageSourceId) {

        String selection = Messages.SOURCE_ID + "=?" + " AND "
                + Messages.PLATFORM_ID + "=?";
        String[] selectionArgs = new String[]{messageSourceId,
                platform.getIdAsString()};
        Cursor cursor = db.query(false, HeadboxDatabaseHelper.TABLE_MESSAGES,
                null, selection, selectionArgs, null, null, null, null);

        long feedId = 0;
        try {
            if (cursor.moveToFirst()) {
                do {
                    feedId = cursor.getLong(cursor
                            .getColumnIndex(Messages.Feed_ID));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage() + "");
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return feedId;
    }

    /**
     * Delete All Messages in certain platform from Headbox db.
     */
    private int deleteAllPlatformMessages(SQLiteDatabase db, Uri uri,
                                          String selection, String[] selectionArgs) {
        String platform = uri.getPathSegments().get(2);
        String extendedSelection = Messages.PLATFORM_ID + "=" + platform;
        selection = DatabaseUtils
                .concatenateWhere(selection, extendedSelection);
        int affectedRows = db.delete(HeadboxDatabaseHelper.TABLE_MESSAGES,
                selection, selectionArgs);
        return affectedRows;
    }

    /**
     * update feed as modified
     */
    private void markFeedAsModified(SQLiteDatabase db, long feedId, int modified) {
        // mark the feed as modified

        String whereClause = FeedColumns._ID + " = " + feedId;
        ContentValues values = new ContentValues(1);
        values.put(FeedColumns.MODIFIED, modified);
        db.update(HeadboxDatabaseHelper.TABLE_FEEDS, values, whereClause, null);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    // These URIs are defined to be used when querying feed provider.
    public static final Uri INSERT_MESSAGE_URI = Uri.parse(URL + "/messages");
    public static final Uri MESSAGES_CONTENT_URI = Uri.parse(URL
            + "/messages/#");
    public static final Uri MESSAGES_DELETE_URI = Uri.parse(URL
            + "/messages/source_id/#/*/#");
    public static final Uri ALL_MESSAGES_DELETE_URI = Uri.parse(URL
            + "/messages/platform/#");
    public static final Uri FEED_LIST_URI = Uri.parse(URL + "/complete-feeds/");
    public static final Uri FILTERED_FEEDS_URI = Uri.parse(URL + "/filtered/#");
    public static final Uri FEED_PLATFORMS_URI = Uri.parse(URL
            + "/feedPlatforms/#");
    public static final Uri FEED_UNREAD_PLATFORMS_URI = Uri.parse(URL
            + "/feedUnreadPlatforms/#");
    public static final Uri FEED_URI = Uri.parse(URL + "/feedID/");
    public static final Uri FEED_HISTORY_URI = Uri.parse(URL
            + "/feedsHistory/#");
    public static final Uri FEEDS_URI = Uri.parse(URL + "/feeds/");
    public static final Uri FEED_REFRESH_HISTORY_URI = Uri.parse(URL
            + "/refreshHistory");
    public static final Uri FEED_MESSAGES_URI = Uri.parse(URL
            + "/feed_messages/#");
    public static final Uri FEED_UNDO_MERGE_URI = Uri.parse(URL
            + "/feeds_undo_merge/");
    public static final Uri FEED_MEMBER_CONTACTS_URI = Uri.parse(URL
            + "/feed_contacts/#");
    public static final Uri CONTACT_PHONES_EMAILS_URI = Uri.parse(URL
            + "/contact_phones_emails/#");

    public static final Uri CONTACTS_URI = Uri.parse(URL + "/contacts/#");
    public static final Uri CONTACTS_SEARCH_URI = Uri
            .parse(URL + "/search/#/*");

    public static final Uri FACEBOOK_URI = Uri.parse(URL + "/facebookHistory");

    public static final Uri GENERAL_CONTACTS_URI = Uri.parse(URL
            + "/general_contacts/#");
    public static final Uri PLATFORM_CONTACTS_URI = Uri.parse(URL
            + "/platform_contacts/#");
    public static final Uri UPDATE_GOOGLE_CONTACTS_URI = Uri.parse(URL
            + "/updateGoogleContacts");
    public static final Uri GOOGLE_PLUS_CONTACTS_URI = Uri.parse(URL
            + "/googlePlusContacts");
    public static final Uri CONTACTS_MERGE_URI = Uri.parse(URL
            + "/contacts_merge/#/#");
    public static final Uri CONTACTS_UNMERGE_URI = Uri.parse(URL
            + "/contacts_unmerge/#/*");
    public static final Uri FEEDS_MERGE_URI = Uri.parse(URL + "/feeds_merge/#");
    public static final Uri GOOGLE_COVERS_URI = Uri.parse(URL
            + "/googleContactsCovers");
    public static final Uri FEED_LAST_MESSAGE_BY_PLATFORM_URI = Uri.parse(URL
            + "/feed_last_message_by_platform/#");
    public static final Uri MERGE_LINK_DELETE_URI = Uri.parse(URL
            + "/merge_links/#/*");
    public static final Uri UPDATE_LOCAL_CONTACTS_URI = Uri.parse(URL
            + "/update_local_contacts");
    public static final Uri UPDATE_TOP_FRIENDS_NOTIFICATIONS = Uri.parse(URL +
            "/update_top_friends_notifications");
    public static final Uri UPDATE_NOTIFICATION_FOR_CONTACT = Uri.parse(URL +
            "/update_notification_for_contact/#");
    public static final Uri INSERT_WELCOME_FEED = Uri.parse(URL + "/welcome_feed");
    public static final Uri UPDATE_CONTACT_NAME_FROM_SERVER = Uri.parse(URL + "/update_contact_name_from_server/#");
    public static final Uri CONTACTS_COUNT = Uri.parse(URL + "/contacts_count");

    static {

        // Create the common contacts-feeds columns
        sContactsProjection = new HashMap<String, String>();
        sContactsProjection.put(Feeds._ID, Feeds._ID);
        sContactsProjection.put(Feeds.LAST_MSG_DATE, Feeds.LAST_MSG_DATE);
        sContactsProjection.put(Feeds.READ, Feeds.READ);
        sContactsProjection.put(Feeds.LAST_CALL_TYPE, Feeds.LAST_CALL_TYPE);
        sContactsProjection.put(Feeds.LAST_MSG_TYPE, Feeds.LAST_MSG_TYPE);
        sContactsProjection.put(Feeds.LAST_PLATFORM, Feeds.LAST_PLATFORM);
        sContactsProjection.put(HeadboxContacts.CONTACT_ID,
                HeadboxContacts.CONTACT_ID);
        sContactsProjection.put(HeadboxContacts.NAME, HeadboxContacts.NAME);
        sContactsProjection.put(HeadboxContacts.PRIMARY_INDENTIFIER,
                HeadboxContacts.PRIMARY_INDENTIFIER);
        sContactsProjection.put(HeadboxContacts.COMPLETE_INDENTIFIER,
                HeadboxContacts.COMPLETE_INDENTIFIER);
        sContactsProjection.put(HeadboxContacts.PICTURE_URL,
                HeadboxContacts.PICTURE_URL);
        sContactsProjection.put(HeadboxContacts.NOTIFICATION_ENABLED,
                HeadboxContacts.NOTIFICATION_ENABLED);
        sContactsProjection.put(HeadboxContacts.CONTACT_TYPE,
                HeadboxContacts.CONTACT_TYPE);
        sContactsProjection.put(HeadboxContacts.PICTURE_URL,
                HeadboxContacts.PICTURE_URL);

        sFeedsProjection = new HashMap<String, String>(sContactsProjection);
        sFeedsProjection.put(Feeds.SNIPPET_TEXT, Feeds.SNIPPET_TEXT);
        sFeedsProjection.put(Feeds.MODIFIED, Feeds.MODIFIED);
        sFeedsProjection.put(Feeds.MEMBERS_IDS, Feeds.MEMBERS_IDS);
        sFeedsProjection.put(Feeds.FEED_IDENTIFIER, Feeds.FEED_IDENTIFIER);
        sFeedsProjection.put(Feeds.MESSAGE_COUNT, Feeds.MESSAGE_COUNT);
        sFeedsProjection.put(Feeds.LAST_MSG_TYPE, Feeds.LAST_MSG_TYPE);

        // Contacts URI matching table
        URIMatcher.addURI(PROVIDER_NAME, "feeds", URI_FEEDS);
        URIMatcher.addURI(PROVIDER_NAME, "complete-feeds", URI_COMPLETE_FEEDS);
        URIMatcher.addURI(PROVIDER_NAME, "feed_messages/#", URI_FEED_MESSAGES);
        URIMatcher.addURI(PROVIDER_NAME, "feed_contacts/#",
                URI_FEED_MEMBER_CONTACTS);
        URIMatcher.addURI(PROVIDER_NAME, "feeds_undo_merge",
                URI_UNDO_FEED_MERGE);
        URIMatcher.addURI(PROVIDER_NAME, "contact_phones_emails/#",
                URI_CONTACT_PHONES_EMAILS);
        URIMatcher.addURI(PROVIDER_NAME, "feedsHistory/*", URI_FEEDS_HISTORY);
        URIMatcher
                .addURI(PROVIDER_NAME, "refreshHistory/", URI_REFRESH_HISTORY);
        URIMatcher.addURI(PROVIDER_NAME, "feeds/#/subject", URI_FEEDS_SUBJECT);
        URIMatcher.addURI(PROVIDER_NAME, "feedPlatforms/#", URI_FEED_PLATFORMS);
        URIMatcher.addURI(PROVIDER_NAME, "feedUnreadPlatforms/#",
                URI_FEED_UNREAD_PLATFORMS);
        URIMatcher.addURI(PROVIDER_NAME, "feedID/#", URI_FEED_ID);
        URIMatcher.addURI(PROVIDER_NAME, "filtered/#", URI_FEED_BY_PLATFORMS);
        URIMatcher.addURI(PROVIDER_NAME, "messages/#", URI_MESSAGE_ID);
        URIMatcher.addURI(PROVIDER_NAME, "messages/source_id/#/*/#",
                URI_MESSAGE_SOURCE_ID);

        URIMatcher.addURI(PROVIDER_NAME, "/messages/platform/#",
                URI_ALL_MESSAGES_DELETE);
        URIMatcher.addURI(PROVIDER_NAME, "search/#/*", URI_SEARCH);
        URIMatcher.addURI(PROVIDER_NAME, "contacts/", URI_CONTACT);
        URIMatcher.addURI(PROVIDER_NAME, "general_contacts/#",
                URI_GENERAL_CONTACTS);
        URIMatcher.addURI(PROVIDER_NAME, "googlePlusContacts",
                +URI_GOOGLE_PLUS_CONTACTS);
        URIMatcher.addURI(PROVIDER_NAME, "messages", URI_MESSAGE);
        URIMatcher.addURI(PROVIDER_NAME, "facebookHistory",
                URI_FACEBOOK_HISTORY);
        URIMatcher.addURI(PROVIDER_NAME, "contacts_merge/#/#",
                URI_CONTACTS_MERGE);
        URIMatcher.addURI(PROVIDER_NAME, "contacts_unmerge/#/*",
                URI_CONTACTS_UNMERGE);
        URIMatcher.addURI(PROVIDER_NAME, "feeds_merge/#", URI_FEEDS_MERGE);
        URIMatcher.addURI(PROVIDER_NAME, "updateGoogleContacts",
                URI_UPDATE_GOOGLE_CONTACTS);
        URIMatcher.addURI(PROVIDER_NAME, "platform_contacts/#",
                URI_PLATFORM_CONTACTS);
        URIMatcher.addURI(PROVIDER_NAME, "googleContactsCovers",
                URI_UPDATE_GOOGLE_CONTACTS_COVERS);
        URIMatcher.addURI(PROVIDER_NAME, "feed_last_message_by_platform/#",
                URI_FEED_LAST_MESSAGE_BY_PLATFORM);
        URIMatcher.addURI(PROVIDER_NAME, "merge_links/#/*",
                URI_DELETE_MERGE_LINK);
        URIMatcher.addURI(PROVIDER_NAME, "update_local_contacts",
                URI_UPDATE_CONTACTS);
        URIMatcher.addURI(PROVIDER_NAME, "update_top_friends_notifications",
                URI_UPDATE_TOP_FRIENDS_NOTIFICATIONS);
        URIMatcher.addURI(PROVIDER_NAME, "update_notification_for_contact/#",
                URI_UPDATE_NOTIFICATION_FOR_CONTACT);
        URIMatcher.addURI(PROVIDER_NAME, "welcome_feed", URI_INSERT_WELCOME_FEED);
        URIMatcher.addURI(PROVIDER_NAME, "update_contact_name_from_server/#",
                URI_UPDATE_CONTACT_NAME_FROM_SERVER);
        URIMatcher.addURI(PROVIDER_NAME, "contacts_count", URI_CONTACTS_COUNT);
    }

    public static void setMessages(Map<String, ContentValues[]> inbox) {
        messages = inbox;
    }

    public static void setContactsForMerge(List<String> contactIdentifiers) {
        contactsForMerge = contactIdentifiers;
    }

    public long insertWelcomeFeed() {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        int contactId = getLastIntInColumn(db, HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, PhoneContacts.ID) + 1;
        ContentValues contactValues = createWelcomeContactContectValues(contactId);
        db.insert(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, null, contactValues);
        long feedId = insertFeed(db, contactValues.getAsString(HeadboxContacts.PRIMARY_INDENTIFIER));
        ContentValues messageValues = createWelcomeMessageContectValues(feedId);
        db.insert(HeadboxDatabaseHelper.TABLE_MESSAGES, null, messageValues);
        updateFeed(feedId);
        return feedId;
    }

    private int getLastIntInColumn(SQLiteDatabase db, String table, String column) {
        int value = 0;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT " + column + " FROM " + table + " ORDER BY " + column + " DESC LIMIT 1", null);
            c.moveToFirst();
            value = c.getInt(0);
        } finally {
            c.close();
            return value;
        }
    }

    private ContentValues createWelcomeContactContectValues(int contactId) {
        Resources resources = getContext().getResources();
        ContentValues values = new ContentValues(8);
        values.put(PhoneContacts.ID, contactId);
        values.put(HeadboxContacts.NAME, resources.getString(R.string.welcome_feed_name));
        values.put(HeadboxContacts.PICTURE_URL, ImageUtils.DRAWABLE_PATH + R.drawable.welcome_feed_profile);
        values.put(HeadboxContacts.COVER_URL, ImageUtils.DRAWABLE_PATH + R.drawable.welcome_feed_profile);
        values.put(HeadboxContacts.COMPLETE_INDENTIFIER, resources.getString(R.string.welcome_feed_email));
        values.put(HeadboxContacts.PRIMARY_INDENTIFIER, resources.getString(R.string.welcome_feed_email));
        values.put(HeadboxContacts.CONTACT_TYPE, Platform.SMS.getId());
        values.put(HeadboxContacts.NOTIFICATION_ENABLED, 0);
        return values;
    }

    private ContentValues createWelcomeMessageContectValues(long feedId) {
        Resources resources = getContext().getResources();
        ContentValues values = new ContentValues(11);
        String contact = resources.getString(R.string.welcome_feed_email);
        values.put(Messages.Feed_ID, feedId);
        values.put(Messages.CONTACT, contact);
        values.put(Messages.CONTACT_COMPLETE, contact);
        values.put(Messages.DATE, System.currentTimeMillis());
        values.put(Messages.PLATFORM_ID, Platform.SMS.getId());
        values.put(Messages.READ, 0);
        values.put(Messages.CALL_DURATION, 0);
        values.put(Messages.STATUS, 0);
        values.put(Messages.TYPE, 1);
        values.put(Messages.BODY, resources.getString(R.string.welcome_feed_snippet_text));
        values.put(Messages.SEEN, 0);
        return values;
    }

    private void updateFeed(long feedId) {
        ContentValues values = new ContentValues(1);
        values.put(Feeds.MODIFIED, Feeds.FEED_UNMODIFIED);
        update(FeedProvider.FEEDS_URI, values,
                    Feeds._ID + " IN (" + feedId + ")", null);
    }

}