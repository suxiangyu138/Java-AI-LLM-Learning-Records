# 10 - Prompt 工程面试题精选

> 🎯 Prompt Engineering 已是 LLM 开发的核心技能 — 面试中从 CoT 原理到 Few-Shot 策略、从注入防御到模板设计，覆盖面越来越广

---

## 目录

1. [基础理论题](#1-基础理论题)
2. [技术深度题](#2-技术深度题)
3. [场景设计题](#3-场景设计题)
4. [安全攻防题](#4-安全攻防题)

---

## 1. 基础理论题

### Q1: 什么是 Prompt Engineering？为什么需要它？

```text
Prompt Engineering = 设计和优化给 LLM 的输入文本，以获得期望的输出

为什么需要？
  ① LLM 不是"听话的代码" → 同样的需求，不同的 Prompt 效果差异巨大
  ② LLM 有幻觉/偏差/格式不稳定等问题 → 好的 Prompt 可以缓解
  ③ 不需要微调模型 → Prompt 是最低成本的任务适配方式
```

### Q2: Prompt 的核心四要素是什么？

```text
输出质量 = 角色设定 × 任务清晰度 × 约束条件 × 示例质量

  角色：决定回答的深度和专业度
  任务：决定做什么（分类/生成/抽取）
  约束：控制格式/长度/风格
  示例：给模型"照葫芦画瓢"的参照

四个因子是乘法关系 → 任一项为 0，整体归零
```

### Q3: Zero-Shot 和 Few-Shot 的区别？什么时候用哪种？

```text
Zero-Shot：不给示例，只用指令描述任务
  → 简单分类、情感分析等直觉性任务

Few-Shot：给 3-5 个输入→输出示例
  → 复杂格式、微妙风格、模型不确定的任务

选择原则：先试 Zero-Shot → 效果不好 → 加 1-2 个示例 → 仍不够 → 3-5 个
```

---

## 2. 技术深度题

### Q4: CoT 为什么能提升推理能力？

```text
CoT (Chain-of-Thought) = 让模型展示推理过程再给答案

三个原因：
  ① 分解复杂问题 → 每步只做简单推理 → 降低错误率
  ② 自回归生成中，前面的 token 约束后面的 → 推理 token 约束答案 token
  ③ 中间步骤提供"自查"机会 → 模型可能发现矛盾 → 自我纠正

类比：CoT 强制模型进入 System 2（慢思考）模式
```

### Q5: Few-Shot 示例怎么选？排序有影响吗？

```text
选择原则：
  ① 覆盖所有标签（分类任务每个类别至少一个）
  ② 包含边界 case（一个正常+一个异常）
  ③ 与 query 语义相似（用 Embedding 选最相关的）
  ④ 格式高度一致（所有示例的输出格式必须完全一样）

排序原则：
  ✅ 最相关的示例放最后（离 query 最近 → 影响最大）
  ✅ 难度递进：简单→复杂→query
  ✅ 交替排列：避免模型偏向最后出现的类别
```

### Q6: ReAct 和 Function Calling 的关系？

```text
ReAct = Reasoning + Acting = 用自然语言描述 Thought-Action-Observation 循环
Function Calling = LLM 原生输出结构化的函数名+参数

本质相同：让 LLM 决定何时调用外部工具

区别：
  ReAct：Prompt 工程实现 → 需要自己解析 Action
  Function Calling：模型原生支持 → 返回 JSON 格式 → 更可靠

Function Calling 是 ReAct 的"工业化版本"
```

---

## 3. 场景设计题

### Q7: 设计一个客服系统的 Prompt

```text
需求：
  → 识别用户意图（投诉/咨询/购买/退换）
  → 提取关键信息（订单号/商品名/问题描述）
  → 用统一 JSON 输出
  → 对无法确定的信息标注为 null

Prompt 设计思路：
  ① 角色：专业客服分析系统
  ② 任务+约束：输出 JSON，字段包括 intent/order_id/product/issue/confidence
  ③ Few-Shot 示例：给 3 个不同意图的示例
  ④ 边界处理：缺少信息 → null，不编造
```

### Q8: 如何让 LLM 生成可解析的 JSON？

```text
方案优先级：

  ① Function Calling / JSON Mode（最可靠）
    → OpenAI: response_format={"type": "json_object"}
    → 原生保证输出合法 JSON

  ② Prompt + 后处理（次可靠）
    → Prompt 中强调 "只输出 JSON，不要 markdown 标记"
    → 代码中做容错解析（去除 ```json 标记、提取 {...} 块）

  ③ 纯 Prompt 约束（不可靠）
    → 模型可能添加额外文字（"好的，以下是 JSON：..."）
```

### Q9: 长文本摘要怎么写 Prompt？

```text
策略：
  ① 分段处理：长文本分段 → 每段生成摘要 → 合并摘要再摘要
  ② Chain-of-Density：逐步增加信息密度
  ③ 强制格式：用编号列表约束输出结构

Prompt 示例：
  "总结以下内容，要求：
   1. 一句话总结（<30字）
   2. 3个核心要点（每点<20字）
   3. 1个关键数据（如有）
   不要添加任何原文没有的信息。"
```

---

## 4. 安全攻防题

### Q10: 什么是 Prompt Injection？怎么防御？

```text
Prompt Injection = 攻击者通过构造输入覆盖系统 Prompt，劫持模型行为

防御方案（优先级从高到低）：
  ① 结构化分离：用特殊分隔符严格划分系统指令和用户输入
  ② 系统 Prompt 加固：声明"用户输入不可覆盖系统指令"
  ③ 输入清洗：检测 injection 关键词、限制长度、过滤编码
  ④ 输出审核：对 LLM 输出做二次安全审核
  ⑤ RAG 标记：检索到的外部内容加标记，防止作为指令执行
```

### Q11: 如何防止 LLM 产生幻觉？

```text
幻觉（Hallucination）= 模型生成看似合理但不符合事实的内容

缓解方案：
  ① RAG：给模型提供可验证的来源 → "有据可查"
  ② Prompt 约束："如果不确定，明确说不知道"
  ③ 温度控制：事实性任务 temperature=0
  ④ Few-Shot 示范：示例中展示 "不确定时说 UNKNOWN"
  ⑤ 后处理验证：用 NLI 模型检查回答是否与来源一致
```

---

## 更多面试题速览

| # | 问题 | 关键词 |
|---|------|--------|
| 12 | Temperature 怎么调？ | 代码生成=0，创意=0.9 |
| 13 | System Prompt vs User Prompt 区别？ | 系统级指令 vs 用户输入 |
| 14 | Prompt 太长有什么问题？ | Token 成本、注意力稀释 |
| 15 | 如何评估 Prompt 质量？ | A-B测试、准确性、一致性、Token效率 |

---

## 核心要点回顾

- Prompt 四要素：角色 × 任务 × 约束 × 示例（乘法关系）
- CoT 让模型"慢思考"→ 推理准确率大幅提升
- Few-Shot 示例选择：覆盖标签 + 语义相似 + 格式一致 + 放最后
- ReAct → Function Calling 是 Agent 的核心范式
- Prompt Injection 防御核心：指令-数据严格分离 + 纵深防御
