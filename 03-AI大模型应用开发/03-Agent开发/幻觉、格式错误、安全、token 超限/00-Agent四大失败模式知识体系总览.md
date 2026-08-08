# Agent 四大失败模式知识体系总览

> 定位：幻觉、格式错误、安全、Token 超限——Agent 生产环境四个最高频的失败源，本体系覆盖原理→防御→可观测→生产落地全链路（2026-08 基准）

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 版本窗口](#5-2026-版本窗口)

## 1. 知识体系导图

```text
Agent 四大失败模式（生产必备）
│
├── 00 总览（本文件）
│
├── 01 失败模式全景与诊断 ── 频率分布 / 五步诊断法 / 分层排查
│
├── 02 幻觉：原理与类型 ── 本质 → 类型学 → Agent 场景特殊性 → 级联幻觉
│        │
│        └── 03 幻觉治理：接地与验证 ── RAG接地 / 引用溯源 / 多Agent验证 / 评估指标
│
├── 04 格式错误：原理与防御 ── JSON失败模式 → 结构化输出 → 约束解码 → 修复循环
│
├── 05 Token超限：上下文窗口机制 ── 窗口机制 / 超限行为 / Lost-in-the-Middle / NoLiMa
│        │
│        └── 06 上下文管理工程 ── 滑动窗口 / 压缩摘要 / 结构化状态 / 外部记忆 / Prompt Caching
│
├── 07 Agent安全：提示注入与OWASP Top 10 ── 直接/间接注入 / LLM01-LLM10 全解读
│        │
│        └── 08 Agent安全：过度授权与数据治理 ── Excessive Agency / 提示词泄露 / 向量库 / 治理
│
├── 09 失败模式可观测性与评估 ── 遥测 / 指标 / LLM-as-Judge / 离线在线评估
│
└── 10 生产实践与面试冲刺 ── 落地清单 / 真实案例 / 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 失败模式全景与诊断 | 四类失败频率分布、诊断五步法、现象→根因映射 | 全部（先读） |
| 02 | 幻觉：原理与类型 | 幻觉定义、四维类型学、Agent 级联幻觉、成因机制 | 全部 |
| 03 | 幻觉治理：接地与验证 | RAG 接地、引用溯源、检索质量、多 Agent 验证、指标 | 进阶 |
| 04 | 格式错误：原理与防御 | JSON 失败模式、Structured Output、约束解码工具链、修复循环 | 全部 |
| 05 | Token 超限：上下文窗口机制 | 窗口机制、超限行为、Lost-in-the-Middle、NoLiMa 基准 | 全部 |
| 06 | 上下文管理工程 | 四大压缩策略、Deep Agents 三层压缩、Prompt Caching、成本 | 进阶 |
| 07 | Agent 安全：提示注入与 OWASP Top 10 | 直接/间接注入、注入七技术、LLM01-LLM10、多层防御 | 全部 |
| 08 | Agent 安全：过度授权与数据治理 | Excessive Agency、提示词泄露、向量库弱点、预算治理 | 进阶 |
| 09 | 可观测性与评估 | OTel 追踪、关键指标、LLM-as-Judge 校准、离线/在线评估 | 进阶 |
| 10 | 生产实践与面试冲刺 | 落地清单、案例复盘、14 避坑、面试题 | 全部 |

## 3. 学习路线推荐

**路线 A：面试速成（1 天）**——01 → 02 → 04 → 05 → 07 → 10
> 覆盖"幻觉怎么治？JSON 老报错？上下文超了？提示注入怎么办？"四个面试必问题。

**路线 B：生产工程师（1 周）**——01 → 02 → 03 → 04 → 05 → 06 → 09 → 10
> 完整防御链：先懂原理，再学治理，最后建立可观测性闭环。

**路线 C：Agent 安全专项（2 天）**——01 → 07 → 08 → 09 → 10
> 面向安全敏感场景（金融、企业数据、合规），配合 OWASP Top 10 原文食用。

## 4. 核心概念速查

| 概念 | 一句话定义 | 关键数字 |
|------|-----------|---------|
| 幻觉（Hallucination） | 模型生成与事实/上下文/指令不符的内容 | Agent 场景 7-10% 幻觉率（ORCA-bench） |
| 级联幻觉（Cascading） | 多步推理中幻觉在步骤间传播放大（CHARM） | 四型级联分类 |
| 引用溯源 | 强制输出 `[D12]` 式文档编号引用 | Robust-GAP 达 100% 引用 F1 |
| 格式错误 | 输出无法被解析（坏 JSON、错字段、漏 schema） | 解析类失败占生产故障 ~38%（AgentFixer） |
| Structured Output | 原生 API 级 schema 强制（OpenAI strict / Claude） | 100% 合规但 schema 覆盖有限 |
| 约束解码（Constrained Decoding） | 生成期屏蔽非法 token（Outlines/XGrammar） | 99%+ 合法 JSON，<10% 延迟开销 |
| Token 超限 | 请求超出模型上下文窗口被拒 | 11/13 模型 128K 宣称在 32K 处性能腰斩（NoLiMa） |
| Lost-in-the-Middle | 上下文中间位置的信息利用率最差 | 关键信息放首尾 |
| 压缩摘要（Compaction） | 旧消息用 LLM 摘要替代（有损） | 触发阈值 85-90% |
| Prompt Caching | 相同前缀命中缓存省成本 | 缓存命中成本降 50-90% |
| 提示注入（Prompt Injection） | 恶意指令混入内容，操纵模型行为 | 维持 OWASP LLM01 首位 |
| 间接注入 | 恶意指令藏在网页/文档/工具输出中 | 比直接注入更危险 |
| Excessive Agency | Agent 权限/自主性过度导致的失控 | OWASP LLM06，2025 显著升位 |
| LLM-as-Judge | 用 LLM 当评估器 | 需 ≥20 条人工标注校准 |
| 外部遥测 | 独立于 Agent 自述的链路追踪 | Agent 自报日志不可信 |

## 5. 2026 版本窗口

> 本体系以 2026-08 为基准版本，涉及的关键参照物：
>
> - **OWASP Top 10 for LLM Applications 2025**（当前版本，LLM07 系统提示词泄露、LLM08 向量嵌入弱点为新增项）
> - **ORCA-bench**（2026，Agent 接警场景：最佳 RCA 准确率 Medium 40% / Hard 25.3%）
> - **AgentFixer**（ACM AGENT '26，解析类失败占生产故障 ~38%）
> - **Deep Agents SDK**（LangChain 2026-01，三层压缩：落盘 > 截断 > 摘要）
> - **CHARM**（2026，Agentic RAG 级联幻觉检测与治理）
> - **NoLiMa 基准**（长上下文性能衰减量化）
> - 约束解码工具：Outlines ~15k star / Guidance ~19k star / XGrammar（MLC）
> - 结构化输出：OpenAI Strict Mode / Claude `output_config` / Gemini `response_schema` / PydanticAI v1（2026-04）

## 【参考来源】

- OWASP Top 10 for LLM Applications 2025（safeguard.sh 解读 / claudesec 整理）
- Semantic Scholar: Cascading Hallucination in Agentic RAG: The CHARM Framework
- LangChain: Context Management for Deep Agents（2026-01）
- MachineLearningMastery: Context Window Management for Long-Running Agents
- AgentFixer: From Failure Detection to Fix Recommendations（ACM AGENT '26）
- MLflow: Monitoring Agentic AI in Production 2026 / Top LLM Observability Tools in 2026
- LangChain: Evaluating AI Agents at the Run, Trace, and Thread Level
- tensoria.fr: Why 15% of Your JSON Prompts Fail (And How to Fix It in 2026)
- JSONSchemaBench / Techsy 2026 Structured Output 工具横评

---

**下一模块**：[01-失败模式全景与诊断方法论](01-失败模式全景与诊断方法论.md)
