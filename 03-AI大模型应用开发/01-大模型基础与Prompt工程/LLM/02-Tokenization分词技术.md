# 02 - Tokenization 分词技术

> 🎯 Tokenization 是 LLM 的"第一道工序"——把自然语言切成模型能消化的 Token。词表大小、分词策略、特殊 Token 的设计，直接影响模型的效果和效率

---

## 目录

1. [Tokenization 基础](#1-tokenization-基础)
2. [BPE 算法详解](#2-bpe-算法详解)
3. [主流分词器对比](#3-主流分词器对比)
4. [实战：HuggingFace Tokenizer](#4-实战huggingface-tokenizer)

---

## 1. Tokenization 基础

### 1.1 三个层次

```text
输入: "我喜欢吃苹果"

字符级 (Character):   我 / 喜 / 欢 / 吃 / 苹 / 果
  优点: 词表极小 (~几千)
  缺点: 序列太长，语义信息弱

词级 (Word):         我 / 喜欢 / 吃 / 苹果
  优点: 语义完整
  缺点: 词表巨大(OOV问题)，"喜欢吃苹果"没有独立 token

子词级 (Subword):    我 / 喜欢 / 吃 / 苹果
  优点: 平衡词表大小和语义 → LLM 的标准方案
```

### 1.2 关键概念

| 概念 | 说明 |
|------|------|
| **Token** | 模型的输入/输出基本单位 |
| **词表 (Vocabulary)** | 所有可能 Token 的集合，通常 32K-256K |
| **特殊 Token** | `<bos>`开头/`<eos>`结尾/`<pad>`填充/`<unk>`未知 |
| **Tokenization** | 文本 → Token ID 的转换过程 |
| **Detokenization** | Token ID → 文本的逆过程 |
| **Chat Template** | 应用层模板(ChatML/Llama Chat)，包装对话格式 |

## 2. BPE 算法详解

```text
BPE (Byte Pair Encoding) — 最主流的分词算法

核心思想：从字符开始，反复合并最高频的相邻字符对

训练过程：
1. 初始化：每个字符是一个 token
2. 统计所有相邻 token 对的出现频率
3. 合并最高频的对 → 新 token
4. 重复 2-3，直到词表达到目标大小

示例："low" 出现 5 次, "lower" 出现 2 次, "newest" 出现 6 次

初始: l, o, w, e, r, n, s, t (原始字符)
Step 1: l+o → lo (最高频)
Step 2: lo+w → low
Step 3: e+r → er
Step 4: er+s → ers  (newest → new + est → n + ew + est...)
...

最终词表: l, o, w, lo, low, e, r, er, n, ew, est...
```

## 3. 主流分词器对比

| 分词器 | 算法 | 词表大小 | 代表模型 | 特点 |
|------|------|:---:|------|------|
| **GPT-2/3/4** | BPE | 50K-100K | GPT系列 | 字节级 BPE |
| **BERT** | WordPiece | 30K | BERT | 概率合并 |
| **Llama** | BPE+SentencePiece | 32K-128K | Llama/Qwen | 字节回退 |
| **Gemini** | SentencePiece | 256K | Gemini | 最大词表 |
| **Claude** | BPE (custom) | ~100K | Claude | 多语言优化 |

### 词表大小的影响

| 词表大小 | 优点 | 缺点 |
|:---:|------|------|
| 小 (32K) | 嵌入层小，训练快 | 中文等语言 Token 效率低 |
| 中 (50K-100K) | **平衡点** | 少数语言仍低效 |
| 大 (128K-256K) | 多语言效率高 | 嵌入层占更多参数 |

> 🎯 "中文 1 个汉字 ≈ 1.5-3 个 Token"→ 同样内容，中文的 Token 消耗比英文高 50-100%

## 4. 实战：HuggingFace Tokenizer

```python
from transformers import AutoTokenizer

# 加载 Llama 3.1 分词器
tokenizer = AutoTokenizer.from_pretrained("meta-llama/Llama-3.1-8B")

# 编码
text = "Hello, 世界!"
tokens = tokenizer.encode(text)
print(tokens)  # [128000, 9906, 11, 1773, 8878, 0]
print(tokenizer.decode(tokens))  # "Hello, 世界!"

# 查看词表大小
print(len(tokenizer))  # 128256

# 特殊 Token
print(f"BOS: {tokenizer.bos_token} (id={tokenizer.bos_token_id})")
print(f"EOS: {tokenizer.eos_token} (id={tokenizer.eos_token_id})")
print(f"PAD: {tokenizer.pad_token}")

# Chat Template
messages = [
    {"role": "system", "content": "You are helpful."},
    {"role": "user", "content": "Hello!"}
]
formatted = tokenizer.apply_chat_template(messages, tokenize=False)
print(formatted)
# <|begin_of_text|><|start_header_id|>system<|end_header_id|>
# You are helpful.<|eot_id|>...

# Token 计数
token_count = len(tokenizer.encode("解释一下什么是Transformer"))
print(f"8 个中文字符 → {token_count} tokens")  # 通常 > 15
```

## 核心要点回顾

- LLM 标准 = 子词级分词（BPE/WordPiece/SentencePiece）
- BPE 核心：反复合并最高频字符对，直到词表达到目标大小
- 词表大小权衡：小(快)+大(多语言效率)，50K-128K 是主流
- 中文 Token 效率低：1 个汉字常有 1.5-3 个 Token
- 特殊 Token：BOS/EOS/PAD + Chat Template 标记
- `apply_chat_template` 必须用，别手写特殊 Token

## 参考资料

1. BPE 论文 (Sennrich et al., 2016)
2. HuggingFace Tokenizer 文档 — huggingface.co/docs/tokenizers
