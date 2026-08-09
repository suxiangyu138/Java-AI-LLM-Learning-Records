# Function-call 完整理论流程总览

> 定位：「LLM AI Agent 相关理论」之「Function-call」**完整理论流程背熟层**——从工具声明到循环终止的七步全流程、每个协议细节的背诵版（tool_calls/tool_call_id/tool_choice/流式 index）、常见坑与面试冲刺。与 [Function Calling 函数调用【Agent 基石】](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用%E3%80%90Agent%20基石%E3%80%91%2F00-FunctionCalling知识体系总览.md)（协议深潜 11 篇）和 [区分概念 02-函数调用-vs-工具-vs-MCP-vs-Skills](..%2F区分概念（面试容易混淆）%2F02-函数调用-vs-工具-vs-MCP-vs-Skills.md)（概念辨析）分工：本体系讲**流程怎么走、细节怎么背**。2026 一句话：**"模型只出意图（tool_calls），执行与回传是 Harness 的活——流程的每一环都有协议铁律"**。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [七步流程一图速记](#3-七步流程一图速记)
4. [学习路线推荐](#4-学习路线推荐)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Function-call 完整理论流程（背熟）
├── 01 七步流程总图：完整生命周期      声明→决策→提议→执行→回传→定稿→循环
├── 02 工具声明：tools 与 JSON Schema  嵌套结构 / 描述铁律 / <20 工具
├── 03 模型决策：tool_calls 与参数      content=None 是正常 / arguments 是 JSON 字符串
├── 04 执行与回传：tool 消息与配对      tool_call_id 精确匹配 / 必须追加两条消息
├── 05 调用循环工程：五步循环与终止     追加-判空-执行-回传 / 零调用自然出口
├── 06 高级形态：并行-强制-流式         parallel / tool_choice / index 聚合
├── 07 推理模型与函数调用              thinking 先思后调 / reasoning_content 回传
├── 08 常见坑与错误处理                十二坑 / 400 错误族 / 修复
└── 09 面试高频问答冲刺                20 题 + 答题范式 + 金句弹药库
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [七步流程总图：完整生命周期](01-七步流程总图：完整生命周期.md) | 七步、两轮模式、谁做什么 | 全部（地基） |
| 02 | [工具声明：tools 与 JSON Schema](02-工具声明：tools与JSON%20Schema.md) | 嵌套结构、描述铁律、schema 五铁律 | 全部（面试必背） |
| 03 | [模型决策：tool_calls 与参数](03-模型决策：tool_calls与参数.md) | content=None、tool_call 结构、tool_choice | 全部（面试必背） |
| 04 | [执行与回传：tool 消息与配对](04-执行与回传：tool消息与配对.md) | tool_call_id 精确匹配、追加两条消息、结果截断 | 全部（面试必背） |
| 05 | [调用循环工程：五步循环与终止](05-调用循环工程：五步循环与终止.md) | 五步循环、终止条件、错误回传 | Agent 工程师 |
| 06 | [高级形态：并行-强制-流式](06-高级形态：并行-强制-流式.md) | parallel/streaming index 聚合/工具发现 | Agent 工程师 |
| 07 | [推理模型与函数调用](07-推理模型与函数调用.md) | thinking 先思后调、reasoning_content 铁律 | 全部（面试必背） |
| 08 | [常见坑与错误处理](08-常见坑与错误处理.md) | 十二坑、400 错误族、修复对照 | 全部 |
| 09 | [面试高频问答冲刺](09-面试高频问答冲刺.md) | 20 题 + 答题范式 + 金句弹药库 | 面试前 |

## 3. 七步流程一图速记

```text
一次函数调用的完整生命周期（两轮模式，背诵版）：
  ① 工具声明  tools: [{type:"function", function:{name, description, parameters}}]
  ② 模型决策  模型看 tools + 用户消息 → 决定调不调
  ③ 提议输出  message.tool_calls = [{id, function:{name, arguments(JSON 字符串)}}]
              ——content = None 是正常信号（选了工具而非文本）
  ④ Harness 执行  解析 arguments → 校验 → 调用真实函数 → 得结果
  ⑤ 结果回传  {role:"tool", tool_call_id: <原样复制>, content: json.dumps(结果)}
  ⑥ 再推理    模型综合结果 → 再调工具 or 生成最终答案
  ⑦ 定稿与循环  无 tool_calls → 输出答案；否则回到③（直到终止条件）

铁律三条（背熟）：
  A. arguments 是 JSON 字符串——必须 json.loads 才能执行
  B. tool_call_id 必须原样复制——不匹配 = 400
  C. assistant 消息与 tool 消息都必须追加回 messages——漏一条断上下文
```

## 4. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 面试速成（1 天） | 00 → 01 → 03/04 → 08 → 09 | 能画流程、报铁律、避坑 |
| 完整背诵（2 天） | 00 → 01-05 → 06 → 08 → 09 | 七步全流程 + 高级形态 |
| 工程深潜（3 天） | 全量 + [Function Calling 基石体系](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用%E3%80%90Agent%20基石%E3%80%91%2F00-FunctionCalling知识体系总览.md) | 能手写调用循环 |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **协议两代并存**：Chat Completions（嵌套 `{"type":"function","function":{...}}`，输出 `message.tool_calls[]`，回传 `role:"tool"` + `tool_call_id`）与 Responses API（扁平结构，输出顶层 `{"type":"function_call","call_id"...}`，回传 `function_call_output`）——**两种格式不可混用**（混用是"invalid parameter"报错最常见原因）。
- **流程本质不变**：2026 年仍是两轮模式——声明→模型提议（`tool_calls`）→Harness 执行→`role:"tool"` 回传→模型定稿；模型**从不执行函数**，只输出意图；每次工具调用迭代 = 一次独立 API 请求（5 步循环 ≈ 10 次调用）。
- **流式聚合是难点**：流式下工具调用分片到达——`index` 字段标识、ID 可能缺失（需自生成 `call_<index>`）、函数名可能后到（不能把缺名当致命错）、arguments 跨块拼接、按 index 排序、跳过空名；openai-agents-python PR #3506 提供 `buffer_streamed_tool_calls` 兜底（流完再合成完整块，避免非法消息顺序）。
- **推理模型改变节奏**：thinking 先思后调（内部计划），复杂任务外部轮次可从 8-12 降到 2-3；DeepSeek 等模型 **reasoning_content 必须回传**否则 400；`finish_reason="tool_calls"` 是流式收尾判据。
- **协议细节即生产正确性**：`tool_call_id` 精确匹配（case-sensitive UUID，复制不重建）、`content` 必须是字符串（否则 TypeError）、结果要截断（"Rows 1-10 of 250"——结果字符串每轮都重新 token 化）、`tool_calls` 空列表≠None（判空用 `if message.tool_calls:`）。
- **与体系分工**：[Function Calling 基石体系](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用%E3%80%90Agent%20基石%E3%80%91%2F00-FunctionCalling知识体系总览.md) 讲协议深潜/设计规范/成本优化；[区分概念 02](..%2F区分概念（面试容易混淆）%2F02-函数调用-vs-工具-vs-MCP-vs-Skills.md) 讲函数调用 vs 工具 vs MCP vs Skills 的概念对；本体系讲**完整理论流程的背诵版与协议铁律**。

---

**下一模块**：[01-七步流程总图：完整生命周期](01-七步流程总图：完整生命周期.md)

## 参考来源

- [OpenAI Function Calling 函数调用指南（API 易文档中心）](https://docs.apiyi.com/api-capabilities/openai/function-calling)
- [None content: tool call messages（The Neural Base）](https://theneuralbase.com/openai/learn/beginner/none-content-tool-call-messages/)
- [Returning result to model | Function Calling Beginner Course（The Neural Base）](http://theneuralbase.com/function-calling/learn/beginner/returning-result-to-model/)
- [feat: add buffered Chat Completions tool-call streaming（openai-agents-python PR #3506）](https://github.com/openai/openai-agents-python/pull/3506)
- [working_with_llm_apis/05_openai/02_tool_calling.md](https://github.com/baluragala/working_with_llm_apis/blob/main/05_openai/02_tool_calling.md)
- [fix(provider): handle delayed tool call names in streamed responses（opencode PR #18623）](https://github.com/anomalyco/opencode/pull/18623)
