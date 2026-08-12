# 运行时与执行模型：JVM vs CPython

> 性能差距不是"语言写的"，而是**运行时设计**决定的：JVM 把字节码编译到机器码再热点优化，CPython 逐行解释字节码；JVM 有世界级分代 GC，CPython 用引用计数兜底。理解两个运行时的骨架，就理解了 90% 的性能对比结论

---

## 📚 目录

1. [执行链对比：javac→JIT vs pyc→解释器](#1-执行链对比javacjit-vs-pyc解释器)
2. [垃圾回收：分代 GC vs 引用计数+分代](#2-垃圾回收分代-gc-vs-引用计数分代)
3. [类型特化与动态分派](#3-类型特化与动态分派)
4. [启动时间与 GraalVM Native Image](#4-启动时间与-graalvm-native-image)
5. [CPython 的实验性 JIT 进展](#5-cpython-的实验性-jit-进展)
6. [核心要点与思考题](#6-核心要点与思考题)

---

## 1. 执行链对比：javac→JIT vs pyc→解释器

两条执行链各自有三段，只有第一段不同，后两段都在运行时：

```text
Java:  源码 → javac 编译 → 字节码(.class) → JVM 解释 + JIT 热点编译为机器码
Python:源码 → 无显式编译（py_compile 生成 .pyc）→ 字节码 → CPython 逐条解释执行
```

| 环节 | Java | Python |
|------|------|--------|
| 编译时机 | 开发期 javac（类型检查在此完成） | 无编译期（语法检查 + 生成 .pyc） |
| 执行单位 | JVM 解释字节码；热点方法 JIT 编译 | 解释器循环逐条执行字节码（eval loop） |
| 机器码 | 运行时由 C2/Graal JIT 生成 | 解释器本身是 C 程序，Python 代码不生成机器码 |
| 优化窗口 | 方法执行万次后 C2 全强度优化 | 3.13 起实验性 JIT 仍在早期 |

关键推论：**Java 的性能是"热起来"的性能**——服务跑几分钟后 C2 完成全部优化，吞吐达到峰值；**Python 的性能是"稳定"的性能**——从第一行到最后一行业务代码全是解释执行，没有预热概念。这也解释了监控里 Java 服务的"启动爬坡曲线"（warm-up）与 Python 服务的"平直曲线"。

## 2. 垃圾回收：分代 GC vs 引用计数+分代

内存管理是运行时第二大差异，直接塑造了两边的编程习惯：

- **JVM**：完全自动的分代追踪式 GC（G1/ZGC/Shenandoah）。对象存活周期分代管理，GC 线程与应用线程并行（ZGC 停顿毫秒级）；没有"即时释放"语义，程序员不关心对象的死亡时机——代价是停顿风险与 GC 调优（堆大小、收集器选择）
- **CPython**：**引用计数为主 + 分代 GC 兜底**。对象引用数归零立刻释放（`__del__` 即时、内存即还）；但循环引用计数不了，用分代 GC 周期扫描回收。代价是"计数操作"本身有开销（每个引用增减都有 CPU 成本），且 `__del__` 时机不可靠

```python
# Python：引用计数即刻回收——大对象用完即还，无需调堆
def process():
    data = load_large()      # 离开作用域引用数归零 → 立即释放
```

工程含义对比：Java 需要"GC 调优"这门学问（内存泄漏 = 对象被意外引用），Python 主要防"循环引用 + `__del__`"与"全局缓存不释放"两类问题；Java 的堆是统一大池子，Python 的对象零散分配、受 `sys.gettotalrefcount`（调试构建）等观测手段制约。2026 年两者的 GC 都在进步：Java 的 ZGC 已支撑 TB 级堆，Python 3.14 优化了分配器与垃圾回收的暂停。

**基础占用与泄漏画像**也不同：一个空 Java 应用（JVM + Spring）常占 200-500MB 基线内存，Docker 部署要预留；Python 应用基线几十 MB，但每个对象自带引用计数与类型信息，**大量小对象的密度比 Java 更差**（字符串、dict 都很"重"）。泄漏的表现也各异：Java 是"老年代缓慢增长"（heap dump 定位引用链），Python 是"常驻对象集合膨胀"（tracemalloc 定位分配点）。运维团队排内存故障的技能树因此完全不同——Java 工程师熟练 MAT/JFR，Python 工程师熟练 tracemalloc/objgraph，这是跨语言协作时最容易被低估的认知差。

## 3. 类型特化与动态分派

性能差距的根源之一在**方法调用成本**：

- JVM 的 JIT 看到 `List<String>` 与 `Integer::sum` 后，可以内联、去虚拟化（devirtualize）、甚至把循环改成 SIMD 向量化——因为类型是静态已知的
- CPython 每次执行 `a + b` 都要走"类型分派"：查 `a` 的 `__add__`、检查 `b` 是否兼容、处理可能的协议重载——同一行代码每次执行都付同样的动态分派税

```python
# Python：看似简单的一行，每次执行都要做完整的分派查找
total = sum(x * 2 for x in data)

# Java：JIT 可以把它优化成一次内联循环
int total = data.stream().mapToInt(x -> x * 2).sum();
```

CPython 的缓解手段是**特化字节码**（3.11+ 的"自适应解释器"）：把"常见路径"（如 `int + int`）编译为特化指令，避免每次完整分派——3.11 比 3.10 快约 60% 主要靠这个；3.14 进一步扩大特化覆盖面。但解释器的特化与 JIT 的深度优化（逃逸分析、循环优化、向量化）仍有量级差距，这是"10-100 倍"结论的底层原因。

## 4. 启动时间与 GraalVM Native Image

性能的另一面是**启动与冷启动**：

| 指标 | Java（默认） | Java（Native Image） | Python |
|------|-------------|---------------------|--------|
| 启动时间 | 数百毫秒-数秒（类加载 + JIT 预热） | 毫秒级（AOT 编译为本机代码） | 几十-几百毫秒（解释器加载即可跑） |
| 峰值性能 | 最高（C2 全优化） | 略低于 JIT（无运行时优化） | 最低（解释执行） |
| 内存占用 | 高（JVM 本身数百 MB） | 低（6.5MB 级 demo） | 中 |

GraalVM Native Image（AOT 编译）是 Java 对抗"重启动"的方案：把应用编译为本机可执行文件，启动降到毫秒级、内存大减——Serverless/函数计算场景（冷启动计费）2026 年大量采用。但 Native Image 放弃运行时动态特性（反射需配置、无 JIT 预热），适合"已知代码路径"的服务。Python 侧没有同类方案（解释器必须存在），冷启动主要靠优化导入与精简依赖。

## 5. CPython 的实验性 JIT 进展

Python 性能之路的 2026 状态，按里程碑排开：

- **3.11（2022）**：自适应解释器 + 特化字节码，速度提升约 60%——"免费的性能"
- **3.13（2024-10）**：实验性 JIT 编译器（`--enable-experimental-jit`），免费线程构建（PEP 703）实验——两个"实验"同时开始
- **3.14（2025-10）**：free-threaded 官方可选支持（PEP 779，2025-06-16 通过），性能损耗从 3.13 的 20-40% 降到 0-10%；JIT 继续打磨
- **3.15（2026-10 计划）**：统一 ABI（PEP 803 `abi3t`）——一个扩展二进制同时跑在有/无 GIL 两种构建上，生态兼容的大前提

要校准预期：**CPython 的 JIT 目标是"把解释器性能提高一个档次"，不是与 JVM/JIT 竞争**——它主要靠消除解释开销，而 JVM 的 C2 做的是全局优化（内联、逃逸、向量化）。加上 CPython 生态的性能真相：**重度计算场景的惯例是用 C 扩展（NumPy/PyTorch）**，解释器性能对这类场景根本不重要——这决定了 Python 性能问题的答案通常是"换工具"而非"等优化"。

工程师视角的现实结论：**"Python 慢"有三个速度档位**——纯 Python 逻辑慢（解释税，10-100 倍差距）、调 C 扩展的胶水代码中速（与 Java 调库接近）、被 C 内核整体接管的重计算快（与 Java 调库同档）。所以描述性能时永远要问一句"慢在哪一段"：是慢在算法、慢在解释循环、还是慢在 IO——三段各有各的解法，混为一谈就会得出"Python 不能做 X"的错误结论。

> 🎯 **核心要点**：JVM = 编译期契约 + 运行时深度优化（预热后爆发）；CPython = 解释执行 + C 扩展外包重活。性能差距是第一性原理的（解释 vs 编译），不是"努力不够"；Java 的代价是启动与内存，Python 的代价是 CPU 密集场景的解释税——两边都在修各自的短板（Java 的 Native Image、Python 的 JIT），但物理边界清楚。

## 6. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. Java 先编译后 JIT（热起来才快），Python 全程解释（第一行就慢，且一直慢）；
> 2. GC 策略：JVM 分代追踪式（停顿可调），CPython 引用计数+分代（即时但有计数税）；
> 3. Python 性能路线是特化字节码 + 实验 JIT + C 扩展外包，3.15 统一 ABI 是 2026 关键里程碑。

**思考题**：

1. Java 服务监控里的"预热爬坡"是什么？Python 服务为什么没有？（→ 1 节）
2. 引用计数和追踪式 GC 各有什么致命弱点？（→ 2 节）
3. 为什么同一段循环 Java 可以向量化、Python 不行？（→ 3 节）
4. Native Image 适合什么场景？Serverless 为什么热衷？（→ 4 节）

---

**下一模块**：[03-性能对比与基准解读](03-性能对比与基准解读.md)｜**返回总览**：[00-Java与Python对比与场景剖析知识体系总览](00-Java与Python对比与场景剖析知识体系总览.md)

---

## 参考来源

- [Python vs Java: Enterprise Comparison（Lang Pop）](https://langpop.com/blog/python-vs-java-enterprise)——执行模型对照
- [PEP 779: Free-threaded Build Officially Supported（Python）](https://peps.python.org/pep-0779/)——2025-06-16 通过
- [PEP 803: abi3t — Stable ABI for Free-Threaded Builds（Python）](https://peps.python.org/pep-0803/)——3.15 统一 ABI
- [GraalVM CE 25.1.3 Native Image 6.5MB（Phoronix）](https://www.phoronix.com/news/GraalVM-Community-25.1.3)——AOT 启动优化实证
