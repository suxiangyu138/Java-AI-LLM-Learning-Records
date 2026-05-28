AI Agent 的核心可以抽象成一句话：**“LLM 大脑 + 规划 + 记忆 + 工具 + 行动闭环”**。 [ibm](https://www.ibm.com/cn-zh/think/topics/ai-agents)

下面我按「体系化知识点」帮你梳理，适配你 Java 后端 + 大模型应用开发的技术路线。

***

## 1. 概念与整体框架

- **AI Agent 定义**：把大模型和一组工具、记忆、规划逻辑组合在一起，让它能感知环境、自主决策并执行任务的智能程序。 [developer.volcengine](https://developer.volcengine.com/articles/7534567993976897574)
- 与普通 LLM 区别：LLM 只会“聊天 + 生成文本”，Agent 能主动调用 API、读写文件、操作系统、访问业务系统，形成从理解到执行的闭环。 [ibm](https://www.ibm.com/cn-zh/think/topics/ai-agents)
- 常用抽象公式：  
  - AI Agent = LLM（大脑） + 规划(Planning) + 记忆(Memory) + 工具使用(Tools) + 行动执行(Action)。 [damodev.csdn](https://damodev.csdn.net/692d0cbe2087ae0db79de14f.html)

***

## 2. 核心能力模块

多数文章会把 Agent 拆成 4–6 个核心模块，本质是一致的：感知 → 决策 → 行动 → 学习。 [betteryeah](https://www.betteryeah.com/blog/ai-agent-core-components-architecture-guide)

1. **感知（Perception）**  
   - 输入形态：文本、语音、图片、传感器/业务数据等。 [cloud.tencent](https://cloud.tencent.com/developer/ask/2186787)
   - 技术重点：多模态模型、ASR（语音转文本）、OCR、业务系统数据接入等。 [damodev.csdn](https://damodev.csdn.net/692d0cbe2087ae0db79de14f.html)

2. **决策 / 规划（Reasoning & Planning）**  
   - LLM 负责理解用户意图、拆解任务、决定调用哪些工具、何时结束。 [juejin](https://juejin.cn/post/7626005571403137078)
   - 常见模式：  
     - 任务分解（Task Decomposition）：把复杂目标拆成子任务。 [developer.volcengine](https://developer.volcengine.com/articles/7534567993976897574)
     - ReAct：Thought → Action → Observation → Final Answer 循环。 [developer.volcengine](https://developer.volcengine.com/articles/7534567993976897574)

3. **工具调用（Tool Use / Function Calling）**  
   - 通过函数调用、HTTP API、数据库查询等让 Agent 能“改变环境”。 [ibm](https://www.ibm.com/cn-zh/think/topics/ai-agents)
   - 大模型生成结构化调用参数（比如 JSON），Agent 框架负责真正执行，对接你 Java 的服务、微服务、脚本等。 [juejin](https://juejin.cn/post/7626005571403137078)

4. **记忆系统（Memory）**  
   - 短期记忆：当前会话上下文、任务状态。 [cloud.tencent](https://cloud.tencent.com/developer/ask/2186787)
   - 长期记忆：用户画像、历史任务、知识库（向量库/RAG）。 [betteryeah](https://www.betteryeah.com/blog/ai-agent-core-components-architecture-guide)
   - 作用：跨轮对话、个性化推荐、长任务持续执行。 [betteryeah](https://www.betteryeah.com/blog/ai-agent-core-components-architecture-guide)

5. **行动执行（Action / Actuator）**  
   - 执行外部操作：调用第三方 API、调度工作流、写文件、发邮件、改业务状态等。 [betteryeah](https://www.betteryeah.com/blog/ai-agent-core-components-architecture-guide)
   - 对于你：就是在 Agent 层统一管理对 Java 微服务的调用和状态追踪。  

6. **学习与自进化（Learning）**  
   - 通过反馈数据调整策略、更新提示词、更新知识库，或进一步做微调。 [damodev.csdn](https://damodev.csdn.net/692d0cbe2087ae0db79de14f.html)
   - 企业里多是“软学习”：规则迭代、提示工程更新，而不是频繁全量微调。 [damodev.csdn](https://damodev.csdn.net/692d0cbe2087ae0db79de14f.html)

***

## 3. 大模型在 Agent 中的角色

- 大模型是 Agent 的“核心大脑”，承担**理解 + 推理 +生成**三件事： [juejin](https://juejin.cn/post/7626005571403137078)
  - 理解：解析用户意图、解析工具返回结果、识别是否完成任务。 [juejin](https://juejin.cn/post/7626005571403137078)
  - 推理：决定下一步行动、是否继续调用工具、如何调整计划。 [developer.volcengine](https://developer.volcengine.com/articles/7534567993976897574)
  - 生成：输出自然语言回复，或输出结构化结果（如 JSON 工具参数、步骤列表）。 [juejin](https://juejin.cn/post/7626005571403137078)
- Agent 其他模块的作用：弥补大模型的局限（时效性、上下文长度、计算能力、执行能力）。 [ibm](https://www.ibm.com/cn-zh/think/topics/ai-agents)

***

## 4. 典型技术模式与协议

1. **ReAct/CoT 流程**  
   - Thought：模型内部思考，确定下一步。 [developer.volcengine](https://developer.volcengine.com/articles/7534567993976897574)
   - Action：发出工具调用指令。 [developer.volcengine](https://developer.volcengine.com/articles/7534567993976897574)
   - Observation：拿到工具结果，再次思考。 [developer.volcengine](https://developer.volcengine.com/articles/7534567993976897574)
   - Final Answer：认为任务完成，输出最终答案。 [developer.volcengine](https://developer.volcengine.com/articles/7534567993976897574)

2. **RAG 与记忆/知识接入**  
   - 用向量检索把外部文档、业务知识接到 Agent 里，提升专业性与时效性。 [woshipm](https://www.woshipm.com/ai/6237707.html)
   - 在 Agent 场景中，RAG 常和长期记忆/知识库模块合并考虑。 [woshipm](https://www.woshipm.com/ai/6237707.html)

3. **多 Agent 协作 / MAS**  
   - 多个具有不同角色的 Agent 协同完成复杂任务（如“规划 Agent + 执行 Agent + 质检 Agent”）。 [woshipm](https://www.woshipm.com/ai/6237707.html)
   - 需要协议（如 A2A 协议）来管理 Agent 间的通信和任务分配。 [woshipm](https://www.woshipm.com/ai/6237707.html)

4. **函数调用、工具协议、MCP 等**  
   - 函数调用：通过模型原生 function calling 或 tool calling API，把 Java 后端、脚本等暴露为工具。 [woshipm](https://www.woshipm.com/ai/6237707.html)
   - MCP（Model Context Protocol）等协议：统一工具、数据源和模型之间的连接方式，提高可扩展性。 [woshipm](https://www.woshipm.com/ai/6237707.html)

***

## 5. 从知识到实战的落地链路（偏工程视角）

结合你“Java 后端 + AI Agent”的路线，一个 Agent 应用的核心链路可以抽象成： [damodev.csdn](https://damodev.csdn.net/692d0cbe2087ae0db79de14f.html)

1. 输入与感知：HTTP/WS 接口接收用户请求，可叠加语音、前端 Web UI。  
2. 调度 LLM：调用大模型（本地/云端）作为 Agent 大脑。  
3. 规划与循环：实现 ReAct/工作流，控制「思考–工具–观察–迭代」。 [damodev.csdn](https://damodev.csdn.net/692d0cbe2087ae0db79de14f.html)
4. 工具接入：  
   - Java 微服务 API（Spring Boot）、数据库（MySQL）、Redis、文件系统等暴露为工具。 [damodev.csdn](https://damodev.csdn.net/692d0cbe2087ae0db79de14f.html)
5. 记忆与 RAG：  
   - 短期：会话上下文；长期：向量库（如 Milvus/Faiss/pgvector）+ 你的业务知识。 [betteryeah](https://www.betteryeah.com/blog/ai-agent-core-components-architecture-guide)
6. 监控与反馈：  
   - 日志、指标、用户评分，用于不断优化提示、工具设计和工作流。 [damodev.csdn](https://damodev.csdn.net/692d0cbe2087ae0db79de14f.html)

***

## 6. 常见八大/六大核心知识点速记版

部分中文资料喜欢列清单，你可以按下面这组当“复盘 Checklist”来记： [betteryeah](https://www.betteryeah.com/blog/ai-agent-core-components-architecture-guide)

- 智能体/Agent 的定义与目标驱动。  
- 大模型能力：理解、推理、生成。  
- 感知模块：多模态输入、业务数据接入。  
- 规划与决策：任务分解、ReAct、工作流编排。  
- 工具调用：函数调用、API 集成、系统操作。  
- 记忆系统：短期上下文、长期向量库/RAG。  
- 行动执行与任务闭环：从计划到真正改变环境。  
- 多 Agent 与协议：MAS、MCP、A2A 等协作机制。  

***
