package com.blinq.ui.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinq.ImageLoaderManager;
import com.blinq.R;
import com.blinq.analytics.Analytics;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.models.FeedDesign;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.ui.activities.search.BlinqSearchHandler;
import com.blinq.utils.StringUtils;

import java.util.List;

public class BlinqSearchAdapter extends HeadboxBaseAdapter {

    private static final String TAG = BlinqSearchAdapter.class.getSimpleName();

    private int matchedTextColor;

    private Context context;
    private LayoutInflater layoutInflater;

    private List<SearchResult> searchResults;

    private BlinqSearchHandler.SearchViewMode searchMode;
    private Platform mergePlatform;

    /**
     * Search query taken from search bar, used to color part of contact name
     * which matched with search query.
     */
    private String searchQuery;

    private Analytics analyticsManager;

    private BlinqSearchHandler searchHandler;

    private ImageLoaderManager imageLoaderManager;

    public BlinqSearchAdapter(Context context, List<SearchResult> searchResults,
                              BlinqSearchHandler.SearchViewMode handlerType, Platform mergePlatform, BlinqSearchHandler searchHandler) {
        super();

        this.searchResults = searchResults;
        this.context = context;
        this.searchHandler = searchHandler;
        this.mergePlatform = mergePlatform;
        this.searchMode = handlerType;

        init();
    }

    /**
     * Initialize components.
     */
    private void init() {

        analyticsManager = new BlinqAnalytics(context);

        this.matchedTextColor = context.getResources().getColor(
                R.color.search_matched_text);

        imageLoaderManager = new ImageLoaderManager(context);

        layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return searchResults.size();
    }

    @Override
    public Object getItem(int position) {
        return searchResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {

            convertView = layoutInflater.inflate(R.layout.search_item_layout,
                    parent, false);

            viewHolder = new ViewHolder();

            viewHolder.searchView = convertView;
            viewHolder.contactImageView = (ImageView) convertView
                    .findViewById(R.id.imageViewForSearchSender);
            viewHolder.contactNameTextView = (TextView) convertView
                    .findViewById(R.id.textViewForSearchSenderName);
            viewHolder.messageTimeTextView = (TextView) convertView
                    .findViewById(R.id.textViewForSearchMessageTimeAgo);
            viewHolder.lastUsedPlatformImageView = (ImageView) convertView
                    .findViewById(R.id.imageViewForLastUsedPlatform);
            viewHolder.searchSeparatorTextView = (TextView) convertView
                    .findViewById(R.id.textViewForSearchSeparator);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        SearchResult item = searchResults.get(position);

        // Contact image.
        Uri imageUri = item.getContact().getPhotoUri();

        if (imageUri != null && !StringUtils.isBlank(imageUri.toString())) {

            imageLoaderManager.loadImage(viewHolder.contactImageView, imageUri);

        } else {

            imageLoaderManager.loadImage(viewHolder.contactImageView,
                    Uri.parse(""));
        }

        setListenersOnContactAvatar(viewHolder.contactImageView, position);

        // Contact name.
        viewHolder.contactNameTextView.setText(colorMatchedText(item
                .getContact().getName()));

        // Message time.
        viewHolder.messageTimeTextView.setText(StringUtils
                .normalizeDifferenceDate(item.getLastMessageTime()));

        // Platform icon.
        viewHolder.lastUsedPlatformImageView
                .setBackgroundResource(getIconToShowInFeed(item));

        setSearchItemDesign(viewHolder);

        return convertView;
    }

    /**
     * Work as holder for search view in order to decrease the time of creating
     * list-view items.
     *
     * @author Johan Hansson.
     */
    public static class ViewHolder {

        View searchView;
        public ImageView contactImageView;
        public TextView contactNameTextView;
        public TextView messageTimeTextView;
        public ImageView lastUsedPlatformImageView;
        public TextView searchSeparatorTextView;
    }

    /**
     * Return the icon of last platform from resources depends on last message
     * platform in the feed if its general search and depend on chosen platform
     * if it merge search.
     *
     * @param searchItem search item model.
     */
    private int getIconToShowInFeed(SearchResult searchItem) {

        Platform lastMessagePlatform = Platform.NOTHING;

        if (searchMode == BlinqSearchHandler.SearchViewMode.GENERAL) {

            lastMessagePlatform = searchItem.getLastMessagePlatform();

        } else if (searchMode == BlinqSearchHandler.SearchViewMode.MERGE
                || searchMode == BlinqSearchHandler.SearchViewMode.REMERGE) {

            lastMessagePlatform = mergePlatform;

        }

        if (lastMessagePlatform != null) {

            return getPlatformIcon(lastMessagePlatform, searchItem.getLastCallType());
        }

        return 0;
    }


    /**
     * Color part of the given text depends on the search query.
     *
     * @param text text to color part of.
     * @return Spannable String with matched part colored.
     */
    private Spannable colorMatchedText(String text) {

        String searchText = this.searchQuery;
        String textLower = text.toLowerCase();

        int matchedQueryIndex = textLower.indexOf(searchText);

        // Check if the search query not in the start of the name.
        if (matchedQueryIndex != 0) {

            // Add space to the first of query to search for query in the
            // (middle & last name).
            searchText = " " + searchText;

            matchedQueryIndex = textLower.indexOf(searchText);

        }

        Spannable spannableOfText = new SpannableString(text);

        if (matchedQueryIndex >= 0 && matchedQueryIndex < text.length()) {

            spannableOfText.setSpan(new ForegroundColorSpan(matchedTextColor),
                    matchedQueryIndex, matchedQueryIndex + searchText.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }

        return spannableOfText;
    }

    /**
     * Set the design of search layout depends on the application mode.
     *
     * @param viewHolder holder for search list item layout components.
     */
    private void setSearchItemDesign(ViewHolder viewHolder) {

        FeedDesign modeDesign = FeedDesign.getInstance();

        viewHolder.searchView.setBackgroundResource(modeDesign
                .getFeedBodySelector());

        viewHolder.contactNameTextView.setTextColor(context.getResources()
                .getColorStateList(modeDesign.getContactNameTextSelectorRead()));

        viewHolder.contactNameTextView.setTypeface(modeDesign
                .getSenderTypefaceRead());

        viewHolder.searchSeparatorTextView.setBackgroundColor(modeDesign
                .getFeedSeparatorColor());
    }

    /**
     * Set different listeners on the contact avatar image view.
     *
     * @param avatarImageView contact avatar image view.
     * @param position        feed index.
     */
    private void setListenersOnContactAvatar(View avatarImageView,
                                             final int position) {

        // Set on click listener.
        avatarImageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                // Apply the same action as if search item is selected.
                searchHandler.handleSelectedItem(position);

            }
        });

        // Set on long click listener
        avatarImageView.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                // Send search analytics
                analyticsManager.sendEvent(
                        AnalyticsConstants.LONG_CLICKED_RESULT_SEARCH_EVENT,
                        false, AnalyticsConstants.ACTION_CATEGORY);

                return true;
            }
        });

    }

    /**
     * @param searchQuery search query assigned with the adapter.
     */
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

}
