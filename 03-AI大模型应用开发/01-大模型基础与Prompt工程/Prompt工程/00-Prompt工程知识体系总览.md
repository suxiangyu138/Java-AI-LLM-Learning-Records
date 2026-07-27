# 00 - Prompt 工程知识体系总览

> 🎯 Prompt Engineering 是 LLM 时代的"编程语言" — 写好 Prompt 能让 7B 模型超越 70B 模型的裸跑效果。这是 ROI 最高的 AI 技能

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [学习路线](#3-学习路线)

---

## 1. 知识全景

```
Prompt 工程体系（11个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-Prompt核心公式与设计原则.md       # 输出质量公式/角色-任务-约束-示例四要素
│   ├── 02-Zero-Shot与Few-Shot提示.md         # 零样本/少样本/示例选择/示例排序
│   └── 03-思维链CoT与推理增强.md              # CoT/Zero-Shot-CoT/Least-to-Most/Self-Consistency
│
├── 🔧 进阶篇（04-06）
│   ├── 04-角色扮演与结构化输出.md             # 角色设定/格式约束/JSON-YAML-Markdown输出
│   ├── 05-分步追问与迭代优化.md               # 追问三层法/Prompt迭代/Chain-of-Density
│   └── 06-高级推理技术-ToT与ReAct.md          # Tree-of-Thought/ReAct/Reflexion/Plan-and-Solve
│
├── 🚀 工程篇（07-09）
│   ├── 07-Prompt模板与变量化.md               # Jinja2模板/变量插值/Few-Shot动态选择
│   ├── 08-Prompt优化与自动生成.md             # DSPy/APE自动生成/对比评估/A-B测试
│   └── 09-Prompt安全-注入攻击与防御.md         # Prompt Injection/Jailbreak/防护策略
│
├── 📋 面试篇（10）
│   └── 10-Prompt工程面试题精选.md             # 高频面试题+场景设计
│
└── 📌 00-Prompt工程知识体系总览.md             # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Prompt工程知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | Prompt核心公式与设计原则 | 输出质量 = 角色×任务×约束×示例、四要素详解 | ⭐⭐⭐⭐ |
| 02 | Zero-Shot与Few-Shot提示 | 零样本/Few-Shot/示例选择策略/示例排序/格式一致性 | ⭐⭐⭐⭐ |
| 03 | 思维链CoT与推理增强 | CoT/Zero-Shot-CoT/Least-to-Most/Self-Consistency | ⭐⭐⭐⭐⭐ |
| 04 | 角色扮演与结构化输出 | 角色设定技巧/JSON-YAML-Markdown输出/格式约束 | ⭐⭐⭐ |
| 05 | 分步追问与迭代优化 | 追问三层法/Prompt迭代优化/Chain-of-Density摘要 | ⭐⭐⭐ |
| 06 | 高级推理技术-ToT与ReAct | Tree-of-Thought/ReAct/Reflexion/Plan-and-Solve | ⭐⭐⭐⭐ |
| 07 | Prompt模板与变量化 | Jinja2模板/变量插值/Few-Shot动态选择/模板管理 | ⭐⭐⭐ |
| 08 | Prompt优化与自动生成 | DSPy/APE自动生成/对比评估/A-B测试/版本管理 | ⭐⭐⭐ |
| 09 | Prompt安全-注入攻击与防御 | Prompt Injection/Jailbreak/防护策略/输入清洗 | ⭐⭐⭐ |
| 10 | Prompt工程面试题精选 | CoT原理/Few-Shot示例选择/Prompt Injection防御 | ⭐⭐⭐ |

---

## 3. 学习路线

### 🟢 L1：写出好 Prompt（30分钟）

```
01-核心公式 → 02-Zero-Shot与Few-Shot
产出：掌握四要素公式、能写有效的 Few-Shot Prompt
```

### 🔵 L2：让模型推理（1小时）

```
03-思维链CoT → 04-角色扮演 → 05-迭代优化
产出：能用 CoT 解决复杂推理、能约束输出格式
```

### 🟣 L3：高级技巧（1小时）

```
06-高级推理 → 07-模板化 → 08-自动优化
产出：掌握 ReAct/ToT、能构建可复用的 Prompt 模板
```

### 🟡 L4：安全+面试（30分钟）

```
09-安全攻防 → 10-面试题
产出：理解注入攻击防御、覆盖高频面试题
```
