# 06 SDK 高级参数全解

> 定位：把 `chat.completions.create` 的每个参数用明白——采样控制、确定性、成本、thinking 组合（2026-08 基准）

## 📚 目录

1. [参数全景表](#1-参数全景表)
2. [采样参数：temperature 与 top_p](#2-采样参数temperature-与-top_p)
3. [确定性控制：seed 与输出稳定性](#3-确定性控制seed-与输出稳定性)
4. [长度与截断：max_tokens 三件套](#4-长度与截断max_tokens-三件套)
5. [重复抑制：频率惩罚与存在惩罚](#5-重复抑制频率惩罚与存在惩罚)
6. [成本相关参数](#6-成本相关参数)
7. [DeepSeek 专属：thinking 与兼容参数](#7-deepseek-专属thinking-与兼容参数)
8. [参数组合决策表](#8-参数组合决策表)

## 1. 参数全景表

| 参数 | 作用 | 默认 | 阶段 1 用过？ |
|------|------|:---:|:---:|
| `model` | 模型选择 | 必填 | ✅ |
| `messages` | 对话历史 | 必填 | ✅ |
| `tools` / `tool_choice` | 工具声明与策略 | 无/auto | ✅ |
| `temperature` | 随机性（0-2） | 1.0 | ✅（未深究） |
| `top_p` | 核采样截断（0-1） | 1.0 | ❌ |
| `max_tokens` | 输出上限 | 无 | ❌ |
| `max_completion_tokens` | 含推理 token 的输出上限 | 无 | ❌ |
| `seed` | 随机种子（确定性） | 无 | ❌ |
| `frequency_penalty` | 重复惩罚（-2~2） | 0 | ❌ |
| `presence_penalty` | 话题惩罚（-2~2） | 0 | ❌ |
| `stream` | 流式 | false | ✅ |
| `parallel_tool_calls` | 并行工具 | true | ❌ |
| `response_format` | 结构化输出 | 无 | ❌ |
| `stop` | 停止序列 | 无 | ❌ |
| `timeout` / `max_retries` | 网络健壮性 | SDK 默认 | ✅ |

> 🎯 **核心要点**：90% 场景只需要默认值 + temperature。其余参数是**按需武器**——本章目标是"知道什么时候该动它们"。

## 2. 采样参数：temperature 与 top_p

### 2.1 temperature：随机性旋钮

| 值 | 行为 | 适用 |
|:---:|------|------|
| 0-0.3 | 近乎确定 | 提取、分类、工具调用参数、代码 |
| 0.5-0.8 | 平衡 | 常规对话、总结 |
| 0.9-1.5 | 高随机 | 创意写作、头脑风暴 |

### 2.2 top_p：核采样（替代或叠加）

```text
top_p=0.1 → 只从概率最高的 10% token 里采样（更保守）
top_p=1.0 → 全量采样（默认）
```

> ⚠️ **官方建议**：**temperature 与 top_p 二选一调整，不要同时调**（改变其一即可，同时改引入不确定交互）。

### 2.3 工具调用场景的铁律

```text
工具调用（参数生成）→ temperature 低（0-0.3）或 0
原因：参数填错 = 工具执行失败；确定性优先于花样
```

## 3. 确定性控制：seed 与输出稳定性

```python
response = client.chat.completions.create(
    model="gpt-4o-mini",
    messages=messages,
    seed=42,               # 相同输入 + 相同 seed → 尽量相同输出
    temperature=0.2,
)
```

| 事实 | 说明 |
|------|------|
| 确定性是"尽力而为" | 同 seed 下输出高度一致，但**不保证 100%**（服务端负载/更新影响） |
| 用途 | 回归测试（同一 prompt 对比版本差异）、复现问题 |
| 局限 | seed 在 Responses API 中已移除；多供应商无此参数 |

> 💡 **测试用法**：调 prompt 前后用同 seed 跑回归集——输出 diff 直观反映 prompt 影响。

## 4. 长度与截断：max_tokens 三件套

| 参数 | 作用 | 陷阱 |
|------|------|------|
| `max_tokens` | 输出 token 上限（不含推理） | 旧参数；推理模型下与 max_completion_tokens 冲突 |
| `max_completion_tokens` | 含推理 token 的总上限 | **推理模型必须用它**（reasoning 也占额度） |
| `stop` | 停止序列（字符串数组） | 可配合结构化输出收尾 |

**截断 = 格式错误的隐藏入口**（阶段 1 已埋的坑）：

```text
max_tokens 太小 → 输出被截断 → JSON 缺闭合括号 → 解析器报"格式错误"
诊断：先查 finish_reason == "length"，再查 JSON 本身
```

**设置原则**：输出预留窗口的 5-15%；推理模型按"推理 + 回答"总量预算。

## 5. 重复抑制：频率惩罚与存在惩罚

| 参数 | 机制 | 适用 |
|------|------|------|
| `frequency_penalty` | 按**已出现频率**惩罚重复 token（-2~2） | 长文生成防车轱辘话 |
| `presence_penalty` | 按**是否出现过**惩罚（-2~2） | 鼓励话题多样性 |

```text
❌ 常见误区：工具调用/结构化输出场景开惩罚
   → 惩罚扰动 token 分布，可能破坏格式稳定性
✅ 正确场景：创意长文、聊天机器人的表达多样性
```

> ⚠️ **工具场景一律 0**——重复抑制与"精确生成工具参数"目标冲突。

## 6. 成本相关参数

| 手段 | 效果 |
|------|------|
| `model` 降档 | flash/nano 级模型处理简单任务 |
| `max_completion_tokens` 上限 | 防"话痨攻击"（无限输出） |
| `stream` + 用户取消 | 中断生成即省 token |
| Prompt Caching 前缀稳定 | 命中缓存成本降 50-90% |
| 动态工具过滤 | 每轮只发相关工具定义（省 8-20% 输入） |

> 💡 成本三问：**这个任务需要旗舰模型吗？输出上限设了吗？缓存前缀稳吗？**

## 7. DeepSeek 专属：thinking 与兼容参数

| 参数 | 位置 | 说明 |
|------|------|------|
| `thinking_mode` | `extra_body={"thinking_mode": "thinking"}` | non-thinking（默认）/ thinking / thinking_max |
| `reasoning_content` | 响应字段 | thinking 时出现；多轮回传（阶段 1 已学） |
| `strict: true`（工具 schema） | tools 声明内 | V4-Flash 并发调用推荐开启 |
| `response_format: json_object` | OpenAI 兼容 | DeepSeek 的 JSON Mode 等价物 |

**thinking 与采样参数的组合**：

```text
思考轮（planning）      → thinking_max + temperature 低（0-0.3）
执行轮（tool 调用）     → non-thinking（快、便宜、省缓存）
终轮（回答组织）        → 按需 thinking
```

> 💡 2026 实践：**thinking 按需开**——不是每轮都开。规划/综合轮开，工具轮关（省 30-50% 成本与延迟）。

## 8. 参数组合决策表

| 场景 | temperature | top_p | 惩罚 | max_tokens | 其它 |
|------|:---:|:---:|:---:|:---:|------|
| 工具参数生成 | 0-0.3 | 默认 | 0 | 充足 | parallel 按需 |
| 结构化提取 | 0-0.2 | 默认 | 0 | 充足 | strict/parse |
| 常规对话 | 0.7 | 默认 | 0 | 默认 | — |
| 创意写作 | 0.9-1.2 | 0.9+ | 0.3-0.6 | 按需 | — |
| 长文防重复 | 0.7 | 默认 | freq 0.3-0.5 | 按需 | — |
| 回归测试 | 0 | 默认 | 0 | 固定 | seed 固定 |

> 🎯 **核心要点**：高级参数的正确心智——**默认值就是最佳实践，改参数要能说出理由**。工具与提取场景"低温度"是唯一的高频改动；惩罚类参数与工具场景冲突；seed 是测试工具不是生产魔法。

---

**返回总览**：[00-阶段总览：原生 OpenAI-style Function-call](00-阶段总览：原生%20OpenAI-style%20Function-call.md) / **上一模块**：[05-流式全组合](05-流式全组合.md) / **下一模块**：[07-Responses API：新路线全解](07-Responses%20API：新路线全解.md)
