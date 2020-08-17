package com.bubble.common.file;

import java.io.IOException;
import java.io.OutputStream;

/**
 * DataWriter抽象工厂类
 *
 * @author wugang
 * date: 2020-08-14 10:45
 **/
public abstract class AbstractDataWriterFactory<T> {

    /**
     * Writer构造抽象方法
     *
     * @param out 输出
     * @return DataWriter抽象类
     * @throws IOException 异常
     */
    public abstract AbstractDataWriter<T> constructWriter(OutputStream out) throws IOException;

}
