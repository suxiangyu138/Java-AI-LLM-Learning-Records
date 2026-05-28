package IO;

import java.io.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;

public class FileLockDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        try (RandomAccessFile raf = new RandomAccessFile("lock.dat", "rw");
             FileChannel channel = raf.getChannel()) {

            // 获取独占锁
            try (FileLock lock = channel.lock()) {
                System.out.println("获取到锁，开始写入...");
                channel.write(ByteBuffer.wrap("Locked data".getBytes()));
                Thread.sleep(5000); // 模拟持有锁
            }
            System.out.println("锁已释放");
        }
    }
}
