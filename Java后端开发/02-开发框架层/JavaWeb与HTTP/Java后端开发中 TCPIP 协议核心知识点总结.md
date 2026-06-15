# Java 后端开发中 TCP/IP 协议核心知识点总结

> **文档定位**：Java 后端技术参考文档 | TCP/IP 协议核心知识  
> **核心说明**：TCP/IP 是分层协议簇，采用四层模型（与 OSI 七层对应），HTTP 协议基于 TCP，理解 TCP/IP 对排查网络问题至关重要  
> **Java 关联**：Socket 编程、Spring MVC 底层通信、网络问题排查

---

## 目录

- [一、TCP/IP 协议簇整体架构](#一tcpip-协议簇整体架构)
- [二、核心协议：IP 协议](#二核心协议ip-协议)
- [三、核心协议：TCP 协议](#三核心协议tcp-协议)
- [四、核心协议：UDP 协议](#四核心协议udp-协议)
- [五、TCP/IP 与 Java 后端开发的关联](#五tcpip-与-java-后端开发的关联)

---

## 一、TCP/IP 协议簇整体架构

TCP/IP 采用四层模型，数据在传输时自上而下逐层封装，接收时自下而上逐层解封装。

| 层级 | 作用 | 核心协议 | OSI 对应 |
|------|------|----------|----------|
| **应用层** | 为应用程序提供服务 | HTTP、FTP、SMTP、DNS | 应用层 + 表示层 + 会话层 |
| **传输层** | 端到端通信 | **TCP**、**UDP** | 传输层 |
| **网络层** | 跨网络数据包路由转发 | **IP**、ICMP、ARP | 网络层 |
| **网络接口层** | 物理网络数据帧传输 | 以太网、WiFi | 数据链路层 + 物理层 |

---

## 二、核心协议：IP 协议

网络层核心，为数据包分配 IP 地址，实现跨网络路由转发。

| 特性 | 说明 |
|------|------|
| **核心特点** | 无连接、不可靠（不保证顺序到达，不保证不丢失） |
| **IPv4** | 32 位，如 `192.168.1.1` |
| **IPv6** | 128 位，如 `2001:0db8:85a3:0000:0000:8a2e:0370:7334` |
| **TTL** | 每经过一个路由器减 1，为 0 则丢弃 |

### Java 相关 API

```java
import java.net.InetAddress;

public class IpDemo {
    public static void main(String[] args) throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        System.out.println("主机名：" + localHost.getHostName());
        System.out.println("IP 地址：" + localHost.getHostAddress());
    }
}
```

---

## 三、核心协议：TCP 协议

传输层**可靠、面向连接**的协议，是 HTTP 协议的底层支撑。

### 核心特点

| 特点 | 说明 |
|------|------|
| **面向连接** | 通信前三次握手建立连接，通信后四次挥手释放 |
| **可靠传输** | 序列号 + 确认应答（ACK）+ 重传机制 + 流量控制（滑动窗口）+ 拥塞控制 |
| **面向字节流** | 以字节流形式传输，无数据边界 |

### 三次握手（建立连接）

```
1. 客户端 → SYN → 服务器
2. 客户端 ← SYN+ACK ← 服务器
3. 客户端 → ACK → 服务器（连接建立）
```

### 四次挥手（释放连接）

```
1. 客户端 → FIN → 服务器
2. 客户端 ← ACK ← 服务器
3. 客户端 ← FIN ← 服务器
4. 客户端 → ACK → 服务器（连接释放）
```

### Java TCP 编程（Socket）

```java
// 服务端
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;

public class TcpServer {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器已启动，等待连接...");
        Socket socket = serverSocket.accept();  // 阻塞等待
        
        OutputStream out = socket.getOutputStream();
        out.write("Hello TCP Client!".getBytes());
        
        out.close();
        socket.close();
        serverSocket.close();
    }
}

// 客户端
import java.net.Socket;
import java.io.InputStream;

public class TcpClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("127.0.0.1", 8888);
        
        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        System.out.println("收到：" + new String(buffer, 0, len));
        
        in.close();
        socket.close();
    }
}
```

---

## 四、核心协议：UDP 协议

传输层**无连接、不可靠**的协议，适用于对实时性要求高的场景。

| 特性 | 说明 |
|------|------|
| **无连接** | 无需建立连接，直接发送数据包 |
| **不可靠** | 不保证到达顺序，不重传丢失的数据 |
| **面向报文** | 以数据包为传输单位，保留数据边界 |
| **适用场景** | 视频直播、语音通话、游戏数据传输 |

### Java UDP 编程

```java
// 发送端
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class UdpSender {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        byte[] data = "Hello UDP!".getBytes();
        DatagramPacket packet = new DatagramPacket(
            data, data.length, InetAddress.getByName("127.0.0.1"), 9999);
        socket.send(packet);
        socket.close();
    }
}

// 接收端
import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class UdpReceiver {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9999);
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);  // 阻塞接收
        String msg = new String(packet.getData(), 0, packet.getLength());
        System.out.println("收到：" + msg);
        socket.close();
    }
}
```

---

## 五、TCP/IP 与 Java 后端开发的关联

| 关联点 | 说明 |
|--------|------|
| **HTTP 基于 TCP** | Spring MVC 等框架底层通过 Servlet 封装了 TCP 通信细节 |
| **Socket 编程** | 即时通讯、文件传输等场景需直接使用 TCP/UDP 通信 |
| **网络排查** | 理解三次握手、四次挥手、IP 路由、TTL 等概念 |
| **连接问题** | 连接超时 → 可能是三次握手未完成；连接断开 → 四次挥手异常 |
