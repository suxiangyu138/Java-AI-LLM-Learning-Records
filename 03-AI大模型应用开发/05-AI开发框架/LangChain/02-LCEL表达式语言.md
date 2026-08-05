# 02 - LCEL 表达式语言

> **核心摘要**：LCEL（LangChain Expression Language）是 LangChain 的组合原语——用管道符 `|` 把任意 Runnable 串成执行链，声明式、可并行、可流式、可观测。1.x 时代 LCEL 仍是**确定性工作流（for 循环）**的首选，与 create_agent（while 循环）分工明确。

> **前置阅读**：[[01-LangChain 1.x生态全景]]

---

## 📚 目录

1. [LCEL 是什么](#1-lcel-是什么)
2. [Runnable 协议](#2-runnable-协议)
3. [管道符与 RunnableSequence](#3-管道符与-runnablesequence)
4. [并行执行 RunnableParallel](#4-并行执行-runnableparallel)
5. [组合原语](#5-组合原语)
6. [流式输出](#6-流式输出)
7. [错误处理与回退](#7-错误处理与回退)
8. [配置与调试](#8-配置与调试)
9. [LCEL vs create_agent](#9-lcel-vs-create_agent)
10. [核心要点](#10-核心要点)

---

## 1. LCEL 是什么

> **背景**：0.x 时代链的写法混乱（LLMChain/SequentialChain/AgentExecutor……）；1.x 统一为 Runnable 协议 + 管道符组合。
> **目的**：掌握「代码即数据流图」的声明式编程范式。
> **适用范围**：直线/分支/并行等**确定性**流程；流程需要模型自主决策时改用 create_agent。

```text
LCEL 的本质
├── ① 声明式：| 连接组件，数据流即代码结构
├── ② 统一协议：一切皆 Runnable（invoke/ainvoke/batch/stream）
├── ③ 自动优化：并行自动执行、流式自动传递
├── ④ 可观测：LangSmith 自动记录每一步
└── ⑤ 最小化：prompt | model | parser 仅 3 行核心

典型对比
├── 命令式（旧式）：
│   ├── 手写 for 循环调 model.invoke(prompt.format(...))
│   └── 手动拼接上下文、手动处理输出、无追踪
└── 声明式（LCEL）：
    ├── chain = prompt | model | parser
    └── 链自动处理格式、并行、流式、追踪
```

---

## 2. Runnable 协议

> 🎯 **Runnable 是 LCEL 的基石**——任何实现统一接口的对象都可以用 `|` 连接：PromptTemplate、Model、Parser、Tool、Retriever，甚至普通函数（RunnableLambda 包装）。

```python
from langchain_core.runnables import Runnable  # 接口抽象

class Runnable(Generic[Input, Output]):
    def invoke(self, input: Input, config: Config | None = None) -> Output: ...
    async def ainvoke(self, input: Input, config: Config | None = None) -> Output: ...
    def batch(self, inputs: list[Input], config: Config | None = None) -> list[Output]: ...
    async def abatch(self, inputs: list[Input], config: Config | None = None) -> list[Output]: ...
    def stream(self, input: Input, config: Config | None = None) -> Iterator[Output]: ...
    async def astream(self, input: Input, config: Config | None = None) -> AsyncIterator[Output]: ...
    # 组合方法
    def pipe(self, *others) -> RunnableSequence: ...
    def __or__(self, other) -> RunnableSequence: ...   # 即 |
    def with_retry(self, *, retry_if_exception_type=...) -> RunnableRetry: ...
    def with_fallbacks(self, fallbacks, exceptions_to_handle=...) -> RunnableWithFallbacks: ...
```

**六个方法的适用场景**：

| 方法 | 同步 | 异步 | 批量 | 流式 | 典型场景 |
|------|:---:|:---:|:---:|:---:|------|
| `invoke` | ✅ | | | | 单次调用（调试/服务） |
| `ainvoke` | | ✅ | | | FastAPI/异步服务 |
| `batch` | ✅ | | ✅ | | 离线批量处理 |
| `abatch` | | ✅ | ✅ | | 并发批量 |
| `stream` | ✅ | | | ✅ | 逐 token 输出（前端打字机） |
| `astream` | | ✅ | | ✅ | 异步流式 |

> 💡 **金句**：六个方法「同源」——一个链实现了 invoke，其余方法自动可用（LangChain 内部做数据流拆分），零改动切换。

```python
# 三态同源示例
from langchain_openai import ChatOpenAI

model = ChatOpenAI(model="gpt-5.5")

print(model.invoke("你好").content)          # 同步
async def main():
    print((await model.ainvoke("你好")).content)   # 异步
for chunk in model.stream("写一首诗"):       # 流式
    print(chunk.content, end="", flush=True)
```

---

## 3. 管道符与 RunnableSequence

> 🎯 **管道符 `|` 是最核心的语法糖**——左侧输出自动注入右侧输入（链内部完成格式适配，无需胶水代码）。

```python
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是{role}，回答要简洁"),
    ("user", "{question}"),
])
model = ChatOpenAI(model="gpt-5.5")
parser = StrOutputParser()

chain = prompt | model | parser   # ← 核心三行

result = chain.invoke({"role": "技术顾问", "question": "什么是 RAG？"})
```

**数据流逐级解析**（理解 LCEL 的关键）：

```text
chain.invoke({"role": ..., "question": ...})
│
├── ① prompt（ChatPromptTemplate）
│   └── 输入: dict {role, question}
│   └── 输出: ChatPromptValue（消息列表 + 系统提示）
│        ↓ 自动转换为模型输入格式
├── ② model（ChatOpenAI）
│   └── 输入: ChatPromptValue
│   └── 输出: AIMessage
│        ↓ 自动取 .content
└── ③ parser（StrOutputParser）
    └── 输入: AIMessage
    └── 输出: str ← 最终结果
```

> ⚠️ **输入类型自动适配的规则**：链组件间传 dict / str / Message 时，LCEL 内部会尝试兼容（`_transform` 钩子）；若类型不匹配且无法转换，会在 invoke 时报 `TypeError`——排查时逐段打印中间输出即可定位。

**等价写法（底层）**：

```python
from langchain_core.runnables import RunnableSequence

chain = RunnableSequence(first=prompt, last=model, middle=[parser])
# 或
chain = prompt.pipe(model).pipe(parser)
```

---

## 4. 并行执行 RunnableParallel

> 🎯 **`RunnableParallel` 让多个链并发跑同一输入**——典型场景：一次检索多种来源、多个视角分析后汇总。

```python
from langchain_core.runnables import RunnableParallel
from langchain_core.output_parsers import StrOutputParser

# 场景：一份文档，同时生成「摘要」和「关键词」
summary_chain = prompt_summary | model | StrOutputParser()
keywords_chain = prompt_keywords | model | StrOutputParser()

parallel = RunnableParallel(
    summary=summary_chain,     # ← 两个子链并发执行
    keywords=keywords_chain,
)

result = parallel.invoke({"document": doc_text})
# → {"summary": "……", "keywords": ["……", "……"]}
```

**管道 + 并行组合**（并行结果继续传给下游）：

```python
# 上游先处理，下游并行，最终合并
chain = (preprocess_prompt
         | model
         | RunnableParallel(a=chain_a, b=chain_b)   # 中间分叉
         | final_chain)                              # 合并收口
```

> 💡 **RunnablePassthrough 传原样**：`RunnablePassthrough()` 直接透传输入不处理，常用在并行字典里保留原始输入（如 `RunnableParallel(context=retriever, question=RunnablePassthrough())`）。

---

## 5. 组合原语

| 原语 | 作用 | 示例 |
|------|------|------|
| `RunnableSequence` | 顺序执行 | `a \| b \| c` |
| `RunnableParallel` | 并发执行 | `RunnableParallel(a=..., b=...)` |
| `RunnablePassthrough` | 透传输入 | 保留原始 question |
| `RunnableLambda` | 函数包装 | `RunnableLambda(lambda x: x.strip())` |
| `RunnableBranch` | 条件分支 | 按路由结果选择链 |
| `RunnableMap` | dict 变换 | 提取/重组字段（1.x 常用 `lambda` 简写） |

```python
from langchain_core.runnables import RunnableLambda, RunnableBranch

# ① 函数接入管道（普通函数即 Runnable）
def strip_whitespace(text: str) -> str:
    return text.strip()

chain = RunnableLambda(strip_whitespace) | model | StrOutputParser()

# ② 条件分支：按意图路由（简单 router 模式）
from langchain_core.output_parsers import StrOutputParser

route_chain = prompt_router | model | StrOutputParser()  # 输出 "rag" / "chat"

branch = RunnableBranch(
    (lambda x: x["route"] == "rag", rag_chain),    # (条件, 链)
    (lambda x: x["route"] == "chat", chat_chain),
    fallback_chain,                                 # 默认链
)
```

> ⚠️ **RunnableBranch 注意**：条件函数接收的是**整个输入 dict**（含上游字段），不是路由字符串本身——常见坑是把 `x["route"]` 写成 `x`。

---

## 6. 流式输出

> 🎯 **LCEL 链天然支持逐 token 流式**——只要每一环都实现 `stream`（模型支持 token 级流式即可，parser 会逐段转换）。

```python
chain = prompt | model | StrOutputParser()

for chunk in chain.stream({"role": "顾问", "question": "什么是 Agent？"}):
    print(chunk, end="", flush=True)
```

**LCEL 流式 vs Agent 事件流式（v1.3）**：

| 维度 | LCEL `chain.stream()` | Agent `stream_events(version="v3")` |
|------|----------------------|-------------------------------------|
| 输出 | 最终链的输出 token | 消息 + 工具调用 + 状态**分通道**投影 |
| 场景 | 纯生成链 | 带工具调用的 Agent |
| 1.3 推荐 | 简单链 | Agent 用 typed-projection（见 [[05-Agent开发]]） |

---

## 7. 错误处理与回退

> 🎯 **生产环境必备**：LLM 调用失败/超时是常态，必须配置重试与回退。

```python
# ① 重试：对指定异常类型指数退避重试
from openai import APITimeoutError, RateLimitError

robust_model = model.with_retry(
    retry_if_exception_type=(APITimeoutError, RateLimitError),
    stop_after_attempt=3,        # 最多 3 次
    wait_exponential_jitter=False,
)

# ② 回退：主模型挂了切备用模型（成本/可用性权衡）
cheap_model = ChatOpenAI(model="gpt-4.1-mini")
fallback_chain = prompt | robust_model.with_fallbacks([cheap_model]) | parser

# ③ 超时与最大 token（防失控）
model = model.bind(max_tokens=2048).with_config({"timeout": 60})
```

> ⚠️ **重试注意事项**：只重试**可重试**的异常（网络超时/限流），不要重试「输入错误/上下文超长」——重试也必败且浪费 token。此原则即 v1.3 `ToolRetryMiddleware` 的修复点（#38845：只重试可重试异常）。

---

## 8. 配置与调试

```python
# with_config：链路级配置（调用链/元数据/token 预算）
chain.with_config({
    "run_name": "qa_chain",          # LangSmith 中识别
    "tags": ["prod", "v2"],
    "metadata": {"team": "core"},
}).invoke(input_data)

# with_llm_usage：获取 token 用量（成本核算）
result = chain.invoke(input_data)
```

**LangSmith 追踪**（1.x 默认接入）：

```text
链调用自动产生 trace（无需埋点）
├── 每次 invoke → 一个 trace（含各节点耗时/token）
├── prompt 版本变化 → 自动对比
├── 异常 → 自动记录输入/输出快照
└── 查看入口：langsmith SDK 或 Web 面板
```

**本地调试三板斧**：

```python
# ① 单节点测试（不跑整链）
prompt.invoke({"role": "x", "question": "y"})     # 看 prompt 输出
model.invoke(...)                                  # 看模型输出
parser.invoke(...)                                 # 看解析输出

# ② 逐步打印
for step in chain.stream(input_data):
    print(step, flush=True)

# ③ 关闭 LangSmith 可观测数据时仍可加日志
chain = RunnableLambda(lambda x: (print("INPUT:", x), x)[1]) | chain
```

---

## 9. LCEL vs create_agent

> 🎯 **1.x 的核心决策点**——确定性 vs 自主性：

| 维度 | LCEL | create_agent |
|------|:---:|:---:|
| 控制方式 | 开发者显式定义流程 | LLM 自主决策（ReAct 循环） |
| 适用场景 | 固定步骤/直线/分支 | 动态选工具、多步推理 |
| 记忆 | 需手动注入历史消息 | checkpointer 自动管理 |
| 可预测性 | 高（路径确定） | 中（依赖模型推理） |
| 复杂度 | 10-30 行 | 3 行 + 工具定义 |
| 典型形态 | QA 管道、批量处理、Eval | 客服、数据分析、工作流 Agent |

```text
最佳实践（官方推荐）
├── 用 LCEL 构建确定性子任务链（清洗/提取/格式化）
├── 用 create_agent 作为大脑统一调度
└── 「for 循环用 LCEL，while 循环用 create_agent/LangGraph」
```

---

## 10. 核心要点

> 🎯 **核心要点**：
> 1. **LCEL = 管道符组合**：`prompt | model | parser`，声明式表达数据流
> 2. **Runnable 六方法同源**：invoke/ainvoke/batch/stream 零切换
> 3. **RunnableParallel 并发**：多源检索、多视角分析后汇总
> 4. **组合原语**：Sequence/Parallel/Passthrough/Lambda/Branch/Map
> 5. **生产必备**：with_retry（只重试可重试异常）+ with_fallbacks + 超时
> 6. **选型**：确定性流程用 LCEL；自主决策用 create_agent（见 [[05-Agent开发]]）
> 7. **调试**：LangSmith 自动追踪 + 单节点 invoke 验证

---

**下一模块**：[03-模型与输出解析](03-模型与输出解析.md) | **返回总览**：[00-LangChain知识体系总览](00-LangChain知识体系总览.md)
