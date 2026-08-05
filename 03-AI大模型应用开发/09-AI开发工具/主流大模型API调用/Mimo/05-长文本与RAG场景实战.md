# 05 - 长文本与 RAG 场景实战

> 🎯 MiMo 的杀手级场景 — 1M 上下文让"直接整库加载"成为可能。本章覆盖四种长文本架构方案、RAG 降本策略、代码库 Agent、成本对比

---

## 目录

1. [四种长文本架构方案](#1-四种长文本架构方案)
2. [直接加载 vs RAG 分块](#2-直接加载-vs-rag-分块)
3. [长文档处理实战](#3-长文档处理实战)
4. [代码库 Agent 实战](#4-代码库-agent-实战)
5. [RAG 成本优化策略](#5-rag-成本优化策略)

---

## 1. 四种长文本架构方案

| 方案 | 原理 | 适用 | 成本 |
|------|------|------|:---:|
| **① 直接加载** | 整文塞入 1M 上下文 | 单文档 ≤50 万 token | 高（未命中） |
| **② RAG 分块** | 向量检索 TopK 片段 | 海量文档（>1M token） | 低 |
| **③ 前缀缓存** | 稳定前缀复用缓存 | 重复查询同一文档 | **极低** |
| **④ 混合** | 大文档直接加载 + 海量文档 RAG | 组合场景 | 中 |

**选择逻辑：**
```text
单文档 ≤50 万 token → 直接加载（MiMo 1M 上下文）
单文档 >1M token → RAG 分块
多文档重复查询 → 前缀缓存（命中 ¥0.025/百万）
组合 → 混合方案
```

---

## 2. 直接加载 vs RAG 分块

| 对比 | 直接加载（1M） | RAG 分块 |
|------|:---:|:---:|
| 原理 | 整文进入上下文 | 检索 TopK 片段 |
| 准确性 | **最高（全文可见）** | 依赖检索质量 |
| 实现复杂度 | 极简（无向量库） | 高（分块/嵌入/检索） |
| 单次成本 | 高（按全文计费） | 低（只算片段） |
| 重复查询成本 | **极低（缓存命中）** | 低 |
| 适用 | 单文档深度分析 | 海量文档 |

**MiMo 的优势：** 1M 上下文让"直接加载"成为可行方案 — 无需建设 RAG 基础设施，且缓存命中后重复查询极便宜。

---

## 3. 长文档处理实战

```python
# 场景：500 页合同审查
# 方案：直接加载 + 结构化输出

from openai import OpenAI
import json

client = OpenAI(api_key=os.getenv("XIAOMI_API_KEY"),
                base_url="https://api.xiaomimimo.com/v1")

with open("contract.txt", "r", encoding="utf-8") as f:
    contract = f.read()                     # 约 30 万 token

response = client.chat.completions.create(
    model="MiMo-V2.5-Pro",
    max_tokens=16384,
    response_format={"type": "json_object"},
    messages=[{
        "role": "user",
        "content": f"""审查这份合同并输出 JSON：
        {{
          "summary": "合同概要",
          "key_terms": ["关键条款列表"],
          "risks": [{{"clause": "条款位置", "risk": "风险描述", "level": "高/中/低"}}],
          "suggestions": ["修改建议"]
        }}
        
        合同内容：
        {contract}"""
    }]
)
report = json.loads(response.choices[0].message.content)
for risk in report["risks"]:
    print(f"[{risk['level']}] {risk['clause']}: {risk['risk']}")
```

---

## 4. 代码库 Agent 实战

```python
# 场景：整库代码分析（无需 RAG/分块）

# ① 读取仓库关键文件（1M 上下文可容纳中大型仓库）
repo_files = []
for root, dirs, files in os.walk("src"):
    for f in files:
        if f.endswith(".java"):
            path = os.path.join(root, f)
            repo_files.append(f"=== {path} ===\n{open(path).read()}")

repo_text = "\n".join(repo_files)          # 可到 50 万 token

# ② 整库分析
response = client.chat.completions.create(
    model="MiMo-V2.5-Pro",
    max_tokens=8192,
    messages=[{
        "role": "user",
        "content": f"""分析这个 Java 仓库（{len(repo_files)} 个文件）：
        1. 架构分层是否合理
        2. 潜在的性能问题
        3. 代码重复情况
        4. 重构建议
        
        代码内容：
        {repo_text}"""
    }]
)
print(response.choices[0].message.content)
```

---

## 5. RAG 成本优化策略

```text
MiMo + RAG 的成本优化：

① 稳定前缀缓存（省钱核心）
   知识库内容放前面 + cache 标记 → 命中 ¥0.025/百万
   同样 50 万 token 知识库前缀：
     未命中 ¥1.50 vs 命中 ¥0.0125 → 省 120 倍

② 长上下文替代 RAG（简单场景）
   单文档直接加载 → 省去向量库运维成本

③ 分级方案
   常见问题 → 缓存命中（极便宜）
   新问题 → 直接加载全文（1M 内）
   海量检索 → RAG 分块（TopK 控制成本）

④ 组合架构（生产推荐）
   知识库 → 向量检索 TopK → 拼接 Prompt → MiMo 生成
   长文档单篇 → 直接加载
   稳定前缀 → 标记缓存
```

---

> 🎯 **核心要点**：MiMo 长文本四方案 — **① 直接加载（≤50 万 token，最准最简）② RAG 分块（海量文档）③ 前缀缓存（重复查询，命中 ¥0.025）④ 混合**。生产组合：知识库 RAG + 单篇直接加载 + 稳定前缀缓存 = 准确性与成本的最优平衡。

**下一模块**：[06-MiMo在开发框架中的集成](06-MiMo在开发框架中的集成.md) / **返回总览**：[00-总览](00-Mimo-API知识体系总览.md)
