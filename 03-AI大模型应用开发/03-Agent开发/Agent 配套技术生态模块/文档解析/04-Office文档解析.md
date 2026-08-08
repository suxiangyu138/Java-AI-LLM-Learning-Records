# Office 文档解析

> DOCX/PPTX/XLSX 的本质是 ZIP+XML——解析比 PDF 友好得多（结构天然存在）。挑战在于：样式嵌套、公式、文本框与合并单元格。本章给四件套库速查与实战要点。

## 1. Office 三件套的本质：ZIP 里的 XML

| 格式 | 结构 | 内容位置 |
|---|---|---|
| DOCX | ZIP + word/document.xml | 段落/表格/样式全在 XML |
| PPTX | ZIP + ppt/slides/*.xml | 每页一个 XML，文本框散布 |
| XLSX | ZIP + xl/worksheets/*.xml | 单元格按行列存 |

```python
# 一切 Office 解析的底层：直接用 zipfile 也能读
import zipfile, re
with zipfile.ZipFile("doc.docx") as z:
    xml = z.read("word/document.xml").decode("utf-8")
    texts = re.findall(r"<w:t[^>]*>(.*?)</w:t>", xml)   # 简易提取
```

> 🎯 核心要点：**Office 文档的结构是"本来就存在"的**（不像 PDF 要恢复）——解析的关键是"按结构走"，而不是"猜结构"。用对了库，DOCX 的结构保留度天然高于 PDF。

## 2. 四件套库速查

| 库 | 格式 | 能力 | 备注 |
|---|---|---|---|
| python-docx | DOCX | 段落/表格/样式/页眉页脚 | 只读+写 |
| python-pptx | PPTX | 文本框/形状/表格 | 读取散落文本框需遍历 |
| openpyxl | XLSX | 单元格/合并单元格/公式 | 只读模式性能好 |
| BeautifulSoup/lxml | HTML | 标签树解析 | 需去噪（导航/广告） |
| mammoth | DOCX→HTML/Markdown | 一键转换 | 保留标题/表格/列表 |

```python
# python-docx：段落 + 表格两路提取
import docx

doc = docx.Document("report.docx")
paras = [p.text for p in doc.paragraphs if p.text.strip()]
tables = [[[c.text for c in row.cells] for row in t.rows] for t in doc.tables]
```

## 3. PPTX：文本框是主要陷阱

```python
from pptx import Presentation

prs = Presentation("slides.pptx")
texts = []
for i, slide in enumerate(prs.slides):
    parts = []
    for shape in slide.shapes:
        if shape.has_text_frame:
            parts.append(shape.text_frame.text)
        if shape.has_table:
            parts.append(table_to_markdown(shape.table))   # 表格单独处理
    texts.append(f"## 第{i+1}页\n" + "\n".join(parts))
```

| 陷阱 | 表现 | 对策 |
|---|---|---|
| 文本框散落 | 同一页多个小框，顺序无保证 | 按位置排序（top 坐标） |
| 图形/SmartArt 无文本层 | 信息丢失 | 截图走 OCR/VLM（05/06） |
| 演讲者备注 | 备注中有内容 | `slide.notes_slide` 读取 |
| 母版/占位符 | 提取到重复占位符文本 | 过滤空与占位符 |

## 4. XLSX：合并单元格与公式

```python
from openpyxl import load_workbook

wb = load_workbook("data.xlsx", read_only=True, data_only=True)  # data_only: 取计算值
ws = wb["Sheet1"]
rows = [[cell.value for cell in row] for row in ws.iter_rows()]
```

| 场景 | 处理 |
|---|---|
| 合并单元格 | 只有左上角有值——用 merged_cells 范围回填 |
| 公式 | `data_only=True` 取缓存值；无缓存需用 LibreOffice 重算 |
| 大文件 | `read_only=True` 流式读取，防内存爆炸 |
| 多 sheet | 按 sheet 名分别结构化，保留 sheet 名作元数据 |

> 💡 XLSX 解析后建议**保持表格式输出（Markdown 表格/JSON）**而非摊成纯文本——这是 [07 模块](07-表格与版面结构化.md) 的核心原则，表格数据线性化会毁掉行列关系。

## 5. HTML：去噪是第一优先级

```python
from bs4 import BeautifulSoup

soup = BeautifulSoup(html, "lxml")
for tag in soup(["script", "style", "nav", "footer", "header"]):
    tag.decompose()                    # 去导航/广告/脚本
main = soup.find("article") or soup.body
md = main_to_markdown(main)            # 标题→#、表格→| |
```

| 噪音来源 | 去法 |
|---|---|
| 导航/页脚/广告 | 标签黑名单 decompose |
| 图片 alt 冗余 | 过滤无意义 alt |
| 嵌套过深 | 折叠无内容容器 |
| 动态内容（JS 渲染） | 需要浏览器渲染（爬虫体系[阶段5]的做法） |

## 6. 公式：OMML 与 LaTeX

| 格式 | 公式存储 | 提取 |
|---|---|---|
| DOCX | OMML（MathML 变体） | python-docx 读 m:oMath → 转 LaTeX（latex2mathml 反向工具链） |
| PDF | 字体+坐标 | Docling/PaddleOCR-VL 直接出 LaTeX |
| 扫描件 | 图像 | OCR 公式识别（PaddleOCR-VL 支持公式转 LaTeX） |

> 💡 论文/教材场景公式必须转 LaTeX 才可检索可引用——纯文本提取会把公式变成乱码。

## 7. WPS 与兼容性

| 情况 | 处理 |
|---|---|
| WPS 生成的 .docx | 标准 OOXML，python-docx 可读（个别私有扩展忽略） |
| .doc（老格式） | python-docx 不支持——用 LibreOffice headless 转换或 antiword |
| .ppt/.xls（老格式） | 同上，`soffice --headless --convert-to pptx` 先转换 |
| 加密文档 | 拒绝处理或引导解密 |

```bash
# LibreOffice 万能转换：老格式 → 新格式（服务端安装即可）
soffice --headless --convert-to docx --outdir /tmp/input/ legacy.doc
```

> 🎯 核心要点：Office 解析的成功率排序是 XLSX > DOCX > PPTX > 老格式——**遇到老格式（.doc/.ppt/.xls）先转新格式再解析**，别自己写解析器。表格类（XLSX）输出保持表格结构，这是下游 RAG/入库的前提。

## 8. 端到端：批量 Office → Markdown 入库前置格式

```python
def office_to_markdown(path: str) -> str:
    ext = Path(path).suffix.lower()
    if ext == ".docx":
        return docx_to_md(path)            # python-docx：段落+表格
    if ext == ".pptx":
        return pptx_to_md(path)            # python-pptx：按页+位置排序
    if ext == ".xlsx":
        return xlsx_to_md(path)            # openpyxl：表格保持
    if ext in (".doc", ".ppt", ".xls"):    # 老格式先转换
        subprocess.run(["soffice", "--headless", "--convert-to",
                        ext[1:] + "x", "--outdir", str(tmp), path])
        return office_to_markdown(tmp / (Path(path).stem + ext + "x"))
    raise UnsupportedFormatError(ext)
```

```python
def xlsx_to_md(path: str) -> str:
    wb = load_workbook(path, read_only=True, data_only=True)
    parts = [f"# {Path(path).stem}"]
    for ws in wb.worksheets:
        rows = [list(r) for r in ws.iter_rows(values_only=True)]
        rows = fill_merged(ws, rows)               # 合并单元格回填
        parts.append(f"## {ws.title}\n" + to_markdown_table(rows))
    return "\n\n".join(parts)
```

## 9. Office 解析常见坑速查

| 坑 | 现象 | 解法 |
|---|---|---|
| 合并单元格丢值 | 只有左上角有值 | merged_cells 范围回填 |
| 公式无缓存值 | openpyxl 取到 None | data_only=True；无缓存用 LibreOffice 重算 |
| PPT 文本框乱序 | 阅读顺序乱 | 按 top 坐标排序 |
| 老格式读不了 | 抛异常 | soffice 先转新格式 |
| 大 xlsx 内存爆 | 加载卡死 | read_only=True 流式 |
| HTML 噪音多 | 导航/广告混入 | BeautifulSoup decompose 黑名单 |
| 文档损坏 | zipfile 异常 | 显式报错，不重试 |

## 10. Office 文档元数据提取

| 元数据 | python-docx | python-pptx | openpyxl |
|---|---|---|---|
| 标题 | `doc.core_properties.title` | 同左（通用属性） | `wb.properties.title` |
| 作者 | `core_properties.author` | 同左 | `wb.properties.creator` |
| 创建时间 | `core_properties.created` | 同左 | `wb.properties.created` |
| 修改时间 | `core_properties.modified` | 同左 | `wb.properties.modified` |
| 页数/行数 | 段落计数 | slide 计数 | sheet 列表 |

```python
def extract_meta(path: str) -> dict:
    doc = docx.Document(path)
    cp = doc.core_properties
    return {"title": cp.title, "author": cp.author,
            "created": cp.created.isoformat() if cp.created else None,
            "paragraphs": len(doc.paragraphs), "tables": len(doc.tables)}
```

> 💡 元数据是 RAG 检索的过滤键（作者/时间/文档类型）——Office 文档的元数据免费可得（XML 里现成），务必随正文一起入库（衔接 08 篇元数据清单）。

Office 解析的最终心智：**"格式是外壳，XML 是本体"**——一切解析都是读 XML 树，遇到任何奇怪格式问题，先解压看一眼 document.xml 就懂了（`unzip -p file.docx word/document.xml | head`）。

---

**下一模块**：[05-OCR：扫描件与图像文档](05-OCR：扫描件与图像文档.md)　**返回总览**：[00-文档解析总览](00-文档解析总览.md)

## 参考来源

- [Best AI Document Parsers for Developers（LlamaIndex）](https://www.llamaindex.cloud/insights/best-ai-document-parsers)
- [Document AI: From OCR to Agentic Doc Extraction（DeepLearning.AI）](https://learn.deeplearning.ai/courses/document-ai-from-ocr-to-agentic-doc-extraction/)
- [Document Ingestion Fundamentals（KodeKloud）](https://notes.kodekloud.com/docs/Fundamentals-of-RAG/Document-Processing-and-Chunking/Document-Ingestion-Fundamentals/page)
