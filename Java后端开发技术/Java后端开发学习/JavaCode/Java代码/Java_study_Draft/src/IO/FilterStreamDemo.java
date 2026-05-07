package IO;


import java.io.*;

public class FilterStreamDemo {
    public static void main(String[] args) throws IOException {
        // 组合：文件输入流 + 缓冲 + 数据输入流
        File file = new File("data.bin");

        // 1. 先创建并写入数据（解决文件不存在问题）
        if (!file.exists()) {
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(file)))) {
                // 写入示例数据
                out.writeInt(123);    // 写入int类型数据
                out.writeUTF("Hello");// 写入UTF字符串
            }
        }

        // 2. 然后读取数据
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(
                        new FileInputStream(file)))) {
            int num = in.readInt();
            String str = in.readUTF();
            System.out.println(num + " " + str); // 输出：123 Hello
        }

        // 组合：文件输出流 + 缓冲 + 数据输出流（可单独测试写入）
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream("data.bin")))) {
            out.writeInt(123);
            out.writeUTF("Hello");
        }
    }
}