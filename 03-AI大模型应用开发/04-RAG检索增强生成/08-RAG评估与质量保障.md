# 08 - RAG 评估与质量保障

> 🎯 没有评估就没有优化方向 — RAGAS 是 RAG 评估的事实标准，衡量忠实度、答案相关性、检索精度三个核心维度

---

## 目录

1. [RAG 评估维度](#1-rag-评估维度)
2. [RAGAS 框架](#2-ragas-框架)
3. [评估数据集构建](#3-评估数据集构建)
4. [评估 Pipeline](#4-评估-pipeline)
5. [持续质量监控](#5-持续质量监控)

---

## 1. RAG 评估维度

```text
RAG 质量 = 检索质量 × 生成质量

检索质量（找到对的内容了吗？）：
  ① Context Precision：检索结果中相关文档的比例
  ② Context Recall：正确答案所需的信息是否被检索到
  ③ Context Relevancy：检索到的内容与问题的相关度

生成质量（回答得好吗？）：
  ④ Faithfulness（忠实度）：回答是否完全基于检索到的内容（不编造）
  ⑤ Answer Relevancy（答案相关性）：回答是否切题
  ⑥ Answer Correctness（答案正确性）：回答的事实是否正确
```

---

## 2. RAGAS 框架

### 2.1 快速使用

```python
from ragas import evaluate
from ragas.metrics import (
    faithfulness, answer_relevancy, 
    context_precision, context_recall
)
from datasets import Dataset

# 准备评估数据
eval_dataset = Dataset.from_dict({
    "question": ["公司年假怎么算？", "离职流程是什么？"],
    "answer": ["根据员工手册，年假...", "离职需提前30天..."],
    "contexts": [["员工手册第3章：年假..."], ["离职管理制度v2.0..."]],
    "ground_truth": ["年假=工龄×5天...", "提前30天提交申请..."]
})

# 评估
result = evaluate(
    eval_dataset,
    metrics=[faithfulness, answer_relevancy, 
             context_precision, context_recall]
)
print(result)
# {'faithfulness': 0.85, 'answer_relevancy': 0.92, ...}
```

### 2.2 核心指标解读

| 指标 | 分数含义 | 目标 |
|------|----------|:---:|
| **Faithfulness** | 回答中可追溯到上下文的陈述比例 | >0.85 |
| **Answer Relevancy** | 回答与问题的相关程度 | >0.80 |
| **Context Precision** | 检索到的上下文中相关文档的排名 | >0.75 |
| **Context Recall** | 答案所需信息是否在检索结果中 | >0.80 |

---

## 3. 评估数据集构建

```text
评估数据集 = 问题 + 标准答案 + 预期检索文档

构建方式：

  ① 人工标注（最可靠）
    → 领域专家编写 50-100 个 QA 对
    → 标注每个问题应该检索到哪些文档

  ② LLM 生成 + 人工校验（平衡方案）
    → 从文档中随机选段落 → LLM 生成问题
    → 人工审核 → 修正

  ③ 用户反馈（持续积累）
    → 记录真实用户的 👍/👎 反馈
    → 点赞多的作为正例，点踩多的作为负例
```

---

## 4. 评估 Pipeline

```python
class RAGEvaluator:
    """RAG 系统评估器"""
    
    def __init__(self, rag_system, eval_dataset):
        self.rag = rag_system
        self.dataset = eval_dataset
    
    def evaluate(self):
        questions = []
        answers = []
        contexts = []
        ground_truths = []
        
        for item in self.dataset:
            result = self.rag.query(item["question"])
            questions.append(item["question"])
            answers.append(result["answer"])
            contexts.append(result["contexts"])
            ground_truths.append(item["ground_truth"])
        
        from ragas import evaluate
        from ragas.metrics import faithfulness, context_precision
        
        return evaluate(
            Dataset.from_dict({
                "question": questions,
                "answer": answers,
                "contexts": contexts,
                "ground_truth": ground_truths,
            }),
            metrics=[faithfulness, context_precision]
        )
```

---

## 5. 持续质量监控

```text
RAG 生产质量监控：

  ① 检索质量监控
    → 平均检索分数趋势
    → 低分查询比例（<0.5 的占比）
    → 零结果查询比例

  ② 生成质量监控
    → 用户点赞率
    → 回答长度分布（过长/过短异常）
    → "不知道/无法回答"的比例

  ③ 系统健康监控
    → P50/P95/P99 延迟
    → 错误率（Embedding 失败/LLM 超时）

  ④ 定期回归测试
    → 每月跑一次 RAGAS 评估
    → 与上月对比 → 发现退化
```

---

## 核心要点回顾

- RAGAS 是 RAG 评估的事实标准
- 核心四指标：Faithfulness + Answer Relevancy + Context Precision + Recall
- 评估数据集：50-100 个人工标注 QA 对 → 持续积累用户反馈
- 生产监控：检索分数 + 点赞率 + 延迟 + 定期回归测试
