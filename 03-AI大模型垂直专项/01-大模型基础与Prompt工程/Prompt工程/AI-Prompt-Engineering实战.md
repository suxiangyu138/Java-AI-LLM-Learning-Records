# Prompt Engineering 实战：从 Zero-Shot 到 CoT 完整指南

> **核心摘要**：本文从基础提示技巧到高级工程化实践，全面覆盖 Prompt Engineering 的核心技术，包括 Zero-Shot、Few-Shot、Chain-of-Thought、Self-Consistency、ReAct 等范式，并提供 Prompt 版本管理和 A/B 测试等工程化方法。

> **前置阅读**：[[大模型基础核心知识点：Prompt工程]]、[[Prompt Engineering详细知识点]]

---

## 目录

1. [什么是 Prompt Engineering](#1-什么是-prompt-engineering)
2. [基础提示技巧](#2-基础提示技巧)
3. [进阶技巧](#3-进阶技巧)
4. [高级技巧](#4-高级技巧)
5. [Prompt 工程化实践](#5-prompt-工程化实践)
6. [常见失败模式与修复](#6-常见失败模式与修复)
7. [实战检查清单](#7-实战检查清单)
8. [核心要点回顾](#8-核心要点回顾)
9. [参考资料](#9-参考资料)

---

## 1. 什么是 Prompt Engineering

**Prompt Engineering（提示工程）** 是设计输入文本以引导大模型产生期望输出的技术。它是连接"人类意图"和"AI 理解"的桥梁。

```
人类意图 ──→ Prompt Engineering ──→ 大模型 ──→ 高质量输出
```

> **重点**：Prompt Engineering 是成本最低、回报最高的 AI 技能。好的 Prompt 能让同一模型的输出质量天差地别。

---

## 2. 基础提示技巧

### 2.1 指令清晰化

```markdown
# 模糊指令
解释 Spring

# 结构化指令
你是一位 Java 后端技术专家。请用 200 字以内，为一个有 2 年 Java 经验的开发者，
解释 Spring Framework 的 IoC（控制反转）概念。要求：
1. 给出定义
2. 提供一个简单代码示例
3. 对比传统 new 对象的方式
```

### 2.2 角色设定

```markdown
你是一位资深 Java 后端面试官，有 10 年互联网大厂经验。
请针对以下简历提出 5 个技术面试问题，按难度递增排列。
```

### 2.3 输出格式控制

```markdown
请以 JSON 格式输出，不要包含其他内容：
{
  "concept": "概念名称",
  "definition": "一句话定义",
  "example": "Java 代码示例"
}
```

---

## 3. 进阶技巧

### 3.1 Zero-Shot Prompting（零样本）

不提供示例，直接提问。适用于简单任务。

```markdown
将以下 Java 代码重构为使用 Stream API：

List<String> result = new ArrayList<>();
for (User user : users) {
    if (user.getAge() > 18) {
        result.add(user.getName());
    }
}
```

### 3.2 Few-Shot Prompting（少样本）

提供 2-3 个示例，引导输出格式和逻辑。

```markdown
请将给定的 MySQL 建表语句转换为 Java Entity 类（使用 JPA 注解）。

示例输入：
CREATE TABLE user (id BIGINT PRIMARY KEY, name VARCHAR(50), age INT);
示例输出：
@Entity @Table(name = "user")
public class User {
    @Id private Long id;
    @Column(length = 50) private String name;
    private Integer age;
}

现在请转换：
CREATE TABLE order (id BIGINT PRIMARY KEY, user_id BIGINT, ...);
```

### 3.3 Chain-of-Thought（CoT，思维链）

让模型展示推理过程，显著提升复杂问题准确率。

```markdown
请按以下步骤分析 Java 代码是否存在线程安全问题：
1. 分析 increment() 方法的字节码层面操作
2. 判断各操作是否为原子操作
3. 给出结论和修复方案
```

### 3.4 Zero-Shot CoT

只需加上 `Let's think step by step`：

```markdown
请判断以下 SQL 是否存在性能问题，并逐步分析（Let's think step by step）：
SELECT * FROM orders o LEFT JOIN users u ON o.user_id = u.id
WHERE YEAR(o.create_time) = 2024 ORDER BY o.amount DESC
```

---

## 4. 高级技巧

### 4.1 Self-Consistency（自洽性）

同一问题多次采样，取多数结果：

```python
def self_consistent_answer(client, prompt, n=5):
    """对同一个 prompt 采样 n 次，返回最常见的答案"""
    answers = []
    for _ in range(n):
        resp = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7
        )
        answers.append(resp.choices[0].message.content)
    return max(set(answers), key=answers.count)
```

### 4.2 ReAct（Reasoning + Acting）

结合推理与行动，适用于 Agent 开发：

```markdown
你可以使用以下工具：
- search(query)：搜索技术文档
- execute(code)：执行 Java 代码

问题：找出 Spring Boot 3.0 中 @PostConstruct 的替代方案

请按以下格式回答：
Thought: 我需要了解这个问题的背景
Action: search("Spring Boot 3.0 PostConstruct deprecated")
Observation: [搜索结果]
Thought: 根据搜索结果，我需要给出替代方案
Answer: [最终答案]
```

### 4.3 结构化 Prompt 模板

```python
def build_code_review_prompt(code, language="java"):
    return f"""
你是一位资深代码审查专家。

## 角色
- 10 年{language}开发经验
- 熟悉设计模式和最佳实践

## 审查维度
1. 逻辑正确性  2. 性能问题  3. 安全漏洞  4. 代码可读性  5. 设计模式合理性

## 输出格式
每发现一个问题，请用以下格式：
- **严重程度**：[高/中/低]
- **位置**：[第 N 行]
- **问题**：[描述]
- **建议**：[修改方案]

## 代码
```{language}
{code}
```"""
```

---

## 5. Prompt 工程化实践

### 5.1 Prompt 版本管理

```python
# prompts.py — 集中管理 Prompt 模板
PROMPTS = {
    "code_review_v1": """你是一位代码审查专家。请审查以下代码：{code}""",
    "code_review_v2": """
你是一位资深{language}开发专家。请从以下维度审查代码：
1. 正确性 2. 性能 3. 安全 4. 可维护性
代码：{code}""",
}

def get_prompt(name, version="v1", **kwargs):
    return PROMPTS[f"{name}_{version}"].format(**kwargs)
```

### 5.2 A/B 测试 Prompt

```python
def ab_test_prompts(client, test_cases, prompt_a, prompt_b):
    """对比两个 Prompt 的效果"""
    results = {"a": [], "b": []}
    for case in test_cases:
        import random
        if random.random() < 0.5:
            resp_a = call_with_prompt(client, prompt_a, case)
            resp_b = call_with_prompt(client, prompt_b, case)
        else:
            resp_b = call_with_prompt(client, prompt_b, case)
            resp_a = call_with_prompt(client, prompt_a, case)
        results["a"].append(resp_a)
        results["b"].append(resp_b)
    return results
```

---

## 6. 常见失败模式与修复

| 问题 | 原因 | 修复 |
|------|------|------|
| 回答过于宽泛 | 指令不够具体 | 添加角色、字数限制、输出格式 |
| 输出格式不稳定 | 未指定格式 | 使用 Few-Shot 示例或 JSON 格式要求 |
| 推理错误 | 模型跳过推理步骤 | 使用 CoT：`Let's think step by step` |
| 产生幻觉 | 知识边界模糊 | 要求引用来源，或结合 RAG |
| 拒绝回答 | 安全过滤过严 | 调整措辞，添加使用场景说明 |

---

## 7. 实战检查清单

- [ ] 是否设定了明确的角色？
- [ ] 是否给出了具体的输出格式？
- [ ] 对于推理类问题，是否引导了思考过程？
- [ ] 是否有 Few-Shot 示例来约束输出风格？
- [ ] 是否考虑了边界条件和错误情况？
- [ ] Prompt 长度是否适中（避免过长导致注意力衰减）？

---

## 8. 核心要点回顾

- Prompt Engineering 的基础技巧包括指令清晰化、角色设定和输出格式控制
- Zero-Shot 适合简单任务，Few-Shot 适合格式要求严格的场景
- CoT（思维链）显著提升逻辑推理类任务的准确率
- Self-Consistency 多次采样取多数，降低单次输出错误率
- ReAct 框架将推理和行动结合，是 Agent 开发的核心模式
- 工程化实践包括 Prompt 版本管理、A/B 测试和效果评估

---

## 9. 参考资料

1. Wei et al. "Chain-of-Thought Prompting Elicits Reasoning in Large Language Models". NeurIPS 2022
2. Yao et al. "ReAct: Synergizing Reasoning and Acting in Language Models". ICLR 2023
3. Wang et al. "Self-Consistency Improves Chain of Thought Reasoning in Language Models". ICLR 2023
4. Kojima et al. "Large Language Models are Zero-Shot Reasoners". NeurIPS 2022
5. OpenAI Prompt Engineering Guide
