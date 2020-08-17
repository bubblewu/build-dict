package com.bubble.common;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CounterMap词数统计
 *
 * @author wugang
 * date: 2020-08-14 14:48
 **/
public class CounterMap implements Serializable {
    private static final long serialVersionUID = -2956724253324773232L;

    private Map<String, Integer> count = new ConcurrentHashMap<>();

    public CounterMap() {
    }

    public CounterMap(int capacitySize) {
        count = new ConcurrentHashMap<>(capacitySize);
    }

    public void incr(String key) {
        if (count.containsKey(key)) {
            count.put(key, count.get(key) + 1);
        } else {
            count.put(key, 1);
        }
    }

    public void incrby(String key, int delta) {
        if (count.containsKey(key)) {
            count.put(key, count.get(key) + delta);
        } else {
            count.put(key, delta);
        }
    }

    public int get(String key) {
        Integer value = count.get(key);
        if (null == value) {
            return 0;
        }
        return value;
    }

    public Map<String, Integer> countAll() {
        return count;
    }

}
