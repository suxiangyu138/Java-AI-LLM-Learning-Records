# Java API 知识体系总览

> Java 标准库是一个"层次分明的超大型工具箱"——掌握核心 API 的类层次、设计模式与惯用法，是写出简洁高效 Java 代码的基础

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须系统学 Java API](#3-为什么必须系统学-java-api)
4. [核心概念速查](#4-核心概念速查)
5. [学习路线推荐](#5-学习路线推荐)
6. [快速自测 10 题](#6-快速自测-10-题)
7. [与周边知识的关系](#7-与周边知识的关系)

---

## 1. 知识体系导图

```text
Java API 知识体系
│
├── 01 java.lang 核心 API
│   ├── Object：equals / hashCode / toString / clone / finalize
│   ├── String：不可变性 / 常量池 / intern / 常用方法全景
│   ├── StringBuilder / StringBuffer：可变字符串与线程安全
│   ├── 包装类：自动装箱拆箱 / 缓存机制 / valueOf vs new
│   ├── System / Runtime：系统交互 / 环境变量 / 资源管理
│   └── Math / StrictMath：数学运算 / 精确度差异
│
├── 02 Java 集合框架
│   ├── Collection 接口体系：List / Set / Queue 分支
│   ├── Map 接口体系：HashMap / TreeMap / LinkedHashMap
│   ├── ArrayList vs LinkedList：底层结构与选型
│   ├── HashSet / TreeSet / LinkedHashSet：唯一性与有序性
│   ├── fail-fast vs fail-safe：迭代器并发行为
│   ├── Comparable vs Comparator：自然序与定制序
│   └── Collections / Arrays 工具类：排序 / 查找 / 同步包装
│
├── 03 Java I/O 体系
│   ├── 装饰器模式在 I/O 中的应用
│   ├── 字节流：InputStream / OutputStream 族
│   ├── 字符流：Reader / Writer 族
│   ├── 缓冲流与转换流：BufferedReader / InputStreamReader
│   ├── try-with-resources：AutoCloseable 机制
│   ├── File 与 NIO.2 Path / Files：现代文件操作
│   └── 序列化基础：Serializable / Externalizable
│
├── 04 Java 时间日期 API
│   ├── 旧 API 之痛：Date / Calendar / SimpleDateFormat
│   ├── java.time 核心类：LocalDate / LocalTime / LocalDateTime
│   ├── 瞬时与时区：Instant / ZonedDateTime / ZoneId
│   ├── 时间段：Duration / Period
│   ├── 格式化和解析：DateTimeFormatter
│   └── 新旧 API 互转与最佳实践
│
├── 05 Optional 与防御性编程
│   ├── 创建 Optional：of / ofNullable / empty
│   ├── 消费与转换：map / flatMap / filter / ifPresent
│   ├── 兜底策略：orElse / orElseGet / orElseThrow / or
│   ├── 错误使用模式与性能陷阱
│   └── Optional 在 API 设计中的定位
│
└── 06 Java 常用工具类大全
    ├── Objects：null 安全 equals / requireNonNull / checkIndex
    ├── Arrays：排序 / 二分查找 / 填充 / 拷贝 / 比较
    ├── Collections：排序 / 洗牌 / 旋转 / 不可变集合
    ├── Random / ThreadLocalRandom：随机数生成
    ├── Scanner：输入解析
    └── Properties / ResourceBundle：配置与国际化
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | java.lang 核心 API | Object 契约、String 内存模型、包装类缓存、System | 所有 Java 开发者 | [01-java.lang核心API深度解析](./01-java.lang核心API深度解析.md) |
| 02 | Java 集合框架 | Collection/Map 体系、HashMap 原理、排序与选型 | 中级 | [02-Java集合框架原理与实战](./02-Java集合框架原理与实战.md) |
| 03 | Java I/O 体系 | 装饰器模式、字节/字符流、NIO.2 Files、AutoCloseable | 中级 | [03-Java IO与NIO体系全解](./03-Java%20IO与NIO体系全解.md) |
| 04 | Java 时间日期 API | java.time 全家桶、DateTimeFormatter、时区处理 | 所有开发者 | [04-Java时间日期API深度解析](./04-Java时间日期API深度解析.md) |
| 05 | Optional 与防御性编程 | 链式空安全、flatMap、orElseGet 陷阱、最佳实践 | 中级 | [05-Optional与防御性编程实战](./05-Optional与防御性编程实战.md) |
| 06 | 常用工具类大全 | Objects / Arrays / Collections / Scanner / Properties | 初中级 | [06-Java常用工具类大全](./06-Java常用工具类大全.md) |

---

## 3. 为什么必须系统学 Java API

### 3.1 不学 API 的后果

| 现象 | 根因 | 对应模块 |
|------|------|---------|
| 写 `if (str != null && !str.equals(""))` | 不知道 `isBlank()` / `isEmpty()` | 01 / 05 |
| 遍历 Map 先 `keySet` 再 `get` | 不知道 `entrySet()` 一趟遍历 | 02 |
| 用 `FileInputStream` 逐字节读文本 | 不知道 `BufferedReader` | 03 |
| `new Date(year, month, day)` 算不对月份 | 不知道 `java.time.LocalDate` | 04 |
| `optional.get()` 不检查 `isPresent()` | 不理解 Optional 的设计意图 | 05 |
| 手写数组拷贝 / 排序 | 不知道 `Arrays.copyOf` / `Arrays.sort` | 06 |

### 3.2 一句话定位

> 🎯 **Java API = 标准库提供的类和方法集合**。学会知道"什么类在哪个包里能做什么事"，遇到需求时能第一时间从标准库找现成工具，而不是自己写。API 功底直接决定代码的简洁度和排错效率。

---

## 4. 核心概念速查

| 概念 | 一句话解释 | 详见 |
|------|-----------|------|
| `equals` / `hashCode` 契约 | 相等的对象必须有相同哈希码；集合正确性的基石 | 01 |
| String 不可变性 | 任何"修改"操作都返回新对象；常量池复用 | 01 |
| 自动装箱缓存 | `Integer.valueOf(127) == Integer.valueOf(127)` 为 true，128 则否 | 01 |
| 集合 vs Map | Collection 存单值，Map 存键值对 | 02 |
| fail-fast | 迭代中结构被修改立即抛 `ConcurrentModificationException` | 02 |
| 装饰器模式 | 包一层加功能：`new BufferedReader(new FileReader(f))` | 03 |
| try-with-resources | `try (InputStream in = ...) { }` 自动调用 `close()` | 03 |
| 不可变日期 | `LocalDate.plusDays(1)` 返回新对象，原对象不变 | 04 |
| `orElse` vs `orElseGet` | 前者参数总是求值，后者延迟求值 | 05 |
| `Objects.requireNonNull` | 一行做空检查和抛出 NPE | 06 |

---

## 5. 学习路线推荐

### 路线 A：新手快速上手（1 周）

1. **D1**：01 java.lang — Object 四个方法 + String 常用 API + 包装类装箱拆箱
2. **D2**：02 集合 — ArrayList、HashMap、forEach、排序
3. **D3**：03 I/O — 文件读写的标准模板（try-with-resources + BufferedReader/BufferedWriter）
4. **D4**：04 日期 — LocalDate / LocalDateTime + DateTimeFormatter
5. **D5**：05 Optional — 用 Optional 重构 5 处空判断 + 06 工具类 — 按需查阅

### 路线 B：系统补齐（中高级，2 周）

1. 重新走一遍 01，重点关注 `equals/hashCode` 契约与 `intern()` 底层
2. 精读 02：`HashMap` 的 hash 扰动函数与红黑树化阈值；`ArrayList` 扩容策略
3. 03 深入：`ByteBuffer`、`MappedByteBuffer`、`FileVisitor` 递归遍历
4. 04 深入：`ZonedDateTime` 的夏令时边界行为、`TemporalAdjuster` 自定义
5. 05 深入：`flatMap` 组合多个 Optional、反模式清单
6. 用 06 清理项目中的手写工具方法

### 路线 C：面试突击（3 天）

1. 01 从头到尾过一遍（`equals/hashCode` 源码级别理解是高频）
2. 02 重点：HashMap put 流程、ArrayList vs LinkedList、ConcurrentModificationException
3. 跳跃阅读 03-04，记住核心类名和方法名
4. 05-06 当参考手册，回答"如何安全处理 null"直接引用 Optional + Objects

---

## 6. 快速自测 10 题

| # | 问题 | 自测要点 |
|:-:|------|---------|
| 1 | `String s = new String("abc")` 创建了几个对象？ | 考虑常量池 + 堆，可能是 1 个或 2 个 |
| 2 | `Integer a = 127; Integer b = 127; a == b` 结果？128 呢？ | 自动装箱缓存范围 -128 ~ 127 |
| 3 | `HashMap` 的默认初始容量和负载因子是多少？ | 16 和 0.75 |
| 4 | `ArrayList` 无参构造创建的是空数组还是容量为 10？ | JDK 8 起是空数组，首次 add 才扩容到 10 |
| 5 | `FileReader` 能指定编码吗？不能的话怎么办？ | 不能，用 `new InputStreamReader(new FileInputStream(f), charset)` |
| 6 | `LocalDate` 修改日期用 `setXxx` 吗？ | 没有 setter，用 `plusDays` / `withMonth` 等返回新对象 |
| 7 | `Optional.get()` 如果为空会抛什么异常？ | `NoSuchElementException` |
| 8 | `orElse` 和 `orElseGet` 的区别？ | `orElse` 参数是值（先求值），`orElseGet` 是 Supplier（延迟） |
| 9 | `Arrays.asList(...)` 返回的 List 能 add 吗？ | 不能，是固定大小的内部类 ArrayList（不是 java.util.ArrayList） |
| 10 | `try-with-resources` 需要接口是什么？ | `AutoCloseable`（`Closeable` 是其子接口） |

> 💡 答不上来的题，优先回对应模块精读，不要只背结论。

---

## 7. 与周边知识的关系

```text
                     ┌──────────────────────┐
                     │      Java API        │
                     │  java.lang / util    │
                     │  java.io / nio       │
                     │  java.time           │
                     └──────┬───────────────┘
                            │
          ┌─────────────────┼─────────────────┐
          ▼                 ▼                  ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────────┐
│ 集合框架 + Stream │ │  I/O → 网络  │ │ 时间 → 定时任务  │
│ → Stream API     │ │ → Netty/     │ │ → Quartz/       │
│ → 数据结构算法    │ │   HttpClient │ │   Scheduled      │
└─────────────────┘ └──────────────┘ └──────────────────┘
```

| 相关体系 | 关系 | 路径提示 |
|---------|------|---------|
| Java 基础语法 | 语法是"怎么写"，API 是"用什么写" | `../01-Java基础语法与核心特性/` |
| Java 高级语法 | Stream / Lambda / Optional / 泛型 | `../Java高级语法/` |
| Lambda 表达式 | 函数式接口是 Lambda 的目标类型 | `../Lambda表达式/` |
| JUC 高并发 | `java.util.concurrent` 是集合/线程池的并发扩展 | `../02-JUC高并发编程/` |
| 数据结构与算法 | API 是数据结构的标准实现 | `../06-数据结构与算法/` |
| Spring 框架 | Spring 大量重用 Java 标准 API（Resource、类型转换等） | `../../02-后端中间件.../Spring/` |

---

## 版本与参考

| 项 | 说明 |
|----|------|
| 基线版本 | Java 8 引入核心 API；部分标注 Java 9/11/17+ 增强 |
| 核心包 | `java.lang`、`java.util`、`java.io`、`java.nio`、`java.time` |
| 建议对照 | JDK 源码（`src.zip`）；Effective Java 第 3 版 |
| 扩展阅读 | Java Language Specification；《Java 核心技术》卷 I/II |

---

**下一模块**：[01-java.lang核心API深度解析](./01-java.lang核心API深度解析.md)
