# Testcontainers 核心知识点

## 一、概述

Testcontainers 是一个 Java 集成测试库，提供 **Docker 容器中的真实依赖服务**（数据库、消息队列、缓存等）用于测试，解决传统 H2 内存数据库测试与生产环境不一致的问题。

**核心定位：** "测试即生产"——用真实 MySQL/PostgreSQL/Redis 跑测试，杜绝 Mock 环境与生产环境的差异。

**官网：** https://testcontainers.com

## 二、核心价值

### 2.1 传统测试痛点

```
┌─────────────────────────────────────────────┐
│ 测试用 H2 / Derby         生产用 MySQL       │
│                                                │
│ ❌ SQL 方言差异 → 测试过但生产报错              │
│ ❌ JSON/日期函数不一致                         │
│ ❌ 存储过程/触发器行为不同                     │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ Testcontainers 方案                           │
│                                                │
│ ✅ 测试 = 真实 MySQL Docker 容器               │
│ ✅ 完全一致的 SQL 行为                        │
│ ✅ 用完即销毁，无残留                          │
└─────────────────────────────────────────────┘
```

### 2.2 支持的模块

| 模块 | 用途 |
|------|------|
| **MySQL / PostgreSQL** | 数据库测试 |
| **Redis** | 缓存测试 |
| **Kafka / RabbitMQ** | 消息队列测试 |
| **Elasticsearch** | 搜索引擎测试 |
| **MongoDB** | NoSQL 测试 |
| **LocalStack** | AWS 服务模拟 |
| **Toxiproxy** | 网络故障注入 |
| **WireMock** | HTTP API 模拟 |

## 三、快速上手

### 3.1 依赖

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
```

### 3.2 基础示例（MySQL）

```java
@Testcontainers  // 自动管理容器生命周期
class UserRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindUser() {
        User user = new User("张三", "zhangsan@example.com");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("zhangsan@example.com");
        assertTrue(found.isPresent());
        assertEquals("张三", found.get().getName());
    }
}
```

### 3.3 Redis 测试

```java
@Testcontainers
class CacheServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
}
```

### 3.4 Kafka 测试

```java
@Container
static KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

@DynamicPropertySource
static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
}
```

## 四、最佳实践

### 4.1 性能优化

```java
// 单例容器：所有测试类共享同一个容器实例
// 抽象基类集中管理
@Testcontainers
@SpringBootTest
public abstract class BaseIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withReuse(true);  // 允许复用容器（需 ~/.testcontainers.properties）

    static {
        MYSQL.start();
    }
}
```

### 4.2 Docker 镜像加速

```properties
# ~/.testcontainers.properties
testcontainers.reuse.enable=true
docker.client.strategy=org.testcontainers.dockerclient.UnixSocketClientProviderStrategy
```

### 4.3 CI/CD 集成

```yaml
# GitHub Actions
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      docker:
        image: docker:24-dind
        options: --privileged
    steps:
      - uses: actions/checkout@v4
      - name: Run tests
        run: ./mvnw verify
```

## 五、AI 辅助

```java
// Prompt: "为 OrderRepository 生成 Testcontainers 集成测试，
//         包含创建订单、按状态查询、分页查询三个测试用例"
// AI 可自动生成测试基类 + 具体测试方法
```

## 六、总结

Testcontainers 解决了集成测试最大的痛点——**环境不一致**。核心优势：真实依赖服务 + 用完即弃 + 与 CI/CD 天然兼容。建议所有涉及数据库/缓存/消息队列的集成测试都采用 Testcontainers。
