# 00 Spring Cloud LoadBalancer 知识体系总览

> 组件卡片：Spring Cloud LoadBalancer 是什么、版本现状、能做什么、与对照体系如何衔接——**客户端负载均衡（Ribbon 的官方继任者）**，为 Feign/WebClient/RestTemplate 提供"从注册中心选一个实例"的能力；5.x 主线。对照体系见 [Spring Cloud Gateway](../Spring Cloud Gateway/00-Spring Cloud Gateway知识体系总览.md)（网关侧 lb://）、[Nacos 注册中心](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md) 与 [后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/)。

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与对照体系的映射](#4-与对照体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Cloud LoadBalancer 是 Spring Cloud 官方的客户端负载均衡组件**——调用方（Feign/WebClient/RestTemplate）发起服务名调用时，由它向注册中心获取实例列表并按策略（默认轮询）**选出一个实例**完成调用——Ribbon 停止维护后的官方替代品，也是网关 `lb://` 的底层实现。

```text
核心心智模型：
    order-service 调用 payment-service（serviceId 调用）
        │
        ▼
    Spring Cloud LoadBalancer（客户端侧，在调用方进程内）
        │
        ├── ① ServiceInstanceListSupplier：从注册中心拿实例列表
        │        （可叠加：缓存 → 过滤（健康/权重/区域/提示）→ 输出）
        ├── ② ReactiveLoadBalancer：按策略选一个实例
        │        （RoundRobin 默认 / Random / Weighted ...）
        └── ③ 用选中实例的 ip:port 发起真实 HTTP 调用
        │
        ▼
    payment-service 实例 A（192.168.1.10:8082）← 或 B / C
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Cloud 官方（代码在 **spring-cloud-commons** 项目内） |
| 版本线 | **5.x 主线（5.0.2 当前主流，2025.1/Oakwood，Boot 4）**；4.3.x 供 Boot 3.5 |
| 定位 | **客户端负载均衡**（调用方进程内选实例），与 Nginx 的服务端负载均衡相对 |
| 策略 | 轮询（默认）/随机/权重/区域/健康检查/提示/子集/粘性会话/API 版本（5.0 新增） |
| 集成 | @LoadBalanced RestTemplate、Feign、WebClient、Gateway lb://、HTTP Service Clients |
| 缓存 | Caffeine（默认 TTL 35s），防注册中心压力 |

### 1.1 LoadBalancer 解决什么问题

**① 服务名调用**：微服务里不写死 IP——`http://payment-service/api` 由 LoadBalancer 解析成真实实例地址（[02 篇](02-快速开始与核心API速查.md)）。

**② 实例故障感知**：实例下线后注册中心摘除，LoadBalancer 拉取列表自动避开（配合健康检查/缓存 TTL，[05 篇](05-缓存与健康检查.md)）。

**③ 流量调度策略**：按权重（不同配置实例）、按区域（同机房优先）、按提示（灰度实例）分流（[03 篇](03-负载均衡策略全解.md)）。

| 维度 | 没有 LoadBalancer | 有 LoadBalancer |
|------|-------------------|-----------------|
| 调用方式 | 写死 IP（实例变更即改代码） | 服务名调用，实例变更无感 |
| 实例故障 | 调用必失败直到手动改 | 注册中心摘除 + 缓存过期自动避开 |
| 多实例分配 | 无（全打一个） | 轮询/权重/区域智能分配 |
| 灰度 | 无 | Hint/权重/子集分流 |

> 🎯 判断标准一句话：**"代码里有没有写死别的服务 IP？"**——有，就该用 LoadBalancer；微服务生态（Feign/Gateway/WebClient）里它是标配底座，几乎隐式生效。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 服务间 HTTP 调用（Feign/RestTemplate/WebClient） | ✅ | 主战场 |
| 网关 lb:// 转发 | ✅ | Gateway 的底层实现 |
| 按权重/区域/灰度分流 | ✅ | 策略 supplier 组合 |
| 外部固定地址 API | ❌ | 无注册中心，直接 http:// 调用 |
| 服务端负载均衡（对外入口） | ❌ | 那是 Nginx/Gateway 的职责 |
| 数据库读写分离 | ❌ | 那是数据层中间件的领域 |

> ⚠️ **最大认知误区**：把 LoadBalancer 当"独立的负载均衡服务器"——**它是客户端进程内的算法库**（选实例的逻辑在调用方），与 Nginx 的"入口分发器"是两种不同层级的负载均衡（[08 篇](08-生产实践与选型避坑.md) 对比表）。

### 1.3 与负载均衡方案对照

| 方案 | 类型 | 位置 | 特点 | 生态 |
|------|------|------|------|------|
| **Spring Cloud LoadBalancer** | 客户端 | 调用方进程内 | 注册中心驱动、策略丰富、响应式 | **官方 Cloud** |
| Ribbon | 客户端 | 调用方进程内 | 2018 停止维护（Netflix 退出） | 遗留 |
| Nginx | 服务端 | 独立入口 | 高性能 L7 分发 | 独立 |
| Spring Cloud Gateway | 服务端 | 独立网关 | lb:// 走 LoadBalancer | 官方 Cloud |
| Nacos 负载均衡（SCA） | 客户端 | 调用方进程内 | 权重/集群感知（Nacos 驱动） | SCA |

> 🎯 面试必答：**"Ribbon 为什么被弃，替代方案是什么？"**——Ribbon 2018 年停止维护（Netflix 全家桶退出 Spring Cloud），官方替代是 **Spring Cloud LoadBalancer**：**响应式 API（ReactiveLoadBalancer）、策略用 ServiceInstanceListSupplier 组合式装配（比 Ribbon 的 rule 更灵活）、默认带 Caffeine 缓存**；国内 SCA 生态则常直接用 Nacos 自带的负载均衡（权重/集群感知更强）。

## 2. 版本现状（2026-08）

| 版本线 | 状态 | 关键点 |
|--------|------|--------|
| **5.x（5.0.2 主流）** | **当前主线** | Spring Cloud **2025.1.x（Oakwood）**、Boot 4.0/4.1、Framework 7；**API Versioning 负载均衡**、HTTP Service Clients 集成、JSpecify |
| 4.3.x（4.3.3） | 存量（EOL） | Spring Cloud 2025.0.x（Northfields）、Boot 3.5；OSS 已于 2026-06-30 结束 |
| 4.2.x（4.2.4） | 存量（EOL） | Spring Cloud 2024.0.x（Moorgate）、Boot 3.4 |
| 4.1.x（4.1.6） | 存量 | Spring Cloud 2023.0.x（Leyton）、Boot 3.2/3.3 |
| 3.x | 老存量 | Spring Cloud 2021.0/2022.0、Boot 2.6-3.1 |
| 2.x | 废弃 | 2020.0 时代（Ribbon 并存期） |

> ⚠️ **版本策略（2026 起）**：**新项目 Boot 4.x + Cloud 2025.1.x + LoadBalancer 5.0.x**；存量 Boot 3.5 用 4.3.x（**OSS 已 EOL**）；版本由 spring-cloud-commons BOM 管理（LoadBalancer 代码在 commons 项目里，[01 篇](01-模块清单与版本矩阵.md)）。

### 2.1 5.0 关键变化（4.3.x → 5.0.x 迁移要点）

| 变化 | 说明 |
|------|------|
| **API Versioning 负载均衡** | 新增 `BlockingApiVersionServiceInstanceListSupplier` / `ReactiveApiVersionServiceInstanceListSupplier`（按实例 metadata 的 API 版本选择，issue #1582） |
| **HTTP Service Clients 集成** | LoadBalancer 自动集成 Spring Framework 7 的声明式 HTTP 客户端（RestClient/WebClient 版 @HttpExchange） |
| JSpecify | 公共 API null-safety 标注 |
| 4.1.0+ 行为保留 | `callGetWithRequestOnDelegates`（默认 true） |
| 核心 API 稳定 | ReactiveLoadBalancer / ServiceInstanceListSupplier / 属性前缀不变 |

### 2.2 关键演进时间线

| 版本 | 里程碑 |
|------|--------|
| 2.x（2020） | 作为 Ribbon 替代品诞生（默认仍兼容 Ribbon） |
| 3.x（2021-2022） | 响应式核心成熟；Ribbon 全面移除；策略 supplier 体系成形 |
| 4.0（2022.12） | Boot 3 首线：jakarta、AOT 支持 |
| 4.1-4.3（2023-2025） | callGetWithRequestOnDelegates、性能与缓存迭代 |
| 5.0（2025.11） | **API Versioning**、HTTP Service Clients 集成、JSpecify |

## 3. 能力地图

| 能力域 | 能力点 | 对应机制 |
|--------|--------|---------|
| 实例发现 | 从注册中心拉实例列表 | DiscoveryClientServiceInstanceListSupplier |
| 负载均衡 | 轮询（默认）/随机 | RoundRobinLoadBalancer / RandomLoadBalancer |
| 权重分流 | 按 metadata weight 加权 | WeightedServiceInstanceListSupplier |
| 区域优先 | 同区域实例优先 | ZonePreferenceServiceInstanceListSupplier |
| 健康感知 | 主动探测实例健康 | HealthCheckServiceInstanceListSupplier |
| 灰度提示 | 按 hint 头/属性过滤 | HintBasedServiceInstanceListSupplier |
| 实例子集 | 大集群确定性子集 | SubsetServiceInstanceListSupplier |
| 粘性会话 | 会话绑定实例 | RequestBasedStickySessionServiceInstanceListSupplier |
| **API 版本** | 按版本选择实例（5.0 新） | ApiVersionServiceInstanceListSupplier |
| 缓存 | 实例列表缓存 | Caffeine（TTL 35s 默认） |
| 集成 | RestTemplate/Feign/WebClient/Gateway/HTTP Client | 各集成适配器 |
| 可观测 | 请求统计 | Micrometer Stats + LoadBalancerLifecycle |

### 3.1 能力边界：LoadBalancer 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 服务注册 | Nacos/Eureka/Consul | LoadBalancer 只**消费**实例列表 |
| 服务端入口分发 | Nginx / Gateway | 客户端 LB 与服务端 LB 是两层 |
| 熔断降级 | CircuitBreaker（SCCB） | 选实例 ≠ 保护调用（配合使用） |
| 限流 | 网关/Sentinel | 与 LB 无关 |

> ⚠️ **常见归因错误**：实例挂了还被打，怪 LoadBalancer"没感知"——**排查路径："注册中心实例是否摘除 → 实例列表缓存（TTL 35s）是否过期 → 健康检查是否配置 → 策略是否过滤"**——LB 的感知速度受缓存 TTL 限制，不是实时（[08 篇](08-生产实践与选型避坑.md) 坑位表）。

## 4. 与对照体系的映射

| 速查文档 | 对照/深度体系 |
|---------|-------------|
| 02-核心 API | [Spring Cloud OpenFeign](../Spring Cloud OpenFeign/)（Feign 集成）、[Spring Cloud Gateway](../Spring Cloud Gateway/00-Spring Cloud Gateway知识体系总览.md)（lb:// 转发） |
| 03-策略 | [Nacos 注册中心](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md)（权重/集群感知对照） |
| 04-原理 | [JUC 高并发编程](../../../../../01-底层根基-Java核心底座/02-JUC高并发编程/)（响应式底座）、[后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/)（负载均衡白话） |
| 05-缓存 | [Caffeine（缓存）](../../../../../01-底层根基-Java核心底座/)（缓存实现） |
| 07-5.0 特性 | [Spring 框架核心](../../../../Spring框架核心/)（Framework 7 HTTP 客户端） |

> 💡 分工约定：**本系列回答"实例怎么选、策略怎么配、缓存怎么调、Ribbon 怎么迁"**；服务注册发现本身在 [Nacos 系列](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md)，网关侧的 lb:// 在 [Gateway 系列](../Spring Cloud Gateway/00-Spring Cloud Gateway知识体系总览.md)。

## 5. 快速上手 3 步

**① 加依赖**（starter）：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<!-- 注册中心（按你的选型） -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

**② 给 RestTemplate 标 @LoadBalanced**：

```java
@Configuration
public class RestClientConfig {
    @Bean
    @LoadBalanced                                   // ★ 关键：让 RestTemplate 支持服务名调用
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class OrderService {
    @Autowired
    private RestTemplate restTemplate;

    public String callPayment() {
        // ★ 服务名调用（不写 IP）：LoadBalancer 选实例后替换为真实地址
        return restTemplate.getForObject("http://payment-service/api/hello", String.class);
    }
}
```

**③ 配策略与缓存**（可选）：

```yaml
spring:
  cloud:
    loadbalancer:
      cache:
        ttl: 35s               # 实例列表缓存时长（默认 35s）
      configurations: weighted # 权重策略（默认轮询）
```

> 🎯 跑通即及格：**① 注册中心有 payment-service 两个实例；② 调接口多次，日志/监控看到请求分发到两个实例（轮询）；③ 停掉一个实例（等缓存过期后）请求自动只打存活实例**——三件事都通，主链路打通（[02 篇](02-快速开始与核心API速查.md)）。

### 5.1 三步验证与排障起点

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 服务名解析 | 日志/抓包看请求 URL | 请求打到注册的实例 ip:port |
| ② 轮询分发 | 连续调 N 次 | 两个实例交替收到（近似均匀） |
| ③ 故障规避 | 停一个实例，等 TTL | 请求只打存活实例（不报错） |

> 💡 排障起点：**先查注册中心实例列表（实例在不在）→ 再查 LB 缓存（TTL 是否过期）→ 最后查策略（是否被过滤）**——"打到了已下线的实例"大概率是缓存没过期（[08 篇](08-生产实践与选型避坑.md) 排错表）。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单与版本矩阵](01-模块清单与版本矩阵.md) | starter 坐标、commons 归属、版本矩阵（5.0.2/4.3.x）、5.0 变化 |
| [02-快速开始与核心API速查](02-快速开始与核心API速查.md) | @LoadBalanced、RestTemplate/WebClient/Feign 三集成、编程式 API |
| [03-负载均衡策略全解](03-负载均衡策略全解.md) | 9 种策略全清单（configurations 开关 + builder 组合） |
| [04-核心原理：Supplier 链与响应式实现](04-核心原理：Supplier 链与响应式实现.md) | 实例选择流程、supplier 组合模型、上下文隔离、vs Ribbon |
| [05-缓存与健康检查](05-缓存与健康检查.md) | Caffeine（TTL 35s）、health-check 探测、与注册中心缓存关系 |
| [06-重试与自定义扩展](06-重试与自定义扩展.md) | retry、自定义 LoadBalancer/Supplier、Lifecycle、@LoadBalancerClient |
| [07-API Versioning与HTTP Service Clients（5.0新特性）](07-API Versioning与HTTP Service Clients（5.0新特性）.md) | api-version 策略、metadata 匹配、声明式客户端集成 |
| [08-生产实践与选型避坑](08-生产实践与选型避坑.md) | vs Ribbon/Nacos 选型、坑位表、调优、升级路径 |

### 6.1 阅读顺序建议

- **第一次接触**：02（快速跑通）→ 03（策略）→ 05（缓存）；
- **项目实战**：02 → 05 缓存调优 → 08 避坑；
- **面试冲刺**：04 原理 → 00 vs Ribbon → 03 策略；
- **Boot 4 新特性**：07 API Versioning（5.0 亮点）。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| 注册中心 | [Nacos 注册中心](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md) | 实例列表来源 |
| Feign 调用 | [Spring Cloud OpenFeign](../Spring Cloud OpenFeign/) | 最常见的集成方 |
| Gateway lb:// | [Spring Cloud Gateway](../Spring Cloud Gateway/00-Spring Cloud Gateway知识体系总览.md) | 服务端侧的使用方 |
| 负载均衡概念 | [后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/) | 客户端/服务端 LB 白话 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会用 Spring Boot | 00 总览 → 02 快速跑通 → 03 策略 |
| 项目实践 | 上生产 | 02 → 05 缓存 → 08 避坑 |
| 面试冲刺 | 全考点 | 04 原理 → 00 vs Ribbon → 03 策略 |
| 架构选型 | 负责人 | 07 API Versioning → 08 vs Nacos |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| 客户端负载均衡 | 调用方进程内选实例（vs 服务端 Nginx） |
| ReactiveLoadBalancer | 核心接口：选择实例的策略抽象 |
| ServiceInstanceListSupplier | 实例列表供应器（可链式组合：发现→缓存→过滤） |
| RoundRobinLoadBalancer | 轮询策略（默认） |
| @LoadBalanced | 让 RestTemplate 支持服务名调用的注解 |
| lb:// | 网关/客户端里的服务名协议前缀 |
| metadata | 实例元数据（weight/zone/hint/版本... 策略读取） |
| Caffeine 缓存 | 实例列表缓存（默认 TTL 35s） |
| configurations | 策略开关：weighted/zone-preference/health-check... |
| Ribbon | 被替代的旧组件（2018 停止维护） |
| API Versioning | 5.0 新：按实例 API 版本负载均衡 |

---

**下一模块**：[01-模块清单与版本矩阵](01-模块清单与版本矩阵.md)　**返回总览**：本页

**【参考来源】**：[Spring Cloud LoadBalancer 5.0.2 官方文档（Spring Cloud Commons）](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)、[Spring Cloud 2025.1.0（Oakwood）发布公告](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)、[Spring Cloud release train EOL（endoflife.date）](https://endoflife.date/spring-cloud)
