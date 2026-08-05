## 1. 打基础：语言 + 大模型概念（1～3 周）

你是 Java 后端，可以不从零学编程，但需要补两块： [developer.volcengine](https://developer.volcengine.com/articles/7587307838910693395)

- Python 基础（建议）  
  - 目的：方便用现成 SDK、示例和开源项目（大多是 Python）。  
  - 范围：函数、类、列表/字典、虚拟环境、pip、常见库（requests、pydantic、fastapi 等）。 [yun.itheima](https://yun.itheima.com/subject/aimap/index.html)
- 大模型基础认知  
  - 知道什么是 LLM、Transformer、自注意力，理解“预训练 + 微调”的概念即可，不必推公式。 [blog.csdn](https://blog.csdn.net/monokai/article/details/146390870)
  - 区分几类模型：对话类（ChatGPT 等）、代码类、Embedding 模型、多模态模型。 [betteryeah](https://www.betteryeah.com/blog/large-language-model-application-development-tech-stack-guide-2025)

输出要求：你能看懂一篇“LLM 简介 + ChatGPT 原理”的文章、能用 Python 写一个简单脚本调 HTTP API。 [cloud.tencent](https://cloud.tencent.com/developer/article/2436380)

## 2. 入门实战：调用 API + Prompt 工程（2～4 周）

这是“从不会到能做 Demo”的关键阶段。 [github](https://github.com/datawhalechina/llm-universe)

- 调用大模型 API  
  - 学会用 HTTP / SDK 调国内外主流模型（OpenAI/DeepSeek/文心/星火等）。 [docs.feishu](https://docs.feishu.cn/v/wiki/Pbruw4cZEiFLmrkXNmGcFhB2nId/a5)
  - 学会：补全、对话、函数调用（tool calling）这三类基本模式。 [cnblogs](https://www.cnblogs.com/longronglang/p/18696880)
- Prompt Engineering（提示词工程）  
  - 常用模式：角色设定、思维链（CoT）、分步骤规划、少样本（few-shot）示例。 [developer.volcengine](https://developer.volcengine.com/articles/7587307838910693395)
  - 学会为：问答助手、格式化输出、代码生成等场景写 Prompt 并反复迭代。 [cloud.tencent](https://cloud.tencent.com/developer/article/2436380)
- 简单 Demo 项目  
  - 用 FastAPI/Flask + 前端（或命令行/Gradio）做 1–2 个 Demo，如：  
    - 问答机器人（支持多轮对话）；  
    - 文本改写/摘要工具（给定文章生成摘要）。 [github](https://github.com/datawhalechina/llm-universe)

可参考的开源教程：  
- Datawhale 的「动手学大模型应用开发/LLM Universe」：完整拆解从 API 调用到一个知识库助手的流程。 [cloud.tencent](https://cloud.tencent.com/developer/article/2436380)
- 一些“零基础大模型应用开发”教程，强调从项目中学工程流程。 [cnblogs](https://www.cnblogs.com/longronglang/p/18696880)

阶段目标：能独立写出一个调用多个模型 API 的小 Web 应用，对 Prompt 和返回结果调优有直觉。 [developer.volcengine](https://developer.volcengine.com/articles/7587307838910693395)

## 3. 核心能力：RAG 检索增强 & LangChain 等框架（3–6 周）

当前大模型应用的主流形态是“RAG + Agent”，RAG 是第一必修课。 [nowcoder](https://www.nowcoder.com/feed/main/detail/e6cc64b21f4644a6b7a8f6538b35c038)

- RAG（检索增强生成）概念  
  - 流程：切分文档 → 向量化（Embedding）→ 存入向量数据库 → 基于查询召回片段 → 组织 Prompt 交给 LLM。 [cnblogs](https://www.cnblogs.com/longronglang/p/18696880)
  - 典型场景：知识库问答、企业文档问答、FAQ 机器人等。 [nowcoder](https://www.nowcoder.com/feed/main/detail/e6cc64b21f4644a6b7a8f6538b35c038)
- 向量数据库与数据处理  
  - 学会用 1–2 个向量库：如 Chroma、FAISS、Milvus、Pinecone 等。 [blog.csdn](https://blog.csdn.net/monokai/article/details/146390870)
  - 会做：文档抽取（PDF/HTML/Markdown）、清洗、切分（chunking）、Embedding 存储。 [blog.csdn](https://blog.csdn.net/monokai/article/details/146390870)
- 开发框架（LangChain / LlamaIndex / LangGraph 等）  
  - 掌握 LangChain/LlamaIndex 的基本组件：LLM、Prompt、Retriever、Chain、Tool、Memory。 [betteryeah](https://www.betteryeah.com/blog/large-language-model-application-development-tech-stack-guide-2025)
  - 能搭出一个“企业知识库问答”应用：  
    - 支持上传文档；  
    - 构建向量索引；  
    - 实现基于 RAG 的高质量问答。 [github](https://github.com/datawhalechina/llm-universe)

阶段目标：你能做一个“自家项目文档/接口文档知识库问答系统”，并解释清楚它的架构和各技术选型原因。 [nowcoder](https://www.nowcoder.com/feed/main/detail/e6cc64b21f4644a6b7a8f6538b35c038)

## 4. 进阶：Agent、工作流、多模态与工程落地（4–8 周）

在 RAG 上再往前一步，就是多工具协作的 Agent 和工程化落地。 [damodev.csdn](https://damodev.csdn.net/698c4ffc54b52172bc5b1f81.html)

- Agent / 工作流  
  - 理解 Agent 的概念：基于 LLM 的“决策 + 调用工具 + 迭代计划”。 [betteryeah](https://www.betteryeah.com/blog/large-language-model-application-development-tech-stack-guide-2025)
  - 熟悉常见方案：LangChain Agents、LangGraph 的状态机/有向图工作流，或 Coze/Dify 这类“低代码/可视化 Agent 平台”。 [yun.itheima](https://yun.itheima.com/subject/aimap/index.html)
  - Demo 示例：  
    - 一个“工单处理助手”：读取工单 → 调用搜索/知识库 → 生成回复草稿；  
    - 一个“数据分析 Agent”：结合 SQL 工具/BI 接口做简单分析解读。 [cnblogs](https://www.cnblogs.com/mingtingspring/p/19192103)
- 多模态与特定场景  
  - 文本 + 图像：图像理解、图文生成、截图问答。  
  - 语音相关（可选）：语音识别、语音合成接入，对话机器人。 [blog.csdn](https://blog.csdn.net/monokai/article/details/146390870)
- 工程化落地  
  - 后端开发视角：  
    - 把 LLM 调用封装成微服务，对接你现有 Java 后端（REST、gRPC）。  
    - 做限流、重试、熔断、日志、Tracing、埋点统计（如调用耗时、Token 消耗）。 [betteryeah](https://www.betteryeah.com/blog/large-language-model-application-development-tech-stack-guide-2025)
  - 部署与运维：  
    - 云服务部署（Docker + K8s 或 Serverless）；  
    - API Key/权限管理、审计日志、敏感词过滤等合规能力。 [nvidia](https://www.nvidia.cn/training/learning-path/generative-ai-llm/)

阶段目标：把一个 RAG/Agent 型应用产品化：有前端、有后端服务、能部署上线、能监控效果并迭代。 [developer.volcengine](https://developer.volcengine.com/articles/7587307838910693395)

## 5. 后端开发者的技术栈对照表

你目前是 Java 后端，可以这么类比自己的已有技能与大模型栈。 [processon](https://www.processon.com/view/687518f8b4e033210ee2c655)

| 你现有后端技能 | 在大模型应用里的对应 |
| --- | --- |
| Spring Boot + REST API | FastAPI / Node + LLM API 网关服务  [cloud.tencent](https://cloud.tencent.com/developer/article/2436380) |
| MySQL/Redis | 向量数据库 + 结构化业务库（两者并存） [cloud.tencent](https://cloud.tencent.com/developer/article/2436380) |
| MQ、定时任务 | LLM 异步任务执行、批处理生成、评估任务  [betteryeah](https://www.betteryeah.com/blog/large-language-model-application-development-tech-stack-guide-2025) |
| 网关、鉴权 | LLM 应用 API 网关、多租户 & Key 管理  [betteryeah](https://www.betteryeah.com/blog/large-language-model-application-development-tech-stack-guide-2025) |
| 微服务 & Observability | LLM 应用的日志、Tracing、性能指标、AB 实验  [betteryeah](https://www.betteryeah.com/blog/large-language-model-application-development-tech-stack-guide-2025) |

一条典型的“后端转大模型应用开发”路径也会强调：后端 + RAG + Agent 是主线，不必一上来就研究训练/微调。 [damodev.csdn](https://damodev.csdn.net/698c4ffc54b52172bc5b1f81.html)

***
