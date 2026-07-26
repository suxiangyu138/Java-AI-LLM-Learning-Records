# 12 - JVM原理与性能调优

> 类加载、内存模型、GC 算法、JIT 编译、调优实战，攻克 Java 面试中最难的一关。

---

## 目录

1. [JVM 整体架构](#1-jvm-整体架构)
2. [类加载机制](#2-类加载机制)
3. [运行时数据区](#3-运行时数据区)
4. [垃圾回收算法](#4-垃圾回收算法)
5. [垃圾回收器详解](#5-垃圾回收器详解)
6. [GC 日志与工具](#6-gc-日志与工具)
7. [JIT 编译与优化](#7-jit-编译与优化)
8. [JVM 调优实战](#8-jvm-调优实战)
9. [OOM 排查全流程](#9-oom-排查全流程)
10. [常见面试题深度解析](#10-常见面试题深度解析)

---

## 1. JVM 整体架构

### 1.1 架构全景图

```
┌────────────────────────────────────────────────────────────────┐
│                         JVM 架构                                │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐                                           │
│  │     类加载器子系统  │  Loading → Linking → Initialization      │
│  │  Bootstrap / Ext / App / Custom                             │
│  └────────┬─────────┘                                           │
│           │                                                     │
│           ▼                                                     │
│  ┌────────────────────────────────────────────────────────────┐│
│  │                   运行时数据区 (Runtime Data Areas)          ││
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     ││
│  │  │     堆        │  │   方法区      │  │   直接内存    │     ││
│  │  │   (Heap)     │  │ (Metaspace)  │  │ (Direct     │     ││
│  │  │  线程共享     │  │  线程共享     │  │  Memory)    │     ││
│  │  └──────────────┘  └──────────────┘  └──────────────┘     ││
│  │  ┌──────┐ ┌──────┐ ┌──────────┐                           ││
│  │  │虚拟机栈│ │本地方法│ │ 程序计数器 │  线程私有               ││
│  │  │(Stack)│ │  栈    │ │ (PC)      │                       ││
│  │  └──────┘ └──────┘ └──────────┘                           ││
│  └────────────────────────────────────────────────────────────┘│
│           │                                                     │
│           ▼                                                     │
│  ┌────────────────────────────────────────────────────────────┐│
│  │                      执行引擎                               ││
│  │  解释器 (Interpreter)  ←→  JIT 编译器 (C1/C2)               ││
│  │  GC (垃圾回收器)                                             ││
│  └────────────────────────────────────────────────────────────┘│
│           │                                                     │
│           ▼                                                     │
│  ┌────────────────────────────────────────────────────────────┐│
│  │              本地方法接口 (JNI) + 本地方法库                  ││
│  └────────────────────────────────────────────────────────────┘│
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

---

## 2. 类加载机制

### 2.1 类加载过程

```
类生命周期（7 个阶段）：

Loading（加载）：
  1. 通过全限定类名获取二进制字节流
  2. 将字节流转为方法区的运行时数据结构
  3. 在堆中生成 Class 对象作为入口

Linking（链接）：
  Verification（验证）：字节码安全性校验
  Preparation（准备）：为静态变量分配内存 + 赋默认值
                       public static int value = 123;
                       → 准备阶段 value = 0（不是 123！）
  Resolution（解析）：将常量池符号引用 → 直接引用

Initialization（初始化）：
  执行 <clinit>() 方法
  静态变量赋实际值 + 静态代码块执行
  value = 123（这个阶段赋值）

Using（使用）
Unloading（卸载）：类可以被 GC 卸载的条件：
  - 所有实例被回收
  - 类加载器被回收
  - Class 对象没有被引用

触发初始化的 6 种情况（主动使用）：
  1. new / getstatic / putstatic / invokestatic 指令
  2. 反射调用
  3. 初始化子类 → 先初始化父类
  4. 包含 main() 方法的类
  5. MethodHandle 调用
  6. default 方法所在接口的初始化
```

### 2.2 类加载器

```
双亲委派模型（Parents Delegation Model）：

Bootstrap ClassLoader（启动类加载器）：
  - C/C++ 实现，JVM 一部分
  - 加载 <JAVA_HOME>/lib（rt.jar、java.base 模块等）
  - 加载不到返回 null

Extension / Platform ClassLoader：
  - Java 8: sun.misc.Launcher$ExtClassLoader（加载 lib/ext）
  - Java 9+: PlatformClassLoader（加载 java.platform 模块）

Application ClassLoader（应用类加载器）：
  - 加载 classpath 上的类
  - 我们写的类都是它加载的

双亲委派机制：
  加载一个类 → 先问父加载器能不能加载
  → 能加载就父加载器加载
  → 不能加载才自己加载

为什么需要双亲委派？
  - 防止重复加载（核心类如 String 统一由 Bootstrap 加载）
  - 防止核心类被篡改（你写的 java.lang.String 不会被加载）

破坏双亲委派的场景：
  - JDBC：通过 SPI 加载驱动（线程上下文类加载器）
  - Tomcat：WebApp 隔离（每个应用独立类加载器）
  - OSGi：网状类加载器结构
```

---

## 3. 运行时数据区

### 3.1 各区域详解

```
堆（Heap）- 线程共享：
  占据 JVM 内存的最大部分
  几乎所有的对象实例都分配在这里
  -Xms（初始大小） -Xmx（最大大小）
  
  分代结构（G1 之前）：
  ┌────────────────────────────────┐
  │ 新生代（Young）    占堆 1/3     │
  │  ├── Eden          占新生代 80%│ ← 新对象分配
  │  ├── Survivor 0    占新生代 10%│
  │  └── Survivor 1    占新生代 10%│
  ├────────────────────────────────┤
  │ 老年代（Old/Tenured）占堆 2/3   │ ← 长期存活的对象
  └────────────────────────────────┘
  GC 后 Eden 存活对象 → Survivor（互换），
  年龄（GC次数）超过阈值 → 晋升老年代

方法区（Method Area = Metaspace，JDK 8+）- 线程共享：
  存储：类信息、运行时常量池、静态变量、JIT 代码缓存
  JDK 7：PermGen（永久代，在堆外但大小固定 -XX:MaxPermSize）
  JDK 8+：Metaspace（元空间，本地内存，-XX:MaxMetaspaceSize）
  元空间的好处：默认无限大（受系统内存限制），减少 OOM

虚拟机栈（VM Stack）- 线程私有：
  每个方法执行 → 创建栈帧（Stack Frame）
  栈帧包含：
    - 局部变量表（Local Variable Table）：基本类型值 + 对象引用
    - 操作数栈（Operand Stack）：计算的中间结果
    - 动态连接（Dynamic Linking）：指向运行时常量池的引用
    - 方法返回地址
  -Xss 设置栈大小（默认 1M）
  StackOverflowError：栈深度超过限制（如递归太深）

程序计数器（PC Register）- 线程私有：
  指向当前线程执行的字节码行号
  分支/循环/跳转/异常处理都依赖它
  Native 方法时值为 undefined

本地方法栈（Native Method Stack）- 线程私有：
  为 Native 方法服务
  HotSpot 中与虚拟机栈合二为一

直接内存（Direct Memory）- 非运行时数据区：
  NIO 的 DirectByteBuffer 使用的内存
  -XX:MaxDirectMemorySize（默认等于 -Xmx）
  堆外内存不受 GC 管理（但 DirectByteBuffer 被回收时会通过 Cleaner 释放）
```

### 3.2 对象创建过程

```
创建对象的内存分配方式：

1. 指针碰撞（Bump the Pointer）：
   堆内存规整 → 指针一边是已分配，一边是空闲
   分配 = 指针移动对象大小距离
   适用：Serial、ParNew（带压缩整理的 GC）

2. 空闲列表（Free List）：
   堆内存不规整 → 维护一个空闲块列表
   分配 = 从列表中找一个够大的块
   适用：CMS（标记-清除算法）

分配优化：
  TLAB（Thread Local Allocation Buffer）：
  每个线程在 Eden 区独占一小块空间（默认 Eden 的 1%）
  线程内分配对象直接在 TLAB 上（无锁，CAS 分配）
  只有 TLAB 用完时才需要在 Eden 区同步分配

逃逸分析（Escape Analysis）：
  分析对象的作用域，如果只在一个方法内使用（未逃逸）
  → 可以在栈上分配（随栈帧销毁而回收，不用 GC）
  → 可以进行标量替换（对象拆成基本类型成员放栈上）
```

---

## 4. 垃圾回收算法

### 4.1 四种基础算法

```
标记-清除（Mark-Sweep）：
  Mark：从 GC Roots 出发，标记所有可达对象
  Sweep：清除未标记的对象
  优点：简单
  缺点：产生内存碎片、效率不高

标记-复制（Mark-Copy）：
  将内存分为两块，只用一块
  GC 时把存活对象复制到另一块，清空当前块
  优点：无碎片、效率高
  缺点：内存浪费 50%
  用于：新生代（Eden → Survivor 互换，存活率低）

标记-整理（Mark-Compact）：
  Mark：标记存活对象
  Compact：把所有存活对象移到一端，清除边界外
  优点：无碎片、无空间浪费
  缺点：移动对象需要更新引用、STW 时间较长
  用于：老年代（存活率高）

分代收集（Generational Collection）：
  新生代：标记-复制（对象存活率低，复制开销小）
  老年代：标记-清除 / 标记-整理（对象存活率高，复制开销大）
  这是将前三种算法组合使用的策略
```

### 4.2 GC Roots 有哪些？

```
什么是 GC Roots？
  GC Roots 是一组必须活跃的引用
  从 GC Roots 出发，沿引用链可以到达的对象 → 存活
  不可达的对象 → 需要回收

GC Roots 包括：
  1. 虚拟机栈（栈帧中的局部变量表）引用的对象
  2. 方法区中静态属性引用的对象
  3. 方法区中常量引用的对象（运行时常量池）
  4. 本地方法栈中 JNI 引用的对象
  5. JVM 内部引用（类、基本类型包装类、常驻异常对象）
  6. 被 synchronized 持有的对象
  7. JMXBean、JVMTI 回调、本地代码缓存等
  8. 特定 GC 的内部结构引用
```

### 4.3 四种引用类型

```java
// 强引用（Strong Reference）
Object obj = new Object();
// 只要强引用存在，绝对不会被 GC 回收

// 软引用（Soft Reference）
SoftReference<Object> softRef = new SoftReference<>(new Object());
// 内存足够时不回收，内存不足（OOM 前）时回收
// 适用：缓存（在 OOM 前自动释放）

// 弱引用（Weak Reference）
WeakReference<Object> weakRef = new WeakReference<>(new Object());
// 下一次 GC 时必定回收
// 适用：ThreadLocal 的 key、WeakHashMap

// 虚引用（Phantom Reference）
PhantomReference<Object> phantomRef = new PhantomReference<>(obj, queue);
// 无法通过虚引用获取对象引用
// 配合 ReferenceQueue 记录对象被回收的时间
// 适用：堆外内存释放（DirectByteBuffer 的 Cleaner）
```

---

## 5. 垃圾回收器详解

### 5.1 经典 GC 组合

```
新生代回收器 + 老年代回收器：

组合 1：Serial + Serial Old
  单线程，适合客户端、小内存（100MB 以下）
  不用（但简单微服务可选，内存小 GC 停顿反而短）

组合 2：Parallel Scavenge + Parallel Old（JDK 8 默认）
  多线程，吞吐量优先
  目标：最小化 GC 时间占总运行时间的比例
  适合：后台计算、批处理
  不适合：Web 应用（单次停顿时间长）

组合 3：ParNew + CMS（JDK 8 经典 Web 组合）
  CMS：并发标记清除，最短停顿时间为目标
  JDK 14 已废弃移除
  问题：并发模式失败 → 退化为 Serial Old（单线程，极慢）

组合 4：G1 Garbage First（JDK 9+ 默认）
  兼顾吞吐量和低延迟
  可预测停顿时间

组合 5：ZGC（JDK 11 实验，17+ 成熟）
  亚毫秒级停顿，TB 级堆
  染色指针 + 读屏障

组合 6：Shenandoah
  与 ZGC 类似
  Red Hat 维护
```

### 5.2 G1 详细解析

```
G1 核心思想：
  不再固定分代大小
  将堆划分为大小相等的 Region（1~32MB，2048 个）
  Region 可以是：Eden / Survivor / Old / Humongous

  回收时优先回收垃圾最多的 Region（Garbage First）

G1 GC 周期：

Young GC：
  只回收新生代 Region
  STW（Stop-The-World），并行回收

Mixed GC：
  回收所有新生代 + 部分老年代 Region
  是 G1 独有的！
  STW，分多个阶段

Full GC（已尽全力，后备方案）：
  Serial Old 单线程，STW
  不应该出现！应通过调优避免

G1 特点：
  1. 可预测停顿：-XX:MaxGCPauseMillis=200
  2. 分 Region，不要求连续内存
  3. 优先回收垃圾多的 Region
  4. 并发标记阶段：与应用程序并发执行
  5. 通过 Remembered Set（RSet）维护跨 Region 引用
  6. SATB（Snapshot-At-The-Beginning）算法做并发标记
```

### 5.3 ZGC 简介

```
ZGC（Z Garbage Collector）：
  JDK 11 首批实验支持
  JDK 15 生产可用
  JDK 17 成熟（Windows 支持也是 JDK 17 加入的）

核心特点：
  - 最大停顿 < 1ms（亚毫秒级！）
  - 支持 8MB ~ 16TB 堆
  - 并发回收所有阶段（连移动对象都是并发的！）

核心技术：染色指针（Colored Pointers）
  在 64 位指针中借用几个 bit 存储 GC 元数据
  不需要额外的对象头或侧表
  限制了可用内存（但 16TB 足够用）

适用场景：
  超大堆（百 GB 级别）
  对延迟极度敏感的 Web 服务
  替代 G1 的下一代选择

GC 类型选择建议：
  小内存（< 4G）→ Serial / Parallel
  中等内存（4G-32G）→ G1（默认）
  大内存（> 32G, 低延迟）→ ZGC
  注意：堆 > 32G 时压缩指针失效，慎提！
```

---

## 6. GC 日志与工具

### 6.1 GC 日志配置

```bash
# Java 8
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log

# Java 9+ 统一日志
-Xlog:gc*:file=gc.log:time,level,tags:filecount=10,filesize=100m

# 推荐配置
java \
  -Xms4g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Xlog:gc*=info:file=logs/gc.log:time,uptime,level,tags:filecount=10,filesize=100M \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=logs/heap.hprof \
  -jar app.jar
```

### 6.2 诊断工具

```bash
# === JDK 自带命令 ===

# 查看 Java 进程
jps -lvm

# GC 统计（实时）
jstat -gc <pid> 1000 10     # 每秒输出一次，共 10 次
jstat -gcutil <pid> 1000    # 显示各区域使用百分比

# 堆转储（内存快照）
jmap -dump:format=b,file=heap.hprof <pid>
jmap -histo <pid>           # 堆中各类对象数量（快速，不 STW）
jcmd <pid> GC.heap_dump heap.hprof

# 线程栈
jstack <pid>                # 所有线程栈
jstack <pid> > thread.dump
# 查死锁：输出文件末尾有 Found one Java-level deadlock

# JVM 参数/配置
jinfo <pid>                 # JVM 配置信息
jcmd <pid> VM.flags         # 实际生效的 JVM 参数

# === 图形化工具 ===

# JConsole：
  JDK 自带的监控工具
  内存、线程、类、MBean

# VisualVM：
  多合一可视化工具
  CPU Profiling、内存 Profiling
  需要插件

# Arthas（阿里，线上诊断神器）：
  curl -O https://arthas.aliyun.com/arthas-boot.jar
  java -jar arthas-boot.jar
  
  dashboard          → 实时看板（内存/GC/线程/CPU）
  thread -b          → 查找死锁
  thread -n 3        → Top 3 CPU 线程
  trace Class method → 追踪方法调用耗时
  watch Class method → 监控方法入参/返回值
  jad Class          → 反编译线上代码
  heapdump           → 堆转储
  profiler start     → 火焰图（CPU Profiling）

# MAT（Memory Analyzer Tool）：
  Eclipse MAT：分析 heap dump 文件
  功能：检测内存泄漏、找出大对象、对象引用链
```

---

## 7. JIT 编译与优化

### 7.1 JIT 编译原理

```
解释执行 vs 编译执行：
  解释器：边解释边执行，启动快，执行慢
  JIT：热点代码编译为本地代码，启动慢（需要编译），执行快

HotSpot（热点代码检测）：
  超过一定次数的循环 → 编译
  超过一定次数的方法调用 → 编译
  使用计数器统计

编译器：
  C1（Client Compiler）：
    编译快，优化程度低
    适合：GUI 应用、对启动时间敏感
  C2（Server Compiler）：
    编译慢，深度优化
    适合：长时间运行的服务端应用
  分层编译（Tiered Compilation，JDK 7+ 默认）：
    Level 0: 解释执行
    Level 1: C1 无 profiling
    Level 2: C1 带简单 profiling
    Level 3: C1 带完整 profiling
    Level 4: C2 深度优化
    结合 C1 的快速编译和 C2 的深度优化
```

### 7.2 常见 JIT 优化技术

```
方法内联（Method Inlining）：
  将方法调用替换为方法体
  消除方法调用开销 → 为进一步优化铺路
  private int add(int a, int b) { return a + b; }
  调用处：int c = add(1, 2);  →  int c = 1 + 2;

逃逸分析（Escape Analysis）：
  分析对象的动态作用域
  → 不逃逸 → 栈上分配 / 标量替换
  → 同步消除（逃逸对象不加锁）

锁消除（Lock Elimination）：
  检测到某个锁对象不会被其他线程获取 → 去掉锁

锁粗化（Lock Coarsening）：
  连续的加锁-解锁 → 合并为一次大的锁

标量替换（Scalar Replacement）：
  class Point { int x; int y; }
  不创建 Point 对象，直接分配 x 和 y 在栈上

空值检查消除：
  重复的空值检查去掉

代码提升（Code Hoisting）：
  循环不变表达式提到循环外
```

---

## 8. JVM 调优实战

### 8.1 调优步骤

```
1. 明确目标：
   低延迟？→ G1/ZGC，MaxGCPauseMillis=100
   高吞吐？→ Parallel，关注 GC 时间占比
   小内存？→ Serial，少即是快

2. 选择 GC：
   8G 以下 → G1（默认，简单）
   4G 以下 → Parallel（吞吐优先）/ Serial（小应用）
   16G 以上 + 低延迟 → ZGC

3. 设置堆大小：
   -Xms = -Xmx（避免动态伸缩开销）
   预留 30% 系统内存给 OS + 堆外内存

4. 设置元空间：
   -XX:MaxMetaspaceSize=256m（防止无限增长）

5. 开启 GC 日志：
   -Xlog:gc*:file=gc.log

6. 设置 GC 相关参数：
   G1: -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=4m
   ZGC: -XX:+UseZGC -XX:+ZGenerational（JDK 21+）
   Parallel: -XX:MaxGCPauseMillis=200

7. 压测 + 观察：
   监控 GC 频率、每次停顿时间、内存使用趋势
   根据 GC 日志调整参数

8. 重复 5-7 直到满意
```

### 8.2 常见参数速查表

```
堆设置：
  -Xms2g -Xmx2g             初始/最大堆（建议相等）
  -Xss256k                  线程栈大小
  -XX:NewRatio=2            老年代:新生代 = 2:1
  -XX:SurvivorRatio=8       Eden:Survivor = 8:1
  -XX:MaxMetaspaceSize=256m 最大元空间

GC 选择：
  -XX:+UseG1GC              G1（JDK 9+ 默认）
  -XX:+UseZGC               ZGC
  -XX:+UseParallelGC        Parallel

G1 专用：
  -XX:MaxGCPauseMillis=200  预期停顿目标（ms）
  -XX:G1HeapRegionSize=4m   Region 大小（1~32M）
  -XX:InitiatingHeapOccupancyPercent=45  触发 Mixed GC 的老年代占比
  -XX:G1NewSizePercent=5    新生代最小占比
  -XX:G1MaxNewSizePercent=60 新生代最大占比

ZGC 专用：
  -XX:SoftMaxHeapSize=4g    软最大堆（压缩指针在 4G 以下更省内存）

GC 日志：
  -Xlog:gc*:file=gc.log

OOM 应急：
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/path/to/dump.hprof
  -XX:+ExitOnOutOfMemoryError      直接终止进程（便于 K8s 重启）
  -XX:+CrashOnOutOfMemoryError     产生 core dump

其它：
  -XX:+PrintCommandLineFlags       打印 JVM 参数
  -XX:+DisableExplicitGC           禁用 System.gc()
  -Djava.security.egd=file:/dev/./urandom  加快随机数生成（容器内）
```

---

## 9. OOM 排查全流程

### 9.1 常见 OOM 类型

```
java.lang.OutOfMemoryError: Java heap space
  堆内存不足
  原因：堆设置太小 / 内存泄漏 / 数据量过大

java.lang.OutOfMemoryError: Metaspace
  元空间不足
  原因：动态生成类太多（CGLIB、Groovy、大量反射）

java.lang.OutOfMemoryError: GC overhead limit exceeded
  GC 太频繁但回收很少：98% 时间 GC 但回收不到 2% 内存
  原因：堆太小 / 内存泄漏末期

java.lang.OutOfMemoryError: Direct buffer memory
  堆外内存（NIO）不足
  解决：增大 MaxDirectMemorySize 或排查泄漏

java.lang.OutOfMemoryError: Unable to create new native thread
  无法创建新线程（通常不是内存不足，而是线程数超系统限制）
  解决：降低线程数 / 调整 ulimit -u

java.lang.StackOverflowError
  栈溢出
  原因：递归太深 / 循环依赖调用
```

### 9.2 排查流程

```
1. 保留现场：-XX:+HeapDumpOnOutOfMemoryError

2. MAT 分析 heap dump：
   a. Leak Suspects Report
      → 检查是否内存泄漏
   b. Histogram
      → 哪个类的实例最多
   c. Dominator Tree
      → 哪个对象占内存最大
   d. Reference Chains
      → 引用链到 GC Roots
      → 为什么这个对象没有被回收？

3. 线程 dump 分析（jstack）：
   a. 是否有大量线程 Blocked/Waiting
   b. 是否有死锁（Found one Java-level deadlock）
   c. 是否有线程泄漏（大量同名线程）

4. GC 日志分析：
   a. Full GC 频率
   b. GC 前后内存变化
   → 对象增长率 > GC 回收率 → 会 OOM

5. 代码分析：
   a. 缓存（Map、List）是否无限增长
   b. 线程池是否正确关闭
   c. ThreadLocal 是否 remove
   d. 资源（连接、流）是否正确关闭

6. 容器环境：
   docker stats → 看容器内存是否超限
   kubectl top pod → 看 K8s 资源限制
```

---

## 10. 常见面试题深度解析

### Q1: 对象从创建到消亡的完整过程？

```
1. 类加载检查 → 类是否已加载（没有则类加载）
2. 分配内存 → 栈上分配（逃逸分析+标量替换）/ TLAB / Eden
3. 初始化零值 → 成员变量被赋默认值（0/null/false）
4. 设置对象头 → Mark Word（hash/GC年龄/锁状态）+ 类型指针
5. 执行 <init> → 构造方法执行，字段赋实际值

对象晋升老年代：
  - 年龄超过 MaxTenuringThreshold（默认 15）
  - Survivor 区相同年龄的对象大小 > Survivor 的 50% → 该年龄及以上全部晋升
  - 大对象直接进入老年代（-XX:PretenureSizeThreshold）
  - GC 后 Survivor 放不下 → 直接进老年代

对象回收：
  - 被标记为可回收 → 被 finalize()（JDK 18 已移除） → GC 回收
```

### Q2: Full GC 触发条件？

```
1. System.gc()（通过 -XX:+DisableExplicitGC 禁用）
2. 老年代空间不足
3. Metaspace 空间不足
4. 空间分配担保失败（Minor GC 前判断老年代剩余空间不够）
5. CMS/G1 并发模式失败（并发回收速度跟不上分配速度 → 退化为 Full GC）
6. jmap -histo:live 触发

尽量避免 Full GC：
  - G1 下：调整 Mixed GC 触发阈值（IHOP）
  - 避免大对象频繁分配
  - 合理设置堆大小和新生代比例
```

### Q3: G1 和 CMS 的区别？

```
1. 内存布局
   CMS：连续的老年代
   G1：Region 化（更灵活）

2. 回收方式
   CMS：只回收老年代
   G1：Mixed GC 同时回收新生代和部分老年代

3. 停顿模型
   CMS：不可预测
   G1：可预测（MaxGCPauseMillis）

4. 碎片处理
   CMS：标记-清除 → 碎片多（降级 Serial Old 时整理）
   G1：标记-复制整理 → 无碎片

5. 并发标记实现
   CMS：增量更新（Incremental Update）
   G1：SATB（Snapshot-At-The-Beginning）

6. 大对象
   CMS：直接老年代
   G1：Humongous Region（连续多个 Region）

7. 内存占用
   CMS：较低
   G1：RSet 额外占用内存（约 5% 堆）

8. 当前状态
   CMS：JDK 14 已移除
   G1：JDK 9+ 默认，活跃维护
```

> **上一篇：** [11-DevOps与云原生部署](./11-DevOps与云原生部署.md)
>
> **下一篇：** [13-企业级设计模式与架构](./13-企业级设计模式与架构.md)
