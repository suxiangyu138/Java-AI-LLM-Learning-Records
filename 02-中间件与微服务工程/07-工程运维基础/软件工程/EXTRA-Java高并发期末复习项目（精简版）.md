# Java 高并发期末复习项目（精简版）

> "理论 + 精简实战"覆盖期末核心考点，包含线程基础、锁机制、原子操作、线程池、并发工具类，代码可直接运行调试。

---

## 目录

- [一、项目概述](#一项目概述)
- [二、Maven 核心依赖](#二maven-核心依赖)
- [三、各模块精简实现](#三各模块精简实现)

---

## 一、项目概述

### 1.1 核心考点

| 分类 | 考点 |
|------|------|
| 线程基础 | 线程创建与生命周期 |
| 锁机制 | synchronized / Lock |
| 原子操作 | Atomic 原子类、CAS |
| 线程池 | ThreadPoolExecutor |
| 并发工具 | CountDownLatch / CyclicBarrier / Semaphore |
| 关键字 | volatile、CAS |

### 1.2 项目环境

| 项 | 说明 |
|----|------|
| JDK | 1.8（贴合考点） |
| IDE | IntelliJ IDEA |
| 构建 | Maven |
| 测试 | JUnit 4/5 |

### 1.3 项目结构

```
com.concurrent.review
├── base          // 并发基础（线程、volatile、CAS）
├── atomic        // 原子操作（Atomic 系列）
├── lock          // 锁机制（synchronized、Lock、读写锁）
├── threadpool    // 线程池（ThreadPoolExecutor）
├── tool          // 并发工具类（三大工具）
└── test          // 测试用例
```

---

## 二、Maven 核心依赖

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

## 三、各模块精简实现

### 模块 1：并发基础（base 包）

> **考点：** 线程创建 3 种方式、生命周期；volatile（可见性、禁止重排，不保证原子性）；CAS（无锁，ABA 问题）

```java
package com.concurrent.review.base;

import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@Slf4j
public class BaseDemo {
    // 1. 线程创建 3 种方式
    static class ThreadCreate {
        static class MyThread extends Thread {
            @Override
            public void run() { log.info("Thread 方式执行"); }
        }
        static class MyRunnable implements Runnable {
            @Override
            public void run() { log.info("Runnable 方式执行"); }
        }
        static class MyCallable implements Callable<Integer> {
            @Override
            public Integer call() { return 100; }
        }
    }

    // 2. volatile 演示
    private static volatile boolean flag = false;
    private static volatile int count = 0;

    public static void testVolatile() throws InterruptedException {
        new Thread(() -> { while (!flag) ; log.info("感知到 flag 变化"); }).start();
        Thread.sleep(500);
        flag = true;
        for (int i = 0; i < 10; i++) {
            new Thread(() -> { for (int j = 0; j < 1000; j++) count++; }).start();
        }
        Thread.sleep(1000);
        log.info("volatile 不保证原子性，count={}（预期 10000）", count);
    }

    // 3. CAS 原理演示
    private static int value = 10;
    private static Unsafe getUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    public static void testCAS() throws Exception {
        Unsafe unsafe = getUnsafe();
        long offset = unsafe.objectFieldOffset(BaseDemo.class.getDeclaredField("value"));
        boolean success1 = unsafe.compareAndSwapInt(BaseDemo.class, offset, 10, 20);
        boolean success2 = unsafe.compareAndSwapInt(BaseDemo.class, offset, 10, 30);
        log.info("CAS1:{}（value={}）, CAS2:{}（value={}）", success1, value, success2, value);
    }

    public static void main(String[] args) throws Exception {
        new ThreadCreate.MyThread().start();
        new Thread(new ThreadCreate.MyRunnable()).start();
        FutureTask<Integer> ft = new FutureTask<>(new ThreadCreate.MyCallable());
        new Thread(ft).start();
        log.info("Callable 返回值：{}", ft.get());
        testVolatile();
        testCAS();
    }
}
```

---

### 模块 2：原子操作（atomic 包）

> **考点：** Atomic 系列原子类（底层 CAS）、AtomicStampedReference 解决 ABA 问题

```java
package com.concurrent.review.atomic;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

@Slf4j
public class AtomicDemo {
    public static void testAtomicInteger() throws InterruptedException {
        AtomicInteger ai = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> { for (int j = 0; j < 1000; j++) ai.incrementAndGet(); }).start();
        }
        Thread.sleep(1000);
        log.info("AtomicInteger 结果：{}（预期 10000）", ai.get());
    }

    public static void testABA() {
        AtomicStampedReference<Integer> asr = new AtomicStampedReference<>(10, 1);
        new Thread(() -> {
            int stamp = asr.getStamp();
            asr.compareAndSet(10, 20, stamp, stamp + 1);
            asr.compareAndSet(20, 10, asr.getStamp(), asr.getStamp() + 1);
            log.info("ABA 操作后：值={}，版本号={}", asr.getReference(), asr.getStamp());
        }).start();
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            int stamp = asr.getStamp();
            boolean success = asr.compareAndSet(10, 30, 1, stamp + 1);
            log.info("CAS 结果：{}，最终值={}", success, asr.getReference());
        }).start();
    }

    public static void main(String[] args) throws InterruptedException {
        testAtomicInteger();
        testABA();
    }
}
```

---

### 模块 3：锁机制（lock 包）

> **考点：** synchronized（内置锁、可重入）、ReentrantLock（手动锁、可中断/超时/公平锁）、读写锁（读共享、写独占）

```java
package com.concurrent.review.lock;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class LockDemo {
    private int count = 0;

    // 1. synchronized
    public synchronized void syncIncrement() { count++; }
    public synchronized void reentrantTest() { log.info("重入锁 1"); reentrantTest2(); }
    public synchronized void reentrantTest2() { log.info("重入锁 2"); }

    // 2. ReentrantLock
    private final Lock lock = new ReentrantLock(true);
    public void lockIncrement() {
        lock.lock();
        try { count++; } finally { lock.unlock(); }
    }

    // 3. 读写锁
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private String cache = "初始缓存";

    public void readCache() {
        rwLock.readLock().lock();
        try { log.info("读缓存：{}", cache); } finally { rwLock.readLock().unlock(); }
    }
    public void writeCache(String newCache) {
        rwLock.writeLock().lock();
        try { cache = newCache; log.info("写缓存：{}", cache); } finally { rwLock.writeLock().unlock(); }
    }

    public static void main(String[] args) {
        LockDemo demo = new LockDemo();
        new Thread(demo::reentrantTest).start();
        for (int i = 0; i < 3; i++) { new Thread(demo::readCache).start(); }
        new Thread(() -> demo.writeCache("新缓存")).start();
    }
}
```

---

### 模块 4：线程池（threadpool 包）

> **考点：** ThreadPoolExecutor 核心参数、执行流程、4 种拒绝策略

```java
package com.concurrent.review.threadpool;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;

@Slf4j
public class ThreadPoolDemo {
    public static ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(
                5, 10, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public static void main(String[] args) {
        ThreadPoolExecutor threadPool = createThreadPool();
        for (int i = 0; i < 30; i++) {
            int taskId = i;
            threadPool.submit(() -> log.info("线程{}执行任务{}",
                    Thread.currentThread().getName(), taskId));
        }
        // 测试拒绝策略
        ThreadPoolExecutor smallPool = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
        try {
            for (int i = 0; i < 4; i++) smallPool.execute(() -> log.info("执行任务"));
        } catch (Exception e) { log.error("任务拒绝（AbortPolicy）"); }
        threadPool.shutdown();
        smallPool.shutdown();
    }
}
```

---

### 模块 5：并发工具类（tool 包）

> **考点：** CountDownLatch（倒计时、不可重复）、CyclicBarrier（循环屏障、可重复）、Semaphore（限流）

```java
package com.concurrent.review.tool;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;

@Slf4j
public class ConcurrentToolDemo {
    // CountDownLatch
    public static void testCountDownLatch() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            new Thread(() -> { log.info("线程执行完成"); cdl.countDown(); }).start();
        }
        cdl.await();
        log.info("所有线程完成，主线程继续");
    }

    // CyclicBarrier
    public static void testCyclicBarrier() {
        CyclicBarrier cb = new CyclicBarrier(3, () -> log.info("所有线程到达屏障"));
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                log.info("线程前往屏障");
                try { cb.await(); } catch (Exception e) {}
                log.info("线程越过屏障");
            }).start();
        }
    }

    // Semaphore
    public static void testSemaphore() {
        Semaphore sem = new Semaphore(2);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    sem.acquire();
                    log.info("获取许可，访问资源");
                    Thread.sleep(500);
                } catch (InterruptedException e) {} finally { sem.release(); }
            }).start();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        testCountDownLatch();
        testCyclicBarrier();
        testSemaphore();
    }
}
```

---

## 三大工具对比

| 工具 | 核心逻辑 | 可重复 | 典型场景 |
|------|----------|--------|----------|
| **CountDownLatch** | 倒计时，等待其他线程完成 | 否 | 主线程等待子线程 |
| **CyclicBarrier** | 线程互相等待，同时出发 | 是 | 多线程阶段性同步 |
| **Semaphore** | 控制并发访问数量 | 是 | 接口限流、资源池 |
