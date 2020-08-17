package com.bubble.common.file;

import java.io.IOException;
import java.io.InputStream;

/**
 * DataReader抽象工厂类
 *
 * @author wugang
 * date: 2020-08-14 10:47
 **/
public abstract class AbstractDataReaderFactory<T> {

    /**
     * Reader构造抽象方法
     *
     * @param in 输入
     * @return DataReader
     * @throws IOException 异常
     */
    public abstract AbstractDataReader<T> constructReader(InputStream in) throws IOException;

}
