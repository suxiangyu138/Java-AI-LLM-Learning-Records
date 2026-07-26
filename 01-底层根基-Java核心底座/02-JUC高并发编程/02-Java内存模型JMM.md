# 02 — Java内存模型（JMM）
> 定位：深入理解JMM的主内存与工作内存模型、并发编程三大特性（原子性/可见性/有序性）、happens-before原则与volatile底层原理

## 目录

1. [JMM概述与内存架构](#1-jmm概述与内存架构)
2. [主内存与工作内存交互](#2-主内存与工作内存交互)
3. [并发三大特性：原子性、可见性、有序性](#3-并发三大特性原子性可见性有序性)
4. [指令重排序与内存屏障](#4-指令重排序与内存屏障)
5. [happens-before原则详解](#5-happens-before原则详解)
6. [volatile关键字底层原理](#6-volatile关键字底层原理)
7. [volatile使用场景与限制](#7-volatile使用场景与限制)
8. [volatile与synchronized对比](#8-volatile与synchronized对比)
9. [双重检查锁定(DCL)与volatile](#9-双重检查锁定dcl与volatile)
10. [面试高频考点](#10-面试高频考点)

---

## 1. JMM概述与内存架构

### 1.1 什么是JMM

Java内存模型（Java Memory Model，JMM）是Java虚拟机定义的一组规范，用来屏蔽底层硬件和操作系统的内存访问差异，保证Java程序在不同平台下并发访问共享内存时的一致性。JMM规定了程序中变量的访问规则，定义了线程如何与主内存交互，是并发编程的底层理论基础。

> **核心目标**：规范线程对共享变量的读写行为，解决并发编程中的原子性、可见性、有序性问题。

### 1.2 JMM内存架构

```
┌─────────────────────────────────────────────────────────────┐
│                        主内存 (Main Memory)                   │
│                    ┌─────────────────────┐                  │
│                    │  共享变量 (堆)       │                  │
│                    │  - 实例对象          │                  │
│                    │  - 静态变量          │                  │
│                    │  - 数组元素          │                  │
│                    └─────────────────────┘                  │
└──────────────────────────┬──────────────────────────────────┘
                           │  read / load / store / write
         ┌─────────────────┼─────────────────┐
         │                 │                  │
         ▼                 ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  线程A工作内存   │ │  线程B工作内存   │ │  线程C工作内存   │
│ ┌─────────────┐ │ │ ┌─────────────┐ │ │ ┌─────────────┐ │
│ │ 变量副本     │ │ │ │ 变量副本     │ │ │ │ 变量副本     │ │
│ │ (CPU缓存)   │ │ │ │ (CPU缓存)   │ │ │ │ (CPU缓存)   │ │
│ └─────────────┘ │ │ └─────────────┘ │ │ └─────────────┘ │
└─────────────────┘ └─────────────────┘ └─────────────────┘
      ▲                                      ▲
      │                                      │
      ▼                                      ▼
┌─────────────┐                      ┌─────────────┐
│  CPU寄存器  │                      │  CPU寄存器  │
└─────────────┘                      └─────────────┘
```

### 1.3 内存区域划分

| 内存区域 | 存储内容 | 线程共享 | 对应物理区域 |
|----------|---------|---------|-------------|
| **主内存** | 所有共享变量（实例字段、静态字段、数组元素） | 是（所有线程共享） | 堆内存 |
| **工作内存** | 线程使用的变量副本（共享变量的拷贝） | 否（每个线程独有） | CPU寄存器 + 缓存 |
| **栈内存** | 局部变量、方法参数、返回地址 | 否（每个线程独有） | 物理内存栈区 |

> **关键结论**：线程无法直接操作主内存，只能操作工作内存中的副本。工作内存与主内存之间的同步规则由JMM定义，这种设计导致了可见性问题。

### 1.4 JMM与Java内存区域的区别

| 概念 | JMM | JVM运行时数据区 |
|------|-----|----------------|
| 定位 | 并发编程规范 | 运行时内存布局 |
| 主内存 | 共享变量存放区域（逻辑概念） | 堆（Heap） |
| 工作内存 | 线程私有变量副本区域（逻辑概念） | 虚拟机栈 + CPU寄存器 |
| 关注点 | 线程间变量访问的规则 | 内存分配与回收 |

---

## 2. 主内存与工作内存交互

### 2.1 八种原子操作

JMM定义了8种原子操作，用于规范主内存与工作内存之间的交互协议：

```
                    主内存
             ┌──────────────────┐
             │  共享变量         │
             └──────┬───────────┘
               read ↑ ↓ write
             ┌──────┴───────────┐
             │ 传输层 (总线)     │
             └──────┬───────────┘
               load ↑ ↓ store
             ┌──────┴───────────┐
             │  工作内存变量副本  │
             └──────┬───────────┘
               use  ↑ ↓ assign
             ┌──────┴───────────┐
             │  线程执行引擎     │
             └──────────────────┘
```

| 操作 | 作用 | 操作对象 |
|------|------|---------|
| **lock**（锁定） | 将主内存变量标识为线程独占状态 | 主内存变量 |
| **unlock**（解锁） | 释放主内存变量的锁定状态 | 主内存变量 |
| **read**（读取） | 从主内存读取变量到传输层 | 主内存变量 |
| **load**（载入） | 将读取的变量值放入工作内存副本 | 工作内存变量 |
| **use**（使用） | 将工作内存变量值传递给执行引擎 | 工作内存变量 |
| **assign**（赋值） | 将执行引擎的结果赋值给工作内存变量 | 工作内存变量 |
| **store**（存储） | 将工作内存变量值传回传输层 | 工作内存变量 |
| **write**（写入） | 将store的值写入主内存变量 | 主内存变量 |

### 2.2 交互规则

```java
/**
 * 线程操作共享变量的完整流程：
 * 1. read → load：从主内存拷贝到工作内存
 * 2. use：工作内存中读取副本值（执行引擎）
 * 3. assign：执行引擎计算结果写回工作内存
 * 4. store → write：工作内存刷新回主内存
 */
public class JMMInteractionDemo {
    private static int counter = 0;  // 主内存中的共享变量

    public static void main(String[] args) {
        // 线程A：read→load→use→assign→store→write
        // 线程B：read→load→use→assign→store→write
        // 两个线程的工作内存中各自持有一个counter副本
        counter++;  // 不是原子操作！
    }
}
```

**JMM对8种操作制定的规则**：

- 不允许read和load、store和write操作之一单独出现（必须成对）
- 不允许线程丢弃最近的assign操作（工作内存变化后必须同步回主内存）
- 不允许线程无原因地将数据从工作内存同步回主内存
- 新变量只能在主内存中诞生，不允许在工作内存中直接使用未初始化的变量
- 一个变量同一时刻只允许一个线程对其执行lock操作
- 对变量执行lock操作后，会清空工作内存中该变量的值，使用前需重新read/load
- unlock操作前必须先将变量同步回主内存（store/write）

> **核心理解**：工作内存与主内存之间的同步延迟，是多线程可见性问题的根本原因。

---

## 3. 并发三大特性：原子性、可见性、有序性

线程安全问题的根源在于这三条性质在并发环境下被破坏。理解每个特性的底层原因、破坏场景和解决方案，是掌握并发编程的基础。

### 3.1 原子性（Atomicity）

**定义**：一个或多个操作要么全部执行成功且不可中断，要么全部不执行，执行过程中不会被线程调度打断。

**破坏场景**：

```java
/**
 * 原子性破坏演示：i++ 不是原子操作
 *
 * i++ 在字节码层面分为三步：
 * 1. getfield   - 读取变量值
 * 2. iadd       - 执行加1
 * 3. putfield   - 写回变量
 *
 * 线程A执行完步骤1后被切换，线程B执行完三步，
 * 线程A恢复后基于旧值执行步骤2、3 → 数据丢失
 */
public class AtomicityDemo {
    private static int count = 0;
    private static final int THREAD_COUNT = 1000;

    public static void main(String[] args) throws Exception {
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> count++);
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        System.out.println("期望值: " + THREAD_COUNT);
        System.out.println("实际值: " + count);  // 通常 < 1000
    }
}
```

**常见非原子操作**：

| 操作 | 字节码步骤 | 是否原子 |
|------|-----------|---------|
| `int i = 1;` | 常量赋值 | 是（对32位基本类型） |
| `long l = 1L;` | 两次16位写入（32位JVM） | 否（64位类型） |
| `i++` | read → modify → write | 否 |
| `obj.ref = newObj` | 对象引用赋值 | 是（引用类型32/64位） |
| `list.add(obj)` | 多步操作 | 否 |

**解决方案**：

```java
public class AtomicitySolution {
    // 方案1：synchronized（重量级，保证原子性）
    private int count1 = 0;
    public synchronized void increment1() { count1++; }

    // 方案2：Lock（显式锁）
    private final Lock lock = new ReentrantLock();
    private int count2 = 0;
    public void increment2() {
        lock.lock();
        try { count2++; } finally { lock.unlock(); }
    }

    // 方案3：Atomic类（CAS无锁，高性能推荐）
    private final AtomicInteger count3 = new AtomicInteger(0);
    public void increment3() { count3.incrementAndGet(); }
}
```

### 3.2 可见性（Visibility）

**定义**：一个线程修改了共享变量的值，其他线程能立即感知到该修改，获取最新值。

**破坏场景**：

```java
/**
 * 可见性破坏演示
 *
 * 线程A修改了flag，但修改可能只停留在工作内存（CPU缓存）
 * 线程B读取的仍是自己工作内存中的旧值，导致死循环
 */
public class VisibilityDemo {
    private static boolean flag = true;  // 未加volatile

    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            System.out.println("线程A: 开始循环...");
            while (flag) {
                // 死循环！线程B修改了flag，但线程A看不到
            }
            System.out.println("线程A: 退出循环");
        }).start();

        Thread.sleep(1000);

        new Thread(() -> {
            flag = false;  // 修改仅刷新到工作内存，未及时写回主内存
            System.out.println("线程B: flag已设为false");
        }).start();
    }
}
```

**可见性问题的根源**：现代CPU为提升性能，采用了多级缓存架构。线程对变量的操作优先在缓存中进行，缓存与主内存的同步存在延迟。

```
CPU核心0          CPU核心1
┌────────┐       ┌────────┐
│ 寄存器  │       │ 寄存器  │
├────────┤       ├────────┤
│ L1缓存 │       │ L1缓存 │  ← 各自缓存独立
├────────┤       ├────────┤
│ L2缓存 │       │ L2缓存 │
├────────┴───────┴────────┤
│        L3缓存 (共享)     │
├─────────────────────────┤
│        主内存            │
└─────────────────────────┘
```

**解决方案**：

| 机制 | 原理 | 适用场景 |
|------|------|---------|
| `volatile` | 写入后强制刷新到主内存，读取时从主内存加载 | 状态标记、开关量 |
| `synchronized` | 加锁时清空工作内存，解锁时刷新到主内存 | 复合操作、临界区 |
| `Lock` | 与synchronized类似，基于AQS保证可见性 | 复杂同步场景 |
| `final` | 构造器中初始化完成后，其他线程可见 | 不可变对象 |

### 3.3 有序性（Ordering）

**定义**：程序代码的执行顺序与实际执行顺序一致，不会被打乱。

**破坏场景**：JVM和CPU出于性能优化，会在不影响单线程执行结果的前提下改变指令执行顺序（指令重排序），但多线程环境下重排序会破坏程序的逻辑。

```java
/**
 * 有序性破坏演示
 *
 * 线程A执行write()，线程B执行read()
 * 在没有同步的情况下，可能出现 flag == true 但 a == 0
 * 原因：指令重排序导致 flag = true 先于 a = 42 执行
 */
public class OrderingDemo {
    private static int a = 0;
    private static boolean flag = false;

    public static void writer() {
        a = 42;          // 操作1
        flag = true;     // 操作2（可能被重排序到操作1之前）
    }

    public static void reader() {
        if (flag) {          // 可能看到 flag = true
            System.out.println(a);  // 但 a 可能还是 0！（重排序导致）
        }
    }
}
```

**解决方案**：

```java
public class OrderingSolution {
    private static int a = 0;
    private static volatile boolean flag = false;  // volatile禁止重排序

    public static void writer() {
        a = 42;          // 普通写
        flag = true;     // volatile写 → 插入StoreStore屏障，禁止与前面的写重排序
    }

    public static void reader() {
        if (flag) {          // volatile读 → 插入LoadLoad屏障
            System.out.println(a);  // 保证看到 a = 42
        }
    }
}
```

### 3.4 三大特性小结

| 特性 | 问题根源 | 解决手段 |
|------|---------|---------|
| 原子性 | 线程切换导致操作中断 | synchronized、Lock、Atomic类 |
| 可见性 | 工作内存与主内存同步延迟 | volatile、synchronized、Lock、final |
| 有序性 | 编译器和CPU指令重排序 | volatile、synchronized、Lock |

> **关键认知**：synchronized和Lock同时保证了原子性、可见性、有序性（通过内存屏障和锁语义）；volatile只保证了可见性和有序性，不保证原子性。

---

## 4. 指令重排序与内存屏障

### 4.1 指令重排序分类

```bash
指令重排序的三种层次：

┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│  编译器重排序  │ ──▶ │  处理器重排序  │ ──▶ │  内存系统重排序 │
│ (JIT编译器)    │     │ (CPU乱序执行)  │     │ (缓存/写缓冲)  │
└───────────────┘     └───────────────┘     └───────────────┘
      ↓                        ↓                       ↓
  不改变单线程        不改变数据          不同CPU核心看到的
  语义（as-if-serial）  依赖性              内存操作顺序可能不同
```

```java
/**
 * 指令重排序演示
 *
 * 以下代码在单线程下结果始终正确（as-if-serial语义）
 * 但在多线程下，由于指令重排序，执行顺序可能被改变
 */
public class ReorderingDemo {
    private int x = 0, y = 0;
    private int a = 0, b = 0;

    public void thread1() {
        a = 1;  // 操作1
        x = b;  // 操作2（可能被重排序到操作1之前）
    }

    public void thread2() {
        b = 1;  // 操作3
        y = a;  // 操作4（可能被重排序到操作3之前）
    }
}
```

### 4.2 as-if-serial语义

**定义**：无论编译器和处理器如何重排序，单线程程序的执行结果不能被改变。

**核心原则**：编译器、runtime和处理器必须遵守as-if-serial语义，即重排序不会改变单线程程序的执行结果。

```java
/**
 * as-if-serial 示例
 * 操作1与操作2存在数据依赖（操作2依赖操作1的结果）
 * 操作3与操作1/2无数据依赖 → 可被重排序
 */
public class AsIfSerialDemo {
    public void example() {
        int a = 1;   // 操作1
        int b = 2;   // 操作2（与操作1无依赖，可重排序）
        int c = a + b; // 操作3（依赖操作1和2的结果，不可重排序）
    }
}
```

### 4.3 内存屏障（Memory Barrier）

内存屏障是一条CPU指令，用于禁止特定类型的重排序，强制刷新缓存或使缓存失效。JMM通过插入不同类型的内存屏障来实现volatile的语义。

```bash
内存屏障分类：

1. LoadLoad屏障
   读操作1 → [LoadLoad] → 读操作2
   确保读操作1先于读操作2完成

2. StoreStore屏障
   写操作1 → [StoreStore] → 写操作2
   确保写操作1先于写操作2对其他处理器可见

3. LoadStore屏障
   读操作1 → [LoadStore] → 写操作2
   确保读操作1先于写操作2

4. StoreLoad屏障
   写操作1 → [StoreLoad] → 读操作2
   确保写操作1对其他处理器可见后，再进行读操作2
   （最昂贵的屏障，几乎全屏障）
```

### 4.4 JMM内存屏障插入策略

```bash
# volatile 写的内存屏障插入
┌──────────────────────────────────────────┐
│  普通读写 (所有前面的普通变量操作)         │
├──────────────────────────────────────────┤
│  StoreStore 屏障 ← 禁止前面的普通读写重排到此屏障之后   │
├──────────────────────────────────────────┤
│  volatile 写操作                          │
├──────────────────────────────────────────┤
│  StoreLoad 屏障 ← 禁止此volatile写重排到后面的volatile读  │
├──────────────────────────────────────────┤
│  后续的 volatile 读或普通读写              │
└──────────────────────────────────────────┘

# volatile 读的内存屏障插入
┌──────────────────────────────────────────┐
│  前面的普通读写                           │
├──────────────────────────────────────────┤
│  volatile 读操作                          │
├──────────────────────────────────────────┤
│  LoadLoad 屏障 ← 禁止后面的普通读重排到此屏障之前      │
├──────────────────────────────────────────┤
│  LoadStore 屏障 ← 禁止后面的普通写重排到此屏障之前     │
├──────────────────────────────────────────┤
│  后续的普通读写                           │
└──────────────────────────────────────────┘
```

### 4.5 JMM对重排序的规则

| 第一个操作 | 第二个操作 | 普通读 | 普通写 | volatile读 | volatile写 |
|-----------|-----------|--------|--------|-----------|-----------|
| **普通读** | - | 可以重排 | 可以重排 | 可以重排 | 不可重排 |
| **普通写** | - | 可以重排 | 可以重排 | 可以重排 | 不可重排 |
| **volatile读** | - | 不可重排 | 不可重排 | 不可重排 | 不可重排 |
| **volatile写** | - | 可以重排 | 可以重排 | 不可重排 | 不可重排 |

---

## 5. happens-before原则详解

### 5.1 定义与作用

happens-before是JMM最核心的概念，用于判断两个操作之间是否存在数据竞争，以及一个操作对另一个操作是否可见。如果两个操作存在happens-before关系，那么第一个操作的结果对第二个操作可见，且第一个操作的执行顺序在第二个操作之前。

> **本质**：happens-before是JMM对程序员做出的"可见性保证"，遵循这些规则编写的并发代码无需担心重排序问题。

### 5.2 八大规则

#### 规则1：程序次序规则

**在一个线程内**，按照控制流顺序，前面的操作happens-before后面的操作。

```java
public class ProgramOrderRule {
    public void method() {
        int a = 1;    // 1
        int b = 2;    // 2 (happens-before关系不保证实际执行顺序，但保证结果一致)
        int c = a + b; // 3
    }
}
```

> 注意：这是as-if-serial语义的体现。虽然有重排序的可能，但不会影响单线程内的执行结果。

#### 规则2：volatile变量规则

对一个volatile变量的写操作，happens-before于后续对这个volatile变量的读操作。

```java
public class VolatileRule {
    private volatile boolean flag = false;
    private int data = 0;

    public void writer() {
        data = 42;      // 1. 普通写
        flag = true;    // 2. volatile写（happens-before于3）
    }

    public void reader() {
        if (flag) {          // 3. volatile读（看到2的结果）
            int result = data;  // 4. 保证看到1的结果（1 happens-before 2，2 happens-before 3 → 1 happens-before 4）
        }
    }
}
```

#### 规则3：锁规则

对一个锁的解锁（unlock）happens-before于后续对这个锁的加锁（lock）。

```java
public class LockRule {
    private int data = 0;
    private final Object lock = new Object();

    public void writer() {
        synchronized (lock) {  // 加锁
            data = 42;         // 临界区内写
        }                      // 解锁（happens-before于后续加锁）
    }

    public void reader() {
        synchronized (lock) {  // 加锁（看到解锁前的所有操作）
            int result = data; // 保证看到42
        }
    }
}
```

#### 规则4：传递性

如果A happens-before B，B happens-before C，则A happens-before C。

```
   data = 42          (普通写)
      ↓
   flag = true        (volatile写)
      ↓
   if (flag)          (volatile读)
      ↓
   int r = data       (保证看到 data = 42)

传递链：data写 → volatile写 → volatile读 → data读
```

#### 规则5：线程启动规则

线程对象上的 `start()` 方法 happens-before 该线程中的任何操作。

```java
public class StartRule {
    private int data = 0;

    public void test() throws Exception {
        data = 42;  // 1. happens-before于2

        Thread t = new Thread(() -> {
            int r = data;  // 2. 保证看到42
        });

        t.start();  // 启动线程
    }
}
```

#### 规则6：线程终止规则

线程中的所有操作 happens-before 于其他线程检测到该线程终止（从 `join()` 返回）。

```java
public class JoinRule {
    private int data = 0;

    public void test() throws Exception {
        Thread t = new Thread(() -> {
            data = 42;  // 1. happens-before于2
        });

        t.start();
        t.join();       // 2. 等待线程终止
        int r = data;   // 保证看到42
    }
}
```

#### 规则7：中断规则

对线程的 `interrupt()` 调用 happens-before 于被中断线程检测到中断事件（`InterruptedException` 抛出或 `isInterrupted()` 返回true）。

```java
public class InterruptRule {
    public void test() throws Exception {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                // 循环
            }
            // 检测到中断时，保证看到interrupt()之前的所有操作
        });

        t.start();
        // 某些操作
        t.interrupt();  // happens-before于线程检测到中断
    }
}
```

#### 规则8：finalizer规则

一个对象的构造函数执行完毕（finalize开始前）happens-before于其finalize()方法的开始。

### 5.3 happens-before关系图

```
┌─────────────────────────────────────────────────────────────┐
│                    happens-before 规则网络                    │
│                                                             │
│  程序次序规则 ────── 传递性 ────── volatile变量规则          │
│       │                          │                          │
│       │                          │                          │
│       ▼                          ▼                          │
│  锁规则 ──────────────────→ 线程启动规则                     │
│       │                          │                          │
│       │                          │                          │
│       ▼                          ▼                          │
│  线程终止规则 ←─────────── 中断规则                          │
│                                                             │
│  finalizer规则（独立）                                      │
└─────────────────────────────────────────────────────────────┘
```

> **应用技巧**：实际编码中最常使用的是 volatile规则 + 锁规则 + 传递性。理解这三者的组合，就能解决90%的并发可见性问题。

---

## 6. volatile关键字底层原理

### 6.1 volatile的语义

volatile是Java提供的最轻量级的同步机制，具有三个核心语义：

1. **可见性**：对一个volatile变量的写操作，会立即刷新到主内存，其他线程读取时从主内存加载
2. **有序性**：禁止对volatile变量相关的代码进行指令重排序
3. **不保证原子性**：对volatile变量的复合操作（如i++）仍然不是线程安全的

### 6.2 内存屏障实现机制

```java
/**
 * volatile底层通过内存屏障实现
 *
 * volatile写：插入 StoreStore + StoreLoad 屏障
 *   - StoreStore：禁止前面的普通写与当前volatile写重排序
 *   - StoreLoad：禁止当前volatile写与后面的volatile读/写重排序
 *
 * volatile读：插入 LoadLoad + LoadStore 屏障
 *   - LoadLoad：禁止后面的普通读与当前volatile读重排序
 *   - LoadStore：禁止后面的普通写与当前volatile读重排序
 */
public class VolatileBarrierDemo {
    private volatile int value;

    public void setValue(int v) {
        // 普通写 → [StoreStore] → volatile写 → [StoreLoad]
        value = v;
    }

    public int getValue() {
        // volatile读 → [LoadLoad] → [LoadStore] → 普通读
        return value;
    }
}
```

### 6.3 内存屏障完整架构图

```
                          线程A                                   线程B
              ┌──────────────────────────┐        ┌──────────────────────────┐
              │  data = 42 (普通写)       │        │  if (flag) {             │
              │  [StoreStore]             │        │    [LoadLoad]            │
              │  flag = true (volatile写) │        │    [LoadStore]           │
              │  [StoreLoad]             │        │    int r = data;         │
              └────────────┬─────────────┘        └────────────┬─────────────┘
                           │                                    │
                           │  StoreStore保证                      │  LoadLoad保证
                           │  data写不重排到flag写之后             │  data读不重排到flag读之前
                           │                                    │
                           ▼                                    ▼
              ┌─────────────────────────────────────────────────────┐
              │                   总线/主内存                        │
              │  flag = true (通过StoreLoad保证对其他线程可见)       │
              │  data = 42 (通过StoreStore保证已在flag写之前写入)    │
              └─────────────────────────────────────────────────────┘
```

### 6.4 volatile与缓存一致性协议

volatile的可见性底层依赖于CPU的缓存一致性协议（如MESI协议）和总线嗅探机制：

```
volatile 写操作流程：
1. JIT编译器检测到volatile写 → 插入StoreStore屏障
2. CPU执行volatile写 → 触发缓存一致性协议
3. 当前核心发出"缓存行失效"消息到总线（总线嗅探）
4. 其他核心监听到消息 → 将自己的缓存行标记为无效
5. 当前核心将数据写回主内存

volatile 读操作流程：
1. JIT编译器检测到volatile读 → 插入LoadLoad屏障
2. CPU执行volatile读 → 检查缓存行是否有效
3. 无效 → 从主内存重新加载到缓存（缓存未命中）
4. 有效 → 直接从缓存读取（但缓存中的数据已是最新）
```

> **总线嗅探（Bus Snooping）**：每个CPU核心持续监听总线上的数据请求，当其他核心请求修改共享变量时，将自身缓存的对应行标记为失效，下次读取时触发缓存未命中，强制从主内存加载最新值。

### 6.5 volatile底层指令实现

```bash
# x86架构下volatile变量写操作的汇编级别
[StoreStore]  # 实际上x86不会重排序写操作，StoreStore是no-op
[volatile写]  # lock addl $0, 0(%rsp) — 对栈顶加锁
[StoreLoad]   # 实际上是mfence指令 — 全屏障

# x86使用lock前缀指令实现volatile语义：
# lock指令的作用：
# 1. 锁定总线/缓存行（禁止其他CPU访问）
# 2. 强制将写缓冲区的数据刷新到主内存
# 3. 使其他CPU的缓存行失效
```

---

## 7. volatile使用场景与限制

### 7.1 正确使用场景

#### 场景1：状态标记（开关量）

```java
/**
 * volatile最经典的应用：线程停止标记
 * 纯赋值操作，不依赖当前值 → 线程安全
 */
public class ShutdownDemo {
    private volatile boolean running = true;

    public void run() {
        while (running) {
            // 执行业务逻辑
        }
        System.out.println("线程安全退出");
    }

    public void stop() {
        running = false;  // volatile写，对其他线程立即可见
    }
}
```

#### 场景2：一次性安全发布（one-shot safe publication）

```java
/**
 * volatile保证对象的安全发布
 * 仅用于"发布后不再修改"的场景
 */
public class SafePublicationDemo {
    private volatile Map<String, String> config;

    public void loadConfig() {
        Map<String, String> newConfig = new HashMap<>();
        newConfig.put("key1", "value1");
        newConfig.put("key2", "value2");
        // volatile写：newConfig内部的所有写入在发布前对其他线程可见
        config = newConfig;
    }

    public String getConfig(String key) {
        // volatile读：保证看到最新的config引用
        return config.get(key);
    }
}
```

#### 场景3：独立观察（独立变量之间的读写不互相依赖）

```java
/**
 * volatile适用于独立变量的读写操作
 * 不适用于"先读取再修改"的复合操作
 */
public class IndependentObservationDemo {
    private volatile double temperature;
    private volatile double humidity;

    // 正确：两个volatile变量相互独立
    public void update(double t, double h) {
        temperature = t;  // 纯赋值
        humidity = h;     // 纯赋值
    }

    // 错误：依赖当前值进行修改
    // public void increment() { counter++; }  // 不是原子操作！
}
```

#### 场景4：双重检查锁定（DCL）的单例模式

详见第9节。

### 7.2 错误使用场景

```java
/**
 * volatile的典型误用：试图保证原子性
 *
 * volatile int count;
 * count++ 不是原子操作！
 */
public class VolatileMisuseDemo {
    private volatile int count = 0;

    // 错误用法：count++是read-modify-write，多线程下数据丢失
    public void increment() {
        count++;  // 非原子操作！
    }

    // 正确用法：使用AtomicInteger
    private final AtomicInteger atomicCount = new AtomicInteger(0);
    public void safeIncrement() {
        atomicCount.incrementAndGet();
    }

    // 正确用法：使用synchronized
    private int syncCount = 0;
    public synchronized void syncIncrement() {
        syncCount++;
    }
}
```

### 7.3 volatile适用条件总结

**使用volatile需要同时满足以下所有条件**：

> **volatile适用条件**：
> 1. 对变量的写入操作不依赖变量的当前值（纯赋值）
> 2. 变量不参与其他变量的不变式约束
> 3. 变量不需要与其他状态变量共同构成不变式
> 4. 访问变量时不需要加锁

| 场景 | 适用volatile | 原因 |
|------|-------------|------|
| boolean状态标记 | 是 | 纯赋值操作 |
| 一次性发布配置 | 是 | 发布后不修改 |
| 计数器（i++） | 否 | 依赖当前值，非原子 |
| 累加器（sum += x） | 否 | 依赖当前值，非原子 |
| 复合不变式（x > y） | 否 | 多个变量共同约束 |

---

## 8. volatile与synchronized对比

### 8.1 功能对比

| 特性 | volatile | synchronized |
|------|----------|-------------|
| 保证原子性 | 否（仅保证单次读/写原子性） | 是 |
| 保证可见性 | 是（立即刷新到主内存） | 是（解锁时刷新到主内存） |
| 保证有序性 | 是（禁止指令重排序） | 是（同一把锁内串行化） |
| 是否阻塞线程 | 否（无锁，无阻塞） | 是（锁竞争导致阻塞） |
| 性能开销 | 极低（仅有内存屏障） | 较高（锁升级、线程调度） |
| 使用范围 | 仅变量 | 方法、代码块 |
| 底层实现 | 内存屏障 + MESI缓存一致性 | Monitor对象（锁升级） |
| 能否保证复合操作安全 | 否 | 能 |

### 8.2 性能对比

```bash
性能对比（从高到低）：
volatile > 偏向锁 > 轻量级锁（CAS自旋） > 重量级锁（OS互斥量）

使用选择原则：
1. 能用volatile的绝不使用synchronized
2. 能用代码块同步的绝不用方法同步
3. 能用JUC原子类的绝不使用锁
```

### 8.3 选择策略

```java
/**
 * 选择策略示例
 */
public class ChoiceStrategyDemo {
    // 场景1：状态标记 → volatile
    private volatile boolean done;

    // 场景2：复合操作 → synchronized/Lock/Atomic
    private int count;
    public synchronized void increment() { count++; }

    private final AtomicInteger atomicCount = new AtomicInteger(0);
    public void atomicIncrement() { atomicCount.incrementAndGet(); }

    // 场景3：读写锁 → ReadWriteLock（读多写少）
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private Map<String, String> cache = new HashMap<>();

    public String get(String key) {
        rw.readLock().lock();
        try { return cache.get(key); } finally { rw.readLock().unlock(); }
    }

    public void put(String key, String value) {
        rw.writeLock().lock();
        try { cache.put(key, value); } finally { rw.writeLock().unlock(); }
    }
}
```

---

## 9. 双重检查锁定(DCL)与volatile

### 9.1 问题背景

单例模式的延迟初始化需要保证线程安全，最直接的方案是对 `getInstance()` 方法加锁，但这会导致每次获取实例时都有锁竞争，严重影响性能。双重检查锁定（Double-Checked Locking，DCL）通过先检查后加锁的方式，避免了不必要的锁竞争。

### 9.2 为什么需要volatile

```java
/**
 * 双重检查锁定单例模式（正确版本）
 *
 * volatile的关键作用：禁止指令重排序
 *
 * instance = new Singleton(); 的底层三步：
 * 1. memory = allocate();     // 分配内存空间
 * 2. ctorInstance(memory);    // 初始化对象（调用构造器）
 * 3. instance = memory;       // 将引用指向内存地址
 *
 * 没有volatile时，步骤2和3可能被重排序：
 * 1. memory = allocate();     // 分配内存空间
 * 2. instance = memory;       // 引用指向未初始化的内存（重排序！）
 * 3. ctorInstance(memory);    // 初始化对象（此时其他线程已经使用了instance！）
 *
 * 结果：其他线程拿到半初始化的instance，访问其字段时出现异常
 */
class Singleton {
    // volatile禁止指令重排序，保证happens-before关系
    private static volatile Singleton instance;

    private Singleton() {
        // 初始化操作
    }

    public static Singleton getInstance() {
        // 第一次检查：避免不必要的加锁
        if (instance == null) {
            // 加锁：保证临界区串行执行
            synchronized (Singleton.class) {
                // 第二次检查：防止重复创建
                if (instance == null) {
                    instance = new Singleton();
                    // volatile保证：
                    // 1. 分配内存 → 初始化对象 → 赋值（禁止重排序）
                    // 2. 赋值完成后，对其他线程立即可见
                }
            }
        }
        return instance;
    }
}
```

### 9.3 DCL执行流程图

```
线程A第一次调用getInstance()：

    instance == null ?
         │
         ├── true ──→ synchronized (Singleton.class)
         │                │
         │                ├── instance == null ?
         │                │       │
         │                │       ├── true ──→ instance = new Singleton()
         │                │       │              │
         │                │       │              ├── 1. allocate memory
         │                │       │              ├── 2. ctor (volatile禁止与3重排)
         │                │       │              └── 3. instance = memory (volatile写)
         │                │       │
         │                │       └── false ──→ 返回已有instance
         │                │
         │                └── 释放锁
         │
         └── false ──→ 直接返回instance


线程B在初始化过程中调用getInstance()：

    ┌─ 没有volatile ──────────────────────┐
    │ instance != null（但对象未初始化）     │
    │ ↓                                    │
    │ 使用instance → 空指针/字段异常        │
    └──────────────────────────────────────┘

    ┌─ 有volatile ────────────────────────┐
    │ instance == null（初始化未完成）      │
    │ ↓                                    │
    │ 进入同步块等待或继续检查              │
    │ 保证在instance赋值前已完全初始化      │
    └──────────────────────────────────────┘
```

### 9.4 其他线程安全单例方案

```java
/**
 * 方案1：饿汉式（类加载时初始化）
 * 天然线程安全，但可能造成资源浪费
 */
class EagerSingleton {
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    private EagerSingleton() {}
    public static EagerSingleton getInstance() { return INSTANCE; }
}

/**
 * 方案2：静态内部类（推荐）
 * 懒加载 + 天然线程安全（类加载机制保证）
 */
class StaticInnerSingleton {
    private StaticInnerSingleton() {}

    private static class Holder {
        static final StaticInnerSingleton INSTANCE = new StaticInnerSingleton();
    }

    public static StaticInnerSingleton getInstance() {
        return Holder.INSTANCE;  // 首次调用时触发Holder类加载
    }
}

/**
 * 方案3：枚举单例（最简洁、绝对安全）
 * 天然防止反射攻击和序列化破坏
 */
enum EnumSingleton {
    INSTANCE;

    private String config;
    public void setConfig(String config) { this.config = config; }
    public String getConfig() { return config; }
}
```

| 方案 | 懒加载 | 线程安全 | 防反射 | 防序列化破坏 | 性能 |
|------|--------|---------|--------|-------------|------|
| DCL + volatile | 是 | 是 | 否 | 否 | 高 |
| 饿汉式 | 否 | 是 | 否 | 否 | 高 |
| 静态内部类 | 是 | 是 | 否 | 否 | 高 |
| 枚举 | 否 | 是 | **是** | **是** | 高 |

---

## 10. 面试高频考点

### 10.1 基础问答

**Q1: 什么是JMM？它的核心作用是什么？**

JMM（Java Memory Model）是Java虚拟机定义的内存模型规范，用于屏蔽不同硬件和操作系统的内存访问差异，规定线程如何与主内存交互。核心作用是解决并发编程中的原子性、可见性、有序性问题，保证Java并发程序在各种平台下都能有一致的行为。

**Q2: JMM中的主内存和工作内存分别是什么？**

- **主内存**：所有线程共享的存储区域，存放共享变量（实例字段、静态字段、数组元素），对应JVM堆内存。
- **工作内存**：每个线程私有的存储区域，存放共享变量的副本，对应CPU缓存和寄存器。线程不能直接操作主内存，只能操作工作内存中的副本。

**Q3: JMM定义了哪些内存交互操作？**

8种原子操作：lock（锁定）、unlock（解锁）、read（读取）、load（载入）、use（使用）、assign（赋值）、store（存储）、write（写入）。read/load必须成对出现，store/write必须成对出现。

### 10.2 进阶问答

**Q4: volatile如何保证可见性？**

volatile写操作会立即刷新到主内存，并触发缓存一致性协议（MESI），使其他CPU核心的缓存行失效。volatile读操作会从主内存重新加载最新值。底层通过插入StoreStore、StoreLoad、LoadLoad、LoadStore内存屏障指令实现。

**Q5: volatile为什么不保证原子性？**

volatile仅保证单次读/写操作的原子性，但像 `i++` 这样的复合操作包含"读→改→写"三步，volatile无法保证这三步的不可分割性。线程A在读和写之间可能被线程B打断，导致数据丢失。

**Q6: 什么是happens-before原则？列举几个重要规则。**

happens-before是JMM保证可见性的核心原则：如果操作A happens-before操作B，则A的结果对B可见。重要规则包括：
1. 程序次序规则（单线程内顺序操作）
2. volatile变量规则（volatile写happens-before于volatile读）
3. 锁规则（解锁happens-before于加锁）
4. 传递性（A→B→C则A→C）
5. 线程启动/终止/中断规则

**Q7: 为什么双重检查锁定单例需要volatile？**

防止指令重排序导致获取到半初始化对象。`instance = new Singleton()` 在字节码层面分为三步：分配内存、初始化对象、赋值引用。没有volatile时，步骤2和3可能被重排序，导致其他线程拿到一个非空但未完成初始化的instance。

**Q8: volatile和synchronized的区别？**

| 维度 | volatile | synchronized |
|------|----------|-------------|
| 原子性 | 不保证 | 保证 |
| 可见性 | 保证 | 保证 |
| 有序性 | 保证（禁止重排序）| 保证（串行化） |
| 是否阻塞 | 否 | 是 |
| 使用范围 | 仅变量 | 方法/代码块 |
| 性能开销 | 极低 | 较高 |

### 10.3 深度问答

**Q9: JMM中有哪些内存屏障？它们的各自作用？**

四种内存屏障：
- **LoadLoad**：禁止前面的读操作与后面的读操作重排序
- **StoreStore**：禁止前面的写操作与后面的写操作重排序
- **LoadStore**：禁止前面的读操作与后面的写操作重排序
- **StoreLoad**：禁止前面的写操作与后面的读操作重排序（最昂贵）

**Q10: 什么是as-if-serial语义？**

无论编译器、处理器如何重排序，单线程程序的执行结果不变。这是JMM对程序员的基本承诺，也是指令重排序的安全边界。

**Q11: 64位变量的原子性问题？**

在32位JVM中，对 `long` 和 `double` 的读写操作可能被拆分为两次32位操作，不是原子操作。而在64位JVM中，对64位变量的读写是原子的。volatile可以保证64位变量的读写原子性（通过禁止非原子方式实现）。

**Q12: volatile在单例模式以外的应用场景？**

1. 线程停止标志
2. 轻量级状态开关
3. 一次性安全发布（publish-once对象引用）
4. 独立观察变量（如传感器数据）
5. 双重检查锁定

### 10.4 速查表

```
三大特性
├── 原子性：synchronized / Lock / Atomic*
├── 可见性：volatile / synchronized / Lock / final
└── 有序性：volatile / synchronized / Lock

happens-before八大规则
├── 1. 程序次序
├── 2. volatile变量
├── 3. 锁
├── 4. 传递性
├── 5. 线程启动
├── 6. 线程终止
├── 7. 中断
└── 8. finalizer

volatile三语义
├── 可见性（MESI缓存一致性）
├── 有序性（内存屏障）
└── 不保证原子性（复合操作需要锁）
```

---

## 参考资源

- [JSR-133: Java Memory Model and Thread Specification](https://jcp.org/en/jsr/detail?id=133)
- [JEP 188: Java Memory Model Update](https://openjdk.org/jeps/188)
- [The JSR-133 Cookbook for Compiler Writers](https://gee.cs.oswego.edu/dl/jmm/cookbook.html)
- [Java Concurrency in Practice (JCIP)](https://jcip.net/) — Chapter 16: The Java Memory Model
- [Alec Shipley: What's New in the JMM](https://shipilev.net/)
- [Doug Lea: JMM Causality Test Cases](https://www.cs.umd.edu/~pugh/java/memoryModel/)

---

> **核心总结**：JMM是Java并发编程的理论基石，所有并发安全问题都可以归结为原子性、可见性、有序性三者之一的破坏。volatile作为最轻量级的同步手段，通过内存屏障和缓存一致性协议保证可见性和有序性，但无法替代锁在原子性上的保障。理解happens-before原则是判断并发安全的关键，而DCL单例则是volatile最经典的实际应用场景。

*最后更新: 2026-07-26 | 适用于 JDK 8/11/17/21*
