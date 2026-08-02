# 04 - JVM 内存与垃圾回收高频题

> 🎯 JVM 是衡量 Java 深度的重要标尺 — 内存结构、类加载、GC 算法、收集器、调优排查五层递进。面试核心是"结构讲清 + 算法讲透 + 排查讲实战"

---

## 目录

1. [运行时数据区](#1-运行时数据区)
2. [对象创建与内存布局](#2-对象创建与内存布局)
3. [类加载机制](#3-类加载机制)
4. [垃圾回收算法](#4-垃圾回收算法)
5. [GC 收集器演进](#5-gc-收集器演进)
6. [内存泄漏与 OOM 排查](#6-内存泄漏与-oom-排查)
7. [JVM 调优实战](#7-jvm-调优实战)

---

## 1. 运行时数据区

### 1.1 JVM 内存结构（运行时数据区）？

| 区域 | 线程 | 存什么 | 异常 |
|------|:---:|--------|------|
| 程序计数器 | 私有 | 当前执行字节码行号 | 无 |
| 虚拟机栈 | 私有 | 栈帧（局部变量表、操作数栈、方法返回地址） | StackOverflowError / OOM |
| 本地方法栈 | 私有 | native 方法调用 | StackOverflowError |
| 堆 | **共享** | 对象实例（GC 主战场） | OOM（Java heap space） |
| 方法区（JDK8 为元空间） | 共享 | 类元信息、常量、静态变量、JIT 产物 | OOM（Metaspace） |

**追问：**
- 栈帧里有什么？→ 局部变量表（槽 slot）、操作数栈、动态链接、方法出口；一个方法调用 = 一个栈帧入栈
- JDK7 → JDK8 永久代 → 元空间的变化？→ 永久代在堆中，大小难控，full GC 时容易 OOM；元空间使用**本地内存**（默认无上限，靠系统内存），类元信息不再触发堆 GC，也支持动态扩容
- 为什么 String 常量池移到堆？→ 池在永久代时溢出风险高；移入堆后可被 GC 回收，且方便调优（-Xmx 控制）

### 1.2 直接内存（Direct Memory）？

- 堆外内存，NIO 的 ByteBuffer.allocateDirect 分配，零拷贝（减少一次用户态↔内核态拷贝）
- 不归 GC 管（GC 只回收堆内）→ 由 Cleaner/Cleaner 机制回收，**分配太多且不回收会 OOM（Direct buffer memory）**
- 配置参数 -XX:MaxDirectMemorySize（默认等于堆大小）

**追问：** Netty 为什么用直接内存？→ 零拷贝 + 减少 GC 压力；但需要池化（Netty 的 PooledByteBufAllocator）防止频繁申请/释放。

---

## 2. 对象创建与内存布局

### 2.1 对象创建的完整流程？

```text
new Object()
├── ① 类加载检查（类未被加载 → 加载-连接-初始化）
├── ② 分配内存（指针碰撞 / 空闲列表，TLAB 优先）
├── ③ 内存置零（零值初始化）
├── ④ 设置对象头（Mark Word + 类型指针）
└── ⑤ 执行构造方法（init）
```

**追问：** TLAB？→ Thread Local Allocation Buffer：每个线程在 Eden 划一块缓冲区，对象优先在线程私有区域分配，避免 CAS 竞争。大对象直接进老年代？→ -XX:PretenureSizeThreshold 超过阈值直接分配老年代（避免 Eden 复制开销）。

### 2.2 对象内存布局与四种引用？

```text
对象 = 对象头（Mark Word 8B + 类型指针 8B）+ 实例数据 + 对齐填充
```

| 引用类型 | 回收时机 | 场景 |
|----------|----------|------|
| 强引用 | 永不（OOM 也不回收） | 日常 new |
| 软引用 | 内存不足时回收 | 图片缓存（SoftReference） |
| 弱引用 | 下次 GC 即回收 | ThreadLocal key、WeakHashMap |
| 虚引用 | 回收时通知（无法获取对象） | 管理堆外内存（NIO DirectByteBuffer 回收跟踪） |

**追问：** 判断对象死亡的两种方式？→ 引用计数（无法解决循环引用）+ **可达性分析（GC Roots，主流）**。GC Roots 有哪些？→ 虚拟机栈（栈帧局部变量）、静态变量、常量引用、JNI 引用、活跃线程、synchronized 持有的对象。

---

## 3. 类加载机制

### 3.1 类加载过程与双亲委派？

**加载五阶段：** 加载 → 验证 → 准备 → 解析 → 初始化（使用/卸载为可选阶段）

**双亲委派模型（JDK8）：**

```text
        Bootstrap ClassLoader（启动类加载器：JDK 核心类 rt.jar）
                  ↑ 委派
        Extension ClassLoader（扩展类：JDK8 为 lib/ext；JDK9+ 改为 Platform）
                  ↑ 委派
        Application ClassLoader（应用类：classpath）
                  ↑ 委派
        自定义 ClassLoader
```

**工作流程：** 类加载请求先**向上委派**给父加载器，父加载器无法加载时才自己加载。

**设计目的：**
1. 防止重复加载（一个类只被加载一次）
2. **沙箱安全**：防止用户自定义 `java.lang.String` 覆盖 JDK 核心类（核心类永远由 Bootstrap 加载）

**追问：** 如何打破双亲委派？→ 重写 `loadClass()`（不重写 findClass）。典型场景：
- **JDBC SPI**：DriverManager 在 Bootstrap 层，却要加载 classpath 的 MySQL 驱动 → 线程上下文类加载器（TCCL）逆向委派
- **Tomcat**：每个 Webapp 独立类加载器，实现应用隔离 + 热部署（每个 webapp 优先加载自己的类）
- **Spring 插件化/热加载**：自定义 ClassLoader 实现

**记忆点：**
> 🎯 双亲委派 = "先请长辈查，查不到自己查"。目的两句话：类不重复加载 + 核心类不被篡改。打破它 = 重写 loadClass，典型是 SPI（TCCL）和 Tomcat。

---

## 4. 垃圾回收算法

### 4.1 三大基础算法？

| 算法 | 原理 | 优点 | 缺点 | 应用 |
|------|------|------|------|------|
| 标记-清除 | 标记存活 → 清除垃圾 | 简单 | **内存碎片** | CMS 老年代 |
| 标记-复制 | 存活对象复制到另一半区 | 无碎片、高效 | 空间浪费一半 | 新生代 |
| 标记-整理 | 标记存活 → 移向一端 | 无碎片 | 移动对象成本高（需改引用） | 老年代 |

**追问：** 为什么新生代用复制、老年代用标记-整理？→ 新生代存活率低（"朝生夕灭"占 90%+），复制成本极小；老年代存活率高，复制代价大，选整理。分代假设？→ **弱分代假说**：绝大多数对象朝生夕灭；熬过多次 GC 的对象存活率升高。这是分代收集的理论基础。

### 4.2 Minor GC / Major GC / Full GC？

| 类型 | 区域 | 触发 |
|------|------|------|
| Minor GC（Young GC） | 新生代 | Eden 满 |
| Major GC | 老年代 | 常与 Full GC 混称 |
| Full GC | 堆 + 方法区/元空间 | 老年代满、元空间满、System.gc() |

**追问：** Full GC 频繁的常见原因？
1. 老年代空间不足（大对象过多 / 内存泄漏）
2. 元空间不足（动态生成类过多 — CGLIB 代理、反射）
3. 显式 System.gc()（RMI 远程调用会周期性触发）
4. CMS 并发模式失败（碎片化严重 → 降级 Full GC）
5. 大对象直接进老年代占满空间

**记忆点：**
> 🎯 Full GC 频繁五板斧：老年代满 / 元空间满 / 代码 System.gc / 碎片降级 / 大对象。排查第一件事是**看 GC 日志和堆快照**，不是猜。

---

## 5. GC 收集器演进

### 5.1 收集器全家福与选择？

```text
新生代：Serial（单线程） → Parallel Scavenge（多线程，吞吐优先）
        → G1（JDK9+ 默认，分区统一管理）
老年代：Serial Old → Parallel Old → CMS（低延迟，已废弃）
        → G1 → ZGC（超低延迟，着色指针）→ Generational ZGC（JDK21+）
```

| 收集器 | 特点 | 适用 | 状态 |
|--------|------|------|------|
| Serial | 单线程，STW | 客户端小内存 | 过时 |
| Parallel | 多线程，**吞吐量优先** | 批处理、后台计算 | 可选 |
| CMS | 并发标记清除，低延迟 | 老年代（JDK8 常用） | **JDK14 废弃** |
| G1 | Region 分区，**可预测停顿** | **JDK9+ 默认** | 主流 |
| ZGC | **着色指针 + 读屏障，<1ms** | 超大堆低延迟（TB 级） | JDK15+ 生产 |
| Generational ZGC | ZGC + 分代 | 大堆低延迟兼顾吞吐 | JDK21+ |

**追问：**
- G1 为什么能预测停顿？→ 堆划分为多个 Region，G1 追踪各 Region 回收收益，**按收益优先回收**（Garbage First），可设 -XX:MaxGCPauseMillis 停顿目标
- G1 vs CMS 的核心区别？→ CMS 只做老年代、碎片化、空间预留复杂；G1 全堆统一管理 + 无碎片（复制式回收）+ 可预测
- ZGC 为什么快？→ **着色指针**（指针中存标记信息，无需对象头标记）+ 读屏障 + 并发搬运对象，STW 阶段极短（<1ms）
- JDK8 默认收集器？→ Parallel Scavenge + Parallel Old

---

## 6. 内存泄漏与 OOM 排查

### 6.1 常见 OOM 类型？

| 异常 | 原因 | 排查方向 |
|------|------|----------|
| Java heap space | 堆内存不足（泄漏/对象过多） | 堆转储分析 |
| Metaspace | 动态类过多（反射/CGLIB） | 类加载器分析 |
| Direct buffer memory | 堆外内存未释放 | 检查 ByteBuffer 池化 |
| GC overhead limit exceeded | GC 时间 >98% 且回收 <2% | 堆过小或泄漏 |
| StackOverflowError | 递归过深 | 检查递归终止条件 |

### 6.2 线上排查方法论（OOM / Full GC 频繁）？

```bash
# 1. 查看进程与 GC 日志
jps                                  # 找 Java 进程 PID
jstat -gcutil <pid> 1000             # 每秒看 GC 利用率，确认是否频繁 Full GC
jstat -gc <pid> | head               # 各代容量与 GC 次数

# 2. 堆转储
jmap -dump:format=b,file=heap.hprof <pid>   # 生产慎用（STW），推荐 -XX:+HeapDumpOnOutOfMemoryError 自动转储

# 3. 分析（MAT / JVisualVM / Arthas）
#    MAT 查看 Dominator Tree → 定位大对象 → 找 GC Root 引用链
#    Arthas: dashboard 看线程、heapdump 命令

# 4. 常见泄漏代码
#    静态集合只增不减（static Map 缓存无清理）
#    ThreadLocal 未 remove（value 强引用）
#    连接/流未关闭（Connection/IO 未 close）
```

**追问：** CPU 100% 怎么排查？→ ① top 找 PID ② `top -Hp PID` 找线程 ③ `jstack PID` 看线程栈 ④ 定位业务代码（常见：死循环、正则回溯、频繁 GC）。jstack 中 GC 线程（GC task thread）占高 → 是 GC 导致，转向查堆。

**记忆点：**
> 🎯 排查口诀：**jps 找进程 → jstat 看 GC → jstack 看线程 → jmap dump 看堆 → MAT 找大对象**。Arthas 是线上神器（不重启、命令式诊断）。

---

## 7. JVM 调优实战

### 7.1 常用调优参数？

| 参数 | 含义 | 建议 |
|------|------|------|
| -Xms / -Xmx | 初始堆 / 最大堆 | 生产设相同值（避免扩容抖动） |
| -XX:NewRatio | 老年代:新生代（默认 2） | 根据对象存活分布调整 |
| -XX:SurvivorRatio | Eden:Survivor（默认 8） | 保证 Survivor 够装"熬过一次 GC"的对象 |
| -XX:MaxGCPauseMillis | G1 停顿目标 | 根据业务容忍度设（如 100ms） |
| -XX:+UseG1GC / UseZGC | 选收集器 | 常规 G1；大堆低延迟 ZGC |
| -XX:+HeapDumpOnOutOfMemoryError | 自动堆转储 | 生产必须开 |
| -XX:+PrintGCDetails（及日志文件） | GC 日志 | 生产必须开 |

### 7.2 调优决策流程（先调代码，再调参数）？

```text
① 明确目标：吞吐优先（批处理）还是低延迟（在线交易）？
② 量化现状：GC 频率/停顿、对象分配速率（jstat）
③ 代码层优先：内存泄漏、大对象、缓存设计（90% 的问题在代码）
④ 参数层：堆大小 → 比例（NewRatio/SurvivorRatio）→ 收集器 → 停顿目标
⑤ 验证：压测对比 GC 日志指标，一次只改一个参数
```

**追问：** -Xms 与 -Xmx 为什么要相等？→ 避免扩容/缩容的 STW 与性能抖动。停顿时间与吞吐矛盾？→ 更小停顿目标 → 更多并发回收 → 可能降低吞吐；需按业务权衡。

---

> 🎯 **核心要点**：JVM 面试的完整链路 = **内存结构（5 区）→ 对象与引用（GC Roots）→ 类加载（双亲委派 + 打破）→ 算法与分代（为什么）→ 收集器演进（G1/ZGC 特点）→ 排查实战（OOM 五步法）**。前四层是"八股"，后两层是"区分度" — 有真实排查经历的人，聊起来完全不同。

**下一模块**：[05-Java新特性与JDK演进高频题](05-Java新特性与JDK演进高频题.md) / **返回总览**：[00-Java核心面试大全总览](00-Java核心面试大全总览.md)
