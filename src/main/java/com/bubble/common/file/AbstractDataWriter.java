package com.bubble.common.file;

import java.io.IOException;

/**
 * DataWriter抽象方法
 *
 * @author wugang
 * date: 2020-08-14 10:43
 **/
public abstract class AbstractDataWriter<T> {

    /**
     * 写
     *
     * @param item item条目
     * @throws IOException 异常
     */
    public abstract void writeEntry(T item) throws IOException;

    /**
     * 关闭
     *
     * @throws IOException 异常
     */
    public abstract void close() throws IOException;

}
