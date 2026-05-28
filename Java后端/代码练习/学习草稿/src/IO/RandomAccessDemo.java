package IO;

import java.io.*;

public class RandomAccessDemo {
    public static void main(String[] args) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile("raf.dat", "rw")) {
            // 写入数据
            raf.writeInt(100);
            raf.writeDouble(99.99);

            // 移动到文件开头读取
            raf.seek(0);
            System.out.println(raf.readInt());
            System.out.println(raf.readDouble());

            // 在指定位置写入
            raf.seek(4); // 跳过 int（4字节）
            raf.writeDouble(88.88);
        }
    }
}