# LangChain 文档处理：加载、切分与检索链实战

> **所属阶段**：阶段二 — RAG 知识库应用开发
> **前置知识**：Python 基础、Embedding
> **核心目标**：用 LangChain 快速搭建 RAG 管道

---

## 1. LangChain 核心概念

LangChain 是构建 LLM 应用的标准框架，核心抽象：

```
Document → Text Splitter → Embeddings → VectorStore → Retriever → Chain → LLM
```

### 安装

```bash
pip install langchain langchain-community langchain-openai chromadb
```

---

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
from langchain_community.document_loaders import PyPDFLoader
loader = PyPDFLoader("spring-framework.pdf")
pages = loader.load()  # List[Document]
print(pages[0].page_content[:200])
print(pages[0].metadata)  # {'source': '...', 'page': 1}

# 加载 Markdown
from langchain_community.document_loaders import UnstructuredMarkdownLoader
loader = UnstructuredMarkdownLoader("notes/spring-ioc.md")
docs = loader.load()

# 批量加载目录
from langchain_community.document_loaders import DirectoryLoader
loader = DirectoryLoader(
    "./java_notes/",
    glob="**/*.md",
    loader_cls=UnstructuredMarkdownLoader
)
docs = loader.load()
print(f"Loaded {len(docs)} documents")
```

### 2.2 统一文档模型

```python
from langchain.schema import Document

# LangChain 的 Document 对象
doc = Document(
    page_content="Spring IoC 容器负责管理 Bean...",
    metadata={
        "source": "spring-core.md",
        "page": 1,
        "topic": "Spring"
    }
)
```

---

## 3. Text Splitters（文本切分）

### 3.1 为什么需要切分

LLM 有上下文窗口限制，Embedding 模型也有最大 token 限制。切分策略直接影响检索质量。

### 3.2 切分策略

```python
from langchain.text_splitter import (
    RecursiveCharacterTextSplitter,  # 递归切分（最常用）
    MarkdownHeaderTextSplitter,      # 按 Markdown 标题切分
    TokenTextSplitter,               # 按 Token 数切分
)

# 方法一：递归字符切分（通用，推荐）
splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,       # 每块最大字符数
    chunk_overlap=100,    # 重叠字符数（保持上下文连贯）
    separators=["\n\n", "\n", "。", ".", " ", ""]  # 优先按段落切
)
chunks = splitter.split_documents(docs)

# 方法二：按标题切分（适合 Markdown 文档）
headers_to_split_on = [
    ("#", "h1"),
    ("##", "h2"),
    ("###", "h3"),
]
markdown_splitter = MarkdownHeaderTextSplitter(
    headers_to_split_on=headers_to_split_on
)
chunks = markdown_splitter.split_text(markdown_content)

# 方法三：代码专用切分
from langchain.text_splitter import Language, RecursiveCharacterTextSplitter
java_splitter = RecursiveCharacterTextSplitter.from_language(
    language=Language.JAVA,
    chunk_size=500,
    chunk_overlap=50
)
```

### 3.3 切分原则

| 原则 | 说明 |
|------|------|
| 保持语义完整性 | 不要在句子中间切断 |
| 适当重叠 | 10-20% 重叠避免边界信息丢失 |
| 元数据继承 | 切分后的 chunk 保留源文档元数据 |
| 大小适中 | 中文 300-800 字，英文 500-1000 tokens |

---

## 4. 完整 RAG 管道

```python
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.chains import RetrievalQA
from langchain_openai import ChatOpenAI

# 1. 加载文档
loader = DirectoryLoader("./java_notes/", glob="**/*.md")
docs = loader.load()

# 2. 切分
splitter = RecursiveCharacterTextSplitter(
    chunk_size=500, chunk_overlap=100
)
chunks = splitter.split_documents(docs)

# 3. Embedding + 存储到向量数据库
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Chroma.from_documents(
    documents=chunks,
    embedding=embeddings,
    persist_directory="./chroma_db"
)

# 4. 创建检索器
retriever = vectorstore.as_retriever(
    search_type="similarity",  # 或 "mmr" 最大边际相关性
    search_kwargs={"k": 4}
)

# 5. 创建 RAG 链
llm = ChatOpenAI(model="gpt-4o", temperature=0)
qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=retriever,
    return_source_documents=True  # 返回引用来源
)

# 6. 提问
result = qa_chain.invoke({"query": "Spring Boot 如何实现自动配置？"})
print(result["result"])                        # 答案
for doc in result["source_documents"]:         # 引用来源
    print(f"来源: {doc.metadata['source']}")
```

---

## 5. 高级检索策略

### 5.1 MMR（最大边际相关性）

```python
# 平衡相关性与多样性，避免检索到高度相似的冗余文档
retriever = vectorstore.as_retriever(
    search_type="mmr",
    search_kwargs={"k": 4, "fetch_k": 20, "lambda_mult": 0.5}
)
```

### 5.2 混合检索

```python
from langchain.retrievers import EnsembleRetriever
from langchain_community.retrievers import BM25Retriever

# 语义检索器
semantic_retriever = vectorstore.as_retriever(search_kwargs={"k": 4})

# 关键词检索器
bm25_retriever = BM25Retriever.from_documents(chunks)
bm25_retriever.k = 4

# 混合：融合两种检索结果
ensemble_retriever = EnsembleRetriever(
    retrievers=[semantic_retriever, bm25_retriever],
    weights=[0.7, 0.3]  # 语义权重更大
)
```

### 5.3 自定义检索链

```python
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate

prompt = ChatPromptTemplate.from_messages([
    ("system", """你是一位 Java 技术专家。
    请根据以下上下文回答问题。如果上下文中没有答案，请如实说不知道。
    
    上下文：
    {context}"""),
    ("human", "{input}")
])

combine_docs_chain = create_stuff_documents_chain(llm, prompt)
rag_chain = create_retrieval_chain(retriever, combine_docs_chain)

result = rag_chain.invoke({"input": "什么是循环依赖？"})
```

---

## 6. 常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| 检索结果不相关 | chunk 太大或太小 | 调整 chunk_size 到 300-800 |
| 答案不完整 | 上下文被截断 | 增大 k 值，检索更多文档 |
| 答案错误 | 检索到了不相关内容 | 使用 MMR 或提高相似度阈值 |
| 速度慢 | 向量数据库未优化 | 使用 FAISS + IVF 索引 |
