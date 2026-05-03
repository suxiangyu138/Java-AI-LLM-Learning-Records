04.27 11:18
Python 高阶全家桶（AI+工程+异步+OOP+数据处理+Java交互）
一、高级数据结构 & 高效数据处理（RAG 文本/向量必用）

1. collections 工具集

python
from collections import defaultdict, Counter, deque

# 自动初始化字典，避免 key 不存在报错

d = defaultdict(list)
d["a"].append(1)

# 快速统计词频、文本关键词

cnt = Counter("abracadabra")

# 高效队列，头尾操作 O(1)

q = deque([1,2,3])
q.popleft()
 
2. dataclass 轻量实体类（替代普通 Class）
python
from dataclasses import dataclass, field
from typing import List
@dataclass
class Document:
    content: str
    vector: List[float] = field(default_factory=list)
    source: str = ""
 
3. 高阶推导式 + 生成器（大数据省内存）
python

# 嵌套推导

matrix = [[i+j for j in range(3)] for i in range(3)]

# 惰性生成器，百万级文本/向量不爆内存

chunk_gen = (doc for doc in big_doc_list if len(doc) > 50)
 
4. 正则文本清洗（RAG 预处理刚需）
python
import re
def clean_text(raw: str) -> str:
    raw = re.sub(r"\s+", " ", raw)
    raw = re.sub(r"[^\u4e00-\u9fa5a-zA-Z0-9\s，。；：]", "", raw)
    return raw.strip()
 
 
二、OOP 面向对象进阶（自定义Agent、Skill、服务封装）

1. 继承、多态、私有属性

python
class BaseSkill:
    def run(self):
        raise NotImplementedError
class RagSkill(BaseSkill):
    def __init__(self, milvus_url: str):
        self._url = milvus_url  # 私有约定
    def run(self, query: str):
        return f"检索：{query} ｜ 连接 {self._url}"
 
1. 类方法 / 静态方法 / 属性装饰器

python
class VectorUtil:
    @staticmethod
    def normalize(vec: list[float]) -> list[float]:
        import math
        norm = math.sqrt(sum(x*x for x in vec))
        return [x/norm for x in vec]
    @classmethod
    def info(cls):
        print(cls.__name__)
    @property
    def dim(self):
        return 1024
 
 

三、装饰器 & 高阶函数（日志、缓存、权限、接口拦截）
python
from functools import wraps, lru_cache
# 通用日志装饰器
def logger(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        print(f"[RUN] {func.__name__}")
        res = func(*args, **kwargs)
        return res
    return wrapper
# 内存缓存，重复LLM/向量检索直接命中
@lru_cache(maxsize=128)
def embed_text(text: str) -> str:
    return f"vec_{hash(text)}"
 
 
四、异步编程 async/await（并发调大模型、批量Embedding）
1. 基础异步语法
python
import asyncio
async def async_task(name: str):
    await asyncio.sleep(0.5)
    print(f"完成：{name}")
async def main():
    tasks = [async_task(f"doc{i}") for i in range(5)]
    await asyncio.gather(*tasks)
# 入口运行
# asyncio.run(main())
 
2. 异步 HTTP 请求（调用 Ollama/各类AI接口）
python
import aiohttp
async def fetch_llm(query: str):
    async with aiohttp.ClientSession() as session:
        async with session.post(
            "http://localhost:11434/api/chat",
            json={"model":"qwen","messages":[{"role":"user","content":query}]}
        ) as resp:
            return await resp.json()
 
 
五、文件系统 & 路径工程化（跨平台统一）
python
from pathlib import Path
# 无需拼接字符串，Windows/Linux 自动适配
BASE_DIR = Path(__file__).parent
data_path = BASE_DIR / "data" / "docs.txt"
# 快速创建目录
data_path.parent.mkdir(parents=True, exist_ok=True)
# 读写文件极简
text = data_path.read_text(encoding="utf-8")
data_path.write_text("content", encoding="utf-8")
 
 
六、异常处理 & 日志体系（项目上线必备）
1. 精细化异常捕获
python
def safe_div(a: int, b: int) -> float | None:
    try:
        return a / b
    except ZeroDivisionError:
        print("除零错误")
    except TypeError:
        print("类型错误")
    return None
 
2. 标准库日志替代 print
python
import logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
logging.info("服务启动成功")
 
 
七、配置文件 & JSON 序列化（项目解耦）
python
import json
# 写入配置
config = {"milvus_host":"127.0.0.1", "port":19530}
with open("config.json", "w", encoding="utf-8") as f:
    json.dump(config, f, ensure_ascii=False, indent=2)
# 读取配置
with open("config.json", "r", encoding="utf-8") as f:
    cfg = json.load(f)
 
 
八、多线程 / 多进程（密集任务并行）
python
# 多线程：IO密集（网络、文件）
import threading
def io_task():
    pass
t = threading.Thread(target=io_task)
t.start()
# 多进程：CPU密集（向量计算、大模型本地推理）
import multiprocessing
 
 
九、Python 调用 Java（双栈打通，你后端刚需）
方案1：subprocess 调用 Jar（最简稳定）
python
import subprocess
def call_java_jar(jar_path: str, args: list[str]):
    cmd = ["java", "-jar", jar_path] + args
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    return res.stdout, res.stderr
 
方案2：py4j 跨语言实时互调
bash
pip install py4j
 
Python 直接调用 Java 类、方法，双向通信，适合混合开发。
 
十、工程化项目目录结构（企业级规范）
plaintext
ai_project/
├── config/         # 配置文件
├── core/           # 核心服务：RAG、Agent、向量库
├── skills/         # 自定义工具Skill
├── utils/          # 工具函数：文本清洗、日志、请求
├── data/           # 本地文档、向量缓存
├── main.py         # 入口
└── requirements.txt
 
 
十一、极简学习落地顺序（直接照着练）
1. 先练： dataclass  + 文本清洗 + 文件路径 → RAG 预处理
2. 再练：装饰器 + 日志 + 异常 → 代码健壮性
3. 进阶： async/await  + 异步请求 → 批量调用AI
4. 拔高：OOP 封装 Agent / Skill 类
5. 最后：Python 调用 Java、工程化分层

