import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TcpClient {
    // 服务端地址和端口
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        // 1. 连接服务端
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println("✅ 已成功连接到服务端：" + SERVER_IP + ":" + SERVER_PORT);
            System.out.println("💡 输入消息发送（输入 exit 退出）\n");

            // 2. 获取输入输出流（修复：用 StandardCharsets.UTF_8 代替字符串）
            try (
                    // PrintWriter 正确写法
                    PrintWriter out = new PrintWriter(
                            socket.getOutputStream(),
                            true,  // autoFlush
                            StandardCharsets.UTF_8  // 这里用 Charset 类型
                    );
                    // BufferedReader 正确写法
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8  // 这里用 Charset 类型
                            )
                    );
                    Scanner scanner = new Scanner(System.in)
            ) {
                String userInput;
                // 循环读取用户输入并发送
                while (true) {
                    System.out.print("请输入消息：");
                    userInput = scanner.nextLine();

                    // 发送消息到服务端
                    out.println(userInput);

                    // 读取服务端响应
                    String serverResponse = in.readLine();
                    System.out.println("📤 服务端响应：" + serverResponse);

                    // 输入exit退出
                    if ("exit".equalsIgnoreCase(userInput)) {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("❌ 连接服务端失败：" + e.getMessage());
            System.err.println("🔍 检查：1. 服务端是否已启动  2. IP/端口是否正确");
        }
    }
}