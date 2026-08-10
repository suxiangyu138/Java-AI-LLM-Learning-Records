# 03 - 模型封装：ChatModel

> 本体系第三课：模型接入的统一姿势——ChatModel、切换模型、工具绑定、结构化输出——"换模型只改一个类名——ChatModel 是'模型层的统一插座'"

---

## 📚 目录

1. [ChatModel 的定位](#1-chatmodel-的定位)
2. [接入各家模型](#2-接入各家模型)
3. [bind_tools：工具绑定](#3-bind_tools工具绑定)
4. [with_structured_output：结构化输出](#4-with_structured_output结构化输出)
5. [练习 5 题](#5-练习-5-题)
6. [本节验收](#6-本节验收)

---

## 1. ChatModel 的定位

**ChatModel = 语言模型的统一封装（消息进、消息出——01 篇问题①的解）**：

```text
ChatModel 的定位
├── 本质：LLM 的统一接口（OpenAI/DeepSeek/本地 vLLM 都是 ChatModel）
├── 输入：消息序列（Message——user/assistant/system/tool 角色）
├── 输出：AIMessage（含内容/工具调用意图）
├── 能力：invoke/stream/batch（Runnable——02 篇）+ bind_tools + 结构化输出
└── 价值：换模型 = 换一个类名（其余代码不动）
    ——"ChatModel = 模型层的'统一插座'——什么模型都能插"
```

**定位心智**：**"ChatModel 的记忆：'换模型只改一个类名'"**——"**OpenAI → DeepSeek → 本地 vLLM：`ChatOpenAI(...)` → `ChatDeepSeek(...)`（或换 base_url）——调用代码（invoke/消息结构）完全不变——'这就是 01 篇'接入不统一'问题的解'"**（"对比裸 SDK：换模型要改整个调用代码（各家 SDK 不同）——**'ChatModel 把差异封装在类名里'"**）；**消息结构**——"输入是消息序列（角色 + 内容——system 设定/user 提问/assistant 历史）——**'消息结构是 LLM 对话的'通用语言'（各家一致）'"**（"了解即可：消息结构细节（SystemMessage/UserMessage）用时查——**'记住'角色消息'模型就够'"**）。

## 2. 接入各家模型

**接入示例——三家的统一姿势**：

```python
# OpenAI 系（官方）
from langchain_openai import ChatOpenAI
model = ChatOpenAI(model="gpt-4o-mini", api_key="sk-xxx")

# DeepSeek 系（OpenAI 兼容——换 base_url）
model = ChatOpenAI(
    model="deepseek-chat",
    base_url="https://api.deepseek.com/v1",   # 换 base_url（05 篇 vLLM 同款思路）
    api_key="sk-xxx",
)

# 本地 vLLM（OpenAI 兼容——vLLM 体系 05 篇）
model = ChatOpenAI(
    model="Qwen/Qwen2.5-7B-Instruct",
    base_url="http://localhost:8000/v1",      # 指向 vLLM
    api_key="sk-no-key",
)

# 调用姿势（统一）
resp = model.invoke("你好")                    # AIMessage（.content 取文本）
print(resp.content)
```

**接入心智**：**"接入的记忆：'ChatOpenAI 一家通吃——换 base_url 换模型'"**——"**核心洞察：主流模型大多 OpenAI 兼容（DeepSeek/本地 vLLM/Ollama）——一个 ChatOpenAI 类 + base_url 全搞定——'OpenAI 兼容 = 一个类接所有（vLLM 体系 05 篇的框架版落地）'"**（"非 OpenAI 兼容的（部分国产）用专属类（ChatDeepSeek 等 langchain-xxx 包）——**'先试 OpenAI 兼容（base_url），不行再专属类'"**）；**密钥管理**——"api_key 从环境变量读（不写代码——`02-后端核心技术` 的安全纪律）——**'密钥进环境变量（.env——别提交 Git）'"**（"LangChain 生态的安装：langchain-openai 等集成包（按模型装）——**'装包 = langchain + langchain-openai（按需）'"**）。

## 3. bind_tools：工具绑定

**bind_tools = 让模型"知道有哪些工具可用"（Agent 的基础）**：

```python
# 定义工具（LangChain 的 @tool 装饰器——08 篇详讲）
from langchain_core.tools import tool

@tool
def get_weather(city: str) -> str:
    """查询城市天气（工具描述——模型靠它决定何时用）"""
    return f"{city} 今天晴，25 度"

# 绑定工具（让模型知道工具存在——调用意图在 AIMessage.tool_calls）
model_with_tools = model.bind_tools([get_weather])

# 调用：模型可能返回"工具调用意图"（不是直接回答）
resp = model_with_tools.invoke("北京天气怎么样？")
print(resp.tool_calls)      # [{'name': 'get_weather', 'args': {'city': '北京'}, ...}]
# 模型说"要调用 get_weather(city=北京)"——执行工具是应用的事（08 篇 Agent 循环）
```

**bind_tools 心智**：**"bind_tools 的记忆：'让模型知道'有这些工具'——调用意图在 tool_calls'"**——"**分工：bind_tools 只做'告知'（模型输出调用意图）；'执行工具'是应用/Agent 的活（拿到 tool_calls → 调工具 → 结果回传——08 篇）——'模型不执行工具（只表达意图）——Function Calling 协议层原理（`Function Calling 函数调用` 体系）'"**（"工具描述的重要：模型的'决策依据'是工具的描述（docstring——@tool 的 docstring 就是描述）——**'工具描述写清'做什么/何时用'（Function Calling 体系的三原则）'"**）；**工具调用的循环**——"完整 Agent 循环：模型输出 tool_calls → 执行工具 → ToolMessage 回传 → 模型继续——**'bind_tools 是循环的第一步（08 篇 create_agent 自动做循环）'"**（"了解即可：本课知道 bind_tools 是什么——**'循环的自动化是 08 篇 create_agent 的事'"**）。

## 4. with_structured_output：结构化输出

**with_structured_output = 强制结构化输出（Pydantic——比提示词可靠）**：

```python
from pydantic import BaseModel, Field

# 定义输出结构（Pydantic 模型）
class MovieReview(BaseModel):
    title: str = Field(description="电影名")
    rating: float = Field(description="评分 0-10")
    summary: str = Field(description="一句话总结")

# 绑定结构化输出（模型直接返回 Pydantic 对象——不是字符串！）
structured_model = model.with_structured_output(MovieReview)

review = structured_model.invoke("评价一下《流浪地球》")
print(review.title)       # 流浪地球（直接取字段——不用解析字符串！）
print(review.rating)      # 8.5
```

**结构化输出心智**：**"结构化输出的记忆：'with_structured_output——模型直接返回对象（不是 JSON 字符串）'"**——"**为什么比提示词可靠：提示词写'以 JSON 返回'——模型可能格式错（引号/字段名不对）——with_structured_output 用 Pydantic schema 强制（模型按结构生成——格式保证）——'框架层解决'输出不可靠'（01 篇问题③）'"**（"底层实现（了解即可）：框架用工具调用机制/JSON 模式强制结构——**'知道'强制可靠'就够——细节是框架的事'"**）；**结构化输出的价值**——"① 代码直接取字段（不用解析字符串——省解析代码）；② 类型安全（Pydantic 校验——字段缺失报错）；③ 后续处理方便（对象传业务逻辑）——**'结构化输出 = 模型输出的'类型安全'"**（"应用场景：信息抽取/表单生成/数据转换（模型输出直接进数据库）——**'RAG/Agent/数据处理场景的标配'"**）。

**模型封装的工程细节**（接入生产的注意）："**① 超时与重试**——模型调用可能慢/失败（配超时 + 重试——06 篇容错）；**② 成本控制**——模型选择（gpt-4o-mini vs 大模型——按任务难度选）；**③ 温度设置**——ChatModel 的 temperature（事实任务低/创意任务高）——**'三个细节 = 模型封装的'生产三件套'"**（"了解即可：知道'生产要配超时/重试/选型'——具体参数用时查——**'先会'接入与调用'，生产细节按需补'"**）。

**ChatModel 的常见参数**（调用时的"旋钮"）："**temperature**（随机性——0 事实/1 创意）、**max_tokens**（输出上限）、**stream**（流式——05 篇 vLLM 同款）、**model**（模型选择）——**'四个旋钮 = ChatModel 调用的高频参数'"**（"参数的传递：invoke 时传或 bind 绑定（model.bind(temperature=0)——固定参数绑定）——**'bind = 参数的'预设'（和 bind_tools 同款机制）'"**）。

**模型封装的调试姿势**（调用异常的排查）："**① 401/403**——key 没配/配错（环境变量检查——03 篇 2 节）；**② 404**——base_url/模型名错（vLLM 用 /v1/models 查——09 篇）；**③ 超时**——模型响应慢（网络/大模型——加超时 + 重试）——**'三个报错 = 模型接入的新手三连（10 篇速查）'"**（"调试的通用姿势：先单测模型（不带链——model.invoke 直接调）——**'先模型后链（小步验证——02 篇调试）'"**）。

## 5. 练习 5 题

1. ChatModel 是什么？（统一插座——消息进消息出）
2. 换模型改什么？（类名/base_url——调用代码不动）
3. bind_tools 做什么？（告知工具——意图在 tool_calls）
4. 谁执行工具？（应用/Agent——不是模型）
5. 结构化输出的姿势？（with_structured_output + Pydantic）

## 6. 本节验收

**验收动作**：① 用 ChatOpenAI 接入 OpenAI/DeepSeek/vLLM 三选二（base_url 方式）；② 定义 @tool 并用 bind_tools 调用（看 tool_calls 输出）；③ 用 with_structured_output 输出一个 Pydantic 对象；④ 写"模型接入备忘"（三家配置）——**"接入 + 工具 + 结构化 = ChatModel 能力"**——**练习纪律**：模型配置进环境变量——"密钥不写代码"。

> 🎯 **核心要点**：ChatModel = 统一插座（**消息进消息出——换模型只改类名/base_url——OpenAI 兼容一家通吃**）；**bind_tools（告知工具——意图在 tool_calls——执行是应用的事——工具描述是模型决策依据）**；**with_structured_output（Pydantic 强制——比提示词可靠——输出对象的类型安全——抽取/表单场景标配）**——"换模型只改一个类名——ChatModel 是'模型层的统一插座'"。

---

**上一模块**：[02-核心抽象Runnable.md](./02-核心抽象Runnable.md) / **下一模块**：[04-提示词框架.md](./04-提示词框架.md)
