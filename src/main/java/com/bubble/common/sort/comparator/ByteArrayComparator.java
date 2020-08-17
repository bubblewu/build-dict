package com.bubble.common.sort.comparator;

import java.util.Comparator;

/**
 * 比较器
 *
 * @author wugang
 * date: 2020-08-14 14:34
 **/
public class ByteArrayComparator implements Comparator<byte[]> {

    @Override
    public int compare(byte[] o1, byte[] o2) {
        final int len = Math.min(o1.length, o2.length);
        for (int i = 0; i < len; ++i) {
            // alas, sign extension means we must do masking...
            int diff = (o1[i] & 0xFF) - (o2[i] & 0xFF);
            if (diff != 0) {
                return diff;
            }
        }
        return o1.length - o2.length;
    }
}
