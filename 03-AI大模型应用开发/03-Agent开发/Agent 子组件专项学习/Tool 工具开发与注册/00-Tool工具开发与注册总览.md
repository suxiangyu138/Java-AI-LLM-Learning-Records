# Tool 工具开发与注册知识体系总览

> 定位：Agent 子组件专项之「工具组件」——[四大核心组件](..%2F..%2FAgent%20四大核心组件%2F06-工具组件：Function-Calling与工具注册.md)工具组件的深化篇：从工具注册表讲到契约设计、动态发现、执行循环、结果反馈与安全治理。2026 年核心共识：**工具是契约优先、代码其次**——LLM 只见描述不见实现，工具数量与描述质量直接决定 Agent 上限；注册从"静态塞入"走向"运行时发现"。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Tool 工具开发与注册
├── 01 工具全景：Agent 的能力边界          工具 vs 函数调用 vs MCP / 分类 / 三阶段
├── 02 工具注册：注册表与发现机制          注册表设计 / 冲突检测 / 静态 vs 动态
├── 03 工具定义：JSON Schema 契约设计       三要素 / 描述黄金法则 / strict / token 预算
├── 04 工具声明与协议：从提示词到原生协议   提示词 JSON vs 原生 FC / 双协议对照 / 统一适配
├── 05 工具发现：tool_search 与动态加载     defer_loading / namespace / 数量治理
├── 06 工具执行：执行器与调用循环           五步循环 / 并行 / 结构化错误 / 预算
├── 07 工具结果处理：反馈回路设计           结果注入 / 截断 / 摘要 / 重复失败检测
├── 08 工具安全：权限、注入与治理           tool poisoning / 审批流 / 最小权限 / 审计
├── 09 框架与生态：从注解到 MCP             Spring AI / LangChain4j / MCP 架构模式
└── 10 生产实践与面试冲刺                   12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [工具全景：Agent 的能力边界](01-工具全景：Agent的能力边界.md) | 工具三阶段、分类、与 FC/MCP 分工 | 全部（地基） |
| 02 | [工具注册：注册表与发现机制](02-工具注册：注册表与发现机制.md) | 注册表数据结构、冲突检测、静态 vs 动态 | Agent 工程师 |
| 03 | [工具定义：JSON Schema 契约设计](03-工具定义：JSON-Schema契约设计.md) | 三要素、描述黄金法则、strict、token 预算 | 全部（核心） |
| 04 | [工具声明与协议：从提示词到原生协议](04-工具声明与协议：从提示词到原生协议.md) | 提示词 JSON vs 原生 FC、双协议对照 | Agent 工程师 |
| 05 | [工具发现：tool_search 与动态加载](05-工具发现：tool-search与动态加载.md) | defer_loading、namespace、数量治理 | 落地开发者 |
| 06 | [工具执行：执行器与调用循环](06-工具执行：执行器与调用循环.md) | 五步循环、并行、结构化错误、预算 | 落地开发者 |
| 07 | [工具结果处理：反馈回路设计](07-工具结果处理：反馈回路设计.md) | 结果注入、截断、摘要、重复失败检测 | Agent 工程师 |
| 08 | [工具安全：权限、注入与治理](08-工具安全：权限-注入与治理.md) | tool poisoning、审批流、最小权限 | 架构师/安全 |
| 09 | [框架与生态：从注解到 MCP](09-框架与生态：从注解到MCP.md) | Spring AI/LangChain4j、MCP 架构模式 | Java 工程师 |
| 10 | [生产实践与面试冲刺](10-生产实践与面试冲刺.md) | 12 避坑、面试题、落地清单 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 03 → 06 → 10 | 能开发第一个可用的工具 |
| 进阶（1 周） | 01-02 → 03 → 04 → 06-07 → 10 | 能设计注册表、契约与执行循环 |
| 高级（2 周） | 全量 + 05 → 08 → 09 | 能治理大工具生态、做安全加固 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 工具（Tool） | Agent 调用的外部能力：模型不执行，只发出结构化请求 |
| 三要素 | name + description + JSON Schema 参数契约（LLM 只见这三样） |
| 工具注册（Registration） | 把工具能力登记进注册表，供模型选择与运行时查找 |
| 注册表（Registry） | name → 定义+执行器的映射；冲突检测、版本、权限元数据 |
| 工具发现（Discovery） | 从工具目录中找出当前任务需要的工具（静态声明 vs 动态检索） |
| tool_search | OpenAI Responses API 的运行时工具发现：defer_loading + 按需加载 |
| namespace | 工具分组：模型只见组名与描述，组内定义按需加载（≤10 个/组） |
| JSON Schema | 参数契约的行业标准互转格式（OpenAI `parameters`/Anthropic `input_schema`/MCP `inputSchema`） |
| strict 模式 | 强制生成符合 schema 的参数：全 required + additionalProperties:false |
| 描述黄金法则 | 描述决定"何时调用"，比实现重要；含示例提升 ~25% 准确率 |
| 工具数量上限 | <20 个建议、30 个硬拒绝；Haiku 级模型 10-15 个/上下文准确率边界 |
| 调用循环（Tool Loop） | 生成请求 → 校验 → 授权 → 执行 → 结果回注，直到终止 |
| 循环不变量 | 每个 tool_call 恰好一个结果；tool_calls 消息先于结果追加 |
| 结构化错误返回 | 失败转成模型可读的 observation（invalid_arguments/unknown_tool），而非抛异常 |
| 预算（Budget） | max_iterations 15-25、max_tool_result_chars、成本上限等硬停止机制 |
| 重复失败检测 | 记录失败签名并约束后续动作（Narrowing 的 checkProposal/recordOutcome） |
| Tool poisoning | 恶意工具描述/结果劫持模型选择的攻击（DREAD 9.0/10 第一威胁） |
| MCP | 工具分发协议：一次定义、多宿主可调（10 个官方 SDK） |
| 确认模式 | 高风险副作用前的人工审批（CVE-2025-53773 曾移除确认步骤） |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **契约优先共识**：工具 = 契约（描述+schema），代码其次——LLM 永远看不见实现。描述含示例提升 ~25% 准确率；工具数 <20（30 硬拒绝）；Haiku 级模型单上下文 10-15 个工具即准确率边界；模糊描述导致选错工具、缺参数描述导致幻觉参数。
- **注册从静态到动态**：OpenAI Responses API `tool_search`（GPT-5.4/5.5）支持 `defer_loading` + namespace 按需加载，hosted（服务端搜）与 client-executed（应用搜）双模式；Google ADK 提出 Context-Aware Polymorphic Schema Validation（Gemini 3 Flash）：schema 放注册中心，模型先识别意图再只加载相关描述符，减 token、防"注意力弥散"。
- **MCP 成为分发标准**：10 个官方 SDK（TS/Python/Go/C#/Java/Kotlin/Swift/Rust/Ruby/PHP）；`inputSchema` 一次定义多宿主调用；MCP Inspector 为官方调试工具；服务器架构五模式（Resource Gateway/Tool Orchestrator/Stateful Session/Proxy Aggregator/Domain Adapter）；2026 路线图：agent-to-agent、富媒体、无状态协议扩展。
- **安全威胁升级为第一等**：tool poisoning 在五组件威胁建模中 DREAD 9.0/10 排名第一；6/7 主流 MCP 客户端对工具元数据不做静态校验；Unicode 隐形注入对 Claude 3.5 成功率 87.5%；MSRC 2026-05 确认 prompt injection → RCE 链路；2026 SoK（78 研究）：42 种攻击技术、自适应攻击下 85%+ 成功率、18 项防御均值 <50% 缓解——防御必须架构化（注册期静态校验 + 审批流 + 行为异常检测 + 审计）。
- **执行循环工程化**：循环不变量（每个调用恰一结果、消息顺序）、结构化错误返回（坏参数不中断整轮）、重试只针对安全失败、max_iterations 15-25 + early stopping 模式、max_tool_result_chars 硬截断、turn-atomic pruning（整轮剪除保 tool-call/result 配对）；重复失败检测（Narrowing 三 API，blame 分类区分 Agent 故障与基础设施故障）。
- **Java 框架成熟**：Spring AI `@Tool/@ToolParam`（ToolCallbacks.from()）与 LangChain4j `@Tool/@P`（AiServices.tools()）双注解路线；并发执行、错误处理器、动态 ToolProvider 均为标配；LangGraph4j 双框架集成，`internalToolExecutionEnabled(false)` 可接管执行路由。
- **与体系分工**：[四大核心组件](..%2F..%2FAgent%20四大核心组件%2F06-工具组件：Function-Calling与工具注册.md) 06 篇讲工具组件概览与 FC 接入；[Function Calling 函数调用【Agent 基石】](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用【Agent%20基石】%2F00-FunctionCalling知识体系总览.md) 讲 API 协议层（流式/成本/多模型）；[Output Parser 07 篇](..%2FOutput%20Parser%20输出解析器%2F07-函数调用与结构化输出：工具参数解析.md) 讲参数解析校验；本体系深潜"工具本体"（注册/契约/发现/执行/结果/安全/生态）。

---

**下一模块**：[01-工具全景：Agent 的能力边界](01-工具全景：Agent的能力边界.md)

## 参考来源

- [Tool Calling Schema Standards（agentpatterns）](https://github.com/agentpatterns-ai/website/blob/main/standards/tool-calling-schema-standards.md)
- [Ultimate Guide: How AI Agents Use Tools — 2026（Skywork）](https://skywork.ai/blog/ai-agents-using-tools-ultimate-guide-2026/)
- [Use tool search with the Azure OpenAI Responses API（Microsoft Learn）](https://learn.microsoft.com/zh-cn/azure/foundry/openai/how-to/tool-search)
- [Google outlines runtime schema checks for AI agents（IT Brief）](https://itbrief.in/story/google-outlines-runtime-schema-checks-for-ai-agents)
- [MCP Server Complete Guide for Developers (2026)（Cosmic JS）](https://www.cosmicjs.com/blog/mcp-server-complete-guide)
- [The MCP Ecosystem in 2026（StraySpark）](https://www.strayspark.studio/blog/mcp-ecosystem-2026-strayspark-landscape)
- [Model Context Protocol Threat Modeling and Analyzing Vulnerabilities to Prompt Injection with Tool Poisoning（arXiv 2603.22489）](https://www.semanticscholar.org/paper/Model-Context-Protocol-Threat-Modeling-and-to-with-Huang-Huang/c17678b39d95c7d80e0bca0dcebfe72ad8d30395)
- [ADR-107: Agent-loop context-window management（TYPO3）](https://docs.typo3.org/p/netresearch/nr-llm/main/en-us/Adr/Adr107ContextWindowManagement.html)
- [The Anatomy of an Agent Loop（Steve Kinney）](https://stevekinney.com/writing/agent-loops)
- [Tool Management and Execution（LangGraph4j DeepWiki）](https://deepwiki.com/langgraph4j/langgraph4j/4.2-tool-management-and-execution)
