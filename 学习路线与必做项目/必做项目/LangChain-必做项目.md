# LangChain 必做项目清单（入门 → RAG → Agent → 工程化）

---

## 一、入门必做（吃透 LangChain 基础链路）

### 1. 基础对话链 + 多轮记忆对话

**技术栈**  
LangChain + Ollama  

**核心知识点**  
- 模型封装：`ChatOpenAI` / `ChatOllama`  
- Prompt 模板：`PromptTemplate`、`ChatPromptTemplate`  
- 会话记忆：`ConversationBufferMemory`、`ConversationSummaryMemory`  
- 链式调用：LCEL 表达式、`Runnable` 编排  

**核心功能**  
- 多轮上下文对话  
- 对话总结记忆  
- 自定义角色人格对话  

**预计耗时**：1–2 天  

---

### 2. Prompt 工程模块化项目

**技术栈**  
LangChain + Ollama  

**核心知识点**  
- Few-shot 提示：`FewShotPromptTemplate`  
- 动态变量注入、严格格式约束  
- 输出解析器：`PydanticOutputParser`、`StrOutputParser`  

**核心功能**  
- 代码生成 / 重构 / 解释  
- 结构化 JSON 输出（如接口定义、测试用例）  
- 文档总结、翻译改写  

**预计耗时**：2 天  

---

## 二、核心必做（RAG 检索增强，企业刚需）

### 3. 单文件 PDF 知识库问答

**技术栈**  
LangChain + Ollama + Chroma / Milvus  

**核心知识点**  
- 文档加载：`PyPDFLoader`、`DirectoryLoader`  
- 文本分割：`RecursiveCharacterTextSplitter`  
- 嵌入模型：BGE / m3e 等中文向量  
- 检索链：`RetrievalQA`，`Stuff` / `MapReduce` / `Refine` 问答链  

**核心功能**  
- 上传单个 PDF  
- AI 基于文档内容精准问答  
- 回答中标注原文引用位置  

**预计耗时**：2–3 天  

---

### 4. 多文档混合 RAG 系统

**技术栈**  
LangChain + Milvus  

**核心知识点**  
- 批量文档加载、文件夹遍历  
- 持久化向量库、增量向量更新  
- 相似度检索、TopK 召回  

**核心功能**  
- 多 PDF / TXT 文档统一知识库  
- 跨文档问答与来源标注  

**预计耗时**：3 天  

---

### 5. 网页实时 RAG 问答

**技术栈**  
LangChain + WebBaseLoader + Ollama  

**核心知识点**  
- `WebBaseLoader` 网页加载与正文清洗  
- 临时向量库构建  
- 即时检索与问答  

**核心功能**  
- 输入网页 URL  
- 自动抓取页面并构建临时知识库  
- 基于网页内容实时回答问题  

**预计耗时**：2–3 天  

---

### 6. RAG 检索优化实战

**技术栈**  
LangChain + Milvus  

**核心知识点**  
- 混合检索：关键词 + 向量检索融合  
- 重排序：CrossEncoder Reranker  
- 分块策略调优（chunk 大小、重叠、语义分块）  

**核心功能**  
- 提升问答准确率、召回率  
- 降低幻觉，增强引用溯源准确性  

**预计耗时**：3–4 天  

---

## 三、进阶必做（Agent 智能体，高阶竞争力）

### 7. 基础工具调用 Agent

**技术栈**  
LangChain + Ollama  

**核心知识点**  
- 自定义工具、`@tool` / `StructuredTool`  
- Agent 类型：ReAct、Functions / Tool Calling Agent  
- 思考–行动–观察循环、错误重试机制  

**核心功能**  
- 计算器、时间查询、文件读写  
- 调用 HTTP 接口、组合多工具执行任务  

**预计耗时**：3 天  

---

### 8. 联网搜索 Agent

**技术栈**  
LangChain + SerpAPI / DuckDuckGo  

**核心知识点**  
- 搜索工具集成  
- 结合搜索结果进行时效性问答  

**核心功能**  
- 回答最新新闻、热点、实时数据问题  
- 输出带来源链接的总结  

**预计耗时**：2–3 天  

---

### 9. 多任务规划 Agent

**技术栈**  
LangChain + Ollama  

**核心知识点**  
- Plan-and-Execute 规划执行 Agent  
- 复杂任务拆解、子任务编排、状态追踪  
- 子任务执行结果汇总生成最终报告  

**核心功能**  
- 将复杂自然语言需求拆成多步执行计划  
- 自动选择合适工具与链路完成任务  

**预计耗时**：4 天  

---

## 四、工程化必做（后端整合 + 业务落地）

### 10. 带权限的企业级 RAG 后台

**技术栈**  
LangChain + FastAPI + Milvus + MySQL  

**核心知识点**  
- 用户体系：会话隔离、文档权限管理  
- 问答记录存储、操作日志、监控  
- 接口限流、异步任务处理  

**核心功能**  
- 多用户独立知识库  
- 文档上传 / 删除 / 权限配置  
- 问答溯源记录与后台管理界面  

**预计耗时**：4–5 天  

---

### 11. 智能客服对话系统

**技术栈**  
LangChain + RAG + Ollama  

**核心知识点**  
- 意图识别、问题分类  
- 多轮对话管理、上下文记忆  
- 知识库匹配与回复生成  
- 人工转接逻辑  

**核心功能**  
- 企业客服自动问答  
- 工单生成与 FAQ 更新  
- 对话数据统计与分析  

**预计耗时**：4–5 天  

---

### 12. 代码生成 & 调试 Agent

**技术栈**  
LangChain + CodeLlama + Ollama  

**核心知识点**  
- 代码工具调用、格式化输出  
- 代码执行与错误捕获  
- 自动修复、注释 / 文档生成  

**核心功能**  
- Java / Python 代码生成与重构  
- 运行代码 + 根据报错自动修复  
- 输出带注释与文档的代码片段  

**预计耗时**：3–4 天  

---

## 五、高阶必做（性能优化 + 部署）

### 13. LangChain 流式输出 + SSE 接口

**技术栈**  
LangChain + FastAPI + Ollama  

**核心知识点**  
- 模型流式输出（callbacks / streaming）  
- SSE（Server-Sent Events）接口封装  
- 前端打字机效果实现  

**核心功能**  
- 流式问答 API  
- 前后端实时交互体验  

**预计耗时**：2 天  

---

### 14. Docker 容器化部署 LangChain 应用

**技术栈**  
Docker + LangChain + Milvus  

**核心知识点**  
- LangChain 服务、Milvus、Ollama 多容器编排  
- 环境变量管理、网络配置  
- 向量库数据持久化、一键部署  

**核心功能**  
- 私有化 RAG / Agent 应用集群  
- 快速部署、扩容、迁移  

**预计耗时**：3 天  

---

## 六、极简必做优先级（6 个项目就够打简历）

1. 多轮记忆对话 + LCEL 基础链路  
2. PDF 单文件 RAG 问答  
3. 网页实时 RAG 问答  
4. 工具调用 Agent  
5. 联网搜索 Agent  
6. 企业级 RAG 后台（带权限 + 日志）  

---

## 七、项目落地标准（可直接写进简历）

1. 所有项目代码上传 GitHub，配套标准 README（架构图 + 时序图可选）  
2. 项目描述中明确：  
   - 使用 LangChain 组件（Loader / Splitter / VectorStore / Chain / Agent / Memory）  
   - RAG 流程、Agent 工具调用逻辑  
   - 私有化部署 / 限流 / 权限 / 日志等工程化设计  
3. 项目完整链路必须覆盖：  
   - 文档 / 数据加载 → 分块 → 嵌入向量化 → 写入向量库 → 检索 → 生成回答  
   - 出错重试、超时处理、日志记录等基础健壮性  
4. 优先选用：  
   - 模型：Ollama 本地大模型  
   - 向量库：Chroma（入门）、Milvus（企业级）  
   - Embedding：BGE / m3e  
   - 后端：FastAPI（原型）+ SpringAI（Java 生态复用）  
   - 部署：Docker 容器化，方便接入你的 Java 后端体系  
