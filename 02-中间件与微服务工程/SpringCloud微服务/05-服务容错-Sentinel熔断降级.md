# 05 - 服务容错：Sentinel 熔断降级

> 🎯 Sentinel 是阿里开源的流量防卫兵 — 流控削峰、熔断兜底、系统自适应保护，替代已停维的 Hystrix，是 Spring Cloud Alibaba 的容错标准组件

---

## 目录

1. [Sentinel 概述与安装](#1-sentinel-概述与安装)
2. [流控规则（Flow Control）](#2-流控规则flow-control)
3. [熔断降级（Circuit Breaking）](#3-熔断降级circuit-breaking)
4. [热点与系统规则](#4-热点与系统规则)
5. [规则持久化](#5-规则持久化)

---

## 1. Sentinel 概述与安装

### 1.1 核心概念

| 概念 | 说明 |
|------|------|
| **资源** | Sentinel 保护的对象（方法/接口） |
| **规则** | 对资源的控制策略（流控/熔断/热点） |
| **Entry** | 进入资源的入口，统计流量 |
| **Slot Chain** | 责任链，逐槽处理（统计→规则检查→流控） |

### 1.2 Sentinel 控制台

```bash
docker run -d --name sentinel -p 8080:8080 \
  -e SENTINEL_DASHBOARD_USERNAME=sentinel \
  -e SENTINEL_DASHBOARD_PASSWORD=sentinel \
  bladex/sentinel-dashboard:1.8.6
# 访问 http://localhost:8080 (sentinel/sentinel)
```

### 1.3 依赖与配置

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: 127.0.0.1:8080        # 控制台地址
      eager: true                         # 启动即注册（懒加载默认）
```

---

## 2. 流控规则（Flow Control）

### 2.1 两种阈值类型

| 类型 | 含义 | 适用 |
|------|------|------|
| **QPS** | 每秒请求数超过阈值限流 | 大部分场景 |
| **线程数** | 并发线程数超过阈值限流 | 耗时接口 |

### 2.2 三种流控模式

| 模式 | 行为 | 示例 |
|------|------|------|
| **直接** | 当前资源超过阈值直接限流 | `/api/order` QPS>100 → 限流 |
| **关联** | 关联资源超阈值时，当前资源被限流 | `/api/order/create` 超阈值 → 限流 `/api/order/query` |
| **链路** | 只有从入口资源进来的请求才限流 | 从 `gateway` 入口的 `/api/order` 限流 |

### 2.3 流控效果

| 效果 | 说明 | 适用 |
|------|------|------|
| **快速失败** | 超过阈值直接拒绝 | 默认 |
| **Warm Up** | 预热：初始阈值低，逐渐提升 | 防止冷启动压垮系统 |
| **排队等待** | 请求排队，匀速通过 | 削峰填谷 |

### 2.4 注解方式定义

```java
@RestController
public class OrderController {

    @GetMapping("/order/{id}")
    @SentinelResource(value = "getOrder",
        blockHandler = "getOrderBlock")
    public Order getOrder(@PathVariable Long id) {
        return orderService.get(id);
    }

    // 限流后的降级方法（必须在同一个类，参数+返回类型一致）
    public Order getOrderBlock(Long id, BlockException e) {
        return new Order(id, "降级订单", 0);
    }
}
```

---

## 3. 熔断降级（Circuit Breaking）

### 3.1 三种熔断策略

| 策略 | 触发条件 | 适用 |
|------|----------|------|
| **慢调用比例** | 慢调用（RT > 阈值）占比 > 设定比例 | 服务变慢 |
| **异常比例** | 异常占比 > 设定比例 | 服务异常 |
| **异常数** | 1 分钟内异常 > 设定数量 | 精确控制 |

### 3.2 配置示例

```
规则：慢调用比例熔断
  - 最大 RT：200ms
  - 比例阈值：50%
  - 熔断时长：10s
  - 最小请求数：5
  - 统计时长：1s

含义：1 秒内 ≥5 个请求，其中 50% 的 RT>200ms
  → 触发熔断，接下来 10s 直接降级（不调用真实方法）
  → 10s 后进入半开状态，试探一个请求
  → 成功 → 关闭熔断；失败 → 继续熔断 10s
```

```java
@SentinelResource(value = "createOrder",
    fallback = "createOrderFallback",       // 业务异常降级
    blockHandler = "createOrderBlock")      // 流控熔断降级
public Order createOrder(OrderRequest req) {
    return orderService.create(req);
}

// Fallback：处理业务异常（如数据库挂了）
public Order createOrderFallback(OrderRequest req, Throwable e) {
    log.error("创建订单失败", e);
    return Order.failed("服务暂时不可用");
}
```

| 类型 | 触发条件 | 方法 |
|------|----------|------|
| `blockHandler` | 流控/熔断触发 | 参数+返回类型一致 + `BlockException` |
| `fallback` | 业务异常 | 参数+返回类型一致 + `Throwable` |

---

## 4. 热点与系统规则

### 4.1 热点参数限流

```
规则：对 getUser(Long id) 的 id 参数限流
  - 参数索引：0
  - 单机阈值：100（默认 QPS）
  - 例外参数：
    id=1 → QPS 1000 （VIP 用户不限流）
    id=2 → QPS 500

效果：普通用户 QPS 超过 100 就限流，VIP 用户享受高配额
```

```java
@GetMapping("/user/{id}")
@SentinelResource(value = "getUser",
    blockHandler = "getUserBlock")
public User getUser(@PathVariable Long id) { ... }
```

### 4.2 系统自适应规则

| 规则 | 阈值 | 说明 |
|------|:---:|------|
| **Load** | 系统负载 > 阈值 | Linux load1（仅 Linux） |
| **RT** | 平均 RT > 阈值 | 所有入口流量的平均 RT |
| **线程数** | 并发线程数 > 阈值 | 全局线程数 |
| **入口 QPS** | 总 QPS > 阈值 | 全局 QPS |
| **CPU 使用率** | CPU > 阈值 | 系统 CPU 使用率 |

---

## 5. 规则持久化

> ⚠️ 默认规则存在内存中，重启 Sentinel 控制台丢失。

```yaml
# 方式1：Nacos 持久化（⭐ 推荐）
spring:
  cloud:
    sentinel:
      datasource:
        ds-flow:
          nacos:
            server-addr: 127.0.0.1:8848
            data-id: ${spring.application.name}-flow-rules
            group: SENTINEL_GROUP
            data-type: json
            rule-type: flow          # flow / degrade / system

        ds-degrade:
          nacos:
            server-addr: 127.0.0.1:8848
            data-id: ${spring.application.name}-degrade-rules
            rule-type: degrade
```

> 🎯 **容错体系**：QPS 流控防刷、慢调用熔断兜底、系统规则保全局、热点规则精细化。规则统一持久化到 Nacos，实现配置即代码。
