Netty 安装配置 + 企业级项目实战（高并发IM聊天系统）
一、Netty 环境安装与配置（从零开始）
1. 安装 JDK（1.8+）
    - 下载地址：https://www.oracle.com/java/technologies/downloads/
    - 配置环境变量：
    plaintext
    JAVA_HOME=D:\jdk1.8.0_301
    PATH=%JAVA_HOME%\bin
 
- 验证： java -version 
    2. 创建 Maven 项目
- IDEA → New → Project → Maven
- 项目名： netty-im 
    3. 引入 Netty 依赖（pom.xml）
    xml
    <dependencies>
    <!-- Netty 核心依赖 -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.100.Final</version>
    </dependency>
    <!-- Lombok 简化代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>
    </dependencies>
 
4. Netty 核心概念（必懂）
    - BossGroup：接收客户端连接
    - WorkerGroup：处理读写事件
    - Channel：网络连接通道
    - Pipeline：责任链（处理器队列）
    - Handler：业务处理器
 
二、企业级项目：高并发即时通讯（IM）系统
项目简介
基于 Netty 实现的分布式、高并发、可扩展即时通讯系统，支持：
- 单聊、群聊
- 心跳检测、断线重连
- 消息持久化
- 在线状态管理
- 分布式部署
    项目结构
    plaintext
    netty-im/
    ├── src/main/java/com/im
    │   ├── server/          # 服务端
    │   │   ├── ImServer.java
    │   │   ├── ImServerInitializer.java
    │   │   └── ImServerHandler.java
    │   ├── client/          # 客户端
    │   │   ├── ImClient.java
    │   │   ├── ImClientInitializer.java
    │   │   └── ImClientHandler.java
    │   ├── protocol/        # 消息协议
    │   │   └── MessageProtocol.java
    │   └── constant/        # 常量
    │       └── ImConstant.java
    └── pom.xml
 
 
三、消息协议设计（企业级标准）
MessageProtocol.java
java
import lombok.Data;
/**
 * 消息协议
     */
    @Data
    public class MessageProtocol {
    /**
     * 消息类型：1-登录 2-单聊 3-群聊 4-心跳
     */
    private int type;
    /**
     * 发送者ID
     */
    private String fromId;
    /**
     * 接收者ID
     */
    private String toId;
    /**
     * 消息内容
     */
    private String content;
    }
 
ImConstant.java
java
/**
 * 常量
     */
    public class ImConstant {
    /**
     * 服务端端口
     */
    public static final int PORT = 8888;
    /**
     * 心跳间隔（秒）
     */
    public static final int HEARTBEAT_INTERVAL = 30;
    }
 
 
四、Netty 服务端实现
1. ImServer.java（启动类）
    java
    import io.netty.bootstrap.ServerBootstrap;
    import io.netty.channel.ChannelFuture;
    import io.netty.channel.ChannelOption;
    import io.netty.channel.nio.NioEventLoopGroup;
    import io.netty.channel.socket.nio.NioServerSocketChannel;
    /**
     * IM 服务端
     */
    public class ImServer {
    public static void main(String[] args) {
        // 主线程组：接收连接
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 工作线程组：处理读写
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ImServerInitializer());
            ChannelFuture future = bootstrap.bind(ImConstant.PORT).sync();
            System.out.println("IM 服务端启动，端口：" + ImConstant.PORT);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    }
 
2. ImServerInitializer.java（通道初始化）
    java
    import io.netty.channel.ChannelInitializer;
    import io.netty.channel.socket.SocketChannel;
    import io.netty.handler.codec.serialization.ClassResolvers;
    import io.netty.handler.codec.serialization.ObjectDecoder;
    import io.netty.handler.codec.serialization.ObjectEncoder;
    import io.netty.handler.timeout.IdleStateHandler;
    /**
     * 服务端通道初始化
     */
    public class ImServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                // 心跳检测：30秒无读则断开
                .addLast(new IdleStateHandler(30, 0, 0))
                // 对象解码器
                .addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)))
                // 对象编码器
                .addLast(new ObjectEncoder())
                // 业务处理器
                .addLast(new ImServerHandler());
    }
    }
 
3. ImServerHandler.java（业务处理）
    java
    import io.netty.channel.Channel;
    import io.netty.channel.ChannelHandlerContext;
    import io.netty.channel.SimpleChannelInboundHandler;
    import io.netty.handler.timeout.IdleState;
    import io.netty.handler.timeout.IdleStateEvent;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;
    /**
     * 服务端业务处理器
     */
    public class ImServerHandler extends SimpleChannelInboundHandler<MessageProtocol> {
    /**
     * 在线用户通道（线程安全）
     */
    private static final Map<String, Channel> ONLINE_USERS = new ConcurrentHashMap<>();
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) {
        switch (msg.getType()) {
            case 1: // 登录
                handleLogin(ctx, msg);
                break;
            case 2: // 单聊
                handleSingleChat(msg);
                break;
            case 4: // 心跳
                handleHeartbeat(ctx);
                break;
            default:
                break;
        }
    }
    /**
     * 登录处理
     */
    private void handleLogin(ChannelHandlerContext ctx, MessageProtocol msg) {
        String userId = msg.getFromId();
        ONLINE_USERS.put(userId, ctx.channel());
        System.out.println("用户登录：" + userId + "，在线人数：" + ONLINE_USERS.size());
    }
    /**
     * 单聊处理
     */
    private void handleSingleChat(MessageProtocol msg) {
        String toId = msg.getToId();
        Channel channel = ONLINE_USERS.get(toId);
        if (channel != null) {
            channel.writeAndFlush(msg);
        } else {
            System.out.println("用户 " + toId + " 不在线，消息暂存");
        }
    }
    /**
     * 心跳处理
     */
    private void handleHeartbeat(ChannelHandlerContext ctx) {
        System.out.println("收到心跳：" + ctx.channel().remoteAddress());
    }
    /**
     * 心跳超时处理
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                ctx.close();
                System.out.println("心跳超时，断开连接：" + ctx.channel().remoteAddress());
            }
        }
    }
    /**
     * 断开连接处理
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ONLINE_USERS.values().remove(ctx.channel());
        System.out.println("用户下线，在线人数：" + ONLINE_USERS.size());
    }
    }
 
 
五、Netty 客户端实现
1. ImClient.java（启动类）
    java
    import io.netty.bootstrap.Bootstrap;
    import io.netty.channel.Channel;
    import io.netty.channel.ChannelFuture;
    import io.netty.channel.nio.NioEventLoopGroup;
    import io.netty.channel.socket.nio.NioSocketChannel;
    import java.util.Scanner;
    /**
     * IM 客户端
     */
    public class ImClient {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ImClientInitializer());
            Channel channel = bootstrap.connect("localhost", ImConstant.PORT).sync().channel();
            // 发送登录消息
            sendLogin(channel, "user1");
            // 控制台输入发送消息
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String content = scanner.nextLine();
                sendChat(channel, "user1", "user2", content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
    /**
     * 发送登录消息
     */
    private static void sendLogin(Channel channel, String userId) {
        MessageProtocol msg = new MessageProtocol();
        msg.setType(1);
        msg.setFromId(userId);
        channel.writeAndFlush(msg);
    }
    /**
     * 发送聊天消息
     */
    private static void sendChat(Channel channel, String fromId, String toId, String content) {
        MessageProtocol msg = new MessageProtocol();
        msg.setType(2);
        msg.setFromId(fromId);
        msg.setToId(toId);
        msg.setContent(content);
        channel.writeAndFlush(msg);
    }
    }
 
2. ImClientInitializer.java（通道初始化）
    java
    import io.netty.channel.ChannelInitializer;
    import io.netty.channel.socket.SocketChannel;
    import io.netty.handler.codec.serialization.ClassResolvers;
    import io.netty.handler.codec.serialization.ObjectDecoder;
    import io.netty.handler.codec.serialization.ObjectEncoder;
    /**
     * 客户端通道初始化
     */
    public class ImClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)))
                .addLast(new ObjectEncoder())
                .addLast(new ImClientHandler());
    }
    }
 
3. ImClientHandler.java（消息接收）
    java
    import io.netty.channel.ChannelHandlerContext;
    import io.netty.channel.SimpleChannelInboundHandler;
    /**
     * 客户端处理器
     */
    public class ImClientHandler extends SimpleChannelInboundHandler<MessageProtocol> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) {
        System.out.println(msg.getFromId() + "：" + msg.getContent());
    }
    }
 
 
六、运行项目
1. 启动服务端
    运行  ImServer.java ，控制台输出：
    plaintext
    IM 服务端启动，端口：8888
 
2. 启动客户端1
    运行  ImClient.java ，自动登录  user1 
3. 启动客户端2
    复制  ImClient.java ，修改用户ID为  user2 ，运行
4. 发送消息
    在客户端1控制台输入消息，客户端2会收到
 
七、企业级功能扩展
1. 群聊功能
    - 新增群聊消息类型（3）
    - 维护群成员列表
    - 遍历群成员转发消息
2. 消息持久化
    - 离线消息存入 MySQL
    - 上线后拉取离线消息
3. 分布式部署
    - Nginx 负载均衡
    - Redis 共享在线状态
4. 消息加密
    - AES 加密消息内容
    - 防止消息窃听
 
八、Netty 核心知识点总结
1. 线程模型：主从多线程模型，高并发
2. 编解码器：处理消息序列化/反序列化
3. 心跳检测：维持长连接，避免死连接
4. 责任链模式：Pipeline 灵活扩展
5. 零拷贝：减少内存拷贝，提升性能
 
九、企业级应用场景
- 即时通讯（微信、钉钉、QQ）
- 游戏服务器
- 物联网（IoT）设备通信
- 实时数据推送（直播、监控）
- 分布式系统内部通信
