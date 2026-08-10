# LLaVA 类核心架构总览

> 多模态体系的架构深潜——开源多模态模型的事实标准：LLaVA 家族演进（1.5 经典配方 → NeXT 动态分辨率 → OneVision 统一 → OneVision-2 感知智能）、三组件解剖、两阶段配方——面试必背的 VLM 架构

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 LLaVA 架构](#3-为什么必须学透-llava-架构)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
LLaVA 类核心架构（多模态体系·架构深潜，面试必背）
│
├── 01 LLaVA 家族演进史
│   ├── 1.0 → 1.5 → NeXT → OneVision → OneVision-2
│   ├── 演进主线（分辨率/投影/数据/底座）
│   └── 每个版本的里程碑
│
├── 02 LLaVA 1.5：经典配方
│   ├── 三组件（CLIP ViT-L/336 + MLP + Vicuna）
│   ├── 两阶段训练（558K 1e-3 → 665K 2e-5）
│   ├── 760K 数据构成
│   └── SOTA 影响（VQA/GQA/POPE）
│
├── 03 LLaVA-NeXT/1.6：动态分辨率
│   ├── AnyRes 分块（2304² 上限）
│   ├── Stage-1.5 知识学习
│   ├── 分辨率/数据/底座消融
│   └── LLM 规模是关键
│
├── 04 LLaVA-OneVision：统一多场景
│   ├── SigLIP + Qwen-2 换装
│   ├── Higher-AnyRes 融合
│   ├── 单图/多图/交错/视频统一
│   └── 三阶段课程 + token 预算对齐
│
├── 05 OneVision-1.5/2：开放与感知
│   ├── 1.5 民主化（4 天/$16K 全开源）
│   ├── 2 感知智能（OneVision-Encoder）
│   └── codec 对齐稀疏性原理
│
├── 06 架构解剖：组件细节
│   ├── 视觉编码器（CLIP vs SigLIP）
│   ├── MLP 投影演进
│   ├── 语言塔（Vicuna→Qwen）
│   └── 融合机制（LLM 自注意力）
│
├── 07 训练配方深潜
│   ├── 两阶段 lr（1e-3/2e-5）
│   ├── LLaVATrainer 特化
│   ├── 分离 lr（视觉 2e-6/语言 1e-5）
│   └── 冻结策略
│
├── 08 数据配方深潜
│   ├── 三类指令数据
│   ├── 665K 构成（158K 对话/50K GQA）
│   ├── 760K → 5.2M 规模演进
│   └── 数据质量的影响
│
├── 09 LLaVA 家族与其他模型对比
│   ├── vs Qwen-VL / InternVL / Pixtral
│   ├── 架构差异
│   └── 定位与选型
│
└── 10 LLaVA 家族面试题
    ├── 面试必背问答
    └── 架构/训练/对比题
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | 家族演进史 | 六版本里程碑 | 全部必须掌握 | [01-LLaVA家族演进史.md](./01-LLaVA家族演进史.md) |
| 02 | LLaVA 1.5：经典配方 | 三组件、两阶段 | 全部必须掌握 | [02-LLaVA1.5经典配方.md](./02-LLaVA1.5经典配方.md) |
| 03 | LLaVA-NeXT：动态分辨率 | AnyRes、Stage-1.5 | 中高级 | [03-LLaVA-NeXT动态分辨率.md](./03-LLaVA-NeXT动态分辨率.md) |
| 04 | OneVision：统一多场景 | SigLIP/Qwen-2/多场景 | 中高级 | [04-LLaVA-OneVision统一多场景.md](./04-LLaVA-OneVision统一多场景.md) |
| 05 | OneVision-1.5/2 | 开放、感知智能 | 中高级 | [05-OneVision-1.5与2.md](./05-OneVision-1.5与2.md) |
| 06 | 架构解剖：组件细节 | 编码器/投影/语言塔 | 全部必须掌握 | [06-架构解剖组件细节.md](./06-架构解剖组件细节.md) |
| 07 | 训练配方深潜 | lr/冻结/Trainer | 入门必备 | [07-训练配方深潜.md](./07-训练配方深潜.md) |
| 08 | 数据配方深潜 | 665K 构成、演进 | 入门必备 | [08-数据配方深潜.md](./08-数据配方深潜.md) |
| 09 | 与其他模型对比 | Qwen-VL/InternVL 等 | 中高级 | [09-与其他模型对比.md](./09-与其他模型对比.md) |
| 10 | LLaVA 家族面试题 | 必背问答 | 面试冲刺 | [10-LLaVA家族面试题.md](./10-LLaVA家族面试题.md) |

---

## 3. 为什么必须学透 LLaVA 架构

1. **LLaVA 是开源多模态的事实标准**：从 LLaVA 1.5（2023-10）到 OneVision-2（2026）——**"frozen vision encoder + MLP projector + LLM 微调"配方成为整个领域的默认起点**——绝大多数开源 VLM（含 Qwen-VL/InternVL 的架构基础）遵循 LLaVA 范式——**"学 LLaVA = 学 VLM 架构的通用框架"**。
2. **面试必背**："LLaVA 架构怎么设计""两阶段训练为什么""AnyRes 是什么""OneVision 和 1.5 的区别"——多模态面试的高频考点——本体系 02/03/04/10 篇给完整答案。
3. **演进史就是 VLM 的发展史**：分辨率（224 → 336 → 动态 2304²）、投影（线性 → MLP）、数据（158K → 5.2M）、底座（Vicuna → Qwen-3）、范围（单图 → 统一多场景）——**"LLaVA 的每一步演进对应 VLM 的一个设计维度"**（读演进史 = 读设计空间）。
4. **与多模态体系各模块的交汇**：架构（`多模态/基础概念/` 的 VLM 架构落地）、微调（`多模态/多模态微调（PEFT）/` 的冻塔策略/配方）、数据（`多模态/多模态数据集/` 的指令数据）、部署（`多模态/推理部署工程/` 的 vLLM 支持）——**"LLaVA 是把概念层原理落地的具体模型"**。
5. **OneVision-2 代表 2026 前沿**：codec 对齐稀疏性（OneVision-Encoder）——**"感知智能"的下一个方向**——本体系 05 篇给前沿概览（与 `多模态/基础概念/` 的原生统一呼应）。

---

## 4. 核心概念速查

### 4.1 核心概念

| 概念 | 一句话 | 关键数字 |
|------|--------|---------|
| 经典配方 | 冻编码器 + MLP + LLM 微调 | 领域默认起点 |
| 两阶段 | 对齐（只训投影）→ SFT | lr 1e-3 → 2e-5 |
| 760K | 1.5 的训练数据 | 558K 描述 + 158K 对话等 |
| AnyRes | 动态分辨率分块 | 2304² 上限 |
| Stage-1.5 | 高质量知识学习 | NeXT 引入 |
| SigLIP 换装 | OneVision 的编码器 | SO400M |
| Higher-AnyRes | 场景自适应裁剪融合 | 单图 729×(9+1) token |
| 分离 lr | 视觉 2e-6/语言 1e-5 | OneVision |
| 民主化 | 1.5 全开源 | 4 天/$16K |
| codec 稀疏 | OneVision-Encoder 原理 | 感知智能 |

### 4.2 家族版本速查

| 版本 | 年份 | 关键创新 |
|------|:---:|---------|
| LLaVA 1.0 | 2023 | 线性投影 + 指令微调 |
| LLaVA 1.5 | 2023-10 | MLP 投影、336²、760K |
| LLaVA-NeXT/1.6 | 2024 | AnyRes、Stage-1.5 |
| LLaVA-o1 | 2024-11 | 阶段级 beam search（+8.9%） |
| OneVision | 2024 | SigLIP + Qwen-2、多场景统一 |
| OneVision-1.5 | 2025 | 民主化（4 天/$16K） |
| OneVision-2 | 2026 | OneVision-Encoder 感知智能 |

### 4.3 常见误区速查

- "LLaVA 已经过时" → 错：配方仍是领域默认，OneVision-2 是 2026 前沿
- "LLaVA 就是 GPT-4V 的开源复制" → 错：独立设计的模块化范式，影响整个开源生态
- "投影层越复杂越好" → 错：1.5 的 MLP 两阶段是验证过的最优起点
- "分辨率越高越好" → 错：AnyRes 是"按内容动态分配"——token 与感知的平衡

---

## 5. 与周边知识的关系

```text
                    ┌── 基础概念/ —— 原理层（VLM 架构的通用框架）
多模态体系         ├── 多模态微调（PEFT）/ —— 训练层（LLaVA 配方的微调实践）
                    ├── 多模态数据集/ —— 数据层（LLaVA 指令数据的构建）
                    ├── 推理部署工程/ —— 服务层（vLLM 的 LLaVA 支持）
                    ├── 多模态常见问题与缺陷/ —— 排障层（LLaVA 系缺陷）
                    ├── 本体系 —— 架构深潜（LLaVA 家族全景）
                    └── 多模态 RAG/ —— 应用层（LLaVA 系的应用）
```

**本体系与周边知识的分工**：`基础概念` 讲 VLM 架构的通用框架（三组件/对齐/注入——`多模态/基础概念/05-视觉语言模型VLM架构.md` 的模块化范式）；本体系是**具体家族深潜**（LLaVA 的每个版本怎么实现——演进史/配方/数据）——**"基础概念给骨架、本体系给血肉"**；微调/数据/部署体系消费 LLaVA 的配方（冻塔/两阶段/AnyRes 部署）。读法建议：基础概念（骨架）→ 本体系（LLaVA 血肉）→ 应用体系（微调/数据/部署落地）。

---

## 6. 学习路线推荐

**路线一：面试速成（半天，对应模块 01-04 + 10）**
演进史 → 1.5 配方 → NeXT → OneVision → 面试题；掌握"经典配方 + 两阶段 + AnyRes + 演进线"四大考点。

**路线二：架构理解（1 天，对应模块 01-08）**
演进史 → 各版本 → 组件解剖 → 配方 → 数据；配合读 LLaVA-OneVision 技术文档实践。

**路线三：研究与前沿（对应模块 04-05 + 09）**
OneVision → 1.5/2 → 对比；面向研究跟踪与前沿理解。

> 🎯 **核心要点**：LLaVA 架构的学习终点 = "**懂演进**（1.0→2 的六版本主线——分辨率/投影/数据/底座四维演进）、**懂配方**（1.5 的经典三组件 + 两阶段——领域默认）、**懂增强**（NeXT 的 AnyRes/OneVision 的统一多场景）、**懂前沿**（OneVision-2 感知智能）、**懂对比**（与 Qwen-VL 等的定位差异）"——五件事覆盖 LLaVA 家族全貌；记住"**LLaVA 是 VLM 架构的 LIMA**"——它的配方定义了开源多模态的默认起点，学架构先学 LLaVA。

---

## 7. 快速自测 10 题

1. LLaVA 家族的主演进线（版本 → 关键创新）？
2. LLaVA 1.5 的经典配方（三组件 + 两阶段）？
3. 两阶段的 lr 分别是多少？为什么差 50 倍？
4. 760K 训练数据的构成？
5. AnyRes 是什么？解决什么问题？
6. Stage-1.5 是什么？
7. OneVision 换装了哪些组件？
8. 分离 lr（视觉 2e-6/语言 1e-5）说明什么？
9. OneVision-2 的 OneVision-Encoder 是什么？
10. LLaVA 家族和 Qwen-VL 的架构差异？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**参考来源**：

- [LLaVA 系列架构演进（灏天文库）](https://aiknowledge.cn/article/70329-72-llava-%E7%B3%BB%E5%88%97%E6%9E%B6%E6%9E%84%E6%BC%94%E8%BF%9B)
- [LLaVA-OneVision（LLaVA-NeXT 官方文档）](https://github.com/LLaVA-VL/LLaVA-NeXT/blob/e9835311/docs/LLaVA_OneVision.md)
- [LLaVA-OneVision-2: Fully Open Framework（GitHub）](https://github.com/EvolvingLMMs-Lab/LLaVA-OneVision-2)
- [LLaVA-1.5/NeXT: Unified Multimodal Fusion（Emergent Mind）](https://www.emergentmind.com/topics/llava-1-5-next)
- [Guide to Vision-Language Models (VLMs)（UVA 2026 课程）](https://www.cs.virginia.edu/~rmw7my/Courses/AgenticAISpring2026/Topic6VLM/vlm_guide.html)

---

**下一模块**：[01-LLaVA家族演进史.md](./01-LLaVA家族演进史.md)
