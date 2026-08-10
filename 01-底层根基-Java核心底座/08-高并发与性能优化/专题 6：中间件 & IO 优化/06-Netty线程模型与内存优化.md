# 06 Netty 线程模型与内存优化

> Netty 是 Java 网络编程的"地基"——Dubbo、Kafka 客户端、网关几乎都跑在它上面；它的高性能来自两个设计：EventLoop 的串行无锁，与内存池的预分配；2026 年 4.2 系列在 io_uring 与内存分配上继续进化

---

## 📚 目录

1. [EventLoop：串行无锁的并发模型](#1-eventloop串行无锁的并发模型)
2. [Pipeline：事件处理的流水线](#2-pipeline事件处理的流水线)
3. [内存池：PooledByteBufAllocator 与 arena](#3-内存池pooledbytebufallocator-与-arena)
4. [Netty 的四种零拷贝](#4-netty-的四种零拷贝)
5. [4.2 系列 2026 优化与背压](#5-42-系列-2026-优化与背压)

---

## 1. EventLoop：串行无锁的并发模型

Netty 的并发模型只有一句话：**一个 EventLoop 线程绑定一批 Channel，一个 Channel 的所有事件永远只由同一个 EventLoop 处理**。

- **串行 = 无锁**：既然同一 Channel 的读写、业务回调都在同一线程顺序执行，代码路径上不需要加锁——这是 Netty 高并发的第一来源。加锁的正确姿势是"锁永远不跨线程"，跨线程通信统一走 `eventLoop.execute()` 投递任务。
- **boss/worker 分离**：boss 组只 accept 新连接，把连接注册给 worker 组的某个 EventLoop；worker 组默认线程数 = 2 × CPU 核数，每个 EventLoop 持有一个 epoll/io_uring 轮询器。
- **铁律**：EventLoop 内禁止阻塞（睡眠、锁等待、同步 IO），阻塞 N 毫秒 = 该 EventLoop 上所有连接停顿 N 毫秒。耗时业务必须提交给业务线程池或虚拟线程执行。
- 一个经典反例：业务代码在 handler 里直接调 `Thread.sleep(100)` 模拟限速，P99 与吞吐同时崩掉——这不是 Netty 的锅，是线程模型用错了。

## 2. Pipeline：事件处理的流水线

`ChannelPipeline` 是 Netty 的责任链：入站事件（读）从 Head 流向 Tail，出站事件（写）反向流动，每个 handler 只处理自己关心的事件（[专题 1 的流水线思想](../../../01-底层根基-Java核心底座/08-高并发与性能优化/专题%201：性能理论基础/05-瓶颈定位方法论.md)同源）。

性能相关的两个要点：

- **编解码器放最前**：消息解码（字节 → 对象）尽早完成，后续 handler 处理业务对象；解码器要复用（如 `LengthFieldBasedFrameDecoder` 无状态）避免每个消息 new 对象。
- **handler 数量影响链长度**：一次读事件要穿过所有 handler，长链在百万 QPS 时是真实开销；把无关 handler 拆到不同 Channel 或用条件跳过，别让链上站着不干活的 handler。
- **粘包/拆包处理是吞吐暗坑**：TCP 是字节流没有消息边界，编解码器要负责切分（定长、分隔符、长度字段三种模式）。错误做法是每条消息都走正则/全量扫描；标准做法是 `LengthFieldBasedFrameDecoder` 按长度字段切帧——它内部用索引定位而非复制，大帧高频场景性能差距数倍。
- **线程数配置**：`bossGroup` 通常 1（只 accept）；`workerGroup` 默认 2 × CPU，纯 IO 场景可降到 CPU 数，业务重则加大但别超过"IO 线程不阻塞"的边界——**线程数不是性能参数，阻塞才是**。虚拟线程（JDK 21+）承载业务 handler 后，worker 可以回归纯 IO 的"核数级"规模（[05 篇](05-JavaIO模型演进与多路复用.md)）。

## 3. 内存池：PooledByteBufAllocator 与 arena

网络 IO 的 ByteBuf 分配/释放高频发生，直接 `new byte[]` 会产生大量对象与 GC 压力。Netty 用内存池解决：

- **jemalloc 思想**：把内存按 size class 切块（8/16/32/.../16MB），小块从 thread-local 缓存取（Tiny/PoolSubpage），大块走 buddy 分配（PoolChunk），线程间竞争通过 arena 分片摊薄。命中缓存时分配只是指针操作，**近乎零开销、零 GC**。
- **堆内 vs 直接内存**：直接内存（DirectBuffer）省掉堆与内核之间的一次拷贝，是网络 IO 首选，但不受 JVM 堆管，要防泄漏（`ReferenceCountUtil.release` 或 `SimpleChannelInboundHandler` 自动释放）。
- **2026 的 AdaptiveByteBuf**：4.2 系列大改——大缓冲用 buddy 分配器、`setBytes(byte[])` 免分配提速、批量字节搬运（getBytes）改为解包目标缓冲 + NIO absolutePut，直写内存不 new ByteBuffer（PR #16781）；4.2.16 起 thread-local 的 chunk 缓存用环形缓冲 + 分代回收。**升级 4.2 系列本身就是内存优化**。
- 参数注意：`PooledByteBufAllocator` 拒绝负 maxOrder（4.2.17 修复）；低核容器要防直接内存 OOM（4.2.17 专项修复）。

## 4. Netty 的四种零拷贝

Netty 文档里的"零拷贝"有四种含义，面试常混（内核零拷贝的完整拆解见[07 篇](07-零拷贝与序列化优化.md)）：

- **CompositeByteBuf**：多个缓冲逻辑合并为一个，读写时零数据搬移（避免"拼包"的数组拷贝）。
- **slice() / duplicate()**：共享同一段内存的视图，改原缓冲影响视图，零拷贝切分。
- **FileRegion**：文件发送走 `sendfile()`，数据从内核页缓存直达网卡，不进用户态——大文件传输的官方姿势。
- **Unpooled.wrappedBuffer**：包装已有字节数组，不复制。注意：上三种是"逻辑零拷贝"（省 CPU 拷贝），FileRegion 才是真正的内核零拷贝。

## 5. 4.2 系列 2026 优化与背压

2026 版本线（4.2.10 → 4.2.17.Final，6 月 1 日的 4.2.15 是安全大版本，修复 DNS 投毒、HTTP/2 流轰炸、请求走私等一批高危 CVE，**必须升级**）：

- **io_uring 传输转正**：4.2 正式支持 io_uring，高吞吐场景优于 epoll（Apache RATIS 实测并采用）；4.2.10 重写了 `SENDMSG_ZC` 零拷贝发送，4.2.14 扩展 user data 至 long。
- **HTTP/2 治理**：默认并发流上限提到 100；流卡死不再空转 DATA 帧而是失败重启（PR #16947，修 HTTP/2 代理 OOM）。
- **背压**：`WRITE_BUFFER_WATER_MARK`（高水位 64KB/低水位 32KB）——写缓冲越过高水位触发 `channelWritabilityChanged`，应用应暂停生产，回落后恢复；流量控制做在应用层，是防 OOM 的最后防线。
- **分配器选型**：`io.netty.allocator.type` 默认 pooled——**不要改回 unpooled**；直接内存（`PooledByteBufAllocator.DEFAULT`）是网络 IO 默认，堆内分配器留给本地小对象场景。生产常见的"高 GC"误诊是业务代码在 handler 里 new 大数组，锅在业务不在分配器。
- **优雅停机**：`channel.closeFuture().sync()` + 业务线程池 shutdown 后再退进程，否则发布时在途请求被硬杀，客户端表现为连接重置（[08 篇](08-网络层与连接治理.md)的 CLOSE_WAIT 也常源于此）。
- **引用计数纪律**：入站 `ByteBuf` 归 Netty 管、出站归业务管——handler 里 `retain/release` 错一对，线上就是泄漏或复用错误；`SimpleChannelInboundHandler` 自动释放是安全默认。与"连接泄漏"并列，**内存泄漏是 Netty 应用最隐蔽的生产事故**，配合 4.2 的 leak detector 在测试环境全开，生产不背这个债。

Netty 的 API 全解、编解码器、与 Spring 集成见 [Netty 体系](../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/Netty/00-Netty知识体系总览.md)。一句话总结定位：**Netty 把"并发正确性"用线程模型解决，把"内存效率"用内存池解决，把"CPU 效率"用零拷贝解决**——三个维度互不替代。

> 🎯 **核心要点**：EventLoop 串行无锁是 Netty 高并发的第一来源，一个 Channel 永远只被一个线程处理，跨线程通信走 execute()；EventLoop 内禁止阻塞；内存池（arena 分片 + thread-local 缓存 + buddy 大块）让 ByteBuf 分配近乎零 GC，4.2 的 AdaptiveByteBuf 与 io_uring 是 2026 两大升级；零拷贝四形态分清逻辑零拷贝与内核零拷贝（FileRegion）；4.2.15 安全版本必须升级。

---

**下一模块**：[07 零拷贝与序列化优化](07-零拷贝与序列化优化.md)

**返回总览**：[00-总览](00-总览.md)
