# 🚀 快速精通 Gemini（Google DeepMind）

> **核心摘要**：Gemini 是 Google DeepMind 打造的原生多模态大模型系列，拥有 1M 超长上下文、联网搜索、代码执行等一站式能力。本文涵盖模型全景、API 完整实战、多模态处理、超长上下文、成本优化与 GPT 迁移指南。

> **前置阅读**：[[大模型API调用实践]]、[[LLM API 全面解析]]

---

## 目录

1. [Gemini 是什么](#1-gemini-是什么)
2. [模型全景图](#2-模型全景图)
3. [核心概念深入](#3-核心概念深入)
4. [API 完整实战](#4-api-完整实战)
5. [超长上下文实战](#5-超长上下文实战)
6. [多模态深入](#6-多模态深入)
7. [高级特性](#7-高级特性)
8. [成本与优化](#8-成本与优化)
9. [GPT 迁移到 Gemini](#9-gpt-迁移到-gemini)
10. [最佳实践](#10-最佳实践)

---

## 1. Gemini 是什么

**Gemini** 是 Google DeepMind 打造的**原生多模态**大语言模型系列。从设计之初即把文本、图片、音频、视频、代码统一训练，而非事后拼接。

### 核心差异化

| 特性 | 说明 |
|---|---|
| **超长上下文** | 1M tokens 起步，可扩展到 2M |
| **原生多模态** | 图片/音频/视频/PDF 直接传入，无需预处理 |
| **Thinking 模式** | 可查看模型内部的推理过程 |
| **Google Search Grounding** | 答案自动附上搜索来源链接 |
| **Code Execution** | 模型写代码 → 沙箱执行 → 基于结果继续推理 |
| **免费层级** | 2.5 Flash 每天免费 1500 次请求 |

---

## 2. 模型全景图

### 2.1 当前主力模型

| 模型 | Context | 速度 | 多模态 | 价格（input/1M） | 适用场景 |
|---|---|---|---|---|---|
| **Gemini 2.5 Pro** | 1M（可扩 2M） | 中 | 全支持 | 按量付费 | 最强推理+编码，复杂 Agent |
| **Gemini 2.5 Flash** | 1M | 快 | 全支持 | 极低（有免费层） | 主力工作马，性价比之王 |
| **Gemini 2.5 Flash Lite** | 1M | 极快 | 全支持 | 最低 | 简单分类/标签/高吞吐 |

### 2.2 模型选择指南

```
任务类型 → 推荐模型
├── 超大文件分析（>200K tokens）→ Gemini 2.5 Pro / Flash
├── 复杂推理 + 编码 → Gemini 2.5 Pro（开启 Thinking）
├── 日常对话、内容生成 → Gemini 2.5 Flash
├── 大规模批处理、简单分类 → Gemini 2.5 Flash Lite
└── 需要联网搜索 → Gemini 2.5 Flash + Search Grounding
```

### 2.3 历史演进

| 时间 | 版本 | 关键进展 |
|---|---|---|
| 2023.12 | Gemini 1.0 | 首次亮相，Ultra/Pro/Nano 三款 |
| 2024.02 | Gemini 1.5 Pro | 1M context，革命性突破 |
| 2024.05 | Gemini 1.5 Flash | 轻量高速，免费开放 |
| 2024.12 | Gemini 2.0 Flash | 速度翻倍，多模态增强 |
| 2025.03 | Gemini 2.5 Pro | 推理+编码登顶多个榜单 |
| 2025.06 | Gemini 2.5 Flash | Flash 线推理增强，Thinking 模式 |

---

## 3. 核心概念深入

### 3.1 原生多模态 vs 拼接多模态

```
拼接多模态（竞品常见）：图片 → 单独的视觉编码器 → 转文本 token → 拼到大模型
原生多模态（Gemini）：图片/音频/视频 → 和文本一起训练 → 模型直接从像素/波形理解
```

### 3.2 Context Caching（上下文缓存）

```python
from google import genai
from google.genai import types

client = genai.Client(api_key="YOUR_API_KEY")

# 上传大文件作为缓存内容
doc = client.files.upload(file="large_document.pdf")

cache = client.caches.create(
    model="gemini-2.5-flash",
    config=types.CreateCachedContentConfig(
        contents=[doc],
        system_instruction="你是专业的文档分析师。",
        ttl="3600s"  # 缓存 1 小时
    )
)

# 使用缓存生成
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="总结文档的第一章",
    config=types.GenerateContentConfig(
        cached_content=cache.name
    )
)
```

### 3.3 Thinking 模式

```python
response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents="证明：在任意 6 个人中，必有 3 人两两相识或两两不相识。",
    config=types.GenerateContentConfig(
        thinking_config=types.ThinkingConfig(
            include_thoughts=True  # 返回思考过程
        )
    )
)

for part in response.candidates[0].content.parts:
    if part.thought:
        print(f"[思考] {part.text}")
    else:
        print(f"[回答] {part.text}")
```

---

## 4. API 完整实战

### 4.1 环境准备

```bash
pip install google-genai
```

```python
from google import genai
from google.genai import types
import os

client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
# 获取 API Key: https://aistudio.google.com/apikey
```

### 4.2 基础文本生成

```python
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="用 Java 17 写一个线程安全的懒汉单例模式，带详细解释"
)
print(response.text)
print(f"输入 tokens: {response.usage_metadata.prompt_token_count}")
print(f"输出 tokens: {response.usage_metadata.candidates_token_count}")
```

### 4.3 System Instruction

```python
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="解释 Spring Boot 的自动配置原理",
    config=types.GenerateContentConfig(
        system_instruction=[
            types.Part.from_text("你是资深的 Java 架构师。回复用中文，代码示例用 Java 17+。")
        ],
        temperature=0.5,
        max_output_tokens=4096,
    )
)
```

> **重点**：Gemini 的 system prompt 是独立参数 `system_instruction`，不混在 messages 中。

### 4.4 多轮对话

```python
chat = client.chats.create(
    model="gemini-2.5-flash",
    config=types.GenerateContentConfig(
        system_instruction=[types.Part.from_text("你是 Java 面试官，会追问答到原理层。")],
        temperature=0.6
    )
)

response = chat.send_message("解释 JVM 内存模型")
print(response.text)
response = chat.send_message("那堆和栈的具体区别是什么？")
print(response.text)
```

### 4.5 流式输出

```python
# 基础流式
response = client.models.generate_content_stream(
    model="gemini-2.5-flash",
    contents="写一个完整的 Java Spring Boot 全局异常处理器"
)

for chunk in response:
    print(chunk.text, end="", flush=True)
print()

# 高级流式：收集全文 + 用量统计
full_text = ""
response = client.models.generate_content_stream(
    model="gemini-2.5-flash",
    contents="用 Java 实现一个布隆过滤器",
    config=types.GenerateContentConfig(temperature=0.3)
)

for chunk in response:
    if chunk.text:
        full_text += chunk.text
        print(chunk.text, end="", flush=True)
```

### 4.6 JSON 模式（结构化输出）

```python
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="列出 5 个常用的 Java 设计模式，每个包含名称、适用场景、描述",
    config=types.GenerateContentConfig(
        response_mime_type="application/json",
        response_schema={
            "type": "object",
            "properties": {
                "patterns": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "name": {"type": "string"},
                            "scenario": {"type": "string"},
                            "description": {"type": "string"}
                        },
                        "required": ["name", "scenario", "description"]
                    }
                }
            },
            "required": ["patterns"]
        }
    )
)

import json
result = json.loads(response.text)
```

### 4.7 Function Calling

```python
search_tool = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="search_products",
            description="搜索产品数据库",
            parameters={
                "type": "object",
                "properties": {
                    "keyword": {"type": "string", "description": "搜索关键词"},
                    "category": {"type": "string", "enum": ["电子产品", "服装", "图书"]},
                },
                "required": ["keyword"]
            }
        )
    ]
)

chat = client.chats.create(
    model="gemini-2.5-flash",
    config=types.GenerateContentConfig(tools=[search_tool])
)

response = chat.send_message("帮我找 200 元以内的蓝牙耳机")
# 执行函数并返回结果
response = chat.send_message(
    types.Part.from_function_response(
        name="search_products",
        response={"products": [{"id": "P001", "name": "小米蓝牙耳机", "price": 149}]}
    )
)
print(response.text)
```

---

## 5. 超长上下文实战

### 5.1 上传大文件

```python
file = client.files.upload(file="book.pdf")
print(f"状态: {file.state}")  # PROCESSING → ACTIVE

# 等待处理完成
import time
while file.state.name == "PROCESSING":
    time.sleep(1)
    file = client.files.get(name=file.name)

response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents=[file, "这本书讲了什么？列出 10 个核心观点。"]
)
print(response.text)
```

### 5.2 视频分析

```python
video = client.files.upload(file="presentation.mp4")

while video.state.name == "PROCESSING":
    time.sleep(5)
    video = client.files.get(name=video.name)

response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents=[video, "分析这个演示视频：演讲者提到了哪几个关键点？整体演讲结构如何？"]
)
print(response.text)
```

### 5.3 文件管理

```python
# 查看已上传文件
for f in client.files.list():
    print(f"{f.name}: {f.display_name} ({f.state.name})")

# 删除不再需要的文件
client.files.delete(name="files/abc123")
```

---

## 6. 多模态深入

### 6.1 图片分析

```python
import PIL.Image

img = PIL.Image.open("architecture_diagram.png")
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents=["分析这个系统架构图：识别所有组件和数据流向", img]
)
print(response.text)
```

### 6.2 音频处理

```python
audio = client.files.upload(file="meeting_recording.mp3")
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents=[audio, "总结会议讨论的主要议题，列出所有待办事项"]
)
```

### 6.3 混合多模态

```python
screenshot = PIL.Image.open("bug_screenshot.png")
error_log = client.files.upload(file="error_stacktrace.txt")

response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents=["我遇到了一个 bug，请帮忙诊断：", screenshot, error_log,
              "上面是报错截图和堆栈日志，请分析根本原因并给出修复方案。"]
)
```

---

## 7. 高级特性

### 7.1 Google Search Grounding（联网搜索）

```python
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="2026 年 NBA 总冠军是谁？",
    config=types.GenerateContentConfig(
        tools=[types.Tool(google_search=types.GoogleSearch())]
    )
)
print(response.text)

# 获取引用来源
if hasattr(response.candidates[0], 'grounding_metadata'):
    meta = response.candidates[0].grounding_metadata
    for chunk in meta.grounding_chunks:
        if chunk.web:
            print(f"来源: {chunk.web.title} - {chunk.web.uri}")
```

### 7.2 Code Execution（代码执行）

```python
response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents="计算斐波那契数列的第 50 项，并验证结果是否正确。",
    config=types.GenerateContentConfig(
        tools=[types.Tool(code_execution=types.CodeExecution())]
    )
)

for part in response.candidates[0].content.parts:
    if part.executable_code:
        print(f"[执行代码]\n{part.executable_code.code}")
    elif part.code_execution_result:
        print(f"[执行结果]\n{part.code_execution_result.output}")
    elif part.text:
        print(f"[分析]\n{part.text}")
```

### 7.3 Safety Settings

```python
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="分析这个网络攻击的特征",
    config=types.GenerateContentConfig(
        safety_settings=[
            types.SafetySetting(
                category=types.HarmCategory.HARM_CATEGORY_HARASSMENT,
                threshold=types.HarmBlockThreshold.BLOCK_ONLY_HIGH
            ),
        ]
    )
)
```

---

## 8. 成本与优化

### 8.1 免费额度

| 模型 | 免费额度 | 说明 |
|---|---|---|
| Gemini 2.5 Flash | 每天 1500 次请求 | 适用所有功能 |
| Gemini 2.5 Pro | 每天 50 次请求 | 适合低频重度推理 |

### 8.2 降本策略

1. **层级递减**：复杂任务 → 2.5 Pro，简单任务 → 2.5 Flash，批处理 → 2.5 Flash Lite
2. **Context Caching**：重复使用的大文件/长 System Instruction → 创建缓存，减少重复计费
3. **限制 `max_output_tokens`**：避免生成不必要的长文本
4. **图片分辨率优化**：不需要高清时使用低分辨率

```python
cache = client.caches.create(
    model="gemini-2.5-flash",
    config=types.CreateCachedContentConfig(
        contents=[large_document],
        ttl="86400s"  # 24 小时
    )
)
```

---

## 9. GPT 迁移到 Gemini

### 9.1 对应关系速查

| 概念 | OpenAI (GPT) | Google (Gemini) |
|---|---|---|
| SDK 包 | `openai` | `google-genai` |
| 初始化 | `OpenAI(api_key=...)` | `genai.Client(api_key=...)` |
| 生成文本 | `chat.completions.create()` | `models.generate_content()` |
| 流式 | `stream=True` 参数 | `generate_content_stream()` 函数 |
| System Prompt | `{"role": "system", ...}` | `system_instruction` 配置参数 |
| 多轮对话 | 手动管理 messages | `client.chats.create()` 自动管理 |
| 图片传入 | Base64 / URL | PIL Image / bytes 直接传入 |
| JSON 模式 | `response_format` | `response_mime_type` |
| Function Calling | `tools` 参数 | `types.Tool` + `function_declarations` |

### 9.2 OpenAI 兼容模式

```python
from openai import OpenAI

client = OpenAI(
    api_key="YOUR_GEMINI_API_KEY",
    base_url="https://generativelanguage.googleapis.com/v1beta/openai/"
)

response = client.chat.completions.create(
    model="gemini-2.5-flash",
    messages=[{"role": "user", "content": "Hello"}]
)
print(response.choices[0].message.content)
```

> **注意**：部分 Gemini 高级特性（Thinking、Search Grounding）在兼容模式下不可用。

---

## 10. 最佳实践

### 10.1 错误处理

```python
from google.genai.errors import ClientError, ServerError

def safe_generate(model, contents, **config_kwargs):
    """带重试的安全生成"""
    import time
    max_retries = 3

    for attempt in range(max_retries):
        try:
            return client.models.generate_content(
                model=model,
                contents=contents,
                config=types.GenerateContentConfig(**config_kwargs)
            )
        except ClientError as e:
            print(f"客户端错误: {e.code} - {e.message}")
            raise
        except ServerError as e:
            if attempt < max_retries - 1:
                wait = 2 ** attempt * 3
                print(f"服务端错误，{wait}s 后重试 ({attempt+1}/{max_retries})")
                time.sleep(wait)
            else:
                raise
```

### 10.2 安全检查

```python
def check_safety(response):
    """检查回复是否被安全过滤"""
    if response.prompt_feedback and response.prompt_feedback.block_reason:
        print(f"输入被拦截: {response.prompt_feedback.block_reason}")
        return False

    if response.candidates:
        candidate = response.candidates[0]
        if candidate.finish_reason.name == "SAFETY":
            print("输出被安全过滤")
            return False
    return True
```

### 10.3 生产检查清单

- [ ] API Key 放环境变量，不硬编码
- [ ] 敏感数据使用付费版（免费版数据可能用于训练）
- [ ] 设置 `safety_settings` 适配业务场景
- [ ] 实现重试逻辑（指数退避）
- [ ] 大文件使用 Context Caching 降低成本
- [ ] 流式输出使用 `generate_content_stream`
- [ ] 记录 `usage_metadata` 做成本追踪

---

## 速查卡片

```python
# 1. 快速开始
from google import genai
from google.genai import types
client = genai.Client(api_key="YOUR_KEY")

# 2. 生成文本
r = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="你的问题",
    config=types.GenerateContentConfig(
        system_instruction=[types.Part.from_text("角色设定")],
        temperature=0.5
    )
)
print(r.text)

# 3. 多轮对话
chat = client.chats.create(model="gemini-2.5-flash")
r1 = chat.send_message("问题1")

# 4. 图片分析
import PIL.Image
img = PIL.Image.open("file.png")
r = client.models.generate_content(
    model="gemini-2.5-flash", contents=["分析这张图", img]
)

# 5. 分析大文件
doc = client.files.upload(file="doc.pdf")
r = client.models.generate_content(
    model="gemini-2.5-pro", contents=[doc, "总结"]
)
```

---

## 核心要点回顾

- Gemini 拥有 1M 超长上下文、原生多模态、Code Execution、Search Grounding 等独特优势
- 推荐模型选择：复杂推理用 2.5 Pro，日常用 2.5 Flash，批处理用 Flash Lite
- 核心 API 差异：System Prompt 为独立参数，流式使用 `generate_content_stream()`
- 超长上下文支持文件上传和视频分析，需等待文件状态变为 ACTIVE
- Context Caching 可显著降低大文件重复处理的成本
- Gemini 提供 OpenAI 兼容模式，便于从 GPT 迁移

## 参考资料

1. Google AI Studio：https://aistudio.google.com/
2. Gemini API 文档：https://ai.google.dev/gemini-api/docs
3. Gemini Cookbook：https://github.com/google-gemini/cookbook
4. Gemini Pricing：https://ai.google.dev/pricing
5. google-genai Python SDK：https://pypi.org/project/google-genai/
