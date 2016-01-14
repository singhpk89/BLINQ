package com.blinq.authentication.impl.Instagram.Actions;

import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.Instagram.InstagramAuthenticator;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.utils.Log;

import net.londatiga.android.instagram.Instagram;
import net.londatiga.android.instagram.InstagramRequest;

import org.apache.http.NameValuePair;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Instagram Base actions-handler. other sub handlers must extends this class
 * in order to manage the process of requesting and converting the response of Instagram API.
 *
 * @param <T> - Represents the converted response.
 */
public class InstagramActionImpl<T, Y> extends Action<T> {

    protected Page<T, Y> page = null;
    protected long me;
    protected Instagram instagram;
    private String endPoint;
    protected int limit = 10;
    protected InstagramRequest request;
    protected ArrayList<NameValuePair> requestParameters;

    Thread callBack;

    private void initializeThread() {
        callBack = new Thread(new Runnable() {
            @Override
            public void run() {
                if (getActionListener() != null) {
                    getActionListener().onLoading();
                }
                runRequest();
            }
        });
    }

    public InstagramActionImpl(Authenticator authenticator) {
        super(authenticator);
        initializeThread();
        page = new Page<T, Y>(this);
    }

    @Override
    protected void executeImpl() {
        OnActionListener<T> actionListener = getActionListener();
        if (authenticator.isConnected()) {
            initializeRequest();
            startCallBack();
        } else {
            String reason = "Authentication Error";
            Log.e(InstagramActionImpl.class.getSimpleName(), reason);
            if (actionListener != null) {
                actionListener.onFail(reason);
            }
        }
    }

    @Override
    protected void cancelImpl() {
        //callBack.stop();
    }

    protected void initializeRequest() {

        instagram = ((InstagramAuthenticator) authenticator).getInstagram();
        request = new InstagramRequest(instagram.getSession().getAccessToken());
    }

    private void startCallBack() {
        if (requestType == RequestType.ASYNC) {
            if (callBack.isAlive()) {
                cancelImpl();
                initializeThread();
            }
            callBack.start();
        } else {
            runRequest();
        }
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getEndPoint() {
        return endPoint;
    }

    protected String getUrl() {
        return endPoint;
    }

    protected T processResponse(Y object) throws JSONException {
        return null;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    void runRequest() {

        OnActionListener<T> actionListener = getActionListener();
        try {

            Y response = getResponse();

            if (response == null) {
                this.result = null;
                return;
            }

            T result = processResponse(response);
            updateCursor(response);

            if (requestType == RequestType.ASYNC) {
                actionListener.onComplete(result);
            } else {
                this.result = result;
            }

        } catch (Exception e) {
            this.result = null;
            if (actionListener != null)
                actionListener.onException(e);
        }
    }

    protected Y getResponse() throws Exception {
        return null;
    }

    protected void updateCursor(Y response) throws JSONException {

        if (response == null)
            return;
        if (mOnActionListener == null && requestType == RequestType.ASYNC) {
            return;
        }

        if (page == null) {
            page = new Page<T, Y>(InstagramActionImpl.this);
        }

        long pageNum = page.getPageNum();
    }


    /*
    * Enum to define two methods to the get data from instagram.
    * <ul>
    * <li>POST.</li>
    * <li>GET.</li>
    * </ul>
    */
    public enum RequestMethod {
        POST, GET
    }

}
