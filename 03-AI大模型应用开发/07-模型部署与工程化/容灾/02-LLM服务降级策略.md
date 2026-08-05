# 02 - LLM 服务降级策略

> 🎯 LLM 挂了不是"返回 500"，而是"有策略地降低服务质量" — 从模型降级链到静态兜底，从功能裁剪到用户体验保护

---

## 目录

1. [降级策略全景](#1-降级策略全景)
2. [模型降级链](#2-模型降级链)
3. [功能降级与裁剪](#3-功能降级与裁剪)
4. [静态兜底方案](#4-静态兜底方案)
5. [降级决策引擎](#5-降级决策引擎)
6. [用户体验保护](#6-用户体验保护)

---

## 1. 降级策略全景

```text
降级 = 在故障时牺牲非核心能力，保护核心体验

LLM 降级四层漏斗：

  ┌───────────────────────────────────────┐
  │ ① 模型降级：GPT-4o→GPT-4o-mini→Qwen  │  ← 最常用
  ├───────────────────────────────────────┤
  │ ② 功能降级：去掉RAG→去掉工具→纯LLM    │
  ├───────────────────────────────────────┤
  │ ③ 质量降级：降低max_tokens→减少上下文 │
  ├───────────────────────────────────────┤
  │ ④ 静态兜底：返回预设回复/模板         │  ← 最后防线
  └───────────────────────────────────────┘
```

---

## 2. 模型降级链

### 降级链配置

```python
from enum import Enum
from dataclasses import dataclass

class ModelTier(Enum):
    PREMIUM = "premium"     # GPT-4o / Claude Opus
    STANDARD = "standard"   # GPT-4o-mini / Qwen-72B
    BASIC = "basic"         # Qwen-7B / Llama-8B
    FALLBACK = "fallback"   # 静态模板回复

@dataclass
class ModelNode:
    name: str
    endpoint: str
    tier: ModelTier
    max_tokens: int
    
DEGRADATION_CHAIN = {
    ModelTier.PREMIUM: [
        ModelNode("gpt-4o", "https://api.openai.com", ModelTier.PREMIUM, 4096),
        ModelNode("claude-sonnet", "https://api.anthropic.com", ModelTier.PREMIUM, 4096),
    ],
    ModelTier.STANDARD: [
        ModelNode("gpt-4o-mini", "https://api.openai.com", ModelTier.STANDARD, 2048),
        ModelNode("qwen-72b", "http://vllm-internal:8000", ModelTier.STANDARD, 2048),
    ],
    ModelTier.BASIC: [
        ModelNode("qwen-7b", "http://vllm-basic:8000", ModelTier.BASIC, 1024),
    ],
}
```

### 自动降级流程

```python
import asyncio
from typing import Optional

class LLMWithFallback:
    """带降级链的 LLM 调用"""
    
    def __init__(self, chain_config):
        self.chain = chain_config  # 多层降级链
        self.circuit_breaker = CircuitBreaker()
    
    async def chat(self, messages, requested_tier=ModelTier.PREMIUM):
        """从请求等级开始，逐层降级尝试"""
        current_tier = requested_tier
        
        while current_tier.value >= ModelTier.FALLBACK.value:
            models = self.chain.get(current_tier, [])
            
            for model in models:
                # 检查断路器
                if self.circuit_breaker.is_open(model.name):
                    continue
                
                try:
                    response = await asyncio.wait_for(
                        self._call_model(model, messages),
                        timeout=30
                    )
                    # 成功！记录并返回
                    self.circuit_breaker.record_success(model.name)
                    return response
                    
                except (TimeoutError, ConnectionError) as e:
                    # 本次失败，记录 → 换下一个模型
                    self.circuit_breaker.record_failure(model.name)
                    continue
            
            # 当前等级所有模型都失败 → 降级
            current_tier = self._next_tier(current_tier)
        
        # 全部模型失败 → 静态兜底
        return self.static_fallback(messages)
    
    def _next_tier(self, tier):
        tiers = list(ModelTier)
        idx = tiers.index(tier)
        return tiers[idx + 1] if idx + 1 < len(tiers) else ModelTier.FALLBACK
    
    def static_fallback(self, messages):
        """最后兜底：返回预设回复"""
        return {
            "content": "抱歉，当前服务繁忙，请稍后再试。\n"
                       "如紧急，请发送邮件至 support@example.com",
            "model": "fallback",
            "tier": "FALLBACK"
        }
```

---

## 3. 功能降级与裁剪

```text
功能降级矩阵：

  正常运行 (100%功能)：LLM + RAG + Tools + Agent + 长上下文
    ↓ 轻微故障
  功能裁剪 (80%)：LLM + RAG + Tools（去掉Agent，降低自主性）
    ↓ 中度故障
  核心保留 (50%)：LLM + RAG（去掉工具调用）
    ↓ 严重故障
  纯LLM (30%)：LLM + 短上下文（去掉RAG和工具）
    ↓ 灾难故障
  静态兜底 (5%)：关键词匹配 → FAQ回复
```

### 功能降级实现

```python
class FeatureDegrader:
    def __init__(self):
        self.feature_level = "full"  # full / reduced / core / minimal
    
    def degrade(self, reason):
        levels = ["full", "reduced", "core", "minimal", "static"]
        current_idx = levels.index(self.feature_level)
        if current_idx < len(levels) - 1:
            self.feature_level = levels[current_idx + 1]
            logger.warning(f"Feature degraded to {self.feature_level}: {reason}")
    
    def process(self, request):
        if self.feature_level == "full":
            return self.full_pipeline(request)     # LLM+RAG+Tools+Agent
        elif self.feature_level == "reduced":
            return self.llm_rag_tools(request)     # LLM+RAG+Tools
        elif self.feature_level == "core":
            return self.llm_rag(request)           # LLM+RAG
        elif self.feature_level == "minimal":
            return self.pure_llm(request)          # 纯LLM+短上下文
        else:
            return self.static_reply(request)      # 关键词→FAQ
```

---

## 4. 静态兜底方案

```python
# 关键词→FAQ 兜底
FAQ_FALLBACK = {
    "退款": "退款流程：登录→我的订单→申请退款→3-5个工作日到账。人工客服：support@example.com",
    "发货": "订单发货后您将收到短信通知。通常24小时内发货。如超48小时未发货，请联系客服。",
    "价格": "商品价格以页面显示为准。如发现价格异常，请截图联系客服核实。",
    "default": "抱歉，当前咨询量较大，请稍后再试。或发送邮件至 support@example.com",
}

def keyword_fallback(user_message):
    for keyword, reply in FAQ_FALLBACK.items():
        if keyword in user_message:
            return reply
    return FAQ_FALLBACK["default"]
```

---

## 5. 降级决策引擎

```text
多因素降级决策：

  触发条件：
    ① API 错误率 > 5%（持续 1 分钟）→ 降级
    ② P99 延迟 > 10s（持续 1 分钟）→ 降级
    ③ 并发数 > GPU 容量的 120% → 降级
    ④ 成本异常（单小时费用超过预算 2×）→ 降级

  恢复条件（全部满足才恢复）：
    ① API 错误率 < 1%（持续 2 分钟）
    ② P99 延迟 < 3s（持续 2 分钟）
    ③ 人工确认恢复（企业版）

  核心原则：快速降级，谨慎恢复
    → 降级快（1 分钟内），避免长期故障影响用户
    → 恢复慢（观察 2 分钟+人工确认），避免"抖动"
```

---

## 6. 用户体验保护

```text
降级时的用户体验原则：

  ① 明确告知："当前正在使用轻量模型为您服务，如需深度分析请稍后再试"
  ② 保证核心功能：chat 不能挂，画图/数据分析可以降级
  ③ 降级用户无感：前端不显示技术细节，只说"服务模式调整"
  ④ 自动恢复：切勿让降级成为永久状态

糟糕的用户体验：
  ❌ "[Error] LLM timeout, fallback to gpt4o-mini"（暴露技术细节）
  ❌ 降级后不告知 → 用户发现回答质量下降 → 以为是产品变差了
  
好的用户体验：
  ✅ "系统正在优化中，如回答不够准确请稍后再试"（给合理预期）
  ✅ 降级完成后自动恢复 → 用户下一次使用时已正常
```
