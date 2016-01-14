package com.blinq.models;

/**
 * Temporary Model, will be merged with the contact model.
 */
public class MemberContact {


    private String id;

    private String contactName;

    /**
     * Contact type : Local,Facebook,google etc..
     */
    private Platform type;
    /**
     * Contact identifier: phoneNumber,Facebook Id, Google Id etc.
     */
    private String identifier;
    /**
     * Normalized identifier of original identifier.
     */
    private String normalizedIdentifier;
    /**
     * Contact Photo/Picture URL.
     */
    private String photoUrl;
    /**
     * Contact Cover photo URL .
     */
    private String coverUrl;
    /**
     * Check whether the contact has photo or not.
     */
    private boolean hasPhoto;
    /**
     * Check whether the contact has cover or not.
     */
    private boolean hasCover;

    public MemberContact(Platform type, String identifier) {
        super();
        this.type = type;
        this.identifier = identifier;
    }

    public MemberContact(Platform type, String identifier, String normalizedIdentifier) {
        super();
        this.type = type;
        this.identifier = identifier;
        this.normalizedIdentifier = normalizedIdentifier;
    }

    public MemberContact(Platform type, String identifier, String photoUrl, String coverUrl) {
        super();
        this.type = type;
        this.identifier = identifier;
        this.photoUrl = photoUrl;
        this.coverUrl = coverUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactName() {
        return contactName;
    }

    public Platform getType() {
        return type;
    }

    public void setType(Platform type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getNormalizedIdentifier() {
        return normalizedIdentifier;
    }

    public void setNormalizedIdentifier(String normalizedIdentifier) {
        this.normalizedIdentifier = normalizedIdentifier;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Boolean getHasPhoto() {
        return hasPhoto;
    }

    public void setHasPhoto(Boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }

    public boolean hasCover() {
        return hasCover;
    }

    public void setHasCover(boolean hasCover) {
        this.hasCover = hasCover;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((coverUrl == null) ? 0 : coverUrl.hashCode());
        result = prime * result + (hasCover ? 1231 : 1237);
        result = prime * result + (hasPhoto ? 1231 : 1237);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result
                + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime
                * result
                + ((normalizedIdentifier == null) ? 0 : normalizedIdentifier
                .hashCode());
        result = prime * result
                + ((photoUrl == null) ? 0 : photoUrl.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof MemberContact))
            return false;
        MemberContact other = (MemberContact) obj;
        if (coverUrl == null) {
            if (other.coverUrl != null)
                return false;
        } else if (!coverUrl.equals(other.coverUrl))
            return false;
        if (hasCover != other.hasCover)
            return false;
        if (hasPhoto != other.hasPhoto)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (normalizedIdentifier == null) {
            if (other.normalizedIdentifier != null)
                return false;
        } else if (!normalizedIdentifier.equals(other.normalizedIdentifier))
            return false;
        if (photoUrl == null) {
            if (other.photoUrl != null)
                return false;
        } else if (!photoUrl.equals(other.photoUrl))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MemberContact [id=" + id + ", type=" + type + ", identifier="
                + identifier + ", normalizedIdentifier=" + normalizedIdentifier
                + ", photoUrl=" + photoUrl + ", coverUrl=" + coverUrl
                + ", hasPhoto=" + hasPhoto + ", hasCover=" + hasCover + "]";
    }

}
