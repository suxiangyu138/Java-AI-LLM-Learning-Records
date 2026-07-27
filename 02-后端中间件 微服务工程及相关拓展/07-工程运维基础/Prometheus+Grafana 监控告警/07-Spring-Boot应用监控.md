# 07 - Spring Boot 应用监控

> 🎯 Micrometer + Actuator 是 Spring Boot 接入 Prometheus 的标准路径 — 从 JVM 指标到 HTTP 延迟、从连接池到自定义业务 Metrics，构建 Java 应用完整的可观测性

---

## 目录

1. [Micrometer 概述](#1-micrometer-概述)
2. [Spring Boot 接入配置](#2-spring-boot-接入配置)
3. [JVM 核心指标解读](#3-jvm-核心指标解读)
4. [HTTP 请求监控](#4-http-请求监控)
5. [连接池监控（HikariCP）](#5-连接池监控hikaricp)
6. [自定义业务 Metrics](#6-自定义业务-metrics)
7. [关键告警规则](#7-关键告警规则)

---

## 1. Micrometer 概述

Micrometer 是 JVM 应用的指标门面（类比 SLF4J），提供统一的 Metrics API，对接 Prometheus、Datadog、InfluxDB 等多种后端。

```
Spring Boot App
    │
Micrometer（指标门面）
    │
┌───┼───┬──────────┬──────────┐
│   │   │          │          │
Prometheus Datadog InfluxDB Graphite
```

| 特性 | 说明 |
|------|------|
| **JVM 指标** | 自动采集堆内存/GC/线程/类加载 |
| **HTTP 指标** | 自动采集请求数/延迟/状态码分布 |
| **连接池** | HikariCP/Tomcat JDBC 连接池指标 |
| **自定义** | `@Timed`、`@Counted`、`Timer`、`Counter`、`Gauge` |
| **多后端** | 切换 Prometheus → Datadog 只需改依赖 |

---

## 2. Spring Boot 接入配置

### 2.1 Maven 依赖

```xml
<!-- Spring Boot 3.x -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2.2 application.yml

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
      base-path: /actuator
  metrics:
    tags:
      application: ${spring.application.name}
      env: ${spring.profiles.active}
    distribution:
      percentiles-histogram:
        http.server.requests: true        # 启用 HTTP 请求 Histogram
      slo:
        http.server.requests: 10ms,50ms,100ms,500ms,1s,2s,5s,10s
  endpoint:
    prometheus:
      enabled: true
    health:
      show-details: always
```

### 2.3 Prometheus 采集配置

```yaml
scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
    static_configs:
      - targets:
          - '10.0.1.10:8080'
          - '10.0.1.11:8080'
        labels:
          app: 'user-service'
```

---

## 3. JVM 核心指标解读

### 3.1 内存

| 指标 | 说明 | 告警阈值 |
|------|------|:---:|
| `jvm_memory_used_bytes{area="heap"}` | 堆内存已用 | — |
| `jvm_memory_max_bytes{area="heap"}` | 堆内存最大值 (-Xmx) | — |
| `jvm_memory_used_bytes{area="nonheap"}` | 非堆已用（Metaspace等） | — |
| `jvm_memory_committed_bytes` | 已提交内存 | — |

```promql
# 堆内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### 3.2 GC

| 指标 | 说明 |
|------|------|
| `jvm_gc_pause_seconds_count` | GC 次数 |
| `jvm_gc_pause_seconds_sum` | GC 总暂停时间 |
| `jvm_gc_memory_allocated_bytes_total` | GC 后释放的总内存 |

```promql
# GC 频率（次/秒）
rate(jvm_gc_pause_seconds_count[5m])

# 平均 GC 暂停时间
rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])

# GC 吞吐率（应用运行时间占比）
1 - rate(jvm_gc_pause_seconds_sum[5m])
```

### 3.3 线程

| 指标 | 说明 |
|------|------|
| `jvm_threads_live_threads` | 当前活跃线程数 |
| `jvm_threads_daemon_threads` | 守护线程数 |
| `jvm_threads_peak_threads` | 历史峰值线程数 |
| `jvm_threads_states_threads{state}` | 各状态线程数 |

```promql
# 线程状态分布
jvm_threads_states_threads

# BLOCKED 线程数（死锁风险）
jvm_threads_states_threads{state="blocked"}
```

---

## 4. HTTP 请求监控

### 核心指标

| 指标 | 说明 |
|------|------|
| `http_server_requests_seconds_count` | HTTP 请求总数（Counter） |
| `http_server_requests_seconds_sum` | 总耗时（秒） |
| `http_server_requests_seconds_bucket` | 延迟分布桶（Histogram） |
| `http_server_requests_seconds_max` | 最大延迟 |

```promql
# ═══ QPS ═══
sum(rate(http_server_requests_seconds_count{application="user-service"}[5m]))

# ═══ 错误率 ═══
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count[5m])) * 100

# ═══ P99 延迟 ═══
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# ═══ 按接口分组的 QPS Top 5 ═══
topk(5, sum(rate(http_server_requests_seconds_count[5m])) by (uri))

# ═══ 慢接口（P99 > 1s） ═══
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)) > 1
```

---

## 5. 连接池监控（HikariCP）

Spring Boot 2.x/3.x 默认使用 HikariCP，自动暴露以下指标：

| 指标 | 说明 |
|------|------|
| `hikaricp_connections_active` | 活跃连接数 |
| `hikaricp_connections_idle` | 空闲连接数 |
| `hikaricp_connections_pending` | 等待获取连接的线程数 |
| `hikaricp_connections_max` | 最大连接数 |
| `hikaricp_connections_timeout_total` | 获取连接超时次数 |
| `hikaricp_connections_creation_seconds` | 连接创建耗时 |

```promql
# 连接池使用率
hikaricp_connections_active / hikaricp_connections_max * 100

# 等待连接数（> 0 说明连接池不够用）
hikaricp_connections_pending

# 连接超时次数（> 0 需要排查）
rate(hikaricp_connections_timeout_total[5m])
```

---

## 6. 自定义业务 Metrics

### 6.1 @Timed（方法级计时）

```java
@RestController
public class OrderController {

    @Timed(value = "orders.create", histogram = true,
           extraTags = {"module", "order"})
    @PostMapping("/api/orders")
    public Order createOrder(@RequestBody OrderRequest req) {
        return orderService.create(req);
    }
}
```

### 6.2 Counter（计数）

```java
@Component
public class OrderMetrics {

    private final Counter orderCounter;
    private final Counter failedOrderCounter;

    public OrderMetrics(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders_total")
            .description("Total orders")
            .tag("type", "created")
            .register(registry);
        this.failedOrderCounter = Counter.builder("orders_total")
            .tag("type", "failed")
            .register(registry);
    }

    public void incrementCreated() { orderCounter.increment(); }
    public void incrementFailed() { failedOrderCounter.increment(); }
}
```

### 6.3 Gauge（瞬时值）

```java
@Component
public class QueueMetrics {

    private final BlockingQueue<Task> taskQueue;

    public QueueMetrics(MeterRegistry registry, BlockingQueue<Task> taskQueue) {
        this.taskQueue = taskQueue;
        Gauge.builder("task_queue_size", taskQueue, BlockingQueue::size)
            .description("Task queue size")
            .register(registry);
    }
}
```

### 6.4 Timer（耗时+计数）

```java
public class PaymentService {
    private final Timer paymentTimer;

    public PaymentService(MeterRegistry registry) {
        this.paymentTimer = Timer.builder("payment_duration")
            .description("Payment processing duration")
            .publishPercentileHistogram(true)    // 启用 Histogram
            .register(registry);
    }

    public PaymentResult pay(PaymentRequest req) {
        return paymentTimer.record(() -> {
            // 业务逻辑
            return doPay(req);
        });
    }
}
```

---

## 7. 关键告警规则

```yaml
# rules/spring-boot-app.yml
groups:
  - name: spring-boot-alerts
    rules:
      # ═══ 应用宕机 ═══
      - alert: ApplicationDown
        expr: up{job="spring-boot"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "应用 {{ $labels.app }} 宕机"

      # ═══ JVM 堆内存 ═══
      - alert: HighHeapUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100 > 85
        for: 5m
        labels:
          severity: warning

      # ═══ GC 频繁 ═══
      - alert: FrequentGC
        expr: rate(jvm_gc_pause_seconds_count[5m]) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.app }} GC 频率 > 5次/秒"

      # ═══ 高错误率 ═══
      - alert: HighErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
              / sum(rate(http_server_requests_seconds_count[5m])) * 100 > 5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.app }} 5xx 错误率 > 5%"

      # ═══ P99 延迟高 ═══
      - alert: HighP99Latency
        expr: histogram_quantile(0.99,
                sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.app }} P99 延迟 > 2s"

      # ═══ 连接池耗尽 ═══
      - alert: ConnectionPoolExhausted
        expr: hikaricp_connections_pending > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.app }} HikariCP 连接池有等待线程"
```

> 🎯 **关键思路**：基础设施监控（Node Exporter）+ 应用监控（Spring Boot Actuator）+ 中间件监控（Redis/MySQL Exporter）= 三层可观测性。从下往上建设，逐层覆盖。
