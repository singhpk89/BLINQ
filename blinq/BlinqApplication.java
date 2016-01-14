package com.blinq;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import com.blinq.analytics.BlinqAnalytics;
import com.facebook.SessionDefaultAudience;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.settings.FacebookSettings;
import com.blinq.models.NotificationContentIntent;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.provider.MergeSearchProvider;
import com.blinq.utils.AppUtils;
import com.blinq.utils.FileUtils;
import com.blinq.utils.Log;
import com.blinq.utils.Log4jHelper;
import com.blinq.utils.ManageKeyguard;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.L;
import com.nu.art.software.TacB0sS.samples.genericPreferences.AppSpecificPreferenceStorage;
import com.nu.art.software.core.utils.SmarterHandler;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Headbox main entry point where libraries,settings,etc.. initialized.
 */
public class BlinqApplication extends Application {

    public static final String MONITORING_SERVICE_NAME = "com.blinq.service.MonitoringService";
    private static final String NAMESPACE = "blinq";
    private static final String TAG = BlinqApplication.class.getSimpleName();
    /**
     * Temporary.
     */
    public static Platform searchSource = null;
    public static MergeSearchProvider.SearchActivityType searchType = null;

    /**
     * Flag to indicate that feed view need to be refreshed.
     */
    public static boolean refresh = false;
    public static String contactId;
    public static List<SearchResult> searchResults = null;
    public static BlinqAnalytics analyticsManager;
    public static PreferencesManager preferenceManager;
    public static SettingsManager settingsManager;
    public static AppSpecificPreferenceStorage preferences;
    public static AnalyticsSender analyticsSender;
    public static boolean isDebug;

    /**
     * Temporary.
     */
    public static HashMap<Platform, NotificationContentIntent> notificationsIntents;
    public static Platform notification_platform = Platform.NOTHING;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = AppSpecificPreferenceStorage.createSingleton(this);
        analyticsManager = new BlinqAnalytics(this);
        preferenceManager = new PreferencesManager(this);
        settingsManager = new SettingsManager(this);
        analyticsSender = new AnalyticsSender(this);

        ManageKeyguard.initialize(this);
        initializeFacebook();
        startServices();
        initializeImageLoader();
        configureLog4j();
        setNetworkCountryIso();

        isDebug = AppUtils.isDebug(this);
        Log.d(TAG, TAG + " " + AppUtils.getVersionName(getApplicationContext()));
    }

    /**
     * Disable headbox services and receivers if the user is not completely authenticated.
     */
    private void manageComponents() {

        boolean disableComponents = !preferenceManager.getProperty(PreferencesManager.APP_AUTHENTICATED, false)
                && !preferenceManager.getProperty(PreferencesManager.COMPONENTS_DISABLED, false);

        if (disableComponents) {
            AppUtils.setAppComponentsEnabledSetting(getApplicationContext(),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            preferenceManager.setProperty(PreferencesManager.COMPONENTS_DISABLED, true);
            Log.d(TAG, "Disable app components..");
        }
    }

    private void startServices() {

        int delay = 150;
        SmarterHandler handler = new SmarterHandler(TAG);
        handler.removeAndPost(delay, new Runnable() {
            @Override
            public void run() {
                AppUtils.startMainService(BlinqApplication.this);
            }
        });
    }

    private void initializeFacebook() {

        FacebookSettings settings = new FacebookSettings();
        settings.setAppId(getString(R.string.facebookAppID));
        settings.setNamespace(NAMESPACE);
        settings.setDefaultAudience(SessionDefaultAudience.FRIENDS);
        FacebookAuthenticator.setSettings(settings);
    }

    private void configureLog4j() {

        String fileName = getApplicationContext().getFilesDir().getPath()
                + File.separator + FileUtils.LOG_FILE_NAME;
        String filePattern = "%d - [%c] - %p : %m%n";
        int maxBackupSize = 10;
        long maxFileSize = 1024 * 1024 * 2;
        Log4jHelper
                .configure(fileName, filePattern, maxBackupSize, maxFileSize);
    }

    private void initializeImageLoader() {

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                getApplicationContext())
                .threadPoolSize(5)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheExtraOptions(480, 320, null).build();
        L.disableLogging();

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config);
    }

    private void setNetworkCountryIso() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        preferenceManager.setNetworkCountyISO(telephonyManager.getNetworkCountryIso().toUpperCase());
    }

}