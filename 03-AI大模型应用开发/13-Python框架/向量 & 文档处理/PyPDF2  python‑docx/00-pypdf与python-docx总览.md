# pypdf 与 python-docx 知识体系总览
> 一句话定位：Python 文档处理双件套——pypdf 管 PDF 读写、python-docx 管 Word 读写，RAG 数据预处理与报告生成的基础底座；注意 PyPDF2 已死，2026 一律用 pypdf。

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [常见误区](#5-常见误区)
6. [一周计划](#6-一周计划)
7. [自测题](#7-自测题)
8. [参考来源](#8-参考来源)

## 1. 知识体系导图

```text
pypdf 与 python-docx（文档处理双件套）
├── 01 是什么（定位/2026基线/PyPDF2死亡史/替代品对比）
├── 02 安装与快速开始（锁版本/十分钟双库闭环）
├── pypdf（PDF 读写）
│   ├── 03 文本提取（PdfReader/extract_text/质量真相）
│   ├── 04 合并拆分与编辑（PdfWriter/页面操作/加密）
│   └── 05 表单与元数据（AcroForm/书签/注解/结构）
├── python-docx（Word 读写）
│   ├── 06 文档模型（Document/段落/run/样式）
│   └── 07 进阶（表格/图片/页眉页脚/注释）
├── 08 提取质量与 RAG 衔接（评估/表格/清洗/管道）
├── 09 生产实践（批量/异常/与unstructured分工）
└── 10 生产实战与自测（批量提取+报告生成/毕业验收）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 是什么 | 双库定位、PyPDF2→pypdf 演进、基线 | 所有人 |
| 02 | 安装与快速开始 | 锁版本、十分钟读写闭环 | 新手 |
| 03 | PDF 文本提取 | PdfReader、extract_text、乱码治理 | 必须 |
| 04 | PDF 合并编辑 | PdfWriter、拆分旋转、加密 | 必须 |
| 05 | PDF 表单元数据 | AcroForm、书签、PDF 结构 | 进阶 |
| 06 | docx 文档模型 | 段落/run/样式、打开新建 | 必须 |
| 07 | docx 进阶 | 表格/图片/页眉页脚/注释 | 进阶 |
| 08 | 提取质量与 RAG | 评估、表格、清洗、管道衔接 | 必须 |
| 09 | 生产实践 | 批量、异常、unstructured 分工 | 进阶 |
| 10 | 实战与自测 | 批量提取+报告生成、20 题 | 毕业 |

## 3. 学习路线推荐

**路线一：快速上手（1-2 天）**——01 → 02 → 03 → 06 → 10。目标：能读 PDF 文本、能生成 Word 文档，处理日常文档任务。

**路线二：文档处理全栈（3-5 天）**——路线一 + 04 → 07。目标：PDF 合并拆分加密、docx 表格图片页眉全掌握，能写批量处理脚本。

**路线三：RAG 数据预处理（一周）**——路线二 + 05 → 08 → 09。目标：搭建文档→文本→清洗→分块的预处理管道，衔接 [[../../强烈推荐（做项目必用，简历加分）/unstructured/00-unstructured总览|unstructured]] 与向量库。

**先厘清分工**：本体系是「单文档精细处理」工具课——PDF 提取哪页、Word 加个表格；批量文档解析（25+ 格式、版面分析、表格结构化）是 [[../../强烈推荐（做项目必用，简历加分）/unstructured/00-unstructured总览|unstructured 体系]] 的领域，它内部正依赖 pypdf 这类底层库。RAG 检索侧见 [[../../向量%20&%20文档处理/chromadb/00-chromadb总览|chromadb]] 与 [[../../向量%20&%20文档处理/faiss‑cpu%20%20faiss‑gpu/00-faiss-cpu总览|faiss]]。

**为什么这套体系值得学**：文档处理是 AI 应用里最「脏」的活——PDF 没有统一的数据模型，提取质量看 PDF 来源脸色；docx 的格式细节藏在 XML 里，一个字体设置不对整篇样式崩。但同时它是 RAG 的第一公里：知识库里的企业文档、报告、合同，绝大多数以 PDF/Word 形态存在。学会双件套，你就有了「把任意文档变成可检索语料」的能力，也有了「把数据变成专业文档」的能力——一进一出，贯穿整个数据流。十篇正文按「PDF 三篇 + Word 两篇 + 管道三篇 + 实战一篇」展开，覆盖读写两端与生产全流程。

## 4. 核心概念速查

| 概念 | 一句话 |
|------|-------|
| pypdf | PyPDF2 的继任者，纯 Python PDF 读写库，6.15.0 |
| PdfReader | 读 PDF 入口：`PdfReader(path)` → `.pages` → `extract_text()` |
| PdfWriter | 写 PDF 入口：合并、拆分、旋转、加密 |
| pdfplumber | 更精细的文本/表格提取（坐标级），与 pypdf 分工 |
| python-docx | Word .docx 读写库（非 .doc），1.2.0 |
| Document | docx 顶层对象：`Document(path)` 或新建 |
| paragraph/run | 段落与文本片段：run 是样式的最小单位 |
| styles | 样式体系：段落样式/字符样式，模板化写作核心 |
| AcroForm | PDF 表单：`reader.get_fields()` 读取填写 |
| 增量写入 | 在原文上追加修改而非重写，保留签名 |
| extract_text | 文本提取方法，质量取决于 PDF 是否含文本层 |
| 乱码治理 | 编码/字体问题导致提取乱码的排查路径 |
| 分块衔接 | 提取文本 → 清洗 → 按结构分块 → 向量化 |
| 批量管道 | 目录遍历 → 提取 → 结构化输出 → 日志留痕 |

## 5. 常见误区

**误区一：还在 import PyPDF2**。PyPDF2 2023 年弃用、2024 年停止维护，Python 3.12+ 文本提取静默失败，还带着未修补的 CVE——`pip uninstall PyPDF2 && pip install pypdf`，导入改成 `from pypdf import ...`。

**误区二：`PdfFileReader`/`getPage()` 老 API**。3.0 起是 `PdfReader`/`PdfWriter`，5.0 移除了 `getPage`/`numPages`——看到 camelCase 代码就是老教程，别抄。

**误区三：PDF 提取质量都一样**。PDF 是排版格式不是文本格式——扫描件没有文本层，extract_text 提取出来是空的或乱码；「提取质量」由 PDF 本身决定，工具只能挖不能造。

**误区四：python-docx 能处理 .doc**。只能读写 .docx（Office 2007+ XML 格式），老 .doc 是二进制格式，要用 LibreOffice 转换或换库。

**误区五：docx 和 PDF 可以互转**。两个库都不能直接互转——pypdf 只吃 PDF，python-docx 只吃 docx；互转要中间件（LibreOffice 命令行）。

**误区六：Word 里有的格式 docx 都能读**。python-docx 覆盖常用格式（段落/表格/图片/页眉页脚），文本框、复杂域、宏等不支持——读不了不一定是代码错。

**误区七：提取完直接喂 RAG**。PDF 提取的文本带页眉页脚、页码、乱码、错误断行——不清洗不分块直接入库，检索质量打折，08 篇讲完整预处理。

**误区八：以为提取是「复制粘贴」**。PDF 提取是字形还原，顺序、空白、字体都可能出错——提取结果必须按 03 篇评估，不是「能跑就行」。

七个误区的共同根源：**把文档处理当成「文件 IO」**——以为读 PDF 像读 txt 一样干净。PDF 是排版格式、docx 是 XML 封装，两者都需要「懂格式的库 + 懂质量的工程」。带着「IO 心态」写文档代码，格式坑、质量坑、版本坑一个都躲不掉；换成「格式工程」心智——先懂模型再操作、先评估再入库、先隔离再批量——一切行为都有了解释。

## 6. 一周计划

| 天 | 内容 | 产出 |
|:---:|------|------|
| Day1 | 01 + 02：安装与双库闭环 | 读一个 PDF + 生成一个 docx |
| Day2 | 03：PDF 文本提取 | 批量提取 + 乱码排查 |
| Day3 | 04：PDF 编辑 | 合并拆分加密脚本 |
| Day4 | 06 + 07：docx 文档模型 | 生成带表格图片的报告 |
| Day5 | 08：质量与 RAG 衔接 | 预处理管道 + 分块 |
| Day6 | 09：生产实践 | 批量管道 + 日志 |
| Day7 | 10：综合实战 + 自测 | 提取+报告生成 + 20 题 |

## 7. 自测题

1. PyPDF2 为什么不能用？迁移怎么做？
2. `PdfReader` 与 `PdfWriter` 的分工？
3. extract_text 提取为空的可能原因？
4. 扫描件 PDF 怎么提取文本？
5. python-docx 能读 .doc 吗？
6. run 与 paragraph 的关系？
7. 样式在 python-docx 里的作用？
8. PDF 表格提取的正确工具是什么？
9. 提取文本进 RAG 前要做什么？
10. pypdf 与 pdfplumber 怎么分工？

## 8. 参考来源

- [pypdf 官方文档](https://pypdf.readthedocs.io/)
- [pypdf GitHub Releases（6.15.0 2026-08-06）](https://github.com/py-pdf/pypdf/releases)
- [python-docx 官方文档](https://python-docx.readthedocs.io/)
- [PyPDF2 弃用说明（迁移指南）](https://theneuralbase.com/document-ai/errors/pypdf2-deprecated-use-pypdf-pdfplumber/)
- [PyPDF2 CHANGELOG（历史存档）](https://raw.githubusercontent.com/py-pdf/PyPDF2/main/CHANGELOG.md)

---

**下一模块**：[01-pypdf与python-docx是什么](01-pypdf与python-docx是什么.md) → 从双库定位与 PyPDF2 死亡史开始。
