package com.bubble.common;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 常量配置
 *
 * @author wugang
 * date: 2020-08-13 18:01
 **/
public class Constants {

    /**
     * 用于预排序的最大内存限制为256M
     */
    public final static long MAX_HEAP_FOR_PRESORT = 256L * 1024 * 1024;

    /**
     * 为了防止计算出错，预先排序需要10M(如果从CLI命令行界面调用)
     */
    public final static long MIN_HEAP_FOR_PRESORT = 10L * 1024 * 1024;

    /**
     * 停用词：过滤每一行的停词
     */
    private final static String STOP_WORDS = "的很了么呢是嘛个都也比还这于不与才上用就好在和对挺去后没说";

    /**
     * 词的前后连接符号
     */
    public final static String CONNECTOR = "$";

    /**
     * 文本过滤
     */
    public final static Map<String, String> FILTER_MAP = Maps.newHashMap();
    public final static String EMPTY = " ";

    static {
        FILTER_MAP.put("[" + STOP_WORDS + "]", EMPTY);
        // 匹配任何标点字符 !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
        FILTER_MAP.put("\\p{Punct}", EMPTY);
        // 其中的小写 p 是 property 的意思，表示 Unicode 属性，用于 Unicode 正表达式的前缀。
        // 大写 P 表示 Unicode 字符集七个字符属性之一：标点字符。
        FILTER_MAP.put("\\pP", EMPTY);
        FILTER_MAP.put("　", EMPTY);
        // 空格或制表符：[ \t]
        FILTER_MAP.put("\\p{Blank}", EMPTY);
        // 空白字符：[ \t\n\x0B\f\r]
        FILTER_MAP.put("\\p{Space}", EMPTY);
        // 控制字符：[\x00-\x1F\x7F]
        FILTER_MAP.put("\\p{Cntrl}", EMPTY);
    }

    private static final String BASE_PATH = "/Users/wugang/code/java/github/build-dict/data/temp/";
    /**
     * 右
      */
    public static final String NGRAM_FILE = BASE_PATH + "/ngram.data";
    public static final String NGRAM_SORT_FILE = BASE_PATH + "/ngram_sort.data";
    public static final String NGRAM_FREQ_FILE = BASE_PATH + "/ngram_freq.data";
    public static final String NGRAM_FREQ_SORT_FILE = BASE_PATH + "/ngram_freq_sort.data";
    /**
     * 左
     */
    public static final String NGRAM_FILE_LEFT = BASE_PATH + "/ngram_left.data";
    public static final String NGRAM_SORT_FILE_LEFT = BASE_PATH + "/ngram_sort_left.data";
    public static final String NGRAM_FREQ_FILE_LEFT = BASE_PATH + "/ngram_freq_left.data";
    public static final String NGRAM_FREQ_SORT_FILE_LEFT = BASE_PATH + "/ngram_freq_sort_left.data";

    /**
     * 左右熵文件
     */
    public static final String MERGE_FILE = BASE_PATH + "/merge.temp";
    public static final String MERGE_SORT_FILE = BASE_PATH + "/merge_sort.tmp";
    public static final String MERGE_ENTROPY_FILE = BASE_PATH + "/merge_entropy.data";

    /**
     * 抽词输出结果
     */
    public static final String WORDS_FILE = BASE_PATH + "/words.data";
    public static final String WORDS_SORT_FILE = BASE_PATH + "/words_sort.data";


    /**
     * 各字的位置成词概率
     * /pos_prop.txt 或 /sougou/position_prop.txt
     */
    public static final String POSITION_PROBABILITY_FILE = "/sougou/position_prop.txt";


}
