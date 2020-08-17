package com.bubble.common.file;

import com.google.common.base.Charsets;

import java.io.*;

/**
 * LineWriter
 *
 * @author wugang
 * date: 2020-08-17 11:44
 **/
public class LineWriter extends AbstractDataWriter<String> {

    protected final BufferedWriter bufferedWriter;


    public LineWriter(OutputStream out) {
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(out, Charsets.UTF_8));
    }


    public static Factory factory() {
        return new Factory();
    }

    @Override
    public void close() throws IOException {
        bufferedWriter.close();
    }

    @Override
    public void writeEntry(String item) throws IOException {
        bufferedWriter.write(item + "\n");
    }

    /**
     * Basic factory implementation. The only noteworthy things are:
     * <ul>
     * <li>Ability to configure linefeed to use (including none, pass null)</li>
     * <li>Writer uses {@link BufferedOutputStream} by default (can be disabled)
     *  </ul>
     */
    public static class Factory
            extends AbstractDataWriterFactory<String> {
        public Factory() {
        }

        @Override
        public AbstractDataWriter<String> constructWriter(OutputStream out) {
            return new LineWriter(out);
        }
    }

}
