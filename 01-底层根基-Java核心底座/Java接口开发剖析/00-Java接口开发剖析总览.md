# Java 接口开发剖析总览

> 接口是 Java 的"契约艺术"——从 JDK 1.0 的纯抽象到 JDK 21 的密封接口 + 模式匹配，接口已从"能做什么"进化到"应该怎么设计"

## 📚 目录

1. [知识体系导图](#1)
2. [接口演进时间线](#2)
3. [模块导航](#3)
4. [核心概念速查](#4)
5. [学习路线推荐](#5)

---

## 1. 知识体系导图 {#1}

```
Java 接口开发剖析
│
├── 01 接口基础与演进之路
│   ├── 接口的本质：契约 (Contract)
│   ├── 接口基础语法
│   │   ├── 抽象方法 / 常量字段
│   │   ├── 接口的多实现 (implements)
│   │   └── 接口的多继承 (extends)
│   ├── JDK 8: 默认方法 (default method)
│   │   ├── 向后兼容的接口演化
│   │   ├── 菱形继承问题与解决规则
│   │   └── 静态方法 (static method)
│   ├── JDK 9: 私有方法 (private method)
│   │   ├── default 方法间代码复用
│   │   └── 未来：私有字段？(OpenJDK 讨论中)
│   ├── JDK 17: 密封接口 (sealed interface)
│   │   ├── permits 子句
│   │   ├── final / sealed / non-sealed
│   │   └── 封闭类型体系
│   └── JDK 21: 模式匹配 switch + 密封接口
│       ├── 穷尽性检查 (exhaustiveness)
│       └── 无需 default 分支
│
├── 02 接口与抽象类深度对决
│   ├── 语法差异全景表
│   ├── 语义差异
│   │   ├── "是什么" vs "能做什么"
│   │   ├── 状态持有 vs 纯契约
│   │   └── 单继承 vs 多实现
│   ├── 场景选型决策树
│   │   ├── 接口优先原则 (ISP)
│   │   ├── 何时必须用抽象类
│   │   └── 现代替代：sealed interface + record
│   ├── JDK 演进对选型的影响
│   │   ├── 默认方法是否模糊了界限？
│   │   └── sealed interface 减少抽象类使用
│   └── 混合模式：接口 + 骨架实现类
│
├── 03 函数式接口与 Lambda
│   ├── @FunctionalInterface 注解
│   │   ├── 单一抽象方法 (SAM) 契约
│   │   ├── 编译器验证机制
│   │   └── 常见反模式
│   ├── java.util.function 四大金刚
│   │   ├── Function<T,R> / BiFunction
│   │   ├── Consumer<T> / BiConsumer
│   │   ├── Predicate<T> / BiPredicate
│   │   └── Supplier<T>
│   ├── 扩展函数式接口
│   │   ├── UnaryOperator / BinaryOperator
│   │   ├── Int/Long/Double 特化接口
│   │   └── 自定义函数式接口
│   ├── 函数式接口组合
│   │   ├── andThen / compose (Function)
│   │   ├── and / or / negate (Predicate)
│   │   └── 高阶函数模式
│   └── Lambda 的方法引用
│       ├── ClassName::staticMethod
│       ├── instance::instanceMethod
│       └── ClassName::new (构造器引用)
│
├── 04 接口设计原则与契约
│   ├── SOLID 中的接口原则
│   │   ├── ISP (接口隔离原则) 详解
│   │   ├── 胖接口的危害
│   │   └── 角色接口 (Role Interface)
│   ├── Effective Java 接口设计黄金法则
│   │   ├── 为后代设计接口 (#21)
│   │   ├── 接口只定义类型 (#22)
│   │   ├── 层次结构优先于标签类 (#23)
│   │   └── 优先考虑静态成员类 (#24)
│   ├── 接口演化与兼容性
│   │   ├── 源兼容 / 二进制兼容 / 行为兼容
│   │   ├── 默认方法的安全添加
│   │   └── @Deprecated 与迁移路径
│   ├── 命名与文档规范
│   │   ├── 接口命名模式 (-able, -Handler, -Service)
│   │   ├── Javadoc 契约文档
│   │   └── @implSpec / @implNote
│   └── 接口测试策略
│       ├── 接口契约测试 (Contract Test)
│       └── 基于接口的 Mock/Stub
│
└── 05 接口实战模式与案例
    ├── 策略模式 (Strategy Pattern)
    │   ├── 接口定义算法族
    │   ├── Lambda 简化策略
    │   └── 实战：支付策略 / 折扣计算
    ├── 工厂模式 (Factory Pattern)
    │   ├── 简单工厂 / 工厂方法 / 抽象工厂
    │   └── 函数式工厂 (Supplier)
    ├── SPI (Service Provider Interface)
    │   ├── Java SPI 机制 (ServiceLoader)
    │   ├── JDBC Driver / SLF4J 案例
    │   └── Spring Boot 自动配置与 SPI
    ├── 模板方法 vs 策略模式
    │   ├── 抽象类模板方法 (继承)
    │   └── 接口策略 (组合) —— 优先组合
    ├── 适配器模式
    │   ├── 接口转接
    │   └── 默认方法实现适配器
    └── sealed interface + record 实现代数数据类型
        ├── Either<L,R> / Result<T,E>
        ├── AST 节点建模
        └── 状态机建模
```

---

## 2. 接口演进时间线 {#2}

```
JDK 1.0 ─── 纯抽象方法 + 常量字段
    │        interface 诞生：定义契约，实现多态
    │
JDK 1.1 ─── 内部类 (Inner Class)
    │        接口与匿名内部类组合使用
    │
JDK 5  ─── 泛型 (Generics)
    │        interface Comparable<T>, Collection<T>
    │
JDK 8  ─── 默认方法 + 静态方法 + 函数式接口 ★ 里程碑
    │        default / static 方法
    │        @FunctionalInterface + Lambda
    │        Stream API 基于函数式接口
    │
JDK 9  ─── 私有方法
    │        default 方法间共享代码
    │        接口模块化 (module-info.java)
    │
JDK 14 ─── Records (Preview)
    │        接口 + record = 轻量数据载体
    │
JDK 17 ─── 密封接口 (Sealed Interface) ★ 里程碑
    │        sealed / permits
    │        封闭类型层次
    │
JDK 21 ─── 模式匹配 switch
            密封接口 + switch 穷尽性检查
            无需 default 分支
```

---

## 3. 模块导航 {#3}

| 序号 | 模块 | 核心内容 | 适合人群 | 前置要求 |
|:---:|------|---------|---------|---------|
| 01 | 接口基础与演进之路 | 语法、default/static/private/sealed、菱形问题、演化策略 | 初中级 Java 开发者 | Java 基础语法 |
| 02 | 接口与抽象类深度对决 | 语法/语义差异、选型决策树、sealed 替代、骨架实现类 | 中级开发者 | 接口 + 抽象类基础 |
| 03 | 函数式接口与 Lambda | @FunctionalInterface、四大核心 FI、组合模式、方法引用 | 中级开发者 | Lambda 基础 |
| 04 | 接口设计原则与契约 | ISP、Effective Java 法则、演化兼容性、契约测试 | 中高级开发者 | 接口 + 设计经验 |
| 05 | 接口实战模式与案例 | 策略、工厂、SPI、适配器、sealed+record 案例 | 中高级开发者 | 设计模式基础 |

---

## 4. 核心概念速查 {#4}

| 概念 | 一句话解释 | 模块 |
|------|-----------|:---:|
| **接口 (interface)** | 定义行为契约的抽象类型，不含状态 | 01 |
| **默认方法 (default)** | JDK 8+，接口中带方法体，向后兼容演化 | 01 |
| **功能性接口 (SAM)** | 只有一个抽象方法的接口，可用 Lambda 表达 | 03 |
| **密封接口 (sealed)** | JDK 17+，限制哪些类可以实现此接口 | 01 |
| **ISP** | 接口隔离原则：不应强迫实现者依赖不使用的方法 | 04 |
| **SPI** | 服务提供者接口：框架定义接口，第三方实现 | 05 |
| **菱形继承** | 两个接口有同名默认方法时的冲突及解决规则 | 01 |
| **骨架实现** | AbstractXxx 类，简化接口实现的模板 | 02 |
| **方法引用** | `ClassName::method`，Lambda 的简写形式 | 03 |
| **契约测试** | 验证接口的所有实现都满足同一份契约 | 04 |
| **穷尽性** | sealed + switch 编译器确保覆盖所有子类型 | 01 |

---

## 5. 学习路线推荐 {#5}

### 🟢 初级：理解接口是什么（1 周）

```
接口语法基础 → 接口 vs 抽象类 → @FunctionalInterface → 常用内置接口
```

### 🟡 中级：用好接口做设计（2-3 周）

```
ISP 原则 → default 方法演化 → 策略/工厂/适配器 → SPI 机制
```

### 🔴 高级：掌握接口演化与架构（3-4 周）

```
sealed interface + record → 模式匹配 → 契约测试 → 框架级接口设计
```

---

**下一模块：** [01-接口基础与演进之路](./01-接口基础与演进之路.md)
