# 00 总览：低代码 Agent 平台知识体系

> 定位：框架层（工程加速）第二站——用 Dify、Coze 扣子、n8n、Langflow 等低代码/零代码平台快速验证 Agent 想法，再决定是否迁移到代码框架（2026-08 基准）

## 📚 目录

1. [本模块的定位](#1-本模块的定位)
2. [知识体系导图](#2-知识体系导图)
3. [模块导航](#3-模块导航)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [与关联体系的分工](#6-与关联体系的分工)
7. [2026 版本窗口](#7-2026-版本窗口)

## 1. 本模块的定位

```text
Agent 内核理论模块      = 原理：Agent 是什么、循环怎么转（必修）
四大核心组件模块        = 组件：工具/记忆/规划/执行 怎么做
单 Agent 框架模块       = 手写/代码框架：选哪个框架、每个框架怎么用
本模块（低代码平台）    = 拖拽式平台：不动代码快速出原型、验证想法
多 Agent 框架模块       = 更复杂编排（进阶）
工程化 & 部署模块       = 生产化：可观测/测试/部署（落地）

一句话分工：本模块回答"一个想法先用哪个低代码平台 1 天出原型、验证后再决定怎么迁移"
```

> 🎯 **核心价值**：2026 年低代码 Agent 平台已从"玩具"成长为生产级工具——Gartner 在 2026 年首次发布 **No-Code Agent Builders（NCAB）新兴市场象限**，预测 2028 年公民开发者构建的 AI Agent 数量将超过传统开发者构建的应用。本模块把主流平台（开源：Dify/n8n/Langflow/Flowise；云厂商：阿里云百炼/Coze 扣子；SaaS：Lindy/Relevance AI 等）的核心机制讲透，给出选型决策图，并回答最关键的问题：**什么时候用低代码平台、什么时候迁移到手写框架**。

## 2. 知识体系导图

```text
低代码 Agent 平台知识体系
│
├── 00 总览（本文件）
│
├── 全景篇
│   └── 01 平台全景与选型 ── 平台分类地图 / 选型决策树 / Gartner NCAB 象限
│
├── 平台深潜篇（每个平台：版本 / 核心机制 / 适用）
│   ├── 02 Dify ── 开源标杆：Agent 沙箱 / Workflow / Skills / MCP
│   ├── 03 Coze 扣子 ── 零代码：Agent Skills / 工作流 / AI 团队协作
│   ├── 04 n8n ── AI 自动化：Agent 节点 / 工具化工作流 / HITL
│   ├── 05 Flowise 与 Langflow ── LangChain 可视化双子星 / MCP 一键导出
│   └── 06 云厂商与 SaaS ── 百炼 Agent 2.0 / Copilot Studio / Lindy 生态
│
├── 原理篇
│   └── 07 低代码平台核心机制 ── 画布 → 节点执行引擎 → 运行时 → 发布
│
├── 对比篇
│   ├── 08 平台能力横评与选型决策 ── RAG/工作流/多Agent/可观测/许可五维
│   └── 09 原型快速验证实战 ── 低代码 vs 手写框架 / 验证后迁移路线
│
└── 检验篇
    └── 10 面试与自测 ── 面试题 / 自测 / 毕业检查单
```

## 3. 模块导航

| 序号 | 模块 | 核心内容 | 场景 |
|:---:|------|---------|------|
| 01 | 平台全景与选型 | 平台分类、选型决策树、Gartner 象限 | 先读（建立全局） |
| 02 | Dify | Agent 沙箱、Workflow、Skills、MCP 升级 | 要开源+生产级 RAG 时 |
| 03 | Coze 扣子 | 零代码建 Agent、Agent Skills、项目空间 | 不写代码、快速演示时 |
| 04 | n8n | AI Agent 节点、工作流即工具、HITL | 连接业务系统自动化时 |
| 05 | Flowise & Langflow | LangChain 可视化、MCP 一键导出 | Python 生态 / 让 CLI Agent 调用流程时 |
| 06 | 云厂商与 SaaS | 百炼 Agent 2.0、Copilot Studio、Lindy 等 | 用云厂商全家桶 / 业务人员自建时 |
| 07 | 核心机制 | 画布→执行引擎→运行时→发布 | 理解"平台在做什么" |
| 08 | 能力横评 | RAG/工作流/多Agent/可观测/许可五维 | 平台间抉择时 |
| 09 | 原型验证实战 | 低代码 vs 手写、验证后迁移路线 | 动手前/迁移时 |
| 10 | 面试与自测 | 面试题、自测、毕业检查单 | 求职/自检 |

## 4. 学习路线推荐

**路线 A：快速上手（1 天）**——01 → 02（或 03，按开源/零代码偏好选一个）→ 09
> 先懂平台地图，精一个平台出原型，最后学会"何时迁移"。

**路线 B：全面掌握（1 周）**——01 → 07 → 02 → 03 → 04 → 05 → 06 → 08 → 09
> 所有平台过一遍，重点对比"画布能力 vs 运行时能力"的机制差异。

**路线 C：面试冲刺（2 天）**——10 → 01 → 07 → 08
> 以题为纲：先会答"低代码 vs 手写怎么选"类问题，再补机制细节。

## 5. 核心概念速查

| 概念 | 一句话 | 关键数字 |
|------|--------|---------|
| 低代码 Agent 平台 | 用可视化画布+配置替代手写代码构建 Agent 的开发平台 | 2026 Gartner 首次设立 NCAB 品类 |
| NCAB | No-Code Agent Builders：SaaS 交付的可视化 Agent 构建平台 | Gartner 2026 新兴市场象限 |
| 公民开发者 | 非专业开发者用低代码平台构建 Agent | Gartner 预测 2028 年其产出超过传统开发者 |
| Dify | 开源 Agent 平台标杆：Workflow+Agent+知识库+RAG 一体 | v1.16.0（2026-07-19）；GitHub ~147k stars |
| Dify Agent 沙箱 | Agent 在完整 Linux 沙箱内执行代码、装包、调 API | 1.16.0 开放 Beta |
| Coze 扣子 | 字节跳动零代码 Agent 平台，"AI 团队协作"OS | 3.0（2026-06-01）；2025-07 开源 |
| Agent Skills | 把领域最佳实践封装成可复用技能模块（拖拽配置） | Coze 技能商店 12 领域 300+ 技能 |
| n8n | 节点式自动化平台，工作流可被 Agent 当工具调用 | 2.33.0（2026-07-28）；~400+ 应用连接器 |
| Langflow | Python 版可视化流程构建器，流程可一键导出为 MCP Server | 1.9（2026-04-13）；GitHub ~150k stars |
| 阿里云百炼 | 阿里云大模型服务平台，Agent 2.0 工具统一化 | Agent 2.0（2026-07 文档基准）；百炼 CLI 2026-05 开源 |
| 编排差距 | Gartner 认为企业 Agent 落地最大障碍非技术而是治理编排 | 42% 企业 2026 年部署 AI Agent（2025 年 17%） |
| 原型快速验证 | 用低代码平台 1-3 天验证需求/交互/流程，验证后决定去留 | 本模块核心方法论（09 篇） |

## 6. 与关联体系的分工

| 体系 | 分工 | 本模块怎么用 |
|------|------|------------|
| [Agent 内核理论模块](../../Agent%20内核理论模块/) | Agent 原理、循环概念 | 平台背后的循环机制以原理为基础 |
| [四大核心组件模块](../../Agent%20四大核心组件/) | 工具/记忆/规划/执行组件设计 | 平台里的工具、记忆节点对应这些概念 |
| [单 Agent 框架](../单%20Agent%20框架/00-总览：单%20Agent%20框架知识体系.md) | 代码框架：LangGraph/OpenAI SDK 等 | 低代码验证通过后迁移到哪个框架（09 篇决策） |
| [多 Agent（Multi-Agent）框架](../多%20Agent（Multi‑Agent）框架/) | 多 Agent 编排框架 | 平台的多 Agent 编排对照代码实现 |
| [新兴 SDK](../新兴%20SDK/) | 更小的新框架/协议 | 低代码平台内部也可能依赖这些 SDK |
| [工程化 & 部署模块](../../Agent%20工程化%20&%20部署模块（从%20demo%20到可用应用）/00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md) | 可观测/测试/部署 | 低代码原型验证后的生产化路径 |
| [MCP 协议体系](../../Agent与MCP协议/) | MCP 协议原理 | 平台工具接入（Dify MCP 升级、Langflow MCP 导出）依赖此 |
| [主流 Agent 范式](../../主流%20Agent%20范式/) | ReAct/计划-执行等工作流模式 | 平台画布编排的工作流对应范式 |
| [Function Calling 体系](../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md) | 协议层原理 | 平台的 Agent 节点基于工具调用循环 |

## 7. 2026 版本窗口

> 本模块以 2026-08 为基准，以下为检索到的版本事实（详见各篇【参考来源】）：
>
> - **Dify v1.16.0**（2026-07-19）：Dify Agent 开放 Beta——原生 Linux 沙箱、Agent Builder、Skills 系统、Workflow 中 Agent 一等公民；MCP 协议升级至 2025-06-18；GPT-5.6 Responses 兼容
> - **Coze 扣子 3.0**（2026-06-01）：AI 团队协作架构、本地 Agent（Claude Code/Codex CLI/OpenClaw）一键接入、项目空间、三端协同；2.0（2026-01）引入 Agent Skills/Agent Plan/Agent Coding/Agent Office
> - **n8n 2.x**（2026-01 起 2.0 大版本）：AI Agent 节点转正（2.33.0 预览节点升级为 V1）；工作流可被 Agent 当工具调用；HITL 支持
> - **Langflow 1.9**（2026-04-13）：Flow DevOps Toolkit、流程一键导出 MCP Server、流程版本管理；GitHub ~152k stars（2026-08-04）
> - **阿里云百炼 Agent 2.0**（2026-07 文档基准）：工具统一化（知识库/MCP 均为工具）、规划-执行-反思过程透明；百炼 CLI 2026-05-29 开源
> - **Gartner NCAB 象限**（2026 首发）：42% 企业 2026 年计划部署 AI Agent（2025 仅 17% 在生产中）；Lindy/Tray.ai 等新一代 SaaS 平台崛起
> - 未确认项：腾讯元器等国产平台 2026 版本号待补充检索，本模块不做断言

---

**下一模块**：[01 平台全景与选型](01-平台全景与选型：2026%20低代码%20Agent%20平台地图.md)　**返回上级**：[Agent 开发框架层（工程加速）](../)

## 【参考来源】

- [Gartner: Mapping the Emerging Market Landscape of No-Code Agent Builders](https://www.gartner.com/en/articles/no-code-agent-builders-emerging-market)
- [Dify Release v1.16.0](https://github.com/langgenius/dify/releases/tag/1.16.0)
- [扣子 3.0 正式上线（太平洋电脑网）](https://news.pconline.com.cn/2163/21635593.html)
- [n8n Release 2.33.0](https://github.com/n8n-io/n8n/releases/tag/n8n%402.33.0)
- [Langflow 1.9 released](https://www.langflow.org/blog/langflow-1-9)
- [阿里云百炼：新版智能体应用（Agent 2.0）](https://help.aliyun.com/zh/model-studio/new-single-agent-application)
- [阿里云开源百炼CLI](https://www.cls.cn/detail/2385243)
