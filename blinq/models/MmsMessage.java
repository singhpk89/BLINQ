package com.blinq.models;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.Date;

/**
 * Extends HeadboxMessage model to hold Mms details.
 */
public class MmsMessage extends HeadboxMessage {

    /**
     * Image bitmap.
     */
    private Bitmap image;
    /**
     * Link/Uri to the image.
     */
    private Uri imageUri;
    /**
     * Indicates whether the MMS contains image.
     */
    private boolean containsMultimedia;

    private String subject;

    public MmsMessage(Contact contact, String messageId, String subject,
                      MessageType type, Platform platform, Date date) {

        super(contact, messageId, subject, type, platform, date);
        this.subject = subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setContainsMultimedia(boolean containsMultimedia) {
        this.containsMultimedia = containsMultimedia;
    }

    public boolean containsMultimedia() {
        return containsMultimedia;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (containsMultimedia ? 1231 : 1237);
        result = prime * result + ((image == null) ? 0 : image.hashCode());
        result = prime * result
                + ((imageUri == null) ? 0 : imageUri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof MmsMessage))
            return false;
        MmsMessage other = (MmsMessage) obj;
        if (containsMultimedia != other.containsMultimedia)
            return false;
        if (image == null) {
            if (other.image != null)
                return false;
        } else if (!image.equals(other.image))
            return false;
        if (imageUri == null) {
            if (other.imageUri != null)
                return false;
        } else if (!imageUri.equals(other.imageUri))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MmsMessage [image=" + image + ", imageUri=" + imageUri
                + ", containsMultimedia=" + containsMultimedia
                + ", getContact()=" + getContact() + ", getMessageId()="
                + getMessageId() + ", getBody()=" + getBody() + ", getType()="
                + getType() + ", getPlatform()=" + getPlatform()
                + ", getDate()=" + getDate() + ", getRead()=" + getRead()
                + ", getSourceId()=" + getSourceId() + ", getNormalizedDate()="
                + getNormalizedDate() + ", getFeedId()=" + getFeedId()
                + ", hashCode()=" + hashCode() + ", toString()="
                + super.toString() + ", getClass()=" + getClass() + "]";
    }

}
