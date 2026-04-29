import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * TCP 客户端
 * 功能：连接本地 8888 端口，发送消息给服务端
 */
public class TcpEchoClient {
    public static void main(String[] args) {
        // 服务端 IP 和端口（本地测试用 127.0.0.1）
        String serverIp = "127.0.0.1";
        int port = 8888;

        // 1. 创建 Socket，连接服务端
        try (Socket socket = new Socket(serverIp, port)) {
            System.out.println("✅ 已连接到 TCP 服务端");

            // 2. 获取输入输出流
            try (
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream())
                    );
                    Scanner scanner = new Scanner(System.in)
            ) {
                String userInput;
                // 3. 循环读取用户输入，发送给服务端
                while (true) {
                    System.out.print("请输入消息（输入 exit 退出）：");
                    userInput = scanner.nextLine();
                    // 发送消息给服务端
                    out.println(userInput);
                    // 读取服务端返回的消息
                    String serverMsg = in.readLine();
                    System.out.println("服务端返回：" + serverMsg);
                    // 输入 exit 退出循环
                    if ("exit".equalsIgnoreCase(userInput)) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.err.println("连接服务端失败：" + e.getMessage());
            System.err.println("检查：1. 服务端是否已启动  2. 端口是否正确");
        }
    }
}