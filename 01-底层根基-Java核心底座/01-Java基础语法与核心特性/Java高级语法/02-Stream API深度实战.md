# 02 - Stream API 深度实战

> 🎯 Stream 是 Java 函数式编程的核心——filter/map/reduce 替代 for 循环，代码量减少 70%。AI 数据处理（筛选、转换、聚合）几乎离不开 Stream

---

## 目录

1. [Stream 三阶段模型](#1-stream-三阶段模型)
2. [核心操作速查](#2-核心操作速查)
3. [Collectors 收集器](#3-collectors-收集器)
4. [并行流与陷阱](#4-并行流与陷阱)

---

## 1. Stream 三阶段模型

```text
数据源 → 中间操作(惰性) → 终端操作(触发)

1. 数据源:    list.stream() / IntStream.range()
2. 中间操作:  filter / map / sorted / distinct / limit (返回 Stream, 不执行)
3. 终端操作:  collect / forEach / reduce / count (触发计算)
```

```java
List<String> names = List.of("Alice", "Bob", "Charlie", "David");

// 过滤 + 转换 + 收集
List<String> result = names.stream()
    .filter(name -> name.length() > 3)   // 中间：过滤
    .map(String::toUpperCase)             // 中间：转换
    .sorted()                             // 中间：排序
    .toList();                            // 终端：收集 (Java 16+)
```

## 2. 核心操作速查

```java
// ====== 中间操作 ======

// filter: 过滤
stream.filter(u -> u.getAge() > 18)

// map: 一对一转
stream.map(User::getName)                    // Stream<String>
stream.mapToInt(User::getAge)                // IntStream (避免装箱)

// flatMap: 一对多转 (展平嵌套结构)
orders.stream()
    .flatMap(order -> order.getItems().stream())  // List<Item> → Stream<Item>

// distinct: 去重
stream.distinct()

// sorted: 排序
stream.sorted(Comparator.comparing(User::getAge).reversed())

// limit / skip: 分页
stream.skip((page - 1) * size).limit(size)

// peek: 调试
stream.peek(u -> System.out.println("处理: " + u.getName()))

// ====== 终端操作 ======

// collect: 收集到集合
stream.collect(Collectors.toList())
stream.collect(Collectors.toMap(User::getId, Function.identity()))

// reduce: 归约
int sum = stream.mapToInt(Item::getPrice).sum()
int total = stream.reduce(0, Integer::sum)

// 查找
stream.findFirst()       // Optional
stream.findAny()         // 并行友好
stream.anyMatch(u -> u.getAge() > 18)  // boolean

// 遍历
stream.forEach(System.out::println)
```

## 3. Collectors 收集器

```java
import static java.util.stream.Collectors.*;

// toList / toSet / toMap
List<User> list = stream.toList();           // Java 16+ (不可变)
Map<Long, User> map = stream.collect(toMap(User::getId, identity()));

// 分组
Map<String, List<User>> byCity = stream.collect(groupingBy(User::getCity));
Map<String, Long> countByCity = stream.collect(groupingBy(User::getCity, counting()));

// 分区 (true / false)
Map<Boolean, List<User>> adults = stream.collect(partitioningBy(u -> u.getAge() >= 18));

// 统计
IntSummaryStatistics stats = stream.collect(summarizingInt(User::getAge));
// stats.getAverage(), getMax(), getSum()...

// 拼接
String names = stream.map(User::getName).collect(joining(", "));

// 自定义 Collector
Collector<User, ?, TreeMap<String, List<User>>> sortedGrouping =
    groupingBy(User::getCity, TreeMap::new, toList());
```

## 4. 并行流与陷阱

```java
// 并行流：一行代码获得多线程
List<Integer> result = list.parallelStream()
    .map(this::heavyComputation)
    .toList();

// ⚠️ 陷阱 1: 不要用并行流操作共享可变状态
int[] sum = {0};  // ❌ 线程不安全
list.parallelStream().forEach(i -> sum[0] += i);

// ⚠️ 陷阱 2: parallelStream 使用 ForkJoinPool.commonPool()
// Hibernate Session 绑定在主线程 → parallelStream 中不可用
// 解决：Spring @Async 代替，或用自定义 ForkJoinPool

// ⚠️ 陷阱 3: 小数据量用并行反而更慢（线程调度开销）
// < 1000 条数据 → 不用并行
```

## 核心要点回顾

- Stream = 数据源(stream) → 中间(filter/map) → 终端(collect)
- flatMap 展平嵌套结构（最常用也最容易被忘）
- Collectors.groupingBy + counting = 分组统计（代替 SQL GROUP BY）
- 调试用 peek()，不要用 forEach 里 System.out.println
- 并行流三不：不操作共享状态、不在 Hibernate 中用、小数据不用

## 参考资料

1. java.util.stream 包文档
2. Modern Java in Action — Raoul-Gabriel Urma
