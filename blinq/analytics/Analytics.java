package com.blinq.analytics;

import java.util.HashMap;

import android.content.Context;

public abstract class Analytics {

    protected Context context;
    protected Boolean isAnalyticsEnabled;

    /**
     * Send event
     *
     * @param eventName
     */
    public abstract void sendEvent(String eventName);

    /**
     * Send event with property value
     */
    public abstract void sendEvent(String eventName,
                                   String propertyName, Object propertyValue);

    /**
     * Send event with properties list.
     */
    public abstract void sendEvent(String eventName,
                                   HashMap<String, Object> propertiesHash);


    /**
     * Send event, is sent to mixpanel, Category for google analytics
     */
    public abstract void sendEvent(String eventName, boolean sendToMixpanel, String category);

    /**
     * Send event with property value, is sent to mixpanel,
     * Category for google analytics
     */
    public abstract void sendEvent(String eventName,
                                   String propertyName, Object propertyValue, boolean sendToMixpanel, String category);

    /**
     * Send event with properties list, is sent to mixpanel,
     * Category for google analytics
     */
    public abstract void sendEvent(String eventName,
                                   HashMap<String, Object> propertiesHash, boolean sendToMixpanel, String category);


    /**
     * Invoked when application is shutdown to flush the analytics manager to send all
     * remaining events.
     */
    public abstract void complete();

    /**
     * Set property in  user profile
     */
    public abstract void setUserProfileProperty(String propertyName,
                                                Object propertyValue);
}
