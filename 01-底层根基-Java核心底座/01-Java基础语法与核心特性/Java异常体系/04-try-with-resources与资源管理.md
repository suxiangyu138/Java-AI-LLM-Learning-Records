# 04 try-with-resources 与资源管理

> 资源泄漏是 Java 7 之前最普遍的 bug——try-with-resources（TWR）把"关闭资源"从手动纪律变成语言保证，逆序关闭与 suppressed 机制值得吃透

---

## 📚 目录

1. [资源管理的演进：从手动 finally 到 TWR](#1-资源管理的演进从手动-finally-到-twr)
2. [TWR 语法与编译器展开原理](#2-twr-语法与编译器展开原理)
3. [AutoCloseable vs Closeable](#3-autocloseable-vs-closeable)
4. [逆序关闭与 suppressed 抑制异常](#4-逆序关闭与-suppressed-抑制异常)
5. [JDK 9 增强：引用外部资源变量](#5-jdk-9-增强引用外部资源变量)
6. [TWR 的边界与 2025 最新动态](#6-twr-的边界与-2025-最新动态)

---

## 1. 资源管理的演进：从手动 finally 到 TWR

**Java 7 之前**：关闭资源靠手动 finally——漏写、异常路径不关闭、关闭顺序错误，是三大泄漏源头：

```java
// ❌ Java 7 前的经典写法（问题重重）
BufferedReader reader = null;
try {
    reader = Files.newBufferedReader(Path.of("a.txt"));
    String line = reader.readLine();
    // ... 业务
} catch (IOException e) {
    // 处理
} finally {
    // 问题 1：忘记写 finally → 异常路径泄漏
    // 问题 2：reader 可能为 null（newBufferedReader 就抛了）→ NPE
    // 问题 3：close 又抛异常 → 覆盖 try 里的原始异常
    reader.close();          // 三处隐患叠加
}
```

**TWR 之后**（JDK 7，JEP 183）：

```java
// ✅ 一行声明，关闭由语言保证
try (BufferedReader reader = Files.newBufferedReader(Path.of("a.txt"))) {
    String line = reader.readLine();
    // 无论这里发生什么，reader 必然被关闭
} catch (IOException e) {
    // 处理（close 的异常也在这里可见）
}
```

> 🎯 **核心要点**：TWR 把"关闭资源"从"程序员纪律"变成"编译器保证"——任何实现 `AutoCloseable` 的资源（流、连接、锁、线程池、HTTP 客户端），只要出现在 try 头，就**必然关闭**，无论正常、异常还是 return。

---

## 2. TWR 语法与编译器展开原理

**语法形态**：

```java
// 多资源：分号分隔（不是逗号！）
try (InputStream in = Files.newInputStream(p);
     OutputStream out = Files.newOutputStream(p2)) {
    in.transferTo(out);
}

// 变量在 try 块内有效，块外不可访问
// 资源变量隐式 final（JDK 9 前必须声明在 try 内，JDK 9+ 可引用外部变量，见第 5 节）
```

**编译器展开**（`javap -c` 可验证）：TWR 等价于自动生成的 finally + 隐藏的 catch 机制：

```java
// 编译器视角的展开逻辑（伪代码）：
AutoCloseable r1 = ...;
Throwable primary = null;
try {
    // 业务体
} catch (Throwable t) {
    primary = t;                    // 记录主异常
    throw t;
} finally {
    if (r1 != null) {
        if (primary != null) {      // 主异常存在时
            try { r1.close(); } catch (Throwable t) { primary.addSuppressed(t); }  // close 失败 → suppressed
        } else {
            r1.close();             // 无主异常 → close 异常正常抛出
        }
    }
}
```

**这段展开说明三个事实**：

1. **主异常优先**：try 体的异常是"主角"，close 的异常成为配角（suppressed）；
2. **无主异常时**：close 异常正常抛出（比手动 finally 更符合直觉）；
3. **null 资源**：`try (X x = null)` 不抛 NPE——展开里有判空（虽然正常代码不应写 null 资源）。

> 💡 验证方式：`javac` 后 `javap -c` 反编译，能看到编译器生成的 `Throwable` 局部变量与 `addSuppressed` 调用——面试答"TWR 底层是编译期展开"时可现场演示这一过程。

---

## 3. AutoCloseable vs Closeable

```text
AutoCloseable (JDK 7, java.lang)         close() throws Exception      —— 通用协议
   └── Closeable (JDK 5, java.io)        close() throws IOException    —— IO 特化
         ├── InputStream / OutputStream / Reader / Writer ...
         └── 其他 IO 流
```

| 维度 | AutoCloseable | Closeable |
|------|:-------------:|:---------:|
| 版本 | JDK 7 | JDK 5 |
| close 声明 | `throws Exception`（宽） | `throws IOException`（窄） |
| 幂等性 | 未约定（**实现可能不幂等**） | 约定幂等（多次 close 无害） |
| 范围 | 通用：流、连接、锁、ExecutorService | IO 专用 |
| TWR 支持 | ✅ 都支持 | ✅ |

**设计要点**：

- **自定义资源类实现 `AutoCloseable` 即可获得 TWR 能力**——close() 里做真正的释放；
- close() 建议**幂等**（多次调用安全）——TWR 只调用一次，但防御性设计更稳；
- close() 抛异常类型：自定义资源尽量抛窄异常或干脆不声明（close 失败抛 RuntimeException 亦可接受，但要注意它会变成 suppressed 或主异常，见第 4 节）。

```java
// 自定义资源：一行实现 AutoCloseable 即获 TWR 能力
public class DbSession implements AutoCloseable {
    private boolean closed;

    public void query(String sql) { /* ... */ }

    @Override
    public void close() {
        if (!closed) {            // 幂等：重复关闭无害
            releaseConnection();  // 真正释放
            closed = true;
        }
    }
}

// 使用
try (DbSession session = new DbSession()) {
    session.query("select 1");
}
```

> 🎯 **核心要点**：JDK 21 的虚拟线程、JDK 的 `ExecutorService`、`Lock`、`HttpClient` 等大量新 API 都实现了 `AutoCloseable`——"万物皆可 TWR"是现代 Java 的资源管理趋势。

---

## 4. 逆序关闭与 suppressed 抑制异常

**逆序关闭**（LIFO）：多资源按**声明顺序的逆序**关闭——先声明先打开，后声明后打开，关闭时先关后开的：

```java
try (InputStream in = ...;      // ① 先打开
     OutputStream out = ...) {  // ② 后打开
    in.transferTo(out);
}
// 关闭顺序：out（②）→ in（①）—— 与"先开后关"直觉一致
// 为什么重要：out 可能依赖 in 的数据缓冲，先关 out 确保数据冲刷，再关 in
```

**suppressed 抑制异常**（JDK 7 新增）：

```java
// 场景：try 体抛异常 + close() 也抛异常
try (Resource r = new Resource()) {
    throw new IllegalStateException("业务失败");   // 主异常
    // close() 此时抛 IOException("关闭失败")
}
// 调用方看到：主异常 IllegalStateException
// e.getSuppressed()[0] == IOException("关闭失败") —— 关闭失败没有被吞，而是"挂"在主异常上

catch (IllegalStateException e) {
    for (Throwable s : e.getSuppressed()) {        // 排查时绝不能忽略 suppressed
        log.warn("资源关闭失败", s);
    }
}
```

**suppressed 的价值**：TWR 之前，close 异常会**覆盖**业务异常（调试时看到错误的方向完全跑偏）；TWR 让两个异常**都保留**——主异常在前，关闭异常挂在 `getSuppressed()`。

> ⚠️ **排查纪律**：线上日志看到主异常后，务必检查 `getSuppressed()`——资源关闭失败的线索常常藏在这里（连接没释放、文件句柄泄漏的根因往往是 suppressed 异常）。

---

## 5. JDK 9 增强：引用外部资源变量

**JDK 9（JEP 213）**：try 头可以引用 **effectively final** 的外部变量——不再要求资源在 try 内声明：

```java
// JDK 7/8：资源必须声明在 try 内 —— 若资源在别处创建，还得再写一遍
Resource r = new Resource();           // 外部创建（如工厂返回）
// JDK 7/8 写法：
try (Resource r2 = r) { ... }          // 被迫再声明一个变量

// JDK 9+：直接引用
Resource r = new Resource();
try (r) {                              // ✅ 引用外部变量（must be effectively final）
    // ...
}
// 注意：r 必须 effectively final（声明后不再重新赋值）
```

**典型场景**：方法入参是 AutoCloseable、工厂方法返回资源、资源需要条件创建——JDK 9 后不用再包一层。

```java
public void process(AutoCloseableFactory factory) throws Exception {
    Resource r = factory.create();          // 资源来自外部
    try (r) {                               // JDK 9+：直接引用
        r.work();
    }
}
```

> 💡 多资源混合也支持：`try (r; r2)`——只要每个引用都是 effectively final。

---

## 6. TWR 的边界与 2025 最新动态

**TWR 的边界与注意点**：

| 边界 | 说明 |
|------|------|
| 资源变量不可变 | JDK 9+ 引用的外部资源必须 effectively final |
| 自定义 close 语义 | close() 抛异常时：有主异常→suppressed；无主异常→正常抛出——设计 close 时想清楚 |
| 不能替代业务 catch | TWR 只管关闭，业务异常的捕获仍需 catch 块 |
| 对象构造异常 | `try (new Resource())` 若构造抛异常 → 直接传播（无关闭义务，对象没建成） |
| 虚拟线程（JDK 21） | TWR 与虚拟线程配合无特殊问题——但**锁要避免在 TWR 中持有过久**（虚拟线程挂载成本） |

**2025 年在途动态**（时效性要点）：OpenJDK 正在评估让 `java.lang.Process` 实现 `AutoCloseable`（JDK-8364361，2025-08 core-libs-dev 讨论中）——届时 `Process` 可参与 TWR，自动等待/清理子进程资源。评审中关于 close() 是否处理 `InterruptedException` 的讨论，再次印证"TWR 的异常处理与重抛足够复杂"这一设计难点。

> 🎯 **核心要点**：TWR 是资源管理的**默认写法**——所有 `AutoCloseable` 资源一律 TWR，没有任何理由回到手动 finally。JDK 自身也在不断扩大 AutoCloseable 家族（Process 拟加入），说明这是语言演进的方向。

---

**下一模块**：[05-异常传播与捕获策略](./05-异常传播与捕获策略.md) / **返回总览**：[00-Java异常体系知识体系总览](./00-Java异常体系知识体系总览.md)
