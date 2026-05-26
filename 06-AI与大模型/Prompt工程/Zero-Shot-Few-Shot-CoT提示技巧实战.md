# Zero-Shot / Few-Shot / CoT 提示技巧实战

> **核心认知**：Prompt Engineering 是用大模型**成本最低、回报最高**的技能。写好 Prompt，不用微调就能让模型输出质量大幅提升。
> **前置阅读**：`Prompt Engineering详细知识点.md`

---

## 1. 三种核心范式对比

| 范式 | 解释 | 适用场景 | 成本 |
|------|------|----------|------|
| **Zero-Shot** | 不给示例，直接描述任务 | 简单任务（翻译、摘要、分类） | 最低 |
| **Few-Shot** | 给 2-5 个示例，让模型模仿 | 格式要求严格、输出有特定风格 | 中等 |
| **CoT（思维链）** | 要求模型展示推理步骤 | 数学、逻辑推理、代码调试 | 较高（Token 多） |

---

## 2. Zero-Shot 提示

### 2.1 基础模板

```python
def zero_shot_prompt(task: str, context: str = "") -> str:
    """基础 Zero-Shot 模板"""
    return f"""{task}

{context if context else ""}
请直接输出结果，不需要额外解释。"""

# 示例：代码审查
prompt = zero_shot_prompt(
    task="找出以下 Java 代码中的潜在问题，包括：空指针风险、资源泄露、线程安全问题",
    context="""
```java
public class UserService {
    private Connection conn;

    public User getUser(String id) {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id=" + id);
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

### 2.2 Role Prompting（角色设定）

```python
SYSTEM_PROMPTS = {
    "java_expert": "你是一名资深 Java 后端开发工程师，有 10 年经验，擅长 Spring Boot 和微服务架构。",
    "code_reviewer": "你是一个严格的代码审查员，关注安全漏洞、性能瓶颈和代码规范。回答时先列出严重问题，再列建议。",
    "interviewer": "你是一个技术面试官，需要根据候选人的回答进行追问，评估其技术深度。",
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

# 使用时配合 json.loads + 异常处理
import json, re

def parse_structured_response(response: str) -> dict:
    try:
        return json.loads(response)
    except json.JSONDecodeError:
        # 尝试提取 JSON 代码块
        match = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', response)
        if match:
            return json.loads(match.group(1))
        raise ValueError("无法解析模型输出为 JSON")
```

---

## 3. Few-Shot 提示

### 3.1 基础示例

```python
def few_shot_prompt(task: str, examples: list[tuple[str, str]], new_input: str) -> str:
    """Few-Shot 模板"""
    prompt_parts = [task, "", "示例："]
    for i, (inp, out) in enumerate(examples, 1):
        prompt_parts.append(f"输入{i}: {inp}")
        prompt_parts.append(f"输出{i}: {out}")
        prompt_parts.append("")
    prompt_parts.append(f"输入{len(examples) + 1}: {new_input}")
    prompt_parts.append(f"输出{len(examples) + 1}:")
    return "\n".join(prompt_parts)


# 示例：Java 代码注释生成
examples = [
    (
        "public void setAge(int age) { this.age = age; }",
        "/**\n * 设置用户年龄\n * @param age 用户年龄，必须为非负整数\n */"
    ),
    (
        "public List<User> findByAgeGreaterThan(int age) {\n    return userRepo.findByAgeGreaterThan(age);\n}",
        "/**\n * 查询年龄大于指定值的所有用户\n * @param age 最小年龄（不包含）\n * @return 符合条件的用户列表，无结果时返回空列表\n */"
    ),
]

prompt = few_shot_prompt(
    task="为以下 Java 方法生成规范的 Javadoc 注释（中文）",
    examples=examples,
    new_input="public void deleteById(Long id) {\n    userRepo.deleteById(id);\n}"
)
```

### 3.2 动态示例选择（RAG + Few-Shot）

```python
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

class DynamicFewShot:
    """从示例库中动态选择与输入最相似的示例"""

    def __init__(self, examples: list[dict], embed_fn):
        """
        examples: [{"input": "...", "output": "..."}, ...]
        embed_fn: 文本 → Embedding 向量的函数
        """
        self.examples = examples
        self.embeddings = [embed_fn(ex["input"]) for ex in examples]
        self.embed_fn = embed_fn

    def select_examples(self, query: str, k: int = 3) -> list[tuple[str, str]]:
        query_emb = self.embed_fn(query)
        similarities = cosine_similarity([query_emb], self.embeddings)[0]
        top_indices = np.argsort(similarities)[-k:][::-1]
        return [
            (self.examples[i]["input"], self.examples[i]["output"])
            for i in top_indices
        ]
```

---

## 4. Chain-of-Thought（思维链）

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

# 示例：SQL 优化问题
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

```python
def few_shot_cot_prompt(question: str, examples: list[tuple[str, str]]) -> str:
    """Few-Shot + CoT 组合：示例中也包含推理步骤"""
    prompt = "请按示例的推理方式，一步一步思考后给出答案。\n\n"
    for i, (q, a) in enumerate(examples, 1):
        prompt += f"问题{i}: {q}\n答案{i}: {a}\n\n"
    prompt += f"问题{len(examples) + 1}: {question}\n答案{len(examples) + 1}:"
    return prompt


# 示例：Java 异常分析
debug_examples = [
    (
        "程序报 NullPointerException at UserService.java:25",
        "分析：NPE 发生在 UserService.java 第25行。\n推理：查看该行代码为 user.getName()，说明 user 对象为 null。可能是 getUser() 方法返回了 null，或者传入的参数为 null。\n答案：建议在第25行之前添加 null 检查，或使用 Optional.ofNullable(user).map(User::getName)。同时检查 getUser() 方法的返回值是否可能为 null。"
    ),
]

prompt = few_shot_cot_prompt(
    "启动 Spring Boot 应用时报错 'Failed to configure a DataSource: 'url' attribute is not specified'",
    debug_examples
)
```

### 4.3 Self-Consistency（多次采样 + 投票）

```python
def self_consistency(prompt: str, client: OpenAI, n: int = 5) -> str:
    """对同一个问题采样 n 次，选出现最多的答案"""
    responses = []
    for _ in range(n):
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7  # 非 0 温度产生多样性
        )
        responses.append(response.choices[0].message.content)

    # 简化版投票：选出现频率最高的答案
    from collections import Counter
    most_common = Counter(responses).most_common(1)[0][0]
    return most_common
```

---

## 5. 实战 Prompt 模板库

### 5.1 Java 代码生成

```
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

```
请根据以下 Spring Boot Controller 代码生成 API 文档（Markdown 格式）：

```java
{code}
```

文档需包含：
1. 接口路径、请求方法
2. 请求参数表格（参数名、类型、必填、说明）
3. 响应示例
4. 错误码说明
```

### 5.3 Bug 分析

```
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
2. 根因分析
3. 修复方案（含代码 diff）
4. 预防措施（如何避免同类问题）
```

### 5.4 学习路径推荐

```
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

## 6. Prompt 调试技巧

### 6.1 迭代优化流程

```
1. 写最简单的 Prompt → 看输出
2. 发现输出哪里不对 → 加约束条件
3. 输出格式不统一 → 加"请严格按照以下格式输出"
4. 输出不够深入 → 加"请一步一步思考"（CoT）
5. 输出和期望有偏差 → 加 2-3 个 Few-Shot 示例
6. 还不够好 → 考虑微调或换更强模型
```

### 6.2 常见问题与解法

| 问题 | 解法 |
|------|------|
| 输出太短 | 加 `请详细回答，至少 500 字` |
| 输出太长 | 加 `请用一句话总结` 或限制输出格式 |
| 输出格式不稳定 | 加 JSON 格式约束 + `不要包含其他内容` |
| 推理错误 | 加 `请一步一步思考` (CoT) |
| 输出风格不对 | Few-Shot 给 2-3 个目标风格的示例 |
| 输出包含幻觉 | 加 `如果不知道请说"不知道"，不要编造` |

### 6.3 Evaluation 驱动的提示优化

```python
def evaluate_prompt(prompt: str, test_cases: list[dict], client: OpenAI) -> dict:
    """用测试集评估 Prompt 效果"""
    scores = []
    for case in test_cases:
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt.format(**case)}]
        )
        actual = response.choices[0].message.content

        # 简单评估：检查关键词是否存在
        score = sum(
            1 for keyword in case.get("expected_keywords", [])
            if keyword in actual
        ) / max(len(case.get("expected_keywords", [])), 1)

        scores.append({"input": case, "output": actual, "score": score})

    avg_score = sum(s["score"] for s in scores) / len(scores)
    return {"average_score": avg_score, "details": scores}
```

---

## 7. API 调用的 Prompt 管理

```python
class PromptManager:
    """Prompt 模板管理"""

    def __init__(self, templates: dict[str, str] = None):
        self.templates = templates or {}

    def register(self, name: str, template: str):
        self.templates[name] = template

    def render(self, name: str, **kwargs) -> str:
        if name not in self.templates:
            raise KeyError(f"Prompt '{name}' not found")
        return self.templates[name].format(**kwargs)


# 使用
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

---

## 学习建议

1. **从 Zero-Shot 开始**：大部分任务 Zero-Shot + 清晰指令就够用
2. **遇到瓶颈再加 Few-Shot**：2-3 个精心挑选的示例比 10 个随便选的强
3. **推理任务必须加 CoT**：数学、逻辑、Bug 定位场景，CoT 效果提升显著
4. **把 Prompt 当代码管理**：版本控制、测试用例、团队 Review
