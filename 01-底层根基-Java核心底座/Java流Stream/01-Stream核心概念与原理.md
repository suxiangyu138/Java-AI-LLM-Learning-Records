# 01 - Stream 核心概念与原理

> **核心摘要**：Stream 是元素序列的函数式处理管道——不存数据、只做变换。理解惰性求值、一次性、非侵入、可并行四大特性，是掌握 Stream 的第一课。本文从定义到执行机制，拆解 Stream 的本质。

> **前置阅读**：[[00-Java流Stream知识体系总览]]

---

## 📚 目录

1. [Stream 是什么](#1-stream-是什么)
2. [四大核心特性](#2-四大核心特性)
3. [惰性求值机制](#3-惰性求值机制)
4. [流生命周期与一次性](#4-流生命周期与一次性)
5. [中间操作 vs 终止操作](#5-中间操作-vs-终止操作)
6. [Stream vs 集合 vs 循环](#6-stream-vs-集合-vs-循环)
7. [Stream 的适用边界](#7-stream-的适用边界)
8. [核心要点](#8-核心要点)

---

## 1. Stream 是什么

> **背景**：Java 8（2014）引入 Stream——把集合数据处理从「命令式循环」升级为「函数式流水线」。
> **目的**：声明式描述「要做什么」而非「怎么做」。
> **适用范围**：集合/数组/文件的批量数据处理（过滤/转换/分组/聚合）。
> **不适用场景**：需要精确控制循环/下标/中断的场景；简单遍历（for 更直观）。

```text
Stream 的本质
├── ① 定义：元素序列的「函数式处理管道」
│   ├── 不是数据结构（不存储元素）
│   ├── 是「处理流程」的描述
│   └── 数据来源：集合/数组/生成器/文件
├── ② 类比：工厂流水线
│   ├── 原料（集合）→ 过滤工位 → 加工工位 → 包装（结果）
│   └── 每个工位 = 一个操作
└── ③ 金句：Stream 描述「数据怎么流动」——不是数据本身

一个 Stream 处理（对比命令式）
// 命令式（怎么做）
List<String> result = new ArrayList<>();
for (String s : list) {
    if (s.startsWith("A")) {
        result.add(s.toUpperCase());
    }
}

// Stream（要什么）
List<String> result = list.stream()
    .filter(s -> s.startsWith("A"))
    .map(String::toUpperCase)
    .toList();
// 声明式：过滤 + 转换——不写循环细节
```

---

## 2. 四大核心特性

> 🎯 **Stream 四大特性**——理解它们就理解 Stream 的行为边界：

| 特性 | 含义 | 影响 |
|------|------|------|
| **惰性求值** | 中间操作不执行，终止操作才触发 | 没 collect = 什么都没发生 |
| **非侵入** | 不修改数据源（集合不变） | filter/map 产生新流 |
| **一次性** | 流只能消费一次 | 二次终止操作抛异常 |
| **可并行** | parallelStream 利用多核 | ⚠️ 有陷阱（见 05 篇） |

```text
各特性的代码体现
// ① 惰性：下面什么都不执行（无终止操作）
list.stream().filter(x -> x > 10).map(x -> x * 2);
// ③ 一次性：
Stream<String> stream = list.stream();
stream.forEach(System.out::println);
stream.count();   // ❌ IllegalStateException（已消费）
// ② 非侵入：
list.stream().filter(x -> x > 10).toList();
// 原 list 不变（filter 产生新流）
```

---

## 3. 惰性求值机制

### 3.1 执行时序

> 🎯 **惰性求值**：中间操作只是「构建管道」，遇到终止操作才真正处理数据——**且是「逐元素」处理而非「分阶段」处理**：

```text
错误理解（分阶段）：
filter 全部 → map 全部 → limit（每阶段处理完整集合）

正确理解（逐元素流水线）：
每个元素：filter → map → limit 判定 → 下一个元素
├── 一个元素走完全部操作，才轮到下一个
├── limit(3)：找到 3 个符合的就停止（短路！）
└── 这就是短路操作高效的原因

示例（打印执行顺序）
Stream.of(1, 2, 3, 4, 5, 6)
    .filter(x -> { System.out.println("filter: " + x); return x % 2 == 0; })
    .map(x -> { System.out.println("map: " + x); return x * 10; })
    .limit(2)
    .forEach(System.out::println);
// 输出（注意顺序）：
// filter: 1  filter: 2  map: 2  filter: 3
// filter: 4  map: 4  （limit 2 已满足，5、6 不处理！）
// 20  40
```

### 3.2 惰性求值的价值

```text
惰性求值的好处
├── ① 短路优化：limit/findFirst 找到即停（不全量）
├── ② 无限流支持：Stream.iterate 无限生成 + limit 截断
├── ③ 资源节约：不处理多余元素
└── ④ 管道灵活：中间操作可复用（同一管道多个终止）

⚠️ 代价
├── ① 忘记终止操作 = 白写（什么都不执行）
├── ② 排障困难（看不到中间结果——用 peek 调试）
└── ③ 有状态操作（sorted）需全量（惰性失效）
```

---

## 4. 流生命周期与一次性

### 4.1 生命周期

```text
Stream 生命周期
├── ① 创建：从数据源构建（list.stream()）
├── ② 配置：中间操作（可多个——构建管道）
├── ③ 消费：终止操作（触发执行）
└── ④ 终结：流不可再用（一次性）

对比：集合可以反复遍历；Stream 只能消费一次
```

### 4.2 一次性陷阱

```java
// ❌ 错误：同一流消费两次
Stream<String> stream = list.stream();
stream.count();          // 第一次消费 ✅
stream.collect(toList()); // ❌ IllegalStateException: stream has already been operated upon

// ✅ 正确：需要多次操作 → 重新创建流（成本低）
long count = list.stream().count();
List<String> result = list.stream().filter(...).toList();

// ⚠️ 为什么设计成一次性
// ① 管道构建成本低（重新创建比复用便宜）
// ② 避免状态混乱（流有内部状态：位置/短路标记）
```

---

## 5. 中间操作 vs 终止操作

### 5.1 分类总览

| 类型 | 操作 | 返回 | 执行 |
|------|------|:---:|:---:|
| **中间操作** | filter/map/sorted/distinct/limit/skip/peek | 新 Stream | 惰性 |
| **终止操作** | collect/toList/forEach/count/reduce/findFirst/anyMatch | 结果 | 触发 |

```text
中间操作两类
├── 无状态：filter/map/peek（每个元素独立）
├── 有状态：sorted/distinct/limit/skip（需记录状态）
│   ├── ⚠️ sorted 需全量（惰性失效——全读完才排序）
│   └── ⚠️ 无限流上慎用有状态操作（永远等不完）

终止操作两类
├── 短路：findFirst/anyMatch/allMatch（找到即停）
├── 非短路：collect/forEach/count/reduce（全量处理）
```

### 5.2 操作组合原则

```text
操作顺序（影响性能）
├── ① 先过滤再转换：filter 前置（减少 map 工作量）
├── ② 先过滤再排序：sorted 有状态（先缩量）
├── ③ limit 尽量前置：短路减少处理量
└── ④ 金句：操作顺序 = 性能（把「缩量」放前面）

示例对比
list.stream().map(expensive).filter(...).limit(3)
// ❌ map 处理了全部元素（浪费）

list.stream().filter(...).limit(3).map(expensive)
// ✅ 只对 3 个元素做昂贵转换
```

---

## 6. Stream vs 集合 vs 循环

| 维度 | 传统循环 | 集合操作 | Stream |
|------|:---:|:---:|:---:|
| 表达 | 怎么做（命令式） | 数据存储 | 要什么（声明式） |
| 可读性 | 嵌套时差 | - | **复杂处理最佳** |
| 性能 | 简单场景好 | - | 简单场景略慢 |
| 惰性 | 无 | 无 | ✅ |
| 并行 | 手动 | 无 | **一行 parallel()** |
| 调试 | 直观 | 直观 | 需 peek |

```text
选型（2026 共识）
├── 简单遍历/赋值 → for（直观）
├── 复杂过滤/转换/分组 → Stream（可读性远超嵌套循环）
├── 大数据量无状态 → 并行流（评估后）
├── 需要下标/中断 → for
└── 金句：不为了 Stream 而 Stream——可读性优先
```

---

## 7. Stream 的适用边界

```text
✅ 适合
├── 集合过滤/转换/排序/去重
├── 分组/聚合统计（groupingBy/summarizing）
├── 扁平化（flatMap 多层结构）
├── 字符串拼接（joining）
├── 文件行处理（Files.lines）
└── 数据生成（Stream.iterate/generate）

❌ 不适合
├── 简单遍历（for 更直观）
├── 需要下标操作（Stream 无下标）
├── 需要精确中断（Stream 靠短路近似）
├── 修改数据源/外部状态（副作用——禁止）
├── 递归/图遍历（用算法）
└── IO 密集并行（用虚拟线程——见 05 篇）
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. Stream = 元素序列的函数式处理管道（不存数据只处理）
> 2. 四大特性：惰性求值/非侵入/一次性/可并行——**没 collect = 什么都没发生**
> 3. 惰性求值是逐元素流水线（非分阶段）——短路操作高效的原因
> 4. 流一次性：二次消费抛 IllegalStateException——需要就重新创建
> 5. 操作顺序 = 性能：filter/limit 前置、sorted 后置
> 6. 选型：复杂处理用 Stream、简单遍历用 for——**不为了 Stream 而 Stream**

---

**下一模块**：[02-创建流与中间操作](02-创建流与中间操作.md) | **返回总览**：[00-Java流Stream知识体系总览](00-Java流Stream知识体系总览.md)
