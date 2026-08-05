# 02 - Zero-Shot 与 Few-Shot 提示

> 🎯 Few-Shot 是 Prompt Engineering 中最强大的技巧之一 — 不需要微调模型，只靠几个示例就能显著提升效果。示例的选择和排序是关键

---

## 目录

1. [Zero-Shot 零样本提示](#1-zero-shot-零样本提示)
2. [Few-Shot 少样本提示](#2-few-shot-少样本提示)
3. [示例选择策略](#3-示例选择策略)
4. [示例排序技巧](#4-示例排序技巧)
5. [何时用 Zero-Shot vs Few-Shot](#5-何时用-zero-shot-vs-few-shot)

---

## 1. Zero-Shot 零样本提示

### 1.1 定义

```text
Zero-Shot = 不给示例，只用指令描述任务

  "将以下文本分类为正面、负面或中性：
   文本：这个产品质量很好，物流也快
   分类："

优势：简单快速，无需准备示例
局限：对复杂/模糊任务效果差
```

### 1.2 Zero-Shot 强化技巧

```text
普通 Zero-Shot：
  "分析这段话的情感"
  → 模型可能回答"积极"、"正面的"、"还不错" → 格式不统一

强化 Zero-Shot：
  "分析这段话的情感。你的回答必须且只能是以下三个词之一：
   正面 | 负面 | 中性
   文本：这个产品质量很好
   回答："
  → 模型输出："正面" ✅ 格式统一

强化手段：
  ✅ 限定输出空间（"必须是以下之一：A/B/C"）
  ✅ 要求固定格式（"用 JSON 返回"）
  ✅ 加入否定约束（"不要解释，只给答案"）
  ✅ 用角色增强（"你是一个专业的情感分析系统"）
```

---

## 2. Few-Shot 少样本提示

### 2.1 定义

```text
Few-Shot = 先给几个示例（输入→输出对），再提出真正的问题

  "将文本翻译为英文：
   中文：你好 → 英文：Hello
   中文：谢谢 → 英文：Thank you
   中文：再见 → 英文：Goodbye
   中文：今天天气真好 → 英文："

模型从示例中学习：
  ✅ 任务的模式（输入→输出映射）
  ✅ 输出的格式和风格
  ✅ 边界情况的处理方式
```

### 2.2 Few-Shot 示例数量

```text
Few-Shot 示例数量建议：

  1-shot： 一个示例 → 展示基本格式
  3-shot： 三个示例 → 展示模式（最常用）
  5-shot： 五个示例 → 展示更多变化
  >8-shot：通常不再显著提升

经验法则：
  → 简单分类：1-3 个示例足够
  → 复杂格式：3-5 个示例
  → 微妙风格：5-8 个示例
```

### 2.3 Few-Shot 实战

```python
def few_shot_classify(text, examples, labels):
    """Few-Shot 分类"""
    prompt_parts = [f"将文本分类为 {', '.join(labels)} 之一：\n"]
    
    # 添加示例
    for ex_text, ex_label in examples:
        prompt_parts.append(f"文本：{ex_text}")
        prompt_parts.append(f"分类：{ex_label}\n")
    
    # 待分类文本
    prompt_parts.append(f"文本：{text}")
    prompt_parts.append("分类：")
    
    prompt = "\n".join(prompt_parts)
    return llm(prompt)

# 使用
examples = [
    ("物流太慢了，等了5天", "投诉"),
    ("东西不错，但包装有点破损", "中评"),
    ("客服态度很好，问题解决了", "好评"),
]
result = few_shot_classify("产品质量可以，但是颜色和图片不一样", 
                           examples, ["好评", "中评", "投诉"])
```

---

## 3. 示例选择策略

### 3.1 选择原则

```text
好的 Few-Shot 示例应该：

① 覆盖标签空间：每个类别至少一个示例
   → 3 分类 → 最少 3 个示例 → 每个类别一个

② 覆盖边界情况：
   → 包含一个"正常"case + 一个"难"case
   → 模型能看到你期望如何处理边界

③ 格式高度一致：
   → 所有示例的格式必须完全一样
   → 不一致的示例比没有示例更糟

④ 与目标问题相关：
   → 示例和真正问题的领域/风格/难度相近
```

### 3.2 动态示例选择

```python
from sentence_transformers import SentenceTransformer
import numpy as np

# 用 Embedding 从示例库中选最相似的示例
model = SentenceTransformer('BAAI/bge-small-zh')

def select_best_examples(query, example_pool, k=3):
    """选择与 query 最相似的 k 个示例"""
    query_vec = model.encode(query)
    
    # 计算相似度
    scores = []
    for ex_text, ex_label in example_pool:
        ex_vec = model.encode(ex_text)
        sim = np.dot(query_vec, ex_vec) / (
            np.linalg.norm(query_vec) * np.linalg.norm(ex_vec)
        )
        scores.append((sim, (ex_text, ex_label)))
    
    # 取 Top-K
    scores.sort(reverse=True)
    return [ex for _, ex in scores[:k]]
```

---

## 4. 示例排序技巧

```text
示例的排列顺序影响效果：

  ① "相关示例放最后"原则：
     → 离 query 最近的示例对输出影响最大
     → 与 query 最相关的示例 → 放在最后

  ② 难度递进：
     → 简单示例 → 复杂示例 → query
     → 模型逐渐"热身"

  ③ 交替排列（分类任务）：
     → A类示例 → B类示例 → C类示例 → query
     → 避免模型偏向最后出现的类别

  示例排序对比：
    差序： 相关度低, 相关度中, 相关度高, query
          → 最新的"高相关"示例主导输出 ✅ 其实这是对的！

    好序： 相关度中, 相关度低, 相关度高, query
          → 最近的示例最相关 ✅ 最佳！
```

---

## 5. 何时用 Zero-Shot vs Few-Shot

| 场景 | Zero-Shot | Few-Shot |
|------|:---:|:---:|
| 简单分类（情感正负） | ✅ 够用 | 可选 |
| 复杂格式（嵌套 JSON） | ❌ 容易出错 | ✅ 必须 |
| 微妙风格（幽默/正式） | ❌ 很难描述 | ✅ 给示例 |
| 快速验证 | ✅ | 太重 |
| 罕见任务 | ❌ 模型不确定 | ✅ 明确模式 |
| Token 预算紧 | ✅ 省 token | ❌ 示例占用 token |

```text
推荐策略：
  → 先试 Zero-Shot（最快）
  → 效果不好 → 加 1-2 个示例
  → 仍不够 → 3-5 个精挑细选的示例
  → 不是示例越多越好！
```

---

## 核心要点回顾

- Zero-Shot：不给示例，只靠指令 → 适合简单任务
- Few-Shot：给 3-5 个示例 → 适合复杂格式/微妙风格
- 示例选择：覆盖所有标签 + 包含边界 + 与 query 相关
- 示例排序：最相关的放最后（离 query 最近）
- 不是示例越多越好：3-5 个精挑细选的 > 10 个随便的
