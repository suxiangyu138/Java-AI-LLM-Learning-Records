# 17 Agentic RAG 与自我反思

> 检索从"固定流水线"变成"Agent 的工具"——自主决策、多跳推理、自我反思，让 RAG 从"查一次"进化为"查明白"，代价是要防住死循环

---

## 📚 目录

1. [从流水线到 Agent：范式的转变](#1-从流水线到-agent范式的转变)
2. [检索即工具：Agent 的决策空间](#2-检索即工具agent-的决策空间)
3. [任务分解与多跳检索](#3-任务分解与多跳检索)
4. [自我反思：CRAG / Self-RAG](#4-自我反思crag--self-rag)
5. [死循环风险与防护](#5-死循环风险与防护)
6. [生产级 Agentic RAG 架构](#6-生产级-agentic-rag-架构)

---

## 1. 从流水线到 Agent：范式的转变

**传统 RAG vs Agentic RAG 的本质差异——"是否强制检索"**：

```text
传统 RAG（流水线）：
  问题 → 检索（强制）→ 拼上下文 → 生成
  → 简单问题也检索（浪费）、检索结果差也硬用（污染）

Agentic RAG（工具化）：
  问题 → Agent 决策：
    - 需要知识吗？→ 检索（工具调用）
    - 需要实时数据吗？→ API/搜索
    - 需要算数吗？→ 计算器
    - 不需要外部知识 → 直接回答（省一次检索）
  → 检索变成"可选工具"，由 Agent 自主决定何时调用、调用几次
```

**为什么需要 Agentic RAG**（传统 RAG 的三个失效场景）：

| 场景 | 传统 RAG | Agentic RAG |
|------|---------|-------------|
| "先查负责人再查审批额度" | 一次检索拿不到链条 | **多跳**：查 A → 用 A 的结果查 B |
| 问题不需要知识库 | 强制检索白花钱 | 判断后直接回答 |
| 检索结果不满意 | 硬着头皮生成 | **反思后重查** |

> 🎯 **核心要点**：Agentic RAG = "**检索从'必经之路'变成'可用工具'**"——Agent 决定**要不要查、查什么、查几次、不满意怎么办**。这是 2026 年"Naive RAG 死亡"讨论后的主要进化方向之一。

---

## 2. 检索即工具：Agent 的决策空间

**Agentic RAG 的 Agent 循环**（ReAct 模式）：

```text
循环：观察（Observe）→ 思考（Think）→ 行动（Act）→ 观察...
  ① 问题输入
  ② Agent 思考：需要检索吗？需要哪个工具？
  ③ 行动：调用工具（search_kb / search_web / calculator / sql）
  ④ 观察工具结果 → 决定：够了？继续查？直接回答？
  ⑤ 输出最终答案
```

```python
# 工具定义（LangChain/LlamaIndex 风格伪代码）
tools = [
    Tool(name="search_kb", func=vector_search,
         description="搜索企业内部知识库（文档/制度/FAQ）"),
    Tool(name="search_web", func=web_search,
         description="搜索互联网最新信息"),
    Tool(name="query_sql", func=sql_query,
         description="查询结构化业务数据（订单/库存）"),
    Tool(name="calculate", func=calculator, description="数学计算"),
]

agent = create_react_agent(llm, tools)
answer = agent.run("统计一下上季度各区域订单量并总结趋势")
# 执行链：query_sql（拿数据）→ calculate（汇总）→ 直接回答（无需检索知识库）
```

**工具设计的三个要点**：

| 要点 | 说明 |
|------|------|
| description 必须写清"什么时候用" | Agent 靠描述选工具（描述差 = 选错工具） |
| 工具返回结构化 | 便于 Agent 解析（JSON > 长文本） |
| 工具数量克制 | 3-5 个最佳（太多增加选错概率） |

> 🎯 **核心要点**：检索成为工具的收益 = "**按需调用（省钱）+ 组合使用（多跳/跨源）+ 判断退出（不硬查）**"——工具的描述质量直接决定 Agent 的选型正确率。

---

## 3. 任务分解与多跳检索

**多跳检索（Multi-hop）**：链条式查询——第一跳的结果是第二跳的输入：

```text
问题："报销政策里，部门负责人的审批额度上限是多少？"
  跳 1：查"报销政策"→ 找到"审批额度与角色对应表"
  跳 2：从表中找到"部门负责人"→ 额度上限
  → 单次向量检索做不到（问题里没有"部门负责人"这个词出现在同一块）
  → Agent 拆解为子任务逐个检索
```

**任务分解（Decomposition）的两种模式**：

| 模式 | 做法 | 适用 |
|------|------|------|
| **Plan-then-Execute** | 先规划子任务清单，再依次执行 | 问题结构清晰（"先查A再查B"） |
| 动态分解（ReAct 式） | 边执行边决定下一步 | 未知结构（探索式） |

```python
# Plan-then-Execute 伪代码
plan = llm(f"把问题拆成有序的子任务（每步可调用一个工具）：{question}")
for step in plan:
    result = agent.execute(step)     # 每步可能检索/查询
    context.append(result)
answer = llm(f"基于以下分步结果回答问题：{context}")
```

**多跳的成功关键**：**中间结果的结构化保留**——每跳的"实体/结论"要能被下一跳引用（图谱在这类问题上有天然优势，见 16 模块）。

> 🎯 **核心要点**：多跳 = "**把复合问题拆成链条，每跳喂给下一跳**"——这是 Agentic RAG 相对单次检索的核心能力。面试答"多跳检索怎么实现"用"任务分解 + 结果链接"框架。

---

## 4. 自我反思：CRAG / Self-RAG

**自我反思 = Agent 对检索结果的"质检循环"**——2025-2026 的两大代表：

### CRAG（Corrective RAG，2024）

```text
核心：检索后先"评估"再决定"纠错"
  ① 评估器判断检索结果质量（相关/不相关/部分相关）
  ② 相关 → 继续生成
  ③ 不相关 → 触发纠错动作：
     - 改写查询重查（query rewriting）
     - 换检索源（向量 → 网络搜索）
  ④ 部分相关 → 过滤掉不相关块再生成
```

### Self-RAG（2024）

```text
核心：模型自己决定"要不要检索"+"检索结果是否被使用"
  ① 按需检索：模型先判断问题是否需要外部知识（不需要 → 直接答）
  ② 反思 token：生成时输出反思标记
     - [Retrieve] 需要检索 / [No Retrieval] 不需要
     - [Relevant] 检索结果相关 / [Irrelevant] 不相关
  ③ 基于反思标记调节生成（不相关的引用会被抑制）
```

```python
# 生产简化版：评估-重查循环（最常用的落地方案）
def agentic_search(question: str, max_retries=2):
    docs = retrieve(question)
    quality = judge(question, docs)          # ① 评估器（LLM-as-judge）
    for _ in range(max_retries):
        if quality == "relevant":
            return docs                       # ② 合格直接用
        question = rewrite_query(question, docs)   # ③ 不满意：改写重查
        docs = retrieve(question)
        quality = judge(question, docs)
    return docs                               # ④ 超限兜底（防死循环）
```

> 🎯 **核心要点**：自我反思 = "**评估 → 纠错 → 重试（有上限）**"三件套——CRAG 反思"检索结果好不好"，Self-RAG 反思"要不要检索"。生产落地常用简化版（judge + rewrite + max_retries）。

---

## 5. 死循环风险与防护

**Agentic RAG 的头号生产事故：无限循环烧 Token**：

```text
症状：Agent 陷入"改写查询 → 检索 → 不满意 → 再改写"的循环
根因：评估器永远不满意 + 没有退出机制
后果：单次请求消耗大量 token（成本黑洞）、延迟暴涨、用户体验崩

2026 工程共识：必须设置硬限制！
```

**四道防线**（缺一不可）：

| 防线 | 实现 | 说明 |
|------|------|------|
| **max_iterations 硬限制** | `max_iterations=3`（检索循环上限） | 超限直接兜底（用已有结果生成或告知失败） |
| 循环检测 | 记录已试过的查询，重复则停止 | 防"同一查询反复重试" |
| 评估器保守化 | judge 阈值放宽 | 避免"永远不满意" |
| 全局超时 | 请求级 timeout（如 30s） | 最后防线（含 LLM 调用总时长） |

```python
# 生产级防护示例
def safe_agentic_search(question, max_iterations=3):
    seen_queries = set()
    docs = []
    for i in range(max_iterations):
        if question in seen_queries:      # 循环检测
            break
        seen_queries.add(question)
        docs = retrieve(question)
        if judge(question, docs) == "relevant":
            break                         # 满意即退出
        question = rewrite_query(question, docs)   # 不满意：改写（次数受限）
    return docs                           # 兜底返回（可能不完美但不会死循环）
```

> 🎯 **核心要点**：Agentic RAG 的生产铁律 = "**自主性必须配上限**"——max_iterations + 循环检测 + 超时三件套，否则"聪明"变成"烧钱"。（成本账见 21 模块。）

---

## 6. 生产级 Agentic RAG 架构

**2026 生产级参考架构**（模块化组合）：

```text
入口（问题）
  → ① 意图识别（Router）：需要检索吗？哪种检索？
      ├── 不需要知识 → 直接回答（省钱）
      ├── 局部事实 → 向量 RAG（快）
      ├── 全局/关系 → GraphRAG（见 16）
      └── 多跳/复杂 → Agent 循环（本模块）
  → ② Agent 循环（max_iterations=3 + 循环检测 + 超时）
      └── 工具集：search_kb / search_web / query_sql / calculate
  → ③ 反思质检（judge + rewrite 重试，次数受限）
  → ④ 上下文组装 + 重排压缩（见 15）
  → ⑤ 生成 + 引用溯源
  → ⑥ 评估埋点（每步耗时/token/检索次数 → 监控）
```

**生产注意清单**：

| 注意 | 说明 |
|------|------|
| 工具沙箱 | Agent 调用的外部工具要有权限/审计 |
| 中间结果持久化 | 多跳结果入 trace（可排查"它为什么这么答"） |
| 成本监控 | 每次请求的 tool_calls 次数与 token 上报 |
| 渐进上线 | 先单工具 Agent → 再多工具 → 再反思循环（逐步放开） |
| 降级路径 | Agent 失败 → 回退传统 RAG（保底可用） |

> 🎯 **核心要点**：生产级 Agentic RAG = "**Router 分流 + Agent 循环（有上限）+ 反思质检 + 降级路径**"四件套——**自主性给"聪明"，上限与降级给"可靠"**。没有降级路径的 Agentic RAG 是事故预告。

---

**下一模块**：[18-长上下文时代RAG定位之争](./18-长上下文时代RAG定位之争.md) / **返回总览**：[12-RAG拓展优化深化总览](./12-RAG拓展优化深化总览.md)
