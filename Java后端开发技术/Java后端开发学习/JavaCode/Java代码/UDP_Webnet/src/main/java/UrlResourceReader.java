import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * 用 URL 读取网络资源
 * 功能：读取指定网页的内容
 */
public class UrlResourceReader {
    public static void main(String[] args) {
        // 要读取的 URL（示例：百度首页）
        String urlStr = "https://jwc.ahu.edu.cn/";

        try {
            // 1. 创建 URL 对象
            URL url = new URL(urlStr);
            // 2. 打开连接，获取 URLConnection
            URLConnection connection = url.openConnection();
            // 3. 设置请求头（模拟浏览器访问）
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // 4. 读取网页内容
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8")
            )) {
                String line;
                System.out.println("===== 网页内容 =====");
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            System.err.println("读取网络资源失败：" + e.getMessage());
        }
    }
}