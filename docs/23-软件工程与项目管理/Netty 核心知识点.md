04.30 11:40
Netty 核心知识点
Netty 是基于 Java NIO 的高性能、异步事件驱动的网络通信框架，广泛应用于 RPC、网关、中间件、IM 等场景。下面从核心架构、核心组件、核心机制、核心特性、线程模型、内存模型、零拷贝、粘包拆包、异常处理、最佳实践等维度整理核心知识点，结构化输出。
 
一、Netty 核心定位
1. 封装 JDK 原生 NIO 复杂 API，屏蔽 Selector、Buffer、Channel 底层细节
2. 提供异步、非阻塞通信模型，高并发、高吞吐、低延迟
3. 内置编解码器、负载均衡、心跳检测、断连重连等开箱即用组件
4. 兼容 TCP/UDP/HTTP/WebSocket/Protobuf 等协议
5. 可扩展性强，通过  ChannelHandler  自定义业务逻辑
 
二、核心架构分层
Netty 整体分为 4 层：
1. 网络通信层：底层 NIO 封装， Channel 、 Selector 、零拷贝
2. 事件调度层： EventLoop  线程模型，任务调度、事件分发
3. 业务处理层： ChannelHandler 、 ChannelPipeline  责任链
4. 应用接口层： Bootstrap  启动器，简化客户端/服务端配置
 
三、核心组件详解
1. Bootstrap / ServerBootstrap
- Bootstrap：客户端启动器，绑定连接地址
- ServerBootstrap：服务端启动器，绑定端口监听
- 核心配置：线程组、Channel 类型、处理器、参数配置
2. EventLoop & EventLoopGroup
- EventLoop：单线程事件循环，负责IO 事件处理 + 任务执行
- EventLoopGroup：一组 EventLoop，提供线程池管理
- NioEventLoopGroup：默认 NIO 实现
- BossGroup：服务端接收连接，单线程即可
- WorkerGroup：处理 IO 读写，多线程
核心规则：一个 Channel 只会绑定一个 EventLoop，保证线程安全
3. Channel
- 代表网络连接，封装底层 JDK NIO SocketChannel
- 核心方法： connect() 、 bind() 、 write() 、 flush() 、 close() 
- 常用实现：
- NioSocketChannel：TCP 客户端
- NioServerSocketChannel：TCP 服务端
- NioDatagramChannel：UDP
4. ChannelHandler & ChannelPipeline
- ChannelHandler：业务逻辑处理器，分两类：
-  ChannelInboundHandler ：入站事件（读、连接、注册）
-  ChannelOutboundHandler ：出站事件（写、关闭、绑定）
- ChannelPipeline： ChannelHandler  责任链，每个 Channel 独有
- 入站：从头往后执行
- 出站：从后往前执行
- 常用内置 Handler：
-  LoggingHandler ：日志打印
-  ProtobufDecoder/Encoder ：Protobuf 编解码
-  LengthFieldBasedFrameDecoder ：解决粘包拆包
-  IdleStateHandler ：心跳检测
5. ChannelHandlerContext
-  ChannelHandler  与  ChannelPipeline  的桥梁
- 可获取 Channel、EventLoop、Pipeline
- 提供精准事件传播，避免整条链遍历
6. ByteBuf
Netty 自研高性能缓冲区，替代 JDK NIO ByteBuffer
- 优势：
- 读写指针分离（readerIndex/writerIndex），无需 flip()
- 自动扩容
- 支持堆内存、直接内存、复合缓冲区
- 类型：
- HeapByteBuf：堆内存，GC 管理
- DirectByteBuf：直接内存，零拷贝，堆外内存
- CompositeByteBuf：多个 ByteBuf 组合，无需拷贝
 
四、核心线程模型
1. 经典 Reactor 模式
Netty 基于主从 Reactor 多线程模型
- Boss Reactor：单线程，只做连接建立
- Worker Reactor：多线程，处理IO 读写 + 业务任务
- 任务类型：
1. IO 事件任务（读写、连接）
2. 普通任务  execute() 
3. 定时任务  schedule() 
2. 线程安全保障
- 一个 Channel 全程绑定一个 EventLoop
- 所有 IO 操作、Handler 回调都在同一个线程执行
- 业务逻辑无需加锁，天然线程安全
 
五、内存模型 & 零拷贝
1. 内存分配
- 池化内存： PooledByteBufAllocator （默认），复用内存，减少 GC
- 非池化内存： UnpooledByteBufAllocator ，每次新建
- 直接内存：绕过 JVM 堆，直接操作操作系统内存，减少拷贝
2. Netty 零拷贝
1. Buffer 共享： slice() 、 duplicate() ，不复制数据，只修改引用
2. 文件传输： FileRegion ，底层 sendfile，内核态直接传输
3. CompositeByteBuf：多缓冲区逻辑合并，无物理拷贝
 
六、粘包 & 拆包 解决方案
TCP 是流式协议，无边界，会出现：
- 粘包：多个数据包合并
- 拆包：一个数据包被拆分
Netty 内置解码器：
1. 固定长度： FixedLengthFrameDecoder 
2. 分隔符： DelimiterBasedFrameDecoder 
3. 长度字段： LengthFieldBasedFrameDecoder （最常用）
4. 自定义协议：继承  ByteToMessageDecoder  实现
 
七、核心异步机制
1. Future & Promise
-  Future ：异步结果只读
-  Promise ：可写的 Future，手动设置成功/失败
2. Listener 回调： addListener()  监听异步结果
3. 所有 IO 操作都是异步非阻塞，返回 ChannelFuture
 
八、核心特性
1. 心跳检测： IdleStateHandler  检测读空闲/写空闲/读写空闲
2. 断连重连：自定义  ChannelFutureListener  失败后重新连接
3. 优雅关闭： gracefulShutdown()  等待任务执行完再关闭
4. 流量控制： ChannelTrafficShapingHandler  限制读写速率
5. 协议编解码：内置 HTTP、WebSocket、Protobuf、JSON 等
6. 内存泄漏检测： ResourceLeakDetector  检测 ByteBuf 泄漏
 
九、常见问题 & 最佳实践
1. ByteBuf 释放
- 入站： SimpleChannelInboundHandler  自动释放
- 出站：手动  release() ，防止内存泄漏
2. 线程池配置
- BossGroup：1 线程足够
- WorkerGroup：默认 CPU核心数 * 2
3. 避免在 IO 线程执行耗时业务
- 耗时逻辑丢到业务线程池，防止阻塞 EventLoop
4. 优先使用直接内存 + 池化分配，提升性能
5. 必须处理半包，线上必须使用长度字段解码器
6. 关闭时调用 EventLoopGroup.shutdownGracefully()
 
十、核心执行流程
1. 启动 ServerBootstrap，绑定端口
2. BossGroup 线程监听 ACCEPT 事件
3. 客户端连接到达，Boss 接收连接，注册到 Worker EventLoop
4. Worker 线程监听 READ/WRITE 事件
5. 事件触发，进入 ChannelPipeline 责任链执行 Handler
6. 业务处理完成，写回数据，flush 发送
 
十一、高频面试核心考点
1. Netty 与 NIO、BIO 的区别
2. 主从 Reactor 线程模型原理
3. ChannelPipeline 责任链执行顺序
4. ByteBuf 设计与内存模型
5. 粘包拆包原因与解决方案
6. 零拷贝实现方式
7. EventLoop 线程安全机制
8. Future/Promise 异步模型
9. 内存泄漏排查与 ByteBuf 释放
10. 心跳检测与断线重连实现

