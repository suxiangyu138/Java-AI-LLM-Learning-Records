# 10 - 函数式编程：map / filter / reduce

> 🎯 Python 虽然不是纯函数式语言，但内置的 map、filter、reduce + lambda + 推导式，构成了强大的数据处理流水线。AI 开发中数据预处理、特征提取、批量转换——函数式风格一行顶十行

---

## 目录

1. [推导式 vs map/filter](#1-推导式-vs-mapfilter)
2. [reduce 与累积操作](#2-reduce-与累积操作)
3. [functools 工具箱](#3-functools-工具箱)
4. [AI 开发实战](#4-ai-开发实战)

---

## 1. 推导式 vs map/filter

```python
data = [1, 2, 3, 4, 5]

# 推导式（通常更 Pythonic）
squared = [x**2 for x in data]                     # [1, 4, 9, 16, 25]
evens = [x for x in data if x % 2 == 0]            # [2, 4]
pairs = [(x, y) for x in data for y in [10, 20]]   # 嵌套

# map/filter（函数式风格，配合已有函数更简洁）
squared = list(map(lambda x: x**2, data))
evens = list(filter(lambda x: x % 2 == 0, data))

# 混合：推导式 + 函数
names = list(map(str.upper, names))         # map + 已有函数 ✅
admins = [u for u in users if u.is_admin]  # 推导式 + 条件 ✅
```

### 选择建议

| 场景 | 推荐 | 原因 |
|------|:---:|------|
| 简单转换 | 推导式 | 更可读 |
| 配合已有函数 | `map(str.upper)` | 更简洁 |
| 复杂逻辑 | 推导式 | 内联逻辑 |
| 惰性处理 | `map` 生成器 | 大数据集不占内存 |

## 2. reduce 与累积操作

```python
from functools import reduce

# reduce: 累积计算
nums = [1, 2, 3, 4, 5]
product = reduce(lambda a, b: a * b, nums)      # 120 (5!)
max_val = reduce(lambda a, b: a if a > b else b, nums)  # 5

# 初始值
total = reduce(lambda a, b: a + b, nums, 0)      # 15
# a 从 0 开始: 0+1=1 → 1+2=3 → 3+3=6 → 6+4=10 → 10+5=15
```

## 3. functools 工具箱

```python
from functools import partial, lru_cache, reduce

# partial: 固定部分参数
def call_llm(model, prompt, temperature=0.7):
    return f"{model}: {prompt} (T={temperature})"

call_gpt = partial(call_llm, "gpt-4o", temperature=0.3)
print(call_gpt("Hello"))  # gpt-4o: Hello (T=0.3)

# lru_cache: 函数结果缓存（避免重复计算）
@lru_cache(maxsize=128)
def embed(text: str) -> tuple:
    return tuple(expensive_embedding(text))  # 相同文本只算一次

# cached_property: 惰性属性（Python 3.8+）
from functools import cached_property
class DataLoader:
    @cached_property
    def dataset(self):
        return load_huge_dataset()  # 第一次访问时加载，之后缓存
```

## 4. AI 开发实战

```python
# 1. 数据预处理流水线
raw_texts = ["  Hello World!  ", "Python AI", "", None]

cleaned = (raw_texts
    |> filter(None)                         # 去掉 None
    |> map(str.strip)                        # 去空格
    |> filter(bool)                          # 去掉空字符串
    |> map(str.lower)                        # 小写
    |> list
)
# ['hello world!', 'python ai']

# 2. 批量 Embedding（惰性+缓存）
from functools import lru_cache

@lru_cache(maxsize=10000)
def cached_embed(text: str) -> tuple:
    return tuple(model.encode(text))

embeddings = map(cached_embed, texts)  # 相同文本不会重复计算

# 3. 批量 API 调用统计
from functools import reduce

costs = [calc_cost(r) for r in responses]
total_tokens = reduce(lambda a, b: a + b, costs, 0)
```
## 核心要点回顾

- 推导式 > map/filter（多数场景更可读）
- `map(str.upper, items)` 配合已有函数最优雅
- `partial` = 函数参数预填充（依赖注入的轻量替代）
- `lru_cache` = 免费的性能提升（AI Embedding 场景必备）
- `|>` 管道操作符 (Python 3.12+ 讨论中，可用库实现)

## 参考资料

1. functools 官方文档
2. Python 函数式编程 HOWTO
