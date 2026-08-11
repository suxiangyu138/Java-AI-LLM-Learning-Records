# 07 Function Calling 工具调用 Demo

> AI 应用的两大核心能力之二：让模型"调用你的代码"。用工具声明 + 调用循环做一个天气查询 Demo，理解"模型不执行代码、只声明意图"的机制——这是 Agent 的第一块基石。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [原理：模型只声明，代码才执行](#2-原理模型只声明代码才执行)
3. [工具声明：JSON Schema 说清能力](#3-工具声明json-schema-说清能力)
4. [调用循环：五步闭环](#4-调用循环五步闭环)
5. [多工具与并行调用](#5-多工具与并行调用)
6. [常见坑](#6-常见坑)

---

## 1. 目标与验收

本 Demo 的产出：一个终端对话脚本，问"北京今天适合跑步吗？"——脚本先调用天气工具拿到数据，再基于数据回答。验收标准：**能画出自绘调用循环的时序图（用户→模型→工具→模型→用户）**；**能讲清为什么模型不能直接执行代码**；**能处理工具返回的 JSON 参数**。与仓库「Function Calling 函数调用【Agent 基石】」体系的分工：那边是协议全解与生产实践，这里是动手跑通的最小闭环。

## 2. 原理：模型只声明，代码才执行

先破除最流行的误解：**Function Calling 不是让模型执行代码**。模型的输出只有文本——它"决定"要调用某个工具，并把调用意图（工具名 + JSON 参数）作为结构化输出返回；真正执行工具的是**你的程序**（比如 requests 请求天气 API），执行结果再作为"工具消息"回传给模型，模型基于结果组织最终回答。这样设计的三个原因：**安全**（模型永远碰不到你的代码执行环境，参数校验、权限控制都在你手里）；**能力边界**（模型不会做真实世界的事——查天气、查库存、转账都是代码的事）；**可靠**（工具执行的可观测、可重试、可审计都归程序管）。一句话：**模型是决策者，代码是执行者**。

## 3. 工具声明：JSON Schema 说清能力

要让模型"知道有什么工具可用"，得把工具的**能力描述**发给模型——这决定了模型会不会调用、调得对不对：

```python
tools = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "查询指定城市当天的天气，用于回答天气相关问题",
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名，如：北京"},
                },
                "required": ["city"],
            },
        },
    }
]
```

description 是工具声明里最重要的字段——**它是模型的"使用说明书"**：写清"做什么、什么时候用、边界是什么"（比如"仅查询当天天气，不支持历史数据"），模型据此决定是否调用。参数要**只留必要项**（参数越多模型越容易填错）；**required 标记必填**。工具数量 Demo 阶段 1-3 个即可——工具声明本身消耗 token（每个约 100-200 token），且数量多时模型选择会变差（「主流 Agent 范式」体系里有工具发现的进阶做法）。

描述写作的三个检查标准：**动词开头**（"查询""计算"比"该函数用于……"清楚）；**写清触发条件**（"当用户询问某城市天气时使用"——模型靠这句话决定是否调用）；**参数给示例**（description 里注明格式，如"城市名，例如：北京"——模型填参更准）。写完后自测：把 description 读给一个不了解系统的人听，他能判断何时调用、填什么参数，说明描述合格。

## 4. 调用循环：五步闭环

核心循环五步：**发消息**（带 tools 声明）→ **看响应**（模型要么直接回答，要么返回 tool_calls）→ **执行工具**（程序调真实代码）→ **回传结果**（以 role=tool 的消息返回）→ **模型给最终回答**：

```python
import json
from openai import OpenAI

def get_weather(city: str) -> str:
    """真实工具：Demo 用模拟数据，Level2 换成真实天气 API"""
    return json.dumps({"city": city, "weather": "晴", "temp": 26, "advice": "适合户外活动"},
                      ensure_ascii=False)

def run_conversation(user_msg: str) -> str:
    messages = [{"role": "user", "content": user_msg}]
    while True:
        resp = client.chat.completions.create(
            model="deepseek-v4-flash", messages=messages, tools=tools)
        msg = resp.choices[0].message

        # 情况一：模型直接回答（没有工具调用）—— 对话结束
        if not msg.tool_calls:
            return msg.content

        # 情况二：模型要调工具 —— 把工具调用追加进历史
        messages.append(msg.model_dump())
        for call in msg.tool_calls:
            result = get_weather(**json.loads(call.function.arguments))
            messages.append({
                "role": "tool",
                "content": result,
                "tool_call_id": call.id,     # 与工具调用配对，不能少
            })
```

理解这个 while 循环的终止条件：**模型不返回 tool_calls 就是终止**（它认为信息够了、可以直接回答了）。每轮循环：assistant 消息（带 tool_calls）与 tool 消息（带 tool_call_id）必须**成对追加**，ID 对不上会 400。参数 `call.function.arguments` 是 JSON 字符串，**必须 json.loads 解析后再传给函数**（用 ** 展开或按 key 取）。防御注意：模型可能传错参数（城市名带引号）——工具函数内部 try/except，失败时把错误信息作为 tool 消息回传（"参数无效：xxx"），模型会修正后重试——**工具错误回传机制能消除 80% 的调用卡死**。

## 5. 多工具与并行调用

一个真实 Agent 有多个工具：查天气、查日历、算费用……声明多个 tools 条目即可，模型按 description 选择。**并行调用**：模型在一次响应里可以返回多个 tool_calls（比如同时查北京和上海的天气）——循环里 for 遍历就是并行执行（Demo 串行也行，Level2 用 asyncio.gather 并行）。并行调用的配对规则不变：**每个 tool_call 都要有自己的 tool_call_id 与 tool 消息**。

```python
# 响应里可能出现多个工具调用
for call in msg.tool_calls:              # tool_calls 是数组
    result = execute_tool(call.function.name, json.loads(call.function.arguments))
    messages.append({"role": "tool", "content": result, "tool_call_id": call.id})
```

多工具场景的常见坑：**工具名冲突**（两个工具同名——命名加模块前缀，如 weather_get / calendar_check）；**工具间有依赖**（B 工具需要 A 工具的结果——模型不支持"工具调工具"，依赖逻辑写在你的循环里：先执行 A，把结果再发给模型，让模型决定是否调 B）。**"工具循环上限"**：真实场景加 `max_iterations = 8` 左右防死循环（模型反复调工具不收敛时强制终止，提示"信息已足够，请直接回答"）。

两个进阶参数现在就可以知道：**tool_choice 控制模型"要不要调"**——默认 `"auto"`（模型自行决定），`"required"`（这轮必须调工具，适合先查库再回答的流程），指定工具名（如 `{"type": "function", "function": {"name": "get_weather"}}`）则强制只调这个工具（路由场景用——比如"查天气"命令直接走工具）；**parallel_tool_calls**——默认开启并行，若工具的调用有先后依赖（B 需要 A 的输出），可以传 `parallel_tool_calls=False` 关掉，模型就一次只调一个工具。这两个参数在「Function Calling 函数调用」体系的"工具发现与强制调用"章节有系统展开，Demo 期先用 tool_choice 做一次"强制调用"实验：不传问题、直接问模型"帮我看看今天天气"，观察它是否仍然调工具——这验证的是"指令与工具挂钩"的机制。

## 6. 常见坑

**400 invalid tool_call_id**：tool 消息缺 tool_call_id 或与 assistant 消息里的 ID 不一致——追加时原样复制 call.id。

**400 invalid arguments**：arguments 不是合法 JSON——模型偶发输出残缺 JSON，json.loads 包 try/except，失败回传错误信息。

**模型就是不调工具**：description 写得模糊（模型不知道何时该用）——按"做什么/何时用/边界"三段重写 description；或问题本身不需要工具。

**模型乱调工具**：问题与工具无关也调——在 system 提示词加约束"仅在必要时调用工具"。

**循环不终止**：模型每轮都调工具——加 max_iterations 上限；检查回传内容是否让模型"能得出结论"（空结果会诱使模型继续调）。

**arguments 是字符串不是 dict**：解析前 type 检查——`json.loads` 可能得到 dict 也可能得到其他类型，传参前验证字段存在。

**流式模式与工具调用的配合**：`stream=True` 时工具调用也是流式返回的——chunk 里的 `delta.tool_calls` 逐段拼出完整调用（name、arguments 都可能是分段到达的，需要按 index 聚合）。Demo 阶段的经验是"工具场景先不流式"（工具调用本身用户看不到中间过程，非流式实现简单、不易出错），流式工具回调在 Level2 处理——但要知道这个组合是存在的，面试被问"流式和工具调用能一起用吗"时答案是"能，流式下工具参数是增量拼装的"。

> 🎯 **核心要点**：Function Calling 的心智模型是"**决策与执行分离**"——模型给意图（工具名+参数）、代码给结果、结果回流促成最终回答。五步循环跑通后，你就掌握了 Agent 的最小形态：模型 + 工具 + 循环，这是 Level2 Agent 项目的引擎。

---

**下一模块**：[08 Python + AI 最小闭环 Demo](./08-Python%20%2B%20AI%20最小闭环%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)

【参考来源】
- [Function Calling | DeepSeek API Docs](https://api-docs.deepseek.com/zh-cn/guides/function_calling)
- [Function Calling | OpenAI API](https://platform.openai.com/docs/guides/function-calling)
- [Tool use | openai-python SDK](https://github.com/openai/openai-python?tab=readme-ov-file#tool-use)
