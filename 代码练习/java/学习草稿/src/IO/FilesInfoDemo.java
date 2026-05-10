package IO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

public class FilesInfoDemo {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get("test.txt");
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

        System.out.println("创建时间：" + attrs.creationTime());
        System.out.println("最后修改时间：" + attrs.lastModifiedTime());
        System.out.println("文件大小：" + attrs.size() + " 字节");
        System.out.println("是否是目录：" + attrs.isDirectory());
        System.out.println("是否是常规文件：" + attrs.isRegularFile());
    }
}