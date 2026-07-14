# 阶段 2：进阶语法与工程基础

> **目标**：写的 Python 不再是"随手脚本"，而是可维护的工程代码
> **核心**：OOP（与 Java 的异同）、异常体系、虚拟环境、包管理
> **产出**：能创建有结构的小型 Python 项目

---

## 📌 章节定位

阶段 1 让你会写 Python，阶段 2 让你会**组织** Python 代码。Python 的工程化思路和 Java 有很大差异——它更轻量、更灵活，但也有自己的最佳实践。

---

## 🎯 核心章节

### 1. 面向对象编程（与 Java 的对比）

**🔑 核心差异**：Python OOP 比 Java 更灵活、约定大于强制。

| 特性 | Java | Python |
|------|------|--------|
| 构造函数 | 与类同名 | `__init__(self)` |
| this/self | `this`（隐式） | `self`（**必须显式写**） |
| 私有成员 | `private` 关键字 | `_name`（约定） / `__name`（名称改写） |
| 方法重载 | ✅ 支持 | ❌ 不支持 |
| 多继承 | ❌（接口多实现） | ✅（MRO 菱形继承） |
| 抽象类 | `abstract class` | `ABC` + `@abstractmethod` |
| 接口 | `interface` | 鸭子类型 / `Protocol`（3.8+） |
| 静态方法 | `static` | `@staticmethod` / `@classmethod` |
| getter/setter | 手写或 Lombok | `@property` 装饰器 |

```python
class Employee:
    """
    员工类 —— 一个典型的 Python 类示例。
    """
    # 类变量（相当于 Java static 变量）
    company = "ACME Corp"
    _total_count = 0           # 约定：受保护成员

    def __init__(self, name: str, salary: float = 0.0):
        """
        构造函数（⭐ self 必须显式写，相当于 Java 的 this）
        """
        self.name = name       # 实例变量（动态创建，无需预先声明！）
        self._salary = salary  # 约定：受保护属性
        Employee._total_count += 1

    # --- 属性装饰器（⭐ Python 特色的 getter/setter）---
    @property
    def salary(self) -> float:
        """只读属性 —— 像访问字段一样调用方法"""
        return self._salary

    @salary.setter
    def salary(self, value: float):
        if value < 0:
            raise ValueError("工资不能为负")
        self._salary = value

    # --- 魔术方法 ---
    def __str__(self) -> str:
        """print(obj) 时调用（相当于 Java toString）"""
        return f"Employee({self.name}, {self._salary})"

    def __repr__(self) -> str:
        """REPL 和调试时调用 —— 应返回可重建对象的字符串"""
        return f"Employee(name={self.name!r}, salary={self._salary!r})"

    def __eq__(self, other) -> bool:
        """== 比较（相当于 Java equals）"""
        if not isinstance(other, Employee):
            return NotImplemented
        return self.name == other.name and self._salary == other._salary

    def __hash__(self) -> int:
        """使对象可作为 dict key 或 set 元素"""
        return hash((self.name, self._salary))

    # --- 类方法与静态方法 ---
    @classmethod
    def from_string(cls, s: str) -> "Employee":
        """工厂方法：从字符串解析创建（cls 指向类本身）"""
        name, salary = s.split(",")
        return cls(name.strip(), float(salary.strip()))

    @staticmethod
    def is_valid_name(name: str) -> bool:
        """静态工具方法：不需要访问实例或类"""
        return bool(name and name.strip())

    @classmethod
    def total_count(cls) -> int:
        return cls._total_count


# 使用示例
emp = Employee("Alice", 50000)
print(emp.salary)            # 50000.0  （像属性一样访问）
emp.salary = 60000           # 调用 setter，含验证逻辑
print(emp)                   # Employee(Alice, 60000.0)
emp2 = Employee.from_string("Bob, 45000")  # 工厂方法
```

#### 继承与多态

```python
# 继承
class Manager(Employee):
    def __init__(self, name: str, salary: float, department: str):
        super().__init__(name, salary)  # ⭐ 必须显式调用 super()
        self.department = department

    def __str__(self):
        return f"Manager({self.name}, {self.department})"

# 多继承与 MRO（方法解析顺序）
class A:
    def method(self): return "A"

class B(A):
    def method(self): return "B"

class C(A):
    def method(self): return "C"

class D(B, C):   # MRO: D → B → C → A
    pass

d = D()
print(d.method())           # "B"  （广度优先 + C3 线性化）
print(D.__mro__)            # 查看解析顺序
```

#### 访问控制详解

```python
class Demo:
    def __init__(self):
        self.public = 1           # 公开
        self._protected = 2       # 约定：受保护（仍可访问，靠自觉）
        self.__private = 3        # 名称改写为 _Demo__private

    def __private_method(self):   # 名称改写为 _Demo__private_method
        pass

d = Demo()
print(d.public)                   # 1
print(d._protected)               # 2  （能访问，但不应该）
# print(d.__private)              # AttributeError!
print(d._Demo__private)           # 3  （名称改写，仍可绕过）
```

### 2. 异常处理

```python
# 基本结构（比 Java 少了 throws 声明）
try:
    result = 10 / 0
except ZeroDivisionError as e:           # ⚠️ 捕获具体异常，不要裸 except
    print(f"除零错误: {e}")
except (TypeError, ValueError) as e:     # 一次捕获多种
    print(f"类型或值错误: {e}")
except Exception as e:                    # 兜底（不推荐在顶层用）
    print(f"未知错误: {e}")
else:                                     # ⭐ Java 没有：try 成功时执行
    print(f"结果: {result}")
finally:                                  # 无论是否异常都执行
    print("清理资源")

# 自定义异常
class BusinessError(Exception):
    """业务异常基类"""
    def __init__(self, message: str, code: int = 500):
        super().__init__(message)
        self.code = code

# 抛出异常
raise BusinessError("用户不存在", code=404)

# ⭐ Python 3.11+：异常组与 except*
try:
    raise ExceptionGroup("issues", [
        ValueError("bad value"),
        TypeError("bad type"),
    ])
except* ValueError as e:
    print(f"处理所有 ValueError: {e.exceptions}")
except* TypeError as e:
    print(f"处理所有 TypeError: {e.exceptions}")
```

### 3. 上下文管理器（with 语句）

```python
# ⭐ Python 核心模式：确保资源正确释放（相当于 Java try-with-resources）
# 传统写法
f = open("data.txt", "r")
try:
    content = f.read()
finally:
    f.close()

# Python 写法 —— 自动调用 close()
with open("data.txt", "r") as f:
    content = f.read()
# 缩进块结束后自动释放，即使发生异常

# 自定义上下文管理器（协议：__enter__ + __exit__）
class Timer:
    """一个计时上下文管理器"""
    def __enter__(self):
        import time
        self.start = time.perf_counter()
        return self                        # return 的值赋给 as 后的变量

    def __exit__(self, exc_type, exc_val, exc_tb):
        import time
        elapsed = time.perf_counter() - self.start
        print(f"耗时: {elapsed:.3f}s")
        return False                       # False = 不吞异常

with Timer():
    sum(range(10**7))

# 使用 contextlib 更简洁
from contextlib import contextmanager

@contextmanager
def timer():
    import time
    start = time.perf_counter()
    yield                                  # yield 前 = __enter__，后 = __exit__
    print(f"耗时: {time.perf_counter() - start:.3f}s")
```

### 4. 文件 I/O

```python
# 文本读写
with open("input.txt", "r", encoding="utf-8") as f:
    content = f.read()                     # 一次性读入（小文件）
    # line = f.readline()                  # 逐行读
    # lines = f.readlines()                # 读入列表
    # for line in f:                       # ⭐ 逐行迭代（大文件推荐）

with open("output.txt", "w", encoding="utf-8") as f:
    f.write("Hello\n")
    f.writelines(["line1\n", "line2\n"])

# 文件模式速查
# r  = 只读（文件必须存在）
# w  = 只写（清空已有内容）
# a  = 追加
# x  = 排他创建（文件存在则报错）
# b  = 二进制模式（rb, wb）
# +  = 读写模式（r+, w+）
# t  = 文本模式（默认）

# 二进制读写（图片、模型文件等）
with open("image.png", "rb") as f:
    data = f.read()
```

### 5. 虚拟环境与依赖管理

**🔑 这是 Python 工程化的第一课**。Python 项目必须使用虚拟环境隔离依赖。

```bash
# --- venv（Python 内置，⭐ 推荐首选）---
python -m venv .venv              # 创建虚拟环境（.venv 是社区约定目录名）
source .venv/bin/activate         # Linux/Mac 激活
# .venv\Scripts\activate          # Windows 激活
deactivate                        # 退出虚拟环境

# --- pip 基本操作 ---
pip install package_name           # 安装包
pip install package==1.2.3         # 指定版本
pip install "package>=1.0,<2.0"   # 版本范围
pip install -r requirements.txt   # 从文件批量安装
pip freeze > requirements.txt     # 导出当前环境依赖
pip list                          # 列出已安装的包
pip show package_name             # 查看包详情
pip uninstall package_name        # 卸载
```

#### 现代依赖管理工具对比

| 工具 | 特点 | 适用场景 |
|------|------|---------|
| `pip + requirements.txt` | 最简单，Python 内置 | 简单项目、脚本 |
| `pip + pyproject.toml` | 现代标准，PEP 621 | **⭐ 推荐**，中大型项目 |
| `Poetry` | 依赖解析 + 锁定 + 打包 | 复杂依赖项目 |
| `pipenv` | Pipfile + Pipfile.lock | 应用开发 |
| `conda/mamba` | 管理非 Python 依赖 | 数据科学 / AI（CUDA 等） |
| `uv` | Rust 实现，极快 | 追求速度，新一代工具 |

```toml
# pyproject.toml 示例（PEP 621 标准，⭐ 现代项目标配）
[build-system]
requires = ["setuptools>=68.0"]
build-backend = "setuptools.backends._legacy:_Backend"

[project]
name = "my-ai-service"
version = "0.1.0"
description = "Python AI 推理服务"
requires-python = ">=3.10"
dependencies = [
    "numpy>=1.24",
    "pandas>=2.0",
    "fastapi>=0.100",
    "uvicorn[standard]>=0.23",
]

[project.optional-dependencies]
dev = ["pytest>=7.0", "ruff>=0.1", "mypy>=1.0"]
ai = ["torch>=2.0", "transformers>=4.30"]

[tool.ruff]
line-length = 100
target-version = "py310"
```

### 6. 包结构与项目布局

```
my-python-project/
├── pyproject.toml              # 项目元数据与依赖（PEP 621）
├── requirements.txt            # 或 requirements/*.txt
├── README.md
├── LICENSE
├── .gitignore
├── .venv/                      # 虚拟环境（不提交到 Git）
│
├── src/                        # ⭐ 推荐：src 布局
│   └── my_package/             # 包目录（下划线命名）
│       ├── __init__.py         # 标记为包（可以为空）
│       ├── __main__.py         # python -m my_package 的入口
│       ├── core.py
│       ├── utils/
│       │   ├── __init__.py
│       │   └── helpers.py
│       └── api/
│           ├── __init__.py
│           └── routes.py
│
├── tests/                      # 测试目录
│   ├── __init__.py
│   ├── conftest.py             # pytest 配置
│   ├── test_core.py
│   └── test_utils/
│       └── test_helpers.py
│
├── scripts/                    # 辅助脚本
├── docs/                       # 文档
└── notebooks/                  # Jupyter notebooks（探索/演示用）
```

```python
# __init__.py 的典型用法
# my_package/__init__.py
"""My AI Service - Python AI 推理服务包。"""

__version__ = "0.1.0"
__author__ = "Your Name"

# 控制 from my_package import * 的行为
__all__ = ["core", "api"]

# 可以在 __init__.py 中做便捷导入
from .core import Config, Pipeline
```

### 7. 装饰器（⭐ Python 标志性高级特性）

```python
# 装饰器本质：接受函数，返回新函数
import functools
import time

# 一个计时装饰器
def timer(func):
    """测量函数执行时间的装饰器。"""
    @functools.wraps(func)       # ⭐ 保留原函数的元信息
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"{func.__name__} 耗时: {elapsed:.4f}s")
        return result
    return wrapper

@timer                           # 等价于 slow_function = timer(slow_function)
def slow_function(n):
    return sum(range(n))

# 带参数的装饰器（三层嵌套）
def retry(max_attempts: int = 3, delay: float = 1.0):
    """重试装饰器。"""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            import time
            for attempt in range(max_attempts):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    if attempt == max_attempts - 1:
                        raise
                    print(f"重试 {attempt + 1}/{max_attempts}: {e}")
                    time.sleep(delay)
            return None
        return wrapper
    return decorator

@retry(max_attempts=3, delay=0.5)
def call_external_api(url: str):
    # ... 可能失败的网络调用
    pass
```

### 8. 生成器与迭代器

```python
# ⭐ 生成器函数 —— 用 yield 代替 return，惰性产出值
def fibonacci(n: int):
    """生成前 n 个斐波那契数（惰性，节省内存）。"""
    a, b = 0, 1
    for _ in range(n):
        yield a                      # ⭐ yield 而非 return
        a, b = b, a + b

# 逐个消费（内存中同时只保存当前值）
for num in fibonacci(100):
    print(num)

# 生成器表达式（语法类似列表推导式，但用小括号）
sum_of_squares = (x**2 for x in range(10**9))  # 不会立即创建 10 亿个元素！
# next(sum_of_squares) 逐个取值

# 双重收益：可迭代 + 节省内存
import sys
nums_list = [x for x in range(1000)]
nums_gen = (x for x in range(1000))
print(sys.getsizeof(nums_list))   # ≈ 9000 bytes
print(sys.getsizeof(nums_gen))    # ≈ 200 bytes
```

### 9. 类型注解（Type Hints）

```python
# Python 3.5+ 支持类型注解（运行时不做检查，类似注释）
from typing import Optional, Union, Callable, TypeVar, Generic

# 基本类型注解
def greet(name: str, age: int = 0) -> str:
    return f"Hello {name}, you are {age}"

# Optional = 可以是 None
def find_user(id: int) -> Optional[dict]:
    """返回用户字典或 None。"""
    ...

# Union = 多种类型之一
def process(data: Union[str, bytes]) -> str:
    ...

# Python 3.10+：用 | 替代 Union 和 Optional
def find_user_v2(id: int) -> dict | None:        # 更简洁
    ...

def process_v2(data: str | bytes) -> str:
    ...

# Callable 用于函数类型
def execute(action: Callable[[int, int], int], x: int, y: int) -> int:
    return action(x, y)

# 泛型
T = TypeVar('T')
def first(items: list[T]) -> T | None:
    return items[0] if items else None

# 类型检查工具（在开发时使用，不运行时检查）
# mypy my_script.py     ← 静态类型检查
# ruff check .          ← 代码检查
```

---

## 🏗️ 工程化检查清单

- [ ] 项目使用虚拟环境（`.venv/` 已加入 `.gitignore`）
- [ ] 依赖写在 `pyproject.toml` 或 `requirements.txt`
- [ ] 使用 `src/` 布局组织代码
- [ ] 每个包有 `__init__.py`
- [ ] 使用 `if __name__ == "__main__"` 作为入口
- [ ] 异常捕获具体类型，不使用裸 `except:`
- [ ] 文件操作使用 `with` 语句
- [ ] 函数添加类型注解（可选但推荐）
- [ ] 使用 `ruff` 或 `black` 统一代码风格

---

## ✅ 阶段验收

1. 创建一个有 `pyproject.toml` 的 Python 项目，包含至少 3 个模块
2. 实现一个带 `@property`、`__str__`、`__repr__` 的类，并写单元测试
3. 写一个上下文管理器或带参数的装饰器
4. 解释生成器与列表在内存使用上的差异

---

> **上一阶段** ← [01-语法速通与基础](01-语法速通与基础.md)
> **下一阶段** → [03-标准库与日常脚本](03-标准库与日常脚本.md)
