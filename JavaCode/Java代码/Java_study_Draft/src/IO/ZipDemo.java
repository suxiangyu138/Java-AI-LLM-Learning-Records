package IO;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.*;

public class ZipDemo {
    public static void main(String[] args) throws IOException {
        // 压缩文件
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream("test.zip"))) {
            File file = new File("test.txt");
            ZipEntry entry = new ZipEntry(file.getName());
            zos.putNextEntry(entry);
            Files.copy(file.toPath(), zos);
            zos.closeEntry();
        }

        // 解压文件
        try (ZipInputStream zis = new ZipInputStream(
                new FileInputStream("test.zip"))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File("unzip_" + entry.getName());
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    zis.transferTo(fos);
                }
                zis.closeEntry();
            }
        }
    }
}