package com.blinq.authentication.settings;

import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.internal.SessionAuthorizationType;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds Facebook settings such as: application id,application permissions.
 */
public class FacebookSettings {

    public static final String APPLICATION_ID_PROPERTY = "com.facebook.sdk.ApplicationId";
    private String appId;
    private String appNamespace;
    private List<String> permissions;

    private SessionDefaultAudience defaultAudience;
    /**
     * Specifies that Session should attempt Single Sign On (SSO), and if that
     * does not work fall back to dialog auth. This is the default behavior.
     */
    private SessionLoginBehavior loginBehavior;

    public FacebookSettings() {

        loginBehavior = SessionLoginBehavior.SSO_WITH_FALLBACK;

        permissions = new ArrayList<String>();
        permissions.add(FacebookPermission.EMAIL.getName());
        permissions.add(FacebookPermission.READ_STREAM.getName());
        permissions.add(FacebookPermission.USER_PHOTOS.getName());
        permissions.add(FacebookPermission.FRIENDS_STATUS.getName());
        permissions.add(FacebookPermission.FRIENDS_PHOTOS.getName());
        permissions.add(FacebookPermission.FRIENDS_EVENTS.getName());
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getNamespace() {
        return appNamespace;
    }

    public void setNamespace(String namespace) {
        this.appNamespace = namespace;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public SessionDefaultAudience getDefaultAudience() {
        return defaultAudience;
    }

    public void setDefaultAudience(SessionDefaultAudience defaultAudience) {
        this.defaultAudience = defaultAudience;
    }

    public SessionLoginBehavior getLoginBehavior() {
        return loginBehavior;
    }

    public void setLoginBehavior(SessionLoginBehavior loginBehavior) {
        this.loginBehavior = loginBehavior;
    }

    /**
     * Facebook permissions.
     *
     * @author Johan Hansson
     * @see https://developers.facebook.com/docs/reference/fql/permissions/
     */
    public enum FacebookPermission {

        USER_BIRTHDAY("user_birthday", Type.READ),
        USER_HOMETOWN("user_hometown", Type.READ),
        EMAIL("email", Type.READ),
        USER_PHOTOS("user_photos", Type.READ),
        USER_STATUS("user_status", Type.READ),
        FRIENDS_STATUS("friends_status", Type.READ),
        USER_LOCATION("user_location", Type.READ),
        FRIENDS_LOCATION("friends_location", Type.READ),
        FRIENDS_EVENTS("friends_events", Type.READ),
        FRIENDS_HOMETOWN("friends_hometown", Type.READ),
        XMPP_LOGIN("xmpp_login", Type.READ),
        FRIENDS_PHOTOS("friends_photos", Type.READ),
        READ_MAILBOX("read_mailbox", Type.READ),
        READ_STREAM("read_stream", Type.READ),
        PUBLISH_ACTION("publish_actions", Type.PUBLISH),
        PUBLISH_STREAM("publish_stream", Type.PUBLISH),
        USER_EVENTS("user_events", Type.READ),
        FRIENDS_LIKES("friends_likes", Type.READ);


        /**
         * Permission type enum:
         * <ul>
         * <li>READ</li>
         * <li>PUBLISH</li>
         * </ul><br>
         */
        public static enum Type {
            PUBLISH(SessionAuthorizationType.PUBLISH),
            READ(SessionAuthorizationType.READ);

            private SessionAuthorizationType sessionAuthorizationType;

            private Type(SessionAuthorizationType sessionAuthorizationType) {
                this.sessionAuthorizationType = sessionAuthorizationType;
            }
        }

        ;

        private String name;
        private SessionAuthorizationType type;

        FacebookPermission(String name, Type type) {
            this.name = name;
            this.type = type.sessionAuthorizationType;
        }

        public String getName() {
            return name;
        }

        public static FacebookPermission fromName(String name) {
            for (FacebookPermission e : FacebookPermission.values()) {
                if (e.getName().equals(name))
                    return e;
            }
            return null;
        }

    }

}
