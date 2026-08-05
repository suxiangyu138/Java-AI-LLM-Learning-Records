# LangChain全套 面试宝典
> 基于尚硅谷2026版120集LangChain全套教程大纲，从零基础到项目实战，覆盖LangChain 1.x架构、模型调用、工具调用、Agent、中间件、Hook、Memory、RAG全链路及企业级项目面试考点

## 目录
1. [一、基础概念速答](#一基础概念速答15-20题)
2. [二、深度原理剖析](#二深度原理剖析10-15题)
3. [三、实战场景题](#三实战场景题8-12题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1.1 LangChain 是什么？
LangChain 是一个用于构建 LLM 驱动应用的**开发框架**，提供标准化的接口和组件来编排 Prompt、模型调用、工具调用、记忆管理、Agent 和 RAG 等能力。2026年最新的 LangChain 1.x 版本在架构上全面采用 **Runnable 协议**和**管道化设计**，组件间通过管道符 `|` 串联。

### 1.2 LangChain 的四大支柱是什么？
| 支柱 | 说明 |
|------|------|
| **LangChain** | 核心框架：模型调用、Prompt 管理、Agent、Memory、RAG 等 |
| **LangSmith** | LLM 应用全链路可观测平台：调试、测试、评估、监控 |
| **LangServe** | 将 LangChain 应用部署为 REST API 的标准方式 |
| **LangGraph** | 构建有状态、多 Actor 的 Agent 工作流（支持循环和分支） |

### 1.3 LangChain 1.x 主要模块有哪些？
| 模块 | 作用 |
|------|------|
| **Model I/O** | Chat Models / LLMs / Embeddings 的统一接口 |
| **Prompt Templates** | ChatPromptTemplate、MessagePlaceholder 等 |
| **Tool Calling** | @tool 装饰器、ToolSchema、tool_choice |
| **Output Parsers** | 结构化输出：Pydantic / TypedDict / JSON Schema / dataclass |
| **Memory** | 短期/长期记忆：BufferMemory / SummaryMemory / VectorStoreMemory |
| **Agent** | 智能体：绑定工具、调用模型、结构化输出、错误处理 |
| **Middleware** | 中间件：Summarization / HumanInTheLoop / PII / Fallback 等 |
| **Callbacks / Hooks** | 钩子函数：装饰器定义 / 类定义 / wrap_model_call / wrap_tool_call |
| **RAG** | 文档加载、切分、嵌入、向量存储、检索生成全流程 |

### 1.4 什么是 ChatPromptTemplate？它有哪些实例化方式？
ChatPromptTemplate 是 LangChain 中用于构建**多轮对话消息**的提示模板，支持 SystemMessage、HumanMessage、AIMessage 等多种消息角色。

```python
from langchain_core.prompts import ChatPromptTemplate

# 方式1：从模板字符串创建
prompt = ChatPromptTemplate.from_template("Tell me a {topic} joke")

# 方式2：从消息列表创建（推荐）
prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant."),
    ("human", "Hello, I need help with {topic}"),
])

# 方式3：部分变量预填充
prompt = ChatPromptTemplate.from_template("Hello {name}, today is {day}")
partial_prompt = prompt.partial(day="Monday")
result = partial_prompt.invoke({"name": "Alice"})
```

### 1.5 什么是消息占位符（MessagePlaceholder）？
MessagePlaceholder 用于在 Prompt 模板中**动态插入多轮消息**，典型场景是拼接对话历史。

```python
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant."),
    MessagesPlaceholder(variable_name="chat_history"),
    ("human", "{input}"),
])

# 调用时动态传入历史消息列表
result = prompt.invoke({
    "chat_history": [HumanMessage("Hello"), AIMessage("Hi!")],
    "input": "What is LangChain?",
})
```

### 1.6 Pydantic 在 LangChain 中的作用是什么？
Pydantic 是 LangChain 结构化输出的核心方案，用于定义**输出模型的结构和类型约束**。LangChain 通过 Pydantic 模型生成 JSON Schema，指导 LLM 输出符合结构的 JSON 数据。

```python
from pydantic import BaseModel, Field

class Person(BaseModel):
    name: str = Field(description="The person's name")
    age: int = Field(description="The person's age")
    email: str = Field(description="The person's email address")

# 在 with_structured_output 中使用
structured_llm = llm.with_structured_output(Person)
result: Person = structured_llm.invoke("Extract info: John is 30, john@example.com")
```

### 1.7 Agent 的四种结构化输出策略是什么？
| 策略 | 说明 | 适用场景 |
|------|------|---------|
| **Pydantic** | 基于 BaseModel 定义输出结构，类型安全 | 需要类型校验和 IDE 提示 |
| **TypedDict** | Python 原生类型注解方式 | 简单结构，无需校验 |
| **JSON Schema** | 原生 JSON Schema 描述 | 跨语言兼容性要求 |
| **@dataclass** | Python 标准库数据类 | 轻量级场景，无需额外依赖 |

### 1.8 LangChain 中间件有哪些分类？
中间件位于模型调用链路中，实现**横切关注点**的拦截与增强：

| 中间件 | 功能 |
|--------|------|
| **SummarizationMiddleware** | 长上下文自动摘要 |
| **HumanInTheLoopMiddleware** | 人工审核确认 |
| **PIIMiddleware** | 敏感信息脱敏 |
| **TodoListMiddleware** | 待办事项提取和管理 |
| **ModelCallLimitMiddleware** | 模型调用频率限制 |
| **ToolCallLimitMiddleware** | 工具调用次数限制 |
| **ModelFallbackMiddleware** | 模型故障切换 |
| **LLMToolSelectorMiddleware** | 工具选择优化 |
| **ToolRetryMiddleware** | 工具调用失败重试 |

### 1.9 Hook 函数的作用是什么？
Hook（钩子函数）在 LangChain 中用于**拦截模型调用和工具调用的各个生命周期阶段**，实现日志记录、性能监控、参数修改等需求。

- **Node-style 钩子**：通过装饰器或继承类定义，在 call 开始/结束时触发
- **wrap_model_call**：包装模型调用，注入额外逻辑
- **wrap_tool_call**：包装工具调用，拦截和修改工具执行

### 1.10 Memory 的分类有哪些？
| 记忆类型 | 说明 | 实现方式 |
|---------|------|---------|
| **短期记忆** | 会话内的消息缓存 | BufferMemory / BufferWindowMemory |
| **长期记忆** | 跨会话持久化 | PostgreSQL / Redis / 文件存储 |
| **摘要记忆** | 对长对话自动总结 | ConversationSummaryMemory |
| **向量记忆** | 基于语义相似度检索 | VectorStoreRetrieverMemory |
| **实体记忆** | 提取和存储实体信息 | ConversationEntityMemory |

### 1.11 什么是消息裁剪治理？
当对话历史过长超出 LLM 上下文窗口时，**裁剪治理**策略用于控制消息数量：

- **固定窗口裁剪**：保留最近 N 轮对话
- **Token 计数裁剪**：按 Token 数量截断
- **摘要压缩裁剪**：将旧消息总结为摘要替换
- **重要性评分裁剪**：保留关键消息，丢弃低价值消息

### 1.12 Milvus 向量数据库的特点是什么？
Milvus 是一个开源的**高性能向量数据库**，专门为 AI 应用设计：

| 特性 | 说明 |
|------|------|
| **分布式架构** | 支持水平扩展，分片和副本 |
| **多索引类型** | IVF_FLAT、HNSW、ANNOY 等 |
| **混合查询** | 向量相似度 + 标量过滤 |
| **Collection 管理** | Collection → Partition → Segment 层级 |
| **GPU 加速** | 支持 GPU 索引构建和查询 |
| **云原生** | 支持 K8s 部署、存算分离 |

### 1.13 文档加载器有哪些类型？
| 加载器 | 适用格式 |
|--------|---------|
| TextLoader | 纯文本文件 |
| PyPDFLoader | PDF 文档 |
| CSVLoader | CSV 表格数据 |
| JSONLoader | JSON 文件 |
| DirectoryLoader | 批量加载目录下所有文件 |
| WebBaseLoader | 网页内容 |
| UnstructuredFileLoader | 非结构化文档（DOCX、PPT 等） |
| SeleniumURLLoader | 需要 JS 渲染的网页 |

### 1.14 ChatOpenAI 兼容用法是什么意思？
很多国产大模型（DeepSeek、智谱、阿里云百炼）都支持**OpenAI 兼容接口**，LangChain 通过 `ChatOpenAI` 即可调用：

```python
from langchain_openai import ChatOpenAI

# 调用 DeepSeek
llm = ChatOpenAI(
    model="deepseek-chat",
    api_key="your-api-key",
    base_url="https://api.deepseek.com/v1",
)

# 调用智谱 GLM
llm = ChatOpenAI(
    model="glm-4",
    api_key="your-api-key",
    base_url="https://open.bigmodel.cn/api/paas/v4",
)

# 调用阿里云百炼 Qwen
llm = ChatOpenAI(
    model="qwen-plus",
    api_key="your-api-key",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)
```

### 1.15 model_kwargs 和 extra_body 参数的作用是什么？
| 参数 | 作用 | 使用场景 |
|------|------|---------|
| **model_kwargs** | 传递给模型的额外参数字典 | 控制 temperature、top_p、max_tokens 等 |
| **extra_body** | 请求体中额外的参数字段 | 厂商特定参数（如 DeepSeek 的 `enable_search`） |

```python
llm = ChatOpenAI(
    model="deepseek-chat",
    temperature=0.7,
    max_tokens=1024,
    model_kwargs={"top_p": 0.9},
    extra_body={"enable_search": True},  # DeepSeek 联网搜索
)
```

### 1.16 流式/批量/异步调用的区别？
```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(model="gpt-4")

# 1. 普通调用
result = llm.invoke("Tell me a story")

# 2. 流式调用
for chunk in llm.stream("Tell me a story"):
    print(chunk.content, end="", flush=True)

# 3. 批量调用（并行）
results = llm.batch(["Tell me a joke", "Tell me a poem"])

# 4. 异步调用
result = await llm.ainvoke("Tell me a story")

# 5. 异步流式
async for chunk in llm.astream("Tell me a story"):
    print(chunk.content, end="", flush=True)
```

### 1.17 LangSmith 是什么？
LangSmith 是 LangChain 生态中的**LLM 应用可观测平台**，提供：

- **Tracing**：全链路追踪每次模型调用、工具调用、检索
- **Evaluation**：定义评估指标，自动化测试 Prompt 效果
- **Dataset Management**：管理测试数据集
- **Monitoring**：生产环境监控延迟、Token 消耗、错误率
- **Hub**：共享和版本管理 Prompt 模板

### 1.18 消息的两种格式是什么？
LangChain 中消息（Message）主要分为两种格式：

| 格式 | 说明 | 示例 |
|------|------|------|
| **BaseMessage** | 对象格式，包含 content 和 role | `HumanMessage("Hello")` |
| **Tuple 格式** | (role, content) 元组 | `("human", "Hello")` |

两者在 PromptTemplate 中可互换使用，但 BaseMessage 支持更丰富的属性（如 tool_calls、additional_kwargs）。

### 1.19 @tool 装饰器的基本用法？
```python
from langchain_core.tools import tool

@tool
def get_weather(location: str, unit: str = "celsius") -> str:
    """Get the current weather for a location."""
    # 实际调用天气 API
    return f"Weather in {location}: 25 degrees {unit}"

# 工具会自动生成 JSON Schema
print(get_weather.name)        # "get_weather"
print(get_weather.description) # "Get the current weather for a location."
print(get_weather.args)        # {"location": {...}, "unit": {...}}
```

### 1.20 tool_choice 参数的作用是什么？
`tool_choice` 控制 LLM 是否调用工具以及调用哪个工具：

| 取值 | 行为 |
|------|------|
| `"auto"`（默认） | LLM 自主决定是否调用工具 |
| `"any"` | 强制调用某个工具 |
| `"none"` | 禁止调用任何工具 |
| `"ToolName"` | 强制调用指定名称的工具 |

---

## 二、深度原理剖析（10-15题）

### 2.1 LangChain 核心架构：Runnable 协议和管道化设计
LangChain 1.x 核心抽象是 **Runnable 协议**（`Runnable` 接口），所有组件都实现该协议，通过 `|` 操作符进行管道化编排。

```python
from langchain_core.runnables import RunnableSequence

# 管道化设计
chain = (
    ChatPromptTemplate.from_messages([
        ("system", "You are a helpful assistant."),
        ("human", "{input}"),
    ])
    | llm
    | StrOutputParser()
)

# 等价于显式序列
chain = RunnableSequence(
    ChatPromptTemplate.from_messages([...]),
    llm,
    StrOutputParser(),
)
```

**核心接口方法**：
- `invoke(input)` — 同步调用
- `stream(input)` — 流式调用
- `batch(inputs)` — 批量调用
- `ainvoke(input)` / `astream(input)` / `abatch(inputs)` — 异步版本

每个 Runnable 组件**输入和输出**类型明确，管道自动进行类型适配和转换。

### 2.2 工具调用完整流程
```
用户请求 → LLM 判断需要调用工具 → 生成工具调用参数 → 执行工具 → 返回结果 → LLM 整合最终回答
```

**详细流程分解**：

1. **Schema 生成**：`@tool` 装饰器解析函数签名和文档字符串，自动生成 JSON Schema（参数名、类型、描述、是否必填）
2. **工具绑定**：`llm.bind_tools([tool1, tool2])` 将工具 Schema 注入到 LLM 请求中
3. **模型选择执行**：LLM 分析用户意图，决定调用哪个工具并生成参数 JSON
4. **参数解析**：LangChain 将 LLM 返回的 tool_call 解析为具体的函数参数
5. **工具执行**：调用实际函数，获取结果
6. **结果整合**：将工具结果返回给 LLM，LLM 整合后生成最终回答

```python
from langchain_core.tools import tool

@tool
def search(query: str) -> str:
    """Search the web for information."""
    return f"Search results for: {query}"

# 绑定工具
llm_with_tools = llm.bind_tools([search])

# 调用过程（内部自动完成）
response = llm_with_tools.invoke("What is the weather in Beijing?")
```

### 2.3 结构化输出四种模式对比
```python
from pydantic import BaseModel, Field
from typing import TypedDict, List
from dataclasses import dataclass

# 方案1: Pydantic（推荐 — 类型安全、校验强）
class MovieReview(BaseModel):
    title: str = Field(description="Movie title")
    rating: float = Field(ge=0, le=10, description="Rating out of 10")
    summary: str = Field(description="Brief summary")

# 方案2: TypedDict（轻量、无需导入 Pydantic）
class MovieReviewDict(TypedDict):
    title: str
    rating: float
    summary: str

# 方案3: JSON Schema（跨语言兼容）
json_schema = {
    "type": "object",
    "properties": {
        "title": {"type": "string"},
        "rating": {"type": "number", "minimum": 0, "maximum": 10},
        "summary": {"type": "string"},
    },
    "required": ["title", "rating", "summary"],
}

# 方案4: @dataclass（Python 原生，无校验）
@dataclass
class MovieReviewData:
    title: str
    rating: float
    summary: str

# 使用方式
structured_llm = llm.with_structured_output(MovieReview)
result = structured_llm.invoke("Review the movie Inception")
```

**选择建议**：
| 方式 | 推荐场景 | 优势 | 劣势 |
|------|---------|------|------|
| Pydantic | 生产环境、复杂结构 | 校验强、嵌套支持好 | 额外依赖 |
| TypedDict | 简单快速原型 | 零依赖、轻量 | 无运行时校验 |
| JSON Schema | 多语言系统 | 通用性最强 | 无类型提示 |
| @dataclass | 已有大量 dataclass 的代码 | Python 标准 | 无校验、无字段描述 |

### 2.4 中间件设计模式
LangChain 中间件采用**责任链设计模式**，请求在中间件链中依次经过拦截、处理、响应三个阶段。

```python
from langchain_core.middleware import BaseMiddleware

# 中间件分类示意
# -----------------------------------------------------------------------
# 输入阶段中间件：     SummarizationMiddleware → PIIMiddleware
# 调用阶段中间件：     ModelCallLimitMiddleware → ToolCallLimitMiddleware
# 输出阶段中间件：     ModelFallbackMiddleware
# 混合中间件：         HumanInTheLoopMiddleware
# 工具相关中间件：     ToolRetryMiddleware → LLMToolSelectorMiddleware
# -----------------------------------------------------------------------

# 中间件优先级排序逻辑
# 1. 按 priority 属性排序（数值越小优先级越高）
# 2. 输入阶段：优先级高的先执行
# 3. 输出阶段：优先级高的后执行（责任链反向）
```

**责任链模式实现要点**：
- 每个中间件实现 `__call__` 方法，接收 `(messages, call_options)`
- 调用 `next(messages, call_options)` 将请求传递给下一个中间件
- 可以在请求前和响应后分别插入逻辑

### 2.5 Hook 机制深度解析
Hook 机制允许在不修改核心调用逻辑的情况下，在模型/工具调用的**各个生命周期点**插入自定义逻辑。

```python
# 方式1：装饰器定义 Node-style 钩子
from langchain_core.callbacks import callback_manager

@callback_manager.on_model_start
def log_model_start(serialized, messages, **kwargs):
    print(f"Model called with {len(messages)} messages")

@callback_manager.on_model_end
def log_model_end(response, **kwargs):
    print(f"Model returned: tokens={response.usage_metadata}")

# 方式2：基于类定义 Node-style 钩子
from langchain_core.callbacks import BaseCallbackHandler

class LoggingHandler(BaseCallbackHandler):
    def on_llm_start(self, serialized, prompts, **kwargs):
        print(f"[LLM START] Prompts: {len(prompts)}")

    def on_llm_end(self, response, **kwargs):
        print(f"[LLM END] Generation: {response.generations[0][0].text[:50]}...")

    def on_tool_start(self, serialized, input_str, **kwargs):
        print(f"[TOOL START] Tool: {serialized['name']}")

    def on_tool_end(self, output, **kwargs):
        print(f"[TOOL END] Output: {str(output)[:50]}...")

# 方式3：wrap_model_call
from langchain_core.hooks import wrap_model_call

@wrap_model_call
def monitored_model_call(original_call, *args, **kwargs):
    print("Before model call")
    result = original_call(*args, **kwargs)
    print("After model call")
    return result

# 方式4：wrap_tool_call
from langchain_core.hooks import wrap_tool_call

@wrap_tool_call
def monitored_tool_call(original_call, *args, **kwargs):
    print(f"Before tool call: {args}")
    result = original_call(*args, **kwargs)
    print(f"After tool call: {result}")
    return result
```

**Hook 执行顺序**：
```
模型调用开始
  ├── on_model_start 钩子 (注册顺序执行)
  ├── 实际模型调用
  └── on_model_end 钩子 (注册顺序执行)
```

### 2.6 记忆管理全体系
LangChain 记忆管理采用**分层缓存架构**，从短期到长期形成完整体系。

```
短期记忆 (BufferMemory)
    ↓ 会话结束持久化
中期记忆 (SummaryMemory / BufferWindowMemory)
    ↓ 定期摘要归档
长期记忆 (PostgreSQL / Redis / Vector)
    ↓ 语义检索
向量记忆 (VectorStoreRetrieverMemory)
```

```python
from langchain.memory import ConversationBufferMemory
from langchain.memory import ConversationSummaryMemory
from langchain.memory import VectorStoreRetrieverMemory
from langchain_postgres import PostgresChatMessageHistory

# 1. 短期记忆 — Buffer
memory = ConversationBufferMemory()
memory.chat_memory.add_user_message("Hello")
memory.chat_memory.add_ai_message("Hi, how can I help?")

# 2. 摘要记忆 — Summary
summary_memory = ConversationSummaryMemory(llm=llm)
summary_memory.save_context({"input": "Hello"}, {"output": "Hi!"})
summary = summary_memory.load_memory_variables({})

# 3. 向量记忆 — Semantic Retrieval
vector_memory = VectorStoreRetrieverMemory(
    vectorstore=vector_store,
    memory_key="relevant_history",
    k=3,
)

# 4. PostgreSQL 持久化
from langchain_postgres import PostgresChatMessageHistory

history = PostgresChatMessageHistory(
    connection_string="postgresql://user:pass@localhost:5432/memory_db",
    session_id="user_session_123",
)

history.add_user_message("Hello")
history.add_ai_message("How can I help you today?")
```

### 2.7 RAG 完整 Pipeline
```
原始文档 → 文档加载 → 文档切分 → 嵌入向量化 → 向量存储 → 用户查询 → 检索 TopK → 重排序 → 注入 Prompt → 生成回答
  Load       Split       Embed        Store        Retrieve     Rerank       Generate
```

```python
# 完整 RAG 流程
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Milvus
from langchain_core.runnables import RunnablePassthrough

# 1. 加载文档
loader = TextLoader("knowledge_base.txt")
documents = loader.load()

# 2. 切分文档
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=50,
    separators=["\n\n", "\n", "。", " ", ""],
)
docs = text_splitter.split_documents(documents)

# 3. 嵌入 + 存储
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Milvus.from_documents(
    documents=docs,
    embedding=embeddings,
    connection_args={"host": "localhost", "port": "19530"},
)

# 4. 检索器
retriever = vectorstore.as_retriever(search_kwargs={"k": 5})

# 5. 构建 RAG Chain
template = """Answer the question based on the context below:
{context}

Question: {question}
"""
prompt = ChatPromptTemplate.from_template(template)

def format_docs(docs):
    return "\n\n".join(doc.page_content for doc in docs)

rag_chain = (
    {"context": retriever | format_docs, "question": RunnablePassthrough()}
    | prompt
    | llm
    | StrOutputParser()
)

# 6. 执行查询
result = rag_chain.invoke("What is LangChain?")
```

### 2.8 Milvus 向量数据库核心概念
| 概念 | 说明 | 类比关系型数据库 |
|------|------|-----------------|
| Collection | 数据集，Schema 定义字段和向量维度 | 表（Table） |
| Partition | Collection 的分区，水平切分数据 | 分区表（Partition） |
| Index | 加速检索的数据结构 | 索引（Index） |
| Segment | 数据存储的最小物理单元 | 数据块 |
| Alias | Collection 的别名 | 视图别名 |
| Metric Type | 距离度量方式（IP/L2/COSINE） | — |

**Milvus 集成示例**：
```python
from langchain_community.vectorstores import Milvus

# 创建/连接 Collection
vectorstore = Milvus(
    embedding_function=embeddings,
    collection_name="knowledge_base",
    connection_args={"host": "localhost", "port": "19530"},
    index_params={
        "metric_type": "COSINE",
        "index_type": "HNSW",
        "params": {"M": 8, "efConstruction": 200},
    },
)

# 插入文档
vectorstore.add_documents(docs)

# 相似度检索
results = vectorstore.similarity_search_with_score(
    "What is RAG?",
    k=5,
    score_threshold=0.7,
)
```

---

## 三、实战场景题（8-12题）

### 3.1 LangChain 模型调用（多种模型/流式/批量/异步）
```python
from langchain_openai import ChatOpenAI
from langchain_deepseek import ChatDeepSeek
from langchain_core.messages import HumanMessage, SystemMessage

# OpenAI 模型
gpt_llm = ChatOpenAI(
    model="gpt-4o",
    temperature=0.7,
    max_tokens=2048,
)

# DeepSeek 模型
deepseek_llm = ChatDeepSeek(
    model="deepseek-chat",
    temperature=0.3,
    extra_body={"enable_search": True},  # 联网搜索
)

# 多模型流式输出
def stream_with_fallback(prompt: str, models: list):
    for model in models:
        try:
            for chunk in model.stream(prompt):
                yield chunk.content
            break  # 成功则停止
        except Exception as e:
            print(f"Model failed: {e}")
            continue

# 批量调用处理
async def batch_process(questions: list[str]):
    results = await gpt_llm.abatch([
        [HumanMessage(content=q)] for q in questions
    ])
    return [r.content for r in results]
```

### 3.2 @tool 装饰器定义工具 + 工具调用
```python
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI
from typing import Optional

@tool
def calculate(expression: str) -> str:
    """Evaluate a mathematical expression."""
    try:
        result = eval(expression)
        return f"Result: {result}"
    except Exception as e:
        return f"Error: {str(e)}"

@tool
def search_knowledge_base(query: str, top_k: Optional[int] = 3) -> str:
    """Search the knowledge base for relevant information."""
    # 模拟知识库检索
    results = [f"Result {i}: info about {query}" for i in range(top_k)]
    return "\n".join(results)

# 绑定多工具
llm_with_tools = llm.bind_tools([calculate, search_knowledge_base])

# 工具调用执行
def call_with_tool_execution(query: str):
    response = llm_with_tools.invoke(query)

    if response.tool_calls:
        for tool_call in response.tool_calls:
            tool_name = tool_call["name"]
            tool_args = tool_call["args"]

            # 根据名称分发
            if tool_name == "calculate":
                result = calculate.invoke(tool_args)
            elif tool_name == "search_knowledge_base":
                result = search_knowledge_base.invoke(tool_args)
            print(f"Tool {tool_name} returned: {result}")
    else:
        print(f"Direct response: {response.content}")
```

### 3.3 Agent + 结构化输出
```python
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

class FinalAnswer(BaseModel):
    answer: str = Field(description="Final answer to the user question")
    sources: list[str] = Field(description="Sources used for the answer")
    confidence: float = Field(ge=0, le=1, description="Confidence score")

# 定义 Agent
prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a research assistant. Use tools to gather info."),
    ("placeholder", "{chat_history}"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}"),
])

agent = create_tool_calling_agent(llm, tools=[search, calculate], prompt=prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

# 带结构化输出的 Agent
structured_agent = agent_executor.with_structured_output(FinalAnswer)
result: FinalAnswer = structured_agent.invoke({
    "input": "What is the capital of France and its population?"
})
print(f"Answer: {result.answer}")
print(f"Sources: {result.sources}")
print(f"Confidence: {result.confidence}")
```

### 3.4 中间件编写（Summarization / HumanInTheLoop）
```python
from langchain_core.middleware import BaseMiddleware
from langchain_core.messages import HumanMessage, AIMessage

# SummarizationMiddleware — 长上下文自动摘要
class SummarizationMiddleware(BaseMiddleware):
    priority = 100

    def __init__(self, llm, max_messages=10):
        self.llm = llm
        self.max_messages = max_messages

    def __call__(self, messages, call_options, next):
        if len(messages) > self.max_messages:
            # 对早期消息进行摘要
            to_summarize = messages[:-self.max_messages]
            summary_prompt = f"Summarize the following conversation:\n{to_summarize}"
            summary = self.llm.invoke(summary_prompt)

            # 用摘要替换早期消息
            messages = [HumanMessage(f"Previous summary: {summary.content}")] + messages[-self.max_messages:]

        return next(messages, call_options)

# HumanInTheLoopMiddleware — 人工审核
class HumanInTheLoopMiddleware(BaseMiddleware):
    priority = 50  # 高优先级

    def __call__(self, messages, call_options, next):
        # 高风险请求需确认
        content = messages[-1].content if messages else ""
        if any(word in content for word in ["delete", "drop", "shutdown"]):
            confirmed = input(f"Confirm action? (y/n): {content[:100]}...")
            if confirmed.lower() != "y":
                return AIMessage(content="Operation cancelled by human.")

        return next(messages, call_options)

# 使用中间件
from langchain_core.middleware import MiddlewareStack

middleware_stack = MiddlewareStack([
    SummarizationMiddleware(llm, max_messages=10),
    HumanInTheLoopMiddleware(),
])

llm_with_middleware = middleware_stack.wrap(llm)
result = llm_with_middleware.invoke("Tell me about AI")
```

### 3.5 Hook 函数实现日志记录
```python
from langchain_core.callbacks import BaseCallbackHandler
import time

class LoggingCallbackHandler(BaseCallbackHandler):
    def __init__(self):
        self.start_time = None

    def on_llm_start(self, serialized, prompts, **kwargs):
        self.start_time = time.time()
        print(f"[{time.strftime('%H:%M:%S')}] LLM 调用开始")
        print(f"    消息数量: {len(prompts)}")

    def on_llm_end(self, response, **kwargs):
        elapsed = time.time() - self.start_time
        generation = response.generations[0][0]
        tokens = response.llm_output.get("token_usage", {})
        print(f"[{time.strftime('%H:%M:%S')}] LLM 调用完成")
        print(f"    耗时: {elapsed:.2f}s")
        print(f"    Token: 输入={tokens.get('prompt_tokens', 'N/A')}, "
              f"输出={tokens.get('completion_tokens', 'N/A')}")
        print(f"    内容预览: {generation.text[:100]}...")

    def on_llm_error(self, error, **kwargs):
        print(f"[ERROR] LLM 调用失败: {error}")

    def on_tool_start(self, serialized, input_str, **kwargs):
        print(f"[工具调用] {serialized.get('name', 'unknown')}")

    def on_tool_end(self, output, **kwargs):
        print(f"[工具结果] {str(output)[:100]}...")

# 调用时传入
handler = LoggingCallbackHandler()
result = llm.invoke("Hello", config={"callbacks": [handler]})
```

### 3.6 PostgreSQL Memory 持久化
```python
from langchain_postgres import PostgresChatMessageHistory
from langchain.memory import ConversationBufferMemory
from langchain.schema import BaseMessage

# 连接 PostgreSQL
history = PostgresChatMessageHistory(
    connection_string="postgresql+psycopg2://user:password@localhost:5432/chat_memory",
    session_id="user_session_456",
    table_name="chat_history",  # 自定义表名
)

# 添加消息
history.add_message(HumanMessage(content="Hello"))
history.add_message(AIMessage(content="Hi! How can I help?"))

# 构建带持久化的 Memory
memory = ConversationBufferMemory(
    chat_memory=history,
    return_messages=True,
    memory_key="chat_history",
)

# 在 Chain 中使用
chain = (
    ChatPromptTemplate.from_messages([
        ("system", "You are a helpful assistant."),
        MessagesPlaceholder(variable_name="chat_history"),
        ("human", "{input}"),
    ])
    | llm
    | StrOutputParser()
)

def chat_with_memory(user_input: str):
    # 获取历史
    history_vars = memory.load_memory_variables({})
    result = chain.invoke({
        "chat_history": history_vars.get("chat_history", []),
        "input": user_input,
    })
    # 保存到数据库
    memory.save_context({"input": user_input}, {"output": result})
    return result
```

### 3.7 RAG 完整项目：Assistant 客服知识库
```python
import os
from langchain_community.document_loaders import DirectoryLoader, TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_community.vectorstores import Milvus
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser

# ============ 1. 数据准备 ============
loader = DirectoryLoader(
    "knowledge_docs/",
    glob="**/*.md",
    loader_cls=TextLoader,
    show_progress=True,
)
docs = loader.load()
print(f"Loaded {len(docs)} documents")

# ============ 2. 文档切分 ============
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=300,
    chunk_overlap=30,
    separators=["\n## ", "\n### ", "\n", ".", " "],
)
chunks = text_splitter.split_documents(docs)
print(f"Split into {len(chunks)} chunks")

# ============ 3. 向量化+存储 ============
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Milvus(
    embedding_function=embeddings,
    collection_name="assistant_kb",
    connection_args={"host": "localhost", "port": "19530"},
)
vectorstore.add_documents(chunks)

# ============ 4. 检索器 ============
retriever = vectorstore.as_retriever(
    search_type="similarity",  # 或 mmr 增加多样性
    search_kwargs={"k": 4, "fetch_k": 10},
)

# ============ 5. Prompt 优化 ============
prompt = ChatPromptTemplate.from_messages([
    ("system", """You are a customer service assistant for a tech company.
Answer user questions based ONLY on the context below.
If the context doesn't contain enough info, say "I don't have enough information".
Always cite the source document name.

Context:
{context}"""),
    MessagesPlaceholder(variable_name="chat_history", optional=True),
    ("human", "{question}"),
])

# ============ 6. 构建完整 Chain ============
def format_docs(docs):
    return "\n\n---\n".join(
        f"Source: {doc.metadata.get('source', 'unknown')}\n{doc.page_content}"
        for doc in docs
    )

rag_chain = (
    {
        "context": retriever | format_docs,
        "question": RunnablePassthrough(),
        "chat_history": lambda _: [],
    }
    | prompt
    | ChatOpenAI(model="gpt-4o", temperature=0.3)
    | StrOutputParser()
)

# ============ 7. 查询接口 ============
def ask(question: str):
    return rag_chain.invoke(question)

# 测试
print(ask("How do I reset my password?"))
```

### 3.8 Milvus 向量数据库集成
```python
from langchain_community.vectorstores import Milvus
from langchain_openai import OpenAIEmbeddings

embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

# 方式1：从文档创建
vectorstore = Milvus.from_documents(
    documents=chunks,
    embedding=embeddings,
    collection_name="my_collection",
    connection_args={
        "host": "localhost",
        "port": "19530",
        "user": "default",
        "password": "",
        "secure": False,
    },
    index_params={
        "metric_type": "COSINE",
        "index_type": "HNSW",
        "params": {"M": 16, "efConstruction": 500},
    },
    drop_old=True,
)

# 方式2：连接已有 Collection
vectorstore = Milvus(
    embedding_function=embeddings,
    collection_name="my_collection",
    connection_args={"host": "localhost", "port": "19530"},
)

# 混合查询（向量 + 标量过滤）
results = vectorstore.similarity_search(
    query="How to configure LangChain?",
    k=5,
    expr="source == 'documentation.md'",  # 标量过滤
)

# 带分数检索
results_with_score = vectorstore.similarity_search_with_score(
    query="Agent tutorial",
    k=3,
)
for doc, score in results_with_score:
    print(f"Score: {score:.4f}, Content: {doc.page_content[:50]}...")
```

### 3.9 消息裁剪与摘要策略
```python
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage, trim_messages
from langchain.memory import ConversationSummaryMemory

# 策略1：固定窗口裁剪 — 只保留最后 N 轮
def trim_by_window(messages: list, max_pairs: int = 5):
    """保留最近 N 轮对话（1轮 = 1 Human + 1 AI）"""
    if len(messages) <= max_pairs * 2:
        return messages

    # 确保 SystemMessage 保留
    system_msgs = [m for m in messages if isinstance(m, SystemMessage)]
    recent_msgs = messages[-(max_pairs * 2):]
    return system_msgs + recent_msgs

# 策略2：Token 计数裁剪
from langchain_core.messages import trim_messages

trimmed = trim_messages(
    messages,
    token_counter=llm.get_num_tokens_from_messages,
    max_tokens=4000,
    strategy="last",  # 保留最近的
    start_on="human",  # 从 Human 消息开始计数
    include_system=True,
)

# 策略3：摘要压缩裁剪
summary_memory = ConversationSummaryMemory(llm=llm)

def compress_with_summary(messages: list, max_messages: int = 6):
    if len(messages) <= max_messages:
        return messages, ""

    # 需要压缩的旧消息
    old_msgs = messages[:-max_messages]
    summary_memory.clear()
    for i in range(0, len(old_msgs) - 1, 2):
        if i+1 < len(old_msgs):
            summary_memory.save_context(
                {"input": old_msgs[i].content},
                {"output": old_msgs[i+1].content},
            )

    summary = summary_memory.load_memory_variables({})
    summary_msg = HumanMessage(f"[Conversation Summary]: {summary}")
    return [summary_msg] + messages[-max_messages:], summary
```

### 3.10 错误处理与 Fallback 机制
```python
from langchain_core.middleware import ModelFallbackMiddleware
from langchain_openai import ChatOpenAI

# 方案1：使用 Fallback 中间件
primary_llm = ChatOpenAI(model="gpt-4", timeout=10)
fallback_llm = ChatOpenAI(model="gpt-3.5-turbo", timeout=10)
tertiary_llm = ChatDeepSeek(model="deepseek-chat")

fallback_middleware = ModelFallbackMiddleware(
    models=[primary_llm, fallback_llm, tertiary_llm],
    max_retries=2,
    fallback_on_exception=True,
)

llm_with_fallback = fallback_middleware.wrap(primary_llm)

# 方案2：使用 .with_fallbacks()
from langchain_core.runnables import RunnableBranch

chain = (
    ChatPromptTemplate.from_template("Answer: {question}")
    | primary_llm.with_fallbacks([fallback_llm, tertiary_llm])
    | StrOutputParser()
)

# 方案3：自定义错误重试
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
def robust_llm_call(prompt: str):
    result = llm.invoke(prompt)
    if not result.content:
        raise ValueError("Empty response from LLM")
    return result

# ToolRetryMiddleware 用法
from langchain_core.middleware import ToolRetryMiddleware

@tool
def unstable_api(query: str) -> str:
    """An API that may fail occasionally."""
    import random
    if random.random() < 0.3:
        raise ConnectionError("API temporarily unavailable")
    return f"Result: {query}"

retry_middleware = ToolRetryMiddleware(
    max_retries=3,
    retry_delay=1.0,
    retry_on_exceptions=[ConnectionError, TimeoutError],
)
```

---

## 四、手写代码题（5-8题）

### 4.1 手写 ChatPromptTemplate
```python
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

# 1. 基础模板
def create_simple_prompt():
    prompt = ChatPromptTemplate.from_messages([
        ("system", "You are a {role} assistant."),
        ("human", "{input}"),
    ])
    return prompt

# 2. 带历史消息的模板
def create_chat_prompt_with_history():
    prompt = ChatPromptTemplate.from_messages([
        ("system", "You are a helpful assistant."),
        MessagesPlaceholder(variable_name="history"),
        ("human", "{input}"),
    ])
    return prompt

# 3. 部分变量预填充
def create_partial_prompt():
    prompt = ChatPromptTemplate.from_template("Hello {name}, your role is {role}")
    partial = prompt.partial(role="developer")
    return partial  # 只需传入 {name}

# 4. Few-shot 示例注入
def create_few_shot_prompt():
    examples = [
        ("human", "What is Java?"),
        ("ai", "Java is a programming language."),
        ("human", "What is Python?"),
        ("ai", "Python is a scripting language."),
    ]

    prompt = ChatPromptTemplate.from_messages([
        ("system", "Answer questions concisely."),
        *examples,
        ("human", "{input}"),
    ])
    return prompt
```

### 4.2 手写 @tool 装饰器
```python
from langchain_core.tools import tool, BaseTool
from typing import Optional, Type
from pydantic import BaseModel, Field

# 方式1：函数式工具
@tool
def calculator(expression: str) -> str:
    """Evaluate a mathematical expression and return the result."""
    try:
        result = eval(expression, {"__builtins__": {}}, {})
        return str(result)
    except Exception as e:
        return f"Error: {e}"

# 方式2：带类型和描述的复杂工具
@tool
def get_user_info(user_id: int, include_email: Optional[bool] = False) -> dict:
    """Get user information by user ID.

    Args:
        user_id: The unique identifier for the user.
        include_email: Whether to include the user's email address.
    """
    user_data = {"id": user_id, "name": "John Doe"}
    if include_email:
        user_data["email"] = "john@example.com"
    return user_data

# 方式3：基于类的工具（自定义 Schema）
class SearchInput(BaseModel):
    query: str = Field(description="The search query")
    max_results: int = Field(default=5, description="Max number of results")

class SearchTool(BaseTool):
    name: str = "search"
    description: str = "Search the web for information"
    args_schema: Type[BaseModel] = SearchInput

    def _run(self, query: str, max_results: int = 5) -> str:
        return f"Search results for '{query}' (max: {max_results})"
```

### 4.3 手写简单中间件
```python
from langchain_core.middleware import BaseMiddleware

# 1. 计时中间件
class TimingMiddleware(BaseMiddleware):
    priority = 500

    def __call__(self, messages, call_options, next):
        import time
        start = time.time()
        response = next(messages, call_options)
        elapsed = time.time() - start
        print(f"[Timing] 耗时: {elapsed:.2f}s, 消息数: {len(messages)}")
        return response

# 2. 内容过滤中间件
class ContentFilterMiddleware(BaseMiddleware):
    priority = 100  # 高优先级

    def __init__(self, blocked_words: list[str]):
        self.blocked_words = blocked_words

    def __call__(self, messages, call_options, next):
        for msg in messages:
            content = msg.content if hasattr(msg, 'content') else str(msg)
            for word in self.blocked_words:
                if word in content:
                    return AIMessage(
                        content=f"Request blocked: contains forbidden word '{word}'"
                    )
        return next(messages, call_options)

# 3. 缓存中间件
class CacheMiddleware(BaseMiddleware):
    priority = 300

    def __init__(self):
        self.cache = {}

    def __call__(self, messages, call_options, next):
        import json
        key = json.dumps([
            m.content if hasattr(m, 'content') else str(m)
            for m in messages
        ])
        if key in self.cache:
            print("[Cache] HIT")
            return self.cache[key]
        response = next(messages, call_options)
        self.cache[key] = response
        print("[Cache] MISS - cached")
        return response
```

### 4.4 手写 Hook 装饰器
```python
from functools import wraps
import time
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 1. 模型调用钩子装饰器
def monitor_model_call(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        logger.info(f"[模型调用] 开始调用...")
        start = time.time()
        try:
            result = func(*args, **kwargs)
            elapsed = time.time() - start
            logger.info(f"[模型调用] 完成，耗时: {elapsed:.2f}s")
            return result
        except Exception as e:
            logger.error(f"[模型调用] 失败: {e}")
            raise
    return wrapper

# 2. 工具调用钩子装饰器
def monitor_tool_call(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        logger.info(f"[工具调用] {func.__name__}, 参数: {args}, {kwargs}")
        start = time.time()
        result = func(*args, **kwargs)
        elapsed = time.time() - start
        logger.info(f"[工具调用] 完成，结果: {str(result)[:100]}, 耗时: {elapsed:.2f}s")
        return result
    return wrapper

# 3. 高级组合钩子
def with_retry(max_retries=3, delay=1.0):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_exception = None
            for attempt in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    last_exception = e
                    logger.warning(f"尝试 {attempt+1}/{max_retries} 失败: {e}")
                    if attempt < max_retries - 1:
                        time.sleep(delay * (attempt + 1))
            raise last_exception
        return wrapper
    return decorator

# 使用示例
@monitor_tool_call
@with_retry(max_retries=3)
def get_weather(city: str) -> str:
    # 模拟 API 调用
    return f"Weather in {city}: sunny, 25°C"
```

### 4.5 手写 Memory 持久化存储
```python
import json
import sqlite3
from datetime import datetime
from typing import Optional

class SQLiteChatMemory:
    """基于 SQLite 的对话记忆持久化"""

    def __init__(self, db_path: str = "chat_memory.db"):
        self.conn = sqlite3.connect(db_path)
        self._create_table()

    def _create_table(self):
        self.conn.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        self.conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_session
            ON messages(session_id, created_at)
        """)
        self.conn.commit()

    def add_message(self, session_id: str, role: str, content: str):
        self.conn.execute(
            "INSERT INTO messages (session_id, role, content) VALUES (?, ?, ?)",
            (session_id, role, content),
        )
        self.conn.commit()

    def get_history(self, session_id: str, limit: Optional[int] = None) -> list[dict]:
        query = """
            SELECT role, content, created_at FROM messages
            WHERE session_id = ?
            ORDER BY created_at ASC
        """
        if limit:
            query = f"""
                SELECT role, content, created_at FROM messages
                WHERE session_id = ? AND id > (
                    SELECT COALESCE(MAX(id) - {limit * 2}, 0) FROM messages
                    WHERE session_id = ?
                )
                ORDER BY created_at ASC
            """
            params = (session_id, session_id)
        else:
            params = (session_id,)

        rows = self.conn.execute(query, params).fetchall()
        return [
            {"role": row[0], "content": row[1], "timestamp": row[2]}
            for row in rows
        ]

    def clear_session(self, session_id: str):
        self.conn.execute("DELETE FROM messages WHERE session_id = ?", (session_id,))
        self.conn.commit()

    def close(self):
        self.conn.close()

# 使用
memory_store = SQLiteChatMemory("langchain_memory.db")
memory_store.add_message("session_1", "human", "Hello")
memory_store.add_message("session_1", "ai", "Hi there!")
history = memory_store.get_history("session_1")
```

### 4.6 手写 RAG Pipeline
```python
from typing import List
from dataclasses import dataclass

@dataclass
class Document:
    page_content: str
    metadata: dict

class SimpleRAG:
    """简易 RAG Pipeline"""

    def __init__(self, llm, embeddings):
        self.llm = llm
        self.embeddings = embeddings
        self.documents: List[Document] = []
        self.index = {}  # text_hash -> embedding

    def add_documents(self, documents: List[Document]):
        self.documents.extend(documents)
        # 计算并缓存嵌入向量
        for doc in documents:
            import hashlib
            doc_hash = hashlib.md5(doc.page_content.encode()).hexdigest()
            if doc_hash not in self.index:
                embedding = self.embeddings.embed_query(doc.page_content)
                self.index[doc_hash] = embedding

    def retrieve(self, query: str, k: int = 3) -> List[Document]:
        query_embedding = self.embeddings.embed_query(query)

        # 余弦相似度计算
        def cosine_sim(a, b):
            dot = sum(x * y for x, y in zip(a, b))
            norm_a = sum(x ** 2 for x in a) ** 0.5
            norm_b = sum(x ** 2 for x in b) ** 0.5
            return dot / (norm_a * norm_b + 1e-10)

        # 计算所有文档的相似度
        scored = []
        for doc in self.documents:
            import hashlib
            doc_hash = hashlib.md5(doc.page_content.encode()).hexdigest()
            if doc_hash in self.index:
                score = cosine_sim(query_embedding, self.index[doc_hash])
                scored.append((score, doc))

        # 取 TopK
        scored.sort(key=lambda x: x[0], reverse=True)
        return [doc for _, doc in scored[:k]]

    def generate(self, query: str, context_docs: List[Document]) -> str:
        context = "\n\n".join(d.page_content for d in context_docs)
        prompt = f"""Answer the question based on the context below.

Context:
{context}

Question: {query}

Answer:"""
        return self.llm.invoke(prompt)

    def query(self, query: str, k: int = 3) -> str:
        docs = self.retrieve(query, k)
        return self.generate(query, docs)

# 使用
rag = SimpleRAG(llm=llm, embeddings=embeddings)
rag.add_documents([
    Document("LangChain is a framework for building LLM apps.", {"source": "doc1"}),
    Document("RAG stands for Retrieval-Augmented Generation.", {"source": "doc2"}),
])
answer = rag.query("What is LangChain?")
```

### 4.7 手写 Pydantic 结构化输出模型
```python
from pydantic import BaseModel, Field, field_validator
from typing import List, Optional
from enum import Enum

class Sentiment(str, Enum):
    POSITIVE = "positive"
    NEGATIVE = "negative"
    NEUTRAL = "neutral"

class Entity(BaseModel):
    name: str = Field(description="Entity name")
    type: str = Field(description="Entity type: person, organization, location")
    sentiment: Sentiment = Field(description="Sentiment toward this entity")

class AnalysisResult(BaseModel):
    summary: str = Field(description="Text summary, max 200 chars")
    entities: List[Entity] = Field(description="Extracted entities")
    language: str = Field(default="zh-CN", description="Detected language code")
    word_count: int = Field(description="Word count of original text")

    @field_validator("summary")
    @classmethod
    def summary_not_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("summary cannot be empty")
        return v

    @field_validator("word_count")
    @classmethod
    def positive_word_count(cls, v: int) -> int:
        if v <= 0:
            raise ValueError("word_count must be positive")
        return v

# 使用 with_structured_output
def analyze_text(text: str) -> AnalysisResult:
    structured_llm = llm.with_structured_output(AnalysisResult)
    result = structured_llm.invoke(
        f"Analyze the following text:\n{text}"
    )
    return result

# 嵌套 Pydantic 模型
class CodeBlock(BaseModel):
    language: str = Field(description="Programming language")
    code: str = Field(description="Code content")
    explanation: str = Field(description="Explanation of the code")

class TechnicalAnswer(BaseModel):
    question: str = Field(description="Original question")
    answer: str = Field(description="Text answer")
    code_examples: List[CodeBlock] = Field(description="Code examples if applicable")
    difficulty: int = Field(ge=1, le=5, description="Difficulty level 1-5")
```

---

## 五、系统设计题（3-5题）

### 5.1 设计客服知识库系统
**需求分析**：
- 支持多种文档格式（PDF、Word、Markdown）导入
- 支持实时文档更新和索引刷新
- 支持会话历史记忆
- 支持多轮对话
- 高并发低延迟（<500ms）

**系统架构**：
```
[用户] → [API Gateway] → [Chat Service]
                              ├── [RAG Pipeline]
                              │    ├── Document Loader → Text Splitter
                              │    ├── Embedding Service → Milvus
                              │    └── Reranker → LLM
                              ├── [Memory Service → PostgreSQL]
                              ├── [Middleware Stack]
                              │    ├── PII Middleware
                              │    ├── Rate Limit Middleware
                              │    └── Fallback Middleware
                              └── [Monitoring → LangSmith]
```

**核心设计要点**：
| 组件 | 选型 | 考虑因素 |
|------|------|---------|
| 向量数据库 | Milvus | 支持高并发、分布式、标量过滤 |
| 嵌入模型 | text-embedding-3-small | 性价比高、1536维 |
| LLM | gpt-4o-mini | 延迟低、成本低、效果可接受 |
| 文档存储 | MinIO/S3 | 原始文档持久化 |
| 缓存 | Redis | 热点查询缓存 |
| 消息队列 | RabbitMQ/Kafka | 异步文档处理 |

**文档更新策略**：
1. 增量更新：监控文件变更，只重新索引变动文档
2. 定时全量重建：每天凌晨低峰期全量重建索引
3. 版本管理：保留历史版本，支持回滚

### 5.2 设计中间件编排系统
**设计目标**：提供一个可扩展的中间件编排平台，支持热插拔、优先级排序和动态配置。

```python
class MiddlewareOrchestrator:
    """中间件编排引擎"""

    def __init__(self):
        self.middlewares = []

    def register(self, middleware, priority: int = 500):
        """注册中间件，priority 越小优先级越高"""
        middleware.priority = priority
        self.middlewares.append(middleware)
        self.middlewares.sort(key=lambda m: m.priority)
        return self

    def unregister(self, middleware):
        """卸载中间件（热插拔）"""
        self.middlewares = [m for m in self.middlewares if m != middleware]
        return self

    def wrap(self, llm):
        """包装 LLM 实例"""
        from langchain_core.middleware import MiddlewareStack
        stack = MiddlewareStack(self.middlewares)
        return stack.wrap(llm)
```

**编排系统架构**：
```
[输入请求]
    ↓
[中间件编排引擎] — 按优先级排序执行
    ├── 安全组（priority: 100）
    │   ├── PIIMiddleware — 敏感信息脱敏
    │   └── ContentFilterMiddleware — 内容过滤
    ├── 控制组（priority: 200）
    │   ├── RateLimitMiddleware — 频率限制
    │   └── TokenBudgetMiddleware — Token预算控制
    ├── 增强组（priority: 300）
    │   ├── ContextAugmentationMiddleware — 上下文增强
    │   └── HistorySummarizationMiddleware — 历史摘要
    ├── 监控组（priority: 400）
    │   ├── LoggingMiddleware — 日志记录
    │   └── MetricsMiddleware — 指标采集
    └── 容错组（priority: 500）
        ├── RetryMiddleware — 重试机制
        └── FallbackMiddleware — 降级处理
    ↓
[核心调用] → [LLM / Tool]
```

### 5.3 设计多层级记忆管理系统
**分层架构**：
```
Layer 1: 缓存层 (Redis)
  - 当前活跃会话的短期记忆
  - TTL 自动过期
  - 支持 LRU 淘汰

Layer 2: 持久层 (PostgreSQL)
  - 所有会话的历史消息
  - 支持时间范围查询
  - 消息归档策略

Layer 3: 摘要层 (Summary)
  - 对长对话自动生成摘要
  - 按 Token 阈值触发摘要
  - 多级摘要：每日摘要 → 每周摘要

Layer 4: 语义检索层 (VectorStore)
  - 将消息片段向量化
  - 基于语义检索相关历史
  - 支持跨会话检索
```

```python
class HierarchicalMemoryManager:
    """分层记忆管理器"""

    def __init__(self, redis_client, pg_client, vectorstore, llm):
        self.redis = redis_client       # 短期缓存
        self.postgres = pg_client       # 长期存储
        self.vectorstore = vectorstore  # 语义检索
        self.llm = llm                  # 摘要生成
        self.summary_threshold = 4000   # Token 摘要阈值

    async def add_message(self, session_id: str, role: str, content: str):
        # 1. 写入 Redis 缓存（短期）
        await self.redis.lpush(f"session:{session_id}:recent", {
            "role": role, "content": content, "timestamp": time.time()
        })
        await self.redis.ltrim(f"session:{session_id}:recent", 0, 19)  # 保留最近20条

        # 2. 写入 PostgreSQL（持久化）
        await self.postgres.execute(
            "INSERT INTO messages (session_id, role, content) VALUES ($1, $2, $3)",
            session_id, role, content,
        )

        # 3. 检查是否需要摘要
        token_count = await self._count_session_tokens(session_id)
        if token_count > self.summary_threshold:
            await self._generate_and_store_summary(session_id)

        # 4. 写入向量存储（语义检索）
        if role == "ai":
            embedding = await self.embeddings.aembed_query(content)
            await self.vectorstore.aadd_texts(
                texts=[content],
                metadatas=[{"session_id": session_id, "role": role}],
            )

    async def get_context(self, session_id: str, query: str) -> str:
        # 1. 获取短期记忆（最近20条）
        recent = await self.redis.lrange(f"session:{session_id}:recent", 0, -1)

        # 2. 获取当前摘要
        summary = await self.redis.get(f"session:{session_id}:summary")

        # 3. 语义检索相关历史
        similar = await self.vectorstore.similarity_search(
            query, k=3, filter={"session_id": session_id}
        )

        # 4. 组合上下文
        context = {
            "summary": summary or "",
            "recent_messages": list(reversed(recent)),
            "relevant_history": [d.page_content for d in similar],
        }
        return context
```

### 5.4 设计企业级 AI Agent 平台
**平台架构**：
```
[用户层]
  Web UI / API / Slack Bot / 微信小程序

[网关层]
  API Gateway → 认证 → 限流 → 路由

[编排层]
  Agent Orchestrator
    ├── Task Planner — 任务分解
    ├── Tool Selector — 工具选择
    ├── Memory Manager — 记忆管理
    └── Quality Control — 质量控制

[能力层]
  ├── LLM 集群 (GPT-4/DeepSeek/Qwen) — 负载均衡 + Fallback
  ├── 工具库 (搜索/计算/数据库/API) — 动态注册 + 版本管理
  ├── RAG 引擎 (文档问答/代码搜索) — 多向量库路由
  └── 记忆系统 (短期+长期+语义) — 用户画像构建

[数据层]
  PostgreSQL / Milvus / Redis / S3 / Kafka

[可观测层]
  LangSmith / Prometheus / Grafana / ELK
```

**关键设计要点**：
| 需求 | 方案 |
|------|------|
| 多模型支持 | 统一的 ChatOpenAI 兼容接口，路由到不同厂商 |
| 工具动态注册 | 基于 @tool 装饰器 + 注册中心 |
| 任务分解 | Agent 自主规划，或使用 LangGraph 有向图编排 |
| 质量控制 | 结构化输出约束 + 人工审核中间件 |
| 成本控制 | Token 预算中间件 + 模型分级（简单任务用小模型） |
| 安全审计 | PII 脱敏 + 内容审核 + 全链路 tracing |

### 5.5 设计 RAG 评估与监控系统
**评估维度**：
| 维度 | 指标 | 测量方法 |
|------|------|---------|
| 检索质量 | Recall@K / MRR / NDCG | 人工标注标准答案集 |
| 生成质量 | Faithfulness / Relevancy | LLM-as-Judge 评估 |
| 系统性能 | P95 延迟 / QPS | 实时监控 |
| 业务指标 | 用户满意度 / 解决率 | 用户反馈收集 |

```python
class RAGEvaluator:
    """RAG 评估系统"""

    def __init__(self, judge_llm):
        self.judge = judge_llm  # 评估用的 LLM（如 GPT-4）

    async def evaluate_faithfulness(self, question: str, context: str, answer: str) -> dict:
        """评估回答是否忠于上下文"""
        prompt = f"""Evaluate if the answer is faithful to the context (1-5).

Context: {context}
Question: {question}
Answer: {answer}

Score (1=hallucination, 5=fully faithful):
Reasoning:"""
        result = await self.judge.ainvoke(prompt)
        return {"type": "faithfulness", "score": self._extract_score(result)}

    async def evaluate_relevancy(self, question: str, retrieved_docs: list) -> dict:
        """评估检索到的文档与问题的相关性"""
        relevant_count = 0
        for doc in retrieved_docs:
            result = await self.judge.ainvoke(
                f"Question: {question}\nDocument: {doc}\nIs this document relevant? (yes/no)"
            )
            if "yes" in result.content.lower():
                relevant_count += 1

        return {
            "type": "relevancy",
            "score": relevant_count / len(retrieved_docs) if retrieved_docs else 0,
        }

    def build_dashboard(self, session_id: str):
        """构建评估仪表盘"""
        # 实际对接 Prometheus + Grafana
        return {
            "total_queries": 10000,
            "avg_latency_ms": 320,
            "p95_latency_ms": 850,
            "faithfulness_score": 4.2,
            "relevancy_score": 0.85,
            "token_usage_total": 50_000_000,
            "cost_total_usd": 12.50,
        }
```

---

## 六、常见坑点与最佳实践

| 序号 | 坑点/问题 | 原因 | 解决方案 | 最佳实践 |
|------|----------|------|---------|---------|
| 1 | **模型返回空 tool_calls** | LLM 未识别需要调用工具 | 优化工具描述、检查 tool_choice 是否为 "any" | 对必用工具设 `tool_choice="ToolName"` |
| 2 | **工具参数解析失败** | LLM 生成 JSON 格式错误 | 使用 Pydantic 约束参数结构、增加错误提示 | 用 `with_structured_output` 结构化工具参数 |
| 3 | **Agent 陷入死循环** | Agent 反复调用同一工具不前进 | 设置 `max_iterations` 和 `early_stopping_method` | `AgentExecutor(max_iterations=5, early_stopping_method="generate")` |
| 4 | **Memory 无限增长导致 Token 超限** | 未配置上下文窗口管理 | 配合消息裁剪或摘要策略 | 设置 `max_token_limit` + 定期生成摘要 |
| 5 | **RAG 检索不相关文档** | Chunk 切分策略不当 | 调整 chunk_size、chunk_overlap、separators | 按语义段落切分，非固定字数 |
| 6 | **中间件顺序混乱** | 未设置 priority 导致执行顺序不可控 | 显式设置 priority 值 | 安全组 100、控制组 200、监控组 400、容错组 500 |
| 7 | **流式输出与 Agent 不兼容** | Agent 需完整 tool_call 才能继续 | 使用 Agent 专属流式策略 | 用 `.astream_events()` 过滤 agent 事件 |
| 8 | **Pydantic 校验失败** | LLM 输出不符合 Schema | 添加 `field_validator` + 宽松模式 | 设 `strict=False`，避免过严格约束 |
| 9 | **Milvus 连接超时** | 未配置健康检查和重连机制 | 封装连接池 + 健康检查 | `connection_args` 中配置 `timeout` 和 `retry` |
| 10 | **LangSmith 追踪数据泄露** | 生产环境未过滤敏感信息 | 配置 LangSmith 的 masking 功能 | 在 PIIMiddleware 中过滤后再发送 |
| 11 | **多模型切换失败** | Fallback 未考虑模型接口差异 | 使用 `ModelFallbackMiddleware` 统一处理 | 确保 Fallback 模型参数兼容 |
| 12 | **异步调用未 await** | 混合 sync/async 代码导致死锁 | 统一使用全异步或全同步 | RAG 全链路 async + asyncio |
| 13 | **Prompt 中变量未转义** | 用户输入包含模板语法 `{}` | 使用 `MessagesPlaceholder` 而非字符串拼接 | 一律通过模板引擎传参，不手动拼 SQL |
| 14 | **向量库未建索引** | 大规模检索性能极差 | 建索引并选择合适算法 | 10w+ 数据用 HNSW，百万级用 IVF |
| 15 | **工具函数状态污染** | 工具内部有可变全局变量 | 函数式无状态设计 | `@tool` 函数内不使用全局变量 |

---

## 七、面试回答模板（Top 5高频题的结构化回答模板）

### 7.1 "LangChain 的 Agent 是如何实现工具调用的？"
**回答要点**（STAR 结构）：

```
Agent 的工具调用本质上是 LLM 在对话中生成工具调用指令的流程。

1. Schema 生成层：@tool 装饰器解析函数签名和 docstring，自动生成 JSON Schema
   （参数名、类型、是否必填、描述），绑定到 LLM 请求中。

2. 决策层：LLM 分析用户意图，在生成文本的同时选择是否调用工具以及调用哪个工具。
   通过 tool_choice 参数控制：auto（自主）、any（强制）、none（禁止）、指定名称。

3. 执行层：LangChain 解析 LLM 返回的 tool_calls，提取 name 和 args，
   路由到对应工具函数执行。

4. 结果整合：工具返回结果作为新的消息传回 LLM，LLM 整合后生成最终回答给用户。

关键代码：
    agent = create_tool_calling_agent(llm, tools=[search, calculator], prompt=prompt)
    executor = AgentExecutor(agent=agent, tools=tools, max_iterations=5)

在 AgentExecutor 中，整个过程由 while 循环驱动，用 agent_scratchpad 维护中间步骤，
直到 LLM 不再调用工具或达到 max_iterations 上限。
```

### 7.2 "LangChain 中间件的设计原理和常见用法？"
**回答要点**：

```
LangChain 中间件采用责任链设计模式，在 LLM 调用链中插入横切关注点。

设计原理：
1. 每个中间件实现 BaseMiddleware 接口，提供 __call__ 方法
2. 中间件接收 (messages, call_options, next) 三个参数
3. 通过 next(messages, call_options) 将请求传递给下一个中间件
4. 按 priority 排序，数值越小执行优先级越高
5. 可以在 next 前后分别插入请求前和响应后逻辑

常用分类：
- 安全类：PIIMiddleware（脱敏）、ContentFilterMiddleware（过滤）
- 控制类：ModelCallLimitMiddleware（限流）、ToolCallLimitMiddleware（调用次数）
- 增强类：SummarizationMiddleware（长上下文压缩）
- 容错类：ModelFallbackMiddleware（降级）、ToolRetryMiddleware（重试）
- 交互类：HumanInTheLoopMiddleware（人工确认）

底层原理图：
    请求 → M1 → M2 → M3 → LLM/Tool → M3 → M2 → M1 → 响应
          (请求链)          (响应链反向)

面试亮点：提到 LangChain Middleware 的 priority 排序机制和多层嵌套的洋葱模型执行顺序。
```

### 7.3 "Memory 管理中如何处理长对话？"
**回答要点**：

```
长对话处理有三个核心挑战：Token 超限、关键信息丢失、检索效率。

我的处理策略是分层记忆 + 自适应摘要：

1. 短期记忆（缓存层）：
   - 使用 BufferWindowMemory 保留最近 N 轮完整对话
   - 通过 Redis 存储，TTL 过期机制

2. 摘要记忆（压缩层）：
   - 当 Token 数超过阈值（如 4000）时触发摘要
   - ConversationSummaryMemory 自动生成对话摘要
   - 支持多级摘要：会话级 → 日级 → 周级

3. 语义检索（长期层）：
   - 将历史消息向量化存入 Milvus/PostgreSQL
   - 根据当前 query 检索相关历史片段
   - VectorStoreRetrieverMemory 实现

4. 消息裁剪策略：
   - 固定窗口：保留最近 K 轮
   - Token 预算：保留 Token 数不超过上限
   - 重要性评分：保留关键信息，丢弃冗余

代码示例：
    memory = ConversationSummaryMemory(llm=llm, max_token_limit=2000)
    chain = RunnableWithMessageHistory(
        chain,
        get_session_history,
        input_messages_key="input",
        history_messages_key="chat_history",
        max_token_limit=4000,
    )

注意：生产环境一定要结合中间件 SummarizationMiddleware 自动处理长上下文，
以及定期归档到长期存储，避免无限增长。
```

### 7.4 "什么是 RAG？LangChain 中如何实现？"
**回答要点**：

```
RAG（Retrieval-Augmented Generation）是一种将检索与生成结合的技术范式，
解决 LLM 的知识滞后和幻觉问题。

完整工作流：
  文档加载 → 文本切分 → 嵌入向量化 → 向量存储 → 用户查询
  → 语义检索 TopK → 重排序 → 注入 Prompt → LLM 生成

在 LangChain 中的实现（七行核心代码）：

    loader = TextLoader("doc.txt")
    docs = loader.load()
    splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
    chunks = splitter.split_documents(docs)
    vectorstore = Milvus.from_documents(chunks, embeddings)
    retriever = vectorstore.as_retriever(search_kwargs={"k": 4})
    rag_chain = (
        {"context": retriever | format_docs, "question": RunnablePassthrough()}
        | prompt | llm | StrOutputParser()
    )

关键优化点：
1. 切分策略：按语义段落切分，非固定字数，维护上下文完整性
2. 检索策略：结合 keyword search + vector search（混合检索）
3. 重排序：使用 CohereRerank / Cross-Encoder 提升 TopK 质量
4. Prompt 优化：清晰区分"基于上下文回答" vs "不知道就说不知道"

RAG 三大挑战：
- 检索精度：低召回 = 回答不完整
- 上下文窗口：文档过多超出 LLM 限制
- 评估困难：需要 Faithfulness + Relevancy 多维评估
```

### 7.5 "LangChain 结构化输出的几种方式及选择？"
**回答要点**：

```
LangChain 支持四种结构化输出方式：

1. Pydantic（推荐生产使用）
   优势：类型安全、运行时校验、嵌套模型支持、字段描述
   使用：BaseModel + Field + with_structured_output
   场景：复杂 JSON 输出、需要严格校验的场景

2. TypedDict（推荐快速原型）
   优势：零依赖、Python 原生、轻量
   使用：继承 TypedDict + with_structured_output
   场景：内部工具、原型验证

3. JSON Schema（推荐跨语言）
   优势：通用性强、非 Python 系统兼容
   使用：传递 dict Schema + with_structured_output
   场景：多语言微服务、开放 API

4. @dataclass（推荐已有 dataclass 的代码库）
   优势：Python 标准、简洁
   劣势：无校验、无字段描述
   场景：内部简单结构

选择建议：
- 生产环境 → Pydantic（校验 + 文档 + 嵌套支持）
- 快速原型 → TypedDict（最轻量）
- 多语言 → JSON Schema（最通用）
- 简单数据结构 → @dataclass（最简洁）

实现原理：with_structured_output 内部将 Schema 注入 System Prompt，
指导 LLM 生成符合结构的 JSON，再自动反序列化为对应类型。

注意：设置 strict=False 避免过严格校验导致失败，
同时配合 field_validator 做业务校验。
```

---

## 八、快速查漏补缺 Checklist

- [ ] LangChain 四大支柱（LangChain / LangSmith / LangServe / LangGraph）
- [ ] Runnable 协议（invoke / stream / batch / ainvoke / astream / abatch）
- [ ] ChatPromptTemplate 三种创建方式和 MessagesPlaceholder
- [ ] 模型调用的 4 种模型（OpenAI / DeepSeek / 智谱 / Ollama）
- [ ] 流式调用（stream / astream）和异步调用（ainvoke / abatch）
- [ ] profile、model_kwargs、extra_body 参数含义
- [ ] LangSmith 可观测平台（Tracing / Evaluation / Monitoring）
- [ ] 消息的两种格式（BaseMessage 对象 vs Tuple 字符串）
- [ ] @tool 装饰器定义工具并绑定到模型
- [ ] tool_choice 四种模式（auto / any / none / ToolName）
- [ ] 结构化输出四种方式及选择（Pydantic / TypedDict / JSON Schema / dataclass）
- [ ] Pydantic 模式流程图：Schema → Prompt → LLM → JSON → 校验 → 对象
- [ ] Agent 概述和 create_tool_calling_agent
- [ ] Agent 绑定工具并调用完整流程
- [ ] Agent 工具调用流程分析（Schema → 绑定 → 决策 → 执行 → 整合）
- [ ] Agent 结构化输出 4 种策略
- [ ] Agent 错误处理机制（max_iterations、early_stopping）
- [ ] Agent 流式输出策略（astream_events）
- [ ] 中间件分类（Summarization / HumanInTheLoop / PII / Fallback 等9种）
- [ ] 中间件设计模式（责任链 + priority 排序）
- [ ] Hook 函数理解（on_llm_start / on_llm_end / on_tool_start / on_tool_end）
- [ ] 装饰器定义 Node-style 钩子
- [ ] 基于类定义 Node-style 钩子（BaseCallbackHandler）
- [ ] wrap_model_call 和 wrap_tool_call
- [ ] Memory 分类（Buffer / Summary / Vector / Entity / 长期）
- [ ] 短期记忆持久化到 PostgreSQL
- [ ] 消息裁剪治理（固定窗口 / Token 预算 / 重要性评分）
- [ ] 消息删除与摘要治理
- [ ] 长期记忆（跨会话持久化）
- [ ] RAG 工作流程（Load → Split → Embed → Store → Retrieve → Rerank → Generate）
- [ ] 各类文档加载器（TextLoader / PyPDFLoader / WebBaseLoader 等）
- [ ] 切分策略和 TextSplitter（RecursiveCharacterTextSplitter 参数）
- [ ] 嵌入模型初始化及文档向量化
- [ ] 向量数据库核心概念（Milvus：Collection / Partition / Index / Segment）
- [ ] Milvus 集成（from_documents / similarity_search / index_params）
- [ ] RAG 重排序（Reranker）
- [ ] Assistant 客服知识库项目架构
- [ ] @dataclass 结构化输出
- [ ] ChatOpenAI 兼容接口调用国产大模型
- [ ] PII 中间件敏感信息脱敏
- [ ] HumanInTheLoop 人工审核
- [ ] ModelFallback 故障切换
- [ ] ToolRetry 失败重试
- [ ] LLMToolSelector 工具选择优化
- [ ] RunnableSequence 管道编排
- [ ] RunnableWithMessageHistory 带历史记录的 Chain
- [ ] Pydantic field_validator 自定义校验
- [ ] 余弦相似度计算和检索原理
- [ ] LangGraph 有状态工作流（了解）
- [ ] LangServe 部署 REST API（了解）
- [ ] 向量数据库索引类型（IVF_FLAT / HNSW / ANNOY）
- [ ] 嵌入模型选择（text-embedding-3-small / bge-m3 / m3e）
- [ ] Pagination / Offset 查询优化
- [ ] 批量文档处理和异步写入
- [ ] 工具调用结果缓存策略
- [ ] Prompt 注入防护
- [ ] Token 成本估算和预算控制
