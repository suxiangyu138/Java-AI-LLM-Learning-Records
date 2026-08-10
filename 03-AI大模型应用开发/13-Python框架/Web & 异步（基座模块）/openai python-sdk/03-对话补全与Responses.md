# 03 - 对话补全与 Responses API

> 定位：双 API 选型课——"chat.completions 是行业标准、responses 是 2026 新方向"——参数全解、消息角色、迁移映射——"两个都会用才是 2026 姿势"

---

## 📚 目录

1. [双 API 现状：标准与新方向](#1-双-api-现状标准与新方向)
2. [chat.completions：参数全解](#2-chatcompletions参数全解)
3. [消息角色与多轮对话](#3-消息角色与多轮对话)
4. [responses：服务端状态与新能力](#4-responses服务端状态与新能力)
5. [迁移映射表](#5-迁移映射表)
6. [常见坑](#6-常见坑)
7. [练习 5 题](#7-练习-5-题)

---

## 1. 双 API 现状：标准与新方向

2026 年 OpenAI 有两条对话 API，**并存且都受支持**，选型逻辑必须清楚：

| | Chat Completions | Responses |
|---|---|---|
| 端点 | /v1/chat/completions | /v1/responses |
| 官方态度 | 无限期支持（行业标准） | **新项目推荐** |
| 会话状态 | 无（messages 每次全发） | `store` + `previous_response_id` |
| 内置工具 | 仅函数调用 | web_search/file_search/code_interpreter/MCP |
| 缓存命中 | 基线 | 提升 40-80% |
| 流式 | delta 增量 | 类型化事件（output_text.delta 等） |
| 推理摘要 | 无 | 有（reasoning.summary） |

**"chat 是'所有人都会的接口'，responses 是'新项目该用的接口'"**——存量项目不必急迁（官方承诺支持），**新项目默认 responses**；框架生态（Agno 等）已默认切到 responses，Codex CLI 2026-02 起强制——"**2026 年写代码，两个 API 都要会，选型看项目新老**"。

## 2. chat.completions：参数全解

```python
resp = client.chat.completions.create(
    model="gpt-5.2",                 # 模型名（gpt-5.x 系列）
    messages=[...],                  # 对话历史（必填）
    temperature=0.7,                 # 采样温度 0-2（默认 1）
    max_completion_tokens=1024,      # 输出上限（含推理 token，o 系必用）
    stream=False,                    # 流式开关（04 篇）
    tools=[...],                     # 工具定义（05 篇）
    response_format={"type": "json_object"},  # JSON 模式（05 篇）
    top_p=1.0, stop=None, seed=None, # 采样/停止/确定性
)
```

**关键参数语义**：其一，**`temperature` 与 `top_p`**——随机性旋钮，二选一调（同时调会互相干扰），事实型任务 0-0.3、创意任务 0.7+；其二，**`max_completion_tokens` 取代 `max_tokens`**——o 系列推理模型先消耗"推理 token"，老参数在推理模型上行为不对，"**2026 一律用 max_completion_tokens**"；其三，**`seed` 做尽力而为的确定性**（非严格保证）；其四，**`stop`** 停止序列列表——"**先定输出上限，再谈其他参数——成本与稳定性都从这开始**"。

**模型选型直觉**：gpt-5.x 系列是 2026 主力（通用/推理/工具），min 版走低成本高吞吐——"**重任务上大模型，高频任务上 min 版，成本账 09 篇算**"。

**stop 与采样细节**：`stop` 传字符串列表（如 `["\n\n", "再见"]`）让模型在命中时停止生成——"**格式控制的小工具**"；`top_p` 与 `temperature` 是同一随机性的两种表达，**别同时调**（API 建议二选一）；`presence_penalty`/`frequency_penalty` 控制重复倾向（0-2，创意任务可微调）——"**默认参数先跑通，哪个指标不达标再拧哪个旋钮**"。

## 3. 消息角色与多轮对话

```python
messages = [
    {"role": "developer", "content": "你是客服助手，回答简洁。"},   # 系统指令（新推荐）
    {"role": "user", "content": "你们支持退款吗？"},
    {"role": "assistant", "content": "支持，7 天内可申请。"},        # 回填模型回复
    {"role": "user", "content": "怎么申请？"},                       # 新问题
]
resp = client.chat.completions.create(model="gpt-5.2", messages=messages)
```

**角色体系**：`developer`（系统指令，2026 官方推荐——比 system 更受模型遵循）、`system`（旧写法，仍兼容）、`user`（用户）、`assistant`（模型回复，**多轮必须回填**）、`tool`（工具结果回传，05 篇）。

**多轮对话铁律**：**历史完整回放**——每次调用 messages 包含全部轮次（无服务端状态）；**assistant 消息必须回填**（不回填模型就"失忆"）；**只回放需要的窗口**——超长历史要截断/摘要（09 篇上下文管理）——"**chat API 的无状态是你管理状态，responses 的 store 是服务端管理状态——这是两条 API 的本质差异**"。

## 4. responses：服务端状态与新能力

```python
# 第一次：创建响应（可存服务端）
resp = client.responses.create(
    model="gpt-5.2",
    input=[{"role": "user", "content": "总结一下量子计算"}],   # input 取代 messages
    store=True,                          # 服务端保存会话状态
)
print(resp.output_text)                  # 便捷属性：全文（取代 choices[0].message.content）

# 后续轮：用 previous_response_id 接续（无需重发全部历史）
resp2 = client.responses.create(
    model="gpt-5.2",
    previous_response_id=resp.id,        # 服务端自动续上下文
    input=[{"role": "user", "content": "再展开讲应用场景"}],
)
```

**responses 的核心价值**：其一，**服务端会话**——`previous_response_id` 接续，省去历史全量重发（省 token、省延迟、缓存命中率高）；其二，**内置工具**——`tools=[{"type": "web_search"}]` 平台替你执行搜索返回结果，不用自己写工具循环（05 篇对比）；其三，**推理摘要**——`reasoning.summary` 免费拿推理过程摘要；其四，**类型化流式事件**——`response.output_text.delta` 等（04 篇）。

**注意**：**不是所有模型都支持 responses**（老模型 gpt-3.5 系不支持）——"**新项目选 responses 前先确认模型支持**"。

**store 与合规**：`store=True` 表示服务端保存会话（OpenAI 侧存储），**数据敏感场景要评估存储合规**——`store=False` + 自己管理上下文（把 input 每次全发）也完全可用，"**store 是便利不是必需，合规优先时关掉它**"；`previous_response_id` 只认服务端保存过的 id——"**关了 store，previous_response_id 就断了，两条线二选一**"。

## 5. 迁移映射表

| Chat Completions | Responses |
|---|---|
| `messages` | `input` |
| `max_completion_tokens` | `max_output_tokens` |
| `response_format` | `text.format` |
| `stream=True` 的 delta | 类型化事件（04 篇） |
| `choices[0].message.content` | `output_text` |
| tool 定义嵌套 `{type, function:{...}}` | **扁平化** `{type: "function", name, description, parameters}` |
| tool 结果回传 `{"role": "tool"}` | `function_call_output` 项（带 call_id） |
| 图像 `image_url` 嵌套 | `input_image`（嵌套形状会校验报错） |

**迁移节奏**：**新代码直接写 responses；存量代码按模块迁移**——"迁移不是重写，是映射：参数名换、响应路径换、工具形状换——**心智不变，写法变**"；**先在小模块跑通映射表，再逐步替换**（09 篇灰度思想同源）。

## 6. 常见坑

1. **模型不支持 responses**——老模型 400 报错；先查模型支持矩阵。
2. **responses 里传旧 tool 形状**——嵌套 function 报校验错误；用扁平形状。
3. **tool 结果用 `{"role": "tool"}` 回传 responses**——必须 `function_call_output` + call_id。
4. **temperature 在推理模型上**——部分 o 系模型忽略/拒绝采样参数；查模型文档。
5. **max_tokens 老参数**——推理模型上行为不对；统一 max_completion_tokens/max_output_tokens。
6. **seed 当严格确定性**——它是尽力而为；严格场景要人工校验（05 篇结构化输出）。
7. **多轮不回填 assistant**——模型"失忆"；历史完整回放是铁律。

## 7. 练习 5 题

1. 双 API 现状？"新项目用 responses"的完整理由？
2. max_completion_tokens 为什么取代 max_tokens？
3. 角色体系？多轮对话铁律？
4. responses 的四个核心价值？previous_response_id 解决什么？
5. 迁移映射表的五个关键映射？迁移节奏？

> 🎯 **核心要点**：双 API = **chat 标准（messages 全量回放 + max_completion_tokens）+ responses 新方向（previous_response_id 服务端状态 + 内置工具 + 缓存提升 40-80%）**——"新项目 responses、存量不急着迁、两个都会用——参数映射表背熟，迁移只是换写法"。

---

**下一模块**：[04-流式响应.md](04-流式响应.md) / **返回总览**：[00-openai-python-sdk总览.md](00-openai-python-sdk总览.md)
