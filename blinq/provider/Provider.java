package com.blinq.provider;

import android.content.Context;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides functionalities built over Headbox providers.
 */
public interface Provider {

    public Uri insertMessage(HeadboxMessage message);

    public List<FeedModel> getFeedsByPlatforms(List<Platform> platforms);

    public List<FeedModel> getFeeds(long offset, long limit,
                                    List<Platform> platforms);

    int insertLogsHistory(int count);

    public List<Platform> getFeedPlatforms(long feedId);

    public void markFeedAsRead(Context context, long feedId);

    public void markMessageAsRead(Context context, long messageId);

    public HeadboxMessage getMessage(long id);

    public HeadboxMessage getMessage(Platform platform, String sourceId);

    public List<HeadboxMessage> getFeedMessages(long feedId, long offset,
                                                long limit);

    public FeedModel getFeedById(long feedId, String read);

    public List<FeedModel> getUnreadFeeds(List<Platform> platforms);

    public List<HeadboxMessage> getMessagesAfter(long feedId, long messageId);

    public int setFeedsAsUnmodified(List<FeedModel> feeds);

    public List<FeedModel> getModifiedFeeds();

    public FeedModel getFeed(long feedId, Platform platform);

    public FeedModel getFeed(long feedId);

    public HeadboxMessage getDraftMessage(long feedId);

    public int deleteDraftMessage(long feedId);

    public HashMap<Platform, List<MemberContact>> getContacts(long feedId);

    public Map<Platform, Integer> getFeedUnReadPlatforms(long feedId);

    List<MemberContact> getFeedMergedContacts(long feedId,
                                              Platform platform);

    List<MemberContact> getContactIdentifiers(String contactId);

    public List<SearchResult> convertToSearchResult(Cursor cursor);

    Uri insertNotification(NotificationData notificationData);

    boolean changeMessageType(Uri uri, Platform platform, MessageType type);

    public String getLastPhoneNumber(long feedId);

    int deleteMessage(long feedId, long messageId);

    int deleteAllMessages(Platform platform);

    boolean deleteFeed(long feedId);

    int deleteMessage(String messageSourceId, Platform platform, long feedId);

    void undoLastMerge();

    void deleteMergeLinks(Platform mergePlatform, long feedId);

    public void buildContacts(List<Contact> friends, Platform platform);

    Uri insertContact(Contact contact);

    boolean contactExists(String contactIdentifier);

    public void updateGoogleXMPPContactsTable(List<Contact> contacts);

    void updateGoogleCovers(List<Contact> friends);

    int UpdateContact(Contact contact);

    void refresh();

    List<FeedModel> getFeedsFor(Platform platform, long offset, long limit);

    List<FeedModel> getModifiedFeedsFor(Platform platform);

    FeedModel getFeedFor(Platform platform, long feedId);

    List<MemberContact> getContactIdentifiers(Platform platform,
                                              String contactId);

    HashMap<Platform, MemberContact> getAllContacts(long feedId);

    Map<Integer, List<Platform>> getFeedUnReadPlatforms();

    int createEmptyFeed(String contactId, String contactIdentifier);

    /**
     * Takes the list of top friend - meaning the people
     * the user has the most conversations with and
     * update their isNotification to true - means the
     * app would push notifications from those people
     */
    int updateTopFriendsNotifications();
}
