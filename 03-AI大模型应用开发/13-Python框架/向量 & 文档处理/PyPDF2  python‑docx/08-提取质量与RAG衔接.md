# 08-提取质量与RAG衔接
> 定位：提取文本只是预处理的一半——清洗噪音、表格结构化、按语义切块、带溯源入库，这一篇把「PDF 文本」变成「RAG 可检索的干净语料」。

## 📚 目录
1. [从文本到语料：五步管道](#1-从文本到语料五步管道)
2. [噪音清洗](#2-噪音清洗)
3. [表格与结构信息](#3-表格与结构信息)
4. [按语义切块](#4-按语义切块)
5. [溯源：页码与元数据](#5-溯源页码与元数据)
6. [质量不足的升级路径](#6-质量不足的升级路径)
7. [常见坑](#7-常见坑)
8. [练习](#8-练习)

## 1. 从文本到语料：五步管道

extract_text 的产物是「文本流」——页眉页脚、页码、错误断行、表格散乱全混在一起，直接入库的检索效果打折。标准管道五步：**提取 → 清洗 → 结构化 → 切块 → 入库**。

```python
def pdf_to_chunks(path: str, max_chars: int = 800) -> list[dict]:
    reader = PdfReader(path)
    texts = []
    for i, page in enumerate(reader.pages):          # 1. 提取（带页码）
        texts.append((i, page.extract_text() or ""))
    cleaned = [clean_text(t) for _, t in texts]      # 2. 清洗
    sections = split_by_headings(cleaned)            # 3. 结构化（标题识别）
    chunks = chunk_sections(sections, max_chars)     # 4. 切块
    return [{"text": c, "source": path, "page": p} for c, p, _ in chunks]  # 5. 入库带溯源
```

每步的职责：提取保「字」、清洗去「噪」、结构化保「义」、切块控「粒度」、入库带「源」。五步管道是 RAG 预处理的最小骨架——和 [[../../强烈推荐（做项目必用，简历加分）/unstructured/00-unstructured总览|unstructured]] 的 Elements 模型（类型+内容+元数据）同构，本体系用双件套手搓，那边是现成引擎。

## 2. 噪音清洗

PDF 提取文本的四类典型噪音与清洗规则：

```python
import re

def clean_text(text: str) -> str:
    # 1. 页眉页脚/页码：行级规则（公司名、第 X 页）
    lines = [l for l in text.splitlines()
             if not re.match(r"^(第?\s*\d+\s*页?|Page\s+\d+|- \d+ -)$", l.strip())]
    # 2. 多余空白：错误断行合并（行尾无标点接下一行）
    text = " ".join(l.strip() for l in lines if l.strip())
    # 3. 控制字符与乱码符号
    text = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f]", "", text)
    text = text.replace("�", "")                  # 替换符
    # 4. 连续空行压缩
    return re.sub(r"\n{3,}", "\n\n", text)
```

清洗纪律：**规则要「稳」不要「准」**——宁可少清也不误伤正文（「第 1 章」不是页码）；**页眉页脚的规律从样本里总结**（每个文档源的页眉格式不同，规则按源配置）；**清洗是纯函数**（输入输出确定，可测试可复现——09 篇管道纪律）。乱码治理详见 03 篇五问。

## 3. 表格与结构信息

表格是 PDF 提取的重灾区——extract_text 把表格单元格按坐标顺序拼出来，行列关系全丢。三个升级路径：

**路径一：pypdf layout 模式**——间距保留下，肉眼可读但结构仍非结构化。

**路径二：pdfplumber 表格提取**——坐标级提取，`extract_table()` 直接给行列二维数组：

```python
import pdfplumber

with pdfplumber.open("table.pdf") as pdf:
    for page in pdf.pages:
        for table in page.extract_tables():
            for row in table:                      # row 是单元格列表
                print(row)
```

**路径三：unstructured / 云文档解析**——版面分析自动识别表格并转 HTML（`text_as_html`），RAG 场景的事实标准（`partition_pdf` 的表格处理，见 [[../../强烈推荐（做项目必用，简历加分）/unstructured/00-unstructured总览|unstructured 05 篇]]）。

选型：表格少而简单 → pdfplumber 手动；表格多且要进 RAG → unstructured；表格要进数据库 → pdfplumber 行列数组转 DataFrame 最直接。**表格的归宿决策**：检索场景「表格转 HTML 文本」保留结构语义（LLM 能读懂）；分析场景「转 DataFrame」进结构化存储——同一张表两条路，按下游需求选。

## 4. 按语义切块

切块是 RAG 检索质量的分水岭——块太大语义稀释、块太小上下文破碎。双件套场景的切块锚点按优先级：**书签标题 → 章节标题（正则识别）→ 页边界 → 定长**。

```python
def split_by_headings(cleaned_lines):
    """按「第 X 章 / 数字. 标题」行识别章节边界"""
    chunks, current = [], []
    for line in cleaned_lines:
        if re.match(r"^(第[一二三四五六七八九十百\d]+[章节]|\d+(\.\d+)*\s+\S)", line):
            if current: chunks.append("\n".join(current))
            current = [line]
        else:
            current.append(line)
    if current: chunks.append("\n".join(current))
    return chunks
```

分块参数经验值：**块大小 500-1000 字符**（中文约 1-2 个自然段）、**重叠 10-20%**（跨块语义不丢）；**标题作为块的锚点**（块首是标题，检索命中标题即命中章节）。结构化文档（docx）切块更简单——**段落即天然块**：`[p.text for p in doc.paragraphs if p.text.strip()]` 是 docx 场景最优切分（段落是语义最小完整单位）。切块是 [[../../向量%20&%20文档处理/chromadb/00-chromadb总览|chromadb]] 入库的前置——块是向量化的最小单元。

**切块参数的调优方法**（别抄参数，要会调）：先定「查询形态」——用户问「报告里的具体数字」要小块（500 字符内，命中精确）；问「章节总结」要大块（1000+ 字符，上下文完整）。**验证靠检索回测**：拿 20 个真实问题去检索，看「答案所在块是否被召回」——召回率不达标就调（块小了切碎答案、块大了稀释相似度）。**重叠的作用**：块边界切在句子中间时，重叠让语义不丢在接缝——中文按句号边界切块（`re.split(r"(?<=[。！？])")`）比纯字符切更聪明。**分块与向量化的配合**：块是「文本单元」，向量化是「块 → 向量」——块边界先于向量化决定，切完块再 embed（embedding 模型见 [[../../../05-AI开发框架|AI 开发框架]] 体系）。

## 5. 溯源：页码与元数据

RAG 回答必须能「回到原文」——向量库的 metadata 字段是溯源载体：

```python
chunks = []
for i, page in enumerate(reader.pages):
    text = page.extract_text() or ""
    for chunk in chunk_text(text, size=800):
        chunks.append({
            "text": chunk,
            "metadata": {
                "source": "report.pdf",
                "page": i + 1,                        # 页码（1 基）
                "title": meta_title,                  # 文档元数据
                "author": meta_author,
                "chunk_index": len(chunks),
            },
        })
```

溯源三要素：**source**（文件路径/URL——回答引用「来自哪个文件」）、**page**（定位到页——人工复核的入口）、**title/author**（文档级元数据——来源可信度判断）。检索时 metadata 一并返回，回答带上引用就是「有据可查」。入库用 [[../../向量%20&%20文档处理/chromadb/00-chromadb总览|chromadb]] 的 `add(documents, metadatas)`——metadata 与文本同存。

## 6. 质量不足的升级路径

管道跑完发现质量不足时，按成本从低到高升级：**换 layout 模式**（一行改动，版面顺序改善）→ **换 pdfplumber**（坐标级，表格与多栏改善）→ **OCR**（扫描件必选，03 篇 300dpi + chi_sim）→ **unstructured 全套**（版面分析 + 表格结构化 + 元素类型，RAG 预处理终极形态）→ **云文档解析服务**（最高质量，付费）。**升级的决策依据是 03 篇的质量评估**——先量化再升级，别凭感觉换工具；每次升级后重跑评估对比，数字说话。

## 7. 常见坑

**坑一：提取完直接入库**。噪音 + 无结构 + 无溯源——检索质量打折，五步管道走全。

**坑二：清洗规则误伤正文**。页码正则把「第 1 章」清了——规则按样本校准，宁少勿多。

**坑三：表格当文本拼**。行列关系丢失——表格走 pdfplumber/unstructured 结构化。

**坑四：定长切块不管语义**。标题/段落锚点比定长切质量高——先结构后长度。

**坑五：溯源只有 source 没 page**。回答引用「报告.pdf」无法定位——page 是人工复核入口。

**坑六：升级不评估**。换工具全凭感觉——评估先行，数字决策。

## 8. 练习

1. 五步管道每步的职责是什么？
2. 清洗的「稳 vs 准」怎么权衡？
3. 表格的三条升级路径与选型标准？
4. 切块锚点的优先级是什么？docx 为什么段落即块？
5. 溯源三要素是什么？为什么 page 不能少？

> 🎯 **核心要点**：五步管道（提取→清洗→结构化→切块→入库）；清洗规则宁稳勿准；表格走结构化工具；切块先语义锚点后定长；metadata 带 source/page；质量不足按成本升级且评估驱动。

---

**下一模块**：[09-生产实践与批量处理](09-生产实践与批量处理.md)｜**返回总览**：[00-pypdf与python-docx总览](00-pypdf与python-docx总览.md)
