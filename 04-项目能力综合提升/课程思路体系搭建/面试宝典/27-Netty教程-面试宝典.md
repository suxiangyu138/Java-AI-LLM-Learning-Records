# Netty 面试宝典
> 基于课程大纲全面覆盖面试高频考点，从 Java NIO 基础到 Netty 核心 API、粘包/半包解决、协议设计、性能优化及源码分析。

## 目录
1. [一、基础概念速答（20题）](#一基础概念速答20题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码/配置文件题（6题）](#四手写代码配置文件题6题)
5. [五、系统设计题（4题）](#五系统设计题4题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺 CheckList](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（20题）

> 以下问题考察 Java NIO / Netty 基础知识，通常出现在面试一面的前 15 分钟。

### Q1：BIO、NIO、AIO 的区别是什么？

| 模型 | 英文 | IO 模式 | 线程模型 | 适用场景 |
|------|------|---------|---------|---------|
| **BIO** | Blocking IO | 同步阻塞 | 一个连接一个线程 | 连接数少、固定架构 |
| **NIO** | Non-blocking IO / New IO | 同步非阻塞/多路复用 | 一个线程管理多个连接 | 连接数多、短连接 |
| **AIO** | Asynchronous IO | 异步非阻塞 | 回调/CompletionHandler | 连接数巨大、长连接 |

```
BIO 模型：
Thread1───Client1 (阻塞 read)
Thread2───Client2 (阻塞 read)
Thread3───Client3 (阻塞 read)
问题：线程数 = 连接数，连接数大时 OOM

NIO 多路复用模型：
SelectorThread───Client1
               ├──Client2
               ├──Client3
               └──ClientN (一个线程管理 N 个连接)

AIO 模型：
Thread───Client1 (注册回调，OS 完成后通知)
       └──Client2 (异步不阻塞)
```

> 💡 Netty 本质上是 NIO 的封装，但它的 I/O 模型实际上是 **Reactor 模式**（多路复用）。

---

### Q2：Java NIO 三大核心组件是什么？

| 组件 | 说明 | 类比 |
|------|------|------|
| **Channel（通道）** | 双向数据传输的通道 | 类似铁路轨道 |
| **Buffer（缓冲区）** | 数据读写的容器 | 类似火车车厢 |
| **Selector（选择器）** | 监听多个 Channel 的事件 | 类似火车站调度员 |

```java
// NIO 核心代码骨架
// 1. 打开 ServerSocketChannel
ServerSocketChannel ssc = ServerSocketChannel.open();
ssc.bind(new InetSocketAddress(8080));
ssc.configureBlocking(false);  // 非阻塞模式

// 2. 打开 Selector
Selector selector = Selector.open();

// 3. 注册 Channel 到 Selector
ssc.register(selector, SelectionKey.OP_ACCEPT);

// 4. 事件循环
while (true) {
    int readyChannels = selector.select();  // 阻塞等待事件
    Set<SelectionKey> keys = selector.selectedKeys();
    Iterator<SelectionKey> iter = keys.iterator();

    while (iter.hasNext()) {
        SelectionKey key = iter.next();
        if (key.isAcceptable()) {
            // 处理连接
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        } else if (key.isReadable()) {
            // 处理读事件
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            int read = client.read(buffer);
            // ... 处理数据
        }
        iter.remove();  // 必须移除已处理的 key
    }
}
```

---

### Q3：BIO 和 NIO 的线程模型对比？

| 模型 | 线程与连接关系 | 线程数 | 上下文切换 | 适用场景 |
|------|--------------|--------|-----------|---------|
| **BIO** | 1 线程 : 1 连接 | 与连接数正比 | 多 | 连接数少 (< 1000) |
| **NIO** | 1 线程 : N 连接 | 固定（CPU 核数）| 少 | 连接数多 (1000~百万) |
| **Netty** | Boss 线程 + Worker 线程池 | CPU 核数 × 2 | 优化 | 所有高性能场景 |

```java
// BIO 服务端（传统）
// 问题：每个连接需要一个独立线程
ExecutorService threadPool = Executors.newFixedThreadPool(100);
ServerSocket server = new ServerSocket(8080);
while (true) {
    Socket socket = server.accept();  // 阻塞
    threadPool.execute(() -> {
        InputStream in = socket.getInputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {  // 阻塞
            // 处理请求
        }
    });
}
```

> ⚠️ BIO 在连接数 > 1000 时性能急剧下降，因为大量线程导致频繁上下文切换和内存溢出。

---

### Q4：Netty 的整体架构是怎样的？

```
┌─────────────────────────────────────────────┐
│              Netty Architecture              │
├─────────────────────────────────────────────┤
│  Transport Layer          │  Transport Layer │
│  (NIO/Epoll/OIO/KQueue)  │  (NIO/Epoll...)  │
├───────────────────────────┴─────────────────┤
│  Protocol Support            HTTP/WebSocket/ │
│  (Codec Framework)          Protobuf/Redis   │
├─────────────────────────────────────────────┤
│  Core Layer                                  │
│  Extensible Event Model / ChannelPipeline    │
│  ByteBuf / Channel / ChannelHandler          │
├─────────────────────────────────────────────┤
│  Event Loop / Reactor Thread Model           │
│  BossGroup (Accept) / WorkerGroup (IO)      │
└─────────────────────────────────────────────┘
```

**Netty 核心组件：**
- **BossGroup**：接收连接（Acceptor），对应 Reactor 模型中的 Main Reactor
- **WorkerGroup**：处理 IO 读写，对应 Sub Reactor
- **EventLoop**：事件循环线程，每个 EventLoop 对应一个 Selector
- **ChannelPipeline**：责任链模式管理 Handler 链
- **ByteBuf**：Netty 自己实现的字节缓冲区（功能远强于 JDK ByteBuffer）
- **ChannelHandler**：业务逻辑处理接口

---

### Q5：Netty 的 Reactor 线程模型是怎样的？

**三种 Reactor 模型：**

| 模型 | 结构 | 适用场景 |
|------|------|---------|
| **单 Reactor 单线程** | 1 个 Reactor 处理所有事件 | 小量连接 |
| **单 Reactor 多线程** | 1 个 Reactor + Worker 线程池 | 中等连接 |
| **主从 Reactor 多线程** | Main Reactor（Accept）+ Sub Reactor（IO）| 大量连接（Netty 默认）|

```
Netty 主从 Reactor 模型：

BossGroup (1~N 个 EventLoop)
  │
  ├─ EventLoop1 (Selector) ─── accept ─→ NioSocketChannel
  ├─ EventLoop2 (Selector) ─── accept ─→ NioSocketChannel
  │
  └─ 将 SocketChannel 注册到 WorkerGroup

WorkerGroup (N 个 EventLoop，默认 CPU*2)
  │
  ├─ EventLoop1 (Selector) ─── read/write (管理多个连接)
  ├─ EventLoop2 (Selector) ─── read/write (管理多个连接)
  └─ EventLoop3 (Selector) ─── read/write (管理多个连接)
```

```java
// Netty 默认配置
EventLoopGroup bossGroup = new NioEventLoopGroup(1);      // Boss 线程数 = 1
EventLoopGroup workerGroup = new NioEventLoopGroup();      // 默认 = CPU 核数 × 2

ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
 .channel(NioServerSocketChannel.class)
 .childHandler(new ChannelInitializer<SocketChannel>() {
     @Override
     protected void initChannel(SocketChannel ch) {
         ch.pipeline().addLast(new MyServerHandler());
     }
 });
```

---

### Q6：EventLoop 是什么？它的生命周期是怎样的？

**EventLoop** 是 Netty 的**事件循环线程**，本质是一个**永远不停止的线程**。

```
EventLoop 生命周期：

┌─────────────────────────────────────┐
│          EventLoop (线程)            │
│                                      │
│  while (true) {                       │
│    1. selector.select(timeout)       │ ← 等待事件
│    2. processSelectedKeys()          │ ← 处理 IO 事件
│    3. runAllTasks(timeout)           │ ← 处理非 IO 任务
│  }                                    │
│                                      │
│  EventLoop 职责：                      │
│  1. 处理注册到该 Selector 的 IO 事件   │
│  2. 执行定时任务（schedule）           │
│  3. 执行普通任务（execute/submit）     │
└─────────────────────────────────────┘
```

```java
// EventLoop 也是线程池的一部分
EventLoopGroup group = new NioEventLoopGroup(4);

// EventLoop 可以执行普通任务
group.next().submit(() -> {
    System.out.println("普通任务在 EventLoop 中执行");
});

// EventLoop 可以执行定时任务
group.next().schedule(() -> {
    System.out.println("5 秒后执行");
}, 5, TimeUnit.SECONDS);

// EventLoop 可以执行周期性任务
group.next().scheduleAtFixedRate(() -> {
    System.out.println("每 1 秒执行一次");
}, 0, 1, TimeUnit.SECONDS);
```

> 💡 **核心约束**：一个 Channel 的生命周期内只绑定到一个固定的 EventLoop，**一个 EventLoop 可以服务于多个 Channel**。这种设计避免了线程安全问题。

---

### Q7：ChannelPipeline 的工作原理？

ChannelPipeline 采用 **责任链模式（Chain of Responsibility）**，IO 事件按顺序经过注册的 Handler。

```
                    I/O Request
                        │
                   ┌────┴────┐
        ──────────→│ Inbound │──────────→
                   │ Handler1│
                   └────┬────┘
                        │
                   ┌────┴────┐
        ──────────→│ Inbound │──────────→
                   │ Handler2│
                   └────┬────┘
                        │
    Socket ←─────────┐  │
      Read    ┌──────┴──┴──────┐
      Write ←─│  Tail Handler  │
              └──────┬──┬──────┘
                     │  │
            ┌────────┘  └────────┐
            ▼                    ▼
      ┌──────────┐        ┌──────────┐
      │ Outbound │        │ Outbound │
      │ Handler2 │        │ Handler1 │
      └──────────┘        └──────────┘
```

```java
// Pipeline 中的 Handler 注册
ChannelPipeline p = ch.pipeline();
p.addLast("decoder", new MyDecoder());           // Inbound
p.addLast("encoder", new MyEncoder());           // Outbound
p.addLast("handler", new MyBusinessHandler());   // Inbound

// Handler 编写示例
public class MyInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("收到消息: " + msg);
        ctx.fireChannelRead(msg);  // 传给下一个 Inbound Handler
    }
}

public class MyOutboundHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        System.out.println("写出消息: " + msg);
        ctx.write(msg, promise);   // 传给下一个 Outbound Handler
    }
}
```

> 💡 Inbound 事件从 Head → Tail 传播（入站），Outbound 事件从 Tail → Head 传播（出站）。

---

### Q8：Channel 的状态有哪些？

```
Channel 生命周期状态迁移：

ChannelUnregistered (未注册)
       │
       ▼
ChannelRegistered (已注册到 EventLoop)
       │
       ▼
ChannelActive (已连接/已绑定)
       │
       ▼
ChannelInactive (已断开)
       │
       ▼
ChannelUnregistered (取消注册)
```

```java
// 监听生命周期事件
public class LifeCycleHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        System.out.println("Handler 被添加到 Pipeline");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        System.out.println("Channel 注册到 EventLoop");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Channel 已激活（TCP 连接建立）");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("收到数据");
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Channel 已断开");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        System.out.println("Channel 取消注册");
    }
}
```

---

### Q9：ChannelHandlerContext 的作用是什么？

ChannelHandlerContext 表示 **Pipeline 中一个 Handler 的上下文**，持有 Handler 的前驱和后继节点的引用。

```java
// ChannelHandlerContext 核心方法
public interface ChannelHandlerContext extends ... {
    Channel channel();                    // 获取关联的 Channel
    EventExecutor executor();             // 获取 EventExecutor
    String name();                        // Handler 名称
    ChannelHandler handler();            // 获取 Handler 实例

    // 事件传播
    ChannelHandlerContext fireChannelRead(Object msg);   // 向后传播
    ChannelHandlerContext write(Object msg);             // 向前传播
    ChannelHandlerContext flush();

    // I/O 操作
    ChannelFuture writeAndFlush(Object msg);
    ChannelFuture close();
}
```

> 💡 **重要特性**：从哪个 Context 开始传播事件，就从哪个位置开始。例如 `ctx.write()` 是从当前 Handler 的**前一节点**（出站方向）开始传播，而 `ctx.channel().write()` 是从 **Tail** 开始传播。

---

### Q10：ByteBuf 和 JDK ByteBuffer 的区别？

| 特性 | JDK ByteBuffer | Netty ByteBuf |
|------|---------------|---------------|
| 读写指针 | 1 个 position 指针 | 2 个指针（readerIndex + writerIndex）|
| 扩容 | 手动扩容 | 自动扩容 |
| 池化 | 不支持 | 支持（PooledByteBufAllocator）|
| 零拷贝 | 不支持 | 支持（slice / composite / wrap）|
| 引用计数 | 不支持 | 支持（ReferenceCounted）|
| 内存类型 | 堆内存（Heap） | 堆内存（Heap） + 直接内存（Direct）|

```java
// ByteBuf 结构
//  +-------------------+------------------+------------------+
//  | discardable bytes |  readable bytes  |  writable bytes  |
//  +-------------------+------------------+------------------+
//  |                   |                  |                  |
//  0      <=      readerIndex     <=    writerIndex    <=   capacity

// 基本使用
ByteBuf buf = Unpooled.buffer(10);
System.out.println(buf);  // readerIndex: 0, writerIndex: 0, capacity: 10
buf.writeBytes("Hello".getBytes());
System.out.println(buf);  // readerIndex: 0, writerIndex: 5
byte b = buf.readByte();  // 读一个字节
System.out.println(buf);  // readerIndex: 1, writerIndex: 5
```

---

### Q11：Heap ByteBuf 和 Direct ByteBuf 的区别？

| 类型 | 名称 | 内存位置 | 优点 | 缺点 |
|------|------|---------|------|------|
| **Heap** | 堆缓冲区 | JVM 堆 | 分配快、GC 管理 | Socket IO 需要拷贝到 Direct |
| **Direct** | 直接缓冲区 | 堆外内存（OS）| Socket IO 零拷贝 | 分配慢、需手动释放 |

```java
// Heap ByteBuf
ByteBuf heapBuf = Unpooled.buffer();
// 或
ByteBuf heapBuf2 = UnpooledByteBufAllocator.DEFAULT.heapBuffer();

// Direct ByteBuf
ByteBuf directBuf = Unpooled.directBuffer();
// 或
ByteBuf directBuf2 = UnpooledByteBufAllocator.DEFAULT.directBuffer();
// 或
ByteBuf directBuf3 = PooledByteBufAllocator.DEFAULT.directBuffer();

// 判断类型
if (buf.hasArray()) {
    byte[] array = buf.array();  // Heap Buffer 可以直接取数组
    int offset = buf.arrayOffset() + buf.readerIndex();
    int length = buf.readableBytes();
} else {
    // Direct Buffer，需要读入自己的数组
    byte[] array = new byte[buf.readableBytes()];
    buf.getBytes(buf.readerIndex(), array);
}
```

> ⚠️ 网络 IO 操作推荐使用 **Direct ByteBuf**，减少数据从堆到堆外的一次拷贝。但 Direct Buffer 分配和释放成本高，推荐使用 **池化**（`PooledByteBufAllocator`）。

---

### Q12：Pooled 和 Unpooled ByteBuf 的区别？

| 特性 | PooledByteBuf | UnpooledByteBuf |
|------|--------------|----------------|
| 内存分配 | 从对象池/内存池获取 | 每次都新建 |
| 分配速度 | 快（复用） | 慢（GC 压力）|
| 使用场景 | 高并发生产环境 | 低并发、测试环境 |
| 默认 | Netty 4.x 默认池化 | 需手动指定 |

```java
// 默认分配器
ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
// Netty 4.1+ 默认 = PooledByteBufAllocator

// 切换为非池化
// 启动参数：-Dio.netty.allocator.type=unpooled
// 或代码指定
// System.setProperty("io.netty.allocator.type", "unpooled");

// 池化分配器参数
// io.netty.allocator.numHeapArenas    # 堆内存区数量（默认 CPU*2）
// io.netty.allocator.numDirectArenas  # 直接内存区数量（默认 CPU*2）
// io.netty.allocator.pageSize         # 页面大小（默认 8192）
// io.netty.allocator.maxOrder         # 最大分配大小（默认 11 → 16MB）
```

> 💡 生产环境永远使用 **PooledByteBufAllocator**，性能相差可达 **3~5 倍**。

---

### Q13：ByteBuf 的零拷贝方法有哪些？

| 方法 | 说明 | 是否复制数据 |
|------|------|-------------|
| `slice()` | 切片，共享同一块内存 | 否（零拷贝）|
| `duplicate()` | 复制指针，共享内存 | 否（零拷贝）|
| `composite()` | 组合多个 ByteBuf | 否（零拷贝）|
| `Unpooled.wrappedBuffer()` | 包装字节数组 | 否（零拷贝）|
| `copy()` | 真正复制数据 | 是（深拷贝）|

```java
// slice() — 零拷贝切片
ByteBuf original = Unpooled.wrappedBuffer("Hello World".getBytes());
ByteBuf slice = original.slice(0, 5);  // "Hello"
// slice 和 original 共享同一块内存
slice.setByte(0, 'h');  // 修改 slice 也会影响 original

// composite() — 零拷贝组合
ByteBuf header = Unpooled.wrappedBuffer("Header:".getBytes());
ByteBuf body = Unpooled.wrappedBuffer("BodyData".getBytes());
CompositeByteBuf composite = Unpooled.compositeBuffer();
composite.addComponents(true, header, body);
// 不会复制数据，只是逻辑组合

// wrappedBuffer() — 包装现有数组
byte[] array = {1, 2, 3, 4, 5};
ByteBuf wrapped = Unpooled.wrappedBuffer(array);
// 直接操作原始数组，零拷贝

// copy() — 真正拷贝（非零拷贝）
ByteBuf copy = original.copy();  // 深拷贝，独立内存
```

---

### Q14：Netty 中如何管理 ByteBuf 的内存（retain/release）？

Netty 使用 **引用计数（Reference Counting）** 管理 ByteBuf 生命周期。

```java
// 引用计数规则
// 1. 每个 ByteBuf 初始 count = 1
// 2. retain() 增加引用计数
// 3. release() 减少引用计数
// 4. count == 0 时自动释放内存

ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
System.out.println(buf.refCnt());  // 1

buf.retain();                       // retain → count = 2
System.out.println(buf.refCnt());  // 2

buf.release();                      // release → count = 1
System.out.println(buf.refCnt());  // 1

buf.release();                      // release → count = 0，内存释放
// buf.writeByte(1);               // 报错：IllegalReferenceCountException
```

**谁负责 release？—— 谁最后使用谁释放：**

```
// 规则：
// ┌─ Handler 1 从 Channel 读到 msg ──→ retain 后传递
// ┌─ Handler 2 处理完，不再使用 ────→ release
// ┌─ TailHandler 会自动释放所有未释放的 msg

// 标准示例
public class MyHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            // 只读不传 → 必须 release
            ByteBuf buf = (ByteBuf) msg;
            // 处理数据...
        } finally {
            ReferenceCountUtil.release(msg);  // 释放
            // 或 ((ByteBuf) msg).release();
        }
    }
}

// 如果还要往下传，需要 retain
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    // 传给下一个 Handler（常用）
    ctx.fireChannelRead(msg);  // 下一个 Handler 负责 release
}
```

> 💡 **黄金法则**：谁 `getByteBuf` 谁 release，谁 `retain` 谁 release。不要重复 release，不要漏 release。

---

### Q15：Netty 的解码器（Codec）类层次结构？

```
ChannelInboundHandler
      │
      ├── ByteToMessageDecoder（基础解码器）
      │       └── ReplayingDecoder（无需检查可读字节）
      │       └── LineBasedFrameDecoder（行解码器）
      │       └── DelimiterBasedFrameDecoder（分隔符解码器）
      │       └── FixedLengthFrameDecoder（定长解码器）
      │       └── LengthFieldBasedFrameDecoder（长度域解码器）
      │
      ├── MessageToMessageDecoder（消息→消息解码）
      │
      └── MessageToByteEncoder（消息→字节编码）
```

```java
// 自定义解码器（继承 ByteToMessageDecoder）
public class IntegerDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 必须检查可读字节数
        if (in.readableBytes() >= 4) {
            out.add(in.readInt());  // 每读到 4 字节，解码为一个 int
        }
    }
}

// 自定义编码器（继承 MessageToByteEncoder）
public class IntegerEncoder extends MessageToByteEncoder<Integer> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Integer msg, ByteBuf out) {
        out.writeInt(msg);  // 将 int 写入 4 字节 ByteBuf
    }
}
```

---

### Q16：Netty 如何解决 TCP 粘包/半包问题？

**粘包（Sticky Packet）：** 发送方发的多个包被接收方一次读到。
**半包（Half Packet）：** 发送方的一个包被接收方拆成多次读到。

| 解决方案 | 解码器 | 原理 |
|---------|--------|------|
| **定长解码** | `FixedLengthFrameDecoder` | 固定每个包长度 |
| **行解码** | `LineBasedFrameDecoder` | 以 `\n` 或 `\r\n` 分割 |
| **分隔符解码** | `DelimiterBasedFrameDecoder` | 自定义分隔符 |
| **长度域解码** | `LengthFieldBasedFrameDecoder` | 头部长度 + 数据体（最常用）|

```java
// 1. 定长解码器
ch.pipeline().addLast(new FixedLengthFrameDecoder(10));
// 每个包固定 10 字节，不足等够了再处理

// 2. 行解码器
ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
// 每个包以 \n 或 \r\n 结尾，最大 1024 字节

// 3. 分隔符解码器
ByteBuf delimiter = Unpooled.copiedBuffer("$$".getBytes());
ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter));

// 4. 长度域解码器（最常用，如自定义协议）
// 协议格式：| length(4字节) | body(n字节) |
ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
    1024,      // maxFrameLength：最大包长
    0,         // lengthFieldOffset：长度域偏移
    4,         // lengthFieldLength：长度域字节数
    0,         // lengthAdjustment：长度调整
    4          // initialBytesToStrip：跳过的字节（去掉 length）
));
```

---

### Q17：Netty 的心跳检测和 IdleStateHandler 如何实现？

```java
// 空闲检测（服务端）
ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
 .channel(NioServerSocketChannel.class)
 .childHandler(new ChannelInitializer<SocketChannel>() {
     @Override
     protected void initChannel(SocketChannel ch) {
         ch.pipeline().addLast(
             new IdleStateHandler(  // 空闲检测处理器
                 60,   // readerIdleTime：读空闲超时（秒）
                 30,   // writerIdleTime：写空闲超时
                 0     // allIdleTime：全部空闲超时（0 = 不触发）
             ),
             new HeartbeatHandler()  // 自定义心跳处理器
         );
     }
 });

// 自定义心跳处理器
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case READER_IDLE:
                    System.out.println("读空闲，关闭连接");
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    System.out.println("写空闲，发送心跳");
                    ctx.writeAndFlush(Unpooled.wrappedBuffer("PING".getBytes()));
                    break;
            }
        }
    }
}

// 客户端心跳
Bootstrap b = new Bootstrap();
b.group(group)
 .channel(NioSocketChannel.class)
 .handler(new ChannelInitializer<SocketChannel>() {
     @Override
     protected void initChannel(SocketChannel ch) {
         ch.pipeline().addLast(
             new IdleStateHandler(0, 10, 0),  // 每 10s 写空闲触发
             new ClientHeartbeatHandler()
         );
     }
 });

public class ClientHeartbeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            // 发送心跳包
            ctx.writeAndFlush(new HeartbeatPacket());
        }
    }
}
```

---

### Q18：@Sharable 注解的作用是什么？

`@Sharable` 标记一个 Handler 实例可以被多个 ChannelPipeline **共享**。

```java
@ChannelHandler.Sharable  // 表明该 Handler 是无状态的，可以共享
public class ShareableHandler extends ChannelInboundHandlerAdapter {
    private final AtomicLong counter = new AtomicLong();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        long count = counter.incrementAndGet();
        System.out.println("处理消息数: " + count);
        ctx.fireChannelRead(msg);
    }
}

// 使用方式
ShareableHandler shared = new ShareableHandler();

// 多个 Channel 共享同一个 Handler 实例
b.childHandler(new ChannelInitializer<SocketChannel>() {
    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(shared);  // 同一个实例被重复添加
    }
});
```

> ⚠️ **注意**：只有**无状态**（不用成员变量保存连接相关数据）的 Handler 才能加 `@Sharable`。如果 Handler 包含连接特定的数据，不要共享。

---

### Q19：Netty 的 Future 和 Promise 是什么？

| 接口 | 说明 | 特点 |
|------|------|------|
| `io.netty.util.concurrent.Future` | 异步操作结果 | 可以添加 Listener（回调），可以同步 get |
| `io.netty.util.concurrent.Promise` | Future 的子接口 | 可手动设置结果（setSuccess / setFailure）|

```java
// Netty Future 使用
ChannelFuture future = ctx.writeAndFlush(msg);
future.addListener((ChannelFutureListener) f -> {
    if (f.isSuccess()) {
        System.out.println("发送成功");
    } else {
        System.out.println("发送失败: " + f.cause());
    }
});

// Promise 使用（通常在底层框架中使用）
public class MyPromiseExample {
    public static void main(String[] args) {
        EventLoopGroup group = new NioEventLoopGroup();
        Promise<String> promise = group.next().newPromise();

        // 在其他线程设置结果
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                promise.setSuccess("Hello from Promise");
            } catch (Exception e) {
                promise.setFailure(e);
            }
        }).start();

        // 获取结果（同步阻塞）
        try {
            String result = promise.get();  // 阻塞等待
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// 实际应用：RPC 调用中等待结果
public class RpcPromise {
    private final Promise<Object> promise;
    private final long requestId;

    public RpcPromise(Promise<Object> promise, long requestId) {
        this.promise = promise;
        this.requestId = requestId;
    }

    public void success(Object result) {
        promise.setSuccess(result);
    }

    public void fail(Throwable cause) {
        promise.setFailure(cause);
    }
}
```

> 💡 JDK 的 Future 需要自己 get() 阻塞获取结果，Netty 的 Future 支持**回调监听器**，不会阻塞调用线程。

---

### Q20：ChannelOption 有哪些常用配置？

| 选项 | 说明 | 默认值 | 建议 |
|------|------|--------|------|
| `SO_BACKLOG` | TCP 连接队列大小 | 128 | 根据并发调整（如 1024）|
| `TCP_NODELAY` | 禁用 Nagle 算法 | false | 低延迟场景设为 true |
| `SO_KEEPALIVE` | TCP 心跳探测 | false | 长连接设为 true |
| `SO_REUSEADDR` | 地址复用 | false | 快速重启时设为 true |
| `SO_SNDBUF` | 发送缓冲区大小 | OS 默认 | 大流量场景调大 |
| `SO_RCVBUF` | 接收缓冲区大小 | OS 默认 | 大流量场景调大 |
| `CONNECT_TIMEOUT_MILLIS` | 连接超时 | 30000 | 根据业务调整 |

```java
// 服务端配置
ServerBootstrap b = new ServerBootstrap();
b.option(ChannelOption.SO_BACKLOG, 1024)         // accept 队列大小
 .option(ChannelOption.SO_REUSEADDR, true)
 .childOption(ChannelOption.TCP_NODELAY, true)   // 禁用 Nagle
 .childOption(ChannelOption.SO_KEEPALIVE, true);

// 客户端配置
Bootstrap b = new Bootstrap();
b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 5s 连接超时
 .option(ChannelOption.TCP_NODELAY, true)
 .option(ChannelOption.SO_KEEPALIVE, true);
```

---

## 二、深度原理剖析（12题）

### Q21：BIO、NIO、AIO 的底层 IO 模型？

```
Linux IO 模型分类：

1. 阻塞 IO（Blocking IO）
   进程 → recvfrom → 内核等待数据 → 数据从内核复制到用户 → 返回
                ←—————— 全程阻塞 ——————→

2. 非阻塞 IO（Non-blocking IO）
   进程 → recvfrom → EAGAIN → recvfrom → EAGAIN → recvfrom → 有数据 → 复制 → 返回
                  ←———— 轮询，每次都返回 —————→

3. IO 多路复用（IO Multiplexing）
   进程 → select/epoll → 阻塞 → 有事件 → recvfrom → 复制 → 返回
              ↓ 可管理多个 fd
          select/epoll 可同时监视多个 fd

4. 异步 IO（AIO / POSIX AIO / io_uring）
   进程 → aio_read → 立即返回 → 内核完成数据复制 → 回调通知
                 ←—— 全程不阻塞 —————→
```

| 模型 | 对应 Java 实现 | 激活状态 |
|------|---------------|---------|
| 阻塞 IO | `BIO`（`java.net.ServerSocket`） | 阻塞 |
| IO 多路复用 | `NIO`（`java.nio.channels.Selector`） | 就绪通知 |
| 异步 IO | `AIO`（`AsynchronousServerSocketChannel`） | 完成通知 |

> 💡 Linux 的 epoll 是**水平触发（LT）**和**边缘触发（ET）**。Java NIO Selector 是**水平触发**，Netty 支持 epoll ET（如果使用 `EpollEventLoopGroup`）。

---

### Q22：零拷贝（Zero-Copy）原理？

**零拷贝**：减少数据在 **内核态** 和 **用户态** 之间的拷贝次数。

```
传统 IO（4 次拷贝，4 次上下文切换）：
磁盘 → 内核缓冲区 → 用户缓冲区 → Socket 缓冲区 → 网卡

零拷贝（2 次拷贝，2 次上下文切换）：
磁盘 → 内核缓冲区 → 网卡（DMA 直接传输）
```

| 技术 | 方法 | 说明 |
|------|------|------|
| **sendFile** | `FileChannel.transferTo()` | 数据从磁盘→内核→网卡，不经过用户态 |
| **MMAP** | `FileChannel.map()` | 文件映射到内存，用户态直接操作 |
| **Direct Buffer** | `DirectByteBuffer` | 用户态直接操作堆外内存，减少拷贝 |
| **Netty Composite** | `CompositeByteBuf` | 逻辑组合多个 buffer，避免物理组合 |

```java
// 零拷贝文件传输（FileChannel.transferTo）
public void zeroCopySend(FileChannel fileChannel, SocketChannel socketChannel, long length) {
    long position = 0;
    while (position < length) {
        long transferred = fileChannel.transferTo(position, length - position, socketChannel);
        position += transferred;
    }
}

// MMAP（内存映射文件）
public void mmapRead(String filePath) throws IOException {
    try (RandomAccessFile file = new RandomAccessFile(filePath, "r");
         FileChannel channel = file.getChannel()) {

        MappedByteBuffer mapped = channel.map(
            FileChannel.MapMode.READ_ONLY, 0, channel.size());

        // 直接读取映射内存
        byte[] data = new byte[(int) channel.size()];
        mapped.get(data);
    }
}

// Netty 零拷贝 — FileRegion
FileRegion region = new DefaultFileRegion(
    new FileInputStream(file).getChannel(), 0, file.length());
ctx.writeAndFlush(region);  // 零拷贝发送
```

> 💡 Kafka 使用 `FileChannel.transferTo()` 实现文件到网络的高效传输，这是 Kafka 高性能的关键之一。

---

### Q23：Netty 的 writeBufferWaterMark 是什么？

用于控制 **写缓冲区水位线**，当 write buffer 积压超过高水位时，自动对 Channel 进行写停止反馈。

```java
// 默认水位
// lowWaterMark = 32KB
// highWaterMark = 64KB

// 自定义
ServerBootstrap b = new ServerBootstrap();
b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
    new WriteBufferWaterMark(64 * 1024, 256 * 1024));  // low=64KB, high=256KB

// 处理写水位回调
public class WriteMonitorHandler extends ChannelDuplexHandler {
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        if (channel.isWritable()) {
            System.out.println("可以继续写数据");
            // 恢复发送
        } else {
            System.out.println("写缓冲区满（超过 highWaterMark），暂停发送");
            // 暂停发送，等待缓冲区下降
        }
        ctx.fireChannelWritabilityChanged();
    }
}
```

**原理：**
```
写数据 → 进入 ChannelOutboundBuffer
         ↓
    Channel.isWritable() == true  (buffer < lowWaterMark)
         ↓
    Channel.isWritable() == false (buffer > highWaterMark)
         ↓
    write 积压 → 自动停止从业务层读取
```

---

### Q24：Netty 的 EventLoop 源码执行流程？

```java
// 简化版 EventLoop 源码分析（NioEventLoop.run()）
@Override
protected void run() {
    int selectCnt = 0;
    for (;;) {  // 永不停歇的事件循环
        try {
            int strategy;
            try {
                strategy = selectStrategy.calculateStrategy(
                    selectSupplier, hasTasks());
                switch (strategy) {
                    case SelectStrategy.SELECT:
                        // 没有任务时，阻塞 select
                        strategy = select(selectDeadlineNanos);
                    case SelectStrategy.CONTINUE:
                        continue;
                    default:
                        // fallthrough
                }
            } catch (IOException e) {
                rebuildSelector();  // 处理 select 空轮询 bug
                continue;
            }

            selectCnt++;  // 记录 select 次数（检测空轮询）
            cancelledKeys = processSelectedKeys();  // 处理 IO 事件
            selectCnt = 0;

            runAllTasks();  // 执行非 IO 任务和定时任务

            if (isShuttingDown()) {
                closeAll();
                return;
            }
        } catch (Throwable t) {
            handleLoopException(t);
        }
    }
}

// 关键点：
// 1. select() 阻塞等待事件（或定时任务到期）
// 2. processSelectedKeys() 处理 accept/read/write 事件
// 3. runAllTasks() 执行 schedule 和 submit 的非 IO 任务
// 4. ioRatio 控制 IO 和非 IO 任务的时间占比
```

---

### Q25：Netty 的 ioRatio 参数？

`ioRatio` 控制 **IO 任务** 和 **非 IO 任务** 的执行时间比例。

```java
// 默认 ioRatio = 50（IO 和非 IO 各占 50% 的时间）
// ioRatio = 100：不限制，处理完所有非 IO 任务再循环

// 设置（通过 EventLoopGroup 构造函数）
EventLoopGroup group = new NioEventLoopGroup(
    4,                    // 线程数
    new DefaultThreadFactory("worker"),
    new DefaultSelectStrategyFactory() {
        @Override
        public SelectStrategy newSelectStrategy() {
            return new DefaultSelectStrategy(70);  // ioRatio = 70
        }
    }
);
```

```
ioRatio = 50 时的执行模型：

while (true) {
    select();                         // 阻塞等待事件
    long ioTime = processSelectedKeys();  // 记录 IO 消耗时间

    // 非 IO 任务最多执行的时间
    // taskTime = ioTime * (100 - ioRatio) / ioRatio
    // ioRatio = 50 → taskTime = ioTime，各占一半
    // ioRatio = 70 → taskTime ≈ ioTime * 0.43
    runAllTasks(taskTime);
}
```

---

### Q26：Netty 启动流程源码分析（bind 过程）？

```java
// 核心调用链
// Bootstrap.bind() → AbstractBootstrap.doBind()

// doBind() 源码分析
private ChannelFuture doBind(final SocketAddress localAddress) {
    // 1. initAndRegister() — 创建并初始化 Channel，注册到 EventLoop
    final ChannelFuture regFuture = initAndRegister();

    final Channel channel = regFuture.channel();
    if (regFuture.isDone()) {
        ChannelPromise promise = channel.newPromise();
        doBind0(regFuture, channel, localAddress, promise);
        return promise;
    } else {
        // 异步注册完成时再执行 bind
        // ... 注册回调
    }

    return regFuture;
}

// initAndRegister() 流程：
// 1. channelFactory.newChannel() → 反射创建 NioServerSocketChannel
// 2. init(channel) → 设置 option、attr、Pipeline（添加 ServerBootstrapAcceptor）
// 3. config().group().register(channel) → 将 Channel 注册到 Boss EventLoop 的 Selector

// doBind0() 流程：
// 1. javaChannel().bind(localAddress, config.getBacklog()) → JDK ServerSocketChannel.bind()
// 2. pipeline.fireChannelActive() → 触发 ChannelActive 事件
// 3. 在 ChannelActive 中，ServerBootstrapAcceptor 开始关注 OP_ACCEPT 事件
```

---

### Q27：Netty accept 流程源码分析？

```java
// 1. NioEventLoop 中的 processSelectedKeys()
→ processSelectedKeysOptimized()
→ processSelectedKey(SelectionKey k, AbstractNioChannel ch)

// 2. 如果是 OP_ACCEPT 事件
→ NioServerSocketChannel.unsafe.read()
   → doReadMessages(jdkChannel.accept())  // 调用 ServerSocketChannel.accept()
   → 创建 NioSocketChannel（包装 JDK SocketChannel）

// 3. 将 NioSocketChannel 交给 childHandler
→ pipeline.fireChannelRead(nioSocketChannel)
   → ServerBootstrapAcceptor.channelRead()

// 4. ServerBootstrapAcceptor.channelRead()
   // a. 为 NioSocketChannel 添加 childHandler
   child.pipeline().addLast(childHandler);
   // b. 设置 childOptions / childAttrs
   // c. 注册到 WorkerGroup 的 EventLoop
   childGroup.register(child).addListener(...)
   // 注册 = 将 NioSocketChannel 的 OP_READ 注册到 Worker 的 Selector
```

---

### Q28：Netty read 流程源码分析？

```java
// 1. Worker EventLoop 检测到 OP_READ 事件
// 2. NioByteUnsafe.read() 被调用
@Override
public final void read() {
    final ChannelConfig config = config();
    final RecvByteBufAllocator.Handle allocHandle = config.getRecvByteBufAllocator().newHandle();

    // 循环读取，直到读完或达到最大读取次数（默认 16 次）
    do {
        // 分配 ByteBuf
        ByteBuf byteBuf = allocHandle.allocate(allocator);

        // 从 SocketChannel 读入数据
        int readBytes = doReadBytes(byteBuf);

        if (readBytes <= 0) {
            byteBuf.release();
            break;
        }

        allocHandle.lastBytesRead(readBytes);
        // 触发 channelRead 事件 → Pipeline 中的 Handler 处理
        pipeline.fireChannelRead(byteBuf);
    } while (allocHandle.continueReading());

    // 所有数据读完，触发 channelReadComplete
    pipeline.fireChannelReadComplete();

    // 如果我们读到数据，可能是关注写事件
    // 做一些清理
}
```

---

### Q29：Netty 的 Selector 空轮询 Bug 及处理？

**问题：** JDK NIO Selector 在 Linux 上偶发 `select()` 立即返回 0 的事件，导致 CPU 100%。

**原因：** JDK epoll 实现的 Bug（Linux 内核 `epoll_wait` 在特定条件下的空轮询）。

```java
// Netty 的处理（NioEventLoop.select()）

// 检测空轮询：
// 1. 记录当前时间
// 2. 调用 select()
// 3. 如果 select() 返回 0，增加 selectCnt
// 4. 如果 selectCnt > SELECTOR_AUTO_REBUILD_THRESHOLD（默认 512）
//    触发重建 Selector

private void select(boolean oldWakenUp) throws IOException {
    Selector selector = this.selector;
    int selectCnt = 0;

    for (;;) {
        int selectedKeys = selector.select(timeoutMillis);
        selectCnt++;

        if (selectedKeys != 0 || oldWakenUp || hasTasks() || hasScheduledTasks()) {
            break;  // 正常有事件
        }

        // 空轮询检测
        if (selectCnt > SELECTOR_AUTO_REBUILD_THRESHOLD) {
            // 重建 Selector
            rebuildSelector();
            break;
        }
    }
}

// 重建 Selector 逻辑：
// 1. 创建一个新的 Selector
// 2. 将旧 Selector 上所有 Channel 的 SelectionKey 迁移到新 Selector
// 3. 关闭旧 Selector
// 4. 替换 NioEventLoop 中的 selector 引用
```

---

### Q30：Netty 内存分配机制（PooledByteBufAllocator）？

```
PooledByteBufAllocator 内存分配体系：

Arena（竞技场，默认 CPU 核数 × 2）
  │
  ├── PoolChunkList（Chunk 链表，8 级）
  │     │
  │     ├── PoolChunk（16MB）
  │     │     │
  │     │     ├── PoolSubpage（8KB 页 → 更小的分配单元）
  │     │     │     ├── 512 byte × 16
  │     │     │     ├── 256 byte × 32
  │     │     │     └── ...
  │     │     │
  │     │     └── 普通分配（buddy allocation 伙伴算法）
  │     │
  │     ├── PoolChunk（16MB）
  │     └── ...
  │
  └── Tiny / Small / Normal / Huge 分配策略

分配策略：
- Tiny（<512B）：从 PoolSubpage 分配
- Small（512B ~ 8KB）：从 PoolSubpage 分配
- Normal（8KB ~ 16MB）：从 PoolChunk 分配
- Huge（>16MB）：非池化，每次新建
```

> 💡 **PoolThreadCache**：每个线程有独立的缓存，分配时优先从 ThreadLocal 缓存获取，避免加锁。这是 Netty 高性能内存分配的核心。

---

### Q31：Netty 与 Tomcat 线程模型对比？

| 对比维度 | Tomcat | Netty |
|---------|--------|-------|
| **IO 模型** | BIO 或 NIO（默认 NIO）| NIO / Epoll |
| **线程模型** | 1 连接 1 线程（BIO）或少量线程处理 IO | Reactor：Boss + Worker |
| **连接管理** | 线程池管理连接 | EventLoop 管理 Channel |
| **请求处理** | IO 线程 + 业务线程 | 默认在 IO 线程处理 |
| **CPU 利用** | 多线程竞争 | 单线程处理多个连接（无竞争）|
| **内存管理** | JDK ByteBuffer | PooledByteBuf（池化）|
| **协议支持** | HTTP 为主 | 多种协议（自定义协议）|

```java
// Tomcat NIO 模型
// Acceptor 线程 → accept() → Poller 线程 → read → Worker 线程 → 业务处理

// Netty 模型
// Boss EventLoop → accept → Worker EventLoop → read + 业务处理
```

> 💡 Tomcat 适用于 HTTP 请求-响应模式，Netty 适用于长连接和自定义协议的高性能通信场景。

---

### Q32：Thin 和 Netty 的 Future/Promise 的关系？

```
Promise 继承体系：

java.util.concurrent.Future（JDK）
  │
io.netty.util.concurrent.Future（Netty）
  │  └─ addListener() ✓  ← Netty 的增强
  │  └─ sync() / await() ← 比 JDK get() 更可控
  │
io.netty.util.concurrent.Promise（Netty）
  │  └─ setSuccess()     ← 可写入结果
  │  └─ setFailure()     ← 可写入异常
  │  └─ trySuccess()     ← 非阻塞版本
  │
DefaultPromise（实现类）
  │  └─ 内部使用 wait() / notifyAll() 实现线程间通信
```

```java
// Promise 使用场景：异步 RPC 调用
public class RpcClient {
    private final Map<Long, Promise<Object>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicLong requestIdGen = new AtomicLong();

    public Object call(String method, Object[] args) throws Exception {
        long requestId = requestIdGen.incrementAndGet();

        // 创建 Promise
        Promise<Object> promise = group.next().newPromise();
        pendingRequests.put(requestId, promise);

        // 发送请求
        sendRequest(requestId, method, args);

        // 等待结果（最多 5s）
        return promise.get(5, TimeUnit.SECONDS);
    }

    public void handleResponse(long requestId, Object result) {
        Promise<Object> promise = pendingRequests.remove(requestId);
        if (promise != null) {
            promise.setSuccess(result);
        }
    }
}
```

---

## 三、实战场景题（10题）

### Q33：Netty 实现 HTTP 服务端？

```java
public class HttpServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(
                         new HttpServerCodec(),             // HTTP 编解码
                         new HttpObjectAggregator(65536),   // 聚合 HTTP 请求
                         new HttpServerHandler()
                     );
                 }
             });
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("HTTP Server started on 8080");
            f.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            String uri = req.uri();
            System.out.println("收到请求: " + uri);

            // 构造响应
            String responseBody = "{\"message\": \"Hello, Netty!\"}";
            FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, OK,
                Unpooled.wrappedBuffer(responseBody.getBytes())
            );
            response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=UTF-8")
                .set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

            ctx.writeAndFlush(response);
        }
    }
}
```

---

### Q34：Netty 实现 WebSocket 服务端？

```java
public class WebSocketServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(
                         new HttpServerCodec(),
                         new HttpObjectAggregator(65536),
                         new WebSocketServerProtocolHandler("/ws"),  // WebSocket 升级
                         new WebSocketFrameHandler()                 // 自定义处理
                     );
                 }
             });
            ChannelFuture f = b.bind(8888).sync();
            f.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}

// WebSocket 消息处理
public class WebSocketFrameHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) msg).text();
            System.out.println("收到 WebSocket 消息: " + text);
            ctx.channel().writeAndFlush(
                new TextWebSocketFrame("Server echo: " + text)
            );
        } else if (msg instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame());
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        System.out.println("WebSocket 连接建立: " + ctx.channel().remoteAddress());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        System.out.println("WebSocket 连接断开: " + ctx.channel().remoteAddress());
    }
}
```

---

### Q35：如何用 LengthFieldBasedFrameDecoder 设计自定义协议？

```java
// 自定义协议格式
// ┌─────────┬──────────┬────────────┬──────────────┐
// │ Magic(2)│ Version(1)│ Length(4)  │  Body(n)      │
// ├─────────┼──────────┼────────────┼──────────────┤
// │ 0xCAFE  │    1     │  body_len  │ JSON/Protobuf  │
// └─────────┴──────────┴────────────┴──────────────┘

// 服务端 Pipeline
ch.pipeline().addLast(
    new LengthFieldBasedFrameDecoder(
        1024 * 1024,  // maxFrameLength: 最大 1MB
        3,            // lengthFieldOffset: 跳过 Magic(2) + Version(1)
        4,            // lengthFieldLength: Length 占 4 字节
        0,            // lengthAdjustment: 不需要调整
        7             // initialBytesToStrip: 去掉包头 (2+1+4)
    ),
    new CustomProtocolDecoder(),
    new CustomProtocolEncoder(),
    new BusinessHandler()
);

// 自定义解码器
public class CustomProtocolDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 经过 LengthFieldBasedFrameDecoder 后，in 就是完整的 body
        byte[] data = new byte[in.readableBytes()];
        in.readBytes(data);
        out.add(new CustomRequest(new String(data, StandardCharsets.UTF_8)));
    }
}

// 自定义编码器
public class CustomProtocolEncoder extends MessageToByteEncoder<CustomResponse> {
    @Override
    protected void encode(ChannelHandlerContext ctx, CustomResponse msg, ByteBuf out) {
        byte[] body = msg.toJson().getBytes(StandardCharsets.UTF_8);

        out.writeShort(0xCAFE);     // Magic
        out.writeByte(1);           // Version
        out.writeInt(body.length);  // Length
        out.writeBytes(body);       // Body
    }
}
```

---

### Q36：Netty 实现心跳检测 + 断线重连？

```java
// 客户端（含断线重连）
public class ReconnectClient {
    private static final int MAX_RETRIES = 5;
    private static final AtomicInteger RETRY_COUNT = new AtomicInteger(0);

    public static void main(String[] args) {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(NioSocketChannel.class)
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ch.pipeline().addLast(
                     new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS),
                     new HeartbeatEncoder(),
                     new ClientBusinessHandler()
                 );
             }
         });

        connect(b, "localhost", 8080);
    }

    private static void connect(Bootstrap b, String host, int port) {
        b.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("连接成功");
                RETRY_COUNT.set(0);
            } else {
                if (RETRY_COUNT.getAndIncrement() < MAX_RETRIES) {
                    int delay = 1 << RETRY_COUNT.get();  // 指数退避：1, 2, 4, 8, 16s
                    System.out.println("重连中... " + delay + "s 后重试");
                    future.channel().eventLoop().schedule(
                        () -> connect(b, host, port), delay, TimeUnit.SECONDS);
                }
            }
        });
    }
}

// 心跳编码器
public class HeartbeatEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof HeartbeatPacket) {
            ByteBuf buf = ctx.alloc().buffer(8);
            buf.writeLong(0xDEADBEEF);  // 心跳标识
            ctx.writeAndFlush(buf);
        } else {
            ctx.write(msg, promise);
        }
    }
}
```

---

### Q37：Netty 实现聊天室（群聊）？

```java
// 群聊处理器（@Sharable 共享）
@ChannelHandler.Sharable
public class ChatServerHandler extends SimpleChannelInboundHandler<String> {

    // 保存所有在线 Channel
    private static final ChannelGroup onlineUsers = new DefaultChannelGroup(
        GlobalEventExecutor.INSTANCE);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        onlineUsers.add(ctx.channel());
        String msg = "[系统] " + ctx.channel().remoteAddress() + " 加入群聊";
        onlineUsers.writeAndFlush(msg + "\n");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        String broadcast = "[" + ctx.channel().remoteAddress() + "]: " + msg;
        // 群发消息（排除自己，可选）
        onlineUsers.writeAndFlush(broadcast + "\n", ch -> ch != ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        onlineUsers.remove(ctx.channel());
        String msg = "[系统] " + ctx.channel().remoteAddress() + " 退出群聊";
        onlineUsers.writeAndFlush(msg + "\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

// 聊天服务端
public class ChatServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                            new LineBasedFrameDecoder(2048),
                            new StringDecoder(),
                            new ChatServerHandler());
                    }
                })
                .bind(8080).sync().channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
```

---

### Q38：Netty 写数据时如何控制流量（背压）？

```java
// 背压策略：利用 isWritable + ChannelWritabilityChanged

public class BackPressureHandler extends ChannelDuplexHandler {
    private final int lowWaterMark;
    private final int highWaterMark;

    public BackPressureHandler(int low, int high) {
        this.lowWaterMark = low;
        this.highWaterMark = high;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (ctx.channel().isWritable()) {
            ctx.write(msg, promise);
        } else {
            // 缓冲区满了，丢弃或暂存
            byte[] data = ((ByteBuf) msg).array();
            System.out.println("缓冲区满，丢弃数据: " + data.length + " bytes");
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        if (ctx.channel().isWritable()) {
            System.out.println("缓冲区可写，从暂存队列中恢复发送");
            // 发送暂存队列中的数据
            flushPendingMessages(ctx);
        }
        ctx.fireChannelWritabilityChanged();
    }
}

// 或者使用 ChannelOption.WRITE_BUFFER_WATER_MARK
ServerBootstrap b = new ServerBootstrap();
b.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 32 * 1024)
 .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 64 * 1024);
```

---

### Q39：Netty 的 EmbeddedChannel 单元测试？

```java
// 使用 EmbeddedChannel 对 Handler 进行单元测试
@Test
public void testIntegerCodec() {
    EmbeddedChannel channel = new EmbeddedChannel(
        new IntegerDecoder(),
        new IntegerEncoder()
    );

    // 测试解码（入站）
    ByteBuf input = Unpooled.buffer();
    input.writeInt(1);
    input.writeInt(2);
    input.writeInt(3);
    assertTrue(channel.writeInbound(input));
    assertTrue(channel.finish());

    // 读取解码后的数据
    assertEquals(1, channel.readInbound());
    assertEquals(2, channel.readInbound());
    assertEquals(3, channel.readInbound());
    assertNull(channel.readInbound());

    // 测试编码（出站）
    assertTrue(channel.writeOutbound(42));
    ByteBuf output = channel.readOutbound();
    assertEquals(42, output.readInt());
    output.release();
}

// 测试异常路径
@Test(expected = TooLongFrameException.class)
public void testLineFrameDecoder_ExceedMaxLength() {
    EmbeddedChannel channel = new EmbeddedChannel(
        new LineBasedFrameDecoder(5)  // 最大 5 字节
    );
    ByteBuf buf = Unpooled.wrappedBuffer("Hello World\n".getBytes());
    channel.writeInbound(buf);  // 抛出 TooLongFrameException
}
```

---

### Q40：生产环境 Netty 参数调优？

```java
// Netty 生产配置模板
public class NettyProductionConfig {

    // 1. 线程模型优化
    // Boss 线程 = 1（只需一个 accept）
    // Worker 线程 = CPU 核数 × 2（IO 密集型）
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup(
        Runtime.getRuntime().availableProcessors() * 2,
        new DefaultThreadFactory("NettyWorker", true)  // daemon = true
    );

    // 2. ServerBootstrap 配置
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
     .channel(NioServerSocketChannel.class)

     // 3. 服务端参数
     .option(ChannelOption.SO_BACKLOG, 1024)
     .option(ChannelOption.SO_REUSEADDR, true)

     // 4. 客户端连接参数
     .childOption(ChannelOption.TCP_NODELAY, true)        // 禁用 Nagle
     .childOption(ChannelOption.SO_KEEPALIVE, true)        // TCP 心跳
     .childOption(ChannelOption.SO_RCVBUF, 128 * 1024)    // 接收缓冲区 128KB
     .childOption(ChannelOption.SO_SNDBUF, 128 * 1024)    // 发送缓冲区 128KB
     .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
         new WriteBufferWaterMark(32 * 1024, 256 * 1024)) // 写水位

     // 5. 内存池化（默认已开启）
     // System.setProperty("io.netty.allocator.type", "pooled");
     // System.setProperty("io.netty.allocator.maxOrder", "9");  // 最大 8MB
     // System.setProperty("io.netty.allocator.numDirectArenas", "8");
}
```

---

### Q41：Netty 如何支持多种序列化协议？

```java
// 序列化策略接口
public interface Serialization {
    <T> byte[] serialize(T obj);
    <T> T deserialize(byte[] data, Class<T> clazz);
}

// 实现：JSON 序列化
public class JsonSerialization implements Serialization {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public <T> byte[] serialize(T obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize error", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            return mapper.readValue(data, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON deserialize error", e);
        }
    }
}

// 使用策略模式扩展
public class SerializationFactory {
    private static final Map<Byte, Serialization> MAP = new HashMap<>();

    static {
        MAP.put((byte) 0x01, new JsonSerialization());
        // 可扩展：Protobuf、Hessian、Kryo 等
        // MAP.put((byte) 0x02, new ProtobufSerialization());
        // MAP.put((byte) 0x03, new KryoSerialization());
    }

    public static Serialization get(byte type) {
        Serialization s = MAP.get(type);
        if (s == null) throw new IllegalArgumentException("未知序列化类型: " + type);
        return s;
    }
}
```

---

### Q42：Netty 如何实现 SSL/TLS 加密通信？

```java
// 添加 SslHandler 到 Pipeline
public class SslServer {
    public static void main(String[] args) throws Exception {
        // 加载证书
        SelfSignedCertificate cert = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(cert.certificate(), cert.privateKey())
            .build();

        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(
                         sslCtx.newHandler(ch.alloc()),  // SSL 处理器
                         new HttpServerCodec(),
                         new HttpObjectAggregator(65536),
                         new BusinessHandler()
                     );
                 }
             });
            ChannelFuture f = b.bind(8443).sync();
            f.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
```

---

## 四、手写代码/配置文件题（6题）

### Q43：Netty 服务端完整启动代码

```java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {
    private final int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        // 1. 创建线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 2. 创建启动器
            ServerBootstrap bootstrap = new ServerBootstrap();

            // 3. 配置
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .option(ChannelOption.SO_BACKLOG, 1024)
                     .childOption(ChannelOption.SO_KEEPALIVE, true)
                     .childOption(ChannelOption.TCP_NODELAY, true)
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             ch.pipeline().addLast(
                                 new LineBasedFrameDecoder(2048),
                                 new StringDecoder(),
                                 new ServerHandler()
                             );
                         }
                     });

            // 4. 绑定端口
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("Server started on port: " + port);

            // 5. 等待关闭
            future.channel().closeFuture().sync();
        } finally {
            // 6. 优雅关闭
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new NettyServer(8080).start();
    }
}

@ChannelHandler.Sharable
class ServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String request = (String) msg;
        System.out.println("收到: " + request);
        ctx.writeAndFlush(Unpooled.copiedBuffer(
            ("回应: " + request + "\n").getBytes()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

---

### Q44：Netty 客户端完整启动代码

```java
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
    private final String host;
    private final int port;

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                     .channel(NioSocketChannel.class)
                     .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                     .option(ChannelOption.TCP_NODELAY, true)
                     .handler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             ch.pipeline().addLast(
                                 new StringEncoder(),
                                 new ClientHandler()
                             );
                         }
                     });

            // 连接
            ChannelFuture future = bootstrap.connect(host, port).sync();
            System.out.println("Connected to " + host + ":" + port);

            // 发送消息
            future.channel().writeAndFlush("Hello, Netty!\n");

            // 等待关闭
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new NettyClient("localhost", 8080).start();
    }
}

class ClientHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("收到服务端响应: " + msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("连接建立成功");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

---

### Q45：自定义编解码器（ByteToMessageDecoder + MessageToByteEncoder）

```java
// 协议定义
// ┌──────┬───────┬──────────┐
// │ type │ length │  body    │
// │ 1B   │ 4B     │  nB      │
// └──────┴───────┴──────────┘

// 消息类
@Data
public class Message {
    private byte type;       // 消息类型
    private int length;      // 消息体长度
    private String body;     // 消息体（JSON 字符串）
}

// 解码器
public class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 检查可读字节
        if (in.readableBytes() < 5) {
            return;  // 头部还没收完整
        }

        // 标记当前读位置
        in.markReaderIndex();

        byte type = in.readByte();
        int length = in.readInt();

        if (in.readableBytes() < length) {
            // body 还没到齐，重置读指针等下一次
            in.resetReaderIndex();
            return;
        }

        byte[] bodyBytes = new byte[length];
        in.readBytes(bodyBytes);

        Message message = new Message();
        message.setType(type);
        message.setLength(length);
        message.setBody(new String(bodyBytes, StandardCharsets.UTF_8));
        out.add(message);
    }
}

// 编码器
public class MessageEncoder extends MessageToByteEncoder<Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
        byte[] bodyBytes = msg.getBody().getBytes(StandardCharsets.UTF_8);
        out.writeByte(msg.getType());
        out.writeInt(bodyBytes.length);
        out.writeBytes(bodyBytes);
    }
}
```

---

### Q46：Netty 实现简易 RPC 框架（核心代码）

```java
// ========== RPC 请求/响应 ==========
@Data
public class RpcRequest implements Serializable {
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Class<?>[] paramTypes;
    private Object[] args;
}

@Data
public class RpcResponse implements Serializable {
    private String requestId;
    private Object result;
    private Throwable error;
}

// ========== 服务端 ==========
public class RpcServer {
    public void start(int port, Map<String, Object> services) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(
                         new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4),
                         new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                         new ObjectEncoder(),
                         new RpcServerHandler(services)
                     );
                 }
             });
            b.bind(port).sync().channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}

// ========== 客户端代理 ==========
public class RpcClientProxy {
    @SuppressWarnings("unchecked")
    public static <T> T create(final Class<T> interfaceClass, final String host, final int port) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class[]{interfaceClass},
            (proxy, method, args) -> {
                RpcRequest request = new RpcRequest();
                request.setRequestId(UUID.randomUUID().toString());
                request.setInterfaceName(interfaceClass.getName());
                request.setMethodName(method.getName());
                request.setParamTypes(method.getParameterTypes());
                request.setArgs(args);

                // 发送请求并等待响应
                RpcClient client = new RpcClient(host, port);
                RpcResponse response = client.send(request);

                if (response.getError() != null) {
                    throw response.getError();
                }
                return response.getResult();
            }
        );
    }
}
```

---

### Q47：Netty 文件服务器

```java
public class FileServerHandler extends ChannelInboundHandlerAdapter {
    private static final String BASE_DIR = "D:/files/";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FileRequest) {
            handleFileRequest(ctx, (FileRequest) msg);
        } else if (msg instanceof FileUpload) {
            handleFileUpload(ctx, (FileUpload) msg);
        }
    }

    // 文件下载
    private void handleFileRequest(ChannelHandlerContext ctx, FileRequest req) {
        File file = new File(BASE_DIR + req.getFileName());
        if (!file.exists()) {
            ctx.writeAndFlush(new FileResponse("File not found"));
            return;
        }

        // 使用 FileRegion 零拷贝
        try {
            FileRegion region = new DefaultFileRegion(
                new FileInputStream(file).getChannel(), 0, file.length());
            ctx.writeAndFlush(new FileResponse("OK", file.length()))
               .addListener(f -> ctx.writeAndFlush(region));
        } catch (IOException e) {
            ctx.writeAndFlush(new FileResponse("Error: " + e.getMessage()));
        }
    }

    // 文件上传
    private void handleFileUpload(ChannelHandlerContext ctx, FileUpload upload) {
        try (FileOutputStream fos = new FileOutputStream(BASE_DIR + upload.getFileName())) {
            fos.write(upload.getData());
            ctx.writeAndFlush(new FileResponse("Upload success"));
        } catch (IOException e) {
            ctx.writeAndFlush(new FileResponse("Upload failed: " + e.getMessage()));
        }
    }
}
```

---

### Q48：Netty 中的 StringDecoder 和 StringEncoder 配置

```java
// 完整 Pipeline 配置（基于行分隔符的字符串通信）
public class StringPipelineConfig {
    public static void configure(ChannelPipeline p) {
        p.addLast(new LineBasedFrameDecoder(4096));     // 行解码器
        p.addLast(new StringDecoder(CharsetUtil.UTF_8)); // ByteBuf → String
        p.addLast(new StringEncoder(CharsetUtil.UTF_8)); // String → ByteBuf
        p.addLast(new StringBusinessHandler());
    }

    // 如果使用分隔符
    public static void configureWithDelimiter(ChannelPipeline p) {
        ByteBuf delimiter = Unpooled.copiedBuffer("##END##".getBytes());
        p.addLast(new DelimiterBasedFrameDecoder(4096, delimiter));
        p.addLast(new StringDecoder());
        p.addLast(new StringEncoder());
        p.addLast(new StringBusinessHandler());
    }
}

// 如果使用基于长度的协议
public static void configureLengthProtocol(ChannelPipeline p) {
    p.addLast(new LengthFieldBasedFrameDecoder(
        1024, 0, 4, 0, 4));          // 去掉 4 字节长度头
    p.addLast(new StringDecoder());    // body 转为 String
    p.addLast(new StringEncoder());
    p.addLast(new BusinessHandler());
}
```

---

## 五、系统设计题（4题）

### Q49：设计一个基于 Netty 的 RPC 框架

```java
// 整体架构
┌─────────────────────────────────────────────────┐
│               RPC Client                         │
│  ┌─────────────┐    ┌───────────────────────┐   │
│  │ 动态代理     │───→│ Netty Client          │   │
│  │ (JDK Proxy) │    │ (连接池 / 心跳)       │   │
│  └─────────────┘    └──────────┬────────────┘   │
│                                │                  │
└────────────────────────────────┼─────────────────┘
                                 │ 网络传输
┌────────────────────────────────┼─────────────────┐
│  RPC Server                    │                  │
│  ┌─────────────────────────────┴────────────┐   │
│  │  Netty Server (Boss/Worker)              │   │
│  │  ┌─────────┐ ┌────────┐ ┌───────────┐   │   │
│  │  │ Decoder │→│ Thread │→│ 调用本地   │   │   │
│  │  │         │←│ Pool   │←│ Service   │   │   │
│  │  │ Encoder │ │        │ │ 返回结果   │   │   │
│  │  └─────────┘ └────────┘ └───────────┘   │   │
│  └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

**设计要点：**
1. **动态代理**：客户端通过 JDK Proxy 生成接口代理
2. **连接池**：池化管理 TCP 连接，复用 Channel
3. **序列化**：支持多种序列化（JSON / Protobuf / Kryo）
4. **心跳**：客户端定时发心跳，服务端 IdleStateHandler 检测
5. **负载均衡**：支持随机、轮询、最小连接等策略
6. **超时控制**：每个 RPC 请求设置超时时间，超时自动移除 Promise
7. **异常处理**：网络异常自动重试（幂等接口）

---

### Q50：设计一个高性能消息推送系统

```java
// 系统架构
// Client → Netty Gateway (长连接维护) → Kafka → Push Service → Netty Gateway → Client

public class PushServer {
    // 连接管理（全局映射：userId → Channel）
    private static final ConcurrentHashMap<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

    public static void registerUser(String userId, Channel channel) {
        Channel old = USER_CHANNEL_MAP.put(userId, channel);
        if (old != null && old.isActive()) {
            old.close();  // 相同用户重复登录，踢掉旧连接
        }
    }

    public static void pushToUser(String userId, Object message) {
        Channel channel = USER_CHANNEL_MAP.get(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }

    public static void removeUser(String userId) {
        USER_CHANNEL_MAP.remove(userId);
    }

    // 广播消息
    public static void broadcast(Object message) {
        USER_CHANNEL_MAP.values().forEach(ch -> {
            if (ch.isActive()) {
                ch.writeAndFlush(message);
            }
        });
    }
}
```

**架构要点：**
| 组件 | 职责 | 选型 |
|------|------|------|
| 接入层 | 维持客户端长连接 | Netty（百万连接）|
| 鉴权层 | 登录/Token 校验 | Netty Handler |
| 路由层 | 用户 → 连接映射 | Redis / 本地 Map |
| 消息队列 | 削峰、异步、广播 | Kafka / RocketMQ |
| 推送层 | 将消息推送到目标连接 | Netty Channel |

---

### Q51：设计一个游戏服务器（帧同步/状态同步）

```java
// 游戏服务器架构（MMO 游戏）

public class GameServer {
    // 房间管理
    public static class Room {
        private final int roomId;
        private final List<Channel> players = new ArrayList<>();
        private final ScheduledFuture<?> gameLoop;

        public Room(int roomId) {
            this.roomId = roomId;
            // 50ms 一帧的 Game Loop
            this.gameLoop = GlobalEventExecutor.INSTANCE
                .scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
        }

        public void addPlayer(Channel ch) {
            synchronized (players) {
                players.add(ch);
            }
        }

        private void tick() {
            synchronized (players) {
                // 接收玩家输入，计算下一状态，广播给所有玩家
                // ...
                players.forEach(ch -> ch.writeAndFlush(gameState));
            }
        }
    }

    // 协议设计（Protobuf）
    // message PlayerInput {
    //    int32 direction = 1;    // 方向
    //    bool attack = 2;        // 攻击
    //    int64 timestamp = 3;    // 时间戳
    // }
    //
    // message GameState {
    //    repeated PlayerState players = 1;
    //    repeated Bullet bullets = 2;
    //    int64 frameId = 3;
    // }
}
```

---

### Q52：设计一个高性能网关（API Gateway）

```java
// API 网关架构
public class ApiGateway {
    // 路由配置
    // Map<路由模式, 后端服务地址>
    private static final Map<String, String> ROUTES = new HashMap<>();
    static {
        ROUTES.put("/api/user/**", "user-service:8081");
        ROUTES.put("/api/order/**", "order-service:8082");
        ROUTES.put("/api/product/**", "product-service:8083");
    }

    public static void main(String[] args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(
                         new HttpServerCodec(),
                         new HttpObjectAggregator(65536),
                         new GatewayHandler()
                     );
                 }
             });
            b.bind(80).sync().channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}

// GatewayHandler 核心功能：
// 1. 协议解析（HTTP/WebSocket/自定义）
// 2. 路由匹配（负载均衡）
// 3. 限流（令牌桶/滑动窗口）
// 4. 鉴权（Token/API Key）
// 5. 转发（使用 Netty HttpClient 转发到后端）
// 6. 超时控制
// 7. 日志和监控
```

---

## 六、常见坑点与最佳实践

### 常见坑点（Pitfalls）

| 坑 | 现象 | 解决方案 |
|----|------|---------|
| **ByteBuf 未 release** | 内存泄漏（Off-Heap Memory 飙升）| 用 `ReferenceCountUtil.release()` 或在 finally 中释放 |
| **Handler 未 @Sharable 却共享** | 数据混乱/并发问题 | 检查 Handler 状态，不加 @Sharable 则不共享实例 |
| **selector.select() 空轮询** | CPU 100% | 使用 Netty 提供的 NioEventLoop（已内置处理）|
| **阻塞操作在 EventLoop 中执行** | 所有连接都被阻塞 | 使用 `DefaultEventExecutorGroup` 或业务线程池 |
| **Pipeline 顺序错误** | 编解码器不生效 | 确认 Inbound 顺序（前→后），Outbound 顺序（后→前）|
| **粘包/半包未处理** | 数据错乱 | 必须使用 FrameDecoder（如 LengthFieldBasedFrameDecoder）|
| **Direct Buffer 分配过多** | Native Memory OOM | 使用池化 + 限制缓存大小 |
| **EventLoop 线程阻塞** | 连接超时/无响应 | 不要在 channelRead 中做耗时操作 |
| **未设置 TCP_NODELAY** | 延迟高（Nagle 算法）| `.childOption(ChannelOption.TCP_NODELAY, true)` |
| **连接未关闭（泄漏）** | 文件描述符耗尽 | 使用 ChannelGroup 统一管理，异常时关闭 |

### 最佳实践（Best Practices）

1. **内存管理**
   - 优先使用 `PooledByteBufAllocator`（默认已开启）
   - ByteBuf 使用完毕必须 release
   - 使用 `CompositeByteBuf` 代替手动数据合并

2. **线程模型**
   - Boss 线程数固定为 1（只需一个线程 accept）
   - Worker 线程数 = CPU 核数 × 2（IO 密集型）
   - 耗时业务逻辑放在**业务线程池**中执行

```java
// 在 EventLoop 外执行耗时操作
public class OffloadHandler extends ChannelInboundHandlerAdapter {
    private static final ExecutorService BUSINESS_POOL = Executors.newFixedThreadPool(200);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        BUSINESS_POOL.submit(() -> {
            // 耗时业务处理
            String result = doBusiness((String) msg);
            // 回到 EventLoop 写出
            ctx.executor().execute(() -> ctx.writeAndFlush(result));
        });
    }
}
```

3. **协议设计**
   - 自定义协议统一使用 **LengthFieldBasedFrameDecoder**
   - 协议版本号放前面，方便协议升级兼容
   - 序列化框架优先选 **Protobuf**（性能高、跨语言）

4. **连接管理**
   - 设置合理的超时时间
   - 空闲检测 + 心跳保活
   - 使用 ChannelGroup 统一管理连接

5. **性能监控**
   - Netty 内置 `NettyRuntime` 和 `SystemPropertyUtil` 可查看配置
   - 监控 `EventLoop.pendingTasks()`（待处理任务积压）
   - 监控 `Channel.isWritable()`（背压状态）

6. **生产参数推荐**

```properties
# JVM 参数
-Dio.netty.allocator.type=pooled
-Dio.netty.allocator.maxOrder=9
-Dio.netty.allocator.numDirectArenas=8
-Dio.netty.allocator.numHeapArenas=8
-Dio.netty.leakDetectionLevel=advanced  # 内存泄漏检测（生产环境建议 paranoid）
-Dio.netty.maxDirectMemory=0
```

---

## 七、面试回答模板（Top 5）

### 模板 1：谈谈 Netty 的线程模型

> "Netty 采用**主从 Reactor 多线程模型**：
>
> **BossGroup** 是主 Reactor，通常只有一个 EventLoop，负责处理客户端的连接请求（accept），然后将 SocketChannel 注册到 WorkerGroup 的某个 EventLoop。
>
> **WorkerGroup** 是从 Reactor，包含多个 EventLoop（默认 CPU 核数 × 2），每个 EventLoop 对应一个线程和一个 Selector，管理多个 Channel 的 IO 读写。
>
> 这种设计的优势：
> 1. 一个连接绑定到一个固定的 EventLoop，**无需加锁**处理该连接的所有事件
> 2. Boss 和 Worker 职责分离，accept 和 IO 互不影响
> 3. 一个 EventLoop 管理多个 Channel，线程数少，**上下文切换成本低**
> 4. EventLoop 内同时处理 IO 事件和非 IO 任务（定时任务、普通 Task）

### 模板 2：Netty 零拷贝的实现

> "Netty 的零拷贝体现在多个层面：
>
> **1. 操作系统层**（通过 FileRegion）：
> 使用 `FileChannel.transferTo()` 或 `DefaultFileRegion`，数据直接从**磁盘→内核缓冲区→网卡**，不经过用户态内存。适合文件传输场景。
>
> **2. Netty ByteBuf 层**：
> - `slice()`：共享同一块内存，只改变读写指针
> - `composite()`：逻辑组合多个 ByteBuf，不复制数据
> - `wrappedBuffer()`：包装外部数组/内存，避免拷贝
>
> **3. 直接内存（Direct Buffer）**：
> Java 堆外内存（DirectByteBuffer），Socket IO 直接操作，**省去从 JVM 堆到堆外的一次拷贝**。
>
> 这是 Netty 高性能通信的核心之一，Kafka 也通过类似机制实现高效文件传输。

### 模板 3：粘包/半包问题及解决方案

> "TCP 是**流式协议**，数据没有边界。粘包指多个包黏在一起被读，半包指一个包被拆分读。
>
> **原因**：
> - Nagle 算法（发送端合并小包）
> - TCP 接收缓冲区大小限制
> - 网络 MTU 限制
>
> **Netty 提供的解决方案**：
> - `FixedLengthFrameDecoder`：按固定长度拆分（适合定长协议）
> - `LineBasedFrameDecoder`：按行分隔符拆分（适合文本协议）
> - `DelimiterBasedFrameDecoder`：按自定义分隔符拆分
> - `LengthFieldBasedFrameDecoder`：按长度域拆分（**最推荐**，适合二进制协议）
>
> 企业级应用最常用的是 **LengthFieldBasedFrameDecoder**，在消息头部写入 body 长度，接收方先读取头部确定长度，再读取完整 body。

### 模板 4：Netty 内存管理

> "Netty 的内存管理显著优于 JDK NIO：
>
> **1. 池化**：`PooledByteBufAllocator` 是 Netty 4.1+ 默认分配器，使用**伙伴算法**（Buddy Allocation）管理内存，避免了频繁 GC。
>
> **2. 内存层级**：Arena → Chunk（16MB）→ Subpage（8KB）→ Tiny/Small/Normal/Huge 四级分配策略。
>
> **3. ThreadLocal 缓存**（PoolThreadCache）：每个线程有本地缓存，分配优先从本地取，**无锁分配**。
>
> **4. 引用计数**：使用 `refCnt()` 管理 ByteBuf 生命周期，`retain()` 增加引用，`release()` 减少，计数归零自动释放。
>
> **5. 内存泄漏检测**：通过 `-Dio.netty.leakDetectionLevel=paranoid` 开启 `ResourceLeakDetector`，在开发环境捕捉内存泄漏。

### 模板 5：Netty 源码启动流程

> "Netty 服务端启动分为三个核心步骤：
>
> **1. initAndRegister()**：
> - 通过反射创建 `NioServerSocketChannel`
> - `init(channel)`：向 Pipeline 添加 `ServerBootstrapAcceptor`（用于将新连接注册到 Worker）
> - `register(channel)`：将 Channel 注册到 Boss EventLoop 的 Selector（关注 0，后续改为 OP_ACCEPT）
>
> **2. doBind0()**：
> - 调用 JDK 的 `ServerSocketChannel.bind()` 绑定端口
> - 触发 `pipeline.fireChannelActive()` → 委托 `ServerBootstrapAcceptor` 关注 OP_ACCEPT
>
> **3. 事件循环**：
> - Boss EventLoop 检测到 OP_ACCEPT → `accept` → 创建 NioSocketChannel
> - 自动注册到 Worker EventLoop → Worker 监听 OP_READ
> - Worker 检测到 OP_READ → 读取数据 → Pipeline 处理

---

## 八、快速查漏补缺 CheckList

### Java NIO 基础
- [ ] 理解 BIO、NIO、AIO 的区别和适用场景
- [ ] 掌握 Channel、ByteBuffer、Selector 三大组件
- [ ] 会用 NIO 实现多路复用服务器
- [ ] 理解零拷贝（sendFile/MMAP/Direct Buffer）
- [ ] 理解 IO 模型（阻塞/非阻塞/多路复用/异步）

### Netty 基础
- [ ] 掌握 Netty 主从 Reactor 线程模型
- [ ] 理解 EventLoop 的工作循环（select → process → runTasks）
- [ ] 理解 ChannelPipeline 责任链模式
- [ ] 知道 Inbound/Outbound Handler 的区别和传播顺序
- [ ] 掌握 @Sharable 的使用前提

### ByteBuf
- [ ] 理解 readerIndex 和 writerIndex 双指针
- [ ] 知道 Heap vs Direct 的区别和使用场景
- [ ] 理解 Pooled vs Unpooled
- [ ] 掌握零拷贝方法（slice/composite/wrappedBuffer）
- [ ] 掌握 retain/release 引用计数管理

### 编解码与粘包
- [ ] 理解 ByteToMessageDecoder / MessageToByteEncoder
- [ ] 掌握 LengthFieldBasedFrameDecoder 配置
- [ ] 理解粘包/半包的原因和四种解决方案
- [ ] 掌握 ReplayingDecoder 简化解码

### 协议设计
- [ ] 会设计自定义协议（Magic + Version + Length + Body）
- [ ] 知道如何扩展序列化（JSON/Protobuf/Kryo）
- [ ] 掌握 Netty HTTP/WebSocket 集成

### 高级特性
- [ ] 理解 IdleStateHandler + 心跳机制
- [ ] 掌握 Future/Promise 异步编程
- [ ] 理解 WriteBufferWaterMark 背压机制
- [ ] 理解 ChannelOption 配置（BACKLOG/TCP_NODELAY/SO_KEEPALIVE）

### 源码分析
- [ ] 理解 bind 流程（initAndRegister → doBind0）
- [ ] 理解 accept 流程（NioEventLoop → ServerBootstrapAcceptor）
- [ ] 理解 read 流程（NioByteUnsafe.read → Pipeline）
- [ ] 理解 EventLoop 源码（select → processSelectedKeys → runAllTasks）
- [ ] 理解空轮询 Bug 的重建 Selector 机制

### 实战
- [ ] 会写完整的 Netty Server 启动代码
- [ ] 会写完整的 Netty Client 启动代码
- [ ] 会实现 RPC 框架的核心组件
- [ ] 会实现 WebSocket 服务端
- [ ] 会实现心跳 + 断线重连

---

> **面试建议：**
> - **一面**：重点准备 BIO/NIO 对比、Netty 线程模型、ByteBuf 基础使用
> - **二面**：重点准备粘包/半包解决方案、编码器解码器、内存管理
> - **三面**：重点准备源码分析（启动流程/EventLoop）、RPC 框架设计、Netty 参数优化
> - **手写代码**：ServerBootstrap 配置、LengthFieldBasedFrameDecoder 配置、自定义编解码器
