# 快速精通 GPT（OpenAI）

> OpenAI 的 GPT 系列是当前最主流、生态最成熟的闭源大语言模型。从 API 调用到高阶技巧，本文一网打尽。

---

## 目录

1. [GPT 是什么](#1-gpt-是什么)
2. [模型全景图](#2-模型全景图)
3. [核心概念深入](#3-核心概念深入)
4. [API 完整实战](#4-api-完整实战)
5. [进阶能力](#5-进阶能力)
6. [成本与优化](#6-成本与优化)
7. [Prompt 工程](#7-prompt-工程)
8. [最佳实践](#8-最佳实践)
9. [常见问题](#9-常见问题)

---

## 1. GPT 是什么

**Generative Pre-trained Transformer** —— 生成式预训练 Transformer。OpenAI 从 2018 年 GPT-1 起步，到 2023 年 GPT-4 引爆全球，再到 2025~2026 年的 GPT-4.1、o3/o4-mini、GPT-5.5，一路定义了大语言模型的能力边界。

GPT 的核心能力：
- **文本生成**：续写、总结、翻译、改写、创意写作
- **代码能力**：生成、解释、重构、调试、转换语言
- **推理能力**：逻辑推导、数学证明、多步规划
- **多模态理解**：图片输入（GPT-4o / GPT-4.1）
- **工具使用**：Function Calling，调用外部 API
- **结构化输出**：保证输出符合 JSON Schema

---

## 2. 模型全景图

### 2.1 当前主力模型（2026 年 5 月）

| 模型 | 定位 | Context | 速度 | 多模态 | 典型场景 |
|------|------|---------|------|--------|----------|
| **GPT-5.5** | 最新旗舰 | 1M tokens | 中 | 是 | 最强综合能力，复杂 Agent |
| **GPT-4.1** | 编码旗舰 | 1M tokens | 中 | 是 | 代码生成、审查、重构，指令跟随极好 |
| **GPT-4o** | 多模态主力 | 128K tokens | 快 | 全文/图/音 | 日常对话、内容创作、图片分析 |
| **GPT-4o-mini** | 经济首选 | 128K tokens | 极快 | 是 | 简单分类、标签、聊天、批处理 |
| **o3** | 深度推理 | 200K tokens | 慢 | 否 | 数学竞赛、复杂逻辑、PhD 级科学推理 |
| **o4-mini** | 轻量推理 | 200K tokens | 快 | 否 | 代码审查、逻辑校验、成本敏感的推理任务 |

### 2.2 模型选择决策树

```
需要多模态（图片/音频）？
├── 是 → GPT-4.1（编码+多模态）或 GPT-4o（纯多模态）
└── 否 → 需要深度推理？
    ├── 是 → o3（最强推理）或 o4-mini（轻量推理）
    └── 否 → 简单任务？批处理？
        ├── 是 → GPT-4o-mini
        └── 否 → GPT-4.1（综合最强）
```

### 2.3 历史模型演进

| 时间 | 模型 | 里程碑 |
|------|------|--------|
| 2018.06 | GPT-1 | 1.17 亿参数，验证预训练+微调范式 |
| 2019.02 | GPT-2 | 15 亿参数，Zero-shot 涌现能力 |
| 2020.06 | GPT-3 | 1750 亿参数，Few-shot learning |
| 2022.11 | GPT-3.5 / ChatGPT | 引爆全球，RLHF 对齐 |
| 2023.03 | GPT-4 | 多模态，律师考试前 10% |
| 2024.05 | GPT-4o | 原生多模态，音频实时对话 |
| 2024.09 | o1-preview | 首个推理模型，Chain-of-Thought |
| 2024.12 | o1 / o1-mini | 正式推理模型 |
| 2025.04 | GPT-4.1 | 1M context，编码新标杆 |
| 2025.06 | o3 / o4-mini | 第二代推理模型 |
| 2026.03 | GPT-5.5 | 统一推理+对话，1M context |

---

## 3. 核心概念深入

### 3.1 Token

**Token 是大模型的"字"**，所有计费和上下文都以 token 计数。

```
中英文 token 换算（经验值）：
- 1 个英文单词 ≈ 1.3 tokens
- 1 个中文字 ≈ 1.5~2 tokens
- 1 个代码字符 ≈ 0.3 tokens
- 1000 tokens ≈ 750 英文单词 ≈ 500 中文字
```

**在线工具**：[OpenAI Tokenizer](https://platform.openai.com/tokenizer) 可以直接查看任意文本的 token 数。

```python
# 用 tiktoken 精确计算 token 数
import tiktoken

enc = tiktoken.encoding_for_model("gpt-4.1")
tokens = enc.encode("Hello, 你好世界")
print(f"Token 数: {len(tokens)}")  # 输出约 7~8

# 更快捷的方式（不需要 tiktoken）
# pip install tiktoken
```

### 3.2 Context Window（上下文窗口）

模型一次能"看到"的最大 token 数，包括输入和输出。

| 窗口大小 | 实际承载 | 说明 |
|----------|----------|------|
| 128K | ≈ 300 页 PDF | GPT-4o, GPT-4o-mini |
| 200K | ≈ 500 页 PDF | o3, o4-mini |
| 1M | ≈ 2500 页 PDF | GPT-4.1, GPT-5.5 |

**重要**：上下文不是越大越好。超过 70% 填充率时模型可能"忽略中间"（Lost in the Middle 效应）。

### 3.3 Temperature 与 Top-P

两个参数共同控制输出的**随机性**：

```
Temperature (0~2):
  0.0 → 确定性强，每次输出一致（适合数学、代码、分类）
  0.7 → 平衡（适合日常对话）
  1.0+ → 创意强，随机性大（适合写作、头脑风暴）

Top-P (0~1):
  0.1 → 只选最可能的 10% 词（保守）
  0.9 → 选 90% 最可能的词（灵活）
  1.0 → 不限制（最随机）

建议：只调 Temperature，不动 Top-P（默认 1.0）。
```

### 3.4 System Prompt（系统提示词）

放在对话最前面，定义 AI 的**角色、行为规则、输出格式**。

```python
# 好的 System Prompt 结构
system_prompt = """
## 角色
你是一位有着 10 年经验的 Java 架构师。

## 能力
- 精通 Spring Boot、MyBatis、JVM 调优
- 擅长分布式系统设计

## 行为规则
- 回答用中文
- 代码示例优先用 Java 17+
- 涉及性能时给出具体数据，不空谈

## 输出格式
1. 先给出结论（一句话）
2. 再展开解释（分点）
3. 最后给代码示例（如有必要）
"""
```

```python
# 不良示范（太模糊）
bad = "你是一个有帮助的AI助手。"
```

---

## 4. API 完整实战

### 4.1 环境准备

```bash
pip install openai
```

```python
from openai import OpenAI

# 推荐：从环境变量读取 API Key
import os
client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))

# 或者显式传入（开发环境）
client = OpenAI(api_key="sk-proj-xxx")

# 如需代理
client = OpenAI(
    api_key="sk-xxx",
    base_url="https://your-proxy.com/v1"  # 自定义 API 地址
)
```

### 4.2 基础对话

```python
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[
        {"role": "system", "content": "你是 Java 技术专家。"},
        {"role": "user", "content": "volatile 关键字的作用是什么？"}
    ],
    temperature=0.5,
    max_tokens=2048
)

# 提取回复
answer = response.choices[0].message.content
print(answer)

# 获取 token 用量
usage = response.usage
print(f"输入: {usage.prompt_tokens} tokens, 输出: {usage.completion_tokens} tokens")

# 完整响应对象的实用字段
print(f"模型: {response.model}")
print(f"结束原因: {response.choices[0].finish_reason}")  # stop / length / content_filter
print(f"响应 ID: {response.id}")  # 用于追踪和审计
```

### 4.3 流式输出（Streaming）

```python
# 基础流式——最简单的打字机效果
stream = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "写一个 Java 二分查找，带注释"}],
    stream=True
)

for chunk in stream:
    content = chunk.choices[0].delta.content
    if content:
        print(content, end="", flush=True)
print()  # 最后的换行
```

```python
# 生产级流式：收集全文 + 逐块显示
full_response = ""
stream = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "写一个 Java 线程池的完整实现"}],
    stream=True,
    # stream_options={"include_usage": True}  # 需要用量统计时开启
)

for chunk in stream:
    if chunk.choices and chunk.choices[0].delta.content:
        text = chunk.choices[0].delta.content
        full_response += text
        print(text, end="", flush=True)

print(f"\n\n总计 {len(full_response)} 字符")
```

### 4.4 多轮对话

```python
messages = [
    {"role": "system", "content": "你是 Java 面试官。"}
]

# 第一轮
messages.append({"role": "user", "content": "解释 HashMap 的工作原理"})
r1 = client.chat.completions.create(model="gpt-4.1", messages=messages)
messages.append({"role": "assistant", "content": r1.choices[0].message.content})

# 第二轮（基于上下文追问）
messages.append({"role": "user", "content": "那 JDK 8 中 HashMap 有什么优化？"})
r2 = client.chat.completions.create(model="gpt-4.1", messages=messages)
messages.append({"role": "assistant", "content": r2.choices[0].message.content})

# 第三轮
messages.append({"role": "user", "content": "为什么链表转红黑树的阈值是 8？"})
r3 = client.chat.completions.create(model="gpt-4.1", messages=messages)

print(r3.choices[0].message.content)
```

```python
# 状态跟踪：保存每轮的 token 用量
history = []
total_cost = 0

def ask(question):
    history.append({"role": "user", "content": question})
    r = client.chat.completions.create(model="gpt-4.1", messages=history)
    history.append({"role": "assistant", "content": r.choices[0].message.content})
    print(f"[本轮: {r.usage.total_tokens} tokens]")
    return r.choices[0].message.content

ask("什么是 JVM?")
ask("讲一下类加载机制")
ask("双亲委托模型为什么这么设计？")
```

### 4.5 Function Calling（工具调用）

这是 GPT 最强大的能力之一——让模型决定**何时**调用**哪个**外部函数。

```python
# 第一步：定义工具
tools = [
    {
        "type": "function",
        "function": {
            "name": "get_current_weather",
            "description": "获取指定城市当前的天气情况",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {
                        "type": "string",
                        "description": "城市名，如 'Beijing, China'"
                    },
                    "unit": {
                        "type": "string",
                        "enum": ["celsius", "fahrenheit"],
                        "description": "温度单位"
                    }
                },
                "required": ["location"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "search_database",
            "description": "搜索公司内部数据库",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string"},
                    "table": {
                        "type": "string",
                        "enum": ["employees", "projects", "finance"]
                    },
                    "limit": {"type": "integer", "minimum": 1, "maximum": 100}
                },
                "required": ["query", "table"]
            }
        }
    }
]

# 第二步：发起带工具的请求
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "查询 projects 表中关于 AI 的项目，前 5 条"}],
    tools=tools,
    tool_choice="auto"  # 让模型自己决定是否调用工具
)

# 第三步：处理工具调用
msg = response.choices[0].message
if msg.tool_calls:
    for tool_call in msg.tool_calls:
        func_name = tool_call.function.name
        func_args = json.loads(tool_call.function.arguments)
        print(f"模型要调用: {func_name}({func_args})")

        # 实际执行函数
        if func_name == "search_database":
            result = your_db_search(**func_args)

        # 第四步：把结果返回给模型
        messages.append({
            "role": "tool",
            "tool_call_id": tool_call.id,
            "content": json.dumps(result)
        })

    # 第五步：模型基于工具结果生成最终回答
    final_response = client.chat.completions.create(
        model="gpt-4.1",
        messages=messages
    )
    print(final_response.choices[0].message.content)
```

```python
# 好的 function description 范例
# 描述要告诉模型：什么时候用、参数含义、返回值是什么

{
    "type": "function",
    "function": {
        "name": "create_jira_ticket",
        "description": (
            "在 Jira 中创建新工单。"
            "当用户说'记录一个bug'、'建个task'、'提个issue'时调用。"
            "返回创建后的工单链接。"
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "title": {
                    "type": "string",
                    "description": "工单标题，要简洁描述问题（不超过 80 字符）"
                },
                "description": {
                    "type": "string",
                    "description": "详细描述，包括复现步骤、期望行为、实际行为"
                },
                "priority": {
                    "type": "string",
                    "enum": ["P0-紧急", "P1-高", "P2-中", "P3-低"],
                    "description": "优先级，P0 表示线上故障"
                },
                "assignee": {
                    "type": "string",
                    "description": "指派给谁（用户名），不填则为未分配"
                }
            },
            "required": ["title", "priority"]
        }
    }
}
```

### 4.6 Structured Outputs（结构化输出）

保证模型输出**严格符合**你定义的 JSON Schema。

```python
from pydantic import BaseModel
from typing import Optional, Literal

# 定义输出结构
class CodeReview(BaseModel):
    overall_score: float  # 0-10 分
    summary: str
    issues: list[str]
    suggestion: Optional[str] = None
    risk_level: Literal["low", "medium", "high", "critical"]

# 方式一：使用 response_format 参数
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[
        {"role": "system", "content": "你是高级代码审查员。"},
        {"role": "user", "content": """
        审查这段代码：
        ```java
        public void process(String input) {
            String sql = "SELECT * FROM users WHERE name = '" + input + "'";
            statement.execute(sql);
        }
        ```
        """}
    ],
    response_format={
        "type": "json_schema",
        "json_schema": {
            "name": "code_review",
            "schema": CodeReview.model_json_schema(),
            "strict": True  # 严格模式，100% 保证格式
        }
    }
)

result = json.loads(response.choices[0].message.content)
print(json.dumps(result, indent=2, ensure_ascii=False))
```

```python
# 方式二：使用 parse 辅助方法（更简洁）
# 需要 openai >= 1.50.0
completion = client.beta.chat.completions.parse(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "张三，28岁，Java工程师"}],
    response_format={
        "type": "json_schema",
        "json_schema": {
            "name": "person",
            "schema": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "age": {"type": "integer"},
                    "job": {"type": "string"}
                },
                "required": ["name", "age", "job"],
                "additionalProperties": False
            },
            "strict": True
        }
    }
)
# 直接获取解析后的 JSON
person = json.loads(completion.choices[0].message.content)
print(person["name"])  # 张三

# JSON Schema 核心要点：
# - "strict": true 保证 100% 符合 schema（推荐）
# - additionalProperties: false 防止模型添加多余的字段
# - required 字段要明确标出
# - enum 限制可选值，降低出错率
```

### 4.7 图片理解（Vision）

```python
import base64

# 方式一：传入 URL（推荐——省 token）
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "这张架构图中有哪些组件？用列表输出。"},
            {
                "type": "image_url",
                "image_url": {
                    "url": "https://example.com/architecture-diagram.png",
                    "detail": "high"  # low / high / auto
                }
            }
        ]
    }],
    max_tokens=1000
)
print(response.choices[0].message.content)
```

```python
# 方式二：传入本地图片（Base64 编码）
def encode_image(path: str) -> str:
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")

image_b64 = encode_image("screenshot.png")

response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "这张 UI 截图有没有对齐问题？"},
            {
                "type": "image_url",
                "image_url": {
                    "url": f"data:image/png;base64,{image_b64}",
                    "detail": "high"
                }
            }
        ]
    }]
)
```

```python
# detail 参数说明
# "auto"  → 模型自动决定（默认）
# "low"   → 512x512 压缩，低成本，适合图标/简单截图
# "high"  → 高分辨率裁剪，贵但细节好，适合文字密集的文档/图表
```

---

## 5. 进阶能力

### 5.1 Prompt Caching（自动缓存）

GPT-4.1 / GPT-4o 系列**自动**对重复的前缀内容进行缓存，无需任何代码改动。

```python
# 自动缓存的触发条件：
# - 至少 1024 tokens 的相同前缀
# - 在多轮对话中，system + 前几轮对话自动被缓存

# 查看缓存命中情况
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=messages  # system + 长历史
)
# 响应头中有 usage.prompt_tokens_details.cached_tokens
print(response.usage.prompt_tokens_details)

# 最佳实践：
# 1. 把静态内容（system prompt、参考文档）放前面
# 2. 把变化的内容（最新的 user message）放最后
# 3. 相同前缀超过 1024 tokens 就会自动缓存，降价 50%
```

### 5.2 Batch API（离线批处理）

适合不需要实时响应的场景（数据标注、批量翻译、评测），**费用减半**，48 小时内完成。

```python
# 第一步：准备 JSONL 文件
import json

tasks = [
    {"custom_id": "task-1", "method": "POST", "url": "/v1/chat/completions",
     "body": {"model": "gpt-4o-mini", "messages": [
         {"role": "user", "content": "Translate to Chinese: Hello World"}]}},
    {"custom_id": "task-2", "method": "POST", "url": "/v1/chat/completions",
     "body": {"model": "gpt-4o-mini", "messages": [
         {"role": "user", "content": "Translate to Chinese: Good Morning"}]}},
]

with open("batch_input.jsonl", "w", encoding="utf-8") as f:
    for task in tasks:
        f.write(json.dumps(task) + "\n")

# 第二步：上传 + 创建批处理任务
batch_input = client.files.create(
    file=open("batch_input.jsonl", "rb"),
    purpose="batch"
)

batch = client.batches.create(
    input_file_id=batch_input.id,
    endpoint="/v1/chat/completions",
    completion_window="24h"
)

print(f"批处理 ID: {batch.id}, 状态: {batch.status}")

# 第三步：轮询状态
import time
while batch.status not in ["completed", "failed", "cancelled"]:
    time.sleep(30)
    batch = client.batches.retrieve(batch.id)
    print(f"状态: {batch.status}...")

# 第四步：下载结果
if batch.status == "completed":
    content = client.files.content(batch.output_file_id)
    for line in content.text.strip().split("\n"):
        result = json.loads(line)
        print(f"{result['custom_id']}: {result['response']['body']['choices'][0]['message']['content']}")
```

### 5.3 推理模型（o3 / o4-mini）

推理模型会**内部思考**（Chain-of-Thought），对复杂数学、逻辑、编程任务显著更强。

```python
# 推理模型的使用方式与普通模型相同，但注意事项不同
response = client.chat.completions.create(
    model="o4-mini",  # 按需选 o3（最强）或 o4-mini（更便宜）
    messages=[
        {"role": "user", "content": """
        有三个盒子，每个装有两个球。
        盒子1：两个白球
        盒子2：两个黑球
        盒子3：一个白球一个黑球
        随机选一个盒子，随机摸出一个球，是白球。
        问：另一个球也是白球的概率？
        """}
    ],
    # 推理模型不支持：temperature, top_p, system prompt
    # 推理模型支持：max_completion_tokens（不是 max_tokens）
    max_completion_tokens=5000
)

print(response.choices[0].message.content)
# 注意：reasoning_tokens 也会计费，但不显示在 content 中
```

```python
# 推理模型的使用原则：
# 1. Prompt 要简洁直接——不需要"think step by step"
# 2. 不需要 system prompt（会忽略）
# 3. 用 max_completion_tokens 不是 max_tokens
# 4. 适合：数学、编程、科学、法律分析、多步推理
# 5. 不适合：简单 Q&A、创意写作、需要 system prompt 控制风格的场景
```

### 5.4 多模态音频（GPT-4o）

```python
# 音频输入（GPT-4o 支持）
response = client.chat.completions.create(
    model="gpt-4o-audio-preview",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "这段录音在说什么？用中文总结。"},
            {"type": "input_audio", "input_audio": {
                "data": base64.b64encode(open("meeting.mp3", "rb").read()).decode(),
                "format": "mp3"
            }}
        ]
    }]
)
```

---

## 6. 成本与优化

### 6.1 定价速览（2026 年 5 月）

| 模型 | Input $/1M tokens | Output $/1M tokens | Cached Input |
|------|-------------------|--------------------|--------------|
| **GPT-5.5** | $2.50 | $10.00 | $1.25 |
| **GPT-4.1** | $2.00 | $8.00 | $1.00 |
| **GPT-4o** | $2.50 | $10.00 | $1.25 |
| **GPT-4o-mini** | $0.15 | $0.60 | $0.075 |
| **o3** | $10.00 | $40.00 | $5.00 |
| **o4-mini** | $1.10 | $4.40 | $0.55 |

> 推理模型的 reasoning_tokens 按 output 价格计费，但不显示在回复中。

### 6.2 降本策略

```python
# 策略 1：选对模型
# 简单分类 → gpt-4o-mini（不是 gpt-4.1）
# 代码生成 → gpt-4.1（不是 o3）

# 策略 2：缩短 system prompt
# 差距：2000 tokens system prompt vs 200 tokens
# 每次调用省 ~$0.004，每天 1000 次省 $4

# 策略 3：限制 max_tokens
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=messages,
    max_tokens=500  # 不需要长篇回答时严格限制
)

# 策略 4：用 batch API
# 非实时任务走 batch，费用减半

# 策略 5：缓存重复前缀
# 相同的 system prompt + 参考文档 → 自动缓存，费用减半

# 策略 6：清理对话历史
def trim_history(messages, max_tokens=50000):
    """当 token 数超限时，保留 system + 最近 N 轮"""
    total = 0
    kept = []
    # 始终保留 system message
    if messages[0]["role"] == "system":
        kept.append(messages[0])
    # 从后往前保留 user/assistant
    for msg in reversed(messages[1:]):
        total += len(msg["content"]) // 2  # 粗略估算
        if total > max_tokens:
            break
        kept.insert(1, msg)
    return kept
```

---

## 7. Prompt 工程

### 7.1 五个黄金法则

```
1. 角色明确  → "你是 XX 专家，擅长 YY"
2. 任务具体  → 不是"写得好一点"，是"用 Java 17 + 不可变类"
3. 格式限定  → "输出 JSON，包含 name, age, skills 三个字段"
4. 给出例子  → Few-shot：给 1~3 个输入→输出范例
5. 约束边界  → "只回答 Java 相关，否则回复'超出范围'"
```

### 7.2 实战 Prompt 模板

```python
# 模板 1：代码生成
CODE_GEN = """
## 角色
{role}

## 任务
{task}

## 技术栈
{tech_stack}

## 要求
{requirements}

## 输出格式
{output_format}
"""

# 使用
prompt = CODE_GEN.format(
    role="Java 高级工程师",
    task="实现一个线程安全的 LRU 缓存",
    tech_stack="Java 17, 不使用第三方库",
    requirements="1. O(1) 读写 2. 支持过期时间 3. 线程安全",
    output_format="先给完整代码，再逐段解释关键逻辑"
)
```

```python
# 模板 2：代码审查
CODE_REVIEW = """
审查以下代码，按以下维度给出评分（1-10）：

1. 正确性：逻辑是否正确，边界条件是否处理
2. 安全性：有无注入、泄露、权限漏洞
3. 性能：时间复杂度、内存使用
4. 可读性：命名、注释、结构
5. 可维护性：是否好扩展、好测试

最后给出改进后的代码。

代码：
```{language}
{code}
```
"""
```

```python
# 模板 3：翻译（保持技术术语）
TRANSLATE = """
将以下内容翻译为{target_lang}。

规则：
- 技术术语保留英文（如 API, JVM, HashMap）
- 代码块不做任何改动
- 保持原文语气和结构

原文：
{text}
"""
```

### 7.3 Few-Shot 示例

```python
# Few-shot：给模型几个范例，它学会模式
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{
        "role": "user",
        "content": """
将句子转为被动语态：

输入：The cat ate the fish.
输出：The fish was eaten by the cat.

输入：The team completed the project.
输出：The project was completed by the team.

输入：The developer wrote the code.
输出：
"""
    }]
)
# 模型会输出：The code was written by the developer.
```

---

## 8. 最佳实践

### 8.1 错误处理

```python
import time
from openai import (
    RateLimitError, APITimeoutError, APIConnectionError,
    APIError, AuthenticationError
)

def call_with_retry(messages, model="gpt-4.1", max_retries=3):
    """生产级调用：自动重试 + 指数退避"""
    for attempt in range(max_retries):
        try:
            return client.chat.completions.create(
                model=model, messages=messages, timeout=60
            )
        except RateLimitError:
            if attempt < max_retries - 1:
                wait = 2 ** attempt * 5  # 5s, 10s, 20s
                print(f"限流，{wait}s 后重试...")
                time.sleep(wait)
            else:
                raise
        except APITimeoutError:
            if attempt < max_retries - 1:
                print(f"超时，重试中 {attempt + 1}/{max_retries}...")
                time.sleep(2)
            else:
                raise
        except APIConnectionError:
            if attempt < max_retries - 1:
                print(f"连接失败，重试中...")
                time.sleep(5)
            else:
                raise
        except AuthenticationError:
            print("API Key 无效，检查环境变量")
            raise  # 不重试
```

### 8.2 安全红线

```python
# 永远不要这样做：
# ❌ 把 API Key 硬编码在代码里
api_key = "sk-proj-abc123..."

# ❌ 把 API Key 提交到 git
# 检查 .gitignore 中有 .env

# ❌ 把用户输入直接拼接为 SQL（即使让 AI 写 SQL）
# AI 可能被 prompt injection 诱导生成恶意 SQL

# ✅ 正确做法：
# 1. API Key 放环境变量
# 2. 用户输入只有展示作用，不放 system prompt 的可执行部分
# 3. AI 生成的代码必须人工审查后执行
```

### 8.3 日志与监控

```python
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("gpt-api")

def logged_chat(messages, model="gpt-4.1", **kwargs):
    """带日志的调用——生产必备"""
    logger.info(f"调用 {model}, 输入约 {len(str(messages))} 字符")
    start = time.time()

    try:
        response = client.chat.completions.create(
            model=model, messages=messages, **kwargs
        )
        elapsed = time.time() - start
        logger.info(
            f"成功 | {model} | "
            f"耗时 {elapsed:.1f}s | "
            f"输入 {response.usage.prompt_tokens}t | "
            f"输出 {response.usage.completion_tokens}t | "
            f"费用 ≈ ${response.usage.prompt_tokens / 1e6 * 2 + response.usage.completion_tokens / 1e6 * 8:.4f}"
        )
        return response
    except Exception as e:
        elapsed = time.time() - start
        logger.error(f"失败 | {model} | 耗时 {elapsed:.1f}s | {type(e).__name__}: {e}")
        raise
```

---

## 9. 常见问题

### Q1: GPT-4.1 vs GPT-4o 怎么选？
- **写代码、遵循复杂指令** → GPT-4.1（编码能力显著更强）
- **多模态对话、音频** → GPT-4o（音频原生支持）
- **性价比日常任务** → GPT-4o-mini

### Q2: 推理模型什么时候用？
需要**多步推理**时：数学证明、复杂算法设计、法律分析、竞品代码审查。不需要时不用——贵且慢。

### Q3: 1M context 真的能用满吗？
能，但不建议。超过 100K tokens 后模型检索精度下降（Lost in the Middle）。最佳实践是分块+索引，每次只注入相关上下文。

### Q4: Function Calling 返回格式不对怎么办？
- 函数描述要清晰（含使用场景、参数含义、返回值格式）
- 用 `strict: true` 搭配 Structured Outputs
- 对参数加 `enum` 和 `minimum`/`maximum` 约束

### Q5: 如何估算成本？
```python
# 粗略公式（GPT-4.1）
cost = prompt_tokens / 1e6 * 2.0 + completion_tokens / 1e6 * 8.0

# 经验值：
# - 一次中等对话 ≈ 2000 input + 500 output ≈ $0.008
# - 一次完整代码生成 ≈ 3000 input + 2000 output ≈ $0.022
```

---

> **参考资源**
> - [OpenAI 官方文档](https://platform.openai.com/docs)
> - [OpenAI Cookbook](https://cookbook.openai.com/)
> - [Tokenizer 工具](https://platform.openai.com/tokenizer)
> - [Pricing 页面](https://openai.com/api/pricing/)
