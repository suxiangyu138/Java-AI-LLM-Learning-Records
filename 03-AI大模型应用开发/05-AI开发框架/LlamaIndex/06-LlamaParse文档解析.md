# 06 LlamaParse：文档解析

> LlamaIndex 的竞争资产：LlamaParse v2 四级解析（Fast 1 积分/页 → Agentic Plus 45 积分/页）、Parse API v2 结构化配置、Retrieval Harness 文件系统原语——"解析质量决定 RAG 上限"的实践答案。

## 📚 目录

1. [为什么解析是竞争资产](#1-为什么解析是竞争资产)
2. [LlamaParse v2 四级服务](#2-llamaparse-v2-四级服务)
3. [Parse API v2](#3-parse-api-v2)
4. [Retrieval Harness](#4-retrieval-harness)
5. [视觉布局保留](#5-视觉布局保留)
6. [能力与格式支持](#6-能力与格式支持)
7. [面试高频问法](#7-面试高频问法)
8. [解析选型实战](#8-解析选型实战)
9. [常见误区](#9-常见误区)

## 1. 为什么解析是竞争资产

### 2026 行业共识

```
RAG 的瓶颈不在模型，在数据管道：
解析质量（复杂文档 → 文本的保真度）
是区分系统好坏的第一道分水岭

智能体时代的放大效应：
解析错误 → 检索错误 → Agent 错误行动
```

### 传统解析的痛点（回顾阶段 4）

```
OCR 误差（85-90% 准确率）
表格结构丢失（转文本后行列关系毁掉）
扫描件/手写/多栏排版处理差
→ LlamaParse 用 VLM 智能体路由解决这些
```

## 2. LlamaParse v2 四级服务

### 四级定价（2025-12-18 发布）

| 级别 | 积分/页 | 适用 |
|---|---|---|
| Fast | 1 | 纯文本文档（便宜快速） |
| Cost-effective | 3 | 通用目的（默认） |
| Agentic | 10 | 复杂布局混合内容 |
| Agentic Plus | 45 | 最高精度（成本降 50%） |

### 分级选择

```
纯文本 PDF → Fast（1 积分/页）
常规文档 → Cost-effective（3 积分）
表格/图表/多栏 → Agentic（10 积分）
扫描件/复杂排版 → Agentic Plus（45 积分）
```

### 定价的工程含义

```
解析成本可控：按文档复杂度分级
积分制：1000 积分 ≈ $1.25；免费层 10K 积分/月
生产管线：按文档类型路由到对应级别（成本优化）
```

## 3. Parse API v2

### 结构化配置

```python
# 新 SDK：pip install llama-cloud
from llama_cloud import ParseApi, InputOptions, OutputOptions

parse_api = ParseApi()
result = parse_api.parse(
    document_path="复杂合同.pdf",
    input_options=InputOptions(
        processing_options={"mode": "agentic"},   # 解析级别
    ),
    output_options=OutputOptions(
        output_type="markdown",                    # 输出格式
    ),
    version="2026-04-09",                         # 版本固定
)
```

### API v2 的关键改进

| 改进 | 说明 |
|---|---|
| 结构化配置 | input_options/output_options/processing_options 分离 |
| expand 参数 | 精确控制返回（文本/Markdown/JSON/元数据） |
| 版本固定 | 生产管线锁定解析行为（如 2025-12-11/2026-04-09） |
| 类型安全 | 输出类型一致（完整 SDK 支持） |
| 双语言 | Python（llama-cloud）+ TypeScript（@llamaindex/llama-cloud） |

### 版本固定的价值（生产关键）

```
解析行为会随服务演进变化：
锁定版本 → 生产管线行为稳定（不随上游漂移）
升级 = 显式换版本 + 回归测试
（对应 RAG 阶段 5 的"锁定版本防漂移"纪律）
```

## 4. Retrieval Harness

### 是什么

```
2026 新增：为 Agent 提供的文件系统检索原语
确定性文档遍历工具（不是向量检索，是"文件级"操作）
```

### 四个原语

| 原语 | 能力 | 解决 |
|---|---|---|
| Hybrid Retrieve | 混合检索 + 重排序 | 语义+关键词 |
| List Files | 文件发现 | Agent 知道"有什么" |
| File Grep | 服务端正则扫描 | 精确模式匹配 |
| File Read | 直接读取上下文 | **克服分块截断**（读原文） |

### File Read 的意义

```
传统 RAG 的痛点：检索到的是"分块"，不是"原文"
File Read：Agent 直接读完整文件 → 上下文不断裂
（对应"先宽检索、再慷慨纳入"的 2026 融合思想）
```

## 5. 视觉布局保留

### 是什么

```
解析时捕获页面截图，链接到源分块
Agent 在文本不足时调取渲染后的原始页面
```

### 解决的痛点

```
密集表格/多栏文档：
文本提取丢失布局 → 视觉保留兜底
Agent 看图 → 防幻觉（文本不足时不瞎猜）
```

### 价值

```
① 可审计：答案可回溯到原始页面
② 防幻觉：视觉确认代替猜测
③ 复杂文档的"第二通道"
```

## 6. 能力与格式支持

### 支持范围

```
文件类型：50+ 种（部分资料 90+）
PDF/图片/表格/图表/手写笔记/扫描件
VLM 智能体路由：内容自动分派到专门解析模型
```

### 能力清单

| 能力 | 说明 |
|---|---|
| 表格识别 | 嵌套/合并单元格还原 |
| 扫描件 OCR | 无文本层文档 |
| 手写识别 | 手写笔记 |
| 多栏排版 | 阅读顺序正确 |
| 图表理解 | 图表转结构化 |
| 指令跟随 | 用户指令定制解析 |

### 与阶段 4 多模态的衔接

```
LlamaParse = 阶段 4"Agentic 解析"的托管实现：
Docling/LlamaParse 同类（VLM 看页面重绘结构）
LlamaParse 的优势：商业级 + 四级 + 视觉保留
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 为什么解析重要？ | 解析质量决定 RAG 上限（2026 共识） |
| v2 四级？ | Fast 1/Cost-effective 3/Agentic 10/Agentic Plus 45 积分 |
| API v2 改进？ | 结构化配置/版本固定/类型安全/双语言 SDK |
| Retrieval Harness？ | 文件系统原语（List/Grep/Read 等，File Read 克服分块截断） |
| 视觉布局保留？ | 页面截图链接分块，Agent 看图防幻觉 |
| 与 OCR 区别？ | VLM 智能体路由 + 表格还原 + 视觉保留 |

### 面试加分表达

> "LlamaParse 是 LlamaIndex 在'数据管道是瓶颈'共识下的竞争资产：四级服务按文档复杂度定价（纯文本 1 积分、复杂扫描件 45 积分），API v2 支持版本固定——生产管线锁定解析行为不漂移。Retrieval Harness 的 File Read 让 Agent 直接读原文，克服分块截断，这是传统 RAG 的结构性痛点。"

## 8. 解析选型实战

### 文档类型 → 解析级别

| 文档类型 | 推荐级别 | 理由 |
|---|---|---|
| 纯文本 PDF/Word | Fast（1 积分） | 无复杂结构 |
| 常规文档（规整排版） | Cost-effective（3） | 默认平衡 |
| 表格/图表密集 | Agentic（10） | VLM 还原结构 |
| 扫描件/手写/多栏 | Agentic Plus（45） | 最高保真 |

### 成本优化策略

```
① 按类型路由：解析前先分类 → 对应级别（成本分层）
② 本地先行：简单文档本地解析（pypdf），复杂才上 LlamaParse
③ 积分监控：用量看板（免费 10K/月 用完才付费）
④ 版本固定：锁定解析行为防漂移（生产纪律）
```

### 与本地解析的对比决策

```
本地（pypdf/pdfplumber）→ 免费但复杂文档丢信息
LlamaParse → 付费但 VLM 级保真
决策：文档复杂度 × 信息价值 > 解析成本？
（金融研报/合同/法规 → 值；日常 PDF → 不值）
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "LlamaParse 只是 OCR" | VLM 智能体路由 + 表格还原 + 指令跟随 |
| "越贵级别越好" | 按文档类型匹配（纯文本 Fast 就够） |
| "解析一次搞定" | 版本固定 + 定期回归（解析行为会演进） |
| "离线也免费" | 是托管服务（API 调用）；开源解析替代有限 |
| "解析不重要" | 2026 共识：解析质量决定 RAG 上限 |

> 🎯 核心要点：LlamaParse = 解析层竞争资产（v2 四级：Fast 1 → Agentic Plus 45 积分）；API v2 的结构化配置 + 版本固定让生产管线可锁定；Retrieval Harness 四原语（File Read 克服分块截断）；视觉布局保留防复杂文档幻觉；选型按"文档复杂度 × 信息价值 > 解析成本"决策；对应阶段 4 的 Agentic 解析思想（商业级实现）。

---

**下一模块**：[07-LlamaCloud与生产化](07-LlamaCloud与生产化.md) / **返回总览**：[00-LlamaIndex知识体系总览](00-LlamaIndex知识体系总览.md)
