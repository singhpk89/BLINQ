package com.blinq;

import android.util.Log;

import com.blinq.models.Platform;
import com.blinq.models.social.window.MeCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Roi on 1/12/15.
 */
public class MeCardHolder {

    private static final String TAG = MeCardHolder.class.getSimpleName();

    private static final String NAME = "name";
    private static final String ORGANIZATION = "organization";
    private static final String TITLE = "title";
    private static final String BIO = "bio";
    private static final String IMAGE_PATH = "image_path";
    private static final String LIKES_COUNT = "likes_count";

    private static MeCardHolder instance;

    private Map<String, String> information;
    private Map<Platform, String> socialProfiles;
    private List<MeCard.MutualFriend> mutualFriends;


    public static MeCardHolder getInstance() {
        if (instance == null) {
            instance = new MeCardHolder();
        }
        return instance;
    }

    private MeCardHolder() {
        this.socialProfiles = new HashMap<Platform, String>();
    }

    public List<MeCard.MutualFriend> getMutualFriends() {return mutualFriends;}

    public void setMutualFriends(List<MeCard.MutualFriend> mutualFriends) {this.mutualFriends = mutualFriends;}

    public String getName() {return information.get(NAME);}

    public void setName(String name) {information.put(NAME, name);}

    public String getImagePath() {return information.get(IMAGE_PATH);}

    public void setImagePath(String imagePath) {information.put(IMAGE_PATH, imagePath);}

    public void setTitle(String title) {information.put(TITLE, title);}

    public String getTitle() {return information.get(TITLE);}

    public void setOrganization(String organization) {information.put(ORGANIZATION, organization);}

    public String getOrganization() {return information.get(ORGANIZATION);}

    public void setBio(String bio) {information.put(BIO, bio);}

    public String getBio() {return information.get(BIO);}

    public void setLikesCount(String likesCount) {information.put(LIKES_COUNT, likesCount);}

    public String getLikesCount() {return information.get(LIKES_COUNT);}

    public void setSocialProfile(Platform platform, String url) {
        socialProfiles.put(platform, url);
    }

    public String getSocialProfile(Platform platform) {
        return socialProfiles.get(platform);
    }

    public Map<Platform, String> getSocialProfiles() {return socialProfiles;}

    public void clearData() {
        Log.d(TAG, "Clearing me card data");
        this.socialProfiles = new HashMap<Platform, String>();
        this.mutualFriends = new ArrayList<MeCard.MutualFriend>();
        this.information = new HashMap<String, String>();
    }
}
