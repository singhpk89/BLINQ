package com.blinq.ui.views;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.blinq.R;
import com.blinq.analytics.Analytics;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.ui.fragments.FeedsFragmentList;
import com.blinq.utils.AppUtils;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

/**
 * Custom ListView that contain search view as header and that have its own
 * header view behavior.
 *
 * @author Johan Hansson
 */
public class SearchListView extends ListView {

    private static final String TAG = SearchListView.class.getSimpleName();

    private static final int SEARCH_BAR_HEIGHT_DP = 48;
    private static final int PAGE_INDICATOR_HEIGHT_DP = 35;

    private static final double START_TRANSPARENCY_PERCENTAGE = 0.1;
    private static final double END_TRANSPARENCY_PERCENTAGE = 0.7;
    private static final double STATIC_TRANSPARENCY_PERCENTAGE = 0.7;

    // Search view components hierarchy indices
    private final int SEARCH_PARENT_LAYOUT = 0;
    private final int SEARCH_EDIT_FRAME = 2;
    private final int SEARCH_PLATE_FRAME = 0;
    private int SEARCH_TEXT_VIEW = 0;

    private float previousY;
    private float previousX;

    private FrameLayout header;
    private FrameLayout headerContent;
    private FrameLayout searchViewContainer;
    private LinearLayout searchLayout;
    private SearchView searchView;
    private TextView searchTextView;

    private Context context;
    private Activity activity;

    private boolean isSearchViewInserted = false;
    private boolean isSearchViewVisible = false;

    private boolean isLayoutChanged = false;

    private Analytics analyticsManager;

    private int searchBarHeightPX;

    private int pageIndicatorHeightPX;
    private FeedsFragmentList fragment;

    public SearchListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initDataComponents(context);
    }

    public SearchListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initDataComponents(context);
    }

    public SearchListView(Context context) {
        super(context);
        this.context = context;
        initDataComponents(context);
    }

    /**
     * Initialize the needed components
     */
    private void initDataComponents(Context context) {
        this.context = context;
        analyticsManager = new BlinqAnalytics(context);
        header = new FrameLayout(context);
        addHeaderView(header);

        searchBarHeightPX = (int) UIUtils.getPx(SEARCH_BAR_HEIGHT_DP, context);
        pageIndicatorHeightPX = (int) UIUtils.getPx(PAGE_INDICATOR_HEIGHT_DP,
                context);

    }

    /**
     * Send Search screen event
     */
    private void sendSearchActivityAnalytics(String from) {
        analyticsManager
                .sendEvent(AnalyticsConstants.OPENED_SEARCH_SCREEN_EVENT, AnalyticsConstants.FROM_PROPERTY, from, true, AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Initiate items of the class.
     */
    public void initUiComponents() {

        searchViewContainer = (FrameLayout) searchLayout
                .findViewById(R.id.search_header);
        searchView = (SearchView) searchLayout
                .findViewById(R.id.searchView);
        LinearLayout parentFrame = (LinearLayout) searchView
                .getChildAt(SEARCH_PARENT_LAYOUT);
        LinearLayout searchEditFrame = (LinearLayout) parentFrame
                .getChildAt(SEARCH_EDIT_FRAME);

        LinearLayout searchPlate = (LinearLayout) searchEditFrame
                .getChildAt(SEARCH_PLATE_FRAME);
        searchTextView = (TextView) searchPlate.getChildAt(SEARCH_TEXT_VIEW);
        searchTextView.clearFocus();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (isLayoutChanged) {
            previousY = event.getY();
            isLayoutChanged = false;
        }

        // Add Search Bar if it is not already inserted
        if (!isSearchViewInserted && getFirstVisiblePosition() == 0) {

            storeOnTouchMotionEventData(event);
        }

        if (isSearchViewInserted && getFirstVisiblePosition() == 0) {

            // Set search view as visible (used to disable showing feed options while search taking the action).
            // TODO: Uncomment if search is back
            //fragment.setSearchViewStarted(true);

            //analysisOnTouchMotionEventData(event);
            //return true;

            return super.onTouchEvent(event);
        }

        return super.onTouchEvent(event);
    }

    private void analysisOnTouchMotionEventData(MotionEvent event) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) searchViewContainer
                .getLayoutParams();
        searchLayout.setVisibility(View.VISIBLE);

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                previousY = event.getY() + searchBarHeightPX;

                break;
            case MotionEvent.ACTION_UP:

                final int currentHeight = params.height;

                if (currentHeight > pageIndicatorHeightPX) {

                    if (currentHeight < searchBarHeightPX) {

                        showSearchView(AnalyticsConstants.SWIPE_VALUE);

                    } else {

                        applyShowingSearchViewAnimation(currentHeight);
                    }

                } else {

                    // as a search event
                    removeSearchView();

                    fragment.openSelectedFeedWhenSearchViewInitializationNotCompleted();
                }

                break;

            case MotionEvent.ACTION_MOVE:

                float y = event.getY();
                float diff = y - previousY;
                previousY = y;
                params = (LinearLayout.LayoutParams) searchViewContainer
                        .getLayoutParams();

                // When the user scrolls down
                if (!isSearchViewVisible) {

                    params.height = Math.min(Math.round(params.height + (diff)),
                            (int) UIUtils.getPx(SEARCH_BAR_HEIGHT_DP, context));
                    if (params.height > 0) {

                        searchViewContainer.setLayoutParams(params);
                        applySearchBackgroundAnimation(params.height);
                    }
                }

                break;
        }

    }

    /**
     * Apply drag/drop animation on displaying the search view from the
     * scrolling down
     */
    private void applyShowingSearchViewAnimation(int currentHeight) {
        searchViewContainer.animate()
                .translationY(-(currentHeight - searchBarHeightPX))
                .withStartAction(new Runnable() {

                    @Override
                    public void run() {

                        applySearchBackgroundAnimation(searchBarHeightPX);
                        setSearchBackgroundAlpha(STATIC_TRANSPARENCY_PERCENTAGE);
                    }
                }).withEndAction(new Runnable() {

            @Override
            public void run() {

                searchViewContainer.setTranslationY(0);
                showSearchView(AnalyticsConstants.SWIPE_VALUE);

            }
        });

    }

    private void storeOnTouchMotionEventData(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                previousY = event.getY();
                previousX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                float x = event.getX();

                float diff = y - previousY;
                float diffX = x - previousX;

                previousY = y;
                previousX = x;

                if (diff > 0.4 && Math.abs(diffX) < 0.5) {
                    this.insertSearchView();
                    isLayoutChanged = true;
                }
                break;
        }

    }

    /**
     * Set transparency percentage of search view background depending on search
     * view height.
     *
     * @param height the current height of scrolled search view.
     */
    private void applySearchBackgroundAnimation(int height) {

        double heightPercentage, alpha;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) headerContent
                .getLayoutParams();

        heightPercentage = (double) height
                / UIUtils.getPx(SEARCH_BAR_HEIGHT_DP, context);
        alpha = heightPercentage
                * (END_TRANSPARENCY_PERCENTAGE - START_TRANSPARENCY_PERCENTAGE)
                + START_TRANSPARENCY_PERCENTAGE;
        setSearchBackgroundAlpha(alpha);

        if (height > UIUtils.getPx(PAGE_INDICATOR_HEIGHT_DP, context)) {
            params.height = (int) (height - UIUtils.getPx(
                    PAGE_INDICATOR_HEIGHT_DP, context));
            headerContent.setLayoutParams(params);
        }
    }

    /**
     * Sets search view background transparency to given percentage.
     *
     * @param alpha the percentage of transparency to be set.
     */
    private void setSearchBackgroundAlpha(double alpha) {

        int mappedAlpha = (int) (alpha * 255);
        searchLayout.setBackgroundColor(Color.argb(mappedAlpha, 0, 0, 0));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        return false;
    }

    /**
     * Returns boolean that describe header search view visibility status.
     *
     * @return true if search view is visible, false otherwise.
     */
    public boolean isSearchViewVisible() {
        return isSearchViewVisible;
    }

    /**
     * sets visibility status of header view.
     *
     * @param isSearchViewVisible the visibility status to be set.
     */
    public void setSearchViewVisible(boolean isSearchViewVisible) {
        this.isSearchViewVisible = isSearchViewVisible;
    }

    /**
     * Returns boolean that describe header search view existence status .
     *
     * @return true if search view is inserted, false otherwise.
     */
    public boolean isSearchViewInserted() {
        return isSearchViewInserted;
    }

    /**
     * Inserts header view in the list and sets isSearchViewInserted as true.
     */
    public void insertSearchView() {

        isSearchViewInserted = true;
        headerContent = new FrameLayout(context);
        header.addView(headerContent);

    }

    /**
     * Sets layout that contain list view of search.
     *
     * @param searchLayout the layout that contain search view.
     */
    public void setSearchLayout(LinearLayout searchLayout) {
        this.searchLayout = searchLayout;
    }

    /**
     * Show search view without animation.
     */
    public void showSearchView(String from) {

        // Send search analytics
        sendSearchActivityAnalytics(from);
        LinearLayout.LayoutParams params;

        this.insertSearchView();
        searchLayout.setVisibility(View.VISIBLE);

        params = (LinearLayout.LayoutParams) searchViewContainer
                .getLayoutParams();
        params.height = searchBarHeightPX;
        searchViewContainer.setLayoutParams(params);
        setSearchViewVisible(true);
        applySearchBackgroundAnimation(searchBarHeightPX);
        setSearchBackgroundAlpha(STATIC_TRANSPARENCY_PERCENTAGE);
        // Show keyboard when user search view
        // start.
        searchTextView.requestFocus();
        AppUtils.showKeyboard(activity, searchTextView);

    }

    /**
     * Hide search view.
     */
    public void removeSearchView() {

        ObjectAnimator
                .ofFloat(
                        searchViewContainer,
                        "translationY",
                        -(UIUtils.getPx(SEARCH_BAR_HEIGHT_DP, context) - searchBarHeightPX),
                        0).setDuration(0).start();

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) searchViewContainer
                .getLayoutParams();
        params.height = 1;
        searchViewContainer.setLayoutParams(params);
        setSearchViewVisible(false);
        isSearchViewInserted = false;
        searchLayout.setVisibility(View.INVISIBLE);
        searchView.setQuery(StringUtils.EMPTY_STRING, false);
        searchView.clearFocus();
        header.removeAllViews();

        // Set search view as not visible (used to enable showing feeds options when long click).
        fragment.setSearchViewStarted(false);
    }

    /**
     * @return activity where search list shown.
     */
    public Activity getActivity() {
        return activity;
    }

    /**
     * @param activity where search list shown.
     */
    public void setActivity(Activity activity) {
        this.activity = activity;
    }


    /**
     * Set reference on fragment containing this list-view.
     *
     * @param fragment fragment containing the list-view..
     */
    public void setFragment(FeedsFragmentList fragment) {
        this.fragment = fragment;
    }
}