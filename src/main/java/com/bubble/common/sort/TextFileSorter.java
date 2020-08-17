package com.bubble.common.sort;

import com.bubble.common.file.RawTextLineReader;
import com.bubble.common.file.RawTextLineWriter;
import com.bubble.common.sort.comparator.ByteArrayComparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 对输入文件内容进行排序
 * Basic {@link Sorter} implementation that operates on text line input.
 *
 * @author wugang
 * date: 2020-08-13 19:17
 **/
public class TextFileSorter extends Sorter<byte[]> {

    /**
     * Let's limit maximum memory used for pre-sorting when invoked from command-line to be
     * 256 megs
     */
    public final static long MAX_HEAP_FOR_PRESORT = 256L * 1024 * 1024;

    /**
     * Also just in case our calculations are wrong, require 10 megs for pre-sort anyway
     * (if invoked from CLI)
     */
    public final static long MIN_HEAP_FOR_PRESORT = 10L * 1024 * 1024;

    public TextFileSorter() {
        this(new SortConfig());
    }

    public TextFileSorter(SortConfig config) {
        super(config,
                RawTextLineReader.factory(), RawTextLineWriter.factory(),
                new ByteArrayComparator());
    }


    /**
     * 使用默认的ISO-8859-1排序规则进行基于行的排序的简单命令行操作的主要方法（即按字节排序）
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage: java " + TextFileSorter.class.getName() + " [input-file]");
            System.err.println("(where input-file is optional; if missing, read from STDIN)");
            System.exit(1);
        }

        // One more thing: use 50% of memory (but no more than 200 megs) for pre-sort
        // minor tweak: consider first 40 megs to go for other overhead...
        long availMem = Runtime.getRuntime().maxMemory() - (40 * 1024 * 1024);
        long maxMem = (availMem >> 1);
        if (maxMem > MAX_HEAP_FOR_PRESORT) {
            maxMem = MAX_HEAP_FOR_PRESORT;
        } else if (maxMem < MIN_HEAP_FOR_PRESORT) {
            maxMem = MIN_HEAP_FOR_PRESORT;
        }
        final TextFileSorter sorter = new TextFileSorter(new SortConfig().withMaxMemoryUsage(maxMem));
        final InputStream in;

        if (args.length == 0) {
            in = System.in;
        } else {
            File input = new File(args[0]);
            if (!input.exists() || input.isDirectory()) {
                System.err.println("File '" + input.getAbsolutePath() + "' does not exist (or is not file)");
                System.exit(2);
            }
            in = new FileInputStream(input);
        }

        // To be able to print out progress, need to spin one additional thread...
        new Thread(() -> {
            final long start = System.currentTimeMillis();
            try {
                while (!sorter.isCompleted()) {
                    Thread.sleep(5000L);
                    if (sorter.isPreSorting()) {
                        System.err.printf(" pre-sorting: %d files written\n", sorter.getNumberOfPreSortFiles());
                    } else if (sorter.isSorting()) {
                        System.err.printf(" sorting, round: %d/%d\n",
                                sorter.getSortRound(), sorter.getNumberOfSortRounds());
                    }
                }
                double secs = (System.currentTimeMillis() - start) / 1000.0;
                System.err.printf("Completed: took %.1f seconds.\n", secs);
            } catch (InterruptedException e) {
                double secs = (System.currentTimeMillis() - start) / 1000.0;
                System.err.printf("[INTERRUPTED] -- took %.1f seconds.\n", secs);
            }
        }).start();
        sorter.sort(in, System.out);
    }

}
