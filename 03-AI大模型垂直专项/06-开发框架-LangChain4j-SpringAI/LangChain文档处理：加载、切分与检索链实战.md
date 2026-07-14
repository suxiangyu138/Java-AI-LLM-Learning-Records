# ⛓️ LangChain 文档处理：加载、切分与检索链实战

> **核心摘要**：LangChain 提供了完整的文档处理管线——从加载、切分到向量化存储和检索问答。本文通过可运行代码演示 Document Loaders、Text Splitters 及完整 RAG 管道的搭建，覆盖常见文档类型和高级检索策略（MMR、混合检索）。

**前置阅读**：[[快速吃透 LangChain]] | [[文档处理与解析实战]]

---

## 1. LangChain 核心概念

```
Document → Text Splitter → Embeddings → VectorStore → Retriever → Chain → LLM
```

### 安装

```bash
pip install langchain langchain-community langchain-openai chromadb
```

## 2. Document Loaders（文档加载）

### 2.1 常见文档类型

```python
from langchain_community.document_loaders import (
    TextLoader,           # .txt
    PyPDFLoader,          # .pdf
    CSVLoader,            # .csv
    UnstructuredMarkdownLoader,  # .md
    WebBaseLoader,        # 网页
    DirectoryLoader,      # 批量加载目录
)

# 加载 PDF
loader = PyPDFLoader("spring-framework.pdf")
pages = loader.load()  # List[Document]

# 批量加载目录
loader = DirectoryLoader("./java_notes/", glob="**/*.md",
                         loader_cls=UnstructuredMarkdownLoader)
docs = loader.load()
```

### 2.2 统一文档模型

```python
from langchain.schema import Document

doc = Document(
    page_content="Spring IoC 容器负责管理 Bean...",
    metadata={"source": "spring-core.md", "page": 1, "topic": "Spring"}
)
```

## 3. Text Splitters（文本切分）

```python
from langchain.text_splitter import (
    RecursiveCharacterTextSplitter,
    MarkdownHeaderTextSplitter,
    TokenTextSplitter,
)

# 递归字符切分（通用推荐）
splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=100,
    separators=["\n\n", "\n", "。", ".", " ", ""]
)
chunks = splitter.split_documents(docs)

# Markdown 按标题切分
markdown_splitter = MarkdownHeaderTextSplitter(
    headers_to_split_on=[("#", "h1"), ("##", "h2"), ("###", "h3")]
)

# 代码专用切分
java_splitter = RecursiveCharacterTextSplitter.from_language(
    language=Language.JAVA, chunk_size=500, chunk_overlap=50
)
```

### 切分原则

| 原则 | 说明 |
|------|------|
| 保持语义完整性 | 不要在句子中间切断 |
| 适当重叠 | 10-20% 重叠避免边界信息丢失 |
| 元数据继承 | 切分后的 chunk 保留源文档元数据 |
| 大小适中 | 中文 300-800 字，英文 500-1000 tokens |

## 4. 完整 RAG 管道

```python
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_community.vectorstores import Chroma
from langchain.chains import RetrievalQA
from langchain.text_splitter import RecursiveCharacterTextSplitter

# 1. 加载文档
loader = DirectoryLoader("./java_notes/", glob="**/*.md")
docs = loader.load()

# 2. 切分
splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=100)
chunks = splitter.split_documents(docs)

# 3. Embedding + 存储
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Chroma.from_documents(
    documents=chunks, embedding=embeddings, persist_directory="./chroma_db"
)

# 4. 创建检索器
retriever = vectorstore.as_retriever(
    search_type="similarity", search_kwargs={"k": 4}
)

# 5. 创建 RAG 链
llm = ChatOpenAI(model="gpt-4o", temperature=0)
qa_chain = RetrievalQA.from_chain_type(
    llm=llm, retriever=retriever, return_source_documents=True
)

# 6. 提问
result = qa_chain.invoke({"query": "Spring Boot 如何实现自动配置？"})
print(result["result"])
```

## 5. 高级检索策略

### 5.1 MMR（最大边际相关性）

```python
retriever = vectorstore.as_retriever(
    search_type="mmr",
    search_kwargs={"k": 4, "fetch_k": 20, "lambda_mult": 0.5}
)
```

### 5.2 混合检索

```python
from langchain.retrievers import EnsembleRetriever
from langchain_community.retrievers import BM25Retriever

semantic_retriever = vectorstore.as_retriever(search_kwargs={"k": 4})
bm25_retriever = BM25Retriever.from_documents(chunks)
bm25_retriever.k = 4

ensemble_retriever = EnsembleRetriever(
    retrievers=[semantic_retriever, bm25_retriever],
    weights=[0.7, 0.3]
)
```

### 5.3 自定义检索链

```python
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate

prompt = ChatPromptTemplate.from_messages([
    ("system", """请根据以下上下文回答问题。如果上下文中没有答案，请如实说不知道。
    上下文：{context}"""),
    ("human", "{input}")
])

combine_docs_chain = create_stuff_documents_chain(llm, prompt)
rag_chain = create_retrieval_chain(retriever, combine_docs_chain)
result = rag_chain.invoke({"input": "什么是循环依赖？"})
```

## 6. 常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| 检索结果不相关 | chunk 太大或太小 | 调整 chunk_size 到 300-800 |
| 答案不完整 | 上下文被截断 | 增大 k 值 |
| 答案错误 | 检索到不相关内容 | 使用 MMR 或提高相似度阈值 |
| 速度慢 | 向量数据库未优化 | 使用 FAISS + IVF 索引 |

---

## 核心要点回顾

- LangChain 文档处理管线：Loader → Splitter → Embedding → VectorStore → Retriever → Chain
- 切分原则：语义完整、适当重叠（10-20%）、大小适中
- 高级检索：MMR（多样性）、混合检索（语义+关键词）
- 标准 RAG 链：RetrievalQA 或自定义 create_retrieval_chain

## 参考资料

1. [[快速吃透 LangChain]]
2. [[文档处理与解析实战]]
3. [[RAG混合检索与Rerank优化]]
