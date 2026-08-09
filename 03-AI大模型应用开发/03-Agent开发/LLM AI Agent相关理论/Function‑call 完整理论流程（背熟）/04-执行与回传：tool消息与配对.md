# 执行与回传：tool 消息与配对

> 背诵篇：流程第④⑤步——**Harness 执行与结果回传**：tool_call_id 精确匹配、两条消息必须追加、结果截断与格式化。这里是最容易出 400 的环节。

## 1. 30 秒背诵卡

| 概念 | 背诵版一句话 |
|---|---|
| 执行 | Harness `json.loads(arguments)` → 校验 → 调真实函数 → 得结果 |
| 回传格式 | `{role:"tool", tool_call_id, content}`——content 必须是字符串 |
| tool_call_id 铁律 | **精确匹配**（case-sensitive UUID）——复制不重建不索引 |
| 多条调用 | 多个 tool_call → 多条 `role:"tool"` 消息，各配各的 ID |
| 追加铁律 | **assistant（含 tool_calls）与 tool 消息都必须追加**回 messages |
| content 类型 | 必须是 str——非字符串抛 TypeError；一般 `json.dumps(result)` |
| 结果截断 | 大结果截断（"Rows 1-10 of 250"）——结果每轮都被重新 token 化 |
| 执行校验 | pre-call 参数校验（模型输出不可信）+ post-call 结果形状校验 |

## 2. 回传消息结构（背诵版代码）

```python
# ① 执行：解析 + 调用
arguments = json.loads(tool_call.function.arguments)   # 铁律A：先解析
validate_args(arguments, TOOL_SCHEMA)                  # 模型输出不可信，先校验
result = get_weather(**arguments)                      # 执行真实函数

# ② 回传：构造 tool 消息
messages.append({
    "role": "tool",
    "tool_call_id": tool_call.id,        # 铁律B：原样复制，不重建！
    "content": json.dumps(result)        # content 必须是字符串
})
# ⚠️ 铁律C：assistant 消息也必须追加（在 tool 消息之前）
messages.append(assistant_msg_with_tool_calls)  # 顺序：assistant → tool
```

| 环节 | 铁律 | 违反后果 |
|---|---|---|
| arguments 解析 | json.loads + schema 校验 | 直接传字符串 → 类型错误/注入 |
| tool_call_id | 原样复制（不重建不索引） | 不匹配 → **400 错误** |
| content | 字符串（json.dumps） | TypeError |
| 消息追加 | assistant + tool 都追加 | 断上下文/请求被拒 |
| 消息顺序 | assistant(tool_calls) 在前，tool 在后 | 非法消息顺序报错 |

> ⚠️ 2026 细节：**顺序敏感**——assistant 的 tool_calls 消息必须在其对应的 tool 消息之前；流式下若先收到文本增量后收到工具增量（部分 OpenAI 兼容提供商），会产生非法顺序 `assistant(tool_calls) → assistant(text) → tool`——openai-agents-python 的 `buffer_streamed_tool_calls` 模式就是为此兜底。

## 3. 多条并行调用的回传

```python
# 模型一次提议 2 个调用：
#   tool_calls = [call_1, call_2]
for tc in tool_calls:
    result = execute(tc)
    messages.append({"role": "tool", "tool_call_id": tc.id, "content": json.dumps(result)})
# → 一条 assistant（含 2 个 tool_calls）+ 2 条 tool 消息
# → 顺序：assistant(tool_calls) → tool(call_1) → tool(call_2)
```

> 💡 要点：**每条 tool 消息必须带自己的 ID**；执行顺序无关（无依赖才并行），但回传时每条都配对正确。有依赖的调用**不能**并行（模型可能假设 B 的结果已就绪——并行是 2026 新失败面）。

## 4. 结果格式化的三个纪律

| 纪律 | 内容 | 原因 |
|---|---|---|
| 字符串化 | json.dumps（结构化数据利于模型推理） | content 必须是 str |
| 截断 | 大结果截断 + 摘要（"Rows 1-10 of 250"） | 结果每轮重新 token 化——不截断成本爆炸 |
| 错误结构化 | 失败返回 `{error_code, message, recoverable, suggested_action}` | 模型据此自修复（消除 ~80% 卡死） |

| 返回类型 | 适用 |
|---|---|
| 结构化 JSON | 模型需要继续推理的中间结果 |
| 人类可读文本 | 最终展示结果（工具输出=最终答案时） |
| 结构化错误 | 执行失败——给模型可分支的语义 |

## 5. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 执行前要做什么？ | json.loads + schema 校验（模型输出不可信） |
| 回传格式？ | `{role:"tool", tool_call_id, content}`——content 必须字符串 |
| tool_call_id 怎么处理？ | 原样复制——case-sensitive，不匹配 400 |
| 两条消息都追加？ | 是——assistant（含 tool_calls）+ tool 都要，顺序 assistant 在前 |
| 大结果怎么办？ | 截断+摘要——结果每轮重新 token 化 |
| 失败怎么回传？ | 结构化错误（error_code/recoverable/suggested_action） |

---

**下一模块**：[05-调用循环工程：五步循环与终止](05-调用循环工程：五步循环与终止.md)　**返回总览**：[00-Function-call 完整理论流程总览](00-Function-call完整理论流程总览.md)

## 参考来源

- [Returning result to model | Function Calling Beginner Course（The Neural Base）](http://theneuralbase.com/function-calling/learn/beginner/returning-result-to-model/)
- [OpenAI Function Calling 函数调用指南（API 易文档中心）](https://docs.apiyi.com/api-capabilities/openai/function-calling)
- [feat: add buffered Chat Completions tool-call streaming（openai-agents-python PR #3506）](https://github.com/openai/openai-agents-python/pull/3506)
- [Ultimate Guide: How AI Agents Use Tools — 2026（skywork.ai）](https://skywork.ai/blog/ai-agents-using-tools-ultimate-guide-2026/)
