package com.blinq.authentication.impl.Twitter.actions;

import com.blinq.models.Contact;
import com.blinq.models.SearchResult;
import com.blinq.authentication.impl.Twitter.Mappers.UsersMapper;
import com.blinq.authentication.impl.Twitter.TwitterAuthenticator;
import com.blinq.authentication.impl.onSearchAction;

import java.util.ArrayList;
import java.util.List;

import twitter4j.ResponseList;
import twitter4j.TwitterException;
import twitter4j.User;

public class SearchUserAction extends TwitterActionImpl<List<SearchResult>, ResponseList<User>> implements onSearchAction {

    private static final String TAG = SearchUserAction.class.getSimpleName();

    /**
     * User to be searched on twitter.
     */
    private String queryString;

    public SearchUserAction(TwitterAuthenticator twitterAuthenticator) {

        super(twitterAuthenticator);
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
    protected ResponseList<User> getPageResponse() throws TwitterException {
        return twitter.searchUsers(getQueryString(), (int) page.getPageNum());
    }

    @Override
    protected List<SearchResult> processResponse(ResponseList<User> users) {

        List<SearchResult> results = new ArrayList<SearchResult>();

        for (User user : users) {

            SearchResult result = new SearchResult();
            Contact contact = UsersMapper.create(user);

            if (contact == null)
                continue;

            result.setContact(contact);
            result.setSearchType(SearchResult.SearchType.GLOBAL);
            results.add(result);
        }

        return results;
    }

}