# 02 Java 核心语法快速 Demo

> 不是重新学一遍 Java——是用一个小 Demo 把仓库 01 层学过的核心语法激活一遍，并建立"写代码时能想到有哪些工具可用"的语法地图。目标是 3 天找回手感，不是 3 周重学语法。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [Demo 设计：订单统计器](#2-demo-设计订单统计器)
3. [核心语法点覆盖](#3-核心语法点覆盖)
4. [编码中检验：Stream 与集合](#4-编码中检验stream-与集合)
5. [异常与防御式编程](#5-异常与防御式编程)
6. [JDK 21+ 新语法尝鲜](#6-jdk-21-新语法尝鲜)
7. [核心要点](#7-核心要点)

---

## 1. 目标与验收

本 Demo 的产出：一个用纯 Java（无框架）实现的"订单统计器"——读取一批订单数据，按用户分组统计金额、过滤异常订单、输出 Top 5。验收标准：**不使用任何第三方库，全部用 JDK 自带 API 实现**；**能独立重写（不看代码）**；**每用一个 API 都能说清它解决什么问题**。最后一条是 Level1 的隐性要求：语法 Demo 不是背 API，是建立"遇到问题 → 想到哪个 API"的映射。

语法地图先建立整体认知：Java 核心语法按用途分五组——**类型与结构**（基本类型、包装类、泛型、接口、枚举）、**集合**（List/Set/Map 及实现类）、**函数式**（Lambda、Stream、Optional）、**异常**（受检/非受检、try-with-resources）、**新语法**（record、sealed、switch 表达式）。本 Demo 逐组覆盖，每组只写最能体现该组能力的代码。

## 2. Demo 设计：订单统计器

先设计再编码——用一句话描述功能，拆成三步：**读入**（硬编码一批订单作为数据源，Demo 阶段不接数据库）→ **处理**（过滤无效订单、按用户分组求和、排序）→ **输出**（打印 Top 5 用户与金额）。

```java
record Order(Long userId, String product, BigDecimal amount, boolean valid) {}

var orders = List.of(
    new Order(1L, "AI课", new BigDecimal("299.00"), true),
    new Order(1L, "Java课", new BigDecimal("199.00"), true),
    new Order(2L, "AI课", new BigDecimal("299.00"), false),  // 无效订单
    new Order(3L, "面试课", new BigDecimal("499.00"), true));

var top5 = orders.stream()
    .filter(Order::valid)                          // 过滤无效
    .collect(Collectors.groupingBy(Order::userId,  // 按用户分组
        Collectors.mapping(Order::amount,
            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
    .entrySet().stream()
    .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
    .limit(5)
    .toList();
```

这段代码同时覆盖：record（数据载体）、List.of（不可变集合）、var（局部类型推断）、Stream 全链路（filter/collect/sorted/limit/toList）、方法引用、BigDecimal（金额必须用，浮点算钱是事故）。写完这段，Java 语法的"现代形态"就建立了——**老教程里的 for 循环 + HashMap 手写分组，在 JDK 21+ 里十行内解决**。

## 3. 核心语法点覆盖

**面向对象四件**：接口（定义能力契约）、实现类（JDK 自带实现优先）、枚举（状态值用枚举不用魔法数字——订单状态 `PENDING/PAID/REFUNDED` 是 Level2 的高频设计）、泛型（`List<Order>` 比裸 List 安全）。**集合选型**是面试高频：List 用 ArrayList（查多）、LinkedList（插入删除多但实测性能优势场景少）、Map 用 HashMap（默认）、需要顺序用 LinkedHashMap、线程安全用 ConcurrentHashMap——选型理由不是背的，是场景驱动的。

**函数式三件**：Lambda（匿名函数语法糖）、方法引用（`Order::valid` 等价于 `o -> o.valid()`）、Optional（消灭空指针的首选方案——`orders.stream().findFirst().ifPresentOrElse(...)` 比 `if (x != null)` 链优雅得多）。这三件是"现代 Java 代码长什么样"的答案，Level2 的代码里到处是它们。

## 4. 编码中检验：Stream 与集合

写 Stream 时对照"中间操作与终端操作"的认知模型：中间操作（filter/map/sorted/limit）是惰性的，不触发执行；终端操作（collect/forEach/toList/count）才真正遍历——理解这一点才能解释"为什么 Stream 链不写终端操作不报错也不执行"。调试 Stream 链的两个技巧：**把链拆开打印**（每步 `.peek(System.out::println)` 观察元素变化，用完删除）；**先跑小数据集**（用 3 条数据验证逻辑，再上全量）。常见的 Stream 坑：**collect 与 toList 混用**（JDK 16+ 直接 `.toList()` 即可，`Collectors.toList()` 是旧写法）；**并行流乱用**（`parallelStream()` 对含共享状态的代码会出竞态，Demo 阶段一律不用，面试被问"为什么不用"的答案就是"并行有竞态风险，数据量没到阈值"）。

集合相关的两个高频面试点顺手练掉：**HashMap 的 put 流程**（hash → 定位桶 → 链表/红黑树 → 扩容，红黑树阈值 8）与**ArrayList 扩容**（1.5 倍，初始 10）——这两个原理是本 Demo 代码背后"为什么快/为什么慢"的答案，能讲出原理是 Level1 语法阶段的隐藏要求。

**包装类与字符串的三个陷阱**一起排掉：**Integer 缓存**——`Integer a = 127; Integer b = 127; a == b` 为 true，但 `128 == 128` 为 false（Integer 缓存 -128~127），**对象比较永远用 equals 不用 ==**，这是面试连环坑的第一问；**字符串拼接**——循环里 `s += x` 每次创建新对象（String 不可变），循环量大用 `StringBuilder`；**BigDecimal 构造**——`new BigDecimal(0.1)` 是脏值（浮点二进制不精确），要用 `new BigDecimal("0.1")` 字符串构造——本 Demo 的金额运算已经用对了方式，知道为什么对才算真懂。

**面向对象的"三件小事"**也顺手练掉：**接口默认方法**（`default` 关键字——接口里给默认实现，List.sort 就是接口默认方法的实际应用）；**record 与不可变**（record 的字段全是 final，天然线程安全——理解"不可变 = 并发安全"这个等价关系，是 JMM 体系的入口）；**enum 用法的两个场景**（状态枚举 `OrderStatus.PAID` 与策略枚举——枚举里带行为方法 `status.canRefund()` 是 Level2 的常用模式）。这三件小事不新，但它们是"面向对象落到工程里长什么样"的具体样本——本 Demo 的 Order record 与 valid 布尔字段，就是这三个模式的雏形。

## 5. 异常与防御式编程

Demo 里加一段防御式处理：解析用户输入金额时可能抛 `NumberFormatException`，用受检与非受检的差异体现异常设计——**业务可恢复用受检异常（或返回 Optional/Result），编程错误用非受检**。现代 Java 的实践倾向：少用受检异常，用 Optional 或自定义 Result 类型表达"可能失败"。

```java
BigDecimal parseAmount(String raw) {
    if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
    try {
        return new BigDecimal(raw);
    } catch (NumberFormatException e) {
        return BigDecimal.ZERO;   // 解析失败降级为 0，不抛出
    }
}
```

两个要点：**try-with-resources** 必须会用（文件/网络资源自动关闭，Level1 写文件、Level2 连数据库都用它）；**异常吞掉与转换的区别**——`catch` 里什么都不做是吞（事故源头），`catch` 后返回降级值或包装抛出是转换（工程做法）。Demo 阶段的代码也要保持"每个 catch 都有明确行为"的习惯。

## 6. JDK 21+ 新语法尝鲜

Level1 建议主动用三个新语法，它们会让代码质量上一个台阶且面试可讲：**record**（不可变数据载体，替代手写 POJO 的 getter/setter，本 Demo 已用）；**switch 表达式**（带返回值、箭头语法，替代旧的 break 写法）；**文本块**（`"""` 三引号字符串，写 SQL 与提示词模板的利器，Level1 的 06/07 Demo 会大量用到）。三个语法都是"预览转正"的稳定特性（JDK 21 已转正），放心使用。不要碰的：还在预览期的新特性（结构化并发 JEP 525 等），Demo 阶段没必要为尝鲜承担版本风险。

## 7. 核心要点

1. 语法 Demo 的目的是建立"问题 → API"映射，不是背 API 清单。
2. 现代 Java 形态：record + var + Stream + Optional，老教程写法只作理解用。
3. Stream 认知模型：中间操作惰性、终端操作触发；调试用 peek 拆链。
4. HashMap put 流程与 ArrayList 扩容是必讲原理，顺手练掉。
5. 异常设计：可恢复用 Optional/Result，catch 必须有明确行为，try-with-resources 必会。

> 🎯 **核心要点**：本 Demo 真正的产出是一份"手感"——看到数据要分组统计，手自动伸向 Stream；看到可能为空的返回值，手自动伸向 Optional。这种手感是 Level2 写业务代码时效率的保证，语法本身只是载体。

---

**下一模块**：[03 Spring Boot 入门 Demo](./03-Spring%20Boot%20入门%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
