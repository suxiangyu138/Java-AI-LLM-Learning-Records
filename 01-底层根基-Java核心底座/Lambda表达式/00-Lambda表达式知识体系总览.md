# Lambda 表达式知识体系总览

> 从匿名内部类到函数一等公民——Lambda 是 Java 8 最重要的语法与编程范式升级，也是 Stream、Optional、响应式编程与现代框架回调的共同底座

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 Lambda](#3-为什么必须学透-lambda)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Lambda 表达式知识体系
│
├── 01 Lambda 语法精讲与变量捕获
│   ├── 匿名内部类 → Lambda 演进
│   ├── 完整语法 / 简化规则 / 类型推断
│   ├── 目标类型（Target Typing）
│   ├── 局部变量捕获与 effectively final
│   ├── this / super 语义差异
│   └── 异常与返回值规则
│
├── 02 函数式接口全家桶
│   ├── @FunctionalInterface 本质
│   ├── 四大核心：Function / Predicate / Consumer / Supplier
│   ├── 二元与操作符：Bi* / UnaryOperator / BinaryOperator
│   ├── 原始类型特化：Int*/Long*/Double* / ToInt*
│   ├── 默认方法组合：andThen / compose / and / or / negate
│   └── 自定义函数式接口设计规范
│
├── 03 方法引用深度解析
│   ├── 静态方法引用 Class::staticMethod
│   ├── 实例方法引用 instance::method
│   ├── 类的任意对象方法 Class::instanceMethod
│   ├── 构造器引用 Class::new / 数组构造
│   ├── 何时用方法引用、何时用 Lambda
│   └── 歧义解析与常见编译错误
│
├── 04 Lambda 底层原理与字节码
│   ├── 不是匿名内部类：invokedynamic
│   ├── LambdaMetafactory 与 CallSite
│   ├── 捕获型 / 非捕获型 Lambda
│   ├── 反编译实战：javap -c -v / -p
│   ├── 序列化与 SerializedLambda
│   └── 性能对比：Lambda vs 匿名类 vs 方法引用
│
├── 05 Lambda 实战模式与 Stream 协同
│   ├── 策略 / 模板方法 / 责任链 / 观察者重构
│   ├── 延迟求值与日志/配置惰性加载
│   ├── 回调、事务模板、重试包装
│   ├── 与 Stream / Optional / CompletableFuture 配合
│   └── 业务代码重构清单
│
└── 06 Lambda 面试高频题精讲
    ├── 语法与捕获题
    ├── 函数式接口与方法引用题
    ├── 底层原理题（invokedynamic）
    ├── 性能与陷阱题
    └── 设计与重构综合题
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | Lambda 语法精讲与变量捕获 | 语法演进、目标类型、effectively final、this 语义 | 初中级必须掌握 | [01-Lambda语法精讲与变量捕获](./01-Lambda语法精讲与变量捕获.md) |
| 02 | 函数式接口全家桶 | java.util.function 全景、组合方法、自定义接口 | 初中级 → 中高级 | [02-Lambda函数式接口全家桶](./02-Lambda函数式接口全家桶.md) |
| 03 | 方法引用深度解析 | 四种引用、数组构造、选择策略与编译歧义 | 中级 | [03-Lambda方法引用深度解析](./03-Lambda方法引用深度解析.md) |
| 04 | 底层原理与字节码 | invokedynamic、LambdaMetafactory、捕获分类、性能 | 中高级 / 追求原理 | [04-Lambda底层原理与字节码](./04-Lambda底层原理与字节码.md) |
| 05 | 实战模式与 Stream 协同 | 设计模式重构、惰性求值、异步与流式 API | 中高级业务落地 | [05-Lambda实战模式与Stream协同](./05-Lambda实战模式与Stream协同.md) |
| 06 | 面试高频题精讲 | 20+ 高频题完整答法 + 追问 | 面试冲刺 | [06-Lambda面试高频题精讲](./06-Lambda面试高频题精讲.md) |

---

## 3. 为什么必须学透 Lambda

### 3.1 它不只是“写法更短”

| 维度 | 匿名内部类时代 | Lambda 时代 |
|------|---------------|-------------|
| 语法成本 | 样板代码多 | 表达式级回调 |
| 编程范式 | 面向对象为主 | 对象 + 函数式混合 |
| 集合处理 | for / Iterator | Stream 流水线 |
| 空安全 | 手动判空 | Optional + map/flatMap |
| 异步编排 | 回调地狱 | CompletableFuture 链式 |
| 框架扩展点 | 接口实现类 | 函数式接口注入 |

### 3.2 面试与工程双重高频

- **面试**：语法陷阱、effectively final、`this` 差异、是否生成 class 文件、`invokedynamic`、与 Stream 区别
- **工程**：业务策略表、校验器链、事务模板、日志惰性拼接、线程池任务提交、消息监听回调

### 3.3 一句话定位

> 🎯 **Lambda = 以函数式接口为目标类型的匿名函数语法糖 + 运行时 invokedynamic 动态链接**。学会写法只是入门，理解目标类型、捕获规则和底层链接机制，才能在重构与排错时不踩坑。

---

## 4. 核心概念速查

| 概念 | 一句话解释 | 详见 |
|------|-----------|------|
| 函数式接口 | 有且仅有一个抽象方法的接口（可含 default/static） | 02 |
| `@FunctionalInterface` | 编译期约束，不是运行时必需 | 02 |
| 目标类型 Target Type | Lambda 被赋值/传参时编译器推断出的函数式接口类型 | 01 |
| 类型推断 | 参数类型可从目标类型上下文省略 | 01 |
| effectively final | 未声明 final 但实际不再赋值的局部变量，可被捕获 | 01 |
| 方法引用 | Lambda 的“已有方法别名”写法 | 03 |
| `invokedynamic` | JVM 动态调用指令，Lambda 延迟链接实现类 | 04 |
| 捕获 / 非捕获 | 是否闭包外部变量，影响实例化与性能 | 04 |
| 四大接口 | Function / Predicate / Consumer / Supplier | 02 |
| 原始类型特化 | IntPredicate 等避免装箱 | 02 / 04 |

### 4.1 语法速记

```java
// 无参
() -> expression
() -> { statements; }

// 单参（括号可省）
x -> expression
(x) -> expression

// 多参
(a, b) -> expression
(Type a, Type b) -> { return expression; }
```

### 4.2 四大接口速记

```text
输入\输出     有返回值              无返回值
有输入        Function / Predicate  Consumer
无输入        Supplier              Runnable（JDK 自带）
```

---

## 5. 与周边知识的关系

```text
                    ┌──────────────────┐
                    │   函数式接口 API  │
                    │ java.util.function│
                    └────────┬─────────┘
                             │ 实现
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────────┐
        │  Lambda  │  │ 方法引用  │  │ 匿名内部类    │
        └────┬─────┘  └────┬─────┘  └──────────────┘
             │             │
             └──────┬──────┘
                    ▼
        ┌───────────────────────┐
        │ Stream / Optional /   │
        │ CompletableFuture /   │
        │ 自定义回调扩展点       │
        └───────────────────────┘
```

| 相关体系 | 关系 | 路径提示 |
|---------|------|---------|
| Java 高级语法 | 有精简版 Lambda 章节，本体系更深更全 | `../Java高级语法/01-Lambda表达式与函数式接口.md` |
| Stream API | Lambda 是 Stream 中间/终端操作的参数形态 | `../Java高级语法/02-Stream API深度实战.md` |
| 语法糖 | Lambda 属于“混合型”糖：部分解糖 + invokedynamic | `../Java语法糖/` |
| 泛型 | 函数式接口几乎全是泛型接口；桥方法/擦除影响签名 | `../泛型 反射 注解/` |
| JUC | `Runnable`/`Callable`/线程池任务常用 Lambda 提交 | `../02-JUC高并发编程/` |

---

## 6. 学习路线推荐

### 路线 A：一周速通（业务开发向）

1. **D1**：01 语法 + 变量捕获（能写、能解释 effectively final）
2. **D2**：02 四大接口 + 组合方法（能独立选型）
3. **D3**：03 方法引用（代码里优先 `::`）
4. **D4**：05 策略表 / 校验链 / 事务模板重构一个真实模块
5. **D5**：06 面试题过一遍 + 自己口述原理

### 路线 B：原理加深（中高级 / 面试向）

1. 先完成路线 A
2. 精读 **04 底层原理**：`javap` 反编译自己写的捕获/非捕获 Lambda
3. 对比匿名内部类生成的 `Outer$1.class` 与 Lambda 无额外 class 的差异
4. 结合 Stream 流水线理解惰性与无状态 Lambda 的性能含义
5. 准备 3 个“业务重构前后对比”故事用于项目深挖

### 路线 C：已有 Stream 基础的查漏补缺

1. 直接看 **01 捕获规则** + **04 invokedynamic**
2. 用 **02** 补齐 Bi* / 原始类型特化
3. **06** 当错题本

---

## 7. 快速自测 10 题

| # | 问题 | 自测要点 |
|:-:|------|---------|
| 1 | 函数式接口能否有多个 default 方法？ | 可以，抽象方法只能一个 |
| 2 | 为何局部变量必须 effectively final？ | 避免并发语义与实现模型复杂化 |
| 3 | Lambda 里 `this` 指谁？ | 外部类实例，不是 Lambda 自己 |
| 4 | `list.forEach(System.out::println)` 是哪种引用？ | 实例方法引用（PrintStream 实例） |
| 5 | Lambda 一定会生成 `Xxx$1.class` 吗？ | 不会，走 invokedynamic |
| 6 | `Function` 的 `compose` 与 `andThen` 顺序？ | compose 先执行参数函数 |
| 7 | 何时必须用原始类型特化接口？ | 大量基本类型计算，避免装箱 |
| 8 | 方法引用一定比 Lambda 快吗？ | 不一定，语义等价时通常相当 |
| 9 | 自定义函数式接口要注意什么？ | 单抽象方法、清晰语义、泛型边界、文档 |
| 10 | Stream 的 `map` 参数是什么接口？ | `Function<? super T, ? extends R>` |

> 💡 答不上来的题，优先回对应模块精读，不要只背结论。

---

## 版本与参考

| 项 | 说明 |
|----|------|
| 基线版本 | Java 8 引入；示例兼容 Java 8+，部分写法标注 11/17 |
| 核心包 | `java.util.function`、`java.lang.invoke` |
| 建议对照 | JLS §15.27 Lambda Expressions；`LambdaMetafactory` JavaDoc |
| 扩展阅读 | Effective Java 第 3 版 Item 42–44；Java 语言规范函数式接口章节 |

---

**下一模块**：[01-Lambda语法精讲与变量捕获](./01-Lambda语法精讲与变量捕获.md)

---

**返回上一级**：[../Java高级语法/](../Java高级语法/) | [../Java语法糖/](../Java语法糖/) | [../泛型 反射 注解/](../泛型%20反射%20注解/)
