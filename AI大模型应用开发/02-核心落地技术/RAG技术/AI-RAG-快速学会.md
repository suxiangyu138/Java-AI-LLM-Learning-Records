# 🔍 快速学会 RAG - 极简核心原理 + 完整流程 + 架构分类 + 代码 Demo

> **核心摘要**：RAG 即为大模型"外挂私有知识库"，让模型学会私有文档、笔记、业务资料，不产生幻觉且知识可实时更新。本文用极简方式整合核心原理、完整流水线、关键技术及 LangChain 可运行 Demo。

**前置阅读**：[[快速学会RAG]] | [[检索增强生成（RAG）核心知识点（快速掌握版）]]

---

## 一、RAG 是什么

**RAG** = Retrieval-Augmented Generation（检索增强生成）。一句话：给大模型"外挂私有知识库"，让模型学会你自己的文档、笔记、业务资料。

| 对比 | 说明 |
|------|------|
| 纯大模型 | 只会训练截止前的公开知识，容易瞎编 |
| RAG 大模型 | 先查私有资料 → 再结合资料回答 |

## 二、为什么必须用 RAG（三大痛点解决）

1. **解决大模型幻觉**：只基于参考资料回答，禁止编造
2. **解决知识滞后**：可随时新增文档、实时更新
3. **解决私有/内网数据**：本地资料不上网也能用

## 三、RAG 标准完整流水线

### 离线预处理（只做一次）

1. **文档加载 Loader**：MD、PDF、TXT、Word、网页
2. **文本切片 Chunk Split**：长文章切成小块
3. **Embedding 向量化**：文字 → 高密度数字向量
4. **存入向量数据库**：Milvus / Chroma / ES

### 在线问答（每问一次执行）

1. 用户问题
2. 问题 Embedding 向量化
3. 向量库相似度检索，召回最相关文档片段
4. 把"召回上下文 + 用户问题"拼接进 Prompt
5. 大模型基于给定资料生成答案
6. 返回结果

## 四、两个核心关键技术

| 技术 | 作用 |
|------|------|
| **Embedding 嵌入** | 把非结构化文本转为多维向量，语义越相似向量距离越近 |
| **向量数据库** | 专门存向量、做高速相似度检索：Chroma（轻量）/ Milvus（生产） |

## 五、RAG 两大架构

| 类型 | 特点 | 适用场景 |
|------|------|----------|
| **基础 RAG**（朴素 RAG） | 直接切块 + 全局检索 + 一次性拼接 | 简单场景、快速验证 |
| **进阶 RAG**（优化方向） | Rerank + 多级检索 + 问句改写 + 摘要压缩 | 企业级高精度问答 |

## 六、RAG vs 微调

| 维度 | RAG | 微调 |
|------|-----|------|
| 知识存储 | 外挂知识库，不改动模型权重 | 修改模型底层参数 |
| 成本 | 低成本、随时改资料 | 高显存、高成本 |
| 幻觉控制 | 可控性强，杜绝幻觉 | 容易过拟合 |

> **重点**：私有知识库一律用 RAG，不用微调。

## 七、RAG 核心 Prompt 模板

```markdown
你是专业知识库问答助手，请严格遵守以下规则：
1. 仅根据下方【参考上下文】回答问题
2. 禁止编造、禁止拓展外部知识
3. 资料不足时，直接回答：「暂无相关资料」

【参考上下文】
{context}

【用户问题】
{question}
```

## 八、最简可运行代码（LangChain + Ollama + Chroma）

```python
from langchain_community.document_loaders import TextLoader
from langchain.text_splitter import CharacterTextSplitter
from langchain_community.embeddings import OllamaEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_openai import ChatOpenAI
from langchain.chains import RetrievalQA

# 1. 本地大模型+Embedding（Ollama离线）
llm = ChatOpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama",
    model="qwen"
)
embedding = OllamaEmbeddings(model="qwen")

# 2. 加载本地文档、切块
loader = TextLoader("test.txt")
docs = loader.load()
splitter = CharacterTextSplitter(chunk_size=300, chunk_overlap=30)
split_docs = splitter.split_documents(docs)

# 3. 向量化存入向量库
vector_db = Chroma.from_documents(split_docs, embedding)
retriever = vector_db.as_retriever()

# 4. 组装RAG问答链
rag_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=retriever,
    return_source_documents=True
)

# 5. 提问
res = rag_chain.invoke("你的问题")
print(res["result"])
```

## 九、极简背诵总结

1. RAG = 检索 + 大模型生成
2. 四步离线：加载 → 切块 → 向量化 → 向量库存储
3. 四步在线：问题向量化 → 语义召回 → 上下文拼接 → LLM回答
4. 核心组件：LLM + Embedding + 向量数据库 + 文档切片
5. 标准技术栈：Ollama（本地模型）+ LangChain + Milvus（RAG向量库）

---

## 核心要点回顾

- RAG 解决三大痛点：幻觉、知识滞后、私有数据
- 两大关键技术：Embedding + 向量数据库
- 进阶 RAG 核心优化：Rerank、多级检索、问句改写
- 标准技术栈：Ollama + LangChain + Milvus

## 参考资料

1. [[快速学会RAG]]
2. [[AI-RAG-BGE-M3+Milvus链路]]
3. [[LangChain文档处理：加载、切分与检索链实战]]
