# 03 调用大模型 API 的第一个 Demo

> Level1 的转折点：从纯 Python 进入 AI。用 openai SDK 调用 DeepSeek V4 大模型，理解 OpenAI 兼容协议，跑通"Python 发请求 → 大模型回答"的第一条 AI 链路，再升级为流式与多轮对话。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [OpenAI 兼容协议：先理解再写代码](#2-openai-兼容协议先理解再写代码)
3. [准备：API Key、模型名与 SDK](#3-准备api-key模型名与-sdk)
4. [第一个调用：openai SDK](#4-第一个调用openai-sdk)
5. [多轮对话：messages 数组即上下文](#5-多轮对话messages-数组即上下文)
6. [流式响应：逐字返回](#6-流式响应逐字返回)
7. [思考模式与常用参数](#7-思考模式与常用参数)
8. [常见坑](#8-常见坑)

---

## 1. 目标与验收

本 Demo 的产出：一个能跑的 Python 脚本，输入一句用户消息返回大模型回答；再升级出**多轮对话版**（记得上下文）与**流式版**（逐字输出）。验收标准：**能不看文档写出请求体结构**（base_url、model、messages 三要素）；**能讲清为什么用 OpenAI 兼容协议**；**流式与一次性调用的差异能说清**。版本基线（2026-08）：DeepSeek V4 系列、openai SDK 1.x。

## 2. OpenAI 兼容协议：先理解再写代码

2026 年大模型 API 的事实标准是 **OpenAI 兼容协议**——不管底层是 DeepSeek、Qwen 还是 MiniMax，都提供同一套 HTTP 接口格式。这意味着**学会一次，所有模型通用**：只需改 base_url、api_key、model 三个配置。协议核心是 chat/completions 接口：

```json
POST https://api.deepseek.com/chat/completions
{
  "model": "deepseek-v4-flash",
  "messages": [
    {"role": "system", "content": "你是 Python 学习助手"},
    {"role": "user", "content": "什么是 RAG？"}
  ],
  "stream": false
}
```

理解三个字段：**model**（模型名，见第 3 节，2026-08 必须用 V4 系列新名）；**messages**（对话历史数组——这是多轮对话的载体，system 设定角色、user 用户消息、assistant 模型回答；"上下文"就是把多轮消息都带上）；**stream**（false 一次性返回，true 流式逐块返回）。响应结构：`choices[0].message.content` 是回答内容，`usage` 是 token 消耗。把请求体与响应结构背下来——这是后续所有 AI 链路（RAG、Agent）的地基，面试必问（"messages 里 system 和 user 的区别""为什么模型不记得上一轮"都是从这里展开的）。

## 3. 准备：API Key、模型名与 SDK

三个准备动作：**注册 DeepSeek 开放平台拿 API Key**（platform.deepseek.com，充值少量余额，Key 格式 `sk-...`，放 .env 不进代码，01 篇已配好）；**模型名用 2026-08 的新命名**：主力是 `deepseek-v4-flash`（284B MoE、13B 活跃参数，快且便宜，1M 上下文）与 `deepseek-v4-pro`（更强更贵），旧名 `deepseek-chat`/`deepseek-reasoner` 已于 2026-07-24 弃用——**用旧名会直接报错或静默走旧模型**，这是 2026 年最典型的"照着老教程写不出来"的原因；**装 SDK**：`uv add openai`（1.x 版本，官方 Python 客户端，DeepSeek 兼容它所以直接复用）。

价格参考（2026-08，官方已预告将上调，留意官方通知）：flash 输入约 1 元/百万 token（命中缓存仅 0.02 元）、输出约 2 元/百万 token，Pro 约 3 倍——本 Demo 消耗不到 1 分钱。DeepSeek 还预告了高峰期（北京时间 9-12 点、14-18 点）约 2 倍的价格浮动，Demo 阶段不用理会，但要知道这不是固定的。

## 4. 第一个调用：openai SDK

SDK 的调用分三层：**构造 client**（api_key + base_url）、**拼 messages**、**create 拿结果**：

```python
import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

client = OpenAI(
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    base_url="https://api.deepseek.com/v1",   # /v1 必加，漏掉 404
)

resp = client.chat.completions.create(
    model="deepseek-v4-flash",
    messages=[
        {"role": "system", "content": "你是 Python 学习助手，回答简洁"},
        {"role": "user", "content": "什么是 RAG？一句话"},
    ],
)
answer = resp.choices[0].message.content
print(answer)
```

三个必背细节：**base_url 的 /v1 后缀不能省**（SDK 会拼 /chat/completions，漏后缀全部 404）；**system 提示词单独一条消息**（设定角色与风格，这是第 4 篇 Prompt 工程的起点）；**取回答是 choices[0].message.content**，`resp.usage` 里有 prompt_tokens 与 completion_tokens（记账与监控用）。跑通后尝试改 system 提示词（"用三句话回答"），观察回答风格变化——**"提示词即配置"**的第一次体感。

## 5. 多轮对话：messages 数组即上下文

把 messages 从"一条 user 消息"扩展为**历史数组**（每次请求带上之前的 user/assistant 消息），模型就有上下文记忆。实现：本地维护一个列表，每次对话 append 用户消息、调接口、append 模型回答，下次请求把整个数组带上：

```python
history: list[dict] = [{"role": "system", "content": "你是 Python 学习助手"}]

def chat(user_msg: str) -> str:
    history.append({"role": "user", "content": user_msg})
    resp = client.chat.completions.create(
        model="deepseek-v4-flash", messages=history)
    answer = resp.choices[0].message.content
    history.append({"role": "assistant", "content": answer})
    return answer

print(chat("我叫小明，记住我"))
print(chat("我叫什么？"))     # 模型能答出"小明"——上下文生效
```

两个关键认知：**模型本身无记忆**，所谓"记得"只是每次把历史重新发一遍——所以上下文越长、费用越高、响应越慢；**历史有长度上限**（模型上下文窗口，flash 是 1M token），超了要裁剪（截断最旧消息、或做摘要）；**历史数组的 role 顺序有讲究**（system 第一条，user/assistant 交替，末尾是本次 user）——顺序错乱会让模型迷惑。**"上下文管理"的概念从这一刻开始建立**——RAG 的检索注入本质是"只带相关内容而非全量历史"，Level2 会系统化处理。

## 6. 流式响应：逐字返回

升级为流式——回答像打字机一样逐字出现，这是 AI 应用的用户体验标配：

```python
def chat_stream(user_msg: str):
    history.append({"role": "user", "content": user_msg})
    stream = client.chat.completions.create(
        model="deepseek-v4-flash", messages=history, stream=True)
    full = ""
    for chunk in stream:
        delta = chunk.choices[0].delta.content
        if delta:
            full += delta
            print(delta, end="", flush=True)   # 逐段打印，flush 立即输出
    print()
    history.append({"role": "assistant", "content": full})
```

理解流式的本质：**模型逐 token 生成，每生成一段就推给客户端**，而不是等全部生成完——所以首字延迟远小于总耗时。流式下响应不是整体 JSON，而是多个增量 chunk，每个 chunk 的 `choices[0].delta.content` 是这一段文字，**服务端不保证流结束前的内容完整**，所以要做增量拼接（上面代码的 full）。为什么需要流式：大模型回答长内容要几十秒，一次性返回让用户干等（体验差且可能超时），流式让用户 1 秒内看到第一个字——05 篇 Streamlit 会用 st.write_stream 消费这个生成器。想在看流式的同时统计 token 消耗，可以传 `stream_options={"include_usage": True}`，最后一个 chunk 的 usage 字段会带上完整消耗（默认流式不带 usage）。

## 7. 思考模式与常用参数

DeepSeek V4 系列支持**思考模式**（推理模型特性）与**非思考模式**：非思考模式直接给答案（快、便宜，默认）；思考模式先推理再回答（慢，适合复杂问题）。开启方式：`client.chat.completions.create(..., extra_body={"thinking": {"type": "enabled"}})`（V4 系列 API 的参数名，具体以官方文档为准）。思考模式下响应里可能带 `reasoning_content` 字段（推理过程），**如果请求带了 thinking 参数，回传历史时要把 reasoning_content 一并回传，否则 400**——这是推理模型最常见的坑。

常用参数速查：**temperature**（0-2，越低越确定，0.7 是通用默认；JSON 输出场景建议 0）；**max_tokens**（单次回答上限，flash 最高 384K 输出，够用就行，别设太大浪费钱）；**top_p**（与 temperature 二选一调，别同时动）；**seed**（固定随机种子，测试复现用）；**user**（业务侧用户标识，模型按用户治理限流/审计的字段）。这些参数在 04 篇结构化输出时会组合使用。

响应对象 `resp.usage` 的读取与记账：`usage.prompt_tokens`（输入消耗，含历史）、`usage.completion_tokens`（输出消耗）、`usage.total_tokens`（合计）。Demo 阶段打印出来看一眼即可，但**养成"每次调用看 usage"的习惯**——它是你的 API 账单明细，Level2 的监控与成本优化全靠它（比如发现 prompt_tokens 随着对话暴涨，就该做历史裁剪了）。历史裁剪的简单实现：当 `sum(m["content"] 长度)` 超过阈值时，丢弃最旧的 user/assistant 对（保留 system）——一行判断的事，先记下这个思路。

## 8. 常见坑

**401 鉴权失败**：API Key 错误、过期或格式不对（多了引号、少了 sk- 前缀）。先用 curl 验证 Key（01 篇的冒烟脚本）。

**404 或模型不存在**：模型名写错（还在用 deepseek-chat 旧名），或 base_url 漏了 /v1。2026-08 用 deepseek-v4-flash / deepseek-v4-pro。

**429 限流**：请求太频繁或余额不足。检查余额，加指数退避重试（02 篇的 call_with_retry 直接复用）。

**400 参数错误**：messages 结构不合法（role 拼错）、thinking 开启后回传缺 reasoning_content、max_tokens 超限。响应体里有 error 详情，先读它。

**超时**：模型响应慢或网络差时默认等很久。SDK 传 `timeout=30.0`（构造 client 时设，或每次 create 时设）——超时与重试的配合是：**超时只在"可重试层"触发重试**（网络与 429 重试，见 02 篇），模型真正在生成时的慢不要打断，那是它该花的时间。

**并发与连接数**：SDK 默认连接池够 Demo 用；如果做了并行请求（asyncio 或线程），注意 client 是**线程安全**的（全局共用一个 client 即可，不要每个请求新建——连接池会失控）。

**中文乱码**：Windows 终端打印中文乱码是终端编码问题，脚本内文件读写必须显式 encoding="utf-8"（02 篇）。

> 🎯 **核心要点**：本 Demo 的完成标志是**协议层理解到位**——base_url 加 /v1、messages 数组即上下文、流式是增量拼接。这三个理解叠加，是 Level2 里 RAG、Agent、工具调用一切 AI 功能的地基。

---

**下一模块**：[04 Prompt 工程入门 Demo](./04-Prompt%20工程入门%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)

【参考来源】
- [模型 & 价格 | DeepSeek API Docs](https://api-docs.deepseek.com/zh-cn/quick_start/pricing/)
- [DeepSeek V4 Flash: 284B MoE, 1M Context, Benchmarks, Pricing (2026)](https://www.morphllm.com/deepseek-v4-flash)
- [Using OpenAI SDK with DeepSeek base URL](https://theneuralbase.com/deepseek-api/learn/beginner/using-openai-sdk-with-deepseek-base-url/)
- [DeepSeek，计划上调API服务定价](https://finance.eastmoney.com/a/202608063833653917.html)
