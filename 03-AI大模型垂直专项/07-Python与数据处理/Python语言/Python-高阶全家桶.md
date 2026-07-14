# Python 高阶全家桶

> **核心摘要**：面向 AI + 工程 + 异步 + OOP + 数据处理 + Java 交互的 Python 高阶技能图谱，每一项都标注了在 RAG/Agent/大模型开发中的实际应用场景。

---

## 一、高级数据结构与高效数据处理

### 1.1 collections 工具集

```python
from collections import defaultdict, Counter, deque

d = defaultdict(list)       # 自动初始化字典，避免 KeyError
d["a"].append(1)

cnt = Counter("abracadabra")  # 快速统计词频

q = deque([1, 2, 3])         # 高效队列，头尾操作 O(1)
q.popleft()
```

### 1.2 dataclass 轻量实体类

```python
from dataclasses import dataclass, field
from typing import List

@dataclass
class Document:
    content: str
    vector: List[float] = field(default_factory=list)
    source: str = ""
```

### 1.3 正则文本清洗（RAG 预处理刚需）

```python
import re

def clean_text(raw: str) -> str:
    raw = re.sub(r"\s+", " ", raw)
    raw = re.sub(r"[^一-龥a-zA-Z0-9\s，。；：]", "", raw)
    return raw.strip()
```

## 二、OOP 面向对象进阶

```python
class BaseSkill:
    def run(self):
        raise NotImplementedError

class RagSkill(BaseSkill):
    def __init__(self, milvus_url: str):
        self._url = milvus_url  # 私有约定

    def run(self, query: str):
        return f"检索：{query} | 连接 {self._url}"
```

**应用场景**：自定义 Agent、自定义 Skill、服务封装。

## 三、装饰器与高阶函数

```python
from functools import wraps, lru_cache

# 通用日志装饰器
def logger(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        print(f"[RUN] {func.__name__}")
        return func(*args, **kwargs)
    return wrapper

# 内存缓存，重复 LLM/向量检索直接命中
@lru_cache(maxsize=128)
def embed_text(text: str) -> str:
    return f"vec_{hash(text)}"
```

## 四、异步编程 async/await

```python
import asyncio
import aiohttp

async def fetch_llm(query: str):
    async with aiohttp.ClientSession() as session:
        async with session.post(
            "http://localhost:11434/api/chat",
            json={"model": "qwen", "messages": [{"role": "user", "content": query}]}
        ) as resp:
            return await resp.json()

async def main():
    tasks = [fetch_llm(f"doc{i}") for i in range(5)]
    results = await asyncio.gather(*tasks)
```

**应用场景**：并发调用大模型 API、批量 Embedding。

## 五、文件系统与路径工程化

```python
from pathlib import Path

BASE_DIR = Path(__file__).parent
data_path = BASE_DIR / "data" / "docs.txt"
data_path.parent.mkdir(parents=True, exist_ok=True)
text = data_path.read_text(encoding="utf-8")
data_path.write_text("content", encoding="utf-8")
```

## 六、异常处理与日志体系

```python
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
logging.info("服务启动成功")
```

## 七、Python 调用 Java

### 方案 1：subprocess 调用 Jar

```python
import subprocess

def call_java_jar(jar_path: str, args: list[str]):
    cmd = ["java", "-jar", jar_path] + args
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    return res.stdout, res.stderr
```

### 方案 2：py4j 跨语言互调

```bash
pip install py4j
```

## 八、工程化项目目录结构

```
ai_project/
├── config/         # 配置文件
├── core/           # 核心服务：RAG、Agent、向量库
├── skills/         # 自定义工具 Skill
├── utils/          # 工具函数：文本清洗、日志、请求
├── data/           # 本地文档、向量缓存
├── main.py         # 入口
└── requirements.txt
```

## 核心要点回顾

- `dataclass` + 文本清洗 + 文件路径 → RAG 预处理
- 装饰器 + 日志 + 异常 → 代码健壮性
- `async/await` + 异步请求 → 批量调用 AI
- OOP 封装 Agent / Skill 类 → 模块化设计
- subprocess/py4j → Python 调用 Java 实现双栈打通

## 参考资料

1. Python `pathlib` 文档 - 文件路径处理
2. Python `asyncio` 文档 - 异步 IO
3. py4j 官方文档 - Python/Java 互操作
