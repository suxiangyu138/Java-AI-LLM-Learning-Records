# 追踪：Trace 执行树

> 追踪 = 记录输入到输出的完整执行树：每个 LLM 调用、工具调用、检索步骤、子 Agent 跳转都是一个 span。2026 规范：**Agent 两类 span（create_agent/invoke_agent）**、层次树结构、conversation.id 跨 trace 关联会话。本章给 Agent 追踪的完整设计。

## 1. 追踪的层次模型

```text
Root span: user request
  ├── invoke_agent: orchestrator
  │   ├── chat: LLM call（模型、token、成本）
  │   ├── execute_tool: file_read（延迟、输出大小）
  │   └── invoke_agent: 子 Agent 交接
  │       ├── chat: 推理步骤
  │       └── execute_tool: api_call
  └── final response
```

| 层 | span 类型 | 记录 |
|---|---|---|
| 顶层 | 会话/用户轮次 span | 会话 ID、任务 |
| 中层 | 推理 span（每步规划） | 计划、决策 |
| 底层 | 工具调用 span（挂在触发它的推理 span 下） | 工具名、参数、结果、延迟 |

> 🎯 核心要点：**工具 span 是触发它的推理 span 的子节点**——归因"哪步决策导致了哪个调用"，这是失败归因的基础（LangCost 类工具的底层结构）。

## 2. Agent 两类 span（OTel GenAI 约定）

| span | 覆盖 | 属性 |
|---|---|---|
| create_agent | Agent 初始化 | agent.id、agent.name、agent.description、agent.version |
| invoke_agent | Agent 执行 | conversation.id、输入/输出类型、token 用量、温度、finish_reason |

> 💡 版本属性的价值：**agent.version 让"模型/提示升级后行为漂移"可归因**——先查版本再查代码，是 2026 调试的第一步。

## 3. GenAI span 的边界：不只是 LLM 调用

> 2026 关键洞察（Honeycomb Agent Timeline）：**GenAI span 包含 Agent 触发的所有下游工作**——数据库查询、第三方 API、后台任务，不只是模型调用。

| 必备三属性 | 作用 |
|---|---|
| gen_ai.conversation.id | 把跨 trace 的 span 绑成一个会话 |
| gen_ai.agent.name | 泳道视图与交接可见性 |
| gen_ai.operation.name | 操作类型（chat/execute_tool/invoke_agent） |

> ⚠️ **conversation.id 必须传播进下游系统 span**——否则只有 LLM 视角没有全栈视角：模型调了 API，但 API 侧发生了什么看不到。

## 4. Threads/Sessions：跨轮关联

| 概念 | 说明 |
|---|---|
| Thread | 跨多轮对话的关联 trace 组 |
| Session 级失败 | 第 1 轮识别问题、第 2 轮检索对策略、第 3 轮没应用——单 trace 看不出 |
| 会话成本 | 跨轮 token 聚合——意外账单的源头（05 篇） |
| 会话质量 | 目标是否在整段会话达成（06 篇多轮 eval） |

> 🎯 核心要点：**单 trace 分析抓不住会话级失败模式**——Threads 是跨轮分析的容器，会话是观测单位（01 篇）。追踪链路：trace 内的 span 树 + trace 间的 conversation.id 关联。

## 5. RAG 追踪：三段隔离

| span 段 | 记录 | 诊断价值 |
|---|---|---|
| Embedding | 嵌入调用（模型、token） | 嵌入质量问题 |
| Retrieval | 召回（查询、命中数、来源） | 检索质量问题 |
| Generation | 生成（模型、上下文、输出） | 生成质量问题 |

> 💡 **隔离的价值：回答差 → 查是检索差还是生成差**——三段 span 分离是 RAG 排障的分水岭（与 RAG 08 篇评估衔接：评估定位同样三段）。

## 6. 结构化的自动收益

| 收益 | 机制 |
|---|---|
| 循环检测 | 自动识别重复相同工具调用（如重复读同一文件） |
| 错误归因 | 工具调用失败挂到触发它的推理步 |
| 瓶颈定位 | 哪类 span 耗时占比最高 |
| 成本分配 | 每 span 带 token/成本（05 篇） |

> ⚠️ 2026 生产警示：**编码 Agent 事故（Copilot 1526 个 worktree、$8 重复读文件 1000 次）都有清晰 trace 特征**——重复工具调用 span 模式可自动告警（与 Tool 07 篇失败记忆互补：trace 提供证据，约束机制提供阻止）。

## 7. 插桩方式对比（2026）

| 方式 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| 框架内建 | CrewAI 等原生发 OTel | 简单 | 耦合 OTel 版本 |
| 外部库 | Traceloop/Langtrace 包 span | 解耦维护 | 碎片化风险 |
| 手动 SDK | 手写 span | 灵活 | 规模上不可维护 |
| 非侵入 eBPF | 网络层自动生成（阿里 OBI） | 零代码 | 依赖网络流量可见性 |

> 🎯 核心要点：**框架插桩器优先**（LangChain/LlamaIndex/OpenAI SDK 官方 instrumentors 补全每条代码路径）；手动 span 只适合 demo——规模上必然漏埋。

## 8. 常见误区

| 误区 | 真相 |
|---|---|
| "Trace=LLM 调用日志" | GenAI span 含下游全部工作——数据库/API/后台任务 |
| "单 trace 够分析" | 会话级失败模式要 threads——跨轮关联 |
| "工具 span 挂根上" | 挂触发它的推理 span 下——归因的前提 |
| "conversation.id 只在 Agent 层" | 必须传播进下游系统 span——全栈视角 |
| "RAG 一个 span 就够" | 三段隔离——回答差查检索还是生成 |
| "手写 span 灵活" | 规模上漏埋——框架插桩器优先 |
| "版本信息可有可无" | agent.version 是漂移归因第一步 |

## 9. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 追踪层次模型？ | 会话 span → 推理 span → 工具 span（子挂触发它的推理步） |
| 两类 Agent span？ | create_agent（初始化）/ invoke_agent（执行） |
| GenAI span 边界？ | 含 Agent 触发所有下游工作，不只模型调用 |
| 三必备属性？ | conversation.id / agent.name / operation.name |
| conversation.id 传播？ | 进下游系统 span——全栈视角 |
| Thread 是什么？ | 跨轮 trace 关联组——会话级失败分析 |
| RAG 三段隔离？ | Embedding/Retrieval/Generation——排障分水岭 |
| 结构化自动收益？ | 循环检测/错误归因/瓶颈定位/成本分配 |
| 插桩方式？ | 内建/外部库/手动/非侵入 eBPF——框架插桩器优先 |
| 循环检测例子？ | 重复读同一文件 1000 次——trace 特征明显 |
| 手动插桩问题？ | 规模上漏埋——demo 可以生产不行 |
| agent.version 价值？ | 升级后漂移归因第一步 |

---

**下一模块**：[03-OTel 语义约定与规范](03-OTel语义约定与规范.md)　**返回总览**：[00-观测&可观测组件总览](00-观测可观测组件总览.md)

## 参考来源

- [Instrumenting AI Agents for the Agent Timeline（Honeycomb）](https://www.honeycomb.io/blog/instrumenting-ai-agents-agent-timeline-opentelemetry-guide)
- [LLM App Observability with OpenTelemetry: The 2026 Setup（FutureAGI）](https://futureagi.com/blog/llm-app-observability-otel-2026/)
- [Agent Observability: How to Monitor and Evaluate LLM Agents in Production（LangChain）](https://www.langchain.com/blog/production-monitoring)
- [Monitoring multi-agent reasoning latency & token costs with OpenTelemetry（Google Cloud Trace）](https://discuss.google.dev/t/monitoring-multi-agent-reasoning-latency-token-costs-distributed-tracing-with-opentelemetry-and-google-cloud-trace/376685)
- [Best LLM Tracing Tools for Multi-Agent Systems in 2026（MLflow）](https://mlflow.org/articles/best-llm-tracing-tools-for-multi-agent-systems-in-2026/)
