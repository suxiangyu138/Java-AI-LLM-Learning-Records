# 06 - GPTs 与自定义应用

> 🎯 GPTs = 不用写代码就能创建 AI 应用。Assistant API = 把 GPT-4 嵌入你的产品。两者结合 = AI 应用开发的最低门槛

---

## 目录

1. [GPTs 是什么](#1-gpts-是什么)
2. [GPTs 配置详解](#2-gpts-配置详解)
3. [Assistant API 实战](#3-assistant-api-实战)
4. [GPTs vs Assistant API](#4-gpts-vs-assistant-api)
5. [Actions 外部 API 对接](#5-actions-外部-api-对接)

---

## 1. GPTs 是什么

GPTs 是 ChatGPT Plus/Team/Enterprise 内的可定制 AI 助手。通过 System Prompt + 知识库 + 内置工具 + 外部 Actions，零代码创建专属 AI 应用。

**核心本质**：把 Prompt 模板 + RAG + Tools 打包成一个可复用的"AI App"。

---

## 2. GPTs 配置详解

```
GPTs = 四个可配置组件：

① Instructions（System Prompt）
   "你是 Java 代码审查专家。按阿里巴巴 Java 规范审查代码。
    输出格式：🔴致命 / 🟡警告 / 🟢建议 分级"

② Knowledge（知识库）
   上传文件：阿里巴巴Java开发手册.pdf
   → GPT 基于上传的文档回答，类似 RAG

③ Capabilities（内置工具）
   Web Browsing（联网搜索）/ DALL-E（图片生成）/ Code Interpreter（代码执行+数据分析）

④ Actions（外部 API 调用）
   定义 OpenAPI Schema → GPT 可调用企业内部 API
   本质 = Function Calling 的可视化配置版
```

---

## 3. Assistant API 实战

```python
from openai import OpenAI
client = OpenAI()

# ① 创建 Assistant（定义行为和工具）
assistant = client.beta.assistants.create(
    name="Java Code Reviewer",
    instructions="审查Java代码，按阿里巴巴规范，输出🔴🟡🟢分级",
    model="gpt-4o",
    tools=[{"type": "code_interpreter"}]
)

# ② 创建 Thread（独立对话会话）
thread = client.beta.threads.create()

# ③ 添加用户消息
client.beta.threads.messages.create(
    thread_id=thread.id, role="user",
    content="审查这段代码：[贴代码]"
)

# ④ 运行 Assistant 并等待结果
run = client.beta.threads.runs.create_and_poll(
    thread_id=thread.id, assistant_id=assistant.id
)

# ⑤ 获取回复
messages = client.beta.threads.messages.list(thread_id=thread.id)
print(messages.data[0].content[0].text.value)
```

---

## 4. GPTs vs Assistant API

| 维度 | GPTs | Assistant API |
|------|:---:|:---:|
| 开发门槛 | 零代码 | 需编程 |
| 分发渠道 | ChatGPT 内置 | 嵌入自己的产品 |
| RAG | 上传文件 | 自定义检索逻辑 |
| 成本 | Plus $20/月 | API 按量计费 |
| 适用 | 个人/小团队 | 产品化/规模化 |

**选型建议**：个人使用 → GPTs（零配置）；产品集成 → Assistant API（完全控制）；需要复杂 RAG → 自建 LangChain + 向量库。

---

## 5. Actions 外部 API 对接

Actions 是 GPTs 最大的差异化能力——让 GPT 直接调用企业内部的 API：

```yaml
# OpenAPI Schema → GPT 自动解析为可调用工具
openapi: 3.1.0
paths:
  /orders/{orderId}:
    get:
      summary: 查询订单详情
      parameters:
        - name: orderId
          in: path
          required: true
          schema:
            type: string
```

配置后 GPT 可自主决定何时查询订单、何时发邮件、何时查数据库——实现轻量级 Agent。

> 🎯 GPTs = 零代码 AI 应用。Assistant API = 嵌入式 GPT-4。知识库 + Tools + Actions 三件套，够用不贵。
