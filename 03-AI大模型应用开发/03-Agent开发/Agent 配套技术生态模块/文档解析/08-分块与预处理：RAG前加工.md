# 分块与预处理：RAG 前加工

> 解析的终点是"可检索的块"。2026 共识：**先版面感知提取成 Markdown，再结构感知分块**——按标题切、表格保持原子、块携带上下文。这层做不好，向量库再强也白搭。

## 1. 两阶段流水线（2026 共识）

```text
第一阶段：版面感知提取（解析，03-07 模块）
  文档 → 结构完整 Markdown（标题层级/表格/顺序/元数据）
第二阶段：结构感知分块（本章）
  Markdown → 按标题边界切块 + 原子块保护 + 元数据挂载
  → 向量库（嵌入） / 数据库 / Agent 上下文
```

| 误区 | 后果 |
|---|---|
| 原始文本直接按字符数切 | 表格被切碎、代码块半截、标题上下文丢失 |
| 跳过解析直接喂 LLM | 长文档 token 爆炸、结构错乱 |

> 🎯 核心要点：**"文本切块"是 2023 年的做法，"结构切块"是 2026 年的标准**——分块器只该回答"哪些块是一个语义单元"，而不是"第 500 个字符切在哪"。

## 2. 为什么 Markdown 是规范中间格式

| 理由 | 说明 |
|---|---|
| LLM 友好 | 模型天然理解 Markdown 结构 |
| 保留层级 | #/##/### 直接可作分块边界 |
| 表格/代码完整 | 可识别原子单元 |
| 工具生态 | 所有分块器都支持 |

```text
解析器输出（Docling/PaddleOCR-VL/pymupdf4llm）→ Markdown
→ 结构分块器（标题边界）→ chunks（带 heading path）
```

## 3. 分块策略对比

| 策略 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| 字符数硬切 | 每 500 字切一刀 | 简单 | 表格碎/代码断/标题丢（❌） |
| 递归字符切 | 先按段落再按字符 | 略好 | 仍不懂结构 |
| **标题感知切** | 按 #/##/### 边界切 | 语义完整、上下文保留 | 长章节需再分 |
| 语义切（嵌入） | 向量相似度找边界 | 主题一致 | 慢、贵、不稳定 |

**2026 主流：标题感知切 + 软上限**——以标题为边界，章节超过软目标（如 1800 字符）再细分，但**永远不切穿表格与代码块**。

## 4. 原子块保护：表格与代码

| 原子单元 | 规则 |
|---|---|
| 表格 | 整表为一个块（小表）或按行分块（大表，07 篇路线 B）——**绝不从行中间切断** |
| 代码块 | 围栏代码整体保留 |
| 公式 | LaTeX 块整体保留 |
| 列表 | 尽量同章保留 |

```python
# 伪代码：结构分块器骨架
def structure_chunk(markdown, soft_max=1800):
    chunks, current = [], []
    for node in parse_tree(markdown):       # 按标题层级构建树
        if is_heading(node):                # 标题 → 新块起点
            flush(current, chunks)
            current = [node]
        elif is_atomic(node):               # 表格/代码 → 永远整块
            flush(current, chunks)
            chunks.append(node)             # 单独成块
        elif size(current) + size(node) > soft_max:
            flush(current, chunks); current = [node]
        else:
            current.append(node)
    flush(current, chunks)
    return chunks
```

## 5. Heading Path：孤块也有上下文

```text
文档：## 安装\n### Windows\n安装步骤...\n### Mac\n...
分块结果：
  chunk1: heading_path=["安装","Windows"] + 内容
  chunk2: heading_path=["安装","Mac"] + 内容
检索到 chunk2 时，模型仍知道它属于"安装"章节
```

| 元数据字段 | 内容 | 用途 |
|---|---|---|
| heading_path | `["安装","Mac"]` | 孤块上下文 |
| page_range | `[3,4]` | 引用溯源 |
| source | 文件路径/URL | 溯源 |
| doc_type | 财报/论文/合同 | 过滤检索 |
| chunk_type | text/table/figure | 定向检索（只搜表格） |
| created_at | 入库时间 | 时效过滤 |

> 💡 元数据不是可选项——**检索的"过滤"与"溯源"全靠它**。没有 heading_path 的块，被检索到时模型不知道它属于哪章。

## 6. 表格分块的两种模式（回顾 07 篇）

| 模式 | 结构 | 适合 |
|---|---|---|
| 整表块 | 一张表一个 chunk | ≤50 行、问题针对整表 |
| 行块+摘要块 | 摘要块（标题+列名）+ 每行块（带表头上下文） | 大表、明细检索 |

```text
行块示例（携带上下文）：
"季度收入表 | 季度: Q1 | 收入: 12.4M | 成本: 8.1M | 利润: 4.3M | 页码: 3"
```

## 7. 预处理清单（解析后、入库前）

```text
□ 页眉页脚已剔除（防每页重复噪音）
□ 空段落/空白页已清理
□ 标题层级正确（供分块边界）
□ 表格未摊平、跨页已合并
□ 图片有替代文本或建议描述（图表问答用）
□ 公式为 LaTeX（数学场景）
□ 敏感信息处理（脱敏/过滤，合规）
□ 元数据齐备（heading_path/page/source/doc_type）
□ 分块测试：抽查 20 块确认"每块语义独立、上下文自足"
□ 缓存解析结果（同一文档不重复解析）
```

## 8. 分块质量评估

| 指标 | 测什么 |
|---|---|
| 块内语义完整性 | 每块是否独立可理解（heading path 后） |
| 检索命中率 | 测试问题能否命中所属块 |
| 原子性 | 表格/代码从未被切开（自动化断言） |
| Token 利用率 | 每块是否接近目标大小（512 token 软目标利用率 399-402 为佳） |

> 🎯 核心要点：分块的目标是**"检索时命中的块恰好是回答所需的语义单元"**——结构感知分块 + heading path + 原子块保护，三者齐备才是 2026 标准的 RAG 前加工。

## 9. 分块常见坑速查

| 坑 | 现象 | 解法 |
|---|---|---|
| 按字符数硬切 | 表格碎、代码断、标题丢 | 标题感知 + 原子块 |
| 表格从中间切断 | 行列关系毁 | 表格整块/按行分块 |
| 无 heading path | 孤块无上下文 | 标题路径元数据 |
| 块内无标题 | 检索不知所属章节 | 块首携带最近标题 |
| 页眉页脚进块 | 每块重复噪音 | 解析阶段剔除 |
| 过度重叠 | 冗余 token 翻倍 | 重叠 ≤10% 或按语义 |
| 章节超长 | 单块超上下文 | 软上限内细分（不切原子） |
| 元数据缺失 | 无法过滤/溯源 | 解析阶段挂载 |

## 10. 与向量库/数据库的衔接

```text
结构感知分块产物
  ├── 文本块（带 heading_path/页码） → 向量库（嵌入检索）
  ├── 表格块（JSON/行块）           → 向量库 + 结构化表（表格问答）
  ├── 元数据                        → 过滤检索（doc_type/时间/页）
  └── 全文/源码                     → 数据库/对象存储（审计、重切）
```

| 下游 | 需要什么 | 本模块交付 |
|---|---|---|
| 向量检索 | 语义完整块 + 元数据 | chunk + heading_path/page/source |
| 表格问答 | 结构化表格可检索 | 表格原子块（07 篇） |
| 引用溯源 | 页码/来源 | page_range/source 元数据 |
| 过滤检索 | 类型/时间/章节 | doc_type/created_at/heading_path |
| 数据库入库 | 结构化行 | 表格 JSON（衔接[数据库交互](../数据库交互/00-数据库交互总览.md)体系） |

## 11. 分块参数参考

| 参数 | 参考值 | 说明 |
|---|---|---|
| 块大小（软目标） | 512 token / ~1800 字符 | 对齐嵌入模型上下文 |
| 重叠（overlap） | ≤10% 或按语义 | 过度重叠纯浪费 token |
| 最小块 | ≥50 token | 过小无检索价值 |
| 标题深度 | 到 ###（三级） | 过深碎片化 |
| 表格块 | 整表或按行（07 篇） | 原子块不切穿 |
| 代码块 | 围栏整体 | 原子块 |
| 长章节 | 软上限内按子标题细分 | 无子标题则按段落 |

> 💡 参数要**实测调优**：用 50 条业务问题跑检索命中率，对比不同块大小/重叠——不要照抄任何人的默认值，你的文档分布决定最优值。调优顺序建议：先定"表格/代码原子规则"（结构性），再调块大小与重叠（数值性）——结构错了调参救不回来。

---

**下一模块**：[09-Agent 文档解析工具链](09-Agent文档解析工具链.md)　**返回总览**：[00-文档解析总览](00-文档解析总览.md)

## 参考来源

- [Layout Awareness（MariaDB Docs）](https://mariadb.com/docs/tools/mariadb-ai-rag/api-reference/layout-awareness)
- [Document Ingestion Fundamentals（KodeKloud）](https://notes.kodekloud.com/docs/Fundamentals-of-RAG/Document-Processing-and-Chunking/Document-Ingestion-Fundamentals/page)
- [Build a RAG ingestion pipeline with the Data Extraction API（Nutrient）](https://www.nutrient.io/guides/dws-data-extraction/examples/build-rag-ingestion-pipeline/)
- [DocSlicer：deterministic document parser and chunker（GitHub）](https://github.com/DocSlicer/DocSlicer)
- [n8n-nodes-markdown-chunker（npm）](https://socket.dev/npm/package/n8n-nodes-markdown-chunker)
- [Structure-Aware Chunking for Tabular Data（arXiv 2605.00318）](https://arxiv.org/html/2605.00318v1)
