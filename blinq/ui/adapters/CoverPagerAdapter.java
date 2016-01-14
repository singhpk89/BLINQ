package com.blinq.ui.adapters;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.blinq.R;
import com.blinq.analytics.Analytics;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.models.Platform;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.authentication.impl.AuthUtils.AuthAction;
import com.blinq.ui.activities.webpage.WebPageActivity;
import com.blinq.ui.recyclers.RecycleCoverHolder;
import com.blinq.utils.Constants;
import com.blinq.utils.ExternalAppsUtils;
import com.blinq.utils.StringUtils;

/**
 * Manages the process of displaying different platforms cover page and status.
 *
 * @author Johan Hansson
 */
public class CoverPagerAdapter extends PagerAdapter {

    protected static final float COVER_PAGE_SCROLL_HEIGHT = 100;

    private final String TAG = CoverPagerAdapter.class.getSimpleName();

    private LayoutInflater inflater;

    private List<List<SocialWindowPost>> platformCoverPageList;

    private Analytics analyticsManager;

    private boolean showStatus = true;

    private FragmentActivity activity;

    private int feedId;

    private AuthAction authAction;

    private boolean firstItem = true;

    public void setShowStatus(boolean showStatus) {
        this.showStatus = showStatus;
    }

    /**
     * @param inflater       layout inflater for instant message main activity.
     * @param platformCovers array of list of SocialWindowPost object that contains cover
     *                       page and status for different platforms.
     */
    public CoverPagerAdapter(LayoutInflater inflater,
                             List<List<SocialWindowPost>> platformCovers,
                             FragmentActivity activity, int feedId, AuthAction authAction) {
        this(inflater, platformCovers);
        this.activity = activity;
        this.feedId = feedId;
        this.authAction = authAction;

    }

    public CoverPagerAdapter(LayoutInflater inflater,
                             List<List<SocialWindowPost>> platformCovers) {
        this.inflater = inflater;
        this.platformCoverPageList = platformCovers;
        analyticsManager = new BlinqAnalytics(inflater.getContext());

    }

    @Override
    public void destroyItem(final ViewGroup container, final int position,
                            final Object object) {


        // PlatformCoverListAdapter.ViewHolder holder = (PlatformCoverListAdapter.ViewHolder) object;
        // Toast.makeText(container.getContext(),"Remove View "+holder.toString(),Toast.LENGTH_SHORT).show();
        ListView listView = (ListView) object;
        try {
            View listItem = listView.getChildAt(0);

            if (listItem != null) {

                PlatformCoverListAdapter.ViewHolder viewHolder = (PlatformCoverListAdapter.ViewHolder) listItem.getTag();

                if (viewHolder != null) {
                    if (viewHolder.coverImage != null) {
                        viewHolder.coverImage.setImageDrawable(null);
                        viewHolder.coverImage.setImageBitmap(null);

                    }
                }
            }
        } catch (Exception ex) {

        }

        ((ViewPager) container).removeView((ListView) object);

    }

    @Override
    public int getCount() {
        return platformCoverPageList.size();
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {

        if (firstItem) {
            firstItem = false;
            ((ViewPager) container).setOnPageChangeListener(pageChangedListner);
        }
        View singlePageView = inflater.inflate(R.layout.cover_list, null);
        final ListView coverList = (ListView) singlePageView
                .findViewById(R.id.cover_list);

        coverList.setRecyclerListener(new RecycleCoverHolder());

        coverList.setSmoothScrollbarEnabled(true);
        List<SocialWindowPost> currentPlatformCoverPage = platformCoverPageList
                .get(position);


        final ListAdapter currentPlatformCoverPageAdapter = new PlatformCoverListAdapter(
                inflater, currentPlatformCoverPage, showStatus, activity,
                container.getContext(), feedId, authAction);

        coverList.setAdapter(currentPlatformCoverPageAdapter);
        ((ViewPager) container).addView(singlePageView, 0);

        coverList.setOnTouchListener(new OnTouchListener() {

            float previousY;
            float diffY;
            int currentIndex;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        previousY = event.getY();
                        currentIndex = coverList.getFirstVisiblePosition();

                        break;
                    case MotionEvent.ACTION_UP: {

                        diffY = event.getY() - previousY;

                        if (Math.abs(diffY) < 4) {

                            onSocialWindowClick(container, coverList,
                                    currentPlatformCoverPageAdapter);
                            return v.onTouchEvent(event);
                        }
                        if (Math.abs(diffY) > COVER_PAGE_SCROLL_HEIGHT) {

                            if (diffY > 0) {
                                int firstItemIndex = 0;

                                coverList.smoothScrollToPosition(Math.max(
                                        currentIndex - 1, firstItemIndex));

                                sendSocialWindowScrollAnalytics(AnalyticsConstants.SCROLLING_UP_VALUE);

                            } else {

                                int lastItemIndex = coverList.getCount() - 1;
                                coverList.smoothScrollToPosition(Math.min(
                                        lastItemIndex,
                                        coverList.getFirstVisiblePosition() + 1));

                                sendSocialWindowScrollAnalytics(AnalyticsConstants.SCROLLING_DOWN_VALUE);

                            }
                        } else {
                            coverList.smoothScrollToPosition(currentIndex);
                        }
                        return true;
                    }
                }
                return v.onTouchEvent(event);

            }

        });

        return singlePageView;
    }

    private OnPageChangeListener pageChangedListner = new OnPageChangeListener() {

        int lastPage = 0;

        @Override
        public void onPageSelected(int currentPage) {

            if (lastPage > currentPage) {
                // User Move to left

                sendSocialWindowScrollAnalytics(AnalyticsConstants.SCROLLING_LEFT_VALUE);

            } else if (lastPage < currentPage) {
                // User Move to right
                sendSocialWindowScrollAnalytics(AnalyticsConstants.SCROLLING_RIGHT_VALUE);

            }

            lastPage = currentPage;

        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    private void sendSocialWindowScrollAnalytics(String direction) {

        analyticsManager.sendEvent(
                AnalyticsConstants.SOCIAL_WINDOW_SCROLL_EVENT,
                AnalyticsConstants.SOCIAL_WINDOW_DIRECTION_PROPERTY, direction,
                false, AnalyticsConstants.ACTION_CATEGORY);
    }

    private void onSocialWindowClick(ViewGroup container, ListView coverList,
                                     ListAdapter currentPlatformCoverPageAdapter) {

        SocialWindowPost currentSocialWindowPost = (SocialWindowPost) currentPlatformCoverPageAdapter
                .getItem(coverList.getFirstVisiblePosition());
        if (PlatformCoverListAdapter.isInitialPost(currentSocialWindowPost))
            return;

        Context context = container.getContext();
        Platform socialWindowPlatform = currentSocialWindowPost
                .getCoverPageStatusPlatform();
        String socialWindowId = currentSocialWindowPost.getId();

        if (ExternalAppsUtils.isAppInstalledFor(socialWindowPlatform, context)
                && !StringUtils.isBlank(socialWindowId)) {
            context.startActivity(ExternalAppsUtils.getPlatformPostIntent(
                    currentSocialWindowPost, context));

        } else {
            Intent webPageIntent = new Intent(context.getApplicationContext(),
                    WebPageActivity.class);
            webPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            webPageIntent.putExtra(Constants.WEB_PAGE_LINK,
                    currentSocialWindowPost.getLink());
            if (currentSocialWindowPost.HasLink()) {
                context.startActivity(webPageIntent);
            }
        }

        sendSocialWindowClickAnalytics(socialWindowPlatform);
    }

    private void sendSocialWindowClickAnalytics(Platform platform) {
        analyticsManager.sendEvent(
                AnalyticsConstants.SOCIAL_WINDOW_CLICKED_EVENT,
                AnalyticsConstants.TYPE_PROPERTY,
                platform.getName(), false,
                AnalyticsConstants.ACTION_CATEGORY);
    }

    @Override
    public boolean isViewFromObject(final View view, final Object object) {
        return view == ((ListView) object);
    }

}
