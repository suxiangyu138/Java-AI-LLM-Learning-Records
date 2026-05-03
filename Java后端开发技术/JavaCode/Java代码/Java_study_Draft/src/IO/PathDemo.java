package IO;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathDemo {
    public static void main(String[] args) {
        Path path = Paths.get("dir", "file.txt");
        System.out.println("路径：" + path);
        System.out.println("父路径：" + path.getParent());
        System.out.println("文件名：" + path.getFileName());
        System.out.println("根路径：" + path.getRoot());
    }
}