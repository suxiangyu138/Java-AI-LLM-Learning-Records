# PDF 解析深潜

> PDF 是最难解析的常见格式（内容按坐标存而非逻辑顺序），也是 Agent 文档输入的主力。本章讲透：文本层提取、Docling 结构化、扫描 PDF 陷阱与 2026 基准位次。

## 1. PDF 的本质：坐标不是逻辑

```text
PDF 内部 = 页面 + 坐标对象（文本/图形/图片），没有"段落/表格"概念
  → 提取器要自己恢复：阅读顺序、段落边界、表格结构、标题层级
```

| 特点 | 后果 |
|---|---|
| 按坐标存字 | 双栏、分页会打乱阅读顺序 |
| 同一文件两种"面孔" | 数字化 PDF 有文本层可直提；扫描 PDF 是纯图像——**同一 .pdf 扩展名下是两种完全不同的解析任务**，所有 PDF 处理流水线的第一步都必须是"探测文本层" |

PDF 的类型学（解析决策的地基）：

```text
数字化 PDF（文本层存在）→ 提取器直接读，快且免费
扫描 PDF（无文本层）   → OCR/VLM，慢且贵
混合 PDF（部分页扫描）  → 页面级路由（每页单独判定）
```
| 表格是"画"的线+字 | 需要专门算法恢复行列结构 |
| 无语义标签 | 标题/正文全靠版面推断 |
| 字体嵌入 | 字库缺失会乱码/缺字 |
| 可能纯图像 | 扫描 PDF 无文本层，直接提取=空 |

## 2. 文本层提取三兄弟

| 库 | 定位 | 优点 | 缺点 |
|---|---|---|---|
| PyMuPDF (fitz) | PDF 工程库 | 最快（0.01-0.05s/页）、定位/合并/脱敏全能 | AGPL、表格弱、无 OCR |
| pdfplumber | 版面细节提取 | 表格/坐标精细、可调试 | 慢（0.5-2s/页） |
| pdfminer.six | 底层解析 | 文本/布局信息全 | 速度慢、上手重 |

```python
import fitz

doc = fitz.open("report.pdf")
text = "\n".join(page.get_text("text") for page in doc)   # 快路径：文本层
# 注意：扫描 PDF 的 get_text() 返回空字符串——不报错，静默丢内容
if not text.strip():
    raise NeedsOcrError("PDF 无文本层，需走 OCR/VLM 路由")   # 显式拦截！
```

> ⚠️ **扫描 PDF 陷阱**：PyMuPDF 等对扫描页返回空文本而不报错——这是"静默丢内容"的最大来源。生产代码必须检测"提取结果为空/过短"并显式路由到 OCR/VLM（09 模块工具链落地）。

## 3. pymupdf4llm：快路径的 Markdown 出口

```python
import pymupdf4llm

md = pymupdf4llm.to_markdown("report.pdf")      # 文本层 → Markdown
# 优点：快、阅读顺序好（NID 0.89-0.905）
# 局限：表格 TEDS 仅 0.54-0.61（表格会摊平成文本）、无 OCR、AGPL
```

| 适用 | 不适用 |
|---|---|
| 纯文本论文/报告/合同（数字化） | 复杂表格（财报/发票） |
| 大批量低价路径 | 扫描件 |
| 不需要版面元素的场景 | 需要图表/公式结构 |

## 4. Docling：开源结构化王者（MIT）

```python
from docling.document_converter import DocumentConverter

converter = DocumentConverter()
result = converter.convert("annual_report.pdf")
doc = result.document

md = doc.export_to_markdown()      # 结构完整的 Markdown（标题层级/表格/列表）
json_ = doc.export_to_dict()       # 带版面元素/坐标的 JSON
```

| 能力 | 说明 |
|---|---|
| 表格 TEDS 0.89 | 开源最强表格结构恢复 |
| 阅读顺序 NID 0.89 | 双栏/复杂版面还原好 |
| 格式覆盖 | PDF/DOCX/XLSX/HTML → Markdown/JSON |
| 数学公式 | 可输出 LaTeX |
| 代价 | 0.3-3s/页、~500MB 模型、首次加载 5-10s、弱 OCR |

> 💡 Docling 是"表格重、允许慢"场景的开源首选（MIT 无许可风险）——财报、技术手册、合同扫描补 OCR 后都很适合。

## 5. 基准位次：2026 年各引擎怎么排

| 工具 | 综合 | 表格 | 速度(s/页) | 结论 |
|---|---|---|---|---|
| Nutrient API | 0.93 | 0.71-0.94 | 网络 | 商用最高 |
| Docling | 0.877-0.89 | 0.89 | 0.3-3 | 开源准王 |
| 路由混合 | ~0.905 | 混合 | 混合 | 免费最强 |
| Unstructured | 0.788-0.89 | 0.70-0.86 | 中 | 企业 API |
| pymupdf4llm | 0.80-0.86 | 0.54-0.61 | 0.01-0.05 | 快路径 |

## 6. 实战决策：一份 PDF 怎么选路

```text
PDF 到达
 ├─ 无文本层？ → OCR/VLM 路由（05/06 模块）
 ├─ 文本层 + 表格少？ → pymupdf4llm（快）
 ├─ 文本层 + 表格重？ → Docling（准）
 ├─ 双栏/复杂版面？ → Docling 或 VLM
 └─ 每页混合？ → 路由式混合（02 篇 6 节）
```

| 场景 | 推荐 |
|---|---|
| 批量导入论文库 | pymupdf4llm 快路径（纯文本多） |
| 论文含公式 | Docling（公式转 LaTeX）或 VLM |
| 财报/招股书 | Docling（表格 TEDS 0.89） |
| 扫描合同 | PaddleOCR-VL 或 VLM（05/06） |
| 混合档案库 | 路由混合（pdfmux/OpenDataLoader 方案） |

## 7. PDF 解析常见坑速查

| 坑 | 现象 | 解法 |
|---|---|---|
| 扫描页静默空文本 | 提取为空不报错 | 显式检测长度 + 路由 OCR |
| 双栏顺序错乱 | 左栏右栏穿插 | Docling/VLM（有版面理解） |
| 表格摊平 | 行/列关系丢失 | 表格专用引擎或表格工具（07） |
| 页眉页脚混入正文 | 每页重复噪音 | 版面分析剔除（Docling 可配置） |
| 字体缺失乱码 | 提取出现 □/乱码 | 换解析器（pdfminer 兜底）或 OCR |
| 加密 PDF | 提取报错 | 解密（有密码）/ 拒绝（无密码），并向 Agent 返回"需要密码"状态 |
| 字体子集化 | 复制文本缺字/乱码 | 换 pdfminer 或 OCR 兜底 |
| 超大 PDF | 内存暴涨/超时 | 分页流式处理 + 页数上限 |

> 🎯 核心要点：PDF 解析的第一纪律是**"先判断有没有文本层"**——有则快库直取，无则 OCR/VLM；第二纪律是**"表格必须走专用引擎"**——任何通用提取器摊平表格的结果都不值得喂给 RAG。

## 8. 完整实战：一份财报 PDF 的处理链路

```python
import fitz, pymupdf4llm
from docling.document_converter import DocumentConverter

def parse_financial_pdf(path: str) -> dict:
    doc = fitz.open(path)
    pages = len(doc)
    sample = "\n".join(doc[p].get_text() for p in range(min(3, pages)))

    # 1. 文本层探测：空/过短 → 扫描件，路由 OCR（05 篇）
    if len(sample.strip()) < 50:
        return {"status": "needs_ocr", "pages": pages,
                "hint": "扫描件：调用 ocr_page 逐页识别（PaddleOCR-VL）"}

    # 2. 表格密度启发式：财报必重 → 直接 Docling（准）
    md = DocumentConverter().convert(path).document.export_to_markdown()

    # 3. 质量门：表格摊平检测（无 | 分隔行 → 警告）
    if "|" not in md:
        return {"status": "warning", "markdown": md,
                "hint": "检测不到表格结构，建议换引擎重试"}
    return {"status": "ok", "markdown": md, "pages": pages}
```

| 环节 | 对应纪律 |
|---|---|
| 文本层探测 | 第一纪律（防静默空文本） |
| 表格密度→Docling | 第二纪律（表格专用引擎） |
| 质量门（表格检测） | 显式失败（09 篇工具链原则） |

## 9. 与 VLM 直读的配合边界

| 情况 | 走哪条路 |
|---|---|
| 数字化文本 PDF | PyMuPDF/Docling（免费确定） |
| 扫描件 | PaddleOCR-VL（比云端 VLM 便宜可靠） |
| 手写/超复杂/解析器反复失败 | VLM 直读（06 篇） |
| 单页问答 | 直接 VLM 问答（06 篇 7 节），不经过全量解析 |

## 10. 加密、损坏与超大 PDF

| 情况 | 检测 | 处理 |
|---|---|---|
| 加密（无密码） | `doc.needs_pass` | 返回"需要密码"，不硬破 |
| 加密（有密码） | `doc.authenticate(pwd)` | 提供密码则解密后解析 |
| 损坏文件 | 打开/读取抛异常 | 显式报错，提示用户重新上传 |
| 超大（>500 页） | `doc.page_count` | 分页流式 + 返回"已处理前 N 页" |
| 混合（含扫描页） | 逐页试读 | 页面级路由（文本页快提取、扫描页 OCR） |

```python
def handle_pdf_edge_cases(path: str) -> dict:
    doc = fitz.open(path)
    if doc.needs_pass:
        return {"status": "encrypted", "hint": "请提供文档密码"}
    if doc.page_count > 500:
        return {"status": "too_large", "pages": doc.page_count,
                "hint": "将分页处理，请确认范围"}
    # 页面级路由：文本页 vs 扫描页混合处理
    plan = [probe_page(doc, p) for p in range(doc.page_count)]
    return {"status": "ok", "page_plan": plan}
```

> 💡 页面级路由是"混合文档"（既有文本页又有扫描页）的唯一正解——整体判定会浪费一半页面的处理路径。混合文档在现实里很常见（扫描合同后追加的打印附件、批量导入的档案包），页面级路由能省 30-60% 的 OCR 成本。

---

**下一模块**：[04-Office 文档解析](04-Office文档解析.md)　**返回总览**：[00-文档解析总览](00-文档解析总览.md)

## 参考来源

- [Which PDF extractor should you use? An honest guide.（pdfmux）](https://pdfmux.com/blog/which-pdf-extractor-should-you-use/)
- [Best PDF extraction library for Python in 2026 (benchmarked)（pdfmux）](https://pdfmux.com/blog/best-pdf-extraction-library-python/)
- [Best PDF Parsing Tools for RAG in 2026（fast.io）](https://fast.io/resources/best-pdf-parsing-tools-rag/)
- [面向开发者的最佳 AI 文档解析器（LlamaIndex）](https://llamaindex.org.cn/insights/best-ai-document-parsers)
- [Data Extraction API accuracy benchmarks（Nutrient）](https://www.nutrient.io/api/data-extraction-api/benchmarks/)
