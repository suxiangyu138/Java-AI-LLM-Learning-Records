# 03 - Extended Thinking 深度思考

> 🎯 Claude 的核心差异化 — 让模型在"草稿纸"上推理后再回答。2026 年 Opus/Sonnet 4.6+ 升级为"自适应思考"（自动判断是否需要深度推理）。本章覆盖两种模式、effort 控制、成本管理、流式可视化

---

## 目录

1. [两种模式：Adaptive vs Manual](#1-两种模式adaptive-vs-manual)
2. [Adaptive Thinking 实战](#2-adaptive-thinking-实战)
3. [effort 参数详解](#3-effort-参数详解)
4. [成本管理与预算策略](#4-成本管理与预算策略)
5. [流式可视化 Thinking](#5-流式可视化-thinking)
6. [常见错误速查](#6-常见错误速查)

---

## 1. 两种模式：Adaptive vs Manual

```text
2026 年的变化（4.6+ 模型）：
├── Adaptive Thinking（新，推荐）— 模型自动决定是否需要推理、推理多深
│   语法：thinking: {"type": "adaptive"}
│   可选：output_config: {"effort": "low/medium/high/xhigh/max"}
│   支持模型：Opus 4.7/4.6、Sonnet 4.6
│
└── Manual Extended Thinking（旧，Haiku 4.5 仍用）— 手动设置预算
    语法：thinking: {"type": "enabled", "budget_tokens": 4096}
    支持模型：Haiku 4.5 及更早模型
    ⚠️ Opus 4.7/Sonnet 4.6 使用此语法 → 400 错误
```

| 对比 | Adaptive | Manual（旧） |
|------|----------|:---:|
| 模型支持 | Opus 4.7/4.6、Sonnet 4.6 | Haiku 4.5 及老模型 |
| 推理深度 | effort 参数（low~xhigh） | budget_tokens（1024+） |
| 是否自动判断 | ✅ 自动 | ❌ 手动 |
| 交错思考 | ✅ 自动支持 | ❌ |

---

## 2. Adaptive Thinking 实战

### 2.1 基础用法

```python
response = client.messages.create(
    model="claude-sonnet-4-6",
    max_tokens=8192,
    thinking={"type": "adaptive", "display": "summarized"},  # ★ 启用
    output_config={"effort": "medium"},                       # ★ 推理深度
    messages=[{"role": "user", "content": "这段代码的时间复杂度是多少？..."}]
)

# 思考内容在 response.content 中（thinking block + text block 分离）
for block in response.content:
    if block.type == "thinking":
        print(f"[思考] {block.thinking}")   # AI 的推理过程
    elif block.type == "text":
        print(f"[回答] {block.text}")       # 最终回复
```

### 2.2 门控策略（按任务启用）

```python
# ★ 不要全局启用 — 只对复杂任务用
def should_use_thinking(messages):
    """判断是否需要深度推理"""
    complex_keywords = ["解释原理", "分析", "重构", "修复 Bug", "设计"]
    last_msg = messages[-1]["content"].lower()
    return any(k in last_msg for k in complex_keywords)

def smart_call(messages):
    params = {
        "model": "claude-sonnet-4-6",
        "max_tokens": 8192,
        "messages": messages
    }
    if should_use_thinking(messages):
        params["thinking"] = {"type": "adaptive"}
        params["output_config"] = {"effort": "high"}
    return client.messages.create(**params)
```

---

## 3. effort 参数详解

| effort | 适用场景 | 思考时长 | Token 成本 |
|--------|----------|:---:|:---:|
| `low` | 简单分析、小修小补 | 短 | 低 |
| `medium` | 代码审查、API 设计、规划 | 中 | 中 |
| **`high`** | **复杂调试、多文件重构、文档生成** | 长 | 高 |
| `xhigh` | 架构设计、安全审计、极难推理 | 很长 | 很高 |
| `max` | 研究级难题（仅 Opus 4.6） | 最长 | 最高 |

**使用建议：**
- Coding/Agent 任务 → `xhigh`
- 生产代码审查 → `medium`~`high`
- 简单问题 → 不启用 thinking（省成本）

---

## 4. 成本管理与预算策略

```
Thinking Tokens 的成本真相：
├── 按输出价格计费（不便宜！）
├── 可能是最终回答的 3-5 倍 Token
├── Opus: $25/MTok（输出）× 10000 thinking tokens = $0.25/次

示例（Sonnet 4.6, effort=high）：
  输入: 500 tokens × $3 = $0.0015
  思考: 5000 tokens × $15 = $0.075
  回答: 1000 tokens × $15 = $0.015
  总计: ~$0.09/次

如果每次请求都启用 → 成本可能增长 3-10 倍！
```

**成本优化三策略：**
1. **任务分级** — 简单任务不启用、复杂任务用 `high`、极难用 `xhigh`
2. **Haiku 替代 Sonnet** — 可枚举的简单任务用 Haiku 的 manual thinking（budget_tokens=1024）
3. **监控 thinking_tokens** — `usage.output_tokens` 中查看 thinking 占比，异常告警

---

## 5. 流式可视化 Thinking

```python
# SSE 流式 → 前端显示"思考中..."
async with client.messages.stream(
    model="claude-sonnet-4-6",
    max_tokens=8192,
    thinking={"type": "adaptive", "display": "summarized"},
    messages=[...]
) as stream:
    # ① 思考阶段
    async for event in stream:
        if event.type == "content_block_delta":
            if event.delta.type == "thinking_delta":
                print(f"[思考] {event.delta.thinking}", end="", flush=True)
            elif event.delta.type == "text_delta":
                print(event.delta.text, end="", flush=True)
```

**事件顺序（UI 映射）：**
```text
content_block_start (thinking) → 显示"思考中..."面板
  thinking_delta × N            → 面板内实时更新
content_block_stop              → 面板结束
content_block_start (text)      → 显示"回答"面板
  text_delta × N                → 逐字输出
content_block_stop              → 完毕

前端：可折叠"推理过程"面板（默认折叠，用户可选展开）
```

---

## 6. 常见错误速查

| 错误 | 原因 | 解决 |
|------|------|------|
| `400 budget_tokens not supported` | Opus/Sonnet 4.6+ 不支持 manual 模式 | 改用 `thinking: {"type": "adaptive"}` |
| `400 temperature not supported` | Opus 4.7 移除了采样参数 | 删除 `temperature/top_p/top_k` |
| 响应被截断 | `max_tokens` < thinking + answer | `max_tokens` ≥ `budget_tokens × 2` |
| 思考大量空白 | `display` 未设为 `"summarized"` | 加 `"display": "summarized"` |
| `429` 限流 | Thinking 消耗速度快 | 指数退避重试 + 提升 Tier |

---

> 🎯 **核心要点**：Extended Thinking 的 2026 年三句话 — **① 4.6+ 模型用 adaptive（effort 控制深度），旧模型用 manual（budget_tokens）② 不要全局启用 — 按任务复杂度门控，简单任务不启用 ③ Thinking 按输出价计费，可能是最终回答的 3-5 倍 Token**。Coding 推荐 `effort: "xhigh"`。

**下一模块**：[04-Tool-Use工具调用](04-Tool-Use工具调用.md) / **返回总览**：[00-总览](00-Claude-API知识体系总览.md)
