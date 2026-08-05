# 多路复用机制深潜：select、poll、epoll、io_uring

> select 的 O(n) 扫描、poll 的链表改进、epoll 的红黑树+就绪链表+LT/ET——每一步都是"省 CPU、省拷贝、省系统调用"的工程演进。2026 年 io_uring 用共享内存环形队列把系统调用几乎清零，成为数据库与存储的新标配

---

## 📚 目录

1. [select：1024 个 fd 的轮询时代](#1-select1024-个-fd-的轮询时代)
2. [poll：解除数量限制](#2-poll解除数量限制)
3. [epoll：事件驱动的地基](#3-epollevent-驱动的地基)
4. [LT 与 ET：两种触发语义](#4-lt-与-et两种触发语义)
5. [kqueue 与 IOCP：对照坐标系](#5-kqueue-与-iocp对照坐标系)
6. [io_uring：环形队列与零系统调用（2026）](#6-io_uring环形队列与零系统调用2026)
7. [选型决策：epoll 还是 io_uring](#7-选型决策epoll-还是-io_uring)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. select：1024 个 fd 的轮询时代

### 1.1 接口与限制

```c
int select(int nfds, fd_set *readfds, fd_set *writefds, fd_set *exceptfds,
           struct timeval *timeout);
// fd_set 是位图，FD_SETSIZE 默认 1024 —— 单次最多监视 1024 个 fd
```

| 问题 | 说明 |
|------|------|
| 数量限制 | 位图大小固定 1024（可改但需重编译内核） |
| O(n) 扫描 | 每次调用把整个 fd 集合从用户态**拷贝**到内核，内核逐个扫描，再拷回用户态 |
| 重复拷贝 | 每次调用都要传集合（不能增量注册） |
| 无就绪信息 | 只告诉你"有 fd 就绪"，不知道是哪个——仍需遍历全部 fd |

```text
1 万个连接用 select：
  每次 select 调用：拷贝 1 万个 fd 的位图（~1.25KB）×2 次 + 内核 O(n) 扫描
  1 万连接即使只有 1 个活跃 → 也要遍历 1 万个 fd
  → 复杂度 O(n)，CPU 空转在"扫描"上
```

## 2. poll：解除数量限制

```c
struct pollfd { int fd; short events; short revents; };
int poll(struct pollfd *fds, nfds_t nfds, int timeout);
// 链表数组传递，无 1024 限制
```

| 改进 | 仍未解决 |
|------|---------|
| 数量无上限（链表） | 仍是每次调用**全量拷贝 + 全量扫描**（O(n)） |
| 返回就绪的 revents | 返回后仍需遍历全部 fd 检查 revents |

> 💡 **定位**：poll 是 select 的"扩量版"，复杂度模型没变——**fd 数量级提升后两者同样低效**。它们的共同病灶：**内核无状态**（每次调用都要把整个集合重新传一遍）。

## 3. epoll：事件驱动的地基

### 3.1 三个 API 与内核状态

```c
int epoll_create1(int flags);                // 创建内核事件表（红黑树根）
int epoll_ctl(int epfd, int op, int fd, struct epoll_event *ev);  // 注册/修改/删除（增量！）
int epoll_wait(int epfd, struct epoll_event *events, int maxevents, int timeout); // 取就绪事件
```

**核心设计：内核持状态**——注册一次，永不重复拷贝：

```text
内核数据结构：
  红黑树（eventpoll.rbr）    ：存放所有注册的 fd（增删改查 O(log n)）
  就绪链表（eventpoll.rdllist）：存放"已就绪"的 fd（epoll_wait 直接取出，O(1)）
  等待队列（wq）              ：epoll_wait 的阻塞队列
```

### 3.2 就绪通知机制（以可读为例）

```text
① epoll_ctl 注册 fd → 插入红黑树，fd 关联一个回调（ep_poll_callback）
② 数据到达 → 网卡中断 → 协议栈处理 → socket 缓冲区有数据
   → 触发回调 → 该 fd 被加入就绪链表
   → 若有线程在 epoll_wait 阻塞 → 唤醒
③ epoll_wait 返回：就绪链表中的 fd 集合（用户态只需处理"就绪的"）
```

```text
复杂度对比（10 万连接，1 个活跃）：
  select：每次调用 O(n) 全量扫描 + 拷贝  → 10 万次检查
  epoll：注册 O(log n)；等待 O(1) 取就绪链表 → 只处理就绪的 1 个
```

### 3.3 三个决定性优势

| 优势 | 机制 |
|------|------|
| **无重复拷贝** | 内核持状态，注册一次 |
| **O(1) 取就绪** | 就绪链表直接拿，不扫描全部 |
| **回调通知** | 数据到达即入链，不是轮询发现 |

## 4. LT 与 ET：两种触发语义

| 触发模式 | 语义 | 应用侧要求 |
|---------|------|-----------|
| LT（水平触发，默认） | 只要缓冲区**还有数据**，每次 epoll_wait 都报告 | 读一部分没关系，下次还会报 |
| ET（边缘触发） | 只在"缓冲区从空→非空"的**那一刻**报告一次 | **必须一次性读完**，否则事件丢失 |

```java
// Java NIO 中（SelectionKey）：
// 默认 LT；ET 需 SocketChannel 配置 + OP_READ 常注册（Netty 内部实现）
```

```text
ET 的两个配套动作（缺一不可）：
  ① 就绪后循环 read 到 EAGAIN（读完为止）
  ② 保持 OP_READ 事件常驻注册（避免"边缘过后不再触发"）

ET 的收益：减少 epoll_wait 触发次数（高吞吐下系统调用更少）
ET 的代价：读不全 = 永久丢事件 —— 写错就卡死，所以默认 LT
生产选择：Netty 默认用 ET + 底层 EPOLLET；业务自写多用 LT 更稳
```

> 🎯 **核心要点**：LT/ET 是面试必考。一句话答法——"LT 有数据就报（读不完下次还报），ET 只在从无到有时报一次（必须读完，否则事件丢失）；ET 省系统调用但要求读循环到 EAGAIN"。

## 5. kqueue 与 IOCP：对照坐标系

| 机制 | 平台 | 模型 | 特点 |
|------|------|------|------|
| epoll | Linux | 多路复用（就绪） | 红黑树+回调；ET/LT |
| kqueue | BSD/macOS | 多路复用（就绪） | 同样事件驱动；额外支持文件/信号事件 |
| IOCP | Windows | **异步（完成）** | 完成端口模型：提交 I/O → 完成后投递完成包 |
| io_uring | Linux 5.1+ | **异步（完成）** | 与 IOCP 同属"完成模型"，环形队列实现 |

> 💡 **理解角度**：epoll 与 kqueue 是"就绪模型"（同族异平台）；**io_uring 与 IOCP 才是同族**（完成模型）——io_uring 常被比作"Linux 终于有了 IOCP"，但用共享内存环形队列实现得更好。

## 6. io_uring：环形队列与零系统调用（2026）

### 6.1 核心机制

```text
两个共享内存环形队列（内核与用户态共享，无需拷贝）：
  SQ（Submission Queue）：应用提交 I/O 请求（写 SQ 环即可）
  CQ（Completion Queue）：内核完成请求后写结果（应用消费 CQ 环）

三种运行模式：
  普通模式：提交/收割仍需少量 syscall（io_uring_enter）
  SQPOLL 模式：内核线程（io_uring-wq）持续轮询 SQ
    → 应用提交请求时"零系统调用"（写共享内存即可）
    → 代价：一个内核线程持续占一个 CPU 核心
  注册优化：注册文件（registered files）、固定缓冲区（fixed buffers）、
    multishot（一次提交多次完成，如 accept/recv）
```

### 6.2 与 epoll 的基准对比（2026 实测）

```text
200,000 并发连接 / 100Gbps TCP 回显压测：
  方案                       吞吐           p99 时延
  epoll                      2.1M req/s     980µs
  io_uring                   3.4M req/s     520µs   （+60%）
  io_uring + SQPOLL          4.8M req/s     240µs   （再 +40%）

UringCL（滑铁卢大学硕士论文，2026-01）：
  传统事件循环接入 io_uring 收敛层：批量传输吞吐 +40%，尾时延更低更稳
```

### 6.3 2026 采用现状

| 阵营 | 采用 |
|------|------|
| 数据库/存储（新标配） | **PostgreSQL PG17/18、MySQL/InnoDB、RocksDB、ScyllaDB、TigerBeetle** |
| 中间件 | HAProxy 2.6+、MinIO、SeaweedFS、容器运行时（crun/containerd 实验） |
| 主流网络服务（仍用 epoll） | **Nginx、Redis、Node.js（libuv）、Go netpoller** |

### 6.4 需要注意的代价

```text
① 生产差距：合成基准 +43% 的收益在混合流量下缩水到 ~8%（Go/epoll 案例）
② 安全史：2022 年 Google 统计约 60% 内核漏洞赏金投给了 io_uring
   （CVE-2021-41073、CVE-2023-2598、CVE-2023-21400；曾禁用于 ChromeOS）
③ 可观测性：strace/EDR 看不到环形队列的数据路径（需 eBPF tracepoint）
④ 复杂度：预分配缓冲区、显式背压处理、SQPOLL 占核
⑤ 内核版本：完整特性需 6.0+/6.1+
```

## 7. 选型决策：epoll 还是 io_uring

```text
用 io_uring 的时机（2026 共识）：
  ✅ 单机数百万 QPS、要求 p99 < 10µs
  ✅ 网络 + 磁盘混合 I/O（统一异步模型简化架构）
  ✅ 尾时延敏感（直播/交易/实时推送）
  ✅ 内核 6.0+ 且团队有 C/底层能力

用 epoll 的时机（默认）：
  ✅ 单机 < 50 万 req/s（业务逻辑是瓶颈而非系统调用）
  ✅ 团队熟悉度与运维简单性优先
  ✅ 高连接抖动场景（Go/epoll 实测 p99 更稳）
  ✅ 安全/可观测性要求严格

混合模式（推荐路径）：热路径 io_uring（如零拷贝文件服务）+
                     其余 epoll —— 增量迁移而非整体重写
```

> 🎯 **核心要点**：2026 年的共识是——**"io_uring 是未来，epoll 是稳定的当打之年"**。数据库/存储已全面转向 io_uring；Web 服务器领域 epoll 仍是主流；除非 I/O 确实是瓶颈，否则不必迁移。

## 8. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. select/poll 的病根是"内核无状态"（全量拷贝+O(n)扫描）；epoll 用红黑树+就绪链表+回调实现注册一次、O(1) 取就绪；
> 2. LT/ET 是"有数据就报" vs "从无到有报一次"——ET 省系统调用但必须读循环到 EAGAIN；
> 3. io_uring 用共享内存环形队列实现真异步（完成模型），2026 年数据库标配、Web 领域仍是 epoll 当打，选型看 QPS 与团队能力。

**思考题**：

1. select 为什么慢？epoll 从哪三点上解决？（→ 1/3.3）
2. ET 模式读不完会发生什么？（→ 4）
3. io_uring 的"零系统调用"靠什么实现？（→ 6.1 SQPOLL）
4. 为什么"合成基准 43% 优势"在生产会缩水？（→ 6.4）

---

**下一模块**：[04-Reactor 与 Proactor 模式](04-Reactor 与 Proactor 模式.md)｜**返回总览**：[00-计算机网络高并发知识体系总览](00-计算机网络高并发知识体系总览.md)
