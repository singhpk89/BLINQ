package com.blinq.authentication.impl.provider;

import android.net.Uri;

import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.models.server.ContactInfo;
import com.blinq.models.server.ContactProfile;
import com.blinq.models.server.SocialContactProfile;
import com.blinq.models.server.Organization;
import com.blinq.utils.StringUtils;

/**
 * Maps json object to Contacts
 * Supports:
 * Facebook, Twitter, GooglePlus, Instagram
 */
public class ServerContactMapper
{
    public static Contact convertSocialProfileToContact(ContactProfile profile, Platform platform){
        if (profile == null)
            return null;
        switch(platform) {
            case FACEBOOK:
                return convertFromFacebook(profile.getFacebookSocialProfile());
            case TWITTER:
                return convertFromTwitter(profile.getTwitterSocialProfile());
//            case GOOGLEPLUS:
//                return convertFromGooglePlus(profile.getGooglePlusSocialProfile());
            case INSTAGRAM:
                return convertFromInstagram(profile.getInstagramSocialProfile());
            default:
                return new Contact();
        }
    }

    private static Contact convertFromFacebook(SocialContactProfile fbContact) {
        Contact contact = new Contact();
        contact.setContactId(fbContact.getId());
        contact.setName(fbContact.getUsername());
        contact.setUserName(fbContact.getUsername());
        Uri uri = Uri.parse(fbContact.getPhotoUrl() == null ? "":fbContact.getPhotoUrl());
        contact.setPhotoUri(uri);
        contact.setContactType(fbContact.getPlatform());
        contact.setCoverUri(Uri.parse(""));
        contact.setAlternativeId(fbContact.getId());
        contact.setIdentifier(fbContact.getId());
        contact.setFirstName("");
        contact.setLastName("");
        contact.setEmail("");
        return contact;
    }

    private static Contact convertFromTwitter(SocialContactProfile twContact) {
        Contact contact = new Contact();
        contact.setContactId(twContact.getId());
        contact.setName(twContact.getUsername());
        contact.setUserName(twContact.getUsername());
        Uri uri = Uri.parse(twContact.getPhotoUrl() == null ? "":twContact.getPhotoUrl());
        contact.setPhotoUri(uri);
        contact.setContactType(twContact.getPlatform());
        contact.setCoverUri(Uri.parse(""));
        contact.setIdentifier(twContact.getId());
        contact.setAlternativeId(twContact.getId());
        contact.setFirstName("");
        contact.setLastName("");
        contact.setEmail("");
        return contact;
    }

    private static Contact convertFromGooglePlus(SocialContactProfile gContact) {
        Contact contact = new Contact();
        contact.setContactId(gContact.getId());
        contact.setName(gContact.getUsername());
        contact.setUserName(gContact.getUsername());
        Uri uri = Uri.parse(gContact.getPhotoUrl() == null ? "":gContact.getPhotoUrl());
        contact.setPhotoUri(uri);
        contact.setContactType(gContact.getPlatform());
        contact.setCoverUri(Uri.parse(""));
        contact.setIdentifier(gContact.getId());
        contact.setAlternativeId(gContact.getId());
        contact.setFirstName("");
        contact.setLastName("");
        contact.setEmail("");
        return contact;
    }

    private static Contact convertFromInstagram(SocialContactProfile iContact) {
        Contact contact = new Contact();
        contact.setContactId(iContact.getId());
        contact.setName(iContact.getUsername());
        contact.setUserName(iContact.getUsername());
        Uri uri = Uri.parse(iContact.getPhotoUrl() == null ? "":iContact.getPhotoUrl());
        contact.setPhotoUri(uri);
        contact.setContactType(iContact.getPlatform());
        contact.setCoverUri(Uri.parse(""));

        contact.setIdentifier(iContact.getId());
        contact.setAlternativeId(iContact.getId());
        contact.setFirstName("");
        contact.setLastName("");
        contact.setEmail("");
        return contact;
    }


    public static Contact convertStaticInfo(ContactProfile profile) {
        Contact contact = new Contact();
        contact.setContactType(Platform.STATICINFO);

        contact.setContactId(profile.getId());
        contact.setName(profile.getUser());
        contact.setIdentifier(profile.getId());
        contact.setFirstName("");
        contact.setLastName("");
        contact.setEmail("");

        contact.setPhotoUri(Uri.parse(getPhoto(profile)));
        contact.setCoverUri(Uri.parse(""));

        setName(profile.getContactInfo(), contact);

        if(profile.getOrganizations() != null && !profile.getOrganizations().isEmpty()) {
            Organization organization = profile.getOrganizations().get(0); //assume sorted by date
            contact.setTitle(organization.getTitle());
            contact.setOrganizationName(organization.getName());
        }

        contact.setBio(getBio(profile));
        contact.setSocialProfiles(profile.getSocialProfiles());

        return contact;
    }

    private static void setName(ContactInfo contactInfo, Contact contact) {
        if(contactInfo != null) {
            if(!StringUtils.isBlank(contactInfo.getFullName())) {
                contact.setName(contactInfo.getFullName());
            }else if(!StringUtils.isBlank(contactInfo.getGivenName())) {
                contact.setFirstName(contactInfo.getGivenName());
                contact.setName(contactInfo.getGivenName());
            }else if(!StringUtils.isBlank(contactInfo.getFamilyName())) {
                contact.setLastName(contactInfo.getFamilyName());
                contact.setName(contactInfo.getFamilyName());
            }
        }
    }

    private static String getPhoto(ContactProfile profile) {
        if(profile.getLinkedinSocialProfile()!= null &&
                profile.getLinkedinSocialProfile().getPhotoUrl() != null)
            return profile.getLinkedinSocialProfile().getPhotoUrl();
        else if(profile.getAngellistSocialProfile()!= null &&
                profile.getAngellistSocialProfile().getPhotoUrl() != null)
            return profile.getAngellistSocialProfile().getPhotoUrl();
        else if(profile.getFacebookSocialProfile()!= null &&
                profile.getFacebookSocialProfile().getPhotoUrl() != null)
            return profile.getFacebookSocialProfile().getPhotoUrl();
        else if(profile.getTwitterSocialProfile()!= null &&
                profile.getTwitterSocialProfile().getPhotoUrl() != null)
            return profile.getTwitterSocialProfile().getPhotoUrl();
        else if(profile.getGooglePlusSocialProfile()!= null &&
                profile.getGooglePlusSocialProfile().getPhotoUrl() != null)
            return profile.getGooglePlusSocialProfile().getPhotoUrl();
        else if(profile.getPinterestSocialProfile()!= null &&
                profile.getPinterestSocialProfile().getPhotoUrl() != null)
            return profile.getPinterestSocialProfile().getPhotoUrl();
        else if(profile.getGravatarSocialProfile()!= null &&
                profile.getGravatarSocialProfile().getPhotoUrl() != null)
            return profile.getGravatarSocialProfile().getPhotoUrl();

        return "";
    }

    private static String getBio(ContactProfile profile) {
        if(profile.getTwitterSocialProfile() != null && profile.getTwitterSocialProfile().getBio() != null)
            return profile.getTwitterSocialProfile().getBio();
        if(profile.getAngellistSocialProfile() != null && profile.getAngellistSocialProfile().getBio() != null)
            return profile.getAngellistSocialProfile().getBio();
        if(profile.getPinterestSocialProfile() != null  && profile.getPinterestSocialProfile().getBio() != null)
            return profile.getPinterestSocialProfile().getBio();

        return null;
    }

}
