# 10 - Spring Cloud Alibaba 全栈实战

> 🎯 以电商微服务为例，从零搭建完整的 Spring Cloud Alibaba 全栈项目 — Docker Compose 一键启动基础设施 + 多个微服务模块完整代码

---

## 目录

1. [项目架构总览](#1-项目架构总览)
2. [Docker Compose 基础设施](#2-docker-compose-基础设施)
3. [项目模块结构](#3-项目模块结构)
4. [核心服务代码](#4-核心服务代码)
5. [部署与验证](#5-部署与验证)

---

## 1. 项目架构总览

```text
电商微服务 DEMO — mall-microservice

外部请求
    │
[Spring Cloud Gateway :8080]  — 网关
    │
    ├── /api/users/**  → [user-service :8081]  — 用户服务
    ├── /api/orders/** → [order-service :8082]  — 订单服务
    └── /api/products/** → [product-service :8083] — 商品服务

基础设施：
  ├── Nacos :8848     — 注册中心 + 配置中心
  ├── MySQL :3306     — 数据库
  ├── Sentinel :8080  — 熔断限流控制台
  ├── Zipkin :9411    — 链路追踪
  └── Seata :8091     — 分布式事务
```

### 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot + Cloud | 3.2 / 2023.0 |
| 注册/配置 | Nacos | 2.3.0 |
| 远程调用 | OpenFeign + LoadBalancer | — |
| 熔断限流 | Sentinel | 1.8.6 |
| 网关 | Spring Cloud Gateway | — |
| 链路追踪 | Micrometer Tracing + Zipkin | 3.0 |
| 分布式事务 | Seata | 1.8.0 |
| 数据库 | MySQL + MyBatis-Plus | 8.0 / 3.5 |

---

## 2. Docker Compose 基础设施

```yaml
# docker-compose.yml — 本地开发一键启动
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: root123
    volumes:
      - ./sql:/docker-entrypoint-initdb.d  # 初始化 SQL

  nacos:
    image: nacos/nacos-server:v2.3.0
    ports: ["8848:8848", "9848:9848"]
    environment:
      MODE: standalone

  sentinel:
    image: bladex/sentinel-dashboard:1.8.6
    ports: ["8090:8080"]      # Web 访问用 8090（避免与 Gateway 冲突）

  zipkin:
    image: openzipkin/zipkin:3.0
    ports: ["9411:9411"]

  seata:
    image: seataio/seata-server:1.8.0
    ports: ["8091:8091"]
    environment:
      STORE_MODE: db
      SEATA_STORE_DB_URL: jdbc:mysql://mysql:3306/seata?useSSL=false
```

```bash
# 启动所有基础设施
docker-compose up -d

# 验证
curl http://localhost:8848/nacos     # Nacos 控制台
curl http://localhost:9411           # Zipkin
curl http://localhost:8090           # Sentinel (端口映射 8090→8080)
```

---

## 3. 项目模块结构

```
mall-microservice/
├── pom.xml (父 POM + BOM 管理)
├── docker-compose.yml
│
├── mall-gateway/                   # 网关模块
│   ├── pom.xml
│   └── src/main/resources/application.yml
│
├── mall-api/                       # 公共 API 模块（Feign 接口 + DTO）
│   ├── pom.xml
│   ├── UserClient.java
│   ├── OrderClient.java
│   └── commons/Result.java
│
├── user-service/                   # 用户服务（Provider + Consumer）
│   └── src/main/resources/application.yml
│
├── order-service/                  # 订单服务
│   └── src/main/resources/application.yml
│
├── product-service/                # 商品服务
│   └── src/main/resources/application.yml
│
└── sql/                            # 初始化 SQL
    └── init.sql
```

### 父 POM 关键依赖

```xml
<!-- BOM 统一版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2023.0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 公共依赖（所有子模块继承） -->
<dependencies>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-brave</artifactId>
    </dependency>
</dependencies>
```

---

## 4. 核心服务代码

### 4.1 订单服务（完整示例）

```yaml
# order-service/application.yml
server:
  port: 8082
spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
    sentinel:
      transport:
        dashboard: 127.0.0.1:8090
  datasource:
    url: jdbc:mysql://localhost:3306/mall_order
    username: root
    password: root123
```

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.mall.api")
@EnableDiscoveryClient
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProductClient productClient;

    @GetMapping("/{id}")
    @SentinelResource(value = "getOrder", fallback = "getOrderFallback")
    public Result<OrderVO> getOrder(@PathVariable Long id) {
        Order order = orderService.getById(id);
        User user = userClient.getUser(order.getUserId());      // Feign 调用
        Product product = productClient.getProduct(order.getProductId());
        return Result.ok(OrderVO.of(order, user, product));
    }

    public Result<OrderVO> getOrderFallback(Long id, Throwable e) {
        log.error("查询订单降级: id={}", id, e);
        return Result.fail(500, "订单服务繁忙，请稍后");
    }
}
```

### 4.2 Feign 接口定义

```java
// mall-api/UserClient.java
@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    @GetMapping("/users/{id}")
    User getUser(@PathVariable Long id);
}

// fallback 工厂（获取异常信息）
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        log.error("UserClient 调用失败", cause);
        return id -> new User(id, "未知用户", "");
    }
}
```

---

## 5. 部署与验证

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 依次启动微服务
java -jar mall-gateway.jar
java -jar user-service.jar
java -jar order-service.jar
java -jar product-service.jar

# 3. 验证服务注册
curl http://localhost:8848/nacos → 服务列表
# user-service / order-service / product-service / mall-gateway

# 4. 测试接口
curl http://localhost:8080/api/orders/1

# 5. 查看链路追踪
open http://localhost:9411 → 搜索 traceId

# 6. 验证 Sentinel
open http://localhost:8090 → 实时监控 / 流控规则

# 7. 压测验证
ab -n 1000 -c 50 http://localhost:8080/api/orders/1
```

> 🎯 **从零到全栈**：Docker Compose 启动基础设施 → 4 个微服务模块 → Feign 调用 → Sentinel 保护 → Gateway 路由 → Zipkin 追踪。这个 DEMO 覆盖了 Spring Cloud Alibaba 最核心的每日使用场景。
