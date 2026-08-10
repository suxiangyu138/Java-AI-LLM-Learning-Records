# 02 synchronized 与 volatile 深潜

> JVM 级互斥锁与现代内存可见性的全部细节——从对象头与 Monitor 到锁升级路径，再到虚拟线程 pinning（JEP 491）

---

## 📚 目录

1. [synchronized 语义与三种用法](#1-synchronized-语义与三种用法)
2. [对象头、Monitor 与锁升级](#2-对象头monitor-与锁升级)
3. [锁消除、锁粗化与 JIT 优化](#3-锁消除锁粗化与-jit-优化)
4. [虚拟线程 pinning 与 JEP 491](#4-虚拟线程-pinning-与-jep-491)
5. [volatile：可见性与有序性的边界](#5-volatile可见性与有序性的边界)

---

## 1. synchronized 语义与三种用法

synchronized 是 JVM 内置的互斥锁，同时满足并发三要素：互斥保证原子性，进入/退出锁时建立 happens-before 保证可见性，临界区整体不重排保证有序性。它是可重入的——同一线程可重复进入自己持有的锁，递归调用不会死锁。

三种用法对应三个锁粒度：

```java
// 1. 静态方法：锁 Class 对象（全局互斥）
public static synchronized void a() {}

// 2. 实例方法：锁 this（同实例互斥）
public synchronized void b() {}

// 3. 代码块：锁任意对象，粒度最细
public void c() {
    synchronized (this.lock) { ... }
}
```

用法的核心区别在**锁对象是谁**：静态方法锁 Class，实例方法锁 this，代码块锁指定对象。`synchronized(new Object())` 是经典反模式——每次 new 一个新锁对象，等于没锁。锁的语义详见[Java多线程 04 篇](../../02-JUC%20多线程%20高并发编程/Java多线程/04-synchronized与volatile.md)。

## 2. 对象头、Monitor 与锁升级

synchronized 的实现依托对象头（Mark Word）与 Monitor。Mark Word 64 位里存放锁状态、hashCode、分代年龄等，锁状态的变迁就发生在 Mark Word 的位切换上：

- **无锁**：Mark Word 记录 hashCode 与分代年龄。
- **偏向锁**（JDK 15 标记废弃，JDK 18 默认关闭并移除以回归完整移除路径）：记录持有线程 ID，无竞争时零开销。现代 JVM 直接走轻量级锁，面试讲清楚"历史路径 + 现在默认关闭"即可。
- **轻量级锁**：线程 CAS 把 Mark Word 替换为栈中锁记录指针；无竞争时一次 CAS 完成加锁，无内核交互。锁竞争出现时，竞争线程自旋等待，超过阈值后升级为重量级锁。
- **重量级锁**：依赖操作系统 Mutex，通过 Monitor（ObjectMonitor）维护 EntryList 与 WaitSet，进入阻塞-唤醒的内核态交互，开销最大。

升级是**单向不可逆**的：无锁 → 轻量级 → 重量级。理解这条路径的价值在于"加锁成本不是固定的"——无竞争时 synchronized 开销极小，所以"synchronized 性能差"是过时认知；真正昂贵的是**竞争**导致的升级与阻塞唤醒，优化方向永远是减小临界区与缩短持锁时间。竞争到来时，轻量级锁的自旋也不是无脑空转——JVM 有适应性自旋（Adaptive Spinning）：根据最近一次同锁自旋成功率动态调整自旋次数，自旋成功率高就多等一会，否则尽快升级重量级，避免 CPU 空耗。

Monitor 内部有三个关键角色：**Owner**（持锁线程）、**EntryList**（等锁队列）、**WaitSet**（等待队列）。wait/notify 的语义全在这三者之间流转：`wait()` 释放锁、Owner 让位、线程进 WaitSet；`notify()` 把 WaitSet 中一个线程移到 EntryList 重新抢锁；`notifyAll()` 全员转移。经典陷阱是**虚假唤醒**——被唤醒不保证条件成立（可能是 notify 风暴或 JVM 实现细节），必须 while 循环检查条件，这也是 `LockSupport.park` 语境下同样的纪律。另一个易错点：**synchronized 块内抛异常会自动释放锁**（JVM 保证），而 Lock 不会——这是 Lock 必须 try-finally 的原因。

## 3. 锁消除、锁粗化与 JIT 优化

JIT 编译器在 C2 层做两个看似矛盾的优化：

- **锁消除**：逃逸分析证明锁对象只在线程内使用（如方法内 new 的局部 StringBuilder），直接去掉加锁——所以单线程代码里的 synchronized 可能零开销。
- **锁粗化**：连续的加锁-解锁（如循环体内反复 synchronized 同一对象）合并为一个临界区，减少加解锁次数。代价是临界区变长——因此手写代码不要人为拆得太碎。

这两个优化说明：**性能优化的结论必须基于测量**，JIT 层的行为不能用直觉推断。配合锁升级机制，synchronized 在现代 JVM 上的表现早已不是"重锁"代名词。

## 4. 虚拟线程 pinning 与 JEP 491

虚拟线程执行到 synchronized 块时，如果内部发生阻塞（如 IO），**载入线程被 pin 住**——虚拟线程退出调度器而载入线程也腾不出来，百万虚拟线程可能退化为数十个平台线程的吞吐。这就是 pinning 问题。

修复历程：JDK 24 通过 [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491) 移除了对象监视器的线程本地缓存，**synchronized 阻塞不再 pin 载入线程**，虚拟线程可自由卸载重挂。但注意边界：

- **synchronized 内的阻塞已基本解除 pinning**，但**嵌套在 native 方法或外部调用中持锁**的路径仍可能 pin；
- 保守做法：虚拟线程内的长阻塞路径尽量用 `ReentrantLock`（纯 Java 实现，无条件不 pin）；
- 生产检查清单：JDBC 驱动需虚拟线程友好（JDBC 5.0+ 或 R2DBC），日志/序列化等库避免在持锁块内做阻塞 IO。

## 5. volatile：可见性与有序性的边界

volatile 提供两种保证，**不提供**第三种：

- ✅ **可见性**：写入 volatile 变量立即刷新主内存并使其他核心缓存行失效；读 volatile 强制从主内存取。这是它最常见的使用场景——状态标志位、DCL 单例、发布"已初始化完成"信号。
- ✅ **有序性**：volatile 写禁止与其前后的读写重排序（JMM 插入内存屏障：写前 StoreStore、写后 StoreLoad，读后 LoadLoad/LoadStore）。DCL 单例全靠这条保证对象构造完成先于引用发布。
- ❌ **原子性**：`volatile int i; i++` 依然是读-改-写三步，高并发下丢更新。计数必须用 AtomicInteger 或 LongAdder（见 [04 篇](04-CAS与原子类.md)）。

DCL 单例是 volatile 有序性的教科书案例——`instance = new Singleton()` 在字节码层是"分配内存 → 构造 → 引用赋值"三步，普通引用下后两步可能重排，线程 B 拿到"非空但未构造完成"的实例，调用即崩：

```java
private static volatile Singleton instance;
public static Singleton getInstance() {
    if (instance == null) {                       // ① 快路径：无锁读
        synchronized (Singleton.class) {
            if (instance == null) {               // ② 慢路径：二次检查
                instance = new Singleton();       // ③ volatile 保证构造先于发布
            }
        }
    }
    return instance;
}
```

快路径的无锁读能安全执行，靠的正是 volatile 读的可见性语义——这也是"volatile 读不阻塞、DCL 性能好"的根源。

**volatile vs synchronized 选型**：只有"单写多读"或"标志位"语义时用 volatile（开销低、不阻塞）；需要"读-改-写"原子性或有复合操作时用锁或原子类。volatile 写入不阻塞，但读也不保证拿到最新值之外的任何新鲜度承诺——它只是"写后立即可见"，不是"读时必然最新"。

> 🎯 **核心要点**：synchronized 是现代 JVM 上开销可控的完整互斥方案（升级路径 + 锁消除）；volatile 只做可见性与有序性，不做原子性；虚拟线程时代 synchronized 的 pinning 已被 JEP 491 基本解决，但持锁阻塞路径仍建议换 ReentrantLock。

---

**下一模块**：[03 显式锁与 AQS 源码](03-显式锁与AQS源码.md)

**返回总览**：[00-总览](00-总览.md)
