# Python 面向对象快速入门

> **核心摘要**：以 Java 后端的视角，极速掌握 Python 面向对象编程。类是模板、对象是实例，三大特性：封装、继承、多态。10 分钟即可上手。

---

## 一、两个核心概念

- **类（Class）**：模板、图纸，抽象概念
- **对象（实例）**：根据模板造出来的具体个体

## 二、最简语法结构

```python
class 类名:
    # 构造方法：创建对象时自动执行
    def __init__(self, 参数1, 参数2):
        # self 代表当前对象自己
        self.属性1 = 参数1
        self.属性2 = 参数2

    # 实例方法
    def 方法名(self):
        业务逻辑
```

## 三、实战示例

```python
class Person:
    def __init__(self, name, age):
        self.name = name
        self.age = age

    def introduce(self):
        print(f"我叫{self.name}，今年{self.age}岁")

# 创建对象
p1 = Person("张三", 18)
p2 = Person("李四", 20)
p1.introduce()  # 我叫张三，今年18岁
p2.introduce()  # 我叫李四，今年20岁
```

**关键知识点**：
1. `class` 定义类
2. `__init__` 构造方法，创建对象自动调用
3. `self` 必须写在第一个参数，代表当前对象
4. `self.name` 给对象绑定属性

## 四、三大特性

### 4.1 封装

把属性和方法装在类里，隐藏内部细节，只暴露可用方法。

```python
class Person:
    def __init__(self, name):
        self.__name = name  # 私有属性：前面加两个下划线

    def get_name(self):
        return self.__name  # 提供方法对外访问
```

### 4.2 继承

子类复用父类的代码，不用重复写。

```python
class Person:
    def __init__(self, name):
        self.name = name

class Student(Person):  # Student 继承 Person
    def __init__(self, name, stu_id):
        super().__init__(name)  # 调用父类构造
        self.stu_id = stu_id

    def show(self):
        print(self.name, self.stu_id)

s = Student("小明", "2026001")
s.show()
```

### 4.3 多态

父类引用指向子类对象，不同子类重写同一方法，表现不同行为。

```python
class Animal:
    def speak(self):
        pass

class Dog(Animal):
    def speak(self):
        print("汪汪叫")

class Cat(Animal):
    def speak(self):
        print("喵喵叫")

def test_animal(a):
    a.speak()

test_animal(Dog())  # 汪汪叫
test_animal(Cat())  # 喵喵叫
```

## 五、三种方法区分

| 方法类型 | 装饰器 | 第一个参数 | 调用方式 |
|---------|--------|-----------|---------|
| 实例方法 | 无 | `self` | 只能对象调用 |
| 类方法 | `@classmethod` | `cls` | 类和对象都能调用 |
| 静态方法 | `@staticmethod` | 无 | 像普通函数 |

```python
class Demo:
    def func1(self):      # 实例方法
        pass

    @classmethod
    def func2(cls):       # 类方法
        pass

    @staticmethod
    def func3():          # 静态方法
        pass
```

## 六、魔术方法（常用）

- `__init__`：构造方法，创建对象自动执行
- `__str__`：打印对象时返回自定义字符串

```python
class Person:
    def __init__(self, name):
        self.name = name
    def __str__(self):
        return f"名字：{self.name}"

print(Person("张三"))  # 名字：张三
```

## 核心要点回顾

- 类是模板，对象是实例
- `__init__` 初始化，`self` 代表自己
- 三大特性：封装（`__` 私有属性）、继承（`super()`）、多态（方法重写）
- 三种方法：实例方法（self）、类方法（cls）、静态方法（无）
- 日常写代码：先建类 → 定属性 → 写方法 → 造对象调用

## 参考资料

1. Python 官方文档 - 类与面向对象编程
2. PEP 8 - Python 代码风格指南
