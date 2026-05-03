package IO;

import java.nio.file.*;
import java.io.IOException;

public class DirectoryStreamDemo {
    public static void main(String[] args) throws IOException {
        Path dir = Paths.get(".");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path entry : stream) {
                System.out.println(entry.getFileName());
            }
        }
    }
}