package com.blinq.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import com.blinq.models.Platform;
import com.blinq.ui.activities.feed.FeedActivity;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides the functionalities for contacts search.
 *
 * @author Johan Hansson.
 */
public class SearchProvider extends ContentProvider {

    private static final String TAG = SearchProvider.class.getSimpleName();
    private String previousSearchQuery = "";

    public static String AUTHORITY = "com.blinq.provider.SearchProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/search");

    private static final int SEARCH_ACTIVITY = 0;
    private static final int SEARCH_ACTION = 1;

    private static final UriMatcher searchURIMatcher = buildUriMatcher();

    private Cursor cursor = null;

    /**
     * Builds up a UriMatcher for search for filtering the URIs.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, "search", SEARCH_ACTIVITY);

        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
                SEARCH_ACTION);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                SEARCH_ACTION);

        return matcher;
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projectin, String selection,
                        String[] selectionArgs, String sortOrder) {

        switch (searchURIMatcher.match(uri)) {
            // When SearchActivity requests the query.
            case SEARCH_ACTIVITY:

                return cursor;

            // When search action occurs.
            case SEARCH_ACTION:
                if (selectionArgs != null && selectionArgs.length > 0
                        && selectionArgs[0] != null) {
                    triggerSearch(selectionArgs[0]);
                } else {
                    triggerSearch("");
                }

                break;
        }

        return null;
    }

    /**
     * Perform the search query . When done trigger the SearchActivity .
     *
     * @param searchQuery - name entered by user from the searchView.
     */
    private void triggerSearch(final String searchQuery) {

        if (StringUtils.isBlank(searchQuery)) {
            // clear the search view if the user cancel the search.
            cursor = null;
            triggerSearchActivity();

        } else if (!searchQuery.equals(previousSearchQuery)) {

            startSearch(searchQuery);
        }
    }

    private void startSearch(String searchQuery) {

        previousSearchQuery = searchQuery;
        if (searchQuery.contains("'")) {

            searchQuery.replaceAll("'", "");
        }

        Uri uri = Uri.withAppendedPath(FeedProvider.CONTACTS_SEARCH_URI,
                Platform.OTHER.getId() + "");
        uri = Uri.withAppendedPath(uri, searchQuery);
        cursor = getContext().getContentResolver().query(uri, null, null,
                new String[]{searchQuery}, null);
        triggerSearchActivity();

    }

    /**
     * Trigger the SearchActivity with an ACTION_SAERCH to take the query and
     * refresh.
     */
    private void triggerSearchActivity() {
        Intent intent = new Intent(getContext(), FeedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_SEARCH);
        //getContext().startActivity(intent);
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }

}
