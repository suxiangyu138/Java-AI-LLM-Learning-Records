# 05-告警体系与 SLO
> 告警不是"越多越好"：告警分级、SLO/错误预算设计、告警疲劳治理——让告警"响得值得、响得及时"

## 📚 目录
1. [告警设计原则](#1-告警设计原则)
2. [告警分级](#2-告警分级)
3. [SLO 与错误预算](#3-slo-与错误预算)
4. [SLO 燃烧率告警](#4-slo-燃烧率告警)
5. [告警规则设计](#5-告警规则设计)
6. [告警疲劳治理](#6-告警疲劳治理)
7. [告警到排查的闭环](#7-告警到排查的闭环)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. 告警设计原则

> 🎯 **告警哲学**：告警是"需要人立即行动"的信号——**不是每个异常都告警，只有"影响用户且需要人处理"才告警**。

| 原则 | 说明 |
|------|------|
| 面向用户影响 | 内部小抖动不告警，用户可感知才告警 |
| 可行动 | 收到告警必须知道"下一步做什么" |
| 可去重 | 同一根因不重复轰炸 |
| 有优先级 | 分级响应（电话/Slack/邮件） |
| 能自愈的不告警 | K8s 重启能解决的不打扰人（记录即可） |

## 2. 告警分级

| 级别 | 触发条件示例 | 通知方式 | 响应时限 |
|------|-------------|---------|---------|
| **Critical（P1）** | SLO 燃烧 > 14x、服务宕机、数据丢失 | 电话 + 短信 | 立即 |
| **Warning（P2）** | 慢燃烧、磁盘 80%、证书 7 天内过期 | Slack/企业微信 | 工作时间内 |
| **Info（P3）** | 容量趋势、非关键指标 | 邮件/周报 | 每周回顾 |

```text
告警分级示例：
  Critical：错误率 > 5% 持续 5 分钟（用户可见故障）
  Warning ：错误率 > 1% 持续 10 分钟 / 磁盘 > 80%
  Info    ：QPS 趋势异常 / 容量预测
```

> 💡 分级核心：**Critical 必须"电话能叫醒人"**——如果做不到，说明 Critical 太多（需要治理）。

## 3. SLO 与错误预算

### 3.1 概念

| 概念 | 定义 | 示例 |
|------|------|------|
| SLI | 服务质量指标（可测量） | 请求成功率、p99 延迟 |
| SLO | 服务级别目标（SLI 的目标值） | 成功率 ≥ 99.9%（30 天） |
| 错误预算 | 100% - SLO = 可容忍的失败量 | 30 天允许 0.1% 失败 |

```text
示例：订单服务 SLO
  SLI：HTTP 请求成功率（5xx 计为失败）
  SLO：99.9%（30 天滚动窗口）
  错误预算：30 天内 0.1% 的请求可以失败（约 43 分钟）
```

### 3.2 SLO 设计步骤

```text
① 选 SLI：对用户最重要的指标（成功率/延迟/可用性）
② 定 SLO：合理的业务目标（99.9%？99.95%？——不是越高越好，越高越贵）
③ 算预算：错误预算 = 1 - SLO
④ 建告警：SLO 燃烧率告警（见第 4 节）
⑤ 迭代：按实际表现调整
```

| 服务类型 | SLO 参考 |
|----------|---------|
| 电商核心（下单/支付） | 99.95% |
| 常规业务服务 | 99.9% |
| 内部工具/非关键 | 99% |
| 批处理任务 | 完成率 + 延迟目标 |

> ⚠️ **SLO 不是越高越好**：99.99% 的成本是 99.9% 的数倍（冗余/资源/运维投入）——SLO 是**业务与成本的平衡决策**，不是工程竞赛。

## 4. SLO 燃烧率告警

### 4.1 燃烧率概念

```text
燃烧率 = 实际错误率 / 允许错误率

示例：SLO 99.9%（允许 0.1% 错误）
  实际错误率 0.2% → 燃烧率 = 2x（2 倍速度消耗预算）
  实际错误率 1.4% → 燃烧率 = 14x

14x 持续 30 分钟 ≈ 消耗 30 天预算的 1%（约 43 分钟中的 30 分钟）
```

### 4.2 双层燃烧告警（Google SRE 实践）

| 告警 | 燃烧率 | 窗口 | 含义 |
|------|--------|------|------|
| **Page（Critical）** | ≥ 14x | 30 分钟 | 快速燃烧，立即处理 |
| **Warning** | ≥ 2x | 6 小时 | 慢燃烧，观察处理 |

```text
PromQL 示例（错误率燃烧告警）：
# 错误率
error_rate = sum(rate(http_requests_total{job="order", code=~"5.."}[1m]))
             / sum(rate(http_requests_total{job="order"}[1m]))

# Critical：燃烧率 ≥ 14x（30 分钟窗口）
error_rate > 0.014    # 0.1% × 14

# Warning：燃烧率 ≥ 2x（6 小时窗口）
error_rate > 0.002    # 0.1% × 2
```

| 优势 | 说明 |
|------|------|
| 业务语言 | 告警 = "预算在燃烧"，团队理解一致 |
| 自校准 | 窗口与燃烧率组合自动过滤瞬时抖动 |
| 优先级明确 | 14x 电话、2x 观察 |

## 5. 告警规则设计

```yaml
# Prometheus 告警规则示例（简化）
groups:
  - name: order-service
    rules:
      - alert: OrderErrorRateHigh        # Critical
        expr: |
          sum(rate(http_requests_total{job="order-service",code=~"5.."}[1m]))
          / sum(rate(http_requests_total{job="order-service"}[1m])) > 0.014
        for: 5m                           # 持续 5 分钟才触发（防抖动）
        labels: { severity: critical }
        annotations:
          summary: "订单服务错误率超标"
          description: "错误率 {{ $value }}%，SLO 预算燃烧中"

      - alert: OrderP99High              # Warning
        expr: histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 10m
        labels: { severity: warning }
```

| 设计要点 | 说明 |
|----------|------|
| `for` 持续时间 | 防瞬时抖动误报（5-10 分钟） |
| 表达式可读 | 注释 + 命名规范 |
| annotations | **告警信息要可行动**（"做什么"） |
| 分服务 | 按 job/服务拆分，避免全局告警 |
| 告警去重 | 根因聚合（Alertmanager grouping） |

> ⚠️ Alertmanager 的 group_by/group_wait 配置（告警聚合与抑制）见同级 `11-云原生/Prometheus+Grafana 监控告警/08-AlertManager告警管理.md`。

## 6. 告警疲劳治理

| 问题 | 治理手段 |
|------|---------|
| 告警太多没人看 | 分级 + 只保留"可行动"告警 |
| 重复轰炸 | Alertmanager 分组/抑制（同根因合并） |
| 阈值过敏感 | `for` 窗口 + 燃烧率告警替代硬阈值 |
| 噪音告警 | 记录 rule 不告警（dashboard 可见） |
| 误报率高 | 告警准确率 KPI（目标 > 80%） |
| 告警无人认领 | 值班表 + 告警认领流程 |

```text
告警健康度三指标：
  告警准确率（触发后确实有问题的比例，目标 > 80%）
  平均响应时间（MTTA）
  平均修复时间（MTTR）
```

> 🎯 告警治理的终极目标：**收到告警的第一反应是"行动"而不是"忽略"**——准确率低会摧毁整个告警体系的可信度。

## 7. 告警到排查的闭环

```text
告警触发（Critical：错误率 14x）
  → 通知（电话/Slack）
  → 认领（谁负责）
  → 排查（Grafana 指标 → Loki 日志 → Tempo 链路，见 06）
  → 修复（回滚/降级/扩容）
  → 复盘（根因、防复发、告警规则是否合理）
  → 预算消耗记录（SLO 燃烧了多少）
```

| 闭环环节 | 产出 |
|----------|------|
| 告警 | 明确的问题描述 |
| 排查 | 根因（三支柱联动） |
| 修复 | 恢复时间（MTTR） |
| 复盘 | 防复发措施 + 告警规则优化 |

## 8. 核心要点

> 🎯 **核心要点**：
> - 告警哲学：只有"影响用户且需要人处理"才告警；
> - 三级告警：Critical（电话）/ Warning（Slack）/ Info（周报）；
> - SLO 三步：选 SLI → 定 SLO（业务成本平衡）→ 算错误预算；
> - 燃烧率告警：14x/30 分钟（Page）+ 2x/6 小时（Warning）——Google SRE 标准实践；
> - 规则设计：`for` 防抖动、annotations 可行动、按服务拆分；
> - 疲劳治理：准确率 > 80%、Alertmanager 分组抑制、只留可行动告警；
> - 闭环：告警 → 认领 → 三支柱排查 → 修复 → 复盘。

## 9. 参考来源

- [Google SRE Book：SLO 与告警](https://sre.google/sre-book/service-level-objectives/)
- [Prometheus 告警规则文档](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)
- [Alertmanager 文档](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [Monitoring Setup Guide（SLO 实践）](https://ilirivezaj.com/guides/monitoring-setup-guide)

---

**下一模块**：[06-可观测性三支柱与OpenTelemetry](06-可观测性三支柱与OpenTelemetry.md)　/　**返回总览**：[00-总览](00-日志监控指标总览.md)
