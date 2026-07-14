# 阶段 1：语法速通与基础

> **目标**：3–7 天不看语法书能独立写 Python 脚本
> **策略**：与 Java 对比学习，集中精力在差异点，相同概念一笔带过
> **前置**：已掌握 Java 或任一静态类型语言

---

## 📌 章节定位

你已经会 Java，Python 基础语法大量概念可以直接映射。本章聚焦**Python 独有特性**和**与 Java 的核心差异**，帮你用最短时间建立 Python 编程直觉。

---

## 🎯 核心章节

### 1. 环境与解释器

- **交互式环境（REPL）**：终端输入 `python` 进入，适合快速实验
- **脚本执行**：`python script.py` 运行 .py 文件
- **IPython**：增强版交互式环境，支持 Tab 补全、`?` 查文档、`%timeit` 计时
- **Jupyter Notebook**：单元格式交互编程，AI/数据方向必备工具

```python
# 在 REPL 中快速测试代码片段
>>> 2 + 3
5
>>> type(42)
<class 'int'>
>>> help(print)      # 查看内置函数文档
```

### 2. 变量与基本数据类型

**🔑 核心认知**：Python 是**动态类型 + 强类型**语言

| 特性 | Java | Python |
|------|------|--------|
| 类型声明 | `int x = 5;` | `x = 5`（变量无类型） |
| 类型检查 | 编译期 | 运行时 |
| 隐式转换 | `"a" + 1` → `"a1"` | `"a" + 1` → **TypeError** |
| 变量本质 | 存储值的容器 | **指向对象的标签（引用）** |

#### 内置基本类型

```python
# 数字类型
a: int = 42            # 整数（任意精度，无溢出）
b: float = 3.14        # 浮点（双精度，IEEE 754）
c: complex = 1 + 2j    # 复数（内置支持）
d: bool = True         # 布尔（True/False，首字母大写）

# 序列类型（⭐ 重点）
s: str = "hello"       # 字符串（不可变，Unicode）
lst: list = [1, 2, 3]  # 列表（可变动态数组）
tup: tuple = (1, 2, 3) # 元组（不可变序列）

# 映射与集合
d: dict = {"a": 1}     # 字典（哈希表，3.6+ 保序）
se: set = {1, 2, 3}    # 集合（无序不重复）

# 特殊
n: None = None         # 空值（相当于 Java 的 null）
```

#### 可变 vs 不可变（⭐ 关键概念）

```python
# 不可变类型：int, float, str, tuple, frozenset
s = "hello"
s[0] = "H"            # TypeError! 字符串不可变
s = "H" + s[1:]       # ✅ 创建新字符串对象

# 可变类型：list, dict, set
lst = [1, 2, 3]
lst[0] = 99           # ✅ 原地修改

# ⚠️ 坑点：默认参数的可变对象
def add_item(item, target=[]):       # ❌ list 只初始化一次！
    target.append(item)
    return target

print(add_item(1))   # [1]
print(add_item(2))   # [1, 2]  ← 不是 [2]！

def add_item_fixed(item, target=None): # ✅ 正确写法
    if target is None:
        target = []
    target.append(item)
    return target
```

### 3. 字符串与格式化

```python
# 字符串定义方式
s1 = 'single quotes'
s2 = "double quotes"
s3 = '''triple quotes for
multi-line strings'''
s4 = f"interpolated: {1 + 1}"  # f-string (3.6+)，⭐ 推荐

# f-string 进阶（Python 3.8+）
name = "Alice"
price = 123.456
print(f"{name=}")             # name='Alice'  （调试神器）
print(f"{price:.2f}")         # 123.46        （格式化浮点）
print(f"{42:08b}")            # 00101010      （二进制补零）

# 常用字符串方法
"hello world".split()          # ['hello', 'world']
"  trim  ".strip()             # 'trim'
"a,b,c".split(",")             # ['a', 'b', 'c']
"_".join(["a", "b"])           # 'a_b'
"hello".upper()                # 'HELLO'
"hello".replace("l", "L")      # 'heLLo'
"abc".find("b")                # 1  （-1 表示未找到）
```

### 4. 序列操作与切片（⭐ Python 标志性特性）

```python
lst = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]

# 切片语法：seq[start:stop:step]
lst[2:5]        # [2, 3, 4]    - 左闭右开（和 Java substring 一致）
lst[:3]         # [0, 1, 2]    - 省略 start 从头开始
lst[7:]         # [7, 8, 9]    - 省略 stop 直到末尾
lst[::2]        # [0, 2, 4, 6, 8] - step 隔一个取一个
lst[::-1]       # [9, 8, 7, 6, 5, 4, 3, 2, 1, 0] - 反转序列！

# 解包（Unpacking）
a, b, c = [1, 2, 3]             # a=1, b=2, c=3
first, *middle, last = [1, 2, 3, 4, 5]  # first=1, middle=[2,3,4], last=5

# 常用序列操作
len(lst)                        # 长度
3 in lst                        # True  （成员检查）
lst.index(5)                    # 5    （首次出现位置）
lst.count(3)                    # 1    （计数）
```

### 5. 控制流

```python
# if-elif-else（注意是 elif，不是 else if）
x = 10
if x > 20:
    print("large")
elif x > 5:
    print("medium")
else:
    print("small")

# Python 特色：比较链
if 0 < x < 20:                  # 等同于 0 < x and x < 20
    print("in range")

# while 循环
count = 0
while count < 5:
    print(count)
    count += 1
else:                           # ⭐ while-else（循环正常结束触发）
    print("loop finished")

# for 循环（⭐ 与 Java 完全不同）
# Java: for (int i = 0; i < n; i++)
# Python: for item in iterable
for i in range(5):              # 0, 1, 2, 3, 4
    print(i)

for item in ["a", "b", "c"]:    # 直接遍历元素
    print(item)

for i, item in enumerate(["a", "b", "c"]):  # 同时需要索引
    print(f"{i}: {item}")

for k, v in {"a": 1, "b": 2}.items():      # 遍历字典
    print(f"{k} -> {v}")

# range 详解
range(5)        # 0, 1, 2, 3, 4
range(2, 5)     # 2, 3, 4
range(0, 10, 2) # 0, 2, 4, 6, 8
```

### 6. 推导式（⭐ Python 标志性语法）

```python
# 列表推导式 —— 用一行表达式替代 for 循环创建列表
squares = [x**2 for x in range(10)]                     # [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
evens = [x for x in range(20) if x % 2 == 0]            # 带过滤
pairs = [(x, y) for x in range(3) for y in range(2)]    # 嵌套循环

# 字典推导式
word_lengths = {word: len(word) for word in ["hi", "hello", "world"]}
# {'hi': 2, 'hello': 5, 'world': 5}

# 集合推导式
unique_lengths = {len(word) for word in ["hi", "hello", "world"]}
# {2, 5}

# 生成器表达式（惰性求值，节省内存）
sum(x**2 for x in range(10**6))  # 不会先创建百万元素的列表
```

### 7. 函数

```python
# 基本定义（⭐ 不需要声明返回类型）
def greet(name, greeting="Hello"):     # 默认参数
    """返回问候语。"""                   # docstring（相当于 JavaDoc）
    return f"{greeting}, {name}!"

# Python 不支持方法重载！用默认参数和可变参数替代
def add(*args):                        # *args 收集所有位置参数为 tuple
    return sum(args)

add(1, 2)       # 3
add(1, 2, 3, 4) # 10

def print_info(**kwargs):              # **kwargs 收集关键字参数为 dict
    for key, value in kwargs.items():
        print(f"{key}: {value}")

print_info(name="Alice", age=20)
# name: Alice
# age: 20

# lambda（单行匿名函数，相当于 Java lambda）
square = lambda x: x**2
sorted([3, 1, 2], key=lambda x: x)     # 按自定义规则排序

# 作用域（LEGB 规则：Local → Enclosing → Global → Built-in）
x = "global"
def outer():
    x = "enclosing"
    def inner():
        x = "local"
        print(x)                       # "local"
    inner()
outer()

# ⚠️ 坑：Python 是函数级作用域，不是块级作用域！
if True:
    y = 42
print(y)    # 42  ← Java 中这里会编译错误！
```

### 8. 模块与导入

```python
# import 的几种形式
import math                          # 导入整个模块
from math import sqrt, sin           # 导入特定函数
from math import *                   # 导入所有（不推荐，污染命名空间）
import numpy as np                   # 别名导入（⭐ 社区惯例）

# 自己创建模块
# my_module.py:
#   CONSTANT = 42
#   def helper(): ...
#
# main.py:
#   import my_module
#   print(my_module.CONSTANT)

# __name__ 魔法变量
if __name__ == "__main__":           # 脚本直接运行时为 "__main__"
    print("Running as script")       # 被 import 时为模块名
```

---

## 🆚 Java vs Python 语法速查

| 概念 | Java | Python |
|------|------|--------|
| 入口函数 | `public static void main` | `if __name__ == "__main__"` |
| 打印 | `System.out.println(x)` | `print(x)` |
| 数组 | `int[] a = {1,2,3};` | `a = [1, 2, 3]` |
| Map | `Map<String,Integer> m = new HashMap<>()` | `m = {"a": 1}` |
| for-each | `for (int x : arr)` | `for x in arr` |
| 三元 | `x > 0 ? 1 : -1` | `1 if x > 0 else -1` |
| 相等比较 | `a.equals(b)` | `a == b`（值相等） |
| 身份比较 | `a == b` | `a is b`（判断是否是同一个对象） |
| null 检查 | `if (x == null)` | `if x is None` |
| 类型检查 | `x instanceof String` | `isinstance(x, str)` |
| 类型转换 | `(String) obj` | `str(obj)`（调用构造函数） |
| 字符串长度 | `s.length()` | `len(s)` |
| 注释 | `//` `/* */` | `#` `""" """`（文档字符串） |
| 逻辑运算符 | `&&` `\|\|` `!` | `and` `or` `not` ← 注意是英文单词 |

---

## ✅ 阶段验收

完成以下自测，视为阶段 1 合格：

1. 手写一个脚本，接收命令行参数，遍历目录下所有 `.log` 文件并统计总行数
2. 用列表推导式生成 1–100 中所有能被 3 整除但不能被 5 整除的数
3. 实现一个函数 `group_by_type(items)`，输入混合类型列表，返回按类型分组的 dict
4. 解释 `a = [1,2]` → `b = a` → `b.append(3)` 后 `a` 的值，以及为什么

---

## 📚 推荐资源

- 官方入门教程：[docs.python.org/3/tutorial](https://docs.python.org/3/tutorial/)
- 交互式学习：[pythontutor.com](https://pythontutor.com/) — 可视化代码执行过程
- 《Python-100-Days》Day01–20：系统刷一遍基础语法
- 编程指北 Python 一条龙：适合快速建立知识框架

---

> **下一阶段** → [02-进阶语法与工程基础](02-进阶语法与工程基础.md)
