# 06-可观测性三支柱与 OpenTelemetry
> 指标、日志、链路单独使用都不足以定位问题——三支柱关联排查 + OpenTelemetry 统一标准 + LGTM 实践

## 📚 目录
1. [三支柱：指标/日志/链路](#1-三支柱指标日志链路)
2. [三支柱关联机制](#2-三支柱关联机制)
3. [OpenTelemetry：统一标准](#3-opentelemetry统一标准)
4. [Java 应用的 OTel 接入](#4-java-应用的-otel-接入)
5. [LGTM 实践：统一查询入口](#5-lgtm-实践统一查询入口)
6. [采样策略](#6-采样策略)
7. [三支柱排查实战](#7-三支柱排查实战)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. 三支柱：指标/日志/链路

| 支柱 | 回答的问题 | 工具（2026） | 局限 |
|------|-----------|-------------|------|
| **Metrics（指标）** | 出事了？（异常检测） | Prometheus + Grafana | 有现象无细节 |
| **Logs（日志）** | 出了什么事？（现场） | Loki（轻量）/ EFK（全文） | 有现场难串联 |
| **Traces（链路）** | 在哪出的？（路径） | Tempo / Jaeger | 有路径无全貌 |

> 🎯 **核心认知**：三支柱是"三维一体的同一事件"——单独任何一个都不足以定位问题，**组合起来才是可观测性**。典型路径：指标告警发现异常 → 日志查看现场 → 链路定位瓶颈。

## 2. 三支柱关联机制

### 2.1 关联键：traceId

```text
traceId 贯穿三支柱：
  指标：请求标签（可选，低基数采样）
  日志：MDC 注入 %X{traceId}（见 02）
  链路：Span 的 traceId（OTel 生成）

排查：日志中看到 traceId → 在 Tempo 中查该链路全貌
```

### 2.2 关联流程

```text
① Grafana 指标面板：order-service 错误率飙升
② 点击跳转 Loki：{service="order-service"} |= "ERROR"（同时间窗）
③ 日志中的 traceId → 点击跳转 Tempo：完整调用链
④ 链路中看到：下游 payment-service 超时 → 回到 payment 日志深挖
⑤ 根因：数据库连接池耗尽 → 修复 → 指标恢复
```

> 💡 2026 标准做法：**Grafana 一个界面统一查询三支柱**（LGTM），日志中的 traceId 可点击跳转链路——"关联"是产品化的，不是手动的。

## 3. OpenTelemetry：统一标准

### 3.1 为什么需要

```text
过去：每个厂商一套 SDK（Prometheus 埋点 / Jaeger 埋点 / 日志格式各不相同）
问题：厂商锁定、重复埋点、切换成本高

OTel（OpenTelemetry，CNCF 毕业项目）：
  统一 API/SDK → 一次埋点，任意导出
  埋点与导出分离：换后端只改 exporter 配置
```

### 3.2 核心组件

| 组件 | 职责 |
|------|------|
| API/SDK | 统一埋点（Java/Python/Node 等） |
| Collector | 采集/处理/转发中枢 |
| OTLP 协议 | 统一传输（gRPC 4317 / HTTP 4318） |
| 自动埋点 | Java agent 零代码接入 |

```text
OTLP 统一承载三类信号：
  指标（Metrics）→ Prometheus / Mimir
  日志（Logs）→ Loki
  链路（Traces）→ Tempo / Jaeger
```

## 4. Java 应用的 OTel 接入

### 4.1 自动埋点（零代码，推荐起步）

```bash
# Java Agent 方式：不改代码
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=order-service \
  -Dotel.exporter.otlp.endpoint=http://collector:4318 \
  -jar app.jar
```

```text
自动埋点覆盖：
  HTTP 框架（Spring MVC/WebFlux）
  数据库（JDBC/JPA）
  消息（Kafka/RabbitMQ）
  线程池、日志关联（MDC 自动注入 traceId）
```

### 4.2 手动埋点（业务关键点）

```java
import io.opentelemetry.api.trace.Span;

// 自动埋点外的关键业务路径手动加 Span
Span span = tracer.spanBuilder("payment.execute").startSpan();
try (Scope scope = span.makeCurrent()) {
    // 业务逻辑
} finally {
    span.end();
}
```

### 4.3 日志关联（关键配置）

```text
OTel Java Agent 自动将 traceId/spanId 注入 MDC：
  %X{traceId} %X{spanId} → 日志自动带链路 ID

日志 → 链路关联零配置（Loki 中的 traceId 可跳 Tempo）
```

## 5. LGTM 实践：统一查询入口

```text
LGTM = Loki（日志）+ Grafana（可视化）+ Tempo（链路）+ Mimir（指标存储）

Grafana 统一入口：
  Explore → 数据源选择（Prometheus/Loki/Tempo）
  指标面板 → "View logs" 跳日志
  日志 traceId → "View trace" 跳链路
```

| 数据源 | 信号 | 查询语言 |
|--------|------|---------|
| Prometheus / Mimir | 指标 | PromQL |
| Loki | 日志 | LogQL |
| Tempo | 链路 | TraceQL |

```text
# 链路查询（TraceQL 示例）
{ service.name="order-service" } && { status = error }    # 错误链路
{ span.duration > 1s }                                     # 慢链路
```

> 💡 **采集链路 2026**：Grafana Alloy（DaemonSet）统一完成"抓指标 + 采日志 + 转发链路"——一个采集器覆盖三支柱。

## 6. 采样策略

### 6.1 为什么必须采样

```text
不采样 → 高流量下追踪存储成本膨胀 → 被迫关闭追踪（最糟结果）
采样目标：数据量可控 + 关键数据不丢
```

### 6.2 两种采样

| 策略 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **概率采样** | 按比例随机保留（如 10%） | 便宜、无状态、易实现 | 可能丢弃最需要排查的错误/慢请求 |
| **尾部采样（Tail）** | 缓冲完整 trace 后按结果决策 | **保留所有错误 + 超阈值慢请求** + 少量随机 | 要求同 trace 的 span 到同一 Collector；大规模需两层部署 |

```text
选型建议：
  仅成本控制 → 概率采样（10%）
  需要错误可查 → 尾部采样（保留 error + slow + 少量随机）
```

### 6.3 OTel Collector 配置要点

```yaml
# OTel Collector 配置要点
processors:
  memory_limiter:        # 必须放 pipeline 第一位（防 OOM）
    check_interval: 5s
    limit_mib: 2048
  batch:                 # 放最后（高效导出）
    send_batch_size: 1024
```

> ⚠️ 两大配置铁律：**memory_limiter 第一、batch 最后**——否则 Collector 可能 OOM 或导出效率低。

## 7. 三支柱排查实战

```text
场景：凌晨告警"order-service 错误率 14x"

① 指标：Grafana 看错误率曲线（开始时间/接口维度/错误码分布）
   → 定位：POST /orders 5xx 飙升，从 02:30 开始

② 日志：Loki 查该接口 ERROR 日志
   {service="order-service"} |= "/orders" |= "ERROR"
   → 发现：大量 "connection pool exhausted"（连接池耗尽）

③ 链路：取一条错误日志的 traceId → Tempo 查看
   → 发现：调用 payment-service 的 Span 全部超时（2s 超时）

④ 深挖：payment-service 日志/指标
   → 根因：payment 数据库连接池泄漏（该服务 GC 后 live data 持续增长）

⑤ 修复：重启 payment + 修复连接池泄漏代码 → 指标恢复
⑥ 复盘：告警规则是否合理、SLO 预算消耗记录、防复发措施
```

| 环节 | 使用的支柱 | 工具 |
|------|-----------|------|
| ① 发现 | 指标 | Grafana |
| ② 现场 | 日志 | Loki |
| ③ 路径 | 链路 | Tempo |
| ④ 根因 | 日志+指标 | Loki + Grafana |
| ⑤ 修复 | - | 运维操作 |
| ⑥ 复盘 | 全部 | 报告 |

## 8. 核心要点

> 🎯 **核心要点**：
> - 三支柱分工：指标（出事了）、日志（什么事）、链路（在哪出）——**组合才是可观测性**；
> - 关联键 traceId：MDC 注入 → 日志带 ID → 点击跳链路（零配置关联）；
> - OTel 是 2026 统一标准：一次埋点任意导出、Java Agent 零代码接入；
> - LGTM 一体：Grafana 一个入口查三支柱，日志 traceId 可跳 Tempo；
> - 采样铁律：第一天就配（不配会成本爆炸被迫关闭）；错误可查用尾部采样；
> - Collector 配置：memory_limiter 第一、batch 最后；
> - 排查六步：指标找现象 → 日志看现场 → 链路定位 → 深挖根因 → 修复 → 复盘。

## 9. 参考来源

- [OpenTelemetry 官方文档](https://opentelemetry.io/docs/)
- [Grafana Tempo 文档](https://grafana.com/docs/tempo/)
- [OTel Java Agent](https://opentelemetry.io/docs/languages/java/agent/)
- [2026 可观测性堆栈（PLG + OTel）](https://xdev.asia/zh-tw/lessons/kubernetes-tu-co-ban-den-nang-cao/bai-28-observability-stack-2026-plg-opentelemetry/)

---

**下一模块**：[07-生产实践与面试题](07-生产实践与面试题.md)　/　**返回总览**：[00-总览](00-日志监控指标总览.md)
