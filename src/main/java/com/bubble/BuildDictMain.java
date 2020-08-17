package com.bubble;

import com.bubble.service.DictBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;


/**
 * 主入口
 *
 * @author wugang
 * date: 2020-08-13 17:52
 **/
public class BuildDictMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildDictMain.class);

    public static void main(String[] args) {
        Instant begin = Instant.now();
        String textFile = "data/隋唐演义.txt";
        DictBuilder builder = new DictBuilder(6);
        String right = builder.genRight(textFile);
        String left = builder.genLeft(textFile);
        String entropyFile = builder.mergeEntropy(right, left);
        builder.extractWords(right, entropyFile);
        LOGGER.info("total costs {} ms", Duration.between(begin, Instant.now()).toMillis());
    }

}
