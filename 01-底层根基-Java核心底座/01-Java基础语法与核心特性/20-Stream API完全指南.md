# 20 - Stream API完全指南
> 定位：彻底掌握Stream的惰性求值、中间操作与终止操作分类、Collector收集器、并行流的正确使用，能用Stream替代传统循环写出简洁高效的代码

## 目录
1. [Stream概述与特性](#1-stream概述与特性)
2. [Stream的创建方式](#2-stream的创建方式)
3. [中间操作（Intermediate Operations）](#3-中间操作intermediate-operations)
4. [终止操作（Terminal Operations）](#4-终止操作terminal-operations)
5. [Collector收集器详解](#5-collector收集器详解)
6. [基本类型流（IntStream/LongStream/DoubleStream）](#6-基本类型流intstreamlongstreamdoublestream)
7. [并行流（Parallel Stream）](#7-并行流parallel-stream)
8. [Stream的执行顺序与短路](#8-stream的执行顺序与短路)
9. [Stream vs 传统循环：性能考量](#9-stream-vs-传统循环性能考量)
10. [Stream实战案例集](#10-stream实战案例集)
11. [常见陷阱与最佳实践](#11-常见陷阱与最佳实践)
12. [面试高频考点](#12-面试高频考点)

---

## 1. Stream概述与特性

**什么是Stream**：Java 8引入的函数式编程API（`java.util.stream`），是数据源的视图而非数据结构。本身不存储数据，对数据源进行声明式操作，不改变原数据源。

| 核心特性 | 说明 |
|----------|------|
| **惰性求值** | 中间操作不会立即执行，终止操作才触发全部执行 |
| **一次性使用** | 终止操作执行后流关闭，再次使用抛 `IllegalStateException` |
| **不修改源数据** | 所有操作产生新结果，不改变原始数据源 |

**操作三阶段**：`创建Stream → 0~N个中间操作 → 1个终止操作 → 最终结果`

> 💡 **记忆口诀**："创建流、中间连、终止算"。

---

## 2. Stream的创建方式

```java
// 1. 从集合创建（最常用）
List<String> list = Arrays.asList("Java", "Stream", "API");
Stream<String> stream = list.stream();
Stream<String> parallelStream = list.parallelStream();

// 2. 从数组创建
String[] arr = {"a", "b", "c"};
Stream<String> arrStream = Arrays.stream(arr);
IntStream intStream = Arrays.stream(new int[]{1, 2, 3});  // 基本类型流

// 3. 通过Stream静态方法创建
Stream<String> ofStream = Stream.of("a", "b", "c");      // 指定元素
Stream<Integer> iterateStream = Stream.iterate(0, n -> n + 2).limit(5); // 0,2,4,6,8
Stream<Double> randomStream = Stream.generate(Math::random).limit(3);
Stream<Integer> iterate9 = Stream.iterate(0, n -> n < 100, n -> n + 1); // JDK 9+

// 4. 其他方式
IntStream range = IntStream.range(0, 10);       // [0,10)
IntStream rangeClosed = IntStream.rangeClosed(0, 10); // [0,10]
// Stream<String> lines = Files.lines(Paths.get("file.txt"));
```

| 方法 | 说明 | 场景 |
|------|------|------|
| `集合.stream()` | 从Collection创建 | 最常用，覆盖90%场景 |
| `Arrays.stream(arr)` | 从数组创建 | 处理数组数据 |
| `Stream.of(...)` | 直接传入元素 | 少量数据快速构造 |
| `Stream.iterate/generate` | 无限流，需配合`limit()` | 生成序列/随机数 |
| `IntStream.range()` | 整数范围 | 数值计算 |

---

## 3. 中间操作（Intermediate Operations）

中间操作返回新Stream，构建操作链。**所有中间操作都是惰性求值**——不触发实际计算。

### 3.1 完整速查表

| 操作分类 | API | 参数 | 说明 |
|----------|-----|------|------|
| **过滤** | `filter(Predicate)` | `Predicate<T>` | 筛选符合条件的元素 |
| **映射** | `map(Function)` | `Function<T, R>` | 一对一映射转换 |
| **映射** | `flatMap(Function)` | `T → Stream<R>` | 一对多映射，展开流再合并 |
| **映射** | `mapToInt/ToLong/ToDouble` | ToXxxFunction | 转为基本类型流 |
| **排序** | `sorted()` | 无 | 自然排序（元素需实现Comparable） |
| **排序** | `sorted(Comparator)` | `Comparator<T>` | 按比较器排序 |
| **去重** | `distinct()` | 无 | 依赖`equals()`/`hashCode()`去重 |
| **截取** | `limit(long n)` | `long` | 保留前n个元素 |
| **跳过** | `skip(long n)` | `long` | 跳过前n个元素 |
| **调试** | `peek(Consumer)` | `Consumer<T>` | 消费每个元素但不变更数据 |

### 3.2 操作详解

```java
List<String> words = Arrays.asList("Java", "Python", "Go", "Rust", "Java");

// filter：过滤
List<String> longWords = words.stream()
    .filter(s -> s.length() > 3)
    .collect(Collectors.toList());  // [Java, Python, Rust]

// map：一对一映射
List<Integer> lengths = words.stream()
    .map(String::length)
    .collect(Collectors.toList());  // [4, 6, 2, 4, 4]

// flatMap：一对多扁平化（避免流嵌套流）
List<List<String>> nested = Arrays.asList(
    Arrays.asList("a", "b"), Arrays.asList("c", "d"));
List<String> flat = nested.stream()
    .flatMap(Collection::stream)
    .collect(Collectors.toList());  // [a, b, c, d]

// 实用：句子拆单词
List<String> words_ = Arrays.stream("Hello World Java Stream".split(" "))
    .collect(Collectors.toList());  // [Hello, World, Java, Stream]

// distinct：去重
List<String> distinct = words.stream().distinct().collect(Collectors.toList());

// sorted：排序
List<String> sorted = words.stream()
    .sorted(Comparator.comparingInt(String::length).reversed()
        .thenComparing(Comparator.naturalOrder()))
    .collect(Collectors.toList());

// limit / skip：分页（每页5条取第2页）
List<String> page2 = list.stream().skip(5).limit(5).collect(Collectors.toList());

// peek：调试（仅调试用，无终止操作不执行）
long c = words.stream().peek(System.out::println).filter(s -> s.length() > 3).count();
```

> ⚠️ **map vs flatMap**：`map` 是一对一，`flatMap` 是一对多——将每个元素展开为一个流再合并。当处理"流中元素本身就是集合/数组"时用 `flatMap`。

---

## 4. 终止操作（Terminal Operations）

终止操作触发所有中间操作执行，执行后流关闭，返回非Stream结果。

### 4.1 完整速查表

| 分类 | API | 返回值 | 说明 |
|------|-----|--------|------|
| **收集** | `collect(Collector)` | `R` | 收集为集合/Map/字符串等 |
| **收集** | `toArray(IntFunction)` | `T[]` | 转为数组 |
| **遍历** | `forEach(Consumer)` | `void` | 遍历每个元素 |
| **遍历** | `forEachOrdered(Consumer)` | `void` | 按源顺序遍历（并行流保证顺序） |
| **统计** | `count()` | `long` | 元素个数 |
| **统计** | `max(Comparator)` | `Optional<T>` | 最大值 |
| **统计** | `min(Comparator)` | `Optional<T>` | 最小值 |
| **匹配** | `anyMatch(Predicate)` | `boolean` | 任一匹配→true（短路） |
| **匹配** | `allMatch(Predicate)` | `boolean` | 全部匹配→true（短路） |
| **匹配** | `noneMatch(Predicate)` | `boolean` | 无匹配→true（短路） |
| **查找** | `findFirst()` | `Optional<T>` | 第一个元素 |
| **查找** | `findAny()` | `Optional<T>` | 任意元素（并行流更优） |
| **归约** | `reduce(identity, acc)` | `T` | 带初始值归约 |
| **归约** | `reduce(acc)` | `Optional<T>` | 无初始值归约 |
| **归约** | `reduce(id, acc, combiner)` | `U` | 并行三参数归约 |

### 4.2 代码示例

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
List<String> words = Arrays.asList("Java", "Python", "Go", "Rust");

// collect：收集
List<String> collected = words.stream().filter(s -> s.length() > 3)
    .collect(Collectors.toList());
String joined = words.stream().collect(Collectors.joining(", ", "[", "]"));
// [Java, Python, Go, Rust]

// count / max / min
long count = words.stream().filter(s -> s.length() > 3).count();
Optional<String> longest = words.stream().max(Comparator.comparingInt(String::length));

// 匹配（短路）
boolean hasEven = numbers.stream().anyMatch(n -> n % 2 == 0);    // true
boolean allPositive = numbers.stream().allMatch(n -> n > 0);     // true
boolean noneNegative = numbers.stream().noneMatch(n -> n < 0);   // true

// findFirst / findAny
Optional<String> first = words.stream().filter(s -> s.startsWith("J")).findFirst();
Optional<String> any = words.parallelStream().filter(s -> s.startsWith("J")).findAny();

// forEach
words.stream().forEach(System.out::println);

// reduce：归约
int sum = numbers.stream().reduce(0, Integer::sum);       // 55
Optional<Integer> product = numbers.stream().reduce((a, b) -> a * b); // 3628800
int max = numbers.stream().reduce(Integer.MIN_VALUE, Integer::max);
```

> 💡 **findFirst vs findAny**：串行流下行为相同；并行流下 `findAny` 性能更优（不要求返回第一个）。仅在需要有序结果时才用 `findFirst`。

---

## 5. Collector收集器详解

### 5.1 toMap

```java
// 基础用法（key冲突抛异常）
List<Student> students = Arrays.asList(...);
Map<String, Integer> map = students.stream()
    .collect(Collectors.toMap(Student::getName, Student::getScore));

// 解决key冲突：取较大值
Map<String, Integer> mergeMap = students.stream()
    .collect(Collectors.toMap(
        Student::getName, Student::getScore,
        (v1, v2) -> Math.max(v1, v2)   // mergeFunction
    ));

// 指定Map类型
Map<String, Integer> linkedMap = students.stream()
    .collect(Collectors.toMap(
        Student::getName, Student::getScore,
        (v1, v2) -> v1,
        LinkedHashMap::new
    ));
```

### 5.2 groupingBy

```java
List<String> words = Arrays.asList("Java", "Python", "Go", "Rust", "JS");

// 基本分组
Map<Integer, List<String>> byLen = words.stream()
    .collect(Collectors.groupingBy(String::length));
// {2=[Go, JS], 4=[Java, Rust], 6=[Python]}

// 分组后计数
Map<Integer, Long> countByLen = words.stream()
    .collect(Collectors.groupingBy(String::length, Collectors.counting()));

// 分组后映射
Map<Integer, List<String>> upperGroup = words.stream()
    .collect(Collectors.groupingBy(
        String::length,
        Collectors.mapping(String::toUpperCase, Collectors.toList())
    ));

// 分组后求和
Map<Integer, Integer> sumByLen = words.stream()
    .collect(Collectors.groupingBy(
        String::length,
        Collectors.summingInt(String::length)
    ));

// 多级分组
Map<Integer, Map<Character, List<String>>> multiGroup = words.stream()
    .collect(Collectors.groupingBy(
        String::length,
        Collectors.groupingBy(s -> s.charAt(0))
    ));
```

### 5.3 partitioningBy

```java
// 分区：始终只有 true/false 两组
Map<Boolean, List<String>> partitioned = words.stream()
    .collect(Collectors.partitioningBy(s -> s.length() > 3));
// {true=[Java, Python, Rust], false=[Go, JS]}

// 分区后统计
Map<Boolean, Long> partCount = words.stream()
    .collect(Collectors.partitioningBy(
        s -> s.length() > 3, Collectors.counting()
    ));
```

> 💡 **groupingBy vs partitioningBy**：`partitioningBy` 的键固定为boolean（两组）；`groupingBy` 可多组。分支少时 `partitioningBy` 性能略优。

### 5.4 全部下游收集器速查

| 收集器 | 说明 | 返回值 |
|--------|------|--------|
| `toList()` / `toSet()` / `toCollection(factory)` | 收集到集合 | `List/Set/Collection` |
| `toMap(key, value, merge)` | 收集到Map | `Map<K,V>` |
| `groupingBy(classifier, downstream)` | 分组 | `Map<K, List<V>>` |
| `partitioningBy(predicate, downstream)` | 分区 | `Map<Boolean, List<V>>` |
| `counting()` | 计数 | `Long` |
| `summingInt/Long/Double(mapper)` | 求和 | 数值 |
| `averagingInt/Long/Double(mapper)` | 平均值 | `Double` |
| `summarizingInt/Long/Double(mapper)` | 一次性统计 | `XxxSummaryStatistics` |
| `maxBy(comparator)` / `minBy(comparator)` | 最大/小值 | `Optional<T>` |
| `mapping(mapper, downstream)` | 转换后收集 | 取决于下游 |
| `filtering(predicate, downstream)` | JDK 9+ 过滤后收集 | 取决于下游 |
| `collectingAndThen(downstream, finisher)` | 收集后再转换 | 取决于finisher |
| `reducing(identity, op)` | 归约 | 取决于结果 |
| `joining(delimiter, prefix, suffix)` | 连接字符串 | `String` |

---

## 6. 基本类型流（IntStream/LongStream/DoubleStream）

避免自动装箱开销并提供专属数值方法。

```java
// 创建
IntStream.range(1, 10);             // [1,10)
IntStream.rangeClosed(1, 10);       // [1,10]
IntStream intStream = Arrays.stream(new int[]{1,2,3});
IntStream fromObj = words.stream().mapToInt(String::length);
IntStream.of(1, 2, 3);

// 独有方法
int sum = intStream.sum();                    // 求和
OptionalDouble avg = intStream.average();     // 平均值
OptionalInt max = intStream.max();            // 最大值

// summaryStatistics：一次性获取全部统计值
IntSummaryStatistics stats = IntStream.rangeClosed(1, 100)
    .summaryStatistics();
// stats.getSum()=5050, getAverage()=50.5, getMax()=100, getMin()=1, getCount()=100

// 转回对象流
Stream<Integer> boxed = IntStream.range(0, 10).boxed();
```

---

## 7. 并行流（Parallel Stream）

### 7.1 原理与使用

底层使用 `ForkJoinPool.commonPool()`，默认并行度 = CPU核数 - 1。

```java
// 开启方式
list.parallelStream();
list.stream().parallel();

// 关闭并行
list.parallelStream().sequential();
```

### 7.2 适用场景表

| 场景 | 是否适合 | 原因 |
|------|----------|------|
| 数据量大（>10000） | ✅ | 并行化收益超过开销 |
| CPU密集型计算 | ✅ | 充分利用多核 |
| 无状态操作 | ✅ | 元素独立，无共享状态 |
| ArrayList/数组为源 | ✅ | O(1)随机访问，易拆分 |
| 数据量小（<1000） | ❌ | 创建线程开销更大 |
| IO密集型 | ❌ | CPU空闲，多线程无帮助 |
| 有状态操作（sorted/limit） | ❌ | 需要全局协调 |
| LinkedList为源 | ❌ | 拆分困难需遍历 |

### 7.3 线程安全问题

```java
// ❌ 错误：并行流修改非线程安全集合
List<Integer> list = new ArrayList<>();
IntStream.range(0, 1000).parallel().forEach(list::add);  // 结果 < 1000

// ✅ 正确：使用收集器
List<Integer> safe = IntStream.range(0, 1000).parallel()
    .boxed().collect(Collectors.toList());

// ✅ 正确：无状态 reduce
int sum = IntStream.range(0, 1000).parallel().sum();

// 自定义ForkJoinPool（避免占用公共线程池）
ForkJoinPool pool = new ForkJoinPool(4);
pool.submit(() -> LongStream.rangeClosed(1, 10_000_000).parallel().sum()).get();
pool.shutdown();
```

> ⚠️ **并行流警告**：不是银弹。共享可变状态、小数据量、有状态操作时可能更慢或产生线程安全问题。务必测试验证加速比。

---

## 8. Stream的执行顺序与短路

### 8.1 垂直处理

Stream元素是**逐个穿过**所有中间操作的，而非先全部完成一个操作再开始下一个。

```java
Stream.of("Alice", "Bob", "Charlie", "David")
    .filter(s -> { System.out.println("filter: " + s); return s.length() > 3; })
    .map(s -> { System.out.println("map: " + s); return s.toUpperCase(); })
    .limit(2)
    .forEach(s -> System.out.println("forEach: " + s));

// 输出顺序（垂直处理）：
// filter: Alice → map: Alice → forEach: ALICE
// filter: Bob（被过滤，不继续）
// filter: Charlie → map: Charlie → forEach: CHARLIE
// limit(2) 已满足 → David 不会被处理
```

> 🎯 理解垂直处理很重要：`limit`/`anyMatch` 等短路操作只需处理到满足条件即可停止，避免不必要的计算。

### 8.2 短路操作

| 操作 | 短路条件 |
|------|----------|
| `limit(n)` | 处理完n个后停止 |
| `anyMatch(predicate)` | 找到第一个匹配→true |
| `allMatch(predicate)` | 找到第一个不匹配→false |
| `noneMatch(predicate)` | 找到第一个匹配→false |
| `findFirst()` / `findAny()` | 找到后立即停止 |

```java
// 短路 + 无限流：必须配合 limit 使用
List<Integer> first10 = Stream.iterate(0, n -> n + 1).limit(10)
    .collect(Collectors.toList());  // 无 limit 则 OOM
```

---

## 9. Stream vs 传统循环：性能考量

| 场景 | Stream | 传统for | 选择建议 |
|------|--------|---------|----------|
| 简单过滤/映射/收集 | 略慢5-10% | 快 | 推荐Stream（差异可忽略） |
| 复杂多操作链 | 相近 | 相近 | 用Stream（更简洁） |
| 大数求和（基本类型） | 相近 | 相近 | 用IntStream可匹敌手写 |
| 并行计算 | 快3-4x | 需手动多线程 | 用并行流 |
| 修改外部变量 | 不支持 | 支持 | 用传统循环 |
| break/continue/return | 不支持 | 支持 | 用传统循环 |
| 捕获异常 | 难处理 | 容易 | 用传统循环 |

```java
// ✅ 推荐 Stream：链式过滤/转换/分组
List<String> result = list.stream()
    .filter(s -> s.startsWith("A")).map(String::toUpperCase)
    .sorted().collect(Collectors.toList());

// ❌ 适合传统循环：需要 break/异常处理
for (String s : list) {
    try { process(s); } catch (Exception e) { break; }
}
```

> 🎯 **一句话原则**：能用Stream清晰表达意图就用Stream；需要细粒度控制（异常处理、break/continue、多层嵌套）时用传统循环。

---

## 10. Stream实战案例集

### 10.1 交易统计

```java
record Transaction(String id, String currency, double amount, LocalDate date) {}

List<Transaction> transactions = Arrays.asList(
    new Transaction("T001", "USD", 1000, LocalDate.of(2024, 1, 5)),
    new Transaction("T002", "CNY", 5000, LocalDate.of(2024, 1, 10)),
    new Transaction("T003", "USD", 2000, LocalDate.of(2024, 2, 1)),
    new Transaction("T004", "JPY", 10000, LocalDate.of(2024, 2, 15)),
    new Transaction("T005", "USD", 300, LocalDate.of(2024, 3, 1))
);

// 按货币分组统计总额
Map<String, Double> sumByCurrency = transactions.stream()
    .collect(Collectors.groupingBy(
        Transaction::currency,
        Collectors.summingDouble(Transaction::amount)
    ));

// 美元交易 > 500，金额降序取前3
List<Transaction> topUsd = transactions.stream()
    .filter(t -> "USD".equals(t.currency()) && t.amount() > 500)
    .sorted(Comparator.comparingDouble(Transaction::amount).reversed())
    .limit(3).collect(Collectors.toList());

// 大/小额交易分区
Map<Boolean, List<Transaction>> partitioned = transactions.stream()
    .collect(Collectors.partitioningBy(t -> t.amount() >= 1000));

// 按月份分组
Map<Month, List<Transaction>> byMonth = transactions.stream()
    .collect(Collectors.groupingBy(t -> t.date().getMonth()));
```

### 10.2 员工统计

```java
record Employee(String name, String dept, double salary, int age) {}

List<Employee> employees = Arrays.asList(
    new Employee("张三", "技术部", 15000, 28),
    new Employee("李四", "技术部", 20000, 35),
    new Employee("王五", "市场部", 12000, 25),
    new Employee("赵六", "市场部", 18000, 32),
    new Employee("钱七", "技术部", 25000, 40)
);

// 部门平均薪资
Map<String, Double> avgSalaryByDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::dept, Collectors.averagingDouble(Employee::salary)
    ));

// 部门薪资统计（一次性获取总额/平均/最大/最小）
Map<String, DoubleSummaryStatistics> deptStats = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::dept, Collectors.summarizingDouble(Employee::salary)
    ));

// 按年龄分组
Map<String, List<Employee>> ageGroup = employees.stream()
    .collect(Collectors.groupingBy(e -> {
        if (e.age() < 30) return "青年";
        else if (e.age() <= 50) return "中年";
        else return "老年";
    }));

// 筛选薪资 > 15000 且年龄 < 35
List<String> filtered = employees.stream()
    .filter(e -> e.salary() > 15000 && e.age() < 35)
    .map(Employee::name).collect(Collectors.toList());
```

### 10.3 词频统计

```java
String text = "java stream api java lambda stream";
Map<String, Long> wordCount = Arrays.stream(text.split(" "))
    .collect(Collectors.groupingBy(
        Function.identity(), Collectors.counting()
    ));
// {java=2, stream=2, api=1, lambda=1}

// 按频率降序排序
List<Map.Entry<String, Long>> sorted = wordCount.entrySet().stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .collect(Collectors.toList());
```

---

## 11. 常见陷阱与最佳实践

### 11.1 常见陷阱

| 陷阱 | 错误示例 | 正确做法 | 后果 |
|------|----------|----------|------|
| **流重复使用** | 声明Stream后多次终止操作 | 每次重新创建流 | `IllegalStateException` |
| **Optional.get()** | `stream.findFirst().get()` | `orElse()` / `orElseThrow()` | `NoSuchElementException` |
| **并行线程安全** | 并行流 `ArrayList::add` | `collect(Collectors.toList())` | 数据丢失 |
| **无限流无limit** | `Stream.generate(...)` 无limit | 加`limit(n)` | OOM |
| **forEach顺序依赖** | 并行流用forEach处理有序数据 | 用`forEachOrdered`或串行流 | 顺序不固定 |
| **先map后filter** | 大量数据先map后filter | 先filter减少数据量再map | 性能浪费 |

### 11.2 最佳实践

```java
// 1. 先 filter 再 map（减少不必要的映射）
list.stream().filter(Objects::nonNull).map(this::expensiveMapping)

// 2. 用方法引用替代 Lambda
list.stream().map(String::toUpperCase)

// 3. 用 Optional 安全获取结果
String first = list.stream().findFirst().orElse("默认值");

// 4. 集合判空后再创建流
Optional.ofNullable(list).orElseGet(Collections::emptyList).stream()

// 5. 长链操作换行
list.stream()
    .filter(x -> x > 0)
    .map(String::valueOf)
    .sorted()
    .collect(Collectors.toList());

// 6. 避免在 forEach 中修改外部变量 —— 用 collect 替代
List<String> result = list.stream()
    .filter(s -> s.length() > 3)
    .collect(Collectors.toList());
```

> ⚠️ 保持可读性是第一原则。团队项目中约定统一的Stream使用规范。

---

## 12. 面试高频考点

### 12.1 基础概念

| 问题 | 要点 |
|------|------|
| Stream和集合的区别？ | 集合存数据，Stream算数据；Stream不修改源、惰性求值、一次性 |
| 中间vs终止操作？ | 中间返回Stream（惰性），终止触发计算（返回非Stream） |
| 惰性求值的好处？ | 避免不必要计算，支持短路优化，可组合为流水线 |

### 12.2 操作原理

| 问题 | 要点 |
|------|------|
| map vs flatMap？ | map一对一；flatMap一对多（展开为流再合并） |
| findFirst vs findAny？ | findFirst返回第一个（有序），findAny返回任意（并行更优） |
| peek vs forEach？ | peek是中间操作（调试），forEach是终止操作（消费） |
| reduce三参数含义？ | identity（初始值）、accumulator（累加器）、combiner（并行合并器） |

### 12.3 Collector

| 问题 | 要点 |
|------|------|
| toMap key冲突？ | 传入mergeFunction处理，否则抛IllegalStateException |
| groupingBy原理？ | 按classifier分组，下游收集器二次处理 |
| partitioningBy vs groupingBy？ | 前者boolean键（2组），后者可多组 |
| 自定义Collector？ | `Collector.of(supplier, accumulator, combiner, finisher)` |

### 12.4 并行流

| 问题 | 要点 |
|------|------|
| 并行流底层？ | ForkJoinPool.commonPool()，工作窃取 |
| 何时使用并行流？ | 数据量大+CPU密集+无状态+易拆分 |
| 线程安全问题？ | 共享可变状态危险，用collect/reduce |
| 并行一定更快？ | 不一定，小数据/有状态/IO密集时可能更慢 |

### 12.5 手写代码题

```java
// 按长度分组
Map<Integer, List<String>> groupByLength = list.stream()
    .collect(Collectors.groupingBy(String::length));

// 偶数平方和
int sum = numbers.stream().filter(n -> n % 2 == 0)
    .mapToInt(n -> n * n).sum();

// 去重排序
List<Integer> result = list.stream().distinct().sorted()
    .collect(Collectors.toList());

// 词频排序
Map<String, Long> freq = Arrays.stream(text.split(" "))
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
List<Map.Entry<String, Long>> sorted = freq.entrySet().stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .collect(Collectors.toList());

// List<Student> → Map<id, name>
Map<Integer, String> studentMap = students.stream()
    .collect(Collectors.toMap(Student::getId, Student::getName, (v1, v2) -> v1));
```

---

> 🎯 **学习路径**：先掌握 `filter`/`map`/`collect` 三件套（覆盖80%场景），再学 `flatMap`/`groupingBy`/`reduce` 进阶操作。日常开发中刻意用Stream替代传统循环，逐步内化函数式编程思维。Stream + Lambda + 方法引用是Java 8+开发的"铁三角"。
