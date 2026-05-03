package IO;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FilesReadWriteDemo {
    public static void main(String[] args) throws Exception {
        // 写入文件
        Files.write(Paths.get("files_demo.txt"), "Hello, NIO!".getBytes());

        // 读取文件所有行
        List<String> lines = Files.readAllLines(Paths.get("files_demo.txt"));
        lines.forEach(System.out::println);

        // 读取文件所有字节
        byte[] bytes = Files.readAllBytes(Paths.get("files_demo.txt"));
        System.out.println(new String(bytes));
    }
}