# 05 - Python 对象模型与魔术方法

> 🎯 Python 的"魔法"全部来自魔术方法 — `obj[key]` 的背后是 `__getitem__`，`obj()` 的背后是 `__call__`，属性访问的背后是描述符协议。理解对象模型 = 理解 Python 的一切

---

## 目录

1. [对象模型三层结构](#1-对象模型三层结构)
2. [魔术方法速查总表（按功能分类）](#2-魔术方法速查总表按功能分类)
3. [__getitem__ / __call__ / __iter__ 详解](#3-__getitem__--__call__--__iter__-详解)
4. [描述符协议](#4-描述符协议)
5. [元类 — 99% 时候不需要](#5-元类--99-时候不需要)
6. [对象创建全链路](#6-对象创建全链路)

---

## 1. 对象模型三层结构

```text
实例 (instance) → 类 (class) → 元类 (metaclass)
   obj              MyClass         type
   obj.attr 解析路径：obj.__dict__ → MyClass.__dict__ → 父类链 → __getattr__
   类也是对象：type(MyClass) is type、type(type) is type（自举）
```

| 层 | 例子 | 由谁创建 |
|----|------|----------|
| 实例 | `obj = MyClass()` | 类的 `__call__`（实际是元类 `type.__call__`） |
| 类 | `class MyClass: ...` | 元类的 `__call__`（默认是 `type`） |
| 元类 | `class Meta(type): ...` | `type` 自身 |

---

## 2. 魔术方法速查总表（按功能分类）

### 2.1 对象生命周期

| 方法 | 触发 | 说明 |
|------|------|------|
| `__new__(cls)` | `MyClass()` | **创建实例**（在 __init__ 之前，返回实例） |
| `__init__(self)` | `MyClass()` | **初始化实例**（不返回、只设置属性） |
| `__del__(self)` | `del obj` / GC | 析构（不保证立即调用！用 context manager 替代） |

### 2.2 属性访问（描述符协议）

| 方法 | 触发 | 说明 |
|------|------|------|
| `__getattr__(self, name)` | `obj.attr` 且普通查找失败 | 后门、延迟加载 |
| `__getattribute__(self, name)` | **每次** `obj.attr` | 全能拦截（慎用，易死循环） |
| `__setattr__(self, name, value)` | `obj.attr = value` | 设置属性 |
| `__delattr__(self, name)` | `del obj.attr` | 删除属性 |
| `__get__` / `__set__` / `__delete__` | 类属性访问 | **描述符协议**（见 4 节） |

### 2.3 容器行为

| 方法 | 触发 | 说明 |
|------|------|------|
| `__len__(self)` | `len(obj)` | 长度 |
| `__getitem__(self, key)` | `obj[key]` | 索引/切片/迭代（for 循环依赖它） |
| `__setitem__(self, key, val)` | `obj[key] = val` | 设置 |
| `__delitem__(self, key)` | `del obj[key]` | 删除 |
| `__contains__(self, item)` | `item in obj` | 成员检查 |
| `__iter__(self)` | `for x in obj` / `iter(obj)` | 迭代器（优先于 __getitem__） |
| `__next__(self)` | `next(obj)` | 迭代的下一个元素 |

### 2.4 可调用与运算符

| 方法 | 触发 | 说明 |
|------|------|------|
| `__call__(self, *args)` | `obj()` | 实例当函数调用 |
| `__eq__` / `__ne__` / `__lt__` / `__gt__` | `==` `!=` `<` `>` | 比较 |
| `__hash__(self)` | `hash(obj)` / `set` / `dict key` | 哈希（与 __eq__ 联动：eq 相等→hash 必须相等） |
| `__bool__(self)` | `bool(obj)` / `if obj:` | 真值判断 |

### 2.5 字符串表示

| 方法 | 触发 | 说明 |
|------|------|------|
| `__repr__(self)` | `repr(obj)` / 控制台裸输 | 给开发者看（"精确"） |
| `__str__(self)` | `str(obj)` / `print(obj)` | 给用户看（"友好"） |
| `__format__(self, spec)` | `f"{obj:spec}"` | 格式化 |

### 2.6 上下文管理器与运算符重载

| 方法 | 触发 | 说明 |
|------|------|------|
| `__enter__` / `__exit__` | `with obj:` | 上下文管理器 |
| `__add__` / `__sub__` / `__mul__` | `+` `-` `*` | 算数运算 |
| `__iadd__` | `+=` | 原地加法（不实现则退化为 __add__） |

---

## 3. __getitem__ / __call__ / __iter__ 详解

### 3.1 __getitem__：让对象支持 obj[key]

```python
class Dataset:
    """只需实现 __getitem__ 和 __len__ → 自动支持 for/in/切片/随机选取"""
    def __init__(self, data): self.data = data
    def __getitem__(self, idx):
        if isinstance(idx, slice):
            return Dataset(self.data[idx])   # 切片返回同类型
        return self.data[idx]
    def __len__(self): return len(self.data)

ds = Dataset([1, 2, 3, 4, 5])
ds[0]      # OK、ds[1:3]   # OK（切片）
for x in ds: ...            # OK（__getitem__ 支持迭代）
```

### 3.2 __call__：让实例像函数

```python
class Retry:
    """装饰器类：@Retry(max_retries=5) 为 LLM API 调用加自动重试"""
    def __init__(self, max_retries=3, delay=0.5):
        self.max_retries = max_retries
        self.delay = delay
    def __call__(self, func):
        @wraps(func)
        def wrapper(*a, **kw):
            for i in range(self.max_retries):
                try: return func(*a, **kw)
                except Exception as e:
                    if i == self.max_retries - 1: raise
                    time.sleep(self.delay * (2 ** i))
        return wrapper

@Retry(max_retries=5, delay=1.0)
def call_llm(prompt): ...       # 自动重试！
```

### 3.3 __iter__ + __next__：迭代器协议

```python
class ChunkedReader:
    """批量读取大文件 → 内存友好的 AI 数据处理"""
    def __init__(self, path, chunk_size=100):
        self.f = open(path)
        self.chunk_size = chunk_size
    def __iter__(self): return self
    def __next__(self):
        lines = [self.f.readline() for _ in range(self.chunk_size)]
        lines = [l for l in lines if l]   # 过滤空行
        if not lines: raise StopIteration
        return lines
    def __del__(self): self.f.close()
```

---

## 4. 描述符协议

**实现了 `__get__`/`__set__`/`__delete__` 中任一方法的类 = 描述符。它的实例作为另一个类的类属性时，能拦截该属性的访问。**

```python
class ValidatedParameter:
    """AI 场景：自动校验 LLM 超参数（temperature/max_tokens/top_p）"""
    def __init__(self, name, min_val, max_val, dtype=float):
        self.name = name
        self.min_val = min_val
        self.max_val = max_val
        self.dtype = dtype

    def __set_name__(self, owner, name):    # Python 3.6+ 自动绑定字段名
        self.name = name

    def __get__(self, obj, objtype=None):
        if obj is None: return self         # 类级别访问返回描述符本身
        return obj.__dict__.get(self.name)

    def __set__(self, obj, value):
        if not isinstance(value, self.dtype):
            raise TypeError(f"{self.name} 必须是 {self.dtype}")
        if value < self.min_val or value > self.max_val:
            raise ValueError(f"{self.name} 必须在 [{self.min_val}, {self.max_val}] 之间")
        obj.__dict__[self.name] = value

class LLMConfig:
    temperature = ValidatedParameter("temperature", 0.0, 2.0, float)
    max_tokens = ValidatedParameter("max_tokens", 1, 8192, int)

cfg = LLMConfig()
cfg.temperature = 1.0     # ✅
cfg.temperature = 3.0     # ❌ ValueError
```

**描述符优先级：** 数据描述符（定义了 `__set__`）> 实例 `__dict__` > 非数据描述符（仅 `__get__`）。

---

## 5. 元类 — 99% 时候不需要

```python
# 元类 = 创建类的类
class PluginMeta(type):
    """AI 场景：自动注册所有 LLM 插件"""
    registry = {}
    def __new__(cls, name, bases, attrs):
        new_cls = super().__new__(cls, name, bases, attrs)
        if name != "BasePlugin":
            cls.registry[name] = new_cls
        return new_cls

class BasePlugin(metaclass=PluginMeta): pass

class GPTPlugin(BasePlugin): ...       # 自动注册到 PluginMeta.registry
class ClaudePlugin(BasePlugin): ...    # 自动注册
```

> ⚠️ Python 核心开发者："99% 的时间里你不需要元类。能用装饰器、继承、普通函数解决的，不要用元类。"

---

## 6. 对象创建全链路

```text
MyClass(*args)
  → type.__call__(MyClass, *args)       # 元类的 __call__ 被触发
     → MyClass.__new__(MyClass)          # 创建实例（返回空壳）
     → MyClass.__init__(instance, *args) # 初始化实例
     → 返回实例
```

**单例模式（利用 __new__）：**

```python
class Singleton:
    _instance = None
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance  # 永远返回同一个实例
```

---

> 🎯 **核心要点**：Python 对象模型记忆法 — **① 运行时行为 = 魔术方法（obj[key]→__getitem__、obj()→__call__）② 属性控制 = 描述符（ORM/校验的底层机制）③ 类创建 = 元类（99%不需要）**。AI 开发中最实用的三个：`__call__` 做可调用装饰器、`__getitem__` 做自定义 Dataset、描述符做参数校验。

**下一模块**：[06-并发与异步编程模块](06-并发与异步编程模块.md) / **返回总览**：[00-总览](00-Python模块对象知识体系总览.md)
