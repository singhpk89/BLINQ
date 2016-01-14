package com.blinq.ui.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.AuthUtils.AuthAction;
import com.blinq.authentication.impl.AuthUtils.RequestStatus;
import com.blinq.utils.AppUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to populate the list of connected/disconnected platforms on the feed's drawer.
 *
 * @author Johan Hansson
 */
public class DrawerPlatformAdapter extends HeadboxBaseAdapter implements AuthAction {

    private static final String TAG = DrawerPlatformAdapter.class.getSimpleName();
    private Context context;
    private List<Platform> platformList;
    private Activity activity;
    private LayoutInflater layoutInflater;

    public DrawerPlatformAdapter(Activity activity) {

        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.layoutInflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        platformList = new ArrayList<Platform>();
        platformList.add(Platform.FACEBOOK);
        //platformList.add(Platform.HANGOUTS);
        platformList.add(Platform.TWITTER);
        platformList.add(Platform.INSTAGRAM);
        //platformList.add(Platform.WHATSAPP);
        //platformList.add(Platform.EMAIL);
        //platformList.add(Platform.SKYPE);
    }

    /**
     * Initialize Platforms Authenticators
     */

    private Integer[] enabledIconsIds = {R.drawable.drawer_fb_circle_on,
            //R.drawable.drawer_hangouts_circle_on,
            R.drawable.drawer_twitter_circle_on,
            //R.drawable.drawer_whatsapp_on,
            //R.drawable.drawer_email_circle_on,
            R.drawable.drawer_email_circle_on};

    public int getCount() {
        return platformList.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {

            convertView = layoutInflater.inflate(R.layout.drawer_platform_row, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.platformName = (TextView) convertView
                    .findViewById(R.id.platform_name);
            viewHolder.platformIconRight = (ImageView) convertView
                    .findViewById(R.id.platform_right_icon);
            viewHolder.platformIconLeft = (ImageView) convertView
                    .findViewById(R.id.platform_left_icon);
            viewHolder.platformOffElipse = (RelativeLayout) convertView.findViewById(R.id.platform_on_off);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        customizePlatformView(position, viewHolder);

        return convertView;
    }

    /**
     * Set a proper icon and on click event for each platform.
     *
     * @param position   position of given platform with the list.
     * @param viewHolder - current list item.
     */
    private void customizePlatformView(final int position, ViewHolder viewHolder) {
        final Platform platform = platformList.get(position);

        final boolean isConnected = AuthUtils.getConnectivityStatus(getActivity(), platform);

        String name = platformList.get(position).getName();
        viewHolder.platformName.setText(name);

        if (isConnected) {
            configurePlatformConnectedAppearance(viewHolder, position);
        } else {
            configurePlatformDisconnectedAppearance(viewHolder, position);
        }

        OnClickListener listener =
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (!isConnected) {
                            AuthUtils.connect(activity, DrawerPlatformAdapter.this, platformList.get(position), AnalyticsConstants.LOGIN_FROM_DRAWER);
                        }
                        refresh();
                    }
                };
        viewHolder.platformIconLeft.setOnClickListener(listener);
        viewHolder.platformOffElipse.setOnClickListener(listener);
    }

    private void configurePlatformDisconnectedAppearance(ViewHolder viewHolder, int position) {

        viewHolder.platformIconRight.setVisibility(View.INVISIBLE);
        viewHolder.platformOffElipse.setBackgroundResource(R.drawable.drawer_platfrom_off_elipse);
        viewHolder.platformIconLeft.setVisibility(View.VISIBLE);
    }

    private void configurePlatformConnectedAppearance(ViewHolder viewHolder, int position) {

        viewHolder.platformIconLeft.setVisibility(View.INVISIBLE);
        viewHolder.platformIconRight.setVisibility(View.VISIBLE);
        viewHolder.platformOffElipse.setBackgroundResource(android.R.color.transparent);
        viewHolder.platformIconRight.setImageResource(enabledIconsIds[position]);

    }

    /**
     * Refresh the platforms adapter
     */
    public void refresh() {
        notifyDataSetChanged();
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    /**
     * Hold a reference to view items, used to improve the performance of
     * displaying data.
     */
    static class ViewHolder {
        private TextView platformName;
        private ImageView platformIconLeft;
        private RelativeLayout platformOffElipse;
        private ImageView platformIconRight;
    }

    @Override
    public void onLoginCompleted(Platform platform) {

        if (!AppUtils.isActivityActive(activity))
            return;
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                refresh();

            }
        });
    }

    @Override
    public void onUserProfileLoaded(Platform platform, Contact profile, boolean success) {
        //No action needed
    }

    @Override
    public void onContactsUpdated(List<Contact> contacts) {
    }

    @Override
    public void onInboxUpdated(RequestStatus status) {
    }
}