# 01 LlamaIndex 概述：数据框架

> LlamaIndex 的核心主张："RAG 的瓶颈不是模型，而是数据管道"——检索是第一抽象，数据接入/解析/检索是它的主场；2026 年这一判断成为行业共识。

## 📚 目录

1. [LlamaIndex 是什么](#1-llamaindex-是什么)
2. [核心主张：数据管道是瓶颈](#2-核心主张数据管道是瓶颈)
3. [发展简史](#3-发展简史)
4. [与 LangChain / Haystack 对比](#4-与-langchain--haystack-对比)
5. [适用场景判断](#5-适用场景判断)
6. [面试高频问法](#6-面试高频问法)
7. [常见误区](#7-常见误区)
8. [经典应用场景](#8-经典应用场景)

## 1. LlamaIndex 是什么

| 维度 | 说明 |
|---|---|
| 出品方 | LlamaIndex Inc.（原 GPT Index / LlamaHub） |
| 定位 | 数据框架：为 LLM 应用连接/索引/检索数据 |
| 许可 | MIT |
| 规模 | 49k stars、月下载 2500 万（2026-04） |
| 版本 | v0.14.21（2026-04-21） |
| 生态 | LlamaHub（300+ 连接器）、LlamaCloud、LlamaParse |

一句话定义：

> LlamaIndex 把"外部数据"变成"LLM 可用的知识"——连接（300+ 源）→ 解析（LlamaParse）→ 索引（四种）→ 检索（QueryEngine）→ 编排（Workflows），检索是贯穿始终的第一抽象。

## 2. 核心主张：数据管道是瓶颈

### 2026 行业共识

```
RAG 系统的好坏，决定因素已经从模型转向数据管道：
解析质量（文档变文本丢不丢信息）
检索质量（能不能把对的东西找出来）
——模型大家都差不多，数据管道拉开差距
```

### LlamaIndex 的体现

| 数据环节 | LlamaIndex 资产 |
|---|---|
| 连接 | 300+ 数据连接器（LlamaHub） |
| 解析 | LlamaParse（v2 四级，复杂文档 18 倍成本优势） |
| 检索 | 层级分块/混合搜索/高级检索策略 |
| 智能体 | Retrieval Harness（文件系统原语） |

### 数据管道的代价（为什么重要）

```
智能体时代的放大效应：
基于错误信息行动的 Agent 不只给错误答案，还会执行错误操作
——数据层错误被 Agent 放大了
（引用阶段 4 的"垃圾进垃圾出"认知）
```

## 3. 发展简史

| 时间 | 事件 | 意义 |
|---|---|---|
| 2022 | GPT Index 发布 | 数据连接 LLM 的早期探索 |
| 2023 | 更名 LlamaIndex | RAG 数据框架定位确立 |
| 2023-2024 | 爆火（RAG 浪潮） | 与 LangChain 并称 RAG 双雄 |
| 2025-06 | **Workflows 1.0 GA** | 事件驱动编排能力成熟 |
| 2025 | LlamaAgents 推出 | 文档处理 Agent 一键部署 |
| 2025-12 | **LlamaParse v2** | 四级定价 + API v2 |
| 2026-02 | LlamaCloud 过渡更名 | 以 LlamaParse 为核心重塑 |
| 2026-04 | v0.14.21 | 当前稳定版 |

### 演进主线

```
数据连接（连接器）→ 数据解析（LlamaParse）→ 数据编排（Workflows/Agent）
数据层的深度越来越成为核心竞争资产
```

## 4. 与 LangChain / Haystack 对比

| 维度 | LlamaIndex | LangChain | Haystack |
|---|---|---|---|
| 第一抽象 | **检索/数据** | 编排/链 | 组件/管道 |
| 数据连接 | **300+ 连接器** | 多（生态大） | 中（30+ 存储） |
| 文档解析 | **LlamaParse（商业级）** | 基础 | 基础 |
| 索引 | **四种内置** | 外部依赖 | 存储抽象 |
| 编排 | Workflows（事件驱动） | LCEL/LangGraph | 管道 DAG |
| 生产导向 | 中 | 低 | **高** |
| 生态 | 大 | **最大** | 聚焦 |
| 代码量（基础 RAG） | **少 30-40%**（vs LangChain） | 基准 | 中 |

### 定位解读

```
LangChain：编排优先（什么都能连，抽象厚）
Haystack：管道优先（生产导向，强类型）
LlamaIndex：检索优先（数据接入/解析/检索最专）
```

## 5. 适用场景判断

### 适合 LlamaIndex

| 场景 | 为什么 |
|---|---|
| 复杂文档 RAG（PDF/表格/扫描件） | LlamaParse 核心资产 |
| 多数据源接入（300+ 连接器） | 连接器生态 |
| 知识库问答（向量/图谱索引） | 索引 + QueryEngine |
| 文档处理 Agent（发票/合同） | LlamaAgents 模板 |
| 需要内置评估 | 免独立评估平台 |

### 不适合 LlamaIndex

| 场景 | 为什么 |
|---|---|
| 复杂多步 Agent 编排 | LangGraph 更专（可混用） |
| 强类型生产管道 | Haystack 更生产化 |
| 需要最大通用生态 | LangChain |
| 非 RAG 场景 | 它是数据框架，非通用编排 |

### 判断口诀

```
数据复杂（解析/多源/检索）→ LlamaIndex
管道生产化 → Haystack
编排/Agent → LangGraph（检索交给 LlamaIndex）
```

## 6. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| LlamaIndex 是什么？ | 数据框架：连接/解析/索引/检索，检索第一抽象 |
| 与 LangChain 区别？ | 检索优先 vs 编排优先 |
| 为什么说数据是瓶颈？ | 2026 共识：模型趋同，解析/检索质量拉开差距 |
| 核心资产是什么？ | LlamaParse（解析）+ 300 连接器 + 四种索引 |
| Workflows 是什么？ | 事件驱动异步编排（1.0，2025-06） |
| 生产上怎么用？ | 检索层用 LlamaIndex，编排层用 LangGraph（混用） |

### 面试加分表达

> "LlamaIndex 的核心判断是'RAG 瓶颈不是模型而是数据管道'——2026 年这成了行业共识：解析质量决定信息保真，检索质量决定答案上限，而智能体时代数据错误还会被放大成错误行动。它的 LlamaParse 是解析层的竞争资产，我倾向用 LlamaIndex 管数据检索、LangGraph 管编排。"

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "只是 LangChain 的子集" | 数据层深度远超（解析/索引/商业资产） |
| "数据框架 = 不能编排" | Workflows 1.0 事件驱动编排成熟 |
| "LlamaParse 只是 OCR" | VLM 智能体路由 + 表格还原 + 视觉保留 |
| "必须用 LlamaCloud" | 开源全免费（LlamaCloud 是可选托管） |
| "学了 RAG 就够" | 框架把数据管道封装——但质量判断力来自原理 |
| "只能做 RAG" | 语义搜索/文档 Agent/结构化提取都支持 |

## 8. 经典应用场景

| 场景 | 用 LlamaIndex 的姿势 |
|---|---|
| 复杂文档知识库 | LlamaParse（v2 四级）+ 层级分块 + QueryEngine |
| 多源数据接入 | 300+ 连接器（数据库/网页/云盘） |
| 文档处理 Agent | LlamaAgents 模板（发票/合同/理赔） |
| 结构化提取 | LlamaExtract（schema + 页面级引用） |
| 多跳问答 | KnowledgeGraphIndex + Workflows |

### 场景落地骨架

```
复杂文档知识库（生产参考）：
① LlamaParse（按复杂度路由级别）→ 解析为 Markdown
② 层级分块（HierarchicalNodeParser）
③ VectorStoreIndex + 混合检索（RRF）
④ QueryEngine + 内置评估（忠实度）
⑤ Workflows 编排（多步/多文档）
```

> 🎯 核心要点：LlamaIndex = 检索第一抽象的数据框架——连接（300+）/解析（LlamaParse）/索引（四种）/检索（QueryEngine）/编排（Workflows）；核心主张"RAG 瓶颈是数据管道"已成为 2026 共识；vs LangChain（检索 vs 编排）、vs Haystack（数据 vs 管道）；适用"数据复杂场景"，生产常与 LangGraph 混用；六个误区校准认知。

---

**下一模块**：[02-核心概念-五大抽象](02-核心概念-五大抽象.md) / **返回总览**：[00-LlamaIndex知识体系总览](00-LlamaIndex知识体系总览.md)
