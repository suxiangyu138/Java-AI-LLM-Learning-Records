# Java NIO与非阻塞IO
> 核心定位：掌握Java NIO的核心组件与编程模型，解决高并发场景下BIO的性能瓶颈

## 目录
1. [BIO的痛点与NIO的诞生](#1-bio的痛点与nio的诞生)
2. [NIO核心组件总览](#2-nio核心组件总览)
3. [Buffer缓冲区](#3-buffer缓冲区)
4. [Channel通道](#4-channel通道)
5. [Selector选择器](#5-selector选择器)
6. [NIO编程实战](#6-nio编程实战)
7. [NIO进阶特性](#7-nio进阶特性)
8. [NIO与BIO对比总结](#8-nio与bio对比总结)

---

## 1. BIO的痛点与NIO的诞生

### 1.1 BIO工作机制与痛点

BIO（Blocking I/O）基于`ServerSocket`/`Socket`，采用**线程-连接**模型：

```java
public class BioServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        while (true) {
            Socket socket = serverSocket.accept();  // 阻塞等待连接
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) { // 阻塞读取
                        System.out.println("Received: " + line);
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }).start();
        }
    }
}
```

**核心痛点**：

| 痛点 | 说明 |
|------|------|
| **线程资源浪费** | 一个连接对应一个线程，无数据传输时线程空占内存 |
| **C10K瓶颈** | 线程数受OS限制（Linux默认32768），无法支撑万级并发 |
| **上下文切换频繁** | 大量线程阻塞、唤醒导致内核态/用户态频繁切换 |
| **内存开销大** | 每线程默认栈空间约1MB，万级连接需10GB以上 |

### 1.2 NIO的诞生

NIO（Non-blocking I/O / New I/O）由JDK 1.4引入，位于`java.nio`包。核心思想是**多路复用**——一个线程管理多个连接，仅在有I/O事件就绪时才处理，从根本上解决了BIO的高并发瓶颈。

> 💡 **本质区别**：BIO是"线程等数据"，NIO是"数据就绪了通知线程"。

---

## 2. NIO核心组件总览

NIO由三大核心组件构成，协同工作：

```
Buffer（数据容器） ←→ Channel（传输通道） ←→ Selector（事件监听器）
```

| 组件 | 角色 | 类比 |
|------|------|------|
| **Buffer** | 数据存储容器，所有数据读写必经Buffer | 字节数组+读写指针 |
| **Channel** | 双向数据传输通道，可注册到Selector | 比Stream多了双向性 |
| **Selector** | I/O事件多路复用器，监听多Channel就绪事件 | 事件通知器 |

**协作流程**：Selector监听Channel的I/O事件 → 事件就绪时通知线程 → Channel从Buffer读写数据。

---

## 3. Buffer缓冲区

### 3.1 核心属性

Buffer本质是一个内存块，通过四个指针管理读写状态：

| 属性 | 说明 | 初始值（写模式） |
|------|------|-----------------|
| **Capacity（容量）** | 最大容量，创建后不可修改 | 创建时指定 |
| **Position（位置）** | 当前读写位置 | 0 |
| **Limit（限制）** | 可读写数据上限 | capacity |
| **Mark（标记）** | 标记位置，`reset()`可返回 | -1（未标记） |

```
写模式：position →            limit → (capacity)
读模式（flip后）：position=0 → limit=写入的数据量
```

### 3.2 核心方法

| 方法 | 说明 | 状态变化 |
|------|------|---------|
| `allocate(int capacity)` | 创建指定容量Buffer | position=0, limit=capacity |
| `put(byte[] src)` | 写入数据 | position后移 |
| `flip()` | 切换读模式 | limit=position, position=0 |
| `get()` | 读取数据 | position后移 |
| `clear()` | 清空，切回写模式 | position=0, limit=capacity |
| `compact()` | 保留未读数据，切写模式 | 未读数据移至开头 |
| `rewind()` | 重读 | position=0 |
| `remaining()` | 剩余可读写元素数 | 返回limit - position |

```java
ByteBuffer buffer = ByteBuffer.allocate(10);
buffer.put((byte) 'H'); buffer.put((byte) 'E');  // position=2
buffer.flip();  // 读模式：position=0, limit=2
while (buffer.hasRemaining()) {
    System.out.print((char) buffer.get());  // 输出: HE
}
buffer.clear();  // 切回写模式
```

> ⚠️ **常见错误**：`flip()`后若继续写入会覆盖未读数据，需保留未读数据时用`compact()`而非`clear()`。

### 3.3 Buffer类型

| Buffer类型 | 底层类型 | 主要用途 |
|------------|---------|---------|
| **ByteBuffer** | byte[] | **网络编程最常用** |
| CharBuffer | char[] | 字符处理 |
| IntBuffer / LongBuffer / etc. | int[] / long[] | 基本类型数据 |

### 3.4 Direct vs Heap ByteBuffer

```java
ByteBuffer heapBuf = ByteBuffer.allocate(1024);           // 堆内存
ByteBuffer directBuf = ByteBuffer.allocateDirect(1024);   // 直接内存
ByteBuffer wrapBuf = ByteBuffer.wrap(new byte[1024]);     // 包装数组
```

| 对比维度 | Heap ByteBuffer | Direct ByteBuffer |
|---------|----------------|-------------------|
| **内存位置** | JVM堆 | OS直接内存（堆外） |
| **GC管理** | 受GC管理 | 不受GC管理 |
| **分配速度** | 快 | 慢（系统调用） |
| **I/O性能** | 慢（需一次额外拷贝） | 快（OS直接操作） |
| **适用场景** | 小数据量、临时Buffer | 大数据量、网络I/O、长生命周期 |

> 💡 网络编程中写入SocketChannel优先用Direct ByteBuffer，避免额外拷贝。Netty通过池化技术解决Direct Buffer分配慢的问题。

---

## 4. Channel通道

### 4.1 Channel vs Stream

| 对比维度 | Stream（BIO） | Channel（NIO） |
|---------|--------------|----------------|
| 方向性 | 单向（InputStream/OutputStream） | **双向**（读写同一Channel） |
| 操作模式 | 阻塞 | 可阻塞、可非阻塞 |
| 数据载体 | byte[] | **Buffer**（数据须经Buffer中转） |
| Selector注册 | 不支持 | 支持 |
| 零拷贝 | 不支持 | 支持（transferTo/transferFrom） |

### 4.2 Channel类型

| Channel类型 | 用途 | 非阻塞 |
|------------|------|:------:|
| **SocketChannel** | TCP客户端，读写网络数据 | 是 |
| **ServerSocketChannel** | TCP服务端，监听连接 | 是 |
| **DatagramChannel** | UDP数据包收发 | 是 |
| **FileChannel** | 文件I/O（零拷贝、内存映射、文件锁） | 否 |
| **Pipe.SinkChannel / SourceChannel** | 线程间通信 | 是 |

```java
// SocketChannel基础用法
SocketChannel channel = SocketChannel.open();
channel.configureBlocking(false);  // 必须非阻塞才能注册Selector
channel.connect(new InetSocketAddress("localhost", 8080));
while (!channel.finishConnect()) { /* 等待连接完成 */ }

ByteBuffer buf = ByteBuffer.allocate(1024);
channel.read(buf);   // 非阻塞：无数据时返回0，-1表示对端关闭
buf.flip();
channel.write(buf);  // 非阻塞：可能未写完所有数据
```

### 4.3 ServerSocketChannel

```java
ServerSocketChannel server = ServerSocketChannel.open();
server.configureBlocking(false);
server.bind(new InetSocketAddress(8080));
SocketChannel client = server.accept();  // 非阻塞：无连接返回null
```

---

## 5. Selector选择器

### 5.1 核心原理

Selector是多路复用的核心——**一个线程监听多个Channel的I/O事件**：

```
Thread → Selector
              ├── ServerSocketChannel (OP_ACCEPT)
              ├── SocketChannel #1 (OP_READ)
              ├── SocketChannel #2 (OP_READ)
              └── SocketChannel #3 (OP_WRITE)
```

### 5.2 核心方法

| 方法 | 说明 |
|------|------|
| `Selector.open()` | 创建Selector |
| `channel.register(sel, ops)` | 注册Channel和感兴趣的事件，返回SelectionKey |
| `select()` | **阻塞**等待至少一个Channel就绪 |
| `select(long timeout)` | 带超时的阻塞等待 |
| `selectNow()` | **非阻塞**立即返回，无事件返回0 |
| `selectedKeys()` | 获取就绪的SelectionKey集合 |
| `wakeup()` | 唤醒阻塞在select()上的线程 |

### 5.3 SelectionKey事件类型

```java
SelectionKey.OP_ACCEPT  = 16  // 接收连接（仅ServerSocketChannel）
SelectionKey.OP_CONNECT = 8   // 连接就绪（客户端SocketChannel）
SelectionKey.OP_READ    = 1   // 读就绪（有数据可读）
SelectionKey.OP_WRITE   = 4   // 写就绪（可写入数据）
```

**SelectionKey常用方法**：

| 方法 | 说明 |
|------|------|
| `isAcceptable()` / `isReadable()` / `isWritable()` | 检查事件就绪状态 |
| `channel()` | 获取对应的Channel |
| `attachment()` / `attach(Object)` | 附加/获取自定义对象（如Buffer） |
| `interestOps(int ops)` | 动态修改关注的事件 |
| `cancel()` | 取消注册 |

> ⚠️ **OP_WRITE陷阱**：Socket发送缓冲区大部分时间空闲，OP_WRITE**几乎永远就绪**。仅在确有大量数据待发送时注册OP_WRITE，发送完成后立即取消，否则`select()`会持续返回导致CPU空转。

---

## 6. NIO编程实战

### 6.1 完整NIO Echo服务器

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class NioEchoServer {
    private static final int PORT = 8080;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("NIO Echo Server started on port " + PORT);

        while (true) {
            if (selector.select() == 0) continue;
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();  // 必须移除，防重复处理

                try {
                    if (key.isAcceptable()) handleAccept(key, selector);
                    else if (key.isReadable()) handleRead(key);
                    else if (key.isWritable()) handleWrite(key);
                } catch (IOException e) {
                    key.cancel();
                    key.channel().close();
                }
            }
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        sc.register(selector, SelectionKey.OP_READ, buffer);
        System.out.println("Client: " + sc.getRemoteAddress());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = sc.read(buffer);
        if (bytesRead == -1) {  // 客户端断开
            sc.close();
            key.cancel();
            return;
        }
        if (bytesRead > 0) {
            buffer.flip();
            key.interestOps(SelectionKey.OP_WRITE);  // 切为写模式，回显数据
        }
    }

    private static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        sc.write(buffer);
        if (!buffer.hasRemaining()) {  // 数据写完
            buffer.compact();
            key.interestOps(SelectionKey.OP_READ);  // 切回读模式
        }
    }
}
```

### 6.2 完整NIO Echo客户端

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class NioEchoClient {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(HOST, PORT));

        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_CONNECT);

        // 读取线程：处理服务器响应
        new Thread(() -> {
            try {
                while (true) {
                    if (selector.select() == 0) continue;
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        SocketChannel sc = (SocketChannel) key.channel();

                        if (key.isConnectable()) {
                            if (sc.finishConnect()) {
                                sc.register(selector, SelectionKey.OP_READ);
                                System.out.println("Connected");
                            }
                        } else if (key.isReadable()) {
                            ByteBuffer buf = ByteBuffer.allocate(1024);
                            int n = sc.read(buf);
                            if (n > 0) {
                                buf.flip();
                                byte[] data = new byte[buf.remaining()];
                                buf.get(data);
                                System.out.print("Echo: ");
                                System.out.write(data);
                                System.out.println();
                            }
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();

        // 主线程：发送用户输入
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String msg = scanner.nextLine();
                if ("quit".equalsIgnoreCase(msg)) break;
                ByteBuffer buf = ByteBuffer.wrap((msg + "\n").getBytes());
                while (buf.hasRemaining()) channel.write(buf);
            }
        }
        channel.close();
        selector.close();
    }
}
```

### 6.3 编程要点

1. 所有注册到Selector的Channel必须`configureBlocking(false)`
2. 遍历`selectedKeys()`必须`remove()`，否则重复处理
3. `read()`返回-1表示对端断开，务必关闭Channel并`cancel()` Key
4. 非阻塞`write()`可能未写完，需缓存剩余数据等待下次OP_WRITE
5. 使用`key.attachment()`附加Buffer，避免为每个Channel重复分配

---

## 7. NIO进阶特性

### 7.1 FileChannel零拷贝

FileChannel的`transferTo()` / `transferFrom()`实现**零拷贝**——数据直接从内核缓冲区传输到目标Channel，无需经过用户空间：

```java
// 零拷贝文件复制
try (FileChannel src = FileChannel.open(Paths.get("source.dat"), StandardOpenOption.READ);
     FileChannel dst = FileChannel.open(Paths.get("dest.dat"),
             StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
    src.transferTo(0, src.size(), dst);  // 内核态直接传输
}
```

**零拷贝 vs 传统拷贝**：
- 传统拷贝：磁盘 → 内核缓冲区 → 用户缓冲区 → 内核Socket缓冲区 → 网卡（4次拷贝+4次切换）
- 零拷贝：磁盘 → 内核缓冲区 → 网卡（2次拷贝+2次切换，DMA直传）

**FileChannel其他特性**：

```java
// 文件锁
try (FileLock lock = channel.lock(0, Long.MAX_VALUE, false)) { /* 独占写入 */ }

// 内存映射文件（MappedByteBuffer）
MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
mapped.put(0, (byte) 'A');  // 直接操作内存，系统自动同步到文件
mapped.force();             // 强制刷新到磁盘
```

| FileChannel特性 | 用途 |
|----------------|------|
| `transferTo/transferFrom` | 零拷贝文件传输、下载加速 |
| `FileLock` | 多进程文件同步 |
| `MappedByteBuffer` | 大文件随机读写、共享内存 |

### 7.2 Pipe管道

同JVM内线程间通信，包含SinkChannel（写端）和SourceChannel（读端）：

```java
Pipe pipe = Pipe.open();
// 写线程
new Thread(() -> {
    pipe.sink().write(ByteBuffer.wrap("Hello".getBytes()));
    pipe.sink().close();
}).start();
// 读线程
new Thread(() -> {
    ByteBuffer buf = ByteBuffer.allocate(1024);
    pipe.source().read(buf);
    buf.flip();
    System.out.println(new String(buf.array(), 0, buf.remaining()));
    pipe.source().close();
}).start();
```

### 7.3 Scatter / Gather

一次I/O操作读取/写入多个Buffer，适合固定协议头+可变消息体场景：

```java
ByteBuffer header = ByteBuffer.allocate(128);
ByteBuffer body = ByteBuffer.allocate(1024);
channel.read(new ByteBuffer[]{header, body});   // 分散读取（先填满header再填body）
channel.write(new ByteBuffer[]{header, body});  // 聚集写入（先写header再写body）
```

### 7.4 常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| **Selector空轮询** | JDK Linux epoll bug，select()返回0但无事件 | 记录空轮询次数，超阈值重建Selector（Netty方案） |
| **OP_WRITE持续触发** | 发送缓冲区一直空闲 | 仅在有数据要发时注册OP_WRITE，发完取消 |
| **Buffer未flip** | 写后直接读，忘记切换模式 | 写后`flip()`，读后`compact()`或`clear()` |
| **read()返回-1忽略** | 对端关闭连接 | 检测-1后关闭Channel并取消Key |
| **Buffer容量不合适** | 过大浪费内存，过小频繁复制 | 根据消息大小动态调整/使用Netty ByteBuf |

---

## 8. NIO与BIO对比总结

| 对比维度 | BIO | NIO |
|---------|-----|-----|
| **JDK版本** | JDK 1.0 | JDK 1.4（NIO），JDK 7（NIO.2/AIO） |
| **数据载体** | Stream（单向流） | Channel + Buffer（双向通道+缓冲区） |
| **线程模型** | 一个连接一个线程 | 一个线程管理多连接（Selector多路复用） |
| **I/O模式** | 阻塞：accept/read/write均阻塞 | 非阻塞：Selector监听就绪状态 |
| **资源消耗** | 高（大量线程） | 低（少量线程） |
| **高并发支持** | 差（C10K瓶颈） | 好（C10K+/C100K） |
| **编程复杂度** | 低 | 中高（需理解三组件和事件模型） |
| **适用场景** | 低并发、短连接、固定连接数 | **高并发、长连接、连接数不确定** |
| **代表框架** | 传统Tomcat BIO | Netty、Tomcat NIO、Vert.x、gRPC |

**选型建议**：

```
连接数<1000、低并发  → BIO（简单直接）
连接数>1000、高并发  → NIO（Netty/WebFlux/Vert.x）
极致异步场景        → AIO（较少使用）
```

> 🎯 **NIO是Netty的基础**：几乎所有Java高性能网络框架（Netty、Tomcat NIO、gRPC、Dubbo）底层都基于NIO。Netty对NIO的增强包括：解决Selector空轮询bug、提供ByteBuf池化与自动扩容、Reactor线程模型、更易用的编解码API。掌握NIO三大组件是理解这些框架的前提。

### 核心要点速记

1. **三大组件**：Buffer（容器）、Channel（双向通道）、Selector（事件多路复用器）
2. **Buffer四指针**：capacity、position、limit、mark；`flip()`/`clear()`/`compact()`切换模式
3. **Channel特点**：双向读写、支持非阻塞、可注册Selector、支持零拷贝
4. **Selector原理**：单线程管理多Channel I/O事件，`select()`阻塞等待就绪
5. **四类事件**：OP_ACCEPT（接收连接）、OP_CONNECT（连接就绪）、OP_READ（读）、OP_WRITE（写）
6. **编程步骤**：配置非阻塞 → 创建Selector → 注册事件 → 事件循环 → 分发处理
7. **零拷贝**：FileChannel.transferTo()避免用户态/内核态数据拷贝
