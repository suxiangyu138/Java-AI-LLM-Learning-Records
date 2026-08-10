# 05 - OpenAI 兼容 API

> 本体系第五课：vLLM 的"杀手级接口"——完全 OpenAI 兼容——"你的 OpenAI 代码只改 base_url 一行就能接 vLLM——切换成本 = 一行配置"

---

## 📚 目录

1. [兼容的意义](#1-兼容的意义)
2. [支持的端点](#2-支持的端点)
3. [切换姿势](#3-切换姿势)
4. [流式与工具调用](#4-流式与工具调用)
5. [练习 5 题](#5-练习-5-题)
6. [本节验收](#6-本节验收)

---

## 1. 兼容的意义

**vLLM 完全 OpenAI 兼容 = 整个 OpenAI 生态直接复用**：

```text
兼容的意义
├── 代码零改：OpenAI SDK/任何 OpenAI 兼容客户端直接接（只改 base_url）
├── 生态复用：LangChain/OpenAI 工具链/现成前端全兼容
├── 切换无痛：从 OpenAI API → vLLM（自部署）→ 换模型不换代码
└── 标准即生态：OpenAI API 是事实标准——兼容它 = 兼容整个生态
    ——"vLLM 的兼容 = 'API 层面的 OpenAI'——你写的 OpenAI 代码全能用"
```

**兼容心智**：**"兼容的记忆：'base_url 一行切换——整个 OpenAI 生态白嫖'"**——"**为什么重要：AI 应用开发者 90% 用过 OpenAI SDK（或兼容客户端）——vLLM 兼容 = 你的现有代码直接接自部署模型——'切换成本 = 一行配置（01 篇测试的深化）'"**（"生态的复用：RAG 工具/Agent 框架（LangChain 等）都支持 OpenAI 兼容端点——**'vLLM = OpenAI 兼容的自部署服务'"**）；**兼容的边界**——"vLLM 支持 OpenAI API 的核心（chat/completions/embeddings 等）——非核心端点（管理类）不支持——**'核心能力全兼容、管理类没有（05 篇 2 节端点清单）'"**（"了解即可：知道'核心全兼容'——细节查 vLLM 文档（OpenAI Compatibility 页）"）。

## 2. 支持的端点

**vLLM 支持的 OpenAI 端点（vllm serve 自动提供）**：

```text
支持端点（核心三件套 + 辅助）
├── /v1/chat/completions：聊天补全（Chat Completions——主力）
├── /v1/completions：文本补全（Completions——老接口）
├── /v1/embeddings：向量嵌入（Embeddings——RAG 场景）
├── /health：健康检查（监控/负载均衡——06 篇）
└── 其他：/v1/models（列出模型）、流式（SSE）、工具调用（Function Calling）
    ——"核心三件套（chat/complete/embedding）+ 健康检查 = 服务的 API 面"
```

**端点心智**：**"端点的记忆：'chat/completions 是主力、embeddings 是 RAG 用、health 是监控用'"**——"**主力端点：/v1/chat/completions（对话——ChatGPT 同款接口）——'90% 的调用走它'"**（"embeddings 的用途：RAG 检索的向量化（`模型推理与部署/` 体系或 RAG 体系的向量）——**'vLLM 一个服务 = 对话 + 向量两用'"**）；**端点的验证**——"`curl http://localhost:8000/v1/models` 列出模型——**'服务起了没 + 模型对不对，/v1/models 说话'"**（"`/health` 返回 ok = 健康（监控/负载均衡用——06 篇）——**'两个验证端点 = 服务的体检'**）。

## 3. 切换姿势

**从 OpenAI API 切换到 vLLM——只改 base_url（一行）**：

```python
# 切换前（OpenAI 官方 API）
from openai import OpenAI

client = OpenAI(api_key="sk-xxxx")          # OpenAI 的 key
resp = client.chat.completions.create(
    model="gpt-4o",                          # OpenAI 的模型
    messages=[{"role": "user", "content": "你好"}],
)

# 切换后（vLLM 自部署——只改两行！）
client = OpenAI(
    base_url="http://localhost:8000/v1",     # 改：指向 vLLM
    api_key="sk-no-key",                     # 改：占位（vLLM 不校验）
)
resp = client.chat.completions.create(
    model="Qwen/Qwen2.5-7B-Instruct",        # 改：你的模型名
    messages=[{"role": "user", "content": "你好"}],
)
# 其余代码（消息结构/响应处理）完全不变
```

**切换心智**：**"切换的记忆：'base_url + api_key + model 三处——其余代码不动'"**——"**切换的本质：OpenAI SDK 是'通用客户端'——base_url 指向谁就调谁（OpenAI 官方/vLLM/任何兼容服务）——'SDK 是'电话'，base_url 是'拨号'"**（"api_key 占位：vLLM 本地服务不校验 key（02 篇）——**'占位即可，别纠结'"**）；**切换的验证**——"切完跑同样的对话（对比响应）——**'切换成功 = 同样的代码、不同的模型、都跑通'"**（"切换的注意：模型名要和服务启动的一致（02 篇）——**'模型名不匹配报 404（09 篇速查）'"**）。

## 4. 流式与工具调用

**流式输出与工具调用——AI 应用的两个高频能力**：

```python
# 流式输出（SSE——LLM 逐字输出——聊天体验的关键）
from openai import OpenAI

client = OpenAI(base_url="http://localhost:8000/v1", api_key="sk-no-key")
stream = client.chat.completions.create(
    model="Qwen/Qwen2.5-7B-Instruct",
    messages=[{"role": "user", "content": "写一首诗"}],
    stream=True,                              # 流式开关
)
for chunk in stream:                          # 逐块接收（打字机效果）
    delta = chunk.choices[0].delta.content
    if delta:
        print(delta, end="", flush=True)

# 工具调用（Function Calling——Agent 场景）
# OpenAI SDK 的 tools 参数——vLLM 兼容（v0.23.0 统一工具解析）
# response = client.chat.completions.create(..., tools=[...])
```

**流式心智**：**"流式的记忆：'stream=True 逐块收——打字机效果'"**——"**为什么流式重要：LLM 生成要几秒——全等会"卡死感"——流式逐字出（用户体验）；vLLM 的流式由连续批处理支撑（03 篇——请求动态进出）——'流式 = LLM 服务的标配体验'"**（"流式的实现：SSE（Server-Sent Events——uvicorn 体系 03 篇的流式）——**'vLLM 的流式 = OpenAI 兼容的 SSE'"**）；**工具调用**——"Function Calling（tools 参数——Agent 让模型调工具）——vLLM 兼容（v0.23.0 统一工具解析管道）——**'vLLM 跑 Agent 的工具调用（`../../../03-Agent开发/` 体系的需求）'"**（"工具调用的流程（`../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/` 体系详讲）——**'vLLM 提供兼容端点——Agent 框架照常用'"**）。

**兼容的工程价值**（为什么兼容是"杀手级"）："**① 团队技能复用**——会 OpenAI SDK 就会接 vLLM（零学习成本）；**② 切换自由**——OpenAI API ↔ vLLM ↔ 其他兼容服务来回切（成本一行——供应商不锁定）；**③ 测试方便**——本地 vLLM 模拟 OpenAI（开发/测试环境省钱）——**'兼容 = 技能复用 + 切换自由 + 测试便利——三赢'"**（"生产切换的注意：模型能力差异（开源 vs GPT 的差距）——**'兼容的是接口，不是能力——模型效果要单独评估'"**）。

**兼容的常见问题**（接入时的三个高频疑问）："**① 要不要真实 api_key**——不需要（vLLM 本地不校验——占位即可——02 篇）；**② 模型名填什么**——启动时的模型名（`vllm serve` 的模型——/v1/models 可查——09 篇 404 的解法）；**③ 参数全兼容吗**——核心参数（temperature/max_tokens/stream/tools）兼容——个别 OpenAI 专有参数不支持（报错再查文档）——**'三个疑问 = 接入新手的第一批问题——答案都在本课'"**（"接入的验证：对话 + 流式 + 工具三连测（本课 3/4 节）——**'三连测 = 兼容的完整验证'"**）。

**兼容与 AI 应用生态**（vLLM 在 AI 应用栈里的位置）："**AI 应用的常见调用链：应用（LangChain/Agent/自定义）→ OpenAI SDK → vLLM（模型服务）——'vLLM 是 AI 应用的'模型底座'——应用层不关心引擎，只认 OpenAI 兼容端点'"**（"这个抽象的意义：换模型/换引擎不动应用代码（兼容的价值——本课 1 节）——**'兼容 = AI 应用与模型之间的'通用插座'"**（"了解即可：知道 vLLM 在调用链的位置——**'AI 应用开发 = 会调 OpenAI 兼容端点即可'"**）——**兼容的总结**："vLLM 的兼容 = '接口标准 + 生态复用'——接口标准（OpenAI 是事实标准）、生态复用（应用/工具/Agent 全兼容）——**'兼容是 vLLM 流行的工程原因——性能（03 篇）是技术原因'"**。

## 5. 练习 5 题

1. 兼容的意义？（base_url 一行——整个 OpenAI 生态复用）
2. 核心端点三件套？（chat/completions/embeddings）
3. 切换改三处？（base_url/api_key/model）
4. 流式怎么开？（stream=True——打字机效果）
5. 工具调用的场景？（Agent——vLLM 兼容）

## 6. 本节验收

**验收动作**：① 用 OpenAI SDK 调 vLLM（切换三处——跑通对话）；② 流式输出体验（逐字打印）；③ 用 /v1/models 和 /health 验证；④ 写"切换备忘"（OpenAI → vLLM 的三处改动）——**"兼容认知 + 端点 + 切换 + 流式 = API 接入能力"**——**练习纪律**：切完必测（对话 + 流式）——"切换成功 = 同样代码不同模型都跑通"。

> 🎯 **核心要点**：兼容 = 生态白嫖（**OpenAI API 是事实标准——vLLM 兼容它 = 兼容整个生态——切换成本一行**）；**端点三件套（chat/completions 主力/embeddings RAG/health 监控——/v1/models 验证）**；**切换三处（base_url/api_key 占位/model 名——SDK 是电话 base_url 是拨号——其余代码不动）**；**流式（stream=True 打字机效果——SSE）+ 工具调用（Function Calling——Agent 场景）**——"你的 OpenAI 代码只改 base_url 一行就能接 vLLM——切换成本 = 一行配置"。

---

**上一模块**：[04-模型加载与配置.md](./04-模型加载与配置.md) / **下一模块**：[06-部署形态.md](./06-部署形态.md)
