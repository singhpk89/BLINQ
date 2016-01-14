package com.blinq.authentication.impl.facebook.Actions;

import android.os.Bundle;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.RequestBatch;
import com.facebook.Response;
import com.facebook.Response.PagingDirection;
import com.facebook.Session;
import com.blinq.HeadboxAccountsManager;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.utils.Log;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Facebook Base actions-handler. other sub handlers must extends this class
 * in order to manage the process of requesting and converting the response of Facebook SDK.
 *
 * @param <T> - Represents the converted response.
 */
public class FacebookActionImpl<T> extends Action<T> {

    private int limit = 10; // default
    protected Page<T> page = null;
    protected String me = null;
    public static final String LIMIT_FIELD = "limit";
    protected T result;

    private Request.Callback mCallback = new Request.Callback() {
        @Override
        public void onCompleted(Response response) {
            final OnActionListener<T> actionListener = getActionListener();
            FacebookRequestError error = response.getError();
            if (error != null) {
                Log.e(FacebookActionImpl.class.getSimpleName(),
                        "Failed to get what you have requested",
                        error.getException());
                if (actionListener != null) {
                    actionListener.onException(error.getException());
                }
            } else {
                if (response.getGraphObject() == null) {
                    Log.e(FacebookActionImpl.class.getSimpleName(),
                            "The response GraphObject has null value. Response="
                                    + response.toString(), null
                    );
                } else {
                    if (actionListener != null) {
                        try {
                            updateCursor(response);
                            T result = processResponse(response);
                            actionListener.onComplete(result);
                        } catch (Exception e) {
                            actionListener.onException(e);
                        }
                    }
                }
            }
        }
    };

    private Request.Callback mSyncCallback = new Request.Callback() {
        @Override
        public void onCompleted(Response response) {
            FacebookRequestError error = response.getError();
            if (error != null) {
                Log.e(FacebookActionImpl.class.getSimpleName(),
                        "Failed to get what you have requested",
                        error.getException());
                result = null;

            } else {
                if (response.getGraphObject() == null) {
                    Log.e(FacebookActionImpl.class.getSimpleName(),
                            "The response GraphObject has null value. Response="
                                    + response.toString(), null
                    );
                } else {

                    try {
                        updateCursor(response);
                        result = processResponse(response);
                    } catch (Exception e) {

                    }
                }
            }
        }
    };

    public FacebookActionImpl(FacebookAuthenticator sessionManager) {
        super(sessionManager);
        me = HeadboxAccountsManager.getInstance().
                getAccountsByType(HeadboxAccountsManager.AccountType.FACEBOOK)
                .name;
    }

    public void setLimit(int limit) {
        if (limit != 0)
            this.limit = limit;
    }

    public Page<T> getPage() {
        return page;
    }

    @Override
    protected void executeImpl() {
        OnActionListener<T> actionListener = getActionListener();
        if (authenticator.isConnected()) {
            Session session = ((FacebookAuthenticator) authenticator).getActiveSession();
            Bundle bundle = getBundle();
            Request request = new Request(session, getGraphPath(), bundle,
                    HttpMethod.GET);
            runRequest(request);
        } else {
            String reason = "Unknown Error";
            Log.e(FacebookActionImpl.class.getSimpleName(), reason);
            if (actionListener != null) {
                actionListener.onFail(reason);
            }
        }
    }

    @Override
    protected void cancelImpl() {
    }

    /**
     * Represent the Page,user,entity,comment,conversation id.
     *
     * @return
     */
    protected String getTarget() {
        return mEntity;
    }

    public String getMe() {
        return me;
    }

    protected String getGraphPath() {

        return mEntity;
    }

    public T getResult() {
        return result;
    }

    protected Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("date_format", "U");
        return bundle;
    }

    protected int getLimit() {
        return limit;
    }

    protected T processResponse(Response response) {
        Type type = mOnActionListener.getGenericType();
        return FacebookUtils.convert(response, type);
    }


    void runRequest(Request request) {

        if (requestType == RequestType.ASYNC) {

            OnActionListener<T> actionListener = getActionListener();
            request.setCallback(mCallback);
            RequestAsyncTask task = new RequestAsyncTask(request);
            task.execute();
            if (actionListener != null) {
                actionListener.onLoading();
            }
        } else {
            runRequestSync(request);
        }
    }

    void runRequestSync(Request request) {

        request.setCallback(mSyncCallback);
        RequestBatch task = new RequestBatch(request);
        List<Response> response = task.executeAndWait();
        updateCursor(response.get(0));
        result = processResponse(response.get(0));
    }

    /**
     * set next and prev pages requests
     *
     * @param response
     */
    private void updateCursor(Response response) {
        if (mOnActionListener == null) {
            return;
        }

        if (page == null) {
            page = new Page<T>(FacebookActionImpl.this);
        }

        Request next = response
                .getRequestForPagedResults(PagingDirection.NEXT);
        if (next != null) {
            next.setCallback(mCallback);
        }
        page.setNextPage(next);

        Request prev = response
                .getRequestForPagedResults(PagingDirection.PREVIOUS);
        if (prev != null) {
            prev.setCallback(mCallback);
        }
        page.setPrevPage(prev);
        mOnActionListener.setCursor(page);
    }

}