# Python 进阶速成（工程级）

> **核心摘要**：面向 Java 后端的 Python 工程级进阶指南，覆盖类型注解、推导式、装饰器、异步编程、OOP 封装等关键知识点，每项都标注了在大模型开发中的实际应用场景。

---

## 一、类型注解（企业必写）

```python
from typing import List, Dict, Optional

def calc(nums: List[int]) -> int:
    total: int = sum(nums)
    return total

def get_info(name: str, age: Optional[int] = None) -> Dict[str, str]:
    return {"name": name}
```

**AI 场景**：RAG 结构化数据、Agent 参数类型定义。

## 二、推导式（高效简写）

```python
# 列表推导
arr = [x * 2 for x in range(10) if x % 2 == 0]

# 字典推导
data = {k: str(v) for k, v in enumerate(range(5))}

# 生成器（省内存，处理大文本/大向量）
gen = (x for x in range(100000))
```

**AI 场景**：批量切片、向量批量处理。

## 三、高阶函数

```python
lst = [1, 2, 3, 4]
res1 = list(map(lambda x: x ** 2, lst))
res2 = list(filter(lambda x: x > 2, lst))
```

## 四、装饰器（接口缓存、日志、权限封装）

```python
def log_wrapper(func):
    def inner(*args, **kwargs):
        print("函数执行开始")
        res = func(*args, **kwargs)
        print("函数执行结束")
        return res
    return inner

@log_wrapper
def run_task():
    print("执行任务")
```

**AI 场景**：工具函数封装、日志记录、API 调用缓存。

## 五、异常精细捕获 + 上下文管理

```python
try:
    1 / 0
except ZeroDivisionError:
    print("除零错误")
finally:
    print("必执行")

# with 上下文（文件、数据库、Milvus 连接全用这个）
with open("data.txt", "r", encoding="utf-8") as f:
    content = f.read()
```

## 六、面向对象 OOP

```python
class VectorService:
    source: str = "milvus"

    def __init__(self, dim: int):
        self.dim = dim

    def search(self, query: str) -> list:
        return [query]

svc = VectorService(1024)
```

**AI 场景**：自定义 Agent、自定义 Skill、工具类封装。

## 七、dataclass 极简实体类

```python
from dataclasses import dataclass

@dataclass
class Chunk:
    text: str
    vector: list[float]
    source: str
```

> **重点**：`dataclass` 替代普通 class 的样板代码，适合 RAG 中的数据实体定义。

## 八、异步编程 async/await

```python
import asyncio

async def async_req():
    await asyncio.sleep(1)
    return "done"

async def main():
    tasks = [async_req() for _ in range(5)]
    results = await asyncio.gather(*tasks)

asyncio.run(main())
```

**AI 场景**：并发调用 LLM、批量 Embedding、HTTP 接口请求。

## 九、JSON 序列化

```python
import json

# 序列化
json_str = json.dumps({"code": 200, "data": []})

# 反序列化
obj = json.loads(json_str)
```

## 十、正则表达式（文本清洗必备）

```python
import re

text = re.sub(r"\s+", "", raw_text)  # 去除空白
match = re.findall(r"\d+", text)     # 提取数字
```

**AI 场景**：RAG 文档清洗、文本预处理、模型输出解析。

## 核心要点回顾

- 推导式/生成器 → 批量切片、向量批量处理
- 类型注解 / dataclass → RAG 结构化数据、Agent 参数
- 装饰器 → 工具函数封装、日志、缓存
- OOP → 自定义向量服务、Agent 调度器
- 异步 async → 并发调用 LLM、Embedding
- 正则 + 文件 → 文档清洗、预处理

## 参考资料

1. Python 官方文档 - typing 类型注解
2. Python 官方文档 - asyncio 异步编程
3. PEP 557 - Data Classes
