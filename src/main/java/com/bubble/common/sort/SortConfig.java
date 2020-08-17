package com.bubble.common.sort;

import com.bubble.common.file.StdTempFileProvider;
import com.bubble.common.file.TempFileInterface;

/**
 * 排序配置：用于更改排序过程细节的配置对象
 *
 * @author wugang
 * date: 2020-08-13 19:19
 **/
public class SortConfig {
    /**
     * 默认情况下，使用40M来预排序。
     */
    public final static long DEFAULT_MEMORY_USAGE = 40 * 1024 * 1024;
    /**
     * 默认的合并排序是16路排序(同时使用16个输入文件)
     */
    public final static int DEFAULT_MERGE_FACTOR = 16;

    protected int mergeFactor;
    protected long maxMemoryUsage;
    protected TempFileInterface tempFileInterface;

    public SortConfig() {
        this.mergeFactor = DEFAULT_MERGE_FACTOR;
        this.maxMemoryUsage = DEFAULT_MEMORY_USAGE;
        this.tempFileInterface = new StdTempFileProvider();
    }

    public SortConfig(SortConfig base, int mergeFactor) {
        this.mergeFactor = mergeFactor;
        this.maxMemoryUsage = base.maxMemoryUsage;
        this.tempFileInterface = base.tempFileInterface;
    }

    protected SortConfig(SortConfig base, long maxMemoryUsage) {
        this.maxMemoryUsage = maxMemoryUsage;
        this.mergeFactor = base.mergeFactor;
        this.tempFileInterface = base.tempFileInterface;
    }

    protected SortConfig(SortConfig base, TempFileInterface tempFileInterface) {
        this.mergeFactor = base.mergeFactor;
        this.maxMemoryUsage = base.maxMemoryUsage;
        this.tempFileInterface = tempFileInterface;
    }

    /**
     * 基于maxMemoryUsage构造新的SortConfig
     *
     * @param maxMemoryUsage 预排序最大内存
     * @return 新的SortConfig
     */
    public SortConfig withMaxMemoryUsage(long maxMemoryUsage) {
        if (maxMemoryUsage == this.maxMemoryUsage) {
            return this;
        }
        return new SortConfig(this, maxMemoryUsage);
    }

    public SortConfig withTempFileProvider(TempFileInterface provider) {
        if (provider == this.tempFileInterface) {
            return this;
        }
        return new SortConfig(this, provider);
    }


    public int getMergeFactor() {
        return mergeFactor;
    }

    public long getMaxMemoryUsage() {
        return maxMemoryUsage;
    }

    public TempFileInterface getTempFileInterface() {
        return tempFileInterface;
    }
}
