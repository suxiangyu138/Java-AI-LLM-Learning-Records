import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP 服务端
 * 功能：监听 8888 端口，接收客户端消息并原样返回（Echo）
 */
public class TcpEchoServer {
    public static void main(String[] args) {
        // 1. 创建 ServerSocket，绑定端口 8888
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("TCP 服务端已启动，监听端口 8888...");

            // 2. 阻塞等待客户端连接（accept() 方法会一直等待，直到有客户端连接）
            Socket clientSocket = serverSocket.accept();
            System.out.println("✅ 客户端已连接：" + clientSocket.getInetAddress());

            // 3. 获取输入输出流（BufferedReader 读消息，PrintWriter 写消息）
            try (
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream())
                    );
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String clientMsg;
                // 4. 循环读取客户端消息，直到客户端发送 "exit"
                while ((clientMsg = in.readLine()) != null) {
                    System.out.println("客户端发送：" + clientMsg);
                    // 客户端发送 exit，关闭连接
                    if ("exit".equalsIgnoreCase(clientMsg)) {
                        out.println("服务端：连接关闭");
                        break;
                    }
                    // 5. 原样返回消息（Echo）
                    out.println("服务端 Echo：" + clientMsg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientSocket.close();
                System.out.println("❌ 客户端连接已关闭");
            }

        } catch (IOException e) {
            System.err.println("服务端启动失败：" + e.getMessage());
        }
    }
}