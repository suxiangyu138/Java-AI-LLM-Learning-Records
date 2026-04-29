package network;

import java.io.*;
import java.net.*;

/**
 * 适配 Java 8 的 TCP 服务器（移除 Java 9+ 专属语法）
 */
public class TCPServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8888)) { // Java 8 支持的 try-with-resources（声明+初始化）
            System.out.println("服务器启动，等待客户端连接...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功：" + clientSocket.getInetAddress());

                // 多线程处理客户端
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 处理单个客户端通信（完全适配 Java 8）
    private static void handleClient(Socket socket) {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            // 1. 初始化流资源（Java 8 需手动声明，不能直接在 try 中引用已有 socket 变量）
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // autoFlush=true

            // 2. 读取客户端消息
            String clientMsg = in.readLine();
            if (clientMsg != null) { // 防止空消息
                System.out.println("收到客户端消息：" + clientMsg);
                // 响应客户端
                out.println("服务器响应：" + clientMsg);
            }

            // 3. 半关闭输出流
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 4. 手动关闭资源（Java 8 需在 finally 中释放，替代 Java 9+ 的 try (socket; ...)）
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * Java 8 兼容的 TCP 客户端
 */
class TCPClient {
    public static void main(String[] args) {
        // Java 8 风格：try-with-resources 内声明并初始化所有资源
        try (Socket socket = new Socket("localhost", 8888);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            socket.setSoTimeout(5000); // 超时设置

            // 发送消息
            out.println("Hello TCP Server!");

            // 接收响应
            String serverMsg = in.readLine();
            if (serverMsg != null) {
                System.out.println("收到服务器响应：" + serverMsg);
            }
        } catch (UnknownHostException e) {
            System.err.println("无法连接服务器：" + e.getMessage());
        } catch (SocketTimeoutException e) {
            System.err.println("连接超时！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}