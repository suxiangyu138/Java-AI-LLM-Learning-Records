# 05 - 面向对象特性与 MRO

> Python 支持多继承，所以必须解决「菱形继承」——C3 线性化算法和 super() 的协作机制是本篇核心，也是高级面试的分水岭

---

## 📚 目录

1. [类也是对象：type 与 object](#1-类也是对象type-与-object)
2. [属性查找链](#2-属性查找链)
3. [三种方法：实例/类/静态](#3-三种方法实例类静态)
4. [多继承与 MRO 的 C3 线性化](#4-多继承与-mro-的-c3-线性化)
5. [super() 的真正含义](#5-super-的真正含义)
6. [抽象基类与 Protocol](#6-抽象基类与-protocol)
7. [__slots__ 与内存优化](#7-slots-与内存优化)
8. [dataclass 与现代数据类](#8-dataclass-与现代数据类)
9. [核心要点回顾](#9-核心要点回顾)

---

## 1. 类也是对象：type 与 object

Python 里类本身也是对象，它的类型是 `type`：

```python
class A: pass

a = A()
print(type(a))        # <class '__main__.A'>   实例的类型是类
print(type(A))        # <class 'type'>         类的类型是 type
print(type(type))     # <class 'type'>         type 的类型是自己（终点）

print(A.__bases__)    # (<class 'object'>,)    所有类都继承 object
print(type.__bases__) # (<class 'object'>,)
print(object.__bases__)  # ()                  继承链的顶点
```

```text
两条正交的关系链：

  实例化关系（type 链）          继承关系（bases 链）
  ─────────────────────          ────────────────────
      a                                 A
      │ type                            │ 继承
      ▼                                 ▼
      A                              object
      │ type                            │ 继承
      ▼                                 ▼
    type ──┐ type                      ()
      ▲    │
      └────┘（自引用）

  type 是「造类的类」→ 元类；object 是「所有类的基类」
  且 type 也继承自 object，object 的类型也是 type（互相纠缠但合法）
```

用 `type` 三参数形式可以动态造类——这就是元类的基础：

```python
# class B: x = 1  等价于：
B = type("B", (), {"x": 1})
print(B, B.x)              # <class 'B'> 1

# 带方法和基类
C = type("C", (B,), {"show": lambda self: print(self.x)})
C().show()                 # 1
```

> 💡 元类的完整讲解在 [07-装饰器与元编程](./07-装饰器与元编程.md)。这里只需建立认知：**`class` 关键字本质是调用元类（默认 `type`）来创建类对象**。

---

## 2. 属性查找链

`obj.attr` 的查找顺序（简化版，完整版见 [08-描述符](./08-上下文管理器与描述符.md)）：

```text
obj.attr 求值：
  ① type(obj).__mro__ 中查找「数据描述符」（有 __set__/__delete__）
     └─ 找到 → 调用它的 __get__，结束
  ② obj.__dict__ 中查找
     └─ 找到 → 直接返回，结束
  ③ type(obj).__mro__ 中查找（非数据描述符 / 普通类属性）
     └─ 找到描述符 → 调 __get__；找到普通值 → 返回
  ④ 都没找到 → 调用 type(obj).__getattr__(name)（若定义）
  ⑤ 仍无 → 抛 AttributeError
```

```python
class Demo:
    cls_attr = "class"
    def __init__(self):
        self.inst_attr = "instance"
    def __getattr__(self, name):            # 只在常规查找失败时调用
        return f"fallback:{name}"

d = Demo()
print(d.inst_attr)      # instance       ← ② 实例字典
print(d.cls_attr)       # class          ← ③ 类字典
print(d.whatever)       # fallback:whatever  ← ④ 兜底
print(d.__dict__)       # {'inst_attr': 'instance'}
```

```python
# 实例属性遮蔽类属性（shadowing）
class Config:
    debug = False

c1, c2 = Config(), Config()
c1.debug = True             # 写进 c1.__dict__，不影响类属性
print(c1.debug, c2.debug, Config.debug)   # True False False
del c1.debug
print(c1.debug)             # False —— 遮蔽解除，重新看到类属性
```

| 方法 | 触发时机 | 用途 |
|------|---------|------|
| `__getattribute__` | **每次**属性访问 | 拦截一切（易写死循环，慎用） |
| `__getattr__` | 常规查找**失败后** | 动态属性、代理转发、惰性加载 |
| `__setattr__` | 每次属性赋值 | 校验、只读对象、变更追踪 |
| `__delattr__` | 每次 `del obj.x` | 保护关键属性 |
| `__dir__` | `dir(obj)` | 自定义补全列表 |

```python
# 动态属性代理（ORM / 配置对象常见套路）
class DictProxy:
    def __init__(self, data): self._data = data
    def __getattr__(self, name):
        try: return self._data[name]
        except KeyError: raise AttributeError(name) from None

cfg = DictProxy({"host": "localhost", "port": 8080})
print(cfg.host, cfg.port)          # localhost 8080
```

> ⚠️ 在 `__setattr__` 里写 `self.x = v` 会无限递归。必须用 `object.__setattr__(self, 'x', v)` 或 `self.__dict__['x'] = v`。

---

## 3. 三种方法：实例/类/静态

| 类型 | 装饰器 | 首参 | 能访问 | 典型用途 |
|------|-------|------|-------|---------|
| 实例方法 | 无 | `self` | 实例 + 类 | 常规业务逻辑 |
| 类方法 | `@classmethod` | `cls` | 仅类 | 替代构造器、工厂 |
| 静态方法 | `@staticmethod` | 无 | 都不能 | 逻辑相关的工具函数 |

```python
class Model:
    registry = {}

    def __init__(self, name): self.name = name

    def predict(self, x):                       # 实例方法
        return f"{self.name} -> {x}"

    @classmethod
    def from_config(cls, cfg: dict):            # 类方法：替代构造器
        return cls(cfg["name"])                 # cls 支持继承，比写死 Model 好

    @classmethod
    def register(cls, key):                     # 类方法：操作类状态
        cls.registry[key] = cls
        return cls

    @staticmethod
    def validate(x) -> bool:                    # 静态方法：纯工具
        return isinstance(x, (int, float))

m = Model.from_config({"name": "bert"})
print(m.predict(1), Model.validate("a"))        # bert -> 1  False
```

```python
# 为什么类方法比硬编码类名好：继承时自动适配
class Base:
    @classmethod
    def create(cls): return cls()          # ✅ cls 是实际调用的类
    @staticmethod
    def create_bad(): return Base()        # ❌ 子类调用也返回 Base

class Sub(Base): pass
print(type(Sub.create()))        # <class 'Sub'>  ✅
print(type(Sub.create_bad()))    # <class 'Base'> ❌
```

对照 Java：Java 的 `static` 方法 ≈ Python 的 `@staticmethod`，但**Java 没有 `@classmethod` 的等价物**（静态方法里拿不到「实际被调用的子类」）。Python 的 `cls` 让工厂方法天然支持继承。

> 💡 判断依据：**要不要用实例状态 → 用实例方法；要不要用类本身（含子类适配）→ 用类方法；两个都不要 → 用静态方法（甚至考虑挪成模块级函数）**。

---

## 4. 多继承与 MRO 的 C3 线性化

Python 允许多继承，因此必须回答「菱形继承时方法找哪个父类」。答案是 **MRO（Method Resolution Order）**，由 **C3 线性化算法**计算。

### 4.1 菱形继承问题

```python
class A:
    def who(self): print("A")
class B(A):
    def who(self): print("B")
class C(A):
    def who(self): print("C")
class D(B, C):
    pass

D().who()                 # B
print([c.__name__ for c in D.__mro__])
# ['D', 'B', 'C', 'A', 'object']
```

```text
      object
        │
        A
       / \
      B   C
       \ /
        D          ← 菱形（diamond）

  C3 线性化结果：D → B → C → A → object
  注意 A 排在 C 之后（而非 B 之后）——保证「子类总在父类前」
```

### 4.2 C3 算法规则

C3 保证三条性质：

| 性质 | 含义 |
|------|------|
| **局部优先顺序** | 基类在 MRO 中的相对顺序与 `class D(B, C)` 声明顺序一致 |
| **单调性** | 子类的 MRO 是所有父类 MRO 的「一致合并」，不会出现顺序反转 |
| **子类优先** | 任何类都排在它的父类之前 |

计算方式（merge 操作）：

```text
L[D] = D + merge(L[B], L[C], [B, C])

L[B] = [B, A, object]
L[C] = [C, A, object]

merge([B,A,object], [C,A,object], [B,C]):
  取 B ── B 是首元素且不出现在其他列表的「尾部」 → 收下 B
  剩 merge([A,object], [C,A,object], [C])
  取 A ── ❌ A 出现在 [C,A,object] 的尾部 → 跳过
  取 C ── ✅ → 收下 C
  剩 merge([A,object], [A,object], [])
  取 A ── ✅ → 收下 A
  取 object ── ✅
结果：L[D] = [D, B, C, A, object]
```

```python
# 无法线性化的情况会直接报错
class X: pass
class Y: pass
class M(X, Y): pass
class N(Y, X): pass
# class Z(M, N): pass
#   → TypeError: Cannot create a consistent method resolution order (MRO)
#   因为 M 要求 X 在 Y 前，N 要求 Y 在 X 前，矛盾
```

```python
# 查看 MRO 的三种方式
print(D.__mro__)              # tuple
print(D.mro())                # list
import inspect
print(inspect.getmro(D))
```

> 🎯 面试高频：给一段多继承代码问 MRO。**实操技巧**：先画继承图，按「深度优先 + 子类优先 + 遇到共同父类就往后推」手推，然后一定用 `__mro__` 验证。

---

## 5. super() 的真正含义

**最大的误解**：`super()` 不是「调用父类」，而是「**按 MRO 顺序调用下一个类**」。

```python
class A:
    def go(self): print("A.go")
class B(A):
    def go(self): print("B.go"); super().go()
class C(A):
    def go(self): print("C.go"); super().go()
class D(B, C):
    def go(self): print("D.go"); super().go()

D().go()
# D.go
# B.go
# C.go     ← B 里的 super() 跳到了 C，不是 A！
# A.go
```

关键点：`B.go` 里的 `super()` 在 `D()` 的调用链上指向 **C**，因为 MRO 是 `D→B→C→A`。同一份 `B` 代码，在不同 MRO 下 `super()` 指向不同的类——**super 依赖运行时的实例类型，不是编译期的父类**。

```python
# super() 的完整形式揭示了原理
class B(A):
    def go(self):
        super().go()                  # 隐式，等价于下一行
        super(B, type(self).__mro__ and self).go()   # 概念上：
        # super(当前类, 实例) → 在 type(实例).__mro__ 里找「当前类」之后的下一个
```

| 形式 | 含义 | 版本 |
|------|------|------|
| `super()` | 零参形式，编译器自动填 `(__class__, self)` | Py3 推荐 |
| `super(B, self)` | 显式：在 `type(self).__mro__` 中找 B 之后的类 | Py2 兼容 |
| `super(B, C)` | 类绑定：在 `C.__mro__` 中找 B 之后（用于类方法） | 少见 |

### 5.1 协作式多继承（Cooperative Multiple Inheritance）

要让多继承正常工作，**每个类都必须调用 `super()`**，形成完整链条：

```python
# ✅ 正确：全链路调用 super，且签名用 **kwargs 兼容
class Base:
    def __init__(self, **kwargs):
        super().__init__(**kwargs)          # 最终到 object.__init__()
        print("Base init")

class LoggerMixin:
    def __init__(self, log_level="INFO", **kwargs):
        super().__init__(**kwargs)          # ← 必须转发！
        self.log_level = log_level

class CacheMixin:
    def __init__(self, cache_size=100, **kwargs):
        super().__init__(**kwargs)
        self.cache_size = cache_size

class Service(LoggerMixin, CacheMixin, Base):
    def __init__(self, name, **kwargs):
        super().__init__(**kwargs)
        self.name = name

s = Service("api", log_level="DEBUG", cache_size=50)
print(s.name, s.log_level, s.cache_size)    # api DEBUG 50
print([c.__name__ for c in Service.__mro__])
# ['Service', 'LoggerMixin', 'CacheMixin', 'Base', 'object']
```

```python
# ❌ 断链：某个类不调 super()，后面的类全部被跳过
class BadMixin:
    def __init__(self, **kwargs):
        pass                    # 没有 super().__init__() → CacheMixin/Base 不会执行
```

### 5.2 Mixin 设计规范

| 规范 | 原因 |
|------|------|
| Mixin 类名以 `Mixin` 结尾 | 表明它不能独立实例化 |
| Mixin 不定义 `__init__` 状态（或用 `**kwargs` 转发） | 避免破坏 MRO 链 |
| Mixin 放在**基类之前**：`class C(Mixin1, Mixin2, Base)` | MRO 里 Mixin 优先，才能覆写 Base 行为 |
| Mixin 只提供方法，不做深继承 | 保持扁平，避免 MRO 冲突 |

> ⚠️ 常见 bug：把 Mixin 写在基类后面（`class C(Base, Mixin)`），结果 Mixin 的方法被 Base 遮蔽，「不生效」。记住 **MRO 从左到右**，想覆写就往左放。

---

## 6. 抽象基类与 Protocol

两条路线，分别对应「名义子类型」和「结构化子类型」：

### 6.1 ABC：显式继承 + 强制实现

```python
from abc import ABC, abstractmethod

class Retriever(ABC):
    @abstractmethod
    def search(self, query: str, top_k: int = 5) -> list[str]:
        """必须实现"""

    @abstractmethod
    def index(self, docs: list[str]) -> None: ...

    def search_one(self, query: str) -> str | None:   # 可提供默认实现
        r = self.search(query, top_k=1)
        return r[0] if r else None

# r = Retriever()   # ❌ TypeError: Can't instantiate abstract class

class BM25Retriever(Retriever):
    def search(self, query, top_k=5): return ["doc1"]
    def index(self, docs): pass

print(BM25Retriever().search_one("q"))    # doc1
```

```python
# 其他抽象成员
from abc import ABC, abstractmethod
class Cfg(ABC):
    @property
    @abstractmethod
    def name(self) -> str: ...            # 抽象属性（顺序：property 在外层）

    @classmethod
    @abstractmethod
    def load(cls, path): ...              # 抽象类方法
```

### 6.2 Protocol：鸭子类型 + 静态检查

```python
from typing import Protocol, runtime_checkable

@runtime_checkable
class Embedder(Protocol):
    def embed(self, text: str) -> list[float]: ...

class OpenAIEmbedder:                     # 不继承 Protocol！
    def embed(self, text): return [0.1, 0.2]

def build_index(e: Embedder, docs: list[str]):    # mypy 会检查
    return [e.embed(d) for d in docs]

build_index(OpenAIEmbedder(), ["a"])              # ✅ 结构匹配即可
print(isinstance(OpenAIEmbedder(), Embedder))     # True（需 runtime_checkable）
```

| 维度 | ABC | Protocol |
|------|-----|----------|
| 是否需要继承 | ✅ 必须 | ❌ 不需要 |
| 检查时机 | 运行时（实例化时） | 静态（mypy），可选运行时 |
| 类型系统 | 名义子类型（像 Java interface） | 结构化子类型（真鸭子类型） |
| 能否给第三方类「补接口」 | ❌ 需 `register()` | ✅ 天然支持 |
| 适用场景 | 自己项目内的框架骨架 | 对接外部库、松耦合边界 |

```python
# ABC 的虚拟子类：给无法修改的第三方类注册
from collections.abc import Sized
class ThirdParty:
    def __len__(self): return 0
Sized.register(ThirdParty)
print(issubclass(ThirdParty, Sized))     # True
```

> 🎯 选型建议：**框架内部的必须实现约束用 ABC**（能在实例化时立刻报错），**跨库边界、只想描述「需要什么能力」用 Protocol**。AI 项目里对接多家模型 SDK 时，Protocol 更省心。

---

## 7. __slots__ 与内存优化

默认每个实例都带一个 `__dict__` 字典存属性，开销不小。`__slots__` 把属性改为固定槽位：

```python
class PointDict:
    def __init__(self, x, y): self.x, self.y = x, y

class PointSlots:
    __slots__ = ("x", "y")
    def __init__(self, x, y): self.x, self.y = x, y

import sys
p1, p2 = PointDict(1, 2), PointSlots(1, 2)
print(sys.getsizeof(p1) + sys.getsizeof(p1.__dict__))   # 56 + 104 ≈ 160 字节
print(sys.getsizeof(p2))                                # 48 字节
print(hasattr(p2, "__dict__"))                          # False

# p2.z = 3    # ❌ AttributeError: 'PointSlots' object has no attribute 'z'
```

| 维度 | 有 `__dict__`（默认） | 用 `__slots__` |
|------|---------------------|---------------|
| 内存 | 大（每实例一个 dict） | 小（约省 40-60%） |
| 属性访问速度 | 字典查找 | 数组偏移，略快 |
| 能否动态加属性 | ✅ | ❌ |
| 能否被 `weakref` 引用 | ✅ | 需显式加 `"__weakref__"` |
| 多继承 | 无限制 | 多个父类都有非空 slots 会冲突 |
| 类属性同名 | 允许 | ❌ ValueError |

```python
# 继承时的注意点
class Base:
    __slots__ = ("a",)
class Child(Base):
    __slots__ = ("b",)          # 只声明新增的，不要重复写 a
print(Child.__slots__, Base.__slots__)     # ('b',) ('a',)

class Leaky(Base):
    pass                         # 没声明 __slots__ → 自动获得 __dict__，优化失效
print(hasattr(Leaky(), "__dict__"))        # True
```

> 💡 使用门槛：**只有当实例数量达到十万级以上、且属性固定时才值得用**。数据量小的业务对象用 `__slots__` 是过度优化，反而牺牲灵活性。典型适用场景：图节点、粒子系统、大批量数据记录。

---

## 8. dataclass 与现代数据类

Python 3.7+ 的 `@dataclass` 自动生成 `__init__`/`__repr__`/`__eq__`，相当于 Java 的 record + Lombok：

```python
from dataclasses import dataclass, field, asdict, replace

@dataclass
class ModelConfig:
    name: str                                  # 必填
    hidden_size: int = 768                     # 有默认值
    layers: list[int] = field(default_factory=list)   # ✅ 可变默认值的正确写法
    _cache: dict = field(default_factory=dict, repr=False, compare=False)

    def __post_init__(self):                   # 校验/派生字段
        if self.hidden_size % 64 != 0:
            raise ValueError("hidden_size 必须是 64 的倍数")

c = ModelConfig("bert", layers=[12])
print(c)                    # ModelConfig(name='bert', hidden_size=768, layers=[12])
print(c == ModelConfig("bert", layers=[12]))   # True  ← 自动 __eq__
print(asdict(c))            # 转 dict（递归）
print(replace(c, hidden_size=1024))            # 基于现有实例创建修改版
```

```python
# 关键参数
@dataclass(frozen=True)     # 不可变 + 自动生成 __hash__ → 可做 dict key
class Point:
    x: int
    y: int

@dataclass(order=True)      # 生成 __lt__/__le__/__gt__/__ge__（按字段顺序比较）
class Version:
    major: int
    minor: int

@dataclass(slots=True)      # 3.10+：自动加 __slots__
class Fast:
    a: int

@dataclass(kw_only=True)    # 3.10+：所有字段变仅关键字参数
class Cfg:
    name: str
    debug: bool = False
```

> ⚠️ `field(default_factory=list)` 是必须的——直接写 `layers: list = []` 会被 dataclass 拒绝（它主动帮你避开了 [04 章的可变默认值陷阱](./04-函数特性深度解析.md#4-默认参数可变陷阱)）。

### 8.1 四种「数据容器」选型

| 方案 | 可变 | 类型校验 | 性能 | 适用 |
|------|:----:|:-------:|:----:|------|
| `dict` | ✅ | ❌ | 快 | 临时数据、JSON 直通 |
| `NamedTuple` | ❌ | 仅注解 | 最快、最省内存 | 不可变的小记录、函数多返回值 |
| `@dataclass` | 可选 | 仅注解（不强制） | 快 | **默认选择**：业务实体、配置 |
| `Pydantic BaseModel` | ✅ | ✅ 运行时强制 | 较慢（v2 已大幅优化） | 外部输入校验、API 边界 |

```python
from typing import NamedTuple
class Result(NamedTuple):           # 不可变、可解包、有字段名
    label: str
    score: float
r = Result("cat", 0.9)
label, score = r                    # 支持解包
print(r.label, r[0], r._asdict())
```

```python
from pydantic import BaseModel, Field
class Request(BaseModel):           # 运行时强校验，FastAPI 的基础
    prompt: str = Field(min_length=1, max_length=4096)
    temperature: float = Field(0.7, ge=0.0, le=2.0)
# Request(prompt="", temperature=5)  → ValidationError（两个字段都不合法）
```

> 🎯 决策路径：**内部数据结构用 dataclass，外部输入用 Pydantic，只读小记录用 NamedTuple**。dataclass 的注解不做运行时校验，别指望它挡住脏数据。

---

## 9. 核心要点回顾

- 类也是对象，类型是 `type`；`type(name, bases, dict)` 能动态造类
- 属性查找顺序：**数据描述符 → 实例 `__dict__` → 类 MRO → `__getattr__`**
- `__getattr__` 只在常规查找失败时触发，`__getattribute__` 拦截一切
- `@classmethod` 的 `cls` 让工厂方法**自动适配子类**，这是 Java static 做不到的
- 多继承靠 **C3 线性化**算出 MRO，保证「局部顺序 + 单调性 + 子类优先」
- **`super()` 是「MRO 里的下一个」，不是「父类」**——协作式多继承要求全链路调 `super`
- Mixin 必须放在基类**左边**，否则方法被遮蔽
- **ABC = 名义子类型（强制继承 + 实例化时报错）；Protocol = 结构化子类型（静态检查）**
- `__slots__` 省内存但禁止动态属性，只在**十万级实例**时才值得
- 数据容器选型：dataclass（内部）／Pydantic（外部输入）／NamedTuple（不可变记录）

---

## 参考资料

| 类型 | 名称 |
|------|------|
| 文章 | Python 2.3 Method Resolution Order（官方 C3 说明） |
| PEP | PEP 3119（ABC）、PEP 544（Protocol）、PEP 557（dataclass）、PEP 681 |
| 书籍 | 《Fluent Python》第 12-14 章 |
| 模块 | `abc`、`dataclasses`、`typing`、`collections.abc` |

---

**上一模块**：[04 函数特性深度解析](./04-函数特性深度解析.md) ／ **下一模块**：[06 迭代协议与生成器](./06-迭代协议与生成器.md) ／ **返回总览**：[00 总览](./00-Python语言特性知识体系总览.md)
