# 07 - Prompt 模板与变量化

> 🎯 Prompt 是可复用的软件资产 — 用 Jinja2 模板化、变量化，从"每次手写"进化为"一次编写、处处使用"

---

## 目录

1. [为什么需要模板化](#1-为什么需要模板化)
2. [Jinja2 Prompt 模板](#2-jinja2-prompt-模板)
3. [变量插值与条件渲染](#3-变量插值与条件渲染)
4. [Few-Shot 动态选择](#4-few-shot-动态选择)
5. [模板管理与版本控制](#5-模板管理与版本控制)

---

## 1. 为什么需要模板化

```text
没有模板：
  prompt = "你是" + role + "，请" + task + "。要求：" + requirements
  → 字符串拼接脆弱、难以维护、无法复用

有模板：
  prompt = template.render(role="Java架构师", task="...", requirements="...")
  → 类型安全、可复用、可测试、可版本管理
```

```text
Prompt 模板化的收益：
  ✅ 团队共享：好的 Prompt 成为团队资产
  ✅ A-B 测试：同一变量不同值 → 对比效果
  ✅ 国际化：模板 + 语言变量 → 多语言 Prompt
  ✅ 版本管理：Git 追踪 Prompt 变更
```

---

## 2. Jinja2 Prompt 模板

### 2.1 基础语法

```python
from jinja2 import Template

# 基础模板
template = Template("""
你是一个 {{ role }}，有 {{ years }} 年经验。

请根据以下需求{{ task_type }}：
{{ task_description }}

要求：
{% for req in requirements %}
- {{ req }}
{% endfor %}

{% if examples %}
参考示例：
{% for ex in examples %}
输入：{{ ex.input }}
输出：{{ ex.output }}
{% endfor %}
{% endif %}
""")

# 渲染
prompt = template.render(
    role="Java 架构师",
    years=10,
    task_type="设计系统架构",
    task_description="设计一个支持百万并发的短链接服务",
    requirements=["高可用", "可扩展", "成本可控"],
    examples=[
        {"input": "...", "output": "..."}
    ]
)
```

### 2.2 完整的 Prompt 模板类

```python
from jinja2 import Template
from typing import Optional

class PromptTemplate:
    """可复用的 Prompt 模板"""
    
    def __init__(self, template_str: str):
        self.template = Template(template_str.strip())
    
    def render(self, **kwargs) -> str:
        return self.template.render(**kwargs)

# === 预定义模板库 ===

CODE_REVIEW_TEMPLATE = PromptTemplate("""
你是 {{ role | default('资深代码审查员') }}。

请审查以下 {{ language }} 代码，重点关注：
{% for focus in focuses %}
- {{ focus }}
{% endfor %}

{% if context %}
业务背景：{{ context }}
{% endif %}

对每个问题标注严重级别（🔴致命/🟡警告/🟢建议）并给出修改方案。

```{{ language }}
{{ code }}
```
""")

# 使用
prompt = CODE_REVIEW_TEMPLATE.render(
    language="Java",
    focuses=["并发安全", "异常处理", "资源释放"],
    code=java_code,
    context="订单支付回调处理"
)
```

---

## 3. 变量插值与条件渲染

### 3.1 条件输出

```text
{% if task == "classification" %}
输出格式：{"label": "...", "confidence": 0.XX}
{% elif task == "generation" %}
输出格式：自由文本，200 字以内
{% else %}
输出格式：Markdown
{% endif %}
```

### 3.2 循环与过滤器

```python
# 循环
{% for item in items %}
{{ loop.index }}. {{ item }}
{% endfor %}

# 过滤器
{{ text | truncate(100) }}        # 截断
{{ name | upper }}                # 大写
{{ items | join(', ') }}          # 拼接
{{ description | default('无') }} # 默认值
```

### 3.3 实战：多场景复用模板

```python
TASK_TEMPLATE = PromptTemplate("""
你是 {{ role }}。

任务：{{ task }}
{% if constraints %}
约束：
{% for c in constraints %}
- {{ c }}
{% endfor %}
{% endif %}

{% if output_format == 'json' %}
请严格用 JSON 格式输出，不要包含任何额外文字。
{% elif output_format == 'table' %}
请用 Markdown 表格输出。
{% else %}
请用自然语言回答。
{% endif %}

{% if few_shot_examples %}
示例：
{% for ex in few_shot_examples %}
[输入]：{{ ex.q }}
[输出]：{{ ex.a }}
{% endfor %}
{% endif %}

现在请处理：
{{ user_input }}
""")

# 同一个模板，不同场景
# 场景 1：代码审查
prompt = TASK_TEMPLATE.render(
    role="Java 代码审查员",
    task="审查代码质量",
    constraints=["关注并发安全", "按阿里巴巴规范"],
    output_format="json",
    user_input=code
)

# 场景 2：SQL 优化
prompt = TASK_TEMPLATE.render(
    role="MySQL DBA",
    task="优化慢查询",
    constraints=["使用索引", "避免全表扫描"],
    output_format="table",
    user_input=slow_sql
)
```

---

## 4. Few-Shot 动态选择

```python
from sentence_transformers import SentenceTransformer
import numpy as np

class DynamicFewShotPrompt:
    """动态选择 Few-Shot 示例的 Prompt 模板"""
    
    def __init__(self, template: PromptTemplate, example_pool: list):
        self.template = template
        self.example_pool = example_pool
        self.encoder = SentenceTransformer('BAAI/bge-small-zh')
        
        # 预计算所有示例的 Embedding
        self.example_vecs = self.encoder.encode(
            [ex['input'] for ex in example_pool]
        )
    
    def select_examples(self, query, k=3):
        """选择与 query 最相似的 k 个示例"""
        query_vec = self.encoder.encode(query)
        sims = np.dot(self.example_vecs, query_vec) / (
            np.linalg.norm(self.example_vecs, axis=1) * np.linalg.norm(query_vec)
        )
        top_k = np.argsort(sims)[-k:][::-1]
        return [self.example_pool[i] for i in top_k]
    
    def render(self, user_input, k=3):
        """渲染带动态 Few-Shot 的 Prompt"""
        examples = self.select_examples(user_input, k)
        return self.template.render(
            user_input=user_input,
            few_shot_examples=examples
        )
```

---

## 5. 模板管理与版本控制

```text
推荐的文件组织：

  prompts/
  ├── code_review.j2        # 代码审查模板
  ├── sql_optimize.j2       # SQL 优化模板
  ├── api_design.j2         # API 设计评审模板
  ├── classification.j2     # 文本分类模板
  └── examples/             # Few-Shot 示例库
      ├── code_review_examples.json
      └── classification_examples.json

版本管理（Git）：
  → Prompt 变更记录在 commit history 中
  → 可以回滚到之前效果好的版本
  → 关联 issue → 知道为什么改
```

```python
# 从文件加载模板
import json
from pathlib import Path

class TemplateRegistry:
    """Prompt 模板注册中心"""
    
    def __init__(self, template_dir="prompts"):
        self.templates = {}
        self.examples = {}
        
        # 加载所有模板
        for f in Path(template_dir).glob("*.j2"):
            name = f.stem
            self.templates[name] = PromptTemplate(f.read_text(encoding='utf-8'))
        
        # 加载示例库
        examples_dir = Path(template_dir) / "examples"
        for f in examples_dir.glob("*.json"):
            name = f.stem
            self.examples[name] = json.loads(f.read_text(encoding='utf-8'))
    
    def get(self, name):
        return self.templates[name]
    
    def get_examples(self, name):
        return self.examples.get(name, [])
```

---

## 核心要点回顾

- Prompt 模板化 = 从字符串拼接 → Jinja2 变量化 → 可复用资产
- Jinja2 支持条件/循环/过滤器 → 一个模板覆盖多场景
- 动态 Few-Shot：用 Embedding 选最相关示例 → 提升效果
- Git 管理 Prompt 模板 → 版本可追溯、可回滚
