# 03 — synchronized与锁升级

> 定位：彻底理解synchronized的三种用法、Monitor监视器原理、JDK 6+锁升级全链路（无锁→偏向锁→轻量级锁→重量级锁）及锁优化策略

## 目录

1. [synchronized三种用法](#1-synchronized三种用法)
2. [Java对象头与Monitor原理](#2-java对象头与monitor原理)
3. [synchronized底层实现](#3-synchronized底层实现)
4. [锁升级全链路：无锁→偏向锁→轻量级锁→重量级锁](#4-锁升级全链路无锁偏向锁轻量级锁重量级锁)
5. [偏向锁详解与JDK 15废弃](#5-偏向锁详解与jdk-15废弃)
6. [轻量级锁与自旋](#6-轻量级锁与自旋)
7. [重量级锁与Monitor](#7-重量级锁与monitor)
8. [锁优化：锁粗化、锁消除、锁细化](#8-锁优化锁粗化锁消除锁细化)
9. [synchronized vs Lock对比](#9-synchronized-vs-lock对比)
10. [面试高频考点](#10-面试高频考点)

---

## 1. synchronized三种用法

`synchronized`是Java原生的互斥锁，基于对象监视器（Monitor）实现，自动加锁和释放锁，能同时保障原子性、可见性、有序性三大特性。根据锁对象不同分为三种用法。

### 1.1 修饰实例方法

锁对象为当前实例对象（`this`），作用于单个对象的多线程访问，不同实例对象之间锁互不干扰。

```java
/**
 * 修饰实例方法 — 锁的是当前对象实例 this
 */
public class SynchronizedDemo {
    private int count = 0;

    // 同一时刻，同一个对象只能有一个线程进入此方法
    public synchronized void instanceMethod() {
        count++;
    }
    // 等价于：
    // public void instanceMethod() {
    //     synchronized (this) { count++; }
    // }
}
```

### 1.2 修饰静态方法

锁对象为当前类的Class对象（`SynchronizedDemo.class`），全局唯一，作用于该类的所有实例对象。

```java
/**
 * 修饰静态方法 — 锁的是 Class 对象
 */
public class SynchronizedDemo {
    private static int staticCount = 0;

    // 所有实例共享同一把 Class 锁
    public static synchronized void staticMethod() {
        staticCount++;
    }
    // 等价于：
    // public static void staticMethod() {
    //     synchronized (SynchronizedDemo.class) { staticCount++; }
    // }
}
```

### 1.3 修饰代码块（推荐）

自定义锁对象，锁粒度可灵活控制，仅对核心临界区加锁，而非整个方法，性能远高于同步方法。

```java
/**
 * 同步代码块 — 自定义锁对象，粒度最小
 */
public class SynchronizedDemo {
    private int count = 0;
    private final Object lock = new Object();  // 专用锁对象，推荐 private final

    public void blockMethod() {
        // 非临界区代码（无需加锁）
        doSomething();

        // 仅对核心临界区加锁
        synchronized (lock) {
            count++;
        }
    }

    // 也可以使用 this 作为锁
    public void thisLock() {
        synchronized (this) {
            count++;
        }
    }

    private void doSomething() {
        // 非同步操作
    }
}
```

### 1.4 三种用法对比

| 用法 | 锁对象 | 作用范围 | 粒度 | 推荐度 |
|------|--------|---------|------|--------|
| 修饰实例方法 | `this`（当前实例） | 单个实例的方法 | 方法级别 | 中 |
| 修饰静态方法 | `Class`对象（全局唯一） | 所有实例 | 方法级别 | 低（粒度过大） |
| 修饰代码块 | 自定义锁对象 | 代码块范围 | 块级别（最小） | **高** |

> 💡 **最佳实践**：优先使用同步代码块 + `private final` 专用锁对象，将锁粒度控制在最小范围；禁止使用字符串常量或基本类型包装类作为锁对象，避免锁对象误用导致同步失效。

> ⚠️ **关键注意**：
> - 多个线程竞争**同一把锁**才能实现同步，锁对象不同则同步失效
> - `synchronized`锁是**可重入锁**，同一线程可重复获取同一把锁，避免自死锁
> - 锁对象不能为`null`，否则抛出`NullPointerException`
> - 实例方法与静态方法混用`synchronized`，锁对象不同（this vs Class），无法互斥

---

## 2. Java对象头与Monitor原理

`synchronized`的底层实现依赖于Java对象头（Object Header）中的Mark Word。理解对象头结构是理解锁升级的前提。

### 2.1 对象内存布局

```
Java 对象内存布局（以 64 位 JVM 为例）：
┌──────────────────────────────────────────────────┐
│              对象头 (Object Header)               │
│  ┌────────────────────┬─────────────────────┐    │
│  │  Mark Word         │  Klass Pointer       │    │
│  │  (标记字段, 8B)    │  (类型指针, 4/8B)    │    │  ← 前 12/16B
│  └────────────────────┴─────────────────────┘    │
├──────────────────────────────────────────────────┤
│              实例数据 (Instance Data)              │  ← 成员变量
├──────────────────────────────────────────────────┤
│              对齐填充 (Padding)                    │  ← 8的倍数补齐
└──────────────────────────────────────────────────┘
```

| 组件 | 大小（64位JVM） | 说明 |
|------|----------------|------|
| Mark Word | 8字节（64bit） | 存储锁信息、GC标记、hashCode等，**锁升级的核心** |
| Klass Pointer | 4字节（指针压缩开启）或8字节 | 指向类元数据的指针 |
| 实例数据 | 可变 | 成员变量按类型对齐排列 |
| 对齐填充 | 可变 | 使总大小为8的倍数 |

### 2.2 Mark Word 结构详解

Mark Word是对象头的核心，其存储内容随锁状态动态变化。

```txt
64位JVM Mark Word 布局（小端模式）：

无锁 (01):
┌──────────┬────────────┬──────────┬──────────┬───────┐
│ unused:25│   hash:31  │ age:4    │ biased:0 │  tag  │  ← tag = 01
└──────────┴────────────┴──────────┴──────────┴───────┘

偏向锁 (01):
┌──────────┬────────────┬──────────┬──────────┬───────┐
│ thread:54│  epoch:2   │ age:4    │ biased:1 │  tag  │  ← tag = 01
└──────────┴────────────┴──────────┴──────────┴───────┘

轻量级锁 (00):
┌──────────────────────────────────────┬───────────────┐
│          LockRecord 指针 (62)        │    tag = 00   │
└──────────────────────────────────────┴───────────────┘

重量级锁 (10):
┌──────────────────────────────────────┬───────────────┐
│           Monitor 指针 (62)          │    tag = 10   │
└──────────────────────────────────────┴───────────────┘

GC 标记 (11):
┌──────────────────────────────────────┬───────────────┐
│              转发指针 (62)           │    tag = 11   │
└──────────────────────────────────────┴───────────────┘
```

### 2.3 Mark Word 字段速查表

| 锁状态 | 标志位(tag) | Mark Word 存储内容 | 说明 |
|--------|------------|-------------------|------|
| 无锁 | 01 | hashCode、GC分代年龄、偏向位=0 | 对象刚创建时 |
| 偏向锁 | 01 | 偏向线程ID、epoch、分代年龄、偏向位=1 | 第一次被线程获取 |
| 轻量级锁 | 00 | 指向栈中LockRecord的指针 | CAS竞争时 |
| 重量级锁 | 10 | 指向Monitor对象的指针 | 竞争激烈时 |
| GC标记 | 11 | 转发指针（Forwarding Pointer） | GC时使用 |

> 💡 **关键理解**：Mark Word是**复用内存**的设计，不同锁状态下同一块内存存储不同含义的数据。例如无锁时存储hashCode，偏向锁时存储线程ID，两者不可共存——因此调用`hashCode()`会撤销偏向锁。

---

## 3. synchronized底层实现

### 3.1 字节码层面

`synchronized`在字节码层面通过`monitorenter`和`monitorexit`两条指令实现。

```java
// Java 源码
public void demo() {
    synchronized (lock) {
        System.out.println("hello");
    }
}
```

编译后字节码（简化）：
```text
 0: aload_0
 1: getfield      #lock
 4: dup
 5: astore_1
 6: monitorenter              // 进入监视器，获取锁
 7: getstatic     #System.out
10: ldc           #hello
12: invokevirtual #println
15: aload_1
16: monitorexit               // 退出监视器，释放锁（正常路径）
17: goto          25
20: astore_2
21: aload_1
22: monitorexit               // 退出监视器，释放锁（异常路径）
23: aload_2
24: athrow
25: return
```

> 💡 注意`monitorexit`出现了两次：一次是正常退出，一次是异常退出（确保异常时锁也能释放）。这就是`synchronized`比`Lock`更安全的原因——JVM保证锁一定会释放。

### 3.2 同步方法层面

同步方法在字节码层面不使用`monitorenter/monitorexit`，而是通过方法表标志位`ACC_SYNCHRONIZED`来标识。

```text
// 实例同步方法在常量池中的标志
public synchronized void method();
  flags: ACC_PUBLIC, ACC_SYNCHRONIZED
  // JVM通过检查该标志自动完成加锁/解锁
```

### 3.3 Monitor 监视器机制

`synchronized`的底层依赖操作系统的Mutex Lock（互斥量），但JDK 6之后引入了锁升级机制来避免直接使用重量级的Mutex Lock。

```
Monitor（管程/监视器）的核心数据结构：
┌─────────────────────────────────────┐
│              ObjectMonitor          │
├─────────────────────────────────────┤
│  _owner        — 持有锁的线程       │
│  _EntryList    — 等待获取锁的队列   │  ← BLOCKED 状态的线程在此
│  _WaitSet      — 调用 wait() 的队列 │  ← WAITING 状态的线程在此
│  _recursions   — 重入次数           │
│  _count        — 计数器             │
└─────────────────────────────────────┘
```

| 组件 | 作用 |
|------|------|
| `_owner` | 标记当前持有锁的线程，null表示没有线程持有 |
| `_EntryList` | 竞争锁失败的线程进入该队列，被阻塞（BLOCKED） |
| `_WaitSet` | 调用`wait()`的线程进入该队列等待被唤醒 |
| `_recursions` | 记录锁的重入次数，实现可重入特性 |

---

## 4. 锁升级全链路：无锁→偏向锁→轻量级锁→重量级锁

JDK 6对`synchronized`进行了重大优化，引入锁升级机制（锁膨胀），核心思想是：**随着锁竞争加剧，锁从轻到重逐步升级，且只升不降**。

### 4.1 锁升级流程图

```txt
                          ┌──────────┐
                          │   无锁    │
                          │  tag=01   │
                          │ biased=0  │
                          └─────┬─────┘
                                │ 第一次被线程获取
                                ▼
                    ┌─────────────────────┐
                    │      偏向锁          │
                    │  tag=01, biased=1    │  ← 单线程反复获取，无竞争
                    │  存储偏向线程ID      │
                    └─────────┬───────────┘
                              │ 另一个线程尝试获取
                              ▼
                    ┌─────────────────────┐
                    │     轻量级锁         │
                    │  tag=00              │  ← 少量线程交替执行，自旋
                    │  指向LockRecord      │
                    └─────────┬───────────┘
                              │ 自旋失败 / 自旋次数到阈值
                              ▼
                    ┌─────────────────────┐
                    │     重量级锁         │
                    │  tag=10              │  ← 大量线程竞争，阻塞
                    │  指向Monitor         │
                    └─────────────────────┘
```

### 4.2 锁升级核心原则

```java
/**
 * 锁升级演进过程（只升不降）
 *
 * 无锁 → 偏向锁：同一个线程第一次获取锁
 * 偏向锁 → 轻量级锁：另一个线程竞争偏向锁（偏向锁撤销）
 * 轻量级锁 → 重量级锁：自旋超过阈值或自旋线程数过多
 *
 * 注意：
 * - 锁只能升级，不能降级（GC时可能降级，但仅是短暂状态）
 * - JDK 15+ 默认禁用偏向锁，升级路径变为：无锁 → 轻量级锁 → 重量级锁
 * - 锁升级是针对对象头的 Mark Word，不同锁状态复用同一块内存
 */
```

### 4.3 观测锁升级：使用JOL工具

```java
// 依赖：org.openjdk.jol:jol-core
// JVM 参数：-XX:+UseBiasedLocking (JDK 8 默认开启，JDK 15+ 移除)

import org.openjdk.jol.info.ClassLayout;

public class LockUpgradeDemo {
    public static void main(String[] args) throws Exception {
        Object obj = new Object();

        // 1. 无锁状态：biased=0, tag=01
        System.out.println("=== 无锁状态 ===");
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());

        // 2. 偏向锁：第一次 synchronized（需要延迟启动）
        //    注意：JVM 启动后有 4 秒偏向锁延迟（-XX:BiasedLockingStartupDelay=4000）
        synchronized (obj) {
            System.out.println("=== 偏向锁（或轻量级锁）===");
            System.out.println(ClassLayout.parseInstance(obj).toPrintable());
        }

        // 3. 模拟轻量级锁：另一个线程竞争
        Thread t = new Thread(() -> {
            synchronized (obj) {
                System.out.println("=== 轻量级锁（竞争产生）===");
                System.out.println(ClassLayout.parseInstance(obj).toPrintable());
            }
        });
        t.start();
        t.join();

        // 4. 重量级锁：模拟大量竞争（实际需要多个线程并发）
        // 使用 -XX:-UseBiasedLocking 强制关闭偏向锁可观察到
    }
}
```

> 💡 **JVM偏向锁延迟**：JDK 8默认在JVM启动后的4秒内偏向锁不可用（`-XX:BiasedLockingStartupDelay=4000`），这是为了减少JVM启动时的锁竞争优化。可通过`-XX:BiasedLockingStartupDelay=0`关闭延迟。

---

## 5. 偏向锁详解与JDK 15废弃

### 5.1 偏向锁原理

偏向锁的思想是：**锁不仅不存在竞争，而且同一个线程反复获取**。在这种情况下，让线程第一次获取锁时记录线程ID，后续该线程再次进入时直接通过Mark Word判断即可，无需CAS操作。

```txt
偏向锁获取流程：
1. 线程T第一次获取锁
   ┌─────────────────────────────┐
   │ Mark Word 当前为无锁(01)    │
   │ CAS 将线程T的ID写入Mark Word│
   │ __owner = Thread ID of T    │
   │ biased = 1                  │
   └─────────────────────────────┘
   
2. 线程T再次获取锁（重入）
   ┌─────────────────────────────┐
   │ Mark Word 已记录线程T的ID   │
   │ 直接进入，不需CAS，不需同步 │  ← 极低成本！
   └─────────────────────────────┘

3. 线程U尝试获取锁（发生竞争）
   ┌─────────────────────────────┐
   │ 检测到biased=1              │
   │ 但__owner ≠ Thread ID of U  │
   │ 到达安全点 -> 撤销偏向锁     │
   │ -> 升级为轻量级锁           │
   └─────────────────────────────┘
```

### 5.2 偏向锁的优劣

| 优势 | 劣势 |
|------|------|
| 同一线程反复加锁开销极低（仅一次CAS） | 偏向锁撤销需要等待全局安全点（STW） |
| 适合锁竞争极少、同一线程反复获取的场景 | 撤销成本高，甚至高于直接使用轻量级锁 |
| 减少不必要的CAS操作 | 在高竞争场景下反而降低性能 |
| 提升单线程加锁性能 | 与hashCode()不兼容（调用hashCode会撤销偏向锁） |

### 5.3 偏向锁的批量撤销与批量重偏向

JVM为了优化偏向锁在特定场景下的性能，引入了批量撤销和批量重偏向机制：

```txt
批量重偏向（Bulk Rebias）：
  一个类的大多数锁对象被不同线程获取
  JVM将类的epoch+1，将已撤销偏向锁的对象视为可重偏向

批量撤销（Bulk Revocation）：
  一个类的大多数锁对象发生竞争（撤销阈值超过20）
  JVM认为该类不适合偏向锁，直接禁用该类的偏向锁
```

| 阈值 | 触发行为 |
|------|---------|
| 偏向锁撤销次数 >= 20 | 触发批量重偏向（epoch+1） |
| 偏向锁撤销次数 >= 40 | 触发批量撤销，禁用该类偏向锁 |
| -XX:BiasedLockingBulkRebiasThreshold=20 | 批量重偏向阈值 |
| -XX:BiasedLockingBulkRevokeThreshold=40 | 批量撤销阈值 |

### 5.4 JDK 15 废弃偏向锁

```txt
JEP 374: Deprecate and Disable Biased Locking (JDK 15)

原因：
1. 偏向锁的维护成本远高于其带来的收益
2. JDK 15+ 默认偏向锁延迟为4秒，之后的新应用很少达到此阶段
3. 虚拟线程（Project Loom）的架构与偏向锁不兼容
4. 现代Java应用的锁竞争模式远比偏向锁设计时复杂

影响：
- JDK 15+：-XX:+UseBiasedLocking 为 deprecated
- JDK 17+：偏向锁代码基本移除
- 升级路径变为：无锁 → 轻量级锁 → 重量级锁
```

> ⚠️ **重要**：JDK 15+ 不再支持偏向锁，偏向锁相关内容主要适用于JDK 8~11。面试中对偏向锁的考察重点是**理解其设计思想**和**为何被废弃**。

---

## 6. 轻量级锁与自旋

### 6.1 轻量级锁原理

轻量级锁适用于**多线程交替执行临界区**（而非同时竞争）的场景。线程通过CAS操作在栈帧中创建LockRecord，尝试将对象头的Mark Word更新为指向LockRecord的指针。

```txt
轻量级锁加锁流程：

线程 T1 的栈帧                   堆中的对象
┌─────────────────┐            ┌──────────────┐
│  LockRecord     │            │  Mark Word   │
│  ┌───────────┐  │     CAS    │  ┌─────────┐ │
│  │ displaced │──┼───────────▶│  │指向LR   │ │  ← 成功：T1持有锁
│  │ Mark Word │  │  attempt   │  │(00)     │ │
│  └───────────┘  │  (00)      │  └─────────┘ │
└─────────────────┘            └──────────────┘

如果 CAS 失败（Mark Word 已被其他线程修改为指向其他LockRecord）：
→ 锁竞争发生，进入自旋等待
→ 自旋次数达到阈值 → 膨胀为重量级锁
```

```java
/**
 * 轻量级锁示意
 * 实际由 JVM 内部实现，Java 层面只使用 synchronized
 */
public class LightweightLockDemo {
    private final Object lock = new Object();
    private int count = 0;

    public void increment() {
        // 编译后：monitorenter（尝试 CAS 获取轻量级锁）
        synchronized (lock) {
            count++;
        }
        // 编译后：monitorexit（释放锁，恢复 displaced Mark Word）
    }
}
```

### 6.2 自旋优化

自旋是轻量级锁的核心优化手段：当锁被其他线程持有时，当前线程不立即阻塞，而是**循环等待**（自旋），期望持有锁的线程能很快释放锁。

```txt
自旋流程：
                    ┌──────────────┐
                    │ 尝试获取轻量级锁│
                    │   (CAS)       │
                    └──────┬───────┘
                     成功  │  失败
                      ↓    ↓
                    ┌──────────────┐
               ┌───▶│  自旋等待    │
               │    │ (循环N次)    │
               │    └──────┬───────┘
               │     成功  │  失败
               │      ↓    ↓
               │    ┌──────────────┐
               │    │ 返回成功     │
               │    └──────────────┘
               │    ┌──────────────┐
               └────│ CAS重试      │  ← 自旋期间尝试获取锁
                    └──────────────┘
```

### 6.3 自旋参数调优

| JVM参数 | 默认值 | 说明 |
|---------|--------|------|
| `-XX:PreBlockSpin` | 10 | 自旋次数（JDK 6之前） |
| `-XX:+UseSpinning` | true | 开启自旋（JDK 6+默认开启） |
| `-XX:MaxTenuringThreshold` | 15 | 自适应自旋，JVM动态调整 |
| `-XX:+UseAdaptiveSizePolicy` | true | 自适应自旋开关 |

> 💡 **自适应自旋**（JDK 6+）：JVM根据前一次自旋的结果动态调整自旋次数。如果上次自旋后成功获取锁，JVM会适当增加自旋次数；如果很少成功，JVM会减少甚至省略自旋，避免CPU空转。

### 6.4 自旋的优缺点

| 优点 | 缺点 |
|------|------|
| 避免线程阻塞/唤醒的上下文切换开销 | 自旋期间占用CPU但不做有用工作 |
| 适合锁持有时间短的场景 | 锁持有时间长时自旋浪费CPU |
| 响应速度快，不进入OS调度 | 自旋线程数过多会导致CPU飙升 |

> 🎯 **团队规范**：选择合适的锁方案应基于临界区的执行时间。锁内操作执行时间短（纳秒级）时，自旋收益高；锁内执行时间长（微秒级以上或因IO阻塞），应直接使用重量级锁或Lock接口，避免CPU空转。

---

## 7. 重量级锁与Monitor

### 7.1 重量级锁原理

当轻量级锁自旋失败或自旋次数达到阈值时，锁会升级为重量级锁。重量级锁依赖于操作系统的Mutex Lock，线程在获取不到锁时进入**阻塞状态**（BLOCKED），不占用CPU。

```txt
重量级锁的获取流程：

1. 锁膨胀
   对象 Mark Word → 指向 ObjectMonitor 的指针（tag=10）
   
2. 线程竞争锁
   ┌──────────┐       ┌─────────────────┐
   │ 线程 A   │──────▶│ _owner = 线程A   │  ← 持有锁
   └──────────┘       └─────────────────┘
   
   ┌──────────┐       ┌─────────────────┐
   │ 线程 B   │──────▶│ _EntryList 排队  │  ← BLOCKED
   └──────────┘       └─────────────────┘
   
   ┌──────────┐       ┌─────────────────┐
   │ 线程 C   │──────▶│ _EntryList 排队  │  ← BLOCKED
   └──────────┘       └─────────────────┘

3. 锁释放
   线程A执行完毕 → 释放锁 → 唤醒 _EntryList 中的线程
   被唤醒的线程重新竞争 _owner（非公平）
```

### 7.2 ObjectMonitor 核心结构

```cpp
// HotSpot 源码 ObjectMonitor 核心字段（C++ 层面）
struct ObjectMonitor {
    volatile markOop   _header;       // 原始 Mark Word（用于恢复）
    void*              _object;       // 关联的 Java 对象
    void*              _owner;        // 持有锁的线程
    ObjectWaiter*      _EntryList;    // 等待获取锁的线程列表（BLOCKED）
    ObjectWaiter*      _WaitSet;      // 执行 wait() 的线程列表（WAITING）
    volatile intptr_t  _recursions;   // 锁重入次数
    int                _count;        // 锁计数器
    int                _SpinDuration; // 自旋时间（用于自适应自旋）
};
```

### 7.3 wait/notify 与 Monitor 的关系

```java
/**
 * wait/notify 机制底层依托 Monitor 的 WaitSet
 *
 * 调用 obj.wait() 时：
 * 1. 当前线程必须持有 obj 的锁（否则抛 IllegalMonitorStateException）
 * 2. JVM 将当前线程移入 Monitor 的 _WaitSet
 * 3. 线程释放锁（_owner = null）
 * 4. 线程状态变为 WAITING
 *
 * 调用 obj.notify() 时：
 * 1. 从 _WaitSet 中随机唤醒一个线程
 * 2. 被唤醒的线程移入 _EntryList 重新竞争锁
 * 3. 获得锁后才能从 wait() 返回继续执行
 */
public class WaitNotifyDemo {
    private final Object lock = new Object();

    public void waitMethod() throws InterruptedException {
        synchronized (lock) {
            // 释放锁并进入 WAITING 状态
            lock.wait();          // 必须在 synchronized 内
            // 被唤醒后重新获取锁，继续执行
        }
    }

    public void notifyMethod() {
        synchronized (lock) {
            // 从 _WaitSet 中唤醒一个线程
            lock.notify();        // 必须在 synchronized 内
            // lock.notifyAll();  // 唤醒所有等待线程
        }
    }
}
```

### 7.4 wait/notify 核心规则

| 要求 | 说明 |
|------|------|
| 必须持有锁 | 必须在`synchronized`代码块/方法内调用 |
| 调用对象必须与锁对象一致 | `lock.wait()` 必须在 `synchronized(lock)` 内 |
| wait 释放锁 | 调用wait后线程释放锁并进入WAITING状态 |
| sleep 不释放锁 | `Thread.sleep()`不释放锁，进入TIMED_WAITING |
| notify 随机唤醒 | 随机选择一个_WaitSet中的线程唤醒（非公平） |
| notifyAll 唤醒全部 | 所有_WaitSet中的线程进入_EntryList竞争 |

> ⚠️ **常见陷阱**：`wait()`应在循环中调用（**Guarded Suspension模式**），而非if判断。因为线程被唤醒后可能不满足执行条件（虚假唤醒或条件被其他线程修改）。
> ```java
> synchronized (lock) {
>     while (!condition) {  // 必须用 while，不能用 if
>         lock.wait();
>     }
>     // 条件满足，继续执行
> }
> ```

---

## 8. 锁优化：锁粗化、锁消除、锁细化

### 8.1 锁粗化（Lock Coarsening）

将多个连续的加锁/解锁操作合并为一次，减少锁操作次数。

```java
/**
 * 锁粗化前：循环内反复加锁解锁
 */
// ❌ JIT 会认为这是低效的
StringBuffer sb = new StringBuffer();
for (int i = 0; i < 100; i++) {
    synchronized (sb) {       // 每次都加锁/解锁
        sb.append(i);
    }
}

/**
 * 锁粗化后：JIT 优化为
 */
// ✅ JVM 会自动优化为：
synchronized (sb) {          // 一次性加锁
    for (int i = 0; i < 100; i++) {
        sb.append(i);
    }
}                             // 一次性解锁
```

> 💡 JVM的JIT编译器会自动进行锁粗化，但手工编写时仍建议将循环体内的` synchronized `移到循环外，减少锁竞争次数。

### 8.2 锁消除（Lock Elimination）

JIT通过逃逸分析发现锁对象只被当前线程访问，不存在竞争时，直接消除锁操作。

```java
/**
 * 锁消除示例
 * JVM 通过逃逸分析发现 lock 对象不会被其他线程访问
 */
public class LockEliminationDemo {

    // ❌ 看起来有锁，但实际上不存在竞争
    public void append(String s1, String s2) {
        StringBuffer sb = new StringBuffer();  // sb 是局部变量
        sb.append(s1);                         // 不会逃逸
        sb.append(s2);                         // JIT 消除锁
    }

    // ✅ 等价于无锁操作
    public void appendOptimized(String s1, String s2) {
        StringBuilder sb = new StringBuilder();
        sb.append(s1);
        sb.append(s2);
    }
}
```

锁消除的JVM参数控制：

| 参数 | 默认 | 说明 |
|------|------|------|
| `-XX:+EliminateLocks` | true | 开启锁消除（JDK 6+） |
| `-XX:+DoEscapeAnalysis` | true | 开启逃逸分析 |
| `-XX:+UseBiasedLocking` | true(JDK8) / false(JDK15+) | 偏向锁 |

### 8.3 锁细化（减小锁粒度）

锁细化是**开发者主动优化**，将大锁拆分为多个小锁，减少锁竞争的范围。

```java
import java.util.concurrent.ConcurrentHashMap;

/**
 * 锁细化示例
 */
public class LockGranularityDemo {

    // ❌ 粗粒度：对整个方法加锁
    // 并发度极低，不同key的操作互相阻塞
    private final Map<String, Integer> badCache = new HashMap<>();

    public synchronized Integer getFromBadCache(String key) {
        return badCache.get(key);
    }

    // ✅ 细粒度：只锁需要同步的代码块
    private final Map<String, Integer> cache = new HashMap<>();
    private final Object lock = new Object();

    public Integer getFromCache(String key) {
        // 读操作可能不需要锁（根据业务决定）
        Integer val = cache.get(key);
        if (val != null) return val;

        // 仅在写操作时加锁
        synchronized (lock) {
            // double-check
            val = cache.get(key);
            if (val == null) {
                val = computeValue(key);
                cache.put(key, val);
            }
            return val;
        }
    }

    // ✅ 最细粒度：使用并发容器代替手写同步
    private final ConcurrentHashMap<String, Integer> concurrentCache
            = new ConcurrentHashMap<>();

    public Integer getFromConcurrentCache(String key) {
        return concurrentCache.computeIfAbsent(key, this::computeValue);
    }

    private Integer computeValue(String key) {
        return key.hashCode(); // 模拟耗时计算
    }
}
```

### 8.4 三大优化对比

| 优化手段 | 执行者 | 核心理念 | 效果 |
|---------|--------|---------|------|
| 锁粗化 | JIT自动/开发者手动 | 合并多次加锁为一次 | 减少锁操作次数 |
| 锁消除 | JIT自动 | 通过逃逸分析删除无用锁 | 消除无竞争锁开销 |
| 锁细化 | 开发者手动 | 大锁拆小锁，减少竞争范围 | 提升并发度 |

> 🎯 **优化原则**：先保证正确性，再追求性能。不要过早优化锁粒度，先用JMH基准测试确认瓶颈再动手。

---

## 9. synchronized vs Lock对比

### 9.1 核心对比表

| 比较维度 | synchronized | ReentrantLock |
|---------|-------------|---------------|
| 关键字/接口 | JVM关键字 | `java.util.concurrent.locks.Lock`接口 |
| 锁获取/释放 | 隐式（自动释放） | 显式（必须 `finally{ lock.unlock() }`） |
| 公平性 | 非公平 | 支持公平和非公平（构造参数） |
| 可中断 | **不可中断**（`lock()`不响应中断） | 支持 `lockInterruptibly()` |
| 超时获取 | 不支持 | 支持 `tryLock(timeout, unit)` |
| 多个条件 | 一个等待队列（`wait/notify`） | 多个 `Condition`（精细控制） |
| 性能（JDK 6+） | 已高度优化，接近于Lock | 高竞争场景略优 |
| 调试追踪 | 简单，线程栈直接显示 | 较复杂，需额外标记 |
| 与虚拟线程 | 会"钉住(pin)"平台线程 | 不会pin住 |
| 锁状态检查 | 不支持 | `tryLock()`, `isHeldByCurrentThread()` |

### 9.2 代码对比

```java
/**
 * synchronized vs ReentrantLock 代码对比
 */
public class LockComparisonDemo {
    private int count = 0;

    // ===== synchronized =====
    private final Object syncLock = new Object();

    public void syncIncrement() {
        synchronized (syncLock) {
            count++;
        }  // 自动释放锁
    }

    // ===== ReentrantLock =====
    private final ReentrantLock lock = new ReentrantLock(true); // 公平锁

    public void lockIncrement() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();  // 必须手动释放！
        }
    }

    // Lock 独有功能：tryLock 超时
    public boolean tryIncrement() {
        boolean acquired = false;
        try {
            acquired = lock.tryLock(1, TimeUnit.SECONDS);
            if (acquired) {
                count++;
                return true;
            }
            return false;  // 超时未获取锁，可以处理其他逻辑
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    // Lock 独有功能：多个 Condition 等待队列
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public void conditionDemo() throws InterruptedException {
        lock.lock();
        try {
            // 类似 wait()，但只影响"不满"条件队列
            notFull.await();
            // 唤醒"非空"条件队列的线程
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
}
```

### 9.3 选择建议

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| 简单同步，无复杂条件 | `synchronized` | 简洁、安全、不易出错 |
| 需要可中断/超时 | `ReentrantLock` | `lockInterruptibly()` / `tryLock()` |
| 读多写少 | `ReentrantReadWriteLock` / `StampedLock` | 读写分离，读不互斥 |
| 虚拟线程环境 | `ReentrantLock`（优先） | `synchronized`会pin住平台线程 |
| 多个条件变量 | `ReentrantLock` + `Condition` | 多个独立等待队列 |
| 性能敏感高竞争 | 先`LongAdder`/CAS无锁方案 | 无锁 > 有锁 |

> ⚠️ **虚拟线程注意事项**（JDK 21+）：`synchronized`在虚拟线程中会"钉住"底层平台线程，使得虚拟线程无法在阻塞时yield回调度器，降低了虚拟线程的优势。在虚拟线程环境中应优先使用`ReentrantLock`。

---

## 10. 面试高频考点

### 10.1 基础问题

**Q1: synchronized的三种用法及锁对象分别是什么？**
- 实例方法：锁`this`当前实例
- 静态方法：锁`Class`对象
- 代码块：锁自定义对象

**Q2: 什么是可重入锁？synchronized是可重入的吗？**
- 可重入：同一个线程可重复获取已持有的锁，避免自死锁
- `synchronized`是可重入的，依靠Monitor的`_recursions`计数器实现

**Q3: `wait()`和`sleep()`的区别？**
| 维度 | wait() | sleep() |
|------|--------|---------|
| 所属类 | `Object` | `Thread` `TimeUnit` |
| 释放锁 | **释放**锁 | **不释放**锁 |
| 必须持有锁 | 是（否则抛异常） | 否 |
| 状态 | WAITING | TIMED_WAITING |
| 唤醒方式 | notify/notifyAll | 时间到/中断 |

**Q4: `notify()`和`notifyAll()`的区别？**
- `notify()`：随机唤醒一个等待线程（不公平，可能产生信号丢失）
- `notifyAll()`：唤醒所有等待线程，共同竞争锁（更安全，推荐）

### 10.2 进阶问题

**Q5: 描述synchronized的锁升级过程？**
```
JDK 8：无锁 → 偏向锁 → 轻量级锁 → 重量级锁（只升不降）
JDK 15+：无锁 → 轻量级锁 → 重量级锁（偏向锁废弃）
```
触发条件：
- 偏向锁：单线程反复获取同一锁
- 轻量级锁：多线程交替获取，少量竞争
- 重量级锁：多线程同时竞争，自旋失败

**Q6: Java对象头中的Mark Word在锁升级中如何变化？**
- 无锁(01)：存储hashCode + GC年龄
- 偏向锁(01)：存储偏向线程ID + epoch + GC年龄
- 轻量级锁(00)：存储栈中LockRecord指针
- 重量级锁(10)：存储堆中Monitor指针

**Q7: 偏向锁为什么在JDK 15被废弃？**
- 维护成本高，复杂场景下收益有限
- 现代应用的锁竞争模式更复杂
- 与虚拟线程架构不兼容
- 撤销偏向锁需要STW（Stop-The-World）

**Q8: 轻量级锁和重量级锁的本质区别？**
| 维度 | 轻量级锁 | 重量级锁 |
|------|---------|---------|
| 实现 | CAS + 自旋 | OS Mutex + 阻塞 |
| 线程等待 | 自旋（不阻塞，占用CPU） | 阻塞（不占用CPU） |
| 适用场景 | 锁持有时间短 | 锁持有时间长 |
| 上下文切换 | 无 | 有（成本高） |

**Q9: 什么是锁消除和锁粗化？**
- 锁消除：JIT通过逃逸分析消除不会产生竞争的锁
- 锁粗化：JIT将多次连续的加锁/解锁合并为一次

**Q10: 如何选择synchronized和ReentrantLock？**
- 简单场景用`synchronized`（安全、简洁、自动释放）
- 需要可中断、超时、公平锁、多个Condition时用`ReentrantLock`
- 虚拟线程环境优先用`ReentrantLock`
- 读多写少场景优先用`ReadWriteLock`/`StampedLock`

### 10.3 深度问题

**Q11: 一个对象调用`hashCode()`后还能进入偏向锁吗？**
- 不能。偏向锁的Mark Word需要存储偏向线程ID，与hashCode空间冲突
- 调用`hashCode()`后对象处于无锁(01)状态（hashCode已填入），进入synchronized时直接升级为轻量级锁
- 偏向锁撤销的触发条件之一就是identityHashCode的访问

**Q12: 为什么wait()必须在synchronized块中调用？**
- `wait()`需要操作ObjectMonitor的_WaitSet
- 不持有Monitor（synchronized未获取锁）则无法操作Monitor内部结构
- 设计意图：避免"lost wake-up"问题（notify在wait之前发生）

**Q13: 如何观测锁升级？**
- 使用JOL（Java Object Layout）工具查看Mark Word
- 使用`-XX:+PrintBiasedLockingStatistics`打印偏向锁统计
- 使用`-XX:+TraceBiasedLocking`追踪偏向锁行为
- 使用`jstack`查看线程阻塞状态判断锁竞争情况

**Q14: 自旋失败的线程会立即升级为重量级锁吗？**
- 不一定会立即升级。JVM的自适应自旋会动态调整策略
- 自旋次数达到阈值（或自旋线程数超过CPU核数的一半）时升级
- 升级后，未获取到锁的线程进入BLOCKED状态（OS互斥量排队）

---

## 附录：相关JVM参数速查

| 参数 | 默认值 | 作用 |
|------|--------|------|
| `-XX:+UseBiasedLocking` | true(JDK8) / false(JDK15+) | 开启偏向锁 |
| `-XX:BiasedLockingStartupDelay=0` | 4000ms | 偏向锁启动延迟 |
| `-XX:BiasedLockingBulkRebiasThreshold=20` | 20 | 批量重偏向阈值 |
| `-XX:BiasedLockingBulkRevokeThreshold=40` | 40 | 批量撤销阈值 |
| `-XX:+UseSpinning` | true | 开启自旋 |
| `-XX:PreBlockSpin=10` | 10 | 自旋次数（仅JDK 6之前） |
| `-XX:+EliminateLocks` | true | 锁消除 |
| `-XX:+DoEscapeAnalysis` | true | 逃逸分析 |
| `-XX:-RestrictContended` | false | `@jdk.internal.vm.annotation.Contended`注解 |

---

> 🎯 **核心总结**：`synchronized`是Java并发编程的基石，理解其从偏向锁到重量级锁的完整升级链路是JUC学习的核心。JDK 6+通过锁升级机制让synchronized在无竞争时几乎零开销，有竞争时逐步加重。掌握锁优化的三大手段（粗化、消除、细化）和与Lock接口的选型对比，能帮助你在不同并发场景下做出正确选择。在JDK 21+虚拟线程时代，理解synchronized的pin问题并合理选用Lock接口同样重要。

*最后更新：2026-07-26 | 适用于 JDK 8/11/17/21*
