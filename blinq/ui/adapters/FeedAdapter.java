package com.blinq.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blinq.R;
import com.blinq.ImageLoaderManager;
import com.blinq.PreferencesManager;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.models.FeedDesign;
import com.blinq.models.FeedModel;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.ui.fragments.FeedsFragmentList;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class FeedAdapter extends HeadboxBaseAdapter {

    private final String TAG = FeedAdapter.class.getSimpleName();

    private static final int MAX_NUMBER_OF_PLATFORMS = 4;

    private Context context;
    private List<Platform> platforms;
    private List<FeedModel> feeds;
    private FeedsFragmentList feedFragment;
    private ImageLoaderManager imageLoaderManager;
    private LayoutInflater layoutInflater;

    private final int[] platformsViewsIds = new int[]{R.id.frameLayoutForPlatform_0x0, R.id.frameLayoutForPlatform_0x1, R.id.frameLayoutForPlatform_1x0, R.id.frameLayoutForPlatform_1x1};
    private final int[] platformsIconsImageViewsIds = new int[]{R.id.imageViewForPlatform_0x0, R.id.imageViewForPlatform_0x1, R.id.imageViewForPlatform_1x0, R.id.imageViewForPlatform_1x1};

    private int resource;
    private boolean isAllFeed = false;

    private OnDataLoadedListener onDataLoadedListener;

    private PreferencesManager preferencesManager;
    private BlinqAnalytics analyticsManager;

    public FeedAdapter(Context context, int resource, List<FeedModel> feeds,
                       FeedsFragmentList feedFragment, List<Platform> platforms, OnDataLoadedListener onDataLoadedListener) {
        super();

        this.feeds = feeds;
        this.context = context;
        this.resource = resource;
        this.feedFragment = feedFragment;
        this.platforms = platforms;
        this.onDataLoadedListener = onDataLoadedListener;
        analyticsManager = new BlinqAnalytics(context.getApplicationContext());
        imageLoaderManager = new ImageLoaderManager(context);

        layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        preferencesManager = new PreferencesManager(context);
    }

    /**
     * Get new instance of image loader, used when changing application theme to
     * update image loader configuration (day/night) before displaying data.
     */
    public void refreshImageLoader() {

        imageLoaderManager = new ImageLoaderManager(context);

    }

    @Override
    public int getCount() {
        return feeds.size();
    }

    @Override
    public Object getItem(int position) {
        return feeds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        FeedModel feed = feeds.get(position);
        if (convertView == null) {

            convertView = layoutInflater.inflate(resource, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.feedBodyLayout = convertView.findViewById(R.id.relativeLayoutFeedBody);
            viewHolder.senderImageView = (ImageView) convertView
                    .findViewById(R.id.imageViewForSenderImage);
            viewHolder.senderNameTextView = (TextView) convertView
                    .findViewById(R.id.textViewForSenderName);
            viewHolder.messageSnippetTextView = (TextView) convertView
                    .findViewById(R.id.textViewForMessageContent);
            viewHolder.messageTimeTextView = (TextView) convertView
                    .findViewById(R.id.textViewForMessageTime);
            viewHolder.feedSeparatorTextView = (TextView) convertView
                    .findViewById(R.id.textViewForFeedSeparator);

            viewHolder.platformsIconsViews = new ViewHolder.PlatformViewHolder[MAX_NUMBER_OF_PLATFORMS];

            for (int index = 0; index < MAX_NUMBER_OF_PLATFORMS; index++) {

                viewHolder.platformsIconsViews[index] = new ViewHolder.PlatformViewHolder();
                viewHolder.platformsIconsViews[index].platformIconFrameLayout = (FrameLayout) convertView
                        .findViewById(platformsViewsIds[index]);
                viewHolder.platformsIconsViews[index].platformIconImageView = (ImageView) convertView
                        .findViewById(platformsIconsImageViewsIds[index]);
            }


            // Options layout
            viewHolder.deleteFeedOptionImageView = (ImageView) convertView.findViewById(R.id.imageViewForDeleteFeedOption);
            viewHolder.longPressOptions = (RelativeLayout) convertView.findViewById(R.id.relativeLayoutFeedOptions);

            convertView.setTag(viewHolder);

        } else {

            // When returned view is not null that means screen filled with new inflated layouts. That means list-view
            // done rendering screen views so apply first time animation on feeds list-view.
            if (preferencesManager.getProperty(PreferencesManager.SHOULD_APPLY_FIRST_TIME_FEED_ANIMATION, true)) {

                // Apply first time animation and update flag in preferences in order just to appear only once.
                onDataLoadedListener.onDataLoaded();

                preferencesManager.setProperty(PreferencesManager.SHOULD_APPLY_FIRST_TIME_FEED_ANIMATION, false);
            }

            viewHolder = (ViewHolder) convertView.getTag();
        }


        //set is read true.
        setFeedItemDesign(viewHolder, true);

        imageLoaderManager.loadContactAvatarImage(viewHolder.senderImageView,
                feed.getContact(), true);

        String messageSender = feed.getContact().toString();
        viewHolder.senderNameTextView.setText(messageSender);

        String messageBody = feed.getLastMessageBody();
        viewHolder.messageSnippetTextView.setText(messageBody);
        viewHolder.messageSnippetTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        viewHolder.messageTimeTextView.setText(feed.getLastMessageTimeString());

        MessageType lastCallType = feed.getLastCallType();

        int index = 0;
        Map<Platform, Integer> platformsCount;

        // If it's not the LIFEED only display the platform icon.
        if (!isAllFeed && platforms != null) {

            for (Platform platform : platforms) {

                viewHolder.platformsIconsViews[index].platformIconImageView
                        .setImageResource(getPlatformIcon(platform,
                                lastCallType));
                index++;
            }

        } else {

            ViewHolder.PlatformViewHolder currentPlatformView;

            // Show missed call icon if last message type is missed call.
            if (lastCallType == MessageType.MISSED && feed.getLastMessagePlatform() == Platform.CALL) {
                viewHolder.messageSnippetTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.feed_call_icon_missed, 0, 0, 0);
            }

            if (feed.isRead()) {

                // Display only last message platform icon.
                Platform platform;

                if (feed.getLastCallType() == MessageType.MISSED
                        && feed.getLastMessagePlatform() == Platform.CALL) {

                    // To display missed call icon.
                    platform = Platform.CALL;

                } else {

                    platform = feed.getLastMessagePlatform();
                }

                // Get read icon for current platform.
                currentPlatformView = viewHolder.platformsIconsViews[index];

                currentPlatformView.platformIconImageView
                        .setImageResource(getPlatformIcon(platform,
                                lastCallType));
                currentPlatformView.platformIconFrameLayout.setVisibility(View.VISIBLE);

                index++;

            } else {


                // All platforms in the feed.
                platformsCount = feed.getUnreadMessagesCount();

                if (platformsCount != null) {

                    for (Map.Entry<Platform, Integer> entry : platformsCount.entrySet()) {

                        Platform platform = entry.getKey();

                        currentPlatformView = viewHolder.platformsIconsViews[index];

                        currentPlatformView.platformIconFrameLayout.setVisibility(View.VISIBLE);

                        // Get unread icon for current platform.
                        currentPlatformView.platformIconImageView
                                .setImageResource(getUnreadPlatformIcon(platform,
                                        lastCallType));

                        index++;
                    }

                }

            }

            // Clear content of other images.
            while (index < MAX_NUMBER_OF_PLATFORMS) {

                viewHolder.platformsIconsViews[index].platformIconFrameLayout.setVisibility(View.GONE);
                index++;
            }


            setFeedOptionsListeners(viewHolder, position);
        }

        return convertView;
    }


    /**
     * Return the unread icon for given platform.
     *
     * @param platform     platform to get icon for.
     * @param lastCallType last call type in the feed.
     * @return id of unread platform icon.
     */

    private int getUnreadPlatformIcon(Platform platform, MessageType lastCallType) {

        switch (platform) {

            case CALL:

                if (lastCallType == MessageType.MISSED) {
                    return R.drawable.feed_call_missed_unread;
                } else {
                    return R.drawable.feed_call_incoming_unread;
                }

            case FACEBOOK:
                return R.drawable.feed_fb_unread;
            case HANGOUTS:
                return R.drawable.feed_hangouts_unread;
            case SMS:
                return R.drawable.feed_sms_unread;
            case MMS:
                return R.drawable.feed_sms_unread;
            case WHATSAPP:
                return R.drawable.feed_whatsapp_unread;
            case EMAIL:
                return R.drawable.feed_email_incoming_unread;
            case SKYPE:
                return R.drawable.feed_incoming_skype_unread;
            default:
                return 0;
        }
    }

    /**
     * Work as holder for feed view in order to decrease the time of creating
     * list-view items.
     *
     * @author Johan Hansson.
     */
    public static class ViewHolder {

        View feedBodyLayout;
        public ImageView senderImageView;
        public TextView senderNameTextView;
        public TextView messageSnippetTextView;
        public TextView messageTimeTextView;
        public TextView feedSeparatorTextView;
        public PlatformViewHolder[] platformsIconsViews;
        public ImageView deleteFeedOptionImageView;
        public RelativeLayout longPressOptions;

        public static class PlatformViewHolder {

            FrameLayout platformIconFrameLayout;
            public ImageView platformIconImageView;
        }
    }

    /**
     * Set the design of feed layout depends on the application mode.
     *
     * @param viewHolder view holder for feed item components.
     * @param isRead     feed status (read/unread). true if feed is read, false if feed
     *                   is unread.
     */
    private void setFeedItemDesign(ViewHolder viewHolder, Boolean isRead) {

        FeedDesign modeDesign = FeedDesign.getInstance();

        // Feed separator.
        viewHolder.feedSeparatorTextView.setBackgroundColor(modeDesign
                .getFeedSeparatorColor());

        // Message time ago.
        viewHolder.messageTimeTextView.setTextColor(context.getResources().getColorStateList(modeDesign.getMessageTimeAgoSelector()));

        // To clear selection color when search view opened.
        viewHolder.feedBodyLayout.setPressed(false);

        //Feed long press background color
        viewHolder.longPressOptions.setBackgroundColor(modeDesign.getFeedLongPressBackground());

        if (isRead) {

            // read
            viewHolder.feedBodyLayout.setBackgroundResource(modeDesign
                    .getFeedBodySelector());

            // Contact name.
            viewHolder.senderNameTextView.setTypeface(modeDesign
                    .getSenderTypefaceRead());
            viewHolder.senderNameTextView.setTextColor(context.getResources()
                    .getColorStateList(modeDesign.getContactNameTextSelectorRead()));

            // Message
            viewHolder.messageSnippetTextView.setTypeface(modeDesign
                    .getSenderTypefaceRead());
            viewHolder.messageSnippetTextView.setTextColor(context.getResources()
                    .getColorStateList(modeDesign.getMessageTextSelectorRead()));

        } else {

            // unread
            viewHolder.feedBodyLayout.setBackgroundResource(modeDesign
                    .getFeedBodySelectorUnread());

            // Contact name.
            viewHolder.senderNameTextView.setTypeface(modeDesign
                    .getSenderTypefaceUnread());
            viewHolder.senderNameTextView.setTextColor(context.getResources()
                    .getColorStateList(modeDesign.getContactNameTextSelectorUnread()));

            // Message
            viewHolder.messageSnippetTextView.setTypeface(modeDesign
                    .getSenderTypefaceUnread());
            viewHolder.messageSnippetTextView.setTextColor(context.getResources()
                    .getColorStateList(modeDesign.getMessageTextSelectorUnread()));

        }
    }

    /**
     * Set listeners on long click feed item options.
     *
     * @param viewHolder view holder for feed item components.
     */
    private void setFeedOptionsListeners(ViewHolder viewHolder, final int position) {

        viewHolder.deleteFeedOptionImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // Delete feed.
                OnFeedDeleted onFeedDeleted = (OnFeedDeleted) feedFragment;
                onFeedDeleted.onFeedDeleted(position);
                analyticsManager.sendEvent(
                        AnalyticsConstants.FEED_DELETE_CONVERSATION_EVENT, true,
                        AnalyticsConstants.ACTION_CATEGORY);

            }
        });

    }


    /**
     * Updates last message date for given feed.
     *
     * @param position        The position of feed to update.
     * @param lastMessageDate The updated last message date.
     */
    public void updateLastMessageDate(int position, Date lastMessageDate) {

        feeds.get(position).setLastMessageTime(lastMessageDate);
    }

    /**
     * Updates given feed as read.
     *
     * @param position The position of feed to update.
     */
    public void markFeedAsRead(int position) {
        feeds.get(position).setRead(true);
    }

    /**
     * @param isAllFeed true if it's the All feed fragment, false if not.
     */
    public void setAllFeed(boolean isAllFeed) {
        this.isAllFeed = isAllFeed;
    }


    /**
     * Interface to send feed deletion action to feeds fragment.
     */
    public interface OnFeedDeleted {

        /**
         * @param position feed position in the list-view.
         */
        public void onFeedDeleted(int position);
    }


    /**
     * Interface work as call back to notify caller when the list-view done inflating items.
     */
    public interface OnDataLoadedListener {

        /**
         * Called when list-view done inflating items.
         */
        public void onDataLoaded();
    }
}