# 工具声明：tools 与 JSON Schema

> 背诵篇：流程第一步——**工具怎么"告诉"模型**：tools 数组结构、schema 五铁律、描述规范、目录治理。设计规范深潜见 [Function Calling 基石 04 篇](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用%E3%80%90Agent%20基石%E3%80%91%2F04-工具定义与设计规范.md)。

## 1. 30 秒背诵卡

| 概念 | 背诵版一句话 |
|---|---|
| tools 参数 | 请求里携带的工具描述数组——模型"知道有什么可用"的唯一途径 |
| 结构 | 嵌套：`{type:"function", function:{name, description, parameters}}` |
| parameters | JSON Schema——描述参数结构、类型、约束 |
| 描述铁律 | **工具描述是准确度第一大因素**——做什么/何时用/何时不用/例子 |
| schema 五铁律 | 枚举/数值范围/字段无歧义/additionalProperties:false/参数描述 |
| 目录治理 | 活动工具集 <20 个；>30-50 选择精度线性下降 |
| 协议差异 | Chat Completions 嵌套结构 vs Responses API 扁平结构——不可混用 |
| 2026 趋势 | MCP 标准化工具声明（JSON-RPC）；描述进上下文（≈100-200 token/工具） |

## 2. 工具声明结构（背诵版代码）

```json
// Chat Completions 风格（嵌套结构）
{
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "查询指定城市的当前天气。当用户询问天气时使用。参数 city 为中国城市名。",
        "parameters": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string",
              "description": "城市名，如：上海、北京（中文）"
            },
            "unit": {
              "type": "string",
              "enum": ["celsius", "fahrenheit"],
              "description": "温度单位"
            }
          },
          "required": ["city"],
          "additionalProperties": false
        }
      }
    }
  ]
}
```

| 字段 | 作用 | 背诵要点 |
|---|---|---|
| type | 固定 `"function"` | 协议标识 |
| function.name | 工具名 | 动词开头、命名空间前缀（`github_list_prs`） |
| function.description | **模型判断何时调用的依据** | 做什么/何时用/何时不用/边界/反例 |
| parameters | 参数契约（JSON Schema） | 五铁律约束模型填参 |

## 3. Schema 五铁律（面试必背）

| 铁律 | 正确 | 错误 |
|---|---|---|
| ① 枚举不自由串 | `"enum": ["approved","rejected"]` | `"type":"string"`（模型乱填） |
| ② 数值加范围 | `"minimum":0, "maximum":10000` | `"type":"number"`（出现 amount:-50） |
| ③ 字段无歧义 | `from_account_id` / `to_account_id` | `from` / `to`（易互换） |
| ④ 收口 | `"additionalProperties": false` | 开放（模型加你没处理的字段） |
| ⑤ 参数级描述 | `"description":"ISO-8601 UTC"` | `"description":"datetime"` |

> 💡 复杂度分级（弱模型更敏感）：**扁平属性 > 嵌套对象 > 对象数组 > oneOf 联合**——弱模型（7B-13B）对嵌套提取/复杂 oneOf 出错率高；**优先单对象 + 可选属性**，避免 oneOf。

## 4. 描述怎么写（决定调用质量）

| 要素 | 内容 | 示例 |
|---|---|---|
| 做什么 | 一句话职责 | "查询指定城市当前天气" |
| 何时用 | 触发条件（When to Use） | "用户询问天气、温度时使用" |
| 何时不用 | 排除相似工具（When NOT to Use） | "查历史天气用 get_weather_history" |
| 参数含义 | 格式/边界/单位 | "city 为中文城市名，如：上海" |
| 正反例 | 常见错误模式标注 ❌ WRONG | "❌ 参数传拼音（shanghai）" |

> 🎯 核心要点（背诵版）：**工具描述是模型唯一能看到的"使用说明书"——描述质量 = 调用准确度上限**（Together AI 实证：描述是准确度第一大因素；反例标注比只给正例更有效）。

## 5. 目录治理（工具太多怎么办）

| 手段 | 机制 |
|---|---|
| 数量上限 | **活动工具集 <20 个**（>30-50 精度线性下降；LongFuncEval 实证） |
| 工具检索 | 先按任务召回相关工具（8 个相关 > 60 个大多无关） |
| 两级选择 | 先选类别再选工具（"先开应用再找功能"） |
| 合并操作 | create/update/close_ticket → `manage_ticket` + action 枚举 |
| 标签过滤 | 按权限/领域/前置条件在入 prompt 前过滤 |
| 命名空间 | `github_list_prs`、`slack_send_message` 防同名冲突 |

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 工具怎么告诉模型？ | tools 数组（JSON Schema）注入请求——模型看到描述的途径 |
| 结构长什么样？ | `{type:"function", function:{name, description, parameters}}` |
| schema 五铁律？ | 枚举/范围/无歧义/additionalProperties:false/参数描述 |
| 描述怎么写？ | 做什么+何时用+何时不用+正反例——准确度第一大因素 |
| 复杂度排序？ | 扁平 > 嵌套 > 数组 > oneOf（弱模型更敏感） |
| 工具治理？ | <20 个 + 检索 + 两级选择 + 合并 + 命名空间 |

---

**下一模块**：[03-模型决策：tool_calls 与参数](03-模型决策：tool_calls与参数.md)　**返回总览**：[00-Function-call 完整理论流程总览](00-Function-call完整理论流程总览.md)

## 参考来源

- [OpenAI Function Calling 函数调用指南（API 易文档中心）](https://docs.apiyi.com/api-capabilities/openai/function-calling)
- [Function calling best practices（Together AI）](https://docs.together.ai/docs/inference/function-calling/best-practices)
- [Ultimate Guide: How AI Agents Use Tools — 2026（skywork.ai）](https://skywork.ai/blog/ai-agents-using-tools-ultimate-guide-2026/)
- [Function Calling Definition | FutureAGI Guide (2026)](https://futureagi.com/glossary/function-calling/)
