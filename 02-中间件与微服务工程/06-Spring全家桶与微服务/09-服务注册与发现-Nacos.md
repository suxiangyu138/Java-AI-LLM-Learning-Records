# 服务注册与发现 Nacos
> 微服务架构的核心枢纽 —— 注册中心负责管理所有微服务的元数据与健康状态，是实现服务间动态调用的基础设施。

## 目录
1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-P2)

---

## 1. 本章总览

### 1.1 知识定位

注册中心是微服务架构的"通信录"与"健康监测中心"。在传统单体架构中，服务间调用通过硬编码 IP 地址完成；在微服务架构中，服务实例动态扩缩容、IP 不确定，必须通过注册中心实现服务地址的自动发现与健康管理。

Nacos (Dynamic Naming and Configuration Service) 是阿里巴巴开源的一站式服务注册与配置管理平台，支持 **服务注册与发现**、**配置管理**、**动态 DNS 服务** 三大核心能力。

### 1.2 前置知识

| 知识领域 | 要求 | 说明 |
|---------|------|------|
| Spring Boot 自动装配 | 掌握 | 理解 spring.factories 与 @Enable 机制 |
| 微服务基础理论 | 掌握 | 了解微服务拆分原则与架构演进 |
| HTTP/REST 基础 | 熟悉 | 服务间通信基本协议 |
| Linux 基本操作 | 了解 | Nacos 服务端部署与运维 |
| Spring Cloud 基础 | 了解 | 知道 Spring Cloud 生态定位 |

### 1.3 学习目标

| 层级 | 目标 | 对应能力 |
|------|------|---------|
| **L1-应用** | 能独立部署 Nacos 服务端，完成 SpringBoot 服务注册与发现开发 | 初级工程师 |
| **L2-原理** | 理解 AP/CP 切换原理、心跳机制、服务健康检查流程，能排查常见注册问题 | 中级工程师 |
| **L3-架构** | 掌握 Nacos 集群部署方案、保护阈值调优、多环境架构设计，能设计高可用注册中心方案 | 高级工程师 |

---

## 2. 分层理论讲解

### 2.1 Nacos 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                      Nacos Server                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                  Service Registry                      │   │
│  │  ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐            │   │
│  │  │Svc-A  │ │Svc-B  │ │Svc-C  │ │Svc-D  │            │   │
│  │  │i1,i2  │ │i1,i2  │ │i1     │ │i1,i3  │            │   │
│  │  └───────┘ └───────┘ └───────┘ └───────┘            │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  Health Check                           Distro Proto  │   │
│  │  (心跳检测 + TCP 健康检查 + HTTP 探测)    (AP 协议)     │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  Config Management                     Naming Service │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
          ▲                            │
          │ 注册 / 心跳 / 摘除           │ 查询 / 订阅
          │                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Microservice Client                       │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │Service-A │    │Service-B │    │Service-C │              │
│  │ 实例 1   │    │ 实例 1   │    │ 实例 1   │              │
│  │ 实例 2   │    │ 实例 2   │    │          │              │
│  └──────────┘    └──────────┘    └──────────┘              │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Nacos 服务端安装与部署

#### 2.2.1 环境准备

> 💡 Nacos 依赖 JDK 1.8+ 和 MySQL 5.7+（生产环境必须使用 MySQL 作为外部存储）。

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 1.8+ | 推荐 JDK 11+ |
| MySQL | 5.7+ | 生产环境必配 |
| Maven | 3.2+ | 源码编译使用 |
| 内存 | 1G+ | 单机模式最少 512M |

#### 2.2.2 单机模式部署

```bash
# 1. 下载 Nacos Server
wget https://github.com/alibaba/nacos/releases/download/2.3.0/nacos-server-2.3.0.tar.gz

# 2. 解压
tar -zxvf nacos-server-2.3.0.tar.gz
cd nacos/bin

# 3. 修改 application.properties（配置 MySQL）
# vi nacos/conf/application.properties
```

```properties
# application.properties 关键配置
spring.datasource.platform=mysql
db.url.0=jdbc:mysql://localhost:3306/nacos_config?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai
db.user.0=nacos
db.password.0=nacos
db.pool.config.initialSize=20
db.pool.config.maxTotal=100
```

```bash
# 4. 初始化数据库（使用 nacos/conf/mysql-schema.sql）

# 5. 启动 Nacos（单机模式）
startup.cmd -m standalone    # Windows
sh startup.sh -m standalone  # Linux/Mac
```

启动后访问 http://localhost:8848/nacos ，默认登录账号密码：`nacos/nacos`。

#### 2.2.3 集群模式部署

```bash
# 集群节点规划（3 节点）
node1: 192.168.1.10:8848
node2: 192.168.1.11:8848
node3: 192.168.1.12:8848
```

```properties
# 每个节点的 application.properties
# node1
server.port=8848

# node2
server.port=8849

# node3
server.port=8850

# 所有节点配置相同的外部数据库
spring.datasource.platform=mysql
db.url.0=jdbc:mysql://192.168.1.100:3306/nacos_config?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai
db.user.0=nacos
db.password.0=nacos
```

```bash
# 配置 cluster.conf（每个节点相同）
vi nacos/conf/cluster.conf
```

```
# cluster.conf
192.168.1.10:8848
192.168.1.11:8848
192.168.1.12:8848
```

```bash
# 依次启动各节点
sh startup.sh
```

> ⚠️ 集群模式必须配置 MySQL 数据源，Nacos 默认的内嵌 Derby 数据库不支持多节点数据一致。

### 2.3 服务注册 —— 客户端集成

#### 2.3.1 添加 Maven 依赖

```xml
<!-- Spring Cloud Alibaba Nacos Discovery -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    <version>2022.0.0.0</version>
</dependency>

<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Cloud LoadBalancer（服务发现负载均衡） -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

#### 2.3.2 application.yml 配置

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service           # 服务名 —— 注册到 Nacos 的服务标识

  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848    # Nacos 服务端地址
        namespace: public               # 命名空间，默认 public
        group: DEFAULT_GROUP            # 分组，默认 DEFAULT_GROUP
        service: ${spring.application.name}  # 注册的服务名（默认取 spring.application.name）
        weight: 1                       # 权重，取值范围 1~100，用于负载均衡
        ip:                            # 指定注册 IP（可选，多网卡场景）
        port:                          # 指定注册端口（可选）
        metadata:                       # 自定义元数据
          version: v1
          region: cn-beijing
        ephemeral: true                # 是否为临时实例，默认 true
        cluster-name: DEFAULT          # 集群名称
        naming-load-cache-at-start: false  # 启动时是否拉取服务列表
```

#### 2.3.3 启动类注解

```java
package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // 启用服务注册与发现（Spring Cloud 通用注解）
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

> 💡 `@EnableDiscoveryClient` 是 Spring Cloud 的通用注解，底层通过 `spring.factories` 中的 `DiscoveryClient` 实现自动适配。使用 Nacos 时，会自动加载 `NacosDiscoveryClient` 实现。从 Spring Cloud Edgware 开始，`@EnableDiscoveryClient` 可以省略（只要 classpath 中存在 discovery 实现类就自动启用），但建议显式标注以增强可读性。

#### 2.3.4 注册流程时序

```
Client                           Nacos Server
  │                                  │
  │  1. 应用启动                      │
  │     └─ Bean 初始化                │
  │                                  │
  │  2. POST /nacos/v1/ns/instance   │
  │     ─────────────────────────→    │
  │     {                             │
  │       "ip":"192.168.1.2",        │
  │       "port":8081,               │
  │       "serviceName":"user-service",│
  │       "weight":1.0,              │
  │       "ephemeral":true,          │
  │       "metadata":{...}           │
  │     }                            │
  │                                  │
  │  3. 返回注册成功                   │
  │     ←─────────────────────────    │
  │                                  │
  │  4. 定时心跳（5s 一次）            │
  │     PUT /nacos/v1/ns/heartbeat   │
  │     ─────────────────────────→    │
  │     {                             │
  │       "serviceName":"user-service",│
  │       "ip":"192.168.1.2",        │
  │       "port":8081                 │
  │     }                             │
  │                                  │
  │  5. 心跳超时（15s）→ 标记不健康     │
  │  6. 心跳停止（30s）→ 实例摘除      │
  │                                  │
```

### 2.4 服务发现 —— 消费端调用

```java
package com.example.orderservice.controller;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class OrderController {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    public OrderController(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
    }

    /**
     * 基于 DiscoveryClient 手动获取服务列表
     */
    @GetMapping("/discover/{serviceName}")
    public List<ServiceInstance> discover(@PathVariable String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        for (ServiceInstance instance : instances) {
            System.out.println("Service: " + serviceName
                    + ", Host: " + instance.getHost()
                    + ", Port: " + instance.getPort()
                    + ", URI: " + instance.getUri()
                    + ", Metadata: " + instance.getMetadata());
        }
        return instances;
    }

    /**
     * 基于 RestTemplate 实现服务调用（需配合 @LoadBalanced）
     */
    @GetMapping("/order/call/{userId}")
    public String callUserService(@PathVariable Long userId) {
        // 使用 服务名 代替 IP:Port，LoadBalancer 自动负载均衡
        String url = "http://user-service/user/" + userId;
        return restTemplate.getForObject(url, String.class);
    }
}
```

```java
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced  // 为 RestTemplate 注入 LoadBalancer 能力
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

#### 服务发现的核心 API

| API | 作用 | 使用场景 |
|-----|------|---------|
| `DiscoveryClient.getInstances(String serviceId)` | 获取指定服务的所有实例 | 手动路由选择 |
| `DiscoveryClient.getServices()` | 获取所有已注册的服务名列表 | 服务目录展示 |
| `LoadBalancerClient.choose(String serviceId)` | 按负载均衡策略选择一个实例 | 增强版手动调用 |
| `@LoadBalanced RestTemplate` | 自动拦截请求并解析服务名 | 最常用的方式 |

### 2.5 心跳机制详解

#### 2.5.1 心跳参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `nacos.naming.heart-beat.interval` | 5000ms | 心跳发送间隔 |
| `nacos.naming.heart-beat.timeout` | 15000ms | 心跳超时时间（超过该时间未收到心跳则标记为不健康） |
| `nacos.naming.heart-beat.retry` | 3 | 心跳重试次数 |
| `nacos.naming.cleaner.trigger-ms` | 30000ms | 清理过期实例的间隔 |
| `ipDeleteTimeout` | 30000ms | 实例被摘除的超时时间 |

#### 2.5.2 心跳生命周期

```
正常状态 ──→ 心跳超时(15s) ──→ 不健康 ──→ 心跳恢复 ──→ 正常
                                  │
                                  ├── 持续不健康(30s) ──→ 实例被摘除
                                  │
                                  └── 手动取消注册 ──→ 实例被摘除
```

#### 2.5.3 客户端心跳源码逻辑

Nacos 客户端通过 `BeatReactor` 类维护心跳任务，核心逻辑：

```java
// 伪代码：Nacos 客户端心跳实现
public class BeatReactor {
    private final ScheduledExecutorService executorService;
    private final Map<String, BeatInfo> dom2Beat = new ConcurrentHashMap<>();

    public void addBeatInfo(String serviceName, BeatInfo beatInfo) {
        dom2Beat.put(serviceName, beatInfo);
        // 每 5s 发送一次心跳
        executorService.schedule(() -> {
            while (true) {
                sendBeat(serviceName, beatInfo);  // 调用 PUT /ns/heartbeat
                Thread.sleep(beatInfo.getPeriod());  // 默认 5000ms
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    private void sendBeat(String serviceName, BeatInfo beatInfo) {
        // 构造心跳请求
        JsonNode result = serverProxy.sendBeat(beatInfo);
        // 如果服务端返回客户端需要重新注册，则重新注册
        if (result.has("lightBeatEnabled") && !result.get("lightBeatEnabled").booleanValue()) {
            reRegisterService(serviceName, beatInfo);
        }
    }
}
```

### 2.6 服务离线与摘除

#### 2.6.1 正常下线（优雅关闭）

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
public class GracefulShutdownConfig {

    @PreDestroy
    public void onShutdown() {
        System.out.println("服务优雅关闭，Nacos 自动摘除实例...");
        // Nacos 客户端在 Spring 容器关闭时自动调用 deregister
        // 默认通过 NacosAutoServiceRegistration 的 destroy() 方法触发
    }
}
```

Spring 容器关闭时，`NacosAutoServiceRegistration` 自动调用 `NacosServiceRegistry.deregister()` 发送取消注册请求。通过钩子确保：

1. 先标记服务为下线状态
2. 停止接收新请求（配合 LoadBalancer 健康检查）
3. 通知 Nacos 删除实例
4. 等待一段时间让已处理的请求完成

```yaml
# 优雅关闭配置
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 最多等待 30s 完成处理
```

#### 2.6.2 异常离线（心跳停止）

当服务进程突然崩溃或网络分区发生时：

1. Nacos Server 在 15s 内未收到心跳 → 将实例标记为 `UNHEALTHY`
2. 再过 15s（累计 30s）仍无心跳 → 将实例从服务列表中摘除
3. 通知已订阅该服务的消费者更新服务列表

#### 2.6.3 临时实例 vs 持久化实例

| 特性 | 临时实例 (ephemeral=true) | 持久化实例 (ephemeral=false) |
|------|--------------------------|-----------------------------|
| 生命周期 | 自动管理，随服务进程生死 | 手动管理，注册后持久存在 |
| 心跳检测 | 依赖心跳保活 | 依赖服务端主动健康检查（TCP/HTTP） |
| 自动摘除 | 心跳超时后自动摘除 | 不会自动摘除，需手动调用取消注册 |
| 数据存储 | 内存存储，不落盘 | 写入数据库持久化 |
| 典型场景 | 普通微服务实例 | 网关、数据库代理等长期稳定节点 |

> 💡 绝大多数微服务场景使用临时实例 (`ephemeral: true`)，心跳机制确保了实例列表的实时性。

### 2.7 AP 与 CP 模式切换

#### 2.7.1 CAP 理论回顾

| 特性 | 说明 |
|------|------|
| **C**onsistency（一致性） | 所有节点在同一时刻看到相同的数据 |
| **A**vailability（可用性） | 每个请求都能获得非错误的响应 |
| **P**artition Tolerance（分区容错性） | 网络分区时系统仍能正常运行 |

Nacos 支持 AP 和 CP 两种模式，默认 AP 模式。

#### 2.7.2 模式切换原理

Nacos 根据 `ephemeral` 参数自动切换模式：

- **临时实例（默认） → AP 模式**
  - 使用 **Distro 协议**（阿里巴巴自研的 AP 协议）
  - 优先保证可用性，允许短暂的数据不一致
  - 节点间通过异步复制同步数据
  - 适用场景：服务注册发现（允许短暂读到过期服务列表）

- **持久化实例 → CP 模式**
  - 使用 **Raft 协议**
  - 优先保证一致性
  - 写操作需要多数节点确认后才返回成功
  - 适用场景：配置管理、分布式锁

```java
// 通过 Nacos 开放 API 临时切换某个服务的模式
// AP 模式（使用临时实例）
POST /nacos/v1/ns/instance
{
    "serviceName": "user-service",
    "ip": "192.168.1.2",
    "port": 8081,
    "ephemeral": true  // AP
}

// CP 模式（使用持久化实例）
POST /nacos/v1/ns/instance
{
    "serviceName": "db-proxy",
    "ip": "192.168.1.100",
    "port": 3306,
    "ephemeral": false  // CP
}
```

> ⚠️ 不建议修改 Nacos 服务端默认的 AP/CP 模式，Nacos 会自动根据实例类型选择。如需强制修改，可在 `application.properties` 中设置 `nacos.naming.distro.protocol.enable=false`，但此操作不推荐生产使用。

#### 2.7.3 AP/CP 对比

| 对比维度 | AP 模式 (Distro) | CP 模式 (Raft) |
|---------|-----------------|----------------|
| 一致性 | 最终一致性 | 强一致性 |
| 可用性 | 极高 (99.99%) | 较高 |
| 写入性能 | 高（异步复制） | 中（需多数节点确认） |
| 分区容忍性 | 强（各节点独立服务） | 弱（多数节点存活才可用） |
| 适用场景 | 服务注册与发现 | 配置管理、分布式协调 |
| 数据丢失风险 | 节点宕机丢失临时实例数据 | 数据持久化，不丢失 |

### 2.8 Nacos vs Eureka vs Consul

| 对比维度 | Nacos | Eureka 2.0 | Consul |
|---------|-------|------------|--------|
| **维护方** | 阿里巴巴 | Netflix（已停止维护） | HashiCorp |
| **AP/CP** | 支持 AP 和 CP | 仅 AP | 仅 CP |
| **一致性协议** | Distro / Raft | 无（纯 AP） | Raft |
| **健康检查** | 心跳 + TCP/HTTP 探测 | 心跳 | TCP/HTTP/gRPC + 自定义脚本 |
| **配置管理** | 内置（Nacos Config） | 需集成 Spring Cloud Config | 自带 KV Store |
| **控制台 UI** | 功能完善，支持可视化管理 | 基础 | 功能完善 |
| **K8s 集成** | 通过 DNS 或 SDK | 一般 | 原生支持（Service Mesh） |
| **性能** | 高（10w+ TPS） | 中 | 高 |
| **多数据中心** | 原生支持 | 需配置 | 原生支持 |
| **权重路由** | 支持 | 支持 | 支持 |
| **协议** | HTTP/gRPC | HTTP | HTTP/DNS |
| **学习成本** | 低 | 低 | 中 |
| **Spring Cloud 集成** | 原生 Alibaba 生态 | Netflix 生态 | Spring Cloud Consul |
| **社区活跃度** | 非常活跃 | 已停止维护 | 活跃 |
| **生产案例** | 阿里、滴滴、饿了么 | 传统 Spring Cloud 项目 | 国外大型企业 |

> 🎯 **选型建议**：新项目首选 Nacos（服务注册 + 配置管理二合一，社区活跃，国内生态完善）；K8s 原生项目考虑 Consul；Eureka 已停止维护，不建议新项目使用。

### 2.9 保护阈值

#### 2.9.1 概念

保护阈值（Protect Threshold）是 Nacos 用于防止服务雪崩的机制。当某个服务的健康实例比例低于设定阈值时，Nacos 会将健康实例和**不健康实例**一起返回给调用方，防止流量全部打到少量健康实例导致其过载崩溃。

#### 2.9.2 配置方式

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        # 保护阈值：0~1 之间的浮点数
        # 当健康实例比例 < 阈值时，触发保护模式
        protect-threshold: 0.3
```

控制台操作路径：「服务列表」→ 点击目标服务 →「保护阈值」→ 输入 0~1 的值。

#### 2.9.3 保护模式示例

```
场景：user-service 有 10 个实例
      健康的实例数：2
      不健康的实例数：8
      健康比例：20%
      保护阈值：0.3

20% < 30% → 触发保护模式
└── 调用方 getInstances() 返回全部 10 个实例（含不健康的）
    └── 调用方可能调用到不健康实例
        └── 优点：健康实例不会被打垮
        └── 缺点：部分请求会失败（需要调用方做容错）
```

> 💡 保护阈值适用于流量突增且大量实例不可用的场景，提供"尽力而为"的服务能力。建议设置为 `0.3 ~ 0.5`，具体数值需根据业务容错能力调整。

### 2.10 Nacos 集群部署详解

#### 2.10.1 生产环境架构推荐

```
                    ┌─────────────────────────┐
                    │      SLB (负载均衡)       │
                    │   nginx / SLB / DNS      │
                    └────────┬───────┬────────┘
                             │       │
              ┌──────────────┼───────┼──────────────┐
              │              │       │              │
              ▼              ▼       ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ Nacos-1  │  │ Nacos-2  │  │ Nacos-3  │
        │ :8848    │  │ :8848    │  │ :8848    │
        └────┬─────┘  └────┬─────┘  └────┬─────┘
             │             │             │
             └─────────────┼─────────────┘
                           │
                    ┌──────┴──────┐
                    │   MySQL     │
                    │  (主从/集群) │
                    └─────────────┘
```

#### 2.10.2 Nginx 反向代理配置

```nginx
upstream nacos_cluster {
    server 192.168.1.10:8848;
    server 192.168.1.11:8848;
    server 192.168.1.12:8848;
}

server {
    listen 8848;
    server_name nacos.example.com;

    location / {
        proxy_pass http://nacos_cluster;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket 支持（Nacos 2.x 需要）
    location /nacos/ {
        proxy_pass http://nacos_cluster/nacos/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

#### 2.10.3 客户端连接集群

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 方式一：直接列出所有节点
        server-addr: 192.168.1.10:8848,192.168.1.11:8848,192.168.1.12:8848

        # 方式二：通过 SLB 统一入口
        # server-addr: nacos.example.com:8848
```

> ⚠️ 集群模式下务必确保所有节点使用同一个 MySQL 数据库，否则各节点的服务列表不一致。Nacos 2.x 使用 gRPC 端口（默认 9848、9849），需确保防火墙放行。

---

## 3. 高频踩坑与误区

### 3.1 服务注册成功但调用失败

**现象**：Nacos 控制台显示服务已注册，但服务间调用报 `java.net.UnknownHostException`。

**根源**：调用方未添加 `@LoadBalanced` 注解，`RestTemplate` 无法解析服务名。

```java
// ❌ 错误：未加 @LoadBalanced
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}

// ✅ 正确：添加 @LoadBalanced
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

### 3.2 多网卡导致注册 IP 错误

**现象**：服务注册后在 Nacos 控制台看到 IP 为 `127.0.0.1` 或 `0.0.0.0`，导致其他服务无法调用。

**根源**：服务器有多块网卡（Docker / VPN / 虚拟网卡），Nacos 客户端获取了错误的 IP 地址。

```yaml
# ✅ 方案一：指定注册 IP
spring:
  cloud:
    nacos:
      discovery:
        ip: 192.168.1.100   # 显式指定本机对外 IP

# ✅ 方案二：指定网卡名称
# application.properties
nacos.naming.network.interface.eth0=true

# ✅ 方案三：在 bootstrap.yml 中配置
spring:
  cloud:
    inetutils:
      preferred-networks:
        - 192.168.1.0/24    # 只使用该网段的 IP
```

### 3.3 Nacos 2.x 端口不通

**现象**：Nacos 1.x 正常升级到 2.x 后，服务注册成功但心跳失败。

**根源**：Nacos 2.x 默认使用 gRPC 通信，增加以下端口：

| 端口 | 协议 | 用途 |
|------|------|------|
| 8848 | HTTP | 主端口（兼容 1.x API） |
| 9848 | gRPC | 客户端 gRPC 端口（8848 + 1000） |
| 9849 | gRPC | 集群间 gRPC 通信端口 |
| 7848 | HTTP | Jraft 通信端口 |

```yaml
# ✅ 解决：确保防火墙放行 gRPC 端口
# 每个端口占用 +1000 偏移
# 如果 server.port=8848 则 gRPC 端口为 9848
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.1.10:8848  # 客户端只需配置主端口
        # 连接 √  gRPC 端口自动推导为 9848
```

### 3.4 服务名大小写问题

**现象**：使用 `@FeignClient(name="user-service")` 调用时，控制台报 `No instances available`。

**根源**：Nacos 默认对服务名做了大小写转换处理，实际存储的服务名可能全小写。

> 💡 Nacos 1.x 对服务名做了大写处理，Nacos 2.x 默认不做转换。建议消费者的 `@FeignClient` 名称与提供者 `spring.application.name` 完全一致，统一使用小写字母和连字符。

### 3.5 健康实例数显示为 0

**现象**：Nacos 控制台显示服务已注册，但健康实例数一直是 0。

**根源**：服务的健康检查端口和业务端口不一致，或者健康检查路径不存在。

```yaml
# ✅ 正确配置健康检查
spring:
  cloud:
    nacos:
      discovery:
        # 确保 actuator 端点可用
        health-check-url: http://${spring.cloud.nacos.discovery.ip}:${server.port}/actuator/health
```

```xml
<!-- actuator 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 3.6 同一个服务注册了多个不同实例

**现象**：服务启动两次，Nacos 显示多个实例，其中一个实例的 IP 为 `0.0.0.0`。

**根源**：服务启动时未指定 `server.port`，两个实例端口冲突，第二个实例虽然启动但注册信息错误。

```yaml
# ✅ 开发环境使用随机端口避免冲突
server:
  port: 0  # 随机端口

spring:
  application:
    name: user-service
```

> ⚠️ 生产环境不要使用随机端口，应通过配置中心统一管理端口分配，或在启动参数中指定 `--server.port=8081`。

---

## 4. 随堂基础练习

### 练习 1：单机部署 Nacos

部署 Nacos 单机模式，使用 MySQL 作为存储后端。验证以下操作：

1. 访问 Nacos 控制台 http://localhost:8848/nacos
2. 登录并查看「服务列表」页面
3. 创建一个命名空间 `dev`
4. 查看「集群管理」下的节点列表

### 练习 2：服务注册

创建一个 `product-service` Spring Boot 项目，实现：

1. 添加 Nacos Discovery 依赖
2. 配置 application.yml 将服务注册到 Nacos
3. 在控制台查看注册结果
4. 尝试修改 `weight` 和 `metadata` 并观察变化

### 练习 3：服务发现与调用

创建 `order-service` 调用练习 2 的 `product-service`：

1. 使用 `@LoadBalanced RestTemplate` 调用 `product-service` 的 `/product/{id}` 接口
2. 使用 `DiscoveryClient` 获取所有实例信息
3. 验证负载均衡效果（启动多个 `product-service` 实例，观察请求分配）

### 练习 4：心跳观察

通过以下方式观察心跳行为：

1. 启动服务，观察 Nacos 控制台健康状态
2. kill 掉服务进程，观察 Nacos 控制台的状态变化时间（15s + 30s）
3. 重新启动服务，观察状态恢复
4. 配置 `server.shutdown=graceful`，发送 SIGTERM 观察优雅下线行为

---

## 5. 章节综合实操案例

### 5.1 案例背景

实现一个 **订单-用户-商品** 三服务微服务系统，所有服务注册到 Nacos，实现服务间的动态调用。

### 5.2 项目结构

```
nacos-demo/
├── pom.xml                      # 父模块（多模块聚合）
├── user-service/                # 用户服务（端口 8081）
│   ├── pom.xml
│   └── src/main/java/com/example/user/
│       ├── UserServiceApplication.java
│       ├── controller/UserController.java
│       ├── entity/User.java
│       └── service/UserService.java
├── product-service/             # 商品服务（端口 8082）
│   ├── pom.xml
│   └── src/main/java/com/example/product/
│       ├── ProductServiceApplication.java
│       ├── controller/ProductController.java
│       ├── entity/Product.java
│       └── service/ProductService.java
└── order-service/               # 订单服务（端口 8083）
    ├── pom.xml
    └── src/main/java/com/example/order/
        ├── OrderServiceApplication.java
        ├── controller/OrderController.java
        ├── entity/Order.java
        ├── service/OrderService.java
        └── config/RestTemplateConfig.java
```

### 5.3 父模块 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>nacos-demo</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>user-service</module>
        <module>product-service</module>
        <module>order-service</module>
    </modules>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.0</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2022.0.3</spring-cloud.version>
        <spring-cloud-alibaba.version>2022.0.0.0</spring-cloud-alibaba.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 5.4 user-service 实现

**application.yml**

```yaml
server:
  port: 8081
  shutdown: graceful

spring:
  application:
    name: user-service
  lifecycle:
    timeout-per-shutdown-phase: 30s

  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: public
        weight: 1
        metadata:
          version: v1
          author: nacos-demo

  datasource:
    url: jdbc:h2:mem:userdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
```

**UserServiceApplication.java**

```java
package com.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

**User.java**

```java
package com.example.user.entity;

public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;

    public User() {}

    public User(Long id, String name, String email, Integer age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
```

**UserController.java**

```java
package com.example.user.controller;

import com.example.user.entity.User;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/user")
public class UserController {

    private final Map<Long, User> userMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        userMap.put(1L, new User(1L, "张三", "zhangsan@example.com", 25));
        userMap.put(2L, new User(2L, "李四", "lisi@example.com", 30));
        userMap.put(3L, new User(3L, "王五", "wangwu@example.com", 28));
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        User user = userMap.get(id);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + id);
        }
        return user;
    }

    @GetMapping("/list")
    public List<User> listUsers() {
        return new ArrayList<>(userMap.values());
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        user.setId(System.currentTimeMillis());
        userMap.put(user.getId(), user);
        return user;
    }
}
```

### 5.5 product-service 实现

**application.yml**

```yaml
server:
  port: 8082

spring:
  application:
    name: product-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

**ProductServiceApplication.java**

```java
package com.example.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

**ProductController.java**

```java
package com.example.product.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Value("${server.port}")
    private String serverPort;

    private final Map<Long, Map<String, Object>> productMap = new HashMap<>();

    public ProductController() {
        productMap.put(1L, new HashMap<>() {{
            put("id", 1L);
            put("name", "Java编程思想");
            put("price", 79.00);
            put("stock", 100);
        }});
        productMap.put(2L, new HashMap<>() {{
            put("id", 2L);
            put("name", "Spring实战");
            put("price", 69.00);
            put("stock", 50);
        }});
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProduct(@PathVariable Long id) {
        Map<String, Object> product = productMap.get(id);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + id);
        }
        product.put("serverPort", serverPort);  // 用于验证负载均衡
        return product;
    }

    @GetMapping("/list")
    public List<Map<String, Object>> listProducts() {
        return new ArrayList<>(productMap.values());
    }
}
```

### 5.6 order-service 实现

**application.yml**

```yaml
server:
  port: 8083

spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

**OrderServiceApplication.java**

```java
package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

**RestTemplateConfig.java**

```java
package com.example.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**OrderService.java**

```java
package com.example.order.service;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    public OrderService(RestTemplate restTemplate, DiscoveryClient discoveryClient) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
    }

    /**
     * 查询用户 + 商品信息来创建订单
     */
    public Map<String, Object> createOrder(Long userId, Long productId) {
        // 1. 调用 user-service 获取用户信息
        String userUrl = "http://user-service/user/" + userId;
        Map<String, Object> user = restTemplate.getForObject(userUrl, Map.class);

        // 2. 调用 product-service 获取商品信息
        String productUrl = "http://product-service/product/" + productId;
        Map<String, Object> product = restTemplate.getForObject(productUrl, Map.class);

        // 3. 模拟生成订单
        Map<String, Object> order = new java.util.HashMap<>();
        order.put("orderId", System.currentTimeMillis());
        order.put("userId", userId);
        order.put("productId", productId);
        order.put("userName", user.get("name"));
        order.put("productName", product.get("name"));
        order.put("productPrice", product.get("price"));
        order.put("status", "CREATED");
        order.put("serverPort", product.get("serverPort"));  // 验证负载均衡

        return order;
    }

    /**
     * 获取 user-service 的所有实例信息
     */
    public List<ServiceInstance> getUserServiceInstances() {
        return discoveryClient.getInstances("user-service");
    }
}
```

**OrderController.java**

```java
package com.example.order.controller;

import com.example.order.service.OrderService;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestParam Long userId,
                                            @RequestParam Long productId) {
        return orderService.createOrder(userId, productId);
    }

    @GetMapping("/instances")
    public List<ServiceInstance> getInstances() {
        return orderService.getUserServiceInstances();
    }
}
```

### 5.7 验证步骤

```bash
# 1. 启动 Nacos
cd nacos/bin
sh startup.cmd -m standalone

# 2. 分别启动三个服务（开启三个终端）
# 终端 1
java -jar user-service/target/user-service-1.0.0.jar

# 终端 2
java -jar product-service/target/product-service-1.0.0.jar

# 终端 3
java -jar order-service/target/order-service-1.0.0.jar

# 3. 验证发现
curl http://localhost:8083/order/instances

# 4. 创建订单
curl -X POST "http://localhost:8083/order/create?userId=1&productId=1"

# 5. 启动第二个 product-service 实例验证负载均衡
java -jar product-service/target/product-service-1.0.0.jar --server.port=8084
# 多次调用 createOrder 观察 serverPort 字段的变化
```

---

## 6. 分层综合习题

### 6.1 基础题

1. **Nacos 默认的 CAP 模式是什么？** 什么配置会触发模式切换？

2. **写出 Spring Boot 集成 Nacos 服务注册的最小配置（application.yml）。**

3. **Nacos 服务端默认端口是多少？2.x 版本新增了哪些端口？**

4. **什么是保护阈值？保护阈值设置为 0.5 的含义是什么？**

### 6.2 进阶题

1. **Nacos 中临时实例 (ephemeral) 和持久化实例的区别是什么？各自的应用场景？**

2. **心跳超时的三个阶段是如何定义的？请描述从心跳超时到实例摘除的完整过程和时间线。**

3. **设计一个方案：多网卡服务器上如何确保 Nacos 注册的 IP 地址是正确的？列举至少三种方案。**

4. **Nacos 2.x 新增了 gRPC 通信，如果客户端与服务端版本不一致会导致什么后果？**

### 6.3 精通题

1. **Nacos 集群模式下，如果某个节点宕机，客户端请求是否会全部失败？请结合 Distro 协议解释。**

2. **推导 Raft 协议在 Nacos CP 模式下的选主过程。当 Leader 节点宕机后，多长时间内集群不可用？**

3. **设计一个多机房高可用 Nacos 部署方案，要求：**
   - 3 个机房，每个机房 2 个 Nacos 节点
   - 任意一个机房整体宕机不影响服务注册发现
   - 跨机房流量尽可能本地优先
   - 写出完整的架构拓扑图和配置要点

4. **Nacos 保护阈值的工作原理与 Sentinel 限流降级有何本质区别？在什么场景下两者需要配合使用？**

---

## 7. 本章复盘速记清单

### 7.1 核心注解

| 注解 | 作用 | 使用位置 |
|------|------|---------|
| `@EnableDiscoveryClient` | 启用服务注册与发现 | Spring Boot 启动类 |
| `@LoadBalanced` | 为 RestTemplate 注入负载均衡能力 | RestTemplate Bean 定义 |

### 7.2 核心配置项

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `spring.cloud.nacos.discovery.server-addr` | Nacos 服务端地址 | `127.0.0.1:8848` |
| `spring.cloud.nacos.discovery.namespace` | 命名空间 | `public`, `dev`, `prod` |
| `spring.cloud.nacos.discovery.group` | 分组 | `DEFAULT_GROUP` |
| `spring.cloud.nacos.discovery.weight` | 权重 | `1` ~ `100` |
| `spring.cloud.nacos.discovery.ephemeral` | 是否临时实例 | `true` |
| `spring.cloud.nacos.discovery.metadata` | 实例元数据 | `{version: v1}` |
| `spring.cloud.nacos.discovery.cluster-name` | 集群名称 | `DEFAULT` |
| `spring.cloud.nacos.discovery.protect-threshold` | 保护阈值 | `0.0` ~ `1.0` |

### 7.3 心跳时间线

| 阶段 | 时间 | 状态 |
|------|------|------|
| 正常心跳 | 每 5s | HEALTHY |
| 心跳超时 | 超过 15s | UNHEALTHY（标记不健康） |
| 实例摘除 | 超过 30s | REMOVED（从列表删除） |
| 实例恢复 | 重新发送心跳 | HEALTHY |

### 7.4 Nacos 架构对比速查

| 维度 | AP 模式 | CP 模式 |
|------|---------|---------|
| 协议 | Distro | Raft |
| 一致性 | 最终一致性 | 强一致性 |
| 数据存储 | 内存 | 数据库 |
| 实例类型 | 临时实例 | 持久化实例 |

---

## 8. 精通拓展补充-P2

### 8.1 Nacos Distro 协议源码分析

Distro 协议是 Nacos 自研的 AP 协议，核心设计理念：**最终一致性 + 可用性优先**。

```
Distro 协议核心流程：

1. 写请求处理
   Client → 任意 Nacos 节点 ──→ 本地写入 ──→ 异步同步到其他节点
                                         ↓
                                   放入 SyncTask 队列

2. 节点间数据同步
   SyncTask ──→ 按 partition 分组 ──→ 批量发送到目标节点
                                         ↓
                                   目标节点 → 校验 → 合并

3. 定时一致性校验
   每个节点定时（默认 5s）向其他节点发送校验和
   不一致的数据触发增量同步
```

```java
// DistroConsistencyServiceImpl 核心源码分析
public class DistroConsistencyServiceImpl implements ConsistencyService {

    // 异步同步任务分发器
    private final DistroTaskEngine distroTaskEngine;

    // 用于数据校验的定时任务
    private final ScheduledExecutorService executorService;

    @Override
    public void put(String key, Record value) throws NacosException {
        // 1. 写入本地存储（内存 + 磁盘）
        onPut(key, value);

        // 2. 异步分发到其他节点
        distroTaskEngine.addSyncTask(key, value);
    }

    @Override
    public void onPut(String key, Record value) {
        // 写入本地 DataStore（ConcurrentHashMap）
        dataStore.put(key, value);

        // 如果是持久化实例，同时写入数据库
        if (value instanceof Instances instances && !instances.isEphemeral()) {
            persistToDatabase(key, instances);
        }
    }
}
```

### 8.2 Nacos 服务注册客户端源码

```java
// NacosNamingService 核心流程
public class NacosNamingService implements NamingService {

    @Override
    public void registerInstance(String serviceName, String group, Instance instance)
            throws NacosException {

        // 1. 服务名校验与标准化
        String groupedServiceName = NamingUtils.getGroupedName(serviceName, group);

        // 2. 构造心跳信息（临时实例需要心跳保活）
        if (instance.isEphemeral()) {
            BeatInfo beatInfo = new BeatInfo();
            beatInfo.setServiceName(groupedServiceName);
            beatInfo.setIp(instance.getIp());
            beatInfo.setPort(instance.getPort());
            beatInfo.setCluster(instance.getClusterName());
            beatInfo.setWeight(instance.getWeight());
            beatInfo.setMetadata(instance.getMetadata());
            beatInfo.setScheduled(false);
            beatInfo.setPeriod(instance.getInstanceHeartBeatInterval());
            // 启动心跳任务
            beatReactor.addBeatInfo(groupedServiceName, beatInfo);
        }

        // 3. 调用 HTTP API 注册实例
        reqApi(UtilAndComs.NACOS_URL_INSTANCE, params, HttpMethod.POST);
    }
}
```

### 8.3 Nacos 与 Spring Cloud 的自动装配

```java
// NacosServiceRegistryAutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnNacosDiscoveryEnabled
@AutoConfigureAfter(NacosServiceManagementConfiguration.class)
public class NacosServiceRegistryAutoConfiguration {

    @Bean
    public NacosServiceRegistry nacosServiceRegistry(
            NacosDiscoveryProperties nacosDiscoveryProperties) {
        return new NacosServiceRegistry(nacosDiscoveryProperties);
    }

    @Bean
    public NacosRegistration nacosRegistration(
            ObjectProvider<Map<String, NacosRegistrationCustomizer>> registrationCustomizers,
            NacosDiscoveryProperties nacosDiscoveryProperties,
            ApplicationContext context) {
        return new NacosRegistration(registrationCustomizers, nacosDiscoveryProperties, context);
    }

    @Bean
    public NacosAutoServiceRegistration nacosAutoServiceRegistration(
            NacosServiceRegistry registry,
            AutoServiceRegistrationProperties autoServiceRegistrationProperties,
            NacosRegistration registration) {
        return new NacosAutoServiceRegistration(registry,
                autoServiceRegistrationProperties, registration);
    }
}
```

### 8.4 设计模式运用

| 设计模式 | 应用位置 | 说明 |
|---------|---------|------|
| 策略模式 | `NamingService` 接口 | 不同的命名服务实现策略 |
| 观察者模式 | `NotifyCenter` | 服务变更时通知订阅者 |
| 适配器模式 | `SpringCloudSdkAdapter` | 适配不同的 Spring Cloud 版本 |
| 模板方法模式 | `AbstractNamingService` | 定义注册/发现的标准流程 |
| 单例模式 | `NacosFactory` | Nacos 客户端实例管理 |
| 生产者-消费者 | `DistroTaskEngine` | 同步任务的异步处理 |

### 8.5 性能调优建议

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| `db.pool.config.maxTotal` | 100+ | 数据库连接池上限，根据节点数调整 |
| `nacos.naming.distro.taskDispatchThreadCount` | CPU 核数 * 2 | 分发线程数 |
| `nacos.naming.distro.taskDispatchPeriod` | 2000ms | 分发任务周期 |
| `nacos.naming.distro.batchSyncKeyCount` | 1000 | 批量同步的数据量 |
| `nacos.naming.cleaner.trigger-ms` | 30000ms | 过期实例清理间隔 |
| `nacos.core.auth.plugin.nacos.token.secret.key` | 自定义密钥 | 生产环境必配权限认证 |

> 🎯 本章学习了 Nacos 作为注册中心的核心能力：服务注册与发现的完整流程、心跳机制的三个时间窗口、AP/CP 模式切换原理、保护阈值防止雪崩。掌握这些知识后，你可以设计高可用的微服务注册中心架构。

---

> 本文档属于 `02-中间件与微服务工程/06-Spring全家桶与微服务` 模块，P1 就业必备层级。下一章：[10-配置中心-Nacos-Config](./10-配置中心-Nacos-Config.md)。
