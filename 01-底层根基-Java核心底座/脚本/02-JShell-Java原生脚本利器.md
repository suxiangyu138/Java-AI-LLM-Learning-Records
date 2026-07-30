# JShell —— Java 原生脚本利器

> JShell 是 Java 的"交互式实验室"——无需 main 方法、无需编译、即写即验，让 Java 成为真正的脚本语言

## 📚 目录

1. [JShell 简介与定位](#1)
2. [基础操作入门](#2)
3. [斜杠命令完全指南](#3)
4. [Shebang 脚本与 JEP 330/495](#4)
5. [实战场景与应用](#5)
6. [IDE 集成与工程化](#6)
7. [JShell API 编程调用](#7)
8. [局限性与适用边界](#8)

---

## 1. JShell 简介与定位 {#1}

### 1.1 什么是 JShell？

JShell 是 JDK 9 引入的 **REPL**（Read-Eval-Print Loop）工具，允许开发者输入 Java 代码片段并立即执行，无需创建项目、编写完整类定义或 `public static void main`。

```text
$ jshell
|  欢迎使用 JShell —— 版本 21.0.1
|  要大致了解该版本, 请键入: /help intro

jshell> System.out.println("Hello, JShell!")
Hello, JShell!

jshell> 3 + 5 * 2
$2 ==> 13
```

### 1.2 JShell 的三大定位

| 定位 | 说明 | 典型场景 |
|------|------|---------|
| **交互式探索** | 实时测试 API、验证语法、探索类库 | 学习新 API、调试逻辑 |
| **快速原型** | 无需完整项目结构，即写即验 | 算法验证、数据处理 |
| **脚本自动化** | Shebang 脚本、单文件程序 | 运维工具、文件批处理 |

### 1.3 JShell vs 传统 Java 开发

| 维度 | 传统 Java | JShell |
|------|----------|--------|
| 项目结构 | 需要目录、包、类 | 无结构要求 |
| main 方法 | 必须 `public static void main` | 不需要 |
| 编译 | `javac` 显式编译 | 自动后台编译 |
| 执行反馈 | 运行后看结果 | 逐条即时反馈 |
| 分号 | 必须 | 可选（单语句） |
| 依赖管理 | Maven/Gradle | `--class-path` 手动指定 |
| 适用阶段 | 正式开发 | 探索、原型、学习 |

---

## 2. 基础操作入门 {#2}

### 2.1 启动与退出

```bash
# 基本启动
jshell

# 指定 classpath（加载外部 JAR）
jshell --class-path "lib/gson-2.10.1.jar:lib/commons-lang3-3.14.0.jar"

# 加载启动脚本
jshell --startup DEFAULT --startup ~/my-startup.jsh

# 指定 JDK 模块路径
jshell --add-modules java.sql

# 退出
jshell> /exit
```

### 2.2 代码片段类型

```java
// ── 表达式（自动创建临时变量 $1, $2...）──
jshell> 42 * 3.14
$1 ==> 131.88

jshell> "Hello".toUpperCase()
$2 ==> "HELLO"

// ── 变量声明 ──
jshell> String name = "Java 21"
name ==> "Java 21"

jshell> var list = List.of(1, 2, 3, 4, 5)
list ==> [1, 2, 3, 4, 5]

// ── 方法定义 ──
jshell> int fibonacci(int n) {
   ...>     if (n <= 1) return n;
   ...>     return fibonacci(n - 1) + fibonacci(n - 2);
   ...> }
|  已创建 方法 fibonacci(int)

jshell> fibonacci(10)
$5 ==> 55

// ── 类定义 ──
jshell> record Point(int x, int y) {
   ...>     double distance() {
   ...>         return Math.sqrt(x*x + y*y);
   ...>     }
   ...> }
|  已创建 记录 Point

jshell> var p = new Point(3, 4); p.distance()
$7 ==> 5.0

// ── 接口定义 ──
jshell> interface Greeter {
   ...>     String greet(String name);
   ...> }
|  已创建 接口 Greeter
```

### 2.3 Tab 补全与智能提示

```text
jshell> System.out.<Tab>
          append(        checkError()   close()        equals(
          flush()        format(        getClass()     hashCode()
          notify()       notifyAll()    print(         printf(
          println(       toString()     wait(          write(

jshell> Math.<Tab>   # 按住 Shift+Tab 再按 i 可导入缺失的类
```

### 2.4 前向引用

JShell 支持前向引用（forward reference），可以先调用尚未定义的方法：

```java
jshell> int volume = area(10) * 2    // area() 尚未定义
|  已创建 变量 volume, 但是...      // 暂时允许，但不执行

jshell> int area(int r) { return (int)(Math.PI * r * r); }
area ==> 314
volume ==> 628                     // 自动更新！
```

---

## 3. 斜杠命令完全指南 {#3}

### 3.1 信息查看命令

| 命令 | 缩写 | 功能 |
|------|:---:|------|
| `/list` | `/l` | 列出所有源代码片段（可加参数：`-all` `-start`） |
| `/vars` | `/v` | 列出所有已声明变量及其值 |
| `/methods` | `/m` | 列出所有已声明方法及其签名 |
| `/types` | `/t` | 列出所有已声明的类型（类、接口、枚举） |
| `/imports` | `/i` | 列出所有导入 |
| `/history` | `/h` | 显示输入历史 |

```text
jshell> /vars
|    int $1 = 131
|    String name = "Java 21"
|    Point p = Point[x=3, y=4]

jshell> /methods
|    int fibonacci(int n)
|    int area(int r)

jshell> /types
|    record Point
|    interface Greeter
```

### 3.2 编辑与管理命令

| 命令 | 功能 | 示例 |
|------|------|------|
| `/edit` | 打开 JShell Edit Pad 编辑器 | `/edit fibonacci` |
| `/edit <id>` | 编辑指定 ID 的片段 | `/edit 3` |
| `/drop <id>` | 删除指定片段 | `/drop 1` |
| `/reset` | 重置 JShell 状态（清空所有代码） | `/reset` |
| `/reload` | 重新加载所有片段 | `/reload` |

```text
jshell> /l
   1 : System.out.println("Hello")
   2 : int x = 10;
   3 : int y = x * 2;

jshell> /drop 3
|  已删除代码段 3

jshell> /l
   1 : System.out.println("Hello")
   2 : int x = 10;
```

### 3.3 会话持久化命令

```bash
# ── 保存当前会话 ──
jshell> /save my-session.jsh         # 保存所有有效片段
jshell> /save -all full-session.jsh  # 保存所有（含错误和覆盖的）
jshell> /save -history hist.jsh      # 保存历史命令

# ── 加载会话文件 ──
jshell> /open my-session.jsh         # 从文件加载代码

# ── 预定义启动脚本（每次启动自动加载）──
# 文件：~/.jshrc 或 Jshell 启动时指定
jshell --startup ~/my-startup.jsh
```

### 3.4 环境配置命令

| 命令 | 功能 | 示例 |
|------|------|------|
| `/env` | 查看/修改环境信息 | `/env -class-path lib/*.jar` |
| `/set` | 配置 JShell 设置 | `/set feedback verbose` |
| `/set mode` | 配置反馈模式 | `/set mode mine normal -command` |
| `/feedback` | 切换反馈详细程度 | `/feedback verbose` |

```text
# 反馈模式
jshell> /set feedback concise    # 简洁模式
jshell> /set feedback normal     # 普通模式
jshell> /set feedback verbose    # 详尽模式
jshell> /set feedback silent     # 静默模式（只输出表达式值）
```

### 3.5 外部代码加载

```java
// ── 在 JShell 中动态添加 classpath ──
jshell> /env -class-path lib/gson-2.10.1.jar

// ── 加载其他文件（支持通配符）──
jshell> /open utils.jsh
jshell> /open snippets/*.jsh
```

---

## 4. Shebang 脚本与 JEP 330/495 {#4}

### 4.1 Shebang 脚本（JShell 模式）

利用 Unix Shebang 机制，可以将 Java 代码作为可直接执行的脚本：

```java
#!/usr/bin/env jshell --execution local

// csv-stats.jsh —— 统计 CSV 文件中某列的平均值
import java.nio.file.*;
import java.util.stream.*;

String file = "/tmp/data.csv";
int col = 2;

double avg = Files.lines(Path.of(file))
    .skip(1)    // 跳过表头
    .map(line -> line.split(","))
    .mapToDouble(parts -> Double.parseDouble(parts[col].trim()))
    .average()
    .orElse(0.0);

System.out.printf("第 %d 列平均值: %.2f%n", col, avg);
/exit
```

```bash
chmod +x csv-stats.jsh
./csv-stats.jsh
```

> ⚠️ **注意**：Shebang 脚本必须以 `/exit` 结尾，否则 JShell 保持在交互模式。

### 4.2 JEP 330 —— 单文件源代码程序（JDK 11+）

JDK 11 引入 JEP 330，允许直接运行单个 `.java` 文件，无需显式编译：

```java
// hello.java —— 直接运行：java hello.java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello from JEP 330!");
        System.out.println("Args: " + String.join(", ", args));
    }
}
```

```bash
java hello.java arg1 arg2 arg3
# 输出: Hello from JEP 330!
#       Args: arg1, arg2, arg3
```

### 4.3 JEP 495 —— 简单源文件与实例 Main 方法（JDK 23+）

JDK 23 通过 JEP 495 进一步简化，允许隐式类声明和实例 main 方法：

```java
// hello-modern.java —— JDK 23+ 极简风格
void main() {
    println("Hello from JDK 23+!");
}

// 等价于旧的写法:
// public class HelloModern {
//     public static void main(String[] args) {
//         System.out.println("Hello from JDK 23+!");
//     }
// }
```

```bash
java --enable-preview --source 23 hello-modern.java
```

### 4.4 三种方式的演进对比

| 特性 | 传统 Java | JShell Shebang | JEP 330 | JEP 495 |
|------|----------|:---:|:---:|:---:|
| JDK 版本要求 | 所有版本 | JDK 9+ | JDK 11+ | JDK 23+ |
| 需要类定义 | ✅ | ❌ | ✅ | ❌ |
| 需要 main 方法 | ✅ static | ❌ | ✅ static | ❌ 或实例 |
| 显式编译 | ✅ javac | ❌ | ❌ | ❌ |
| 外部依赖 | 构建工具 | `--class-path` | `--class-path` | `--class-path` |
| 可执行权限 | ❌ | ✅ chmod +x | ❌ | ❌ |
| 适合场景 | 正式项目 | 工具脚本 | 快速原型 | 极简程序 |

> 🎯 **核心要点**：JShell Shebang 适合运维脚本，JEP 330 适合有外部依赖的单文件工具，JEP 495 是未来最简洁的原型方式。

---

## 5. 实战场景与应用 {#5}

### 5.1 场景一：CSV 数据处理

```java
// csv-processor.java —— 分析学生成绩 CSV
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

void main() throws Exception {
    var path = Path.of("grades.csv");

    var stats = Files.lines(path)
        .skip(1)
        .map(line -> line.split(","))
        .collect(Collectors.summarizingDouble(
            parts -> Double.parseDouble(parts[2])  // 第 3 列：成绩
        ));

    System.out.printf("""
        成绩统计:
        ├── 人数: %d
        ├── 平均分: %.1f
        ├── 最高分: %.1f
        ├── 最低分: %.1f
        └── 标准差: %.1f
        """,
        stats.getCount(),
        stats.getAverage(),
        stats.getMax(),
        stats.getMin(),
        Math.sqrt(stats.getCount() > 0 ?
            (stats.getSumOfSquares() / stats.getCount() -
             Math.pow(stats.getAverage(), 2)) : 0)
    );
}
```

### 5.2 场景二：HTTP API 调用

```java
// api-tester.java —— 测试 REST API（JDK 11+ HttpClient）
import java.net.http.*;
import java.net.URI;
import java.time.Duration;

void main() throws Exception {
    var client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.github.com/users/oracle/repos"))
        .header("Accept", "application/vnd.github.v3+json")
        .GET()
        .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.printf("状态码: %d%n", response.statusCode());
    System.out.printf("响应大小: %d 字节%n", response.body().length());
    System.out.printf("前 500 字符: %s%n",
        response.body().substring(0, Math.min(500, response.body().length())));
}
```

### 5.3 场景三：数据库快速查询

```bash
# 启动 JShell 时加载 JDBC 驱动
jshell --class-path "lib/h2-2.2.224.jar" --add-modules java.sql
```

```java
// ── 在 JShell 中执行 ──
import java.sql.*;

var conn = DriverManager.getConnection(
    "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");

// 建表并插入数据
conn.createStatement().execute("""
    CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50))
    """);
conn.createStatement().execute(
    "INSERT INTO users VALUES (1, 'Alice'), (2, 'Bob'), (3, 'Charlie')");

// 查询
var rs = conn.createStatement().executeQuery("SELECT * FROM users");
while (rs.next()) {
    System.out.printf("ID: %d, Name: %s%n", rs.getInt("id"), rs.getString("name"));
}
```

### 5.4 场景四：文件批处理

```bash
#!/usr/bin/env jshell --execution local

// rename-files.jsh —— 批量重命名文件，添加日期前缀
import java.nio.file.*;
import java.time.LocalDate;

String dirPath = "/tmp/photos";
String prefix = LocalDate.now().toString();
Path dir = Path.of(dirPath);

Files.list(dir)
    .filter(Files::isRegularFile)
    .filter(p -> !p.getFileName().toString().startsWith(prefix))
    .forEach(p -> {
        try {
            Path newName = p.resolveSibling(prefix + "_" + p.getFileName());
            Files.move(p, newName);
            System.out.println("✓ " + p.getFileName() + " → " + newName.getFileName());
        } catch (Exception e) {
            System.err.println("✗ 失败: " + p + " - " + e.getMessage());
        }
    });

/exit
```

### 5.5 场景五：算法原型验证

```java
// 在 JShell 中快速验证算法思路
jshell> // 验证单调栈解法
jshell> int[] heights = {2, 1, 5, 6, 2, 3};

jshell> int maxArea(int[] h) {
   ...>     var stack = new java.util.ArrayDeque<Integer>();
   ...>     int max = 0;
   ...>     for (int i = 0; i <= h.length; i++) {
   ...>         int cur = (i == h.length) ? 0 : h[i];
   ...>         while (!stack.isEmpty() && cur < h[stack.peek()]) {
   ...>             int height = h[stack.pop()];
   ...>             int width = stack.isEmpty() ? i : i - stack.peek() - 1;
   ...>             max = Math.max(max, height * width);
   ...>         }
   ...>         stack.push(i);
   ...>     }
   ...>     return max;
   ...> }

jshell> maxArea(heights)
$3 ==> 10    // 答案正确 ✓
```

---

## 6. IDE 集成与工程化 {#6}

### 6.1 IntelliJ IDEA 集成

```
Tools → JShell Console...  (或 Ctrl+Shift+A → 搜索 "JShell")

功能：
├── 直接使用项目 classpath（自动加载依赖）
├── 代码补全（Tab 触发）
├── 一键将片段保存到项目源文件
└── 与调试器集成
```

### 6.2 VS Code 集成

```bash
# 安装 Java Extension Pack
# 打开 Command Palette (Ctrl+Shift+P)
# → "Java: Open JShell Console"
```

### 6.3 项目级 JShell 配置

```bash
# 创建项目专属启动脚本 —— project.jsh
# 自动导入常用类和静态方法

import java.time.*;
import java.util.*;
import java.util.stream.*;
import java.nio.file.*;
import static java.util.stream.Collectors.*;

System.out.println("✅ 项目 JShell 环境已就绪");
System.out.println("已导入: java.time, java.util, java.util.stream, java.nio.file");
```

```bash
# 启动时加载
jshell --startup project.jsh --class-path "$(./mvnw dependency:build-classpath -q -DincludeScope=runtime)"
```

### 6.4 JShell 在 CI/CD 中的应用

```bash
# 利用 JShell 执行构建后验证脚本
jshell --execution local verify-deployment.jsh <<EOF
/exit
EOF
```

---

## 7. JShell API 编程调用 {#7}

JShell 不仅是命令行工具，还提供了完整的 Java API（`jdk.jshell` 包），可用于：
- 代码编辑器中的在线执行
- 自动化代码审查工具
- 教学/面试平台

```java
import jdk.jshell.*;
import java.util.List;

public class JShellApiDemo {
    public static void main(String[] args) {
        try (JShell js = JShell.create()) {

            // ── 逐条执行代码 ──
            List<SnippetEvent> events = js.eval("""
                int x = 10;
                int y = 20;
                int sum = x + y;
                System.out.println("Sum: " + sum);
                """);

            // ── 查看执行结果 ──
            for (SnippetEvent event : events) {
                Snippet snippet = event.snippet();
                System.out.printf("[%s] %s → %s%n",
                    snippet.kind(),
                    snippet.source(),
                    event.value() != null ? event.value() : "(无值)");
            }

            // ── 查看变量 ──
            js.variables().forEach(v ->
                System.out.printf("变量 %s = %s (类型: %s)%n",
                    v.name(), js.varValue(v), v.typeName()));

            // ── 查看方法 ──
            js.methods().forEach(m ->
                System.out.printf("方法 %s%n", m.signature()));

            // ── 代码诊断 ──
            // JShell 内部编译，可获取编译错误
            js.eval("int z = unknown;").forEach(event -> {
                js.diagnostics(event.snippet()).forEach(d ->
                    System.err.printf("❌ 第 %d 行: %s%n",
                        d.getStartPosition(), d.getMessage(Locale.getDefault())));
            });

            // ── Tab 补全支持 ──
            SourceCodeAnalysis sca = js.sourceCodeAnalysis();
            int[] anchor = {0};
            sca.completionSuggestions(
                "System.out.prin",
                13, anchor)  // cursor position
                .forEach(c -> System.out.println("补全: " + c.continuation()));
        }
    }
}
```

| JShell API 核心类 | 功能 |
|-------------------|------|
| `JShell` | 主入口，创建 JShell 实例 |
| `JShell.eval()` | 执行代码片段 |
| `JShell.variables()` | 获取所有变量 |
| `JShell.methods()` | 获取所有方法 |
| `JShell.types()` | 获取所有类型 |
| `Snippet.Status` | 片段状态（VALID / RECOVERABLE / REJECTED） |
| `SourceCodeAnalysis` | 代码补全和分析 |

> 🎯 **核心要点**：JShell API 让"代码即数据"成为可能——你可以程序化地执行、诊断、分析 Java 代码片段。

---

## 8. 局限性与适用边界 {#8}

### 8.1 明确不适合的场景

| 场景 | 原因 | 替代方案 |
|------|------|---------|
| **大型多模块项目** | 无模块系统支持 | Maven/Gradle 项目 |
| **注解处理器** | 注解在 JShell 中工作受限 | 完整编译流程 |
| **性能基准测试** | JShell 有额外包装导致偏差 | JMH |
| **长时间运行服务** | 非为服务化设计 | Spring Boot / Quarkus |
| **复杂调试** | 仅支持 REPL 级交互 | IDE 调试器 |
| **多线程程序** | 线程上下文不可控 | 标准 Java 应用 |
| **与其他 CLI 工具协作** | Java → Shell 传参不便 | 使用 Bash 做上游，单次调用 Java |

### 8.2 性能考量

```text
JShell 执行开销因素：
├── 首次启动：~200-500ms（JVM 冷启动）
├── Shebang 模式：每次执行都重新启动 JVM
└── 片段编译：相对于 javac 有微小额外开销

优化建议：
├── 重用 JShell 实例（API 模式）
├── 使用 JEP 330 单文件模式减少 JShell 开销
└── 对频繁执行的脚本，考虑编译为独立 JAR
```

### 8.3 JShell vs JBang vs 单文件程序 选型

| 维度 | JShell | JBang | JEP 330 单文件 |
|------|:---:|:---:|:---:|
| 启动速度 | 慢（REPL 模式） | 快（首次下载后缓存） | 快 |
| 外部依赖 | 手动 --class-path | `//DEPS` 自动管理 | 手动 --class-path |
| IDE 支持 | ✅ 原生支持 | ✅ 插件支持 | ✅ 原生支持 |
| 脚本可执行性 | Shebang | ✅ 直接运行 | ❌ 需要 java 命令 |
| 交互式探索 | ✅ 核心能力 | ❌ | ❌ |
| 适合用途 | 学习、原型 | 工具脚本 | 快速原型 |

> 🎯 **核心要点**：学习探索用 JShell，工具脚本用 JBang，快速原型用 JEP 330。三者互补，不是替代关系。

---

**返回总览：** [00-脚本知识体系总览](./00-脚本知识体系总览.md) | **下一模块：** [03-Java Scripting API 与 JVM 脚本语言](./03-Java-Scripting-API与JVM脚本语言.md)
