package IO;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class MappedFileDemo {
    public static void main(String[] args) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile("mapped.dat", "rw");
             FileChannel channel = raf.getChannel()) {

            // 内存映射文件
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 1024);

            // 写入数据
            buffer.putInt(123);
            buffer.putDouble(3.14);

            // 读取数据
            buffer.flip();
            System.out.println(buffer.getInt());
            System.out.println(buffer.getDouble());
        }
    }
}
