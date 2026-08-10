# 00 - openai python-sdk 总览

> 定位：OpenAI 官方 Python SDK——"LLM 应用的第一依赖"——对话、流式、结构化输出、工具调用、嵌入、多模态一个客户端全包——"所有 AI 项目的第一个 import"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
openai python-sdk（本体系 11 篇）
├── 定位层：01 openai python-sdk 是什么（官方 SDK/2026 基线）
│          02 安装与快速开始（API key 三姿势/首个对话）
├── 核心层：03 对话补全与 Responses API（双 API 选型）
│          04 流式响应（SSE/增量组装/与 FastAPI 配合）
│          05 结构化输出与工具调用（json_schema/parse/tools）
├── 能力层：06 Embeddings 与向量检索（嵌入/批量/接向量库）
│          07 异步与并发（AsyncOpenAI/批量/限流）
│          08 多模态与音频（图像输入生成/语音）
├── 应用层：09 生产实践与性能（重试/缓存/降级/可观测性）
└── 验收层：10 生产实战与自测（端到端 Agent + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | sdk 是什么 | 官方 SDK 定位/2026 基线/替代品 | 认知 |
| 02 | 安装与快速开始 | API key 三姿势/首个对话 | 会调用 |
| 03 | 对话补全与 Responses | 双 API 选型/参数全解/迁移 | 会选 API |
| 04 | 流式响应 | stream/SSE/增量组装/FastAPI | 会流式 |
| 05 | 结构化与工具 | json_schema/parse/tools 循环 | 会结构化 |
| 06 | Embeddings | 嵌入/批量/接向量库 | 会嵌入 |
| 07 | 异步与并发 | AsyncOpenAI/批量/限流 | 会异步 |
| 08 | 多模态与音频 | 图像输入生成/语音 | 会多模态 |
| 09 | 生产实践 | 重试/缓存/降级/可观测 | 会生产 |
| 10 | 实战与自测 | 端到端 Agent + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 Function Calling 体系（`../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md`）的分工**：那个体系讲**工具调用协议与循环工程**（跨供应商、原理层），本体系讲 **SDK 里的具体写法**（tools 参数、tool_calls 解析、回传）——"**协议课讲道理，SDK 课讲代码**"（05 篇）。

**与 LiteLLM（`../../../02-大模型基础与Prompt工程/LiteLLM%20多模型适配/00-LiteLLM知识体系总览.md`）的分工**：openai SDK 是**官方直连**（单一供应商、最全特性），LiteLLM 是**多供应商适配层**（100+ 供应商统一接口）——"**单供应商项目用官方 SDK，多供应商/网关场景换 LiteLLM——语法同源，换库换一行**"。

**与 LangChain（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/LangChain/00-LangChain总览.md`）、LlamaIndex（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/llama‑index/00-LlamaIndex总览.md`）的分工**：框架内部都封装 openai SDK——**"SDK 是底层 API，框架是上层编排——裸 SDK 是理解一切 LLM 框架的地基"**（03/05 篇与框架篇交叉）。

**与 chromadb（`../../向量%20&%20文档处理/chromadb/00-chromadb总览.md`）、faiss-cpu（`../../向量%20&%20文档处理/faiss‑cpu%20%20faiss‑gpu/00-faiss-cpu总览.md`）的分工**：SDK 的 `embeddings.create` 产出向量 → 向量库入库检索——**"SDK 管'文本→向量'，向量库管'向量→相似'"**（06 篇链路打通）。

**与 FastAPI（`../FastAPI/00-FastAPI知识体系总览.md`）的分工**：FastAPI 管 Web 层，SDK 管 LLM 层——"**FastAPI 接请求、SDK 接模型，流式响应在两层之间的正确姿势**是生产 AI 应用的标配组合"（04/10 篇）。

**2026-08 基线**：openai python-sdk **2.53.0**（2026-08 初，2.x 系列 2026 年约两周一个版本）；**关键认知**：**Assistants API 已标记弃用**（v2.16.0 起，2026 上半年 sunset）；**Responses API 是新项目推荐**（服务端会话状态、内置工具、缓存命中率提升 40-80%），**Chat Completions 官方承诺无限期支持**（行业标准接口）；2.51.0（2026-07-30）新增 **fast tier** 支持；2.32.0 起支持 websocket 事件处理器——"**2026 年新项目默认 Responses，存量 chat.completions 不急着迁——两个都会用才是完整姿势**"。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇动手——**毕业标准：独立实现"流式对话 + 工具调用 + 结构化输出 + FastAPI 发布"的智能问答 Agent**。

**路线二：速成路线（1-2 天）**——01 → 02 → 04 → 05 → 10（跳过 03/06/07/08/09 精读）——适合已有 LLM 应用经验、只想把 SDK 用熟的人。

**路线三：项目驱动路线**——AI 项目遇到"流式/工具/结构化/限流"按需查篇——**"chat.completions.create 一行上手，难点全在流式组装与生产治理——遇到再查"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| OpenAI() | 客户端：key/超时/重试的配置入口 | 02 |
| chat.completions.create | 对话补全（行业标准接口） | 03 |
| responses.create | Responses API（2026 新项目推荐） | 03 |
| messages / input | 对话历史（角色：developer/system/user/assistant/tool） | 03 |
| stream=True | 流式：SSE 逐 token 返回 | 04 |
| delta | 流式增量（chunk.choices[0].delta.content） | 04 |
| response_format | 结构化输出（json_schema/json_object） | 05 |
| parse() | Pydantic 解析封装（message.parsed） | 05 |
| tools / tool_calls | 工具定义与调用（finish_reason=="tool_calls"） | 05 |
| embeddings.create | 文本 → 向量（接向量库） | 06 |
| AsyncOpenAI | 异步客户端（await/并发） | 07 |
| max_completion_tokens | o 系模型总 token 上限（取代 max_tokens） | 03 |
| RateLimitError | 429 限流异常（SDK 自动重试 2 次） | 09 |

## 6. 常见误区

**误区一：SDK 只是"HTTP 封装"**——它内置**重试退避、类型化异常、流式解析、Pydantic 校验**——"裸 HTTP 你要写的东西，SDK 全包了"（01 篇）。

**误区二：chat.completions 是唯一的 API**——2026 年**新项目推荐 Responses**（状态管理/内置工具/缓存提升）；chat 无限期支持但算"标准路线"——"**两个都会用才是 2026 姿势**"（03 篇）。

**误区三：Assistants API 还能用**——**已弃用**，2026 上半年 sunset——老教程的 Assistants 代码别再学（03 篇）。

**误区四：API key 写进代码**——泄露 = 被盗刷；**环境变量/配置文件**，永不入 git（09 篇）。

**误区五：流式 chunk 不做空判断**——`chunk.choices` 可能为空列表（最后一个 chunk），直接取 `.delta` 会崩——"**流式解析的第一课是空判断**"（04 篇）。

**误区六：max_tokens 打天下**——o 系列推理模型要 **max_completion_tokens**（含推理 token）；老参数在推理模型上行为诡异（03 篇）。

**误区七：循环里新建 Client**——Client 是**重量级对象**（连接池/重试器），进程内建一次复用——"**一个进程一个 Client**"（02/09 篇）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装 SDK，环境变量配 key，跑通首个对话 |
| 2 | 03 | chat 与 responses 各写一遍，对比参数与响应结构 |
| 3 | 04 | 流式对话 + 增量组装；接 FastAPI 做 SSE |
| 4 | 05 | 结构化输出 + 工具调用循环（查询类工具） |
| 5 | 06 + 07 | embeddings 接 chromadb；AsyncOpenAI 并发批量 |
| 6 | 08 + 09 | 图像输入/语音一次；重试与限流实战 |
| 6-7 | 10 自测 + 面试 | 端到端 Agent 全链路，跑 20 题 |

## 8. 快速自测 10 题

1. SDK 相比裸 HTTP 提供了哪四类价值？
2. 2026-08 版本基线？Assistants/Responses/Chat 三者的状态？
3. API key 三种配置姿势？为什么永不入代码？
4. chat 与 responses 的参数映射（messages/input、max_tokens/max_output_tokens）？
5. 流式 chunk 为什么必须空判断？增量怎么组装？
6. 结构化输出的两种姿势？parse() 的 message.parsed 是什么？
7. 工具调用的完整循环？finish_reason 的作用？
8. AsyncOpenAI 与 OpenAI 的关系？并发批量怎么控制限流？
9. SDK 默认重试策略？429 异常类叫什么？
10. embeddings 与向量库的链路？"SDK 管转换、向量库管检索"？

## 9. 参考来源

- [openai/openai-python GitHub 官方仓库（Release/CHANGELOG）](https://github.com/openai/openai-python)
- [openai PyPI 页面（2.53.0 版本信息）](https://pypi.org/project/openai/)
- [OpenAI 官方文档（Responses/Chat/Embeddings/多模态）](https://platform.openai.com/docs)
- [Responses API 迁移指南（参数映射）](https://learn.microsoft.com/en-us/azure/developer/ai/how-to/azure-openai-to-responses)
- [Responses API 官方参考](https://platform.openai.com/docs/api-reference/responses)
- [Python SDK 参数与配置说明（DeepWiki）](https://deepwiki.com/openai/openai-python/4.1.1-parameters-and-configuration)

---

**下一模块**：[01-openai-python-sdk是什么.md](01-openai-python-sdk是什么.md)
