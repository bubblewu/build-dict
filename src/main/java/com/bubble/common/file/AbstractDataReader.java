package com.bubble.common.file;

import java.io.IOException;

/**
 * DataReader抽象类
 *
 * @author wugang
 * date: 2020-08-14 10:37
 **/
public abstract class AbstractDataReader<T> {

    /**
     * 读取下一个数据项的方法;
     *
     * @return 将返回null来表示输入结束，否则返回一个非null项。
     * @throws IOException 异常
     */
    public abstract T readNext() throws IOException;

    /**
     * 估计给定item的内存使用情况的方法，以便在预排序阶段限制存储在内存中的数据量。
     *
     * @param item 给定item
     * @return int
     */
    public abstract int estimateSizeInBytes(T item);

    /**
     * 关闭Reader的方法。
     * 请注意，reader需要确保多次调用close是正确的。
     * 一旦Reader它已经达到输入的结束，Reader会关闭底层资源。
     *
     * @throws IOException 异常
     */
    public abstract void close() throws IOException;

}
