package com.blinq.models;

/**
 * Represent message type in our system maps a code from android default SMS
 * message types to our system
 *
 * @author Gal Bracha
 */
public enum MessageType {

    /**
     * INCOMING - for received message.
     * OUTGOING - for sent message.
     * MISSED - for missed calls.
     * DRAFT - for draft message.
     * PENDING - for sending message.
     * FAILED - for failed outgoing message.
     * QUEUED - for message to send later
     */
    NOTHING(0), INCOMING(1), OUTGOING(2), MISSED(3), DRAFT(4), FAILED(5), QUEUED(6), PENDING(7);

    private int id;

    private MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static MessageType fromId(int id) {
        for (MessageType e : MessageType.values()) {
            if (e.getId() == id)
                return e;
        }
        return MessageType.NOTHING;
    }
}