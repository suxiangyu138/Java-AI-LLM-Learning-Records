# 03 - Spring Boot 4 与 Spring Framework 7 新特性

> 🎯 2025.11 发布的 Boot 4 / Framework 7 是 Spring "十年最大升级" — 模块化自动配置、虚拟线程默认、内置弹性、声明式 HTTP 客户端、AOT 一等公民。2026 面试必考，本文件讲透 10 大特性

---

## 目录

1. [发布背景与版本概况](#1-发布背景与版本概况)
2. [特性一：自动配置全量模块化](#2-特性一自动配置全量模块化)
3. [特性二：Jakarta EE 11 与新基线](#3-特性二jakarta-ee-11-与新基线)
4. [特性三：虚拟线程原生集成](#4-特性三虚拟线程原生集成)
5. [特性四：内置弹性能力](#5-特性四内置弹性能力)
6. [特性五：声明式 HTTP 客户端](#6-特性五声明式-http-客户端)
7. [特性六：API 版本控制一等公民](#7-特性六api-版本控制一等公民)
8. [特性七：可观测性 2.0](#8-特性七可观测性-20)
9. [特性八：AOT 与 GraalVM 原生镜像](#9-特性八aot-与-graalvm-原生镜像)
10. [特性九：Jackson 3 与安全升级](#10-特性九jackson-3-与安全升级)
11. [特性十：JSpecify 标准空安全](#11-特性十jspecify-标准空安全)
12. [对开发者的实际影响](#12-对开发者的实际影响)

---

## 1. 发布背景与版本概况

| 项目 | 版本 | 发布时间 | 定位 |
|------|------|----------|------|
| Spring Framework | 7.0 | 2025.11.20 | 引擎层大版本 |
| Spring Boot | 4.0 | 2025.11.20 | 开发层大版本 |
| Spring Boot | 4.1 | 2026.05 | 稳定演进线 |
| Spring Framework | 7.0.8 | 2026.06 | 修复 16 个高危 CVE |

**一句话概括：** 面向云原生、Java 21+、AOT 编译、原生镜像和虚拟线程场景的全面现代化重构 — 告别"为 2007 年设计、为 2015 年修修补补"的架构。

---

## 2. 特性一：自动配置全量模块化

**变更：** 原本 6.2MB 的 `spring-boot-autoconfigure` 单体 JAR 拆分为 **47 个轻量模块**，按需加载。

| 收益 | 实测数据 |
|------|----------|
| 启动更快 | 启动时间缩短 33%（4.2s → 2.8s） |
| 镜像更小 | Docker 镜像体积减少 19%（387MB → 312MB） |
| 原生镜像更优 | AOT 编译范围更小，静态分析更精确 |
| 依赖管理清晰 | 不再隐式引入全量自动配置 |

```xml
<!-- Boot 3：一个 autoconfigure 全包 -->
<dependency><groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId></dependency>

<!-- Boot 4：按需引入模块化自动配置 -->
<dependency><groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-webmvc-autoconfigure</artifactId></dependency>
```

**追问：** 对现有项目的影响？→ 原依赖声明继续可用（有兼容汇总模块），但推荐新项目按模块引入；部分冷门自动配置需显式添加模块才生效。

---

## 3. 特性二：Jakarta EE 11 与新基线

| 基线项 | 新标准 | 影响 |
|--------|--------|------|
| Jakarta EE | **11**（Servlet 6.1 / JPA 3.2 / Validation 3.1） | javax.* 彻底退出历史舞台 |
| Java | 最低 17，**推荐 21（LTS）**，兼容 21/24/25 | JDK 8 时代彻底终结 |
| Kotlin | 2.2 基线 | — |
| 容器 | Tomcat 11 / Jetty 12 | **Undertow 被移除**（不支持 Servlet 6.1） |

```java
// Boot 3 时代还能见到的旧写法（已无法编译）：
import javax.persistence.Entity;      // ❌
// Boot 4 统一：
import jakarta.persistence.Entity;    // ✅
```

**追问：** 为什么是"等了 10 年"？→ 上次 Jakarta 大迁移（EE 9/10，javax→jakarta）发生在 Boot 3（2022）；Boot 4 直接跳升到 EE 11，同时完成 Java 17 门槛 + 虚拟线程，跨度是 Boot 历史上最大的。

---

## 4. 特性三：虚拟线程原生集成

**变更：** MVC、WebFlux、@Scheduled、@Async 默认使用虚拟线程（`VirtualThreadTaskExecutor`）。

```yaml
# Boot 4 中默认生效；也可以显式控制
spring:
  threads:
    virtual:
      enabled: true        # 4.0 中默认开启
```

| 收益 | 说明 |
|------|------|
| 单机支撑上万并发请求 | 每请求一线程成为可行方案 |
| 线程池耗尽问题消失 | 阻塞不再消耗 OS 线程 |
| 异步编程简化 | 同步写法获得异步性能 |

**追问：** 有没有坑？→ ① 虚拟线程在 synchronized 内做阻塞 IO 会"钉住"载体线程 → 优先用 ReentrantLock ② 线程局部变量（ThreadLocal）在百万级虚拟线程下开销放大 → 考虑 Scoped Values ③ CPU 密集任务无收益。

---

## 5. 特性四：内置弹性能力

**变更：** 重试与并发限流直接集成进 Spring 核心 — 不再需要 Spring Retry / Resilience4j。

```java
// 声明式重试（spring-core 内置）
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200))
public Order getOrder(String id) { ... }

// 方法级并发限制（对虚拟线程场景尤其实用）
@ConcurrencyLimit(limit = 20)
public void process(String task) { ... }
```

| 能力 | 替代品 | 说明 |
|------|--------|------|
| @Retryable | Spring Retry | 支持命令式和响应式返回类型 |
| @ConcurrencyLimit | Resilience4j 限流 | 方法级并发控制 |
| 熔断/降级 | Resilience4j / Sentinel | 复杂场景仍需第三方（分布式场景） |

**追问：** Sentinel 还有必要吗？→ 有 — 内置能力解决"单机进程内"弹性；分布式限流（集群维度）、动态规则下发仍是 Sentinel 的战场。

---

## 6. 特性五：声明式 HTTP 客户端

**变更：** `@HttpServiceClient` / `@HttpExchange` 定义类型安全 HTTP 客户端 — **Feign 的官方替代方案**；RestTemplate 进入弃用倒计时（7.1 标记 @Deprecated）。

```java
// 定义接口（无需实现类）
@HttpServiceClient("order-service")          // 服务名（配合发现）
public interface OrderClient {
    @HttpExchange("/orders/{id}")
    Order getOrder(@PathVariable Long id);
}
```

| 对比 | @HttpServiceClient | OpenFeign |
|------|--------------------|-----------|
| 来源 | Spring 官方（Framework 7） | 社区项目 |
| 性能 | 优于 Feign（代理/反射少） | 有额外开销 |
| AOT/原生镜像 | 全面支持 | 需要额外配置 |
| 生态成熟度 | 新但官方主推 | 成熟但停更趋缓 |

**追问：** 微服务项目要从 Feign 迁走吗？→ 新项目直接上 @HttpServiceClient；存量项目迁移路径清晰（接口注解几乎 1:1），Boot 4 升级时可顺带迁移。

---

## 7. 特性六：API 版本控制一等公民

**变更：** 原生支持 4 种版本策略，RestClient / WebClient / @HttpExchange 全链路支持。

| 策略 | 示例 | 适用 |
|------|------|------|
| 路径 | `/v2/customer` | 最常见、可缓存 |
| 请求头 | `X-API-Version: v2` | 不改变 URL 结构 |
| 查询参数 | `?api-version=2` | 简单场景 |
| 媒体类型 | `Accept: application/vnd.v2+json` | 内容协商 |

```yaml
spring:
  mvc:
    api-versioning:
      enabled: true        # 需显式启用
      strategy: path       # path / header / query / media-type
```

> 💡 Boot 不提供默认方案 — 团队需显式选择策略并约定，这对多端（App/Web/开放平台）并存场景价值巨大。

---

## 8. 特性七：可观测性 2.0

**变更：** Micrometer 2 + OpenTelemetry 深度集成，内置 `spring-boot-starter-opentelemetry`。

| 能力 | 说明 |
|------|------|
| 开箱即用 | 分布式追踪、指标、日志、上下文传播 |
| 自动埋点 | Redis、MongoDB 等客户端自动埋点 |
| 导出 | OTLP 协议（对接 Grafana/Jaeger/SkyWalking 等） |
| 生态对齐 | 与 Spring AI 的 Token 监控、Micrometer 一致 |

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

---

## 9. 特性八：AOT 与 GraalVM 原生镜像

| 指标 | Boot 3.2 | Boot 4 |
|------|----------|--------|
| 启动时间 | 秒级 | **< 50ms** |
| 内存占用 | 基准 | **降低 30%–60%** |
| 反射 Hint | 需手写 reflect-config.json | **自动收集**（反射/资源/代理/序列化） |
| 编译速度 | 基准 | 增量 AOT 提升 ~40% |

```bash
# 构建原生镜像（GraalVM 或容器构建）
./mvnw -Pnative native:compile
./my-app          # 启动 <50ms，内存下降 30-60%
```

**追问：** 原生镜像的代价？→ 构建时间长、动态特性受限（反射/动态代理需 AOT 期确定）、调试体验差异。适合 Serverless / 冷启动敏感场景；传统长驻服务按需采用。

---

## 10. 特性九：Jackson 3 与安全升级

| 变更 | 说明 |
|------|------|
| Jackson 3 | 全栈默认（包名 `tools.jackson`），提供 Jackson 2 兼容模块，**7.2 彻底移除 Jackson 2** |
| Spring Security 7 | 增强 OIDC、优化 OAuth2 架构、现代化加密默认值、改进 MFA、零信任微服务模式 |
| 三方集成 | 无缝集成 Okta、Auth0、Azure AD |

```java
// Jackson 3：API 基本一致，包名变化
import tools.jackson.databind.ObjectMapper;   // Jackson 3
import com.fasterxml.jackson.databind.ObjectMapper;  // Jackson 2（过渡期）
```

---

## 11. 特性十：JSpecify 标准空安全

**变更：** 采用行业标准 JSpecify 1.0 注解替代 `org.springframework.lang` 自定义空注解。

| 收益 | 说明 |
|------|------|
| 编译期空安全 | IDE/静态分析工具可校验 @Nullable/@NonNull 契约 |
| 减少 NPE | 跨库 API 契约统一 |
| 生态互通 | 与其他采用 JSpecify 的库（Guava 等）互通 |

```java
// Framework 7 源码中的空安全标注
public @NonNull String resolve(@Nullable String key) { ... }
```

---

## 12. 对开发者的实际影响

### 12.1 技术栈变化一览

| 旧习惯（Boot 3） | 新习惯（Boot 4） |
|------------------|------------------|
| 引入 Spring Retry | 直接用 @Retryable |
| 引入 Resilience4j 限流 | 方法级 @ConcurrencyLimit |
| Feign 调用服务 | @HttpServiceClient |
| 手写 API 版本逻辑 | 框架原生策略 |
| 手动配链路（Sleuth/Zipkin） | starter-opentelemetry |
| javax.* 老 API | jakarta.* 全量 |
| RestTemplate | RestClient（构造器风格） |

### 12.2 面试必答句

```text
Q: 你怎么看 Spring Boot 4？
A: 三个关键词 — ① 基线换代（Java 17+ / Jakarta EE 11）② 性能重构（模块化 + 虚拟线程默认 + AOT）③ 内置能力整合（重试/限流/HTTP 客户端/可观测性）。
升级策略：3.5.x 稳定过渡 → Java 21 → 4.0，AI 应用配 Spring AI 2.x。
```

---

> 🎯 **核心要点**：Boot 4 的 10 大特性浓缩成一句话 — **"现代 Java 的一等公民体验"**：模块化让启动更快，虚拟线程让并发更高，内置弹性让代码更少，HttpServiceClient 让 Feign 退休，AOT 让云原生更近。2026 年面试聊到 Spring，谈 Boot 4 是"加分项"，能讲迁移路线是"加分到顶"。

**下一模块**：[04-SpringAI与AI应用生态](04-SpringAI与AI应用生态.md) / **返回总览**：[00-Spring生态知识体系总览](00-Spring生态知识体系总览.md)
