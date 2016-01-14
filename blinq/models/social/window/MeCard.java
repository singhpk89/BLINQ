package com.blinq.models.social.window;

import java.util.Date;
import java.util.List;

/**
 * Model for the me card. The card includes:
 * User Picture & Title
 * Full name
 * Last location
 * Social Handles
 * Birthday
 * Bio
 */
public class MeCard implements SocialWindowItem {

    private String pictureUrl;
    private String name;
    private String title;
    private String lastLocation;

    private String likesCount;

    private List<SocialProfile> socialProfiles;
    private List<MutualFriend> mutualFriends;

    private Date birthday;
    private String bio;

    @Override
    public SocialItemType getItemType() {
        return SocialItemType.ME_CARD;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(String lastLocation) {
        this.lastLocation = lastLocation;
    }

    public List<SocialProfile> getSocialProfiles() {
        return socialProfiles;
    }

    public void setSocialProfiles(List<SocialProfile> socialProfiles) {
        this.socialProfiles = socialProfiles;
    }

    public List<MutualFriend> getMutualFriends() {return mutualFriends;}

    public void setMutualFriends(List<MutualFriend> mutualFriends) {
        this.mutualFriends = mutualFriends;
    }

    public String getLikesCount() { return likesCount; }

    public void setLikesCount(String count) {
        this.likesCount = count;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public static class SocialProfile {
        private String url;
        private int photoResource;

        public SocialProfile(String url, int photoResource) {
            this.url = url;
            this.photoResource = photoResource;
        }

        public String getUrl() {
            return url;
        }

        public int getPhoto() {
            return photoResource;
        }
    }

    public static class MutualFriend {
        private final String id;
        private final String photoUrl;

        public MutualFriend(String id, String photoUrl) {
            this.id = id;
            this.photoUrl = photoUrl;
        }

        public String getId() {return id;}
        public String getPhotoUrl() {return photoUrl;}
    }

    @Override
    public String toString() {
        return "MeCard{" +
                "pictureUrl='" + pictureUrl + '\'' +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", lastLocation='" + lastLocation + '\'' +
                ", socialProfiles=" + socialProfiles +
                ", birthday=" + birthday +
                ", bio='" + bio + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MeCard meCard = (MeCard) o;

        if (bio != null ? !bio.equals(meCard.bio) : meCard.bio != null) return false;
        if (birthday != null ? !birthday.equals(meCard.birthday) : meCard.birthday != null)
            return false;
        if (lastLocation != null ? !lastLocation.equals(meCard.lastLocation) : meCard.lastLocation != null)
            return false;
        if (name != null ? !name.equals(meCard.name) : meCard.name != null) return false;
        if (pictureUrl != null ? !pictureUrl.equals(meCard.pictureUrl) : meCard.pictureUrl != null)
            return false;
        if (socialProfiles != null ? !socialProfiles.equals(meCard.socialProfiles) : meCard.socialProfiles != null)
            return false;
        if (title != null ? !title.equals(meCard.title) : meCard.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pictureUrl != null ? pictureUrl.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (lastLocation != null ? lastLocation.hashCode() : 0);
        result = 31 * result + (socialProfiles != null ? socialProfiles.hashCode() : 0);
        result = 31 * result + (birthday != null ? birthday.hashCode() : 0);
        result = 31 * result + (bio != null ? bio.hashCode() : 0);
        return result;
    }
}
