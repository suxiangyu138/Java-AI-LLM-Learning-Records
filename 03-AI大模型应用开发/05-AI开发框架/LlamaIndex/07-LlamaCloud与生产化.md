# 07 LlamaCloud 与生产化

> 托管层全景：LlamaParse/Extract/Cloud Index/Sheets 产品矩阵、内置评估器与 OTel/LlamaTrace 可观测、SOC 2 合规与积分定价——从开源框架到生产平台的完整路径。

## 📚 目录

1. [LlamaCloud 定位与更名](#1-llamacloud-定位与更名)
2. [产品矩阵](#2-产品矩阵)
3. [托管索引与增量同步](#3-托管索引与增量同步)
4. [内置评估器](#4-内置评估器)
5. [可观测性](#5-可观测性)
6. [合规与定价](#6-合规与定价)
7. [面试高频问法](#7-面试高频问法)
8. [生产化落地清单](#8-生产化落地清单)
9. [常见误区](#9-常见误区)

## 1. LlamaCloud 定位与更名

### 是什么

```
LlamaCloud = LlamaIndex 的商业托管平台
2026-02 公告过渡更名：→ LlamaParse（以解析为核心重塑）
```

### 更名的含义

```
从"企业 RAG 平台"转向"智能体文档处理平台"：
核心资产 = LlamaParse（解析）+ 周边数据产品
反映行业判断：数据层（解析/检索）是竞争主战场
```

### 规模背书

```
累计处理 10 亿份文档
30 万 LlamaParse 用户
公开客户：Rakuten/Carlyle/Salesforce/KPMG
```

## 2. 产品矩阵

| 产品 | 能力 | 定位 |
|---|---|---|
| LlamaParse | 智能文档解析（v2 四级） | 核心资产 |
| LlamaExtract | schema 结构化提取（免训练） | 数据→结构化 |
| LlamaCloud Index | 托管索引与检索 | 生产检索 |
| LlamaSheets | 电子表格→AI 数据 | 表格场景 |
| LlamaSplit | 自动文档分离 | 预处理 |

### LlamaExtract（重点）

```
基于 schema 的结构化数据提取：
定义目标 schema → 自动提取（无需训练模型）
页面级引用：数据到具体页面及边界框映射（可审计）
适用：发票/合同/表单类结构化抽取
```

### 产品组合用例

```
发票处理全链路：
LlamaSplit（分离）→ LlamaParse（解析）
→ LlamaExtract（提取字段+页面引用）
→ LlamaCloud Index（入库检索）
```

## 3. 托管索引与增量同步

### 托管索引的价值

```
自动配置生产级索引流水线（免自建）
增量同步：只处理变更文件（增量更新托管化）
数据可移植：自带向量库/嵌入模型（不锁定）
可观测：逐阶段状态与文件计数
```

### 与自建对比

| 维度 | 自建（开源） | LlamaCloud Index |
|---|---|---|
| 部署 | 自己管 | 托管 |
| 增量 | 自己写（阶段 3 的 06 篇） | 内置 |
| 可移植 | — | 自带存储/模型 |
| 运维 | 全自担 | 平台处理 |
| 成本 | 基建成本 | 订阅 |

> 增量同步托管化 = 阶段 3 增量更新的"平台版"——不用自己写指纹/状态管理。

## 4. 内置评估器

### 评估能力

```
内置评估器（免独立可观测平台）：
忠实度（Faithfulness）
相关性（Relevancy）
检索质量（MRR/NDCG 等）
```

### 使用

```python
from llama_index.core.evaluation import (
    FaithfulnessEvaluator, RelevancyEvaluator,
)

faithfulness = FaithfulnessEvaluator()
result = faithfulness.evaluate_response(response=response)
print(f"忠实度通过：{result.passing}")

# 批量评估（测试集）
from llama_index.core.evaluation import BatchEvalRunner
runner = BatchEvalRunner({
    "faithfulness": faithfulness,
    "relevancy": RelevancyEvaluator(),
}, workers=8)
results = await runner.aevaluate_queries(
    query_engine, queries=test_queries,
)
```

### 与 RAG 阶段 3 的关系

```
内置评估器 = RAGAS 思想的框架内实现
忠实度/相关性 = RAGAS 四指标的核心两个
生产建议：内置评估进 CI + Ragas 定期深评
```

## 5. 可观测性

### OTel + LlamaTrace

```
OTel：Workflows/查询引擎原生追踪
LlamaTrace：LlamaIndex 专用可观测仪表板
```

### 可观测内容

| 层 | 追踪 |
|---|---|
| 查询 | 全链路耗时 |
| 检索 | 检索耗时/节点数 |
| 步骤 | Workflows 每步事件 |
| LLM | 调用/token/成本 |

### 生产价值

```
"数据管道可观测"：
解析阶段耗时/失败率
索引同步进度
检索质量指标趋势
（对应 RAG 阶段 5 的 06 篇监控思想）
```

## 6. 合规与定价

### 合规

| 认证 | 说明 |
|---|---|
| SOC 2 Type 2 | 安全审计认证 |
| GDPR | 欧洲数据合规 |
| HIPAA | 医疗数据合规 |

```
部署形态：SaaS 或本地 VPC（数据主权）
```

### 定价

| 层级 | 价格 | 额度 |
|---|---|---|
| 免费 | $0 | 10K 积分/月 |
| Starter | $50/月 | 40K 积分 |
| Pro | $500/月 | 400K 积分 |
| 企业 | 定制 | 定制 |

```
计费：积分制（1000 积分 ≈ $1.25）
解析成本：约 1 积分/页起（Fast 级）
```

### 成本意识（RAG 阶段 3 纪律）

```
解析成本分层：按文档复杂度路由级别（06 篇）
积分监控：解析/提取用量纳入成本看板
开源替代：简单文档本地解析，复杂文档才上 LlamaParse
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| LlamaCloud 是什么？ | 托管平台（过渡更名中，解析为核心） |
| 产品矩阵？ | Parse/Extract/Index/Sheets/Split |
| LlamaExtract 特点？ | schema 提取免训练 + 页面级引用 |
| 增量同步？ | 托管内置（只处理变更文件） |
| 内置评估器？ | 忠实度/相关性（RAGAS 思想内建） |
| 合规与定价？ | SOC 2/GDPR/HIPAA；免费 10K 积分起 |

### 面试加分表达

> "LlamaCloud 的产品矩阵覆盖数据全链路：LlamaParse 解析、LlamaExtract schema 提取（带页面级引用可审计）、Cloud Index 托管检索（增量同步内置）。内置评估器把忠实度/相关性检查做到框架里，OTel/LlamaTrace 让数据管道可观测——从开源到生产的路径是完整的。"

## 8. 生产化落地清单

| 类别 | 检查项 | ✅ |
|---|---|---|
| 解析 | 文档类型 → 级别路由（成本分层） | ☐ |
| 版本 | Parse API version 固定 | ☐ |
| 索引 | 托管索引（增量同步）或自建存储 | ☐ |
| 评估 | 内置评估器进 CI（忠实度阈值） | ☐ |
| 观测 | OTel/LlamaTrace 接入 | ☐ |
| 合规 | 数据主权（SaaS vs VPC）确认 | ☐ |
| 成本 | 积分用量看板 + 预算告警 | ☐ |
| 可移植 | 存储/嵌入模型可自带（防锁定） | ☐ |

### 开源 vs 托管的路径

```
开源路径：LlamaIndex + 自建向量库 + 本地解析（成本低、自维护）
托管路径：LlamaCloud（LlamaParse/Index）+ 平台运维（省心）
混合路径：开源编排 + LlamaParse 按需（常见折中）
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "LlamaCloud 必须用" | 开源全免费；托管是可选项 |
| "托管 = 锁定" | 数据可移植（自带存储/嵌入） |
| "评估要另买平台" | 内置评估器够用（深度评估用 Ragas） |
| "更名说明业务萎缩" | 是聚焦（解析为核心的重塑） |
| "积分制很贵" | 免费 10K/月 + 分层路由可控制 |

> 🎯 核心要点：LlamaCloud 正在以 LlamaParse 为核心重塑（2026-02 更名）；产品矩阵（Parse/Extract/Index/Sheets/Split）覆盖数据全链路；托管索引内置增量同步（阶段 3 的平台版）；内置评估器 = RAGAS 思想内建；SOC 2/GDPR/HIPAA + 积分制定价；落地走八项清单；开源/托管/混合三路径按团队选。

---

**下一模块**：[08-对比选型与面试](08-对比选型与面试.md) / **返回总览**：[00-LlamaIndex知识体系总览](00-LlamaIndex知识体系总览.md)
