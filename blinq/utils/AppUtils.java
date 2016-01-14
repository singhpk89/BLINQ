package com.blinq.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.models.Contact;
import com.blinq.provider.ContactsManager;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.receivers.CallsReceiver;
import com.blinq.receivers.NotificationBroadcastReceiver;
import com.blinq.service.ConnectivityService;
import com.blinq.service.MonitoringService;
import com.blinq.service.TasksObserver;
import com.blinq.service.platform.FacebookUtilsService;
import com.blinq.service.platform.GoogleUtilsService;
import com.blinq.ui.activities.notificationsetup.NotificationSetupActivity;
import com.blinq.ui.activities.splash.SplashActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

/**
 * Holds application utilities: like get meta-data, start main service and
 * other.
 *
 * @author Exalt
 */

public class AppUtils {

    private static final String TAG = AppUtils.class.getSimpleName();
    private static final String HEADBOX_CONFIGURATION_FILE = "/headbox.properties";
    private static final String EXTRA_FINISH_ACTIVITY_ON_COMPLETE = "finishActivityOnSaveCompleted";
    // Private request code for the sender
    private static final int ALRAM_REQUEST_CODE = 234324243;
    public static final int CONTACT_SAVE_INTENT_REQUEST = 4;
    public static final String SIMPLE_DATE_FORMAT = "dd/MM/yyyy";
    public static final String DEFAULT_DATE = "1/1/1979";
    private static final String ADD_APP_SHORTCUT_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";
    private static final String APP_SHORTCUT_INTENT_EXTRA = "duplicate";
    private static Random generator;

    /**
     * Get meta-data from manifest using input string as a key.
     *
     * @param context      - the activity to use to get meta-data.
     * @param propertyName meta-data field alias in Manifest.
     */
    public static String getMetadataByPropertyName(Context context,
                                                   String propertyName) {

        try {
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (applicationInfo.metaData != null) {
                return applicationInfo.metaData.getString(propertyName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, e.getMessage() + "");
        }
        return null;
    }

    /**
     * Check if the service is running or not.
     *
     * @param context     - activity from which to call this method.
     * @param serviceName service class name.
     * @return True if the service is running.
     */
    public static boolean isServiceStarted(Context context, String serviceName) {

        ActivityManager manager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        // Retrieves running services then check the existence of needed service
        // by
        // comparing it's name.
        for (RunningServiceInfo runningServiceInfo : manager
                .getRunningServices(200)) {
            if (runningServiceInfo.service.getClassName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check device version.
     */
    public static boolean isICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    /**
     * Get Application version Code.
     */
    public static int getVersionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
        }
        return 0;
    }

    /**
     * Get Application version Name.
     */
    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Delete Application preferences files.
     */
    public static void deleteAppPreferences(Context context) {
        File sharedPreferenceFile = new File(Constants.APPLICATION_PATH
                + context.getPackageName()
                + Constants.DEAFAULT_SHARED_PREFERENCES_PATH);
        File[] listFiles = sharedPreferenceFile.listFiles();
        for (File file : listFiles) {
            file.delete();
        }
    }

    /**
     * Convert to date from long millisecond.
     *
     * @param currentTimeMillis - date in milliseconds.
     */
    public static Date convertToDate(long currentTimeMillis) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentTimeMillis);
        Date date = cal.getTime();
        return date;
    }

    /**
     * Starts Headbox <code>MainService</code>
     *
     * @param context - context to start service from.
     */
    public static void startMainService(Context context) {

        // Check if the service is running or not.
        if (!isServiceStarted(context, BlinqApplication.MONITORING_SERVICE_NAME)) {

            Intent intent = new Intent(context, MonitoringService.class);
            context.startService(intent);
            Log.d(TAG, "Start MonitoringService..");
        } else {
            Log.d(TAG, "MonitoringService is already started.");
        }
    }

    /**
     * Start service.
     *
     * @param context - context to start service from.
     */
    public static void startService(String name, Class<?> service,
                                    Context context) {

        // Check if the service is running or not.
        if (!isServiceStarted(context, name)) {
            Intent mainServiceIntent = new Intent(context, service);
            context.startService(mainServiceIntent);
            Log.d(TAG, "Start Main Service..");

        } else {
            Log.d(TAG, "Main service is already running");
        }
    }

    /**
     * Get resource value by id.
     *
     * @param context Activity from which to call this method.
     */
    public static String getResourceString(Context context, int resourceId) {
        return context.getResources().getString(resourceId);
    }

    /**
     * Get Array of string from xml file.
     *
     * @param arrayId - resource (array) id.
     */
    public static String[] getStringArrayById(Context context, int arrayId) {
        String[] array = context.getResources().getStringArray(arrayId);
        return array;
    }

    /**
     * Return random string from an array of string defined on xml file.
     *
     * @param arrayId - resource (array) id.
     */
    public static String getRandomString(Context context, int arrayId) {

        String[] array = getStringArrayById(context, arrayId);
        if (generator == null) {
            generator = new Random();
        }
        String string = array[generator.nextInt(array.length)];
        return string;
    }

    /**
     * Send refresh request for all running activities new message added.
     *
     * @param context   - activity from which to call this method.
     * @param messageId - ID of new received message.
     * @param feedId    - ID of received message feed
     */
    public static void sendRefreshBroadcast(Context context, String messageId,
                                            String feedId) {

        Intent intent = new Intent(Constants.ACTION_REFRESH_INSERT);
        intent.putExtra(Constants.FEED_EXTRA, feedId);
        intent.putExtra(Constants.MESSAGE_EXTRA, messageId);
        context.sendBroadcast(intent);
    }

    public static void sendMessageStatusChangedBroadcast(Context context,
                                                         String messageId, String feedId) {

        Intent intent = new Intent(Constants.ACTION_REFRESH_MESSAGE_TYPE_CHANGE);
        intent.putExtra(Constants.FEED_EXTRA, feedId);
        intent.putExtra(Constants.MESSAGE_EXTRA, messageId);
        context.sendBroadcast(intent);
    }

    /**
     * Send refresh request for all running activities when a message updated.
     *
     * @param context      - activity from which to call this method.
     * @param oldMessageId - ID of the updated message.
     * @param messageId    - ID of new message.
     * @param feedId       - ID of received message feed
     */
    public static void sendRefreshBroadcastOnUpdate(Context context,
                                                    String messageId, String oldMessageId, String feedId) {

        Intent intent = new Intent(Constants.ACTION_REFRESH_UPDATE);
        intent.putExtra(Constants.FEED_EXTRA, feedId);
        intent.putExtra(Constants.MESSAGE_EXTRA, messageId);
        intent.putExtra(Constants.OLD_MESSAGE_EXTRA, oldMessageId);
        context.sendBroadcast(intent);
    }

    /**
     * Check if the application is running in the foreground
     *
     * @param context - activity from which to call this method.
     */
    public static boolean isAppOnForeground(Context context) {

        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;

    }

    public static boolean isAppOnBackground(Context context) {
        return !isAppOnForeground(context);
    }

    /**
     * Check if the application has been loaded before or not.
     */
    public static boolean isAppLoaded(Context context) {
        PreferencesManager preferencesManager = new PreferencesManager(context);
        return preferencesManager.isLoaded();
    }

    /**
     * Hide soft keyboard.
     *
     * @param activity activity to hide keyboard from.
     */
    public static void hideKeyboard(Activity activity) {

        if (activity != null) {
            InputMethodManager inputManager = (InputMethodManager) activity
                    .getApplicationContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);

            if (activity.getCurrentFocus() != null
                    && activity.getCurrentFocus().getWindowToken() != null) {
                inputManager.hideSoftInputFromWindow(activity.getCurrentFocus()
                        .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    /**
     * Show soft keyboard.
     *
     * @param activity activity to show keyboard over.
     * @param view     view to show keyboard for.
     */
    public static void showKeyboard(Activity activity, View view) {

        if (activity != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            inputMethodManager.showSoftInput(view,
                    InputMethodManager.SHOW_IMPLICIT);
        }

    }

    /**
     * Check whether the has upgraded and do upgrading process.
     *
     * @param context - activity from which to call this method.
     */
    public static void upgradeApp(Context context) {

        PreferencesManager manager = new PreferencesManager(context);

        FeedProviderImpl.getInstance(context).refresh();
        int currentVersionCode = manager.getVersionCode();
        String currentVersionName = manager.getVersionName();
        int versionCode = AppUtils.getVersionCode(context);
        String versionName = AppUtils.getVersionName(context);

        if (versionCode != currentVersionCode
                || !versionName.equals(currentVersionName)) {

            Log.d(TAG, "Start upgrading Headbox...");

            PreferencesManager preferencesManager = new PreferencesManager(
                    context);
            preferencesManager.setAppLoaded(false);

            // Delete Logs folders.
            FileUtils
                    .deleteDirectory(new File(FileUtils.COMPRESSED_FOLDER_PATH));
            FileUtils.deleteDirectory(new File(FileUtils.DATA_FOLDER_PATH));
            FileUtils.deleteDirectory(new File(FileUtils.HEADBOX_FOLDER_PATH));

            manager = new PreferencesManager(context);
            manager.setVersionCode(versionCode);
            manager.setVersionName(versionName);

        }

    }

    /**
     * Generate user distinct Id from the device Id
     *
     * @param context
     * @return
     */
    public static String generateUserDistinctId(Context context) {
        // get the device Id
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID) + UUID.randomUUID();

    }

    /**
     * To disable/enable headbox receivers and services programmatically.
     *
     * @param context - activity or context.
     * @param enabled - enabled setting,ex:PackageManager.COMPONENT_ENABLED_STATE_ENABLED
     */
    public static void setAppComponentsEnabledSetting(Context context, int enabled) {

        List<Class<?>> components = new ArrayList<Class<?>>();
        components.add(CallsReceiver.class);
        components.add(NotificationBroadcastReceiver.class);
        components.add(MonitoringService.class);
        components.add(ConnectivityService.class);
        components.add(FacebookUtilsService.class);
        components.add(GoogleUtilsService.class);

        for (Class<?> component : components) {
            setComponentEnabledSetting(context, component, enabled);
        }
    }

    /**
     * Enable/disable/change the status setting of a certain component.
     *
     * @param context    - activity or context.
     * @param component  - component class.
     * @param statusCode - enabled setting.ex:PackageManager.COMPONENT_ENABLED_STATE_DISABLED
     */
    public static void setComponentEnabledSetting(Context context, Class<?> component, int statusCode) {

        ComponentName componentName = new ComponentName(context, component);
        context.getPackageManager().setComponentEnabledSetting(componentName, statusCode, PackageManager.DONT_KILL_APP);
    }

    /**
     * Clear Application preferences files.
     */
    public static void clearAppPreferences(String preferencesName,
                                           Context context) {

        SharedPreferences settings = context.getSharedPreferences(
                preferencesName, Context.MODE_PRIVATE);
        settings.edit().clear().commit();
    }

    public static void clearApplicationData(Context context) {
        File cache = context.getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!s.equals("lib") && !s.equals("databases")) {
                    FileUtils.deleteDirectory(new File(appDir, s));
                }
            }
        }
    }

    /**
     * Load the head box configuration and save it in shared preferences
     */
    public static void loadHeadboxConfiguration(Context context) {

        PreferencesManager preferencesManager = new PreferencesManager(context);

        // check if application was opened before
        if (!preferencesManager.isLoaded()) {

            InputStream inputStream = context.getClass().getResourceAsStream(
                    HEADBOX_CONFIGURATION_FILE);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(
                        inputStream);

                try {
                    Properties headBoxProperties = new Properties();

                    headBoxProperties.load(inputStreamReader);
                    // load all properties then save them on preferences
                    for (String key : headBoxProperties.stringPropertyNames()) {

                        preferencesManager.setProperty(key,
                                headBoxProperties.getProperty(key));
                    }

                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
            }

        }
    }

    /**
     * Read the application logs using log-cat process.
     *
     * @return string of application logs.
     */
    public static String readApplicationLogs(Context context) {

        String packageName = context.getPackageName();

        Process logcatProcess = null;
        BufferedReader logsReader = null;

        try {
            // Filter system logs to get only headbox application logs.
            logcatProcess = Runtime.getRuntime().exec(
                    "logcat -d -v time " + "[" + packageName + "]:V *:V");

            logsReader = new BufferedReader(new InputStreamReader(
                    logcatProcess.getInputStream()));

            String line;
            final StringBuilder log = new StringBuilder();

            // Start reading
            while ((line = logsReader.readLine()) != null) {
                log.append(line + "\n");
            }

            return log.toString();

        } catch (IOException e) {

            Log.e(TAG, "Error reading application logs : " + e.getMessage());
        } finally {

            // Close logs reader
            if (logsReader != null)

                try {

                    logsReader.close();

                } catch (IOException e) {

                    Log.e(TAG, "Error closing logs reader : " + e.getMessage());
                }

        }

        return "";
    }

    /**
     * @return string of specific device information (Model, Resolution,
     * Version) .
     */
    public static String getDeviceInfo(Activity activity) {

        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay()
                .getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;

        String debugInfo = "\n\n\n\n\n";

        debugInfo += android.os.Build.MODEL + " (" + Build.VERSION.RELEASE
                + ")";
        debugInfo += "\nResolution: " + width + "X" + height;
        try {
            debugInfo += "\nBlinq "
                    + activity.getPackageManager().getPackageInfo(
                    activity.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            Log.e("", "Failed to get Blinq Version");
        }

        return debugInfo;
    }

    /**
     * Open android native add-contact screen with pre-filled info
     *
     * @param activity    Activity activity
     * @param requestCode Intent request code.
     * @param phoneNumber contact phone number
     */
    public static void openAddContactIntent(final Activity activity,
                                            int requestCode, String phoneNumber) {

        // Creates a new Intent to insert a contact.
        Intent intent = new Intent(Intent.ACTION_INSERT);
        // Sets the MIME type
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber);
        intent.putExtra(EXTRA_FINISH_ACTIVITY_ON_COMPLETE, true);
        // Sends the Intent with an request ID
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Open photo using the default gallary app.
     *
     * @param photoUri
     */
    public static void openPhotoIntent(Activity activity, Uri photoUri) {

        Intent intent = new Intent(Intent.ACTION_VIEW, photoUri);
        activity.startActivity(intent);
    }

    /**
     * Registers the activity is going into background(should be called on
     * activity pause).
     *
     * @param activity
     */
    public static void registerActivityGoingIntoBackground(final Activity activity) {

        PreferencesManager preferencesManager = new PreferencesManager(
                activity.getApplicationContext());
        long currentTime = System.currentTimeMillis();
        String activityName = activity.getClass().getSimpleName();
        preferencesManager.setProperty(activityName
                + StringUtils.ACTIVITY_LEAVING_SUFFIX, currentTime);
        preferencesManager.setProperty(activityName
                + StringUtils.ACTIVITY_COME_FROM_BACKGROUND_SUFFIX, true);

    }

    /**
     * Get notification settings intent.
     */
    public static Intent getNotificationsServiceIntent() {

        Intent intent;

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent = new Intent(NotificationSetupActivity.NOTIFICATION_ACCESS);
        } else {
            intent = new Intent(
                    android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        return intent;
    }

    /**
     * Checks if the activity come from background and stayed there more than
     * allowed idle time.
     *
     * @param activity
     */
    public static boolean isActivityIdle(Activity activity) {
        PreferencesManager preferencesManager = new PreferencesManager(
                activity.getApplicationContext());
        String activityName = activity.getClass().getSimpleName();
        // check if the activity came from background
        if (preferencesManager.getProperty(activityName
                + StringUtils.ACTIVITY_COME_FROM_BACKGROUND_SUFFIX, false)) {

            long currentTime = System.currentTimeMillis();
            long activityLeavingTime = preferencesManager.getProperty(
                    activityName + StringUtils.ACTIVITY_LEAVING_SUFFIX,
                    currentTime);
            if (Math.abs(currentTime - activityLeavingTime) >= PreferencesManager.ALLOWED_IDLE_TIME_In_MILLIS)
                return true;
            else
                return false;
        } else {
            return false;
        }

    }

    /**
     * UnRegisters the activity from background state(should be called on
     * activity create).
     *
     * @param activity
     */
    public static void unRegisterActivityGoingIntoBackground(Activity activity) {
        PreferencesManager preferencesManager = new PreferencesManager(
                activity.getApplicationContext());
        preferencesManager.setProperty(activity.getClass().getSimpleName()
                + StringUtils.ACTIVITY_COME_FROM_BACKGROUND_SUFFIX, false);
    }

    /**
     * checks if the package name exist on user's device.
     *
     * @param context
     * @param applicationPackageName
     * @return
     */
    public static boolean isPackageExist(Context context,
                                         String applicationPackageName) {
        try {
            context.getPackageManager().getApplicationInfo(
                    applicationPackageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;

        }

    }

    /**
     * Return the selected font scale from device settings.
     *
     * @param context application context.
     * @return font scale.
     */
    public static float getFontScaleFromDeviceSettings(Context context) {

        return context.getResources().getConfiguration().fontScale;

    }

    /**
     * @param context - application context
     * @param text    - text that will be copied to device clipboard
     */
    public static void copyTextToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context
                .getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clipData = android.content.ClipData.newPlainText("", text);
        clipboard.setPrimaryClip(clipData);

    }

    /**
     * Notify that Headbox database was upgraded
     *
     * @param context
     */
    public static void HeadBoxDatabaseUpgraded(Context context) {

        Log.d(TAG, "Upgrade db");
        PreferencesManager preferencesManager = new PreferencesManager(context);
        preferencesManager.setAppLoaded(false);
        preferencesManager.setDatabaseUpgraded(true);

    }

    /**
     * Check if the activity is still active.
     */
    public static boolean isActivityActive(Activity activity) {

        if (activity == null || activity.isFinishing())
            return false;

        return true;
    }

    /**
     * Insert contact [added by the intent] to Headbox contacts.
     *
     * @param intent - intent to retrieve new contact from.
     */
    public static void addContact(Context context, Intent intent) {

        // Getting the URI of the last added contact.
        if (intent != null && intent.getData() != null) {
            Uri contactUri = intent.getData();
            String contactId = contactUri.getLastPathSegment();
            Contact contact;
            // Fetch and convert to Headbox contact
            if (contactId != null) {
                contact = ContactsManager.getContact(context, contactId);
                if (contact != null
                        && org.apache.commons.lang3.StringUtils
                        .isNoneBlank(contact.getIdentifier()))
                    FeedProviderImpl.getInstance(context)
                            .insertContact(contact);
            }
        }
    }

    /**
     * Add shortcut for the Main Activitiy on Home screen
     *
     * @param context
     */
    public static void addHomeScreenShortcut(Context context) {

        Intent shortcutIntent = new Intent(context, SplashActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getResources()
                .getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context,
                        R.drawable.ic_launcher)
        );
        addIntent.putExtra(APP_SHORTCUT_INTENT_EXTRA, false);
        addIntent.setAction(ADD_APP_SHORTCUT_ACTION);
        context.sendBroadcast(addIntent);
    }

    /**
     * Get the current date-time string
     *
     * @param dateFormat - the pattern of the date time like yyyy-mm-dd
     */
    public static String getCurrentDateTime(String dateFormat) {
        SimpleDateFormat dataFormat = new SimpleDateFormat(dateFormat);
        return dataFormat.format(new Date());

    }

    /**
     * Check the it is a new day since the user last entered the application
     */
    public static boolean isNewDay(Context context) {

        SimpleDateFormat dateFormater = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
        PreferencesManager preferencesManager = new PreferencesManager(context);
        Date nowDate = new Date();
        Date lastCheckedDate = new Date();
        String now = StringUtils.EMPTY_STRING;
        try {
            now = dateFormater.format(nowDate);
            nowDate = (Date) dateFormater.parse(now);
            lastCheckedDate = (Date) dateFormater.parse(preferencesManager
                    .getProperty(PreferencesManager.USER_ENTERED_LAST_DAY,
                            DEFAULT_DATE));
        } catch (ParseException e) {
            Log.e(TAG, "Data Parsing Error:" + e.toString());
            return true;
        }

        if (nowDate.after(lastCheckedDate)) {

            return true;
        }

        return false;

    }

    /**
     * Get the selected ringer mode from device settings.
     *
     * @param context application context.
     * @return the ringer mode of device.
     */
    public static int getDeviceRingerMode(Context context) {

        AudioManager am = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);

        return am.getRingerMode();

    }

    /**
     * @return true if there a ringing or running call.
     */
    public static boolean isThereACall(Context context) {

        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK
                || telephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {

            return true;
        }

        return false;
    }

    /**
     * Just a temporary code to be used to apply strict mode any-where it's
     * called.
     */
    public static void applyStrictMode() {

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll().penaltyLog().penaltyDialog().build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());
    }

    /**
     * Find the load time from start time until now.
     */
    public static long findTime(long startTime) {
        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;

        return loadTime;
    }


    /**
     * Used to fix the Translucent problem in swipe-back library occurred in
     * newest versions of android > 4.4.2.
     *
     * @param activity activity to be converted.
     */
    public static void convertActivityFromTranslucent(Activity activity) {

        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                Method method = Activity.class
                        .getDeclaredMethod("convertFromTranslucent");
                method.setAccessible(true);
                method.invoke(activity);
            }
        } catch (Throwable t) {
        }
    }



    /**
     * Check whether a specific app is on foreground or on background
     *
     * @param context     - context from which to call this method.
     * @param packageName - application package name.
     */
    public static boolean isAppOnForeground(final Context context, String packageName) {

        if (packageName.equals(TasksObserver.COM_FACEBOOK_ORCA)) {
            return TasksObserver.getInstance(context).isFacebookMessengerServiceRunning(TasksObserver.FACEBOOK_BLUE_SERVICE);
        }

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();

        if (processes.isEmpty())
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : processes) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    packageName.equals(appProcess.processName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if the app is in debug or production mode
     * @return true if debug. False otherwise
     * @param activity
     */
    public static boolean isDebug(final Context context) {
        return (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }
}