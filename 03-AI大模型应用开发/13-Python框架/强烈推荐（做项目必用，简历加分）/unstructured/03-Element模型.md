# 03 - Element 模型

> 定位：unstructured 的核心数据模型——20+ 元素类型体系、metadata 溯源字段、坐标与内容哈希——"看懂 Elements 就懂了 RAG 切分与检索质量的底层依据"

---

## 📚 目录

1. [Element：带类型的文档单元](#1-element带类型的文档单元)
2. [20+ 元素类型体系](#2-20-元素类型体系)
3. [metadata：溯源信息的价值](#3-metadata溯源信息的价值)
4. [坐标与版面信息](#4-坐标与版面信息)
5. [Element 操作：过滤/排序/合并](#5-element-操作过滤排序合并)
6. [Element 与分块的衔接](#6-element-与分块的衔接)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. Element：带类型的文档单元

**Element（元素）是 unstructured 的基本产出单元**——"解析后的文档 = 一串有序的 Elements"。每个元素三件套：**category（类型）+ text（内容）+ metadata（元数据）**——这与"PDF 转文本"的本质区别就在"类型"与"元数据"上：**RAG 需要知道"这是标题还是正文、在第几页、坐标在哪"**，纯文本流给不了（01 篇）。

**底层实现**：`Element` 基类 + 各类子类（`Title`、`NarrativeText`……），`el.category` 返回类型名（字符串）、`el.text` 返回内容、`el.metadata` 返回元数据对象——**0.15+ 的 metadata 是 dict 风格访问**（`el.metadata["page_number"]` 或属性风格 `el.metadata.page_number` 都行，dict 为主流写法）——**老教程的 `el.metadata.filename` 点访问在新版可能报错，dict 访问是 2026 标准**（00 篇误区六延伸）。

## 2. 20+ 元素类型体系

| 类别 | 类型 | RAG 意义 |
|------|------|---------|
| 文本 | `Title` / `NarrativeText`（正文）/ `UncategorizedText` | 切分粒度与正文识别 |
| 结构 | `ListItem` / `BulletedText` / `Header` / `Footer` / `PageBreak` | 列表语义、页眉页脚过滤 |
| 数据 | `Table`（含 HTML 结构）/ `Formula` / `KeyValueItem` | 表格/公式结构化 |
| 媒体 | `Image` / `FigureCaption` / `Audio` | 图片与配文关联 |
| 元信息 | `PageNumber` / `PageBreak` / `EmailAddress` / `Url` | 定位与过滤 |

**类型体系的 RAG 价值**：**过滤**——`Header`/`Footer`/`PageBreak` 在入库前删掉（页眉页脚是检索噪音）；**切分**——`Title` 是 chunk_by_title 的分块锚点（07 篇）；**召回**——`Table` 单独处理（结构化表示进检索）；**溯源**——每条检索结果可回指到文档的"标题下第几段"（metadata 配合）。

**类型判断的准确性**：fast 策略用启发式（字体/位置/正则），hi_res 用布局模型（更准）——**"类型的准确性取决于策略，策略选择决定 Element 质量"**（04 篇）。

## 3. metadata：溯源信息的价值

metadata 是每个 Element 的"身份证"，常用字段：

| 字段 | 含义 | 用途 |
|------|------|------|
| `page_number` | 所在页码 | 检索结果回指页码 |
| `filename` / `filetype` | 来源文件与类型 | 多文件管道区分来源 |
| `languages` | 检测语言 | 多语言过滤 |
| `coordinates` | 页面坐标（四点） | 版面定位（04 篇） |
| `last_modified` | 文件修改时间 | 增量更新判断 |
| `text_as_html` | 表格的 HTML 表示 | 表格检索（05 篇） |
| `link_texts` / `link_urls` | 超链接 | 引用溯源 |
| `element_hash` | 内容哈希 | 去重（06 篇） |

**溯源的价值**：**RAG 的"引用"能力建立在 metadata 上**——用户问"这个结论来自哪里"，检索结果带上 `page_number` + `filename` 就能回答"来自《2026 年报》第 12 页"——**"无 metadata 的 RAG 是黑箱检索，有 metadata 的 RAG 可审计"**是它与简单解析的分水岭。

**metadata 的工程链路**（它不只"好看"，是管道的联接线）：**入库**——metadata 与 text 一起写进向量库（milvus 的字段设计，10 篇实战）；**检索**——`filter` 按 metadata 过滤（"只搜某文件/某页"——milvus 07 篇 filter 表达式）；**展示**——检索结果的引用回指（来源 + 页码）；**增量**——`last_modified` 与 `element_hash` 支撑增量更新（06 篇）。**"metadata 是管道每一环的联接线"**——从解析到展示全程传递，这就是为什么"只存 text 不存 metadata"的管道是半成品。

## 4. 坐标与版面信息

`coordinates` 是 hi_res 策略的产物：元素在页面上的**四点坐标**（左上/右上/左下/右下），意义分三层：

**版面还原**——多列 PDF 的"列混排"问题靠坐标修复：hi_res 检测到列边界后，**同一列的元素按阅读顺序重组**——"坐标是解决多列文档乱序的关键"（04 篇）。

**阅读顺序**——坐标 + 区域信息决定元素排序：`partition_pdf` 的 `sort_by_coordinates` 或读取后按 `(page_number, y_coord)` 排序——**"坐标排序 = 还原人眼阅读顺序"**。

**定位检索**——"这张表在页面左半部分"这类版面语义查询。**要点**：坐标只在支持版面分析的策略（hi_res/布局模型）下完整；fast 策略坐标可能缺失或粗糙——**"要坐标就上 hi_res"**。

## 5. Element 操作：过滤/排序/合并

解析后的 Elements 是普通列表，**Python 原生操作即可**：

```python
# 过滤：去掉页眉页脚与分页符（入库前标配）
clean = [el for el in elements
         if el.category not in ("Header", "Footer", "PageBreak")]

# 过滤：只要正文与标题（检索主体）
body = [el for el in elements
        if el.category in ("Title", "NarrativeText", "Table")]

# 排序：按页码 + 坐标（多列文档还原阅读顺序）
from unstructured.documents.elements import sort_by_coordinates
sorted_elements = sort_by_coordinates(elements)

# 按页码分组（分页处理）
from itertools import groupby
by_page = {k: list(v) for k, v in
           groupby(elements, key=lambda el: el.metadata["page_number"])}
```

**"Elements 是数据，过滤排序是 Python 的活"**——unstructured 给结构化，业务逻辑（留什么、排什么）是自己的代码——**06 篇的清洗是过滤的进阶，07 篇的分块是排序后的切分**。

**Element 与数据管道的对照**（把元素操作放进管道视角）：Elements 列表 ≈ **ETL 的中间行集**——"解析（提取）→ 过滤（清洗）→ 排序（规整）→ 分块（聚合）→ 入库（装载）"——**每一站都是对列表的变换**，这种"纯数据流"设计让管道可测试、可断点、可并行（02 篇 JSON 交接、09 篇并发批处理都建立在它之上）——**"Element 是数据流设计的具体化"**是理解 unstructured 工程价值的钥匙。

## 6. Element 与分块的衔接

**Element → Chunk 是文档管道的第二跳**（07 篇详讲）：分块器（`chunk_by_title` 等）读取 Elements 的类型与文本，按规则合并成**更大的 Chunk 单元**：

```python
from unstructured.chunking.title import chunk_by_title

chunks = chunk_by_title(elements, max_characters=2000, overlap=200)
# chunks 是 CombineElements 列表——每个 chunk 含多个 Element + 合并的 metadata
```

**衔接的关键**：**Element 的类型质量直接决定分块质量**——标题识别错了，分块锚点就错了（07 篇）；**metadata 在分块时合并保留**（chunk 的 metadata 含起止页码）——**"先保 Element 质量，才有好 Chunk"**是两环的因果链。

## 7. 常见坑

**坑一：metadata 点访问报错**——0.15+ dict 风格为主；**`el.metadata["key"]` 是 2026 标准写法**（第 1 节）。

**坑二：把 PageBreak 当正文**——分页符进向量库是噪音；**入库前过滤 Header/Footer/PageBreak**（第 5 节）。

**坑三：Table 的 text 是碎的**——表格元素的 text 只是拼接文本，**结构在 `metadata["text_as_html"]`**——要用 HTML 表示（05 篇）。

**坑四：多列文档乱序**——没排序直接分块，内容跳来跳去；**sort_by_coordinates 还原阅读顺序**（第 4 节）。

**坑五：只读 text 忽略 metadata**——检索结果无溯源；**metadata 与 text 一起入库**（第 3 节——10 篇实战落地）。

## 8. 练习 5 题

1. Element 三件套是什么？与"PDF 转文本"的本质区别？
2. 类型体系里哪些用于过滤、哪些用于切分、哪些用于召回？
3. metadata 的常用字段与用途？"可审计 RAG"指什么？
4. 坐标信息的三层价值？为什么"要坐标就上 hi_res"？
5. Element → Chunk 的衔接关系？"先保 Element 质量才有好 Chunk"？

> 🎯 **核心要点**：Element 模型 = **20+ 类型（过滤/切分/召回三用途）+ metadata 溯源（页码/坐标/哈希——可审计 RAG）+ 坐标排序（多列还原）+ 普通列表操作**——"看懂 Elements 就懂了解析质量的判据与 RAG 切分的起点"。

---

**下一模块**：[04-分区策略.md](04-分区策略.md) / **返回总览**：[00-unstructured总览.md](00-unstructured总览.md)
