package com.blinq.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.provider.HeadboxFeed.FacebookContacts;
import com.blinq.provider.HeadboxFeed.FeedPlatformsColumns;
import com.blinq.provider.HeadboxFeed.Feeds;
import com.blinq.provider.HeadboxFeed.GoogleContacts;
import com.blinq.provider.HeadboxFeed.GooglePlusContacts;
import com.blinq.provider.HeadboxFeed.HeadboxContacts;
import com.blinq.provider.HeadboxFeed.MergeLinks;
import com.blinq.provider.HeadboxFeed.Messages;
import com.blinq.provider.HeadboxFeed.MessagesColumns;
import com.blinq.provider.HeadboxFeed.PhoneContacts;
import com.blinq.provider.HeadboxFeed.PlatformContacts;
import com.blinq.provider.utils.HeadboxDBQueries;
import com.blinq.utils.AppUtils;

/**
 * @author Johan Hansson.
 */
public class HeadboxDatabaseHelper extends SQLiteOpenHelper {

    public static final String TAG = HeadboxDatabaseHelper.class
            .getSimpleName();

    private static HeadboxDatabaseHelper instance = null;
    public static final String DATABASE_NAME = "headbox.db";
    private static final int DATABASE_VERSION = 102;
    private final Context context;

    /**
     * Database Tables.
     */
    public static final String TABLE_FEEDS = "feeds";
    public static final String TABLE_MESSAGES = "messages";
    public static final String TABLE_DELETED_MESSAGES = "deleted_messages";
    public static final String TABLE_FEED_PLATFORMS = "feed_platforms";
    public static final String TABLE_PHONE_CONTACTS = "headbox_phone_contacts";
    public static final String TABLE_FACEBOOK_CONTACTS = "headbox_facebook_contacts";
    public static final String TABLE_HEADBOX_CONTACTS = "headbox_contacts";
    public static final String TABLE_HEADBOX_CONTACTS_LOOKUP = "headbox_contacts_Lookup";
    public static final String TABLE_GOOGLE_CONTACTS = "headbox_google_contacts";
    public static final String TABLE_PLATFORMS_CONTACTS = "headbox_temporary_contacts";
    public static final String TABLE_GOOGLE_PLUS_CONTACTS = "headbox_google_plus_contacts";
    public static final String TABLE_MERGE_LINKS = "merge_links";

    /**
     * Prevent updating the feed record for the deleted messages.
     */
    private static final String IGNORE_DELETED_MESSAGE_CONDITION = "new.id_source "
            + "NOT IN ( SELECT deleted_messages.id_source FROM deleted_messages "
            + "WHERE deleted_messages.id_source=new.id_source AND deleted_messages.platform = new.platform )";

    /**
     * Trigger called automatically to update the feed read column when message
     * read column updated.
     */
    private static final String UPDATE_FEED_READ = " UPDATE feeds SET "
            + Feeds.READ + " = " + " CASE (SELECT COUNT(*)" + " FROM messages"
            + " WHERE " + Messages.READ + " = " + Messages.MSG_UNREAD
            + " AND " + Messages.Feed_ID + " = feeds._id)" + " WHEN 0 THEN 1"
            + " ELSE 0" + " END" + " WHERE feeds._id = new." + Messages.Feed_ID
            + " AND " + IGNORE_DELETED_MESSAGE_CONDITION + "; ";

    private static final String UPDATE_FEED_PLATFORMS_READ = " UPDATE feed_platforms SET "
            + FeedPlatformsColumns.UNREAD_MESSAGES_COUNT + " = "
            + " CASE (" + "new." + Feeds.READ + ")" + " WHEN " + Feeds.FEED_READ + " THEN 0"
            + " ELSE " + "feed_platforms." + FeedPlatformsColumns.UNREAD_MESSAGES_COUNT + " END"
            + " WHERE feed_platforms.feed_id = new." + Feeds._ID + ";";

    /**
     * Trigger to update messages count of the feed on new message.
     */
    private static final String UPDATE_FEED_COUNT_ON_NEW_MESSAGE =
            " UPDATE feeds SET message_count = " +
                    " (SELECT COUNT(messages._id) FROM messages LEFT JOIN feeds " +
                    " ON feeds._id = " + "messages." + Messages.Feed_ID +
                    " WHERE " + Messages.Feed_ID + " = new.feed_id" +
                    " AND messages." + Messages.TYPE + " !=" + MessageType.DRAFT.getId() + ")" +
                    " WHERE feeds._id" + " = new." + Messages.Feed_ID +
                    " AND " + IGNORE_DELETED_MESSAGE_CONDITION + ";";

    private static final String UPDATE_FEED_COUNT_ON_MESSAGE_DELETED =
            " UPDATE feeds SET " + Feeds.MODIFIED + " = "
                    + Feeds.FEED_MODIFIED + "," + Feeds.MESSAGE_COUNT + "=" +
                    " (SELECT COUNT(messages._id) FROM messages LEFT JOIN feeds " +
                    " ON feeds._id = " + "messages." + Messages.Feed_ID +
                    " WHERE " + Messages.Feed_ID + " = old.feed_id" +
                    " AND messages." + Messages.TYPE + "!=" + MessageType.DRAFT.getId() + ")" +
                    " WHERE feeds._id" + " = old." + Messages.Feed_ID + ";";

    private static final String UPDATE_FEED_LAST_CALL_TYPE = " UPDATE feeds SET "
            + Feeds.LAST_CALL_TYPE + " = new." + Messages.TYPE
            + " WHERE feeds._id = new." + Messages.Feed_ID
            + " AND new." + Messages.PLATFORM_ID + "=" + Platform.CALL.getId()
            + " AND " + IGNORE_DELETED_MESSAGE_CONDITION + ";";


    private static final String RESET_UNREAD_COUNT_IF_LAST_MESSAGE_IS_OUTGOING = " UPDATE feed_platforms SET " + FeedPlatformsColumns.UNREAD_MESSAGES_COUNT + " = 0"
            + " WHERE " + FeedPlatformsColumns.Feed_ID + "=" + " new." + Messages.Feed_ID
            + " AND " + FeedPlatformsColumns.PLATFORM_ID + "=" + " new." + Messages.PLATFORM_ID
            + " AND new." + Messages.TYPE + " = " + MessageType.OUTGOING.getId()
            + " OR " + "( " + " new." + Messages.TYPE + " = " + MessageType.INCOMING.getId()
            + " AND " + " new." + Messages.PLATFORM_ID + "=" + Platform.CALL.getId() + " )" + ";";

    /**
     * Trigger called to update feed platforms table when new message inserted.
     */
    private static final String UPDATE_FEED_PLATFORMS_ON_NEW_PLATFORM_ADDED = " WHEN (( SELECT COUNT(*) FROM feed_platforms WHERE "
            + FeedPlatformsColumns.Feed_ID + " = " + "new." + Messages.Feed_ID
            + " AND " + FeedPlatformsColumns.PLATFORM_ID + " = " + "new."
            + Messages.PLATFORM_ID + ") == 0) "
            + " BEGIN "
            + "INSERT INTO feed_platforms ("
            + FeedPlatformsColumns.Feed_ID + "," + FeedPlatformsColumns.PLATFORM_ID + ","
            + FeedPlatformsColumns.SNIPPET_TEXT + "," + FeedPlatformsColumns.LAST_MSG_DATE + ","
            + FeedPlatformsColumns.LAST_MSG_TYPE + "," + FeedPlatformsColumns.MESSAGES_COUNT + ","
            + FeedPlatformsColumns.UNREAD_MESSAGES_COUNT + ")"
            + " VALUES (" + "new." + Messages.Feed_ID + "," + "new." + Messages.PLATFORM_ID + ","
            + "new." + Messages.BODY + "," + "new." + Messages.DATE + ","
            + "new." + Messages.TYPE + "," + 1 + "," + "CASE WHEN new.read = 0 THEN 1 ELSE 0 END" + ");"
            + RESET_UNREAD_COUNT_IF_LAST_MESSAGE_IS_OUTGOING
            + " END;";

    private static final String UPDATE_FEED_PLATFORMS_ON_EXIST_PLATFORM_UPDATED = " WHEN (( SELECT COUNT(*) FROM feed_platforms WHERE "
            + FeedPlatformsColumns.Feed_ID + " = " + "new." + Messages.Feed_ID
            + " AND " + FeedPlatformsColumns.PLATFORM_ID + " = " + "new."
            + Messages.PLATFORM_ID + ") > 0) "
            + " BEGIN "
            + "UPDATE feed_platforms SET "
            + FeedPlatformsColumns.SNIPPET_TEXT + "=" + "new." + Messages.BODY + ","
            + FeedPlatformsColumns.LAST_MSG_DATE + "=" + "new." + Messages.DATE + ","
            + FeedPlatformsColumns.LAST_MSG_TYPE + "=" + "new." + Messages.TYPE + ","
            + FeedPlatformsColumns.MESSAGES_COUNT + "=" + FeedPlatformsColumns.MESSAGES_COUNT + "+1" + ","
            + FeedPlatformsColumns.UNREAD_MESSAGES_COUNT + "=" + "(SELECT COUNT(*) FROM messages"
            + " WHERE feed_id = " + "new." + Messages.Feed_ID + " AND read = " + Messages.MSG_UNREAD + ")"
            + " WHERE " + FeedPlatformsColumns.Feed_ID + "=" + "new." + Messages.Feed_ID
            + " AND " + FeedPlatformsColumns.PLATFORM_ID + "=" + "new." + Messages.PLATFORM_ID + ";"
            + RESET_UNREAD_COUNT_IF_LAST_MESSAGE_IS_OUTGOING
            + " END; ";


    private static final String UPDATE_FEED_SET_FEED_AS_NOT_DELETED = " UPDATE feeds SET "
            + Feeds.DELETED + " = " + Feeds.FEED_UNDELETED + ","
            + Feeds.MODIFIED + " = " + Feeds.FEED_MODIFIED
            + " WHERE feeds._id = new." + Messages.Feed_ID
            + " AND " + Feeds.DELETED + "=" + Feeds.FEED_DELETED + ";";
    /**
     * Trigger called automatically when new message inserted.
     */
    private static final String UPDATE_FEED_SNIPPEST_TEXT_READ = "BEGIN"
            + " UPDATE feeds SET " + Feeds.LAST_MSG_DATE + " = new."
            + Messages.DATE + " , " + Feeds.SNIPPET_TEXT + " = new."
            + Messages.BODY + " , " + Feeds.MODIFIED + " = "
            + Feeds.FEED_MODIFIED + " , " + Feeds.LAST_MSG_TYPE + " = new."
            + Messages.TYPE + " , " + Feeds.LAST_PLATFORM + "= new." + Messages.PLATFORM_ID
            + " WHERE feeds._id = new." + Messages.Feed_ID
            + " AND new." + Messages.TYPE + "!=" + MessageType.DRAFT.getId() + " AND "
            + IGNORE_DELETED_MESSAGE_CONDITION + "; "
            + UPDATE_FEED_READ + UPDATE_FEED_COUNT_ON_NEW_MESSAGE +
            UPDATE_FEED_LAST_CALL_TYPE + UPDATE_FEED_SET_FEED_AS_NOT_DELETED + " END;";

    private HeadboxDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * Return a singleton helper for the Headbox database.
     *
     * @param context - activity from which to call the helper.
     * @return HeadboxDatabaseHelper instance.
     */
    static synchronized HeadboxDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new HeadboxDatabaseHelper(context);
        }
        return instance;

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createFeedsTables(db);
    }

    private void createFeedsTables(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + TABLE_MESSAGES + " ("
                + MessagesColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MessagesColumns.SOURCE_ID + " INTEGER,"
                + MessagesColumns.Feed_ID + " INTEGER,"
                + MessagesColumns.CONTACT + " TEXT,"
                + MessagesColumns.CONTACT_COMPLETE + " TEXT,"
                + MessagesColumns.DATE + " INTEGER,"
                + MessagesColumns.PLATFORM_ID + " INTEGER,"
                + MessagesColumns.READ + " INTEGER DEFAULT 0,"
                + MessagesColumns.CALL_DURATION + " INTEGER DEFAULT 0,"
                + MessagesColumns.STATUS + " INTEGER DEFAULT 0,"
                + MessagesColumns.TYPE + " INTEGER," + MessagesColumns.SUBJECT
                + " TEXT," + MessagesColumns.BODY + " TEXT,"
                + MessagesColumns.SEEN + " INTEGER DEFAULT 0" + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DELETED_MESSAGES
                + " (" + MessagesColumns.SOURCE_ID + " INTEGER ," + MessagesColumns.PLATFORM_ID + " INTEGER);");

        db.execSQL("CREATE TABLE " + TABLE_FEEDS + " (" + Feeds._ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Feeds.TYPE + " INTEGER DEFAULT 0,"
                + Feeds.DATE + " INTEGER DEFAULT 0,"
                + Feeds.MESSAGE_COUNT + " INTEGER DEFAULT 0,"
                + Feeds.MEMBERS_IDS + " TEXT,"
                + Feeds.FEED_IDENTIFIER + " TEXT,"
                + Feeds.LAST_MSG_DATE + " INTEGER,"
                + Feeds.SNIPPET_TEXT + " TEXT,"
                + Feeds.UPDATED_TIME + " INTEGER DEFAULT 0,"
                + Feeds.READ + " INTEGER DEFAULT 0,"
                + Feeds.LAST_CALL_TYPE + " INTEGER DEFAULT 0,"
                + Feeds.LAST_PLATFORM + " INTEGER DEFAULT 0,"
                + Feeds.LAST_MSG_TYPE + " INTEGER DEFAULT 0,"
                + Feeds.MODIFIED + " INTEGER DEFAULT 0,"
                + Feeds.DELETED + " INTEGER DEFAULT 0,"
                + Feeds.HAS_ATTACHMENT + " INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE " + TABLE_FEED_PLATFORMS + " ("
                + FeedPlatformsColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FeedPlatformsColumns.Feed_ID + " INTEGER , "
                + FeedPlatformsColumns.PLATFORM_ID + " INTEGER , "
                + FeedPlatformsColumns.SNIPPET_TEXT + " TEXT , "
                + FeedPlatformsColumns.LAST_MSG_DATE + " INTEGER DEFAULT 0 , "
                + FeedPlatformsColumns.LAST_MSG_TYPE + " INTEGER DEFAULT 0 ,"
                + FeedPlatformsColumns.MESSAGES_COUNT + " INTEGER DEFAULT 0 ,"
                + FeedPlatformsColumns.UNREAD_MESSAGES_COUNT + " INTEGER DEFAULT 0 "
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PHONE_CONTACTS + " ("
                + PhoneContacts.ID + " INTEGER,"
                + PhoneContacts.PHONE_CONTACT_COMPLETE_IDENTIFIER + " TEXT,"
                + PhoneContacts.PHONE_CONTACT_IDENTIFIER + " TEXT,"
                + PhoneContacts.PHONE_CONTACT_FIRST_NAME + " TEXT,"
                + PhoneContacts.PHONE_CONTACT_LAST_NAME + " TEXT,"
                + PhoneContacts.PHONE_CONTACT_NAME + " TEXT,"
                + PhoneContacts.PHONE_PICTURE_URI + " TEXT,"
                + PhoneContacts.PHONE_CONTACT_TYPE + " INTEGER,"
                + PhoneContacts.IS_IM_TYPE + " INTEGER);");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FACEBOOK_CONTACTS + " ("
                + FacebookContacts.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FacebookContacts.CONTACT_ID + " TEXT,"
                + FacebookContacts.FIRST_NAME + " TEXT,"
                + FacebookContacts.LAST_NAME + " TEXT,"
                + FacebookContacts.NAME + " TEXT,"
                + FacebookContacts.USER_NAME + " TEXT,"
                + FacebookContacts.PICTURE_URL + " TEXT,"
                + FacebookContacts.COVER_PICTURE_URL + " TEXT,"
                + "UNIQUE (facebook_id) ON CONFLICT REPLACE" + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLATFORMS_CONTACTS + " ("
                + PlatformContacts.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + PlatformContacts.CONTACT_ID + " TEXT,"
                + PlatformContacts.FIRST_NAME + " TEXT,"
                + PlatformContacts.LAST_NAME + " TEXT,"
                + PlatformContacts.NAME + " TEXT,"
                + PlatformContacts.USER_NAME + " TEXT,"
                + PlatformContacts.PICTURE_URL + " TEXT,"
                + PlatformContacts.HEADER_URL + " TEXT,"
                + PlatformContacts.CONTACT_TYPE + " INTEGER);");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GOOGLE_PLUS_CONTACTS + " ("
                + GooglePlusContacts.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + GooglePlusContacts.GOOGLE_PLUS_ID + " TEXT,"
                + GooglePlusContacts.GOOGLE_PLUS_FIRST_NAME + " TEXT,"
                + GooglePlusContacts.GOOGLE_PLUS_LAST_NAME + " TEXT,"
                + GooglePlusContacts.GOOGLE_PLUS_NAME + " TEXT,"
                + GooglePlusContacts.GOOGLE_PLUS_USER_NAME + " TEXT,"
                + GooglePlusContacts.GOOGLE_PLUS_PICTURE_URL + " TEXT,"
                + GooglePlusContacts.GOOGLE_PLUS_COVER_PICTURE_URL + " TEXT);");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GOOGLE_CONTACTS + " ("
                + GoogleContacts.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + GoogleContacts.GOOGLE_ID + " TEXT,"
                + GoogleContacts.PLUS_ID + " TEXT,"
                + GoogleContacts.NAME + " TEXT,"
                + GoogleContacts.PICTURE_URL + " TEXT,"
                + GoogleContacts.COVER_PICTURE_URL + " TEXT);");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_HEADBOX_CONTACTS + " ("
                + HeadboxContacts.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HeadboxContacts.CONTACT_ID + " INTEGER, "
                + HeadboxContacts.FIRST_NAME + " TEXT, "
                + HeadboxContacts.LAST_NAME + " TEXT, "
                + HeadboxContacts.NAME + " TEXT, "
                + HeadboxContacts.PICTURE_URL + " TEXT, "
                + HeadboxContacts.COVER_URL + " TEXT, "
                + HeadboxContacts.COMPLETE_INDENTIFIER + " TEXT, "
                + HeadboxContacts.PRIMARY_INDENTIFIER + " TEXT, "
                + HeadboxContacts.ALTER_INDENTIFIER + " TEXT, "
                + HeadboxContacts.CONTACT_TYPE + " INTEGER, "
                + HeadboxContacts.NOTIFICATION_ENABLED + " INTEGER DEFAULT 0 )");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MERGE_LINKS + " ("
                + MergeLinks.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MergeLinks.FIRST_CONTACT_INDENTIFIER + " TEXT,"
                + MergeLinks.FIRST_CONTACT_TYPE + " TEXT,"
                + MergeLinks.SECOND_CONTACT_INDENTIFIER + " TEXT,"
                + MergeLinks.SECOND_CONTACT_TYPE + " TEXT,"
                + MergeLinks.TYPE + " INTEGER);");

        // To use while searching contacts.
        db.execSQL("CREATE TABLE " + TABLE_HEADBOX_CONTACTS_LOOKUP + "("
                + "token TEXT,"
                + "source INTEGER REFERENCES headbox_contacts(id),"
                + "token_index INTEGER" + ");");

        db.execSQL("CREATE INDEX headbox_contacts_LookupIndex "
                + "ON headbox_contacts_Lookup ("
                + "token," + "source" + ");");

        // Triggers to keep the contactsLookup table up to date
        db.execSQL("CREATE TRIGGER headbox_contacts_Lookup_update "
                + "UPDATE OF headbox_contact_name ON headbox_contacts "
                + "BEGIN "
                + "DELETE FROM headbox_contacts_Lookup WHERE source = new.id;"
                + "SELECT "
                + "_TOKENIZE('headbox_contacts_Lookup', new.id, new.headbox_contact_name,' ',1);"
                + "END");

        db.execSQL("CREATE TRIGGER headbox_contacts_Lookup_insert "
                + "AFTER INSERT ON headbox_contacts "
                + "BEGIN "
                + "SELECT "
                + "_TOKENIZE('headbox_contacts_Lookup', new.id, new.headbox_contact_name,' ',1);"
                + "END");

        // Trigger to completely remove a contacts data when they're deleted

        db.execSQL("CREATE TRIGGER contact_lookup_cleanup DELETE ON headbox_contacts "
                + "BEGIN "
                + "DELETE FROM headbox_contacts_Lookup WHERE source = old.id;"
                + "END");

        // Updates feeds table whenever a new message is inserted.
        db.execSQL("CREATE TRIGGER update_feed_on_insert AFTER INSERT ON messages "
                + UPDATE_FEED_SNIPPEST_TEXT_READ);

        db.execSQL("CREATE TRIGGER update_feed_on_message_delete AFTER DELETE ON messages "
                + "BEGIN" + UPDATE_FEED_COUNT_ON_MESSAGE_DELETED + " END;");

        db.execSQL("CREATE TRIGGER "
                + "update_feed_platforms_on_new_message1 AFTER INSERT ON messages "
                + UPDATE_FEED_PLATFORMS_ON_NEW_PLATFORM_ADDED);

        db.execSQL("CREATE TRIGGER "
                + "update_feed_platforms_on_new_message2 AFTER INSERT ON messages "
                + UPDATE_FEED_PLATFORMS_ON_EXIST_PLATFORM_UPDATED);

        // Updates feeds table whenever a message is updated.
        db.execSQL("CREATE TRIGGER update_feed_read_on_update_message AFTER"
                + " UPDATE OF " + Messages.READ + " ON messages " + "BEGIN "
                + UPDATE_FEED_READ + "END;");

        db.execSQL("CREATE TRIGGER update_feed_platforms_read_on_update_feed AFTER"
                + " UPDATE OF " + Feeds.READ + " ON feeds " + "BEGIN "
                + UPDATE_FEED_PLATFORMS_READ + "END;");

        db.execSQL("CREATE TRIGGER " + " delete_platforms_on_delete_feed "
                + " AFTER DELETE ON feeds" + " BEGIN "
                + "DELETE FROM feed_platforms where feed_id = old._id; "
                + "END");

        db.execSQL("CREATE INDEX hpc_phone_contact_identifier ON "
                + TABLE_PHONE_CONTACTS + "("
                + PhoneContacts.PHONE_CONTACT_IDENTIFIER + ");");

        db.execSQL("CREATE INDEX hfc_user_name ON " + TABLE_FACEBOOK_CONTACTS
                + "(" + FacebookContacts.USER_NAME + ");");

        db.execSQL(HeadboxDBQueries.TRIGGER_UPDATE_FACEBOOK_CONTACT_ON_HEADBOX_CONTACTS_TABLE);
        db.execSQL(HeadboxDBQueries.TRIGGER_UPDATE_PLATFORM_CONTACT_ON_HEADBOX_CONTACTS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        //Temporary. We will manage upgrading scenarios from version to version.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEED_PLATFORMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHONE_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FACEBOOK_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HEADBOX_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HEADBOX_CONTACTS_LOOKUP);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOOGLE_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLATFORMS_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOOGLE_PLUS_CONTACTS);
        db.execSQL("DROP TRIGGER IF EXISTS " + "update_feed_platforms");

        onCreate(db);
        AppUtils.HeadBoxDatabaseUpgraded(getContext());
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase();
    }

    public Context getContext() {
        return context;
    }

}