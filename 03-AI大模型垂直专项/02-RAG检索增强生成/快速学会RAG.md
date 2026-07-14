# 🔍 快速学会 RAG

> **核心摘要**：RAG = "先检索私有知识库，再让大模型基于这些内容回答"，工程上即"索引 → 检索 → 生成"三段流水线。本文提供理论 + 实战速通版，包含 Python 最小 Demo 和 Java 后端实现思路。

**前置阅读**：[[AI-RAG-快速学会]] | [[检索增强生成（RAG）核心知识点（快速掌握版）]]

---

## 1. 概念速通

**RAG**（Retrieval-Augmented Generation，检索增强生成）的核心流程抽象为 4 步：

1. **预处理和索引（离线）**
   - 文档加载：PDF、Markdown、数据库记录 → 纯文本
   - 文本切分（Chunking）：按段落/长度切成小块（300-500 字），适当重叠
   - 向量化（Embedding）：用嵌入模型将文本块转为高维向量
   - 向量存储：存入向量数据库（FAISS、Milvus、pgvector）

2. **在线检索（R）**
   - 用户提问 → 用同一 embedding 模型转为向量
   - 在向量库做相似度搜索，拿到 top-K 最相关 chunk

3. **生成增强（A+G）**
   - 将 chunk 拼接为 context，连同原问题传给大模型
   - 大模型基于检索文本生成回答，减少幻觉

4. **优化点**
   - 更好的 chunk 策略（按语义/标题/代码块切）
   - Hybrid 检索：向量检索 + 关键字检索混合
   - Rerank：先粗召回再用 cross-encoder 重新排序

> **重点**：传统 ChatGPT 只靠"训练时记忆"，RAG 带上了"实时查资料再作答"，适合做企业知识库问答、代码库问答。

## 2. 最小可用 RAG Demo（Python 端到端）

依赖安装：

```bash
pip install sentence-transformers faiss-cpu openai
```

核心代码：

```python
import os
import faiss
import numpy as np
from sentence_transformers import SentenceTransformer
from openai import OpenAI

model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"), base_url="https://api.openai.com/v1")

def load_docs(dir_path):
    docs = []
    for name in os.listdir(dir_path):
        p = os.path.join(dir_path, name)
        if os.path.isfile(p) and p.endswith(".txt"):
            with open(p, "r", encoding="utf-8") as f:
                docs.append(f.read())
    return docs

def chunk_text(text, size=300, overlap=50):
    chunks = []
    start = 0
    while start < len(text):
        end = start + size
        chunks.append(text[start:end])
        start = end - overlap
    return chunks

def build_index(docs):
    chunks = []
    for d in docs:
        chunks.extend(chunk_text(d))
    embeddings = model.encode(chunks, normalize_embeddings=True)
    dim = embeddings.shape[1]
    index = faiss.IndexFlatIP(dim)
    index.add(embeddings.astype("float32"))
    return index, chunks, embeddings

def search(index, query, chunks, top_k=5):
    q_emb = model.encode([query], normalize_embeddings=True)
    D, I = index.search(np.array(q_emb, dtype="float32"), top_k)
    results = [chunks[i] for i in I[0]]
    return results

def build_prompt(query, contexts):
    context_text = "\n\n".join(contexts)
    prompt = f"你是一个严谨的知识助手。请只根据以下提供的资料回答问题，不能从资料外编造信息。\n\n资料：\n{context_text}\n\n问题：{query}\n\n请用中文给出清晰、结构化的回答。"
    return prompt

def ask_llm(prompt):
    completion = client.chat.completions.create(
        model="gpt-4.1-mini",
        messages=[{"role": "user", "content": prompt}],
        temperature=0.1
    )
    return completion.choices[0].message.content

if __name__ == "__main__":
    docs = load_docs("docs")
    index, chunks, _ = build_index(docs)
    while True:
        q = input("问题：")
        if q.strip() == "":
            break
        ctx = search(index, q, chunks, top_k=5)
        prompt = build_prompt(q, ctx)
        answer = ask_llm(prompt)
        print("回答：", answer)
```

## 3. 关键知识点清单

1. **为什么需要 RAG**
   - 解决大模型知识过期、不了解私有数据的问题
   - 减少幻觉，让回答"有据可依"

2. **RAG 典型架构组件**
   - 文档源：文件系统、数据库、Git 仓库
   - 文本处理：清洗、分词、去噪、chunking
   - 向量化：Embedding 模型（开源或闭源）
   - 向量数据库：FAISS、Milvus、Elasticsearch、pgvector
   - 检索层：向量相似度搜索、过滤、排序、Rerank
   - LLM 层：大模型调用 + Prompt 模板 + 输出格式治理

3. **常见 RAG 变体**
   - **Naive RAG**：单轮语义检索 + stuff prompt
   - **Multi-query RAG**：多 query 扩展提升召回率
   - **Hybrid RAG**：语义向量 + BM25 结合
   - **GraphRAG**：用图结构表达实体和关系

4. **常见坑**
   - chunk 太大：超过上下文限制或噪声太多
   - chunk 太小：语义断裂
   - embedding 模型和语料不匹配
   - top-k 选取不当

## 4. Java 后端 RAG 项目思路

技术栈：Spring Boot + Ollama + pgvector/Milvus

```java
class RagService {
    EmbeddingClient embeddingClient;
    VectorStore vectorStore;
    LlmClient llmClient;

    String ask(String question) {
        float[] qVec = embeddingClient.embed(question);
        List<DocChunk> chunks = vectorStore.search(qVec, 5);
        String context = buildContext(chunks);
        String prompt = buildPrompt(question, context);
        return llmClient.chat(prompt);
    }

    String buildContext(List<DocChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (DocChunk c : chunks) {
            sb.append(c.getText()).append("\n\n");
        }
        return sb.toString();
    }

    String buildPrompt(String question, String context) {
        return "你是一个严谨的企业知识库助手，只能根据给定资料回答问题，禁止编造。\n\n资料：\n"
                + context + "\n\n问题：" + question + "\n\n请用中文回答：";
    }
}
```

> **重点**：Java 只负责接请求 → 调 embedding 服务 → 查向量库 → 拼 prompt → 调 LLM → 返回结果。Embedding 和向量库可以是独立服务。

---

## 核心要点回顾

- RAG = 索引 + 检索 + 生成三段流水线
- Python Demo 仅需 sentence-transformers + FAISS + OpenAI
- Java 后端可用 Spring Boot + pgvector/Milvus 实现
- 核心优化点：chunk 策略、Hybrid 检索、Rerank

## 参考资料

1. [[AI-RAG-快速学会]]
2. [[RAG混合检索与Rerank优化]]
3. [[个人技术文档问答系统：RAG实战项目]]
