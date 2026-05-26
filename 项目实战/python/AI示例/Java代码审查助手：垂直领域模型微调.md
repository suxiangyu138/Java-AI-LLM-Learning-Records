# Java 代码审查助手：垂直领域模型微调

> **所属阶段**：阶段四 — 大模型微调与部署
> **技术栈**：Python + Qwen + LoRA + Ollama
> **项目定位**：垂直领域微调项目，可提交简历

---

## 1. 项目概述

使用 Java 项目代码微调 Qwen 模型，打造一个专业的 Java 代码审查助手。能自动检测代码质量、性能问题和安全漏洞。

### 核心检测维度
- **代码规范**：命名、格式、注释
- **性能问题**：不必要的对象创建、字符串拼接
- **安全漏洞**：SQL 注入、XSS、敏感信息泄露
- **设计模式**：硬编码、耦合度、接口缺失
- **最佳实践**：资源关闭、异常处理、空值检查

---

## 2. 项目流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────────┐
│ 1. 构造数据集 │ → │ 2. LoRA 微调 │ → │ 3. 模型评估  │ → │ 4. Ollama 部署│
│ 收集+标注代码 │    │  Qwen 7B    │    │ BLEU + 人工  │    │  本地服务     │
└─────────────┘    └─────────────┘    └─────────────┘    └──────────────┘
```

---

## 3. 数据集构造

### 3.1 数据格式

```json
{
  "instruction": "审查以下 Java 代码的安全性和性能",
  "input": "public class LoginService {\n    public boolean login(String username, String password) {\n        String sql = \"SELECT * FROM users WHERE username='\" + username + \"' AND password='\" + password + \"'\";\n        return jdbcTemplate.queryForList(sql).size() > 0;\n    }\n}",
  "output": "## 安全审查\n\n### 1. SQL 注入 (严重)\n**位置**: 第3行\n**问题**: 使用字符串拼接构建SQL，存在SQL注入漏洞\n**修复**:\n```java\nString sql = \"SELECT * FROM users WHERE username=? AND password=?\";\nreturn jdbcTemplate.queryForList(sql, username, password).size() > 0;\n```\n\n### 2. 密码明文存储 (严重)\n**问题**: 密码应该使用哈希存储，建议使用 BCrypt\n\n### 3. 缺少参数校验 (中等)\n**修复**: 添加 @NotNull @NotBlank 注解\n\n### 性能建议\n- 使用 SELECT COUNT 代替 SELECT * + size()\n- 添加索引: CREATE INDEX idx_username ON users(username)"
}
```

### 3.2 数据来源

| 来源 | 数量 | 说明 |
|------|------|------|
| 个人 Java 项目代码 | 50-100 条 | 最高价值，覆盖你的技术栈 |
| SonarQube 规则生成 | 200 条 | 基于静态分析规则自动生成 |
| GitHub 开源项目 PR | 100 条 | 提取 Code Review 评论数据 |
| LLM 生成 + 人工审核 | 200 条 | GPT-4 生成初始样本后人工修正 |
| **总计目标** | **500-600 条** | |

---

## 4. 核心代码

### 4.1 数据预处理

```python
# data_pipeline.py
import json

class CodeReviewDataset:
    """构造代码审查微调数据集"""

    @staticmethod
    def from_sonarqube_rules():
        """从 SonarQube 规则生成训练数据"""
        # 每条规则 → 生成多个包含该问题的代码示例
        rules = {
            "S1192": {
                "name": "字符串字面量不应重复",
                "bad_code": 'log.info("user login"); log.info("user login");',
                "good_code": 'private static final String MSG = "user login";\nlog.info(MSG);'
            },
            "S2095": {
                "name": "资源应正确关闭",
                "bad_code": "FileInputStream fis = new FileInputStream(\"data.txt\");\n// 缺少close",
                "good_code": "try (FileInputStream fis = new FileInputStream(\"data.txt\")) {\n    // ...\n}"
            }
        }
        samples = []
        for rule_id, rule in rules.items():
            samples.append({
                "instruction": "审查此代码的资源管理",
                "input": rule["bad_code"],
                "output": f"**{rule['name']}**\n修复:\n```java\n{rule['good_code']}\n```"
            })
        return samples

    @staticmethod
    def from_llm_generated(batch_size=50):
        """使用 LLM 批量生成训练数据"""
        from openai import OpenAI
        client = OpenAI(api_key="sk-xxx", base_url="https://api.deepseek.com")

        prompt = f"""请生成 {batch_size} 组 Java 代码审查训练数据。
每组包含：一段有问题的 Java 代码（含常见错误）和详细审查报告。

以 JSON 数组格式返回：
[{{"input": "有问题的代码", "output": "审查报告"}}]

覆盖以下问题类型：
- SQL 注入
- 空指针风险
- 资源未关闭
- 线程安全问题
- 性能低效写法"""

        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.8
        )
        return json.loads(response.choices[0].message.content)

    @staticmethod
    def format_qwen(samples):
        """格式化为 Qwen 对话模板"""
        formatted = []
        for s in samples:
            text = f"""<|im_start|>system
你是资深 Java 代码审查专家，擅长发现代码中的安全漏洞、性能问题和设计缺陷。<|im_end|>
<|im_start|>user
{s['instruction']}

```java
{s['input']}
```<|im_end|>
<|im_start|>assistant
{s['output']}<|im_end|>"""
            formatted.append({"text": text})
        return formatted
```

### 4.2 模型评估

```python
# evaluate.py
from nltk.translate.bleu_score import sentence_bleu

def evaluate_model(model, tokenizer, test_cases):
    """评估微调后的模型"""
    scores = []

    for case in test_cases:
        prompt = build_prompt(case["input"])
        inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
        outputs = model.generate(**inputs, max_new_tokens=512)
        prediction = tokenizer.decode(outputs[0], skip_special_tokens=True)

        # BLEU 评估
        reference = case["output"]
        bleu = sentence_bleu([reference.split()], prediction.split())
        scores.append({
            "bleu": bleu,
            "prediction": prediction,
            "reference": reference
        })

    avg_bleu = sum(s["bleu"] for s in scores) / len(scores)
    print(f"Average BLEU: {avg_bleu:.3f}")
    return scores

def human_evaluation(checkpoints, test_cases, n_reviewers=3):
    """人工评测 — 多位评审打分 (1-5)"""
    rubric = """
    评分标准 (1-5):
    1. 问题检测完整性 (是否遗漏严重问题)
    2. 修复建议可操作性 (是否给出具体代码)
    3. 分类准确性 (严重程度判断是否合理)
    4. 报告可读性 (格式是否清晰)
    """
    # ... 人工评审流程
```

---

## 5. 部署使用

```bash
# 1. 导入 Ollama
ollama create java-reviewer -f Modelfile

# 2. Python 调用
from openai import OpenAI
client = OpenAI(base_url="http://localhost:11434/v1", api_key="ollama")

def review(code):
    resp = client.chat.completions.create(
        model="java-reviewer",
        messages=[{"role": "user", "content": f"审查以下代码:\n```java\n{code}\n```"}]
    )
    return resp.choices[0].message.content
```

---

## 6. 简历要点

- 数据集：构造 500+ 条 Java 代码审查训练数据
- 微调：基于 Qwen2.5-7B-Instruct，LoRA rank=16
- 效果：BLEU 提升 X%，人工评测通过率 X%
- 部署：Ollama 本地化部署，单卡 RTX 3060 推理
