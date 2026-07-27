# Java 语言核心名词剖析

> 🔤 深入理解 Java 语言层面的 60+ 核心概念 —— JUC 并发、集合框架、IO/NIO、泛型、注解、反射、异常与 JVM 基础交互

---

## 📚 目录

1. [基础语法与关键字](#1-基础语法与关键字)
2. [面向对象核心概念](#2-面向对象核心概念)
3. [集合框架](#3-集合框架)
4. [泛型](#4-泛型)
5. [注解](#5-注解)
6. [反射](#6-反射)
7. [异常体系](#7-异常体系)
8. [IO 与 NIO](#8-io-与-nio)
9. [JUC 并发编程](#9-juc-并发编程)
10. [Java 8+ 新特性速览](#10-java-8-新特性速览)

---

## 1. 基础语法与关键字

### 1.1 访问修饰符

| 修饰符 | 本类 | 同包 | 子类 | 其他包 | 说明 |
|--------|:--:|:--:|:--:|:---:|------|
| **private** | ✅ | ❌ | ❌ | ❌ | 最严格，仅本类可见 |
| **default** | ✅ | ✅ | ❌ | ❌ | 包级别访问（不写修饰符） |
| **protected** | ✅ | ✅ | ✅ | ❌ | 子类可访问 |
| **public** | ✅ | ✅ | ✅ | ✅ | 完全公开 |

### 1.2 关键关键字

| 关键字 | 含义 | 示例 |
|--------|------|------|
| **static** | 类级别成员，属于类而非实例 | `static int count = 0;` |
| **final** | 不可修改：类不可继承 / 方法不可重写 / 变量不可变 | `final String NAME = "Java";` |
| **abstract** | 抽象类/方法，必须被子类实现 | `abstract void execute();` |
| **synchronized** | 同步锁，保证线程安全 | `synchronized(this) { ... }` |
| **volatile** | 保证变量可见性，禁止指令重排 | `volatile boolean flag = false;` |
| **transient** | 序列化时忽略该字段 | `transient String password;` |
| **native** | 本地方法（C/C++ 实现） | `native void start0();` |

### 1.3 == vs equals vs hashCode

```java
// == 比较引用地址（基本类型比较值）
String a = new String("hello");
String b = new String("hello");
System.out.println(a == b);       // false ← 不同对象

// equals 比较内容（需重写）
System.out.println(a.equals(b));  // true  ← String 已重写 equals

// hashCode 约定：
// 1. equals 相等的对象，hashCode 必须相等
// 2. hashCode 相等的对象，equals 不一定相等（哈希冲突）
// 3. 重写 equals 必须重写 hashCode
```

| 方法 | 比较内容 | 用途 |
|------|---------|------|
| `==` | 引用地址（堆内存地址） | 判断是否同一对象 |
| `equals()` | 逻辑内容（需重写） | 判断业务上是否相等 |
| `hashCode()` | 哈希值（int） | HashMap/HashSet 定位桶位置 |

---

## 2. 面向对象核心概念

### 2.1 三大特性

```text
封装（Encapsulation）
  → 隐藏内部实现细节，暴露公共接口
  → private 字段 + public getter/setter
  → 好处：降低耦合、提高安全性

继承（Inheritance）
  → 子类复用父类的属性和方法
  → extends 关键字，单继承（一个类只能继承一个父类）
  → 好处：代码复用、建立类层次结构

多态（Polymorphism）
  → 同一方法调用，不同对象表现不同行为
  → 重写（Override）：子类覆盖父类方法，运行时多态
  → 重载（Overload）：同类同名不同参，编译时多态
```

```java
// 多态示例
List<String> list = new ArrayList<>();  // 编译类型 = List，运行类型 = ArrayList
list.add("hello");  // 调用 ArrayList 的实现

// 重写 vs 重载
class Parent {
    void method() { }           // 被重写
    void method(int x) { }      // 重载 1
    void method(String s) { }   // 重载 2
}
class Child extends Parent {
    @Override
    void method() { }           // 重写父类方法
}
```

### 2.2 接口 vs 抽象类

| 维度 | 抽象类（abstract class） | 接口（interface） |
|------|------------------------|-------------------|
| **继承** | 单继承 | 多实现（implements 多个接口） |
| **构造器** | 可以有 | 不能有 |
| **成员变量** | 任意类型 | 只能 public static final 常量 |
| **方法** | 可以有实现 | Java 8+ 可以有 default/static 方法 |
| **访问修饰符** | 可以各种 | 默认 public（Java 9+ 支持 private） |
| **设计意图** | "是什么"（is-a） | "能做什么"（can-do） |
| **使用场景** | 模板方法模式、代码复用 | 定义契约、解耦 |

---

## 3. 集合框架

### 3.1 集合体系全景

```
Collection (接口)
│
├── List (接口)                ← 有序、可重复
│   ├── ArrayList              ← 数组实现，查快 O(1)，增删慢 O(n)
│   ├── LinkedList             ← 双向链表，增删快 O(1)，查慢 O(n)
│   └── Vector                 ← 线程安全版 ArrayList（已过时）
│       └── Stack              ← LIFO 栈（已过时，用 Deque 替代）
│
├── Set (接口)                 ← 无序、不可重复
│   ├── HashSet                ← HashMap 实现，O(1)
│   ├── LinkedHashSet          ← 双向链表维护插入顺序
│   └── TreeSet                ← 红黑树实现，自然排序 O(log n)
│
└── Queue (接口)               ← 队列
    ├── LinkedList             ← 双向链表实现
    ├── PriorityQueue          ← 堆实现，优先级队列
    └── Deque (接口)           ← 双端队列
        └── ArrayDeque         ← 循环数组实现，推荐代替 Stack

Map (接口)                     ← 键值对
├── HashMap                    ← 数组+链表+红黑树，O(1)
├── LinkedHashMap              ← 维护插入/访问顺序
├── TreeMap                    ← 红黑树，Key 有序 O(log n)
├── Hashtable                  ← 线程安全版 HashMap（已过时）
└── ConcurrentHashMap          ← 分段锁/CAS，高并发场景
```

### 3.2 HashMap 核心原理

```java
// HashMap 数据结构：Node[] table
// 冲突解决：链表 → 红黑树（链表长度 >= 8 且 table.length >= 64 时树化）

// put 流程：
// 1. 计算 hash：hash = key.hashCode() ^ (key.hashCode() >>> 16)
// 2. 计算 index：index = hash & (table.length - 1)
// 3. 桶为空 → 直接插入
// 4. 桶不为空 → 链表/红黑树插入
// 5. size > threshold → resize() 扩容

// 关键参数
// 默认容量：16
// 负载因子：0.75
// 扩容阈值：capacity × 0.75
// 树化阈值：链表长度 8
// 链化阈值：树节点数 6
```

### 3.3 集合选择速查

| 场景 | 推荐集合 | 原因 |
|------|---------|------|
| 频繁随机访问 | `ArrayList` | O(1) get |
| 频繁增删（头部） | `LinkedList` / `ArrayDeque` | O(1) |
| 去重 + 快速查找 | `HashSet` | O(1) contains |
| 去重 + 保持顺序 | `LinkedHashSet` | 插入顺序 |
| 排序 + 去重 | `TreeSet` | 自然排序 |
| 键值对查找 | `HashMap` | O(1) get/put |
| 线程安全 Map | `ConcurrentHashMap` | 分段锁，高并发 |
| LRU 缓存 | `LinkedHashMap` | accessOrder=true |

---

## 4. 泛型

### 4.1 核心概念

```java
// 泛型类
class Box<T> {
    private T value;
    public T get() { return value; }
    public void set(T value) { this.value = value; }
}

// 泛型方法
public static <T> T getFirst(List<T> list) {
    return list.get(0);
}

// 通配符
List<?>           // 无界通配符，只读
List<? extends T> // 上界通配符，生产者（Producer Extends）
List<? super T>   // 下界通配符，消费者（Consumer Super）

// PECS 原则：Producer Extends, Consumer Super
// 从集合读取 → ? extends T
// 向集合写入 → ? super T
```

### 4.2 泛型擦除

```java
// Java 泛型是编译时检查，运行时类型被擦除
List<String> list1 = new ArrayList<>();
List<Integer> list2 = new ArrayList<>();
System.out.println(list1.getClass() == list2.getClass()); // true! 都是 ArrayList.class

// 泛型擦除后：T → Object（无界），T extends Number → Number（有界）
// 这解释了为什么不能 new T()、不能 instanceof T、静态字段不能引用泛型参数
```

---

## 5. 注解

### 5.1 内置注解 vs 元注解

| 注解 | 类型 | 说明 |
|------|:--:|------|
| `@Override` | 内置 | 标记方法重写 |
| `@Deprecated` | 内置 | 标记已过时 |
| `@SuppressWarnings` | 内置 | 抑制编译器警告 |
| `@FunctionalInterface` | 内置 | 标记函数式接口 |
| `@Target` | 元注解 | 限定注解使用位置（TYPE/METHOD/FIELD...） |
| `@Retention` | 元注解 | 限定注解保留阶段（SOURCE/CLASS/RUNTIME） |
| `@Inherited` | 元注解 | 子类是否继承注解 |
| `@Documented` | 元注解 | 是否包含在 javadoc 中 |

```java
// 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyLog {
    String value() default "";
    boolean printArgs() default true;
}
```

---

## 6. 反射

### 6.1 获取 Class 对象的三种方式

```java
// 1. Class.forName（最常用，动态加载）
Class<?> clazz1 = Class.forName("com.example.User");

// 2. 类名.class（编译时已知）
Class<?> clazz2 = User.class;

// 3. 实例.getClass()
Class<?> clazz3 = user.getClass();
```

### 6.2 反射核心 API

```java
Class<?> clazz = User.class;

// 构造对象
Constructor<?> ctor = clazz.getDeclaredConstructor(String.class);
Object obj = ctor.newInstance("John");

// 访问字段
Field field = clazz.getDeclaredField("name");
field.setAccessible(true);  // 暴力访问 private
String name = (String) field.get(obj);

// 调用方法
Method method = clazz.getDeclaredMethod("setName", String.class);
method.invoke(obj, "Jane");

// Spring IOC 的核心就是反射！
```

---

## 7. 异常体系

```
Throwable
├── Error                    ← 严重错误，程序不应处理
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── NoClassDefFoundError
│
└── Exception                ← 可处理的异常
    ├── RuntimeException     ← 非检查异常（unchecked）
    │   ├── NullPointerException
    │   ├── IndexOutOfBoundsException
    │   ├── IllegalArgumentException
    │   └── ClassCastException
    │
    └── 其他 Exception       ← 检查异常（checked，必须处理）
        ├── IOException
        ├── SQLException
        └── ClassNotFoundException

// 经典考题
try { ... }
catch (Exception e) { ... }
finally { ... }  // 总是执行（除非 System.exit()）
```

---

## 8. IO 与 NIO

| 维度 | BIO (Blocking IO) | NIO (Non-blocking IO) | AIO (Async IO) |
|------|:---------------:|:-------------------:|:------------:|
| **阻塞模式** | 阻塞 | 非阻塞（多路复用） | 异步回调 |
| **线程模型** | 1连接1线程 | 1线程管理多连接 | 回调通知 |
| **核心组件** | InputStream/OutputStream | Channel + Buffer + Selector | AsynchronousChannel |
| **性能** | 连接数多时性能差 | 高并发场景优秀 | 理论上最优 |
| **复杂度** | 简单 | 较复杂 | 复杂 |
| **框架** | 传统 Socket | Netty（基于 NIO） | - |

---

## 9. JUC 并发编程

### 9.1 核心组件速查

```
JUC = java.util.concurrent

├── 原子类（Atomic）
│   ├── AtomicInteger / AtomicLong / AtomicBoolean  ← CAS 无锁
│   ├── AtomicReference / AtomicStampedReference   ← ABA 问题解决
│   └── LongAdder                                    ← 高并发计数优于 AtomicLong
│
├── 锁（Locks）
│   ├── ReentrantLock          ← 可重入互斥锁，替代 synchronized
│   ├── ReentrantReadWriteLock ← 读写锁，读共享写互斥
│   ├── StampedLock            ← 乐观读锁，性能更好
│   └── Condition              ← await/signal，替代 wait/notify
│
├── 同步工具（Synchronizers）
│   ├── CountDownLatch         ← 等待 N 个任务完成
│   ├── CyclicBarrier          ← N 个线程互相等待到齐
│   ├── Semaphore              ← 信号量，限流
│   ├── Exchanger              ← 两个线程交换数据
│   └── Phaser                 ← 分阶段屏障
│
├── 并发集合（Concurrent Collections）
│   ├── ConcurrentHashMap      ← 分段锁/CAS
│   ├── CopyOnWriteArrayList   ← 写时复制，读多写少
│   ├── BlockingQueue          ← 阻塞队列（生产者-消费者）
│   │   ├── ArrayBlockingQueue    ← 有界
│   │   ├── LinkedBlockingQueue   ← 可选有界
│   │   ├── SynchronousQueue      ← 无容量，直接交付
│   │   └── DelayQueue            ← 延迟队列
│   └── ConcurrentLinkedQueue  ← 无锁非阻塞队列
│
└── 线程池（Executor Framework）
    ├── Executors.newFixedThreadPool(n)      ← 固定大小
    ├── Executors.newCachedThreadPool()      ← 弹性伸缩
    ├── Executors.newSingleThreadExecutor()  ← 单线程串行
    ├── Executors.newScheduledThreadPool(n)  ← 定时调度
    └── ThreadPoolExecutor（手动配置）        ← 推荐！
```

### 9.2 线程池核心参数

```java
ThreadPoolExecutor(int corePoolSize,      // 核心线程数
                   int maximumPoolSize,   // 最大线程数
                   long keepAliveTime,    // 空闲线程存活时间
                   TimeUnit unit,
                   BlockingQueue<Runnable> workQueue,  // 任务队列
                   ThreadFactory threadFactory,
                   RejectedExecutionHandler handler    // 拒绝策略
)

// 四种拒绝策略：
// AbortPolicy         → 抛异常（默认）
// CallerRunsPolicy    → 由调用线程执行
// DiscardPolicy       → 直接丢弃新任务
// DiscardOldestPolicy → 丢弃队列中最旧的任务

// 推荐：使用 ThreadPoolExecutor 手动配置，不用 Executors 快捷方法
// 原因：Executors 创建的队列可能无限大（OOM风险）
```

### 9.3 synchronized vs Lock

| 维度 | synchronized | Lock (ReentrantLock) |
|------|:----------:|:------------------:|
| **实现** | JVM 关键字，monitorenter/monitorexit | JDK 接口，AQS 实现 |
| **锁释放** | 自动释放（代码块结束/异常） | 必须 finally 中 unlock |
| **中断响应** | 不可中断 | lockInterruptibly() 可中断 |
| **超时获取** | 不支持 | tryLock(time, unit) |
| **公平锁** | 非公平 | 可选公平 |
| **条件变量** | wait/notify（单条件） | Condition（多条件） |
| **性能** | Java 6+ 优化后差距很小 | 略好（高竞争时） |

---

## 10. Java 8+ 新特性速览

| 版本 | 关键特性 |
|:--:|---------|
| **Java 8** | Lambda、Stream、Optional、CompletableFuture、新日期API |
| **Java 9** | 模块系统(Jigsaw)、集合工厂方法、私有接口方法 |
| **Java 11** | var 局部变量、HttpClient、String 新方法(LTS) |
| **Java 14** | Record 类型（预览）、Switch 表达式 |
| **Java 17** | Sealed Classes、Pattern Matching (LTS) |
| **Java 21** | Virtual Threads（协程）、Record Patterns (LTS) |

```java
// Java 21 虚拟线程（协程）—— 颠覆性的并发模型
Thread.startVirtualThread(() -> {
    // 轻量级线程，无需池化管理
    // 阻塞操作不会消耗 OS 线程
});

// 对比：平台线程 ~1MB 栈空间，虚拟线程 ~几KB
// 可以创建百万级虚拟线程！
```

---

> 🎯 **一句话总结**：Java 语言层是后端开发的基石，掌握集合（HashMap 原理）、泛型（类型擦除）、反射（Spring 基石）、并发（JUC 全家桶）是面试和日常开发的基本功。

---

**下一模块**：[02-JVM虚拟机名词剖析](./02-JVM虚拟机名词剖析.md) → 深入 JVM 内存模型与 GC

---

*创建于：2026年7月*
