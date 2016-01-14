package com.blinq.authentication.impl.Instagram.Actions;

import com.blinq.models.Contact;
import com.blinq.models.SearchResult;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Instagram.Mappers.ContactsMapper;
import com.blinq.authentication.impl.onSearchAction;
import com.blinq.authentication.settings.InstagramSettings;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Make a call to Instagram search endpoint
 *
 * For example:
 * https://api.instagram.com/v1/users/search?count=20&q=sean%20&access_token=15161738.350d0a7.b7f86ca1c5e9459890dee2baa287045e
 */
public class SearchUserAction extends InstagramActionImpl<List<SearchResult>, JSONObject> implements onSearchAction {

    private String queryString;

    public SearchUserAction(Authenticator authenticator) {
        super(authenticator);
    }

    @Override
    public void setQueryString(String queryString) {
        //turn spaces to '+' to bypass bugs in their api
        if(queryString != null) {
            queryString = queryString.replace(' ', '+');
        }
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
    protected List<SearchResult> processResponse(JSONObject response) throws JSONException {

        if (response == null)
            return null;

        JSONArray usersArray = response.getJSONArray(InstagramSettings.INSTAGRAM_DATA);

        List<SearchResult> searchResults = new ArrayList<SearchResult>();

        if (usersArray == null)
            return null;

        for (int index = 0; index < usersArray.length(); index++) {

            SearchResult searchResult = new SearchResult();
            Contact contact = ContactsMapper.convert(usersArray.getJSONObject(index));
            searchResult.setContact(contact);
            searchResult.setSearchType(SearchResult.SearchType.GLOBAL);

            if (contact != null) {
                searchResults.add(searchResult);
            }
        }

        return searchResults;
    }

    @Override
    protected void initializeRequest() {

        super.initializeRequest();

        BasicNameValuePair countPair
                = new BasicNameValuePair(InstagramSettings.INSTAGRAM_COUNT, String.valueOf(getLimit()));
        BasicNameValuePair queryString
                = new BasicNameValuePair(InstagramSettings.INSTAGRAM_USERS_SEARCH_QUERY_STRING, getQueryString());
        requestParameters = new ArrayList<NameValuePair>();
        requestParameters.add(countPair);
        requestParameters.add(queryString);

    }


    @Override
    protected String getUrl() {
        return InstagramSettings.INSTAGRAM_USERS_SEARCH_ENDPOINT;
    }

    @Override
    protected JSONObject getResponse() throws Exception {
        return new JSONObject(request.createRequest(RequestMethod.GET.name(),
                getUrl(), requestParameters));
    }

    @Override
    protected void updateCursor(JSONObject response) throws JSONException {

        super.updateCursor(response);

        if (response.isNull(InstagramSettings.INSTAGRAM_PAGINATION)) {
            page.setNextPage(0);
            return;
        }
        JSONObject pagination = response.getJSONObject(InstagramSettings.INSTAGRAM_PAGINATION);

        // check for more pages
        if (pagination.isNull(InstagramSettings.INSTAGRAM_NEXT_MAX_ID)) {
            page.setNextPage(0);
            return;
        }

        String nextMaxId = pagination.getString(InstagramSettings.INSTAGRAM_NEXT_MAX_ID);
        if (nextMaxId.isEmpty()) {
            page.setNextPage(0);
            return;
        }

        requestParameters = new ArrayList<NameValuePair>();
        requestParameters.add(new BasicNameValuePair(InstagramSettings.INSTAGRAM_MAX_ID, nextMaxId));
        BasicNameValuePair countParameter =
                new BasicNameValuePair(InstagramSettings.INSTAGRAM_COUNT, String.valueOf(getLimit()));
        requestParameters.add(countParameter);

    }

}
