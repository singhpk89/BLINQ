package com.blinq.authentication.impl.facebook.Mappers;

import org.json.JSONObject;

import com.blinq.authentication.settings.FacebookSettings.FacebookPermission;

import android.os.Bundle;

/**
 * The feed to be published on the wall
 *
 * @see https://developers.facebook.com/docs/reference/dialogs/feed/
 */
public class FeedMapper {

    /**
     * GRAPH PATH.
     */
    private static final String FEED = "feed";

    private Bundle mBundle = null;

    private FeedMapper(Builder builder) {
        this.mBundle = builder.mBundle;
    }

    public Bundle getBundle() {
        return mBundle;
    }

    public String getPath() {
        return FEED;
    }

    public FacebookPermission getPermission() {
        return FacebookPermission.PUBLISH_ACTION;
    }

    public static class Builder {

        Bundle mBundle;
        JSONObject mProperties = new JSONObject();

        /**
         * Properties.
         */
        public static class Parameters {
            public static final String MESSAGE = "message";
            public static final String LINK = "link";
            public static final String PICTURE = "picture";
            public static final String NAME = "name";
            public static final String CAPTION = "caption";
            public static final String DESCRIPTION = "description";
            public static final String PROPERTIES = "properties";
            public static final String ACTIONS = "actions";
        }

        public Builder() {
            mBundle = new Bundle();
        }

        /**
         * The name of the link attachment.
         *
         * @param name
         * @return {@link Builder}
         */
        public Builder setName(String name) {
            mBundle.putString(Parameters.NAME, name);
            return this;
        }

        /**
         * This message (shown as user input) attached to this post.
         *
         * @param message
         * @return {@link Builder}
         */
        public Builder setMessage(String message) {
            mBundle.putString(Parameters.MESSAGE, message);
            return this;
        }

        /**
         * The link attached to this post
         *
         * @param link
         * @return {@link Builder}
         */
        public Builder setLink(String link) {
            mBundle.putString(Parameters.LINK, link);
            return this;
        }

        /**
         * The URL of a picture attached to this post. The picture must be at
         * least 200px by 200px
         *
         * @param picture
         * @return {@link Builder}
         */
        public Builder setPicture(String picture) {
            mBundle.putString(Parameters.PICTURE, picture);
            return this;
        }

        /**
         * The caption of the link (appears beneath the link name). If not
         * specified, this field is automatically populated with the URL of the
         * link.
         *
         * @param caption
         * @return {@link Builder}
         */
        public Builder setCaption(String caption) {
            mBundle.putString(Parameters.CAPTION, caption);
            return this;
        }

        /**
         * The description of the link (appears beneath the link caption). If
         * not specified, this field is automatically populated by information
         * scraped from the link, typically the title of the page.
         *
         * @param description
         * @return {@link Builder}
         */
        public Builder setDescription(String description) {
            mBundle.putString(Parameters.DESCRIPTION, description);
            return this;
        }

        public FeedMapper build() {
            // add properties if needed
            if (mProperties.length() > 0) {
                mBundle.putString(Parameters.PROPERTIES, mProperties.toString());
            }

            return new FeedMapper(this);
        }

    }

}
