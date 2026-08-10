# 01-pypdf与python-docx是什么
> 定位：Python 文档处理双件套——pypdf 管 PDF 读写的纯 Python 库、python-docx 管 Word 文档的读写库；2026 年 PDF 侧唯一维护线是 pypdf，PyPDF2 只是历史曾用名。

## 📚 目录
1. [双件套的分工](#1-双件套的分工)
2. [PyPDF2 死亡史与 pypdf 诞生](#2-pypdf2-死亡史与-pypdf-诞生)
3. [2026 基线](#3-2026-基线)
4. [解决什么问题](#4-解决什么问题)
5. [替代品对比](#5-替代品对比)
6. [与相邻体系的分工](#6-与相邻体系的分工)
7. [五条边界](#7-五条边界)
8. [练习](#8-练习)

## 1. 双件套的分工

文档处理的两大战场是 PDF 与 Word——**pypdf 管 PDF，python-docx 管 Word**，各管一端，互不越界。

pypdf 的能力线：读取 PDF（按页提取文本、元数据、表单字段、书签）、写入 PDF（合并、拆分、旋转、裁剪、插入页面、加密解密）。它不解析版面（哪段是标题哪段是正文）——那是 [[../../强烈推荐（做项目必用，简历加分）/unstructured/00-unstructured总览|unstructured]] 的活；它做的是「PDF 文件层面的操作」。

python-docx 的能力线：读取与生成 .docx 文档——段落、run、样式、表格、图片、页眉页脚、注释（1.2.0 新增）。它的独特价值是「程序化生成 Word 文档」：报告、合同、简历、导出——把数据变成排版好的 docx。

两库的共性：纯 Python 实现（无系统依赖）、API 简单直接、是 AI 应用里文档数据预处理的常用底座——RAG 项目里「PDF 文本提取 + Word 报告生成」就是它们的典型组合。

## 2. PyPDF2 死亡史与 pypdf 诞生

理解命名混乱是 2026 年使用这两个库的第一课。历史脉络：PDF 处理库起源于 PyPDF（2010 年代），后分裂为 PyPDF2/PyPDF3/PyPDF4 多个分支；2022 年 12 月维护者把分裂的分支合并回原项目，恢复 **pypdf** 名字并继续版本号；**PyPDF2 就此成为历史**——2023 年被正式标记弃用，2024 年 6 月停止维护。

2026 年的真相：**PyPI 上的 PyPDF2 包只是 pypdf 的再导出壳**（re-export），没有任何独立修复，这意味着——它带着未修补的安全漏洞（PDF 解析的缓冲区溢出类 CVE），它在 Python 3.12+ 上文本提取静默失败（不报错、返回空/残缺文本，最难排查的故障形态）。**新代码写 PyPDF2 不是「老派」，是「错误」**。迁移一行搞定：

```python
# 旧：from PyPDF2 import PdfFileReader  （3.0 前）
# 旧：from PyPDF2 import PdfReader      （3.0 起）
from pypdf import PdfReader              # 2026 唯一正确写法
```

另外注意 API 演进的坑：3.0（2022）把 `PdfFileReader` 改名为 `PdfReader`；5.0（2024）移除全部 camelCase 旧 API（`getPage`/`numPages` 换成 `pages[i]`/`len(reader.pages)`）；7.0 计划中还有更多移除——**网上老教程的代码大概率编译不过，以 pypdf 官方文档为准**。

## 3. 2026 基线

**pypdf 最新稳定版 6.15.0（2026-08-06）**，版本节奏是快速 major 迭代（3.x→4.x→5.x→6.x），但核心 API `PdfReader`/`PdfWriter` 自 3.0 起保持稳定——major 升级多为 API 清理而非重写。使用建议：锁 `pypdf==6.15.0`，升级前看 CHANGELOG 确认移除项。

**python-docx 最新稳定版 1.2.0（2025-06-16）**，2026 年无新上游版本（仅 Linux 发行版打包更新）——这是一个维护稳定的库。1.2.0 的变化：新增注释（comments）支持、移除 Python 3.8、测试覆盖 Python 3.13。历史脉络：1.0.0（2023-10）弃 Python 2 并加超链接能力，1.1.x（2024）补 Python 3.12 兼容——库本身非常稳定，学一遍用多年。

两库的许可证：pypdf 为 BSD-3-Clause，python-docx 为 MIT——商业使用均无顾虑。生态规模：pypdf 周下载量数千万级，是 PyPI 最活跃的 PDF 库之一。

## 4. 解决什么问题

**PDF 的「数据不可得」问题**。PDF 是排版格式，不是数据格式——它的设计目标是「打印出来一样」，而不是「方便读出来」。文本在 PDF 里以「字形位置」存储，pypdf 的 `extract_text()` 把字形还原成文本流，这是 RAG 文档预处理的第一步。同时 PDF 操作（合并拆分、加密、填表单）在企业场景高频出现——合同归档、报告合并、发票处理，pypdf 全包。

**Word 的「程序化生成」问题**。手工复制粘贴生成报告是重复劳动；python-docx 把「文档」变成「对象模型」——段落、表格、样式都是可编程对象，数据一变文档即变。典型场景：数据库查询结果 → 自动生成月度报告 docx；简历模板 → 程序填充。

**两者的组合价值**：RAG 管道里「读 PDF 入知识库」+「读库生成 Word 报告」是完整闭环——数据进来（pypdf），结果出去（python-docx）。

## 5. 替代品对比

| 库 | 定位 | 对比 pypdf/python-docx |
|----|------|----------------------|
| pdfplumber | PDF 精细提取（坐标级） | 提取精度更高、表格支持好，但更慢，操作能力弱 |
| PyMuPDF | PDF 全功能（C 后端） | 快 10 倍+，渲染/搜索/编辑全，API 更复杂 |
| pdfminer.six | 提取引擎（底层） | pypdf 的提取底层之一，直接用它太底层 |
| docling | 文档→结构化（AI 版） | 版面分析/表格结构化，重量级 |
| docx2txt | docx 纯文本提取 | 极简，无生成能力 |

选择逻辑：**日常 PDF 操作与基础提取 → pypdf**；**提取质量敏感（表格、坐标）→ pdfplumber**；**高性能/渲染 → PyMuPDF**；**批量文档解析（RAG 预处理）→ unstructured**（内部封装上述库）。python-docx 在 docx 读写领域无有力替代（docx2txt 只读不写），是事实标准。

## 6. 与相邻体系的分工

**与 [[../../强烈推荐（做项目必用，简历加分）/unstructured/00-unstructured总览|unstructured]]**：unstructured 是「文档解析 ETL」——25+ 格式统一转 Elements，包含版面分析与表格结构化，是 RAG 数据预处理的事实标准；本体系是「单库精细操作」——unstructured 内部用 pypdf 类底层库，但你需要精细控制（合并拆分、填表、逐页提取）时直接用本体系。分工口诀：**批量解析用 unstructured，精细操作用 pypdf/python-docx**。

**与向量库**：提取的文本要进 [[../../向量%20&%20文档处理/chromadb/00-chromadb总览|chromadb]] 或 [[../../向量%20&%20文档处理/faiss‑cpu%20%20faiss‑gpu/00-faiss-cpu总览|faiss]] 需要先分块与向量化——08 篇讲衔接；**与 RAG 体系**：文档处理是 RAG 管道的第一公里（加载→切分→向量化→检索），见 [[../../../04-RAG检索增强生成/阶段%202：动手实现%20Basic%20RAG/00-阶段2总览|RAG 阶段 2]]。

## 7. 五条边界

**学习成本低是刻意设计**：两个库的 API 都遵循「对象 + 方法」的朴素设计——pypdf 一个 `PdfReader` 打通读取、`PdfWriter` 打通写出；python-docx 一个 `Document` 管文档全局。不像 OpenCV 或 pandas 需要先学数据模型，文档处理库的模型就是文档本身——打开即操作。这意味着两库可以「用到再查」：核心 API（reader.pages、extract_text、add_paragraph、add_table）记熟，其余按需查文档即可，不需要背全 API 表。

**不解析版面语义**——提取的是「文本流」不是「标题/正文/表格」结构；版面分析用 unstructured。

**不渲染 PDF**——pypdf 不能把 PDF 变成图片；渲染（预览、OCR 预处理）用 PyMuPDF。

**不处理扫描件**——无文本层的 PDF 提取为空；先 OCR（tesseract 等）再提取。

**不读 .doc**——python-docx 只认 .docx；老格式先转换（LibreOffice）。

**不做文档互转**——PDF↔Word 互转需要中间件，两个库都不直接支持。

## 8. 练习

1. 向同事解释为什么 2026 年不能用 PyPDF2。
2. pypdf 与 pdfplumber 的选型标准是什么？
3. python-docx 能做什么不能做什么？
4. 「PDF 是排版格式不是数据格式」怎么理解？
5. 与 unstructured 的分工口诀是什么？

> 🎯 **核心要点**：pypdf 管 PDF、python-docx 管 Word，各管一端；PyPDF2 已死（2024 停止维护、3.12+ 静默失败、未打补丁 CVE），2026 一律 pypdf；批量解析交给 unstructured，精细操作用双件套。

---

**下一模块**：[02-安装与快速开始](02-安装与快速开始.md)｜**返回总览**：[00-pypdf与python-docx总览](00-pypdf与python-docx总览.md)
