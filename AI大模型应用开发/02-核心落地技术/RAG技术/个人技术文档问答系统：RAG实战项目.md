# 个人技术文档问答系统：RAG 实战项目

> **所属阶段**：阶段二 — RAG 知识库应用开发
> **技术栈**：Python + LangChain + Chroma + DeepSeek API + Flask
> **项目定位**：企业级 RAG 应用原型，可提交简历

---

## 1. 项目概述

构建一个基于个人技术文档的智能问答系统。上传你的 Java 学习笔记、Spring 官方文档等内容，AI 能基于这些私域知识给出准确答案并标注引用来源。

### 核心功能
- 文档上传与自动解析（PDF、Markdown）
- 语义检索 + 关键词检索混合策略
- 基于上下文的答案生成
- 引用溯源（展示答案来源）

---

## 2. 系统架构

```
┌──────────────────────────────────────────────────────┐
│                   Flask Web 应用                       │
│  ┌──────────┐  ┌───────────┐  ┌──────────────────┐  │
│  │ 文档上传  │  │ 问答接口   │  │ 知识库管理        │  │
│  └────┬─────┘  └─────┬─────┘  └────────┬─────────┘  │
│       │              │                 │             │
│  ┌────┴──────────────┴─────────────────┴──────────┐  │
│  │              RAG Engine                        │  │
│  │  ┌──────────┐  ┌────────┐  ┌──────────────┐   │  │
│  │  │ Doc Proc │→│ Vector │→│   Retriever   │   │  │
│  │  │ (解析切分)│  │ Store  │  │ (混合检索)    │   │  │
│  │  └──────────┘  └────────┘  └──────┬───────┘   │  │
│  └───────────────────────────────────┬───────────┘  │
│                                      │               │
│                              ┌───────┴───────┐       │
│                              │    LLM (API)   │       │
│                              │  DeepSeek/GPT  │       │
│                              └───────────────┘       │
└──────────────────────────────────────────────────────┘
```

---

## 3. 核心实现

### 3.1 文档处理模块

```python
# document_processor.py
from langchain_community.document_loaders import PyPDFLoader, UnstructuredMarkdownLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.schema import Document
import os

class DocumentProcessor:
    """文档加载与预处理"""

    SUPPORTED = {".pdf": PyPDFLoader, ".md": UnstructuredMarkdownLoader}

    def __init__(self, chunk_size=500, chunk_overlap=100):
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", ".", " ", ""]
        )

    def load_file(self, file_path):
        ext = os.path.splitext(file_path)[1].lower()
        loader_cls = self.SUPPORTED.get(ext)
        if not loader_cls:
            raise ValueError(f"Unsupported file type: {ext}")
        loader = loader_cls(file_path)
        return loader.load()

    def process(self, file_paths):
        all_chunks = []
        for path in file_paths:
            try:
                docs = self.load_file(path)
                chunks = self.splitter.split_documents(docs)
                # 注入来源元数据
                for chunk in chunks:
                    chunk.metadata["source_file"] = os.path.basename(path)
                all_chunks.extend(chunks)
                print(f"Processed {path}: {len(chunks)} chunks")
            except Exception as e:
                print(f"Failed to process {path}: {e}")
        return all_chunks
```

### 3.2 RAG 引擎

```python
# rag_engine.py
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_openai import ChatOpenAI
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate

class RAGEngine:
    """RAG 核心引擎"""

    def __init__(self, persist_dir="./chroma_db"):
        self.persist_dir = persist_dir
        self.embeddings = OpenAIEmbeddings(
            model="text-embedding-3-small",
            openai_api_key="sk-xxx",
            openai_api_base="https://api.deepseek.com"  # 或 OpenAI
        )
        self.llm = ChatOpenAI(
            model="deepseek-chat",
            api_key="sk-xxx",
            base_url="https://api.deepseek.com",
            temperature=0.3
        )
        self.vectorstore = None
        self.chain = None

    def build_index(self, documents):
        self.vectorstore = Chroma.from_documents(
            documents=documents,
            embedding=self.embeddings,
            persist_directory=self.persist_dir
        )
        self._create_chain()

    def load_index(self):
        self.vectorstore = Chroma(
            persist_directory=self.persist_dir,
            embedding_function=self.embeddings
        )
        self._create_chain()

    def _create_chain(self):
        retriever = self.vectorstore.as_retriever(
            search_type="mmr",
            search_kwargs={"k": 5, "fetch_k": 20}
        )

        prompt = ChatPromptTemplate.from_messages([
            ("system", """你是 Java 技术专家。请严格根据以下上下文回答问题。

上下文：
{context}

要求：
1. 如果上下文有答案，准确回答并引用来源
2. 如果上下文没有答案，说"根据已有资料无法回答"
3. 回答简洁，200字以内"""),
            ("human", "{input}")
        ])

        combine_chain = create_stuff_documents_chain(self.llm, prompt)
        self.chain = create_retrieval_chain(retriever, combine_chain)

    def query(self, question):
        if not self.chain:
            raise RuntimeError("Index not built. Call build_index() or load_index() first.")
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
from flask import Flask, request, jsonify, render_template
from werkzeug.utils import secure_filename
import os
from document_processor import DocumentProcessor
from rag_engine import RAGEngine

app = Flask(__name__)
app.config["UPLOAD_FOLDER"] = "./uploads"
os.makedirs(app.config["UPLOAD_FOLDER"], exist_ok=True)

processor = DocumentProcessor(chunk_size=500, chunk_overlap=100)
engine = RAGEngine()

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/api/upload", methods=["POST"])
def upload():
    files = request.files.getlist("files")
    saved_paths = []
    for f in files:
        if f.filename:
            path = os.path.join(app.config["UPLOAD_FOLDER"], secure_filename(f.filename))
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
    # 如果已有索引，直接加载
    if os.path.exists("./chroma_db"):
        engine.load_index()
    app.run(debug=True, port=5000)
```

---

## 4. 核心价值

### 4.1 解决大模型幻觉

```
没有 RAG：大模型凭记忆回答 → 可能编造不存在的 API
有 RAG：大模型基于你的文档回答 → 精准、可信
```

### 4.2 引用溯源

每个答案都标注来源文档，用户可验证信息准确性。

---

## 5. 进阶优化

| 优化项 | 方案 | 效果 |
|--------|------|------|
| 检索不准 | 混合检索（BM25 + 语义） | 召回率 +15% |
| 答案不完整 | 增大 k 值 + 摘要 | 覆盖率提升 |
| 文档更新 | 增量索引 + 版本标记 | 支持动态更新 |
| 多格式 | 支持 .docx, .html | 覆盖更多场景 |
| 性能 | 加入 Redis 缓存常见问题 | 响应速度 +80% |
