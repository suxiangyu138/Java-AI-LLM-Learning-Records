# PEFT 参数高效微调总览

> 微调执行层的方法模块——"参数高效"的家族全景：LoRA 原理与变体、QLoRA 深潜、其他 PEFT 方法对比、生产实践——2026 年微调的事实标准，也是面试最高频区

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 PEFT](#3-为什么必须学透-peft)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
PEFT 参数高效微调（微调执行层·方法模块）
│
├── 01 PEFT 全景与定位
│   ├── 三类方法（选择性/增量式/重参数化）
│   ├── 为什么 PEFT 是 2026 默认
│   └── LoRA 恢复全量 98% 收益实证
│
├── 02 LoRA 原理深潜
│   ├── 低秩分解 W = W₀ + BA
│   ├── intrinsic dimensionality 假说
│   ├── r 是容量上限不是平滑旋钮
│   └── ICLR 2026：梯度压缩器视角与 GID 对齐
│
├── 03 LoRA 变体家族
│   ├── DoRA（幅度方向分解）
│   ├── rsLoRA（α/√r 稳定缩放）
│   ├── AdaLoRA / PiSSA / LoRA+
│   └── 2026 重要提醒：超参敏感审计
│
├── 04 LoRA 配置工程
│   ├── r / alpha / 目标模块 / dropout
│   ├── alpha==r vs 2r 讨论
│   └── 训练与推理行为、合并
│
├── 05 其他 PEFT 方法
│   ├── Prefix / Prompt Tuning
│   ├── Adapter / IA3 / BitFit
│   └── FLAN-T5-XL 基准与选型
│
├── 06 QLoRA 深潜
│   ├── NF4 + 双量化 + paged optimizer
│   ├── 4bit 存储 vs bf16 计算
│   └── 单卡量级与稳定性
│
├── 07 PEFT 显存与性能
│   ├── LoRA 显存账本
│   ├── 训练速度（QLoRA 慢 30-40%）
│   └── 推理开销与混合分片
│
├── 08 PEFT 生产实践
│   ├── 多 LoRA 服务与合并
│   ├── 适配器生命周期管理
│   └── 热切换与版本治理
│
├── 09 PEFT 选型与边界
│   ├── PEFT vs 全量决策
│   ├── 方法间选型
│   └── PEFT 不够的信号与 RAG 组合
│
└── 10 PEFT 面试题
    ├── 必问 8 题 + 答题范式
    └── 原理与选型场景题
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | PEFT 全景与定位 | 三类方法、98% 实证 | 全部必须掌握 | [01-PEFT全景与定位.md](./01-PEFT全景与定位.md) |
| 02 | LoRA 原理深潜 | 低秩、intrinsic dim | 全部必须掌握 | [02-LoRA原理深潜.md](./02-LoRA原理深潜.md) |
| 03 | LoRA 变体家族 | DoRA/rsLoRA 等 | 中高级 | [03-LoRA变体家族.md](./03-LoRA变体家族.md) |
| 04 | LoRA 配置工程 | r/alpha/模块/合并 | 入门必备 | [04-LoRA配置工程.md](./04-LoRA配置工程.md) |
| 05 | 其他 PEFT 方法 | Prefix/Adapter 等 | 中高级 | [05-其他PEFT方法.md](./05-其他PEFT方法.md) |
| 06 | QLoRA 深潜 | NF4、双量化 | 全部必须掌握 | [06-QLoRA深潜.md](./06-QLoRA深潜.md) |
| 07 | PEFT 显存与性能 | 账本、速度、推理 | 入门必备 | [07-PEFT显存与性能.md](./07-PEFT显存与性能.md) |
| 08 | PEFT 生产实践 | 多 LoRA、生命周期 | 中高级 | [08-PEFT生产实践.md](./08-PEFT生产实践.md) |
| 09 | PEFT 选型与边界 | 选型决策、边界 | 全部必须掌握 | [09-PEFT选型与边界.md](./09-PEFT选型与边界.md) |
| 10 | PEFT 面试题 | 必问 8 题、答题范式 | 面试冲刺 | [10-PEFT面试题.md](./10-PEFT面试题.md) |

---

## 3. 为什么必须学透 PEFT

1. **PEFT 是 2026 年微调的事实标准**：Baseten 2026 实证——**LoRA 恢复全量微调收益的中位数 98%**，可训练参数少几个数量级；1k-50k 数据量级 PEFT 是默认选择（阈值判断见边界判断 06 篇）——不懂 PEFT 等于不懂 2026 微调。
2. **面试最高频区**："LoRA 为什么有效""r 和 alpha 怎么配""QLoRA 原理""DoRA 和 LoRA 区别"——本体系 02/03/06 篇给完整答案，且带 2026 最新研究（ICLR 2026 GID 对齐、rsLoRA 缩放理论、"Learning Rate Matters"审计）。
3. **原理是配置的地基**：r 是"容量上限不是平滑旋钮"（intrinsic dimensionality 假说）——理解了这一点，"r 越大越好吗"的答案自然浮现；alpha 与 r 的缩放关系（rsLoRA 的 α/√r）让"alpha 怎么配"从经验变成理论。
4. **生产形态与全量不同**：LoRA 可合并（无推理开销）也可多适配器共存（一个底座服务多个变体）——适配器生命周期管理（staging → production）是 2026 生产标准，本体系 08 篇全给。
5. **边界要清楚**：PEFT 不是万能的——复杂推理/领域迁移时弱于全量；知识注入时该用 RAG（PEFT 不更新知识截止）；<500-1000 条数据时 Prompt 更优——本体系 09 篇把边界划清。

---

## 4. 核心概念速查

### 4.1 核心概念

| 概念 | 一句话 | 关键数字 |
|------|--------|---------|
| PEFT | 冻结底座只训少量参数 | 0.1%-1% |
| LoRA | 低秩分解 W=W₀+BA | 恢复全量 98% 收益 |
| intrinsic dim | 任务更新只占低维子空间 | ~100-1000 维 |
| r | 秩 = 容量上限 | 8-64 |
| alpha | 缩放系数 | =r（2026 保守）或 2r |
| QLoRA | 4bit 量化 + LoRA | 7B 单卡 ~6GB |
| DoRA | 幅度/方向分解 | 接近全量（QLoRA+DoRA 默认） |
| rsLoRA | α/√r 稳定缩放 | 大秩不坍缩 |
| 多 LoRA | 一底座多变体 | 适配器 0.1-1% |
| 合并 | merge_and_unload | 无推理开销 |

### 4.2 方法与容量

| 方法 | 可训练参数 | 特点 |
|------|:---:|------|
| LoRA | 0.1-1% | 事实默认 |
| QLoRA | ~0.1% | 单卡训 7B-70B |
| Prefix/Prompt | <0.1% | 层级/输入级软提示 |
| Adapter | 1-5% | 模块化但 +10-20% 延迟 |
| BitFit | 极小 | 低资源场景 |
| 全量 | 100% | 上限最高、成本最高 |

### 4.3 常见误区速查

- "r 越大越好" → 错：r 是容量上限，超过任务 intrinsic dim 无收益
- "alpha 必须 2 倍 r" → 错：2026 共识 alpha==r（缩放 1.0），rsLoRA 理论是 α/√r
- "LoRA 一定不如全量" → 错：98% 收益恢复；低资源场景甚至反超
- "PEFT 能注入知识" → 错：知识缺口归 RAG，PEFT 不更新知识截止

---

## 5. 与周边知识的关系

```text
                    ┌── 基础概念/ —— 原理层（参数矩阵/损失/优化器）
大模型微调知识体系 ├── 边界判断/ —— 决策层（何时 PEFT、何时全量）
                    ├── 超参体系/ —— 配置层（lr/r 的实证调参）
                    ├── 常见问题/ —— 排障层（LoRA 相关坑）
                    ├── 本体系 —— 方法层（PEFT 家族全景）
                    └── 完整微调工程流程/ —— 流程层（训练/部署落地）
```

**本体系与同级子体系的分工**：`边界判断` 06 篇讲"PEFT 与全量的阈值判断"（决策），本体系讲"PEFT 家族的方法细节"（执行）；`超参体系` 讲"lr/batch 怎么调"（配置），本体系讲"r/alpha/模块怎么配"（方法参数）；`常见问题` 讲"PEFT 相关的坑怎么排"（排障）。读法建议：边界判断（要不要 PEFT）→ 本体系（PEFT 怎么选怎么配）→ 超参/流程（怎么训怎么部署）→ 常见问题（踩坑时查）。

---

## 6. 学习路线推荐

**路线一：面试速成（半天，对应模块 01-03 + 06 + 10）**
PEFT 定位 → LoRA 原理 → 变体家族 → QLoRA → 面试题；覆盖面试 PEFT 题 90% 考点（原理 + 变体 + 配置）。

**路线二：工程入门（1 天，对应模块 01-02 + 04 + 06-07）**
全景 → 原理 → 配置 → QLoRA → 显存性能；配合一次 QLoRA 微调实践，把配置与账本跑通。

**路线三：生产专家（对应模块 03 + 08-09）**
变体深潜 → 生产实践 → 选型边界；面向多变体服务与选型决策场景。

> 🎯 **核心要点**：PEFT 的学习终点 = "**懂定位**（三类方法、98% 实证、事实默认）、**懂原理**（低秩 + intrinsic dimensionality，r 是容量上限）、**懂变体**（DoRA/rsLoRA/AdaLoRA/PiSSA 各解决什么）、**懂配置**（r/alpha/模块）、**懂 QLoRA**（NF4 配方）、**懂生产**（合并 vs 多 LoRA、生命周期）、**懂边界**（何时 PEFT 不够）"——七件事覆盖 PEFT 从原理到生产的全貌；记住"LoRA 有效是因为任务更新只占低维子空间"是面试与配置的共同根基。

---

## 7. 快速自测 10 题

1. PEFT 的三类方法是什么？LoRA 属于哪类？
2. 为什么 LoRA 用很少参数能接近全量效果？（intrinsic dimensionality）
3. r 是"容量上限"是什么意思？r 超过任务 intrinsic dim 会怎样？
4. alpha 和 r 的关系？2026 共识与 rsLoRA 理论分别是什么？
5. DoRA 解决 LoRA 的什么问题？2026 默认组合？
6. QLoRA 的关键组件是什么？为什么 7B 能单卡训？
7. Prefix/Prompt Tuning、Adapter、BitFit 各适合什么场景？
8. LoRA 合并进底座的目的是什么？多 LoRA 服务的开销？
9. 什么时候 PEFT 不够用？（三个信号）
10. "Learning Rate Matters"（2026）对 LoRA 变体的结论是什么？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**参考来源**：

- [Why low-rank works: intrinsic dimensionality hypothesis（The Neural Base）](https://theneuralbase.com/lora-qlora/learn/intermediate/why-low-rank-works-intrinsic-dimensionality-hypothesis/)
- [Gradient Intrinsic Dimensionality Alignment（ICLR 2026）](https://en.papernotes.org/ICLR2026/model_compression/gradient_intrinsic_dimensionalityalignmentnarrowing_the_gap_between_low-rank_ad/)
- [Low-Rank Adapter Fine-Tuning（Emergent Mind）](https://www.emergentmind.com/topics/low-rank-adapter-fine-tuning)
- [Benchmarking PEFT Techniques for Large Language Models（播客）](https://podcast.do-not-panic.com/episodes/benchmarking-peft-techniques-for-large-language-models/)
- [What Is PEFT? A Guide to Parameter-Efficient Fine-Tuning（dev.to）](https://dev.to/bahadir_kusat_7df590dc9cd/what-is-peft-a-guide-to-parameter-efficient-fine-tuning-273b)

---

**下一模块**：[01-PEFT全景与定位.md](./01-PEFT全景与定位.md)
