package IO;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class FilesListDemo {
    public static void main(String[] args) throws Exception {
        // 列出目录下的直接子项
        try (Stream<java.nio.file.Path> stream = Files.list(Paths.get("."))) {
            stream.forEach(System.out::println);
        }
    }
}