# 微服务 - Sentinel 限流与熔断

> **定位**：Sentinel 是阿里开源的流量治理组件，轻量高效，提供限流+熔断+降级+热点防护。替代 Hystrix，国内主流选择。

---

## 1. 限流 vs 熔断

| 概念 | 类比 | 作用 |
|------|------|------|
| **限流** | 流量闸门 | 限制单位时间请求数，防止过载 |
| **熔断** | 故障隔离阀 | 故障服务自动断开，避免雪崩 |

### 组件对比

| 组件 | 优势 | 不足 |
|------|------|------|
| **Sentinel** ⭐ | 轻量、动态配置、控制台可视化 | 依赖 Spring Cloud Alibaba |
| Hystrix | 成熟稳定 | ❌ 已停更 |
| Resilience4j | 轻量、无依赖 | 控制台弱 |

---

## 2. 核心原理

```text
埋点采集流量 → 规则判断（限流/熔断）→ 执行动作（拒绝/排队/兜底）
```

| 环节 | 方式 |
|------|------|
| **埋点** | 注解 `@SentinelResource` / 自动埋点（Gateway/Feign） |
| **规则** | 控制台动态配置 / yml 本地配置 |
| **动作** | 直接拒绝 / 排队等待 / 匀速通过 |

---

## 3. 实操落地

### 依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

### 配置

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8719
```

### 注解埋点

```java
@RestController
@RequestMapping("/order")
public class OrderController {
    @GetMapping("/create")
    @SentinelResource(value = "/order/create",
        fallback = "orderFallback",         // 异常兜底
        blockHandler = "orderBlockHandler") // 限流兜底
    public ResultVO createOrder(@RequestParam Long userId) {
        return ResultVO.success("订单创建成功");
    }

    public ResultVO orderFallback(Long userId) {
        return ResultVO.fail("服务异常，请稍后再试");
    }

    public ResultVO orderBlockHandler(Long userId, BlockException e) {
        return ResultVO.fail("请求过于频繁，请稍后再试");
    }
}
```

### 启动控制台

```bash
java -jar sentinel-dashboard-1.8.6.jar
# 访问 http://localhost:8080  sentinel/sentinel
```

---

## 4. 进阶用法

| 功能 | 做法 |
|------|------|
| **按来源限流** | `limitApp: app` 只允许指定来源 |
| **热点参数限流** | 控制台→热点规则→指定参数索引+阈值 |
| **Feign 集成** | `feign.sentinel.enabled: true` |
| **熔断优化** | 提高阈值+最小请求数+延长恢复期 |

---

## 5. 常见问题

| 问题 | 方案 |
|------|------|
| 控制台看不到服务 | 检查 dashboard 地址 + 先发一个请求触发埋点 |
| 限流不生效 | resource 名与 `value` 一致 + 检查 grade 类型 |
| 熔断后不恢复 | 延长恢复期 + 排查服务是否真恢复 |
| 性能影响 | 只对核心接口埋点 + 减少规则数量 |
