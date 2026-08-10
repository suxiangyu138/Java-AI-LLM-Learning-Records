# 03 - 文档加载：LlamaHub 与 LlamaParse

> 本体系第三课：数据从哪来——本地文件用 SimpleDirectoryReader，复杂文档用 LlamaParse，其他来源用 LlamaHub——"加载器选对了，RAG 就成功了一半"

---

## 📚 目录

1. [一句话定位](#1-一句话定位)
2. [SimpleDirectoryReader：本地文件](#2-simpledirectoryreader本地文件)
3. [LlamaHub：300+ 现成连接器](#3-llamahub300-现成连接器)
4. [LlamaParse：复杂文档解析](#4-llamaparse复杂文档解析)
5. [LlamaCloud：托管平台](#5-llamacloud托管平台)
6. [加载器选型](#6-加载器选型)
7. [加载实战要点](#7-加载实战要点)
8. [练习 5 题](#8-练习-5-题)
9. [本节验收](#9-本节验收)

---

## 1. 一句话定位

**加载器把"任何来源的数据"变成 `List[Document]`——LlamaHub 给现成轮子，LlamaParse 解决"机器读不懂的文档"**：

```text
加载三路
├── SimpleDirectoryReader：本地文件夹（txt/md/pdf/docx——零配置起步）
├── LlamaHub Readers：300+ 集成（数据库/网页/云盘/API——pip install 即用）
└── LlamaParse：云解析（PDF 表格/扫描件/手写/多模态——复杂版式唯一解）
    ——"加载的本质：来源 → Document——Loader 是第一个标准件"
```

**定位心智**：**"别自己写解析——社区 300+ 连接器就是别人踩过的坑"**——"**先查 LlamaHub 有没有现成的（https://llamahub.ai）→ 没有再考虑 LlamaParse/自写——'自己写解析 = 重复造轮子（编码/版式/认证全是坑）'"**。

## 2. SimpleDirectoryReader：本地文件

**零依赖起步——指定文件夹一把梭**：

```python
from llama_index.core import SimpleDirectoryReader

# 读整个目录（txt/md/pdf/docx/csv 自动按扩展名分发解析）
docs = SimpleDirectoryReader("./data").load_data()

# 进阶：指定文件 + 自定义文件名元数据（溯源靠它）
docs = SimpleDirectoryReader(
    input_files=["./data/report.pdf"],
    filename_as_metadata=True,   # file_name 进 metadata（05 篇过滤/溯源）
    recursive=True,              # 递归子目录
).load_data()
```

**要点**：① PDF 解析依赖 `pypdf`——`pip install "llama-index-readers-file"` 补齐全家桶；② 简单 PDF 够用，复杂版式（表格/双栏/扫描）必须上 LlamaParse——**"SimpleDirectoryReader 是起点不是终点"**；③ 大目录分批 load（每批几百个文件），避免内存峰值；④ `filename_as_metadata=True` 是溯源标配——"**回答带引用（05 篇）的第一步就是这里存下 file_name**"。

## 3. LlamaHub：300+ 现成连接器

**按需安装——集成包与核心解耦（0.10+ 多包架构）**：

```bash
pip install llama-index-readers-database   # SQL 数据库
pip install llama-index-readers-notion     # Notion
pip install llama-index-readers-confluence # Confluence
pip install llama-index-readers-web        # 网页
```

```python
from llama_index.readers.database import DatabaseReader

reader = DatabaseReader(scheme="mysql", host="localhost", db="wiki")
docs = reader.load_data(query="SELECT title, body FROM articles LIMIT 100")
```

**要点**：① 覆盖数据库/云盘/文档站/代码仓库/音视频等 150+ 数据源；② 读库类连接器把"查询结果"变成 Document——**"加载器不关心数据在哪，只关心输出是不是 Document"**；③ 每个集成包有独立版本与维护状态——**"生产选型看维护活跃度，冷门连接器先小范围验证"**；④ 官方 hub 地址 `llamahub.ai`——**"找连接器先搜 hub 再搜代码——'hub 没有的再自写'"**。

## 4. LlamaParse：复杂文档解析

**GenAI 解析服务——把"人看的文档"变成"RAG 用的 Markdown"**：

```python
# 2026 新包：llama-cloud（llama-parse 旧包已弃用，维护至 2026-05-01）
pip install llama-cloud

from llama_cloud.parse import LlamaParse

parser = LlamaParse(
    result_type="markdown",
    parsing_instruction="保留表格结构，公式用 LaTeX",  # 自定义指令
    tier="agentic",   # 解析档位：fast / cost_effective / agentic / agentic_plus
)
docs = parser.load_data("./report.pdf")
```

**四档选型**：

| 档位 | 特点 | 适用 | 成本量级 |
|------|------|------|---------|
| fast | 规则式，无 AI | 简单标准版式 | 约 $1/千页 |
| cost_effective | 平衡 | 常规文档性价比 | 约 $3/千页 |
| agentic | 全 AI 解析 | 复杂表格/双栏 | 约 $15/千页 |
| agentic_plus | 最高精度 | 极难版式/密集图表 | 约 $12/千页 |

**要点**：① 支持 90+ 文件类型（PDF/PPTX/DOCX/XLSX/HTML）；② 输出带页级引用与置信度——**"引用溯源（05 篇）直接可用"**；③ 免费档约 1000 页/月（1 万 credits），超出按 credits 计费（1K credits ≈ $1.25）；④ 解析失败也扣配额——**"生产要重试 + 监控失败率"**；⑤ 生产可 pin 版本（如 2026-07-24）保证输出稳定。

**三种输出格式**：`result_type` 支持 markdown/text/json——**markdown** 保结构（默认，RAG 首选）；**text** 纯文本（省 token）；**json** 结构化抽取（发票/合同字段提取——配合 `parsing_instruction` 指定要抽的字段）——"**按下游用途选格式：喂检索用 markdown，喂表单用 json**"。

## 5. LlamaCloud：托管平台

**全托管 RAG/Agent 平台——索引、检索、评估、Agent 一条龙**：

| 套餐 | 月费 | 内容 | 适合 |
|------|------|------|------|
| Free | $0 | 1 万 credits/月 | 学习原型 |
| Starter | $50 | 50 万 credits + 5 数据源 | 小团队 |
| Pro | $500 | 500 万 credits + 25 数据源 | 正式产品 |
| Enterprise | 定制 | 不限用户/VPC 部署 | 大企业 |

**要点**：开源核心 MIT 免费，云服务按用量付费——**"数据敏感/合规严选本地自建（核心 + LlamaParse API），图省事选 LlamaCloud"**。

## 6. 加载器选型

```text
选型决策
├── 本地文件、常规版式 → SimpleDirectoryReader（免费零配置）
├── 数据库/网页/云盘等 → LlamaHub 对应 reader（按需安装）
├── 复杂 PDF/扫描/表格 → LlamaParse（付费但省人力）
├── 图片/音视频 → 多模态 reader 或 LlamaParse 多模态档
└── 找不到现成的 → 自写 reader 继承 BaseReader 返回 List[Document]
```

**选型心智**：**"先免费后付费——SimpleDirectoryReader 跑通原型，再评估 LlamaParse 的收益（表格解析质量直接决定检索质量）"**——"**判断标准：文档版式复杂度 + 解析错误成本——'解析错了，后面全错'"**；**混用也常见**——"简单文档本地解析 + 复杂文档（合同/财报）走 LlamaParse——按文档类型分流，成本与质量兼顾（pipeline 里两种加载器并存）"。

**数据合规提醒**：文档包含敏感数据时——**"LlamaParse/LlamaCloud 是云服务，数据出境要评估；合规要求严的企业用本地解析（pypdf 等）+ 开源 embedding，或私有化部署 LlamaParse（Enterprise 支持 VPC）"**——"**先问'数据能不能出域'，再选解析方案（与 02-后端 数据安全体系同思路）"**；爬虫抓取的网页转知识库——**"来源合法性先过一遍（robots/版权），解析只是技术层（12-Python爬虫 体系的合规四原则适用）"**。

## 7. 加载实战要点

**① 大批量分批 + 失败重试**——加载 1 万+ 文件分批跑（每批 500），单文件失败记日志跳过，别让一个坏文件炸掉全流程：

```python
docs = []
for batch in chunks(file_list, 500):
    try:
        docs += SimpleDirectoryReader(input_files=batch).load_data()
    except Exception as e:
        print(f"batch failed: {e}")   # 记日志，断点续跑
```

**② 编码与乱码**——Windows 老文档常是 GBK——**"text 里出现 � 先查源文件编码，在加载前转 UTF-8"**（中文文档高频坑——爬虫/办公文件尤其常见，解法与 12-Python爬虫 的乱码四步法一致）。

**③ 自定义 Reader**——没有现成连接器时继承 `BaseReader`，返回 `List[Document]` 即可被全链路消费——**"Reader 协议 = load_data() → List[Document]——会这一个接口就能接入任意数据源"**（异步实现 `aload_data()`——FastAPI 服务里同步 load_data 会阻塞事件循环）。

**LlamaParse vs 本地解析**：pypdf/PyMuPDF 等免费库对简单 PDF 够用，但表格结构、双栏排版、扫描件（OCR）、手写体是规则解析的硬伤——**"决策公式：解析错误成本 × 文档比例 > LlamaParse 费用 → 上云；否则本地"**——"**表格解析错了，检索和回答一起错——复杂版式别省这笔钱**"。

## 8. 练习 5 题

1. 三种加载方式？（本地/LlamaHub/LlamaParse）
2. SimpleDirectoryReader 何时够用？（常规版式本地文件）
3. LlamaParse 四档？（fast/cost_effective/agentic/agentic_plus）
4. 2026 年 LlamaParse 用什么包？（llama-cloud——llama-parse 已弃用）
5. 解析失败会怎样？（扣配额——生产要重试监控）

## 9. 本节验收

**验收动作**：① 用 SimpleDirectoryReader 加载 10 个本地文件并打印 Document 数量与元数据；② 查 LlamaHub 确认自己数据源有没有现成 reader；③ 理解 LlamaParse 四档定位——**"加载器 = RAG 的第一道质量关卡"**。

> 🎯 **核心要点**：**加载三路（本地/LlamaHub/LlamaParse）**；**LlamaParse 四档按版式复杂度选**；**2026 新包 llama-cloud（llama-parse 弃用）**；**解析失败扣配额要重试**——"别自己写解析——300+ 现成轮子 + 云解析兜底"。

---

**上一模块**：[02-核心抽象：Document与Settings.md](./02-核心抽象：Document与Settings.md) / **下一模块**：[04-切分与索引构建.md](./04-切分与索引构建.md)
