# Prometheus + Grafana 监控体系入门

## 为什么需要

日志告诉你出了问题，监控告诉你"快要出问题了"和"问题在哪"。

监控的三大支柱：
- **Metrics**（指标）— 数字，如 QPS、响应时间、CPU 使用率
- **Logging**（日志）— 文本，如异常堆栈
- **Tracing**（链路追踪）— 一次请求的完整链路

## Prometheus

Prometheus 是时序数据库，定期从目标拉取指标数据（Pull 模式）。

### Spring Boot 接入

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
  metrics:
    tags:
      application: ${spring.application.name}
```

访问 `/actuator/prometheus` 即可看到 Prometheus 格式的指标数据。

### 关键 JVM 指标

| 指标 | 含义 | 告警参考 |
|------|------|----------|
| `jvm_memory_used_bytes` | JVM 已用内存 | > 总内存 80% |
| `jvm_gc_pause_seconds` | GC 暂停时间 | > 500ms |
| `jvm_threads_live` | 活跃线程数 | 持续增长可能是泄漏 |
| `http_server_requests_seconds` | HTTP 请求耗时 | P99 > 1s |
| `hikaricp_connections_active` | 数据库活跃连接数 | > 池大小 80% |

### 自定义指标

```java
@Component
public class OrderMetrics {
    private final Counter orderCounter;
    private final Timer orderTimer;

    public OrderMetrics(MeterRegistry registry) {
        orderCounter = Counter.builder("orders.created.total")
                .description("订单创建总数")
                .register(registry);
        orderTimer = Timer.builder("orders.create.duration")
                .description("订单创建耗时")
                .register(registry);
    }

    public void recordOrder() {
        orderCounter.increment();
        orderTimer.record(() -> { /* 业务 */ });
    }
}
```

## Grafana

Grafana 负责可视化。从 Prometheus 拉取数据，以图表/仪表盘展示。

**常用 Dashboard（Grafana 官网上搜 ID 直接导入）：**
- JVM 监控：Dashboard ID `4701`
- Spring Boot 监控：Dashboard ID `10280`

## 告警规则示例

```yaml
groups:
  - name: spring-boot
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 1
        annotations:
          summary: "5xx 错误率超过 1/秒"
```

## ELK 补充

对于日志层面，ELK（Elasticsearch + Logstash + Kibana）是标准栈：
- **Logstash**：收集、解析、转发日志
- **Elasticsearch**：存储和搜索日志
- **Kibana**：日志可视化、查询、Dashboard

一般项目中用 Filebeat 替代 Logstash（更轻量），Spring Boot 输出 JSON 格式日志到文件，Filebeat 采集发送到 ES。

## 实战建议

作为 Java 后端，你需要会：
1. **接入 Micrometer + Prometheus**：Spring Boot 几分钟搞定
2. **看懂 Grafana 面板**：JVM 内存、GC、QPS、错误率、P99 延迟
3. **配置关键告警**：错误率飙升、内存 > 80%、接口 P99 暴涨
4. **线上排查套路**：Grafana 看到异常 → Kibana 搜日志 → 定位问题

不需要自己去搭 Prometheus + Grafana 集群，那是运维的事。但要会用、会看、会配指标。
