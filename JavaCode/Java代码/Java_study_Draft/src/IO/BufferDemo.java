package IO;

import java.nio.*;

public class BufferDemo {
    public static void main(String[] args) {
        IntBuffer buffer = IntBuffer.allocate(10);

        // 写入
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);

        // 切换到读模式
        buffer.flip();

        // 读取
        while (buffer.hasRemaining()) {
            System.out.println(buffer.get());
        }
    }
}