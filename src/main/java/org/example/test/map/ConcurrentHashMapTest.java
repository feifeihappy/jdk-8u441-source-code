package org.example.test.map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * description：TODO
 * time：2026/2/1 14:53
 * auther：zhaopengfei
 */
public class ConcurrentHashMapTest {
    public static void main(String[] args) {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("1", "1");
        String s = map.get("1");
        System.out.println(s);
    }
}
