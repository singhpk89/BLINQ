package com.blinq.authentication.impl.Instagram.Actions;

import com.blinq.authentication.impl.IPage;

/**
 * Created by Osama on 9/25/2014.
 */
public class Page<T, Y> extends IPage {

    private final InstagramActionImpl<T, Y> mAction;
    private long mNextPage = 0;
    private long mPrevPage = 0;
    private long mPageNum = 1;

    public Page(InstagramActionImpl<T, Y> action) {
        mAction = action;
    }

    public boolean hasNext() {
        return mNextPage != 0 ? true : false;
    }

    public boolean hasPrev() {
        return mPrevPage != -1 ? true : false;
    }

    public long getPageNum() {
        return mPageNum;
    }

    @Override
    public void next() {
        mPageNum++;
        mAction.runRequest();
    }

    @Override
    public void prev() {
        mPageNum--;
        mAction.runRequest();
    }

    void setNextPage(long cursor) {
        mNextPage = cursor;
    }

    void setPrevPage(long cursor) {
        mPrevPage = cursor;
    }
}
