# 00 - LangChain 总览

> 强烈推荐：LangChain（LLM 应用开发框架）——做项目必用、简历加分——"不写 LangChain 也能做 AI 应用，但写了 LangChain 的应用更专业——2026 年 LLM 编排的事实标准"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)

---

## 1. 知识体系导图

```text
LangChain（本体系 11 篇——强烈推荐）
├── 定位层：01 LangChain 是什么（LLM 应用框架/2026 v1.0）
├── 核心层：02 核心抽象：Runnable（一切皆可组合）
│          03 模型封装（ChatModel——接入各家模型）
│          04 提示词框架（PromptTemplate）
│          05 输出解析（结构化输出）
├── 组合层：06 LCEL 表达式语言（管道组合）
│          07 记忆与对话（Memory）
├── 智能层：08 工具调用与 Agent（create_agent）
│          09 集成生态（RAG/向量库/与 vLLM 配合）
└── 验收层：10 阶段实战与自测（LangChain 项目 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | LangChain 是什么 | 定位/2026 v1.0/生态四件套 | 认知 |
| 02 | Runnable 核心抽象 | 统一接口/一切可组合 | 懂核心 |
| 03 | 模型封装 | ChatModel/bind_tools/结构化输出 | 会接模型 |
| 04 | 提示词框架 | PromptTemplate/ChatPromptTemplate | 会写提示词 |
| 05 | 输出解析 | StrOutputParser/Pydantic 输出 | 会解析 |
| 06 | LCEL | 管道组合/链式工作流 | 会组合 |
| 07 | 记忆与对话 | 短期注入/长期 Checkpointer | 会做对话 |
| 08 | 工具调用与 Agent | Tool/create_agent | 会做 Agent |
| 09 | 集成生态 | RAG/向量库/vLLM 配合 | 会集成 |
| 10 | 实战与自测 | 完整项目 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 Function Calling 体系（`../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/`）的分工**：那个体系讲"工具调用的协议层原理"（tools 参数/循环/安全），本体系讲"LangChain 框架怎么用"——**"原理课 vs 框架课——先懂协议（那个体系），再用框架（本体系）"**。

**与 vLLM 体系（`../了解即可（知道能干什么，不需要深挖源码）/vLLM/`）的分工**：vLLM 是"模型服务"（部署），LangChain 是"应用框架"（编排）——**"LangChain 应用 → 调 vLLM 的 OpenAI 兼容端点——两体系是'应用层与模型层'的配合"**（09 篇详讲）。

**与 LangChain4j（`../../../05-AI开发框架/` 或 Java 体系的 Spring AI）的分工**：LangChain（Python）与 LangChain4j（Java）同思想不同语言——**"本体系是 Python 版——Java 版在 05-AI开发框架 或 02-后端 的 LangChain4j 体系"**。

**2026-08 基线**：LangChain **1.0**（2025-10 发布——首个 1.x 稳定版；MIT；1000+ 集成；100K+ 星标）；**关键变化**：Agent 跑在 LangGraph 上、`create_agent` 新姿势、AgentExecutor 维护至 2026-12、旧 Chain 类移 langchain-classic——**"老教程（0.x 时代）大量过时——以 1.x 写法为准"**。

## 4. 学习路线推荐

**路线一：标准路线（5-7 天）**——01 → 10 逐篇 + 每篇动手——**毕业标准：做一个 LangChain 项目（模型接入 + LCEL 链 + Agent 或 RAG）**。

**路线二：速成路线（3-4 天）**——01 → 02 → 03 → 06 → 08 → 10（跳过 04/05/07/09 精读）——适合已有 LLM API 经验者。

**路线三：项目驱动路线**——先定项目（客服 Agent/RAG 问答）→ 按需查篇目——**"强烈推荐的体系 = 项目驱动的使用"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| Runnable | 一切组件的统一接口（invoke/stream/batch） | 02 |
| ChatModel | 模型封装（消息输入/消息输出） | 03 |
| PromptTemplate | 提示词模板（变量插值） | 04 |
| LCEL | 管道组合（`prompt \| model \| parser`） | 06 |
| 结构化输出 | with_structured_output（Pydantic 强制） | 05 |
| Memory | 对话记忆（短期注入/长期 Checkpointer） | 07 |
| create_agent | 1.x 的 Agent 新姿势（LangGraph 底层） | 08 |
| LangGraph | 编排引擎（Agent 的状态机） | 08 |
| LangSmith | 可观测性（追踪/调试/监控） | 09 |
| langchain-classic | 旧 Chain 类的兼容包（0.x 写法） | 01 |

## 6. 常见误区

**误区一：LangChain = Agent 框架**——"LangChain 就是做 Agent 的"——**正确：Agent 只是它的一部分（1.x 的智能层）——LCEL 链/RAG/记忆都是核心**（01 篇——"Agent 优先不等于只有 Agent"）。

**误区二：老教程照抄**——"网上 0.x 教程直接抄"——**正确：1.x 变化大（AgentExecutor 维护/旧 Chain 废弃/新 API）——以 1.x 写法为准**（01 篇——"老教程大量过时"）。

**误区三：Memory 模块照用**——"ConversationBufferMemory 直接用"——**正确：内置 Memory 已软弃用——短期 messages 注入、长期 LangGraph Checkpointer**（07 篇）。

**误区四：写提示词让模型输出 JSON**——"提示词里写'以 JSON 返回'"——**正确：with_structured_output（Pydantic 强制——可靠得多）**（05 篇）。

**误区五：什么都用 LangChain**——"所有调用都包一层"——**正确：简单调用直接用 SDK；复杂编排（链/Agent/RAG）才用 LangChain**（01 篇——"框架是编排工具，不是必需品"）。

## 7. 一周学习计划示例

| 天 | 学习内容 | 动手任务 | 验收 |
|:---:|---------|---------|------|
| 第 1 天 | 01 是什么 + 02 Runnable | 装 LangChain + 第一个 invoke | 能调用 |
| 第 2 天 | 03 模型 + 04 提示词 | 接入模型 + 模板化提示词 | 会接入 |
| 第 3 天 | 05 输出解析 + 06 LCEL | 结构化输出 + 管道链 | 会组合 |
| 第 4 天 | 07 记忆 + 08 Agent | 多轮对话 + 工具调用 | 会对话 |
| 第 5 天 | 09 集成 + 10 实战 | 做一个小项目（Agent/RAG） | 项目完成 |

**计划纪律**：每天 2-3 小时；**每篇动手必做**（LangChain 是"写出来"的知识）；卡住先查 10 篇报错速查——"产出物齐 = 毕业"。

**学习环境准备**（动手前先备齐）："**① Python**——3.10+（LangGraph v1 要求）；**② 环境**——venv 隔离 + `pip install langchain langchain-openai`（按集成补装）；**③ 模型**——OpenAI key 或本地 vLLM（base_url——09 篇）——**'环境三件套（venv/包/模型接入）备齐，第一天就顺'"**（"没 key 的替代：本地 vLLM 或 Ollama（OpenAI 兼容——03 篇姿势）——**'模型接入是 LangChain 的第一前提'"**）。

**学习重点提醒（2026 官方建议）**："LCEL + RAG + LangGraph 三件事是 LangChain 学习的重点——**'本体系覆盖 LCEL（06 篇）与 RAG（09 篇）——LangGraph 深入是进阶'"**（"学习顺序：先本体系（组装基础）→ 再 LangGraph（编排——`主流 Agent 范式`/Multi-Agent 体系）——**'基础先行，进阶随后'"**）。

**本体系与全库的配合**："**LangChain 应用层 + vLLM 模型层 + bitsandbytes/trl 训练层**——'AI 应用全链路的三个层次（13-Python框架 的横向配合）'——**'会 LangChain（应用）+ 会 vLLM（部署）= AI 应用工程师的完整技能'**"（"学习组合建议：本体系 + vLLM 体系（了解即可）一起学——**'应用与部署配套（09 篇的深入配合）'"**）。

**强烈推荐 vs 了解即可的定位对照**："**了解即可（bitsandbytes/trl/uvicorn/vLLM）**——知道能干什么、怎么用、何时用（字典式）；**强烈推荐（本体系 LangChain）**——会写代码、会做项目（动手层）——**'深度阶梯：了解即可（认知）→ 强烈推荐（动手）——LangChain 是'强烈推荐'的第一套'"**（"定位的落地：本体系每篇都带完整代码（不是概念）——**'强烈推荐 = 代码会写、项目会做'"**）。

## 8. 快速自测 10 题

1. LangChain 1.0 的关键变化？（01）
2. Runnable 是什么？（02）
3. ChatModel 的输入输出？（03）
4. 结构化输出的正确姿势？（05）
5. LCEL 是什么？（06）
6. 1.x 记忆的正确姿势？（07）
7. create_agent 是什么？（08）
8. AgentExecutor 的 2026 状态？（01/08）
9. LangGraph 是什么？（08）
10. 本体系毕业标准？（10）

> 🎯 **核心要点**：LangChain = LLM 应用框架（**编排事实标准——1000+ 集成**）；1.x 关键变化（**Agent 跑 LangGraph/create_agent 新姿势/旧 Chain 移 classic**）；核心四件（**Runnable 统一接口/LCEL 组合/结构化输出/Agent 工具**）；**"强烈推荐"深度：会写代码、会做项目——比"了解即可"深（动手层），比源码深潜浅（使用层）**——"不写 LangChain 也能做 AI 应用，但写了更专业"。

---

**参考来源**：

- [LangChain 官方文档](https://docs.langchain.com/)
- [LangChain 1.0 正式发布（2025-10）](https://developer.baidu.com/article/detail.html?id=6982600)
- [LangChain 全解：从基础组件到生产级 Agent 开发（2026）](https://healthjian.github.io/pages/blog/moban_new_md.html?md=../../context/20260423_zh_1.md)
- [vLLM（姊妹体系——模型服务）](../../了解即可（知道能干什么，不需要深挖源码）/vLLM/00-vLLM总览.md)

---

**下一模块**：[01-LangChain是什么.md](./01-LangChain是什么.md)
