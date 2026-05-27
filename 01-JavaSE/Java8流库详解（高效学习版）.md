# Java8流库详解（高效学习版）

Java8流库（Stream API）是Java8核心新特性之一，核心作用是简化集合的遍历、过滤、映射、聚合等操作，替代传统的for循环和迭代器，让代码更简洁、易读、可维护。

学习流库的关键是"理解流的本质、掌握核心API、区分中间操作与终止操作"，结合"输出倒逼输入"的学习方法，每学一个API就搭配代码练习，就能快速上手。

---

## 一、流库核心前提（必懂基础）

在学习流库前，先明确3个核心要点：

- **流的本质**：Stream 不是集合，也不是数据结构，而是"数据源的视图"——它本身不存储数据，只是对数据源（集合、数组等）进行一系列操作，操作不会改变原数据源
- **流的特性**：
  - **惰性求值**：中间操作不会立即执行，只有执行终止操作时，所有中间操作才会一次性执行（提升效率）
  - **一次性使用**：一个流只能执行一次终止操作，执行后流就会关闭，再次使用会报异常
- **核心用途**：替代传统 for-each 循环，简化集合的复杂操作（如过滤、映射、统计聚合等），尤其适合处理大数据量的集合操作

> **学习技巧**：先记住 **"流 = 数据源 + 操作链 + 终止操作"**，用简单的集合创建流，执行一个简单操作（如过滤），直观感受流的使用方式，比单纯记忆概念更高效。

---

## 二、流的创建方式（入门必会）

常用4种创建方式，覆盖大部分开发场景，**重点掌握前3种**。

### 1. 通过集合创建（最常用）

Java8为 `Collection` 接口新增了 `stream()` 和 `parallelStream()` 方法。

```java
List<String> list = Arrays.asList("Java", "Stream", "API");
Stream<String> stream = list.stream();                     // 串行流
Stream<String> parallelStream = list.parallelStream();     // 并行流
```

### 2. 通过数组创建

使用 `Arrays.stream()` 方法，支持基本类型数组和引用类型数组。

```java
String[] arr = {"a", "b", "c"};
Stream<String> stream = Arrays.stream(arr);

int[] intArr = {1, 2, 3};
IntStream intStream = Arrays.stream(intArr);    // 基本类型流（避免自动装箱，提升效率）
```

### 3. 通过 Stream 静态方法创建

| 方法 | 作用 |
|------|------|
| `Stream.of(T... values)` | 创建包含指定元素的流 |
| `Stream.empty()` | 创建空流 |
| `Stream.generate(Supplier)` | 生成无限流（需配合 `limit()`） |
| `Stream.iterate(seed, f)` | 迭代生成无限流 |

```java
Stream<String> stream1 = Stream.of("a", "b", "c");               // 指定元素流
Stream<String> stream2 = Stream.empty();                          // 空流
Stream<Integer> stream3 = Stream.generate(() -> new Random().nextInt(100));  // 无限流，需limit()
```

### 4. 通过其他方式创建

如从文件、IO流、字符串中创建流（实际开发中较少用，了解即可）。

---

## 三、流的核心操作（重点突破）

流的操作分为**中间操作**和**终止操作**，两者必须搭配使用：中间操作构建操作链，终止操作触发执行。

### 1. 中间操作（惰性求值，不触发执行）

中间操作返回一个新的流，可链式调用多个。

| API | 作用 | 示例 |
|-----|------|------|
| `filter(Predicate)` | 过滤出符合条件的元素 | `stream.filter(s -> s.length() > 5)` |
| `map(Function)` | 将元素映射为另一种类型 | `stream.map(String::length)` |
| `flatMap(Function)` | 将元素转为流再合并（解决"流嵌套流"） | `stream.flatMap(s -> Stream.of(s.split("")))` |
| `sorted()` / `sorted(Comparator)` | 排序，默认自然顺序 | `stream.sorted()` / `stream.sorted(Comparator.reverseOrder())` |
| `distinct()` | 去重（依赖 `equals()` 方法） | `stream.distinct()` |
| `limit(long n)` | 保留前n个元素 | `Stream.generate(...).limit(5)` |
| `skip(long n)` | 跳过前n个元素 | `stream.skip(1)` |

> `map` 是"一对一"映射，`flatMap` 是"一对多"映射——避免用 `map` 处理"流嵌套流"的场景。

### 2. 终止操作（触发执行，返回非流结果）

终止操作触发所有中间操作的执行，执行后流关闭。

#### （1）收集结果（最常用）

```java
stream.collect(Collectors.toList());                                    // 转为 List
stream.collect(Collectors.toSet());                                     // 转为 Set
stream.collect(Collectors.toMap(keyMapper, valueMapper));               // 转为 Map（注意key不重复）
stream.toArray(String[]::new);                                          // 转为数组
```

#### （2）聚合统计

| API | 返回值 | 说明 |
|-----|--------|------|
| `count()` | `long` | 元素个数 |
| `max(Comparator)` | `Optional<T>` | 最大值 |
| `min(Comparator)` | `Optional<T>` | 最小值 |
| `sum()` | 数值 | 求和（仅数值流 IntStream/LongStream/DoubleStream） |
| `summaryStatistics()` | `IntSummaryStatistics` | 一次性获取总和、平均值、最大/最小值、个数 |

#### （3）遍历

```java
stream.forEach(System.out::println);    // 遍历每个元素，Consumer 函数式接口
```

#### （4）判断匹配

| API | 说明 |
|-----|------|
| `allMatch(Predicate)` | 所有元素都符合条件 → `true` |
| `anyMatch(Predicate)` | 至少一个元素符合条件 → `true` |
| `noneMatch(Predicate)` | 所有元素都不符合条件 → `true` |
| `findFirst()` | 获取第一个元素，返回 `Optional<T>` |
| `findAny()` | 获取任意一个元素（并行流效率更高），返回 `Optional<T>` |

---

## 四、流库实战示例

### 示例1：过滤、映射、收集

需求：从学生列表中筛选出年龄 > 18 的学生，获取姓名，存入新列表。

```java
class Student {
    private String name;
    private int age;
    // 构造方法、getter/setter 省略
}

public class StreamDemo {
    public static void main(String[] args) {
        List<Student> studentList = Arrays.asList(
            new Student("张三", 17),
            new Student("李四", 19),
            new Student("王五", 20),
            new Student("赵六", 18)
        );

        List<String> adultNames = studentList.stream()
            .filter(student -> student.getAge() > 18)   // 过滤年龄 > 18
            .map(Student::getName)                       // 映射为姓名
            .collect(Collectors.toList());                // 收集到 List

        System.out.println(adultNames);  // 输出：[李四, 王五]
    }
}
```

### 示例2：数值流聚合统计

需求：统计整数列表的总和、平均值、最大值、最小值。

```java
public class StreamStatDemo {
    public static void main(String[] args) {
        List<Integer> numList = Arrays.asList(10, 20, 30, 40, 50);

        IntSummaryStatistics stats = numList.stream()
            .mapToInt(Integer::intValue)       // 转为 IntStream（避免自动装箱）
            .summaryStatistics();

        System.out.println("总和：" + stats.getSum());        // 150
        System.out.println("平均值：" + stats.getAverage());   // 30.0
        System.out.println("最大值：" + stats.getMax());       // 50
        System.out.println("最小值：" + stats.getMin());       // 10
        System.out.println("元素个数：" + stats.getCount());   // 5
    }
}
```

---

## 五、学习技巧与避坑要点

### 1. 高效学习技巧

- **先记核心区别**：牢记"中间操作惰性求值，终止操作触发执行"，避免误以为中间操作会立即执行（单独调用 `filter()` 不会有任何效果）
- **API分类记忆**：中间操作按"过滤、映射、排序、限制"分类，终止操作按"收集、统计、遍历、匹配"分类，每天练1-2个API
- **函数式接口配合**：流库依赖 `Predicate`、`Function`、`Consumer` 等函数式接口，重点掌握用法（接收什么参数、返回什么结果），结合方法引用简化代码

### 2. 常见易错点

| 易错点 | 说明 |
|--------|------|
| **流重复使用** | 一个流执行终止操作后会关闭，再次使用抛出 `IllegalStateException`，需重新创建 |
| **空指针问题** | 终止操作（如 `max`、`findFirst`）返回 `Optional`，直接 `get()` 可能报 `NoSuchElementException`；建议用 `orElse()` 或 `ifPresent()` |
| **并行流滥用** | 并行流会带来线程安全问题，小数据量场景下串行流效率更高 |
| **map与flatMap混淆** | `map` 是"一对一"映射，`flatMap` 是"一对多"映射（将元素转为流再合并） |

---

## 六、实战建议

| 阶段 | 内容 | 目标 |
|------|------|------|
| **基础阶段** | 每天写1-2个简单案例（过滤集合、映射元素、统计数据），刻意用流替代传统for循环 | 熟悉常用API用法 |
| **进阶阶段** | 在小项目中灵活运用（处理接口返回的集合数据、筛选查询结果、统计业务数据），结合Lambda和方法引用 | 提升代码可读性和简洁性 |

流库是Java8以后开发的常用工具，掌握好流库能大幅提升集合操作的效率和代码质量，也是后续学习Java高级特性（如 Optional、函数式编程）的基础。
