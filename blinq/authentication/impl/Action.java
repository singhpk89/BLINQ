package com.blinq.authentication.impl;

import com.blinq.authentication.Authenticator;

/**
 * Basic class - platforms like Facebook,Twitter,and Instagram must extend
 * in order to manage executing and converting our supported platform's API results.
 *
 * @param <T> - Results to be returned from sub actions. [TwitterActionImpl,FacebookActionImpl..]
 */
public abstract class Action<T> {

    protected Authenticator authenticator;
    protected RequestType requestType = RequestType.ASYNC;
    protected OnActionListener<T> mOnActionListener = null;
    protected String mEntity = "me"; // default
    protected String mEntityName = "";
    protected T result;

    public Action(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public void execute() {
        executeImpl();
    }

    public void cancel() {
        cancelImpl();
    }

    protected abstract void executeImpl();
    protected abstract void cancelImpl();

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public T getResult() {
        return result;
    }

    public void setActionListener(OnActionListener<T> onActionListener) {
        this.mOnActionListener = onActionListener;
    }

    public OnActionListener<T> getActionListener() {
        return mOnActionListener;
    }

    public void setEntity(String entity) {
        if (entity != null)
            this.mEntity = entity;
    }

    public void setEntityName(String entityName) {
        if (entityName != null)
            this.mEntityName = entityName;
    }

    protected String getEntity() {
        return mEntity;
    }

    protected String getEntityName() { return mEntityName;}

    /*
    * Enum to define two methods to process the request.
    * <ul>
    * <li>Synchronously.</li>
    * <li>ASynchronously.</li>
    * </ul>
    */
    public enum RequestType {
        SYNC, ASYNC
    }
}