# 06 云厂商与 SaaS：百炼、Copilot Studio、Lindy 生态

> 定位：非自托管阵营全景——阿里云百炼（Agent 2.0）深潜 + 微软/Google/AWS 大厂平台 + Lindy/Relevance AI/Voiceflow 等 SaaS 新锐 + 企业落地治理（2026-08 基准）

## 📚 目录

1. [阵营概览](#1-阵营概览)
2. [阿里云百炼：Agent 2.0 深潜](#2-阿里云百炼agent-20-深潜)
3. [百炼 CLI：模型能力 CLI 化](#3-百炼-cli模型能力-cli-化)
4. [大厂平台：Copilot Studio / Google / AWS](#4-大厂平台copilot-studio--google--aws)
5. [SaaS 新锐：Lindy / Relevance AI / Voiceflow](#5-saas-新锐lindy--relevance-ai--voiceflow)
6. [企业落地：编排差距与 Agent 蔓延](#6-企业落地编排差距与-agent-蔓延)
7. [云厂商 vs 开源 vs SaaS 选型](#7-云厂商-vs-开源-vs-saas-选型)
8. [核心要点](#8-核心要点)

## 1. 阵营概览

| 子阵营 | 代表 | 一句话定位 |
|--------|------|-----------|
| 国产云厂商 | 阿里云百炼 | 大模型服务平台内置 Agent 2.0，Qwen 生态绑定 |
| 国际大厂 | Microsoft Copilot Studio、Google Agent Builder、AWS Bedrock Agents | 存量生态（M365/云）内的 Agent 平台（Gartner Pacesetter） |
| SaaS 新锐 | Lindy、Relevance AI、Voiceflow、Tray.ai | 面向业务人员的"Agent 即服务" |
| 传统 SaaS 延伸 | Zapier、Asana、Atlassian、Creatio、SnapLogic | 各自领域（自动化/协作/CRM）叠加 Agent 能力（Gartner Specialist） |

## 2. 阿里云百炼：Agent 2.0 深潜

百炼（Model Studio）是阿里云的大模型服务平台，2026 年推出**新版智能体应用（Agent 2.0）**（官方文档 2026-07-16 更新）。

### 2.1 Agent 2.0 核心变化

| 维度 | Agent 1.0（旧） | Agent 2.0（新） |
|------|----------------|----------------|
| 工具 | 先检索知识库再决策是否调 MCP（固定顺序） | **知识库、MCP 等统一为工具，Agent 自主规划调用顺序** |
| 过程 | 只展示最终结果 | **完整展示每一轮"规划-执行-反思"链路** |
| 适用 | 意图单一、流程固定的简单任务 | 简单问答到复杂规划全覆盖 |

### 2.2 Agent 2.0 能力配置

| 配置项 | 说明 |
|--------|------|
| 模型选择 | 推荐强工具调用能力的千问-Max 系列；支持最长回复长度、温度、enable_thinking（思考模式） |
| 系统提示词 | 支持静态文本与自定义变量 |
| 预解析文件 | 控制上传文件处理（开/关） |
| 内置工具 | **沙箱环境**运行：bash、write、read、edit、glob、grep、download_file；默认关闭、按需开启 |
| 知识库 | 作为技能（工具）由 Agent 自主规划调用，提升准确率减少幻觉 |

> 💡 **机制要点**：百炼 Agent 2.0 的"工具统一化"与 Dify Agent 沙箱、Anthropic 的 Claude Code 思路同源——**所有能力都是工具，模型自主编排**。这是 2026 年 Agent 平台的设计共识。

### 2.3 配套能力

- **高代码应用升级**（2026-04-24）：K8s 部署方案（实例级隔离、Serverless/容器）、APIG/ALB 生产级网关、MCP 工具一站式接入（知识库/工作流/插件均可作为 MCP 服务）
- **Qwen3.7 系列**（2026-06-02）：Qwen3.7-Plus 多模态智能体模型（"看、想、写、做、验"统一工作流，可复刻桌面软件）；Qwen3.7-Max 原生 Function Calling、128K 上下文、多工具并行调用
- **平台数据**（2026-03）：客户数同比增 8 倍，日均 Token 收入 5 个月增约 15 倍

## 3. 百炼 CLI：模型能力 CLI 化

2026-05-29 阿里云开源百炼 CLI（GitHub：modelstudioai/cli）：

| 维度 | 说明 |
|------|------|
| 是什么 | 一行命令让 Agent 接入 150+ 模型、十多款应用 + 知识库/记忆/联网搜索全套能力 |
| 兼容 | 原生支持 Claude Code、Qoder、OpenClaw、Hermes Agent 等主流 AI Agent 框架 |
| 模型 | Qwen、GLM、Kimi、DeepSeek、Wan 等多模态模型 |
| 组合 | 模型/应用/知识库/记忆/联网搜索 MCP/文件处理组合成完整任务流 |
| 配套 | 吞吐弹性调度（波峰波谷）、Agentic RL 强化学习（基于执行反馈迭代） |

```text
百炼的双轨模式：
轨道一：平台画布（Agent 2.0）── 业务人员在控制台构建
轨道二：CLI（百炼 CLI）───── 开发者在终端构建，平台能力变成 CLI 工具
→ 与 Coze 3.0"本地 Agent 接入"殊途同归：打通"平台能力 ↔ 本地 Agent"
```

## 4. 大厂平台：Copilot Studio / Google / AWS

Gartner 将微软、Google、AWS 划入 NCAB 象限 **Pacesetter（领跑者）**：

| 平台 | 优势 | 风险 |
|------|------|------|
| Microsoft Copilot Studio | M365/Teams/Dynamics 深度集成、企业信任、公民开发生态 | 许可昂贵、强制升级 |
| Google Agent Builder | Vertex AI + Gemini 生态 | 生态锁定 |
| AWS Bedrock Agents | AWS 全栈集成（Lambda/S3/API Gateway） | 深度依赖 AWS |

> 💡 **判断**：大厂平台适合"存量生态决定一切"的企业（已有 M365/AWS）——选它们本质是选生态，而不是选 Agent 能力；Agent 能力本身三家都与开源平台同源（都是 tool-calling + RAG 编排）。

## 5. SaaS 新锐：Lindy / Relevance AI / Voiceflow

### 5.1 Lindy——"AI 员工"平台

- **定位**：Agent 像员工一样干活（邮件、日程、CRM 更新、多步流程），不是聊天机器人
- **特点**：电话/聊天/邮件/内部 Agent 一平台，共享上下文 + HITL 控制；模板库覆盖支持/销售/市场/招聘/运营/财务；"Lindy Build"可 Prompt 建小型内部应用
- **合规**：SOC 2、GDPR、HIPAA、PIPEDA；纯云无自托管
- **价格**：Plus $49.99/月、Pro $99.99/月、Max $199.99/月（无免费档）
- **评价**：2026 榜单最高分 8.6/10——无需技术帮助处理最广范围的业务自动化

### 5.2 Relevance AI——Agent 团队

- **定位**：面向 GTM 团队（销售/市场/运营）的非技术多 Agent 编排
- **特点**：400+ 模板、2000+ 集成（Salesforce/HubSpot/Slack）、MCP 支持、BYO API Key、SOC 2 Type II
- **价格**：免费档 200 actions/月，Pro $19/月，Team $234/月
- **弱点**：双计费模型（Actions + 供应商积分）成本不可预测；5 步以上复杂链路约 1/8 需人工介入

### 5.3 Voiceflow——客户对话

- **定位**：客户面向的对话 Agent（聊天/语音/WhatsApp）视觉画布，300+ 集成
- **特点**：免费档品类里最实用；50 万+ 团队；宣称 70% 工单减少
- **局限**：不适合后台办公自动化；月额度过期作废；白标锁定 Enterprise

### 5.4 Tray.ai——企业集成 + Agent

- **定位**：企业集成平台 + 无代码 Agent 构建，被 Gartner 评为 **Pioneer**
- **特点**：iPaaS 基因（复杂集成）+ Agent 编排

## 6. 企业落地：编排差距与 Agent 蔓延

Gartner 2026 调研的核心结论——**落地障碍不是技术，是编排（orchestration）**：

| 障碍 | 表现 |
|------|------|
| 治理缺口 | 权限、审批、审计、责任归属未定义 |
| 多系统连接 | 与遗留 ERP/核心系统打通难 |
| 安全模型 | RBAC、数据边界、Agent 权限最小化 |
| 基建 | 可靠、可审计的大规模 Agent 运行时 |
| 试点成功、规模化失败 | 单个部门试点 OK，跨部门推广失败 |

**新问题：Agent 蔓延（Agent Sprawl）**——从 1 个 Agent 到几十个后：
- 未知的 Agent 清单（谁建了哪些？）
- 重复工作流
- 所有权/审批不清
- 成本不可见

> ⚠️ **治理清单（企业落地必备）**：Agent 注册表（谁/什么/权限/数据域）→ 统一审批流 → 成本标签 → 权限最小化 → 审计日志 → 失败/降级预案。

## 7. 云厂商 vs 开源 vs SaaS 选型

```text
第一问：构建者是谁？
├── 业务人员（市场/运营/HR）自建 → SaaS（Lindy/Relevance/Voiceflow）或 Coze
├── 开发者 + 业务协作 → 云厂商平台（百炼/Copilot Studio）
└── 纯技术团队 → 开源自托管（Dify/n8n/Langflow）

第二问：数据主权？
├── 必须内网/私有化 → 开源自托管
├── 云厂商区域内合规 → 百炼（国内）/Copilot Studio（国际）
└── 无严格要求 → 任意

第三问：与存量生态？
├── 深度使用 M365 → Copilot Studio
├── 深度使用阿里云/Qwen → 百炼
├── 已有大量 n8n/Zapier 自动化 → 其 Agent 能力延伸
└── 无强绑定 → 按构建者与数据主权选
```

| 场景 | 推荐 |
|------|------|
| 阿里云用户 + Qwen 模型 | 百炼 Agent 2.0 |
| 字节生态 / 业务人员零代码 | Coze 扣子 |
| 已有 M365/AWS 的企业 | Copilot Studio / Bedrock |
| 业务部门自建员工型 Agent | Lindy |
| 销售/市场 Agent 团队 | Relevance AI |
| 客服对话（聊天/语音） | Voiceflow |
| 企业级集成 + Agent | Tray.ai |

## 8. 核心要点

> 🎯 **核心要点**：
> 1. **百炼 Agent 2.0**（2026-07 基准）核心变化：知识库/MCP 统一为工具、规划-执行-反思过程透明——与 Dify Agent、Claude Code 同一设计共识
> 2. **百炼 CLI**（2026-05-29 开源）让平台能力 CLI 化，原生兼容 Claude Code/Qoder/OpenClaw——"平台 ↔ 本地 Agent"双向打通
> 3. 大厂平台（Copilot Studio 等）本质是**生态选择**而非能力选择
> 4. SaaS 新锐（Lindy 8.6/10 最高分）最快上手但有**锁定与成本风险**（Lindy 迁移一个 Agent 重建 1-3 小时）
> 5. 企业落地第一障碍是**编排/治理**不是技术；注意 Agent 蔓延与成本可见性

---

**上一模块**：[05 Flowise 与 Langflow](05-Flowise%20与%20Langflow：LangChain%20可视化双子星.md)　**下一模块**：[07 低代码平台核心机制](07-低代码平台核心机制：从画布到运行时.md)　**返回总览**：[00 总览](00-总览：低代码%20Agent%20平台知识体系.md)

## 【参考来源】

- [阿里云百炼：新版智能体应用（Agent 2.0）官方文档](https://help.aliyun.com/zh/model-studio/new-single-agent-application)
- [阿里云开源百炼CLI，Agent可调用全套模型和应用能力（财联社）](https://www.cls.cn/detail/2385243)
- [阿里云百炼核心能力CLI化（IT168）](https://cloud.it168.com/a2026/0601/6931/000006931909.shtml)
- [高代码应用全新升级_大模型服务平台百炼（阿里云产品动态）](https://cn.aliyun.com/product/news/28889)
- [Qwen3.7-Plus上线！多模态智能体新基座（量子位）](https://www.qbitai.com/2026/06/427730.html)
- [Gartner: Mapping the Emerging Market Landscape of No-Code Agent Builders](https://www.gartner.com/en/articles/no-code-agent-builders-emerging-market)
- [Gartner Emerging Market Quadrant for No-Code Agent Builders（官方报告页）](https://www.gartner.com/doc/reprints?id=1-2NIKE5SY&ct=260609)
- [Lindy: Botpress vs Voiceflow vs Lindy: Which One Works Best in 2026](https://www.lindy.ai/blog/botpress-vs-voiceflow)
- [Relevance AI vs Voiceflow (2026)](https://www.respan.ai/market-map/compare/relevance-ai-vs-voiceflow)
- [Glean: Agent orchestration platforms compared](https://www.glean.com/blog/agent-orchestration-platforms-compared)
- [Tray.ai: Gartner Defined a New AI Category](https://tray.ai/blog/gartner-pioneer-no-code-agent-builders-2026/)
- [No-Code AI Agents: 14 Platforms Compared (March 2026)](https://www.morphllm.com/no-code-ai-agent)
- [Deloitte 推出 GenW.AI 低代码平台（The Hindu）](https://www.thehindu.com/business/deloitte-to-unveil-fully-india-developed-ai-platform-that-offers-scale-speed-to-global-enterprises/article70616893.ece)
- [Oracle AI Agent Studio for Fusion Applications（Tech Monitor）](https://www.techmonitor.ai/news/oracle-unveils-ai-native-builder-for-agentic-apps-in-fusion-platform)
