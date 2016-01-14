package com.blinq.ui.activities.search;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;

import com.blinq.MeCardHolder;
import com.blinq.PreferencesManager;
import com.blinq.analytics.Analytics;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.models.Contact;
import com.blinq.models.FeedModel;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.provider.ContactsMerger;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.ui.adapters.BlinqSearchAdapter;
import com.blinq.ui.fragments.InstantMessageFragment;
import com.blinq.ui.fragments.MergeFragment;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.HeadboxPhoneUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle the main functionalities of search.
 *
 * @author Johan Hansson.
 */
public class BlinqSearchHandler {

    private static final String TAG = BlinqSearchHandler.class.getSimpleName();

    private ListView searchResultsList;
    private SearchView searchView;

    private BlinqSearchAdapter searchResultAdapter;

    private static List<SearchResult> searchResults;

    private Fragment fragment;
    private Activity activity;
    private Context context;
    private boolean isClicked;

    private SearchViewMode searchMode;

    private PreferencesManager preferencesManager;

    private Platform mergePlatform;

    private Analytics analyticsManager;

    private int feedId;

    private Provider provider;

    private OnMergeItemSelectedListener mergeItemListener;

    public BlinqSearchHandler(Activity activity, ListView searchResultsList,
                              SearchView searchView, SearchViewMode searchMode) {

        this.searchMode = searchMode;

        this.activity = activity;
        this.searchResultsList = searchResultsList;
        this.searchView = searchView;
        this.context = activity.getApplicationContext();
        this.provider = FeedProviderImpl.getInstance(context);
        setClicked(false);
        init();
    }

    public BlinqSearchHandler(Fragment fragment, ListView searchResultsList,
                              SearchView searchView, SearchViewMode searchMode, int feedId) {

        this.searchMode = searchMode;
        this.fragment = fragment;
        this.activity = fragment.getActivity();
        this.searchResultsList = searchResultsList;
        this.searchView = searchView;
        this.context = activity.getApplicationContext();
        this.provider = FeedProviderImpl.getInstance(context);
        this.feedId = feedId;
        setClicked(false);
        init();
    }

    private void init() {

        analyticsManager = new BlinqAnalytics(context);

        preferencesManager = new PreferencesManager(context);

        setListenersOnListView();
    }

    /**
     * Handle different listeners on search result list.
     */
    private void setListenersOnListView() {

        // On click listener.
        searchResultsList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg00, View arg11,
                                    int arg22, long position) {

                handleSelectedItem((int) position);
            }

        });

        // On scroll listener.
        searchResultsList.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {

                    AppUtils.hideKeyboard(activity);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

            }
        });
    }

    /**
     * Handle selected search item in general search handler.
     *
     * @param position search item index.
     */
    public void handleSelectedItem(int position) {


        if (!(position >= 0 && position < searchResults.size()))
            return;

        SearchResult selected = searchResults.get(position);

        //If the selected contact is a global result then insert to the headbox contact.
        if (selected.getSearchType() == SearchResult.SearchType.GLOBAL) {
            insertGlobalContact(selected);
        }

        if (searchMode == SearchViewMode.MERGE) {

            handleMergeSelectedItem(position);

        } else if (searchMode == SearchViewMode.REMERGE) {

            handleRemergeSelectedItem(position);

        }

    }

    /**
     * Insert the selected global contact to our headbox_contacts then get the inserted contact id
     * and update the search item model to be used in completing the merge.
     *
     * @param result - global contact
     */
    private void insertGlobalContact(SearchResult result) {

        Uri uri = provider.insertContact(result.getContact());
        String contactId = uri.getLastPathSegment();
        result.getContact().setContactId(contactId);
    }

    /**
     * Handle selected search item in merge search list.
     *
     * @param position search item index.
     */
    private void handleMergeSelectedItem(int position) {

        mergeItemListener = (OnMergeItemSelectedListener) fragment;
        mergeItemListener.onMergeItemSelected();
        startMerge(position);

        sendPersonMergedAnalytics(false);

    }

    /**
     * Handle selected search item in re-merge search list.
     *
     * @param position search item index.
     */
    private void handleRemergeSelectedItem(int position) {

        mergeItemListener = (OnMergeItemSelectedListener) fragment;
        mergeItemListener.onMergeItemSelected();

        provider.deleteMergeLinks(mergePlatform, feedId);
        startMerge(position);

        sendPersonMergedAnalytics(true);
        preferencesManager.setProperty(PreferencesManager.IS_SHOW_UNDO_MERGE, false);

    }


    /**
     * Start merge the search item with the feed
     *
     * @param position search item index.
     */
    private void startMerge(int position) {

        SearchResult selectedItem = searchResults.get(position);
        setClicked(true);
        FeedModel feed = provider.getFeed(feedId);

        boolean hasContact = feed != null
                && selectedItem.getContact() != null
                && feed.getContact() != null;

        if (hasContact) {
            Log.i(MergeFragment.TAG,
                    "Start merge for contact (Id,name) : ("
                            + selectedItem.getContact().getContactId()
                            + "," +

                            selectedItem.getContact().toString()
                            + " with contact (Id,name):( "
                            + feed.getContact().getContactId() + ","
                            + feed.getContact().toString() + ")"
            );
        }

        int secondContactFeedId = selectedItem.HasFeed() ? Integer.parseInt(selectedItem.getFeedId()) : 0;
        ContactsMerger.getInstance(context)
                .setContacts(feed.getContact().getContactId(), selectedItem.getContact().getContactId())
                .setFeeds(feed.getFeedId(), secondContactFeedId)
                .setPlatform(mergePlatform)
                .merge();

        if (hasContact) {
            Log.i(MergeFragment.TAG, "Finish merge for contact Id :"
                    + selectedItem.getContact().getContactId()
                    + " with contact Id "
                    + feed.getContact().getContactId());
        }
        AppUtils.hideKeyboard(activity);

        updateMeCardInformation(selectedItem.getContact());
    }

    private void updateMeCardInformation(Contact contact) {
        MeCardHolder meCardHolder = MeCardHolder.getInstance();

        if (StringUtils.isBlank(meCardHolder.getImagePath())) {
            meCardHolder.setImagePath(contact.getPhotoUri().toString());
        }
        String meCardName = meCardHolder.getName();
        if (StringUtils.isBlank(meCardName) || HeadboxPhoneUtils.isPhoneNumber(meCardName)) {
            meCardHolder.setName(contact.getName());
        }
        String userName = contact.getUserName();
        if (StringUtils.isBlank(userName)) {
            userName = contact.getNormalizedIdentifier().split("@")[0];
        }
        if (mergePlatform == Platform.FACEBOOK && StringUtils.isBlank(meCardHolder.getSocialProfile(Platform.FACEBOOK))) {
            meCardHolder.setSocialProfile(Platform.FACEBOOK, userName);
            preferencesManager.setLastPersonSocialProfileFromMerge(mergePlatform, feedId, userName);
        } else if (mergePlatform == Platform.TWITTER && StringUtils.isBlank(meCardHolder.getSocialProfile(Platform.TWITTER))) {
            meCardHolder.setSocialProfile(Platform.TWITTER, userName);
            preferencesManager.setLastPersonSocialProfileFromMerge(mergePlatform, feedId, userName);
        } else if (mergePlatform == Platform.INSTAGRAM && StringUtils.isBlank(meCardHolder.getSocialProfile(Platform.INSTAGRAM))) {
            meCardHolder.setSocialProfile(Platform.INSTAGRAM, userName);
            preferencesManager.setLastPersonSocialProfileFromMerge(mergePlatform, feedId, userName);
        }
    }

    private void sendPersonMergedAnalytics(boolean isRemerged) {

        analyticsManager.sendEvent(AnalyticsConstants.PERSON_MERGED_EVENT,
                AnalyticsConstants.TYPE_PROPERTY,
                mergePlatform.getName(), false,
                AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Start instants message activity with specific feed id.
     *
     * @param selectedFeedId feed id of the selected item in the search list.
     */
    private void startPopupSocialWindowActivity(int selectedFeedId) {

        activity.finish();
        Intent intent = new Intent(context,
                PopupSocialWindow.class);
        intent.putExtra(Constants.FEED_ID, selectedFeedId);
        intent.putExtra(InstantMessageFragment.SHOW_KEYBOARD,
                true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    /**
     * Set the adapter for search result list.
     */
    private void setSearchAdapter(List<SearchResult> searchResultMap,
                                  String query) {

        searchResultAdapter = new BlinqSearchAdapter(context, searchResults,
                searchMode, mergePlatform, this);

        searchResultAdapter.setSearchQuery(query.toLowerCase());
        searchResultsList.setAdapter(searchResultAdapter);
    }

    /**
     * Asynchronous task for applying search functionality in the background
     * then update list view contents.
     *
     * @author Johan Hansson
     */
    public class SearchResultHandler extends
            AsyncTask<String, String, List<SearchResult>> {

        @Override
        protected List<SearchResult> doInBackground(String... params) {

            //We might get null response so lets bind an empty array.
            if (searchResults == null)
                searchResults = new ArrayList<SearchResult>();

            return searchResults;

        }

        @Override
        protected void onPostExecute(List<SearchResult> searchResult) {
            super.onPostExecute(searchResult);

            setSearchAdapter(searchResult, searchView.getQuery().toString());

        }

    }

    /**
     * @return list of search items.
     */
    public List<SearchResult> getSearchResults() {
        return searchResults;
    }

    public void setPlatform(Platform mergePlatform) {
        this.mergePlatform = mergePlatform;
    }

    /**
     * @param searchResults list of search items.
     */
    public void setSearchResults(List<SearchResult> searchResults) {
        BlinqSearchHandler.searchResults = searchResults;
    }

    public boolean isClicked() {
        return isClicked;
    }

    public void setClicked(boolean isClicked) {
        this.isClicked = isClicked;
    }

    /**
     * Refresh the search list contents.
     */
    public void refresh() {

        if (searchResultAdapter != null
                && preferencesManager.getProperty(
                PreferencesManager.IS_THEME_CHANGED, false)) {

            searchResultAdapter.notifyDataSetChanged();
        }

    }

    /**
     * An inner interface which the fragment must implement in order to handle
     * clicks from merge list items.
     *
     * @author Johan Hansson
     */
    public interface OnMergeItemSelectedListener {

        /**
         * Called when the merge list item selected.
         */
        public void onMergeItemSelected();

    }


    /**
     * An Inner enum defines modes to manage the search view's appearance and functionalities.
     *
     * @author Johan Hansson.
     */
    public static enum SearchViewMode {

        GENERAL(1), //opened to start the default search [feeds view].
        MERGE(2), // Opened to merge contact [Merge screen]
        REMERGE(3); // Opened to re merge contact. [Merge screen]

        private int id;

        private SearchViewMode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static SearchViewMode fromId(int id) {
            for (SearchViewMode e : SearchViewMode.values()) {
                if (e.getId() == id)
                    return e;
            }
            return SearchViewMode.MERGE;
        }

    }
}