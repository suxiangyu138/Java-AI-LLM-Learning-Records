# 00 - unstructured 总览

> 强烈推荐：unstructured（文档解析 ETL 库）——做项目必用、简历加分——"任何文档 → LLM 就绪的结构化数据：PDF/Word/PPT/扫描件 25+ 格式一键分区，RAG 数据预处理的事实标准——93M+ 下载的 RAG 第一环"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
unstructured（本体系 11 篇——强烈推荐）
├── 定位层：01 unstructured 是什么（文档解析 ETL/RAG 第一环）
│          02 安装与快速开始（extras/partition 入门）
├── 核心层：03 Element 模型（20+ 元素类型/元数据溯源）
│          04 分区策略（fast/hi_res/ocr_only 选型）
│          05 文件类型与 partition 家族（25+ 格式）
│          06 清洗与后处理（clean/convert 家族）
├── 应用层：07 分块 chunking（chunk_by_title/四策略）
│          08 与 LangChain/LlamaIndex 集成（Loader/Reader）
│          09 生产实践与性能（大文件/GPU/Docker）
└── 验收层：10 生产实战与自测（RAG 文档管道 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | unstructured 是什么 | 文档解析 ETL/2026 基线/替代品 | 认知 |
| 02 | 安装与快速开始 | extras/partition 家族入门 | 会解析 |
| 03 | Element 模型 | 20+ 元素类型/metadata 溯源 | 懂数据 |
| 04 | 分区策略 | fast/hi_res/ocr_only 选型 | 会选策略 |
| 05 | 文件类型家族 | partition_pdf/docx/html/… | 会分格式 |
| 06 | 清洗与后处理 | clean/convert 家族 | 会清洗 |
| 07 | 分块 chunking | chunk_by_title/四策略 | 会分块 |
| 08 | 框架集成 | LangChain/LlamaIndex/Connectors | 会集成 |
| 09 | 生产与性能 | 大文件/GPU/Docker/API | 会部署 |
| 10 | 实战与自测 | RAG 文档管道 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 RAG 体系（`../../../04-RAG检索增强生成/阶段%201：基础概念/00-阶段总览与学习路径.md`）的分工**：RAG 链路"加载 → 切分 → 向量化 → 检索 → 生成"，那个体系讲全链路方法论，**本体系专讲"加载 + 切分"这两环**——**"RAG 的第一公里由 unstructured 承包：文档解析 + 元素分块，之后交给 milvus 检索"**。

**与 milvus‑python（`../milvus‑python/00-milvus‑python总览.md`）的分工**：milvus 是检索存储端，unstructured 是数据预处理端——**"unstructured 产出 Elements → embedding → milvus 入库检索"是 RAG 文档管道的完整两段**（10 篇实战打通）。

**与 LangChain（`../LangChain/00-LangChain总览.md`）、LlamaIndex（`../llama‑index/00-LlamaIndex总览.md`）的分工**：那两个框架各自封装了 UnstructuredLoader/UnstructuredReader——**"本体系讲裸库原理，框架篇讲集成姿势——裸库是集成层的地基"**（08 篇详讲）。

**与 pyyaml（`../pyyaml/00-pyyaml总览.md`）、loguru（`../loguru/00-loguru总览.md`）的分工**：文档管道的"配置 + 日志"两个地基件——**"unstructured 处理文档、pyyaml 管配置、loguru 管留痕"**是 RAG 数据管道的三件套（10 篇实战落地）。

**2026-08 基线**：unstructured **0.24.1**（2026-07-11 发布——0.24.0 于 2026-07-06，0.23.x 于 2026-06，发布节奏活跃）；**关键认知**：Apache-2.0、15K stars、**PyPI 累计 93M+ 下载**、25+ 文档格式、20+ 元素类型、40+ 数据连接器——**"RAG 数据预处理的事实标准"**；本地库适合开发/原型，**生产走 Docker 或托管 API**（资源隔离/GPU/弹性）；2026-01 托管 API 升级为**统一 push 式接口**（一次调用完成分区→富化→分块→向量化），MCP 服务器集成开发中。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇动手——**毕业标准：独立实现"PDF/Word/扫描件全格式 → 清洗 → 分块 → 入库 milvus"的 RAG 文档管道**。

**路线二：速成路线（1-2 天）**——01 → 02 → 04 → 07 → 10（跳过 03/05/06/08/09 精读）——适合已有 RAG 经验、只想最快把文档解析用起来的人。

**路线三：项目驱动路线**——RAG 项目遇到"PDF 解析不准/表格乱/扫描件"按需查篇——**"partition 一行上手，难点全在生产细节（策略/性能/清洗）——遇到再查"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| partition | 分区：文档 → Elements 列表（核心 API） | 02 |
| Element | 带类型与元数据的解析单元（Title/Table…） | 03 |
| metadata | 元素溯源信息（页码/坐标/语言/哈希） | 03 |
| strategy | 解析策略：fast/hi_res/ocr_only/auto | 04 |
| hi_res | 高精度策略（布局 ML 模型/表格/多列） | 04 |
| OCR | 扫描件文字识别（Tesseract/DocTR） | 04/05 |
| clean_* | 文本清洗函数家族 | 06 |
| convert_* | 输出转换（JSON/CSV/DataFrame/Markdown） | 06 |
| chunk_by_title | 按标题分块（默认分块策略） | 07 |
| max_characters | 分块最大字符数 | 07 |
| UnstructuredLoader | LangChain 集成加载器 | 08 |
| UnstructuredReader | LlamaIndex 集成读取器 | 08 |
| Connector | 数据源连接器（S3/Gmail/Jira…40+） | 08 |

## 6. 常见误区

**误区一：unstructured 是"PDF 转文本工具"**——它是**结构化解析 ETL**：输出的是带类型（标题/表格/列表）与元数据（页码/坐标）的 Elements，不是一坨文本——**"分区 ≠ 转换，元素 ≠ 字符串"**（03 篇）。

**误区二：默认策略就够**——默认 auto/fast 策略对**扫描件、多列排版、复杂表格**会解析失败；**扫描件必须 hi_res/ocr_only**（04 篇策略选型）。

**误区三：解析完直接入库**——Elements 里有页眉页脚、重复标题、乱码片段——**清洗（clean_*）+ 过滤是入库前必须的中间环**（06 篇）。

**误区四：分块随便切**——chunk_by_title 的参数（max_characters/overlap）直接决定检索质量；**表格默认被过滤掉，要显式 include_roles**（07 篇）。

**误区五：本地库跑生产**——hi_res 在 CPU 上 3-5 页/分钟、200 页 PDF 吃 4-6GB 内存、[all-docs] 依赖 2GB——**生产用 Docker/API，本地只做原型**（09 篇）。

**误区六：老教程的 import 姿势**——`from unstructured.partition.auto import partition` 在 0.10.0（2024-09）已移除；**2026 写法：`from unstructured.partition.pdf import partition_pdf`** 等格式专属分区器（02/05 篇）。

**误区七：它什么都做**——unstructured 提取**内容与结构，不做格式转换**（PDF→PDF 不是它的活）；富化/向量化在托管 API 里是增值服务，本地库只到 Elements（01 篇边界）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装库解析一份 PDF 与 Word，看 Elements 输出 |
| 2 | 03 + 04 | 对比 fast/hi_res 在扫描件上的差异 |
| 3 | 05 + 06 | 解析 HTML/表格文档，做清洗与转换 |
| 4 | 07 | chunk_by_title 分块，调 max_characters/overlap |
| 5 | 08 + 09 | 用 UnstructuredLoader 接入 LangChain，测性能 |
| 6-7 | 10 自测 + 面试 | RAG 文档管道全链路，跑 20 题 |

## 8. 快速自测 10 题

1. unstructured 与"PDF 转文本工具"的本质区别？Element 是什么？
2. 四种 partition 策略（auto/fast/hi_res/ocr_only）各自适用什么文档？
3. metadata 里有哪些溯源字段？为什么"元素坐标"有价值？
4. 表格在解析与分块两个环节分别怎么处理？
5. clean_* 家族解决什么问题？页眉页脚怎么过滤？
6. chunk_by_title 的核心参数？为什么表格默认被过滤？
7. 0.10.0 的 import 变化是什么？2026 的正确写法？
8. 为什么生产推荐 Docker/API 而不是本地库？性能数据是什么？
9. UnstructuredLoader 与 partition_pdf 的关系？框架集成层做了什么？
10. 2026 年 unstructured 的版本基线？托管 API 的新形态是什么？

## 9. 参考来源

- [unstructured GitHub 官方仓库（Release/文档）](https://github.com/Unstructured-IO/unstructured)
- [unstructured PyPI 页面（0.24.1 版本信息/安装 extras）](https://pypi.org/project/unstructured/)
- [PyPI 下载统计（93M+ 累计下载）](https://pepy.tech/projects/unstructured)
- [unstructured 包版本历史（Snyk）](https://security.snyk.io/package/pip/unstructured/versions)
- [Unstructured 官方博客：托管 API 统一接口（2026-01）](https://unstructured.io/blog/unstructured-api-prototype-without-connectors-scale-with-one-api)
- [unstructured 官方文档（分区策略/元素类型/chunking）](https://docs.unstructured.io/)

---

**下一模块**：[01-unstructured是什么.md](01-unstructured是什么.md)
