# 03 JVM 性能调优实战

> 高并发的根基是 JVM——GC 停顿 1 秒可能拒绝数万请求。JVM 调优不是玄学，是有方法论的科学

---

## 📚 目录

1. [JVM 调优策略全景](#1-jvm-调优策略全景)
2. [GC 调优实战](#2-gc调优实战)
3. [内存调优](#3-内存调优)
4. [线程调优](#4-线程调优)
5. [JIT 编译优化](#5-jit编译优化)
6. [JVM 调优工具链](#6-jvm调优工具链)
7. [高并发场景 JVM 最佳实践](#7-高并发场景-jvm-最佳实践)

---

## 1. JVM 调优策略全景

### 1.1 调优层次总览

JVM 性能调优涉及四个核心维度，每个维度的优化目标和手段各不相同：

| 调优维度 | 核心关注点 | 目标 | 关键参数 / 手段 |
|----------|------------|------|-----------------|
| **堆内存调优** | 堆大小、分代比例、对象分配 | 减少 GC 频率，降低停顿时间 | `-Xms` / `-Xmx` / `-Xmn` / `-XX:SurvivorRatio` |
| **GC 调优** | GC 算法选择、参数配置 | 平衡吞吐量与延迟 | `-XX:+UseG1GC` / `-XX:MaxGCPauseMillis` |
| **线程调优** | 线程数、线程池参数 | 发挥 CPU 最大效能，减少上下文切换 | `-Xss` / 线程池核心参数 |
| **JIT 编译调优** | 编译层级、内联策略 | 热点代码编译优化，提升执行效率 | `-XX:+TieredCompilation` / `-XX:InlineSize` |

> 🎯 **调优黄金法则**：先保证代码正确，再考虑调优；先定位瓶颈，再针对性调整；一次只改一个参数，观察效果后再进行下一步。

### 1.2 调优原则

| 原则 | 说明 |
|------|------|
| **无监控不调优** | 没有完整的性能指标数据，任何调优都是盲目的 |
| **一次只改一个参数** | 多个参数同时修改无法判断每个参数的真实效果 |
| **灰度验证** | 先在测试环境验证，再小范围上线，确认无副作用后全量推送 |
| **可回滚意识** | 任何调优参数都应有回滚方案，记录调优前后的参数快照 |
| **业务场景驱动** | GC 暂停容忍度、吞吐量要求因业务而异，不存在"通用的最优配置" |

### 1.3 常用 JVM 参数速查

#### 1.3.1 堆内存配置

| 参数 | 含义 | 建议 |
|------|------|------|
| `-Xms` | 初始堆大小 | 建议与 `-Xmx` 设置一致，避免动态调整 |
| `-Xmx` | 最大堆大小 | 不超过系统内存的 70%~80% |
| `-Xmn` | 新生代大小 | 一般为堆的 1/3 ~ 1/4 |
| `-XX:MaxMetaspaceSize` | 元空间最大值 | 根据类加载量设置，默认无上限 |
| `-XX:MetaspaceSize` | 元空间触发 FGC 的阈值 | 建议明确设置 |

#### 1.3.2 GC 参数

| 参数 | 含义 | 适用 GC |
|------|------|---------|
| `-XX:+UseParallelGC` | 使用 Parallel Scavenge + Parallel Old | 吞吐量优先 |
| `-XX:+UseG1GC` | 使用 G1 垃圾回收器 | 低延迟 + 大堆 |
| `-XX:+UseZGC` | 使用 ZGC | 超低延迟 + 超大堆 |
| `-XX:MaxGCPauseMillis` | G1 目标最大停顿时间 | G1 |
| `-XX:InitiatingHeapOccupancyPercent` | G1 触发并发周期的堆占用百分比 | G1 |
| `-XX:ParallelGCThreads` | 并行 GC 线程数 | Parallel / G1 |
| `-XX:ConcGCThreads` | 并发 GC 线程数 | G1 / ZGC |
| `-XX:MaxTenuringThreshold` | 对象晋升老年代的最大年龄 | Parallel / G1 |

#### 1.3.3 调试与日志参数

```yaml
# GC 日志（JDK 11+ 推荐 -Xlog）
-Xlog:gc*:file=gc.log:time,pid,tags:filecount=10,filesize=100m

# JDK 8 及以下
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log

# 发生 OOM 时自动 dump 堆
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/path/dump.hprof

# Native Memory Tracking（排查堆外内存）
-XX:NativeMemoryTracking=summary

# 安全点日志
-Xlog:safepoint*:file=safepoint.log
```

#### 1.3.4 其他重要参数

| 参数 | 默认值 | 说明 |
|------|:------:|------|
| `-XX:+AlwaysPreTouch` | false | 启动时预分配物理内存，避免运行时缺页中断 |
| `-XX:+DisableExplicitGC` | false | 禁用 `System.gc()` 显式触发 FGC |
| `-server` | 64位默认 | 开启服务端模式，使用 C2 编译器 |
| `-XX:ReservedCodeCacheSize` | 240M | JIT 编译代码缓存大小，编译量大时需要调大 |
| `-XX:+PrintCommandLineFlags` | false | 打印 JVM 最终使用的参数值（优先级验证） |

> ⚠️ **`-XX:+AlwaysPreTouch` 的副作用**：开启后 JVM 启动时会预分配所有堆内存对应的物理内存页，大幅缩短 GC 停顿中的缺页处理时间，但会显著延长启动时间（大型应用可能多 30s+）。适合对启动时间不敏感但对运行时 GC 停顿敏感的场景。

---

## 2. GC 调优实战

### 2.1 GC 算法选型决策树

```
堆内存 < 4GB 且 CPU 核心数多？
    ├── Yes → Parallel GC（高吞吐，适合批处理 / 后台任务）
    └── No  → 继续判断

延迟要求 < 10ms 且 堆内存 < 100GB？
    ├── Yes → ZGC（JDK 21+ 默认首选）
    └── No  → 继续判断

延迟要求在 50~200ms 之间？
    ├── Yes → G1 GC（JDK 9+ 默认，通用场景）
    └── No  → Parallel GC（批处理 / 离线计算）
```

| GC 算法 | 适用场景 | 延迟 | 吞吐量 | 堆大小 |
|---------|----------|:----:|:------:|:------:|
| **Parallel** | 批处理、离线计算、后台任务 | 秒级停顿可接受 | 最高 | < 8 GB |
| **G1** | 通用企业应用、微服务、大促场景 | P99 < 200ms | 高 | 4~64 GB |
| **ZGC** | 高频交易、实时推荐、大内存 | < 10ms 停顿 | 中高 | 8 GB~16 TB |

### 2.2 G1 GC 核心参数与调优

G1（Garbage First）是 JDK 9+ 的默认垃圾回收器，将堆划分为多个 Region（1~32MB），优先回收垃圾最多的 Region。

#### 2.2.1 核心参数

| 参数 | 说明 | 默认值 | 调优建议 |
|------|------|:------:|----------|
| `-XX:MaxGCPauseMillis` | 目标最大 GC 停顿时间 | 200ms | 设置 50~200ms，过小会导致 GC 频率升高 |
| `-XX:InitiatingHeapOccupancyPercent` | 触发并发周期的堆占用比例 | 45% | 大数据量业务可调小到 30%，小对象可调大到 60% |
| `-XX:G1HeapRegionSize` | Region 大小（1/2/4/8/16/32MB） | 堆/2048 | 大对象多时调大（如 16MB），小对象多时保持默认 |
| `-XX:G1ReservePercent` | 预留空间百分比（防止晋升失败） | 10% | Full GC 频繁时可调大到 15%~20% |
| `-XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent` | 新生代最小占比 | 5% | 可微调，通常不建议改动 |

#### 2.2.2 G1 GC 调优案例：电商大促

**背景**：某电商平台双十一期间，订单服务出现多次 FGC，GC 日志显示老年代占用快速膨胀，单次停顿长达 3~5 秒，导致大量请求超时。

**初始 JVM 配置**：
```yaml
-Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**症状分析**：
```
[GC pause (G1 Evacuation Pause) (young) 2.5s]
  ... (to-space exhausted) → 触发 Full GC
[Full GC (Allocation Failure) 5.1s]
  E: 0.2%, O: 98%, M: 92%
  → 25134.5ms 用户线程停顿
```

根本原因：大促流量是平时的 10 倍，订单对象分配速率过快；`-Xmx4g` 不足以支撑峰值；G1 新生代区域被快速填满，`to-space exhausted` 导致对象直接晋升失败，触发 Full GC。

**调整方案**：

```yaml
-Xms8g -Xmx8g                          # 堆内存翻倍
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100                # 降低目标停顿，加快回收
-XX:InitiatingHeapOccupancyPercent=30   # 堆占用 30% 即开始并发标记，避免并发标记赶不上分配
-XX:G1HeapRegionSize=16m                # 大订单对象多，增大 Region
-XX:ConcGCThreads=6                     # 并发线程数 = ParallelGCThreads 的 1/4
-XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=15  # 增大新生代，减少晋升
```

**调优结果**：
```
调优前：FGC 每 3 分钟一次，平均暂停 5s，P99 RT = 3200ms
调优后：FGC 清零，Mixed GC 平均暂停 85ms，P99 RT = 180ms
```

> 🎯 **核心思路**：发生 FGC 时大部分原因是**分配速率 > 回收速率**。解决方案有三个方向——增大堆（给 GC 更多空间）、提前触发并发标记（让并发回收更早开始）、调整新生代大小（减少对象晋升）。

### 2.3 GC 日志解读

#### 2.3.1 Parallel GC 日志

```text
[GC (Allocation Failure) [PSYoungGen: 512000K->4909K(599040K)] 512000K->43434K(1020928K), 0.013s]
[Full GC (Ergonomics) [PSYoungGen: 231K->0K(617472K)] [ParOldGen: 893K->1093K(1024K)] 1124K->1093K(1048576K), 0.045s]
```

| 字段 | 含义 |
|------|------|
| `Allocation Failure` | 分配失败触发 GC |
| `PSYoungGen: 512000K->4909K(599040K)` | 新生代 GC 前/GC 后/总大小 |
| `512000K->43434K(1020928K)` | 堆 GC 前/GC 后/总大小 |
| `0.013s` | GC 暂停时间 |
| `Full GC (Ergonomics)` | 由自适应策略触发的 Full GC |

#### 2.3.2 G1 GC 日志

```text
[GC pause (G1 Evacuation Pause) (young) 85M->25M(512M), 0.062s]
  -- 停顿原因：年轻代垃圾回收，堆从 85MB 降到 25MB，总堆 512MB，暂停 62ms

[GC pause (G1 Evacuation Pause) (mixed) 256M->180M(512M), 0.128s]
  -- 停顿原因：混合回收（年轻代 + 部分老年代 Region），128ms

[GC concurrent-root-region-scan-start]
[GC concurrent-root-region-scan-end, 0.015s]
[GC concurrent-mark-start]
[GC concurrent-mark-end, 0.210s]
[GC remark, 0.031s]
[GC cleanup, 0.002s]
  -- 并发标记周期：扫描根对象 → 并发标记 → 最终标记 → 清理
```

> 💡 重点关注四项指标：**暂停时间**（是否超过目标值）、**GC 频率**（新生代 GC 频率过高说明分配速率太快）、**Mixed GC 占比**（老年代回收是否充分）、**FGC 出现**（FGC 是 G1 最需要避免的事件，说明 G1 并发回收已跟不上分配速率）。

#### 2.3.3 ZGC 日志

```text
[ZGC] Allocation Stall (Thread=0x..., 10 requesters, 3.4ms)
[ZGC] GC(0) Garbage Collection (Proactive) 2560M(4096M) -> 2048M(4096M) 1.2ms
[ZGC] GC(1) Garbage Collection (Allocation Rate) 3072M(4096M) -> 2300M(4096M) 2.1ms
```

ZGC 最重要的指标是 **`Allocation Stall`**——这是用户线程因为分配不到内存而阻塞的时间。ZGC 的目标是让这个时间几乎为零。如果出现大量 `Allocation Stall`，意味着分配速率超过了 ZGC 的回收能力，需要增大堆或调整并发线程数。

### 2.4 ZGC 配置与适用场景

#### 2.4.1 ZGC 配置参数

```yaml
# ZGC 核心配置（JDK 15+ 生产可用，JDK 21+ 显著优化）
-XX:+UseZGC
-Xms16g -Xmx16g
-XX:ZAllocationSpikeTolerance=2.0   # 分配尖峰容忍度（默认 2.0，越高预留空间越大）
-XX:ConcGCThreads=4                 # 并发 GC 线程数
-XX:ParallelGCThreads=8             # 并行 GC 线程数
```

#### 2.4.2 ZGC 适用场景

| 场景 | 推荐度 | 原因 |
|------|:------:|------|
| 大堆内存（> 100GB） | ⭐⭐⭐⭐⭐ | ZGC 堆大小几乎不影响停顿时间 |
| 低延迟要求（< 10ms） | ⭐⭐⭐⭐⭐ | ZGC 停顿从不超过 10ms |
| 高 QPS 在线服务 | ⭐⭐⭐⭐ | 低停顿 = 少丢请求 |
| 批处理 / 离线计算 | ⭐⭐ | Parallel GC 吞吐更高 |
| 小堆（< 4GB） | ⭐⭐ | ZGC 优势在大堆，小堆不如使用 G1 或 Parallel |

#### 2.4.3 ZGC vs G1 性能对比

| 指标 | G1 (8GB 堆) | ZGC (8GB 堆) |
|------|:-----------:|:------------:|
| 平均 GC 暂停 | 50~150ms | < 2ms |
| P99 GC 暂停 | 200~500ms | < 5ms |
| 最大暂停 | 数秒（FGC） | < 10ms |
| 吞吐量折损 | ~2% | ~5% |
| 适用 JDK 版本 | JDK 9+ | JDK 15+（生产）|

> ⚠️ ZGC 相比 G1 会有一定吞吐量折损（约 3%~5%），因为并发标记和引用处理消耗 CPU。在追求极致吞吐量的场景（如批处理），ZGC 可能不如 Parallel GC。ZGC 的定位是**用少量吞吐换低延迟**。

### 2.5 GC 调优方法论

```
GC 调优 = 确定目标 → 收集数据 → 定位问题 → 调整参数 → 验证效果 → 持续循环
```

**目标确定**：

| 业务类型 | 优先目标 | 推荐 GC | 说明 |
|----------|:--------:|---------|------|
| 在线交易、秒杀 | 低延迟 | G1 / ZGC | 用户请求不能长时间等待 |
| 批处理、日志分析 | 高吞吐 | Parallel GC | 关注整体处理时间，而非单个请求 |
| 混合型 | 平衡 | G1 | 默认选择，参数可调空间大 |

**问题定位速查**：

| 现象 | 可能原因 | 排查手段 |
|------|----------|----------|
| FGC 频繁 | 堆过小 / 对象泄漏 / 晋升太快 | GC 日志 + heap dump |
| GC 停顿飙升 | 堆过大未分区 / 安全点问题 | 安全点日志 + 线程 dump |
| CPU 飙高 | GC 线程繁忙 / JIT 编译 | CPU Profile + GC 日志分析 |
| 内存持续增长 | 内存泄漏 | MAT 分析 dump + 代码审查 |

---

## 3. 内存调优

### 3.1 堆内存布局与优化

#### 3.1.1 JVM 堆内存结构

```
堆内存（JDK 8+，新一代 GC 无固定分代边界）
│
├── 新生代（Young Generation）
│   ├── Eden（80%） — 对象分配主区域
│   ├── Survivor 0（10%）
│   └── Survivor 1（10%）
│
├── 老年代（Old Generation）
│   └── G1 中为 Old Region，ZGC 中不区分代
│
└── Metaspace（元空间，从堆外移到本地内存）
    └── 类元数据、方法字节码、常量池
```

#### 3.1.2 新生代大小优化

| 新生代大小 | 优势 | 劣势 |
|:----------:|------|------|
| **较大（> 堆的 40%）** | 对象在新生代回收，较少晋升到老年代 | 新生代 GC 暂停时间更长 |
| **较小（< 堆的 20%）** | 新生代 GC 暂停短 | 对象更容易晋升到老年代，可能引发 FGC |
| **适中（堆的 1/3）** | 平衡两者 | 需根据对象分配速率微调 |

> 💡 经验公式：如果应用每秒分配大量临时对象（如 RPC 序列化/反序列化），建议增大新生代到 40%~50%，让大部分临时对象在新生代被回收，减少老年代压力。

### 3.2 堆外内存排查

堆外内存泄漏是高并发场景中最隐蔽、最难排查的问题之一。典型表现为：系统 `free` 内存持续下降，但 JVM 堆内存正常（通过 jstat 或 jmap 查看），最终 OOM 或操作系统 OOM Killer 杀掉进程。

#### 3.2.1 堆外内存主要来源

| 来源 | 说明 | 典型场景 |
|------|------|----------|
| **DirectByteBuffer** | `ByteBuffer.allocateDirect()` 分配的堆外内存 | Netty / NIO 框架 |
| **MappedByteBuffer** | 文件内存映射 | 文件读写 |
| **Unsafe.allocateMemory** | 直接通过 `Unsafe` 分配 | 某些序列化框架 |
| **Native 库** | JNI 调用的 C 代码分配 | 加密库、压缩库 |
| **JVM 内部** | JIT Code Cache、Metaspace、GC 内部结构 | 所有场景 |

#### 3.2.2 堆外内存排查步骤

```text
Step 1: 确认内存问题主体
  $ free -h        # 看操作系统内存使用
  $ jstat -gc <pid> # 看 JVM 堆内存使用
  如果 OS 内存占用高但 JVM 堆正常 → 极大概率是堆外内存泄漏

Step 2: 开启 Native Memory Tracking
  启动参数添加：-XX:NativeMemoryTracking=summary
  $ jcmd <pid> VM.native_memory summary scale=MB
  → 输出各区域（Java Heap / Class / Thread / Code / GC / Compiler / Internal）内存占用

Step 3: 检查 DirectBuffer 使用
  $ jcmd <pid> VM.native_memory summary.diff scale=MB
  对比两次结果，看哪部分内存持续增长

Step 4: 分析 DirectByteBuffer 引用链（JDK 11+）
  $ jcmd <pid> VM.info
  查看 -XX:MaxDirectMemorySize 配置

Step 5: 代码审查重点
  - Netty ByteBuf 是否未 release
  - 自定义 ByteBuffer.allocateDirect() 是否未释放
  - 所有 DirectBuffer 相关代码是否用了 try-finally 确保回收
```

#### 3.2.3 DirectByteBuffer 排查示例

```java
// 问题代码示例（Netty 中 ByteBuf 未释放）
public class LeakyService {
    @Autowired
    private EchoClient client;

    public void sendMessage(Object msg) {
        // 问题：ByteBuf 转换为 NIO ByteBuffer 后，原始 ByteBuf 未 release
        ByteBuf buf = Unpooled.buffer(1024);
        // ... 写入数据
        client.channel().writeAndFlush(buf.nioBuffer());  // ⚠️ nioBuffer() 不 release 原 buf
        // buf.release() 被遗漏了！
    }
}

// 正确做法
public void sendMessage(Object msg) {
    ByteBuf buf = Unpooled.buffer(1024);
    try {
        // ... 写入数据
        client.channel().writeAndFlush(buf);
    } finally {
        ReferenceCountUtil.release(buf);  // 确保释放
    }
}
```

> ⚠️ **堆外内存泄漏的典型特征**：Java 堆内存占用正常甚至很低，但 RSS（常驻内存）持续升高。GC 日志正常，无 FGC，但容器/服务器 OOM 不断。此时 NMT（Native Memory Tracking）是最有效的排查工具。

### 3.3 Metaspace 调优

#### 3.3.1 Metaspace 与 PermGen 对比

| 特性 | PermGen（JDK 7 及以前） | Metaspace（JDK 8+） |
|------|:----------------------:|:-------------------:|
| 位置 | JVM 堆内 | 本地内存（堆外） |
| 默认大小 | 固定（-XX:MaxPermSize） | 无上限（受 OS 限制） |
| 溢出错 | `java.lang.OutOfMemoryError: PermGen space` | `java.lang.OutOfMemoryError: Metaspace` |
| 回收时机 | Full GC | Full GC（或 FGC 中） |
| 典型场景 | 动态类加载频繁（如 CGLib、JSP）导致溢出 | 同左 |

#### 3.3.2 Metaspace 参数建议

```yaml
# 建议明确设置 Metaspace 大小，防止无限制增长
-XX:MetaspaceSize=256m              # 初始触发 FGC 的阈值
-XX:MaxMetaspaceSize=512m           # 最大 Metaspace 大小
```

| 应用类型 | 建议 MetaspaceSize | 建议 MaxMetaspaceSize |
|----------|:------------------:|:---------------------:|
| 单体 Spring Boot 应用 | 128m | 256m |
| 微服务（带大量 AOP） | 256m | 512m |
| 动态类加载（Groovy / CGLib 频繁） | 512m | 1024m |

> 💡 Metaspace 过快增长通常由**动态类加载**引起（CGLib 代理、Groovy 脚本引擎、JSP 编译等）。如果 Metaspace 持续增长到 `MaxMetaspaceSize` 并触发 FGC，应考虑是否有关联类加载器泄漏。

### 3.4 内存泄漏排查链路

#### 3.4.1 内存泄漏识别

| 信号 | 说明 | 紧急程度 |
|------|------|:--------:|
| 堆使用量持续增长，GC 后不回落 | 最明显的内存泄漏信号 | 🔴 高 |
| FGC 频率越来越高 | 老年代持续增长触发 FGC | 🔴 高 |
| GC 日志中 Full GC 后老年代占用不降 | 存在不可回收的泄漏对象 | 🔴 极高 |
| 系统 OOM，检查 `hs_err_pid.log` | JVM 崩溃，包含 dump 路径 | 🔴 极高 |

#### 3.4.2 排查流程

```
现象：内存持续增长 / FGC 频繁
    │
    ▼
Step 1: 通过 jstat 确认内存使用趋势
  $ jstat -gcutil <pid> 1000 10
  → 观察 EU / OU 是否持续增长且 GC 后不降
    │
    ▼
Step 2: 生成 Heap Dump
  $ jmap -dump:live,format=b,file=heap.hprof <pid>
  ⚠️ 生产环境慎用 jmap -dump（会停顿应用），推荐：
  -XX:+HeapDumpOnOutOfMemoryError（自动 dump）
  或使用 jcmd <pid> GC.heap_dump heap.hprof
    │
    ▼
Step 3: 使用 MAT（Memory Analyzer Tool）分析
  ① 打开 dump → "Leak Suspects Report" → 自动定位泄漏疑点
  ② "Histogram" → 按 retained size 排序 → 找出最大的对象
  ③ "Dominator Tree" → 查看 GC Root 引用链
    │
    ▼
Step 4: 确定泄漏根因并修复
```

#### 3.4.3 MAT 分析实战示例

```text
Leak Suspects Report 输出示例：

Problem Suspect 1
─────────────────────────────────────────────────
"org.springframework.cglib.proxy.Enhancer"
  实例数量: 2,345,678
  Retained Heap: 1.2 GB (68% of total heap)
  
  ── 引用链 ──
  WebClassLoader @ 0x7c2d1e830
    └── Enhancer @ 0x7c2d1e800 (x2,345,678)
       └── [...]
  
  ️ Shortest Path To GC Root:
  java.lang.Thread @ 0x7a0b31200 main
    └── local variable → HashMap (CGLIB 代理缓存)
        └── EntityProxy@0x7c2... → Enhancer@0x7c2...

  分析：CGLIB 代理对象未被回收，每次请求生成新的代理类
  → 建议：排查是否在循环/高频方法中重复创建 Enhancer 代理
```

> 🎯 **MAT 分析三看**：一看 **Leak Suspects**（自动分析报告），二看 **Histogram**（哪些类对象最多），三看 **Dominator Tree**（谁持有了这些对象的引用）。

#### 3.4.4 JProfiler 分析流程

```text
JProfiler 远程连接步骤（生产环境推荐）：
1. 应用启动参数添加 JProfiler 的 agentlib
2. IDE 或 JProfiler GUI 远程连接
3. 实时观察：
   - Heap Walker → 查看大对象 / 存活对象
   - Allocation Recorder → 录制对象分配调用栈
   - GC Activity → 观察 GC 频率与耗时
```

| 工具 | 优势 | 劣势 |
|------|------|------|
| **MAT** | 离线分析，自动泄漏疑点报告 | 无实时视图，需额外安装 |
| **JProfiler** | 实时监控 + 录制，UI 友好 | 商业授权，远程连接有开销 |
| **VisualVM** | 免费，插件丰富 | 对大 dump 分析效率低 |
| **Eclipse Memory Analyzer** | 开源，MAT 核心 | 只有离线分析 |

---

## 4. 线程调优

### 4.1 线程池监控

#### 4.1.1 线程池核心指标

| 指标 | 含义 | 健康范围 | 异常处理 |
|------|------|:--------:|----------|
| **活跃线程数** | 当前正在执行任务的线程数 | < corePoolSize × 0.8 | 接近 maxPoolSize 说明任务积压 |
| **队列大小** | 等待处理的任务数 | < 队列容量 × 0.7 | 队列满说明处理速度不够，触发拒绝策略 |
| **拒绝次数** | 被拒绝策略处理的任务数 | 0 | 不为 0 说明系统过载，需扩容或限流 |
| **completedTaskCount** | 已完成的任务总数 | 持续增长 | 停滞说明线程池挂起 |
| **最大池大小** | 历史最大活跃线程数 | < maxPoolSize | 频繁达到 maxPoolSize 需考虑扩容 |

#### 4.1.2 线程池监控实现

```java
@Component
public class ThreadPoolMonitor {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolMonitor.class);

    @Scheduled(fixedRate = 5000)  // 每 5 秒上报
    public void monitor() {
        ThreadPoolExecutor executor = ThreadPoolConfig.ORDER_EXECUTOR;
        int activeCount = executor.getActiveCount();
        int poolSize = executor.getPoolSize();
        int queueSize = executor.getQueue().size();
        long completedCount = executor.getCompletedTaskCount();
        long rejectedCount = ThreadPoolConfig.REJECTED_COUNT.get(); // AtomicLong 计数器

        // 上报到监控系统（Prometheus / Metrics）
        log.info("线程池状态 | active={}/{} queue={} completed={} rejected={}",
            activeCount, poolSize, queueSize, completedCount, rejectedCount);

        // 告警规则
        if (queueSize > executor.getQueue().remainingCapacity() * 0.7) {
            log.warn("⚠️ 线程池队列积压超过 70%！当前队列大小: {}", queueSize);
        }
        if (rejectedCount > 0) {
            log.error("🔴 线程池发生任务拒绝！已拒绝: {} 次", rejectedCount);
        }
    }
}
```

#### 4.1.3 线程池监控对接 Prometheus

```yaml
# Micrometer 自动暴露线程池指标（Spring Boot Actuator + Micrometer）
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: prometheus,health

# 自定义线程池注入 MeterRegistry
@Bean
public ExecutorService orderExecutor(MeterRegistry meterRegistry) {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(200), new ThreadPoolExecutor.AbortPolicy());
    // Micrometer 自动收集 jvm.threads.* / executor.* 指标
    return executor;
}
```

### 4.2 线程池参数计算公式

#### 4.2.1 任务类型分类

| 类型 | 特点 | CPU 占比 | 示例 |
|------|------|:--------:|------|
| **CPU 密集型** | 计算为主，几乎不阻塞 | 90%~100% | 图像处理、加密解密、数据聚合 |
| **IO 密集型** | 大量等待 IO 操作 | 10%~30% | HTTP 调用、数据库查询、文件读写 |
| **混合型** | 计算 + IO 均有 | 30%~70% | 大多数业务逻辑 |

#### 4.2.2 线程数计算公式

```text
CPU 密集型：
  线程数 = CPU核心数 + 1  （+1 防止偶尔缺页中断导致 CPU 空闲）

IO 密集型：
  线程数 = CPU核心数 × (1 + 等待时间 / 计算时间)

  等待时间 = 单次 IO 调用平均耗时
  计算时间 = CPU 处理业务逻辑耗时

混合型：
  通过 CompletableFuture / 责任链模式拆分任务类型，分别用不同线程池处理
```

#### 4.2.3 计算案例

```text
案例：订单服务，CPU 核心数 = 8

场景 1：计算订单价格（CPU 密集型）
  线程数 = 8 + 1 = 9
  → 约 9 个线程

场景 2：查询订单详情（IO 密集型，等待 80ms，计算 20ms）
  线程数 = 8 × (1 + 80/20) = 8 × 5 = 40
  → 约 40 个线程

场景 3：混合型（RPC 调用 + 少量计算，等待 50ms，计算 30ms）
  线程数 = 8 × (1 + 50/30) ≈ 8 × 2.67 ≈ 21
  → 约 20~25 个线程
```

#### 4.2.4 队列大小计算

```text
队列大小 = (核心线程数 / 单线程处理能力) × 可接受的排队时间

案例：
  核心线程数 = 10
  单线程每秒处理能力 = 50 个任务
  可接受的排队时间 = 2 秒
  队列大小 = 10 / 50 × 2 × 50 = 20
  → 建议队列大小：500~1000

实际经验：队列大小建议保持 200~2000 之间
  太小 → 容易触发拒绝策略
  太大 → 任务排队时间长，内存占用大
```

> 💡 **线程池参数没有"万能公式"**。上述公式给出的是初始参考值，实际运行后应根据监控数据（活跃线程数、队列积压、RT）进行动态调整。推荐通过配置中心（Apollo / Nacos）支持线程池参数的**动态变更**。

### 4.3 线程数过多排查

JVM 线程数过多会导致上下文切换开销急剧增大、堆栈内存耗尽、系统性能急剧下降。

#### 4.3.1 线程数过多识别

| 症状 | 排查手段 |
|------|----------|
| CPU 利用率高但吞吐量低 | 上下文切换过多，CPU 在"切线程"而非"做业务" |
| 堆内存正常但 RSS 飙升 | 每个线程默认栈 1MB，1000 个线程 = 1GB 堆外内存 |
| 应用响应变慢，卡顿明显 | 线程调度开销增大，GC 停顿变长 |

#### 4.3.2 排查命令

```bash
# 查看进程总线程数
$ top -H -p <pid>                  # Linux 查看线程 CPU 使用
$ ps -Lp <pid> | wc -l             # 查看线程总数

# jstack 分析（Java 方式）
$ jstack <pid>                     # 打印所有线程堆栈
$ jstack <pid> | grep 'java.lang.Thread.State' | sort | uniq -c
  → 输出线程状态分布，正常预期：
     RUNNABLE: 15
     TIMED_WAITING: 30
     WAITING: 20

# 异常情况（线程池泄漏）
$ jstack <pid> | grep "pool-" | wc -l
  → 如果 pool- 开头的线程数远大于预期，说明线程池未正确关闭
```

#### 4.3.3 线程状态异常速查

| 状态组合 | 含义 | 处理 |
|----------|------|------|
| **大量 WAITING（park）** | 线程池空闲线程，正常 | 调整 corePoolSize |
| **大量 BLOCKED** | 锁竞争激烈 | 优化锁粒度 / 减少死锁 |
| **大量 RUNNABLE 但 CPU 低** | 线程在跨网络 IO 等待中 | 检查上游服务 / 数据库 |
| **WAITING (on object monitor)** | `wait()` 等待 notify | 检查生产者-消费者逻辑 |
| **TIMED_WAITING (sleeping)** | `Thread.sleep()` 中 | 检查是否应使用 `ScheduledExecutor` |

> ⚠️ **线程池线程泄漏**：如果每次调用后线程池大小持续增长、需要检查未关闭的线程池。典型场景：在 `@Async` 方法中手动创建 `ExecutorService` 但未在最终关闭；或每次 HTTP 请求都创建新线程池。

---

## 5. JIT 编译优化

### 5.1 分层编译（TieredCompilation）

JVM 的 Just-In-Time（JIT）编译器通过分析热点代码并将其编译为本地机器码来提升执行效率。

#### 5.1.1 编译层级

| 层级 | 名称 | 说明 | 性能 |
|:----:|------|------|:----:|
| 0 | Interpreter | 解释执行，启动时默认模式 | 最慢 |
| 1 | C1 (client) | 简单优化编译，编译快 | 中 |
| 2 | C1 with limited profiling | 带有限性能分析的 C1 | 中高 |
| 3 | C1 with full profiling | 带完整性能分析的 C1 | 中高 |
| 4 | C2 (server) | 深度优化编译，编译慢但执行快 | 最高 |

编译流程（默认分层编译开启）：

```
方法调用计数达到阈值
    ↓
0: 解释执行 ←──────────────────┐
    ↓ (调用计数达到 Tier 2/3 阈值)  │
3: C1 编译（带 profiling）──────┤
    ↓ (调用计数达到 Tier 4 阈值)    │
4: C2 编译（深度优化）──────────┘
    ↓
如果 C2 编译后的代码被反优化（如被 profiling 数据误导）→ 回退到 Tier 3
```

> 💡 **分层编译的意义**：启动阶段使用解释执行快速启动；C1 在短时间内提供基本的编译优化；C2 在充分收集性能数据后进行深度优化。JDK 8+ 默认开启分层编译（`-XX:+TieredCompilation`）。

### 5.2 逃逸分析（Escape Analysis）

逃逸分析是 C2 编译器最重要的优化技术之一，通过分析对象的作用域来决定是否可以采取激进的优化。

#### 5.2.1 逃逸状态

| 状态 | 含义 | 优化手段 |
|:----:|------|----------|
| **NoEscape** | 对象不逃逸当前方法/线程 | 标量替换 / 栈上分配 / 锁消除 |
| **ArgEscape** | 对象作为参数传递但不被全局引用 | 锁消除 |
| **GlobalEscape** | 对象可能被其他线程访问 | 无优化（按常规堆分配） |

#### 5.2.2 标量替换（Scalar Replacement）

```java
// 原始代码：每次调用都创建 Point 对象
public class PointService {
    public double distance(int x, int y) {
        Point p = new Point(x, y);  // 创建对象
        return Math.sqrt(p.x * p.x + p.y * p.y);
    }
}

// C2 优化后的效果（标量替换）：
// Point 对象不会真实创建，而是将 x, y 作为局部变量处理
public double distance(int x, int y) {
    int p_x = x;  // 标量替换：对象字段拆分为局部变量
    int p_y = y;
    return Math.sqrt(p_x * p_x + p_y * p_y);
}
```

#### 5.2.3 锁消除（Lock Elimination）

```java
// 原始代码：StringBuffer 是线程安全的，但方法局部变量不存在竞争
public String concat(String a, String b) {
    StringBuffer sb = new StringBuffer();
    sb.append(a);
    sb.append(b);
    return sb.toString();
}

// C2 优化：消除 sb.append() 中的同步锁
// 因为 sb 不会逃逸出 concat 方法，不存在多线程竞争
```

#### 5.2.4 栈上分配（Stack Allocation）

```java
// 满足条件的对象直接在栈上分配，不进入堆
// 条件：对象不逃逸 + 标量替换无法完整处理
public void process() {
    // 如果 Order 对象没有逃逸，可能在栈上分配
    Order order = new Order(1001, 299.0);
    // 使用 order ...
}  // 方法结束，栈帧回收，order 自动销毁，不需要 GC

// 如果 order 被返回或赋值给全局变量 → 必须堆分配
private Order cache;
public void process() {
    Order order = new Order(1001, 299.0);
    this.cache = order;  // 逃逸了！必须堆分配
}
```

> 🎯 **逃逸分析的意义**：在方法内部创建的临时对象，如果满足不逃逸的条件，C2 可以避免堆分配，直接在栈上分配或使用标量替换。这极大减轻了 GC 压力——**减少了对象分配也就减少了 GC 回收的工作量**。

### 5.3 预热与 C2 编译

#### 5.3.1 为什么需要预热

高并发应用中，服务启动后短时间内可能面临大量请求。如果 JIT 编译器未完成热点代码编译，代码在解释执行阶段性能很低。

```text
服务启动 → 大量请求涌入 → 解释执行（慢）→ 请求超时 ❌
服务启动 → 预热 → C2 编译完成 → 全速运行 → 请求正常通过 ✔
```

#### 5.3.2 预热策略

| 策略 | 做法 | 适用场景 |
|------|------|----------|
| **自动预热（默认）** | 调用计数达到阈值后自动触发 C2 编译 | 生产环境 |
| **手动预热** | 启动后发一批模拟请求触发热点代码编译 | 压测 / 高并发服务 |
| **记录与恢复** | 将编译结果保存到文件，下次启动直接加载 | 追求极致启动速度 |

```java
// 手动预热示例
@Component
public class ApplicationWarmUp {

    @PostConstruct
    public void warmUp() {
        log.info("开始预热关键接口...");
        // 模拟用户查询请求，触发热点代码编译
        for (int i = 0; i < 20000; i++) {
            try {
                orderService.getOrderDetail(1001L);
                userService.getUserInfo(10001L);
                paymentService.queryPayment(50001L);
            } catch (Exception e) {
                // 预热期间的异常可忽略
            }
        }
        log.info("预热完成，C2 编译器已触发");
    }
}
```

#### 5.3.3 C2 编译阈值

| 参数 | 默认值 | 说明 |
|------|:------:|------|
| `-XX:CompileThreshold` | 10000 | 方法调用计数的编译阈值 |
| `-XX:-TieredCompilation` | 开启 | 关闭分层编译（使用 C2 阈值 10000） |
| `-XX:Tier4InvocationThreshold` | 5000 | C2 编译所需的调用计数阈值 |
| `-XX:InlineSize` | 35 | 方法内联的最大字节码长度 |

> 💡 **方法内联**是 C2 最重要的优化手段之一。小方法（< 35 字节码）会被直接内联到调用方，消除方法调用的开销。优化建议：将热点路径上的方法保持在**200 行以内**，**无循环嵌套**，**参数和局部变量数量合理**。

---

## 6. JVM 调优工具链

### 6.1 JDK 内置工具实战

#### 6.1.1 jstat（JVM 统计信息监控）

```bash
# 查看 GC 概况（每 1 秒输出一次，共 5 次）
$ jstat -gc <pid> 1000 5
  S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC
 1024.0 1024.0  0.0   512.0   8192.0   4096.0   16384.0    8192.0   25600.0

# 查看 GC 汇总（吞吐量、暂停时间）
$ jstat -gcutil <pid> 1000
  S0     S1     E      O      M     YGC     YGCT    FGC    FGCT    GCT
  0.00  50.00  45.00  50.00  85.00  120    2.345    2     0.567   2.912
  ↑ Survivor 0 使用率  ↑ Eden 已用  ↑ YGC=120 次, YGCT=2.345s
                          ↑ O 老年代已用  ↑ FGC=2 次, FGCT=0.567s
```

**关键指标解读**：

| 指标 | 正常范围 | 异常信号 |
|------|:--------:|----------|
| **YGC** | 频率取决于分配速率 | 过快 → 分配速率高 / 新生代太小 |
| **YGCT** | 单次 < 100ms | 单次 > 500ms → 可能有大量存活对象复制 |
| **FGC** | 0 | > 0 需重视，持续出现说明严重问题 |
| **FGCT** | 单次 < 1s | 单次 > 3s → 大堆 Full GC 时间长 |

#### 6.1.2 jstack（线程堆栈分析）

```bash
# 打印所有线程堆栈
$ jstack <pid>

# 查看死锁
$ jstack <pid> | grep -A 30 "deadlock"
  Found one Java-level deadlock:
  "thread-1": waiting to lock <0x000000076b5f6c78> (a java.lang.Object)
  "thread-2": waiting to lock <0x000000076b5f6c60> (a java.lang.Object)

# 查看线程状态分布
$ jstack <pid> | grep "java.lang.Thread.State" | sort | uniq -c
     15 RUNNABLE
     30 TIMED_WAITING (on object monitor)
     20 WAITING (parking)
      0 BLOCKED

# 查看 CPU 最高的线程
$ top -H -p <pid>                  # 找到最耗 CPU 的线程 ID（十进制）
$ printf '%x\n' <十进制线程ID>      # 转换为十六进制
$ jstack <pid> | grep -A 30 '<十六进制>'
```

> 🎯 **jstack 实战技巧**：当系统出现"假死"或响应缓慢时，**连续 dump 3~5 次（间隔 5 秒）** 对比分析，可以区分"短暂锁等待"和"真正死锁/死循环"。

#### 6.1.3 jmap（堆转储与对象统计）

```bash
# 统计对象实例数（不 dump 全量）
$ jmap -histo <pid> | head -20
  num     #instances         #bytes  class name
  1:       1234567      123456789  [B           (byte 数组)
  2:        987654       98765432  java.lang.String
  3:        234567       34567890  com.example.Order

# 强制 Full GC 并生成 dump（生产环境谨慎使用，会触发 FGC）
$ jmap -dump:live,format=b,file=heap.hprof <pid>

# 更安全的 dump 方式（JDK 11+）
$ jcmd <pid> GC.heap_dump heap.hprof
```

#### 6.1.4 jcmd（多功能诊断命令）

```bash
# 查看 JVM 进程列表
$ jcmd -l

# 查看所有可用的诊断命令
$ jcmd <pid> help

# 查看 JVM 生效参数
$ jcmd <pid> VM.flags
  -XX:CICompilerCount=4 -XX:ConcGCThreads=2 -XX:G1HeapRegionSize=16777216 ...

# 查看 JVM 版本与运行时信息
$ jcmd <pid> VM.version
$ jcmd <pid> VM.info

# 查看系统属性
$ jcmd <pid> VM.system_properties

# 查看堆外内存（NMT）
$ jcmd <pid> VM.native_memory summary scale=MB
```

### 6.2 Arthas 在线调优

Arthas 是阿里巴巴开源的 Java 诊断工具，可以在线查看 JVM 状态、方法执行耗时、调用栈等，无需重启应用。

#### 6.2.1 常用命令速查

```bash
# 启动 Arthas
$ java -jar arthas-boot.jar

# dashboard — 实时面板（线程 / GC / 堆 / CPU / 内存）
$ dashboard

# thread — 查看线程信息
$ thread                    # 查看所有线程
$ thread -n 5               # 查看 CPU 最高的 5 个线程
$ thread <tid>              # 查看指定线程堆栈

# jvm — 查看 JVM 信息（参数 / 版本 / 运行时）
$ jvm

# memory — 查看内存使用
$ memory
  Memory             used      total     max     usage
  heap               2.1G      4.0G     4.0G    52.50%
  metaspace          180M      256M     512M    35.16%

# monitor — 方法监控
$ monitor -c 5 com.example.OrderService getOrder

# trace — 方法调用路径耗时
$ trace com.example.OrderService getOrder

# watch — 观测方法入参、返回值和异常
$ watch com.example.OrderService getOrder '{params,returnObj,throwExp}' -x 2

# ognl — 在线调用 Spring Bean
$ ognl '#bean=@com.example.SpringContext@getBean("orderService"), #bean.getOrder(1001L)'

# sc / sm — 查找类和方法
$ sc -d com.example.OrderService   # 查看类信息
$ sm com.example.OrderService      # 查看方法列表
```

> 💡 **Arthas 的三大核心场景**：① `trace` 定位方法级性能瓶颈；② `watch` 排查方法入参/返回值异常；③ `ognl` 在线调用 Spring Bean 查询运行状态。这些操作**不会影响生产环境的正常运行**。

#### 6.2.2 Arthas 实战案例：线上接口慢排查

```bash
# 1. 先看哪些线程 CPU 高
thread -n 5

# 2. 假设发现 getOrder 方法慢，trace 追踪调用链
trace com.example.OrderService getOrder -n 3 --skipJDKMethod false

# 输出示例：
`---[0.825s] com.example.OrderService:getOrder()
    +---[0.020s] com.example.UserService:getUser()
    +---[0.400s] com.example.InventoryService:checkStock()  ← 最慢！
    +---[0.030s] com.example.PriceService:calculate()
    `---[0.010s] com.example.Logger:info()

# 3. 深入 checkStock 方法
trace com.example.InventoryService checkStock -n 3

# 4. 发现是 Redis 调用慢，检查 Redis 连接情况
ognl '#redis=@com.example.RedisConfig@getJedisPool(), #redis.getNumActive()'
```

### 6.3 JFR（JDK Flight Recorder）

JFR 是 JDK 自带的低开销性能事件记录工具，**生产环境可默认开启**（JDK 11+ 含商业特性，JDK 17+ 免费开放）。

```bash
# 启动 JFR 记录
$ jcmd <pid> JFR.start name=profile duration=60s filename=/tmp/recording.jfr settings=profile

# 查看当前 JFR 记录
$ jcmd <pid> JFR.check

# 停止并保存 JFR 记录
$ jcmd <pid> JFR.stop name=profile

# 查看 JFR 记录（使用 JDK Mission Control 打开 .jfr 文件）
```

**JFR 能记录什么**：

| 事件类型 | 记录内容 | 分析用途 |
|----------|----------|----------|
| GC Events | GC 暂停时间、原因、各阶段耗时 | GC 调优 |
| Allocation | 对象分配热点、分配速率 | 内存优化 |
| Method Profiling | 热点方法调用频率、耗时 | 方法级性能瓶颈 |
| Thread Lock | 线程等待锁的时长、锁竞争 | 并发优化 |
| IO Events | 文件 / Socket 读写事件 | IO 瓶颈分析 |

---

## 7. 高并发场景 JVM 最佳实践

### 7.1 大促 JVM 参数模板

以下是针对大促/秒杀场景经过验证的 JVM 参数模板，可根据业务实际情况调整。

#### 7.1.1 G1 GC 模板（通用推荐）

```yaml
# JVM 参数模板：大促专用（8GB 堆，G1 GC）
-Xms8g -Xmx8g
-Xmn3g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:InitiatingHeapOccupancyPercent=30
-XX:G1HeapRegionSize=16m
-XX:ConcGCThreads=6
-XX:ParallelGCThreads=8
-XX:G1ReservePercent=15
-XX:+UnlockExperimentalVMOptions
-XX:G1NewSizePercent=15
-XX:+DisableExplicitGC
-XX:+AlwaysPreTouch
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/export/logs/dump.hprof
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
-Xlog:gc*:file=/export/logs/gc.log:time,pid,tags:filecount=10,filesize=100m
-Xlog:safepoint*:file=/export/logs/safepoint.log
-Xss512k
-Djava.security.egd=file:/dev/./urandom
```

#### 7.1.2 ZGC 模板（超低延迟场景）

```yaml
# JVM 参数模板：超低延迟大堆场景（16GB 堆，ZGC）
-Xms16g -Xmx16g
-XX:+UseZGC
-XX:ZAllocationSpikeTolerance=2.0
-XX:ConcGCThreads=4
-XX:ParallelGCThreads=8
-XX:+DisableExplicitGC
-XX:+AlwaysPreTouch
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
-Xlog:gc*:file=/export/logs/gc.log:time,pid,tags:filecount=10,filesize=100m
-Xss512k
```

### 7.2 微服务容器化 JVM 配置

#### 7.2.1 Docker 内存限制与 JVM 堆协调

容器环境下，JVM 无法感知 Docker 的内存限制（默认），需要手动配置堆大小。

**错误做法**（在 Docker 4GB 限制下）：
```yaml
# Docker 容器限制内存 4GB
-Xms4g -Xmx4g
# → 堆占 4GB，但 JVM 自身 + Metaspace + 线程栈 + DirectBuffer 需要额外内存
# → 容器 OOMKilled 进程被杀！ ️
```

**正确做法**（使用容器感知参数 + 留出余量）：

```yaml
# Docker 容器限制内存 4GB
# JDK 10+ 支持 UseContainerSupport（默认开启）
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0        # 占用容器内存的 75%
-XX:InitialRAMPercentage=75.0
-XX:MinRAMPercentage=75.0

# 实际效果：4GB × 75% = 3GB 给 JVM 堆
# 剩余 1GB 给：Metaspace（256M）+ 线程栈 + JIT Code Cache + DirectBuffer
```

#### 7.2.2 容器内存分配建议

| 容器总内存 | 建议堆大小 | 建议 MaxRAMPercentage | 预留用途 |
|:----------:|:----------:|:--------------------:|----------|
| 1 GB | 700 MB | 70% | 线程 + Metaspace + 系统 |
| 2 GB | 1.5 GB | 75% | 同上 |
| 4 GB | 3 GB | 75% | 同上 |
| 8 GB | 6 GB | 75% | 同上 |
| 16 GB | 12 GB | 75% | 同上 |

> ⚠️ **容器内存配置三大陷阱**：① 堆 + JVM 其他部分 + 应用内存 > 容器限制 → OOMKilled；② 未开启 `UseContainerSupport`，老版本 JDK 认为物理机内存无限；③ -Xmx 设置等于容器限制内存，没有预留余量。

### 7.3 Spring Boot 应用 JVM 调优 Checklist

这是 Spring Boot 微服务从开发到上线的 JVM 调优检查清单，建议每个服务上线前逐项核对。

#### 7.3.1 基础配置

- [ ] 确认 JDK 版本 ≥ 17（推荐 JDK 21，ZGC 更加成熟）
- [ ] `-Xms` = `-Xmx`（避免堆动态调整带来的性能抖动）
- [ ] 开启 `-XX:+AlwaysPreTouch`（生产环境，减少运行时缺页中断）
- [ ] 设置明确 `-XX:MetaspaceSize` 和 `-XX:MaxMetaspaceSize`
- [ ] 开启 GC 日志（`-Xlog:gc*`），配置日志轮转
- [ ] 开启 OOM 自动 Heap Dump（`-XX:+HeapDumpOnOutOfMemoryError`）
- [ ] 设置 `-XX:+DisableExplicitGC`，防止第三方库调用 `System.gc()`

#### 7.3.2 GC 选型

- [ ] 延迟敏感且堆 > 4GB → 选择 G1 GC（默认）或 ZGC
- [ ] 批处理 / 吞吐优先 → 选择 Parallel GC
- [ ] 超大堆（> 100GB）+ 超低延迟（< 10ms）→ 选择 ZGC
- [ ] 配置 G1 `-XX:MaxGCPauseMillis` = 100（非默认的 200）
- [ ] 大促前调整 `-XX:InitiatingHeapOccupancyPercent` = 30~35

#### 7.3.3 线程池配置

- [ ] 为不同业务创建独立命名线程池（方便 jstack 定位）
- [ ] 配置线程池监控指标（Micrometer + Prometheus）
- [ ] 设置合理的拒绝策略（非关键业务用 CallerRunsPolicy，关键业务用 AbortPolicy + 告警）
- [ ] `-Xss` 建议 512K（默认 1M，大量线程时节省内存）

#### 7.3.4 容器化检查

- [ ] 容器环境开启 `-XX:+UseContainerSupport`
- [ ] 设置 `-XX:MaxRAMPercentage=75`（预留 25% 给 JVM 非堆 + 系统）
- [ ] 不设置固定 `-Xmx`，优先使用百分比参数
- [ ] Kubernetes Resource request = limit（避免 CPU 压缩导致 GC 性能抖动）

#### 7.3.5 监控与告警

- [ ] 集成 Micrometer 暴露 JVM 指标（heap / GC / thread / class）
- [ ] Prometheus + Grafana 配置 GC 暂停时间面板
- [ ] 告警规则：FGC 产生立即告警，YGC 暂停 > 500ms 告警
- [ ] 配置安全点日志（排查 GC 之外的线程停顿原因）

#### 7.3.6 大促前准备

- [ ] 提前进行全链路压测，确认 JVM 参数在大流量下的表现
- [ ] 预热关键接口（`@PostConstruct` + 模拟请求触发 C2 编译）
- [ ] 确认 Heap Dump 路径有足够磁盘空间（建议至少 10GB）
- [ ] 准备 Arthas 安装包（快速线上诊断）
- [ ] 记录基线参数，调优后对比效果

### 7.4 JVM 调优红线总结

```
JVM 调优不是万能的，先确认瓶颈在 JVM 层面：

                          ┌──────────────────┐
                          │ 系统响应慢 / 吞吐低 │
                          └────────┬─────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                              │
                    ▼                              ▼
            ┌──────────────┐           ┌──────────────────┐
            │ CPU 异常高    │           │ CPU 正常 / 内存高  │
            └──────┬───────┘           └────────┬─────────┘
                   │                            │
                   ▼                            ▼
         ┌──────────────────┐       ┌────────────────────┐
         │ GC 线程繁忙？      │       │ GC 暂停多 / FGC 频繁？│
         │ 安全点问题？       │       │ 内存泄漏？           │
         │ JIT 编译占 CPU？  │       │ 缓存失效导致 DB 慢？  │
         └──────────────────┘       └────────────────────┘

JVM 调优解决不了的问题：
  × SQL 慢查询 / 无索引（→ 优化 DB 或加缓存）
  × 上游服务 RT 高（→ 降级 / 超时 / 熔断）
  × 业务逻辑复杂（→ 简化逻辑 / 异步化）
  × 网络带宽不足（→ 扩容带宽 / DNS 优化）
```

> 🎯 **JVM 调优的终点**：当 GC 停顿在可接受范围内、内存使用稳定、线程池无拒绝、异常监控无告警时，就不需要继续调优了。过度调优在微服务架构下的收益递减（服务粒度越小，单体 JVM 毛刺的影响面越窄）。

---

**下一模块**：[04 高并发编程最佳实践](./04-高并发编程最佳实践.md) | **返回总览**：[总览](./00-高并发与性能优化总览.md)
