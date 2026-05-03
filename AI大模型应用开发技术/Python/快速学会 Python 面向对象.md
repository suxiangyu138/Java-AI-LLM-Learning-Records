# 快速学会 Python 面向对象
## 一、先记2个核心概念
- **类（Class）**：模板、图纸，抽象概念
- **对象（实例）**：根据模板造出来的**具体个体**

一句话：**类是模板，对象是实例**。

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

## 三、写个实战例子（秒懂）
```python
class Person:
    # 构造函数，初始化属性
    def __init__(self, name, age):
        self.name = name
        self.age = age

    # 行为方法
    def introduce(self):
        print(f"我叫{self.name}，今年{self.age}岁")

# 创建对象
p1 = Person("张三", 18)
p2 = Person("李四", 20)

# 调用方法
p1.introduce()
p2.introduce()
```
运行结果：
```
我叫张三，今年18岁
我叫李四，今年20岁
```

### 关键知识点
1. `class` 定义类
2. `__init__` 构造方法，**创建对象自动调用**
3. `self` 必须写在第一个参数，代表**当前对象**
4. `self.name` 给对象绑定属性

## 四、三大特性（必背）
### 1. 封装
把**属性和方法装在类里**，隐藏内部细节，只暴露可用方法。
```python
class Person:
    def __init__(self, name):
        # 私有属性：前面加两个下划线
        self.__name = name

    def get_name(self):
        # 提供方法对外访问
        return self.__name
```
`__name` 外部不能直接改，只能通过方法操作。

### 2. 继承
子类复用父类的代码，不用重复写。
```python
# 父类
class Person:
    def __init__(self, name):
        self.name = name

# 子类 Student 继承 Person
class Student(Person):
    def __init__(self, name, stu_id):
        # 调用父类构造
        super().__init__(name)
        self.stu_id = stu_id

    def show(self):
        print(self.name, self.stu_id)

s = Student("小明", "2026001")
s.show()
```

### 3. 多态
父类引用指向子类对象，**不同子类重写同一方法，表现不同行为**。
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

# 多态体现
def test_animal(a):
    a.speak()

test_animal(Dog())
test_animal(Cat())
```

## 五、三种方法区分
1. **实例方法**：带 `self`，只能对象调用
2. **类方法**：`@classmethod`，带 `cls`，类和对象都能调用
3. **静态方法**：`@staticmethod`，不用 `self/cls`，像普通函数

```python
class Demo:
    # 实例方法
    def func1(self):
        pass

    # 类方法
    @classmethod
    def func2(cls):
        pass

    # 静态方法
    @staticmethod
    def func3():
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

print(Person("张三"))
```

## 七、一句话总结
1. **类是模板，对象是实例**
2. `__init__` 初始化，`self` 代表自己
3. 三大特性：**封装、继承、多态**
4. 日常写代码：先建类 → 定属性 → 写方法 → 造对象调用

