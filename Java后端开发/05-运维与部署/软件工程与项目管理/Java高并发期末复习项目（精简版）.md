Java高并发期末复习项目（精简版）
一、项目概述
1.1 项目目标
专为Java高并发期末复习设计，以“理论+精简实战”覆盖核心考点，包含线程基础、锁机制、原子操作、线程池、并发工具类，代码可直接运行调试，聚焦期末常考内容。
核心考点：线程创建与生命周期、synchronized/Lock、Atomic原子类、ThreadPoolExecutor、CountDownLatch/CyclicBarrier/Semaphore、volatile、CAS。
1.2 项目环境
JDK 1.8（贴合考点）、IntelliJ IDEA、Maven、JUnit 4/5
1.3 项目结构
com.concurrent.review
├── base          // 并发基础（线程、volatile、CAS）
├── atomic        // 原子操作（Atomic系列）
├── lock          // 锁机制（synchronized、Lock、读写锁）
├── threadpool    // 线程池（ThreadPoolExecutor）
├── tool          // 并发工具类（三大工具）
└── test          // 测试用例
二、Maven核心依赖
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
        <!-- Lombok简化代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.24</version>
            <scope>provided</scope>
        </dependency>
        <!-- JUnit测试 -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
        <!-- 日志依赖 -->
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
三、各模块精简实现（核心考点+代码）
模块1：并发基础（base包）
核心考点
线程创建3种方式、生命周期；volatile（可见性、禁止重排，不保证原子性）；CAS（无锁，ABA问题）
核心代码（ThreadDemo.java + VolatileDemo.java + CasDemo.java 合并精简）
package com.concurrent.review.base;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
@Slf4j
public class BaseDemo {
    // 1. 线程创建3种方式
    static class ThreadCreate {
        // 方式1：继承Thread
        static class MyThread extends Thread {
            @Override
            public void run() { log.info("Thread方式执行"); }
        }
        // 方式2：实现Runnable
        static class MyRunnable implements Runnable {
            @Override
            public void run() { log.info("Runnable方式执行"); }
        }
        // 方式3：实现Callable（带返回值）
        static class MyCallable implements Callable<Integer> {
            @Override
            public Integer call() { return 100; }
        }
    }
    // 2. volatile演示（保证可见性，不保证原子性）
    private static volatile boolean flag = false;
    private static volatile int count = 0;
    public static void testVolatile() throws InterruptedException {
        new Thread(() -> {
            while (!flag) ;
            log.info("感知到flag变化");
        }).start();
        Thread.sleep(500);
        flag = true;
        // 验证不保证原子性
        for (int i = 0; i < 10; i++) {
            new Thread(() -> { for (int j = 0; j < 1000; j++) count++; }).start();
        }
        Thread.sleep(1000);
        log.info("volatile不保证原子性，count={}（预期10000）", count);
    }
    // 3. CAS原理演示
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
        // 测试线程创建
        new ThreadCreate.MyThread().start();
        new Thread(new ThreadCreate.MyRunnable()).start();
        FutureTask<Integer> ft = new FutureTask<>(new ThreadCreate.MyCallable());
        new Thread(ft).start();
        log.info("Callable返回值：{}", ft.get());
        // 测试volatile和CAS
        testVolatile();
        testCAS();
    }
}
模块2：原子操作（atomic包）
核心考点
Atomic系列原子类（底层CAS）、AtomicStampedReference解决ABA问题
核心代码（AtomicDemo.java）
package com.concurrent.review.atomic;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
@Slf4j
public class AtomicDemo {
    // 1. AtomicInteger保证原子性
    public static void testAtomicInteger() throws InterruptedException {
        AtomicInteger ai = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> { for (int j = 0; j < 1000; j++) ai.incrementAndGet(); }).start();
        }
        Thread.sleep(1000);
        log.info("AtomicInteger结果：{}（预期10000）", ai.get());
    }
    // 2. AtomicStampedReference解决ABA问题
    public static void testABA() {
        AtomicStampedReference<Integer> asr = new AtomicStampedReference<>(10, 1);
        // 线程1：ABA操作
        new Thread(() -> {
            int stamp = asr.getStamp();
            asr.compareAndSet(10, 20, stamp, stamp+1);
            asr.compareAndSet(20, 10, asr.getStamp(), asr.getStamp()+1);
            log.info("ABA操作后：值={}，版本号={}", asr.getReference(), asr.getStamp());
        }).start();
        // 线程2：验证ABA问题解决
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
            int stamp = asr.getStamp(); // 版本号已变，CAS失败
            boolean success = asr.compareAndSet(10, 30, 1, stamp+1);
            log.info("CAS结果：{}，最终值={}", success, asr.getReference());
        }).start();
    }
    public static void main(String[] args) throws InterruptedException {
        testAtomicInteger();
        testABA();
    }
}
模块3：锁机制（lock包）
核心考点
synchronized（内置锁、可重入）、ReentrantLock（手动锁、可中断/超时/公平锁）、读写锁（读共享、写独占）
核心代码（LockDemo.java 合并精简）
package com.concurrent.review.lock;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
@Slf4j
public class LockDemo {
    // 1. synchronized演示（可重入、原子性）
    private int count = 0;
    public synchronized void syncIncrement() { count++; }
    public synchronized void reentrantTest() {
        log.info("重入锁1");
        reentrantTest2();
    }
    public synchronized void reentrantTest2() { log.info("重入锁2"); }
    // 2. ReentrantLock演示（公平锁、可中断）
    private final Lock lock = new ReentrantLock(true);
    public void lockIncrement() {
        lock.lock();
        try { count++; } finally { lock.unlock(); }
    }
    // 3. 读写锁演示（读共享、写独占）
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();
    private String cache = "初始缓存";
    public void readCache() {
        readLock.lock();
        try { log.info("读缓存：{}", cache); } finally { readLock.unlock(); }
    }
    public void writeCache(String newCache) {
        writeLock.lock();
        try { cache = newCache; log.info("写缓存：{}", cache); } finally { writeLock.unlock(); }
    }
    public static void main(String[] args) {
        LockDemo demo = new LockDemo();
        // 测试synchronized重入
        new Thread(demo::reentrantTest).start();
        // 测试读写锁
        for (int i = 0; i < 3; i++) { new Thread(demo::readCache).start(); }
        new Thread(() -> demo.writeCache("新缓存")).start();
    }
}
模块4：线程池（threadpool包）
核心考点
ThreadPoolExecutor核心参数、执行流程、4种拒绝策略；Executors工具类弊端
核心代码（ThreadPoolDemo.java）
package com.concurrent.review.threadpool;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
@Slf4j
public class ThreadPoolDemo {
    // 自定义线程池（核心参数：核心5、最大10、队列20、拒绝策略Abort）
    public static ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(
                5, 10, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
    // 测试执行流程和拒绝策略
    public static void main(String[] args) {
        ThreadPoolExecutor threadPool = createThreadPool();
        // 提交30个任务（5核心+20队列+5非核心，刚好处理）
        for (int i = 0; i < 30; i++) {
            int taskId = i;
            threadPool.submit(() -> log.info("线程{}执行任务{}", Thread.currentThread().getName(), taskId));
        }
        // 测试拒绝策略（核心1、最大2、队列1，第4个任务拒绝）
        ThreadPoolExecutor smallPool = new ThreadPoolExecutor(1,2,60L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1));
        try {
            for (int i = 0; i < 4; i++) smallPool.execute(() -> log.info("执行任务"));
        } catch (Exception e) { log.error("任务拒绝（AbortPolicy）"); }
        threadPool.shutdown();
        smallPool.shutdown();
    }
}
模块5：并发工具类（tool包）
核心考点
CountDownLatch（倒计时、不可重复）、CyclicBarrier（循环屏障、可重复）、Semaphore（限流）
核心代码（ConcurrentToolDemo.java）
package com.concurrent.review.tool;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
@Slf4j
public class ConcurrentToolDemo {
    // 1. CountDownLatch（倒计时，不可重复）
    public static void testCountDownLatch() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                log.info("线程执行完成");
                cdl.countDown();
            }).start();
        }
        cdl.await();
        log.info("所有线程完成，主线程继续");
    }
    // 2. CyclicBarrier（循环屏障，可重复）
    public static void testCyclicBarrier() {
        CyclicBarrier cb = new CyclicBarrier(3, () -> log.info("所有线程到达屏障"));
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                log.info("线程前往屏障");
                try { cb.await(); } catch (Exception e) { e.printStackTrace(); }
                log.info("线程越过屏障");
            }).start();
        }
    }
    // 3. Semaphore（限流，控制并发数2）
    public static void testSemaphore() {
        Semaphore sem = new Semaphore(2);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    sem.acquire();
                    log.info("获取许可，访问资源");
                    Thread.sleep(500);
                } catch (InterruptedException e) { e.printStackTrace(); }
                finally { sem.release(); }
            }).start();
        }
    }
    public static void main(String[] args) throws InterruptedException {
        testCountDownLatch();
        testCyclicBarrier();
        testSemaphore();
    }
}
