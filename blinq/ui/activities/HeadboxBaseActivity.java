package com.blinq.ui.activities;

import android.app.Activity;
import android.os.Bundle;

import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.SettingsManager;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.utils.Log;
import com.blinq.utils.UIUtils;

import java.util.HashMap;

/**
 * A base activity to be extended.
 * Has initialization for common methods
 * like logs, analytics, preferences and any other thing that is common between all of headbox activities.
 */
public class HeadboxBaseActivity extends Activity {

    private String tag = getClass().getSimpleName();

    protected BlinqAnalytics analytics;
    protected AnalyticsSender analyticsSender;
    protected PreferencesManager preferencesManager;
    protected SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        analytics = BlinqApplication.analyticsManager;
        analyticsSender = new AnalyticsSender(this.getApplicationContext());
        preferencesManager = BlinqApplication.preferenceManager;
        settingsManager = BlinqApplication.settingsManager;
    }


    protected final void fillScreen() {
        UIUtils.fillScreen(this);
    }

    protected final void hideActionBar() {
        getActionBar().hide();
    }
    
    protected final void logDebug(String debug) {
        Log.d(tag, debug);
    }

    protected final void sendEvent(String eventName) {
        analytics.sendEvent(eventName);

    }

    protected final void sendEvent(String eventName, String propertyName, Object propertyValue) {
        analytics.sendEvent(eventName, propertyName, propertyValue);

    }

    protected final void sendEvent(String eventName, HashMap<String, Object> properties) {
        analytics.sendEvent(eventName, properties);
    }

    protected final void sendEvent(String eventName, boolean sendToMixpanel, String category) {
        analytics.sendEvent(eventName, sendToMixpanel, category);
    }

    protected final void sendEvent(String eventName, String propertyName, Object propertyValue, boolean sendToMixpanel, String category) {
        analytics.sendEvent(eventName, propertyName, propertyValue, sendToMixpanel, category);
    }

    protected final void sendEvent(String eventName, HashMap<String, Object> properties, boolean sendToMixpanel, String category) {
        analytics.sendEvent(eventName, properties, sendToMixpanel, category);
    }

    protected final void complete() {
        analytics.complete();
    }

    protected final PreferencesManager getPreferencesManager() {
        return preferencesManager;
    }

    protected final void setUserProfileProperty(String propertyName,
                                                Object propertyValue) {
        analytics.setUserProfileProperty(propertyName, propertyValue);
    }


}
