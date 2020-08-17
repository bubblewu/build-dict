package com.bubble.common;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;

/**
 * 工具包
 *
 * @author wugang
 * date: 2020-08-13 18:25
 **/
public class TooKits {
    private static final Logger LOGGER = LoggerFactory.getLogger(TooKits.class);

    public static String readFile(File file) {
        try {
            BufferedReader bufferedReader = Files.newReader(file, Charsets.UTF_8);
            return bufferedReader.readLine();
        } catch (Exception e) {
            LOGGER.error("read file error. file = {}", file.toString(), e);
        }
        return null;
    }

    /**
     * 输入的字符是否是汉字
     *
     * @param a char
     * @return boolean
     */
    public static boolean isChinese(char a) {
        //  [0x4e00, 0x29fa5]
//        return ((int) a >= 19968 && (int) a <= 40869);
        return ((int) a >=19968 && (int) a <= 171941);
    }

    /**
     * 是否全为中文
     *
     * @param s text
     * @return boolean
     */
    public static boolean allChinese(String s) {
        if (null == s || "".equals(s.trim())) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!isChinese(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String reverse(String raw) {
        StringBuilder bui = new StringBuilder();
        for (int i = raw.length() - 1; i >= 0; --i) {
            bui.append(raw.charAt(i));
        }
        return bui.toString();
    }

    public static double log2(double N) {
        // Math.log的底为e
        return Math.log(N) / Math.log(2);
    }

    /**
     * 词的构成全部为字母或数字时返回true
     *
     * @param w 词语
     * @return boolean
     */
    public static boolean allLetterOrNumber(String w) {
        for (char c : w.toLowerCase().toCharArray()) {
            boolean letter = c >= 'a' && c <= 'z';
            boolean digit = c >= '0' && c <= '9';
            if (!letter && !digit) {
                return false;
            }
        }
        return true;
    }

}
