# 🔍 BGE-M3 + Milvus 完整 RAG 链路

> **核心摘要**：本文演示 BGE-M3 嵌入模型 + Milvus 向量数据库的端到端 RAG 部署方案，包含环境准备、完整可运行代码和链路拆解，是本地 RAG 开发的标准参考配置。

**前置阅读**：[[AI-RAG-快速学会]] | [[RAG混合检索与Rerank优化]]

---

## 环境准备

### 1. 安装 Ollama 并拉取 BGE-M3

```bash
ollama pull bge-m3
```

### 2. Docker 启动 Milvus

```bash
docker run -d -p 19530:19530 --name milvus milvusdb/milvus:latest
```

### 3. 安装依赖

```bash
pip install langchain langchain-community pymilvus
```

## 完整可运行代码

```python
from langchain_community.embeddings import OllamaEmbeddings
from langchain_community.vectorstores import Milvus
from langchain.text_splitter import CharacterTextSplitter

# 1. 初始化本地 Embedding 模型 BGE-M3
embedding = OllamaEmbeddings(
    model="bge-m3",
    base_url="http://localhost:11434"
)

# 2. 测试文档
docs_content = [
    "RabbitMQ 是一款基于 AMQP 协议的消息队列，用于异步解耦、削峰填谷。",
    "Elasticsearch 基于倒排索引，擅长全文检索与海量数据模糊查询。",
    "Docker 可以快速部署中间件，隔离环境，解决软件冲突问题。"
]

# 3. 文本切块
splitter = CharacterTextSplitter(chunk_size=200, chunk_overlap=20)
split_docs = splitter.create_documents(docs_content)

# 4. 连接 Milvus 并写入向量
vector_db = Milvus.from_documents(
    documents=split_docs,
    embedding=embedding,
    connection_args={"host": "localhost", "port": "19530"},
    collection_name="ai_knowledge",
    drop_old=True
)

# 5. 语义检索
query = "消息队列有什么作用"
res = vector_db.similarity_search(query, k=2)

for idx, doc in enumerate(res):
    print(f"【结果{idx+1}】{doc.page_content}")
```

## 链路拆解

| 环节 | 组件 | 作用 |
|------|------|------|
| Embedding 模型 | BGE-M3 | 文本 → 多维语义向量 |
| 切块 | CharacterTextSplitter | 防止文本过长、向量精度下降 |
| 向量数据库 | Milvus | 存储向量 + 原始文本，持久化 |
| 相似度检索 | Milvus 内置 | 余弦距离计算，召回语义匹配内容 |

## 扩展为完整 RAG 问答

```python
from langchain_openai import ChatOpenAI
from langchain.chains import RetrievalQA

llm = ChatOpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama",
    model="qwen"
)

rag_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=vector_db.as_retriever(k=2)
)

answer = rag_chain.invoke("消息队列有什么作用")
print("\n【RAG最终回答】")
print(answer["result"])
```

## 技术栈闭环汇总

| 层级 | 选型 |
|------|------|
| 大模型 | Ollama（Qwen/DeepSeek） |
| 嵌入模型 | BGE-M3 |
| 向量库 | Milvus |
| 框架 | LangChain |
| 增强方案 | RAG |
| 高级能力 | Agent + MCP + 自定义 Skill |

---

## 核心要点回顾

- BGE-M3 + Milvus 是本地 RAG 开发的标准技术组合
- 核心流程：文本 → 切块 → BGE-M3 嵌入 → Milvus 存储 → 语义检索
- 无缝对接 LangChain RetrievalQA 实现完整 RAG 问答
- 整套技术栈可离线、私有化部署

## 参考资料

1. [[RAG混合检索与Rerank优化]]
2. [[LangChain文档处理：加载、切分与检索链实战]]
3. [[基于大模型的RAG应用开发与优化——数据嵌入与索引核心知识点大全]]
