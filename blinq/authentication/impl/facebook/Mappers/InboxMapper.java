package com.blinq.authentication.impl.facebook.Mappers;

import android.os.Bundle;

import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;
import com.blinq.models.Contact;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.authentication.impl.facebook.FacebookUtils.User;
import com.blinq.provider.HeadboxFeed;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible to convert the response of the inbox graph path
 * initialized by {@link com.blinq.authentication.impl.facebook.Actions.InboxAction} to
 * a headbox {@link com.blinq.models.HeadboxMessage} model.
 */
public class InboxMapper {

    private static final String TAG = InboxMapper.class.getSimpleName();
    private Contact mContact;
    private List<HeadboxMessage> mMessages;
    private Map<Contact, List<HeadboxMessage>> map;
    private final GraphObject mGraphObject;
    private final String me;

    private InboxMapper(String me, GraphObject graphObject) {

        this.mGraphObject = graphObject;
        this.me = me;

        Date date = new FacebookUtils().getDateFromString(FacebookUtils.getPropertyString(mGraphObject, Properties.UPDATED_TIME));
        int unreadMessages = FacebookUtils.getPropertyInteger(mGraphObject, Properties.UNREAD);
        int read = unreadMessages > 0 ? HeadboxFeed.Messages.MSG_UNREAD : HeadboxFeed.Messages.MSG_READ;

        try {

            JSONObject to = (JSONObject) mGraphObject.asMap().get(Properties.TO);
            GraphObjectList<GraphObject> graphObjects = FacebookUtils.getGraphObjectList(to, Properties.DATA);

            if (graphObjects == null || graphObjects.size() != Properties.CONVERSATION_MEMBER)
                return;

            Contact sender = ProfileMapper.create(graphObjects.get(Properties.CONVERSATION_SENDER));
            Contact receiver = ProfileMapper.create(graphObjects.get(Properties.CONVERSATION_RECEIVER));

            if (sender.getContactId().equals(me)) {
                mContact = receiver;
            } else {
                mContact = sender;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {

            JSONObject conversation = (JSONObject) mGraphObject.asMap().get(Properties.COMMENTS);
            GraphObjectList<GraphObject> graphMessages = FacebookUtils.getGraphObjectList(conversation, Properties.DATA);

            if (graphMessages == null)
                return;

            this.mMessages = new ArrayList<HeadboxMessage>();

            int index = 0;
            for (GraphObject message : graphMessages) {

                HeadboxMessage headboxMessage = convertToMessage(message);
                if (headboxMessage == null)
                    continue;

                if (headboxMessage.getType() == MessageType.OUTGOING)
                    headboxMessage.setRead(HeadboxFeed.Feeds.FEED_READ);
                else {
                    headboxMessage.setRead(isMessageRead(index, graphMessages.size(), unreadMessages));
                }
                index++;

                headboxMessage.setPlatform(Platform.FACEBOOK);
                mMessages.add(headboxMessage);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mMessages == null)
            return;

        this.map = new HashMap<Contact, List<HeadboxMessage>>();
        this.map.put(mContact, mMessages);

    }

    private HeadboxMessage convertToMessage(GraphObject mGraphObject) {

        HeadboxMessage headboxMessage = new HeadboxMessage();
        // message id
        headboxMessage.setMessageId(FacebookUtils.getPropertyString(mGraphObject, Properties.ID));
        headboxMessage.setSourceId(FacebookUtils.getPropertyString(mGraphObject, Properties.ID));

        // created time
        Date date = new FacebookUtils().getDateFromString(FacebookUtils.getPropertyString(mGraphObject, Properties.CREATED_TIME));
        headboxMessage.setDate(date);

        // message body
        headboxMessage.setBody(FacebookUtils.getPropertyString(mGraphObject, Properties.MESSAGE));

        // from
        User from = FacebookUtils.createUser(mGraphObject, Properties.FROM);

        if (from == null || StringUtils.isBlank(from.getId()) || date == null
                || StringUtils.isEmpty(headboxMessage.getBody())
                || StringUtils.isBlank(headboxMessage.getMessageId()))
            return null;

        if (from.getId().equals(me)) {
            headboxMessage.setType(MessageType.OUTGOING);
        } else {
            headboxMessage.setType(MessageType.INCOMING);
        }

        headboxMessage.setPlatform(Platform.FACEBOOK);
        String id = from.getId().concat(com.blinq.utils.StringUtils.FACEBOOK_SUFFIX);
        Contact contact = new Contact(id, from.getName(), id, id);
        headboxMessage.setContact(contact);

        return headboxMessage;
    }

    /**
     * Checks whether the message within the last number of read messages.
     *
     * @param index               of the messages.
     * @param length              - number of messages.
     * @param unreadMessagesCount number of last unread messages.
     * @return
     */
    public int isMessageRead(int index, int length, int unreadMessagesCount) {
        if (length - unreadMessagesCount <= index)
            return HeadboxFeed.Messages.MSG_UNREAD;
        return HeadboxFeed.Messages.MSG_READ;
    }


    public static Map<Contact, List<HeadboxMessage>> create(String me, GraphObject graphObject) {
        return new InboxMapper(me, graphObject).getMessages();
    }

    /**
     * Profile properties.
     */
    public static class Properties {

        private final Bundle mBundle;

        private Properties(Builder builder) {

            mBundle = new Bundle();
            Iterator<String> iterator = builder.properties.iterator();
            String fields = FacebookUtils.join(iterator, ",");
            mBundle.putString(Properties.FIELDS, fields);
        }

        public Bundle getBundle() {
            return mBundle;
        }


        /**
         * Graph API Path.
         */
        public static final String INBOX_PATH = "me/inbox";

        public static String FIELDS = "fields";

        /**
         * Message receiver.
         */
        public static final String TO = "to";

        /**
         * Message sender.
         */
        public static final String FROM = "from";

        public static final int CONVERSATION_MEMBER = 2;

        /**
         * Sender/receiver id indices.
         */
        public static final int CONVERSATION_SENDER = 0;
        public static final int CONVERSATION_RECEIVER = 1;

        /**
         * Data content.
         */
        public static final String DATA = "data";
        /**
         * Message sender Id.
         */
        public static final String ID = "id";

        public static final String UPDATED_TIME = "updated_time";

        /**
         * Comments/Messages field of the session.
         */
        public static final String COMMENTS = "comments";
        /**
         * Message field of the session.
         */
        public static final String MESSAGE = "message";
        /**
         * Message creation time.
         */
        public static final String CREATED_TIME = "created_time";
        /**
         * Message read status
         */
        public static final String UNREAD = "unread";

        /**
         * Conversations limit's field.
         */
        public static final String LIMIT = "limit";
        /**
         * Max # of returned conversations each request.
         */
        public static final String LIMIT_VALUE = "10";

        /**
         * Properties and attributes builder.
         */
        public static class Builder {

            Set<String> properties;

            public Builder() {
                properties = new HashSet<String>();
            }

            /**
             * Add property you need
             *
             * @param property For example: {@link Properties#COMMENTS}
             * @return {@link Builder}
             */
            public Builder add(String property) {
                properties.add(property);
                return this;
            }

            /**
             * Add property and attribute you need
             *
             * @param property For example: {@link Properties#TO}
             * @param map      For example: To[user] can have id,first_name, and last_name<br>
             * @return {@link Builder}
             */
            public Builder add(String property, Map<String, String> map) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(property);
                stringBuilder.append('.');
                stringBuilder.append(FacebookUtils.join(map, '.', '(', ')'));
                properties.add(stringBuilder.toString());
                return this;
            }

            public Builder add(String property, String fields, List<String> map) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(property);
                stringBuilder.append('.');
                stringBuilder.append(fields);
                stringBuilder.append('(');
                stringBuilder.append(FacebookUtils.join(map.iterator(), ","));
                stringBuilder.append(')');
                properties.add(stringBuilder.toString());
                return this;
            }

            public Properties build() {
                return new Properties(this);
            }

        }
    }

    /**
     * Get a converted message.
     */
    public Map<Contact, List<HeadboxMessage>> getMessages() {
        return map;
    }

}
