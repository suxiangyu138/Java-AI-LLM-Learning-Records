# Python 速通：Java 后端视角的 Python 核心语法

> **核心摘要**：10-20 小时快速掌握 Python，以 Java 为对照系，突出差异点，快速建立 Python 思维模型。目标是能写可运行脚本，无需死磕语法细节。

---

## 一、环境搭建

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

## 二、变量与数据类型

### Java vs Python 对照

| Java | Python |
|------|--------|
| `String name = "Alice";` | `name = "Alice"` |
| `int age = 25;` | `age = 25` |
| `boolean isActive = true;` | `is_active = True` |

### 核心数据类型

```python
# 字符串
s = "hello world"
f"name={name}"   # f-string 格式化

# 列表（≈ Java ArrayList + 更灵活）
items = [1, 2, 3]
items.append(4)
items[1:3]        # 切片：[2, 3]

# 字典（≈ Java HashMap）
user = {"name": "Alice", "age": 25}
user.get("email", "N/A")  # 安全访问

# 元组 — 不可变列表
point = (3, 4)
```

## 三、控制流

```python
# 缩进即代码块
score = 85
if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
else:
    grade = "C"

# 推导式 — Python 特色
squares = [x**2 for x in range(10)]
evens = [x for x in range(10) if x % 2 == 0]
```

## 四、函数

```python
def greet(name, greeting="Hello"):
    """文档字符串 — 相当于 JavaDoc"""
    return f"{greeting}, {name}"

# 多返回值（本质是返回元组）
def min_max(items):
    return min(items), max(items)

lo, hi = min_max([3, 1, 4, 1, 5])
```

## 五、类与面向对象

```python
class ChatBot:
    default_model = "deepseek-chat"  # 类变量

    def __init__(self, name, model=None):
        self.name = name
        self.model = model or self.default_model
        self._history = []  # 约定 _ 前缀表示私有

    def chat(self, message):
        self._history.append({"role": "user", "content": message})
        return self._call_api(message)

    def __str__(self):
        return f"ChatBot(name={self.name}, model={self.model})"
```

## 六、文件与异常处理

```python
# 文件读取
with open("data.txt", "r", encoding="utf-8") as f:
    content = f.read()

# 异常处理
try:
    result = 10 / 0
except ZeroDivisionError as e:
    print(f"Error: {e}")
finally:
    print("cleanup")
```

## 七、大模型开发常用库速查

```python
# HTTP 请求
import requests
resp = requests.post(url, headers=headers, json=data, stream=True)

# Web 框架
from flask import Flask, request, jsonify
import gradio as gr  # 快速搭建 ML Demo

# AI 核心库
from openai import OpenAI          # OpenAI/兼容 API
import chromadb                    # 向量数据库
```

## 八、Java 开发者高频踩坑

| Java 习惯 | Python 正确写法 |
|-----------|----------------|
| `name.equals("Alice")` | `name == "Alice"` |
| `&&`, `||`, `!` | `and`, `or`, `not` |
| `null` | `None` |
| `array.length` | `len(array)` |
| `for (int i=0; i<n; i++)` | `for i in range(n):` |
| `try { } catch { }` | `try: except:` |

## 核心要点回顾

- Python 是动态类型语言，无需声明变量类型
- 缩进（4 空格）替代花括号表示代码块
- 列表推导式是 Python 最具特色的语法
- `__init__` 是构造函数，`self` 相当于 Java 的 `this`
- 理解 `if __name__ == "__main__"` 的作用

## 学习检查清单

- [ ] 能用 Python 写 100 行以内的脚本
- [ ] 理解动态类型 vs 静态类型的区别
- [ ] 熟练使用列表推导式
- [ ] 能用 `requests` 库调用 REST API 并解析 JSON
- [ ] 能写出带 `__init__` 和方法的类
- [ ] 理解 `if __name__ == "__main__"` 的作用

## 参考资料

1. Python 官方文档 - docs.python.org
2. Python 之禅 - PEP 20
