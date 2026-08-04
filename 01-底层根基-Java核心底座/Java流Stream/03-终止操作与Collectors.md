# 03 - 终止操作与 Collectors

> **核心摘要**：终止操作触发 Stream 执行——收集（toList/joining）、聚合（count/sum/reduce）、匹配（anyMatch/findFirst）。Collectors 是收集的核心工具：分组、统计、映射、teeing。本文覆盖终止操作全集与 Collectors 深度。

> **前置阅读**：[[02-创建流与中间操作]]

---

## 📚 目录

1. [终止操作分类](#1-终止操作分类)
2. [收集：toList 家族](#2-收集tolist-家族)
3. [聚合：count/sum/reduce](#3-聚合countsumreduce)
4. [匹配与查找](#4-匹配与查找)
5. [Collectors 核心](#5-collectors-核心)
6. [分组与分区](#6-分组与分区)
7. [统计与连接](#7-统计与连接)
8. [高级 Collectors：teeing/collectingAndThen](#8-高级-collectorsteeingcollectingandthen)
9. [Collector 内部机制](#9-collector-内部机制)
10. [核心要点](#10-核心要点)

---

## 1. 终止操作分类

> **背景**：终止操作是 Stream 的「发动机」——触发惰性管道执行并产生结果。
> **目的**：掌握终止操作全集与分类。
> **适用范围**：所有 Stream 处理的终点。

| 分类 | 操作 | 返回 | 说明 |
|------|------|:---:|------|
| **收集** | collect/toList/toSet/toMap | 集合/Map | 物化结果 |
| **聚合** | count/sum/min/max/average | 数值 | 汇总 |
| **归约** | reduce | 单值 | 通用归约 |
| **匹配** | anyMatch/allMatch/noneMatch | boolean | 短路 |
| **查找** | findFirst/findAny | Optional | 短路 |
| **遍历** | forEach/forEachOrdered | void | 副作用 |

> 🎯 **短路终止操作**（性能关键）：anyMatch/allMatch/findFirst/findAny——结果确定即停止（不全量）。

---

## 2. 收集：toList 家族

```java
// ① toList（Java 16+ 推荐——不可变！）
List<String> result = list.stream().filter(...).toList();
// ⚠️ toList() 返回不可变 List（修改抛异常）

// ② collect(toList())（Java 8——可变）
List<String> result = list.stream()
    .collect(Collectors.toList());
// ⚠️ 可变（可 add）——但具体类型未保证（通常 ArrayList）

// ③ 指定实现（toCollection）
List<String> result = list.stream()
    .collect(Collectors.toCollection(ArrayList::new));
Set<String> set = list.stream()
    .collect(Collectors.toCollection(LinkedHashSet::new));  // 保序

// ④ toSet
Set<String> unique = list.stream().collect(Collectors.toSet());

// ⑤ toMap（⚠️ 必须处理键冲突！）
// 两参数：键冲突抛 IllegalStateException！
Map<String, User> byId = users.stream()
    .collect(Collectors.toMap(User::getId, u -> u));
// 三参数（推荐）：提供合并函数
Map<String, User> byId = users.stream()
    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
// 四参数：保序（LinkedHashMap）
Map<String, User> byId = users.stream()
    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, LinkedHashMap::new));
```

> ⚠️ **toList vs collect(toList())**：Java 16+ 推荐 `toList()`（不可变、简洁）；需要可变集合用 `collect(toCollection(...))`。

---

## 3. 聚合：count/sum/reduce

```java
// ① count
long count = list.stream().filter(...).count();

// ② min/max（返回 Optional）
Optional<Integer> min = list.stream().min(Integer::compareTo);
Optional<Order> max = orders.stream()
    .max(Comparator.comparing(Order::getAmount));

// ③ 原始类型流聚合（免装箱——性能）
int sum = ints.stream().mapToInt(x -> x).sum();
OptionalDouble avg = ints.stream().mapToDouble(x -> x).average();

// ④ reduce（通用归约）
// 用法 1：无初始值（返回 Optional）
Optional<Integer> sum = list.stream().reduce(Integer::sum);

// 用法 2：有初始值（无 Optional）
int sum = list.stream().reduce(0, Integer::sum);

// 用法 3：并行安全的三参数（identity/accumulator/combiner）
int sum = list.stream()
    .reduce(0, (a, b) -> a + b, Integer::sum);
// ⚠️ 并行流必须三参数（combiner 合并分片结果）

// 用法 4：字符串连接（自定义）
String joined = words.stream()
    .reduce("", (a, b) -> a + "|" + b);
// ⚠️ 复杂归约优先用 Collectors（可读性）
```

> 🎯 **reduce 的选择**：数值聚合用原始流方法（sum/average）；通用归约用 reduce；**字符串连接/拼接用 Collectors.joining**（可读性）。

---

## 4. 匹配与查找

```java
// ① 匹配（短路！）
boolean any = orders.stream().anyMatch(o -> o.getStatus() == PAID);
// 任何一个满足 → true（找到即停）

boolean all = orders.stream().allMatch(o -> o.getAmount() > 0);
// 全部满足 → true（遇到不满足即停）

boolean none = orders.stream().noneMatch(o -> o.getStatus() == CLOSED);
// 全不满足 → true

// ② 查找
Optional<Order> first = orders.stream()
    .filter(o -> o.getStatus() == PAID)
    .findFirst();          // 第一个（顺序保证）
Optional<Order> any = orders.stream()
    .filter(o -> o.getStatus() == PAID)
    .findAny();            // 任意一个（并行更快）

// ③ 结果处理
Order order = orders.stream()
    .filter(o -> o.getId().equals("ORD-1"))
    .findFirst()
    .orElseThrow(() -> new OrderNotFoundException("ORD-1"));
// ⚠️ 业务查找：orElseThrow 比 orElse(null) 安全
```

> 🎯 **findFirst vs findAny**：顺序敏感用 findFirst（并行流额外开销）；只需要「任意一个」用 findAny（并行性能更好）。

---

## 5. Collectors 核心

> **背景**：Collectors 是收集器的工厂类——把流元素聚合成各种结果。
> **目的**：掌握核心收集器（分组/统计/连接/映射）。
> **适用范围**：Stream 收集的所有场景。

```java
// 核心收集器速览
Collectors.toList();          // 列表
Collectors.toSet();           // 集合
Collectors.toMap(k, v, merge);// Map
Collectors.joining(", ");     // 字符串连接
Collectors.groupingBy(f);     // 分组
Collectors.partitioningBy(p); // 分区（boolean）
Collectors.counting();        // 计数
Collectors.summingInt(f);     // 求和
Collectors.averagingDouble(f);// 平均
Collectors.summarizingInt(f); // 全统计（count/sum/min/max/avg）
Collectors.mapping(f, down);  // 映射（下游）
Collectors.reducing(id, f, op);// 归约
Collectors.collectingAndThen(c, finisher);  // 最终转换
Collectors.teeing(c1, c2, merge);           // 双收集（Java 12+）
```

---

## 6. 分组与分区

### 6.1 groupingBy（分组）

```java
// ① 基本分组（按字段）
Map<OrderStatus, List<Order>> byStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus));
// 结果：{PAID=[...], PENDING=[...], ...}

// ② 分组 + 计数（下游收集器）
Map<OrderStatus, Long> countByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

// ③ 分组 + 求和
Map<OrderStatus, Integer> sumByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus,
             Collectors.summingInt(Order::getAmount)));

// ④ 分组 + 映射（分组后取字段）
Map<OrderStatus, List<String>> namesByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus,
             Collectors.mapping(Order::getOrderId, Collectors.toList())));

// ⑤ 分组 + 保序（LinkedHashMap）
Map<OrderStatus, List<Order>> ordered = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus, LinkedHashMap::new,
             Collectors.toList()));

// ⑥ 多级分组（先按状态再按用户）
Map<OrderStatus, Map<String, List<Order>>> nested = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus,
             Collectors.groupingBy(Order::getUserId)));
```

### 6.2 partitioningBy（分区）

```java
// 分区：boolean 两组（true/false）
Map<Boolean, List<Order>> partition = orders.stream()
    .collect(Collectors.partitioningBy(o -> o.getAmount() > 100));
// {true=[大额订单], false=[小额订单]}

// 分区 + 下游
Map<Boolean, Long> count = orders.stream()
    .collect(Collectors.partitioningBy(o -> o.getAmount() > 100,
             Collectors.counting()));

// vs groupingBy：分区是「二分」（boolean）；分组是「多类」
```

---

## 7. 统计与连接

```java
// ① summarizing（单次遍历拿全统计——性能！）
IntSummaryStatistics stats = orders.stream()
    .collect(Collectors.summarizingInt(Order::getAmount));
stats.getCount();    // 数量
stats.getSum();      // 总和
stats.getMin();      // 最小
stats.getMax();      // 最大
stats.getAverage();  // 平均
// ⚠️ 对比：分别调用 count/sum/min/max 会多次遍历（慢）
// ✅ summarizing 一次遍历全拿到

// ② joining（字符串连接——比手动拼接优雅）
String csv = orders.stream()
    .map(Order::getOrderId)
    .collect(Collectors.joining(","));            // a,b,c
String withPrefix = orders.stream()
    .map(Order::getOrderId)
    .collect(Collectors.joining(",", "[", "]"));  // [a,b,c]

// ③ 常见组合（分组 + 统计）
Map<OrderStatus, IntSummaryStatistics> statsByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus,
             Collectors.summarizingInt(Order::getAmount)));
// 每个状态的全统计（count/sum/min/max/avg）
```

---

## 8. 高级 Collectors：teeing/collectingAndThen

### 8.1 teeing（Java 12+——双收集）

```java
// teeing：同时执行两个收集器，合并结果
// 场景：一次遍历同时拿「计数」和「总和」
SumAndCount result = orders.stream()
    .collect(Collectors.teeing(
        Collectors.counting(),                    // 收集器 1
        Collectors.summingInt(Order::getAmount),  // 收集器 2
        (count, sum) -> new SumAndCount(count, sum)  // 合并
    ));

// 场景：同时统计平均和拼接姓名
record Stats(double avg, String names) {}
Stats stats = users.stream()
    .collect(Collectors.teeing(
        Collectors.averagingDouble(User::getSalary),
        Collectors.mapping(User::getName, Collectors.joining(",")),
        Stats::new
    ));
```

### 8.2 collectingAndThen（最终转换）

```java
// collectingAndThen：收集后做最终转换
List<String> immutable = list.stream()
    .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        Collections::unmodifiableList    // 转不可变
    ));

// 场景：收集后取第一个/求最值
Optional<Order> maxOrder = orders.stream()
    .collect(Collectors.collectingAndThen(
        Collectors.maxBy(Comparator.comparing(Order::getAmount)),
        Optional::orElseThrow
    ));
```

---

## 9. Collector 内部机制

### 9.1 Collector 五要素

```text
Collector<T, A, R> 五要素（理解收集器的工作原理）
├── ① supplier：创建容器（如 ArrayList::new）
├── ② accumulator：向容器添加元素（list::add）
├── ③ combiner：合并两个容器（并行流分片合并）
├── ④ finisher：最终转换（如 toList 的转型）
└── ⑤ characteristics：特性标记
    ├── CONCURRENT：容器线程安全（可并行添加）
    ├── UNORDERED：无序（并行更自由）
    └── IDENTITY_FINISH：无最终转换（finisher 恒等）

并行流的正确性依赖
├── ① combiner 必须满足结合律（associative）
├── ② 有状态 accumulator 不能标记 CONCURRENT
└── ③ 自定义 Collector 需理解契约
```

### 9.2 自定义 Collector 示例

```java
// 自定义 Collector：逗号连接（理解五要素）
Collector<String, StringBuilder, String> joiningCollector =
    Collector.of(
        StringBuilder::new,                    // supplier
        (sb, s) -> {                           // accumulator
            if (sb.length() > 0) sb.append(",");
            sb.append(s);
        },
        (sb1, sb2) -> sb1.append(",").append(sb2),  // combiner
        StringBuilder::toString,               // finisher
        Collector.Characteristics.UNORDERED
    );

List<String> words = List.of("a", "b", "c");
String joined = words.stream().collect(joiningCollector);  // a,b,c
```

---

## 10. 核心要点

> 🎯 **核心要点**：
> 1. 终止操作触发执行——短路操作（anyMatch/findFirst）结果确定即停
> 2. toList（Java 16+ 不可变）vs collect(toList())（可变）；toMap 必须处理键冲突（三参数）
> 3. reduce 数值聚合用原始流方法；字符串连接用 joining（可读性）
> 4. findFirst（顺序）vs findAny（并行更快）——业务查找用 orElseThrow
> 5. groupingBy 六种用法（计数/求和/映射/保序/多级）；partitioningBy 二分
> 6. **summarizingInt 单次遍历拿全统计**（比多次调用高效）；teeing 双收集（Java 12+）
> 7. Collector 五要素 + 并行正确性（combiner 结合律）

---

**下一模块**：[04-高级操作与实战模式](04-高级操作与实战模式.md) | **返回总览**：[00-Java流Stream知识体系总览](00-Java流Stream知识体系总览.md)
