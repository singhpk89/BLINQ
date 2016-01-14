package com.blinq.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;

import com.google.gson.Gson;
import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.models.Contact;
import com.blinq.models.FeedModel;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MemberContact;
import com.blinq.models.MessageType;
import com.blinq.models.NotificationData;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.provider.HeadboxFeed.FacebookContacts;
import com.blinq.provider.HeadboxFeed.FeedColumns;
import com.blinq.provider.HeadboxFeed.FeedPlatformsColumns;
import com.blinq.provider.HeadboxFeed.Feeds;
import com.blinq.provider.HeadboxFeed.GoogleContacts;
import com.blinq.provider.HeadboxFeed.HeadboxContacts;
import com.blinq.provider.HeadboxFeed.Messages;
import com.blinq.provider.HeadboxFeed.MessagesColumns;
import com.blinq.provider.HeadboxFeed.PlatformContacts;
import com.blinq.provider.utils.ModelConverter;
import com.blinq.service.notification.HeadboxNotificationManager;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements Provider interface and override its methods to deal with feeds
 * provider.
 *
 * @author Johan Hansson
 */
public class FeedProviderImpl implements Provider {

    private static final String TAG = FeedProviderImpl.class.getSimpleName();

    private static final int MESSAGE_ID = 2;
    private static final int FEED_ID = 1;

    private static final String MESSAGES_SORT_ORDER = Messages.DATE + " DESC ";
    private static final String FEEDS_SORT_ORDER = Feeds.LAST_MSG_DATE
            + " DESC ";
    private static final String FEEDS_BY_PLATFORMS_SELECTION_CLAUSE = "feeds._id IN ( SELECT feed_id FROM feed_platforms WHERE %s )";

    /**
     * Where statement used while getting feed messages.
     */
    private final String orderLimitClause = MESSAGES_SORT_ORDER
            + " limit %s offset %s";

    private final String FEEDS_LIMIT_AND_ORDER_BY_CLAUSE = FEEDS_SORT_ORDER
            + " limit %s offset %s";

    private static Provider instance = null;
    private Context context;

    private Uri photoUri;

    private PreferencesManager preferencesManager;

    private FeedProviderImpl(Context context) {
        Log.d(TAG, TAG);
        this.setContext(context);
        this.preferencesManager = new PreferencesManager(context);
    }

    /**
     * Return a singleton helper for the <code>FeedProviderImpl</code>.
     *
     * @param context - activity from which to call the helper.
     * @return Provider instance.
     */
    public static Provider getInstance(Context context) {
        if (instance == null) {
            instance = new FeedProviderImpl(context);
        }
        return instance;

    }

    @Override
    public List<HeadboxMessage> getFeedMessages(long feedId, long offset,
                                                long limit) {

        String ignoreDraft = Messages.TYPE + "!=" + MessageType.DRAFT.getId();
        String orderBy = String.format(orderLimitClause, limit, offset);
        return getFeedMessages(feedId, ignoreDraft, orderBy);
    }

    public List<HeadboxMessage> getFeedUnReadMessages(long feedId) {

        String ignoreDraft = Messages.READ + "=" + Messages.MSG_UNREAD;
        String orderBy = null;
        return getFeedMessages(feedId, ignoreDraft, orderBy);
    }

    @Override
    /**
     * Get list of messages after such message by feed id.
     */
    public List<HeadboxMessage> getMessagesAfter(long feedId, long messageId) {

        String selection = Messages._ID + ">" + messageId;
        String ignoreDraft = Messages.TYPE + "!=" + MessageType.DRAFT.getId();
        selection = DatabaseUtils.concatenateWhere(selection, ignoreDraft);
        String sortOrder = Messages.DATE;
        return getFeedMessages(feedId, selection, sortOrder);

    }

    /**
     * Check if a contact with certain id exist or not.
     */
    @Override
    public boolean contactExists(String contactIdentifier) {

        String selection = HeadboxContacts.PRIMARY_INDENTIFIER + " LIKE ?"
                + " OR " + HeadboxContacts.COMPLETE_INDENTIFIER + " LIKE ?";

        String[] selectionArgs = new String[]{contactIdentifier + "%",
                contactIdentifier + "%"};

        Cursor cursor = context.getContentResolver()
                .query(FeedProvider.CONTACTS_URI, null, selection,
                        selectionArgs, null);

        if (cursor != null && cursor.getCount() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns Map of platform and contacts.
     */
    @Override
    public HashMap<Platform, MemberContact> getAllContacts(long feedId) {

        Uri uri = Uri.withAppendedPath(
                FeedProvider.FEED_LAST_MESSAGE_BY_PLATFORM_URI, feedId + "");

        Cursor cursor = null;
        HashMap<Platform, MemberContact> contacts = new HashMap<Platform, MemberContact>();

        try {

            cursor = context.getContentResolver().query(uri, null, null, null,
                    null);

            if (cursor.moveToFirst()) {
                do {
                    MemberContact contact = ModelConverter
                            .convertToMemberContact(cursor);

                    if (contact != null) {
                        HeadboxMessage message = ModelConverter
                                .convertToMessage(cursor);
                        contact.setIdentifier(message.getContact()
                                .getIdentifier());
                        contacts.put(message.getPlatform(), contact);
                    }
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage() + "");
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return contacts;
    }

    /**
     * Returns draft message.
     *
     * @param feedId - given feed id
     */
    @Override
    public HeadboxMessage getDraftMessage(long feedId) {

        String selection = Messages.TYPE + "=" + MessageType.DRAFT.getId();
        String sortOrder = null;
        HeadboxMessage message = null;

        if (getFeedMessages(feedId, selection, sortOrder).size() > 0) {
            message = getFeedMessages(feedId, selection, sortOrder).get(0);
        }
        return message;
    }

    /**
     * Delete draft messages by feedId.
     */
    @Override
    public int deleteDraftMessage(long feedId) {

        String selection = Messages.TYPE + "=" + MessageType.DRAFT.getId();
        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_MESSAGES_URI, feedId
                + "");
        return context.getContentResolver().delete(uri, selection, null);
    }

    /**
     * Delete message by Id.
     */
    @Override
    public int deleteMessage(long feedId, long messageId) {

        String selection = Messages._ID + "=" + messageId;
        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_MESSAGES_URI, feedId
                + "");
        return context.getContentResolver().delete(uri, selection, null);
    }

    @Override
    public boolean deleteFeed(long feedId) {

        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_URI, feedId + "");
        boolean result = false;
        try {
            result = context.getContentResolver().delete(uri, null, null) > 0 ? true : false;
        } catch (Exception e) {
            Log.d(TAG, "Error while deleting feed " + feedId);
        }
        return result;
    }

    @Override
    public int deleteMessage(String messageSourceId, Platform platform,
                             long feedId) {
        int deleteResult = deleteMessageBySourceId(messageSourceId, platform,
                feedId);

        if (deleteResult > 0) {
            switch (platform) {
                case CALL:
                    CallsManager.deleteCall(context, messageSourceId);
                    break;
                case SMS:
                    SMSManager.deleteSms(context, messageSourceId);
                    break;
                case MMS:
                    SMSManager.deleteMms(context, messageSourceId);
                    break;
                default:
                    break;
            }
        }
        return deleteResult;
    }

    /**
     * Delete message by source Id.
     */

    private int deleteMessageBySourceId(String messageSourceId,
                                        Platform platform, long feedId) {

        Uri uri = Uri.withAppendedPath(FeedProvider.MESSAGES_DELETE_URI,
                platform.getId() + "");
        uri = Uri.withAppendedPath(uri, messageSourceId);
        uri = Uri.withAppendedPath(uri, String.valueOf(feedId));

        return context.getContentResolver().delete(uri, null, null);
    }

    @Override
    public int deleteAllMessages(Platform platform) {
        Uri uri = Uri.withAppendedPath(FeedProvider.ALL_MESSAGES_DELETE_URI,
                platform.getId() + "");
        return context.getContentResolver().delete(uri, null, null);
    }

    /**
     * Retrieves given feed messages from feeds provider.
     *
     * @param feedId - given feed id.
     */
    private List<HeadboxMessage> getFeedMessages(long feedId, String selection,
                                                 String sortOrder) {

        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_MESSAGES_URI, feedId
                + "");

        List<HeadboxMessage> messages = new ArrayList<HeadboxMessage>();
        HeadboxMessage message = null;
        Cursor cursor = null;

        try {

            cursor = context.getContentResolver().query(uri, null, selection,
                    null, sortOrder);

            if (cursor.moveToFirst()) {
                do {
                    message = ModelConverter.convertToMessage(cursor);
                    messages.add(message);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage() + "");
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return messages;
    }

    /**
     * Get List of feeds filtered by list of platforms id.
     *
     * @param platforms - array of integers represents need platforms.
     */
    @Override
    public List<FeedModel> getFeedsByPlatforms(List<Platform> platforms) {

        String selectedPlatforms = null;
        if (platforms != null && platforms.size() != 0) {
            selectedPlatforms = FeedPlatformsColumns.PLATFORM_ID + " IN ("
                    + Platform.convertToString(platforms) + " )";
        }
        //TODO: NEED FIX.
        String selection = String.format(FEEDS_BY_PLATFORMS_SELECTION_CLAUSE,
                selectedPlatforms);

        return getFeeds(FeedProvider.FEED_LIST_URI, selection, FEEDS_SORT_ORDER);
    }

    /**
     * Map search query's results to the list of search models.
     *
     * @param cursor - query results.
     */
    public List<SearchResult> convertToSearchResult(Cursor cursor) {

        List<SearchResult> models = new ArrayList<SearchResult>();
        HashMap<String, Integer> contacts = new HashMap<String, Integer>();
        HashMap<String, Integer> contactsIndentifiers = new HashMap<String, Integer>();

        if (cursor != null && cursor.moveToFirst()) {

            do {

                SearchResult model = new SearchResult();

                Contact contact = new Contact();
                // Could be empty.
                contact.setContactId(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.CONTACT_ID)));

                contact.setName(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.NAME)));
                contact.setNormalizedIdentifier(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.PRIMARY_INDENTIFIER)));
                contact.setIdentifier(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.COMPLETE_INDENTIFIER)));
                if (cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.PICTURE_URL)) != null) {
                    Uri photoUri = Uri.parse(cursor.getString(cursor
                            .getColumnIndex(HeadboxContacts.PICTURE_URL)));
                    contact.setPhotoUri(photoUri);
                }

                model.setContact(contact);

                String feedId = null;
                try {
                    feedId = cursor.getString(cursor.getColumnIndex(Feeds._ID));
                } catch (Exception e) {
                }
                String contactId = model.getContact().getContactId();

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

                    List<MemberContact> memberContacts = getContactDetailsByFeed(
                            Long.valueOf(model.getFeedId()), null);
                    Uri photoUri = handleContactPhoto(memberContacts);
                    contact.setPhotoUri(photoUri);

                } else {

                    model.setHasFeed(false);
                    List<MemberContact> memberContacts = getContactIdentifiers(contact
                            .getContactId());

                    if (memberContacts != null) {
                        Uri photoUri = handleContactPhoto(memberContacts);
                        contact.setPhotoUri(photoUri);
                    } else {
                        contact.setPhotoUri(Uri.parse(""));
                    }
                }

                // Get contact's phone number if exist

                Platform contactPlatform = Platform.fromId(cursor.getInt(cursor
                        .getColumnIndex(HeadboxContacts.CONTACT_TYPE)));

                if (contactPlatform == Platform.CALL) {
                    model.setPhoneNumber(model.getContact().getIdentifier());
                    model.setHasPhoneNumber(true);
                } else {
                    model.setHasPhoneNumber(false);
                }

                // Handle duplicate Search results.

                if (StringUtils.isBlank(contactId)) {

                    String contactIdentifier = model.getContact()
                            .getIdentifier();
                    if (!contactsIndentifiers.containsKey(contactIdentifier)) {
                        models.add(model);
                        contactsIndentifiers.put(contactIdentifier,
                                models.size() - 1);
                    }

                } else if (!contacts.containsKey(contactId)) {
                    models.add(model);
                    contacts.put(contactId, models.size() - 1);

                } else if (contacts.containsKey(contactId) && model.HasFeed()) {
                    models.set(contacts.get(contactId), model);

                } else if (contacts.containsKey(contactId) && !model.HasFeed()) {
                    // models.set(duplicates.get(contactId), model);
                }

            } while (cursor.moveToNext());
            cursor.close();
        }
        return models;
    }



    /**
     * Get List of feeds filtered by list of platforms id with offset and limit.
     * to use while loading the feed view.
     */
    @Override
    public List<FeedModel> getFeeds(long offset, long limit,
                                    List<Platform> platforms) {

        String selectedPlatforms = null;
        if (platforms != null && platforms.size() != 0) {
            selectedPlatforms = FeedPlatformsColumns.PLATFORM_ID + " IN ("
                    + Platform.convertToString(platforms) + " )";
        }
        //TODO: Need fixes.
        String selection = Feeds.MESSAGE_COUNT + " > 0 ";
        String orderBy = String.format(FEEDS_LIMIT_AND_ORDER_BY_CLAUSE, limit,
                offset);

        return getFeeds(FeedProvider.FEED_LIST_URI, selection, orderBy);
    }

    @Override
    public List<FeedModel> getFeedsFor(Platform platform, long offset,
                                       long limit) {

        Uri uri = ContentUris.withAppendedId(FeedProvider.FILTERED_FEEDS_URI,
                platform.getId());
        String orderBy = String.format(FEEDS_LIMIT_AND_ORDER_BY_CLAUSE, limit,
                offset);
        return getFeeds(uri, null, orderBy);
    }

    @Override
    public List<FeedModel> getModifiedFeedsFor(Platform platform) {

        Uri uri = ContentUris.withAppendedId(FeedProvider.FILTERED_FEEDS_URI,
                platform.getId());
        String selection = Feeds.MODIFIED + "=" + Feeds.FEED_MODIFIED;
        return getFeeds(uri, selection, FEEDS_SORT_ORDER);
    }

    @Override
    public FeedModel getFeedFor(Platform platform, long feedId) {

        if (feedId == 0)
            return null;

        Uri uri = ContentUris.withAppendedId(FeedProvider.FILTERED_FEEDS_URI,
                platform.getId());
        String selection = Feeds._ID + "=" + feedId;

        List<FeedModel> models = getFeeds(uri, selection, FEEDS_SORT_ORDER);
        if (models.size() > 0)
            return models.get(0);

        return null;
    }

    @Override
    public FeedModel getFeed(long feedId, Platform platform) {

        String selectedPlatform = null;
        String platformsSelection = null;

        if (platform != Platform.ALL) {
            selectedPlatform = Messages.PLATFORM_ID + "=" + platform.getId();
            platformsSelection = String.format(
                    FEEDS_BY_PLATFORMS_SELECTION_CLAUSE, selectedPlatform);
        }

        String feedSelection = Feeds._ID + "=" + feedId;
        String selection = DatabaseUtils.concatenateWhere(feedSelection, platformsSelection);

        List<FeedModel> models = getFeeds(FeedProvider.FEED_LIST_URI,
                selection, FEEDS_SORT_ORDER);
        if (models.size() > 0)
            return models.get(0);

        return null;
    }

    /**
     * Get list contact identifiers related to the feed.
     */
    @Override
    public HashMap<Platform, List<MemberContact>> getContacts(long feedId) {

        String selection = null;
        List<MemberContact> memberContacts = getMergedContactsByFeedId(
                feedId, selection);

        HashMap<Platform, List<MemberContact>> map = new HashMap<Platform, List<MemberContact>>();

        // convert to map.
        for (MemberContact memberContact : memberContacts) {
            if (map.get(memberContact.getType()) != null) {
                List<MemberContact> contacts = map.get(memberContact.getType());
                contacts.add(memberContact);
                map.put(memberContact.getType(), contacts);
            } else {
                List<MemberContact> contacts = new ArrayList<MemberContact>();
                contacts.add(memberContact);
                map.put(memberContact.getType(), contacts);
            }
        }
        if (map.containsKey(Platform.CALL)) {
            map.put(Platform.SMS, map.get(Platform.CALL));
        }

        return map;
    }

    @Override
    public List<MemberContact> getFeedMergedContacts(long feedId,
                                                     Platform platform) {

        String selection = HeadboxContacts.CONTACT_TYPE + "="
                + platform.getId();
        return getMergedContactsByFeedId(feedId, selection);
    }

    /**
     * Get last mobile/phone number from given feed's messages.
     */
    @Override
    public String getLastPhoneNumber(long feedId) {

        if (feedId == 0) {
            Log.e(TAG, "getLastPhoneNumber(),Wrong feedId.");
            return null;
        }

        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_MESSAGES_URI, feedId
                + "");

        String selection = Messages.PLATFORM_ID + "=" + Platform.CALL.getId()
                + " OR " + Messages.PLATFORM_ID + "=" + Platform.SMS.getId();
        String sortOrder = MESSAGES_SORT_ORDER + " LIMIT " + 1;

        Cursor cursor = context.getContentResolver().query(uri, null,
                selection, null, sortOrder);

        String phoneNumber = null;
        if (cursor.moveToFirst()) {
            do {
                phoneNumber = cursor.getString(cursor
                        .getColumnIndex(Messages.CONTACT_COMPLETE));

            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
        return phoneNumber;
    }

    /**
     * Get Contact details.
     */
    private List<MemberContact> getContactDetailsByFeed(long feedId,
                                                        String selection) {

        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_MEMBER_CONTACTS_URI,
                feedId + "");
        Cursor cursor = null;
        List<MemberContact> contacts = null;

        try {

            cursor = context.getContentResolver().query(uri, null, selection,
                    null, null);

            if (cursor.moveToFirst()) {

                contacts = new ArrayList<MemberContact>();

                do {

                    MemberContact contact = ModelConverter
                            .convertToMemberContact(cursor);
                    contacts.add(contact);

                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG,
                    "Exception on getContactDetailsByFeed() method = "
                            + e.getMessage() + ""
            );
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contacts;
    }

    private List<MemberContact> getMergedContactsByFeedId(long feedId,
                                                          String selection) {

        List<MemberContact> contacts = getContactDetailsByFeed(feedId,
                selection);

        if (contacts != null && contacts.size() > 0) {

            return contacts;

        } else {

            // In case if we don't have saved contact for this feed.
            // so will get the contact information from feed itself.
            FeedModel model = getFeed(feedId);
            contacts = new ArrayList<MemberContact>();

            if (model == null || model.getContact() == null)
                return new ArrayList<MemberContact>();

            String contactIdentifier = model.getContact().getIdentifier();

            String photoUrl = (model.getContact().getPhotoUri() != null) ? model
                    .getContact().getPhotoUri().toString()
                    : "";
            String coverUrl = (model.getContact().getCoverUri() != null) ? model
                    .getContact().getCoverUri().toString()
                    : "";

            Platform platform = Platform.NOTHING;

            if (contactIdentifier.contains(StringUtils.FACEBOOK_SUFFIX)
                    || contactIdentifier
                    .contains(StringUtils.FACEBOOK_CHAT_SUFFIX)) {
                platform = Platform.FACEBOOK;
            } else if (contactIdentifier
                    .contains(StringUtils.GOOGLE_TALK_SUFFIX)
                    || contactIdentifier.contains(StringUtils.GOOGLE_SUFFIX)) {
                platform = Platform.HANGOUTS;
            } else if (contactIdentifier.contains(StringUtils.SKYPE_SUFFIX)) {
                platform = Platform.SKYPE;
            } else if (contactIdentifier.contains(StringUtils.WHATSAPP_SUFFIX)) {
                platform = Platform.WHATSAPP;
            } else if (StringUtils.validateEmailAddress(contactIdentifier)) {
                platform = Platform.EMAIL;
            } else {
                platform = Platform.CALL;
            }

            MemberContact contact = new MemberContact(platform,
                    contactIdentifier);

            if (!StringUtils.isBlank(coverUrl)) {
                contact.setHasCover(true);
                contact.setCoverUrl(coverUrl);
            } else {
                contact.setHasCover(false);
                contact.setCoverUrl(StringUtils.EMPTY_STRING);
            }

            if (!StringUtils.isBlank(photoUrl)) {
                contact.setHasPhoto(true);
                contact.setPhotoUrl(photoUrl);
            } else {
                contact.setHasPhoto(false);
                contact.setPhotoUrl(StringUtils.EMPTY_STRING);
            }

            contacts.add(contact);
        }

        return contacts;
    }

    @Override
    public List<MemberContact> getContactIdentifiers(Platform platform,
                                                     String contactId) {
        String selection = HeadboxContacts.CONTACT_TYPE + "="
                + platform.getId();
        return getContactIdentifiers(contactId, selection);
    }

    @Override
    public List<MemberContact> getContactIdentifiers(String contactId) {
        String selection = null;
        return getContactIdentifiers(contactId, selection);
    }

    private List<MemberContact> getContactIdentifiers(String contactId,
                                                      String selection) {

        Uri uri = Uri.withAppendedPath(FeedProvider.CONTACT_PHONES_EMAILS_URI,
                contactId + "");
        Cursor cursor = null;
        List<MemberContact> contacts = null;

        try {

            cursor = context.getContentResolver().query(uri, null, selection,
                    null, null);

            if (cursor != null)
                if (cursor.moveToFirst()) {

                    contacts = new ArrayList<MemberContact>();
                    do {
                        MemberContact contact = ModelConverter
                                .convertToMemberContact(cursor);
                        contacts.add(contact);
                    } while (cursor.moveToNext());
                }
        } catch (Exception e) {
            Log.e(TAG,
                    "Such exception happened on getContactIdentifiers() method = "
                            + e.getMessage() + ""
            );
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return contacts;
    }

    @Override
    /**
     * Get List of unread feeds filtered by list of platforms id.
     *
     * @param platforms
     *            - array of integers represents need platforms.
     */
    public List<FeedModel> getUnreadFeeds(List<Platform> platforms) {

        String selectedPlatforms = null;
        if (platforms != null && platforms.size() != 0) {
            selectedPlatforms = Messages.PLATFORM_ID + " IN ("
                    + Platform.convertToString(platforms) + " )";
        }
        String readSelection = Feeds.READ + "=" + Feeds.FEED_UNREAD;
        String platformSelection = String.format(
                FEEDS_BY_PLATFORMS_SELECTION_CLAUSE, selectedPlatforms);
        String selection = DatabaseUtils.concatenateWhere(platformSelection,
                readSelection);
        return getFeeds(FeedProvider.FEED_LIST_URI, selection, FEEDS_SORT_ORDER);
    }

    /**
     * Get list of modified feeds filtered by platforms.
     */
    @Override
    public List<FeedModel> getModifiedFeeds() {

        String selection = Feeds.MODIFIED + "="
                + Feeds.FEED_MODIFIED;

        return getFeeds(FeedProvider.FEED_LIST_URI, selection, FEEDS_SORT_ORDER);
    }

    /**
     * Mark given feeds as unmodified.
     */
    @Override
    public int setFeedsAsUnmodified(List<FeedModel> feeds) {

        String selectedFeeds = null;
        if (feeds != null && feeds.size() != 0) {

            Integer[] ids = getFeedsIds(feeds);
            selectedFeeds = Feeds._ID + " IN ("
                    + StringUtils.convertToString(ids) + " )";
            ContentValues values = new ContentValues(1);
            values.put(Feeds.MODIFIED, Feeds.FEED_UNMODIFIED);

            ContentResolver cr = context.getContentResolver();
            int result = 0;
            try {
                result = cr.update(FeedProvider.FEEDS_URI, values,
                        selectedFeeds, null);
            } catch (Exception e) {
                Log.v(TAG, "Error marking feeds as unmodified.");
            }

            if (result > 0) {
                Log.d(TAG, "Feeds " + selectedFeeds
                        + "  are marked as unmodified, affected feeds are = "
                        + result);
            } else {
                Log.d(TAG, "Feeds already marked as unmodified");
            }

            return result;
        }
        return 0;
    }

    /**
     * Take list of feeds and return list of ids represent these feeds.
     */
    private Integer[] getFeedsIds(List<FeedModel> feeds) {

        Integer[] ids = new Integer[feeds.size()];
        int counter = 0;
        for (FeedModel feedModel : feeds) {
            ids[counter++] = feedModel.getFeedId();
        }
        return ids;
    }

    /**
     * Insert new message to the database.
     *
     * @param message -message to insert.
     */
    public Uri insertMessage(boolean showNotification, final HeadboxMessage message) {

        if (!AppUtils.isAppLoaded(context))
            return null;

        Uri uri = null;

        ContentValues values = ModelConverter.convert(message);

        uri = context.getContentResolver().insert(
                FeedProvider.INSERT_MESSAGE_URI, values);

        if (uri == null || message.getType() == MessageType.DRAFT)
            return uri;

        Log.d(TAG, "Insert " + message.getPlatform().name()
                + " message , uri: " + uri.toString());

        String messageId = uri.getPathSegments().get(MESSAGE_ID);
        final String feedId = uri.getPathSegments().get(FEED_ID);

        AppUtils.sendRefreshBroadcast(context, messageId, feedId);

        if (showNotification)
            HeadboxNotificationManager.displayNotification(context,
                    message, null, Integer.parseInt(feedId));

        return uri;
    }

    @Override
    public Uri insertMessage(final HeadboxMessage message) {
        boolean showNotification = message.getPlatform() != Platform.FACEBOOK
                && message.getPlatform() != Platform.HANGOUTS;
        return insertMessage(showNotification, message);
    }

    @Override
    public Uri insertNotification(final NotificationData notificationData) {

        HeadboxMessage message = ModelConverter.convertToHeadboxMessage(notificationData);
        return insertMessage(false, message);
    }

    /**
     * Change message type to the given type.
     *
     * @param uri - the message to move
     * @param -   message platform.
     * @param -   message type.
     * @return true if the operation succeeded
     */
    @Override
    public boolean changeMessageType(Uri uri, Platform platform,
                                     MessageType type) {

        if (uri == null) {
            return false;
        }

        boolean markAsUnread = false;
        boolean markAsRead = false;

        switch (type) {

            case INCOMING:
            case DRAFT:
                break;
            case OUTGOING:
                markAsRead = true;
                break;
            case FAILED:
            case QUEUED:
                markAsUnread = true;
                break;
            default:
                return false;
        }

        ContentValues values = new ContentValues(2);
        values.put(Messages.TYPE, type.getId());
        if (markAsUnread) {
            values.put(Messages.READ, Messages.MSG_UNREAD);
        } else if (markAsRead) {
            values.put(Messages.READ, Messages.MSG_READ);
        }

        String messageId = uri.getLastPathSegment();
        String selection = Messages.SOURCE_ID + "=? AND "
                + Messages.PLATFORM_ID + "=?";
        String[] selectionArgs = new String[]{messageId,
                platform.getIdAsString()};

        Uri messageUri = Uri.withAppendedPath(
                FeedProvider.MESSAGES_CONTENT_URI, String.valueOf(messageId));

        int result = context.getContentResolver().update(messageUri, values,
                selection, selectionArgs);

        if (result > 0) {
            Log.d(TAG, platform.name() + " message of source id = " + messageId
                    + " has been changed to " + type.name() + " type.");
        }
        return result > 0;
    }

    @Override
    public Uri insertContact(Contact contact) {

        ContentValues values = ModelConverter.convert(contact);
        Uri uri = context.getContentResolver().insert(
                ContentUris.withAppendedId(FeedProvider.GENERAL_CONTACTS_URI,
                        contact.getContactType().getId()), values
        );

        return uri;
    }

    @Override
    public int UpdateContact(Contact contact) {

        ContentValues values = ModelConverter.convert(contact);
        String selection = buildContactSelection(contact);
        String[] selectionArgs = new String[]{contact.getContactId() + "%"};

        int updatedContacts = context.getContentResolver().update(
                ContentUris.withAppendedId(FeedProvider.GENERAL_CONTACTS_URI,
                        contact.getContactType().getId()), values, selection,
                selectionArgs
        );

        if (updatedContacts > 0) {
            Log.d(TAG, "#of updated contacts is = " + updatedContacts);
        } else {
            Log.e(TAG,
                    "Unable to update contact, id= " + contact.getContactId());
        }

        return updatedContacts;
    }

    private String buildContactSelection(Contact contact) {

        String selection = null;

        switch (contact.getContactType()) {
            case FACEBOOK:
                selection = FacebookContacts.CONTACT_ID + " LIKE ?";
                break;
            case HANGOUTS:
                selection = GoogleContacts.PLUS_ID + " LIKE ?";
                break;
            case INSTAGRAM:
            case TWITTER:
                selection = PlatformContacts.CONTACT_ID + " LIKE ?";
                break;
            default:
                break;
        }
        return selection;
    }

    @Override
    public int insertLogsHistory(int count) {

        int insertedRows = 0;
        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_HISTORY_URI,
                Uri.encode(count + ""));
        insertedRows = context.getContentResolver().bulkInsert(uri, null);

        return insertedRows;
    }

    @Override
    public List<Platform> getFeedPlatforms(long feedId) {

        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_PLATFORMS_URI, feedId
                + "");
        List<Platform> platforms = new ArrayList<Platform>();
        Cursor cursor = null;
        try {

            cursor = context.getContentResolver().query(uri, null, null, null,
                    null);

            if (cursor.moveToFirst()) {
                do {
                    platforms.add(Platform.fromId(cursor.getInt(cursor
                            .getColumnIndex(Messages.PLATFORM_ID))));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Such exception happened on getFeedPlatforms() method = "
                            + e.getMessage() + ""
            );
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return platforms;
    }

    /**
     * Get list of platforms have unread messages for a given feed.
     */
    @Override
    public Map<Platform, Integer> getFeedUnReadPlatforms(long feedId) {

        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_UNREAD_PLATFORMS_URI,
                feedId + "");
        Map<Platform, Integer> platforms = new HashMap<Platform, Integer>();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null,
                    null);
            if (cursor.moveToFirst()) {
                do {
                    platforms.put(Platform.fromId(cursor.getInt(cursor
                            .getColumnIndex(Messages.PLATFORM_ID))), cursor.getInt(cursor
                            .getColumnIndex(FeedPlatformsColumns.UNREAD_MESSAGES_COUNT)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Such exception happened on getFeedUnReadPlatforms() method = "
                            + e.getMessage() + ""
            );
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return platforms;
    }

    @Override
    public Map<Integer, List<Platform>> getFeedUnReadPlatforms() {

        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_UNREAD_PLATFORMS_URI,
                0 + "");
        Map<Integer, List<Platform>> platforms = new HashMap<Integer, List<Platform>>();
        Cursor cursor = null;

        try {

            cursor = context.getContentResolver().query(uri, null, null, null,
                    null);

            if (cursor.moveToFirst()) {
                do {
                    Platform platform = Platform.fromId(cursor.getInt(cursor
                            .getColumnIndex(Messages.PLATFORM_ID)));
                    int feedId = cursor.getInt(cursor
                            .getColumnIndex(Messages.Feed_ID));

                    List<Platform> feedPlatforms = platforms.get(feedId);

                    if (feedPlatforms == null) {
                        feedPlatforms = new ArrayList<Platform>();
                        feedPlatforms.add(platform);
                    } else {
                        feedPlatforms.add(platform);
                    }
                    platforms.put(feedId, feedPlatforms);

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Such exception happened on getFeedUnReadPlatforms() method = "
                            + e.getMessage() + ""
            );
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return platforms;
    }

    @Override
    public int createEmptyFeed(String contactId, String contactIdentifier) {

        //TODO: This method needs better implementation.

        List<MemberContact> contacts = null;

        if (!StringUtils.isBlank(contactId)) {
            contacts = getContactIdentifiers(contactId);
        }

        Contact contact = null;

        if (contacts != null) {
            contact = new Contact(contacts.get(0).getIdentifier());
            contact.setNormalizedIdentifier(contacts.get(0).getNormalizedIdentifier());
        } else {
            contact = new Contact(contactIdentifier);
        }


        HeadboxMessage dummyMessage = new HeadboxMessage();
        dummyMessage.setDate(new Date());
        dummyMessage.setRead(Messages.MSG_READ);
        dummyMessage.setBody(StringUtils.EMPTY_STRING);
        dummyMessage.setPlatform(Platform.NOTHING);
        dummyMessage.setType(MessageType.DRAFT);
        dummyMessage.setContact(contact);

        Uri uri = insertMessage(dummyMessage);
        int feedId = Integer.parseInt(uri.getPathSegments().get(1));

        deleteDraftMessage(feedId);

        return feedId;
    }

    /**
     * @return list of feeds
     */
    public List<FeedModel> getFeeds(Uri uri, String selection, String sortOrder) {

        FeedModel feed = null;
        List<FeedModel> feeds = new ArrayList<FeedModel>();

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, selection,
                    null, sortOrder);
            if (cursor.moveToFirst()) {
                do {
                    feed = buildFeedModel(cursor);
                    feeds.add(feed);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Such exception happened on getFeeds() method = "
                            + e.getMessage() + ""
            );
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return feeds;
    }

    /**
     * Get specific feed information.
     */
    @Override
    public FeedModel getFeedById(long feedId, String selection) {

        FeedModel feed = null;
        Uri uri = Uri.withAppendedPath(FeedProvider.FEED_URI, feedId + "");

        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(uri, null,
                    selection, null, null);
            if (cursor.moveToFirst()) {
                feed = buildFeedModel(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Such exception happened on getFeedById() method = "
                            + e.getMessage() + ""
            );
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return feed;
    }

    @Override
    public FeedModel getFeed(long feedId) {
        String selection = null;
        return getFeedById(feedId, selection);
    }

    /**
     * Get Message using its id.
     */
    @Override
    public HeadboxMessage getMessage(long messageId) {

        if (messageId <= 0)
            return null;

        Uri uri = ContentUris.withAppendedId(FeedProvider.MESSAGES_CONTENT_URI,
                messageId);

        String selection = MessagesColumns._ID + " = " + messageId;

        Cursor cursor = null;
        cursor = getContext().getContentResolver().query(uri, null, selection,
                null, null);

        HeadboxMessage message = getMessageFromCursor(cursor);

        return message;
    }

    @Override
    public HeadboxMessage getMessage(Platform platform, String sourceId) {

        if (sourceId == null)
            return null;

        Uri uri = Uri.withAppendedPath(FeedProvider.MESSAGES_CONTENT_URI,
                sourceId);

        String selection = Messages.SOURCE_ID + " =? AND "
                + Messages.PLATFORM_ID + "=?";
        String[] selectionArgs = new String[]{sourceId,
                platform.getIdAsString()};

        Cursor cursor = null;
        HeadboxMessage message = null;
        try {
            cursor = getContext().getContentResolver().query(uri, null,
                    selection, selectionArgs, null);
            message = getMessageFromCursor(cursor);

        } catch (Exception e) {
        }

        return message;
    }

    private HeadboxMessage getMessageFromCursor(Cursor cursor) {

        HeadboxMessage message = null;
        try {
            if (cursor.moveToFirst()) {
                message = ModelConverter.convertToMessage(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Such exception happened on getMessageById() method = "
                    + e.getMessage() + "");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return message;

    }

    /**
     * Marks a specific feed messages as read - all messages in the feed will be
     * marked read
     */
    @Override
    public void markFeedAsRead(Context context, long feedId) {

        if (feedId > 0) {

            // markMessagesAsReadOnPlatform(feedId);
            ContentValues values = new ContentValues(1);
            values.put(Messages.READ, Messages.MSG_READ);

            ContentResolver cr = context.getContentResolver();
            int result = 0;
            try {
                // We just need to mark unread messages as read not all
                // messages.
                String where = Messages.READ + "!=" + Messages.MSG_READ;
                result = cr.update(ContentUris.withAppendedId(
                                FeedProvider.FEED_MESSAGES_URI, feedId), values, where,
                        null
                );
            } catch (Exception e) {
                Log.e(TAG, "Error while marking feed #" + feedId + " as read.");
            }

            if (result > 0) {
                Log.d(TAG, "Feed #" + feedId + " marked as read.");
            }
        }
    }

    public void markMessagesAsReadOnPlatform(long feedId) {

        List<HeadboxMessage> messages = getFeedUnReadMessages(feedId);

        for (HeadboxMessage headboxMessage : messages) {

            if (headboxMessage.getPlatform() == Platform.SMS) {
                SMSManager.markAsRead(context,
                        new String[]{headboxMessage.getSourceId()});
            } else if (headboxMessage.getPlatform() == Platform.CALL) {
                CallsManager.markAsRead(context,
                        new String[]{headboxMessage.getSourceId()});
            }
        }

    }

    /**
     * Marks a specific message as read
     */
    @Override
    public void markMessageAsRead(Context context, long messageId) {

        if (messageId == 0)
            return;

        ContentValues values = new ContentValues(1);
        values.put(Messages.READ, Messages.MSG_READ);

        Uri messageUri;
        messageUri = Uri.withAppendedPath(FeedProvider.MESSAGES_CONTENT_URI,
                String.valueOf(messageId));
        String selection = Messages._ID + "=?" + messageId;
        String[] selectionArgs = new String[]{String.valueOf(messageId)};

        ContentResolver cr = context.getContentResolver();
        int result;
        try {
            result = cr.update(messageUri, values, selection, selectionArgs);
        } catch (Exception e) {

            Log.e(TAG, String.format(
                    "Error while trying to mark message # %s as read",
                    messageId));
            result = 0;
        }

        if (result > 0) {
            Log.d(TAG, String.format("Message # %s marked as read", messageId));
        } else {
            Log.d(TAG, String.format("Message # %s already marked as read",
                    messageId));
        }
    }


    @Override
    public void deleteMergeLinks(Platform mergePlatform, long feedId) {


        // DELETE MERGE LINK.
        HashMap<Platform, List<MemberContact>> memberContacts = getContacts(feedId);
        String[] contactIdentifiers = null;
        List<String> contacts = new ArrayList<String>();

        for (Platform platform : memberContacts.keySet()) {
            if (platform == mergePlatform) {
                int index = 0;
                contactIdentifiers = new String[memberContacts.get(platform)
                        .size()];
                for (MemberContact contact : memberContacts.get(platform)) {
                    contactIdentifiers[index++] = contact
                            .getNormalizedIdentifier();
                }
            } else {
                for (MemberContact contact : memberContacts.get(platform)) {
                    contacts.add(contact
                            .getNormalizedIdentifier());
                }
            }
        }

        if (contactIdentifiers != null && contactIdentifiers.length > 0) {

            Uri uri = Uri.withAppendedPath(FeedProvider.CONTACTS_UNMERGE_URI,
                    feedId + "");
            uri = Uri.withAppendedPath(uri, feedId + "");

            FeedProvider.setContactsForMerge(contacts);
            context.getContentResolver().update(uri, null, null,
                    contactIdentifiers);
        }
    }

    /**
     * Move feed's messages to the other feed.
     *
     * @param feedId       - from feed.
     * @param mergedFeedId - to feed.
     */
    private void mergeFeeds(String feedId, String mergedFeedId) {

        if (!StringUtils.isBlank(feedId)
                && !StringUtils.isBlank(mergedFeedId)) {

            ContentValues values = new ContentValues(1);
            values.put(Messages.Feed_ID, mergedFeedId);

            context.getContentResolver().update(
                    ContentUris.withAppendedId(FeedProvider.FEEDS_MERGE_URI,
                            Long.valueOf(feedId)), values, null, null
            );
        }
    }

    /**
     * Map query results to the feed object.
     *
     * @param cursor -query results.
     */
    private FeedModel buildFeedModel(Cursor cursor) {

        FeedModel feed = ModelConverter.convertToFeed(cursor);


        // Sometimes we don't need to load feed unread platforms.
        if (!feed.isRead()) {
            Map<Platform, Integer> count = getFeedUnReadPlatforms(feed
                    .getFeedId());
            feed.setUnreadMessagesCount(count);
            feed.setPlatforms(new ArrayList<Platform>(count.keySet()));

            if (count.size() == 0)
                feed.setRead(true);

        } else {
            feed.setUnreadMessagesCount(new HashMap<Platform, Integer>());
            feed.setPlatforms(new ArrayList<Platform>());
        }

        Contact contact = new Contact(cursor.getString(cursor
                .getColumnIndex(Feeds.MEMBERS_IDS)));
        contact.setIdentifier(cursor.getString(cursor
                .getColumnIndex(Feeds.FEED_IDENTIFIER)));

        String name = cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.NAME));

        if (name != null && !name.equals(""))
            contact.setName(name);

        contact.setContactId(cursor.getString(cursor
                .getColumnIndex(HeadboxContacts.CONTACT_ID)));
        contact.setContactType(Platform.fromId(cursor.getInt(cursor
                .getColumnIndex(HeadboxContacts.CONTACT_TYPE))));

        long notificationEnabledValue = cursor.getLong(cursor.getColumnIndex(HeadboxContacts.NOTIFICATION_ENABLED));
        boolean notificationEnabled = (notificationEnabledValue == HeadboxContacts.NOTIFICATION_DISABLED_VALUE ? false : true);
        contact.setNotificationEnabled(notificationEnabled);

        if (!StringUtils.isBlank(contact.getContactId())) {
            Platform contactType = contact.getContactType();
            if (contactType == Platform.CALL) {
                feed.setPhoneNumber(contact.getIdentifier());
                feed.setHasPhoneNumber(true);
            }

            Uri photoUri = null;
            if (contactType == Platform.FACEBOOK) {

                photoUri = Uri.parse(cursor.getString(cursor
                        .getColumnIndex(HeadboxContacts.PICTURE_URL)));
                contact.setPhotoUri(photoUri);
            } else {
                List<MemberContact> contacts = getContactIdentifiers(
                        contact.getContactId(), null);
                photoUri = handleContactPhoto(contacts);
                contact.setPhotoUri(photoUri);
            }
        } else {
            if (feed.getLastMessagePlatform() == Platform.CALL
                    || feed.getLastMessagePlatform() == Platform.SMS) {

                feed.setPhoneNumber(contact.getIdentifier());
                feed.setHasPhoneNumber(true);
            }
        }
        feed.setContact(contact);
        return feed;
    }

    /**
     * Select proper photo for the contact.
     */
    private Uri handleContactPhoto(List<MemberContact> contacts) {

        String facebookPhoto = null;
        String googlePhoto = null;
        String twitterPhoto = null;
        String instagramPhoto = null;
        String phonePhoto = null;
        String smsPhoto = null;

        if (contacts != null && contacts.size() > 0) {

            for (MemberContact memberContact : contacts) {
                if (memberContact.getHasPhoto()) {
                    if (memberContact.getType() == Platform.FACEBOOK) {
                        facebookPhoto = memberContact.getPhotoUrl();
                        break;
                    } else if (memberContact.getType() == Platform.HANGOUTS) {
                        googlePhoto = memberContact.getPhotoUrl();
                    } else if (memberContact.getType() == Platform.TWITTER) {
                        twitterPhoto = memberContact.getPhotoUrl();
                    } else if (memberContact.getType() == Platform.INSTAGRAM) {
                        instagramPhoto = memberContact.getPhotoUrl();
                    } else if (memberContact.getType() == Platform.SMS) {
                        smsPhoto = memberContact.getPhotoUrl();
                    } else {
                        phonePhoto = memberContact.getPhotoUrl();
                    }
                }
            }

            if (facebookPhoto != null) {
                photoUri = Uri.parse(facebookPhoto);
            } else if (twitterPhoto != null) {
                photoUri = Uri.parse(twitterPhoto);
            } else if (instagramPhoto != null) {
                photoUri = Uri.parse(instagramPhoto);
            } else if (googlePhoto != null) {
                photoUri = Uri.parse(googlePhoto);
            } else if (phonePhoto != null) {
                photoUri = Uri.parse(phonePhoto);
            } else if (smsPhoto != null) {
                photoUri = Uri.parse(smsPhoto);
            } else {
                photoUri = Uri.parse("");
            }

        } else {
            photoUri = Uri.parse("");
        }
        return photoUri;

    }

    public int buildContacts() {

        Log.d(TAG, "Building device contacts table.");
        this.deleteContacts();
        return this.loadContacts();

    }

    public void updateLocalContacts() {

        Log.d(TAG, "Updating local contacts...");

        int insertedContacts = context.getContentResolver().bulkInsert(
                FeedProvider.UPDATE_LOCAL_CONTACTS_URI, null);
        Log.i(TAG, "Insert " + insertedContacts + " device contacts.");

    }

    private int loadContacts() {

        int insertedContacts = context.getContentResolver().bulkInsert(
                FeedProvider.CONTACTS_URI, null);

        if (insertedContacts > 0) {
            Log.i(TAG, "Insert " + insertedContacts + " device contacts.");
        } else {
            Log.i(TAG, "No device contacts were found");
        }

        Uri welcomeFeed = context.getContentResolver().insert(FeedProvider.INSERT_WELCOME_FEED, null);

        return insertedContacts;
    }

    /**
     * Insert certain platform contacts.
     */
    public void buildContacts(List<Contact> contacts, final Platform platform) {

        ContentValues[] valuesArray = new ContentValues[contacts.size()];
        Log.d(TAG, platform.name() + " contacts = " + contacts.size());

        // Loop through each contact and convert to contentValues object.
        for (int i = 0; i < contacts.size(); i++) {

            ContentValues values = ModelConverter.convert(contacts.get(i));
            valuesArray[i] = values;
        }

        int insertedContacts = context.getContentResolver().bulkInsert(
                ContentUris.withAppendedId(FeedProvider.GENERAL_CONTACTS_URI,
                        platform.getId()), valuesArray
        );


        Log.i(TAG, platform.name() + ", Inserted Contacts = "
                + insertedContacts);
    }


    public List<Contact> getContacts(Context context, Platform platform) {


        List<Contact> contacts = new ArrayList<Contact>();
        Cursor cursor = null;
        try {

            cursor = context.getContentResolver().query(ContentUris.withAppendedId(FeedProvider.GENERAL_CONTACTS_URI,
                    platform.getId()), null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    contacts.add(ModelConverter.convertToHeadboxContact(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "exception on uploadContacts method = "
                            + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return contacts;
    }

    @Override
    public void updateGoogleXMPPContactsTable(List<Contact> contacts) {

        ContentValues[] values = new ContentValues[contacts.size()];
        final String photoUrl = "https://plus.google.com/s2/photos/profile/%s";

        for (int i = 0; i < contacts.size(); i++) {

            ContentValues value = new ContentValues();
            value.put(HeadboxContacts.PRIMARY_INDENTIFIER, contacts.get(i)
                    .getContactId());
            value.put(HeadboxContacts.PICTURE_URL,
                    String.format(photoUrl, contacts.get(i).getAlternativeId()));

            if (StringUtils.isBlank(contacts.get(i).getAlternativeId())) {
                value.put(HeadboxContacts.ALTER_INDENTIFIER, "");
            } else {
                value.put(HeadboxContacts.ALTER_INDENTIFIER, contacts.get(i)
                        .getAlternativeId());
            }
            values[i] = value;
        }
        context.getContentResolver().bulkInsert(
                FeedProvider.UPDATE_GOOGLE_CONTACTS_URI, values);

    }

    @Override
    public void updateGoogleCovers(List<Contact> friends) {

        ContentValues[] values = new ContentValues[friends.size()];

        int index = 0;
        for (Contact contact : friends) {

            ContentValues value = new ContentValues();
            if (!StringUtils.isBlank(contact.getLink())) {

                String googlePlusId = StringUtils.getStringAfter(
                        contact.getLink(), "/");
                value.put(HeadboxContacts.ALTER_INDENTIFIER, googlePlusId);

            } else {
                value.put(HeadboxContacts.ALTER_INDENTIFIER,
                        contact.getContactId());
            }
            if (contact.HasCover())
                value.put(HeadboxContacts.COVER_URL, contact.getCoverUri()
                        .toString());
            if (contact.HasPhoto())
                value.put(HeadboxContacts.PICTURE_URL, contact.getPhotoUri()
                        .toString());
            values[index++] = value;
        }

        context.getContentResolver().bulkInsert(FeedProvider.GOOGLE_COVERS_URI,
                values);

    }

    private void deleteContacts() {
        int contacts = context.getContentResolver().delete(
                FeedProvider.CONTACTS_URI, null, null);
        Log.d(TAG, "# of Deleted contacts = " + contacts);
    }

    @Override
    public void refresh() {

        try {

            Cursor cursor = context.getContentResolver().query(
                    FeedProvider.FEED_LIST_URI, null, null, null, null);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {

        }


    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void undoLastMerge() {

        PreferencesManager preferencesManager = new PreferencesManager(context);
        String mergedContactId = preferencesManager.getProperty(
                PreferencesManager.MERGED_CONTACT_ID, "");
        String feedContactId = preferencesManager.getProperty(
                PreferencesManager.FEED_CONTACT_ID, "");
        String searchSourceJson = preferencesManager.getProperty(
                PreferencesManager.SEARCH_SOURCE, "");
        BlinqApplication.searchSource = new Gson().fromJson(searchSourceJson,
                Platform.class);
        // delete merge link

        Uri uri = Uri.withAppendedPath(FeedProvider.MERGE_LINK_DELETE_URI,
                feedContactId + "");
        uri = Uri.withAppendedPath(uri, mergedContactId);
        context.getContentResolver().delete(uri, null, null);

        // restore merged feeds
        String feedHashMapJson = preferencesManager.getProperty(
                PreferencesManager.MERGED_FEEDS, "");
        HashMap<String, String> feedHashMap = new Gson().fromJson(
                feedHashMapJson, HashMap.class);
        restoreMergedFeeds(feedHashMap);

        String mergedContactFeed;
        String contactFeed;
        try {
            mergedContactFeed = String.valueOf(preferencesManager.getProperty(
                    PreferencesManager.MERGED_CONTACT_FEED, 0));
        }catch(ClassCastException c) {
            mergedContactFeed = String.valueOf(preferencesManager.getProperty(
                    PreferencesManager.MERGED_CONTACT_FEED));
        }
        try{
        contactFeed =  String.valueOf(preferencesManager.getProperty(
                PreferencesManager.CONTACT_FEED, 0));
        }catch(ClassCastException c) {
            contactFeed =  String.valueOf(preferencesManager.getProperty(
                    PreferencesManager.CONTACT_FEED));
        }
        mergeFeeds(contactFeed, mergedContactFeed);
    }

    /**
     * Restore the merged feeds.
     */
    private void restoreMergedFeeds(HashMap<String, String> feedHashMap) {
        for (String key : feedHashMap.keySet()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(FeedColumns.MEMBERS_IDS, feedHashMap.get(key));

            String selection = FeedColumns._ID + "=" + key;

            int result = context.getContentResolver().update(
                    FeedProvider.FEED_UNDO_MERGE_URI, contentValues, selection,
                    null);
        }

    }

    /**
     * Takes the list of top friend - meaning the people
     * the user has the most conversations with and
     * update their isNotification to true - means the
     * app would push notifications from those people
     *
     * @return number of replaced friends.
     */
    @Override
    public int updateTopFriendsNotifications() {

        ContentResolver cr = context.getContentResolver();
        int updatedFriends = cr.update(FeedProvider.UPDATE_TOP_FRIENDS_NOTIFICATIONS,
                null, null, null);
        return updatedFriends;
    }

}
