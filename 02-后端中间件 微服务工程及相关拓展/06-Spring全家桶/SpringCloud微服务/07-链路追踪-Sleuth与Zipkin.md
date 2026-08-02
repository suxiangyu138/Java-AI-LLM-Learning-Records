# 07 - 链路追踪：Micrometer Tracing 与 Zipkin

> 🎯 微服务中一个请求跨 N 个服务 — 没有链路追踪就像蒙眼开车。TraceId 贯穿全链路、Zipkin 可视化调用链、SkyWalking 零侵入 APM

---

## 目录

1. [链路追踪概述](#1-链路追踪概述)
2. [Micrometer Tracing 集成](#2-micrometer-tracing-集成)
3. [Zipkin 部署与使用](#3-zipkin-部署与使用)
4. [TraceId 全链路透传](#4-traceid-全链路透传)
5. [SkyWalking 简介](#5-skywalking-简介)

---

## 1. 链路追踪概述

```text
一次请求跨 4 个服务：

  Gateway → Order Service → User Service → Database
               │
               └── Inventory Service → Redis

Trace: 整个请求的完整调用链（一个 TraceId 贯穿全局）
Span:  Trace 中的一个节点（每次服务调用是一个 Span）

结构：
  Trace: abc123
    ├── Span-1: Gateway (10ms)
    ├── Span-2: OrderService (100ms)
    │   ├── Span-3: UserService (80ms)
    │   │   └── Span-4: MySQL (50ms)
    │   └── Span-5: InventoryService (15ms)
    └── Span-end
```

---

## 2. Micrometer Tracing 集成

> Spring Boot 3 使用 Micrometer Tracing 替代 Sleuth。

```xml
<!-- Micrometer Tracing + Brave(Zipkin) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
spring:
  application:
    name: user-service
management:
  tracing:
    sampling:
      probability: 1.0            # 采样率 100%（开发）/ 0.1（生产）
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

```text
日志输出变为：
  INFO [user-service,abc123def456,abc123def456] 收到请求 ...

  [服务名, TraceId, SpanId]
```

---

## 3. Zipkin 部署与使用

```bash
docker run -d --name zipkin -p 9411:9411 \
  openzipkin/zipkin:3.0
# 访问 http://localhost:9411
```

### Zipkin 控制台功能

```
1. 按服务名查询调用链
2. 按 TraceId 精确搜索
3. 查看每个 Span 的耗时、标签、异常
4. 依赖拓扑图（服务间调用关系）
```

---

## 4. TraceId 全链路透传

### 4.1 线程池透传

```java
// ⚠️ @Async 会切换线程，TraceId 会丢失！需要手动透传

// 方式1：使用 LazyTraceExecutor 包装
@Bean
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    return new LazyTraceExecutor(beatContext, executor);  // ← 自动透传
}

// 方式2：手动透传（Hystrix/自定义线程池）
TraceContext context = tracer.currentTraceContext().get();
executorService.submit(() -> {
    try (Tracer.SpanInScope ws = tracer.withSpanInScope(context)) {
        // 业务逻辑 — TraceId 正确传递
    }
});
```

### 4.2 MQ 透传

```java
// 发送消息时带上 TraceId
String traceId = tracer.currentSpan().context().traceId();
message.setProperty("traceId", traceId);
kafkaTemplate.send("order-topic", message);

// 消费消息时恢复 TraceId
@KafkaListener(topics = "order-topic")
public void handle(String msg) {
    String traceId = ...;  // 从消息头获取
    Span span = tracer.spanBuilder()
        .setParent(tracer.traceContextBuilder()
            .traceId(traceId).build())
        .start();
    try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
        // 业务 — 正确关联到原始 Trace
    } finally { span.end(); }
}
```

---

## 5. SkyWalking 简介

> SkyWalking 是 Apache 的 APM 工具，**Java Agent 零侵入**，功能比 Zipkin 更强。

| 维度 | Zipkin | SkyWalking |
|------|--------|-----------|
| 侵入性 | 需引入依赖 | ⭐ Agent 零侵入 |
| 安装 | 简单（单容器） | 较重（OAP + ES + UI） |
| 功能 | 链路追踪 | APM（追踪+指标+拓扑+告警） |
| 存储 | 内存/ES/MySQL | ES/H2/MySQL |
| 适用 | 开发/测试/小规模 | ⭐ 生产/大规模 |

```bash
# SkyWalking 快速启动（Docker）
docker run -d --name skywalking-oap \
  -e SW_STORAGE=h2 \
  apache/skywalking-oap-server:9.7.0

docker run -d --name skywalking-ui -p 8080:8080 \
  -e SW_OAP_ADDRESS=http://skywalking-oap:12800 \
  apache/skywalking-ui:9.7.0

# Java 应用启动时加 Agent
java -javaagent:/path/to/skywalking-agent.jar \
  -DSW_AGENT_NAME=user-service \
  -jar user-service.jar
```

> 🎯 **选型**：开发测试用 Micrometer Tracing + Zipkin（轻量简单）；生产环境上 SkyWalking（零侵入 APM + 指标 + 拓扑）。两者不冲突，可并存。
