# 05 - MDC 链路追踪

> **核心摘要**：MDC（Mapped Diagnostic Context）让日志带上链路上下文——traceId 贯穿一次请求的所有日志。2026 年实践：**MDC 注入 traceId + 跨服务传递 + 线程池上下文传递（TTL）**。本文覆盖 MDC 原理、链路设计、线程池断裂的五大场景与解决方案。

> **前置阅读**：[[03-Logback深度实践]]、[[04-日志级别与最佳实践]]

---

## 📚 目录

1. [MDC 的原理](#1-mdc-的原理)
2. [traceId 链路设计](#2-traceid-链路设计)
3. [请求入口注入](#3-请求入口注入)
4. [线程池断裂：五大场景](#4-线程池断裂五大场景)
5. [解决方案：快照恢复](#5-解决方案快照恢复)
6. [TTL：TransmittableThreadLocal](#6-ttltransmittablethreadlocal)
7. [跨服务传递](#7-跨服务传递)
8. [Kibana 中的链路查询](#8-kibana-中的链路查询)
9. [核心要点](#9-核心要点)

---

## 1. MDC 的原理

> **背景**：日志需要「上下文」——同一次请求的日志要能串联（traceId）、带业务标识（userId）。
> **目的**：理解 MDC 的机制与使用规范。
> **适用范围**：日志上下文注入（请求级/业务级）。
> **前提假设**：MDC 基于 ThreadLocal——**线程内有效，跨线程需传递**（核心难点）。

```text
MDC（Mapped Diagnostic Context）
├── ① 本质：基于 ThreadLocal 的 Map（线程级上下文）
├── ② 能力：put/get/remove/clear
│   ├── MDC.put("traceId", "abc123")
│   ├── MDC.get("traceId")
│   └── MDC.clear()（finally 必调！）
├── ③ 输出：日志格式 %X{traceId}（Logback/Log4j2）
├── ④ 生命周期：线程创建 → 注入 → 使用 → **清理**
└── ⑤ 金句：MDC = 「日志的线程级随身包」——请求进来挂上，处理完卸下

使用铁律
├── ① 入口注入：请求进入时 put
├── ② finally 清理：处理完必须 clear（线程复用污染！）
├── ③ 不清理的后果：
│   ├── 线程池复用 → 下一个请求继承了旧 traceId
│   └── 日志串线（A 请求的日志带 B 的 traceId）
```

---

## 2. traceId 链路设计

### 2.1 traceId 是什么

```text
traceId（链路追踪 ID）
├── ① 一次请求的唯一标识（全局）
├── ② 生成：UUID / Snowflake（有状态可排序）
├── ③ 传递：请求进入时生成 → 贯穿所有日志
├── ④ 串联：Kibana 按 traceId 搜 → 一次请求的所有日志
└── ⑤ 对比：
    ├── traceId：一次请求（全局唯一）
    └── spanId：一次调用（服务间一跳）

设计要点
├── ① 入口生成：网关/Filter（第一个服务）
├── ② 无则生成、有则透传（跨服务）
├── ③ 日志输出：%X{traceId:-}（默认空不报错）
└── ④ 关联指标：traceId 关联日志与监控（三支柱）
```

### 2.2 链路结构

```text
一次请求的链路（traceId 贯穿）
┌────────┐  traceId=abc  ┌────────┐  traceId=abc  ┌────────┐
│ 网关    │ ───────────→ │ 订单服务 │ ───────────→ │ 支付服务 │
│ 生成 abc│              │ 透传 abc│              │ 透传 abc│
└────────┘              └────────┘              └────────┘
   日志全带 [traceId=abc] → Kibana 搜 abc 串联全部
```

---

## 3. 请求入口注入

### 3.1 Filter 实现（标准做法）

```java
@Component
public class TraceFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        try {
            // ① 有则透传（跨服务）、无则生成
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            MDC.put("traceId", traceId);

            // ② 业务标识（可选）
            MDC.put("userId", extractUserId(request));

            chain.doFilter(req, res);
        } finally {
            // ③ 清理（铁律！）
            MDC.clear();
        }
    }
}
```

### 3.2 异步入口（MQ/定时任务）

```java
// MQ 消费入口（消息带 traceId）
@Component
public class OrderListener {
    @KafkaListener(topics = "order-events")
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            // 消息头带 traceId（生产端注入）
            String traceId = new String(record.headers().lastHeader("traceId").value());
            MDC.put("traceId", traceId);
            // 处理消息...
        } finally {
            MDC.clear();
        }
    }
}

// 定时任务入口
public void scheduledTask() {
    try {
        MDC.put("traceId", UUID.randomUUID().toString());
        // 任务逻辑...
    } finally {
        MDC.clear();
    }
}
```

---

## 4. 线程池断裂：五大场景

> ⚠️ **核心痛点**：MDC 基于 ThreadLocal——**线程池复用线程时不传递上下文**。子线程日志丢失 traceId（或带上旧值）。

```text
五大断裂场景（2026 高频）
├── ① ThreadPoolExecutor.submit()：
│   ├── 任务在新线程/复用线程执行
│   └── MDC 不传递 → 子任务日志无 traceId
├── ② Spring @Async：
│   ├── 异步方法在不同线程执行
│   └── 日志无 traceId
├── ③ ForkJoinPool：
│   ├── 并行流/递归任务
│   └── 上下文不继承
├── ④ Reactor/WebFlux：
│   ├── Schedulers.boundedElastic() 等
│   └── 响应式线程切换丢失
└── ⑤ Kafka 监听容器：
    ├── 消费线程与业务线程分离
    └── 消息处理链断裂

断裂的症状
├── ① 子任务日志 traceId 为空（[traceId=]）
├── ② 或带了旧请求的 traceId（串线）
└── ③ Kibana 搜 traceId 缺子任务日志
```

---

## 5. 解决方案：快照恢复

### 5.1 手动快照-恢复-清理

```java
// 方案 1：装饰 Runnable（通用）
public class MdcRunnable implements Runnable {
    private final Runnable task;
    private final Map<String, String> context;

    public MdcRunnable(Runnable task) {
        this.task = task;
        this.context = MDC.getCopyOfContextMap();  // ① 快照
    }

    @Override
    public void run() {
        try {
            MDC.setContextMap(context);            // ② 恢复
            task.run();
        } finally {
            MDC.clear();                           // ③ 清理
        }
    }
}

// 使用：提交时包装
executor.submit(new MdcRunnable(() -> {
    log.info("子任务执行（traceId 已恢复）");
}));
```

### 5.2 Spring 的 MdcTaskDecorator

```java
// 方案 2：Spring TaskDecorator（全局生效）
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(new MdcTaskDecorator());  // 装饰器
        executor.initialize();
        return executor;
    }

    static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    MDC.setContextMap(context);
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}
```

---

## 6. TTL：TransmittableThreadLocal

### 6.1 什么是 TTL

> **背景**：阿里开源的 TransmittableThreadLocal（TTL）——解决线程池上下文传递的通用方案（不只日志，还有事务/安全上下文）。
> **目的**：透明传递上下文（无需手动包装）。
> **适用范围**：线程池/异步/跨线程的场景。
> **不适用场景**：少量异步（手动快照足够）。

```xml
<!-- 依赖 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>transmittable-thread-local</artifactId>
    <version>2.14.x</version>
</dependency>
```

### 6.2 使用方式

```java
// ① 替换 ThreadLocal 为 TTL
TransmittableThreadLocal<String> traceId = new TransmittableThreadLocal<>();

// ② 线程池包装（透明传递）
ExecutorService executor = TtlExecutors.getTtlExecutorService(
    Executors.newFixedThreadPool(4));
// 提交任务时：上下文自动传递（无需手动快照）

// ③ 或 Agent 方式（Java Agent 无侵入）
// JVM 参数：-javaagent:transmittable-thread-local.jar
// 效果：无需改代码（高级用法）

// 使用示例
MDC.put("traceId", "abc123");      // 前提：LogbackMDCAdapter 支持 TTL
executor.submit(() -> {
    // 子线程自动继承 traceId（TTL 透明传递）
    log.info("子任务（traceId 自动传递）");
});
```

> 🎯 **选型**：少量异步 → 手动快照（MdcTaskDecorator）；大量异步/复杂链路 → TTL（透明传递）。TTL 是 2026 分布式系统的标准选择（配合 MDC 可无缝工作）。

---

## 7. 跨服务传递

### 7.1 HTTP 传递

```java
// 服务 A 调用服务 B（RestTemplate/OpenFeign 拦截器）
@Component
public class TraceInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpRequestInterceptor intercept(
            HttpRequest request, byte[] body, ...) {
        // 透传 traceId（MDC 中的当前值）
        request.getHeaders().set("X-Trace-Id", MDC.get("traceId"));
        return ...;
    }
}
// 服务 B：Filter 读取 X-Trace-Id（有则透传、无则生成）
// → 全链路同一个 traceId
```

### 7.2 消息传递

```java
// 生产者：消息头带 traceId
Message msg = MessageBuilder.withPayload(payload)
    .setHeader("traceId", MDC.get("traceId"))
    .build();
kafkaTemplate.send("order-events", msg);

// 消费者：读消息头恢复（见第 3 节）
```

```text
跨服务传递要点
├── ① HTTP：自定义头（X-Trace-Id）或标准（W3C traceparent）
├── ② MQ：消息头传递（headers）
├── ③ 无则生成：接收方缺失时生成（链路不丢）
├── ④ 有则透传：优先使用传入值
└── ⑤ 金句：traceId 的传递 = 「无则生成、有则透传」
```

---

## 8. Kibana 中的链路查询

```text
Kibana 链路查询（2026）
├── ① 搜索 traceId：Kibana → Discover → 输入 traceId
├── ② 结果：一次请求的全部日志（时间排序）
├── ③ 关联：日志 + 指标（traceId 关联）
├── ④ 视图：时间线展示（请求 → 各服务日志）
└── ⑤ 金句：traceId 是「日志的索引键」——搜到它 = 看到全部

排查流程（用 traceId）
├── ① 用户报障 → 拿请求时间/订单号
├── ② 搜订单号 → 找到 traceId
├── ③ 搜 traceId → 全部日志
├── ④ 按时间线看：哪一步出错
└── ⑤ 定位根因 → 修复
```

---

## 9. 核心要点

> 🎯 **核心要点**：
> 1. MDC = ThreadLocal 的 Map——线程内有效，**跨线程需传递**（核心难点）
> 2. 使用铁律：入口注入 + finally 清理（不清理 = 线程池串线）
> 3. traceId 传递原则：**无则生成、有则透传**——HTTP 头/MQ 消息头
> 4. 五大断裂场景：线程池/@Async/ForkJoinPool/Reactor/Kafka 容器
> 5. 三种解法：手动快照（少量）/MdcTaskDecorator（Spring 全局）/**TTL（大量异步——2026 标准）**
> 6. Kibana 按 traceId 串联——搜到 traceId = 看到一次请求的全部

---

**下一模块**：[06-结构化日志](06-结构化日志.md) | **返回总览**：[00-日志Log知识体系总览](00-日志Log知识体系总览.md)
