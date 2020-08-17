package com.bubble.common.sort.comparator;

import java.util.Comparator;

/**
 * Comparator
 *
 * @author wugang
 * date: 2020-08-17 11:46
 **/
public class SplitStringComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        String[] seg1 = o1.split("\t");
        String[] seg2 = o2.split("\t");
        if (4 > seg1.length || 4 > seg2.length) {
            return 1;
        }
        // 【词，词频，互信息，左右熵，成词位置概率】基于词频大小进行比较
        Double d1 = Double.parseDouble(seg1[1]);
        Double d2 = Double.parseDouble(seg2[1]);
        return d2.compareTo(d1);
    }

}
