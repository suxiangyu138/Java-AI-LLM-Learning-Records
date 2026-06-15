# Python 核心知识点

> **核心摘要**：从入门到精通的 Python 核心知识体系，涵盖基础语法、数据结构、函数、面向对象、高级特性和工程化规范。按照入门 → 进阶 → 精通三阶段组织。

---

## 目录

- [一、入门基础](#一入门基础)
- [二、核心数据结构](#二核心数据结构)
- [三、函数与模块化](#三函数与模块化)
- [四、文件操作与异常处理](#四文件操作与异常处理)
- [五、面向对象 OOP](#五面向对象-oop)
- [六、高级特性](#六高级特性)
- [七、并发编程](#七并发编程)
- [八、标准库高频模块](#八标准库高频模块)
- [九、第三方库](#九第三方库)
- [十、工程化与高级规范](#十工程化与高级规范)

---

## 一、入门基础

### 1.1 环境与基础语法

- 安装 Python + 开发工具（PyCharm / VS Code）
- 注释：`# 单行注释`、`"""多行注释"""`
- 输出：`print()` / 输入：`input()`
- 缩进规则：用缩进代替大括号，**4 个空格**

### 1.2 变量与数据类型

```python
# 数字
a = 10       # int
b = 3.14     # float
c = True     # bool

# 字符串
s = f"年龄：{a}"  # f-string 格式化（最常用）

# 类型判断
type(a)
isinstance(a, int)
```

### 1.3 运算符

| 类别 | 运算符 |
|------|--------|
| 算术 | `+ - * / % ** //` |
| 比较 | `> < >= <= == !=` |
| 逻辑 | `and or not` |
| 赋值 | `= += -= *=` |

### 1.4 流程控制

```python
# 条件判断
if 条件:
    pass
elif 条件:
    pass
else:
    pass

# for 循环
for i in range(10):
    print(i)

# while 循环
while 条件:
    pass
```

## 二、核心数据结构

| 类型 | 特点 | 示例 |
|------|------|------|
| 列表 list | 有序、可修改 | `[1, 2, 3]` |
| 元组 tuple | 有序、不可修改 | `(1, 2, 3)` |
| 字典 dict | 键值对、最快查找 | `{"name": "小明"}` |
| 集合 set | 去重、无序 | `{1, 2, 3}` |

## 三、函数与模块化

### 3.1 参数类型

- 必选参数
- 默认参数
- 可变参数 `*args`
- 关键字参数 `**kwargs`

### 3.2 高级函数

```python
# lambda 匿名函数
lambda x: x + 1

# 高阶函数
map(lambda x: x*2, [1, 2, 3])
filter(lambda x: x > 2, [1, 2, 3])
```

## 四、文件操作与异常处理

```python
# 文件读写（自动关闭）
with open("test.txt", "r", encoding="utf-8") as f:
    data = f.read()

# 异常处理
try:
    pass
except Exception as e:
    print(e)
finally:
    pass
```

## 五、面向对象 OOP

### 5.1 类与对象

```python
class Person:
    def __init__(self, name):
        self.name = name

    def say(self):
        print("我是", self.name)
```

### 5.2 三大特性

- **封装**：私有属性 `__name`
- **继承**：`class Student(Person)`
- **多态**：不同子类重写同一方法

### 5.3 核心概念

- 实例方法 / 类方法 `@classmethod` / 静态方法 `@staticmethod`
- 属性装饰器 `@property`
- 魔术方法：`__init__`、`__str__`、`__del__`

## 六、高级特性

### 6.1 推导式

```python
[x * 2 for x in range(10)]           # 列表推导式
{k: v for k, v in dict.items()}       # 字典推导式
```

### 6.2 装饰器

```python
def decorator(func):
    def wrapper():
        print("before")
        func()
        print("after")
    return wrapper

@decorator
def test():
    print("run")
```

### 6.3 生成器

```python
def gen():
    yield 1
    yield 2
```

## 七、并发编程

| 方式 | 适用场景 |
|------|---------|
| 多线程 threading | IO 密集型任务 |
| 多进程 multiprocessing | CPU 密集型任务 |
| 协程 asyncio | 高并发 IO 任务 |

## 八、标准库高频模块

- `os` / `sys`：系统操作
- `json`：数据处理
- `re`：正则表达式
- `datetime`：时间处理
- `random`：随机数
- `logging`：日志
- `unittest`：测试

## 九、第三方库

| 方向 | 库 |
|------|-----|
| 数据分析 | `pandas`、`numpy` |
| 爬虫 | `requests`、`beautifulsoup4` |
| Web 开发 | `Flask`、`Django` |
| 自动化 | `selenium`、`pyautogui` |
| 人工智能 | `tensorflow`、`pytorch` |

## 十、工程化与高级规范

- 虚拟环境 `venv` / `conda`
- 包管理 `pip` / `requirements.txt`
- 代码规范 PEP8
- 类型注解 `def add(a: int) -> int:`
- 设计模式
- 性能优化（时间/空间复杂度）

## 核心要点回顾

- 入门：语法 + 4 大数据结构（列表、元组、字典、集合）
- 进阶：函数 + 面向对象 + 异常处理
- 精通：装饰器、生成器、并发、标准库、工程化
- 推导式和装饰器是 Python 最具特色的高级特性

## 参考资料

1. Python 官方文档 - 语言参考
2. PEP 8 - Python 代码风格指南
3. Real Python - Python 教程
