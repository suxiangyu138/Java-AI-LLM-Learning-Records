# 05 JDK 性能监控与调优工具

> JFR零开销记录、JMC可视化分析、Arthas在线诊断——这些现代工具让Java性能调优从"玄学"变成"科学"

## 📚 目录

1. [JFR — 零开销性能记录](#1-jfr--零开销性能记录)
2. [JMC — 可视化分析](#2-jmc--可视化分析)
3. [JConsole — 基础监控](#3-jconsole--基础监控)
4. [VisualVM — 全能可视化](#4-visualvm--全能可视化)
5. [Arthas — 在线诊断神器（重点）](#5-arthas--在线诊断神器重点)
6. [async-profiler — 高性能采样](#6-async-profiler--高性能采样)
7. [GC日志分析](#7-gc日志分析)
8. [工具选型速查](#8-工具选型速查)

---

## 1. JFR — 零开销性能记录

### 1.1 JFR简介

**JFR**（JDK Flight Recorder，JDK飞行记录器）是Oracle JDK（以及OpenJDK 11+）内置的**低开销事件采集框架**。它通过JVM内置的探针持续采集运行时数据，对应用性能的影响通常低于 **1%**。

> 💡 **核心概念**：JFR的独到之处在于"飞行记录"——像黑匣子一样始终在记录，发生问题时只需"提取"最近的数据即可回溯，无需提前启动。

### 1.2 JFR原理

```
┌─────────────────────────────────────────────────┐
│                  JVM Events                      │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ │
│  │ CPU  │ │ Heap │ │ Lock │ │ IO   │ │ Class│ │
│  │Usage │ │Alloc │ │Wait  │ │Ops   │ │Load  │ │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ │
│     └────────┴────────┴────────┴────────┘       │
│                      ↓                          │
│            ┌─────────────────┐                   │
│            │  Ring Buffer    │  ← 循环覆写缓冲区  │
│            │  (循环缓冲区)    │                   │
│            └────────┬────────┘                   │
│                     ↓                            │
│   ┌─────────────────────────────────────┐        │
│   │  Flight Recording (.jfr 文件)       │        │
│   │  → JMC / CLI / API 分析            │        │
│   └─────────────────────────────────────┘        │
└─────────────────────────────────────────────────┘
```

- JFR使用**环形缓冲区**持续写入事件数据
- 缓冲区满后覆盖最旧的数据，始终保留最新的记录
- 可配置事件类型和采样频率，控制性能开销
- 输出为 `.jfr` 文件，使用 **JMC** 分析

### 1.3 启动方式

#### 方式一：启动参数（推荐生产环境长期开启）

```bash
# 启动即开始记录，持续60秒后自动停止
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr,name=MyRecord \
     -jar app.jar

# 持续记录，不自动停止（最长可配到24小时）
java -XX:StartFlightRecording=settings=profile,dumponexit=true,filename=recording.jfr,name=MyApp \
     -jar app.jar

# 参数详解：
#   settings=default|profile   → default: 基础事件(~1%开销); profile: 详细事件(~2%开销)
#   dumponexit=true            → 应用退出时自动dump
#   maxage=1h                  → 事件保留1小时
#   maxsize=250MB              → 最大记录文件大小
```

#### 方式二：jcmd动态启动（线上按需开启）

```bash
# 启动记录（无需重启）
jcmd <PID> JFR.start name=MyRecord settings=profile duration=60s

# 查看记录状态
jcmd <PID> JFR.check

# 导出记录
jcmd <PID> JFR.dump name=MyRecord filename=/tmp/profile_$(date +%Y%m%d_%H%M%S).jfr

# 停止记录
jcmd <PID> JFR.stop name=MyRecord
```

#### 方式三：Java代码控制（程序化）

```java
// JDK 9+ 通过Java API控制
import jdk.jfr.*;

try (Recording recording = new Recording()) {
    recording.setName("My Recording");
    recording.setDumpOnExit(true);
    recording.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1));
    recording.enable("jdk.GCPhasePause").withThreshold(Duration.ofMillis(10));
    recording.start();
    // ... 业务逻辑 ...
    recording.dump(Path.of("/tmp/recording.jfr"));
}
```

### 1.4 常用事件分类

| 事件类别 | 事件 | 说明 | 诊断场景 |
|---------|------|------|---------|
| **CPU** | `jdk.CPULoad` | CPU使用率 | 高CPU场景 |
| | `jdk.ThreadCPULoad` | 线程级CPU使用 | 找CPU消耗大户 |
| **内存** | `jdk.ObjectAllocationInNewTLAB` | TLAB对象分配 | 对象分配速率 |
| | `jdk.GCPhasePause` | GC暂停时间 | 停顿时长监控 |
| | `jdk.GCHeapSummary` | 堆概况 | 堆使用趋势 |
| **锁** | `jdk.JavaMonitorWait` | 锁等待 | 锁竞争分析 |
| | `jdk.JavaMonitorEnter` | 锁进入 | 锁争抢热点 |
| **IO** | `jdk.FileWrite` | 文件写入 | 磁盘IO瓶颈 |
| | `jdk.SocketRead` / `jdk.SocketWrite` | Socket读写 | 网络IO瓶颈 |
| **类** | `jdk.ClassLoad` | 类加载 | 类加载风暴 |
| | `jdk.ClassDefine` | 类定义 | - |
| **线程** | `jdk.ThreadStart` / `jdk.ThreadEnd` | 线程生命周期 | 线程创建风暴 |
| | `jdk.ThreadSleep` | 线程休眠 | - |

### 1.5 实战：记录10分钟生产环境性能数据

```bash
# Step 1: 查看Java进程
jps -l -v | grep -v Jps

# Step 2: 启动JFR记录10分钟（profile级别）
jcmd <PID> JFR.start name=prod_profile settings=profile duration=10m

# Step 3: 等待10分钟...

# Step 4: 检查记录状态
jcmd <PID> JFR.check

# Step 5: 导出记录
jcmd <PID> JFR.dump name=prod_profile \
    filename=/tmp/prod_$(hostname)_$(date +%Y%m%d_%H%M%S).jfr

# Step 6: 下载.jfr到本地，用JMC打开分析
```

### 1.6 JFR vs 传统Profiling

| 特性 | JFR | 传统Profiler（如YourKit/JProfiler） |
|------|:---:|:---:|
| 性能开销 | <1%（default模式） | 5%-20%（采样模式） |
| 生产环境使用 | **完全可用** | 风险较高 |
| Agent注入 | 无需（JVM内置） | 需要附加agent |
| 数据采集方式 | **事件驱动** | 采样/字节码增强 |
| 数据保留 | 环形缓冲区，固定大小 | 全量记录，内存占用大 |
| 远程连接 | 无需（本地记录） | 需要JMX或Socket连接 |
| 启动时机 | 可随时动态启停 | 需随进程启动或attach |
| 分析能力 | 需配合JMC | 自带UI |

> 🎯 **核心要点**：
> - JFR最大的价值是**可以安全地在生产环境长期开启**，开销极小
> - 用 `jcmd` 动态启停，无需重启应用——这是线上利器
> - "飞行记录"的特性意味着**问题发生后再回溯也能看到原因**
> - profile级别（详细采样）建议按需开启，日常用default级别即可
> - 建议生产环境默认开启 `-XX:StartFlightRecording` 的default级别

---

## 2. JMC — 可视化分析

### 2.1 JMC简介

**JMC**（JDK Mission Control，JDK任务控制）是JFR数据的官方分析工具，提供丰富的可视化面板和自动化分析规则。

```bash
# Windows/Mac: 在JDK bin目录下双击 jmc.exe / jmc
# 或通过命令行启动
jmc

# 在JMC中：File → Open File → 选择.jfr文件
```

### 2.2 关键面板解读

#### 概览界面（Overview）

| 面板 | 展示内容 | 快速判断 |
|------|---------|---------|
| CPU | 总CPU使用率、JVM CPU、GC CPU | GC CPU > 10% → GC开销过大 |
| Heap | 堆使用量趋势曲线 | 持续上升不下降 → 泄漏嫌疑 |
| Threads | 活跃线程数、线程状态分布 | 活跃线程数异常 → 线程泄漏 |
| Latency | 锁等待延迟分布 | 大量锁等待 → 锁竞争严重 |
| GC Pauses | GC暂停时间 | 单次>1s → 需优化GC |
| TLAB Allocations | TLAB分配速率 | 分配速率异常高 → 优化对象创建 |

#### 自动化分析（Automated Analysis）

JMC内置数十条分析规则，自动扫描JFR文件并标记问题：

```text
Rules Check Results:
✅ No problems found (20/29 rules checked)
❌ Problems found (9/29 rules checked)

Critical:
  - GC Pause Time: 15% of total runtime (阈值: <5%)
  - Heap Content: Old generation usage 95%

Warning:
  - Lock Contention: 25% of threads in blocked state
  - Exception: 1,200 stack traces with exceptions
  - TLAB Allocation Rate: 850MB/s (阈值: <500MB/s)

Info:
  - CPU Load: JVM uses 40% of 4 CPUs
  - GC Configuration: Parallel GC, 4 threads
```

#### 火焰图（Flame Graph）

JMC 8+ 内置火焰图支持：

```text
┌─────────────────────────────────────────────────────┐
│                 CPU Flame Graph                      │
│                                                      │
│   ┌──────────────────────────────────┐               │
│   │  org.apache.tomcat.threads.run   │               │
│   ├──────────────────────────────────┤               │
│   │  com.example.service.handleRequest              │
│   ├───────────────────────┬──────────┤               │
│   │  service.processOrder │ service. │               │
│   ├────────┬──────────────┤  log     │               │
│   │  dao.  │  mq.send     │          │               │
│   │  query │              │          │               │
│   └────────┴──────────────┴──────────┘               │
│                                                      │
│   🔥 最宽的函数 = 最热的CPU消耗点                      │
│   → 这里dao.query占CPU时间最长                        │
└─────────────────────────────────────────────────────┘
```

### 2.3 关键面板使用场景

**场景1：接口变慢——查线程等待**

1. 打开JFR → "Threads" → "Thread Latencies"
2. 查看等待时间最长的线程
3. 双击查看堆栈 → 定位是数据库查询慢还是锁等待

**场景2：GC频繁——查GC详情**

1. "Memory" → "GC Pause Times"
2. 看单次GC时间分布：是YGC多还是FGC多
3. "GC Configuration" → 检查GC配置是否合理

**场景3：OOM风险——查对象分配**

1. "Memory" → "Object Allocations"
2. 看分配速率最高的类
3. 定位到代码中创建对象最密集的位置

> 🎯 **核心要点**：
> - JMC是**JFR的分析工具**，一定要配合JFR使用才有意义
> - **Automated Analysis** 是最省力的功能——双击即可知道应用健康度
> - 火焰图找热点：**最宽的函数** = 最消耗资源的方法
> - 记住一句话：JFR是"录像机"，JMC是"播放器+分析器"

---

## 3. JConsole — 基础监控

### 3.1 JConsole简介

`JConsole` 是最基础的JVM图形化监控工具，JDK自带，无需额外安装。

```bash
# 启动JConsole
jconsole

# 连接指定进程
jconsole <PID>

# 连接远程JMX
jconsole <host>:<port>
```

### 3.2 核心面板

| 面板 | 功能 | 关键指标 |
|------|------|---------|
| **概览** | CPU/堆/类/线程总览 | 实时曲线 |
| **内存** | 堆各代使用详情 | Eden/Survivor/Old/Metaspace |
| **线程** | 线程列表+堆栈 | 线程数/死锁检测 |
| **类** | 类加载统计 | Loaded/Unloaded |
| **VM概要** | JVM参数/系统属性 | 启动参数情况 |

### 3.3 远程连接配置

**被监控应用**（需要开启JMX）：

```bash
# JVM参数开启JMX远程连接
java \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Djava.rmi.server.hostname=<ip_address> \
  -jar app.jar
```

> ⚠️ **安全警告**：生产环境务必开启 `ssl=true` 和 `authenticate=true`！

**连接方式**：

```bash
# 本地连接
jconsole <PID>

# 本地高级模式
jconsole -J-Djava.class.path=<jconsole.jar>:<管理MBean的JAR>

# 远程连接
jconsole 192.168.1.100:9999
```

### 3.4 JMX MBeans浏览

JConsole最强大的功能之一——直接操作MBean：

- 查看MBean属性（实时监控）
- 调用MBean操作（如触发GC、切换日志级别）
- 查看通知（事件告警）

```text
# 常用MBean路径
java.lang:type=Memory            → 堆使用、GC信息
java.lang:type=Threading         → 线程信息、死锁检测
java.lang:type=Runtime           → 运行时间、系统属性
java.lang:type=ClassLoading      → 类加载统计
java.nio:type=BufferPool,name=direct → 直接内存使用
```

> 🎯 **核心要点**：JConsole适合**开发环境快速查看**和**远程监督**。其最大优势是JDK内置、零配置、即开即用。但功能有限，不适合深度性能分析，线上环境建议优先使用JFR+JMC。

---

## 4. VisualVM — 全能可视化

### 4.1 VisualVM简介

**VisualVM** 是继JConsole之后更强大的可视化监控工具，曾经是JDK的一部分（JDK 6-8附带），现在需要独立下载。

```bash
# 下载并启动
wget https://github.com/oracle/visualvm/releases/latest
tar -xzf visualvm_*.tar.gz
cd visualvm/bin
./visualvm
```

### 4.2 核心功能

| 功能 | 说明 | 对比JConsole |
|------|------|-------------|
| 本地/远程监控 | 自动发现本地进程，支持JMX远程 | 功能类似，但UI更现代 |
| CPU采样 | 统计方法级别的CPU耗时 | JConsole无此功能 |
| 内存采样 | 统计对象分配频率和大小 | JConsole无此功能 |
| 线程分析 | 线程状态时间线 | 带有时间线可视化 |
| 堆Dump分析 | 打开.hprof文件分析对象引用 | JConsole无此功能 |
| GC可视化 | GC活动时间线 | 更直观 |
| 插件系统 | 丰富的扩展能力 | JConsole不支持 |

### 4.3 插件生态

VisualVM的强大在于其插件体系（Tools → Plugins）：

| 插件名称 | 功能 | 推荐 |
|---------|------|:----:|
| **BTrace Workbench** | 动态字节码追踪 | 强烈推荐 |
| **VisualVM-MBeans** | MBean增强浏览 | 推荐 |
| **VisualGC** | 实时GC可视化 | 强烈推荐 |
| **Tracer** | 自定义指标收集 | 进阶使用 |
| **HeapWalker** | 堆分析增强 | 推荐 |
| **Threads Inspector** | 线程分析增强 | 推荐 |

### 4.4 VisualGC插件实战

安装VisualGC后可以实时看到GC各代空间变化：

```text
VisualGC - 实时GC面板
┌──────────────────────┐ ┌──────────────────────┐
│     Eden             │ │     S0    │    S1     │
│     ████████░░░      │ │     ██░░  │    ░░░░   │
│     使用率: 72%      │ │     使用率: 35% │  0%   │
├──────────────────────┴───────────┴─────────────┤
│               Old Generation                    │
│               ████████████████░░                 │
│               使用率: 82%                        │
├────────────────────────────────────────────────┤
│               GC Activity Timeline               │
│   YGC│  │ │  ││ │  │ │  ││   │  │ │  ││  │  │   │
│   FGC│          │              │                │
│   ────────────────────────────────────────────  │
│   Time: 0:00:00                        0:02:00  │
└────────────────────────────────────────────────┘
```

### 4.5 BTrace集成

VisualVM可以通过插件集成BTrace（动态字节码追踪）：

> 💡 **BTrace原理**：在不重启应用的情况下，动态注入追踪代码到运行中的Java类中，实时输出方法调用参数、返回值和耗时。

```java
// BTrace脚本示例：监控指定方法的调用耗时
@BTrace
public class MethodTimer {
    @OnMethod(
        clazz = "com.example.service.OrderService",
        method = "createOrder",
        location = @Location(Kind.RETURN)
    )
    public static void traceCreateOrder(String userId, @Duration long duration) {
        println("=== OrderService.createOrder ===");
        println("userId: " + userId);
        println("duration(ms): " + duration / 1_000_000);
        println("===============================");
    }
}
```

> 🎯 **核心要点**：
> - VisualVM是"功能最全"的开源监控工具，**VisualGC插件**看GC变化最直观
> - 插件体系是VisualVM的差异化优势，**BTrace** 和 **VisualGC** 是必装插件
> - 适合**开发和测试环境**的深度分析，线上环境推荐JFR（更低开销）
> - 采样和 profiling 功能会加大应用开销，不要在线上长期开启采样

---

## 5. Arthas — 在线诊断神器（重点）

### 5.1 Arthas简介

**Arthas**（阿尔萨斯）是阿里开源的Java在线诊断工具。它的核心理念是"**在线**"——不需要重启应用、不需要修改代码、不需要配置Agent，直接Attach到目标进程后即可进行诊断。

```bash
# GitHub: https://github.com/alibaba/arthas
# 官方文档: https://arthas.aliyun.com/
```

### 5.2 快速开始

```bash
# 方式一：curl快速下载（推荐）
curl -O https://arthas.aliyun.com/arthas-boot.jar

# 方式二：本地使用（离线环境）
# 在连接外网的机器上下载后传到服务器

# 启动
java -jar arthas-boot.jar

# 启动后控制台会显示Java进程列表，输入序号选择要Attach的进程
# 或直接指定PID
java -jar arthas-boot.jar <PID>
```

**启动界面**：

```text
[INFO] arthas-boot version: 3.6.7
[INFO] Found existing java process, please choose one and hit RETURN.
* [1]: 23456 com.example.app.Application
  [2]: 12345 org.jetbrains.idea.maven.server.RemoteMavenServer

输入数字 1，进入Arthas控制台
```

### 5.3 `dashboard` — 实时系统面板

```bash
# 进入Arthas后，输入dashboard
dashboard
```

```text
ID     NAME                          GROUP           PRIORI   STATE     %CPU    TIME   INTERRUPTED DAEMON
40     SimplePauseTester              main            5        TIMED_WAITING 0       0:0    false   false
39     Common-Request-executor-1      main            5        WAITING      0       0:0    false   false
36     Catalina-utility-1             main            1        TIMED_WAITING 0       0:0    false   true
...
Memory                    used     total    max     usage    GC
heap                      1.2G     2.0G     4.0G    30.00%   gc.ps_scavenge.count     120
ps_eden_space             500M     1.0G     2.0G    50.00%   gc.ps_scavenge.time(ms)  2400
ps_survivor_space         50M      100M     100M    50.00%   gc.ps_marksweep.count    3
ps_old_gen                650M     900M     2.0G    72.22%   gc.ps_marksweep.time(ms) 1200

Runtime Info:
  os.name: Linux
  os.version: 3.10.0-1160.el7.x86_64
  java.version: 1.8.0_222
  java.home: /usr/local/java/jdk1.8.0_222/jre
  systemload.average: 2.50
  processors: 8
  uptime: 2 days 3 hours
```

> 💡 **dashboard解读**：
> - 左上角：所有线程列表及CPU使用时间（%CPU列）
> - 右侧上方：堆各代内存使用情况
> - 右侧下方：GC统计、运行时信息
> - `%CPU` 列排序：找到CPU占用最高的线程

### 5.4 `thread` — 线程分析

```bash
# 查看所有线程及CPU使用情况
thread

# 查看指定线程堆栈
thread <thread_id>

# 显示CPU使用率最高的前N个线程
thread -n 3

# 检测死锁
thread -b

# 查看线程状态统计
thread --state WAITING
```

**死锁检测三连**：

```bash
# 进入Arthas后
# Step 1: 检测是否有死锁
thread -b
# 输出：
# "Thread-A" Id=12 BLOCKED on java.lang.Object@1234 owned by "Thread-B" Id=13
#    at com.example.service.OrderService.createOrder(OrderService.java:45)
#    - waiting to lock <0x000000076b4f3e50> (a java.lang.Object)
#    - locked <0x000000076b4f3d50> (a java.lang.Object)

# Step 2: 查看死锁参与线程的堆栈
thread 12

# Step 3: 查看线程CPU占用TOP10
thread -n 10
```

### 5.5 `trace` — 方法调用链路+耗时统计

这是Arthas最强大的功能之一——跟踪方法调用链路和耗时。

```bash
# 跟踪指定方法，显示调用链路和每步耗时
trace com.example.service.OrderService createOrder

# 输出：
`---ts=2025-06-15 14:30:22;thread_name=http-nio-8080-exec-7;id=27;is_daemon=true;priority=5;
    `---[1.234567s] com.example.service.OrderService:createOrder()
        +---[0.003000ms] com.example.service.OrderService:validateOrder()        # 3μs
        +---[0.850000ms] com.example.dao.OrderDao:saveOrder()                     # 850μs
        +---[0.020000ms] com.example.dao.InventoryDao:deductInventory()           # 20μs
        +---[0.300000ms] com.example.mq.OrderProducer:sendMessage()               # 300μs
        `---[0.001000ms] com.example.service.OrderService:logOperation()          # 1μs

# 跟踪耗时超过阈值的方法（跳过太快的调用）
trace com.example.service.OrderService createOrder '#cost > 100'
```

**条件追踪**：

```bash
# 只追踪参数userId=1001的调用
trace com.example.service.OrderService createOrder "params[0].equals('1001')"

# 追踪异常抛出的链路
trace com.example.service.OrderService createOrder -e

# 多层级追踪（默认只追踪当前类，跳过JDK类）
trace com.example.service.OrderService createOrder --skip-jdk false
```

### 5.6 `watch` — 出入参+返回值观测

```bash
# 查看方法的入参和返回值
watch com.example.service.OrderService createOrder "{params,returnObj,throwExp}" -x 2

# 输出：
ts=2025-06-15 14:35:00; [cost=1.234567ms] result:
@ArrayList[
    @Object[][
        @String["user_1001"],                                   # 参数1
        @OrderRequest[
            productId=@String["prod_001"],                      # 请求体
            quantity=@Integer[2],
            address=@String["Beijing"],
        ],
    ],
    @OrderVO[                                                    # 返回值
        orderId=@String["order_20250615001"],
        status=@String["CREATED"],
        totalAmount=@BigDecimal["299.00"],
    ],
    null,                                                        # 异常（无异常）
]

# 只观察异常情况
watch com.example.service.OrderService createOrder "{params,throwExp}" -e -x 2

# 条件观察：只输出返回值金额>1000的调用
watch com.example.service.OrderService createOrder "{returnObj}" \
  "returnObj.totalAmount > 1000"
```

### 5.7 `stack` — 调用路径追踪

`stack` 查看当前方法的调用来源（从哪个方法被一步步调用过来的）：

```bash
# 查看createOrder方法被哪些调用链出发
stack com.example.service.OrderService createOrder

# 输出：
`---ts=2025-06-15 14:40:00
    `---[1.2345ms] com.example.service.OrderService:createOrder()
        `---[0.034ms] com.example.controller.OrderController:submitOrder()
            `---[0.012ms] org.springframework.web.method.support.InvocableHandlerMethod:doInvoke()
                `---[0.008ms] org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter:invokeHandlerMethod()
                    ...

# 只看来自特定来源的调用
stack com.example.service.OrderService createOrder \
  "stack[1].getMethodName().contains('batch')"
```

### 5.8 `redefine` — 热替换Class（紧急修复）

> ⚠️ **红牌警告**：redefine是"核武器"，只有紧急情况才使用！它只能替换方法体，不能增减字段或方法。

```bash
# 场景：线上发现某个方法有bug，紧急修改后编译替换
# Step 1: 在本地修改源码并编译
javac OrderService.java

# Step 2: 上传编译后的class文件到服务器
scp OrderService.class user@server:/tmp/

# Step 3: 在Arthas中热替换
redefine /tmp/OrderService.class

# 验证
redefine /tmp/OrderService.class
# 输出: redefine success, size: 1, classes: com.example.service.OrderService
```

**热替换的限制**：

| 项目 | 支持 | 不支持 |
|------|:----:|:------:|
| 方法体修改 | yes | - |
| 添加/删除方法 | - | no |
| 添加/删除字段 | - | no |
| 修改类继承关系 | - | no |
| 添加/删除注解 | - | no |
| 修改方法签名 | - | no |

> 💡 **redefine应用场景**：
> - 紧急修复空指针异常（增加null判断）
> - 临时增加日志输出（debug用）
> - 紧急修改配置值（硬编码修改）
> - **事后必须补正常发版流程！**

### 5.9 实战案例：接口突然变慢

> 💡 **场景**：线上接到告警，下单接口响应时间P99从50ms飙升到5s。

**Arthas排查全流程**：

```bash
# Step 1: 进入Arthas，看整体情况
dashboard
# → 发现CPU使用率正常（~30%），但活跃线程数异常高，大量线程TIMED_WAITING

# Step 2: 看线程状态分布
thread --state TIMED_WAITING
# → 发现大量线程在等待数据库连接

# Step 3: 追踪下单接口的调用耗时
trace com.example.controller.OrderController submitOrder '#cost > 1000'

# 输出关键信息：
`---[5.2s] com.example.controller.OrderController:submitOrder()
    +---[5.0s] com.example.service.OrderService:createOrder()
        +---[4.8s] com.example.dao.OrderDao:batchInsert()
        |   `---[4.8s] org.apache.ibatis.session.SqlSession:insert()  # 慢在这里！
        +---[0.1s] com.example.service.InventoryService:deduct()

# Step 4: 确认是batchInsert慢，进一步查看具体SQL
watch com.example.dao.OrderDao batchInsert "{params}" -x 3
# → 发现一次batchInsert传入5000条数据！

# 结论：batchInsert一次插入5000条数据导致SQL执行慢
# 根因：上游调用方修改了批量大小但没有通知，导致单次批量插入数据量过大
# 解决方案：限制单次批量大小不超过500条，或对批量插入进行分页
```

### 5.10 其他实用命令速查

| 命令 | 作用 | 示例 | 场景 |
|------|------|------|------|
| `sc` | 查看JVM已加载的类信息 | `sc -d com.example.service.OrderService` | 确认class是否加载、版本号 |
| `sm` | 查看已加载类的方法 | `sm com.example.service.OrderService` | 查看类有哪些方法 |
| `jad` | 反编译已加载的类 | `jad com.example.service.OrderService` | 查看线上运行的代码是否与本地一致 |
| `mc` | 在线编译Java文件 | `mc /tmp/OrderService.java -d /tmp` | 配合redefine使用 |
| `ognl` | 执行表达式 | `ognl '@java.lang.System@getProperty("user.dir")'` | 查看配置、调用方法 |
| `heapdump` | 导出堆快照 | `heapdump /tmp/heap.hprof` | 类似jmap dump |
| `vmtool` | 强制GC | `vmtool --action forceGc` | 触发Full GC |
| `logger` | 查看和修改日志级别 | `logger --name ROOT --level debug` | 动态改日志级别 |

**修改日志级别实战**：

```bash
# 查看当前所有logger
logger

# 修改指定包的日志级别为debug（无需重启）
logger --name com.example.service --level debug

# 验证
logger --name com.example.service
```

> 🎯 **核心要点**：
> - Arthas最核心的思想是"**在线**"——不重启、不部署、不配置，Attach进程即可
> - 掌握这三板斧："**trace** 看慢在哪，**watch** 看入参返回值，**stack** 看谁调的"
> - `thread -b` 一键检测死锁，比jstack更直观
> - `dashboard` 是实时监控的入口，看系统整体健康度
> - `redefine` 是"核武器"，紧急修复用，事后必须走正常发布
> - `logger --name --level debug` 动态改日志级别是最常用的"第一排查手段"
> - 建议记住一句话口诀：**trace慢，watch看，stack查，thread杀**

---

## 6. async-profiler — 高性能采样

### 6.1 async-profiler简介

**async-profiler** 是一个基于 **Linux perf** 的低开销采样分析器，支持CPU、内存和锁采样。

```bash
# 下载
wget https://github.com/async-profiler/async-profiler/releases/latest/download/async-profiler-2.9-linux-x64.tar.gz
tar -xzf async-profiler-*.tar.gz
cd async-profiler-*/

# 项目地址: https://github.com/async-profiler/async-profiler
```

### 6.2 CPU采样

```bash
# CPU采样30秒，生成火焰图
./profiler.sh -e cpu -d 30 -f /tmp/cpu_flame.html <PID>

# 采样后会在/tmp/cpu_flame.html生成交互式火焰图
# 浏览器打开：火焰越宽的函数，CPU占用越高
```

### 6.3 内存采样

```bash
# 内存分配采样30秒
./profiler.sh -e alloc -d 30 -f /tmp/alloc_flame.html <PID>

# 锁采样30秒
./profiler.sh -e lock -d 30 -f /tmp/lock_flame.html <PID>

# 综合采样（CPU + 内存）
./profiler.sh -e cpu,alloc -d 30 -f /tmp/combo_flame.html <PID>
```

### 6.4 火焰图解读

```text
CPU火焰图（从上往下看）：
          ┌─────────────────┐
          │    Thread.run   │ ← 顶部：线程入口
          ├────────┬────────┤
          │  Controller  │ 其他  │
          ├────────┼────────┤
          │  Service     │      │
          ├────┬──────┤        │
          │ DAO│  MQ  │        │
          └────┴──────┘        │
           ↑           ↑
    最宽 = 最热   窄 = 不那么热
```

| 火焰图类型 | 分析目标 | 典型使用场景 |
|-----------|---------|-------------|
| CPU | CPU热点函数 | 高CPU问题 |
| Alloc | 对象分配热点 | OOM/GC频繁 |
| Lock | 锁竞争热点 | 线程阻塞 |

> 🎯 **核心要点**：async-profiler基于Linux perf_events，对应用几乎零侵入，是目前Linux上最好的采样分析工具之一。生成的交互式HTML火焰图可以在浏览器中放大、缩小、搜索，是性能调优的"地图"。

---

## 7. GC日志分析

### 7.1 GC日志开启

```bash
# JDK 8 及以前
-XX:+PrintGCDetails \
-XX:+PrintGCDateStamps \
-XX:+PrintGCTimeStamps \
-Xloggc:/data/logs/gc.log

# JDK 9+ 统一日志
-Xlog:gc*:file=/data/logs/gc.log:time,uptime,level,tags

# 推荐的生产环境配置
-XX:+PrintGCDetails \
-XX:+PrintGCDateStamps \
-XX:+PrintGCTimeStamps \
-XX:+PrintGCApplicationStoppedTime \    # 打印应用暂停时间
-XX:+PrintTenuringDistribution \         # 打印对象晋升分布
-Xloggc:/data/logs/gc-$(date +%Y%m%d).log \
-XX:+UseGCLogFileRotation \              # 日志滚动
-XX:NumberOfGCLogFiles=10 \
-XX:GCLogFileSize=10M
```

### 7.2 各GC收集器日志格式

#### Parallel GC（JDK 8默认）

```text
2025-06-15T14:30:00.123+0800: 120.456: [GC (Allocation Failure) [PSYoungGen: 524288K->65536K(611456K)] 1048576K->589824K(2097152K), 0.0250000 secs] [Times: user=0.10 sys=0.02, real=0.03 secs]
# 解读：
# 时间: 2025-06-15T14:30:00.123+0800
# 启动后: 120.456秒
# GC类型: Young GC (Allocation Failure = 分配失败触发)
# 新生代: 524288K → 65536K (容量611456K) = 回收了458752K
# 整个堆: 1048576K → 589824K (容量2097152K)
# 耗时: 0.025秒 = 25ms，良好
# user=0.10: 用户态CPU耗时
# sys=0.02: 内核态CPU耗时
# real=0.03: 实际耗时（墙钟时间）
```

#### G1 GC

```text
2025-06-15T14:30:00.123+0800: 120.456: [GC pause (G1 Evacuation Pause) (young) 2048M->1200M(4096M), 0.0350000 secs]
# 年轻代Mixed GC暂停，35ms，良好

2025-06-15T14:35:00.123+0800: 420.456: [GC pause (G1 Humongous Allocation) (young) (initial-mark) 3500M->3200M(4096M), 0.5000000 secs]
# 大对象分配触发的并发标记启动，500ms，需要关注

2025-06-15T14:36:00.123+0800: 480.456: [GC pause (G1 Evacuation Pause) (mixed) 4000M->1500M(4096M), 0.1200000 secs]
# Mixed GC（同时回收年轻代+部分老年代），120ms
```

#### CMS GC（JDK 9已弃用）

```text
2025-06-15T14:30:00.123+0800: 120.456: [GC (Allocation Failure) 120.456: [ParNew: 524288K->65536K(611456K), 0.0200000 secs] 1048576K->589824K(2097152K), 0.0220000 secs] [Times: user=0.08 sys=0.01, real=0.02 secs]
# ParNew: 并行新生代回收，20ms

# CMS初始标记（会STW）
2025-06-15T14:31:00.123+0800: 180.456: [GC (CMS Initial Mark) [1 CMS-initial-mark: 524288K(1048576K)] 589824K(2097152K), 0.0010000 secs]
# 只标记GC Roots直接引用的对象，1ms极短

# CMS并发标记
2025-06-15T14:31:00.124+0800: 180.457: [CMS-concurrent-mark-start]
2025-06-15T14:31:00.524+0800: 180.857: [CMS-concurrent-mark: 0.400/0.400 secs]
# 与应用并发执行，不STW
```

### 7.3 GCViewer / GCEasy 可视化分析

> 💡 **推荐**：GC日志太长时，使用可视化工具一键分析

| 工具 | 地址 | 特点 |
|------|------|------|
| **GCViewer** | GitHub开源 | 本地运行，图表丰富 |
| **GCEasy** | gceasy.io | 网页版上传即可，自动诊断 |
| **GCeasy** | gc-easy.com | 同GCEasy，在线分析 |

**GCEasy自动诊断输出**：

```text
📊 Heap Analysis Report
───────────────────────────────
Key Performance Indicators:
  Throughput: 98.5% (阈值: >99%, ⚠️ 略低于标准)
  Avg Pause Time: 85ms (阈值: <200ms, ✅ 正常)
  Max Pause Time: 1.2s (阈值: <1s, ❌ 超标!)
  GC Frequency: 12次/分钟 (阈值: <10次/分钟, ⚠️ 偏高)

Diagnosis:
  1. Full GC: 发现3次Full GC，平均耗时850ms
     → 建议检查老年代使用率，排查内存泄漏
  2. Max Pause: 单次GC暂停1.2s，超过1s阈值
     → 建议优化GC配置或增加堆内存
  3. GC Throughput: 98.5%略低，建议优化
```

### 7.4 GC日志分析关键指标

| 指标 | 健康值 | 警告值 | 危险值 |
|------|:------:|:------:|:------:|
| FGC次数 | 0次/天 | 1-2次/天 | >5次/天 |
| YGC平均耗时 | <50ms | 50-100ms | >100ms |
| FGC平均耗时 | 0ms | <1s | >1s |
| GC吞吐量 | >99% | 95-99% | <95% |
| GC暂停总时间 | <1%运行时间 | 1-5% | >5% |
| YGC频率 | <1次/分钟 | 1-10次/分钟 | >10次/分钟 |

> 🎯 **核心要点**：
> - GC日志是判断GC健康度的第一手资料，**生产环境必须开启**GC日志
> - GC日志分析三连问：**FGC次数（有或没有？）、单次暂停时间（是否超过1s？）、GC吞吐量（是否>99%？）**
> - 重点关注 `real=` 值（实际暂停时间），`user+sys` 是多核并行总耗时
> - G1 GC的 `mixed` 暂停正常，CMS的 `concurrent` 阶段不暂停应用
> - 推荐使用 GCEasy 自动分析，比自己肉眼读日志高效百倍

---

## 8. 工具选型速查

### 8.1 按场景选型

| 场景 | 首选工具 | 备选 | 说明 |
|------|---------|------|------|
| 快速了解应用状态 | `dashboard` (Arthas) | JConsole | 看线程数/堆使用/内存情况 |
| 实时GC监控 | `jstat -gcutil` | VisualVM+VisualGC | 确认GC频率和耗时 |
| GC日志分析 | GCEasy | GCViewer | 上传日志看自动分析报告 |
| CPU热点排查 | `async-profiler`火焰图 | `trace` (Arthas) | 找最消耗CPU的方法 |
| 接口变慢 | `trace` (Arthas) | JFR+JMC | 定位方法级耗时 |
| 线程死锁 | `thread -b` (Arthas) | `jstack -l` | 一键检测 |
| CPU飙高 | 四步法(jstack) | `thread -n` (Arthas) | 定位有问题的代码行 |
| OOM排查 | `jmap -dump` + MAT | `heapdump` (Arthas) | 导出堆快照分析 |
| 内存泄漏分析 | Eclipse MAT | JProfiler | 离线分析hprof文件 |
| 在线日志级别修改 | `logger` (Arthas) | JMX MBean | 动态改日志级别debug |
| 热替换Class | `redefine` (Arthas) | - | 紧急修复（慎用） |
| 远程监控 | JMC (JMX连接) | VisualVM | 远程连接查看 |
| 生产环境长期监控 | JFR (default级别) | Prometheus+Granfana | 低开销持续记录 |
| 方法入参返回值 | `watch` (Arthas) | BTrace | 快速定位参数问题 |
| 调用链路追踪 | `trace` (Arthas) | SkyWalking | 方法调用耗时分析 |

### 8.2 按环境选型

**开发环境**：
```
推荐工具：VisualVM + VisualGC插件
特点：功能全面、可视化好、方便本地调试
```

**测试/预发环境**：
```
推荐工具：Arthas + JFR
特点：在线诊断灵活、随时可以attach、调试方便
```

**生产环境**：
```
推荐优先级：
  1️⃣ JFR（default级别）—— 长期开启，出事回溯
  2️⃣ Arthas（按需attach）—— 即时诊断，用完即走
  3️⃣ GC日志 —— 必须开启，事后分析
  4️⃣ async-profiler（按需）—— 深度性能采样
```

### 8.3 一句话总结

```text
jps → 找进程
jstat → 看GC
jstack → 抓线程
jmap → 分析内存
JFR → 零成本记录
JMC → 可视化分析
Arthas → 在线诊断全能王
async-profiler → 火焰图专家
VisualVM → 本地开发神器
```

### 8.4 面试高频问题

> 💡 **面试官常问**：
> 1. "生产环境如何零开销采集性能数据？" → JFR default模式 + jcmd动态启停
> 2. "Arthas相比传统JDK工具有什么优势？" → 在线attach、trace/watch/stack三连、redefine热替换
> 3. "接口突然变慢，只给你一台机器的权限怎么排查？" → Arthas trace定位慢方法
> 4. "怎么不重启修改日志级别？" → Arthas logger命令或jinfo动态修改
> 5. "JFR和Arthas分别适合什么场景？" → JFR长期记录+回溯、Arthas即时诊断
> 6. "GC日志怎么看有没有问题？" → 看FGC次数、单次暂停时间、GC吞吐量
> 7. "火焰图怎么看？" → 最宽的函数=热区，从顶部到底部=调用链路
> 8. "线上OOM了怎么办？" → 先看HeapDumpOnOutOfMemoryError配置，再用jmap -dump或Arthas heapdump导出堆，最后用MAT分析

---

> 🎯 **总结**：JDK性能监控与调优工具经历了"命令行→可视化→在线诊断→零开销采样"的进化。建议分层掌握：**基础层**（jstat/jstack/jmap），**现代层**（JFR/JMC），**在线诊断层**（Arthas），**深度分析层**（async-profiler/MAT）。面试和工作中，Arthas和JFR是最高频提到的两个武器。

---

**返回总览**：[总览](./00-JDK知识体系总览.md)
