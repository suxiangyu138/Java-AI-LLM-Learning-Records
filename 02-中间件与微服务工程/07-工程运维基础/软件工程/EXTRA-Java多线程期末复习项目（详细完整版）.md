# Java 多线程期末复习项目（详细完整版）

> 通过"模拟多窗口售票系统"经典场景，完整覆盖多线程核心知识点，实现"边编码、边复习、边巩固"。代码注释详细，每一步对应多线程核心考点。

---

## 目录

- [一、项目概述](#一项目概述)
- [二、项目整体结构](#二项目整体结构)
- [三、分步实现项目](#三分步实现项目)
- [四、项目测试与复习巩固](#四项目测试与复习巩固)
- [五、核心知识点总结](#五核心知识点总结)

---

## 一、项目概述

### 1.1 项目目标

| 维度 | 说明 |
|------|------|
| **核心场景** | 模拟火车站多窗口售票系统 |
| **覆盖考点** | 线程创建、线程状态、同步机制、线程通信、线程池 |
| **学习方式** | 边编码边复习，每步代码对应具体考点 |

### 1.2 核心复习考点

| 考点 | 具体内容 |
|------|----------|
| 线程创建方式 | 继承 Thread 类、实现 Runnable 接口、实现 Callable 接口 + FutureTask |
| 线程状态及转换 | 新建 → 就绪 → 运行 → 阻塞 → 终止 |
| 线程同步机制 | synchronized 关键字、Lock 锁、volatile 关键字 |
| 线程通信 | wait()、notify()、notifyAll()、Condition |
| 线程池 | ThreadPoolExecutor、Executors 工具类 |
| 线程安全问题 | 可见性、原子性、有序性及解决方案 |

### 1.3 技术选型

| 项目 | 说明 |
|------|------|
| 核心技术 | Java SE 8+（多线程核心 API）、JUnit（可选） |
| 开发工具 | IDEA（推荐）/ Eclipse |
| 特点 | 纯 Java 原生实现，无需额外框架，贴合期末复习重点 |

---

## 二、项目整体结构

```
com.review.multithread
├── entity          // 实体类（封装票数信息，保证线程安全）
│   └── Ticket.java
├── thread          // 线程相关类（三种线程创建方式）
│   ├── ThreadTicket.java    // 继承 Thread 类
│   ├── RunnableTicket.java  // 实现 Runnable 接口
│   └── CallableTicket.java  // 实现 Callable 接口
├── pool            // 线程池相关类
│   └── TicketThreadPool.java
├── test            // 测试入口类
│   └── TicketTest.java
└── util            // 工具类
    └── ThreadUtil.java
```

---

## 三、分步实现项目

### 第一步：实体类 Ticket（核心考点：线程安全的共享资源）

> **考点：** 共享资源的线程安全问题、synchronized 关键字、volatile 关键字、原子性保证。

```java
package com.review.multithread.entity;

public class Ticket {
    private volatile int totalTicket;
    private volatile boolean isSoldOut = false;

    public Ticket(int totalTicket) {
        this.totalTicket = totalTicket;
    }

    public synchronized boolean sellTicket(String windowName) {
        if (isSoldOut) {
            System.out.println(windowName + "：票已售罄，无法售票！");
            return false;
        }
        try {
            Thread.sleep((long) (Math.random() * 200 + 100));
        } catch (InterruptedException e) {
            System.out.println(windowName + "：售票被中断，操作失败！");
            Thread.currentThread().interrupt();
            return false;
        }
        totalTicket--;
        if (totalTicket <= 0) {
            isSoldOut = true;
            System.out.println(windowName + "：售出最后一张票！票已全部售罄！");
        } else {
            System.out.println(windowName + "：售出1张票，剩余票数：" + totalTicket);
        }
        return true;
    }

    public int getTotalTicket() { return totalTicket; }
    public boolean isSoldOut() { return isSoldOut; }
}
```

| 知识点 | 说明 |
|--------|------|
| `volatile` | 解决可见性问题，一个线程修改后其他线程立即可见 |
| `synchronized` | 锁对象是当前 Ticket 实例，确保同一时刻只有一个线程执行 sellTicket |
| `Thread.sleep()` | 使线程进入阻塞状态，模拟售票延迟 |
| 线程中断 | 捕获 InterruptedException，需调用 `Thread.currentThread().interrupt()` 恢复中断状态 |

---

### 第二步：三种线程创建方式

#### 2.1 方式一：继承 Thread 类

```java
package com.review.multithread.thread;

import com.review.multithread.entity.Ticket;

public class ThreadTicket extends Thread {
    private Ticket ticket;
    private String windowName;

    public ThreadTicket(Ticket ticket, String windowName) {
        this.ticket = ticket;
        this.windowName = windowName;
        this.setPriority(Thread.NORM_PRIORITY);
    }

    @Override
    public void run() {
        while (!ticket.isSoldOut()) {
            ticket.sellTicket(windowName);
            Thread.yield();
        }
    }
}
```

#### 2.2 方式二：实现 Runnable 接口

```java
package com.review.multithread.thread;

import com.review.multithread.entity.Ticket;

public class RunnableTicket implements Runnable {
    private Ticket ticket;
    private String windowName;

    public RunnableTicket(Ticket ticket, String windowName) {
        this.ticket = ticket;
        this.windowName = windowName;
    }

    @Override
    public void run() {
        while (!ticket.isSoldOut()) {
            ticket.sellTicket(windowName);
            Thread.yield();
        }
    }
}
```

#### 2.3 方式三：实现 Callable 接口

```java
package com.review.multithread.thread;

import com.review.multithread.entity.Ticket;
import java.util.concurrent.Callable;

public class CallableTicket implements Callable<Integer> {
    private Ticket ticket;
    private String windowName;
    private int sellCount = 0;

    public CallableTicket(Ticket ticket, String windowName) {
        this.ticket = ticket;
        this.windowName = windowName;
    }

    @Override
    public Integer call() throws Exception {
        while (!ticket.isSoldOut()) {
            if (ticket.sellTicket(windowName)) {
                sellCount++;
            }
            Thread.yield();
        }
        System.out.println(windowName + "：售票结束，共售出" + sellCount + "张票");
        return sellCount;
    }
}
```

---

### 第三步：线程池实现

```java
package com.review.multithread.pool;

import com.review.multithread.thread.RunnableTicket;
import com.review.multithread.entity.Ticket;
import java.util.concurrent.*;

public class TicketThreadPool {
    private ThreadPoolExecutor threadPool;

    public void initThreadPool(int corePoolSize, int maximumPoolSize,
                               long keepAliveTime, TimeUnit unit, int queueCapacity) {
        threadPool = new ThreadPoolExecutor(
                corePoolSize, maximumPoolSize,
                keepAliveTime, unit,
                new LinkedBlockingQueue<Runnable>(queueCapacity),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public void submitTicketTask(Ticket ticket, String[] windowNames) {
        for (String windowName : windowNames) {
            RunnableTicket task = new RunnableTicket(ticket, windowName);
            threadPool.submit(task);
        }
    }

    public void shutdownThreadPool() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            System.out.println("线程池已关闭，等待所有任务执行完毕...");
        }
    }
}
```

| 参数 | 说明 |
|------|------|
| corePoolSize | 核心线程数（常驻，空闲不销毁） |
| maximumPoolSize | 最大线程数 |
| keepAliveTime | 空闲线程存活时间 |
| workQueue | 任务队列（建议有界队列） |
| handler | 拒绝策略 |

---

### 第四步：线程通信

```java
// Ticket 类中新增线程通信逻辑
public synchronized boolean sellTicket(String windowName) {
    // 线程通信：票售罄时等待
    while (isSoldOut) {
        try {
            System.out.println(windowName + "：票已售罄，进入等待状态...");
            this.wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    // ... 售票逻辑 ...
    if (totalTicket <= 0) {
        isSoldOut = true;
        this.notifyAll(); // 唤醒所有等待线程
    }
    return true;
}
```

| 方法 | 说明 |
|------|------|
| `wait()` | 释放锁，进入等待状态，必须在同步代码块中调用 |
| `notifyAll()` | 唤醒所有等待该锁的线程 |
| `notify()` | 只唤醒一个等待线程（随机，建议优先用 notifyAll） |

---

### 第五步：测试类

```java
package com.review.multithread.test;

import com.review.multithread.entity.Ticket;
import com.review.multithread.thread.*;
import com.review.multithread.pool.TicketThreadPool;
import java.util.concurrent.*;

public class TicketTest {
    private static final int TOTAL_TICKET = 100;
    private static final String[] WINDOW_NAMES = {"窗口1", "窗口2", "窗口3"};

    public static void main(String[] args) throws Exception {
        // testThreadWay();     // 场景1：继承 Thread
        // testRunnableWay();  // 场景2：实现 Runnable
        // testCallableWay();  // 场景3：实现 Callable（获取返回值）
        testThreadPoolWay();   // 场景4：线程池（推荐）
    }

    // 场景4：线程池测试
    public static void testThreadPoolWay() throws Exception {
        System.out.println("=== 线程池 测试多窗口售票 ===");
        Ticket ticket = new Ticket(TOTAL_TICKET);
        TicketThreadPool threadPool = new TicketThreadPool();
        threadPool.initThreadPool(3, 5, 30, TimeUnit.SECONDS, 10);
        threadPool.submitTicketTask(ticket, WINDOW_NAMES);
        while (!ticket.isSoldOut()) {
            Thread.sleep(1000);
        }
        threadPool.shutdownThreadPool();
        System.out.println("=== 所有窗口售票结束 ===");
    }
}
```

---

## 四、项目测试与复习巩固

### 4.1 常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 超卖/重卖 | 未使用同步机制 | 使用 synchronized 或 Lock 保证原子性 |
| 线程启动无反应 | 直接调用 run() 而非 start() | 调用 `thread.start()` |
| wait() 抛异常 | wait() 未在同步代码块中调用 | 将 wait() 放在 synchronized 修饰的代码中 |
| 线程池程序立即结束 | 主线程未等待 | 使用 awaitTermination() 或循环检查 |
| Callable 无法获取返回值 | 未使用 FutureTask | 使用 FutureTask 包装，调用 get() 方法 |

### 4.2 扩展练习

- 使用 ReentrantLock 替代 synchronized
- 使用 Condition 接口替代 wait()/notifyAll()
- 新增"退票"功能
- 使用 CountDownLatch 等待所有窗口结束后打印统计
- 模拟线程死锁场景并解决

---

## 五、核心知识点总结

### 5.1 线程创建方式对比

| 创建方式 | 核心方法 | 返回值 | 可抛异常 | 特点 |
|----------|----------|--------|----------|------|
| 继承 Thread | 重写 run() | 无 | 无 | 简单直接，单继承限制 |
| 实现 Runnable | 重写 run() | 无 | 无 | 无单继承限制，灵活 |
| 实现 Callable | 重写 call() | 有 | 有 | 有返回值，需配合 FutureTask |

### 5.2 线程同步机制对比

| 同步方式 | 特点 | 优点 | 缺点 |
|----------|------|------|------|
| synchronized | JVM 层面，自动释放锁 | 简单易用，安全性高 | 灵活性差，不可中断 |
| Lock（ReentrantLock） | API 层面，手动释放 | 灵活，可中断/超时/公平锁 | 需手动释放，易遗漏 |
| volatile | 保证可见性、有序性 | 轻量级，无锁开销 | 不保证原子性 |

### 5.3 线程状态转换（期末必考）

```
New → Runnable → Running → Terminated
                ↕
              Blocked
```

| 状态转换 | 触发条件 |
|----------|----------|
| New → Runnable | 调用 start() |
| Runnable → Running | CPU 调度 |
| Running → Blocked | sleep()、wait()、等待锁 |
| Blocked → Runnable | sleep 时间到、被 notify 唤醒、获取锁 |
| Running → Terminated | run()/call() 执行完毕或抛出未捕获异常 |

---

> **复习建议：** 先理解项目整体结构，逐行阅读代码，运行每个测试场景观察输出结果，完成扩展练习深化理解。
