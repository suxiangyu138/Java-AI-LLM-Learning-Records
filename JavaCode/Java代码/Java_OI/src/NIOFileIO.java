import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * NIO Files 工具类快速读写文件
 * 适用于小文件快速操作
 */
public class NIOFileIO {
    public static void main(String[] args) {
        String filePath = "nio_test.txt";

        // ========== 1. 写入文件 ==========
        String content = "Java NIO 快速读写文件\n一行代码搞定！";
        try {
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("NIO 写入文件成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ========== 2. 读取文件 ==========
        try {
            // 读取所有行到 List<String>
            List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
            System.out.println("\n===== NIO 读取文件内容 =====");
            lines.forEach(System.out::println);

            // 读取所有字节到 byte[]
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            String content2 = new String(bytes, StandardCharsets.UTF_8);
            System.out.println("\n读取所有字节：" + content2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}