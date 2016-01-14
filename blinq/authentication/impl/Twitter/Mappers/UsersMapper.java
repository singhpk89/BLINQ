package com.blinq.authentication.impl.Twitter.Mappers;

import android.net.Uri;

import com.blinq.models.Contact;
import com.blinq.models.Platform;

import org.apache.commons.lang3.StringUtils;

import twitter4j.User;

/**
 * Created by Johan Hansson on 9/30/2014.
 * Map twitter users to headbox contacts.
 */
public class UsersMapper {

    /**
     * Create headbox contact based on {@link twitter4j.User} instance.
     *
     * @param twitterUser {@link twitter4j.Status} instance
     * @return {@link com.blinq.models.Contact} model.
     */
    public static Contact create(User twitterUser) {


        if (twitterUser == null)
            return null;

        Contact contact = new Contact();
        contact.setContactId(String.valueOf(twitterUser.getId()));
        contact.setName(twitterUser.getName());
        String userFirstName = "";
        String userLastName = "";
        contact.setFirstName(userFirstName);
        contact.setLastName(userLastName);
        contact.setUserName(twitterUser.getScreenName());
        contact.setLink(twitterUser.getURL());
        contact.setHasCover(true);
        contact.setContactType(Platform.TWITTER);

        String bannerImageUrl = twitterUser.getProfileBannerURL();
        String backgroundImageUrl = twitterUser.getProfileBackgroundImageURL();

        if (!StringUtils.isBlank(bannerImageUrl)) {
            contact.setCoverUri(Uri.parse(bannerImageUrl));

        } else if (!StringUtils.isBlank(backgroundImageUrl)) {
            contact.setCoverUri(Uri.parse(backgroundImageUrl));
        } else {
            contact.setHasCover(false);
        }

        contact.setPhotoUri(Uri.parse(twitterUser.getProfileImageURL()));

        return contact;
    }
}
