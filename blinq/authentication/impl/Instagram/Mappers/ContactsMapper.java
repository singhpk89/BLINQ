package com.blinq.authentication.impl.Instagram.Mappers;

import android.net.Uri;

import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Johan Hansson on 10/8/2014.
 */
public class ContactsMapper {

    public static Contact convert(JSONObject json) throws JSONException {

        Contact contact = null;

        if (json == null)
            return null;

        contact = new Contact();
        contact.setContactId(json.getString(Properties.USER_ID));
        contact.setPhotoUri(Uri.parse(json.getString(Properties.PICTURE)));
        contact.setHasPhoto(true);
        contact.setCoverUri(Uri.parse(json.getString(Properties.PICTURE)));
        contact.setHasCover(true);
        contact.setName(json.getString(Properties.FULL_NAME));
        contact.setUserName(json.getString(Properties.USER_NAME));
        contact.setLink(json.getString(Properties.WEBSITE));
        contact.setContactType(Platform.INSTAGRAM);
        if (StringUtils.isBlank(contact.getName())) {
            contact.setName(contact.getUserName());
        }

        return contact;
    }

    public static class Properties {

        /**
         * <b>Description:</b><br>
         * The Instagram user's ID<br>
         * <br>
         */
        public static final String USER_ID = "id";

        /**
         * <b>Description:</b><br>
         * The Instagram user's user name<br>
         * <br>
         */
        public static final String USER_NAME = "username";

        /**
         * <b>Description:</b><br>
         * The Instagram user's profile Picture<br>
         * <br>
         */
        public static final String PICTURE = "profile_picture";

        /**
         * <b>Description:</b><br>
         * The Instagram user's full name<br>
         * <br>
         */
        public static final String FULL_NAME = "full_name";

        /**
         * <b>Description:</b><br>
         * The Instagram user's website<br>
         * <br>
         */
        public static final String WEBSITE = "website";

    }
}
