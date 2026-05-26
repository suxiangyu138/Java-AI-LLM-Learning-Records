# Python 速通：大模型开发必备语法

> **目标读者**：有 Java 基础的开发者，快速掌握 Python 中与大模型开发直接相关的语法
> **前置条件**：熟悉至少一门编程语言（Java/C++/Go 等）
> **学习策略**：能写出可运行的脚本即可，无需死磕语法细节

---

## 1. Python vs Java 语法速查

| 特性 | Java | Python |
|------|------|--------|
| 变量声明 | `String name = "hello";` | `name = "hello"` |
| 代码块 | `{}` 花括号 | 缩进（4 空格） |
| 语句结束 | `;` 分号 | 换行 |
| 注释 | `//` 或 `/* */` | `#` 或 `""" """` |
| 布尔值 | `true / false` | `True / False` |
| 空值 | `null` | `None` |
| 类型检查 | 编译时静态检查 | 运行时动态类型 |

---

## 2. 基础语法（15 分钟速通）

### 2.1 变量与数据类型

```python
# 基本类型
name: str = "hello"          # 字符串（类型注解可选）
age: int = 25                # 整数
price: float = 9.99          # 浮点数
is_active: bool = True       # 布尔值

# 复合类型
scores: list = [85, 92, 78]            # 列表（类似 Java ArrayList）
config: dict = {"model": "deepseek"}    # 字典（类似 Java HashMap）
tags: set = {"python", "ai"}           # 集合（不重复元素）
point: tuple = (3, 4)                  # 元组（不可变列表）

# 字符串操作（大模型开发高频）
text = "Hello, {name}"
formatted = f"Hello, {name}"           # f-string 格式化（最常用）
prompt = """
你是一个{role}。
请根据以下内容回答问题：
{context}
""".format(role="Java专家", context="...")  # format 方法
```

### 2.2 控制流

```python
# if-elif-else
score = 85
if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
else:
    grade = "C"

# for 循环（大模型开发高频）
models = ["gpt-4", "deepseek", "qwen"]
for model in models:
    print(f"Testing {model}")

# 列表推导式（Pythonic 写法）
tokens = [t.strip() for t in text.split() if t]  # 等同于 Java Stream.map().filter()

# while 循环
count = 0
while count < 3:
    response = call_api(prompt)
    if response: break
    count += 1
```

### 2.3 函数定义

```python
# 基本函数
def call_llm(prompt: str, model: str = "deepseek") -> str:
    """调用大模型 API

    Args:
        prompt: 用户输入的提示词
        model: 模型名称，默认 deepseek

    Returns:
        模型的文本响应
    """
    response = api.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}]
    )
    return response.choices[0].message.content

# 可变参数（大模型开发高频：多轮对话消息组装）
def build_messages(system_prompt: str, *user_messages: str) -> list[dict]:
    messages = [{"role": "system", "content": system_prompt}]
    for msg in user_messages:
        messages.append({"role": "user", "content": msg})
    return messages
```

---

## 3. 面向对象（Java 程序员 5 分钟上手）

```python
class LLMClient:
    """大模型客户端封装"""

    def __init__(self, api_key: str, base_url: str, model: str = "deepseek-chat"):
        self.api_key = api_key
        self.base_url = base_url
        self.model = model
        self._history: list[dict] = []  # 单下划线 = protected 约定

    def chat(self, prompt: str) -> str:
        """发送对话请求"""
        self._history.append({"role": "user", "content": prompt})
        # ... API 调用逻辑
        return response

    def clear_history(self):
        """清空对话历史"""
        self._history.clear()

# 继承
class StreamingLLMClient(LLMClient):
    """支持流式输出的客户端"""

    def chat_stream(self, prompt: str):
        """流式返回对话结果（生成器）"""
        for chunk in self._call_api_stream(prompt):
            yield chunk  # yield = Java 的 Iterator + lazy evaluation
```

**关键差异**：
- 没有 `private` / `protected` 关键字，靠 `_` 前缀约定
- `self` 相当于 Java 的 `this`，且必须显式声明为第一个参数
- 多继承用 Mixin 模式，而非 Java 的单继承 + 多接口

---

## 4. 大模型开发高频模块

### 4.1 requests — HTTP API 调用

```python
import requests

# POST 请求（调用 DeepSeek API）
response = requests.post(
    url="https://api.deepseek.com/v1/chat/completions",
    headers={
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    },
    json={
        "model": "deepseek-chat",
        "messages": [{"role": "user", "content": "你好"}],
        "stream": False
    },
    timeout=30  # 必须设置超时
)
result = response.json()
print(result["choices"][0]["message"]["content"])
```

### 4.2 json — 数据序列化（Prompt 组装高频）

```python
import json

# Python dict → JSON 字符串
config = {"model": "deepseek", "temperature": 0.7, "max_tokens": 2048}
json_str = json.dumps(config, ensure_ascii=False, indent=2)

# JSON 字符串 → Python dict
with open("config.json", "r", encoding="utf-8") as f:
    config = json.load(f)

# 大模型 Tool Calling 参数解析
tool_args = json.loads(function_call.arguments)  # JSON str → dict
```

### 4.3 re — 正则表达式（响应解析必备）

```python
import re

# 从模型回复中提取代码块
code_pattern = r"```(\w+)?\n(.*?)```"
matches = re.findall(code_pattern, response, re.DOTALL)
for lang, code in matches:
    print(f"Language: {lang or 'unknown'}")
    print(code)

# 清洗模型输出
cleaned = re.sub(r"\s+", " ", response)  # 合并多余空白
cleaned = re.sub(r"[^一-鿿\w\s]", "", cleaned)  # 去除非中英文符号
```

### 4.4 typing — 类型注解（提升代码可读性）

```python
from typing import Optional

# 大模型开发中的类型定义
Message = dict[str, str]  # {"role": "...", "content": "..."}

def chat(
    messages: list[Message],
    model: str = "deepseek-chat",
    temperature: float = 0.7,
    max_tokens: Optional[int] = None,  # Optional[T] = T | None
) -> str:
    ...
```

### 4.5 asyncio + aiohttp — 异步并发（进阶）

```python
import asyncio
import aiohttp

async def call_llm_async(session: aiohttp.ClientSession, prompt: str) -> str:
    """异步调用大模型（可并发处理多个请求）"""
    async with session.post(
        url=API_URL,
        headers=HEADERS,
        json={"messages": [{"role": "user", "content": prompt}]}
    ) as resp:
        data = await resp.json()
        return data["choices"][0]["message"]["content"]

async def batch_process(prompts: list[str]) -> list[str]:
    """批量并发处理多个 Prompt"""
    async with aiohttp.ClientSession() as session:
        tasks = [call_llm_async(session, p) for p in prompts]
        return await asyncio.gather(*tasks)

# 运行
results = asyncio.run(batch_process(["翻译: hello", "总结: ..."]))
```

---

## 5. 异常处理与重试（生产环境必备）

```python
import time
from typing import Callable

def retry_on_failure(
    func: Callable,
    max_retries: int = 3,
    base_delay: float = 1.0
):
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
                raise  # 最后一次重试仍失败则抛出
            print(f"Attempt {attempt + 1} failed: {e}")
            time.sleep(base_delay)
```

---

## 6. pip 与虚拟环境

```bash
# 创建虚拟环境（隔离项目依赖）
python -m venv llm-env

# 激活虚拟环境
# Windows:
llm-env\Scripts\activate
# Mac/Linux:
source llm-env/bin/activate

# 安装常用包
pip install requests openai langchain chromadb fastapi uvicorn

# 导出依赖列表
pip freeze > requirements.txt

# 从依赖列表安装
pip install -r requirements.txt
```

---

## 7. 从 Java 到 Python 的常见坑

| 坑 | Java 习惯 | Python 正确写法 |
|----|-----------|-----------------|
| 字符串比较 | `str.equals("x")` | `str == "x"` |
| 检查 None | `obj == null` | `obj is None` |
| 检查类型 | `obj instanceof String` | `isinstance(obj, str)` |
| 字典取值 | `map.get("key")` | `d.get("key")` 或 `d["key"]`（后者 KeyError） |
| 遍历索引 | `for (int i=0; ...)` | `for i, item in enumerate(lst):` |
| 拼接字符串 | `StringBuilder` / `+` | `f"{var1} {var2}"` 或 `"".join()` |
| 空列表判空 | `list.isEmpty()` | `if not lst:` |
| 文件路径 | `"C:\\path\\file"` | `r"C:\path\file"` 或 `"C:/path/file"` |

---

## 学习路线建议

1. **第 1 天**（2 小时）：变量、控制流、函数 → 写一个简单的 API 调用脚本
2. **第 2 天**（2 小时）：类、文件 IO、异常处理 → 封装一个 LLMClient 类
3. **第 3 天**（2 小时）：json、re、生成器 → 实现流式对话 + 响应解析
4. **第 4-5 天**（4 小时）：动手写阶段一的实战项目（智能聊天机器人）

> **核心原则**：不需要学完再动手，学到什么就立即在项目里用。遇到不会的语法，搜索 "Python how to X" 比翻书快 10 倍。
