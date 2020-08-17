package com.bubble.common.sort;

import java.util.Iterator;

/**
 * IteratingSorter
 *
 * @author wugang
 * date: 2020-08-14 13:08
 **/
public class CastingIterator<T> implements Iterator<T> {
    private final Iterator<Object> it;

    public CastingIterator(Iterator<Object> it) {
        this.it = it;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T next() {
        return (T) it.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}