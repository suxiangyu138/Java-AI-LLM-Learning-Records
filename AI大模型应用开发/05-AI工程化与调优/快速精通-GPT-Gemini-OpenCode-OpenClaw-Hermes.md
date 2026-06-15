# ⚙️ 快速精通 GPT · Gemini · OpenCode · OpenClaw · Hermes

> **核心摘要**：五款主流 AI 工具/模型的核心知识点、实战用法与横向对比。涵盖 GPT（最强闭源生态）、Gemini（超长上下文多模态）、OpenCode（开源终端编码代理）、OpenClaw（个人 AI 代理框架）和 Hermes（自进化代理），附统一 API 调用封装示例。

> **前置阅读**：[[快速精通GPT]]、[[快速精通Gemini]]

---

## 目录

1. [GPT（OpenAI）](#1-gptopenai)
2. [Gemini（Google）](#2-geminigoogle)
3. [OpenCode](#3-opencode)
4. [OpenClaw](#4-openclaw)
5. [Hermes Agent](#5-hermes-agent)
6. [横向对比与选型指南](#6-横向对比与选型指南)
7. [附录：统一 API 封装](#7-附录统一-api-封装)

---

## 1. GPT（OpenAI）

### 1.1 模型矩阵

| 模型 | 定位 | 关键能力 |
|---|---|---|
| **GPT-4.1** | 旗舰编码 | 1M context，最强代码能力 |
| **GPT-4o** | 多模态全能 | 文本+图片+音频 |
| **GPT-4o-mini** | 轻量经济 | 成本极低，适合批处理 |
| **o3 / o4-mini** | 推理模型 | Chain-of-Thought 深度推理 |
| **GPT-5.5** | 最新旗舰 | 统一推理与对话 |

### 1.2 API 速查

```python
from openai import OpenAI
client = OpenAI(api_key="sk-xxx")

# 基础对话
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "解释 JVM 垃圾回收"}],
    temperature=0.7
)
print(response.choices[0].message.content)

# 流式输出
stream = client.chat.completions.create(
    model="gpt-4.1", messages=[{"role": "user", "content": "写一个快排"}], stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="")

# Function Calling
tools = [{"type": "function", "function": {
    "name": "get_weather", "description": "获取天气",
    "parameters": {"type": "object", "properties": {"city": {"type": "string"}},
    "required": ["city"]}
}}]
response = client.chat.completions.create(
    model="gpt-4.1", messages=[{"role": "user", "content": "北京天气？"}],
    tools=tools, tool_choice="auto"
)

# Structured Outputs (JSON)
from pydantic import BaseModel
class UserInfo(BaseModel):
    name: str; age: int; skills: list[str]
response = client.chat.completions.create(
    model="gpt-4.1", messages=[{"role": "user", "content": "张三，28岁，会Java"}],
    response_format={"type": "json_schema", "json_schema": {
        "name": "user_info", "schema": UserInfo.model_json_schema()}}
)
```

### 1.3 概念速查

| 概念 | 说明 |
|---|---|
| Token | 计费单位，1 token ≈ 0.75 英文词 ≈ 0.5 汉字 |
| Context Window | 最大 token 数，GPT-4.1 = 1M |
| System Prompt | 设定角色与规则的顶层指令 |
| Temperature | 0~2，越低越确定，越高越随机 |
| Function Calling | 模型输出结构化函数调用 |
| Structured Outputs | 强制输出符合 JSON Schema |

---

## 2. Gemini（Google）

### 2.1 模型矩阵

| 模型 | 定位 | Context | 关键能力 |
|---|---|---|---|
| **Gemini 2.5 Pro** | 旗舰推理 | 1M（可扩2M） | 最强推理+编码，Thinking 模式 |
| **Gemini 2.5 Flash** | 高速主力 | 1M | 低延迟高吞吐，性价比最优 |
| **Gemini 2.5 Flash Lite** | 极致经济 | 1M | 成本最低 |

### 2.2 API 速查

```python
from google import genai
from google.genai import types

client = genai.Client(api_key="YOUR_KEY")

# 基础文本生成
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="用 Java 写一个线程安全的单例模式"
)
print(response.text)

# 多模态：图片理解
import PIL.Image
image = PIL.Image.open("screenshot.png")
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents=["描述这张截图", image]
)

# 超长上下文：上传整本书
file = client.files.upload(file="book.pdf")
response = client.models.generate_content(
    model="gemini-2.5-pro", contents=[file, "总结核心论点"]
)

# 流式 + Thinking
response = client.models.generate_content_stream(
    model="gemini-2.5-pro", contents="证明√2是无理数",
    config=types.GenerateContentConfig(
        thinking_config=types.ThinkingConfig(include_thoughts=True))
)
for chunk in response:
    print(chunk.text, end="")

# 联网搜索
response = client.models.generate_content(
    model="gemini-2.5-flash", contents="2026年NBA总冠军？",
    config=types.GenerateContentConfig(
        tools=[types.Tool(google_search=types.GoogleSearch())])
)
```

### 2.3 GPT vs Gemini API 差异

| 维度 | GPT | Gemini |
|---|---|---|
| SDK | `openai` | `google-genai` |
| 流式 | `stream=True` | `generate_content_stream()` |
| 图片 | Base64 / URL | PIL Image / bytes |
| System Prompt | `role: "system"` | `system_instruction` 参数 |
| JSON 模式 | `response_format` | `response_mime_type` |
| 联网搜索 | 需第三方 | 原生 `GoogleSearch` |

---

## 3. OpenCode

### 3.1 概述

由 **SST 团队** 推出的开源终端 AI 编码代理，GitHub 14 万+ Stars。被称为"开源版 Claude Code"，MIT 协议完全免费。

### 3.2 核心特性

- **终端原生 TUI**：精美终端界面，纯键盘操作
- **双代理模式**：`build`（开发，全权限）和 `plan`（分析，只读）
- **75+ LLM 提供商**：Claude、GPT、Gemini、Ollama 本地模型
- **LSP 集成**：零配置接入 Language Server Protocol
- **MCP 支持**：Model Context Protocol 扩展工具链

### 3.3 安装与使用

```bash
# 安装
curl -fsSL https://opencode.ai/install | bash

# 启动
cd your-project && opencode

# 规划模式（只读分析）
/plan 这个项目的认证模块如何重构？

# 构建模式（完整开发权限）
/build @src/auth/ 实现 JWT 刷新令牌

# 指定本地模型
opencode --model ollama/qwen3:32b
```

### 3.4 对比

| 维度 | OpenCode | Claude Code | Cursor |
|---|---|---|---|
| 价格 | 免费 | Pro $18/月 | Pro $20/月 |
| 模型选择 | 75+ | 仅 Claude | 多模型 |
| 开源 | MIT 完全开源 | 闭源 | 闭源 |
| 本地模型 | Ollama 原生支持 | 不支持 | 有限支持 |

---

## 4. OpenClaw

### 4.1 概述

奥地利开发者 **Peter Steinberger** 创建的开源个人 AI 代理框架，36 万+ GitHub Stars。将 AI 从"对话"升级为"自主执行"。

### 4.2 四层架构

- **Gateway**：Telegram/Slack/Discord/微信/飞书 等 12+ 平台
- **Agent**：自主决策引擎、任务规划、执行调度
- **Skills**：ClawHub 市场 5700+ 技能
- **Memory**：SOUL.md（长期人格）+ MEMORY.md（关键事件）

### 4.3 快速部署

```bash
docker run -d \
  -v ~/.openclaw:/app/data \
  -e OPENAI_API_KEY=sk-xxx \
  ghcr.io/openclaw/openclaw:latest
```

### 4.4 安全提示

- **CVE-2026-25253**：已修复的高危 RCE 漏洞，务必更新到最新版
- **ClawHub 技能审计**：约 12% 社区技能含恶意代码
- **公网部署**：务必配置 Nginx 反向代理 + HTTPS

---

## 5. Hermes Agent

### 5.1 概述

**Nous Research** 开源的自进化 AI 代理框架，GitHub 1.4 万+ Stars。核心理念："The agent that grows with you"。

### 5.2 核心功能

```bash
# 模型热切换
/model switch gemini-2.5-flash

# 技能管理
/skills list
/skills create "代码审查"
/skills improve 3

# 定时任务
/cron "每天早上8点生成昨日工作总结并发送到飞书"

# 子代理
/subagent "分析这个3000行的Java类，找出性能问题"

# 记忆搜索
/memory "上周讨论过的数据库迁移方案"
```

### 5.3 支持的平台

Telegram · Discord · Slack · WhatsApp · 微信 · QQ · 钉钉 · 飞书 · Email · SMS · Web Dashboard

---

## 6. 横向对比与选型指南

### 6.1 一句话总结

| 工具 | 最适合 |
|---|---|
| **GPT** | 需要稳定、高质的文本生成/推理 |
| **Gemini** | 处理海量文档/多媒体分析 |
| **OpenCode** | 日常编码，想省 Claude Code 订阅费 |
| **OpenClaw** | 需要跨平台 AI 助理/自动化 |
| **Hermes** | 希望 AI 长期积累经验、自我优化 |

### 6.2 按场景选型

```
写代码（单次交互）       → GPT-4.1 / Gemini 2.5 Pro
写代码（终端全流程）     → OpenCode（免费）
构建 7x24 AI 助理       → OpenClaw（成熟）或 Hermes（自进化）
分析 500 页 PDF         → Gemini 2.5 Pro（1M context）
数学证明/逻辑推导       → o3 / Gemini 2.5 Pro Thinking
多模态（图+文+音）      → Gemini 2.5 Flash
省钱简单任务             → GPT-4o-mini / Gemini 2.5 Flash Lite
离线内网编码             → OpenCode + Ollama
微信/飞书 AI 机器人      → Hermes Agent
```

---

## 7. 附录：统一 API 封装

```python
from abc import ABC, abstractmethod
from openai import OpenAI
from google import genai

class LLMClient(ABC):
    @abstractmethod
    def chat(self, prompt: str, system: str = "") -> str:
        ...

class GPTClient(LLMClient):
    def __init__(self, api_key: str, model: str = "gpt-4.1"):
        self.client = OpenAI(api_key=api_key)
        self.model = model

    def chat(self, prompt: str, system: str = "") -> str:
        messages = []
        if system:
            messages.append({"role": "system", "content": system})
        messages.append({"role": "user", "content": prompt})
        r = self.client.chat.completions.create(model=self.model, messages=messages)
        return r.choices[0].message.content

class GeminiClient(LLMClient):
    def __init__(self, api_key: str, model: str = "gemini-2.5-flash"):
        self.client = genai.Client(api_key=api_key)
        self.model = model

    def chat(self, prompt: str, system: str = "") -> str:
        content = f"[System]\n{system}\n\n[User]\n{prompt}" if system else prompt
        r = self.client.models.generate_content(model=self.model, contents=content)
        return r.text

# 使用
llm = GPTClient(api_key="sk-xxx")
print(llm.chat("解释 CAP 定理", system="你是分布式系统专家"))
```

---

## 核心要点回顾

- GPT 是生态最成熟的闭源模型，Function Calling 和 Structured Outputs 是核心差异化能力
- Gemini 拥有 1M 超长上下文和原生多模态，免费额度慷慨
- OpenCode 是免费的终端编码代理，支持 75+ 模型和本地 Ollama
- OpenClaw 是成熟的个人 AI 代理框架，适合 7x24 自动化
- Hermes 的核心卖点是自我进化，越用越聪明

## 参考资料

1. OpenAI API：https://platform.openai.com/docs
2. Gemini API：https://ai.google.dev/gemini-api/docs
3. OpenCode：https://opencode.ai
4. OpenClaw：https://github.com/openclaw/openclaw
5. Hermes Agent：https://github.com/NousResearch/hermes-agent
