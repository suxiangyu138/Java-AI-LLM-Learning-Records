# 01 - 容灾核心概念与 SLA 度量

> 🎯 99.9% 和 99.99% 差了 10 倍 — 一天宕 1.4 分钟 vs 8.6 秒。理解 RTO/RPO/MTTR 才能设计正确的容灾方案

---

## 目录

1. [可用性 SLA](#1-可用性-sla)
2. [RTO 与 RPO](#2-rto-与-rpo)
3. [故障分类](#3-故障分类)
4. [MTBF-MTTR-MTTF](#4-mtbf-mttr-mttf)
5. [LLM 服务的 SLA 设计](#5-llm-服务的-sla-设计)

---

## 1. 可用性 SLA

### 可用性等级与年宕机时间

| 等级 | 可用性 | 年宕机时间 | 日宕机时间 | 典型实现 |
|:---:|:---:|------|------|------|
| 2个9 | 99% | 3.65 天 | 14.4 分钟 | 单机+定时备份 |
| 3个9 | 99.9% | 8.76 小时 | 1.44 分钟 | 主备切换 |
| 4个9 | 99.99% | 52.6 分钟 | 8.6 秒 | 同城双活 |
| 5个9 | 99.999% | 5.26 分钟 | 0.86 秒 | 异地多活 |

```text
可用性计算公式：
  Availability = MTBF / (MTBF + MTTR)
  
  MTBF (Mean Time Between Failures) = 平均故障间隔
  MTTR (Mean Time To Repair) = 平均修复时间

提升可用性的两条路：
  ① 增大 MTBF（减少故障发生）→ 高质量代码+充分测试
  ② 减小 MTTR（加速故障恢复）→ 自动切换+自动回滚
```

### LLM 服务的可用性计算

```text
LLM 服务依赖链的可用性：
  Gateway(99.99%) × LLM Service(99.9%) × GPU(99.9%) × Model(99.99%)
  = 0.9999 × 0.999 × 0.999 × 0.9999
  = 99.78%！

依赖越多，整体可用性越低 → 需要冗余！
加冗余后：
  Gateway(99.99%) × [LLM-A(99.9%) ∥ LLM-B(99.9%)] × GPU(99.9%)
  = 99.99% × 99.9999% × 99.9%
  = 99.89%
```

---

## 2. RTO 与 RPO

```text
RTO (Recovery Time Objective) = 恢复时间目标
  → 故障后多久必须恢复服务？
  → LLM API：RTO < 30s（自动切换）
  → 模型训练 Pipeline：RTO < 4h（可人工介入）

RPO (Recovery Point Objective) = 恢复点目标
  → 允许丢失多少数据？
  → 对话历史：RPO = 0（用户看到一半的对话不能丢）
  → 模型权重：RPO = 上次保存的 Checkpoint（可能丢几小时训练）
```

---

## 3. 故障分类

### LLM 服务特有故障

| 故障类型 | 示例 | 检测方式 | 恢复手段 |
|----------|------|----------|----------|
| **API 不可用** | OpenAI 全球宕机 | 健康检查 5xx | 切到备选 API/模型 |
| **GPU 故障** | ECC Error、NVLINK 断 | NVML 监控 | GPU 隔离+重调度 |
| **显存溢出** | OOM，进程被 Kill | 显存监控 | 减 max_model_len/重启 |
| **模型输出异常** | 幻觉率突升、违规输出 | 输出质量监控 | 切到旧版本模型 |
| **推理超时** | 复杂 Prompt → 30s+ | 超时熔断 | 降级为简单回复 |
| **计费异常** | API 扣费暴增 | 成本监控 | 限流+告警 |

---

## 4. MTBF-MTTR-MTTF

```python
class ReliabilityMetrics:
    """LLM 服务可靠性指标追踪"""
    
    def __init__(self):
        self.incidents = []  # [(start_time, end_time), ...]
    
    def record_incident(self, start, end):
        self.incidents.append((start, end))
    
    @property
    def mtbf(self):
        """平均故障间隔"""
        if len(self.incidents) < 2:
            return float('inf')
        intervals = [self.incidents[i][0] - self.incidents[i-1][1]
                     for i in range(1, len(self.incidents))]
        return sum(intervals) / len(intervals)
    
    @property
    def mttr(self):
        """平均修复时间"""
        if not self.incidents:
            return 0
        downtimes = [(end - start) for start, end in self.incidents]
        return sum(downtimes) / len(downtimes)
    
    @property
    def availability(self):
        total = self.mtbf + self.mttr
        return self.mtbf / total if total > 0 else 1.0
```

---

## 5. LLM 服务的 SLA 设计

### 分级 SLA 策略

```text
不同用户级别不同 SLA：

  Free 用户：
    可用性: 99%（允许偶尔不可用）
    降级策略: 返回"服务繁忙，请稍后重试"
    
  Pro 用户：
    可用性: 99.9%
    降级策略: GPT-4o 不可用 → 自动切换 GPT-4o-mini
    
  Enterprise 用户：
    可用性: 99.99%
    降级策略: OpenAI → Azure OpenAI → 自建 vLLM 三级切换
    专属 GPU 资源池
    赔偿条款：<99.99% 退 10% 月费
```

### LLM 服务可用性监控看板

```python
# Prometheus 指标
llm_request_total{status="success"}        # 成功请求
llm_request_total{status="error"}          # 失败请求
llm_model_switch_total{from="gpt4o",to="gpt4o-mini"}  # 切换次数
llm_circuit_breaker_state{model="gpt4o"}   # 熔断器状态(1=开,0=关)
llm_fallback_hit_total                     # 降级命中次数
```
