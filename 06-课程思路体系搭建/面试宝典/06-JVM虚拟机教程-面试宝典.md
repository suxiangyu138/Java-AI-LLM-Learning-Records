# JVM虚拟机教程 面试宝典
> 基于课程大纲全面覆盖JVM面试高频考点，包含基础概念、深度原理、实战场景、手写代码、系统设计及常见坑点。

## 目录

- [一、基础概念速答（20题）](#一基础概念速答20题)
- [二、深度原理剖析（15题）](#二深度原理剖析15题)
- [三、实战场景题（12题）](#三实战场景题12题)
- [四、手写代码题（8题）](#四手写代码题8题)
- [五、系统设计题（5题）](#五系统设计题5题)
- [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
- [七、面试回答模板（Top 5高频题）](#七面试回答模板top-5高频题)
- [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（20题）

### 1.1 JVM内存区域有哪些？

JVM 内存区域主要分为线程私有和线程共享两大类。线程私有区域包括**程序计数器**（记录当前线程执行的字节码行号）、**虚拟机栈**（存储局部变量表、操作数栈、动态链接、方法出口等）和**本地方法栈**（为 Native 方法服务）。线程共享区域包括**堆**（存放对象实例，是 GC 主要区域）和**方法区**（存储类信息、常量、静态变量、即时编译后的代码等）。JDK 8 之后方法区改为元空间（Metaspace），使用本地内存，不再发生永久代 OOM。

### 1.2 类加载过程包含哪些阶段？

类加载过程分为 **加载（Loading）→ 连接（Linking）→ 初始化（Initialization）** 三大步，其中连接又细分为**验证（Verification）→ 准备（Preparation）→ 解析（Resolution）** 三个子阶段。

- **加载**：通过全限定名获取类的二进制字节流，在方法区生成 `Class` 对象。
- **验证**：确保字节流符合 JVM 规范（文件格式验证、元数据验证、字节码验证等）。
- **准备**：为类变量（static）分配内存并设置零值（如 `int` 为 0，`Object` 为 null）。
- **解析**：将常量池中的符号引用替换为直接引用。
- **初始化**：执行类构造器 `<clinit>()` 方法，按顺序赋值 static 变量和执行 static 代码块。

### 1.3 GC Roots 有哪些？

GC Roots 是垃圾收集器判断对象是否存活的根引用集合，包括以下对象：

1. **虚拟机栈（栈帧中的局部变量表）引用的对象**，即各线程栈中正在调用的方法参数、局部变量等。
2. **方法区中静态属性引用的对象**，如 `static` 字段引用的对象。
3. **方法区中常量引用的对象**，如 `final` 常量引用的 String 对象。
4. **本地方法栈中 JNI（Native 方法）引用的对象**。
5. **Java 虚拟机内部的引用**，如基本数据类型对应的 Class 对象、常驻的异常对象（NullPointerException 等）、系统类加载器。
6. **所有被 synchronized 持有的对象**。
7. **反映 Java 虚拟机内部情况的 JMXBean、JVMTI 回调等**。

> 💡 **Tip**：在 GC 根节点枚举时，必须暂停所有用户线程（STW），但 CMS 和 G1 可以通过 `-XX:+UsePerfData` 等参数配合 OopMap 加速根节点扫描。

### 1.4 对象一定分配在堆中吗？

**不一定**。随着 JIT 编译期的**逃逸分析**（Escape Analysis）技术发展，如果对象未发生逃逸（即方法内局部使用，不会被外部引用），JVM 会对其进行如下优化：

1. **栈上分配**：将对象拆解为标量（标量替换），直接在栈帧中分配，方法结束后自动销毁，减少 GC 压力。
2. **同步消除**：如果对象未逃逸，对其加的锁会被消除（如 `StringBuffer` 在方法内使用时）。
3. **标量替换**：将对象成员变量分解为多个局部变量存储。

> 🎯 **示例**：`-XX:+DoEscapeAnalysis -XX:+EliminateAllocations` 开启逃逸分析后，简单 POJO 可能在栈上分配而非堆中。

### 1.5 强、软、弱、虚引用区别？

| 引用类型 | 回收时机 | 用途 | 是否可通过 ReferenceQueue 获取 |
|---------|---------|------|------------------------------|
| **强引用** | 永不回收，OOM 才终止 | `new Object()` | 否 |
| **软引用** | 内存不足时回收 | 缓存实现（如图片缓存） | 是，回收后入队 |
| **弱引用** | 下一次 GC 时回收 | `ThreadLocalMap` 的 Key、WeakHashMap | 是，回收后入队 |
| **虚引用** | 任何时候可能回收 | 跟踪对象回收（NIO DirectBuffer 清理） | 是，但无法通过 get() 获取对象 |

> ⚠️ **注意**：虚引用 `PhantomReference` 的 `get()` 永远返回 `null`，必须配合 `ReferenceQueue` 使用，其作用是在对象被回收时得到通知，从而执行资源释放。

### 1.6 OOM 一定会导致 JVM 退出吗？

**不一定**。OOM 按类型可分为：

1. **Java heap space**：堆内存溢出。如果该 OOM 发生在非守护线程中，通常会导致该线程终止，但 JVM 主线程可能继续运行。
2. **Metaspace**：元空间溢出，通常会导致类加载相关线程异常退出。
3. **Unable to create new native thread**：线程数超限，可能导致整个进程不稳定。
4. **Direct buffer memory**：直接内存溢出，NIO 相关操作会抛出异常。

> 💡 **关键点**：JVM 进程退出取决于 OOM 发生的线程是否为守护线程、是否被 `UncaughtExceptionHandler` 捕获。在生产环境中建议配置 `-XX:+ExitOnOutOfMemoryError` 或 `-XX:+CrashOnOutOfMemoryError` 让 JVM 在 OOM 时主动退出。

### 1.7 内存泄漏和内存溢出的区别？

| 对比项 | 内存泄漏（Memory Leak） | 内存溢出（Out Of Memory） |
|-------|----------------------|------------------------|
| **定义** | 已分配对象无法被 GC 回收，持续占用内存 | 内存不足以分配新对象 |
| **关系** | 泄漏是溢出的原因之一 | 溢出是泄漏可能导致的结果 |
| **举例** | `static` 集合不断增加元素未移除 | 堆内存不足新建对象 |
| **表现** | 可用内存逐渐减少，GC 频率上升 | 直接抛出 `OutOfMemoryError` |
| **解决** | 分析堆 dump，找到泄漏对象引用链 | 增大堆内存或优化代码 |

### 1.8 堆一定是线程共享的吗？

**大部分是**。堆是 JVM 中最大的内存区域，是所有线程共享的。但为了提升对象分配效率，JVM 引入了 **TLAB（Thread Local Allocation Buffer）** 机制：每个线程在堆的 Eden 区预留一块私有的缓冲区（TLAB），线程优先在 TLAB 中分配对象，避免锁竞争。当 TLAB 空间不足时，需要重新申请。因此：

- **逻辑上**：堆是所有线程共享的。
- **物理上**：TLAB 是线程私有的分配区域（但仍然属于堆内存）。

通过 `-XX:+UseTLAB`（默认开启）和 `-XX:TLABSize` 可控制 TLAB 行为。

### 1.9 类常量池和运行时常量池的区别？

| 对比项 | Class 常量池 | 运行时常量池 |
|-------|-------------|-------------|
| **位置** | `.class` 文件中 | 方法区（元空间） |
| **时间** | 编译期生成 | 类加载后，Class 常量池解析放入 |
| **内容** | 字面量 + 符号引用 | 字面量 + 直接引用（解析后） |
| **动态性** | 固定不可变 | 运行时可以添加新常量（如 `String.intern()`） |

### 1.10 什么情况会导致 JVM 退出？

1. 所有**非守护线程**全部执行完毕（正常退出）。
2. 调用 `System.exit()` 或 `Runtime.halt()` / `Runtime.exit()`。
3. 发生未被捕获的异常/错误且主线程结束。
4. OOM 且配置了 `-XX:+ExitOnOutOfMemoryError`。
5. 操作系统发送信号（如 `SIGKILL`, `SIGTERM`）。
6. 使用 `jstack` / `jmap` 等外部工具强制终止进程。

> 💡 **守护线程（Daemon Thread）**不会阻止 JVM 退出，GC 线程就是典型的守护线程。当所有用户线程结束时，JVM 会直接终止所有守护线程并退出。

### 1.11 JVM 内存为什么要分代？

分代设计的核心思想是**弱代假设**（Weak Generational Hypothesis）：绝大多数对象的生命周期很短，朝生夕灭（如循环内的临时对象）。

- **新生代（Young Generation）**：存放短生命周期对象，GC 频率高但单次耗时短，使用复制算法，分为 Eden + Survivor0 + Survivor1。
- **老年代（Old Generation）**：存放经过多轮 GC 后仍存活的长生命周期对象，GC 频率低但单次耗时长，使用标记-整理或标记-清除算法。
- **元空间（Metaspace）**：存放类元信息，使用本地内存。

> 💡 **不分代会怎样？** 每次 GC 都需要扫描整个堆，效率极低。分代后，大部分对象在新生代就被回收，只有少数对象晋升老年代，大幅降低 Full GC 频率。

### 1.12 GC 在任意时刻都能进行吗？

**不是**。GC 在执行时存在 **安全点（SafePoint）** 和 **安全区域（SafeRegion）** 的概念：

- **安全点（SafePoint）**：线程只有在到达安全点时才能暂停进行 GC。安全点通常选择在循环末尾、方法返回前、异常抛出前等位置。
- **安全区域（SafeRegion）**：对于处于 Sleep 或 Blocked 状态的线程，无法主动到达安全点，它们会标记自己进入安全区域，GC 期间不会影响它们。
- **STW（Stop The World）**：GC 根节点枚举、全局安全点同步等阶段必须暂停所有用户线程。

> ⚠️ **避免 GC 不必要的触发**：频繁的 System.gc() 会触发 Full GC，应尽量避免显式调用。

### 1.13 对象的创建过程？

对象的创建分为 5 个步骤：

1. **类加载检查**：检查能否在常量池中定位到类的符号引用，若未加载则执行类加载过程。
2. **分配内存**：根据对象大小在堆中分配连续空间，分配方式有**指针碰撞**（规整堆）和**空闲列表**（不规整堆）。
3. **初始化零值**：将分配到的内存空间全部初始化为零值（不包括对象头），保证字段可直接使用。
4. **设置对象头**：设置 Mark Word（GC 分代年龄、锁状态等）和 Klass Pointer（指向类的元数据）。
5. **执行 `<init>()` 方法**：执行构造函数，按照代码进行字段赋值。

### 1.14 对象内存分配方式有哪些？

| 分配方式 | 适用场景 | 描述 |
|---------|---------|------|
| **指针碰撞（Bump The Pointer）** | Serial、ParNew 等带 Compact 的收集器 | 内存绝对规整，空闲指针向空闲侧移动即可 |
| **空闲列表（Free List）** | CMS 基于 Mark-Sweep 的收集器 | 维护空闲块列表，分配时找足够大的块 |
| **TLAB** | 所有收集器 | 线程本地缓冲区，避免 CAS 竞争 |
| **栈上分配** | JIT 逃逸分析后 | 对象不逃逸时分配在栈帧中 |

### 1.15 对象的内存布局？

对象在内存中分为三个部分：

1. **对象头（Header）**：
   - **Mark Word**（8 字节/64 位）：存储 GC 标记、分代年龄、锁状态（偏向锁/轻量锁/重量锁）、hashCode 等。
   - **Klass Pointer**（4 字节/未压缩前 8 字节）：指向类的元数据，用于确定对象类型。
   - **数组长度**（仅数组对象，4 字节）。
2. **实例数据（Instance Data）**：各字段的实际值，按声明顺序和大小对齐存储。
3. **对齐填充（Padding）**：保证对象起始地址是 8 字节的整数倍。

> 💡 对象大小可通过 `jol-core`（Java Object Layout）工具查看。

### 1.16 如何判断对象仍然存活？

两种算法：

1. **引用计数法（Reference Counting）**：每个对象维护一个引用计数器，为 0 时回收。缺点是无法解决**循环引用**问题（A 引用 B，B 引用 A，但外部无引用）。
2. **可达性分析（Reachability Analysis）**：从 GC Roots 出发向下搜索，遍历的路径称为**引用链（Reference Chain）**。当一个对象到 GC Roots 没有任何引用链相连，则判定为可回收。JVM 采用此算法。

> 🎯 **可达性分析 + finalize()**：对象第一次被标记后，如果覆盖了 `finalize()` 且未执行过，会进入 F-Queue 队列等待 Finalizer 线程执行。如果在此方法中重新建立引用链，对象可自救。但不推荐使用，因为执行时机不可控。

### 1.17 垃圾收集算法有哪些？

| 算法 | 原理 | 优点 | 缺点 |
|-----|------|------|------|
| **标记-清除** | 标记存活对象，清除未标记对象 | 无需移动对象 | 产生内存碎片 |
| **标记-整理** | 标记存活对象，向一端移动压缩 | 无内存碎片 | 移动对象开销大 |
| **复制算法** | Eden + Survivor，存活对象复制到另一半 | 快速高效，无碎片 | 浪费 10% 空间 |
| **分代收集** | 新生代复制，老年代标记-整理/清除 | 综合各算法优点 | 实现复杂 |

### 1.18 OOM 有哪些类型？

| OOM 类型 | 触发原因 | 排查方向 |
|---------|---------|---------|
| `Java heap space` | 堆内存不足，对象无法分配 | 检查堆大小、是否有内存泄漏 |
| `Metaspace` | 类加载过多 | 检查动态代理、CGlib 生成类过多 |
| `GC overhead limit exceeded` | 98% 时间花在 GC 且回收 < 2% 堆 | 调整 GC 参数或增加堆内存 |
| `Unable to create new native thread` | 线程数超限 | 减少线程数或调整 OS 线程限制 |
| `Direct buffer memory` | 直接内存不足 | 检查 NIO 缓存是否释放 |

### 1.19 JVM 常见参数配置？

| 参数 | 说明 |
|------|------|
| `-Xms` | 初始堆大小（如 `-Xms4g`） |
| `-Xmx` | 最大堆大小（如 `-Xmx4g`） |
| `-Xmn` | 新生代大小 |
| `-XX:MetaspaceSize` | 元空间初始大小 |
| `-XX:MaxMetaspaceSize` | 元空间最大大小 |
| `-XX:SurvivorRatio` | Eden/Survivor 比例（默认 8:1:1） |
| `-XX:MaxTenuringThreshold` | 晋升老年代阈值（默认 15） |
| `-XX:+UseG1GC` | 使用 G1 收集器 |
| `-XX:+PrintGCDetails` | 打印 GC 日志 |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 时自动 dump 堆 |
| `-XX:HeapDumpPath` | dump 文件路径 |
| `-XX:MaxDirectMemorySize` | 直接内存上限 |

### 1.20 什么是 STW（Stop The World）？

STW 是指垃圾回收过程中，JVM 暂停所有用户线程的现象。在以下阶段会发生 STW：

1. **GC Roots 枚举**：需要扫描所有线程的栈、JNI 句柄等，确定根对象。
2. **安全点同步**：等待所有线程到达安全点。
3. **CMS 初始标记**：标记 GC Roots 直接引用的对象。
4. **CMS 重新标记**（Remark）：修正并发标记期间变动的引用。
5. **G1 初始标记/最终标记/清理**：各阶段都有短暂 STW。

> 💡 减少 STW 时间是 JVM 优化的核心目标，G1 和 ZGC 通过并发标记、增量回收等方式大幅缩减 STW 时间。

---

## 二、深度原理剖析（15题）

### 2.1 CMS 收集器底层原理

CMS（Concurrent Mark Sweep）以**最短停顿时间**为目标，适用于响应时间敏感的应用。

**四阶段流程**：

```
1. 初始标记（Initial Mark）—— STW，标记 GC Roots 直接关联对象，速度快
2. 并发标记（Concurrent Mark）—— 与用户线程并发，从 GC Roots 遍历所有对象
3. 重新标记（Remark）—— STW，修正并发标记期间因用户线程运行导致变动的引用
4. 并发清除（Concurrent Sweep）—— 与用户线程并发，清除不可达对象
```

**优点**：低延迟，适合 Web 应用。  
**缺点**：无法处理浮动垃圾，产生内存碎片，并发阶段占用 CPU。  
**参数**：`-XX:+UseConcMarkSweepGC`、`-XX:CMSInitiatingOccupancyFraction=70`、`-XX:+UseCMSCompactAtFullCollection`。

### 2.2 G1 收集器底层原理

G1（Garbage First）将堆划分为 2048 个 **Region**（1MB~32MB），每个 Region 可独立扮演 Eden、Survivor、Old 或 Humongous（巨型对象）区域。

**核心特色**：

1. **分区堆**：不再要求物理连续，逻辑上分代。
2. **Region**：每个 Region 大小通过 `-XX:G1HeapRegionSize` 设置。
3. **优先级回收**：跟踪每个 Region 的 GC 效益，优先回收收益最大的 Region（"Garbage First"）。
4. **SATB（Snapshot At The Beginning）**：在并发标记开始时生成快照，确保不遗漏新引用。
5. **RSet（Remembered Set）**：记录 Region 之间的跨代引用，避免全堆扫描。

**工作周期**：

```
年轻代 GC（Young GC） → 并发标记周期（Concurrent Marking） → 混合回收（Mixed GC） → Full GC（若并发失败）
```

> 💡 G1 是 JDK 9+ 的默认收集器，目标是**可预测的停顿时间**，通过 `-XX:MaxGCPauseMillis=200` 设置。

### 2.3 ZGC 收集器原理

ZGC 是 JDK 11 引入的低延迟垃圾收集器，**停顿时间不超过 10ms**，与堆大小无关。

**核心技术**：

1. **染色指针（Colored Pointer）**：利用指针的 64 位地址空间中的高 4 位存储 Meta 信息（Finalizable、Remapped、Marked0、Marked1），无需对象头额外空间。
2. **读屏障（Load Barrier）**：在读取对象引用时执行检查，如果指针状态异常，则修正指针引用。
3. **并发整理**：ZGC 的**所有阶段**均并发执行，无长时间 STW。
4. **Region 动态大小**：支持 Small（2MB）、Medium（32MB）、Large（N×2MB）三种 Region。

```java
// ZGC 启用参数
// -XX:+UseZGC -Xmx16g -XX:ZCollectionInterval=120 -XX:ZAllocationSpikeTolerance=5
```

> 🎯 ZGC 适合大堆（几百 GB 以上）、低延迟要求严格（<10ms）的服务。

### 2.4 Shenandoah GC 原理

Shenandoah 是 OpenJDK 12 引入的、与 ZGC 类似的低延迟 GC，采用了不同的技术路线：

1. **Brooks Pointer**：在对象头部增加一个转发指针（Brooks Pointer），用于对象移动时重定向访问。
2. **读/写屏障**：Shenandoah 使用写屏障 + 部分读屏障，与 ZGC 的全读屏障不同。
3. **并发整理（Concurrent Compaction）**：将整理也变成并发操作，无需 STW。

**与 ZGC 的对比**：

| 特性 | ZGC | Shenandoah |
|------|-----|-----------|
| 核心技术 | 染色指针 | Brooks Pointer |
| 屏障类型 | 读屏障 | 写屏障为主 |
| JDK 版本 | JDK 11 (production) | JDK 12 (experimental) |
| 堆大小支持 | 最大 16TB | 无上限 |
| 额外内存开销 | 约 15% | 约 15-30% |

### 2.5 三色标记算法详解

三色标记是 CMS 和 G1 使用的并发标记算法，用于解决 STW 时间过长的问题。

**三种颜色**：

| 颜色 | 含义 | 处理方式 |
|------|------|---------|
| **白色** | 尚未被标记的对象 | GC 结束后仍为白色的对象视为不可达 |
| **灰色** | 自身已标记，但其引用对象未标记 | 需要进一步扫描 |
| **黑色** | 自身已标记且所有引用也标记完 | 无需再扫描 |

**标记过程**：

1. 初始状态：所有对象均为白色，GC Roots 入栈。
2. 从 GC Roots 开始遍历，将根对象标记为灰色。
3. 取出灰色对象，将其直接引用的对象标记为灰色，自身变为黑色。
4. 重复步骤 3，直到没有灰色对象。
5. 最终白色对象判定为可回收。

**并发标记的问题**：

1. **对象消失**（漏标）：黑色对象引用了一个白色对象，而该白色对象到 GC Roots 的引用被切断，导致白色对象被漏回收。
2. **浮动垃圾**：并发标记期间新产生的垃圾对象，只能等下次 GC 处理。

**解决方案**：
- **增量更新（Incremental Update）**：CMS 采用，当黑色对象引用白色对象时，将黑色变回灰色重新扫描。
- **SATB（Snapshot At The Beginning）**：G1 采用，记录标记开始时所有存活对象的快照，标记过程中新增的引用全部记录（通过写屏障），最终标记时重新处理。

> ⚠️ **SATB vs 增量更新**：SATB 可能产生更多浮动垃圾但避免多次扫描，增量更新更精确但需要重扫黑色对象。

### 2.6 对象头结构（Mark Word）

在 64 位 JVM 中，Mark Word 占用 8 字节（64 位），其结构根据对象状态变化：

| 状态 | 标志位 | 内容 |
|------|--------|------|
| 无锁 | 01 | unused(25bit) | hashCode(31bit) | unused(1bit) | 分代年龄(4bit) | 偏向锁位(1bit) | 01(2bit) |
| 偏向锁 | 01 | 线程ID(54bit) | epoch(2bit) | unused(1bit) | 分代年龄(4bit) | 偏向锁位=1 | 01(2bit) |
| 轻量锁 | 00 | 指向栈中锁记录的指针(62bit) | 00(2bit) |
| 重量锁 | 10 | 指向互斥量（Monitor）的指针(62bit) | 10(2bit) |
| GC 标记 | 11 | 未使用 | 11(2bit) |

### 2.7 逃逸分析技术

逃逸分析是 JIT 编译器的重要优化手段，分析对象的作用域是否逃逸出方法外。

**三种逃逸级别**：

1. **全局逃逸**：对象被赋值给静态变量或存入线程共享集合。
2. **参数逃逸**：对象作为参数传递给其他方法。
3. **不逃逸**：对象仅在本方法作用域内使用。

**优化效果**：

```java
public class EscapeAnalysisDemo {
    // 开启逃逸分析后：-XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks
    
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100_000_000; i++) {
            createPoint();  // 内部 Point 对象不逃逸
        }
        System.out.println("耗时: " + (System.currentTimeMillis() - start));
    }

    public static int createPoint() {
        Point p = new Point(1, 2); // 未逃逸 -> 栈上分配/标量替换
        return p.x + p.y;
    }
}
```

### 2.8 JIT 编译技术

JIT（Just-In-Time）编译是 JVM 将热机代码编译为本地机器码的过程。

**关键组件**：

1. **热点检测（HotSpot Detection）**：基于方法调用计数器和回边计数器，达到阈值（`-XX:CompileThreshold`，Client 1500，Server 10000）则触发编译。
2. **分层编译（Tiered Compilation）**：
   - **Tier 0**：解释执行。
   - **Tier 1-3**：C1 编译器（简单优化，收集 profiling）。
   - **Tier 4**：C2 编译器（深度优化，生成高性能机器码）。
3. **常见优化**：方法内联、逃逸分析、去虚拟化、循环展开、锁消除等。

> 💡 `-XX:+PrintCompilation` 可打印 JIT 编译日志，观察方法被编译的时机。

### 2.9 类加载器源码分析（双亲委派模型）

双亲委派模型（Parent Delegation Model）的核心实现位于 `java.lang.ClassLoader.loadClass()`：

```java
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
        // 1. 检查类是否已加载
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                // 2. 委派给父类加载器
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {
                    // 3. 没有父类加载器，交给 Bootstrap ClassLoader
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // 4. 父加载器无法加载，自己加载
            }
            if (c == null) {
                c = findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}
```

**四层加载器**：

| 类加载器 | 加载目录 | 实现语言 |
|---------|---------|---------|
| Bootstrap ClassLoader | `$JAVA_HOME/jre/lib/rt.jar` | C++ |
| Extension ClassLoader | `$JAVA_HOME/jre/lib/ext/` | Java |
| Application ClassLoader | `classpath` | Java |
| Custom ClassLoader | 自定义路径 | Java |

**破坏双亲委派的场景**：
- **JDBC 驱动加载**：`ServiceLoader` 使用线程上下文类加载器（`Thread.currentThread().getContextClassLoader()`）加载。
- **Tomcat 类加载器**：每个 WebApp 独立加载，优先加载自己的类（先尝试自加载，失败再委派）。
- **SPI 机制**：通过 `ContextClassLoader` 打破双亲委派的层级。

### 2.10 TLAB 机制详解

TLAB（Thread Local Allocation Buffer）是新生代中每个线程私有的分配区域。

```java
// TLAB 工作流程
// 1. 线程首次分配对象时，从 Eden 区申请一块 TLAB（默认约为 Eden 的 1%）
// 2. 后续对象分配优先在 TLAB 内进行（无锁分配，仅指针碰撞）
// 3. TLAB 耗尽则重新从 Eden 申请新的 TLAB
// 4. 若对象大小超过 TLAB 剩余空间，直接在 Eden 进行 CAS 分配（PLAB）
```

**参数控制**：`-XX:+UseTLAB`（默认启用）、`-XX:TLABSize`、`-XX:TLABWasteTargetPercent`（默认 1%）。

> 🎯 **TLAB 的意义**：将堆空间逻辑上按线程划分，大幅减少并发分配时的 CAS 竞争，提升分配效率数十倍。

### 2.11 CMS 重新标记（Remark）详解

CMS 的 Remark 阶段是解决并发标记期间**引用变更**问题的关键步骤。

**为什么需要 Remark？**并发标记期间用户线程仍在运行，对象的引用关系可能改变，导致两种问题：

1. **浮动垃圾**：标记为黑色的新垃圾（本次不回收，等下次 GC）。
2. **漏标**：黑色对象新引用的白色对象（如果不修正会被误回收）。

**处理方式**：CMS 采用**增量更新**——通过写屏障（Write Barrier）记录并发期间黑色对象的新增引用，在 Remark 阶段重新扫描这部分对象。

### 2.12 G1 RSet 与卡表（Card Table）

**卡表（Card Table）** 是一种用于解决跨代引用扫描问题的数据结构：

- 堆空间划分为 512 字节的 **Card**。
- 一个字节的 **Card Table** 数组记录每个 Card 是否被修改过（dirty）。
- YGC 时只需扫描 dirty 的 Card，而不必扫描整个老年代。

**G1 的 Remembered Set（RSet）** 是卡表的增强版：

- 每个 Region 维护一个 RSet，记录其他 Region 指向本 Region 的引用。
- RSet 使用**卡页（Card Page）**粒度的位图 + 稀疏哈希表实现。
- 混合 GC 时，通过 RSet 快速定位存活对象，无需全堆扫描。

### 2.13 G1 SATB 详解

SATB（Snapshot At The Beginning）是 G1 并发标记的核心算法，用于解决并发标记的漏标问题。

| 阶段 | 描述 | 是否 STW |
|------|------|---------|
| 初始标记 | 标记 GC Roots 直接引用的对象（伴随 YGC 进行） | 是 |
| 并发标记 | 从 GC Roots 遍历对象图 | 否 |
| 最终标记 | 处理 SATB 队列中记录的引用变更，完成标记 | 是 |
| 清理 | 统计活对象，选择回收 Region | 否 |

**SATB 写屏障**：

```java
// 伪代码：当引用 p.x = o 时，记录 o 之前的值
// 即记录标记开始时该引用指向的对象
void pre_write_barrier(oop* field, oop new_value) {
    oop old_value = *field;
    if (old_value != null && is_concurrent_marking()) {
        satb_mark_queue().enqueue(old_value); // 将旧值加入 SATB 队列
    }
}
```

### 2.14 直接内存（Direct Memory）原理

直接内存是 `NIO` 通过 `DirectByteBuffer` 在堆外分配的内存，通过**虚引用 + Cleaner** 机制自动释放。

**底层原理**：

```java
// 直接内存分配流程
ByteBuffer buffer = ByteBuffer.allocateDirect(1024); // 分配 1KB 直接内存

// 底层实现：
// 1. Unsafe.allocateMemory(size) 分配堆外内存
// 2. 创建 DirectByteBuffer 对象，包含内存地址引用
// 3. 注册 Cleaner（基于 PhantomReference），GC 回收 DirectByteBuffer 时触发 Deallocator
```

**优点**：减少 IO 操作时的内核态/用户态拷贝（零拷贝）。  
**缺点**：回收依赖 GC，且 `-XX:MaxDirectMemorySize` 限制。`-XX:+DisableExplicitGC` 会导致堆外内存回收不及时。

### 2.15 Arthas JVM 调优工具源码架构

Arthas 是阿里巴巴开源的 JVM 诊断工具，基于 **Instrumentation API + ASM 字节码增强** 实现。

**核心机制**：

1. **Attach 机制**：通过 `tools.jar` 的 `VirtualMachine.attach(pid)` 连接目标 JVM。
2. **Agent 启动**：向目标 JVM 加载 Agent jar，启动 Arthas 服务端。
3. **字节码增强**：使用 ASM 动态修改类的字节码，插入监控逻辑。
4. **命令执行**：通过 telnet/HTTP 协议与 Arthas 服务端交互。

**常用命令**：`dashboard`（看板）、`thread`（线程堆栈）、`sc`（搜索类）、`sm`（搜索方法）、`heapdump`（堆转储）。

---

## 三、实战场景题（12题）

### 3.1 线上 CPU 飙高如何排查？

**排查步骤**（以 Linux 为例）：

```bash
# 1. 找到 CPU 占用高的进程
top -c

# 2. 查看进程内 CPU 占用高的线程（注意 10 进制）
top -Hp <pid>

# 3. 将线程 ID 转为 16 进制
printf "%x\n" <thread_id>

# 4. 用 jstack 导出线程堆栈
jstack <pid> | grep -A 100 <nid_hex>
```

**常见原因**：死循环、频繁 GC（内存泄漏导致 Full GC）、线程竞争激烈（锁自旋）。

**Arthas 方式**：

```bash
# 使用 thread 命令
thread -n 3   # 查看最忙的 3 个线程
# 使用 dashboard
dashboard     # 实时面板查看各线程 CPU 占比
```

### 3.2 频繁 Full GC 怎么办？

**排查与解决流程**：

1. **看 GC 日志**：`-XX:+PrintGCDetails -Xloggc:gc.log`，分析 GC 频率、耗时、各代内存变化。
2. **堆 dump 分析**：用 `jmap -dump:live,format=b,file=heap.hprof <pid>` 或 Arthas `heapdump` 导出。
3. **分析对象**：用 MAT（Memory Analyzer Tool）分析大对象、泄漏可疑对象、GC Roots 引用链。
4. **常见原因**：
   - **晋升失败**（Promotion Failed）：新生代对象晋升时老年代空间不足，`-XX:+PrintTenuringDistribution` 查看。
   - **并发模式失败**（Concurrent Mode Failure）：CMS 回收速度跟不上分配速度，调大 `-XX:CMSInitiatingOccupancyFraction`。
   - **元空间不足**：类加载过多，动态代理大量生成代理类。
   - **System.gc() 调用**：通过 `-XX:+PrintGCDetails` 排查是否由 `System.gc()` 触发。

**调整策略**：

```bash
# 老年代触发比例从 70% 降至 50%，提前触发 CMS GC
-XX:CMSInitiatingOccupancyFraction=50 -XX:+UseCMSInitiatingOccupancyOnly
# 或切换到 G1
-XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### 3.3 OOM 如何定位？

**自动 dump 配置**：

```bash
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/path/to/dump/
```

**定位步骤**：

1. **查看异常栈**：OOM 类型和栈信息确定是堆（Java heap space）、元空间（Metaspace）还是栈（StackOverflowError）。
2. **MAT 分析 heap dump**：
   - **Leak Suspects Report**：快速定位泄漏嫌疑对象。
   - **Dominator Tree**：查看大对象、对象引用层级。
   - **GC Roots Path**：找对象被谁引用导致无法回收。
3. **常见 OOM 模式**：
   - **大对象 OOM**：一次性加载过多数据（如不分页的 SQL 查询）。
   - **泄漏 OOM**：`static` 集合无限制增长、`ThreadLocal` 未清除、InputStream 未关闭。

### 3.4 内存泄漏如何排查？

**典型泄漏模式及排查**：

```java
// 案例 1：静态集合泄漏
public class LeakExample {
    private static final List<byte[]> CACHE = new ArrayList<>();
    
    public void addData() {
        CACHE.add(new byte[10 * 1024 * 1024]); // 不断增长不会释放
    }
}

// 案例 2：ThreadLocal 泄漏
// 线程池中的线程未清理 ThreadLocal，导致 Entry 的 key 为 null（弱引用）但 value 仍存在
ThreadLocal<byte[]> tl = new ThreadLocal<>();
tl.set(new byte[1024 * 1024]); // 使用后忘记 remove()
```

**排查方法**：
1. `jstat -gcutil <pid> <interval>` 观察老年代持续增长。
2. `jmap -histo:live <pid>` 查看各类型实例数，找到异常增长的类型。
3. `jcmd <pid> GC.heap_dump /path/to/heap.hprof` dump 后 MAT 分析。

### 3.5 频繁 Minor GC 怎么办？

**排查步骤**：

1. **查看 GC 日志**：确认 Minor GC 频率（如间隔几秒一次）。
2. **检查 Eden 区大小**：如果 Eden 过小，临时对象无法容纳，触发频繁 Minor GC。
3. **检查 Survivor 区**：如果 Survivor 空间不足，对象过早晋升老年代。
4. **对象分配速率**：用 `jstat -gc <pid> 1000` 查看 `YGC` 和 `YGCT` 列，计算分配速率。

**优化方案**：

```bash
# 增大新生代大小
-Xmn2g
# 调整 Eden:Survivor 比例
-XX:SurvivorRatio=6  # Eden:S0:S1 = 6:1:1
# 调整晋升阈值
-XX:MaxTenuringThreshold=15
```

### 3.6 堆内存飙高问题排查

**排查命令链**：

```bash
# 1. 看堆内存概况
jmap -heap <pid>

# 2. 看对象直方图
jmap -histo:live <pid> | head -30

# 3. 查看 GC 情况
jstat -gcutil <pid> 1000 10

# 4. dump 分析
jmap -dump:live,format=b,file=heap.hprof <pid>
```

**Arthas 方式**：

```bash
# 查看堆内存 TOP 对象
vmtool --action getInstances --className java.util.HashMap --limit 10

# 看内存使用概览
memory
```

### 3.7 Arthas 使用场景大全

| 场景 | Arthas 命令 | 说明 |
|------|------------|------|
| 查看方法调用耗时 | `trace` | `trace com.example.service.UserService getUser` |
| 查看方法入参/返回值 | `watch` | `watch com.example.service.UserService getUser '{params,returnObj}' -x 3` |
| 热更新代码 | `redefine` | 替换已加载的类的字节码 |
| 查看异常调用栈 | `stack` | `stack com.example.service.UserService getUser` |
| 在线排查 OOM | `heapdump` | `heapdump /tmp/heap.hprof --live` |
| 查看线程状态 | `thread` | `thread -b` 查看死锁 |
| 修改运行中日志级别 | `ognl` | `ognl '@com.example.config.LoggerConfig@setLevel("DEBUG")'` |
| 查看 Spring Bean | `vmtool` | `vmtool -x 3 --action getInstances --className org.springframework.context.ApplicationContext` |

### 3.8 G1 GC 参数调优实战

**双十一高并发场景 G1 调优**：

```bash
# 基础配置
-Xms8g -Xmx8g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200          # 目标停顿时间 200ms
-XX:G1HeapRegionSize=4m            # Region 大小 4MB（堆 8G 时约 2048 个 Region）
-XX:G1NewSizePercent=5             # 新生代初始占比（5% = 400MB）
-XX:G1MaxNewSizePercent=60         # 新生代最大占比
-XX:InitiatingHeapOccupancyPercent=45  # 触发并发标记的堆占用阈值
-XX:ConcGCThreads=4                # 并发 GC 线程数
-XX:+ParallelRefProcEnabled        # 并行处理 Reference
```

**关键指标监控**：查看 GC 日志中 `Evacuation Pause` 的耗时和 Region 数量，调整 `MaxGCPauseMillis` 平衡吞吐量。

### 3.9 CMS 参数调优实战

```bash
# CMS 调优配置
-Xms6g -Xmx6g
-XX:+UseConcMarkSweepGC
-XX:+UseParNewGC
-XX:CMSInitiatingOccupancyFraction=65   # 老年代 65% 触发 CMS GC
-XX:+UseCMSInitiatingOccupancyOnly      # 只使用此阈值，不使用 JVM 计算值
-XX:+CMSScavengeBeforeRemark            # Remark 前执行一次 YGC，减少 Remark 扫描对象
-XX:CMSFullGCsBeforeCompaction=1        # 一次 Full GC 后执行一次整理
-XX:+CMSParallelRemarkEnabled           # 并行 Remark
```

### 3.10 线上系统 YGC 时间过长

**原因分析**：`SafePoint` 耗时太长、`StringTable` 过大、`JNI` 或 `System.gc()` 触发。

**排查命令**：

```bash
# 查看 Safepoint 耗时
-XX:+PrintSafepointStatistics -XX:PrintSafepointStatisticsCount=1

# 查看 StringTable 大小
jcmd <pid> VM.stringtable
```

**解决方案**：
1. 减小 `-XX:StringTableSize` 或增大以解决 hash 冲突导致的遍历耗时。
2. 通过 `-XX:+UseBiasedLocking` 关闭偏向锁（JDK 15+ 已默认关闭）。
3. 避免在 GC 日志中使用过多的 Print 参数（降低日志 I/O 影响）。

### 3.11 美团 JVM 面试题：CMS 并发失败（Concurrent Mode Failure）

**问题场景**：CMS 并发回收期间，老年代空间被快速占满，CMS 回收速度跟不上对象分配速度。

**根因**：
1. 老年代晋升过快（Survivor 空间过小，对象直接进入老年代）。
2. 并发标记阶段用户线程产生了大量浮动垃圾。
3. CMS 触发阈值设置过高，启动回收时老年代剩余空间已不足。

**解决方案**：
1. `-XX:CMSInitiatingOccupancyFraction=50` 提前触发 CMS GC。
2. 使用 `-XX:+CMSScavengeBeforeRemark`，Remark 前先 Young GC，减少并发标记期的对象晋升。
3. 增大老年代空间（增大 `-Xmx` 或调整新生代大小）。

### 3.12 字节跳动 JVM 面试题：大内存服务器 GC 选型

**问题**：32GB 堆内存、要求 GC 停顿不超过 50ms，如何选型？

**推荐方案**：

| 收集器 | 是否适合 | 原因 |
|--------|---------|------|
| CMS | 否 | JDK 14 已移除，大堆时碎片严重 |
| G1 | 部分适合 | 32G 堆可控制停顿在 200ms 左右，但 50ms 很难 |
| ZGC | **最适合** | JDK 17+ 生产可用，停顿 <10ms |
| Shenandoah | 备选 | 类似 ZGC，Red Hat 主导 |

**推荐配置**：
```bash
-XX:+UseZGC -Xmx32g -Xms32g -XX:ZCollectionInterval=60 -XX:ZAllocationSpikeTolerance=5
```

---

## 四、手写代码题（8题）

### 4.1 手动实现自定义类加载器

```java
import java.io.FileInputStream;
import java.io.IOException;

public class CustomClassLoader extends ClassLoader {
    private String classPath;

    public CustomClassLoader(String classPath) {
        this.classPath = classPath;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] classData = loadClassData(name);
            if (classData == null) {
                throw new ClassNotFoundException(name);
            }
            // 将字节数组转换为 Class 对象
            return defineClass(name, classData, 0, classData.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    private byte[] loadClassData(String name) throws IOException {
        String fileName = classPath + "/" + name.replace('.', '/') + ".class";
        try (FileInputStream fis = new FileInputStream(fileName)) {
            byte[] data = new byte[fis.available()];
            fis.read(data);
            return data;
        }
    }

    public static void main(String[] args) throws Exception {
        // 使用自定义类加载器加载类
        CustomClassLoader loader = new CustomClassLoader("D:/custom-classes/");
        Class<?> clazz = loader.loadClass("com.example.MyService");
        Object instance = clazz.newInstance();
        System.out.println("类加载器: " + clazz.getClassLoader());
        System.out.println("父类加载器: " + clazz.getClassLoader().getParent());
    }
}
```

### 4.2 模拟 OOM（堆溢出）

```java
import java.util.ArrayList;
import java.util.List;

/**
 * JVM 参数：-Xms32m -Xmx32m -XX:+HeapDumpOnOutOfMemoryError
 */
public class SimulateHeapOOM {
    static class OOMObject {
        private byte[] data = new byte[1024 * 1024]; // 1MB
    }

    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<>();
        int count = 0;
        try {
            while (true) {
                list.add(new OOMObject());
                count++;
                if (count % 10 == 0) {
                    System.out.println("已创建 " + count + " 个对象，占用约 " + count + " MB");
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OOM 发生在创建第 " + count + " 个对象时");
            throw e;
        }
    }
}
```

### 4.3 模拟栈溢出

```java
/**
 * JVM 参数：-Xss128k  设置线程栈大小 128KB
 */
public class SimulateStackOverflow {
    private static int depth = 0;

    public static void recursiveMethod() {
        depth++;
        recursiveMethod(); // 无限递归
    }

    public static void main(String[] args) {
        try {
            recursiveMethod();
        } catch (StackOverflowError e) {
            System.out.println("递归深度: " + depth + " 时发生栈溢出");
        }
    }
}
```

### 4.4 模拟死锁并排查

```java
public class SimulateDeadlock {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {
                System.out.println("线程1 持有锁A，等待锁B...");
                sleep(100);
                synchronized (LOCK_B) {
                    System.out.println("线程1 获取锁B");
                }
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {
                System.out.println("线程2 持有锁B，等待锁A...");
                sleep(100);
                synchronized (LOCK_A) {
                    System.out.println("线程2 获取锁A");
                }
            }
        }, "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { }
    }
}

// 排查命令：
// jstack <pid>  ->  看到 "Found 1 deadlock"
// Arthas: thread -b  ->  显示死锁线程
```

### 4.5 模拟内存泄漏

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimulateMemoryLeak {
    // 静态集合导致内存泄漏
    private static final List<byte[]> LEAK_CACHE = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("内存泄漏模拟开始，每 500ms 分配 2MB...");
        while (true) {
            LEAK_CACHE.add(new byte[2 * 1024 * 1024]); // 每次添加 2MB
            System.out.println("当前缓存大小: " + LEAK_CACHE.size() + " 个对象，约 "
                    + (LEAK_CACHE.size() * 2) + " MB");
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }
}
```

### 4.6 演示各种引用类型

```java
import java.lang.ref.*;

public class ReferenceTypesDemo {
    public static void main(String[] args) throws InterruptedException {
        // 强引用
        Object strongRef = new Object();
        System.out.println("强引用: " + strongRef);

        // 软引用（内存不足时回收）
        SoftReference<byte[]> softRef = new SoftReference<>(new byte[10 * 1024 * 1024]);
        System.out.println("软引用 get(): " + softRef.get());

        // 弱引用（GC 时立即回收）
        WeakReference<String> weakRef = new WeakReference<>(new String("WeakRef"));
        System.out.println("GC 前弱引用: " + weakRef.get());
        System.gc();
        System.out.println("GC 后弱引用: " + weakRef.get()); // null

        // 虚引用（PhantomReference）
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        PhantomReference<Object> phantomRef = new PhantomReference<>(new Object(), queue);
        System.out.println("虚引用 get() 永远 null: " + phantomRef.get()); // null
        System.gc();
        // 虚引用对象被回收后，PhantomReference 被放入 ReferenceQueue
    }
}
```

### 4.7 查看 Java 对象内存布局

```java
// 需要引入 jol-core 依赖
// Maven: org.openjdk.jol:jol-core:0.17

import org.openjdk.jol.info.ClassLayout;

public class ObjectLayoutDemo {
    static class User {
        private int id;
        private String name;
        private boolean active;
        private long timestamp;
    }

    public static void main(String[] args) {
        User user = new User();
        // 输出对象内存布局
        System.out.println(ClassLayout.parseInstance(user).toPrintable());
        
        // 输出:
        // OFFSET  SIZE      TYPE DESCRIPTION
        //     0     4           (object header)  -- Mark Word 部分
        //     4     4           (object header)  -- Mark Word 部分  
        //     8     4           (object header)  -- Klass Pointer
        //    12     4       int User.id
        //    16     1   boolean User.active
        //    17     3           (alignment/padding gap)
        //    20     8      long User.timestamp
        //    28     4    String User.name (引用)
        //    32     4           (loss due to the next object alignment)
        // Instance size: 36 bytes (aligned to 40 bytes)
    }
}
```

### 4.8 模拟字符串常量池 intern 行为

```java
public class StringInternDemo {
    public static void main(String[] args) {
        // JDK 8 测试
        String s1 = new String("a") + new String("b"); // new String("ab")
        s1.intern(); // "ab" 放入字符串常量池，并返回引用
        String s2 = "ab";
        System.out.println("s1 == s2: " + (s1 == s2)); // true (JDK 8)
        
        String s3 = new String("c") + new String("d");
        String s4 = "cd";  // "cd" 在常量池中
        s3.intern();       // "cd" 已经存在，不影响 s3
        System.out.println("s3 == s4: " + (s3 == s4)); // false
        
        // 总结：
        // JDK 7+ intern() 首次遇到字符串时，将堆中引用存入常量池（而非复制字符串）
        // 所以第一个例子 s1.intern() 后，"ab" 的常量池引用指向 s1，s2 == s1 为 true
    }
}
```

---

## 五、系统设计题（5题）

### 5.1 设计一个 GC 日志分析系统

**功能需求**：
1. 自动采集各节点的 GC 日志。
2. 可视化展示 GC 频率、各代内存变化、STW 时间。
3. 设置告警阈值（如 Full GC 频率 > 1次/小时、STW > 1s）。

**系统架构**：

```
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ Tomcat/Spring│   │ Tomcat/Spring│   │ Tomcat/Spring│
│  GC日志文件   │   │  GC日志文件   │   │  GC日志文件   │
└──────┬───────┘   └──────┬───────┘   └──────┬───────┘
       │                  │                  │
       ▼                  ▼                  ▼
┌─────────────────────────────────────────────────┐
│           日志采集 Agent（Filebeat/Logstash）     │
└─────────────────────┬───────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────┐
│              Kafka / 消息队列                      │
└─────────────────────┬───────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────┐
│     GC 日志解析服务（基于正则/Parsing 库）         │
│   - 解析 GC pause、各代容量、耗时、晋升信息        │
└─────────────────────┬───────────────────────────┘
                      │
         ┌────────────┴────────────┐
         ▼                         ▼
┌──────────────────┐   ┌──────────────────────────┐
│  时序数据库(InfluxDB)│   │   搜索引擎(ES)           │
└────────┬─────────┘   └──────────┬───────────────┘
         │                        │
         ▼                        ▼
┌──────────────────┐   ┌──────────────────────────┐
│  Grafana 可视化   │   │  告警服务（Prometheus +  │
│  - GC 频率图      │   │   AlertManager）          │
│  - STW 耗时图     │   │   - Full GC 告警          │
│  - 各代内存趋势    │   │   - OOM 告警             │
└──────────────────┘   └──────────────────────────┘
```

**GC 日志解析核心逻辑**：

```text
// G1 GC 日志示例（需解析的格式）
2024-01-15T10:30:00.123+0800: 3.456: [GC pause (G1 Evacuation Pause) (young)
  ... 47M->9M(128M), 0.0156780 secs]

// CMS GC 日志示例
2024-01-15T10:30:01.456+0800: 5.678: [Full GC (Allocation Failure)
  ... 1024M->512M(2048M), 0.6543210 secs]
```

### 5.2 设计 JVM 监控平台

**核心模块**：

| 模块 | 功能 | 技术选型 |
|------|------|---------|
| **指标采集** | 内存、CPU、GC、线程、类加载 | JMX + Micrometer + Prometheus |
| **健康检查** | JVM 存活检测、心跳 | Spring Boot Actuator / Health Endpoint |
| **日志聚合** | GC 日志、OOM 日志 | ELK / Loki |
| **实时诊断** | 远程执行 jstack、jmap、jcmd | Arthas Tunnel Server |
| **告警系统** | 阈值告警、趋势告警 | Prometheus AlertManager |
| **可视化** | 大盘面板 | Grafana |

**监控指标体系**：

```java
// Micrometer 定义的 JVM 指标（示例）
// jvm.memory.used          - 已用堆内存
// jvm.memory.max           - 最大堆内存
// jvm.gc.pause             - GC 停顿时间
// jvm.gc.memory.allocated  - 对象分配速率
// jvm.threads.live         - 活跃线程数
// jvm.classes.loaded       - 已加载类数量
```

### 5.3 设计一个自动 JVM 调优系统

**系统思路**：

1. **输入**：业务特征（响应时间敏感/吞吐量敏感）、硬件配置（CPU 核数、内存大小）。
2. **自动诊断**：通过 GC 日志分析，识别当前瓶颈（GC 频率过高、STW 过长、吞吐量低等）。
3. **参数推荐引擎**：基于规则引擎 + 历史经验库，推荐 JVM 参数组合。

**推荐规则示例**：

```text
IF (模式 = 响应时间敏感) AND (堆大小 > 8G)
  THEN 推荐收集器 = G1 OR ZGC
  THEN MaxGCPauseMillis = 200

IF (模式 = 批处理/离线计算) AND (CPU 核数 >= 16)
  THEN 推荐收集器 = Parallel Scavenge + Parallel Old
  THEN 目标 = 最大化吞吐量

IF (GC 日志诊断 = Concurrent Mode Failure 频繁)
  THEN 调大 CMSInitiatingOccupancyFraction = 50
    OR 切换收集器到 G1
```

### 5.4 设计高并发网关上 JVM 优化方案

**场景**：API 网关，日请求量 10 亿+，要求 P99 延迟 <100ms。

**JVM 优化方案**：

```bash
# 1. 使用 G1 收集器（低延迟 + 可预测停顿）
-XX:+UseG1GC
-XX:MaxGCPauseMillis=50

# 2. 堆大小合理设置（避免大堆 GC 长停顿）
-Xms8g -Xmx8g

# 3. 元空间监控
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=256m

# 4. GC 日志配置
-Xlog:gc*:file=gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100m

# 5. OOM 自动处理
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/dumps/
-XX:+ExitOnOutOfMemoryError

# 6. 减少安全点检查
-XX:+UnlockDiagnosticVMOptions
-XX:GuaranteedSafepointInterval=0  # 只在 JIT 和安全点日志需要时开启
```

**应用层优化**：
- 使用**堆外缓存**（如 Caffeine + Direct Memory）减少 GC 压力。
- **对象池化**复用大对象（如 ByteBuf、StringBuilder）。
- **异步非阻塞**（WebFlux / Netty）减少线程数。

### 5.5 设计一个双十一大促 JVM 应急预案

**三级预案体系**：

```text
一级预警（GC 指标异常）：
- 指标：YGC 耗时 > 1s 或 Full GC 触发
- 动作：自动扩缩容（增加 Pod 实例）、触发堆 dump

二级预警（内存使用率 > 80%）：
- 指标：堆内存使用率持续 > 80%
- 动作：自动 dump 堆导出用于事后分析，提前扩容

三级预警（OOM 发生）：
- 指标：OOM 发生
- 动作：-XX:+ExitOnOutOfMemoryError 自动重启，健康检查摘除节点
```

**大促前 JVM 检查清单**：
1. 确认 GC 日志已开启。
2. 确认 OOM dump 配置有效。
3. 压测验证 GC 停顿在预期范围内。
4. 确认 Prometheus + Grafana 监控大盘正常。
5. 确认告警通道（钉钉/电话），阈值合理设置。

---

## 六、常见坑点与最佳实践

### 6.1 常见坑点

| 坑点 | 问题描述 | 最佳实践 |
|------|---------|---------|
| `System.gc()` 滥用 | 显式触发 Full GC，影响性能 | 禁用 `-XX:+DisableExplicitGC`，依赖 JVM 自动 GC |
| 大对象直接进入老年代 | `-XX:PretenureSizeThreshold` 设置不当，导致大对象直接分配在老年代 | 合理设置阈值（如 1MB），或通过 G1 的 Humongous Region 处理 |
| CMS 碎片导致 Full GC | CMS 未压缩产生大量碎片，老年代无法分配连续空间 | `-XX:+UseCMSCompactAtFullCollection` 启用压缩 |
| TLAB 浪费 | TLAB 剩余空间浪费（`-XX:TLABWasteTargetPercent` 默认 1%） | 监控 TLAB 浪费率，适当调整大小 |
| 元空间 OOM | CGLib/动态代理生成大量代理类 | 设置 `-XX:MaxMetaspaceSize` 上限，关注类加载数 |
| `-XX:+DisableExplicitGC` 影响堆外内存 | NIO DirectByteBuffer 依赖 GC 回收，关闭后堆外内存泄漏 | 使用 `-XX:+ExplicitGCInvokesConcurrent` 替换，或手动调用 Cleaner |
| 日志过多影响性能 | `-XX:+PrintGCDetails` 在高频 GC 时产生大量 I/O | 使用 `-Xlog:gc*:file=gc.log:time,level,tags`（JDK 9+ Unified Logging） |
| 忽略安全点耗时 | SafePoint 同步可能超过 GC 本身耗时 | 使用 `-XX:+PrintSafepointStatistics` 监控安全点耗时 |
| 堆内存设置过大 | 大堆导致 Full GC 时间过长 | 大堆（>32GB）建议使用 ZGC 或 G1 |
| 线程栈过大 | `-Xss` 设置过大占用过多内存，降低可创建线程数 | 默认 1MB 通常够用，减少到 256KB~512KB |

### 6.2 最佳实践总结

> 💡 **生产环境 JVM 参数模板（Spring Boot / 微服务）**：

```bash
# ===== 堆设置 =====
-Xms4g -Xmx4g                    # 堆大小（建议 -Xms = -Xmx）
-Xmn1.5g                         # 新生代大小
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=256m

# ===== GC 选型 =====
-XX:+UseG1GC                     # 或 ParallelGC（批处理）
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=4m

# ===== 日志与诊断 =====
-Xlog:gc*:file=/data/logs/gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/dumps/

# ===== 错误处理 =====
-XX:+ExitOnOutOfMemoryError      # OOM 时主动退出（由容器重新调度）
-XX:+CrashOnOutOfMemoryError     # 或：OOM 时生成 crash 文件

# ===== 调试与性能 =====
-Djava.net.preferIPv4Stack=true
-XX:+AlwaysPreTouch              # 启动时预分配物理内存
-XX:+UseStringDeduplication      # G1 字符串去重
```

## 七、面试回答模板（Top 5高频题）

### 7.1 请介绍一下 JVM 内存区域

> **回答结构**：分区域描述 → JDK 8 变化 → TLAB 机制 → 各区域 OOM 场景

**参考答案**：
"JVM 内存区域分为线程私有和线程共享两大类。

线程私有区域包括：**程序计数器**，记录字节码执行行号，是 JVM 中唯一不会 OOM 的区域；**虚拟机栈**，每个方法调用对应一个栈帧，包含局部变量表、操作数栈等，递归过深会导致 StackOverflowError；**本地方法栈**，为 Native 方法服务。

线程共享区域包括：**堆**，存放所有对象实例，是 GC 的主要工作区域，可通过 -Xms/-Xmx 调节，主要 OOM 类型为 'Java heap space'；**方法区**（JDK 8 后改为元空间 Metaspace），存放类信息、常量、静态变量等，使用本地内存，避免了永久代 OOM。

此外，JDK 8 引入了字符串常量池和小整数常量池等运行时优化；线程分配对象时优先使用 TLAB 以提升并发分配效率。"

### 7.2 类加载过程与双亲委派模型

> **回答结构**：加载 → 连接 → 初始化 → 双亲委派 → 打破场景

**参考答案**：
"类加载分为加载、连接（验证、准备、解析）、初始化三个阶段。加载是通过全限定名获取类的二进制字节流并在方法区生成 Class 对象；验证是检查字节流格式是否符合规范；准备是为静态变量分配内存并设置零值；解析是把常量池的符号引用替换为直接引用；初始化是执行类构造器方法，为静态变量赋值。

双亲委派模型是指当一个类加载器收到加载请求时，先委派给父加载器，依次向上委托，直到 Bootstrap ClassLoader，只有父加载器无法加载时才自己加载。这样保证了核心 API 的一致性，防止用户自定义类覆盖核心类库。

打破双亲委派的典型场景有：JDBC 使用线程上下文类加载器加载驱动实现；Tomcat 为每个 WebApp 提供独立的类加载器，优先加载自己目录下的类；SPI 机制也采用类似的上下文加载器模式。"

### 7.3 CMS 和 G1 的区别

> **回答结构**：各自特点 → 核心差异 → 适用场景 → 面试官追问应对

**参考答案**：
"CMS 和 G1 是两款主流的低延迟垃圾收集器。

**CMS** 以最短停顿时间为目标，基于标记-清除算法，分为初始标记、并发标记、重新标记、并发清除四阶段。优点是低延迟、高响应，缺点是无法处理浮动垃圾、产生内存碎片、JDK 14 已移除。

**G1** 将堆划分为 2048 个 Region，支持分代和优先级回收，通过 SATB 解决并发标记问题，使用 RSet 管理跨代引用。G1 的停顿更可预测，可通过 `-XX:MaxGCPauseMillis` 控制。在 JDK 9+ 中 G1 是默认收集器。

主要区别：G1 支持**Region 优先级回收**（优先回收垃圾多的 Region），**无碎片**（使用复制/整理），**停顿可控**；CMS 更早进入并发标记（70% 老年代利用率即开始），但碎片和浮动垃圾问题突出。我之前的项目使用 G1，通过调整 Region 大小和 MaxGCPauseMillis，将 Full GC 频率从每天多次降到每周一次。"

### 7.4 线上 OOM 如何排查？

> **回答结构**：事前准备 → 事中排查 → 事后分析 → 预防措施

**参考答案**：
"排查 OOM 我有一套成熟的 SOP：

**事前准备**：开启 `-XX:+HeapDumpOnOutOfMemoryError` 和 `-XX:HeapDumpPath`，确保 OOM 时自动生成堆 dump 文件。配置告警，通过监控平台（Prometheus + Grafana）实时观察堆内存使用趋势。

**事中排查**：收到 OOM 告警后，首先通过 jstat -gcutil、jmap -heap 快速查看当前 JVM 状态。确认 OOM 类型——是堆溢出（Java heap space）、元空间溢出（Metaspace）还是无法创建线程。如果是堆溢出，使用 `jmap -dump:live,format=b,file=heap.hprof` 导出 dump。

**事后分析**：使用 MAT（Memory Analyzer Tool）分析 dump 文件。重点关注 Leak Suspects Report 找到泄漏嫌疑对象，Dominator Tree 查看大对象，GC Roots 路径分析查找引用链。

**预防措施**：合理设置 `-Xmx`，对大对象进行分页处理，使用连接池和线程池，定期执行 Code Review 防止代码层面的泄漏。"

### 7.5 如何进行 JVM 调优？

> **回答结构**：确定调优目标 → GC 选型 → 参数调整 → 监控验证

**参考答案**：
"JVM 调优的核心是平衡三个目标：**延迟**（STW 时间）、**吞吐量**（GC 时间占比）、**内存占用**。

我的调优方法论是：

第一步，**明确目标**。响应时间敏感型服务（如 API 网关）以低延迟为目标，批处理系统以高吞吐量为目标。

第二步，**GC 选型**。低延迟推荐 G1 或 ZGC（JDK 17+ 大堆），高吞吐量推荐 Parallel GC。堆大小建议 `-Xms = -Xmx` 避免扩缩容开销。

第三步，**参数调整**。以 G1 为例：`-XX:MaxGCPauseMillis=200` 设置目标停顿时间，`-XX:G1HeapRegionSize` 根据堆大小匹配合适的 Region，`-XX:InitiatingHeapOccupancyPercent=45` 控制并发标记触发时机。

第四步，**监控验证**。通过 GC 日志和监控指标（GC 频率、STW 耗时、各代内存变化）验证调优效果，持续迭代。

之前一个项目通过将 CMS 切换到 G1，并将堆大小从 4G 调整为 8G，Full GC 频率从每 10 分钟一次降至每 4 小时一次，P99 延迟从 500ms 降到 120ms。"

## 八、快速查漏补缺Checklist

### 8.1 基础概念 (共25个知识点)

- [ ] JVM 内存区域划分（堆、栈、方法区、程序计数器、本地方法栈）
- [ ] JDK 8 元空间替换永久代的原因和影响
- [ ] 类加载全过程（加载 → 验证 → 准备 → 解析 → 初始化）
- [ ] 双亲委派模型原理及打破方式
- [ ] 强软弱虚引用区别及使用场景
- [ ] GC Roots 包含哪些对象
- [ ] 分代回收的思想和依据
- [ ] 对象创建过程（类加载 → 分配内存 → 零值初始化 → 设对象头 → init）
- [ ] 对象内存布局（Mark Word、Klass Pointer、实例数据、对齐）
- [ ] TLAB 机制原理
- [ ] 安全点（SafePoint）和安全区域（SafeRegion）

### 8.2 GC 算法与收集器 (共15个知识点)

- [ ] 标记-清除、标记-整理、复制算法原理及优缺点
- [ ] 三色标记算法及并发标记问题（漏标、浮动垃圾）
- [ ] CMS 四阶段流程（初始标记、并发标记、重新标记、并发清除）
- [ ] G1 Region、SATB、RSet 原理
- [ ] G1 Young GC / Mixed GC / Full GC 触发条件
- [ ] CMS vs G1 vs ZGC vs Shenandoah 对比
- [ ] ZGC 染色指针、读屏障原理
- [ ] 并发模式失败（Concurrent Mode Failure）原因及处理
- [ ] GC 参数配置（各收集器关键参数）

### 8.3 调优与排障 (共10个知识点)

- [ ] CPU 飙高排查（jstack、top -H）
- [ ] 内存飙高排查（jmap -histo、MAT）
- [ ] OOM 定位与分析（heap dump + MAT Leak Suspects）
- [ ] Arthas 常用命令（dashboard、thread、trace、watch、heapdump）
- [ ] GC 日志分析方法
- [ ] 最优 JVM 参数模板（含 GC 日志、OOM 配置）
- [ ] 大堆（>32GB）场景下的 GC 选型
- [ ] 高并发场景下 YGC 优化
- [ ] 直接内存泄漏排查
- [ ] 安全点耗时优化

### 8.4 手写代码 (共8个知识点)

- [ ] 自定义类加载器
- [ ] 模拟堆 OOM
- [ ] 模拟栈溢出
- [ ] 模拟死锁 + jstack 排查
- [ ] 模拟内存泄漏（静态集合）
- [ ] 测试各种引用类型
- [ ] 对象内存布局查看（jol-core）
- [ ] String intern 行为验证

### 8.5 面试高频追问 (共8个知识点)

- [ ] 有没有实际调优经验？（准备好项目案例）
- [ ] 了解 ZGC 和 Shenandoah 吗？
- [ ] 说一说 G1 的 RSet 设计细节
- [ ] 三色标记中漏标如何解决（SATB vs 增量更新）？
- [ ] Concurrent Mode Failure 的原因和应对？
- [ ] JDK 8 StringTable 和 intern 的变化？
- [ ] JIT 编译的逃逸分析和标量替换？
- [ ] 如何自己实现一个 GC 日志分析工具？

---

> 🎯 **总结**：JVM 面试考察的是三个层次——**基础概念**（内存模型、类加载、GC 算法）、**实战能力**（调优经验、排障方法）和**原理深度**（源码级理解 CMS/G1/ZGC、三色标记、JIT 编译）。建议结合自己项目中遇到的实际案例进行准备，比背诵知识点更有说服力。
