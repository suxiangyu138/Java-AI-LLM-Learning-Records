# 02 Dify：开源 Agent 平台标杆（Agent 沙箱与 Skills）

> 定位：开源阵营第一选择——LLM 应用后端即服务，2026 年 1.16.0 引入原生 Agent 沙箱与 Skills 体系，从"工作流平台"升级为"Agent 平台"（2026-08 基准）

## 📚 目录

1. [平台定位与版本演进](#1-平台定位与版本演进)
2. [核心架构：四大能力块](#2-核心架构四大能力块)
3. [Dify Agent：Linux 沙箱机制深潜](#3-dify-agentlinux-沙箱机制深潜)
4. [Skills 系统：类 Claude Code 的技能封装](#4-skills-系统类-claude-code-的技能封装)
5. [Workflow 与 Agent 的融合](#5-workflow-与-agent-的融合)
6. [MCP 协议升级与工具生态](#6-mcp-协议升级与工具生态)
7. [RAG 与知识库能力](#7-rag-与知识库能力)
8. [部署形态与生产实践](#8-部署形态与生产实践)
9. [局限与适用边界](#9-局限与适用边界)
10. [核心要点](#10-核心要点)

## 1. 平台定位与版本演进

Dify 是开源社区最"产品化"的 LLM 应用平台（GitHub ~147k stars），提供 Workflow、Agent、Chatbot、知识库（RAG）、API 网关、用量分析的一体化能力，可完全自托管。

```text
2026 版本演进主线（每 2-4 周一版）：
1.11.3（2026-01-16）── 性能：MCP embeddedResource 支持、数据集批量 re-index、
                     AgentMaxIterationError 健壮性、OAuth 优化、PDF 图片提取
1.14.0-rc1（2026-03 前后）── New Agent x Skills：沙箱化 Agent 运行时（Agent Mode）、
                     Skill Editor（可复用 SOP 块 + @send_email 内联工具调用）、
                     文件上传沙箱执行命令、模板市场
1.16.0（2026-07-19）── Dify Agent 开放 Beta：完整 Linux 沙箱、Agent Builder、
                     Skills 系统、Workflow 一等公民、Web App 发布、
                     MCP 升级 2025-06-18、GPT-5.6 Responses 兼容
```

> 💡 **版本节奏**：Dify 保持高频迭代（约 2-4 周一个 minor），自托管升级时要看 release notes 中标注的破坏性变更——例如 1.16 起新 API Key 默认走 OpenAI Responses 协议，旧自定义 Key 需手动切换。

## 2. 核心架构：四大能力块

| 能力块 | 定位 | 关键组成 |
|--------|------|---------|
| Workflow | 确定性流程编排（画布） | 节点：LLM/知识检索/代码/HTTP/条件分支/循环/变量聚合 |
| Agent | 智能体编排（模型自主决策） | ReAct 与 Function Calling 双模式、工具循环、多 Agent |
| Knowledge（知识库） | 内置 RAG 流水线 | 文档摄取/分块/Embedding/检索/重排，多模态（文本+图片） |
| API & Ops | 发布与运营 | 托管聊天 UI、API 网关（密钥+限流）、统一计费、日志分析 |

```text
Dify 与其他开源平台的本质差异：
n8n/Flowise/Langflow = 流程编排器（画布是主体）
Dify = LLM 应用后端（画布 + RAG + 用户 + 密钥 + 分析 一体）
→ 需要"能跑的产品"而不是"能跑的工作流"时选 Dify
```

## 3. Dify Agent：Linux 沙箱机制深潜

1.16.0 的核心更新是 **Dify Agent（开放 Beta）**——Agent 拥有自己的完整 Linux 沙箱环境：

| 机制 | 说明 |
|------|------|
| 沙箱运行时 | Agent 在完整 Linux 沙箱中运行，可执行 Python、安装软件包、操作文件、调用 API |
| Agent Builder | UI 内直接构建 Agent：设置 Prompt、上传 Skills 与文件、连接 Dify 生态工具与知识库 |
| 对话配置沙箱 | 通过对话让助手配置 Linux 沙箱环境、安装软件包、自动创建 Skills |
| 代码执行 | 自有 shell 环境，Agent 边想边执行（对标 Smolagents 的代码执行范式） |
| Agent 花名册 | 团队空间内像资产库一样管理与复用多个 Agent |

> 🎯 **设计意图**：Dify 的 Agent 沙箱对标 Claude Code 的"代码 Agent"体验，但完全托管在平台上——业务用户可以用自然语言配出能写代码、装包、调 API 的 Agent，无需本地环境。

## 4. Skills 系统：类 Claude Code 的技能封装

Dify 1.14/1.16 引入 Skills 系统，与 Claude Code 的 Skills 机制同源思路：

| 维度 | 说明 |
|------|------|
| 是什么 | 把工具与能力打包成标准化 Skills，Agent 按需调用 |
| Skill Editor | 构建可复用 SOP 块，支持内联工具调用（如 `@send_email`） |
| 创建方式 | 手动编辑 + 对话自动创建（Agent 在沙箱中自建 Skills） |
| 分发 | 可发布到模板市场（1.14 引入） |
| 意义 | 让"领域最佳实践"成为可复用资产，而非每次重建提示词 |

```text
Skills 与工具的区别：
工具     = 单一能力（发邮件、查天气）
Skills   = 完整作业程序（含 SOP：先查库存 → 生成报价单 → 发邮件 → 记录 CRM）
Agent 收到任务 → 识别该用哪个 Skill → 按 Skill 的 SOP 执行
```

## 5. Workflow 与 Agent 的融合

1.16 让 Agent 成为 Workflow 的"一等公民"：

- **Workflow 节点调用 Agent**：Agent 可在现有 Workflow 中作为节点被调用，执行任务后把输出传给下游节点
- **内联创建**：在 Workflow 画布内直接创建 Agent
- **Web App 发布**：Agent 构建的应用可发布为 Web App，底层能力更强

```text
融合的架构含义：
确定性流程（Workflow）= 需要稳定、可审计、可控的核心链路
自主决策（Agent）    = 需要灵活、探索性的非核心环节
→ 2026 共识：两者嵌套编排，而不是二选一
（同思路：n8n 的 Agent 工具节点、Langflow 的 Agent 组件）
```

## 6. MCP 协议升级与工具生态

| 能力 | 1.16.0 事实 |
|------|-----------|
| 协议版本 | MCP 升级至 2025-06-18 规范 |
| 版本协商 | 支持 MCP 版本协商 |
| 结构化工具输出 | 工具输出结构化（tool 返回 JSON Schema 校验） |
| 动态 HTTP 头注入 | 每次请求级鉴权透传（Authorization 动态注入） |
| embeddedResource | 1.11.3 起支持 MCP embeddedResource 资源类型 |
| 模型兼容 | GPT-5.6：默认 API 类型从 Chat Completions 切到 Responses（旧 Key 保留原设置） |

> 💡 **MCP 的意义**：Dify 通过 MCP 接入任意工具市场（官方插件市场 + 社区 MCP 服务器），这是它生态化的关键——工具不再是平台内置列表，而是开放协议。

## 7. RAG 与知识库能力

- **内置 RAG 流水线**：文档摄取 → 分块 → Embedding → 检索 → 重排，全部可视化节点配置
- **多模态检索**：2026 初支持文本+图片检索
- **原生集成**：2026 年新增 MongoDB Atlas（向量检索）+ Voyage AI（Embedding）原生节点——可在画布上拖出"Embedding → 向量搜索 → 重排 → 格式化"的完整 RAG 链路
- **PDF 图片提取**：1.11.3 起支持 PDF 内图片提取，提升扫描件 RAG 效果

```text
RAG 场景速配：
知识库助手 / 客服 Copilot / 运营 Agent → Dify 内置 RAG 开箱即用
已有 ES/Milvus 等外部向量库         → 通过 HTTP/自定义工具接入
```

## 8. 部署形态与生产实践

| 形态 | 说明 | 适用 |
|------|------|------|
| Docker Compose | 多容器：API/Worker/Web/DB/向量库 | 自托管标准姿势 |
| 云版 Dify Cloud | Sandbox 免费（有限额）；Professional ~$59/月 | 快速起步 |
| 私有化 K8s | 生产级高可用 | 企业 |

**生产实践要点**：
1. **升级看破坏性变更**：高频版本迭代，升级前通读 release notes
2. **大并发注意**：Dify 自带 Postgres + 向量库，高流量场景考虑外部向量库与 Redis 加速（1.11.3 已优化 Redis pipeline 缓存）
3. **API 网关即产品**：对外交付用 Dify 的 API 密钥+限流，而不是直接暴露 Workflow
4. **会话记忆深浅**：社区反馈 Dify 大规模会话记忆管理偏浅——长会话场景需自行设计记忆策略

## 9. 局限与适用边界

| 局限 | 表现 | 应对 |
|------|------|------|
| 心智模型学习成本 | 画布强但需要学 Dify 的编排心智模型 | 教程/模板起步 |
| 状态管理浅 | 大规模会话记忆管理偏浅 | 长会话迁移代码框架 |
| 许可限制 | 修改版 Apache 2.0：多租户托管他人需书面许可 | 商用前确认许可条款 |
| 部署较重 | 多容器依赖（自带 Postgres/向量库） | 与单容器 n8n 对比权衡 |
| 复杂状态流 | 图级状态/时间旅行等高级能力弱于 LangGraph | 需要时迁移（09 篇） |

> 🎯 **适用结论**：Dify 是"开源里最接近生产产品"的低代码平台——**验证+上线一个 RAG/客服/运营类 Agent 产品**时优先考虑；它是本体系 02-06 五篇中唯一"平台即终态"概率最高的。

## 10. 核心要点

> 🎯 **核心要点**：
> 1. Dify 1.16.0（2026-07-19）从"工作流平台"升级为"Agent 平台"：Linux 沙箱 Agent + Skills 系统 + Workflow 一等公民
> 2. Skills = 完整作业程序（SOP 化），工具 = 单一能力——两者分开设计
> 3. MCP 2025-06-18 协议升级让 Dify 接入任意工具生态，模型兼容切到 OpenAI Responses
> 4. 选 Dify 的判断标准：要交付"能跑的产品"（用户/密钥/看板/RAG/分析），不是"能跑的工作流"

---

**上一模块**：[01 平台全景与选型](01-平台全景与选型：2026%20低代码%20Agent%20平台地图.md)　**下一模块**：[03 Coze 扣子](03-Coze%20扣子：零代码%20Agent%20开发与%20AI%20团队协作.md)　**返回总览**：[00 总览](00-总览：低代码%20Agent%20平台知识体系.md)

## 【参考来源】

- [Dify Release v1.16.0 (GitHub)](https://github.com/langgenius/dify/releases/tag/1.16.0)
- [dify 1.16.0 发布：原生Agent沙箱、MCP协议升级、GPT-5.6兼容适配（腾讯云）](https://cloud.tencent.com.cn/developer/article/2713157)
- [Dify Release 1.14.0-rc1: New Agent x Skills for Production Workflows](https://github.com/langgenius/dify/releases/tag/1.14.0-rc1)
- [dify 1.11.3 最新版本发布（腾讯云）](https://cloud.tencent.com.cn/developer/article/2622104)
- [Grounding Dify Agents in Real Data: MongoDB Atlas and Voyage AI](https://dify.ai/blog/grounding-dify-agents-in-real-data-mongodb-atlas-and-voyage-ai-are-now-native-to-dify-rag-workflows)
- [Dify Review 2026 (ToolBrain)](https://toolbrain.net/blog/dify-review-2026/)
- [Flowise vs Langflow vs Dify vs n8n: The Honest 2026 Comparison](https://agentswarms.fyi/blog/flowise-vs-langflow-vs-dify-vs-n8n-vs-agentswarms)
