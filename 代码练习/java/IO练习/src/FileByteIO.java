import java.io.*;

/**
 * 字节流读写二进制文件（如图片、视频）
 * 功能：复制一张图片
 */
public class FileByteIO {
    public static void main(String[] args) {
        String srcPath = "source.jpg"; // 源文件路径
        String destPath = "copy.jpg"; // 目标文件路径

        try (
                FileInputStream fis = new FileInputStream(srcPath);
                FileOutputStream fos = new FileOutputStream(destPath);
                BufferedInputStream bis = new BufferedInputStream(fis);
                BufferedOutputStream bos = new BufferedOutputStream(fos)
        ) {
            byte[] buffer = new byte[1024]; // 缓冲区：一次读写1024字节
            int len;
            // 读取数据到缓冲区，返回实际读取的字节数，-1表示文件末尾
            while ((len = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, len); // 写入缓冲区数据
            }
            System.out.println("图片复制成功！");
        } catch (FileNotFoundException e) {
            System.err.println("源文件不存在！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}