# 09 - 高级 RAG 范式

> 🎯 基础 RAG 只是起点 — Self-RAG 让模型自我反思、CRAG 自动纠错、GraphRAG 引入知识图谱。这些范式的目标是让 RAG 更"聪明"

---

## 目录

1. [Self-RAG：自我反思检索](#1-self-rag自我反思检索)
2. [Corrective-RAG CRAG](#2-corrective-rag-crag)
3. [GraphRAG：知识图谱增强](#3-graphrag知识图谱增强)
4. [Agentic RAG](#4-agentic-rag)
5. [多模态 RAG](#5-多模态-rag)

---

## 1. Self-RAG：自我反思检索

```text
Self-RAG = 让 LLM 自己决定"要不要检索"和"检索结果有没有用"

传统 RAG：总是检索 → 即使问题不需要检索（如"你好"）
Self-RAG：先判断 → 需要检索才检索 → 检索后评估质量

流程：
  ① 判断是否检索（Retrieval on Demand）
  ② 检索
  ③ 判断检索结果是否相关（Relevance Check）
  ④ 生成回答时标注每句话的来源（Citation）
  ⑤ 判断回答是否得到检索结果支持（Faithfulness Check）
```

---

## 2. Corrective-RAG (CRAG)

```text
CRAG = 检索结果不好时自动修正

核心流程：
  ① 检索 → 用检索质量评估器打分
  ② 分数评估：
     高 → 直接用检索结果生成
     中 → 用 Web Search 补充
     低 → 完全用 Web Search 替换

  ③ 对最终结果做知识精炼 → 去除噪声 → 生成

比基础 RAG 多了"自知之明" → 知道检索不行时不硬用
```

---

## 3. GraphRAG：知识图谱增强

### 3.1 原理

```text
GraphRAG = 在向量检索之外，引入知识图谱的结构化知识

传统 RAG：纯向量检索 → 只能找到"语义相似"的文档
GraphRAG：向量检索 + 图遍历 → 能找到"关系相关"的实体

例子：
  问题："马云的投资人还有哪些公司？"
  
  向量检索：找到马云的百科页面 → 可能没有投资人信息
  GraphRAG：马云 → 投资人 → 软银 → 软银还投了 → Uber, ARM...
    → 通过图关系获取了向量检索拿不到的信息
```

### 3.2 构建知识图谱

```python
# 用 LLM 从文档中抽取实体和关系构建图
def build_knowledge_graph(documents, llm):
    graph = {}
    for doc in documents:
        # LLM 抽取三元组
        prompt = f"从以下文本抽取(实体, 关系, 实体)三元组：\n{doc}"
        triples = llm(prompt)
        for subj, rel, obj in triples:
            graph.setdefault(subj, []).append((rel, obj))
    return graph
```

---

## 4. Agentic RAG

```text
Agentic RAG = 用 Agent 的思维做 RAG

  不是"检索一次 → 生成一次"
  而是"思考 → 检索 → 思考 → 可能需要再检索 → 再思考 → 生成"

类似人类查资料：
  ① 先查关键词 → 发现资料不够
  ② 调整关键词再查 → 找到了
  ③ 发现需要补充数据 → 查另一个来源
  ④ 综合 → 给出答案

框架：ReAct + 多步检索 = Agentic RAG
```

---

## 5. 多模态 RAG

```text
传统 RAG：纯文本
多模态 RAG：文本 + 图片 + 表格 + 图表

  用户问："这个架构图中用的是什么数据库？"
  → 检索图片 → 用多模态 LLM 理解图片 → 回答

挑战：
  → 图片的 Embedding 和文本的 Embedding 不在同一空间
  → 需要多模态 Embedding 模型 (CLIP/Jina CLIP)
```

---

## 核心要点回顾

- Self-RAG：模型自己决定是否检索 + 检索结果是否有用
- CRAG：检索不好时自动用 Web Search 兜底
- GraphRAG：向量检索 + 知识图谱 = 关系推理能力
- Agentic RAG：多步检索 + 思考 → 类人查资料过程
