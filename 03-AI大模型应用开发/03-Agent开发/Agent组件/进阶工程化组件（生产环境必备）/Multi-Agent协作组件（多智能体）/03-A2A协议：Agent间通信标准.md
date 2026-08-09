# A2A 协议：Agent 间通信标准

> A2A = Agent 间通信的开放标准（Google 2025-04 提出 → Linux Foundation）——"Agent 界的 HTTP"：让不同框架/语言/厂商的 Agent 无需共享代码即可发现、通信、协作。2026 已生产成熟：**150+ 组织、Spec 1.0.0、三大云集成、IETF 立项**。本章给 A2A 完整技术深潜。

## 1. A2A 的定位与成熟度（2026）

| 项 | 说明 |
|---|---|
| 定位 | 水平问题：Agent↔Agent 对等协调（跨组织/平台边界） |
| 起源 | Google 2025-04 提出 → Linux Foundation 托管 |
| 一周年（2026-04） | **150+ 支持组织**；生产部署：供应链/金融/保险/IT 运维 |
| Spec | **1.0.0 正式版**（SDK 对齐） |
| 云集成 | Azure AI Foundry/Copilot Studio、AWS Bedrock AgentCore、Google Cloud |
| 标准推进 | IETF 筹备 AIPROTO WG（以 A2A 为基线）；NIST AI Agent Standards Initiative（2026-02-17） |

> 🎯 核心要点：**A2A 是"水平"标准**——解决"我的 Agent 和你的 Agent 怎么对话"；与 MCP（垂直：Agent→工具）分层互补（§7）。2026 已从提案变生产事实。

## 2. Agent Card：发现机制

```json
// Agent Card（/.well-known/agent.json 服务）
{
  "name": "compliance-agent",
  "url": "https://agent.example.com/a2a",
  "version": "1.2.0",
  "skills": [{"id": "compliance_check", "inputModes": ["text"], "outputModes": ["text/json"]}],
  "securitySchemes": { "oauth2": { "type": "oauth2", "flows": { "clientCredentials": {} } } }
}
```

| 要素 | 说明 |
|---|---|
| 位置 | `/.well-known/agent.json`（或 agent-card.json） |
| 内容 | 名称/URL/版本/技能/输入输出格式/安全方案 |
| 流程 | 调用方先取卡 → 理解能力 → 发起调用 |
| 签名 | 生产可加密签名——接收方验证真实性 |
| 类比 | **版本化的公共 API 契约**——锁版本 + 严格 IO 校验 |

> 🎯 核心要点：Agent Card 是"能力发现"的契约——**先取卡再调用**；生产把卡当版本化 API 管理（pinned 版本、Pydantic 类严格校验、显式安全方案）。

## 3. 通信机制：JSON-RPC 2.0

| 方法 | 用途 |
|---|---|
| message/send | 发送消息（多轮对话） |
| tasks/send | 创建任务（一次性委托） |
| tasks/get | 查询任务状态 |

| 消息部件 | 类型 | 用途 |
|---|---|---|
| TextPart | 文本 | 自然语言内容 |
| DataPart | 结构化 JSON | 类型化数据 |

> 💡 消息 vs 任务：**message/send 适合多轮对话（贵）；tasks/send 适合一次性委托（省）**——05 篇 token 乘数效应的协议级对应。

## 4. Task 生命周期

```text
Task 状态机：
  submitted → working → completed
                         └→ failed
  支持同步与异步工作流（异步：轮询 tasks/get）
```

| 状态 | 语义 | 编排层动作 |
|---|---|---|
| submitted | 已提交待处理 | 记录交接开始 |
| working | 执行中 | 监控进度 |
| completed | 完成（带结果） | 消费结果 |
| failed | 失败（带错误） | 重试/降级/人工 |

> 🎯 核心要点：**Task 是 A2A 的最小执行单元**——状态机让编排层可监控/可重试/可审计；"任务有明确定义的完成状态"是防级联（08 篇）的基础结构。

## 5. A2A 安全：零信任落地

| 机制 | 说明 |
|---|---|
| securitySchemes | Agent Card 声明：OAuth 2.0/OIDC/API key/mTLS |
| 生产组合 | **mTLS + OAuth2**（RFC 8705/DPoP 发送者约束令牌） |
| 零信任原则 | 每次 A2A 调用单独认证授权——内部流量当外部流量 |
| 可观测 | W3C Trace Context（traceparent）跨 Agent 传播（09 篇） |

> ⚠️ 2026 铁律：**内部 Agent 流量 = 外部流量**——每个 A2A 调用单独认证；信任"内部"是多 Agent 系统最大的安全幻觉（08 篇信任边界）。

## 6. SDK 与生态（2026）

| SDK | 说明 |
|---|---|
| 官方 A2A SDK | 全语言 |
| Quarkus A2A Java SDK | 1.0.0.CR1（2026-05）：v0.3 协议兼容层、JSON-RPC/gRPC/REST 传输 |
| @a2a-js/sdk | JS/TS |
| Go Micro A2A gateway | 注册表元数据生成 Agent Card + 翻译 A2A 任务到既有 RPC |
| MuleSoft Anypoint A2A Connector | 企业集成 |
| Microsoft Teams | bot-to-bot 交接走 A2A |

> 💡 跨语言实证：**Python ADK Agent 协调 Go 合规 Agent 纯 A2A**（Google 官方 demo）——语言无关是 A2A 的核心卖点；Java 侧 Quarkus SDK 是 JVM 生态入口。

## 7. A2A vs MCP：分层互补

| 维度 | MCP | A2A |
|---|---|---|
| 问题 | 垂直：Agent→工具/数据 | 水平：Agent↔Agent |
| 模式 | client-server（JSON-RPC） | 对等（JSON-RPC 2.0） |
| 关系 | 一个 Agent 向下取工具 | 两个 Agent 平级协作 |
| 典型组合 | 编排 Agent 内部用 MCP 调后端 | 编排 Agent 用 A2A 委派专家 |
| 类比 | USB-C（接设备） | HTTP（上网） |

> 🎯 核心要点：**分层架构 = A2A 高层编排/委派 + MCP 低层工具执行**——常见模式：编排 Agent 说 A2A 委派专家，专家内部用 MCP 调后端（信用检查/身份验证/库存系统）。

## 8. 常见误区

| 误区 | 真相 |
|---|---|
| "A2A 与 MCP 竞争" | 互补——垂直工具 vs 水平 Agent，分层组合 |
| "Agent Card 随便写" | 版本化公共 API 契约——锁版本+严格校验+签名 |
| "message/send 通用" | 多轮贵——一次性委托用 tasks/send |
| "内部流量可信" | 零信任——每个 A2A 调用单独认证 |
| "A2A 是提案" | 1.0.0 正式版 + 150 组织 + 三大云集成 |
| "Task 无状态" | Task 有明确状态机——可监控可重试 |
| "跨语言不行" | Python↔Go 实证——语言无关 |

## 9. 面试速记

| 问题 | 一句话答案 |
|---|---|
| A2A 是什么？ | Agent 间通信开放标准——"Agent 界 HTTP" |
| 2026 成熟度？ | 1.0.0 正式版 + 150 组织 + 三大云集成 + IETF 立项 |
| Agent Card？ | /.well-known/agent.json——身份/能力/安全方案，可签名 |
| 发现流程？ | 先取卡再调用——版本化公共 API |
| 核心方法？ | message/send、tasks/send、tasks/get |
| Task 状态机？ | submitted→working→completed/failed |
| 安全方案？ | OAuth2/OIDC/API key/mTLS——生产 mTLS+OAuth2 |
| 零信任？ | 内部流量当外部流量——每次调用单独认证 |
| 与 MCP 分工？ | MCP 垂直（工具）、A2A 水平（Agent）——分层组合 |
| Quarkus SDK？ | A2A Java SDK 1.0.0.CR1（2026-05） |
| 跨语言？ | Python ADK ↔ Go 合规 Agent 纯 A2A 实证 |
| 与 05 篇关系？ | message vs tasks 对应交接的贵与省 |

---

**下一模块**：[04-协议栈全景：MCP/A2A/ACP](04-协议栈全景：MCP-A2A-ACP.md)　**返回总览**：[00-Multi-Agent 协作组件总览](00-Multi-Agent协作组件总览.md)

## 参考来源

- [Agent2Agent vs MCP: 2 Protocols Your 2026 Stack Needs（Beam AI）](https://beam.ai/agentic-insights/agent2agent-vs-mcp-2026-ai-agent-stack)
- [A2A protocol: Architecture and technical specification（Tyk）](https://tyk.io/learning-center/a2a-protocol-architecture-and-technical-specification/)
- [A2A Java SDK 1.0.0.CR1 Released（Quarkus）](https://quarkus.io/blog/a2a-java-sdk-1-0-0-cr1-released/)
- [Build Cross-Language Multi-Agent Team with Google ADK and A2A（Google）](https://developers.googleblog.com/build-cross-language-multi-agent-team-with-google-agent-development-kit-and-a2a/)
- [Agents Across Frameworks: A2A（Go Micro）](https://go-micro.dev/blog/26)
- [The practical guide to A2A agent framework integrations（Tyk）](https://tyk.io/learning-center/the-practical-guide-to-a2a-agent-framework-integrations/)
