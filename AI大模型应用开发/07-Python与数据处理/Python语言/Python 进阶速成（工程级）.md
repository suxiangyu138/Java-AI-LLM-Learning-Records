Python 进阶速成（工程级）
1. 类型注解（企业必写）
    python
    from typing import List, Dict, Optional
    def calc(nums: List[int]) -> int:
    total: int = sum(nums)
    return total

# 可选参数
def get_info(name: str, age: Optional[int] = None) -> Dict[str, str]:
    return {"name": name}
 
2. 推导式（高效简写，AI数据处理高频）
    python

# 列表推导
arr = [x*2 for x in range(10) if x % 2 == 0]

# 字典推导
data = {k: str(v) for k, v in enumerate(range(5))}

# 生成器（省内存，处理大文本/大向量）
gen = (x for x in range(100000))
 
3. 高阶函数
    python
    lst = [1,2,3,4]
    res1 = list(map(lambda x:x**2, lst))
    res2 = list(filter(lambda x:x>2, lst))
 
4. 装饰器（接口缓存、日志、权限、工具封装）
    python
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
 
5. 异常精细捕获 + 上下文管理
    python
    try:
    1 / 0
    except ZeroDivisionError:
    print("除零错误")
    finally:
    print("必执行")

# with 上下文（文件、数据库、Milvus 连接全用这个）
with open("data.txt", "r", encoding="utf-8") as f:
    content = f.read()
 
6. 面向对象 OOP（写Agent、自定义Skill、工具类必备）
    python
    class VectorService:

    # 类属性
    source: str = "milvus"
    def __init__(self, dim: int):

        # 实例属性
        self.dim = dim
    def search(self, query: str) -> list:
        return [query]

# 实例化
svc = VectorService(1024)
 
7. 魔术方法
    python
    class Doc:
    def __init__(self, content):
        self.content = content
    def __str__(self):
        return self.content
    def __repr__(self):
        return f"Doc({self.content})"
 
8. 模块 & 包管理
    python

# 相对导入 / 绝对导入
from utils.tools import search

# 动态导入
import importlib
mod = importlib.import_module("langchain")
 
9. 并发编程（批量嵌入、批量文档处理）
    python
    import threading
    import asyncio

# 多线程
def task():
    pass
t = threading.Thread(target=task)

# 异步 async/await（AI接口、HTTP 请求高频）
async def async_req():
    await asyncio.sleep(1)
 
10. 常用高阶标准库
    python
    import os, sys, time, json, re
    from pathlib import Path  # 现代文件路径
    from collections import defaultdict, Counter
    from dataclasses import dataclass  # 轻量化实体类
 
dataclass 极简实体定义（替代普通 class）
python
from dataclasses import dataclass
@dataclass
class Chunk:
    text: str
    vector: list[float]
    source: str
 
11. JSON / 序列化（接口交互、配置文件）
    python
    import json

# 序列化
json_str = json.dumps({"code":200,"data":[]})

# 反序列化
obj = json.loads(json_str)
 
12. 网络请求（调用大模型API、第三方接口）
    python
    import requests
    resp = requests.post(
    "http://localhost:11434/api/chat",
    json={"model":"qwen","messages":[]}
    )
 
13. 正则表达式（文本清洗、文档预处理 RAG 刚需）
    python
    import re
    text = re.sub(r"\s+", "", raw_text)
    match = re.findall(r"\d+", text)
 
 
你专属：Python 高阶 + AI 技术栈 对应关系
1. 推导式 / 生成器 → 批量切片、向量批量处理
2. 类型注解 / dataclass → RAG 结构化数据、Agent 参数
3. 装饰器 → 工具函数封装、日志、缓存
4. OOP → 自定义向量服务、Agent 调度器
5. 异步 async → 并发调用 LLM、Embedding
6. 正则+文件 → 文档清洗、预处理
