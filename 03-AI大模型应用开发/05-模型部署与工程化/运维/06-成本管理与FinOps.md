# 06 - 成本管理与 FinOps

> 🎯 LLM 的成本是持续支出 — API 按 Token、GPU 按小时、存储按月。没有 FinOps，月底账单会让你怀疑人生

## 1. 成本模型

```text
LLM 全链路成本构成：

  ① API 调用（按量）：
     → GPT-4o: $5/1M input + $15/1M output
     → 日均 100 万 Token ≈ $10~$15/天

  ② GPU 租用（按时）：
     → A100(80GB): ~$3/小时 × 2 张 = ~$4300/月
     → 闲置 30% = 浪费 ~$1300/月

  ③ 存储（按月）：
     → 模型权重(10个版本×15GB) = 150GB ≈ $3/月
     → 向量数据库(100万×1024维) ≈ 5GB ≈ $0.1/月

  ④ 网络流量：
     → 模型下载(15GB×每日5次更新) ≈ $15/月
```

## 2. 成本监控

```python
class CostTracker:
    def __init__(self, budget_monthly=5000):
        self.budget = budget_monthly
        self.daily_costs = []
    
    def track_api_call(self, model, tokens_input, tokens_output):
        rates = {"gpt-4o": (5, 15), "gpt-4o-mini": (0.15, 0.6)}
        input_rate, output_rate = rates[model]
        cost = (tokens_input * input_rate + tokens_output * output_rate) / 1e6
        self._record(cost, "api", model)
        return cost
    
    def track_gpu_hour(self, gpu_type, hours):
        rates = {"A100-80GB": 3.0, "A100-40GB": 2.0, "H100": 4.5}
        cost = rates[gpu_type] * hours
        self._record(cost, "gpu", gpu_type)
        return cost
    
    @property
    def daily_total(self):
        today = time.strftime("%Y-%m-%d")
        return sum(c["cost"] for c in self.daily_costs if c["date"] == today)
    
    @property
    def budget_remaining_pct(self):
        monthly = sum(c["cost"] for c in self.daily_costs)
        return (1 - monthly / self.budget) * 100
```

## 3. 成本优化策略

| 策略 | 做法 | 节省 |
|------|------|:---:|
| **分层模型** | 简单问题→mini，复杂问题→pro | 30-50% |
| **语义缓存** | 相同/相似问题不重复推理 | 20-40% |
| **量化** | FP16→INT4 | 显存减半 |
| **Spot/抢占实例** | 离线任务用低价实例 | 60-80% |
| **Prompt 压缩** | 去掉冗余上下文 | 20-30% |
| **批量合并** | 积攒请求合并处理 | 10-20% |

```python
# 语义缓存 → 避免重复推理
class SemanticCache:
    def __init__(self, embed_fn, vector_store, similarity_threshold=0.95):
        self.embed = embed_fn
        self.store = vector_store
        self.threshold = similarity_threshold
    
    def get_or_compute(self, prompt, llm_func):
        vec = self.embed(prompt)
        results = self.store.search(vec, top_k=1)
        
        if results and results[0]["score"] > self.threshold:
            logger.info(f"缓存命中！节省 {(prompt_len+resp_len)} tokens")
            return results[0]["response"]
        
        response = llm_func(prompt)
        self.store.add(prompt, vec, response)
        return response
```

## 4. 预算告警

```yaml
# Prometheus 成本告警
- alert: DailyBudgetExceeded
  expr: sum(increase(llm_cost_total[1d])) > 200
  annotations:
    summary: "日 Token 消耗超过 $200"
    
- alert: GPUIdleHigh
  expr: avg(gpu_utilization[1h]) < 30
  for: 2h
  annotations:
    summary: "GPU 闲置 > 2 小时 → 考虑缩容"
```
