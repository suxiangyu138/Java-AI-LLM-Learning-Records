# 02 线程创建与 Thread API 精讲

> 创建线程只是开始——interrupt 的语义、join/yield/daemon 的细节、生命周期状态机，才是 Thread API 的真正考点

---

## 📚 目录

1. [四种创建方式与本质区别](#1-四种创建方式与本质区别)
2. [线程生命周期状态机](#2-线程生命周期状态机)
3. [interrupt 三件套：协作式中断](#3-interrupt-三件套协作式中断)
4. [join / yield / daemon 语义细节](#4-join--yield--daemon-语义细节)
5. [线程命名、优先级与异常处理](#5-线程命名优先级与异常处理)
6. [JDK 26：Thread.stop() 移除与遗留 API](#6-jdk-26threadstop-移除与遗留-api)
7. [创建线程 vs 线程池](#7-创建线程-vs-线程池)

---

## 1. 四种创建方式与本质区别

| 方式 | 写法 | 本质 | 适用 |
|------|------|------|------|
| 继承 Thread | `class My extends Thread { run() }` | 类继承，run 是重写 | 极少（占用继承位） |
| **实现 Runnable** | `new Thread(runnable)` | 组合任务对象 | 通用（推荐） |
| **实现 Callable** | `submit(callable)` | 有返回值 + 可抛异常 | 需要结果 |
| 线程池 | `executor.submit(...)` | 复用线程 | **生产唯一推荐** |

```java
// 方式 2：Runnable（推荐 —— 任务与线程分离）
Runnable task = () -> System.out.println("执行中");
new Thread(task, "worker-1").start();

// 方式 3：Callable —— 返回值与异常
ExecutorService pool = Executors.newFixedThreadPool(4);
Future<Integer> f = pool.submit(() -> compute());   // Callable 可以返回 + 抛异常

// 方式 4：线程池（生产）
try (var pool = Executors.newFixedThreadPool(4)) {
    pool.execute(task);
}   // try-with-resources：池自动关闭（JDK 19+）
```

**Runnable vs Callable vs Thread 对比**：

| 维度 | Thread | Runnable | Callable |
|------|:------:|:--------:|:--------:|
| 返回值 | 无 | 无 | **有（Future）** |
| 受检异常 | 不能抛 | 不能抛 | **可以抛** |
| 任务复用 | ❌（继承即任务） | ✅ | ✅ |
| 语义 | 线程 = 任务（耦合） | 任务独立（解耦） | 任务 + 结果 |

> 🎯 **核心要点**：**生产代码永远走线程池**——`new Thread()` 只出现在教学与一次性实验。Runnable 表达"做什么"，线程池决定"怎么跑"，职责分离。

---

## 2. 线程生命周期状态机

```text
                    start()
        ┌──────────────────────────────┐
        ▼                              │
NEW ──▶ RUNNABLE ──▶ TERMINATED ◀─────┘
          │  ▲
  获得锁   │  │ 释放锁/竞争失败
          ▼  │
        BLOCKED（等锁）
          │
   wait()/join()/park()
          ▼
        WAITING / TIMED_WAITING（sleep/wait(timeout)）
```

| 状态 | 进入 | 退出 | 说明 |
|------|------|------|------|
| NEW | new Thread() | start() | 未启动 |
| RUNNABLE | start()/唤醒 | — | **含运行与就绪**（JVM 视角不区分） |
| BLOCKED | synchronized 竞争失败 | 获得锁 | **只在等"监视器锁"** |
| WAITING | wait()/join()/park() | notify/join 结束/unpark | 无限等待 |
| TIMED_WAITING | sleep()/wait(ms) | 超时/唤醒 | 限时等待 |
| TERMINATED | run() 返回/异常 | — | 结束，**不可重启** |

```java
// 验证状态机：用 jstack 或 Thread.getState() 观察
Thread t = new Thread(() -> {
    synchronized (lock) { sleep(1000); }
});
t.getState();   // NEW → start() 后 RUNNABLE → ...
```

> ⚠️ **两个高频陷阱**：① RUNNABLE 包含"正在执行"和"排队等 CPU"——**它不表示线程在干活**；② BLOCKED 只发生在"等 synchronized 锁"——等 ReentrantLock 是 WAITING（LockSupport.park），这是"synchronized 与 Lock 状态可观测性差异"的底层原因。

---

## 3. interrupt 三件套：协作式中断

**Java 的中断是"协作式"**——`interrupt()` 只是**设置中断标志**，被中断线程**不一定停**，停不停由它自己决定：

```java
// 三件套
thread.interrupt();              // ① 设置中断标志（请求中断）
thread.isInterrupted();          // ② 查询标志（不清除）
Thread.interrupted();            // ③ 静态方法：查询并清除标志（仅当前线程）

// 标志与阻塞的交互（关键！）：
// 线程处于 sleep/wait/join 时收到 interrupt → 立即抛 InterruptedException 并【清除标志】
// 线程处于运行中收到 interrupt → 标志置位，但【继续执行】—— 需代码自行检查
```

```java
// 正确的中断响应模式
Thread worker = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {   // ① 循环条件检查标志
        try {
            Thread.sleep(100);                          // ② 阻塞等待（会被中断唤醒）
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();         // ③ 恢复标志！让循环退出
            break;
        }
    }
    System.out.println("线程被请求停止，干净退出");
});
worker.start();
Thread.sleep(500);
worker.interrupt();      // 请求中断 → sleep 抛异常 → 恢复标志 → 循环退出
```

**中断处理三原则**（见 `Java异常体系/07`）：

1. **能抛就抛**：方法签名加 `throws InterruptedException`；
2. **不能抛就恢复标志**：`Thread.currentThread().interrupt()`——吞掉中断 = 拒绝协作停机；
3. **别用 Thread.stop()**：强制终止会让共享数据处于不一致状态（JDK 26 已移除，见第 6 节）。

> 🎯 **核心要点**：interrupt 是"**礼貌的请求**"而非"强制的命令"——被中断线程用 `isInterrupted()` 检查 + `InterruptedException` 响应，自己决定何时安全退出。这也是优雅停机（shutdownNow）能工作的机制。

---

## 4. join / yield / daemon 语义细节

### 4.1 join：等待线程结束

```java
Thread t = new Thread(task);
t.start();
t.join();                // 当前线程阻塞，直到 t 终止（底层：wait 循环）
t.join(1000);            // 限时等待：1 秒后不管 t 死活都返回
// 注意：join 会抛 InterruptedException（被中断时）
```

**join 的实现本质**：`join()` 内部是 `wait(0)`——**底层就是 wait/notify 机制**（isAlive 循环 + notifyAll 在线程结束时被 JVM 调用），这也解释了为什么 join 需要处理 InterruptedException。

### 4.2 yield：让出 CPU（几乎不用）

```java
Thread.yield();          // 提示调度器"我愿意让出 CPU"——纯提示，不保证
// 现代 JVM 中 yield 效果微弱（调度器基本忽略），生产代码几乎不用
// 面试点：yield 不释放锁、不进入等待状态、只是"就绪队尾排个队"（语义上）
```

### 4.3 daemon：守护线程

| 维度 | 用户线程 | 守护线程（daemon） |
|------|:--------:|:------------------:|
| JVM 退出 | **所有用户线程结束才退出** | 不阻止 JVM 退出（随进程消失） |
| 典型 | main、业务线程 | GC、编译器、监控线程 |
| 设置 | — | `setDaemon(true)` **必须在 start() 前** |

```java
Thread monitor = new Thread(() -> { while (true) { check(); } });
monitor.setDaemon(true);     // 必须 start 前设置，否则 IllegalThreadStateException
monitor.start();
// 所有用户线程退出 → JVM 退出 → 守护线程被强制终止（没有清理机会！）
```

> ⚠️ **守护线程陷阱**：守护线程随 JVM 退出而"猝死"——**不能在守护线程里做"必须完成"的清理工作**（写日志落盘、状态持久化）；它适合"可有可无"的辅助任务（监控、心跳）。

---

## 5. 线程命名、优先级与异常处理

**生产规范三件套**（可观测性的基础）：

```java
// ① 命名：线程名必须能定位用途
Thread t = new Thread(task, "order-sync-worker-1");
// 线程池命名：ThreadFactory
ThreadFactory factory = new ThreadFactoryBuilder()
        .setNameFormat("pay-thread-%d").build();    // Guava 或自定义

// ② 优先级（提示性，几乎不保证）：1-10，默认 5
t.setPriority(Thread.MAX_PRIORITY);   // 依赖 OS 调度，生产不依赖它

// ③ 未捕获异常处理：默认打印 stderr（线上易丢）→ 自定义收集
t.setUncaughtExceptionHandler((th, e) -> log.error("线程 {} 异常终止", th.getName(), e));
// 全局兜底（应用启动设置一次）
Thread.setDefaultUncaughtExceptionHandler((th, e) -> log.error("未捕获异常", e));
```

**为什么要命名**：线上 jstack 输出时，`"order-sync-worker-3"` 一眼定位业务线程，`"Thread-17"` 则无法判断——**线程命名是免费的可观测性**。

> 🎯 **核心要点**：线程三件套规范 = **命名（定位）+ handler（不丢异常）+ 生命周期管理（池化）**——缺一个，线上排障就多一分困难（详见 `Java异常体系/07-异常与并发异步.md`）。

---

## 6. JDK 26：Thread.stop() 移除与遗留 API

**Thread.stop() 的历史与结局**：

```text
Thread.stop()（JDK 1.0）：
  强制终止线程 → 线程在任意位置被杀死 → 持有的锁不释放、共享数据处于半更新状态
  → 数据损坏（更糟的是静默损坏，不报错）
  → JDK 1.2 标记废弃（@Deprecated），官方一直警告不要使用
  → 实现保留多年（兼容），但文档标注"unsafe"
  → JDK 26（2026-03）终于移除（JDK-8368226）——调用将抛 NoSuchMethodError
```

**其他遗留 API 现状**：

| API | 状态 | 替代 |
|-----|------|------|
| `Thread.stop()` | **JDK 26 移除** | interrupt 协作式中断 |
| `Thread.suspend()/resume()` | 长期废弃 | Lock 的 park/unpark、ReentrantLock |
| `Thread.destroy()` | 从未实现 | — |
| `Thread.countStackFrames()` | 废弃 | StackWalker（JDK 9+） |
| `Thread.stop(Throwable)` | 废弃 | 异常通过 Future/Handler 传播 |

```java
// 替代 Thread.stop 的正确姿势：interrupt 协作式 + 状态检查
thread.interrupt();          // 请求中断（线程内部检查标志退出）
// 或线程池场景：executor.shutdownNow()（内部就是 interrupt 所有工作线程）
```

> 🎯 **核心要点**：**"强制终止线程"在 Java 里从来就不是合法操作**——JDK 26 移除 Thread.stop() 是对这一原则的最终确认。协作式中断（interrupt + 标志检查）是唯一正确姿势，面试答"怎么停止线程"时先否 stop 再给 interrupt。

---

## 7. 创建线程 vs 线程池

| 维度 | new Thread() | 线程池 |
|------|:------------:|:------:|
| 创建成本 | 每次创建/销毁（微秒级×2） | 复用（只付一次） |
| 数量控制 | ❌ 无上限，可 OOM | ✅ 核心/最大/队列 |
| 任务排队 | ❌ 并发数失控 | ✅ 队列缓冲 |
| 生命周期 | 自生自灭 | 统一管理（shutdown） |
| 异常处理 | 默认丢到 stderr | 任务包装 + Future/Handler |
| 可观测性 | 弱 | 线程名/指标统一 |

```java
// 为什么"裸线程"危险：1 万请求 1 万线程 = 10GB+ 虚拟内存 + 上下文切换风暴
for (int i = 0; i < 100_000; i++) {
    new Thread(task).start();      // ❌ 资源耗尽（栈内存 + 切换）
}

// 线程池：有界并发 + 队列缓冲
ExecutorService pool = new ThreadPoolExecutor(
        4, 16, 60, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1000),        // 有界队列！
        new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

> 🎯 **核心要点**：**线程池 = 线程的"资源管理"**——复用、限流、排队、统一生命周期。生产环境创建线程的唯一入口是线程池（虚拟线程场景除外——每任务一个虚拟线程，见 01 模块第 5 节；线程池细节见 `02-JUC高并发编程/06-线程池与Executor框架.md`）。

---

**下一模块**：[03-线程安全三要素与设计思维](./03-线程安全三要素与设计思维.md) / **返回总览**：[00-Java多线程知识体系总览](./00-Java多线程知识体系总览.md)
