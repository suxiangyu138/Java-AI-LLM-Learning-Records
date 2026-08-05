# 05 - BERT：Encoder-Only 架构

> 🎯 BERT 是 NLP 的"ImageNet 时刻"——证明大规模预训练 + 下游微调范式远超任务特定模型。理解 BERT 的双向注意力、MLM 预训练、微调范式，是理解整个预训练时代的钥匙

---

## 目录

1. [BERT 的设计理念](#1-bert-的设计理念)
2. [MLM 预训练](#2-mlm-预训练)
3. [双向注意力 vs 单向](#3-双向注意力-vs-单向)
4. [Encoder-Only vs Decoder-Only](#4-encoder-only-vs-decoder-only)

---

## 1. BERT 的设计理念

```text
BERT (Bidirectional Encoder Representations from Transformers)

思想：语言理解应该是双向的
  "我 [MASK] 北京天安门" → 可以根据前后文推断出"爱"

架构：Transformer Encoder（仅 Encoder，无 Decoder）
  → 没有 Causal Mask → 每个 token 可以看到左右两边
```

### BERT 模型规格

| 模型 | 层数 | d_model | 头数 | 总参数 |
|------|:---:|:---:|:---:|:---:|
| BERT-Base | 12 | 768 | 12 | 110M |
| BERT-Large | 24 | 1024 | 16 | 340M |

## 2. MLM 预训练

```text
MLM (Masked Language Model) 

训练方式：
  随机遮挡 15% 的 token → 让模型预测被遮挡的 token

  输入:  "我 [MASK] 北京 [MASK] 安门"
  目标: 预测 [MASK] = "爱", [MASK] = "天"

遮挡策略（避免预训练-微调不匹配）：
  15% 选中的 token 中：
    80% → 替换为 [MASK]
    10% → 替换为随机 token
    10% → 保持不变

为什么不全用 [MASK]？
  → 下游任务没有 [MASK] token → 训练-推理不匹配
  → 混合策略让模型学会"不确定时给出更合理的猜测"
```

### NSP (Next Sentence Prediction)

```text
输入：句子 A + [SEP] + 句子 B
目标：判断 B 是不是 A 的下一句

→ 训练句子级别的理解能力
→ 但后续研究发现 NSP 作用不大，RoBERTa 去掉了
```

## 3. 双向注意力 vs 单向

```text
双向注意力（BERT Encoder）：
  Token "爱" 可以同时看到左边"我"和右边"北京天安门"
  适合：理解任务（分类/实体识别/问答）

因果注意力（GPT Decoder）：
  Token "爱" 只能看到左边"我"，看不到右边
  适合：生成任务（续写/翻译/对话）
```

```text
Attention Mask 对比：

BERT Mask（双向）：        GPT Mask（因果）：
  a b c d e                  a b c d e
a 1 1 1 1 1              a 1 0 0 0 0
b 1 1 1 1 1              b 1 1 0 0 0
c 1 1 1 1 1              c 1 1 1 0 0
d 1 1 1 1 1              d 1 1 1 1 0
e 1 1 1 1 1              e 1 1 1 1 1

1 = 可以看，0 = 不能看
```

## 4. Encoder-Only vs Decoder-Only

| 维度 | BERT (Encoder-Only) | GPT (Decoder-Only) |
|------|:---:|:---:|
| 注意力 | 双向 | 因果（只看左边） |
| 预训练目标 | MLM（预测遮挡词） | LM（预测下一个词） |
| 擅长 | 理解（分类/抽取） | 生成（续写/对话） |
| 下游适配 | 需要加任务头+微调 | Few-shot/Zero-shot |
| 代表 | BERT/RoBERTa/DeBERTa | GPT/Llama/Qwen |
| 当前地位 | 被 LLM 替代 | **主流范式** |

> 🎯 为什么 Decoder-Only 赢了？GPT-3 证明了"足够大的 Decoder-Only 模型 + Few-shot Prompt 可以替代 BERT 的理解能力"—一个模型搞定理解和生成

## 核心要点回顾

- BERT = 双向 Encoder + MLM 预训练 → NLP 的 ImageNet 时刻
- MLM = 随机遮挡 15% token → 预测 → 学习双向理解
- 双向注意力 = 理解任务强；因果注意力 = 生成任务强
- Decoder-Only (GPT) 最终统一了理解和生成
- BERT 时代已过，但其预训练-微调范式影响了整个 NLP

## 参考资料

1. BERT 论文 (Devlin et al., 2019)
2. RoBERTa 论文 (Liu et al., 2019)
