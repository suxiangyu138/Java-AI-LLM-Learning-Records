# 04 n8n：AI 自动化工作流平台（Agent 原生）

> 定位：集成优先的 AI 工作流平台——400+ 应用连接器 + AI Agent 节点，2026 年 2.x 让"Agent 成为工作流执行者"，适合把 AI 接到现有业务系统（2026-08 基准）

## 📚 目录

1. [平台定位与版本演进](#1-平台定位与版本演进)
2. [AI Agent 节点：从步骤到执行者](#2-ai-agent-节点从步骤到执行者)
3. [工作流即工具：Agent 调用的技能化](#3-工作流即工具agent-调用的技能化)
4. [记忆与上下文](#4-记忆与上下文)
5. [HITL 人工介入](#5-hitl-人工介入)
6. [集成生态：400+ 连接器](#6-集成生态400-连接器)
7. [结构化输出与质量能力](#7-结构化输出与质量能力)
8. [部署与生产实践](#8-部署与生产实践)
9. [局限与适用边界](#9-局限与适用边界)
10. [核心要点](#10-核心要点)

## 1. 平台定位与版本演进

n8n 是通用节点式自动化平台（TypeScript），核心优势是**集成**：~400+ 应用连接器（Slack、HubSpot、Postgres、S3、Notion、HTTP 端点），AI 作为一等公民叠加。单容器轻量部署（1-2 GB 内存最低，2-4 GB 推荐）。

```text
2026 版本演进主线：
2.0（2026-01）── 从线性自动化工具转型 AI Agent 编排器：
   原生 LangChain 集成（Chains/Agents/Memory/Vector Stores 节点）、
   AI Agent Tool 节点（工作流可被 Agent 调用）、持久记忆（Redis/Postgres）、
   自动保存、新画布 UI
2.6.0（2026-01-26）── AI Agent 节点错误信息优化、Gemini 3 签名处理
1.85 线（2026-04）── 范式转变：AI Agent 节点成为工作流"核心执行者"，
   LLM 从"单步"变为"决策中枢"，工作流引擎变成基础设施；
   Queue Mode 改进（高吞吐并行 Agent 工作流）、Expression Engine v2
2.30.0（2026-07-07）── 并行工具调用结构保留在聊天记忆中、Agent 执行项目级作用域、
   AI 助手推理分块渲染
2.33.0（2026-07-28）── AI Agent 预览节点转正为 V1、已验证社区节点可作 Agent 工具、
   结构化输出从示例 JSON 推断、HITL 暂停/恢复、Agent 评估执行场景、
   agents 支持 availableInMCP 列（MCP 可发现）
```

> 🎯 **一句话定位**：n8n 2026 年的转变是"**LLM 作为工作流的一步**"→"**Agent 作为工作流的执行者**"——Agent 做决策，n8n 引擎提供基础设施（触发、重试、队列、监控）。

## 2. AI Agent 节点：从步骤到执行者

| 版本阶段 | Agent 节点形态 |
|---------|--------------|
| 2.0 前 | LLM 是流程中的一个节点（输入→输出） |
| 2.0 | 原生 Agent 节点：模型 + 工具 + 记忆 打包，可在流程中自主循环 |
| 2.33 | AI Agent 节点转正为 **V1**（不再 preview），成为一等公民 |

**AI Agent 节点核心能力**（2.33 基准）：
- 多工具并行调用（并行工具调用结构保留在聊天记忆）
- 结构化输出推断：从示例 JSON 推断输出 schema（无需手写 JSON Schema）
- 已验证社区节点可作为 Agent 工具（生态扩展）
- 中间步骤返回：无工具时返回中间步骤
- "Fix with Assistant"：失败的 Agent 预览工具一键修复
- MCP 连接失败对 Agent 非阻塞（容错）

```text
Agent 节点的工作形态：
触发（Webhook/定时/聊天）→ Agent 节点决策 → 调工具（连接器/HTTP/子工作流）
→ 记忆更新 → 循环直到完成 → 结果写回（CRM/DB/消息）
```

## 3. 工作流即工具：Agent 调用的技能化

这是 n8n 2026 最具特色的机制——**任何工作流都可以被声明为 Agent 的工具**：

| 维度 | 说明 |
|------|------|
| 机制 | AI Agent "Tool" 节点：把某个工作流封装成一个工具，Agent 自主决定何时调用 |
| 效果 | 模块化、可复用的 Agent 技能（技能=工作流） |
| 复用 | 一个工作流可被多个 Agent 调用，Agent 可调用多个工作流 |
| 对应概念 | 与 Dify Skills、Coze Agent Skills 同思路——但 n8n 的技能就是"已有的自动化流程" |

> 💡 **关键洞察**：企业在 n8n 里已有大量业务自动化流程，Agent 工具化让这些存量资产**零改写**变成 Agent 的技能——这是 n8n 相对其他平台最大的迁移优势。

## 4. 记忆与上下文

| 后端 | 特点 |
|------|------|
| Redis | 生产推荐（共享、可持久化） |
| Postgres | 生产推荐（与业务库一体） |
| 内存 | 开发调试用，重启即失 |

- 2.30+：并行工具调用结构保留在聊天记忆中（多工具结果不丢）
- 会话记忆按执行/会话管理，支持跨执行保留上下文

## 5. HITL 人工介入

- 2.33.0：AI Agent 预览工具的 **HITL 暂停/恢复** 能力浮出——Agent 执行中可挂起等待人工审批/修正
- 暂停/恢复内容在 Instance AI 追踪中可见

```text
HITL 四种形态（与单 Agent 框架模块的 interrupt() 对应）：
审批      = 人工确认后继续
拒绝      = 终止该分支
修正      = 人工改参数后继续
回答      = 人工直接回复用户，Agent 终止
n8n 2.33 的 HITL 覆盖前两者为主，代码框架（LangGraph interrupt）四者全支持
```

## 6. 集成生态：400+ 连接器

| 类别 | 示例 |
|------|------|
| 协作 | Slack、Notion、Google Workspace、Teams |
| 数据 | Postgres、MySQL、S3、MongoDB、Elasticsearch |
| CRM/营销 | HubSpot、Salesforce、Mailchimp |
| 消息 | WhatsApp、Telegram、Email (SMTP/IMAP) |
| 基础 | HTTP、Webhook、Code（JS/Python）、条件分支 |

> 🎯 **n8n 的适用判据**：当你的问题本质是"**集成 + 一点智能**"（Slack 消息 → LLM 处理 → 回写 CRM）时，n8n 是 2026 年的最优解；而"智能 + 一点集成"（复杂 RAG 产品）则 Dify 更合适。

## 7. 结构化输出与质量能力

- **结构化输出推断**：Agent 节点可从示例 JSON 推断输出结构（2.33）
- **Evals**：支持一等公民 Agent 的执行场景评估（2.33）——Agent 行为可测
- **AI Assistant**：内置 AI 助手辅助排错（推理分块渲染）
- **Expression Engine v2**：表达式语法升级（1.85 线）

## 8. 部署与生产实践

| 形态 | 说明 |
|------|------|
| 自托管 | 单容器 Docker 最简；Queue Mode 多实例高吞吐 |
| n8n Cloud | Starter ~€20/月（年付，约 $22/月）；自托管免费 |

**生产实践要点**：
1. **Queue Mode**：高吞吐并行 Agent 工作流启用队列模式（1.85+ 改进）
2. **记忆后端选 Redis/Postgres**：多实例共享记忆必须用外部后端
3. **工具命名规范**：工作流当工具用时要起"模型能理解"的名字与描述（工具描述决定 Agent 是否调用）
4. **成本控制**：1000 次 AI 调用/天规模下自托管比 Cloud 省 4-10 倍（基础设施 $30-100/月 vs 云 $150-300/月）
5. **许可注意**：Sustainable Use License——内部使用免费，对外商业化（托管付费客户）需商业协议

## 9. 局限与适用边界

| 局限 | 表现 | 应对 |
|------|------|------|
| 许可 | Sustainable Use（非纯 OSS），商业化受限 | 商用前签商业协议 |
| RAG 深度 | 内置 RAG 弱于 Dify（无完整知识库流水线） | 配外部向量库节点 |
| 学习曲线 | 节点式心智模型偏技术（业务人员不如 Coze 友好） | 模板市场起步 |
| 多 Agent | 编排为单 Agent 为主（多 Agent 能力弱于 Dify/Langflow） | 复杂多 Agent 用专门平台 |
| 可观测 | 追踪/分析弱于 Dify 的完整 Ops | 接外部可观测（OTel 等） |

> 🎯 **适用结论**：n8n = "**AI 自动化 + 业务系统集成**"——触发型、连接型、流程型 Agent（邮件处理、工单路由、数据同步）是它的主场；它是企业"已有自动化资产"升级为 Agent 技能的最佳跳板。

## 10. 核心要点

> 🎯 **核心要点**：
> 1. n8n 2026 的范式转变：**Agent 从工作流的一步变成工作流的执行者**（2.0 开始，2.33 Agent 节点转正 V1）
> 2. 杀手锏机制：**工作流即工具**——存量自动化流程零改写成为 Agent 技能
> 3. 结构化输出推断（示例 JSON）+ HITL 暂停/恢复 + Evals 评估：质量闭环补齐
> 4. 选 n8n 的判据：问题本质是"集成 + 一点智能"；"智能 + 一点集成"选 Dify

---

**上一模块**：[03 Coze 扣子](03-Coze%20扣子：零代码%20Agent%20开发与%20AI%20团队协作.md)　**下一模块**：[05 Flowise 与 Langflow](05-Flowise%20与%20Langflow：LangChain%20可视化双子星.md)　**返回总览**：[00 总览](00-总览：低代码%20Agent%20平台知识体系.md)

## 【参考来源】

- [n8n Release 2.33.0 (GitHub)](https://github.com/n8n-io/n8n/releases/tag/n8n%402.33.0)
- [n8n Release 2.30.0 (newreleases.io)](https://newreleases.io/project/github/n8n-io/n8n/release/n8n@2.30.0)
- [n8n 2.6.0 release notes (2026-01-26)](https://selfhosted.libhunt.com/n8n-changelog/2.6.0)
- [Building Agentic Workflows with n8n 2.0 & LangChain: A 2026 Guide](https://finbyz.tech/n8n/insights/n8n-2-0-langchain-agentic-workflows)
- [n8n v1.85：AI Agent 原生工作流（BotLearn）](https://www.botlearn.ai/zh/community/ai_tools/767a0835-c8e0-40c2-9794-ed2ab0e951c3/n8n-v1-85-ai-agent-%E5%8E%9F%E7%94%9F%E5%B7%A5%E4%BD%9C%E6%B5%81)
- [Flowise vs Langflow vs Dify vs n8n: The Honest 2026 Comparison](https://agentswarms.fyi/blog/flowise-vs-langflow-vs-dify-vs-n8n-vs-agentswarms)
- [Workflow Automation News: Zapier AI, n8n 2.0 & Make Updates (January 2026)](https://finbyz.tech/ai-automation/insights/january-2026-workflow-automation-news)
