# 06 RAG 入门 Demo

> AI 应用的两大核心能力之一：让模型"读过"你的文档再回答。用最小链路（分块 → 向量化 → 检索 → 注入 → 生成）做一个本地文档问答 Demo，理解向量数据库与检索增强的本质。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [RAG 为什么存在：幻觉与知识边界](#2-rag-为什么存在幻觉与知识边界)
3. [最小链路全景与选型](#3-最小链路全景与选型)
4. [分块：把文档切成检索单元](#4-分块把文档切成检索单元)
5. [向量化与入库：Chroma](#5-向量化与入库chroma)
6. [检索与注入生成](#6-检索与注入生成)
7. [验证与常见坑](#7-验证与常见坑)

---

## 1. 目标与验收

本 Demo 的产出：一段文档（比如你整理的 Python 学习笔记）入库 Chroma，命令行提问能"带文档上下文"回答。验收标准：**能画出 RAG 五步链路图并讲清每一步的职责**；**能解释为什么不能把全文塞给模型**；**能说清分块大小与 overlap 的权衡**。版本基线（2026-08）：Chroma 向量库 + 本地 embedding（sentence-transformers）或 OpenAI 兼容 embedding API。顺带一提：为什么不能把全文塞给模型——模型上下文窗口有限（flash 1M token 看似大，但塞全量文档既贵又会让模型"注意力被无关内容稀释"，检索的本质是**只给模型它需要的部分**）。

## 2. RAG 为什么存在：幻觉与知识边界

模型的固有局限是"知识边界"：**训练数据有截止时间、不含你的私有文档**，面对边界外的问题，模型会"自信地编造"——这就是幻觉。解决思路两个：**微调**（把知识烙进模型权重，成本高、更新慢、易灾难性遗忘）与 **RAG**（把知识放在模型外面，回答时检索相关片段注入提示词）。RAG 的优势是**知识可增删**（加一篇文档即新增知识）、**答案可溯源**（能指出依据哪段原文）、**成本低**（不用重新训练）。2026 年的共识：**多数知识问答场景先用 RAG，微调只在"风格/格式/专业表达"这类需求上做**。理解"模型是大脑、向量库是外挂记忆"这个比喻，RAG 的一切细节都围绕它展开。

## 3. 最小链路全景与选型

RAG 最小链路五步：**分块**（把长文档切成可检索的片段）→ **向量化**（每块文本转成向量）→ **入库**（向量存进向量库）→ **检索**（把问题向量化后取相似度最高的几块）→ **注入生成**（检索结果拼进提示词，模型基于它们回答）。前四步在提问前完成一次（离线），第五步每次提问都跑（在线）。

选型三个决定：**向量库**用 Chroma（本地文件存储，零部署，Demo 期首选；Level2 再考虑 Milvus/ES 等）；**embedding 模型**两条路——本地 `sentence-transformers`（如 BAAI/bge-small-zh-v1.5，中文效果好、离线免费、首次下载模型几百 MB）或 API 版（SiliconFlow 等平台的 OpenAI 兼容 embedding 接口，免本地资源）。注意 **DeepSeek 官方 API 目前不提供 embedding 端点**，本地 embedding 是本 Demo 的默认方案。**检索方式**用 top_k 相似度（余弦距离），混合检索（BM25+向量）留到 Level2。

三个决定都指向同一个原则：**Demo 期选"最快能跑通"的**。向量库选 Chroma 是因为零部署（一个目录就是一个库），embedding 选本地是因为不依赖额外 API 账号，检索用 top_k 是因为一步到位。选型的取舍记录在案（为什么这么选、换什么更好）——这是 08 篇调优记录和面试素材的共同来源。

## 4. 分块：把文档切成检索单元

分块决定检索质量的上限：**块太大**，检索结果语义杂、token 费高；**块太小**，单块信息不足、切碎上下文。经验基线：**500-800 字符 + overlap 50-100**——overlap 的作用是让被切在边缘的句子在相邻块里重现，避免"句首在 A 块、句尾在 B 块"的语义断裂：

```python
def chunk_text(text: str, size: int = 500, overlap: int = 80) -> list[str]:
    """按字符切块，带 overlap。Demo 期够用；中文语义切分见进阶篇。"""
    chunks = []
    start = 0
    while start < len(text):
        chunks.append(text[start:start + size])
        start += size - overlap
    return chunks

with open("data/notes.md", encoding="utf-8") as f:
    docs = chunk_text(f.read())
print(f"文档切成 {len(docs)} 块")
```

两点说明：字符切块是"能用但粗糙"的方案，**语义切分**（按段落/标题层级切）在「RAG拓展优化深化」体系里展开；分块参数（size/overlap）是 RAG 调优的第一组旋钮，**效果对比方法是固定其他环节、只改分块，看回答质量**——这是 08 篇调优记录要做的事。

## 5. 向量化与入库：Chroma

每块文本转成向量（embedding），存进 Chroma。向量化的核心认知：**语义相近的文本，向量距离近**——"什么是 RAG"和"检索增强生成介绍"的向量距离，远小于和"今天天气如何"的距离。向量库的工作就是"找最近的邻居"。

```python
import chromadb
from sentence_transformers import SentenceTransformer

# 本地 embedding 模型：首次运行自动下载（bge-small-zh 约 100MB）
embedder = SentenceTransformer("BAAI/bge-small-zh-v1.5")

def embed(texts: list[str]) -> list[list[float]]:
    return embedder.encode(texts).tolist()

client = chromadb.PersistentClient(path="data/chroma")   # 持久化到本地目录
collection = client.get_or_create_collection("level1_notes")

# 入库：id 必须唯一；documents 存原文（检索后要注入提示词用）
collection.add(
    ids=[f"chunk-{i}" for i in range(len(docs))],
    documents=docs,
    embeddings=embed(docs),
)
print(f"入库完成：{collection.count()} 块")
```

三个要点：**PersistentClient 持久化**（数据落盘 data/chroma，重启不丢；默认是内存库，重启清零）；**documents 必须存原文**（检索结果最终要喂给模型，只存向量等于丢了原文）；**embedding 模型全局加载一次**（加载耗内存耗时间，配合 05 篇的 @st.cache_resource 缓存，别每次提问都加载）。Chroma 也支持 `collection.add(ids, documents)` 不带 embeddings（自动用默认 embedding 函数），但显式传 embeddings 更可控。

## 6. 检索与注入生成

提问时走"在线链路"：问题向量化 → 查相似度 → 拼提示词 → 生成。检索是"召回"（找出可能相关的块），注入是"组织"（把块放对位置、定好约束）：

```python
def ask_with_rag(question: str, top_k: int = 3) -> str:
    # 1. 问题向量化并检索
    results = collection.query(
        query_embeddings=embed([question]), n_results=top_k)

    # 2. 拼上下文（每块带来源编号，方便溯源）
    context = "\n\n".join(
        f"[文档{i+1}] {doc}" for i, doc in enumerate(results["documents"][0]))

    # 3. 注入提示词：上下文 + 任务 + 约束
    system = """你是文档助手。基于【上下文】回答问题。
约束：1. 只依据上下文回答，上下文没有的明确说"文档中未找到"
2. 回答时标注依据的文档编号"""
    messages = [
        {"role": "system", "content": system},
        {"role": "user", "content": f"【上下文】\n{context}\n\n【问题】\n{question}"},
    ]
    resp = client.chat.completions.create(
        model="deepseek-v4-flash", messages=messages, temperature=0.2)
    return resp.choices[0].message.content

print(ask_with_rag("我笔记里写了 uv 和 pip 什么区别？"))
```

三个设计要点：**提示词约束"只依据上下文"**（这是防幻觉的最后一道闸门——模型被允许说"没找到"而不是编）；**带来源编号**（溯源是 RAG 与微调相比的核心卖点，Level2 会做成引用展示）；**temperature 降低**（知识问答要确定性，0.2 左右）。验证方式：问"文档里没有的问题"（比如文档没写的内容），模型应回答"未找到"而不是编造——**这个负例测试是 RAG 验收的关键一步**。

## 7. 验证与常见坑

验收三条：**正例命中**（文档里有的内容能答对）；**负例拒答**（文档没有的内容答"未找到"）；**溯源正确**（标注的文档编号确实是依据内容）。

排查检索链路的两个调试技巧：**先看检索再谈生成**——回答不对时，先打印 `results["documents"]` 看检出来的块与问题是否相关（检索坏了，生成必然错；检索对了生成还错，才是提示词问题）——这条"链路分层排查"的思路与 09 篇的方法论一脉相承；**看距离分数**——Chroma 的 query 结果带 `distances`（余弦距离，越小越相似），分数整体偏高说明 embedding 与文档领域不匹配（换模型），分数接近 0 说明问的就是原文片段（正常）。

常见坑按频率排：**embedding 每次运行重新下载/加载**（模型文件已缓存到本地就不重复下载，但内存加载要缓存——用 @st.cache_resource）；**入库时 ids 重复**（重复跑入库脚本会报错——先 collection.delete 或给 id 加时间戳）；**检索结果与问题无关**（embedding 模型选错了语言——中文文档用 bge-*zh 系列，英文模型检索中文效果差）；**注入后回答变差**（上下文太长挤占模型注意力——top_k 调小、提示词把问题放最后）；**中文乱码**（读文件忘 encoding="utf-8"）。

> 🎯 **核心要点**：RAG 的本质是"**外挂记忆 + 防幻觉闸门**"——分块定检索粒度、向量化定语义匹配、注入定回答边界。链路跑通后，把"负例拒答"实验做出来，你就真正理解了 RAG 为什么存在——这是面试讲 RAG 项目的第一句话。

---

**下一模块**：[07 Function Calling 工具调用 Demo](./07-Function%20Calling%20工具调用%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)

【参考来源】
- [Chroma: Getting Started](https://docs.trychroma.com/docs/overview/getting-started)
- [BAAI/bge-small-zh-v1.5 | Hugging Face](https://huggingface.co/BAAI/bge-small-zh-v1.5)
- [LangChain vs LangGraph 2026（RAG 栈选型）](https://superml.dev/langchain-vs-langgraph-2026-enterprise-agents)
