package com.blinq.provider;

import android.provider.BaseColumns;

/**
 * Contains Data related to the feed operations.
 *
 * @author Johan Hansson.
 */
public class HeadboxFeed {

    /**
     * Columns for the Messages table.
     */
    public interface MessagesColumns extends BaseColumns {

        /**
         * Represent Message ID on its platform. TYPE: STRING
         */
        public static final String SOURCE_ID = "id_source";

        /**
         * Represent Message type: INCOMING/OUTGOING/FAILED/DRAFT/MISSED
         */
        public static final String TYPE = "type";

        /**
         * The feed id of the message. TYPE: INTEGER
         */
        public static final String Feed_ID = "feed_id";

        /**
         * The ID of the sender TYPE: INTEGER (long)
         */
        public static final String SENDER_ID = "sender";

        /**
         * The date the message was received TYPE: INTEGER (long)
         */
        public static final String DATE = "date";
        /**
         * The Platform from which the message sent/received. TYPE : INTEGER
         */
        public static final String PLATFORM_ID = "platform";

        /**
         * Has the message been read. TYPE : INTEGER
         */
        public static final String READ = "read";

        public static final int MSG_READ = 1;
        public static final int MSG_UNREAD = 0;

        /**
         * Indicates whether this message has been seen by the user. TYPE :
         * INTEGER
         */
        public static final String SEEN = "seen";

        /**
         * Message subject. if present. TYPE: TEXT
         */
        public static final String SUBJECT = "subject";

        /**
         * The body of the message. TYPE: TEXT
         */
        public static final String BODY = "body";

        /**
         * The duration of the call in seconds,Only for MessageType.CALL.
         */
        public static final String CALL_DURATION = "duration";

        /**
         * The id of the sender of the feed conversation. TYPE: STRING
         */
        public static final String CONTACT = "contact";
        public static final String CONTACT_COMPLETE = "contact_complete";

        public static final String STATUS = "is_new";
        public static final int OLD_RECORD = 0;
        public static final int NEW_RECORD = 1;

    }

    /**
     * Columns for the Feeds table.
     */
    public interface FeedColumns extends BaseColumns {

        /**
         * Represents the conversation type: SINGLE/GROUP.
         */
        public static final String TYPE = "type";

        public static final int TYPE_SINGLE = 1;
        public static final int TYPE_GROUP = 2;

        /**
         * The date at which the feed was created. Type: INTEGER (long)
         */
        public static final String DATE = "date";

        /**
         * The time where the feed was updated [changed] mainly for facebook inbox.
         */
        public static final String UPDATED_TIME = "updated_time";
        /**
         * Recipient the IDs of the recipients of the message,separated by
         * commas. Type: TEXT
         */
        public static final String MEMBERS_IDS = "members_ids";
        /**
         * The message count of the feed. Type: INTEGER
         */
        public static final String MESSAGE_COUNT = "message_count";
        /**
         * Indicates whether all messages of the thread have been read. Type:
         * INTEGER
         */
        public static final String READ = "read";

        public static final int FEED_READ = 1;
        public static final int FEED_UNREAD = 0;

        /**
         * The snippet text of the latest message in the feed. Type: TEXT
         */
        public static final String SNIPPET_TEXT = "snippet";
        /**
         * The date of the latest message in the feed. Type: TEXT
         */
        public static final String LAST_MSG_DATE = "msg_date";
        /**
         * Indicates whether the latest message is Incoming,Outgoing,Missed.
         */
        public static final String LAST_MSG_TYPE = "msg_type";

        /**
         * Indicates whether this thread contains any attachments. Type: INTEGER
         */
        public static final String HAS_ATTACHMENT = "has_attachment";

        // TODO: change column's name.
        public static final String FEED_IDENTIFIER = "phonenumber";
        /**
         * Indicates whether the latest call is Incoming,Outgoing,Missed.
         */
        public static final String LAST_CALL_TYPE = "last_call_type";
        /**
         * The platform id of the last sent/received message.
         */
        public static final String LAST_PLATFORM = "last_platform_id";

        /**
         * Indicates whether this feed has been modified by the user. TYPE :
         * INTEGER
         */
        public static final String MODIFIED = "modified";
        public static final int FEED_MODIFIED = 1;
        public static final int FEED_UNMODIFIED = 0;

        public static final String DELETED = "deleted";
        public static final int FEED_DELETED = 1;
        public static final int FEED_UNDELETED = 0;

    }

    /**
     * Contain relations between each feed and its connected platform.
     */
    public interface FeedPlatformsColumns extends BaseColumns {
        /**
         * The feed id.
         * <p/>
         * TYPE: INTEGER
         */
        public static final String Feed_ID = "feed_id";

        /**
         * The Platform connected with the feed.
         * <p/>
         * TYPE : INTEGER
         */
        public static final String PLATFORM_ID = "platform";

        /**
         * The Platform snippet text [last message body] with the feed.
         * <p/>
         * TYPE : TEXT
         */
        public static final String SNIPPET_TEXT = "snippet";

        /**
         * The date of the latest message in the feed for a certain platform.
         * <p/>
         * Type: LONG
         */
        public static final String LAST_MSG_DATE = "msg_date";

        /**
         * Indicates whether the latest message is Incoming,Outgoing,Missed for
         * each platform.
         */
        public static final String LAST_MSG_TYPE = "msg_type";

        /**
         * The count of messages under a platform.
         * <p/>
         * TYPE : INTEGER
         */
        public static final String MESSAGES_COUNT = "messages_count";

        /**
         * The unread messages count under a platform.
         * <p/>
         * TYPE : INTEGER
         */
        public static final String UNREAD_MESSAGES_COUNT = "unread_messages_count";

    }

    public interface HeadboxContacts {
        /**
         * Contact row id.
         * <p/>
         * TYPE: INTEGER
         */
        public static final String ID = "id";
        /**
         * Phone contact id
         * <p/>
         * TYPE: INTEGER
         */
        public static final String CONTACT_ID = "phone_contact_id";
        /**
         * Contact first name
         * <p/>
         * TYPE: TEXT
         */
        public static final String FIRST_NAME = "headbox_contact_first_name";
        /**
         * Contact last name
         * <p/>
         * TYPE: TEXT
         */
        public static final String LAST_NAME = "headbox_contact_last_name";
        /**
         * Contact full name
         * <p/>
         * TYPE: TEXT
         */
        public static final String NAME = "headbox_contact_name";
        /**
         * Picture URI/URL.
         */
        public static final String PICTURE_URL = "headbox_picture_url";
        /**
         * Social cover URL.
         */
        public static final String COVER_URL = "headbox_cover_url";
        /**
         * Contact complete identifier [Like: Facebook user name,google user
         * name,phone complete number,etc..]
         */
        public static final String COMPLETE_INDENTIFIER = "headbox_contact_complete_identifier";
        /**
         * Contact normalized identifier [like: phone number without the country
         * code.]
         */
        public static final String PRIMARY_INDENTIFIER = "headbox_contact_primary_identifier";
        /**
         * Contact alternative identifier [Like: Facebook user id]
         */
        public static final String ALTER_INDENTIFIER = "headbox_contact_alternative_identifier";
        /**
         * Contact kind/Platform [Facebook,Hangouts,etc..]
         */
        public static final String CONTACT_TYPE = "headbox_contact_type";

        public static final String NOTIFICATION_ENABLED = "notification_enabled";
        public static final int NOTIFICATION_DISABLED_VALUE = 0;
        public static final int NOTIFICATION_ENABLED_VALUE = 1;

    }

    /***
     *
     * Temporay tables:
     *
     */

    /**
     * Columns for contacts table.
     */
    public interface PhoneContacts {
        public static final String ID = "phone_contact_id";
        public static final String PHONE_CONTACT_COMPLETE_IDENTIFIER = "phone_contact_complete_identifier";
        public static final String PHONE_CONTACT_IDENTIFIER = "phone_contact_identifier";
        public static final String PHONE_CONTACT_FIRST_NAME = "phone_contact_first_name";
        public static final String PHONE_CONTACT_LAST_NAME = "phone_contact_last_name";
        public static final String PHONE_CONTACT_NAME = "phone_contact_name";
        public static final String PHONE_PICTURE_URI = "phone_picture_url";
        public static final String IS_IM_TYPE = "is_im";
        public static final String PHONE_CONTACT_TYPE = "phone_contact_type";

    }

    /**
     * Columns for Facebook contacts table.
     */
    public interface FacebookContacts {
        public static final String ID = "_id";
        public static final String CONTACT_ID = "facebook_id";
        public static final String FIRST_NAME = "facebook_first_name";
        public static final String LAST_NAME = "facebook_last_name";
        public static final String NAME = "facebook_name";
        public static final String USER_NAME = "facebook_user_name";
        public static final String PICTURE_URL = "facebook_picture_url";
        public static final String COVER_PICTURE_URL = "facebook_cover_url";

    }

    /**
     * Columns for Google XMPP contacts table.
     */
    public interface GoogleContacts {
        public static final String ID = "_id";
        public static final String GOOGLE_ID = "google_id";
        public static final String PLUS_ID = "google_plus_user_id";
        public static final String NAME = "google_name";
        public static final String PICTURE_URL = "google_picture_url";
        public static final String COVER_PICTURE_URL = "google_cover_url";

    }

    /**
     * Columns for Google PLUS contacts table.
     */
    public interface GooglePlusContacts {
        public static final String ID = "_id";
        public static final String GOOGLE_PLUS_ID = "google_plus_id";
        public static final String GOOGLE_PLUS_FIRST_NAME = "google_plus_first_name";
        public static final String GOOGLE_PLUS_LAST_NAME = "google_plus_last_name";
        public static final String GOOGLE_PLUS_NAME = "google_plus_name";
        public static final String GOOGLE_PLUS_USER_NAME = "google_plus_user_name";
        public static final String GOOGLE_PLUS_PICTURE_URL = "google_plus_picture_url";
        public static final String GOOGLE_PLUS_COVER_PICTURE_URL = "google_plus_cover_url";
    }

    /**
     * Columns for temporary table holds contacts from different platforms.
     */
    public interface PlatformContacts {

        public static final String ID = "_id";
        public static final String CONTACT_ID = "contact_id";
        public static final String FIRST_NAME = "first_name";
        public static final String LAST_NAME = "last_name";
        public static final String NAME = "name";
        public static final String USER_NAME = "user_name";
        public static final String PICTURE_URL = "picture_url";
        public static final String HEADER_URL = "header_url";
        public static final String CONTACT_TYPE = "contact_type";

    }

    /**
     * Responsible to save merge links to not lose after doing upgrade or any
     * other database operations. By adding links between merged contacts using
     * contacts identifiers.
     */
    public interface MergeLinks {
        public static final String ID = "_id";
        public static final String FIRST_CONTACT_INDENTIFIER = "first_contact_identifier";
        public static final String FIRST_CONTACT_TYPE = "first_contact_type";
        public static final String SECOND_CONTACT_INDENTIFIER = "second_contact_identifier";
        public static final String SECOND_CONTACT_TYPE = "second_contact_type";
        public static final String TYPE = "type";
        public static final int TYPE_LINK = 1;
        public static final int TYPE_BREAK = 2;

    }

    public class Messages implements MessagesColumns {
    }

    public class Feeds implements FeedColumns {

    }
}