package org.example.test;

import java.io.InputStreamReader;

/**
 * description：TODO
 * time：2025/5/7 14:15
 * auther：zhaopengfei
 */
class InputStreamReaderTest {

    public static void main(String[] args) {
        //将字节流转换为字符流、
        InputStreamReader inputStreamReader = new InputStreamReader(System.in);
        try {

            while (true) {
                int read = inputStreamReader.read();
                if (read == -1) {
                    break;
                }
                System.out.println((char) read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}