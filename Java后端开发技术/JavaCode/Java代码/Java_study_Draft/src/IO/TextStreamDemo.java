package IO;

import java.io.*;

public class TextStreamDemo {
    public static void main(String[] args) throws IOException {
        // 写入文本（BufferedWriter + FileWriter）
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("text.txt"))) {
            writer.write("第一行");
            writer.newLine();
            writer.write("第二行");
        }

        // 读取文本（BufferedReader + FileReader）
        try (BufferedReader reader = new BufferedReader(new FileReader("text.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}