package com.blinq.authentication.impl;

import android.net.Uri;

import com.google.android.gms.plus.model.people.Person;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provide utilities to parse and convert contacts from different platform to
 * our headbox model.
 * <p/>
 * Maps the following platforms to the Headbox contact:
 * <ul>
 * <li>Map Facebook contact</li>
 * <li>Map Google contact</li>
 * <li>Map Twitter contact</li>
 * <li>Map Instagram contact</li>
 * </ul>
 */
public class ContactsMapper {

    /**
     * Map google plus account to headbox contact object.
     * -TODO Build a contacts mapper for this method.
     *
     * @param person - google plus person object.
     */
    public static Contact createContact(Person person) {

        Contact contact = new Contact();
        String personName = (person != null && person.hasDisplayName()) ? person
                .getDisplayName() : StringUtils.UNKNOWN_PERSON;
        contact.setName(personName);
        contact.setContactType(Platform.HANGOUTS);

        contact.setContactId((person != null && person.hasId()) ? person
                .getId() : StringUtils.EMPTY_STRING);

        return contact;
    }

    /**
     * Map google+ profile to Headbox profile.
     * * -TODO Build a contacts mapper for this method.
     */
    public static Contact createFriend(Person person) {

        Contact contact = new Contact();

        if (person.hasId()) {
            contact.setContactId(person.getId());
            contact.setAlternativeId(person.getId());
        }

        if (person.hasDisplayName())
            contact.setName(person.getDisplayName());

        if (person.getName().hasGivenName())
            contact.setFirstName(person.getName().getGivenName());

        if (person.getName().hasFamilyName())
            contact.setLastName(person.getName().getFamilyName());

        contact.setUserName(person.getId());
        contact.setLink(person.getUrl());
        contact.setContactType(Platform.HANGOUTS);

        contact.setHasCover(person.hasCover());
        contact.setHasPhoto(person.hasImage());

        if (contact.HasCover())
            contact.setCoverUri(Uri.parse(person.getCover().getCoverPhoto()
                    .getUrl()));
        if (contact.HasPhoto())
            contact.setPhotoUri(Uri.parse(person.getImage().getUrl()));

        return contact;
    }

    /**
     * Use with Google to Map user information to the Headbox profile.
     * TODO: Remove.
     * <p/>
     * Parses the response and returns profile object of the user.
     *
     * @param jsonResponse - Response string.
     * @throws JSONException
     */
    public static Contact createProfile(String jsonResponse)
            throws JSONException {

        JSONObject object = new JSONObject(jsonResponse);

        Contact profile = new Contact();
        profile.setContactId(object.getString(GoogleProperities.KEY_ID));
        profile.setUserName(object.getString(GoogleProperities.NAME_KEY));
        profile.setName(object.getString(GoogleProperities.NAME_KEY));
        profile.setContactType(Platform.HANGOUTS);

        return profile;
    }

    /**
     * TODO: REMOVE.
     */
    public interface GoogleProperities {
        public static final String KEY_ID = "id";
        public static final String GIVEN_NAME_KEY = "given_name";
        public static final String LAST_NAME_KEY = "family_name";
        public static final String NAME_KEY = "name";
    }

}