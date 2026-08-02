# JDK 知识体系总览

> JDK 是 Java 开发者的"操作系统"——从安装管理到工具链实战，从版本选型到诊断调优

## 📚 目录

1. [知识体系导图](#1)
2. [模块导航](#2)
3. [JDK 全景架构图](#3)
4. [核心概念速查](#4)
5. [学习路线推荐](#5)
6. [面试高频 JDK 问题索引](#6)

---

## 1. 知识体系导图 {#1}

本知识体系围绕 JDK 分为五大模块，覆盖从入门到精通的完整路径：

```
JDK 知识体系
│
├── 01 版本演进与选型
│   ├── 版本历史全景 (JDK 1.0 → JDK 25)
│   ├── LTS 版本深度对比 (8 / 11 / 17 / 21 / 25)
│   ├── 迁移实战 (8→11→17→21)
│   ├── 新特性速览 (Virtual Threads, Pattern Matching, etc.)
│   ├── 企业选型决策框架
│   └── OpenJDK vs Oracle JDK vs 发行版对比
│
├── 02 安装与多版本管理
│   ├── 各平台安装 (Windows / macOS / Linux)
│   ├── 环境变量配置 (JAVA_HOME / PATH / CLASSPATH)
│   ├── 多版本管理工具
│   │   ├── SDKMAN (Linux/macOS)
│   │   ├── jEnv (macOS/Linux)
│   │   ├── jabba (跨平台)
│   │   └── 手动切换 (Windows)
│   ├── 验证与测试安装
│   └── 常见安装问题排查
│
├── 03 核心工具链
│   ├── 编译与运行
│   │   ├── javac —— Java 编译器
│   │   ├── java —— 应用启动器
│   │   └── jshell —— REPL 交互式编程
│   ├── 打包与部署
│   │   ├── jar —— 归档打包工具
│   │   ├── jlink —— 自定义运行时镜像
│   │   ├── jpackage —— 原生应用打包
│   │   └── jmod —— JMOD 模块管理
│   ├── 文档与代码辅助
│   │   ├── javadoc —— API 文档生成
│   │   ├── javap —— 反汇编工具
│   │   └── jdeprscan —— 废弃 API 扫描
│   └── 安全与签名
│       ├── keytool —— 密钥和证书管理
│       ├── jarsigner —— JAR 签名工具
│       └── policytool —— 策略文件管理
│
├── 04 诊断与故障排查
│   ├── JVM 进程与信息
│   │   ├── jps —— JVM 进程状态
│   │   ├── jinfo —— 运行时配置查看
│   │   └── jstat —— JVM 统计监控
│   ├── 堆分析与内存诊断
│   │   ├── jmap —— 堆内存转储
│   │   ├── jhat —— 堆转储分析
│   │   └── VisualVM —— 可视化监控
│   ├── 线程与锁诊断
│   │   ├── jstack —— 线程堆栈跟踪
│   │   └── jconsole —— JMX 管理控制台
│   └── 高级诊断工具
│       ├── jcmd —— 统一诊断命令
│       ├── Flight Recorder (JFR)
│       └── Mission Control (JMC)
│
└── 05 性能监控与调优
    ├── 垃圾回收 (GC) 调优
    │   ├── Serial / Parallel / CMS / G1 / ZGC / Shenandoah
    │   ├── GC 日志分析与可视化
    │   └── 常用 GC 参数详解
    ├── JVM 内存调优
    │   ├── 堆内存设置 (-Xms / -Xmx / -Xmn)
    │   ├── 元空间 (Metaspace) 调优
    │   ├── 堆外内存 (Direct Memory) 管理
    │   └── 内存泄漏排查方法论
    ├── 线程与并发调优
    │   ├── 线程池参数优化
    │   ├── 虚拟线程调优 (JDK 21+)
    │   └── 锁竞争分析与优化
    └── 性能基准测试
        ├── JMH 基础与实战
        ├── 微基准测试陷阱
        └── 全链路性能剖析
```

---

## 2. 模块导航 {#2}

| 序号 | 模块 | 核心内容 | 适合人群 | 前置要求 |
|:---:|------|---------|---------|---------|
| 01 | 版本演进与选型 | 版本历史、LTS 对比、迁移指南、新特性速览、发行版对比 | 所有 Java 开发者 | 无 |
| 02 | 安装与多版本管理 | 跨平台安装、环境变量、SDKMAN/jEnv/jabba | 初学者、环境配置者 | 无 |
| 03 | 核心工具链 | javac/java/jar/jlink/javadoc/jshell 等工具详解 | 中级开发者 | 基础 Java 语法 |
| 04 | 诊断与故障排查 | jps/jmap/jstack/jcmd/JFR/JMC 等诊断工具 | 中高级开发者、运维 | JVM 基础 |
| 05 | 性能监控与调优 | GC 调优、内存调优、线程调优、JMH 基准测试 | 高级开发者、架构师 | JVM 内存模型、GC 原理 |

---

## 3. JDK 全景架构图 {#3}

JDK（Java Development Kit）不仅包含 JRE 和 JVM，还提供了完整的开发工具链。以下为 JDK 的全景分层架构：

```
┌─────────────────────────────────────────────────────────────────────┐
│                        JDK (Java Development Kit)                    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    开发工具 (Development Tools)               │   │
│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌────────┐ ┌───────┐ ┌──────┐  │   │
│  │  │javac │ │ java │ │jar   │ │jlink   │ │javadoc│ │jshell│  │   │
│  │  └──────┘ └──────┘ └──────┘ └────────┘ └───────┘ └──────┘  │   │
│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌────────┐ ┌───────┐ ┌──────┐  │   │
│  │  │jps   │ │jmap  │ │jstack│ │jcmd    │ │jstat  │ │jinfo │  │   │
│  │  └──────┘ └──────┘ └──────┘ └────────┘ └───────┘ └──────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              JRE (Java Runtime Environment)                  │   │
│  │  ┌──────────────────────────────────────────────────────┐   │   │
│  │  │              JVM (Java Virtual Machine)               │   │   │
│  │  │  ┌──────────┐ ┌──────────┐ ┌────────────────────┐   │   │   │
│  │  │  │类加载子系统│ │运行时数据区│ │执行引擎            │   │   │   │
│  │  │  │          │ │ Heap    │ │  ├── 解释器         │   │   │   │
│  │  │  │  Bootstrap│ │ Metaspace│ │  ├── C1/C2 JIT    │   │   │   │
│  │  │  │  Ext/Platform│Stack    │ │  ├── Graal JIT    │   │   │   │
│  │  │  │  System   │ │ PC Reg  │ │  └── GC (G1/ZGC/  │   │   │   │
│  │  │  │          │ │ NatMtdStk│ │       Shenandoah) │   │   │   │
│  │  │  └──────────┘ └──────────┘ └────────────────────┘   │   │   │
│  │  └──────────────────────────────────────────────────────┘   │   │
│  │                                                             │   │
│  │  ┌──────────────────────────────────────────────────────┐   │   │
│  │  │          核心类库 (Java API / Class Library)           │   │   │
│  │  │  java.lang │ java.util │ java.io │ java.nio          │   │   │
│  │  │  java.net  │ java.sql  │ java.rmi│ java.security    │   │   │
│  │  │  java.math │ java.text │ java.time │ java.prefs     │   │   │
│  │  └──────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │            运行时基础库 (Runtime Libraries)                   │   │
│  │  zip │ lang │ awt │ nio │ net │ security │ jdk.unsupported │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │               底层操作系统 (Operating System)                 │   │
│  │         Windows │ macOS │ Linux │ Solaris                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

> 💡 **JDK 包含 JRE，JRE 包含 JVM**。JDK = 开发工具 + JRE；JRE = JVM + 核心类库。从 JDK 9 开始引入模块化系统 (JPMS)，JDK 本身也被拆分为多个模块。

---

## 4. 核心概念速查 {#4}

### 4.1 JDK 工具速查表

| 工具 | 全称 | 所属模块 | 主要用途 |
|------|------|---------|---------|
| `javac` | Java Compiler | 开发工具 | 将 `.java` 源文件编译为 `.class` 字节码 |
| `java` | Java Application Launcher | 开发工具/运行时 | 启动 Java 应用程序 |
| `jar` | Java Archive Tool | 打包部署 | 创建和管理 JAR 文件 |
| `javadoc` | Java Documentation Generator | 文档工具 | 从源码注释生成 API 文档 |
| `javap` | Java Disassembler | 诊断工具 | 反编译 class 文件查看字节码 |
| `jshell` | Java Shell (REPL) | 学习/原型 | 交互式 Java 代码执行 |
| `jlink` | Java Linker | 打包部署 | 构建自定义 JRE 运行时镜像 |
| `jpackage` | Java Packaging Tool | 打包部署 | 将应用打包为原生安装包 |
| `jmod` | Java Module Tool | 模块管理 | 创建和管理 JMOD 模块文件 |
| `jps` | Java Process Status | 诊断工具 | 列出当前用户的所有 JVM 进程 |
| `jstat` | JVM Statistics Monitoring | 诊断/监控 | 监控 JVM 的 GC、类加载等统计信息 |
| `jinfo` | Java Configuration Info | 诊断工具 | 查看和修改 JVM 运行时系统属性 |
| `jmap` | Java Memory Map | 诊断工具 | 生成 JVM 堆转储快照 |
| `jstack` | Java Stack Trace | 诊断工具 | 生成线程快照 (线程 dump) |
| `jcmd` | Java Command | 诊断工具 | 统一诊断命令，整合 jmap、jstack、jstat 等功能 |
| `jconsole` | Java Monitoring & Management Console | 监控工具 | JMX 图形化管理控制台 |
| `jhsdb` | Java HotSpot Debugger | 诊断工具 | 附加到崩溃的 JVM 进行事后诊断 |
| `jdeprscan` | Java Deprecation Scanner | 迁移工具 | 扫描编译输出中废弃 API 的使用 |
| `jdeps` | Java Dependencies Analyzer | 依赖分析 | 分析 class 文件的包级别依赖 |
| `javaws` | Java Web Start Launcher | 部署工具 | (已废弃) 启动 JNLP 网络应用 |
| `keytool` | Key and Certificate Management Tool | 安全工具 | 管理密钥库和证书 |
| `jarsigner` | JAR Signing Tool | 安全工具 | 对 JAR 文件进行数字签名 |
| `policytool` | Policy Tool | 安全工具 | 管理安全策略文件 |
| `serialver` | Serial Version Command | 序列化工具 | 返回类的 serialVersionUID |
| `rmic` | RMI Compiler | RMI 工具 | (已废弃) 为远程对象生成 stub/skeleton |
| `rmiregistry` | RMI Registry | RMI 工具 | 创建远程对象注册表 |
| `extcheck` | Extension Check | 检测工具 | (已废弃) 检测 JAR 冲突 |
| `native2ascii` | Native-to-ASCII Converter | 国际化工具 | (已废弃) 编码转换 |
| `jstatd` | JVM Statistics Monitor Daemon | 远程监控 | 远程 JVM 统计信息守护进程 |
| `javah` | Java Header Generator | JNI 工具 | (JDK 10 移除) 生成 C 头文件和 stub |

### 4.2 关键版本时间线

| 版本 | 发布时间 | 维护周期 | 关键特性 |
|:----:|---------|---------|---------|
| JDK 1.0 | 1996.01 | — | 首个正式版，Oak 改名 Java |
| JDK 1.2 | 1998.12 | — | 引入集合框架、Swing、JIT |
| JDK 5 | 2004.09 | — | 泛型、注解、枚举、自动装箱、foreach |
| JDK 8 | 2014.03 | LTS (2030.12) | Lambda、Stream、Optional、新日期 API、Metaspace |
| JDK 11 | 2018.09 | LTS (2032.01) | HTTP Client、ZGC (实验)、模块化 (JPMS)、String 增强 |
| JDK 17 | 2021.09 | LTS (2029.09) | Sealed Class、Pattern Matching (预览)、Vector API、增强伪随机 |
| JDK 21 | 2023.09 | LTS (2031.09) | Virtual Threads (虚拟线程)、Record Patterns、String Templates (预览)、Sequenced Collections |
| JDK 25 | 2025.09 | LTS (2033.09) | 最新 LTS，稳定版 Virtual Threads、Scoped Values、Structured Concurrency |

### 4.3 GC 算法速查

| GC 算法 | 引入版本 | 特点 | 适用场景 |
|---------|---------|------|---------|
| Serial GC | JDK 1.3 | 单线程、Stop-The-World | 单核、小内存、客户端 |
| Parallel GC | JDK 1.4 | 多线程、吞吐量优先 | 批处理、大数据计算 |
| CMS | JDK 1.5 | 低停顿、并发标记清除 | (已废弃) 低延迟应用 |
| G1 GC | JDK 7 (u4) | 分区式、可预测停顿、默认 | 大堆 (4GB+)、服务端 |
| ZGC | JDK 11 (实验)/15 (生产) | 亚毫秒级停顿、无分代 | 超大堆 (TB级)、极低延迟 |
| Shenandoah | JDK 12 (实验)/15 (生产) | 并发压缩、停顿可控 | 低延迟敏感应用 |

### 4.4 关键 API 演进

| API / 特性 | 引入版本 | 状态 (JDK 25) |
|------------|---------|--------------|
| Lambda 表达式 | JDK 8 | 稳定 |
| Stream API | JDK 8 | 稳定 |
| Optional | JDK 8 | 稳定 |
| 新日期时间 API (java.time) | JDK 8 | 稳定 |
| CompletableFuture | JDK 8 | 稳定 |
| HTTP Client (java.net.http) | JDK 11 | 稳定 |
| 模块化系统 (JPMS) | JDK 9 | 稳定 |
| 模式匹配 for instanceof | JDK 16 | 稳定 |
| Record | JDK 16 | 稳定 |
| Sealed Class | JDK 17 | 稳定 |
| Virtual Threads (Project Loom) | JDK 21 | 稳定 |
| Record Patterns | JDK 21 | 稳定 |
| Pattern Matching for switch | JDK 21 | 稳定 |
| Scoped Values | JDK 24 (预览) | 孵化 |
| Structured Concurrency | JDK 24 (预览) | 孵化 |

---

## 5. 学习路线推荐 {#5}

### 5.1 基础路线（1-3 年经验）

```
目标：掌握 JDK 常用工具，能够独立开发 Java 应用

顺序：
① JDK 安装与环境配置 (模块 02)
② 掌握 javac、java、jar 三大基本工具 (模块 03)
③ 理解 JDK 版本差异，至少了解 8/11/17/21 四个 LTS (模块 01)
④ 学会使用 javadoc 生成文档
⑤ 会用 jps、jstack、jmap 进行基本问题定位 (模块 04)
⑥ 了解 GC 基本概念，能配置常见 GC 参数 (模块 05)
```

### 5.2 进阶路线（3-5 年经验）

```
目标：独立排查线上问题，参与 JVM 调优

顺序：
① 深入学习 jlink 和 jpackage 构建自定义运行时 (模块 03)
② 掌握 jstat、jcmd 实时监控 JVM 状态 (模块 04)
③ JFR + JMC 分析运行时性能 (模块 04)
④ GC 调优实战：G1 参数调优 → ZGC 配置 (模块 05)
⑤ 内存泄漏排查方法论 (模块 05)
⑥ 参与 JDK 版本升级迁移 (模块 01)
⑦ 掌握 Module System (JPMS) 实践 (模块 03)
```

### 5.3 专家路线（5+ 年经验）

```
目标：架构级性能调优，JDK 源码级理解

顺序：
① JMH 微基准测试框架深度实战 (模块 05)
② Virtual Threads 与结构化并发调优 (模块 01)
③ 全链路性能剖析：CPU → 内存 → IO → GC (模块 05)
④ JDK 源码阅读：HashMap、ConcurrentHashMap、ThreadPoolExecutor
⑤ 自定义 JVM 参数模板 (不同业务场景)
⑥ 参与 OpenJDK 社区或发行版选型决策 (模块 01)
⑦ JDK 安全机制：SecurityManager、加密扩展、TLS 配置
```

---

## 6. 面试高频 JDK 问题索引 {#6}

### 6.1 JDK 工具与基础

| 面试题 | 考查核心 | 参考模块 |
|--------|---------|---------|
| `==` 和 `equals()` 的区别是什么？ | Java 基础、String 池 | JDK 核心类库 |
| `String`、`StringBuilder`、`StringBuffer` 的区别 | 不可变性、线程安全 | JDK 核心类库 |
| `HashMap` 底层原理？JDK 7 vs 8 的区别？ | 哈希冲突、红黑树 | JDK 核心类库 |
| `ConcurrentHashMap` 如何保证线程安全？ | CAS、synchronized、分段锁 | JDK 核心类库 |
| 什么是类加载机制？双亲委派模型？ | 类加载器、ClassLoader | JVM 基础 |
| `try-with-resources` 的原理是什么？ | AutoCloseable、异常抑制 | JDK 7+ 新特性 |

### 6.2 JDK 版本与新特性

| 面试题 | 考查核心 | 参考模块 |
|--------|---------|---------|
| "你项目用 Java 几？为什么选它？" | 版本选型思路 | 模块 01 |
| JDK 8、11、17、21 的主要区别？ | LTS 版本对比 | 模块 01 |
| Virtual Threads 和 Platform Threads 的区别？ | 虚拟线程原理 | 模块 01 |
| 什么是 Record 类？和 Lombok 的 @Data 有何不同？ | Record 语义 | 模块 01 |
| 什么是 Sealed Class？解决了什么问题？ | 继承控制 | 模块 01 |
| Pattern Matching for instanceof 的好处？ | 类型匹配语法糖 | 模块 01 |
| 模块化系统 (JPMS) 解决了什么问题？ | 模块化设计 | 模块 01 / 03 |
| 迁移到 JDK 11+ 遇到了哪些问题？ | 迁移实战 | 模块 01 |

### 6.3 JVM 诊断与调优

| 面试题 | 考查核心 | 参考模块 |
|--------|---------|---------|
| 线上 CPU 100% 如何排查？ | jstack 定位线程 | 模块 04 |
| 内存泄漏如何排查？ | jmap + MAT 分析 | 模块 04 / 05 |
| 频繁 Full GC 如何排查？ | GC 日志、jstat | 模块 05 |
| G1 GC 的调优参数有哪些？ | G1 工作机制 | 模块 05 |
| ZGC 相比 G1 的优势和限制？ | 并发 GC 原理 | 模块 05 |
| JVM 参数 `-Xms`、`-Xmx`、`-Xmn` 的含义？ | 内存区域划分 | 模块 05 |
| 什么是 JIT？C1 和 C2 编译器的区别？ | 即时编译 | JVM 基础 |
| OOM 有哪些类型？如何配置 OOM dump？ | 内存溢出处理 | 模块 05 |

### 6.4 场景综合题

| 场景问题 | 考查维度 | 参考模块 |
|---------|---------|---------|
| "老项目 JDK 8 要升级到 17，你如何规划？" | 迁移评估、兼容性 | 模块 01 |
| "新项目选 JDK 版本，你怎么选？" | 技术选型能力 | 模块 01 |
| "线上服务突然变慢，如何入手排查？" | 诊断方法论 | 模块 04 / 05 |
| "能否解释一下从源码到运行的完整过程？" | 编译→类加载→执行 | 模块 03 / JVM |
| "如何进行 Java 应用的性能基准测试？" | JMH、基准测试 | 模块 05 |

---

> 🎯 **核心要点**：JDK 知识体系覆盖版本选型、工具链、诊断调优三大主线。对于不同阶段的开发者，学习重点不同——初学者先掌握基本工具和版本差异，中级开发者深入诊断能力，高级开发者专注性能调优和架构决策。在面试中，"版本选型"和"线上问题排查"是最常被考察的两类话题。

---

**下一模块**：[01 JDK版本演进与选型指南](./01-JDK版本演进与选型指南.md)
