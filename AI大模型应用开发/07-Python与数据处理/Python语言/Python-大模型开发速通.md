# Python 速通：大模型开发必备语法

> **核心摘要**：目标读者为有 Java 基础的开发者。快速掌握 Python 中与大模型开发直接相关的语法，能写出可运行的脚本即可，无需死磕语法细节。

> 前置阅读：[[Python速通：Java后端视角的Python核心语法]]

---

## 一、Python vs Java 语法速查

| 特性 | Java | Python |
|------|------|--------|
| 变量声明 | `String name = "hello";` | `name = "hello"` |
| 代码块 | `{}` 花括号 | 缩进（4 空格） |
| 语句结束 | `;` 分号 | 换行 |
| 注释 | `//` 或 `/* */` | `#` 或 `""" """` |
| 布尔值 | `true / false` | `True / False` |
| 空值 | `null` | `None` |
| 类型检查 | 编译时静态检查 | 运行时动态类型 |

## 二、基础语法（15 分钟速通）

### 2.1 变量与数据类型

```python
# 字符串操作（大模型开发高频）
text = "Hello, {name}"
formatted = f"Hello, {name}"                      # f-string 格式化（最常用）
prompt = """你是一个{role}。请根据以下内容回答问题：{context}""".format(role="Java专家", context="...")
```

### 2.2 控制流

```python
# 列表推导式（Pythonic 写法）
tokens = [t.strip() for t in text.split() if t]  # 等同于 Java Stream.map().filter()

# while 循环（重试机制）
count = 0
while count < 3:
    response = call_api(prompt)
    if response: break
    count += 1
```

### 2.3 函数定义

```python
def call_llm(prompt: str, model: str = "deepseek") -> str:
    """调用大模型 API"""
    response = api.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}]
    )
    return response.choices[0].message.content

# 可变参数（多轮对话消息组装）
def build_messages(system_prompt: str, *user_messages: str) -> list[dict]:
    messages = [{"role": "system", "content": system_prompt}]
    for msg in user_messages:
        messages.append({"role": "user", "content": msg})
    return messages
```

## 三、面向对象（5 分钟上手）

```python
class LLMClient:
    """大模型客户端封装"""
    def __init__(self, api_key: str, base_url: str, model: str = "deepseek-chat"):
        self.api_key = api_key
        self.base_url = base_url
        self.model = model
        self._history: list[dict] = []  # 单下划线 = protected 约定

    def chat(self, prompt: str) -> str:
        self._history.append({"role": "user", "content": prompt})
        return response

# 继承 + 生成器
class StreamingLLMClient(LLMClient):
    def chat_stream(self, prompt: str):
        for chunk in self._call_api_stream(prompt):
            yield chunk  # yield = Java 的 Iterator + lazy evaluation
```

**关键差异**：
- 没有 `private` / `protected` 关键字，靠 `_` 前缀约定
- `self` 相当于 Java 的 `this`，且必须显式声明为第一个参数

## 四、大模型开发高频模块

| 模块 | 用途 | 核心函数/类 |
|------|------|-------------|
| `requests` | HTTP API 调用 | `requests.post()`, `response.json()` |
| `json` | 数据序列化 | `json.dumps()`, `json.loads()` |
| `re` | 正则表达式 | `re.findall()`, `re.sub()` |
| `typing` | 类型注解 | `Optional`, `List`, `Dict` |
| `asyncio` | 异步并发 | `asyncio.run()`, `asyncio.gather()` |

## 五、异常处理与重试

```python
import time
from typing import Callable

def retry_on_failure(func: Callable, max_retries: int = 3, base_delay: float = 1.0):
    """带指数退避的重试装饰器"""
    for attempt in range(max_retries):
        try:
            return func()
        except RateLimitError:
            wait = base_delay * (2 ** attempt)  # 指数退避
            print(f"Rate limited, waiting {wait}s...")
            time.sleep(wait)
        except Exception as e:
            if attempt == max_retries - 1:
                raise
            print(f"Attempt {attempt + 1} failed: {e}")
            time.sleep(base_delay)
```

## 六、Java 到 Python 常见坑

| 坑 | Java 习惯 | Python 正确写法 |
|----|-----------|----------------|
| 字符串比较 | `str.equals("x")` | `str == "x"` |
| 检查 None | `obj == null` | `obj is None` |
| 检查类型 | `obj instanceof String` | `isinstance(obj, str)` |
| 遍历索引 | `for (int i=0; ...)` | `for i, item in enumerate(lst):` |
| 文件路径 | `"C:\\path\\file"` | `r"C:\path\file"` 或 `"C:/path/file"` |

## 学习路线建议

1. **第 1 天**（2 小时）：变量、控制流、函数 → 写一个简单的 API 调用脚本
2. **第 2 天**（2 小时）：类、文件 IO、异常处理 → 封装一个 LLMClient 类
3. **第 3 天**（2 小时）：json、re、生成器 → 实现流式对话 + 响应解析
4. **第 4-5 天**（4 小时）：动手写实战项目

> **核心原则**：不需要学完再动手，学到什么就立即在项目里用。

## 参考资料

1. OpenAI Python SDK 文档 - API 调用指南
2. Python requests 库文档 - HTTP 请求
3. Python asyncio 文档 - 异步编程
