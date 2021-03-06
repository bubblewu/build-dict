package com.bubble.common.sort;

import com.bubble.common.file.AbstractDataReader;
import com.bubble.common.file.AbstractDataReaderFactory;
import com.bubble.common.file.AbstractDataWriter;
import com.bubble.common.file.AbstractDataWriterFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Iterator;

/**
 * 排序：它驱动排序过程从预排序到最终输出。
 * 实例不是线程安全的
 *
 * @author wugang
 * date: 2020-08-13 19:40
 **/
public class Sorter<T> extends IteratingSorter<T> {

    /**
     * @param config        Configuration for the sorter
     * @param readerFactory Factory used for creating readers for pre-sorted data;
     *                      as well as for input if an {@link InputStream} is passed as source
     * @param writerFactory Factory used for creating writers for storing pre-sorted data;
     *                      as well as for results if an {@link OutputStream} is passed as destination.
     */
    public Sorter(SortConfig config,
                  AbstractDataReaderFactory<T> readerFactory,
                  AbstractDataWriterFactory<T> writerFactory,
                  Comparator<T> comparator) {
        super(config, readerFactory, writerFactory, comparator);
    }

    public Sorter() {
        super();
    }

    public Sorter(SortConfig config) {
        super(config);
    }

    protected Sorter<T> withReaderFactory(AbstractDataReaderFactory<T> f) {
        return new Sorter<T>(sortConfig, f, writerFactory, comparator);
    }

    protected Sorter<T> withWriterFactory(AbstractDataWriterFactory<T> f) {
        return new Sorter<T>(sortConfig, readerFactory, f, comparator);
    }

    protected Sorter<T> withComparator(Comparator<T> cmp) {
        return new Sorter<T>(sortConfig, readerFactory, writerFactory, cmp);
    }

    /**
     * Method that will perform full sort on specified input, writing results
     * into specified destination. Data conversions needed are done
     * using {@link AbstractDataReaderFactory} and {@link AbstractDataWriterFactory} configured
     * for this sorter.
     */
    public void sort(InputStream source, OutputStream destination)
            throws IOException {
        sort(readerFactory.constructReader(source), writerFactory.constructWriter(destination));
    }

    /**
     * Method that will perform full sort on input data read using given
     * {@link AbstractDataReader}, and written out using specified {@link AbstractDataWriter}.
     * Conversions to and from intermediate sort files is done
     * using {@link AbstractDataReaderFactory} and {@link AbstractDataWriterFactory} configured
     * for this sorter.
     *
     * @return true if sorting completed successfully; false if it was cancelled
     */
    public boolean sort(AbstractDataReader<T> inputReader, AbstractDataWriter<T> resultWriter) throws IOException {
        Iterator<T> it = super.sort(inputReader);
        if (it == null) {
            return false;
        }
        try {
            while (it.hasNext()) {
                T value = it.next();
                resultWriter.writeEntry(value);
            }
            resultWriter.close();
        } finally {
            super.close();
        }
        return true;
    }

}
