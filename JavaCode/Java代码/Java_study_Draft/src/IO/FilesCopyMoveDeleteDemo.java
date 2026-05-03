package IO;

import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.*;

public class FilesCopyMoveDeleteDemo {
    public static void main(String[] args) throws Exception {
        // 复制文件（覆盖已存在）
        Files.copy(Paths.get("test.txt"), Paths.get("copy_test.txt"), REPLACE_EXISTING);

        // 移动文件（重命名）
        Files.move(Paths.get("copy_test.txt"), Paths.get("moved_test.txt"), REPLACE_EXISTING);

        // 删除文件
        Files.delete(Paths.get("moved_test.txt"));
    }
}