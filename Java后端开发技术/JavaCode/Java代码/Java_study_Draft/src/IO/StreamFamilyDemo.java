package IO;

import java.io.*;

public class StreamFamilyDemo {
    public static void main(String[] args) throws IOException {
        // 字节流：FileInputStream/FileOutputStream
        InputStream fileIn = new FileInputStream("test.txt");
        OutputStream fileOut = new FileOutputStream("copy.txt");

        // 过滤流：BufferedInputStream/BufferedOutputStream（缓冲）
        InputStream bufferedIn = new BufferedInputStream(fileIn);
        OutputStream bufferedOut = new BufferedOutputStream(fileOut);

        // 字符流：InputStreamReader/OutputStreamWriter（字节转字符）
        Reader reader = new InputStreamReader(bufferedIn);
        Writer writer = new OutputStreamWriter(bufferedOut);

    }
}