# 05 服务调用：OpenFeign 与负载均衡

> 微服务化的第二根支柱：服务间怎么优雅地调用。OpenFeign 把「HTTP 调用 + JSON 序列化 + 负载均衡 + 超时重试」封装成「声明一个接口」——调用方写接口签名，Feign 生成实现。这一篇覆盖声明式调用、超时重试、熔断集成与 2026 的 @HttpExchange 新方向。

## 📚 目录

1. [为什么用 Feign：从手动 HTTP 到声明式接口](#1-为什么用-feign从手动-http-到声明式接口)
2. [声明式调用与负载均衡](#2-声明式调用与负载均衡)
3. [超时、重试与熔断集成](#3-超时重试与熔断集成)
4. [链路传递与错误处理](#4-链路传递与错误处理)
5. [方向：@HttpExchange 与 HTTP Service Clients](#5-方向httpexchange-与-http-service-clients)
6. [验证实验](#6-验证实验)
7. [核心要点](#7-核心要点)

---

## 1. 为什么用 Feign：从手动 HTTP 到声明式接口

Level2 的单体内调用是 `docService.xxx()`（JVM 内方法调用）。微服务化后 kb-ai 调 kb-doc 变成跨进程 HTTP——**最朴素的做法是手写 RestClient 调用**：拼 URL、设置头、序列化、解析响应、处理错误，每个服务间调用都要写一遍，且「URL 写死」（回到 04 篇的问题）。OpenFeign 的答案：**调用方声明一个接口**，Feign 在运行时生成实现——接口签名即契约，URL 由服务名 + LoadBalancer 解析（不写死地址），序列化与错误处理内置。收益：**契约清晰**（接口 = 服务间 API 文档）、**调用代码量减少 80%**、**与注册中心天然集成**（服务名自动解析实例列表）。代价也讲清：**接口与实现分离**（提供方没有编译期约束，契约变更靠约定与测试，07 篇的契约测试是兜底）、**调试链路变长**（调用经过代理，断点要进 Feign 内部，配合 08 篇的链路日志定位）。

## 2. 声明式调用与负载均衡

kb-ai 调 kb-doc 的标准形态：

```java
@FeignClient(name = "kb-doc")          // name = 服务名，实例由 LoadBalancer 解析
public interface DocClient {
    @GetMapping("/api/docs/{id}")
    Result<DocVO> getDoc(@PathVariable Long id);
}
```

关键点：**`name` 是注册中心的服务名，不是 URL**——这是 Feign 与注册中心集成的入口；**接口签名与提供方 Controller 一致**（路径、参数、返回类型），返回类型用统一 Result（Level2 02 篇的规范在服务间继续生效，错误码体系跨服务统一是排查的基础）。

契约维护的工程实践：**提供方的接口变更必须同步调用方**——Feign 接口没有编译期约束（调用方编译不依赖提供方），改了 kb-doc 的接口签名而 kb-ai 没改，运行期才炸。三个兜底：**接口文档**（提供方接口变更先更新文档）；**契约测试**（07 篇的 Testcontainers 集成测试里，kb-ai 的调用测试跑真 kb-doc，接口不匹配第一时间暴露）；**版本兼容**——新增字段用可空默认值、不删旧字段，保证向后兼容（服务间接口的破坏性变更是发布事故）。负载均衡由底层 **Spring Cloud LoadBalancer**（Ribbon 的继任者）完成：默认轮询策略，Feign 每次调用从 Nacos 实例列表选一个——**负载均衡 + 故障摘除**（04 篇）合起来就是「实例扩缩容无感」的完整闭环。两个扩展点：**自定义负载均衡策略**（如权重策略：按实例健康度/容量分配，`@LoadBalanced` 场景可注入自定义 `ServiceInstanceListSupplier`）；**多实例演示**——kb-doc 起两个实例，观察轮询把请求分到两个实例（验证实验）。

## 3. 超时、重试与熔断集成

Level2 06 篇的超时/重试/熔断心智在服务间调用完整复用，且更必要——**服务间调用是链式的：一个服务慢，下游全部排队**（A 调 B、B 调 C，C 慢则 B 的线程池被打满，A 的也满，雪崩）。Feign 三件套：**超时**——连接 2s、读 5s（服务间调用比外部 AI 调用要短，内部服务 SLA 是毫秒级，超时过长等于容忍慢服务）；**重试**——只对网络类错误与幂等 GET 重试，写操作（POST 创建文档）不重试或必须幂等（Level2 05 篇的幂等键在服务间同样适用）；**熔断**——Feign 集成 Sentinel（`feign.sentinel.enabled=true`），06 篇展开——**微服务化的熔断不是可选项**：没有熔断的链路，一个慢服务就能拖垮整条链（雪崩的传播路径是面试必考）。

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:                      # 全局默认值，个别服务单独覆盖
            connect-timeout: 2000
            read-timeout: 5000
          kb-doc:                       # 按服务粒度覆盖
            read-timeout: 8000          # kb-ai 的检索接口允许更慢
```

超时配置的粒度纪律：**先设全局默认值，再按服务粒度覆盖**——默认值防「忘了配超时的服务裸奔」，覆盖值应对「确实需要更长时间的服务」；没有全局默认值时，每新增一个 Feign 客户端都要记得配超时，漏配就是生产事故——「默认安全 + 例外覆盖」是配置设计的第一原则。

## 4. 链路传递与错误处理

两个工程细节决定服务间调用是否「可用」：**链路 ID 传递**——Level2 08 篇的 traceId 在单体里用 MDC 就够了，微服务化后必须跨服务传递：Feign 的 `RequestInterceptor` 把当前 MDC 的 traceId 塞进请求头，被调方从请求头取出放入自己的 MDC——**这条拦截器是 08 篇「跨服务链路追踪」的前置**，现在不写，08 篇补课成本翻倍：

```java
@Bean
RequestInterceptor traceIdInterceptor() {
    return template -> {
        String traceId = MDC.get("traceId");
        if (traceId != null) template.header("X-Trace-Id", traceId);
    };
}
```

**错误处理契约**——服务间返回统一 Result（错误码 + message），调用方拿到非 0 码按错误码处理（Level2 02 篇的分段错误码跨服务延续）：4xxx 外部依赖错误转降级（Level2 06 篇的 AiGateway 降级逻辑在 kb-ai 内复用）、2xxx 业务错误透传、5xxx 系统错误记录并告警。**服务间的错误不能静默**——Feign 抛的异常要带服务名与接口信息进日志（`@FeignClient` 的 `fallback` 里记录降级原因）。

## 5. 方向：@HttpExchange 与 HTTP Service Clients

2026 的技术方向要讲清：**Spring 官方在推进 `@HttpExchange` + HTTP Service Clients**（Spring 6.1+/SC 2025.x 支持）——用接口 + 注解声明 HTTP 调用，Feign 的声明式理念被官方吸收，OpenFeign 在 Spring Cloud 2025 系列已降级为「兼容适配器」定位。迁移路径也值得了解：`@HttpExchange` 接口通过 `HttpServiceProxyFactory` 生成代理，配合 `@LoadBalanced` 客户端工厂同样能拿到负载均衡能力——**从 Feign 迁移到 @HttpExchange 的改动量是「换注解 + 换代理工厂」，接口签名基本不变**（仓库 OpenFeign 体系的迁移章节有完整对比表）。选型口径：**本项目用 OpenFeign**（SCA 全家桶集成成熟、生态教程多、面试主流），但要知道方向——「@HttpExchange 是官方新方向、Feign 是当前生态主流、两边是同一个『声明式接口』理念」——**知道「当前用什么」和「未来往哪走」是技术判断力的体现**，仓库的 OpenFeign 体系有完整的对比深潜（迁移步骤、差异表、生产建议三章齐全）。

## 6. 验证实验

三个验收实验：**调用实验**——kb-ai 通过 Feign 调 kb-doc 接口成功，抓包（或 Feign 日志）确认请求 URL 是服务名解析出的实例地址；**负载均衡实验**——kb-doc 起两个实例，日志确认轮询分发到两个实例；**降级实验**——停掉 kb-doc 一个实例（另一个保留），连续调用确认 LoadBalancer 自动摘除故障实例（不报错）；**超时实验**——给 kb-doc 的接口加人为延迟（如 sleep 3s），确认 Feign 读超时（5s）与熔断（Sentinel 集成后）生效，错误响应按契约返回。四个实验是 03 篇「服务化可用」的验收证据。

## 7. 核心要点

1. Feign 的本质：声明式接口 = 契约 + 服务名解析 + 内置序列化/错误处理，调用代码量减 80%。
2. `name` 是服务名不是 URL，LoadBalancer 轮询 + 故障摘除构成「扩缩容无感」闭环。
3. 链式调用必须三件套：短超时（连接 2s/读 5s）、只重试幂等请求、熔断（没熔断 = 雪崩放大器）。
4. traceId 的 Feign 拦截器必须现在写——08 篇跨服务链路的前置，漏掉补课成本翻倍。
5. 错误契约：统一 Result 跨服务延续，服务间错误不静默、带服务名进日志。
6. 方向感：OpenFeign 是当前主流、@HttpExchange 是官方新方向，同一个声明式理念。

> 🎯 **核心要点**：Feign 的工程价值是**「把服务间调用的 80% 样板代码变成一行接口」**，但真正的功力在「调用的非功能部分」——超时重试熔断、traceId 传递、错误契约。声明式调用是语法糖，**非功能三件套才是服务间调用的可靠性本体**——面试深挖一定挖到这里。

---

**下一模块**：[06 流量治理：Sentinel](./06-流量治理：Sentinel.md) | **返回总览**：[Level3 总览](./00-Level3%20进阶加分%20总览.md)
