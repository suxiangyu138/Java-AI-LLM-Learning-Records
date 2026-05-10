# 微服务 & SpringCloud 项目合集（Spring Cloud Alibaba 企业级实战）

本文档整理了 3 个适合校招、实习与中大厂后端岗位的微服务项目，统一围绕 `Spring Boot 3 + Spring Cloud Alibaba + MySQL + Redis + RabbitMQ` 展开，重点覆盖服务注册发现、配置中心、网关、限流熔断、分布式事务、统一认证鉴权等企业级核心能力。Spring Cloud Alibaba 官方文档将 Nacos、Sentinel、分布式事务与配置管理作为微服务开发的一站式能力组合；Seata 官方说明其面向高性能分布式事务；Spring Authorization Server 则提供 OAuth 2.1 与 OpenID Connect 1.0 的授权服务器实现 [page:4][page:3][web:26][web:29]。

## 项目总览

| 项目 | 技术栈 | 核心问题 | 简历价值 |
|---|---|---|---|
| SpringCloud 电商微服务 | Nacos + OpenFeign + Gateway + Sentinel | 服务治理、配置管理、路由转发、限流熔断 | 体现微服务基础设施能力 |
| 分布式订单系统 | Seata + Redis + RabbitMQ + MySQL | 跨服务事务一致性、库存锁定、超时取消 | 体现复杂业务链路落地能力 |
| 微服务后台权限平台 | OAuth2.1 + Spring Authorization Server + Gateway + Resource Server | 统一认证、统一鉴权、权限控制、网关校验 | 体现中后台权限平台设计能力 |

---

## 1. SpringCloud 电商微服务

### 1.1 项目目标

搭建一个标准的电商微服务系统，拆分用户、商品、购物车、订单、支付等服务，完成服务注册发现、配置中心、网关统一入口、Feign 远程调用、Sentinel 限流熔断。Spring Cloud Alibaba 文档明确支持 Nacos 服务注册发现、Nacos 配置中心、OpenFeign 调用适配以及 Sentinel 流控降级能力 [page:4]。

### 1.2 服务拆分

```text
mall-gateway
mall-auth（可选，后续权限平台复用）
mall-user
mall-product
mall-cart
mall-order
mall-pay
mall-common
```

### 1.3 架构设计

```text
前端 Vue/React
   ↓
Spring Cloud Gateway
   ↓
Nacos 注册中心 / 配置中心
   ↓
user-service / product-service / cart-service / order-service / pay-service
   ↓
MySQL + Redis + MQ
```

### 1.4 核心能力

- 服务注册发现：各服务启动后注册到 Nacos，消费者可通过服务名发现实例，避免手工维护地址列表 [page:4]。
- 配置中心：将数据库、Redis、Sentinel 等公共配置统一放在 Nacos，支持外部化配置与动态刷新 [page:4]。
- 网关路由：所有请求统一走 Gateway，根据 `Path` 断言转发到目标服务。
- Feign 调用：订单服务调用商品服务、用户服务时使用 OpenFeign。
- Sentinel 限流熔断：为热点接口增加流控、降级、Fallback，保护下游稳定性 [page:4]。

### 1.5 Maven 依赖建议

Spring Cloud Alibaba 官方建议通过 BOM 管理版本，并分别接入 `spring-cloud-starter-alibaba-nacos-discovery`、`spring-cloud-starter-alibaba-nacos-config`、`spring-cloud-starter-alibaba-sentinel` 等组件 [page:4]。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2022.0.0.0-RC1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

```xml
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
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
</dependencies>
```

### 1.6 Nacos 服务注册

Spring Cloud Alibaba 文档指出，只要引入 Nacos Discovery 并配置 `spring.cloud.nacos.discovery.server-addr`，服务即可自动注册与发现；OpenFeign 也可基于服务名完成远程调用 [page:4]。

```yaml
server:
  port: 8081
spring:
  application:
    name: product-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
```

### 1.7 Nacos 配置中心

Nacos Config 支持外部化配置、Profile 级配置和动态刷新。DataId 默认可按 `${spring.application.name}.properties` 或 `${spring.application.name}.yaml` 组织，适合管理多环境配置 [page:4]。

```yaml
spring:
  application:
    name: order-service
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
```

推荐拆分：

```text
order-service-dev.yaml
order-service-test.yaml
order-service-prod.yaml
common-mysql.yaml
common-redis.yaml
```

### 1.8 Gateway 路由设计

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product_route
          uri: lb://product-service
          predicates:
            - Path=/product/**
          filters:
            - StripPrefix=1
        - id: order_route
          uri: lb://order-service
          predicates:
            - Path=/order/**
          filters:
            - StripPrefix=1
```

设计要点：

- 所有外部请求先到网关。
- 网关负责统一日志、鉴权、跨域、灰度路由。
- 后续 OAuth2 鉴权平台可直接挂在这一层实现统一认证入口。

### 1.9 Feign 远程调用

Spring Cloud Alibaba 文档说明 Sentinel 可与 OpenFeign 集成，开启 `feign.sentinel.enabled=true` 后可以为 FeignClient 配置 fallback [page:4]。

```yaml
feign:
  sentinel:
    enabled: true
```

```java
@FeignClient(name = "product-service", fallback = ProductFeignFallback.class)
public interface ProductFeignClient {

    @GetMapping("/api/product/{id}")
    ProductDTO findById(@PathVariable("id") Long id);
}
```

```java
@Component
public class ProductFeignFallback implements ProductFeignClient {
    @Override
    public ProductDTO findById(Long id) {
        return new ProductDTO(id, "默认商品", 0);
    }
}
```

### 1.10 Sentinel 限流熔断

Spring Cloud Alibaba 文档指出 Sentinel 提供流量控制、熔断降级、实时监控，并支持 WebServlet、OpenFeign、RestTemplate 等接入场景；`@SentinelResource` 可指定资源名、`blockHandler` 与 `fallback` [page:4]。

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8719
```

```java
@GetMapping("/hot")
@SentinelResource(value = "hotProduct", blockHandler = "hotBlockHandler")
public String hotProduct() {
    return "hot product";
}

public String hotBlockHandler(BlockException e) {
    return "当前访问人数过多，请稍后再试";
}
```

### 1.11 可展示业务流程

1. 用户访问 `/order/create`。
2. Gateway 根据路由转发到订单服务。
3. 订单服务通过 Feign 调用商品服务校验商品与价格。
4. 商品服务、库存服务都已注册到 Nacos，支持服务发现与负载均衡 [page:4]。
5. 热点接口经过 Sentinel 限流，异常时触发 fallback 返回兜底结果 [page:4]。

### 1.12 面试亮点

- 通过 Nacos 完成服务注册发现和配置中心统一管理，降低多服务维护成本 [page:4]。
- 通过 Gateway 实现统一入口治理，提升路由与鉴权扩展性。
- 通过 OpenFeign + Sentinel 实现服务调用与熔断降级一体化。
- 通过模块化拆分展示标准微服务治理体系，符合中大厂常见技术栈要求 [web:17][page:4]。

---

## 2. 分布式订单系统

### 2.1 项目目标

实现一个跨订单服务、库存服务、账户服务的分布式下单链路，要求下单、扣库存、扣余额三步要么全部成功，要么全部回滚，并支持订单超时取消与库存回补。Seata 官方将自身定义为高性能、易用的分布式事务解决方案，并支持 Spring Cloud 场景 [web:28][page:3]。

### 2.2 业务链路

```text
用户下单
  ↓
订单服务创建订单（待支付）
  ↓
库存服务锁定库存
  ↓
账户服务扣减余额
  ↓
支付成功 -> 订单变已支付
  ↓
超时未支付 -> 自动取消订单并释放库存
```

### 2.3 核心问题

- 跨服务调用后如何保证数据一致性。
- 库存不能超卖，不能重复锁定。
- 订单超过 30 分钟未支付要自动取消。
- 取消后库存要释放，保证可再次销售。

### 2.4 技术方案

- Seata AT 模式：管理订单库、库存库、账户库的全局事务。
- Redis：保存订单超时任务状态、做防重控制。
- RabbitMQ：发送延迟取消消息。
- MySQL：保存订单、库存锁定记录、账户流水。

### 2.5 Seata 接入思路

Seata FAQ 指出，AT 模式要求使用数据源代理、业务表具备单列主键、每个业务库包含 `undo_log` 表，并支持 Spring Cloud 集成 [page:3]。

#### 关键依赖

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>2.x</version>
</dependency>
```

#### 核心事务代码

Seata 文档示例表明，分布式事务入口通常通过 `@GlobalTransactional` 启动；FAQ 还说明全局事务默认超时时间通常为 60 秒，可显式设置 `timeoutMills` [web:22][page:3]。

```java
@Service
public class OrderAppService {

    @Resource
    private OrderService orderService;
    @Resource
    private StockFeignClient stockFeignClient;
    @Resource
    private AccountFeignClient accountFeignClient;

    @GlobalTransactional(name = "create-order-tx", timeoutMills = 300000)
    public Long createOrder(CreateOrderCommand cmd) {
        Long orderId = orderService.createPendingOrder(cmd);
        stockFeignClient.lockStock(cmd.getProductId(), cmd.getCount(), orderId);
        accountFeignClient.debit(cmd.getUserId(), cmd.getAmount(), orderId);
        return orderId;
    }
}
```

### 2.6 表设计

#### `tb_order`

```sql
CREATE TABLE tb_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    count INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status TINYINT NOT NULL,
    expire_time DATETIME NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status (user_id, status)
);
```

#### `tb_stock`

```sql
CREATE TABLE tb_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    total_stock INT NOT NULL,
    lock_stock INT NOT NULL DEFAULT 0,
    available_stock INT NOT NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_id (product_id)
);
```

#### `tb_stock_lock`

```sql
CREATE TABLE tb_stock_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    count INT NOT NULL,
    status TINYINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_product (order_id, product_id)
);
```

### 2.7 库存锁定流程

推荐不要在下单时直接减真实库存，而是先锁定可用库存：

1. 检查 `available_stock >= 购买数量`。
2. 扣减 `available_stock`，增加 `lock_stock`。
3. 写入 `tb_stock_lock` 锁定记录。
4. 支付成功后正式扣减总库存并释放锁定库存。
5. 订单取消时释放锁定库存。

这样设计可以把“下单未支付”与“实际售出”分开，减少业务状态混乱。

### 2.8 订单超时取消

Seata FAQ 说明，全局事务存在超时概念；但“订单 30 分钟未支付自动取消”属于业务超时，不应依赖 Seata 事务超时去处理，而应通过 MQ 延迟消息或定时任务实现 [page:3]。

推荐方案：

- 下单成功后发送一条 30 分钟 TTL 的取消消息。
- 到期后消费者检查订单状态。
- 若仍为“待支付”，则关闭订单、释放库存。
- 若已经支付，则直接忽略。

### 2.9 取消订单代码骨架

```java
@Service
public class OrderTimeoutService {

    @Transactional
    public void closeOrder(Long orderId) {
        Order order = findById(orderId);
        if (order == null || order.getStatus() != 0) {
            return;
        }
        updateOrderStatus(orderId, 3);
        unlockStock(orderId);
    }
}
```

### 2.10 Seata 使用注意点

Seata FAQ 强调以下 AT 模式前提：必须使用代理数据源、每个业务库都要有 `undo_log`、业务表应采用单列主键；同时如果发生脏数据回滚失败，通常需要人工介入修正 [page:3]。

- 远程调用链路不能过长，否则更容易触发全局事务超时 [page:3]。
- RPC 超时、网络延迟可能导致全局事务已回滚而分支才到达注册阶段 [page:3]。
- 不要把耗时 MQ、第三方接口调用塞进全局事务主链路。
- 事务只包数据库核心步骤，其他步骤异步化。

### 2.11 面试亮点

- 基于 Seata AT 模式解决跨订单、库存、账户服务的一致性问题 [web:28][page:3]。
- 通过库存锁定表拆分“占用库存”和“真实扣减库存”，提升业务语义清晰度。
- 通过延迟消息实现超时订单自动关闭与库存自动回补。
- 理解 Seata 超时、XID 传播、undo_log、脏写与脏数据处理机制，面试深度明显高于普通 CRUD 项目 [page:3]。

---

## 3. 微服务后台权限平台

### 3.1 项目目标

实现一个统一认证中心 + 网关统一鉴权 + 后台 RBAC 权限控制平台，覆盖登录、发令牌、刷新令牌、菜单权限、接口权限、网关校验。Spring Authorization Server 官方项目用于实现 OAuth 2.1 与 OpenID Connect 1.0 授权服务器能力，Spring Security 文档也提供了对应授权服务器支持 [web:26][web:29]。

### 3.2 架构设计

```text
auth-server（认证中心）
   ├─ 用户登录
   ├─ client 管理
   ├─ access_token / refresh_token 发放
   └─ JWT 签发

gateway
   ├─ 登录放行
   ├─ Token 校验
   ├─ 黑名单校验
   └─ 权限头透传

system-service
   ├─ 用户管理
   ├─ 角色管理
   ├─ 菜单管理
   └─ 权限点管理
```

### 3.3 核心能力

- OAuth2.1 统一认证：登录、授权、发放 access token。
- Gateway 网关鉴权：所有请求先校验 token 再路由。
- Resource Server：业务服务只关注资源权限，不关心登录流程。
- RBAC 模型：用户、角色、菜单、权限点多对多关联。
- 统一认证中心：前后端分离项目只需对接一个登录入口。

### 3.4 权限模型设计

#### `sys_user`

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
);
```

#### `sys_role`

```sql
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_role_code (role_code)
);
```

#### `sys_menu`

```sql
CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    menu_name VARCHAR(64) NOT NULL,
    path VARCHAR(128) DEFAULT NULL,
    permission_code VARCHAR(128) DEFAULT NULL,
    type TINYINT NOT NULL,
    sort INT NOT NULL DEFAULT 0
);
```

#### `sys_user_role`

```sql
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
);
```

#### `sys_role_menu`

```sql
CREATE TABLE sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_menu (role_id, menu_id)
);
```

### 3.5 认证中心设计

Spring Authorization Server 项目说明其实现 OAuth 2.1 与 OpenID Connect 1.0 规范，适合作为现代 Spring 体系下的统一认证中心 [web:26][web:29]。

认证中心职责：

- 用户账号密码校验。
- 管理 OAuth2 Client。
- 签发 JWT Access Token。
- 提供刷新令牌能力。
- 提供 JWK 公钥供资源服务器验签。

### 3.6 认证服务核心配置思路

```java
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.formLogin(Customizer.withDefaults()).build();
    }
}
```

### 3.7 Gateway 鉴权流程

1. 前端先调用认证中心登录。
2. 认证中心签发 JWT。
3. 前端请求 Gateway 时携带 `Authorization: Bearer token`。
4. Gateway 验证 token 合法性、过期时间、黑名单状态。
5. 校验通过后把用户 ID、角色、权限透传给下游服务。
6. 下游资源服务根据权限点做细粒度控制。

### 3.8 Resource Server 权限控制

Spring Security 授权服务器与资源服务器体系通常配合 JWT scope/authority 完成接口保护，资源接口可通过 `@PreAuthorize` 等方式限制访问 [web:29][web:23]。

```java
@RestController
@RequestMapping("/admin/user")
public class UserController {

    @PreAuthorize("hasAuthority('sys:user:list')")
    @GetMapping("/list")
    public List<UserVO> list() {
        return Collections.emptyList();
    }
}
```

### 3.9 网关统一鉴权价值

- 避免每个服务都重复写登录校验逻辑。
- 可以在网关层统一做黑名单、续签、日志追踪。
- 权限平台后续扩展 SSO、第三方登录时改动更集中。
- 更符合中后台平台类项目的真实落地方式。

### 3.10 推荐扩展功能

- 登录失败次数限制。
- Redis 黑名单注销机制。
- 权限变更后强制令牌失效。
- 操作日志、登录日志、审计日志。
- 菜单树懒加载与按钮级权限控制。

### 3.11 面试亮点

- 基于 Spring Authorization Server 实现统一认证中心，具备 OAuth2.1 标准化授权能力 [web:26][web:29]。
- 在 Gateway 层统一完成 token 校验与权限透传，减少业务服务重复开发。
- 通过 RBAC 实现用户、角色、菜单、权限点全链路权限模型。
- 平台架构具备很强的企业后台复用价值，适合作为实习与校招的微服务权限类代表项目。

---

## 4. 统一目录结构建议

```text
springcloud-projects/
├── mall-cloud/
│   ├── mall-gateway/
│   ├── mall-user-service/
│   ├── mall-product-service/
│   ├── mall-cart-service/
│   ├── mall-order-service/
│   ├── mall-pay-service/
│   └── mall-common/
├── distributed-order/
│   ├── order-service/
│   ├── stock-service/
│   ├── account-service/
│   ├── order-job/
│   └── order-common/
└── auth-platform/
    ├── auth-server/
    ├── gateway/
    ├── system-service/
    ├── user-service/
    └── common-security/
```

---

## 5. 技术栈建议

| 层级 | 选型 |
|---|---|
| 微服务框架 | Spring Boot 3 + Spring Cloud Alibaba |
| 注册中心 | Nacos |
| 配置中心 | Nacos Config |
| 服务调用 | OpenFeign |
| 网关 | Spring Cloud Gateway |
| 熔断限流 | Sentinel |
| 分布式事务 | Seata |
| 缓存 | Redis |
| 消息队列 | RabbitMQ |
| 数据库 | MySQL 8 |
| 认证授权 | Spring Security + Spring Authorization Server |
| 构建工具 | Maven |
| 容器部署 | Docker Compose |

---

## 6. 简历写法模板

### SpringCloud 电商微服务

- 基于 Spring Cloud Alibaba 搭建电商微服务系统，完成 Nacos 服务注册发现、配置中心、Gateway 网关路由与 OpenFeign 服务调用 [page:4]。
- 接入 Sentinel 实现热点接口限流、熔断降级与服务兜底，提升系统稳定性 [page:4]。
- 完成用户、商品、购物车、订单等模块拆分，具备标准微服务治理能力。

### 分布式订单系统

- 基于 Seata 实现订单、库存、账户三服务分布式事务一致性，保障下单链路原子性 [web:28][page:3]。
- 设计库存锁定与订单超时取消机制，结合延迟消息完成库存自动回补。
- 理解并落地 XID 传播、undo_log、事务超时、异常回滚等 Seata 核心机制 [page:3]。

### 微服务后台权限平台

- 基于 Spring Authorization Server 搭建统一认证中心，实现 OAuth2.1 登录授权与 JWT 令牌签发 [web:26][web:29]。
- 在 Gateway 层实现统一鉴权与权限透传，结合 Resource Server 完成接口级权限控制。
- 基于 RBAC 模型设计用户、角色、菜单、权限点管理体系，支撑后台平台统一权限治理。

---

## 7. 推荐完成顺序

建议按以下顺序推进：

1. 先做 SpringCloud 电商微服务，建立服务治理全局认知。
2. 再做分布式订单系统，把事务一致性、库存锁定、超时取消补齐。
3. 最后做微服务后台权限平台，形成“交易系统 + 平台系统”双项目组合。

这个组合非常适合你当前的 Java 后端 + 企业级项目路线：前者突出服务治理，中者突出复杂业务一致性，后者突出平台化权限设计，放在简历里层次会非常清晰。
