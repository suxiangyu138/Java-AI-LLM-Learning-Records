# 01 — JVM概述与体系结构

> 定位：建立JVM全局观——理解Java"一次编写到处运行"的底层基石、JVM/HotSpot/GraalVM的演进脉络、三大子系统（类加载/运行时数据区/执行引擎）的分工协作

## 目录

1. [JVM是什么：从源码到运行](#1-jvm是什么从源码到运行)
2. [JVM整体架构：三大子系统](#2-jvm整体架构三大子系统)
3. [JVM家族：HotSpot/OpenJ9/GraalVM](#3-jvm家族hotspotopenj9graalvm)
4. [JVM与JDK版本演进](#4-jvm与jdk版本演进)
5. [编译与执行：解释执行/JIT/AOT](#5-编译与执行解释执行jit-aot)
6. [JVM启动与退出](#6-jvm启动与退出)
7. [常用JVM参数速查](#7-常用jvm参数速查)
8. [面试高频考点](#8-面试高频考点)

---

## 1. JVM是什么：从源码到运行

JVM（Java Virtual Machine）是一台**虚拟计算机**的规范（由JSR定义），屏蔽底层OS与硬件差异，是Java"一次编写，到处运行"的基石。

```
┌──────────────────────────────────────────────────────────────────┐
│               源文件 → 字节码 → JVM运行时                        │
│                                                                  │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────────┐ │
│  │ MyApp.java   │────▶│ MyApp.class  │────▶│   JVM Runtime    │ │
│  │ (源码)        │javac│ (字节码)      │     │  ┌────────────┐ │ │
│  └──────────────┘     └──────────────┘     │  │ClassLoader │ │ │
│                                             │  │子系统       │ │ │
│    Magic Number: 0xCAFEBABE                 │  └────────────┘ │ │
│    cafe → 咖啡, babe → 新生命               │  ┌────────────┐ │ │
│                                             │  │运行时数据区 │ │ │
│  "Write Once, Run Anywhere"                 │  │(堆/栈/方法区)│ │
│                                             │  └────────────┘ │ │
│  Kotlin/Scala/Groovy/GraalVM多语言          │  ┌────────────┐ │ │
│  均可编译为.class运行在JVM上                 │  │执行引擎    │ │ │
│                                             │  │(解释+JIT+GC)│ │ │
│                                             │  └────────────┘ │ │
│                                             └──────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

| JVM核心价值 | 说明 |
|-------------|------|
| **平台无关性** | 字节码由JVM翻译为平台机器码，一次编译处处运行 |
| **自动内存管理** | GC自动回收无用对象，开发无需手动释放内存 |
| **安全沙箱** | 字节码验证器保障类型安全 |
| **运行时优化** | 解释执行保可移植性，JIT编译保热点性能 |
| **动态链接** | 类按需加载，支持反射、动态代理 |

> 💡 JVM是一个**规范**，HotSpot是最主流的**实现**。任何能编译为`.class`的语言都可跑在JVM上——不限于Java，还包括Kotlin/Scala/Groovy等。

> 🎯 **80%的线上Java性能问题都与JVM直接相关**——学会JVM是从"会用Java"走向"懂Java"的核心门槛。

---

## 2. JVM整体架构：三大子系统

JVM运行时由**三个核心子系统**协同完成从字节码到程序执行的完整流程。

### 2.1 架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                      JVM Runtime System                          │
│                                                                  │
│  ┌──────────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   ClassLoader        │  │  Runtime Data    │  │ Execution   │ │
│  │   Subsystem          │  │  Areas           │  │ Engine      │ │
│  │                      │  │                  │  │             │ │
│  │  ┌────────────────┐  │  │  ┌─────────────┐ │  │ ┌─────────┐ │ │
│  │  │   Loading      │  │  │  │  Heap (堆)  │ │  │ │Interpre-│ │ │
│  │  │   .class →     │──┼──┼─▶│  Young+Old  │──┼──┼─▶│ter      │ │ │
│  │  │   Class对象     │  │  │  └─────────────┘ │  │ └─────────┘ │ │
│  │  ├────────────────┤  │  │  ┌─────────────┐ │  │ ┌─────────┐ │ │
│  │  │   Linking      │  │  │  │ Method Area  │ │  │ │JIT Com- │ │ │
│  │  │   验证/准备/解析 │  │  │  │ Metaspace   │ │  │ │piler    │ │ │
│  │  ├────────────────┤  │  │  └─────────────┘ │  │ └─────────┘ │ │
│  │  │   Init         │  │  │  ┌─────────────┐ │  │ ┌─────────┐ │ │
│  │  │   <clinit>     │  │  │  │ Stack/PC/   │ │  │ │  GC     │ │ │
│  │  └────────────────┘  │  │  │ Native       │ │  │ └─────────┘ │ │
│  └──────────────────────┘  │  └─────────────┘ │  └─────────────┘ │
│                             └──────────────────┘                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 三大子系统职责

| 子系统 | 核心职责 | 专题章节 |
|--------|----------|----------|
| **ClassLoader** | 读取`.class`→验证→准备→解析→初始化→生成`Class`对象 | 类加载机制专题 |
| **Runtime Data Areas** | 五大区域：堆（对象）、栈（方法调用）、方法区（类元数据）、PC（指令地址）、本地方法栈（Native） | 运行时数据区专题 |
| **Execution Engine** | 解释器（逐行翻译）+ JIT（热点编译）+ GC（自动回收） | 执行引擎专题 |

### 2.3 类加载完整生命周期

```
┌──────────┐   ┌──────────────────────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ Loading  │──▶│        Linking           │──▶│ Init     │──▶│ Using    │──▶│Unloading │
│ (加载)    │   │  验证→准备→解析          │   │ (<clinit>)│   │ (使用)    │   │ (卸载)    │
└──────────┘   └──────────────────────────┘   └──────────┘   └──────────┘   └──────────┘
```

```java
// 六种主动引用触发类初始化
Class.forName("com.example.MyClass");  // 反射
new MyClass();                          // new关键字
MyClass.staticField = 1;                // 访问static字段
MyClass.staticMethod();                 // 调用static方法
// 4. 子类初始化触发父类
// 5. 启动类 (main所在类)
// 6. 接口默认方法 (JDK 8+)
```

> 💡 Resolution（解析）不必在Initialization之前完成——这是**动态绑定**和**延迟加载**的重要基础。

### 2.4 运行时数据区

| 区域 | 线程 | 存储内容 | 异常风险 |
|------|:----:|----------|:--------:|
| **堆（Heap）** | 共享 | 对象实例、数组 | ⚠️ OOM主战场 |
| **方法区/Metaspace** | 共享 | 类元数据、常量池、static变量、JIT代码缓存 | ⚠️ 动态类过多OOM |
| **程序计数器** | 私有 | 当前字节码行号 | ✅ 唯一不OOM |
| **虚拟机栈** | 私有 | 栈帧（局部变量表/操作数栈/动态链接/返回地址） | `StackOverflow` |
| **本地方法栈** | 私有 | Native方法调用 | `StackOverflow` |

```
堆内存布局:
┌─────────────────────────────────────────────────────────────┐
│                          Heap                                 │
│  ┌───────────────────────┬──────────────────────────────┐   │
│  │      Young Gen        │          Old Gen              │   │
│  │  ┌────┬────┬────┐     │  大对象/长期存活对象           │   │
│  │  │Eden│ S0 │ S1 │     │  默认: 老:新 = 2:1            │   │
│  │  │ 80%│ 10%│ 10%│     │                              │   │
│  │  └────┴────┴────┘     │                              │   │
│  └───────────────────────┴──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.5 执行引擎

```
字节码 → 解释器（逐行翻译，启动快）→ JIT（编译热点代码为机器码缓存，运行快）→ GC（自动回收）
```

| 模式 | 原理 | 优劣 |
|------|------|------|
| **解释执行** | 启动即用，逐行解析 | 启动快，执行慢 |
| **JIT编译** | 热点探测→C1编译→C2深度优化→机器码缓存 | 启动后逐渐提速，峰值性能高 |
| **AOT** | 构建时预先编译为原生可执行文件 | 毫秒级启动，但牺牲动态性 |

> 💡 服务刚启动慢（解释器为主）→ 运行一段时间后变快（JIT编译热点）。**接口预热**的底层原理即提前触发JIT编译。

---

## 3. JVM家族：HotSpot/OpenJ9/GraalVM

### 3.1 主流JVM实现对比

| 特性 | HotSpot (Oracle) | OpenJ9 (Eclipse) | GraalVM (Oracle) |
|------|-----------------|------------------|-------------------|
| **许可证** | GPLv2 + CE | GPLv2 | GPLv2 + CE |
| **默认GC** | G1 (JDK 9+) | Balanced GC | G1 |
| **JIT编译器** | C1 + C2 | JITServer | Graal JIT（替代C2） |
| **AOT** | jaotc（有限） | AOT ahead-of-time | Native Image（强力） |
| **启动** | 中等 | 快（-20-30%） | 极快（毫秒级，Native Image） |
| **内存** | 中等 | 低（-30-50%） | 极低 |
| **多语言** | 仅Java | 仅Java | Java+JS+Python+Ruby+WASM |
| **场景** | 通用服务端 | 云原生/容器 | Serverless/微服务/多语言 |

### 3.2 HotSpot — 工业标准

> `java -version`输出的就是HotSpot，全球使用最广的JVM实现。

- **C1编译器**：轻量编译，编译快优化少（客户端模式）
- **C2编译器**：重量编译，编译慢优化深（服务端模式）——逃逸分析、循环优化、向量化
- **分层编译（JDK 7+）**：C1快速过渡→C2深度优化，兼顾启动与峰值

### 3.3 GraalVM — 下一代高性能虚拟化平台

```
GraalVM 架构:
┌──────────────────────────────────────────────────────────────┐
│  Graal Compiler (Java写的JIT编译器，JDK 17+可替代C2)        │
│  + Truffle (多语言框架: JS/Python/Ruby)                     │
│  + Native Image (AOT编译→毫秒级启动→无需JRE)               │
│     ⚠️ 需配置反射/动态代理，不支持动态类加载                │
└──────────────────────────────────────────────────────────────┘
```

> ⚠️ GraalVM Native Image启动极快、内存极省，但存在局限：反射需预配置、不支持完全动态类加载、构建时间较长。

### 3.4 选型决策

```
标准Java服务端 → HotSpot + G1 (JDK 8) / Graal JIT + ZGC (JDK 17+)
云原生/容器    → OpenJ9（内存省30-50%）
Serverless/网关 → GraalVM Native Image（毫秒级启动）
多语言混编     → GraalVM Polyglot
高吞吐批处理   → HotSpot + ParallelGC
```

---

## 4. JVM与JDK版本演进

### 4.1 LTS关键特性时间线

| 版本 | 发布 | GC默认 | 里程碑特性 |
|------|------|--------|-----------|
| JDK 7 | 2011 | Parallel | G1实验性、String Pool移入堆、invokedynamic |
| **JDK 8** | 2014 | Parallel | **Metaspace替代PermGen**、Lambda+Stream |
| **JDK 11** | 2018 | **G1** | ZGC实验性、Epsilon GC、jaotc、字符串去重 |
| **JDK 17** | 2021 | G1 | **ZGC生产就绪**（<1ms停顿）、Graal可替代C2、CMS移除 |
| **JDK 21** | 2023 | G1 | **虚拟线程正式**、分代ZGC、记录模式 |
| JDK 25 | 2025 | G1 | Project Leyden AOT增强、强化分代ZGC |

### 4.2 Metaspace vs PermGen

| 特性 | PermGen (JDK ≤ 7) | Metaspace (JDK 8+) |
|------|-------------------|---------------------|
| 位置 | JVM堆内 | 本地内存（Native） |
| 默认大小 | 64-82MB | 无上限（取决于OS） |
| 参数 | `-XX:MaxPermSize` | `-XX:MaxMetaspaceSize` |
| 扩容 | 需要Full GC | 自动利用OS内存 |
| OOM风险 | 高 | 低 |

> 🎯 **Metaspace替换PermGen的核心动机**：PermGen大小固定易OOM（尤其动态类生成场景），GC回收效率低。本地内存动态扩展，大幅降低OOM概率。

### 4.3 GC默认配置沿革

```bash
# JDK 7/8: Parallel Scavenge + Parallel Old
# JDK 9/11/17/21: G1（JDK 9起成为默认）
# JDK 21+: ZGC + ZGenerational 可启用分代模式
java -XX:+PrintCommandLineFlags -version  # 查看当前GC
```

---

## 5. 编译与执行：解释执行/JIT/AOT

### 5.1 三种执行方式

| 维度 | 解释执行 | JIT即时编译 | AOT提前编译 |
|------|----------|-------------|-------------|
| **时机** | 运行时逐行翻译 | 运行时热点编译 | 构建时预编译 |
| **启动** | ✅ 最快 | ❌ 需要预热 | ✅ 毫秒级 |
| **峰值性能** | ❌ 慢 | ✅ 高 | ✅ 较高（架构绑定） |
| **动态特性** | ✅ 全支持 | ✅ 全支持 | ❌ 需配置 |
| **适用场景** | 启动阶段、非热点 | **长驻服务端** | **Serverless、CLI** |

### 5.2 JIT分层编译流水线

```
方法调用 → 达到阈值 → C1编译(带profiling) → 达到C2阈值 → C2深度编译
                                                              ├─ 方法内联
                                                              ├─ 逃逸分析+标量替换
                                                              ├─ 循环优化/向量化
                                                              └─ 去虚拟化
                                                              ↓
                                                        CodeCache 缓存
```

```java
// 逃逸分析示例
public class EscapeAnalysisDemo {
    // ✅ 未逃逸 — JIT做标量替换，栈上分配
    public long sumAges() {
        Point p = new Point(1, 2);
        return p.x + p.y;
    }
    // ❌ 逃逸 — 堆分配
    public Point createPoint() {
        return new Point(1, 2);
    }
    static class Point { int x, y; Point(int x, int y) { this.x = x; this.y = y; } }
}
```

> 💡 **选型原则**：长驻微服务 → JIT（JDK 17+ Graal Compiler）；Serverless/网关 → AOT（GraalVM Native Image）。JDK 11+支持JIT+AOT混合。

---

## 6. JVM启动与退出

### 6.1 启动流程

```
java -Xms1g -Xmx2g -jar MyApp.jar

1. 创建JVM环境: 加载jvm.dll, 初始化JNI, 创建Bootstrap ClassLoader
2. 解析启动参数: 堆/栈/GC/元空间设置, -D系统属性
3. 初始化类加载器: Platform → Application → 线程上下文类加载器
4. 加载运行时核心类: java.lang.*, JNI方法注册
5. 运行main线程: 加载启动类 → 执行static块 → main()

⏱ 典型Spring Boot启动: 3-8秒 (类加载40% + JIT预热30% + 框架初始化30%)
```

```bash
# 追踪启动过程
java -XX:+TraceClassLoading -jar app.jar         # 类加载追踪
java -XX:+PrintCommandLineFlags -jar app.jar      # 最终参数
java -Xlog:class+load=info,gc*=info:file=startup.log -jar app.jar  # JDK 9+
```

### 6.2 JVM退出与shutdownHook

| 退出方式 | 触发 | 执行Hook？ |
|----------|------|:---------:|
| 正常退出 | `main()`返回 / `System.exit(n)` | ✅ |
| 外部中断 | `Ctrl+C` / `kill -TERM` (SIGTERM) | ✅ |
| 强制终止 | `kill -9` (SIGKILL) | ❌ |

```java
// 用于JVM关闭前的资源清理
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("JVM关闭中...释放连接池/缓存/注册中心");
}));
```

> ⚠️ shutdownHook限制：不保证执行顺序、执行时间有限、`kill -9`无法拦截。

---

## 7. 常用JVM参数速查

### 7.1 内存参数

```bash
# ---------- 堆 ---------- #
-Xms512m              # 初始堆
-Xmx2g                # 最大堆
-Xmn256m              # 新生代大小
-XX:NewRatio=2        # 老:新 = 2:1
-XX:SurvivorRatio=8   # Eden:S0:S1 = 8:1:1

# ---------- 栈 & 元空间 ---------- #
-Xss256k              # 线程栈（默认1MB，递归多用大值）
-XX:MetaspaceSize=128m    # 元空间阈值
-XX:MaxMetaspaceSize=256m # 元空间上限
-XX:+UseCompressedOops    # 指针压缩（堆<32GB时默认开启）
```

### 7.2 GC参数

```bash
# ---------- 收集器选择 ---------- #
-XX:+UseParallelGC     # JDK 7/8 默认（吞吐量优先）
-XX:+UseG1GC           # JDK 9+ 默认（平衡延迟+吞吐）
-XX:+UseZGC            # JDK 15+ 生产（超低延迟，<1ms）

# ---------- G1调优 ---------- #
-XX:MaxGCPauseMillis=200   # 期望最大停顿(ms)
-XX:G1HeapRegionSize=4m    # Region大小
-XX:InitiatingHeapOccupancyPercent=45  # 启动并发标记阈值(%)

# ---------- ZGC调优 ---------- #
-XX:ConcGCThreads=N
-XX:+ZGenerational     # 分代模式（JDK 21+）
```

### 7.3 JIT编译参数

```bash
-XX:ReservedCodeCacheSize=256m   # CodeCache大小
-XX:CICompilerCount=4            # 编译线程数
-XX:CompileThreshold=10000       # 编译阈值
-XX:MaxInlineSize=100            # 内联字节码上限
-XX:+PrintCompilation            # 打印编译事件
```

### 7.4 诊断参数

```bash
# JDK 8 GC日志
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log

# JDK 9+ 统一日志
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=50m

# 堆Dump
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/path/to/dump.hprof

# 类加载追踪
-XX:+TraceClassLoading -XX:+TraceClassUnloading

# JIT编译失败原因
-XX:+PrintCompileFailureReason
```

### 7.5 典型配置模板

```bash
# 高并发在线服务 (4核8G, G1)
java -Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
     -XX:InitiatingHeapOccupancyPercent=35 -XX:ParallelGCThreads=4 \
     -XX:ReservedCodeCacheSize=256m -XX:+HeapDumpOnOutOfMemoryError \
     -Xlog:gc*:file=gc.log:time,uptime -jar app.jar

# 批处理高吞吐 (8核16G, Parallel)
java -Xms8g -Xmx8g -Xmn5g -XX:+UseParallelGC -XX:ParallelGCThreads=8 \
     -XX:GCTimeRatio=19 -jar batch.jar

# 超低延迟金融服务 (16核32G, ZGC)
java -Xms16g -Xmx16g -XX:+UseZGC -XX:ConcGCThreads=4 \
     -XX:ReservedCodeCacheSize=512m -XX:+HeapDumpOnOutOfMemoryError \
     -jar lowlatency.jar
```

---

## 8. 面试高频考点

### 基础

**Q1: JVM三大子系统和五大运行时数据区？**

> ClassLoader + Runtime Data Areas + Execution Engine。线程私有：程序计数器（唯一不OOM）、JVM栈、本地方法栈。共享：堆、方法区。

**Q2: 对象在堆中的分配过程？**

> 逃逸分析（栈分配）→ TLAB线程本地分配 → Eden → 大对象直接进入Old。GC后：Eden→Survivor（年龄+1）→晋升Old（年龄达15阈值）。

**Q3: JIT分层编译？**

> 解释执行 → C1（轻量编译带profiling）→ C2（深度编译，内联/逃逸分析/循环优化/向量化）。分层编译兼顾启动速度与峰值性能。

### 进阶

**Q4: 为什么用Metaspace替代PermGen？**

> PermGen大小固定易OOM、Full GC才回收效率低。Metaspace用本地内存动态扩展，降低OOM概率，String Pool已在JDK 7移入堆。

**Q5: HotSpot vs GraalVM vs OpenJ9选型？**

> HotSpot最成熟生态好；OpenJ9内存省30-50%适合云原生；GraalVM Native Image毫秒级启动适合Serverless。

**Q6: 接口预热（Warm-up）原理？**

> 热点方法先C1编译，达到C2阈值后深度编译为机器码缓存于CodeCache。预热即通过压测提前触发此过程，避免上线初期解释执行高延迟。

### 深度

**Q7: 逃逸分析使哪些优化成为可能？**

> ① **栈上分配**：未逃逸对象不进入堆，减少GC压力；② **标量替换**：对象拆为基础类型直接在寄存器操作；③ **锁消除**：未逃逸对象的同步被消除。

**Q8: 一个Object对象在64位JVM占多少内存？**

> 开启指针压缩：Mark Word(8B) + Klass Pointer(4B) + Padding(4B) = **16B**。关闭(堆>32GB)：Mark Word(8B) + Klass Pointer(8B) = **16B**（无需填充）。

---

## 参考资源

- [JVM Specification (Java SE 21)](https://docs.oracle.com/javase/specs/jvms/se21/html/)
- [GraalVM Official Documentation](https://www.graalvm.org/latest/docs/)
- [JDK 17 Garbage Collector Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [OpenJ9 User Guide](https://eclipse.dev/openj9/docs/)
- [Shipilev: JMM Pragmatics](https://shipilev.net/blog/2016/close-encounters-of-jmm-kind/)

---

*最后更新: 2026-07-26 | 适用JDK 8/11/17/21/25 | 配套: [01-JVM原理与内存模型.md](./01-JVM原理与内存模型.md)*
