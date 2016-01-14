package com.blinq.models;

import android.net.Uri;

import com.blinq.models.server.SocialContactProfile;
import com.blinq.utils.HeadboxPhoneUtils;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

import java.util.List;

/**
 * Holds Headbox contacts information.
 *
 * @author Johan Hansson.
 */
public class Contact {

    private final String TAG = Contact.class.getSimpleName();

    private String id;
    /**
     * Some platforms provide alternative id such as google XMPP and google plus
     * contact.
     */
    private String alternativeId;

    /**
     * Could be phoneNumber,Facebook Id , Google Id,etc..
     */
    private String identifier;

    /**
     * Normalized identifier of the complete identifier.
     */
    private String normalizedIdentifier;

    /**
     * Contact first Name
     */
    private String firstName;
    /**
     * Contact last Name
     */
    private String lastName;

    /**
     * Contact Display Name
     */
    private String name;

    /**
     * Contact username
     */
    private String userName;

    /**
     * The contact's email address
     */
    private String email;

    /**
     * Contact photo URL/URI.
     */
    private Uri photoUri;

    /**
     * Contact cover URL/URI
     */
    private Uri coverUri;

    /**
     * Contact timeZone offset from UTC.
     */
    private float timeZone;

    private boolean hasCover;
    private boolean hasPhoto;

    private Platform contactType;

    /**
     * The URL of the profile for the contact
     */
    private String link;
    /**
     * Is notification enabled for contact
     */
    private boolean isNotificationEnabled;
    /**
     * The title of the contact in the organization
     */
    private String title;
    /**
     * The organization of the contact
     */
    private String organizationName;
    /**
     * Bio about that person from one of the social networks
     */
    private String bio;
    /**
     * The contact social profiles
     */
    private List<SocialContactProfile> socialProfiles;

    public Contact() {
    }

    /**
     * @param identifier
     */
    public Contact(String identifier) {
        super();
        this.identifier = identifier;
        setNormalizedIdentifier(HeadboxPhoneUtils.getPhoneNumber(identifier));
    }

    public Contact(String contactId, String name, String identifier,
                   String normalizedIdentifier) {

        this.id = contactId;
        this.name = name;
        this.identifier = identifier;
        this.normalizedIdentifier = normalizedIdentifier;
    }

    public String getEmail() {
        return email;
    }

    public String getContactId() {
        return id;
    }

    public void setContactId(String id) {
        this.id = id;
    }

    public String getAlternativeId() {
        return alternativeId;
    }

    public void setAlternativeId(String alternativeId) {
        this.alternativeId = alternativeId;
    }

    public String getName() {
        return name != null ? name : normalizedIdentifier;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedIdentifier() {
        return normalizedIdentifier;
    }

    public void setNormalizedIdentifier(String normalizedIdentifier) {
        this.normalizedIdentifier = normalizedIdentifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }

    public void setCoverUri(Uri coverUri) {
        this.coverUri = coverUri;
    }

    public Uri getCoverUri() {
        return coverUri;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public String getFirstName() {
        String name = toString();
        if (StringUtils.isBlank(firstName)
                && !StringUtils.isBlank(name)) {
            firstName = name.split(StringUtils.SPACE)[0];
        }
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Platform getContactType() {
        return contactType;
    }

    public void setContactType(Platform contactType) {
        this.contactType = contactType;
    }

    public boolean HasCover() {
        return hasCover;
    }

    public void setHasCover(boolean hasCover) {
        this.hasCover = hasCover;
    }

    public boolean HasPhoto() {
        return hasPhoto;
    }

    public void setHasPhoto(boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }

    public float getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(float timeZone) {
        this.timeZone = timeZone;
    }

    public void setNotificationEnabled(boolean isNotificationEnabled) {
        this.isNotificationEnabled = isNotificationEnabled; }

    public boolean isNotificationEnabled() { return isNotificationEnabled; }

    /**
     * Return dummy avatar for the contact, used when loading the contact avatar failed.
     *
     * @return dummy avatar drawable id.
     */
    public int getDummyAvatarImage() {

        return UIUtils.getDummyAvatar(getDummyAvatarSeed());
    }

    /**
     * Calculate the index of dummy avatar image for given feed.
     *
     * @return seed to calculate dummy avatars index.
     */
    private int getDummyAvatarSeed() {

        int seed = 0;

        if (!StringUtils.isBlank(getContactId())) {

            seed = Integer.parseInt(getContactId());

        } else {

            try {
                seed = Integer.parseInt(getNormalizedIdentifier());
            } catch (NumberFormatException numberFormatException) {
                // No need to implement.
            }
        }

        return Math.abs(seed);
    }

    public boolean isContactable() {

        return getIdentifier() != null
                && !getIdentifier().equals(StringUtils.PRIVATE_NUMBER)
                && !getIdentifier().equals(StringUtils.UNKNOWN_NUMBER);
    }

    /**
     * Check if the contact is known (Local contact, Facebook contact ...).
     *
     * @return true if the contact is known, false otherwise.
     */
    public boolean isKnownContact() {

        if (StringUtils.isBlank(getContactId())) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {

        // Handle the case of an empty name.
        if (identifier == null) {
            return StringUtils.UNKNOWN_CONTACT;
        } else if (identifier.equals(StringUtils.UNKNOWN_NUMBER)) {
            return StringUtils.UNKNOWN_CONTACT;
        } else if (identifier.equals(StringUtils.PRIVATE_NUMBER)) {
            return StringUtils.PRIVATE;
        }
        return (name != null && name.length() > 0) ? name
                : (identifier != null ? identifier
                : StringUtils.UNKNOWN_CONTACT);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result
                + ((contactType == null) ? 0 : contactType.hashCode());
        result = prime * result
                + ((coverUri == null) ? 0 : coverUri.hashCode());
        result = prime * result
                + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + (hasCover ? 1231 : 1237);
        result = prime * result + (hasPhoto ? 1231 : 1237);
        result = prime * result
                + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result
                + ((lastName == null) ? 0 : lastName.hashCode());
        result = prime * result + ((link == null) ? 0 : link.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime
                * result
                + ((normalizedIdentifier == null) ? 0 : normalizedIdentifier
                .hashCode());
        result = prime * result
                + ((photoUri == null) ? 0 : photoUri.hashCode());
        result = prime * result
                + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Contact))
            return false;
        Contact other = (Contact) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (contactType != other.contactType)
            return false;
        if (coverUri == null) {
            if (other.coverUri != null)
                return false;
        } else if (!coverUri.equals(other.coverUri))
            return false;
        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else if (!firstName.equals(other.firstName))
            return false;
        if (hasCover != other.hasCover)
            return false;
        if (hasPhoto != other.hasPhoto)
            return false;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else if (!lastName.equals(other.lastName))
            return false;
        if (link == null) {
            if (other.link != null)
                return false;
        } else if (!link.equals(other.link))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (normalizedIdentifier == null) {
            if (other.normalizedIdentifier != null)
                return false;
        } else if (!normalizedIdentifier.equals(other.normalizedIdentifier))
            return false;
        if (photoUri == null) {
            if (other.photoUri != null)
                return false;
        } else if (!photoUri.equals(other.photoUri))
            return false;
        if (userName == null) {
            if (other.userName != null)
                return false;
        } else if (!userName.equals(other.userName))
            return false;
        return true;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getBio() {
        return bio;
    }

    public void setSocialProfiles(List<SocialContactProfile> socialProfiles) {
        this.socialProfiles = socialProfiles;
    }

    public List<SocialContactProfile> getSocialProfiles() {
        return socialProfiles;
    }
}