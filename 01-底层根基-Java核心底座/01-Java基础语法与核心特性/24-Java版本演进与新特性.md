# 24 - Java版本演进与新特性

> 定位：系统性了解JDK 8到JDK 21各LTS版本的核心新特性、语法糖的底层反糖机制、JPMS模块系统，掌握版本选型策略

## 目录

1. [JDK版本命名与LTS策略](#1-jdk版本命名与lts策略)
2. [JDK 8里程碑特性](#2-jdk-8里程碑特性)
3. [JDK 11关键升级](#3-jdk-11关键升级)
4. [JDK 17现代化基石](#4-jdk-17现代化基石)
5. [JDK 21最新LTS特性](#5-jdk-21最新lts特性)
6. [Java语法糖全解析](#6-java语法糖全解析)
7. [JPMS模块系统详解](#7-jpms模块系统详解)
8. [版本升级迁移指南](#8-版本升级迁移指南)
9. [废弃API与移除功能](#9-废弃api与移除功能)
10. [版本选型建议](#10-版本选型建议)

---

## 1. JDK版本命名与LTS策略

### 1.1 版本命名规范

| 阶段 | 格式 | 示例 |
|------|------|------|
| 早期（JDK 1.0 ~ 1.8） | `JDK 1.x`，日常简称 JDK x | JDK 8 = Java 8 |
| 新版（JDK 9 及以后） | 直接版本号标识 | JDK 11、17、21 |

### 1.2 LTS vs 非LTS

| 维度 | LTS长期支持版 | 非LTS短期版 |
|------|-------------|------------|
| 发布周期 | 每 2~3 年 | 每 6 个月 |
| 支持周期 | 8 年以上商业维护 | 仅 6 个月 |
| 定位 | 企业生产唯一推荐 | 尝鲜新特性，禁止线上使用 |
| 代表版本 | JDK 8、11、17、21 | JDK 9/10、12~16、18~20 |

> 💡 生产环境只选 LTS 版本，非 LTS 仅供本地技术调研。

---

## 2. JDK 8里程碑特性

> JDK 8（2014年）是Java史上最具里程碑意义的版本，奠定函数式编程基础。

### 2.1 Lambda表达式（核心）

```java
// 语法：(参数) -> {方法体}
x -> x * 2                                    // 单参数省略括号
(a, b) -> a + b                               // 单行省略 {} 和 return
(int a, int b) -> { return a + b; }           // 完整写法
```

**底层原理**：`invokedynamic` 指令动态调用，不生成匿名内部类。

### 2.2 四大核心函数式接口

| 接口 | 称法 | 方法签名 | 用途 |
|------|------|----------|------|
| `Consumer<T>` | 消费型 | `void accept(T t)` | 遍历操作 |
| `Supplier<T>` | 供给型 | `T get()` | 工厂方法 |
| `Function<T,R>` | 函数型 | `R apply(T t)` | 类型转换 |
| `Predicate<T>` | 断定型 | `boolean test(T t)` | 条件过滤 |

### 2.3 Stream流式操作

```java
List<String> result = list.stream()
    .filter(s -> s.startsWith("A"))          // 过滤
    .map(String::toUpperCase)                 // 转换
    .sorted()                                 // 排序
    .collect(Collectors.toList());            // 收集

// 并行流（大数据量场景优化）
long count = list.parallelStream().filter(s -> s.length() > 3).count();
```

### 2.4 Optional空指针安全工具

```java
String display = Optional.ofNullable(name).orElse("默认值");
Optional.ofNullable(user).map(User::getAddress).ifPresent(addr -> log.info(addr));
```

### 2.5 接口增强

```java
interface A {
    default void hello() { System.out.println("Hello"); }  // 解决升级兼容
    static void test() {}                                   // A.test() 直接调用
}
```

### 2.6 新日期时间API（`java.time`）

```java
LocalDate today = LocalDate.now();                          // 当前日期
LocalDateTime now = LocalDateTime.now();
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = now.format(fmt);
```

> `SimpleDateFormat` 线程不安全，使用 `DateTimeFormatter` 替代。

### 2.7 CompletableFuture

```java
CompletableFuture.supplyAsync(() -> queryUser())
    .thenApply(user -> enrichInfo(user))
    .thenAccept(result -> save(result))
    .exceptionally(ex -> { log.error("异常", ex); return null; });
```

### 2.8 HashMap底层优化（JDK 7 vs 8）

| 维度 | JDK 1.7 | JDK 1.8 |
|------|---------|---------|
| 底层结构 | 数组 + 链表 | 数组 + 链表 + 红黑树 |
| 插入方式 | 头插法（循环链表风险） | 尾插法 |
| 树化 | 无 | 链表 >= 8 且数组 >= 64 转红黑树 |

---

## 3. JDK 11关键升级

> JDK 11（2018年）聚焦精简 + 性能优化，是 JDK 8 升级过渡首选。

### 3.1 HTTP Client（标准内置）

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://api.xxx.com")).GET().build();
client.sendAsync(req, BodyHandlers.ofString())
    .thenApply(HttpResponse::body).thenAccept(System.out::println);
```

### 3.2 局部变量类型推断（`var`）

```java
var list = new ArrayList<String>();         // ArrayList<String>
var map = new HashMap<String, List<Integer>>();
// 仅局部变量可用，不能用于字段/方法参数/返回值
```

### 3.3 字符串与文件增强

```java
"  ".isBlank();                             // true
"Hello".repeat(3);                          // "HelloHelloHello"
Files.readString(Path.of("data.txt"));      // 读文件
Files.writeString(Path.of("out.txt"), s);   // 写文件
```

> ⚠️ JDK 11 移除 Java EE 模块（JAXB、CORBA），升级需补充 Maven 依赖。

---

## 4. JDK 17现代化基石

> JDK 17（2021年）Oracle 免费商用，是当前全新项目首选 LTS。

### 4.1 密封类（Sealed Classes）

```java
public sealed class Shape permits Circle, Rectangle, Triangle {}
final class Circle extends Shape {}
sealed interface Vehicle permits Car, Truck {}
```

### 4.2 Record记录类

```java
public record User(Long id, String name, String email) {}
// 自动生成构造器、equals、hashCode、toString
User user = new User(1L, "张三", "zhang@example.com");
System.out.println(user.name());            // 访问器，非 getName()
```

### 4.3 Switch表达式增强

```java
String result = switch (status) {
    case 1 -> "待支付";
    case 2, 3 -> "已完成";                  // 多值合并
    case 4 -> {
        log.info("特殊状态");
        yield "特殊";
    }
    default -> "未知";
};

// 模式匹配（JDK 17+）
String info = switch (obj) {
    case Integer i when i > 0 -> "正整数";
    case String s -> "字符串: " + s;
    case null -> "null 值";
    default -> "其他";
};
```

### 4.4 文本块

```java
String json = """
    {
        "name": "张三",
        "age": 30
    }
    """;
```

### 4.5 instanceof模式匹配（JDK 16+）

```java
if (obj instanceof String s) {
    System.out.println(s.length());         // 无需强转
}
if (obj instanceof String s && s.length() > 5) { ... }
```

---

## 5. JDK 21最新LTS特性

> JDK 21（2023年）聚焦并发革命与开发效率。

### 5.1 虚拟线程（Virtual Threads）

```java
// 百万级轻量线程，适合 IO 密集型场景
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        executor.submit(() -> {
            Thread.sleep(100);              // 阻塞时自动让出平台线程
            return task();
        });
    }
}

// 单实例
Thread vt = Thread.startVirtualThread(() -> System.out.println("虚拟线程"));
```

> 💡 虚拟线程适用于 IO 密集型任务（HTTP 调用、DB 查询），不适用于 CPU 密集型计算。

### 5.2 未命名模式与变量

```java
var _ = map.remove(key);                   // 不关心旧值
if (obj instanceof String _) { ... }       // 不关心匹配后的值
```

### 5.3 分代ZGC

```bash
-XX:+UseZGC -XX:+ZGenerational             # JDK 21+ 启用分代 ZGC
```

---

## 6. Java语法糖全解析

> 语法糖是**编译期语法优化**，编译后还原为基础语法，运行期无额外开销。

### 6.1 泛型擦除

```java
// 编译时擦除为原始类型
List<String> list = new ArrayList<>();      // 编译后：List list = new ArrayList();
// List<String> 与 List<Integer> 运行时是同一个 Class 对象
```

### 6.2 自动装箱与拆箱

| 操作 | 源码 | 编译还原 |
|------|------|----------|
| 装箱 | `Integer i = 100;` | `Integer.valueOf(100)` |
| 拆箱 | `int n = i;` | `i.intValue()` |

```java
// 缓存陷阱：Integer 缓存 -128~127
Integer a = 127, b = 127;                  // a == b 为 true（缓存）
Integer c = 128, d = 128;                  // c == d 为 false（新对象）

// NPE 陷阱
Integer x = null;
int y = x;                                 // NullPointerException
```

### 6.3 增强for循环

```java
// 集合：反糖为 Iterator
for (String s : list) { ... }              // 底层 Iterator
// 数组：反糖为普通 for
for (int n : arr) { ... }                  // 底层 for(int i=0;...)

// ⚠️ 遍历时不可增删集合，否则 ConcurrentModificationException
```

### 6.4 其他语法糖速查

| 语法糖 | 反糖原理 | 要点 |
|--------|----------|------|
| 变长参数 | 编译为数组 | 必须在参数最后，一个方法仅一个 |
| try-with-resources | 自动补全 finally + close() | 实现 AutoCloseable |
| 字符串拼接 | JDK 8: StringBuilder；JDK 9+: invokedynamic | 循环内手动用 StringBuilder |
| switch 字符串 | hashCode() + equals() | 有哈希冲突风险 |
| 枚举 | 继承 Enum + 静态常量 | 单例、线程安全 |
| 方法引用 | Lambda 进一步简化 | `obj::method`、`Class::staticMethod` |

### 6.5 Lambda 编译原理

```java
// 非匿名内部类，通过 invokedynamic 指令
list.forEach(item -> System.out.println(item));   // invokedynamic
list.forEach(System.out::println);                // 方法引用简化
```

> 🎯 所有语法糖均在编译期解糖，运行期无特殊逻辑。泛型擦除、装箱拆箱、foreach 为面试最高频。

---

## 7. JPMS模块系统详解

> JPMS（Project Jigsaw）是 JDK 9 推出的模块化规范，解决"类路径地狱"问题。

### 7.1 解决的痛点

| 痛点 | 传统类路径 | JPMS 方案 |
|------|-----------|-----------|
| 依赖冲突 | 同名类无序加载 | 模块隔离，编译期检测 |
| 封装缺失 | 所有类可反射 | 未导出包不可访问 |
| JDK 臃肿 | 全量 JRE | jlink 构建精简运行时 |
| 循环依赖 | 运行时暴露 | 编译期检测 |

### 7.2 模块分类

| 类型 | 来源 | 命名 |
|------|------|------|
| 平台模块 | JDK 自身拆分 | `java.*` 开头 |
| 应用模块 | 开发者编写 | 反向域名 |
| 自动模块 | 传统 Jar 放入模块路径 | 文件名去版本号 |
| 未命名模块 | 类路径下所有内容 | 兼容老项目 |

### 7.3 `module-info.java` 核心指令

```java
module com.example.user {
    exports com.example.user.service;                // 导出包
    exports com.example.user.entity to com.example.web;  // 定向导出
    requires transitive java.sql;                    // 传递依赖
    requires static org.junit.jupiter.api;           // 编译期依赖
    opens com.example.user.entity to org.mybatis;    // 反射开放
    uses com.example.user.service.UserService;
    provides com.example.user.service.UserService
        with com.example.user.service.impl.UserServiceImpl;
}
```

### 7.4 模块路径 vs 类路径

| 维度 | 模块路径 | 类路径 |
|------|---------|--------|
| 依赖管理 | 有序、显式声明 | 无序、隐式 |
| 封装性 | 强封装 | 所有类可访问 |
| 依赖检查 | 编译期 | 运行期报错 |

### 7.5 高频异常

| 异常 | 原因 | 解决 |
|------|------|------|
| `ModuleNotFoundException` | requires 模块不存在 | 检查模块名和路径 |
| `InaccessibleObjectException` | 反射未 opens 的包 | 添加 opens 指令或 `--add-opens` |
| `CyclicDependencyException` | 模块循环依赖 | 拆分公共模块 |

> **核心原则**：显式声明 + 最小权限 + 渐进兼容。新项目优先模块化，老项目渐进改造。

---

## 8. 版本升级迁移指南

### 8.1 迁移检查清单

| 迁移路径 | 检查项 |
|----------|--------|
| 8 → 11 | 补充 javax.xml.bind 等 EE 依赖；替代废弃 API；检查 JPMS 反射 |
| 11 → 17 | 替换 Applet；利用 Record/Sealed Class/Switch 表达式优化代码 |
| 17 → 21 | 评估虚拟线程；配置分代 ZGC；验证中间件兼容性 |

### 8.2 Maven 编译配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <release>17</release>
    </configuration>
</plugin>
```

---

## 9. 废弃API与移除功能

| 版本 | 废弃/移除 | 替代方案 |
|------|-----------|----------|
| JDK 9 | Applet API | 无（已淘汰） |
| JDK 11 | Java EE 模块 | Maven 独立引入 |
| JDK 11 | SecurityManager | 最小权限 + 网络隔离 |
| JDK 17 | Applet 彻底移除 | 无 |
| JDK 17 | finalize() | Cleaner / try-with-resources |
| JDK 21 | finalize() 彻底移除 | Cleaner |

---

## 10. 版本选型建议

### 10.1 场景推荐

| 场景 | 推荐版本 | 理由 |
|------|---------|------|
| 老系统维护 | JDK 8 | 兼容性最强 |
| 老系统升级 | JDK 11 | 精简优化，过渡首选 |
| 全新项目 | JDK 17 | 免费商用，语法现代化 |
| 高并发IO | JDK 21 | 虚拟线程革命性提升 |
| 低延迟交易 | JDK 17/21 + ZGC | 极低 GC 停顿 |

### 10.2 版本定位速查

| 版本 | 发布 | 关键词 |
|------|------|--------|
| JDK 8 | 2014 | Lambda、Stream、Optional、新日期API |
| JDK 11 | 2018 | var、HTTP Client、精简模块、ZGC实验 |
| JDK 17 | 2021 | Record、Sealed Class、Switch 增强、免费商用 |
| JDK 21 | 2023 | 虚拟线程、分代ZGC、字符串模板 |

### 10.3 学习路线

```
入门：JDK 8（教程最丰富） → 进阶：JDK 17（新特性） → 前沿：JDK 21（虚拟线程）
```

> 🎯 LTS 是生产唯一选择。JDK 8 入门、JDK 17 新项目、JDK 21 高并发为当前主流路线。
