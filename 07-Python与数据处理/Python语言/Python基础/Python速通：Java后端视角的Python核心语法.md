# Python 速通：Java 后端视角的 Python 核心语法

> **目标**：10-20 小时快速掌握 Python，能写可运行脚本即可，无需死磕语法细节
> **策略**：以 Java 为对照系，突出差异点，快速建立 Python 思维模型

---

## 1. 环境搭建

```bash
# 安装 Python 3.10+
python --version

# 包管理工具 pip
pip install requests flask gradio

# 虚拟环境（隔离项目依赖）
python -m venv venv
source venv/bin/activate   # Linux/Mac
venv\Scripts\activate      # Windows
```

---

## 2. 变量与数据类型

### Java vs Python 对照

```java
// Java — 静态类型
String name = "Alice";
int age = 25;
double score = 98.5;
boolean isActive = true;
```

```python
# Python — 动态类型，无需声明
name = "Alice"
age = 25
score = 98.5
is_active = True  # 注意大写 True/False
```

### 核心数据类型

```python
# 字符串
s = "hello world"
s.upper()        # 'HELLO WORLD'
s.split()        # ['hello', 'world']
f"name={name}"   # f-string 格式化

# 列表（≈ Java ArrayList + 更灵活）
items = [1, 2, 3]
items.append(4)
items[0]          # 1
items[-1]         # 最后一个：4
items[1:3]        # 切片：[2, 3]

# 字典（≈ Java HashMap）
user = {"name": "Alice", "age": 25}
user["name"]      # 'Alice'
user.get("email", "N/A")  # 安全访问

# 元组 — 不可变列表
point = (3, 4)

# 集合 — 去重
tags = {"python", "ai"}
```

---

## 3. 控制流

```python
# if/elif/else — 缩进即代码块
score = 85
if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
else:
    grade = "C"

# for — 直接遍历可迭代对象
for item in [1, 2, 3]:
    print(item)

for i in range(5):      # 0,1,2,3,4
    print(i)

for k, v in user.items():  # 遍历字典
    print(f"{k}: {v}")

# while
count = 0
while count < 3:
    count += 1

# 推导式 — Python 特色
squares = [x**2 for x in range(10)]          # 列表推导
evens = [x for x in range(10) if x % 2 == 0]  # 带过滤
```

---

## 4. 函数

```python
# 基本定义
def greet(name, greeting="Hello"):
    """文档字符串 — 相当于 JavaDoc"""
    return f"{greeting}, {name}"

# 调用
greet("Alice")               # 'Hello, Alice'
greet("Bob", greeting="Hi")  # 'Hi, Bob'

# 多返回值（本质是返回元组）
def min_max(items):
    return min(items), max(items)

lo, hi = min_max([3, 1, 4, 1, 5])  # lo=1, hi=5
```

---

## 5. 类与面向对象

```python
class ChatBot:
    """智能聊天机器人"""

    # 类变量
    default_model = "deepseek-chat"

    # 构造函数
    def __init__(self, name, model=None):
        self.name = name                    # 实例变量
        self.model = model or self.default_model
        self._history = []                  # 约定 _ 前缀表示私有

    # 实例方法
    def chat(self, message):
        self._history.append({"role": "user", "content": message})
        reply = self._call_api(message)
        self._history.append({"role": "assistant", "content": reply})
        return reply

    def _call_api(self, message):
        # 私有方法：调用大模型 API
        pass

    # 魔法方法
    def __str__(self):
        return f"ChatBot(name={self.name}, model={self.model})"
```

---

## 6. 模块与导入

```python
# 导入整个模块
import requests

# 导入特定函数
from flask import Flask, request

# 导入并重命名
import numpy as np

# 导入自定义模块
from myapp.chatbot import ChatBot

# 条件执行入口
if __name__ == "__main__":
    # 仅当直接运行此文件时执行
    app.run()
```

---

## 7. 文件与异常处理

```python
# 文件读取
with open("data.txt", "r", encoding="utf-8") as f:
    content = f.read()

# 逐行读取
with open("data.txt", "r") as f:
    for line in f:
        print(line.strip())

# 文件写入
with open("output.txt", "w") as f:
    f.write("hello world")

# 异常处理
try:
    result = 10 / 0
except ZeroDivisionError as e:
    print(f"Error: {e}")
finally:
    print("cleanup")
```

---

## 8. 常用内置工具

```python
# JSON 处理
import json
json.dumps({"name": "Alice"})           # Python → JSON 字符串
json.loads('{"name": "Alice"}')         # JSON 字符串 → Python

# 类型检查
isinstance(42, int)     # True
type(42)                # <class 'int'>

# 枚举
for i, item in enumerate(["a", "b"]):
    print(i, item)      # 0 a, 1 b

# zip — 并行迭代
for a, b in zip([1, 2], ["x", "y"]):
    print(a, b)         # 1 x, 2 y

# lambda — 匿名函数
sorted([3, 1, 2], key=lambda x: -x)  # [3, 2, 1]
```

---

## 9. Java 开发者高频踩坑

| Java 习惯 | Python 正确写法 |
|-----------|----------------|
| `name.equals("Alice")` | `name == "Alice"` |
| `// 注释` | `# 注释` |
| `&&`, `\|\|`, `!` | `and`, `or`, `not` |
| `null` | `None` |
| `array.length` | `len(array)` |
| `Map<String, Object>` | `dict` |
| `List<T>` | `list` |
| `for (int i=0; i<n; i++)` | `for i in range(n):` |
| `try { } catch { }` 用 `{ }` | `try: except:` 用缩进 |

---

## 10. 大模型开发常用库速查

```python
# HTTP 请求
import requests
resp = requests.post(url, headers=headers, json=data, stream=True)

# Web 框架
from flask import Flask, request, jsonify
import gradio as gr  # 快速搭建 ML Demo

# 数据处理
import json
import re

# AI 核心库
from openai import OpenAI          # OpenAI/兼容 API
from langchain.llms import ...     # LangChain
import chromadb                    # 向量数据库
```

---

## 学习检查清单

- [ ] 能用 Python 写 100 行以内的脚本
- [ ] 理解动态类型 vs 静态类型的区别
- [ ] 熟练使用列表推导式
- [ ] 能用 `requests` 库调用 REST API 并解析 JSON
- [ ] 能写出带 `__init__` 和方法的类
- [ ] 理解 `if __name__ == "__main__"` 的作用
