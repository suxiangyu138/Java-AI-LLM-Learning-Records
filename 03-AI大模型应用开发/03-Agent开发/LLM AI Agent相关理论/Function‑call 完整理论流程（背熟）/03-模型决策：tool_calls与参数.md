# 模型决策：tool_calls 与参数

> 背诵篇：流程第②③步——**模型怎么"提议"调用**：tool_calls 结构、content=None 语义、arguments 解析、tool_choice 四值。协议细节深潜见 [Function Calling 基石 02 篇](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用%E3%80%90Agent%20基石%E3%80%91%2F02-API协议全解.md)。

## 1. 30 秒背诵卡

| 概念 | 背诵版一句话 |
|---|---|
| 决策依据 | 工具描述（何时用）+ 用户意图（要不要）——两者匹配才调 |
| tool_calls | 模型提议的调用数组——`[{id, function:{name, arguments}}]` |
| content=None | **正常信号**——模型选择了工具而非文本（不是错误） |
| arguments | **JSON 字符串**——必须 json.loads 才能执行 |
| id | case-sensitive UUID——回传时原样复制 |
| 空列表判空 | `tool_calls` 是空列表而非 None——用 `if message.tool_calls:` 判断 |
| tool_choice | 四值：auto / none / required / 指定函数（强制调用） |
| finish_reason | `"tool_calls"` = 本轮以工具提议收尾（流式判据） |

## 2. 模型决策逻辑（理论）

```text
模型的决策过程（理论模型）：
  ① 读工具描述（schema 进上下文）
  ② 判断用户意图是否匹配某个工具
     匹配 → 生成 tool_calls（选工具 + 填参数）
     不匹配 → 不调（content 正常出文本）——工具不是必须用的
  ③ 多工具可并行 → parallel_tool_calls（无依赖时）
  ④ 无法判断 → 澄清问题（Agent 场景）或按 tool_choice 策略
```

> 🎯 核心要点（背诵版）：**调用工具是模型的权利，不是义务**——`content=None + tool_calls` 表示"我选工具"，`content=文本 + 空 tool_calls` 表示"我直接答"；Harness 必须两种都处理（判空铁律：`if message.tool_calls:`，不要用 `is not None`）。

## 3. 响应结构（背诵版代码）

```python
# 模型返回（Chat Completions）
response.choices[0].message
# → content: None                     # 选工具时是 None（正常！）
# → tool_calls: [                     # 提议的调用列表
#     {
#       "id": "call_abc123",           # 唯一 ID（回传时原样复制）
#       "function": {
#         "name": "get_weather",
#         "arguments": '{"city":"上海"}'  # ★ JSON 字符串，必须解析
#       }
#     }
#   ]
# → role: "assistant"
```

| 字段 | 类型 | 铁律 |
|---|---|---|
| message.content | str 或 None | None = 选了工具（正常） |
| message.tool_calls | list | 空列表 ≠ None——用 `if msg.tool_calls:` 判空 |
| tool_call.id | str | 回传精确匹配（不重建） |
| tool_call.function.name | str | 必须在已声明工具中 |
| tool_call.function.arguments | **str（JSON）** | `json.loads()` 后校验再执行 |

## 4. tool_choice：四种决策模式

| 值 | 行为 | 适用 |
|---|---|---|
| `auto`（默认） | 模型自主决定调/不调 | 绝大多数场景 |
| `none` | 禁止调用（tools 白给） | 只需要结构化输出/防误调 |
| `required` | 必须调一次（强制） | 路由/提取场景——强制走工具出口 |
| `"function_name"` | 指定必须调某工具 | 单工具任务（强约束） |

> 💡 高级用法（面试加分）：**`required`/指定函数 + 严格参数校验 = "结构化输出"的替代实现**——用工具协议强制模型走 JSON 出口（比 response_format 更灵活，可并行关闭）。注意：强制调用在并行模式下会失效（strict + 并行组合需验证）。

## 5. 面试速记

| 问题 | 一句话答案 |
|---|---|
| content=None 是错误吗？ | 不是——是"模型选了工具"的正常信号 |
| arguments 是什么类型？ | JSON 字符串——必须 json.loads 再执行 |
| 怎么判空？ | `if message.tool_calls:`（空列表不是 None） |
| tool_choice 四值？ | auto / none / required / 指定函数 |
| 模型一定调工具吗？ | 不一定——调用是权利不是义务，工具不匹配就直答 |
| 什么时候强制调用？ | 路由/提取——required 走工具出口 |

---

**下一模块**：[04-执行与回传：tool 消息与配对](04-执行与回传：tool消息与配对.md)　**返回总览**：[00-Function-call 完整理论流程总览](00-Function-call完整理论流程总览.md)

## 参考来源

- [None content: tool call messages（The Neural Base）](https://theneuralbase.com/openai/learn/beginner/none-content-tool-call-messages/)
- [OpenAI Function Calling 函数调用指南（API 易文档中心）](https://docs.apiyi.com/api-capabilities/openai/function-calling)
- [working_with_llm_apis/05_openai/02_tool_calling.md](https://github.com/baluragala/working_with_llm_apis/blob/main/05_openai/02_tool_calling.md)
