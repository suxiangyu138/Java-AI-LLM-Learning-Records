# Java网络编程基础（BIO）
> 核心定位：掌握Java基于TCP/UDP的阻塞式网络编程核心API与编程模型，为后续学习NIO、Netty等高阶框架打下基础

## 目录
1. [Java网络编程概述](#1-java网络编程概述)
2. [InetAddress与网络信息](#2-inetaddress与网络信息)
3. [URL与URI](#3-url与uri)
4. [TCP编程：Socket与ServerSocket](#4-tcp编程socket与serversocket)
5. [UDP编程：DatagramSocket](#5-udp编程datagramsocket)
6. [IP组播编程](#6-ip组播编程)
7. [多线程网络服务器](#7-多线程网络服务器)
8. [URLConnection与HttpURLConnection](#8-urlconnection与httpurlconnection)
9. [常见问题与优化](#9-常见问题与优化)

---

## 1. Java网络编程概述

### 1.1 核心要素

Java网络编程依托TCP/IP协议簇，核心三要素：

| 要素 | 说明 | Java类 |
|------|------|--------|
| IP地址 | 设备唯一标识 | `InetAddress` |
| 端口号 | 进程唯一标识（0-65535） | int |
| 协议 | 数据传输规则 | `Socket`(TCP), `DatagramSocket`(UDP) |

### 1.2 TCP/IP分层与Java对应

| 分层 | 职责 | Java实现 |
|------|------|----------|
| 应用层 | 具体网络服务 | `HttpURLConnection`, `HttpClient` |
| 传输层 | 端到端传输 | `Socket`, `DatagramSocket` |
| 网络层 | IP寻址/路由 | `InetAddress` |
| 链路层 | 物理传输 | 操作系统/硬件实现 |

### 1.3 BIO vs NIO

| 维度 | BIO（本章焦点） | NIO |
|------|----------------|-----|
| 阻塞模型 | accept()/read()阻塞线程 | Selector多路复用 |
| 线程模型 | 1连接=1线程 | 1线程管理N连接 |
| 适用场景 | 连接数少、并发低 | 高并发 |
| 核心类 | Socket, ServerSocket | SocketChannel, Selector |

---

## 2. InetAddress与网络信息

### 2.1 InetAddress

封装IP地址和主机名，无构造方法，通过静态方法获取。

| 方法 | 说明 |
|------|------|
| `getByName(String host)` | 根据主机名/IP字符串获取 |
| `getLocalHost()` | 获取本机信息 |
| `getAllByName(String host)` | 获取域名所有IP |
| `getHostName()` / `getHostAddress()` | 获取主机名/IP |
| `isReachable(int timeout)` | 测试可达性 |

```java
InetAddress baidu = InetAddress.getByName("www.baidu.com");
System.out.println("IP: " + baidu.getHostAddress());

InetAddress local = InetAddress.getLocalHost();
System.out.println("本机: " + local.getHostAddress());
```

### 2.2 InetSocketAddress

封装IP+端口：

```java
InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8080);
```

### 2.3 NetworkInterface

```java
Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
while (nis.hasMoreElements()) {
    NetworkInterface ni = nis.nextElement();
    System.out.println(ni.getName() + ": " + ni.getDisplayName());
}
```

---

## 3. URL与URI

### 3.1 URI vs URL

| 维度 | URI | URL |
|------|-----|-----|
| 作用 | 标识资源 | 定位+访问资源 |
| 范围 | 包含URL和URN | URI的子集 |
| 协议 | 可选 | 必须包含 |
| 示例 | `urn:isbn:...` | `https://www.baidu.com` |

> 所有URL都是URI，但URI不一定是URL。

### 3.2 URL类

```java
URL url = new URL("https://www.example.com:8080/docs/api?name=java");
System.out.println("协议: " + url.getProtocol());
System.out.println("主机: " + url.getHost());
System.out.println("端口: " + url.getPort());
System.out.println("路径: " + url.getPath());
System.out.println("查询: " + url.getQuery());

// 直接读取资源
try (InputStream is = url.openStream();
     BufferedReader br = new BufferedReader(
         new InputStreamReader(is, StandardCharsets.UTF_8))) {
    br.lines().forEach(System.out::println);
}
```

### 3.3 URI类

```java
URI uri = URI.create("https://www.example.com/docs/api?name=java");
System.out.println("路径: " + uri.getPath());

// 相对路径解析
URI resolved = uri.resolve("test.html");
System.out.println("解析后: " + resolved);

// 编码解码
String encoded = URLEncoder.encode("测试 页面", StandardCharsets.UTF_8);
String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
```

---

## 4. TCP编程：Socket与ServerSocket

### 4.1 ServerSocket（服务器端）

**构造**：`ServerSocket(int port)`、`ServerSocket(int port, int backlog)`

**核心方法**：

| 方法 | 说明 |
|------|------|
| `accept()` | 阻塞等待客户端连接，返回Socket |
| `close()` | 关闭，释放端口 |
| `setSoTimeout(int)` | 设置accept()超时 |
| `setReuseAddress(boolean)` | 端口复用（bind前调用） |

### 4.2 Socket（客户端）

**构造**：`Socket(String host, int port)`、`Socket(InetAddress, int)`

**核心方法**：

| 方法 | 说明 |
|------|------|
| `getInputStream()` / `getOutputStream()` | 获取数据流 |
| `connect(SocketAddress, int timeout)` | 手动连接 |
| `setSoTimeout(int)` | 设置读取超时 |
| `shutdownOutput()` | 关闭输出（发送EOF） |
| `close()` | 关闭连接 |

### 4.3 TCP回声服务器

**服务器端**：

```java
public class TcpEchoServer {
    public static void main(String[] args) throws IOException {
        try (ServerSocket ss = new ServerSocket(8888)) {
            System.out.println("服务端启动，端口 8888");
            try (Socket client = ss.accept()) {
                client.setSoTimeout(10000);
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("收到: " + msg);
                    out.println("回声: " + msg);
                }
            }
        }
    }
}
```

**客户端**：

```java
public class TcpEchoClient {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 8888)) {
            socket.setSoTimeout(5000);
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            out.println("Hello TCP!");
            System.out.println("收到: " + in.readLine());
        } catch (ConnectException e) {
            System.err.println("连接失败，服务端未启动");
        }
    }
}
```

### 4.4 Socket选项

| 选项 | 方法 | 说明 |
|------|------|------|
| SO_TIMEOUT | `setSoTimeout(int ms)` | 读取超时（推荐始终设置） |
| SO_REUSEADDR | `setReuseAddress(boolean)` | 端口复用 |
| TCP_NODELAY | `setTcpNoDelay(boolean)` | 禁用Nagle算法 |
| SO_KEEPALIVE | `setKeepAlive(boolean)` | 检测连接存活 |
| SO_LINGER | `setSoLinger(boolean, int)` | 关闭等待时间 |

> 务必设置 `setSoTimeout()`，避免网络异常导致线程永久阻塞。

---

## 5. UDP编程：DatagramSocket

### 5.1 UDP vs TCP

| 维度 | UDP | TCP |
|------|-----|-----|
| 连接性 | 无连接 | 面向连接 |
| 可靠性 | 不可靠（可能丢失乱序） | 可靠 |
| 传输方式 | 数据报（有边界） | 字节流（无边界） |
| 速度 | 快 | 较慢 |
| 场景 | 视频、语音、游戏 | 文件、HTTP、登录 |

### 5.2 核心类

**DatagramSocket**：

| 构造 | 说明 |
|------|------|
| `DatagramSocket()` | 随机端口（发送方） |
| `DatagramSocket(int port)` | 指定端口（接收方） |

| 方法 | 说明 |
|------|------|
| `send(DatagramPacket)` | 发送数据报 |
| `receive(DatagramPacket)` | 接收（阻塞） |
| `setSoTimeout(int)` | 接收超时 |

**DatagramPacket**：

| 场景 | 构造 |
|------|------|
| 接收 | `DatagramPacket(byte[] buf, int length)` |
| 发送 | `DatagramPacket(byte[] buf, int len, InetAddress addr, int port)` |

### 5.3 UDP回声示例

**接收方**：

```java
public class UdpEchoServer {
    public static void main(String[] args) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(9999)) {
            System.out.println("UDP服务端启动，端口 9999");
            socket.setSoTimeout(30000);

            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            System.out.println("收到: " + msg);

            byte[] resp = ("回声: " + msg).getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(resp, resp.length,
                    packet.getAddress(), packet.getPort()));
        }
    }
}
```

**发送方**：

```java
public class UdpEchoClient {
    public static void main(String[] args) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000);
            byte[] data = "Hello UDP".getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length,
                    InetAddress.getByName("127.0.0.1"), 9999));

            byte[] buf = new byte[1024];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            socket.receive(p);
            System.out.println("收到: " + new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8));
        }
    }
}
```

### 5.4 注意事项

| 要点 | 说明 |
|------|------|
| 数据报长度 | 最大65535字节（头部8字节），有效数据最多65507字节 |
| 不可靠性 | 应用层自行处理丢失/乱序（序列号+重传） |
| 缓冲区 | 太小截断数据，推荐1024或MTU大小(1500) |
| 阻塞receive() | 务必设置setSoTimeout() |

---

## 6. IP组播编程

### 6.1 组播概念

"一对多"通信：发送者将数据发送到组播地址，所有加入该组的接收者都能收到。

| 模式 | 带宽效率 | 场景 |
|------|----------|------|
| 单播(1:1) | N次发送 | 普通通信 |
| 广播(1:all) | 占所有主机 | ARP, DHCP |
| 组播(1:many) | 一次发送，路由器复制 | 视频会议、股票行情 |

### 6.2 组播地址

IPv4组播范围：`224.0.0.0` ~ `239.255.255.255`

| 类型 | 范围 | 说明 |
|------|------|------|
| 本地链路 | 224.0.0.0 - 224.0.0.255 | 仅局域网 |
| 全球组播 | 224.0.1.0 - 238.255.255.255 | 可跨网段 |
| 管理权限 | 239.0.0.0 - 239.255.255.255 | 私有 |

常用：`224.0.0.1`（本地所有主机）、`224.0.0.2`（本地组播路由器）。

### 6.3 MulticastSocket

继承自 `DatagramSocket`，关键方法：

| 方法 | 说明 |
|------|------|
| `joinGroup(InetAddress group)` | 加入组播组 |
| `leaveGroup(InetAddress group)` | 退出 |
| `setTimeToLive(int ttl)` | 0=本机, 1=局域网, >1=跨网段 |

**发送者**：

```java
try (MulticastSocket socket = new MulticastSocket()) {
    socket.setTimeToLive(1);
    InetAddress group = InetAddress.getByName("224.0.0.1");
    String msg = "组播测试";
    DatagramPacket p = new DatagramPacket(msg.getBytes(), msg.length(), group, 8888);
    socket.send(p);
}
```

**接收者**：

```java
try (MulticastSocket socket = new MulticastSocket(8888)) {
    InetAddress group = InetAddress.getByName("224.0.0.1");
    socket.joinGroup(group);
    socket.setSoTimeout(10000);
    byte[] buf = new byte[1024];
    DatagramPacket p = new DatagramPacket(buf, buf.length);
    socket.receive(p);
    System.out.println("收到: " + new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8));
    socket.leaveGroup(group);
}
```

---

## 7. 多线程网络服务器

### 7.1 单线程局限

`accept()` 和 `read()` 均为阻塞方法，处理一个客户端时无法接受其他连接。

### 7.2 线程池服务器

```java
public class ThreadPoolTcpServer {
    private static final ExecutorService pool =
        new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    public static void main(String[] args) throws IOException {
        try (ServerSocket ss = new ServerSocket(8888)) {
            System.out.println("多线程服务器启动");
            while (true) {
                Socket client = ss.accept();
                pool.submit(() -> {
                    try (client;
                         BufferedReader in = new BufferedReader(
                             new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                         PrintWriter out = new PrintWriter(
                             new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
                        String msg;
                        while ((msg = in.readLine()) != null) {
                            out.println("已收到: " + msg);
                        }
                    } catch (IOException e) {
                        System.err.println("处理异常: " + e.getMessage());
                    }
                });
            }
        }
    }
}
```

### 7.3 I/O流使用要点

| 流类型 | 类 | 场景 |
|--------|-----|------|
| 字节流 | `InputStream`/`OutputStream` | 二进制数据（文件、图片） |
| 字符流 | `Reader`/`Writer` | 文本数据（JSON、HTML） |
| 缓冲流 | `BufferedXxx` | 减少系统调用，提升性能 |
| 转换流 | `InputStreamReader`/`OutputStreamWriter` | 字节字符转换 |

常用组合：

```java
BufferedReader in = new BufferedReader(
    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
PrintWriter out = new PrintWriter(
    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
```

---

## 8. URLConnection与HttpURLConnection

### 8.1 类层次

```
URLConnection（抽象类）
  ├── HttpURLConnection    HTTP/HTTPS
  ├── JarURLConnection     JAR包
  └── FileURLConnection     file协议
```

### 8.2 HttpURLConnection核心方法

| 类别 | 方法 |
|------|------|
| 方法 | `setRequestMethod("GET"/"POST")` |
| 请求头 | `setRequestProperty(key, value)` |
| 超时 | `setConnectTimeout(int)` / `setReadTimeout(int)` |
| 输出 | `setDoOutput(boolean)`（POST需设为true） |
| 响应码 | `getResponseCode()` |
| 读取 | `getInputStream()` / `getErrorStream()` |
| 关闭 | `disconnect()` |

### 8.3 GET请求

```java
URL url = new URL("https://api.example.com/user?id=1");
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setConnectTimeout(5000);
conn.setReadTimeout(5000);
conn.setRequestProperty("Accept", "application/json");

int code = conn.getResponseCode();
if (code == HttpURLConnection.HTTP_OK) {
    try (BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        System.out.println(br.lines().collect(Collectors.joining()));
    }
}
conn.disconnect();
```

### 8.4 POST请求

```java
URL url = new URL("https://api.example.com/login");
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("POST");
conn.setDoOutput(true);
conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

String json = "{\"username\":\"admin\"}";
try (OutputStream os = conn.getOutputStream()) {
    os.write(json.getBytes(StandardCharsets.UTF_8));
}

if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
    try (BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        System.out.println(br.lines().collect(Collectors.joining()));
    }
}
conn.disconnect();
```

### 8.5 使用要点

| 要点 | 说明 |
|------|------|
| 配置必须在connect()之前 | 所有setXxx()需在connect()前调用 |
| POST必须setDoOutput(true) | 否则无法发送请求体 |
| 判断状态码再读流 | 错误时用getErrorStream() |
| 流必须关闭 | 使用try-with-resources |
| 单次使用 | 每个URLConnection只能用于一次请求 |

---

## 9. 常见问题与优化

### 9.1 常见异常

| 异常 | 原因 | 解决 |
|------|------|------|
| `BindException` | 端口被占用 | 换端口；`setReuseAddress(true)` |
| `ConnectException: Connection refused` | 服务端未启动/端口错 | 确认服务端；核对IP和端口 |
| `ConnectException: Connection timed out` | 网络不通 | 检查网络；设置超时 |
| `SocketTimeoutException` | 读取超时 | `setSoTimeout()` |
| `UnknownHostException` | DNS解析失败 | 检查域名/DNS配置 |
| `SocketException: Broken pipe` | 连接已关闭仍写入 | 检查连接状态 |
| `SocketException: Connection reset` | 对端突然关闭 | 增加异常处理 |

### 9.2 BIO瓶颈与优化

| 瓶颈 | 说明 | 优化方案 |
|------|------|----------|
| 线程开销 | 每连接一线程 | 线程池ThreadPoolExecutor |
| 阻塞等待 | read()阻塞线程 | setSoTimeout()+合理超时 |
| C10K问题 | 难支撑上万连接 | 升级NIO/Netty |
| 上下文切换 | 大量线程频繁切换 | 控制线程数 |

**连接管理**：

```java
Socket socket = new Socket();
socket.connect(new InetSocketAddress(host, port), 5000); // 连接超时
socket.setSoTimeout(5000); // 读取超时
```

**资源管理**（始终使用try-with-resources）：

```java
try (Socket socket = new Socket(host, port);
     BufferedReader in = new BufferedReader(
         new InputStreamReader(socket.getInputStream()));
     PrintWriter out = new PrintWriter(
         new OutputStreamWriter(socket.getOutputStream()), true)) {
    // 通信逻辑
}
```

### 9.3 端口排查

```bash
# Windows
netstat -ano | findstr 8888
# Linux
netstat -anp | grep 8888
# 或
lsof -i :8888
```

### 9.4 BIO → NIO演进

```
BIO: 1连接=1线程, 阻塞读写
  ↓
NIO: Selector + Channel + Buffer, 单线程管理N连接
  ↓
Netty: 基于NIO的高性能网络框架
```

> 本文件的BIO模型是Java网络编程的入门基础。理解BIO的工作原理和局限后，学习NIO和Netty时会更深刻理解其设计初衷。

---

> ⚠️ 三大铁律：始终设置超时、始终关闭资源（try-with-resources）、理解BIO的线程模型局限。
