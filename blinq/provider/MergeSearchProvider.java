package com.blinq.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import com.blinq.BlinqApplication;
import com.blinq.provider.HeadboxFeed.HeadboxContacts;
import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.ui.activities.instantmessage.InstantMessageActivity;

/**
 * Provides the functionalities for contacts search.
 *
 * @author Johan Hansson.
 */
public class MergeSearchProvider extends ContentProvider {

    private static final String TAG = MergeSearchProvider.class.getSimpleName();
    private String previousSearchQuery = "";

    public static String AUTHORITY = "com.blinq.provider.MergeSearchProvider";
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
                        String[] selectionArgs, String platform) {

        switch (searchURIMatcher.match(uri)) {
            // When SearchActivity requests the query.
            case SEARCH_ACTIVITY:

                return cursor;

            // When search action occurs.
            case SEARCH_ACTION:
                if (selectionArgs != null && selectionArgs.length > 0
                        && selectionArgs[0] != null) {
                    triggerSearch(selectionArgs[0], platform);
                } else {
                    triggerSearch("", "");
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
    private void triggerSearch(String searchQuery, String platform) {

        if (!searchQuery.equals(previousSearchQuery)) {
            previousSearchQuery = searchQuery;
            if (searchQuery.contains("'")) {

                searchQuery.replaceAll("'", "");
            }

            if (BlinqApplication.searchSource != null) {

                String where = HeadboxDatabaseHelper.TABLE_HEADBOX_CONTACTS
                        + "." + HeadboxContacts.CONTACT_ID + " NOT IN ('"
                        + BlinqApplication.contactId + "')";

                Uri uri = Uri.withAppendedPath(
                        FeedProvider.CONTACTS_SEARCH_URI,
                        BlinqApplication.searchSource.getId() + "");
                uri = Uri.withAppendedPath(uri, searchQuery);
                cursor = getContext().getContentResolver().query(uri, null,
                        where, new String[]{searchQuery}, null);
                triggerSearchActivity();
            }

        }
    }


    private void triggerSearchActivity() {
        if (BlinqApplication.searchType == SearchActivityType.INSTANT) {
            triggerInstantMessageSearchActivity();
        } else {
            triggerBlinqPopSearchActivity();
        }
    }

    /**
     * Trigger the SearchActivity with an ACTION_SEARCH to take the query and
     * refresh.
     */
    private void triggerBlinqPopSearchActivity() {
        Intent intent = new Intent(getContext(), PopupSocialWindow.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(Intent.ACTION_SEARCH);
        getContext().startActivity(intent);
    }

    private void triggerInstantMessageSearchActivity() {
        Intent intent = new Intent(getContext(), InstantMessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(Intent.ACTION_SEARCH);
        getContext().startActivity(intent);
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }


    public enum SearchActivityType {
        POPUP, INSTANT
    }

}
