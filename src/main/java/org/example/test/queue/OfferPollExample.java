package org.example.test.queue;

import java.util.concurrent.ArrayBlockingQueue;

public class OfferPollExample {

    public static void main(String[] args) {
        // 创建一个大小为 3 的 ArrayBlockingQueue
        ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(3);

        // 向队列中添加元素
        System.out.println(queue.offer("A"));
        System.out.println(queue.offer("B"));
        System.out.println(queue.offer("C"));

        // 尝试向队列中添加元素，但队列已满，返回 false
        System.out.println(queue.offer("D"));

        // 从队列中取出元素
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());

        // 尝试从队列中取出元素，但队列已空，返回 null
        System.out.println(queue.poll());
    }

}