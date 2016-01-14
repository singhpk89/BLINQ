package com.blinq.authentication.impl.Twitter.actions;

import com.blinq.authentication.impl.IPage;

public class Page<T, Y> extends IPage {

    private final TwitterActionImpl<T, Y> mAction;
    private long mNextPage = 0;
    private long mPrevPage = 0;
    private long mPageNum = 1;

    public Page(TwitterActionImpl<T, Y> action) {
        mAction = action;
    }

    public boolean hasNext() {
        return mNextPage != 0 && mNextPage <= mAction.getMaxPages() ? true : false;
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

    void setToNextPage() {
        mPageNum++;
    }

    void setNextPage(long cursor) {
        mNextPage = cursor;
    }

    void setPrevPage(long cursor) {
        mPrevPage = cursor;
    }
}