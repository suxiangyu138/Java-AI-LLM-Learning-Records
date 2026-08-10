# 05 - 文件类型与 partition 家族

> 定位：25+ 格式的分区器逐个过——PDF/Word/PPT/Excel/HTML/图片/邮件——各自的专属参数与注意点——"换格式只换函数名，但每种格式有自己的脾气"

---

## 📚 目录

1. [partition 家族全景](#1-partition-家族全景)
2. [partition_pdf：头号选手](#2-partition_pdf头号选手)
3. [Office 三件套：docx/pptx/xlsx](#3-office-三件套docxpptxxlsx)
4. [网页与文本：html/md/txt](#4-网页与文本htmlmdtxt)
5. [图片与邮件：image/email](#5-图片与邮件imageemail)
6. [表格提取深潜](#6-表格提取深潜)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. partition 家族全景

| 分区器 | 格式 | 专属要点 |
|--------|------|---------|
| `partition_pdf` | PDF/扫描件 | 策略四档 + 表格提取 |
| `partition_docx` | Word | 样式解析（标题层级/列表） |
| `partition_pptx` | PowerPoint | 每页 = 一组元素（含文本框/图片） |
| `partition_xlsx` | Excel | 每 sheet 表格化 |
| `partition_html` | HTML/网页 | URL 抓取 + 标签语义 |
| `partition_md` | Markdown | 标题/列表/代码块结构 |
| `partition_text` | TXT | 纯文本 + 启发式分类 |
| `partition_image` | PNG/JPG/TIFF | 图片 OCR（专属分区器） |
| `partition_email` | EML/MSG | 邮件头/正文/附件分离 |
| `partition_epub` | EPUB | 电子书章节结构 |

**统一心智**（02 篇重申）：**函数名即格式，输出全是 Elements**——"下游代码零改动"是设计的核心价值；**每种格式的"脾气"**（专属参数与注意点）在本篇逐个过——**"换格式只换函数名，但格式差异要心里有数"**。

## 2. partition_pdf：头号选手

PDF 是 RAG 最常处理的格式，partition_pdf 参数最全：

```python
from unstructured.partition.pdf import partition_pdf

elements = partition_pdf(
    filename="report.pdf",
    strategy="auto",                    # 04 篇：策略四档
    infer_table_structure=True,         # 表格结构提取（HTML 表示）
    model_name="chipperv2",             # hi_res 的布局模型
    languages=["eng"],                  # OCR 语言
    include_page_breaks=True,           # 保留 PageBreak 元素（默认 False）
    extract_images_in_pdf=True,         # 提取 PDF 内嵌图片（图片 RAG 场景）
    extract_image_block_types=["Image", "Table"],  # 图片/表格块提取
)
```

**要点**：**表格结构**（infer_table_structure）是 PDF 的默认关注项（第 6 节）；**图片提取**（extract_images_in_pdf）是"图文 RAG"的入口（多模态场景）；**PageBreak 默认不保留**——需要页码分段时才开（03 篇过滤的对称操作）。**PDF 的两种形态决定策略**：文字层 PDF 走 fast/hi_res，纯图片 PDF 走 OCR——**"先看 PDF 是数字的还是扫描的"是 PDF 解析第一问**（04 篇）。

## 3. Office 三件套：docx/pptx/xlsx

**partition_docx**（Word）：利用 docx 的样式元数据（比 PDF 的启发式可靠）——**标题层级（Heading 1/2/3）直接映射为 Title 与层级信息**、列表/表格原生识别——**"Word 解析质量天然高于 PDF"**（有结构元数据）。

```python
from unstructured.partition.docx import partition_docx
elements = partition_docx(filename="guide.docx")
```

**partition_pptx**（PPT）：**每张幻灯片 = 一组 Elements**（标题 + 文本框 + 图片），`metadata["page_number"]` 即幻灯片序号——**"PPT 的页 = slide，不是纸张"**；要点：文本框的层级（title/body）与位置影响元素顺序（坐标排序适用）。

**partition_xlsx**（Excel）：**每个 sheet 解析为表格**（Table 元素，HTML 表示）——**"Excel 天生是表格，别让它在 RAG 里碎成单元格文本"**——`text_as_html` 是它的主要产出。**Office 三件套的共同点**：**有原生结构（样式/sheet/slide），解析质量稳定，无需 OCR**——"Office 文档是 RAG 里的优质输入"。

## 4. 网页与文本：html/md/txt

**partition_html**：**唯一支持 URL 抓取的分区器**（02 篇输入姿势）：

```python
from unstructured.partition.html import partition_html
elements = partition_html(url="https://example.com/docs/guide")
# 或本地文件
elements = partition_html(filename="page.html")
```

HTML 的标签语义（h1-h6/table/li）被映射为 Element 类型——**"网页的标题标签 = Title 元素"**；要点：**网页噪音**（导航/广告/页脚）解析后要清洗（06 篇）；**动态网页**（JS 渲染）它不处理——**"静态 HTML 用它，动态网页先渲染再喂它"**（爬虫体系 12-Python 爬虫的 Playwright 补位，00 篇分工）。

**partition_md**（Markdown）：标题/列表/代码块/表格的 Markdown 语法映射为元素——**"Markdown 是最友好的 RAG 输入"**（结构清晰、无解析歧义）。**partition_text**（TXT）：纯文本 + 启发式分类（空行分段/数字列表识别）——**"TXT 是下限：没有结构，全靠猜"**——`[all-docs]` 依赖里 txt/md/html 是轻量的。

**多格式统一处理的管道姿势**（05 篇的统一心智落地——"换格式只换函数名"的完整画面）：

```python
PARTITIONERS = {
    "pdf":  lambda p: partition_pdf(filename=p),
    "docx": lambda p: partition_docx(filename=p),
    "pptx": lambda p: partition_pptx(filename=p),
    "xlsx": lambda p: partition_xlsx(filename=p),
    "html": lambda p: partition_html(filename=p),
    "md":   lambda p: partition_md(filename=p),
    "txt":  lambda p: partition_text(filename=p),
}

def parse_any(path: str):
    ext = path.rsplit(".", 1)[-1].lower()
    return PARTITIONERS.get(ext, partition_text)(path)   # 未知格式兜底 txt

for doc in ["a.pdf", "b.docx", "c.xlsx"]:
    elements = parse_any(doc)      # 下游处理统一
```

**"一个 parse_any 走天下"**——注册表模式（10 篇实战策略匹配器的简化版）——**"格式的分发是注册表的事，业务代码只认 Elements"**。**注册表的扩展性**：新格式（如 EPUB）只加一行 `"epub": lambda p: partition_epub(filename=p)`——**"支持新格式 = 注册表加一行"**是这套模式的工程红利；**兜底设计**：未知格式回退 `partition_text`（至少拿到文本）——**"注册表要有兜底，管道才不因未知格式崩溃"**（与 10 篇实战 `pick_partitioner` 的 raise 策略二选一：兜底宽容 vs 显式报错，按管道容忍度选）。

## 5. 图片与邮件：image/email

**partition_image**（图片 OCR 专属）：

```python
from unstructured.partition.image import partition_image
elements = partition_image(filename="photo.png", strategy="ocr_only")
```

**图片必须 OCR**（没有文字层）——`ocr_only` 或 hi_res（模型理解版面）；**图片 = 扫描页的组成单元**（多张图 = 多页扫描）。**partition_email**（EML/MSG）：

```python
from unstructured.partition.email import partition_email
elements = partition_email(filename="mail.eml")
# 邮件头 → 元数据（sender/recipient/subject），正文 → 文本元素
```

**邮件解析的要点**：**头信息进 metadata**（发件人/收件人/主题——检索"谁发的邮件"用元数据过滤）、正文与附件分离（附件是独立文档，要单独分区）、**引用链**（回复邮件里的"上文"——正文清洗时处理）。**企业 RAG 里邮件是高频来源**（Connector 里有 Outlook/Gmail，08 篇）。

## 6. 表格提取深潜

**表格是 RAG 解析质量的重灾区与分水岭**——处理链条：

```python
# 第一步：提取（hi_res + infer_table_structure）
elements = partition_pdf(filename="tables.pdf",
                         strategy="hi_res",
                         infer_table_structure=True)

# 第二步：取表格元素的 HTML 结构
for el in elements:
    if el.category == "Table":
        print(el.metadata.get("text_as_html"))
        # <table><thead><tr><th>列1</th>...  ← 结构完整可解析
```

**表格提取的三层认知**：**fast 策略的表格是"碎裂文本"**（行结构丢失——RAG 召回率灾难）；**hi_res + infer_table_structure 产出 text_as_html**（行/列结构保留——**检索时表格作为结构化单元**）；**text_as_html 是 HTML 字符串**——后续可用 pandas.read_html 转 DataFrame 做进一步处理。**"表格的归宿是结构化表示（HTML/DataFrame），不是文本流"**是表格 RAG 的第一原则（03 篇 Table 类型的深化）。

**表格后处理的两种走向**（根据检索策略二选一或并用）：

```python
# 走向一：表格作为独立结构化单元进库（表格语义完整保留）
for el in elements:
    if el.category == "Table":
        html = el.metadata.get("text_as_html")
        records.append({"text": html, "is_table": True, ...})

# 走向二：表格转 DataFrame 做数值化处理（分析型下游）
import pandas as pd
dfs = [pd.read_html(el.metadata.get("text_as_html"))[0]
       for el in elements if el.category == "Table"]
```

**选型**：**问答型 RAG 用走向一**（表格 HTML 结构进向量库，检索时结构保留）；**分析型应用用走向二**（DataFrame 做聚合/计算）。**注意**：表格 HTML 直接进 embedding 的效果取决于模型对 HTML 的感知——**混合姿势"表格文本摘要 + 原表引用"是 2026 年表格 RAG 的进阶方案**（超出本体系范围，见 RAG 体系进阶）。

## 7. 常见坑

**坑一：xlsx/pptx 忘装 extras**——`[xlsx]`/`[pptx]` 缺失 ImportError；**按格式装依赖**（02 篇）。

**坑二：HTML 动态网页解析为空**——JS 渲染内容没有；**先渲染（Playwright）再 partition_html**（第 4 节）。

**坑三：表格用 fast 策略**——结构碎裂；**表格文档必 hi_res + infer_table_structure**（第 6 节）。

**坑四：邮件附件当正文**——附件是独立文档；**附件单独 partition**（第 5 节）。

**坑五：图片不 OCR**——partition_image 结果空；**图片必 strategy="ocr_only"**（第 5 节）。

## 8. 练习 5 题

1. partition 家族的统一心智？每种格式的"脾气"指什么？
2. partition_pdf 的五个专属参数各自解决什么？
3. Office 三件套为什么"解析质量天然高于 PDF"？
4. 网页与邮件的特殊注意点？动态网页怎么办？
5. 表格提取的三层认知？text_as_html 的用途？

> 🎯 **核心要点**：partition 家族 = **函数名即格式（统一输出）+ PDF 头号选手（策略 + 表格 + 图片）+ Office 靠原生结构 + 网页/邮件各有脾气 + 表格提取走 text_as_html**——"换格式只换函数名，但格式差异（OCR/样式/噪音）要心里有数"。

---

**下一模块**：[06-清洗与后处理.md](06-清洗与后处理.md) / **返回总览**：[00-unstructured总览.md](00-unstructured总览.md)
