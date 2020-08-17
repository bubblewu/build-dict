package com.bubble.service;

import com.bubble.common.Constants;
import com.bubble.common.CounterMap;
import com.bubble.common.TooKits;
import com.bubble.common.sort.SortConfig;
import com.bubble.common.sort.SplitFileSorter;
import com.bubble.common.sort.TextFileSorter;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * 未登录词 词典构建
 *
 * @author wugang
 * date: 2020-08-13 17:59
 **/
public class DictBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DictBuilder.class);
    /**
     * 候选词的长度
     */
    private final int maxLen;

    public DictBuilder(int maxLen) {
        this.maxLen = maxLen;
    }

    /**
     * 生成右临接词的组合
     *
     * @param textFile 文件
     * @return ngramFreqSort的绝对路径
     */
    public String genRight(String textFile) {
        return genFreqAndEntropy(textFile, true);
    }

    /**
     * 生成左临接词的组合
     *
     * @param textFile 文件
     * @return ngramFreqSort的绝对路径
     */
    public String genLeft(String textFile) {
        return genFreqAndEntropy(textFile, false);
    }

    /**
     * 词频和凝聚程度（信息熵）计算
     *
     * @param textFile 输入文本文件
     * @param isRight  是否为右临接计算
     * @return 左/右临接的输出存储文件绝对路径
     */
    private String genFreqAndEntropy(String textFile, boolean isRight) {
        LOGGER.info("开始[{}] 临接词计算", isRight ? "右" : "左");
        File file = new File(textFile);
        File ngramFile;
        File ngramFreqFile;
        File ngramSortFile;
        File ngramFreqSort;
        if (isRight) {
            ngramFile = new File(Constants.NGRAM_FILE);
            ngramFreqFile = new File(Constants.NGRAM_FREQ_FILE);
            ngramSortFile = new File(Constants.NGRAM_SORT_FILE);
            ngramFreqSort = new File(Constants.NGRAM_FREQ_SORT_FILE);
        } else {
            ngramFile = new File(Constants.NGRAM_FILE_LEFT);
            ngramFreqFile = new File(Constants.NGRAM_FREQ_FILE_LEFT);
            ngramSortFile = new File(Constants.NGRAM_SORT_FILE_LEFT);
            ngramFreqSort = new File(Constants.NGRAM_FREQ_SORT_FILE_LEFT);
        }
        try (BufferedReader reader = Files.newReader(file, Charsets.UTF_8)) {
            // 生成Ngram文件，并排序：将相同的词语放在一起
            genNgramAndSortFile(isRight, reader, ngramFile, ngramSortFile);
            // 凝聚程度：词频和信息熵
            genFreqEntropyAndSortFile(isRight, ngramSortFile, ngramFreqFile, ngramFreqSort);
        } catch (IOException e) {
            LOGGER.error("read file error. file = {}", file, e);
        }
        return ngramFreqSort.getAbsolutePath();
    }


    /**
     * 凝聚程度
     * 对排序后的Ngram文件进行邻字集合统计，并计算某词有邻字的词频和信息熵存储到ngramFreqFile文件，之后进行排序存储到ngramFreqSort文件
     * 步骤为：
     * - 遍历排序后的Ngram文件，并分片段进行下面的处理（即首字相同的为同一片段）
     * - 进行邻字集合统计，记为Map<String, CounterMap>；
     * - 计算某词邻字的词频；
     * - 计算某词邻字的信息熵；
     * <p>
     * Sort排序：
     * - 输入ngramFreqFile文件和输出文件ngramFreqSortFile，进行Sort；
     * - 将相同的词语放在一起。
     *
     * @param isRight           是否计算右临接词
     * @param ngramSortFile     排序后的Ngram文件
     * @param ngramFreqFile     某词有邻字的词频和信息熵存储文件
     * @param ngramFreqSortFile 某词有邻字的词频和信息熵排序后的存储文件
     * @throws IOException 异常
     */
    private void genFreqEntropyAndSortFile(boolean isRight, File ngramSortFile, File ngramFreqFile, File ngramFreqSortFile) throws IOException {
        // 加载排序后的Ngram文件
        BufferedReader sortReader = Files.newReader(ngramSortFile, Charsets.UTF_8);
        // 存储的Ngram的词频和信息熵
        BufferedWriter freqWriter = Files.newWriter(ngramFreqFile, Charsets.UTF_8);
        // 记录每个Ngram的第一个字
        String first = null;
        String curr = null;
        Map<String, CounterMap> stat = Maps.newHashMap();
        while (null != (curr = sortReader.readLine())) {
            if (null == first) {
                // 进行邻字集合统计
                for (int i = 1; i < curr.length(); ++i) {
                    // 步长为1，获取当前Ngram的从左到右的组成词
                    String w = curr.substring(0, i);
                    // 当前词的邻字
                    String suffix = curr.substring(i).substring(0, 1);
                    if (stat.containsKey(w)) {
                        stat.get(w).incr(suffix);
                    } else {
                        CounterMap cm = new CounterMap();
                        // 当前词的邻字，出现次数加1
                        cm.incr(suffix);
                        // Map<当前词，当前词的邻字及其出现次数>
                        stat.put(w, cm);
                    }
                }
                // 当前片段词的第一个字
                first = curr.substring(0, 1);
            } else {
                // 如果当前Ngram的首字不同，则先计算并存储上一轮的词频和信息熵，之后再去遍历Ngram文件
                if (!curr.startsWith(first)) {
                    StringBuilder builder = new StringBuilder();
                    for (String w : stat.keySet()) {
                        CounterMap cm = stat.get(w);
                        // 计算某词有邻字的词频
                        int freq = nextWordTotalCount(cm);
                        // 计算某词有邻字的的信息熵
                        double entropy = informationEntropy(freq, cm);
                        if (isRight) {
                            builder.append(w).append("\t").append(freq).append("\t").append(entropy).append("\n");
                        } else {
                            builder.append(TooKits.reverse(w)).append("\t").append(entropy).append("\n");
                        }
                    }
                    freqWriter.write(builder.toString());
                    stat.clear();

                    // 记录当前新片段词的首字
                    first = curr.substring(0, 1);
                }

                // 当前词的首字第一个字与上次相同时，进行邻字集合统计
                for (int i = 1; i < curr.length(); ++i) {
                    String w = curr.substring(0, i);
                    String suffix = curr.substring(i).substring(0, 1);
                    if (stat.containsKey(w)) {
                        stat.get(w).incr(suffix);
                    } else {
                        CounterMap cm = new CounterMap();
                        cm.incr(suffix);
                        stat.put(w, cm);
                    }
                }
            }
        }
        // 把最后一行读取的数据进行计算存储
        StringBuilder builder = new StringBuilder();
        for (String w : stat.keySet()) {
            CounterMap cm = stat.get(w);
            // 计算某词有邻字的词频
            int freq = nextWordTotalCount(cm);
            // 计算某词有邻字的的信息熵
            double entropy = informationEntropy(freq, cm);
            if (isRight) {
                builder.append(w).append("\t").append(freq).append("\t").append(entropy).append("\n");
            } else {
                builder.append(TooKits.reverse(w)).append("\t").append(entropy).append("\n");
            }
        }
        freqWriter.write(builder.toString());
        stat.clear();
        freqWriter.close();

        LOGGER.info("ngramFreq -> ngramFreqSort, start sorting...");
        sortFile(ngramFreqFile, ngramFreqSortFile);
    }

    /**
     * 某个词的左/右临接字的出现总数
     *
     * @param counterMap 某个词的左右临接字及其出现次数
     * @return 左/右临接字的出现总数
     */
    private int nextWordTotalCount(CounterMap counterMap) {
        int totalCount = 0;
        // 遍历该词对应的下一个词及其次数，求其和
        for (String k : counterMap.countAll().keySet()) {
            totalCount += counterMap.get(k);
        }
        return totalCount;
    }

    /**
     * 根据某词的左邻字集合和右邻字集合，求左/右邻字的信息熵
     * （越小说明混乱程度越低，更有确定性）
     * 如：一句话“吃葡萄不吐葡萄皮不吃葡萄倒吐葡萄皮”，“葡萄”一词出现了四次，
     * 其中左邻字分别为 {吃, 吐, 吃, 吐} ，右邻字分别为 {不, 皮, 倒, 皮} 。
     * 根据公式，“葡萄”一词的
     * - 左邻字的信息熵为 – (1/2) · log(1/2) – (1/2) · log(1/2) ≈ 0.693 ，
     * - 它的右邻字的信息熵则为 – (1/2) · log(1/2) – (1/4) · log(1/4) – (1/4) · log(1/4) ≈ 1.04 。
     * 可见，在这个句子中，“葡萄”一词的右邻字更加丰富一些，不确定性也更强。
     *
     * @param totalCount 某词的临接字的出现总次数
     * @param counterMap 某个词的左右临接字及其出现次数
     * @return 左/右邻字的信息熵
     */
    private double informationEntropy(int totalCount, CounterMap counterMap) {
        double ie = 0d;
        for (String k : counterMap.countAll().keySet()) {
            // 某个左/右临字出现的概率
            double probability = counterMap.get(k) * 1.0 / totalCount;
            ie += -1 * TooKits.log2(probability) * probability;
        }
        return ie;
    }

    /**
     * 根据输入文本生成指定窗口大小的Ngram文件，并对其进行排序（将相同的词语放在一起）存入ngramSortFile文件。
     * 步骤为：
     * Ngram文件存储：
     * - 将输入文件按行读取；
     * - 对该行文本进行预处理：替换停词和无效符号为空格；
     * - 基于空格将line切分为多个片段section；
     * - 对片段section处理：trim且为左临接词计算时reserve反转词对顺序；
     * - 只对全部为中文组成的片段做处理，添加符号$来表示该片段的起始；
     * - 基于窗口大小MaxLen且步长为1，切分该片段的可能的词（最大长度为MaxLen）；
     * 如，右临接：$三国演义$ 记录为：[$三国演义$, 国演义$, 演义$, 义$]
     * 如，左临接：$三国演义$ 记录为：[义演国三$, 演国三$, 国三$, 三$]
     * - 存储切分的可能组合的词语，到ngramFile文件；
     * <p>
     * Sort排序：
     * - 输入ngramFile文件和输出文件ngramSortFile，进行Sort；
     * - 将相同的词语放在一起。
     *
     * @param isRight       是否计算右临接词
     * @param reader        输入文件input
     * @param ngramFile     ngram存储文件
     * @param ngramSortFile ngramSort存储文件
     * @throws IOException 异常
     */
    private void genNgramAndSortFile(boolean isRight, BufferedReader reader,
                                     File ngramFile, File ngramSortFile) throws IOException {
        // 生成Ngram文件
        BufferedWriter ngramWriter = Files.newWriter(ngramFile, Charsets.UTF_8);
        String line = null;
        while (null != (line = reader.readLine())) {
            // 预处理：字符过滤
            if (StringUtils.isBlank(line)) {
                continue;
            }
            for (Map.Entry<String, String> entry : Constants.FILTER_MAP.entrySet()) {
                line = line.replaceAll(entry.getKey(), entry.getValue());
            }
            for (String section : Splitter.on(Constants.EMPTY).omitEmptyStrings().splitToList(line)) {
                if (isRight) {
                    section = section.trim();
                } else {
                    section = TooKits.reverse(section.trim());
                }
                // 只对全部为中文组成的短句做处理
                if (TooKits.allChinese(section)) {
                    // 一个片段对起始标识
                    section = Constants.CONNECTOR + section + Constants.CONNECTOR;
                    for (int i = 1; i < section.length() - 1; ++i) {
                        String l = section.substring(i, Math.min(this.maxLen + i, section.length())) + "\n";
                        // $三国演义$ 记录为：[$三国演义$, 国演义$, 演义$, 义$]
                        ngramWriter.write(l);
                    }
                }
            }
        }
        ngramWriter.close();
        LOGGER.info("ngramFile -> ngramSort, start sorting...");
        // 对ngramFile进行排序，输出为ngramSortFile
        sortFile(ngramFile, ngramSortFile);
    }

    public void sortFile(File in, File out) {
        try {
            long availMem = Runtime.getRuntime().maxMemory() - (40 * 1024 * 1024);
            long maxMem = (availMem >> 1);
            if (maxMem > Constants.MAX_HEAP_FOR_PRESORT) {
                maxMem = Constants.MAX_HEAP_FOR_PRESORT;
            } else if (maxMem < Constants.MIN_HEAP_FOR_PRESORT) {
                maxMem = Constants.MIN_HEAP_FOR_PRESORT;
            }
            SortConfig sortConfig = new SortConfig().withMaxMemoryUsage(maxMem);
            final TextFileSorter sorter = new TextFileSorter(sortConfig);
            sorter.sort(new FileInputStream(in), new PrintStream(out));
        } catch (IOException e) {
            LOGGER.error("sort file error, file = {}", in.toString(), e);
        }
    }

    /**
     * 对左和右邻字集合计算出对词频和信息熵合并：取信息熵最小的值
     *
     * @param rightFreqEntropyFile 右邻字集合计算出的词频和信息熵文件
     * @param leftFreqEntropyFile  左邻字集合计算出的信息熵文件（没有词频）
     * @return 合并后的词的信息熵文件
     */
    public String mergeEntropy(String rightFreqEntropyFile, String leftFreqEntropyFile) {
        File frFile = new File(rightFreqEntropyFile);
        File lFile = new File(leftFreqEntropyFile);
        File mergeTmpFile = new File(Constants.MERGE_FILE);
        File mergeTmpSortFile = new File(Constants.MERGE_SORT_FILE);
        File mergeFile = new File(Constants.MERGE_ENTROPY_FILE);
        try (BufferedReader rightReader = Files.newReader(frFile, Charsets.UTF_8);
             BufferedReader leftReader = Files.newReader(lFile, Charsets.UTF_8);
             BufferedWriter tempWriter = Files.newWriter(mergeTmpFile, Charsets.UTF_8);
             BufferedWriter mergeWriter = Files.newWriter(mergeFile, Charsets.UTF_8);) {
            // 将左右文件合并，并相同的排序在一起，存储到新文件mergeTmpSortFile
            String line = null;
            while (null != (line = rightReader.readLine())) {
                tempWriter.write(line + "\n");
            }
            line = null;
            while (null != (line = leftReader.readLine())) {
                tempWriter.write(line + "\n");
            }
            line = null;
            tempWriter.close();
            sortFile(mergeTmpFile, mergeTmpSortFile);

            // 加载排序后的文件
            BufferedReader br = Files.newReader(mergeTmpSortFile, Charsets.UTF_8);
            String line_2 = null;
            // 词的左/右计算的一个
            line = br.readLine();
            // 词的左/右计算的一个
            line_2 = br.readLine();
            while (true) {
                if (null == line || null == line_2) {
                    break;
                }
                String[] seg_1 = line.split("\t");
                String[] seg_2 = line_2.split("\t");
                // 排序后，如果正常右和左会同时存在，否则继续读取下一行
                if (!seg_1[0].equals(seg_2[0])) {
                    line = new String(line_2.getBytes());
                    line_2 = br.readLine();
                    continue;
                }
                // 为左邻字计算结果
                if (seg_1.length < 2) {
                    line = new String(line_2.getBytes());
                    line_2 = br.readLine();
                    continue;
                }
                // 左长度为2，右为3
                if (seg_1.length < 3 && seg_2.length < 3) {
                    continue;
                }
                double leftEntropy = seg_1.length == 2 ? Double.parseDouble(seg_1[1]) : Double.parseDouble(seg_2[1]);
                double rightEntropy = seg_1.length == 3 ? Double.parseDouble(seg_1[2]) : Double.parseDouble(seg_2[2]);
                int freq = seg_1.length == 3 ? Integer.parseInt(seg_1[1]) : Integer.parseInt(seg_2[1]);
                // 选择最小信息熵
                double entropy = Math.min(leftEntropy, rightEntropy);
                mergeWriter.write(seg_1[0] + "\t" + freq + "\t" + entropy + "\n");

                // 读取下一个词的左或右
                line = br.readLine();
                line_2 = br.readLine();
            }
            mergeWriter.close();
        } catch (Exception e) {
            LOGGER.error("merge right and left entropy error.", e);
        }
        return mergeFile.toString();
    }

    /**
     * 新词抽取，基于词频排序输出：[词，词频，互信息，左右熵，位置成词概率]
     *
     * @param rightFreqSortFile 排序后的右邻字集合计算结果文件
     * @param entropyFile       左右合并后的信息熵文件
     */
    public void extractWords(String rightFreqSortFile, String entropyFile) {
        LOGGER.info("start to extract words");
        // 加载位置成词概率数据
        TreeMap<String, double[]> posProp = PositionProbability.loadPosProp();
        // radix tree是一种多叉搜索树，树的叶子结点是实际的数据条目。
        // 每个结点有一个固定的、2^n指针指向子结点（每个指针称为槽slot），并有一个指针指向父结点。
        RadixTree<Integer> tree = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());
        File eFile = new File(entropyFile);
        File wordsFile = new File(Constants.WORDS_FILE);
        File wordsSortFile = new File(Constants.WORDS_SORT_FILE);
        try (BufferedReader fr = Files.newReader(new File(rightFreqSortFile), Charsets.UTF_8);
             BufferedReader er = Files.newReader(eFile, Charsets.UTF_8);
             BufferedWriter ww = Files.newWriter(wordsFile, Charsets.UTF_8);) {
            String line = null;
            // 词为单个字时的总词频
            long total = 0;
            long epoch = 0;
            // 遍历原始的右邻字集合的计算结果
            while (null != (line = fr.readLine())) {
                String[] seg = line.split("\t");
                if (seg.length == 3) {
                    // [词，词频]
                    tree.put(seg[0], Integer.parseInt(seg[1]));
                    epoch += 1;
                    // all single char's frequency 词为单个字时
                    if (seg[0].length() < 2) {
                        // 词为单个字时的总词频，也就是单字出现单总次数
                        total += Integer.parseInt(seg[1]);
                    }
                    if (epoch % 1000 == 0) {
                        LOGGER.info("load freq to radix tree done: " + total);
                    }
                }
            }
            LOGGER.info("[词典] build freq TST done!");

            line = null;
            int cnt = 0;
            // 遍历合并后的信息熵文件
            while (null != (line = er.readLine())) {
                cnt += 1;
                if (cnt % 1000 == 0) {
                    LOGGER.info("extract words now: " + cnt);
                }
                String[] seg = line.split("\t");
                if (seg.length == 3) {
                    String w = seg[0];
                    // 词的构成全部为字母或数字时过滤
                    if (!TooKits.allLetterOrNumber(w)) {
                        // 词频
                        int f = Integer.parseInt(seg[1]);
                        // 信息熵
                        double e = Double.parseDouble(seg[2]);
                        long max = -1;
                        for (int s = 1; s < w.length(); ++s) {
                            String lw = w.substring(0, s);
                            String rw = w.substring(s);
                            int lf = tree.getValueForExactKey(lw);
                            int rf = tree.getValueForExactKey(rw);
                            long ff = lf * rf;
                            if (ff > max) {
                                max = ff;
                            }
                        }
                        /*
                         * PMI公式疑惑： https://github.com/sing1ee/dict_build/issues/31
                         * 互信息（值越大表示x和y越相关）
                         * 公式：PMI(x,y) = log(p(x,y) / p(x)p(y) )
                         * 其中：p(x,y) = freq(x, y)/total;  p(x) = freq(x)/total; p(y) = freq(y)/total
                         *  p(x,y) / p(x)p(y) = (freq(x, y) * total) / (freq(x) * freq(y))
                         * 即PMI(x,y)= log((freq(x, y) * total) / (freq(x) * freq(y)))
                         */
                        double pf = f * total / max;
                        double pmi = TooKits.log2(pf);
                        if (Double.isNaN(pmi)) {
                            continue;
                        }
                        double pp = -1;
                        // 首字
                        CharSequence w1 = w.subSequence(0, 1);
                        //尾字
                        CharSequence w2 = w.subSequence(w.length() - 1, w.length());
                        double[] w1P = posProp.get(w1);
                        double[] w2P = posProp.get(w2);
                        if (null != w1P && null != w2P) {
                            // 取首字/尾字的最小位置概率
                            pp = Math.min(w1P[0], w2P[2]);
                        }
                        // 阈值限制条件：互信息、信息熵（左右信息熵最小值）、成词位置概率
                        if (pmi < 1 || e < 2 || pp < 0.1) {
                            continue;
                        }
                        // [词，词频，互信息，左右熵，位置成词概率]
                        ww.write(w + "\t" + f + "\t" + pmi + "\t" + e + "\t" + pp + "\n");
                    }
                }
            }
            ww.close();

            try {
                LOGGER.info("start to sort extracted words");
                long availMem = Runtime.getRuntime().maxMemory() - (2048 * 1024 * 1024);
                long maxMem = (availMem >> 1);
                if (maxMem > Constants.MAX_HEAP_FOR_PRESORT) {
                    maxMem = Constants.MAX_HEAP_FOR_PRESORT;
                } else if (maxMem < Constants.MIN_HEAP_FOR_PRESORT) {
                    maxMem = Constants.MIN_HEAP_FOR_PRESORT;
                }
                final SplitFileSorter sorter = new SplitFileSorter(new SortConfig().withMaxMemoryUsage(maxMem));
                sorter.sort(new FileInputStream(wordsFile), new PrintStream(wordsSortFile));
            } catch (IOException e) {
                LOGGER.error("extract words error.", e);
            }
        } catch (Exception e) {
            LOGGER.error("before extract words error.", e);
        }
        LOGGER.info("all done");
    }

}
