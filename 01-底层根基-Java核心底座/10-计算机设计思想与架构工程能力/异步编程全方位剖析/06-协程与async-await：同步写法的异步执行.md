# 协程与 async/await：同步写法的异步执行

> 第三代异步模型的目标：**用同步的写法写异步的代码**。实现机制是编译器魔法——async 函数被 CPS 变换成状态机，await 是挂起点。本章拆开魔法看机制，再对照 Python/JS/Kotlin/Go/C++ 五语言的协程形态，最后回答"虚拟线程是不是协程"

---

## 📚 目录

1. [魔法拆解：CPS 变换与状态机](#1-魔法拆解cps-变换与状态机)
2. [await 的本质：挂起与恢复](#2-await-的本质挂起与恢复)
3. [Python asyncio 协程](#3-python-asyncio-协程)
4. [JS async/await：Promise 的语法糖](#4-js-asyncawaitpromise-的语法糖)
5. [Kotlin coroutine：结构化并发的语言级答案](#5-kotlin-coroutine结构化并发的语言级答案)
6. [Go goroutine 与 C++20 coroutine：两极形态](#6-go-goroutine-与-c20-coroutine两极形态)
7. [虚拟线程是"隐形协程"吗](#7-虚拟线程是隐形协程吗)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. 魔法拆解：CPS 变换与状态机

`async/await` 的魔法是编译器的：**CPS（Continuation-Passing Style）变换**——把"顺序代码"改写成"每次 await 都是一个状态，恢复时从下一个状态继续"：

```python
# 源码：看起来是顺序代码
async def fetch_and_save(url):
    data = await fetch(url)       # 挂起点 1
    save(data)                    # 恢复后执行
    result = await process(data)  # 挂起点 2
    return result

# 编译后（概念形态）：状态机
# state 0 → 执行 fetch → 挂起（保存 state=1）
# state 1 → 恢复 → save → 发起 process → 挂起（state=2）
# state 2 → 恢复 → 返回 result
```

关键认知：

- **协程没有"线程栈"**——挂起时保存的是状态机的局部变量（闭包对象），恢复时从"下一个状态"继续
- **内存极省**：一个挂起的协程约 256B-几 KB（03 章对照），万级协程轻松
- **代价是显式**：挂起点必须写 `await`——编译器无法自动识别"哪里可以挂起"，这是协程与虚拟线程的本质分界（第 7 节）

```python
# 反例：忘了 await——协程不会执行，只是创建了一个对象
async def fetch(url): ...
data = fetch(url)          # 没有 await！data 是协程对象而非结果（Python 会有警告）
data = await fetch(url)    # 正确：挂起直到完成
```

## 2. await 的本质：挂起与恢复

`await` 的完整语义 = 三个动作：

1. **挂起**：把当前状态机的"下一个状态 + 局部变量"保存到堆上
2. **让出**：把控制权还给事件循环/调度器——**线程没闲着，去跑别的协程**
3. **恢复**：目标操作完成（IO 就绪/定时器到期），调度器把结果送回来，状态机从保存点继续

```python
# await 的等价心智模型
def await_(task):
    event_loop.register(task, callback=resume_me)  # 注册回调
    yield_to_event_loop()                            # 让出控制权
    return task.result()                             # 恢复后拿到结果
```

两个工程推论：**await 挂起的是"当前协程"，不是"当前线程"**——同一个线程可以依次"跑"上万个协程（事件循环的串行性，04 章）；**await 的调用方必须也是协程**——着色问题在此显现（05 章），"同步函数里不能 await"是协程世界的铁律。

## 3. Python asyncio 协程

Python 的协程是"**事件循环 + 协程**"双件套：`async def` 定义协程，asyncio 提供事件循环与调度：

```python
import asyncio

async def fetch_order(order_id: int) -> dict:
    async with httpx.AsyncClient() as client:      # 异步客户端（着色生态）
        r = await client.get(f"/orders/{order_id}")  # 挂起：IO 等待让出
        return r.json()

async def main():
    results = await asyncio.gather(                 # 并行扇出
        fetch_order(1), fetch_order(2), fetch_order(3))
    return results

asyncio.run(main())                                # 入口：创建事件循环
```

2026 的 Python 异步格局（细节见「Python 异步 + FastAPI」体系）：

- **uvloop**（C 实现事件循环，2024 起可选的 io_uring 后端）让 asyncio 的吞吐接近 Node 水平
- **FastAPI** 让协程成为 AI 模型服务的事实标准（流式/SSE 的天然载体）
- **边界清晰**：协程管 IO 并发，free-threading（PEP 703/779）管 CPU 并行，`asyncio.to_thread` 桥接阻塞任务

Python 协程的**排障补充**：`await` 忘写、阻塞调用混入、`asyncio.run` 重复创建循环是三大高频事故——"协程对象没有 await"（报 `RuntimeWarning: coroutine was never awaited`）、"事件循环已在运行"（嵌套 `asyncio.run`）都有明确报错特征，先认识症状再谈原理，排障效率翻倍。

## 4. JS async/await：Promise 的语法糖

JS 的协程是"**Promise 的语法糖**"——`async` 函数返回 Promise，`await` 等价于 `.then`：

```javascript
// 链式写法 → 语法糖写法（两者等价）
fetch(url).then(r => r.json()).then(render);           // Promise 链
async function load() {                                 // async/await
    const r = await fetch(url);
    render(await r.json());
}
```

JS 协程的特点：**没有自己的调度器**——"调度"就是微任务队列（04 章），`await` 展开后仍是 Promise 链；浏览器环境没有"线程池"概念，事件循环是唯一模型。**JS 是"协程但非轻量线程"的典型样本**：着色问题被语言层面"全员异步"化解（浏览器 API 全是 Promise），代价是同步代码在 JS 世界被边缘化（Node 的同步 fs API 是特例而非主流）。

## 5. Kotlin coroutine：结构化并发的语言级答案

Kotlin 协程是目前"语言级协程"的标杆，两个独有设计：

- **Dispatchers（调度器显式化）**：协程自己声明跑在哪个线程——`Dispatchers.IO`（IO 线程池）、`Dispatchers.Default`（CPU 池）、`Dispatchers.Main`（UI 线程）；**"协程与线程的绑定"由代码决定，而非运行时猜测**
- **结构化并发（coroutineScope，稳定）**：父协程取消 → 子协程自动取消；父失败 → 子立即终止——**"并发任务的生命周期"第一次成为语言级语义**（Java 的 StructuredTaskScope 还在预览，JEP 525）

```kotlin
suspend fun loadDashboard(): Dashboard = coroutineScope {
    val orders = async(Dispatchers.IO) { orderRepo.findAll() }   // 并行
    val users  = async(Dispatchers.IO) { userRepo.findAll() }
    Dashboard(orders.await(), users.await())                    // 结构化：异常自动取消全部
}
```

**2026 的启示**：Kotlin 证明"协程 + 结构化并发"可以解决回调地狱的剩余问题（取消传播、生命周期）；Java 虚拟线程 + JEP 525 正在追赶同一目标——**两条路线（显式协程 vs 透明线程）在"结构化并发"上交汇**，这是 2026 年异步编程最重要的合流信号。

## 6. Go goroutine 与 C++20 coroutine：两极形态

- **Go goroutine**：不是协程——它是"**语言内置的轻量线程**"：无 await、无状态机、阻塞即让出；goroutine 与线程的映射由运行时 M:N 调度（03 章）。**Go 选择"隐形并发"路线**，代价是控制力（没有显式挂起点，协程语义的精细操作做不了）
- **C++20 coroutine**：标准库只有机制（co_await/co_yield）没有调度器——协程"挂哪、恢复谁"全部由实现者自建（cppcoro/Asio 等库补全）；C++26 的 Sender 模型（std::execution）试图用"可组合的异步工作描述"统一碎片生态。**C++ 是"机制先行、生态后补"的极端样本**——2026 年 stdexec 参考实现落地，但生产采用仍需时间

两极启示：**语言设计的取舍决定协程形态**——Go 押"透明"（生态零分裂），Kotlin 押"显式 + 结构化"（控制力），C++ 押"机制"（性能极致、自行组装），Python/JS 押"事件循环 + 协程"（生态单一）。**没有免费的形态，只有适合生态的形态**。

## 7. 虚拟线程是"隐形协程"吗

2026 年讨论的高频问题，答案：**机制上是，形态上不是**：

- **机制等价**：虚拟线程的挂起/恢复（continuation 保存栈帧到堆）与协程的状态机保存本质同类——都是"用户态挂起，不让 OS 线程闲着"
- **形态差异**：协程要求**显式标注挂起点**（await/suspend，编译期确定状态机）；虚拟线程**自动挂起**（任意阻塞点即挂起点，运行时识别）——这就是"隐形协程"的含义：**协程的能力 + 同步代码的外观，无着色问题**

```text
协程：    挂起 = await 显式标注 → 状态机编译期生成 → 无栈、极省内存
虚拟线程：挂起 = 阻塞自动识别 → continuation 运行时保存 → 有栈、可调试
goroutine：挂起 = 阻塞自动识别 → 运行时调度 → 有栈、语言内置
```

**选型含义**：要"零学习成本 + 生态不分裂"选虚拟线程/goroutine；要"内存极致 + 显式控制"选协程；2026 的折中答案——**虚拟线程做 IO 基座，协程做上层并发编排**（Kotlin 社区已验证组合可行，03 章第 6 节）。

> 🎯 **核心要点**：协程 = 编译器 CPS 变换的状态机（无栈、256B 级、显式 await）；JS 的 await 是 Promise 语法糖，Python 是"事件循环 + 协程"双件套，Kotlin 是"调度器 + 结构化并发"标杆，Go/C++ 走两极；虚拟线程是"隐形协程"——同机制、无着色，与显式协程在"结构化并发"上交汇。

## 8. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. async/await 的魔法 = CPS 变换：await 即"保存状态机 + 让出控制权 + 恢复继续"；
> 2. 五语言形态：Python（事件循环+协程）、JS（Promise 语法糖）、Kotlin（Dispatchers+结构化并发）、Go（隐形线程）、C++（机制先行）；
> 3. 虚拟线程 = 隐形协程：同挂起机制、无着色、可调试——2026 年与显式协程在结构化并发上合流。

**思考题**：

1. 协程挂起时保存的是什么？不保存什么？（→ 1 节）
2. 为什么"同步函数里不能 await"？（→ 2 节）
3. Kotlin 的结构化并发解决了回调模型的哪个遗留问题？（→ 5 节）
4. 虚拟线程与协程的"机制等价、形态差异"具体指什么？（→ 7 节）

---

**下一模块**：[07-事件循环深潜：Python 与 JS 为样本](07-事件循环深潜：Python 与 JS 为样本.md)｜**返回总览**：[00-异步编程全方位剖析知识体系总览](00-异步编程全方位剖析知识体系总览.md)

---

## 参考来源

- [异步非阻塞的三种实现（阿里云开发者）](https://developer.aliyun.com/article/1731890)——协程机制综述
- [Coroutines vs Virtual Threads（Deep Engineering）](https://deepengineering.net/p/coroutines-vs-virtual-threads-and)——协程 vs 虚拟线程
- [Virtual Threads vs. Coroutines in 2026（CodeMotion）](https://www.codemotion.com/magazine/languages/virtual-threads-vs-coroutines-in-2026-codemotion-madrid-2026/)——2026 格局
- [Java 原生协程与 Kotlin 协程深度比较（OSCHINA）](https://my.oschina.net/emacs_9735824/blog/19182194)——机制对照
