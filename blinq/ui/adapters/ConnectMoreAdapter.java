package com.blinq.ui.adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.animation.SetGoneWhenFinishedAnimationListener;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.ui.animations.OnSwipeTouchListener;
import com.blinq.utils.AppUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Responsible to populate the list connected platforms on Connect More view.
 */
public class ConnectMoreAdapter extends BaseAdapter implements AuthUtils.AuthAction {

    private Activity activity;
    private List<Platform> platforms;
    private int connectedPlatformsCount = 0;
    private OnPlatformConnectedListener action;
    private AnalyticsSender analyticsSender;

    private final int[] icons = {
            R.drawable.login_insta,
            R.drawable.login_tw,
    };

    private final EnumMap<Platform, WeakReference<ViewHolder>> viewHoldersToAnimate =
            new EnumMap<Platform, WeakReference<ViewHolder>>(Platform.class);

    public ConnectMoreAdapter(Activity activity, OnPlatformConnectedListener action) {
        super();
        this.activity = activity;
        this.action = action;
        platforms = new ArrayList<Platform>(2);
        platforms.add(Platform.INSTAGRAM);
        platforms.add(Platform.TWITTER);
        analyticsSender = new AnalyticsSender(activity);
    }

    @Override
    public int getCount() {
        return platforms.size();
    }

    @Override
    public Object getItem(int i) {
        return platforms.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = View.inflate(activity, R.layout.connect_more_platform_row, null);
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        bindView(viewHolder, position);

        return view;
    }

    /**
     * Sets icon recourse and on click event for each platform.
     *
     * @param position position of given platform with the list.
     */
    private void bindView(final ViewHolder viewHolder, int position) {
        final Platform platform = platforms.get(position);

        if (viewHolder.platformIcon.getAnimation() != null) {
            viewHolder.platformIcon.getAnimation().cancel();
            viewHolder.platformIcon.setAnimation(null);
        }

        if (viewHolder.checkIcon.getAnimation() != null) {
            viewHolder.checkIcon.getAnimation().cancel();
            viewHolder.checkIcon.setAnimation(null);
        }

        viewHolder.platformName.setText(platform.getName());
        viewHolder.platformIcon.setImageResource(icons[position]);

        final boolean isConnected = AuthUtils.getConnectivityStatus(activity, platform);
        if (isConnected) {
            viewHolder.platformIcon.setVisibility(View.GONE);
            viewHolder.checkIcon.setVisibility(View.VISIBLE);
        } else {
            viewHolder.platformIcon.setVisibility(View.VISIBLE);
            viewHolder.checkIcon.setVisibility(View.GONE);
        }

        viewHolder.platformIcon.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        connectPlatform(platform, isConnected, viewHolder);
                    }
                }
        );

        viewHolder.platformIcon.setOnTouchListener(new OnSwipeTouchListener() {
            public boolean onSwipeTop() {
                connectPlatform(platform, isConnected, viewHolder);
                return true;
            }

            public boolean onSwipeRight() {
                connectPlatform(platform, isConnected, viewHolder);
                return true;
            }

            public boolean onSwipeLeft() {
                connectPlatform(platform, isConnected, viewHolder);
                return true;
            }

            public boolean onSwipeBottom() {
                connectPlatform(platform, isConnected, viewHolder);
                return true;
            }

        });
    }

    private void connectPlatform(Platform platform, boolean isConnected, ViewHolder viewHolder) {
        if (platform == Platform.LINKEDIN) {
            onLinkedInItemClicked();
        } else if (!isConnected) {
            viewHoldersToAnimate.put(platform, new WeakReference<ViewHolder>(viewHolder));
            AuthUtils.connect(activity, ConnectMoreAdapter.this, platform,
                    AnalyticsConstants.LOGIN_FROM_LOGIN_SCREEN);
        }
    }

    private void onLinkedInItemClicked() {

        analyticsSender.sendLinkedInButtonClickedEvent();

        Toast.makeText(activity, activity.getString(R.string.linkedin_soon), Toast.LENGTH_LONG).show();
    }

    /**
     * Hold a reference to view items.
     */
    private static class ViewHolder {
        private final ImageView platformIcon;
        private final ImageView checkIcon;
        private final TextView platformName;

        public ViewHolder(View view) {
            platformName = (TextView) view.findViewById(R.id.connect_more_platform_name);
            platformIcon = (ImageView) view.findViewById(R.id.connect_more_platform_icon);
            checkIcon = (ImageView) view.findViewById(R.id.connect_more_check_icon);
        }
    }

    private void animatePlatformIconIfNecessary(Platform platform) {
        final boolean isConnected = AuthUtils.getConnectivityStatus(activity, platform);
        if (!isConnected) return;

        final WeakReference<ViewHolder> viewHolderWeakReference = viewHoldersToAnimate.get(platform);
        if (viewHolderWeakReference == null) return;

        final ViewHolder viewHolder = viewHolderWeakReference.get();
        if (viewHolder == null) return;

        if (viewHolder.checkIcon.getVisibility() == View.VISIBLE) return;

        final Animation hidePlatformIconAnimaiton =
                AnimationUtils.loadAnimation(activity, R.anim.connect_more_hide_platform_icon);
        hidePlatformIconAnimaiton.setAnimationListener(
                new SetGoneWhenFinishedAnimationListener(viewHolder.platformIcon));
        final Animation showCheckAnimation =
                AnimationUtils.loadAnimation(activity, R.anim.connect_more_show_check);
        viewHolder.checkIcon.setVisibility(View.VISIBLE);

        viewHolder.platformIcon.startAnimation(hidePlatformIconAnimaiton);
        viewHolder.checkIcon.startAnimation(showCheckAnimation);
    }

    @Override
    public void onLoginCompleted(final Platform platform) {
        analyticsSender.sendSuccessConnectionEvent(platform);
        connectedPlatformsCount++;

        if (!AppUtils.isActivityActive(activity))
            return;

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                animatePlatformIconIfNecessary(platform);
                //Show the next button if one platform or more is connected
                if (connectedPlatformsCount >= 1) {
                    action.onAllPlatformsConnected();
                }
            }
        });
    }

    @Override
    public void onUserProfileLoaded(Platform platform, Contact profile, boolean success) {
        animatePlatformIconIfNecessary(platform);
    }

    @Override
    public void onContactsUpdated(List<Contact> contacts) {

    }

    @Override
    public void onInboxUpdated(AuthUtils.RequestStatus status) {

    }


    /**
     * An inner interface which the activity responsible to call this adapter must implement in order to handle the connect more platforms changes.
     *
     * @author Johan Hansson
     */
    public interface OnPlatformConnectedListener {
        /**
         * Fired if platforms are all connected.
         */
        public void onAllPlatformsConnected();
    }
}
