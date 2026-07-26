# Testcontainers 核心知识点

> "测试即生产" —— 在 Docker 容器中启动真实依赖服务（数据库 / 消息队列 / 缓存 / 搜索引擎等）进行集成测试，彻底解决传统 Mock 测试与生产环境不一致的问题。

**官网：** https://testcontainers.com

---

## 目录

1. [概述](#1-概述)
2. [核心价值与问题解决](#2-核心价值与问题解决)
3. [依赖配置](#3-依赖配置)
4. [容器类型详解](#4-容器类型详解)
5. [注解体系与生命周期管理](#5-注解体系与生命周期管理)
6. [Singleton 单例容器模式](#6-singleton-单例容器模式)
7. [DynamicPropertySource 动态配置注入](#7-dynamicpropertysource-动态配置注入)
8. [GenericContainer 通用容器与自定义镜像](#8-genericcontainer-通用容器与自定义镜像)
9. [Docker Compose 模块](#9-docker-compose-模块)
10. [等待策略 (Wait Strategies)](#10-等待策略-wait-strategies)
11. [网络与容器间通信](#11-网络与容器间通信)
12. [Testcontainers Desktop](#12-testcontainers-desktop)
13. [Testcontainers Cloud](#13-testcontainers-cloud)
14. [性能优化与最佳实践](#14-性能优化与最佳实践)
15. [调试技巧](#15-调试技巧)
16. [CI/CD 集成](#16-cicd-集成)
17. [完整示例：SpringBoot + PostgreSQL + Redis + Kafka 集成测试](#17-完整示例springboot--postgresql--redis--kafka-集成测试)
18. [对比分析](#18-对比分析)
19. [总结](#19-总结)

---

## 1. 概述

Testcontainers 是一个 Java 测试库（也有 Python 和 .NET 版本），它在 JUnit 测试中通过 Docker 容器自动启动和管理外部依赖服务。每次测试运行时，Testcontainers 拉取指定镜像、启动容器、执行测试、最后销毁容器——整个过程自动完成，无需开发者在本地手动管理任何外部服务。

### 1.1 核心定位

```
传统集成测试
┌─────────────────────────────────────────────────┐
│ 问题：测试环境 ≠ 生产环境                          │
│                                                   │
│   H2 测试通过 → 生产 MySQL 报错                   │
│   Embedded Redis 测试通过 → 生产 Redis 集群报错    │
│   WireMock 测试通过 → 真实 Kafka 行为不同          │
└─────────────────────────────────────────────────┘

Testcontainers 方案
┌─────────────────────────────────────────────────┐
│ 方案：测试 = 真实 Docker 容器 = 生产环境          │
│                                                   │
│   ✅ 每次测试启动真实的 MySQL 容器                 │
│   ✅ 完全一致的 SQL 行为和索引行为                 │
│   ✅ 用完自动销毁，无残留                          │
│   ✅ CI/CD 环境中一样运行                          │
└─────────────────────────────────────────────────┘
```

### 1.2 版本信息

| 版本 | 说明 |
|------|------|
| **Testcontainers for Java** | 主推版本，JUnit 4 / JUnit 5 均支持 |
| **Testcontainers for Go** | 支持 Go 语言测试 |
| **Testcontainers for .NET** | 支持 C# 集成测试 |
| **Testcontainers Desktop** | macOS 桌面应用，容器可视化管理 |
| **Testcontainers Cloud** | CI 环境中无需本地 Docker 的云方案 |

---

## 2. 核心价值与问题解决

### 2.1 传统集成测试的痛点

| 痛点 | 传统方案 | 风险 |
|------|----------|------|
| **SQL 方言差异** | 测试用 H2 / Derby，生产用 MySQL/PostgreSQL | SQL 方言不同导致测试通过但生产报错 |
| **JSON / 日期函数不一致** | H2 的 JSON 函数和日期行为与 MySQL 不同 | 特定查询在数据库间行为不一致 |
| **存储过程 / 触发器** | H2 不支持或行为完全不同 | 复杂业务逻辑无法验证 |
| **中间件不可用** | 测试中无法启动真实的 Kafka / Redis | 缓存 / 消息队列逻辑无法测试 |
| **网络 / 故障场景** | 难以模拟网络超时、连接断开 | 容错逻辑无法验证 |
| **环境依赖** | 每个开发者需要本地安装各种中间件 | 环境搭建成本高，新入职耗时数天 |

### 2.2 Testcontainers 的优势

| 优势 | 说明 |
|------|------|
| **真实环境** | 与生产完全一致的 Docker 镜像，零差异 |
| **自动生命周期** | 容器自动启动、等待就绪、测试完毕销毁 |
| **无残留** | Ryuk 容器自动清理，不会留下僵尸容器 |
| **并行友好** | JUnit 多线程测试各自使用独立容器 |
| **CI/CD 就绪** | Docker-in-Docker 或 Testcontainers Cloud 无缝集成 |
| **灵活** | 可启动任意 Docker 镜像，非官方模块也支持 |

---

## 3. 依赖配置

### 3.1 Maven

```xml
<!-- BOM 统一版本管理（推荐） -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>1.20.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- 核心库（必须） -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- JUnit 5 集成（必须） -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- 特定服务模块（按需添加） -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>redis</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3.2 Gradle

```groovy
testImplementation platform('org.testcontainers:testcontainers-bom:1.20.4')
testImplementation 'org.testcontainers:testcontainers'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mysql'
testImplementation 'org.testcontainers:postgresql'
testImplementation 'org.testcontainers:kafka'
testImplementation 'org.testcontainers:redis'
```

---

## 4. 容器类型详解

### 4.1 完整模块矩阵

| 模块 | Maven artifact | 核心类 | 典型用途 |
|------|---------------|--------|----------|
| **MySQL** | `mysql` | `MySQLContainer` | 关系型数据库集成测试 |
| **PostgreSQL** | `postgresql` | `PostgreSQLContainer` | 关系型数据库（支持 PostGIS） |
| **MariaDB** | `mariadb` | `MariaDBContainer` | MySQL 兼容替代 |
| **Oracle XE** | `oracle-xe` | `OracleContainer` | Oracle 数据库测试 |
| **SQL Server** | `mssqlserver` | `MSSQLServerContainer` | SQL Server 测试 |
| **Redis** | `redis` | `RedisContainer` | 缓存 / 分布式锁测试 |
| **Kafka** | `kafka` | `KafkaContainer` | 消息队列 / 事件驱动测试 |
| **RabbitMQ** | `rabbitmq` | `RabbitMQContainer` | AMQP 消息代理测试 |
| **Elasticsearch** | `elasticsearch` | `ElasticsearchContainer` | 搜索引擎 / 全文检索测试 |
| **MongoDB** | `mongodb` | `MongoDBContainer` | NoSQL 文档数据库测试 |
| **LocalStack** | `localstack` | `LocalStackContainer` | AWS 服务（S3 / SQS / SNS / DynamoDB 等）模拟 |
| **Toxiproxy** | `toxiproxy` | `ToxiproxyContainer` | 网络故障注入（延迟 / 断开 / 丢包） |
| **WireMock** | `wiremock` | `WireMockContainer` | HTTP API 模拟 |
| **Neo4j** | `neo4j` | `Neo4jContainer` | 图数据库测试 |
| **Solr** | `solr` | `SolrContainer` | 搜索引擎测试 |
| **Pulsar** | `pulsar` | `PulsarContainer` | 消息队列测试 |
| **Nginx** | `nginx` | `NginxContainer` | Web 服务器测试 |
| **CockroachDB** | `cockroachdb` | `CockroachContainer` | 分布式 SQL 测试 |
| **ClickHouse** | `clickhouse` | `ClickHouseContainer` | 列式数据库测试 |

### 4.2 MySQL 容器深度

```java
@Testcontainers
class MysqlIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")              // 数据库名称
        .withUsername("test")                     // 用户名
        .withPassword("test")                     // 密码
        .withCommand("--character-set-server=utf8mb4",  // 字符集
                     "--collation-server=utf8mb4_unicode_ci")
        .withEnv("TZ", "Asia/Shanghai");          // 时区设置

    @Test
    void testConnection() {
        // 获取 JDBC URL
        String jdbcUrl = mysql.getJdbcUrl();
        String username = mysql.getUsername();
        String password = mysql.getPassword();
        String host = mysql.getHost();
        Integer port = mysql.getFirstMappedPort();

        // 使用 JDBC 直接连接验证
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT VERSION()");
            rs.next();
            System.out.println("MySQL Version: " + rs.getString(1));
        }
    }
}
```

### 4.3 PostgreSQL 容器

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

// 支持 PostGIS 扩展
// .withUrlParam("currentSchema", "public")
```

### 4.4 Redis 容器

```java
@Container
static RedisContainer redis = new RedisContainer(
    DockerImageName.parse("redis:7-alpine"))
    .withExposedPorts(6379);

// 获取 Redis 连接信息
String host = redis.getHost();
Integer port = redis.getFirstMappedPort();

// 配合 Jedis 或 Lettuce 使用
// Jedis jedis = new Jedis(host, port);
```

> 💡 RedisContainer 提供了 `getRedisURI()` 方法，直接返回 `redis://host:port` 格式的 URI。

### 4.5 Kafka 容器

```java
@Container
static KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))
    .withEmbeddedZookeeper();  // 内置 Zookeeper，无需额外启动

// 获取 Bootstrap Servers 地址
String bootstrapServers = kafka.getBootstrapServers();
// 返回: PLAINTEXT://localhost:32789
```

### 4.6 Elasticsearch 容器

```java
@Container
static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0"))
    .withPassword("changeme")              // 设置密码
    .withEnv("xpack.security.enabled", "true");

// 获取连接地址
String httpHostAddress = elasticsearch.getHttpHostAddress();
```

### 4.7 LocalStack 容器（AWS 模拟）

```java
@Container
static LocalStackContainer localstack = new LocalStackContainer(
    DockerImageName.parse("localstack/localstack:3.8"))
    .withServices(
        LocalStackContainer.Service.S3,
        LocalStackContainer.Service.SQS,
        LocalStackContainer.Service.SNS,
        LocalStackContainer.Service.DYNAMODB
    );

// 获取 AWS 客户端
@Test
void testS3() {
    AmazonS3 s3 = AmazonS3ClientBuilder
        .standard()
        .withEndpointConfiguration(
            localstack.getEndpointConfiguration(LocalStackContainer.Service.S3))
        .withCredentials(
            new StaticCredentialsProvider(
                new BasicAWSCredentials(localstack.getAccessKey(),
                                        localstack.getSecretKey())))
        .build();

    s3.createBucket("test-bucket");
    s3.putObject("test-bucket", "test.txt", "Hello World");

    S3Object obj = s3.getObject("test-bucket", "test.txt");
    String content = new String(obj.getObjectContent().readAllBytes());
    assertEquals("Hello World", content);
}
```

### 4.8 MongoDB 容器

```java
@Container
static MongoDBContainer mongodb = new MongoDBContainer(
    DockerImageName.parse("mongo:7.0"))
    .withExposedPorts(27017);

// 获取连接字符串
String connectionString = mongodb.getConnectionString();
// 返回: mongodb://localhost:32790/test
```

> 💡 MongoDBContainer 自动创建 `test` 数据库，可直接使用。

---

## 5. 注解体系与生命周期管理

### 5.1 @Testcontainers 与 @Container

| 注解 | 作用 | 说明 |
|------|------|------|
| `@Testcontainers` | 标记测试类，启用 Testcontainers 生命周期管理 | 类级别注解 |
| `@Container` | 标记容器字段，自动管理启动和销毁 | 字段级别注解 |

### 5.2 两种声明方式

**方式一：静态字段（推荐）-- 所有测试方法共享同一容器**

```java
@Testcontainers
class SharedContainerTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void test1() { /* 使用 mysql 容器 */ }

    @Test
    void test2() { /* 使用同一个 mysql 容器 */ }
}
```

> 💡 `static` + `@Container`：在测试类初始化时启动一次，所有测试方法共享。

**方式二：实例字段 -- 每个测试方法独立容器**

```java
@Testcontainers
class PerTestContainerTest {

    @Container
    MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void test1() { /* 独立的 mysql 容器 */ }

    @Test
    void test2() { /* 另一个独立的 mysql 容器 */ }
}
```

> ⚠️ 实例字段会在每个测试方法执行前启动新容器，测试结束后销毁。**仅在需要隔离的场景下使用**，性能开销较大。

### 5.3 手动管理生命周期

```java
// 手动 start / stop（不依赖 @Testcontainers）
class ManualLifecycleTest {

    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @BeforeAll
    static void init() {
        mysql.start();
    }

    @AfterAll
    static void cleanup() {
        mysql.stop();
    }
}
```

---

## 6. Singleton 单例容器模式

### 6.1 抽象基类模式

在大型项目中，通常定义一个抽象基类，所有集成测试继承它，实现容器复用：

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    // ─── 容器定义 ───
    @Container
    protected static final MySQLContainer<?> MYSQL =
        new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    protected static final RedisContainer REDIS =
        new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Container
    protected static final KafkaContainer KAFKA =
        new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.0"));

    // ─── 静态初始化块 ───
    static {
        // 手动启动（确保 @DynamicPropertySource 准备就绪）
        MYSQL.start();
        REDIS.start();
        KAFKA.start();
    }

    // ─── 动态配置注入 ───
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
```

### 6.2 使用基类的具体测试

```java
class UserServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void testCreateUser() {
        User user = userService.createUser("test@example.com", "TestUser");
        assertNotNull(user.getId());
        assertEquals("test@example.com", user.getEmail());
    }
}
```

> 🎯 单例模式是**最佳实践**：所有容器在测试套件开始时启动一次，全部测试运行结束后才销毁，大幅缩短测试执行时间。

---

## 7. DynamicPropertySource 动态配置注入

### 7.1 基本原理

`@DynamicPropertySource` 是 Spring 5.2.5+ 引入的注解，配合 Testcontainers 在运行时动态修改 Spring 配置属性：

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    // 格式: registry.add("spring.xxx", () -> 动态值);
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
}
```

### 7.2 复杂配置示例

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    // 数据库
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

    // Redis
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getFirstMappedPort()));
    registry.add("spring.data.redis.password", () -> "");

    // Kafka
    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

    // Elasticsearch
    registry.add("spring.elasticsearch.uris", ELASTICSEARCH::getHttpHostAddress);

    // AWS (LocalStack)
    registry.add("aws.s3.endpoint",
        () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    registry.add("aws.region", LOCALSTACK::getRegion);
    registry.add("aws.accessKeyId", LOCALSTACK::getAccessKey);
    registry.add("aws.secretAccessKey", LOCALSTACK::getSecretKey);
}
```

> 💡 `DynamicPropertyRegistry` 接收的是 `Supplier<String>` 或 `Supplier<Integer>`，所以需要使用 Lambda 或方法引用。

---

## 8. GenericContainer 通用容器与自定义镜像

### 8.1 基本用法

对于 Testcontainers 没有提供专用模块的服务，使用 `GenericContainer`：

```java
@Container
static GenericContainer<?> myApp = new GenericContainer<>(
    DockerImageName.parse("mya.0.0"))
    .withExposedPorts(8080)            // 暴露容器端口
    .withEnv("SPRING_PROFILES_ACTIVE", "test")   // 环境变量
    .withEnv("DB_URL", "jdbc:mysql://host:3306/db")
    .withCommand("java", "-jar", "app.jar")       // 覆盖 CMD
    .waitingFor(Wait.forHttp("/actuator/health")  // 等待健康检查
        .forStatusCode(200))
    .withStartupTimeout(Duration.ofSeconds(60));  // 启动超时

// 获取映射端口
String host = myApp.getHost();
Integer port = myApp.getMappedPort(8080);
```

### 8.2 自定义 Dockerfile

```java
// 从指定路径构建镜像（需要 Dockerfile）
GenericContainer<?> customApp = new GenericContainer<>(
    new ImageFromDockerfile("my-custom-app", false)
        .withDockerfileFromBuilder(builder -> builder
            .from("openjdk:17-slim")
            .copy("app.jar", "/app/app.jar")
            .workDir("/app")
            .entryPoint("java", "-jar", "app.jar")
        ))
    .withExposedPorts(8080);

// 或使用类路径中的 Dockerfile 构建
GenericContainer<?> appFromDockerfile = new GenericContainer<>(
    new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder -> builder
            .from("nginx:alpine")
            .copy("nginx.conf", "/etc/nginx/conf.d/default.conf")
            .build()));
```

### 8.3 文件挂载与复制

```java
// 从类路径复制文件到容器
genericContainer.withCopyFileToContainer(
    MountableFile.forClasspathResource("test-data/init.sql"),
    "/docker-entrypoint-initdb.d/init.sql");

// 挂载主机目录
genericContainer.withFileSystemBind(
    "/host/path/data",
    "/container/path/data",
    BindMode.READ_WRITE);
```

---

## 9. Docker Compose 模块

### 9.1 基本用法

Testcontainers 支持通过 Docker Compose 启动复杂的多服务环境：

```java
@Testcontainers
@SpringBootTest
class DockerComposeIntegrationTest {

    // 方式一：类路径资源
    @Container
    static DockerComposeContainer<?> environment =
        new DockerComposeContainer<>(
            new File("src/test/resources/docker-compose.yml"))
            .withExposedService("db", 3306)
            .withExposedService("cache", 6379)
            .withExposedService("queue", 9092);
}
```

### 9.2 docker-compose.yml 示例

```yaml
version: '3.8'
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: testdb
    ports:
      - "3306"
  cache:
    image: redis:7-alpine
    ports:
      - "6379"
  queue:
    image: confluentinc/cp-kafka:7.7.0
    ports:
      - "9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
```

### 9.3 服务端口获取

```java
// 获取 Compose 中服务的映射端口
Integer dbPort = environment.getServicePort("db", 3306);
String dbUrl = "jdbc:mysql://localhost:" + dbPort + "/testdb";

Integer redisPort = environment.getServicePort("cache", 6379);
Integer kafkaPort = environment.getServicePort("queue", 9092);
```

> 💡 `DockerComposeContainer` 适用于需要精确复现生产环境的复杂多服务场景，比单容器组合更贴近真实部署拓扑。

---

## 10. 等待策略 (Wait Strategies)

### 10.1 等待策略矩阵

| 策略 | 类 | 适用场景 |
|------|-----|----------|
| **默认等待** | `Wait.defaultWaitStrategy()` | 等待默认端口可用 |
| **日志等待** | `Wait.forLogMessage(regex)` | 等待容器输出特定日志 |
| **HTTP 等待** | `Wait.forHttp(path)` | 等待 HTTP 端点返回 200 |
| **Healthcheck 等待** | `Wait.forHealthcheck()` | 等待 Docker HEALTHCHECK 通过 |
| **端口等待** | `Wait.forListeningPort()` | 等待端口开始监听 |

### 10.2 日志等待

```java
GenericContainer<?> container = new GenericContainer<>("my-app:latest")
    // 等待日志中出现 "Started Application in" 字样
    .waitingFor(Wait.forLogMessage(".*Started Application in.*\\n", 1))
    .withStartupTimeout(Duration.ofSeconds(120));
```

### 10.3 HTTP 等待

```java
GenericContainer<?> container = new GenericContainer<>("my-app:latest")
    .withExposedPorts(8080)
    .waitingFor(Wait.forHttp("/actuator/health")   // HTTP GET 请求
        .forStatusCode(200)                         // 期望状态码
        .forStatusCodePredicate(code -> code >= 200 && code < 400)  // 或自定义
        .withBasicCredentials("user", "pass")       // HTTP Basic 认证
        .withHeader("X-Custom", "value")            // 自定义 Header
        .withMethod(HttpMethod.GET)                 // 请求方法（默认 GET）
        .withBody("{\"key\":\"value\"}")            // 请求体（POST/PUT 时使用）
        .withStartupTimeout(Duration.ofSeconds(60)));
```

### 10.4 自定义等待策略

```java
// 自定义等待：等待某个文件出现在容器中
GenericContainer<?> container = new GenericContainer<>("my-app:latest")
    .waitingFor(new GenericContainer.AbstractWaitStrategy() {
        @Override
        protected void waitUntilReady() {
            ExecResult result = null;
            try {
                result = container.execInContainer(
                    "sh", "-c", "test -f /tmp/ready && echo ready || echo not");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!"ready\n".equals(result.getStdout())) {
                throw new IllegalStateException("Container not ready yet");
            }
        }
    });
```

> ⚠️ 合理的等待策略是避免测试不稳定（Flaky Test）的关键。避免使用固定 `Thread.sleep()`，应始终使用标准的 Wait Strategy。

---

## 11. 网络与容器间通信

### 11.1 自定义网络

当多个容器需要互相通信时（例如应用容器连接数据库容器），应使用自定义网络：

```java
@Testcontainers
class ContainerNetworkTest {

    // 创建自定义网络
    static Network network = Network.newNetwork();

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withNetwork(network)
        .withNetworkAliases("mysql-host");  // 容器别名，其他容器可通过此名称访问

    @Container
    static GenericContainer<?> app = new GenericContainer<>("my-app:latest")
        .withNetwork(network)
        .withExposedPorts(8080)
        .withEnv("DB_URL", "jdbc:mysql://mysql-host:3306/testdb")
        .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));
}
```

### 11.2 网络别名解析

```java
// MySQL 容器设置网络别名为 "mysql-host"
// 应用容器通过 "mysql-host:3306" 访问 MySQL，无需关心端口映射
.withEnv("DB_URL", "jdbc:mysql://mysql-host:3306/testdb")
.withEnv("REDIS_HOST", "redis-host")
.withEnv("REDIS_PORT", "6379")
.withEnv("KAFKA_BOOTSTRAP", "kafka-host:9092");
```

> 💡 使用 `withNetworkAliases()` 结合自定义网络，可以模拟生产环境中的容器间服务发现行为，且不依赖宿主机端口映射。

---

## 12. Testcontainers Desktop

Testcontainers Desktop 是 macOS 桌面应用程序，提供了容器运行的可视化管理界面：

### 12.1 核心功能

| 功能 | 说明 |
|------|------|
| **容器列表** | 实时显示所有 Testcontainers 启动的容器 |
| **日志查看** | 流式查看容器日志，便于调试 |
| **容器控制** | 手动停止 / 暂停 / 重启容器 |
| **镜像管理** | 查看已拉取的 Docker 镜像 |
| **配置修改** | 修改 `.testcontainers.properties` 配置 |
| **Ryuk 状态** | 查看 Ryuk 清理器状态 |

### 12.2 容器复用管理

通过 Desktop 启用容器复用：

```properties
# ~/.testcontainers.properties
testcontainers.reuse.enable=true
```

启用后，Desktop 会显示当前可复用的容器，测试结束时不会销毁，再次运行时直接复用，显著加速重复测试执行。

---

## 13. Testcontainers Cloud

### 13.1 简介

Testcontainers Cloud 是 Testcontainers 的托管云服务，解决 CI 环境中无法运行 Docker 的问题：

```
核心优势：
├── CI 环境中不需要 Docker daemon
├── 容器在云端运行，CI 节点零配置
├── 自动弹性伸缩，支持海量并发
├── 比 Docker-in-Docker 更快、更稳定
└── 支持 GitHub Actions / Jenkins / GitLab CI 等
```

### 13.2 配置示例

```bash
# 1. 安装 TC Cloud Agent
curl -fsSL https://get.testcontainers.com/cloud | bash

# 2. 配置环境变量
TC_CLOUD_TOKEN=your_token_here

# 3. Maven 插件配置
```

```yaml
# GitHub Actions + Testcontainers Cloud
name: Integration Tests
on: [push]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Testcontainers Cloud
        uses: atomicjar/testcontainers-cloud-setup-action@v1
        with:
          token: ${{ secrets.TC_CLOUD_TOKEN }}
      - name: Run tests
        run: ./mvnw verify
```

---

## 14. 性能优化与最佳实践

### 14.1 容器复用

```properties
# ~/.testcontainers.properties — 启用容器复用
testcontainers.reuse.enable=true
```

```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withReuse(true);  // 测试结束后不销毁，下次复用
```

> ⚠️ `withReuse(true)` 仅用于本地开发环境。CI 环境中不应启用复用，以避免容器状态污染。

### 14.2 镜像选择策略

```text
DO:    mysql:8.0-alpine        ← 轻量镜像，快速拉取
DO:    postgres:16-alpine      ← Alpine 基础镜像更小
DON'T: mysql:latest            ← 版本不确定，破坏可重复性
DON'T: postgres:16             ← 未使用 slim/alpine 版本
```

### 14.3 启动速度优化

| 策略 | 说明 | 效果 |
|------|------|------|
| **单例容器** | 所有测试共享容器实例 | 减少 80% 启动时间 |
| **容器复用** | 启用 `withReuse(true)` | 减少 95% 重复启动 |
| **Alpine 镜像** | 使用 Alpine 版本 | 拉取快、启动快 |
| **预热镜像** | CI 中提前 `docker pull` | 避免测试时等待拉取 |
| **并行测试** | JUnit 5 + Maven Surefire 并行 | 充分利用并发 |
| **抽象基类** | 统一管理容器生命周期 | 减少重复配置 |

### 14.4 镜像拉取策略

```java
// 使用镜像名称缓存，避免二次拉取
// Docker 自动缓存已拉取的镜像

// 在 CI 中，可以在测试前预热镜像
// pre-test.sh
// docker pull mysql:8.0
// docker pull redis:7-alpine
// docker pull confluentinc/cp-kafka:7.7.0
```

### 14.5 测试类组织

```
src/test/java/
├── base/
│   └── AbstractIntegrationTest.java        ← 单例容器基类
├── repository/
│   ├── UserRepositoryTest.java             ← 继承基类
│   └── OrderRepositoryTest.java            ← 继承基类
├── service/
│   ├── UserServiceTest.java                ← 继承基类
│   └── OrderServiceTest.java               ← 继承基类
├── controller/
│   ├── UserControllerTest.java             ← 继承基类
│   └── OrderControllerTest.java            ← 继承基类
└── integration/
    └── FullWorkflowIntegrationTest.java    ← 完整流程测试
```

---

## 15. 调试技巧

### 15.1 容器日志查看

```java
// 方式一：绑定日志到 SLF4J（推荐）
@Testcontainers
class LoggingTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("testcontainers.mysql")));

    // 方式二：输出到标准输出
    @Container
    static RedisContainer redis = new RedisContainer(
        DockerImageName.parse("redis:7-alpine"))
        .withLogConsumer(outputFrame -> {
            System.out.print(outputFrame.getUtf8String());
        });

    // 方式三：捕获到 StringBuilder 用于断言
    static StringBuilder logs = new StringBuilder();

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))
        .withLogConsumer(outputFrame -> logs.append(outputFrame.getUtf8String()));
}
```

### 15.2 Ryuk 容器

Ryuk（Testcontainers 的清理器容器）负责在测试结束后自动清理所有启动的容器：

```
Ryuk 工作原理：
1. Testcontainers 启动测试容器
2. 同时启动一个 Ryuk 容器
3. Ryuk 监视 JVM 进程
4. 当 JVM 退出 / 测试结束 → Ryuk 自动清理所有容器
5. 即使 JVM 被强制杀死，Ryuk 也能清理残留

Ryuk 相关问题：
├── 容器名: testcontainers-ryuk-xxx
├── 如果 Ryuk 失败，容器会残留 → 手动 docker rm -f
├── 可以使用 TESTCONTAINERS_RYUK_DISABLED=true 禁用 Ryuk
└── Ryuk 镜像需要可拉取：testcontainers/ryuk:0.7.0
```

### 15.3 常见问题排查

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **容器启动超时** | 镜像拉取慢 / 启动慢 | 增加 `withStartupTimeout()`；预热镜像 |
| **端口冲突** | 本地已有服务占用端口 | 使用随机端口映射（默认行为） |
| **Docker 未运行** | CI 中没有 Docker | 使用 Testcontainers Cloud |
| **Ryuk 杀死容器** | 测试被中断 | 检查 Ryuk 日志；临时禁用 Ryuk |
| **测试间数据污染** | 复用容器但未清理数据 | 每个测试方法前 `@BeforeEach` 清理数据 |
| **Mac M1/M2 兼容性** | ARM 架构拉取 x86 镜像 | 添加 `platform("linux/amd64")` 或使用 ARM 镜像 |

### 15.4 调试配置

```properties
# 开启 Testcontainers 调试日志
logging.level.org.testcontainers=DEBUG

# 或通过环境变量
TESTCONTAINERS_LOG_LEVEL=DEBUG
```

---

## 16. CI/CD 集成

### 16.1 GitHub Actions

```yaml
name: Integration Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  integration-test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: maven

      - name: Pre-pull Docker images
        run: |
          docker pull mysql:8.0
          docker pull redis:7-alpine
          docker pull confluentinc/cp-kafka:7.7.0

      - name: Run integration tests
        run: ./mvnw verify -Pintegration-test

      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: target/surefire-reports/
```

### 16.2 Jenkins Pipeline

```groovy
pipeline {
    agent {
        docker { image 'maven:3.9-eclipse-temurin-21' }
    }

    stages {
        stage('Pre-pull Docker images') {
            steps {
                sh 'docker pull mysql:8.0'
                sh 'docker pull redis:7-alpine'
                sh 'docker pull confluentinc/cp-kafka:7.7.0'
            }
        }
        stage('Integration Tests') {
            steps {
                sh './mvnw verify -Pintegration-test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }

    options {
        // 使用 Docker-in-Docker
        docker {
            image 'docker:24-dind'
            reuseNode true
        }
    }
}
```

### 16.3 GitLab CI

```yaml
integration-test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  services:
    - docker:dind
  variables:
    DOCKER_HOST: tcp://docker:2375
  script:
    - docker pull mysql:8.0
    - docker pull redis:7-alpine
    - ./mvnw verify
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml
```

---

## 17. 完整示例：SpringBoot + PostgreSQL + Redis + Kafka 集成测试

### 17.1 项目依赖

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-bom</artifactId>
        <version>1.20.4</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 17.2 AbstractIntegrationTest 基类

```java
package com.example.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    // ─── PostgreSQL ───
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    // ─── Redis ───
    @Container
    protected static final RedisContainer REDIS =
        new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    // ─── Kafka ───
    @Container
    protected static final KafkaContainer KAFKA =
        new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))
            .waitingFor(Wait.forLogMessage(".*started \\(kafka.server.KafkaServer\\).*\\n", 1));

    // ─── 动态配置注入 ───
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
            () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Redis
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port",
            () -> String.valueOf(REDIS.getFirstMappedPort()));
        registry.add("spring.cache.type", () -> "redis");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.producer.key-serializer",
            () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
            () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    }
}
```

### 17.3 实体与仓库

```java
// ─── User 实体 ───
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    // getters and setters...
}

// ─── UserRepository ───
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByNameContaining(String name);
    boolean existsByEmail(String email);
}
```

### 17.4 具体测试类

```java
class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ─── 数据库测试 ───
    @Test
    void shouldSaveAndFindUserInPostgreSQL() {
        // Given
        User user = new User();
        user.setEmail("alice@example.com");
        user.setName("Alice");
        user.setAge(28);

        // When
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findByEmail("alice@example.com");

        // Then
        assertTrue(found.isPresent());
        assertNotNull(saved.getId());
        assertEquals("Alice", found.get().getName());
        assertEquals(28, found.get().getAge());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // Given
        User user = new User();
        user.setEmail("duplicate@example.com");
        user.setName("First");
        user.setAge(20);
        userRepository.save(user);

        // When
        User duplicate = new User();
        duplicate.setEmail("duplicate@example.com");
        duplicate.setName("Second");
        duplicate.setAge(25);

        // Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(duplicate);
        });
    }

    // ─── Redis 缓存测试 ───
    @Test
    void shouldCacheUserInRedis() {
        // Given
        User user = new User();
        user.setEmail("cache@example.com");
        user.setName("Cache Test");
        user.setAge(30);
        userRepository.save(user);

        // When
        String cacheKey = "user:" + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user.getName());

        // Then
        String cachedName = redisTemplate.opsForValue().get(cacheKey);
        assertEquals("Cache Test", cachedName);
    }

    // ─── Kafka 消息队列测试 ───
    @Test
    void shouldSendAndReceiveKafkaMessage() throws InterruptedException {
        // Given
        String topic = "user-events";
        UserEvent event = new UserEvent("CREATED", "alice@example.com");

        // 配置 Kafka 消费者
        CountDownLatch latch = new CountDownLatch(1);
        final String[] receivedMessage = {null};

        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener(
            (MessageListener<String, String>) message -> {
                receivedMessage[0] = message.value();
                latch.countDown();
            }
        );

        // When
        kafkaTemplate.send(topic, event);
        boolean messageReceived = latch.await(10, TimeUnit.SECONDS);

        // Then
        assertTrue(messageReceived, "应该在 10 秒内收到 Kafka 消息");
        assertNotNull(receivedMessage[0]);
        assertTrue(receivedMessage[0].contains("alice@example.com"));
    }

    // ─── 完整工作流测试 ───
    @Test
    void fullUserWorkflow() {
        // 1. Create
        User user = new User();
        user.setEmail("workflow@example.com");
        user.setName("Workflow");
        user.setAge(35);
        User saved = userRepository.save(user);
        assertNotNull(saved.getId());

        // 2. Read
        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("workflow@example.com", found.get().getEmail());

        // 3. Update
        found.get().setName("Workflow Updated");
        User updated = userRepository.save(found.get());
        assertEquals("Workflow Updated", updated.getName());

        // 4. Cache after read
        redisTemplate.opsForValue().set("user:" + saved.getId(), updated.getName());

        // 5. Delete
        userRepository.deleteById(saved.getId());
        assertFalse(userRepository.existsById(saved.getId()));

        // 6. Verify cache is stale (demonstrates cache invalidation needs)
        String cached = redisTemplate.opsForValue().get("user:" + saved.getId());
        assertNotNull(cached);  // Cache was not invalidated — intentional demo
        System.out.println("Note: Cache invalidation would be needed in production!");
    }
}
```

> 🎯 这个完整示例展示了 Testcontainers 在实际 SpringBoot 项目中的最佳实践：单例容器基类 + PostgreSQL / Redis / Kafka 三种中间件同时测试 + 完整的 Create-Read-Update-Delete 工作流验证。

---

## 18. 对比分析

### 18.1 方案对比

| 特性 | Testcontainers | H2 / HSQLDB | Embedded PostgreSQL | Docker Compose (手动) |
|------|---------------|-------------|-------------------|----------------------|
| **SQL 一致性** | ✅ 100% 一致 | ❌ 方言差异 | ✅ 一致（仅 PG） | ✅ 一致 |
| **启动速度** | ⚡ 中等（首次需拉取） | 🚀 极快 | 🚀 较快 | 🐢 手动操作 |
| **自动清理** | ✅ Ryuk 自动清理 | ✅ JVM 退出清理 | ✅ 进程退出清理 | ❌ 手动清理 |
| **中间件支持** | ✅ 任意 Docker 镜像 | ❌ 仅数据库 | ❌ 仅 PostgreSQL | ✅ 任意服务 |
| **并行测试** | ✅ 各测试独立容器 | ✅ 独立数据库 | ❌ 单实例 | ❌ 手动配置 |
| **CI/CD 适配** | ✅ 极佳（TC Cloud） | ✅ 无需 Docker | ❌ 需要 native lib | ⚠️ 需 DIND |
| **网络模拟** | ✅ Toxiproxy 集成 | ❌ | ❌ | ❌ |
| **版本一致性** | ✅ Docker 标签固定 | ❌ H2 版本独立 | ⚠️ 依赖库版本 | ✅ Docker 标签 |
| **学习成本** | ⚠️ 中等 | 🟢 低 | 🟢 低 | ⚠️ 需 Docker 经验 |

### 18.2 选择建议

| 场景 | 推荐方案 |
|------|----------|
| **纯 JPA/SQL 简单查询测试** | H2 内存数据库（快速） |
| **数据库函数 / 存储过程 / 触发器** | Testcontainers（一致性） |
| **微服务多中间件集成测试** | Testcontainers（首选） |
| **单一 PostgreSQL 应用** | Testcontainers 或 Embedded PG |
| **CI 环境无 Docker** | Testcontainers Cloud |
| **快速测试反馈循环** | Testcontainers + 容器复用 |

---

## 19. 总结

### 19.1 核心优势

- **真实环境**：使用与生产一致的 Docker 镜像，消灭环境差异
- **自动生命周期**：容器自动启动、等待就绪、执行测试、清理销毁，全流程零人工干预
- **丰富的模块矩阵**：30+ 官方模块覆盖主流数据库、消息队列、缓存、搜索引擎、云服务
- **CI/CD 原生支持**：GitHub Actions / Jenkins / GitLab CI 无缝集成

### 19.2 最佳实践速查

```
✅ DO:
├── 使用静态 `@Container` 字段（单例模式）
├── 定义抽象 `AbstractIntegrationTest` 基类
├── 使用 Alpine 基础镜像（更小更快）
├── 本地开发启用 `withReuse(true)` 加速
├── 使用 `@DynamicPropertySource` 注入配置
├── 充分利用 Wait Strategies，避免 Thread.sleep
├── 测试方法前 `@BeforeEach` 清理数据
└── CI 中预热 Docker 镜像

❌ DON'T:
├── 实例字段 `@Container`（每个测试都启动新容器）
├── 使用 `mysql:latest` 等未锁定版本标签
├── 在测试中使用 `Thread.sleep()` 等待
├── 在 CI 中启用 `withReuse(true)`
├── 忽略 Ryuk 清理器（确保其正常运行）
└── 测试间共享数据状态
```

### 19.3 2026 年趋势

- **Testcontainers Cloud** 逐渐成为 CI 集成标准方案，替代传统 Docker-in-Docker
- **AI 辅助测试生成**：LLM 可自动生成 Testcontainers 配置和测试代码
- **多语言生态扩展**：Python / Go / .NET 版本持续成熟，统一跨语言集成测试体验
- **Kubernetes 支持**：Testcontainers 正在实验性支持在 K8s 中启动容器

> 🎯 Testcontainers 已经从一个 "方便的工具" 演变为 **Java 集成测试的事实标准**。它用最优雅的方式解决了软件工程中一个老大难问题——"在我机器上能跑"的环境差异。任何涉及外部依赖的测试，都值得优先考虑 Testcontainers。
