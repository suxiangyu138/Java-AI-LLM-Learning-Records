Netty 的用途
Netty 是一款基于 Java NIO 的异步、事件驱动的高性能网络通信框架，主要用于快速开发高并发、高可靠的网络应用，核心用途包括：
1. 构建高性能服务端/客户端，如 RPC 框架（Dubbo）、消息中间件（RocketMQ）、分布式通信系统。
2. 处理海量并发连接，适合游戏服务器、物联网（IoT）通信、即时通讯（IM）等场景。
3. 简化 Java NIO 开发的复杂度，封装了底层的 ByteBuffer、Selector 等 API，避免原生 NIO 的坑（如空轮询、缓冲区管理）。
    Netty 的基础使用方法（中文版 IntelliJ IDEA 操作）
    步骤 1：创建 Maven 项目并引入依赖
    1. 打开 IntelliJ IDEA → 点击文件 → 新建 → 项目 → 选择 Maven → 点击下一步 → 填写项目名称和路径 → 完成。
    2. 打开  pom.xml ，添加 Netty 核心依赖（以 4.1.x 版本为例）：
    xml
    <dependencies>
    <!-- Netty 核心依赖 -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.94.Final</version>
    </dependency>
    </dependencies>
 
3. 点击右侧 Maven → 刷新，自动下载依赖。
    步骤 2：编写服务端代码
    创建  NettyServer.java  类，实现一个简单的 echo 服务器（接收客户端消息并原样返回）：
    java
    import io.netty.bootstrap.ServerBootstrap;
    import io.netty.channel.*;
    import io.netty.channel.nio.NioEventLoopGroup;
    import io.netty.channel.socket.SocketChannel;
    import io.netty.channel.socket.nio.NioServerSocketChannel;
    import io.netty.handler.codec.string.StringDecoder;
    import io.netty.handler.codec.string.StringEncoder;
    public class NettyServer {
    // 端口号
    private static final int PORT = 8888;
    public static void main(String[] args) throws InterruptedException {
        // 1. 创建两个事件循环组
        // bossGroup：处理客户端连接请求
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // workerGroup：处理客户端的读写操作
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 2. 创建服务端启动助手
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup) // 设置两个事件循环组
                    .channel(NioServerSocketChannel.class) // 设置服务端通道实现类
                    .option(ChannelOption.SO_BACKLOG, 128) // 设置连接队列长度
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置保持连接
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 设置通道初始化器
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 3. 向通道流水线添加处理器
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new StringDecoder()); // 字符串解码器
                            pipeline.addLast(new StringEncoder()); // 字符串编码器
                            pipeline.addLast(new NettyServerHandler()); // 自定义业务处理器
                        }
                    });
            System.out.println("Netty 服务端启动中...");
            // 4. 绑定端口并同步，启动服务
            ChannelFuture channelFuture = serverBootstrap.bind(PORT).sync();
            // 5. 监听通道关闭事件
            channelFuture.channel().closeFuture().sync();
        } finally {
            // 6. 优雅关闭事件循环组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    // 自定义业务处理器
    static class NettyServerHandler extends SimpleChannelInboundHandler<String> {
        // 接收客户端消息时触发
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println("收到客户端消息：" + msg);
            // 向客户端发送消息
            ctx.writeAndFlush("服务端已收到：" + msg);
        }
    }
    }
 
步骤 3：编写客户端代码
创建  NettyClient.java  类，实现客户端与服务端通信：
java
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
public class NettyClient {
    public static void main(String[] args) throws InterruptedException {
        // 1. 创建事件循环组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 2. 创建客户端启动助手
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group) // 设置事件循环组
                    .channel(NioSocketChannel.class) // 设置客户端通道实现类
                    .handler(new ChannelInitializer<SocketChannel>() { // 设置通道初始化器
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 3. 向通道流水线添加处理器
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new NettyClientHandler()); // 自定义客户端处理器
                        }
                    });
            System.out.println("Netty 客户端启动中...");
            // 4. 连接服务端
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8888).sync();
            // 5. 获取通道并发送消息
            Channel channel = channelFuture.channel();
            channel.writeAndFlush("你好，Netty 服务端！");
            // 6. 监听通道关闭事件
            channel.closeFuture().sync();
        } finally {
            // 7. 优雅关闭事件循环组
            group.shutdownGracefully();
        }
    }
    // 自定义客户端处理器
    static class NettyClientHandler extends SimpleChannelInboundHandler<String> {
        // 接收服务端消息时触发
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println("收到服务端消息：" + msg);
        }
    }
}
 
步骤 4：运行测试
1. 先运行  NettyServer.java ，控制台输出  Netty 服务端启动中... 。
2. 再运行  NettyClient.java ，客户端控制台输出  收到服务端消息：服务端已收到：你好，Netty 服务端！ ，服务端控制台输出  收到客户端消息：你好，Netty 服务端！ 。
    核心关键点总结
    1. EventLoopGroup：相当于线程池， bossGroup  负责接受连接， workerGroup  负责处理读写。
    2. ChannelPipeline：处理器链，消息会依次经过所有处理器的  handler  方法。
    3. 编解码器：Netty 提供多种编解码器（如  StringDecoder / StringEncoder ），用于解决 TCP 粘包/拆包问题。
    4. 异步非阻塞：Netty 基于 NIO 实现，所有 I/O 操作都是异步的，通过  ChannelFuture  监听操作结果。
 
