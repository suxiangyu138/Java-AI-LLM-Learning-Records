# 03 超时重试与 HTTP 客户端

> 传输篇：**超时配置（connectTimeout/readTimeout，camelCase 键）、超时可刷新、重试（默认关闭！）、HTTP 客户端选型（Apache HC5）、连接池**——"调用慢/超时"的排查与调优全在这篇。

---

## 📚 目录

1. [超时配置：connectTimeout / readTimeout](#1-超时配置connecttimeout--readtimeout)
2. [超时可刷新与优先级](#2-超时可刷新与优先级)
3. [重试：默认关闭的 Retryer](#3-重试默认关闭的-retryer)
4. [HTTP 客户端选型](#4-http-客户端选型)
5. [连接池与性能](#5-连接池与性能)
6. [超时链对齐](#6-超时链对齐)

---

## 1. 超时配置：connectTimeout / readTimeout

### 1.1 用法（注意 camelCase 键！）

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:                    # ★ 全局默认（所有客户端）
            connectTimeout: 5000      # 连接超时 5s
            readTimeout: 5000         # 读取超时 5s
          payment-service:            # ★ 按客户端名覆盖
            connectTimeout: 2000
            readTimeout: 3000
```

### 1.2 语义与默认

| 参数 | 语义 | 默认 |
|------|------|------|
| connectTimeout | 建立连接的超时（TCP 连接） | 无显式默认（Feign 默认 10s） |
| readTimeout | 连接建立后等待响应的超时 | 无显式默认（Feign 默认 60s） |

> ⚠️ **键名坑**：官方属性键是 **camelCase**（`connectTimeout`），不是 `connect-timeout`——短横线写法不生效（[08 篇](08-生产实践与选型避坑.md) 坑位表）；yaml 里两者等价映射但 OpenFeign 的属性绑定只认 camelCase 声明。

### 1.3 常见配置模式

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 3000      # 全局：连接 3s
            readTimeout: 5000         # 全局：读取 5s
          payment-service:            # 慢服务单独放宽
            readTimeout: 10000
          notification-service:       # 轻服务收紧
            readTimeout: 2000
```

> 🎯 面试必答：**"Feign 超时和下游超时怎么配合？"**——**超时链**：网关超时（如 10s）> 调用方 Feign readTimeout（如 5s）> 下游自身超时（如 3s）——**外层必须大于内层**，否则外层先超时，下游的降级/兜底逻辑永远触发不了（[6 节](#6-超时链对齐)）。

## 2. 超时可刷新与优先级

### 2.1 超时可刷新（refresh-enabled）

```yaml
spring:
  cloud:
    openfeign:
      client:
        refresh-enabled: true     # ★ 超时等 Request.Options 可动态刷新（默认 false）
```

```text
refresh-enabled=true 效果：
  ├── Request.Options 变成 refresh-scope Bean
  ├── 改配置 + /actuator/refresh → 新超时生效（无需重启）
  └── 代价：每次请求多一次 scope 查询（性能略降）
```

> 💡 生产实践：**超时经常调的服务开 refresh-enabled**；固定超时不开（性能优先）。

### 2.2 属性 vs 配置类优先级

```yaml
# 默认：属性（yaml）优先于 @Configuration 配置类
spring:
  cloud:
    openfeign:
      client:
        default-to-properties: true   # 默认 true；false = 配置类优先
```

| 优先级 | 默认（true） | false |
|--------|------------|-------|
| 高 | yaml 属性 | @Configuration Bean |
| 低 | @Configuration Bean | yaml 属性 |

> 💡 团队实践：**团队规范用 yaml（可审计、可刷新），保持默认 true**；配置类方案仅用于"属性表达不了"的复杂逻辑。

## 3. 重试：默认关闭的 Retryer

### 3.1 默认行为（重要！）

```text
Spring Cloud OpenFeign 默认：Retryer.NEVER_RETRY（不重试）
  └── 与原生 Feign 不同（原生 Feign 默认对 IOException 重试 5 次）
      —— Spring Cloud 官方默认关闭，避免"重复请求"事故
```

### 3.2 开启重试

```yaml
# 方式一：属性指定 Retryer 类
spring:
  cloud:
    openfeign:
      client:
        config:
          payment-service:
            retryer: com.example.MyRetryer   # 类需是 Bean 或有默认构造
```

```java
// 方式二：全局配置类
@Configuration
public class FeignConfig {
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);   // 间隔 100ms 起、最大 1000ms、最多 3 次
    }
}
```

### 3.3 重试策略要点

| 规则 | 说明 |
|------|------|
| 只重试瞬时故障 | 连接异常/超时可重试；业务异常（ErrorDecoder 抛的）不重试 |
| 幂等检查 | 写接口重试 = 重复提交（除非下游幂等） |
| 与熔断组合 | 重试在熔断内层（先熔断后重试语义，[04 篇](04-熔断降级集成.md)） |
| 退避 | Retryer.Default 指数退避（100ms ×1.5 递增） |

> ⚠️ **重试风暴警示**：100 请求 × 3 重试 = 300 次下游调用——下游已挂时重试加剧雪崩；**重试必须配合熔断（下游故障时熔断快速失败，重试不生效）**。

## 4. HTTP 客户端选型

### 4.1 三层客户端

| Client | 依赖 | 特点 | 默认 |
|--------|------|------|------|
| FeignBlockingLoadBalancerClient | starter-loadbalancer | **LB 集成**（服务名→实例）+ 底层 Client 再包装 | ✅ 默认 |
| Apache HttpClient 5 | httpclient5 依赖 | 连接池、HTTP/2、TLS 细粒度 | 加依赖自动切换 |
| OkHttp | okhttp 依赖 | 轻量 | 可选 |
| Java 11 HttpClient | 内置 | JDK 原生 | 可选 |

```xml
<!-- 切 Apache HC5 -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

### 4.2 默认 Client 的职责链

```text
FeignBlockingLoadBalancerClient（最外层，LB 集成）
  └── 内部：真正的 HTTP 发送
        ├── 默认：Feign 内置 Client（java.net.HttpURLConnection）
        └── 加 HC5：ApacheHttp5Client（连接池/HTTP2）
```

> ⚠️ **HC4 已移除**：Spring Cloud OpenFeign 4.0 起 **Apache HttpClient 4 不再支持**（只用 HC5）——老项目升级时 `httpclient`（HC4）依赖换成 `httpclient5`。

## 5. 连接池与性能

### 5.1 Client Bean 必须 Singleton（官方强调）

```text
Feign Client Bean 必须是 Singleton 作用域：
  └── 非 Singleton（如 request 作用域）→ 每次请求新建连接 → 连接池耗尽/端口耗尽
  └── 生产必查：自定义 Client Bean 时确认单例
```

### 5.2 HC5 连接池配置

```yaml
spring:
  cloud:
    openfeign:
      httpclient:
        hc5:
          enabled: true          # 默认开（有依赖时）
          max-connections: 200
          max-connections-per-route: 50
          time-to-live: 900s
          connection-timer-repeat: 3000ms
          pool-concurrency-factor: 20
          pool-max-per-route: 200
```

### 5.3 性能调优方向

| 方向 | 手段 |
|------|------|
| 连接池 | max-connections / per-route 按 QPS × 延迟估算 |
| HTTP/2 | HC5 + http2 配置（多路复用） |
| 压缩 | compression.request.enabled（减少带宽，[05 篇](05-拦截器日志压缩速查.md)） |
| 超时 | 收紧 readTimeout（防慢调用占连接） |
| 重试 | 关（默认）或严格控制次数 |

> 💡 压测指标：**关注"连接建立率"与"连接复用率"**——复用率高 = 连接池健康；异常高建连率 = 池太小或 Client 非单例。

## 6. 超时链对齐

### 6.1 完整超时链（从外到内）

```text
网关（Gateway response-timeout，如 10s）
  → 调用方 Feign readTimeout（如 5s）
    → 下游服务自身超时（如 3s）
      → 下游数据库/中间件超时（如 2s）
外层 > 内层（每层留余量）
```

### 6.2 违反后果

| 违反 | 现象 |
|------|------|
| Feign 超时 > 网关超时 | 网关先 504，Feign 的降级/熔断统计失效 |
| Feign 超时 < 下游处理时间 | 正常慢请求被误杀（P99 校准） |
| 下游无超时 | 慢 SQL 无限拖拽全链路 |

> 🎯 调优步骤：**① 量下游 P99 → ② Feign readTimeout = P99 × 2（留余量）→ ③ 上游超时 > Feign → ④ 慢调用阈值 < Feign 超时**（配合熔断的慢调用统计，[04 篇](04-熔断降级集成.md)）——一条链路一个数理关系。

---

**下一模块**：[04-熔断降级集成](04-熔断降级集成.md)　**返回总览**：[00-Spring Cloud OpenFeign知识体系总览](00-Spring Cloud OpenFeign知识体系总览.md)

**【参考来源】**：[Spring Cloud OpenFeign 5.0.2（Timeouts/Retryer/HTTP Clients）](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html)
