package IO;

import java.io.*;

public class ByteStreamDemo {
    public static void main(String[] args) throws IOException {
        // 写入字节
        try (OutputStream out = new FileOutputStream("test.txt")) {
            out.write("Hello, IO!".getBytes());
        }

        // 读取字节
        try (InputStream in = new FileInputStream("test.txt")) {
            int data;
            while ((data = in.read()) != -1) {
                System.out.print((char) data);
            }
        }
    }
}