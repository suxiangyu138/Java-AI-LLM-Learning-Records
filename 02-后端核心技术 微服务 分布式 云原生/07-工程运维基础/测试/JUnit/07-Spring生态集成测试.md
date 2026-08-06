# 07-Spring 生态集成测试

> Spring 测试体系以 SpringExtension 桥接 Jupiter，@SpringBootTest 全量启动 + 切片测试定点加载 + Testcontainers 真实依赖，构成 2026 年（Spring Boot 4 + JUnit 6）的标准测试栈。

## 📚 目录

1. [Spring 测试体系架构](#1)
2. [单元级：SpringExtension](#2)
3. [集成级：@SpringBootTest](#3)
4. [切片测试](#4)
5. [Mock 集成：@MockitoBean](#5)
6. [Testcontainers：真实依赖容器化](#6)
7. [上下文缓存与性能](#7)

## 1. Spring 测试体系架构

```text
@SpringBootTest / @WebMvcTest / @DataJpaTest ...
        │
        ▼
Spring TestContext Framework（spring-test）
  ├── TestContextManager —— 管理上下文缓存（全局单例）
  ├── TestExecutionListener —— 事务回滚、依赖注入、缓存清理
  └── SpringExtension（org.springframework.test.context.junit.jupiter）
        │
        ▼
JUnit Jupiter（@ExtendWith(SpringExtension.class)）
```

**Spring Boot 4 测试栈**（`spring-boot-starter-test` 默认内含）：

| 组件 | 版本 |
|------|------|
| JUnit Jupiter | **6.x**（Vintage 已移除） |
| AssertJ | 3.26+ |
| Mockito | 5.x |
| Hamcrest | 2.2 |
| JSONassert / JsonPath | 2.0+ / 2.9+ |

## 2. 单元级：SpringExtension

不启动上下文，只验证 Spring 组件装配（DI 配置正确性）：

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class, DataConfig.class})
class ConfigTest {

    @Autowired
    OrderService orderService;

    @Test
    void beans_are_wired() {
        assertNotNull(orderService);
        assertNotNull(orderService.getRepo());
    }
}
```

- **无 SpringBootTest**：只加载指定配置类，速度快；
- 也可用组合注解 `@SpringJUnitConfig(classes = AppConfig.class)` 一步到位；
- 适合「配置类正确性」冒烟测试，比全量启动快一个数量级。

## 3. 集成级：@SpringBootTest

### 3.1 全量上下文

```java
@SpringBootTest
class OrderFlowIntegrationTest {

    @Autowired
    OrderService orderService;

    @Test
    void order_flow_with_real_beans() {
        // 真实 Bean、真实 DB（或测试容器）、真实 MQ
    }
}
```

| 属性 | 作用 |
|------|------|
| `webEnvironment = WebEnvironment.MOCK`（默认） | 不启动 Web 服务器，用 MockMvc |
| `webEnvironment = RANDOM_PORT` | 启动随机端口，配 `TestRestTemplate`/WebTestClient |
| `webEnvironment = DEFINED_PORT` | 固定端口（CI 并发冲突风险） |
| `webEnvironment = NONE` | 纯服务层集成 |

### 3.2 Boot 4 的 AOT/Native 测试

```java
@SpringBootTest(AOT = true)          // Boot 4：AOT 编译路径下运行测试
class NativeReadyTest { }
```

Boot 4 支持原生镜像（GraalVM）测试模式，测试也在 AOT 应用模式下执行，验证真实部署形态。

### 3.3 事务回滚测试

```java
@SpringBootTest
@Transactional                      // 每个测试结束后自动回滚，测试间数据零污染
class RepoTest {

    @Test
    void insert_then_rollback() {
        repo.save(new User("zhang"));
        assertEquals(1, repo.count());   // 测试内可见，结束后回滚
    }
}
```

> ⚠️ **@Transactional 的局限**：只回滚事务型 DAO 的写入；异步任务、消息消费、独立线程中的写入不回滚；HTTP 调用场景下 MockMvc 请求在**同一线程**执行才在事务内——`RANDOM_PORT` 真实服务器模式不受事务保护。

## 4. 切片测试

切片测试（Test Slicing）只加载 Web/JPA/JSON 等某一层的 Bean，比全量启动快得多：

| 切片注解 | 加载内容 |
|---------|---------|
| `@WebMvcTest(Controller.class)` | MVC 层：Controller + Filter + 拦截器（**不加载 Service/DAO**） |
| `@DataJpaTest` | JPA 层：Repository + 嵌入式 DB（默认替换为 H2 内存库） |
| `@JsonTest` | JSON 序列化/反序列化测试 |
| `@RestClientTest` | RestClient 配置测试 |
| `@DataRedisTest` / `@DataMongoTest` 等 | 各持久化切片 |

```java
@WebMvcTest(OrderController.class)
class OrderControllerSliceTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean            // Boot 4：替代旧 @MockBean，只 Mock Controller 依赖的 Service
    OrderService orderService;

    @Test
    void get_order_returns_200() throws Exception {
        when(orderService.findById(1L)).thenReturn(new Order(1L));

        mockMvc.perform(get("/orders/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1));
    }
}
```

> ⚠️ Boot 4 注意：切片注解所在包已调整（`org.springframework.boot.test.autoconfigure.*` 迁移），`@WebMvcTest` 未显式指定 Controller 时可能因 Bean 不完整失败——切片测试**必须显式声明被测切片**，且只测「本层契约」，不要期望它覆盖跨层逻辑。

## 5. Mock 集成：@MockitoBean

**Spring Boot 4 重大变更**：`@MockBean`/`@SpyBean` 已移除，替换为 Spring Framework 的 Bean 覆盖 API：

```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PaymentIntegrationTest {

    @MockitoBean
    private PaymentClient paymentClient;      // 覆盖容器中的 PaymentClient Bean

    @MockitoSpyBean
    private AuditService auditService;        // 真实 Bean 的 Spy 版本

    @Test
    void pay_mocks_external() {
        when(paymentClient.charge(any())).thenReturn(true);
        assertTrue(paymentService.pay(100));
        verify(paymentClient).charge(any());
    }
}
```

| 旧（Boot 3.x） | 新（Boot 4） |
|---------------|-------------|
| `org.springframework.boot.test.mock.mockito.MockBean` | `org.springframework.test.context.bean.override.mockito.MockitoBean` |
| `@SpyBean` | `@MockitoSpyBean` |
| `MockReset`（boot 包） | `org.springframework.test.context.bean.override.mockito.MockReset` |
| `answer` 属性 | `answers` 属性 |

迁移可用 OpenRewrite 配方 `ReplaceMockBeanAndSpyBean` 自动改写。**纯单元测试不受影响**：标准 Mockito `@Mock`/`@InjectMocks` + `@ExtendWith(MockitoExtension.class)` 无需任何改动。

## 6. Testcontainers：真实依赖容器化

### 6.1 基础用法

```java
@Testcontainers
class OrderRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    // Boot 4：@ServiceConnection 自动配置 DataSource 连接
    @ServiceConnection
    @Container
    static RedisContainer redis = new RedisContainer("redis:7.4-alpine");

    @Test
    void db_works() { /* 真实 Postgres + Redis */ }
}
```

### 6.2 为什么 Testcontainers 成为一等公民

- **真实性**：与生产同版本数据库/中间件，H2 模拟不出的 SQL 方言、锁行为、JSONB 特性全部真实覆盖；
- **Boot 4 支持**：`@ServiceConnection` 把容器连接信息自动映射到 `DataSource`/`RedisConnectionFactory` 等，零样板配置；
- **CI 一致**：本地与流水线共享同一容器镜像，环境漂移最小化。

### 6.3 实战模式

| 模式 | 适用 |
|------|------|
| 单测不启容器 | 纯单元测试（Mock/Fake），容器只用于 `*IT` 集成测试 |
| `maven-failsafe-plugin` 管理 IT | 集成测试单独阶段运行，`integration-test`/`verify` 生命周期 |
| 容器复用（`withReuse(true)`） | 本地开发提速，CI 中关闭 |
| 多容器（`GenericContainer` 组合） | 全链路（PG + Redis + Kafka）冒烟 |

> ⚠️ 注意：Testcontainers 需要 Docker 守护进程——CI 上必须配置 Docker 环境（GitHub Actions 内置），无 Docker 环境用嵌入式替代并显式标注降级策略。

## 7. 上下文缓存与性能

### 7.1 上下文缓存机制

Spring TestContext 按「配置组合」缓存上下文（`ApplicationContext` 缓存是 JVM 级全局单例）：

- 相同 `@ContextConfiguration`/配置组合 → 复用同一上下文，**不重复启动**；
- 上下文不满足时销毁重建（代价高：每次约 1-5 秒 + 全部 Bean 初始化）。

### 7.2 性能调优要点

| 手段 | 效果 |
|------|------|
| 切片测试替代全量启动 | 启动时间降低 50-90% |
| 减少上下文变体 | 每个测试类尽量复用同一配置组合（上下文缓存命中率 ↑） |
| `@DirtiesContext` 少用 | 每次使用强制重建上下文，性能杀手；用事务回滚替代 |
| 并行执行 + 上下文预热 | `junit.jupiter.execution.parallel.*` 配合切片分层 |
| `@MockitoBean` 只 mock 外部边界 | 减少容器内 Bean 数量，缩短启动 |

> 🎯 **核心要点**：Spring 测试分三层——SpringExtension 验证装配、切片测试验证单层契约、@SpringBootTest + Testcontainers 验证真实集成；2026 年的正确姿势是「**切片测试为主力、容器集成做验收**」，并牢记 Boot 4 三大迁移点：JUnit 6 默认、@MockBean → @MockitoBean、Vintage 移除。

---

**上一模块**：[06-Mock 与依赖隔离](06-Mock与依赖隔离.md) ｜ **下一模块**：[08-构建工具与 CI 集成](08-构建工具与CI集成.md) ｜ **返回总览**：[00-知识体系总览](00-JUnit知识体系总览.md)

**【参考来源】**
- OpenRewrite：Replace @MockBean and @SpyBean（Boot 4）：https://docs.openrewrite.org/recipes/java/spring/boot4/replacemockbeanandspybean
- Spring Boot 4 测试迁移（CSDN 译文）：https://blog.csdn.net/wayle123/article/details/160830525
- Spring Boot 官方测试文档：https://docs.spring.io/spring-boot/testing
