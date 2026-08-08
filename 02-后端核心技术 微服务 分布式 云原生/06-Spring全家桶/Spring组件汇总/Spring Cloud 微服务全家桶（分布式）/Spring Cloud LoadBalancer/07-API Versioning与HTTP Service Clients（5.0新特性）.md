# 07 API Versioning 与 HTTP Service Clients（5.0 新特性）

> 新特性篇：**5.0 两大新能力——API Versioning 负载均衡（按实例版本选择）与 Spring HTTP Service Clients 自动集成**——多版本实例共存与声明式客户端的完整方案，Boot 4 时代的"版本化调用"新姿势。

---

## 📚 目录

1. [背景：为什么需要 API 版本负载均衡](#1-背景为什么需要-api-版本负载均衡)
2. [API Versioning 配置](#2-api-versioning-配置)
3. [版本匹配机制](#3-版本匹配机制)
4. [与 Gateway API Versioning 的配合](#4-与-gateway-api-versioning-的配合)
5. [HTTP Service Clients 集成](#5-http-service-clients-集成)
6. [选型与实践建议](#6-选型与实践建议)

---

## 1. 背景：为什么需要 API 版本负载均衡

### 1.1 场景

```text
多版本实例共存（滚动升级/兼容期）：
  ├── 支付服务 v1.0 实例（老逻辑）仍在服务老客户端
  ├── 支付服务 v1.1 实例（新逻辑）只服务新请求
  ├── 注册中心里两类实例并存
  └── 问题：普通 LB 随机选——老请求可能打到新实例（行为不一致）！

API Versioning LB（issue #1582）：
  ├── 请求带版本信息（头/参数/路径/媒体类型）
  ├── LB 按版本过滤实例（metadata.api-version 匹配）
  └── 老请求打老实例、新请求打新实例——版本隔离
```

> 🎯 一句话：**API Versioning = "请求的 API 版本"与"实例的 API 版本"配对**——多版本并存时保证流量不乱窜。

### 1.2 与 Path 版本（/v1/）的区别

| 方案 | 版本载体 | 粒度 | 局限 |
|------|---------|------|------|
| Path 版本（/v1/api） | URL 路径 | 接口级 | 服务内多版本共存（同一服务新旧实例）无法区分 |
| **API Versioning LB（5.0）** | 请求特征（头等） | **实例级** | 解决"新旧实例并存"的选路问题 |

> 💡 定位差异：**/v1 是"接口契约"版本（对外），API Versioning LB 是"实例路由"版本（对内）**——两者可组合使用。

## 2. API Versioning 配置

### 2.1 开启

```yaml
spring:
  cloud:
    loadbalancer:
      configurations: api-version            # ★ 一键开启（阻塞/响应式都生效）
```

### 2.2 版本信息从哪来（四种载体）

```yaml
spring:
  cloud:
    loadbalancer:
      clients:                               # 按服务定制（注意前缀是 load-balancer）
        payment-service:
          api-version:
            header: X-API-Version            # ① 从请求头读版本
            # queryParameter: version        # ② 从查询参数读
            # pathSegment: 1                 # ③ 从路径段读
            # mediaTypeParameters: version   # ④ 从媒体类型参数读
            default-version: 1.0             # ★ 请求无版本信息时的默认版本
            required: false                  # 版本匹配要求：true=必须匹配否则拒绝
            fallback-to-available-instances: true   # 无匹配时回退全部实例
```

### 2.3 实例侧标注版本

```yaml
# 实例注册时在 metadata 写版本
spring:
  cloud:
    nacos:
      discovery:
        metadata:
          api-version: 1.1          # ★ 该实例提供 1.1 版本 API
```

## 3. 版本匹配机制

### 3.1 匹配流程

```text
ApiVersionServiceInstanceListSupplier：
  ├── ① 从请求提取版本（按配置的载体：头/参数/路径/媒体类型）
  ├── ② 请求无版本 → 用 default-version
  ├── ③ 过滤：保留 metadata.api-version 与请求版本匹配的实例
  ├── ④ 无匹配实例：
  │     ├── required=true → 空列表（调用失败，显式拒绝）
  │     └── fallback-to-available-instances=true → 回退全部实例
  └── ⑤ 匹配列表交给 LoadBalancer 选择（轮询/随机...）

版本语义解析：
  ├── 默认 SemanticApiVersionParser（Spring Framework 7 的语义版本解析：主.次.补丁比较）
  └── 自定义：提供 ApiVersionParser Bean 覆盖（精确/前缀匹配等）
```

### 3.2 与 Framework 7 的关系

```text
官方说明：
  ├── 基于 Spring Framework 7.0 的 blocking/reactive API versioning 支持构建
  ├── 与 Framework 默认行为差异：LB 版不做版本校验（无匹配时按配置回退/拒绝）
  └── 好处：LB 集成零成本（语义解析器等基础设施复用）
```

### 3.3 关键配置语义表

| 配置 | 默认 | 语义 | 陷阱 |
|------|------|------|------|
| default-version | 无 | 请求无版本时的兜底 | 不配 + 请求无版本 → 无匹配 |
| required | false | 必须匹配 | true + 无匹配 = 全部拒绝（服务不可用！） |
| fallback-to-available-instances | true | 无匹配回退全部 | 回退 = 版本隔离失效（老请求可能打新实例） |
| 载体 | 头/参数/路径/媒体类型 | 请求版本从哪读 | 载体不一致（客户端头 vs 服务端参数）永远匹配不上 |

> ⚠️ **版本隔离 vs 可用性的权衡**：`required=true` 严格隔离（无匹配就拒绝）但风险是"版本没对上传 → 全部拒绝"；`fallback` 保可用性但破坏隔离——**上线期用 fallback（先保流量），稳定后用 required（强隔离）**。

## 4. 与 Gateway API Versioning 的配合

### 4.1 两层版本路由

```text
完整的多版本方案（两级）：
  ├── 网关层：API Versioning 谓词（5.0 新）——按版本路由到"服务"（不同路径/服务）
  └── LB 层：API Versioning supplier——按版本选择"实例"（同一服务的不同版本实例）

组合示例：
  请求带 X-API-Version: 1.1
    → 网关：路由到 payment-service（版本谓词）
    → LB：选 metadata.api-version=1.1 的实例（API Versioning supplier）
```

> 🎯 面试加分：**"5.0 的版本路由怎么落地？"**——**两级配合**：Gateway 的版本谓词决定"进哪个服务/路由"，LoadBalancer 的版本 supplier 决定"选哪个版本实例"——网关管服务级，LB 管实例级，缺一层都不完整（对照 [Gateway 03 篇](../Spring Cloud Gateway/03-路由谓词工厂速查.md)）。

## 5. HTTP Service Clients 集成

### 5.1 背景

```text
Spring Framework 7 的声明式 HTTP 客户端（@HttpExchange）：
  ├── RestClient 版 / WebClient 版（Spring AI/新项目的首选客户端形态）
  ├── 5.0 起 LoadBalancer 自动集成（issue #1491/#1492）
  └── 意义：声明式客户端也能服务名调用 + LB
```

### 5.2 用法

```java
// ① 定义声明式客户端（Framework 7）
public interface PaymentServiceClient {
    @HttpExchange("/api/hello")
    String hello();
}

// ② 服务组自动获得 LB（base-url 自动设为 http://<serviceId>）
spring:
  http:
    serviceclient:
      payment-service:
        # base-url: http://payment-service   ← 不配则自动 http://payment-service（serviceId=组名）
        base-url: lb://payment-service        # ★ 也可显式 lb://（LB 语义）

// ③ 启用
@Configuration
class ClientConfig {
    @Bean
    PaymentServiceClient paymentClient(RestClient.Builder builder) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder))
                .build().createClient(PaymentServiceClient.class);
    }
}
```

### 5.3 集成语义

| 特性 | 说明 |
|------|------|
| 自动 base-url | `spring.http.serviceclient.<组名>.base-url` 为 null 时自动设为 `http://<serviceId>`（组名=服务名） |
| lb:// 支持 | base-url 可显式写 `lb://服务名`（走 LB 解析） |
| HTTPS 升级 | 选中 secure 实例 → scheme 自动升级 https |
| 非 lb URL | 显式 http(s):// 地址不参与 LB（原样直连） |
| 对比 Feign | @HttpExchange 是 Framework 7 原生；Feign 降级为"兼容适配器"（官方 2025.1 表态） |

> 🎯 面试必答：**"Feign 和 @HttpExchange 什么关系？"**——**官方 2025.1（Oakwood）明确：新项目导向 Framework 7 的 @HttpExchange 声明式客户端，OpenFeign 降级为"兼容适配器"（维护但不再是新方向）**——LoadBalancer 对两者都自动集成：Feign 通过 FeignBlockingLoadBalancerClient，@HttpExchange 通过 HTTP Service Clients 自动配置。

## 6. 选型与实践建议

### 6.1 决策建议

| 场景 | 建议 |
|------|------|
| 需要新旧实例并存（滚动升级） | API Versioning + fallback 过渡期 |
| 严格版本隔离（强一致） | API Versioning + required=true（配好 default-version） |
| 新项目客户端选型 | @HttpExchange（官方新方向）+ LB 自动集成 |
| 存量 Feign 项目 | 保持 Feign（兼容适配器仍维护） |
| 仅网关级版本路由 | Gateway 版本谓词足够（无实例级需求） |

### 6.2 上线 Checklist

```text
□ 1. 实例 metadata 全部标注 api-version（新旧实例都标！）
□ 2. 请求载体统一（头/参数选一个，客户端与服务端一致）
□ 3. default-version 配好（老客户端无版本头 → 打老版本实例）
□ 4. 过渡期 fallback=true → 稳定后 required=true
□ 5. 验证：新旧请求各打对应版本实例（LoadBalancerLifecycle 日志确认）
□ 6. 灰度：与 Hint 灰度组合（版本 + 灰度双维度）
```

> 💡 最佳实践：**API Versioning 与 Hint 灰度组合**——版本决定"打新还是打旧实例"，Hint 决定"新实例里打灰度还是全量"——多维流量治理的完整矩阵（[03 篇](03-负载均衡策略全解.md) 组合语义）。

---

**下一模块**：[08-生产实践与选型避坑](08-生产实践与选型避坑.md)　**返回总览**：[00-Spring Cloud LoadBalancer知识体系总览](00-Spring Cloud LoadBalancer知识体系总览.md)

**【参考来源】**：[Spring Cloud LoadBalancer 5.0.2 官方文档（API Versioning / HTTP Service Clients）](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)、[Spring Cloud 2025.1.0（Oakwood）发布公告](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)、[Spring Cloud Gateway（API Versioning 谓词）](../Spring Cloud Gateway/03-路由谓词工厂速查.md)
