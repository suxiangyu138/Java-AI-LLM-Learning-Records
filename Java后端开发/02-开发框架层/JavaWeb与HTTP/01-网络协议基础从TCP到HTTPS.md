# 网络协议基础：从 TCP 到 HTTPS

## 前言

网络协议是互联网通信的基石。作为一名后端开发工程师，深入理解从 TCP 到 HTTPS 的协议栈，不仅有助于编写高性能的网络应用，更是排查网络问题、优化系统性能的必备技能。本文将从 OSI 七层模型讲起，逐层深入到 TCP、UDP、HTTP 各版本以及 HTTPS 加密流程，结合 Java 代码示例，力求让读者建立起完整的网络协议知识体系。

---

## 一、OSI 七层模型与 TCP/IP 五层模型

### 1.1 模型概览

| OSI 七层模型 | TCP/IP 五层模型 | 核心协议举例 | 传输单位 |
|---|---|---|---|
| 应用层 (Application) | 应用层 | HTTP, DNS, FTP, SMTP, SSH | 报文 (Message) |
| 表示层 (Presentation) | 应用层 | SSL/TLS (实际工作在此附近) | 报文 |
| 会话层 (Session) | 应用层 | RPC, NetBIOS | 报文 |
| 传输层 (Transport) | 传输层 | TCP, UDP | 段 (Segment) / 数据报 (Datagram) |
| 网络层 (Network) | 网络层 | IP, ICMP, ARP, OSPF | 包 (Packet) |
| 数据链路层 (Data Link) | 数据链路层 | MAC, Ethernet, PPP | 帧 (Frame) |
| 物理层 (Physical) | 物理层 | 网线, 光纤, 无线电波 | 比特 (Bit) |

OSI 七层模型是理论上的标准，而 TCP/IP 五层模型是实际互联网所采用的模型。OSI 的表示层和会话层功能在 TCP/IP 模型中被合并到了应用层中。

### 1.2 各层职责

**物理层**：负责透明地传输原始比特流，定义物理接口的机械、电气、功能和过程特性。例如 RJ45 网线接口、光纤中的光信号、Wi-Fi 的无线电波频段等。

**数据链路层**：将比特流封装成帧，提供节点到节点的可靠传输。通过 MAC 地址识别设备，通过 CRC 校验检测错误。核心协议包括 Ethernet（以太网）和 ARP（地址解析协议，将 IP 地址映射为 MAC 地址）。

**网络层**：负责数据包的路由和转发，选择最佳路径将数据从源端送达目的端。核心协议是 IP（网际协议），辅以 ICMP（互联网控制报文协议，ping 命令的基础）和路由协议。

**传输层**：提供端到端的通信服务，这是网络协议中最关键的一层。TCP 提供可靠、面向连接的传输；UDP 提供不可靠、无连接的传输。传输层引入端口号概念，使多个应用可以共享同一网络连接。

**应用层**：为应用程序提供网络服务接口。HTTP（网页浏览）、DNS（域名解析）、FTP（文件传输）、SMTP（电子邮件）等协议均位于此层。

### 1.3 封装与解封装过程

当数据从应用层向下传输时，每一层都会在数据前面加上自己的头部信息（有些层还会加尾部，如数据链路层的 FCS 校验尾）：

```
[应用层]    HTTP 报文 (GET /index.html HTTP/1.1...)
[传输层]    TCP 头部 + HTTP 报文 = TCP 段
[网络层]    IP 头部 + TCP 段 = IP 包
[数据链路层] MAC 头部 + IP 包 + FCS 尾部 = 以太网帧
[物理层]    比特流
```

接收端逆向操作，逐层解封，每层去掉对应的头部，最终将原始 HTTP 报文交给应用层处理。

### 1.4 为什么要分层

分层的设计哲学是**关注点分离**和**解耦**：

- **独立演进**：每一层可以独立升级优化，不影响其他层。例如从 HTTP/1.1 升级到 HTTP/2，底层 TCP 无需任何改动；从 IPv4 过渡到 IPv6，上层应用基本不受影响。
- **模块化开发**：开发人员只需关注自己所在层次的协议实现，降低了复杂度。
- **标准化接口**：层与层之间通过清晰的接口定义交互，不同厂商的设备可以互联互通。

---

## 二、TCP 协议（核心重点）

TCP（Transmission Control Protocol，传输控制协议）是互联网最核心的传输层协议，提供了面向连接的、可靠的、基于字节流的通信服务。

### 2.1 TCP 核心特点

| 特性 | 说明 | 实现机制 |
|---|---|---|
| 面向连接 | 通信前必须先建立连接，通信结束后释放连接 | 三次握手 / 四次挥手 |
| 可靠传输 | 数据无丢失、无重复、无差错地到达 | 确认应答 (ACK) + 超时重传 |
| 有序传输 | 数据按发送顺序到达接收端 | 序列号 (Sequence Number) |
| 流量控制 | 防止发送方过快，压垮接收方 | 滑动窗口 (rwnd) |
| 拥塞控制 | 防止发送方过快，压垮网络 | 慢启动 / 拥塞避免 / 快重传 / 快恢复 |

### 2.2 TCP 报文段头部结构

TCP 头部固定部分为 20 字节，结构如下：

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|         源端口 (16)           |        目的端口 (16)           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       序列号 (32)                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       确认号 (32)                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| 数据偏移 (4) | 保留 (6) | 标志位 (6) |        窗口大小 (16)    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|        校验和 (16)             |       紧急指针 (16)           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         选项 (可变)                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                             数据                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

关键标志位：
- **SYN** (Synchronize)：建立连接时使用
- **ACK** (Acknowledgment)：确认字段有效
- **FIN** (Finish)：释放连接
- **RST** (Reset)：重置连接
- **PSH** (Push)：接收方应尽快将数据交给应用层
- **URG** (Urgent)：紧急指针字段有效

### 2.3 三次握手（建立连接）

三次握手是 TCP 建立连接的经典过程，目的是在通信双方之间同步序列号并协商参数。

```
客户端                             服务端
  |                                   |
  |---- SYN (seq=x) ---------------->|  第一次握手 (SYN)
  |                                   |
  |<--- SYN+ACK (seq=y, ack=x+1) ---|  第二次握手 (SYN+ACK)
  |                                   |
  |---- ACK (seq=x+1, ack=y+1) ---->|  第三次握手 (ACK)
  |                                   |
  |<======== 连接建立完成 ===========>|
```

**详细过程**：

1. **第一次握手**：客户端发送 SYN 报文，设定客户端初始序列号 `client_isn = x`。客户端进入 `SYN_SENT` 状态。
2. **第二次握手**：服务端收到 SYN，回复 SYN+ACK 报文。设定服务端初始序列号 `server_isn = y`，确认号 `ack = x + 1`。服务端进入 `SYN_RCVD` 状态。
3. **第三次握手**：客户端收到 SYN+ACK，回复 ACK 报文，确认号 `ack = y + 1`。客户端进入 `ESTABLISHED` 状态。服务端收到 ACK 后也进入 `ESTABLISHED` 状态。

**为什么是三次，而不是两次或四次？**

核心原因：**防止已过期的连接请求到达服务端而建立错误连接**。

网络中存在延迟和重发。假设客户端发送的第一个 SYN 在网络中延迟了，客户端超时后重发了一个新的 SYN 并成功建立了连接，传输数据后释放连接。此时，第一个延迟的 SYN 才到达服务端。如果只有两次握手，服务端收到这个过期的 SYN 后会误以为客户端想建立新连接，于是分配资源并回复 SYN+ACK，从而建立一条无效连接，造成资源浪费。三次握手要求客户端在收到 SYN+ACK 后再次确认，由于客户端知道自己并没有发起新连接，会忽略（或发送 RST）这个过期的 SYN+ACK，避免了这一问题的发生。

**四次握手**理论上也可以，但三次已经足够，多一次只是增加延迟而无增益。

### 2.4 四次挥手（释放连接）

TCP 连接是全双工的，因此释放连接需要双方分别关闭各自的数据传输方向。

```
客户端                             服务端
  |                                   |
  |---- FIN (seq=u) --------------->|  第一次挥手 (FIN)
  |                                   |
  |<--- ACK (ack=u+1) -------------|  第二次挥手 (ACK)
  |                                   |
  |<--- FIN (seq=v, ack=u+1) ------|  第三次挥手 (FIN)
  |                                   |
  |---- ACK (ack=v+1) ------------->|  第四次挥手 (ACK)
  |                                   |
  |  TIME_WAIT (2MSL)                |
  |                                   |
```

**详细过程**：

1. **第一次挥手**：客户端发送 FIN 报文，表示客户端不再发送数据，进入 `FIN_WAIT_1` 状态。
2. **第二次挥手**：服务端回复 ACK，进入 `CLOSE_WAIT` 状态。客户端收到 ACK 后进入 `FIN_WAIT_2` 状态。
3. **第三次挥手**：服务端处理完剩余数据后，发送 FIN 报文，进入 `LAST_ACK` 状态。
4. **第四次挥手**：客户端回复 ACK，进入 `TIME_WAIT` 状态，等待 2MSL 后自动关闭。服务端收到 ACK 后立即进入 `CLOSED` 状态。

**TIME_WAIT 为什么需要 2MSL？**

MSL（Maximum Segment Lifetime）是报文段在网络中的最大生存时间，通常为 30 秒到 2 分钟。2MSL 的原因有二：

1. **确保最后一次 ACK 能到达服务端**：如果服务端没有收到第四次挥手的 ACK，会超时重发 FIN。客户端在 TIME_WAIT 期间可以重新发送 ACK。如果客户端直接关闭，服务端重发的 FIN 将得不到响应，导致服务端无法正常关闭。
2. **让旧连接的所有数据包在网络中消失**：等待 2MSL 可以确保本次连接的所有数据包都从网络中消失，避免它们干扰后续使用相同端口的新连接。

**CLOSE_WAIT 过多是什么原因？**

CLOSE_WAIT 表示服务端收到了客户端的 FIN 并回复了 ACK，但服务端自身的 close() 一直没有被调用。**根本原因是应用层代码没有正确关闭 socket**——通常是没有在 finally 块中执行 close，或者服务器处理请求的逻辑异常退出导致 close 被跳过。大量 CLOSE_WAIT 会耗尽文件描述符，导致服务器无法接受新连接。

### 2.5 滑动窗口与流量控制

TCP 使用**滑动窗口**机制实现流量控制，防止发送方发送数据过快而接收方来不及处理。

**核心概念**：

- **接收窗口 (rwnd, receive window)**：接收方在 TCP 头部窗口字段中通告的剩余缓冲区大小，表示接收方还能接收多少字节数据。
- **发送窗口 (swnd, send window)**：发送方可以发送但尚未收到确认的最大字节数。`swnd = min(cwnd, rwnd)`，其中 cwnd 是拥塞窗口。
- **窗口滑动**：每收到一个 ACK，发送窗口向右滑动，释放已确认的字节，允许发送新数据。

**工作流程示意**：

```
发送方缓冲区：
| 已发送并确认 | 已发送待确认 | 可发送 | 不可发送 |
|--------------|--------------|--------|----------|
               ^              ^        ^
              SND.UNA        SND.NXT  SND.WND
```

- `SND.UNA`：已发送但未收到 ACK 的最小序号
- `SND.NXT`：下一个要发送的字节序号
- `SND.WND`：发送窗口大小

接收方通过 ACK 中的窗口字段告知发送方自己的可用缓冲区大小。如果接收方处理缓慢，窗口逐渐缩小，最终可能变为 0。

**零窗口探测 (Zero Window Probe)**：当接收方窗口为 0 时，发送方停止发送数据，但会定期发送一个 1 字节的探测报文，询问接收方窗口是否已恢复。这防止了死锁——即接收方发送了窗口更新报文但丢失了，双方相互等待。

### 2.6 拥塞控制

流量控制解决的是发送方和接收方之间的速度匹配问题，而**拥塞控制**解决的是发送方和整个网络之间的容量匹配问题。

TCP 拥塞控制包含四个核心算法：

#### 慢启动 (Slow Start)

连接建立后，拥塞窗口 `cwnd` 初始值通常为 10 个 MSS（最大段大小，约 1460 字节）。每收到一个 ACK，`cwnd` 增加 1 个 MSS，实际上是**指数级增长**：

- 初始：cwnd = 10 MSS
- 第 1 轮：发送 10 个段，收到 10 个 ACK，cwnd = 20
- 第 2 轮：发送 20 个段，收到 20 个 ACK，cwnd = 40
- 第 3 轮：发送 40 个段，收到 40 个 ACK，cwnd = 80
- ...

当 `cwnd` 达到慢启动阈值 `ssthresh`（初始值通常很大，如 65535 字节）时，进入拥塞避免阶段。

#### 拥塞避免 (Congestion Avoidance)

进入拥塞避免后，`cwnd` 从指数增长变为**线性增长**：每个 RTT（往返时间）增加 1 个 MSS（实际实现中通常是每收到一个 ACK，cwnd 增加 `MSS * (MSS / cwnd)`）。

```
cwnd 增长趋势：
慢启动：  1 -> 2 -> 4 -> 8 -> 16 -> 32 (指数)
拥塞避免：32 -> 33 -> 34 -> 35 -> ... (线性)
```

#### 快重传 (Fast Retransmit)

当发送方收到 **3 个重复的 ACK**（即 4 次相同序号的 ACK），即使没有超时，也判定该报文段丢失，立即重传。不必等待超时计时器，从而节省时间。

#### 快恢复 (Fast Recovery)

快重传后，发送方执行快恢复：
- `ssthresh = cwnd / 2`
- `cwnd = ssthresh + 3`（+3 是因为已收到 3 个重复 ACK，认为有 3 个数据段已离开网络）
- 然后进入拥塞避免阶段（线性增长）

**完整拥塞控制状态机**：

```
                    +-----------+
                    |  慢启动    |
                    +-----+-----+
                          |
                   cwnd >= ssthresh
                          |
                          v
                    +-----------+
                    | 拥塞避免   |
                    +-----+-----+
                          |
              +-----------+-----------+
              |                       |
          超时(Tahoe/Reno)         3个重复ACK
              |                       |
              v                       v
        ssthresh = cwnd/2      ssthresh = cwnd/2
        cwnd = 1 (Tahoe)      cwnd = ssthresh+3
        cwnd = cwnd/2 (NewReno)  => 快恢复
              |
              v
         回到慢启动
```

### 2.7 Nagle 算法与 TCP_NODELAY

**Nagle 算法**：用于减少网络中小报文的数量。规则如下：

- 如果发送方有数据要发送，但已发送的数据尚未得到确认，则**合并小数据**到一个 TCP 段中一起发送。
- 当已发送数据得到 ACK 后，再将缓冲区中累积的数据一次性发送。

Nagle 算法显著减少了网络负载，但在某些低延迟场景（如实时游戏、远程鼠标移动、SSH 按键回显）中会引入不必要的延迟，因为算法会等待 ACK 到来才发送下一个数据包。

**TCP_NODELAY**：通过设置 socket 选项 `TCP_NODELAY` 可以禁用 Nagle 算法，让每个写入的数据都立即发送，实现低延迟。在 Java 中通过 `socket.setTcpNoDelay(true)` 设置。

选择建议：
- 批量数据传输、大文件上传：保持 Nagle 算法启用，减少网络包数量
- 交互式应用、实时通信：启用 TCP_NODELAY，降低延迟

### 2.8 TCP Keep-Alive

TCP Keep-Alive 是一种**探测机制**，用于检测空闲连接的另一端是否仍然存活。

- 默认情况下，TCP 连接在**2 小时**内没有数据交互后，才会开始发送 Keep-Alive 探测报文。
- 探测间隔通常为 75 秒，共发送 9 次探测。
- 如果探测均无响应，连接被关闭。

Keep-Alive 的目的是清理死连接，释放系统资源。但在应用层设计心跳机制（如 WebSocket Ping/Pong）通常比依赖 TCP Keep-Alive 更灵活可控。

### 2.9 Java Socket 示例

```java
import java.io.*;
import java.net.*;

public class TcpEchoServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("TCP Server listening on port 8080...");

        while (true) {
            // 阻塞等待客户端连接（三次握手发生在此处背后）
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(
                         clientSocket.getOutputStream(), true)) {

                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // 启用 TCP_NODELAY 关闭 Nagle 算法
                clientSocket.setTcpNoDelay(true);

                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("Received: " + line);
                    out.println("Echo: " + line);
                    if ("bye".equalsIgnoreCase(line)) {
                        break;
                    }
                }
                System.out.println("Client disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

```java
import java.io.*;
import java.net.*;

public class TcpEchoClient {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 8080);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(
                     new InputStreamReader(System.in))) {

            socket.setTcpNoDelay(true);

            String userInput;
            while ((userInput = console.readLine()) != null) {
                out.println(userInput);
                System.out.println("Server response: " + in.readLine());
                if ("bye".equalsIgnoreCase(userInput)) {
                    break;
                }
            }
        }
    }
}
```

---

## 三、UDP 协议

### 3.1 UDP 特点

UDP（User Datagram Protocol，用户数据报协议）与 TCP 同属传输层，但设计哲学截然相反：

| 特性 | UDP | TCP |
|---|---|---|
| 连接状态 | 无连接，无需建立连接 | 面向连接，三次握手 |
| 可靠性 | 不可靠，无确认和重传 | 可靠，确认重传 |
| 有序性 | 不保证有序 | 序列号保证有序 |
| 流量控制 | 无 | 滑动窗口 |
| 拥塞控制 | 无 | 慢启动/拥塞避免等 |
| 首部大小 | 8 字节固定首部 | 20 字节固定首部 + 选项 |
| 传输模式 | 基于报文，不合并 | 基于字节流 |
| 速度 | 快，无握手/确认延迟 | 相对较慢 |
| 适用场景 | 实时应用 | 可靠传输场景 |

### 3.2 UDP 首部结构

UDP 首部极其简洁，只有 8 个字节：

```
 0               15 16              31
+------------------+------------------+
|    源端口 (16)    |   目的端口 (16)   |
+------------------+------------------+
|    长度 (16)      |   校验和 (16)     |
+------------------+------------------+
|              数据 (可变)             |
+-------------------------------------+
```

### 3.3 UDP 适用场景

UDP 虽然不可靠，但在以下场景中比 TCP 更合适：

1. **音视频实时通信**（VoIP、视频会议、直播）：允许少量丢包，但不能接受重传带来的延迟抖动。
2. **DNS 查询**：查询请求极小，一次往返即可完成，用 TCP 的开销过大。
3. **广播与组播**：UDP 天然支持一对多的通信模式，TCP 无法实现广播。
4. **物联网传感器数据上报**：大量小报文上报，可以容忍部分丢失。
5. **QUIC（HTTP/3 的底层）**：在 UDP 之上实现可靠传输，兼具 TCP 的可靠性和 UDP 的低延迟。

### 3.4 TCP vs UDP 选择决策表

| 场景 | 推荐协议 | 原因 |
|---|---|---|
| 网页浏览 | TCP | 必须完整、准确地接收页面内容 |
| 文件下载/上传 | TCP | 数据完整性优先 |
| 电子邮件 (SMTP/IMAP) | TCP | 邮件内容不能丢失 |
| 在线视频直播 | UDP (RTMP/SRT) | 延迟敏感，可容忍少量丢帧 |
| 视频会议 (WebRTC) | UDP | 实时性要求极高 |
| 域名解析 (DNS) | UDP（默认） | 请求短小，TCP 连接开销大 |
| 网络游戏 | UDP (部分可靠) | 状态同步需要低延迟 |
| IoT 传感器上报 | UDP | 高频小报文，可容忍丢失 |

### 3.5 Java UDP 示例

```java
import java.net.*;

public class UdpEchoServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9090);
        byte[] buffer = new byte[1024];
        System.out.println("UDP Server listening on port 9090...");

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet); // 阻塞等待

            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received from " + packet.getAddress() + ": " + received);

            String response = "Echo: " + received;
            byte[] respData = response.getBytes();
            DatagramPacket respPacket = new DatagramPacket(
                    respData, respData.length, packet.getAddress(), packet.getPort());
            socket.send(respPacket);
        }
    }
}
```

---

## 四、HTTP 协议

HTTP（HyperText Transfer Protocol，超文本传输协议）是 Web 的基石。它经历了 HTTP/1.0、HTTP/1.1、HTTP/2 到 HTTP/3 的演进，每次升级都带来了性能上的飞跃。

### 4.1 HTTP/1.0（短连接时代）

HTTP/1.0 的主要特点：

- **短连接**：每次 HTTP 请求都会创建一个新的 TCP 连接，请求完成后立即关闭。一个包含 CSS、JS、多张图片的页面需要建立数十次 TCP 连接。
- **无 Host 头**：无法在同一个 IP 上托管多个域名（虚拟主机）。
- **有限的缓存控制**：仅通过 `Expires` 头控制缓存。

性能瓶颈：频繁的 TCP 三次握手和四次挥手导致大量延迟开销。每个资源都需要独立的连接，无法复用。

### 4.2 HTTP/1.1（持久连接时代）

HTTP/1.1 是目前仍然广泛使用的版本，引入了一系列重要改进：

#### 持久连接 (Persistent Connection)

默认启用 `Connection: keep-alive`，允许多个请求复用同一个 TCP 连接，大幅减少了握手开销。

```
HTTP/1.0:  [TCP握手] [请求] [响应] [TCP挥手] [TCP握手] [请求] [响应] [TCP挥手] ...
HTTP/1.1:  [TCP握手] [请求] [响应] [请求] [响应] [请求] [响应] ... [TCP挥手]
```

#### 管道化 (Pipelining)

客户端可以在收到上一个请求的响应之前，连续发送多个请求：

```
客户端:  [请求1] [请求2] [请求3] ------------------------------>
服务端:  ------> [响应1] ---------> [响应2] ---------> [响应3]
```

**问题**：虽然请求可以并行发送，但**响应必须按顺序返回**。如果响应1的处理时间较长，会阻塞后面所有响应——这就是**队头阻塞（Head-of-Line Blocking）**。这也解释了为什么管道化在实际中很少被有效使用，许多浏览器甚至默认禁用了管道化。

#### Host 头

请求头中必须包含 `Host: example.com`，使得一台服务器可以在同一 IP 地址上托管多个不同域名的网站（虚拟主机），这是 HTTP/1.1 唯一强制要求的头部字段。

#### 缓存控制增强

引入 `Cache-Control` 头，提供更精细的缓存策略：

```
Cache-Control: max-age=3600           # 缓存1小时
Cache-Control: no-cache                # 每次使用前验证
Cache-Control: no-store                # 完全不缓存
Cache-Control: private/public          # 能否被中间代理缓存
```

`ETag`（实体标签）实现了**条件请求**：客户端在 `If-None-Match` 头中携带之前的 ETag 值，如果资源未变化，服务端返回 `304 Not Modified`，不返回响应体。

#### 分块传输 (Chunked Transfer)

`Transfer-Encoding: chunked` 允许服务端在不知道完整内容长度的情况下开始发送响应。适用于动态生成的内容（如实时日志流）。每个数据块包含长度标记，最后以长度为 0 的块结束。

#### 范围请求 (Range Request)

通过 `Range` 头实现断点续传和多线程下载：

```
请求:  GET /bigfile.zip HTTP/1.1
       Range: bytes=0-1023

响应:  HTTP/1.1 206 Partial Content
       Content-Range: bytes 0-1023/1048576
```

### 4.3 HTTP/2（二进制分帧时代）

HTTP/2 基于 Google 的 SPDY 协议，是对 HTTP 性能的一次革命性升级。

#### 二进制分帧层

HTTP/1.x 是文本协议，HTTP/2 改为**二进制协议**，将 HTTP 报文拆分为帧（Frame）和流（Stream）：

```
HTTP/2 帧格式：
+-----------------------------------------------+
| Length (24) | Type (8) | Flags (8) | R (1)     |
+---------------+---------------+---------------+
| Stream Identifier (31)                        |
+-----------------------------------------------+
| Frame Payload (可变长度)                       |
+-----------------------------------------------+
```

- **帧 (Frame)**：HTTP/2 的最小通信单位，包含帧类型（DATA、HEADERS、SETTINGS 等）、流标识符和负载。
- **流 (Stream)**：一个虚拟的、双向的、位于一个 TCP 连接内的独立信道。每个 HTTP 请求-响应对应一个流。

#### 多路复用 (Multiplexing)

HTTP/2 最核心的改进：**在同一个 TCP 连接上可以同时运行多个流**，各个流的帧可以交错发送和接收。

```
HTTP/1.1（串行，有队头阻塞）:
| 请求1开始 | ...等待... | 响应1完成 | 请求2开始 | ...等待... | 响应2完成 |

HTTP/2（多路复用）:
| 请求1帧1 | 请求2帧1 | 请求3帧1 | 请求1帧2 | 响应1帧1 | 响应2帧1 | ...
```

多路复用解决了 HTTP/1.1 的队头阻塞问题——一个请求的处理慢不会阻塞其他请求的响应传输。

**但 TCP 层仍有队头阻塞**：虽然 HTTP/2 解决了 HTTP 层的队头阻塞，但如果 TCP 层发生丢包，TCP 必须等待丢包重传完成才能将数据递交给上层。这意味着一个 TCP 连接上的所有 HTTP/2 流都会被阻塞。这个问题的最终解决方案是 HTTP/3。

#### 头部压缩 (HPACK)

HTTP 请求和响应中往往包含大量重复的头部字段（如 Cookie、User-Agent、Accept 等）。HTTP/2 使用 HPACK 算法压缩头部：

- **静态表**：预定义了 61 个常用头部字段（如 `:method: GET`、`:status: 200`），可以直接用索引号表示。
- **动态表**：通信过程中动态维护的字典，双方同步更新，用于压缩自定义或非常用的头部。
- **Huffman 编码**：对字符串进行 Huffman 压缩。

HPACK 通常能将头部大小减少 80%-90%，这是 Web 性能的显著提升。

#### 服务器推送 (Server Push)

服务端可以在客户端请求之前，主动推送客户端可能需要的资源。例如，客户端请求 `index.html`，服务端知道该页面需要 `style.css` 和 `app.js`，可以在返回 HTML 的同时主动推送这两个文件，无需等待客户端解析 HTML 后再次发起请求。

**注意**：浏览器厂商对 Server Push 的支持不统一，且可能导致推送了客户端已缓存的资源。HTTP/3 的推进者建议使用 `103 Early Hints` 替代 Server Push。

#### 流优先级

HTTP/2 允许客户端为每个流设置优先级，让服务端优先处理重要资源的请求（如先发送 CSS 和 JS，再发送图片）。

#### HTTP/2 必须使用 TLS

虽然 HTTP/2 规范不强制要求 TLS 加密，但所有主流浏览器（Chrome、Firefox、Safari）只支持基于 TLS 的 HTTP/2。实际应用中，HTTP/2 == HTTPS。

### 4.4 HTTP/3（QUIC 时代）

HTTP/3 将传输层从 TCP 换成了基于 UDP 的 **QUIC**（Quick UDP Internet Connections）协议。

#### QUIC 核心特性

1. **无队头阻塞 (No HOL Blocking)**：QUIC 在 UDP 之上实现了独立的流，一个流的丢包完全不影响其他流。这是真正的无队头阻塞。
2. **0-RTT 连接建立**：对于之前连接过的服务端，QUIC 可以**零往返时间**建立连接并发送数据。而 TCP + TLS 1.3 至少需要 1-RTT。
3. **连接迁移 (Connection Migration)**：QUIC 使用连接 ID 而非 IP 地址标识连接。当客户端从 Wi-Fi 切换到移动网络时，IP 改变但 QUIC 连接仍然有效，连接不中断。
4. **前向纠错 (FEC)**：发送方在数据包中加入冗余校验信息，接收方即使丢失部分数据包也可以直接恢复，无需重传（未来版本中 FEC 使用趋于保守）。
5. **用户态实现**：QUIC 在用户空间实现，不再依赖操作系统内核的 TCP 栈，协议迭代速度大大加快。

#### HTTP 版本对比

| 特性 | HTTP/1.0 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|---|---|---|---|---|
| 连接复用 | 否 (短连接) | 是 (keep-alive) | 是 (多路复用) | 是 (QUIC 多路复用) |
| 队头阻塞 | 有 | 有 (HOL at HTTP) | 有 (HOL at TCP) | 无 |
| 头部压缩 | 无 | 无 | HPACK | QPACK |
| 服务器推送 | 无 | 无 | 有 | 有 |
| 传输层 | TCP | TCP | TCP (TLS) | QUIC (UDP) |
| 连接建立 | 2-RTT | 2-RTT | 2-RTT (TCP+TLS) | 0-1 RTT |
| 二进制协议 | 否 | 否 | 是 | 是 |

### 4.5 常见 HTTP 状态码

#### 2xx 成功

| 状态码 | 含义 | 说明 |
|---|---|---|
| 200 OK | 请求成功 | 最常用的成功状态码 |
| 201 Created | 资源已创建 | POST/PUT 创建资源后返回，通常带 Location 头 |
| 204 No Content | 无内容 | DELETE 成功、或不需要返回响应体的操作 |
| 206 Partial Content | 部分内容 | Range 请求成功，用于断点续传 |

#### 3xx 重定向

| 状态码 | 含义 | 说明 |
|---|---|---|
| 301 Moved Permanently | 永久重定向 | 搜索引擎会更新 URL，下次直接访问新地址 |
| 302 Found | 临时重定向 | 搜索引擎继续使用原 URL，常用于登录跳转 |
| 304 Not Modified | 缓存未修改 | 条件请求（ETag/If-Modified-Since）命中，不返回响应体 |
| 307 Temporary Redirect | 临时重定向 (保持 Method) | 与 302 类似，但禁止改变请求方法（如 POST 不能变为 GET） |
| 308 Permanent Redirect | 永久重定向 (保持 Method) | 与 301 类似，但禁止改变请求方法 |

#### 4xx 客户端错误

| 状态码 | 含义 | 说明 |
|---|---|---|
| 400 Bad Request | 请求语法错误 | 最常见的参数校验失败响应 |
| 401 Unauthorized | 未认证 | 需要登录或提供有效 Token |
| 403 Forbidden | 无权限 | 已认证但无访问权限 |
| 404 Not Found | 资源不存在 | URL 找不到对应资源 |
| 405 Method Not Allowed | 方法不允许 | 如 GET 接口被 POST 请求 |
| 429 Too Many Requests | 请求过多 | 触发限流（Rate Limiting） |

#### 5xx 服务端错误

| 状态码 | 含义 | 说明 |
|---|---|---|
| 500 Internal Server Error | 服务器内部错误 | 通用服务器异常 |
| 502 Bad Gateway | 网关错误 | 反向代理/网关的上游服务无响应或返回无效响应 |
| 503 Service Unavailable | 服务不可用 | 服务器过载或维护中，通常带 Retry-After 头 |
| 504 Gateway Timeout | 网关超时 | 上游服务在超时时间内未响应 |

### 4.6 HTTP 请求方法

| 方法 | 幂等 | 安全 | 请求体 | 语义 |
|---|---|---|---|---|
| GET | 是 | 是 | 无（通常） | 获取资源 |
| HEAD | 是 | 是 | 无 | 获取响应头（无响应体），用于探测资源信息 |
| POST | 否 | 否 | 有 | 创建资源或提交数据 |
| PUT | 是 | 否 | 有 | 全量更新资源（不存在则创建） |
| PATCH | 否 | 否 | 有 | 部分更新资源 |
| DELETE | 是 | 否 | 无（通常） | 删除资源 |
| OPTIONS | 是 | 是 | 无 | 查询服务器支持的请求方法，预检请求 (CORS) |

**幂等 (Idempotent)**：同一个请求执行多次和执行一次的结果一致。POST 不幂等——多次提交会创建多条记录；PUT 幂等——多次全量更新相同内容结果不变。

**安全 (Safe)**：不会修改服务器状态。GET 和 HEAD 是安全的。

**OPTIONS 与 CORS 预检**：当浏览器发起跨域请求（如跨域 POST 带自定义头）时，会先发送一个 OPTIONS 预检请求，询问服务器是否允许真实的请求。服务器通过 `Access-Control-Allow-*` 系列头响应。

### 4.7 GET vs POST 深度对比

| 对比维度 | GET | POST |
|---|---|---|
| 语义 | 获取资源 | 提交/创建资源 |
| 参数位置 | URL 查询字符串 | 请求体 (Body) |
| URL 长度限制 | 浏览器/服务器有限制（如 2KB-8KB） | 无限制（理论上） |
| 缓存 | 可被浏览器缓存、加入书签 | 不可缓存 |
| 历史记录 | 参数会保留在浏览器历史中 | 参数不在历史中 |
| 编码类型 | application/x-www-form-urlencoded | multipart/form-data 或 application/json |
| 回退/刷新 | 幂等，安全 | 会提示重新提交表单 |
| 可见性 | 参数暴露在 URL 中 | 参数在请求体中，相对隐蔽 |

**重要提醒**：不要用 GET 还是 POST 来决定安全性——HTTPS 才是保护数据的关键。GET 参数在 URL 中可能被服务器日志、浏览器历史记录、Referer 头泄露。

---

## 五、HTTPS 加密流程（核心重点）

HTTPS = HTTP + TLS/SSL。TLS（Transport Layer Security，传输层安全协议）在 TCP 之上、HTTP 之下，为 HTTP 提供了加密、完整性和身份认证三大安全保证。

### 5.1 为什么需要 HTTPS？

HTTP 以明文传输数据，存在三大安全风险：

1. **窃听 (Eavesdropping)**：中间人可以截获通信内容，获取密码、信用卡号等敏感信息。
2. **篡改 (Tampering)**：中间人可以修改通信内容，如植入广告或恶意代码。
3. **冒充 (Impersonation)**：中间人可以伪装成服务器，与客户端建立连接并获取信任。

HTTPS 通过 TLS 解决了以上所有问题。

### 5.2 混合加密机制

HTTPS 使用**混合加密**，结合了非对称加密和对称加密的优点：

```
混合加密流程（宏观）：
1. 使用非对称加密安全地协商出一个对称密钥
2. 使用对称密钥加密实际的 HTTP 数据
```

**为什么不用纯非对称加密？**

非对称加密（RSA、ECDHE）计算量极大，比对称加密慢 2-3 个数量级，不适合加密大量数据。

**为什么不用纯对称加密？**

对称加密要求通信双方事先共享同一个密钥。在互联网环境中，双方素未谋面，如何安全地传递这个密钥本身就是一个难题。

**解决方案**：先用非对称加密安全地交换对称密钥（密钥协商），再用对称密钥高效地加密通信数据。

| 加密类型 | 算法举例 | 优点 | 缺点 |
|---|---|---|---|
| 对称加密 | AES-GCM, ChaCha20, AES-CBC | 加密速度快，适合大量数据 | 密钥分发困难 |
| 非对称加密 | RSA, ECDHE, ECDSA | 无需共享密钥即可安全通信 | 计算速度慢，不适合大量数据 |

### 5.3 TLS 1.2 握手过程（ECDHE 密钥交换）

TLS 1.2 握手由客户端和服务器多次交换消息完成，总共需要 2 个 RTT（往返时间）。以下是使用 ECDHE（椭圆曲线 Diffie-Hellman 临时密钥）的完整流程：

```
客户端                              服务端
  |                                   |
  | 1. ClientHello                  |
  |    (TLS版本, 密码套件列表,        |
  |     随机数1, Session ID)          |
  |---------------------------------->|
  |                                   |
  | 2. ServerHello                  |
  |    (选定TLS版本, 选定密码套件,    |
  |     随机数2, Session ID)          |
  |<----------------------------------|
  |                                   |
  | 3. Certificate                  |
  |    (服务端证书链)                  |
  |<----------------------------------|
  |                                   |
  | 4. ServerKeyExchange            |
  |    (ECDHE参数: 椭圆曲线,         |
  |     服务端临时公钥, 签名)          |
  |<----------------------------------|
  |                                   |
  | 5. ServerHelloDone              |
  |<----------------------------------|
  |                                   |
  | 6. ClientKeyExchange            |
  |    (客户端临时公钥)                |
  |---------------------------------->|
  |                                   |
  | 7. ChangeCipherSpec             |
  |    (告知后续将加密通信)            |
  |---------------------------------->|
  |                                   |
  | 8. Finished                     |
  |    (加密的握手消息验证)            |
  |---------------------------------->|
  |                                   |
  | 9. ChangeCipherSpec             |
  |<----------------------------------|
  |                                   |
  | 10. Finished                    |
  |    (加密的握手消息验证)            |
  |<----------------------------------|
  |                                   |
  |<===== TLS 握手完成 ============>|
  |                                   |
  | (后续应用数据使用对称加密传输)      |
  |<======== HTTP 数据 ============>|
```

**关键步骤详解**：

**步骤 1-2：Hello 阶段**。双方交换随机数（ClientHello.random 和 ServerHello.random），这两个随机数将参与后续对称密钥的生成。

**步骤 3：证书验证**。服务端发送证书链，客户端验证证书的合法性（是否由受信任的 CA 颁发、是否过期、域名是否匹配等）。

**步骤 4-6：ECDHE 密钥交换**。双方交换各自的 ECDHE 临时公钥。客户端和服务端分别用自己的私钥和对方的公钥计算出相同的**预主密钥 (Pre-Master Secret)**。然后结合之前的两个随机数，通过 PRF（伪随机函数）生成**主密钥 (Master Secret)**，再派生出加密密钥、MAC 密钥和 IV。

**步骤 7-10：握手验证**。双方各自发送 ChangeCipherSpec 表示后续通信将加密，并发送 Finished 消息（包含之前所有握手消息的 HMAC 校验）供对方验证，确保握手过程中没有被篡改。

**密码套件 (Cipher Suite)** 示例：

```
TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
|     |       |       |           |
协议  密钥交换 身份认证  对称加密    消息认证码
```

- 密钥交换：ECDHE（前向安全性）
- 身份认证：RSA
- 对称加密：AES 128 位 GCM 模式
- MAC：SHA-256

**前向安全性 (Forward Secrecy)**：使用 ECDHE 密钥交换时，即使服务器私钥被泄露，攻击者也无法解密之前截获的通信内容。因为每个会话的对称密钥依赖于临时生成的 ECDHE 私钥，与服务器长期私钥无关。RSA 密钥交换不具备前向安全性。

### 5.4 TLS 1.3 握手（更快更安全）

TLS 1.3 是对 TLS 1.2 的重大改进，主要变化包括：

#### 1-RTT 握手（正常情况）

TLS 1.3 将握手从 2-RTT 减少到 **1-RTT**，方法是将 ClientHello 和 ServerHello 后的多个消息合并发送：

```
客户端                              服务端
  |                                   |
  | 1. ClientHello                  |
  |    (TLS版本, 随机数,              |
  |     密码套件列表,                 |
  |     Key Share: 客户端ECDHE公钥)    |
  |---------------------------------->|
  |                                   |
  | 2. ServerHello                  |
  |    (选定密码套件, 随机数,          |
  |     Key Share: 服务端ECDHE公钥)    |
  | 3. EncryptedExtensions          |
  | 4. Certificate                  |
  | 5. CertificateVerify            |
  | 6. Finished                     |
  |<----------------------------------|
  |                                   |
  | 7. Finished                     |
  | (客户端此时已有对称密钥,           |
  |  立即发送HTTP数据)                |
  |---------------------------------->|
  |                                   |
  |<==== 1-RTT 完成，开始加密通信 ===>|
```

客户端在第一条消息中就携带了 ECDHE 公钥（Key Share），服务端可以立即计算出对称密钥。ServerHello 之后的证书和验证消息直接用该对称密钥加密，节省了一个 RTT。

#### 0-RTT 握手（PSK 复用）

对于之前连接过的服务端，TLS 1.3 支持 **0-RTT** 通信。客户端在 ClientHello 中携带 PSK（Pre-Shared Key，预共享密钥）标识，可以立即发送加密的应用数据，无需等待握手完成。

```
客户端                              服务端
  |                                   |
  | 1. ClientHello                  |
  |    (PSK标识, 0-RTT数据)           |
  |---------------------------------->|
  | 2. ServerHello                  |
  |    (确认PSK, 返回完成消息)         |
  |<----------------------------------|
  |                                   |
  |<=== 0-RTT: 客户端在第一次消息中   |
  |     就发送了加密的HTTP请求 ======>|
```

**0-RTT 的安全风险**：存在**重放攻击**的隐患。如果攻击者截获了 0-RTT 数据，可以重复发送给服务器。服务器需要实现重放检测机制（如基于时间戳的一次性票证）。

#### TLS 1.2 vs TLS 1.3

| 对比项 | TLS 1.2 | TLS 1.3 |
|---|---|---|
| 握手 RTT | 2-RTT | 1-RTT (正常) / 0-RTT (PSK) |
| 支持的密码套件 | 数十种 | 5 种强安全套件 |
| 前向安全性 | 可选（ECDHE） | 强制（所有套件都支持） |
| 不支持的特性 | - | RSA 密钥交换、压缩、DHE（非椭圆曲线） |
| 握手消息 | 步骤多，往返次数多 | 精简合并，减少消息数量 |

### 5.5 证书链与信任链验证

**数字证书**是 HTTPS 身份认证的基石。证书由证书颁发机构（CA，Certificate Authority）签发。

#### 证书链结构

```
      根 CA 证书（自签名，预装在操作系统/浏览器中）
              │ 签发
      中间 CA 证书
              │ 签发
      域名证书（your-site.com）
```

**信任锚**：根 CA 证书预装在操作系统和浏览器中（如 Mozilla NSS 根证书库）。这些证书是自签名的，是整个信任链的锚点。

**信任链验证过程**：

1. 服务端发送域名证书 + 中间 CA 证书（通常不发送根证书）。
2. 客户端使用中间 CA 证书的公钥验证域名证书的签名。
3. 如果中间 CA 证书不被信任，客户端查找它的签发者——根 CA。
4. 使用根 CA 证书的公钥验证中间 CA 证书的签名。
5. 根 CA 在操作系统信任库中，信任链建立。

**验证内容**：
- 数字签名是否正确
- 证书是否在有效期内（`notBefore` 和 `notAfter`）
- 证书是否被吊销（CRL 或 OCSP 查询）
- 证书的域名是否与当前访问的 URL 匹配（`Subject Alternative Names`）

#### 自签名证书

开发环境常用自签名证书。它缺少 CA 签名，浏览器会提示不安全。自签名证书加密通信功能正常，但无法通过身份验证。

### 5.6 常见安全攻击

#### 中间人攻击 (MITM, Man-in-the-Middle)

攻击者在客户端和服务器之间拦截通信，双方都不知道自己正在与中间人通信。

```
客户端 <---> 攻击者 <---> 服务端
```

**防护**：HTTPS 的证书验证机制。攻击者无法伪造服务端的证书（除非客户端信任了攻击者伪造的根 CA），因此 TLS 握手会失败。

#### SSL Stripping

攻击者在客户端和服务器之间降级通信：与客户端建立 HTTP（明文），与服务器建立 HTTPS，将服务器的加密内容解密后以明文转发给客户端。

**防护**：HSTS（HTTP Strict Transport Security）。服务器通过 `Strict-Transport-Security` 头告知客户端：在指定时间内，只能通过 HTTPS 访问该站点。浏览器首次收到 HSTS 头后，后续访问会自动使用 HTTPS，不再给 SSL Stripping 可乘之机。

#### 证书伪造

攻击者通过伪造证书伪装成合法网站。如果攻击者控制了某家 CA 或 CA 被攻陷，可以签发出合法域名的假证书。

**防护**：
- **证书透明度 (Certificate Transparency, CT)**：CA 签发的每个证书都必须记录到公开的 CT 日志中。浏览器检查证书是否有对应的 SCT（Signed Certificate Timestamp）证明。
- **证书固定 (Certificate Pinning)**：客户端硬编码或缓存期望的证书指纹，避免信任假冒证书。

### 5.7 OkHttp 配置 HTTP/2 与 HTTPS

```java
import okhttp3.*;

public class OkHttpExample {
    public static void main(String[] args) {
        // OkHttp 默认支持 HTTP/2 和 HTTPS
        // 当服务器支持时，自动升级到 HTTP/2
        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        Request request = new Request.Builder()
                .url("https://api.github.com/users/octocat")
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        // 同步请求
        try (Response response = client.newCall(request).execute()) {
            System.out.println("Protocol: " + response.protocol()); // h2 或 http/1.1
            System.out.println("Response: " + response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

```java
// 自定义 SSL 配置示例（开发环境使用自签名证书）
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class CustomSslClient {
    public static OkHttpClient createUnsafeClient() {
        try {
            // 信任所有证书（仅用于开发测试！）
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(X509Certificate[] c, String a) {}
                        @Override public void checkServerTrusted(X509Certificate[] c, String a) {}
                        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(),
                            (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

---

## 六、总结与知识自检清单

### 6.1 知识体系回顾

本文从网络分层模型出发，依次深入讲解了：

1. **网络分层模型**：OSI 七层和 TCP/IP 五层的职责、封装过程、分层意义。
2. **TCP 协议**：三次握手、四次挥手、滑动窗口、拥塞控制四个算法、Nagle 算法和 Keep-Alive。
3. **UDP 协议**：无连接、不可靠特性及其适用场景，TCP/UDP 选择决策。
4. **HTTP 协议**：从 HTTP/1.0 短连接到 HTTP/1.1 持久连接，再到 HTTP/2 多路复用和 HTTP/3 QUIC 的完整演进。
5. **HTTPS**：混合加密机制、TLS 1.2 和 TLS 1.3 握手流程、证书链验证、常见攻击及防护。

### 6.2 自检清单

请在阅读后对照以下清单检查自己的掌握程度：

| 序号 | 知识点 | 掌握程度 (1-5) |
|---|---|---|
| 1 | 能画出 OSI 七层模型，说明每层职责和代表协议 | ☐ |
| 2 | 能描述数据从应用层到物理层的封装和解封装过程 | ☐ |
| 3 | 能完整画出 TCP 三次握手和四次挥手，解释为什么是三次 | ☐ |
| 4 | 能解释 TIME_WAIT 的作用和 2MSL 原因 | ☐ |
| 5 | 能排查 CLOSE_WAIT 过多的原因（应用层未 close） | ☐ |
| 6 | 能解释滑动窗口如何实现流量控制 | ☐ |
| 7 | 能画出拥塞控制的四个状态及转换条件 | ☐ |
| 8 | 能解释 Nagle 算法和 TCP_NODELAY 的取舍 | ☐ |
| 9 | 能说出 TCP 和 UDP 各自适用场景并给出理由 | ☐ |
| 10 | 能区分 HTTP/1.1 的队头阻塞和 HTTP/2 的多路复用 | ☐ |
| 11 | 能解释 HPACK 头部压缩原理（静态表+动态表） | ☐ |
| 12 | 能说出 HTTP/3 相比 HTTP/2 的核心优势（QUIC） | ☐ |
| 13 | 能记住 200/201/204/301/302/304/400/401/403/404/429/500/502/503 状态码 | ☐ |
| 14 | 能区分 GET/POST/PUT/PATCH/DELETE 的幂等性和安全性 | ☐ |
| 15 | 能画出 TLS 1.2 ECDHE 握手完整流程 | ☐ |
| 16 | 能解释前向安全性（Forward Secrecy）的含义 | ☐ |
| 17 | 能说明 TLS 1.3 如何实现 1-RTT 和 0-RTT | ☐ |
| 18 | 能描述证书链验证过程 | ☐ |
| 19 | 能解释 SSL Stripping 攻击及 HSTS 防护原理 | ☐ |

### 6.3 推荐学习路径

1. **动手抓包分析**：使用 Wireshark 抓取真实的 TCP 三次握手、TLS 握手包，观察每个协议字段。
2. **阅读 RFC 文档**：TCP (RFC 793、RFC 5681)、TLS 1.3 (RFC 8446)、HTTP/2 (RFC 7540)、HTTP/3 (RFC 9114)。
3. **深入源码**：阅读 Netty 框架源码，理解异步网络编程；阅读 OpenSSL 或 BoringSSL 源码，理解 TLS 实现。
4. **性能调优实践**：在生产环境中调整 TCP 内核参数（`tcp_tw_reuse`、`tcp_fastopen`、`tcp_rmem`、`tcp_wmem` 等），观察性能变化。

---

*本文涵盖了网络协议从底层 TCP 到上层 HTTPS 的核心知识体系，适用于 Java 后端开发工程师的系统学习与面试准备。建议结合实战项目加深理解，在调试网络问题时回头查阅具体协议细节。*
