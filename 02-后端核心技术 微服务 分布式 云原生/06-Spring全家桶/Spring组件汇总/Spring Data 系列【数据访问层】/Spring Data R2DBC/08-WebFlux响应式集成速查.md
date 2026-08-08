# 08 WebFlux 响应式集成速查

> 全链路非阻塞的完整姿势、阻塞陷阱排查、响应式链路（Controller→Service→Repository）、超时/限流/熔断联动、响应式测试——"R2DBC 在 WebFlux 里的正确打开方式"完整手册

---

## 📚 目录

1. [全链路非阻塞架构](#1-全链路非阻塞架构)
2. [响应式三件套：Controller/Service/Repository](#2-响应式三件套controllerservicerepository)
3. [阻塞陷阱：性能第一杀手](#3-阻塞陷阱性能第一杀手)
4. [超时、限流与熔断联动](#4-超时限流与熔断联动)
5. [响应式测试](#5-响应式测试)
6. [常见问题排错](#6-常见问题排错)

---

## 1. 全链路非阻塞架构

**通俗**：R2DBC 的价值前提是"整条链路都是非阻塞的"——从 HTTP 请求进来，到数据库查询，到第三方调用，没有一环"占着线程等"。

```text
Netty（WebFlux）──► WebClient（第三方 HTTP）──► R2DBC（数据库）
    ▲                    ▲                        ▲
    │  全链路非阻塞：一个线程同时服务 N 个请求     │
    │  （IO 等待期间线程去处理别的请求）            │
    └──────────────────┴────────────────────────┘
```

| 环节 | 阻塞（反模式） | 非阻塞（正解） |
|------|---------------|---------------|
| Web 框架 | Spring MVC | **WebFlux**（`spring-boot-starter-webflux`） |
| HTTP 客户端 | RestTemplate / Feign（同步） | **WebClient**（响应式） |
| 数据库 | JDBC / JPA | **R2DBC** |
| Redis | RedisTemplate（同步） | **ReactiveRedisTemplate** |
| MQ | 同步生产者 | 响应式客户端（RocketMQ reactive / Pulsar reactive） |
| 连接池 | Hikari | r2dbc-pool |

> 🎯 面试必答：**"什么项目适合 WebFlux + R2DBC？"**——答：**IO 密集 + 高并发 + 链路可控**（网关/聚合服务/实时推送/数据服务）；业务逻辑重、关联复杂、团队不熟响应式的项目**不建议**（复杂度收益不匹配）；**加分句**：WebFlux + R2DBC 是"同一套非阻塞心智模型"的全栈组合，缺一不可——混 MVC + R2DBC 意义减半，混 WebFlux + JDBC 等于没换。

## 2. 响应式三件套：Controller/Service/Repository

### 2.1 Controller（直接返回响应式类型）

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping("/{id}")
    public Mono<Product> get(@PathVariable Long id) {              // ✅ 返回 Mono
        return productService.getProduct(id);
    }

    @GetMapping
    public Flux<Product> list(@RequestParam(required = false) String category) {
        return productService.search(category);
    }

    @PostMapping
    public Mono<Product> create(@RequestBody Product p) {          // ✅ 非阻塞
        return productService.create(p);
    }
}
```

| 铁律 | 说明 |
|------|------|
| **控制器禁止 `.block()`** | 返回 Mono/Flux，让 WebFlux 处理订阅 |
| **禁止 `Thread.sleep`** | 用 `Mono.delay`（非阻塞等待） |
| **禁止阻塞 IO** | 文件读写用 `Flux.using` + 异步 IO |
| 错误处理 | `onErrorReturn`/`onErrorResume` 在 Service 层（事务外） |
| 请求体 | `Mono<RequestBody>`（WebFlux 的 body 也是响应式） |

### 2.2 Service（事务边界 + 编排）

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ReactiveRedisTemplate<String, Product> redisTemplate;   // 响应式 Redis

    @Transactional(readOnly = true)                       // 读事务（管道内）
    public Mono<Product> getProduct(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("商品不存在")));
    }

    // 响应式编排：查缓存 → miss → 查库 → 回填
    public Mono<Product> getWithCache(Long id) {
        return redisTemplate.opsForValue().get("product:" + id)      // 缓存
                .switchIfEmpty(productRepository.findById(id)         // miss 查库
                        .flatMap(p -> redisTemplate.opsForValue()
                                .set("product:" + id, p, Duration.ofMinutes(10))
                                .thenReturn(p)));
    }

    @Transactional
    public Mono<Product> updatePrice(Long id, BigDecimal price) {
        return productRepository.findById(id)
                .flatMap(p -> {
                    p.setPrice(price);
                    return productRepository.save(p);       // 显式更新（无脏检查）
                });
    }
}
```

> ⚠️ **事务边界注意**：`@Transactional(readOnly = true)` 也是事务（占用连接）——**纯读方法可以不用 @Transactional**（单查询天然原子）；事务只包"多写/跨表一致性"（[06 篇](06-响应式事务与并发控制速查.md)）。

## 3. 阻塞陷阱：性能第一杀手

**通俗**：响应式链路里混入任何阻塞调用，就像高速路上突然出现一个红绿灯——**所有车（线程）都会堵在它面前**。

### 3.1 五大阻塞陷阱

| # | 陷阱 | 症状 | 正解 |
|---|------|------|------|
| ① | **`.block()` 偷懒同步** | 调用线程被占（往往还是 Netty 事件循环线程）→ 全服务卡死 | 响应式贯穿到底 |
| ② | **Thread.sleep / 同步等待** | 同上 | `Mono.delay` / `Flux.interval` |
| ③ | **同步 IO**（文件/网络） | 线程阻塞在 IO | 异步 IO 或换到 boundedElastic（仅限非热点） |
| ④ | **混入 JDBC/JPA 调用** | 数据库环节阻塞 | 全量 R2DBC（或接受该链路为阻塞段） |
| ⑤ | **CPU 密集计算**（加密/压缩/大 JSON） | 占住事件循环线程 | `publishOn(Schedulers.parallel())` 单独调度 |

```java
// 反模式：在 Netty 事件循环上 block（灾难）
@GetMapping("/{id}")
public Mono<Product> get(@PathVariable Long id) {
    return Mono.fromSupplier(() -> productRepository.findById(id).block())  // ❌
            // 事件循环线程被占 → 所有请求排队
}

// 正解：直接返回响应式
public Mono<Product> get(@PathVariable Long id) {
    return productRepository.findById(id);   // ✅
}
```

> 🎯 面试必答：**"WebFlux 里 block() 为什么是灾难？"**——WebFlux 的请求处理跑在 **Netty 事件循环线程**（数量 = CPU 核数，如 8 个）上；`.block()` 会让事件循环线程**原地等待**——8 个线程被 8 个慢请求占完，整个服务对一切请求失去响应（比线程池模型更脆，因为事件循环线程同时还要管网络收发）；**铁律：事件循环线程上永远不阻塞**。

### 3.2 排查方法

| 手段 | 做法 |
|------|------|
| 代码审查 | 搜 `.block()` / `Thread.sleep` / `RestTemplate` / `RedisTemplate` / `JdbcTemplate` |
| 线程名观察 | 日志里线程名 `reactor-http-nio-*`（事件循环）上出现慢操作 = 可疑 |
| 压测对比 | 同场景 MVC vs WebFlux 对比线程占用（[07 篇](07-性能优化与批量速查.md) 1 节） |
| Actuator | 线程 dump 看事件循环线程卡在哪 |

## 4. 超时、限流与熔断联动

**通俗**：响应式服务的容错组件要"响应式版"的——同步的熔断/限流库会自己阻塞，反而破坏非阻塞。

### 4.1 响应式超时

```java
// 数据库查询超时（R2DBC 侧）
@Query("select * from product where id = :id")
Mono<Product> findByIdSlow(...);            // SQL 层超时由驱动/数据库参数控制

// 管道超时（应用侧，响应式标准姿势）
public Mono<Product> getProductWithTimeout(Long id) {
    return productRepository.findById(id)
            .timeout(Duration.ofSeconds(3))                 // 3 秒超时
            .onErrorResume(TimeoutException.class,
                    e -> Mono.error(new BizException("查询超时，请稍后重试")));
}

// 带默认值的超时兜底
.timeout(Duration.ofSeconds(2)).onErrorReturn(defaultProduct);
```

### 4.2 响应式限流/熔断（Resilience4j 响应式版）

```java
// Resilience4j 支持响应式（Mono/Flux 包裹，非阻塞）
@Configuration
public class ResilienceConfig {
    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> reactiveCb() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(5))
                        .build())
                .build());
    }
}

// 使用：熔断 + 降级（响应式管道内）
@CircuitBreaker(name = "productDb", fallbackMethod = "fallback")
public Mono<Product> getProductSafe(Long id) {
    return productRepository.findById(id);
}

public Mono<Product> fallback(Long id, Throwable t) {
    return Mono.just(cachedProduct(id));     // 兜底：缓存/默认值（降级）
}
```

> 💡 与 [名词系列-容错](../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/03-容错保护：熔断、降级与雪崩.md) 的联动：**超时/熔断/降级/限流在响应式下全部是"管道操作符"**（timeout/retryWhen/onErrorResume + Resilience4j 响应式适配）——概念不变，载体从"线程拦截"变成"管道包装"；**绝不能用同步熔断库**（其内部线程池阻塞会破坏非阻塞）。

## 5. 响应式测试

### 5.1 StepVerifier（响应式断言标准姿势）

```java
// Repository 测试
@SpringBootTest
class ProductRepositoryTest {

    @Autowired ProductRepository productRepository;

    @Test
    void findByName_shouldReturn() {
        StepVerifier.create(productRepository.save(new Product("手机")))
                .assertNext(p -> assertThat(p.getId()).isNotNull())
                .verifyComplete();

        StepVerifier.create(productRepository.findByName("手机"))
                .assertNext(p -> assertThat(p.getPrice()).isNotNull())
                .verifyComplete();
    }
}
```

| StepVerifier 方法 | 语义 |
|-------------------|------|
| `.expectNext(x)` / `.assertNext(断言)` | 断言下一条 |
| `.expectComplete()` / `.verifyComplete()` | 断言正常完成 |
| `.expectError(类型)` / `.verifyError()` | 断言异常 |
| `.expectNextCount(n)` | 断言条数 |
| `.thenAwait(Duration)` | 模拟时间推进（测试 delay 逻辑） |

### 5.2 数据库测试

| 姿势 | 说明 |
|------|------|
| 内嵌 H2（r2dbc-h2） | 快速单测（方言差异注意：MySQL 语法别在 H2 上测） |
| Testcontainers | **真库测试**（r2dbc-mysql 容器），关键 SQL 必测 |
| `@Transactional` 测试 | 响应式测试默认**不自动回滚**（与 JPA 测试不同！）——测试后手动清理或容器隔离 |

> ⚠️ 响应式测试三坑：① **没有 @DataJpaTest 等价物**（无 JPA 切片，用 @SpringBootTest + r2dbc-h2/容器）；② **事务不回滚**（响应式事务的提交在管道完成时，测试框架无法像 JPA 一样自动回滚——用容器/清理）；③ 断言必须 StepVerifier（`block()` 在测试里可用但会掩盖时序问题）。

## 6. 常见问题排错

| 症状 | 根因 | 解法 |
|------|------|------|
| 全服务卡死（无响应） | 事件循环线程被 block/阻塞调用占满 | 搜 `.block()`/同步 IO（第 3 节） |
| 数据没提交 | 事务方法返回但调用方没订阅完整 | 检查调用链是否完整订阅（[06 篇](06-响应式事务与并发控制速查.md)） |
| 并发反而更差 | 链路混 JDBC/RestTemplate | 全链路非阻塞改造 |
| 事务回滚失败 | 异常被 onErrorReturn 吞掉 | 事务内不吞异常（[06 篇](06-响应式事务与并发控制速查.md) 4 节） |
| 测试断言超时 | StepVerifier 用法/时序 | 用 expectNext 系列 + thenAwait |
| 返回 500 但无日志 | 错误被管道吞/未订阅 | onErrorResume 记录 + 全局错误处理器 |
| 内存增长 | flatMap 无并发限制 | `.flatMap(..., concurrency)` / `.concatMap`（[07 篇](07-性能优化与批量速查.md) 5 节） |

> 🎯 排错口诀：**"卡死查 block、丢数据查订阅、并发差查阻塞混入"**——响应式三问，覆盖 90% 的线上事故。

---

**下一模块**：[09-集成地图与常见问题](09-集成地图与常见问题.md)　**返回总览**：[00-组件总览](00-Spring Data R2DBC组件总览.md)

**【参考来源】**：[Spring WebFlux 官方文档](https://docs.spring.io/spring-framework/reference/web/webflux.html)、[Reactor 官方文档](https://projectreactor.io/docs/core/release/reference/)、[Resilience4j 响应式支持文档](https://resilience4j.readme.io/docs/getting-started-3)、[R2DBC + WebFlux 官方示例](https://docs.spring.io/spring-data/r2dbc/reference/)
