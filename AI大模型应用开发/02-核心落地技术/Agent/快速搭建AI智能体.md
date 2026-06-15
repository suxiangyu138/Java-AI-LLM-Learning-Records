# 快速搭建 AI 智能体

> **核心摘要**：AI 智能体是能够感知环境、推理规划、调用工具、执行动作并反馈迭代的自主 AI 系统。本文从三种技术路径（低代码平台 Dify、代码级开发 LangGraph、Java 原生 Spring AI Alibaba）出发，涵盖记忆系统设计、工具调用、多智能体系统和生产部署的全流程指南。

## 前置阅读

- [[AI Agent核心知识点]]
- [[AI-Agent-快速吃透]]
- [[LangGraph实战：状态图与多Agent工作流]]

---

## 一、核心架构认知

Agent 的四大能力模块：

| 能力模块 | 描述 | 技术实现 |
|---|---|---|
| **感知（Perception）** | 理解文本、语音、图像、API 输入 | Prompt + 多模态模型 |
| **推理（Reasoning）** | LLM 对信息进行逻辑分析与决策 | ReAct / CoT / ToT |
| **行动（Action）** | 调用外部工具、API 或系统执行任务 | Function Calling / Tool Use |
| **记忆（Memory）** | 存储与检索历史交互，保持上下文连续 | 向量数据库 + 滑动窗口 |

> **重点**：LangGraph 将这些流程抽象为有向图（Graph），每个节点代表一个处理步骤，相比链式的 LangChain，更适合构建有条件分支、循环和并行执行的复杂 Agent 工作流。

---

## 二、三条技术路径

### 路径一：低代码平台快速验证（新手推荐）

推荐工具链：**Dify + Ollama**

```
Ollama（本地模型服务） → Dify（可视化编排） → 知识库 + 工作流 → 对外 API
```

操作步骤：

1. 安装 Ollama，拉取模型：`ollama pull qwen2.5:7b`
2. Docker 部署 Dify：`docker compose up -d`，访问 `localhost:80`
3. Dify 接入 Ollama：基础 URL 填 `http://host.docker.internal:11434`
4. 新建知识库 → 上传 PDF/Word → 自动切片 + 向量化
5. 创建智能体 → 配置 System Prompt + 知识库 + 工具 → 发布 API

### 路径二：LangGraph 代码级开发（核心能力）

```bash
pip install langgraph langchain langchain-community chromadb
```

```python
from langgraph.graph import StateGraph, END
from typing import TypedDict, List

class AgentState(TypedDict):
    messages: List
    tools_result: str

def reasoning_node(state: AgentState):
    # LLM 推理，决定是否调用工具
    pass

def tool_node(state: AgentState):
    # 执行工具调用
    pass

def should_continue(state: AgentState):
    return "tool" if state["tools_result"] else END

graph = StateGraph(AgentState)
graph.add_node("reason", reasoning_node)
graph.add_node("tool", tool_node)
graph.add_edge("tool", "reason")
graph.add_conditional_edges("reason", should_continue)
graph.set_entry_point("reason")
agent = graph.compile()
```

> **注意**：LangGraph 支持状态持久化（Checkpoints）、并行分支和 Human-in-the-loop 等生产级特性。

### 路径三：Spring AI Alibaba（Java 原生）

Spring AI Alibaba 1.0 GA 为 Java 开发者提供了生产可用的企业级 Agent 框架。

**核心能力：**
- 内置 ReAct Agent、Supervisor 多智能体模式
- 基于 Spring AI Alibaba Graph 编排工作流，类似 Python 版 LangGraph
- 原生支持 Streaming、Human-in-the-loop、记忆与持久存储

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 三、记忆系统设计

| 场景 | 推荐方案 | 实现 |
|---|---|---|
| 单次脚本 / Demo | 全量记忆 | 全部对话塞入 context |
| 客服 / 问答 Agent | 滑动窗口（10 轮） | LangChain ConversationBufferWindowMemory |
| 个人助理（跨天） | 分层记忆：短期窗口 + 长期向量库 | Redis + ChromaDB / Milvus |
| 法律 / 研究 Agent | 向量库 + 相关性过滤（RAG） | Chroma + nomic-embed |

> **重点**：RAG 的本质是给 LLM 接上"外部记忆"——将文档向量化、通过相似度检索、将结果注入 Prompt，解决知识截止问题。

---

## 四、工具调用（Function Calling）

```
用户意图 → LLM 识别需调用哪个工具
→ 提取工具参数（JSON）
→ Agent 执行函数调用（HTTP/DB/代码）
→ 返回结果给 LLM
→ LLM 整合生成最终回答
```

**常见工具类型：**

| 类型 | 示例 |
|---|---|
| 数据库查询 | MySQL SELECT |
| 网络搜索 | Tavily / SerpAPI |
| 代码执行器 | Python Sandbox |
| 企业系统 API | CRM / 工单 / 邮件 |

---

## 五、多智能体系统（Multi-Agent）

四种主流架构模式：

| 模式 | 特点 | 适用场景 |
|---|---|---|
| **Agents as Tools** | 子 Agent 作为主 Agent 的工具调用 | 功能模块化清晰 |
| **Supervisor（监督者）** | 主 Agent 分配任务给子 Agent | 企业流程自动化 |
| **Swarm（蜂群）** | Agent 间平等通信、自主协作 | 分布式研究任务 |
| **Graph（图结构）** | 节点式编排，条件分支灵活 | LangGraph / Spring AI Graph |

> **重点**：多 Agent 系统的核心优势是关注点分离、可追溯调试和并行提速。

---

## 六、测试与生产部署

### 分层防御策略

- **部署前**：自动化评测（单元测试 + LLM rubric 评分器）+ 人工抽查对话记录
- **部署后**：生产监控（成功率、延迟、Token 消耗）+ 用户反馈转为新测试用例

### 评估核心指标

| 指标 | 说明 |
|---|---|
| 任务完成率 | 是否完成了用户任务 |
| 工具调用准确率 | 是否选择了正确的工具 |
| 响应延迟 P95 | 95% 的请求在多少时间内完成 |
| 安全性 / 幻觉率 | 是否执行了危险操作或编造信息 |

---

## 七、推荐技术栈总览

```
模型层:   Ollama (本地) + DeepSeek/Qwen API (云端)
编排层:   LangGraph (Python) / Spring AI Alibaba Graph (Java)
记忆层:   Redis (短期) + ChromaDB / Milvus (长期向量)
知识库:   Dify 知识库 / LangChain RAG Pipeline
工具层:   Function Calling + MySQL + HTTP Tool
前端:     Vue3 / React (对话界面)
监控:     LangSmith (LangGraph trace) / OpenTelemetry
```

### 项目落地建议

1. **Dify + Ollama + Qwen**：搭建私有知识问答助手（1 天出 Demo）
2. **LangGraph + RAG + Tool**：构建带自我纠错能力的客服 Agent
3. **Spring AI Alibaba + Multi-Agent**：实现企业级多 Agent 工作流系统

---

## 核心要点回顾

- Agent 四大核心模块：感知、推理、行动、记忆
- 三条技术路径：Dify（低代码）、LangGraph（代码级）、Spring AI Alibaba（Java 原生）
- 记忆系统需按场景选择：滑动窗口、分层记忆或 RAG
- 多 Agent 系统有 Agents as Tools、Supervisor、Swarm 和 Graph 四种模式
- 生产部署需建立分层防御策略和核心评估指标体系

---

## 参考资料

1. Dify 官方文档. 智能体搭建与部署指南
2. LangGraph 官方文档. StateGraph 与多 Agent 协作
3. Spring AI Alibaba 官方文档. 1.0 GA 版本发布说明
4. LangSmith 官方文档. Agent 链路追踪与评估
5. OpenTelemetry 官方文档. 可观测性标准
