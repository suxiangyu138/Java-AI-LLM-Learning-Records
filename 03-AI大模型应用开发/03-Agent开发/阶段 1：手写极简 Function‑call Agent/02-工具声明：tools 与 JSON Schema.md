# 02 工具声明：tools 与 JSON Schema

> 定位：告诉模型"你能用哪些工具、怎么用"——tools 数组三要素、描述纪律、strict 模式（2026-08 基准）

## 📚 目录

1. [tools 参数的本质](#1-tools-参数的本质)
2. [工具声明的三要素](#2-工具声明的三要素)
3. [参数 JSON Schema 详解](#3-参数-json-schema-详解)
4. [描述纪律：为什么 description 决定成败](#4-描述纪律为什么-description-决定成败)
5. [strict 模式与类型约束](#5-strict-模式与类型约束)
6. [tool_choice：让模型调还是不调](#6-tool_choice让模型调还是不调)
7. [常见坑速查](#7-常见坑速查)

## 1. tools 参数的本质

`tools` 是随请求发送给模型的**工具说明书**——模型不会执行它，只会"读懂并决定调用"：

```text
tools = [说明书1, 说明书2, ...]
                  ↓ 随每次请求发送
模型"阅读"说明书 → 判断当前问题需要哪个工具 → 返回"调用意图"（tool_calls）
                  ↓
真正执行的是你的代码（模型不执行任何东西！）
```

> 🎯 **核心要点**：模型只返回"调用意图"（工具名 + 参数 JSON），**执行永远发生在你的进程里**。这是 Agent 安全的根基——你拥有最终执行权。

## 2. 工具声明的三要素

每个工具声明由三部分组成：

```python
tools = [
    {
        "type": "function",                    # ① 固定类型
        "function": {                          # ② function 容器
            "name": "get_weather",             #   ③a 名字：动词开头、唯一、≤64 字符
            "description": "查询指定城市的当前天气，返回温度与天气状况",   # ③b 做什么+何时用
            "parameters": {                    #   ③c JSON Schema（参数说明）
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "城市名，如 北京、Shanghai"
                    },
                    "unit": {
                        "type": "string",
                        "enum": ["celsius", "fahrenheit"],
                        "description": "温度单位"
                    }
                },
                "required": ["city"]
            }
        }
    },
]
```

| 要素 | 作用 | 易错点 |
|------|------|--------|
| `name` | 模型选择工具的依据 | 必须与执行函数名一致；重名会导致错调 |
| `description` | 模型判断"何时用"的依据 | 太模糊 → 模型乱调；太长 → 占上下文 |
| `parameters` | 模型填参数的模板 | 类型/必填定义不严 → 参数错误 |

## 3. 参数 JSON Schema 详解

### 3.1 常用类型

| 类型 | 用法 | 示例 |
|------|------|------|
| `string` | 文本 | `{"type": "string", "description": "..."}` |
| `number`/`integer` | 数字 | `{"type": "integer", "minimum": 1}` |
| `boolean` | 布尔 | `{"type": "boolean"}` |
| `array` | 列表 | `{"type": "array", "items": {"type": "string"}}` |
| `object` | 嵌套 | `{"type": "object", "properties": {...}}` |
| `enum` | 枚举 | `{"type": "string", "enum": ["a", "b"]}` |

### 3.2 可选参数的默认值处理

JSON Schema 没有 `default` 语义（多数实现忽略它）——**默认值要在你的执行函数里给**：

```python
# schema 里不写 default
"parameters": {
    "properties": {
        "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]}
    }
}

# 执行函数里给默认值
def get_weather(city: str, unit: str = "celsius") -> dict:
    ...
```

### 3.3 最小够用原则

```text
❌ 过度设计：每个参数写 5 行说明 + 复杂嵌套 → 模型混淆、token 浪费
✅ 最小够用：类型 + 一行描述 + 必填列表 → 模型理解成本最低
```

## 4. 描述纪律：为什么 description 决定成败

模型的"工具选择"完全依赖 description 文字——**描述写得越清楚，模型调用越准**：

| 原则 | 好描述 | 差描述 |
|------|--------|--------|
| 做什么 | "查询指定城市的当前天气" | "天气工具" |
| 何时用 | "当用户询问天气/气温/降雨时使用" | （缺失） |
| 边界 | "仅支持中国主要城市；查不到返回错误" | （缺失） |
| 参数含义 | "城市名，如 北京、Shanghai" | "城市" |

**描述公式**：`做什么 + 何时用 + 边界条件`

> 💡 一个经验值：工具定义单个约 100-200 token（2026 共识）。工具集大了要控制总量——每轮全量发送会吃掉上下文预算。

## 5. strict 模式与类型约束

| 开关 | 作用 | 代价 |
|------|------|------|
| `strict: true` | 模型输出必须严格符合 schema（类型/必填硬校验） | 参数必须全 required、拒绝 anyOf 等复杂结构 |
| 不开启 | 模型"尽力"符合 schema | 类型错误、缺字段偶发 |

```python
{
    "type": "function",
    "function": {
        "name": "get_weather",
        "strict": True,          # 2026 主流供应商支持
        "parameters": {
            "type": "object",
            "properties": {...},
            "required": ["city", "unit"],   # strict 模式必须全 required
            "additionalProperties": False   # strict 模式建议显式关闭
        }
    }
}
```

> ⚠️ **strict 的隐藏坑（2026 高频）**：strict 与 `response_format`/tool_choice 同时使用时可能冲突（如 strict 服务端上约束抑制工具调用）——阶段 1 先用最简单组合（strict + auto），复杂组合见失败模式体系 04 篇。

## 6. tool_choice：让模型调还是不调

| 取值 | 行为 | 场景 |
|------|------|------|
| `"auto"`（默认） | 模型自主决定调不调 | 大多数场景 |
| `"required"` | 至少调一个工具 | 必须查询实时数据（股价、天气） |
| `"none"` | 禁止调用 | 纯问答轮次 |
| `{"type":"function","function":{"name":"X"}}` | 强制指定工具 | 结构化提取 |

**实战经验**：

> 用户问"苹果股价多少"时，`"auto"` 下模型可能直接凭记忆回答过期价格——**需要外部数据的查询，用 `"required"` 或指定工具**。

## 7. 常见坑速查

| 坑 | 现象 | 修复 |
|----|------|------|
| schema 与执行函数签名不一致 | 模型传的参数函数接不住 | 用同一份定义（见 05 篇注册表） |
| description 写"调参用" | 模型不敢调/乱调 | 按公式重写描述 |
| 参数名中文/特殊字符 | 模型传错参数 | 参数名用英文蛇形命名 |
| 工具数量 >30 | 模型选择准确率下降 | 分组/动态过滤（阶段 2） |
| 声明里用了 `default` | 部分实现忽略导致参数缺失 | 默认值放执行函数 |
| strict + 工具叠加 | 工具调用被抑制 | 回合分离（失败模式 04 篇） |

> 🎯 **核心要点**：工具声明的本质是**"写给模型看的 API 文档"**——文档质量决定调用准确率。把每个 description 当成面试回答写：做什么、何时用、边界在哪。

---

**返回总览**：[00-阶段总览：手写极简Function-call Agent](00-阶段总览：手写极简Function-call%20Agent.md) / **上一模块**：[01-环境准备与第一声对话](01-环境准备与第一声对话.md) / **下一模块**：[03-模型返回：tool_calls 解析](03-模型返回：tool_calls%20解析.md)
