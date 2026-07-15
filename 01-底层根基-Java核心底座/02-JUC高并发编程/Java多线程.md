# Java 多线程

> **定位**：Java 多线程编程是 JUC（Java Util Concurrent）高并发编程的核心基础，覆盖线程创建、同步控制、线程间通信、Lock 锁机制、定时任务等核心模块。

---

## 目录

1. [Java 多线程技能](#1-java-多线程技能)
2. [对象及变量的并发访问](#2-对象及变量的并发访问)
3. [线程间通信](#3-线程间通信)
4. [Lock 的使用](#4-lock-的使用)
5. [定时器 Timer](#5-定时器-timer)
6. [单例模式与多线程](#6-单例模式与多线程)
7. [拾遗增补](#7-拾遗增补)

---

## 1. Java 多线程技能

### 1.1 线程创建方式

| 方式 | 做法 | 特点 |
|------|------|------|
| 继承 `Thread` | `class MyThread extends Thread { run() }` | 单继承限制，不灵活 |
| 实现 `Runnable` | `class MyTask implements Runnable { run() }` | ✅ 推荐，可多实现 |
| 实现 `Callable` | `class MyTask implements Callable<T> { call() }` | ✅ 有返回值 + 可抛异常 |
| 线程池 | `Executors.newFixedThreadPool()` | ✅ 生产环境标准方式 |

### 1.2 线程生命周期

```text
NEW → RUNNABLE → BLOCKED / WAITING / TIMED_WAITING → TERMINATED
```

### 1.3 核心方法

| 方法 | 作用 | 注意 |
|------|------|------|
| `start()` | 启动线程 | 只能调用一次 |
| `run()` | 线程执行体 | 直接调用只是普通方法 |
| `sleep(long)` | 休眠（不释放锁） | `InterruptedException` |
| `join()` | 等待线程结束 | — |
| `yield()` | 让出 CPU | 不保证生效 |
| `interrupt()` | 中断线程 | 配合 `isInterrupted()` 检查 |

---

## 2. 对象及变量的并发访问

### 2.1 线程安全问题

| 问题 | 原因 | 示例 |
|------|------|------|
| **原子性** | 多步操作被中断 | `count++`（读-改-写） |
| **可见性** | 线程缓存导致读不到最新值 | 一个线程修改变量，另一个看不到 |
| **有序性** | 指令重排 | 单例 DCL 的 `volatile` |

### 2.2 synchronized 关键字

| 用法 | 锁对象 | 示例 |
|------|--------|------|
| 同步方法 | `this`（实例方法）/ `Class`（静态方法） | `public synchronized void m() {}` |
| 同步代码块 | 指定对象 | `synchronized(lock) { ... }` |
| 静态同步方法 | `类名.class` | `public static synchronized void m() {}` |

### 2.3 volatile 关键字

| 特性 | 说明 |
|------|------|
| **保证可见性** | 修改后立即刷新到主内存 |
| **禁止指令重排** | 内存屏障 |
| **不保证原子性** | `count++` 仍不安全 |

---

## 3. 线程间通信

### 3.1 wait / notify 机制

| 方法 | 作用 | 归属 |
|------|------|------|
| `wait()` | 释放锁 + 进入等待 | `Object` |
| `notify()` | 唤醒一个等待线程 | `Object` |
| `notifyAll()` | 唤醒所有等待线程 | `Object` |

> ⚠️ 必须在 `synchronized` 块内调用，否则抛 `IllegalMonitorStateException`

### 3.2 生产者-消费者模式

```java
class Buffer {
    private int data;
    private boolean empty = true;

    public synchronized void produce(int value) throws InterruptedException {
        while (!empty) wait();
        data = value;
        empty = false;
        notifyAll();
    }

    public synchronized int consume() throws InterruptedException {
        while (empty) wait();
        empty = true;
        notifyAll();
        return data;
    }
}
```

### 3.3 通信方式对比

| 方式 | 锁释放 | 适用场景 |
|------|:------:|----------|
| `wait / notify` | ✅ 释放 | `synchronized` 内线程协调 |
| `await / signal` | ✅ 释放 | `Lock` 条件变量 |
| `sleep` | ❌ 不释放 | 定时等待 |
| `join` | — | 等待线程结束 |

---

## 4. Lock 的使用

### 4.1 Lock 体系

| 类/接口 | 特点 |
|----------|------|
| `ReentrantLock` | 可重入互斥锁，支持公平/非公平 |
| `ReentrantReadWriteLock` | 读写锁，读共享、写互斥 |
| `Condition` | Lock 条件变量（替代 `wait/notify`） |
| `StampedLock` | JDK 8+ 乐观读锁 |

### 4.2 Lock vs synchronized

| 维度 | `synchronized` | `Lock` |
|------|---------------|--------|
| 锁释放 | 自动（代码块结束） | 手动（`unlock()` 必须在 `finally`） |
| 可中断 | ❌ | ✅ `lockInterruptibly()` |
| 超时获取 | ❌ | ✅ `tryLock(time, unit)` |
| 公平锁 | ❌ | ✅ 可配公平/非公平 |
| 条件变量 | 单一隐式 | 多个 `Condition` |
| 性能 | JDK 6+ 优化后接近 | 略高开销 |

```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    // 临界区
} finally {
    lock.unlock();  // ⚠️ 必须在 finally 中释放
}
```

---

## 5. 定时器 Timer

### 5.1 Timer 核心方法

| 方法 | 说明 |
|------|------|
| `schedule(task, delay)` | 延迟执行一次 |
| `schedule(task, delay, period)` | 固定延迟周期执行 |
| `scheduleAtFixedRate(task, delay, period)` | 固定频率执行 |

### 5.2 Timer vs ScheduledThreadPoolExecutor

| 维度 | `Timer` | `ScheduledThreadPoolExecutor` |
|------|---------|-------------------------------|
| 线程数 | 单线程 | 线程池 |
| 异常处理 | 任务抛异常后 Timer 终止 | ✅ 不影响其他任务 |
| 推荐 | ❌ 不推荐 | ✅ 推荐 |

```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
executor.scheduleAtFixedRate(() -> {
    System.out.println("定时任务执行");
}, 0, 5, TimeUnit.SECONDS);
```

---

## 6. 单例模式与多线程

### 6.1 多线程下单例实现对比

| 方式 | 线程安全 | 延迟加载 | 性能 |
|------|:--------:|:--------:|:----:|
| 饿汉式 | ✅ | ❌ | 高 |
| 懒汉式（无同步） | ❌ | ✅ | 高 |
| **DCL**（双重检查锁） | ✅ | ✅ | 高 |
| 静态内部类 | ✅ | ✅ | 高 |
| 枚举 | ✅ | ❌ | 高 |

### 6.2 DCL 核心代码

```java
public class Singleton {
    // volatile 禁止指令重排（关键！）
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {                    // 第一次检查
            synchronized (Singleton.class) {
                if (instance == null) {            // 第二次检查
                    instance = new Singleton();    // 非原子操作，volatile 防重排
                }
            }
        }
        return instance;
    }
}
```

---

## 7. 拾遗增补

### 7.1 线程组

> `ThreadGroup` 统一管理一组线程，不推荐新项目使用，线程池更优。

### 7.2 线程异常处理

| 方式 | 说明 |
|------|------|
| `try-catch` 在 `run()` 内 | 捕获当前线程异常 |
| `UncaughtExceptionHandler` | 未捕获异常全局处理 |
| `Future.get()` | `Callable` 异常通过 `ExecutionException` 抛出 |

### 7.3 ThreadLocal

```java
private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

// 使用
String date = DATE_FORMAT.get().format(new Date());

// ⚠️ 用完记得 remove()，避免内存泄漏（尤其线程池场景）
DATE_FORMAT.remove();
```

### 7.4 线程工具类速查

| 工具 | 用途 |
|------|------|
| `CountDownLatch` | 等待 N 个线程完成 |
| `CyclicBarrier` | 线程互相等待到齐 |
| `Semaphore` | 限流控制并发数 |
| `Exchanger` | 线程间交换数据 |
| `Phaser` | 多阶段同步 |

---

> 🎯 **核心总结**：多线程编程重点掌握线程创建方式（线程池优先）、`synchronized` vs `Lock` 选型、`wait/notify` 与 `Condition` 通信机制、单例的 DCL 写法（`volatile` 防重排）、以及 JUC 工具类的场景化使用。
