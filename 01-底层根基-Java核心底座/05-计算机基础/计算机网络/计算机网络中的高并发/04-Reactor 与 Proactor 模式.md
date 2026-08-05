# Reactor 与 Proactor 模式

> 多路复用是"内核怎么通知"，Reactor/Proactor 是"用户态怎么分发"。Reactor 是就绪通知（epoll 的天然搭档，Netty/Redis/Nginx 的骨架），Proactor 是完成通知（io_uring 的天然搭档）——模式选型决定了线程模型与回调代码的形态

---

## 📚 目录

1. [两个模式的本质](#1-两个模式的本质)
2. [Reactor 模式：就绪通知 + 事件分发](#2-reactor-模式就绪通知--事件分发)
3. [Reactor 的三种线程模型](#3-reactor-的三种线程模型)
4. [Netty 的 EventLoop 剖析](#4-netty-的-eventloop-剖析)
5. [Proactor 模式：完成通知](#5-proactor-模式完成通知)
6. [Reactor vs Proactor 选型](#6-reactor-vs-proactor-选型)
7. [核心要点与思考题](#7-核心要点与思考题)

---

## 1. 两个模式的本质

```text
Reactor（反应器）：内核说"你可以读了" → 用户态自己 read → 处理
Proactor（前摄器）：内核说"我已经读好了" → 用户态直接处理数据

对应关系：
  Reactor  ←→ epoll / kqueue / select（就绪模型）
  Proactor ←→ io_uring / IOCP / AIO（完成模型）

一句话：
  Reactor = "事件就绪再干活"
  Proactor = "命令交给内核干完再叫醒我"
```

> 🎯 **核心要点**：模式不是"性能之争"，而是**内核机制的用户态投影**——你在 Linux 用 epoll，天然是 Reactor 风格；用 io_uring，天然是 Proactor 风格。强行跨层（如 epoll 上模拟 AIO）得不偿失。

## 2. Reactor 模式：就绪通知 + 事件分发

### 2.1 四个组件

```text
Event Handler（事件处理器）：业务回调（onReadable/onWritable/onAccept）
Reactor（反应器）：管理事件注册 + 事件循环分发
     ├── init_dispatcher：注册事件处理器
     ├── event_loop：阻塞等待就绪事件（epoll_wait）
     └── dispatch：按事件类型调用对应处理器
Synchronous Event Demultiplexer：内核多路复用器（epoll）
Handle（句柄）：fd/连接
```

```text
一次读请求的旅程（Reactor）：
  ① 连接注册 OP_READ → epoll 红黑树
  ② 数据到达 → 内核回调 → 就绪链表
  ③ epoll_wait 返回 → Reactor 拿到就绪事件
  ④ Reactor 分发 → 对应 EventHandler.onReadable()
  ⑤ 处理器里 read() + 业务处理
```

### 2.2 最小 Reactor 骨架

```java
// 伪代码：一个 Reactor 的完整骨架（Java NIO 实现思路）
class Reactor implements Runnable {
    final Selector selector = Selector.open();

    void register(SocketChannel ch, int ops, EventHandler handler) {
        ch.register(selector, ops);          // 注册到内核（epoll_ctl）
        handlers.put(ch, handler);
    }

    @Override
    public void run() {                       // 事件循环
        while (!Thread.currentThread().isInterrupted()) {
            selector.select();                // 阻塞等就绪（epoll_wait）
            for (SelectionKey key : selector.selectedKeys()) {
                dispatch(key);                // 分发
            }
            selector.selectedKeys().clear();
        }
    }

    void dispatch(SelectionKey key) {
        SocketChannel ch = (SocketChannel) key.channel();
        if (key.isAcceptable()) handlers.get(ch).onAccept(ch);
        if (key.isReadable())   handlers.get(ch).onReadable(ch);
        if (key.isWritable())   handlers.get(ch).onWritable(ch);
    }
}
```

## 3. Reactor 的三种线程模型

### 3.1 单线程 Reactor（最简，Redis 模型）

```text
一个线程：accept + read + 业务 + write 全部串行
  Redis（单线程事件循环）就是这个模型 —— 业务必须是快操作
优点：无锁、无上下文切换、简单
缺点：单核；业务阻塞 = 全站卡死
适用：CPU 轻量、内存操作型服务（Redis、游戏房间服）
```

### 3.2 多线程 Reactor（引入工作线程池）

```text
一个 Reactor 线程：accept + read + 分发（不阻塞）
工作线程池 N 个：执行业务（慢操作在这里）
主线程写回：业务完成后主线程 write（或工作线程直接写）

优点：业务慢操作不阻塞事件循环
缺点：主 Reactor 仍是单点；写回路径多一次线程切换
适用：业务含 DB/文件等慢操作的通用服务
```

### 3.3 主从 Reactor（Netty 模型，生产主流）

```text
Main Reactor（1 个）：只负责 accept（接受连接）
Sub Reactor（N 个）：每个一个事件循环，负责已建立连接的读写
  → 连接均匀分配给多个 Sub Reactor（负载均衡）
  → 每连接固定绑定一个 Sub Reactor（连接内无竞争、顺序保证）
工作线程池：业务（可选，Netty 默认业务也在 EventLoop 内执行）

优点：accept 与读写分离；多核并行；连接亲和（无锁）
代表：Netty、Tomcat NIO、Nginx（多 worker 进程类似思想）
```

```text
演进逻辑：单线程(简单) → 多线程(解耦业务) → 主从(解耦 accept+并行读写)
面试答法：先讲三种模型结构，再讲各自解决什么问题、代表框架
```

## 4. Netty 的 EventLoop 剖析

### 4.1 结构

```java
// Netty 的 EventLoopGroup ↔ Sub Reactor 组
EventLoopGroup bossGroup = new NioEventLoopGroup(1);      // Main Reactor（accept）
EventLoopGroup workerGroup = new NioEventLoopGroup();     // Sub Reactor（读写，默认 2×核数）
ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
 .channel(NioServerSocketChannel.class)                   // 基于 epoll（Linux）的 NIO 通道
 .childHandler(new ChannelInitializer<SocketChannel>() { ... });
```

| Netty 组件 | 对应模式组件 | 说明 |
|-----------|-------------|------|
| `EventLoopGroup` | Reactor 组 | 线程池，每线程一个事件循环 |
| `EventLoop` | Reactor | 单线程事件循环（**一个连接的一生只在一个 EventLoop**） |
| `ChannelHandler` | Event Handler | 业务回调（pipeline 链式） |
| `Selector` | Demultiplexer | 封装 epoll |

### 4.2 关键设计：连接亲和（Channel → EventLoop 绑定）

```text
连接注册到某个 EventLoop 后永不迁移：
  ① 该连接的所有事件按序执行（天然顺序性，无并发）
  ② 该连接内无锁（Handler 无需处理同一连接并发）
  ③ 代价：慢 Handler 会阻塞同 EventLoop 上的其他连接
      → 规范：业务重操作交给单独的业务线程池（channel.writeAndFlush 后线程池执行）
```

### 4.3 Netty 对 epoll 的封装细节

```text
默认使用 NioEventLoop（Java NIO 封装 epoll LT）
Linux 上可用 EpollEventLoopGroup（原生 epoll，含 EPOLLET 边缘触发）
2026 方向：io_uring 的 Netty 集成仍属实验性（netty-incubator 项目）
```

## 5. Proactor 模式：完成通知

### 5.1 结构

```text
Completion Handler（完成处理器）：处理"内核已完成"的结果
Proactor（前摄器）：提交异步操作 + 分发完成事件
Asynchronous Operation Processor：内核异步执行器（io_uring 内核侧）
  → 提交：把 read 请求写入 SQ 环
  → 完成：内核拷贝完数据，写 CQ 环，唤醒
  → 分发：Proactor 消费 CQ，调 CompletionHandler
```

```text
一次读请求的旅程（Proactor / io_uring）：
  ① 提交 read 请求（写 SQ 环，SQPOLL 下零系统调用）
  ② 内核自行拷贝数据到应用提供的缓冲区
  ③ 完成 → CQ 环出现完成项 → 唤醒应用
  ④ 应用直接消费已就绪的数据（无需再 read！）
```

### 5.2 与 Reactor 的本质差异

| 维度 | Reactor（epoll） | Proactor（io_uring） |
|------|:---:|:---:|
| 通知内容 | "**可以**读"（就绪） | "**已**读完"（完成） |
| 数据拷贝 | 用户态做（read） | **内核做** |
| 系统调用次数 | 每事件至少 1 次 read | 提交 1 次（SQPOLL 下 0 次） |
| 缓冲区管理 | 就绪后现分配/复用 | **必须预注册**（fixed buffers） |
| 背压 | 读多少自己控制 | 提交前就要决定缓冲大小 |
| 代表 | Nginx/Netty/Redis | io_uring 系（数据库存储领域） |

## 6. Reactor vs Proactor 选型

```text
选 Reactor（epoll）：
  ✅ Web 服务器/网关/微服务（业务逻辑是瓶颈）
  ✅ 团队熟悉、生态成熟（Netty/Nginx/Go/Node 全部 Reactor）
  ✅ 需要灵活的缓冲区管理
  ✅ 2026 默认选择

选 Proactor（io_uring）：
  ✅ 存储/数据库引擎（磁盘 I/O 混合场景收益最大）
  ✅ 尾时延极致敏感（交易、直播）
  ✅ 数据面固定、可预分配缓冲（回显、转发）
  ✅ 单核吞吐极限（SQPOLL + multishot + ZC Rx）

混合（生产常见）：
  业务路径 Reactor + 文件/大流量路径 io_uring
  例：HAProxy 2.6+ 双引擎；RocksDB/MySQL 内嵌 io_uring 同时对外 Reactor
```

> 💡 **2026 判断**：不要把"io_uring 更快"简单等同"Proactor 更好"——模式选型跟着**瓶颈与团队**走：业务瓶颈选 Reactor（生态红利），I/O 瓶颈选 Proactor（机制红利）。

## 7. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. Reactor=就绪通知（epoll 投影）、Proactor=完成通知（io_uring 投影）——模式是内核机制的用户态形态；
> 2. Reactor 三线程模型：单线程（Redis）→ 多线程（业务解耦）→ 主从（Netty：accept 与读写分离、连接亲和无锁）；
> 3. 选型：默认 Reactor（生态成熟）；io_uring 系 Proactor 用于存储/尾时延敏感场景；生产可混合。

**思考题**：

1. Reactor 分发的是"就绪"还是"完成"？（→ 2.1）
2. 主从 Reactor 解决了前两种模型的什么问题？（→ 3.3）
3. Netty 为什么让"一个连接只属于一个 EventLoop"？（→ 4.2）
4. Proactor 为什么要求预分配缓冲区？（→ 5.2）

---

**下一模块**：[05-零拷贝与内核优化](05-零拷贝与内核优化.md)｜**返回总览**：[00-计算机网络高并发知识体系总览](00-计算机网络高并发知识体系总览.md)
