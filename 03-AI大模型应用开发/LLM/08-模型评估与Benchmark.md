# 08 - 模型评估与 Benchmark

> 🎯 "这个模型好不好"不能靠感觉。MMLU 测知识、HumanEval 测代码、GSM8K 测数学、MT-Bench 测对话——理解每个 Benchmark 测什么、怎么测，是做模型选型和评估的基础

---

## 目录

1. [Benchmark 全景](#1-benchmark-全景)
2. [核心 Benchmark 详解](#2-核心-benchmark-详解)
3. [评估方法论](#3-评估方法论)
4. [LLM 竞技场](#4-llm-竞技场)

---

## 1. Benchmark 全景

| Benchmark | 测什么 | 题型 | 指标 |
|------|------|------|------|
| **MMLU** | 知识广度 (57个学科) | 选择题 | Accuracy |
| **HumanEval** | 代码生成 | 写函数 → 跑测试 | pass@k |
| **GSM8K** | 小学数学推理 | 应用题 | Accuracy |
| **MATH** | 竞赛级数学 | 证明/计算 | Accuracy |
| **HellaSwag** | 常识推理 | 选最合理的续写 | Accuracy |
| **TruthfulQA** | 真实性（避免幻觉） | 多选题 | MC/Truth |
| **MT-Bench** | 多轮对话质量 | GPT-4 评分 | 1-10 |
| **AlpacaEval** | 指令遵循 | GPT-4 评分 vs Davinci | Win Rate |
| **IFEval** | 格式约束遵循 | 检查输出格式 | Accuracy |
| **SWE-bench** | 真实代码修bug | 给定repo + issue → 修bug | Resolved% |

## 2. 核心 Benchmark 详解

### 2.1 MMLU（知识广度）

```text
MMLU = Massive Multitask Language Understanding

57 个学科：数学/物理/历史/法律/医学/计算机...
15,908 道选择题（4 选 1）

代表模型的 MMLU 分数：
  GPT-4:       86.4%
  Claude 4:    88.7%
  Llama 3.1 405B: 88.6%
  DeepSeek V3:  88.5%
  Qwen 2.5 72B: 86.1%
  随机猜:       25.0%
```

### 2.2 HumanEval（代码能力）

```text
HumanEval = 164 道编程题 + 单元测试

每题：给函数签名 + docstring → 写实现 → 跑测试

代表模型 pass@1:
  GPT-4:       87.0%
  Claude 4:    92.0%
  DeepSeek V3:  82.6%
  Qwen 2.5 72B: 85.0%
```

### 2.3 GSM8K / MATH（数学推理）

```text
GSM8K: 小学数学应用题（8.5K 题）
  GPT-4:       92.0%
  DeepSeek R1:  89.0%
  Qwen 2.5:    91.6%

MATH: 竞赛级数学（12.5K 题）
  GPT-4:       52.9%
  DeepSeek R1:  97.3%  ← 推理模型的巨大优势
  Qwen 2.5 72B: 58.7%
```

## 3. 评估方法论

### 3.1 评估陷阱

| 陷阱 | 说明 | 对策 |
|------|------|------|
| **数据污染** | Benchmark 题目在训练数据中出现过 | 用最新/未公开的测试集 |
| **过拟合 Benchmark** | 模型只擅长考试，实际场景不行 | 用多个 Benchmark 交叉验证 |
| **评分主观** | LLM-as-Judge 可能有偏见 | 多个评判者 + 人工抽检 |
| **指标单一** | 只看一个指标做决策 | 看多维：知识+推理+代码+对话 |

### 3.2 自建评估集

```python
# 构建项目专属的评估集
eval_set = [
    {
        "question": "解释 Spring Boot 的自动配置原理",
        "expected_keywords": ["@EnableAutoConfiguration", "spring.factories", "条件装配"],
        "min_length": 200,
        "forbidden": ["不确定", "可能", "我猜"]  # 不应出现的词
    },
    # ... 20-50 条覆盖核心场景的测试
]

def evaluate(response, criteria):
    score = 0
    if all(kw in response for kw in criteria["expected_keywords"]):
        score += 1
    if len(response) >= criteria["min_length"]:
        score += 1
    if not any(fw in response for fw in criteria["forbidden"]):
        score += 1
    return score / 3
```

## 4. LLM 竞技场

```text
LMSYS Chatbot Arena = LLM 的"图灵测试竞技场"

规则：用户提问 → 两个匿名模型回答 → 用户选更好的

排行榜 (2025-2026 典型)：
  1. Claude 4 Opus         ← 🥇
  2. GPT-4.1/GPT-5.5       
  3. Gemini 2.5 Pro
  4. DeepSeek R1
  5. Llama 3.1 405B        ← 开源最高
  6. Qwen 2.5 72B

Arena 的优势：
  → 真实用户 + 真实问题 + 人类偏好
  → 无法作弊（不知道对面是谁）
  → 比固定 Benchmark 更能反映"实际好用程度"
```

## 核心要点回顾

- MMLU = 知识广度，HumanEval = 代码，GSM8K/MATH = 数学
- 评估三陷阱：数据污染、过拟合、单一指标
- 项目必须有专属评估集（20-50 条核心场景）
- LMSYS Arena = 最接近"真实好用程度"的排名
- 推理模型在 MATH 上有巨大优势（R1 97.3% vs GPT-4 52.9%）

## 参考资料

1. MMLU 论文 (Hendrycks et al., 2021)
2. HumanEval 论文 (Chen et al., 2021)
3. LMSYS Chatbot Arena — chat.lmsys.org
