# 00 - Token 总览

> 定位：LLM 世界的基本货币——Token 是模型的输入输出单位、API 的计费单位、上下文窗口的度量单位——"会算 token = 会算成本、会管窗口、会排错；2026 年 token 计量用 tiktoken 0.13.0，定价横跨 600 倍"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
Token（本体系 11 篇——大模型基础与 Prompt 工程）
├── 认知层：01 Token 是什么（基本单位/三层演进/2026 基线）
│          02 分词算法（BPE/WordPiece/Unigram/byte-level）
│          03 tokenizer 构成与训练（词表/特殊 token/训练流程）
├── 实操层：04 编码解码实战（tiktoken/HF tokenizers API）
│          05 token 与语言的关系（中英文/多语言公平性/度量）
├── 成本层：06 token 计数与成本（2026 定价/估算/预算治理）
│          07 token 与上下文窗口（窗口管理/长文本/缓存）
├── 工程层：08 tokenizer 选型与生态（各家对比/版本纪律）
│          09 token 与性能优化（吞吐/KV cache/投机采样）
└── 验收层：10 生产实战与自测（成本估算器 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | Token 是什么 | 基本单位/演进/2026 基线 | 认知 |
| 02 | 分词算法 | BPE/Unigram/byte-level 原理 | 懂原理 |
| 03 | tokenizer 构成 | 词表/特殊 token/训练 | 懂结构 |
| 04 | 编码解码实战 | tiktoken/HF API/性能 | 会编码 |
| 05 | token 与语言 | 中英文差异/多语言公平性 | 懂差异 |
| 06 | 计数与成本 | 2026 定价/估算/预算 | 会算账 |
| 07 | 上下文窗口 | 窗口管理/缓存/长文本 | 会管理 |
| 08 | 选型与生态 | 各家 tokenizer/版本纪律 | 会选型 |
| 09 | 性能优化 | 吞吐/KV cache/投机采样 | 会提速 |
| 10 | 实战与自测 | 成本估算器 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 LLM 主体系（`../LLM/00-LLM大语言模型知识体系总览.md`）的分工**：LLM 体系 02 篇"Tokenization 分词技术"讲分词在模型训练里的**概览地位**，**本体系专讲 token 的工程全链路**——算法细节、编码 API、计数成本、窗口管理、性能优化——**"概览看 LLM 体系，工程实操看本体系"**。

**与 HuggingFace 主体系（`../HuggingFace/00-HuggingFace知识体系总览.md`）的分工**：HF 体系 03 篇讲 AutoTokenizer 在模型加载链里的用法，**本体系讲 tokenizer 本身**——"框架篇讲怎么调用，本体系讲它是什么、怎么算账、怎么选"。

**与 LLM-API（`../LLM-API/00-LLM API知识体系总览.md`）、LiteLLM（`../LiteLLM 多模型适配/00-LiteLLM知识体系总览.md`）的分工**：API 体系讲调用协议，LiteLLM 讲多供应商记账——**本体系补上"token 怎么数、成本怎么估"这一层**：API 返回 usage、LiteLLM 记账、本体系给估算与预算方法。

**与 Embedding（`../Embedding/00-Embedding知识体系总览.md`）、Function Calling（`../Function Calling 函数调用【Agent 基石】/00-FunctionCalling知识体系总览.md`）的分工**：Embedding 是"token 的向量化下游"，Function Calling 的每轮工具调用都按 token 计费——**"token 是贯穿全链路的最小单位，本体系是它的度量学"**；Prompt 工程（`../Prompt工程/00-Prompt工程知识体系总览.md`）的窗口管理与本体系 07 篇互为正反面。

**2026-08 基线**：**tiktoken 0.13.0**（2026-07 发布，快速迭代——0.12.0 于 2026-02，**版本升级可使 token 计数漂移最高 ±12%，生产必须锁版本**）；编码体系 **cl100k_base**（GPT-3.5/GPT-4 Turbo 及更早）与 **o200k_base**（GPT-4o 家族与 GPT-5 系）；**byte-level BPE 仍是主导算法**（GPT/Llama/Gemma/Qwen/Mistral 全家）；puretiktoken（2026 新工具）零依赖纯 Python 逐字节兼容；**定价横跨 600 倍**：输入 $0.10/M（GPT-4.1 Nano）到 $30/M（GPT-5.5 Pro），输出 $0.28/M（DeepSeek V4 Flash）到 $180/M（GPT-5.5 Pro）——"**同一个应用，模型选错成本差 48 倍**"。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇跑代码——**毕业标准：独立写出"成本估算器 + token 预算中间件 + 窗口管理器"三合一工具**。

**路线二：成本优先路线（1-2 天）**——01 → 05 → 06 → 07 → 10——适合要把 AI 应用成本管起来的人——**"不会算 token 的成本控制都是拍脑袋；06 篇一张表把 2026 定价讲透"**。

**路线三：原理深潜路线**——做过 API 调用、想知道"token 到底是什么"的人：02 → 03 → 04 精读——**"会用 encode/decode 不等于懂分词；BPE 的合并规则值得一次彻底搞懂"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| Token | LLM 的最小输入输出单位（词元） | 01 |
| BPE | 字节对编码：合并最频繁相邻对（主导算法） | 02 |
| byte-level BPE | 256 字节起步，零 <UNK>（LLaMA/Qwen/GPT） | 02 |
| 词表 | tokenizer 的字典（GPT-2 为 50,257） | 03 |
| 特殊 token | BOS/EOS/PAD/工具 token（结构标记） | 03 |
| tiktoken | OpenAI 分词库（0.13.0/编码体系 cl100k/o200k） | 04 |
| encoding_for_model | 按模型名取编码器（只认 OpenAI 官方名） | 04 |
| TPC/BPC | 跨 tokenizer 公平度量（每字符 token/bit） | 05 |
| usage | API 返回的计费 token 数（账单唯一依据） | 06 |
| prompt caching | 缓存命中打折（Claude 90%/DeepSeek 98%） | 06/07 |
| context window | 上下文窗口（2026 主流 128K-1M） | 07 |
| KV cache | 已处理 token 的缓存（token 即显存） | 09 |

## 6. 常见误区

**误区一：token 是"单词"**——token 是**子词**："unbelievable" 可能拆成 "un"+"believable"——**"3 个 token ≈ 1 个英文单词、中文 1 字 ≈ 1-2 token"是经验值不是定义**（01/05 篇）。

**误区二：token 计数跨模型可比**——**每个模型的 tokenizer 不同，同一文本 token 数不同**（Claude Opus 4.7 换新 tokenizer 同文本多 35% token）——**跨模型比成本必须回到"字符/字节"维度**（05/08 篇）。

**误区三：升级 tiktoken 无所谓**——**版本升级 token 计数可漂移 ±12%**（词表更新）——成本预测必须锁版本（04 篇）。

**误区四：自己数 token 当账单**——**客户端计数只是估算，账单以 API 返回的 usage 为准**（推理模型 o1/o3 显示数与计费数不同）——"估算用于预算，usage 用于结算"（06 篇）。

**误区五：tokenizer 可以随便换**——**tokenizer 跟模型绑定，混用直接乱码/错位**——"模型的 tokenizer 就是它的语言，换语言等于换人"（08 篇）。

**误区六：窗口只够就行**——**填充 1M 上下文的价格差 36 倍**（DeepSeek V4 Flash $0.14 vs GPT-5.5 $5.00）——**窗口越大越要管"往里放什么"**（07 篇）。

**误区七：token 只是成本问题**——token 也是**性能问题**：tokens/s 是吞吐单位、KV cache 按 token 吃显存——**"token 决定你付多少钱，也决定你跑多快"**（09 篇）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装 tiktoken，对同一文本跑三种算法直觉 |
| 2 | 03 + 04 | 用 tiktoken 与 AutoTokenizer 编解码对比 |
| 3 | 05 | 统计中英文/代码的 TPC，画对比 |
| 4 | 06 | 写成本估算函数（输入/输出/cache 折扣） |
| 5 | 07 | 写窗口管理器（截断 + 摘要降级） |
| 6 | 08 + 09 | 对比三家 tokenizer；测吞吐 |
| 7 | 10 自测 + 面试 | 三合一工具 + 20 题 |

## 8. 快速自测 10 题

1. token 与单词/字符/字节的关系？"3 token ≈ 1 英文单词"成立吗？
2. BPE 的核心合并规则？byte-level BPE 为什么零 <UNK>？
3. GPT-2 词表 50,257 怎么构成的？
4. tiktoken 的 cl100k_base 与 o200k_base 分别对应什么模型？
5. 为什么 tiktoken 版本升级会让计数漂移？生产怎么防？
6. 同一文本在 GPT-5 与 Claude 上 token 数一样吗？为什么？
7. 2026 年定价的 600 倍跨度怎么来的？"48 倍成本差"指什么？
8. prompt caching 的折扣机制？计费怎么算？
9. 窗口管理三策略是什么？1M 窗口全填充各模型多少钱？
10. token 与 KV cache 的关系？"token 即显存"怎么理解？

## 9. 参考来源

- [tiktoken PyPI 页面（0.13.0 版本信息/安装）](https://pypi.org/project/tiktoken/)
- [OpenAI Tokenizer 官方工具（可视化解码）](https://platform.openai.com/tokenizer)
- [HF Tokenizers 官方文档（BPE/Unigram/训练 API）](https://huggingface.co/docs/tokenizers)
- [What is Tokenization in LLMs? BPE/SentencePiece/tiktoken 2026](https://futureagi.com/blog/what-is-tokenization-llms-2026/)
- [BenchLM 2026 LLM 定价对比（$0.11-$50/M token）](https://benchlm.ai/blog/posts/llm-pricing-2026)
- [AI API Pricing Comparison 2026（各家模型定价表）](https://ofox.ai/blog/ai-api-pricing-comparison-2026)
- [puretiktoken（零依赖纯 Python 逐字节兼容替代）](https://pypi.org/project/puretiktoken/0.1.0/)

---

**下一模块**：[01-Token是什么.md](01-Token是什么.md)
