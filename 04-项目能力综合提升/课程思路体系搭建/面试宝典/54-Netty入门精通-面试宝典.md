# Netty入门精通 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — NIO基础、Netty核心API、粘包半包与协议设计、聊天室案例、性能优化与RPC手写实战

## 目录

1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

### 1.1 什么是Netty？
Netty是一个基于Java NIO的异步事件驱动网络应用框架，用于快速开发可维护的高性能协议服务器和客户端。它封装了JDK NIO的复杂性，提供了易于使用的API。

> 💡 **一句话总结**：Netty = NIO + Reactor模式 + 大量工程优化，是目前Java网络编程的事实标准。

### 1.2 BIO、NIO、AIO的区别

| 特性 | BIO (Blocking I/O) | NIO (Non-blocking I/O) | AIO (Asynchronous I/O) |
|------|-------------------|----------------------|----------------------|
| I/O模型 | 同步阻塞 | 同步非阻塞 | 异步非阻塞 |
| 线程模型 | 一个连接一个线程 | 多路复用，一个线程管理多个连接 | 回调机制，数据就绪通知 |
| 适用场景 | 连接数少且固定 | 连接数多、连接短 | 连接数多、连接长 |
| Java版本 | JDK 1.4之前 | JDK 1.4引入 | JDK 1.7引入 |
| 性能 | 低（线程开销大） | 高 | 高（但Netty未使用） |
| 编程复杂度 | 简单 | 中等 | 复杂 |

> ⚠️ Netty底层使用的是NIO多路复用，而非AIO。因为Linux的AIO（epoll的异步实现）并不成熟，NIO多路复用在实践中表现更优。

### 1.3 Reactor模型的三种变体

| 模型 | 描述 | 适用场景 |
|------|------|---------|
| 单Reactor单线程 | 一个线程处理accept、read、write所有事件 | Redis等简单场景 |
| 单Reactor多线程 | 一个线程处理accept，worker线程池处理业务 | 中小型应用 |
| 主从多Reactor | Boss Group处理accept，Worker Group处理IO读写 | 高并发场景（Netty默认） |

```java
// Netty默认的主从多Reactor模型配置示例
EventLoopGroup bossGroup = new NioEventLoopGroup(1);      // 处理accept
EventLoopGroup workerGroup = new NioEventLoopGroup();      // 处理IO读写，默认CPU*2

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

### 1.4 ChannelPipeline和ChannelHandler
- **ChannelPipeline**：Handler链的容器，负责事件的传播。是一个双向链表，包含入站Inbound和出站Outbound两种处理器。
- **ChannelHandler**：事件处理器，分为 `ChannelInboundHandler`（处理入站数据）和 `ChannelOutboundHandler`（处理出站数据）。
- Handler的顺序：Inbound按添加顺序正向执行，Outbound按添加顺序反向执行。

```java
// Pipeline演示
public class MyServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        // 入站handler — 按顺序1->2->3
        p.addLast("inbound1", new ChannelInboundHandlerAdapter());
        p.addLast("inbound2", new ChannelInboundHandlerAdapter());
        p.addLast("inbound3", new SimpleChannelInboundHandler<String>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                System.out.println("收到消息: " + msg);
                ctx.writeAndFlush("回复: " + msg);
            }
        });
        // 出站handler
        p.addLast("outbound1", new ChannelOutboundHandlerAdapter());
    }
}
```

### 1.5 ByteBuf的核心特性

| 特性 | 说明 |
|------|------|
| 堆内内存 (Heap) | 在JVM堆上分配，GC管理，速度快但需拷贝到内核 |
| 直接内存 (Direct) | 在堆外分配，减少拷贝，适合IO但分配/释放成本高 |
| 池化 (Pooled) | 内存复用，减少GC压力（Netty默认开启） |
| 非池化 (Unpooled) | 每次分配新内存，适合小数据量 |
| 读写指针 | `readerIndex` 和 `writerIndex` 分隔三个区域 |

```java
// ByteBuf操作示例
// 1. 创建
ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(256);

// 2. 写入
buf.writeBytes("Hello Netty".getBytes());
buf.writeInt(42);

// 3. 读取
int readable = buf.readableBytes();
byte[] data = new byte[readable];
buf.readBytes(data);

// 4. 零拷贝操作 — slice不复制数据
ByteBuf slice = buf.slice(0, 5);
slice.retain();

// 5. 组合ByteBuf
CompositeByteBuf comp = Unpooled.compositeBuffer();
comp.addComponents(true, buf1, buf2);

// 6. 释放
buf.release();
```

### 1.6 EventLoop和EventLoopGroup的关系
- **EventLoop** = 一个线程 + 一个Selector + 一个任务队列
- **EventLoopGroup** = EventLoop的池，负责管理多个EventLoop的创建和分配
- 每个Channel生命周期绑定到一个固定的EventLoop（**线程绑定**），保证线程安全

```java
// EventLoop定时任务
EventLoopGroup group = new NioEventLoopGroup(4);
EventLoop loop = group.next();

// 提交普通任务
loop.execute(() -> System.out.println("执行任务"));

// 提交定时任务
loop.schedule(() -> System.out.println("延迟任务"), 5, TimeUnit.SECONDS);
loop.scheduleAtFixedRate(() -> System.out.println("周期任务"), 0, 1, TimeUnit.SECONDS);
```

### 1.7 TCP粘包和拆包问题

**原因**：TCP是流式协议，消息没有边界。Nagle算法、缓冲区大小等因素导致多个小消息合并（粘包）或一个大消息拆分（拆包）。

**Netty提供的解决方式**：

| 解码器 | 工作原理 | 适用场景 |
|--------|---------|---------|
| `FixedLengthFrameDecoder` | 固定长度拆包 | 消息定长 |
| `LineBasedFrameDecoder` | 按换行符`\n`或`\r\n`拆分 | 文本协议 |
| `DelimiterBasedFrameDecoder` | 自定义分隔符拆分 | 自定义文本协议 |
| `LengthFieldBasedFrameDecoder` | 消息头中指定长度 | **最通用，推荐使用** |

```java
// 推荐方案：LengthFieldBasedFrameDecoder
ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
    1024,       // maxFrameLength
    0,          // lengthFieldOffset
    4,          // lengthFieldLength
    0,          // lengthAdjustment
    4           // initialBytesToStrip
));
ch.pipeline().addLast(new StringDecoder());
ch.pipeline().addLast(new MyBusinessHandler());
```

### 1.8 Future和Promise的区别

| 类型 | 特点 |
|------|------|
| JDK Future | 只读，需要阻塞获取结果（`future.get()`） |
| Netty Future | 继承JDK Future，增加`addListener`回调机制 |
| Netty Promise | 继承Netty Future，可写入结果（`setSuccess/setFailure`） |

```java
// Promise示例
Promise<String> promise = eventLoop.newPromise();
promise.addListener((FutureListener<String>) future -> {
    System.out.println("结果: " + future.getNow());
});

// 在另一个线程中完成
new Thread(() -> {
    String result = doHeavyWork();
    promise.setSuccess(result);
}).start();
```

### 1.9 @Sharable注解

```java
// @Sharable — 标记Handler可以被多个Channel共享使用
@Sharable
public class SharedHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 此Handler实例可以在多条pipeline中共享
    }
}
```

> ⚠️ @Sharable要求Handler必须是线程安全的，不能保存与具体Channel相关的状态。

### 1.10 EmbeddedChannel的使用

```java
// 单元测试ChannelHandler
EmbeddedChannel channel = new EmbeddedChannel(
    new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4),
    new StringDecoder(),
    new MyBusinessHandler()
);

// 模拟入站数据
ByteBuf buf = Unpooled.buffer();
buf.writeInt("hello".length());
buf.writeBytes("hello".getBytes());
assertTrue(channel.writeInbound(buf));

// 模拟出站数据
assertTrue(channel.writeOutbound(Unpooled.wrappedBuffer("response".getBytes())));
```

### 1.11 Netty服务端启动基本流程

```java
public class NettyServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new StringDecoder());
                     ch.pipeline().addLast(new ServerHandler());
                 }
             });
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("服务器启动成功，端口: 8080");
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

### 1.12 Netty客户端基本流程

```java
public class NettyClient {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new StringEncoder());
                     ch.pipeline().addLast(new ClientHandler());
                 }
             });
            ChannelFuture f = b.connect("localhost", 8080).sync();
            f.channel().writeAndFlush("Hello Server");
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
```

### 1.13 Netty编解码体系

| 编码器/解码器 | 方向 | 说明 |
|---------------|------|------|
| `ByteToMessageDecoder` | 入站 | 将ByteBuf解码为业务对象 |
| `MessageToByteEncoder` | 出站 | 将业务对象编码为ByteBuf |
| `ReplayingDecoder` | 入站 | 自动等待数据足够的ByteToMessageDecoder |
| `MessageToMessageDecoder` | 入站 | 将一种消息转换为另一种 |
| StringDecoder/StringEncoder | 双向 | 字符串编解码器 |

```java
// 自定义编解码器 — 消息格式: [长度(int)][消息内容(byte[])]
public class MessageCodec extends MessageToByteEncoder<String> {
    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) {
        byte[] bytes = msg.getBytes(CharsetUtil.UTF_8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}

public class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) return;
        int len = in.readInt();
        if (in.readableBytes() < len) return;
        byte[] bytes = new byte[len];
        in.readBytes(bytes);
        out.add(new String(bytes, CharsetUtil.UTF_8));
    }
}
```

### 1.14 Netty的心跳机制（IdleStateHandler）

```java
// 服务端心跳检测 — 5s读空闲、7s写空闲、10s全空闲时触发idle事件
p.addLast(new IdleStateHandler(5, 7, 10, TimeUnit.SECONDS));
p.addLast(new HeartbeatHandler());

public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            switch (((IdleStateEvent) evt).state()) {
                case READER_IDLE:
                    System.out.println("读空闲，关闭连接");
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    System.out.println("写空闲");
                    break;
                case ALL_IDLE:
                    ctx.writeAndFlush(Unpooled.wrappedBuffer("PING".getBytes()));
                    break;
            }
        }
    }
}
```

---

## 二、深度原理剖析

### 2.1 Netty为何比原生NIO简单？
1. **消灭模板代码**：Netty封装了Selector的select、wakeup、处理SelectionKey等繁琐操作
2. **异常处理**：自动处理连接断开、半包、异常传播
3. **内存管理**：提供池化ByteBuf和引用计数，避免OOM和内存泄漏
4. **线程模型**：Pipeline模型保证了Handler执行的线程安全
5. **零拷贝**：CompositeByteBuf、slice、FileRegion等实现零拷贝

### 2.2 ChannelPipeline的事件传播机制

```java
// 入站事件传播顺序（从Head -> Tail）
public class InboundA extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("InboundA");
        ctx.fireChannelRead(msg);  // 传播给下一个InboundHandler
    }
}

public class OutboundA extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        System.out.println("OutboundA");
        ctx.write(msg, promise);  // 传播给下一个OutboundHandler
    }
}
```

### 2.3 ByteBuf的引用计数机制
- **引用计数**：Netty使用引用计数（ReferenceCounted接口）管理ByteBuf生命周期
- `retain()`：引用计数+1
- `release()`：引用计数-1，减到0时释放内存
- **谁retain谁release**：接收消息的Handler如果要将消息传出它所在的方法，需要retain

```java
// 正确释放ByteBuf
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    try {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
    } finally {
        ReferenceCountUtil.release(msg);
    }
}
```

### 2.4 Netty的零拷贝实现
Netty中的零拷贝概念分为两种：
1. **操作系统级别的零拷贝**：使用FileRegion实现文件传输，底层调用`transferTo/transferFrom`
2. **Netty级别的零拷贝**：
   - `CompositeByteBuf`：将多个ByteBuf合并，避免内存复制
   - `Unpooled.wrappedBuffer`：包装现有字节数组，不复制
   - `ByteBuf.slice`：创建共享底层数组的切片
   - `ByteBuf.duplicate`：共享整个缓冲区

```java
// FileRegion — 文件传输零拷贝
RandomAccessFile file = new RandomAccessFile("data.txt", "r");
FileRegion region = new DefaultFileRegion(file.getChannel(), 0, file.length());
ctx.writeAndFlush(region);

// CompositeByteBuf — 组合零拷贝
CompositeByteBuf composite = Unpooled.compositeBuffer();
composite.addComponents(true, header, body);
```

### 2.5 Netty的线程模型优势

| 对比项 | 传统BIO | Netty |
|--------|--------|-------|
| 线程策略 | 一个连接一个线程 | 多个连接共享一个EventLoop |
| 线程切换 | 频繁 | 线程固定，上下文切换少 |
| 锁竞争 | 无锁但线程多 | 串行化无锁设计 |
| 内存占用 | 高（每个线程栈1MB+） | 低 |
| 连接数上限 | 取决于线程数 | 取决于操作系统FD限制 |

> 🎯 Netty核心优化：一个EventLoop处理多个Channel，Channel上的所有操作都在同一个线程上执行，避免了锁竞争。

### 2.6 NIO多路复用的核心原理
- **Selector**：Java NIO的事件选择器，底层通过操作系统的`select/poll/epoll`实现
- **SelectionKey**：表示Channel在Selector上注册的事件（OP_ACCEPT、OP_READ、OP_WRITE、OP_CONNECT）
- **select流程**：Selector监视注册的Channel -> Channel就绪事件 -> 遍历SelectionKeys处理事件

```java
// NIO Selector核心代码（Netty底层原理）
Selector selector = Selector.open();
channel.register(selector, SelectionKey.OP_ACCEPT);
while (true) {
    int ready = selector.select();
    Set<SelectionKey> keys = selector.selectedKeys();
    Iterator<SelectionKey> iter = keys.iterator();
    while (iter.hasNext()) {
        SelectionKey key = iter.next();
        iter.remove();
        if (key.isAcceptable()) {  /* 处理accept */ }
        else if (key.isReadable()) { /* 处理read */ }
    }
}
```

### 2.7 Select空轮询Bug及Netty的解决方案

**JDK NIO空轮询Bug**：在Linux下，`Selector.select()`在特定条件下会立即返回0，但没有任何IO事件就绪，导致CPU 100%。

**Netty的解决方案**：
1. 统计`select()`空轮询次数
2. 若超过阈值（默认512），重建Selector
3. 将旧Selector上的所有Channel迁移到新Selector

```java
// Netty自动检测空轮询
if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
    selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
    rebuildSelector();
    selectCnt = 0;
}
```

### 2.8 自定义协议的设计与解析

```java
// 协议格式: 魔数(4B) | 版本号(1B) | 序列化方式(1B) | 指令类型(1B) | 序列号(4B) | 长度(4B) | 内容(N)
public class ProtocolEncoder extends MessageToByteEncoder<MessageProtocol> {
    @Override
    protected void encode(ChannelHandlerContext ctx, MessageProtocol msg, ByteBuf out) {
        out.writeInt(msg.getMagicNumber());
        out.writeByte(msg.getVersion());
        out.writeByte(msg.getSerializer());
        out.writeByte(msg.getCommand());
        out.writeInt(msg.getSequenceId());
        out.writeInt(msg.getContent().length);
        out.writeBytes(msg.getContent());
    }
}

public class ProtocolDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 14) return;
        in.skipBytes(4);  // 魔数
        // ... 读取各字段
        int length = in.readInt();
        if (in.readableBytes() < length) return;
        byte[] content = new byte[length];
        in.readBytes(content);
        out.add(new MessageProtocol(/*...*/));
    }
}
```

---

## 三、实战场景题

### 3.1 如何用Netty实现一个简单的HTTP服务器？

```java
public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(65536));
        p.addLast(new HttpContentCompressor());
        p.addLast(new HttpServerHandler());
    }
}

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        String uri = req.uri();
        String content = "Hello, URI: " + uri;
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
            Unpooled.wrappedBuffer(content.getBytes()));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }
}
```

### 3.2 如何解决Netty中的内存泄漏问题？
1. **使用`ResourceLeakDetector`**：Netty默认检测1%的ByteBuf分配
2. **遵循retain/release配对**：谁用谁release，传递就要retain
3. **使用`SimpleChannelInboundHandler`**：它会自动释放消息
4. **启动参数**：`-Dio.netty.leakDetectionLevel=paranoid`开启全面检测

### 3.3 Netty在生产环境中的连接超时配置

```java
ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
 .channel(NioServerSocketChannel.class)
 .option(ChannelOption.SO_BACKLOG, 1024)
 .option(ChannelOption.SO_REUSEADDR, true)
 .childOption(ChannelOption.SO_KEEPALIVE, true)
 .childOption(ChannelOption.TCP_NODELAY, true)
 .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
 .childHandler(new ChannelInitializer<SocketChannel>() {
     @Override
     protected void initChannel(SocketChannel ch) {
         ch.pipeline().addLast(new IdleStateHandler(60, 30, 0));
     }
 });
```

---

## 四、手写代码题

### 4.1 使用Netty实现完整的Echo服务器

```java
public class EchoServer {
    private final int port;
    public EchoServer(int port) { this.port = port; }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new EchoServerHandler());
                 }
             });
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    public static void main(String[] args) throws Exception {
        new EchoServer(8080).start();
    }
}

class EchoServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        System.out.println("收到: " + in.toString(CharsetUtil.UTF_8));
        ctx.write(msg);
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) { ctx.flush(); }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

### 4.2 实现基于LengthField的编解码器

```java
// 请求消息
public class RequestMessage {
    private int messageId;
    private byte[] data;
    // constructor, getter, setter
}

// 编码器
public class RequestEncoder extends MessageToByteEncoder<RequestMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RequestMessage msg, ByteBuf out) {
        out.writeInt(msg.getMessageId());
        out.writeInt(msg.getData().length);
        out.writeBytes(msg.getData());
    }
}

// 解码器
public class RequestDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 8) return;
        in.markReaderIndex();
        int messageId = in.readInt();
        int bodyLength = in.readInt();
        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[bodyLength];
        in.readBytes(data);
        out.add(new RequestMessage(messageId, data));
    }
}
```

### 4.3 使用Future/Promise实现异步调用

```java
public class AsyncClient {
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final Map<Integer, Promise<String>> pending = new ConcurrentHashMap<>();
    private volatile Channel channel;
    private final AtomicInteger idGen = new AtomicInteger(0);

    public void connect(String host, int port) throws Exception {
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class)
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ch.pipeline().addLast(new ClientHandler(pending));
             }
         });
        channel = b.connect(host, port).sync().channel();
    }

    public Promise<String> send(String msg) {
        Promise<String> promise = group.next().newPromise();
        int msgId = idGen.incrementAndGet();
        pending.put(msgId, promise);
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(msgId);
        buf.writeBytes(msg.getBytes());
        channel.writeAndFlush(buf);
        return promise;
    }
}

class ClientHandler extends ChannelInboundHandlerAdapter {
    private final Map<Integer, Promise<String>> pending;
    ClientHandler(Map<Integer, Promise<String>> pending) { this.pending = pending; }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        int msgId = buf.readInt();
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        Promise<String> promise = pending.remove(msgId);
        if (promise != null) promise.setSuccess(new String(data));
    }
}
```

### 4.4 手写一个心跳检测处理器

```java
@Sharable
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    private static final ByteBuf HEARTBEAT_SEQUENCE =
            Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HEARTBEAT", CharsetUtil.UTF_8));

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                System.out.println("读空闲超时，关闭连接");
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate());
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        if (buf.toString(CharsetUtil.UTF_8).equals("HEARTBEAT")) {
            return;
        }
        ctx.fireChannelRead(msg);
    }
}
```

### 4.5 多客户端连接管理

```java
public class ChannelManager {
    private static final Map<String, Channel> channels = new ConcurrentHashMap<>();

    public static void addChannel(String userId, Channel channel) {
        channels.put(userId, channel);
        channel.closeFuture().addListener(future -> channels.remove(userId));
    }

    public static void sendToUser(String userId, String msg) {
        Channel ch = channels.get(userId);
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
        }
    }

    public static void broadcast(String msg) {
        ByteBuf buf = Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8);
        channels.values().forEach(ch -> {
            if (ch.isActive()) ch.writeAndFlush(buf.retainedDuplicate());
        });
    }
}
```

---

## 五、系统设计题

### 5.1 设计一个百万连接的消息推送系统
1. **网络层**：Netty主从多Reactor + NioEventLoopGroup，每个EventLoop管理多个Channel
2. **内存层**：PooledDirectByteBuf减少GC，合理设置高低水位线
3. **业务层**：生产者-消费者模式，使用Disruptor或RingBuffer加速
4. **背压机制**：通过`Channel.isWritable()`和`channelWritabilityChanged`实现

### 5.2 设计一个支持单聊和群聊的IM系统
1. **协议设计**：魔数+版本+指令类型+序列化方式+序列号+长度+内容
2. **Channel管理**：ConcurrentHashMap维护用户ID到Channel的映射
3. **群组管理**：Map<groupId, Set<Channel>>，群聊消息遍历广播
4. **离线消息**：消息持久化到Redis，用户上线后拉取
5. **顺序保证**：每个连接绑定一个EventLoop，保证单个连接消息顺序

### 5.3 如何设计一个基于Netty的RPC框架
1. **服务注册**：服务端将接口名->实现类的映射注册到本地Map
2. **协议定义**：接口名+方法名+参数类型+参数值 -> 编码发送
3. **动态代理**：客户端通过JDK动态代理生成接口代理，调用时封装请求
4. **请求响应关联**：每个请求分配唯一ID，通过Promise异步等待响应
5. **序列化**：支持JSON/Protobuf/Kryo等可扩展序列化方式

---

## 六、常见坑点与最佳实践

| 坑点 | 原因 | 解决方案 |
|------|------|---------|
| ByteBuf内存泄漏 | `release()`未调用或引用计数不平衡 | 使用`SimpleChannelInboundHandler`自动释放；开启`ResourceLeakDetector` |
| Handler中阻塞操作 | 在EventLoop中执行耗时业务，阻塞IO事件 | 使用`DefaultEventExecutorGroup`添加业务线程池 |
| @Sharable使用错误 | Handler中有非线程安全的状态变量 | 只对无状态Handler使用@Sharable |
| 粘包/半包问题 | TCP流式协议无边界 | 使用`LengthFieldBasedFrameDecoder`解码器 |
| 写缓冲区OOM | 写入速度远大于网络发送速度 | 调用`channel.config().setWriteBufferHighWaterMark()`设置水位线 |
| ChannelHandlerContext泄漏 | 保存ctx引用到其他线程，产生并发安全问题 | 不要缓存ctx给其他线程使用 |
| EventLoop里加锁 | 单个EventLoop本身线程安全 | 避免在Handler中使用synchronized |
| Pipeline中Handler顺序错误 | Inbound/Outbound执行顺序不清晰 | Inbound正向执行，Outbound反向执行 |
| 客户端未处理连接超时 | 没有设置CONNECT_TIMEOUT_MILLIS | `.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)` |
| 关闭时未优雅退出 | 直接System.exit导致正在处理的消息丢失 | 调用`shutdownGracefully()`等待任务完成 |

---

## 七、面试回答模板

### 7.1 "谈谈你对Netty的理解"
> Netty是一个基于NIO的异步事件驱动网络框架。它对JDK NIO做了封装，提供了Reactor线程模型、Pipeline责任链模式、池化内存管理等特性。我在项目中使用Netty实现了消息推送服务，通过主从多Reactor模型处理高并发连接，使用LengthFieldBasedFrameDecoder解决粘包问题，结合IdleStateHandler实现心跳检测。与直接使用JDK NIO相比，Netty让我们专注于业务Handler而不用处理底层Selector的复杂逻辑。

### 7.2 "TCP粘包半包问题及解决方案"
> TCP是流式协议，消息没有边界，加上Nagle算法和缓冲区大小的影响，会导致粘包和半包。Netty提供了四种解码器：FixedLengthFrameDecoder用于定长协议，LineBasedFrameDecoder用于换行符分隔的文本协议，DelimiterBasedFrameDecoder用于自定义分隔符，而LengthFieldBasedFrameDecoder是最通用的方案——在消息头中携带长度信息。推荐使用LengthFieldBasedFrameDecoder，适用于绝大多数自定义协议场景。

### 7.3 "Netty的零拷贝如何理解"
> Netty的零拷贝有两个层面。第一是操作系统层面，通过FileRegion调用transferTo方法，数据直接从内核缓冲区传输到网卡，不经过用户态。第二是Netty框架层面，包括：CompositeByteBuf将多个ByteBuf虚拟合并；slice操作共享底层数组而不是复制；wrappedBuffer包装已有数组零拷贝。这些都是通过引用计数+共享指针实现的，避免了不必要的内存复制。

### 7.4 "Netty线程模型是怎样的"
> Netty默认使用主从多Reactor模型。Boss EventLoopGroup负责处理客户端连接（accept事件），Worker EventLoopGroup负责处理每个连接的IO读写。每个EventLoop内部是一个单线程的Selector循环，处理注册在其上的所有Channel的IO事件和任务。关键设计是：一个Channel的生命周期绑定到一个固定的EventLoop上，所以Channel上的所有操作都是单线程执行的，无需加锁。

### 7.5 "Netty的内存管理机制"
> Netty的内存管理参考了jemalloc的设计思路。ByteBuf分为堆内存和直接内存，直接内存减少了内核和用户态的拷贝。Netty默认使用池化内存（PooledByteBufAllocator），将内存分成多个chunk、page、subpage级别，通过伙伴算法和缓存来减少GC压力和内存碎片。引用计数机制配合ResourceLeakDetector防止内存泄漏。建议生产环境开启PooledDirectByteBuf，通过`-Dio.netty.leakDetectionLevel=paranoid`在测试阶段检测泄漏。

---

## 八、快速查漏补缺Checklist

- [ ] 能够完整搭建一个Netty Server和Client
- [ ] 理解BIO/NIO/AIO三种IO模型的区别
- [ ] 掌握Reactor模型三种变体（单线程/多线程/主从多Reactor）
- [ ] 能解释ChannelPipeline的事件传播机制
- [ ] 掌握ByteBuf的heap/direct、pooled/unpooled区别
- [ ] 了解ByteBuf的读写指针和引用计数机制
- [ ] 熟悉四种粘包半包解码器及其使用场景
- [ ] 能手写LengthFieldBasedFrameDecoder的使用
- [ ] 能手写自定义协议编解码器
- [ ] 掌握EventLoop/EventLoopGroup的设计
- [ ] 了解Netty的零拷贝实现方式
- [ ] 理解Future/Promise的异步回调机制
- [ ] 能实现心跳检测（IdleStateHandler）
- [ ] 能使用EmbeddedChannel进行单元测试
- [ ] 熟悉@Sharable注解的使用条件和线程安全要求
- [ ] 掌握Netty的启动参数（SO_BACKLOG、TCP_NODELAY、SO_KEEPALIVE）
- [ ] 能使用ChannelFuture添加回调监听器
- [ ] 理解Netty内存泄漏的检测和解决方式
- [ ] 能实现简单的HTTP服务器
- [ ] 了解Netty在RPC框架和IM系统中的应用
