package com.blinq.authentication.impl;

/**
 * Created by Osama on 9/3/2014.
 */
public abstract class IPage<T> {

    public abstract boolean hasNext();
    public abstract boolean hasPrev();
    public abstract long getPageNum();
    public abstract void next() ;
    public abstract void prev() ;
}
