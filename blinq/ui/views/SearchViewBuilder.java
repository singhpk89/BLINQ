package com.blinq.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Search View customizer
 * Created by Johan Hansson on 10/2/2014.
 */
public class SearchViewBuilder {


    private static final String TAG = SearchViewBuilder.class.getSimpleName();

    /**
     * SEARCH VIEW COMPONENTS HIERARCHY INDICES
     */
    public static final int SEARCH_PARENT_LAYOUT = 0;
    public static final int SEARCH_EDIT_FRAME = 2;
    public static final int SEARCH_APP_ICON = 0;
    public static final int SEARCH_PLATE_FRAME = 0;
    public static final int SEARCH_TEXT_VIEW = 0;
    public static final int SEARCH_CLOSE_BUTTON = 1;
    public static final String SEARCH_VIEW_TEXT_ID = "android:id/search_src_text";
    public static final String CURSOR_RESOURCE_FIELD_NAME = "mCursorDrawableRes";

    private Context context;

    /**
     * Search view to be customized.
     */
    private SearchView searchView;

    /**
     * Custom drawable for the cancel button.
     */
    private int cancelImageViewResourceId;

    /**
     * The action that should be applied when clicking the edit text X button.
     */
    private View.OnClickListener cancelButtonOnClickListener;

    private int searchPlateBackgroundResourceId;

    private int searchBarTextColorResourceId;

    /**
     * The hint text to display in the query text field.
     */
    private String searchQueryHint;

    private TextView textViewForSearchContent;

    private android.graphics.Typeface textContentTypeFace;

    private Object searchContentTextCursorResource;

    public SearchViewBuilder setContext(Context context) {
        this.context = context;
        return this;
    }

    public String getSearchQueryHint() {
        return searchQueryHint;
    }

    public SearchViewBuilder setSearchQueryHint(String searchQueryHint) {
        this.searchQueryHint = searchQueryHint;
        return this;
    }

    public int getSearchPlateBackgroundResourceId() {
        return searchPlateBackgroundResourceId;
    }

    public SearchViewBuilder setSearchPlateBackgroundResourceId(int searchPlateBackgroundResourceId) {
        this.searchPlateBackgroundResourceId = searchPlateBackgroundResourceId;
        return this;
    }

    public int getSearchBarTextColorResourceId() {
        return searchBarTextColorResourceId;
    }

    public SearchViewBuilder setSearchBarTextColorResourceId(int searchBarTextColorResourceId) {
        this.searchBarTextColorResourceId = searchBarTextColorResourceId;
        return this;
    }

    public View.OnClickListener getCancelButtonOnClickListener() {
        return cancelButtonOnClickListener;
    }

    public SearchViewBuilder setCancelButtonOnClickListener(View.OnClickListener cancelButtonOnClickListener) {
        this.cancelButtonOnClickListener = cancelButtonOnClickListener;
        return this;
    }

    public int getCancelImageViewResourceId() {
        return cancelImageViewResourceId;
    }

    public SearchViewBuilder setCancelImageViewResourceId(int cancelImageViewResourceId) {
        this.cancelImageViewResourceId = cancelImageViewResourceId;
        return this;
    }

    public SearchView getSearchView() {
        return searchView;
    }

    public SearchViewBuilder setSearchView(SearchView searchView) {
        this.searchView = searchView;
        return this;
    }

    public TextView getTextViewForSearchContent() {
        return textViewForSearchContent;
    }

    public Typeface getTextContentTypeFace() {
        return textContentTypeFace;
    }

    public SearchViewBuilder setTextContentTypeFace(Typeface textContentTypeFace) {
        this.textContentTypeFace = textContentTypeFace;
        return this;
    }

    public SearchViewBuilder setSearchContentTextCursorResource(Object searchContentTextCursorResource) {
        this.searchContentTextCursorResource = searchContentTextCursorResource;
        return this;
    }

    public SearchViewBuilder() {
    }

    public SearchViewBuilder build() {

        LinearLayout parentFrame = (LinearLayout) getSearchView().getChildAt(SEARCH_PARENT_LAYOUT);
        LinearLayout searchEditFrame = (LinearLayout) parentFrame.getChildAt(SEARCH_EDIT_FRAME);

        // Change hint.
        getSearchView().setQueryHint(getSearchQueryHint());


        try {
            // Customize the text view cursor.
            final int textViewId = getSearchView().getContext().getResources().getIdentifier(SEARCH_VIEW_TEXT_ID, null, null);
            final AutoCompleteTextView searchTextView = (AutoCompleteTextView) getSearchView().findViewById(textViewId);
            Field cursorResource = TextView.class.getDeclaredField(CURSOR_RESOURCE_FIELD_NAME);
            cursorResource.setAccessible(true);
            cursorResource.set(searchTextView, searchContentTextCursorResource);

        } catch (Exception e) {
            Log.d(TAG, "Error while parsing the search view cursor.");
        }

        searchEditFrame.removeView(searchEditFrame.getChildAt(SEARCH_APP_ICON));

        LinearLayout searchPlate = (LinearLayout) searchEditFrame.getChildAt(SEARCH_PLATE_FRAME);

        // Cancel search image view.
        ImageView cancelSearchImageView = ((ImageView) searchPlate.getChildAt(SEARCH_CLOSE_BUTTON));
        cancelSearchImageView.setImageResource(getCancelImageViewResourceId());
        cancelSearchImageView.setBackgroundResource(0);

        if (getCancelButtonOnClickListener() != null)
            cancelSearchImageView.setOnClickListener(getCancelButtonOnClickListener());

        searchPlate.setBackgroundResource(getSearchPlateBackgroundResourceId());

        textViewForSearchContent = (TextView) searchPlate.getChildAt(SEARCH_TEXT_VIEW);
        textViewForSearchContent.setTextColor(context.getResources().getColor(getSearchBarTextColorResourceId()));
        textViewForSearchContent.setTypeface(getTextContentTypeFace());
        textViewForSearchContent.requestFocus();

        return this;
    }
}
