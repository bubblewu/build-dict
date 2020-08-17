package com.bubble.common.file;

import java.io.File;
import java.io.IOException;

/**
 * 临时文件生成机制
 *
 * @author wugang
 * date: 2020-08-13 19:23
 **/
public interface TempFileInterface {

    /**
     * 临时文件生成，并自动删除
     *
     * @return 临时文件File
     * @throws IOException 异常
     */
    File provide() throws IOException;

}
