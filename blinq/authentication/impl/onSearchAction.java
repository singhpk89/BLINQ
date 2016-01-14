package com.blinq.authentication.impl;

/**
 * Provide search functionalities that should be implemented by the search providers.
 * </p>
 * Implemented by :
 * <ul/>
 * <li> {@link com.blinq.authentication.impl.facebook.Actions.SearchUserAction}</li>
 * <li> {@link com.blinq.authentication.impl.Twitter.actions.SearchUserAction}</li>
 * <ul/>
 * <p/>
 * Created by Johan Hansson on 10/1/2014.
 */
public interface onSearchAction {

    public void setQueryString(String queryString);

    public String getQueryString();

    public void startSearch();

    public void stopSearch();

}
