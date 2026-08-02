# 01 - Java 语言核心

> 定位：JDK 演进、集合框架、并发体系、JVM 内存与调优——Java 语言层企业考点全景（导览）

## 📚 目录

1. [JDK 版本演进](#1-jdk-版本演进)
2. [集合框架](#2-集合框架)
3. [并发编程体系](#3-并发编程体系)
4. [JVM 内存模型](#4-jvm-内存模型)
5. [JVM 调优](#5-jvm-调优)

---

## 1. JDK 版本演进

| 版本 | 发布 | 核心特性 |
|------|:---:|---------|
| Java 8 | 2014 | **Lambda/Stream/Optional**（长期主流） |
| Java 11 | 2018 | 局部变量推断、ZGC |
| Java 17 | 2021 | **LTS**（虚拟线程前最后稳定） |
| Java 21 | 2023 | **LTS**：虚拟线程、模式匹配 |
| Java 25 | 2025 | LTS（Spring Boot 4 推荐） |

```
⚠️ 版本选型（2026）：
  新项目：Java 21/25 LTS（虚拟线程 + Boot 4）
  遗留：Java 8（大量老系统仍在）

⚠️ 面试必答：
"Java 21 是里程碑 LTS——虚拟线程 +
 模式匹配 + 记录类；
 Spring Boot 4 基线 Java 17+、推荐 21/25。"
```

---

## 2. 集合框架

### 2.1 集合体系

```
Collection（单列）：
  List：ArrayList（数组）/LinkedList（链表）
  Set：HashSet（哈希）/TreeSet（红黑树）
  Queue：ArrayDeque/PriorityQueue

Map（键值）：
  HashMap（数组+链表+红黑树）
  LinkedHashMap（有序）
  ConcurrentHashMap（并发）
  TreeMap（排序）

⚠️ 面试必答：
"集合选型四问——有序？去重？排序？并发？
 默认 ArrayList/HashMap，
 并发 ConcurrentHashMap。"
```

### 2.2 高频考点

```
① HashMap 原理：put/resize/树化（JDK8+）
② 并发：HashMap 不安全 → CHM（CAS+锁）
③ fail-fast：遍历中修改抛 CME
④ 排序：TreeMap 红黑树 O(log n)
⑤ 遍历：entrySet 优于 keySet+get

⚠️ 面试必答：
"HashMap 三问——put 流程、扩容重定位、
 树化条件（8/64）；
 CHM 一问——CAS + synchronized 桶锁。"
```

> 深入：[Java 有关 Hash 的一切](../../01-底层根基-Java核心底座/01-Java基础语法与核心特性/Java有关Hash的一切/00-Hash总览.md)（本仓库已建）

---

## 3. 并发编程体系

### 3.1 并发三要素

```
并发三要素：
  原子性：synchronized/Lock/Atomic
  可见性：volatile
  有序性：happens-before

工具族：
  synchronized/Lock、volatile
  JUC：CountDownLatch/CyclicBarrier/Semaphore
  Atomic 类、线程池（ExecutorService）
  CompletableFuture（异步编排）

⚠️ 面试必答：
"并发三要素（原子/可见/有序）+
 线程池七参数 + JUC 工具选型——
 是并发面试的三大支柱。"
```

### 3.2 线程池（必考）

```
线程池七参数：
  核心线程/最大线程/存活时间/时间单位/
  队列/线程工厂/拒绝策略

拒绝策略四种：
  AbortPolicy（抛异常）/CallerRuns（调用者执行）
  Discard（丢弃）/DiscardOldest（丢最老）

⚠️ 面试必答：
"线程池七参数 + 四拒绝策略——
 参数公式（IO 密集 2 倍核数）；
 实际生产用 ThreadPoolExecutor 自定义。"
```

> 深入：[多线程专题](../../01-底层根基-Java核心底座/06-数据结构与算法/多线程/00-多线程专题精要.md)

---

## 4. JVM 内存模型

### 4.1 内存结构

```
JVM 内存五区（运行时数据区）：
  线程共享：堆（对象）、方法区（类/常量）
  线程私有：虚拟机栈（方法帧）、本地方法栈、程序计数器

堆分区（分代）：
  新生代（Eden + S0/S1）→ 老年代
  GC：Minor GC（新生代）/Full GC（全堆）

⚠️ 面试必答：
"JVM 内存五区 + 分代收集——
 对象优先 Eden、大对象直接老年代、
 长期存活进老年代（年龄 15）。"
```

### 4.2 GC 与垃圾回收器

```
垃圾判定：可达性分析（GC Roots）
回收算法：标记清除/复制/标记整理

收集器演进：
  CMS → G1（JDK9+ 默认）→ ZGC（低延迟）

⚠️ 面试必答：
"G1 默认（可预测停顿）、ZGC 亚毫秒；
 CMS 已废弃；'对象怎么死的'
 = 可达性分析 + 引用类型（强/软/弱/虚）。"
```

---

## 5. JVM 调优

### 5.1 调优三板斧

```
JVM 调优流程：
  ① 监控（GC 日志 + 堆转储 + jstat）
  ② 定位（Full GC 频率/OOM 场景）
  ③ 调参（堆大小/GC 选择/参数）

常用参数：
  -Xms/-Xmx：堆（相等防伸缩）
  -XX:+UseG1GC：G1
  -XX:MaxGCPauseMillis：停顿目标
  -XX:+HeapDumpOnOutOfMemoryError：OOM 转储

⚠️ 面试必答：
"调优 = 监控 → 定位 → 调参；
 OOM 排查用堆转储 + MAT 分析大对象；
 80% 的'JVM 问题'其实是代码问题
 （泄漏/大对象/无限缓存）。"
```

### 5.2 OOM 场景

```
OOM 四类：
  堆溢出（大对象/泄漏）
  栈溢出（深递归）
  元空间溢出（类加载泄漏）
  直接内存溢出（NIO）

⚠️ 面试必答：
"OOM 四类——堆/栈/元空间/直接内存；
 排查第一步：HeapDump + MAT
 找'谁占着内存不放'。"
```

---

> 🎯 **核心要点**：Java 语言 = **JDK 演进**（21 LTS 虚拟线程）+ **集合**（HashMap 原理 + 选型四问）+ **并发**（三要素 + 线程池七参数）+ **JVM 内存**（五区 + 分代 + G1）+ **调优**（监控定位 + OOM 四类）。这是 Java 后端面试的第一大板块（占比 ~30%）。

---

**返回总览**：[00-Java后端技术栈总览](00-Java后端技术栈总览.md) | **下一篇**：[02-Spring生态全景](02-Spring生态全景.md)
