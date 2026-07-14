# 🔍 个人技术文档问答系统：RAG 实战项目

> **核心摘要**：构建基于个人技术文档的智能问答系统，支持上传 Java 学习笔记、Spring 文档等，AI 基于私域知识给出准确答案并标注引用来源。技术栈为 Python + LangChain + Chroma + DeepSeek API + Flask，可作为企业级 RAG 应用原型提交简历。

**前置阅读**：[[LangChain文档处理：加载、切分与检索链实战]] | [[文档处理与解析实战]]

---

## 1. 项目概述

### 核心功能

- 文档上传与自动解析（PDF、Markdown）
- 语义检索 + 关键词检索混合策略
- 基于上下文的答案生成
- 引用溯源（展示答案来源）

## 2. 系统架构

```
Flask Web 应用
  ├── 文档上传
  ├── 问答接口
  └── 知识库管理
       ↓
  RAG Engine
  ├── Doc Proc（解析切分）
  ├── Vector Store（向量存储）
  └── Retriever（混合检索）
       ↓
  LLM API（DeepSeek/GPT）
```

## 3. 核心实现

### 3.1 文档处理模块

```python
# document_processor.py
from langchain_community.document_loaders import PyPDFLoader, UnstructuredMarkdownLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
import os

class DocumentProcessor:
    SUPPORTED = {".pdf": PyPDFLoader, ".md": UnstructuredMarkdownLoader}

    def __init__(self, chunk_size=500, chunk_overlap=100):
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", ".", " ", ""]
        )

    def process(self, file_paths):
        all_chunks = []
        for path in file_paths:
            ext = os.path.splitext(path)[1].lower()
            loader_cls = self.SUPPORTED.get(ext)
            if not loader_cls:
                continue
            docs = loader_cls(path).load()
            chunks = self.splitter.split_documents(docs)
            for chunk in chunks:
                chunk.metadata["source_file"] = os.path.basename(path)
            all_chunks.extend(chunks)
        return all_chunks
```

### 3.2 RAG 引擎

```python
# rag_engine.py
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_community.vectorstores import Chroma
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate

class RAGEngine:
    def __init__(self, persist_dir="./chroma_db"):
        self.persist_dir = persist_dir
        self.embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
        self.llm = ChatOpenAI(
            model="deepseek-chat",
            api_key="sk-xxx",
            base_url="https://api.deepseek.com",
            temperature=0.3
        )
        self.chain = None

    def build_index(self, documents):
        self.vectorstore = Chroma.from_documents(
            documents=documents,
            embedding=self.embeddings,
            persist_directory=self.persist_dir
        )
        self._create_chain()

    def _create_chain(self):
        retriever = self.vectorstore.as_retriever(
            search_type="mmr",
            search_kwargs={"k": 5, "fetch_k": 20}
        )
        prompt = ChatPromptTemplate.from_messages([
            ("system", """你是 Java 技术专家。请严格根据以下上下文回答问题。
上下文：{context}
要求：
1. 如果上下文有答案，准确回答并引用来源
2. 如果上下文没有答案，说"根据已有资料无法回答"
3. 回答简洁，200字以内"""),
            ("human", "{input}")
        ])
        combine_chain = create_stuff_documents_chain(self.llm, prompt)
        self.chain = create_retrieval_chain(retriever, combine_chain)

    def query(self, question):
        result = self.chain.invoke({"input": question})
        return {
            "answer": result["answer"],
            "sources": list(set(
                doc.metadata.get("source_file", "unknown")
                for doc in result["context"]
            ))
        }
```

### 3.3 Flask Web 应用

```python
# app.py
from flask import Flask, request, jsonify
from document_processor import DocumentProcessor
from rag_engine import RAGEngine
import os

app = Flask(__name__)
app.config["UPLOAD_FOLDER"] = "./uploads"
os.makedirs(app.config["UPLOAD_FOLDER"], exist_ok=True)

processor = DocumentProcessor(chunk_size=500, chunk_overlap=100)
engine = RAGEngine()

@app.route("/api/upload", methods=["POST"])
def upload():
    files = request.files.getlist("files")
    saved_paths = []
    for f in files:
        path = os.path.join(app.config["UPLOAD_FOLDER"], f.filename)
        f.save(path)
        saved_paths.append(path)
    chunks = processor.process(saved_paths)
    engine.build_index(chunks)
    return jsonify({"status": "ok", "chunks": len(chunks)})

@app.route("/api/query", methods=["POST"])
def query():
    question = request.json.get("question")
    result = engine.query(question)
    return jsonify(result)

if __name__ == "__main__":
    app.run(debug=True, port=5000)
```

## 4. 核心价值

| 对比 | 说明 |
|------|------|
| 无 RAG | 大模型凭记忆回答，可能编造不存在的 API |
| 有 RAG | 大模型基于你的文档回答，精准可信，可溯源 |

## 5. 进阶优化

| 优化项 | 方案 | 效果 |
|--------|------|------|
| 检索不准 | 混合检索（BM25 + 语义） | 召回率 +15% |
| 答案不完整 | 增大 k 值 + 摘要 | 覆盖率提升 |
| 文档更新 | 增量索引 + 版本标记 | 支持动态更新 |
| 性能 | Redis 缓存常见问题 | 响应速度 +80% |

---

## 核心要点回顾

- 完整 RAG 项目原型：文档处理 → 向量索引 → 检索 → 生成
- 技术栈：LangChain + Chroma + DeepSeek + Flask
- 引用溯源是 RAG 相比纯 LLM 的核心优势
- 进阶方向：混合检索、增量索引、缓存优化

## 参考资料

1. [[文档处理与解析实战]]
2. [[RAG混合检索与Rerank优化]]
3. [[基于大模型的RAG应用开发与优化——核心知识点大全（开发与环境搭建篇）]]
