package com.blinq.models.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.blinq.models.Platform;

/**
 * Represent a social profile
 * Includes: id, username, bio, photo
 *
 * A social profile can be Facebook, Twitter, GooglePlus, Instagram
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class SocialContactProfile {

    private String id;
    private String username;
    private String bio;
    private String photoUrl;
    private Platform platform;


    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    @JsonProperty("username")
    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    @JsonProperty("bio")
    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    @JsonProperty("photo")
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
}
