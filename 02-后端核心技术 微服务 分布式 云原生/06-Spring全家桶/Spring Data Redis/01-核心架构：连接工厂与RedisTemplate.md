# 01 核心架构：连接工厂与 RedisTemplate

> 从 `RedisConnectionFactory` 抽象到 `RedisTemplate` 模板引擎——理解 Spring Data Redis 的分层设计（连接层 → 模板层 → 操作层），才能解释"为什么 Lettuce 线程安全、Jedis 不是""为什么 RedisTemplate 能并发使用"等核心问题

---

## 📚 目录

1. [整体分层架构](#1-整体分层架构)
2. [连接工厂：RedisConnectionFactory 抽象](#2-连接工厂redisconnectionfactory-抽象)
3. [Lettuce vs Jedis：两套驱动深度对比](#3-lettuce-vs-jedis两套驱动深度对比)
4. [RedisTemplate 模板引擎](#4-redistemplate-模板引擎)
5. [连接池与共享连接](#5-连接池与共享连接)
6. [Spring Boot 自动配置与属性全解](#6-spring-boot-自动配置与属性全解)
7. [自定义连接工厂场景](#7-自定义连接工厂场景)
8. [常见故障排查](#8-常见故障排查)

---

## 1. 整体分层架构

Spring Data Redis 的整体调用链，自上而下分为四层：

```text
┌──────────────────────────────────────────────────────┐
│ 应用层：业务代码（@Cacheable / opsForValue() / Repository）│
└──────────────────────┬───────────────────────────────┘
┌──────────────────────▼───────────────────────────────┐
│ 模板层：RedisTemplate（线程安全，管理序列化与连接获取/释放） │
│  ├── opsForValue()/opsForHash()/... → 类型化 Operations │
│  └── execute(RedisCallback) → 底层命令执行回调          │
└──────────────────────┬───────────────────────────────┘
┌──────────────────────▼───────────────────────────────┐
│ 连接层：RedisConnectionFactory 抽象接口                │
│  ├── LettuceConnectionFactory（默认，netty 多路复用）    │
│  └── JedisConnectionFactory（直连模型，BIO）            │
└──────────────────────┬───────────────────────────────┘
┌──────────────────────▼───────────────────────────────┐
│ 传输层：Redis 服务器（单机 / 哨兵 / 集群）              │
└──────────────────────────────────────────────────────┘
```

> 🎯 **核心要点**：模板层持有的是 `RedisConnectionFactory` 引用而非连接本身——每次命令执行都是"工厂借连接 → 执行回调 → 归还连接"的短生命周期模式。这个设计让 RedisTemplate 天然线程安全，也让"连接泄漏"成为最经典的使用错误。

## 2. 连接工厂：RedisConnectionFactory 抽象

```java
public interface RedisConnectionFactory extends PersistenceExceptionTranslator {
    RedisConnection getConnection();                    // 借连接（同步阻塞）
    RedisClusterConnection getClusterConnection();      // 集群专用连接
    boolean getConvertPipelineAndTxResults();           // 管道/事务结果是否转换
    RedisConnection getReactiveConnection();            // 响应式连接（4.x 增强）
    // ... 观测相关（Micrometer 支持）
}
```

**工厂的三大职责：**

| 职责 | 说明 | 对应实现机制 |
|------|------|-------------|
| 创建/池化连接 | 管理底层驱动连接的生命周期 | Lettuce 共享连接 / Jedis 池 |
| 异常翻译 | 将驱动异常翻译为 Spring DataAccessException | `translateExceptionIfPossible` |
| 拓扑感知 | 集群/哨兵模式下感知节点变化 | 拓扑刷新线程 |

**连接生命周期（一次命令执行）：**

```text
RedisTemplate.opsForValue().set(k, v)
  → RedisTemplate.execute() 内部：
      1. RedisConnectionUtils.bindConnection(factory)  // 从 ThreadLocal/池取连接
      2. 执行回调（实际命令）                             // 序列化 → 发送 → 读回
      3. RedisConnectionUtils.releaseConnection(...)    // finally 归还/关闭
```

> ⚠️ **关键细节**：Spring Data Redis 会通过 `ThreadLocal` 缓存当前线程的绑定连接（支持事务场景）。若你在非事务场景手写 `getConnection()` 却忘记 `finally { conn.close(); }`，每次调用都会新建连接不释放——线上表现为"连接数持续增长直到 Redis 拒绝新连接"。

## 3. Lettuce vs Jedis：两套驱动深度对比

| 维度 | Lettuce（默认） | Jedis |
|------|----------------|-------|
| 传输模型 | Netty 异步多路复用，单连接多命令并发 | 每请求独立连接（BIO 阻塞），需连接池支撑并发 |
| 线程安全 | 连接线程安全，可跨线程共享 | 连接非线程安全，必须池化 |
| 性能特点 | 并发高、连接占用低，适合高 QPS 与连接数受限场景 | 模型简单，小并发场景开销低 |
| 资源管理 | `shareNativeConnection` 共享底层连接 | commons-pool2 连接池 |
| 集群支持 | 原生支持集群拓扑感知与重定向 | 支持集群，重定向处理较繁琐 |
| 响应式 | 原生响应式驱动（ReactiveRedisTemplate 同源） | 不支持响应式 |
| Spring Boot 默认 | ✅ 默认（spring-boot-starter-data-redis 内置） | 需手动引入 `jedis` 依赖 |

**切换驱动的姿势（Spring Boot 4.x）：**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      client-type: jedis   # lettuce（默认）/ jedis
```

> 💡 **选型建议**：默认 Lettuce 在绝大多数场景够用且更好（省连接、异步底层）。Jedis 仅在"依赖 Bio 语义的既有代码""团队强制统一客户端"等场景值得切回。若使用 Lettuce，注意其内部超时与重试策略需单独配置（见第 6 节）。

## 4. RedisTemplate 模板引擎

### 4.1 模板的三大组成

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 1. 序列化：key / value / hashKey / hashValue 可分别指定（详见 02 篇）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer jacksonSerializer = new GenericJacksonJsonRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        // 2. 启用事务支持（详见 05 篇）：把命令绑定到 Spring 事务
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();   // 初始化校验
        return template;
    }
}
```

### 4.2 线程安全原理：回调模型

RedisTemplate 不持有连接，而是持有"借连接的能力"。每次操作传入 `RedisCallback<T>` 回调：

```java
public <T> T execute(RedisCallback<T> action) {
    // 1. 绑定连接（ThreadLocal 缓存 + 引用计数）
    RedisConnection conn = RedisConnectionUtils.bindConnection(connectionFactory);
    try {
        // 2. 序列化 key/value，调用回调执行底层命令
        return action.doInRedis(conn);
    } finally {
        // 3. 归还连接
        RedisConnectionUtils.releaseConnection(conn, connectionFactory);
    }
}
```

**要点：**
- 模板本身无共享可变状态 → 线程安全，可注入任意 Service。
- 回调内不得把 `conn` 传出（例如存进成员变量）——执行完就归还了。
- 连接获取是**同步阻塞**的；想非阻塞请用 `ReactiveRedisTemplate` 或 async 模式。

### 4.3 模板 vs 原生连接

| 场景 | RedisTemplate | 原生 RedisConnection |
|------|--------------|---------------------|
| 日常 CRUD | ✅ 首选，自动序列化 | ❌ 需手动序列化 |
| 细粒度控制（原始字节、指定命令） | 通过 `execute` 回调 | ✅ 直接可用 |
| Lua 脚本 | `execute(script, ...)` | `scriptingOps()` |
| 管道/事务 | `executePipelined` / `@Transactional` | `pipelined()` / `multi()` |

## 5. 连接池与共享连接

### 5.1 Lettuce：共享连接模型

Lettuce 的 `LettuceConnectionFactory` 默认**共享一条线程安全连接**（`shareNativeConnection=true`），不需要也不建议配连接池：

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:            # 仅当 shareNativeConnection=false 时才生效
          max-active: 16
          max-idle: 8
          min-idle: 0
          max-wait: 500ms
```

```java
LettuceConnectionFactory factory = new LettuceConnectionFactory();
factory.setShareNativeConnection(false);   // 关闭共享 → 走池
```

| 配置 | 默认值 | 说明 |
|------|:---:|------|
| shareNativeConnection | true | 共享单连接；false 则使用池 |
| validateConnection | false | 借出前是否校验连接可用 |
| timeout | 60s | 连接/命令超时（`spring.data.redis.timeout`） |
| shutdownTimeout | 100ms | 关闭工厂时的优雅退出超时 |

### 5.2 Jedis：必须池化

```yaml
spring:
  data:
    redis:
      jedis:
        pool:
          max-active: 8      # 连接池最大连接数
          max-idle: 8
          min-idle: 0
          max-wait: -1ms     # 获取连接最大等待（-1 无限）
```

> ⚠️ **常见误区**：给 Lettuce 配了 pool 却发现不生效——因为默认 `shareNativeConnection=true`，请求根本不经过池。先确认模式再调参。

## 6. Spring Boot 自动配置与属性全解

### 6.1 自动配置类

- `RedisAutoConfiguration`：连接工厂 + `RedisTemplate` + `StringRedisTemplate` 自动装配（`@ConditionalOnMissingBean`）。
- `RedisReactiveAutoConfiguration`：响应式模板（引入 spring-boot-starter-data-redis-reactive）。
- `RedisCacheConfiguration`（Boot 内部）：见 06 篇缓存。
- `@EnableCaching`：激活 Spring Cache 注解（Spring 框架核心的知识点，此处仅激活）。

### 6.2 属性清单（spring.data.redis.*）

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1            # 单机主机
      port: 6379
      username: default           # 6.0+ ACL 用户
      password: secret            # 密码
      database: 0                 # 逻辑库编号（单机/哨兵）
      timeout: 3s                 # 读写超时
      connect-timeout: 3s         # 建连超时（Boot 3.1+）
      client-type: lettuce        # lettuce | jedis
      sentinel:
        master: mymaster          # 哨兵模式：主节点名
        nodes: host1:26379,host2:26379,host3:26379
      cluster:
        nodes: node1:6379,node2:6379,node3:6379
        max-redirects: 3          # 集群重定向次数上限
      ssl: false
      lettuce:
        pool: {...}               # 见上节
        shutdown-timeout: 200ms
      jedis:
        pool: {...}
```

> 💡 **版本演进提示**：Spring Boot 3.x 将属性从 `spring.redis.*` 迁移到 `spring.data.redis.*`；Spring Boot 4.x 保持 `spring.data.redis.*` 不变，但内部序列化默认策略随 Spring Data Redis 4.x 的 Jackson 3 迁移（详见 02 篇）。

### 6.3 多数据源场景

单 Redis 实例不够时，手动定义多个 ConnectionFactory + 模板：

```java
@Bean
@Primary
public RedisConnectionFactory primaryFactory() {
    return new LettuceConnectionFactory(new RedisStandaloneConfiguration("cache-host", 6379));
}

@Bean
public RedisConnectionFactory analyticsFactory() {
    return new LettuceConnectionFactory(new RedisStandaloneConfiguration("analytics-host", 6379));
}

@Bean
public RedisTemplate<String, Object> analyticsRedisTemplate(@Qualifier("analyticsFactory") RedisConnectionFactory f) {
    // 与主模板同样的装配逻辑
}
```

## 7. 自定义连接工厂场景

| 场景 | 做法 |
|------|------|
| 自定义超时/重试 | 用 `RedisURI` 或 `ClientOptions` 构建 Lettuce 客户端 |
| TLS 连接 | `spring.data.redis.ssl=true` + 证书配置；或 RedisURI 携带 |
| 慢命令保护 | Lettuce `TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(2)).build()` |
| 重试策略 | `SocketOptions` 建连重试 + 命令级 `Retry`（Lettuce 4.x+ 的 advancedRedisClusterClientOptions） |
| 命令观测 | 启用 Micrometer 观测（见 09 篇） |

```java
LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .clientOptions(ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder()
                        .fixedTimeout(Duration.ofSeconds(2))
                        .build())
                .build())
        .commandTimeout(Duration.ofSeconds(3))
        .build();
RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration("localhost", 6379);
LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
```

## 8. 常见故障排查

| 现象 | 根因 | 处理 |
|------|------|------|
| `RedisConnectionFailureException: Unable to connect to Redis` | 网络/密码/ACL 错误 | 检查 `spring.data.redis.*`、telnet 6379、看 Redis 日志 |
| 连接数暴涨 | 手写 `getConnection()` 未关闭 | 全局搜索 `getConnection()`，检查 finally |
| 超时频繁 | 大 key 操作 / 慢命令阻塞 / 超时配置过小 | 用 `--bigkeys` 排查；合理调大 timeout |
| `ERR max number of clients reached` | 连接未释放或池过小 | 检查释放逻辑；调整 max-active |
| Lettuce 下偶发 `Connection closed` | 共享连接被异常关闭 | 开启 `validateConnection` 或调整客户端 options |
| 集群重定向异常 | max-redirects 太小 / 拓扑未刷新 | 调大 max-redirects；检查拓扑刷新配置 |

> 🎯 **核心要点**：Spring Data Redis 的四层架构（应用 → 模板 → 工厂 → 驱动）决定了它的线程安全模型（回调 + 短连接借用）与故障模式（连接泄漏、共享连接失效）。记牢"模板安全、连接贵重、必须归还"十二字口诀，架构篇即毕业。

---

**上一模块**：[00-Spring Data Redis知识体系总览](00-Spring Data Redis知识体系总览.md)　**下一模块**：[02-序列化器体系深度剖析](02-序列化器体系深度剖析.md)
