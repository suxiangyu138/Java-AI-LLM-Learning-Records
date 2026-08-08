# 09 - NLP 工具与库实战

> 🎯 NLTK、spaCy、jieba、HuggingFace — 从传统 NLP 到 LLM，四套工具覆盖全场景。选对工具，生产力翻倍

---

## 目录

1. [NLTK：教学首选](#1-nltk教学首选)
2. [spaCy：工业级 NLP](#2-spacy工业级-nlp)
3. [jieba：中文分词利器](#3-jieba中文分词利器)
4. [HuggingFace：LLM 时代标配](#4-huggingface-llm-时代标配)
5. [工具选型指南](#5-工具选型指南)

---

## 1. NLTK：教学首选

```python
import nltk

# 下载必要数据（首次使用）
# nltk.download('punkt')       # 分词模型
# nltk.download('stopwords')   # 停用词
# nltk.download('wordnet')     # 词形还原词典
# nltk.download('averaged_perceptron_tagger')  # 词性标注

text = "Natural language processing is fascinating!"

# 分句
from nltk.tokenize import sent_tokenize
sentences = sent_tokenize(text)

# 分词
from nltk.tokenize import word_tokenize
words = word_tokenize(text)

# 词性标注
from nltk import pos_tag
tagged = pos_tag(words)
# [('Natural', 'JJ'), ('language', 'NN'), ...]

# 词形还原
from nltk.stem import WordNetLemmatizer
lemmatizer = WordNetLemmatizer()
lemmatizer.lemmatize("running", pos="v")  # 'run'

# N-gram
from nltk.util import ngrams
list(ngrams(words, 2))  # 二元组
```

**NLTK 定位**：教学 + 原型，不适合生产（速度慢、内存大）。

---

## 2. spaCy：工业级 NLP

```python
import spacy

# 加载模型
# python -m spacy download en_core_web_sm  # 英文小模型
# python -m spacy download zh_core_web_sm  # 中文小模型

nlp = spacy.load("en_core_web_sm")

doc = nlp("Apple is looking at buying U.K. startup for $1 billion")

# 分词
for token in doc:
    print(f"{token.text}: {token.pos_}, {token.lemma_}, {token.is_stop}")

# 命名实体识别
for ent in doc.ents:
    print(f"{ent.text}: {ent.label_}")
# Apple: ORG, U.K.: GPE, $1 billion: MONEY

# 名词短语
for chunk in doc.noun_chunks:
    print(chunk.text)
# Apple, U.K. startup

# 依存句法分析
for token in doc:
    print(f"{token.text} → {token.dep_} → {token.head.text}")
```

### spaCy 处理中文

```python
nlp_zh = spacy.load("zh_core_web_sm")
doc = nlp_zh("马云在杭州创立了阿里巴巴")

for ent in doc.ents:
    print(f"{ent.text}: {ent.label_}")
# 马云: PERSON, 杭州: GPE, 阿里巴巴: ORG
```

**spaCy 定位**：生产级 NLP，高性能 Cython 底层，一站式 NLU pipeline。

---

## 3. jieba：中文分词利器

```python
import jieba
import jieba.analyse

text = "自然语言处理是人工智能的一个重要分支"

# === 分词 ===
words = jieba.lcut(text)
# ['自然语言处理', '是', '人工智能', '的', '一个', '重要', '分支']

# === 关键词提取（TF-IDF）===
keywords = jieba.analyse.extract_tags(text, topK=3)
# ['自然语言处理', '人工智能', '分支']

# === 关键词提取（TextRank）===
keywords = jieba.analyse.textrank(text, topK=3)

# === 词性标注 ===
import jieba.posseg as pseg
for word, flag in pseg.cut(text):
    print(f"{word}: {flag}")
# 自然语言处理: nz, 是: v, 人工智能: n, ...

# === 自定义词典 ===
jieba.add_word("大语言模型")
jieba.load_userdict("my_dict.txt")  # 批量加载
```

---

## 4. HuggingFace：LLM 时代标配

### 4.1 Pipeline 一键调用

```python
from transformers import pipeline

# === 情感分析 ===
classifier = pipeline("sentiment-analysis")
result = classifier("I love this product!")
# [{'label': 'POSITIVE', 'score': 0.999}]

# === NER ===
ner = pipeline("ner", model="dslim/bert-base-NER")
result = ner("Elon Musk founded SpaceX in California")

# === 摘要 ===
summarizer = pipeline("summarization")
summary = summarizer(long_text, max_length=50)

# === 翻译 ===
translator = pipeline("translation", model="Helsinki-NLP/opus-mt-zh-en")
result = translator("自然语言处理很有趣")

# === 文本生成 ===
generator = pipeline("text-generation", model="gpt2")
result = generator("The future of AI is", max_length=30)

# === QA ===
qa = pipeline("question-answering")
result = qa(question="Who founded SpaceX?", context="Elon Musk founded SpaceX...")
```

### 4.2 中文 Pipeline

```python
# 中文情感分析
classifier = pipeline("sentiment-analysis",
                      model="uer/roberta-base-finetuned-jd-binary-chinese")
result = classifier("这个产品质量太差了！")

# 中文分词/词性标注
token_classifier = pipeline("token-classification",
                            model="ckiplab/bert-base-chinese-ner")
```

---

## 5. 工具选型指南

| 场景 | 推荐工具 | 原因 |
|------|----------|------|
| **学习 NLP 概念** | NLTK | 教学资源丰富、API 清晰 |
| **生产级文本处理** | spaCy | 快、一站式、工业级 |
| **中文分词** | jieba | 最成熟的中文分词库 |
| **快速验证 NLP 任务** | HuggingFace pipeline | 一行代码调用 SOTA 模型 |
| **LLM 应用开发** | OpenAI / LangChain | Prompt + LLM |
| **Embedding** | sentence-transformers | 专用 Embedding 库 |
| **RAG 系统** | LangChain / LlamaIndex | 完整 RAG 框架 |

```text
LLM 时代的工具选择建议：

  ✅ 首先尝试 HuggingFace pipeline → 零代码验证可行性
  ✅ 需要定制 → 加载模型微调或用 LLM Prompt
  ✅ 中文预处理 → jieba 分词 + spaCy 中文模型
  ✅ 生产级 RAG → LangChain/LlamaIndex + 向量数据库
```

---

## 核心要点回顾

- NLTK：教学用，理解 NLP 概念
- spaCy：工业级，快速且功能完整
- jieba：中文分词首选
- HuggingFace pipeline：一行代码跑 SOTA → 快速验证
- LLM 时代：Prompt 替代了很多传统工具，但分词/预处理仍需专用库
