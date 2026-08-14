# 02 JVM 内部注解深潜

> `jdk.internal.vm.annotation` 包里的注解是 JDK 自留的"JVM 调教开关"：@Stable 骗过 JIT 做常量折叠、@IntrinsicCandidate 请求 HotSpot 用汇编替换方法体、@Contended 隔离缓存行消除伪共享。普通开发者一辈子不会直接使用它们，但读懂它们 = 读懂 JIT 优化与内存布局的底层机制

## 📚 目录

1. [包的来历：JDK 9 的内部化迁移](#1-包的来历jdk-9-的内部化迁移)
2. [JIT 行为三注解：@Stable / @ForceInline / @IntrinsicCandidate](#2-jit-行为三注解stable--forceinline--intrinsiccandidate)
3. [内存与栈注解：@Contended / @ReservedStackAccess / @Hidden](#3-内存与栈注解contended--reservedstackaccess--hidden)
4. [外部生态与面试视角](#4-外部生态与面试视角)

---

## 1. 包的来历：JDK 9 的内部化迁移

`jdk.internal.vm.annotation` 位于 `java.base` 模块内，但**未导出**（应用代码 import 会编译失败，反射访问需 `--add-exports`）。JDK 9 的模块化改造（JDK-8144223）把散落在 `java.lang.invoke` 中的包私有注解（@ForceInline/@DontInline/@Stable）统一迁入此包，与早已存在的 @Contended 汇合。

设计意图很清晰：这些注解是 **JDK 类库与 HotSpot 之间的内部通信协议**，不是给应用开发者使用的公共 API。协议的特点也因此而来：

1. **Retention 全是 RUNTIME**：HotSpot 在类加载、JIT 编译期读取，必须进字节码；
2. **语义由 VM 解释**：注解本身无处理器类，真正的"消费者"是 C++ 写成的 HotSpot 编译器（C2 JIT）；
3. **可绕过**：@IntrinsicCandidate 在解释执行/低编译层级下就是普通 Java 方法，注解只在特定编译层级生效——所以 JDK 要求被注解方法"字节码实现与 intrinsic 实现语义等价"。

> 🎯 **核心要点**：这组注解揭示了注解的终极形态——消费者不是反射、不是 APT，而是 C++ 虚拟机本体。注解只是"向另一层软件传递意图的声明式协议"。

---

## 2. JIT 行为三注解：@Stable / @ForceInline / @IntrinsicCandidate

### 2.1 @Stable——承诺字段只写一次，换取常量折叠

`@Stable` 的契约是：**被标注字段的所有分量至多变化一次**（通常在构造期或首次使用后冻结）。HotSpot 相信这个承诺后，JIT 可以把字段当作常量执行激进优化——在值初始化完成后，读取直接被折叠为常量传播，后续每次读取零内存访问。

JDK 里的经典案例是 `String.hashCode` 缓存：

```java
private int hash; // 未标 @Stable 但语义相同：0 → 首次 hashCode() 写入 → 永不再变
```

更直接的案例是 JDK 26 的 **JEP 526 Lazy Constants（二次预览）**：`LazyConstant<T>` 惰性初始化字段被标注 `@Stable`，使"惰性初始化"这个本需每次判空的模式在初始化完成后获得与 `static final` 相同的常量折叠待遇——首次访问用 `Supplier` 计算，此后 JIT 直接内联为常量，消除了传统 lazy init 的读取开销。

值得警惕的是契约的脆弱性：@Stable 是**承诺而非强制**。如果字段实际被写两次，JIT 可能基于陈旧常量做错误优化，这是不允许发生的内存模型级破坏。所以此注解仅限 JDK 内部使用，应用代码永远不该碰——`String.hash` 那种"至多写一次"的模式同样要小心：反射可以绕过 `final` 修改字段，一旦发生，所有基于 final 的 JIT 假设全部失效。

### 2.2 @ForceInline / @DontInline——内联决策的优先级插队

正常内联由 C2 基于调用频率、方法大小、调用深度等指标自动决策。`@ForceInline` 让方法**忽略标准内联指标**优先内联（典型如 `Method.invoke()` 的辅助方法，保证反射调用链的 caller 识别优化不被内联边界破坏）；`@DontInline` 则反向声明"此方法不要内联"（用于热点调试、栈帧敏感代码、或防止大方法内联后寄存器压力爆炸）。

理解这对注解的关键认知：**内联是 JIT 所有优化的前提**——只有内联后编译器才能看到跨方法边界的数据流，进而做逃逸分析、消除装箱、常量折叠。@ForceInline 本质是"为关键优化路径强制打开视野"。

### 2.3 @IntrinsicCandidate——方法体的汇编替换

@IntrinsicCandidate（JDK 16 引入）标注的方法是 `vmIntrinsics.hpp` 登记过的**intrinsic 候选**：HotSpot 在特定编译层级会用**手写的平台汇编/编译器 IR** 整体替换方法体。候选名单包括 `System.arraycopy`、`String` 的 indexOf/equals、`Math` 数学函数、`Arrays` 的向量化方法等。

选择 intrinsic 的标准：方法具备"编译器无法从字节码推导的性质"（如 `System.arraycopy` 的内存语义）或"需要编译器 IR 无法表达的硬件指令"（如 `Math.fma` 的融合乘加指令、AES 加密指令）。核心约束：

- intrinsic 实现常省略边界检查，安全性靠**调用方保证**——所以候选方法几乎总是 private，由公开包装方法先完成校验（读共享字段到局部变量、拷贝共享数组），再调用 intrinsic 内核，避免 TOCTOU 竞态；
- 字节码实现与 intrinsic 实现必须语义等价，因为解释器/低层级编译跑的是字节码，高层级才替换；
- HotSpot 类加载时用 `CheckIntrinsics` 标志校验"注解与 VM 登记的一致性"，防止注解漂移。

---

## 3. 内存与栈注解：@Contended / @ReservedStackAccess / @Hidden

### 3.1 @Contended——缓存行隔离与伪共享

CPU 缓存一致性以**缓存行**（通常 64 字节）为最小单位。两个高频独立更新的字段落在同一缓存行时，一个核的写入会使另一个核的整行缓存失效，形成**伪共享（false sharing）**——性能可损失数倍至数十倍。经典受害者是 `long` 计数器数组相邻元素、`ConcurrentHashMap` 的 sizeCtl 与桶数组引用。

`@Contended` 请求 JVM 对被标注字段（或类）做**缓存行填充隔离**，把字段挤到独立缓存行。JDK 内部使用者包括 `ThreadLocalRandom`（多线程随机数种子隔离）、`ConcurrentHashMap`、`Striped64`（LongAdder 基类）等并发基础件。两个使用细节：

1. 默认只在 JDK 内部类生效，应用代码使用需加 `-XX:-RestrictContended` 解锁——这是有意为之的门槛，普通应用应优先用 `LongAdder` 等已封装好隔离的数据结构，而非自己加注解；
2. 填充有内存代价（每字段占用整个缓存行），@Contended 用在大量小对象的字段上会显著膨胀堆占用；
3. 细粒度控制：`@Contended("group")` 支持分组——同组字段隔离到同一条缓存行（互相共享），不同组隔离到不同行；填充宽度由 `-XX:ContendedPaddingWidth` 控制（默认 128 字节，覆盖部分 CPU 的预取行宽）。

### 3.2 @ReservedStackAccess 与 @Hidden

`@ReservedStackAccess` 标注的方法在 JVM 保留的**预留栈区**执行：栈溢出（StackOverflowError）时，普通代码已无栈可用，但预留区还能让关键清理代码（如 `LockSupport.park` 相关、ThreadLocal 清理）安全完成收尾，避免 JVM 在栈耗尽状态下的二次崩溃。JDK 中的使用点高度集中：`jdk.internal.misc.Unsafe` 的 park/unpark 路径与 `ReentrantLock` 的释放路径——这些是"栈溢出后线程必须还能收尾"的临界代码，注解把它们与用户栈隔离开。

`@Hidden` 标注的帧在异常栈轨迹、调试器中**隐藏**——用于 `LambdaForm` 等 JVM 生成的实现帧，避免调用链噪音淹没真实业务帧。这解释了为什么 Lambda 抛异常时栈里看不到 `LambdaForm$...` 帧。

---

## 4. 外部生态与面试视角

外部生态对这套机制的借用是理解其价值的捷径：

- **GraalVM** 有对应的 `org.graalvm.compiler.core.common` 注解体系（@CompilationFinal 类似 @Stable 语义），AOT 下常量折叠更激进；
- **OpenJ9** 等备选 JVM 不识别这些注解，属 HotSpot 私有协议——再次印证"注解消费者决定语义"；
- 面试视角：问 `LongAdder` 为什么快、问伪共享是什么、问 `String.hashCode` 为何只算一次，答案都指向这组注解背后的机制。能说出"@Contended 隔离缓存行、@Stable 常量折叠、@IntrinsicCandidate 汇编替换"三句话，就证明了 JVM 底层知识不是背概念而是懂机制。

三道速答题：「LongAdder 为什么比 AtomicLong 快？」——Cell 数组用 @Contended 隔离各线程的计数器字段，消除伪共享互踩（结合无锁 CAS 分段）；「String.hashCode 为什么只算一次？」——hash 字段"至多写一次"符合 @Stable 语义，JIT 可常量折叠后续读取；「System.arraycopy 为什么比循环快？」——@IntrinsicCandidate 让 HotSpot 用平台优化的内存拷贝指令整体替换方法体。答这三题时把注解与机制对上号，就是与背概念候选人的分界线。

> ⚠️ **边界提醒**：本模块内容属 HotSpot 内部实现协议，注解语义随 JDK 版本可能调整（如 @IntrinsicCandidate 的校验行为、@Contended 的填充策略）。生产代码**禁止**通过 `--add-exports` 使用它们，学习目的只需理解机制。

---

**下一模块**：[03 Lombok 注解剖析](./03-Lombok注解剖析.md) · **返回总览**：[00 总览](./00-Java常用注解知识体系总览.md)

---

【参考来源】
- [OpenJDK 24: jdk.internal.vm.annotation JavaDoc（@Stable/@ForceInline/@DontInline）](https://apidia.net/java/OpenJDK/24/jdk.internal.vm.annotation.html)
- [JDK-8144223: Move j.l.invoke.{ForceInline, DontInline, Stable} to jdk.internal.vm.annotation（openjdk-hotspot-compiler-dev 讨论）](https://marc.info/?l=openjdk-hotspot-compiler-dev&m=144890532712845&w=3)
- [JEP 526: Lazy Constants 二次预览与 @Stable 的关系（Inside Java Newscast #106）](https://inside.java/2026/02/05/newscast-106/)
- [Inside Java 播客：LazyConstants in JDK 26](https://inside.java/2026/03/06/podcast-049/)
- [OpenJDK 源码: IntrinsicCandidate.java（annotation 语义约束）](https://raw.githubusercontent.com/liachmodded/jdk/317dd27a6abb8e34af40299bb2e2b72288a39b2d/src/java.base/share/classes/jdk/internal/vm/annotation/IntrinsicCandidate.java)
