package com.blinq.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.blinq.authentication.impl.provider.SocialWindowProvider;
import com.blinq.models.Platform;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.utils.AppUtils;
import com.nu.art.software.core.utils.SmarterHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Responsible on starting the dot it needed and fetching the feed from the SocialWindowProvider.
 * Keeps records of the muted contacts (contacts for which the dot appeared but removed without opening
 * the social window).
 *
 * Created by Roi on 12/15/14.
 */
public class DotManager {

    private final String TAG = this.getClass().getSimpleName();
    private static final int FACEBOOK_DOT_DELAY_MILLS = 50;

    private Set<Integer> mutedContacts = new HashSet<Integer>();

    Platform platform;
    int feedId;
    Context context;

    private static DotManager instance;
    private String packageName;

    public static DotManager getInstance() {
        if (instance == null) {
            instance = new DotManager();
        }
        return instance;
    }

    public boolean isMuted(int feedId) {
        return mutedContacts.contains(feedId);
    }

    public void muteFeed(Context context, int feedId, WindowManager.LayoutParams params) {
        mutedContacts.add(feedId);
        showMuteMessage(context, params);
    }

    private void showMuteMessage(Context context, WindowManager.LayoutParams params) {
        Toast toast = Toast.makeText(context,
                "Muted until something new happens",
                Toast.LENGTH_SHORT);

        final View toastView = toast.getView();
        final WindowManager windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        Point p = new Point();

        windowManager.getDefaultDisplay().getSize(p);
        toastView.measure(p.x, p.y);

        params.x = (p.x - toastView.getMeasuredWidth()) / 2;
        params.y -= 150;

        windowManager.addView(toastView, params);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                windowManager.removeView(toastView);
            }
        }, 3000);
    }

    public void unmuteFeed(int feedId) {
        mutedContacts.remove(feedId);
    }

    public boolean startNotificationDotServiceIfShould(Context context, int feedId, Platform platform, String packageName) {
        this.feedId = feedId;
        this.platform = platform;
        this.context = context;
        this.packageName = packageName;
        if (!shouldDotServiceStart()) {
            return false;
        }
        if (!isMuted(feedId)) {
            startNotificationDotServiceByPlatform(false);
            fetchSocialPosts(feedId, null);
            return true;
        }
        fetchSocialPosts(feedId, socialWindowPostsListener);
        return true;
    }

    private boolean shouldDotServiceStart() {
        if (!isScreenOn() || !AppUtils.isAppOnForeground(context, packageName)) {
            return false;
        }
        return true;
    }

    private boolean isScreenOn() {
        return ((PowerManager) context.getSystemService(context.POWER_SERVICE)).isScreenOn();
    }

    private void startNotificationDotServiceByPlatform(final boolean startServiceWithAnimation) {
        if (platform == Platform.FACEBOOK) {
            SmarterHandler handler = new SmarterHandler(TAG);
            handler.removeAndPost(FACEBOOK_DOT_DELAY_MILLS, new Runnable() {
                @Override
                public void run() {
                    startNotificationDotService(startServiceWithAnimation);
                }
            });
        } else {
            startNotificationDotService(startServiceWithAnimation);
        }
    }

    public void startNotificationDotService(boolean startServiceWithAnimation) {
        Intent intent = new Intent(context, FloatingDotService.class);
        intent.putExtra(FloatingDotService.FEED_ID_EXTRA_TAG, feedId);
        intent.putExtra(FloatingDotService.PLATFORM_EXTRA_TAG, platform.getId());
        intent.putExtra(FloatingDotService.PACKAGE_NAME_EXTRA_TAG, packageName);
        if (startServiceWithAnimation) {
            intent.putExtra(FloatingDotService.START_WITH_ANIMATION, true);
        }
        context.startService(intent);
    }

    private void fetchSocialPosts(long feedId, SocialWindowProvider.SocialWindowPostsListener listener) {
        SocialWindowProvider.getNewInstance(context, feedId).setListener(listener).fetchSocialPosts(
                listener == null ? true : false, false);
    }

    SocialWindowProvider.SocialWindowPostsListener socialWindowPostsListener = new SocialWindowProvider.SocialWindowPostsListener() {
        @Override
        public void onComplete(List<SocialWindowPost> response, boolean isNew, boolean meCardOnly) {
            if (!isNew) {
                Log.d(TAG, "No new posts available, not showing dot for muted contact");
                return;
            }
            Log.d(TAG, "New posts available, showing dot for muted contact");
            startNotificationDotServiceByPlatform(true);
        }

        @Override
        public void onException(Throwable throwable) {

        }

        @Override
        public void onFail(String reason) {

        }

        @Override
        public void onLoading() {

        }
    };
}
