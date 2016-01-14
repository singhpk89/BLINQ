package com.blinq.service;

import android.app.ActivityManager;
import android.content.Context;

import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.utils.Log;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.app.ActivityManager.RunningAppProcessInfo;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

/**
 * startListen is the main entry point
 * Uses the activity manger to listen for the active processes
 * Look for a change in the process importance and by that
 * knows that the given app was moved to the background by the back / home button
 *
 * Created by galbracha on 12/18/14.
 */
public class TasksObserver {

    public enum AppState {FOREGROUND, BACKGROUND};

    // Since facebook is an exception we don't want to listen for this task
    // Instead we do it by listen to the actual notification called 'chat head is active'
    public static final String COM_FACEBOOK = "com.facebook";
    public static final String COM_FACEBOOK_ORCA = COM_FACEBOOK + ".orca";
    public static final String FACEBOOK_BLUE_SERVICE = COM_FACEBOOK + ".fbservice.service.DefaultBlueService";
    public static final String FACEBOOK_CHAT_HEAD = COM_FACEBOOK_ORCA + ".chatheads.service.ChatHeadService";

    private final String TAG = this.getClass().getSimpleName();

    private static TasksObserver instance;
    private ActivityManager activityManager;
    private ScheduledFuture<?> scheduledFuture;

    static int DELAY_BETWEEN_EACH_CHECK = 1;

    private OnTaskMoveToBackgroundListener listener;
    /* The platform that is currently being listen. */

    public static TasksObserver getInstance(Context context) {
        if (instance == null) {
            instance = new TasksObserver(context);

        }
        return instance;
    }

    private TasksObserver(Context context) {
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    private boolean isInForeground(RunningAppProcessInfo process) {
        return process != null && (process.importance == IMPORTANCE_FOREGROUND
                || process.importance == IMPORTANCE_VISIBLE);
    }

    private boolean isInBackground(RunningAppProcessInfo process) {
        return process == null ||
                (process.importance != IMPORTANCE_FOREGROUND &&
                        process.importance != IMPORTANCE_VISIBLE);
    }

    public void startListen(final Context context, final String packageName, final AppState state,
                            final OnTaskMoveToBackgroundListener listener) {
        cancel();

        if (packageName == null)
            return;
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        Log.d(TAG, "Start to listen for process: " + packageName + ". detecting: " + state);
        this.listener = listener;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable r = new Runnable() {
            public void run() {
                if (packageName.equals(COM_FACEBOOK_ORCA)) {
                    boolean isChatHeadServiceRunning = isFacebookMessengerServiceRunning(FACEBOOK_CHAT_HEAD);
                    boolean isBlueServiceRunning = isFacebookMessengerServiceRunning(FACEBOOK_BLUE_SERVICE);
                    if (state == AppState.BACKGROUND && (!isChatHeadServiceRunning ||
                            (isChatHeadServiceRunning && !isBlueServiceRunning && !PopupSocialWindow.forcingMessengerHide))) {
                        onAppChangedState(packageName, state);
                    }
                    return;
                }
                final RunningAppProcessInfo process = getProcessForPackage(packageName);
                if ((state == AppState.BACKGROUND && isInBackground(process)) ||
                        (state == AppState.FOREGROUND && isInForeground(process))) {
                    onAppChangedState(packageName, state);
                }
            }
        };

        scheduledFuture = scheduler.scheduleAtFixedRate(r, 0, DELAY_BETWEEN_EACH_CHECK, TimeUnit.SECONDS);
    }

    public void cancel() {
        if (scheduledFuture!= null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }
    }

    private void onAppChangedState(String packageName, AppState state) {
        Log.d(TAG, "Discover process not in " + state);
        scheduledFuture.cancel(false);
        listener.OnAppChangedState(packageName, state);
    }

    private RunningAppProcessInfo getProcessForPackage(String packageName) {
        List<RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        if (processes.isEmpty())
            return null;

        for (RunningAppProcessInfo appProcess : processes) {
            if (packageName.equals(appProcess.processName)) {
                return appProcess;
            }
        }
        return null;
    }

    public boolean isFacebookMessengerServiceRunning(String serviceName) {
        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (services.isEmpty())
            return false;

        for (ActivityManager.RunningServiceInfo service : services) {
            if (service.service.getClassName().equals(serviceName))
                return true;
        }
        return false;
    }

    public interface OnTaskMoveToBackgroundListener {

        /**
         * Called when the given task moved to background
         * This is how we identify Home & Back buttons press
         */
        public void OnAppChangedState(String packageName, AppState state);

    }
}
