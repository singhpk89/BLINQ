package com.blinq.ui.activities.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.blinq.R;
import com.blinq.PreferencesManager;
import com.blinq.SettingsManager;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.ui.activities.webpage.WebPageActivity;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;

/**
 * Hold the application settings.
 *
 * @author Johan Hansson.
 */
public class SettingsActivity extends Activity {

    private static BlinqAnalytics analyticsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        AppUtils.unRegisterActivityGoingIntoBackground(this);
        analyticsManager = new BlinqAnalytics(getApplicationContext());
        analyticsManager.sendEvent(
                AnalyticsConstants.OPENED_SETTINGS_SCREEN_EVENT, false,
                AnalyticsConstants.SETTINGS_CATEGORY);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment()).commit();
        }
    }

    @Override
    protected void onDestroy() {

        analyticsManager.complete();
        super.onDestroy();
    }

    @Override
    protected void onPause() {

        AppUtils.registerActivityGoingIntoBackground(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (AppUtils.isActivityIdle(this)) {
            finish();
        } else {
            AppUtils.unRegisterActivityGoingIntoBackground(this);
        }
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * A SettingsFragment containing settings view.
     */
    public static class SettingsFragment extends PreferenceFragment implements
            OnPreferenceClickListener, OnPreferenceChangeListener {

        public static final String TAG = SettingsFragment.class.getSimpleName();

        private PreferencesManager preferencesManager;
        private SettingsManager settingsManager;

        private boolean defaultNotificationPreferenceClicked = false;

        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Set the preferences layout in the fragment.
            addPreferencesFromResource(R.xml.preferences);
            //configureWepPreferences();
            configureDefaultNotificationAppPreference();
            configurePreferencesChangeListener();

            init();

        }

        @Override
        public void onResume() {

            if (defaultNotificationPreferenceClicked) {
                analyticsManager
                        .sendEvent(
                                AnalyticsConstants.PRESSED_BACK_IN_STREAM_NOTIFICATION_EVENT,
                                true, AnalyticsConstants.ONBOARDING_CATEGORY);
                defaultNotificationPreferenceClicked = false;
            }
            super.onResume();
        }

        @Override
        public void onStart() {

            super.onStart();
        }

        @Override
        public void onPause() {

            super.onPause();
        }

        /**
         * Initialize fragment components.
         */
        private void init() {

            preferencesManager = new PreferencesManager(getActivity());

            settingsManager = new SettingsManager(getActivity());
        }


        /**
         * Configure web preferences to open URLs internally when tapped.
         */
        private void configureWepPreferences() {

            // Tips and trick.
            Preference tipsAndTricksPrefrence = findPreference(getActivity()
                    .getResources().getString(
                            R.string.settings_tips_and_tricks_key));
            tipsAndTricksPrefrence.setOnPreferenceClickListener(this);

            tipsAndTricksPrefrence
                    .setIntent(getWebPageActivityIntent(getResources()
                            .getString(R.string.tips_and_tricks_url)));

            // Privacy policy.
            Preference privacyPolicyPreference = findPreference(getActivity()
                    .getResources().getString(
                            R.string.settings_privacy_policy_key));
            privacyPolicyPreference.setOnPreferenceClickListener(this);

            privacyPolicyPreference
                    .setIntent(getWebPageActivityIntent(getResources()
                            .getString(R.string.privacy_url)));

            // Terms of service.
            Preference termsOfServicePreference = findPreference(getActivity()
                    .getResources().getString(
                            R.string.settings_terms_of_service_key));
            termsOfServicePreference.setOnPreferenceClickListener(this);

            termsOfServicePreference
                    .setIntent(getWebPageActivityIntent(getResources()
                            .getString(R.string.terms_url)));

        }

        /**
         * Return intent to open given URL internally.
         *
         * @param URL web page URL to be opened.
         * @return intent to web page activity to open URL internally.
         */
        private Intent getWebPageActivityIntent(String URL) {

            Intent intent = new Intent(getActivity().getApplicationContext(),
                    WebPageActivity.class);
            intent.putExtra(Constants.WEB_PAGE_LINK, URL);

            return intent;
        }

        /**
         * Configure PreferenceChangeListener preference
         */
        private void configurePreferencesChangeListener() {

            findPreference(
                    getActivity().getResources().getString(
                            R.string.settings_theme_key)
            ).setOnPreferenceChangeListener(this);

        }

        /**
         * Configure the intent to be open when clicking
         */
        private void configureDefaultNotificationAppPreference() {

            Preference defaultNotificationApp = (Preference) findPreference(getActivity()
                    .getResources().getString(
                            R.string.settings_default_notification_app_key));

            // This feature is only enabled for API 18 and above.
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                defaultNotificationApp.setOnPreferenceClickListener(this);
                Intent intent = AppUtils.getNotificationsServiceIntent();
                defaultNotificationApp.setIntent(intent);

            } else {
                defaultNotificationApp.setEnabled(false);
            }
        }

        /**
         * Set status text for a "Default Notification APP" Preference.
         */
        @SuppressWarnings("unused")
        private void configureDefaultNotificationAppEnablingStatus() {

            String preferenceKey = getString(R.string.settings_default_notification_app_key);
            Preference preference = (Preference) findPreference(preferenceKey);
            if (settingsManager.isDefaultNotificationApp())
                preference.setSummary(R.string.notification_settings_enabled);
            else {
                preference.setSummary(R.string.notification_settings_disabled);
            }

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {

            String preferenceTitle = preference.getTitle().toString();
            // send Clicked on settings item event.
            analyticsManager.sendEvent(String.format(
                            AnalyticsConstants.CLICKED_ON_SETTINGS_EVENT,
                            preferenceTitle), false,
                    AnalyticsConstants.SETTINGS_CATEGORY
            );

            if (preference.getKey() == getString(R.string.settings_default_notification_app_key)) {
                defaultNotificationPreferenceClicked = true;
            }
            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            String preferenceKey = preference.getKey();
            if (preferenceKey.equals(getString(R.string.settings_theme_key))) {
                analyticsManager.sendEvent(
                        AnalyticsConstants.CHANGE_THEME_EVENT,
                        AnalyticsConstants.TYPE_PROPERTY, newValue.toString(),
                        false, AnalyticsConstants.SETTINGS_CATEGORY);
            }
            return true;
        }

    }

}
