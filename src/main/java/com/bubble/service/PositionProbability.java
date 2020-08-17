package com.bubble.service;

import com.bubble.common.Constants;
import com.bubble.common.CounterMap;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 基于搜狗词典的位置成词概率计算
 *
 * @author wugang
 * date: 2020-08-17 14:39
 **/
public class PositionProbability {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionProbability.class);

    public static void main(String[] args) {
        genData();
    }

    private static void genData() {
        File dictFile = new File("src/main/resources/sougou/SougouLabDic.dic");
        // 词首的概率 = 某个字出现在次首的次数 / 某个字出现的总次数
        File positionProFile = new File(dictFile.getParentFile(), "/position_prop.txt");
        try (BufferedReader br = Files.newReader(dictFile, Charsets.UTF_8);
             BufferedWriter pw = Files.newWriter(positionProFile, Charsets.UTF_8);
        ) {
            String line = null;
            Map<String, CounterMap> wordMap = Maps.newHashMap();
            while (null != (line = br.readLine())) {
                String[] seg = line.split("\t");
                String word = seg[0];
                int freq = 1;
                for (int i = 0; i < word.length(); i++) {
                    String label;
                    // SEM位置标注，并记录每个字出现位置的次数
                    if (0 == i) {
                        label = "S";
                    } else if (word.length() - 1 == i) {
                        label = "E";
                    } else {
                        label = "M";
                    }
                    String key = word.substring(i, i + 1);
                    if (wordMap.containsKey(key)) {
                        wordMap.get(key).incrby(label, freq);
                    } else {
                        CounterMap cm = new CounterMap();
                        cm.incrby(label, freq);
                        wordMap.put(key, cm);
                    }
                }
            }

            String[] labels = new String[]{"S", "M", "E"};
            for (String key : wordMap.keySet()) {
                // 该字出现的总次数
                int total = 0;
                for (String l : labels) {
                    total += wordMap.get(key).get(l);
                }
                if (0 == total) {
                    continue;
                }
                StringBuilder bui = new StringBuilder();
                bui.append(key);
                for (String l : labels) {
                    double p = wordMap.get(key).get(l) * 1.0 / total;
                    bui.append("\t").append(p);
                }
                bui.append("\n");
                pw.write(bui.toString());
            }
        } catch (Exception e) {
            LOGGER.error("read or write error.", e);
        }
    }

    /**
     * 加载位置成词概率数据
     * <p>
     * pos_prop.txt 是统计的结果，来自搜狗词库的统计结果，
     * 三列数值分别是单字出现在词首，词中，词尾的概率。作为一个判断的维度。
     * 计算方式为：
     * - 词首的概率 = 某个字出现在次首的次数 / 某个字出现的总次数
     *
     * @return TreeMap
     */
    public static TreeMap<String, double[]> loadPosProp() {
        TreeMap<String, double[]> prop = Maps.newTreeMap();
        try {
            LOGGER.info("加载位置成词概率数据: {}", DictBuilder.class.getResourceAsStream(Constants.POSITION_PROBABILITY_FILE));
            BufferedReader br = new BufferedReader(new InputStreamReader(PositionProbability.class.getResourceAsStream(Constants.POSITION_PROBABILITY_FILE), StandardCharsets.UTF_8));
            String line = null;
            while (null != (line = br.readLine())) {
                String[] seg = line.split("\t");
                prop.put(seg[0], new double[]{Double.parseDouble(seg[1]), Double.parseDouble(seg[2]), Double.parseDouble(seg[3])});
            }
        } catch (IOException e) {
            LOGGER.error("load {} error", Constants.POSITION_PROBABILITY_FILE, e);
        }
        return prop;
    }

}
