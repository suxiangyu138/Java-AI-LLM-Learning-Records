# 00 - Grok 知识体系总览

> 🎯 Grok 是 SpaceXAI（原 xAI，Elon Musk 创立）推出的 AI 大模型产品线——2026 年 7 月最新 Grok 4.5 以 1.5T MoE 参数、4.2× Token 效率优势、SWE Marathon 全球第一的成绩杀回牌桌。深度集成 X 平台 + Cursor 联合训练 + DeepSearch + Aurora，以"用最少的 Token 干最多的活"为 2026 年的核心竞争力

> 🎯 本系列共 **11 篇高质量技术文档**，基于 **2026 年 7 月 28 日最新数据**，覆盖从 Grok-1 到 Grok 4.5 的完整进化史

---

## 📚 目录

1. [Grok 是什么](#1-grok-是什么)
2. [发展历程速览（2023-2026）](#2-发展历程速览2023-2026)
3. [知识全景](#3-知识全景)
4. [文件导航](#4-文件导航)
5. [学习路线推荐](#5-学习路线推荐)
6. [版本价格速查](#6-版本价格速查)
7. [核心术语速查](#7-核心术语速查)

---

## 1. Grok 是什么

**Grok** 是 SpaceXAI（2026 年 xAI 与 SpaceX 合并后的实体）推出的 AI 大模型及对话产品。2026 年 7 月 8 日发布的 **Grok 4.5** 是当前最新旗舰——1.5T MoE 参数、与 Cursor 联合训练、500K 上下文、Token 效率碾压同级模型。

**核心本质**：MoE 架构大模型 + X 平台实时数据 + Cursor 开发者数据 + DeepSearch + Aurora = **"最省 Token 的旗舰 AI"**。

**2026.07 最新定位**：

| 维度 | Grok 4.5 | 行业对标 |
|------|------|------|
| 代码 Agent | A-CODE-LLM 0.732 🥇 | 超越 Claude Sonnet 5 (0.707) |
| SWE Marathon | **29.0%** 🥇 | 超越 Opus 4.8 (26.0%) 和 Fable 5 (24.0%) |
| Token 效率 | **4.2×** 优于 Opus 4.8 | SWE Bench Pro 平均 15,954 tokens vs 67,020 |
| 定价 | $2/$6 per 1M | 60%+ 低于 Opus 4.8 |
| 生态集成 | X + Cursor + API | 社交 + IDE + 开发者三端覆盖 |

---

## 2. 发展历程速览（2023-2026）

| 时间 | 事件 | 意义 |
|:---:|------|------|
| 2023.07 | xAI 公司成立 | Elon Musk 宣布进军 AI |
| 2023.11 | Grok-1 发布 | 首个 314B MoE 模型，X Premium+ 独占 |
| 2024.03 | Grok-1.5 + Grok-1 开源 | 128K 上下文 + Apache 2.0 开源 |
| 2024.04 | Grok-1.5V | 首次支持视觉多模态 |
| 2024.08 | Grok-2 / Grok-2 mini | 推理能力超越 GPT-4 Turbo |
| 2024.12 | Aurora 图像生成 | 自研文生图模型集成 |
| 2025.02 | Grok-3 + DeepSearch | 10× 算力，DeepSearch + Thinking Mode |
| 2025.05 | Grok 独立 App | iOS/Android 独立应用 |
| 2025.07 | xAI API 全面开放 | 兼容 OpenAI SDK |
| 2026.06 | SpaceX 收购 xAI + Cursor | SpaceXAI 成立，Cursor 600B 收购 |
| **2026.07.08** | **Grok 4.5 发布** | **1.5T MoE + Cursor 联合训练 + Token 效率革命** |

---

## 3. 知识全景

```
Grok 精通体系（11个文件）— 2026.07 最新版
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-Grok概述与发展历程.md          # xAI→SpaceXAI/Grok-1→4.5完整进化
│   ├── 02-Grok底层架构与技术原理.md        # 1.5T MoE/GB300集群/Cursor联合训练
│   └── 03-Grok核心能力深度解析.md          # 代码Agent/DeepSearch/多模态/Token效率
│
├── 🔧 平台篇（04-05）
│   ├── 04-Grok平台与X生态深度集成.md       # X平台+Cursor+独立App三端
│   └── 05-Grok-API与xAI开发实战.md         # Grok 4.5 API/$2/$6定价/FC
│
├── 🚀 进阶篇（06-08）
│   ├── 06-Grok-DeepSearch深度搜索解密.md    # DeepSearch架构+联网检索
│   ├── 07-Grok图像生成与Aurora详解.md       # Aurora模型/文生图
│   └── 08-Grok订阅与付费生态.md             # 免费/Premium+/SuperGrok/API四档
│
├── 📋 实战篇（09-10）
│   ├── 09-Grok-Java后端集成实战.md          # Spring AI+Cursor+API集成
│   └── 10-Grok与主流模型深度对比.md          # vs Claude Opus5/GPT-5.6/DeepSeek V4 Pro
│
└── 📌 冲刺篇（11）
    ├── 11-Grok面试题与前沿展望.md            # 20题+SpaceXAI战略+Grok 5.0展望
    └── 00-Grok知识体系总览.md               # ← 本文件
```

---

## 4. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 2026.07 最新全景+路线+价格 | — |
| 01 | 概述与发展历程 | xAI→SpaceXAI, Grok-1→4.5 完整进化 | ⭐⭐⭐ |
| 02 | 底层架构与技术原理 | 1.5T MoE / GB300 / Cursor 联合训练 | ⭐⭐⭐⭐ |
| 03 | 核心能力深度解析 | 代码Agent/SWE Marathon/Token效率 | ⭐⭐⭐⭐⭐ |
| 04 | 平台与X生态集成 | X+Cursor+Grok Build 三端生态 | ⭐⭐⭐⭐ |
| 05 | API与xAI开发实战 | Grok 4.5 API/$2/$6/FC | ⭐⭐⭐⭐ |
| 06 | DeepSearch深度搜索 | 搜索架构+推理链 | ⭐⭐⭐⭐⭐ |
| 07 | 图像生成与Aurora | Aurora+/文生图实战 | ⭐⭐⭐ |
| 08 | 订阅与付费生态 | 四档方案对比 | ⭐⭐⭐ |
| 09 | Java后端集成实战 | Spring AI+Cursor+Agent | ⭐⭐⭐⭐ |
| 10 | 与主流模型对比 | Grok4.5 vs Opus5/Fable5/DeepSeek V4P | ⭐⭐⭐⭐⭐ |
| 11 | 面试题与前沿展望 | 20题+Grok 5.0展望 | ⭐⭐⭐⭐ |

---

## 5. 学习路线推荐

```text
🟢 入门（30min）：01-概述 → 04-平台生态 → 08-订阅
🔵 理解（1.5h）：02-架构 → 03-核心能力 → 06-DeepSearch
🟣 进阶（1.5h）：05-API → 09-Java集成 → 10-竞品对比
🟡 冲刺（30min）：11-面试题
```

---

## 6. 版本价格速查（2026.07）

### SpaceXAI API 定价

| 模型 | 输入/1M | 输出/1M | 上下文 | 定位 |
|------|:---:|:---:|:---:|------|
| **grok-4.5** | **$2** | **$6** | 500K | 旗舰，Token效率王者 |
| grok-4.5 (缓存) | $0.50 | $6 | 500K | 缓存75%折扣 |
| grok-3 | $3 | $15 | 1M | 前旗舰 |
| grok-3-mini | $0.30 | $4 | 1M | 快速推理 |

### 订阅方案

| 方案 | 月费 | 核心权益 |
|------|:---:|------|
| X 免费版 | $0 | 每 2h 10 次基础查询 |
| X Premium+ | $16 | Grok 4.5 完整 + X 蓝 V |
| SuperGrok | $30 | 无限查询 + Early Access + Cursor 权益 |
| Cursor Pro | $20 | 含 Grok 4.5 Agent 调用额度 |

---

## 7. 核心术语速查（2026.07）

| 术语 | 含义 |
|------|------|
| **Grok 4.5** | 2026.07.08 发布，1.5T MoE，与 Cursor 联合训练 |
| **SpaceXAI** | SpaceX 收购 xAI 后的合并实体 |
| **Cursor 联合训练** | 训练数据包含数万亿 Token 的真实开发者会话数据 |
| **Token 效率** | Grok 4.5 完成同样任务只需对手 1/4 的 Token |
| **SWE Marathon** | 长时间跨度的软件工程评测，Grok 4.5 29.0% 全球第一 |
| **DeepSearch** | 深度搜索，联网检索并生成结构化报告 |
| **Aurora** | 自研文生图模型，Grok 对话中直接生成 |
| **Grok Build** | SpaceXAI 的开发者平台 |
| **Colossus** | 超算集群，已升级至 GB300 GPU |

> 🎯 **核心要点**：Grok 4.5 的竞争力不在"最强"，而在"最省"——**相同质量的任务，只需对手 1/4 的 Token 和 1/3 的成本**。2026 年的 Grok = Token 效率的革命者 + Cursor 生态的受益者 + SWE Marathon 全球第一。
