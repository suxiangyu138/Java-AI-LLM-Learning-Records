# 04-JVM 监控指标与 Micrometer
> JVM 到底该看哪些指标：内存/GC/线程/类加载指标体系、Micrometer 门面、Spring Boot Actuator 暴露——从"指标一堆"到"指标有用"

## 📚 目录
1. [Micrometer：指标门面](#1-micrometer指标门面)
2. [JVM 指标四大族](#2-jvm-指标四大族)
3. [内存指标解读](#3-内存指标解读)
4. [GC 指标解读](#4-gc-指标解读)
5. [线程与类加载指标](#5-线程与类加载指标)
6. [Spring Boot Actuator 暴露](#6-spring-boot-actuator-暴露)
7. [常用告警指标与阈值](#7-常用告警指标与阈值)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. Micrometer：指标门面

```text
业务代码 → Micrometer API（Counter/Timer/Gauge...）
              │
              └─→ 注册表 → Prometheus / Datadog / InfluxDB...
```

| 概念 | 说明 | 类比 |
|------|------|------|
| Meter | 指标抽象 | 日志的 Logger |
| Counter | 单调递增计数 | 请求数、错误数 |
| Gauge | 当前值 | 线程数、内存使用 |
| Timer | 耗时分布 | 接口耗时 p99 |
| DistributionSummary | 值分布 | 消息大小分布 |

```java
// 业务埋点示例
@RestController
public class OrderController {

    private final Counter orderCounter;

    public OrderController(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
                .tag("channel", "web")          // 标签（低基数！）
                .register(registry);
    }

    @PostMapping("/orders")
    public void create() {
        orderCounter.increment();
        // 计时：Timer.Sample
        Timer.Sample sample = Timer.start(registry);
        // ... 业务
        sample.stop(Timer.builder("orders.create.time")
                .tag("channel", "web")
                .register(registry));
    }
}
```

> ⚠️ **标签纪律**（与 Loki 同理）：标签只用低基数维度（接口名/渠道/错误类型）——**用户 ID/订单 ID 进标签 = 指标爆炸**。

## 2. JVM 指标四大族

| 指标族 | 前缀 | 回答的问题 |
|--------|------|-----------|
| 内存 | `jvm_memory_*` | 堆/非堆用了多少？快 OOM 了吗？ |
| GC | `jvm_gc_*` | GC 频繁吗？停顿多久？ |
| 线程 | `jvm_threads_*` | 线程数健康吗？死锁？ |
| 类加载 | `jvm_classes_*` | 类加载异常？（泄漏信号） |

```text
Spring Boot Actuator 默认暴露的 JVM 指标（jvm.* 前缀）：
jvm_memory_used_bytes          # 已用内存
jvm_memory_max_bytes           # 最大内存
jvm_gc_pause_seconds           # GC 停顿（Timer）
jvm_gc_live_data_size_bytes    # GC 后存活数据（老年代容量信号）
jvm_threads_live_threads       # 存活线程数
jvm_threads_states_threads     # 按状态分线程数
jvm_classes_loaded_classes     # 已加载类数
```

## 3. 内存指标解读

### 3.1 堆内存（Heap）

| 指标 | 含义 | 异常信号 |
|------|------|---------|
| `jvm_memory_used_bytes{area="heap"}` | 当前已用堆 | 持续高位 + 频繁 Full GC |
| `jvm_memory_max_bytes{area="heap"}` | 堆上限（-Xmx） | - |
| `jvm_memory_committed_bytes` | 已提交内存 | 接近 max = 需要扩容 |

### 3.2 非堆与元空间

```text
非堆（Non-Heap）：
  元空间（Metaspace）——类元数据，-XX:MaxMetaspaceSize 限制
  代码缓存、压缩类空间等

⚠️ 元空间泄漏信号：jvm_memory_used_bytes{area="nonheap"} 持续增长
  常见原因：动态生成类（CGLIB/反射/热部署）未回收
```

### 3.3 内存排查路径

```text
指标发现（used 持续高位）→ 堆转储（jmap -dump）→ MAT 分析
联动：jvm_gc_live_data_size_bytes 持续增长 = 老年代对象泄漏嫌疑
```

## 4. GC 指标解读

| 指标 | 含义 | 健康标准 |
|------|------|---------|
| `jvm_gc_pause_seconds` | GC 停顿时间（Timer：count/sum/max） | **p99 < 100ms**（G1/ZGC 目标） |
| `jvm_gc_pause_seconds_count` | GC 次数 | 频繁 = 内存压力 |
| `jvm_gc_live_data_size_bytes` | GC 后存活对象大小 | 持续增长 = 泄漏信号 |
| `jvm_gc_max_data_size_bytes` | 老年代容量上限 | - |

```text
GC 健康三问：
  ① 停顿多久？（p99 停顿 < 100ms 是 G1/ZGC 目标）
  ② 频繁吗？（每分钟 Full GC > 1 次 = 内存配置问题）
  ③ 存活数据在增长吗？（持续增长 = 泄漏，不是配置问题）
```

| 现象 | 结论 |
|------|------|
| Young GC 频繁但回收快 | 年轻代偏小 |
| Full GC 频繁 | 堆不足 / 泄漏 |
| GC 后 live data 持续增长 | **内存泄漏**（排查代码） |
| GC 停顿突增 | 大对象分配 / 堆碎片 |

## 5. 线程与类加载指标

### 5.1 线程指标

| 指标 | 含义 | 异常信号 |
|------|------|---------|
| `jvm_threads_live_threads` | 存活线程数 | 持续增长 = 线程泄漏 |
| `jvm_threads_states_threads{state="blocked"}` | 阻塞线程 | 持续 > 0 = 锁竞争 |
| `jvm_threads_states_threads{state="runnable"}` | 运行线程 | 峰值对比线程池配置 |
| `jvm_threads_peak_threads` | 峰值线程数 | 对比 max 配置 |

### 5.2 类加载指标

| 指标 | 含义 | 异常信号 |
|------|------|---------|
| `jvm_classes_loaded_classes` | 已加载类数 | 持续增长 = 类泄漏（动态代理/热部署） |
| `jvm_classes_unloaded_classes` | 已卸载类数 | 长期为 0 + loaded 增长 = 泄漏 |

> 💡 类加载泄漏是"看不见的泄漏"：动态生成类（CGLIB/反射/每次请求 new classloader）不回收时，Metaspace 缓慢上涨直到 OOM。

## 6. Spring Boot Actuator 暴露

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    metrics:
      enabled: true
  prometheus:
    metrics:
      export:
        enabled: true
```

```xml
<!-- 依赖：micrometer-registry-prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```bash
# 验证
curl localhost:8080/actuator/prometheus | grep jvm_memory_used
```

> 💡 Prometheus 采集配置（scrape 配置、ServiceMonitor）见同级 `11-云原生/Prometheus+Grafana 监控告警/07-Spring-Boot应用监控.md`——本体系聚焦"看哪些指标"。

## 7. 常用告警指标与阈值

| 告警 | 指标表达式（PromQL） | 建议阈值 |
|------|---------------------|---------|
| 堆使用率高 | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes` | > 85% 持续 5 分钟 |
| 元空间增长 | `jvm_memory_used_bytes{area="nonheap"}` 增长率 | 连续 24h 增长 |
| Full GC 频繁 | `rate(jvm_gc_pause_seconds_count{action="end of major GC"}[5m])` | > 1/分钟 |
| GC 停顿过长 | `max(jvm_gc_pause_seconds_max)` | p99 > 500ms |
| 线程泄漏 | `jvm_threads_live_threads` 趋势 | 持续增长超基线 50% |
| 类泄漏 | `jvm_classes_loaded_classes` 趋势 | 持续增长无回落 |
| 线程阻塞 | `jvm_threads_states_threads{state="blocked"}` | 持续 > 10 |

> 🎯 **告警不是指标越多越好**：JVM 核心就 6 条（堆、GC 停顿、GC 次数、存活数据、线程、类加载）——**先覆盖这 6 条，再按业务扩展**。

## 8. 核心要点

> 🎯 **核心要点**：
> - Micrometer = 指标门面（类比 SLF4J）：Counter/Gauge/Timer 四类 Meter；
> - JVM 四大族：内存/GC/线程/类加载——各 3-4 个核心指标足够；
> - 内存三信号：used 高位（压力）、live data 增长（泄漏）、Metaspace 增长（类泄漏）；
> - GC 三问：停顿多久（<100ms）、频繁吗（<1 Full GC/分钟）、存活数据涨吗（泄漏）;
> - 线程两信号：live 增长（线程泄漏）、blocked 持续（锁竞争）；
> - 标签纪律：低基数维度才进标签（与 Loki 同源纪律）；
> - Actuator 三件套：exposure 配置 + prometheus 依赖 + curl 验证。

## 9. 参考来源

- [Micrometer 官方文档](https://docs.micrometer.io/micrometer/reference/)
- [Spring Boot Actuator 指标文档](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [JVM 指标最佳实践（Prometheus 生态）](https://prometheus.io/docs/practices/)

---

**下一模块**：[05-告警体系与SLO](05-告警体系与SLO.md)　/　**返回总览**：[00-总览](00-日志监控指标总览.md)
