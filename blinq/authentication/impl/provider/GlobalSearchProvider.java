package com.blinq.authentication.impl.provider;

import android.app.Activity;

import com.blinq.models.Platform;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.impl.onSearchAction;

/**
 * Contains a list of search functionalities that provide the ability to search the supported platforms.
 * <p/>
 * Created by Johan Hansson on 9/30/2014.
 */
public class GlobalSearchProvider {

    /**
     * The target activity.
     */
    private final Activity activity;

    /**
     * The target platform.
     */
    private final Platform platform;


    /**
     * Responds to the search state.
     * to notify the targeted activity or fragment.
     */
    private final OnActionListener onActionListener;

    /**
     * Manage executing and converting the search results.
     */
    private onSearchAction searchAction;

    /**
     * User to search for.
     */
    private String queryString;

    /**
     * The max results that should be returned.
     */
    private final int limit;


    public GlobalSearchProvider(Activity activity, OnActionListener onActionListener, Platform platform, int limit) {

        this.activity = activity;
        this.platform = platform;
        this.onActionListener = onActionListener;
        this.limit = limit;
        initializeAction();
    }

    /**
     * Initialize the search provider's action to a certain platform.
     */
    private void initializeAction() {

        Authenticator authenticator = AuthUtils.getAuthInstance(platform, activity);
        if (authenticator == null)
            return;

        searchAction = (onSearchAction) authenticator.search(onActionListener, queryString, limit);
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getQueryString() {
        return queryString;
    }

    public onSearchAction getSearchAction() {
        return searchAction;
    }

    /**
     * To execute the search action.
     */
    public void execute(String queryString) {

        if (searchAction == null)
            return;

        if (platform == Platform.INSTAGRAM || platform == Platform.TWITTER) {
            //TODO:Temporary until fixing the thread issue.
            initializeAction();
        }

        searchAction.stopSearch();
        searchAction.setQueryString(queryString);
        searchAction.startSearch();
    }

    /**
     * To stop the search action.
     */
    public void cancel() {

        searchAction.stopSearch();

    }


}
