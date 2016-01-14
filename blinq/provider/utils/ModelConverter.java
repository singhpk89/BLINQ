package com.blinq.provider.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.blinq.models.Contact;
import com.blinq.models.FeedModel;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MemberContact;
import com.blinq.models.MessageType;
import com.blinq.models.NotificationData;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.module.message.utils.MessageConverter;
import com.blinq.provider.HeadboxFeed.FacebookContacts;
import com.blinq.provider.HeadboxFeed.Feeds;
import com.blinq.provider.HeadboxFeed.GoogleContacts;
import com.blinq.provider.HeadboxFeed.HeadboxContacts;
import com.blinq.provider.HeadboxFeed.Messages;
import com.blinq.provider.HeadboxFeed.PlatformContacts;
import com.blinq.utils.AppUtils;
import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility to convert and map Headbox models to provider objects.
 * <p/>
 * NOTE: Not complete.
 */
public class ModelConverter {

    private ModelConverter() {
    }

    /**
     * Convert contacts from different platforms to contentValues object.
     */
    public static ContentValues convert(Contact contact) {

        ContentValues values = new ContentValues();

        // TODO: change this..
        String suffix = "@" + contact.getContactType().name().toLowerCase()
                + ".com";

        switch (contact.getContactType()) {

            case CALL:

                values.put(HeadboxContacts.CONTACT_ID, contact.getContactId());
                values.put(HeadboxContacts.COMPLETE_INDENTIFIER,
                        contact.getIdentifier());
                values.put(HeadboxContacts.NAME, contact.getName());
                values.put(HeadboxContacts.PRIMARY_INDENTIFIER,
                        contact.getNormalizedIdentifier());
                values.put(HeadboxContacts.ALTER_INDENTIFIER,
                        contact.getIdentifier());
                values.put(HeadboxContacts.CONTACT_TYPE, contact.getContactType()
                        .getId());
                break;

            case FACEBOOK:

                values.put(FacebookContacts.CONTACT_ID, contact.getContactId());
                values.put(FacebookContacts.FIRST_NAME, contact.getFirstName() + "");
                values.put(FacebookContacts.LAST_NAME, contact.getLastName() + "");
                values.put(FacebookContacts.NAME, contact.getName());

                if (StringUtils.isBlank(contact.getUserName())) {
                    values.put(FacebookContacts.USER_NAME, "");
                } else {
                    values.put(FacebookContacts.USER_NAME, contact.getUserName()
                            + StringUtils.FACEBOOK_SUFFIX);
                }

                values.put(FacebookContacts.PICTURE_URL, contact.getPhotoUri()
                        .toString());
                values.put(FacebookContacts.COVER_PICTURE_URL, contact
                        .getCoverUri().toString());
                break;

            case HANGOUTS:

                values.put(GoogleContacts.GOOGLE_ID, contact.getContactId());
                values.put(GoogleContacts.NAME, contact.getName());

                if (StringUtils.isBlank(contact.getAlternativeId())) {
                    values.put(GoogleContacts.PLUS_ID, "");
                    values.put(GoogleContacts.PICTURE_URL, "");
                } else {
                    values.put(GoogleContacts.PLUS_ID, contact.getAlternativeId());
                    String photo = String.format(StringUtils.GOOGLE_PLUS_PHOTO_URL,
                            contact.getAlternativeId());
                    values.put(GoogleContacts.PICTURE_URL, photo);
                }
                if (contact.HasPhoto()) {
                    values.put(GoogleContacts.PICTURE_URL, contact.getPhotoUri()
                            .toString());
                }
                if (contact.HasCover()) {
                    values.put(GoogleContacts.COVER_PICTURE_URL, contact
                            .getCoverUri().toString());
                }
                break;
            case TWITTER:
            case INSTAGRAM:
            case STATICINFO:
                values.put(PlatformContacts.CONTACT_ID, contact.getContactId()
                        + suffix);
                values.put(PlatformContacts.FIRST_NAME, contact.getFirstName() + "");
                values.put(PlatformContacts.LAST_NAME, contact.getLastName() + "");
                values.put(PlatformContacts.NAME, contact.getName());

                if (StringUtils.isBlank(contact.getUserName())) {
                    values.put(PlatformContacts.USER_NAME, "");
                } else {
                    values.put(PlatformContacts.USER_NAME, contact.getUserName()
                            + suffix);
                }
                values.put(PlatformContacts.PICTURE_URL, contact.getPhotoUri()
                        .toString());
                values.put(PlatformContacts.HEADER_URL, contact.getCoverUri()
                        .toString());
                values.put(PlatformContacts.CONTACT_TYPE, contact.getContactType()
                        .getId());
                break;
            default:
                break;
        }
        return values;
    }

    /**
     * Map a message to the content values object to store a set of values that
     * the ContentResolver can process while inserting new message to the
     * database.
     */
    public static ContentValues convert(HeadboxMessage message) {

        ContentValues values = new ContentValues();
        values.put(Messages.SOURCE_ID, message.getSourceId());
        values.put(Messages.PLATFORM_ID, message.getPlatform().getId());
        values.put(Messages.DATE, message.getDate().getTime());
        values.put(Messages.TYPE, message.getType().getId());
        values.put(Messages.BODY, message.getBody());
        values.put(Messages.Feed_ID, message.getFeedId());

        if (MessageType.OUTGOING == message.getType()
                || MessageType.DRAFT == message.getType()) {
            values.put(Messages.READ, Messages.MSG_READ);
        } else {
            values.put(Messages.READ, message.getRead());
        }

        if (message.getPlatform() == Platform.CALL
                && message.getType() == MessageType.INCOMING) {
            values.put(Messages.READ, Messages.MSG_READ);
        }

        values.put(Feeds.FEED_IDENTIFIER, message.getContact().getIdentifier());

        if (message.getPlatform() == Platform.FACEBOOK) {
            values.put(Messages.CONTACT, message.getContact().getIdentifier());
        }
        if (message.getPlatform() == Platform.HANGOUTS) {
            values.put(Messages.CONTACT, message.getContact().getIdentifier());
        } else {
            values.put(Messages.CONTACT, message.getContact()
                    .getNormalizedIdentifier());
        }

        if (message.getPlatform() == Platform.CALL) {
            values.put(Messages.CALL_DURATION, message.getDuration());
        }

        if (message.getContact().getName() != null) {
            values.put(HeadboxContacts.NAME, message.getContact().getName());
        }
        return values;
    }

    /**
     * Map and convert Cursor which result returned by a database query to
     * HeadboxMessage.
     */
    public static HeadboxMessage convertToMessage(Cursor cursor) {

        HeadboxMessage message = new HeadboxMessage();

        message.setBody(cursor.getString(cursor.getColumnIndex(Messages.BODY)));
        message.setMessageId(cursor.getString(cursor
                .getColumnIndex(Messages._ID)));
        message.setSourceId(cursor.getString(cursor
                .getColumnIndex(Messages.SOURCE_ID)));
        message.setPlatform(Platform.fromId(cursor.getInt(cursor
                .getColumnIndex(Messages.PLATFORM_ID))));
        message.setDate(AppUtils.convertToDate(cursor.getLong(cursor
                .getColumnIndex(Messages.DATE))));
        message.setType(MessageType.fromId(cursor.getInt(cursor
                .getColumnIndex(Messages.TYPE))));
        message.setRead(cursor.getInt(cursor.getColumnIndex(Messages.READ)));
        message.setFeedId(cursor.getLong(cursor
                .getColumnIndex(Messages.Feed_ID)));

        String identifier = cursor.getString(cursor
                .getColumnIndex(Messages.CONTACT_COMPLETE));
        Contact contact = new Contact(identifier);
        message.setContact(contact);

        return message;
    }

    /**
     * Convert the notification model to a headbox messaging model.
     */
    public static HeadboxMessage convertToHeadboxMessage(NotificationData notification) {
        return new MessageConverter(null,
                notification.getPlatform()).convert(notification.getTitle(),
                notification.getText(), notification.getReceived(),
                Messages.MSG_UNREAD, MessageType.INCOMING);
    }

    /**
     * Convert Cursor which result returned by a database query to Feed Model.
     */
    public static FeedModel convertToFeed(Cursor cursor) {

        FeedModel feed = new FeedModel();

        feed.setFeedId(cursor.getInt(cursor.getColumnIndex(Feeds._ID)));

        Date date = AppUtils.convertToDate(cursor.getLong(cursor
                .getColumnIndex(Feeds.LAST_MSG_DATE)));
        feed.setLastMessageTime(date);

        feed.setLastMessageBody(cursor.getString(cursor
                .getColumnIndex(Feeds.SNIPPET_TEXT)));

        // Set read true if the Feed has been read , false if not.
        boolean isRead = cursor.getInt(cursor.getColumnIndex(Feeds.READ)) == Feeds.FEED_READ ? true
                : false;
        feed.setRead(isRead);

        // Set modified true if the Feed has been updated , false if not.
        boolean isModified = cursor.getInt(cursor
                .getColumnIndex(Feeds.MODIFIED)) == Feeds.FEED_MODIFIED ? true
                : false;
        feed.setModified(isModified);

        int lastCallType = cursor.getInt(cursor
                .getColumnIndex(Feeds.LAST_CALL_TYPE));
        feed.setLastCallType(MessageType.fromId(lastCallType));

        int messagesCount = cursor.getInt(cursor
                .getColumnIndex(Feeds.MESSAGE_COUNT));
        feed.setMessagesCount(messagesCount);

        int lastMessagePlatform = cursor.getInt(cursor
                .getColumnIndex(Feeds.LAST_PLATFORM));
        feed.setLastMessagePlatform(Platform.fromId(lastMessagePlatform));

        int lastMessageType = cursor.getInt(cursor
                .getColumnIndex(Feeds.LAST_MSG_TYPE));
        feed.setLastMessageType(MessageType.fromId(lastMessageType));


        return feed;
    }

    public static MemberContact convertToMemberContact(Cursor cursor) {

        Platform platform = Platform.fromId(cursor.getInt(cursor
                .getColumnIndex(HeadboxContacts.CONTACT_TYPE)));

        String contactIdentifier = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.COMPLETE_INDENTIFIER));
        String normalizedIdentifier = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.PRIMARY_INDENTIFIER));
        String alternativeIdentifier = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.ALTER_INDENTIFIER));
        String photoURL = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.PICTURE_URL));
        String coverURL = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.COVER_URL));
        String contactName = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.NAME));

        MemberContact contact = new MemberContact(platform, contactIdentifier,
                normalizedIdentifier);
        contact.setContactName(contactName);

        if (platform == Platform.HANGOUTS) {
            contact.setId(alternativeIdentifier);
        } else if (platform != Platform.CALL) {
            contact.setId(convertToOriginalId(normalizedIdentifier, platform));
        }

        if (!StringUtils.isBlank(coverURL)) {
            contact.setHasCover(true);
            contact.setCoverUrl(coverURL);
        } else {
            contact.setHasCover(false);
            contact.setCoverUrl(StringUtils.EMPTY_STRING);
        }

        if (!StringUtils.isBlank(photoURL)) {
            contact.setHasPhoto(true);
            contact.setPhotoUrl(photoURL);
        } else {
            contact.setHasPhoto(false);
            contact.setPhotoUrl(StringUtils.EMPTY_STRING);
        }

        return contact;
    }

    public static String convertToOriginalId(String id, Platform platform) {

        switch (platform) {
            case FACEBOOK:
                return id.substring(0, id.indexOf(StringUtils.FACEBOOK_SUFFIX));
            case HANGOUTS:
                return id;
            case INSTAGRAM:
                return id.substring(0, id.indexOf(StringUtils.INSTAGRAM_SUFFIX));
            case SKYPE:
                return id.substring(0, id.indexOf(StringUtils.SKYPE_SUFFIX));
            case TWITTER:
                return id.substring(0, id.indexOf(StringUtils.TWITTER_SUFFUX));
            case WHATSAPP:
                return id;
            case EMAIL:
                return id;
            default:
                break;
        }
        return "";
    }


    public static Contact convertToHeadboxContact(Cursor cursor) {

        Contact contact = new Contact();

        String id = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.CONTACT_ID));

        String name = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.NAME));

        Platform platform = Platform.fromId(cursor.getInt(cursor
                .getColumnIndex(HeadboxContacts.CONTACT_TYPE)));

        String contactIdentifier = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.COMPLETE_INDENTIFIER));
        String normalizedIdentifier = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.PRIMARY_INDENTIFIER));
        String photoURL = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.PICTURE_URL));
        String coverURL = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.COVER_URL));

        contact.setContactId(id);
        contact.setContactType(platform);
        contact.setIdentifier(contactIdentifier);
        contact.setName(name);
        contact.setNormalizedIdentifier(normalizedIdentifier);
        contact.setPhotoUri(photoURL == null ? Uri.parse("") : Uri.parse(photoURL));
        contact.setCoverUri(coverURL == null ? Uri.parse("") : Uri.parse(coverURL));

        return contact;
    }

    /**
     * Map search query's results to the list of search models.
     *
     * @param cursor - query results.
     */
    public static List<SearchResult> convertMergeResults(Cursor cursor) {

        List<SearchResult> models = new ArrayList<SearchResult>();

        if (cursor != null && cursor.moveToFirst()) {

            do {

                SearchResult model = new SearchResult();

                Contact contact = new Contact();

                contact.setContactId(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.CONTACT_ID)));

                contact.setName(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.NAME)));
                contact.setNormalizedIdentifier(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.PRIMARY_INDENTIFIER)));
                contact.setIdentifier(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.COMPLETE_INDENTIFIER)));

                Uri photoUri = Uri.parse(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.PICTURE_URL)));
                contact.setPhotoUri(photoUri);

                model.setContact(contact);

                String feedId = null;
                try {
                    feedId = cursor.getString(cursor.getColumnIndex(Feeds._ID));
                } catch (Exception e) {
                }

                // check if the feed is exist
                if (!StringUtils.isBlank(feedId)) {

                    model.setHasFeed(true);
                    model.setFeedId(cursor.getString(cursor
                            .getColumnIndex(Feeds._ID)));

                    model.setLastCallType(MessageType.fromId(cursor
                            .getInt(cursor.getColumnIndex(Feeds.LAST_CALL_TYPE))));

                    // Set read true if the Feed has been read , false if not.
                    boolean isRead = cursor.getInt(cursor
                            .getColumnIndex(Feeds.READ)) == Feeds.FEED_READ ? true
                            : false;
                    model.setRead(isRead);

                    int lastMessagePlatform = cursor.getInt(cursor
                            .getColumnIndex(Feeds.LAST_PLATFORM));
                    model.setLastMessagePlatform(Platform
                            .fromId(lastMessagePlatform));

                    Date date = AppUtils.convertToDate(cursor.getLong(cursor
                            .getColumnIndex(Feeds.LAST_MSG_DATE)));
                    model.setLastMessageTime(date);

                } else {
                    model.setHasFeed(false);
                }
                models.add(model);

            } while (cursor.moveToNext());
            cursor.close();
        }

        return models;
    }
}
