package com.blinq.analytics;

import android.content.Context;

import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;

import java.util.ArrayList;
import java.util.HashMap;

public class HeadboxAnalyst {

    public static String TOTAL_INCOMING_MESSAGES_COUNT = "incoming_messages_count";
    public static String TOTAL_OUTGOING_MESSAGES_COUNT = "outgoing_messages_count";
    public String PLATFORM_INCOMING_COUNT = "%s_messages_incoming_count";
    public String PLATFORM_OUTGOING_COUNT = "%s_messages_outgoing_count";
    public static int MESSAGES_EVENT_COUNT_THRESHOLD = 20;
    private static Object incomingMessagesLock = new Object();
    private static Object outgoingMessagesLock = new Object();
    private final ArrayList<Platform> messagingPlatforms = new ArrayList<Platform>();
    private final PreferencesManager preferencesManager;
    private final Analytics analyticsManager;
    private Context context;

    public HeadboxAnalyst(Context context) {

        this.context = context;
        analyticsManager = BlinqApplication.analyticsManager;
        preferencesManager = BlinqApplication.preferenceManager;
        messagingPlatforms.add(Platform.FACEBOOK);
        messagingPlatforms.add(Platform.HANGOUTS);
        messagingPlatforms.add(Platform.SMS);
        messagingPlatforms.add(Platform.CALL);
    }

    /**
     * Send incoming message event
     */
    public void sendIncomingMessageEvent(Platform messagePlatform, boolean insideHeadbox) {

        synchronized (incomingMessagesLock) {

            sendMessageEvent(messagePlatform, MessageType.INCOMING, insideHeadbox);

        }

    }

    /**
     * Send outgoing message event
     */
    public void sendOutgoingMessageEvent(Platform messagePlatform, boolean insideHeadbox) {
        synchronized (outgoingMessagesLock) {

            sendMessageEvent(messagePlatform, MessageType.OUTGOING, insideHeadbox);

        }
    }

    /**
     * Send outgoing/incoming messages event based on message type.
     */
    private void sendMessageEvent(Platform messagePlatform,
                                  MessageType messageType, boolean insideHeadbox)

    {
        String totalMessagesKey, messagePlatformCountKey, eventName;

        switch (messageType) {
            case OUTGOING:
                totalMessagesKey = TOTAL_OUTGOING_MESSAGES_COUNT;
                messagePlatformCountKey = PLATFORM_OUTGOING_COUNT;
                eventName = AnalyticsConstants.OUTGOING_EVENT;
                if (insideHeadbox)
                    analyticsManager.sendEvent(
                            AnalyticsConstants.SEND_OUTGOING_EVENT,
                            AnalyticsConstants.TYPE_PROPERTY,
                            messagePlatform.name(), true, AnalyticsConstants.COMMUNICATION_CATEGORY);
                analyticsManager
                        .sendEvent(AnalyticsConstants.OUTGOING_EVENT, AnalyticsConstants.TYPE_PROPERTY,
                                messagePlatform.name(), false, AnalyticsConstants.COMMUNICATION_CATEGORY);

                break;
            case INCOMING:
                totalMessagesKey = TOTAL_INCOMING_MESSAGES_COUNT;
                messagePlatformCountKey = PLATFORM_INCOMING_COUNT;
                eventName = AnalyticsConstants.INCOMING_EVENT;
                analyticsManager
                        .sendEvent(AnalyticsConstants.INCOMING_EVENT, AnalyticsConstants.TYPE_PROPERTY,
                                messagePlatform.name(), false, AnalyticsConstants.COMMUNICATION_CATEGORY);
                break;

            default:
                return;
        }

        int totalMessages = preferencesManager.getProperty(totalMessagesKey, 0);

        totalMessages++;
        // send the event every 20 message
        if (totalMessages % MESSAGES_EVENT_COUNT_THRESHOLD == 0) {
            // send incoming/outgoing group messages event
            HashMap<String, Object> properties = new HashMap<String, Object>();

            // find messages count for each platform
            for (Platform platform : messagingPlatforms) {

                int platformMessagesCount = preferencesManager
                        .getProperty(
                                String.format(messagePlatformCountKey,
                                        platform.name()), 0
                        );
                String platformMessagesProperty;

                if (platform == Platform.CALL) {
                    platformMessagesProperty = platform.name();
                } else {
                    platformMessagesProperty = String.format(
                            AnalyticsConstants.PLATFORM_MESSAGES_PROPERTY,
                            platform.name());
                }

                if (platform == messagePlatform) {
                    platformMessagesCount++;
                }

                properties.put(platformMessagesProperty, platformMessagesCount);

            }
            // add total messages property
            properties.put(AnalyticsConstants.TOTAL_MESSAGES_PROPERTY,
                    totalMessages);

            analyticsManager.sendEvent(eventName,
                    properties, true, AnalyticsConstants.COMMUNICATION_CATEGORY);
        }

        // increase the counts
        preferencesManager.setProperty(totalMessagesKey, totalMessages);
        int platformMessagesCount = preferencesManager.getProperty(
                String.format(messagePlatformCountKey, messagePlatform.name()),
                0);
        preferencesManager.setProperty(
                String.format(messagePlatformCountKey, messagePlatform.name()),
                platformMessagesCount + 1);

    }

}
