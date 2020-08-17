package com.bubble.common.file;

import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * LineReader 文本行
 *
 * @author wugang
 * date: 2020-08-17 11:40
 **/
public class LineReader extends AbstractDataReader<String> {
    protected final BufferedReader bufferReader;

    public LineReader(InputStream in) {
        bufferReader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
    }

    /**
     * Convenience method for instantiating factory to create instances of
     * this {@link AbstractDataReader}.
     */
    public static Factory factory() {
        return new Factory();
    }

    @Override
    public void close() throws IOException {
        bufferReader.close();
    }

    @Override
    public int estimateSizeInBytes(String item) {
        // Wild guess: array objects take at least 8 bytes, probably 12 or 16.
        // And size of actual array storage rounded up to 4-byte alignment. So:
        int bytes = item.getBytes().length;
        bytes = ((bytes + 3) >> 2) << 2;
        return 16 + bytes;
    }

    @Override
    public String readNext() throws IOException {
        return bufferReader.readLine();
    }


    public static class Factory extends AbstractDataReaderFactory<String> {
        @Override
        public AbstractDataReader<String> constructReader(InputStream in) {
            return new LineReader(in);
        }

    }

}
