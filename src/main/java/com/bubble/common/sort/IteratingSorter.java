package com.bubble.common.sort;

import com.bubble.common.exection.IterableSorterException;
import com.bubble.common.file.AbstractDataReader;
import com.bubble.common.file.AbstractDataReaderFactory;
import com.bubble.common.file.AbstractDataWriterFactory;
import com.bubble.common.file.SegmentedBuffer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * IteratingSorter
 *
 * @author wugang
 * date: 2020-08-14 13:08
 **/
public class IteratingSorter<T> extends AbstractSorterBase<T> implements Closeable {
    private List<File> mergerInputs;
    private AbstractDataReader<T> merger;

    public IteratingSorter(SortConfig config,
                           AbstractDataReaderFactory<T> readerFactory,
                           AbstractDataWriterFactory<T> writerFactory,
                           Comparator<T> comparator) {
        super(config, readerFactory, writerFactory, comparator);
    }

    public IteratingSorter() {
        super();
    }

    public IteratingSorter(SortConfig config) {
        super(config);
    }

    /**
     * 将给定的数据进行排序
     * <p>
     * Method that will perform full sort on input data read using given
     * {@link AbstractDataReader}.
     * <p>
     * Conversions to and from intermediate sort files is done
     * using {@link AbstractDataReaderFactory} and {@link AbstractDataWriterFactory} configured
     * for this sorter.
     * <p>
     * The returned Iterator will throw {@link IterableSorterException} if any
     * IOException is encountered during calls of {@link Iterator#next()}.
     *
     * @return Iterator if sorting complete and output is ready to be written;
     * null if it was cancelled
     */
    public Iterator<T> sort(AbstractDataReader<T> inputReader) throws IOException {
        // Clean up any previous sort
        close();
        // First, pre-sort:
        phase = Phase.PRE_SORTING;
        boolean inputClosed = false;

        SegmentedBuffer buffer = new SegmentedBuffer();
        presortFileCount = 0;
        sortRoundCount = -1;
        currentSortRound = -1;

        Iterator<T> iterator = null;
        try {
            Object[] items = readMax(inputReader, buffer, sortConfig.getMaxMemoryUsage(), null);
            if (_checkForCancel()) {
                close();
                return null;
            }
            Arrays.sort(items, rawComparator());
            T next = inputReader.readNext();
            /* Minor optimization: in case all entries might fit in
             * in-memory sort buffer, avoid writing intermediate file
             * and just write results directly.
             */
            if (next == null) {
                inputClosed = true;
                inputReader.close();
                phase = Phase.SORTING;
                iterator = new CastingIterator<T>(Arrays.asList(items).iterator());
            } else { // but if more data than memory-buffer-full, do it right:
                List<File> presorted = new ArrayList<>();
                presorted.add(writePresorted(items));
                // it's a big array, clear refs as early as possible
                items = null;
                presort(inputReader, buffer, next, presorted);
                inputClosed = true;
                inputReader.close();
                phase = Phase.SORTING;
                if (_checkForCancel(presorted)) {
                    close();
                    return null;
                }
                mergerInputs = presorted;
                merger = createMergeReader(merge(presorted));
                iterator = new MergerIterator<>(merger);
            }
        } finally {
            if (!inputClosed) {
                try {
                    inputReader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        if (_checkForCancel()) {
            close();
            return null;
        }
        phase = Phase.COMPLETE;
        return iterator;
    }


    @Override
    public void close() throws IOException {
        if (merger != null) {
            try {
                merger.close();
            } catch (IOException ignored) {
            }
        }
        if (mergerInputs != null) {
            for (File input : mergerInputs) {
                input.delete();
            }
        }
        mergerInputs = null;
        merger = null;
    }


    private static class MergerIterator<T> implements Iterator<T> {
        private final AbstractDataReader<T> merger;
        private T next;

        private MergerIterator(AbstractDataReader<T> merger) throws IOException {
            this.merger = merger;
            this.next = merger.readNext();
        }

        @Override
        public boolean hasNext() {
            return (next != null);
        }

        @Override
        public T next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            T t = next;
            try {
                next = merger.readNext();
            } catch (IOException e) {
                throw new IterableSorterException(e);
            }
            return t;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
