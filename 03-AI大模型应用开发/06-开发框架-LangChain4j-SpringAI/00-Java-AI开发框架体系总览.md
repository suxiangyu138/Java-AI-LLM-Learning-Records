# 00 - Java AI 开发框架体系总览

> 🎯 Spring AI 是 Spring 官方的 AI 集成方案、LangChain4j 是 LangChain 的 Java 移植 — 两个框架覆盖了 Java 后端 AI 开发的 90% 场景

## 1. 知识全景

```
Java AI 开发框架体系（12个文件）
│
├── 🌱 Spring AI 篇（01-05）
│   ├── 01-Spring-AI核心原理.md         # 架构/ChatClient/Embedding/自动配置
│   ├── 02-Spring-AI-Chat与对话管理.md    # 对话/流式/多轮/System Prompt
│   ├── 03-Spring-AI-RAG实战.md          # DocumentReader/Splitter/VectorStore/检索
│   ├── 04-Spring-AI-Function-Calling.md  # Tool定义/调用/参数解析
│   └── 05-Spring-AI-多模态与生产.md      # 图片/语音/监控/Actuator
│
├── 🔗 LangChain4j 篇（06-08）
│   ├── 06-LangChain4j核心原理.md        # 架构/模型集成/ChatMemory/Tool
│   ├── 07-LangChain4j-RAG实战.md        # DocumentLoader/Splitter/Store/Retriever
│   └── 08-LangChain4j-Agent与高级特性.md  # Agent/Tool/Chain/流式
│
├── 📊 对比与生态篇（09-10）
│   ├── 09-SpringAI-vs-LangChain4j对比.md   # 架构/生态/社区/选型决策
│   └── 10-其他框架速览.md                  # LlamaIndex/Genkit/SemanticKernel
│
├── 📋 面试篇（11）
│   └── 11-Java-AI框架面试题与选型.md       # 20题+选型矩阵
│
└── 📌 00-Java-AI开发框架体系总览.md          # ← 本文件
```

## 2. 文件导航

| # | 文件 | 核心 | 级别 |
|---|------|------|:---:|
| 00 | 体系总览 | 全景+选型速查 | — |
| 01 | Spring AI核心原理 | ChatClient/自动配置/Embedding/多模型 | ⭐⭐⭐⭐ |
| 02 | Spring AI Chat与对话 | Prompt模板/流式/多轮/System Prompt | ⭐⭐⭐ |
| 03 | Spring AI RAG实战 | Document/Splitter/VectorStore/Advisor | ⭐⭐⭐⭐ |
| 04 | Spring AI Function Calling | @Tool注解/参数解析/多工具 | ⭐⭐⭐⭐ |
| 05 | Spring AI 多模态与生产 | 图片/语音/Monitoring/Actuator | ⭐⭐ |
| 06 | LangChain4j核心原理 | AiService/模型集成/ChatMemory/Tool | ⭐⭐⭐⭐ |
| 07 | LangChain4j RAG实战 | DocumentLoader/EmbeddingStore/Retriever | ⭐⭐⭐⭐ |
| 08 | LangChain4j Agent与高级 | Agent/Chain/流式/自定义Tool | ⭐⭐⭐ |
| 09 | SpringAI vs LangChain4j | 10维度对比/选型决策树 | ⭐⭐⭐⭐ |
| 10 | 其他框架速览 | LlamaIndex/Genkit/SemanticKernel | ⭐⭐ |
| 11 | 面试题与选型 | 20题+选型矩阵 | ⭐⭐⭐ |

## 3. 框架选型速查

```text
Spring Boot 项目 → Spring AI（原生集成、自动配置）
需要复杂 Agent 编排 → LangChain4j（更接近 Python LangChain）
快速原型 + 云原生 → Genkit (Google)
.NET 生态 → Semantic Kernel (Microsoft)
Python 项目 → LlamaIndex / LangChain（不在此目录）
```
