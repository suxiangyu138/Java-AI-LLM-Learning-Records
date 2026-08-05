# 03 - Channel 与 EventLoop

> 🎯 Channel 是连接的抽象、EventLoop 是执行的线程 — Channel 与 EventLoop 的绑定关系，以及 EventLoopGroup 的线程模型，是理解 Netty 高性能的关键

---

## 目录

1. [Channel 生命周期](#1-channel-生命周期)
2. [EventLoop 与 EventLoopGroup](#2-eventloop-与-eventloopgroup)
3. [线程绑定模型](#3-线程绑定模型)
4. [ChannelFuture 异步编程](#4-channelfuture-异步编程)
5. [Channel 类型](#5-channel-类型)

---

## 1. Channel 生命周期

```text
Channel 状态转换：

  [创建] → ChannelRegistered → ChannelActive → [读写数据] → ChannelInactive → ChannelUnregistered → [关闭]

每个状态变化触发对应的 ChannelInboundHandler 方法：
  channelRegistered()    → 注册到 EventLoop
  channelActive()        → 连接建立/端口绑定
  channelRead()          → 收到数据
  channelReadComplete()  → 读完成
  channelInactive()      → 连接断开
  channelUnregistered()  → 从 EventLoop 注销
```

```java
public class LifecycleHandler extends ChannelInboundHandlerAdapter {
    @Override public void channelRegistered(ChannelHandlerContext ctx)   { /* 注册 */ }
    @Override public void channelActive(ChannelHandlerContext ctx)       { /* 激活 */ }
    @Override public void channelRead(ChannelHandlerContext ctx, Object m) { /* 读数据 */ }
    @Override public void channelInactive(ChannelHandlerContext ctx)     { /* 断开 */ }
    @Override public void channelUnregistered(ChannelHandlerContext ctx) { /* 注销 */ }
}
```

---

## 2. EventLoop 与 EventLoopGroup

### 2.1 继承关系

```text
java.util.concurrent.ScheduledExecutorService
    └── io.netty.util.concurrent.EventExecutorGroup
            └── io.netty.channel.EventLoopGroup
                    └── io.netty.channel.nio.NioEventLoopGroup
                            └── NioEventLoop (实际执行者)

每个 NioEventLoop 内部持有：
  ├── 一个 Selector（多路复用器）
  ├── 一个任务队列（TaskQueue）
  └── 一个线程（Thread，绑定不换）
```

### 2.2 核心职责

```text
NioEventLoop 的 run() 循环：

while (true) {
    1. selector.select()          // 阻塞等待 IO 事件
    2. processSelectedKeys()      // 处理 IO 事件（read/write/accept）
    3. runAllTasks()              // 处理任务队列中的异步任务
}
```

| 职责 | 说明 |
|------|------|
| IO 事件处理 | 轮询 Selector，处理 accept/read/write |
| 异步任务 | 执行 `channel.writeAndFlush()` 提交的写任务 |
| 定时任务 | 执行 `schedule()` 提交的延迟任务（如心跳） |

---

## 3. 线程绑定模型

> ⚠️ **核心规则**：一个 Channel 从注册到销毁，始终绑定同一个 EventLoop 线程 — 无锁化设计。

```text
Boss EventLoopGroup(1)          Worker EventLoopGroup(4)
┌──────────────┐               ┌──────────────┐
│ EventLoop-1  │──accept──→    │ EventLoop-2  │ ← Channel-A (永久绑定)
│ (Selector)   │──accept──→    │ EventLoop-3  │ ← Channel-B (永久绑定)
└──────────────┘               │ EventLoop-4  │ ← Channel-C (永久绑定)
                               │ EventLoop-5  │ ← Channel-D (永久绑定)
                               └──────────────┘

为什么这样设计？
  → Channel 的所有 IO 操作都在同一个线程
  → 无需同步（synchronized），性能极高
  → 千万不要在 Handler 中执行耗时操作（会阻塞 EventLoop）
```

### 3.1 耗时操作的处理

```java
// ❌ 错误：在 EventLoop 线程中执行耗时操作
public class BadHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String result = heavyDatabaseQuery(msg);  // 阻塞 EventLoop！
        ctx.writeAndFlush(result);
    }
}

// ✅ 正确：提交到业务线程池
public class GoodHandler extends ChannelInboundHandlerAdapter {
    private final ExecutorService bizPool = Executors.newFixedThreadPool(64);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        bizPool.submit(() -> {
            String result = heavyDatabaseQuery(msg);
            ctx.writeAndFlush(result);     // writeAndFlush 是线程安全的
        });
    }
}

// ✅ 也可以用 Netty 提供的 DefaultEventExecutorGroup
pipeline.addLast(new DefaultEventExecutorGroup(16), new GoodHandler());
```

---

## 4. ChannelFuture 异步编程

> Netty 中所有 IO 操作都是异步的，返回 ChannelFuture。

```java
// ═══ 方式1：sync() 同步等待 ═══
ChannelFuture future = bootstrap.connect("127.0.0.1", 8080).sync();
if (future.isSuccess()) { /* 连接成功 */ }

// ═══ 方式2：addListener() 异步回调（⭐ 推荐） ═══
ChannelFuture future = bootstrap.connect("127.0.0.1", 8080);
future.addListener(f -> {
    if (f.isSuccess()) {
        System.out.println("连接成功");
    } else {
        System.out.println("连接失败: " + f.cause());
    }
});

// ═══ 方式3：Promise（可写的 Future） ═══
Promise<String> promise = new DefaultPromise<>(eventLoop);
promise.addListener(f -> {
    if (f.isSuccess()) { System.out.println("结果: " + f.getNow()); }
});
// 某个地方填充结果
promise.setSuccess("Hello");
```

| 方法 | 说明 |
|------|------|
| `sync()` | 阻塞等待完成，失败抛异常 |
| `await()` | 阻塞等待，不抛异常 |
| `addListener()` | 异步回调，不阻塞 |
| `isSuccess()` | 是否成功 |
| `cause()` | 失败原因 |

---

## 5. Channel 类型

| Channel | 传输方式 | 用途 |
|---------|:---:|------|
| `NioServerSocketChannel` | NIO TCP | 服务端接收连接 |
| `NioSocketChannel` | NIO TCP | 客户端/服务端数据读写 |
| `EpollServerSocketChannel` | Epoll | Linux 高性能（需 `netty-transport-native-epoll`） |
| `EpollSocketChannel` | Epoll | Linux 高性能 |
| `OioServerSocketChannel` | BIO | 兼容旧代码 |
| `LocalServerChannel` | 本地 JVM | 同 JVM 内通信（不走网络） |
| `EmbeddedChannel` | 测试 | 单元测试 ChannelHandler |

```xml
<!-- Linux Epoll 高性能传输 -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-native-epoll</artifactId>
    <classifier>linux-x86_64</classifier>
</dependency>
```

> 🎯 **核心理解**：Channel = 网络连接抽象，EventLoop = 执行线程，两者永久绑定 → 无锁 → 高性能。耗时逻辑务必异步到业务线程池。
