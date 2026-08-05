# 00 - Java AI 技术栈总览与全景架构

> 定位：Java AI 应用开发的定位、三大框架选型、七层架构模型、技术栈全景（2026-07 验证）、模块导航

## 📚 目录

1. [Java 在 AI 时代的定位](#1-java-在-ai-时代的定位)
2. [三大框架选型](#2-三大框架选型)
3. [七层架构模型](#3-七层架构模型)
4. [核心技术栈全景](#4-核心技术栈全景)
5. [模块导航](#5-模块导航)

---

## 1. Java 在 AI 时代的定位

```
2026 共识：Python 是 AI 研究语言、Java 是 AI 工程化落地语言

Java 的核心价值：
  把大模型嵌入现有业务系统
  复用 Spring Boot/微服务/事务/缓存/监控基建
  封装企业数据与能力为 AI 可调用的服务

⚠️ 面试必答：
"Java 不做模型训练，做 AI 落地——
 LLM 接入、RAG 知识库、Agent 编排、
 MCP 协议集成，全部在 Spring 生态内完成。"
```

---

## 2. 三大框架选型

| 维度 | Spring AI | LangChain4j | Solon AI |
|------|:---:|:---:|:---:|
| 版本（2026） | 1.0.6 / 2.0-M5 | 1.13.1 | v3.10.4 |
| Java | 17+（2.0 需 21+） | 17+ | **8 ~ 26** |
| 框架依赖 | 强依赖 Spring Boot | 框架中立 | 可嵌入任意 |
| 编程模型 | Fluent API（ChatClient） | 声明式接口（@AiService） | Builder |
| 模型覆盖 | 20+ Provider | 30+ | 任意方言 |
| 向量存储 | 15+ | 30+（最广） | 14+ |
| MCP | ✅ 最完整（主导 Java SDK） | ✅ 模块支持 | ✅ |
| 企业背书 | VMware/Broadcom | Red Hat/Microsoft | 国内 |

```
选型决策：
  深度绑定 Spring Boot → Spring AI（零摩擦）
  非 Spring 技术栈 → LangChain4j
  Java 8 遗留系统 → Solon AI（唯一）

⚠️ 面试必答：
"Spring Boot 团队选 Spring AI（自动配置 +
 MCP 最完整）；框架中立选 LangChain4j
 （@AiService 声明式接口）；遗留 Java 8
 只能 Solon AI。"
```

---

## 3. 七层架构模型

```
七层架构（黄金分层）：
  ① 基础设施层：K8s + GPU 集群
  ② 模型服务层：vLLM/TGI/Ollama（模型部署）
  ③ AI 中间件层：Spring AI / LangChain4j（抽象）
  ④ 能力组件层：RAG Engine / Tool Hub / MCP Gateway
  ⑤ 业务逻辑层：传统 Service 增强
  ⑥ API 网关层：AI-aware 路由与鉴权
  ⑦ 可观测治理层：Prometheus/Grafana/ELK/LangSmith

⚠️ 面试必答：
"七层架构从底到顶——基础设施、模型服务、
 AI 中间件、能力组件、业务逻辑、网关、
 可观测。模型只是推理引擎，
 真正支撑业务的是数据/权限/事务——Java 老本行。"
```

---

## 4. 核心技术栈全景

| 领域 | 技术 | 说明 |
|------|------|------|
| LLM 接入 | Spring AI / LangChain4j | 多模型统一接口 |
| 模型 | OpenAI/Claude/DeepSeek/Qwen | 模型路由 |
| RAG | 向量库（Milvus/PgVector/Redis）+ ETL | 知识库问答 |
| 工具调用 | @Tool 注解 + MCP 协议 | 能力接入 |
| Agent | ReAct/Plan-Execute/Multi-Agent | 智能体编排 |
| 流式 | SSE/WebFlux | 打字机效果 |
| 可靠性 | Resilience4j/Sentinel | 重试熔断降级 |
| 可观测 | Micrometer/OTel | Token/延迟/成本 |
| 安全 | Prompt 注入防御/脱敏/审计 | 合规 |

---

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-JavaAI技术栈总览与全景架构.md) | 定位、选型、七层架构 | 入口 |
| 01 | [LLM基础与模型接入](01-LLM基础与模型接入.md) | ChatClient、多模型、流式、结构化输出 | 基础 |
| 02 | [Prompt工程](02-Prompt工程.md) | 提示词设计、上下文管理、输出约束 | 基础 |
| 03 | [RAG知识库](03-RAG知识库.md) | ETL、向量库、检索调优、Agentic RAG | 核心 |
| 04 | [Tool Calling与MCP](04-ToolCalling与MCP.md) | 工具设计、MCP 协议、Java 封装 | 核心 |
| 05 | [Agent架构设计](05-Agent架构设计.md) | ReAct/Plan/多 Agent、记忆、设计原则 | 进阶 |
| 06 | [Spring AI深入](06-SpringAI深入.md) | Advisors、Memory、多模型路由、结构化输出 | 框架 |
| 07 | [LangChain4j深入](07-LangChain4j深入.md) | @AiService、RAG 模块、与 Spring AI 对比 | 框架 |
| 08 | [生产级工程化](08-生产级工程化.md) | 可靠性、成本、安全、可观测 | 工程 |
| 09 | [落地案例与面试题](09-落地案例与面试题.md) | 场景案例、避坑清单、面试十问 | 实战 |

---

> 🎯 **本体系学习建议**：Java AI 技术栈主线——**模型接入**（01）→ **Prompt**（02）→ **RAG**（03，最高频场景）→ **工具/MCP**（04）→ **Agent**（05，进阶）→ **框架深入**（06-07）→ **工程化**（08）。"Python 验证、Java 落地"的思维贯穿全程。

---

**下一篇**：[01-LLM基础与模型接入](01-LLM基础与模型接入.md)
