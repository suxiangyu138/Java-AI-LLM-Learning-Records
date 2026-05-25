# 从 Java 后端开发角度深度剖析微服务

> **文档定位**：Java 后端企业级技术文档 | 微服务架构  
> **核心技术栈**：Spring Boot + Spring Cloud Alibaba  
> **核心原则**：高内聚、低耦合、按业务域拆分

---

## 一、核心概念

### 1.1 微服务的定义

微服务是将一个单体 Java 应用按 **业务域** 拆分为多个独立部署、独立扩展、独立维护的 Java 应用，每个微服务聚焦单一业务场景，通过标准化通信方式协同工作。

### 1.2 微服务 vs 单体应用

| 维度 | 单体应用 | 微服务 |
|------|----------|--------|
| **职责边界** | 一个开发者维护全链路（Controller → Dao） | 聚焦单个微服务业务 |
| **部署方式** | 整体打包部署 | **独立打包、独立部署** |
| **扩展方式** | 整体水平扩展 | **按需扩展**（高并发服务单独扩容） |
| **技术栈** | 统一技术栈 | 可根据业务选择适配技术栈 |

> **核心前提**：微服务不是"银弹"。业务简单、并发低、团队小 → 单体应用更合适；业务复杂、并发高、团队大 → 微服务更合适。

---

## 二、底层原理

### 2.1 微服务五大核心维度

```
基础搭建（Spring Boot）→ 服务通信（REST/gRPC/MQ）→ 服务治理（注册/负载/熔断/配置）
    → 数据存储（数据隔离+分布式事务）→ 工程化（CI/CD/容器化/监控）
```

### 2.2 同步与异步通信对比

| 维度 | 同步通信（REST/gRPC） | 异步通信（MQ） |
|------|----------------------|----------------|
| **协议** | HTTP / HTTP/2 | AMQP / TCP |
| **适用场景** | 即时响应（订单创建、用户查询） | 解耦削峰（通知、日志） |
| **耦合度** | 较高（等待响应） | 低（发送即忘） |
| **Java 实现** | RestTemplate / Feign | Spring Cloud Stream |

### 2.3 服务治理体系

```
Nacos（注册中心 + 配置中心）→ Ribbon/LoadBalancer（负载均衡）→ Sentinel（熔断降级）
```

---

## 三、代码实现

### 3.1 服务通信（Feign 声明式调用）

```java
/** Feign 客户端接口 —— 声明式远程调用 */
@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/api/order/{id}")
    OrderDTO getOrderById(@PathVariable Long id);
}
```

### 3.2 Sentinel 熔断降级

```java
@RestController
public class OrderController {

    @GetMapping("/api/order/{id}")
    @SentinelResource(value = "getOrder", fallback = "getOrderFallback")
    public OrderDTO getOrder(@PathVariable Long id) {
        // 调用订单服务
    }

    /** 降级方法 */
    public OrderDTO getOrderFallback(Long id, Throwable t) {
        return OrderDTO.defaultOrder();
    }
}
```

### 3.3 Nacos 配置中心

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848    # 注册中心地址
      config:
        server-addr: 127.0.0.1:8848    # 配置中心地址
```

---

## 四、实战要点

### 4.1 微服务拆分原则

| 原则 | 说明 |
|------|------|
| **按业务域拆分** | 用户服务、订单服务、库存服务（非按技术层） |
| **高内聚低耦合** | 一个微服务只负责一个核心业务场景 |
| **数据隔离** | 每个微服务拥有独立数据库，禁止跨服务直接操作 |

### 4.2 分布式事务选型

| 方案 | 适用场景 |
|------|----------|
| **Seata AT 模式** | 核心业务（支付、订单），强一致性 |
| **消息队列 + 本地事务表** | 非核心业务（通知、日志），最终一致性 |
| **避免跨服务事务** | 最简方案——核心逻辑合并到一个微服务 |

---

## 五、避坑总结

| 痛点 | 问题 | 解决方案 |
|------|------|----------|
| **拆分不合理** | 按技术层而非业务域拆分 | 按 DDD 领域驱动设计拆分 |
| **分布式事务数据不一致** | 订单成功但库存扣减失败 | Seata AT / MQ + 最终一致性 |
| **接口版本混乱** | 多服务接口定义不统一 | Swagger 文档 + URL 版本化（`/api/v1/`） |
| **问题排查困难** | 分布式调用链路复杂 | SkyWalking 链路追踪 + ELK 日志聚合 |
| **雪崩效应** | 一个服务故障拖垮整个链路 | Sentinel 熔断 + 降级 |

---

## 六、企业级最佳实践

### 6.1 技术栈总览

```
服务框架：Spring Boot + Spring Cloud Alibaba
注册中心/配置中心：Nacos
服务调用：Feign + LoadBalancer
熔断降级：Sentinel
网关：Spring Cloud Gateway
分布式事务：Seata
链路追踪：SkyWalking
容器化：Docker + Kubernetes
CI/CD：Jenkins / GitLab CI
```

### 6.2 工程化 Checklist

- [ ] 微服务按业务域拆分（非技术层）
- [ ] 每个微服务独立数据库
- [ ] 接口文档化（Swagger/Knife4j）
- [ ] 熔断降级规则已配置
- [ ] 分布式链路追踪已部署
- [ ] CI/CD 流水线已建立
- [ ] 配置中心统一管理多环境配置
