package com.blinq.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.Analytics;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.models.FeedModel;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.provider.CallsManager;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.ui.adapters.FeedAdapter;
import com.blinq.ui.animations.SlideAnimation;
import com.blinq.ui.recyclers.RecycleFeedHolder;
import com.blinq.ui.views.CustomDialogBuilder;
import com.blinq.ui.views.SearchListView;
import com.blinq.utils.Constants;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.EmailUtils;
import com.blinq.utils.FileUtils;
import com.blinq.utils.Log;
import com.blinq.utils.UIUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Johan Hansson.
 */
public class FeedsFragmentList extends ListFragment implements OnClickListener,
        AdapterView.OnItemLongClickListener, FeedAdapter.OnFeedDeleted, FeedAdapter.OnDataLoadedListener {

    private static final String TAG = FeedsFragmentList.class.getSimpleName();

    private static final int FEED_IS_NOT_EXIST = -1;

    private final int FEED_OPTIONS_SLIDE_DURATION = 300;
    private final int FEED_OPTIONS_SLIDE_DURATION_FAST = 0;

    // Number of millis for feed to still slided before closed back.
    private final int FIRST_TIME_FEED_ANIMATION_DURATION = 1000;

    // Number of millis for feed to start after.
    private final int FIRST_TIME_FEED_ANIMATION_DELAY = 300;

    // Feed item index to apply first time animation on.
    private final int FEED_ITEM_INDEX_TO_ANIMATE_IN_FIRST_TIME_ANIMATION = 1;

    private FeedAdapter feedsAdapter;

    private OnFeedSelectedListener feedListener;

    private Provider provider;

    private PreferencesManager preferencesManager;

    private List<FeedModel> feeds;

    private boolean isAllFeed;
    private boolean isModified = false;

    private Context context;

    private List<Platform> platforms;

    private int numberOfClicksOnBottomLeftCorner = 0;
    private Timer clicksTimer;
    private CustomDialogBuilder dialogBuilder;
    private AlertDialog sendDatabaseLogDialog;
    private AlertDialog copyingDialog;

    private boolean hasMoreFeedsToLoad = true;

    private LinearLayout searchLayout;

    private Analytics analyticsManager;

    private SearchListView searchListView;
    private FeedsLoader feedsLoader;

    private Activity activity;

    private final int NOTHING_SLIDED = -1;

    // Hold the index for currently slided feed.
    private int lastSlidedFeed = NOTHING_SLIDED;


    // Slide distance (in pixels) to show feed options
    private int feedOptionsSlideDistance;

    private int feedsFirstVisibleItem = 0;

    private boolean isSearchViewStarted = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // check if the FeedActivity implements OnFeedSelectedListener
        try {

            feedListener = (OnFeedSelectedListener) activity;

        } catch (ClassCastException e) {

            throw new ClassCastException(activity.toString()
                    + " must implement "
                    + OnFeedSelectedListener.class.getSimpleName());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        context = getActivity();
        analyticsManager = new BlinqAnalytics(context);
        View fragmentLayout = inflater.inflate(R.layout.feed_fragment,
                container, false);

        init(fragmentLayout);

        setFragmentAdapter();

        return fragmentLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (searchLayout == null) {
            return;
        }

        this.activity = getActivity();
        this.preferencesManager = new PreferencesManager(activity);

        searchListView = (SearchListView) getListView();
        searchListView.setSearchLayout(searchLayout);
        searchListView.setRecyclerListener(new RecycleFeedHolder());

        searchListView.setFragment(this);
        searchListView.initUiComponents();
        //searchListView.setOnItemLongClickListener(this);

        feedsLoader = new FeedsLoader(activity);

        setScrollListenerOnListView();

    }


    /**
     * Set scroll listener on list-view.
     */
    private void setScrollListenerOnListView() {

        // Set listener on the list scrolling in order to sliding feedback
        // button.
        AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {
            private int lastVisibleItem;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Update first visible item index when scroll done.
                if (scrollState == SCROLL_STATE_IDLE) {
                    feedsFirstVisibleItem = view.getFirstVisiblePosition();
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Close all slided feeds when scroll by at least one item,
                // used to remove redundant of slided feeds because adapter
                // use the same views with different data while scrolling.
                if (isFirstVisibleItemChanged(firstVisibleItem) && lastSlidedFeed != NOTHING_SLIDED) {

                    hideFeedOptions(lastSlidedFeed, FEED_OPTIONS_SLIDE_DURATION);

                    feedsFirstVisibleItem = firstVisibleItem;
                }

                this.lastVisibleItem = firstVisibleItem
                        + visibleItemCount;
                if (hasMoreFeedsToLoad
                        && ((lastVisibleItem
                        + (Constants.NUMBER_OF_FEEDS_TO_LOAD / 2) == totalItemCount) || (lastVisibleItem == totalItemCount))) {

                    if (!feedsLoader.isRunning) {
                        try {

                            feedsLoader = new FeedsLoader(activity);
                            feedsLoader.isRunning = true;
                            feedsLoader.execute(null, null, null);
                        } catch (IllegalStateException illegalStateException) {
                            Log.e(TAG,
                                    "IllegalStateException in the loading more feeds");
                        }
                    }
                }
            }
        };


        PauseOnScrollListener pauseOnScrollListener = new PauseOnScrollListener(
                ImageLoader.getInstance(), true, true,
                new AbsListView.OnScrollListener() {

                    private int lastVisibleItem;

                    @Override
                    public void onScrollStateChanged(AbsListView view,
                                                     int scrollState) {

                        // Update first visible item index when scroll done.
                        if (scrollState == SCROLL_STATE_IDLE) {
                            feedsFirstVisibleItem = view.getFirstVisiblePosition();
                        }
                    }

                    @Override
                    public void onScroll(AbsListView view,
                                         int firstVisibleItem, int visibleItemCount,
                                         int totalItemCount) {

                        // Close all slided feeds when scroll by at least one item,
                        // used to remove redundant of slided feeds because adapter
                        // use the same views with different data while scrolling.
                        if (isFirstVisibleItemChanged(firstVisibleItem) && lastSlidedFeed != NOTHING_SLIDED) {

                            hideFeedOptions(lastSlidedFeed, FEED_OPTIONS_SLIDE_DURATION);

                            feedsFirstVisibleItem = firstVisibleItem;
                        }

                        this.lastVisibleItem = firstVisibleItem
                                + visibleItemCount;
                        if (hasMoreFeedsToLoad
                                && ((lastVisibleItem
                                + (Constants.NUMBER_OF_FEEDS_TO_LOAD / 2) == totalItemCount) || (lastVisibleItem == totalItemCount))) {

                            if (!feedsLoader.isRunning) {
                                try {

                                    feedsLoader = new FeedsLoader(activity);
                                    feedsLoader.isRunning = true;
                                    feedsLoader.execute(null, null, null);
                                } catch (IllegalStateException illegalStateException) {
                                    Log.e(TAG,
                                            "IllegalStateException in the loading more feeds");
                                }
                            }
                        }
                    }
                }
        );

        searchListView.setOnScrollListener(onScrollListener);
    }

    /**
     * Check if list scrolled at least by one item.
     *
     * @param currentFirstVisibleItem the index of currently first visible item in the list.
     * @return true if list scrolled at least by one item, false otherwise.
     */
    private boolean isFirstVisibleItemChanged(int currentFirstVisibleItem) {

        return currentFirstVisibleItem != feedsFirstVisibleItem;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Pass the activity to search list view class.
        ((SearchListView) getListView()).setActivity(getActivity());

        if (isVisible()) {

            if (isModified) {

                getListView().setSelection(0);
                isModified = false;
            }
        }

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {

            if (!isAllFeed && platforms != null && platforms.get(0) != null) {

                sendFilterSwitchAnalytics(platforms.get(0));
            }
        }
    }

    private void sendFilterSwitchAnalytics(Platform platform) {

        if (analyticsManager == null)
            analyticsManager = new BlinqAnalytics(context);
        if (platform != null) {
            String platformName = platform.name();
            analyticsManager.sendEvent(AnalyticsConstants.FILTER_EVENT,
                    AnalyticsConstants.SWITCHED_TO_PROPERTY, platformName,
                    false, AnalyticsConstants.ACTION_CATEGORY);
        }
    }

    /**
     * Initiate required data for fragment.
     */
    private void init(View fragmentLayout) {

        View sendDatabaseLogView = fragmentLayout
                .findViewById(R.id.viewToSendDatabaseAndLog);
        sendDatabaseLogView.setOnClickListener(this);

        preferencesManager = new PreferencesManager(getActivity());

        provider = FeedProviderImpl.getInstance(getActivity());

        feedOptionsSlideDistance = UIUtils.getScreenWidth(getActivity()) - (int) context.getResources().getDimension(R.dimen.feed_options_slide_margin_right);

        clicksTimer = new Timer();

    }

    /**
     * Used to open marked feed when the search view override it's listener which result in not calling @OnItemClickListener of the list-view.
     */
    public void openSelectedFeedWhenSearchViewInitializationNotCompleted() {

        int firstVisiblePosition = searchListView.getFirstVisiblePosition();
        int lastVisiblePosition = searchListView.getLastVisiblePosition();

        for (int index = firstVisiblePosition; index < lastVisiblePosition; index++) {

            View itemView = UIUtils.getTheViewOfListItemAtSpecificPosition(index, searchListView);

            if (itemView != null) {

                View feedBodyView = itemView.findViewById(R.id.relativeLayoutFeedBody);

                if (feedBodyView.isPressed()) {

                    feedBodyView.setPressed(false);
                    refresh();
                    clickedOnFeedItem(index);

                    break;
                }
            }
        }
    }

    /**
     * Apply first time animation on feeds list-view. Slide first feed for 1 second then close it back.
     */
    private void applyFirstTimeAnimation() {

        // Timer task to close first time animated feed.
        final TimerTask closeFirstTimeFeedAnimationTimerTask = new TimerTask() {
            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        hideFeedOptions(FEED_ITEM_INDEX_TO_ANIMATE_IN_FIRST_TIME_ANIMATION, FEED_OPTIONS_SLIDE_DURATION);

                    }
                });
            }
        };

        // Timer task to slide feed in first time animation.
        TimerTask firstTimeFeedAnimationTimerTask = new TimerTask() {
            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        showFeedOptions(FEED_ITEM_INDEX_TO_ANIMATE_IN_FIRST_TIME_ANIMATION, FEED_OPTIONS_SLIDE_DURATION);

                        Timer closeFirstTimeFeedAnimationTimer = new Timer();
                        closeFirstTimeFeedAnimationTimer.schedule(closeFirstTimeFeedAnimationTimerTask, FIRST_TIME_FEED_ANIMATION_DURATION);
                    }
                });

            }
        };

        Timer firstTimeFeedAnimationTimer = new Timer();
        firstTimeFeedAnimationTimer.schedule(firstTimeFeedAnimationTimerTask, FIRST_TIME_FEED_ANIMATION_DELAY);

    }


    @Override
    public void onListItemClick(ListView l, View v, final int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (lastSlidedFeed != NOTHING_SLIDED) {

            // If it's not the slided feed, close slided then open com-log.
            if (lastSlidedFeed != position) {

                hideFeedOptions(lastSlidedFeed, FEED_OPTIONS_SLIDE_DURATION);
                clickedOnFeedItem(position - 1);
                return;

            }

            hideFeedOptions(lastSlidedFeed, FEED_OPTIONS_SLIDE_DURATION);

        } else {

            // Open feed only if it's not slided.
            clickedOnFeedItem(position - 1);
        }

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        if (lastSlidedFeed == NOTHING_SLIDED) {

            // Nothing slided, slide selected feed.
            showFeedOptions(position, FEED_OPTIONS_SLIDE_DURATION);
            return true;

        } else if (position != lastSlidedFeed) {

            // There is already one slided, hide it then open currently selected.
            hideFeedOptions(lastSlidedFeed, FEED_OPTIONS_SLIDE_DURATION);
            showFeedOptions(position, FEED_OPTIONS_SLIDE_DURATION);
            return true;

        } else {

            // Long click occur on slided feed.
            return false;
        }

    }

    /**
     * Show feed options by sliding the front view to right.
     *
     * @param position feed position to show options for.
     * @param duration show feed options animation duration.
     */
    private void showFeedOptions(int position, int duration) {

        // Don't show feed options if search view currently take the action.
        if (!isSearchViewStarted) {

            slideFeedItemFrontViewToRight(position, duration);

            // Register feed index as slided.
            lastSlidedFeed = position;
        }

    }


    /**
     * Slide the feed item front view to right to show feed options.
     *
     * @param position feed position in list-view to slide.
     * @param duration feed slide animation duration.
     */
    private void slideFeedItemFrontViewToRight(int position, int duration) {

        View itemView = UIUtils.getTheViewOfListItemAtSpecificPosition(position - 1, searchListView);

        if (itemView != null) {

            // Set options view as visible before starting the animation.
            View feedOptionsView = itemView.findViewById(R.id.relativeLayoutFeedOptions);
            feedOptionsView.setVisibility(View.VISIBLE);

            View feedBodyView = itemView.findViewById(R.id.relativeLayoutFeedBody);
            SlideAnimation.slideHorizontally(feedBodyView, 0, feedOptionsSlideDistance, duration);
        }

    }

    /**
     * Hide feed options & remove feed position from slided feeds hash-map.
     *
     * @param position feed position to hide options for.
     * @param duration hide feed options animation duration.
     */
    private void hideFeedOptions(int position, int duration) {

        slideFeedItemFrontViewToLeft(position, duration);

        lastSlidedFeed = NOTHING_SLIDED;
    }

    /**
     * Slide the feed item front view back (to left) to hide feed options.
     *
     * @param position feed position in list-view to slide.
     * @param duration feed slide animation duration.
     */
    private void slideFeedItemFrontViewToLeft(final int position, int duration) {

        final View itemView = UIUtils.getTheViewOfListItemAtSpecificPosition(position - 1, searchListView);

        if (itemView != null) {

            View feedBodyView = itemView.findViewById(R.id.relativeLayoutFeedBody);
            SlideAnimation.slideHorizontally(feedBodyView, feedOptionsSlideDistance, 0, duration);

            feedBodyView.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {

                    // Change visibility for feeds options to GONE when slide-back animation done.
                    View feedOptionsView = itemView.findViewById(R.id.relativeLayoutFeedOptions);
                    feedOptionsView.setVisibility(View.GONE);

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

            });

        }

    }

    /**
     * Apply set of actions on the selected feed.
     *
     * @param position selected feed index.
     */
    public void clickedOnFeedItem(int position) {

        final FeedModel selectedFeed = feeds.get(position);
        String feedId = Long.toString(selectedFeed.getFeedId());
        feedListener.onFeedSelected(Uri.parse(feedId), selectedFeed.getLastMessagePlatform());
    }


    /**
     * Call contact if it has phone number. called when the user apply long
     * click on the contact avatar.
     *
     * @param position contact feed index.
     */
    public void callContact(int position) {

        FeedModel feed = feeds.get(position);

        String phoneNumber = provider.getLastPhoneNumber(feed.getFeedId());

        if (phoneNumber != null) {

            CallsManager.makeACall(getActivity(), phoneNumber);

        } else {

            List<MemberContact> phoneNumbers = provider.getContactIdentifiers(
                    Platform.CALL, feed.getContact().getContactId());

            if (phoneNumbers != null) {

                if (phoneNumbers.size() == 1) {

                    CallsManager.makeACall(getActivity(), phoneNumbers.get(0)
                            .getIdentifier());

                } else {

                    OnItemClickListener listener = new OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent,
                                                View view, int position, long id) {
                            // Get selected number.
                            String number = String.valueOf(parent
                                    .getItemAtPosition(position));
                            CallsManager.makeACall(getActivity(), number);
                        }
                    };

                    DialogUtils.openChooseNumberDialog(getActivity(), listener,
                            phoneNumbers, Platform.CALL);

                }
            } else {

                UIUtils.alertUser(context,
                        getActivity().getString(R.string.long_press_call_error));
            }
        }
    }

    private void setFragmentAdapter() {

        // Instantiating an adapter only if there is data.
        if (feeds != null && feeds.size() > 0) {

            initializeAdapter();
            setListAdapter(feedsAdapter);
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.viewToSendDatabaseAndLog:

                // Show dialog if number of repeated clicks reach 5
                int NUMBER_OF_CLICKS_TO_SHOW_DIALOG = 5;
                if (++numberOfClicksOnBottomLeftCorner == NUMBER_OF_CLICKS_TO_SHOW_DIALOG) {

                    buildSendDatabaseLogDialog();
                }

                // Reset the timer if user click again within specific period of
                // time.
                clicksTimer.cancel();
                clicksTimer = new Timer();
                int PERIOD_BETWEEN_CLICKS = 1500;
                clicksTimer.schedule(new TimerTask() {
                    public void run() {

                        numberOfClicksOnBottomLeftCorner = 0;

                    }
                }, PERIOD_BETWEEN_CLICKS);

                break;

        }

    }


    @Override
    public void onFeedDeleted(int position) {

        Provider provider = FeedProviderImpl.getInstance(context);

        if (provider.deleteFeed(feeds.get(position).getFeedId())) {

            feeds.remove(position);
            hideFeedOptions(position + 1, FEED_OPTIONS_SLIDE_DURATION_FAST);
            refresh();

        } else {

            UIUtils.alertUser(context, "Can't remove feed");
        }
    }


    /**
     * Set a flag indicating if the search view currently take the action, used to disable/enable showing feeds options
     * depends on search view status.
     *
     * @param isSearchViewStarted true if the search view currently take the action, false otherwise.
     */
    public void setSearchViewStarted(boolean isSearchViewStarted) {
        this.isSearchViewStarted = isSearchViewStarted;
    }

    @Override
    public void onDataLoaded() {

        //applyFirstTimeAnimation();
    }


    /**
     * An inner interface which the activity must implement in order to handle
     * clicks by fragment and forward them to container  activity.
     *
     * @author Johan Hansson
     */
    public interface OnFeedSelectedListener {

        /**
         * @param feedUri Uri contains information about the selected feed.
         */
        public void onFeedSelected(Uri feedUri, Platform platform);

    }

    /**
     * Add list of feeds to feed adapter.
     *
     * @param newFeeds list of modified feeds.
     */
    public void updateFeeds(List<FeedModel> newFeeds) {

        int newIndex;
        int currentFeedIndex;

        // Some times called before initialization of the activity.
        if (feeds == null) {
            return;

        } else if (feedsAdapter == null) {

            // Initialize adapter if it's not initialized.
            initializeAdapter();
        }

        // Initialize image loader again to take the effect of theme
        // changes.
        feedsAdapter.refreshImageLoader();

        if (newFeeds.size() > 0) {

            for (int newFeedIndex = (newFeeds.size() - 1); newFeedIndex >= 0; newFeedIndex--) {

                currentFeedIndex = getFeedPosition(newFeeds.get(newFeedIndex)
                        .getFeedId());

                if (currentFeedIndex != FEED_IS_NOT_EXIST) {

                    feeds.remove(currentFeedIndex);

                }

                // To hide empty feeds.
                if (newFeeds.get(newFeedIndex).getMessagesCount() > 0) {

                    newIndex = getNewFeedIndex(newFeeds.get(newFeedIndex));
                    feeds.add(newIndex, newFeeds.get(newFeedIndex));
                }

            }

            refresh();

        } else if (preferencesManager != null
                && preferencesManager.getProperty(
                PreferencesManager.IS_THEME_CHANGED, false)) {

            // Theme is changed.
            refresh();
        }

    }


    /**
     * Refresh feeds adapter.
     */
    public void refresh() {

        if (feedsAdapter == null) {

            // Adapter not created. create new one.
            setFragmentAdapter();

        } else {

            // Adapter exist, update it's contents.
            feedsAdapter.notifyDataSetChanged();
        }

        setBackgroundImage();
    }

    private void setBackgroundImage() {
        getListView().setBackground(feeds == null || feeds.size() > 1 ?
                getResources().getDrawable(R.drawable.feed_background) :
                getResources().getDrawable(R.drawable.feed_background_text));
    }


    /**
     * Returns the insertion place of new feed.
     *
     * @param newFeed the new feed to be inserted
     * @return index of new feed inside the list
     */
    private int getNewFeedIndex(FeedModel newFeed) {
        int index;

        for (index = 0; index < feeds.size(); index++) {
            if (newFeed.getLastMessageTime().after(
                    feeds.get(index).getLastMessageTime())) {
                break;
            }
        }
        return index;
    }

    /**
     * Returns the feed position in feed list.
     *
     * @param feedID The ID of feed.
     * @return The position of given feed in feeds list.
     */
    private int getFeedPosition(int feedID) {
        for (FeedModel feed : feeds) {
            if (feed.getFeedId() == feedID)
                return feeds.indexOf(feed);
        }
        return FEED_IS_NOT_EXIST;
    }

    /**
     * Updates last message date for all feeds in the fragment.
     */
    public void updateFeedLastMessageTime() {

        if (feedsAdapter == null)
            initializeAdapter();

        for (int feedIndex = 0; feedIndex < feedsAdapter.getCount(); feedIndex++) {

            feedsAdapter.updateLastMessageDate(feedIndex, feeds.get(feedIndex)
                    .getLastMessageTime());

            //Update the visible item layouts.
            if (feedIndex >= searchListView.getFirstVisiblePosition() && feedIndex <= searchListView.getLastVisiblePosition())
                updateLastMessageTimeView(feedIndex, feeds.get(feedIndex).getLastMessageTimeString());
        }
    }

    /**
     * Update the last message time for the feed item.
     *
     * @param position - feed position in list-view.
     * @param date     - string date format.
     */
    private void updateLastMessageTimeView(int position, String date) {

        View view = UIUtils.getTheViewOfListItemAtSpecificPosition(position, searchListView);
        if (view == null)
            return;

        TextView timeView = (TextView) view.findViewById(R.id.textViewForMessageTime);
        timeView.setText(date);
    }

    /**
     * @return list of feeds in the fragment.
     */
    public List<FeedModel> getFeeds() {
        return feeds;
    }

    /**
     * @param feeds list of feeds in the fragment.
     */
    public void setFeeds(List<FeedModel> feeds) {
        this.feeds = feeds;
    }

    /**
     * @param isAllFeed true if it's the All feed fragment, false if not.
     */
    public void setAllFeed(boolean isAllFeed) {
        this.isAllFeed = isAllFeed;
    }

    /**
     * Initialize the feed adapter depends on the device font scale.
     */
    private void initializeAdapter() {

        feedsAdapter = new FeedAdapter(context, R.layout.feed_item_layout,
                feeds, this, platforms, this);

        feedsAdapter.setAllFeed(isAllFeed);
    }

    /**
     * Marks given feed as read.
     *
     * @param feedID The ID of the feed.
     */
    public void markFeedAsRead(int feedID) {
        int position = getFeedPosition(feedID);
        if (position != -1 && !feeds.get(position).isRead()) {
            feeds.get(position).setRead(true);
            feedsAdapter.markFeedAsRead(position);
            refresh();
        }
    }

    /**
     * Asynchronous task to load more feeds in background.
     *
     * @author Johan Hansson.
     */
    private class FeedsLoader extends AsyncTask<String, String, String> {

        private Activity activity;
        private boolean isRunning = false;

        /**
         * @param activity activity to show dialog over.
         */
        public FeedsLoader(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {

            isRunning = true;

        }

        @Override
        protected String doInBackground(String... params) {

            activity.runOnUiThread(new Runnable() {
                public void run() {

                    loadMoreFeeds();
                    isRunning = false;

                }

            });
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

        }

    }

    /**
     * Load more feeds from database and update the UI.
     */
    private void loadMoreFeeds() {

        if (feedsAdapter == null)
            initializeAdapter();

        int previousFeedsCount = feedsAdapter.getCount();
        List<FeedModel> loadedFeeds;

        if (isAllFeed) {
            loadedFeeds = provider.getFeeds(previousFeedsCount,
                    Constants.NUMBER_OF_FEEDS_TO_LOAD, platforms);
        } else {
            Platform platform = platforms.get(0);
            loadedFeeds = provider.getFeedsFor(platform, previousFeedsCount,
                    Constants.NUMBER_OF_FEEDS_TO_LOAD);
        }

        for (FeedModel feed : loadedFeeds) {

            if (feed.getMessagesCount() > 0) {

                feeds.add(feed);
            }
        }

        refresh();

        // Check if there is more feeds to be loaded.
        hasMoreFeedsToLoad = (feedsAdapter.getCount() - previousFeedsCount) == Constants.NUMBER_OF_FEEDS_TO_LOAD;
    }

    /**
     * Return platforms to be displayed in the fragment.
     *
     * @return platforms array of platforms id's.
     */
    public List<Platform> getPlatforms() {
        return platforms;
    }

    /**
     * Set platforms to be displayed in the fragment.
     *
     * @param platforms array of platforms id's
     */
    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    /**
     * Sets layout that contain view of search.
     *
     * @param searchLayout the layout that contain search view.
     */
    public void setSearchLayout(LinearLayout searchLayout) {

        this.searchLayout = searchLayout;
    }

    /**
     * Build send database-log dialog.
     */
    private void buildSendDatabaseLogDialog() {

        dialogBuilder = new CustomDialogBuilder(getActivity());

        dialogBuilder
                .setTitle(getString(R.string.send_database_log_dialog_title))
                .setMessage(getString(R.string.send_database_log_dialog_body))
                .showIcon(false)
                .setPositiveButton(
                        R.string.send_database_log_dialog_yes_button,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                sendDatabaseLogDialog.dismiss();
                                buildCopyingDialog();
                                new CopyFilesInBackground().execute(null, null,
                                        null);

                            }
                        }
                )
                .setNegativeButton(R.string.send_database_log_dialog_no_button,
                        null);

        dialogBuilder.setCancelable(false);

        sendDatabaseLogDialog = dialogBuilder.create();
        sendDatabaseLogDialog.show();
    }

    /**
     * Build copying database-log dialog.
     */
    private void buildCopyingDialog() {

        dialogBuilder = new CustomDialogBuilder(getActivity());

        dialogBuilder
                .setTitle(getString(R.string.copy_database_log_dialog_title))
                .setMessage(getString(R.string.copy_database_log_dialog_body))
                .showIcon(true);

        dialogBuilder.setCancelable(false);

        copyingDialog = dialogBuilder.create();
        copyingDialog.show();
    }

    private class CopyFilesInBackground extends
            AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {

            FileUtils.prepareDatabaseLogToSend(getActivity(), true);

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Hide copying dialog
            copyingDialog.dismiss();

            EmailUtils.sendDatabaseLog(getActivity());
        }

    }

}
