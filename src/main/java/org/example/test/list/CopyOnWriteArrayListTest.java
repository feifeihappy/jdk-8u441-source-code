package org.example.test.list;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * description：TODO
 * time：2026/2/3 17:58
 * auther：zhaopengfei
 */
public class CopyOnWriteArrayListTest {
    public static void main(String[] args) {

        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("1");
        list.get(0);
    }
}
