# 快速精通 Gemini（Google DeepMind）

> Google 的原生多模态大模型，1M 超长上下文、联网搜索、代码执行一站式。免费额度慷慨，性价比极高。

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
9. [Prompt 工程（Gemini 特定）](#9-prompt-工程gemini-特定)
10. [GPT 迁移到 Gemini](#10-gpt-迁移到-gemini)
11. [最佳实践](#11-最佳实践)

---

## 1. Gemini 是什么

Google DeepMind 打造的**原生多模态**大语言模型系列。从设计的第一天起就把文本、图片、音频、视频、代码统一训练——不是事后拼接。

### 核心差异化

| 特性 | 一句话 |
|------|--------|
| **超长上下文** | 1M tokens 起步，可扩展到 2M（甩开几乎所有竞品） |
| **原生多模态** | 图片/音频/视频/PDF 直接传入，无需预处理 |
| **Thinking 模式** | 可查看模型内部的推理过程 |
| **Google Search Grounding** | 答案自动附上搜索来源链接 |
| **Code Execution** | 模型写代码 → 沙箱执行 → 基于结果继续推理 |
| **免费层级** | 2.5 Flash 每天免费 1500 次请求 |

---

## 2. 模型全景图

### 2.1 当前主力（2026 年 5 月）

| 模型 | Context | 速度 | 多模态 | 价格（input/1M） | 什么时候用 |
|------|---------|------|--------|-------------------|-----------|
| **Gemini 2.5 Pro** | 1M (可扩2M) | 中 | 全支持 | 按量付费 | 最强推理+编码，复杂 Agent |
| **Gemini 2.5 Flash** | 1M | 快 | 全支持 | 极低（有免费层） | 主力工作马，性价比之王 |
| **Gemini 2.5 Flash Lite** | 1M | 极快 | 全支持 | 最低 | 简单分类/标签/高吞吐 |

### 2.2 模型选择指南

```
你的任务是什么？
├── 超大文件分析（>200K tokens）
│   └── Gemini 2.5 Pro / Flash（都是 1M）
├── 复杂推理 + 编码
│   └── Gemini 2.5 Pro（Thinking 模式开）
├── 日常对话、内容生成、大多数任务
│   └── Gemini 2.5 Flash（速度+成本最优）
├── 大规模批处理、简单分类
│   └── Gemini 2.5 Flash Lite
└── 需要联网搜索
    └── Gemini 2.5 Flash + Google Search Grounding
```

### 2.3 历史演进

| 时间 | 版本 | 关键进展 |
|------|------|----------|
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
"拼接多模态"（竞品常见做法）：
图片 → 单独的视觉编码器 → 转成文本token → 拼到大模型

"原生多模态"（Gemini）：
图片/音频/视频 → 和文本一起训练 → 模型直接从像素/波形理解
好处：跨模态推理更好，比如"这段视频第 12 秒的
      那个人的表情和他说的话一致吗？"
```

### 3.2 Context Caching（上下文缓存）

Gemini 的缓存机制需要**显式创建**，但更灵活：

```python
# 创建缓存（适合有大量静态内容的场景）
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
# 缓存命中后，文档内容不再重复计费
```

### 3.3 Thinking 模式（思考可见）

```python
# Thinking 让模型的推理过程可见
response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents="证明：在任意 6 个人中，必有 3 人两两相识或两两不相识。",
    config=types.GenerateContentConfig(
        thinking_config=types.ThinkingConfig(
            include_thoughts=True  # 返回思考过程
        )
    )
)

# 遍历各部分
for part in response.candidates[0].content.parts:
    if part.thought:  # 思考过程（bool 标志）
        print(f"[思考] {part.text}")
    else:             # 正式回复
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

# 从环境变量读取 API Key
client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])

# 或者显式传入
client = genai.Client(api_key="AIza...")

# 获取 API Key：https://aistudio.google.com/apikey
# 免费额度无需绑定信用卡
```

### 4.2 基础文本生成

```python
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="用 Java 17 写一个线程安全的懒汉单例模式，带详细解释"
)
print(response.text)

# 获取 token 用量
print(f"输入 tokens: {response.usage_metadata.prompt_token_count}")
print(f"输出 tokens: {response.usage_metadata.candidates_token_count}")
print(f"总计 tokens: {response.usage_metadata.total_token_count}")
```

### 4.3 System Instruction（系统指令）

Gemini 的 system prompt 是独立参数，不混在消息里：

```python
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="解释 Spring Boot 的自动配置原理",
    config=types.GenerateContentConfig(
        system_instruction=[
            types.Part.from_text(
                "你是资深的 Java 架构师。回复用中文，代码示例用 Java 17+。"
            ),
        ],
        temperature=0.5,
        max_output_tokens=4096,
    )
)
```

### 4.4 多轮对话

```python
chat = client.chats.create(
    model="gemini-2.5-flash",
    config=types.GenerateContentConfig(
        system_instruction=[
            types.Part.from_text("你是 Java 面试官，会追问答到原理层。")
        ],
        temperature=0.6
    )
)

# 第一轮
response = chat.send_message("解释 JVM 内存模型")
print(response.text)

# 第二轮
response = chat.send_message("那堆和栈的具体区别是什么？画个内存布局图。")
print(response.text)

# 第三轮
response = chat.send_message("为什么字符串常量池要移到堆里？")
print(response.text)

# 查看完整历史
for msg in chat.get_history():
    print(f"[{msg.role}] {msg.parts[0].text[:80]}...")
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
```

```python
# 高级流式：收集全文 + 实时显示 + 用量统计
full_text = ""
response = client.models.generate_content_stream(
    model="gemini-2.5-flash",
    contents="用 Java 实现一个布隆过滤器，包含误判率计算",
    config=types.GenerateContentConfig(temperature=0.3)
)

last_usage = None
for chunk in response:
    if chunk.text:
        full_text += chunk.text
        print(chunk.text, end="", flush=True)
    if chunk.usage_metadata:
        last_usage = chunk.usage_metadata

print(f"\n\n--- 共 {len(full_text)} 字符 ---")
if last_usage:
    print(f"Tokens: {last_usage.prompt_token_count} in / {last_usage.candidates_token_count} out")
```

### 4.6 JSON 模式（结构化输出）

```python
# 方式一：response_mime_type（推荐）
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="""
    列出 5 个常用的 Java 设计模式，
    每个包含：名称、适用场景、一句话描述、代码示例（不超过5行）
    """,
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
                            "description": {"type": "string"},
                            "code_snippet": {"type": "string"}
                        },
                        "required": ["name", "scenario", "description", "code_snippet"]
                    }
                }
            },
            "required": ["patterns"]
        }
    )
)

import json
result = json.loads(response.text)
for p in result["patterns"]:
    print(f"## {p['name']}")
    print(f"  场景: {p['scenario']}")
    print(f"  代码: {p['code_snippet']}\n")
```

```python
# 方式二：在 prompt 中指定（兼容老版本模型）
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="""
    返回一个 JSON 对象，包含 name, age, skills 三个字段。
    不要输出任何 JSON 之外的内容。

    用户：张三，28岁，会Java、Python、K8s
    """
)
# 配合 response_mime_type 更加可靠
```

### 4.7 Function Calling

```python
# 定义函数
search_tool = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="search_products",
            description="搜索产品数据库，返回匹配的产品列表",
            parameters={
                "type": "object",
                "properties": {
                    "keyword": {
                        "type": "string",
                        "description": "搜索关键词"
                    },
                    "category": {
                        "type": "string",
                        "enum": ["电子产品", "服装", "食品", "图书"],
                        "description": "产品类别"
                    },
                    "max_price": {
                        "type": "number",
                        "description": "最高价格（元）"
                    }
                },
                "required": ["keyword"]
            }
        ),
        types.FunctionDeclaration(
            name="place_order",
            description="创建订单",
            parameters={
                "type": "object",
                "properties": {
                    "product_id": {"type": "string"},
                    "quantity": {"type": "integer", "minimum": 1},
                    "address": {"type": "string"}
                },
                "required": ["product_id", "quantity", "address"]
            }
        )
    ]
)

# 多轮工具调用
chat = client.chats.create(
    model="gemini-2.5-flash",
    config=types.GenerateContentConfig(tools=[search_tool])
)

# 用户请求
response = chat.send_message("帮我找 200 元以内的蓝牙耳机")
print(response)  # 这会是一个 function_call

# 执行函数并返回结果
# （实际中你会调用真实 API，这里模拟）
response = chat.send_message(
    types.Part.from_function_response(
        name="search_products",
        response={"products": [
            {"id": "P001", "name": "小米蓝牙耳机 Air3", "price": 149},
            {"id": "P002", "name": "漫步者 TWS1", "price": 179}
        ]}
    )
)
print(response.text)  # 模型会基于结果生成推荐
```

---

## 5. 超长上下文实战

### 5.1 上传大文件

```python
# 上传文件（PDF/视频/音频/文本）
file = client.files.upload(file="book.pdf")
print(f"文件名: {file.name}")
print(f"URI: {file.uri}")
print(f"MIME: {file.mime_type}")
print(f"大小: {file.size_bytes} bytes")
print(f"状态: {file.state}")  # PROCESSING → ACTIVE

# 等待处理完成
import time
while file.state.name == "PROCESSING":
    time.sleep(1)
    file = client.files.get(name=file.name)
    print(".", end="", flush=True)

print(f"\n文件就绪: {file.state.name}")

# 用文件生成回答
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents=[file, "这本书讲了什么？列出 10 个核心观点。"]
)
print(response.text)
```

### 5.2 多文件对比

```python
# 同时分析多个文档
contract_en = client.files.upload(file="contract_en.pdf")
contract_cn = client.files.upload(file="contract_cn.pdf")

response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents=[
        contract_en,
        contract_cn,
        "对比这两份合同的中英文版本，找出 差异之处，包括条款编号和具体内容。用表格呈现。"
    ]
)
```

### 5.3 视频分析

```python
# 上传视频并分析
video = client.files.upload(file="presentation.mp4")

# 等待处理（视频比文档慢）
while video.state.name == "PROCESSING":
    time.sleep(5)
    video = client.files.get(name=video.name)

response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents=[
        video,
        """
        分析这个演示视频：
        1. 演讲者提到了哪几个关键点？
        2. 第 30-60 秒之间展示了什么？
        3. 整体演讲结构如何？
        """
    ]
)
print(response.text)
```

### 5.4 上下文窗口管理

```python
# Gemini 的文件可以多次使用而不重复上传
# files.list() 查看已上传文件
for f in client.files.list():
    print(f"{f.name}: {f.display_name} ({f.state.name})")

# 删除不再需要的文件
client.files.delete(name="files/abc123")

# 大文件最佳实践：
# 1. 提前上传，确保状态为 ACTIVE 再用
# 2. 文件 URI 可复用，不要重复上传同一文件
# 3. 用完删除，释放存储
# 4. 视频文件建议压缩到 720p 以下
```

---

## 6. 多模态深入

### 6.1 图片分析

```python
import PIL.Image

img = PIL.Image.open("architecture_diagram.png")

response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents=[
        "你是一个架构师，分析这个系统架构图：\n"
        "1. 识别所有组件\n"
        "2. 指出数据流向\n"
        "3. 找出潜在的瓶颈或单点故障",
        img
    ]
)
print(response.text)
```

```python
# 多图对比
before = PIL.Image.open("before_refactor.png")
after = PIL.Image.open("after_refactor.png")

response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents=[
        "对比重构前后的两张类图，分析重构解决了什么问题，引入了什么新问题。",
        before, after
    ]
)
```

### 6.2 音频处理

```python
# 上传音频文件
audio = client.files.upload(file="meeting_recording.mp3")

response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents=[
        audio,
        "这是 1 小时项目会议的录音。请：\n"
        "1. 总结会议讨论的主要议题\n"
        "2. 列出所有的待办事项（含负责人）\n"
        "3. 标记有争议的决策点"
    ]
)
```

### 6.3 混合多模态

```python
# 图片 + 文本 + PDF 混合输入
screenshot = PIL.Image.open("bug_screenshot.png")
error_log = client.files.upload(file="error_stacktrace.txt")
api_doc = client.files.upload(file="api_documentation.pdf")

response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents=[
        "我遇到了一个 bug，请帮忙诊断：",
        screenshot,                # UI 报错截图
        "上面的截图是用户的报错界面。",
        error_log,                 # 堆栈日志
        "上面是完整的堆栈信息。",
        api_doc,                   # API 文档
        "上面是对应 API 的文档。请分析根本原因并给出修复方案。"
    ]
)
```

---

## 7. 高级特性

### 7.1 Google Search Grounding（联网搜索）

```python
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="2026 年 NBA 总冠军是谁？FMVP 是谁？",
    config=types.GenerateContentConfig(
        tools=[types.Tool(google_search=types.GoogleSearch())]
    )
)

# 回答中自动包含搜索来源
print(response.text)

# 获取引用来源
# grounding_metadata 包含所有引用的 URL
if hasattr(response.candidates[0], 'grounding_metadata'):
    meta = response.candidates[0].grounding_metadata
    for chunk in meta.grounding_chunks:
        if chunk.web:
            print(f"来源: {chunk.web.title} - {chunk.web.uri}")
```

```python
# Search Grounding 使用场景：
# ✅ 实时信息（天气、股价、新闻、赛事）
# ✅ 需要引用来源的回答
# ✅ 事实核查
# ❌ 私有数据（不会帮你搜内网）
# ❌ 创意写作（不需要搜索）
```

### 7.2 Code Execution（代码执行）

```python
# 模型自动写代码 → 沙箱执行 → 基于结果继续
response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents="计算斐波那契数列的第 50 项（F(50)），并验证结果是否正确。",
    config=types.GenerateContentConfig(
        tools=[types.Tool(code_execution=types.CodeExecution())]
    )
)

# 遍历响应：模型生成代码 → 执行 → 拿到结果 → 继续生成
for part in response.candidates[0].content.parts:
    if part.executable_code:
        print(f"\n[执行代码]\n```python\n{part.executable_code.code}\n```")
    elif part.code_execution_result:
        print(f"\n[执行结果]\n{part.code_execution_result.output}")
    elif part.text:
        print(f"\n[分析]\n{part.text}")
```

```python
# 实用场景：数据分析
response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents="生成 1000 个正态分布的随机数，画出直方图，计算均值和标准差。",
    config=types.GenerateContentConfig(
        tools=[types.Tool(code_execution=types.CodeExecution())]
    )
)
# 模型会用 matplotlib 绘图并返回图片
```

### 7.3 Controlled Generation（受控生成）

```python
# 控制输出的具体格式和约束
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="列出 5 个 Java 异常处理的最佳实践",
    config=types.GenerateContentConfig(
        response_mime_type="application/json",
        response_schema={
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "rank": {"type": "integer"},
                    "principle": {"type": "string"},
                    "bad_example": {"type": "string"},
                    "good_example": {"type": "string"}
                },
                "required": ["rank", "principle", "bad_example", "good_example"],
                "additionalProperties": False
            }
        }
    )
)
```

### 7.4 Safety Settings（安全设置）

```python
# 调整内容安全过滤等级
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="分析这个网络攻击的特征并给出防御建议...",
    config=types.GenerateContentConfig(
        safety_settings=[
            types.SafetySetting(
                category=types.HarmCategory.HARM_CATEGORY_HARASSMENT,
                threshold=types.HarmBlockThreshold.BLOCK_ONLY_HIGH
            ),
            types.SafetySetting(
                category=types.HarmCategory.HARM_CATEGORY_HATE_SPEECH,
                threshold=types.HarmBlockThreshold.BLOCK_ONLY_HIGH
            ),
        ]
    )
)

# 检查是否有安全过滤
try:
    print(response.text)
except ValueError as e:
    print(f"内容被安全过滤: {e}")
    # response.prompt_feedback 包含安全过滤详情
    print(response.prompt_feedback)
```

---

## 8. 成本与优化

### 8.1 免费额度

```
Gemini 2.5 Flash（免费层级）：
- 每天 1500 次请求
- 适用所有功能（含多模态、Search Grounding）
- 数据可能用于改进服务（生产敏感数据用付费版）

Gemini 2.5 Pro（免费层级）：
- 每天 50 次请求
- 适合尝鲜和低频重度推理
```

### 8.2 降本策略

```python
# 策略 1：层级递减
# 复杂任务 → 2.5 Pro，简单任务 → 2.5 Flash，批处理 → 2.5 Flash Lite

# 策略 2：Context Caching
# 重复使用的大文件/长 system instruction → 创建缓存
cache = client.caches.create(
    model="gemini-2.5-flash",
    config=types.CreateCachedContentConfig(
        contents=[large_document],
        ttl="86400s"  # 24 小时
    )
)
# 缓存命中后，存储的 token 不再计费

# 策略 3：限制 max_output_tokens
config = types.GenerateContentConfig(max_output_tokens=500)

# 策略 4：选择合适的分辨率
# 图片不需要高清时用 low detail
# 视频控制在 720p 以下，时长裁剪到关键部分
```

---

## 9. Prompt 工程（Gemini 特定）

### 9.1 Gemini 的 Prompt 偏好

```
✅ 擅长的 Prompt 风格：
- 清晰的结构化指令（分点、编号）
- 直接给角色设定
- 多模态混合提示（图片 + 文字一起问）

⚠️ 注意：
- system_instruction 是单独参数，不要混在 contents 里
- Gemini 对模糊指令的"猜测"倾向弱于 GPT
- 需要更明确的格式要求
```

### 9.2 模板

```python
# 模板 1：代码生成
def generate_code(task, tech_stack, requirements):
    return client.models.generate_content(
        model="gemini-2.5-pro",
        contents=f"""
## 任务
{task}

## 技术栈
{tech_stack}

## 要求
{requirements}

## 输出
1. 先给完整代码
2. 再用 3~5 个要点解释关键逻辑
3. 最后给出使用示例
""",
        config=types.GenerateContentConfig(
            system_instruction=[
                types.Part.from_text("你是资深软件工程师。代码要直接可运行。")
            ],
            temperature=0.3
        )
    )
```

```python
# 模板 2：文档分析（利用长上下文）
def analyze_document(file_path, question):
    doc = client.files.upload(file=file_path)
    return client.models.generate_content(
        model="gemini-2.5-pro",
        contents=[doc, question],
        config=types.GenerateContentConfig(
            max_output_tokens=8000
        )
    )
```

---

## 10. GPT 迁移到 Gemini

### 10.1 对应关系速查

| 概念 | OpenAI (GPT) | Google (Gemini) |
|------|-------------|-----------------|
| SDK 包 | `openai` | `google-genai` |
| 初始化 | `OpenAI(api_key=...)` | `genai.Client(api_key=...)` |
| 生成文本 | `chat.completions.create()` | `models.generate_content()` |
| 流式 | `stream=True` 参数 | `generate_content_stream()` 函数 |
| System Prompt | `{"role": "system", ...}` | `system_instruction` 配置参数 |
| 多轮对话 | 手动管理 messages 列表 | `client.chats.create()` 自动管理 |
| 图片传入 | Base64 URL 或 URL 字符串 | PIL Image / bytes 直接传入 |
| JSON 模式 | `response_format` | `response_mime_type="application/json"` |
| Function Calling | `tools` 参数 | `types.Tool` + `function_declarations` |
| Token 计费 | `response.usage` | `response.usage_metadata` |

### 10.2 OpenAI 兼容模式

```python
# Gemini 也提供 OpenAI 兼容端点
# 可以直接用 openai 包调用 Gemini
from openai import OpenAI

client = OpenAI(
    api_key="YOUR_GEMINI_API_KEY",
    base_url="https://generativelanguage.googleapis.com/v1beta/openai/"
)

response = client.chat.completions.create(
    model="gemini-2.5-flash",
    messages=[
        {"role": "user", "content": "Hello, who are you?"}
    ]
)
print(response.choices[0].message.content)
# 注意：部分 Gemini 高级特性（Thinking、Search）在兼容模式下不可用
```

### 10.3 迁移检查清单

```
□ SDK 替换: openai → google-genai
□ 初始化方式: OpenAI() → genai.Client()
□ System prompt: messages 中 → system_instruction 参数
□ 多轮对话: 手动管理列表 → chats.create()
□ 图片: Base64 → PIL Image / bytes
□ JSON 模式: response_format → response_mime_type
□ 搜索: 无原生支持 → Google Search Grounding
□ 推理模型: o3/o4-mini → 2.5 Pro + Thinking
```

---

## 11. 最佳实践

### 11.1 错误处理

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
            # 4xx 错误——不重试
            print(f"客户端错误: {e.code} - {e.message}")
            raise
        except ServerError as e:
            # 5xx 错误——可重试
            if attempt < max_retries - 1:
                wait = 2 ** attempt * 3
                print(f"服务端错误，{wait}s 后重试 ({attempt+1}/{max_retries})")
                time.sleep(wait)
            else:
                raise
```

### 11.2 安全检查

```python
def check_safety(response):
    """检查回复是否被安全过滤"""
    if response.prompt_feedback:
        feedback = response.prompt_feedback
        if feedback.block_reason:
            print(f"输入被拦截: {feedback.block_reason}")
            return False

    if response.candidates:
        candidate = response.candidates[0]
        if candidate.finish_reason.name == "SAFETY":
            print("输出被安全过滤")
            return False
        if candidate.safety_ratings:
            for rating in candidate.safety_ratings:
                if rating.probability.name in ["HIGH", "MEDIUM"]:
                    print(f"安全警告: {rating.category} - {rating.probability.name}")

    return True
```

### 11.3 生产环境检查清单

```
□ API Key 放环境变量，不硬编码
□ 敏感数据使用付费版（免费版数据可能用于训练）
□ 设置 safety_settings 适配业务场景
□ 实现重试逻辑（指数退避）
□ 大文件使用 Context Caching 降本
□ 流式输出用 generate_content_stream
□ 记录 usage_metadata 做成本追踪
□ 付费版设置 usage limits 防超支
□ 用完的文件 delete 释放存储
```

---

## 速查卡片

```python
# Gemini 最常用的 5 个代码片段

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
r2 = chat.send_message("问题2")

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

> **参考资源**
> - [Google AI Studio](https://aistudio.google.com/) — 免费在线体验
> - [Gemini API 文档](https://ai.google.dev/gemini-api/docs)
> - [Gemini Cookbook](https://github.com/google-gemini/cookbook)
> - [Pricing](https://ai.google.dev/pricing)
