# 12 - 并发模型与 GIL

> GIL 不是「Python 不能并发」，而是「同一进程内同一时刻只有一个线程执行字节码」。搞清这句话的边界，就知道该选线程、进程还是协程

---

## 📚 目录

1. [GIL 的本质](#1-gil-的本质)
2. [三种并发模型选型](#2-三种并发模型选型)
3. [多线程 threading](#3-多线程-threading)
4. [多进程 multiprocessing](#4-多进程-multiprocessing)
5. [concurrent.futures 统一接口](#5-concurrentfutures-统一接口)
6. [asyncio 与协程](#6-asyncio-与协程)
7. [async 底层原理](#7-async-底层原理)
8. [无 GIL 的未来](#8-无-gil-的未来)
9. [核心要点回顾](#9-核心要点回顾)

---

## 1. GIL 的本质

**GIL（Global Interpreter Lock）= 一把保护解释器内部状态的互斥锁**。任何线程执行 Python 字节码前必须持有它。

```text
   4 核 CPU，4 个 Python 线程：

   时间 →
   T1  ███░░░░░░░░░████░░░░░░░░   ← 持有 GIL 时才在跑
   T2  ░░░████░░░░░░░░░░░░████░
   T3  ░░░░░░░███░░░░░░░░░░░░░░
   T4  ░░░░░░░░░░░████░░░░░░░░░
       └─ 同一时刻只有一条实线 → CPU 密集任务并行度恒为 1
```

### 1.1 为什么存在

| 原因 | 说明 |
|------|------|
| **保护引用计数** | `ob_refcnt` 的 `++/--` 不是原子的；无锁会导致计数错乱 → 提前释放或泄漏 |
| **简化 C 扩展** | 扩展作者不用考虑线程安全，numpy/lxml 生态因此繁荣 |
| **单线程性能** | 一把全局锁比给每个对象加锁快得多（细粒度锁开销巨大） |
| **历史包袱** | 1992 年设计，当时多核不是主流 |

### 1.2 GIL 什么时候释放

```text
   ① 主动释放 —— 关键！
      · I/O 操作前（read/write/socket/sleep）
      · 调用 C 扩展且扩展显式释放（numpy 大矩阵运算、torch、压缩、加密）
      · time.sleep()

   ② 被动切换
      · 3.2+：每 sys.getswitchinterval() 秒（默认 5ms）请求切换
      · 3.1 及之前：每 100 条字节码（更容易饿死）
```

```python
import sys
print(sys.getswitchinterval())      # 0.005 秒
sys.setswitchinterval(0.001)        # 调小 → 切换更频繁（响应性↑，吞吐↓）
```

### 1.3 实测对比

```python
import time, threading, multiprocessing

def cpu_bound(n=30_000_000):
    x = 0
    while x < n: x += 1

def io_bound():
    time.sleep(1)                    # 模拟网络/磁盘等待

def bench(fn, worker_cls, n=4):
    t = time.perf_counter()
    ws = [worker_cls(target=fn) for _ in range(n)]
    for w in ws: w.start()
    for w in ws: w.join()
    return time.perf_counter() - t

if __name__ == "__main__":
    # CPU 密集
    print("单线程串行 :", sum(bench(cpu_bound, threading.Thread, 1) for _ in range(4)))
    print("4 线程     :", bench(cpu_bound, threading.Thread, 4))       # ≈ 串行，甚至更慢
    print("4 进程     :", bench(cpu_bound, multiprocessing.Process, 4)) # ≈ 串行/核数

    # I/O 密集
    print("4 线程 I/O :", bench(io_bound, threading.Thread, 4))        # ≈ 1 秒（4 倍加速）
```

典型结果：

| 任务 | 串行 | 4 线程 | 4 进程 |
|------|:----:|:-----:|:-----:|
| CPU 密集（纯 Python） | 4.0s | **4.3s**（更慢，切换开销） | **1.1s** |
| I/O 密集 | 4.0s | **1.0s** | 1.0s（但开销大得多） |
| numpy 大矩阵运算 | 4.0s | **1.2s**（C 层释放了 GIL） | 1.1s |

> 🎯 **GIL 只锁字节码执行，不锁 I/O 等待，也不锁「已释放 GIL 的 C 代码」**。所以 numpy/PyTorch 的多线程是真并行——AI 场景里 GIL 的杀伤力远小于纯 Python 计算场景。

---

## 2. 三种并发模型选型

```text
   ┌────────────────────────────────────────────────────────────┐
   │  任务是什么类型？                                            │
   └────────────────────────────────────────────────────────────┘
        │
        ├─ CPU 密集（纯 Python 计算、图像处理、加解密）
        │     → multiprocessing / ProcessPoolExecutor
        │     → 或改用 numpy/Cython/Rust 扩展绕开 GIL
        │
        ├─ I/O 密集 + 少量任务（< 数百）
        │     → threading / ThreadPoolExecutor（改造成本最低）
        │
        ├─ I/O 密集 + 海量任务（数千~数万并发连接）
        │     → asyncio（内存占用低一到两个数量级）
        │
        └─ 混合型
              → asyncio 主循环 + run_in_executor 卸载 CPU 部分
```

| 维度 | 多线程 | 多进程 | 协程（asyncio） |
|------|-------|-------|---------------|
| 并行 CPU | ❌（GIL） | ✅ | ❌ |
| 并发 I/O | ✅ | ✅ | ✅✅ |
| 单位内存 | ~8MB 栈 | ~30MB+ | **~KB 级** |
| 创建开销 | 中 | 高 | 极低 |
| 切换开销 | 中（OS 调度） | 高 | **极低（用户态）** |
| 切换时机 | 抢占式（随时） | 抢占式 | **协作式（仅 `await`）** |
| 数据共享 | 直接共享（需锁） | IPC/序列化 | 直接共享（无需锁） |
| 竞态风险 | 高 | 低 | **低**（切换点显式可见） |
| 调试难度 | 高 | 中 | 中 |
| 生态改造成本 | 低 | 低 | **高（需全链路 async 库）** |
| 上限量级 | 数百 | 核数 × 2 | **数万** |

> 💡 与 Java 对照：Java 线程是真并行（无 GIL），所以 Java 里「线程池打天下」；Python 里必须先分清任务类型。Java 21 的虚拟线程（Loom）在定位上最接近 asyncio 协程——都是「海量 I/O 并发、极低单位成本」，但虚拟线程对业务代码透明，Python 协程需要显式 `async/await`（即所谓「函数染色」问题）。

---

## 3. 多线程 threading

```python
import threading, time
from queue import Queue

# 基础用法
def worker(name, delay):
    for i in range(3):
        time.sleep(delay)
        print(f"[{name}] step {i}")

t = threading.Thread(target=worker, args=("A", 0.1), daemon=True)
t.start()
t.join(timeout=5)
print(t.is_alive(), threading.active_count(), threading.current_thread().name)
```

### 3.1 竞态条件与同步原语

```python
# ❌ 竞态：count += 1 不是原子操作
count = 0
def unsafe():
    global count
    for _ in range(100_000): count += 1     # LOAD → ADD → STORE，可被打断

ts = [threading.Thread(target=unsafe) for _ in range(5)]
for t in ts: t.start()
for t in ts: t.join()
print(count)          # 期望 500000，实际常常更小
```

```python
# ✅ 加锁
lock = threading.Lock()
count = 0
def safe():
    global count
    for _ in range(100_000):
        with lock: count += 1               # 用 with，异常也能释放
```

| 原语 | 用途 | 关键点 |
|------|------|-------|
| `Lock` | 互斥 | 不可重入，同线程二次 acquire 会死锁 |
| `RLock` | 可重入互斥 | 同线程可多次 acquire（需同样次数 release） |
| `Semaphore(n)` | 限制并发数 | 如「最多 5 个并发 API 请求」 |
| `Event` | 一次性信号 | `set()` / `wait()` / `clear()` |
| `Condition` | 等待条件成立 | `wait()` / `notify()`，实现生产者-消费者 |
| `Barrier(n)` | 集合点 | n 个线程都到达才继续 |
| `local()` | 线程局部存储 | 每线程独立副本，等价 Java `ThreadLocal` |

```python
# Semaphore 限流：控制对 LLM API 的并发请求数
api_sem = threading.Semaphore(5)
def call_llm(prompt):
    with api_sem:                            # 最多 5 个线程同时进入
        return http_post(prompt)

# threading.local()：每线程独立的连接
_local = threading.local()
def get_conn():
    if not hasattr(_local, "conn"):
        _local.conn = create_connection()
    return _local.conn
```

### 3.2 Queue：线程间通信的正确方式

```python
from queue import Queue, Empty
import threading

task_q, result_q = Queue(maxsize=100), Queue()
SENTINEL = object()

def consumer():
    while True:
        item = task_q.get()
        try:
            if item is SENTINEL: break
            result_q.put(item * 2)
        finally:
            task_q.task_done()

workers = [threading.Thread(target=consumer, daemon=True) for _ in range(4)]
for w in workers: w.start()

for i in range(20): task_q.put(i)
task_q.join()                                # 等所有任务被处理
for _ in workers: task_q.put(SENTINEL)       # 毒丸关闭
for w in workers: w.join()
print(sorted(result_q.queue))
```

> 🎯 `Queue` 内部已加锁，是线程安全的。**能用 Queue 传递数据就不要用共享变量 + 锁**——这条原则和 Go 的「用 channel 通信」是一个道理。

### 3.3 常见误区

```python
# ① GIL 不保证复合操作的原子性
d = {}
d[k] = d.get(k, 0) + 1        # ❌ 非原子（三步）
with lock: d[k] = d.get(k, 0) + 1     # ✅
# 单条 dict/list 方法（append/pop/update）本身是原子的

# ② daemon 线程在主线程退出时被强杀，finally 可能不执行
#    → 需要清理的线程别设 daemon=True，用 Event 优雅退出
stop = threading.Event()
def graceful():
    while not stop.is_set():
        do_work()
        stop.wait(1)          # 可被立即打断的 sleep
stop.set()

# ③ 线程里的异常不会传播到主线程
def boom(): raise ValueError("silent")
t = threading.Thread(target=boom); t.start(); t.join()
print("主线程完全不知道出错了")
# → 用 ThreadPoolExecutor，future.result() 会重新抛出异常
```

---

## 4. 多进程 multiprocessing

```python
import multiprocessing as mp

def work(x): return x * x

if __name__ == "__main__":                   # ⚠️ Windows/macOS 必须有这行
    with mp.Pool(processes=4) as pool:
        print(pool.map(work, range(10)))                    # 阻塞、有序
        print(pool.imap_unordered(work, range(10)))         # 惰性、乱序（先到先出）
        r = pool.apply_async(work, (5,))
        print(r.get(timeout=3))
```

### 4.1 三种启动方式

| 方式 | 平台 | 机制 | 特点 |
|------|------|------|------|
| `fork` | Linux/macOS | 复制父进程内存（CoW） | 最快；**但多线程环境下 fork 不安全**（可能死锁） |
| `spawn` | 全平台 | 新解释器 + 重新 import 模块 | 最安全，Windows/macOS 3.8+ 默认；启动慢 |
| `forkserver` | Linux | 单线程服务进程代为 fork | 兼顾安全与速度 |

```python
mp.set_start_method("spawn", force=True)     # 显式指定
# Python 3.14 起 Linux 默认也改为 forkserver（fork 的线程安全隐患）
```

> ⚠️ `spawn` 会重新 import 主模块，所以顶层代码必须放在 `if __name__ == "__main__":` 里，否则无限递归创建进程。目标函数和参数必须**可 pickle**（lambda 不行，用 `functools.partial` 或模块级函数）。

### 4.2 进程间通信

```python
# ① Queue / Pipe
q = mp.Queue()
parent_conn, child_conn = mp.Pipe()

# ② 共享内存（简单类型）
counter = mp.Value("i", 0)                   # 'i'=int, 'd'=double
arr = mp.Array("d", [0.0] * 100)
with counter.get_lock():                     # 需自己加锁
    counter.value += 1

# ③ Manager：支持复杂对象（代价：每次访问都走 IPC，慢）
with mp.Manager() as m:
    shared_dict, shared_list = m.dict(), m.list()
    shared_dict["key"] = "value"

# ④ shared_memory（3.8+）：零拷贝共享大数组，AI 场景最有用
from multiprocessing import shared_memory
import numpy as np

a = np.arange(1_000_000, dtype=np.float64)
shm = shared_memory.SharedMemory(create=True, size=a.nbytes)
buf = np.ndarray(a.shape, dtype=a.dtype, buffer=shm.buf)
buf[:] = a[:]                                # 写入共享内存
# 子进程里：shm2 = shared_memory.SharedMemory(name=shm.name) → 零拷贝读取
shm.close(); shm.unlink()
```

| 方式 | 数据量 | 性能 | 适用 |
|------|-------|------|------|
| `Queue` / `Pipe` | 小 | 需 pickle 序列化 | 任务分发、结果回收 |
| `Value` / `Array` | 小固定 | 快 | 计数器、标志位 |
| `Manager` | 中 | **慢**（每次访问走代理） | 需要 dict/list 语义 |
| `shared_memory` | **大** | **最快（零拷贝）** | numpy 数组、模型权重、图像批次 |

### 4.3 进程池的真实开销

```python
# ❌ 反面教材：任务太细，序列化开销远超计算
with mp.Pool(4) as p:
    p.map(lambda x: x + 1, range(1_000_000))    # 还会因为 lambda 无法 pickle 直接报错

# ✅ 用 chunksize 摊薄开销
with mp.Pool(4) as p:
    p.map(heavy_compute, range(1_000_000), chunksize=1000)
```

> 🎯 进程池的成本 = 进程创建 + 参数 pickle + 结果 pickle + IPC。**单任务耗时必须显著大于这些开销**（经验值：> 10ms）才值得用多进程。传大 numpy 数组时优先 `shared_memory`，别让 pickle 拷两遍。

---

## 5. concurrent.futures 统一接口

线程池和进程池共用同一套 API，**切换只需改一个类名**——这是首选的高层接口。

```python
from concurrent.futures import (ThreadPoolExecutor, ProcessPoolExecutor,
                                as_completed, wait, FIRST_COMPLETED)
import time

def fetch(url):
    time.sleep(0.2)
    if "bad" in url: raise ValueError(f"failed: {url}")
    return f"{url} → 200"

urls = [f"api/{i}" for i in range(8)] + ["api/bad"]

with ThreadPoolExecutor(max_workers=5, thread_name_prefix="fetch") as ex:
    # ① map：有序、异常在迭代时抛出
    for r in ex.map(fetch, urls[:3]): print(r)

    # ② submit + as_completed：完成即处理，可单独处理每个异常（推荐）
    futs = {ex.submit(fetch, u): u for u in urls}
    for f in as_completed(futs, timeout=10):
        url = futs[f]
        try:
            print("✓", f.result())
        except Exception as e:
            print("✗", url, e)                # 单个失败不影响其他

    # ③ wait：等待部分完成
    done, pending = wait(futs, timeout=1, return_when=FIRST_COMPLETED)
```

```python
# Future 的接口
f = ex.submit(fetch, "api/1")
f.done(); f.running(); f.cancel()
f.result(timeout=5)          # 阻塞取结果，任务里的异常在这里重新抛出
f.exception()                # 取异常对象而不抛出
f.add_done_callback(lambda fut: print("完成:", fut.result()))
```

| 特性 | `map` | `submit` + `as_completed` |
|------|-------|--------------------------|
| 结果顺序 | 与输入一致 | 完成顺序 |
| 异常处理 | 迭代到该项时抛出，中断整个循环 | **可逐个 try/except** |
| 惰性 | 是（但会提前提交全部任务） | 手动控制 |
| 适用 | 简单批处理 | **生产代码**：需要容错和进度反馈 |

```python
# 实战：并发调用 LLM API + 重试 + 进度条
from concurrent.futures import ThreadPoolExecutor, as_completed
from tqdm import tqdm

def call_with_retry(prompt, retries=3):
    for i in range(retries):
        try: return llm_client.complete(prompt)
        except RateLimitError:
            time.sleep(2 ** i)
    raise RuntimeError(f"重试 {retries} 次仍失败")

def batch_infer(prompts, workers=8):
    results = [None] * len(prompts)
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futs = {ex.submit(call_with_retry, p): i for i, p in enumerate(prompts)}
        for f in tqdm(as_completed(futs), total=len(futs)):
            i = futs[f]
            try: results[i] = f.result()
            except Exception as e: results[i] = f"ERROR: {e}"
    return results                            # 顺序与输入一致
```

> 💡 `ThreadPoolExecutor` 默认 `max_workers = min(32, cpu_count + 4)`；`ProcessPoolExecutor` 默认 `cpu_count`。I/O 密集任务可以放心设成几十甚至上百，它不受 CPU 核数限制。

---

## 6. asyncio 与协程

### 6.1 核心概念

```python
import asyncio, time

async def fetch(name, delay):                # 协程函数
    print(f"{name} 开始")
    await asyncio.sleep(delay)               # 挂起，让出控制权（不阻塞事件循环）
    print(f"{name} 完成")
    return f"{name}-result"

async def main():
    t = time.perf_counter()
    # ❌ 串行：一个 await 一个
    # r1 = await fetch("A", 1); r2 = await fetch("B", 1)   → 2 秒

    # ✅ 并发：gather
    r = await asyncio.gather(fetch("A", 1), fetch("B", 1), fetch("C", 1))
    print(r, f"{time.perf_counter() - t:.2f}s")            # → 1 秒

asyncio.run(main())                          # 3.7+ 的标准入口
```

| 概念 | 含义 |
|------|------|
| 协程函数 | `async def` 定义的函数 |
| 协程对象 | 调用协程函数的返回值（**不会自动执行**，必须 await 或包成 Task） |
| `await` | 挂起当前协程，等待可等待对象完成；**唯一的切换点** |
| Task | 被事件循环调度的协程包装，`create_task` 后立即开始排队执行 |
| Future | 底层的结果占位符，Task 是它的子类 |
| 事件循环 | 单线程调度器，轮流推进所有就绪的 Task |

```python
# 协程对象不 await 就什么都不会发生
async def f(): print("跑了")
c = f()                                      # 无输出，只创建了协程对象
# RuntimeWarning: coroutine 'f' was never awaited
await c                                      # 现在才执行
```

### 6.2 并发编排

```python
# ① gather：全部并发，按输入顺序返回结果
results = await asyncio.gather(*[fetch(f"t{i}", 1) for i in range(5)])
results = await asyncio.gather(*tasks, return_exceptions=True)   # 异常作为结果返回，不中断其他

# ② TaskGroup（3.11+，推荐）：结构化并发，任一失败则取消全部
async with asyncio.TaskGroup() as tg:
    t1 = tg.create_task(fetch("A", 1))
    t2 = tg.create_task(fetch("B", 2))
# 退出 with 时全部完成；出错抛 ExceptionGroup
print(t1.result(), t2.result())

# ③ create_task：手动管理
task = asyncio.create_task(fetch("bg", 5))
await asyncio.sleep(0.1)
task.cancel()                                # 取消
try: await task
except asyncio.CancelledError: print("已取消")

# ④ as_completed：完成即处理
for coro in asyncio.as_completed([fetch("A",3), fetch("B",1)]):
    print(await coro)                        # B 先出来

# ⑤ 超时
try:
    async with asyncio.timeout(2):           # 3.11+
        await fetch("slow", 10)
except TimeoutError: print("超时")

result = await asyncio.wait_for(fetch("x", 10), timeout=2)   # 老写法
```

```python
# ⑥ 限流：Semaphore 控制并发数（调 LLM API 必备）
sem = asyncio.Semaphore(10)
async def limited_call(prompt):
    async with sem:                          # 最多 10 个并发在飞
        return await client.acomplete(prompt)

results = await asyncio.gather(*[limited_call(p) for p in prompts])
```

### 6.3 不要阻塞事件循环

```python
# ❌ 致命错误：同步阻塞调用冻结整个事件循环
async def bad():
    time.sleep(1)                            # 所有其他协程一起卡 1 秒
    requests.get(url)                        # 同上
    heavy_cpu_compute()                      # 同上

# ✅ 正确做法
async def good():
    await asyncio.sleep(1)                                  # 异步 sleep
    async with aiohttp.ClientSession() as s:                # 异步 HTTP
        async with s.get(url) as r: await r.text()
    # 或用 httpx.AsyncClient

    loop = asyncio.get_running_loop()
    await loop.run_in_executor(None, blocking_io)           # 卸载到线程池
    await asyncio.to_thread(blocking_io)                    # 3.9+ 更简洁

    with ProcessPoolExecutor() as pool:                     # CPU 密集卸载到进程池
        await loop.run_in_executor(pool, cpu_heavy, data)
```

| 同步库 | 异步替代 |
|-------|---------|
| `requests` | `aiohttp` / `httpx.AsyncClient` |
| `time.sleep` | `asyncio.sleep` |
| `open()` | `aiofiles` |
| `psycopg2` / `pymysql` | `asyncpg` / `aiomysql` |
| `redis` | `redis.asyncio` |
| `openai`（同步） | `AsyncOpenAI` / `AsyncAnthropic` |
| 任何没有异步版本的库 | `asyncio.to_thread(...)` 兜底 |

### 6.4 异步迭代与异步上下文

```python
# 异步生成器：流式处理 LLM 响应
async def stream_tokens(prompt):
    async with client.stream(prompt) as resp:
        async for chunk in resp:
            yield chunk.text

async for token in stream_tokens("讲个故事"):
    print(token, end="", flush=True)

# 异步推导式
results = [x async for x in stream_tokens(p)]
lengths = [len(await fetch(u)) for u in urls]     # await 也能用在推导式里

# 异步上下文管理器（详见 08 章）
class AsyncDBPool:
    async def __aenter__(self):
        self.pool = await asyncpg.create_pool(DSN); return self.pool
    async def __aexit__(self, *exc):
        await self.pool.close()

async with AsyncDBPool() as pool:
    rows = await pool.fetch("SELECT 1")
```

---

## 7. async 底层原理

**协程本质上就是生成器**——`await` 是 `yield from` 的语义升级版。

```python
# 用生成器手写一个「事件循环」，理解 async 的本质
def task(name, n):
    for i in range(n):
        print(f"{name} 第 {i} 步")
        yield                                # ← 相当于 await：主动让出
    return f"{name} 完成"

def naive_loop(tasks):
    tasks = list(tasks)
    while tasks:
        t = tasks.pop(0)
        try:
            next(t)                          # 推进一步
            tasks.append(t)                  # 未完成 → 放回队尾
        except StopIteration as e:
            print("✓", e.value)              # 完成，取返回值

naive_loop([task("A", 2), task("B", 3)])
# A 第0步 / B 第0步 / A 第1步 / B 第1步 / ✓A完成 / B第2步 / ✓B完成
```

```text
   真实 asyncio 的结构：

   ┌──────────────────────────────────────────────────────┐
   │                    Event Loop（单线程）                │
   │                                                       │
   │   ready 队列 ──→ 取一个 Task ──→ coro.send(value)      │
   │      ↑                              │                 │
   │      │                              ↓                 │
   │      │                     遇到 await 挂起点            │
   │      │                              │                 │
   │      │              ┌───────────────┴──────────┐      │
   │      │              ↓                          ↓      │
   │      │      注册到 selector          注册定时器（heapq）│
   │      │      (epoll/kqueue/IOCP)                       │
   │      │              │                          │      │
   │      └──── I/O 就绪回调 ←──────── 到期回调 ←─────┘      │
   └──────────────────────────────────────────────────────┘
```

```python
# 关键事实：协程对象和生成器共享底层机制
async def coro(): pass
c = coro()
print(type(c))                               # <class 'coroutine'>
print(c.send, c.throw, c.close)              # 和生成器一样的三个方法！
print(coro.__code__.co_flags & 0x180)        # CO_COROUTINE 标志位

# await 编译成的字节码
import dis
async def f(): await g()
dis.dis(f)      # ... GET_AWAITABLE / SEND / YIELD_VALUE ...
```

| 演进 | 版本 | 写法 |
|------|------|------|
| 生成器协程 | 2.5 | `yield` + `send()` 手工调度 |
| `yield from` | 3.3 | PEP 380，可委托子生成器 |
| `@asyncio.coroutine` | 3.4 | `yield from` + asyncio（已移除） |
| **`async`/`await`** | 3.5 | PEP 492，原生语法 |
| `asyncio.run` | 3.7 | 标准入口，不再手动管 loop |
| `to_thread` | 3.9 | 卸载阻塞调用 |
| **`TaskGroup` / `timeout` / `ExceptionGroup`** | 3.11 | 结构化并发 |
| `eager_task_factory` | 3.12 | Task 首段同步执行，减少调度开销 |

> 🎯 **`await` 只是「把控制权交回事件循环，并登记一个恢复条件」**。理解这一点就明白：await 之间的代码是原子的（单线程、无抢占），所以协程里几乎不需要锁；反过来，一个不带 await 的长循环会完全冻结事件循环。

---

## 8. 无 GIL 的未来

### 8.1 PEP 703：free-threading

```text
   3.13（2024）  实验性构建 python3.13t，--disable-gil
   3.14（2025）  正式支持（officially supported），仍非默认
   3.15+         目标成为默认，GIL 构建逐步退役
```

```bash
python3.14t -c "import sys; print(sys._is_gil_enabled())"   # False
python3.14 -X gil=0 script.py        # 在支持的构建上运行时关闭
```

| 影响 | 说明 |
|------|------|
| ✅ 纯 Python 多线程真并行 | CPU 密集任务终于能用线程扩展 |
| ⚠️ 单线程性能下降 | 3.13t 约 -10%~-20%；3.14 已收窄，仍有开销 |
| ⚠️ C 扩展需适配 | 必须声明 `Py_mod_gil = Py_MOD_GIL_NOT_USED`，否则自动重新启用 GIL |
| ⚠️ **竞态问题暴露** | 以前被 GIL 掩盖的数据竞争会真实出现，必须补锁 |
| 内部改动 | 有偏引用计数、不朽对象、per-object 锁、mimalloc |

> ⚠️ **别指望 free-threading 让老代码自动变快**。GIL 消失后，那些「依赖 GIL 保证 dict/list 操作原子性」的代码会开始出错。迁移时必须审计所有共享可变状态。

### 8.2 PEP 554/734：子解释器

```python
# 3.12 提供 C API，3.13+ 提供 interpreters 模块（3.14 起进入标准库）
from concurrent import interpreters              # 3.14

interp = interpreters.create()
interp.exec("import math; print(math.pi)")
interp.close()

# 每个子解释器有独立的 GIL（3.12+ per-interpreter GIL）
# → 单进程内实现真并行，且比多进程轻量（无需 fork、无需 pickle 大对象）
```

| 方案 | 并行 CPU | 内存开销 | 隔离性 | 成熟度（2026） |
|------|:-------:|---------|-------|--------------|
| 多进程 | ✅ | 高 | 完全 | 成熟 |
| 子解释器 | ✅ | 中 | 较好（对象不共享） | 逐步可用 |
| free-threading | ✅ | 低 | 无（需自己加锁） | 3.14 正式支持 |

### 8.3 绕开 GIL 的传统手段

| 手段 | 做法 |
|------|------|
| 用释放 GIL 的库 | numpy / PyTorch / Pillow-SIMD / lxml —— C 层计算时 GIL 已释放 |
| Cython | `with nogil:` 块内的纯 C 代码可真并行 |
| Numba | `@njit(parallel=True, nogil=True)` |
| Rust 扩展 | PyO3 + `Python::allow_threads` |
| 多进程 | 最通用、最稳的方案 |
| 把计算推到外部 | 数据库、Spark、向量数据库、GPU |

---

## 9. 核心要点回顾

- GIL = 一把保护解释器状态（尤其是引用计数）的全局互斥锁；**只锁字节码执行**
- GIL 在 **I/O 等待**和**显式释放的 C 代码**（numpy/torch）中会放开——所以这两类场景多线程是有效的
- 选型口诀：**CPU 密集 → 多进程；I/O 密集少量 → 线程池；I/O 密集海量 → asyncio**
- `GIL` 不保证复合操作原子性：`count += 1`、`d[k] = d.get(k,0)+1` 都需要锁
- 线程间通信优先 `Queue`，而不是共享变量 + 锁
- 线程里的异常默认静默丢失 → 用 `ThreadPoolExecutor` + `future.result()`
- `multiprocessing` 三个坑：`if __name__ == "__main__"`、参数必须可 pickle、序列化开销
- 传大数组用 `shared_memory` 零拷贝，别让 pickle 拷两遍
- `concurrent.futures` 是首选高层接口，线程池/进程池切换只改类名
- 生产代码用 `submit` + `as_completed`（可逐个容错），而非 `map`
- **协程本质是生成器**，`await` = 让出控制权 + 登记恢复条件；await 之间的代码是原子的
- 绝不在协程里做同步阻塞调用；兜底方案是 `asyncio.to_thread`
- 3.11+ 用 `TaskGroup` + `asyncio.timeout` 写结构化并发
- 调 API 必须配 `Semaphore` 限流，否则第一秒就被限流封掉
- free-threading（PEP 703）在 3.14 正式支持但非默认；它会**暴露**原本被 GIL 掩盖的竞态

---

## 参考资料

| 类型 | 名称 |
|------|------|
| PEP | **703（free-threading）**、492（async/await）、525（异步生成器）、530（异步推导式）、734（子解释器）、3156（asyncio） |
| 文档 | `threading` / `multiprocessing` / `concurrent.futures` / `asyncio` 官方文档 |
| 演讲 | David Beazley《Understanding the Python GIL》（经典）、《Build Your Own Async》 |
| 源码 | `Python/ceval_gil.c`、`Lib/asyncio/base_events.py` |
| 书籍 | 《Using Asyncio in Python》（Caleb Hattingh）、《High Performance Python》第 7-10 章 |

---

**上一模块**：[11 内存管理与垃圾回收](./11-内存管理与垃圾回收.md) ／ **下一模块**：[13 Pythonic 写法与语法糖](./13-Pythonic写法与语法糖.md) ／ **返回总览**：[00 总览](./00-Python语言特性知识体系总览.md)
