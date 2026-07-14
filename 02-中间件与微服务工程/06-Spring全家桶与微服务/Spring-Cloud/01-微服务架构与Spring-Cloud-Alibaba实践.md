# 微服务架构与Spring Cloud Alibaba实践

> 本文从架构演进出发，系统梳理微服务架构的核心设计理念，并结合 Spring Cloud Alibaba 生态对 Nacos、Sentinel、Gateway、Seata 等关键组件进行深度剖析，提供完整的代码实践与生产环境配置经验。

---

## 目录

1. [微服务架构概念](#1-微服务架构概念)
2. [Nacos — 服务注册与配置中心](#2-nacos--服务注册与配置中心)
3. [Sentinel — 流量控制与熔断降级](#3-sentinel--流量控制与熔断降级)
4. [Gateway — API网关](#4-gateway--api网关)
5. [Seata — 分布式事务](#5-seata--分布式事务)
6. [其他重要组件](#6-其他重要组件)
7. [总结清单](#7-总结清单)

---

## 1. 微服务架构概念

### 1.1 从单体架构到微服务架构

#### 单体架构（Monolithic Architecture）

单体架构是将一个应用的所有功能模块（用户管理、订单处理、支付、库存等）打包在同一个部署单元中的传统架构风格。通常表现为一个 WAR 包或一个可执行 JAR 文件。

**单体架构的优势：**

| 维度 | 说明 |
|------|------|
| 开发初期效率高 | 项目结构简单，IDE 加载快，调试方便，一个 Spring Boot 应用即可启动 |
| 测试简单 | 端到端测试只需启动一个服务实例 |
| 部署便捷 | 单点部署，只需将 WAR/JAR 放到容器中运行 |
| 调用开销低 | 方法调用在进程内完成，无网络延迟 |

**单体架构的劣势（随规模增长而放大）：**

| 维度 | 问题描述 |
|------|----------|
| 开发效率下降 | 代码耦合严重，多人协作时频繁冲突，编译时间可达数十分钟 |
| 部署困难 | 细微修改也需要重新部署整个应用，风险大、周期长 |
| 扩展性差 | 无法针对热点模块独立扩容，只能整体水平扩展，资源浪费严重 |
| 技术栈锁定 | 所有模块必须使用同一语言和框架，新技术引入成本极高 |
| 可靠性不足 | 某个模块的内存泄漏会拖垮整个进程，影响所有功能 |

#### 微服务架构

微服务架构将单一应用拆分为一组小型的、自治的服务，每个服务围绕业务能力构建，拥有独立的数据库、独立的部署流水线和独立的研发团队。

**微服务架构的优势：**

- **独立部署**：修改某个服务只需重新部署该服务，不影响其他服务
- **技术多样性**：不同服务可选择最适合的技术栈（如 Python 做 AI 推理、Go 做高并发网关、Java 做业务核心）
- **独立扩展**：可针对热点服务单独扩容，如大促期间只扩容订单服务和支付服务
- **故障隔离**：某个服务崩溃不会导致整个系统不可用（服务熔断、降级机制可兜底）
- **团队自治**：小团队负责小服务，沟通成本低，交付效率高

**微服务架构的劣势：**

- **分布式复杂性**：网络延迟、服务间通信、数据一致性、分布式事务等新问题出现
- **运维成本激增**：服务数量从几个变为几十上百个，监控、日志、告警体系的建设复杂度指数级上升
- **测试难度增加**：端到端测试需要搭建完整的服务环境
- **数据一致性问题**：各服务拥有独立数据库，传统数据库事务不再适用
- **服务治理需求**：需要服务发现、负载均衡、配置中心、链路追踪、熔断降级等基础设施

### 1.2 服务拆分原则

将单体应用拆分为微服务并非随意切分，需要遵循以下原则：

#### 单一职责原则（Single Responsibility Principle）

每个服务只负责一个明确的业务能力。例如"订单服务"只处理订单的创建、修改、查询，而不应包含支付逻辑或用户认证逻辑。一个服务只因为一个业务原因而变更。

#### 高内聚低耦合

- **高内聚**：相关的业务逻辑和行为聚集在同一服务内部，修改时只影响该服务
- **低耦合**：服务之间通过轻量级通信机制（REST/gRPC/消息队列）交互，尽量避免同步调用链过长

#### DDD 限界上下文（Bounded Context）

领域驱动设计（DDD）中的限界上下文是拆分微服务的最佳指导原则之一。每个限界上下文对应一个微服务，包含该上下文内的实体、值对象、领域服务、仓储等。限界上下文之间的通信通过上下文映射（Context Map）来定义。

```
[用户上下文] <---> [订单上下文] <---> [库存上下文] <---> [支付上下文]
  用户服务         订单服务          库存服务          支付服务
```

**DDD 拆分的实用步骤：**

1. **事件风暴（Event Storming）**：业务专家与开发团队一起梳理业务领域事件
2. **识别聚合（Aggregate）**：找到强一致性边界，每个聚合是一个事务一致性单元
3. **划分限界上下文**：一组相关的聚合构成一个限界上下文
4. **每个上下文映射为一个微服务**

#### 服务粒度控制

过于微小的服务导致"微服务爆炸"（Microservices Explosion），带来严重的运维和通信开销。推荐的粒度原则：

- **可独立交付**：每个服务应当能够独立开发、测试和部署
- **团队规模匹配**：一个服务由一个 2-8 人的小队（Squad）维护
- **数据独立**：每个服务拥有自己的数据库或 schema
- **避免跨服务事务**：尽量通过最终一致性而非分布式事务来保证数据一致性

### 1.3 CAP 定理与 BASE 理论

#### CAP 定理

分布式系统无法同时满足以下三个特性：

| 特性 | 说明 |
|------|------|
| **C**onsistency（一致性） | 所有节点在同一时刻看到相同的数据 |
| **A**vailability（可用性） | 每个请求都能获得（非错误的）响应 |
| **P**artition Tolerance（分区容错性） | 系统在网络分区时仍能正常运行 |

**核心结论**：在网络分区必然发生的前提下，只能在 C 和 A 之间做出选择。

- **CP 系统**：优先保证一致性，牺牲可用性。例如 Zookeeper、Etcd。当 Leader 故障时，会进行选举，选举期间不可用。
- **AP 系统**：优先保证可用性，牺牲强一致性，采用最终一致性。例如 Eureka、Nacos（AP 模式）。当出现网络分区时，各分区仍可正常提供服务，数据在分区恢复后同步。

**生产环境的思考：**

> 大多数互联网业务场景（如商品浏览、下单）更适合 AP 系统——短暂的数据不一致可以接受，但系统不可用直接导致用户流失和收入损失。而对于支付、库存扣减等场景，则需要引入分布式事务来保证最终一致性。

#### BASE 理论

BASE 是对 CAP 中 AP 方案的延伸：

- **Basically Available（基本可用）**：系统允许部分功能降级，保障核心功能可用
- **Soft State（软状态）**：允许数据存在中间状态，副本同步存在延迟
- **Eventually Consistent（最终一致性）**：在没有新写入后，经过一段时间所有副本数据最终一致

### 1.4 服务治理核心问题

微服务架构带来了六个核心治理问题，也是 Spring Cloud Alibaba 生态重点解决的问题：

| 问题 | 描述 | 对应组件 |
|------|------|----------|
| **服务发现** | 服务实例动态注册与消费方自动感知 | Nacos |
| **负载均衡** | 请求在多个服务实例间合理分发 | LoadBalancer / Nacos |
| **配置管理** | 配置集中管理、动态刷新、环境隔离 | Nacos Config |
| **容错** | 服务调用失败时的熔断、降级、限流 | Sentinel |
| **链路追踪** | 全链路调用追踪，快速定位故障点 | Skywalking / Zipkin |
| **API网关** | 统一入口、路由转发、鉴权、限流 | Spring Cloud Gateway |

---

## 2. Nacos — 服务注册与配置中心

Nacos（Dynamic Naming and Configuration Service）是阿里巴巴开源的一站式服务注册与配置管理中心，支持服务发现、配置管理和动态 DNS 服务。

### 2.1 服务注册与发现

#### 服务提供者注册

在 Spring Boot 应用中集成 Nacos 服务注册，只需添加依赖和配置：

**pom.xml：**
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

**application.yml：**
```yaml
spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: public
        # 临时实例（默认true）—— 心跳方式注册，不健康则剔除
        # 持久实例 —— 使用Raft协议持久化，手动注销
        ephemeral: true
        # 集群名，用于同城双活/异地多活
        cluster-name: SH
```

**启动类：**
```java
@SpringBootApplication
@EnableDiscoveryClient
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

#### 服务消费者发现与调用

服务消费者可以从 Nacos 获取服务实例列表，结合负载均衡发起调用：

```java
@Component
public class OrderServiceClient {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private RestTemplate restTemplate;

    public String getOrderInfo(Long orderId) {
        // 从 Nacos 获取 user-service 的实例列表
        List<ServiceInstance> instances = discoveryClient
            .getInstances("user-service");
        if (instances.isEmpty()) {
            throw new RuntimeException("user-service 无可用实例");
        }
        // 简单轮询
        ServiceInstance instance = instances.get(0);
        String url = String.format("http://%s:%d/user/%d",
            instance.getHost(), instance.getPort(), orderId);
        return restTemplate.getForObject(url, String.class);
    }
}
```

**生产环境中建议配合 OpenFeign 或 LoadBalancer 使用**（见第 6 章）。

#### 临时实例 vs 持久实例

| 对比维度 | 临时实例（ephemeral: true） | 持久实例（ephemeral: false） |
|---------|---------------------------|---------------------------|
| 注册方式 | 客户端主动发送心跳（5s 间隔） | 服务端主动探测 |
| 健康检查 | 客户端心跳；15s 未收到则标记不健康，30s 剔除 | 服务端 TCP/HTTP/MySQL 探测 |
| 容错性 | AP 模式，适合动态扩缩容的云原生场景 | CP 模式，适合需要强一致性的场景 |
| 典型场景 | 微服务实例，随时扩缩容 | 数据库实例、固定节点 |

### 2.2 健康检查机制

Nacos 的健康检查分为客户端心跳和服务端探测两层：

```
客户端心跳（临时实例）：
  服务提供者 → 每 5s 发送心跳到 Nacos Server
  Nacos Server → 15s 未收到标记不健康，30s 删除实例

服务端主动探测（持久实例）：
  Nacos Server → 按配置周期发起 TCP 连接 / HTTP GET 请求 / SQL 查询
  探测失败达阈值 → 标记实例不健康
```

**心跳配置（客户端）：**
```yaml
spring:
  cloud:
    nacos:
      discovery:
        heart-beat-interval: 5000    # 心跳间隔（ms）
        heart-beat-retry: 3          # 心跳重试次数
        ip-delete-timeout: 30000     # 实例删除超时时间（ms）
```

### 2.3 配置管理

Nacos Config 提供了统一的配置管理能力，核心概念包括：

| 概念 | 说明 | 类比 |
|------|------|------|
| **Data ID** | 配置的唯一标识，通常使用 `${spring.application.name}-${profile}.${file-extension}` | 文件名 |
| **Group** | 配置分组，默认 `DEFAULT_GROUP` | 文件夹 |
| **Namespace** | 命名空间，实现多环境/多租户隔离 | 独立文件柜 |

#### 集成 Nacos Config

**pom.xml：**
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

**bootstrap.yml（注意：必须使用 bootstrap.yml，不能是 application.yml）：**
```yaml
spring:
  application:
    name: order-service
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: dev               # 开发环境
        group: DEFAULT_GROUP
        file-extension: yaml         # 配置格式
        refresh-enabled: true        # 开启动态刷新

# 多配置集加载
  profiles:
    active: dev

# 引用外部共享配置
# spring.cloud.nacos.config.extension-configs[0].data-id: common.yaml
# spring.cloud.nacos.config.extension-configs[0].group: SHARED_GROUP
# spring.cloud.nacos.config.extension-configs[0].refresh: true
```

**配置中心 Data ID 规则：**

```
order-service.yaml          # 基础配置（所有环境共享）
order-service-dev.yaml      # 开发环境配置
order-service-test.yaml     # 测试环境配置
order-service-prod.yaml     # 生产环境配置
```

### 2.4 配置动态刷新

Nacos 支持在不重启应用的情况下动态刷新配置。有三种方式可以实现：

#### 方式一：@RefreshScope

```java
@RestController
@RefreshScope  // 关键注解：当配置变更时重新创建 Bean
public class OrderTimeoutController {

    @Value("${order.timeout:5000}")
    private Integer orderTimeout;

    @GetMapping("/timeout")
    public String getTimeout() {
        return "当前超时时间：" + orderTimeout + "ms";
    }
}
```

#### 方式二：@NacosValue（原生 API）

```java
@RestController
public class DynamicConfigController {

    @NacosValue(value = "${order.discount:0.9}", autoRefreshed = true)
    private String discount;

    @GetMapping("/discount")
    public String getDiscount() {
        return "当前折扣：" + discount;
    }
}
```

#### 方式三：监听器方式

```java
@Component
public class ConfigChangeListener
        implements ApplicationListener<NacosConfigReceivedEvent> {

    @Override
    public void onApplicationEvent(NacosConfigReceivedEvent event) {
        System.out.println("配置已变更，dataId：" + event.getDataId());
        System.out.println("最新内容：" + event.getContent());
    }
}
```

### 2.5 多环境管理

通过 Nacos Namespace 实现环境隔离是最佳实践：

```
Namespace: dev        → 开发环境
Namespace: test       → 测试环境
Namespace: preprod    → 预发布环境
Namespace: prod       → 生产环境
```

**创建 Namespace 后，每个环境拥有独立的配置集和服务实例视图：**

```yaml
# bootstrap.yml
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: a1b2c3d4-xxxx-xxxx-xxxx-xxxxxxxxxxxx  # Namespace ID
      discovery:
        namespace: ${spring.cloud.nacos.config.namespace}
```

### 2.6 CAP 模式切换

Nacos 在 1.x 版本后支持 AP 和 CP 模式的切换：

```java
// 通过 HTTP API 切换 Nacos Server 模式
// AP 模式（临时实例）
curl -X PUT "localhost:8848/nacos/v1/ns/operator/switches?entry=serverMode&value=AP"

// CP 模式（持久实例）
curl -X PUT "localhost:8848/nacos/v1/ns/operator/switches?entry=serverMode&value=CP"
```

**生产建议：**

- **微服务场景**：使用 AP 模式 + 临时实例（默认），保持高可用
- **配置中心场景**：Nacos Server 集群本身使用 CP 模式（基于 Raft 协议保证配置一致性）
- **服务发现场景**：再精确定义下，Nacos 服务发现模块支持二者切换，但推荐微服务使用 AP 模式

### 2.7 与 Eureka 对比

| 对比维度 | Nacos | Eureka 2.x |
|---------|-------|------------|
| 定位 | 服务注册 + 配置中心 | 仅服务注册 |
| CAP 模型 | 支持 AP / CP 切换 | 仅 AP（无 CP 选项） |
| 健康检查 | 心跳 + 服务端主动探测 | 仅心跳（客户端心跳） |
| 自我保护 | 可配置，更灵活 | 强制开启，无法关闭 |
| 配置管理 | 内置配置中心，支持动态刷新 | 需要搭配 Spring Cloud Config |
| 维护状态 | 阿里巴巴活跃维护 | Netflix 已停止维护（进入维护模式） |
| 控制台 | 功能丰富的 Web 控制台 | 较简单的 Web UI |
| 集群部署 | 支持 AP + CP 集群（Raft） | 仅 AP，节点间最终一致 |

> **Eureka 自我保护机制**：当 Eureka Server 在短时间内丢失过多客户端心跳时，会进入自我保护模式，不再剔除任何服务实例。这在网络抖动时可以保护现有实例不被错误剔除，但也意味着"死实例"会继续留在注册表中，导致消费者调用失败。推荐迁移到 Nacos。

---

## 3. Sentinel — 流量控制与熔断降级

Sentinel 是阿里巴巴开源的流量控制、熔断降级组件，以其轻量级、高性能和多维度的防护能力被广泛采用。

### 3.1 快速集成

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

**application.yml：**
```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: 127.0.0.1:8080   # Sentinel 控制台地址
      eager: true                      # 启动时立即注册到控制台
      datasource:
        ds-nacos:
          nacos:
            server-addr: 127.0.0.1:8848
            namespace: sentinel-rules
            data-id: ${spring.application.name}-sentinel-flow.json
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: flow           # 支持 flow/degrade/param-flow/system/authority
```

### 3.2 流量控制

流量控制（Flow Control）是 Sentinel 最核心的功能，通过监控 QPS 或并发线程数来控制流量。

#### 基础限流配置

```java
@RestController
public class OrderController {

    @GetMapping("/order/create")
    @SentinelResource(value = "createOrder", blockHandler = "handleBlock")
    public String createOrder(@RequestParam Long userId, @RequestParam Long productId) {
        return "订单创建成功";
    }

    // 限流后的兜底方法
    public String handleBlock(Long userId, Long productId, BlockException e) {
        return "请求过于频繁，请稍后再试。错误：" + e.getClass().getSimpleName();
    }
}
```

**控制台配置规则参数详解：**

| 参数 | 说明 |
|------|------|
| **资源名** | 唯一标识，对应 `@SentinelResource.value` |
| **阈值类型** | QPS（每秒请求数）或 线程数 |
| **单机阈值** | 允许通过的请求数量上限 |
| **流控模式** | 直接 / 关联 / 链路 |
| **流控效果** | 快速失败 / Warm Up / 排队等待 |

#### 流控模式详解

**直接模式**：当资源访问量超过阈值时直接限流，是最常用的模式。

**关联模式**：当关联资源达到阈值时，对当前资源进行限流。典型场景：一个服务有读接口和写接口，当写接口压力过大时，对读接口限流以保护数据库。

```yaml
# 控制台配置：/order/write 达到 1000 QPS 时，限制 /order/read
# 资源：/order/read
# 流控模式：关联
# 关联资源：/order/write
# 阈值：500 QPS
```

**链路模式**：根据调用链入口进行限流。适合需要区分不同调用来源的场景。

#### 流控效果详解

**快速失败（Default）**：超过阈值立即抛出 `FlowException`，默认行为。

**Warm Up（预热）**：阈值从 `coldFactor`（默认 3）逐步提高到设定值，适用于需要预热的场景（如数据库连接池初始化、JIT 编译预热）。

```
设定阈值：1000 QPS
Warm Up 周期：10s
┌─────────────────────────────────┐
│ 时间 0s  → 阈值 333 QPS         │
│ 时间 5s  → 阈值 667 QPS         │
│ 时间 10s → 阈值 1000 QPS（稳定）│
└─────────────────────────────────┘
```

**排队等待（Throttling）**：请求以均匀速度通过，超出排队超时时间则拒绝。适用于削峰填谷。

```java
// 控制台配置
// 流控效果：排队等待
// 阈值：500 QPS
// 超时时间：500ms
// 效果：请求以每 2ms 一个的速度通过，超出排队时间则拒绝
```

### 3.3 熔断降级

熔断降级用于保护系统在依赖服务出现故障时不被拖垮。

#### 熔断策略配置

```java
@GetMapping("/user/info")
@SentinelResource(value = "getUserInfo",
    fallback = "handleFallback",
    fallbackClass = UserServiceFallback.class)
public UserInfo getUserInfo(Long userId) {
    // 调用用户服务
}
```

| 熔断策略 | 说明 | 配置参数 |
|---------|------|----------|
| **慢调用比例** | 响应时间超过阈值的请求比例达到设定值时熔断 | maxRt（最大RT）、ratioThreshold（比例阈值）、minRequestAmount（最小请求数）、statIntervalMs（统计时长） |
| **异常比例** | 异常请求比例达到阈值时熔断 | ratioThreshold（比例阈值）、minRequestAmount |
| **异常数** | 异常请求数量达到阈值时熔断 | count（异常数阈值）、minRequestAmount |

**熔断器状态机：**

```
CLOSED（关闭） —— 正常状态，请求正常通过
    │
    ├── 触发条件满足 ──────────→ OPEN（开启）—— 请求直接降级
    │                                   │
    │                           （等待熔断时长，默认 5s）
    │                                   │
    │                                   ▼
    └──────── 请求成功 ──── HALF_OPEN（半开）—— 放行少量探测请求
                                │
                                ├── 探测成功 → CLOSED（恢复）
                                └── 探测失败 → OPEN（继续熔断）
```

**熔断配置示例（通过 Nacos 持久化）：**

```json
[
    {
        "resource": "getUserInfo",
        "grade": 0,
        "count": 200,
        "timeWindow": 10,
        "minRequestAmount": 5,
        "statIntervalMs": 1000,
        "slowRatioThreshold": 0.5
    }
]
```

参数说明：
- `grade: 0` — 慢调用比例熔断
- `count: 200` — 最大 RT 为 200ms
- `timeWindow: 10` — 熔断时长 10s
- `slowRatioThreshold: 0.5` — 慢调用比例超过 50% 触发熔断

### 3.4 热点参数限流

热点参数限流允许针对具体的参数值进行精细化限流：

```java
@GetMapping("/product/detail")
@SentinelResource(value = "getProductDetail", blockHandler = "handleBlock")
public String getProductDetail(@RequestParam Long productId,
                                @RequestParam(defaultValue = "normal") String type) {
    return "商品详情";
}

// 热点规则（控制台配置）：
// 资源名：getProductDetail
// 参数索引：0（productId）
// 单机阈值：100
// 统计窗口时长：1s
//
// 参数例外项（热点商品限流）：
//   参数值：9527（爆款商品）→ 阈值：10
//   参数值：9528               → 阈值：50
```

热点规则在控制台的配置路径：`热点规则 → 新增热点规则 → 选择资源 → 配置参数索引和阈值`。热点参数限流对缓存穿透防护、商品秒杀场景非常有效。

### 3.5 系统自适应保护

系统自适应保护（System Protection）从整体维度对系统进行保护，而非单个资源。Sentinel 支持以下保护模式：

| 模式 | 说明 |
|------|------|
| **LOAD** | 系统负载（Load1）超过阈值时触发，仅对 Linux/Unix 有效 |
| **RT** | 所有入口流量的平均 RT 达到阈值时触发 |
| **线程数** | 所有入口流量的并发线程数达到阈值时触发 |
| **入口 QPS** | 所有入口流量的 QPS 达到阈值时触发 |

```json
// 系统规则示例（通过 API 配置）
[
    {
        "resource": "order-service",
        "load": 5.0,
        "avgRt": 100,
        "maxThread": 200,
        "qps": 10000,
        "highestSystemLoad": 5.0
    }
]
```

### 3.6 @SentinelResource 注解详解

`@SentinelResource` 是 Sentinel 在 Spring 生态中的核心注解：

| 属性 | 类型 | 说明 |
|------|------|------|
| `value` | String | 资源名称（唯一标识） |
| `entryType` | EntryType | 入口类型，IN/OUT |
| `blockHandler` | String | 限流/熔断降级后调用的方法名 |
| `blockHandlerClass` | Class<?> | blockHandler 所在类（需为静态方法） |
| `fallback` | String | 业务异常时的降级方法名 |
| `fallbackClass` | Class<?> | fallback 所在类（需为静态方法） |
| `defaultFallback` | String | 默认降级方法 |
| `exceptionsToIgnore` | Class<? extends Throwable>[] | 不触发降级的异常 |

**完整示例：**

```java
@RestController
public class PaymentController {

    @PostMapping("/payment/create")
    @SentinelResource(
        value = "createPayment",
        blockHandler = "handleBlockPayment",    // 限流/熔断时触发
        fallback = "handlePaymentError",        // 业务异常时触发
        exceptionsToIgnore = {IllegalArgumentException.class}
    )
    public Payment createPayment(@RequestBody PaymentRequest request) {
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        // 调用支付服务
        return paymentService.create(request);
    }

    // blockHandler 方法签名必须与原始方法一致，但可以多一个 BlockException 参数
    public Payment handleBlockPayment(PaymentRequest request, BlockException e) {
        log.warn("支付接口被限流", e);
        Payment fallback = new Payment();
        fallback.setStatus("LIMITED");
        return fallback;
    }

    // fallback 方法签名必须与原始方法一致
    public Payment handlePaymentError(PaymentRequest request, Throwable e) {
        log.error("支付创建失败", e);
        Payment fallback = new Payment();
        fallback.setStatus("FAILED");
        return fallback;
    }
}
```

### 3.7 Sentinel 控制台

Sentinel 控制台提供可视化的规则管理和实时监控：

**部署方式：**
```bash
# 下载 sentinel-dashboard
java -Dserver.port=8080 \
     -Dcsp.sentinel.dashboard.server=localhost:8080 \
     -Dproject.name=sentinel-dashboard \
     -jar sentinel-dashboard.jar
```

**控制台核心功能：**

1. **实时监控**：每个服务的 QPS、RT、线程数、成功率等实时曲线
2. **规则管理**：可视化的流控、熔断、热点、系统规则配置
3. **规则持久化**：通过 `DataSource` 扩展将规则持久化到 Nacos、Zookeeper、Apollo 等
4. **集群流控**：集群模式下 Token Server / Token Client 的流量控制

**规则持久化到 Nacos 的关键配置：**
```yaml
spring:
  cloud:
    sentinel:
      datasource:
        ds-flow:
          nacos:
            server-addr: 127.0.0.1:8848
            dataId: ${spring.application.name}-sentinel-flow
            groupId: SENTINEL_GROUP
            rule-type: flow
        ds-degrade:
          nacos:
            server-addr: 127.0.0.1:8848
            dataId: ${spring.application.name}-sentinel-degrade
            groupId: SENTINEL_GROUP
            rule-type: degrade
```

### 3.8 与 Hystrix 对比

| 对比维度 | Sentinel | Hystrix（已停止维护） |
|---------|----------|----------------------|
| 隔离方式 | 信号量隔离（默认） | 线程池隔离 + 信号量隔离 |
| 线程开销 | 无额外线程开销 | 线程池隔离增加线程切换开销 |
| 限流能力 | 丰富的 QPS/线程数/热点限流 | 仅支持线程数限制 |
| 熔断策略 | 慢调用/异常比例/异常数 | 仅异常比例 |
| 动态规则 | 控制台 + Nacos/APollo 动态推送 | 配置中心动态更新（需配合） |
| 实时监控 | 内置控制台 + 监控 API | 需集成 Turbine + Dashboard |
| 预热支持 | 内置 Warm Up 模式 | 不支持 |
| 流量整形 | 排队等待（漏桶/令牌桶） | 不支持 |
| 社区活跃度 | 阿里巴巴持续维护 | Netflix 已进入维护模式 |

> **线程池隔离 vs 信号量隔离**：Hystrix 的线程池隔离为每个依赖分配独立线程池，隔离性强但线程切换开销大（每个请求经历 2-3 次上下文切换）。Sentinel 使用信号量隔离，没有线程切换开销，性能更好，适合大部分业务场景。如果对隔离性有极端要求（如避免某个慢调用耗尽连接池），也可以结合线程池使用。

---

## 4. Gateway — API 网关

Spring Cloud Gateway 是基于 Spring WebFlux 构建的 API 网关，提供路由、过滤、限流等能力。

### 4.1 核心概念

Spring Cloud Gateway 的三个核心组件：

| 组件 | 说明 |
|------|------|
| **Route（路由）** | 网关的基础构建块，包含 ID、目标 URI、断言集合、过滤器集合 |
| **Predicate（断言）** | 匹配 HTTP 请求的条件，如路径、Header、参数、Cookie |
| **Filter（过滤器）** | 对请求和响应进行拦截和修改，支持链式编排 |

**请求处理流程：**

```
客户端 → Gateway → Predicate 匹配 → 过滤器链（pre）→ 目标服务
                ↑                                        │
                └──────── 过滤器链（post） ←── 响应 ←─────┘
```

### 4.2 路由配置

#### 基础路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service          # 使用 Nacos 负载均衡
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=1               # 去掉 /api 前缀
            - AddRequestHeader=X-Gateway, true

        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1

        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/product/**
          filters:
            - StripPrefix=1
```

#### 动态路由（从 Nacos 获取路由配置）

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true                    # 启用服务发现路由
          lower-case-service-id: true      # 服务名转小写
```

**高级动态路由（通过 Nacos 配置中心管理路由规则）：**

```java
@Component
public class NacosDynamicRouteService
        implements ApplicationEventPublisherAware {

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    private ApplicationEventPublisher publisher;

    private static final String DATA_ID = "gateway-routes.json";
    private static final String GROUP = "GATEWAY_GROUP";

    @PostConstruct
    public void init() throws NacosException {
        // 初始化 Nacos 配置监听
        ConfigService configService = NacosFactory.createConfigService("127.0.0.1:8848");
        String config = configService.getConfig(DATA_ID, GROUP, 5000);
        publishRoutes(config);

        configService.addListener(DATA_ID, GROUP, new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                publishRoutes(configInfo);
            }

            @Override
            public Executor getExecutor() {
                return Executors.newSingleThreadExecutor();
            }
        });
    }

    private void publishRoutes(String config) {
        // 解析 JSON 配置并发布路由
        List<RouteDefinition> routes = JSON.parseArray(config, RouteDefinition.class);
        routes.forEach(route -> {
            routeDefinitionWriter.save(Mono.just(route)).subscribe();
            publisher.publishEvent(new RefreshRoutesEvent(this));
        });
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher appPublisher) {
        this.publisher = appPublisher;
    }
}
```

**对应的 Nacos 配置（gateway-routes.json）：**
```json
[
    {
        "id": "order-service",
        "uri": "lb://order-service",
        "predicates": [{
            "name": "Path",
            "args": {"pattern": "/api/order/**"}
        }],
        "filters": [{
            "name": "StripPrefix",
            "args": {"parts": 1}
        }]
    }
]
```

### 4.3 断言（Predicate）详解

Spring Cloud Gateway 内置了多种断言工厂：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**              # 路径匹配
            - Method=GET,POST                # HTTP 方法匹配
            - Header=X-Request-Type, \d+     # Header 匹配（正则）
            - Query=userId, \d+              # 查询参数匹配
            - Cookie=sessionId, [a-z0-9]+    # Cookie 匹配
            - Host=**.example.com            # Host 匹配
            - RemoteAddr=192.168.1.1/24      # IP 地址匹配
            - Weight=group1, 80              # 权重路由（灰度发布）
```

**自定义断言：**

```java
@Component
public class TimeBetweenRoutePredicateFactory
        extends AbstractRoutePredicateFactory<TimeBetweenRoutePredicateFactory.Config> {

    public TimeBetweenRoutePredicateFactory() {
        super(Config.class);
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            LocalTime now = LocalTime.now();
            return !now.isBefore(config.getStart()) && !now.isAfter(config.getEnd());
        };
    }

    @Validated
    public static class Config {
        private LocalTime start;
        private LocalTime end;
        // getters & setters
    }
}
```

### 4.4 过滤器（Filter）详解

Gateway 过滤器分为两种：

- **GatewayFilter**：针对特定路由的局部过滤器
- **GlobalFilter**：作用于全局所有路由的过滤器

#### 内置 GatewayFilter 示例

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=1                  # 去掉前缀
            - AddRequestHeader=X-Gateway, true  # 添加请求头
            - AddRequestParameter=source, gateway  # 添加请求参数
            - AddResponseHeader=X-Response-Time, 2024  # 添加响应头
            - PrefixPath=/api               # 添加前缀
            - RedirectTo=302, https://new.example.com  # 重定向
            - Retry=3                       # 重试
            - RequestRateLimiter=           # 限流（见 4.5 节）
```

#### 自定义 GlobalFilter（鉴权过滤器）

```java
@Component
@Order(-1)  // 优先级最高，最先执行
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Set<String> WHITE_LIST = Set.of(
        "/api/user/login",
        "/api/user/register",
        "/api/product/list"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单路径放行
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }

        // 检查 Token
        String token = exchange.getRequest().getHeaders()
            .getFirst("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 验证 Token（调用认证服务或本地解析 JWT）
        String userId = parseUserIdFromToken(token);
        if (userId == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 将用户信息传递到下游服务
        exchange.getRequest().mutate()
            .header("X-User-Id", userId);

        return chain.filter(exchange);
    }

    private String parseUserIdFromToken(String token) {
        // JWT 解析逻辑
        try {
            Claims claims = Jwts.parser()
                .setSigningKey("secret-key".getBytes())
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
```

#### 自定义 GatewayFilter

```java
@Component
public class RequestLogGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RequestLogGatewayFilterFactory.Config> {

    public RequestLogGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long start = System.currentTimeMillis();
            String path = exchange.getRequest().getURI().getPath();

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - start;
                int status = exchange.getResponse().getStatusCode().value();

                if (config.isLogHeaders()) {
                    HttpHeaders headers = exchange.getRequest().getHeaders();
                    log.info("[{}] {} {}ms - Headers: {}", status, path, duration, headers);
                } else {
                    log.info("[{}] {} {}ms", status, path, duration);
                }
            }));
        };
    }

    public static class Config {
        private boolean logHeaders = false;
        // getters & setters
    }
}
```

### 4.5 限流实现

Gateway 通过 `RequestRateLimiter` 过滤器结合 Redis 实现令牌桶限流：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
          filters:
            - name: RequestRateLimiter
              args:
                key-resolver: "#{@userKeyResolver}"    # 限流 Key 解析器 Bean
                redis-rate-limiter.replenishRate: 100   # 令牌桶填充速率（个/秒）
                redis-rate-limiter.burstCapacity: 200   # 令牌桶容量（突发流量上限）

  # Redis 配置
  redis:
    host: 127.0.0.1
    port: 6379
```

```java
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        // 按用户维度限流（从 Header 中取 userId）
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders()
                .getFirst("X-User-Id") ?? "anonymous"
        );
    }

    @Bean
    public KeyResolver apiKeyResolver() {
        // 按请求路径维度限流
        return exchange -> Mono.just(
            exchange.getRequest().getURI().getPath()
        );
    }
}
```

### 4.6 与 Zuul 对比

| 对比维度 | Spring Cloud Gateway | Zuul 1.x / 2.x |
|---------|---------------------|----------------|
| 底层实现 | Spring WebFlux + Netty（异步非阻塞） | Servlet（同步阻塞） |
| 性能 | 高（异步 I/O，线程开销小） | 较低（同步 I/O，线程模型） |
| 长连接 | 天然支持 WebSocket | Zuul 1.x 不支持 |
| 限流 | 内置 Redis 令牌桶限流 | 需自行实现 |
| 响应式编程 | 基于 Reactor（Mono/Flux） | 普通 Servlet API |
| 社区活跃度 | Spring 官方维护，活跃 | Netflix 已进入维护模式 |

> **性能对比**：在同样硬件条件下，Gateway 的吞吐量是 Zuul 1.x 的 1.5-2 倍，且随着并发量增加差距进一步扩大。这是因为 Netty 的非阻塞 I/O 模型在处理大量长连接时需要更少的线程资源。

---

## 5. Seata — 分布式事务

在微服务架构中，每个服务拥有独立的数据库，传统数据库 ACID 事务无法跨服务生效。Seata 是阿里巴巴开源的分布式事务解决方案。

### 5.1 分布式事务场景

以一个典型的"下单扣库存"场景为例：

```
订单服务（order_db） → 远程调用 → 库存服务（stock_db）
                                          ↓
                             远程调用 → 账户服务（account_db）
```

传统事务无法跨 `order_db`、`stock_db`、`account_db` 三个数据库保证原子性。

### 5.2 Seata 核心架构

Seata 包含三个核心角色：

| 角色 | 说明 |
|------|------|
| **TC（Transaction Coordinator）** | 事务协调者，维护全局事务和分支事务的状态，驱动事务提交/回滚 |
| **TM（Transaction Manager）** | 事务管理器，定义全局事务的范围（`@GlobalTransactional`），向 TC 发起提交/回滚决议 |
| **RM（Resource Manager）** | 资源管理器，管理分支事务的资源，向 TC 注册分支并上报状态 |

**执行流程：**

```
1. TM 向 TC 开启全局事务 → TC 返回 XID（全局事务 ID）
2. TM 通过微服务调用链传递 XID
3. RM 向 TC 注册分支事务
4. TM 向 TC 发起全局提交/回滚
5. TC 驱动所有 RM 完成提交/回滚
```

### 5.3 四种事务模式

#### AT 模式（推荐，默认）

AT 模式是 Seata 最核心的事务模式，基于两阶段提交 + 反向 SQL 实现自动补偿。

**AT 模式执行流程：**

```
一阶段：业务数据和回滚日志在同一个本地事务中提交
  ┌─────────────────────────────────────────┐
  │  INSERT INTO order ...                  │
  │  INSERT INTO undo_log (                 │
  │    branch_id, xid, context,             │
  │    rollback_info, log_status,           │
  │    log_created, log_modified            │
  │  ) VALUES (...)                         │
  └─────────────────────────────────────────┘

二阶段（提交）：异步删除 undo_log 记录（快速）

二阶段（回滚）：根据 undo_log 生成反向 SQL 补偿
  ┌─────────────────────────────────────────┐
  │  根据 rollback_info 中的前后镜像         │
  │  生成反向 SQL：DELETE → INSERT          │
  │              UPDATE → UPDATE 原值        │
  └─────────────────────────────────────────┘
```

**AT 模式快速上手：**

1. **创建 undo_log 表**（每个业务数据库都需要）：

```sql
CREATE TABLE `undo_log` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `branch_id` BIGINT(20) NOT NULL,
    `xid` VARCHAR(100) NOT NULL,
    `context` VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB NOT NULL,
    `log_status` INT(11) NOT NULL,
    `log_created` DATETIME NOT NULL,
    `log_modified` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4;
```

2. **配置 Seata：**

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: my_test_tx_group
  service:
    vgroup-mapping:
      my_test_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      data-id: seata-server.properties
      group: SEATA_GROUP
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      application: seata-server
      group: SEATA_GROUP
```

3. **使用 @GlobalTransactional：**

```java
@Service
public class OrderService {

    @Autowired
    private StockServiceClient stockClient;

    @Autowired
    private AccountServiceClient accountClient;

    @Override
    @GlobalTransactional(name = "createOrder", rollbackFor = Exception.class)
    public Order createOrder(OrderRequest request) {
        // 1. 创建订单（本地事务）
        Order order = orderMapper.insert(request);

        // 2. 远程扣减库存（分支事务）
        stockClient.deduct(request.getProductId(), request.getQuantity());

        // 3. 远程扣减余额（分支事务）
        accountClient.debit(request.getUserId(), request.getAmount());

        return order;
    }
}
```

**AT 模式优缺点：**

| 优势 | 劣势 |
|------|------|
| 无业务侵入，注解即可 | 全局锁影响并发性能 |
| 支持自动回滚，无需手写补偿 | 回滚日志（undo_log）占用存储 |
| 默认支持 MySQL / PostgreSQL / Oracle / MariaDB | DDL 变更需要同步更新前后镜像逻辑 |
| 开发效率高 | 不适合大事务（回滚日志过大） |

#### TCC 模式

TCC（Try-Confirm-Cancel）模式需要手动编写三个阶段的方法。

```
Try：资源检查和预留（如冻结库存）
Confirm：执行业务（扣减冻结库存）
Cancel：回滚（释放冻结库存）
```

```java
@LocalTCC
public interface AccountTccService {

    @TwoPhaseBusinessAction(
        name = "accountTcc",
        commitMethod = "confirm",
        rollbackMethod = "cancel"
    )
    boolean tryDebit(BusinessActionContext context,
                     @BusinessActionContextParameter(paramName = "userId") Long userId,
                     @BusinessActionContextParameter(paramName = "amount") BigDecimal amount);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}
```

```java
@Service
public class AccountTccServiceImpl implements AccountTccService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountFreezeMapper freezeMapper;

    @Override
    public boolean tryDebit(BusinessActionContext context, Long userId, BigDecimal amount) {
        Account account = accountMapper.selectByUserId(userId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }

        // Try：冻结余额
        accountMapper.freezeBalance(userId, amount);

        // 记录冻结记录
        AccountFreeze freeze = new AccountFreeze();
        freeze.setXid(context.getXid());
        freeze.setUserId(userId);
        freeze.setFreezeAmount(amount);
        freeze.setState(FreezeState.TRY);
        freezeMapper.insert(freeze);

        return true;
    }

    @Override
    public boolean confirm(BusinessActionContext context) {
        // Confirm：扣除冻结余额
        Long userId = (Long) context.getActionContext("userId");
        BigDecimal amount = (BigDecimal) context.getActionContext("amount");

        int rows = accountMapper.confirmFreeze(userId, amount);
        freezeMapper.updateState(context.getXid(), FreezeState.CONFIRM);
        return rows > 0;
    }

    @Override
    public boolean cancel(BusinessActionContext context) {
        // Cancel：释放冻结余额
        Long userId = (Long) context.getActionContext("userId");
        BigDecimal amount = (BigDecimal) context.getActionContext("amount");

        int rows = accountMapper.unfreeze(userId, amount);
        freezeMapper.updateState(context.getXid(), FreezeState.CANCEL);
        return rows > 0;
    }
}
```

**TCC 模式优缺点：**

| 优势 | 劣势 |
|------|------|
| 性能好，无全局锁 | 需要手写 Try/Confirm/Cancel 三组代码 |
| 粒度灵活，资源占用少 | 业务侵入性强 |
| 适用于高并发场景 | 空回滚和幂等问题需要额外处理 |

> **空回滚**：Try 尚未执行时收到了 Cancel（如网络超时），Cancel 需要能正确处理。**幂等性**：Confirm/Cancel 可能被多次调用，必须保证幂等。

#### Saga 模式

Saga 模式通过将长事务拆分为一系列本地事务，每个本地事务都有对应的补偿事务。

```
正向流程：
  ServiceA.submit() → ServiceB.submit() → ServiceC.submit()

补偿流程（如果 ServiceC 失败）：
  ServiceC.compensate() → ServiceB.compensate() → ServiceA.compensate()
```

```java
// Saga 模式通常通过状态机或编排方式使用
@EnableAutoDataSourceProxy
@SpringBootApplication
public class SagaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SagaApplication.class, args);
    }
}
```

**Saga 模式优缺点：**

| 优势 | 劣势 |
|------|------|
| 适用于长事务、大事务 | 无隔离性（需要业务层保证） |
| 适用于老系统改造（无需修改数据库） | 补偿逻辑复杂 |
| 性能好，不持有数据库锁 | Saga 正向流程后数据已提交，补偿时需要反向操作 |

#### XA 模式

XA 模式基于 X/Open DTP 模型的 XA 规范，实现强一致性的两阶段提交。

```yaml
seata:
  data-source-proxy-mode: XA  # 使用 XA 模式
```

**XA 模式优缺点：**

| 优势 | 劣势 |
|------|------|
| 强一致性 | 性能差 |
| 数据库原生支持 | 数据库需要支持 XA 协议 |
| 代码入侵小 | 持锁时间长，并发能力低 |

### 5.4 模式选型建议

| 场景 | 推荐模式 | 原因 |
|------|---------|------|
| 新系统，要求快速开发 | AT | 自动补偿，零业务入侵 |
| 高并发场景 | TCC | 无全局锁，性能最高 |
| 老系统改造 | Saga | 无需修改数据库 schema |
| 金融转账（强一致性） | XA 或 TCC | XA 强一致，TCC 可控性好 |
| 跨语言服务 | Saga | 基于消息/API |

---

## 6. 其他重要组件

### 6.1 Feign / OpenFeign — 声明式 RPC 调用

OpenFeign 是 Spring Cloud 生态中的声明式 HTTP 客户端，通过接口和注解即可完成远程服务调用。

#### 快速集成

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableFeignClients  // 启用 Feign 客户端扫描
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

#### 声明 Feign 客户端

```java
@FeignClient(
    name = "user-service",
    path = "/api/user",
    configuration = UserFeignConfig.class,
    fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {

    @GetMapping("/{id}")
    Result<UserVO> getUserById(@PathVariable("id") Long id);

    @GetMapping("/list")
    Result<List<UserVO>> listUsers(@RequestParam("ids") List<Long> ids);

    @PostMapping("/batch")
    Result<List<UserVO>> batchQuery(@RequestBody UserBatchQuery query);
}
```

#### 自定义 Feign 配置

```java
@Configuration
public class UserFeignConfig {

    /**
     * 请求拦截器：传递 Token
     */
    @Bean
    public RequestInterceptor authRequestInterceptor() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String token = attrs.getRequest().getHeader("Authorization");
                if (token != null) {
                    template.header("Authorization", token);
                }
            }
            template.header("X-Source", "order-service");
        };
    }

    /**
     * 自定义日志级别
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  // NONE / BASIC / HEADERS / FULL
    }

    /**
     * 自定义解码器
     */
    @Bean
    public Decoder feignDecoder() {
        return new JacksonDecoder();
    }
}
```

#### 统一降级处理

```java
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public Result<UserVO> getUserById(Long id) {
                log.error("获取用户信息失败，userId: {}", id, cause);
                return Result.error("用户服务暂时不可用");
            }

            @Override
            public Result<List<UserVO>> listUsers(List<Long> ids) {
                return Result.error("用户服务暂时不可用");
            }

            @Override
            public Result<List<UserVO>> batchQuery(UserBatchQuery query) {
                return Result.error("用户服务暂时不可用");
            }
        };
    }
}
```

**Feign 默认集成 Spring Cloud LoadBalancer**，在调用时根据服务名自动从 Nacos 获取实例列表并进行负载均衡。

### 6.2 LoadBalancer — 负载均衡

Spring Cloud LoadBalancer 是 Spring Cloud 官方提供的客户端负载均衡器，替代了已进入维护模式的 Netflix Ribbon。

#### 配置使用

```java
@Bean
@LoadBalanced  // 让 RestTemplate 具备负载均衡能力
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

```java
// 使用时只需使用服务名
@Autowired
private RestTemplate restTemplate;

public String callUserService(Long userId) {
    String url = "http://user-service/api/user/" + userId;
    return restTemplate.getForObject(url, String.class);
}
```

#### 自定义负载均衡策略

```java
@Configuration
public class LoadBalancerConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment, LoadBalancerClientFactory loadBalancerClientFactory) {

        String serviceName = environment.getProperty(
            LoadBalancerClientFactory.PROPERTY_NAME);

        return new RandomLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(serviceName, ServiceInstanceListSupplier.class),
            serviceName
        );
    }
}
```

**内置策略：**

| 策略 | 类名 | 说明 |
|------|------|------|
| 轮询 | RoundRobinLoadBalancer | 默认，按顺序轮流分配 |
| 随机 | RandomLoadBalancer | 随机选择实例 |
| 加权 | WeightedLoadBalancer | 根据 Nacos 配置的权重分配 |

#### Ribbon 迁移到 LoadBalancer

| Ribbon | Spring Cloud LoadBalancer |
|--------|--------------------------|
| `@LoadBalanced` | 同样支持 |
| `IRule` | `ReactorLoadBalancer` |
| `IPing` | 无需单独配置，健康检查由 Nacos 处理 |
| 维护状态 | 进入维护模式，不再更新 |

### 6.3 Skywalking / Zipkin — 链路追踪

分布式系统中，一次请求可能经过多个服务，定位故障点需要全链路追踪能力。

#### Skywalking（推荐）

Apache Skywalking 是亚太地区最流行的 APM 系统之一，支持无侵入式的 Java Agent 接入。

**接入方式：**
```bash
# 在 JVM 启动参数中配置 Skywalking Agent
-javaagent:/path/to/skywalking-agent/skywalking-agent.jar
-Dskywalking.agent.service_name=order-service
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

无需修改任何代码，Skywalking 即可自动追踪：

- HTTP 请求调用链
- 数据库访问（JDBC）
- Redis / Memcached 操作
- MQ 消息生产与消费
- gRPC / Dubbo 调用

**核心概念：**

| 概念 | 说明 |
|------|------|
| **TraceId** | 全局唯一 ID，标识一次完整的请求链路 |
| **SpanId** | 每次远程调用产生一个 Span，标识调用层次 |
| **Span** | 一次调用单元，包含开始时间、结束时间、状态等 |
| **Segment** | 一个进程内的所有 Span 集合 |

**追踪示例：**

```
TraceId: d0c8a43f-8e3a-4f1a-b3a9-9f8a7b6c5d4e

SpanId 操作                   服务             时间
────────────────────────────────────────────────────
0.1    GET /api/order/1001   gateway          00ms
0.1.1  GET /order/1001      order-service    10ms
0.1.1.1 SELECT * FROM order  order-db        15ms
0.1.1.2 GET /user/42         user-service    20ms
0.1.1.3 SELECT * FROM user   user-db         25ms
```

**Skywalking 的告警规则示例（配置在 alarm-settings.yml）：**
```yaml
rules:
  service_resp_time_rule:
    metrics-name: service_resp_time
    op: ">"
    threshold: 2000
    period: 10
    count: 3
    message: 服务响应时间超过 2000ms
  service_sla_rule:
    metrics-name: service_sla
    op: "<"
    threshold: 0.8
    period: 10
    count: 3
    message: 服务可用率低于 80%
```

#### Zipkin + Sleuth（轻量级方案）

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

```yaml
spring:
  zipkin:
    base-url: http://127.0.0.1:9411
    sender:
      type: web    # 支持 web / kafka / rabbit / activemq
  sleuth:
    sampler:
      probability: 1.0   # 采样率，生产环境建议 0.1（10%）
```

**手动埋点（Sleuth）：**
```java
@Autowired
private Tracer tracer;

public void businessMethod() {
    Span newSpan = tracer.nextSpan().name("custom-span").start();
    try (Tracer.SpanInScope ws = tracer.withSpan(newSpan)) {
        // 业务逻辑
        newSpan.tag("custom-key", "custom-value");
    } finally {
        newSpan.finish();
    }
}
```

#### Skywalking vs Zipkin 对比

| 对比维度 | Skywalking | Zipkin + Sleuth |
|---------|------------|-----------------|
| 接入方式 | Java Agent 无侵入 | SDK 依赖 + 配置侵入 |
| 自动追踪能力 | 强（自动追踪数据库、MQ、Redis 等） | 中（需手动配置 Web 和部分中间件） |
| UI 功能 | 丰富（拓扑图、告警、JVM 监控） | 简洁（调用链查询） |
| 性能损耗 | 约 5-10% | 约 3-8% |
| 告警能力 | 内置告警规则引擎 | 需集成 Prometheus + Alertmanager |
| 集群部署 | 支持（Elasticsearch 存储） | 支持（Elasticsearch 存储） |
| 学习成本 | 中 | 低 |

---

## 7. 总结清单

下面是本文涵盖的微服务架构与 Spring Cloud Alibaba 实践知识清单，可用于自检和团队培训：

### 架构基础
- [ ] 理解单体架构与微服务架构的优劣对比
- [ ] 掌握服务拆分的单一职责、高内聚低耦合、DDD 限界上下文原则
- [ ] 理解 CAP 定理（C/A/P 含义，CP 与 AP 的权衡）
- [ ] 理解 BASE 理论（基本可用、软状态、最终一致性）
- [ ] 掌握服务治理六大核心问题的定义

### Nacos
- [ ] 掌握服务注册与发现的基本配置
- [ ] 理解临时实例与持久实例的区别及选择
- [ ] 掌握 Nacos Config 配置管理（dataId / group / namespace）
- [ ] 实现配置动态刷新（@RefreshScope / @NacosValue / 监听器）
- [ ] 掌握多环境隔离方案（Namespace 隔离 dev/test/prod）
- [ ] 理解 Nacos 的 AP / CP 模式切换
- [ ] 了解 Nacos 与 Eureka 的对比及迁移

### Sentinel
- [ ] 掌握 QPS 和线程数两种流量控制方式
- [ ] 理解流控模式（直接 / 关联 / 链路）
- [ ] 理解流控效果（快速失败 / Warm Up / 排队等待）
- [ ] 掌握三种熔断策略（慢调用比例 / 异常比例 / 异常数）
- [ ] 理解热点参数限流的应用场景
- [ ] 理解系统自适应保护（LOAD / RT / 线程数 / 入口 QPS）
- [ ] 掌握 @SentinelResource 注解的完整使用
- [ ] 掌握 Sentinel 控制台部署与规则持久化
- [ ] 了解 Sentinel 与 Hystrix 的对比

### Gateway
- [ ] 理解 Route / Predicate / Filter 三大核心概念
- [ ] 掌握路由配置（静态配置与动态路由）
- [ ] 掌握 Spring Cloud Gateway 内置断言工厂
- [ ] 掌握自定义 GlobalFilter（鉴权 / 日志）
- [ ] 掌握 RequestRateLimiter + Redis 限流
- [ ] 了解 Gateway 与 Zuul 的对比

### Seata
- [ ] 理解 Seata 的 TC / TM / RM 架构
- [ ] 掌握 AT 模式的两阶段提交与 undo_log 机制
- [ ] 掌握 TCC 模式的 Try / Confirm / Cancel 实现
- [ ] 理解 Saga 模式的正向与补偿流程
- [ ] 了解 XA 模式的强一致性特点
- [ ] 掌握四种模式的适用场景选型

### 其他组件
- [ ] 掌握 OpenFeign 声明式 RPC 调用
- [ ] 掌握 Feign 的降级处理与拦截器
- [ ] 理解 Spring Cloud LoadBalancer 与 Ribbon 的替代关系
- [ ] 理解 Skywalking 的无侵入链路追踪
- [ ] 了解 Zipkin + Sleuth 轻量级追踪方案
- [ ] 理解 TraceId / SpanId 在链路追踪中的含义

---

> **本文档配套资源**：建议结合 Spring Cloud Alibaba 官方文档（https://sca.aliyun.com/）和 GitHub 示例仓库（https://github.com/alibaba/spring-cloud-alibaba）进行实践学习。每个章节的代码示例均可直接用于 Spring Boot 2.7.x / 3.x + Spring Cloud Alibaba 2022.x / 2023.x 版本。
