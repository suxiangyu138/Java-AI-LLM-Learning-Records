# 🔍 RAG 技术全链路实战

> 检索增强生成（RAG）是当前企业级 AI 应用中采用率最高的技术路线。它解决了大模型"幻觉"和"知识滞后"两大核心痛点，且无需重新训练模型——只需搭建外部知识库和检索系统。本文从原理到实践，完整覆盖 RAG 的六大技术环节：文档解析、文本切分、向量化、检索、重排序和生成。

## 前置阅读

- [[检索增强生成（RAG）核心知识点（快速掌握版）]]
- [[RAG（检索增强生成）详细知识点]]
- [[Embedding向量化：从原理到代码实践]]

## 目录

1. [RAG 解决的问题](#1-rag-解决的问题)
2. [RAG 全链路架构](#2-rag-全链路架构)
3. [文档解析：把世界装进模型](#3-文档解析把世界装进模型)
4. [文本切分：分块的艺术](#4-文本切分分块的艺术)
5. [向量化与索引：构建语义地图](#5-向量化与索引构建语义地图)
6. [检索：找到最相关的片段](#6-检索找到最相关的片段)
7. [重排序与生成：精准回答](#7-重排序与生成精准回答)
8. [端到端实战：构建知识库问答系统](#8-端到端实战构建知识库问答系统)

---

## 1. RAG 解决的问题

> **重点**：RAG 的核心价值不在于"让模型变聪明"，而在于"让模型有据可查"。

| 痛点 | 纯 LLM 的表现 | RAG 的解决方案 |
|------|-------------|--------------|
| **幻觉** | 编造不存在的 API、虚构的数据 | 检索到的真实文档作为回答依据 |
| **知识滞后** | 只知道训练截止日期前的信息 | 知识库可实时更新，秒级生效 |
| **领域盲区** | 不了解企业内部制度和流程 | 接入企业内部文档、知识库 |
| **不可追溯** | 无法验证信息来源 | 明确标注每段回答的引用来源 |

---

## 2. RAG 全链路架构

```
┌─────────────────────────────────────────────────────────┐
│                     RAG 全链路                            │
│                                                          │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐             │
│  │ 文档解析  │ → │ 文本切分  │ → │ 向量化   │ → 向量数据库 │
│  │ PDF/Word  │   │ Chunking │   │Embedding │   Milvus/   │
│  │ HTML/MD   │   │ 策略选择  │   │ 模型选型  │   Chroma    │
│  └──────────┘   └──────────┘   └──────────┘             │
│                                                          │
│                      ↓ 在线阶段 ↓                         │
│                                                          │
│  用户提问 → 问题向量化 → 向量检索 → 重排序 → LLM 生成     │
│              Embedding    top-K     Rerank   + 检索结果   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**离线阶段**（索引构建）和**在线阶段**（检索增强）是两个独立的流程。离线阶段可以批量处理、定期更新；在线阶段要求低延迟、高吞吐。

---

## 3. 文档解析：把世界装进模型

### 3.1 不同文档格式的处理策略

| 文档类型 | 解析工具 | 注意事项 |
|----------|---------|---------|
| **纯文本/Markdown** | 直接读取 | 保留标题层级信息 |
| **PDF** | PyMuPDF / pdfplumber | 注意表格和图片中的文字提取 |
| **Word (.docx)** | python-docx | 保留段落样式（标题 vs 正文） |
| **HTML** | BeautifulSoup | 去除 script/style 标签，保留主体内容 |
| **代码** | 自定义解析 | 按函数/类拆分，保留 import 语句 |

### 3.2 文档解析实战

```python
from typing import List, Dict
import fitz  # PyMuPDF

class DocumentParser:
    """多格式文档解析器"""

    def parse_pdf(self, file_path: str) -> List[Dict]:
        """解析 PDF 文档，保留页面和结构信息"""
        doc = fitz.open(file_path)
        chunks = []

        for page_num, page in enumerate(doc):
            text = page.get_text()
            if text.strip():
                chunks.append({
                    "content": text,
                    "metadata": {
                        "source": file_path,
                        "page": page_num + 1,
                        "type": "pdf"
                    }
                })

        return chunks

    def parse_markdown(self, file_path: str) -> List[Dict]:
        """解析 Markdown，按标题层级分段"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # 按 ## 标题分割
        import re
        sections = re.split(r'\n(?=## )', content)

        chunks = []
        for i, section in enumerate(sections):
            if section.strip():
                # 提取标题作为元数据
                title_match = re.match(r'^#+\s+(.+)', section)
                title = title_match.group(1) if title_match else ""

                chunks.append({
                    "content": section.strip(),
                    "metadata": {
                        "source": file_path,
                        "section": i + 1,
                        "title": title,
                        "type": "markdown"
                    }
                })

        return chunks
```

---

## 4. 文本切分：分块的艺术

### 4.1 为什么需要切分

- **向量模型有输入长度限制**（如 BGE 模型最大 512 token）
- **太长**的 chunk 会稀释语义信息，检索精度下降
- **太短**的 chunk 缺少上下文，LLM 无法生成完整回答

### 4.2 三种切分策略对比

| 策略 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **固定长度** | 按 token 数等分 | 简单高效 | 可能在句子中间截断 |
| **语义切分** | 按句子/段落边界切分 | 语义完整 | 块大小不均匀 |
| **递归切分** | 按分隔符优先级逐级切分 | 平衡效率和质量 | 参数调优复杂 |

### 4.3 递归切分实战

```python
from langchain.text_splitter import RecursiveCharacterTextSplitter

def create_chunker(chunk_size: int = 500, chunk_overlap: int = 50):
    """创建智能切分器"""
    return RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,     # 每个块的最大字符数
        chunk_overlap=chunk_overlap,  # 相邻块的重叠字符数
        separators=[
            "\n## ",   # Markdown H2
            "\n### ",  # Markdown H3
            "\n\n",    # 段落分隔
            "\n",      # 行分隔
            "。",      # 中文句号
            ". ",      # 英文句号
            " ",       # 空格
            ""         # 字符级（最后手段）
        ],
        length_function=len,
    )
```

> **重点**：`chunk_overlap` 是关键参数。适当的重叠（10-15% 的 chunk_size）可以确保相邻块之间的信息不丢失，显著提升检索召回率。

### 4.4 不同场景的推荐配置

| 场景 | chunk_size | chunk_overlap | 理由 |
|------|-----------|---------------|------|
| FAQ 问答 | 200-300 | 30-50 | 问题答案通常简短 |
| 技术文档 | 500-800 | 80-120 | 需要完整解释概念 |
| 法律合同 | 1000-1500 | 150-200 | 条款之间关联紧密 |
| 代码库 | 按函数/类 | 0 | 代码结构本身就是天然边界 |

---

## 5. 向量化与索引：构建语义地图

### 5.1 Embedding 模型选型

| 模型 | 维度 | 最大长度 | 中文能力 | 推荐场景 |
|------|------|---------|---------|---------|
| **BGE-M3** | 1024 | 8192 | ⭐⭐⭐⭐⭐ | 中文 RAG 首选 |
| **BGE-large-zh** | 1024 | 512 | ⭐⭐⭐⭐⭐ | 中文短文本 |
| **text2vec-large-chinese** | 1024 | 512 | ⭐⭐⭐⭐ | 中文通用 |
| **text-embedding-3-large** | 3072 | 8191 | ⭐⭐⭐ | 多语言（API） |
| **all-MiniLM-L6-v2** | 384 | 256 | ⭐⭐ | 英文轻量级 |

### 5.2 向量化与存储

```python
from sentence_transformers import SentenceTransformer
import chromadb

class VectorIndexBuilder:
    """向量索引构建器"""

    def __init__(self, model_name: str = "BAAI/bge-m3"):
        self.model = SentenceTransformer(model_name)
        self.client = chromadb.PersistentClient(path="./chroma_db")
        self.collection = self.client.get_or_create_collection("knowledge_base")

    def index_documents(self, chunks: list):
        """将文档块向量化并存入向量数据库"""
        for i, chunk in enumerate(chunks):
            # 生成向量
            embedding = self.model.encode(
                chunk["content"],
                normalize_embeddings=True  # 归一化，方便余弦相似度计算
            )

            # 存入 Chroma
            self.collection.add(
                ids=[f"doc_{i}"],
                embeddings=[embedding.tolist()],
                documents=[chunk["content"]],
                metadatas=[chunk["metadata"]]
            )

        print(f"✅ 已索引 {len(chunks)} 个文档块")

    def search(self, query: str, top_k: int = 5) -> list:
        """语义检索"""
        query_embedding = self.model.encode(query, normalize_embeddings=True)

        results = self.collection.query(
            query_embeddings=[query_embedding.tolist()],
            n_results=top_k,
            include=["documents", "metadatas", "distances"]
        )

        return results
```

---

## 6. 检索：找到最相关的片段

### 6.1 混合检索策略

单一检索方式存在盲区。**混合检索 = 语义检索 + 关键词检索** 是工业界标准做法。

```
最终分数 = α × 语义相似度 + (1-α) × BM25 关键词分数

α = 0.7（偏重语义）：适用于自然语言提问
α = 0.3（偏重关键词）：适用于搜索特定术语
```

### 6.2 检索优化技巧

| 技巧 | 说明 | 效果 |
|------|------|------|
| **Query 重写** | 用 LLM 将用户的模糊提问改写为精确检索语句 | +15% 召回率 |
| **HyDE** | 先生成假设性答案，用答案向量去检索 | +20%（特定场景） |
| **多路召回** | 同时用多个 query 变体检索，合并去重 | +25% 召回率 |
| **父子文档** | 检索小 chunk，返回大 chunk 的上下文 | 平衡精度和上下文 |

---

## 7. 重排序与生成：精准回答

### 7.1 为什么需要重排序

向量检索返回 top-K 个结果，但这些结果的排序不一定最优。**Reranker**（重排序模型）用更强的交叉编码器对 top-K 结果重新打分，大幅提升最相关文档的排位。

```python
from FlagEmbedding import FlagReranker

reranker = FlagReranker('BAAI/bge-reranker-v2-m3')

def rerank(query: str, documents: list) -> list:
    """重排序检索结果"""
    pairs = [[query, doc] for doc in documents]
    scores = reranker.compute_score(pairs)

    # 按分数降序排列
    ranked = sorted(
        zip(documents, scores),
        key=lambda x: x[1],
        reverse=True
    )
    return ranked
```

### 7.2 生成 Prompt 模板

```python
RAG_PROMPT_TEMPLATE = """
你是一个基于给定资料回答问题的助手。
请严格根据以下参考资料回答问题。如果资料中没有相关信息，请明确说"根据已有资料，我无法回答这个问题"。

## 参考资料
{context}

## 用户问题
{question}

## 要求
1. 引用资料中的具体内容作为依据
2. 如果资料之间存在矛盾，指出矛盾点
3. 在回答末尾列出引用的资料来源（文档名 + 页码/段落）
"""
```

---

## 8. 端到端实战：构建知识库问答系统

```python
class RAGPipeline:
    """RAG 完整流水线"""

    def __init__(self):
        self.parser = DocumentParser()
        self.splitter = create_chunker(chunk_size=500, chunk_overlap=50)
        self.indexer = VectorIndexBuilder()
        self.reranker = FlagReranker('BAAI/bge-reranker-v2-m3')

    def build_index(self, file_paths: list):
        """离线阶段：构建知识库索引"""
        all_chunks = []

        for path in file_paths:
            # 1. 解析文档
            if path.endswith('.pdf'):
                chunks = self.parser.parse_pdf(path)
            elif path.endswith('.md'):
                chunks = self.parser.parse_markdown(path)

            # 2. 文本切分
            for chunk in chunks:
                sub_chunks = self.splitter.split_text(chunk["content"])
                for sc in sub_chunks:
                    all_chunks.append({
                        "content": sc,
                        "metadata": chunk["metadata"]
                    })

        # 3. 向量化 + 存储
        self.indexer.index_documents(all_chunks)
        return len(all_chunks)

    def query(self, question: str, top_k: int = 10) -> str:
        """在线阶段：检索增强生成"""
        # 1. 检索
        raw_results = self.indexer.search(question, top_k=top_k)

        # 2. 重排序
        documents = raw_results['documents'][0]
        ranked = self.rerank(question, documents)

        # 3. 取 top-5 最有用的结果
        top_docs = [doc for doc, score in ranked[:5] if score > 0.3]
        context = "\n\n---\n\n".join(top_docs)

        # 4. 生成
        prompt = RAG_PROMPT_TEMPLATE.format(
            context=context,
            question=question
        )

        # 调用 LLM（这里用伪代码示意）
        # answer = llm.chat(prompt)
        return prompt  # 实际应返回 LLM 生成结果


# ========== 使用示例 ==========
if __name__ == "__main__":
    rag = RAGPipeline()

    # 离线：构建索引
    doc_count = rag.build_index([
        "./docs/技术文档.md",
        "./docs/API手册.pdf",
        "./docs/FAQ.md"
    ])
    print(f"✅ 已索引 {doc_count} 个文档块")

    # 在线：问答
    answer = rag.query("如何配置数据库连接池？")
    print(answer)
```

---

## 核心要点回顾

- RAG = **文档解析 → 切分 → 向量化 → 检索 → 重排序 → 生成**，六大环节环环相扣
- 切分策略是 RAG 质量的**第一决定因素**：chunk_size 和 chunk_overlap 需要按场景调优
- 混合检索（语义 + 关键词）是工业界标准，比单纯向量检索召回率高 20-30%
- **重排序**是投入产出比最高的优化手段：用 Reranker 二次打分，检索精度提升显著
- 生成 Prompt 必须明确要求模型**引用来源**，这是 RAG 可追溯性的核心保障

## 参考资料

1. [[检索增强生成（RAG）核心知识点（快速掌握版）]]
2. [[RAG（检索增强生成）详细知识点]]
3. [[快速学会RAG]]
4. [[RAG混合检索与Rerank优化]]
5. [[AI-RAG-BGE-M3+Milvus链路]]
6. [[文档处理与解析实战]]
7. [[个人技术文档问答系统：RAG实战项目]]
