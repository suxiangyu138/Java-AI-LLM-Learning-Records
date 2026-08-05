# 07 - 协程与 asyncio 深度

> 🎯 协程是 Python 异步编程的核心——比线程轻量 1000 倍，AI API 并发调用、流式数据处理、WebSocket 实时通信的基石。掌握 `async/await` + 事件循环，就掌握了 Python 高性能 I/O 的秘密

---

## 目录

1. [协程 vs 线程 vs 进程](#1-协程-vs-线程-vs-进程)
2. [async/await 核心](#2-asyncawait-核心)
3. [Task 与并发模式](#3-task-与并发模式)
4. [AI 开发实战](#4-ai-开发实战)

---

## 1. 协程 vs 线程 vs 进程

| 维度 | 协程 (asyncio) | 线程 (threading) | 进程 (multiprocessing) |
|------|:---:|:---:|:---:|
| 切换者 | 程序自己 (await) | 操作系统 | 操作系统 |
| 切换成本 | ~100 ns | ~1-10 μs | ~100 μs |
| 内存 | KB 级 | MB 级 | GB 级 |
| GIL 影响 | 无（单线程） | 受限 | 无 |
| 适合 | I/O 密集 | I/O 密集 | CPU 密集 |
| 并发数 | 10,000+ | 100-1000 | 10-100 |

## 2. async/await 核心

```python
import asyncio

# async def = 协程函数（返回协程对象）
# await = 暂停当前协程，等待另一个协程完成

async def fetch(url):
    print(f"开始: {url}")
    await asyncio.sleep(1)       # 模拟 I/O（释放控制权）
    print(f"完成: {url}")
    return f"data from {url}"

async def main():
    # 串行: 1+1=2s
    # r1 = await fetch("url1")
    # r2 = await fetch("url2")

    # 并发: max(1,1)=1s
    results = await asyncio.gather(
        fetch("url1"),
        fetch("url2"),
    )
    print(results)

asyncio.run(main())
```

### 事件循环

```python
# 获取/创建事件循环
loop = asyncio.get_event_loop()             # 已存在的
loop = asyncio.new_event_loop()             # 新创建
asyncio.set_event_loop(loop)                # 设为当前线程的事件循环

# 事件循环的执行流程：
# 1. 从队列取出一个就绪的协程
# 2. 执行直到遇到 await → 挂起
# 3. 检查 I/O 完成 → 标记协程就绪
# 4. 重复 1-3 直到队列为空
```

## 3. Task 与并发模式

```python
# Task: 并发执行的协程单元
async def main():
    task1 = asyncio.create_task(fetch("url1"))  # 立即开始（不等 await！）
    task2 = asyncio.create_task(fetch("url2"))

    # task1 和 task2 已经并发运行了！
    result1 = await task1
    result2 = await task2

# 超时控制
try:
    result = await asyncio.wait_for(slow_task(), timeout=5.0)
except asyncio.TimeoutError:
    result = "fallback"

# 竞速：取第一个完成的结果
done, pending = await asyncio.wait(
    [fetch("url1"), fetch("url2")],
    return_when=asyncio.FIRST_COMPLETED
)
```

## 4. AI 开发实战

```python
# 1. 并发调用多个 AI API
async def call_multiple_llms(prompt):
    async def call_llm(provider, model, prompt):
        try:
            return await provider.generate(model, prompt)
        except Exception as e:
            return f"{provider.name}: Error - {e}"

    tasks = [
        call_llm(openai_client, "gpt-4o", prompt),
        call_llm(anthropic_client, "claude-sonnet", prompt),
        call_llm(deepseek_client, "deepseek-chat", prompt),
    ]
    return await asyncio.gather(*tasks, return_exceptions=True)

# 2. 流式 WebSocket（FastAPI）
@app.websocket("/chat/stream")
async def chat_stream(websocket: WebSocket):
    await websocket.accept()
    async for chunk in llm.stream(await websocket.receive_text()):
        await websocket.send_text(chunk)

# 3. 信号量限流
sem = asyncio.Semaphore(10)  # 最多 10 个并发
async def rate_limited_call(prompt):
    async with sem:
        return await llm.generate(prompt)
```

## 核心要点回顾

- 协程 = 用户态切换、KB 级内存、10K+ 并发
- `async def` 定义协程，`await` 释放控制权
- `asyncio.gather` = 并发执行多个协程（最常用）
- `asyncio.create_task` = 后台启动协程
- `Semaphore` = 控制并发上限
- CPU 密集任务要用 `loop.run_in_executor` 在线程池跑

## 参考资料

1. asyncio 官方文档 — docs.python.org/library/asyncio
2. Real Python — Async IO in Python
