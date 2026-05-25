Java后端开发中 TCP/IP 协议核心知识点总结
一、TCP/IP 协议簇整体架构
TCP/IP 是一个分层的协议簇，通常采用 四层模型（与 OSI 七层模型对应），数据在传输时会自上而下逐层封装，接收时自下而上逐层解封装。
1. 应用层：直接为应用程序提供服务，对应 OSI 应用层、表示层、会话层，包含 HTTP、FTP、SMTP、DNS 等协议，Java 后端开发中接触的 HTTP 协议就处于这一层。
2. 传输层：负责端到端的通信，提供可靠或不可靠的数据传输服务，核心协议为 TCP 和 UDP。
3. 网络层：负责跨网络的数据包路由与转发，核心协议是 IP，还有 ICMP（网络控制报文协议，用于 ping 测试）、ARP（地址解析协议，将 IP 映射为 MAC 地址）。
4. 网络接口层：负责物理网络的数据帧传输，对应 OSI 数据链路层和物理层，包含以太网协议、WiFi 协议等。
    二、核心协议：IP 协议
    IP 协议是网络层的核心，作用是为数据包分配 IP 地址，实现跨网络的路由转发。
    1. 核心特点：无连接、不可靠，不保证数据包的顺序到达，也不保证数据包不丢失。
    2. IP 地址：用于标识网络中的主机，分为 IPv4（32 位，如  192.168.1.1 ）和 IPv6（128 位，如  2001:0db8:85a3:0000:0000:8a2e:0370:7334 ）。
    3. 数据包结构：包含版本号、首部长度、生存时间（TTL，每经过一个路由器减 1，为 0 则丢弃）、源 IP、目的 IP 等字段。
    4. Java 相关 API： java.net.InetAddress  类可获取主机的 IP 地址，示例：
    java  
    import java.net.InetAddress;
    import java.net.UnknownHostException;
    public class IpDemo {
    public static void main(String[] args) throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        System.out.println("主机名：" + localHost.getHostName());
        System.out.println("IP 地址：" + localHost.getHostAddress());
    }
    }
 
三、核心协议：TCP 协议
TCP 是传输层的可靠、面向连接的协议，是 HTTP 协议的底层支撑。
1. 核心特点
    - 面向连接：通信前必须通过 三次握手 建立连接，通信结束后通过 四次挥手 释放连接。
    - 可靠传输：通过序列号、确认应答（ACK）、重传机制、流量控制（滑动窗口）、拥塞控制保证数据有序、不丢失。
    - 面向字节流：以字节流的形式传输数据，无数据边界。
2. 三次握手（建立连接）
    1. 客户端发送 SYN 报文（同步序列号），请求建立连接。
    2. 服务器回复 SYN+ACK 报文，确认客户端请求，并同步自身序列号。
    3. 客户端发送 ACK 报文，确认服务器的回复，连接建立完成。
3. 四次挥手（释放连接）
    1. 客户端发送 FIN 报文，请求关闭连接。
    2. 服务器回复 ACK 报文，确认客户端的 FIN 请求。
    3. 服务器发送 FIN 报文，告知客户端数据已发送完毕，请求关闭连接。
    4. 客户端回复 ACK 报文，确认服务器的 FIN 请求，连接释放完成。
4. Java 中的 TCP 编程（Socket 编程）
    - 服务器端：使用  ServerSocket  监听端口， Socket  与客户端通信。
    java  
    import java.io.IOException;
    import java.io.OutputStream;
    import java.net.ServerSocket;
    import java.net.Socket;
    public class TcpServer {
    public static void main(String[] args) throws IOException {
        // 监听 8888 端口
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器已启动，等待客户端连接...");
        // 阻塞等待客户端连接
        Socket socket = serverSocket.accept();
        // 获取输出流，向客户端发送数据
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("Hello TCP Client!".getBytes());
        // 关闭资源
        outputStream.close();
        socket.close();
        serverSocket.close();
    }
    }
 
- 客户端：使用  Socket  连接服务器，读写数据。
    java  
    import java.io.IOException;
    import java.io.InputStream;
    import java.net.Socket;
    public class TcpClient {
    public static void main(String[] args) throws IOException {
        // 连接本地服务器的 8888 端口
        Socket socket = new Socket("127.0.0.1", 8888);
        // 获取输入流，读取服务器发送的数据
        InputStream inputStream = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int len = inputStream.read(buffer);
        System.out.println("收到服务器消息：" + new String(buffer, 0, len));
        // 关闭资源
        inputStream.close();
        socket.close();
    }
    }
 
四、核心协议：UDP 协议
UDP 是传输层的无连接、不可靠的协议，适用于对实时性要求高的场景。
1. 核心特点
    - 无连接：通信前无需建立连接，直接发送数据包。
    - 不可靠：不保证数据包的到达顺序，也不重传丢失的数据包。
    - 面向报文：以数据包为传输单位，保留数据边界，开销小、传输速度快。
2. 适用场景：视频直播、语音通话、游戏数据传输等。
3. Java 中的 UDP 编程（DatagramSocket 编程）
    - 发送端：使用  DatagramSocket  发送  DatagramPacket  数据包。
    java  
    import java.io.IOException;
    import java.net.DatagramPacket;
    import java.net.DatagramSocket;
    import java.net.InetAddress;
    public class UdpSender {
    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        String msg = "Hello UDP Receiver!";
        byte[] data = msg.getBytes();
        // 封装数据包：数据、长度、目标 IP、目标端口
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), 9999);
        socket.send(packet);
        socket.close();
    }
    }
 
- 接收端：使用  DatagramSocket  接收数据包。
    java  
    import java.io.IOException;
    import java.net.DatagramPacket;
    import java.net.DatagramSocket;
    public class UdpReceiver {
    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(9999);
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        // 阻塞接收数据包
        socket.receive(packet);
        String msg = new String(packet.getData(), 0, packet.getLength());
        System.out.println("收到消息：" + msg);
        socket.close();
    }
    }
 
五、TCP/IP 与 Java 后端开发的关联
1. HTTP 协议基于 TCP 协议，Java 后端的 Spring MVC 等框架底层通过 Servlet 技术封装了 TCP 通信细节。
2. 开发网络通信功能（如即时通讯、文件传输）时，需直接使用 Socket 编程实现 TCP/UDP 通信。
3. 排查网络问题时，需理解 TCP 三次握手、四次挥手机制，以及 IP 路由、TTL 等概念。
