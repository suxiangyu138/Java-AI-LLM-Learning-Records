# 04 - PromQL 查询语言

> 🎯 PromQL 是 Prometheus 的灵魂 — 从选择器到聚合函数、从 rate/irate 到 histogram_quantile，掌握查询语言才能真正发挥指标数据的价值

---

## 目录

1. [即时向量与范围向量](#1-即时向量与范围向量)
2. [选择器与匹配器](#2-选择器与匹配器)
3. [rate 与 irate（核心！）](#3-rate-与-irate核心)
4. [聚合操作符](#4-聚合操作符)
5. [运算与函数](#5-运算与函数)
6. [子查询](#6-子查询)
7. [Java 后端高频 PromQL 速查](#7-java-后端高频-promql-速查)

---

## 1. 即时向量与范围向量

| 类型 | 含义 | 示例 |
|------|------|------|
| **Instant Vector** | 单个时间点的一组时间序列 | `http_requests_total` |
| **Range Vector** | 一段时间窗口内的一组时间序列 | `http_requests_total[5m]` |

```promql
# Instant Vector — 当前时刻所有实例的请求总数
http_requests_total

# Range Vector — 过去 5 分钟每个实例的请求数据序列
http_requests_total[5m]

# Range Vector 不能直接绘图，需要 rate() 等函数转换
rate(http_requests_total[5m])   # → 转为 Instant Vector，可绘图
```

> ⚠️ **常见错误**：直接用 `http_requests_total[5m]` 画图会报错。Range Vector 必须配合 `rate()`/`increase()`/`avg_over_time()` 等函数使用。

---

## 2. 选择器与匹配器

### 2.1 标签匹配

```promql
# ═══ 精确匹配 ═══
http_requests_total{method="GET", status="200"}

# ═══ 正则匹配 ═══
http_requests_total{method=~"GET|POST"}                    # =~ 正则匹配
http_requests_total{handler=~"/api/.*"}                    # 前缀匹配
http_requests_total{status!~"5.."}                         # !~ 取反匹配

# ═══ 不等于 ═══
http_requests_total{status!="200"}                         # != 不等于
http_requests_total{method!~"DELETE"}                      # !~ 取反正则

# ═══ 无标签匹配 ═══
http_requests_total{handler=""}                            # handler 为空
http_requests_total{handler!=""}                           # handler 存在
```

### 2.2 偏移修饰符 offset

```promql
# 查看 1 小时前的数据（对比分析）
http_requests_total offset 1h
rate(http_requests_total[5m] offset 1h)

# 环比：当前 vs 上周同一时间
rate(http_requests_total[5m]) / rate(http_requests_total[5m] offset 1w)
```

---

## 3. rate 与 irate（核心！）

> ⚠️ **Counter 类型必须用 rate/irate 求速率！直接用 Counter 值是累计值，没有监控意义。**

| 函数 | 计算方式 | 适用场景 | 灵敏度 |
|------|----------|----------|:---:|
| `rate(v [d])` | 时间窗口内的平均增长率 | **图表展示（推荐）** | 平滑 |
| `irate(v [d])` | 最后两个样本点的瞬时增长率 | **告警规则（灵敏）** | 高 |
| `increase(v [d])` | 时间窗口内的总增量 | 统计每小时请求数 | 平滑 |

```promql
# ═══ rate vs irate ═══
# rate — 平均速率，图形平滑（适合长期趋势图）
rate(http_requests_total[5m])

# irate — 瞬时速率，反应灵敏（适合快速告警）
irate(http_requests_total[5m])

# ═══ increase — 总量 ═══
# 过去 1 小时的总请求数
increase(http_requests_total[1h])

# 过去 24 小时的错误总数
increase(http_requests_total{status=~"5.."}[24h])
```

| 场景 | 推荐函数 | 原因 |
|------|----------|------|
| Grafana 趋势图 | `rate(v[5m])` | 平滑曲线，易于观察趋势 |
| 告警规则 | `irate(v[1m])` 或 `rate(v[1m])` | 反应快 |
| 容量报告 | `increase(v[24h])` | 看日总量 |
| 计数器重置 | 两个函数都自动处理 | Counter 归零不跳变 |

---

## 4. 聚合操作符

```promql
# ═══ 基础聚合 ═══
sum(http_requests_total)                          # 总和（所有实例合并）
avg(node_cpu_seconds_total)                       # 平均值
max(jvm_memory_used_bytes)                        # 最大值
min(node_filesystem_avail_bytes)                  # 最小值（找最危险的）
count(http_requests_total)                        # Series 数量

# ═══ 按标签聚合 ═══
# by: 保留指定标签
sum(http_requests_total) by (handler)             # 按接口汇总
sum(rate(http_requests_total[5m])) by (instance)  # 按实例汇总 QPS

# without: 排除指定标签，保留其余
sum(http_requests_total) without (instance)       # 去掉实例维度

# ═══ topk / bottomk ═══
topk(5, rate(http_requests_total[5m]))            # QPS Top 5 接口
bottomk(3, node_filesystem_avail_bytes)           # 磁盘剩余最少 3 台主机

# ═══ count_values ═══
count_values("status_code", http_requests_total)  # 按值分组计数
```

---

## 5. 运算与函数

### 5.1 算术运算

```promql
# 堆内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# 错误率
sum(rate(http_requests_total{status=~"5.."}[5m]))
  / sum(rate(http_requests_total[5m])) * 100

# 磁盘使用率
1 - node_filesystem_avail_bytes / node_filesystem_size_bytes
```

### 5.2 常用函数

```promql
# ═══ 时间聚合（Range Vector → Instant Vector） ═══
avg_over_time(node_load1[1h])                     # 1 小时平均负载
max_over_time(jvm_memory_used_bytes[1h])          # 1 小时内内存峰值
quantile_over_time(0.99, node_load1[1h])          # 1 小时内 P99 负载

# ═══ 预测 ═══
# 根据过去 4 小时数据，预测未来 1 小时磁盘将满
predict_linear(node_filesystem_avail_bytes[4h], 3600) <= 0

# ═══ 标签操作 ═══
label_replace(http_requests_total, "short", "$1", "handler", "/api/(.*)")
label_join(node_cpu_seconds_total, "full", "-", "instance", "cpu")
```

### 5.3 absent / absent_over_time

```promql
# 判断指标是否存在（监控目标下线检测）
absent(up{job="spring-boot-apps"})

# 判断时间窗口内是否有数据
absent_over_time(up{job="spring-boot-apps"}[5m])
```

---

## 6. 子查询

```promql
# 语法：<instant_query> [<range>:<resolution>]
# 对 instant query 的结果按 resolution 采样，做二次计算

# 过去 1 小时内，每 5 分钟采样一次 rate，再取 max
max_over_time(rate(http_requests_total[5m])[1h:5m])

# 每小时 P99 延迟的最大值（找最慢的时段）
max_over_time(
  histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))[1d:1h]
)
```

---

## 7. Java 后端高频 PromQL 速查

```promql
# ═══ HTTP ═══
# QPS（每秒请求数）
sum(rate(http_server_requests_seconds_count[5m]))

# 错误率百分比
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count[5m])) * 100

# P99 延迟
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# ═══ JVM ═══
# 堆内存使用率
sum(jvm_memory_used_bytes{area="heap"}) / sum(jvm_memory_max_bytes{area="heap"}) * 100

# GC 暂停时间
rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])

# 活跃线程数
jvm_threads_live_threads

# ═══ 连接池（HikariCP） ═══
# 活跃连接数
hikaricp_connections_active

# 等待连接数
hikaricp_connections_pending

# ═══ CPU / 内存 ═══
# 主机 CPU 使用率
100 - avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) by (instance) * 100

# 可用内存百分比
node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes * 100

# ═══ 告警常用 ═══
# 服务下线（up=0）
up{job="spring-boot-apps"} == 0

# 磁盘 4 小时内将满
predict_linear(node_filesystem_avail_bytes{mountpoint="/"}[4h], 3600) <= 0
```

> 🎯 **记忆口诀**：Counter 用 rate、Gauge 直接用、Histogram 用 histogram_quantile + rate、聚合用 by/without、对比用 offset。
