# Python 详细知识点梳理

> **核心摘要**：从零基础到进阶的 Python 系统知识大全，涵盖基础语法、容器类型、函数、面向对象、模块与包、异常处理、文件操作、实战技巧等全维度。

---

## 目录

- [一、Python 基础核心](#一python-基础核心)
- [二、流程控制](#二流程控制)
- [三、容器类型](#三容器类型)
- [四、函数](#四函数)
- [五、面向对象](#五面向对象)
- [六、模块与包](#六模块与包)
- [七、异常处理](#七异常处理)
- [八、文件操作](#八文件操作)
- [九、进阶补充](#九进阶补充)
- [十、实战技巧与常见问题](#十实战技巧与常见问题)

---

## 一、Python 基础核心

### 1.1 Python 简介

Python 是一种解释型、面向对象、动态数据类型的高级编程语言，由 Guido van Rossum 于 1991 年发布。核心特点：简洁易读、跨平台、开源免费、生态丰富。

**版本差异**：Python 2.x（2020 年停止维护）与 Python 3.x（目前主流），推荐使用 3.8 及以上版本。

### 1.2 基本语法规则

| 规则 | 说明 |
|------|------|
| 注释 | `#` 单行注释，`""" """` 或 `''' '''` 多行注释 |
| 缩进 | 4 个空格表示代码块（核心语法，缩进错误会报 `IndentationError`） |
| 标识符 | 字母、数字、下划线组成，不能以数字开头，区分大小写 |
| 关键字 | 如 `if`、`for`、`class`、`import` 等，不可作为标识符 |

### 1.3 变量与数据类型

**6 种基本数据类型**：

| 类型 | 说明 | 示例 |
|------|------|------|
| int | 整数（无大小限制） | `10`, `-5`, `0` |
| float | 浮点数 | `3.14`, `-0.5` |
| str | 字符串（不可变） | `"hello"`, `'Python'` |
| bool | 布尔值 | `True`, `False` |
| NoneType | 空值 | `None` |
| complex | 复数 | `3+4j` |

### 1.4 运算符

| 类别 | 运算符 |
|------|--------|
| 算术 | `+ - * / // % **` |
| 比较 | `== != > < >= <=` |
| 逻辑 | `and or not` |
| 成员 | `in not in` |
| 身份 | `is is not` |

## 二、流程控制

### 2.1 分支结构

```python
if 条件1:
    pass
elif 条件2:
    pass
else:
    pass
```

### 2.2 循环结构

```python
# for 循环（遍历可迭代对象）
for i in range(5):
    print(i)

# while 循环（条件循环）
while 条件:
    pass

# 循环控制
break    # 终止循环
continue # 跳过本次
pass     # 占位符
```

## 三、容器类型

| 类型 | 特点 | 示例 |
|------|------|------|
| list | 有序、可修改、可重复 | `[1, "hello", True]` |
| tuple | 有序、不可修改、可重复 | `(1, "hello")` |
| dict | 无序、键唯一、键值对 | `{"name": "Python"}` |
| set | 无序、不可重复 | `{1, 2, 3}` |

## 四、函数

### 4.1 参数类型

| 类型 | 说明 | 示例 |
|------|------|------|
| 位置参数 | 按顺序传递 | `def add(a, b)` |
| 默认参数 | 有默认值 | `def add(a, b=2)` |
| 可变参数 | `*args` 打包成元组 | `def func(*args)` |
| 关键字参数 | `**kwargs` 打包成字典 | `def func(**kwargs)` |

### 4.2 匿名函数 lambda

```python
add = lambda a, b: a + b
list(map(lambda x: x * 2, [1, 2, 3]))  # [2, 4, 6]
```

## 五、面向对象

### 5.1 三种方法

```python
class Demo:
    def instance_method(self):
        pass

    @classmethod
    def class_method(cls):
        pass

    @staticmethod
    def static_method():
        pass
```

### 5.2 三大特性

- **封装**：`__name` 双下划线开头表示私有属性
- **继承**：`class Student(Person)`，`super()` 调用父类
- **多态**：子类重写父类方法，不同对象表现不同行为

### 5.3 魔术方法

| 方法 | 触发时机 |
|------|---------|
| `__init__` | 创建对象时 |
| `__str__` | 打印对象时（`print(obj)`） |
| `__repr__` | 交互环境显示对象 |
| `__add__` | `+` 运算符 |

## 六、模块与包

```python
# 导入模块
import math
from math import pi
import math as m

# 第三方模块安装
# pip install requests
```

## 七、异常处理

```python
try:
    pass
except ValueError as e:
    print(f"值错误: {e}")
except Exception as e:
    print(f"其他错误: {e}")
else:
    print("无异常时执行")
finally:
    print("始终执行")
```

## 八、文件操作

| 模式 | 说明 |
|------|------|
| `r` | 只读（默认） |
| `w` | 写入（覆盖） |
| `a` | 追加 |
| `rb`/`wb` | 二进制模式 |

## 九、进阶补充

| 概念 | 说明 | 场景 |
|------|------|------|
| 生成器 | `yield` 实现惰性计算 | 处理大文件/大数据 |
| 装饰器 | `@decorator` 修改函数行为 | 日志、权限、缓存 |
| 上下文管理器 | `with` 语句 | 文件、数据库连接 |
| 深浅拷贝 | `copy.copy()` vs `copy.deepcopy()` | 对象复制 |

## 十、实战技巧

| 技巧 | 实现 |
|------|------|
| 列表去重（保持顺序） | `[x for i, x in enumerate(lst) if x not in lst[:i]]` |
| 快速交换变量 | `a, b = b, a` |
| 批量赋值 | `a, *b = [1, 2, 3, 4]` |
| 高效字符串拼接 | `"".join(list)` |
| 安全字典取值 | `d.get("key", default)` |

## 核心要点回顾

- 入门：语法 + 4 大数据结构（列表、元组、字典、集合）
- 进阶：函数 + 面向对象 + 异常处理
- 精通：装饰器、生成器、并发、标准库、工程化
- Python 核心优势在于简洁易读、生态丰富
- 学习关键在于多练、多思考、多实战

## 参考资料

1. Python 官方文档 - docs.python.org
2. Python 教程 - runoob.com/python3
3. Real Python - realpython.com
