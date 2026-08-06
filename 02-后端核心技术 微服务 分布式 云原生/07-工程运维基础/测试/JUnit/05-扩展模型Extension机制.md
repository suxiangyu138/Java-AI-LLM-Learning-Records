# 05-扩展模型 Extension 机制

> Extension 是 JUnit 5/6 的灵魂：替代 JUnit 4 的 Runner/Rule，用组合式回调实现测试前中后的一切横切逻辑——资源管理、超时、Mock 注入、并行协调。掌握注册方式与回调接口，即可打造自己的测试基础设施。

## 📚 目录

1. [为什么是 Extension](#1)
2. [三种注册方式](#2)
3. [生命周期回调接口](#3)
4. [内置实用扩展](#4)
5. [条件扩展 ExecutionCondition](#5)
6. [并行执行配置](#6)
7. [扩展开发最佳实践](#7)

## 1. 为什么是 Extension

| 维度 | JUnit 4 Rule/Runner | JUnit 5/6 Extension |
|------|---------------------|---------------------|
| 组合性 | 一个 Runner 只能一个；Rule 顺序不可控 | 任意多个扩展按序叠加 |
| 作用域 | 类级为主 | 方法级/类级/参数级/全局自动发现 |
| 与生命周期交互 | 间接 | 回调接口直接锚定各阶段 |
| 扩展点 | 内置固定 | SPI 接口开放，可自定义 |

> 💡 Extension 设计哲学：**横切逻辑只写一次**（如开启事务、注入 Mock、记录耗时），测试方法保持纯净——只描述「做什么」，不描述「环境怎么搭」。

## 2. 三种注册方式

### 2.1 声明式 @ExtendWith（最常用）

```java
@ExtendWith(MockitoExtension.class)                     // 类级
class UserServiceTest {
    @Test
    @ExtendWith(TimingExtension.class)                  // 方法级（可叠加多个）
    void create() { }
}
```

### 2.2 程序化 @RegisterExtension（字段级，可编程配置）

```java
class TempDirTest {

    @RegisterExtension
    static final ServerExtension SERVER = new ServerExtension("config/prod.yml");  // static：注册时构造

    @RegisterExtension
    final DataSourceExtension ds = new DataSourceExtension();                       // 实例：每测试实例创建

    @Test
    void connect() { SERVER.health(); }
}
```

- **static 字段扩展**：在 `@BeforeAll` 阶段前后生效，可做全局资源（如测试服务实例）；
- **实例字段扩展**：随测试实例创建，可访问实例状态，作用域更细；
- 比 `@ExtendWith` 灵活之处：构造参数、条件注册、按上下文动态选择。

### 2.3 自动发现（全局注册）

```properties
# junit-platform.properties
junit.jupiter.extensions.autodetection.enabled=true
```

配合 `META-INF/services/org.junit.jupiter.api.extension.Extension` 文件注册全局扩展（如统一超时、耗时记录）。**慎用**：全局注册对仓库所有测试生效，隐蔽性强，通常只在基建类扩展（覆盖率 hook、环境探测）使用。

### 2.4 注册顺序

扩展按「类级先于方法级、声明顺序先后」执行回调；`@RegisterExtension` 与 `@ExtendWith` 混用时顺序不保证——**不要在多个扩展间隐式依赖顺序**；必须有序时用单一组合扩展内部编排。

## 3. 生命周期回调接口

### 3.1 回调全家桶

| 接口 | 触发时机 | 典型用途 |
|------|---------|---------|
| `BeforeAllCallback` | 类中首个测试前 | 全局资源启动、环境检查 |
| `BeforeEachCallback` | 每个测试前 | 事务开启、数据准备 |
| `BeforeTestExecutionCallback` | BeforeEach 后、测试体前 | 精确计时起点 |
| `AfterTestExecutionCallback` | 测试体后、AfterEach 前 | 计时终点、状态快照 |
| `AfterEachCallback` | 每个测试后 | 回滚、清理 |
| `AfterAllCallback` | 类中末个测试后 | 全局资源释放 |
| `TestExecutionExceptionHandler` | 测试抛异常时 | 吞掉/转换特定异常（慎用） |
| `ParameterResolver` | 方法参数解析 | Mock 注入、自定义对象构造 |
| `TestInstancePostProcessor` | 实例创建后 | 字段注入 |
| `TestInstanceFactory` | 实例创建前 | 定制实例构造逻辑 |
| `TestTemplateInvocationContextProvider` | 模板测试 | 参数化/重复测试的底层 |
| `InvocationInterceptor` | 每次调用 | 超时、重试、鉴权包装（@Timeout 的实现基础） |
| `ExecutionCondition` | 执行前 | 条件跳过 |
| `TestWatcher` | 测试结束时 | 监听结果（截图、失败上报） |

### 3.2 一个完整示例：事务扩展

```java
public class TransactionExtension implements BeforeEachCallback, AfterEachCallback {

    private final TransactionManager txManager = new TransactionManager();

    @Override
    public void beforeEach(ExtensionContext context) {
        txManager.begin();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (txManager.isRollbackOnly()) {
            txManager.rollback();
        } else {
            txManager.commit();
        }
    }
}
```

### 3.3 InvocationInterceptor：拦截每次调用（5.5+，6.x 稳定）

```java
public class RetryExtension implements InvocationInterceptor {
    @Override
    public <T> T interceptTestClassConstructor(
            Invocation<T> invocation, ReflectiveInvocationContext<Constructor<T>> ctx,
            ExtensionContext extCtx) throws Throwable {
        return intercept(invocation);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> ctx,
                                    ExtensionContext extCtx) throws Throwable {
        intercept(invocation);
    }

    private <T> T intercept(Invocation<T> invocation) throws Throwable {
        try {
            return invocation.proceed();           // 首次执行
        } catch (AssertionError e) {
            return invocation.proceed();           // 失败重试一次（仅演示，勿用于业务测试）
        }
    }
}
```

`InvocationInterceptor` 是 JUnit 6 中 `@Timeout` 的实现机制，也是重试、限流、授权检查类扩展的标准锚点。

## 4. 内置实用扩展

### 4.1 @TempDir：自动清理临时目录

```java
@Test
void file_roundtrip(@TempDir Path dir) throws IOException {
    Path f = dir.resolve("data.txt");
    Files.writeString(f, "hello");
    assertEquals("hello", Files.readString(f));
}

class StaticTempDirTest {
    @TempDir
    static Path sharedDir;        // static：整个类共享一个目录（测试间不隔离！）
}
```

- 方法参数注入：每个测试独立目录，**测试结束后自动删除**（JUnit 6.1 可配置清理失败策略）；
- static 字段：类级共享，注意并行测试下的资源冲突；
- 支持 `@TempDir(dir = "/tmp/base")` 指定父目录。

### 4.2 @Timeout：超时保护

```java
@Test
@Timeout(5)                                  // 秒
void slow_operation() { }

@Test
@Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
void fast_enough() { }

@Timeout(10)                                 // 类级：应用到所有测试
class SlowClassTest { }
```

- 全局默认超时：`junit.jupiter.execution.timeout.default` 属性；
- 与 `assertTimeout` 不同：`@Timeout` 对**生命周期方法也生效**（BeforeEach 等），并在超时后实际**中断线程**（可配置线程池）；
- 注意：超时后的中断需要被测代码响应中断（`InterruptedException`），阻塞式 I/O 可能不可中断。

### 4.3 @ResourceLock：并行下的共享资源协调

```java
@Test
@ResourceLock("system-properties")           // 声明共享资源
void reads_sysprops() { }

@Test
@ResourceLock(value = "db.orders", mode = ResourceLock.ResourceAccessMode.READ_WRITE)
void writes_orders() { }
```

## 5. 条件扩展 ExecutionCondition

```java
public class EnvCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        boolean onProd = "prod".equals(System.getenv("DEPLOY_ENV"));
        return onProd
            ? ConditionEvaluationResult.disabled("生产环境跳过破坏性测试")
            : ConditionEvaluationResult.enabled("非生产环境");
    }
}
```

JUnit 6 中 JRE 条件注解变化：`@EnabledOnJre(JRE.JAVA_8..16)` 常量已弃用，条件恒不满足（Java 17 基线）；`@EnabledOnOs`、`@EnabledIfSystemProperty`、`@EnabledIfEnvironmentVariable`、`@EnabledIf`（脚本）行为不变。

## 6. 并行执行配置

### 6.1 开启与模式

```properties
# junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
```

| 参数 | 取值 | 默认 |
|------|------|------|
| `parallel.enabled` | true/false | false（默认串行） |
| `parallel.mode.default` | concurrent / same_thread | same_thread |
| `parallel.mode.classes.default` | concurrent / same_thread | 继承 mode.default |
| `parallel.config.strategy` | dynamic（默认）/ fixed / custom | dynamic |
| `parallel.config.dynamic.factor` | 0-1+ | 1.0（并行度 = CPU 数 × factor） |
| `parallel.config.fixed.parallelism` | 正整数 | — |
| `parallel.config.executor-service` | **FORK_JOIN_POOL / WORKER_THREAD_POOL（6.1 新增）** | FORK_JOIN_POOL |

### 6.2 粒度控制与陷阱

```java
@Test
@Execution(ExecutionMode.SAME_THREAD)          // 强制串行（共享状态测试）
void legacy_shared_state() { }

@Test
@Execution(ExecutionMode.CONCURRENT)
void parallel_ok() { }
```

**关键陷阱**（官方文档明确列出）：

1. **PER_CLASS 或使用 MethodOrderer 的类，方法不会自动并发**——必须显式 `@Execution(CONCURRENT)`；
2. 并行度 ≠ 最大线程数：ForkJoinPool 可能额外派生线程，需限制 `max-pool-size` 获得严格上限；
3. 静态状态、`System` 属性、固定端口、共享文件是最常见的并行污染源——用 `@ResourceLock` 声明化；
4. `@TempDir` static 字段与并行冲突时改为方法参数注入；
5. 6.1 的 `WORKER_THREAD_POOL` 提供可预测的线程池行为，对受管环境（容器配额）更友好。

> 💡 并行执行优先级：**先保证测试隔离（无共享可变状态），再开并行**。隔离性验证手段：随机顺序 + 单线程跑全绿，再开并行找并发污染。

## 7. 扩展开发最佳实践

| 准则 | 说明 |
|------|------|
| 单一职责 | 一个扩展只做一件事（事务、超时、注入分开） |
| 线程安全 | 扩展实例可能是共享的，内部状态用 ThreadLocal/上下文存储 |
| 使用 Store 传递状态 | `ExtensionContext.Store`（命名空间隔离）而非扩展字段，JUnit 6 用空安全的 `computeIfAbsent` |
| 失败要明确 | 扩展异常会吞掉测试——记录根因后 rethrow |
| 文档化顺序 | 多扩展叠加时注明依赖，避免隐式顺序耦合 |
| 测试扩展本身 | 用 `junit-platform-testkit`（`EngineTestKit`）验证扩展行为 |

```java
// Store 的正确用法：跨回调传递状态
@Override
public void beforeEach(ExtensionContext ctx) {
    long start = System.nanoTime();
    ctx.getStore(NAMESPACE).put("start", start);          // 放入 Store
}

@Override
public void afterEach(ExtensionContext ctx) {
    long start = ctx.getStore(NAMESPACE).get("start", long.class);
    log.info("耗时 {}ms", (System.nanoTime() - start) / 1_000_000);
}
```

> 🎯 **核心要点**：扩展模型的价值不在「写扩展」，而在「用统一抽象管理横切关注点」——内置扩展（@TempDir/@Timeout/@ResourceLock）已覆盖 80% 需求；自定义扩展前先问「能否用组合现有扩展实现」；并行执行是 6.1 之后受管环境的推荐路径，但隔离性永远是并行化的前置条件。

---

**上一模块**：[04-参数化测试全解析](04-参数化测试全解析.md) ｜ **下一模块**：[06-Mock 与依赖隔离](06-Mock与依赖隔离.md) ｜ **返回总览**：[00-知识体系总览](00-JUnit知识体系总览.md)
