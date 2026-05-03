package IO;

import java.nio.file.Files;
import java.nio.file.Paths;

public class FilesCreateDemo {
    public static void main(String[] args) throws Exception {
        // 创建文件
        Files.createFile(Paths.get("new_file.txt"));

        // 创建单级目录
        Files.createDirectory(Paths.get("new_dir"));

        // 创建多级目录
        Files.createDirectories(Paths.get("parent", "child", "grandchild"));
    }
}