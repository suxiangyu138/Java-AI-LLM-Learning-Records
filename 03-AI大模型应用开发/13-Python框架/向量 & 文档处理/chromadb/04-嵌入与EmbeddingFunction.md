# 04 - 嵌入与 EmbeddingFunction

> 定位：检索质量的真正决定者——"嵌入函数是集合的持久化配置，选模型、换模型、自定义模型的全部姿势"——默认 ONNX 免配置起步，中文/领域场景换模型是必须

---

## 📚 目录

1. [嵌入函数机制：集合的"翻译官"](#1-嵌入函数机制集合的翻译官)
2. [默认嵌入：ONNX MiniLM 免配置起步](#2-默认嵌入onnx-minilm-免配置起步)
3. [第三方嵌入：OpenAI/HuggingFace/Cohere](#3-第三方嵌入openaihuggingfacecohere)
4. [自定义嵌入函数](#4-自定义嵌入函数)
5. [持久化与换模型](#5-持久化与换模型)
6. [嵌入一致性纪律](#6-嵌入一致性纪律)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. 嵌入函数机制：集合的"翻译官"

嵌入函数（EmbeddingFunction）解决的是 RAG 链路最容易出事故的一环：**"文本 → 向量"的翻译必须全流程一致**。Chroma 的解法是把嵌入函数**绑定到集合**而不是散落在业务代码里：

```python
collection = client.create_collection(
    name="docs",
    embedding_function=my_embedding_function,  # 绑定翻译官
)
collection.add(documents=[...])   # 自动用绑定函数算向量
collection.query(query_texts=[...])  # 查询也走同一函数
```

**"add 时翻译一次，query 时再翻译一次——两次必须是同一套翻译"**——这是 RAG 检索正确性的根基：若入库用模型 A、查询用模型 B，向量空间不同，相似度全是噪声。Chroma 把嵌入函数收进集合配置，**业务代码里不再出现"向量怎么算"的细节**。

接口长这样：任何实现了 `__call__(self, input: Documents) -> Embeddings` 的对象都可以是嵌入函数——**"接口只有一个方法，门槛低到可以手写"**。

## 2. 默认嵌入：ONNX MiniLM 免配置起步

不传 `embedding_function` 时，Chroma 使用内置的 **`ONNXMiniLM_L6_V2`**（即 DefaultEmbeddingFunction）：基于 sentence-transformers 的 `all-MiniLM-L6-v2` 模型，**本地 OnnxRuntime 推理，384 维向量，模型文件首次使用时自动下载**——"零配置、零 API key、离线可跑"。

**它的适用边界要认清**：模型是**英文为主的通用轻量模型**（句子嵌入基准上的性价比之选），对中文的效果"能跑但一般"，对领域术语（医学/法律/代码）更弱。**定位：原型与 Demo 的事实标准，生产的底线而非上限**。

**GPU 加速**：装 `onnxruntime-gpu` 后传 `preferred_providers=["CUDAExecutionProvider"]` 即可用 GPU 推理；不过文档也提醒——同模型用 sentence-transformers 直接跑 GPU 通常更快，**"默认模型本身不需要 GPU，换模型时再考虑 GPU"**。

## 3. 第三方嵌入：OpenAI/HuggingFace/Cohere

Chroma 内置一批开箱即用的嵌入函数（`chromadb.utils.embedding_functions`）：

```python
from chromadb.utils.embedding_functions import (
    OpenAIEmbeddingFunction, HuggingFaceEmbeddingFunction,
    SentenceTransformerEmbeddingFunction, CohereEmbeddingFunction,
)
# OpenAI：默认 text-embedding-ada-002，可换 text-embedding-3-small
ef = OpenAIEmbeddingFunction(model_name="text-embedding-3-small")  # 读 OPENAI_API_KEY
# 本地模型：sentence-transformers 直接推理（比 ONNX 版更灵活）
ef = SentenceTransformerEmbeddingFunction(model_name="BAAI/bge-m3")  # 中文场景热门
```

**选型三原则**：其一，**中文/多语言场景换模型**——bge-m3、m3e 等中文系模型或 OpenAI `text-embedding-3-small`；其二，**同一集合内模型终身绑定**——"选模型就是选集合的'母语'，换语言要重建集合"；其三，**API 型嵌入注意成本与限流**——批量入库走异步/批处理，别在循环里一次调一个（05 篇批量）。

## 4. 自定义嵌入函数

任何 EmbeddingFunction 子类都能注册进 Chroma：

```python
from chromadb.api.types import EmbeddingFunction, Documents, Embeddings

class MyEF(EmbeddingFunction):
    def __call__(self, input: Documents) -> Embeddings:
        return [my_embedder(doc) for doc in input]  # 自己的向量来源

collection = client.create_collection(name="custom", embedding_function=MyEF())
```

**自定义场景**：接公司自研向量服务、图像/音频向量（多模态，02 篇提到的 OpenCLIP 图文互检）、或任何 Chroma 没内置的供应商——**"只要有个把文本变向量的函数，它就是嵌入函数"**。要支持配置持久化（重连自动恢复），需实现 `name()/get_config()/build_from_config()` 并加 `@register_embedding_function` 装饰器——简单场景可跳过，每次显式传函数即可。

## 5. 持久化与换模型

**持久化**：v1.1.13 起，**嵌入函数持久化在服务端集合配置里**——集合创建时指定一次，之后 `get_collection` 自动恢复同一函数，"重连后集合记得自己的翻译官"（自定义函数未注册时需重新传参）。

**换模型是重活，要认命**：嵌入函数**创建后不可改**（配置在集合元数据里固定），换模型的标准姿势是**克隆集合**——新建集合（新嵌入函数）→ 读出旧集合全部 `documents + metadatas` → 写入新集合 → 删旧集合。**"向量不可迁移，文本可以"**——所以换模型永远搬原文，不搬向量：

```python
old = client.get_collection("kb")
data = old.get(include=["documents", "metadatas"])
new = client.create_collection("kb_v2", embedding_function=BetterEF())
new.add(ids=data["ids"], documents=data["documents"], metadatas=data["metadatas"])
client.delete_collection("kb")
```

## 6. 嵌入一致性纪律

1. **入库与查询必须同一函数**——Chroma 自动保证（集合绑定），但**绕过集合直接传 `embeddings=` 时自己负责**：手传向量必须与集合绑定函数同模型同维度。
2. **一个集合一个模型**——"换模型不换集合"是最大的检索污染（新旧向量空间不同）。
3. **维度校验是硬错误**——维度不匹配抛 `InvalidDimensionException`，报错即是保护：**"报错好过静默错误"**。
4. **框架集成时警惕函数丢失**——第三方集成（如 DSPy）的检索初始化里嵌入函数可能没有正确传播到实际集合，导致与默认函数混用、维度报错——**"集成层配置后要验证 get_collection 拿回的函数"**（08 篇）。

## 7. 常见坑

1. **默认模型跑中文生产**——效果与英文差距明显，中文场景换 bge-m3 / text-embedding-3-small。
2. **换嵌入函数试图改旧集合**——配置不可变，报错或静默失效；正确姿势是克隆重建。
3. **循环里逐个调 API 嵌入**——限流 + 慢；批量嵌入（04/05 篇）是正确姿势。
4. **只给 embeddings 不给 documents**——查询结果无原文（03 篇已述，嵌入与原文绑定存放）。
5. **模型文件下载失败**——离线环境预下载模型文件，或用纯 API 型嵌入。
6. **不同集合不同模型却混着用**——各自集合没问题，别在业务里手动混向量。

## 8. 练习 5 题

1. 嵌入函数为什么要"绑定到集合"而不是散在业务代码里？
2. 默认嵌入模型是什么？它的适用边界（语言/场景）？
3. 中文 RAG 场景换什么嵌入？选型三原则？
4. 换模型的正确姿势？为什么"搬原文不搬向量"？
5. 嵌入一致性纪律四条？"报错好过静默错误"怎么理解？

> 🎯 **核心要点**：嵌入函数 = **集合的持久化配置（一个接口 __call__）+ 默认 ONNX 免配置 + 第三方/自定义任选 + 换模型必须克隆重建**——"检索质量的 80% 在嵌入选择，而嵌入选择在集合创建那一刻定死——选好母语，再谈检索"。

---

**下一模块**：[05-数据写入与更新.md](05-数据写入与更新.md) / **返回总览**：[00-chromadb总览.md](00-chromadb总览.md)
