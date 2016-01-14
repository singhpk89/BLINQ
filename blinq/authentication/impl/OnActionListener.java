package com.blinq.authentication.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class OnActionListener<T> {

    private IPage<T> mPage;

    public OnActionListener() {
    }

    public void onComplete(T response) {
    }

    public void onException(Throwable throwable) {
    }

    public void onFail(String reason) {
    }

    public void onLoading() {
    }

    public void setCursor(IPage<T> page) {
        mPage = page;
    }

    /**
     * Return <code>True</code> if there is another next page with more results.
     * You can iterate to the next page and get more results by calling to
     * {@link #getNext()} method.
     *
     * @return <code>True</code> if more results exist.
     */
    public boolean hasNext() {
        if (mPage != null) {
            return mPage.hasNext();
        }
        return false;
    }

    /**
     * Return <code>True</code> if there is another previous page with more
     * results. You can iterate to the next page and get more results by calling
     * to {@link #getPrev()} method.
     *
     * @return <code>True</code> if more results exist.
     */
    public boolean hasPrev() {
        if (mPage != null) {
            return mPage.hasPrev();
        }
        return false;
    }

    /**
     * Ask for the next page results in async way. When the response will arrive
     * {@link #onComplete(Object)} method will be invoked again.
     */
    public void getNext() {
        if (mPage != null) {
            mPage.next();
        }
    }

    /**
     * Ask for the prev page results in async way. When the response will arrive
     * {@link #onComplete(Object)} method will be invoked again.
     */
    public void getPrev() {
        if (mPage != null) {
            mPage.prev();
        }
    }

    /**
     * Get the cursor that actually does the 'getMore()' action. For example, if
     * you want to hold this instance of cursor somewhere in your app and only
     * in some point of time to use it.
     *
     * @return {@link com.blinq.authentication.impl.facebook.Actions.Page} for iteration over pages of response.
     */
    public IPage<T> getCursor() {
        return mPage;
    }

    /**
     * Get the last page number that was retrieved.
     *
     * @return The page number.
     */
    public long getPageNum() {
        return mPage.getPageNum();
    }

    public Type getGenericType() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass()
                .getGenericSuperclass();
        return parameterizedType.getActualTypeArguments()[0];
    }

}