package com.blinq.module.message.utils;

import android.content.ContentValues;
import android.content.Context;
import android.telephony.SmsMessage;

import com.blinq.models.Contact;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.provider.HeadboxFeed.Feeds;
import com.blinq.provider.HeadboxFeed.Messages;
import com.blinq.utils.HeadboxPhoneUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Map message received from different platforms to headboxMessage.
 * <ul>
 * <li>Map FACEBOOK messages.</li>
 * <li>Map HANGOUTS messages.</li>
 * <li>Map SMS messages.</li>
 * </ul>
 *
 * @author Johan Hansson.
 */
public class MessageConverter {

    private static final String TAG = MessageConverter.class.getSimpleName();
    private Platform platform;
    private Context context;
    private String suffix;

    public MessageConverter(Context context, Platform platform) {
        this.context = context;
        this.platform = platform;
    }


    /**
     * Construct HeadboxMessage given parameters to use specially when reading
     * notification items for WHATSAPP,SKYPE,and EMAIL.
     */
    public HeadboxMessage convert(CharSequence contactName, CharSequence body,
                                  long time, int read, MessageType type) {

        // Convert message's sender to Contact model.
        Contact contact = new Contact();
        contact.setName(HeadboxPhoneUtils.getPhoneNumber(contactName.toString()));
        contact.setContactType(getPlatform());

        Date date = new Date(time);
        HeadboxMessage message = new HeadboxMessage(contact, body.toString(),
                type, getPlatform(), date);
        message.setRead(read);

        return message;
    }

    /**
     * Construct HeadboxMessage given a raw message (created from PDU).
     *
     * @param messages - List of android short Messages.
     * @return - HeadboxMessage.
     */
    public HeadboxMessage convert(SmsMessage[] messages) {

        HeadboxMessage headboxMessage = new HeadboxMessage();

        SmsMessage sms = messages[0];

        String fromAddress = sms.getDisplayOriginatingAddress();

		/*
         * Fetch data from raw SMS
		 */
        String body = "";

        try {
            if (messages.length == 1 || sms.isReplace()) {
                body = sms.getDisplayMessageBody();
            } else {
                StringBuilder bodyText = new StringBuilder();
                for (int i = 0; i < messages.length; i++) {
                    bodyText.append(messages[i].getMessageBody());
                }
                body = bodyText.toString();
            }
        } catch (Exception e) {
            Log.v(TAG, "Sms Message<init> exception: " + e.toString());
        }

        Date date = new Date();
        Contact contact = new Contact(fromAddress);
        contact.setContactType(getPlatform());
        headboxMessage.setContact(contact);

        headboxMessage = new HeadboxMessage(contact, body,
                MessageType.INCOMING, Platform.SMS, date);

        return headboxMessage;

    }

    private String parseSenderId(String from, Platform platform) {

        if (from == null || from.length() == 0)
            return null;

        switch (platform) {

            case HANGOUTS:
                // Packet user in the form (email/someInfo) so we need to remove
                // any data after "/".
                suffix = "";
                if (from.length() > 0 && from.indexOf("/") != -1) {
                    return from.substring(0, from.indexOf("/"));
                }
                break;
            case FACEBOOK:
                // Sender id is always surrounded by '-' and '@'.
                suffix = StringUtils.FACEBOOK_SUFFIX;
                return from.substring(from.indexOf("-") + 1, from.indexOf("@"));
            default:
                break;
        }

        return null;

    }

    /**
     * Convert Headbox message to sets of column_name/value pairs to add to the
     * database.
     *
     * @param message - HeadboxMessage
     */
    public static ContentValues convertToContentValues(HeadboxMessage message) {

        ContentValues values = new ContentValues();
        values.put(Messages.PLATFORM_ID, message.getPlatform().getId());
        values.put(Messages.DATE, message.getDate().getTime());
        values.put(Messages.TYPE, message.getType().getId());
        values.put(Messages.BODY, message.getBody());
        values.put(Messages.SOURCE_ID, message.getSourceId());

        if (MessageType.OUTGOING == message.getType()) {
            values.put(Messages.READ, Messages.MSG_READ);
        } else if (Platform.CALL == message.getPlatform()
                && MessageType.INCOMING == message.getType()) {
            values.put(Messages.READ, Messages.MSG_READ);
        } else if (Platform.CALL == message.getPlatform()) {
            values.put(Messages.READ, message.getRead());
        } else {
            values.put(Messages.READ, message.getRead());
        }

        if (message.getPlatform() == Platform.CALL) {

            values.put(Messages.CALL_DURATION, message.getDuration());

            if (message.getType() == MessageType.INCOMING) {
                values.put(Messages.READ, Messages.MSG_READ);
            }
        }

        values.put(Feeds.FEED_IDENTIFIER, message.getContact().getIdentifier());
        values.put(Messages.CONTACT, message.getContact()
                .getNormalizedIdentifier());
        values.put(Messages.CONTACT_COMPLETE, message.getContact()
                .getIdentifier());

        return values;
    }

    public static ContentValues[] convertToContentValues(List<HeadboxMessage> messages) {

        List<ContentValues> content = new ArrayList<ContentValues>();
        for (HeadboxMessage message : messages) {
            content.add(convertToContentValues(message));
        }
        return content.toArray(new ContentValues[content
                .size()]);
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

}
