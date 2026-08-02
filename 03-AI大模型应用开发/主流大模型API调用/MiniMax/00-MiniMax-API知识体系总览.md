# 00 - MiniMax API 知识体系总览

> 🎯 MiniMax（稀宇科技）是全模态 AGI 公司 — 2026.06 发布 M3：**国内首个"前沿 Coding + 1M 上下文 + 原生多模态"三项兼备的旗舰**，428B 参数/23B 激活、SWE-Bench Pro 59%、全球唯一开源实现该水准。配套 Speech 语音 + Hailuo 视频全模态生态

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [2026 发展里程碑](#3-2026-发展里程碑)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)

---

## 1. 知识全景

```
MiniMax API 知识体系（7个文件 — 家族→架构→能力→文本API→全模态→集成→选型）
│
├── 🏗️ 家族（01）
│   └── 01-MiniMax模型家族与选型.md   # M3/M2.7/Speech/Hailuo 全景与定价
│
├── 🧠 架构（02）
│   └── 02-M3架构与核心技术.md        # 428B/23B激活/MSA稀疏注意力/原生多模态
│
├── 📊 能力（03）
│   └── 03-M3能力矩阵与基准测试.md    # SWE-Bench 59%/BrowseComp 83.5/Agent 实战
│
├── 🔌 文本API（04）
│   └── 04-M3-文本API开发实战.md      # Anthropic+OpenAI双协议/thinking模式/代码
│
├── 🎨 全模态（05）
│   └── 05-语音与视频全模态能力.md    # Speech-2.8/Hailuo 2.3/Computer Use
│
├── 🔗 集成（06）
│   └── 06-MiniMax在开发框架中的集成.md # LangChain4j/Spring AI/OpenClaw/编程工具
│
├── ⚖️ 选型（07）
│   └── 07-MiniMax选型与生态对比.md   # vs DeepSeek/Kimi/Claude/Token Plan
│
└── 📌 00-MiniMax-API知识体系总览.md   # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景导航 + 里程碑 + 学习路线 + 速查 | — |
| 01 | 模型家族与选型 | M3/M2.7/Speech/Hailuo 价格与定位 | ⭐⭐ |
| 02 | M3 架构与核心技术 | 428B/MSA 稀疏注意力/原生多模态 | ⭐⭐⭐ |
| 03 | 能力矩阵与基准 | SWE-Bench 59%/BrowseComp 83.5/Agent 实战 | ⭐⭐⭐ |
| 04 | 文本 API 开发实战 | 双协议/thinking 模式/代码 | ⭐⭐⭐ |
| 05 | 语音与视频全模态 | Speech-2.8/Hailuo 2.3/Computer Use | ⭐⭐⭐ |
| 06 | 框架集成 | LangChain4j/Spring AI/OpenClaw/编程工具 | ⭐⭐ |
| 07 | 选型与生态对比 | vs DeepSeek/Kimi/Token Plan | ⭐⭐ |

---

## 3. 2026 发展里程碑

| 时间 | 事件 |
|------|------|
| 2026.03.18 | M2.7 发布：230B/10B 激活、205K 上下文、agentic 定位 |
| 2026.06.01 | **M3 发布**：1M 上下文 + 原生多模态 + SWE-Bench 59% |
| 2026.06.12 | M3 权重开源（MiniMax Community License） |
| 2026 持续 | 开放平台 ARR 达 2 亿美元；被 OpenClaw 等 10+ 编程工具接入 |

---

## 4. 学习路线推荐

### 🟢 快速了解（1 小时）
```
01-模型家族 → 07-选型对比
产出：知道 MiniMax 家族定位与价格
```

### 🔵 开发者上手（半天）
```
04-文本API → 06-框架集成
产出：Anthropic/OpenAI 双协议调通 M3
```

### 🔴 深度理解（1 天）
```
02-架构 → 03-能力 → 05-全模态
产出：理解 MSA 架构 + 语音视频全模态生态
```

---

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| M3 | 旗舰：428B/23B 激活、**1M 上下文**、原生多模态、SWE-Bench 59% |
| MSA | MiniMax Sparse Attention — 两阶段稀疏注意力（比 DSA/MoBA 快 4 倍） |
| 原生多模态 | 预训练 Day 1 即文本+图像+视频交错训练（非后期拼接） |
| 双思考模式 | thinking（复杂推理）/ non-thinking（快速）共享定价 |
| 双协议 | 同时兼容 Anthropic 和 OpenAI API 格式 |
| Speech-2.8 | 语音模型：HD/Turbo 两档，40+ 语言 |
| Hailuo 2.3 | 视频生成：768P/1080P，攻克肢体畸变 |
| Token Plan | Plus 49 元/月（6 亿 token）— 同价位用量为 Claude 的 15 倍 |

---

> 🎯 **核心要点**：MiniMax M3 的差异化三句话 — **① 1M 上下文 + 原生多模态 + 前沿 Coding 三项兼备（国内唯一）② MSA 稀疏注意力（1M 下每 token 计算量仅上代 1/20）③ 双协议兼容（Anthropic + OpenAI 全生态接入）**。全模态生态：文本（M3）+ 语音（Speech）+ 视频（Hailuo）三线齐发。

**下一模块**：[01-MiniMax模型家族与选型](01-MiniMax模型家族与选型.md)
