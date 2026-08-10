# 07 - 分块 chunking

> 定位：RAG 检索质量的直接决定因素——chunk_by_title 与四策略、max_characters/overlap 参数、表格保留——"分块粒度 = 检索粒度：块太大语义稀释，块太小上下文破碎"

---

## 📚 目录

1. [为什么分块决定检索质量](#1-为什么分块决定检索质量)
2. [chunk_by_title：默认策略](#2-chunk_by_title默认策略)
3. [四策略与参数](#3-四策略与参数)
4. [表格与特殊元素保留](#4-表格与特殊元素保留)
5. [分块质量验证](#5-分块质量验证)
6. [分块与入库衔接](#6-分块与入库衔接)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. 为什么分块决定检索质量

**RAG 的检索粒度 = 分块粒度**——向量检索按"块"召回：**块太大**——一个 chunk 里塞了 3 个主题，query 命中其中 1 个主题，但整块进上下文（语义稀释 + 浪费 token）；**块太小**——一个 chunk 只有半句话，检索命中了但上下文不完整（回答质量崩盘）。**"分块是检索质量最直接的旋钮"**——与向量索引的 ef（召回旋钮）同构但更前置：**索引调优是"找得到"，分块调优是"找到的东西好不好"**（milvus 06/07 篇的对照）。

unstructured 的分块设计：**基于 Element 类型的语义分块**——不是简单按字符数切，而是**尊重文档结构**（标题为锚点、表格完整保留）——**"结构感知分块 vs 暴力切字符"是它与普通 split_text 的本质区别**。

## 2. chunk_by_title：默认策略

**chunk_by_title 是默认与主力**：以标题（Title 元素）为分块锚点，标题下的内容聚成一个块：

```python
from unstructured.chunking.title import chunk_by_title

chunks = chunk_by_title(
    elements,
    max_characters=2000,       # 块最大字符数（核心参数）
    new_after_n_chars=1500,    # 超过该长度且到标题边界就切
    overlap=200,               # 相邻块重叠字符数（上下文连贯）
    combine_text_under_n_chars=500,  # 短元素合并阈值（小段落拼一起）
)
```

**行为心智**：**"标题开新块，内容填充，超限切分"**——`Title` 元素触发新块、其下的 NarrativeText/ListItem 等聚进当前块；**参数语义**：`max_characters` 是硬上限、`new_after_n_chars` 是"够长了就切"的软阈值、`overlap` 是相邻块重叠（防"上下文断裂在块边界"）、`combine_text_under_n_chars` 防碎片（零散小元素合并）。**"一个标题下内容不长就整段一块，长了就按上限切，切的时候留重叠"**是 chunk_by_title 的一句话总结。

## 3. 四策略与参数

unstructured 的 chunking 模块提供四个策略（`unstructured.chunking` 下的模块）：

| 策略 | 模块 | 特点 |
|------|------|------|
| basic | `chunking.basic` | 纯字符/元素计数切分（简单粗暴） |
| **by_title** | `chunking.title` | **标题锚点 + 语义聚块（默认推荐）** |
| by_page | `chunking.base` 的 page 变体 | 按页分块（页即块） |
| similarity | `chunking.similarity` | 语义相似聚块（embeddings 辅助） |

**选型**：**默认 by_title**（结构与语义兼顾）；**by_page** 适合"页面本身就是语义单元"的文档（每页一个独立主题的幻灯片/表单）；**similarity** 是"结构差 + 语义聚类"的高级档（贵——要算 embedding）；**basic** 是兜底（无结构文档）。**参数家族**（by_title 一组 + 通用一组）：通用参数 `max_characters`/`overlap`/`min_characters`（低于此长度丢弃）——**"先定策略，再定参数"**。

**参数调优心法**：`max_characters` 与 embedding 模型上下文对齐——**"块长 ≈ 模型上下文窗口的 1/10-1/5"**（embedding 模型窗口 512 token 时块长 500-1500 字符是常见区间）；`overlap` 取块长的 10-20%——**"overlap 是保上下文连贯的保险，不是越大越好"**（重叠块多 = 向量库冗余）。

## 4. 表格与特殊元素保留

**表格在分块时的默认行为与对策**（00 篇误区四——重要坑）：

```python
chunks = chunk_by_title(
    elements,
    max_characters=2000,
    include_roles=["Title", "NarrativeText", "ListItem", "Table"],  # 显式包含表格！
)
```

**默认行为**：**chunk_by_title 默认过滤掉 Table/Image 等非文本元素**（只留文本类）——**表格默认不进向量库**！**对策**：`include_roles` 显式声明要保留的类型——**"表格要进 RAG 必须显式 include_roles=['Table']"**（00 篇误区四的解法）。

**表格进库的姿势**：表格元素（text_as_html 结构）作为独立 chunk 或并入上下文——**"表格以 HTML 结构块进库，检索时结构保留"**（05 篇表格深潜的延续）；**图片**默认也不进——多模态 RAG 要单独走图片提取通道（05 篇 extract_images_in_pdf）。**"默认过滤是防噪音的设计，不是 bug——要保留就显式声明"**是特殊元素处理的心智。

## 5. 分块质量验证

**分块质量的四个检查维度**（与 milvus 的 recall 验证同构——"分块也要验证"）：

```python
# 维度一：块大小分布（健康：集中在 max 的 60-100%）
sizes = [len(c.text) for c in chunks]
print(f"块数: {len(chunks)}, 平均: {sum(sizes)//len(sizes)}, 最大: {max(sizes)}")

# 维度二：语义完整抽样（人工看 5-10 个块）
for c in chunks[:5]:
    print("---", c.metadata.get("page_number"), "---")
    print(c.text[:100])

# 维度三：边界合理性（块边界是否切断句子/表格）
# 维度四：检索实测（抽样 query 看召回块质量——milvus 10 篇实战）
```

**验证心法**：**"块数 × 平均长"先看分布**（异常：大量空块 = min 参数问题、最大块爆表 = max 没生效）；**抽样人工读**（边界切断句子 → 调 overlap/阈值）；**最终用检索实测说话**（"抽样 query 命中块是否回答了问题"）——**"分块参数没有标准答案，只有针对你的文档的答案"**（与索引参数同哲学，milvus 06 篇）。

**调参迭代的推荐节奏**（别一次全改）：**固定其他参数，单变量实验**——先定 `max_characters`（按窗口），再调 `combine_text_under_n_chars`（碎片治理），最后调 `overlap`（连贯性）；**每次只动一个旋钮 + 跑四维验证**——"**单变量实验 + 验证驱动**"是分块调参与索引调参共通的工程方法（milvus 06 篇同哲学）。

## 6. 分块与入库衔接

**Chunk → 向量入库**的衔接姿势（与 milvus‑python 体系打通，00 篇分工）：

```python
# 分块产出 → 每块一条向量记录
records = []
for i, chunk in enumerate(chunks):
    records.append({
        "id": i,
        "text": chunk.text,
        "title": chunk.metadata.get("title") or chunk.metadata.get("category_depth"),
        "page_number": chunk.metadata.get("page_number"),
        "source": chunk.metadata.get("filename"),
        "embedding": embed_model.encode(chunk.text).tolist(),   # embedding
    })

# 交给 milvus 入库（milvus‑python 体系的 05 篇）
milvus_client.insert("docs", data=records)
```

**衔接要点**：**metadata 与 text 一起入库**（溯源可审计——03 篇）；**chunk 的 metadata 是合并的**（跨页 chunk 记录起止页码）；**embedding 用 chunk 文本而非 Element 文本**（检索粒度一致）。**"Element → Chunk → 向量记录"是文档管道的完整三段**（10 篇实战打通全链路）。

## 7. 常见坑

**坑一：表格默认被过滤**——不 include_roles 表格全丢；**显式 include_roles=['Table']**（第 4 节——00 篇误区四）。

**坑二：max_characters 与 embedding 窗口不匹配**——块太大 embedding 截断；**块长对齐模型窗口**（第 3 节心法）。

**坑三：overlap 设 0 导致边界断句**——相邻块上下文断裂；**overlap 取 10-20%**（第 3 节）。

**坑四：不验证直接入库**——分块问题到检索才暴露；**四维验证（分布/抽样/边界/检索）**（第 5 节）。

**坑五：把 chunk_by_title 当字符切分器**——不理解标题锚点语义，参数乱调；**"标题开新块"心智先行**（第 2 节）。

## 8. 练习 5 题

1. "分块粒度 = 检索粒度"——块太大/太小的后果各是什么？
2. chunk_by_title 的行为心智？四个参数各管什么？
3. 四策略怎么选？参数调优心法（窗口对齐/overlap 比例）？
4. 表格为什么默认被过滤？include_roles 怎么用？
5. 四维验证是哪四维？为什么"参数没有标准答案"？

> 🎯 **核心要点**：分块 = **by_title 默认（标题锚点）+ max_characters/overlap 对齐模型窗口 + 表格显式 include_roles + 四维验证**——"分块粒度 = 检索粒度，结构感知分块 vs 暴力切字符是本质区别；分块参数没有标准答案，只有针对你的文档的答案"。

---

**下一模块**：[08-与框架集成.md](08-与框架集成.md) / **返回总览**：[00-unstructured总览.md](00-unstructured总览.md)
