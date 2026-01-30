package org.example.test;

import java.util.HashMap;

/**
 * description：TODO
 * time：2026/1/30 09:37
 * auther：zhaopengfei
 */
public class HashMapTest {
    public static void main(String[] args) {
        HashMap<String, String> map = new HashMap<>();
        map.put("1", "1");
        System.out.println( map);
        map.get("1");
    }
}
