# 01 JDK 版本演进与选型指南

> "你项目用 Java 几？为什么？"——JDK 版本选型是每个项目的第一道技术决策

## 📚 目录

1. [JDK 版本历史全景](#1)
2. [LTS 版本深度对比](#2)
3. [JDK 8→11→17→21 迁移实战](#3)
4. [JDK 21-25 新特性速览](#4)
5. [企业 JDK 选型决策框架](#5)
6. [OpenJDK vs Oracle JDK vs 其他发行版](#6)
7. [面试标准话术](#7)

---

## 1. JDK 版本历史全景 {#1}

### 1.1 发展时间线

JDK (Java Development Kit) 自 1996 年发布至今已近 30 年，经历了从企业级开发语言到全栈生态平台的演进：

```
JDK 1.0 ── 1996 ── Oak → Java, Applet 时代开启
    │
JDK 1.1 ── 1997 ── 内部类、JDBC、RMI、反射、JAR
    │
JDK 1.2 ── 1998 ── "Java 2" 品牌, 集合框架, Swing, JIT, JFC
    │
JDK 1.3 ── 2000 ── HotSpot VM 成为默认, JNDI, 动态代理
    │
JDK 1.4 ── 2002 ── assert, NIO, 正则, 日志, 异常链, IPv6
    │
JDK 5 ──── 2004 ── "里程碑": 泛型, 注解, 枚举, 自动装箱, foreach, 可变参数, java.util.concurrent
    │
JDK 6 ──── 2006 ── 脚本引擎, 编译器 API, JConsole, 优化 VM
    │                    Oracle 收购 Sun (2009)
JDK 7 ──── 2011 ── 钻石语法, try-with-resources, NIO.2, Fork/Join, invokedynamic
    │
JDK 8 ──── 2014 ── "最经典 LTS": Lambda, Stream, Optional, 新日期 API, Metaspace, Nashorn
    │                    (6年后才出 JDK 9)
JDK 9 ──── 2017 ── 模块化 (JPMS), REPL (jshell), 私有多版本 JAR, G1 默认 GC
    │
JDK 10 ─── 2018 ── 局部变量类型推断 (var), 并行全 GC (G1)
    │
JDK 11 ─── 2018 ── "LTS 转折": HTTP Client 稳定, ZGC (实验), Nest-Based Access, String.repeat()
    │                     Oracle 开始商业收费, OpenJDK 成为官方参考实现
JDK 12 ─── 2019 ── Shenandoah (实验), Switch 表达式 (预览), 微基准测试套件
    │
JDK 13 ─── 2019 ── 文本块 (预览), 动态 CDS 归档, ZGC 增强
    │
JDK 14 ─── 2020 ── Records (预览), Pattern Matching for instanceof (预览), 空指针精准提示
    │
JDK 15 ─── 2020 ── 文本块 (稳定), Sealed Class (预览), Shenandoah 稳定, EdDSA
    │
JDK 16 ─── 2021 ── Record (稳定), Pattern Matching for instanceof (稳定), Unix-Domain Socket
    │
JDK 17 ─── 2021 ── "新世代 LTS": Sealed Class 稳定, Vector API (孵化), 增强伪随机数, 弃用 Applet
    │
JDK 18 ─── 2022 ── UTF-8 默认, 简单 Web 服务器, 方法句柄优化
    │
JDK 19 ─── 2022 ── Virtual Threads (预览), Record Patterns (预览), Linux/RISC-V
    │
JDK 20 ─── 2023 ── Scoped Values (孵化), Record Patterns 二次预览, Structured Concurrency (孵化)
    │
JDK 21 ─── 2023 ── "重大 LTS": Virtual Threads 稳定, Record Patterns 稳定, String Templates (预览),
    │                     Sequenced Collections, Pattern Matching for switch 稳定
JDK 22 ─── 2024 ── Stream Gatherers (预览), 未命名变量, Implicit Classes
    │
JDK 23 ─── 2024 ── Module Import (预览), Markdown 文档注释, ZGC 分代模式默认
    │
JDK 24 ─── 2025 ── Scoped Values (预览), Structured Concurrency (预览), Class File API (预览)
    │
JDK 25 ─── 2025 ── "最新 LTS": 稳定版 ZGC 分代, 稳定版 Stream Gatherers, Vector API 最终状态
```

### 1.2 关键里程碑特性

| 里程碑 | 版本 | 意义 |
|--------|:----:|------|
| 诞生 | JDK 1.0 | Java 正式面世，提出 "Write Once, Run Anywhere" |
| 企业级 | JDK 1.2 | 集合框架奠定了数据结构的基石 |
| 语法革命 | JDK 5 | 泛型、注解、枚举、并发包——至今仍是日常主力 |
| 函数式 | JDK 8 | Lambda + Stream 改变了 Java 的编程范式 |
| 模块化 | JDK 9 | JPMS 解决了 Jar Hell，但迁移成本巨大 |
| 新 LTS 纪元 | JDK 11 | Oracle JDK 收费，社区全面转向 OpenJDK |
| 现代 LTS | JDK 17 | 语言特性加速：Record、Sealed Class、Pattern Matching |
| 并发革命 | JDK 21 | Virtual Threads 改变了 Java 的并发编程模型 |
| 未来 LTS | JDK 25 | 分代 ZGC、Stream Gatherers 进入稳定 |

> 💡 **版本发布节奏**：JDK 1.0-8 采用"大版本 + 小更新"模式，周期 2-3 年。从 JDK 9 开始采用 **每 6 个月一个特性版本**、**每 3 年一个 LTS 版本** 的时间驱动模式，确保新特性快速交付。

---

## 2. LTS 版本深度对比 {#2}

### 2.1 四大主流 LTS 总览

| 对比维度 | JDK 8 | JDK 11 | JDK 17 | JDK 21 |
|---------|:-----:|:------:|:------:|:------:|
| 发布时间 | 2014.03 | 2018.09 | 2021.09 | 2023.09 |
| 免费更新截止 | 2030.12 (Oracle) | 2032.01 | 2029.09 | 2031.09 |
| 是否仍推荐 | 不推荐新项目 | 过渡用 | 主流推荐 | 强烈推荐 |
| 编程范式 | OOP + 初试函数式 | 函数式增强 | 现代 OOP + 函数式 | 并发革命 |
| GC 默认 | Parallel | G1 | G1 | G1 (可配 ZGC) |
| 核心语法 | Lambda, Stream | var (局部类型推断) | Record, Sealed Class | Virtual Threads |
| 模块化 | 无 | JPMS (Jigsaw) | 完善 | 成熟 |
| 启动性能 | 较慢 | 有 CDS 加速 | 更好 | 最优 |
| 安全更新 | 仍提供 (付费) | 仍提供 | 提供中 | 提供中 |

### 2.2 JDK 8 → 11 关键升级

| 新特性 | 说明 | 代码示例 |
|--------|------|---------|
| `var` 局部变量类型推断 | 简化局部变量声明，类型由编译器推断 | `var list = new ArrayList<String>();` |
| HTTP Client (稳定) | 替代 `HttpURLConnection`，支持 HTTP/2、WebSocket | `HttpClient.newHttpClient().send(request, BodyHandlers.ofString())` |
| 集合工厂方法增强 | `List.of()`、`Set.of()`、`Map.of()` 不变集合 | `List.of("a", "b", "c")` |
| `String` 新增方法 | `isBlank()`、`lines()`、`repeat()`、`strip()` | `"  ".isBlank()` → `true` |
| `Files` 增强 | `readString()`、`writeString()` 简化文件 I/O | `Files.readString(Path.of("file.txt"))` |
| Nest-Based Access | 嵌套类可访问私有成员，减少桥接方法 | 编译器优化，对开发者透明 |
| TLS 1.3 | 默认启用 TLS 1.3 协议 | 安全增强 |
| Flight Recorder | JFR 开源化，可用于生产监控 | `-XX:StartFlightRecording` |

### 2.3 JDK 11 → 17 关键升级

| 新特性 | 说明 | 代码示例 |
|--------|------|---------|
| Records | 不可变数据载体，自动生成构造器、equals、hashCode、toString | `record Point(int x, int y) {}` |
| Sealed Class | 限制继承/实现的子类集合 | `sealed interface Shape permits Circle, Square {}` |
| Pattern Matching for instanceof | 类型检查后自动转型 | `if (obj instanceof String s) { s.length(); }` |
| Switch 表达式 | 箭头语法、表达式返回值 | `int num = switch(day) { case MON -> 1; ... };` |
| 文本块 (Text Blocks) | 多行字符串字面量，无需转义 | `""" <html>..."""` |
| 增强伪随机数 (JEP 356) | 新的 `RandomGenerator` 接口 | `RandomGenerator.getDefault().nextInt()` |
| Vector API (孵化) | 向量计算，利用 CPU SIMD 指令 | 通过 `FloatVector` 加速数值计算 |
| 密封的反射 (JEP 403) | 限制反射访问内部 API | 安全增强 |

### 2.4 JDK 17 → 21 关键升级

| 新特性 | 说明 | 受益场景 |
|--------|------|---------|
| Virtual Threads (虚拟线程) | 轻量级线程，百万级并发 | Web 服务、微服务、IO密集型应用 |
| Record Patterns | 解构 Record，嵌套模式匹配 | 数据处理、函数式编程 |
| Pattern Matching for switch | switch 可匹配类型、null、模式 | 减少 if-else 链 |
| Sequenced Collections | 有序集合统一接口 | 数据结构操作统一化 |
| String Templates (预览) | 字符串插值 | 安全构建字符串和 SQL |
| Scoped Values (孵化) | 不可变线程局部变量替代 ThreadLocal | 框架级上下文传递 |
| Structured Concurrency (孵化) | 结构化并发，错误传播和取消 | 多任务编排 |
| ZGC 分代模式 (实验) | 新一代 ZGC，性能提升 | 大内存应用 |

### 2.5 迁移收益总结

```
升级路径              性能收益              开发效率收益              运维收益
──────────────────────────────────────────────────────────────────────────────
JDK 8 → 11      GC 提升 (G1 增强)     var, 集合工厂, HTTP Client   安全修复
                启动加速 (CDS)         Stream 增强                  TLS 1.3
                容器感知改进           String 增强                  JFR 生产可用

JDK 11 → 17     向量计算 (Vector API)  Record, Sealed Class        长期安全支持
                改进的 ZGC (实验)       Switch 表达式, 文本块        弃用不安全 API
                空指针提示             Pattern Matching              容器集成增强

JDK 17 → 21     虚拟线程 (IO 密集型)   Virtual Threads 模型改变      最新 LTS 支持
                分代 ZGC (实验)         Record Patterns              安全更新至 2031
                Foreign Function &     Pattern Matching for switch   现代语言特性
                Memory API (预览)      Sequenced Collections
```

> 🎯 **核心要点**：JDK 8→17 的升级带来约 **15-30% 的性能提升**（通过 GC 改进、JIT 优化），JDK 17→21 的升级虽然单体计算性能提升有限（约 5-10%），但 **Virtual Threads 改变了并发编程模型**，对于 IO 密集型服务可带来数倍的吞吐量提升和代码简化。

---

## 3. JDK 8→11→17→21 迁移实战 {#3}

### 3.1 移除和废弃的 API

从 JDK 8 迁移到高版本时，以下 API 已经被移除或废弃：

| 被移除/废弃的 API | 移除版本 | 替代方案 |
|-------------------|:--------:|---------|
| `javax.xml.bind` (JAXB) | JDK 11 | 独立 Maven 依赖 `jakarta.xml.bind` |
| `javax.activation` | JDK 11 | 独立依赖 `jakarta.activation` |
| `javax.transaction` (JTA) | JDK 11 | 独立依赖 `jakarta.transaction` |
| `javax.xml.ws` (JAX-WS) | JDK 11 | 独立依赖 `jakarta.xml.ws` |
| `javax.xml.soap` (SAAJ) | JDK 11 | 独立依赖 `jakarta.xml.soap` |
| `CORBA` / `org.omg.CORBA` | JDK 11 | 无替代，协议已过时 |
| `java.xml.ws.annotation` (JSR 250) | JDK 11 | 独立依赖 `jakarta.annotation` |
| `javafx.*` (JavaFX) | JDK 11 | 独立 SDK (OpenJFX) |
| `java.se.ee` 聚合模块 | JDK 9 | 按需使用独立模块 |
| `Nashorn` JavaScript 引擎 | JDK 15 (JDK 11 已废弃) | GraalVM JavaScript |
| `ObjectOutputStream.PutField.write(ObjectOutput)` | JDK 11 | 使用 `writeUnshared` |
| `Thread.destroy()` / `Thread.stop(Throwable)` | — | 早已不推荐 |
| `finalize()` | JDK 18 (标记废弃) | Cleaner / 显式资源管理 |
| `SecurityManager` | JDK 18 (标记废弃) | 按需使用替代安全机制 |

### 3.2 Maven 迁移配置

**JDK 8 → 11 依赖调整示例**：

```xml
<!-- JDK 8 的 JAXB 在 JDK 11+ 需要额外引入 -->
<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <version>4.0.2</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>4.0.5</version>
</dependency>

<!-- Annotation API (javax.annotation → jakarta.annotation) -->
<dependency>
    <groupId>jakarta.annotation</groupId>
    <artifactId>jakarta.annotation-api</artifactId>
    <version>2.1.1</version>
</dependency>
```

**Maven compiler plugin 配置**：

```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

> ⚠️ **`maven.compiler.release` 比 `source` + `target` 更推荐设置**，因为它会限制编译器只使用目标版本中的 API，防止误用新版 API 导致运行时错误。

### 3.3 模块化 (JPMS) 迁移要点

JDK 9 引入 Java Platform Module System (JPMS)，当迁移到 JDK 9+ 时需要考虑：

1. **`module-info.java`**：如果启用模块化，需要在 `src/main/java` 根目录创建模块描述文件
2. **非法反射访问**：JDK 16+ 默认不允许 strong encapsulation，使用 `--add-opens` 参数
3. **常用 JVM 参数**：

```bash
# 放宽模块化访问限制（临时解决方案）
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED

# 列出模块依赖关系
jdeps --module-path target/classes -s myapp.jar
```

**模块化迁移策略**：

| 策略 | 适用场景 | 实施难度 |
|------|---------|:-------:|
| 不迁移 (不加 module-info) | 简单应用，JDK 9+ 兼容 | 低 |
| 不迁移 + 添加 `--add-opens` | Spring Boot 等框架应用需要反射 | 中 |
| 仅主模块加 module-info | 有清晰模块边界的项目 | 中高 |
| 全模块化 | 新项目或已设计模块化的老项目 | 高 |

> 💡 **Spring Boot 2.x/3.x 对 JDK 17+ 的兼容性已经非常成熟**，多数情况下只需升级 Spring Boot 版本和修改依赖即可完成迁移。

### 3.4 GC 迁移注意点

| 迁移动作 | 注意事项 |
|---------|---------|
| JDK 8 → 11 | GC 默认从 Parallel 变为 G1，需重新评估 GC 参数配置 |
| JDK 11 → 17 | G1 持续优化，`-XX:G1HeapRegionSize` 等参数行为有变化 |
| JDK 17 → 21 | ZGC 进入生产可用 (JDK 15+) 且性能大提升，可考虑切换 |
| JDK 21 → 25 | 分代 ZGC 成为默认，Shenandoah 持续增强 |

**迁移后 GC 参数检查清单**：

```bash
# 检查当前 GC
java -XX:+PrintCommandLineFlags -version

# 推荐 JDK 17+ 的通用 G1 配置
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:ParallelGCThreads=4
-XX:ConcGCThreads=2
-XX:G1HeapRegionSize=4m
-XX:+UseStringDeduplication
-Xlog:gc*:file=gc.log:time,uptime,level,tags
```

### 3.5 Jakarta EE 迁移

从 JDK 8 升级到 JDK 11+，javax.* 包名迁移到 jakarta.*：

```text
javax.servlet.*       → jakarta.servlet.*
javax.persistence.*   → jakarta.persistence.*
javax.validation.*    → jakarta.validation.*
javax.transaction.*   → jakarta.transaction.*
javax.annotation.*    → jakarta.annotation.*
```

**版本对应关系**：

| 框架 | JDK 8 版本 | JDK 17+ 版本 |
|------|-----------|-------------|
| Spring Boot | 2.x | 3.x (JDK 17+) |
| Tomcat | 9.x | 10.x+ |
| Hibernate | 5.x | 6.x |
| Jetty | 9.x | 11.x+ |

> ⚠️ **Spring Boot 3.0+ 需要 JDK 17+ 且使用 Jakarta EE 9+**，Spring Boot 2.x 仍使用 javax.*。这是从 Spring Boot 2 升级到 3 的最大迁移成本。

---

## 4. JDK 21-25 新特性速览 {#4}

### 4.1 Virtual Threads (虚拟线程) — JDK 21 稳定

**原理**：虚拟线程是 JVM 管理的轻量级线程，由 Project Loom 孵化而来。与传统平台线程 (Platform Thread) 1:1 映射到 OS 线程不同，虚拟线程是 M:N 映射——M 个虚拟线程复用到 N 个 Carrier 线程上。

```
传统线程模型 (Platform Threads):
    Java Thread ──1:1──→ OS Thread ──→ CPU Core

虚拟线程模型 (Virtual Threads):
    Virtual Thread ──M:N──→ Carrier Thread (ForkJoinPool) ──→ OS Thread ──→ CPU Core
```

**核心机制**：当虚拟线程执行阻塞 I/O 操作时 (如 `socket.read()`、`Thread.sleep()`、`Lock.lock()`)，JVM 会 **挂起 (yield)** 该虚拟线程，将 Carrier 线程释放给其他虚拟线程使用，I/O 就绪后再恢复执行。这解决了传统线程模型中"大量线程等待 I/O 浪费 OS 资源"的问题。

**代码示例**：

```java
// 传统方式 —— 每个请求一个平台线程，受限于 OS 线程数
ExecutorService executor = Executors.newFixedThreadPool(200);
for (int i = 0; i < 10000; i++) {
    executor.submit(() -> {
        // 模拟 IO 操作，会阻塞平台线程
        Thread.sleep(100);
        return process();
    });
}

// 虚拟线程方式 —— 百万级并发，IO 等待不阻塞 Carrier 线程
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100000; i++) {
        executor.submit(() -> {
            Thread.sleep(100);  // 不会阻塞平台线程
            return process();
        });
    }
}
```

**适用场景**：

| 适合 | 不适合 |
|------|--------|
| IO 密集型服务 (Web 服务、数据库访问、RPC) | CPU 密集型计算 (加密、图像处理) |
| 高并发且任务短暂 | 长时间占用 CPU 的任务 |
| Microservices 每个请求需要独立上下文 | Pinned 场景 (synchronized 块内执行 IO) |

> 💡 **Virtual Threads 使用建议**：
> - 避免在虚拟线程中使用 `synchronized` (会导致 pinned)，使用 `ReentrantLock` 替代
> - 不要池化虚拟线程——它们是轻量的，每次 new 即可
> - `ThreadLocal` 依然可用但需注意数量，虚拟线程数量巨大可能导致内存问题
> - 考虑使用 `Scoped Values` (JDK 24 预览) 替代 `ThreadLocal` 作为更安全的选择

### 4.2 Record Patterns — JDK 21 稳定

Record Patterns 允许在模式匹配中解构 Record，支持嵌套匹配：

```java
// 定义 Record
record Address(String city, String street) {}
record Person(String name, int age, Address address) {}

// 传统写法
if (obj instanceof Person) {
    Person p = (Person) obj;
    Address a = p.address();
    System.out.println(p.name() + " lives in " + a.city());
}

// Record Patterns 写法 (JDK 21+)
if (obj instanceof Person(String name, int age, Address(String city, String street))) {
    System.out.println(name + " lives in " + city);
}

// 配合 switch 使用
String info = switch (obj) {
    case Person(String name, int age, Address(var city, var street))
        when age >= 18 -> name + " (adult) in " + city;
    case Person(String name, int age, _) -> name + " (minor)";
    case null -> "null";
    default -> "Unknown";
};
```

### 4.3 Pattern Matching for switch — JDK 21 稳定

switch 现在支持类型匹配、null 守卫和 guard 条件：

```java
// JDK 21+ 增强的 switch
String formatted = switch (obj) {
    case Integer i && i > 0 -> "Positive integer: " + i;
    case Integer i -> "Non-positive integer: " + i;
    case String s && !s.isEmpty() -> "Non-empty string: " + s;
    case null -> "It's null!";
    default -> "Unknown type: " + obj.getClass().getSimpleName();
};

// Enum 支持 exhaustive 匹配
enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

int numLetters = switch (day) {
    case MON, FRI, SUN -> 3;   // 逗号分隔多个值
    case TUE -> 3;
    case WED -> 3;
    case THU -> 3;
    case SAT -> 4;
    // 无需 default，enum 已穷举
};
```

### 4.4 Sealed Class — JDK 17 稳定

Sealed Class 限定了哪些类或接口可以继承/实现它：

```java
// 定义密封接口，仅允许三个子类
sealed interface Vehicle permits Car, Truck, Motorcycle {}

// 子类可以是 final、sealed 或 non-sealed
final class Car implements Vehicle {}
sealed class Truck implements Vehicle permits DumpTruck, BoxTruck {}
final class DumpTruck extends Truck {}
final class BoxTruck extends Truck {}
non-sealed class Motorcycle implements Vehicle {}

// 与 Pattern Matching 配合实现穷举检查
String describe(Vehicle v) {
    return switch (v) {
        case Car c -> "Car with " + c.getSeats() + " seats";
        case Truck t -> "Truck - " + t.getCapacity() + " tons";
        case Motorcycle m -> "Motorcycle";
        // 编译器知道这里已经穷举，无需 default
    };
}
```

### 4.5 Sequenced Collections — JDK 21 稳定

统一了有序集合的接口层次：

```java
// 之前：获取第一个/最后一个元素的方式不统一
List<String> list = List.of("a", "b", "c");
list.get(0);                    // 第一个
list.get(list.size() - 1);      // 最后一个

Deque<String> deque = new ArrayDeque<>();
deque.getFirst();               // Deque 方式
deque.getLast();

// JDK 21+：统一接口
SequencedCollection<String> seq = list;
seq.getFirst();                 // "a"
seq.getLast();                  // "c"

SequencedSet<String> orderedSet = new LinkedHashSet<>();
orderedSet.addFirst("first");   // 插入到开头
orderedSet.addLast("last");     // 插入到末尾
orderedSet.reversed();          // 反转视图
```

### 4.6 Scoped Values — JDK 24 预览 (JDK 25 可望稳定)

Scoped Values 旨在替代使用场景下的 ThreadLocal，提供不可变、可继承的上下文传递：

```java
// 定义 Scoped Value (类似 ThreadLocal 但不可变)
final static ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

// 绑定值并执行代码块
ScopedValue.where(REQUEST_ID, "req-12345")
           .run(() -> {
               // 在作用域内可以读取
               String id = REQUEST_ID.get();  // "req-12345"
               // 不可以修改 (ScopedValue 是不可变的)
               handleRequest();
           });

// 在 handleRequest 和整个调用链中，REQUEST_ID 都可以读取
// 但无法被修改——提升了安全性和可预测性
```

**ScopedValues vs ThreadLocal**：

| 特性 | ThreadLocal | ScopedValues |
|------|:----------:|:------------:|
| 可变性 | 可读写 | 只读 (绑定后不可变) |
| 生命周期 | 线程 -> GC 回收 | Scoped 作用域 |
| 继承 (子线程) | InheritableThreadLocal | 自动继承 |
| 虚拟线程友好 | 内存占用大 (每个 VT 持有) | 轻量 (共享引用) |
| 安全性 | 任意代码可修改 | 外部不可修改 |

### 4.7 Structured Concurrency — JDK 24 预览

结构化并发将并发任务的执行纳入一个 **结构化作用域**，确保所有子任务在作用域退出前完成：

```java
// 传统方式：线程管理容易错
Future<Order> orderFuture = es.submit(this::fetchOrder);
Future<User> userFuture = es.submit(this::fetchUser);
try {
    Order order = orderFuture.get();
    User user = userFuture.get();
} catch (ExecutionException e) {
    // 需要手动取消另一个任务
    orderFuture.cancel(true);
    userFuture.cancel(true);
}

// 结构化并发：StructuredTaskScope (JDK 21+ 预览/孵化)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<Order> order = scope.fork(this::fetchOrder);
    Future<User> user = scope.fork(this::fetchUser);
    
    scope.join();           // 等待所有任务完成
    scope.throwIfFailed();  // 任一失败则取消所有并抛出
    
    return new OrderResponse(order.resultNow(), user.resultNow());
}
// 自动管理：scope 会在 try-with-resources 结束时确保所有子任务已完成
```

> 🎯 **核心要点**：JDK 21+ 的新特性集中在 **三** 个方向：**并发革命** (Virtual Threads + Structured Concurrency + Scoped Values)、**模式匹配增强** (Record Patterns + Pattern Matching for switch + Sealed Class)、**数据不可变** (Records + Sequenced Collections)。

---

## 5. 企业 JDK 选型决策框架 {#5}

### 5.1 新项目选型决策树

```
开始新项目选型
    │
    ├── 项目上线时间？
    │   ├── 半年内 → 使用已稳定至少 1 年的 LTS
    │   │                  JDK 21 (推荐) / JDK 17 (保守)
    │   └── 一年后 → 选择当前最新 LTS 或预览 LTS
    │                  JDK 25 (推荐) / JDK 21 (安稳)
    │
    ├── 团队技术能力？
    │   ├── 初级团队 → JDK 17 (简洁语法、学习成本低)
    │   └── 高级团队 → JDK 21+ (充分发挥 Virtual Threads 优势)
    │
    ├── 应用类型？
    │   ├── IO 密集型微服务 → JDK 21+ (Virtual Threads 收益巨大)
    │   ├── 批处理/大数据 → JDK 17+ (G1/ZGC 调优成熟)
    │   ├── Android/移动端 → JDK 17 (受 Android Gradle 插件限制)
    │   └── 桌面应用 → JDK 21+ (JavaFX + jpackage 打包)
    │
    └── 基础设施要求？
        ├── 容器化部署 → JDK 17+ (容器感知良好)
        ├── ARM 架构 (M芯片) → JDK 17+ (ARM64 优化)
        └── 超大内存 (256GB+) → JDK 21+ (ZGC 分代模式)
```

### 5.2 老项目升级决策树

```
老项目是否升级?
    │
    ├── JDK 8 → 评估必须升级的场景：
    │   ├── 安全合规要求 (Java 8 免费更新 2030 截止)
    │   ├── 依赖框架要求新版本 (Spring Boot 3.x 需 JDK 17+)
    │   ├── 需要新特性 (Lambda/Stream 已在 JDK 8，但能用到 Virtual Threads)
    │   └── 性能瓶颈 (高并发场景 JDK 11/17/21 有显著提升)
    │
    └── 升级路径选择：
        ├── 一步到位 → 8 → 21 (适合依赖少的项目，跳过 11、17)
        ├── 分步迁移 → 8 → 11 → 17 → 21 (适合大型项目，逐步验证)
        └── 保守升级 → 8 → 17 (跳过 11，中间状态最短)
```

### 5.3 行业偏好参考

| 行业 | 主流版本 | 考量原因 |
|------|---------|---------|
| 互联网大厂 (BAT/TMD) | JDK 17/21 | 自研发行版 (Dragonwell、毕昇)，追求性能和新技术 |
| 金融/银行 | JDK 8/11 | 监管严格，稳定优先，升级周期长 |
| 大型传统企业 | JDK 8/11 | 遗留系统多，框架兼容性优先 |
| 创业公司/新业务 | JDK 21 | 无历史包袱，直接使用新特性 |
| 云原生/SaaS | JDK 21 | Virtual Threads 适合微服务，容器化友好 |
| 大数据/Spark/Flink | JDK 11/17 | 依赖框架对 JDK 版本有明确要求 |
| Android | JDK 17 (AGP 8.x) | Android Gradle Plugin 限制 |

### 5.4 选型综合决策矩阵

| 评估维度 | JDK 8 | JDK 11 | JDK 17 | JDK 21 | JDK 25 |
|---------|:-----:|:------:|:------:|:------:|:------:|
| 稳定性 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| 新特性 | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 性能 (吞吐) | 基准 | +15% | +20% | +30% | +35% |
| 社区活跃度 | 低 | 中 | 高 | 高 | 中 (初期) |
| 迁移成本 | — | 中 (JAXB 移除) | 中低 | 低 (JDK 17→21) | 低 |
| 学习成本 | 低 | 中 | 中 | 高 (VT 新模式) | 中 |
| 虚拟线程 | ❌ | ❌ | ❌ | ✅ | ✅ |
| ZGC 分代 | ❌ | ❌ | ❌ | 实验 | ✅ |
| 依赖生态 | 最全 | 完善 | 完善 | 完善 | 较完善 |

> 💡 **推荐结论**：
> - **2026 年新项目首选：JDK 21** (LTS 到 2031，Virtual Threads 稳定，生态完善)
> - **激进的团队选：JDK 25** (最新 LTS，分代 ZGC 默认)
> - **保守的项目：JDK 17** (最稳定的 LTS，2029 年 9 月前安全更新)
> - **老项目升级到：JDK 21** (JDK 17→21 迁移成本低，收益大)

---

## 6. OpenJDK vs Oracle JDK vs 其他发行版 {#6}

### 6.1 主流 JDK 发行版总览

| 发行版 | 厂商 | 免费商业使用 | 支持周期 | 特色优势 |
|--------|:----:|:-----------:|:--------:|---------|
| **OpenJDK** | 开源社区 | ✅ 完全免费 | 6 个月 (每版本) | 参考实现，标准纯正 |
| **Oracle JDK** | Oracle | ❌ 付费 (商业) | 最长 LTS 支持 | 最新修复、MC 认证、高级工具 |
| **Oracle OpenJDK** | Oracle | ✅ 免费 | 与社区同步 | 官方构建版本 |
| **Amazon Corretto** | AWS | ✅ 免费 | 长期 (至少 8 年) | 经过 AWS 大规模验证、容器优化 |
| **Adoptium (Eclipse Temurin)** | Eclipse 基金会 | ✅ 免费 | 长期 (至少 4 年) | 社区主导，最广泛使用 |
| **Azul Zulu** | Azul | ✅ 免费 | 长期 + 付费超长期 | Prime 版超长支持 (2030+) |
| **Alibaba Dragonwell** | 阿里巴巴 | ✅ 免费 | 长期 (阿里内部验证) | 中文社区、中文文档、Wisp 协程 |
| **Huawei Bisheng (毕昇)** | 华为 | ✅ 免费 | 长期 | ARM64 深度优化、Kunpeng 处理器 |
| **GraalVM** | Oracle | ✅ 免费 | 社区版 | Native Image、多语言、高性能 |
| **IBM Semeru** | IBM | ✅ 免费 | 长期 | OpenJ9 VM (低内存占用) |
| **Liberica JDK** | BellSoft | ✅ 免费 | 长期 | 完整 JavaFX 支持、镜像优化 |
| **SAP Machine** | SAP | ✅ 免费 | 长期 | SAP 应用优化 |
| **Microsoft Build of OpenJDK** | Microsoft | ✅ 免费 | 长期 | Azure 集成优化 |

### 6.2 核心发行版对比 (JDK 21)

| 对比项 | Oracle JDK | Eclipse Temurin | Amazon Corretto | Alibaba Dragonwell | Azul Zulu Prime |
|-------|:----------:|:--------------:|:---------------:|:------------------:|:--------------:|
| 免费许可 | 仅开发/测试 | 完全免费 | 完全免费 | 完全免费 | 免费版免费 |
| 生产商业支持 | 付费 | 无 (社区) | AWS 客户可用 | 无 (社区) | 付费版支持 |
| 更新频次 | 季度+补丁 | 季度 | 季度 | 季度 | 季度 |
| 安全修复速度 | 最快 (Oracle 主导) | 及时 | 及时 | 及时 | 及时 |
| TCK 认证 | 完整 | 完整 | 完整 | 完整 | 完整 |
| 额外增强 | Oracle 专属工具 | 无 | 冷启动优化 | Wisp 协程、多版本 GC | C4 无暂停 GC |
| 容器支持 | 好 | 好 | 优秀 | 好 | 优秀 |
| ARM64 支持 | 好 | 好 | 好 (Graviton) | 好 | 好 |
| 中文支持 | 一般 | 一般 | 一般 | 优秀 | 一般 |
| 用户量 | 高 (付费用户) | 最高 | 高 (AWS 生态) | 中 (国内广泛) | 中 (金融行业) |

### 6.3 如何选择发行版

**决策树**：

```
选择 JDK 发行版
    │
    ├── 是否愿意付费？
    │   ├── 是 → Oracle JDK (最快更新、商业支持)
    │   └── 否 → 继续看下面
    │
    ├── 部署环境？
    │   ├── AWS 云 → Amazon Corretto (原生集成)
    │   ├── Azure 云 → Microsoft Build (原生集成)
    │   ├── 阿里云/国内部署 → Dragonwell (中文文档+优化)
    │   ├── 华为云/Kunpeng 芯片 → 毕昇 Bisheng (ARM64 极致优化)
    │   └── 多云/通用 → Temurin (最广泛使用、无厂商锁定)
    │
    └── 特殊需求？
        ├── 需要 JavaFX → Liberica JDK (内置 JavaFX)
        ├── 需要极致低内存 → IBM Semeru (OpenJ9)
        ├── 需要 Native Image → GraalVM
        ├── 需要超长支持 (2030+) → Azul Zulu Prime
        └── 无特殊需求 → Temurin (社区首选)
```

> ⚠️ **重要提醒**：无论选择哪个发行版，核心 JDK 代码都来自同一 OpenJDK 代码库。不同发行版的差异主要体现在：
> 1. **更新/修复的交付速度**
> 2. **额外工具和增强功能**
> 3. **商业支持服务**
> 4. **特定平台优化**

### 6.4 国内使用建议

| 场景 | 推荐发行版 | 理由 |
|------|-----------|------|
| 个人学习/本地开发 | Oracle OpenJDK / Temurin | 免费、纯净、标准 |
| 阿里云部署 | Alibaba Dragonwell | 国内优化+中文支持，大厂验证 |
| 华为云/Kunpeng | 毕昇 Bisheng | ARM64 极致优化 |
| AWS 部署 | Amazon Corretto | AWS 全系列服务深度集成 |
| 多云/混合云 | Eclipse Temurin | 无厂商锁定，社区最大 |
| 需要商业支持的单位 | Oracle JDK / Azul Zulu Prime | 合规、技术支持 |

---

## 7. 面试标准话术 {#7}

### 7.1 "你项目用 Java 几？为什么选它？"

**答案模板（根据实际情况选择）**：

> **场景一：使用 JDK 21**
>
> "我们项目使用 JDK 21，原因有三：
> 第一，JDK 21 是 2023 年 9 月发布的 LTS 版本，安全更新至少到 2031 年，覆盖项目生命周期。
> 第二，我们大量使用 Virtual Threads 来处理高并发请求——作为微服务架构，IO 密集型场景占主导，虚拟线程使我们的服务吞吐量提升了约 3 倍，且代码从复杂的 Reactor/回调模型简化为传统的同步阻塞模型，开发效率大幅提升。
> 第三，Record Patterns、Pattern Matching for switch 等语言特性让我们的数据处理代码更简洁、可读性更好。
>
> 不足之处在于团队需要学习虚拟线程的限制 (如避免 synchronized pinned)，不过我们已经在 Code Review 中建立了相关规范。"

> **场景二：使用 JDK 17**
>
> "我们项目使用 JDK 17，它是最成熟的现代 LTS 版本。选择理由：
> 首先，JDK 17 继承了 JDK 11 的模块化系统和 JDK 8 的稳定性，同时引入了 Record、Sealed Class 等生产力提升特性，但没有引入 Virtual Threads 这种改变编程模型的变革性特性，对团队来说学习成本适中。
> 其次，我们的依赖生态 (Spring Boot 3.x、Hibernate 6.x) 对 JDK 17 支持最完善。
> 目前我们计划在 2027 年前迁移到 JDK 21，届时 Virtual Threads 会更成熟，团队也有足够时间过渡。"

> **场景三：使用 JDK 8 (老项目)**
>
> "我们目前还是 JDK 8，主要原因是被历史债务约束——有多个核心依赖库还不支持 JDK 11+，而且迁移成本 (JAXB 移除、模块化兼容) 评估下来需要 3-6 个月。不过我们已经在规划升级路径：先升级依赖到兼容 JDK 17 的版本，然后一步到位升级到 JDK 21 跳过中间版本。
>
> 坦率地说，JDK 8 现在确实有些落后了：缺少 var、新的日期 API 在 JDK 8 虽然已有但不如高版本完善、没有 Record 和 Sealed Class、GC 效率不如 G1/ZGC。我们计划最晚年底前完成迁移。"

### 7.2 "JDK 8、11、17、21 有什么区别？"

> "这四个 LTS 版本代表了 Java 语言演进的四个阶段：
>
> **JDK 8** (2014) 是函数式编程的开端，Lambda + Stream API 改变了编程范式。但它是十年前的版本，缺少很多现代特性。
>
> **JDK 11** (2018) 是模块化的转折点，引入了 JPMS (Jigsaw)、HTTP Client 和 ZGC (实验)。但 Oracle JDK 开始收费，导致社区大规模迁移到 OpenJDK。
>
> **JDK 17** (2021) 是语言特性加速的版本，Record、Sealed Class、Pattern Matching、Text Blocks 等显著提升了开发效率。GC 也成熟了 (G1 增强)。
>
> **JDK 21** (2023) 是并发模型的革命，Virtual Threads 彻底改变了高并发应用的开发方式。加上 Record Patterns、Pattern Matching for switch 等特性，标志着 Java 进入了一个新阶段。
>
> 如果让我给建议：新项目直接 JDK 21 或 25，老项目至少升级到 JDK 17。"

### 7.3 "OpenJDK 和 Oracle JDK 怎么选？"

> "核心问题在于是否需要 Oracle 的商业支持。
>
> 技术层面，两者代码高度一致——Oracle JDK 基于 OpenJDK 构建，差异主要在于：
> - Oracle JDK 包含一些商业特性 (如 Flight Recorder 在 JDK 11 前是收费功能，但现在已经开源)
> - Oracle JDK 的补丁发布速度略快 (Oracle 主导 OpenJDK 社区)
>
> 实际落地时，绝大多数团队选择免费的发行版：AWS 用 Corretto、阿里云用 Dragonwell、通用场景用 Eclipse Temurin。我们团队用得最多的是 Eclipse Temurin，原因是没有厂商锁定、社区活跃、更新及时。"

### 7.4 综合展示深度

> **被追问："JDK 17 到 21，你们迁移时遇到了什么问题？"**
>
> "我们迁移时主要遇到了三个问题：
> 第一，一些第三方库使用了 JAXB、JAF 等已被移除的 Java EE 模块，需要在 pom.xml 中添加 Jakarta EE 的依赖。
> 第二，Spring Boot 从 2.x 升级到 3.x 时，javax.* 到 jakarta.* 的包名变更导致一些自定义 Starter 需要修改。
> 第三，测试环境中发现使用 synchronized 的虚拟线程会有 pinned 问题，导致并发性能不升反降。我们使用 `jcmd <pid> Thread.dump_to_file -format=json` 抓取线程 dump 定位到了 pinned 的虚拟线程，然后改用 ReentrantLock 修复了这个问题。
>
> 最终，迁移的净收益还是非常明显的：在 4C8G 的容器上，我们的 API 网关从 800 TPS 提升到了 3000+ TPS，主要是 Virtual Threads 消除了 IO 等待的开销。"

---

> 🎯 **核心要点**：JDK 版本选型是一个结合技术演进、团队能力、业务场景的**多维度决策**。面试中回答版本选择问题时，要展示出：
> 1. **对 JDK 演进脉络的理解** — 知道每个 LTS 的核心特性和定位
> 2. **对迁移实战的把握** — 了解迁移的痛点和难点，而非纸上谈兵
> 3. **对业务场景的思考** — 版本选择应服务于业务需求，而非盲目追新

---

**下一模块**：[02 JDK安装与多版本管理](./02-JDK安装与多版本管理.md) | **返回总览**：[总览](./00-JDK知识体系总览.md)
