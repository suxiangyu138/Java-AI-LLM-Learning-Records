# 01 asyncio 事件循环与异步核心
> 异步的地基：事件循环运行模型 + Task 生命周期 + 取消与超时 + Python 3.14 的变化

## 📚 目录
1. [事件循环：单线程如何"并发"](#1-事件循环单线程如何并发)
2. [协程 / Task / Future 三件套](#2-协程--task--future-三件套)
3. [调度细节：await 时发生了什么](#3-调度细节await-时发生了什么)
4. [任务取消与 shield](#4-任务取消与-shield)
5. [超时：asyncio.timeout 与 wait_for](#5-超时asynciotimeout-与-wait_for)
6. [结构化并发：TaskGroup](#6-结构化并发taskgroup)
7. [asyncio.run 与 Runner 的正确用法](#7-asynciorun-与-runner-的正确用法)
8. [Python 3.14 异步变化全景](#8-python-314-异步变化全景)
9. [核心要点](#9-核心要点)

---

## 1. 事件循环：单线程如何"并发"

### 1.1 运行模型

```text
┌─────────────────── 单线程事件循环 ───────────────────┐
│                                                      │
│  就绪队列: [协程A] [协程B] [协程C]                     │
│                                                      │
│  循环： 取一个就绪协程 → 执行到 await → 挂起           │
│         │                          │                │
│         └─ 回调就绪(IO完成/计时到) ←─┘                │
│         IO 完成信号来自：select/poll/epoll 监听       │
└──────────────────────────────────────────────────────┘

关键：协程不是"并行执行"，是"轮流执行到 await 就挂起"。
并发（interleaving）≠ 并行（parallelism）。
```

| 角色 | 职责 |
|------|------|
| 事件循环（loop） | 调度器：维护就绪队列 + 就绪回调 |
| 协程（coroutine） | 用户代码，await 处让出 |
| Task | 包住协程的调度单元，挂到事件循环上 |
| Future | 结果容器（IO 完成时由回调填充） |
| epoll/select | 事件循环的"眼睛"，监听 socket 可读/可写/超时 |

> 🎯 **核心要点**：异步高并发的秘密 = **"等待的时间交给别人"**。一个请求的 IO 等待（网络往返常占 90%+ 耗时）期间，事件循环去跑其他请求的代码——单线程也能扛千级并发，代价是不能有长时间阻塞操作（§4 反模式）。

### 1.2 IO 密集 vs CPU 密集（适用性判断）

| 场景 | 异步效果 | 说明 |
|------|:---:|------|
| 网络请求 / 数据库 / 文件 | ⭐⭐⭐ | 等待时间长，让出收益巨大 |
| 计算 / 正则 / 解析大量数据 | ⭐ | 不等待，让出无收益，还增加开销 |
| 混合 | ⭐⭐ | CPU 部分用 to_thread / 进程池（02 章） |

> 💡 判断口诀：**"如果这段代码是等外部结果，用 async；如果是在算，别用 async 骗自己"**。

## 2. 协程 / Task / Future 三件套

```python
import asyncio


async def fetch(name):            # ① 协程：async def
    await asyncio.sleep(0.1)      # await 挂起点
    return f"data-{name}"

# ② 协程对象：调用不执行！
coro = fetch("a")                 # 只是创建对象，函数体一行未跑

# ③ Task：把协程挂上事件循环，成为调度单元
task = asyncio.create_task(fetch("b"))   # 立即入队，稍后执行
await task                        # 等结果

# ④ Future：底层结果容器（一般不直接用）
#    Task 是 Future 的子类；await future 等价于等结果被填充
```

| 对象 | 谁创建 | 能 await 吗 | 说明 |
|------|--------|:---:|------|
| 协程对象 | `fetch()` 调用 | ✅ | 不 await 会告警 `never awaited` |
| Task | `asyncio.create_task()` | ✅ | 并发单元，自动调度 |
| Future | 框架内部 | ✅ | 结果占位符，Task 是它的子类 |

> ⚠️ 三大新手坑：(1) `fetch()` 不 await 不 create_task → 协程永远不执行（还能漏出 `RuntimeWarning: coroutine was never awaited`）；(2) `create_task` 必须在**运行中的事件循环**里调用（在 `asyncio.run` 的协程内部才安全）；(3) 创建了 Task 就要负责等它/取消它——挂着不等的 Task 在程序退出时可能被静默丢弃（3.14 起改为显式警告）。

## 3. 调度细节：await 时发生了什么

```python
async def worker(i):
    print(f"[{i}] 开始")
    await asyncio.sleep(0.1)     # ← 挂起点：进入 sleep，让出 CPU
    print(f"[{i}] 恢复")          # 0.1 秒后事件循环把控制权交回来
    return i

async def main():
    # 两个 worker 几乎同时"开始"，0.1s 后几乎同时"恢复"
    await asyncio.gather(worker(1), worker(2))
```

**调度时序**：

```text
t=0.000  worker1 开始（执行到 sleep，注册 0.1s 定时回调，挂起）
t=0.001  worker2 开始（同上）
t=0.100  worker1 恢复（定时器到期，回到就绪队列，被调度执行）→ 结束
t=0.101  worker2 恢复 → 结束
总耗时 ≈ 0.101s，而不是 0.2s —— 这就是"并发"的收益
```

**await 的完整含义**：

```text
await expr 展开为三步：
 ① 取得 expr 的可等待对象（协程/Task/Future）
 ② 挂起当前协程，事件循环调度其他就绪任务
 ③ 目标完成（结果填充/异常抛出）→ 当前协程回到就绪队列继续
```

> 🎯 **核心要点**：`await` 不是"等待"的意思，是"**让出 + 注册回调**"。理解这一点，就能解释为什么异步服务能同时处理上千请求——每个请求的协程都在"等 IO"时让出了。

## 4. 任务取消与 shield

### 4.1 取消机制

```python
task = asyncio.create_task(fetch("a"))
task.cancel()                    # 请求取消：任务下次进入挂起点时抛 CancelledError

try:
    await task
except asyncio.CancelledError:
    print("被取消了")
```

| 关键点 | 说明 |
|--------|------|
| 取消是"协作式" | `cancel()` 不立即终止，在协程下一个 await 处抛 `CancelledError` |
| CancelledError 继承 BaseException | `except Exception` 拦不住它（3.8+） |
| 取消会向上传播 | 外层 `await` 该任务的代码同时收到取消（gather 的 return_exceptions 可挡） |
| 清理要在 finally | 资源释放写 `finally`，取消时也要执行 |

```python
async def job():
    try:
        await long_io()
    finally:
        await cleanup()          # 取消/异常都执行清理
```

### 4.2 shield：保护不可取消操作

```python
# 取消保护：外部取消不传播给内部关键操作
async def main():
    task = asyncio.create_task(shield_critical())
    task.cancel()                # 取消 shield 外层，但 critical() 继续跑
    try:
        await task
    except asyncio.CancelledError:
        print("外层取消了，但关键操作还在跑")

async def shield_critical():
    await asyncio.shield(critical_commit())   # ⚠️ 结果是"取消被推迟到 critical 完成后"
```

> ⚠️ shield 的语义陷阱：`shield` 包住的协程**不是真的不可取消**——外层取消时，`await shield(...)` 立即抛 CancelledError，**被 shield 的协程继续在后台运行**（成孤儿任务）。正确用法是 3.11+ 的 `TaskGroup` + 手动管理，或确认"被 shield 的操作可安全后台完成"。

## 5. 超时：asyncio.timeout 与 wait_for

```python
# 3.11+ 推荐：asyncio.timeout 上下文管理器
try:
    async with asyncio.timeout(2):
        result = await slow_api()          # 超时抛 TimeoutError
except TimeoutError:
    print("调用超时")

# 传统：asyncio.wait_for（等价，但需要任务引用时用）
result = await asyncio.wait_for(slow_api(), timeout=2)
```

| API | 行为 | 适用 |
|-----|------|------|
| `asyncio.timeout(sec)` | 超时抛 `TimeoutError`，**自动取消被包协程** | ⭐ 首选（3.11+） |
| `asyncio.wait_for` | 同上 | 老代码/需 Task 对象 |
| `asyncio.timeout_at(deadline)` | 绝对时间点（循环调用的截止线） | 多次调用的总预算 |
| `loop.sock_*` 内部超时 | socket 级 | 底层库 |

> 💡 超时是异步代码的**必备卫生习惯**：任何 await 外部 IO（HTTP/DB/第三方）都必须有超时，否则一个卡死的上游会把整个事件循环的服务拖满（FastAPI 一章会反复强调）。

## 6. 结构化并发：TaskGroup

3.11+ 的结构化并发（对标 trio/anyio 的 nursery）：

```python
async def main():
    async with asyncio.TaskGroup() as tg:
        t1 = tg.create_task(fetch("a"))       # 自动创建并调度
        t2 = tg.create_task(fetch("b"))
        t3 = tg.create_task(fetch("c"))
    # 退出 with 时：全部任务已完成
    # 任一任务抛异常 → 组内其他任务被自动取消，异常向上传播
    print(t1.result(), t2.result(), t3.result())
```

| 特性 | TaskGroup | gather / create_task 裸用 |
|------|:---:|:---:|
| 失败即取消全体 | ✅（任一失败，全组取消） | ❌（其他任务继续跑） |
| 等待全部完成 | ✅（with 退出即屏障） | 需手动 gather |
| 异常处理 | 组内首个异常抛出 | 需 return_exceptions 分析 |
| 资源安全 | ✅（退出必回收） | 容易泄漏孤儿任务 |

> 🎯 **核心要点**：2026 年写并发**默认 TaskGroup**——"结构化并发"把"创建/等待/清理/失败传播"绑定在一个代码块里，杜绝孤儿任务与半途而废的并发。gather 仅保留在"需要部分结果、失败不扩散"的场合。

## 7. asyncio.run 与 Runner 的正确用法

```python
# 标准入口：asyncio.run（永远从这里进）
asyncio.run(main())              # 创建循环 → 运行 → 关闭 → 清理

# 同一循环内多次运行（REPL/测试/服务内嵌）
from asyncio import Runner

with Runner() as runner:
    r1 = runner.run(main(1))     # 复用同一事件循环
    r2 = runner.run(main(2))

# 交互式调试：python -m asyncio 进入 asyncio REPL（可直接 await）
```

| 场景 | 正确做法 |
|------|---------|
| 脚本入口 | `asyncio.run()`（每次创建/关闭循环） |
| 多次运行/框架内嵌 | `asyncio.Runner`（复用循环） |
| REPL/调试 | `python -m asyncio` |
| 其他线程提交任务 | `asyncio.run_coroutine_threadsafe()`（02 章线程桥接） |
| 获取当前循环 | `asyncio.get_running_loop()`（**禁止** get_event_loop 隐式创建） |

> ⚠️ **3.14 铁律**：`asyncio.get_event_loop()` 在无当前循环时**抛 RuntimeError**（不再隐式创建，主线程也一样）；在协程内部调用直接废弃（deprecated）——一律改用 `get_running_loop()`。老库（如 grpc.aio）因此报错，升级项目时先排查。

## 8. Python 3.14 异步变化全景

**Python 3.14（2025-10-07 发布）对 asyncio 是"改规则"的一版**：

| 变化 | 内容 | 对开发者的影响 |
|------|------|---------------|
| **自由线程一等公民** | asyncio 在 free-threaded 构建（PEP 703，GIL 可关）下线程安全，支持多核并行事件循环 | 线程内各自 `asyncio.run`，任务不跨线程共享（官方新增 asyncio-threading 文档页） |
| `get_event_loop()` 不再隐式创建 | 无当前循环直接 RuntimeError | 全部改用 `asyncio.run()` / `get_running_loop()` |
| 事件循环策略弃用 | `get_event_loop_policy` / `DefaultEventLoopPolicy` 进入弃用通道（3.16 移除） | 别再用 Policy 定制循环 |
| 子进程 watcher 移除 | `MultiLoopChildWatcher` 等全部删除 | 用 `loop.subprocess_*` 标准方式 |
| start_tls 修复 | 流升级 TLS 时缓冲数据不再丢失（gh-142352） | 自定义协议库受益 |
| never-awaited 警告 | 未等待的协程/Task 清理更严格 | 代码审查多查 create_task 丢任务 |

**3.15（2026-10 发布）预告**：PEP 686 默认 UTF-8（影响文件编码），异步核心继续按弃用通道收尾。

> 🎯 **核心要点**：3.14 的异步主题 = "**线程安全化 + 收紧 API**"。写新代码只要记住三条：`asyncio.run` 进、`get_running_loop` 取、任务归 TaskGroup——就与 3.14 完全兼容。

## 9. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | 异步 = 并发（轮流让出），不是并行；IO 密集才值得用 |
| 2 | 协程调用不执行；create_task 才调度；Task 必须被 await/取消 |
| 3 | await = 让出 + 注册回调；事件循环靠 epoll 监听就绪 |
| 4 | 取消是协作式的（下一 await 处抛 CancelledError），清理写 finally |
| 5 | 外部 IO 一律配 asyncio.timeout，超时自动取消 |
| 6 | 并发默认 TaskGroup（结构化并发：失败即全取消、退出即屏障） |
| 7 | 入口 asyncio.run / Runner；取循环用 get_running_loop |
| 8 | 3.14：get_event_loop 不再隐式创建、循环策略弃用、自由线程一等公民 |

---

**下一模块**：[02-并发控制与异步设计模式](02-并发控制与异步设计模式.md) / **返回总览**：[00-Python异步与FastAPI知识体系总览](00-Python异步与FastAPI知识体系总览.md)

## 参考来源

- [Python 官方 asyncio 文档](https://docs.python.org/3/library/asyncio.html)（3.14）
- [Python 官方：asyncio and free-threaded Python](https://docs.python.org/3/library/asyncio-threading.html)
- [PEP 703：Making the Global Interpreter Lock Optional](https://peps.python.org/pep-0703/)
- [Python 3.14 弃用清单](https://docs.python.org/3/deprecations/pending-removal-in-3.14.html)
- [Quansight：Scaling asyncio on Free-Threaded Python](https://labs.quansight.org/blog/)（3.14 自由线程基准）
- [Python高级语法/07-协程与asyncio深度.md](../Python高级语法/07-协程与asyncio深度.md)（语法入门版）
