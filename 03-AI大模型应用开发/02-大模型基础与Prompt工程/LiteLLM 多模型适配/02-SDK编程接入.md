# 02 SDK 编程接入
> 统一接口实战：completion 全参数、同步异步、流式、成本追踪与 Function Calling

## 📚 目录
1. [安装与第一个调用](#1-安装与第一个调用)
2. [completion 参数全解](#2-completion-参数全解)
3. [同步 / 异步 / 流式三形态](#3-同步--异步--流式三形态)
4. [统一参数映射](#4-统一参数映射)
5. [Function Calling 支持](#5-function-calling-支持)
6. [成本追踪与回调](#6-成本追踪与回调)
7. [常见坑](#7-常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 安装与第一个调用

```bash
pip install litellm        # 2026 基准 v1.94.x（生产锁定版本）
```

```python
import litellm
from litellm import completion

# 第一个调用：默认走 OpenAI
resp = completion(
    model="gpt-4o",
    messages=[{"role": "user", "content": "你好"}],
)
print(resp.choices[0].message.content)

# 换成 Claude / DeepSeek：只改 model 名
resp = completion(model="claude-sonnet-5", messages=...)
resp = completion(model="deepseek/deepseek-v4-pro", messages=...)
```

**Key 管理**（环境变量约定）：

```bash
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...
export DEEPSEEK_API_KEY=sk-...
# 前缀大写模型名，LiteLLM 自动识别
```

| 模型名写法 | 含义 |
|-----------|------|
| `gpt-4o` / `gpt-5.2` | OpenAI（前缀省略） |
| `claude-sonnet-5` / `anthropic/claude-sonnet-5` | Anthropic |
| `deepseek/deepseek-v4-pro` | DeepSeek（官方前缀） |
| `openai/gpt-4o` | 显式指定供应商 |
| `ollama/llama3.1:8b` | 本地 Ollama |
| `openai/vllm-model-name` | 本地 vLLM（OpenAI 兼容） |
| `bedrock/...` / `vertex_ai/...` | 云平台模型 |

> 🎯 **核心要点**：**模型名 = 路由键**。`供应商前缀/模型名` 是统一规范（省略前缀默认 OpenAI）。key 走环境变量（前缀大写模型名），代码零 key 逻辑。

## 2. completion 参数全解

```python
resp = completion(
    model="gpt-4o",
    messages=[
        {"role": "system", "content": "你是客服助手"},
        {"role": "user", "content": "订单 12345 到哪了？"},
    ],
    temperature=0.7,            # 随机性（统一参数）
    max_tokens=512,             # 生成上限（各家自动映射）
    top_p=0.9,
    stop=["END"],               # 停止词
    n=1,                        # 生成几条
    timeout=30,                 # 超时秒
    # 流式 / 结构化
    stream=True,                # 流式（返回迭代器）
    response_format={"type": "json_object"},   # JSON 模式
    # 成本与追踪
    mock_response=None,         # 测试用假响应（免真实调用）
    metadata={"user_id": "u123"},   # 附加元数据（记账用）
)
```

| 参数 | 说明 |
|------|------|
| `model` / `messages` | 必填（与其他 SDK 一致） |
| `temperature` / `max_tokens` / `top_p` | 统一参数，自动映射各家 |
| `stop` | 停止序列 |
| `timeout` | 请求超时 |
| `stream=True` | 流式（§3） |
| `response_format` | 结构化输出 |
| `mock_response` | 测试免真实调用 |
| `metadata` | 元数据（成本归属/审计） |

> ⚠️ 注意：`max_tokens` 在部分新模型（o 系）语境是 `max_completion_tokens`——LiteLLM 做映射；但**各家模型对参数语义有细微差异**（如 temperature 在推理模型被忽略），统一参数 ≠ 行为完全一致。

## 3. 同步 / 异步 / 流式三形态

```python
import asyncio
import litellm

# ① 同步（默认）
resp = litellm.completion(model="gpt-4o", messages=...)

# ② 异步（FastAPI 等 async 环境必用）
async def main():
    resp = await litellm.acompletion(model="gpt-4o", messages=...)
    return resp.choices[0].message.content

# ③ 流式（打字机效果）
for chunk in litellm.completion(model="gpt-4o", messages=..., stream=True):
    delta = chunk.choices[0].delta.content
    if delta:
        print(delta, end="", flush=True)

# ④ 异步流式
async for chunk in litellm.acompletion(model="gpt-4o", messages=..., stream=True):
    ...
```

| 形态 | 方法 | 场景 |
|------|------|------|
| 同步 | `litellm.completion` | 脚本/简单服务 |
| 异步 | `litellm.acompletion` | ⭐ FastAPI/Agent 循环（不冻结事件循环） |
| 同步流式 | `stream=True` 迭代 | 命令行 Demo |
| 异步流式 | `acompletion + stream=True` | ⭐ 生产 API（SSE） |

> 🎯 **核心要点**：**async 环境必须用 `acompletion`**（FastAPI 体系金科律：async def 里不能调同步阻塞）。生产 Agent/FastAPI 服务的标准形态 = `acompletion + stream=True`。

## 4. 统一参数映射

```python
# LiteLLM 的职责之一：把统一参数翻译成各家参数
# 例：max_tokens / temperature / stop → 各家的字段名与格式
{
    "gpt-4o":      {"max_tokens": 512, "temperature": 0.7},
    "claude-sonnet-5": {"max_tokens": 512, "temperature": 0.7},   # 同字段
    "gemini-2.5-pro":  {"max_output_tokens": 512, "temperature": 0.7},  # 字段名不同！
    "deepseek/deepseek-v4-pro": {"max_tokens": 512, "temperature": 0.7, "thinking_mode": ...},
}
```

| 映射层能力 | 说明 |
|-----------|------|
| 字段翻译 | max_tokens → max_output_tokens（Gemini）等 |
| 模型名解析 | 供应商前缀 → 正确端点/认证 |
| 参数兼容 | 各家支持的参数子集 |
| 模型特定参数 | `extra_body` / 供应商前缀参数（如 thinking_mode） |

> 💡 映射的边界：**通用参数（temperature/max_tokens）统一；模型特有能力用前缀参数**——如 DeepSeek 的 `thinking_mode` 在模型名前缀 `deepseek/` 时可用。LiteLLM 的映射覆盖 100+ 家，但"边缘能力"仍需查文档。

## 5. Function Calling 支持

```python
# 与 OpenAI 格式完全一致（Function Calling 体系 02 章）
from litellm import completion

tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "获取城市天气",
        "parameters": {
            "type": "object",
            "properties": {"city": {"type": "string"}},
            "required": ["city"],
        },
    },
}]

resp = completion(
    model="gpt-4o",
    messages=[{"role": "user", "content": "北京天气？"}],
    tools=tools,
    tool_choice="auto",
)

# 响应解析与回传与 OpenAI 一致（FC 体系 03 章循环）
if resp.choices[0].message.tool_calls:
    for tc in resp.choices[0].message.tool_calls:
        print(tc.function.name, tc.function.arguments)
```

| FC 能力 | LiteLLM 支持 |
|---------|:---:|
| tools/tool_calls | ✅（OpenAI 格式） |
| tool_choice | ✅ |
| 各家映射 | ✅（自动转 Anthropic/Gemini 格式） |
| 流式 tool_calls | ✅ |
| 统一循环 | 用 FC 体系 03 章的循环，模型名换 LiteLLM 即可 |

> 🎯 **核心要点**：LiteLLM 让 FC 循环**只写一遍**——`tools` 参数统一 OpenAI 格式，内部自动转各家协议（FC 体系 08 章"适配器"的工作被 LiteLLM 代劳）。Agent 循环 + LiteLLM = 多平台 Agent 零适配。

## 6. 成本追踪与回调

```python
import litellm

# ① 响应内直接拿成本（自动计算）
resp = litellm.completion(model="gpt-4o", messages=...)
print(resp.usage.prompt_tokens, resp.usage.completion_tokens)
print(resp._hidden_params.get("response_cost"))      # 💲 本次调用成本（美元）

# ② 回调钩子：每次调用后触发（记账/告警/日志）
def track_spend(kwargs, completion_response, start_time, end_time):
    cost = kwargs.get("response_cost")
    model = kwargs.get("model")
    logger.info(f"spend: model={model} cost={cost}")

litellm.success_callback = [track_spend]

# ③ 失败回调（重试/告警）
litellm.failure_callback = [on_failure]
```

| 成本能力 | 说明 |
|---------|------|
| 自动计价 | 按模型价格表自动算成本（`response_cost`） |
| 回调 | success/failure 钩子（记账/日志/告警） |
| 按 metadata 归属 | 用户/团队/项目维度记账 |
| Proxy 侧 | Spend Logs 全量记录（05/06 章） |

> 💡 SDK 层的成本追踪 = **"每次调用都知道花了多少钱"**；Proxy 层 = "所有调用集中记账"。单体先用 SDK 回调，多服务上 Proxy（05 章）。

## 7. 常见坑

| # | 坑 | 现象 | 解法 |
|:---:|-----|------|------|
| 1 | key 没配 | AuthenticationError | 环境变量前缀大写模型名 |
| 2 | async 环境用同步 | 事件循环冻结 | acompletion |
| 3 | 模型名拼错 | 未知模型 | 前缀/模型名规范（§1 表） |
| 4 | 新模型价格表缺失 | response_cost=None | 手动配置 cost map / 升级版本 |
| 5 | 参数语义差异 | 行为不符合预期 | 推理模型 temperature 被忽略等 |
| 6 | 流式忘处理 delta | 拼不出全文 | delta.content 增量拼接（FC 体系 07 章） |
| 7 | 供应商特有的参数 | 报错 | 前缀参数 / extra_body |
| 8 | 版本升级破坏 | 行为突变 | 锁定版本 + 读 release notes |

## 8. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | 模型名 = 路由键：`供应商前缀/模型名`（省略默认 OpenAI） |
| 2 | key 走环境变量，前缀大写模型名 |
| 3 | 三形态：completion / acompletion / stream（async 生产必用 acompletion） |
| 4 | 统一参数自动映射各家；模型特有能力用前缀参数 |
| 5 | FC 完整支持：OpenAI 格式，内部转各家——Agent 循环零适配 |
| 6 | 成本自动计价（response_cost）+ success/failure 回调 |
| 7 | 八大坑：key/异步/模型名/价格表/参数语义 |
| 8 | 单模型可以不装；多模型从 SDK 开始，多服务上 Proxy |

---

**下一模块**：[03-模型管理](03-模型管理.md) / **返回总览**：[00-LiteLLM知识体系总览](00-LiteLLM知识体系总览.md)

## 参考来源

- [LiteLLM 官方：Completion 入门](https://docs.litellm.ai/docs/tutorials/first_call)
- [LiteLLM 官方：异步与流式](https://docs.litellm.ai/docs/completion/stream)
- [LiteLLM 官方：Function Calling](https://docs.litellm.ai/docs/completion/function_call)
- [LiteLLM 官方：成本追踪与回调](https://docs.litellm.ai/docs/observability/callback)
