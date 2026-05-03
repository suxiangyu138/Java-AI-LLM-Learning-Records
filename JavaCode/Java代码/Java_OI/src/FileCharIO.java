import java.io.*;

/**
 * 字符流读写文本文件
 * 功能：写入文本到文件 + 读取文件内容
 */
public class FileCharIO {
    public static void main(String[] args) {
        String filePath = "test_char.txt"; // 文件路径（项目根目录）

        // ========== 1. 写入文本到文件 ==========
        try (
                // 自动关闭资源（try-with-resources 语法，JDK7+支持）
                FileWriter writer = new FileWriter(filePath, true); // true 表示追加写入
                BufferedWriter bufferedWriter = new BufferedWriter(writer) // 缓冲流提升效率
        ) {
            bufferedWriter.write("Java 输入输出教程");
            bufferedWriter.newLine(); // 换行
            bufferedWriter.write("字符流读写文本文件");
            System.out.println("文本写入成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ========== 2. 读取文本文件内容 ==========
        try (
                FileReader reader = new FileReader(filePath);
                BufferedReader bufferedReader = new BufferedReader(reader)
        ) {
            System.out.println("\n===== 读取文件内容 =====");
            String line;
            // 按行读取，直到文件末尾（返回null）
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}