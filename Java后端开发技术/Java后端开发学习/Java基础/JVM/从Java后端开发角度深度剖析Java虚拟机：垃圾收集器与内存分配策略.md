# Java 虚拟机：垃圾收集器与内存分配策略（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | JVM GC 与内存分配  
> **前置基础**：JVM 内存区域（运行时数据区）  
> **关联章节**：JVM 内存区域 → **本章** → JVM 调优案例

---

## 一、核心概念

### 1.1 与 GC 强相关的内存区域

GC 的核心作用区域是 **堆内存**，内存分配的主要场景也集中在堆和线程私有区域：

| 内存区域 | 线程 | 存储内容 | GC 关系 | 核心参数 |
|----------|------|----------|---------|----------|
| **堆内存** | 共享 | 所有对象实例和数组 | GC 主战场 | `-Xms`, `-Xmx` |
| **虚拟机栈** | 私有 | 栈帧（局部变量表、操作数栈） | 不直接 GC | `-Xss` |
| **元空间**（JDK 8+） | 共享 | 类元数据、常量池 | Full GC 时回收 | `-XX:MetaspaceSize`, `-XX:MaxMetaspaceSize` |
| **直接内存** | — | NIO Buffer | 不受 GC 直接管理 | `-XX:MaxDirectMemorySize` |

> 中文语境中"JVM 内存模型"有两种指代：本文聚焦 **运行时数据区**（堆、栈、元空间），而非 JMM（Java Memory Model，关注并发语义）。

### 1.2 GC 的核心目标

| 目标 | 说明 | 适用场景 |
|------|------|----------|
| **高吞吐量** | 单位时间内 GC 耗时占比低 | 批量处理、数据同步、后台计算 |
| **低延迟** | GC 停顿（STW）时间短 | 高并发接口服务、支付系统 |

> 吞吐量和低延迟是 **矛盾** 的 — 追求高吞吐量会允许更长停顿，追求低延迟会增加 GC 频率降低吞吐量。

---

## 二、底层原理

### 2.1 三种基础 GC 算法

| 算法 | 原理 | 优点 | 缺点 | 典型应用 |
|------|------|------|------|----------|
| **标记-清除** | 标记可达对象 → 清除未标记 | 简单，无需移动对象 | 内存碎片 | CMS 并发清除阶段 |
| **标记-复制** | 存活对象复制到空闲区 → 清空原区 | 无碎片、高效 | 内存利用率低（始终一半空闲） | 新生代收集器（Serial/ParNew/Parallel Scavenge） |
| **标记-整理** | 存活对象向一端移动 → 清除剩余 | 无碎片 | 移动对象需更新引用，停顿较长 | 老年代收集器（Serial Old/Parallel Old） |

**分代收集理论**：基于"绝大多数对象生命周期短"的经验，堆分新生代（Eden + 2 × Survivor，默认 8:1:1）和老年代，分别采用合适的算法。

### 2.2 常用垃圾收集器详解

#### 2.2.1 Parallel Scavenge + Parallel Old（吞吐量优先，JDK 8 默认）

- **新生代**：Parallel Scavenge，多线程 + 标记-复制
- **老年代**：Parallel Old，多线程 + 标记-整理
- **适用**：批量处理、数据 ETL、报表生成等 CPU 密集型服务
- **注意**：线程数与 CPU 核数相关（`-XX:ParallelGCThreads`），2 核服务器慎用

#### 2.2.2 CMS（低延迟过渡方案，JDK 9 废弃，JDK 14 移除）

- **四阶段**：初始标记（STW）→ 并发标记 → 重新标记（STW）→ 并发清除
- **痛点**：标记-清除产生内存碎片；CPU 核心数少时性能下降；并发模式失败退化为 Serial Old Full GC
- **适用**：JDK 8 高并发接口（过渡方案）

#### 2.2.3 G1（Garbage-First，JDK 9+ 默认，推荐首选）

- **核心**：堆分为多个等大 Region（1MB~32MB），可动态扮演 Eden/Survivor/Old/Humongous
- **流程**：初始标记（STW）→ 并发标记 → 最终标记（STW）→ 筛选回收（STW，选择回收收益最高的 Region）
- **优势**：无内存碎片（标记-复制算法）、可预测停顿（`-XX:MaxGCPauseMillis`）、支持大堆（数十 GB）
- **适用**：微服务、电商核心接口、支付系统

#### 2.2.4 ZGC（超低延迟，JDK 11+，JDK 15 转正）

- **核心**：染色指针 + 读屏障，停顿时间 < 10ms，支持 TB 级堆
- **适用**：堆 > 16GB、延迟 < 10ms 的超大规模服务（高频交易、分布式缓存）

### 2.3 收集器选型速查

| 收集器组合 | 核心优势 | 适用场景 | JDK 版本 | 核心参数 |
|------------|----------|----------|----------|----------|
| Parallel + Parallel Old | 高吞吐量 | 批量处理、后台计算 | JDK 8 默认 | `-XX:GCTimeRatio`, `-XX:MaxGCPauseMillis` |
| CMS + ParNew | 低延迟 | JDK 8 高并发接口（过渡） | JDK 8（JDK 9 废弃） | `-XX:+UseConcMarkSweepGC`, `-XX:CMSInitiatingOccupancyFraction` |
| G1 | 兼顾低延迟与吞吐量 | 高并发、大堆（4GB+）微服务 | JDK 9+ 默认 | `-XX:+UseG1GC`, `-XX:MaxGCPauseMillis` |
| ZGC | 超低延迟（< 10ms） | 超大堆（16GB+）、高频交易 | JDK 11+（JDK 15 转正） | `-XX:+UseZGC`, `-XX:ZHeapSize` |

---

## 三、代码实现

### 3.1 内存分配策略

#### 3.1.1 优先分配到 Eden 区

绝大多数"朝生夕死"的对象优先分配到新生代 Eden 区。Eden 满时触发 Minor GC。Eden : Survivor0 : Survivor1 = 8:1:1（通过 `-XX:SurvivorRatio` 调整）。

#### 3.1.2 大对象直接分配到老年代

```bash
# 大于 1MB 的对象直接进入老年代，避免在 Eden 区频繁复制
-XX:PretenureSizeThreshold=1048576
```

#### 3.1.3 长期存活对象晋升到老年代

```bash
# 经历 15 次 Minor GC 后晋升（默认 15）
-XX:MaxTenuringThreshold=10
```

#### 3.1.4 动态年龄判断

若 Survivor 区中相同年龄的对象总大小超过 Survivor 区的 50%，则年龄 ≥ 该值的对象直接晋升老年代。

#### 3.1.5 TLAB（线程本地分配缓冲）

多线程环境下，每个线程独立在 TLAB 中分配小对象，减少锁竞争。默认开启（`-XX:+UseTLAB`）。

### 3.2 GC 日志与监控

```bash
# G1 收集器常用 JVM 参数（堆 8GB 示例）
-Xms8g -Xmx8g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m
-XX:PretenureSizeThreshold=1048576
-XX:MaxTenuringThreshold=10
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps
-Xloggc:/var/log/app/gc.log
```

### 3.3 GC 监控命令

```bash
# jstat：实时查看 GC 统计（每 1 秒刷新）
jstat -gcutil <pid> 1000

# 关注指标：
# YGC（Minor GC 次数）、YGCT（Minor GC 耗时）
# FGC（Full GC 次数）、FGCT（Full GC 耗时）
```

**生产环境标准**：Full GC < 1 次/天，单次 < 1s；Minor GC < 10 次/分钟，单次 < 50ms。

---

## 四、实战要点

### 4.1 内存泄漏四大场景

| 场景 | 原因 | 解决方案 |
|------|------|----------|
| **静态集合持有对象** | `static List<Object>` 不断添加不清理 | 定期 `clear()` |
| **单例持有短期引用** | 单例 Service 持有 Controller 引用 | 使用 `WeakReference` |
| **资源未关闭** | IO 流、DB 连接、Socket 未关闭 | `try-with-resources` |
| **线程池对象堆积** | 核心线程长期持有局部对象 | `ThreadLocal`（注意 `remove()`） |

### 4.2 OOM 三大类型与解决方案

| OOM 类型 | 原因 | 解决方案 |
|----------|------|----------|
| `Java heap space` | 堆内存不足（对象过多/内存泄漏） | 增大 `-Xmx`、MAT 分析堆快照、优化对象分配 |
| `Metaspace` | 元空间不足（动态生成大量类，如 CGLIB 代理） | 增大 `-XX:MaxMetaspaceSize`、优化代理使用 |
| `Direct buffer memory` | 堆外内存不足（NIO/Netty 过度使用） | 限制 `-XX:MaxDirectMemorySize`、复用 ByteBuffer |

### 4.3 GC 调优三步法

```
1. 监控 → jstat / GC 日志 / VisualVM / Arthas 找到 GC 频率和停顿是否异常
2. 定位 → 频繁 Minor GC = Eden 太小；频繁 Full GC = 老年代不够或内存泄漏
3. 优化 → 先调内存参数 → 再换收集器 → 最后优化业务代码（减少大对象/及时清理引用）
```

---

## 五、避坑总结

| 场景 | 问题 | 正确做法 |
|------|------|----------|
| CPU 核心数少 | 并行收集器线程竞争 CPU | 2 核服务器慎用 Parallel Scavenge |
| CMS 内存碎片 | 长期运行后 Full GC | JDK 9+ 升级到 G1 |
| G1 堆过大 | 停顿时间不可控 | 设置合理 `-XX:MaxGCPauseMillis` |
| 元空间 OOM | CGLIB 动态代理生成大量类 | 增大元空间 + 减少代理 |
| 堆外内存泄露 | NIO Buffer 未释放 | 限制 `-XX:MaxDirectMemorySize` + 复用 |

### 5.2 业务代码层面的内存优化

- 减少大对象创建：避免频繁创建大数组、大集合，使用对象池
- 及时清理：静态集合、缓存定期清理
- 资源管理：`try-with-resources` 关闭 IO/DB/Socket
- 引用类型：缓存用 `SoftReference`，临时对象用 `WeakReference`

---

## 六、企业级最佳实践

### 6.1 收集器选型决策树

```
服务类型？
├── 批量处理/后台计算 → Parallel Scavenge + Parallel Old（吞吐量优先）
└── 高并发接口 / 微服务
    ├── JDK 8 → CMS（注意碎片痛点）或升级到 JDK 11+
    ├── JDK 9-10 → G1（启用以替代 CMS）
    ├── JDK 11+ → G1（默认，首选）
    └── JDK 11+, 堆 16GB+, 延迟 < 10ms → ZGC
```

### 6.2 生产环境 JVM 配置模板

```bash
# 微服务通用配置（G1, 堆 4GB）
-Xms4g -Xmx4g \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=100 \
-XX:MetaspaceSize=256m \
-XX:MaxMetaspaceSize=512m \
-XX:MaxDirectMemorySize=256m \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=/var/log/app/heapdump.hprof \
-XX:+PrintGCDetails \
-XX:+PrintGCDateStamps \
-Xloggc:/var/log/app/gc.log
```

### 6.3 核心要点总结

1. GC 核心目标：根据服务类型选 **吞吐量优先** 或 **低延迟优先**
2. 收集器选型：JDK 8 默认 Parallel，JDK 9+ 默认 G1（推荐），JDK 11+ 超大堆用 ZGC
3. 内存分配五大规则：Eden 优先、大对象直接老年代、年龄晋升、动态年龄判断、TLAB
4. 调优思路：先监控 → 定位瓶颈 → 参数优化 → 收集器切换 → 业务代码优化
5. 常见 OOM：堆溢出、元空间溢出、直接内存溢出，各有针对性方案
