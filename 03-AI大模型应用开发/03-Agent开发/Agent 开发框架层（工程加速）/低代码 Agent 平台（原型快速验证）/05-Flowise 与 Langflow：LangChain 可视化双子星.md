# 05 Flowise 与 Langflow：LangChain 可视化双子星

> 定位：LangChain 生态的可视化层——Flowise（TypeScript/LangChainJS）与 Langflow（Python/LangChain）同源不同语言；Langflow 2026 年以"流程一键导出 MCP Server"出圈（2026-08 基准）

## 📚 目录

1. [双子星定位对比](#1-双子星定位对比)
2. [Langflow：Python 可视化流程构建器](#2-langflowpython-可视化流程构建器)
3. [MCP Server 一键导出：Langflow 的杀手锏](#3-mcp-server-一键导出langflow-的杀手锏)
4. [Flow DevOps Toolkit 与版本管理](#4-flow-devops-toolkit-与版本管理)
5. [Flowise：LangChainJS 拖拽构建器](#5-flowiselangchainjs-拖拽构建器)
6. [收购与许可变局](#6-收购与许可变局)
7. [Flowise vs Langflow 选型](#7-flowise-vs-langflow-选型)
8. [局限与适用边界](#8-局限与适用边界)
9. [核心要点](#9-核心要点)

## 1. 双子星定位对比

| 维度 | Flowise | Langflow |
|------|---------|----------|
| 语言生态 | TypeScript/Node.js | Python |
| 底层框架 | LangChainJS | LangChain Python |
| 许可 | Apache 2.0 | MIT（最宽松） |
| GitHub Stars | ~54.1k | ~150k（2026-08 达 152,826） |
| 母公司 | Workday（2025-08 收购） | DataStax（2024 收购）→ IBM（2025 收购 DataStax） |
| 强项 | 最快出聊天/RAG MVP、模板市场、托管 Widget | 序列化流程可被 Python 服务运行、MCP 导出、企业级 |
| 弱项 | 纯 JS 生态、画布 30 节点后卡顿 | 需 Python 环境、组件体系学习成本 |

> 🎯 **一句话**：两个平台都是"LangChain 的画布化包装"——Flowise 是给 Node/前端团队的快速 MVP 工具，Langflow 是给 Python 团队的可视化-代码桥接工具。

## 2. Langflow：Python 可视化流程构建器

| 能力 | 2026 事实 |
|------|----------|
| 预置组件 | 300+ 预构建组件 |
| LLM 提供商 | 15+（OpenAI/Anthropic/DeepSeek/本地 Ollama 等） |
| 向量库 | 8+（Astra DB 原生、Milvus、Chroma 等） |
| Agent 能力 | Tool-calling Agent 组件、多 Agent 编排组件 |
| 序列化 | 流程可导出为 JSON，从真实 Python 服务运行（LangChain 对象级映射） |
| 版本 | 1.9（2026-04-13）；1.11.x 系列跟进 |

```text
Langflow 的核心设计哲学：
画布上的流程 = 可序列化的 LangChain 对象图
→ 可视化原型可以直接映射为工程师可读的 Python 代码
→ "可视化编排 + 代码兜底"双轨制，比纯画布平台更接近工程
```

## 3. MCP Server 一键导出：Langflow 的杀手锏

2026 年 Langflow 社区最热的能力——**任何流程一键导出为 MCP Server**：

| 维度 | 说明 |
|------|------|
| 是什么 | 把拖拽出的流程封装成标准 MCP Server，暴露为工具 |
| 谁调用 | 任意 MCP 客户端：Claude Desktop、Claude Code、Codex、OpenClaw 等编码 Agent |
| 效果 | 非技术人员画的流程，变成编码 Agent 可调用的工具——零胶水代码 |
| 生态热度 | 被 2026-08 社区文章评为"稀缺能力"，GitHub 星数冲至 152,826 的直接推手 |
| 延伸 | 第三方项目（如 langflow-mcp）提供 create_flow/get_flow/run_flow 等管理工具 |

```text
MCP 导出的典型用法：
业务人员：拖拽搭出"周报生成流程" → 一键导出 MCP Server → 发布内网
开发者：Claude Code 在终端里把该流程当工具调用
        → 流程迭代在画布上做，不用改 Agent 代码
→ 这就是 2026 年"低代码平台 + 编码 Agent"的协作范式
```

## 4. Flow DevOps Toolkit 与版本管理

Langflow 1.9（2026-04-13）引入工程化能力：

| 能力 | 说明 |
|------|------|
| Flow DevOps Toolkit SDK | 从终端做流程的版本/测试/部署（`lfx init`） |
| 流程版本管理 | 侧边栏版本化流程、增量保存、回滚到历史版本 |
| IDE/编码 Agent 支持 | MCP 协议让 IBM Bob、Claude Code 等可编程化构建与执行流程 |
| 部署延伸 | EDB Hybrid Manager 将流程打包为不可变版本化 bundle，经网关以 HTTP 与 MCP 双端点发布 |

> 💡 **工程化信号**：Langflow 在 1.9 前是"画布工具"，1.9 后开始覆盖**版本、测试、部署、CI 化**——补齐了低代码平台通往生产的最后一公里。

## 5. Flowise：LangChainJS 拖拽构建器

| 能力 | 说明 |
|------|------|
| 定位 | 最快路径出聊天机器人/RAG MVP 的拖拽构建器 |
| 模板 | 模板市场（chatbot/RAG/工具调用/agent 模板） |
| 交付 | 托管聊天 Widget、干净 API 接口 |
| 自托管 | 轻量（1-2 GB 内存），Apache 2.0，可自托管 |
| 生态 | 纯 LangChainJS，围绕 JS 技术栈 |

**已知短板**：
- 画布在 ~30 节点后性能明显下降（大型流程受限）
- 底层抽象即 LangChain——超出 LangChain 能力时无处可去
- 企业级功能在非公开付费墙后

## 6. 收购与许可变局

```text
Flowise：2025-08 被 Workday 收购
  → 仍可自托管（Apache 2.0），但许可证条款需要持续关注
  → 社区担忧：核心团队转向 Workday 商业产品后，开源版演进放缓

Langflow：2024 被 DataStax 收购 → 2025 IBM 收购 DataStax
  → MIT 许可最宽松，IBM 加持下企业级路线清晰
  → 2026-08 仍保持高频发布（1.9 → 1.11.x）
```

> ⚠️ **选型提示**：开源平台被收购后，License 与演进方向是长期风险——选择前查看最新条款（Flowise 尤其注意）。

## 7. Flowise vs Langflow 选型

```text
第一问：技术栈是什么？
├── Node/前端团队、无 Python 环境 → Flowise
└── Python 团队或可接受 Python → Langflow

第二问：流程要进生产怎么交付？
├── 嵌入式聊天 Widget / 快速 MVP → Flowise
└── 真实 Python 服务运行 / 导出 MCP 给编码 Agent → Langflow

第三问：工程化要求？
├── 轻量自托管、不折腾 → Flowise
└── 版本管理 / CLI DevOps / 企业部署 → Langflow
```

| 场景 | 推荐 |
|------|------|
| 3 天出聊天机器人原型（Node 团队） | Flowise |
| Python 团队可视化编排 → 代码落地 | Langflow |
| 让 Claude Code 调用业务流程图 | Langflow（MCP 导出） |
| 超大规模流程（30+ 节点） | 均不推荐，直接代码框架 |
| 需要 Astra DB 等向量库原生集成 | Langflow |

## 8. 局限与适用边界

| 局限 | 表现 | 应对 |
|------|------|------|
| 强绑 LangChain | 超出 LangChain 能力边界即受限 | 复杂场景迁移代码框架 |
| 画布规模 | Flowise 30 节点后卡顿 | 大型流程拆分为子流程/API |
| 收购风险 | Flowise 被 Workday 收购后条款演进待观察 | 关注 License 变更 |
| 非产品化 | 无 Dify 式的用户/密钥/分析体系 | 对外交付接自建网关 |
| 多 Agent 深度 | 组件级支持，复杂编排弱于专用框架 | 复杂多 Agent 用 LangGraph 等 |

> 🎯 **适用结论**：Langflow = "**Python 团队的可视化-代码桥**"，最独特的价值是 MCP 一键导出（让编码 Agent 调用业务流程图）；Flowise = "**Node 团队的快速 MVP 工具**"。两者都是"原型验证好、产品化弱"——交付完整产品时仍要接自建网关或迁移。

## 9. 核心要点

> 🎯 **核心要点**：
> 1. 双子星同源：Flowise 包 LangChainJS（Node），Langflow 包 LangChain Python——按技术栈选
> 2. Langflow 的杀手锏是 **MCP Server 一键导出**：拖拽流程 → MCP 工具 → Claude Code/Codex 调用
> 3. Langflow 1.9 补齐工程化（Flow DevOps Toolkit + 版本管理），从"画布工具"走向"开发平台"
> 4. 收购风险要盯：Flowise（Workday，2025-08）、Langflow（IBM 系）——选型看最新 License 条款

---

**上一模块**：[04 n8n](04-n8n：AI%20自动化工作流平台（Agent%20原生）.md)　**下一模块**：[06 云厂商与 SaaS](06-云厂商与%20SaaS：百炼、Copilot%20Studio、Lindy%20生态.md)　**返回总览**：[00 总览](00-总览：低代码%20Agent%20平台知识体系.md)

## 【参考来源】

- [Langflow 1.9 released: Langflow Assistant, Flow DevOps Toolkit, and MCP support (Langflow 官方)](https://www.langflow.org/blog/langflow-1-9)
- [GitHub 152K★ 项目 langflow 深度实战：可视化 AI 工作流 + MCP Server 一键导出](https://www.aiec.fun/github-152k%e2%98%85-%e9%a1%b9%e7%9b%ae-langflow-%e6%b7%b1%e5%ba%a6%e5%ae%9e%e6%88%98%ef%bc%9a%e5%8f%af%e8%a7%86%e5%8c%96-ai-%e5%b7%a5%e4%bd%9c%e6%b5%81-mcp-server-%e4%b8%80%e9%94%ae%e5%af%bc/)
- [Langflow release notes (GitHub)](https://github.com/langflow-ai/langflow/blob/873d5585/docs/docs/Support/release-notes.mdx)
- [EDB Postgres AI Innovation Release - Flow deployment (Langflow)](https://edb-docs.netlify.app/docs/edb-postgres-ai/latest/hybrid-manager/ai-factory/langflow/flow-deployment/)
- [langflow-mcp: MCP server for Langflow (GitHub)](https://github.com/nobrainer-tech/langflow-mcp)
- [Langflow Review 2026 (ToolBrain)](https://toolbrain.net/blog/langflow-review-2026/)
- [Best 5 Flowise Alternatives in 2026 (FutureAGI)](https://futureagi.com/blog/best-flowise-alternatives-2026/)
- [Flowise vs Langflow vs Dify vs n8n: The Honest 2026 Comparison](https://agentswarms.fyi/blog/flowise-vs-langflow-vs-dify-vs-n8n-vs-agentswarms)
