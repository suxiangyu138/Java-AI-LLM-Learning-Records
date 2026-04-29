package IO;

import java.nio.file.*;
import java.util.Map;

public class ZipFileSystemDemo {
    public static void main(String[] args) throws Exception {
        Path zipPath = Paths.get("test.zip");
        Map<String, String> env = Map.of("create", "true");

        // 创建 ZIP 文件系统
        try (FileSystem fs = FileSystems.newFileSystem(zipPath, env)) {
            // 在 ZIP 中创建文件
            Files.write(fs.getPath("zip_file.txt"), "Hello from ZIP!".getBytes());

            // 读取 ZIP 中的文件
            byte[] bytes = Files.readAllBytes(fs.getPath("zip_file.txt"));
            System.out.println(new String(bytes));
        }
    }
}