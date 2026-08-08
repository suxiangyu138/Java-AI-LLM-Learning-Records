# 02 Structured Output 深度

> 定位：格式保证的天花板——strict 模式原理、五条硬约束、Pydantic 集成 `.parse()`、refusal 处理（2026-08 基准）

## 📚 目录

1. [原理：约束解码在服务端](#1-原理约束解码在服务端)
2. [strict 模式的五条硬约束](#2-strict-模式的五条硬约束)
3. [Pydantic 集成：.parse() 一行搞定](#3-pydantic-集成parse-一行搞定)
4. [refusal：模型拒绝的四种形态](#4-refusal模型拒绝的四种形态)
5. [工具参数也用 strict](#5-工具参数也用-strict)
6. [兼容矩阵：DeepSeek 等非 OpenAI 怎么办](#6-兼容矩阵deepseek-等非-openai-怎么办)
7. [生产最佳实践清单](#7-生产最佳实践清单)

## 1. 原理：约束解码在服务端

Structured Outputs 不是"prompt 建议"也不是"事后校验"——**服务端把你的 JSON Schema 编译成有限状态机，采样时物理屏蔽非法 token**：

```text
普通请求:  模型自由采样 → 可能产出坏 JSON → 事后校验（补救）
strict:    schema → 状态机 → 每个位置只允许合法 token → 违规 JSON 不可能生成
```

| 指标 | 数值（2026 实测） |
|------|------------------|
| 语法合规率 | ~99.7% |
| schema 合规率 | ~99.5% |
| prompt 式"请输出 JSON"的失效概率 | ~0.4%（2026 前沿模型，满负载） |
| 首 token 延迟开销 | ~5-8%（schema 编译，之后缓存） |

> 🎯 **核心要点**：strict 的保证是**结构性的**——模型"想"输出坏 JSON 也做不到。但注意：**结构合规 ≠ 语义正确**（模型可以在合法 schema 里填错值），语义校验仍是你的责任。

## 2. strict 模式的五条硬约束

| # | 约束 | 说明 | 应对 |
|:---:|------|------|------|
| 1 | **字段全必填** | strict 拒绝可选字段 | 可选字段用 null 联合：`type: ["string", "null"]`，缺省传 `null` |
| 2 | **禁止附加属性** | `additionalProperties: false` 隐式生效 | 字段写全，别漏 |
| 3 | **顶层必须是对象** | 不能返回裸数组 | 包一层：`{"items": [...]}` |
| 4 | **无 `$ref`、无根层 `oneOf`** | schema 必须确定性 | 展开引用；避免重叠类型分支 |
| 5 | **嵌套深度限制** | 约 5 层（OpenAI） | 保持 <4 层，扁平化 |

**null-union 写法示例**：

```python
# 目标：country 可空
schema = {
    "type": "object",
    "properties": {
        "city": {"type": "string"},
        "country": {"type": ["string", "null"]},   # ★ 可选字段的 strict 写法
    },
    "required": ["city", "country"],               # ★ 全部必填（含可空字段）
    "additionalProperties": False,
}
```

## 3. Pydantic 集成：.parse() 一行搞定

SDK 自动完成"Pydantic 模型 → strict schema → 响应解析回类型"：

```python
from openai import OpenAI
from pydantic import BaseModel, Field

client = OpenAI()

class WeatherReport(BaseModel):
    city: str = Field(description="城市名")
    temperature: float
    unit: str = Field(description="celsius 或 fahrenheit")
    condition: str
    # 可选字段：Optional + None 默认值（SDK 转成 null-union）
    alert: str | None = None

response = client.beta.chat.completions.parse(
    model="gpt-4o-mini",
    messages=[{"role": "user", "content": "北京的天气？"}],
    response_format=WeatherReport,          # ★ 直接传 Pydantic 模型
)
report = response.choices[0].message.parsed   # ★ 类型化实例，无需 json.loads
print(report.city, report.temperature)        # 自动补全，无手动解析
```

| 收益 | 说明 |
|------|------|
| 无 `json.loads` | SDK 内部完成解析与类型转换 |
| 无 try/except 校验 | Pydantic 校验内置 |
| 类型安全 | IDE 自动补全、静态检查 |

**SDK 转换规则（to_strict_json_schema）**：

```text
自动设置 additionalProperties: false
所有属性强制 required（Optional 也转必填 + null 联合）
展开 $ref（解决与其它属性混用）
扁平化单元素 allOf
去掉 None 默认值
```

## 4. refusal：模型拒绝的四种形态

strict 模式引入了新信号——**拒绝（refusal）**。模型可能在四种情况下给出不合 schema 的输出：

| 形态 | 检查方式 | 处理 |
|------|---------|------|
| 内容拒绝 | `message.refusal` 非空 | 明确提示用户/降级 |
| 输出截断 | `finish_reason == "length"` | 增大 max_tokens / 精简 schema |
| 合规但残缺 | `parsed` 为 None | 用 Pydantic 校验失败信息重试 |
| 静默失败 | 语义对但结构边缘 | 校验链兜底 |

```python
msg = response.choices[0].message
if msg.refusal:
    print("模型拒绝:", msg.refusal)          # 明确处理，不静默
elif msg.parsed is None:
    print("结构解析失败，触发修复循环")        # 重试 ≤3 次（见第 7 节）
else:
    report = msg.parsed
```

> ⚠️ **常见误解**：strict 只保证"输出符合 schema"，**不保证模型一定按你的意图输出**——拒绝/截断必须显式检查。

## 5. 工具参数也用 strict

strict 同样作用于工具参数：

```python
from openai import pydantic_function_tool

class GetWeatherParams(BaseModel):
    city: str = Field(description="城市名，如 北京")
    unit: str = "celsius"   # 可选 → null-union

tools = [pydantic_function_tool(GetWeatherParams, name="get_weather",
                                description="查询指定城市的当前天气")]
```

**并行与 strict 的冲突（2026 高频坑）**：

> 需要每个工具调用精确匹配 schema 形状时，**关闭并行**（`parallel_tool_calls: false`）——交错到达的并行调用可能破坏形状保证。

## 6. 兼容矩阵：DeepSeek 等非 OpenAI 怎么办

| 供应商 | strict 等价能力 | 阶段 2 建议 |
|--------|----------------|------------|
| OpenAI | `response_format: json_schema + strict`、`.parse()` | 完整使用 |
| DeepSeek V4 | 兼容 `response_format: {"type": "json_object"}`（JSON Mode） | 用 JSON Mode + 修复循环 |
| Anthropic | 无单开关；用"单工具 + tool_choice 强制" ~99% | 服务端约束 + 解析兜底 |
| Gemini | `response_schema`（strict 保证较弱，2026 路线图对齐中） | 保守使用 + 校验 |

> 💡 **跨供应商铁律**：schema 能跑通 OpenAI strict，不代表其它家一致——**迁移每家跑一次评估**（见阶段 4 评估篇）。

## 7. 生产最佳实践清单

| 类别 | 实践 |
|------|------|
| Schema 设计 | 枚举字段用 enum；字符串加 min/maxLength；数字加 min/max；字段带 description（模型当提示用）；数组对象优于嵌套 map；always strict + additionalProperties: false |
| 错误处理 | refusal 显式处理；校验失败 → 错误信息回喂 → 重试 **≤3 次**（超 3 次模型倾向重复同一错误）；每次重试记日志 |
| 性能 | **预热 schema**（部署时发一次哑请求编译缓存）；流式 + partial JSON 解析（`jiter`/`partial-json`，实测感知延迟降 2.1×） |
| 评估 | 不只报 schema 合规率——**schema_validity × semantic_quality** 双指标（约束解码可能把细微差别折叠进 enum 默认值：合法但语义错） |
| 场景 | 创意/自由文本、未知 schema、>40-50 字段的巨型 schema → 不用 strict（拆解） |

> 🎯 **核心要点**：Structured Output 的正确姿势 = **strict 保证结构 + 校验链保证语义 + refusal 显式处理**。三层缺一不可——"结构对了就万事大吉"是 2026 年最常见的踩坑心态。

---

**返回总览**：[00-阶段总览：原生 OpenAI-style Function-call](00-阶段总览：原生%20OpenAI-style%20Function-call.md) / **上一模块**：[01-原生能力全景](01-原生能力全景.md) / **下一模块**：[03-JSON 提取模式与工具组合](03-JSON%20提取模式与工具组合.md)
