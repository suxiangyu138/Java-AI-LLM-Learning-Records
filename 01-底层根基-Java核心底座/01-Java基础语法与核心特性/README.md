# Java Stage 1: 语言基础 (Language Basics)

> **阶段目标**: 掌握 Java 语言的核心语法、面向对象思想、常用 API、异常处理与文件 I/O，具备独立编写企业级 Java 代码的能力。
>
> **预估时间**: 2-4 周（每天 2-4 小时）
>
> **前置要求**: 无（零基础可学，但具备任意编程语言基础效果更佳）

---

## 目录结构

| 文件 | 内容 | 建议学习时间 |
|------|------|-------------|
| [01-基础语法.md](./01-基础语法.md) | 程序结构、变量、运算符、控制流、数组、方法 | 5-7 天 |
| [02-面向对象.md](./02-面向对象.md) | 类与对象、封装/继承/多态、接口、内部类、枚举、Record、密封类 | 7-10 天 |
| [03-常用API与集合框架.md](./03-常用API与集合框架.md) | String、集合框架、Stream、Optional、时间日期 API | 5-7 天 |
| [04-异常机制.md](./04-异常机制.md) | 异常层次、Checked/Unchecked、try-catch、最佳实践 | 2-3 天 |
| [05-文件与IO.md](./05-文件与IO.md) | 字节/字符流、NIO.2、序列化、企业文件处理模式 | 3-5 天 |

---

## 前置条件检查清单

### 开发环境

- [ ] JDK 17+ 已安装（推荐 JDK 21 LTS）
- [ ] 确认 `java -version` 命令可用
- [ ] 确认 `javac -version` 命令可用
- [ ] IDE 已安装（推荐 IntelliJ IDEA Community/Ultimate 或 VS Code + Java Extension Pack）
- [ ] Maven 或 Gradle 已安装（可选，建议学完基础后配置）
- [ ] Git 已安装并完成基础配置

### 学习方式准备

- [ ] 每个代码示例都手动敲一遍，不要复制粘贴
- [ ] 准备好记录笔记的工具（Markdown、Notion 等）
- [ ] 理解"先思考再编码"的原则
- [ ] 准备好每天 30 分钟复习时间

---

## 学习路径（按周规划）

### 第 1 周：基础语法 + 面向对象基础

| 天 | 学习内容 | 实践任务 |
|----|---------|---------|
| Day 1 | Java 概述、环境搭建、第一个程序 | 编写 HelloWorld，理解编译运行流程 |
| Day 2 | 变量、数据类型、运算符 | 实现基本计算器 |
| Day 3 | 控制流（if/for/while） | 实现猜数字游戏、打印九九乘法表 |
| Day 4 | 数组、方法 | 实现数组排序、二分查找 |
| Day 5 | 类与对象、构造方法、this | 设计 Student 类 |
| Day 6 | 封装、访问修饰符、getter/setter | 完善 Student 类，添加验证逻辑 |
| Day 7 | 复习与综合练习 | 完成图书管理系统雏形 |

### 第 2 周：面向对象深入 + 常用 API

| 天 | 学习内容 | 实践任务 |
|----|---------|---------|
| Day 1 | 继承、super、方法重写 | 设计动物继承体系 |
| Day 2 | 多态、抽象类、接口 | 设计支付系统（支付宝/微信/银行卡） |
| Day 3 | 内部类、枚举、Record | 实现状态机、枚举策略模式 |
| Day 4 | String、StringBuilder、包装类 | 实现字符串处理工具类 |
| Day 5 | 集合框架（List、Set、Map） | 实现学生成绩管理系统 |
| Day 6 | Stream API、Optional | 用 Stream 重构成绩管理 |
| Day 7 | 复习与综合练习 | 完成员工管理系统 |

### 第 3 周：异常 + I/O + 综合项目

| 天 | 学习内容 | 实践任务 |
|----|---------|---------|
| Day 1 | 异常机制、try-catch-finally | 优化之前项目中的异常处理 |
| Day 2 | 自定义异常、try-with-resources | 实现带异常处理的配置文件读取 |
| Day 3 | 字节流、字符流 | 实现文件复制工具 |
| Day 4 | NIO.2、Files 工具类 | 实现目录遍历、文件搜索工具 |
| Day 5 | 序列化 | 实现对象的持久化存储 |
| Day 6 | 综合项目 | 完成日记本应用 |
| Day 7 | 复习与查漏补缺 | 刷面试题、整理笔记 |

### 第 4 周（可选）：巩固与扩展

- 刷 LeetCode 简单/中等难度题目（使用 Java）
- 阅读《Effective Java》前三章
- 阅读《阿里巴巴 Java 开发手册》基础部分
- 学习 JUnit 5，为之前的所有练习编写单元测试
- 学习 Maven/Gradle 基础，将项目改为标准构建

---

## 实践项目建议

按难度递增排列：

### 新手级 Project 1：控制台计算器
- 四则运算、支持括号
- 记录计算历史
- **覆盖知识点**：基础语法、控制流、数组

### 新手级 Project 2：学生成绩管理系统
- 添加/删除/修改/查询学生及成绩
- 统计平均分、最高/最低分、及格率
- 成绩排序（多种排序方式）
- **覆盖知识点**：数组/集合、面向对象、方法

### 进阶级 Project 3：ATM 模拟系统
- 账户类（账号、密码、余额、交易记录）
- 登录验证（最多 3 次尝试）
- 存款、取款、转账、查询余额
- 交易记录查看
- **覆盖知识点**：面向对象、集合、异常处理、日期 API

### 进阶级 Project 4：日记本应用
- 控制台日记编辑器
- 添加/查看/搜索/删除日记
- 支持标签分类
- 日记持久化（文件存储）
- 支持按日期范围搜索
- **覆盖知识点**：文件 I/O、异常处理、集合、Stream API、日期 API

### 挑战级 Project 5：迷你版学生选课系统
- 学生、教师、课程、选课记录
- 选课/退课（冲突检测）
- 成绩录入与统计
- 数据序列化存储
- **覆盖知识点**：全部 Stage 1 知识点

---

## 学习资源推荐

### 书籍
| 书名 | 作者 | 适合阶段 | 推荐指数 |
|------|------|---------|---------|
| 《Head First Java》 | Kathy Sierra & Bert Bates | 零基础入门 | ⭐⭐⭐⭐⭐ |
| 《Java 核心技术 卷 I》 | Cay S. Horstmann | 系统学习 | ⭐⭐⭐⭐⭐ |
| 《On Java 8》 | Bruce Eckel | 进阶提升 | ⭐⭐⭐⭐ |
| 《Effective Java 3rd》 | Joshua Bloch | 编程规范 | ⭐⭐⭐⭐⭐ |
| 《阿里巴巴 Java 开发手册》 | 阿里巴巴 | 企业规范 | ⭐⭐⭐⭐⭐ |

### 在线资源
- [Oracle Java Tutorials](https://docs.oracle.com/javase/tutorial/) - 官方教程
- [Java Language Specification](https://docs.oracle.com/javase/specs/) - 语言规范
- [Baeldung](https://www.baeldung.com/) - 高质量 Java 教程
- [Stack Overflow](https://stackoverflow.com/questions/tagged/java) - 问题解答
- [Java 全栈知识体系](https://pdai.tech/) - 中文 Java 知识体系

### 刷题平台
- [LeetCode](https://leetcode.com/) - 算法练习（Java 语言）
- [CodeWars](https://www.codewars.com/) - 编程挑战
- [牛客网](https://www.nowcoder.com/) - 国内面试题库

---

## 学习建议

1. **手敲代码**：看十遍不如写一遍，每个示例都亲自动手。
2. **先理解再记忆**：不要死记语法，理解设计理念（如为什么 Java 是 pass-by-value、为什么需要接口）。
3. **每日编码**：保持每天至少 1 小时的编码时间。
4. **写学习笔记**：用自己的话总结知识点，输出是最好的输入。
5. **善用 IDE**：学习 IntelliJ IDEA 的快捷键、调试功能、重构功能。
6. **阅读源码**：从 JDK 源码开始（String、ArrayList、HashMap 的源码），理解优秀代码的设计。
7. **提问的艺术**：先搜索、再思考、最后提问（推荐 Stack Overflow、GitHub Issues）。
8. **不要贪多**：每个知识点都掌握扎实再进入下一个，基础不牢地动山摇。

---

## 知识地图

```
                   ┌──────────────────────┐
                   │    Java 语言基础       │
                   └──────────┬───────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
         ┌────┴────┐    ┌────┴────┐    ┌────┴────┐
         │ 基础语法 │    │ 面向对象 │    │  常用API │
         │ (变量、  │    │ (类、   │    │ (集合、  │
         │  控制流) │    │  继承)  │    │  String) │
         └────┬────┘    └────┬────┘    └────┬────┘
              │               │               │
         ┌────┴────┐    ┌────┴────┐    ┌────┴────┐
         │ 异常机制 │    │ 文件I/O  │    │  Stream  │
         │ (try-   │    │ (NIO.2, │    │  (函数   │
         │  catch)  │    │  序列化) │    │  式编程) │
         └─────────┘    └─────────┘    └─────────┘
                              │
                   ┌──────────┴──────────┐
                   │  Stage 1 综合能力    │
                   │ (可进 Stage 2: JVM  │
                   │  并发编程 / 企业框架) │
                   └─────────────────────┘
```

---

## 版本说明

本文档基于 **Java 21 LTS** 编写，同时标注了各特性的引入版本。主要覆盖的 JDK 版本特性：

| JDK 版本 | 重要特性 |
|----------|---------|
| Java 8   | Lambda、Stream API、Optional、新的日期时间 API |
| Java 11  | `var` 用于 Lambda 参数、HTTP Client 标准化 |
| Java 14  | Record（预览）、Switch 表达式标准化、Instanceof 模式匹配（预览） |
| Java 16  | Record 标准化、Pattern Matching for instanceof |
| Java 17  | 密封类（Sealed Classes）标准化、Switch 模式匹配（预览） |
| Java 21  | Record Patterns、Pattern Matching for switch 标准化、虚拟线程 |

> **注意**：本文示例代码默认在 Java 17+ 环境下运行通过。低版本 JDK 可能不支持部分语法特性。

---

*"Any fool can write code that a computer can understand. Good programmers write code that humans can understand." — Martin Fowler*
