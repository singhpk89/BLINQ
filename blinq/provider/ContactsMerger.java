package com.blinq.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.google.gson.Gson;
import com.blinq.PreferencesManager;
import com.blinq.models.MemberContact;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.provider.utils.HeadboxDBQueries;
import com.blinq.provider.utils.ModelConverter;
import com.blinq.utils.ContactsMatcher;
import com.blinq.utils.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Contacts merging to manage the way of merging/un-merging of headbox contacts.
 *
 * @author Johan Hansson.
 */
public class ContactsMerger {

    private static final String TAG = ContactsMerger.class.getSimpleName();

    private static ContactsMerger instance;
    private static Context context;
    private static HeadboxDatabaseHelper openHelper;

    /**
     * Contact that we should link the second contact with.
     */
    private static String firstContact;
    /**
     * Contact that we should link or join to the first contact.
     */
    private static String secondContact;

    /**
     * Feed that we should redirect the second feed data to.
     */
    private static int firstFeed;
    /**
     * Feed that we should link its data to the first feed.
     */
    private static int secondFeed;

    private static Platform platform;

    public ContactsMerger(Context context) {
        openHelper = HeadboxDatabaseHelper.getInstance(context);
    }

    /**
     * Return a single object for the <code>ContactsMerger</code>.
     *
     * @param context - context from which to call the helper.
     * @return ContactsMerger instance.
     */
    public static ContactsMerger getInstance(Context context) {
        if (instance == null) {
            instance = new ContactsMerger(context);
        }
        ContactsMerger.context = context;
        return instance;

    }

    public static ContactsMerger setContacts(String firstContact, String secondContact) {
        ContactsMerger.firstContact = firstContact;
        ContactsMerger.secondContact = secondContact;
        return instance;
    }

    public static ContactsMerger setFeeds(int firstFeed, int secondFeed) {
        ContactsMerger.firstFeed = firstFeed;
        ContactsMerger.secondFeed = secondFeed;
        return instance;
    }

    public static ContactsMerger setPlatform(Platform platform) {
        ContactsMerger.platform = platform;
        return instance;
    }

    public static void merge() {

        mergeContacts();
        mergeFeeds();
    }

    /**
     * In order to merge two contacts together we should pass contact id for both contacts.
     * will merge the second contact with the first one.
     */
    public static void mergeContacts() {

        if (StringUtils.isBlank(secondContact) || StringUtils.isBlank(firstContact))
            return;

        ContentValues values = new ContentValues(1);
        values.put(HeadboxFeed.HeadboxContacts.CONTACT_ID, firstContact);

        SQLiteDatabase db = openHelper.getWritableDatabase();

        buildMergeLinks(db, firstContact, secondContact, platform);

        HashMap<String, String> feedHashMap = getContactJoinedConversations(platform, secondContact);

        for (String feedId : feedHashMap.keySet()) {

            db.execSQL(Query.UPDATE_FEED_MEMBER_ID_COLUMN, new String[]{secondContact, platform.getIdAsString(), feedId});
        }

        /**
         * Update the second contact by the id of the first contact.
         */
        String mergeCondition = HeadboxFeed.HeadboxContacts.CONTACT_ID + "=" + secondContact + " AND "
                + HeadboxFeed.HeadboxContacts.CONTACT_TYPE + "=" + platform.getIdAsString();
        db.update(HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS, values, mergeCondition, null);


        //Here to save the merge info.. in order to undo the merge..
        PreferencesManager preferencesManager = new PreferencesManager(context);

        preferencesManager.setProperty(PreferencesManager.MERGED_CONTACT_ID, secondContact);
        preferencesManager.setProperty(PreferencesManager.FEED_CONTACT_ID, firstContact);
        preferencesManager.setProperty(PreferencesManager.MERGED_FEEDS,
                new Gson().toJson(feedHashMap));
        preferencesManager.setProperty(PreferencesManager.SEARCH_SOURCE,
                new Gson().toJson(platform));
        preferencesManager.setProperty(PreferencesManager.IS_SHOW_UNDO_MERGE, true);

    }

    /**
     * Move all all communications for a given platform form second feed to the first feed.
     */
    public static void mergeFeeds() {

        if (firstFeed == 0 && secondFeed == 0)
            return;

        SQLiteDatabase db = openHelper.getWritableDatabase();

        if (secondFeed == 0) {
            markFeedAsModified(db, firstFeed, HeadboxFeed.Feeds.FEED_MODIFIED);
            return;
        }

        ContentValues values = new ContentValues(1);
        values.put(HeadboxFeed.Messages.Feed_ID, firstFeed);

        String whereClause = HeadboxFeed.Messages.Feed_ID + "=" + secondFeed;

        if (platform == Platform.CALL || platform == Platform.SMS) {

            whereClause += " AND ("
                    + HeadboxFeed.Messages.PLATFORM_ID + "=" + Platform.CALL.getId()
                    + " OR " + HeadboxFeed.Messages.PLATFORM_ID + "="
                    + Platform.SMS.getId() + ")";
        } else {

            whereClause += " AND " + HeadboxFeed.Messages.PLATFORM_ID + "=" + platform.getId();
        }

        PreferencesManager preferencesManager = new PreferencesManager(context);
        preferencesManager.setProperty(PreferencesManager.CONTACT_FEED, firstFeed);
        preferencesManager.setProperty(PreferencesManager.MERGED_CONTACT_FEED, secondFeed);

        db.update(HeadboxDatabaseHelper.TABLE_MESSAGES, values, whereClause, null);

        updateFeed(db, Long.valueOf(firstFeed));
        updateFeed(db, Long.valueOf(secondFeed));

        markFeedAsModified(db, firstFeed, HeadboxFeed.Feeds.FEED_MODIFIED);
        markFeedAsModified(db, secondFeed, HeadboxFeed.Feeds.FEED_MODIFIED);
    }

    /**
     * update feed as modified
     */
    private static void markFeedAsModified(SQLiteDatabase db, long feedId, int modified) {

        String whereClause = HeadboxFeed.FeedColumns._ID + " = " + feedId;
        ContentValues values = new ContentValues(1);
        values.put(HeadboxFeed.FeedColumns.MODIFIED, modified);
        db.update(HeadboxDatabaseHelper.TABLE_FEEDS, values, whereClause, null);
    }

    /**
     * Update feed details
     *
     * @param db - database instance.
     */
    private static void updateFeed(SQLiteDatabase db, long feedId) {

        if (feedId < 0) {
            return;
        }

        db.beginTransaction();
        try {

            // Update the message count in feeds table as the count
            // of all messages on messages table.
            db.execSQL(" UPDATE feeds SET message_count = "
                    + " (SELECT COUNT(messages._id) FROM messages LEFT JOIN feeds "
                    + " ON feeds._id = " + HeadboxFeed.Messages.Feed_ID + " WHERE "
                    + HeadboxFeed.Messages.Feed_ID + " = " + feedId + " AND messages."
                    + HeadboxFeed.Messages.TYPE + " !=" + MessageType.DRAFT.getId() + ")"
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
                    HeadboxFeed.FeedPlatformsColumns.Feed_ID + "=?", new String[]{feedId
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

    private static HashMap<String, String> getContactJoinedConversations(Platform platform, String contactId) {

        HashMap<String, String> feedHashMap = new HashMap<String, String>();
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor c = db.rawQuery(Query.RETURNS_CONTACT_CONNECTED_FEEDS, new String[]{contactId, platform.getIdAsString()});

        if (c != null) {
            try {
                while (c.moveToNext()) {
                    feedHashMap.put(c.getString(0), c.getString(1));
                }
            } finally {
                c.close();
            }
        }
        return feedHashMap;
    }

    /**
     * Build a merge links between the 1st and the 2nd contact.
     */

    private static void buildMergeLinks(SQLiteDatabase db, String firstContact, String secondContact, Platform platform) {

        db.execSQL(Query.BUILD_MERGE_LINKS_QUERY, new String[]{firstContact, secondContact, platform.getId() + ""});
    }


    public static List<SearchResult> getContactsSearchSuggestions(int feedId, String contactId,
                                                                  String query, Platform platform) {

        String platformCondition = HeadboxFeed.HeadboxContacts.CONTACT_TYPE + "=" + platform.getId();
        Cursor cursor = getContacts(platform, platformCondition);
        List<SearchResult> searchResults = ModelConverter.convertMergeResults(cursor);

        if (searchResults.size() == 0)
            return searchResults;

        HashMap<Platform, List<MemberContact>> mergedContacts = FeedProviderImpl.getInstance(context).getContacts(feedId);

        List<String> queries = new ArrayList<String>();

        queries.add(query);

        //Build list of queries.
        if (mergedContacts != null)
            for (Platform contactType : mergedContacts.keySet()) {
                String contactNameQuery = mergedContacts.get(contactType).get(0).getContactName();
                if (!StringUtils.isBlank(contactNameQuery)) {
                    queries.add(contactNameQuery);
                    if (contactNameQuery.contains(" ")) {
                        for (String substring : contactNameQuery.split(" ")) {
                            queries.add(substring);
                        }
                    }
                }
            }

        List<SearchResult> suggestedContacts = ContactsMatcher.getSuggestedContacts(searchResults, queries);

        //Discard the same user from the search suggestion results.
        for (SearchResult result : suggestedContacts) {
            if (contactId.equals(result.getContact().getContactId())
                    || (!StringUtils.isBlank(result.getFeedId()) && result.getFeedId().equals(String.valueOf(feedId)))) {
                suggestedContacts.remove(result);
            }
        }

        return suggestedContacts;
    }


    private static Cursor getContacts(Platform platform, String selection) {

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

    private static final HashMap<String, String> sContactsProjection;

    static {

        // Create the common contacts-feeds columns
        sContactsProjection = new HashMap<String, String>();
        sContactsProjection.put(HeadboxFeed.Feeds._ID, HeadboxFeed.Feeds._ID);
        sContactsProjection.put(HeadboxFeed.Feeds.LAST_MSG_DATE, HeadboxFeed.Feeds.LAST_MSG_DATE);
        sContactsProjection.put(HeadboxFeed.Feeds.READ, HeadboxFeed.Feeds.READ);
        sContactsProjection.put(HeadboxFeed.Feeds.LAST_CALL_TYPE, HeadboxFeed.Feeds.LAST_CALL_TYPE);
        sContactsProjection.put(HeadboxFeed.Feeds.LAST_MSG_TYPE, HeadboxFeed.Feeds.LAST_MSG_TYPE);
        sContactsProjection.put(HeadboxFeed.Feeds.LAST_PLATFORM, HeadboxFeed.Feeds.LAST_PLATFORM);
        sContactsProjection.put(HeadboxFeed.HeadboxContacts.CONTACT_ID,
                HeadboxFeed.HeadboxContacts.CONTACT_ID);
        sContactsProjection.put(HeadboxFeed.HeadboxContacts.NAME, HeadboxFeed.HeadboxContacts.NAME);
        sContactsProjection.put(HeadboxFeed.HeadboxContacts.PRIMARY_INDENTIFIER,
                HeadboxFeed.HeadboxContacts.PRIMARY_INDENTIFIER);
        sContactsProjection.put(HeadboxFeed.HeadboxContacts.COMPLETE_INDENTIFIER,
                HeadboxFeed.HeadboxContacts.COMPLETE_INDENTIFIER);
        sContactsProjection.put(HeadboxFeed.HeadboxContacts.PICTURE_URL,
                HeadboxFeed.HeadboxContacts.PICTURE_URL);
        sContactsProjection.put(HeadboxFeed.HeadboxContacts.NOTIFICATION_ENABLED,
                HeadboxFeed.HeadboxContacts.NOTIFICATION_ENABLED);
        sContactsProjection.put(HeadboxFeed.HeadboxContacts.CONTACT_TYPE,
                HeadboxFeed.HeadboxContacts.CONTACT_TYPE);
        sContactsProjection.put(HeadboxFeed.HeadboxContacts.PICTURE_URL,
                HeadboxFeed.HeadboxContacts.PICTURE_URL);
    }

    private final class Query {

        /**
         * Responsible to build a merge links between the second contact and every sub contact on the first contact.
         * <p/>
         * Assume that we want to merge a facebook contact to another contact have skype,phone,and whatsapp sub-contacts.
         * Executing this query will create links between the second contact and each sub contact of the first contact.
         * <p/>
         * In order to store the those links after upgrading the database.
         * <p/>
         * TO WORK with this query you have to pass three parameters: 1st contact id,2nd contact id,and platform id.
         */
        public static final String BUILD_MERGE_LINKS_QUERY = "INSERT INTO merge_links "
                + "(first_contact_identifier,first_contact_type,second_contact_identifier,second_contact_type,type) "
                + "SELECT DISTINCT "
                + "h1.headbox_contact_primary_identifier,h1.headbox_contact_type,"
                + "h2.headbox_contact_primary_identifier,h2.headbox_contact_type,"
                + HeadboxFeed.MergeLinks.TYPE_LINK
                + " "
                + "FROM headbox_contacts h1 INNER JOIN headbox_contacts h2 "
                + "ON h1.phone_contact_id != h2.phone_contact_id "
                + "WHERE h1.phone_contact_id = ? AND h2.phone_contact_id = ? AND h2.headbox_contact_type = ?";

        /**
         * Returns the id of conversation(feed) which is joined with a specific contact for a certain platform.
         * <p/>
         * To WORK with this query you should pass two parameters: contact id and its platform.
         */
        public static final String RETURNS_CONTACT_CONNECTED_FEEDS = "SELECT _id,members_ids FROM feeds WHERE members_ids "
                + "IN (SELECT headbox_contact_primary_identifier FROM headbox_contacts "
                + "WHERE phone_contact_id =? AND headbox_contact_type = ?)";

        /**
         * Update the members_ids column of a specific feed to a specific contact identifier different from a certain platform.
         * <p/>
         * To WORK with this query you should pass three parameters: contact id,with contact type[platform],and feedId.
         */
        public static final String UPDATE_FEED_MEMBER_ID_COLUMN = "UPDATE feeds "
                + "SET members_ids = (SELECT headbox_contact_primary_identifier FROM headbox_contacts "
                + "WHERE phone_contact_id =? and headbox_contact_type != ? LIMIT 1) "
                + "WHERE _id = ?";
    }

}
