# DeepSeek V4 & V4-Pro 深度解析

> 🚀 484天的进化：1.6T MoE、百万上下文、混合注意力(CSA+HCA)、mHC连接、Muon优化器、OPD蒸馏 —— DeepSeek V4 的范式级架构革新与极致工程

---

## 📚 目录

1. [V4 发布概述](#1-v4-发布概述)
2. [模型规格：Pro vs Flash](#2-模型规格pro-vs-flash)
3. [混合注意力：CSA + HCA](#3-混合注意力csa--hca)
4. [mHC 流形约束超连接](#4-mhc-流形约束超连接)
5. [Muon 优化器](#5-muon-优化器)
6. [后训练范式革新：OPD + GRM](#6-后训练范式革新opd--grm)
7. [三级推理模式](#7-三级推理模式)
8. [DSpark 推理加速](#8-dspark-推理加速)
9. [性能基准](#9-性能基准)
10. [与 V3 的核心差异](#10-与-v3-的核心差异)

---

## 1. V4 发布概述

### 1.1 发布时间线

```text
2024.12.26  DeepSeek-V3 发布（671B MoE）
2025.03     DeepSeek-V3-0324 重大更新
2025.12     DeepSeek-V3.2 过渡版本
2026.04.24  DeepSeek-V4 正式发布  ← 历时 484 天
2026.06.27  DSpark 推理加速方案发布
```

> 🎯 V4 是 DeepSeek **从追赶者到引领者** 转折的标志性版本。技术报告近 60 页，MIT 协议开源。

### 1.2 V4 系列全景

```text
DeepSeek-V4 家族：

  V4-Flash（轻量旗舰）
    └── 284B 总参 / 13B 激活
    └── 43 层 Transformer
    └── FP8 训练

  V4-Pro（全量旗舰）
    └── 1.6T 总参 / 49B 激活
    └── 61 层 Transformer
    └── FP4 + FP8 混合训练

  V4-Pro-Max（最强推理模式）
    └── 同级模型参数 + Think Max 推理
    └── 最强推理链模式

  推理模式：
    ├── Non-think：快速直接响应
    ├── Think High：逻辑分析
    └── Think Max：完整长思维链
```

---

## 2. 模型规格：Pro vs Flash

### 2.1 核心参数对比

| 维度 | V4-Pro | V4-Flash | V3 (对比) |
|------|:------:|:--------:|:--------:|
| **总参数** | 1.6T（1.6万亿） | 284B（2840亿） | 671B |
| **激活参数** | 49B | 13B | 37B |
| **激活比** | ~3.1% | ~4.6% | ~5.5% |
| **层数** | 61 | 43 | 61 |
| **隐藏维度** | 7168 | 4096 | 7168 |
| **上下文窗口** | 1M | 1M | 128K |
| **最大输出** | 384K tokens | 384K tokens | 8K |
| **训练精度** | FP4 + FP8 | FP8 | FP8 |
| **预训练数据** | 33T tokens | 32T tokens | 14.8T |
| **架构** | MoE + Hybrid Attention | MoE + Hybrid Attention | MoE + MLA |

### 2.2 训练策略

```text
V4 训练分阶段渐进式扩展上下文：

  阶段 1：4K 上下文  →  标准预训练
  阶段 2：16K 上下文  →  中等长度扩展
  阶段 3：64K 上下文  →  引入 Sparse Attention
  阶段 4：1M 上下文   →  全量长上下文训练

这种渐进策略确保：
  ✅ 短上下文性能不受损
  ✅ 长上下文能力稳定增长
  ✅ Sparse Attention 在 64K+ 场景下生效
```

---

## 3. 混合注意力：CSA + HCA

### 3.1 为什么放弃 MLA

```text
MLA 的局限性（在百万上下文场景下）：

  V3 的 MLA：
    → 128K 上下文下 KV Cache 压缩 ~60x（优秀）
    → 但 1M 上下文下仍有瓶颈

  V4 的思考：
    → 百万 token 意味着需要处理 ~100万字级别的信息
    → 纯 MLA 压缩依然不够 → 需要更激进的信息筛选
    → 不同任务需要不同类型的注意力 → 混合方案
```

### 3.2 CSA：压缩稀疏注意力

```text
CSA = Compressed Sparse Attention（压缩稀疏注意力）

工作原理：
  1. 分块压缩：每 4 个 token 压缩为 1 个块
     → 1M token → 250K 压缩块

  2. 闪电索引器（Lightning Indexer）：
     → 快速估算每个块与当前 Query 的相关性分数
     → O(n) 复杂度（而非标准注意力 O(n²)）

  3. Top-K 稀疏选择：
     → 从 250K 块中仅选择 1024 个最相关的
     → 只对这 1024 个块做精确注意力计算

  适用场景：
    ✅ 精准检索（如从长文档中找到某个具体信息）
    ✅ 代码仓库级理解
    ✅ 长对话中的关键信息提取
```

```python
# CSA 伪代码
class CompressedSparseAttention:
    def forward(self, Q, K, V, block_size=4, top_k=1024):
        # 1. 分块压缩
        K_compressed = reshape(K, [num_blocks, block_size, d]).mean(dim=1)
        V_compressed = reshape(V, [num_blocks, block_size, d]).mean(dim=1)

        # 2. 闪电索引器：快速计算块相关性
        scores = self.lightning_indexer(Q, K_compressed)  # O(seq/4 × d)

        # 3. Top-K 选择
        topk_indices = topk(scores, k=top_k)

        # 4. 精确注意力（仅 1024 个块）
        K_selected = K_compressed[topk_indices]
        V_selected = V_compressed[topk_indices]
        return scaled_dot_product_attention(Q, K_selected, V_selected)
```

### 3.3 HCA：重度压缩注意力

```text
HCA = Heavily Compressed Attention（重度压缩注意力）

工作原理：
  1. 极端压缩：128:1 压缩比
     → 1M token → ~7.8K 超压缩表示

  2. 稠密注意力：对所有 7.8K 个压缩表征做全注意力
     → 虽然是全注意，但压缩后尺度很小

  3. 全局信号捕获：
     → 捕捉文档的整体语境、主题、情感倾向
     → 适合"这篇文档大概讲了什么"

  适用场景：
    ✅ 全局理解（文档摘要、主题识别）
    ✅ 上下文一致性保持
    ✅ 长程依赖的粗粒度感知
```

### 3.4 混合堆叠策略

```text
V4-Pro 61 层的混合堆叠方案：

  层 1-3:   标准全注意力（保留局部细节）
  层 4-61:  CSA 层 与 HCA 层 交替堆叠
             ├── CSA 层：精准检索 + 稀疏关注
             └── HCA 层：全局感知 + 语境理解

  额外机制：
    → 滑动窗口注意力：始终保留最近 128 个未压缩 token
      → 保证近距离依赖不被压缩破坏
    → CSA/HCA 交替频率：约 3:1（CSA 为主，HCA 为辅）

效率数据（百万 token 场景）：
  ┌────────────────────────────────────────────┐
  │ 指标           │ V3.2 (MLA)  │ V4 (Hybrid) │
  ├────────────────┼────────────┼─────────────┤
  │ 推理 FLOPs     │   100%     │    27%      │
  │ KV Cache 占用  │   100%     │    10%      │
  │ 长上下文精度   │   基准     │   持平或更好 │
  └────────────────┴────────────┴─────────────┘
```

---

## 4. mHC 流形约束超连接

### 4.1 传统残差连接的问题

```text
标准残差连接：x_{l+1} = x_l + F(x_l)

深层网络中的问题：
  → 残差信号累积放大 → 梯度爆炸风险
  → 信号在层层传递中逐渐衰减
  → 深层网络训练不稳定

V4 有 61 层 + 超长序列 → 问题尤其严重
```

### 4.2 mHC 的解决方案

```text
mHC = Manifold-Constrained Hyper-Connections

核心创新：
  1. 对残差映射矩阵施加 Birkhoff Polytope（双随机矩阵）约束
     → 谱范数 ≤ 1，从数学上保证不爆炸

  2. 输入/输出通过 Sigmoid 确保非负
     → 避免正负信号相互抵消

  3. 可学习的 gating 机制
     → 每层自适应调整残差强度

数学表达：
  x_{l+1} = σ(W_out · h_l) ⊙ x_l + σ(W_in · x_l) ⊙ F(x_l)

  其中 h_l 是可学习的历史状态，W_out 受双随机约束

实际成本：仅增加 ~6.7% 的流水线耗时，几乎"免费"
```

### 4.3 mHC 的实际效果

```text
✅ 梯度流动更稳定 → 训练 Loss 曲线更平滑
✅ 60+ 层网络训练无退化 → 更深网络成为可能
✅ 收敛速度提升 → 相同训练步数达到更好效果
✅ 长序列训练更稳定 → 百万上下文训练的关键基石
```

---

## 5. Muon 优化器

### 5.1 AdamW 的局限与 Muon 的突破

```text
传统 AdamW：
  → 逐元素（element-wise）更新
  → 对每个参数独立维护一阶/二阶动量
  → 无法利用参数矩阵的结构信息

Muon 优化器：
  → 矩阵级别（matrix-wise）更新
  → 对权重矩阵整体做 Newton-Schulz 正交化迭代
  → 显式利用权重矩阵的几何结构

DeepSeek V4 的实践：
  ├── 大部分参数：Muon 优化器
  ├── 部分敏感参数：AdamW（如 Embedding, LM Head）
  └── 学习率配比：Muon / AdamW = 0.18
```

### 5.2 Muon 的 Newton-Schulz 迭代

```python
# Muon 优化器核心逻辑（简化）
def muon_update(weight, grad, lr, momentum=0.95, nesterov=True, ns_steps=10):
    # 1. 动量更新
    momentum_buffer = momentum * momentum_buffer + grad
    if nesterov:
        update = momentum * momentum_buffer + grad
    else:
        update = momentum_buffer

    # 2. Newton-Schulz 正交化（10 步迭代）
    # 将更新矩阵投影到正交矩阵空间
    W = update
    for _ in range(ns_steps):
        a = W @ W.T
        b = 1.5 * W - 0.5 * a @ W  # cubic convergence
        c = 1.5 * b - 0.5 * b @ b.T @ b
        W = c

    # 3. 应用正交化后的更新
    weight -= lr * W

    return weight
```

### 5.3 Muon vs AdamW

| 维度 | AdamW | Muon |
|------|-------|------|
| **更新粒度** | 逐元素 | 逐矩阵 |
| **几何感知** | 无 | 利用矩阵正交结构 |
| **收敛速度** | 基准 | 更快 |
| **额外开销** | 无 | Newton-Schulz 迭代（可控） |
| **内存占用** | ~2x 参数（动量+方差） | ~1x 参数（仅动量） |
| **适用性** | 通用 | 矩阵参数（2D及以上） |

> 💡 Muon 此前被 Kimi K2 首次在大规模验证，DeepSeek V4 是该技术的第二次大规模成功应用。

---

## 6. 后训练范式革新：OPD + GRM

### 6.1 On-Policy Distillation (OPD)

```text
传统混合 RL 的问题：
  将所有能力混在一个模型里训练 → "对齐税"
  → 提升推理能力可能损害创意写作
  → 不同能力相互干扰

V4 的 OPD 方案：

  Step 1: 训练领域专家（Domain Specialists）
    ├── 数学专家（GRPO 在数学数据上训练）
    ├── 代码专家
    ├── Agent 专家
    ├── 指令遵循专家
    └── ... 十几个领域专家并行训练

  Step 2: On-Policy 蒸馏
    ├── 所有专家各自在自己的领域内采样
    ├── 全词表 Logit 蒸馏（Full-vocabulary Logit Distillation）
    │   → 不仅蒸馏最终结果，还蒸馏中间推理分布
    └── 蒸馏到一个统一学生模型中

  Step 3: 迭代优化
    └── 学生模型 → 新一轮专家训练 → 再次蒸馏 → ...

  优势：
    ✅ 避免"对齐税"：各能力独立优化
    ✅ 无损蒸馏：全词表 Logit 保持信息完整性
    ✅ 快速迭代：专家可独立更新
```

### 6.2 GRM：生成式奖励模型

```text
传统奖励模型（RM）：
  输入：(prompt, response) → 标量分数
  问题：无法解释为什么好/坏，缺乏细粒度反馈

GRM = Generative Reward Model（生成式奖励模型）：

  输入：(prompt, response)
    ↓
  Step 1: 评分量表引导
    "请从以下维度评分：
     1. 准确性 (1-10): 
     2. 完整性 (1-10):
     3. 语言质量 (1-10):"
    ↓
  Step 2: 思考轨迹生成
    "让我分析这个回答... 
     准确性方面，它正确地...
     但在完整性上，遗漏了..."
    ↓
  Step 3: 最终评分
    基于分析给出结构化评分

  核心优势：
    ✅ 可解释：知道为什么得分高/低
    ✅ 细粒度：多维度反馈指导优化
    ✅ 联合优化：GRM 与生成模型共享参数
    ✅ 因果推演：评分过程本身就是推理链
```

### 6.3 Interleaved Thinking（交错式思考）

```text
问题场景：长时间运行的 Agent 任务

  多轮 Agent 交互：
    Turn 1: [Think]...[Act] call tool
    Turn 2: tool returned → [Think]...[Act] call tool
    Turn 3: tool returned → [Think]...[Act] done

  传统做法：每轮丢弃上一轮的 reasoning traces
    → 模型"失忆"，缺乏全局规划

V4 的 Interleaved Thinking：
  → 普通对话：丢弃 reasoning traces（保持简洁）
  → 工具调用场景：跨用户消息边界保留完整 reasoning history
  → 让模型在长上下文 Agent 工作流中保持连续行动能力

  效果：Agent 任务的连贯性和成功率显著提升
```

---

## 7. 三级推理模式

### 7.1 模式对比

```text
V4 支持三种推理深度：

  Non-think（无推理）
    ├── 直接响应，无显式思考过程
    ├── 适用：日常对话、简单查询、翻译
    ├── 速度：最快（~50 t/s）
    └── 成本：最低

  Think High（逻辑分析）
    ├── 中长度思维链
    ├── 适用：逻辑推理、代码 review、方案设计
    ├── 速度：适中（~25 t/s）
    └── 成本：中等

  Think Max（V4-Pro-Max，最强推理）
    ├── 完整长思维链，深度反思与验证
    ├── 适用：数学竞赛、算法难题、复杂 Agent 任务
    ├── 速度：慢（~10 t/s）
    └── 成本：最高
```

### 7.2 使用建议

| 场景 | 推荐模式 | 示例 |
|------|:------:|------|
| 闲聊对话 | Non-think | "今天天气怎么样？" |
| 简单的代码片段 | Non-think | "写一个 Python 冒泡排序" |
| 技术方案设计 | Think High | "设计一个微服务架构方案" |
| Bug 调试 | Think High | "这段代码为什么报 NullPointer？" |
| 数学证明 | Think Max | "证明费马小定理" |
| 竞赛编程 | Think Max | "Codeforces 2800 分级别题目" |
| 复杂 Agent 任务 | Think Max | "分析整个项目并重构模块" |

---

## 8. DSpark 推理加速

### 8.1 什么是 DSpark

```text
发布时间：2026年6月27日
论文+全栈代码库：DeepSpec

核心思路：推测性解码（Speculative Decoding）
  → 用轻量级草稿模型提前生成候选 token
  → 主模型并行验证
  → 接受正确的，拒绝错误的并重生成
```

### 8.2 DSpark 的两大创新

```text
创新 1：半自回归生成架构
  保留并行草稿模型的高吞吐优势
  + 轻量级串行模块（Markov 头 / RNN 头）
  → 逐 token 注入前缀依赖信息
  → 解决并行草稿在序列后半段 token 接受率衰减问题

创新 2：置信度调度验证
  引入置信度头评估每个 token 的存活概率
  + 硬件感知前缀调度器
  → 根据实时引擎吞吐量动态决定最优验证长度
  → 只对预期回报最高的 token 分配算力

实测效果：
  ├── V4-Flash：生成速度提升 60%-85%
  ├── V4-Pro：  生成速度提升 57%-78%
  └── 已全面部署线上真实流量，替代 MTP-1
```

### 8.3 MTP → DSpark 的演进

```text
V3 时代：MTP（Multi-Token Prediction）
  → 训练时预测多个未来 token
  → 推理时用于推测解码
  → 效果有限，token 接受率衰减严重

V4 初期：MTP-1
  → V3 MTP 的延续

V4 当前：DSpark
  → 全新的推测解码范式
  → 半自回归 + 置信度调度
  → 兼容 Qwen3、Gemma4 等主流模型
```

---

## 9. 性能基准

### 9.1 V4-Pro-Max 核心基准

| 基准 | V4-Pro-Max | GPT-5.4 | Gemini 3.1 Pro | Claude Opus 4.6 |
|------|:--------:|:------:|:------------:|:-------------:|
| **LiveCodeBench** | **93.5** | - | - | - |
| **Codeforces Rating** | **3206** | 3168 | 3052 | - |
| **IMOAnswerBench** | **89.8** | - | - | - |
| **GPQA Diamond** | 90.1 | - | - | - |
| **SWE-bench Verified** | **80.6** | - | - | - |
| **MMLU** | 90.1 | - | - | - |
| **SimpleQA-Verified** | 57.9 | - | - | - |
| **HLE** | 37.7 | - | **44.4** | - |

> 🎯 **代码和 SWE-bench 领域**：V4-Pro-Max 达到开源最强，显著超过 GPT-5.4；**Codeforces Rating 3206** 在人类选手中排名第 23。

### 9.2 V4-Flash 的惊人表现

```text
V4-Flash-Max（仅 13B 激活参数）：
  → 推理任务打平 GPT-5.2 和 Gemini 3.0-Pro
  → 内部 R&D 代码 benchmark 通过率 67%
    （接近 Claude Opus 4.5 的 70%）
  → 仅 13B 激活 = 消费级硬件可运行！
```

### 9.3 自评与展望

```text
DeepSeek 自评（摘自技术报告）：
  → 整体能力落后最前沿闭源模型约 3-6 个月
  → 代码/Agent 领域已基本追平甚至领先
  → 通用知识/多模态仍有一定差距
  → V4 是一个过渡架构，V5 正在研发中
```

### 9.4 API 定价

| 项 | V4-Pro | V3 (对比) |
|----|:-----:|:--------:|
| **输入（cache miss）** | $1.74 / M tokens | ¥1 / M (~$0.14) |
| **输入（cached）** | $0.145 / M tokens | - |
| **输出** | $3.48 / M tokens | ¥2 / M (~$0.28) |

> ⚠️ V4 价格相比 V3 有明显上涨，但对比 GPT-5.4 / Claude Opus 4.6 仍具性价比。

---

## 10. 与 V3 的核心差异

### 10.1 架构演进全景

| 维度 | DeepSeek-V3 (2024.12) | DeepSeek-V4 (2026.04) |
|------|----------------------|----------------------|
| **发布间隔** | - | 484 天 |
| **总参数** | 671B | 1.6T（~2.4x） |
| **激活参数** | 37B | 49B（~1.3x） |
| **激活比** | 5.5% | 3.1%（更稀疏） |
| **上下文** | 128K | 1M（~8x） |
| **注意力** | MLA（KV Cache 压缩） | CSA+HCA（混合注意力） |
| **优化器** | AdamW | Muon + AdamW 混合 |
| **残差连接** | 标准残差 | mHC（流形约束） |
| **训练精度** | FP8 | FP4 + FP8 混合 |
| **训练数据** | 14.8T tokens | 33T tokens（~2.2x） |
| **后训练** | SFT + RLHF | OPD + GRM + Interleaved Thinking |
| **推理加速** | MTP | DSpark（57-85% 加速） |
| **推理模式** | 单一 | 三级（Non-think/Think High/Think Max） |
| **多模态** | 无（纯文本） | 无（纯文本，仍待多模态版） |
| **国产芯片** | H800 训练 | 验证昇腾 NPU EP 方案 |

### 10.2 十大关键升级总结

```text
┌─────────────────────────────────────────────────────────┐
│           V3 → V4 十大升级                                │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1️⃣   参数扩容：671B → 1.6T（2.4x 总参数）              │
│  2️⃣   上下文扩展：128K → 1M（8x 上下文）                 │
│  3️⃣   注意力升级：MLA → CSA+HCA 混合注意力               │
│  4️⃣   残差创新：标准残差 → mHC 流形约束                  │
│  5️⃣   优化器升级：AdamW → Muon                          │
│  6️⃣   精度突破：FP8 → FP4+FP8                           │
│  7️⃣   后训练革新：RLHF → OPD+GRM                        │
│  8️⃣   推理加速：MTP → DSpark（60-85% 提速）             │
│  9️⃣   推理模式：单一 → 三级可选                          │
│  🔟   国产适配：CUDA only → 昇腾验证                     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

> 🎯 **一句话总结**：DeepSeek V4 不是简单的参数堆砌，而是从 **注意力机制（CSA+HCA替代MLA）、残差连接（mHC）、优化器（Muon）、后训练范式（OPD+GRM）** 四个维度同时进行的范式级架构创新。在代码和 Agent 领域已全面进入全球第一梯队，Codeforces 3206 的分数意味着它已经超越了绝大多数人类程序员。

---

**相关模块**：
- [02-DeepSeek核心架构-MoE与MLA](./02-DeepSeek核心架构-MoE与MLA.md) — V3 时代的 MoE+MLA，理解 V4 的起点
- [03-DeepSeek-V3训练与优化策略](./03-DeepSeek-V3训练与优化策略.md) — V3 训练方案，与 V4 对比的基础
- [04-DeepSeek-R1推理模型深度解析](./04-DeepSeek-R1推理模型深度解析.md) — GRPO 算法，V4 后训练的基础
- [08-DeepSeek生态对比与选型指南](./08-DeepSeek生态对比与选型指南.md) — V4 vs 竞品横向对比

---

*最后更新：2026年7月*
