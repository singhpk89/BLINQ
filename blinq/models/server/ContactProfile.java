package com.blinq.models.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.blinq.models.Platform;

import java.util.ArrayList;
import java.util.List;


/**
 * Maps the json data returns from the server
 * for the user profile.
 * Includes all the user info including email, name,
 * different social handles and bios
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactProfile {

    private String email;
    private String id;
    private String user;

    private SocialContactProfile facebookSocialProfile;
    private SocialContactProfile twitterSocialProfile;
    private SocialContactProfile instagramSocialProfile;
    private SocialContactProfile googlePlusSocialProfile;
    private SocialContactProfile linkedinSocialProfile;
    private SocialContactProfile picasaSocialProfile;
    private SocialContactProfile gravatarSocialProfile;
    private SocialContactProfile githubSocialProfile;
    private SocialContactProfile angellistSocialProfile;
    private SocialContactProfile pinterestSocialProfile;
    private SocialContactProfile kloutSocialProfile;
    private SocialContactProfile aboutmeSocialProfile;
    private SocialContactProfile foursquareSocialProfile;
    private SocialContactProfile vimeoSocialProfile;

    private ContactInfo contactInfo;
    private List<Organization> organizations;

    /**
     * TODO: Better way to do it is to get an array from the server.
     */
    public List<SocialContactProfile> getSocialProfiles() {
        List<SocialContactProfile> result = new ArrayList<SocialContactProfile>();
        if(facebookSocialProfile != null) result.add(facebookSocialProfile);
        if(twitterSocialProfile != null) result.add(twitterSocialProfile);
        if(instagramSocialProfile != null) result.add(instagramSocialProfile);
        if(googlePlusSocialProfile != null) result.add(googlePlusSocialProfile);
        if(linkedinSocialProfile != null) result.add(linkedinSocialProfile);
        if(picasaSocialProfile != null) result.add(picasaSocialProfile);
        if(gravatarSocialProfile != null) result.add(gravatarSocialProfile);
        if(githubSocialProfile != null) result.add(githubSocialProfile);
        if(angellistSocialProfile != null) result.add(angellistSocialProfile);
        if(pinterestSocialProfile != null) result.add(pinterestSocialProfile);
        if(kloutSocialProfile != null) result.add(kloutSocialProfile);
        if(aboutmeSocialProfile != null) result.add(aboutmeSocialProfile);
        if(foursquareSocialProfile != null) result.add(foursquareSocialProfile);
        if(vimeoSocialProfile != null) result.add(vimeoSocialProfile);
        return result;
    }


    public String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }


    public String getId() {
        return id;
    }

    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    @JsonProperty("user")
    public void setUser(String user) {
        this.user = user;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    @JsonProperty("contactInfo")
    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    public List<Organization> getOrganizations() {
        return organizations;
    }

    @JsonProperty("organizations")
    public void setOrganizations(List<Organization> organizations) {
        this.organizations = organizations;
    }

    public SocialContactProfile getFacebookSocialProfile() {
        return facebookSocialProfile;
    }

    @JsonProperty("facebook")
    public void setFacebookSocialProfile(SocialContactProfile facebookSocialProfile) {
        facebookSocialProfile.setPlatform(Platform.FACEBOOK);
        this.facebookSocialProfile = facebookSocialProfile;
    }

    public SocialContactProfile getTwitterSocialProfile() {
        return twitterSocialProfile;
    }

    @JsonProperty("twitter")
    public void setTwitterSocialProfile(SocialContactProfile twitterSocialProfile) {
        twitterSocialProfile.setPlatform(Platform.TWITTER);
        this.twitterSocialProfile = twitterSocialProfile;
    }

    public SocialContactProfile getInstagramSocialProfile() {
        return instagramSocialProfile;
    }

    @JsonProperty("instagram")
    public void setInstagramSocialProfile(SocialContactProfile instagramSocialProfile) {
        instagramSocialProfile.setPlatform(Platform.INSTAGRAM);
        this.instagramSocialProfile = instagramSocialProfile;
    }

    public SocialContactProfile getGooglePlusSocialProfile() {
        return googlePlusSocialProfile;
    }

    @JsonProperty("googleplus")
    public void setGooglePlusSocialProfile(SocialContactProfile googlePlusSocialProfile) {
        googlePlusSocialProfile.setPlatform(Platform.GOOGLEPLUS);
        this.googlePlusSocialProfile = googlePlusSocialProfile;
    }


    public SocialContactProfile getPicasaSocialProfile() {
        return picasaSocialProfile;
    }

    @JsonProperty("picasa")
    public void setPicasaSocialProfile(SocialContactProfile picasaSocialProfile) {
        picasaSocialProfile.setPlatform(Platform.PICASA);
        this.picasaSocialProfile = picasaSocialProfile;
    }

    public SocialContactProfile getAngellistSocialProfile() {
        return angellistSocialProfile;
    }

    @JsonProperty("angellist")
    public void setAngellistSocialProfile(SocialContactProfile angellistSocialProfile) {
        angellistSocialProfile.setPlatform(Platform.ANGELLIST);
        this.angellistSocialProfile = angellistSocialProfile;
    }

    public SocialContactProfile getPinterestSocialProfile() {
        return pinterestSocialProfile;
    }

    @JsonProperty("pinterest")
    public void setPinterestSocialProfile(SocialContactProfile pinterestSocialProfile) {
        pinterestSocialProfile.setPlatform(Platform.PINTEREST);
        this.pinterestSocialProfile = pinterestSocialProfile;
    }

    public SocialContactProfile getLinkedinSocialProfile() {
        return linkedinSocialProfile;
    }

    @JsonProperty("linkedin")
    public void setLinkedinSocialProfile(SocialContactProfile linkedinSocialProfile) {
        linkedinSocialProfile.setPlatform(Platform.LINKEDIN);
        this.linkedinSocialProfile = linkedinSocialProfile;
    }

    public SocialContactProfile getKloutSocialProfile() {
        return kloutSocialProfile;
    }

    @JsonProperty("klout")
    public void setKloutSocialProfile(SocialContactProfile kloutSocialProfile) {
        kloutSocialProfile.setPlatform(Platform.KLOUT);
        this.kloutSocialProfile = kloutSocialProfile;
    }

    public SocialContactProfile getAboutmeSocialProfile() {
        return aboutmeSocialProfile;
    }

    @JsonProperty("aboutme")
    public void setAboutmeSocialProfile(SocialContactProfile aboutmeSocialProfile) {
        aboutmeSocialProfile.setPlatform(Platform.ABOUTME);
        this.aboutmeSocialProfile = aboutmeSocialProfile;
    }

    public SocialContactProfile getGravatarSocialProfile() {
        return gravatarSocialProfile;
    }

    @JsonProperty("gravatar")
    public void setGravatarSocialProfile(SocialContactProfile gravatarSocialProfile) {
        gravatarSocialProfile.setPlatform(Platform.GRAVATAR);
        this.gravatarSocialProfile = gravatarSocialProfile;
    }

    public SocialContactProfile getGithubSocialProfile() {
        return githubSocialProfile;
    }

    @JsonProperty("github")
    public void setGithubSocialProfile(SocialContactProfile githubSocialProfile) {
        githubSocialProfile.setPlatform(Platform.GITHUB);
        this.githubSocialProfile = githubSocialProfile;
    }

    public SocialContactProfile getFoursquareSocialProfile() {
        return foursquareSocialProfile;
    }

    @JsonProperty("foursquare")
    public void setFoursquareSocialProfile(SocialContactProfile foursquareSocialProfile) {
        foursquareSocialProfile.setPlatform(Platform.FOURSQUARE);
        this.foursquareSocialProfile = foursquareSocialProfile;
    }

    public SocialContactProfile getVimeoSocialProfile() {
        return vimeoSocialProfile;
    }

    @JsonProperty("vimeo")
    public void setVimeoSocialProfile(SocialContactProfile vimeoSocialProfile) {
        vimeoSocialProfile.setPlatform(Platform.VIMEO);
        this.vimeoSocialProfile = vimeoSocialProfile;
    }
}
