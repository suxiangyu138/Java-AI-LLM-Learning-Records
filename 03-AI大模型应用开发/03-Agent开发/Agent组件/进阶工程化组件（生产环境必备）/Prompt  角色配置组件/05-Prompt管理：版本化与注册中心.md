# Prompt 管理：版本化与注册中心

> Prompt 管理 = 把提示词当作一等版本化工件管理：存储/命名/版本化/参数化/评估/独立于应用代码部署。2026 核心规范：**不可变版本**（发布后不原地改）、**语义化版本**（major 管行为契约）、**版本化完整执行上下文**（prompt+模型+参数+工具+检索——否则不可复现）。本章给 PromptOps 管理完整设计。

## 1. 为什么提示词要独立管理

| 硬编码提示词的问题 | 注册中心管理 |
|---|---|
| 变更无追踪 | 版本化 + 提交信息 |
| 部署耦合（改提示=改代码） | 行为迭代与发布周期解耦 |
| 环境漂移 | 环境标记（dev/staging/prod） |
| 无法独立测试 | 评估门禁（09 篇） |
| 跨服务重复 | 按名引用单一来源 |

> 🎯 核心要点：**应用代码按名引用提示词，不内联字符串**——"30+ 提示词跨多服务的团队没有 eval 循环，质量回归会悄悄漏进来"（2026 实证）；提示词管理是"行为与代码分离"的第一性工具。

## 2. 版本化规范

| 规范 | 内容 |
|---|---|
| 不可变版本 | 发布后不原地编辑——变更 = 新版本（提交信息/作者/时间戳/评估结果） |
| 语义化版本 | major：行为/输出契约变更；minor：能力改进；patch：修复 |
| **完整执行上下文** | 版本化 prompt + 模型 + 参数 + 工具定义 + 检索配置——否则无法复现/回滚真实用户可见行为 |
| 引用方式 | 按名 + 标签加载（运行时解析） |

> ⚠️ 2026 关键细节：**"版本化 prompt 文本不够，要版本化完整执行上下文"**——同一个 v1.2 提示词配不同模型参数就是不同行为；回滚"用户可见行为"必须连模型/参数/工具一起回。

## 3. 注册中心形态

| 形态 | 特点 | 适用 |
|---|---|---|
| Git 仓库 | 版本控制天然、可审阅 | 小团队起步 |
| 托管注册中心 | 非工程师可贡献、UI 管理 | 企业 |
| OSS 自托管 | 数据自控 | 合规要求 |
| 框架内建 | LangSmith/Langfuse 提示管理 | 框架生态 |

> 💡 注册中心的价值之一：**非工程师（产品/合规）可贡献提示词**——行为迭代从"提工单"变成"提版本"；评估门禁（09 篇）兜底质量。

## 4. 环境标记：dev/staging/prod

| 环境 | 指向 | 规则 |
|---|---|---|
| dev | 最新候选 | 开发迭代 |
| staging | 验证中的候选 | 评估门禁通过前 |
| prod | **固定的已审稳定版本** | 绝不浮动 latest |

> 🎯 核心要点：**"生产从不浮动 latest"是铁律**——浮动态 = 未验证变更直接上线；git 式工作流：提交版本 → 打部署标签（Production/Staging/Development）→ 运行时按名+标签加载。

## 5. 评估门禁：晋升的条件

| 门禁 | 内容 |
|---|---|
| 回归测试 | 重放历史生产 trace 对新版本——二进制通过/失败 |
| A/B 测试 | 单变量对比——分数分布对比（非均值） |
| 内置评估器 | Context Adherence/Groundedness/Task Completion/Function Calling 等 |
| 底线规则 | **新版本低于当前生产版本阈值 → 不授予 Production 标签**（坏候选留在 Staging） |

> ⚠️ 2026 底线：**"低于当前生产版本阈值的新版本永远拿不到 Production 标签"**——评估门禁不是"过了就上"，是"比在线的强才上"（评估 09 篇 minPassRate 联动）。

## 6. 与相邻体系联动

| 体系 | 联动 |
|---|---|
| 评估 09 篇 | CI 门禁执行者（minPassRate） |
| 观测 06 篇 | trace 携带 prompt 版本属性——漂移归因（06 篇） |
| Multi-Agent 03 篇 | Agent Card 的 skills 描述也是版本化对象 |
| 自动优化 08 篇 | 优化工具只在有版本化对象时有用——注册中心是前提 |

> 💡 **自动优化的前置条件**（08 篇）：SePO/GEPA 等优化工具"只在有版本化对象时有用"——没有注册中心，优化结果无法落地为可回滚版本。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "版本化=Git 提交" | Git 只是载体——不可变+语义化+完整上下文 |
| "版本化 prompt 文本够" | 完整执行上下文（模型/参数/工具/检索） |
| "生产用 latest" | 绝不浮动——固定已审版本 |
| "评估过了就上" | 比在线的强才上——底线规则 |
| "提示词改=发版" | 注册中心——行为迭代与代码解耦 |
| "内联字符串省事" | 无追踪/耦合/漂移——按名引用 |
| "注册中心是运维的事" | 非工程师贡献入口——行为资产 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Prompt 管理？ | 提示词一等工件——版本化/参数化/独立部署 |
| 不可变版本？ | 发布后不原地改——变更即新版本 |
| 语义化版本？ | major 行为契约/minor 能力/patch 修复 |
| 完整执行上下文？ | prompt+模型+参数+工具+检索——可复现 |
| 环境标记？ | dev 候选/staging 验证/prod 固定 |
| 生产浮动 latest？ | 绝不——铁律 |
| 评估底线？ | 比在线的强才上 |
| 注册中心价值？ | 非工程师贡献 + 行为与代码解耦 |
| 与评估联动？ | CI 门禁（minPassRate） |
| 与观测联动？ | trace 带版本——漂移归因 |
| 与自动优化？ | 优化工具的前提是版本化对象 |
| 30+ 提示词团队？ | 无 eval 循环质量回归悄悄漏 |

---

**下一模块**：[06-生命周期：部署-漂移-回滚](06-生命周期：部署-漂移-回滚.md)　**返回总览**：[00-Prompt 角色配置组件总览](00-Prompt角色配置组件总览.md)

## 参考来源

- [PromptOps: An End-to-End Architecture for Prompt Engineering Lifecycle Management（IEEE 2026）](https://ieeexplore.ieee.org/document/11609183)
- [How to Version & Rollback LLM Agent Prompts（Arthur AI）](https://www.arthur.ai/column/version-rollback-prompts-llm-agents)
- [Implement Prompt Versioning and Optimization（Microsoft Learn）](https://learn.microsoft.com/zh-cn/training/modules/aaai-design-advanced-prompt-production-agents/6-implement-prompt-version-optimization)
- [Best Prompt Management Platform for AI Agents in 2026（FutureAGI）](https://futureagi.com/blog/best-prompt-management-platform-ai-agents-2026/)
- [Centralized Prompt Management for Enterprise（Atlan）](https://atlan.com/know/ai-agent/centralized-prompt-management-for-enterprise/)
