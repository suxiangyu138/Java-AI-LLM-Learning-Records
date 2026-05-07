package IO;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CharsetDemo {
    public static void main(String[] args) throws IOException {
        String content = "你好，世界！";

        // UTF-8 编码写入
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream("utf8.txt"), StandardCharsets.UTF_8)) {
            writer.write(content);
        }

        // GBK 编码写入
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream("gbk.txt"), "GBK")) {
            writer.write(content);
        }

        // 按对应编码读取
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream("gbk.txt"), "GBK"))) {
            System.out.println(reader.readLine());
        }
    }
}