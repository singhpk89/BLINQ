package com.blinq.provider.utils;

import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.provider.HeadboxDatabaseHelper;
import com.blinq.provider.HeadboxFeed.Feeds;
import com.blinq.provider.HeadboxFeed.HeadboxContacts;
import com.blinq.provider.HeadboxFeed.MergeLinks;
import com.blinq.provider.HeadboxFeed.MessagesColumns;
import com.blinq.utils.StringUtils;

/**
 * Contains list of SQL statements [Queries and triggers] to be used on Headbox.
 * TODO : Move all queries to this class.
 *
 * @author Johan Hansson.
 */
public class HeadboxDBQueries {


    public static final String FEEDS_INNER_JOIN_CONTACTS = "feeds JOIN headbox_contacts ON " +
            "feeds.members_ids = headbox_contacts.headbox_contact_primary_identifier ";

    public final static String TRIGGER_UPDATE_FACEBOOK_CONTACT_ON_HEADBOX_CONTACTS_TABLE = "CREATE TRIGGER UPDATE_FB_CONTACT AFTER UPDATE ON headbox_facebook_contacts "
            + " BEGIN UPDATE headbox_contacts SET "
            + "headbox_contact_name = NEW.facebook_name,"
            + "headbox_picture_url = NEW.facebook_picture_url,"
            + "headbox_cover_url = NEW.facebook_cover_url "
            + "WHERE headbox_contact_primary_identifier LIKE NEW.facebook_id || '%' AND headbox_contact_type = "
            + Platform.FACEBOOK.getId() + ";END";

    public final static String TRIGGER_UPDATE_GOOGLE_CONTACT_ON_HEADBOX_CONTACTS_TABLE = "CREATE TRIGGER IF NOT EXISTS UPDATE_GOOGLE_CONTACT AFTER UPDATE ON headbox_google_contacts "
            + " BEGIN UPDATE headbox_contacts SET "
            + "headbox_contact_name = NEW.google_name,"
            + "headbox_picture_url = NEW.google_picture_url,"
            + "headbox_cover_url = NEW.google_cover_url "
            + "WHERE headbox_contact_alternative_identifier LIKE NEW.google_plus_user_id || '%'  AND headbox_contact_type ="
            + Platform.HANGOUTS.getId() + " ; END";

    public final static String TRIGGER_UPDATE_PLATFORM_CONTACT_ON_HEADBOX_CONTACTS_TABLE = "CREATE TRIGGER UPDATE_OTHER_CONTACT AFTER UPDATE ON "+ HeadboxDatabaseHelper.TABLE_PLATFORMS_CONTACTS+ " "
            + " BEGIN UPDATE headbox_contacts SET "
            + "headbox_contact_name = NEW.name,"
            + "headbox_picture_url = NEW.picture_url,"
            + "headbox_cover_url = NEW.header_url "
            + "WHERE headbox_contact_primary_identifier LIKE NEW.contact_id || '%' AND headbox_contact_type = NEW.contact_type; END";

    public final static String CREATE_TRIGGER_DELETE_FROM_MESSAGE_TABLE = "CREATE TRIGGER  IF NOT EXISTS delete_messages DELETE ON  "
            + HeadboxDatabaseHelper.TABLE_MESSAGES
            + "  BEGIN "
            + "  Insert Into "
            + HeadboxDatabaseHelper.TABLE_DELETED_MESSAGES
            + "  values ( old."
            + MessagesColumns.SOURCE_ID
            + ",old."
            + MessagesColumns.PLATFORM_ID + ");" + " END";

    public final static String DROP_TRIGGER_DELETE_FROM_MESSAGE_TABLE = "DROP TRIGGER delete_messages;";

    public static final String IGNORE_DELETED_MESSAGE_CONDITION = "id_source  NOT IN ( Select  deleted_messages.id_source from  deleted_messages  where  deleted_messages.id_source=messages.id_source and deleted_messages.platform =messages.platform )";

    /**
     * This query need to bind: platform id and messages id separated by commas.
     */
    public static final String DELETE_CERTAIN_MESSAGES_FROM_PLATFORM = "DELETE FROM messages WHERE platform=%s AND id_source IN ( %s )";

    /**
     * Condition to disable the full name matching with email,skype,and whatsapp
     * contacts.
     */
    private static final String IGNORED_FROM_MERGE = "headbox_contacts.headbox_contact_type NOT IN "
            + "("
            + Platform.EMAIL.getId()
            + ","
            + Platform.WHATSAPP.getId()
            + "," + Platform.SKYPE.getId() + ")";

    /**
     * This query needs platform id.
     */
    public static final String MERGE_CONTACTS_QUERY = "INSERT INTO headbox_contacts "
            + "(phone_contact_id , headbox_contact_name ,headbox_contact_first_name,headbox_contact_last_name, "
            + "headbox_picture_url, headbox_cover_url,headbox_contact_complete_identifier, headbox_contact_primary_identifier, "
            + "headbox_contact_alternative_identifier, headbox_contact_type) "
            + "SELECT DISTINCT phone_contact_id,phone_contact_name,phone_contact_first_name,phone_contact_last_name,phone_picture_url,'', "
            + "phone_contact_complete_identifier,phone_contact_identifier,'', "
            + "phone_contact_type "
            + "FROM headbox_phone_contacts WHERE phone_contact_type = ? "
            + " ORDER BY phone_contact_id,phone_contact_identifier";

    public static final String FACEBOOK_CONTACTS_MATCHING_ON_ID = "INSERT INTO headbox_contacts "
            + "(phone_contact_id , headbox_contact_name ,headbox_contact_first_name,headbox_contact_last_name,"
            + "headbox_picture_url, headbox_cover_url,headbox_contact_complete_identifier, headbox_contact_primary_identifier,"
            + "headbox_contact_alternative_identifier, headbox_contact_type)"
            + "SELECT distinct phone_contact_id, facebook_name,facebook_first_name,facebook_last_name, facebook_picture_url, facebook_cover_url,"
            + "'-' || facebook_id || '@chat.facebook.com', facebook_id || '@facebook.com', facebook_user_name,"
            + Platform.FACEBOOK.getId()
            + " "
            + "FROM headbox_facebook_contacts JOIN headbox_phone_contacts "
            + "ON ( phone_contact_identifier = facebook_user_name and phone_contact_identifier LIKE '%@facebook.com') "
            + "WHERE headbox_phone_contacts.phone_contact_type = "
            + Platform.FACEBOOK.getId();

    public static final String GOOGLE_CONTACTS_MATCHING_ON_ID = "INSERT INTO headbox_contacts "
            + "(phone_contact_id,headbox_contact_name ,headbox_contact_first_name,headbox_contact_last_name, "
            + " headbox_picture_url, headbox_cover_url,headbox_contact_complete_identifier, headbox_contact_primary_identifier,"
            + "headbox_contact_alternative_identifier, headbox_contact_type)"
            + " SELECT phone_contact_id,google_name,google_plus_first_name,google_plus_last_name, google_picture_url, google_cover_url, "
            + " google_id, google_id, google_id,"
            + Platform.HANGOUTS.getId()
            + " FROM headbox_google_contacts LEFT JOIN headbox_google_plus_contacts "
            + " ON headbox_google_plus_contacts.google_plus_id = headbox_google_contacts.google_plus_user_id "
            + " JOIN headbox_phone_contacts ON ( phone_contact_identifier = google_id) WHERE headbox_phone_contacts.phone_contact_type = "
            + Platform.HANGOUTS.getId();

    public static final String FACEBOOK_CONTACTS_MATCHING_ON_FULL_NAME = "INSERT INTO headbox_contacts "
            + "(phone_contact_id , headbox_contact_name ,headbox_contact_first_name,headbox_contact_last_name, "
            + " headbox_picture_url, headbox_cover_url,headbox_contact_complete_identifier, headbox_contact_primary_identifier ,"
            + "headbox_contact_alternative_identifier, headbox_contact_type)"
            + " SELECT distinct phone_contact_id, facebook_name,facebook_first_name,facebook_last_name, facebook_picture_url, facebook_cover_url, "
            + " '-' || facebook_id || '@chat.facebook.com', facebook_id || '@facebook.com', facebook_user_name,"
            + Platform.FACEBOOK.getId()
            + " FROM headbox_facebook_contacts LEFT JOIN headbox_contacts "
            + " ON (lower(headbox_contact_name) = lower(facebook_name)) AND "
            + IGNORED_FROM_MERGE;

    public static final String GOOGLE_CONTACTS_MATCHING_ON_FULL_NAME = "INSERT INTO headbox_contacts "
            + "(phone_contact_id , headbox_contact_name ,headbox_contact_first_name,headbox_contact_last_name, "
            + " headbox_picture_url, headbox_cover_url,headbox_contact_complete_identifier, headbox_contact_primary_identifier,"
            + "headbox_contact_alternative_identifier, headbox_contact_type)"
            + " SELECT phone_contact_id,google_name,google_plus_first_name,google_plus_last_name, google_picture_url, google_cover_url, "
            + " google_id, google_id, google_id,"
            + Platform.HANGOUTS.getId()
            + " FROM headbox_google_contacts LEFT JOIN headbox_google_plus_contacts "
            + " ON headbox_google_plus_contacts.google_plus_id = headbox_google_contacts.google_plus_user_id "
            + " LEFT JOIN headbox_contacts  "
            + " ON ( lower(headbox_contact_name) = lower(google_name)) AND "
            + IGNORED_FROM_MERGE;

    public static final String CONTACTS_MATCHING_ON_FULL_NAME = "INSERT INTO headbox_contacts "
            + "(phone_contact_id , headbox_contact_name ,headbox_contact_first_name,headbox_contact_last_name, "
            + " headbox_picture_url, headbox_cover_url,headbox_contact_complete_identifier, headbox_contact_primary_identifier,"
            + "headbox_contact_alternative_identifier, headbox_contact_type)"
            + " SELECT distinct phone_contact_id, name,first_name,last_name, picture_url, header_url, "
            + " contact_id, contact_id, user_name,?"
            + " FROM " + HeadboxDatabaseHelper.TABLE_PLATFORMS_CONTACTS +" LEFT JOIN headbox_contacts  "
            + " ON (lower(headbox_contact_name) = lower(name)) AND "
            + IGNORED_FROM_MERGE + " WHERE contact_type =?";

    public static final String ADD_MERGE_LINKS_QUERY = "INSERT INTO merge_links "
            + "(first_contact_identifier,first_contact_type,second_contact_identifier,second_contact_type,type) "
            + "SELECT DISTINCT "
            + "h1.headbox_contact_primary_identifier,h1.headbox_contact_type,"
            + "h2.headbox_contact_primary_identifier,h2.headbox_contact_type,"
            + MergeLinks.TYPE_LINK
            + " "
            + "FROM headbox_contacts h1 INNER JOIN headbox_contacts h2 "
            + "ON h1.phone_contact_id != h2.phone_contact_id "
            + "WHERE h1.phone_contact_id = ? AND h2.phone_contact_id = ? AND h2.headbox_contact_type = ?";

    public static final String GENERAL_SEARCH_QUERY = "SELECT DISTINCT _id,msg_date,read,last_call_type,last_platform_id,"
            + "headbox_contacts.id,headbox_contacts.phone_contact_id,headbox_contacts.headbox_contact_name,"
            + "headbox_contacts.headbox_contact_primary_identifier,headbox_contacts.headbox_contact_complete_identifier,"
            + "headbox_contacts.headbox_picture_url,headbox_contacts.headbox_contact_type "
            + "FROM headbox_contacts LEFT JOIN "
            + "(SELECT DISTINCT feeds.*,headbox_contacts.* FROM headbox_contacts JOIN feeds ON "
            + "feeds.members_ids = headbox_contacts.headbox_contact_primary_identifier ) contacts_feeds ON "
            + "contacts_feeds.phone_contact_id = headbox_contacts.phone_contact_id ";

    public static final String GENERAL_SEARCH_QUERY2 = "SELECT DISTINCT _id,msg_date,read,last_call_type,last_platform_id,deleted,contacts.* "
            + "FROM ( SELECT headbox_contacts.id,headbox_contacts.phone_contact_id,headbox_contacts.headbox_contact_name, "
            + "headbox_contacts.headbox_contact_primary_identifier,headbox_contacts.headbox_contact_complete_identifier, "
            + "headbox_contacts.headbox_picture_url,headbox_contacts.headbox_contact_type "
            + "FROM headbox_contacts WHERE headbox_contacts.headbox_contact_type NOT IN (8,10,11) ) contacts "
            + "LEFT JOIN (SELECT DISTINCT feeds.*,headbox_contacts.* FROM headbox_contacts "
            + "JOIN feeds ON feeds.members_ids = headbox_contacts.headbox_contact_primary_identifier "
            + "WHERE headbox_contacts.headbox_contact_type NOT IN (8,10,11) AND feeds.deleted !=" + Feeds.FEED_DELETED + ") "
            + "contacts_feeds ON contacts_feeds.phone_contact_id = contacts.phone_contact_id ";

    public static final String DELETE_DUPLICATE_CONTACTS = "DELETE FROM headbox_phone_contacts "
            + "WHERE rowid IN"
            + "(SELECT U.rowid FROM headbox_phone_contacts AS U "
            + "WHERE ( (select count(*) FROM headbox_phone_contacts "
            + "WHERE phone_contact_identifier = U.phone_contact_identifier AND  phone_contact_id != U.phone_contact_id ) > 1 ) "
            + "AND ( select count(*) FROM headbox_phone_contacts WHERE phone_contact_id = U.phone_contact_id ) = 1 )";

    public static final String DELETE_DUPLICATE_CONTACTS2 = "DELETE FROM headbox_phone_contacts "
            + "WHERE rowid NOT IN "
            + "( SELECT rowid FROM headbox_phone_contacts "
            + "GROUP BY phone_contact_identifier HAVING MAX(rowid) IS NOT NULL)";

    public static String GET_FEED_PLATFORMS = "SELECT "
            + "a.feed_id,a.platform,a.body AS snippet,a.date AS msg_date,a.type AS msg_type,b.messages_count, "
            + "(SELECT COUNT(*) FROM messages WHERE platform = a.platform AND feed_id = ? AND read = 0 ) AS unread_messages_count "
            + "FROM "
            + "messages a "
            + "INNER JOIN "
            + "(SELECT _id, platform,COUNT(*) AS messages_count,max(date) AS max_date "
            + "FROM messages WHERE feed_id = ? GROUP BY platform) AS b ON "
            + "a._id = b._id AND a.date = b.max_date";

    public static final String UPDATE_FEED_PLATFORM_TABLE = "INSERT INTO feed_platforms "
            + "(feed_id,platform,snippet,msg_date,msg_type,messages_count,unread_messages_count) "
            + GET_FEED_PLATFORMS;

    /**
     * Query for the facebook history feeds.
     */
    public static final String GET_FEEDS_FOR_FACEBOOK_MESSAGE_QUERY = " SELECT DISTINCT "
            + "headbox1.headbox_contact_primary_identifier AS headbox_contact_primary_identifier, feeds._id AS _id,feeds.updated_time as updated_time "
            + "FROM headbox_contacts JOIN feeds ON headbox_contacts.headbox_contact_primary_identifier = feeds.members_ids "
            + "JOIN headbox_contacts headbox1 ON headbox1.phone_contact_id = headbox_contacts.phone_contact_id "
            + "WHERE headbox1.headbox_contact_type= "
            + Platform.FACEBOOK.getId()
            + " UNION "
            + "SELECT DISTINCT feeds.members_ids AS headbox_contact_primary_identifier ,feeds._id AS _id,feeds.updated_time as updated_time "
            + "FROM feeds WHERE feeds.members_ids LIKE '%"
            + StringUtils.FACEBOOK_SUFFIX + "'";

    public static final String CONTACTS_JOIN_FEEDS = "headbox_contacts LEFT JOIN feeds ON "
            + "feeds.members_ids = headbox_contacts.headbox_contact_primary_identifier ";
    public static final String FEEDS_JOIN_CONTACTS = "feeds LEFT JOIN headbox_contacts ON "
            + "feeds.members_ids = headbox_contacts.headbox_contact_primary_identifier ";

    public static final String GET_FEED_FOR_INCOMING_MESSAGE_QUERY = "SELECT "
            + HeadboxDatabaseHelper.TABLE_FEEDS + "." + Feeds._ID + " FROM  "
            + HeadboxDatabaseHelper.TABLE_FEEDS + " JOIN "
            + HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS + " ON "
            + HeadboxDatabaseHelper.TABLE_FEEDS + "." + Feeds.MEMBERS_IDS + "="
            + HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS + "."
            + HeadboxContacts.PRIMARY_INDENTIFIER + " AND "
            + HeadboxContacts.CONTACT_ID + " =( SELECT "
            + HeadboxContacts.CONTACT_ID + " FROM "
            + HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS + " WHERE "
            + HeadboxContacts.PRIMARY_INDENTIFIER + " = '" + "%s" + "')";

    public static final String GET_AUTO_MERGED_FACEBOOK_CONTACTS_MATCHING_ON_FULL_NAME_COUNT = " SELECT count(DISTINCT headbox_contact_name)"
            + " FROM headbox_facebook_contacts  JOIN headbox_contacts  "
            + " ON (lower(headbox_contact_name) = lower(facebook_name)) ";

    public static final String GET_AUTO_MERGED_FACEBOOK_CONTACTS_MATCHING_ON_ID_COUNT = "SELECT count(DISTINCT phone_contact_identifier)"
            + "FROM headbox_facebook_contacts JOIN headbox_phone_contacts "
            + "ON ( phone_contact_identifier = facebook_user_name and phone_contact_identifier LIKE '%@facebook.com') "
            + "WHERE headbox_phone_contacts.phone_contact_type = "
            + Platform.FACEBOOK.getId();

    public static final String GET_AUTO_MERGED_GOOGLE_CONTACTS_MATCHING_ON_ID_COUNT = " SELECT count(DISTINCT phone_contact_identifier)"
            + " FROM headbox_google_contacts"
            + " JOIN headbox_phone_contacts ON ( phone_contact_identifier = google_id) WHERE headbox_phone_contacts.phone_contact_type = "
            + Platform.HANGOUTS.getId();

    public static final String GET_AUTO_MERGED_GOOGLE_CONTACTS_MATCHING_ON_FULL_NAME_COUNT = " SELECT count(DISTINCT headbox_contact_name)"
            + " FROM headbox_google_contacts  JOIN headbox_contacts "
            + " ON ( lower(headbox_contact_name) = lower(google_name)) ";

    public static final String GET_AUTO_MERGED_CONTACTS_MATCHING_ON_FULL_NAME_COUNT = " SELECT count(DISTINCT headbox_contact_name)"
            + " FROM "+ HeadboxDatabaseHelper.TABLE_PLATFORMS_CONTACTS+ " JOIN headbox_contacts  "
            + " ON (lower(headbox_contact_name) = lower(name)) WHERE contact_type = ";

    public static final String GET_AUTO_MERGED_CONTACTS_QUERY_COUNT = "SELECT count(*)"
            + "FROM headbox_phone_contacts WHERE phone_contact_type = ";
    public static final String GET_HEADBOX_CONTACTS_COUNT = "SELECT count(*) from headbox_contacts";

    public static final String FILTERED_MODIFIED_FEEDS_QUERY = "SELECT * FROM ( "
            + "SELECT feeds._id, MAX(messages.date) AS msg_date,feeds.members_ids,"
            + "feeds.phonenumber,feeds.read,feeds.modified,feeds.message_count,feeds.last_call_type,feeds.msg_type,feeds.last_platform_id,"
            + "messages.body AS snippet,headbox_contacts.* FROM feeds "
            + "LEFT JOIN headbox_contacts ON "
            + "feeds.members_ids = headbox_contacts.headbox_contact_primary_identifier "
            + "JOIN MESSAGES ON feeds._id = messages.feed_id "
            + "WHERE messages.platform IN ( %1$s ) AND messages.type !="
            + MessageType.DRAFT.getId()
            + " AND messages.id_source "
            + "NOT IN (SELECT deleted_messages.id_source FROM deleted_messages WHERE deleted_messages.platform IN ( %1$s ) ) "
            + "AND feeds.message_count > 0 "
            + "GROUP BY feeds._id HAVING messages.date = MAX(messages.date) "
            + "UNION SELECT feeds._id, feeds.msg_date,feeds.members_ids,feeds.phonenumber,feeds.read,"
            + "feeds.modified,feeds.message_count,feeds.last_call_type,feeds.msg_type,feeds.last_platform_id,"
            + "feeds.snippet,headbox_contacts.* FROM feeds LEFT JOIN headbox_contacts ON "
            + "feeds.members_ids = headbox_contacts.headbox_contact_primary_identifier where feeds.message_count = 0 "
            + ")";

    public static final String FILTERED_FEEDS_QUERY = "SELECT * FROM ( "
            + "SELECT feeds._id, MAX(messages.date) AS msg_date,feeds.members_ids,"
            + "feeds.phonenumber,feeds.read,feeds.modified,feeds.message_count,feeds.last_call_type,feeds.msg_type,feeds.last_platform_id,"
            + "messages.body AS snippet,headbox_contacts.* FROM feeds "
            + "LEFT JOIN headbox_contacts ON "
            + "feeds.members_ids = headbox_contacts.headbox_contact_primary_identifier "
            + "JOIN MESSAGES ON feeds._id = messages.feed_id "
            + "WHERE messages.platform IN ( %1$s ) AND messages.type !="
            + MessageType.DRAFT.getId()
            + " AND messages.id_source "
            + "NOT IN (SELECT deleted_messages.id_source FROM deleted_messages WHERE deleted_messages.platform IN ( %1$s ) ) "
            + "AND feeds.message_count > 0 "
            + "GROUP BY feeds._id HAVING messages.date = MAX(messages.date) "
            + ")";
}
