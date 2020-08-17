package com.bubble.common.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merger：用于将来自多个输入源的项目合并为一个的对象。
 *
 * @author wugang
 * date: 2020-08-14 13:01
 **/
public abstract class AbstractMerger<T> extends AbstractDataReader<T> {
    protected final Comparator<T> comparator;

    public AbstractMerger(Comparator<T> cmp) {
        comparator = cmp;
    }

    public static <T> AbstractDataReader<T> mergedReader(Comparator<T> cmp, List<AbstractDataReader<T>> inputs)
            throws IOException {
        switch (inputs.size()) {
            /**
             * s
             */
            case 0:
                throw new IllegalArgumentException("Can not pass empty DataReader array");
            case 1:
                return inputs.get(0);
            case 2:
                return new PairwiseMerger<T>(cmp, inputs.get(0), inputs.get(1));
        }
        // otherwise, divide and conquer
        ArrayList<AbstractDataReader<T>> readers = new ArrayList<AbstractDataReader<T>>(1 + (inputs.size() >> 1));
        int i = 0;
        final int end = inputs.size() - 1;
        for (; i < end; i += 2) {
            readers.add(new PairwiseMerger<T>(cmp, inputs.get(i), inputs.get(i + 1)));
        }
        // and for odd number of readers, add last one as is without merging
        if (i < inputs.size()) {
            readers.add(inputs.get(i));
        }
        return mergedReader(cmp, readers);
    }

    protected static class PairwiseMerger<T>
            extends AbstractMerger<T> {
        protected final AbstractDataReader<T> _reader1;
        protected final AbstractDataReader<T> _reader2;

        protected T _data1;
        protected T _data2;

        protected boolean _closed;

        public PairwiseMerger(Comparator<T> comparator,
                              AbstractDataReader<T> reader1, AbstractDataReader<T> reader2)
                throws IOException {
            super(comparator);
            _reader1 = reader1;
            _data1 = reader1.readNext();
            _reader2 = reader2;
            _data2 = reader2.readNext();
        }

        @Override
        public T readNext() throws IOException {
            if (_data1 == null) {
                if (_data2 == null) {
                    // [Issue#8]: Should auto-close merged input when there is no more data
                    close();
                    return null;
                }
                T result = _data2;
                _data2 = _reader2.readNext();
                return result;
            }
            if (_data2 == null) {
                T result = _data1;
                _data1 = _reader1.readNext();
                return result;
            }
            // neither is null, compare
            T result;
            if (comparator.compare(_data1, _data2) <= 0) {
                result = _data1;
                _data1 = _reader1.readNext();
            } else {
                result = _data2;
                _data2 = _reader2.readNext();
            }
            return result;
        }

        @Override
        public int estimateSizeInBytes(T item) {
            // should not matter so
            return _reader1.estimateSizeInBytes(item);
        }

        @Override
        public void close() throws IOException {
            if (!_closed) {
                _reader1.close();
                _reader2.close();
                _closed = true;
            }
        }
    }

}
