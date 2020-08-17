package com.bubble.common.sort;

import com.bubble.common.file.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sorter Base
 *
 * @author wugang
 * date: 2020-08-14 10:21
 **/
public abstract class AbstractSorterBase<T> implements SortingState {

    /**
     * 每个entry(在缓冲区)在32位机器上约4字节;
     * 但我们还是保守点，用8作为底数，加上对象本身的大小。
     */
    private final static long ENTRY_SLOT_SIZE = 8L;

    protected final SortConfig sortConfig;
    /**
     * 用于读取中间Sorted文件的工厂。
     */
    protected AbstractDataReaderFactory<T> readerFactory;
    /**
     * 用于编写中间Sorted文件的工厂。
     */
    protected AbstractDataWriterFactory<T> writerFactory;

    /**
     * 用于对entries排序的比较器
     */
    protected Comparator<T> comparator;

    /*
    /**********************************************************************
    /* State 状态
    /**********************************************************************
     */

    protected SortingState.Phase phase;

    protected int presortFileCount;

    protected int sortRoundCount;

    protected int currentSortRound;

    protected final AtomicBoolean cancelRequest = new AtomicBoolean(false);

    protected Exception cancelForException;

    /*
    /**********************************************************************
    /* Construction 构造方法
    /**********************************************************************
     */

    protected AbstractSorterBase(SortConfig sortConfig,
                                 AbstractDataReaderFactory<T> readerFactory,
                                 AbstractDataWriterFactory<T> writerFactory,
                                 Comparator<T> comparator) {
        this.sortConfig = sortConfig;
        this.readerFactory = readerFactory;
        this.writerFactory = writerFactory;
        this.comparator = comparator;
        this.phase = null;
    }

    protected AbstractSorterBase() {
        this(new SortConfig());
    }

    protected AbstractSorterBase(SortConfig sortConfig) {
        this(sortConfig, null, null, null);
    }

    /*
    /**********************************************************************
    /* SortingState 接口的实现
    /**********************************************************************
    */

    @Override
    public Phase getPhase() {
        return this.phase;
    }

    @Override
    public boolean isPreSorting() {
        return this.phase == Phase.PRE_SORTING;
    }

    @Override
    public boolean isSorting() {
        return this.phase == Phase.SORTING;
    }

    @Override
    public boolean isCompleted() {
        return this.phase == Phase.COMPLETE;
    }

    @Override
    public int getNumberOfPreSortFiles() {
        return this.presortFileCount;
    }

    @Override
    public int getSortRound() {
        return this.currentSortRound;
    }

    @Override
    public int getNumberOfSortRounds() {
        return this.sortRoundCount;
    }

    @Override
    public void cancel() {
        this.cancelForException = null;
        cancelRequest.set(true);
    }

    @Override
    public void cancel(RuntimeException e) {
        this.cancelForException = e;
        cancelRequest.set(true);
    }

    @Override
    public void cancel(IOException e) {
        this.cancelForException = e;
        cancelRequest.set(true);
    }

    /*
    /**********************************************************************
    /* Internal methods： 预排序
    /**********************************************************************
     */

    /**
     * 遵守内存约束的前提下，读取允许的最大数量的数据到缓冲区
     *
     * @param inputReader Reader读取器
     * @param buffer      缓冲区
     * @param memoryToUse 内存约束
     * @param firstItem   第一个Item
     * @return Object数组
     */
    protected Object[] readMax(AbstractDataReader<T> inputReader, SegmentedBuffer buffer,
                               long memoryToUse, T firstItem) throws IOException {
        // 我们期望剩下的最大Item占用多少内存?
        int ptr = 0;
        // 重置当前buffer并初始化一个数组对象
        Object[] segment = buffer.resetAndStart();
        // 当前数组对象的长度
        int segmentLength = segment.length;
        long minMemoryNeeded;

        if (firstItem != null) {
            segment[ptr++] = firstItem;
            long firstSize = ENTRY_SLOT_SIZE + inputReader.estimateSizeInBytes(firstItem);
            minMemoryNeeded = Math.max(firstSize, 256L);
        } else {
            minMemoryNeeded = 256L;
        }
        // 通过缓冲成本减少内存数量
        memoryToUse -= (ENTRY_SLOT_SIZE * segmentLength);
        // 获取输入数据
        while (true) {
            T value = inputReader.readNext();
            // 返回null来表示输入结束
            if (value == null) {
                break;
            }
            // 当前value的占用内存情况预估
            long size = inputReader.estimateSizeInBytes(value);
            if (size > minMemoryNeeded) {
                minMemoryNeeded = size;
            }
            // 如果当前轮次的ptr大于等于当前对象数组的大小
            if (ptr >= segmentLength) {
                // 将当前对象数组添加到缓冲区中，存在到Node节点，
                // 并返回一个指定大小的新的空数组，来继续下一轮次（即Node.next）的缓冲存储
                // 如果当前数组的大小小于最大块的大小，就创建一个当前数组一倍大小的数组返回，否则增加25%的容量到新数组
                segment = buffer.appendCompletedChunk(segment);
                segmentLength = segment.length;
                memoryToUse -= (ENTRY_SLOT_SIZE * segmentLength);
                ptr = 0;
            }
            // 将数组存储在Node中
            segment[ptr++] = value;
            memoryToUse -= size;
            // memoryToUse默认使用40M来预排序，需使用的内存超过时，结束
            if (memoryToUse < minMemoryNeeded) {
                break;
            }
        }
        return buffer.completeAndClearBuffer(segment, ptr);
    }

    protected void presort(AbstractDataReader<T> inputReader, SegmentedBuffer buffer, T nextValue,
                           List<File> presorted)
            throws IOException {
        do {
            Object[] items = readMax(inputReader, buffer, sortConfig.getMaxMemoryUsage(), nextValue);
            Arrays.sort(items, rawComparator());
            presorted.add(writePresorted(items));
            nextValue = inputReader.readNext();
        } while (nextValue != null);
    }

    @SuppressWarnings("resource")
    protected File writePresorted(Object[] items) throws IOException {
        File tmp = sortConfig.getTempFileInterface().provide();
        @SuppressWarnings("unchecked")
        AbstractDataWriter<Object> writer = (AbstractDataWriter<Object>) writerFactory.constructWriter(new FileOutputStream(tmp));
        boolean closed = false;
        try {
            ++presortFileCount;
            for (int i = 0, end = items.length; i < end; ++i) {
                writer.writeEntry(items[i]);
                // to further reduce transient mem usage, clear out the ref
                items[i] = null;
            }
            closed = true;
            writer.close();
        } finally {
            if (!closed) {
                // better swallow since most likely we are getting an exception already...
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
        }
        return tmp;
    }

    protected void writeAll(AbstractDataWriter<T> resultWriter, Object[] items)
            throws IOException {
        // need to go through acrobatics, due to type erasure... works, if ugly:
        @SuppressWarnings("unchecked")
        AbstractDataWriter<Object> writer = (AbstractDataWriter<Object>) resultWriter;
        for (Object item : items) {
            writer.writeEntry(item);
        }
    }


    /**
     * 对给定输入进行排序并写入最终输出的合并方法。
     */
    protected void merge(List<File> presorted, AbstractDataWriter<T> resultWriter)
            throws IOException {
        List<File> inputs = merge(presorted);
        // and then last around to produce the result file
        _merge(inputs, resultWriter);
    }

    /**
     * Main-level merge method that sorts the given input.
     *
     * @return List of files that are individually sorted and ready for final merge.
     */
    protected List<File> merge(List<File> presorted)
            throws IOException {
        // Ok, let's see how many rounds we should have...
        final int mergeFactor = sortConfig.getMergeFactor();
        sortRoundCount = calculateRoundCount(presorted.size(), mergeFactor);
        currentSortRound = 0;
        // first intermediate rounds
        List<File> inputs = presorted;
        while (inputs.size() > mergeFactor) {
            ArrayList<File> outputs = new ArrayList<>(1 + ((inputs.size() + mergeFactor - 1) / mergeFactor));
            for (int offset = 0, end = inputs.size(); offset < end; offset += mergeFactor) {
                int localEnd = Math.min(offset + mergeFactor, end);
                outputs.add(_merge(inputs.subList(offset, localEnd)));
            }
            ++currentSortRound;
            // and then switch result files to be input files
            inputs = outputs;
        }
        return inputs;
    }

    protected static int calculateRoundCount(int files, int mergeFactor) {
        int count = 1;
        while (files > mergeFactor) {
            ++count;
            files = (files + mergeFactor - 1) / mergeFactor;
        }
        return count;
    }

    @SuppressWarnings("resource")
    protected File _merge(List<File> inputs)
            throws IOException {
        File resultFile = sortConfig.getTempFileInterface().provide();
        _merge(inputs, writerFactory.constructWriter(new FileOutputStream(resultFile)));
        return resultFile;
    }

    protected void _merge(List<File> inputs, AbstractDataWriter<T> writer)
            throws IOException {
        AbstractDataReader<T> merger = null;
        try {
            merger = createMergeReader(inputs);
            T value;
            while ((value = merger.readNext()) != null) {
                writer.writeEntry(value);
            }
            merger.close(); // usually not necessary (reader should close on eof) but...
            merger = null;
            writer.close();
        } finally {
            if (merger != null) {
                try {
                    merger.close();
                } catch (IOException e) {
                }
            }
            for (File input : inputs) {
                input.delete();
            }
        }
    }

    protected AbstractDataReader<T> createMergeReader(List<File> inputs) throws IOException {
        ArrayList<AbstractDataReader<T>> readers = new ArrayList<>(inputs.size());
        for (File mergedInput : inputs) {
            readers.add(readerFactory.constructReader(new FileInputStream(mergedInput)));
        }
        return AbstractMerger.mergedReader(comparator, readers);
    }

    protected boolean _checkForCancel() throws IOException {
        return _checkForCancel(null);
    }

    protected boolean _checkForCancel(Collection<File> tmpFilesToDelete) throws IOException {
        if (!cancelRequest.get()) {
            return false;
        }
        if (tmpFilesToDelete != null) {
            for (File f : tmpFilesToDelete) {
                f.delete();
            }
        }
        if (cancelForException != null) {
            // can only be an IOException or RuntimeException, so
            if (cancelForException instanceof RuntimeException) {
                throw (RuntimeException) cancelForException;
            }
            throw (IOException) cancelForException;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    protected Comparator<Object> rawComparator() {
        return (Comparator<Object>) comparator;
    }


}
