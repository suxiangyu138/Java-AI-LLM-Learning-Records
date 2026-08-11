# 02 Python 核心语法快速 Demo

> 面向 AI 应用开发的 Python 语法回顾：不是全面语法手册，而是把后续 Demo 会反复用到的六个语言特性各写一个能跑的小 Demo——类型注解、dataclass、异常、上下文管理器、pathlib、logging。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [类型注解：让代码可读可检](#2-类型注解让代码可读可检)
3. [dataclass：结构化数据的容器](#3-dataclass结构化数据的容器)
4. [异常处理：AI 调用必须三层防护](#4-异常处理ai-调用必须三层防护)
5. [上下文管理器与 pathlib](#5-上下文管理器与-pathlib)
6. [logging：替代 print 调试](#6-logging替代-print-调试)
7. [函数参数与生成器：AI 代码的高频写法](#7-函数参数与生成器ai-代码的高频写法)

---

## 1. 目标与验收

本模块的产出：六个语法特性各一个独立小 Demo，全部 `uv run` 能跑。验收标准：**能独立写出带类型注解的函数签名**；**知道 dataclass 在 AI 应用里装什么**（模型返回、工具参数、配置项）；**能说出 AI 调用为什么必须 try/except**。有 Java 或 Python 基础的读者本模块半天内过完即可——这里不是语法教程，是"AI 应用里这些语法怎么用"的对照练习（每个 Demo 都标注了它在后续哪一篇会被用上）。与仓库「Python 异步 + FastAPI」体系（01-Python语言）的分工：那边是语言层系统学习，这里是动手验证层。

## 2. 类型注解：让代码可读可检

AI 应用的代码是给别人看、给 AI 改的，类型注解就是"代码的文档"。2026 年 Python 的类型系统已经足够强：内置泛型、Optional、Union，加上 `list[str]` 直接写。编辑器配 pyright（VS Code 默认）后，写错的类型当场标红——这是 AI 时代写 Python 的第一生产力。

```python
from typing import Optional

def chat_with_model(user_msg: str, history: list[dict] | None = None,
                    temperature: float = 0.7) -> str:
    """调大模型；history 为 None 时视为空对话。"""
    messages = [{"role": "user", "content": user_msg}]
    if history:
        messages = history + messages
    # ... 调用模型，返回文本
    return "模型回答"

count: int = 10
name: Optional[str] = None   # 等价 str | None
```

注意三个习惯：**函数必须有返回注解**（没写返回值的函数 AI 改起来最容易出错）；**docstring 一行说清做什么**——它会被喂给大模型做上下文；**`str | None` 与 `Optional[str]` 等价**，2026 推荐前者（Python 3.10+）。类型注解不是运行时约束（Python 不强制），但配合 IDE 检查能拦住绝大多数低级错误。

## 3. dataclass：结构化数据的容器

AI 应用里大量数据是"装字段的袋子"：模型返回、工具参数、配置。用普通字典（dict）也能装，但字段拼错要到运行时报错；用 dataclass 后，字段即文档、IDE 即补全。它是后面对接 Pydantic（Level2 必用）前的第一步。

```python
from dataclasses import dataclass, field

@dataclass
class ChatConfig:
    model: str = "deepseek-v4-flash"
    temperature: float = 0.7
    max_tokens: int = 4096
    system_prompt: str = field(default="你是 Python 学习助手", repr=False)

@dataclass
class ToolCall:
    name: str
    arguments: str   # JSON 字符串，需要 json.loads 解析
    call_id: str

cfg = ChatConfig()
print(cfg.model)          # deepseek-v4-flash
```

dataclass 自动生成 `__init__`、`__repr__`、`__eq__`，省掉样板代码。两个常用细节：**field(default=...) 处理默认值**，`field(repr=False)` 隐藏大字段（比如 system_prompt 打印时不想输出全文）；**@dataclass(frozen=True) 做不可变对象**（像工具调用的返回体，防止中途被改）。后续 06 篇的文档块、07 篇的工具调用都会用 dataclass 定义数据结构。

AI 应用里还有一种"半结构"数据——**结构固定但用字典更顺手**（比如 messages 数组、API 响应），用 **TypedDict** 给字典加类型，编辑器检查不输 dataclass：

```python
from typing import TypedDict

class Message(TypedDict):
    role: str          # system / user / assistant / tool
    content: str

def build_messages(system: str, user: str) -> list[Message]:
    return [{"role": "system", "content": system}, {"role": "user", "content": user}]
```

选型直觉：**要加行为（方法）用 dataclass，纯数据结构但想带类型提示用 TypedDict**——openai SDK 的 messages 参数就是这种字典形态，TypedDict 让"字典拼错 key"在编辑器阶段就暴露，而不是等 API 返回 400。

## 4. 异常处理：AI 调用必须三层防护

AI 调用是最典型的"不可靠外部依赖"：网络抖动、限流、模型宕机、余额不足都会抛异常。不处理异常的表现是脚本直接崩，Demo 阶段勉强能忍，Level2 会被打回重写。正确的三层防护：**捕获具体异常**（不裸 except）、**区分可重试与不可重试**、**失败也要有日志**。

```python
import time
from openai import APIError, APIConnectionError, RateLimitError

def call_with_retry(client, messages, retries: int = 3):
    for attempt in range(retries):
        try:
            return client.chat.completions.create(model="deepseek-v4-flash",
                                                  messages=messages)
        except RateLimitError:
            time.sleep(2 ** attempt)          # 429：指数退避
        except APIConnectionError:
            time.sleep(2 ** attempt)          # 网络抖动：同样退避重试
        except APIError:
            raise                              # 其他 API 错误：不重试，上抛
    raise RuntimeError("重试 3 次仍失败")
```

要点：**429 与网络错误重试**（指数退避 1s→2s→4s），**400 参数错误不重试**（重试只会浪费钱），**finally 里收尾**（Demo 阶段主要是打日志）。openai SDK 的异常体系：`RateLimitError`（429）、`APIConnectionError`（连不上）、`AuthenticationError`（401 Key 错）、`APIError`（其余）。后续每篇代码都会带上这个模式——它会在 09 篇的调试里反复被验证。

两个补充姿势：**异常也要"带上下文"**——`raise RuntimeError("调用模型失败") from e`（`from e` 保留原始异常链，排查时能看到根因）；**自定义异常让错误可区分**——业务上"文档未找到"和"模型调用失败"不该用同一个 except 处理，定义 `class DocNotFoundError(Exception): ...` 让上层分别兜底。AI 应用的异常设计原则一句话：**SDK 异常负责"调用失败"，自定义异常负责"业务失败"，两层都要有**。

## 5. 上下文管理器与 pathlib

AI 应用绕不开文件操作：读文档（06 RAG 的输入）、写日志、缓存中间结果。两个语言特性让文件代码干净：**with 自动关文件**、**pathlib 让路径跨平台**。

```python
from pathlib import Path

data_dir = Path("data")            # 相对项目根
data_dir.mkdir(exist_ok=True)

doc = Path("data/readme.txt")
doc.write_text("这是待分块的文档内容", encoding="utf-8")
print(doc.read_text(encoding="utf-8"))        # 带编码读，杜绝乱码

# with 上下文：数据库连接、文件句柄自动关闭
with open(doc, "r", encoding="utf-8") as f:
    lines = f.readlines()
```

两个易错点：**路径一律用 Path 不用字符串拼接**（`Path("data") / "readme.txt"`，Windows 反斜杠问题自动消失）；**读写必须显式写 encoding="utf-8"**——Windows 默认编码是 GBK，不写会乱码甚至报 UnicodeDecodeError，这是 09 篇编码类问题的头号来源。with 语句的自定义实现（`__enter__`/`__exit__`）Demo 阶段不用深究，会用就够。

pathlib 的两个高频操作顺手记下：**遍历目录**（`for p in data_dir.glob("*.md")`——RAG 批量导入文档时用它收集文件）；**判断存在**（`p.exists()`——入库前检查缓存目录）。AI 应用的文档输入输出几乎全走 pathlib，这两个操作覆盖大半场景。

## 6. logging：替代 print 调试

Demo 期 print 够用，但从本模块开始养成用 logging 的习惯——因为 AI 调用链路长（请求参数、返回内容、token 消耗都要留痕），print 无法分级、无法关停，日志可以。标准四件套：

```python
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    handlers=[logging.StreamHandler()],
)
log = logging.getLogger("level1")

log.info("调用模型 model=%s", "deepseek-v4-flash")   # 参数用 %s 惰性拼接
log.warning("响应截断: %s", "usage 字段缺失")
log.error("调用失败", exc_info=True)                  # 带堆栈
```

要点：**日志用 %s 占位惰性拼接**（不产生额外字符串开销）；**exc_info=True 带异常堆栈**（调试利器）；**分级用 INFO/WARNING/ERROR 三档**足够 Demo 使用。后续所有 Demo 的关键节点（发请求、拿到响应、检索到几条）都打一行日志——"留痕"的习惯在 Level2 可观测性章节会变成必须项，现在种下种子。

## 7. 函数参数与生成器：AI 代码的高频写法

三个在 AI 应用里高频出现、但常规语法教程不会单独讲的写法，各配一个 Demo：****kwargs 分发工具调用**（07 篇的核心）、**yield 生成器做流式**（03 篇流式、05 篇 st.write_stream 都要它）、**字典解包拼消息**（多轮对话的常用姿势）。

```python
# 1. **kwargs 分发：按工具名调函数（07 篇用）
def get_weather(city: str, unit: str = "celsius") -> str: ...
def get_time(city: str) -> str: ...

TOOLS = {"get_weather": get_weather, "get_time": get_time}

def execute_tool(name: str, args: dict) -> str:
    fn = TOOLS.get(name)
    if fn is None:
        return f"未知工具: {name}"
    return fn(**args)          # 字典展开成关键字参数

# 2. yield 生成器：把流式 chunk 包装成可消费的序列
def stream_chunks(text: str):
    for i in range(0, len(text), 5):
        yield text[i:i + 5]

for chunk in stream_chunks("你好"):     # for 循环逐段消费
    print(chunk, end="")

# 3. 字典解包拼消息：多轮对话的历史管理
def add_message(history: list[dict], role: str, content: str) -> list[dict]:
    return [*history, {"role": role, "content": content}]
```

三个写法的共同点：**它们都是"把数据形态转换交给语言"**——kwargs 把 dict 变参数、yield 把过程变序列、解包把列表变参数。理解这三个转换，后面读框架代码（LangChain 的调用链、Streamlit 的组件传参）会顺畅得多。**yield 是 Python 里最值得花时间理解的一个关键字**：它让函数变成"可暂停的序列"，流式输出的整个用户体验建立在它之上——面试问"流式怎么实现"时，回答"生成器 + 逐段 yield"是最标准的起点。配套的习惯是**生成器一律配合 for 或 next 消费**，不要手动 index 取值（生成器不是序列，下标访问会 TypeError）——这个细节在 05 篇 st.write_stream 里会再次出现。

> 🎯 **核心要点**：这六个语法特性不是知识，是**工具**——类型注解让代码可读、dataclass 装结构化数据、三层异常防护扛住 AI 调用的不可靠、pathlib 管文件、logging 留痕、kwargs/yield 串起工具分发与流式。后续 03-08 篇的每一段代码都会用到它们，本模块过完就能看懂 80% 的 AI 应用代码。

---

**下一模块**：[03 调用大模型 API 的第一个 Demo](./03-调用大模型%20API%20的第一个%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)

【参考来源】
- [Python 3.14 Release Notes](https://docs.python.org/3.14/whatsnew/3.14.html)
- [Dataclasses - Python Docs](https://docs.python.org/3/library/dataclasses.html)
- [Errors and Retries | openai-python SDK](https://github.com/openai/openai-python)
