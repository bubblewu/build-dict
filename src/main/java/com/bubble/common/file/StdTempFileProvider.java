package com.bubble.common.file;

import java.io.File;
import java.io.IOException;

/**
 * 使用JDK默认的临时文件生成机制。
 *
 * @author wugang
 * date: 2020-08-13 19:28
 **/
public class StdTempFileProvider implements TempFileInterface {
    /**
     * 默认临时文件前缀。
     */
    public final static String DEFAULT_PREFIX = "j-merge-sort-";
    /**
     * 默认临时文件后缀。
     */
    public final static String DEFAULT_SUFFIX = ".tmp";

    protected final String prefix;
    protected final String suffix;

    public StdTempFileProvider() {
        this(DEFAULT_PREFIX, DEFAULT_SUFFIX);
    }

    public StdTempFileProvider(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public File provide() throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        return file;
    }
}
