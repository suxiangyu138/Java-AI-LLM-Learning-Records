# Java 高并发期末复习项目（完整版）

> 整合高并发核心知识点，"理论 + 实战"全覆盖：并发基础、线程池、锁机制、原子操作、并发工具类、分布式并发。可直接运行、可调试、可扩展。

---

## 目录

- [一、项目概述](#一项目概述)
- [二、项目依赖配置（Maven）](#二项目依赖配置maven)
- [三、各模块详细实现](#三各模块详细实现)
- [模块 1：并发基础](#模块-1并发基础-base-包)
- [模块 2：原子操作](#模块-2原子操作-atomic-包)
- [模块 3：锁机制](#模块-3锁机制-lock-包)
- [模块 4：线程池](#模块-4线程池-threadpool-包)
- [模块 5：并发工具类](#模块-5并发工具类-tool-包)

---

## 一、项目概述

### 1.1 核心复习考点

| 分类 | 考点 |
|------|------|
| 线程基础 | 线程创建与生命周期 |
| 线程同步 | synchronized、Lock |
| 原子操作 | Atomic 系列 |
| 线程池 | ThreadPoolExecutor |
| 并发工具 | CountDownLatch、CyclicBarrier、Semaphore |
| 线程通信 | wait/notify、Condition |
| 关键字 | volatile、CAS |
| 分布式锁 | Redis 实现（可选） |

### 1.2 项目环境

| 项 | 说明 |
|----|------|
| JDK | 1.8 |
| IDE | IntelliJ IDEA |
| 构建 | Maven |
| 测试 | JUnit 4/5、JMeter（可选） |

### 1.3 项目结构

```
com.concurrent.review
├── base          // 线程创建、生命周期、volatile、CAS
├── lock          // synchronized、Lock、读写锁、分布式锁
├── atomic        // Atomic 系列、Unsafe 类
├── threadpool    // ThreadPoolExecutor、Executors
├── tool          // CountDownLatch、CyclicBarrier、Semaphore
├── communication // wait/notify、Condition
├── test          // 测试用例、高并发模拟
└── util          // 日志、常量、通用方法
```

---

## 二、项目依赖配置（Maven）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.concurrent</groupId>
    <artifactId>high-concurrency-review</artifactId>
    <version>1.0-SNAPSHOT</version>
    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.24</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>3.7.0</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.36</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.11</version>
        </dependency>
    </dependencies>
</project>
```

---

## 模块 1：并发基础（base 包）

### 知识点梳理

| 考点 | 说明 |
|------|------|
| 线程创建 3 种方式 | 继承 Thread、实现 Runnable、实现 Callable（带返回值） |
| 线程生命周期 | New → Runnable → Running → Blocked → Terminated |
| volatile | 保证可见性、禁止指令重排序，**不保证原子性** |
| CAS | Compare And Swap，无锁机制，底层依赖 Unsafe 类，存在 ABA 问题 |

### 代码实现

#### 线程创建三种方式

```java
package com.concurrent.review.base;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;

@Slf4j
public class ThreadDemo {
    static class MyThread extends Thread {
        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                log.info("MyThread: {}", i);
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }
        }
    }
    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                log.info("MyRunnable: {}", i);
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }
        }
    }
    static class MyCallable implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            int sum = 0;
            for (int i = 1; i <= 5; i++) { sum += i; Thread.sleep(100); }
            return sum;
        }
    }
    public static void main(String[] args) throws Exception {
        new MyThread().start();
        new Thread(new MyRunnable()).start();
        FutureTask<Integer> ft = new FutureTask<>(new MyCallable());
        new Thread(ft).start();
        log.info("Callable 结果：{}", ft.get());
    }
}
```

#### volatile 关键字演示

```java
@Slf4j
public class VolatileDemo {
    private static volatile boolean flag = false;

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> { while (!flag) ; log.info("感知到 flag 变化"); }).start();
        Thread.sleep(1000);
        flag = true;
        log.info("主线程修改 flag 为 true");
    }
}
```

#### CAS 原理与 Unsafe 类

```java
@Slf4j
public class CasDemo {
    private static int value = 10;

    private static Unsafe getUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    public static void main(String[] args) throws Exception {
        Unsafe unsafe = getUnsafe();
        long offset = unsafe.objectFieldOffset(CasDemo.class.getDeclaredField("value"));
        boolean success1 = unsafe.compareAndSwapInt(CasDemo.class, offset, 10, 20);
        log.info("CAS1: {}, value={}", success1, value); // 成功，value=20
        boolean success2 = unsafe.compareAndSwapInt(CasDemo.class, offset, 10, 30);
        log.info("CAS2: {}, value={}", success2, value); // 失败，value 仍为 20
    }
}
```

---

## 模块 2：原子操作（atomic 包）

### 知识点梳理

| 考点 | 说明 |
|------|------|
| 原子类核心 | 解决 volatile 不保证原子性问题，底层 CAS，无锁高效 |
| 常用类 | AtomicInteger、AtomicLong、AtomicBoolean、AtomicReference |
| 原子数组 | AtomicIntegerArray |
| 解决 ABA | AtomicStampedReference（带版本号） |

### 代码实现

```java
@Slf4j
public class AtomicDemo {
    // AtomicInteger 保证原子性
    public static void testAtomicInteger() throws InterruptedException {
        AtomicInteger ai = new AtomicInteger(0);
        for (int i = 0; i < 10; i++)
            new Thread(() -> { for (int j = 0; j < 1000; j++) ai.incrementAndGet(); }).start();
        Thread.sleep(2000);
        log.info("结果：{}（预期 10000）", ai.get());
    }

    // AtomicStampedReference 解决 ABA
    public static void testABA() {
        AtomicStampedReference<Integer> asr = new AtomicStampedReference<>(10, 1);
        new Thread(() -> {
            int stamp = asr.getStamp();
            asr.compareAndSet(10, 20, stamp, stamp + 1);
            asr.compareAndSet(20, 10, asr.getStamp(), asr.getStamp() + 1);
        }).start();
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            boolean success = asr.compareAndSet(10, 30, 1, asr.getStamp() + 1);
            log.info("ABA 解决：CAS={}", success); // 失败，版本号不匹配
        }).start();
    }
}
```

---

## 模块 3：锁机制（lock 包）

### 知识点梳理

| 锁类型 | 特点 |
|--------|------|
| **synchronized** | JVM 层面，自动释放锁，不可中断，非公平锁（默认），可重入 |
| **ReentrantLock** | API 层面，手动释放（try-finally），可中断/超时/公平锁，支持 Condition |
| **ReadWriteLock** | 读写分离，读共享、写独占，适合读多写少场景 |
| **分布式锁** | Redis setnx + 过期时间 + Lua 脚本原子释放 |

### synchronized 演示

```java
@Slf4j
public class SynchronizedDemo {
    private int count = 0;

    public synchronized void increment() { count++; }                    // 实例方法锁
    public static synchronized void staticMethod() { /* Class 对象锁 */ } // 静态方法锁
    public void codeBlock() { synchronized (this) { count++; } }         // 代码块锁

    // 可重入性
    public synchronized void reentrantMethod1() { reentrantMethod2(); }
    public synchronized void reentrantMethod2() { /* 同一线程可重入 */ }
}
```

### ReentrantLock 演示

```java
@Slf4j
public class ReentrantLockDemo {
    private static final Lock lock = new ReentrantLock(true); // 公平锁
    private int count = 0;

    public void increment() {
        lock.lock();
        try { count++; } finally { lock.unlock(); }
    }

    public void testInterrupt() throws InterruptedException {
        lock.lockInterruptibly(); // 可中断锁
        try { /* ... */ } finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    // Condition 条件变量（类似 wait/notify）
    private final Condition condition = lock.newCondition();
    public void awaitCondition() {
        lock.lock();
        try { condition.await(); } // 释放锁等待
        finally { lock.unlock(); }
    }
    public void signalCondition() {
        lock.lock();
        try { condition.signal(); } // 唤醒
        finally { lock.unlock(); }
    }
}
```

### 分布式锁（Redis 实现，可选）

```java
@Slf4j
public class RedisDistributedLock {
    private final Jedis jedis = new Jedis("localhost", 6379);
    private final String lockKey, lockValue;
    private final long expireTime;

    public boolean lock() {
        String result = jedis.set(lockKey, lockValue, "NX", "PX", expireTime);
        return "OK".equals(result);
    }

    public boolean unlock() {
        String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = (Long) jedis.eval(lua, Collections.singletonList(lockKey),
                Collections.singletonList(lockValue));
        return result == 1;
    }
}
```

---

## 模块 4：线程池（threadpool 包）

### 知识点梳理

| 考点 | 说明 |
|------|------|
| ThreadPoolExecutor 核心参数 | corePoolSize、maximumPoolSize、keepAliveTime、workQueue、handler |
| 执行流程 | 核心线程 → 任务队列 → 非核心线程 → 拒绝策略 |
| 4 种拒绝策略 | AbortPolicy（抛异常）、CallerRunsPolicy（调用者执行）、DiscardPolicy（丢弃）、DiscardOldestPolicy（丢弃最老） |
| Executors 弊端 | FixedThreadPool 无界队列易 OOM，CachedThreadPool 最大线程数无限 |

### 代码实现

```java
@Slf4j
public class ThreadPoolDemo {
    public static ThreadPoolExecutor createCustomThreadPool() {
        return new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public static void testRejectedPolicy() {
        // 核心 1，最大 2，队列 1 → 第 4 个任务触发拒绝
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 2, 60L,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        // 测试 4 种拒绝策略...
    }
}
```

| 拒绝策略 | 行为 |
|----------|------|
| **AbortPolicy** | 抛 RejectedExecutionException |
| **CallerRunsPolicy** | 由调用线程执行被拒绝的任务 |
| **DiscardPolicy** | 静默丢弃，无异常 |
| **DiscardOldestPolicy** | 丢弃队列中最老的任务 |

---

## 模块 5：并发工具类（tool 包）

### 三大工具对比

| 工具 | 逻辑 | 可重复 | 场景 |
|------|------|--------|------|
| **CountDownLatch** | 倒计时，等待其他线程完成 | 否 | 主线程等待子线程 |
| **CyclicBarrier** | 线程互相等待，同时出发 | 是 | 多线程阶段性同步 |
| **Semaphore** | 控制并发访问数 | 是 | 接口限流 |

### 代码实现

```java
@Slf4j
public class ConcurrentToolDemo {
    // CountDownLatch：等待 3 个线程完成后继续
    public static void testCountDownLatch() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            new Thread(() -> { cdl.countDown(); }).start();
        }
        cdl.await();
    }

    // CyclicBarrier：3 个线程互相等待后同时继续
    public static void testCyclicBarrier() {
        CyclicBarrier cb = new CyclicBarrier(3,
                () -> log.info("所有线程到达屏障"));
        for (int i = 0; i < 3; i++)
            new Thread(() -> { cb.await(); }).start();
    }

    // Semaphore：控制并发数为 2
    public static void testSemaphore() throws InterruptedException {
        Semaphore sem = new Semaphore(2);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                sem.acquire();
                // 访问资源
                sem.release();
            }).start();
        }
    }
}
```
