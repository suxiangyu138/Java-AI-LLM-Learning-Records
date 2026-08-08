# 02 @tool 工具定义

> 定位：框架版工具声明——`@tool` 装饰器、docstring 铁律、schema 自动生成、与手写注册表的对照（2026-08 基准）

## 📚 目录

1. [@tool 最小示例](#1-tool-最小示例)
2. [docstring 铁律：它是工具的"prompt"](#2-docstring-铁律它是工具的prompt)
3. [schema 自动生成规则](#3-schema-自动生成规则)
4. [高级注入：InjectedState 与 InjectedStore](#4-高级注入injectedstate-与-injectedstore)
5. [结构化返回与 ToolConfig](#5-结构化返回与-toolconfig)
6. [与手写注册表的对照](#6-与手写注册表的对照)
7. [常见坑速查](#7-常见坑速查)

## 1. @tool 最小示例

```python
from langchain_core.tools import tool

@tool
def get_weather(city: str, unit: str = "celsius") -> str:
    """查询指定城市的当前天气。当用户询问天气、气温、降雨时使用。

    Args:
        city: 城市名，如 北京、Shanghai
        unit: 温度单位，celsius 或 fahrenheit
    """
    return f"{city} 当前 22℃，晴天（{unit}）"

# 使用：直接作为工具列表传给 agent
# tools = [get_weather]
```

| 要素 | 作用 | 对应手写代码 |
|------|------|------------|
| `@tool` 装饰器 | 函数 → BaseTool 对象 | 手写 tools 数组 |
| 类型注解 | 自动生成参数 schema | 手写 properties/required |
| **docstring** | 工具描述（模型的"何时用"依据） | 手写 description |
| 返回值 | 直接作为工具结果（自动转字符串） | execute_tool 的返回 |

> 🎯 **核心要点**：`@tool` 的本质 = **"声明与实现同源"的产品化**（阶段 1 的 07 篇手写过 schema 自动生成）。签名 + docstring 即 schema，永远不会不一致。

## 2. docstring 铁律：它是工具的"prompt"

> ⚠️ **框架级铁律**：docstring 是模型判断"何时调用此工具"的唯一依据——**没写 docstring 或写得太差，模型就不调工具**（社区最高频问题："agent not calling tools"）。

| 原则 | 好 docstring | 差 docstring |
|------|-------------|-------------|
| 做什么 | "查询指定城市的当前天气" | "天气工具" |
| 何时用 | "当用户询问天气、气温、降雨时使用" | （缺失） |
| 边界 | "仅支持中国主要城市；查不到返回错误信息" | （缺失） |
| 参数说明 | "city: 城市名，如 北京、Shanghai" | （缺失） |

**标准 docstring 模板**：

```python
@tool
def search_user(user_id: int) -> str:
    """根据用户 ID 查询用户信息。当用户提到"用户/某人"并给出 ID 时使用。

    Args:
        user_id: 用户唯一标识（正整数）

    Returns:
        用户信息的 JSON 字符串；查不到返回错误说明。
    """
```

> 💡 描述公式与阶段 2 一致：**做什么 + 何时用 + 边界**。框架只是把"描述"变成了 docstring 的位置。

## 3. schema 自动生成规则

| 注解 | 生成结果 | 示例 |
|------|---------|------|
| `str` | `{"type": "string"}` | `city: str` |
| `int` / `float` | `{"type": "integer"}` / `{"type": "number"}` | `user_id: int` |
| `bool` | `{"type": "boolean"}` | `verbose: bool` |
| `list[str]` | `{"type": "array", "items": {"type": "string"}}` | `tags: list[str]` |
| `Literal["a","b"]` | `enum` | `unit: Literal["celsius", "fahrenheit"]` |
| 默认值 | 可选参数 | `unit: str = "celsius"` |
| Pydantic 模型 | 嵌套对象 | `params: OrderInfo` |

```python
from typing import Literal

@tool
def get_weather(
    city: str,
    unit: Literal["celsius", "fahrenheit"] = "celsius",  # → enum + 可选
) -> str:
    """..."""
```

> 💡 **enum 用 Literal**：手写 schema 的 enum 字段，框架版用 `Literal[...]` 注解，自动生成——同源原则的又一处体现。

## 4. 高级注入：InjectedState 与 InjectedStore

工具需要访问 Agent 内部状态时，用注入类型（参数**不暴露给模型**）：

```python
from langchain_core.tools import InjectedState, InjectedStore
from langchain_core.store import BaseStore

@tool
def check_balance(account: str, state: InjectedState) -> str:
    """查询账户余额。当用户询问余额时使用。"""
    # state 是 Agent 当前状态（模型看不到这个参数，也传不了）
    session = state.get("session_id", "unknown")
    return f"[会话 {session}] 账户 {account} 余额查询结果"

@tool
def save_note(content: str, store: InjectedStore) -> str:
    """保存一条笔记。当用户说"记一下/记住"时使用。"""
    store.put(("notes", "default"), "latest", content)
    return "已保存"
```

| 注入类型 | 来源 | 模型可见？ |
|---------|------|:---:|
| `InjectedState` | Agent 运行时状态 | ❌（不生成 schema） |
| `InjectedStore` | 跨会话存储 | ❌ |

> 💡 对应手写：就是你在 execute_tool 里"额外传入的上下文"——框架把它变成了声明式注入。

## 5. 结构化返回与 ToolConfig

```python
from pydantic import BaseModel, Field

class WeatherResult(BaseModel):
    city: str = Field(description="城市名")
    temperature: float
    condition: str

@tool(response_format="content_and_artifact", parse_docstring=True)
def get_weather_structured(city: str) -> tuple[str, WeatherResult]:
    """查询天气，返回结构化结果。"""
    return "北京 22℃", WeatherResult(city=city, temperature=22.0, condition="sunny")
```

| 选项 | 作用 |
|------|------|
| `response_format` | content 与 artifact（结构化数据）分离 |
| `parse_docstring` | 从 docstring 的 Args 段解析参数描述 |
| `return_direct` | 工具结果直接作为最终回答（不再回模型） |

## 6. 与手写注册表的对照

| 维度 | 手写（阶段 1） | @tool（框架） |
|------|--------------|--------------|
| 定义位置 | 函数 + 注册表 dict + schema 三处 | 一处（装饰器函数） |
| schema 来源 | 手写 JSON | 签名/docstring 自动生成 |
| 描述来源 | 手写 description | docstring |
| 错误处理 | execute_tool try/except | ToolNode 内置（异常转错误回传） |
| 状态访问 | 手写额外参数 | InjectedState/Store 声明式 |
| 优点 | 全透明、零依赖 | 少写 70% 样板、永不脱节 |
| 缺点 | 样板多、易脱节 | 隐式行为需理解（04 篇） |

> 🎯 **核心要点**：@tool 的学习重点是**"它替你做了什么"**——签名 → schema、docstring → description、异常 → 错误回传。这些你都会手写，现在只是换了位置。docstring 质量依然是工具成败的第一变量。

## 7. 常见坑速查

| 坑 | 现象 | 修复 |
|----|------|------|
| 无 docstring | 模型从不调该工具 | 补"做什么+何时用+边界" |
| 类型注解缺失 | schema 没有该参数 | 补类型注解 |
| 可变默认值 | 共享状态污染 | 用 InjectedState，不塞全局 |
| 返回 dict 而非 str | 结果格式意外 | 返回 str 或 JSON 字符串 |
| 工具名冲突 | 同名工具互相覆盖 | 装饰器改名 `@tool(name="...")` |
| 参数太多（>10） | 模型填错率高 | 拆工具（阶段 2 教训的框架版） |

---

**返回总览**：[00-阶段总览：LangChain Agent 学习](00-阶段总览：LangChain%20Agent%20学习.md) / **上一模块**：[01-生态全景：LangChain 1.0 组件地图](01-生态全景：LangChain%201.0%20组件地图.md) / **下一模块**：[03-create_agent：官方新 API](03-create_agent：官方新%20API.md)
