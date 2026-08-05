# Java 异常体系知识体系总览

> 从 Throwable 层级到受检/非受检的设计哲学，从 try-catch-finally 的执行机制到 try-with-resources 的资源管理——异常处理是 Java 工程质量的试金石

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透异常体系](#3-为什么必须学透异常体系)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Java 异常体系知识体系
│
├── 01 异常体系结构与核心类
│   ├── Throwable 顶层与两大分支：Error / Exception
│   ├── Error：JVM 级错误，为什么不捕获
│   ├── RuntimeException 家族与常见异常速查
│   ├── 异常对象的构成：message / stackTrace / cause / suppressed
│   └── 判断"受检/非受检"的黄金标准
│
├── 02 受检异常与非受检异常
│   ├── 编译器检查机制与设计目的
│   ├── 受检异常三大弊端与 Spring 转向运行时
│   ├── 2025 年仍在持续的学术之争：类型安全 vs 样板代码
│   ├── Lambda / Stream 与受检异常的冲突
│   └── 新 API 设计共识：默认 RuntimeException
│
├── 03 try-catch-finally 执行机制
│   ├── 完整执行流程与三种分支
│   ├── return 与 finally 的交互：返回值快照机制
│   ├── finally 不执行的三种特殊情况
│   ├── catch 顺序：子类在前、父类在后
│   └── 经典面试题：finally 中的 return
│
├── 04 try-with-resources 与资源管理
│   ├── JDK 7 语法与编译器展开原理
│   ├── AutoCloseable vs Closeable
│   ├── 逆序关闭与 suppressed 抑制异常
│   ├── JDK 9 增强：引用外部资源变量
│   └── 2025 动态：Process 拟实现 AutoCloseable（JDK-8364361）
│
├── 05 异常传播与捕获策略
│   ├── 异常沿调用栈传播的机制
│   ├── multi-catch 多异常捕获（JDK 7+）
│   ├── 精确重抛与类型推断
│   ├── 包装与异常链：cause 的正确传递
│   └── 转换边界：何时包装、何时透传
│
├── 06 自定义异常与错误码设计
│   ├── 自定义异常规范：命名、继承、构造器
│   ├── 错误码设计：枚举、分段、文档化
│   ├── 按业务模块拆分的异常家族
│   ├── 全局异常处理器（@RestControllerAdvice）
│   └── 错误响应契约：错误码 + 消息 + 追踪 ID
│
├── 07 异常与并发、异步
│   ├── 线程中的异常去向与 UncaughtExceptionHandler
│   ├── 线程池任务异常的三种处理
│   ├── CompletableFuture 的异常管道
│   ├── 虚拟线程（JDK 21）的异常传播
│   └── 结构化并发：StructuredTaskScope 与 FailedException（JDK 25）
│
├── 08 异常处理最佳实践与反模式
│   ├── 十种反模式清单（吞异常/捕获 Throwable/…）
│   ├── 异常性能：创建开销与热点路径优化
│   ├── 日志纪律：一次记录、带上下文
│   ├── 现代工具：精准 NPE 消息（JDK 15+）/ StackWalker / 模式匹配
│   └── 与阿里巴巴 Java 开发手册规约衔接
│
└── 09 面试高频考点与总结
    ├── 必背考点：Error vs Exception / finally / TWR / 受检
    ├── 高频陷阱题 10 连问
    ├── 场景题：全局异常体系设计
    └── 记忆口诀与进阶导航
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | 异常体系结构与核心类 | Throwable 层级、Error/Exception、核心异常速查 | 初中级必须掌握 | [01-异常体系结构与核心类](./01-异常体系结构与核心类.md) |
| 02 | 受检异常与非受检异常 | 检查机制、三大弊端、设计之争、Lambda 冲突 | 初中级必须掌握 | [02-受检异常与非受检异常](./02-受检异常与非受检异常.md) |
| 03 | try-catch-finally 执行机制 | 流程、return 交互、finally 边界 | 初中级必须掌握 | [03-try-catch-finally执行机制](./03-try-catch-finally执行机制.md) |
| 04 | try-with-resources 与资源管理 | TWR 原理、逆序关闭、suppressed | 初中级必须掌握 | [04-try-with-resources与资源管理](./04-try-with-resources与资源管理.md) |
| 05 | 异常传播与捕获策略 | 传播机制、multi-catch、异常链 | 初中级 → 中高级 | [05-异常传播与捕获策略](./05-异常传播与捕获策略.md) |
| 06 | 自定义异常与错误码设计 | 规范、错误码、全局处理器 | 中高级 | [06-自定义异常与错误码设计](./06-自定义异常与错误码设计.md) |
| 07 | 异常与并发、异步 | 线程异常、线程池、异步管道、虚拟线程 | 中高级 | [07-异常与并发异步](./07-异常与并发异步.md) |
| 08 | 异常处理最佳实践与反模式 | 反模式清单、性能、日志纪律 | 中高级 | [08-异常处理最佳实践与反模式](./08-异常处理最佳实践与反模式.md) |
| 09 | 面试高频考点与总结 | 必背考点、陷阱题、场景题 | 面试冲刺 | [09-面试高频考点与总结](./09-面试高频考点与总结.md) |

---

## 3. 为什么必须学透异常体系

1. **代码里最常见的"隐性分支"**：异常不是"出错才用"，而是方法返回值的第三种形态（正常值 / 错误码 / 异常）——不懂异常的执行机制，`finally` 与 `return` 的交互、资源泄漏、吞异常等问题会反复咬人。
2. **面试必考的执行机制题**：`finally` 中写 return 会发生什么？try-with-resources 的关闭顺序？受检 vs 非受检怎么选？——这些题的区分度极高，答错直接暴露"只会用、不懂原理"。
3. **工程质量的试金石**：异常处理直接决定线上可排查性——错误码是否规范、日志是否重复、堆栈是否保留、资源是否泄漏。**"看一个项目先看它的异常处理"是资深开发者的共识**。
4. **与并发/异步深度绑定**：线程池里任务抛异常会怎样？异步回调的异常怎么传播？虚拟线程（JDK 21）改变了异常传播模型——这是新一代并发编程的必修课。
5. **仍在前沿演进**：从 JDK 7 的 try-with-resources、JDK 15 的精准 NPE 消息，到 JDK 25 结构化并发的 `FailedException` 与受检异常之争论——异常体系不是"老知识"，而是活语言的一部分。

---

## 4. 核心概念速查

### 4.1 异常体系层级（黄金判断标准）

```text
Throwable
├── Error                     —— JVM/系统级，不捕获
│     ├── OutOfMemoryError / StackOverflowError / NoClassDefFoundError ...
└── Exception                 —— 应用级
      ├── RuntimeException    —— 非受检（unchecked）：编译器不强制
      │     ├── NPE / AIOOBE / CCE / IllegalArgumentException ...
      └── 其他 Exception      —— 受检（checked）：编译器强制处理
            ├── IOException / SQLException / ClassNotFoundException ...
```

**判断标准**：一个异常是受检还是非受检 → 看它是否继承自 `RuntimeException`（Error 除外）。

### 4.2 异常对象四要素

| 要素 | 含义 | 获取 |
|------|------|------|
| message | 描述信息 | `getMessage()` |
| stackTrace | 堆栈轨迹（默认创建时填充，性能大头） | `getStackTrace()` / `StackWalker`（JDK 9+） |
| cause | 底层原因链（包装时保留） | `getCause()` / `initCause()` |
| suppressed | 被抑制的异常（try-with-resources 关闭失败） | `getSuppressed()`（JDK 7+） |

### 4.3 关键机制速查

| 机制 | 一句话 | 版本 |
|------|--------|:----:|
| try-with-resources | 资源自动关闭，编译器展开为 finally | JDK 7 |
| multi-catch | 一个 catch 捕获多个异常类型 | JDK 7 |
| 精确重抛 | rethrow 时编译器推断实际类型 | JDK 7 |
| TWR 引用外部变量 | 资源可声明在 try 外（effectively final） | JDK 9 |
| 精准 NPE 消息 | NPE 自动指出"哪个字段为 null" | JDK 15（默认开启） |
| 虚拟线程异常路由 | 未捕获异常不杀载体线程，走 handler | JDK 21 |
| 结构化并发 FailedException | `StructuredTaskScope.join()` 的失败聚合 | JDK 25 |

---

## 5. 与周边知识的关系

```text
                    ┌── Java代码规范/05-异常处理与日志规范 —— 阿里规约落地
                    ├── 并发（JUC）—— 线程池/异步的异常传播
Java 异常体系 ──────┼── Spring —— @RestControllerAdvice 全局异常处理
                    ├── IO/NIO —— 资源管理与 TWR 的主战场
                    ├── 面向对象 —— 自定义异常的继承设计
                    └── 日志框架（SLF4J/Logback）—— 异常记录纪律
```

- **往下走（原理）**：异常对象的创建涉及 JVM 栈帧填充（性能开销）、`Thread` 的异常分发机制、虚拟线程的异常路由。
- **往旁走（规范）**：`Java代码规范/05-异常处理与日志规范.md` 从阿里巴巴开发手册角度给出规约级要求——本体系讲机制与设计，那边讲纪律与检查项。
- **往高走（架构）**：全局异常处理器、错误码契约、错误响应结构是 API 设计的组成部分——异常体系是"前后端契约"的一环。

---

## 6. 学习路线推荐

**路线一：入门夯实（2 天，对应模块 01-04）**
层级结构 → 受检/非受检 → try-catch-finally 执行机制 → try-with-resources；每个机制手写代码验证执行顺序，重点吃透 return 与 finally 的交互。

**路线二：进阶深化（3 天，对应模块 05-08）**
异常传播与包装 → 自定义异常与错误码 → 并发异步异常 → 最佳实践与反模式；用 Spring Boot 搭一个全局异常处理器 + 错误码体系练手。

**路线三：面试冲刺（对应模块 09）**
背考点 → 自测陷阱题 → 场景设计题（设计一个完整的异常体系）；结合 `Java代码规范/05-异常处理与日志规范.md` 过一遍规约。

> 🎯 **核心要点**：异常体系的学习终点不是"会 try-catch"，而是**"让异常成为可控的信息流"**——该捕获的捕获、该包装的包装、该记录的记录、该透传的透传，每一处都是设计决策。

---

## 7. 快速自测 10 题

1. `Error` 和 `Exception` 的本质区别？为什么不应该捕获 Error？
2. 判断标准：怎么快速判定一个异常是受检还是非受检？
3. try 块中 return 了，finally 还会执行吗？finally 修改返回值会生效吗？
4. finally 中写 return 会怎样？为什么绝对禁止？
5. `System.exit(0)` 与 `return` 对 finally 的影响有何不同？
6. try-with-resources 中资源按什么顺序关闭？如果 try 块和 close() 都抛异常，谁会被保留？
7. `Closeable` 和 `AutoCloseable` 什么关系？
8. 为什么 Spring/MyBatis 普遍用 RuntimeException 而不是受检异常？
9. 线程池中任务抛出异常会怎样？怎么拿到异常？
10. `catch (Exception | IOException e)` 能编译吗？为什么？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**下一模块**：[01-异常体系结构与核心类](./01-异常体系结构与核心类.md)
