# 03 - Reasoning 推理控制

> 🎯 GPT-5.6 的推理控制体系 — 六个 effort 等级、Sol 的 Max/Ultra 模式、Pro 质量优先模式。推理深度 = 质量 × 成本 × 延迟的三维权衡

---

## 目录

1. [六档 effort 推理等级](#1-六档-effort-推理等级)
2. [Sol 专属：Max 与 Ultra 模式](#2-sol-专属max-与-ultra-模式)
3. [Pro 模式：质量优先](#3-pro-模式质量优先)
4. [推理成本与延迟权衡](#4-推理成本与延迟权衡)
5. [按场景选择推理深度](#5-按场景选择推理深度)

---

## 1. 六档 effort 推理等级

```python
# reasoning.effort 六档（GPT-5.6 三档模型共享）
response = client.responses.create(
    model="gpt-5.6-terra",
    reasoning={"effort": "high"},    # none/low/medium/high/xhigh/max
    input="..."
)
```

| effort | 推理深度 | Token 消耗 | 适用场景 |
|:---:|:---:|:---:|------|
| `none` | 无推理 | 最低 | 简单分类、路由、翻译 |
| `low` | 浅 | 低 | 简单问答、提取 |
| `medium` | 中 | 中 | 常规分析、总结 |
| `high` | 深 | 高 | 代码审查、复杂分析 |
| `xhigh` | 很深 | 很高 | 多文件重构、调试 |
| `max` | 最深 | 最高 | 极难问题（Sol） |

**分级收益实测：** 简单任务用 `low`（vs `high`）可节省约 60% 成本，质量几乎不降。

---

## 2. Sol 专属：Max 与 Ultra 模式

### 2.1 Max 模式（更深推理）

```python
# Max 模式：更深层推理 + 更久思考检查
response = client.responses.create(
    model="gpt-5.6-sol",
    reasoning={"effort": "max"},     # ★ Sol 独有
    input="证明这个数学定理..."
)
```

**Max vs 其他：** 适用于验证性任务（证明、逻辑检查）— 模型在给出结论前进行更长时间的内部验证。

### 2.2 Ultra 模式（多智能体并行）

```python
# Ultra 模式：默认协调 4 个子智能体并行工作，最高 16 个
response = client.responses.create(
    model="gpt-5.6-sol",
    reasoning={
        "effort": "xhigh",
        "mode": "pro"
    },
    input="...复杂任务..."
)
```

| 模式 | 子智能体 | 效果 |
|------|:---:|------|
| 标准 Sol | 1 | 单线程推理 |
| Ultra（默认） | **4** | 并行工作、交叉验证 |
| Ultra（最高） | **16** | 以更高算力换取更强结果和更快速度 |

**Terminal-Bench 2.1 实测：** Sol 88.8% → **Sol Ultra 91.9%**（+3.1 分，显著提升）。

---

## 3. Pro 模式：质量优先

```python
# Pro 模式：每个层级均可用的质量优先参数（非独立模型）
response = client.responses.create(
    model="gpt-5.6-terra",           # 任何层级可用
    reasoning={
        "effort": "high",
        "mode": "pro"                # ★ 质量优先
    },
    input="..."
)
```

**Pro 模式 vs 标准模式：**

| 对比 | standard | pro |
|------|----------|-----|
| 定位 | 默认 | 质量优先 |
| 推理 | 常规深度 | 更充分推理 |
| 成本 | 标准 | 更高 |
| 适用 | 常规任务 | 高风险任务（法律/金融/代码关键路径） |

---

## 4. 推理成本与延迟权衡

```text
推理深度 = 质量 ↑ × 成本 ↑ × 延迟 ↑ 的三维权衡

成本模型（Terra $15/百万输出）：
  effort=none   → ~500 tokens/请求 → ~$0.0075
  effort=medium → ~2000 tokens → ~$0.03
  effort=high   → ~5000 tokens → ~$0.075
  effort=max    → ~15000 tokens → ~$0.225

延迟模型：
  none   → ~1s
  medium → ~5s
  high   → ~15s
  max    → ~60s+
```

**权衡决策：**
| 场景 | 延迟容忍 | 推荐 effort |
|------|:---:|:---:|
| 用户聊天 | <3s | low/medium |
| 客服分流 | <5s | medium |
| 后台批处理 | 分钟级 | high |
| 关键路径代码 | 分钟级 | xhigh |
| 科研验证 | 小时级 | max（Sol） |

---

## 5. 按场景选择推理深度

| 任务类型 | 推荐 effort | 理由 |
|----------|:---:|------|
| 意图分类/路由 | `none` | 无需推理 |
| 信息提取 | `low` | 提取是确定性任务 |
| 摘要/分析 | `medium` | 平衡质量与成本 |
| 代码审查 | `high` | 需要深入理解 |
| 多文件重构 | `xhigh` | 跨文件推理 |
| 数学证明/安全审计 | `max`（Sol） | 验证性任务 |

**生产建议：** 按请求类型动态设置 effort（而非全局固定）— 简单请求 `low`、复杂请求 `high`，可节省 50-70% 成本。

---

> 🎯 **核心要点**：推理控制的三个层次 — **① effort 六档（none→max）按任务难度分级 ② Sol 的 Max（验证）/Ultra（4-16 子 Agent）模式 ③ Pro 模式（任意层级质量优先）**。生产最优实践：按请求类型动态设置 effort，简单任务降档省 60% 成本。

**下一模块**：[04-Tools工具与多智能体](04-Tools工具与多智能体.md) / **返回总览**：[00-总览](00-GPT-API知识体系总览.md)
