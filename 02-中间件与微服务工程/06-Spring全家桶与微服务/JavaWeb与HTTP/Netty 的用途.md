# Netty 的用途与基础使用方法

> **文档定位**：Java 后端技术参考文档 | Netty 高性能网络通信框架  
> **核心定义**：Netty 是一款基于 Java NIO 的异步、事件驱动的高性能网络通信框架  
> **核心用途**：快速开发高并发、高可靠的网络应用

---

## 目录

- [一、Netty 核心用途](#一netty-核心用途)
- [二、环境准备](#二环境准备)
- [三、服务端代码](#三服务端代码)
- [四、客户端代码](#四客户端代码)
- [五、运行测试](#五运行测试)
- [六、核心关键点总结](#六核心关键点总结)

---

## 一、Netty 核心用途

| 用途 | 典型场景 |
|------|----------|
| **构建高性能服务端/客户端** | RPC 框架（Dubbo）、消息中间件（RocketMQ）、分布式通信系统 |
| **处理海量并发连接** | 游戏服务器、物联网（IoT）通信、即时通讯（IM） |
| **简化 Java NIO 开发** | 封装底层 ByteBuffer、Selector 等 API，避免原生 NIO 的坑（空轮询、缓冲区管理） |

---

## 二、环境准备

### Maven 依赖

```xml
<dependencies>
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.94.Final</version>
    </dependency>
</dependencies>
```

---

## 三、服务端代码

实现一个 Echo 服务器（接收客户端消息并原样返回）。

```java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NettyServer {
    private static final int PORT = 8888;

    public static void main(String[] args) throws InterruptedException {
        // bossGroup：处理客户端连接请求
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // workerGroup：处理客户端的读写操作
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new NettyServerHandler());
                        }
                    });

            System.out.println("Netty 服务端启动中...");
            ChannelFuture channelFuture = serverBootstrap.bind(PORT).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 自定义业务处理器
    static class NettyServerHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            System.out.println("收到客户端消息：" + msg);
            ctx.writeAndFlush("服务端已收到：" + msg);
        }
    }
}
```

---

## 四、客户端代码

```java
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NettyClient {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new NettyClientHandler());
                        }
                    });

            System.out.println("Netty 客户端启动中...");
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8888).sync();
            Channel channel = channelFuture.channel();
            channel.writeAndFlush("你好，Netty 服务端！");
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    // 自定义客户端处理器
    static class NettyClientHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            System.out.println("收到服务端消息：" + msg);
        }
    }
}
```

---

## 五、运行测试

| 步骤 | 操作 | 预期输出 |
|------|------|----------|
| 1 | 运行 `NettyServer` | `Netty 服务端启动中...` |
| 2 | 运行 `NettyClient` | 客户端：`收到服务端消息：服务端已收到：你好，Netty 服务端！` |
| — | — | 服务端：`收到客户端消息：你好，Netty 服务端！` |

---

## 六、核心关键点总结

| 核心概念 | 说明 |
|----------|------|
| **EventLoopGroup** | 相当于线程池，`bossGroup` 负责接受连接，`workerGroup` 负责处理读写 |
| **ChannelPipeline** | 处理器链（责任链模式），消息依次经过所有处理器的 `handler` 方法 |
| **编解码器** | `StringDecoder` / `StringEncoder` 等，解决 TCP 粘包/拆包问题 |
| **异步非阻塞** | 基于 NIO 实现，所有 I/O 操作都是异步的，通过 `ChannelFuture` 监听结果 |
| **ChannelInitializer** | 初始化通道，设置处理器链 |
