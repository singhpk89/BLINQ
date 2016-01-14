package com.blinq.ui.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.blinq.BlinqApplication;
import com.blinq.ImageLoaderManager;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.ThemeManager;
import com.blinq.analytics.Analytics;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.impl.provider.GlobalSearchProvider;
import com.blinq.media.HeadboxAudioManager;
import com.blinq.models.Contact;
import com.blinq.models.FeedDesign;
import com.blinq.models.FeedModel;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.provider.ContactsMerger;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.MergeSearchProvider;
import com.blinq.provider.Provider;
import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.ui.activities.search.BlinqSearchHandler;
import com.blinq.ui.activities.search.BlinqSearchHandler.OnMergeItemSelectedListener;
import com.blinq.ui.views.CustomTypefaceTextView;
import com.blinq.ui.views.SearchViewBuilder;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.TwitterException;

/**
 * Provides a way to link more platforms.
 *
 * @author Johan Hansson.
 */
public class BlinqMergeFragment extends Fragment implements OnClickListener,
        OnMergeItemSelectedListener {

    public static final String TAG = MergeFragment.class.getSimpleName();
    public static final String DISPLAY_LAST_SEARCH_TEXT = "display_last_search_text";
    public static final String LAST_SEARCH_TEXT = "last_merge_search_text";
    private static final int GLOBAL_SEARCH_RESULTS_LIMIT = 20;

    private FeedModel feed;
    private Contact contact;
    private Platform platform;
    private BlinqSearchHandler.SearchViewMode mergeType;
    private int feedId;

    private List<SearchResult> searchResults;
    private List<SearchResult> suggestedList;

    private boolean showSuggestion = false;
    private String lastSearchText;
    private boolean displayLastSearchText = false;

    private Context context;
    private PopupSocialWindow activity;
    private Animation rotateAnimation;

    private Analytics analyticsManager;
    private PreferencesManager preferencesManager;
    private HeadboxAudioManager audioManager;
    private Provider provider;
    private ImageLoaderManager imageLoaderManager;
    private BlinqSearchHandler searchHandler;

    private boolean isFirstSearch;
    private int lastSearchRequest = 1;
    private int lastSearchResult;

    /**
     * Responsible to manage the global search.
     */
    private GlobalSearchProvider searchProvider;

    /**
     * Layouts and views.
     */
    private View root;
    private ImageView contactImageView;

    private LinearLayout mainMergeLayout;
    private TextView suggestionTitle;
    private TextView suggestionHint;
    private ImageView suggestionSeparator;
    private LinearLayout suggestionLayout;
    private ListView searchResultList;
    private SearchView searchView;
    private TextView textViewForSearchContent;
    private LinearLayout noMergeResultsLinearLayout;
    private ImageView noMergeResultsContactImageView;
    private ImageView noMergeResultsPlatformImageView;
    private CustomTypefaceTextView noMergeResultsTextView;
    private CustomTypefaceTextView noMergeResultsTitleText;


    /**
     * Indicates whether the global search is loading or not.
     */
    private boolean isLoading;


    /**
     * Callback
     */
    private OnMergeDone onMergeDone;


    /**
     * Listener for the global search action.
     * Works to populate the search list.
     */
    private OnActionListener<List<SearchResult>> globalSearchListener = new OnActionListener<List<SearchResult>>() {

        @Override
        public void onLoading() {
            showLoadingProgressBar(true);
            isLoading = true;
        }

        @Override
        public void onFail(String reason) {
            isLoading = false;
            showLoadingProgressBar(false);
        }

        @Override
        public void onException(Throwable throwable) {
            super.onException(throwable);
            isLoading = false;
            showLoadingProgressBar(false);
            if (throwable instanceof TwitterException &&
                    ((TwitterException) throwable).exceededRateLimitation()) {
                handleTwitterRateLimitException();
            }
        }

        private void handleTwitterRateLimitException() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(activity, getResources().getString(R.string.twitter_rate_limit_exceed), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 50);
                    toast.show();
                }
            });
        }

        @Override
        public void onComplete(final List<SearchResult> response) {

            isLoading = false;
            lastSearchResult++;
            if (lastSearchRequest != lastSearchResult) {
                return;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (isFirstSearch) {
                        handleFirstSearch(response);
                    } else {
                        searchResults = response;
                    }
                    BlinqApplication.searchResults = null;
                    searchHandler.setSearchResults(searchResults);
                    searchHandler.new SearchResultHandler().execute(null, null, null);
                    showSuggestion = false;
                    updateUIDependsOnSearchResults();
                    showLoadingProgressBar(false);
                }
            });
        }

        private void handleFirstSearch(List<SearchResult> response) {
            if (suggestedList.isEmpty()) {
                suggestedList = response;
            } else if (suggestedList.size() == 1) {
                suggestedList.addAll(response);
            } else {
                // Build the result such that the 1st is the suggested
                // 2nd is from global search, and rest is suggested
                suggestedList.addAll(1, response);
            }
            searchResults = suggestedList;
            isFirstSearch = false;
        }
    };


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        isFirstSearch = true;
        activity = (PopupSocialWindow) getActivity();
        context = activity.getApplicationContext();
        imageLoaderManager = new ImageLoaderManager(context);
        provider = FeedProviderImpl.getInstance(activity.getApplicationContext());
        preferencesManager = new PreferencesManager(context);
        analyticsManager = new BlinqAnalytics(context);
        searchProvider = new GlobalSearchProvider(activity, globalSearchListener, platform, GLOBAL_SEARCH_RESULTS_LIMIT);
        audioManager = HeadboxAudioManager.getInstance(activity.getApplicationContext());
        feed = provider.getFeed(feedId);

        root = inflater.inflate(R.layout.merge_fragment_layout, container, false);

        customizeActionBar();

        lastSearchText = preferencesManager.getProperty(LAST_SEARCH_TEXT, StringUtils.EMPTY_STRING);
        preferencesManager.setProperty(LAST_SEARCH_TEXT, StringUtils.EMPTY_STRING);
        displayLastSearchText = preferencesManager.getProperty(BlinqMergeFragment.DISPLAY_LAST_SEARCH_TEXT, false);

        init();

        analyticsManager.sendEvent(AnalyticsConstants.PERSON_MERGED_START_EVENT,
                AnalyticsConstants.TYPE_PROPERTY, platform.getName(),
                true, AnalyticsConstants.ACTION_CATEGORY);

        initiateSearchList();

        return root;
    }

    /**
     * Search initially for contact using the contact's name.
     * Locally and then Globally.
     */
    private void initiateSearchList() {

        if (suggestedList.size() == 0 && !isFirstSearch) {
            textViewForSearchContent.append(contact.getName());
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            onMergeDone = (OnMergeDone) activity;
        } catch (Exception e) {
            Log.e(TAG, "" + activity.getClass().getSimpleName() + " must implement " + OnMergeDone.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        onMergeDone = null;
        super.onDetach();
    }


    private void customizeActionBar() {

        ActionBar actionBar = activity.getActionBar();
        actionBar.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.action_bar_background));
        actionBar.setDisplayHomeAsUpEnabled(true);

        //disable application icon from ActionBar
        //  actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setIcon(R.drawable.merge_action_bar_custom_icon);
        //disable application name from ActionBar
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater mInflater = LayoutInflater.from(activity);
        View customView = mInflater.inflate(R.layout.merge_custom_action_bar, null);

        contactImageView = (ImageView) customView.findViewById(R.id.merge_contact_image);
        imageLoaderManager.loadContactAvatarImage(contactImageView, feed.getContact(), false);
        contactImageView.setOnClickListener(this);

        initPlatformIconAnimation();

        searchView = (SearchView) customView.findViewById(R.id.searchView);

        actionBar.setCustomView(customView);
        actionBar.setDisplayShowCustomEnabled(true);

        UIUtils.showActionbar(activity);
    }

    @Override
    public void onDestroy() {
        UIUtils.hideActionbar(activity);
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (suggestedList.size() > 0) {
            showSuggestion = true;
        } else {
            showSuggestion = false;
            UIUtils.showKeyboardOnTextView(activity, textViewForSearchContent);
        }

        updateUIDependsOnSearchResults();

    }

    /**
     * Hide/Show no merge results message & suggestion layout depends on the
     * status of merge list.
     */
    private void updateUIDependsOnSearchResults() {

        String searchText = textViewForSearchContent.getText().toString();
        // Hide/Show no merge results message depend if there is some searchText or not
        if ((searchResults != null && searchResults.size() > 0)
                || (searchText != null && searchText.length() > 0)) {
            noMergeResultsLinearLayout.setVisibility(View.GONE);
        } else if (!isLoading) {
            noMergeResultsLinearLayout.setVisibility(View.VISIBLE);
        }

        // Hide/Show suggestion layout.
        if (showSuggestion && suggestedList.size() > 0) {
            sendMergeSuggestedAnalytics();
            suggestionLayout.setVisibility(View.VISIBLE);

        } else {
            suggestionLayout.setVisibility(View.GONE);
        }

    }

    /**
     * Initialize UI components of MergeFragment view.
     */
    private void init() {

        contact = feed.getContact();

        Log.d(TAG, "Open merge view for feed Id :" + feedId + " & contact Id:"
                + contact.getContactId() + " In platform " + platform.getName());

        mainMergeLayout = (LinearLayout) root.findViewById(R.id.merge_layout);

        suggestionLayout = (LinearLayout) root.findViewById(R.id.suggestion_layout);

        suggestionTitle = (TextView) root.findViewById(R.id.suggestion_title);
        suggestionHint = (TextView) root.findViewById(R.id.suggestion_hint);

        suggestionSeparator = (ImageView) root.findViewById(R.id.suggestion_separator);

        noMergeResultsLinearLayout = (LinearLayout) root.findViewById(R.id.linearLayoutForNoMergeResults);

        noMergeResultsTextView = (CustomTypefaceTextView) root.findViewById(R.id.textViewForNoMergeResultsMessage);
        noMergeResultsTextView.setText(buildNoMergeResultsMessage());

        noMergeResultsTitleText = (CustomTypefaceTextView) root.findViewById(R.id.textViewForNoMergeResultsTitle);
        noMergeResultsTitleText.setText(buildNoMergeResultsTitle());

        noMergeResultsContactImageView = (ImageView) root.findViewById(R.id.imageViewForContactNoMergeResults);
        imageLoaderManager.loadContactAvatarImage(noMergeResultsContactImageView, feed.getContact(), false);

        noMergeResultsPlatformImageView = (ImageView) root.findViewById(R.id.imageViewForPlatformNoMergeResults);
        noMergeResultsPlatformImageView.setBackgroundResource(getPlatformIcon());

        setDesignMode();

        searchResultList = (ListView) root.findViewById(R.id.listViewForSearchResult);
        customizeSearchView();

        //To get suggestion results for a given contact name.
        suggestedList = ContactsMerger.getInstance(context).getContactsSearchSuggestions(feedId,
                contact.getContactId(), contact.getName(), platform);
        searchProvider.execute(contact.getName());
        searchHandler.setPlatform(platform);

        if (!displayLastSearchText || StringUtils.isBlank(lastSearchText)) {
            searchResults = suggestedList;
            //setUpSearchResultsList();
        } else {
            searchResults = new ArrayList<SearchResult>();
        }
    }

    /**
     * Initialize the rotate animation for platform icon.
     */
    private void initPlatformIconAnimation() {

        rotateAnimation = AnimationUtils.loadAnimation(activity,
                R.anim.merge_platform_icon_animation);
        rotateAnimation.setFillAfter(true);

        rotateAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                //activity.displaySocialWindowView(platform);
                onMergeDone.onMergeDone();
            }
        });

    }

    private void setDesignMode() {

        ThemeManager.updateThemeData(getActivity().getApplicationContext());

        FeedDesign modeDesign = FeedDesign.getInstance();

        String suggestionHintText = getString(R.string.merge_suggestion_hint, contact.getName(), platform.name());
        suggestionHint.setText(suggestionHintText);

        mainMergeLayout.setBackgroundColor(modeDesign.getMergeViewBackgroundColor());
        suggestionTitle.setTextColor(modeDesign.getMergeViewSuggestionTitleColor());
        suggestionHint.setTextColor(modeDesign.getMergeViewSuggestionHintColor());
        //suggestionSeparator.setImageResource(modeDesign.getMergeViewSuggestionSeparatorColor());

    }

    private void sendMergeSuggestedAnalytics() {

        analyticsManager.sendEvent(AnalyticsConstants.PERSON_SUGGESTED_MERGED_EVENT, true, AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Build the search view hint string using contact & platform.
     *
     * @return search view hint.
     */
    private String buildSearchViewHint() {

        String contactName = StringUtils.getFirstNameFromContactName(contact.getName());

        return getString(R.string.merge_search_view_hint, contactName);
    }

    /**
     * Build the title that will be displayed when there is no merge results.
     *
     * @return no merge results title.
     */
    private String buildNoMergeResultsTitle() {

        String platform = getPlatform().name().toUpperCase();

        return getString(R.string.merge_empty_results_title, platform);
    }

    /**
     * Build the message to be displayed when there is no merge results.
     *
     * @return no merge results message.
     */
    private String buildNoMergeResultsMessage() {

        String contactName = contact.getName();

        return getString(R.string.merge_empty_results_text, contactName, platform.getName());
    }

    /**
     * Return the icon id from resources for given platform.
     *
     * @return platform icon id from resources.
     */
    private int getPlatformIcon() {

        switch (platform) {

            case FACEBOOK:
                return R.drawable.merge_icon_facebook;
            case HANGOUTS:
                return R.drawable.merge_icon_hangouts;
            case TWITTER:
                return R.drawable.merge_icon_twitter;
            case INSTAGRAM:
                return R.drawable.merge_icon_instagram;
            case WHATSAPP:
                return R.drawable.merge_icon_whatsapp;
            case EMAIL:
                return R.drawable.merge_icon_mail;
            case SKYPE:
                return R.drawable.merge_icon_skype;
            case CALL:
                return R.drawable.merge_icon_sms;
            default:
                return R.drawable.ic_launcher;
        }

    }

    /**
     * Show/hide the activity progress bar while loading the global search results.
     *
     * @param show - show if true. hide if false.
     */
    private void showLoadingProgressBar(final boolean show) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // hide the loading button.
                activity.setProgressBarIndeterminateVisibility(show);
            }
        });
    }

    /**
     * Customizes search view edit text and cancel button.
     */

    private void customizeSearchView() {
        OnClickListener cancelButtonOnClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                textViewForSearchContent.setText(StringUtils.EMPTY_STRING);
                showLoadingProgressBar(false);
                AppUtils.hideKeyboard(activity);
            }
        };

        SearchViewBuilder searchViewBuilder = new SearchViewBuilder()
                .setContext(context)
                .setSearchView(searchView)
                .setCancelImageViewResourceId(R.drawable.ic_action_remove)
                .setCancelButtonOnClickListener(cancelButtonOnClickListener)
                .setSearchPlateBackgroundResourceId(R.drawable.action_bar_background)
                .setSearchBarTextColorResourceId(R.color.search_bar_text)
                .setTextContentTypeFace(UIUtils.getFontTypeface(context, UIUtils.Fonts.ROBOTO_CONDENSED))
                .setSearchQueryHint(buildSearchViewHint())
                .setSearchContentTextCursorResource(R.drawable.custom_cursor)
                .build();

        textViewForSearchContent = searchViewBuilder.getTextViewForSearchContent();

        searchHandler = new BlinqSearchHandler(this, searchResultList, searchView, mergeType, feedId);
        connectToSearchAPI();
    }

    public BlinqSearchHandler getSearchHandler() {
        return searchHandler;
    }

    /**
     * Initializes the connection with search API.
     */
    private void connectToSearchAPI() {

        SearchManager searchManager = (SearchManager) activity
                .getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity
                .getComponentName()));
    }


    @Override
    public void onResume() {
        BlinqApplication.searchType = MergeSearchProvider.SearchActivityType.POPUP;

        if (displayLastSearchText) {

            if (!StringUtils.isBlank(lastSearchText))
                searchView.setQuery(lastSearchText, true);

            preferencesManager.setProperty(DISPLAY_LAST_SEARCH_TEXT, false);
            displayLastSearchText = false;
        }

        super.onResume();
    }

    /**
     * Handle search intents sent from search view.
     */
    public void handleIntent(Intent intent) {

        // Show results when the intent action is search.
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            updateContactsList();
        }
    }

    private void updateContactsList() {

        String searchText = textViewForSearchContent.getText().toString();

        if (!searchText.trim().equalsIgnoreCase(""))

        {

            showSuggestion = false;

            Cursor cursor = activity.getContentResolver().query(
                    MergeSearchProvider.CONTENT_URI, null, null, null,
                    platform.getId() + "");
            if (cursor != null) {

                if (!cursor.isClosed()) {

                    searchResults = provider.convertToSearchResult(cursor);
                } else {
                    searchResults = BlinqApplication.searchResults;
                    BlinqApplication.searchResults = null;
                }

            } else {

                searchResults = new ArrayList<SearchResult>();
            }
        } else {


            showSuggestion = true;

            if (StringUtils.isBlank(lastSearchText))
                searchResults = suggestedList;
        }
        setUpSearchResultsList();

        updateUIDependsOnSearchResults();

    }

    private void setUpSearchResultsList() {

        String searchText = textViewForSearchContent.getText().toString();

        if ((searchResults == null || searchResults.size() == 0)
                && !searchText.trim().equalsIgnoreCase("")
                && searchProvider.getSearchAction() != null) {
            lastSearchRequest++;
            searchProvider.execute(searchText);

        } else {
            searchHandler.setSearchResults(searchResults);
            searchHandler.new SearchResultHandler().execute(null, null, null);

        }

    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
        // temp
        BlinqApplication.searchSource = platform;
    }

    public int getFeedId() {
        return feedId;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    public void setMergeType(BlinqSearchHandler.SearchViewMode mergeType) {
        this.mergeType = mergeType;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.merge_contact_image:

                showLoadingProgressBar(false);
                activity.displaySocialWindowView();
                break;

            default:
                break;
        }

    }

    @Override
    public void onMergeItemSelected() {

        preferencesManager.setProperty(LAST_SEARCH_TEXT,
                String.valueOf(textViewForSearchContent.getText()));

        BlinqApplication.searchResults = searchResults;

        contactImageView.startAnimation(rotateAnimation);

        audioManager.setSoundFile(R.raw.merge);
        audioManager.play();
    }


    // ------------------------------ Callback -------------------------------

    /**
     * Used to notify container activity when merge operation done.
     */
    public interface OnMergeDone {

        /**
         * Called when merge operation on contact done. Used to reload social fragment data
         * after merge contact with new platform.
         */
        public void onMergeDone();
    }
}