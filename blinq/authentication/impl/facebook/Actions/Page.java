package com.blinq.authentication.impl.facebook.Actions;

import com.facebook.Request;
import com.blinq.authentication.impl.IPage;

public class Page<T> extends IPage {

    private final FacebookActionImpl<T> mAction;
    private Request mNextPage = null;
    private Request mPrevPage = null;
    private int mPageNum = 0;

    public Page(FacebookActionImpl<T> action) {
        mAction = action;
    }

    public boolean hasNext() {
        return mNextPage != null ? true : false;
    }

    public boolean hasPrev() {
        return mPrevPage != null ? true : false;
    }

    public long getPageNum() {
        return mPageNum;
    }

    public void next() {
        mPageNum++;
        mAction.runRequest(mNextPage);
    }

    public void prev() {
        mPageNum--;
        mAction.runRequest(mPrevPage);
    }

    void setNextPage(Request requestNextPage) {
        mNextPage = requestNextPage;
    }

    void setPrevPage(Request requestPrevPage) {
        mPrevPage = requestPrevPage;
    }
}