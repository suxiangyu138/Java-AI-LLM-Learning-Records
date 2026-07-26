# 02 - 第一个 Netty 应用

> 🎯 跑通第一个 Server ↔ Client 通信，理解 Bootstrap 启动流程和核心组件 — 代码是最好的老师

---

## 目录

1. [Netty Server 实现](#1-netty-server-实现)
2. [Netty Client 实现](#2-netty-client-实现)
3. [Bootstrap 启动流程详解](#3-bootstrap-启动流程详解)
4. [ChannelOption 与参数调优](#4-channeloption-与参数调优)

---

## 1. Netty Server 实现

```java
public class NettyServer {

    public static void main(String[] args) throws InterruptedException {
        // 1. 创建 Boss 和 Worker 线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 2. 创建服务端启动器
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)    // NIO 模式
                     .option(ChannelOption.SO_BACKLOG, 128)   // TCP 连接队列
                     .childOption(ChannelOption.SO_KEEPALIVE, true)
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             ch.pipeline()
                               .addLast(new StringDecoder())       // ByteBuf → String
                               .addLast(new StringEncoder())       // String → ByteBuf
                               .addLast(new ServerHandler());      // 业务 Handler
                         }
                     });

            // 3. 绑定端口，同步等待成功
            ChannelFuture future = bootstrap.bind(8080).sync();
            System.out.println("Server started on port 8080");

            // 4. 监听通道关闭
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

// 业务 Handler
class ServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("收到: " + msg);
        ctx.writeAndFlush("Server 回复: " + msg);   // 回写
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

| 组件 | 说明 |
|------|------|
| `NioEventLoopGroup` | Boss 接收连接，Worker 处理 IO |
| `ServerBootstrap` | 服务端启动引导类 |
| `NioServerSocketChannel` | NIO 模式的服务端 Channel |
| `ChannelInitializer` | 初始化每个新连接的 ChannelPipeline |
| `ChannelFuture` | 异步操作结果（绑定/连接/写入） |

---

## 2. Netty Client 实现

```java
public class NettyClient {

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();          // 客户端用 Bootstrap
            bootstrap.group(group)
                     .channel(NioSocketChannel.class)
                     .handler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             ch.pipeline()
                               .addLast(new StringDecoder())
                               .addLast(new StringEncoder())
                               .addLast(new ClientHandler());
                         }
                     });

            // 连接服务器（异步）
            ChannelFuture future = bootstrap.connect("127.0.0.1", 8080).sync();
            Channel channel = future.channel();

            // 发送消息
            channel.writeAndFlush("Hello Netty!");

            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class ClientHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("收到响应: " + msg);
    }
}
```

### ServerBootstrap vs Bootstrap

| 维度 | ServerBootstrap | Bootstrap |
|------|:---:|:---:|
| 用途 | 服务端 | 客户端 |
| EventLoopGroup | Boss + Worker（两个） | 一个 |
| Channel | `NioServerSocketChannel` | `NioSocketChannel` |
| 绑定 | `bind(port)` | `connect(host, port)` |

---

## 3. Bootstrap 启动流程详解

```text
ServerBootstrap 启动流程：

1. group(boss, worker)        → 创建线程模型
2. channel(NioServerSocket... ) → 设置服务端 Channel 类型
3. option(SO_BACKLOG, 128)    → 服务端 Channel 参数
4. childOption(KEEPALIVE, ... ) → 客户端 Channel 参数
5. childHandler(initializer)  → 客户端 Channel 的 Pipeline
6. bind(8080)                 → 绑定端口
     │
     ├── init: 创建 NioServerSocketChannel（内部）
     ├── register: 注册到 Boss EventLoop 的 Selector
     └── doBind: 调用 JDK 底层绑定端口

新连接到达时：
7. Boss EventLoop accept 新连接 → 创建 NioSocketChannel
8. 注册到 Worker EventLoop 的 Selector
9. 触发 ChannelInitializer → 初始化 Pipeline
```

---

## 4. ChannelOption 与参数调优

### 服务端 ChannelOption

| 参数 | 说明 | 推荐值 |
|------|------|:---:|
| `SO_BACKLOG` | TCP 全连接队列大小 | 128-1024 |
| `SO_REUSEADDR` | 允许端口复用（快速重启） | true |
| `SO_RCVBUF` | 接收缓冲区 | 可设大（如 128KB） |

### 客户端 ChannelOption

| 参数 | 说明 | 推荐值 |
|------|------|:---:|
| `SO_KEEPALIVE` | TCP Keep-Alive（检测死连接） | true |
| `TCP_NODELAY` | 禁用 Nagle 算法（低延迟） | true |
| `SO_SNDBUF` | 发送缓冲区 | 可设大 |
| `CONNECT_TIMEOUT_MILLIS` | 连接超时 | 3000-10000 |

```java
ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.option(ChannelOption.SO_BACKLOG, 1024)
         .option(ChannelOption.SO_REUSEADDR, true)
         .childOption(ChannelOption.SO_KEEPALIVE, true)
         .childOption(ChannelOption.TCP_NODELAY, true)
         .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
```

> 🎯 `TCP_NODELAY=true` 至关重要 — 禁用 Nagle 算法后数据立即发送（低延迟），适合 RPC/即时通信；文件传输场景可设 false 利用 Nagle 批量优化。
