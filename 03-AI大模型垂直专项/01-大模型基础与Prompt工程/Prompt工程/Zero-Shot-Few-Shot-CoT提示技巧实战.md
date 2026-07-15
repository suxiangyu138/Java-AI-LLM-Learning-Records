# Zero-Shot / Few-Shot / CoT 提示技巧实战

> **核心公式**：Prompt 质量 = 指令清晰度 × 示例匹配度 × 推理引导力。Zero-Shot 打底、Few-Shot 精调、CoT 攻坚——三层递进，无需微调即可让模型输出质量提升 3-10×。

> **前置阅读**：[[Prompt Engineering核心技巧]]

---

## 目录

1. [三种范式对比与选型决策](#1-三种范式对比与选型决策)
2. [Zero-Shot 提示](#2-zero-shot-提示)
3. [Few-Shot 提示](#3-few-shot-提示)
4. [Chain-of-Thought 思维链](#4-chain-of-thought-思维链)
5. [实战 Prompt 模板库](#5-实战-prompt-模板库)
6. [Prompt 调试与评估](#6-prompt-调试与评估)
7. [工程化管理](#7-工程化管理)
8. [参考资料](#8-参考资料)

---

## 1. 三种范式对比与选型决策

### 1.1 核心对比

| 范式 | 工作机制 | 输出特点 | 适用场景 | Token 成本 | 推荐起点 |
|------|----------|----------|----------|:---------:|:-------:|
| **Zero-Shot** | 纯指令驱动，无示例 | 灵活但格式不可控 | 简单分类、翻译、摘要 | 最低 | ⭐ 大部分任务 |
| **Few-Shot** | 2-5 个示例引导模式匹配 | 格式稳定、风格可控 | 格式严格、特定风格输出 | 中等 | 遇到瓶颈后加 |
| **CoT** | 显式要求展示推理步骤 | 逻辑性强、可验证性强 | 数学、逻辑、Bug 定位、代码审计 | 较高 | 推理类任务必加 |

### 1.2 选型决策图

```text
开始 → 这个任务有明确对错答案吗？
  │
  ├── 是 → 需要复杂推理吗？
  │         ├── 是 → CoT / Few-Shot CoT
  │         └── 否 → Zero-Shot
  │
  └── 否 → 输出格式/风格严格要求？
            ├── 是 → Few-Shot（给 2-3 个目标格式示例）
            └── 否 → Zero-Shot + 结构化输出约束
```

### 1.3 效果提升阶梯

| 层级 | 手段 | 典型提升 |
|:----:|------|:------:|
| L1 | Zero-Shot + 清晰指令 | 基线 |
| L2 | L1 + 角色设定（Role Prompting） | +20-30% |
| L3 | L2 + 结构化输出约束（JSON 格式） | +10-20% |
| L4 | L3 → 升级为 Few-Shot（2-3 个精选示例） | +20-40% |
| L5 | L4 → 升级为 CoT（推理场景） | +30-50% |
| L6 | L5 + Self-Consistency（多次采样投票） | +10-15% |

> **重点**：写好 Prompt 是使用大模型成本最低、回报最高的技能。L1-L3 零成本可用；L4-L6 多花一些 Token，换来输出质量数量级提升。

---

## 2. Zero-Shot 提示

### 2.1 基础模板

```python
def zero_shot_prompt(task: str, context: str = "") -> str:
    """基础 Zero-Shot 模板：任务描述 + 可选上下文 + 直接输出约束"""
    return f"""{task}

{context if context else ""}
请直接输出结果，不需要额外解释。"""


# === 实战示例：Java 代码审查 ===
prompt = zero_shot_prompt(
    task="找出以下 Java 代码中的潜在问题，包括：空指针风险、资源泄露、线程安全问题",
    context="""
```java
public class UserService {
    private Connection conn;

    public User getUser(String id) {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT * FROM users WHERE id=" + id);
        if (rs.next()) {
            return new User(rs.getString("name"), rs.getInt("age"));
        }
        return null;
    }
}
```
"""
)
```

| Zero-Shot 误区 | 正确做法 |
|---------------|----------|
| "帮我写个排序" | "用 Java 实现一个稳定的归并排序，时间复杂度 O(n log n)，返回新数组不修改原数组" |
| "优化这段代码" | "找出以下代码的性能瓶颈，给出 3 个优化方案，按收益从高到低排列" |

### 2.2 角色设定（Role Prompting）

> 给模型一个明确的专业身份，输出专业度和风格显著提升。

```python
SYSTEM_PROMPTS = {
    "java_expert": "你是一名资深 Java 后端开发工程师，有 10 年经验，"
                   "擅长 Spring Boot 和微服务架构。请用通俗语言解释。",
    "code_reviewer": "你是一个严格的代码审查员，关注安全漏洞、性能瓶颈和代码规范。"
                     "每个问题标注严重程度（严重/中等/建议）。",
    "interviewer": "你是一个技术面试官，根据候选人的回答进行追问，"
                   "评估其技术深度，给出评分和反馈。",
}

def chat_with_role(prompt: str, role: str) -> str:
    system = SYSTEM_PROMPTS.get(role, "你是一个有用的AI助手。")
    messages = [
        {"role": "system", "content": system},
        {"role": "user", "content": prompt}
    ]
    # ... API 调用
```

### 2.3 结构化输出控制

```python
import json
import re


def structured_output_prompt(task: str) -> str:
    """要求模型输出结构化 JSON，方便程序解析"""
    return f"""{task}

请严格按照以下 JSON 格式输出，不要包含任何其他内容：
{{
    "summary": "一句话总结",
    "key_points": ["要点1", "要点2"],
    "confidence": 0.0-1.0,
    "suggestions": ["建议1", "建议2"]
}}"""


def parse_structured_response(response: str) -> dict:
    """容错解析：优先直接 JSON，失败则从代码块中提取"""
    try:
        return json.loads(response)
    except json.JSONDecodeError:
        # 尝试从 ```json ... ``` 代码块中提取
        match = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', response)
        if match:
            return json.loads(match.group(1))
        raise ValueError("无法解析模型输出为 JSON")
```

---

## 3. Few-Shot 提示

### 3.1 基础实现

```python
def few_shot_prompt(task: str, examples: list[tuple[str, str]],
                    new_input: str) -> str:
    """Few-Shot 模板：任务描述 + 示例 + 新输入，让模型模仿示例格式"""
    prompt_parts = [task, "", "示例："]
    for i, (inp, out) in enumerate(examples, 1):
        prompt_parts.append(f"输入{i}: {inp}")
        prompt_parts.append(f"输出{i}: {out}")
        prompt_parts.append("")
    prompt_parts.append(f"输入{len(examples) + 1}: {new_input}")
    prompt_parts.append(f"输出{len(examples) + 1}:")
    return "\n".join(prompt_parts)


# === 示例：Java 代码注释生成 ===
examples = [
    (
        "public void setAge(int age) { this.age = age; }",
        "/**\n * 设置用户年龄\n * @param age 用户年龄，必须为非负整数\n */"
    ),
    (
        "public List<User> findByAgeGreaterThan(int age) {\n"
        "    return userRepo.findByAgeGreaterThan(age);\n}",
        "/**\n * 查询年龄大于指定值的所有用户\n"
        " * @param age 最小年龄（不包含）\n"
        " * @return 符合条件的用户列表\n */"
    ),
]

prompt = few_shot_prompt(
    task="为以下 Java 方法生成规范的 Javadoc 注释（中文）",
    examples=examples,
    new_input="public void deleteById(Long id) {\n"
              "    userRepo.deleteById(id);\n}"
)
```

### 3.2 Few-Shot 示例选择原则

| 原则 | ❌ 坏做法 | ✅ 好做法 |
|------|----------|----------|
| **相关性** | 给代码生成任务用了翻译示例 | 示例的领域/格式与目标一致 |
| **多样性** | 3 个示例都是同一种场景 | 覆盖边界 case（正常/异常/空值） |
| **数量** | 塞 10 个示例 | 2-3 个精选，超过 5 个边际收益递减 |
| **格式一致性** | 示例输出格式各不相同 | 所有示例输出格式完全一致 |

### 3.3 动态示例选择（RAG + Few-Shot）

> 通过向量相似度从示例库中匹配与当前输入最接近的示例，显著优于固定示例。

```python
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np


class DynamicFewShot:
    """从示例库中动态选择与输入最相似的 k 个示例"""

    def __init__(self, examples: list[dict], embed_fn):
        self.examples = examples
        self.embeddings = [embed_fn(ex["input"]) for ex in examples]
        self.embed_fn = embed_fn

    def select_examples(self, query: str, k: int = 3
                        ) -> list[tuple[str, str]]:
        query_emb = self.embed_fn(query)
        similarities = cosine_similarity([query_emb], self.embeddings)[0]
        top_indices = np.argsort(similarities)[-k:][::-1]
        return [
            (self.examples[i]["input"], self.examples[i]["output"])
            for i in top_indices
        ]
```

---

## 4. Chain-of-Thought 思维链

### 4.1 基础 CoT

```python
def cot_prompt(question: str) -> str:
    """带思维链的 Prompt 模板"""
    return f"""{question}

请一步一步思考，先分析问题，再给出推理过程，最后输出答案。
格式：
分析：
推理：
答案："""


# === SQL 优化示例 ===
sql_optimization_prompt = cot_prompt("""
以下 SQL 查询在大数据量下性能很差，请分析原因并给出优化方案：
SELECT u.name, COUNT(o.id) as order_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE o.create_time > '2025-01-01'
GROUP BY u.name
ORDER BY order_count DESC
LIMIT 100
""")
```

### 4.2 Few-Shot CoT（最稳定）

> 示例中包含完整的分析→推理→答案流程，模型模仿推理模式。

```python
def few_shot_cot_prompt(question: str,
                        examples: list[tuple[str, str]]) -> str:
    """Few-Shot + CoT：示例中也包含推理步骤"""
    prompt = "请按示例的推理方式，一步一步思考后给出答案。\n\n"
    for i, (q, a) in enumerate(examples, 1):
        prompt += f"问题{i}: {q}\n答案{i}: {a}\n\n"
    prompt += (f"问题{len(examples) + 1}: {question}\n"
               f"答案{len(examples) + 1}:")
    return prompt


# === Java 异常分析示例 ===
debug_examples = [
    ("程序报 NullPointerException at UserService.java:25",
     "分析：NPE 发生在第25行。"
     "推理：该行代码为 user.getName()，说明 user 对象为 null。"
     "答案：添加 null 检查，或使用 "
     "Optional.ofNullable(user).map(User::getName)。"),
]

prompt = few_shot_cot_prompt(
    "启动 Spring Boot 时报错 "
    "'Failed to configure a DataSource: url attribute is not specified'",
    debug_examples
)
```

### 4.3 Self-Consistency（多次采样 + 投票）

```python
from collections import Counter


def self_consistency(prompt: str, client, n: int = 5) -> str:
    """对同一个问题采样 n 次，选出现最多的答案"""
    responses = []
    for _ in range(n):
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7        # 非零温度确保每次采样有差异
        )
        responses.append(response.choices[0].message.content)
    return Counter(responses).most_common(1)[0][0]
```

| CoT 变体 | 适用场景 | 预期提升 |
|----------|----------|:------:|
| Zero-Shot CoT（加"一步一步思考"） | 简单推理 | +10-20% |
| Few-Shot CoT（示例含推理步骤） | 复杂推理 | +30-50% |
| Self-Consistency（多次采样投票） | 高准确性需求 | +10-15% |
| Tree-of-Thought（多分支探索） | 开放性问题 | 探索型 |

---

## 5. 实战 Prompt 模板库

### 5.1 Java 代码生成

```text
你是一位资深 Java 后端工程师。请根据以下需求生成代码：

需求：{requirement}
技术栈：Spring Boot {version}, JDK {version}

要求：
1. 遵循阿里巴巴 Java 开发手册规范
2. 包含必要的异常处理
3. 包含单元测试
4. 关键逻辑添加注释

请先输出代码，再解释关键技术决策。
```

### 5.2 API 文档生成

```text
请根据以下 Spring Boot Controller 代码生成 API 文档（Markdown 格式）：

```java
{code}
```

文档需包含：
1. 接口路径、请求方法
2. 请求参数表格（参数名 | 类型 | 必填 | 说明）
3. 响应示例
4. 错误码说明
```

### 5.3 Bug 分析（CoT 模板）

```text
你是 Java 代码调试专家。分析以下错误日志和代码，定位 Bug 根因并给出修复方案。

错误日志：
```
{stack_trace}
```

相关代码：
```java
{code}
```

请按以下格式输出：
1. 错误类型与直接原因
2. 根因分析（追踪调用链）
3. 修复方案（含代码 diff）
4. 预防措施（如何避免同类问题）
```

### 5.4 学习路径推荐

```text
你是一位技术成长顾问。根据以下信息，制定个性化的学习路径：
- 当前水平：{current_level}
- 目标方向：{target}
- 每天学习时间：{hours} 小时
- 偏好学习方式：{style}（文档/视频/项目驱动）

请输出：
1. 按周拆解的学习计划
2. 每阶段的实战项目推荐
3. 关键资源链接
```

---

## 6. Prompt 调试与评估

### 6.1 迭代优化六步法

```text
① 写最简单的 Prompt → 看输出
② 发现输出哪里不对 → 加约束条件
③ 输出格式不统一 → 加"请严格按照以下格式输出"
④ 输出不够深入 → 加"请一步一步思考"（CoT）
⑤ 输出和期望有偏差 → 加 2-3 个 Few-Shot 示例
⑥ 还不够好 → 考虑微调或换更强模型
```

### 6.2 常见问题速查

| 问题 | 解法 |
|------|------|
| 输出太短 | 加"请详细回答，至少 500 字" |
| 输出太长 | 加"请用一句话总结"或限制输出格式 |
| 输出格式不稳定 | 加 JSON 格式约束 + "不要包含其他内容" |
| 推理错误 | 加"请一步一步思考"（CoT） |
| 输出风格不对 | Few-Shot 给 2-3 个目标风格示例 |
| 输出包含幻觉 | 加"如果不知道请说'不知道'，不要编造" |

### 6.3 Evaluation 驱动的提示优化

```python
def evaluate_prompt(prompt: str, test_cases: list[dict], client) -> dict:
    """用测试集评估 Prompt 效果，返回平均分和逐条详情"""
    scores = []
    for case in test_cases:
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user",
                       "content": prompt.format(**case)}]
        )
        actual = response.choices[0].message.content
        # 简单评估：检查期望关键词覆盖率
        score = sum(
            1 for kw in case.get("expected_keywords", [])
            if kw in actual
        ) / max(len(case.get("expected_keywords", [])), 1)
        scores.append({"input": case, "output": actual, "score": score})

    avg = sum(s["score"] for s in scores) / len(scores)
    return {"average_score": avg, "details": scores}
```

---

## 7. 工程化管理

```python
class PromptManager:
    """Prompt 模板管理：注册、渲染、版本控制"""

    def __init__(self, templates: dict[str, str] = None):
        self.templates = templates or {}

    def register(self, name: str, template: str):
        """注册一个 Prompt 模板"""
        self.templates[name] = template

    def render(self, name: str, **kwargs) -> str:
        """渲染模板，填充参数"""
        if name not in self.templates:
            raise KeyError(f"Prompt '{name}' not found")
        return self.templates[name].format(**kwargs)


# === 使用示例 ===
pm = PromptManager()
pm.register("code_review", """你是代码审查专家。
请审查以下代码，关注：{focus_areas}

代码：
```{language}
{code}
```

请按以下格式输出：
1. 严重问题（安全/数据）
2. 性能问题
3. 规范问题
4. 总评分（1-10）
""")

prompt = pm.render("code_review",
    focus_areas="SQL注入、空指针、资源泄露",
    language="java",
    code="public void transfer(...) { ... }"
)
```

| 工程化实践 | 说明 |
|-----------|------|
| **版本控制** | Prompt 模板纳入 Git，和代码一起 Review |
| **测试用例** | 每个模板配 3-5 个测试用例，改动后跑 Evaluation |
| **A/B 对比** | 新旧 Prompt 用同一测试集对比评分 |
| **Token 预算** | Few-Shot/CoT 模板标注 Token 消耗，控制成本 |

---

## 8. 参考资料

| # | 文献 | 要点 |
|---|------|------|
| 1 | Wei et al. "Chain-of-Thought Prompting Elicits Reasoning in LLMs" (NeurIPS 2022) | CoT 核心论文 |
| 2 | Wang et al. "Self-Consistency Improves CoT Reasoning" (ICLR 2023) | 多次采样投票 |
| 3 | Kojima et al. "Large Language Models are Zero-Shot Reasoners" (NeurIPS 2022) | "Let's think step by step" |
| 4 | Brown et al. "Language Models are Few-Shot Learners" (NeurIPS 2020) | Few-Shot 基础 |
| 5 | OpenAI Prompt Engineering Guide | 官方最佳实践 |
| 6 | LangChain Prompt Template 文档 | 工程化模板管理 |

---

> 🎯 **核心法则**：Zero-Shot 起步 → 遇到瓶颈加 Few-Shot → 推理任务加 CoT → 精度不够加 Self-Consistency。**把 Prompt 当代码管理**：版本控制、测试验证、持续迭代。
