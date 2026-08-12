# 函数式与Stream范式结合

> Stream 是 Java 函数式范式的最大落点：map-filter-reduce 三件套让"数据怎么流动"取代"循环怎么写"。JDK 24 定稿的 Stream Gatherers（JEP 485）补齐了最后一块短板——自定义中间操作——管线范式自此完整。本章从范式视角讲结合，API 细节交叉引用「Java流Stream」体系

---

## 📚 目录

1. [map-filter-reduce 心智](#1-map-filter-reduce-心智)
2. [管线 = 声明式查询](#2-管线--声明式查询)
3. [Stream Gatherers：自定义中间操作](#3-stream-gatherers自定义中间操作)
4. [collect vs reduce：两种收束](#4-collect-vs-reduce两种收束)
5. [副作用混入流的三大陷阱](#5-副作用混入流的三大陷阱)
6. [惰性管线与短路的配合](#6-惰性管线与短路的配合)
7. [核心要点与思考题](#7-核心要点与思考题)

---

## 1. map-filter-reduce 心智

函数式处理数据的通用范式是**变换-过滤-归约**三件套，Stream API 是其直接体现：

- `map`：逐元素变换（1 → 1），变换函数必须是纯函数
- `filter`：按谓词留取（n → m），谓词必须是纯判断
- `reduce`/`collect`：多元素收束为单值或容器（n → 1）

```java
// 需求：找出金额大于 100 的订单，按会员等级加成，求和
BigDecimal total = orders.stream()
        .filter(o -> o.amount().signum() > 0)          // filter：纯判断
        .map(o -> o.amount().multiply(memberRate(o)))  // map：纯变换
        .reduce(BigDecimal.ZERO, BigDecimal::add);     // reduce：纯归约
```

心智要点：**每一步都是"输入一批、输出一批"的纯函数**，批与批之间只通过值传递。看懂一条管线 = 从左到右读三遍"过滤了什么、变换成什么、收束成什么"，不需要跟踪任何循环变量——这正是与 for 循环的本质差异。

## 2. 管线 = 声明式查询

Stream 管线读起来像 SQL 的"查询计划"：`FROM orders WHERE amount > 0 GROUP BY member` 对应 `stream().filter().collect(groupingBy())`。这份声明式气质带来三个工程收益：

1. **意图自解释**：管线一步一个动词（filter/map/sort/limit），review 不需要"逐行推导"
2. **可组合可复用**：中间操作是函数值，可提取为 `Function<Stream<Order>, Stream<Order>>` 复用（如"已支付订单"过滤器）
3. **优化窗口**：惰性求值 + 短路让运行时有机会合并/跳过步骤（findFirst 不会处理全量流）

声明式不是万能：需要"相邻元素互操作"（滑动窗口、去重相邻值、前序依赖）时，标准中间操作无能为力——这是 2014-2024 十年间 Stream 的最大空白，直到 Gatherers 补上。

同时明确声明式的**适用边界**，避免教条化：

- 需要索引（`i` 位置参与计算）、需要"前一步结果影响后一步"的强序列逻辑——for 循环直白得多，管线只会绕
- 调试敏感或性能热点：管线拆段断点、中间对象分配都是有成本的，热点代码先写清楚再考虑换命令式
- 判断口诀：**"对一批数据做同一种变换"用管线，"跟踪一个状态逐步演变"用循环**——前者是集合处理，后者是状态机，工具不同不必硬融

## 3. Stream Gatherers：自定义中间操作

**Stream Gatherers（JEP 485，JDK 24 定稿）**：`Stream.gather(Gatherer)` 是中间操作的扩展点——正如 `collect(Collector)` 是终止操作的扩展点。它支持状态ful 转换、多对多映射、滑动窗口、批量处理，且可与标准操作无缝衔接。

JDK 自带五个内置 gatherer（`java.util.stream.Gatherers`）：

| 内置 | 语义 | 示例结果（输入 1..5） |
|------|------|----------------------|
| `windowFixed(n)` | 固定大小非重叠分窗 | `[1,2],[3,4],[5]` |
| `windowSliding(n)` | 固定大小滑动窗口 | `[1,2],[2,3],[3,4],[4,5]` |
| `scan(seed, fn)` | 前缀累积（running aggregate） | `1,3,6,10,15` |
| `fold(seed, fn)` | 归约发射单值（reduce 的 gather 版） | `15` |
| `mapConcurrent(n, fn)` | 有界并发映射，保持遇序 | 每个元素并发处理，顺序不变 |

```java
// 滑动窗口：连续三次温度超过阈值则告警
List<List<Temp>> windows = readings.stream()
        .gather(Gatherers.windowSliding(3))
        .filter(w -> w.stream().allMatch(Temp::isHigh))
        .toList();

// 有界并发映射：I/O 型任务并行但限并发度（n 可控，优于无界 parallel）
List<UploadResult> results = files.stream()
        .gather(Gatherers.mapConcurrent(8, storage::upload))
        .toList();
```

自定义 Gatherer 由四部件组成（只需 integrator 必写）：`initializer` 建私有状态、`integrator` 逐元素处理并决定短路、可选 `combiner` 合并并行分片状态、可选 `finisher` 收尾发射残留状态。内置 gatherer 不够用时用 `Gatherer.of(...)`/`Gatherer.ofSequential(...)` 自建，多级可 `andThen` 融合成单一可复用 gatherer。一个典型自建示例——**相邻去重**（连续重复只留一个，标准 API 无法一行表达）：

```java
// JEP 485 API（JDK 24+）；ofSequential 声明顺序相关、禁并行
static <T> Gatherer<T, ?, T> dedupAdjacent() {
    return Gatherer.ofSequential(
            ArrayList<T>::new,                          // initializer：私有状态
            (prev, el, downstream) -> {                 // integrator：与上一个比较
                if (prev.isEmpty() || !prev.get(0).equals(el)) downstream.push(el);
                prev.set(0, el);
                return true;                            // 继续处理
            });
}
// 用法：a,a,b,b,b,c,a → a,b,c,a
List<String> deduped = list.stream().gather(dedupAdjacent()).toList();
```

**可复用管线**的范式配套：把"中间操作集合"提取为函数值，管线成为可命名资产——`Function<Stream<Order>, Stream<Order>> paidOnly = s -> s.filter(o -> o.status() == PAID);` 即可在多个查询中复用同一段筛选语义，配合静态导入形成团队共享的"管线方言"。

> ⚠️ **兼容性**：Gatherers 要求 JDK 24+，生产环境仍跑 JDK 17/21 的团队不能直接使用；2026 年随着 JDK 25 LTS 普及，它已进入 LTS 基线（JDK 25 正式收录），可以放心规划迁移。

## 4. collect vs reduce：两种收束

归约阶段两个入口经常被混淆，选型依据是"结果形态"：

- `reduce(identity, accumulator)`：收束为**单值**——`sum`、`max`、拼接字符串。要求结合律（parallel 的前提），identity 必须是真正单位元（`add` 的 0、`multiply` 的 1）
- `collect(Collector)`：收束为**容器**——`toList`、`groupingBy`、`toMap`。可变容器装箱由 Collector 内部完成，外部仍是纯函数

```java
// reduce：单值
int total = IntStream.rangeClosed(1, 100).reduce(0, Integer::sum);

// collect：容器
Map<Status, List<Order>> byStatus = orders.stream()
        .collect(Collectors.groupingBy(Order::status));
```

> ⚠️ **经典陷阱**：`reduce(0, Integer::sum)` 在并行流里要求加法结合律成立（成立），但 `reduce(new StringBuilder(), (a,b)->a.append(b))` 这类"可变累积"不成立——并行分片后拼接顺序无定义。可变容器归约一律走 `collect`。

## 5. 副作用混入流的三大陷阱

函数式管线的敌人是"看起来无害的副作用"，三大高频坑：

1. **`peek` 打日志**：`peek` 本意是调试工具（javadoc 明言 "mainly for debugging"），在并行流中执行次数与顺序不确定，线上依赖 `peek` 计数/记录必出错
2. **`forEach` 外部写**：`list.stream().forEach(System.out::println)` 在并行下顺序无序；把"收集到共享集合"写进 lambda（`list.forEach(l -> result.add(compute(l)))`）是数据竞态——应改为 `map + collect`
3. **有状态 lambda**：lambda 里用可变外部变量（计数器、`SimpleDateFormat`）——并行分片共享状态直接产生错误结果，且只在压力下浮现

```java
// ❌ 并行下竞态：共享 result 集合
List<Integer> result = new ArrayList<>();
list.parallelStream().forEach(x -> result.add(x * 2));

// ✅ 纯函数管线：map 变换 + collect 收束
List<Integer> result = list.parallelStream().map(x -> x * 2).toList();
```

判据一句话：**中间操作只做值变换，终止操作之前不碰任何外部状态**。补充一个自查动作：review 时对每个 lambda 问"删掉这行副作用（日志、写外部集合），结果变不变"——变，说明副作用混入了变换；不变，说明这行副作用是无害但多余的，同样该删。两条判据合起来，管线的纯度检查就有了机械化的抓手。

## 6. 惰性管线与短路的配合

Stream 的中间操作是惰性的：只有终止操作触发时才开始处理，且逐元素"边算边短路"。与范式结合的两个要点：

- **短路让"昂贵纯函数"变便宜**：`filter(expensive).findFirst()` 只对前几个元素求值——纯函数惰性组合后，"不必要的计算"被自动跳过，无需手动优化
- **无限流成为可能**：`Stream.iterate`/`generate` 配 `limit`/`takeWhile` 表达"无穷序列的前缀"——惰性 + 短路让它不会真的算无穷（08 章详述）

```java
// 惰性 + 短路：最多取 5 个质数，不生成多余元素
Stream.iterate(2, n -> n + 1)
        .filter(FunctionalChapter::isPrime)   // 纯函数
        .limit(5)
        .toList();
```

要同时记住惰性的**物化例外**：`distinct()` 与 `sorted()` 是全量物化操作（内部建 Set/List 缓存整条流），放在无限流上直接 OOM；`flatMap` 对无限流的短路也不友好（内部拉取策略可能拉爆），无限流的拍平应确保输入有限。判断口诀：**凡是要"看到全部才能做"的操作（排序、去重、统计），都要求输入有限**——无限流只配逐元素操作（map/filter/limit/takeWhile）。这类"惰性内部有例外"的知识，正是把 Stream 从"会写"推进到"会用"的分水岭。

> 🎯 **核心要点**：Stream 让函数式范式落地的完整形态 = 纯函数三件套（map/filter/reduce）+ 声明式管线 + Gatherers 扩展中间操作 + 惰性短路。副作用与有状态 lambda 是唯一禁区；JDK 24 后范式拼图完整，学习重心应从"会用 API"转向"设计纯管线"。

## 7. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. map-filter-reduce 是函数式数据处理的心智模型，管线即声明式查询，惰性 + 短路是免费性能；
> 2. Gatherers（JEP 485，JDK 24 定稿、25 LTS 收录）补上自定义中间操作，windowSliding/mapConcurrent 等五个内置最常用；
> 3. 三陷阱（peek 日志、forEach 外部写、有状态 lambda）只在并行时爆雷，管线内禁碰外部状态。

**思考题**：

1. `windowSliding(3)` 和 `windowFixed(3)` 输出差在哪？（→ 3 节）
2. 为什么"可变累积"不能放 reduce？并行下会发生什么？（→ 4 节）
3. `peek(System.out::println)` 在并行流里会被调用几次？（→ 5 节）
4. `mapConcurrent(8, fn)` 和 `parallelStream().map(fn)` 的本质区别？（→ 3 节）

---

**下一模块**：[08-递归与惰性求值](08-递归与惰性求值.md)｜**返回总览**：[00-函数式编程知识体系总览](00-函数式编程知识体系总览.md)

---

## 参考来源

- [JEP 485: Stream Gatherers（OpenJDK）](https://openjdk.org/jeps/485)——定稿于 JDK 24
- [Stream Gatherers（Oracle JDK 26 文档）](https://docs.oracle.com/en/java/javase/26/core/stream-gatherers.html)——五个内置 gatherer 与自定义指南
- [Stream Gatherers: Custom Intermediate Ops（2026-03）](https://www.spaghetticodejungle.com/blog/2026/march/java-24-stream-gatherers/java-24-stream-gatherers)——实践解读
- [Java 25（Java Almanac）](https://www.javaalmanac.io/jdk/25/)——Gatherers 进入 LTS 基线确认
