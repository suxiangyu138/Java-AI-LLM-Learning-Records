package IO;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TextOutputDemo {
    public static void main(String[] args) throws IOException {
        // 指定字符集写入
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream("utf8.txt"), StandardCharsets.UTF_8))) {
            writer.write("你好，世界！");
        }

        // PrintWriter 简化写入
        try (PrintWriter pw = new PrintWriter("print.txt", StandardCharsets.UTF_8)) {
            pw.println("Hello");
            pw.printf("数字：%d%n", 123);
        }
    }
}