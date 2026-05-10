# Java 多线程核心知识点（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | Java 多线程编程  
> **核心包**：`java.lang.Thread`、`java.util.concurrent`  
> **前置基础**：Java 基础语法、面向对象

---

## 一、核心概念

### 1.1 程序、进程与线程

| 概念 | 定义 | 特点 |
|------|------|------|
| **程序** | 为完成特定任务、用某种语言编写的指令集合 | 静态代码 |
| **进程** | 程序的一次执行过程，系统资源分配和调度的独立单位 | 有自己的内存空间和生命周期 |
| **线程** | 进程中的一个执行单元，CPU 调度和分派的基本单位 | 共享进程内存空间，仅有少量私有资源（PC、栈、寄存器） |

### 1.2 并发与并行

| 概念 | 定义 | 硬件要求 |
|------|------|----------|
| **并发** | 多线程在同一时间段交替执行，宏观同时微观切换 | 单核 CPU 即可 |
| **并行** | 多线程在同一时刻同时执行 | 需要多核 CPU |

### 1.3 多线程优势与应用场景

- **优势**：提高 CPU 利用率、提升响应速度、实现异步执行
- **场景**：文件下载、数据批量处理、网络通信、实时监控、并发服务器

---

## 二、底层原理

### 2.1 线程的七种生命周期状态

```
NEW → start() → RUNNABLE ←→ BLOCKED / WAITING / TIMED_WAITING
                   ↓
              TERMINATED（run() 执行完毕或异常终止）
```

| 状态 | 进入方式 | 退出方式 |
|------|----------|----------|
| **NEW** | `new Thread()` | 调用 `start()` |
| **RUNNABLE** | `start()` 被调用，等待 CPU 调度 | 被 CPU 调度执行 |
| **BLOCKED** | 竞争 `synchronized` 锁失败 | 获取锁 |
| **WAITING** | `Object.wait()`、`Thread.join()`、`LockSupport.park()` | `notify()`/`notifyAll()` 显式唤醒 |
| **TIMED_WAITING** | `sleep(long)`、`wait(long)`、`join(long)` | 超时自动唤醒 |
| **TERMINATED** | `run()` 执行完毕或异常终止 | 不可恢复 |

### 2.2 Java 内存模型（JMM）核心

- 每个线程有自己的工作内存（缓存）
- 所有线程共享主内存
- `volatile` 保证可见性 + 禁止指令重排序
- `synchronized` 保证原子性 + 可见性 + 有序性

---

## 三、代码实现

### 3.1 方式 1：继承 Thread 类

```java
/**
 * 继承 Thread 类创建线程。
 * 缺点：Java 单继承，灵活性受限。
 */
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("继承Thread类的线程执行");
    }
}

// 启动
MyThread thread = new MyThread();
thread.start();  // 注意：调用 start() 而非 run()
```

### 3.2 方式 2：实现 Runnable 接口

```java
/**
 * 实现 Runnable 接口创建线程。
 * 优点：避免单继承限制，多个线程可共享同一 Runnable 实例。
 */
class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("实现Runnable接口的线程执行");
    }
}

// 启动
Runnable runnable = new MyRunnable();
Thread thread = new Thread(runnable);
thread.start();
```

### 3.3 方式 3：实现 Callable 接口 + FutureTask

```java
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 实现 Callable 接口 —— 支持返回值和抛出异常。
 */
class MyCallable implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        int sum = 0;
        for (int i = 1; i <= 100; i++) {
            sum += i;
        }
        return sum;
    }
}

// 使用
Callable<Integer> callable = new MyCallable();
FutureTask<Integer> futureTask = new FutureTask<>(callable);
Thread thread = new Thread(futureTask);
thread.start();
Integer result = futureTask.get();  // 阻塞等待结果
System.out.println("计算结果：" + result);
```

### 3.4 方式 4：线程池（实际开发最常用）

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程池方式 —— 实际开发首选，避免频繁创建销毁线程。
 */
public class ThreadPoolDemo {
    public static void main(String[] args) {
        // 创建固定大小线程池（3 个线程）
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 5; i++) {
            int taskId = i;
            executor.submit(() -> {
                System.out.println("任务" + taskId + "执行，线程："
                    + Thread.currentThread().getName());
            });
        }

        executor.shutdown();  // 关闭线程池
    }
}
```

| 线程池类型 | 特点 |
|-----------|------|
| `newFixedThreadPool(n)` | 固定大小，多余任务排队 |
| `newCachedThreadPool()` | 动态调整，空闲 60s 回收 |
| `newSingleThreadExecutor()` | 单线程，任务顺序执行 |
| `newScheduledThreadPool(n)` | 支持定时和周期性任务 |

### 3.5 线程核心方法

| 方法 | 说明 | 注意事项 |
|------|------|----------|
| `start()` | 启动线程，进入就绪状态 | 只能调用一次，重复抛异常 |
| `run()` | 线程执行体 | 直接调用不启动新线程 |
| `sleep(long)` | 当前线程休眠（不释放锁） | 静态方法，超时后自动唤醒 |
| `yield()` | 主动让出 CPU | 不保证其他线程一定能执行 |
| `join()` | 等待该线程执行完毕 | 调用方进入 WAITING 状态 |
| `setPriority(int)` | 设置优先级（1~10，默认 5） | 高优先级概率更高但不保证 |
| `setDaemon(true)` | 设为守护线程 | 用户线程结束，守护线程自动终止 |

```java
// sleep 示例
Thread.sleep(1000);  // 休眠 1 秒，不释放锁

// join 示例
thread.join();  // 主线程等待 thread 执行完毕
thread.join(5000);  // 最多等待 5 秒

// 守护线程示例
Thread daemon = new Thread(() -> { /* 后台任务 */ });
daemon.setDaemon(true);  // 必须在 start() 前设置
daemon.start();
```

---

## 四、实战要点

### 4.1 创建方式选型

| 方式 | 适用场景 | 推荐度 |
|------|----------|--------|
| `Thread` | 简单场景、无需返回值 | ★★ |
| `Runnable` | 多线程共享任务 | ★★★ |
| `Callable` + `FutureTask` | 需要返回值 | ★★★ |
| **线程池** | **生产环境首选** | ★★★★★ |

### 4.2 `ThreadPoolExecutor` 核心参数

```java
new ThreadPoolExecutor(
    5,                    // corePoolSize：核心线程数
    10,                   // maximumPoolSize：最大线程数
    60L, TimeUnit.SECONDS,// 空闲线程存活时间
    new LinkedBlockingQueue<>(100),  // 任务队列
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

### 4.3 `synchronized` vs `ReentrantLock`

| 维度 | `synchronized` | `ReentrantLock` |
|------|---------------|-----------------|
| 实现 | JVM 内置（Monitor） | JDK API（AQS） |
| 锁释放 | 自动 | 必须 `unlock()`（finally） |
| 可中断 | 不支持 | `lockInterruptibly()` |
| 公平锁 | 非公平 | 支持公平/非公平 |
| 条件变量 | `wait/notify` | `Condition` |
| 推荐 | 简单场景 | 需要高级特性时 |

---

## 五、避坑总结

| 坑点 | 问题 | 正确做法 |
|------|------|----------|
| **直接调用 run()** | 不会启动新线程 | 必须调用 `start()` |
| **重复 start()** | 抛 `IllegalThreadStateException` | 一个线程只能 start 一次 |
| **高并发 new Thread** | 线程数暴增导致 OOM | 必须使用线程池 |
| **sleep() 误以为释放锁** | sleep 不释放锁，其他线程仍阻塞 | 用 `wait()` 释放锁 |
| **忘记 shutdown()** | 线程池不关闭，JVM 不退出 | 在 finally 或 shutdown hook 中调用 |
| **守护线程 setDaemon 在 start 后** | 抛 `IllegalThreadStateException` | 必须在 `start()` 前设置 |

---

## 六、企业级最佳实践

### 6.1 线程池使用原则

| 原则 | 说明 |
|------|------|
| **禁止 Executors 直接创建** | `newFixedThreadPool` 队列无限长有 OOM 风险，用 `ThreadPoolExecutor` 构造 |
| **合理配置参数** | CPU 密集型：`coreSize = CPU 核数 + 1`；IO 密集型：`coreSize = CPU 核数 × 2` |
| **自定义线程工厂** | 便于命名线程，排查问题时快速定位 |
| **监控线程池** | 通过 `getQueue().size()` 和 `getActiveCount()` 监控 |

### 6.2 线程安全策略

- **无状态设计**：优先设计不可变对象（`final` 字段 + 无 setter）
- **线程封闭**：使用 `ThreadLocal` 隔离线程数据（用后必须 `remove()`）
- **同步容器 → 并发容器**：`HashMap` → `ConcurrentHashMap`，`ArrayList` → `CopyOnWriteArrayList`
- **原子类**：`AtomicInteger`、`AtomicLong`、`AtomicReference` 代替 `synchronized` 做简单计数

### 6.3 死锁排查

```java
// jstack <pid> 查看线程堆栈，定位死锁
// 日志输出示例：
// Found one Java-level deadlock:
// "Thread-1": waiting to lock monitor ... (held by "Thread-0")
// "Thread-0": waiting to lock monitor ... (held by "Thread-1")
```
