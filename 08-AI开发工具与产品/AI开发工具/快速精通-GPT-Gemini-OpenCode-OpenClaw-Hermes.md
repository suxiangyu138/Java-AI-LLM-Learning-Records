# 快速精通 GPT · Gemini · OpenCode · OpenClaw · Hermes

> 五款主流 AI 工具/模型的核心知识点、实战用法与对比，一份文档全打通。

---

## 目录

- [1. GPT（OpenAI）](#1-gptopenai)
- [2. Gemini（Google）](#2-geminigoogle)
- [3. OpenCode](#3-opencode)
- [4. OpenClaw](#4-openclaw)
- [5. Hermes Agent](#5-hermes-agent)
- [6. 横向对比与选型指南](#6-横向对比与选型指南)

---

## 1. GPT（OpenAI）

### 1.1 是什么

OpenAI 的 **Generative Pre-trained Transformer** 系列，目前最主流的闭源大语言模型。提供了从云端 API 到桌面应用（ChatGPT）的完整生态。

### 1.2 模型矩阵（2026年5月）

| 模型 | 定位 | 关键能力 |
|------|------|----------|
| **GPT-4.1** | 旗舰编码模型 | 1M context，最强代码能力，遵循指令极好 |
| **GPT-4o** | 多模态全能 | 文本+图片+音频输入/输出，速度快 |
| **GPT-4o-mini** | 轻量经济 | 成本极低，适合简单任务和批处理 |
| **o3 / o4-mini** | 推理模型 | Chain-of-Thought 深度推理，数学/逻辑/编程 |
| **GPT-5.5** | 最新旗舰 | 统一推理与对话能力，1M+ context |

### 1.3 API 快速上手

```python
# 安装
# pip install openai

from openai import OpenAI

client = OpenAI(api_key="sk-xxx")

# 基础对话
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[
        {"role": "system", "content": "你是资深 Java 后端工程师。"},
        {"role": "user", "content": "解释 JVM 垃圾回收机制"}
    ],
    temperature=0.7,
    max_tokens=4096
)
print(response.choices[0].message.content)
```

```python
# 流式输出（打字机效果）
stream = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "写一个快排算法"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="")
```

```python
# Function Calling（工具调用）
tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "获取指定城市的天气",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "城市名"}
            },
            "required": ["city"]
        }
    }
}]

response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "北京今天天气如何？"}],
    tools=tools,
    tool_choice="auto"
)
```

```python
# Structured Outputs（JSON 模式，保证输出格式）
from pydantic import BaseModel

class UserInfo(BaseModel):
    name: str
    age: int
    skills: list[str]

response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "张三，28岁，会Java和Python"}],
    response_format={"type": "json_schema", "json_schema": {
        "name": "user_info",
        "schema": UserInfo.model_json_schema()
    }}
)
```

### 1.4 核心概念速查

| 概念 | 说明 |
|------|------|
| **Token** | 计费与上下文的基本单位，1 token ≈ 0.75 英文单词 ≈ 0.5 汉字 |
| **Context Window** | 单次对话能携带的最大 token 数（GPT-4.1 = 1M） |
| **System Prompt** | 设定 AI 角色与行为规则的顶层指令 |
| **Temperature** | 0~2，越高越随机/创造性，越低越确定/保守 |
| **Top-P** | 核采样，0~1，限制候选词概率累积阈值 |
| **Function Calling** | 让模型输出结构化函数调用，连接外部工具 |
| **Structured Outputs** | 强制模型输出符合 JSON Schema 的 JSON |
| **Vision** | 图片理解能力（GPT-4o/GPT-4.1 均支持） |

### 1.5 最佳实践

- **System Prompt 放前面**：角色设定 + 输出格式要求 + 约束条件
- **长文档用分块**：超过 70% context 时考虑 RAG 或摘要
- **推理任务用 o3/o4-mini**：数学、逻辑推导、代码审查时切换推理模型
- **批量任务用 Batch API**：48 小时内完成，费用打 5 折
- **Prompt Caching**：重复的 system/user 前缀自动缓存，降低延迟和费用

---

## 2. Gemini（Google）

### 2.1 是什么

Google DeepMind 的**原生多模态**大模型系列，从设计之初就支持文本、图片、音频、视频、代码的混合输入。最大亮点是 **1M~2M token 的超长上下文**。

### 2.2 模型矩阵（2026年5月）

| 模型 | 定位 | Context | 关键能力 |
|------|------|---------|----------|
| **Gemini 2.5 Pro** | 旗舰推理 | 1M（可扩2M） | 最强推理+编码，Thinking 模式 |
| **Gemini 2.5 Flash** | 高速主力 | 1M | 低延迟高吞吐，性价比最优 |
| **Gemini 2.5 Flash Lite** | 极致经济 | 1M | 成本最低，适合简单任务 |

### 2.3 API 快速上手

```python
# pip install google-genai

from google import genai

client = genai.Client(api_key="YOUR_API_KEY")

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
    contents=["描述这张截图中的 UI 布局", image]
)
print(response.text)
```

```python
# 超长上下文：一次性上传整本书
file = client.files.upload(file="book.pdf")
response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents=[file, "总结这本书的核心论点，列出前 10 个"]
)
```

```python
# 流式输出 + Thinking（推理过程可见）
from google.genai import types

response = client.models.generate_content_stream(
    model="gemini-2.5-pro",
    contents="证明：√2 是无理数",
    config=types.GenerateContentConfig(
        thinking_config=types.ThinkingConfig(
            include_thoughts=True  # 返回模型思考过程
        )
    )
)
for chunk in response:
    print(chunk.text, end="")
```

```python
# Google Search Grounding（联网搜索，答案附来源）
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="2026年NBA总决赛冠军是谁？",
    config=types.GenerateContentConfig(
        tools=[types.Tool(google_search=types.GoogleSearch())]
    )
)
# 答案附带 support_chunks 引用来源
```

```python
# 代码执行（沙箱运行 Python）
response = client.models.generate_content(
    model="gemini-2.5-pro",
    contents="计算斐波那契数列第 50 项的值",
    config=types.GenerateContentConfig(
        tools=[types.Tool(code_execution=types.CodeExecution())]
    )
)
# 模型会写代码 → 执行 → 返回结果
```

### 2.4 核心特色

| 特色 | 说明 |
|------|------|
| **原生多模态** | 不是"缝合"的，图片/音频/视频 token 与文本 token 统一训练 |
| **超长上下文 1M/2M** | 相当于一次性处理 1500 页 PDF 或 1 小时视频 |
| **Thinking 模式** | 可查看模型推理思考过程，便于调试和验证 |
| **Google Search Grounding** | 原生联网搜索，自动附来源链接 |
| **Code Execution** | 模型写代码 → 沙箱执行 → 基于结果继续推理 |
| **Context Caching** | 重复使用的长内容只计一次费用 |
| **免费层级** | Gemini 2.5 Flash 提供慷慨的免费额度 |

### 2.5 与 GPT 的 API 关键差异

| 维度 | GPT (OpenAI) | Gemini (Google) |
|------|-------------|-----------------|
| **SDK** | `openai` | `google-genai` |
| **流式** | `stream=True`，迭代 chunks | `generate_content_stream()` |
| **图片** | Base64 或 URL 传入 | PIL Image / bytes 直接传入 |
| **System Prompt** | `role: "system"` | `system_instruction` 参数 |
| **JSON 模式** | `response_format` | `response_mime_type="application/json"` |
| **联网搜索** | 需插件/第三方 | 原生 `GoogleSearch` tool |

---

## 3. OpenCode

### 3.1 是什么

由 **SST（Serverless Stack）团队** 推出的**开源终端 AI 编码代理**，GitHub 14 万+ Stars，月活 750 万+，被称为"开源版 Claude Code"。MIT 协议，完全免费。

> 官网：[opencode.ai](https://opencode.ai) | GitHub：[opencode-ai/opencode](https://github.com/opencode-ai/opencode)

### 3.2 核心特性

- **终端原生 TUI**：精美的终端 UI，纯键盘操作
- **双代理模式**：`build`（开发，全权限）和 `plan`（分析，只读）
- **75+ LLM 提供商**：Claude、GPT、Gemini、DeepSeek、Ollama 本地模型等
- **LSP 集成**：零配置接入 Language Server Protocol，深度理解代码
- **MCP 支持**：Model Context Protocol，可扩展工具链
- **多会话并行**：同时运行多个代理实例
- **客户端/服务器架构**：终端本地运行，远程客户端（含手机端）可接入
- **隐私优先**：代码和对话数据不存储在云端

### 3.3 安装与启动

```bash
# 方式一：快速安装脚本（推荐）
curl -fsSL https://opencode.ai/install | bash

# 方式二：npm 全局安装
npm install -g opencode-ai

# 方式三：Homebrew（macOS）
brew install opencode

# 方式四：Ollama 本地运行（2026 年 4 月新增）
ollama launch opencode
```

```bash
# 启动 OpenCode
cd your-project
opencode
# 进入 TUI 界面后，用自然语言交互即可

# 命令行直接提问（非交互模式）
opencode "解释 ./src/main.go 的架构设计"
```

### 3.4 核心操作

```bash
# 规划模式：只读分析，不写代码
/plan 这个项目的认证模块如何重构？

# 构建模式：完整开发权限
/build
@src/auth/ 实现 JWT 刷新令牌机制

# 指定模型
opencode --model claude-sonnet-4-6

# 使用本地 Ollama 模型
opencode --model ollama/qwen3:32b

# 多会话
Ctrl+T  # 新建会话标签
Ctrl+W  # 关闭当前会话
```

### 3.5 技能（Skills）系统

OpenCode 支持可复用的技能模块：

```yaml
# .opencode/skills/review.yaml
name: code-review
description: 标准的代码审查流程
steps:
  - type: analyze
    prompt: |
      审查以下代码的：
      1. 安全漏洞（SQL注入、XSS等）
      2. 性能瓶颈
      3. 代码规范
      4. 边界条件处理
  - type: report
    format: markdown
```

### 3.6 OpenCode vs Claude Code vs Cursor

| 维度 | OpenCode | Claude Code | Cursor |
|------|----------|-------------|--------|
| **价格** | 免费 | Pro $18/月 | Pro $20/月 |
| **模型选择** | 75+ 任意切换 | 仅 Claude | 多模型 |
| **开源** | MIT 完全开源 | 闭源 | 闭源 |
| **本地模型** | Ollama 原生支持 | 不支持 | 有限支持 |
| **LSP** | 零配置 | 需手动配置 | IDE 自带 |
| **离线可用** | 是（本地模型） | 否 | 否 |

---

## 4. OpenClaw

### 4.1 是什么

由奥地利开发者 **Peter Steinberger** 于 2025 年 11 月创建的开源**个人 AI 代理框架**，36 万+ GitHub Stars，将 AI 从"对话"升级为"自主执行"。俗称"**龙虾**"。

> 核心定位：让你拥有一个 24×7 运行的个人 AI 代理，连接所有通讯平台，自主完成复杂任务。

### 4.2 四层架构

```
┌──────────────────────────────────────────┐
│              Gateway（网关层）              │
│  Telegram / Slack / Discord / 飞书 / 钉钉  │
│       微信 / WhatsApp / iMessage ...      │
├──────────────────────────────────────────┤
│              Agent（代理层）                │
│    自主决策引擎 · 任务规划 · 执行调度        │
├──────────────────────────────────────────┤
│            Skills（技能层）                 │
│  ClawHub 市场 5700+ 技能 · 自定义技能注册   │
├──────────────────────────────────────────┤
│            Memory（记忆层）                 │
│  SOUL.md 长期人格 + 短期上下文 + MEMORY.md  │
└──────────────────────────────────────────┘
```

### 4.3 安装与部署

```bash
# 方式一：Docker（推荐）
docker run -d \
  -v ~/.openclaw:/app/data \
  -e OPENAI_API_KEY=sk-xxx \
  ghcr.io/openclaw/openclaw:latest

# 方式二：Node.js 直接运行
git clone https://github.com/openclaw/openclaw.git
cd openclaw
npm install
cp .env.example .env   # 填写 API Key
npm run start

# 方式三：一键部署脚本
curl -fsSL https://get.openclaw.ai | bash
```

### 4.4 核心配置文件

```yaml
# openclaw.yaml
persona:
  name: "我的 AI 助理"
  role: "资深全栈工程师 + 项目经理"
  tone: "简洁、直接、技术性"

gateways:
  - type: telegram
    token: "${TELEGRAM_BOT_TOKEN}"
  - type: slack
    token: "${SLACK_BOT_TOKEN}"
    signing_secret: "${SLACK_SIGNING_SECRET}"

models:
  default: "claude-sonnet-4-6"
  fast: "gemini-2.5-flash"
  reasoning: "o4-mini"

skills:
  - code-review
  - deploy-notify
  - daily-standup

memory:
  long_term: SOUL.md        # 持久人格与偏好
  episodic: MEMORY.md       # 关键事件与决策
  working: auto             # 自动管理短期上下文

limits:
  max_tokens_per_day: 500000
  max_cost_per_day: 50      # 美元
```

### 4.5 消息平台集成

```yaml
# 支持 12+ 平台，示例配置
gateways:
  - type: telegram
    token: "7483xxx:AAHxxx"
    
  - type: wechat
    app_id: "wx123456"
    app_secret: "abc123"
    
  - type: feishu
    app_id: "cli_xxx"
    app_secret: "yyy"
    
  - type: discord
    token: "MTE0NjYx..."
    
  - type: slack
    token: "xoxb-..."
```

### 4.6 安全问题注意

- **CVE-2026-25253**：已修复的高危 RCE 漏洞（CVSS 8.8），务必更新到最新版
- **ClawHub 技能审计**：约 12% 社区技能曾含恶意代码，安装前审查来源
- **公网暴露风险**：不要在公网无防护部署，务必配置认证和防火墙
- **生产环境**：建议使用 Docker + Nginx 反向代理 + HTTPS

---

## 5. Hermes Agent

### 5.1 是什么

**Nous Research** 于 2026 年 2 月开源的自进化 AI 代理框架，GitHub 1.4 万+ Stars。核心理念：**"The agent that grows with you"** —— 一个会自己学习、自己成长的 AI 代理。

> GitHub：[NousResearch/hermes-agent](https://github.com/NousResearch/hermes-agent)

### 5.2 核心差异化：自我进化

```
┌─────────────────────────────────────────┐
│              使用 Hermes                 │
│    "帮我每天 9 点抓取 TechCrunch 头条"    │
├─────────────────────────────────────────┤
│           Hermes 自主学习                 │
│  1. 分解任务 → 写爬虫脚本 → 设置定时      │
│  2. 将成功经验保存为新 Skill             │
│  3. 下次类似任务自动复用 + 优化           │
├─────────────────────────────────────────┤
│           Skill 自动进化                  │
│  · 抓取失败？自动调整 User-Agent          │
│  · 格式变化？自动适配新 CSS 选择器        │
│  · 网络超时？自动加重试机制              │
└─────────────────────────────────────────┘
```

### 5.3 安装与启动

```bash
# 方式一：npm 全局安装
npm install -g hermes-agent
hermes init       # 初始化配置
hermes start      # 启动代理

# 方式二：Docker
docker run -d \
  -v ~/.hermes:/app/data \
  -e ANTHROPIC_API_KEY=sk-ant-xxx \
  ghcr.io/nousresearch/hermes-agent:latest

# 方式三：Termux（Android 手机运行）
pkg install nodejs
npm install -g hermes-agent
hermes start --mobile
```

### 5.4 核心功能

```bash
# 模型热切换（运行时切换）
/model switch gemini-2.5-flash
/model switch claude-opus-4-7
/model switch grok-4.3

# 技能管理
/skills list              # 列出所有已学技能
/skills create "代码审查"  # 手动创建技能
/skills improve 3          # 优化第 3 号技能

# 定时任务
/cron "每天早上 8 点生成昨日工作总结并发送到飞书"
/cron "每 30 分钟检查 CI 构建状态"

# 子代理
/subagent "分析这个 3000 行的 Java 类，找出性能问题"
# 自动产生隔离的子代理进行分析

# 记忆搜索
/memory "上周我们讨论过的数据库迁移方案"
# FTS5 全文搜索 + LLM 摘要
```

### 5.5 支持的模型（200+）

```bash
# OpenAI 系
hermes --model gpt-4.1
hermes --model gpt-5.5

# Anthropic 系
hermes --model claude-sonnet-4-6
hermes --model claude-opus-4-7

# Google 系
hermes --model gemini-2.5-flash
hermes --model gemini-2.5-pro

# xAI 系（v0.14.0+）
hermes --model grok-4.3

# 国内模型
hermes --model xiaomi/mimo-v2-pro
hermes --model minimax/m2.7

# 本地模型
hermes --model ollama/qwen3:32b
hermes --model huggingface/meta-llama-4
```

### 5.6 支持的平台（17+）

Telegram · Discord · Slack · WhatsApp · Signal · **微信** · **QQ** · iMessage · **钉钉** · **飞书** · Matrix · Email · SMS · Web Dashboard · Termux/Android

### 5.7 Hermes vs OpenClaw

| 维度 | Hermes Agent | OpenClaw |
|------|-------------|----------|
| **开源时间** | 2026年2月 | 2025年11月 |
| **GitHub Stars** | 1.4 万+ | 36 万+ |
| **自我进化** | 核心卖点（自动学技能） | 需手动创建技能 |
| **模型数量** | 200+ | 主流模型 |
| **技能市场** | 内置自动生成 | ClawHub（人工上传） |
| **平台数** | 17+ | 12+ |
| **学习成本** | 较高（功能密集） | 中等 |
| **适合场景** | 希望代理越用越聪明 | 需要稳定可靠的工作流 |

---

## 6. 横向对比与选型指南

### 6.1 一句话总结

| 工具 | 一句话 | 最适合 |
|------|--------|--------|
| **GPT** | 最强闭源模型 + 最成熟 API 生态 | 需要稳定、高质的文本生成/推理 |
| **Gemini** | 超长上下文 + 原生多模态 + 免费额度 | 处理海量文档/多媒体分析 |
| **OpenCode** | 开源终端编码代理，完全免费 | 日常编码，想省 Claude Code 订阅费 |
| **OpenClaw** | 个人 AI 代理框架，24×7 自主运行 | 需要跨平台 AI 助理/自动化 |
| **Hermes** | 自进化代理，越用越聪明 | 希望 AI 长期积累经验、自我优化 |

### 6.2 按场景选型

```
你要做什么？                     → 选哪个？

写代码（单次交互）               → GPT-4.1 / Gemini 2.5 Pro
写代码（终端内全流程）           → OpenCode（免费）或 Claude Code
构建 7×24 AI 助理               → OpenClaw（成熟稳定）或 Hermes（自进化）
分析 500 页 PDF                 → Gemini 2.5 Pro（1M context）
数学证明 / 逻辑推导             → o3 / o4-mini 或 Gemini 2.5 Pro Thinking
多模态（图+文+音+视频）         → Gemini 2.5 Flash
省钱做简单任务                  → GPT-4o-mini / Gemini 2.5 Flash Lite
离线/内网编码                   → OpenCode + Ollama + 本地模型
微信/飞书 AI 机器人             → Hermes Agent（原生支持）
企业级 AI 平台                  → GPT API + OpenAI 生态
```

### 6.3 费用对比

| 工具 | 免费层 | 最低付费 |
|------|--------|----------|
| **GPT-4o-mini** | 无 | $0.15/1M input tokens |
| **GPT-4.1** | 无 | $2/1M input tokens |
| **Gemini 2.5 Flash** | 慷慨免费额度 | 超出后按量付费 |
| **Gemini 2.5 Pro** | 有限免费 | 按量付费 |
| **OpenCode** | 完全免费（自带 Key 或用本地模型） | OpenCode Zen 可选 |
| **OpenClaw** | 开源免费，用自己 API Key | 仅 API 费用 |
| **Hermes Agent** | 开源免费，用自己 API Key | 仅 API 费用 |

### 6.4 学习路径建议

```
第 1 天：GPT API → 掌握基础对话、流式输出、Function Calling
第 2 天：Gemini API → 多模态输入、超长上下文、Search Grounding
第 3 天：OpenCode → 安装 → 在真实项目中编码一天
第 4 天：OpenClaw → Docker 部署 → 接入 Telegram → 配 3 个技能
第 5 天：Hermes → 安装 → 配微信/飞书 → 观察它自动学习
```

---

## 附录：统一 API 调用封装示例

将 GPT 和 Gemini 封装为统一接口：

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
        r = self.client.chat.completions.create(
            model=self.model, messages=messages
        )
        return r.choices[0].message.content

class GeminiClient(LLMClient):
    def __init__(self, api_key: str, model: str = "gemini-2.5-flash"):
        self.client = genai.Client(api_key=api_key)
        self.model = model

    def chat(self, prompt: str, system: str = "") -> str:
        content = f"[System]\n{system}\n\n[User]\n{prompt}" if system else prompt
        r = self.client.models.generate_content(
            model=self.model, contents=content
        )
        return r.text

# 使用
llm = GPTClient(api_key="sk-xxx")
# llm = GeminiClient(api_key="AIza...")
print(llm.chat("解释 CAP 定理", system="你是分布式系统专家"))
```

---

> **持续更新**：本文档会随各工具版本迭代同步更新。建议保留此文件作为日常速查手册。
