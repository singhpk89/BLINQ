package com.blinq.authentication.impl.Twitter.actions;

import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.impl.Twitter.TwitterAuthenticator;
import com.blinq.utils.Log;

import twitter4j.PagableResponseList;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Twitter Base actions-handler. other sub handlers must extends this class
 * in order to manage the process of requesting and converting the response of twitter API.
 *
 * @param <T> - Represents the converted response.
 * @param <Y> - Represents the response of twitter api.
 */
public class TwitterActionImpl<T, Y> extends Action<T> {

    protected Page<T, Y> page = null;
    protected long me;
    protected Twitter twitter;
    private int maxPages;
    private int resultsPerPage;

    Thread callBack = new Thread(new Runnable() {
        @Override
        public void run() {
            if (getActionListener() != null) {
                getActionListener().onLoading();
            }
            runRequest();
        }
    });

    public TwitterActionImpl(Authenticator authenticator) {
        super(authenticator);
        page = new Page<T, Y>(TwitterActionImpl.this);
    }

    @Override
    protected void executeImpl() {

        OnActionListener<T> actionListener = getActionListener();
        if (authenticator.isConnected()) {
            twitter = ((TwitterAuthenticator) authenticator).createTwitter();
            startCallBack();
        } else {
            String reason = "Authentication Error";
            Log.e(TwitterActionImpl.class.getSimpleName(), reason);
            if (actionListener != null) {
                actionListener.onFail(reason);
            }
        }
    }

    private void startCallBack() {
        if (requestType == RequestType.ASYNC) {
            callBack.start();
        } else {
            runRequest();
        }
    }

    @Override
    protected void cancelImpl() {
        callBack.interrupt();
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }

    void runRequest() {

        OnActionListener<T> actionListener = getActionListener();
        try {

            me = twitter.verifyCredentials().getId();
            Y response = (Y) getPageResponse();
            T result = processResponse(response);

            updateCursor(response);

            if (requestType == RequestType.ASYNC) {
                actionListener.onComplete(result);
            } else {
                this.result = result;
            }

        } catch (TwitterException e) {
            if (actionListener != null)
                actionListener.onException(e);
        }
    }

    private void updateCursor(Y response) {
        if (response instanceof PagableResponseList) {
            updateCursor((PagableResponseList) response);
        } else if (response instanceof ResponseList) {
            updateCursor((ResponseList) response);
        }
        if (mOnActionListener != null)
            mOnActionListener.setCursor(page);
    }

    private void updateCursor(ResponseList response) {

        if (mOnActionListener == null && requestType == RequestType.ASYNC) {
            return;
        }

        if (page == null) {
            page = new Page<T, Y>(TwitterActionImpl.this);
        }

        long pageNum = page.getPageNum();

        page.setToNextPage();
        page.setNextPage(++pageNum);
        page.setPrevPage(--pageNum);

    }

    /**
     * set next and prev pages requests
     *
     * @param response
     */
    private void updateCursor(PagableResponseList response) {

        if (mOnActionListener == null && requestType == RequestType.ASYNC) {
            return;
        }

        if (page == null) {
            page = new Page<T, Y>(TwitterActionImpl.this);
        }

        long next = response.getNextCursor();

        page.setNextPage(next);

        long prev = response.getPreviousCursor();

        page.setPrevPage(prev);

    }

    protected T processResponse(Y object) {
        return null;
    }

    protected Y getPageResponse() throws TwitterException {
        return null;
    }

}
