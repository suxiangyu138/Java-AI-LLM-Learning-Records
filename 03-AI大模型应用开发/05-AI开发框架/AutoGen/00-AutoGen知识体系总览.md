# AutoGen 知识体系总览

> 微软的多智能体对话框架——"智能体即对话参与者"，曾经的多 Agent 编排标杆（54k+ stars）；2025 年末与 Semantic Kernel 合并为 Microsoft Agent Framework（MAF 1.0 于 2026-04 GA），AutoGen 进入维护模式。本体系完整覆盖其思想、用法与迁移。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 年关键状态：维护模式与 MAF](#5-2026-年关键状态维护模式与-maf)
6. [与同级框架的分工](#6-与同级框架的分工)
7. [学习计划与 FAQ](#7-学习计划与-faq)

## 1. 知识体系导图

```
AutoGen（微软 · 多智能体对话框架 · 54k+ stars）
│
├── 01 概述与心智模型      "智能体即对话参与者"
│   ├── 群聊式多 Agent 协作
│   ├── 委派/批评/工具/代码执行/人类审批
│   └── vs LangChain / LangGraph
│
├── 02 核心概念            对话是编排的核心
│   ├── AssistantAgent / UserProxyAgent
│   ├── GroupChat / GroupChatManager
│   └── 消息流与终止条件
│
├── 03 快速上手            安装 + 第一个多 Agent 应用
│   ├── v0.4 三层架构安装（agentchat/ext）
│   └── 双 Agent 对话代码全解
│
├── 04 工具调用与代码执行   Agent 的"手脚"
│   ├── FunctionTool 包装
│   ├── 代码执行器
│   └── 工具注册与错误处理
│
├── 05 多 Agent 协作模式    编排模式全解
│   ├── GroupChat 三种模式
│   ├── 分层编排 / MagenticOne
│   └── 人类审批介入
│
├── 06 AutoGen Studio     低代码可视化
│   ├── 工作流搭建 / 调试
│   └── 评估与部署
│
├── 07 版本演进与生态分裂    v0.2 → v0.4 → 社区分叉
│   ├── v0.4 三层重构（事件驱动）
│   ├── AG2 分叉（核心作者出走）
│   └── 社区现状与选型
│
└── 08 MAF 迁移与未来       微软的下一站
    ├── Semantic Kernel 合并
    ├── MAF 1.0 特性（图工作流/DevUI/负责任 AI）
    ├── 迁移对照表（Team→Workflow 等）
    └── 学习建议与面试
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | 概述与心智模型 | 群聊式多 Agent、核心抽象、框架对比 | 入门必读 |
| 02 | 核心概念 | Agent/对话/GroupChat、消息流、终止条件 | 入门必读 |
| 03 | 快速上手 | v0.4 安装、双 Agent 代码全解 | 新手第一步 |
| 04 | 工具调用与代码执行 | FunctionTool、代码执行器、错误处理 | 动手核心 |
| 05 | 多 Agent 协作模式 | GroupChat 模式、分层编排、人类审批 | 进阶 |
| 06 | AutoGen Studio | 低代码工作流、调试、评估 | 工程化 |
| 07 | 版本演进与生态分裂 | v0.2→v0.4 重构、AG2 分叉、现状 | 选型必读 |
| 08 | MAF 迁移与未来 | 合并、迁移对照、学习建议 | 面试/落地 |

## 3. 学习路线推荐

**路线一：理解思想（半天）**
01 概述 → 02 核心概念 → 07 版本演进（理解"对话编排"思想与框架命运）

**路线二：动手实践（2 天）**
03 快速上手 → 04 工具调用 → 05 协作模式 → 06 Studio

**路线三：生产/迁移视角（1 天）**
07 生态分裂 → 08 MAF 迁移 → 对照本仓库 `LangGraph/` 体系学习迁移方向

> ⚠️ 2026 学习建议：AutoGen 的思想（对话编排）仍值得学，但**新项目官方推荐直接用 Microsoft Agent Framework（MAF）**——本体系 08 篇给出迁移路径；老项目（v0.2 代码）看 AG2。

## 4. 核心概念速查

| 概念 | 一句话速记 |
|---|---|
| AutoGen | 微软多智能体框架："智能体即对话参与者" |
| AssistantAgent | 默认助手 Agent，可配工具/模型 |
| UserProxyAgent | 人类代理 Agent，执行代码/请求输入 |
| GroupChat | 群聊：多个 Agent 在一个对话中协作 |
| GroupChatManager | 群聊主持人：决定下一个发言者 |
| 终止条件 | 对话结束机制（max_messages/关键词/函数） |
| FunctionTool | 工具包装类（v0.4 中把函数转成 Agent 工具） |
| 代码执行器 | 让 Agent 写代码并执行（隔离沙箱） |
| 委派 | Agent 把任务转给另一个 Agent |
| v0.4 三层 | autogen-core / agentchat / ext（事件驱动重构） |
| AG2 | 社区分叉（v0.2 API，核心作者 Chi Wang 主导） |
| MAF | Microsoft Agent Framework（AutoGen + Semantic Kernel 合并，2026-04 GA） |
| MagenticOne | 微软的多 Agent 团队参考实现 |

## 5. 2026 年关键状态：维护模式与 MAF

```
AutoGen 当前状态（2026-08 基准）：
✅ 进入维护模式——不再新增功能，仅 bug 修复与安全补丁
✅ GitHub 54k+ stars（历史峰值）
✅ 2025 年末与 Semantic Kernel 合并 → Microsoft Agent Framework
✅ MAF 1.0 于 2026-04-03 全面可用（GA，MIT 许可证）
✅ 新项目官方推荐直接用 MAF
```

### 对本体系学习的意义

| 视角 | 意义 |
|---|---|
| 学习思想 | 对话编排是 Agent 框架的重要思想，理解它才能理解 MAF |
| 存量项目 | 大量 AutoGen 代码在生产运行，维护与迁移需要知识 |
| 面试价值 | "AutoGen 为什么被合并"是 2026 年 Agent 面试高频题 |
| 迁移路径 | 本体系 08 篇给出 AutoGen → MAF 的完整对照 |

## 6. 与同级框架的分工

同级目录已有 LangChain/LangGraph/LlamaIndex/Haystack/OpenAI Agents SDK 等体系：

| 框架 | 定位 | AutoGen 的关系 |
|---|---|---|
| LangChain | 通用 LLM 应用开发链 | 单 Agent 流程为主 |
| LangGraph | 图编排 Agent（状态机） | **最接近的竞品**（AutoGen 对话式 vs LangGraph 图式） |
| LlamaIndex | 数据框架（RAG 为主） | 侧重数据而非编排 |
| Haystack | 生产级 NLP 管线 | 管道式 |
| OpenAI Agents SDK | OpenAI 官方多 Agent | 后起之秀 |
| **MAF（AutoGen 继任）** | 微软 Agent 统一框架 | 继承 AutoGen 编排 + SK 企业集成 |

> 学习定位：AutoGen 体系的价值是"对话式多 Agent 编排"思想的完整档案 + 存量代码迁移指南；新项目编排选型优先看 MAF（本体系 08 篇）与 LangGraph 体系。

## 7. 学习计划与 FAQ

### 学习计划

| 天 | 内容 | 目标 |
|---|---|---|
| Day 1 | 01 概述 + 02 核心概念 + 07 版本演进 | 理解思想与框架命运 |
| Day 2 | 03 快速上手 + 04 工具 + 05 协作模式 | 动手跑通多 Agent |
| Day 3 | 06 Studio + 08 MAF 迁移 | 低代码与迁移视角 |

### 常见疑问快答

| 疑问 | 回答 |
|---|---|
| 现在学 AutoGen 过时吗？ | 思想不过时——对话编排是所有 Agent 框架的基础课；代码视角看 MAF |
| 新项目用 AutoGen 吗？ | 官方推荐 MAF；非微软栈用 LangGraph/OpenAI Agents SDK |
| 存量 v0.2 代码怎么办？ | AG2 延续兼容；或按 08 篇迁移到 MAF |
| 面试会问吗？ | 会——"框架演进"是 2026 高频话题（为什么合并/迁移要点） |
| 与 LangGraph 学哪个？ | 都学思想：AutoGen 对话式 + LangGraph 图式（MAF 是图式） |

### 学习心态

```
AutoGen 体系定位："思想的完整档案 + 存量的迁移手册"
读它不是为了"用它建新项目"，而是为了：
① 理解多 Agent 编排的对话流派
② 看懂 MAF 从哪来、为什么这么设计
③ 面试讲得出框架演进的故事
```

---

## 参考来源

- [AutoGen 架构演进全梳理：从 v0.4 到 Microsoft Agent Framework（腾讯云）](https://cloud.tencent.cn/developer/article/2635493)
- [Microsoft Retires AutoGen: First Major Agent Framework Sunset（AgentMarketCap, 2026-04）](https://agentmarketcap.ai/blog/2026/04/13/microsoft-autogen-maintenance-mode-agent-framework-sunset-2026)
- [从爆火到合并：AutoGen 的来龙去脉（新浪财经, 2026-03）](https://finance.sina.com.cn/wm/2026-03-13/doc-inhqvxri7748766.shtml)
- [AutoGen v0.4 vs. AG2: The Microsoft Community Split Explained（Blck Alpaca）](https://blckalpaca.at/en/knowledge-base/ai-agents/ai-agent-frameworks-comparison/autogen-vs-ag2)
- [AutoGen 落幕：第一次 AI 框架 Succession（今日头条）](https://m.toutiao.com/article/7651406746270908928/)

---

**下一模块**：[01-AutoGen概述-多智能体对话框架](01-AutoGen概述-多智能体对话框架.md)
