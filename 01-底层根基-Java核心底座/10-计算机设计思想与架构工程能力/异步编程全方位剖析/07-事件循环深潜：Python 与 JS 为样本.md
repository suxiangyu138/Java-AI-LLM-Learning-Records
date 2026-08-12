# 事件循环深潜：Python 与 JS 为样本

> 事件循环是"回调/协程/背压"三者的共同底座，值得单独拆开看内脏。本章以 Python asyncio 与 JS/libuv 为双样本，讲清事件循环的内部结构、调度顺序、C 实现加速（uvloop），以及"阻塞调用混入"的灾难模型——看懂循环，就看得懂一半的异步事故

---

## 📚 目录

1. [事件循环的内部结构：三个角色](#1-事件循环的内部结构三个角色)
2. [调度顺序：就绪队列与等待队列](#2-调度顺序就绪队列与等待队列)
3. [asyncio 的 selector：事件注册与分发](#3-asyncio-的-selector事件注册与分发)
4. [uvloop：C 实现的事件循环](#4-uvloopc-实现的事件循环)
5. [libuv 与 Node 的线程池](#5-libuv-与-node-的线程池)
6. [阻塞调用混入的灾难模型](#6-阻塞调用混入的灾难模型)
7. [核心要点与思考题](#7-核心要点与思考题)

---

## 1. 事件循环的内部结构：三个角色

任何事件循环都由三个角色组成，理解它们就理解了循环的骨架：

- **就绪队列（ready queue）**：等待执行的任务（宏任务/回调/可运行协程）——每轮循环从这里取任务执行
- **等待注册表（waiter registry）**：挂起中的 IO 等待与定时器——**挂在这里的任务不占线程，事件就绪/定时到期时被唤醒移入就绪队列**
- **多路复用器（multiplexer）**：真正监听的底层（epoll/kqueue/IOCP）——就绪事件 → 唤醒对应等待任务

```text
┌───────────────── 事件循环线程 ─────────────────┐
│  就绪队列 ←── 唤醒 ── 等待注册表 ←── 事件 ── epoll │
│     │                    ▲                      │
│     └── 执行任务 ── 发起 IO ──┘                  │
└─────────────────────────────────────────────────┘
```

工程推论：**"挂起"不是"暂停"，而是"移入等待注册表"**——协程/回调在等待时只占一个注册条目（几十字节），线程继续跑就绪队列——这就是单线程承载十万连接的结构性原因。

## 2. 调度顺序：就绪队列与等待队列

事件循环的调度顺序由"队列优先级"决定（JS 的宏/微任务、Python 的 ready 队列）：

```text
每一轮循环（JS 模型）：
  1. 取一个宏任务执行（IO 回调、定时器、事件处理器）
  2. 执行期间产生的微任务（Promise 回调）→ 全部执行完
  3. 回到 1

每轮循环（Python asyncio 模型）：
  1. 执行所有 ready 队列中的任务（Task 按顺序跑，遇 await 挂起）
  2. 处理 IO 事件：epoll 返回就绪 fd → 唤醒对应 Future → 移入 ready
  3. 处理到期定时器
  4. 回到 1
```

两个"顺序陷阱"（生产事故常客）：

- **长任务饿死循环**：一个协程/回调里跑了 100ms 的 CPU 计算——**整个循环这 100ms 停止服务**所有连接；事件循环的"公平性"靠任务自觉（分段/让出），没有抢占
- **优先级反转**：微任务队列"执行完为止"——微任务无限产生会让宏任务永远排不上（JS 的 `queueMicrotask` 递归陷阱）

## 3. asyncio 的 selector：事件注册与分发

Python asyncio 的 IO 层是 **selector 抽象**（`asyncio.SelectorEventLoop`）：

```python
# asyncio 内部形态（概念）：socket 注册 → 事件 → 唤醒 Future
loop.add_reader(sock.fileno(), callback)   # 注册"可读"回调
loop.remove_reader(sock.fileno())          # 取消注册
# 底层：selector（epoll）select() 返回就绪 fd 列表 → 分发到对应回调
```

asyncio 的调度链：**`await sock.read()` → Future 挂入等待注册表 → selector 注册 fd → epoll 事件 → selector 唤醒 Future → Future 完成 → 协程恢复**——每个环节都有明确归属，生产排障时顺着这条链查：

1. fd 注册了吗？（没注册 = 事件永远不来 = 挂起不醒）
2. 事件到了吗？（`epoll` 层面确认，`ss -t` 看连接状态）
3. 唤醒了吗？（Future 完成状态，日志/断点确认）
4. 协程恢复了吗？（卡在 await 之后的哪一行——py-spy 采样定位）

**"协程挂起不醒"的排查，90% 落在 1/2 环节**——不是协程写错，是事件根本没注册或没到达。

## 4. uvloop：C 实现的事件循环

**uvloop**（基于 libuv 的 C 实现事件循环）是 Python 异步性能的关键拼图：

- **机制**：`asyncio` 的 loop 换成 libuv 的 C 循环——事件分发、定时器、线程池全部 C 实现，Python 层只留协程包装
- **效果**：吞吐与 Node.js 同档（基准约为纯 Python 事件循环的 2-4 倍），IO 密集服务的 CPU 占用显著下降
- **2026 状态**：FastAPI/uvicorn 生产标配（`uvicorn --loop uvloop` 或自动检测）；2024 起支持可选的 io_uring 后端（02 章）——**Python 的"慢"在事件循环层面已被 C 实现抹平**，剩余差距在协议解析与业务代码

```bash
# FastAPI 生产启动：uvloop 事件循环 + uvicorn worker
uvicorn app:app --loop uvloop --workers 4
```

启示：**事件循环的性能天花板由"循环实现"决定，不由语言决定**——Python 用 C 循环追平 Node，Java 用 JVM 线程模型绕开循环（虚拟线程），两条路殊途同归：**把"事件循环的 C 化"与"循环的消失（虚拟线程）"看成异步性能战的两条并线**。

顺带澄清一个常见误解：**uvloop 不是"把 asyncio 变成别的框架"**——它替换的只是事件循环内核（libuv），协程语法、`async/await` 语义、调度 API 全部不变；应用代码零改动即可获得性能提升。**"换循环不动代码"是 C 化方案的最大红利**——对比"换模型要改代码"（同步 → 异步改造），生产采纳成本完全不同。

## 5. libuv 与 Node 的线程池

Node.js 的 libuv 有一个常被误解的设计——**线程池（thread pool，默认 4 线程）**：

- **事件循环之外**：文件 IO、DNS 查询、`crypto` 重计算、`fs.readFile` 等"内核没有异步接口或同步阻塞"的操作——**丢进线程池执行**，完成后回调送回事件循环
- **默认大小 4**（`UV_THREADPOOL_SIZE` 可调）：**文件 IO 密集的 Node 服务，线程池是隐藏瓶颈**——4 个线程被慢磁盘占满，所有文件操作排队
- **2026 的类比**：Node 的"线程池 + 事件循环"分工 = Python 的 `asyncio.to_thread` + 事件循环 = Java 的虚拟线程载体——**"事件循环管网络、线程池管阻塞"是异步世界的通用分工**，只是实现与命名不同

```javascript
// Node：fs 操作进线程池，网络 IO 在事件循环
fs.readFile("big.log", (err, data) => { ... });   // 线程池
http.createServer((req, res) => { ... });          // 事件循环
```

## 6. 阻塞调用混入的灾难模型

事件循环的头号生产事故——**阻塞调用混入**：事件循环线程里出现同步阻塞（`time.sleep`、`requests.get`、`socket.recv` 阻塞模式、CPU 重计算），**整个循环停摆**：

```text
灾难链条：
  一个请求的 handler 里 time.sleep(1)（阻塞循环线程 1 秒）
  → 循环 1 秒不处理任何事件
  → 所有连接的 IO 事件排队（内存中积累）
  → 1 秒后集中处理 → 大量连接超时/重试
  → 重试加剧负载 → 雪崩
```

```python
# 事故现场：看似无害的一行
@app.get("/search")
async def search(q: str):
    time.sleep(0.5)                        # ❌ 阻塞事件循环 0.5s——全服务卡顿
    result = requests.get(SEARCH_API).json()  # ❌ 同步 IO 同理
    return result

# 正确姿势
@app.get("/search")
async def search(q: str):
    await asyncio.sleep(0.5)               # ✅ 异步睡眠：让出循环
    result = await httpx.get(SEARCH_API)   # ✅ 异步客户端
    # 或：阻塞任务丢线程池
    result = await asyncio.to_thread(sync_fn)
```

**判别口诀**：`time.sleep` → `asyncio.sleep`；`requests` → `httpx`；`recv` → 非阻塞 + 事件循环；CPU 重计算 → `asyncio.to_thread` 或进程池。**"事件循环线程里只有 await 和 CPU 快操作"**是异步服务的第一铁律（10 章避坑清单再强调）。

这条铁律的**检测手段**也顺带给齐：线上"周期性卡顿"的请求——用 py-spy 采样看事件循环线程当前栈（是否停在同步调用上）；结合"卡顿间隔 = 某阻塞调用的时长"这个特征，就能把嫌疑锁定到具体调用点。**"先采样再怀疑"是异步排障与同步排障最大的方法论差异**——同步靠栈一步到位，异步要先抓"循环停在哪"。

> 🎯 **核心要点**：事件循环 = 就绪队列 + 等待注册表 + 多路复用器——挂起只是"移入等待表"，单线程十万连接的结构秘密；uvloop 用 C 实现追平 Node，libuv 线程池与 to_thread 是"循环管网络、池管阻塞"的通用分工；阻塞调用混入循环 = 全服务停摆的灾难模型，是第一铁律的由来。

## 7. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 循环三角色（就绪队列/等待注册表/复用器）解释"挂起不占线程"；调度顺序的两陷阱是长任务饿死与微任务反转；
> 2. asyncio 调度链"注册→事件→唤醒→恢复"四环节是排障地图；uvloop 把 Python 循环性能拉到 Node 档；
> 3. 阻塞混入循环 = 全服务停摆；铁律：循环线程里只有 await 与 CPU 快操作。

**思考题**：

1. "挂起"与"暂停"的本质区别？（→ 1 节）
2. 微任务递归为什么会饿死宏任务？（→ 2 节）
3. Node 的线程池默认多大？瓶颈在哪类场景？（→ 5 节）
4. 事件循环线程里出现 100ms CPU 计算会怎样？（→ 6 节）

---

**下一模块**：[08-背压与响应式流：异步系统的流量控制](08-背压与响应式流：异步系统的流量控制.md)｜**返回总览**：[00-异步编程全方位剖析知识体系总览](00-异步编程全方位剖析知识体系总览.md)

---

## 参考来源

- [Python 异步 + FastAPI 深潜体系（事件循环章节）](../../../03-AI大模型应用开发/01-Python语言/Python 异步 + FastAPI/00-Python异步与FastAPI知识体系总览.md)——asyncio 细节
- [Node.js 异步编程新范式（OSCHINA）](https://my.oschina.net/emacs_7987128/blog/19321470)——libuv 与线程池
- [io_uring Explained（2026）](https://dargslan.com/blog/io-uring-explained-future-linux-io-performance-2026)——uvloop 的 io_uring 后端背景
