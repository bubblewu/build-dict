package com.bubble.common.exection;

import java.io.IOException;

/**
 * IterableSorterException
 *
 * @author wugang
 * date: 2020-08-14 13:10
 **/
public class IterableSorterException extends RuntimeException {
    private static final long serialVersionUID = 4128275952692775695L;

    public IterableSorterException(IOException cause) {
        super(cause);
    }

}
