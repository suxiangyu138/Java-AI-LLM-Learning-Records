# LangChain+LangGraph开发实战 面试宝典
> 基于AI通识至LangChain实战全流程大纲，覆盖AI基础、大模型API调用、LangChain核心组件、记忆管理、工具调用、LangSmith及项目部署面试考点

## 目录
1. [一、基础概念速答](#一基础概念速答15-20题)
2. [二、深度原理剖析](#二深度原理剖析10-15题)
3. [三、实战场景题](#三实战场景题8-12题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1.1 神经网络的基本原理是什么？
神经网络由**输入层、隐藏层、输出层**组成，每层包含若干神经元（节点）。核心机制分为两步：
- **前馈传播（Forward Propagation）**：输入数据从输入层逐层向前传播，每层通过加权求和 `z = w·x + b`，再经过激活函数（ReLU、Sigmoid、Tanh）引入非线性。
- **反向传播（Backpropagation）**：通过链式法则计算损失函数对各层权重的梯度，从输出层反向传播至输入层，更新权重以最小化损失。

> 💡 神经网络的核心能力在于：多层非线性变换可以逼近任意复杂函数（万能逼近定理）。

### 1.2 大语言模型（LLM）是如何工作的？
LLM基于**Transformer架构**，核心是**自注意力机制（Self-Attention）**：
1. **预训练**：在海量文本上通过"预测下一个词"任务学习语言规律，模型学习到语法、知识、推理能力。
2. **SFT（监督微调）**：用人工标注的高质量问答数据微调，让模型学会遵循指令。
3. **RLHF（基于人类反馈的强化学习）**：通过奖励模型优化输出，使其更符合人类偏好。

> 🎯 关键点：LLM本质是一个巨大的概率模型，每一步生成下一个 token 的概率分布。

### 1.3 LangChain 解决了什么问题？
LangChain 是一个**大模型应用开发框架**，解决了以下核心问题：

| 问题 | LangChain 的解决方案 |
|------|---------------------|
| 模型切换困难 | 统一接口封装（ChatOpenAI、ChatOllama 等） |
| 提示词管理混乱 | PromptTemplate + ChatPromptTemplate |
| 缺乏记忆能力 | Memory 组件（BufferMemory、SummaryMemory） |
| 无法调用外部工具 | Tool 抽象 + Agent 自动决策 |
| 输出格式不规范 | OutputParser + Structured Output |
| 缺少监控追踪 | LangSmith 集成（Tracing、Evaluation） |

### 1.4 Agent 的原理是什么？
Agent 是让 LLM 能够**自主决策并调用工具**的机制：
1. LLM 接收用户问题 + 可用工具描述（Schema）
2. LLM 判断是否需要调用工具，输出结构化调用指令（Function Calling）
3. 系统执行工具并返回结果
4. LLM 结合工具结果生成最终回复
5. 循环执行直到任务完成

> ⚠️ Agent ≠ LLM。Agent = LLM + Tools + Memory + Planning 的组合体。

### 1.5 消息(Message)在 LangChain 中的作用？
LangChain 用消息系统管理对话上下文：

| 消息类型 | 作用 | 示例 |
|---------|------|------|
| `SystemMessage` | 系统指令，设定 AI 角色和行为 | "你是一个助手" |
| `HumanMessage` | 用户输入内容 | "今天天气怎么样？" |
| `AIMessage` | 模型的回复内容 | "今天是晴天，气温25度。" |
| `ToolMessage` | 工具执行结果 | {"temperature": 25} |
| `FunctionMessage` | (旧版) 函数调用结果 | 已逐步被 ToolMessage 替代 |

### 1.6 短期记忆与长期记忆的区别？

| 维度 | 短期记忆 | 长期记忆 |
|------|---------|---------|
| 存储方式 | 内存中（列表/缓冲区） | 持久化存储（DB/VectorStore） |
| 容量限制 | 受 Token 限制（通常几轮对话） | 几乎无限（按需检索） |
| 实现方式 | ConversationBufferMemory | VectorStoreMemory / PostgresMemory |
| 检索方式 | 全部加载 | 语义相似度检索 + 过滤 |
| 适用场景 | 短对话、实时交互 | 长对话、知识库QA、个性化记忆 |

### 1.7 什么是 Tavily 工具？
Tavily 是一个**专为 LLM 优化的搜索引擎 API**，作为 LangChain 预定义工具使用：

```python
from langchain_community.tools.tavily_search import TavilySearchResults

tool = TavilySearchResults(max_results=3)
result = tool.invoke({"query": "2024年诺贝尔物理学奖"})
```

> 💡 Tavily 相比于通用搜索引擎的优势：返回结构化结果、过滤广告、支持实时新闻、专为 AI 调用优化 API 格式。

### 1.8 提示词工程的基本要素有哪些？

| 要素 | 说明 | 示例 |
|------|------|------|
| 角色设定（Role） | 定义 AI 扮演的角色 | "你是一名资深Java工程师" |
| 任务描述（Task） | 明确说明需要完成的任务 | "请解释AOP原理" |
| 上下文（Context） | 提供背景信息 | "用户是初学者" |
| 格式要求（Format） | 指定输出格式 | "用Markdown表格回答" |
| 示例（Few-shot） | 给出输入输出示例 | "例如：Q:... A:..." |
| 约束（Constraints） | 限制行为和边界 | "不要编造信息，不确定时说不知道" |

### 1.9 什么是 Ollama？有什么作用？
Ollama 是一个**本地大模型运行工具**，允许在本地机器上运行开源 LLM（如 Llama、Qwen、DeepSeek 等）：
- 无需联网，数据隐私安全
- 支持 OpenAI 兼容 API（`/v1/chat/completions`）
- 适合开发调试和本地原型验证
- 命令行操作：`ollama pull deepseek-r1:7b`、`ollama run qwen2.5:7b`

### 1.10 什么是 LangSmith？主要功能？
LangSmith 是 LangChain 生态的**LLM 应用可观测性平台**，核心功能：

| 功能 | 说明 |
|------|------|
| Tracing（追踪） | 记录每次 LLM 调用的完整链路（Run Tree） |
| Token Usage | 统计 Token 消耗和费用 |
| Feedback | 收集用户反馈，评估输出质量 |
| Dataset & Testing | 构建测试集，回归测试 Prompt 效果 |
| Hub | 共享和版本管理 Prompt 模板 |
| Monitoring | 生产环境监控、告警、分析 |

### 1.11 什么是 OSS？在 AI 部署中的作用？
**OSS（Object Storage Service，对象存储服务）**，如阿里云 OSS、AWS S3、MinIO：
- 存储 AI 应用的静态资源（图片、文档、模型文件）
- 存储对话日志和用户上传文件
- 作为知识库的文件存储层（配合 Embedding + VectorStore）
- 为 AI 应用提供高可用的文件访问链接

### 1.12 DeepSeek 和阿里云百炼的特点？

| 维度 | DeepSeek | 阿里云百炼 |
|------|---------|-----------|
| 模型 | DeepSeek-V2/V3 系列 | Qwen 系列 + 第三方模型 |
| 定位 | 基础模型提供商 | 一站式大模型服务平台 |
| API 兼容 | OpenAI 兼容 | OpenAI 兼容 |
| 价格 | 性价比高 | 按量计费 + 资源包 |
| 特色 | 开源模型（可本地部署） | 模型训练 + 部署 + 应用构建 |
| 适用场景 | 个人开发者、高性价比需求 | 企业级应用、阿里云生态 |

### 1.13 接口规范（OpenAI 兼容 API）
当前大模型 API 的事实标准格式：

```python
# 请求格式 POST /v1/chat/completions
{
    "model": "gpt-4",
    "messages": [
        {"role": "system", "content": "你是一个助手"},
        {"role": "user", "content": "你好"},
        {"role": "assistant", "content": "你好！有什么可以帮助你的？"},
        {"role": "user", "content": "今天天气怎么样？"}
    ],
    "temperature": 0.7,
    "stream": false
}

# 响应格式
{
    "id": "chatcmpl-xxx",
    "object": "chat.completion",
    "choices": [{
        "index": 0,
        "message": {
            "role": "assistant",
            "content": "今天是晴天，气温25度。"
        },
        "finish_reason": "stop"
    }],
    "usage": {
        "prompt_tokens": 20,
        "completion_tokens": 10,
        "total_tokens": 30
    }
}
```

### 1.14 什么是 Function Calling 结构化输出？
Function Calling 允许 LLM 输出**结构化的 JSON 数据**而不是纯文本：

```python
from langchain_core.pydantic_v1 import BaseModel, Field

class WeatherResponse(BaseModel):
    city: str = Field(description="城市名称")
    temperature: float = Field(description="温度（摄氏度）")
    weather: str = Field(description="天气状况")
    humidity: int = Field(description="湿度百分比")

# 通过 with_structured_output 绑定
llm_with_structure = llm.with_structured_output(WeatherResponse)
result = llm_with_structure.invoke("北京今天天气怎么样？")
# result 是 WeatherResponse 对象
```

### 1.15 什么是 Embedding？在 RAG 中的作用？
**Embedding（嵌入）** 是将文本转换为固定长度的向量表示，语义相似的文本向量距离更近。在 RAG（检索增强生成）中：
1. 文档切块 → Embedding → 存入 VectorStore
2. 用户问题 → Embedding → 向量检索相似文档
3. 检索结果 + 问题 → LLM 生成回答

---

## 二、深度原理剖析（10-15题）

### 2.1 神经网络与 LLM：前馈网络 → Transformer → 预训练 → SFT → RLHF

**完整链路：**

```
前馈神经网络 → RNN/LSTM → Transformer(2017) → GPT系列(2018-) → LLM(2023+)
```

**Transformer 核心创新：**
- **自注意力机制**：每个 token 可以关注序列中任意位置的 token，解决长距离依赖
- **多头注意力**：并行学习不同子空间的特征
- **位置编码**：为序列注入位置信息（无 RNN 的递归结构）

**LLM 训练三阶段：**
1. **预训练（Pre-training）**：在大规模语料上做"下一个 token 预测"，学习通用语言理解和知识
2. **SFT（Supervised Fine-Tuning）**：用人工标注的高质量指令-回复数据微调，学习遵循指令
3. **RLHF（Reinforcement Learning from Human Feedback）**：
   - 训练奖励模型（Reward Model）打分人类偏好
   - 用 PPO 算法优化策略模型
   - 使输出更符合人类偏好（有用、安全、诚实）

> 🎯 面试重点：RLHF 的成本最高但也最关键，决定了模型的对齐质量。

### 2.2 LangChain 消息系统详解

```python
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage, ToolMessage

messages = [
    SystemMessage(content="你是一个智能助手，使用工具回答问题。"),
    HumanMessage(content="北京的天气怎么样？"),
    AIMessage(
        content="",  # 空内容，因为要调用工具
        tool_calls=[{
            "id": "call_123",
            "name": "get_weather",
            "args": {"city": "北京"}
        }]
    ),
    ToolMessage(
        content='{"temperature": 25, "weather": "晴"}',
        tool_call_id="call_123"
    ),
    AIMessage(content="北京今天天气晴朗，气温25度。")
]
```

**消息处理流程：**
1. System 设定角色 → 2. User 提问 → 3. LLM 生成（可能含 tool_calls） → 4. 执行工具，结果包装为 ToolMessage → 5. LLM 结合工具结果生成最终回复

### 2.3 LangChain 记忆管理机制详解

| 记忆类型 | 原理 | 优点 | 缺点 | 适用场景 |
|---------|------|------|------|---------|
| `ConversationBufferMemory` | 缓存所有消息到列表 | 实现简单，信息完整 | Token 消耗大 | 短对话 |
| `ConversationSummaryMemory` | 定期总结对话历史 | Token 效率高 | 可能丢失细节 | 长对话 |
| `VectorStoreMemory` | 语义检索相关记忆 | 支持大规模记忆 | 需要 Embedding 模型 | 知识库对话 |
| `PostgresMemory` | 持久化到数据库 | 持久化、可查询 | 需要数据库 | 生产环境 |
| `ConversationTokenBufferMemory` | 按 Token 数裁剪 | 精确控制 Token | 可能截断关键信息 | Token 敏感场景 |

```python
# 持久化记忆实现
from langchain.memory import PostgresChatMessageHistory
from langchain.memory import ConversationBufferMemory

history = PostgresChatMessageHistory(
    connection_string="postgresql://user:pass@localhost/db",
    session_id="user_session_001"
)

memory = ConversationBufferMemory(
    chat_memory=history,
    return_messages=True
)
```

### 2.4 工具调用流程（Tool Calling）

**完整流程：**

```
用户提问 → LLM判断需要工具 → 生成tool_calls → 系统执行tool → 结果回传 → LLM生成最终回复
```

```python
from langchain_core.tools import tool
import requests

@tool
def get_weather(city: str) -> dict:
    """获取指定城市的天气信息"""
    # 工具实现
    response = requests.get(f"https://api.weather.com/{city}")
    return response.json()

# LangChain 自动生成 JSON Schema
print(get_weather.args_schema.schema())
# {
#   "type": "object",
#   "properties": {
#     "city": {"type": "string", "description": "城市名称"}
#   },
#   "required": ["city"]
# }
```

**底层调用原理：**
1. LangChain 将 Tool 定义转换为 OpenAI Function Calling 格式的 JSON Schema
2. LLM 接收 Schema 后决定调用哪个 Tool 和传入的参数
3. LLM 返回 `tool_calls` 包含 `id`、`name`、`args`
4. LangChain 自动执行工具，将结果包装为 `ToolMessage`
5. 将 `ToolMessage` 传回 LLM 生成最终回复

### 2.5 LangSmith Tracing：Run Tree 详解

```python
from langsmith import Client
from langchain.callbacks.tracers import LangSmithTracer

# 自动 Tracing 所有 LangChain 调用
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_API_KEY"] = "ls_xxx"
os.environ["LANGCHAIN_PROJECT"] = "ai-chef"

# Run Tree 结构
"""
Root Run: Chain.invoke()
├── LLM Run: ChatOpenAI (生成回复)
│   ├── Token Usage: prompt=100, completion=50
│   └── Latency: 1200ms
├── Tool Run: TavilySearchResults (搜索)
│   ├── Input: {"query": "红烧肉做法"}
│   ├── Output: [...搜索结果...]
│   └── Latency: 800ms
└── LLM Run: ChatOpenAI (生成最终回复)
    └── Token Usage: prompt=200, completion=100
"""
```

### 2.6 大模型 API 设计规范

| 特性 | 说明 | 实现方式 |
|------|------|---------|
| OpenAI 兼容格式 | 统一的 messages 接口 | POST /v1/chat/completions |
| Streaming（流式输出） | SSE 协议逐 token 输出 | `stream=True` 参数 |
| Tool Calling | 结构化工具调用 | `tools` 参数传入 Schema |
| Structured Output | JSON Schema 约束输出 | `response_format` 参数 |
| Multi-modal | 多模态输入（图片、音频） | `content` 支持多类型数组 |
| Function Calling | 函数调用（旧称） | `functions` 参数（已弃用） |

```python
# 流式输出实现
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model="deepseek-chat",
    api_key="sk-xxx",
    base_url="https://api.deepseek.com/v1",
    streaming=True  # 启用流式
)

# 服务端流式
from fastapi.responses import StreamingResponse

async def stream_response(query: str):
    async for chunk in llm.astream(query):
        yield f"data: {chunk.content}\n\n"

@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    return StreamingResponse(
        stream_response(request.query),
        media_type="text/event-stream"
    )
```

### 2.7 开发环境配置详解

```bash
# 1. 安装依赖
pip install langchain langchain-openai langchain-community
pip install langchain-core langsmith  # 核心 + 监控
pip install tavily-python  # 搜索引擎
pip install fastapi uvicorn  # 服务端部署
pip install oss2  # 阿里云 OSS
pip install chromadb  # 向量数据库

# 2. 环境变量
export OPENAI_API_KEY="sk-xxx"
export LANGCHAIN_API_KEY="ls_xxx"
export TAVILY_API_KEY="tvly-xxx"
```

---

## 三、实战场景题（8-12题）

### 3.1 DeepSeek/Ollama API 调用

```python
# DeepSeek API 调用
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model="deepseek-chat",
    api_key="sk-your-deepseek-api-key",
    base_url="https://api.deepseek.com/v1",
    temperature=0.7
)

response = llm.invoke("请用Python写一个快速排序算法")
print(response.content)

# Ollama 本地调用
from langchain_ollama import ChatOllama

local_llm = ChatOllama(
    model="qwen2.5:7b",
    temperature=0.8,
    base_url="http://localhost:11434"
)

response = local_llm.invoke("解释一下什么是RAG")
print(response.content)
```

> 💡 面试技巧：提到 DeepSeek 的性价比优势和 Ollama 的本地部署隐私优势。

### 3.2 LangChain 消息管理与对话机器人

```python
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(model="deepseek-chat", api_key="sk-xxx")

messages = [
    SystemMessage(content="你是一个耐心的高中数学老师"),
    HumanMessage(content="解释一下什么是导数"),
]

# 多轮对话
messages.append(AIMessage(content="导数是函数在某点的变化率..."))
messages.append(HumanMessage(content="给我举个具体例子"))

response = llm.invoke(messages)
print(response.content)
```

### 3.3 Agent + Tavily 搜索工具实现

```python
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain.tools import tool
from langchain_community.tools.tavily_search import TavilySearchResults
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

# 1. 定义工具
@tool
def calculate_recipe_calories(ingredients: list) -> dict:
    """计算食材的总热量"""
    calorie_db = {"五花肉": 500, "土豆": 80, "大米": 130}
    total = sum(calorie_db.get(i, 100) for i in ingredients)
    return {"total_calories": total, "unit": "kcal/100g"}

search = TavilySearchResults(max_results=3)

# 2. 创建 Agent
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个AI私厨助手，可以帮助用户查询菜谱、计算热量、推荐菜品。"),
    ("placeholder", "{chat_history}"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}"),
])

agent = create_tool_calling_agent(llm, [search, calculate_recipe_calories], prompt)
agent_executor = AgentExecutor(agent=agent, tools=[search, calculate_recipe_calories], verbose=True)

# 3. 执行
result = agent_executor.invoke({"input": "推荐一道低热量的家常菜，告诉我怎么做"})
print(result["output"])
```

### 3.4 短期记忆与持久化存储

```python
from langchain.memory import ConversationBufferMemory
from langchain.memory import PostgresChatMessageHistory
from langchain.memory.chat_message_histories import FileChatMessageHistory

# 1. 内存记忆（短期）
memory = ConversationBufferMemory(return_messages=True)
memory.chat_memory.add_user_message("你好")
memory.chat_memory.add_ai_message("你好！有什么可以帮你的？")

# 2. 文件持久化
file_history = FileChatMessageHistory(file_path="chat_logs/session_001.json")
file_history.add_user_message("我的名字是张三")
file_history.add_ai_message("你好张三！")

# 3. PostgreSQL 持久化
postgres_history = PostgresChatMessageHistory(
    connection_string="postgresql://user:pass@localhost:5432/chatdb",
    session_id="user_123_session_456"
)
postgres_history.add_user_message("记住我喜欢吃辣")
```

### 3.5 提示词工程实战（Few-shot / CoT）

```python
from langchain_core.prompts import ChatPromptTemplate, FewShotChatMessagePromptTemplate

# 1. Few-shot 示例
examples = [
    {"input": "1 + 1 = ?", "output": "1 + 1 = 2"},
    {"input": "2 * 3 = ?", "output": "2 * 3 = 6"},
]

example_prompt = ChatPromptTemplate.from_messages([
    ("human", "{input}"),
    ("ai", "{output}"),
])

few_shot_prompt = FewShotChatMessagePromptTemplate(
    example_prompt=example_prompt,
    examples=examples,
)

final_prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个数学计算助手。请仔细计算，给出准确答案。"),
    few_shot_prompt,
    ("human", "{input}"),
])

chain = final_prompt | llm
print(chain.invoke({"input": "12 * 15 = ?"}).content)

# 2. CoT（思维链）
cot_prompt = ChatPromptTemplate.from_messages([
    ("system", """请逐步推理，最后给出答案。格式：
推理过程：...
答案：..."""),
    ("human", "{input}")
])
chain = cot_prompt | llm
```

### 3.6 LangSmith 监控集成

```python
import os
from langsmith import Client
from langchain.callbacks.tracers import LangSmithTracer

# 配置 LangSmith
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_API_KEY"] = "ls_your_langsmith_api_key"
os.environ["LANGCHAIN_PROJECT"] = "ai-chef-production"

# 创建运行
client = Client()
chain = prompt | llm

# 自动追踪每次调用
response = chain.invoke({"input": "请推荐一道菜"})

# 提交反馈
run_id = response.id  # 从返回中获取 run_id
client.create_feedback(
    run_id=run_id,
    key="user_rating",
    score=5,
    comment="回答很精准"
)

# 创建测试数据集
dataset = client.create_dataset(
    dataset_name="recipe_qa",
    description="菜谱问答测试集"
)
client.create_examples(
    inputs=[{"input": "红烧肉怎么做"}],
    outputs=[{"output": "五花肉500g..."}],
    dataset_id=dataset.id
)
```

### 3.7 服务端部署 + OSS 对接

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import oss2
from langchain_openai import ChatOpenAI
from langchain.agents import create_tool_calling_agent, AgentExecutor

app = FastAPI(title="AI私厨API")

# OSS 配置
auth = oss2.Auth("access_key_id", "access_key_secret")
bucket = oss2.Bucket(auth, "https://oss-cn-hangzhou.aliyuncs.com", "ai-chef-bucket")

class ChatRequest(BaseModel):
    query: str
    session_id: str = "default"

class ChatResponse(BaseModel):
    answer: str
    sources: list = []

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    try:
        result = agent_executor.invoke({
            "input": request.query
        })
        
        # 记录日志到 OSS
        log_content = f"Q: {request.query}\nA: {result['output']}\n"
        bucket.put_object(f"logs/{request.session_id}/{datetime.now()}.txt", log_content)
        
        return ChatResponse(
            answer=result["output"],
            sources=result.get("intermediate_steps", [])
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    async def generate():
        async for chunk in llm.astream(request.query):
            yield f"data: {chunk.content}\n\n"
    return StreamingResponse(generate(), media_type="text/event-stream")

# 启动：uvicorn main:app --host 0.0.0.0 --port 8000
```

### 3.8 AI 私厨 Agent 开发完整项目

```python
"""
AI私厨 Agent 完整实现
功能：
1. 根据食材推荐菜品
2. 查询菜谱步骤
3. 计算菜品热量
4. 记录用户偏好
"""

from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain.memory import PostgresChatMessageHistory, ConversationBufferMemory
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.tools import tool
from langchain_community.tools.tavily_search import TavilySearchResults
from langchain_openai import ChatOpenAI

# 1. 初始化 LLM
llm = ChatOpenAI(
    model="deepseek-chat",
    api_key="sk-xxx",
    temperature=0.7
)

# 2. 定义工具
@tool
def search_recipe(dish_name: str) -> list:
    """搜索菜谱，返回步骤列表"""
    from tavily import TavilyClient
    client = TavilyClient(api_key="tvly-xxx")
    results = client.search(f"{dish_name} 做法 步骤", max_results=3)
    return [{"title": r["title"], "content": r["content"]} for r in results]

@tool
def calculate_calories(ingredients: str) -> dict:
    """根据食材计算热量，格式：'五花肉200g,土豆100g'"""
    return {"total": 650, "unit": "kcal", "detail": {"肉": 500, "蔬菜": 150}}

@tool
def recommend_by_ingredients(ingredients: str) -> list:
    """根据已有食材推荐菜品"""
    recipes_db = {
        "五花肉,土豆": "土豆烧肉",
        "鸡胸肉,西兰花": "西兰花炒鸡胸肉",
        "鸡蛋,番茄": "番茄炒蛋"
    }
    return [{"dish": recipes_db.get(ingredients, "未知"), "difficulty": "简单"}]

# 3. 设置记忆
history = PostgresChatMessageHistory(
    connection_string="postgresql://user:pass@localhost/ai_chef",
    session_id="user_session_001"
)
memory = ConversationBufferMemory(
    chat_memory=history,
    return_messages=True,
    memory_key="chat_history"
)

# 4. 创建提示词
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是AI私厨助手，帮助用户推荐菜品、查找菜谱、计算热量。"
               "根据用户的历史偏好提供个性化建议。"),
    MessagesPlaceholder(variable_name="chat_history"),
    ("human", "{input}"),
    MessagesPlaceholder(variable_name="agent_scratchpad"),
])

# 5. 构建 Agent
tools = [search_recipe, calculate_calories, recommend_by_ingredients]
agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    memory=memory,
    verbose=True,
    handle_parsing_errors=True
)

# 6. 运行
if __name__ == "__main__":
    response = agent_executor.invoke({"input": "我有五花肉和土豆，推荐一道菜"})
    print(response["output"])
    
    # 第二轮对话（带记忆）
    response = agent_executor.invoke({"input": "这道菜需要多少卡路里？"})
    print(response["output"])
```

### 3.9 流式输出实现

```python
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
import asyncio

app = FastAPI()
llm = ChatOpenAI(model="deepseek-chat", streaming=True)

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个AI助手"),
    ("human", "{input}")
])
chain = prompt | llm

@app.post("/stream")
async def stream_chat(query: str):
    async def event_generator():
        async for chunk in chain.astream({"input": query}):
            content = chunk.content
            if content:
                yield f"data: {content}\n\n"
        yield "data: [DONE]\n\n"
    
    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
        }
    )

# 前端接收
"""
const eventSource = new EventSource('/stream?query=红烧肉做法');
eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
        eventSource.close();
        return;
    }
    document.getElementById('output').textContent += event.data;
};
"""
```

### 3.10 Function Calling 实现结构化输出

```python
from typing import Optional
from langchain_core.pydantic_v1 import BaseModel, Field
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

# 1. 定义输出 Schema
class Recipe(BaseModel):
    """菜品信息"""
    name: str = Field(description="菜品名称")
    difficulty: str = Field(description="难度：简单/中等/困难", enum=["简单", "中等", "困难"])
    cooking_time: int = Field(description="烹饪时间（分钟）")
    ingredients: list[str] = Field(description="食材列表")
    steps: list[str] = Field(description="步骤列表（至少3步）")
    tips: Optional[str] = Field(description="烹饪技巧提示", default=None)

# 2. 绑定结构化输出
llm = ChatOpenAI(model="deepseek-chat", temperature=0.3)
structured_llm = llm.with_structured_output(Recipe)

# 3. 调用
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个美食专家，请详细回答菜品相关问题。"),
    ("human", "{input}")
])

chain = prompt | structured_llm
result = chain.invoke({"input": "请给出鱼香肉丝的完整菜谱"})

print(f"菜名: {result.name}")
print(f"难度: {result.difficulty}")
print(f"时间: {result.cooking_time}分钟")
print(f"食材: {', '.join(result.ingredients)}")
print(f"步骤: {result.steps}")
```

---

## 四、手写代码题（5-8题）

### 4.1 手写消息处理循环

```python
from dataclasses import dataclass, field
from typing import List, Optional
import requests

@dataclass
class Message:
    role: str  # system / user / assistant / tool
    content: str
    tool_calls: Optional[List[dict]] = None
    tool_call_id: Optional[str] = None

class SimpleChatSession:
    """手写消息处理循环"""
    def __init__(self, system_prompt: str = "你是一个助手"):
        self.messages: List[Message] = [Message(role="system", content=system_prompt)]
    
    def add_user_message(self, content: str):
        self.messages.append(Message(role="user", content=content))
    
    def add_ai_message(self, content: str, tool_calls: Optional[List[dict]] = None):
        self.messages.append(Message(role="assistant", content=content, tool_calls=tool_calls))
    
    def add_tool_message(self, content: str, tool_call_id: str):
        self.messages.append(Message(role="tool", content=content, tool_call_id=tool_call_id))
    
    def to_api_format(self) -> List[dict]:
        """转换为 OpenAI API 格式"""
        api_messages = []
        for msg in self.messages:
            entry = {"role": msg.role, "content": msg.content}
            if msg.tool_calls:
                entry["tool_calls"] = msg.tool_calls
            if msg.tool_call_id:
                entry["tool_call_id"] = msg.tool_call_id
            api_messages.append(entry)
        return api_messages
    
    def call_api(self, api_url: str, api_key: str, model: str = "deepseek-chat"):
        """调用 API"""
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": model,
            "messages": self.to_api_format(),
            "temperature": 0.7
        }
        response = requests.post(f"{api_url}/v1/chat/completions", json=payload, headers=headers)
        return response.json()

# 使用示例
session = SimpleChatSession("你是一个数学老师")
session.add_user_message("1+1等于几？")
response = session.call_api("https://api.deepseek.com", "sk-xxx")
print(response["choices"][0]["message"]["content"])
```

### 4.2 手写简单 Agent 循环

```python
import json
import requests
from typing import Dict, Any

class SimpleAgent:
    """手写简单 Agent 循环"""
    def __init__(self, api_key: str, base_url: str):
        self.api_key = api_key
        self.base_url = base_url
        self.tools: Dict[str, callable] = {}
    
    def register_tool(self, name: str, func: callable, description: str, parameters: dict):
        """注册工具"""
        self.tools[name] = func
        self.tool_schemas = [{
            "type": "function",
            "function": {
                "name": name,
                "description": description,
                "parameters": parameters
            }
        }]
    
    def run(self, user_input: str, max_iterations: int = 5) -> str:
        """Agent 主循环"""
        messages = [
            {"role": "system", "content": "你是一个智能助手，可以使用工具。请一步一步思考。"},
            {"role": "user", "content": user_input}
        ]
        
        for i in range(max_iterations):
            response = self._call_llm(messages)
            msg = response["choices"][0]["message"]
            messages.append(msg)
            
            # 如果没有工具调用，返回结果
            if not msg.get("tool_calls"):
                return msg["content"]
            
            # 执行工具
            for tool_call in msg["tool_calls"]:
                func_name = tool_call["function"]["name"]
                func_args = json.loads(tool_call["function"]["arguments"])
                
                if func_name in self.tools:
                    result = self.tools[func_name](**func_args)
                    messages.append({
                        "role": "tool",
                        "tool_call_id": tool_call["id"],
                        "content": json.dumps(result, ensure_ascii=False)
                    })
        
        return "已达到最大迭代次数"
    
    def _call_llm(self, messages: list) -> dict:
        headers = {"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"}
        payload = {
            "model": "deepseek-chat",
            "messages": messages,
            "tools": getattr(self, 'tool_schemas', None)
        }
        resp = requests.post(f"{self.base_url}/v1/chat/completions", json=payload, headers=headers)
        return resp.json()

# 使用示例
agent = SimpleAgent("sk-xxx", "https://api.deepseek.com")

def get_weather(city: str):
    return {"city": city, "temp": 25, "weather": "晴"}

agent.register_tool("get_weather", get_weather, "获取天气", {
    "type": "object",
    "properties": {"city": {"type": "string", "description": "城市"}},
    "required": ["city"]
})

print(agent.run("北京天气怎么样？"))
```

### 4.3 手写 Memory 存储

```python
import json
from pathlib import Path
from typing import List, Dict
from datetime import datetime

class FileMemory:
    """文件持久化记忆存储"""
    def __init__(self, file_path: str, max_tokens: int = 2000):
        self.file_path = Path(file_path)
        self.max_tokens = max_tokens
        self.messages: List[Dict] = self._load()
    
    def _load(self) -> List[Dict]:
        if self.file_path.exists():
            with open(self.file_path, "r", encoding="utf-8") as f:
                return json.load(f)
        return []
    
    def _save(self):
        self.file_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self.file_path, "w", encoding="utf-8") as f:
            json.dump(self.messages, f, ensure_ascii=False, indent=2)
    
    def add_message(self, role: str, content: str):
        self.messages.append({
            "role": role,
            "content": content,
            "timestamp": datetime.now().isoformat()
        })
        self._trim_context()
        self._save()
    
    def get_context(self, max_messages: int = 10) -> List[Dict]:
        """返回最近的消息用于上下文"""
        return [{"role": m["role"], "content": m["content"]}
                for m in self.messages[-max_messages:]]
    
    def _trim_context(self):
        """Token 计数裁剪"""
        total_tokens = sum(len(m["content"]) for m in self.messages)
        while total_tokens > self.max_tokens and len(self.messages) > 1:
            removed = self.messages.pop(1)  # 保留 system 消息
            total_tokens -= len(removed["content"])
    
    def search(self, keyword: str) -> List[Dict]:
        """关键词搜索记忆"""
        return [m for m in self.messages if keyword in m["content"]]
    
    def clear(self):
        self.messages = [self.messages[0]] if self.messages else []
        self._save()

# 使用示例
memory = FileMemory("memory/user_session.json")
memory.add_message("user", "我的名字是张三")
memory.add_message("assistant", "你好张三！")
memory.add_message("user", "我喜欢吃辣")
print(memory.get_context(2))
print(memory.search("辣"))
```

### 4.4 手写 Tool Schema 定义

```python
from typing import get_type_hints, Optional
from enum import Enum
import inspect

class ToolSchemaGenerator:
    """自动从 Python 函数生成 Tool Schema"""
    
    TYPE_MAP = {
        str: "string",
        int: "integer",
        float: "number",
        bool: "boolean",
        list: "array",
        dict: "object",
    }
    
    @classmethod
    def generate(cls, func: callable) -> dict:
        """从函数签名生成 OpenAI Tool Schema"""
        sig = inspect.signature(func)
        hints = get_type_hints(func)
        doc = inspect.getdoc(func) or ""
        
        properties = {}
        required = []
        
        for name, param in sig.parameters.items():
            param_type = hints.get(name, str)
            python_type = cls.TYPE_MAP.get(param_type, "string")
            
            prop = {"type": python_type}
            
            # 解析文档中的参数描述（简化版）
            desc_line = [l for l in doc.split("\n") if name in l]
            if desc_line:
                prop["description"] = desc_line[0].strip()
            else:
                prop["description"] = f"{name} 参数"
            
            properties[name] = prop
            
            # 没有默认值的参数是必需的
            if param.default is inspect.Parameter.empty:
                required.append(name)
        
        return {
            "type": "function",
            "function": {
                "name": func.__name__,
                "description": doc.split("\n")[0] if doc else "",
                "parameters": {
                    "type": "object",
                    "properties": properties,
                    "required": required
                }
            }
        }
    
    @classmethod
    def generate_anthropic(cls, func: callable) -> dict:
        """生成 Anthropic 格式的 Tool Schema"""
        schema = cls.generate(func)
        func_def = schema["function"]
        return {
            "name": func_def["name"],
            "description": func_def["description"],
            "input_schema": func_def["parameters"]
        }

# 测试
def search_products(keyword: str, page: int = 1, category: Optional[str] = None) -> list:
    """搜索商品
    keyword: 搜索关键词
    page: 页码
    category: 商品分类
    """
    return [{"id": 1, "name": f"商品{keyword}"}]

schema = ToolSchemaGenerator.generate(search_products)
print(json.dumps(schema, indent=2, ensure_ascii=False))
```

### 4.5 手写 API 调用封装

```python
import requests
import json
from typing import Optional, Iterator, AsyncIterator
import asyncio

class LLMClient:
    """大模型 API 调用封装"""
    
    def __init__(self, api_key: str, base_url: str = "https://api.deepseek.com", model: str = "deepseek-chat"):
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
    
    def chat(self, messages: list, temperature: float = 0.7, max_tokens: Optional[int] = None) -> dict:
        """普通对话"""
        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": temperature
        }
        if max_tokens:
            payload["max_tokens"] = max_tokens
        
        resp = requests.post(
            f"{self.base_url}/v1/chat/completions",
            json=payload,
            headers=self.headers
        )
        resp.raise_for_status()
        return resp.json()
    
    def chat_stream(self, messages: list, temperature: float = 0.7) -> Iterator[str]:
        """流式对话"""
        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": temperature,
            "stream": True
        }
        
        with requests.post(
            f"{self.base_url}/v1/chat/completions",
            json=payload,
            headers=self.headers,
            stream=True
        ) as resp:
            for line in resp.iter_lines():
                if line:
                    line = line.decode("utf-8")
                    if line.startswith("data: "):
                        data_str = line[6:]
                        if data_str == "[DONE]":
                            break
                        data = json.loads(data_str)
                        delta = data["choices"][0].get("delta", {})
                        if "content" in delta:
                            yield delta["content"]
    
    def chat_with_tools(self, messages: list, tools: list) -> dict:
        """工具调用"""
        payload = {
            "model": self.model,
            "messages": messages,
            "tools": tools,
            "tool_choice": "auto"
        }
        
        resp = requests.post(
            f"{self.base_url}/v1/chat/completions",
            json=payload,
            headers=self.headers
        )
        resp.raise_for_status()
        return resp.json()
    
    def structured_output(self, messages: list, response_schema: dict) -> dict:
        """结构化输出"""
        payload = {
            "model": self.model,
            "messages": messages,
            "response_format": {
                "type": "json_object",
                "schema": response_schema
            }
        }
        
        resp = requests.post(
            f"{self.base_url}/v1/chat/completions",
            json=payload,
            headers=self.headers
        )
        resp.raise_for_status()
        result = resp.json()
        return json.loads(result["choices"][0]["message"]["content"])

# 使用示例
client = LLMClient("sk-xxx")
for chunk in client.chat_stream([{"role": "user", "content": "写一首诗"}]):
    print(chunk, end="", flush=True)
```

---

## 五、系统设计题（3-5题）

### 5.1 设计 AI 私厨 Agent 系统

**系统架构设计：**

```
┌─────────────────────────────────────────────┐
│               客户端 (Web/App)                │
└──────────────────┬──────────────────────────┘
                   │ HTTP/SSE
┌──────────────────▼──────────────────────────┐
│            API 网关 (FastAPI/Nginx)           │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│            AI Agent 服务层                    │
│  ┌─────────┐ ┌──────────┐ ┌─────────────┐  │
│  │LLM Router│ │Agent Core│ │Tool Executor│  │
│  │ (模型路由)│ │ (决策循环)│ │ (工具执行)  │  │
│  └─────────┘ └──────────┘ └─────────────┘  │
│  ┌─────────┐ ┌──────────┐ ┌─────────────┐  │
│  │ Prompt  │ │ Memory   │ │ Output      │  │
│  │ Manager │ │ Manager  │ │ Parser      │  │
│  └─────────┘ └──────────┘ └─────────────┘  │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           基础设施层                          │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │PostgreSQL│ │ Redis    │ │ VectorStore │  │
│  │(对话记录) │ │ (会话缓存)│ │ (知识库)   │  │
│  └──────────┘ └──────────┘ └────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │ OSS      │ │ Tavily   │ │ LangSmith  │  │
│  │(文件存储) │ │ (搜索)   │ │ (监控)     │  │
│  └──────────┘ └──────────┘ └────────────┘  │
└─────────────────────────────────────────────┘
```

**核心组件说明：**

| 组件 | 职责 | 技术选型 |
|------|------|---------|
| LLM Router | 根据任务类型路由到不同模型 | DeepSeek/Qwen/Ollama |
| Agent Core | 决策循环：意图识别→工具选择→执行→生成 | LangChain Agent + LangGraph |
| Memory Manager | 分段记忆管理：短期/长期/持久化 | BufferMemory + PostgresMemory |
| Tool Executor | 工具调度和执行 | LangChain Tool + 自定义Tool |
| VectorStore | 知识库语义检索 | ChromaDB/Milvus/Pinecone |

**高可用设计：**
- 水平扩展：Agent 服务无状态，通过 Redis 共享会话
- 熔断降级：LLM 调用超时 → 降级到本地 Ollama
- 异步处理：长时间任务走消息队列（Celery/RabbitMQ）

### 5.2 设计大模型 API 网关

**需求：**
- 多模型路由（DeepSeek / Qwen / GPT）
- 统一鉴权、限流、计费
- 流式输出透传
- 监控和日志

**架构设计：**

```python
from fastapi import FastAPI, Request, HTTPException
import time
import asyncio
from typing import Optional

class LLMGateway:
    """大模型 API 网关"""
    
    def __init__(self):
        self.models = {}  # model_name -> endpoint config
        self.rate_limiter = {}
        self.api_keys = set()
    
    def register_model(self, name: str, endpoint: str, api_key: str, 
                       rate_limit: int = 60):
        """注册模型"""
        self.models[name] = {
            "endpoint": endpoint,
            "api_key": api_key,
            "rate_limit": rate_limit,
            "client": LLMClient(api_key, endpoint, name)
        }
    
    async def route(self, request: Request):
        """路由请求"""
        body = await request.json()
        model = body.get("model", "")
        
        # 1. 鉴权
        api_key = request.headers.get("Authorization", "").replace("Bearer ", "")
        if api_key not in self.api_keys:
            raise HTTPException(401, "Invalid API key")
        
        # 2. 选择模型
        if model not in self.models:
            raise HTTPException(400, f"Model {model} not supported")
        
        model_config = self.models[model]
        
        # 3. 限流检查
        # (简化版，生产用 Redis + 滑动窗口)
        
        # 4. 转发请求
        is_stream = body.get("stream", False)
        if is_stream:
            return StreamingResponse(
                self._forward_stream(model_config, body),
                media_type="text/event-stream"
            )
        else:
            result = await self._forward_chat(model_config, body)
            return result
    
    async def _forward_stream(self, config, body):
        """透传流式响应"""
        async for chunk in config["client"].achat_stream(body["messages"]):
            yield f"data: {json.dumps(chunk)}\n\n"
    
    async def _forward_chat(self, config, body):
        """转发普通请求"""
        return config["client"].chat(body["messages"])
```

### 5.3 设计多记忆层级的对话系统

**三层记忆架构：**

```
第一层：短期记忆（会话内）
├── 存储：内存列表
├── 容量：最近 N 轮对话（受 Token 限制）
├── 淘汰策略：滑动窗口 / Token 计数裁剪
└── 用途：保持当前对话上下文连贯

第二层：长期记忆（用户级）
├── 存储：PostgreSQL / Redis
├── 容量：整个用户会话历史
├── 检索：按 session_id 精确查询
└── 用途：用户偏好、历史对话、重要信息

第三层：知识记忆（全局）
├── 存储：VectorStore（ChromaDB / Milvus）
├── 容量：百万级文档
├── 检索：语义相似度（Top-K）
└── 用途：知识库问答、RAG
```

```python
class HierarchicalMemory:
    """层级记忆管理器"""
    
    def __init__(self, session_id: str, user_id: str):
        self.session_id = session_id
        self.user_id = user_id
        
        # 第一层：短期
        self.short_term = ConversationBufferMemory(
            max_token_limit=2000,
            return_messages=True
        )
        
        # 第二层：长期 (PostgreSQL)
        self.long_term = PostgresChatMessageHistory(
            connection_string="postgresql://localhost/chatdb",
            session_id=session_id
        )
        
        # 第三层：知识 (VectorStore)
        self.knowledge = None  # 初始化 VectorStore
    
    def get_context(self, query: str) -> list:
        """获取完整上下文"""
        context = []
        
        # 1. 知识记忆（语义检索）
        if self.knowledge:
            docs = self.knowledge.similarity_search(query, k=3)
            context.append({
                "role": "system",
                "content": f"参考知识：\n" + "\n".join(d.page_content for d in docs)
            })
        
        # 2. 长期记忆（最近3轮）
        recent = self.long_term.messages[-6:]  # 3轮=6条
        context.extend(recent)
        
        # 3. 短期记忆（完整）
        st_memory = self.short_term.load_memory_variables({})
        context.extend(st_memory.get("history", []))
        
        return context
```

### 5.4 设计 AI 应用部署架构

**生产环境部署架构：**

```
                        ┌──────────────┐
                        │  DNS/CDN     │
                        │ CloudFlare   │
                        └──────┬───────┘
                               │
                        ┌──────▼───────┐
                        │  Nginx 反向代理│
                        │  SSL 终止     │
                        │  负载均衡     │
                        └──────┬───────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
   ┌──────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐
   │ Web 服务    │    │ AI 服务     │    │ 管理后台    │
   │ React/Vue   │    │ FastAPI     │    │ Admin       │
   │ Nginx 静态  │    │ Uvicorn x4  │    │ Dashboard   │
   └─────────────┘    └──────┬──────┘    └─────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
   ┌──────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐
   │ PostgreSQL  │    │ Redis       │    │ OSS/MinIO  │
   │ (主从)       │    │ (缓存/会话) │    │ (对象存储)  │
   └─────────────┘    └─────────────┘    └─────────────┘
          │                   │                   │
          └───────────────────┼───────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │  Docker Compose   │
                    │  / Kubernetes     │
                    └───────────────────┘
```

**CI/CD 流程：**
1. 代码提交 → GitHub Actions 触发
2. 单元测试 + Lint 检查
3. Docker 构建并推送镜像
4. 部署到测试环境（自动）
5. 集成测试 → 部署到生产（手动审批）
6. LangSmith 监控上线质量

---

## 六、常见坑点与最佳实践

| 编号 | 坑点 | 问题描述 | 解决方案 | 最佳实践 |
|------|------|---------|---------|---------|
| 1 | **Token 超出限制** | 对话历史过长，超过模型最大 Token | 使用 TokenBufferMemory 裁剪；启用 SummaryMemory 压缩 | 设置 `max_token_limit`，定期总结历史 |
| 2 | **工具调用解析失败** | LLM 返回的 JSON 格式错误，导致 Agent 崩溃 | 设置 `handle_parsing_errors=True`；使用 Pydantic 验证 | 加异常捕获 + 重试机制 |
| 3 | **Agent 死循环** | Agent 反复调用同一个工具不停止 | 设置 `max_iterations` 限制；工具加条件判断 | `max_iterations=5-10`，加超时控制 |
| 4 | **API Key 泄露** | 硬编码 API Key 到代码中 | 使用环境变量 + .env 文件 + secrets 管理 | 生产用密钥管理服务（Vault/KMS） |
| 5 | **记忆混乱** | 多用户共享 Memory 实例 | 使用 session_id 隔离；每次请求用独立 memory | 用 PostgresChatMessageHistory 做会话隔离 |
| 6 | **流式输出不完整** | Streaming 中断或最后一段丢失 | 前端检查 `[DONE]` 标记；服务端超时重连 | 实现断点续传 + 心跳检测 |
| 7 | **SQL 注入风险** | 记忆内容拼接到 SQL 查询 | 使用参数化查询；ORM 框架 | 永远不要拼接 SQL 字符串 |
| 8 | **模型幻觉** | LLM 生成不存在的菜谱或数据 | 结合 Tavily 搜索验证；加知识库约束 | RAG 架构 + 事实核查提示 |
| 9 | **并发安全问题** | 多个请求同时修改同一 Memory | 使用线程锁 / 异步队列；数据库事务 | Redis 分布式锁 + 乐观锁 |
| 10 | **Embedding 维度过高** | 向量维度过大导致检索慢 | 降维（PCA）；量化（IVF）；选择合适的 Embedding 模型 | 小规模用 ChromaDB，大规模用 Milvus |
| 11 | **接口兼容性问题** | 不同模型的 API 格式有差异 | 统一使用 LangChain Model 封装；适配层模式 | LangChain 已处理大部分兼容性问题 |
| 12 | **OSS 上传失败** | 大文件/并发上传超时 | 分片上传（multipart）；断点续传 | 设置合理超时；加重试逻辑（指数退避） |

---

## 七、面试回答模板（Top 5高频题的结构化回答模板）

### 7.1 "LangChain 中 Agent 的工作原理是什么？"

**回答框架（STAR 法）：**

**S（核心概念）：** Agent 是 LLM 应用的决策引擎，它让 LLM 能够自主选择并调用工具来完成任务，本质是"LLM + Tools + Memory + Planning"的组合。

**T（工作流程）：**
1. 用户输入触发 Agent
2. LLM 根据当前对话 + 可用工具 Schema 判断是否需要调用工具
3. LLM 输出 `tool_calls`（JSON 格式，包含工具名和参数）
4. Agent 执行对应工具，结果包装为 ToolMessage
5. 工具结果回传 LLM，LLM 生成最终回复
6. 如果还有需要，跳转到步骤 2 继续循环

**A（具体实现）：**
```python
agent = create_tool_calling_agent(llm, tools, prompt)
executor = AgentExecutor(agent=agent, tools=tools, max_iterations=5)
result = executor.invoke({"input": "北京天气怎么样？"})
```

**R（关键点）：**
- Agent 依赖于模型 Function Calling 能力
- 需要设置 `max_iterations` 防止死循环
- 工具定义的质量直接影响 Agent 效果

### 7.2 "短期记忆和长期记忆分别如何实现？"

**回答框架：**

**短期记忆实现（在对话上下文中）：**
```python
from langchain.memory import ConversationBufferMemory
memory = ConversationBufferMemory(max_token_limit=2000, return_messages=True)
```
- 存储在内存列表中
- 受 Token 限制，滑动窗口裁剪
- 每次请求携带完整历史

**长期记忆实现（持久化存储）：**
```python
from langchain.memory import PostgresChatMessageHistory, VectorStoreRetrieverMemory
# 数据库持久化
history = PostgresChatMessageHistory(connection_string="...", session_id="uid")
# 向量检索记忆
vector_memory = VectorStoreRetrieverMemory(
    retriever=VectorStoreRetriever(vectorstore=Chroma(...)),
    memory_key="relevant_history"
)
```

**对比总结：**

| 维度 | 短期记忆 | 长期记忆 |
|------|---------|---------|
| 存储位置 | 内存 | 数据库/向量库 |
| 检索方式 | 全量加载 | 按需查询 |
| 容量 | ~2000 tokens | 无限 |
| 生命周期 | 会话结束即销毁 | 永久保存 |
| 适用场景 | 当前对话连贯性 | 用户偏好、历史知识 |

### 7.3 "大模型 API 调用的最佳实践？"

**回答框架（五大实践）：**

1. **错误处理与重试：**
   ```python
   from tenacity import retry, stop_after_attempt, wait_exponential
   @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2))
   def call_llm(messages):
       return llm.invoke(messages)
   ```

2. **流式输出提升体验：**
   - 设置 `stream=True` 实现逐 token 输出
   - 前端 SSE 接收，实时展示
   - 降低用户等待感知

3. **Token 管理：**
   - 计算每次请求的 Token 消耗
   - 设置 `max_tokens` 防止异常输出
   - 用 `tiktoken` 提前计算 Token 数

4. **模型降级策略：**
   - 主模型失败 → 切换到备用模型
   - 云端超时 → 降级到本地 Ollama
   - 关键任务用高质量模型，批量任务用低成本模型

5. **安全与成本：**
   - API Key 使用环境变量，不硬编码
   - 设置用量告警和预算上限
   - 使用 LangSmith 追踪 Token 消耗

### 7.4 "如何设计和实现 Tool Calling？"

**回答框架：**

**第一步：定义工具函数**
```python
@tool
def get_stock_price(symbol: str) -> float:
    """获取股票实时价格"""
    return call_stock_api(symbol)
```

**第二步：LangChain 自动生成 Schema**
- 函数名 → tool name
- 函数参数 → parameters.properties
- 文档字符串 → description
- 必需参数 → required（无默认值的参数）

**第三步：绑定到 LLM**
```python
llm_with_tools = llm.bind_tools(tools)
response = llm_with_tools.invoke("查询AAPL股价")
# response.tool_calls = [{"name": "get_stock_price", "args": {"symbol": "AAPL"}}]
```

**第四步：执行与回传**
```python
for tool_call in response.tool_calls:
    tool_result = tools_map[tool_call["name"]](**tool_call["args"])
    messages.append(ToolMessage(content=str(tool_result), tool_call_id=tool_call["id"]))
final = llm.invoke(messages)
```

**设计原则：**
- 工具名清晰：动词开头，如 `search_recipe`、`calculate_calories`
- 参数描述完整：LLM 依赖描述来正确传参
- 工具粒度适中：一个工具做一件事（单一职责）
- 错误处理完善：工具内部 try-catch，返回友好错误信息

### 7.5 "AI 应用从开发到部署的完整流程？"

**回答框架（6个阶段）：**

**阶段一：原型验证**
- 用 Jupyter Notebook 或 Python 脚本快速验证思路
- 本地用 Ollama 跑开源模型
- 核心链路通：Prompt → LLM → Output

**阶段二：框架搭建**
- LangChain 组织代码：Chain / Agent / Tool / Memory
- PromptTemplate 管理提示词
- LangSmith 开启 Tracing 调试

**阶段三：服务化（FastAPI）**
```python
@app.post("/chat")
async def chat(request: ChatRequest):
    return await agent_executor.ainvoke({"input": request.query})
```

**阶段四：存储与数据**
- PostgreSQL：对话记录、用户数据
- VectorStore：知识库文档
- OSS（阿里云OSS/MinIO）：日志、文件、图片

**阶段五：监控与评估**
- LangSmith：Token 消耗、延迟、反馈
- 构建测试数据集，Prompt 回归测试
- 用户反馈收集 + 持续优化

**阶段六：部署与运维**
- Docker 容器化，Kubernetes 编排
- Nginx 反向代理 + 负载均衡
- CI/CD 自动化部署
- 资源监控（CPU/Memory/GPU）
- 弹性伸缩：根据请求量自动扩缩容

**关键考量：**
- 成本控制：缓存 + 模型选择 + Token 优化
- 安全性：输入过滤 + 输出审核 + 数据隔离
- 可用性：多活部署 + 降级策略 + 熔断机制

---

## 八、快速查漏补缺 Checklist

| 序号 | 知识点 | 掌握程度 |
|------|--------|---------|
| 1 | 神经网络的前馈传播与反向传播 | ⬜ |
| 2 | Transformer 自注意力机制原理 | ⬜ |
| 3 | LLM 三阶段训练（预训练→SFT→RLHF） | ⬜ |
| 4 | OpenAI 兼容 API 请求/响应格式 | ⬜ |
| 5 | LangChain 核心组件（Model/Prompt/Memory/Tool/Agent） | ⬜ |
| 6 | SystemMessage / HumanMessage / AIMessage / ToolMessage 的区别 | ⬜ |
| 7 | 5种 Memory 类型及适用场景 | ⬜ |
| 8 | Agent 执行循环流程（入→判断→调用→回传→出） | ⬜ |
| 9 | Tool 定义方式（@tool / Tool / StructuredTool） | ⬜ |
| 10 | LangChain Tool 自动生成 Schema 的规则 | ⬜ |
| 11 | Tavily 搜索引擎集成 | ⬜ |
| 12 | LangSmith Tracing 配置和使用 | ⬜ |
| 13 | LangSmith Dataset + Testing 创建 | ⬜ |
| 14 | Ollama 本地部署和调用 | ⬜ |
| 15 | DeepSeek API 调用的参数配置 | ⬜ |
| 16 | 阿里云百炼平台的基本能力 | ⬜ |
| 17 | 流式输出服务端（FastAPI SSE） + 前端实现 | ⬜ |
| 18 | Structured Output（with_structured_output / Pydantic） | ⬜ |
| 19 | Function Calling 与 Tool Calling 的区别 | ⬜ |
| 20 | 阿里云 OSS 对接（文件上传/日志存储） | ⬜ |
| 21 | Agent 防死循环（max_iterations + 条件判断） | ⬜ |
| 22 | 多用户隔离（session_id + 独立 Memory） | ⬜ |
| 23 | Prompt 模板管理（ChatPromptTemplate + FewShotPromptTemplate） | ⬜ |
| 24 | 思维链（Chain-of-Thought）提示词设计 | ⬜ |
| 25 | API 错误处理和重试机制（tenacity） | ⬜ |
| 26 | 多模型路由和降级策略 | ⬜ |
| 27 | VectorStore 知识库构建流程 | ⬜ |
| 28 | RAG 架构（Retrieve-Augment-Generate） | ⬜ |
| 29 | Docker 容器化 AI 应用 | ⬜ |
| 30 | AI 私厨 Agent 完整项目架构 | ⬜ |

---

> 🎯 **面试核心策略：**
> 1. **基础概念区**（一问一答）：快速准确，用关键词 + 一句话总结
> 2. **深度原理区**（链式追问）：展示知识体系完整性，从底层到应用
> 3. **代码实战区**（手写 + 解释）：用 Python 代码证明工程能力
> 4. **系统设计区**（白板 + 架构图）：展示架构思维和全局视野
> 5. **常见坑点**：用亲身经历的语气讲"之前在生产上遇到过……"
>
> 💡 **加分项：** 提到 AI 私厨项目经验、LangSmith 监控实践、Ollama + DeepSeek 成本优化方案
