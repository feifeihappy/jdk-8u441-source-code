package org.example.test.stack;

/**
 * 指令重排测试
 */
public class CommandDisorder {

    // 当使用volatile关键词修饰变量时，则不会出现指令重排现象
    private static /*volatile*/ int a = 0, b = 0, c = 0, d = 0;

    /**
     * 测试方式：一次开启两个线程，同时修改变量
     */
    public static void main(String[] args) throws InterruptedException {

        int i = 0;
        while (true) {

            i++;
            a = b = c = d = 0;
            Thread t1 = new Thread(() -> {

                a = 1;
                c = b; // 指令重排，会先执行这行代码,导致c = 0, d = 0
            });
            Thread t2 = new Thread(() -> {

                b = 1;
                d = a; // 指令重排，会先执行这行代码,导致c = 0, d = 0
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            if (c == 0 && d == 0) {

                System.err.println(String.format("第%s次出现指令重排", i));
                break;
            } else {

                System.out.println(i);
            }
        }
    }
}