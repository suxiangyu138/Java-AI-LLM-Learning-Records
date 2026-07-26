# 10 — JVM调优与监控实战
> 定位：掌握jps/jstat/jinfo/jmap/jstack/jcmd六大数据采集工具、GC日志格式与可视化分析、三套生产级调优案例（Full GC频繁/元空间OOM/CPU飙升）的完整排查链路

## 目录
1. [JVM调优全景与方法论](#1-jvm调优全景与方法论)
2. [命令行工具：jps/jstat/jinfo/jmap/jstack/jcmd](#2-命令行工具jpsjstatjinfojmapjstackjcmd)
3. [可视化工具：JVisualVM/JMC/Arthas](#3-可视化工具jvisualvmjmcarthas)
4. [GC日志格式详解（JDK 8 vs JDK 9+ Unified Logging）](#4-gc日志格式详解jdk-8-vs-jdk-9-unified-logging)
5. [GC日志可视化分析（GCViewer/GCEasy）](#5-gc日志可视化分析gcviewergceasy)
6. [案例一：微服务接口频繁卡顿（Full GC排查）](#6-案例一微服务接口频繁卡顿full-gc排查)
7. [案例二：批处理任务OOM（元空间溢出）](#7-案例二批处理任务oom元空间溢出)
8. [案例三：高并发接口CPU飙升（死循环/JIT编译问题）](#8-案例三高并发接口cpu飙升死循环jit编译问题)
9. [堆转储文件分析（MAT/JProfiler）](#9-堆转储文件分析matjprofiler)
10. [JVM参数速查表（堆/栈/元空间/GC/调试）](#10-jvm参数速查表堆栈元空间gc调试)
11. [面试高频考点](#11-面试高频考点)

---

## 1. JVM调优全景与方法论

### 1.1 监控五大维度

| 监控维度 | 核心指标 | 线上故障场景 | 对应工具 |
|----------|---------|-------------|---------|
| 堆内存 | Eden/Survivor/Old使用率、元空间、堆外内存 | OOM、内存泄漏 | jstat、jmap、MAT |
| GC | YGC/FGC次数、单次耗时、总GC耗时、晋升速率 | 接口RT飙升、服务卡顿 | jstat、GC日志、GCEasy |
| 线程 | 线程总数、RUNNABLE/BLOCKED/WAITING、死锁 | 服务假死、请求堆积、CPU高 | jstack、Arthas |
| CPU | 进程总CPU、GC线程CPU、热点方法占用 | 死循环、频繁反射/序列化 | top、jstack、async-profiler |
| 类加载 | 加载/卸载类数量、类加载耗时 | 元空间持续上涨、代理类泄漏 | jstat -class、-XX:+TraceClassLoading |

### 1.2 调优核心原则

> 💡 **调优本质**：理解机制 + 适配场景 + 基于数据。不是盲目堆参数，而是用数据驱动决策。

```
① 先调代码，再调参数 —— 代码内存泄漏，参数再好也无效
② 参数适配场景 —— 微服务（低延迟 G1/ZGC）vs 批处理（高吞吐 Parallel）
③ 监控先行 —— 基于 GC 日志 + 内存快照 + CPU 数据，拒绝盲调
④ 循序渐进 —— 每次只调 1-2 个参数，验证效果后再继续
```

### 1.3 高频避坑清单

| 避坑项 | 说明 |
|--------|------|
| `-Xms` ≠ `-Xmx` | 堆频繁扩容/缩容造成性能损耗，生产环境务必设相等 |
| 堆内存盲目过大 | 10GB→32GB，Full GC 停顿从 300ms 跃至 1s+ |
| 忽略元空间 | JDK 8+ 动态类过多仍会 OOM，须设 `-XX:MaxMetaspaceSize` |
| 过度依赖 JIT | 热点代码太复杂导致 JIT 编译失败，CPU 不降反升 |
| 关闭 `UseCompressedOops` | 堆超 32GB 时自动关闭，指针膨胀浪费内存 |

> ⚠️ **生产禁忌**：高峰期不要执行 `jmap -histo:live` 或 `jmap -dump`，它们会触发 Full GC 造成业务卡顿。优先使用 Arthas 的无 GC 快照功能。

---

## 2. 命令行工具：jps/jstat/jinfo/jmap/jstack/jcmd

### 2.1 jps — 定位Java进程（排查第一步）

```shell
jps -q                         # 仅输出PID（纯净版）
jps -l                         # PID + 主类全限定名（多服务区分）
jps -v                         # PID + 启动JVM参数（核对-Xmx等配置）
```

> 替代 `ps -ef | grep java`，只筛选 JVM 进程。注意 Alpine 精简 JDK 可能缺失此工具。

### 2.2 jstat — GC/类加载实时监控（GC排查核心）

通用格式：`jstat -选项 PID 间隔ms 打印次数`

```shell
# 每秒打印GC完整数值，持续输出
jstat -gc 1234 1000

# 百分比展示各区使用率，快速判断内存压力
jstat -gcutil 1234 1000

# 监控类加载/卸载数量（排查元空间泄漏）
jstat -class 1234
```

**jstat -gcutil 输出示例及注解：**

```text
# jstat -gcutil 1234 1000 3
  S0     S1     E      O      M     YGC    YGCT    FGC    FGCT    GCT
  0.00   8.45  65.23  72.18  92.34  123    2.345    5    1.234   3.579
  0.00   8.45  68.91  73.05  92.34  124    2.367    5    1.234   3.601
  0.00   8.45  72.34  73.92  92.35  125    2.390    5    1.234   3.624
```

| 字段 | 含义 | 诊断信号 |
|------|------|---------|
| S0 / S1 | Survivor 0/1 区使用率（%） | 若长期接近 100%，说明 Survivor 过小 |
| E | Eden 区使用率（%） | 快速上涨 → 对象创建速率高 |
| O | 老年代使用率（%） | 持续上涨不降 → 内存泄漏 |
| M | 元空间使用率（%） | 持续上涨 → 类加载泄漏 |
| YGC / YGCT | Young GC 次数 / 累计耗时（秒） | 频率过高 → 新生代过小或对象分配过快 |
| FGC / FGCT | Full GC 次数 / 累计耗时（秒） | **持续上涨 = 严重内存问题** |
| GCT | GC 总耗时 | 占运行时间比例过高则卡顿严重 |

### 2.3 jinfo — 动态查看/修改JVM参数

```shell
jinfo 1234                              # 查看全部JVM参数+系统属性
jinfo -flag MaxHeapSize 1234            # 单独查看某参数
jinfo -flag +PrintGCDetails 1234        # 动态开启GC日志（无需重启）
```

> ⚠️ 仅标记为 `manageable` 的参数支持动态修改。堆大小、收集器类型**不可**动态调整。

### 2.4 jmap — 堆内存快照（内存泄漏/OOM定位）

```shell
jmap -heap 1234                         # 查看堆分区配置、GC收集器、内存概况
jmap -histo:live 1234                   # 打印存活对象直方图（按占用排序，会触发Full GC）
jmap -dump:format=b,file=heap.hprof 1234  # 生成hprof堆快照文件
```

**生产最佳实践**：JVM 启动参数配置 OOM 自动 dump，避免宕机丢失现场。

```shell
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/log/heap.hprof
```

### 2.5 jstack — 线程栈快照（死锁/CPU高/阻塞专用）

```shell
jstack -l 1234 > thread_dump.log      # 输出线程快照到文件，带锁详情
jstack -F 1234                         # 进程无响应时强制打印（慎用，会STW）
```

**三大实战场景：**

| 场景 | 操作 |
|------|------|
| **死锁排查** | jstack 日志中出现 `Found one Java-level deadlock`，直接定位两行互斥锁代码 |
| **CPU高定位** | `top -Hp PID` → 记下最高CPU线程十进制ID → `printf "%x" ID`转16进制 → jstack中搜索 `nid=0x...` |
| **请求堆积** | 大量 BLOCKED 线程卡在同一把锁 → 锁竞争剧烈 |

### 2.6 jcmd — 全能整合工具（JDK 7+推荐）

集成 jps/jstat/jstack/jmap/jinfo 全部能力，一站式诊断。

```shell
jcmd 1234 help                          # 查看支持指令列表
jcmd 1234 Thread.print                  # 等价 jstack
jcmd 1234 GC.heap_info                  # 等价 jmap -heap
jcmd 1234 GC.class_histogram            # 等价 jmap -histo
jcmd 1234 VM.flags                      # 等价 jinfo -flags
jcmd 1234 JFR.start duration=60s filename=rec.jfr  # 开启飞行记录
```

### 2.7 六工具速查表

| 工具 | 一句话用途 | 典型命令 |
|------|-----------|---------|
| **jps** | 找Java进程PID | `jps -l -v` |
| **jstat** | 看GC统计/类加载 | `jstat -gcutil PID 1s` |
| **jinfo** | 查/改JVM参数 | `jinfo -flag MaxHeapSize PID` |
| **jmap** | 堆Dump/对象直方图 | `jmap -dump:format=b,file=heap.hprof PID` |
| **jstack** | 线程栈快照 | `jstack -l PID` |
| **jcmd** | 全能整合 | `jcmd PID help` |

---

## 3. 可视化工具：JVisualVM/JMC/Arthas

### 3.1 JVisualVM — JDK自带GUI监控

```shell
# JDK 8 自带
jvisualvm
# 新版下载：https://visualvm.github.io/
```

| 功能 | 用途 |
|------|------|
| Monitor 面板 | 实时曲线：CPU、堆、Metaspace、类、线程 |
| Sampler | CPU/内存采样，低开销 |
| Visual GC 插件 | 直观展示 Eden/Survivor/Old 区 GC 全过程 |
| Heap Dump | 可视化浏览对象分布、引用链 |
| 线程面板 | 一键检测线程死锁 |

**远程连接**：服务端启动 JMX 参数。

```shell
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9010 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false
```

### 3.2 JMC + JFR — 生产低损耗监控

JFR（Java Flight Recorder）是 JDK 内置极低开销采样工具（开销 < 2%），是生产环境唯一允许长时间采样的内置工具。

```shell
# JVM 参数开机自动录制
-XX:+StartFlightRecording,duration=60s,filename=recording.jfr

# jcmd 手动动态开启
jcmd PID JFR.start duration=60s filename=rec.jfr
jcmd PID JFR.dump filename=rec.jfr
jcmd PID JFR.stop
```

> 💡 JFR 核心价值：不停业务、完整记录 GC、锁竞争、方法耗时、IO、线程阻塞事件，是生产调优的"黑匣子"。

### 3.3 Arthas — 阿里在线诊断神器

无需重启、无需修改启动参数，微服务线上排查首选。

```shell
# 启动 Arthas 并选择目标进程
java -jar arthas-boot.jar
```

**高频命令速查：**

| 命令 | 用途 |
|------|------|
| `dashboard` | 总面板：CPU、内存、线程、GC 实时汇总 |
| `thread -n 10` | 按 CPU 排序线程，定位热点 |
| `thread -b` | 查看当前 BLOCKED 的线程 |
| `trace 包.类 方法` | 追踪方法完整调用栈及每层耗时 |
| `watch 包.类 方法 "{params,returnObj}"` | 观测方法入参和返回值 |
| `heapdump /tmp/heap.hprof` | 生成堆快照（不触发 Full GC） |
| `jad 包.类` | 反编译线上类，核对代码版本 |
| `memory` | 内存使用详情 |

> 💡 与 jmap 的关键区别：Arthas 的 `heapdump` **不触发 Full GC**，可以在高峰期安全使用。

---

## 4. GC日志格式详解（JDK 8 vs JDK 9+ Unified Logging）

### 4.1 JDK 8 GC日志

**启用参数：**

```shell
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-XX:+PrintGCTimeStamps
-XX:+PrintHeapAtGC
-XX:+PrintTenuringDistribution
-Xloggc:gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=14
-XX:GCLogFileSize=50m
```

**日志格式逐字段解析：**

```log
# Young GC (Parallel 收集器)
# 格式：[时间戳][相对时间]: [GC类型 (原因)] [新生代: 使用前->使用后(总量)] [整个堆: 使用前->使用后(总量)], 耗时秒]
2026-07-26T10:00:00.123+0800: 0.456: [GC (Allocation Failure)
  [PSYoungGen: 649728K->7616K(749568K)] 821056K->179456K(1953536K),
  0.0102345 secs]
  [Times: user=0.02 sys=0.00, real=0.01 secs]
│                          │ │            │               │
│                          │ │ 新生代回收量 │ 整堆回收量    │ 耗时
│                          │ │ 649MB→7MB  │ 821MB→179MB  │ 10ms
│                  分配失败触发
```

```log
# Full GC (Parallel 收集器)
2026-07-26T10:01:00.456+0800: 61.456: [Full GC (Allocation Failure)
  [PSYoungGen: 7616K->0K(749568K)]
  [ParOldGen: 171840K->175106K(1203968K)]
  179456K->175106K(1953536K),
  [Metaspace: 34560K->34560K(1083392K)], 0.2345678 secs]
  [Times: user=0.45 sys=0.02, real=0.23 secs]
│                               │              │              │
│                         老年代几乎未减少      元空间稳定      耗时234ms
│                         → 疑似内存泄漏                    │
│                                                      user=0.45 > real=0.23
│                                                      说明多线程并行回收
```

> ⚠️ **诊断信号**：Full GC 后老年代使用率几乎不变（如 171840K→175106K 反而略增），是**内存泄漏**的典型标志。

### 4.2 JDK 9+ 统一日志系统

**语法：** `-Xlog:[tag][:[level][:[decorators][:output]]]`

```shell
# 常用配置（推荐）
-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=10,filesize=50m

# 精细分类
-Xlog:gc+heap=debug:file=gc-heap.log          # 堆信息详情
-Xlog:gc+age=trace:file=gc-age.log             # 对象年龄分布
-Xlog:gc+ergo=info:file=gc-ergo.log            # 自适应调节
-Xlog:safepoint:file=safepoint.log             # 安全点日志
```

**G1 GC 日志逐字段解析：**

```log
# G1 Young GC (JDK 17)
[0.234s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
   [0.234s][info][gc]     Eden regions: 128->0(128)
   [0.234s][info][gc]     Survivor regions: 8->16(16)
   [0.234s][info][gc]     Old regions: 32->32
   [0.234s][info][gc]     Humongous regions: 0->0
   [0.234s][info][gc]     Metaspace: 120M->120M(256M)
   [0.236s][info][gc]     0.002s, user=0.01s, sys=0.00s, real=0.002s
│                          │    │     │        │
│                     Eden全部回收  存活8个→16  Old不变   停顿仅2ms
│                     Region数=128  Survivor             │
│                                                  远小于200ms目标
```

**ZGC 日志：**

```log
[0.456s][info][gc] GC(0) Garbage Collection (Allocation Rate)
   [0.456s][info][gc]     Major Mark: 4M->4M(4096M)
   [0.456s][info][gc]     Minor Mark: 10M->10M(1024M)
   [0.456s][info][gc]     Relocation: 0.001s
   [0.456s][info][gc]     Stats: 0.001s user, 0.000s sys, real=0.001s
```

### 4.3 关键分析指标

| 指标 | 计算方式 | 健康范围 |
|------|---------|---------|
| GC 频率 | 两次 GC 的时间间隔 | Young GC: 数十秒~数分钟；Full GC: 最好 0 |
| GC 停顿 | STW 时长 | Young < 100ms；Full < 500ms |
| 吞吐量 | 1 - (GC总时间 / 运行总时间) | > 99% 优秀，> 95% 合格 |
| 晋升速率 | Young GC 后 Old 区增量 / 时间 | 波动不大视为正常 |
| 分配速率 | Eden 使用增量 / 时间 | 与业务 QPS 正相关 |

---

## 5. GC日志可视化分析（GCViewer/GCEasy）

### 5.1 GCEasy （在线首选）

**地址：** https://gceasy.io

上传 GC 日志文件后自动生成可视化报告，包含：

| 报告模块 | 内容 |
|----------|------|
| **吞吐量** | 应用时间 vs GC 时间占比 |
| **停顿时间** | 最大停顿、平均停顿、P99 停顿 |
| **堆使用趋势** | 堆使用率随时间变化曲线，直观展示内存泄漏 |
| **GC 原因分布** | 各类 GC 触发原因占比 |
| **优化建议** | AI 自动分析给出参数调整建议 |

### 5.2 GCViewer （开源离线工具）

```shell
# 启动（需 Java 环境）
java -jar gcviewer.jar gc.log
```

核心视图：

- **总览图**：蓝色=已用堆，绿色=新生代，红色=老年代，竖线=GC 事件
- **停顿图**：每根竖线高度代表 GC 停顿时长
- **吞吐量图**：累积 GC 时间占比

> 💡 **推荐组合**：线上紧急分析用 GCEasy（无需安装），离线/安全环境用 GCViewer（开源可审计）。

---

## 6. 案例一：微服务接口频繁卡顿（Full GC排查）

### 问题现象

电商订单服务，8 核 16GB，JDK 11 + G1 GC，接口响应从 200ms 飙升至 8s。

| 指标 | 正常 | 异常 |
|------|------|------|
| 接口响应 P99 | 200ms | **8s** |
| Young GC 频率 | 2 分钟/次 | **30 秒/次** |
| Full GC 持续时长 | 200ms | **5 秒** |
| 老年代使用率 | 40% | **98%** |

### 诊断链路

```text
Step 1: jinfo -flags <pid>
        → 发现 -Xms4g -Xmx8g（初始≠最大，堆频繁扩容）

Step 2: jstat -gcutil <pid> 1s 10
        → O 区从 72% 持续上涨至 98%，FGC 从 5 次涨至 12 次
        → 每次 Full GC 回收量极少（老年代几乎不变）

Step 3: jmap -dump:format=b,file=heap.hprof <pid>
        → MAT 分析支配树 → 发现百万级 Order 对象驻留

Step 4: 追溯 GC Root 引用链
        → 批量查询接口中使用 HashMap 缓存查询结果，无过期策略
        → 每次调用向缓存追加数据，只增不减
```

### 解决方案

**紧急止血：**

```shell
# 1. 触发一次 Full GC 临时释放
jmap -histo:live <pid>

# 2. 代码修复：关闭批量查询结果集缓存 + 增加 10 分钟过期策略
```

**长期 JVM 参数（16GB 服务器）：**

```shell
-Xms10g -Xmx10g                           # 初始=最大，避免堆震荡（60% 内存）
-XX:+UseG1GC                              # 显式启用 G1
-XX:MaxGCPauseMillis=200                  # 期望最大停顿
-XX:G1HeapRegionSize=8m                   # 适配大堆
-XX:InitiatingHeapOccupancyPercent=45     # 提前触发并发 GC
-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m
-XX:+PrintGCDetails -XX:+PrintGCDateStamps
-Xloggc:/var/log/jvm/gc.log
-XX:+UseStringDeduplication               # 字符串去重
```

### 验证效果

| 指标 | 调优前 | 调优后 |
|------|--------|--------|
| Young GC | 30 秒/次，停顿长 | 1-2 分钟/次，≤50ms |
| Full GC | 频繁，5 秒/次 | 0-1 次/天，≤300ms |
| 接口响应 P99 | 8s | 200-300ms |
| 老年代使用率 | 98% | <60% |

> 🎯 **根因总结**：代码级内存泄漏（无过期缓存的静态集合）是 Full GC 频繁的常见元凶。**先调代码，再调参数**是铁律。

---

## 7. 案例二：批处理任务OOM（元空间溢出）

### 问题现象

数据同步批处理服务，JDK 8，运行 3-4 天后 `java.lang.OutOfMemoryError: Metaspace`，服务崩溃。

### 诊断链路

```text
Step 1: 错误日志明确 "Metaspace" → 问题在元空间，非堆

Step 2: jstat -class <pid>
        → Loaded 持续增加（从 5000 → 35000），Unloaded 极少
        → 确认类加载泄漏

Step 3: 代码排查
        → 大量反射 + Spring 动态代理生成数据转换类
        → 每次批处理创建新的 Enhancer 实例，未缓存
```

### 解决方案

**代码优化（核心）：**

| 措施 | 效果 |
|------|------|
| 缓存动态代理类（Proxy/Enhancer 复用） | 避免每次批处理重新生成类 |
| 释放反射资源（Method/Field 解除引用） | 帮助 GC 回收类元数据 |
| 静态工具类替代部分动态生成 | 减少不必要的反射/代理 |

**JVM 参数：**

```shell
-XX:MetaspaceSize=512m -XX:MaxMetaspaceSize=1024m
-XX:+CMSClassUnloadingEnabled      # JDK 8 默认关闭，需显式开启
-XX:+TraceClassLoading             # 观察类加载详情
-XX:+TraceClassUnloading           # 观察类卸载情况
```

### 验证效果

> 稳定运行 7 天+，元空间使用率稳定 40%，类加载数趋于稳定（约 5000 个），GC 正常卸载无用类。

> ⚠️ **JDK 8 注意**：`CMSClassUnloadingEnabled` 默认关闭，大量动态代理场景须显式开启。JDK 9+ 的 G1 默认启用类卸载。

---

## 8. 案例三：高并发接口CPU飙升（死循环/JIT编译问题）

### 问题现象

支付接口，QPS 5000+，JDK 17 + ZGC，16 核 32GB。CPU 飙至 90%+，响应 50ms-1s 波动，**无 Full GC**。

> 💡 无 GC 问题但 CPU 高 → 排除 GC 因素，重点查业务线程和 JIT 编译。

### 诊断链路

```text
Step 1: top -H -p <pid>
        → 找到 CPU 最高的几个线程 ID（十进制）

Step 2: printf "%x" <threadId> 转 16 进制
        → jstack 日志中搜索 nid=0x...
        → 发现多个线程处于 "JIT编译" 状态（C2 CompilerThread）

Step 3: jinfo -flag CompileThreshold <pid>
        → CompileThreshold=10000（默认值）

Step 4: Arthas trace 热点方法
        trace com.pay.validator.PaymentValidator validate
        → 方法执行 5000+ QPS，但未被 JIT 编译
        → 每次走解释执行，CPU 开销巨大
```

### 解决方案

**JIT 参数调整：**

```shell
-XX:CompileThreshold=5000           # 降低阈值，更快触发 JIT
-XX:CICompilerCount=8              # 编译线程 = CPU 核数/2
-XX:+PrintCompilation              # 跟踪编译情况
-XX:+UseFastAccessorMethods        # 加速 getter/setter
```

**代码优化：**

- 热点方法（`validate()`）拆分简化：将大方法拆为多个小方法，便于 JIT 内联
- 避免频繁创建临时对象：改用对象池或局部变量复用
- 全局变量改局部变量：减少逃逸，利用栈上分配

### 验证效果

| 指标 | 调优前 | 调优后 |
|------|--------|--------|
| CPU 使用率 | 90%+ | 40-60% |
| 接口响应 P99 | 50ms-1s 波动 | 50-80ms 稳定 |
| 热点方法 | 解释执行 | JIT 编译，效率 +60% |

> 🎯 **根因总结**：高 QPS 下热点方法未被及时 JIT 编译，C2 编译器线程成为瓶颈。降低 `CompileThreshold` + 代码简化后 JIT 编译迅速生效。

---

## 9. 堆转储文件分析（MAT/JProfiler）

### 9.1 堆 Dump 获取

```shell
# 方式1：OOM 自动 dump（推荐）
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/log/heap.hprof

# 方式2：jmap 手动 dump（会触发 Full GC，低峰期使用）
jmap -dump:format=b,file=heap.hprof <pid>

# 方式3：Arthas 无 GC dump（线上安全）
heapdump /tmp/arthas-heap.hprof

# 方式4：jcmd 一站式
jcmd <pid> GC.heap_dump /path/to/heap.hprof
```

### 9.2 MAT 分析标准流程

| 步骤 | 操作 | 目的 |
|------|------|------|
| 1 | Histogram（直方图） | 按类统计实例数和占用内存，找到最大的类 |
| 2 | Dominator Tree（支配树） | 找出独占大量内存的顶级对象及其持有链 |
| 3 | Leak Suspects 报告 | MAT 自动推测可疑泄漏点 |
| 4 | Path to GC Roots | 追溯大对象的 GC Root 引用链，定位泄漏代码 |

**OQL 高级查询：**

```sql
-- 查询所有超 1MB 的 byte[]
SELECT * FROM byte[] s WHERE s.@length > 1048576

-- 查询所有 ArrayList 实例
SELECT * FROM java.util.ArrayList

-- 查找名称含 CACHE 的 HashMap
SELECT * FROM java.util.HashMap WHERE toString() LIKE '%CACHE%'
```

### 9.3 JProfiler

商业工具，适用于复杂性能调优。

| 功能 | 说明 |
|------|------|
| CPU 调用树 | 精确采样，区分业务/框架耗时 |
| 中间件探针 | 数据库、MQ、HTTP 专属分析 |
| 锁竞争图谱 | 线程阻塞可视化 |

> 💡 **工具选型**：MAT 是免费开源的内存泄漏分析首选；JProfiler 适合全链路性能分析的付费场景。

---

## 10. JVM参数速查表（堆/栈/元空间/GC/调试）

### 10.1 堆与栈参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `-Xms` | 物理内存 1/64 | 初始堆大小，生产建议 = `-Xmx` |
| `-Xmx` | 物理内存 1/4 | 最大堆大小，不超过物理内存 70% |
| `-Xmn` | 堆的 1/3~1/4 | 新生代大小 |
| `-Xss` | 平台依赖（约 1MB） | 线程栈大小，减小可创建更多线程 |
| `-XX:NewRatio` | 2 | 老年代/新生代比例 |
| `-XX:SurvivorRatio` | 8 | Eden/Survivor 比例 |
| `-XX:MaxTenuringThreshold` | 15 | 晋升老年代年龄阈值 |
| `-XX:PretenureSizeThreshold` | 0（不限） | 直接进入老年代的对象大小阈值 |

### 10.2 元空间参数

| 参数 | 说明 | 建议值 |
|------|------|--------|
| `-XX:MetaspaceSize` | 触发 GC 的阈值 | 256m ~ 512m |
| `-XX:MaxMetaspaceSize` | 最大元空间 | 512m ~ 1g（必须设置） |
| `-XX:CompressedClassSpaceSize` | 压缩类空间 | 默认 1g |
| `-XX:+UseCompressedClassPointers` | 启用类指针压缩 | 默认开启 |

### 10.3 GC 收集器选择

| 收集器组合 | JVM 参数 | JDK 版本 | 适用场景 |
|-----------|---------|---------|---------|
| Serial + Serial Old | `-XX:+UseSerialGC` | 全版本 | 客户端、单核 |
| ParNew + CMS | `-XX:+UseConcMarkSweepGC` | JDK 7-13（已废弃） | 老项目兼容 |
| Parallel Scavenge + Parallel Old | `-XX:+UseParallelGC` | 全版本 | 高吞吐批处理 |
| G1 | `-XX:+UseG1GC` | JDK 9+ 默认 | **微服务首选** |
| ZGC | `-XX:+UseZGC` | JDK 15+ 生产 | 超低延迟（<1ms） |
| Shenandoah | `-XX:+UseShenandoahGC` | JDK 12+ | 低延迟（<10ms） |
| Epsilon | `-XX:+UseEpsilonGC` | JDK 11+ 实验 | 无 GC，仅测试 |

### 10.4 G1 专属调优参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `-XX:MaxGCPauseMillis` | 200 | 期望最大停顿时间（ms） |
| `-XX:G1HeapRegionSize` | 自动（1-32MB） | Region 大小 |
| `-XX:InitiatingHeapOccupancyPercent` | 45 | 触发并发标记的堆占用率（%） |
| `-XX:ConcGCThreads` | CPU数/4 | 并发 GC 线程数 |
| `-XX:G1ReservePercent` | 10 | 预留空间比例（%） |
| `-XX:G1NewSizePercent` | 5 | 新生代初始占堆比例（%） |
| `-XX:G1MaxNewSizePercent` | 60 | 新生代最大占堆比例（%） |
| `-XX:ParallelGCThreads` | CPU数 | STW 时工作线程数 |

### 10.5 GC 日志与调试参数

| 参数 | 说明 |
|------|------|
| `-Xlog:gc*:file=gc.log:time,uptime,level:filecount=10,filesize=50m` | **JDK 9+ 统一日志**（推荐） |
| `-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log` | **JDK 8 GC 日志** |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 时自动 dump 堆 |
| `-XX:HeapDumpPath=/path/to/dump.hprof` | dump 文件路径 |
| `-XX:+PrintCommandLineFlags` | 打印最终生效的 JVM 参数 |
| `-XX:+PrintFlagsFinal` | 打印所有 JVM 参数最终值 |
| `-XX:+TraceClassLoading` | 追踪类加载 |
| `-XX:+TraceClassUnloading` | 追踪类卸载 |
| `-XX:+PrintCompilation` | 打印 JIT 编译情况 |
| `-XX:NativeMemoryTracking=summary` | 本地内存追踪 |

### 10.6 生产环境最佳实践模板

```shell
# ---------- G1 配置（8C16G 微服务，JDK 9+） ----------
java -Xms8g -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -XX:ConcGCThreads=4 \
     -XX:ParallelGCThreads=8 \
     -XX:G1ReservePercent=10 \
     -XX:+ParallelRefProcEnabled \
     -XX:+AlwaysPreTouch \
     -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -Xlog:gc*:file=/var/log/app/gc.log:tags,time,uptime:filecount=10,filesize=50m \
     -jar app.jar

# ---------- ZGC 配置（16C32G 超低延迟，JDK 15+） ----------
java -Xms16g -Xmx16g \
     -XX:+UseZGC \
     -XX:ConcGCThreads=4 \
     -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -Xlog:gc*:file=/var/log/app/gc.log:tags,time,uptime:filecount=10,filesize=50m \
     -jar app.jar

# ---------- Parallel 配置（批处理/离线计算） ----------
java -Xms16g -Xmx16g \
     -XX:+UseParallelGC \
     -XX:MaxGCPauseMillis=500 \
     -XX:GCTimeRatio=19 \
     -XX:+UseAdaptiveSizePolicy \
     -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -Xlog:gc*:file=/var/log/app/gc.log:tags,time,uptime:filecount=10,filesize=50m \
     -jar app.jar
```

---

## 11. 面试高频考点

### 11.1 工具类

**Q1: jps/jstat/jinfo/jmap/jstack/jcmd 各自用途？**

| 工具 | 一句话 |
|------|--------|
| jps | 找 Java 进程 |
| jstat | 看 GC/类加载统计 |
| jinfo | 查/改 JVM 参数 |
| jmap | 堆 Dump + 对象直方图 |
| jstack | 线程栈 + 死锁检测 |
| jcmd | 以上全能整合 + JFR |

**Q2: 如何定位 CPU 飙升的代码位置？**

> `top -Hp PID` → 转 hex → jstack 搜 nid → 读栈信息，定位业务代码。

**Q3: 如何定位死锁？**

> `jstack -l PID` 输出中搜索 `Found one Java-level deadlock`，直接看到两个线程互等对方释放锁。

### 11.2 调优类

**Q4: Full GC 频繁如何排查？**

1. `jstat -gcutil` 确认老年代持续上涨
2. 分析 GC 日志判断停顿时间
3. `jmap -histo:live` 看存活对象分布
4. heap dump 后用 MAT 分析支配树和 GC Roots
5. 常见原因：静态集合泄漏、缓存无过期、ThreadLocal 未 remove

**Q5: -Xms 为什么不建议不等于 -Xmx？**

> 堆频繁扩容/缩容导致性能损耗，生产环境设相等可避免运行时动态调整。

**Q6: G1 相比 Parallel GC 的优势？**

| 对比维度 | Parallel GC | G1 |
|---------|------------|-----|
| 目标 | 高吞吐量 | 平衡延迟+吞吐量 |
| 停顿 | 不可控（可能数秒） | 可预测（默认 ≤200ms） |
| 内存整理 | 需要 Full GC 整理 | 并发整理，减少碎片 |
| 适用场景 | 批处理/离线计算 | **微服务/Web 应用（JDK 9+ 默认）** |

### 11.3 内存泄漏类

**Q7: 常见内存泄漏模式有哪些？**

| 模式 | 代码示例 | 解决方法 |
|------|---------|---------|
| 静态集合 | `static List<byte[]> CACHE` | 添加过期策略或限容 |
| 未关闭资源 | `new FileInputStream()` 未 close | try-with-resources |
| 内部类泄漏 | 非静态内部类持有外部类引用 | 改为静态内部类 |
| ThreadLocal | 使用后未 remove | 使用 finally 块中 remove |
| 类加载泄漏 | 动态代理/反射未缓存 | 缓存代理类 |

**Q8: 如何用 MAT 分析内存泄漏？**

> 标准流程：jmap dump → MAT 打开 → Histogram 找大对象 → Dominator Tree 追引用 → Path to GC Roots 定位泄漏代码。

### 11.4 方法论

**Q9: 线上调优的禁忌有哪些？**

> - 高峰期执行 `jmap -histo:live` 或 `jmap -dump`（触发 Full GC）
> - 同时调整多个参数（无法判断影响）
> - 堆内存盲目设大（Full GC 停顿急剧增加）
> - 不分析直接抄网上的参数模板

**Q10: JFR 相比 jstack/jmap 的优势？**

> JFR 开销 < 2%，可长期开启无需重启，完整记录 GC、锁竞争、方法耗时、IO 事件。jstack/jmap 只能采集单点快照，且会 STW。

---

> 🎯 **核心总结**：JVM 调优的本质 = **理解机制 + 适配场景 + 基于数据**。三个案例覆盖了后端最常见的 OOM / GC 频繁 / CPU 飙升，排查思路可复用到绝大多数生产故障。记住三条铁律：**先代码后参数、先监控后调优、单变量迭代**。

---

*适用版本：JDK 8 / 11 / 17 / 21 | 最后更新：2026-07-26*
