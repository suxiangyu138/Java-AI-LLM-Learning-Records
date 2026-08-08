# 02 Agent 循环深潜：五阶段循环与五要素

> 定位：机制核心——Agent 循环的完整拆解：五阶段循环（Reason/Act/Execute/Observe/Repeat）、五要素（缺一即聊天机器人）、消息缓冲细节与 2026 原生推理变化（2026-08 基准）

## 📚 目录

1. [五阶段循环](#1-五阶段循环)
2. [五要素：缺一即聊天机器人](#2-五要素缺一即聊天机器人)
3. [消息缓冲：循环的血脉](#3-消息缓冲循环的血脉)
4. [工具注册表与调用](#4-工具注册表与调用)
5. [观察格式化：错误的归处](#5-观察格式化错误的归处)
6. [最小实现：~100 行证明循环即一切](#6-最小实现100-行证明循环即一切)
7. [2026 变化：原生推理通道](#7-2026-变化原生推理通道)
8. [核心要点](#8-核心要点)

## 1. 五阶段循环

所有现代 Agent（Claude Code、Cursor、Devin、Operator）都是 ReAct（Yao et al., ICLR 2023）的变体：

```text
      ┌──────────────────────────────────────┐
      │                                      │
      ▼                                      │
 ① Reason     模型决定下一步（基于目标与当前状态）
      │                                      │
      ▼                                      │
 ② Act        模型发出结构化工具调用（含参数）    │
      │                                      │
      ▼                                      │
 ③ Execute    代码执行请求的函数，得到结果       │
      │                                      │
      ▼                                      │
 ④ Observe    结果回填进对话上下文              │
      │                                      │
      ▼                                      │
 ⑤ Repeat/Stop  继续循环 或 模型发出完成信号     │
      └──────────────────────────────────────┘
```

> 🎯 **核心认知**："几乎所有 Agent 工程都是这个循环的变体"——更好的工具、更好的推理提示词、更聪明的停止条件。**智能不在任何单一组件里，而在循环里**。

## 2. 五要素：缺一即聊天机器人

| 要素 | 职责 | 缺失后果 |
|------|------|---------|
| 增长的消息缓冲 | 交替存放 user/assistant(tool_calls)/tool-result 轮次 | 模型失去上下文 = 每次从零开始 |
| 工具注册表 | 按名调用：schema 进 → 执行 → 结果字符串出 | 无法调用工具 |
| 停止条件 | 模型 finish / 无工具调用 / 上限触发 | 无限循环 |
| 轮次预算 | 防止无限循环的硬上限 | 失控烧钱 |
| 观察格式化 | 工具输出转模型可读字符串；**错误也必须是观察而非崩溃** | 一步失败整个 Agent 崩溃 |

> ⚠️ **观察格式化是隐藏关键**：工具返回的错误必须格式化为观察回传给模型（模型可据此调整），而不是抛异常终止循环——"every error must become an observation, not a crash"。

## 3. 消息缓冲：循环的血脉

```text
循环中的消息序列（顺序不可错）：
user: 任务
assistant: thinking + tool_calls [调用工具A]
tool: 工具A结果
assistant: thinking + tool_calls [调用工具B]
tool: 工具B结果
assistant: 最终回答（无 tool_calls）→ 停止

铁律：含 tool_calls 的 assistant 消息必须**先于**工具结果追加
     （违反则 API 报错或模型困惑）
```

| 细节 | 说明 |
|------|------|
| 追加顺序 | assistant(tool_calls) 必须在 tool 结果之前 |
| 推理内容 | 2026：thinking 块单独通道随轮次传递（07/09 篇详述） |
| 上下文增长 | 每轮增长 = 压缩/摘要的触发点（05 篇） |

## 4. 工具注册表与调用

| 环节 | 说明 |
|------|------|
| 声明 | 工具 schema（名称/描述/JSON Schema 参数）给模型 |
| 调用 | 模型返回 `tool_calls`（名称 + 参数 JSON） |
| 执行 | 运行时查注册表执行函数 |
| 回传 | 结果字符串回填消息缓冲 |
| 2026 标准 | MCP 服务器统一工具接入（"universal adapter"）；工具多时用语义发现（向量索引注册表） |

## 5. 观察格式化：错误的归处

| 原则 | 说明 |
|------|------|
| 全量格式化 | 工具输出（含错误）转模型可读字符串 |
| 错误即观察 | 异常/失败 → 观察消息回传，模型自纠正 |
| 截断 | 超长结果截断/摘要（防上下文爆炸） |
| 幂等 | 可重试工具用稳定键（防重试副作用） |

```text
错误处理链路（正确做法）：
工具抛错 → 格式化 "调用失败: <错误信息>" → 回填缓冲
→ 模型观察 → 换参数重试 / 换工具 / 终止
```

## 6. 最小实现：~100 行证明循环即一切

> 关键证据：最小 SWE-agent 实现（~100 行 Python，一个 bash 工具，正则解析动作）在 SWE-bench Verified 上达 **74-76.8%**——循环本身是承重概念，工具与提示词只是围绕它的细节。

```python
# 最小 Agent 循环骨架（概念性 ~15 行核心）
messages = [{"role": "user", "content": task}]
while True:
    resp = llm(messages)                    # Reason：模型决策
    if resp.stop_reason == "end_turn":      # 停止条件
        break
    for call in resp.tool_calls:            # Act + Execute
        fn = tools[call.name]               # 工具注册表查表
        result = fn(**call.args)            # 执行
        messages.append(asst_msg(call))     # 铁律：先助手后结果
        messages.append(tool_msg(result))   # Observe：格式化回填
    if len(messages) > MAX_TURNS:           # 轮次预算
        messages.append("已达上限，基于已有信息给出最佳答案")
        messages.append(llm(messages, tools=False))  # early-stopping
        break
```

## 7. 2026 变化：原生推理通道

| 变化 | 说明 |
|------|------|
| Prompt 式 Thought token | 2022 年的 workaround，2025-2026 被取代 |
| 原生推理 | 推理走独立加密通道（`thinking`）随轮次传递 |
| 内容块类型 | `thinking`（内部推理）/ `text`（用户可见）/ `toolCall`（动作） |
| 推理深度 | 可调（off → xhigh），每轮更丰富但更贵 |
| 循环控制流 | **不变**——变的只是推理表达方式 |

> 💡 **结论**：原生推理让每轮循环"更聪明"，但循环结构、终止条件、消息缓冲规则完全不变——理解循环比追逐模型更重要。

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 五阶段循环：Reason → Act → Execute → Observe → Repeat/Stop——一切 Agent 的底层
> 2. 五要素：消息缓冲/工具注册表/停止条件/轮次预算/观察格式化——缺一即聊天机器人
> 3. 铁律：含 tool_calls 的 assistant 消息先于工具结果追加；**错误必须是观察不是崩溃**
> 4. ~100 行最小实现即达 SWE-bench 74-76.8%——循环是承重概念
> 5. 2026 原生推理（thinking 通道）改变每轮质量，不改变循环结构

---

**上一模块**：[01 Agent 定义与边界](01-Agent%20定义与边界：Task%20Workflow%20Agent%20三分法.md)　**下一模块**：[03 终止条件与循环控制](03-终止条件与循环控制：max_turns%20与防御纵深.md)　**返回总览**：[00 总览](00-总览：Agent%20内核理论知识体系.md)

## 【参考来源】

- [What Is an AI Agent Loop? (FutureAGI)](https://futureagi.com/blog/loop-engineering/what-is-ai-agent-loop/)
- [Agent Loop: Definition, Examples & Guide (FutureAGI 2026)](https://futureagi.com/glossary/agent-loop/)
- [The Agent Loop Decoded: Three Levels Every Agent Engineer Must Know (Oracle)](https://blogs.oracle.com/developers/the-agent-loop-decoded-three-levels-every-agent-engineer-must-know)
- [The AI Agents Stack (2026 Edition) (O'Reilly Radar)](https://www.oreilly.com/radar/the-ai-agents-stack-2026-edition/)
- [What Is ReAct? Definition & Guide (FutureAGI 2026)](https://futureagi.com/glossary/react-pattern/)
- [The Agent Loop (ai-engineering-from-scratch)](https://github.com/rohitg00/ai-engineering-from-scratch/blob/95292efdfbe0cc3daa40d7f367c7e6ed93153d9f/phases/14-agent-engineering/01-the-agent-loop/docs/en.md)
- [How to Build an AI Agent: A Developer's Guide (Rasa)](https://rasa.com/blog/how-to-build-an-ai-agent)
