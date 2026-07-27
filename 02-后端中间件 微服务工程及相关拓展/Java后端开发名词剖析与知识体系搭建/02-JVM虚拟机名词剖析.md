# JVM 虚拟机名词剖析

> 🧠 JVM 内存模型、类加载机制、垃圾回收算法、调优工具链 —— 从原理到实战的 40+ 核心概念

---

## 📚 目录

1. [JVM 整体架构](#1-jvm-整体架构)
2. [类加载机制](#2-类加载机制)
3. [运行时数据区](#3-运行时数据区)
4. [垃圾回收 GC](#4-垃圾回收-gc)
5. [GC 算法详解](#5-gc-算法详解)
6. [常用 GC 收集器](#6-常用-gc-收集器)
7. [JVM 调优参数](#7-jvm-调优参数)
8. [JVM 调优工具](#8-jvm-调优工具)

---

## 1. JVM 整体架构

```text
┌─────────────────────────────────────────────────┐
│                  JVM 整体架构                     │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────────────────────────────┐       │
│  │         类加载子系统                   │       │
│  │   Bootstrap → Extension → Application│       │
│  │   加载 → 链接(验证+准备+解析) → 初始化│       │
│  └──────────────────────────────────────┘       │
│                    │                            │
│  ┌─────────────────┴─────────────────┐         │
│  │         运行时数据区                │         │
│  │  ┌──────┬──────┬──────────────┐   │         │
│  │  │ 堆   │方法区│   Java栈     │   │         │
│  │  │Heap  │Meta  │   VM Stack   │   │         │
│  │  ├──────┤Space ├──────────────┤   │         │
│  │  │  │   │      │ 本地方法栈    │   │         │
│  │  │  │   │      │ Native Stack │   │         │
│  │  │  │   │      ├──────────────┤   │         │
│  │  │  │   │      │  程序计数器   │   │         │
│  │  │  │   │      │  PC Register │   │         │
│  │  └──────┴──────┴──────────────┘   │         │
│  └────────────────────────────────────┘         │
│                    │                            │
│  ┌─────────────────┴─────────────────┐         │
│  │         执行引擎                    │         │
│  │   解释器 → JIT编译器 → GC         │         │
│  └────────────────────────────────────┘         │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 2. 类加载机制

### 2.1 类加载生命周期

```text
加载(Loading) → 链接(Linking) → 初始化(Initialization) → 使用 → 卸载

  链接 = 验证(Verify) + 准备(Prepare) + 解析(Resolve)

  验证：确保 Class 文件字节流符合 JVM 规范
  准备：为 static 变量分配内存并设零值
        static int x = 123;  ← 准备阶段 x=0（仅零值！）
        解析：将符号引用替换为直接引用
```

### 2.2 类加载器层次

```text
Bootstrap ClassLoader（启动类加载器）
  → 加载 JAVA_HOME/lib 下的核心类库（rt.jar, modules）
  → C/C++ 实现，Java 中返回 null

Platform ClassLoader（平台类加载器，Java 9+）
  → 加载 JAVA_HOME/lib/ext 或 java.ext.dirs

Application ClassLoader（应用类加载器）
  → 加载 classpath 上的类

双亲委派模型（Parent Delegation）：
  1. 收到加载请求 → 先委托父加载器
  2. 父加载器能找到 → 返回
  3. 父加载器找不到 → 自己尝试加载
  好处：避免核心类被重复加载/篡改（如自定义 java.lang.String）
```

### 2.3 打破双亲委派

```java
// Tomcat 为什么要打破双亲委派？
// → 隔离不同 Web 应用的类（不同版本的库）
// → 每个 WebApp 有自己的 WebappClassLoader

// SPI 机制（ServiceLoader）：父加载器调用子加载器
// JDBC 驱动加载就是典型案例
// DriverManager 由 Bootstrap 加载，但具体驱动由 AppClassLoader 加载
```

---

## 3. 运行时数据区

### 3.1 五大区域详解

| 区域 | 线程共享 | 存储内容 | 异常 | 说明 |
|------|:-----:|------|------|------|
| **堆 Heap** | ✅ 共享 | 对象实例、数组 | OOM | GC 主战场，分年轻代+老年代 |
| **方法区** | ✅ 共享 | 类信息、常量、静态变量、JIT代码 | OOM | Java 8 后用元空间(MetaSpace)实现 |
| **Java栈** | ❌ 私有 | 局部变量表、操作数栈、帧数据 | StackOverflow/OOM | 每个方法调用 = 一个栈帧 |
| **本地方法栈** | ❌ 私有 | native 方法信息 | StackOverflow/OOM | HotSpot 中与 Java 栈合二为一 |
| **程序计数器** | ❌ 私有 | 当前线程执行字节码行号 | 无 | 唯一不抛 OOM 的区域 |

### 3.2 堆内存结构

```text
Java 8+ 堆内存结构：

  ┌───────────────────────────────────────┐
  │              堆 Heap                   │
  │  ┌─────────────────┬────────────────┐ │
  │  │    新生代 Young   │  老年代 Old    │ │
  │  │  ┌───┬───┬─────┐ │                │ │
  │  │  │Eden│S0 │ S1  │ │                │ │
  │  │  │8  │ 1 │  1  │ │                │ │
  │  │  └───┴───┴─────┘ │                │ │
  │  └─────────────────┴────────────────┘ │
  │  默认比例：Young:Old = 1:2             │
  │  Eden:S0:S1 = 8:1:1                   │
  └───────────────────────────────────────┘

  元空间 MetaSpace（替代永久代 PermGen）
  → 使用本地内存（非堆内存）
  → 默认无上限（受物理内存限制）
  → -XX:MaxMetaspaceSize=256m 设置上限
```

---

## 4. 垃圾回收 GC

### 4.1 判断对象存活

```text
引用计数法：
  → 对象被引用一次 +1，引用失效 -1，为 0 时回收
  → 问题：无法解决循环引用（A→B, B→A）

可达性分析（Java 采用）：
  → 从 GC Roots 出发，通过引用链向下搜索
  → 不可达的对象 = 可回收

GC Roots 包括：
  ├── Java 栈中引用的对象
  ├── 方法区静态属性引用的对象
  ├── 方法区常量引用的对象
  ├── 本地方法栈 JNI 引用的对象
  └── 被 synchronized 持有的对象
```

### 4.2 四大引用类型

| 引用类型 | 回收时机 | 用途 |
|---------|---------|------|
| **强引用 Strong** | 永不回收（除非不可达） | 普通 new 对象 |
| **软引用 Soft** | 内存不足时回收 | 缓存、图片缓存 |
| **弱引用 Weak** | 下一次 GC 必回收 | WeakHashMap、ThreadLocal |
| **虚引用 Phantom** | 任何时候，仅跟踪回收 | 管理直接内存（NIO Buffer） |

```java
// 软引用示例
SoftReference<byte[]> cache = new SoftReference<>(new byte[10*1024*1024]);
byte[] data = cache.get();  // 可能为 null（已被回收）

// 弱引用示例
WeakReference<String> weak = new WeakReference<>(new String("hello"));
System.gc();
System.out.println(weak.get());  // null
```

---

## 5. GC 算法详解

| 算法 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **标记-清除** | 标记存活 → 清除未标记 | 简单 | 内存碎片 |
| **标记-复制** | 存活对象复制到新空间 → 清空旧空间 | 无碎片、效率高 | 浪费一半空间 |
| **标记-整理** | 标记存活 → 移动到一起 → 清理边界外 | 无碎片 | 移动成本高(STW长) |

```text
分代收集策略（Generational Collection）：

  新生代（Young GC / Minor GC）：
    → 对象朝生夕死，存活率低
    → 标记-复制算法（Eden→S0→S1 互倒）
    → 频繁但快速

  老年代（Old GC / Major GC / Full GC）：
    → 对象存活率高
    → 标记-清除 或 标记-整理
    → 低频但耗时（STW 长）

对象晋升老年代的条件：
  1. 年龄超过 -XX:MaxTenuringThreshold（默认15）
  2. Survivor 空间中同龄对象超过一半 → 动态年龄计算
  3. 大对象直接进入老年代（-XX:PretenureSizeThreshold）
```

---

## 6. 常用 GC 收集器

### 6.1 经典收集器

| 收集器 | 位置 | 算法 | 特点 | 适用 |
|--------|:---:|------|------|------|
| **Serial** | 新生代 | 标记-复制 | 单线程，STW | 客户端/小堆 |
| **ParNew** | 新生代 | 标记-复制 | 多线程 Serial | 配合 CMS |
| **Parallel Scavenge** | 新生代 | 标记-复制 | 吞吐量优先 | 后台计算 |
| **Serial Old** | 老年代 | 标记-整理 | 单线程 | 客户端 |
| **CMS** | 老年代 | 标记-清除 | 低延迟 | 响应优先 |
| **Parallel Old** | 老年代 | 标记-整理 | 多线程，吞吐量优先 | 配合 PS |

### 6.2 CMS vs G1 vs ZGC

| 维度 | CMS | G1 | ZGC | Shenandoah |
|------|-----|----|-----|-----------|
| **JDK版本** | 5+ (14移除) | 7+ (9默认) | 11+ (15生产) | 12+ |
| **内存布局** | 连续分代 | Region (分代) | Region (不分代) | Region |
| **目标STW** | <100ms | <10ms | <1ms | <10ms |
| **并发标记** | ✅ | ✅ | ✅ | ✅ |
| **并发清理** | ✅ | ❌ (STW) | ✅ | ✅ |
| **碎片问题** | 严重 | 整理Region，碎片少 | 整理，碎片少 | 整理 |
| **适用堆** | <8GB | 4GB-32GB | 16GB-16TB | 同G1 |

```bash
# JDK 11+ 推荐 G1（平衡延迟与吞吐）
-XX:+UseG1GC

# 大堆 + 超低延迟需求 → ZGC（Java 21+ 生产成熟）
-XX:+UseZGC

# 传统高吞吐场景 → Parallel GC
-XX:+UseParallelGC
```

---

## 7. JVM 调优参数

### 7.1 内存参数

```bash
# 堆大小
-Xms2g              # 初始堆大小
-Xmx4g              # 最大堆大小（建议 -Xms = -Xmx，避免动态扩缩）
-Xmn1g              # 新生代大小

# 元空间
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m

# 栈
-Xss512k            # 每个线程栈大小（默认 1M）

# 直接内存
-XX:MaxDirectMemorySize=512m
```

### 7.2 GC 日志参数

```bash
# Java 8
-XX:+PrintGC
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/path/gc.log

# Java 9+
-Xlog:gc*:file=/path/gc.log:time,level,tags
```

### 7.3 常见问题调优

| 问题 | 现象 | 方向 |
|------|------|------|
| OOM: Java heap | 堆满 | 增大 -Xmx 或排查内存泄漏 |
| OOM: MetaSpace | 类加载过多 | 增大 -XX:MaxMetaspaceSize |
| StackOverflow | 递归太深 | 增大 -Xss 或检查递归 |
| Full GC 频繁 | 老年代增长快 | 调大老年代 / 调优晋升阈值 |
| YGC 时间过长 | 新生代太大 | 减小 -Xmn |
| 对象分配失败 | 大对象多 | 增大 Eden 或直接进老年代 |

---

## 8. JVM 调优工具

| 工具 | 功能 | 使用 |
|------|------|------|
| **jps** | 查看 Java 进程 PID | `jps -l` |
| **jstat** | 监控 GC/类加载/编译 | `jstat -gc PID 1000` |
| **jmap** | 堆 dump/查看内存 | `jmap -heap PID` |
| **jstack** | 线程 dump/死锁检测 | `jstack PID` |
| **jinfo** | JVM 参数查看/修改 | `jinfo -flags PID` |
| **jhat** | 堆 dump 分析（已过时） | 替代：VisualVM / MAT |
| **jconsole** | GUI 监控 | JDK 自带 |
| **VisualVM** | 图形化全能工具 | 插件丰富 |
| **Arthas** | 在线诊断神器 | 阿里开源，无需重启 |
| **MAT** | 堆 dump 分析 | 大文件分析首选 |
| **GCViewer** | GC 日志可视化 | 导入 gc.log 分析 |

```bash
# 常用排查命令
jps -l                          # 列出所有 Java 进程
jstat -gcutil PID 1000 10       # 每秒看一次 GC，共10次
jstack PID | grep -A 20 BLOCKED # 排查死锁
jmap -dump:live,file=heap.hprof PID  # dump 堆（会触发 Full GC）

# Arthas 常用
dashboard                       # 实时面板
thread -b                       # 排查死锁
trace ClassName methodName      # 方法调用链路耗时
watch ClassName methodName      # 观察方法入参/返回值
```

---

> 🎯 **一句话总结**：JVM 是 Java 的基石，核心掌握三件事——**内存模型（堆栈方法区）、GC（分代+算法+收集器选择）、调优（参数+工具）**。

---

**下一模块**：[03-Spring生态名词剖析](./03-Spring生态名词剖析.md) → Spring 全家桶核心概念

---

*创建于：2026年7月*
