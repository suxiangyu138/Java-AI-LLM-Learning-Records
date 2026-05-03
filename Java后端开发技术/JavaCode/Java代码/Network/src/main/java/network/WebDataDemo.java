package network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 4.3 获取Web数据：URL/URLConnection 实现HTTP GET/POST
 */
public class WebDataDemo {
    public static void main(String[] args) {
        HttpURLConnection conn = null;
        HttpURLConnection postConn = null;

        try {
            // 4.3.1/4.3.2：GET请求获取网页内容
            String getUrl = "https://www.baidu.com";
            URL url = new URL(getUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000); // 连接超时
            conn.setReadTimeout(10000);    // 读取超时

            // 读取响应
            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String line;
                StringBuilder content = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
                in.close();
                System.out.println("GET响应内容（前500字符）：" +
                        (content.length() >= 500 ? content.substring(0, 500) : content.toString()));
            }

            // 4.3.3：POST提交表单数据
            String postUrl = "https://httpbin.org/post";
            URL postUrlObj = new URL(postUrl);
            postConn = (HttpURLConnection) postUrlObj.openConnection();
            postConn.setRequestMethod("POST");
            postConn.setDoOutput(true);
            postConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            postConn.setRequestProperty("User-Agent", "Java Client");

            // 构造表单数据
            String formData = "username=test&password=123456";
            try (OutputStream os = postConn.getOutputStream()) {
                byte[] input = formData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取POST响应
            BufferedReader postIn = new BufferedReader(
                    new InputStreamReader(postConn.getInputStream(), StandardCharsets.UTF_8));
            String postLine;
            StringBuilder postContent = new StringBuilder();
            while ((postLine = postIn.readLine()) != null) {
                postContent.append(postLine);
            }
            postIn.close();
            System.out.println("\nPOST响应：" + postContent);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
            if (postConn != null) postConn.disconnect();
        }
    }
}
