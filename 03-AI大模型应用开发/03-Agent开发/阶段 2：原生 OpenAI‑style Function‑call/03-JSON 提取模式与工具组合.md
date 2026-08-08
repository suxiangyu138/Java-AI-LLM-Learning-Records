# 03 JSON 提取模式与工具组合

> 定位：非工具场景的结构化输出——JSON Mode vs json_schema、强制调用做提取、与工具调用的组合矩阵（2026-08 基准）

## 📚 目录

1. [三条结构化路线对比](#1-三条结构化路线对比)
2. [JSON Mode（json_object）：旧方案的边界](#2-json-modejson_object旧方案的边界)
3. [强制工具调用做提取](#3-强制工具调用做提取)
4. [提取场景实战](#4-提取场景实战)
5. [组合矩阵：格式约束 × 工具调用](#5-组合矩阵格式约束--工具调用)
6. [选型决策树](#6-选型决策树)

## 1. 三条结构化路线对比

| 路线 | API 形态 | 保证强度 | 适用 |
|------|---------|:---:|------|
| Strict json_schema | `response_format: {"type": "json_schema", "strict": true}` | ★★★★★ | 新项目首选 |
| JSON Mode（旧） | `response_format: {"type": "json_object"}` | ★★★（只保证合法 JSON，不保证 schema） | 旧模型兼容 / 无 schema 场景 |
| 强制工具调用 | `tool_choice` 锁定 + 提取工具 | ★★★★（~99%） | 无 strict 支持的供应商 / 统一工具管线 |

> 🎯 **核心要点**：三种路线的本质差异是**"约束在哪一层"**——strict 在采样层（物理约束）、JSON Mode 在语法层（结构约束）、工具调用在协议层（调用约束）。约束越靠前，保证越强。

## 2. JSON Mode（json_object）：旧方案的边界

```python
response = client.chat.completions.create(
    model="gpt-4o-mini",
    messages=[
        {"role": "system", "content": "你是一个 JSON 提取器，只输出 JSON。"},  # JSON Mode 强制要求
        {"role": "user", "content": "提取：价格 12.5 元，库存 3 件"},
    ],
    response_format={"type": "json_object"},     # 仅保证合法 JSON
)
data = json.loads(response.choices[0].message.content)
```

| 特性 | 说明 |
|------|------|
| 保证 | 输出**一定是合法 JSON**（语法级） |
| 不保证 | 字段名/类型/结构——模型自定 |
| 前提 | system 消息必须含"json"字样（API 硬性要求） |
| 2026 定位 | 旧模型回退方案；新模型一律用 json_schema |

> ⚠️ **JSON Mode 的典型翻车**：字段名漂移（`price` vs `Price` vs `售价`）、类型漂移（数字变字符串）、多余嵌套——解析后仍需防御性校验。

## 3. 强制工具调用做提取

没有 strict 时（DeepSeek、Anthropic 等），把"提取"伪装成"工具调用"：

```python
# 用一个"提取工具" + tool_choice 锁定
extract_tool = {
    "type": "function",
    "function": {
        "name": "extract_order",
        "description": "从用户输入中提取订单信息。",
        "parameters": {
            "type": "object",
            "properties": {
                "price": {"type": "number", "description": "价格"},
                "quantity": {"type": "integer", "description": "数量"},
            },
            "required": ["price", "quantity"],
        },
    },
}

response = client.chat.completions.create(
    model="deepseek-v4-flash",
    messages=[{"role": "user", "content": "价格 12.5 元，库存 3 件"}],
    tools=[extract_tool],
    tool_choice={"type": "function", "function": {"name": "extract_order"}},  # ★ 锁定
)
args = json.loads(response.choices[0].message.tool_calls[0].function.arguments)
```

| 优点 | 注意 |
|------|------|
| 结构受服务端约束（~99% 合规） | 本质上仍是"模型尽力"，需解析兜底 |
| 任意供应商可用（协议通吃） | tool_choice 锁定后不会触发其它工具 |
| 与工具管线统一 | 提取结果走同一回传路径 |

## 4. 提取场景实战

**场景矩阵**：什么时候用"提取模式"而不是"Agent 工具循环"：

| 场景 | 模式 | 原因 |
|------|------|------|
| 从邮件/工单提取结构化字段 | 强制提取（单轮） | 不需要多步，一轮搞定 |
| 用户输入转 API 参数 | 强制提取 | 校验先行，再进业务 |
| 长文分段/分类标注 | strict json_schema | 结构复杂，需要精确 schema |
| 需要查库/推理的多步任务 | Agent 工具循环 | 单轮提取不够 |

**提取模式与 Agent 循环的统一**：提取 = 单轮、无回传的"瘦身版 Agent"——同协议、不同循环深度。

```python
def extract_with_schema(client, user_text, model_cls) -> dict:
    """强制提取模板：strict + parse（OpenAI），失败带错误重试 ≤3 次"""
    for attempt in range(3):
        response = client.beta.chat.completions.parse(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": user_text}],
            response_format=model_cls,
        )
        parsed = response.choices[0].message.parsed
        if parsed is not None:
            return parsed.model_dump()
        # 失败：把校验错误喂回去再试
        user_text += f"\n（上次输出不合 schema，请修正）"
    raise ValueError("提取失败：3 次尝试均不合 schema")
```

## 5. 组合矩阵：格式约束 × 工具调用

2026 年反复出现的坑：**格式约束与工具调用叠加时互相打架**。矩阵如下：

| 组合 | 行为 | 结论 |
|------|------|------|
| `response_format` + `tool_choice: auto` | strict 从第一 token 生效，**抑制工具调用**——模型输出 schema 形状的假答案 | ❌ 同一轮不要叠加 |
| `response_format` + 无工具 | 正常结构化输出 | ✅ |
| `tool_choice` 锁定 + strict 工具 schema | 工具参数受约束，调用正常 | ✅（注意 parallel 关闭） |
| strict + parallel_tool_calls | 并行交错可能破坏单调用形状保证 | ⚠️ 精确形状需求时关闭并行 |

**回合分离原则（黄金法则）**：

```text
工具循环轮次：不用 response_format（让模型自由调工具）
最终回答轮次：才应用 response_format（结构化收尾）
```

> 💡 这与失败模式体系 04 篇的"延迟结构化（Deferred Structuring）"同源——约束只作用于该作用的位置。

## 6. 选型决策树

```text
供应商支持 strict？
├─ 是 → json_schema + strict + .parse()（新项目默认）
└─ 否 → 有 schema 吗？
        ├─ 有 → 强制工具调用做提取（tool_choice 锁定）
        └─ 无 → JSON Mode（json_object）+ 防御性解析
需要与工具循环叠加？
├─ 是 → 回合分离：循环轮自由，终轮结构化
└─ 否 → 单轮提取，按上树
```

> 🎯 **核心要点**：JSON 提取的工程本质是"**把不可控的自由文本变成可控的结构**"。三条路线记住一句话——**strict 优先、工具兜底、JSON Mode 兼容**；与工具叠加时永远回合分离。

---

**返回总览**：[00-阶段总览：原生 OpenAI-style Function-call](00-阶段总览：原生%20OpenAI-style%20Function-call.md) / **上一模块**：[02-Structured Output 深度](02-Structured%20Output%20深度.md) / **下一模块**：[04-并行调用工程化](04-并行调用工程化.md)
