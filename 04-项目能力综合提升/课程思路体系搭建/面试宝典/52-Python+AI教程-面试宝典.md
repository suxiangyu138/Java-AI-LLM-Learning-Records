# Python+AI教程 面试宝典
> 基于黑马程序员185集全栈教程大纲，覆盖Python核心语法、大模型调用、网络爬虫、数据分析及Web应用开发面试考点

## 目录
1. [一、基础概念速答](#一基础概念速答15-20题)
2. [二、深度原理剖析](#二深度原理剖析10-15题)
3. [三、实战场景题](#三实战场景题8-12题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1.1 Python可变与不可变类型

| 类别 | 类型 | 示例 |
|------|------|------|
| **可变 (Mutable)** | list, dict, set, bytearray | `lst = [1,2]; lst[0]=99` |
| **不可变 (Immutable)** | int, float, str, tuple, frozenset, bytes | `s = "hi"; s[0]="H"` 报错 |

> 💡 不可变类型作为 dict 的 key，而 list 不可以。函数参数的默认值不要用可变类型（如 `def f(lst=[])` 是经典坑点）。

### 1.2 列表推导式与生成器

```python
# 列表推导式 — 立即计算全部元素
squares = [x**2 for x in range(10)]

# 生成器表达式 — 惰性求值，节省内存
squares_gen = (x**2 for x in range(10))  # 返回 generator 对象
next(squares_gen)  # 0

# 生成器函数 — yield 关键字
def fibonacci():
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b
```

| 特性 | 列表推导式 | 生成器表达式 |
|------|-----------|-------------|
| 计算时机 | 立即 | 惰性 |
| 内存占用 | O(n) | O(1) |
| 遍历次数 | 任意次 | 一次 |
| 适用场景 | 数据量小、多次访问 | 大数据流、管道处理 |

### 1.3 装饰器原理

装饰器本质是**接受函数作为参数并返回新函数的高阶函数**。

```python
import time
from functools import wraps

def timer(func):
    """测量函数执行时间"""
    @wraps(func)  # 保留原函数的元信息（__name__, __doc__）
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"{func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper

@timer
def slow_add(a, b):
    """Adds two numbers slowly"""
    time.sleep(0.1)
    return a + b
```

> 🎯 面试要点：`@wraps` 的作用、带参数的装饰器（三层嵌套）、类装饰器（`__call__`）。

### 1.4 GIL 是什么？

GIL（Global Interpreter Lock）是 CPython 解释器中的**全局互斥锁**，确保同一时刻只有一个线程执行 Python 字节码。

| 场景 | 影响 | 解决方案 |
|------|------|---------|
| CPU 密集型 | 多线程退化为串行，性能不升反降 | `multiprocessing`、C 扩展（numpy） |
| I/O 密集型 | 影响小，线程在等待 I/O 时释放 GIL | 多线程或 `asyncio` |

> ⚠️ GIL 不是 Python 语言的限制，是 CPython 实现的限制。Jython、IronPython 没有 GIL。

### 1.5 面向对象三大特性

| 特性 | 说明 | Python 实现方式 |
|------|------|----------------|
| **封装** | 隐藏内部实现，暴露接口 | 单下划线 `_name`（约定）、双下划线 `__name`（名称修饰 name mangling） |
| **继承** | 子类复用父类属性和方法 | `class Dog(Animal):`，支持多继承 MRO（C3 线性化） |
| **多态** | 不同对象响应同一接口 | 鸭子类型（Duck Typing），不依赖继承 |

```python
# 鸭子类型：如果它走起来像鸭子，叫起来像鸭子，那它就是鸭子
class Duck:
    def speak(self): return "Quack"

class Person:
    def speak(self): return "Hello"

def make_sound(entity):
    print(entity.speak())  # 不检查类型，只要求有 speak() 方法

make_sound(Duck())    # Quack
make_sound(Person())  # Hello
```

### 1.6 魔法方法 `__init__` / `__str__` / `__call__`

```python
class Counter:
    def __init__(self, start=0):       # 构造方法，实例化时调用
        self.count = start

    def __str__(self):                  # str() / print() 时调用
        return f"Counter({self.count})"

    def __repr__(self):                 # 调试表示，repr() 时调用
        return f"Counter({self.count})"

    def __call__(self, n=1):            # 实例可调用 c() 等价于 c.__call__()
        self.count += n
        return self.count

c = Counter(5)
c(3)   # 8
print(c)  # Counter(8)
```

### 1.7 Pandas DataFrame vs Series

```python
import pandas as pd

# Series — 一维带标签数组
s = pd.Series([10, 20, 30], index=['a', 'b', 'c'])

# DataFrame — 二维表格数据结构
df = pd.DataFrame({
    'name': ['Alice', 'Bob', 'Charlie'],
    'age': [25, 30, 35],
    'city': ['Beijing', 'Shanghai', 'Shenzhen']
})
```

| 特性 | Series | DataFrame |
|------|--------|-----------|
| 维度 | 一维 | 二维 |
| 结构 | 带索引的一维数组 | 行索引 + 列索引 |
| 访问列 | — | `df['name']` 返回 Series |
| 访问行 | `s.iloc[0]` | `df.iloc[0]` 返回 Series |
| 操作 | 对标 Python 列表 | 对标 Excel / SQL 表 |

### 1.8 Matplotlib 绘图流程

```python
import matplotlib.pyplot as plt

# 标准三步流程
fig, ax = plt.subplots(figsize=(10, 6))  # 1. 创建画布和坐标系

ax.plot(x, y, label='Line', linewidth=2)  # 2. 绘图
ax.bar(categories, values, label='Bar')
ax.set_xlabel('X Label')                  # 3. 美化
ax.set_ylabel('Y Label')
ax.set_title('Title')
ax.legend()
ax.grid(True, alpha=0.3)

plt.tight_layout()
plt.show()
# 或保存：plt.savefig('chart.png', dpi=300)
```

### 1.9 FastAPI 异步特性

```python
from fastapi import FastAPI
import httpx

app = FastAPI()

@app.get("/sync")
def read_root():                # 同步路由，在线程池运行
    return {"message": "Hello"}

@app.get("/async")
async def read_async():         # 异步路由，在主事件循环运行
    async with httpx.AsyncClient() as client:
        resp = await client.get("https://api.example.com/data")
        return resp.json()
```

| 特性 | 说明 |
|------|------|
| **自动 API 文档** | `/docs` (Swagger) 和 `/redoc` (ReDoc) 开箱即用 |
| **异步原生** | 基于 `asyncio` + `uvicorn`，支持 `async def` 路由 |
| **类型校验** | 基于 Pydantic 自动验证请求/响应 |
| **高性能** | 与 Node.js / Go 相近的吞吐量 |

> 💡 面试点：FastAPI 同时支持同步和异步路由。同步路由在**线程池**中运行，异步路由在**事件循环**中运行。

### 1.10 Ollama 本地部署原理

Ollama 是一个**本地大模型运行工具**，将量化后的模型文件（GGUF 格式）通过 llama.cpp 等后端在本地 CPU/GPU 上推理。

```bash
# 拉取并运行模型
ollama pull deepseek-r1:7b
ollama run deepseek-r1:7b

# API 调用
curl http://localhost:11434/api/generate -d '{
  "model": "deepseek-r1:7b",
  "prompt": "你好",
  "stream": false
}'
```

| 组件 | 作用 |
|------|------|
| Model File | 定义模型配置的 Modelfile（类似 Dockerfile） |
| llama.cpp 后端 | CPU 推理优化，支持 4-bit/8-bit 量化 |
| OpenAI 兼容 API | `/v1/chat/completions` 兼容接口 |
| GPU 加速 | 通过 CUDA/Metal 调用 GPU |

### 1.11 Streamlit vs Gradio

| 维度 | Streamlit | Gradio |
|------|-----------|--------|
| 定位 | 数据应用框架 | ML 演示工具 |
| 适用人群 | 数据分析师、Python 开发者 | ML 研究人员 |
| UI 定制 | 有限（组件化布局） | 更灵活（Blocks API） |
| 部署 | Streamlit Cloud 一键部署 | Hugging Face Spaces |
| 典型场景 | 内部 Dashboard、数据探索 | 模型 Demo、HuggingFace 分享 |

```python
# === Streamlit 示例 ===
import streamlit as st
st.title("AI 聊天助手")
prompt = st.text_input("输入问题")
if st.button("发送"):
    st.write(f"你问的是: {prompt}")

# === Gradio 示例 ===
import gradio as gr
def greet(name):
    return f"Hello {name}!"
gr.Interface(fn=greet, inputs="text", outputs="text").launch()
```

### 1.12 会话记忆保存方案

```python
import json
import os

SESSION_DIR = "sessions"

def save_session(session_id: str, messages: list):
    """将会话保存到 JSON 文件"""
    os.makedirs(SESSION_DIR, exist_ok=True)
    path = os.path.join(SESSION_DIR, f"{session_id}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"id": session_id, "messages": messages}, f, ensure_ascii=False, indent=2)

def load_session(session_id: str) -> list:
    """加载历史会话"""
    path = os.path.join(SESSION_DIR, f"{session_id}.json")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f).get("messages", [])
    return []
```

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| 文件存储 (JSON) | 简单直观 | 不适合大规模 | 单机 Demo |
| 数据库 (SQLite/Redis) | 查询高效 | 需要额外依赖 | 生产环境 |
| 内存存储 (dict) | 速度快 | 重启丢失 | 测试开发 |
| 向量数据库 | 支持语义检索 | 复杂度高 | RAG 场景 |

### 1.13 XPath 语法

```python
from lxml import etree

html = "<html><body><div class='content'><p>Hello</p></div></body></html>"
tree = etree.HTML(html)

# 常用 XPath 表达式
tree.xpath("//p")                          # 选择所有 p 标签
tree.xpath("//div[@class='content']")      # 属性过滤
tree.xpath("//div/p/text()")               # 获取文本内容
tree.xpath("//@href")                      # 获取所有 href 属性值
tree.xpath("//div[contains(@class, 'item')]")  # 部分匹配
```

| 表达式 | 含义 |
|--------|------|
| `/` | 从根节点选取 |
| `//` | 任意位置的子孙节点 |
| `.` | 当前节点 |
| `..` | 父节点 |
| `@attr` | 选取属性 |
| `[N]` | 第 N 个元素（从 1 开始） |

### 1.14 HTTP 协议请求响应格式

```python
import requests

# 请求结构
response = requests.get(
    url="https://api.deepseek.com/v1/chat/completions",
    headers={
        "Authorization": "Bearer sk-xxx",
        "Content-Type": "application/json"
    },
    json={
        "model": "deepseek-chat",
        "messages": [{"role": "user", "content": "你好"}],
        "stream": False
    },
    timeout=30
)

# 响应处理
print(response.status_code)   # 200
print(response.json())         # JSON 解析
print(response.text)           # 原始字符串
```

### 1.15 lambda 与匿名函数

```python
# 基本语法：lambda 参数: 表达式
square = lambda x: x ** 2
print(square(5))  # 25

# 常用场景：排序 key
students = [("Alice", 25), ("Bob", 20), ("Charlie", 30)]
students.sort(key=lambda s: s[1])  # 按年龄排序

# 与 map/filter 配合
nums = [1, 2, 3, 4, 5]
squared = list(map(lambda x: x**2, nums))
evens = list(filter(lambda x: x % 2 == 0, nums))
```

### 1.16 异常处理

```python
try:
    result = 10 / 0
except ZeroDivisionError as e:
    print(f"除零错误: {e}")
except (TypeError, ValueError) as e:
    print(f"类型或值错误: {e}")
else:
    print("无异常时执行")
finally:
    print("始终执行（资源清理）")

# 自定义异常
class APIError(Exception):
    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message
        super().__init__(f"[{code}] {message}")

raise APIError(429, "请求过于频繁")
```

### 1.17 模块与包管理

```python
# 导入方式
import math
from datetime import datetime, timedelta
from collections import defaultdict, Counter as CounterAlias

# 自定义模块：my_module.py → import my_module
# 自定义包：my_package/__init__.py → from my_package import sub_module

# 常用标准库模块
import os, sys, json, re, hashlib
import threading, multiprocessing, asyncio
from pathlib import Path
```

| 概念 | 说明 |
|------|------|
| `__init__.py` | 标识目录为 Python 包 |
| `if __name__ == "__main__"` | 模块作为脚本执行时的入口 |
| `__all__` | 控制 `from module import *` 的行为 |
| `sys.path` | 模块搜索路径列表 |

### 1.18 文件操作

```python
# 文本文件
with open("data.txt", "r", encoding="utf-8") as f:
    content = f.read()           # 全部读取
    lines = f.readlines()        # 按行读取列表
    for line in f:               # 逐行迭代（推荐大文件）
        process(line)

# JSON 文件
import json
data = {"name": "Alice", "scores": [90, 85]}
with open("data.json", "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
with open("data.json", "r", encoding="utf-8") as f:
    loaded = json.load(f)
```

### 1.19 Streamlit AI 聊天界面组件

```python
import streamlit as st

st.set_page_config(page_title="AI Chat", layout="wide")
st.title("AI 聊天助手")

# 会话状态管理
if "messages" not in st.session_state:
    st.session_state.messages = []

# 侧边栏
with st.sidebar:
    st.header("设置")
    model = st.selectbox("选择模型", ["deepseek-chat", "gpt-3.5-turbo"])
    temperature = st.slider("温度", 0.0, 2.0, 0.7)
    if st.button("清空对话"):
        st.session_state.messages = []

# 显示历史消息
for msg in st.session_state.messages:
    with st.chat_message(msg["role"]):
        st.markdown(msg["content"])

# 输入框
if prompt := st.chat_input("输入你的问题"):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)
```

### 1.20 Python 类型注解

```python
from typing import List, Dict, Optional, Union, Callable

# 基础类型注解
name: str = "Alice"
count: int = 10
price: float = 99.5
is_valid: bool = True

# 复合类型
scores: List[int] = [90, 85, 88]
mapping: Dict[str, int] = {"a": 1, "b": 2}
maybe: Optional[str] = None  # 等价于 Union[str, None]

# 函数注解
def add(a: int, b: int) -> int:
    return a + b

def process(callback: Callable[[int, str], bool]) -> None:
    pass
```

---

## 二、深度原理剖析（10-15题）

### 2.1 Python 内存管理：引用计数与垃圾回收

**引用计数为核心：** 每个对象维护 `ob_refcnt`，当引用计数降为 0 时立即回收。

```python
import sys

a = []
print(sys.getrefcount(a))  # 2（a 和 getrefcount 的临时引用）
b = a
print(sys.getrefcount(a))  # 3
del b
print(sys.getrefcount(a))  # 2
```

**循环引用问题：** 引用计数无法处理对象间互相引用。

```python
class Node:
    def __init__(self):
        self.ref = None

a = Node()
b = Node()
a.ref = b  # 循环引用：a → b → a
b.ref = a
del a, b   # 引用计数不为 0，无法回收
```

**垃圾回收（GC）：** CPython 使用**分代回收**算法，分三代（0/1/2），新对象在 0 代，经过一次扫描存活则升代。

| 机制 | 触发条件 | 处理对象 |
|------|---------|---------|
| 引用计数 | 引用为 0 立即回收 | 普通对象（立即） |
| 标记-清除 | 分代阈值触发 | 循环引用对象 |
| 分代回收 | 每代计数器超过阈值 | 同代所有容器对象 |

> 🎯 面试亮点：`gc.get_threshold()` 返回 `(700, 10, 10)` 表示第 0 代每分配 700 个对象触发一次 GC。

### 2.2 大模型 API 调用：HTTP 流式 / SSE / Tokenization

**流式输出（SSE：Server-Sent Events）：**

```python
import httpx
import json

def stream_chat(prompt: str):
    """使用 httpx 流式调用大模型 API（SSE 协议）"""
    url = "https://api.deepseek.com/v1/chat/completions"
    headers = {"Authorization": "Bearer sk-xxx", "Content-Type": "application/json"}
    data = {
        "model": "deepseek-chat",
        "messages": [{"role": "user", "content": prompt}],
        "stream": True  # 启用流式
    }

    with httpx.Client() as client:
        with client.stream("POST", url, headers=headers, json=data) as response:
            for line in response.iter_lines():
                if line.startswith("data: "):
                    chunk = line[6:]  # 去掉 "data: " 前缀
                    if chunk == "[DONE]":
                        break
                    content = json.loads(chunk)["choices"][0]["delta"].get("content", "")
                    if content:
                        yield content

# 使用
for token in stream_chat("讲个故事"):
    print(token, end="", flush=True)
```

**Tokenization（分词）：**

| 概念 | 说明 |
|------|------|
| Token | 模型处理的最小单元，中文约 1.5-2 字/token |
| Tokenizer | 文本 ↔ Token ID 的转换器（BPE / SentencePiece） |
| 上下文窗口 | 模型最大 Token 数（如 8K / 32K / 128K） |
| Token 计数 | 通过 `tiktoken` 库提前计算，确保不超限 |

```python
import tiktoken

enc = tiktoken.get_encoding("cl100k_base")
tokens = enc.encode("你好，世界！")
print(len(tokens))     # 5
print(enc.decode(tokens))  # 你好，世界！
```

### 2.3 Pandas 性能优化：Vectorization vs apply vs Loop

```python
import pandas as pd
import numpy as np
import time

df = pd.DataFrame({"a": range(1_000_000), "b": range(1_000_000, 2_000_000)})

# 方法 1：循环（最慢 ❌）
def with_loop():
    result = []
    for i in range(len(df)):
        result.append(df.iloc[i]["a"] + df.iloc[i]["b"])
    return result  # ~500ms

# 方法 2：apply（较慢）
def with_apply():
    return df.apply(lambda row: row["a"] + row["b"], axis=1)  # ~200ms

# 方法 3：vectorization（最快 ✅）
def with_vector():
    return df["a"] + df["b"]  # ~1ms — 利用 numpy 底层 C 实现
```

| 方法 | 100 万行耗时 | 内存占用 | 推荐场景 |
|------|-------------|---------|---------|
| Loop | ~500ms | 低（逐行） | 调试/少量数据 |
| apply | ~200ms | 中 | 复杂行级逻辑 |
| Vectorization | ~1ms | 高（全列） | 数值运算（首选） |
| NumPy 向量化 | ~0.5ms | 中 | 纯数学计算 |

```python
# 更优实践：利用 NumPy 表达式 + pandas 内置方法
df["c"] = np.where(df["a"] > 500_000, df["a"] * 2, df["b"] / 2)
df["d"] = df["a"].rank(pct=True)  # 百分位排名
df["e"] = df.groupby("a")["b"].transform("mean")  # 分组均值
```

### 2.4 FastAPI 异步：asyncio / uvicorn / 异步数据库

```python
from fastapi import FastAPI
from contextlib import asynccontextmanager
import asyncpg   # 异步 PostgreSQL 驱动
import databases  # 异步数据库工具包

DATABASE_URL = "postgresql+asyncpg://user:pass@localhost/db"
database = databases.Database(DATABASE_URL)

@asynccontextmanager
async def lifespan(app: FastAPI):
    await database.connect()   # 启动时连接
    yield
    await database.disconnect()  # 关闭时断开

app = FastAPI(lifespan=lifespan)

@app.get("/items/{item_id}")
async def get_item(item_id: int):
    query = "SELECT * FROM items WHERE id = :id"
    return await database.fetch_one(query, {"id": item_id})
```

**UVicorn 工作模式：**

| 模式 | 命令 | 说明 |
|------|------|------|
| 单进程 | `uvicorn main:app` | 单 Worker，开发用 |
| 多进程 | `uvicorn main:app --workers 4` | 多 Worker，生产部署 |
| 热重载 | `uvicorn main:app --reload` | 开发时自动重启 |

> ⚠️ FastAPI 中 `async def` 路由自动在事件循环中运行；`def` 路由在线程池中运行。大量 I/O 操作应使用 `async def`。

### 2.5 爬虫反爬策略

```python
import requests
import time
import random
from fake_useragent import UserAgent

ua = UserAgent()

def fetch_with_anti_spider(url: str) -> str:
    """反爬虫请求模板"""
    headers = {
        "User-Agent": ua.random,                # 随机 UA
        "Referer": "https://www.google.com/",    # 来源伪装
        "Accept-Language": "zh-CN,zh;q=0.9",
        "Connection": "keep-alive"
    }
    # 随机延迟
    time.sleep(random.uniform(1, 3))

    session = requests.Session()
    session.cookies.update({"session_id": "xxx"})  # Cookie 维持

    proxies = {
        "http": "http://127.0.0.1:7890",
        "https": "http://127.0.0.1:7890"
    }
    # 代理 + 超时 + 重试
    resp = session.get(
        url,
        headers=headers,
        proxies=proxies,
        timeout=10
    )
    resp.raise_for_status()
    return resp.text
```

| 反爬手段 | 应对策略 |
|----------|---------|
| IP 限流 | 代理池 + 随机延迟 |
| UA 检测 | 随机 User-Agent 池 |
| Cookie 校验 | Session 保持 + Cookie 自动管理 |
| 验证码 | OCR / 打码平台 / Selenium（复杂） |
| JS 渲染 | Playwright / Selenium 渲染 |

### 2.6 装饰器进阶：带参数与类装饰器

```python
from functools import wraps
import logging

# 带参数的装饰器（三层嵌套）
def retry(max_retries: int = 3, delay: float = 1.0):
    """失败重试装饰器"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    if attempt == max_retries - 1:
                        raise
                    logging.warning(f"Retry {attempt+1}/{max_retries}: {e}")
                    time.sleep(delay)
            return None
        return wrapper
    return decorator

@retry(max_retries=5, delay=2.0)
def call_api():
    return requests.get("https://api.example.com")

# 类装饰器（通过 __call__）
class Singleton:
    """单例模式装饰器"""
    def __init__(self, cls):
        self.cls = cls
        self.instance = None

    def __call__(self, *args, **kwargs):
        if self.instance is None:
            self.instance = self.cls(*args, **kwargs)
        return self.instance

@Singleton
class Database:
    def __init__(self):
        self.conn = "Connected"
```

### 2.7 面向对象高级：MRO 与 super()

```python
class A:
    def method(self): return "A"

class B(A):
    def method(self): return "B" + super().method()

class C(A):
    def method(self): return "C" + super().method()

class D(B, C):
    def method(self): return "D" + super().method()

print(D().method())  # DBCA
print(D.__mro__)     # D → B → C → A → object（C3 线性化）
```

**MRO（Method Resolution Order）：** Python 使用 C3 线性化算法确定方法搜索路径，保证：子类优先、单调性、拓扑排序。

### 2.8 提示词工程（Prompt Engineering）

```python
def build_prompt(user_input: str, context: list = None) -> list:
    """构建结构化的 Prompt 消息序列"""
    system_prompt = """你是一个专业的 AI 助手，请遵循以下规则：
1. 回答简洁准确
2. 不确定时说明"我无法确认"
3. 使用中文回答
"""

    messages = [{"role": "system", "content": system_prompt}]

    if context:
        messages.extend(context)  # 历史会话上下文

    messages.append({"role": "user", "content": user_input})
    return messages

# Few-shot 示例（少样本学习）
few_shot_prompt = f"""将中文翻译为英文：
中文：今天天气真好 → 英文：The weather is nice today.
中文：我要学习Python → 英文：I want to learn Python.
中文：{user_input} → 英文："""
```

### 2.9 Pandas 数据合并与分组

```python
import pandas as pd

# 数据合并
df1 = pd.DataFrame({"id": [1, 2, 3], "name": ["A", "B", "C"]})
df2 = pd.DataFrame({"id": [1, 2, 4], "score": [90, 85, 95]})

# Merge（类似 SQL JOIN）
inner = pd.merge(df1, df2, on="id", how="inner")    # 内连接
left = pd.merge(df1, df2, on="id", how="left")       # 左连接

# GroupBy 分组聚合
df = pd.DataFrame({
    "category": ["food", "food", "elec", "elec", "food"],
    "sales": [100, 200, 300, 400, 500],
    "region": ["east", "west", "east", "west", "east"]
})

result = df.groupby("category").agg({
    "sales": ["sum", "mean", "count"],
    "region": "nunique"
})
```

| 合并方式 | 说明 |
|---------|------|
| `concat` | 按行/列拼接 |
| `merge` | 类似 SQL JOIN |
| `join` | 索引连接 |
| `combine_first` | 填充缺失值 |

### 2.10 FastAPI 依赖注入

```python
from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

app = FastAPI()
security = HTTPBearer()

# 依赖：数据库会话
def get_db():
    db = DatabaseSession()
    try:
        yield db
    finally:
        db.close()

# 依赖：当前用户
def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    user = verify_token(token)
    if user is None:
        raise HTTPException(status_code=401, detail="无效 token")
    return user

# 使用多重依赖
@app.get("/profile")
async def get_profile(
    user = Depends(get_current_user),
    db = Depends(get_db)
):
    return db.query_user(user.id)
```

---

## 三、实战场景题（8-12题）

### 3.1 Streamlit AI 聊天界面（含会话管理）

```python
import streamlit as st
import json
import os
from datetime import datetime

# === 会话管理 ===
SESSION_DIR = "chat_sessions"

def list_sessions() -> list:
    """列出所有历史会话"""
    os.makedirs(SESSION_DIR, exist_ok=True)
    sessions = []
    for fname in os.listdir(SESSION_DIR):
        if fname.endswith(".json"):
            with open(os.path.join(SESSION_DIR, fname), "r", encoding="utf-8") as f:
                sessions.append(json.load(f))
    return sorted(sessions, key=lambda x: x["updated"], reverse=True)

def save_session(session_id: str, messages: list):
    path = os.path.join(SESSION_DIR, f"{session_id}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump({
            "id": session_id,
            "updated": datetime.now().isoformat(),
            "messages": messages
        }, f, ensure_ascii=False, indent=2)

# === 页面 ===
st.set_page_config(page_title="AI Chat", layout="wide")
st.title("AI 智能对话")

# 侧边栏：会话列表
with st.sidebar:
    st.header("历史会话")
    sessions = list_sessions()
    for sess in sessions:
        if st.button(f"加载 {sess['id'][:8]}...", key=sess["id"]):
            st.session_state.session_id = sess["id"]
            st.session_state.messages = sess["messages"]

    if st.button("新建会话"):
        st.session_state.session_id = datetime.now().strftime("%Y%m%d%H%M%S")
        st.session_state.messages = []
        save_session(st.session_state.session_id, st.session_state.messages)

# 初始状态
if "messages" not in st.session_state:
    st.session_state.session_id = datetime.now().strftime("%Y%m%d%H%M%S")
    st.session_state.messages = []

# 显示消息
for msg in st.session_state.messages:
    with st.chat_message(msg["role"]):
        st.markdown(msg["content"])

# 输入
if prompt := st.chat_input("输入消息..."):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)
    # 模拟 AI 回复（实际替换为 API 调用）
    ai_reply = f"你问的是: {prompt}（此处接入大模型 API）"
    st.session_state.messages.append({"role": "assistant", "content": ai_reply})
    with st.chat_message("assistant"):
        st.markdown(ai_reply)
    save_session(st.session_state.session_id, st.session_state.messages)
```

### 3.2 DeepSeek API 调用（流式输出）

```python
import requests
import json
from typing import Generator

class DeepSeekClient:
    """DeepSeek API 客户端封装"""

    BASE_URL = "https://api.deepseek.com/v1"

    def __init__(self, api_key: str):
        self.headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }

    def chat(self, messages: list, model: str = "deepseek-chat",
             stream: bool = False, temperature: float = 0.7) -> dict | Generator:
        """调用对话 API"""
        data = {
            "model": model,
            "messages": messages,
            "stream": stream,
            "temperature": temperature
        }
        resp = requests.post(
            f"{self.BASE_URL}/chat/completions",
            headers=self.headers,
            json=data,
            timeout=60
        )
        resp.raise_for_status()
        return resp.json()

    def stream_chat(self, messages: list, model: str = "deepseek-chat",
                    temperature: float = 0.7) -> Generator[str, None, None]:
        """流式对话"""
        data = {
            "model": model,
            "messages": messages,
            "stream": True,
            "temperature": temperature
        }
        with requests.Session() as session:
            with session.post(
                f"{self.BASE_URL}/chat/completions",
                headers=self.headers,
                json=data,
                stream=True,
                timeout=120
            ) as resp:
                for line in resp.iter_lines():
                    if line:
                        line = line.decode("utf-8")
                        if line.startswith("data: "):
                            chunk = line[6:]
                            if chunk == "[DONE]":
                                break
                            data = json.loads(chunk)
                            content = data["choices"][0]["delta"].get("content", "")
                            if content:
                                yield content

# 使用示例
client = DeepSeekClient(api_key="sk-xxx")
messages = [{"role": "system", "content": "你是一个 Python 专家"},
            {"role": "user", "content": "解释 Python 装饰器"}]
for token in client.stream_chat(messages):
    print(token, end="", flush=True)
```

### 3.3 Pandas 数据分析实战（电影数据）

```python
import pandas as pd
import numpy as np

# === 加载数据 ===
df = pd.read_csv("movies.csv")
print(f"数据集维度: {df.shape}")
print(f"列名: {df.columns.tolist()}")

# === 数据清洗 ===
print(f"缺失值统计:\n{df.isnull().sum()}")

# 处理缺失值
df["rating"].fillna(df["rating"].median(), inplace=True)
df["votes"].fillna(0, inplace=True)
df.dropna(subset=["title", "year"], inplace=True)

# 去除重复
df.drop_duplicates(subset=["title", "year"], inplace=True)

# === 数据分析 ===
# 1. 评分最高的 10 部电影
top_rated = df.nlargest(10, "rating")[["title", "rating", "year"]]

# 2. 每年电影数量趋势
yearly_count = df["year"].value_counts().sort_index()

# 3. 各类型电影平均评分
genre_avg = df.explode("genres").groupby("genres")["rating"].agg(["mean", "count"])
genre_avg = genre_avg[genre_avg["count"] >= 10].sort_values("mean", ascending=False)

# 4. 评分和投票数的相关性
correlation = df["rating"].corr(df["votes"])  # 正相关则高评分电影获得更多投票

# 5. 年代区间分析
df["decade"] = (df["year"] // 10) * 10
decade_stats = df.groupby("decade").agg(
    avg_rating=("rating", "mean"),
    count=("title", "count"),
    avg_votes=("votes", "mean")
)

print(f"\n=== 分析结果 ===")
print(f"评分和投票相关系数: {correlation:.3f}")
print(f"平均评分最高的年代: {decade_stats['avg_rating'].idxmax()}")
```

### 3.4 Matplotlib 数据可视化

```python
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

plt.rcParams["font.sans-serif"] = ["SimHei"]  # 中文显示
plt.rcParams["axes.unicode_minus"] = False

# 示例数据
df = pd.read_csv("movies.csv")
decade_stats = (df["year"] // 10 * 10).value_counts().sort_index()

# === 1. 柱状图：各年代电影数量 ===
fig, axes = plt.subplots(2, 2, figsize=(14, 10))

axes[0, 0].bar(decade_stats.index, decade_stats.values, color="#4C72B0", width=8)
axes[0, 0].set_title("各年代电影数量")
axes[0, 0].set_xlabel("年代")
axes[0, 0].set_ylabel("数量")
for i, v in enumerate(decade_stats.values):
    axes[0, 0].text(decade_stats.index[i], v + 5, str(v), ha="center")

# === 2. 饼图：前 5 大类型占比 ===
genre_counts = df.explode("genres")["genres"].value_counts().head(5)
axes[0, 1].pie(genre_counts.values, labels=genre_counts.index,
               autopct="%1.1f%%", startangle=90)
axes[0, 1].set_title("前 5 大电影类型占比")

# === 3. 散点图：评分 vs 投票数 ===
axes[1, 0].scatter(df["votes"], df["rating"], alpha=0.3, s=10)
axes[1, 0].set_title("评分 vs 投票数")
axes[1, 0].set_xlabel("投票数")
axes[1, 0].set_ylabel("评分")
axes[1, 0].set_xscale("log")  # 对数刻度

# === 4. 直方图：评分分布 ===
axes[1, 1].hist(df["rating"], bins=20, color="#55A868", edgecolor="white")
axes[1, 1].set_title("评分分布")
axes[1, 1].set_xlabel("评分")
axes[1, 1].set_ylabel("频数")

plt.tight_layout()
plt.savefig("movie_analysis.png", dpi=200, bbox_inches="tight")
plt.show()
```

### 3.5 爬虫获取网页数据

```python
import requests
from bs4 import BeautifulSoup
import csv
import re
from urllib.parse import urljoin
import time
import random

class WebScraper:
    """通用网页爬虫"""

    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        })

    def fetch_page(self, path: str) -> BeautifulSoup | None:
        """获取页面并解析为 BeautifulSoup 对象"""
        url = urljoin(self.base_url, path)
        try:
            resp = self.session.get(url, timeout=10)
            resp.raise_for_status()
            resp.encoding = resp.apparent_encoding  # 自动检测编码
            return BeautifulSoup(resp.text, "html.parser")
        except requests.RequestException as e:
            print(f"请求失败: {url}, 错误: {e}")
            return None

    def extract_items(self, soup: BeautifulSoup) -> list[dict]:
        """提取数据 — 根据目标页面自定义选择器"""
        items = []
        for div in soup.select("div.item"):
            item = {
                "title": div.select_one("h2 a").get_text(strip=True),
                "link": div.select_one("h2 a")["href"],
                "price": div.select_one("span.price").get_text(strip=True) if div.select_one("span.price") else "",
                "rating": div.select_one("span.rating")["class"][-1] if div.select_one("span.rating") else "",
            }
            items.append(item)
        return items

    def save_to_csv(self, items: list[dict], filename: str):
        """保存为 CSV 文件"""
        if not items:
            print("无数据可保存")
            return
        with open(filename, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.DictWriter(f, fieldnames=items[0].keys())
            writer.writeheader()
            writer.writerows(items)
        print(f"已保存 {len(items)} 条记录到 {filename}")

    def crawl_pages(self, start: int, end: int, filename: str):
        """爬取多页"""
        all_items = []
        for page in range(start, end + 1):
            print(f"正在爬取第 {page} 页...")
            soup = self.fetch_page(f"/list?page={page}")
            if soup:
                items = self.extract_items(soup)
                all_items.extend(items)
                print(f"第 {page} 页: 获取 {len(items)} 条数据")
            time.sleep(random.uniform(1, 3))  # 礼貌性延迟
        self.save_to_csv(all_items, filename)

# 使用
scraper = WebScraper("https://example.com")
scraper.crawl_pages(1, 5, "output.csv")
```

### 3.6 FastAPI AI 汉字谜盒项目

```python
from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
import logging
import json
from typing import Optional

# === AI 汉字谜盒 — FastAPI 应用 ===
# 功能：用户输入谜面，AI 猜汉字

app = FastAPI(title="AI 汉字谜盒")

# 日志配置
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("hanzi_riddle")

# === 数据模型 ===
class RiddleRequest(BaseModel):
    riddle: str                         # 谜面
    hint: Optional[str] = None          # 提示（可选）
    session_id: Optional[str] = None    # 会话 ID

class RiddleResponse(BaseModel):
    answer: str                         # 谜底
    explanation: str                    # 解释
    confidence: float                   # 置信度

# === 会话管理（内存）===
sessions: dict = {}

def get_or_create_session(session_id: str = None) -> str:
    if session_id and session_id in sessions:
        return session_id
    import uuid
    new_id = str(uuid.uuid4())
    sessions[new_id] = {"history": []}
    return new_id

# === 路由 ===
@app.get("/")
def root():
    return {"message": "欢迎来到 AI 汉字谜盒！", "docs": "/docs"}

@app.post("/guess", response_model=RiddleResponse)
async def guess_hanzi(request: RiddleRequest):
    """AI 猜汉字谜题"""
    session_id = get_or_create_session(request.session_id)
    sessions[session_id]["history"].append({"riddle": request.riddle})

    # 实际接入大模型 API
    # prompt = f"猜汉字谜题：{request.riddle}，提示：{request.hint}"
    # response = await call_llm(prompt)

    # 模拟返回
    answer, explanation = solve_riddle(request.riddle, request.hint)
    return RiddleResponse(
        answer=answer,
        explanation=explanation,
        confidence=0.85
    )

@app.get("/sessions/{session_id}")
def get_session_history(session_id: str):
    """获取会话历史"""
    if session_id not in sessions:
        raise HTTPException(status_code=404, detail="会话不存在")
    return {"session_id": session_id, "history": sessions[session_id]["history"]}

@app.delete("/sessions/{session_id}")
def clear_session(session_id: str):
    """清除会话"""
    if session_id not in sessions:
        raise HTTPException(status_code=404, detail="会话不存在")
    del sessions[session_id]
    return {"message": "会话已清除"}

# === 谜题逻辑（演示）===
def solve_riddle(riddle: str, hint: str = None) -> tuple[str, str]:
    """简单谜底匹配（实际场景调用大模型）"""
    riddles = {
        "一口咬掉牛尾巴": ("告", "牛字下面加口，去掉尾巴"),
        "山上还有山": ("出", "两个山叠加"),
        "人在云上走": ("会", "人+云=会"),
    }
    if riddle in riddles:
        return riddles[riddle]
    return ("?", "暂时无法解答（接入大模型后可解答任意谜题）")

# === 启动 ===
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

### 3.7 提示词工程模板

```python
# === 提示词模板集合 ===

# 1. 角色扮演模板
ROLE_TEMPLATE = """# 角色
你是{role_name}，{role_description}

# 能力
- {skill_1}
- {skill_2}

# 回答风格
{style}

# 任务
{task}
"""

# 2. 思维链（Chain-of-Thought）模板
COT_TEMPLATE = """问题: {question}

请一步步思考：
1. 首先分析问题的关键要素
2. 回忆相关知识和原理
3. 逐步推导解决方案
4. 验证答案的合理性

最终答案：
"""

# 3. 结构化输出模板
STRUCTURED_TEMPLATE = """请从以下文本中提取信息，以 JSON 格式返回：
```json
{{
  "name": "人名",
  "age": "年龄（数字）",
  "skills": ["技能1", "技能2"],
  "summary": "一句话总结"
}}
```

文本：{text}
"""

# 4. Few-shot 示例模板
FEW_SHOT_TEMPLATE = """请完成以下任务。

示例 1:
输入：{example_input_1}
输出：{example_output_1}

示例 2:
输入：{example_input_2}
输出：{example_output_2}

现在请处理：
输入：{input}
输出："""

# 5. AI 汉字谜盒专用 Prompt
RIDDLE_PROMPT = """你是一个汉字谜语大师。请根据以下谜面猜汉字：

谜面：{riddle}
提示：{hint}

请按以下格式回答：
答案：[一个汉字]
解释：[为什么是这个字，拆解逻辑]
置信度：[0-1 的数值]
"""
```

### 3.8 数据清洗与预处理 Pipeline

```python
import pandas as pd
import numpy as np
from datetime import datetime

class DataCleaner:
    """通用数据清洗 Pipeline"""

    def __init__(self, df: pd.DataFrame):
        self.df = df.copy()
        self.log = []

    def remove_duplicates(self, subset: list = None) -> "DataCleaner":
        """去除重复行"""
        before = len(self.df)
        self.df.drop_duplicates(subset=subset, inplace=True)
        self.log.append(f"去重: {before} → {len(self.df)} 行")
        return self

    def fill_missing(self, strategy: dict) -> "DataCleaner":
        """填充缺失值
        strategy = {"col1": "mean", "col2": "median", "col3": "mode", "col4": 0}
        """
        before = self.df.isnull().sum().sum()
        for col, method in strategy.items():
            if col not in self.df.columns:
                continue
            if method == "mean":
                self.df[col].fillna(self.df[col].mean(), inplace=True)
            elif method == "median":
                self.df[col].fillna(self.df[col].median(), inplace=True)
            elif method == "mode":
                self.df[col].fillna(self.df[col].mode()[0], inplace=True)
            elif method == "ffill":
                self.df[col].fillna(method="ffill", inplace=True)
            else:
                self.df[col].fillna(method, inplace=True)
        after = self.df.isnull().sum().sum()
        self.log.append(f"缺失值填充: {before} → {after}")
        return self

    def filter_outliers(self, col: str, n_std: float = 3) -> "DataCleaner":
        """过滤异常值（3 倍标准差）"""
        before = len(self.df)
        mean, std = self.df[col].mean(), self.df[col].std()
        self.df = self.df[(self.df[col] >= mean - n_std * std) &
                          (self.df[col] <= mean + n_std * std)]
        self.log.append(f"异常值过滤 ({col}): {before} → {len(self.df)} 行")
        return self

    def normalize_date(self, col: str, fmt: str = None) -> "DataCleaner":
        """统一日期格式"""
        if fmt:
            self.df[col] = pd.to_datetime(self.df[col], format=fmt)
        else:
            self.df[col] = pd.to_datetime(self.df[col], errors="coerce")
        self.log.append(f"日期格式化: {col}")
        return self

    def run(self) -> pd.DataFrame:
        """执行并返回清洗后数据"""
        print("=== 数据清洗报告 ===")
        for msg in self.log:
            print(f"  - {msg}")
        print(f"最终维度: {self.df.shape}")
        return self.df

# 使用示例
cleaner = DataCleaner(pd.read_csv("raw_data.csv"))
df_clean = (cleaner
    .remove_duplicates(subset=["id"])
    .fill_missing({"age": "median", "name": "ffill", "score": "mean"})
    .filter_outliers("score", n_std=3)
    .normalize_date("created_at")
    .run())
```

---

## 四、手写代码题（5-8题）

### 4.1 手写装饰器

```python
from functools import wraps
import time

def log_execution(func):
    """日志装饰器：记录函数名、参数和耗时"""
    @wraps(func)
    def wrapper(*args, **kwargs):
        arg_str = ', '.join([repr(a) for a in args] +
                           [f"{k}={v!r}" for k, v in kwargs.items()])
        print(f"调用: {func.__name__}({arg_str})")
        start = time.perf_counter()
        try:
            result = func(*args, **kwargs)
            elapsed = time.perf_counter() - start
            print(f"完成: {func.__name__} 耗时 {elapsed:.4f}s → {result!r}")
            return result
        except Exception as e:
            elapsed = time.perf_counter() - start
            print(f"错误: {func.__name__} 耗时 {elapsed:.4f}s → {e}")
            raise
    return wrapper

@log_execution
def divide(a: float, b: float) -> float:
    return a / b

# divide(10, 2)  → 调用: divide(10, 2)  完成: divide 耗时 0.0001s → 5.0
# divide(10, 0)  → 调用: divide(10, 0)  错误: divide 耗时 0.0001s → division by zero
```

### 4.2 手写列表推导式优化

```python
# 场景 1：筛选 + 转换 — 一行代替 for 循环
nums = range(100)

# 原始 for 循环
result = []
for n in nums:
    if n % 2 == 0:
        result.append(n ** 2)

# 列表推导式（更优）
result = [n ** 2 for n in nums if n % 2 == 0]

# 场景 2：嵌套循环扁平化
matrix = [[1, 2], [3, 4], [5, 6]]
flattened = [x for row in matrix for x in row]  # [1, 2, 3, 4, 5, 6]

# 场景 3：字典推导式
words = ["hello", "world", "python"]
word_length = {w: len(w) for w in words}  # {"hello": 5, "world": 5, "python": 6}

# 场景 4：集合推导式
unique_lens = {len(w) for w in words}  # {5, 6}

# 性能对比
import timeit
setup = "nums = range(1000)"
loop = """
result = []
for n in nums:
    if n % 2 == 0:
        result.append(n ** 2)
"""
comp = "[n ** 2 for n in nums if n % 2 == 0]"
print(f"Loop: {timeit.timeit(loop, setup)}")   # ~70μs
print(f"Comp: {timeit.timeit(comp, setup)}")    # ~45μs — 快约 35%
```

### 4.3 手写 Pandas 数据清洗

```python
import pandas as pd
import numpy as np

def clean_sales_data(df: pd.DataFrame) -> pd.DataFrame:
    """完整的数据清洗函数"""
    df = df.copy()

    # 1. 删除完全重复的行
    df.drop_duplicates(inplace=True)

    # 2. 处理缺失值
    #   - 数值列用中位数填充
    numeric_cols = df.select_dtypes(include=[np.number]).columns
    for col in numeric_cols:
        df[col].fillna(df[col].median(), inplace=True)

    #   - 类别列用众数填充
    cat_cols = df.select_dtypes(include=["object"]).columns
    for col in cat_cols:
        if df[col].isnull().any():
            df[col].fillna(df[col].mode()[0], inplace=True)

    # 3. 过滤异常值（Z-score 方法）
    for col in numeric_cols:
        z_scores = np.abs((df[col] - df[col].mean()) / df[col].std())
        df = df[z_scores < 3]

    # 4. 标准化日期格式
    if "date" in df.columns:
        df["date"] = pd.to_datetime(df["date"], errors="coerce")

    # 5. 去除前后空格
    for col in cat_cols:
        if df[col].dtype == "object":
            df[col] = df[col].str.strip()

    # 6. 重置索引
    df.reset_index(drop=True, inplace=True)

    return df

# 使用
df_raw = pd.DataFrame({
    "name": ["Alice", "Bob", "Charlie", "Alice", None],
    "age": [25, 30, 35, 25, 999],  # 999 是异常值
    "salary": [10000, None, 15000, 10000, 20000],
    "date": ["2024-01-01", "2024-02-15", "2024/03/20", "2024-01-01", "2024-05-01"]
})
df_clean = clean_sales_data(df_raw)
print(df_clean)
```

### 4.4 手写爬虫（requests + BeautifulSoup）

```python
import requests
from bs4 import BeautifulSoup
import csv

def scrape_douban_top250() -> list[dict]:
    """爬取豆瓣电影 Top250"""
    url = "https://movie.douban.com/top250"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    movies = []

    for page in range(10):  # 250 部，每页 25 部
        params = {"start": page * 25, "filter": ""}
        resp = requests.get(url, headers=headers, params=params, timeout=10)
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "html.parser")

        for item in soup.select("div.item"):
            title = item.select_one("span.title").get_text(strip=True)
            rating = item.select_one("span.rating_num").get_text(strip=True)
            # 提取评分人数
            star_elem = item.select_one("div.star")
            votes = star_elem.contents[-1].get_text(strip=True) if star_elem else ""
            votes = votes.replace("人评价", "").strip()

            movies.append({
                "title": title,
                "rating": float(rating),
                "votes": int(votes) if votes.isdigit() else 0,
                "url": item.select_one("a")["href"]
            })

    # 保存
    with open("douban_top250.csv", "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=["title", "rating", "votes", "url"])
        writer.writeheader()
        writer.writerows(movies)

    return movies
```

### 4.5 手写 FastAPI 路由

```python
from fastapi import FastAPI, HTTPException, Query, Path
from pydantic import BaseModel
from typing import List, Optional
from datetime import date

app = FastAPI(title="Task Manager API")

# === 数据模型 ===
class Task(BaseModel):
    id: int
    title: str
    completed: bool = False
    priority: int = 0  # 0=low, 1=medium, 2=high
    due_date: Optional[date] = None

class TaskCreate(BaseModel):
    title: str
    priority: int = 0
    due_date: Optional[date] = None

# === 内存数据库 ===
tasks_db: dict[int, Task] = {}
next_id = 1

# === CRUD 路由 ===

@app.post("/tasks", status_code=201)
def create_task(task: TaskCreate):
    """创建任务"""
    global next_id
    new_task = Task(id=next_id, **task.model_dump())
    tasks_db[next_id] = new_task
    next_id += 1
    return new_task

@app.get("/tasks", response_model=List[Task])
def list_tasks(
    completed: Optional[bool] = Query(None, description="筛选完成状态"),
    priority: Optional[int] = Query(None, ge=0, le=2)
):
    """列出任务（支持筛选）"""
    tasks = list(tasks_db.values())
    if completed is not None:
        tasks = [t for t in tasks if t.completed == completed]
    if priority is not None:
        tasks = [t for t in tasks if t.priority == priority]
    return tasks

@app.get("/tasks/{task_id}", response_model=Task)
def get_task(task_id: int = Path(ge=1)):
    """获取单个任务"""
    task = tasks_db.get(task_id)
    if not task:
        raise HTTPException(status_code=404, detail="任务不存在")
    return task

@app.put("/tasks/{task_id}", response_model=Task)
def update_task(task_id: int, update: TaskCreate):
    """更新任务"""
    if task_id not in tasks_db:
        raise HTTPException(status_code=404, detail="任务不存在")
    updated = Task(id=task_id, **update.model_dump())
    tasks_db[task_id] = updated
    return updated

@app.delete("/tasks/{task_id}")
def delete_task(task_id: int):
    """删除任务"""
    if task_id not in tasks_db:
        raise HTTPException(status_code=404, detail="任务不存在")
    del tasks_db[task_id]
    return {"message": "删除成功"}

# === 启动 ===
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

### 4.6 手写生成器实现斐波那契

```python
def fibonacci(n: int):
    """生成器实现斐波那契数列（内存高效）"""
    a, b = 0, 1
    count = 0
    while count < n:
        yield a
        a, b = b, a + b
        count += 1

def fibonacci_memo(n: int, memo: dict = None) -> int:
    """递归 + 记忆化（O(n) 时间）"""
    if memo is None:
        memo = {}
    if n in memo:
        return memo[n]
    if n <= 1:
        return n
    memo[n] = fibonacci_memo(n-1, memo) + fibonacci_memo(n-2, memo)
    return memo[n]

# 使用
for num in fibonacci(10):
    print(num, end=" ")  # 0 1 1 2 3 5 8 13 21 34
```

### 4.7 手写数据预处理 Pipeline（类 sklearn 风格）

```python
from sklearn.base import BaseEstimator, TransformerMixin
import pandas as pd
import numpy as np

class DataFrameCleaner(BaseEstimator, TransformerMixin):
    """自定义 DataFrame 清洗转换器"""

    def __init__(self, numeric_strategy="median", cat_strategy="mode", drop_threshold=0.5):
        self.numeric_strategy = numeric_strategy
        self.cat_strategy = cat_strategy
        self.drop_threshold = drop_threshold
        self.fill_values_ = {}

    def fit(self, X: pd.DataFrame, y=None):
        """学习填充值"""
        # 数值列
        numeric_cols = X.select_dtypes(include=[np.number]).columns
        for col in numeric_cols:
            if self.numeric_strategy == "median":
                self.fill_values_[col] = X[col].median()
            elif self.numeric_strategy == "mean":
                self.fill_values_[col] = X[col].mean()

        # 类别列
        cat_cols = X.select_dtypes(include=["object"]).columns
        for col in cat_cols:
            if self.cat_strategy == "mode":
                self.fill_values_[col] = X[col].mode()[0]
        return self

    def transform(self, X: pd.DataFrame) -> pd.DataFrame:
        """执行转换"""
        df = X.copy()
        # 删除缺失率超过阈值的列
        df = df.dropna(thresh=len(df) * (1 - self.drop_threshold), axis=1)
        # 填充
        df.fillna(value=self.fill_values_, inplace=True)
        return df

# 使用
cleaner = DataFrameCleaner(numeric_strategy="median")
df_clean = cleaner.fit_transform(df_raw)
```

---

## 五、系统设计题（3-5题）

### 5.1 设计 AI 聊天应用架构

```text
┌──────────────────────────────────────────────────┐
│                  前端层 (Streamlit)              │
│  - 聊天界面 / 会话管理 / 历史记录 / 设置面板     │
└──────────────────┬───────────────────────────────┘
                    │ HTTP / WebSocket
┌──────────────────▼───────────────────────────────┐
│                  API 网关层 (FastAPI)             │
│  - 请求路由 / 身份认证 / 限流 / 日志             │
│  - 会话管理 (Redis) / 消息队列                   │
└────┬─────────┬──────────┬──────────┬─────────────┘
     │         │          │          │
┌────▼───┐ ┌──▼─────┐ ┌──▼─────┐ ┌──▼──────────┐
│ Prompt  ││RAG 检索││LLM 调 ││工具调用         │
│ 工程    ││        ││ 用    ││(搜索/计算/代码) │
└────────┘ └────────┘ └──┬─────┘ └──────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│              LLM 网关层                          │
│  - API 路由 (DeepSeek / OpenAI / Ollama)        │
│  - 负载均衡 / 重试 / 降级                       │
│  - 流式输出 SSE / Token 计数                    │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│              数据存储层                          │
│  - PostgreSQL: 用户 / 会话 / 配置               │
│  - Redis: 缓存 / 限流 / 实时会话               │
│  - 向量数据库: RAG 文档嵌入                     │
└─────────────────────────────────────────────────┘
```

**核心设计要点：**

| 组件 | 选型 | 说明 |
|------|------|------|
| 前端 | Streamlit | 快速开发 AI 演示界面 |
| API 网关 | FastAPI | 异步高性能，原生 SSE 支持 |
| 缓存 | Redis | 会话存储、Token 限流 |
| 数据库 | PostgreSQL | 结构化数据持久化 |
| LLM 调用 | httpx + SSE | 支持流式输出和超时重试 |
| Prompt | Jinja2 模板 | 结构化 Prompt 管理 |
| 监控 | Prometheus + Grafana | 请求量、延迟、Token 用量 |

### 5.2 设计数据采集与分析 Pipeline

```text
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ 数据源   │ → │ 爬虫/API │ → │ 数据清洗 │ → │  存储    │
│(网页/API)│   │ 采集层   │   │ 转换层   │   │ (CSV/DB) │
└──────────┘   └──────────┘   └──────────┘   └────┬─────┘
                                                   │
┌──────────┐   ┌──────────┐   ┌──────────┐        │
│ 可视化   │ ← │ 分析建模 │ ← │ 数据加载 │ ←──────┘
│(Matplot) │   │(Pandas)  │   │ (ETL)    │
└──────────┘   └──────────┘   └──────────┘
```

**实现要点：**

```python
# Pipeline 编排示例
from dataclasses import dataclass
from typing import Callable

@dataclass
class Pipeline:
    stages: list[Callable]

    def add(self, stage: Callable) -> "Pipeline":
        self.stages.append(stage)
        return self

    def run(self, data):
        for stage in self.stages:
            data = stage(data)
        return data

# 构建 pipeline
pipeline = Pipeline([])
pipeline.add(fetch_raw_data)
pipeline.add(clean_data)
pipeline.add(transform_features)
pipeline.add(analyze_patterns)
pipeline.add(generate_report)

result = pipeline.run(start_params)
```

| 阶段 | 工具 | 关键点 |
|------|------|--------|
| 数据采集 | Scrapy / requests | 反爬策略、频率控制、断点续爬 |
| 数据清洗 | Pandas | 缺失值处理、类型转换、去重 |
| 数据存储 | CSV / Parquet / SQLite | 列式存储适合分析 |
| 数据分析 | Pandas / NumPy | 向量化操作、分组聚合 |
| 可视化 | Matplotlib / Seaborn | 图表风格统一、中文支持 |
| 报告生成 | Jinja2 + HTML | 自动化报告输出 |

### 5.3 设计 Web 应用部署方案

```text
┌─────────────────────────────────────────────────┐
│                  用户 (浏览器 / API 客户端)      │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Nginx 反向代理                      │
│  - SSL 终止 / 静态资源 / 负载均衡               │
│  - 速率限制 / 白名单                            │
└────┬───────────────────────────────┬────────────┘
     │                               │
┌────▼─────────┐          ┌─────────▼───────────┐
│ FastAPI App  │          │  Streamlit App       │
│ (uvicorn × N)│          │  (单进程或多会话)    │
│ API 服务      │          │  AI 交互界面         │
└────┬─────────┘          └─────────┬───────────┘
     │                              │
┌────▼──────────────────────────────▼────────────┐
│              数据库 & 缓存                      │
│  PostgreSQL + Redis + 向量数据库                │
└────────────────────────────────────────────────┘
```

**Docker Compose 部署：**

```yaml
version: "3.8"
services:
  api:
    build: ./api
    command: uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql://user:pass@db:5432/app
      - REDIS_URL=redis://redis:6379/0
    depends_on:
      - db
      - redis

  streamlit:
    build: ./frontend
    command: streamlit run app.py --server.port 8501
    ports:
      - "8501:8501"
    depends_on:
      - api

  db:
    image: postgres:15
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - api
      - streamlit

volumes:
  pgdata:
```

### 5.4 设计 RAG 检索增强生成系统

```text
┌────────┐
│ 用户  │ → "XX 技术的最新进展是什么？"
└───┬────┘
    │
┌───▼──────────────────────────────┐
│ 1. 查询理解                       │
│    - 意图识别 / 查询重写 / 多轮   │
└───┬──────────────────────────────┘
    │
┌───▼──────────────────────────────┐
│ 2. 检索阶段                       │
│    ┌─────────────────┐           │
│    │ Embedding 模型   │           │
│    │ text → vector    │           │
│    └────────┬────────┘           │
│             │                     │
│    ┌────────▼────────┐           │
│    │ 向量数据库      │           │
│    │ (Milvus / FAISS)│           │
│    └────────┬────────┘           │
│             │                     │
│    ┌────────▼────────┐           │
│    │ Top-K 相关文档  │           │
│    └────────┬────────┘           │
└───┬──────────────────────────────┘
    │
┌───▼──────────────────────────────┐
│ 3. 增强生成                       │
│    Prompt = 指令 + 检索文档 + 问题│
│    → LLM 生成带引用的回答         │
└───┬──────────────────────────────┘
    │
┌───▼────┐
│ 回答   │ "根据最新资料..."
└────────┘
```

```python
class RAGPipeline:
    """简单 RAG 实现"""

    def __init__(self, embedding_model, llm_client, vector_store):
        self.embedder = embedding_model
        self.llm = llm_client
        self.store = vector_store

    def retrieve(self, query: str, k: int = 5) -> list[str]:
        query_vector = self.embedder.embed(query)
        docs = self.store.search(query_vector, top_k=k)
        return [doc.text for doc in docs]

    def generate(self, query: str, context_docs: list[str]) -> str:
        context = "\n\n".join(context_docs)
        prompt = f"""基于以下资料回答问题。
如果资料中不包含答案，请说明"资料中未找到相关信息"。

资料：
{context}

问题：{query}

回答："""
        return self.llm.chat(prompt)

    def query(self, user_input: str) -> str:
        docs = self.retrieve(user_input)
        return self.generate(user_input, docs)
```

---

## 六、常见坑点与最佳实践

| 编号 | 坑点 | 错误示例 | 正确做法 | 原理 |
|------|------|---------|---------|------|
| 1 | **可变默认参数** | `def f(lst=[])` | `def f(lst=None)` 内部判空创建 | 默认参数在定义时求值，所有调用共享同一 list |
| 2 | **循环中 lambda 捕获** | `lambdas = [lambda: i for i in range(5)]` | `lambda i=i: i` | 闭包捕获的是变量引用，不是值 |
| 3 | **浅拷贝 vs 深拷贝** | `b = a` 修改 b 影响 a | `copy.deepcopy(a)` | 赋值是引用，浅拷贝只复制一层 |
| 4 | **Pandas chained indexing** | `df[df.a>0]["b"] = 1` 可能不生效 | `df.loc[df.a>0, "b"] = 1` | 链式索引返回副本而非视图 |
| 5 | **for 循环修改列表** | `for i in lst: lst.remove(i)` | 用列表推导式或 `lst[:]` 迭代副本 | 删除元素导致索引偏移 |
| 6 | **GIL 下多线程计算** | 用 `threading` 做 CPU 计算 | 用 `multiprocessing` 或 `numpy` | GIL 阻止并行执行字节码 |
| 7 | **爬虫未设置编码** | 直接 `resp.text` 中文乱码 | `resp.encoding = resp.apparent_encoding` | 自动检测编码避免乱码 |
| 8 | **FastAPI 同步 I/O 阻塞** | `async def` 中调用 `time.sleep()` | 用 `asyncio.sleep()` 或 `def` 路由 | 同步阻塞会挂起事件循环 |
| 9 | **大模型 Token 超限** | 不控制上下文长度直接传所有历史 | 滑动窗口 / 摘要压缩历史 | 模型有固定上下文窗口 |
| 10 | **流式输出拼接错误** | 直接拼接 `chunk` 忽略 `delta.content` 为空 | 过滤空 content，并处理 `[DONE]` 标记 | SSE 协议中有的 chunk 不含 content |
| 11 | **Pandas 修改视图警告** | `df[df.a>0].iloc[0] = 1` | 使用 `.loc` 统一修改 | Chain indexing 产生 SettingWithCopyWarning |
| 12 | **Matplotlib 中文乱码** | 不设置字体直接显示中文汉字 | `plt.rcParams["font.sans-serif"] = ["SimHei"]` | 默认字体不含中文字符集 |

---

## 七、面试回答模板（Top 5 高频题的结构化回答模板）

### 7.1 "Python 中可变对象和不可变对象的区别？"

**回答框架（4 层）：**

> **第一层：定义区别**
> Python 对象分为可变和不可变。可变对象创建后内容可以修改（list, dict, set），不可变对象创建后不能修改（int, str, tuple）。
>
> **第二层：内存行为**
> 不可变对象修改时，实际上是创建新对象，原对象不变。可变对象修改时，内存地址不变，内容改变。
> ```python
> a = [1, 2]       # 可变
> b = a
> a.append(3)      # b 也会变成 [1, 2, 3]（同一对象）
>
> c = "hello"      # 不可变
> d = c
> c += " world"    # c 变成新字符串，d 仍是 "hello"
> ```
>
> **第三层：应用场景**
> - 不可变对象可作为 dict 的 key 或 set 的元素
> - 可变对象作为函数默认参数有陷阱（被所有调用共享）
>
> **第四层：性能影响**
> 不可变对象线程安全，适合多线程共享。频繁修改字符串用 list 拼接再 `join()` 比 `+=` 高效。

### 7.2 "Pandas 中 apply 和 vectorization 的性能差异？"

**回答框架：**

> **第一层：本质差异**
> Vectorization 是**向量化操作**，底层由 C/NumPy 实现，整个列一次性计算。Apply 是对每行循环执行 Python 函数，有大量 Python 调用开销。
>
> **第二层：性能量化**
> ```python
> # 100 万行数据
> df["c"] = df["a"] + df["b"]  # vectorization: ~1ms
> df["c"] = df.apply(lambda r: r["a"] + r["b"], axis=1)  # apply: ~200ms
> ```
> Vectorization 快 100-500 倍。
>
> **第三层：何时用 apply**
> - 需要复杂的行级逻辑（多列条件判断）
> - 调用第三方库函数（如文本处理）
> - 数据量小于 1 万行时差异不明显
>
> **第四层：优化方案**
> - 能用 NumPy 表达式就用（`np.where`, `np.select`）
> - 用 `.str` 访问器代替 apply 做字符串处理
> - 用 `groupby.transform` 代替分组后 apply

### 7.3 "大模型 API 调用的最佳实践？"

**回答框架：**

> **第一层：基础架构**
> ```python
> # 核心配置：API Key 管理、超时设置、错误重试
> import requests
> from tenacity import retry, stop_after_attempt, wait_exponential
>
> @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2))
> def call_llm(messages):
>     resp = requests.post(url, headers=headers, json={"messages": messages}, timeout=30)
>     resp.raise_for_status()
>     return resp.json()
> ```
>
> **第二层：流式输出（用户体验关键）**
> 使用 SSE 协议逐 Token 返回，通过 `stream=True` 开启。前端可实时显示打字效果。
>
> **第三层：上下文管理**
> - 滑动窗口：保留最近 N 轮对话
> - 摘要压缩：超出上下文时对早期对话做摘要
> - Token 计数：用 `tiktoken` 提前计数，避免 400 错误
>
> **第四层：工程化要点**
> - API Key 通过环境变量注入，不硬编码
> - 实现熔断降级（连续失败切备用模型）
> - 监控 Token 消耗和响应延迟
> - 缓存相同提问的回复（减少重复消耗）

### 7.4 "FastAPI 和 Flask 的区别？"

**回答框架：**

> **第一层：核心差异**
> ```text
> | 维度       | FastAPI                          | Flask                          |
> |-----------|----------------------------------|--------------------------------|
> | 异步支持   | 原生 async/await，高性能         | 需额外插件，异步不原生         |
> | 类型校验   | Pydantic 自动校验请求/响应       | 手动校验或加 marshmallow        |
> | 文档生成   | 自动 OpenAPI + Swagger UI        | 需手动集成 flasgger             |
> | 性能       | 约 Flask 的 2-3 倍               | 同步模式，受 GIL 限制           |
> | 社区生态   | 新兴，增长迅速                    | 成熟，插件丰富                  |
> | 学习成本   | 中等（需理解类型注解和异步）      | 低，上手简单                    |
> ```
>
> **第二层：适用场景**
> - FastAPI：AI 服务 API、微服务、高并发、需要自动文档
> - Flask：简单 CRUD、小型项目、开发者更熟悉同步模式
>
> **第三层：实战选型建议**
> 新项目选 FastAPI。尤其 Python+AI 场景，高并发调大模型需要异步支持，FastAPI + httpx.AsyncClient 是最佳组合。

### 7.5 "如何设计一个 AI Web 应用？"

**回答框架：**

> **第一层：架构分层**
> 前端（Streamlit） → API 网关（FastAPI） → AI 服务层（LLM/RAG） → 存储层（PostgreSQL/Redis）
>
> **第二层：核心功能模块**
> - 用户交互：聊天界面、历史会话管理、文件上传
> - AI 能力：大模型对话、RAG 知识库检索、流式输出
> - 数据存储：用户配置持久化、会话历史、文档向量
>
> **第三层：技术选型理由**
> ```text
> Streamlit — 快速构建 AI 演示界面，Python 全栈
> FastAPI   — 异步高性能，原生 SSE 支持流式输出
> Redis     — 会话缓存 + Token 限流
> 向量数据库— RAG 场景下的文档检索
> Docker    — 统一部署环境，简化运维
> ```
>
> **第四层：部署与运维**
> - Docker Compose 编排多服务
> - Nginx 反向代理 + SSL
> - 日志监控（Prometheus + Grafana）
> - CI/CD 自动部署
>
> **第五层：可扩展性**
> - 水平扩展：多实例 uvicorn workers
> - 缓存策略：Redis 缓存常见问题回复
> - 降级方案：LLM 调用失败时返回缓存或友好提示

---

## 八、快速查漏补缺 Checklist

> 面试前逐项确认已掌握，未掌握的先标记，优先补齐

### 基础语法
- [ ] 可变与不可变类型的定义和区别
- [ ] 列表推导式、字典推导式、集合推导式
- [ ] 生成器表达式与生成器函数（yield）
- [ ] 装饰器的实现原理与 functools.wraps
- [ ] GIL 的影响与规避方案
- [ ] 深浅拷贝的区别
- [ ] lambda 表达式与使用场景
- [ ] `__init__` / `__str__` / `__repr__` / `__call__` 等魔法方法
- [ ] 异常处理 try/except/else/finally 完整结构
- [ ] with 语句与上下文管理器
- [ ] 模块导入与 `__init__.py` / `__name__ == "__main__"`
- [ ] 类型注解语法（typing 模块）

### 面向对象
- [ ] 封装（单下划线、双下划线 name mangling）
- [ ] 继承与 MRO（C3 线性化）
- [ ] 多态与鸭子类型
- [ ] super() 的调用链
- [ ] 类方法 `@classmethod` vs 静态方法 `@staticmethod`
- [ ] 抽象类与 ABC（`@abstractmethod`）
- [ ] 类属性与实例属性的查找顺序

### AI 与 LLM
- [ ] DeepSeek API 调用（普通 + 流式）
- [ ] SSE 流式协议解析
- [ ] Tokenization 与 Token 计数
- [ ] Prompt Engineering 模板设计
- [ ] 会话记忆管理方案
- [ ] Ollama 本地部署原理
- [ ] RAG 检索增强生成流程

### Streamlit
- [ ] st.session_state 状态管理
- [ ] st.chat_input + st.chat_message 聊天组件
- [ ] 侧边栏布局（st.sidebar）
- [ ] 会话历史持久化（JSON 文件 / 数据库）
- [ ] 缓存（st.cache_data, st.cache_resource）

### 爬虫
- [ ] requests 库基本用法
- [ ] BeautifulSoup 选择器（select / find / find_all）
- [ ] XPath 语法（lxml 库）
- [ ] CSV 文件读写
- [ ] 反爬策略（UA 伪装、代理、随机延迟）
- [ ] 正则表达式基础（re 模块）

### 数据分析
- [ ] Pandas Series vs DataFrame
- [ ] 数据加载（read_csv / read_excel / read_json）
- [ ] 缺失值处理（fillna / dropna）
- [ ] 数据合并（merge / concat / join）
- [ ] 分组聚合（groupby + agg）
- [ ] 排序（sort_values）与排名（rank）
- [ ] 数据透视表（pivot_table）
- [ ] 向量化操作 vs apply vs loop 性能
- [ ] Matplotlib 子图布局（subplots）
- [ ] Matplotlib 柱状图 / 饼状图 / 散点图 / 直方图
- [ ] 图表中文显示配置

### FastAPI
- [ ] 路由定义（GET / POST / PUT / DELETE）
- [ ] Pydantic 数据模型验证
- [ ] 异步端点（async def）vs 同步端点（def）
- [ ] 依赖注入（Depends）
- [ ] 请求参数校验（Query / Path / Body）
- [ ] 异常处理（HTTPException）
- [ ] 日志配置
- [ ] uvicorn 部署
- [ ] 中间件（CORS、请求日志）

### 高级 Python
- [ ] 多线程 vs 多进程 vs 异步选择
- [ ] 内存管理与垃圾回收（分代回收）
- [ ] 元类（metaclass）基础
- [ ] 描述器（`__get__` / `__set__`）

### 部署与工程
- [ ] pip / conda 包管理
- [ ] requirements.txt 与虚拟环境
- [ ] Docker + Docker Compose 编排
- [ ] Nginx 反向代理配置
- [ ] 环境变量管理（.env 文件）

### 手写代码准备
- [ ] 手写装饰器（计时 / 日志 / 重试）
- [ ] 手写生成器（斐波那契）
- [ ] 手写爬虫（requests + BeautifulSoup）
- [ ] 手写 FastAPI CRUD 路由
- [ ] 手写数据清洗函数
- [ ] 手写大模型流式调用

### 整体评估
- [ ] Python 核心语法（可变性、OOP、GC）
- [ ] AI 大模型应用（API 调用、Prompt、会话）
- [ ] 数据分析（Pandas + Matplotlib）
- [ ] Web 应用开发（FastAPI + Streamlit）
- [ ] 系统设计（AI 聊天、RAG、部署方案）

---

> 🎯 **面试核心策略**：Python+AI 面试的核心亮点在于**将 Python 技能应用到 AI 场景**。准备 2-3 个完整项目（AI 聊天助手、数据分析 Dashboard、RAG 知识库），用 FastAPI + Streamlit 做出可演示的 Demo，面试时现场展示或讲解架构，远胜于背诵理论。黑马程序员教程中的 **AI 汉字谜盒** 项目是极佳的面试 Demo：融合了 FastAPI 路由、Pydantic 校验、会话管理、LLM 调用、前端演示，单项目覆盖 80% 面试考点。
