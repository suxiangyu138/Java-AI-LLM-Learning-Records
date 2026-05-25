# Python 核心知识点

---

## 一、入门基础（必须掌握）

### 1. 环境与基础语法
- 安装 Python + 开发工具（PyCharm / VS Code）
- 注释：`# 单行注释`、`"""多行注释"""`
- 输出：`print()` / 输入：`input()`
- 缩进规则（Python 用缩进代替大括号，**4个空格**）

### 2. 变量与数据类型
```python
# 数字
a = 10       # int
b = 3.14     # float
c = True     # bool

# 字符串
s = "hello"
s = 'hello'
s = f"年龄：{a}"  # f-string 格式化（最常用）

# 类型判断
type(a)
isinstance(a, int)
```

### 3. 运算符
- 算术：`+ - * / % ** //`
- 比较：`> < >= <= == !=`
- 逻辑：`and or not`
- 赋值：`= += -= *=`

### 4. 流程控制

#### 条件判断
```python
if 条件:
    执行语句
elif 条件:
    执行语句
else:
    执行语句
```

#### 循环
```python
# for 循环
for i in range(10):
    print(i)

# while 循环
while 条件:
    执行语句

# 循环控制
break   # 终止循环
continue # 跳过本次
```

---

## 二、核心数据结构（重中之重）

### 1. 列表 list（有序、可修改）
```python
lst = [1,2,3,"a"]
lst.append(4)    # 追加
lst.pop()        # 删除最后一个
lst[0]           # 取值
```

### 2. 元组 tuple（有序、不可修改）
```python
t = (1,2,3)
```

### 3. 字典 dict（键值对，最快查找）
```python
d = {"name":"小明", "age":18}
d["name"]        # 取值
d.get("name")    # 安全取值
```

### 4. 集合 set（去重、无序）
```python
s = {1,2,2,3}  # 自动去重 → {1,2,3}
```

### 5. 字符串常用方法
```python
s.split()    # 分割
s.strip()    # 去空格
s.replace()  # 替换
s.upper()    # 大写
```

---

## 三、函数与模块化（进阶必备）

### 1. 函数定义与调用
```python
def 函数名(参数):
    代码块
    return 返回值
```

### 2. 参数类型
- 必选参数
- 默认参数
- 可变参数 `*args`
- 关键字参数 `**kwargs`

### 3. 作用域
- 局部变量
- 全局变量 `global`
- 非局部变量 `nonlocal`

### 4. 匿名函数 lambda
```python
lambda x: x+1
```

### 5. 高阶函数
- `map()`
- `filter()`
- `reduce()`

### 6. 模块与包
- `import 模块`
- `from 模块 import 函数`
- 包结构：`__init__.py`

---

## 四、文件操作与异常处理

### 1. 文件读写
```python
# 推荐写法（自动关闭文件）
with open("test.txt", "r", encoding="utf-8") as f:
    data = f.read()
```
模式：`r` 读 / `w` 写 / `a` 追加 / `rb` 二进制

### 2. 异常处理
```python
try:
    可能出错代码
except Exception as e:
    print(e)
finally:
    无论如何都会执行
```

---

## 五、面向对象 OOP（Python 核心）

### 1. 类与对象
```python
class Person:
    def __init__(self, name):  # 构造方法
        self.name = name
    
    def say(self):
        print("我是", self.name)

p = Person("小明")
p.say()
```

### 2. 三大特性
- **封装**：私有属性 `__name`
- **继承**：`class Student(Person)`
- **多态**：不同子类重写同一方法

### 3. 核心概念
- 实例方法 / 类方法 `@classmethod` / 静态方法 `@staticmethod`
- 属性装饰器 `@property`
- 魔术方法：`__init__`、`__str__`、`__del__`

---

## 六、高级特性（精通必备）

### 1. 推导式
```python
# 列表推导式
[x*2 for x in range(10)]

# 字典推导式
{k:v for k,v in dict.items()}
```

### 2. 生成器 & 迭代器
- `yield` 生成器（节省内存）
- `iter()` / `next()` 迭代器

### 3. 装饰器 @（超级常用）
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

### 4. 闭包

### 5. 上下文管理器 `with`

---

## 七、并发编程（提升效率）

### 1. 多线程 threading

### 2. 多进程 multiprocessing

### 3. 协程 asyncio（最高效）
```python
import asyncio
async def func():
    await asyncio.sleep(1)
```

---

## 八、标准库高频模块
- `os` / `sys` 系统操作
- `json` 数据处理
- `re` 正则表达式
- `datetime` 时间
- `random` 随机
- `logging` 日志
- `unittest` 测试

---

## 九、第三方库（实战必学）
- 数据分析：`pandas`、`numpy`
- 爬虫：`requests`、`beautifulsoup4`、`scrapy`
- Web 开发：`Flask`、`Django`
- 自动化：`selenium`、`pyautogui`
- 人工智能：`tensorflow`、`pytorch`

---

## 十、工程化 & 高级规范（精通）
- 虚拟环境 `venv` / `conda`
- 包管理 `pip` / `requirements.txt`
- 代码规范 PEP8
- 类型注解 `def add(a:int) -> int`
- 设计模式
- 性能优化（时间/空间复杂度）

---

# 学习路线建议
1. **入门**：语法 + 数据类型 + 流程控制
2. **进阶**：函数 + 面向对象 + 文件/异常
3. **精通**：装饰器、生成器、并发、工程化

---

### 总结
- 入门：**语法 + 4 大数据结构**
- 进阶：**函数 + 面向对象 + 异常处理**
- 精通：**装饰器、生成器、并发、标准库、工程化**
