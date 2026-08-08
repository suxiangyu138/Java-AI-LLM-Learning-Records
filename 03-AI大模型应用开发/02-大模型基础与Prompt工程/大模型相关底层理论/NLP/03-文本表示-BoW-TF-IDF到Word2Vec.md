# 03 - 文本表示：从 BoW、TF-IDF 到 Word2Vec

> 🎯 文本表示 = 把文字变成计算机能计算的数字。从 One-Hot 到 Embedding，这是一条从"符号"到"语义"的进化之路

---

## 目录

1. [One-Hot 编码](#1-one-hot-编码)
2. [词袋模型 BoW](#2-词袋模型-bow)
3. [TF-IDF](#3-tf-idf)
4. [Word2Vec](#4-word2vec)
5. [GloVe 与 FastText](#5-glove-与-fasttext)
6. [从静态向量到上下文向量](#6-从静态向量到上下文向量)

---

## 1. One-Hot 编码

```text
One-Hot：每个词是一个独热向量

词表 = ["猫", "狗", "鸟", "鱼"]
  猫 → [1, 0, 0, 0]
  狗 → [0, 1, 0, 0]
  鸟 → [0, 0, 1, 0]

优点：简单、唯一
缺点：
  ❌ 维度 = 词表大小（几万~几十万维）
  ❌ 极度稀疏（99.9% 是 0）
  ❌ 无语义信息 → "猫"和"狗"的距离 = "猫"和"鱼"的距离
```

---

## 2. 词袋模型 BoW

### 2.1 原理

```text
BoW (Bag of Words) = 统计文档中每个词出现的次数

"我喜欢猫，猫很可爱" → {"我":1, "喜欢":1, "猫":2, "很":1, "可爱":1}
"我喜欢狗"         → {"我":1, "喜欢":1, "狗":1}

问题：丢失词序！
  "我喜欢你" 和 "你喜欢我" → BoW 向量完全相同
```

### 2.2 代码

```python
from sklearn.feature_extraction.text import CountVectorizer

corpus = [
    "我喜欢猫，猫很可爱",
    "我喜欢狗",
    "猫和狗都是宠物"
]

vectorizer = CountVectorizer()
X = vectorizer.fit_transform(corpus)

print(vectorizer.get_feature_names_out())
# ['cats', ...]

print(X.toarray())
# [[0, 1, 2, ...],   ← "猫"出现了 2 次
#  [0, 0, 0, ...]]
```

---

## 3. TF-IDF

### 3.1 原理

```text
TF-IDF = 词频 × 逆文档频率

  TF (Term Frequency)：词在本文档中出现的次数（词的局部重要性）
  IDF (Inverse Document Frequency)：总文档数/包含该词的文档数的对数（词的全局区分度）

  TF-IDF(w, d) = TF(w, d) × log(N / DF(w))

直觉：
  → 高频但出现在所有文档中的词 → IDF 小 → 重要性低（如"的"、"是"）
  → 高频但只出现在少数文档中的词 → IDF 大 → 重要性高（区分度强）
```

### 3.2 代码

```python
from sklearn.feature_extraction.text import TfidfVectorizer

vectorizer = TfidfVectorizer(max_features=1000, ngram_range=(1, 2))
X = vectorizer.fit_transform(corpus)

# 查看词的重要性
scores = zip(vectorizer.get_feature_names_out(), 
             X.toarray().sum(axis=0))
for word, score in sorted(scores, key=lambda x: x[1], reverse=True)[:5]:
    print(f"{word}: {score:.3f}")
```

---

## 4. Word2Vec

### 4.1 原理

```text
Word2Vec (Mikolov et al., 2013)：
  用神经网络将词映射为低维稠密向量，语义相近的词向量距离近

两种训练方式：

① CBOW (Continuous Bag of Words)：
   用上下文预测中心词
   "我 _ 吃苹果" → 预测 "喜欢"

② Skip-gram（更常用）：
   用中心词预测上下文
   "喜欢" → 预测 "我"、"吃"、"苹果"

核心创新：
  → 稠密向量（100-300 维 vs 几万维的 One-Hot）
  → 语义信息："国王" - "男人" + "女人" ≈ "王后"
  → 预训练 → 可复用（就像 LLM 的 Embedding 层）
```

### 4.2 训练示例

```python
from gensim.models import Word2Vec

# 准备数据（分好词的句子列表）
sentences = [
    ["我", "喜欢", "自然语言处理"],
    ["自然语言处理", "是", "AI", "的", "分支"],
    ["深度学习", "推动", "了", "NLP", "的", "发展"],
]

# 训练 Word2Vec
model = Word2Vec(
    sentences,
    vector_size=100,    # 向量维度
    window=5,            # 上下文窗口大小
    min_count=1,         # 最小词频
    workers=4,           # 并行线程数
    sg=1                 # 1=Skip-gram, 0=CBOW
)

# 获取词向量
vec = model.wv["自然语言处理"]
print(f"维度: {len(vec)}")  # 100

# 找相似词
similar = model.wv.most_similar("NLP", topn=5)
print(similar)

# 语义运算
result = model.wv.most_similar(
    positive=["国王", "女人"], negative=["男人"]
)
print(result[0])  # 可能是 "王后"
```

### 4.3 Word2Vec 的局限

| 局限 | 说明 |
|------|------|
| **静态向量** | "苹果"在"吃苹果"和"苹果手机"中向量相同 → 无法区分多义词 |
| **OOV** | 训练中未见过的词无法给向量 |
| **上下文窗口有限** | 只能捕捉局部上下文（5-10 词窗口） |
| **被替代** | LLM 的上下文 Embedding 已全面超越 |

---

## 5. GloVe 与 FastText

| 模型 | 原理 | vs Word2Vec |
|------|------|------------|
| **GloVe** | 基于全局词共现矩阵分解 | 利用全局统计信息 |
| **FastText** | 子词 n-gram 组合 | 解决 OOV（未知词可由子词组合） |

---

## 6. 从静态向量到上下文向量

```text
文本表示的进化：

第一代：One-Hot / BoW / TF-IDF
  → 符号表示，无语义

第二代：Word2Vec / GloVe / FastText
  → 静态语义向量，"苹果"始终同一个向量
  → 不能区分多义词

第三代：ELMo → BERT → LLM Embedding
  → 上下文相关向量
  → "吃苹果"的"苹果" ≠ "苹果手机"的"苹果"
  → 不同上下文 → 不同向量

LLM 的 Embedding：
  输入 → Token Embedding + Position Embedding → 自注意力 → 上下文相关表示
  每个 token 的输出向量已经融合了全文信息

这就是为什么 LLM 不需要 Word2Vec —
  它的 Embedding 层就是 Word2Vec，而且更强（上下文相关）
```

---

## 核心要点回顾

- One-Hot：最简单的文本表示，无语义、维度过大
- TF-IDF：文本分类 baseline，对关键词匹配仍有效
- Word2Vec：静态词向量，语义相近=向量相近，多义词无法区分
- LLM Embedding：上下文相关 → 同一个词在不同句子中向量不同
- 文本表示进化 = 从符号 → 静态语义 → 动态上下文语义
