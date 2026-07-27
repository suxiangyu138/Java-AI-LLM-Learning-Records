# DeepSeek API 与工程落地实践

> 🔧 从 API 调用到本地部署、从 Prompt 工程到框架集成 —— DeepSeek 工程实践的完整指南

---

## 📚 目录

1. [API 服务概览](#1-api-服务概览)
2. [API 核心能力](#2-api-核心能力)
3. [Prompt 工程最佳实践](#3-prompt-工程最佳实践)
4. [本地部署方案](#4-本地部署方案)
5. [框架集成](#5-框架集成)
6. [生产环境最佳实践](#6-生产环境最佳实践)
7. [成本优化策略](#7-成本优化策略)

---

## 1. API 服务概览

### 1.1 API 信息

| 维度 | 详情 |
|------|------|
| **API 地址** | `https://api.deepseek.com/v1` |
| **协议** | OpenAI 兼容（Chat Completions + Completions） |
| **认证方式** | Bearer Token：`Authorization: Bearer sk-xxx` |
| **申请地址** | https://platform.deepseek.com |
| **官方文档** | https://api-docs.deepseek.com |
| **SDK 支持** | Python, Node.js, 或任何 OpenAI SDK |

### 1.2 模型与定价

| 模型 | 上下文 | 输入价格 | 输出价格 | 说明 |
|------|:-----:|:------:|:------:|------|
| **deepseek-chat** (V3) | 128K | ¥1 / M tokens | ¥2 / M tokens | 通用旗舰 |
| **deepseek-reasoner** (R1) | 128K | ¥4 / M tokens | ¥16 / M tokens | 推理模型 |
| **deepseek-coder** | 128K | ¥1 / M tokens | ¥2 / M tokens | 代码专用 |

> 💡 对比：GPT-4o 价格 ~$5/$15 per 1M tokens，DeepSeek 便宜 **10-20 倍**。

### 1.3 快速开始

```python
# 安装：pip install openai
from openai import OpenAI

client = OpenAI(
    api_key="sk-你的API密钥",
    base_url="https://api.deepseek.com"
)

response = client.chat.completions.create(
    model="deepseek-chat",  # deepseek-chat / deepseek-reasoner
    messages=[
        {"role": "system", "content": "你是一个有帮助的助手"},
        {"role": "user", "content": "你好！请简单介绍一下你自己。"}
    ],
    max_tokens=1024,
    temperature=0.7,
    stream=False
)

print(response.choices[0].message.content)
```

---

## 2. API 核心能力

### 2.1 流式输出（Streaming）

```python
# 流式输出 —— 实时逐字返回
stream = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{"role": "user", "content": "写一首关于春天的七言绝句"}],
    stream=True
)

for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

### 2.2 多轮对话（上下文管理）

```python
# DeepSeek 支持 128K 上下文
messages = [
    {"role": "system", "content": "你是精通Java Spring Boot的高级工程师"},
    {"role": "user", "content": "如何配置 Spring Security 的 JWT 认证？"},
    {"role": "assistant", "content": "首先需要引入依赖...（详细回答）"},
    {"role": "user", "content": "如果我想加 OAuth2 呢？"},
    # 模型能记住之前的对话上下文
]

response = client.chat.completions.create(
    model="deepseek-chat",
    messages=messages,
    max_tokens=2048
)
```

### 2.3 函数调用（Function Calling）

```python
# 定义可用的函数/工具
tools = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "获取指定城市的天气信息",
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "城市名，如：北京、上海"
                    },
                    "unit": {
                        "type": "string",
                        "enum": ["celsius", "fahrenheit"],
                        "description": "温度单位"
                    }
                },
                "required": ["city"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "search_database",
            "description": "搜索内部知识库",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string"},
                    "top_k": {"type": "integer", "default": 5}
                },
                "required": ["query"]
            }
        }
    }
]

response = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{"role": "user", "content": "北京明天天气怎么样？"}],
    tools=tools,
    tool_choice="auto"
)

# 检查是否触发了函数调用
msg = response.choices[0].message
if msg.tool_calls:
    for tool_call in msg.tool_calls:
        func_name = tool_call.function.name
        func_args = json.loads(tool_call.function.arguments)
        print(f"调用函数：{func_name}({func_args})")
        # 执行实际的函数调用...
```

### 2.4 JSON 模式

```python
# 强制输出 JSON 格式
response = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{
        "role": "user",
        "content": "分析这段文本的情感，返回 JSON 格式：{\"sentiment\": \"positive|negative|neutral\", \"confidence\": 0-1}"
    }],
    response_format={"type": "json_object"},
    temperature=0.0  # JSON 模式推荐低温度
)

result = json.loads(response.choices[0].message.content)
print(f"情感：{result['sentiment']}，置信度：{result['confidence']}")
```

### 2.5 FIM 代码补全

```python
# FIM (Fill-in-the-Middle) 代码补全
response = client.completions.create(
    model="deepseek-coder",
    prompt="def quicksort(arr):\n    if len(arr) <= 1:\n        return arr\n    ",
    suffix="\n\n# 测试\nprint(quicksort([3,1,4,1,5,9,2]))",
    max_tokens=200,
    temperature=0.0,
    stop=["\n\n"]
)

print("补全：", response.choices[0].text)
```

---

## 3. Prompt 工程最佳实践

### 3.1 DeepSeek 特有的 Prompt 技巧

| 技巧 | 说明 | 示例 |
|------|------|------|
| **简洁直接** | 不需要冗长的 Few-shot | ✅ "翻译成英文" ❌ 给3个例子再让翻译 |
| **结构化输出** | 明确格式要求 | "以 JSON 格式返回，字段包括..." |
| **角色设定** | System prompt 中明确角色 | "你是一个10年经验的Java架构师" |
| **分步思考** | 对复杂任务要求分步 | "请先分析问题，再给出解决方案" |
| **明确边界** | 告诉模型什么不要做 | "仅解释核心概念，不需要代码示例" |

### 3.2 System Prompt 模板

```text
# 通用助手
你是 DeepSeek，一个由深度求索公司创造的AI助手。
你的回答应当：
- 准确、客观、有用
- 对于不确定的内容明确说明
- 使用清晰的结构化格式
- 代码使用 markdown 代码块

# 代码专家
你是资深软件工程师，精通：
- 语言：Java, Python, TypeScript, Go
- 框架：Spring Boot, React, Django
- 数据库：MySQL, PostgreSQL, Redis, MongoDB
回答时：
- 优先给出可运行的代码
- 解释关键设计决策
- 指出潜在的性能和安全隐患

# 技术文档作者
你是技术文档工程师，擅长：
- 将复杂概念转化为清晰文档
- 使用表格、图表、代码块组织信息
- 保持术语准确性
输出要求：中文 + 英文技术术语
```

### 3.3 不同场景的推荐参数

| 场景 | 模型 | Temperature | Top-P | Max Tokens |
|------|------|:----------:|:-----:|:----------:|
| 代码生成 | deepseek-chat | 0.0-0.3 | 0.95 | 2048-4096 |
| 创意写作 | deepseek-chat | 0.7-0.9 | 0.95 | 2048 |
| 数据分析 | deepseek-chat | 0.1-0.3 | 0.95 | 2048 |
| 翻译 | deepseek-chat | 0.1-0.3 | 0.95 | 1024 |
| 数学推理 | deepseek-reasoner | 0.6 | 0.95 | 4096-8192 |
| 竞赛编程 | deepseek-reasoner | 0.6 | 0.95 | 8192+ |
| JSON/结构化 | deepseek-chat | 0.0 | 0.95 | 根据需求 |
| 对话聊天 | deepseek-chat | 0.6-0.8 | 0.95 | 1024 |

---

## 4. 本地部署方案

### 4.1 部署方案对比

| 方案 | 难度 | 性能 | 显存需求 | 适用场景 |
|------|:--:|:---:|---------|---------|
| **Ollama** | ⭐ | ⭐⭐⭐ | 低 | 个人使用、快速体验 |
| **vLLM** | ⭐⭐ | ⭐⭐⭐⭐⭐ | 中 | 生产级 API 服务 |
| **SGLang** | ⭐⭐ | ⭐⭐⭐⭐ | 中 | 高性能推理 |
| **llama.cpp** | ⭐ | ⭐⭐ | 极低 | CPU 推理 / 边缘设备 |
| **Transformers** | ⭐ | ⭐⭐ | 高 | 研究、实验 |
| **Text Generation Inference** | ⭐⭐⭐ | ⭐⭐⭐⭐ | 中 | 企业级部署 |

### 4.2 Ollama：最简部署

```bash
# 1. 安装 Ollama
# macOS: brew install ollama
# Linux: curl -fsSL https://ollama.com/install.sh | sh
# Windows: 下载安装包 https://ollama.com/download

# 2. 拉取模型
ollama pull deepseek-r1:8b       # R1 蒸馏版 8B
ollama pull deepseek-r1:14b      # R1 蒸馏版 14B
ollama pull deepseek-r1:32b      # R1 蒸馏版 32B
ollama pull deepseek-r1:70b      # R1 蒸馏版 70B（需大显存）
ollama pull deepseek-coder-v2    # Coder V2
ollama pull deepseek-v3          # V3（需极大显存）

# 3. 运行模型
ollama run deepseek-r1:14b

# 4. API 模式运行
ollama serve  # 启动服务
# API 地址：http://localhost:11434
```

```python
# Ollama 兼容 OpenAI API
import openai

client = openai.OpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama"  # 本地不需要真实 key
)

response = client.chat.completions.create(
    model="deepseek-r1:14b",
    messages=[{"role": "user", "content": "用Python写一个二分查找"}]
)
```

### 4.3 vLLM：生产级部署

```bash
# 1. 安装 vLLM
pip install vllm

# 2. 启动服务（单卡）
python -m vllm.entrypoints.openai.api_server \
    --model deepseek-ai/DeepSeek-R1-Distill-Qwen-32B \
    --tensor-parallel-size 1 \
    --max-model-len 32768 \
    --gpu-memory-utilization 0.95 \
    --port 8000

# 3. 多卡部署（TP=4 for 70B）
python -m vllm.entrypoints.openai.api_server \
    --model deepseek-ai/DeepSeek-R1-Distill-Llama-70B \
    --tensor-parallel-size 4 \
    --max-model-len 32768 \
    --port 8000

# 4. 全量 V3/R1 部署（需要 8×A100/H100）
python -m vllm.entrypoints.openai.api_server \
    --model deepseek-ai/DeepSeek-V3 \
    --tensor-parallel-size 8 \
    --max-model-len 65536 \
    --enable-expert-parallel \
    --port 8000
```

### 4.4 硬件需求参考

| 模型 | 量化 | 最小显存 | 推荐显存 | 推荐 GPU |
|------|:---:|:------:|:------:|---------|
| DeepSeek-R1-Distill-Qwen-1.5B | FP16 | 4 GB | 6 GB | GTX 1660+ |
| DeepSeek-R1-Distill-Qwen-7B | FP16 | 16 GB | 24 GB | RTX 3090/4090 |
| DeepSeek-R1-Distill-Qwen-7B | INT4 | 6 GB | 8 GB | RTX 2060+ |
| DeepSeek-R1-Distill-Llama-8B | FP16 | 18 GB | 24 GB | RTX 3090/4090 |
| DeepSeek-R1-Distill-Qwen-14B | FP16 | 30 GB | 48 GB | A6000 / L40S |
| DeepSeek-R1-Distill-Qwen-32B | FP16 | 64 GB | 80 GB | A100 / H100 |
| DeepSeek-R1-Distill-Llama-70B | FP16 | 140 GB | 160 GB | 2×A100 / 4×A100 |
| DeepSeek-V3 (全量) | FP8 | ~350 GB | ~600 GB | 8×A100/H100 |

---

## 5. 框架集成

### 5.1 LangChain 集成

```python
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# 初始化 DeepSeek
llm = ChatOpenAI(
    model="deepseek-chat",
    openai_api_key="sk-xxx",
    openai_api_base="https://api.deepseek.com/v1",
    temperature=0.7
)

# Prompt 模板
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是{role}，擅长{skill}。"),
    ("user", "{input}")
])

# Chain
chain = prompt | llm | StrOutputParser()

result = chain.invoke({
    "role": "Java高级工程师",
    "skill": "Spring Boot微服务架构",
    "input": "设计一个用户认证微服务的架构方案"
})
```

### 5.2 Spring AI 集成

```java
// application.yml
spring:
  ai:
    openai:
      api-key: sk-xxx
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
```

```java
// ChatController.java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return chatClient
            .prompt()
            .user(message)
            .call()
            .content();
    }

    // 流式响应
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody String message) {
        return chatClient
            .prompt()
            .user(message)
            .stream()
            .content();
    }

    // 函数调用
    @PostMapping("/weather")
    public String weather(@RequestParam String city) {
        return chatClient
            .prompt()
            .user("查询" + city + "的天气")
            .function("getWeather", "获取天气", new WeatherFunction())
            .call()
            .content();
    }
}
```

### 5.3 LlamaIndex 集成

```python
from llama_index.core import VectorStoreIndex, SimpleDirectoryReader
from llama_index.llms.openai_like import OpenAILike

# 配置 DeepSeek
llm = OpenAILike(
    model="deepseek-chat",
    api_key="sk-xxx",
    api_base="https://api.deepseek.com/v1",
    temperature=0.1,
    max_tokens=2048
)

# RAG 应用
documents = SimpleDirectoryReader("./docs").load_data()
index = VectorStoreIndex.from_documents(documents)

query_engine = index.as_query_engine(llm=llm)
response = query_engine.query("Spring Security 如何配置 JWT？")
print(response)
```

### 5.4 其他框架集成速查

| 框架 | 集成方式 |
|------|---------|
| **Semantic Kernel** | `OpenAI` connector，修改 endpoint 到 DeepSeek |
| **Dify** | 内置 DeepSeek 模型提供商，直接选择 |
| **FastGPT** | 添加自定义 OpenAI-compatible 模型 |
| **LangFlow** | OpenAI 组件，填入 DeepSeek 的 API 信息 |
| **Flowise** | ChatOpenAI 节点，配置 basePath 和 apiKey |
| **Ollama + Open WebUI** | `ollama pull` 后自动出现在 WebUI 中 |

---

## 6. 生产环境最佳实践

### 6.1 错误处理与重试

```python
import time
from openai import OpenAI, APIError, RateLimitError, APIConnectionError

client = OpenAI(
    api_key="sk-xxx",
    base_url="https://api.deepseek.com",
    max_retries=3,  # SDK 内置重试
    timeout=60.0
)

def safe_chat(messages, max_retries=3):
    """带指数退避的稳健调用"""
    for attempt in range(max_retries):
        try:
            response = client.chat.completions.create(
                model="deepseek-chat",
                messages=messages,
                max_tokens=2048
            )
            return response.choices[0].message.content

        except RateLimitError:  # 限流
            wait = 2 ** attempt
            print(f"限流，等待 {wait}s...")
            time.sleep(wait)

        except APIConnectionError:  # 网络
            wait = 2 ** attempt
            print(f"连接失败，等待 {wait}s...")
            time.sleep(wait)

        except APIError as e:  # 服务端错误
            if e.status_code >= 500:
                wait = 2 ** attempt
                time.sleep(wait)
            else:
                raise  # 4xx 不重试

    raise Exception("达到最大重试次数")
```

### 6.2 并发控制

```python
import asyncio
from asyncio import Semaphore

class DeepSeekClient:
    def __init__(self, api_key, max_concurrency=10):
        self.client = OpenAI(api_key=api_key, base_url="https://api.deepseek.com")
        self.semaphore = Semaphore(max_concurrency)

    async def chat(self, messages):
        async with self.semaphore:
            response = await asyncio.to_thread(
                self.client.chat.completions.create,
                model="deepseek-chat",
                messages=messages
            )
            return response.choices[0].message.content

# 并发调用
async def batch_chat(prompts):
    client = DeepSeekClient("sk-xxx", max_concurrency=5)
    tasks = [client.chat([{"role": "user", "content": p}]) for p in prompts]
    return await asyncio.gather(*tasks)

results = asyncio.run(batch_chat(["问题1", "问题2", "问题3"]))
```

### 6.3 上下文窗口管理

```python
class ContextManager:
    """管理 128K 上下文窗口，自动裁剪历史消息"""

    def __init__(self, max_tokens=100000, reserve_tokens=28000):
        self.max_tokens = max_tokens   # 最大上下文 tokens
        self.reserve_tokens = reserve_tokens  # 为回答保留的空间
        self.system_prompt = None

    def add_system(self, content):
        self.system_prompt = {"role": "system", "content": content}

    def trim_messages(self, messages):
        """按 token 数裁剪消息历史"""
        # 估算：1 token ≈ 2 字符（中文）/ 4 字符（英文）
        total = len(str(self.system_prompt)) // 2 if self.system_prompt else 0

        trimmed = []
        for msg in reversed(messages):  # 从最新往最旧遍历
            msg_tokens = len(str(msg["content"])) // 2
            if total + msg_tokens > self.max_tokens - self.reserve_tokens:
                break
            trimmed.insert(0, msg)
            total += msg_tokens

        if self.system_prompt:
            trimmed.insert(0, self.system_prompt)
        return trimmed
```

### 6.4 安全注意事项

| 关注点 | 建议 |
|--------|------|
| **API Key 安全** | 环境变量存储，不硬编码，定期轮换 |
| **输入过滤** | 对用户输入做长度和内容过滤，防止 Prompt Injection |
| **输出审查** | 对生成内容做敏感词过滤 |
| **速率限制** | 客户端限流 + 处理 429 状态码 |
| **数据隐私** | 敏感数据不发送到 API，或确保企业版数据隔离 |
| **日志脱敏** | 日志中移除 API Key 和 PII 信息 |

---

## 7. 成本优化策略

### 7.1 Token 消耗优化

```text
降低 Token 消耗的方法：

  1. Prompt 精简：
     ✅ "翻译成英文"
     ❌ "请你作为专业的翻译人员，将以下中文内容精准地翻译成英文..."
     节省：50-100 tokens / 请求

  2. System Prompt 缓存：
     → 相同 System Prompt 的连续请求可复用前缀计算

  3. 合理设置 Max Tokens：
     → 不要设太大（避免浪费）
     → 但也要留足余量（避免截断导致重试）

  4. 避免重复上下文：
     → 多轮对话时裁剪不必要的历史
     → 使用摘要而非完整历史

  5. 选择合适的模型：
     → 简单任务用 deepseek-chat
     → 复杂推理才用 deepseek-reasoner
```

### 7.2 缓存策略

```python
import hashlib
import json
from functools import lru_cache

class CachedDeepSeekClient:
    def __init__(self, api_key):
        self.client = OpenAI(api_key=api_key, base_url="https://api.deepseek.com")
        self.cache = {}  # 生产环境建议用 Redis

    def get_cache_key(self, model, messages, temperature):
        content = json.dumps({"model": model, "messages": messages, "temperature": temperature})
        return hashlib.md5(content.encode()).hexdigest()

    def chat(self, model, messages, temperature=0.0, use_cache=True):
        if temperature != 0.0:
            use_cache = False  # 非确定性输出不缓存

        cache_key = self.get_cache_key(model, messages, temperature)

        if use_cache and cache_key in self.cache:
            print("Cache hit!")
            return self.cache[cache_key]

        response = self.client.chat.completions.create(
            model=model, messages=messages, temperature=temperature
        )

        result = response.choices[0].message.content
        if use_cache:
            self.cache[cache_key] = result

        return result
```

### 7.3 成本估算示例

```text
场景：AI 客服系统，日处理 10万次对话

  每次对话：
    → 输入：~500 tokens（用户问题 + 系统提示 + 上下文）
    → 输出：~300 tokens（回答）

  模型：deepseek-chat
    → 输入成本：500 × 10万 × ¥1/M = ¥50/天
    → 输出成本：300 × 10万 × ¥2/M = ¥60/天
    → 每日成本：~¥110
    → 月成本：~¥3,300

  对比 GPT-4o（约 $5/$15 per 1M tokens）：
    → 月成本约 ¥20,000+

  DeepSeek 节省：~85%！💸
```

---

> 🎯 **一句话总结**：DeepSeek API 以 OpenAI 兼容协议 + 10-20x 成本优势 + 本地部署灵活性，为工程落地提供了从个人开发到企业级应用的全链路方案。

---

**下一模块**：[08-DeepSeek生态对比与选型指南](./08-DeepSeek生态对比与选型指南.md) → 横向对比主流模型，帮你做出最佳选择

---

*最后更新：2026年7月*
