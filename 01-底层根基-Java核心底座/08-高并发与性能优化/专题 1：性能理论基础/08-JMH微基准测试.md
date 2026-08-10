# 08 JMH 微基准测试：Java 性能测量的科学

> JMH 是 OpenJDK 官方的微基准框架，但默认配置下结果基本不可信——JVM 动态优化的陷阱会让"什么都没测"的代码跑得飞快。

---

## 📚 目录

1. [1. 为什么微基准这么难](#1-为什么微基准这么难)
1. [2. 六大陷阱与解法](#2-六大陷阱与解法)
1. [3. 标准配置模板](#3-标准配置模板)
1. [4. Blackhole 的底层机制](#4-blackhole-的底层机制)
1. [5. @State 的三种作用域](#5-state-的三种作用域)
1. [6. 适用场景与结果解读](#6-适用场景与结果解读)
1. [7. CI 回归与 2026 前沿](#7-ci-回归与-2026-前沿)

---

## 1. 为什么微基准这么难

Java 代码先解释执行，热点代码经 C1（Level 1-3）再到 C2（Level 4）激进优化。同一个方法，在真实应用中会被内联、逃逸分析、常量传播改造，在孤立基准中也可能被完全消除。**默认配置下测出的数字，测的是 JVM 的优化能力，不是你的代码能力。** JMH 的价值就是对抗这些优化，让测量反映真实执行。同样的道理要求基准结论只在**相同环境上下文**成立：换 JDK 版本、换 JVM 参数后结论必须重测——跨版本引用的基准数据是无效证据。

## 2. 六大陷阱与解法

| 陷阱 | 现象 | 解法 |
|---|---|---|
| 死代码消除（DCE） | 结果快得不合理，负载被整体删除 | `Blackhole.consume()` 或直接返回结果 |
| 常量折叠 | 基于常量计算，编译期直接算出结果 | `@State` 参数化输入，运行时产生数据 |
| 预热不足 | 测的是启动速度不是巡航速度 | `@Warmup` 显式预热至 C2 编译完成 |
| 对象逃逸/GC 干扰 | 测量时间包含分配+回收 | `@State(Scope.Benchmark)` 预分配，避免装箱 |
| IO 污染 | println/日志/文件读写混入计时 | 基准方法内禁止一切 IO |
| 环境噪音 | 调度、Turbo Boost、GC 随机发生 | `@Fork` 多进程隔离，误差 >±5% 判环境不稳 |

DCE 最阴险：失败模式看起来像成功——有数字、可复现、快得不合理。验证手段只有一个：`-prof perfasm:hotThreshold=0.05` 读生成汇编，若热点区只剩循环计数器，说明负载已被优化掉。

## 3. 标准配置模板

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(3)                                  // 3 次独立 JVM 进程
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Thread)
public class Bench {

    @Param({"100", "1000"})               // 多组输入规模
    int size;

    @Benchmark
    public int sum(Blackhole blackhole) { // 结果必须被消费
        int s = 0;
        for (int i = 0; i < size; i++) s += i;
        return s;                         // 返回即消费
    }
}
```

生产基线推荐：Warmup 10×2s、Measurement 20×2s、Fork 5（约 5 分钟/项）；CI 回归可降到 5×1s / 10×1s / Fork 2-3。必须 `mvn clean package && java -jar target/benchmarks.jar` 运行，**不要在 IDE 里跑 main**——IDE 的 JVM 参数与分层编译状态不可控。

## 4. Blackhole 的底层机制

纯 Java 实现的 `Blackhole.consume` 每次约消耗 25 条指令，对纳秒级操作会主导测量。JDK-8259316 引入 **COMPILER 模式**：C2 把 consume 作为 intrinsic 处理，插入 opaque IR 节点阻止逃逸分析，但最终不发射指令——开销降到约 6 条指令/操作。JDK 21+ 可通过 `-Djmh.blackhole.mode=COMPILER` 或注解方式启用；极小操作（<10ns）优先选 COMPILER 模式，否则用纯 Java 模式也要计入开销。判断该不该用：被测操作自身耗时 >100ns 时，两种模式差异可忽略；只有纳秒级操作才值得纠结。

## 5. @State 的三种作用域

- `Scope.Benchmark`：所有线程共享一个实例——适合测共享状态（缓存、池）。
- `Scope.Thread`：每线程独立实例——适合测无共享的纯计算，避免伪共享干扰。
- `Scope.Group`：组内共享——配合 `@Group` 注解定义读/写线程比例（如 4 读 1 写），测并发数据结构（ConcurrentHashMap 等）比单线程基准真实得多；无 @Group 时基准默认为单线程执行，并发场景必须显式声明。

注意**伪共享（false sharing）**：多线程基准中不同线程写同一缓存行，性能可能骤降 10 倍，与被测逻辑无关。`-prof perfnorm` 与 `-prof perfc2c` 可检测缓存行争抢。

**输入构造与 @Setup**：基准输入必须运行时产生——`@Setup` 中预构造数据（解析好的字符串、填充好的数组），避免在 @Benchmark 内 new 大对象（污染计时），也避免用字面量（触发常量折叠）：

```java
@State(Scope.Benchmark)
public class JsonBench {
    String json;
    @Setup public void setup() {
        json = "{\"name\":\"alice\",\"age\":30,\"tags\":[1,2,3]}";
    }
    @Benchmark @Threads(4)
    public Object parse(Blackhole bh) {
        return PARSER.parse(json);
    }
}
```

**Mode 选型**：Throughput（单位时间执行次数，算法/IO 类默认，最稳定）；AverageTime（单次平均耗时，适合大开销操作）；SingleShotTime（单次执行，适合启动初始化类）；SamplingTime 已不推荐。**prof 插件**：`-prof gc` 看分配率与 GC 次数、`-prof stack` 看采样热点、`-prof perfasm` 看汇编、`-prof async` 出火焰图——结果解读靠 prof 插件，比只看数字强得多。

**@Param 多组输入**：`@Param({"100", "10000", "1000000"})` 自动生成多组数据规模，适合"小数据快 vs 大数据优"这类选型题（如不同容量下 HashMap 与 TreeMap 的优劣翻转）；注意 @Param 之间是**全组合**，参数多时组数爆炸，控制在 2 个维度以内。**基准方法规则**：必须 public、非 static、被 @Benchmark 标注，且不能手动调用——手动调用会绕过框架的 fork 与预热流程，得到"启动速度"而非"巡航速度"。

## 6. 适用场景与结果解读

微基准适合回答**选择题**：数据结构选型（ArrayList vs LinkedList、HashMap vs TreeMap）、序列化库对比（Jackson vs Protobuf）、锁实现对比（synchronized vs ReentrantLock vs 无锁）、字符串处理方案、正则 vs 手写解析。不适合回答"系统能扛多少 QPS"——那是全链路压测的职责（04 篇）；两者的分工边界：**微基准管"这段代码怎么实现最快"，压测管"整个系统在真实流量下怎么样"**，微基准的结论必须经压测验证后才可信。

结果解读三条：两组对比必须看**误差带**——A 快 10% 但误差 ±8%，等于没区别，差异要大于误差之和才算数；同一基准多次运行的离散度大（>±5%）说明环境不稳，先修环境再谈结论；微基准结论是**相对值**（A 比 B 快 30%），绝对值以线上 P99 观测为准——孤立环境测出的 10ms 在真实流量下可能是 50ms。

## 7. CI 回归与 2026 前沿

- 输出 JSON（`-rff result.json`），CI 中对比基线：性能下降超阈值（如吞吐 <95%）阻断合并；**防止"每版慢一点"的温水煮青蛙**。
- 基线文件（benchmark 源码 + 结果 JSON）进版本库，与代码一起评审——改基准不算改代码，但改了基准等于改了标尺，必须走评审；阈值设置参考误差带：阈值取基线误差的 2-3 倍，否则正常抖动天天误报，CI 门禁被跳过就形同虚设。
- JDK 版本必须固定并与生产一致：OpenJDK 17 vs 21 的"快 15%"可能只是编译器开关差异。
- 2026 年学术研究提醒：即使完全遵循指南，微基准仍可能失真——孤立环境的 JIT profile（分支概率、接收者类型）与真实应用不同，会触发不真实的内联等激进优化。对策是：微基准用于**相对比较**（A vs B、前后版本），**绝对数字以线上观测为准**；结论必须与压测、线上指标交叉验证。

> 🎯 **核心要点**：JMH 是"对抗 JIT 优化的测量实验"，六大陷阱逐一防范；结果是相对可信的（对比用），不是绝对真理（绝对值看线上）；读汇编是验证 DCE 的唯一诚实手段。

---

**下一模块**：[09-分层优化策略](09-分层优化策略.md) / **返回总览**：[00-性能理论基础总览](00-性能理论基础总览.md)
