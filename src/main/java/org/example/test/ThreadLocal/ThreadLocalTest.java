package org.example.test.ThreadLocal;

/**
 * description：TODO
 * time：2026/2/6 10:17
 * auther：zhaopengfei
 */
public class ThreadLocalTest {
    public static void main(String[] args) {
        ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
        threadLocal.set(1);
        System.out.println(threadLocal.get());
        threadLocal.remove();
    }
}
