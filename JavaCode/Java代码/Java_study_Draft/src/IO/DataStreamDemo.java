package IO;

import java.io.*;

public class DataStreamDemo {
    public static void main(String[] args) throws IOException {
        // 写入二进制数据
        try (DataOutputStream out = new DataOutputStream(
                new FileOutputStream("binary.dat"))) {
            out.writeInt(123);
            out.writeDouble(3.14);
            out.writeUTF("Java");
        }

        // 读取二进制数据
        try (DataInputStream in = new DataInputStream(
                new FileInputStream("binary.dat"))) {
            int num = in.readInt();
            double pi = in.readDouble();
            String str = in.readUTF();
            System.out.println(num + " " + pi + " " + str);
        }
    }
}