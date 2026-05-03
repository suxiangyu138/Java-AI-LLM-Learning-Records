03.31 01:03
Java高并发期末复习项目（完整版）
一、项目概述
1.1 项目目标
本项目专为Java高并发期末复习设计，整合高并发核心知识点，通过“理论+实战”的方式，帮助巩固并发编程基础、线程池、锁机制、原子操作、并发工具类、分布式并发等核心内容，实现可运行、可调试、可扩展的高并发demo，覆盖期末常考考点，同时具备完整的功能逻辑，便于理解和复习。
核心复习考点：线程创建与生命周期、线程同步（synchronized、Lock）、原子类（Atomic系列）、线程池（ThreadPoolExecutor）、并发工具类（CountDownLatch、CyclicBarrier、Semaphore）、线程通信、volatile关键字、CAS原理、分布式锁（可选）。
1.2 项目环境
JDK版本：JDK 1.8（高并发核心特性均基于JDK8实现，贴合期末考点）
开发工具：IntelliJ IDEA（推荐，便于调试和运行）
依赖管理：Maven（简化依赖引入，如Lombok、JUnit、Redis（分布式锁用））
测试工具：JUnit 4/5、JMeter（可选，用于模拟高并发场景，验证功能）
1.3 项目结构
采用分层结构，按知识点划分模块，便于针对性复习，结构清晰，符合Java开发规范：
com.concurrent.review
├── base          // 并发基础模块（线程创建、生命周期、volatile、CAS）
├── lock          // 锁机制模块（synchronized、Lock、读写锁、分布式锁）
├── atomic        // 原子操作模块（Atomic系列、Unsafe类）
├── threadpool    // 线程池模块（ThreadPoolExecutor、Executors工具类）
├── tool          // 并发工具类模块（CountDownLatch、CyclicBarrier等）
├── communication // 线程通信模块（wait/notify、Condition）
├── test          // 测试模块（各知识点测试用例、高并发模拟）
└── util          // 工具类（日志、常量、通用方法）
二、项目依赖配置（Maven）
在pom.xml中引入所需依赖，简化开发和测试，核心依赖如下（复制即可使用）：
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
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    &lt;dependencies&gt;
        <!-- Lombok：简化实体类代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.24</version>
            <scope>provided</scope&gt;
        &lt;/dependency&gt;
        <!-- JUnit：测试用例 -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
        <!-- Redis：分布式锁依赖（可选，期末若考分布式并发则需引入） -->
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>3.7.0</version>
        &lt;/dependency&gt;
        <!-- 日志依赖：便于查看并发执行流程 -->
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
三、各模块详细实现（含知识点解析+完整代码）
模块1：并发基础（base包）
1.1 知识点梳理（期末常考）
线程创建方式：继承Thread类、实现Runnable接口、实现Callable接口（带返回值）
线程生命周期：新建（New）、就绪（Runnable）、运行（Running）、阻塞（Blocked）、死亡（Terminated）
volatile关键字：保证可见性、禁止指令重排序，不保证原子性（常考区别于synchronized）
CAS原理：比较并交换（Compare And Swap），无锁机制，底层依赖Unsafe类，存在ABA问题
1.2 代码实现
1.2.1 线程创建三种方式（ThreadDemo.java）
package com.concurrent.review.base;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
/**
 * 线程创建三种方式：复习核心考点
 * 1. 继承Thread类（重写run方法，无返回值，不能资源共享）
 * 2. 实现Runnable接口（重写run方法，无返回值，可资源共享）
 * 3. 实现Callable接口（重写call方法，有返回值，可抛异常，结合FutureTask使用）
 */
@Slf4j
public class ThreadDemo {
    // 方式1：继承Thread类
    static class MyThread extends Thread {
        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                log.info("MyThread: {}", i);
                try {
                    Thread.sleep(100); // 模拟线程执行耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // 方式2：实现Runnable接口
    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                log.info("MyRunnable: {}", i);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // 方式3：实现Callable接口（带返回值）
    static class MyCallable implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            int sum = 0;
            for (int i = 1; i <= 5; i++) {
                sum += i;
                log.info("MyCallable: 累加i={}, 当前和={}", i, sum);
                Thread.sleep(100);
            }
            return sum; // 返回累加结果
        }
    }
    // 测试方法
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 测试方式1
        MyThread thread = new MyThread();
        thread.start(); // 启动线程（注意：不能调用run()，否则是普通方法调用）
        // 测试方式2
        Thread runnableThread = new Thread(new MyRunnable());
        runnableThread.start();
        // 测试方式3
        FutureTask<Integer> futureTask = new FutureTask<>(new MyCallable());
        Thread callableThread = new Thread(futureTask);
        callableThread.start();
        Integer result = futureTask.get(); // 获取Callable的返回值（会阻塞，直到线程执行完成）
        log.info("MyCallable执行结果：{}", result);
        // 线程优先级（补充考点：1-10，默认5，优先级高不一定先执行，只是概率高）
        thread.setPriority(Thread.MAX_PRIORITY);
        runnableThread.setPriority(Thread.MIN_PRIORITY);
    }
}
1.2.2 volatile关键字演示（VolatileDemo.java）
package com.concurrent.review.base;
import lombok.extern.slf4j.Slf4j;
/**
 * volatile关键字复习：保证可见性、禁止指令重排序，不保证原子性
 * 对比：无volatile时，主线程修改变量，子线程可能无法感知（可见性问题）
 * 重点：volatile不能替代synchronized，原子性需要锁或原子类保证
 */
@Slf4j
public class VolatileDemo {
    // 未加volatile：子线程可能陷入死循环（主线程修改flag后，子线程看不到）
    // private static boolean flag = false;
    // 加volatile：保证flag的可见性，主线程修改后，子线程立即感知
    private static volatile boolean flag = false;
    public static void main(String[] args) throws InterruptedException {
        // 子线程：循环判断flag，为true则退出
        new Thread(() -> {
            log.info("子线程启动，开始判断flag...");
            while (!flag) {
                // 空循环，模拟子线程持续执行
            }
            log.info("子线程感知到flag变化，退出循环");
        }).start();
        // 主线程：休眠1秒后，修改flag为true
        Thread.sleep(1000);
        flag = true;
        log.info("主线程修改flag为true");
    }
    // 补充：volatile不保证原子性的演示
    private static volatile int count = 0;
    public static void testAtomicity() throws InterruptedException {
        // 10个线程，每个线程执行1000次count++
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    count++; // count++不是原子操作（读取、加1、写入三步）
                }
            }).start();
        }
        Thread.sleep(2000); // 等待所有线程执行完成
        log.info("count最终结果：{}（预期10000，实际小于10000，证明volatile不保证原子性）", count);
    }
    public static void main2(String[] args) throws InterruptedException {
        testAtomicity();
    }
}
1.2.3 CAS原理与Unsafe类（CasDemo.java）
package com.concurrent.review.base;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
/**
 * CAS原理复习：Compare And Swap，无锁机制，底层依赖Unsafe类
 * 核心逻辑：期望值 == 当前内存值 → 则更新为目标值；否则不更新，返回当前内存值
 * 考点：ABA问题、CAS的优缺点、Unsafe类的使用（反射获取，不能直接实例化）
 */
@Slf4j
public class CasDemo {
    // 定义原子变量（模拟CAS操作的变量）
    private static int value = 10;
    // 获取Unsafe实例（Unsafe是单例，只能通过反射获取）
    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true); // 暴力访问私有字段
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            log.error("获取Unsafe实例失败", e);
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        Unsafe unsafe = getUnsafe();
        // 获取value变量的内存偏移量（Unsafe操作内存需要偏移量）
        try {
            long offset = unsafe.objectFieldOffset(CasDemo.class.getDeclaredField("value"));
            log.info("value变量内存偏移量：{}", offset);
            // CAS操作：参数（对象、偏移量、期望值、目标值）
            boolean success1 = unsafe.compareAndSwapInt(CasDemo.class, offset, 10, 20);
            log.info("第一次CAS操作：{}，操作后value={}", success1, value); // 成功，value=20
            // 第二次CAS：期望值是10，当前value是20，操作失败
            boolean success2 = unsafe.compareAndSwapInt(CasDemo.class, offset, 10, 30);
            log.info("第二次CAS操作：{}，操作后value={}", success2, value); // 失败，value仍为20
            // 补充：ABA问题演示（值从A→B→A，CAS无法感知中间变化）
            // 解决ABA问题：使用AtomicStampedReference（带版本号）
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
模块2：原子操作（atomic包）
2.1 知识点梳理（期末常考）
原子类核心：解决volatile不保证原子性的问题，底层基于CAS实现，无锁、高效
常用原子类：AtomicInteger、AtomicLong、AtomicBoolean、AtomicReference（引用类型）
原子数组：AtomicIntegerArray（数组元素的原子操作）
解决ABA问题：AtomicStampedReference（带版本号的原子引用）
2.2 代码实现（AtomicDemo.java）
package com.concurrent.review.atomic;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicStampedReference;
/**
 * 原子类复习：解决原子性问题，底层CAS实现
 * 覆盖常用原子类，以及ABA问题的解决
 */
@Slf4j
public class AtomicDemo {
    // 1. 基本原子类：AtomicInteger
    public static void testAtomicInteger() throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        // 10个线程，每个线程执行1000次自增
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    atomicInteger.incrementAndGet(); // 原子自增（替代count++）
                    // 其他常用方法：decrementAndGet()（自减）、addAndGet(5)（加5）
                }
            }).start();
        }
        Thread.sleep(2000);
        log.info("AtomicInteger最终结果：{}（预期10000，实际等于10000，保证原子性）", atomicInteger.get());
    }
    // 2. 原子数组：AtomicIntegerArray（数组元素的原子操作）
    public static void testAtomicArray() {
        int[] arr = {1, 2, 3, 4, 5};
        AtomicIntegerArray atomicArray = new AtomicIntegerArray(arr);
        // 原子修改数组索引为2的元素（加10）
        atomicArray.addAndGet(2, 10);
        log.info("数组索引2的元素修改后：{}（原3，修改后13）", atomicArray.get(2));
        // 原子比较并交换数组元素
        boolean success = atomicArray.compareAndSet(0, 1, 100);
        log.info("数组索引0 CAS操作：{}，修改后元素：{}", success, atomicArray.get(0));
    }
    // 3. 解决ABA问题：AtomicStampedReference（带版本号）
    public static void testAtomicStampedReference() {
        // 初始化：值为10，版本号为1
        AtomicStampedReference<Integer> stampedReference = new AtomicStampedReference<>(10, 1);
        // 线程1：模拟ABA操作（10→20→10）
        new Thread(() -> {
            int stamp = stampedReference.getStamp(); // 获取当前版本号
            log.info("线程1获取当前值：{}，版本号：{}", stampedReference.getReference(), stamp);
            // 第一次CAS：10→20，版本号+1
            stampedReference.compareAndSet(10, 20, stamp, stamp + 1);
            log.info("线程1第一次CAS后：值={}，版本号={}", stampedReference.getReference(), stampedReference.getStamp());
            // 第二次CAS：20→10，版本号+1
            stamp = stampedReference.getStamp();
            stampedReference.compareAndSet(20, 10, stamp, stamp + 1);
            log.info("线程1第二次CAS后：值={}，版本号={}", stampedReference.getReference(), stampedReference.getStamp());
        }).start();
        // 线程2：尝试修改值为30（期望版本号为1）
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 等待线程1完成ABA操作
                int stamp = stampedReference.getStamp(); // 获取当前版本号（此时已变为3）
                log.info("线程2获取当前值：{}，版本号：{}", stampedReference.getReference(), stamp);
                // CAS操作：期望值10，目标值30，期望版本号1
                boolean success = stampedReference.compareAndSet(10, 30, 1, stamp + 1);
                log.info("线程2 CAS操作：{}，最终值={}，版本号={}",
                        success, stampedReference.getReference(), stampedReference.getStamp());
                // 结果：操作失败，因为版本号不匹配（期望1，实际3），解决了ABA问题
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    public static void main(String[] args) throws InterruptedException {
        log.info("=== 测试AtomicInteger ===");
        testAtomicInteger();
        log.info("\n=== 测试AtomicIntegerArray ===");
        testAtomicArray();
        log.info("\n=== 测试AtomicStampedReference（解决ABA问题） ===");
        testAtomicStampedReference();
    }
}
模块3：锁机制（lock包）
3.1 知识点梳理（期末重点）
synchronized：内置锁，JVM层面，自动释放锁，不可中断，非公平锁（默认）
Lock接口：手动锁，Java层面，需手动释放（try-finally），可中断、可超时、可公平/非公平锁
常用Lock实现：ReentrantLock（可重入锁）、ReentrantReadWriteLock（读写锁，提高并发效率）
synchronized与Lock的区别（常考简答题）：实现层面、释放方式、可中断性、公平性、条件变量等
分布式锁（可选，期末若考分布式并发）：基于Redis实现，解决分布式环境下的并发安全
3.2 代码实现
3.2.1 synchronized演示（SynchronizedDemo.java）
package com.concurrent.review.lock;
import lombok.extern.slf4j.Slf4j;
/**
 * synchronized复习：内置锁，保证原子性、可见性、有序性
 * 用法：修饰方法（实例方法、静态方法）、修饰代码块
 * 重点：可重入性、锁升级（偏向锁→轻量级锁→重量级锁）
 */
@Slf4j
public class SynchronizedDemo {
    // 共享资源
    private int count = 0;
    // 1. 修饰实例方法（锁是当前对象this）
    public synchronized void increment() {
        count++;
    }
    // 2. 修饰静态方法（锁是当前类的Class对象）
    public static synchronized void staticMethod() {
        log.info("静态方法加锁，锁是SynchronizedDemo.class");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    // 3. 修饰代码块（锁是指定的对象，灵活）
    public void codeBlock() {
        // 锁对象可以是this、Class对象、任意对象
        synchronized (this) {
            count++;
            log.info("代码块加锁，count={}", count);
        }
    }
    // 测试synchronized保证原子性
    public static void testAtomicity() throws InterruptedException {
        SynchronizedDemo demo = new SynchronizedDemo();
        // 10个线程，每个线程执行1000次increment
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    demo.increment();
                }
            }).start();
        }
        Thread.sleep(2000);
        log.info("synchronized保证原子性，count最终结果：{}（预期10000）", demo.count);
    }
    // 测试synchronized可重入性（同一线程可多次获取同一把锁）
    public synchronized void reentrantMethod1() {
        log.info("reentrantMethod1：获取锁");
        reentrantMethod2(); // 同一线程调用另一个加锁的实例方法
    }
    public synchronized void reentrantMethod2() {
        log.info("reentrantMethod2：获取同一把锁（可重入）");
    }
    public static void main(String[] args) throws InterruptedException {
        log.info("=== 测试synchronized原子性 ===");
        testAtomicity();
        log.info("\n=== 测试synchronized可重入性 ===");
        SynchronizedDemo demo = new SynchronizedDemo();
        new Thread(demo::reentrantMethod1).start();
        log.info("\n=== 测试静态方法加锁 ===");
        // 两个线程调用静态加锁方法，会排队执行（锁是Class对象，唯一）
        new Thread(SynchronizedDemo::staticMethod).start();
        new Thread(SynchronizedDemo::staticMethod).start();
    }
}
3.2.2 ReentrantLock演示（ReentrantLockDemo.java）
package com.concurrent.review.lock;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 * ReentrantLock复习：可重入锁，实现Lock接口，手动锁
 * 核心特性：可中断、可超时、可公平/非公平锁、条件变量（Condition）
 * 重点：与synchronized的区别，手动释放锁（try-finally必须写，否则会造成死锁）
 */
@Slf4j
public class ReentrantLockDemo {
    // 1. 创建ReentrantLock（默认非公平锁，参数true为公平锁）
    private static final Lock lock = new ReentrantLock(true); // 公平锁
    private int count = 0;
    // 测试ReentrantLock保证原子性
    public void increment() {
        // 手动获取锁
        lock.lock();
        try {
            // 临界区：共享资源操作
            count++;
            log.info("当前线程：{}，count={}", Thread.currentThread().getName(), count);
        } finally {
            // 手动释放锁（必须在finally中，防止异常导致锁无法释放）
            lock.unlock();
        }
    }
    // 测试可中断性（lock.lockInterruptibly()）
    public void testInterrupt() {
        try {
            // 可中断锁：线程在等待锁时，可被其他线程中断
            lock.lockInterruptibly();
            log.info("线程{}获取到可中断锁", Thread.currentThread().getName());
            Thread.sleep(2000); // 模拟执行耗时
        } catch (InterruptedException e) {
            log.info("线程{}在等待锁时被中断", Thread.currentThread().getName());
        } finally {
            // 判断当前线程是否持有锁，避免释放未持有的锁（报异常）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    // 测试可超时性（lock.tryLock(long timeout, TimeUnit unit)）
    public void testTimeout() {
        try {
            // 尝试获取锁，超时时间1秒，获取不到则放弃
            boolean acquired = lock.tryLock(1, java.util.concurrent.TimeUnit.SECONDS);
            if (acquired) {
                log.info("线程{}获取到超时锁", Thread.currentThread().getName());
                Thread.sleep(2000);
            } else {
                log.info("线程{}获取超时锁失败（超时1秒）", Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    // 测试条件变量（Condition）：实现线程间通信（类似wait/notify）
    private final Condition condition = lock.newCondition();
    private boolean flag = false;
    public void waitCondition() {
        lock.lock();
        try {
            // 循环判断（避免虚假唤醒）
            while (!flag) {
                log.info("线程{}等待条件满足", Thread.currentThread().getName());
                condition.await(); // 释放锁，进入等待状态
            }
            log.info("线程{}条件满足，继续执行", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
    public void signalCondition() {
        lock.lock();
        try {
            flag = true;
            log.info("线程{}唤醒等待的线程", Thread.currentThread().getName());
            condition.signal(); // 唤醒一个等待的线程（signalAll()唤醒所有）
        } finally {
            lock.unlock();
        }
    }
    public static void main(String[] args) throws InterruptedException {
        ReentrantLockDemo demo = new ReentrantLockDemo();
        log.info("=== 测试ReentrantLock原子性 ===");
        for (int i = 0; i < 5; i++) {
            new Thread(demo::increment, "线程" + i).start();
        }
        Thread.sleep(1000);
        log.info("\n=== 测试可中断性 ===");
        Thread t1 = new Thread(demo::testInterrupt, "中断线程1");
        Thread t2 = new Thread(demo::testInterrupt, "中断线程2");
        t1.start();
        Thread.sleep(500);
        t2.interrupt(); // 中断t2线程
        log.info("\n=== 测试可超时性 ===");
        Thread t3 = new Thread(demo::testTimeout, "超时线程1");
        Thread t4 = new Thread(demo::testTimeout, "超时线程2");
        t3.start();
        t4.start();
        Thread.sleep(2000);
        log.info("\n=== 测试条件变量 ===");
        Thread t5 = new Thread(demo::waitCondition, "等待线程");
        Thread t6 = new Thread(demo::signalCondition, "唤醒线程");
        t5.start();
        Thread.sleep(1000);
        t6.start();
    }
}
3.2.3 读写锁演示（ReadWriteLockDemo.java）
package com.concurrent.review.lock;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.locks.ReentrantReadWriteLock;
/**
 * 读写锁复习：ReentrantReadWriteLock，读写分离，提高并发效率
 * 核心特性：
 * 1. 读锁（共享锁）：多个线程可同时获取读锁，互不阻塞
 * 2. 写锁（排他锁）：只有一个线程可获取写锁，写锁与读锁、写锁与写锁互斥
 * 适用场景：读多写少（如缓存、配置读取）
 */
@Slf4j
public class ReadWriteLockDemo {
    // 创建读写锁
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    // 读锁
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    // 写锁
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();
    // 共享资源（模拟缓存）
    private String cache = "初始缓存内容";
    // 读操作：获取读锁
    public String readCache() {
        readLock.lock();
        try {
            log.info("线程{}获取读锁，读取缓存：{}", Thread.currentThread().getName(), cache);
            Thread.sleep(500); // 模拟读操作耗时
            return cache;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            readLock.unlock();
            log.info("线程{}释放读锁", Thread.currentThread().getName());
        }
    }
    // 写操作：获取写锁
    public void writeCache(String newCache) {
        writeLock.lock();
        try {
            log.info("线程{}获取写锁，修改缓存：{}→{}", Thread.currentThread().getName(), cache, newCache);
            Thread.sleep(1000); // 模拟写操作耗时
            cache = newCache;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
            log.info("线程{}释放写锁", Thread.currentThread().getName());
        }
    }
    public static void main(String[] args) {
        ReadWriteLockDemo demo = new ReadWriteLockDemo();
        // 3个读线程（同时读取，不阻塞）
        for (int i = 0; i < 3; i++) {
            new Thread(demo::readCache, "读线程" + i).start();
        }
        // 1个写线程（会阻塞读线程，直到写锁释放）
        new Thread(() -> demo.writeCache("修改后的缓存内容"), "写线程").start();
        // 再启动2个读线程（会等待写锁释放后，同时读取）
        for (int i = 3; i < 5; i++) {
            new Thread(demo::readCache, "读线程" + i).start();
        }
        // 结论：读多写少场景下，读写锁比普通锁效率高，读操作可并发，写操作独占
    }
}
3.2.4 分布式锁（Redis实现，可选）（RedisDistributedLock.java）
package com.concurrent.review.lock;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import java.util.Collections;
/**
 * 分布式锁复习（可选）：基于Redis实现，解决分布式环境下的并发安全
 * 核心逻辑：setnx（set if not exists）+ 过期时间，避免死锁
 * 考点：分布式锁的实现原理、防止死锁、释放锁的原子性
 */
@Slf4j
public class RedisDistributedLock {
    // Redis连接（实际开发中应使用连接池，此处简化）
    private final Jedis jedis = new Jedis("localhost", 6379);
    // 锁的key（唯一标识）
    private final String lockKey;
    // 锁的value（用于释放锁时校验，防止误释放）
    private final String lockValue;
    // 锁的过期时间（毫秒），防止死锁
    private final long expireTime;
    // 构造方法
    public RedisDistributedLock(String lockKey, String lockValue, long expireTime) {
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.expireTime = expireTime;
    }
    /**
     * 获取分布式锁
     * @return true：获取成功，false：获取失败
     */
    public boolean lock() {
        try {
            // setnx + 过期时间（原子操作，避免先setnx再expire导致的死锁）
            String result = jedis.set(lockKey, lockValue, "NX", "PX", expireTime);
            // result为"OK"表示获取锁成功
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("获取分布式锁失败", e);
            return false;
        }
    }
    /**
     * 释放分布式锁（原子操作，避免误释放其他线程的锁）
     * 使用Lua脚本：判断value是否匹配，匹配则删除（原子操作）
     * @return true：释放成功，false：释放失败
     */
    public boolean unlock() {
        try {
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            // 执行Lua脚本，KEYS[1]是lockKey，ARGV[1]是lockValue
            Long result = (Long) jedis.eval(luaScript, Collections.singletonList(lockKey), Collections.singletonList(lockValue));
            // result为1表示释放成功，0表示锁不存在或value不匹配
            return result == 1;
        } catch (Exception e) {
            log.error("释放分布式锁失败", e);
            return false;
        }
    }
    // 测试分布式锁
    public static void main(String[] args) throws InterruptedException {
        // 模拟两个分布式节点（线程）竞争锁
        RedisDistributedLock lock1 = new RedisDistributedLock("distributed_lock", "node1", 3000);
        RedisDistributedLock lock2 = new RedisDistributedLock("distributed_lock", "node2", 3000);
        // 线程1获取锁
        new Thread(() -> {
            if (lock1.lock()) {
                log.info("线程1获取分布式锁成功，执行业务逻辑");
                try {
                    Thread.sleep(2000); // 模拟业务执行
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock1.unlock();
                    log.info("线程1释放分布式锁");
                }
            } else {
                log.info("线程1获取分布式锁失败");
            }
        }).start();
        // 线程2尝试获取锁（线程1持有锁时，线程2获取失败）
        new Thread(() -> {
            try {
                Thread.sleep(500); // 等待线程1获取锁
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (lock2.lock()) {
                log.info("线程2获取分布式锁成功，执行业务逻辑");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock2.unlock();
                    log.info("线程2释放分布式锁");
                }
            } else {
                log.info("线程2获取分布式锁失败");
            }
        }).start();
    }
}
模块4：线程池（threadpool包）
4.1 知识点梳理（期末重点）
线程池核心作用：复用线程、控制线程数量、提高并发效率、管理线程生命周期（避免频繁创建/销毁线程的开销）
核心类：ThreadPoolExecutor（核心实现类）、Executors（工具类，快速创建线程池，不推荐生产使用）
ThreadPoolExecutor核心参数（常考）：核心线程数、最大线程数、空闲线程存活时间、任务队列、拒绝策略
线程池执行流程：核心线程→任务队列→非核心线程→拒绝策略
拒绝策略（4种）：AbortPolicy（抛异常）、CallerRunsPolicy（调用者执行）、DiscardPolicy（丢弃任务）、DiscardOldestPolicy（丢弃队列最老任务）
4.2 代码实现（ThreadPoolDemo.java）
package com.concurrent.review.threadpool;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;
/**
 * 线程池复习：ThreadPoolExecutor核心用法，覆盖核心参数、执行流程、拒绝策略
 * 重点：ThreadPoolExecutor的构造方法、核心参数含义、Executors工具类的弊端
 */
@Slf4j
public class ThreadPoolDemo {
    // 1. 自定义线程池（推荐，灵活控制参数）
    public static ThreadPoolExecutor createCustomThreadPool() {
        // 核心参数说明：
        // 1. 核心线程数（corePoolSize）：5，线程池长期保持的线程数量
        // 2. 最大线程数（maximumPoolSize）：10，线程池允许的最大线程数量
        // 3. 空闲线程存活时间（keepAliveTime）：60L，非核心线程空闲后的存活时间
        // 4. 时间单位（unit）：TimeUnit.SECONDS，存活时间的单位
        // 5. 任务队列（workQueue）：ArrayBlockingQueue(20)，有界队列，容量20
        // 6. 线程工厂（threadFactory）：默认线程工厂，可自定义线程名称
        // 7. 拒绝策略（handler）：AbortPolicy，任务满时抛异常
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                5,
                10,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        // 自定义线程工厂（可选，便于区分线程）
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("自定义线程-" + (++count));
                return thread;
            }
        };
        // 替换线程工厂
        return new ThreadPoolExecutor(
                5,
                10,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
    // 2. 测试线程池执行流程
    public static void testThreadPoolExecute() {
        ThreadPoolExecutor threadPool = createCustomThreadPool();
        // 提交30个任务（核心线程5 → 队列20 → 非核心线程5（10-5），刚好处理30个任务）
        for (int i = 0; i < 30; i++) {
            int taskId = i;
            threadPool.submit(() -> {
                log.info("线程{}执行任务{}，当前线程池线程数：{}，队列任务数：{}",
                        Thread.currentThread().getName(),
                        taskId,
                        threadPool.getPoolSize(),
                        threadPool.getQueue().size());
                try {
                    Thread.sleep(100); // 模拟任务执行耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        // 关闭线程池（平缓关闭：等待所有任务执行完成后关闭）
        threadPool.shutdown();
        try {
            // 等待线程池关闭，超时时间10秒
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow(); // 超时强制关闭
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        log.info("线程池已关闭");
    }
    // 3. 测试4种拒绝策略
    public static void testRejectedPolicy() {
        // 线程池参数：核心1，最大2，队列1 → 最多处理3个任务，第4个任务触发拒绝策略
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                1,
                2,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1)
        );
        // 测试1：AbortPolicy（默认）：抛异常
        log.info("=== 测试AbortPolicy ===");
        try {
            for (int i = 0; i < 4; i++) {
                int taskId = i;
                threadPool.execute(() -> log.info("执行任务{}", taskId));
            }
        } catch (RejectedExecutionException e) {
            log.error("任务被拒绝（AbortPolicy）", e);
        }
        threadPool.shutdownNow();
        // 测试2：CallerRunsPolicy：调用者（主线程）执行被拒绝的任务
        log.info("\n=== 测试CallerRunsPolicy ===");
        threadPool = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.CallerRunsPolicy());
        for (int i = 0; i < 4; i++) {
            int taskId = i;
            threadPool.execute(() -> log.info("执行任务{}，线程：{}", taskId, Thread.currentThread().getName()));
        }
        threadPool.shutdown();
        // 测试3：DiscardPolicy：静默丢弃被拒绝的任务，无异常
        log.info("\n=== 测试DiscardPolicy ===");
        threadPool = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardPolicy());
        for (int i = 0; i < 4; i++) {
            int taskId = i;
            threadPool.execute(() -> log.info("执行任务{}", taskId));
        }
        threadPool.shutdown();
        // 测试4：DiscardOldestPolicy：丢弃队列中最老的任务，执行当前任务
        log.info("\n=== 测试DiscardOldestPolicy ===");
        threadPool = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardOldestPolicy());
        for (int i = 0; i < 4; i++) {
            int taskId = i;
            threadPool.execute(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("执行任务{}", taskId);
            });
        }
        threadPool.shutdown();
    }
    // 4. 测试Executors工具类（不推荐生产使用，存在OOM风险）
    public static void testExecutors() {
        // 1. Executors.newFixedThreadPool(5)：固定核心线程数和最大线程数，无界队列
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
        // 2. Executors.newCachedThreadPool()：核心线程数0，最大线程数Integer.MAX_VALUE，无界队列（易OOM）
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        // 3. Executors.newSingleThreadExecutor()：单线程池，无界队列
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        // 提交任务
        fixedThreadPool.submit(() -> log.info("FixedThreadPool执行任务"));
        cachedThreadPool.submit(() -> log.info("CachedThreadPool执行任务"));
        singleThreadExecutor.submit(() -> log.info("SingleThreadExecutor执行任务"));
        // 关闭线程池
        fixedThreadPool.shutdown();
        cachedThreadPool.shutdown();
        singleThreadExecutor.shutdown();
    }
    public static void main(String[] args) {
        log.info("=== 测试线程池执行流程 ===");
        testThreadPoolExecute();
        log.info("\n=== 测试拒绝策略 ===");
        try {
            Thread.sleep(2000); // 等待上一个线程池关闭
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testRejectedPolicy();
        log.info("\n=== 测试Executors工具类 ===");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testExecutors();
    }
}
模块5：并发工具类（tool包）
5.1 知识点梳理（期末常考）
CountDownLatch：倒计时器，等待多个线程完成后，主线程再继续执行（不可重复使用）
CyclicBarrier：循环屏障，多个线程到达屏障后，同时继续执行（可重复使用）
Semaphore：信号量，控制同时访问某个资源的线程数量（限流）
区别：CountDownLatch是“等待其他线程完成”，CyclicBarrier是“线程互相等待，同时出发”，Semaphore是“限流”
5.2 代码实现（ConcurrentToolDemo.java）
package com.concurrent.review.tool;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
/**
 * 并发工具类复习：CountDownLatch、CyclicBarrier、Semaphore
 * 重点：三者的区别和使用场景，期末常考选择题和简答题
 */
@Slf4j
public class ConcurrentToolDemo {
    // 1. CountDownLatch：倒计时器（不可重复使用）
    public static void testCountDownLatch() throws InterruptedException {
        // 初始化倒计时器，计数为3（3个线程完成后，主线程继续）
        CountDownLatch countDownLatch = new CountDownLatch(3);
        // 启动3个线程
        for (int i = 0; i < 3; i++) {
            int threadId = i;
            new Thread(() -> {
                log.info("线程{}开始执行任务", threadId);
                try {
                    Thread.sleep(1000); // 模拟任务耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("线程{}完成任务", threadId);
                countDownLatch.countDown(); // 计数减1
            }).start();
        }
        log.info("主线程等待所有线程完成任务...");
        countDownLatch.await(); // 主线程阻塞，直到计数为0
        log.info("所有线程任务完成，主线程继续执行");
    }
    // 2. CyclicBarrier：循环屏障（可重复使用）
    public static void testCyclicBarrier() {
        // 初始化屏障，参数1：参与的线程数，参数2：所有线程到达屏障后执行的任务
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3, () -> {
            log.info("所有线程到达屏障，执行屏障任务（如汇总结果）");
        });
        // 启动3个线程，每个线程到达屏障后等待，直到所有线程到达
        for (int i = 0; i < 3; i++) {
            int threadId = i;
            new Thread(() -> {
                log.info("线程{}开始执行，前往屏障", threadId);
                try {
                    Thread.sleep((threadId + 1) * 5

