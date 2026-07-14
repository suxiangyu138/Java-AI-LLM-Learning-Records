# 第3步：自然语言处理(NLP)基础

> **阶段目标：** 掌握文本预处理技术，理解Tokenization本质，建立词向量(Word Embeddings)的核心概念  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** Python + 机器学习基础  

---

## 📚 目录

- [3.1 NLP概述与在大模型中的位置](#31-nlp概述与在大模型中的位置)
- [3.2 文本清洗与预处理](#32-文本清洗与预处理)
- [3.3 Token化(Tokenization)](#33-token化tokenization)
- [3.4 词向量(Word Embeddings)](#34-词向量word-embeddings)
- [3.5 经典NLP任务](#35-经典nlp任务)
- [3.6 阶段练习](#36-阶段练习)
- [3.7 常见问题](#37-常见问题)

---

## 3.1 NLP概述与在大模型中的位置

### 3.1.1 NLP全貌

```
NLP (自然语言处理)
├── NLU (自然语言理解)        ← 大部分任务
│   ├── 文本分类、情感分析
│   ├── 命名实体识别(NER)
│   ├── 关系抽取
│   └── 语义相似度
├── NLG (自然语言生成)        ← 大模型的核心能力
│   ├── 文本生成
│   ├── 摘要生成
│   └── 翻译
└── 基础层                    ← 本阶段重点
    ├── 文本清洗
    ├── Tokenization
    └── 词向量/Embedding
```

### 3.1.2 为什么NLP是大模型的基石？

```
文字(人类可读) → Tokenization → Token IDs → Embedding → 向量(机器可计算)
                                                          ↓
                                            Attention/Transformer → 语义理解
```

**关键洞察：** 大模型内部没有文字，只有数字。NLP基础就是教你把文字"翻译"成模型能理解的数字。不理解Tokenizer，你就无法理解为什么"strawberry"会被分成["straw", "berry"]，也无法优化Token成本。

---

## 3.2 文本清洗与预处理

### 3.2.1 清洗流程

```python
import re
import unicodedata
from typing import List, Tuple

class TextCleaner:
    """企业级文本清洗器 — 参考生产环境标准"""
    
    @staticmethod
    def normalize_unicode(text: str) -> str:
        """Unicode归一化 → 统一全角/半角，处理特殊字符"""
        return unicodedata.normalize('NFKC', text)
    
    @staticmethod
    def remove_html(text: str) -> str:
        """移除HTML标签 — 处理网页抓取内容"""
        clean = re.compile('<.*?>')
        return re.sub(clean, '', text)
    
    @staticmethod
    def normalize_whitespace(text: str) -> str:
        """统一空白字符 — 多余空格→单个空格"""
        return ' '.join(text.split())
    
    @staticmethod
    def remove_urls(text: str) -> str:
        """移除URL链接"""
        url_pattern = re.compile(
            r'https?://(?:[-\w.]|(?:%[\da-fA-F]{2}))+[^\s]*'
        )
        return url_pattern.sub('[URL]', text)
    
    @staticmethod
    def remove_emails(text: str) -> str:
        """移除/替换邮箱地址（隐私保护）"""
        return re.sub(r'\S+@\S+', '[EMAIL]', text)
    
    @staticmethod
    def normalize_chinese_punctuation(text: str) -> str:
        """统一中文标点 — 全角→半角转换"""
        replacements = {
            '，': ',', '。': '.', '！': '!', '？': '?',
            '；': ';', '：': ':', '"': '"', '"': '"',
            ''': '\'', ''': '\'', '【': '[', '】': ']',
            '（': '(', '）': ')', '《': '<', '》': '>',
        }
        for full, half in replacements.items():
            text = text.replace(full, half)
        return text
    
    def clean(self, text: str, steps: List[str] = None) -> str:
        """执行完整清洗管线"""
        if steps is None:
            steps = ['unicode', 'html', 'urls', 'whitespace']
        
        pipeline = {
            'unicode': self.normalize_unicode,
            'html': self.remove_html,
            'urls': self.remove_urls,
            'whitespace': self.normalize_whitespace,
        }
        
        for step in steps:
            if step in pipeline:
                text = pipeline[step](text)
        return text


# 使用示例
cleaner = TextCleaner()
raw = "<p>Hello   World!!  访问 https://example.com 了解更多</p>"
print(cleaner.clean(raw))
# Output: "Hello World!! 访问 [URL] 了解更多"
```

### 3.2.2 中文文本处理

```python
import jieba
import re

class ChineseTextProcessor:
    """中文文本专用处理器"""
    
    @staticmethod
    def is_chinese(char: str) -> bool:
        """判断是否为中文字符"""
        return '一' <= char <= '鿿'
    
    @staticmethod
    def extract_chinese(text: str) -> str:
        """提取纯中文内容"""
        return ''.join(c for c in text if ChineseTextProcessor.is_chinese(c))
    
    @staticmethod
    def segment(text: str, mode: str = 'accurate') -> List[str]:
        """中文分词
        
        mode: 'accurate'(精确) | 'full'(全模式) | 'search'(搜索引擎)
        """
        if mode == 'full':
            return list(jieba.cut(text, cut_all=True))
        elif mode == 'search':
            return list(jieba.cut_for_search(text))
        else:
            return list(jieba.cut(text, cut_all=False))
    
    @staticmethod
    def remove_stopwords(words: List[str], 
                         stopwords: set = None) -> List[str]:
        """移除停用词"""
        if stopwords is None:
            # 常见中文停用词
            stopwords = {'的', '了', '在', '是', '我', '有', '和', '就',
                        '不', '人', '都', '一', '一个', '上', '也', '很',
                        '到', '说', '要', '去', '你', '会', '着', '没有'}
        return [w for w in words if w not in stopwords]


# 使用示例
processor = ChineseTextProcessor()
text = "今天天气真好啊，我们一起去公园散步吧！"
words = processor.segment(text, mode='accurate')
print(f"分词结果: {words}")
# ['今天', '天气', '真', '好', '啊', '，', '我们', '一起', '去', '公园', '散步', '吧', '！']

meaningful = processor.remove_stopwords(words)
print(f"去停用词: {meaningful}")
# ['今天', '天气', '啊', '，', '一起', '公园', '散步', '！']
```

### 3.2.3 数据清洗的原则

```
┌─────────────────────────────────────────────────────────────┐
│  做 (DO):                                                   │
│  ✅ 保持可追溯性 — 保存清洗前后的数据                         │
│  ✅ 可配置 — 清洗步骤应该可开关                               │
│  ✅ 一致性 — 训练和推理用完全相同的清洗逻辑                    │
│  ✅ 防御性 — 处理各种异常输入（空字符串、纯数字、纯符号）       │
│                                                             │
│  不做 (DON'T):                                              │
│  ❌ 过度清洗 — 把有意义的信息洗掉了                           │
│  ❌ 静默失败 — 出错了不报也不记录                             │
│  ❌ 规则写死 — 把清洗规则硬编码在代码各处                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3.3 Token化(Tokenization)

### 3.3.1 什么是Token？

```
Token = 模型处理文本的最小语义单元

[原始文本]
"我喜欢学习AI大模型"

[BPE Tokenizer] (GPT系列使用的)
['我', '喜欢', '学习', 'AI', '大', '模型']      # 6 tokens

[Character Tokenizer]
['我', '喜', '欢', '学', '习', 'A', 'I', '大', '模', '型']  # 10 tokens

[Word Tokenizer] (英文)
"I love learning AI large models"
['I', 'love', 'learning', 'AI', 'large', 'models']  # 6 tokens
```

### 3.3.2 主流Tokenization算法

#### BPE (Byte-Pair Encoding) — GPT系列使用

```python
from tokenizers import Tokenizer, models, trainers, pre_tokenizers

# 从零训练一个BPE Tokenizer
tokenizer = Tokenizer(models.BPE())
tokenizer.pre_tokenizer = pre_tokenizers.ByteLevel(add_prefix_space=True)

trainer = trainers.BpeTrainer(
    vocab_size=5000,
    special_tokens=["[PAD]", "[UNK]", "[CLS]", "[SEP]", "[MASK]"],
    min_frequency=2,
)

# 用你的语料训练（这里用示例数据）
corpus = [
    "我喜欢学习人工智能",
    "人工智能正在改变世界",
    "深度学习是人工智能的一个分支",
]
# tokenizer.train_from_iterator(corpus, trainer)
```

#### WordPiece — BERT使用

```python
# BERT的WordPiece特点：
# - 用"##"标记词内部的子词
# - "playing" → ["play", "##ing"]
# - "unhappiness" → ["un", "##happ", "##iness"]
# - 选择能最大化语言模型似然的合并
```

#### SentencePiece — T5, LLaMA使用

```python
# SentencePiece特点：
# - 直接在原始文本上训练（不需要预分词）
# - 将空格也视为普通字符
# - 完全可逆：Token IDs可以无损还原为原始文本
# - 支持BPE和Unigram两种算法
```

### 3.3.3 实战：使用tiktoken

```python
import tiktoken

# ========== GPT-4 Tokenizer ==========
enc = tiktoken.encoding_for_model("gpt-4")

# 编码
text = "Hello, how are you? 你好吗？"
tokens = enc.encode(text)
print(f"文本: {text}")
print(f"Token IDs: {tokens}")
print(f"Token数: {len(tokens)}")

# 逐个解码Token（看清楚每个Token是什么）
for token_id in tokens:
    token_bytes = enc.decode_single_token_bytes(token_id)
    print(f"  ID {token_id:>6}: {token_bytes}")

# 解码回文本
decoded = enc.decode(tokens)
print(f"解码: {decoded}")
assert decoded == text, "编码-解码应该完全可逆！"

# ========== Token计数 — API调用的基本功 ==========
def count_tokens(text: str, model: str = "gpt-4") -> int:
    """计算文本的Token数"""
    try:
        enc = tiktoken.encoding_for_model(model)
    except KeyError:
        enc = tiktoken.get_encoding("cl100k_base")
    return len(enc.encode(text))


def estimate_cost(prompt: str, expected_output_tokens: int = 500,
                  model: str = "gpt-4") -> dict:
    """估算API调用成本"""
    # GPT-4 定价 (2024年参考)
    pricing = {
        "gpt-4": {"input": 0.03, "output": 0.06},       # 每1K tokens
        "gpt-4-turbo": {"input": 0.01, "output": 0.03},
        "gpt-3.5-turbo": {"input": 0.0005, "output": 0.0015},
    }
    
    p = pricing.get(model, pricing["gpt-4"])
    input_tokens = count_tokens(prompt, model)
    
    cost = (input_tokens / 1000) * p["input"] + \
           (expected_output_tokens / 1000) * p["output"]
    
    return {
        "model": model,
        "input_tokens": input_tokens,
        "estimated_output_tokens": expected_output_tokens,
        "estimated_cost_usd": round(cost, 6),
    }

# 测试
result = estimate_cost("给我写一篇关于人工智能的500字文章", model="gpt-4-turbo")
print(f"预估成本: ${result['estimated_cost_usd']}")
```

### 3.3.4 Tokenization的陷阱

```python
# ⚠️ 陷阱1: 中文Token效率远低于英文
en_text = "Artificial Intelligence is changing the world."
zh_text = "人工智能正在改变世界。"

enc = tiktoken.encoding_for_model("gpt-4")
print(f"英文: '{en_text}' → {len(enc.encode(en_text))} tokens")
print(f"中文: '{zh_text}' → {len(enc.encode(zh_text))} tokens")
# 同样的意思，中文可能需要更多Tokens → 成本更高！

# ⚠️ 陷阱2: 特殊格式消耗大量Token
json_text = '{"name": "张三", "age": 25, "city": "北京"}'
print(f"JSON文本: {len(enc.encode(json_text))} tokens")
# JSON的大量标点符号都是独立Token → 结构化输出很贵！

# ⚠️ 陷阱3: 不同模型的Tokenizer不同
# GPT的Tokenizer不一定能在Claude上正确计数
# 跨模型时Token计数只是近似值
```

---

## 3.4 词向量(Word Embeddings)

### 3.4.1 核心直觉

```
词向量 = 把词语映射到高维空间中的一个点
语义相似的词 → 向量距离近
语义关系 → 向量运算 (国王 - 男人 + 女人 ≈ 王后)

    美丽(0.8, 0.6)
      ·              
                    聪明(0.7, 0.3)
                      ·
        漂亮(0.75, 0.55)
          ·
                              ← 这些词在"正面"区域
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                              ← 这些词在"负面"区域
                    ·
                  丑陋(-0.5, -0.7)
            ·
        愚蠢(-0.4, -0.8)
```

### 3.4.2 Word2Vec核心原理

```python
"""
Word2Vec的两种训练方式：

1. CBOW (Continuous Bag of Words)
   上下文词 → 预测 → 目标词
   "我 [___] 学习 AI" → 预测 "喜欢"

2. Skip-gram
   目标词 → 预测 → 上下文词
   "喜欢" → 预测 → "我", "学习", "AI"

关键洞察：我们不关心预测结果本身，
我们关心的是训练过程中学到的隐层权重 → 这就是词向量！
"""
```

### 3.4.3 实战：使用Gensim训练Word2Vec

```python
import jieba
from gensim.models import Word2Vec
from gensim.models.callbacks import CallbackAny2Vec
import logging

class EpochLogger(CallbackAny2Vec):
    """训练进度回调"""
    def __init__(self):
        self.epoch = 0
    
    def on_epoch_end(self, model):
        loss = model.get_latest_training_loss()
        print(f"Epoch {self.epoch}: loss = {loss:.4f}")
        self.epoch += 1

# ========== 准备语料 ==========
corpus = [
    "人工智能正在深刻改变我们的生活方式",
    "机器学习是人工智能的一个重要分支",
    "深度学习使用神经网络来学习数据特征",
    "自然语言处理是AI领域的重要方向",
    "计算机视觉让机器能够理解图像内容",
    "强化学习通过奖励机制训练智能体",
    "大语言模型在文本生成方面表现出色",
    "向量数据库用于存储和检索高维向量",
]

# 中文分词
sentences = [list(jieba.cut(text)) for text in corpus]
print("分词结果：")
for s in sentences:
    print(f"  {s}")

# ========== 训练Word2Vec ==========
model = Word2Vec(
    sentences=sentences,
    vector_size=100,       # 词向量维度
    window=5,              # 上下文窗口大小
    min_count=1,           # 最低词频
    workers=4,             # 并行线程
    epochs=100,            # 训练轮数
    sg=1,                  # 1=Skip-gram, 0=CBOW
    compute_loss=True,
    callbacks=[EpochLogger()],
)

# ========== 使用词向量 ==========
# 查找相似词
print("\n与'人工智能'最相似的词：")
for word, score in model.wv.most_similar('人工智能', topn=5):
    print(f"  {word}: {score:.4f}")

# 词向量运算
print("\n词向量运算：")
result = model.wv.most_similar(
    positive=['人工智能', '数据'],
    negative=['学习'],
    topn=3
)
for word, score in result:
    print(f"  {word}: {score:.4f}")

# 直接获取向量
vec = model.wv['人工智能']
print(f"\n'人工智能'的向量 (前10维): {vec[:10]}")
print(f"向量维度: {vec.shape}")

# 保存和加载
# model.save("word2vec.model")
# model = Word2Vec.load("word2vec.model")
```

### 3.4.4 从Word2Vec到现代Embedding

```
Word2Vec (2013)
  ↓ 问题：每个词只有一个向量，无法处理多义词
  │       "苹果"可以吃，"苹果"也是公司
  ↓
GloVe / FastText (2014-2016)
  ↓ FastText用子词(subword)缓解了OOV问题
  ↓
ELMo (2018)
  ↓ 上下文相关的词向量 → "苹果"在不同句子中有不同向量
  ↓
BERT / GPT (2018-)
  ↓ 真正的上下文Embedding → 整个句子的表示
  ↓
Sentence Transformers (2019-)
  ↓ 句子级别的语义向量 → RAG的核心组件
  ↓
Modern Embeddings (2023-)
  OpenAI text-embedding-3, Cohere Embed, BGE, E5
  → RAG系统的标准选择
```

### 3.4.5 现代Embedding实战

```python
# ========== 使用Sentence Transformers ==========
from sentence_transformers import SentenceTransformer

# 加载模型（首次会自动下载）
model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')

# 句子转向量
sentences = [
    "如何在Python中读取文件？",
    "Python文件操作的方法是什么？",
    "今天天气真不错",
]

embeddings = model.encode(sentences)
print(f"Embedding shape: {embeddings.shape}")  # (3, 384)

# 计算相似度
from sklearn.metrics.pairwise import cosine_similarity
sim_matrix = cosine_similarity(embeddings)
print("\n语义相似度矩阵：")
for i, s1 in enumerate(sentences):
    for j, s2 in enumerate(sentences):
        print(f"  [{i}] vs [{j}]: {sim_matrix[i][j]:.4f}")
    print()

# 结论：前两句关于编程的很相似，第三句天气相关的不相似
# 这就是RAG中"找最相关的文档片段"的基础！
```

---

## 3.5 经典NLP任务

### 3.5.1 文本分类（情感分析）

```python
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline

# 模拟训练数据
texts = [
    "这个产品非常好用，强烈推荐",      # 正面
    "质量太差了，用了两天就坏了",      # 负面
    "服务态度很好，物流也快",          # 正面
    "完全就是垃圾，不要买",            # 负面
    "性价比不错，会回购的",            # 正面
    "客服态度恶劣，退货还不给退",      # 负面
]
labels = [1, 0, 1, 0, 1, 0]  # 1=正面, 0=负面

# 构建Pipeline
pipeline = Pipeline([
    ('vectorizer', TfidfVectorizer(tokenizer=jieba.cut)),
    ('classifier', MultinomialNB()),
])

pipeline.fit(texts, labels)

# 预测
new_texts = ["这个手机拍照效果真棒", "太失望了，完全不值这个价"]
predictions = pipeline.predict(new_texts)
for text, pred in zip(new_texts, predictions):
    sentiment = "正面 😊" if pred == 1 else "负面 😞"
    print(f"'{text}' → {sentiment}")
```

### 3.5.2 命名实体识别(NER)

```python
# NER在大模型中的应用场景：
# 1. RAG查询中的实体识别 → 精准检索
# 2. 对话系统中的信息提取 → 槽位填充
# 3. 文档处理中的关键信息抽取

# 使用大模型做NER（示范）
ner_prompt = """
从以下文本中提取人名、地名、组织名：
文本："马云于1999年在杭州创立了阿里巴巴集团"

请以JSON格式返回：
{
  "人物": [],
  "地点": [],
  "组织": [],
  "时间": []
}
"""
# 这个任务现在通常直接交给大模型完成，不需要传统NER模型
```

---

## 3.6 阶段练习

### 练习1：Tokenizer对比
下载3个不同模型的tokenizer，对同一段中英混合文本编码，比较Token数量和切分方式。

### 练习2：Embedding搜索引擎
构建一个简单的语义搜索引擎：
1. 收集20篇文档（可以是一段段文字）
2. 用Sentence Transformers编码
3. 输入查询，返回最相关的3篇文档

### 练习3：情感分析Pipeline
构建完整的情感分析管线：清洗→分词→特征提取→分类→评估

---

## 3.7 常见问题

### Q1: 多种Tokenizer算法，该选哪个？

**答：** 不需要选——你用的是哪个预训练模型，就用它配套的Tokenizer。不要混用。

### Q2: 中文分词为什么这么难？

**答：** 中文没有天然的空格分隔，且存在歧义。例如"南京市长江大桥"可以理解为"南京/市长/江大桥"或"南京市/长江大桥"。现代大模型用BPE/Unigram直接在字符级别处理，绕过了分词问题。

### Q3: Embedding维度是多少？越大越好吗？

**答：** 
- 传统Word2Vec: 100-300维
- Sentence-BERT: 384/768维
- OpenAI Embedding: 1536/3072维

维度越大表达能力越强，但存储和计算成本也越高。在RAG中，384维通常已经足够好。

### Q4: 需要学NLTK和spaCy吗？

**答：** 在大模型时代，很多传统NLP任务直接交给LLM做了。但以下知识仍然重要：
- **Tokenization** — 每天都要和它打交道
- **Embedding** — RAG系统的核心
- **文本清洗** — 数据质量决定模型效果

NLTK/spaCy可作为工具了解，不必深入。

---

> **✅ 阶段完成检查清单：**
> - [ ] 能写出健壮的文本清洗代码
> - [ ] 理解BPE/WordPiece/Unigram三种Tokenization的区别
> - [ ] 能用tiktoken精确计算Token数和API成本
> - [ ] 理解词向量的语义特性（相似词距离近，可加减运算）
> - [ ] 会用Sentence Transformers生成句子Embedding
> - [ ] 完成了3个阶段练习
>
> **下一步：** [第4步：Transformer架构与Attention](../04-Transformer架构与Attention/README.md)
