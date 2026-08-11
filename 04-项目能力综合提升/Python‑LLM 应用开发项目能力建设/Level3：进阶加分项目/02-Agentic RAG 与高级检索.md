# 02 Agentic RAG 与高级检索

> 第二个加分项：把 Level2 的"线性 RAG"升级为"Agent 驱动的 RAG"——检索失败自动改写重试、多跳问题自动分解、答案质量自检回炉。理解 Agentic RAG 的演进逻辑，并在项目里落地"检索自愈 + 自检"闭环。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [从线性 RAG 到 Agentic RAG：演进逻辑](#2-从线性-rag-到-agentic-rag演进逻辑)
3. [检索自愈：失败检测与改写重试](#3-检索自愈失败检测与改写重试)
4. [查询改写与多跳分解](#4-查询改写与多跳分解)
5. [自检回炉：答案质量的最后闸门](#5-自检回炉答案质量的最后闸门)
6. [评估 Agentic RAG：指标与成本](#6-评估-agentic-rag指标与成本)
7. [常见坑](#7-常见坑)

---

## 1. 目标与验收

本模块的产出：Agentic RAG 闭环——检索失败检测节点、查询改写节点、答案自检节点接入 Level2 的图；评估对比（线性基线 vs Agentic）报告。验收标准：**能画出 Agentic 流程图（含回退与重试分支）**；**评估数据显示多跳/模糊问题上的提升**（忠实度或上下文召回上升）；**能讲清"什么时候该用 Agentic、什么时候线性就够"**。与仓库「RAG拓展优化深化」体系的分工：那边是检索进化的理论全景（五代演进、GraphRAG），本模块是项目里的落地子集。

## 2. 从线性 RAG 到 Agentic RAG：演进逻辑

Level2 的 RAG 是**线性流水线**：分块→检索→重排→注入→生成，一条直线走到黑——问题在于**没有反馈回路**：检索结果差，模型照样硬答（忠实度暴跌）；问题需要多步推理，一次检索答不全。Agentic RAG 的演进核心是**把"决策"注入流程**：图的分支节点根据中间结果决定"继续走、重试、还是换策略"——这正是 LangGraph 条件边的用武之地（Level2 05 的图化投资在这里第二次兑现）。演进方向（2026 实践图谱）：**路由**（简单问题走快速通道、复杂问题走深度通道）→ **自愈**（检索失败重试/改写）→ **多跳**（问题分解分步检索）→ **自检**（生成后验证再交付）。**核心思想一句话：把"人看到坏答案再重问"的循环，交给系统自动完成**。注意边界：Agentic 循环多耗 3-10 倍 token（Level3 01 的成本警告同样适用）——**线性 RAG 是默认通道，Agentic 是问题驱动时的升级路径**，不是全面替换。

## 3. 检索自愈：失败检测与改写重试

检索自愈是 Agentic RAG 的第一环，两个节点：**失败检测**——怎么知道检索失败了？三个信号：检索结果数 < 阈值（空结果）、重排分数全部偏低（分数体检）、生成节点自评"上下文不足"（模型判断——最常用，让模型显式声明"依据不足"）。**改写重试**——检测到失败后，不让模型硬答，而是进入重试循环：

```python
def route_after_retrieve(state: RagState) -> str:
    if len(state["context"]) == 0:
        return "rewrite"              # 空结果 → 改写重试
    if max(state["rerank_scores"]) < 0.3:
        return "rewrite"              # 分数全低 → 改写重试
    return "generate"                 # 正常 → 生成

def rewrite_node(state: RagState) -> dict:
    # 让模型把问题改写得更具体（Level2 06 的查询改写升级为"失败驱动"）
    new_q = call_rewriter(state["question"], state.get("attempts", 0))
    return {"question": new_q, "attempts": state.get("attempts", 0) + 1}
```

实现要点：**重试有上限**（2 次改写仍失败 → 走"未找到"回答通道，绝不让模型编造——Level1 06 的拒答闸门在 Agentic 下依然有效）；**attempts 进 State**（每次改写携带尝试次数，防止死循环——图的循环边 + 计数护栏，LangGraph 检查点让每次尝试可回放）；**改写策略分层**（第一轮：问题扩写（加同义词/补上下文）；第二轮：换检索方式（改 top_k、开混合检索的 BM25 加权）——改写本身也是"参数调整"）。自愈的价值在评估里看：**失败问题集**（人为构造 20 个检索命中难的问题）的自愈成功率是验收指标。

## 4. 查询改写与多跳分解

检索效果差有两类根因：**问题太模糊**（"介绍一下项目"——检索出来一片混乱）与**问题太复杂**（"文档里说的缓存方案和 Level2 的有什么异同"——需要检索两个文档再对比）。对策：**HyDE 与多查询**（Level2 06 已介绍，Agentic 下变成"失败才触发"的按需策略）；**多跳分解**（Multi-Hop）——把复杂问题拆成子问题链，逐跳检索、结果串联：

```python
def decompose_node(state: RagState) -> dict:
    sub_questions = call_decomposer(state["question"])
    # 示例："A 和 B 有什么异同" → ["A 是什么", "B 是什么", "对比要点"]
    return {"sub_questions": sub_questions}

def hop_node(state: RagState, hop: str) -> dict:
    results = hybrid_search(hop, top_k=5)      # 每跳独立检索
    return {"hop_results": state.get("hop_results", []) + [results]}
```

多跳的实现形态：**顺序链**（子问题逐个检索，结果拼入上下文——用 LangGraph 的循环边 + 计数，或 Send API 并行化（2026 LangGraph 的动态扇出）——无依赖子问题并行检索，有依赖的顺序走）；**子问题的答案可能成为下一个问题的检索词**（"它"指代消解——把上一跳的答案注入下一跳的问题，是"记忆"在 RAG 里的形态）。验收问题集：10 个多跳问题，对比线性 RAG（一次检索硬答）与多跳的忠实度——**提升幅度就是简历里的数字**。

## 5. 自检回炉：答案质量的最后闸门

生成之后加一道**自检闸门**（Self-RAG 思想的落地）：让模型（或用第二个模型）验证答案——**忠实度检查**（答案的每个断言都能在检索上下文中找到依据？找得到的依据占比多少）、**相关性检查**（答的是否所问）。自检不合格 → 回炉（回到检索节点换策略重来）或降级（明确标注"部分依据不足"交付）。实现形态：生成节点后接条件边，路由函数调用自检模型打分：

```python
def self_check_node(state: RagState) -> dict:
    verdict = call_verifier(state["question"], state["context"], state["answer"])
    # verdict: {"faithful": bool, "reason": str}
    return {"verdict": verdict, "pass_attempts": state.get("pass_attempts", 0) + 1}

def route_after_check(state: RagState) -> str:
    if state["verdict"]["faithful"] or state["pass_attempts"] >= 2:
        return "finalize"             # 通过或重试耗尽 → 交付
    return "retrieve"                 # 不通过 → 回检索重来
```

设计要点：**自检模型可以降档**（验证是"分类任务"，flash 级即可——又是强生成弱校验的成本配比）；**回炉有界**（2 次自检不过即交付带警示的答案——"无限回炉"是成本失控源）；**verdict 的 reason 进日志**（08 篇的可观测性：每次自检失败的原因统计——"检索缺内容"和"模型乱编"是两类问题，优化方向完全不同）。自检闸门对忠实度的提升是 Agentic RAG 评估报告里最直观的数字。

## 6. 评估 Agentic RAG：指标与成本

Agentic RAG 的评估比线性复杂——**要回答"多花的 token 换来了什么"**。评估矩阵：**质量侧**——忠实度、上下文召回（RAGAS，Level2 06 的评估集直接复用，加一批"失败问题/多跳问题"专项集）；**成本侧**——每问题平均 token 数（03 篇的成本埋点数据）；**效率侧**——每问题平均 Agent 跳数。验收判断标准：**质量提升 / 成本增幅 的比值**——忠实度 +10 分但 token 翻 5 倍，值不值？决策记录写清楚。**分级策略是最优解**（2026 实践共识）：**简单问题走线性快速通道**（一次检索一次生成）、**复杂问题走 Agentic 深通道**（自愈/多跳/自检）——用路由节点分流（01 篇的 Supervisor 顺手做了这件事），整体成本只增加 30-50% 却拿到深问题的质量提升。**这个"分级通道"设计是简历项目里最能打的架构决策之一**——它证明你不只会加功能，还会控成本。

## 7. 常见坑

**无条件全面 Agentic 化**：所有请求都走重试/自检循环——用分级通道（第 6 节），简单问题线性走。

**重试不携带上下文**：改写后的问题没有保留第一次检索的线索——State 里保留 attempts 与每轮检索结果，改写时参考（"上一轮检索未命中，请换角度"比空改写有效）。

**自检与生成同模型同提示词**：自检变成"自我肯定"——自检用不同 prompt（甚至不同模型档位），评分才可信。

**多跳结果堆叠超上下文**：子问题检索结果全塞——每跳只取 top-2，汇总时按子问题归属组织（"关于 A："、"关于 B："），模型才知道哪个结论对应哪个子问题。

**失败检测阈值拍脑袋**：rerank 分数阈值 0.3 是怎么来的——用评估集跑分布，取"正确检索与错误检索的分界"，写进调优记录。

**回炉把错误放大**：第一次生成错，重试生成的还是错的（同样上下文）——回炉必须"变参数"（换检索策略/换提示词），"原样重试"是浪费 token 的表演。

> 🎯 **核心要点**：Agentic RAG 的本质是**给 RAG 装上决策回路**——失败检测决定何时重试、改写与分解决定怎么重试、自检决定结果能不能交付。用分级通道控成本、用评估数据定价值——**"线性为默认、Agentic 为升级、评估为裁判"**是本模块的一句话总结，也是面试回答"为什么用 Agentic RAG"的标准答案。

---

**下一模块**：[03 推理优化与成本控制](./03-推理优化与成本控制.md) | **返回总览**：[Level3 总览](./00-Level3%20进阶加分项目%20总览.md)

【参考来源】
- [Building Real-World Agentic RAG Systems](https://learned-memory.kit.com/posts/building-real-world-agentic-rag-systems)
- [Enterprise RAG in 2026 | Atolio](https://www.atolio.com/blog/enterprise-rag-guide)
- [Advanced RAG Systems 2026 | hjLabs](https://hjlabs.in/AIML/blog/post/advanced-rag-systems.html)
- [RAG 拓展优化深化 体系（本仓库）](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records)
