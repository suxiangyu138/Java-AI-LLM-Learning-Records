import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP 多线程服务端（修复 Charset 类型错误，支持多客户端同时连接）
 * 适配 JDK 1.7+，无语法错误，可直接运行
 */
public class TcpMultiThreadServer {
    // 服务端监听端口
    private static final int PORT = 8888;
    // 线程池（核心线程数5，最大线程数10，控制并发数）
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        // 注册JVM关闭钩子，优雅关闭线程池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 服务端正在优雅关闭...");
            THREAD_POOL.shutdown();
            System.out.println("✅ 线程池已关闭，服务端退出完成");
        }));

        // 1. 创建 ServerSocket 监听指定端口
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🚀 TCP 多线程服务端已启动，监听端口：" + PORT);
            System.out.println("🔍 等待客户端连接...\n");

            // 2. 主线程循环接收客户端连接（永不退出，除非手动停止）
            while (true) {
                // 阻塞等待客户端连接
                Socket clientSocket = serverSocket.accept();
                // 获取客户端唯一标识（IP+端口）
                String clientInfo = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
                System.out.println("✅ 新客户端连接：" + clientInfo);

                // 3. 将客户端连接提交到线程池处理，主线程继续监听
                THREAD_POOL.submit(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("❌ 服务端启动失败：" + e.getMessage());
            System.err.println("🔍 排查方向：1. 端口 " + PORT + " 是否被占用  2. 权限是否足够");
            // 启动失败时关闭线程池
            THREAD_POOL.shutdown();
        }
    }

    /**
     * 客户端处理线程类（每个客户端对应一个实例，线程私有资源，无并发问题）
     */
    static class ClientHandler implements Runnable {
        // 客户端Socket连接
        private final Socket clientSocket;
        // 客户端标识（IP:Port）
        private String clientInfo;

        // 构造方法：初始化客户端连接
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientInfo = socket.getInetAddress() + ":" + socket.getPort();
        }

        @Override
        public void run() {
            // 每个线程独立的输入输出流，使用try-with-resources自动关闭
            try (
                    // 修复：使用 StandardCharsets.UTF_8（Charset类型）替代字符串，解决类型错误
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)
                    );
                    PrintWriter out = new PrintWriter(
                            clientSocket.getOutputStream(),
                            true,  // 自动刷新缓冲区，无需手动flush
                            StandardCharsets.UTF_8
                    )
            ) {
                String clientMsg;
                // 循环读取客户端发送的消息，直到客户端断开/发送exit
                while ((clientMsg = in.readLine()) != null) {
                    // 打印客户端消息到服务端控制台
                    System.out.println("📩 来自 " + clientInfo + " 的消息：" + clientMsg);

                    // 客户端发送exit，主动关闭该连接
                    if ("exit".equalsIgnoreCase(clientMsg)) {
                        out.println("👋 服务端：已收到退出指令，连接即将关闭");
                        break;
                    }

                    // 模拟业务处理：返回带线程ID的响应，证明多线程隔离
                    String response = String.format(
                            "✅ 服务端（线程ID：%d）已接收消息：%s",
                            Thread.currentThread().getId(), clientMsg
                    );
                    // 发送响应给客户端
                    out.println(response);
                }

            } catch (IOException e) {
                System.err.println("❌ 客户端 " + clientInfo + " 通信异常：" + e.getMessage());
                System.err.println("🔍 可能原因：客户端异常断开、网络波动");
            } finally {
                // 最终确保关闭客户端Socket，释放端口资源
                try {
                    if (!clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                    System.out.println("🔌 客户端 " + clientInfo + " 连接已关闭\n");
                } catch (IOException e) {
                    System.err.println("❌ 关闭客户端连接失败：" + e.getMessage());
                }
            }
        }
    }
}