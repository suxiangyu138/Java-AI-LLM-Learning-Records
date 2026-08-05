# 00 - Hermes 知识体系总览

> 🎯 Hermes 是 Nous Research 推出的开源大语言模型系列——涵盖 Hermes 1/2/3 Pro、DeepHermes 3 推理模型，以及 Hermes-Agent 框架，以"无审查、高可控、强函数调用"著称，是开源社区插件式 AI 开发的首选基座之一

> 🎯 本系列共 **12 篇高质量技术文档**，从模型演进、提示词工程、Function Calling、推理模式、Agent 框架、记忆系统到部署实战与面试冲刺，覆盖 Hermes 生态全部核心知识

---

## 1. 知识全景

```
Hermes 生态体系（12个文件）
│
├── 🏗️ 入门篇（01-02）
│   ├── 01-Hermes模型系列全景与演进.md     # 发展历程/版本对比/架构演进
│   └── 02-ChatML提示词格式与对话模板.md    # ChatML标准/多轮对话/角色扮演
│
├── 🔧 核心技术篇（03-05）
│   ├── 03-FunctionCalling工具调用机制.md   # XML工具定义/<tool_call>解析/递归执行
│   ├── 04-结构化输出与JSONMode.md          # Pydantic Schema/<schema>标签/JSON约束
│   └── 05-DeepHermes3推理模式详解.md       # <think>标签/混合推理/CoT机制
│
├── 🚀 Agent框架篇（06-07）
│   ├── 06-Hermes-Agent框架架构与插件系统.md  # 闭环自学习/插件发现/生命周期Hook
│   └── 07-记忆系统与上下文管理.md           # 三层记忆/ContextFencing/安全扫描
│
├── 📋 工程实战篇（08-10）
│   ├── 08-本地部署与量化实践.md             # Ollama/vLLM/GGUF/量化策略/硬件选型
│   ├── 09-与主流模型全面对比.md              # vs GPT-4/Claude/Qwen/DeepSeek/Llama
│   └── 10-Java后端集成与AI开发实战.md        # Spring AI/LangChain4j/Agent开发
│
└── 📌 冲刺篇（11）
    ├── 11-面试高频考点与内化总结.md          # 面试题库/核心概念/技术选型答辩
    └── 00-Hermes知识体系总览.md              # ← 本文件
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | 模型系列全景与演进 | Hermes 1→2→3→DeepHermes 3 | ⭐⭐⭐⭐ |
| 02 | ChatML提示词格式详解 | 对话模板/多轮/角色扮演 | ⭐⭐⭐⭐ |
| 03 | Function Calling机制 | 工具定义/XML解析/递归循环 | ⭐⭐⭐⭐⭐ |
| 04 | 结构化输出与JSON Mode | Pydantic约束/schema模板 | ⭐⭐⭐⭐ |
| 05 | DeepHermes 3推理模式 | <think>/CoT/混合推理 | ⭐⭐⭐⭐⭐ |
| 06 | Hermes-Agent框架架构 | 插件系统/闭环学习/生命周期 | ⭐⭐⭐⭐ |
| 07 | 记忆系统与上下文管理 | 三层架构/ContextFencing/RAG | ⭐⭐⭐⭐ |
| 08 | 本地部署与量化实践 | Ollama/vLLM/GGUF/硬件 | ⭐⭐⭐ |
| 09 | 与主流模型全面对比 | GPT/Claude/Qwen/DeepSeek | ⭐⭐⭐⭐ |
| 10 | Java后端集成实战 | Spring AI/LangChain4j | ⭐⭐⭐⭐ |
| 11 | 面试高频考点 | 面试题库/技术答辩 | ⭐⭐⭐⭐⭐ |

## 3. 学习路线

```text
🟢 上手（30min）：01-模型全景 → 02-ChatML格式
🔵 理解（1h）：03-Function Calling → 04-JSON Mode → 05-推理模式
🟣 进阶（1h）：06-Agent框架 → 07-记忆系统
🟡 实战（45min）：08-部署量化 → 09-模型对比 → 10-Java集成
🔴 冲刺（30min）：11-面试考点
```

## 4. 前置知识

| 知识点 | 重要程度 | 前置来源 |
|--------|:---:|------|
| LLM 基础概念（Token/Embedding/Transformer） | ⭐⭐⭐⭐⭐ | 03-AI大模型垂直专项 |
| Python 基础（vLLM/Transformers） | ⭐⭐⭐ | 03-AI大模型垂直专项 |
| REST API / HTTP 协议 | ⭐⭐⭐ | Java 核心底座 |
| Spring Boot / Spring AI | ⭐⭐⭐ | 02-中间件与微服务工程 |
| Agent 设计模式（ReAct/Tool Use） | ⭐⭐⭐⭐ | AI大模型垂直专项 |

## 5. Hermes 生态核心术语速查

| 术语 | 含义 |
|------|------|
| **ChatML** | Hermes 系列使用的对话标记语言，`<\|im_start\|>` / `<\|im_end\|>` 分隔 |
| **Function Calling** | 模型自主选择并调用外部工具/函数的能力 |
| **`<tool_call>`** | XML 标签包裹的工具调用 JSON，Hermes 的 Function Calling 标准格式 |
| **`<think>`** | DeepHermes 3 推理模式使用的内部思考标签 |
| **DeepHermes** | 2025 年发布的混合推理模型，一键切换直觉模式/深度推理模式 |
| **Hermes-Agent** | Nous Research 的开源自学习 Agent 框架 |
| **Context Fencing** | 用 `<memory-context>` 标签隔离召回记忆，防止注入攻击 |
| **GOAP** | 目标导向行动规划（Goal Oriented Action Planning） |
