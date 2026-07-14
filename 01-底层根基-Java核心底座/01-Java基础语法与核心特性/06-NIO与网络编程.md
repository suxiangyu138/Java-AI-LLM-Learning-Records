# 06 — NIO 与网络编程

> **IO 模型是服务端架构的核心——理解 NIO 是理解 Netty、Tomcat、Redis 的前提**

---

## 目录

1. [IO 模型演进](#1-io-模型演进)
2. [Unix 五大 IO 模型](#2-unix-五大-io-模型)
3. [BIO 详解](#3-bio-详解)
4. [NIO 核心组件](#4-nio-核心组件)
5. [Buffer 详解](#5-buffer-详解)
6. [Channel 详解](#6-channel-详解)
7. [Selector 与多路复用](#7-selector-与多路复用)
8. [Reactor 模式](#8-reactor-模式)
9. [AIO (NIO.2)](#9-aio-nio2)
10. [零拷贝技术](#10-零拷贝技术)
11. [Netty 入门](#11-netty-入门)
12. [TCP 网络基础](#12-tcp-网络基础)
13. [实战：NIO HTTP 服务器](#13-实战nio-http-服务器)
14. [面试经典问题](#14-面试经典问题)

---

## 1. IO 模型演进

```
Java IO 模型演进时间线：

JDK 1.0       ─── BIO (java.io)
                  同步阻塞 IO，每个连接一个线程

JDK 1.4       ─── NIO (java.nio)
                  同步非阻塞 IO，多路复用
                  Channel, Buffer, Selector

JDK 7 (NIO.2) ─── AIO (Asynchronous Channel)
                  真正的异步非阻塞 IO
                  CompletionHandler 回调模式

Netty (4.x)   ─── 封装 NIO，解决 NIO 开发复杂度
                  Reactor 模式，零拷贝
```

---

## 2. Unix 五大 IO 模型

### 2.1 模型对比

```
1. 阻塞 IO (BIO):
   用户进程                内核
   ┌─────┐               ┌─────┐
   │read()│──────────────▶│等待数据│
   │阻塞  │               │数据到│
   │      │◀──────────────│拷贝到│
   │      │               │用户  │
   └─────┘               └─────┘

2. 非阻塞 IO:
   用户进程                内核
   ┌─────┐               ┌─────┐
   │read()│─────▶返回EWOULDBLOCK│
   │轮询  │─────▶返回EWOULDBLOCK│
   │      │─────▶...    │数据到│
   │      │◀────数据就绪│拷贝到│
   └─────┘               └─────┘

3. IO 多路复用 (select/poll/epoll):
   用户进程                内核
   ┌─────┐               ┌─────┐
   │select│─────────────▶│监控  │
   │阻塞  │               │多个FD│
   │      │◀────数据就绪│就绪列表│
   │read()│─────────────▶│拷贝  │
   └─────┘               └─────┘

4. 信号驱动 IO:
   用户进程                内核
   ┌─────┐               ┌─────┐
   │注册  │─────────────▶│      │
   │信号  │               │数据到│
   │      │◀─────SIGIO  │      │
   │read()│─────────────▶│拷贝  │
   └─────┘               └─────┘

5. 异步 IO (AIO):
   用户进程                内核
   ┌─────┐               ┌─────┐
   │aio  │─────────────▶│等待+│
   │_read│               │拷贝 │
   │继续  │               │全部完成│
   │其他  │               │      │
   │工作  │◀───────回调  │通知  │
   └─────┘               └─────┘
```

### 2.2 同步 vs 异步，阻塞 vs 非阻塞

| | 阻塞 | 非阻塞 |
|------|------|--------|
| **同步** | BIO: 数据就绪前线程阻塞 | NIO: 反复检查，不阻塞线程 |
| **异步** | — | AIO: 数据就绪后自动回调 |

**关键区别**：
- **阻塞/非阻塞**：线程在等待数据时是否被挂起
- **同步/异步**：数据由谁拷贝到用户空间（用户线程 vs 内核完成）

---

## 3. BIO 详解

### 3.1 传统 BIO 模型

```java
/**
 * BIO 服务器：一连接一线程
 * 问题：连接数增长时线程数暴增，C10K 问题
 */
public class BIOServer {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("BIO Server started on 8080");

        while (true) {
            // main 线程阻塞在 accept()
            Socket socket = serverSocket.accept();
            System.out.println("New connection from: " + socket.getRemoteSocketAddress());

            // 每个连接创建一个线程（资源消耗大！）
            new Thread(() -> handleConnection(socket)).start();
        }
    }

    private static void handleConnection(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            // readLine() 阻塞 — 如果客户端不发送数据，线程被挂起
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
                out.println("Echo: " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * 改造：使用线程池（伪异步 BIO）
 * 问题：线程池中的线程仍然会阻塞等待 IO
 */
public class BioThreadPoolServer {
    private static final ExecutorService THREAD_POOL =
            Executors.newFixedThreadPool(100); // 最多 100 个线程

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8081);

        while (true) {
            Socket socket = serverSocket.accept();
            THREAD_POOL.submit(() -> handleConnection(socket));
        }
    }
    // 仍然有瓶颈：线程数受限，100+ 连接可能排队
}
```

### 3.2 BIO 的局限性

| 问题 | 表现 | 影响 |
|------|------|------|
| 阻塞 IO | read()/accept() 阻塞 | 线程挂起，资源浪费 |
| 线程数限制 | 一连接一线程 | 1000 连接 = 1000 线程 |
| 上下文切换 | 线程频繁切换 | CPU 浪费 |
| 内存占用 | 每线程栈 1MB | 1000 线程 ≈ 1GB 内存 |

> **C10K 问题**：如何处理 1 万个并发连接？BIO 显然不行。

---

## 4. NIO 核心组件

### 4.1 三大核心组件

```
NIO 核心三角：
  ┌─────────────┐
  │  Channel    │ ← 连接通道（读/写）
  └──────┬──────┘
         │
  ┌──────▼──────┐     ┌─────────────┐
  │   Buffer    │◀───▶│   Selector  │
  │ 数据容器    │     │ 多路复用器  │
  └─────────────┘     └─────────────┘
```

| 组件 | 类比 BIO | 说明 |
|------|---------|------|
| Channel | Stream | 双向（可读可写），非阻塞 |
| Buffer | byte[] | 可读可写，有 position/limit/capacity |
| Selector | — | 监控多个 Channel 的 IO 事件 |

### 4.2 NIO vs BIO 对比

```java
/**
 * BIO 流程：
 * read(): 阻塞等待数据 → 数据完全到达 → 返回
 *
 * NIO 流程：
 * 1. Selector.select() — 阻塞等待就绪事件
 * 2. SelectionKey.isReadable() — 检查是否可读
 * 3. channel.read(buffer) — 读取数据到 Buffer（非阻塞，因为有数据）
 */
public class NIOvsBIO {
    // BIO: 面向流（Stream）
    InputStream in = socket.getInputStream();
    byte[] buf = new byte[1024];
    int len = in.read(buf); // 阻塞，直到有数据

    // NIO: 面向缓冲区（Buffer）
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int len = channel.read(buffer); // 非阻塞，立刻返回
    // 如果 len = 0，表示无数据；如果 len = -1，表示连接关闭
}
```

---

## 5. Buffer 详解

### 5.1 Buffer 状态变量

```
ByteBuffer 内部状态：

初始状态：
  ┌──────────────────────────────────────────┐
  │  数据区域                                  │
  │  [    ] [    ] [    ] ... [    ] [    ]  │
  │  ↑                                         │
  │  position = 0                              │
  │  limit = capacity                          │
  └──────────────────────────────────────────┘

写入 5 个字节后：
  ┌──────────────────────────────────────────┐
  │  [data] [data] [data] [data] [data] [  ] │
  │                       ↑                  │
  │                       position = 5       │
  │                       limit = capacity   │
  └──────────────────────────────────────────┘

flip() 后（读模式）：
  ┌──────────────────────────────────────────┐
  │  [data] [data] [data] [data] [data] [  ] │
  │  ↑                       ↑               │
  │  position = 0            limit = 5       │
  └──────────────────────────────────────────┘

读取 2 个字节后：
  ┌──────────────────────────────────────────┐
  │  [data] [data] [data] [data] [data] [  ] │
  │          ↑              ↑                │
  │          position = 2   limit = 5        │
  └──────────────────────────────────────────┘

clear() 后：
  ┌──────────────────────────────────────────┐
  │  [  ] [  ] [  ] [  ] [  ] [  ] [  ] [  ] │
  │  ↑                                        │
  │  position = 0                             │
  │  limit = capacity                         │
  └──────────────────────────────────────────┘
```

### 5.2 Buffer 核心 API

```java
/**
 * Buffer 常用操作
 */
public class BufferDemo {

    public static void main(String[] args) {
        // 1. 分配缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(1024);     // 堆内缓冲区
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024); // 直接缓冲区（堆外）

        // 2. 写入
        buffer.put((byte) 65);             // 'A'
        buffer.put("Hello".getBytes());    // 批量写入
        // 或通过 Channel 写入：
        // int bytesRead = channel.read(buffer);

        // 3. 切换读模式
        buffer.flip();
        // flip() = limit = position; position = 0; mark = -1

        // 4. 读取
        byte b = buffer.get();               // 读取一个字节
        byte[] dst = new byte[buffer.remaining()];
        buffer.get(dst);                     // 批量读取剩余数据

        // 5. 重新读取（重新标记）
        buffer.rewind();   // position = 0; mark = -1（保留 limit）

        // 6. 切换写模式
        buffer.clear();    // position = 0; limit = capacity
        // 或 compact(): 将未读数据移到开头
        buffer.compact();

        // ========== 其他操作 ==========
        ByteBuffer slice = buffer.slice();           // 切片（共享数据）
        ByteBuffer readOnly = buffer.asReadOnlyBuffer(); // 只读视图
        buffer.mark();    // 标记当前位置
        buffer.reset();   // 回到标记位置
    }
}
```

### 5.3 堆内 Buffer vs 直接 Buffer

| 特性 | HeapByteBuffer | DirectByteBuffer |
|------|---------------|-----------------|
| 内存位置 | JVM 堆 | OS 本地内存 |
| 分配/释放 | 快（GC 管理） | 慢（需显式释放） |
| IO 操作 | 需要中间复制 | 零中间复制 |
| 适用于 | 小数据、频繁分配 | 大数据、长生命周期 |
| 访问方式 | 数组访问 | get/put |

```
堆内 Buffer IO 流程：
  HeapBuffer → 内核缓冲区 → 磁盘/网卡
       ↕（需要复制到中间临时缓冲区）
  DirectBuffer → 内核缓冲区 → 磁盘/网卡
       ↕（零中间复制）
```

---

## 6. Channel 详解

### 6.1 主要 Channel 类型

```java
/**
 * NIO Channel 类型
 */
public class ChannelTypes {
    public static void main(String[] args) throws IOException {
        // 文件 IO
        FileChannel fileChannel = FileChannel.open(
                Path.of("test.txt"),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        );

        // 网络 IO
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false); // 设置为非阻塞

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(8080));

        // UDP
        DatagramChannel datagramChannel = DatagramChannel.open();
    }
}
```

### 6.2 文件 Channel 示例

```java
/**
 * FileChannel 示例
 */
public class FileChannelDemo {

    public static void main(String[] args) throws IOException {
        // 写入文件
        try (FileChannel channel = FileChannel.open(
                Path.of("output.txt"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        )) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put("Hello, NIO!\n".getBytes());
            buffer.flip();
            channel.write(buffer);
        }

        // 读取文件
        try (FileChannel channel = FileChannel.open(
                Path.of("output.txt"),
                StandardOpenOption.READ
        )) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            channel.read(buffer);
            buffer.flip();
            System.out.println(new String(buffer.array(), 0, buffer.remaining()));
        }

        // 零拷贝传输（高效文件复制）
        try (
            FileChannel src = FileChannel.open(Path.of("src.txt"), StandardOpenOption.READ);
            FileChannel dst = FileChannel.open(Path.of("dst.txt"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        ) {
            // 使用 DMA 直接在内核空间传输，不经过用户空间
            src.transferTo(0, src.size(), dst);
            // 或: dst.transferFrom(src, 0, src.size());
        }
    }
}
```

---

## 7. Selector 与多路复用

### 7.1 Selector 工作机制

```
单线程管理多个 Channel：

  Thread
    │
    ├─ Selector.select()    ← 阻塞，直到有事件
    │
    ├─ Selection Keys:
    │   ├─ Channel 1 (READY)
    │   ├─ Channel 2 (READY)
    │   └─ Channel 3 (READY)
    │
    └─ 处理每个 Channel：
        ├─ channel.read(buffer)
        ├─ channel.write(buffer)
        └─ ...
```

### 7.2 完整 NIO 服务器

```java
/**
 * 完整 NIO 服务器 — Reactor 模式的基础实现
 */
public class NioServer {

    public static void main(String[] args) throws IOException {
        // 1. 创建 Selector
        Selector selector = Selector.open();

        // 2. 创建 ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);          // 非阻塞模式
        serverChannel.bind(new InetSocketAddress(8080)); // 绑定端口

        // 3. 注册到 Selector（关注 ACCEPT 事件）
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("NIO Server started on 8080");

        // 4. 事件循环
        while (true) {
            // 阻塞等待事件（可设置超时）
            int readyCount = selector.select(1000); // 1s 超时

            if (readyCount == 0) {
                continue;
            }

            // 获取就绪的 SelectionKey 集合
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove(); // ⚠️ 必须手动移除

                try {
                    if (key.isAcceptable()) {
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        handleRead(key, selector);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
        }
    }

    // 处理新连接
    private static void handleAccept(SelectionKey key, Selector selector)
            throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        // 新连接注册到 Selector，关注 READ 事件
        clientChannel.register(selector, SelectionKey.OP_READ,
                ByteBuffer.allocate(1024)); // 关联 Buffer

        System.out.println("New client: " + clientChannel.getRemoteAddress());
    }

    // 处理可读事件
    private static void handleRead(SelectionKey key, Selector selector)
            throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment(); // 取出关联的 Buffer

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // 连接关闭
            System.out.println("Client disconnected");
            key.cancel();
            clientChannel.close();
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String message = new String(data);
            System.out.println("Received: " + message);

            // Echo 回客户端
            buffer.clear();
            buffer.put(("Echo: " + message).getBytes());
            buffer.flip();
            clientChannel.write(buffer);
            buffer.clear();
        }
    }

    // 处理可写事件
    private static void handleWrite(SelectionKey key) throws IOException {
        // 通常不使用 OP_WRITE（大多数时候可写）
        // 当 write() 无法写完全部数据时才注册
        SocketChannel clientChannel = (SocketChannel) key.channel();
        clientChannel.write((ByteBuffer) key.attachment());
        key.interestOps(SelectionKey.OP_READ); // 切回读
    }
}
```

### 7.3 SelectionKey 事件类型

```java
/**
 * 四种事件类型
 *
 * OP_ACCEPT    = 16 (1 << 4) — 服务器有新的连接
 * OP_CONNECT   = 8  (1 << 3) — 客户端连接成功
 * OP_READ      = 1  (1 << 0) — 有数据可读
 * OP_WRITE     = 4  (1 << 2) — 可以写入数据
 *
 * 注册时使用位掩码组合：
 * key.interestOps(SelectionKey.OP_ACCEPT | SelectionKey.OP_READ);
 *
 * 判断：
 * key.isAcceptable() = (key.readyOps() & OP_ACCEPT) != 0
 */
```

### 7.4 select vs poll vs epoll

| 特性 | select | poll | epoll (Linux 2.6+) |
|------|--------|------|-------------------|
| 底层结构 | FD_SET 位图 | pollfd 数组 | 事件表（红黑树 + 链表） |
| 最大 FD 数 | 1024 (FD_SETSIZE) | 无上限 | 无上限 |
| 遍历方式 | 线性扫描整个集合 | 线性扫描整个集合 | 只返回就绪的 FD |
| 效率 | O(n) | O(n) | O(1)（就绪事件数） |
| 操作 | 每次需重建 FD_SET | 每次需复制数组 | 只需添加/删除 FD |
| 触发方式 | LT | LT | LT + ET |

```java
/**
 * Java NIO Selector 在不同平台的底层实现：
 * Linux: EpollSelectorProvider (epoll)
 * macOS: KQueueSelectorProvider (kqueue)
 * Windows: WindowsSelectorProvider (select)
 *
 * JDK 7+ 默认启用 epoll（Linux）
 * JDK 11+ 默认启用 kqueue（macOS）
 */
```

---

## 8. Reactor 模式

### 8.1 三种 Reactor 变体

```java
/**
 * Reactor 模式：事件驱动的 IO 处理模式
 *
 * 三种变体：
 * 1. 单 Reactor 单线程
 * 2. 单 Reactor 多线程（Worker Thread Pool）
 * 3. 多 Reactor 多线程（Main Reactor + Sub Reactors）
 */

// ========== 变体 1: 单 Reactor 单线程 ==========
// Selector 在一个线程中处理所有 IO 事件
// 问题：处理慢连接时阻塞

// ========== 变体 2: 单 Reactor 多线程 ==========
public class MultiThreadReactor {
    // Reactor 线程：只负责 IO 事件分发
    // Worker 线程池：处理业务逻辑
    private static final ExecutorService WORKER_POOL =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public void processRequest(SocketChannel client, ByteBuffer buffer) {
        WORKER_POOL.submit(() -> {
            try {
                // 业务处理（不阻塞 Reactor 线程）
                Thread.sleep(100); // 模拟业务处理
                buffer.clear();
                buffer.put("Processed".getBytes());
                buffer.flip();
                client.write(buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

// ========== 变体 3: 多 Reactor 多线程 ==========
// 这是 Netty 采用的方式
public class MultiReactorServer {
    // Main Reactor: 处理 ACCEPT 事件
    private final Selector mainSelector = Selector.open();
    // Sub Reactors: 处理 READ/WRITE 事件
    private final Selector[] subSelectors = {
            Selector.open(), Selector.open()
    };
    private final ExecutorService reactorPool = Executors.newFixedThreadPool(2);

    public void start() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(8080));
        server.register(mainSelector, SelectionKey.OP_ACCEPT);

        // 启动 Sub Reactors
        for (Selector selector : subSelectors) {
            reactorPool.submit(new SubReactor(selector));
        }

        // Main Reactor 循环
        while (true) {
            mainSelector.select();
            for (SelectionKey key : mainSelector.selectedKeys()) {
                if (key.isAcceptable()) {
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    SocketChannel client = ssc.accept();
                    client.configureBlocking(false);

                    // 将新连接分配给 Sub Reactor（轮询）
                    Selector subSelector = subSelectors[
                            (int) (Math.random() * subSelectors.length)];
                    subSelector.wakeup();
                    client.register(subSelector, SelectionKey.OP_READ);
                }
            }
            mainSelector.selectedKeys().clear();
        }
    }

    static class SubReactor implements Runnable {
        private final Selector selector;
        SubReactor(Selector selector) { this.selector = selector; }

        @Override
        public void run() {
            try {
                while (true) {
                    selector.select();
                    // 处理 READ/WRITE...
                    selector.selectedKeys().clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

---

## 9. AIO (NIO.2)

```java
/**
 * AIO (JDK 7+): 异步非阻塞 IO
 *
 * 与 NIO 的区别：
 * NIO: 多路复用 + 同步非阻塞（读取还是自己同步读取）
 * AIO: 真正的异步（读取完成后回调通知）
 *
 * 两种使用方式：
 * 1. Future 模式
 * 2. CompletionHandler 回调
 */
public class AIODemo {

    // ========== AsynchronousServerSocketChannel ==========
    public static void main(String[] args) throws Exception {
        AsynchronousServerSocketChannel server =
                AsynchronousServerSocketChannel.open();
        server.bind(new InetSocketAddress(8082));

        System.out.println("AIO Server started on 8082");

        // 注册 ACCEPT 回调
        server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel client, Void attachment) {
                // 立即注册下一个 ACCEPT（形成链式调用）
                server.accept(null, this);

                // 处理连接
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                client.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        if (result > 0) {
                            attachment.flip();
                            byte[] data = new byte[attachment.remaining()];
                            attachment.get(data);
                            System.out.println("AIO Received: " + new String(data));

                            // Echo 回写
                            attachment.clear();
                            attachment.put(("Echo: " + new String(data)).getBytes());
                            attachment.flip();
                            client.write(attachment);
                        }
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        exc.printStackTrace();
                    }
                });
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        });

        // 主线程不退出（AIO 的线程池是守护线程）
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

| 特性 | NIO (Reactor) | AIO (Proactor) |
|------|--------------|----------------|
| 模式 | 同步非阻塞 | 异步非阻塞 |
| 读取 | 自己 read() | 回调通知 |
| 实现 | select/epoll | IOCP (Windows) / 线程池 (Linux) |
| 性能 | 更高（事件驱动） | 略低（回调开销） |
| 适用 | 高并发网络服务 | 文件 IO, 低并发高延迟 |
| 生态 | Netty, Tomcat | 较少使用 |

---

## 10. 零拷贝技术

### 10.1 传统 IO 的数据流

```
传统文件传输（从文件读取并发送到网络）：

1. 硬盘 ──DMA──▶ 内核缓冲区 (read buffer)
2. 内核缓冲区 ──CPU──▶ 用户缓冲区 (application buffer)
3. 用户缓冲区 ──CPU──▶ 套接字缓冲区 (socket buffer)
4. 套接字缓冲区 ──DMA──▶ 网卡

总共：2 次 DMA 拷贝 + 2 次 CPU 拷贝
      4 次上下文切换（用户态/内核态切换）
```

### 10.2 零拷贝技术

```
mmap + write:
  1. 硬盘 ──DMA──▶ 内核缓冲区
  2. 内核缓冲区 ──CPU──▶ 套接字缓冲区 (共享内存，减少一次拷贝)
  3. 套接字缓冲区 ──DMA──▶ 网卡
  总共：2 次 DMA 拷贝 + 1 次 CPU 拷贝

sendfile (Linux 2.6+):
  1. 硬盘 ──DMA──▶ 内核缓冲区
  2. 内核缓冲区 ──DMA──▶ 网卡（直接 DMA 到网卡）
  总共：2 次 DMA 拷贝 + 0 次 CPU 拷贝
       适用于文件→网络的传输

Java NIO 零拷贝 API:
  FileChannel.transferTo() / transferFrom()
  // 底层使用 sendfile（Linux）或 TransmitFile（Windows）
```

```java
/**
 * 零拷贝示例
 */
public class ZeroCopyDemo {

    public static void main(String[] args) throws IOException {
        // 传统方式（BIO）
        traditionalCopy("input.txt", 1024 * 1024);

        // 零拷贝方式（NIO）
        zeroCopy("input.txt");
    }

    // 传统拷贝：文件 → 内存 → socket
    static void traditionalCopy(String filePath, int size) throws IOException {
        try (
            FileInputStream fis = new FileInputStream(filePath);
            FileOutputStream fos = new FileOutputStream("output_trad.txt")
        ) {
            byte[] buffer = new byte[size];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);   // 两次用户态-内核态切换
            }
        }
    }

    // 零拷贝
    static void zeroCopy(String filePath) throws IOException {
        try (
            FileChannel source = FileChannel.open(Path.of(filePath), StandardOpenOption.READ);
            FileChannel destination = FileChannel.open(
                    Path.of("output_zerocopy.txt"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        ) {
            // 零拷贝：在内核空间直接传输
            // 支持 sendfile 的操作系统
            long transferred = source.transferTo(0, source.size(), destination);
            System.out.println("Zero-copy transferred " + transferred + " bytes");
        }
    }

    // 网络场景：文件 → 网卡
    static void fileToNetwork(FileChannel fileChannel, SocketChannel socketChannel)
            throws IOException {
        // 零拷贝：文件 → 网卡（不经过用户空间）
        // 适用场景：HTTP 静态文件服务器
        long position = 0;
        long count = fileChannel.size();
        fileChannel.transferTo(position, count, socketChannel);
        // 等同于 Linux sendfile
    }
}
```

---

## 11. Netty 入门

### 11.1 为什么需要 Netty？

```java
/**
 * 为什么选择 Netty 而非原生 NIO？
 *
 * 1. NIO API 太复杂（需要自己处理很多边缘情况）
 * 2. NIO 的 bug（特别是 epoll bug，Selector 空轮询）
 * 3. Netty 提供了：
 *    ─ 统一的 API（支持 NIO, Epoll, KQueue, IO Uring）
 *    ─ 编解码框架（粘包/拆包处理）
 *    ─ 线程模型（EventLoop）
 *    ─ 零拷贝（CompositeByteBuf）
 *    ─ 大量的内置 Handler
 */
```

### 11.2 Netty 核心组件

```
Netty 架构：

  ┌─────────────────────────────────────────┐
  │  Channel                                 │
  │  ┌─────────────────────────────────────┐ │
  │  │  ChannelPipeline                    │ │
  │  │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐  │ │
  │  │  │H1(IN)│ │H2(IN)│ │H3(OUT)│H4(OUT)│ │ │
  │  │  └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘  │ │
  │  │     │        │       │       │      │ │
  │  │     └────────┘───────┴───────┘      │ │
  │  └─────────────────────────────────────┘ │
  └─────────────────────────────────────────┘
                    │
  ┌─────────────────▼───────────────────────┐
  │  EventLoopGroup                          │
  │  ├─ Boss EventLoop (ACCEPT)             │
  │  └─ Worker EventLoops (READ/WRITE)      │
  └─────────────────────────────────────────┘
```

### 11.3 简易 Netty 服务端

```java
/**
 * Netty 服务端示例
 * 依赖: io.netty:netty-all
 */
public class NettyServer {

    public static void main(String[] args) throws Exception {
        // Boss Group: 处理 ACCEPT 事件（通常 1 个线程）
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // Worker Group: 处理 READ/WRITE 事件（默认 CPU*2 线程）
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .option(ChannelOption.SO_BACKLOG, 128)
                     .childOption(ChannelOption.SO_KEEPALIVE, true)
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             ChannelPipeline pipeline = ch.pipeline();
                             // 编码器/解码器
                             pipeline.addLast(new StringDecoder());
                             pipeline.addLast(new StringEncoder());
                             // 业务处理器
                             pipeline.addLast(new ServerHandler());
                         }
                     });

            // 绑定端口并启动
            ChannelFuture future = bootstrap.bind(8080).sync();
            System.out.println("Netty Server started on 8080");
            future.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 业务处理器
    static class ServerHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            System.out.println("Received: " + msg);
            ctx.writeAndFlush("Echo: " + msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
```

### 11.4 ByteBuf vs ByteBuffer

| 特性 | ByteBuffer | ByteBuf (Netty) |
|------|-----------|----------------|
| 索引 | position/limit/capacity | readerIndex/writerIndex |
| 扩容 | 不支持（抛异常） | 自动扩容 |
| 池化 | 不支持 | PooledByteBufAllocator |
| 零拷贝 | 不支持 | CompositeByteBuf |
| 引用计数 | 不支持 | RefCnt（自动释放） |

```java
/**
 * ByteBuf 优势
 */
public class ByteBufAdvantage {
    public void demo() {
        // 1. 双指针（readerIndex 和 writerIndex 独立）
        //    [已读区域 | 未读区域 | 可写区域]
        //    ↑readIdx    ↑writeIdx  ↑capacity

        // 2. 自动扩容
        ByteBuf buf = Unpooled.buffer(4);
        buf.writeBytes(new byte[]{1, 2, 3, 4, 5}); // 自动扩容，不会抛异常！

        // 3. 零拷贝 — 组合多个 Buffer
        ByteBuf header = Unpooled.wrappedBuffer("HEAD".getBytes());
        ByteBuf body = Unpooled.wrappedBuffer("BODY".getBytes());
        ByteBuf packet = Unpooled.wrappedBuffer(header, body); // 不复制数据
    }
}
```

---

## 12. TCP 网络基础

### 12.1 TCP 三次握手

```
Client                          Server
  │                               │
  │───── SYN (seq=x) ────────────▶│  1. Client 发送 SYN
  │                               │
  │◀──── SYN+ACK (seq=y,ack=x+1)─│  2. Server 回复 SYN+ACK
  │                               │
  │───── ACK (seq=x+1,ack=y+1)──▶│  3. Client 发送 ACK
  │                               │
  │         (连接已建立)           │
```

### 12.2 TCP 四次挥手

```
主动关闭方                   被动关闭方
  │                            │
  │───── FIN ────────────────▶│  1. 主动方发 FIN（不能再发数据）
  │                            │
  │◀──── ACK ─────────────────│  2. 被动方回 ACK
  │                            │
  │◀──── FIN ─────────────────│  3. 被动方发 FIN（不能发数据了）
  │                            │
  │───── ACK ────────────────▶│  4. 主动方回 ACK
  │                            │
  │     (等待 2MSL 后关闭)     │
```

### 12.3 TIME_WAIT 与 CLOSE_WAIT

| 状态 | 含义 | 问题 |
|------|------|------|
| TIME_WAIT | 主动关闭方发送 FIN 后等待 2MSL | 过多时端口耗尽 |
| CLOSE_WAIT | 被动关闭方等待关闭连接 | 过多时连接泄漏 |
| FIN_WAIT2 | 主动关闭方收到 ACK 但未收到 FIN | 半关闭状态 |

### 12.4 粘包与拆包

```
TCP 是流式协议，没有消息边界

消息边界问题：
  发送方：  [Msg1][Msg2][Msg3]
  接收方可能收到：
    [Msg1] [Msg2] [Msg3]    ← 正常
    [Msg1Msg2] [Msg3]       ← 粘包（Msg1+Msg2 合并）
    [Msg1] [Msg2] [Msg3...] ← 拆包（Msg3 不完整）

解决方案（三种常见协议）：
  1. 固定长度：每条消息固定大小（效率低）
  2. 分隔符：如 \n （HTTP Header 用 \r\n 分隔）
  3. 长度前缀：消息头包含消息体长度（最常用）
```

```java
/**
 * 粘包处理：长度前缀法（LTV — Length-Type-Value）
 */
public class StickyPacketHandler {

    // 编码器：写入长度前缀
    public static ByteBuffer encode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
        buffer.putInt(data.length);   // 4 字节长度前缀
        buffer.put(data);              // 数据体
        buffer.flip();
        return buffer;
    }

    // 解码器：基于长度前缀读取完整消息
    public static class Decoder {
        private byte[] buffer = new byte[1024 * 64];
        private int offset = 0;

        public List<byte[]> decode(byte[] newData) {
            List<byte[]> messages = new ArrayList<>();

            // 追加到缓冲区
            System.arraycopy(newData, 0, buffer, offset, newData.length);
            offset += newData.length;

            // 尝试解析出完整的消息
            while (offset >= 4) { // 至少有长度前缀
                int length = bytesToInt(buffer, 0);
                if (offset >= 4 + length) {
                    // 有完整的消息
                    byte[] msg = new byte[length];
                    System.arraycopy(buffer, 4, msg, 0, length);
                    messages.add(msg);

                    // 移除已处理的数据
                    int remaining = offset - (4 + length);
                    System.arraycopy(buffer, 4 + length, buffer, 0, remaining);
                    offset = remaining;
                } else {
                    break; // 消息不完整，等待更多数据
                }
            }

            return messages;
        }

        private int bytesToInt(byte[] bytes, int offset) {
            return ((bytes[offset] & 0xFF) << 24)
                 | ((bytes[offset+1] & 0xFF) << 16)
                 | ((bytes[offset+2] & 0xFF) << 8)
                 | (bytes[offset+3] & 0xFF);
        }
    }
}
```

### 12.5 心跳机制

```java
/**
 * 心跳机制：检测连接是否存活
 *
 * 原理：定期发送心跳包，如果 N 次没有收到响应，判定断开
 */
public class HeartbeatDemo {

    public static class HeartbeatClient {
        private SocketChannel channel;
        private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private int missedHeartbeats;
        private static final int MAX_MISSED = 3;

        public void start() throws IOException {
            channel = SocketChannel.open(new InetSocketAddress("localhost", 8080));

            // 每 5 秒发送一次心跳
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(1);
                    buffer.put((byte) 0xFF); // 心跳标记
                    buffer.flip();
                    channel.write(buffer);
                    missedHeartbeats = 0;
                } catch (IOException e) {
                    missedHeartbeats++;
                    if (missedHeartbeats >= MAX_MISSED) {
                        System.out.println("Server unreachable, reconnecting...");
                        // 触发重连
                        reconnect();
                    }
                }
            }, 0, 5, TimeUnit.SECONDS);
        }

        private void reconnect() {
            // 重连逻辑
        }
    }

    public static class HeartbeatServer {
        private static final long TIMEOUT = 15000; // 15s 超时

        public void handleHeartbeat(SocketChannel channel, ByteBuffer buffer) {
            // Netty 的 IdleStateHandler 可以自动处理
            // 这里只是示意
            if (buffer.get(0) == (byte) 0xFF) {
                // 收到心跳，更新最后活跃时间
                System.out.println("Heartbeat from " + channel.getRemoteAddress());
            }
        }
    }
}
```

---

## 13. 实战：NIO HTTP 服务器

```java
/**
 * 基于 NIO 的简易 HTTP 服务器
 *
 * 功能：处理 GET 请求，返回静态文件或 404
 * 注意：生产环境请使用 Netty / Tomcat / Undertow
 */
public class SimpleHttpServer {

    private static final String ROOT = ".";
    private static final byte[] CRLF = "\r\n".getBytes();

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(8080));
        server.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("HTTP Server on http://localhost:8080");

        while (true) {
            selector.select();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                try {
                    if (key.isAcceptable()) {
                        accept(key, selector);
                    } else if (key.isReadable()) {
                        handleRequest(key);
                    }
                } catch (IOException e) {
                    key.cancel();
                    key.channel().close();
                }
            }
        }
    }

    private static void accept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel client = ssc.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ,
                ByteBuffer.allocate(8192));
    }

    private static void handleRequest(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            client.close();
            return;
        }

        buffer.flip();
        String request = new String(buffer.array(), 0, buffer.remaining());
        buffer.clear();

        // 解析 HTTP 请求行
        String[] lines = request.split("\r\n");
        if (lines.length == 0) return;

        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 2) return;

        String method = requestLine[0];
        String path = requestLine[1];

        if (!"GET".equalsIgnoreCase(method)) {
            sendResponse(client, "405 Method Not Allowed",
                    "Only GET supported".getBytes());
            return;
        }

        // 处理路径
        if (path.equals("/")) path = "/index.html";
        String filePath = ROOT + path;

        try {
            byte[] content = Files.readAllBytes(Path.of(filePath));
            sendResponse(client, "200 OK", content);
        } catch (IOException e) {
            String errorBody = "<h1>404 Not Found</h1>";
            sendResponse(client, "404 Not Found", errorBody.getBytes());
        }

        client.close();
    }

    private static void sendResponse(SocketChannel client,
                                      String status, byte[] body) throws IOException {
        ByteBuffer response = ByteBuffer.allocate(1024 * 64);
        String header = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        response.put(header.getBytes());
        response.put(body);
        response.flip();
        client.write(response);
    }
}
// 测试：curl http://localhost:8080/
```

---

## 14. 面试经典问题

### 基础问题

**Q1: BIO、NIO、AIO 的区别？**
- BIO：同步阻塞，一连接一线程
- NIO：同步非阻塞，多路复用
- AIO：异步非阻塞，回调通知

**Q2: NIO 的三大核心组件？**
- Channel：双向通道
- Buffer：数据缓冲区
- Selector：多路复用器

**Q3: select、poll、epoll 的区别？**

**Q4: 什么是零拷贝？Java 中如何实现？**
- 数据在内核空间直接传输，不经过用户空间
- FileChannel.transferTo() / transferFrom()

### 进阶问题

**Q5: Reactor 和 Proactor 模式的区别？**
- Reactor：事件通知后自己读写（NIO）
- Proactor：事件通知时数据已就绪（AIO）

**Q6: Netty 的线程模型是怎样的？**
- Boss Group (ACCEPT)
- Worker Group (READ/WRITE)
- EventLoop 绑定到线程，Channel 绑定到 EventLoop

**Q7: Netty 的零拷贝体现在哪些方面？**
- CompositeByteBuf（不复制组合）
- FileRegion（transferTo）
- 池化 ByteBuf（重用缓冲区）

**Q8: 粘包/拆包的原因和解决方案？**
- 原因：TCP 是流协议
- 解决：固定长度、分隔符、长度前缀

**Q9: TIME_WAIT 过多怎么处理？**
- 开启 TCP 时间戳：net.ipv4.tcp_tw_reuse
- 调整端口范围：net.ipv4.ip_local_port_range
- 开启 SO_REUSEADDR

**Q10: 如果让你设计一个高性能网络框架，需要考虑哪些点？**
- IO 模型选择（epoll/kqueue）
- 线程模型设计（Reactor）
- 内存管理（池化、零拷贝）
- 协议处理（粘包、编解码）
- 连接管理（心跳、重连）

---

## 参考资源

- [Java NIO 官方教程](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/package-summary.html)
- [Netty 官方文档](https://netty.io/wiki/user-guide.html)
- [Unix Network Programming (Stevens)](https://book.douban.com/subject/1500149/)
- [Reactor Pattern](https://www.dre.vanderbilt.edu/~schmidt/PDF/Reactor3.pdf)
- [Linux man: epoll, select, sendfile](https://man7.org/linux/man-pages/man7/epoll.7.html)

---

*最后更新: 2026-05-31 | 适用于 JDK 8/11/17/21*
