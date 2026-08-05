# 08 - Prompt 优化与自动生成

> 🎯 手工调 Prompt 靠经验，自动优化靠系统 — DSPy 用程序化方式优化 Prompt，APE 让 LLM 自己写 Prompt

---

## 目录

1. [Prompt 效果评估](#1-prompt-效果评估)
2. [A-B 测试方法](#2-a-b-测试方法)
3. [DSPy：程序化 Prompt 优化](#3-dspy程序化-prompt-优化)
4. [APE：自动 Prompt 生成](#4-ape自动-prompt-生成)
5. [人工优化 vs 自动优化](#5-人工优化-vs-自动优化)

---

## 1. Prompt 效果评估

### 1.1 评估维度

```text
Prompt 效果评估矩阵：

  ① 准确性：输出内容是否正确
    → 分类任务：F1 Score
    → 生成任务：人工评分 / LLM-as-Judge

  ② 一致性：相同输入多次运行结果是否一致
    → 温度=0 时应完全一致

  ③ 格式合规率：输出是否符合要求的格式
    → JSON 可解析率 / Markdown 正确率

  ④ Token 效率：完成任务消耗的 Token 数
    → 输入 Token + 输出 Token

  ⑤ 鲁棒性：对输入微小变化是否稳定
    → 加空格/换行/错别字后结果是否变化
```

### 1.2 评估脚本

```python
from sklearn.metrics import f1_score

def evaluate_prompt(prompt_template, test_cases, llm):
    """评估 Prompt 在测试集上的表现"""
    predictions = []
    ground_truth = []
    format_errors = 0
    
    for case in test_cases:
        prompt = prompt_template.format(**case)
        output = llm(prompt)
        
        # 解析输出
        parsed = parse_output(output)  # 你的解析逻辑
        if parsed is None:
            format_errors += 1
            continue
        
        predictions.append(parsed['label'])
        ground_truth.append(case['label'])
    
    metrics = {
        'accuracy': sum(p == g for p, g in zip(predictions, ground_truth)) / len(predictions),
        'f1': f1_score(ground_truth, predictions, average='macro'),
        'format_error_rate': format_errors / len(test_cases),
        'coverage': len(predictions) / len(test_cases)  # 成功解析的比例
    }
    return metrics
```

---

## 2. A-B 测试方法

```python
import random

def ab_test_prompts(prompt_a, prompt_b, test_cases, llm, n=50):
    """对比两个 Prompt 的效果"""
    # 随机抽样
    samples = random.sample(test_cases, min(n, len(test_cases)))
    
    results_a = []
    results_b = []
    
    for case in samples:
        # 随机顺序避免位置偏差
        if random.random() > 0.5:
            output_a = llm(prompt_a.format(**case))
            output_b = llm(prompt_b.format(**case))
        else:
            output_b = llm(prompt_b.format(**case))
            output_a = llm(prompt_a.format(**case))
        
        results_a.append(score_output(output_a, case['expected']))
        results_b.append(score_output(output_b, case['expected']))
    
    # 统计检验
    from scipy import stats
    t_stat, p_value = stats.ttest_rel(results_a, results_b)
    
    return {
        'prompt_a_mean': sum(results_a) / len(results_a),
        'prompt_b_mean': sum(results_b) / len(results_b),
        'p_value': p_value,
        'significant': p_value < 0.05
    }
```

---

## 3. DSPy：程序化 Prompt 优化

### 3.1 核心理念

```text
DSPy (Stanford) = 用程序而非手工的方式构建和优化 Prompt

传统 Prompt Engineering：
  手工写 Prompt → 手工调 → 凭经验 → 不可复现

DSPy：
  定义任务签名 → 选优化器 → 自动搜索最优 Prompt
  → 可复现、可优化、可迁移
```

### 3.2 DSPy 示例

```python
import dspy

# ① 定义 LM
lm = dspy.OpenAI(model="gpt-4o-mini")
dspy.configure(lm=lm)

# ② 定义任务签名（输入→输出）
class SentimentAnalysis(dspy.Signature):
    """分析文本情感"""
    text = dspy.InputField()
    sentiment = dspy.OutputField(desc="positive, negative, or neutral")

# ③ 定义模块
class SentimentModule(dspy.Module):
    def __init__(self):
        super().__init__()
        self.classify = dspy.ChainOfThought(SentimentAnalysis)
    
    def forward(self, text):
        return self.classify(text=text)

# ④ 准备训练数据
trainset = [
    dspy.Example(text="I love this!", sentiment="positive").with_inputs("text"),
    dspy.Example(text="This is terrible", sentiment="negative").with_inputs("text"),
    # ...
]

# ⑤ 自动优化 Prompt
from dspy.teleprompt import BootstrapFewShot
optimizer = BootstrapFewShot(metric=accuracy_metric)
optimized_module = optimizer.compile(SentimentModule(), trainset=trainset)
```

---

## 4. APE：自动 Prompt 生成

### 4.1 原理

```text
APE (Automatic Prompt Engineer) = 让 LLM 自己生成和优化 Prompt

流程：
  ① 给 LLM 一批输入-输出示例
  ② 让 LLM 生成候选 Prompt
  ③ 用验证集评估每个 Prompt
  ④ 选出效果最好的 Prompt
  ⑤ 可选：让 LLM 对最好的 Prompt 做变体优化
```

### 4.2 APE 实现示例

```python
def ape_generate_prompts(examples, llm, n_candidates=10):
    """让 LLM 生成候选 Prompt"""
    
    examples_text = "\n".join([
        f"输入：{ex['input']}\n期望输出：{ex['output']}"
        for ex in examples[:5]  # 只用 5 个示例
    ])
    
    meta_prompt = f"""以下是几个输入-输出示例：

{examples_text}

请生成 {n_candidates} 个不同的 Prompt 模板，使得按照 Prompt 执行能得到类似的输出。
每个 Prompt 应该清晰、可复用，包含必要的约束和格式要求。

输出格式：
Prompt 1: [Prompt内容]
Prompt 2: [Prompt内容]
..."""

    response = llm(meta_prompt)
    prompts = parse_prompts(response)  # 解析出 N 个 Prompt
    return prompts

# 评估候选 Prompt
def evaluate_prompts(candidates, val_set, llm):
    scores = []
    for prompt in candidates:
        score = evaluate_prompt(prompt, val_set, llm)
        scores.append((prompt, score))
    return sorted(scores, key=lambda x: x[1], reverse=True)
```

---

## 5. 人工优化 vs 自动优化

| 维度 | 人工优化 | DSPy | APE |
|------|:---:|:---:|:---:|
| **上手难度** | 低 | 中 | 低 |
| **效果天花板** | 高（专家） | 高 | 中 |
| **可复现性** | 低 | 高 | 中 |
| **Token 成本** | 低 | 中 | 高（搜索过程） |
| **适用场景** | 少量 Prompt | 批量/生产 | 快速原型 |
| **需要标注数据** | 可选 | ✅ 需要 | ✅ 需要 |

```text
推荐策略：
  → 先用 APE 快速生成候选 Prompt
  → 人工挑选和微调最好的 2-3 个
  → 对关键 Prompt 用 DSPy 做系统性优化
  → 持续 A-B 测试 + 迭代
```

---

## 核心要点回顾

- Prompt 评估五维度：准确性、一致性、格式合规率、Token 效率、鲁棒性
- A-B 测试 + 统计检验 → 科学判断 Prompt 改进是否有效
- DSPy = 程序化 Prompt 优化 → 可复现、可迁移
- APE = 让 LLM 自己生成 Prompt → 快速获得候选方案
- 推荐：APE 生成 → 人工筛选 → DSPy 优化 → A-B 验证
