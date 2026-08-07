# Logstash 性能调优与可靠传输
> 吞吐上不去？重启丢数据？写入失败静默丢失？——本文件覆盖性能调优路线图、持久化队列（PQ）、死信队列（DLQ）、监控 API 与 9.x 行为变更

## 目录
1. [性能调优总路线图](#1-性能调优总路线图)
2. [Pipeline 参数调优](#2-pipeline-参数调优)
3. [JVM 与资源调优](#3-jvm-与资源调优)
4. [解析性能优化](#4-解析性能优化)
5. [持久化队列（PQ）：重启不丢数据](#5-持久化队列pq重启不丢数据)
6. [死信队列（DLQ）：失败不丢数据](#6-死信队列dlq失败不丢数据)
7. [监控：Stats API 与 Stack Monitoring](#7-监控stats-api-与-stack-monitoring)
8. [9.x 行为变更与升级注意](#8-9x-行为变更与升级注意)
9. [常见性能问题速查](#9-常见性能问题速查)

---

## 1. 性能调优总路线图

```text
先看现象：
├─ 吞吐低？
│   ├─ 下游慢（ES 写入延迟高）→ 调输出批量/确认机制
│   ├─ 解析 CPU 密集（grok）→ 调 workers
│   └─ 队列积压 → 检查背压来源
├─ 内存高？
│   ├─ 堆压力大 → 9.x buffer 类型检查 + 堆调整
│   └─ 事件过大 → 截断/精简字段
└─ 延迟高？
    └─ batch.delay 太大 → 调小（低流量场景）
```

> 🎯 调优第一原则：**先定位瓶颈在哪一段，再动手**——Logstash 的瓶颈 80% 在下游（ES 写入）或上游（采集速率），自身解析反而少。

---

## 2. Pipeline 参数调优

### 2.1 三参数详解

| 参数 | 默认 | 调优逻辑 |
|------|:---:|------|
| `pipeline.workers` | CPU 核数 | filter 段 CPU 密集且 CPU 有闲 → 调大；output 是 IO 瓶颈 → 调大无益 |
| `pipeline.batch.size` | 125 | 调大 → 吞吐↑、内存↑、尾部延迟↑；建议 125-1000 试 |
| `pipeline.batch.delay` | 50ms | 低流量时决定延迟下限；追求实时调小（5-25ms） |

### 2.2 调优示例（高吞吐场景）

```yaml
# logstash.yml
pipeline.workers: 8
pipeline.batch.size: 500
pipeline.batch.delay: 25
```

### 2.3 吞吐公式与内存预算

```text
吞吐 ≈ workers × (batch.size / 单批处理时间)
内存 ≈ workers × batch.size × 单事件内存 × 2-3（含对象开销）

例：8 workers × 500 × 1KB × 3 ≈ 12MB 批次区 + 队列区
⚠️ 9.x：默认 buffer 类型 direct → heap 后，批次缓冲占用堆，
   调大 batch 前先确认堆余量
```

---

## 3. JVM 与资源调优

### 3.1 JVM 堆配置

```yaml
# jvm.options
-Xms2g
-Xmx2g
```

| 场景 | 堆建议 |
|------|------|
| 小型（< 50MB/s） | 1-2GB |
| 中型 | 2-4GB |
| 大型（grok 重/事件大） | 4-8GB |
| 规则 | 堆 ≤ 物理内存 50%（留 OS 缓存） |

### 3.2 资源规划

| 资源 | 说明 |
|------|------|
| CPU | grok 密集 → 核多；转发为主 → 2-4 核够 |
| 内存 | 堆 + OS 缓存（文件/网络缓冲） |
| 磁盘 | PQ 落盘需要空间（按队列配置预留） |
| 网络 | 采集与输出带宽之和 |

> ⚠️ 常见误区：**堆不是越大越好**——超过物理内存 50% 会挤占 OS 页缓存，反而影响文件/PQ 性能。

---

## 4. 解析性能优化

### 4.1 插件级优化

| 手段 | 效果 |
|------|------|
| dissect 替代 grok | 快 5-10 倍（固定格式） |
| 合并多个 grok 为一个大 grok | 一次正则遍历 |
| 条件包裹（if level == ERROR 才 grok 详情） | 减少无谓解析 |
| message 截断 | 超长日志先 truncate 再解析 |
| JSON 日志优先 | 免正则直接结构化 |

### 4.2 管道级优化

| 手段 | 说明 |
|------|------|
| 多管道隔离 | 重解析管道与转发管道分开 |
| 避免 ruby 插件 | 最后手段，可考虑 mutate/grok 组合 |
| 关闭不需要的字段 | drop 插件丢弃无用事件 |
| 压缩传输 | beats/kafka 默认压缩，省带宽 |

---

## 5. 持久化队列（PQ）：重启不丢数据

### 5.1 为什么需要 PQ

```text
默认内存队列：
  · 快，但重启/崩溃时队列中的事件丢失
  · 无背压边界（内存爆的风险）

持久化队列（PQ）：
  · 事件先落盘（path.data/queue），再进 worker
  · 重启/崩溃后从磁盘恢复，不丢数据
  · 天然背压（队列满则 input 暂停）
```

### 5.2 开启配置

```yaml
# logstash.yml
queue.type: persisted
queue.max_bytes: 4gb          # 队列上限（超过则 input 阻塞）
queue.page_capacity: 64mb     # 页大小（写盘单元）
queue.checkpoint.writes: 1024 # 检查点频率（崩溃恢复粒度）
```

### 5.3 PQ 适用场景与代价

| 场景 | 是否开启 |
|------|:---:|
| 日志链路（可接受少量丢） | 可选 |
| 金融/交易日志（不能丢） | **必须** |
| 高吞吐管道 | 开（写盘有开销，需评估） |
| 测试环境 | 不开 |

> 🎯 权衡：**PQ 用磁盘换可靠性**——吞吐损失约 10-30%，换「重启/崩溃不丢事件」。关键业务必须开，且磁盘要预留 `queue.max_bytes` 空间。

---

## 6. 死信队列（DLQ）：失败不丢数据

### 6.1 DLQ 机制

```text
output 写入失败（ES 拒绝/映射冲突/超限）
   ↓ 启用 DLQ 后
失败事件 + 失败原因 → 写入 data/dead_letter_queue/
   ↓ 后续
可用 dead_letter_queue 输入插件重新处理
```

### 6.2 配置

```yaml
# logstash.yml
dead_letter_queue.enable: true
dead_letter_queue.max_bytes: 1gb
```

```ruby
# 输出侧指定（es 输出）
output {
  elasticsearch {
    hosts => ["http://es:9200"]
    dead_letter_queue_enable => true
    dead_letter_queue_write_size => 4mb
  }
}
```

### 6.3 重新处理 DLQ 数据

```ruby
# dlq_repair.conf：把 DLQ 数据重新灌入管道
input {
  dead_letter_queue {
    path => "/var/lib/logstash/data/dead_letter_queue"
    pipeline_id => "main"          # 原管道 ID
    commit_offsets => true
  }
}
output {
  elasticsearch { hosts => ["http://es:9200"] }
}
```

> 💡 2026 动态：9.x 对 DLQ 段目录扫描做了单遍优化（DirectoryStream + 增量跟踪队列大小），DLQ 场景性能明显提升。

---

## 7. 监控：Stats API 与 Stack Monitoring

### 7.1 Stats API（9600 端口）

```bash
# 节点总览（JVM/事件/流）
curl http://localhost:9600/_node/stats

# 管道统计（9.x 增强）
curl http://localhost:9600/_node/stats/pipelines
```

### 7.2 管道统计关键指标（9.x）

| 指标 | 含义 | 关注点 |
|------|------|------|
| events.in / filtered / out | 各阶段事件数 | 输入输出差 = 丢弃/失败 |
| queue.events / queue.size.bytes | 队列积压 | 积压增长 = 处理不过来 |
| flow rates | 当前与生命周期速率 | 吞吐基准 |
| batch 采样 | 批次大小（事件数/字节）| 9.x 新增字节 max 指标 |
| 配置热加载 | 成功/失败 | 配置管理 |

### 7.3 Stack Monitoring 接入

```yaml
# logstash.yml
xpack.monitoring.enabled: true
xpack.monitoring.elasticsearch.hosts: ["http://es-node:9200"]
xpack.monitoring.elasticsearch.username: "logstash_system"
xpack.monitoring.elasticsearch.password: "xxx"
```

```text
接入后 Kibana → Stack Monitoring → Logstash：
  · 节点列表与健康
  · 管道吞吐/积压曲线
  · 事件流入流出对比（发现丢事件）
  · JVM 堆与 CPU
```

---

## 8. 9.x 行为变更与升级注意

### 8.1 关键变更（2026 现状）

| 变更 | 影响 | 应对 |
|------|------|------|
| 默认 buffer 类型 direct → heap | 升级后批次缓冲占 JVM 堆，堆压力增加 | 显式配置 `logstash_pipeline_buffer_type`，评估堆余量 |
| 自带 JDK 21 | 无需单独装 Java | 检查自定义脚本兼容 |
| Stats API 增强 | batch 字节 max、健康报告、热线程 | 监控更细 |
| DLQ 单遍扫描优化 | DLQ 场景性能提升 | 直接受益 |

### 8.2 升级清单

```text
① 先读 release notes 的 breaking changes
② 测试环境升级 + 管道语法校验
③ 显式设置 logstash_pipeline_buffer_type（审计 + 可控）
④ 验证 PQ/DLQ 数据兼容
⑤ 监控堆内存趋势 1-2 周
```

---

## 9. 常见性能问题速查

| 症状 | 原因 | 处理 |
|------|------|------|
| 吞吐上不去 | 下游 ES 慢 | 查 ES 写入延迟、扩 ES 节点 |
| 队列持续积压 | workers 不足 / 解析太重 | 调 workers、batch，dissect 替代 grok |
| 堆一直涨 | batch 太大 / 事件对象大 | 调小 batch、截断 message、限流 |
| 延迟忽高 | batch.delay 与背压 | 调小 delay、查下游抖动 |
| 重启丢数据 | 未开 PQ | 关键管道开 PQ |
| 静默丢数据 | 输出失败未开 DLQ | 开 DLQ + 定期重放 |
| 9.x 升级后堆高 | buffer 类型变更 | 显式配置 + 调堆 |

> 🎯 **核心要点**：Logstash 可靠性 = **PQ（重启不丢）+ DLQ（失败不丢）** 双保险；性能 = **先定位瓶颈（下游/解析/队列）再调三参数（workers/batch/delay）**；监控 = **Stats API 看积压 + Stack Monitoring 看趋势**。9.x 升级三件事：显式配置 buffer 类型、评估堆压力、验证 PQ/DLQ。面试回答「Logstash 丢数据吗」：默认内存队列会丢，开 PQ + DLQ 后不会。

---

**下一模块**：[06-日志采集全链路实战](06-日志采集全链路实战.md) | **返回总览**：[00-Logstash专题总览](00-Logstash专题总览.md)
