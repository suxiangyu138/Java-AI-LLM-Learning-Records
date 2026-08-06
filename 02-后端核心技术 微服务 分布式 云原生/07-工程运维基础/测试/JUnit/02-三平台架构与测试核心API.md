# 02-三平台架构与测试核心 API

> JUnit Platform 是引擎中立的总线，Jupiter 是默认引擎与 API 模型，Vintage 是旧测试迁移桥。掌握架构分层与注解/断言/假设三大核心 API，是写出正确测试的前提。

## 📚 目录

1. [三平台架构分层](#1)
2. [Jupiter 注解族](#2)
3. [断言 Assertions](#3)
4. [假设 Assumptions](#4)
5. [显示名称与标签](#5)

## 1. 三平台架构分层

### 1.1 架构图

```text
┌─────────────────────────────────────────────────────┐
│  IDE（IDEA/VS Code） / 构建工具（Maven/Gradle）         │
│  通过 JUnit Platform Launcher API 发起发现与执行        │
├─────────────────────────────────────────────────────┤
│  JUnit Platform（junit-platform-*）                   │
│  · Launcher（编程入口，JUnit 6 起支持 CancellationToken）│
│  · TestEngine SPI（引擎注册：ServiceLoader 发现）       │
│  · 过滤/条件求值/报告/并行执行协调（属平台层编排）        │
├──────────────┬──────────────────┬───────────────────┤
│  Jupiter     │  Vintage（弃用）  │  第三方引擎          │
│  新测试引擎   │  JUnit 3/4 桥    │  TestNG / Selenium │
│  org.junit.  │  org.junit.      │  自定义 TestEngine │
│  jupiter     │  vintage         │  SPI 实现          │
└──────────────┴──────────────────┴───────────────────┘
```

### 1.2 模块依赖速查（JUnit 6 统一版本号 6.x；JUnit 5 为 Platform 1.x + Jupiter 5.x）

| 模块 | 职责 |
|------|------|
| `junit-platform-engine` | 引擎 SPI：`TestEngine`、`TestDescriptor`、`TestExecutionListener` |
| `junit-platform-launcher` | 编程式启动（`LauncherFactory`），JUnit 6 内置 JFR 事件 |
| `junit-platform-console` | ConsoleLauncher 命令行执行器 |
| `junit-jupiter-api` | 测试编写 API（注解、断言、条件） |
| `junit-jupiter-engine` | Jupiter 引擎实现（运行时必带） |
| `junit-jupiter-params` | 参数化测试支持 |
| `junit-jupiter`（聚合器） | 同时引入 api + engine + params |

> 💡 依赖选择：Maven 用户引入 `junit-jupiter`（或直接 `junit-bom` 管理版本）；只用 ConsoleLauncher 的 CI 场景引入 `junit-platform-console-standalone` 单包。

### 1.3 Platform 层的三类职责

1. **发现**：按引擎 SPI 扫描类路径，构建 `TestDescriptor` 树（套件→类→方法）；
2. **执行**：把描述符交给引擎执行，向 `TestExecutionListener` 广播开始/成功/失败事件；JUnit 6 的 `CancellationToken` 允许 Launcher 侧取消运行中的执行；
3. **报告**：统一结果模型（`TestExecutionSummary`），与引擎无关。

## 2. Jupiter 注解族

### 2.1 测试类型注解

| 注解 | 作用 |
|------|------|
| `@Test` | 标准测试方法（非 static、非 abstract） |
| `@ParameterizedTest` | 参数化测试（需 junit-jupiter-params） |
| `@RepeatedTest` | 重复执行 N 次，可注入 `RepetitionInfo` |
| `@TestFactory` | 动态测试工厂，返回 `DynamicNode` 流 |
| `@TestTemplate` | 通用模板，由 `TestTemplateInvocationContextProvider` 决定调用次数（参数化/重复测试的底层） |

### 2.2 生命周期注解

| 注解 | 时机 |
|------|------|
| `@BeforeAll` | 类中所有测试前（static；PER_CLASS 实例模式下可非 static） |
| `@BeforeEach` | 每个测试前 |
| `@AfterEach` | 每个测试后 |
| `@AfterAll` | 类中所有测试后 |

### 2.3 结构组织注解

| 注解 | 作用 |
|------|------|
| `@Nested` | 非静态内部类，树形分组并继承外层实例状态 |
| `@DisplayName` | 人类可读名称（支持占位符 `{displayName}` 等） |
| `@Tag` | 打标签，供构建工具按组筛选 |
| `@TestInstance` | `PER_METHOD`（默认）/`PER_CLASS` 实例生命周期 |
| `@TestMethodOrder` / `@TestClassOrder` | 方法/类执行顺序策略 |

### 2.4 条件与扩展注解

| 注解 | 作用 |
|------|------|
| `@Disabled` | 跳过（带理由），无返回值检查能力（比较 assertAll 弱） |
| `@EnabledOnOs/@DisabledOnOs` | 按操作系统启用/禁用 |
| `@EnabledOnJre/@DisabledOnJre` | 按 JRE 版本（JUnit 6：Java 8-16 常量弃用） |
| `@EnabledIfSystemProperty` 等 | 按系统属性/环境变量 |
| `@EnabledIf` | 自定义脚本条件（JVM 语言/JavaScript/自定义类） |
| `@Timeout` | 超时控制（基于 InvocationInterceptor） |
| `@TempDir` | 自动临时目录 |
| `@ExtendWith` | 声明式注册扩展（类级/方法级） |
| `@RegisterExtension` | 字段级程序化注册扩展 |
| `@Execution` | 覆盖并行执行模式（`SAME_THREAD`/`CONCURRENT`） |
| `@ResourceLock` | 声明共享资源锁，协调并行下的资源访问 |

## 3. 断言 Assertions

### 3.1 基础断言

```java
assertEquals(42, answer);                    // 基本相等（对象用 equals）
assertEquals(3.1415, pi, 0.0001);            // double 比较必须带 delta！
assertNotEquals(1, answer);
assertSame(instance, singleton);             // 引用相等 ==
assertNotNull(service);
assertTrue(condition, "业务说明：必须满足 XX");
assertFalse(condition);
```

**消息参数**：所有断言都支持 `String message` 或 `Supplier<String>` 两个重载——**生产代码优先用 Supplier 延迟求值**（只有断言失败才拼接消息字符串，避免无谓开销）：

```java
assertTrue(stock.isValid(), () -> "库存校验失败: " + stock.snapshot()); // 失败才执行 lambda
```

### 3.2 聚合与异常断言

```java
// assertAll：一次测试多个断言，全部失败时聚合报告（不短路）
assertAll("用户信息完整性",
    () -> assertEquals("zhang", user.getName()),
    () -> assertEquals(30, user.getAge()),
    () -> assertTrue(user.getRoles().contains("ADMIN"))
);

// assertThrows：断言异常类型 + 返回异常对象做进一步校验
IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
    () -> userService.register(null));
assertEquals("用户名不能为空", ex.getMessage());

// assertDoesNotThrow：断言不抛异常（注意：宽断言，慎用）
assertDoesNotThrow(() -> cache.evict("key"));
```

> ⚠️ **陷阱**：`assertThrows` 只校验「方法抛出了指定类型异常」，不校验「之后的状态」——需要接着断言异常后的系统状态（如回滚、资源释放）。

### 3.3 超时断言

```java
assertTimeout(Duration.ofMillis(100), () -> repository.findTop10());   // 超时后同步等待方法结束再失败（不中断）
assertTimeoutPreemptively(Duration.ofMillis(100), () -> service.call()); // 超时立即在另一线程失败（有副作用风险）
```

两者差异：`assertTimeout` 与测试同线程执行，超时不中断被测代码（但会继续等到其完成）；`assertTimeoutPreemptively` 在独立线程执行并可能中断，**线程局部变量、事务上下文可能丢失**——优先用前者，仅对会无限阻塞的代码用后者。

### 3.4 集合与类型断言

```java
assertIterableEquals(List.of(1, 2), actual);        // 顺序敏感的 Iterable 比较
assertLinesMatch(List.of("a.*", "b"), lines);       // 支持正则的行匹配（多行输出断言利器）
assertArrayEquals(new int[]{1, 2}, actual);
assertInstanceOf(RedisCache.class, cache);          // 5.8+，返回类型化引用
```

### 3.5 JUnit 6 空安全

JUnit 6 起全模块使用 **JSpecify** 注解标注参数与返回值可空性：IDE 提示、Kotlin 互操作（`String?` vs `String`）、NullAway/Error Prone 静态检查全部受益。**对普通开发者最直接的变化**：写自定义断言/扩展时参数声明 `@Nullable` 与否会被编译期工具校验。

## 4. 假设 Assumptions

假设不满足时**测试被跳过**（aborted），而不是失败——语义是「当前环境不适用」，不同于断言失败：

```java
assumeTrue("dev".equals(System.getenv("ENV")), "仅 dev 环境运行");
assumeFalse(Database.isReadOnly());

// assumingThat：满足假设的部分执行，不满足则整个测试仍算通过
assumingThat(OS.isLinux(), () -> {
    assertEquals(10, path.getPermissions().length);
});
```

**适用场景**：环境相关测试（时区、编码、外部服务存在性）、版本相关测试（`assumeTrue(Runtime.version().feature() >= 17)`）。与 `@EnabledIf*` 条件注解相比，假设是**代码运行时决策**，可读取被测系统状态；注解是**启动期静态决策**。

## 5. 显示名称与标签

### 5.1 @DisplayName 生成策略

```java
@Test
@DisplayName("库存扣减：余额不足时抛出异常")  // 中文描述，测试报告/IDE 可读性之王
void insufficientStock_throws() { }

// 类级 @DisplayNameGeneration：全局命名策略
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OrderServiceTest { }
```

JUnit 6 提供 `DisplayNameGenerator.Standard`（默认）/`ReplaceUnderscores`（下划线→空格）/`Simple`（去参数列表）及自定义实现。

### 5.2 @Tag 与构建工具过滤

```java
@Tag("slow") @Tag("integration")
@Test void fullSync() { }
```

```xml
<!-- Maven Surefire：只跑 fast 组 -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <groups>fast</groups>
    <excludedGroups>slow,integration</excludedGroups>
  </configuration>
</plugin>
```

```groovy
// Gradle
tasks.test {
    useJUnitPlatform {
        includeTags("fast")
        excludeTags("slow", "integration")
    }
}
```

> 💡 标签命名规范：用**分类维度**（`api`/`db`/`slow`/`flaky`）而非环境维度；组名保持小写、单一词，避免空格导致 CLI 转义问题。

> 🎯 **核心要点**：三平台架构是理解一切后续概念（扩展、并行、动态测试、引擎共存）的地基；日常写测试记住三件事——断言带业务消息、`assertAll` 聚合多个断言、`assertTimeout` 优先于 `assertTimeoutPreemptively`；假设与条件注解是「跳过」语义的两套表达，按运行期/启动期选择。

---

**上一模块**：[01-演进史与版本全景](01-演进史与版本全景.md) ｜ **下一模块**：[03-测试生命周期与嵌套](03-测试生命周期与嵌套测试.md) ｜ **返回总览**：[00-知识体系总览](00-JUnit知识体系总览.md)

**【参考来源】**
- JUnit User Guide（Writing Tests）：https://docs.junit.org/6.1.1/writing-tests/
- JUnit 6.1.1 API 文档（Jupiter）：https://docs.junit.org/6.1.1/api/
