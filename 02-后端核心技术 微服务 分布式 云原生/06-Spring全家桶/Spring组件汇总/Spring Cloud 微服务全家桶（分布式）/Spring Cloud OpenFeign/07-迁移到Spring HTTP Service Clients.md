# 07 迁移到 Spring HTTP Service Clients

> 迁移篇：**官方为什么把 OpenFeign 降级为"兼容适配器"、@HttpExchange 对比、迁移清单、何时不迁**——2025.1（Oakwood）后新项目的客户端选型决策；对照 [LoadBalancer 07 篇](../Spring Cloud LoadBalancer/07-API Versioning与HTTP Service Clients（5.0新特性）.md)（@HttpExchange 的 LB 集成）。

---

## 📚 目录

1. [官方立场：feature-complete 与迁移建议](#1-官方立场feature-complete-与迁移建议)
2. [@HttpExchange：Framework 7 的声明式客户端](#2-httpexchangeframework-7-的声明式客户端)
3. [与 OpenFeign 的全维度对比](#3-与-openfeign-的全维度对比)
4. [迁移清单（存量 Feign 项目）](#4-迁移清单存量-feign-项目)
5. [何时不迁：存量项目决策](#5-何时不迁存量项目决策)
6. [决策树](#6-决策树)

---

## 1. 官方立场：feature-complete 与迁移建议

### 1.1 官方原话（5.0.2 文档）

> "As announced in Spring Cloud 2022.0.0 release blog entry, we're now treating the Spring Cloud OpenFeign project as **feature-complete**. We are only going to be adding **bugfixes** and possibly merging some small community feature PRs. **We suggest migrating over to Spring HTTP Service Clients instead.**"

### 1.2 官方文档对响应式场景的明确建议

> "We suggest migrating over to Spring HTTP Service Clients instead. **Both blocking and reactive stacks are supported there.**"

### 1.3 三条关键事实

| 事实 | 说明 |
|------|------|
| ① 2022.0 起就是 feature-complete | 不是 5.0 才宣布（Kilburn 时代开始） |
| ② 5.0 无新特性 | 只有 Jackson 3 兼容 |
| ③ **官方转向的根基** | Spring Cloud Commons 5.0 让 @HttpExchange 自动获得：**服务发现 + 负载均衡 + 熔断**——补上了微服务场景的最后一块拼图 |

> 🎯 一句话：**@HttpExchange 不是"又一个 Feign"，是"Framework 7 原生 + Cloud 自动集成"的完整替代**——Cloud 5.0 把 Feign 的三大卖点（发现/LB/熔断）原样搬给了它，Feign 只剩"存量兼容"角色。

## 2. @HttpExchange：Framework 7 的声明式客户端

### 2.1 基本用法

```java
// ① 定义接口（Framework 7 原生注解）
public interface PaymentServiceClient {
    @HttpExchange("/api/hello")
    String hello();

    @GetExchange("/payments/{id}")           // 或专门方法注解
    Payment getPayment(@PathVariable("id") Long id);

    @PostExchange("/payments")
    Payment createPayment(@RequestBody PaymentRequest request);
}
```

```java
// ② 创建客户端（RestClient 版 / WebClient 版）
@Configuration
public class ClientConfig {
    @Bean
    PaymentServiceClient paymentClient(RestClient.Builder builder) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder))
                .build().createClient(PaymentServiceClient.class);
    }
}
```

### 2.2 微服务能力怎么获得（5.0 自动集成）

```yaml
# ③ 服务组配置（LoadBalancer 自动集成）
spring:
  http:
    serviceclient:
      payment-service:              # 组名 = 服务名
        # base-url 不配则自动设为 http://payment-service（服务名调用）
        # 也可显式：base-url: lb://payment-service
```

```text
5.0 起 @HttpExchange 自动获得（LoadBalancer HTTP Service Clients 集成）：
  ├── 服务名解析（DiscoveryClient 在场时）
  ├── 负载均衡（lb:// + LoadBalancer）
  ├── 熔断（CircuitBreaker 适配装饰）
  └── HTTPS 升级（选中 secure 实例自动 https）
```

> 🎯 对照记忆：**@FeignClient(name=) 的三大能力，@HttpExchange 用"serviceclient 组配置"等价获得**——Feign 的 name 属性 → 组名；Feign 的 fallback → 熔断装饰 + 方法降级。

## 3. 与 OpenFeign 的全维度对比

| 维度 | OpenFeign | @HttpExchange（HTTP Service Clients） |
|------|-----------|--------------------------------------|
| 出身 | Spring Cloud 封装（Netflix Feign 底座） | **Spring Framework 7 原生** |
| 注解 | @FeignClient + Spring MVC 注解 | @HttpExchange/@GetExchange/@PostExchange |
| 依赖 | spring-cloud-starter-openfeign | 无额外依赖（Framework 7 内置） |
| 代理层 | 反射代理（Feign ClientFactoryBean） | 代理（HttpServiceProxyFactory，更轻） |
| 服务发现/LB | ✅（LoadBalancer 集成） | ✅（5.0 自动集成） |
| 熔断 | ✅ fallback/fallbackFactory | ✅（5.0 熔断装饰 + 降级） |
| **响应式** | ❌（无） | ✅（WebClient 版，响应式链路可用） |
| 原生镜像 | ⚠️（反射代理不友好） | ✅ 更友好（编译期代理信息） |
| 启动速度 | 反射处理（略慢） | 更快 |
| 维护状态 | **feature-complete（兼容适配器）** | **官方新方向（活跃）** |
| 生态成熟度 | 极成熟（多年社区积累） | 较新（5.0 补齐微服务能力） |

> 🎯 面试必答：**"@HttpExchange 比 OpenFeign 好在哪？"**——**官方原话三卖点：① 无额外依赖（Framework 7 原生）；② 性能更好（去掉 Feign 的反射代理层）；③ 原生镜像更友好（启动更快）**；再加两点：**响应式支持（Feign 没有）、官方新方向（Feign 只修 bug）**——存量代码继续用 Feign 没问题，新代码官方明确导向 @HttpExchange。

## 4. 迁移清单（存量 Feign 项目）

### 4.1 迁移步骤

```text
□ 1. 接口改造：
     @FeignClient(name="payment-service")  →  接口本身去掉 @FeignClient
     @GetMapping/@PostMapping             →  @GetExchange/@PostExchange（或 @HttpExchange）
     @PathVariable/@RequestBody 等参数注解 → 不变（同一套语义）
□ 2. 客户端创建：
     新增 @Bean 用 HttpServiceProxyFactory 创建客户端（RestClient 或 WebClient 版）
□ 3. 服务配置：
     spring.http.serviceclient.<服务名>.base-url: lb://服务名（或省略自动 http://服务名）
□ 4. 熔断降级迁移：
     Feign fallback/fallbackFactory → 熔断装饰 + 降级方法（Spring Cloud CircuitBreaker 的适配）
□ 5. 拦截器迁移：
     RequestInterceptor → 客户端 Builder 的请求处理（ClientRequestInterceptor / 自定义）
□ 6. 回归验证：
     服务名解析、LB 分发、熔断降级、超时、链路追踪
```

### 4.2 迁移成本评估

| 项 | 成本 | 说明 |
|----|------|------|
| 接口注解 | 中 | 注解替换机械但量大（@GetMapping→@GetExchange 等） |
| 客户端创建 | 低 | 每服务一个 @Bean |
| 熔断/拦截器 | 中 | 语义对应但 API 不同 |
| 团队学习 | 中 | 新 API 熟悉成本 |
| 收益 | 长期 | 响应式能力、原生镜像、官方支持方向 |

> 💡 务实建议：**不要"为迁而迁"**——存量 Feign 代码稳定运行就继续用（bugfix 长期维护）；**迁移的时机**：① 新项目直接上；② 需要响应式/原生镜像时；③ 重大版本升级顺带改造（避免专门安排迁移工单）。

## 5. 何时不迁：存量项目决策

### 5.1 不迁的理由（都成立）

| 理由 | 说明 |
|------|------|
| 稳定运行 | feature-complete ≠ 废弃，bugfix 持续 |
| 迁移成本 > 收益 | 纯阻塞式 + 无原生镜像需求 → 迁移收益有限 |
| 团队熟 Feign | 换 API 有学习/出错成本 |
| 生态依赖 | 内部脚手架/网关对接基于 Feign 概念 |

### 5.2 必须评估迁移的场景

| 场景 | 原因 |
|------|------|
| 新项目（Boot 4） | 官方方向 + 少依赖 + 原生镜像友好 |
| 响应式链路 | Feign 无响应式支持（硬伤） |
| 原生镜像部署 | Feign 反射代理不友好 |
| 大版本升级中 | 顺带改造成本低（反正要动） |

## 6. 决策树

```text
新项目（Boot 4）？
  ├── 是 → @HttpExchange（HTTP Service Clients）★ 官方方向
  │         ├── 阻塞式 → RestClient 版
  │         └── 响应式 → WebClient 版
  └── 否（存量 Feign 项目）：
        ├── 稳定运行 + 无新需求 → 保持 Feign（bugfix 长期维护）✅
        ├── 需要响应式/原生镜像 → 迁移 @HttpExchange
        ├── 大版本升级中 → 顺带迁移（评估后）
        └── 团队想渐进 → 新接口用 @HttpExchange、老接口留 Feign（共存过渡）
```

> 🎯 最终结论：**"2026 年新代码写什么？"——官方答案：@HttpExchange**；**"存量 Feign 要慌吗？"——不慌**，bugfix 长期维护，按需迁移——**共存过渡（新接口 @HttpExchange + 老接口 Feign）是务实的渐进路径**。

---

**下一模块**：[08-生产实践与选型避坑](08-生产实践与选型避坑.md)　**返回总览**：[00-Spring Cloud OpenFeign知识体系总览](00-Spring Cloud OpenFeign知识体系总览.md)

**【参考来源】**：[Spring Cloud OpenFeign 5.0.2（feature-complete 声明/Reactive 建议）](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html)、[Spring Cloud LoadBalancer 5.0.2（HTTP Service Clients 集成）](../Spring Cloud LoadBalancer/07-API Versioning与HTTP Service Clients（5.0新特性）.md)、[Spring Cloud 2025.1.0（Oakwood）发布公告](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)
