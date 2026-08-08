# 08 跨框架能力对比：HITL、持久化、可观测、MCP

> 定位：决定"换不换框架"的四个关键能力维度的深度对比——人工介入、持久化、可观测、工具生态（2026-08 基准）

## 📚 目录

1. [对比方法论](#1-对比方法论)
2. [维度一：Human-in-the-Loop 人工介入](#2-维度一human-in-the-loop-人工介入)
3. [维度二：持久化与会话恢复](#3-维度二持久化与会话恢复)
4. [维度三：可观测性](#4-维度三可观测性)
5. [维度四：MCP 与工具生态](#5-维度四mcp-与工具生态)
6. [维度五：性能成本特征](#6-维度五性能成本特征)
7. [生产就绪度矩阵](#7-生产就绪度矩阵)

## 1. 对比方法论

```text
选框架先比"能力深度"再比"API 风格"：
  四个维度决定架构可能性——框架在这些维度上的深度，
  决定了某些需求是"一行配置"还是"自己造轮子"

评分口径：原生一等公民（★★★）/ 有机制需搭（★★）/ 无原生自建（★）
```

## 2. 维度一：Human-in-the-Loop 人工介入

| 框架 | 等级 | 机制 | 备注 |
|------|:---:|------|------|
| LangGraph | ★★★ | `interrupt()` + `Command(resume=...)` + interrupt_before/after；四决策（approve/reject/edit/respond） | "暂停等人"是图的一等概念；恢复从精确检查点继续 |
| Claude Agent SDK | ★★★ | hooks（PreToolUse 可短路）+ `canUseTool` 回调 + `AskUserQuestion` 工具 + permission_mode | 交互应用的批准回调最自然 |
| OpenAI Agents SDK | ★★ | Guardrails + 人工 Agent（handoff 给人）+ tripwire | 有护栏无"暂停恢复"原语；人工是"另一个 Agent" |
| Pydantic AI | ★★ | deps 注入回调 + Hooks Capability | 机制有，要自己编排流程 |
| Smolagents | ★ | 无原生 | 回调自己搭 |

> 🎯 **结论**：审批型业务（退款/发信/删数据）优先 LangGraph 或 Claude Agent SDK；其余框架要用工程手段补（工程化模块 08 篇审批门）。

## 3. 维度二：持久化与会话恢复

| 框架 | 等级 | 机制 | 生产注意 |
|------|:---:|------|---------|
| LangGraph | ★★★ | checkpointer（PostgresSaver 生产标准）；时间旅行、get_state_history | resume 必须同 thread_id 同 config |
| OpenAI Agents SDK | ★★★ | Sessions 多后端：SQLite/Redis/SQLAlchemy/加密/托管 | 会话连续性自动；长期记忆无 |
| Claude Agent SDK | ★★★ | session_id resume/fork + session_store 适配器（跨无状态容器） | 本地磁盘先写，ephemeral 要配 CLAUDE_CONFIG_DIR |
| Pydantic AI | ★★ | message_history 跨调用 | 轻量；断点恢复/时间旅行无 |
| Smolagents | ★ | 无 | 完全自建 |

> 🎯 **结论**：需要"挂了能续、改错了能回放"→ LangGraph；需要"多实例共享会话"→ OpenAI Sessions（Redis）或 Claude session_store；只要对话连续性 → Pydantic message_history 够。

## 4. 维度三：可观测性

| 框架 | 等级 | 机制 | 说明 |
|------|:---:|------|------|
| Claude Agent SDK | ★★★ | 五种消息事件流 + hooks 审计 + OTel 接入 | 每轮/每工具都有结构化事件 |
| OpenAI Agents SDK | ★★★ | Tracing 内置（Task/Agent/Generation/Function/Guardrail/Handoff spans），trace processor 可换 OTel/Langfuse/Datadog | 默认发 OpenAI Traces |
| Pydantic AI | ★★★ | Logfire/OTel 内置，Instrumentation Capability | 开箱即用 |
| LangGraph | ★★ | LangSmith 原生；OTel 要自己埋 | 生态绑定 LangSmith 最强 |
| Smolagents | ★ | 无内置 | agent.memory.steps 可审计，指标自建 |

> 落地细节（trace 字段标准/采样）见 [工程化模块 04 篇](../../Agent%20工程化%20&%20部署模块（从%20demo%20到可用应用）/04-可观测性：日志、Trace%20与%20Metrics（LLMOps%20三支柱）.md)。

## 5. 维度四：MCP 与工具生态

| 框架 | MCP 支持 | 工具生态 |
|------|---------|---------|
| Claude Agent SDK | ★★★ 原生客户端 + 进程内 server（`create_sdk_mcp_server`） | Claude Code 全套内置工具 + Skills/Plugins |
| Pydantic AI | ★★★ 原生 MCP 客户端，延迟加载 | 40+ 模型供应商；MCP 工具即插 |
| OpenAI Agents SDK | ★★★ 一等支持（mcp_servers 配置） | OpenAI 生态 + 任意 MCP |
| LangGraph | ★★★ 原生（langchain-mcp-adapters） | LangChain 生态最大 |
| Smolagents | ★★ 工具层可接 MCP | 极简——第三方生态小 |

> 💡 **MCP 的框架不可知价值**：工具按 MCP 实现一次，五大框架通用——框架切换时工具层零重写（工程化模块 02 篇"MCP 是集成层不是架构起点"）。

## 6. 维度五：性能成本特征

| 特征 | 对比 |
|------|------|
| 步数效率 | Smolagents 代码执行省 ~30% 步数/调用（多步任务） |
| 上下文效率 | Claude Agent SDK 自动压缩 + 子 Agent 隔离；LangGraph 无原生压缩 |
| 缓存友好 | 五框架都吃前缀缓存——稳定 system + 工具排序（工程化模块 07 篇） |
| 推理深度控制 | Claude Agent SDK effort；Pydantic AI Thinking Capability；LangGraph 模型层 |
| 流式 | 五框架全支持（Claude 需 include_partial_messages） |

## 7. 生产就绪度矩阵

| 框架 | 版本策略 | 社区/维护 | 文档 | 生产验证 |
|------|---------|----------|------|---------|
| LangGraph | v1.x LTS（"ACTIVE 直到 v2.0"） | 大（LangChain 生态） | 优秀 | 大量生产案例 |
| OpenAI Agents SDK | v0.x 快速迭代 | 大（官方） | 良好 | 官方 + 社区 |
| Pydantic AI | v2 稳定但演进快（v2.26 一个月内多次 minor） | 中（Pydantic 团队） | 优秀 | 多个 ADR 选型 |
| Claude Agent SDK | v2.x 稳定 | 中（官方，捆绑 Claude Code 二进制） | 优秀 | 官方 demo 丰富 |
| Smolagents | 1.0 首个稳定 | 中（HuggingFace） | 良好 | 生态较新 |

---

## 【参考来源】

- [morphllm.com: AI Agent Frameworks (2026 Update): 8 SDKs Compared](https://www.morphllm.com/ai-agent-framework)
- [LangChain Docs: Human-in-the-Loop](https://docs.langchain.com/oss/python/langchain/frontend/human-in-the-loop)
- [futureagi.com: What is the OpenAI Agents SDK?（Sessions/Guardrails/Tracing）](https://futureagi.com/blog/what-is-openai-agents-sdk-2026/)
- [Pydantic AI v2.0.0 发布说明（Capabilities/可观测）](https://github.com/pydantic/pydantic-ai/releases/tag/v2.0.0)
- [Claude Agent SDK: How the agent loop works（hooks/权限/会话）](https://code.claude.com/docs/en/agent-sdk/agent-loop)
- [DeepWiki: smolagents Code Execution & Security](https://deepwiki.com/huggingface/smolagents/6-code-execution-and-security)

---

**返回总览**：[00-总览：单 Agent 框架知识体系](00-总览：单%20Agent%20框架知识体系.md)

**下一模块**：[09-框架选型与迁移实战](09-框架选型与迁移实战.md)
