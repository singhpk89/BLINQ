package com.blinq.authentication.impl.facebook.Actions;

import android.os.Bundle;

import com.facebook.Response;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.blinq.models.SearchResult;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.authentication.impl.facebook.Mappers.ProfileMapper;
import com.blinq.authentication.impl.onSearchAction;

import java.util.ArrayList;
import java.util.List;

public class SearchUserAction extends FacebookActionImpl<List<SearchResult>> implements onSearchAction {


    private static final String TAG = SearchUserAction.class.getSimpleName();
    /**
     * user to be searched on facebook search graph.
     */
    private String queryString;

    public SearchUserAction(FacebookAuthenticator facebookAuthenticator) {

        super(facebookAuthenticator);
    }

    @Override
    protected String getGraphPath() {
        return ProfileMapper.Properties.Search.PATH;
    }

    @Override
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public void startSearch() {
        execute();
    }

    @Override
    public void stopSearch() {
        cancel();
    }

    @Override
    protected List<SearchResult> processResponse(Response response) {

        List<GraphUser> graphUsers = FacebookUtils.mappedListFromResponse(
                response, GraphUser.class);

        List<SearchResult> searchResults = new ArrayList<SearchResult>();

        for (GraphObject graphUser : graphUsers) {

            SearchResult result = new SearchResult();
            result.setContact(ProfileMapper.create(graphUser));
            result.setSearchType(SearchResult.SearchType.GLOBAL);
            searchResults.add(result);
        }

        return searchResults;
    }

    @Override
    protected Bundle getBundle() {

        Bundle bundle = FacebookUtils.buildSearchBundle(getQueryString(),
                ProfileMapper.Properties.Search.TYPE_USER, getLimit());
        return bundle;
    }
}