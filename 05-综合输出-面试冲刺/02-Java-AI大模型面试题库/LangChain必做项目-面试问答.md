# LangChain 必做项目 面试问答
> 🎯 基于 LangChain 实战项目清单，涵盖面试高频问题与完美解答方案，帮助你在 LLM 应用开发方向建立核心竞争力。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：LangChain 的核心组件有哪些？LCEL 是什么？
**面试官意图：** 考察对 LangChain 整体架构的理解。

**完美解答：**

LangChain 是一个面向大模型应用开发的编排框架，它的核心组件可以分为以下六层：

**1. 模型 IO（Model I/O）**
- **模型封装：** ChatOllama、ChatOpenAI 等，统一不同模型的调用接口
- **Prompt 模板：** PromptTemplate、ChatPromptTemplate，实现 Prompt 结构化与复用
- **输出解析器：** PydanticOutputParser、StrOutputParser，将模型输出转为结构化数据

**2. 检索增强（Retrieval）**
- **文档加载器：** PyPDFLoader、WebBaseLoader 等 100+ 种加载器
- **文本分割器：** RecursiveCharacterTextSplitter、SemanticChunker
- **向量存储：** Chroma、Milvus、FAISS 的统一接口
- **检索器：** 向量检索、混合检索、上下文压缩、重排序

**3. 链（Chains）**
- **基础链：** LCEL（LangChain Expression Language）的 Runnable 接口
- **预置链：** RetrievalQA、ConversationalRetrievalChain
- **自定义链：** 通过 LCEL 组合多个 Runnable 实现复杂逻辑

**4. 记忆（Memory）**
- BufferMemory：保留完整对话历史
- SummaryMemory：用 LLM 压缩历史为摘要
- 持久化：SQLite / Redis 后端

**5. 智能体（Agent）**
- **ReAct Agent：** 思考-行动-观察循环
- **Tool Calling Agent：** 函数调用模式
- **自定义工具：** @tool 装饰器、StructuredTool

**6. 回调（Callbacks）**
- 日志、监控、流式输出、Token 计数

**LCEL（LangChain Expression Language）** 是 LangChain 的声明式编排语法，用 `|` 管道操作符将多个 Runnable 组件串联成处理流水线：

```python
# LCEL 示例：一条链 = Prompt → 模型 → 解析器
chain = prompt_template | model | output_parser

# 等价于传统写法
chain = LLMChain(
    prompt=prompt_template,
    llm=model,
    output_parser=output_parser
)
```

**LCEL 的核心优势：**
- 流式支持自动生效
- 异步调用自动支持
- 易于调试和跟踪
- 支持分支、并行、回退等高级编排

**延伸追问应对：** 如果问"LCEL 和传统 Chain API 的取舍"，回答：LCEL 是 LangChain 的未来（从 0.1.x 开始 LCEL 是第一公民），新项目优先用 LCEL。传统 Chain API 适合快速原型，LCEL 适合生产级应用。

---

### Q2：LangChain 中的 Agent 是如何工作的？ReAct 模式是什么？
**面试官意图：** 考察 Agent 和推理循环的理解。

**完美解答：**

**Agent 工作流程：**

```
用户输入 → 思考(Thought) → 行动(Action) → 观察(Observation) → 循环...
```

**ReAct（Reasoning + Acting）模式** 是 Agent 的核心推理范式，它将"推理"和"行动"交替进行：

1. **思考（Thought）：** LLM 分析当前问题和已有信息，决定下一步做什么
2. **行动（Action）：** 选择并调用一个工具，传入参数
3. **观察（Observation）：** 获取工具返回的执行结果
4. **重复：** 基于观察结果继续思考，直到生成最终答案或到达最大步数

**实现示例：**
```python
from langchain.agents import create_react_agent, AgentExecutor
from langchain_core.tools import tool

@tool
def calculate(expression: str) -> str:
    """计算数学表达式"""
    return str(eval(expression))

@tool
def get_current_time() -> str:
    """获取当前时间"""
    from datetime import datetime
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

tools = [calculate, get_current_time]

agent = create_react_agent(
    model=llm,
    tools=tools,
    prompt=react_prompt_template
)

agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    verbose=True,
    max_iterations=5,  # 防止无限循环
    handle_parsing_errors=True  # 容错
)

result = agent_executor.invoke({"input": "今天是几号？加上100天是几号？"})
```

**控制循环的关键参数：**
- `max_iterations`：最大思考-行动轮数，防止无限循环
- `max_execution_time`：最大执行时间
- `early_stopping_method`：提前终止策略
- `handle_parsing_errors`：解析错误时的容错处理

> 🎯 **面试总结：** Agent 的本质是"LLM 作为推理引擎 + 外部工具作为手脚"——LLM 负责规划和决策，工具负责执行具体操作。

---

### Q3：LangChain 中 Prompt 工程的核心技巧有哪些？
**面试官意图：** 考察 Prompt 设计和结构化输出能力。

**完美解答：**

**1. Prompt 模板结构化**
```python
from langchain_core.prompts import ChatPromptTemplate

# 系统消息 + 用户消息模板
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}，请用{style}风格回答问题。"),
    ("user", "{question}")
])

chain = prompt | llm
chain.invoke({"role": "Java技术专家", "style": "简洁专业", "question": "什么是RAG？"})
```

**2. Few-Shot 示例学习**
```python
from langchain_core.prompts import FewShotPromptTemplate

examples = [
    {"input": "排序算法有哪些？", "output": "冒泡、快排、归并、堆排序"},
    {"input": "数据库索引类型？", "output": "B+树、Hash、全文索引"},
]

prompt = FewShotPromptTemplate(
    examples=examples,
    example_prompt=example_prompt_template,
    suffix="输入: {input}\n输出:",
    input_variables=["input"]
)
```

**3. 结构化输出（PydanticOutputParser）**
```python
from pydantic import BaseModel, Field
from langchain_core.output_parsers import PydanticOutputParser

class Resume(BaseModel):
    name: str = Field(description="候选人姓名")
    skills: list[str] = Field(description="技能列表")
    years_experience: int = Field(description="工作经验年数")

parser = PydanticOutputParser(pydantic_object=Resume)

prompt = PromptTemplate(
    template="从以下文本中提取简历信息：\n{text}\n{format_instructions}",
    input_variables=["text"],
    partial_variables={"format_instructions": parser.get_format_instructions()}
)

chain = prompt | llm | parser
result = chain.invoke({"text": "张三，5年Java经验，熟悉Spring Cloud..."})
```

**4. Prompt 优化技巧：**
- **明确角色定位：** "你是一个资深的 Java 架构师" 比 "请回答" 效果好 30%+
- **约束先行：** 在 Prompt 开头给出约束条件，比末尾有效
- **One-Shot / Few-Shot 示例：** 比纯描述指令效果更好
- **分步思考指令：** "请一步一步思考"（Chain-of-Thought）能显著提升推理质量

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你是如何用 LangChain 实现多轮记忆对话的？用的哪种 Memory？
**面试官意图：** 考察对话记忆机制的实践。

**完美解答：**

我实现了两种记忆模式，根据场景切换：

**方案一：ConversationBufferMemory（保留完整历史）**
```python
from langchain.memory import ConversationBufferMemory
from langchain.chains import ConversationChain

memory = ConversationBufferMemory()
conversation = ConversationChain(llm=llm, memory=memory)

conversation.predict(input="你好，我是张三")
conversation.predict(input="我是谁？")  # 正确回答：你是张三
```
- 优点：完整保留对话上下文，不丢失信息
- 缺点：长对话会占用大量 token（费用高、上下文窗口溢出）

**方案二：ConversationSummaryMemory（摘要压缩）**
```python
from langchain.memory import ConversationSummaryMemory

memory = ConversationSummaryMemory(llm=llm, max_token_limit=500)
conversation = ConversationChain(llm=llm, memory=memory)
```
- 优点：只保留摘要，token 消耗可控
- 缺点：摘要过程会丢失细节，且每次对话都需要过一遍 LLM 做摘要

**方案三：混合策略（推荐）**
```python
class HybridMemory:
    def __init__(self, recent_count=5):
        self.buffer = ConversationBufferMemory(return_messages=True, k=recent_count)
        self.summary = ConversationSummaryMemory(llm=llm, max_token_limit=300)

    def load_memory_variables(self, inputs):
        # 最近 k 轮完整历史 + 早期对话摘要
        recent = self.buffer.load_memory_variables(inputs)
        summary = self.summary.load_memory_variables(inputs)
        return {"chat_history": summary + recent}
```
这个方案兼顾了近期细节和远期概况，相比单一 Memory 方案效果提升明显。

---

### Q5：请描述你如何用 LangChain 实现一个工具调用 Agent，并做一个联网搜索的 Agent
**面试官意图：** 考察 Agent 工具调用和外部数据源整合能力。

**完美解答：**

**基础工具调用 Agent：**
```python
from langchain_core.tools import tool
from langchain.agents import create_tool_calling_agent, AgentExecutor

# 自定义工具
@tool
def query_database(sql: str) -> str:
    """执行数据库查询并返回结果"""
    # 实际执行 SQL
    return execute_sql(sql)

@tool
def call_api(url: str, params: dict) -> dict:
    """调用外部 HTTP API"""
    return requests.get(url, params=params).json()

# 创建 Agent
agent = create_tool_calling_agent(llm, [query_database, call_api])
agent_executor = AgentExecutor(
    agent=agent,
    tools=[query_database, call_api],
    max_iterations=10,
    return_intermediate_steps=True  # 保留中间步骤用于调试
)
```

**联网搜索 Agent：**
```python
from langchain_community.tools import DuckDuckGoSearchRun

# 搜索工具
search_tool = DuckDuckGoSearchRun()

@tool
def web_search(query: str) -> str:
    """联网搜索获取最新信息"""
    return search_tool.run(query)

@tool
def fetch_url(url: str) -> str:
    """抓取特定 URL 的正文内容"""
    from langchain_community.document_loaders import WebBaseLoader
    loader = WebBaseLoader(url)
    docs = loader.load()
    return docs[0].page_content[:2000]

tools = [web_search, fetch_url]

agent = create_react_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools)

# 使用
result = agent_executor.invoke({
    "input": "帮我查一下2024年诺贝尔物理学奖得主是谁？并简单介绍其贡献"
})
```

**Agent 的容错设计：**
- 工具调用失败时自动重试（`max_retries=2`）
- 解析 LLM 输出格式错误时自动修复
- 超时保护：每个工具执行不超过 10 秒

> 💡 **面试亮点：** "Agent 的设计核心是要做好容错——LLM 生成工具参数、解析工具返回结果这两个环节最容易出错。我的经验是一定要加 `handle_parsing_errors=True`，并给每个工具设置明确的错误返回格式。"

---

### Q6：LangChain 的 RAG 检索优化（混合检索 + 重排序）你是怎么实现的？
**面试官意图：** 考察 RAG 检索优化的深度实践。

**完美解答：**

我使用 EnsembleRetriever + ContextualCompressionRetriever 实现了完整的检索优化管线。

```python
from langchain.retrievers import BM25Retriever, EnsembleRetriever
from langchain.retrievers.document_compressors import CrossEncoderReranker

# 1. 向量检索器
vector_retriever = vectorstore.as_retriever(search_kwargs={"k": 20})

# 2. BM25 关键词检索器
keyword_retriever = BM25Retriever.from_documents(all_documents)
keyword_retriever.k = 20

# 3. 混合检索（RRF 融合）
ensemble_retriever = EnsembleRetriever(
    retrievers=[vector_retriever, keyword_retriever],
    weights=[0.6, 0.4]  # 向量权重略高
)

# 4. Reranker 重排序
reranker = CrossEncoderReranker(
    model_name="BAAI/bge-reranker-large",
    top_n=5
)

compression_retriever = ContextualCompressionRetriever(
    base_compressor=reranker,
    base_retriever=ensemble_retriever
)

# 使用
results = compression_retriever.get_relevant_documents(query)
```

**分块策略调优：**
```python
# 策略一：固定大小分块（通用）
RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)

# 策略二：语义分块（效果更好，但更慢）
from langchain_experimental.text_splitter import SemanticChunker
SemanticChunker(embeddings, breakpoint_threshold_type="percentile")

# 策略三：按文档结构分块（适合技术文档）
# MarkdownHeaderTextSplitter：按标题层级自动分块
```

**效果对比：**
- 基础向量检索：Recall@5 = 72%
- +BM25 混合检索：Recall@5 = 84%
- +Reranker 重排序：Precision@5 = 91%
- +语义分块：Recall@5 = 88%, Precision@5 = 93%

> ⚠️ **注意：** 检索优化遵循"边际递减"规律——前两个优化（混合检索 + 重排序）能带来 20%+ 提升，后续优化可能只有 3-5% 的边际收益。应该根据效果数据决定是否继续投入。

---

### Q7：LangChain 的流式输出（Streaming）是如何实现的？
**面试官意图：** 考察对用户体验优化和 SSE 协议的理解。

**完美解答：**

流式输出（Streaming）的核心是让 LLM 生成的内容一个 token 一个 token 地实时推送给前端，实现"打字机效果"。

**后端实现（FastAPI + LangChain）：**
```python
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from langchain_core.callbacks import CallbackManager

app = FastAPI()

@app.post("/chat/stream")
async def chat_stream(query: str):
    return StreamingResponse(
        generate_tokens(query),
        media_type="text/event-stream"
    )

async def generate_tokens(query: str):
    # 使用 LCEL 的流式支持
    async for chunk in rag_chain.astream({"query": query}):
        # 内容 token
        if "answer" in chunk:
            yield f"data: {json.dumps({'type': 'token', 'content': chunk['answer']})}\n\n"
        # 溯源信息（最后返回）
        if "source_documents" in chunk:
            yield f"data: {json.dumps({'type': 'source', 'content': chunk['source_documents']})}\n\n"
```

**前端消费（JavaScript）：**
```javascript
const eventSource = new EventSource(`/chat/stream?query=${encodeURIComponent(query)}`);
let answer = '';

eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.type === 'token') {
        answer += data.content;
        displayAnswer(answer);  // 实时渲染
    } else if (data.type === 'source') {
        displaySources(data.content);  // 显示引用来源
    }
};
```

**核心要点：**
- LangChain 的 LCEL 链自动支持 `.astream()` 方法
- 回调 `Callbacks` 可以在每个 token 生成时触发自定义逻辑
- SSE 比 WebSocket 更适合"单向流"场景，实现更简单

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q8：设计一个带权限管理的企业级 RAG 后台，你的架构是怎样的？
**面试官意图：** 考察工程化思维和全栈架构能力。

**完美解答：**

**系统分层架构：**
```
[React/Vue 前端]
    ↓
[API 网关]
    ↓                    ↓               ↓
[用户服务]          [RAG 服务]        [日志服务]
[Auth/JWT]    [LangChain+Milvus]    [ELK/监控]
    ↓                    ↓
[MySQL]            [Milvus + Redis]
```

**核心模块设计：**

**1. 会话隔离与权限管理**
```python
class AuthMiddleware:
    def process_request(self, request):
        # JWT 解析 → 获取 user_id, tenant_id, role
        # 注入到 LangChain 回调上下文中
        langchain_callback.set_user(request.user_id)
        langchain_callback.set_tenant(request.tenant_id)

# 检索时自动过滤
retriever = vectorstore.as_retriever(
    search_kwargs={
        "k": 5,
        "filter": {"tenant_id": {"$eq": current_tenant}}
    }
)
```

**2. 文档管理流程**
```
上传 → 格式校验 → 解析 → 分块 → Embedding → 入库(Milvus) → 记录到 MySQL
```
用户只能看到自己租户的文档，超管可查看所有文档。

**3. 问答记录与日志**
```sql
-- MySQL 表结构
CREATE TABLE qa_logs (
    id BIGINT AUTO_INCREMENT,
    user_id VARCHAR(64),
    question TEXT,
    answer TEXT,
    retrieved_docs JSON,
    latency_ms INT,
    feedback TINYINT,  -- 0=无, 1=赞, -1=踩
    created_at DATETIME
);
```

**4. 监控告警**
- Prometheus 采集：RAG 请求量、延迟分布、召回率
- Grafana 看板：实时监控服务健康
- Bad Case 收集：将用户点踩的问题自动入库，用于后续优化

---

### Q9：LangChain 和 Spring AI 的对比？在 Java 生态中怎么选？
**面试官意图：** 考察对 AI 框架选型的判断力。

**完美解答：**

| 对比维度 | LangChain | Spring AI |
|----------|-----------|-----------|
| 语言生态 | Python | Java |
| 社区生态 | 最大（GitHub 90k+ stars） | 较新（GitHub 2k+ stars） |
| 组件丰富度 | 极高（Loader/Retriever/Agent/Memory 等） | 基本覆盖（Model/VectorStore/Retriever） |
| Agent 支持 | 成熟（ReAct, Tools Calling） | 开发中 |
| LCEL | 强大的声明式编排 | 无对应功能 |
| Java 整合 | 需通过 HTTP 服务调用 | 原生 SpringBoot 整合 |
| 学习曲线 | 中高（概念多） | 低（Spring 风格一致） |

**选型建议：**

| 场景 | 推荐 | 原因 |
|------|------|------|
| 原型验证 / 快速迭代 | LangChain (Python) | 组件丰富，社区资源多 |
| 纯 Java 团队 / 企业后端 | Spring AI | Java 生态原生，工程化好 |
| Agent 重度的应用 | LangChain | Agent 系统更成熟 |
| RAG 服务化 | LangChain + FastAPI + Java 网关 | Python 做 AI 层，Java 做网关层 |

**我的实践策略：** 在 RAG 项目中，我用 Python + LangChain 做 AI 推理层（RAG 链路），用 SpringBoot Java 做网关层（鉴权、限流、路由）。这样既利用了 LangChain 的 AI 能力，又复用了 Java 生态的工程化优势。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q10：你的 LangChain Agent 在执行任务时陷入循环，不返回结果，怎么处理？
**面试官意图：** 考察 Agent 调试和容错处理能力。

**完美解答：**

Agent 死循环是常见问题，我会从"预防"和"熔断"两个角度处理。

**预防方案（架构层面）：**
```python
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    max_iterations=6,         # 最大思考-行动轮数
    max_execution_time=30,    # 最大执行时间（秒）
    early_stopping_method="generate",  # 超时后强制生成答案
    handle_parsing_errors=True,        # 解析错误容错
)
```

**熔断方案（运行时）：**

**1. 监控中间步骤**
```python
result = agent_executor.invoke({"input": "..."})
# 查看每一步的思考-行动-观察
for step in result["intermediate_steps"]:
    print(f"思考: {step[0].log}")
    print(f"行动: {step[0].tool} - {step[0].tool_input}")
    print(f"观察: {step[1]}")
```

**2. 手动干预**
```python
# 设置回调，检测到重复模式时主动截断
class LoopDetectorCallback:
    def __init__(self):
        self.action_history = []

    def on_agent_action(self, action, **kwargs):
        action_signature = f"{action.tool}({action.tool_input})"
        self.action_history.append(action_signature)

        # 检测连续重复的行动
        if len(self.action_history) >= 3:
            recent = self.action_history[-3:]
            if len(set(recent)) == 1:  # 三次相同的行动
                raise ValueError("检测到循环，强制终止")
```

**3. 分析根因**
- 工具返回结果格式不匹配，导致 Agent 误以为需要重试
- 工具描述不够清晰，Agent 不知道何时停止使用该工具
- 温度参数过高（>0.5），LLM 决策不稳定

> 💡 **面试话术：** "Agent 循环的根本原因不是 LLM 能力问题，而是'反馈循环设计'问题——工具返回的信息没有帮助 LLM 判断任务是否完成。解决方法是让每个工具在返回结果时同时给出'完成度评估'。"

---

### Q11：线上 LangChain 服务响应延迟高，如何做性能优化？
**面试官意图：** 考察性能优化思维。

**完美解答：**

**延迟分析（先定位瓶颈）：**

| 环节 | 典型延迟 | 是否可优化 | 优化手段 |
|------|---------|-----------|---------|
| Embedding | 50-200ms | 是 | GPU 推理、缓存、batch 处理 |
| 向量检索 | 5-50ms | 是 | 索引调优、减少 TopK |
| LLM 推理 | 1-10s | 是 | 量化、流式、小模型 |
| 工具调用 | 取决于工具 | 是 | 合并请求、缓存结果 |
| 网络 | 10-50ms | 一般 | 就近部署、连接池 |

**分层优化策略：**

**1. LLM 层（最大瓶颈）**
```python
# 量化模型加载
llm = ChatOllama(
    model="qwen2:7b",
    num_predict=512,       # 限制最大 token 数
    temperature=0.1,
    top_k=10,              # 减少候选 token 数
    num_ctx=2048           # 限制上下文窗口
)
```

**2. Agent 层**
```python
# 减少 Agent 迭代次数
agent_executor = AgentExecutor(
    max_iterations=3,       # 从默认的 6 减到 3
    early_stopping_method="generate"
)

# 工具结果缓存
@tool
@cache(ttl=3600)
def search_database(query: str):
    """数据库查询（结果缓存 1 小时）"""
    ...
```

**3. 服务架构层**
```python
# 连接池配置
embeddings = HuggingFaceBgeEmbeddings(
    model_name="BAAI/bge-large-zh-v1.5",
    encode_kwargs={"batch_size": 32}  # 批量处理
)

# 异步处理
async for chunk in rag_chain.astream({"query": query}):
    yield chunk
```

**优化效果：** P95 延迟从 8.2s 降到 2.1s，降幅 74%。

---

### Q12：如何评估你的 LangChain RAG/Agent 系统的效果？
**面试官意图：** 考察评估体系的建立能力。

**完美解答：**

**评估体系分为三层：**

**第一层：离线评估（接入前）**
```python
from langchain.evaluation import EvaluatorType, load_evaluator

# 1. RAG 评估（RAGAS 框架）
from ragas import evaluate
from ragas.metrics import (
    faithfulness,    # 忠实度：答案是否被检索内容支持
    answer_relevancy, # 相关性：答案是否与问题相关
    context_recall,  # 召回率：检索内容是否覆盖了答案所需信息
    context_precision # 精确度：检索内容是否都是必要的
)

# 2. Agent 评估
evaluator = load_evaluator(EvaluatorType.TRAJECTORY)
# 评分 Agent 的决策路径是否合理
```

**第二层：在线评估（接入后）**
- **用户反馈采集：** 点赞/点踩按钮，收集真实用户评价
- **A/B 测试：** 对照组（旧策略）vs 实验组（新策略）
- **关键指标看板：** 回答接受率、用户满意度、平均对话轮数

**第三层：Bad Case 驱动优化**
```python
# 收集 Bad Case
bad_cases = qa_logs.query("feedback == -1")

# 分析 Bad Case 模式
for case in bad_cases:
    # 问题过短 → Query 改写
    if len(case.question) < 5:
        print(f"需要 Query 改写: {case.question}")
    # 检索结果相似度低 → 优化分块/换模型
    if case.avg_similarity < 0.5:
        print(f"检索质量差: {case.question}")
```

> 🎯 **总结：** 没有评估就没有优化。我构建了"离线 RAGAS → 在线 AB 测试 → Bad Case 驱动迭代"的完整评估闭环，让优化有数据依据，而不是凭感觉调参。

---

## 💎 面试加分金句

- "LangChain 的本质不是框架，而是 LLM 应用的'编排范式'——它将 AI 应用拆解为模型、提示词、检索、记忆、工具、链六个可组合的抽象层。"
- "LCEL 最优雅的设计是让'流式支持'和'异步调用'变成默认行为，而不用开发者额外处理——这在生产环境中至关重要。"
- "我的原则是：Agent 尽可能少迭代，RAG 尽可能多召回。Agent 每次迭代都有 LLM 调用成本，而 RAG 检索成本低得多，所以应该用检索代替 Agent 的思考。"
- "LangChain 最大的坑不是技术，而是过度抽象——很多场景用不到 Agent、用不到复杂的链，一个简单的 Prompt + 检索就足够了。先验证再上复杂度。"
- "在生产环境中，我把 LangChain 当作'AI 中间件'——它是模型无关层、向量库无关层，让我们可以随时切换底层模型和存储而不影响业务代码。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| LangChain 0.3.x 版本的核心变更？ | 弃用 legacy Chain API，全面转向 LCEL；AgentExecutor 被 `create_react_agent` 替代 |
| LangChain 和 LlamaIndex 的选型差异？ | LangChain 强在 Agent 和 Chain 编排；LlamaIndex 强在数据索引和检索 |
| 如何调试 LangChain 链？ | `langchain.debug=True` 开启详细日志；LangSmith 可视化追踪 |
| LangChain 支持哪些模型提供商？ | 30+ 种：OpenAI、Anthropic、Ollama、HuggingFace、百度文心、通义千问等 |
| 什么情况下不用 LangChain？ | 只需要单次 LLM 调用时，直接调用 API 比引入 LangChain 更轻量 |
| ChatPromptTemplate 和 PromptTemplate 区别？ | ChatPromptTemplate 适用于聊天模型，支持 system/user/assistant 消息角色 |

## 🔗 关联知识点

- [RAG必做项目清单-面试问答](./RAG必做项目清单-面试问答.md)
- [Embedding必做项目清单-面试问答](./Embedding必做项目清单-面试问答.md)
- [LangGraph必做项目-面试问答](./LangGraph必做项目-面试问答.md)
- [Chroma必做项目清单-面试问答](./Chroma必做项目清单-面试问答.md)
- [Milvus必做项目清单-面试问答](./Milvus必做项目清单-面试问答.md)
