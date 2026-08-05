# 01 - 语法速通：Java 后端视角

> 🎯 用你会的 Java 来学 Python — 对照表 + 关键差异 + 常见陷阱。30 分钟从 Java 到 Python

## Java → Python 速查表

```text
变量与类型：
  Java:   String name = "张三"; int age = 25;
  Python: name = "张三"; age = 25          # 无需声明类型

  Java:   final int X = 10;
  Python: X = 10                            # 约定大写=常量(实际可变)

控制流：
  Java:   if (x > 0) { ... } else { ... }
  Python: if x > 0: ... else: ...           # 冒号+缩进, 无括号

  Java:   for (int i=0; i<10; i++) { ... }
  Python: for i in range(10): ...           # range = 0..9

  Java:   for (String s : list) { ... }
  Python: for s in list: ...

集合：
  Java:   List<String> list = new ArrayList<>();
  Python: list = []                         # 动态类型列表

  Java:   Map<String, Integer> map = new HashMap<>();
  Python: dict = {}                         # 字典 {key: value}

  Java:   Set<String> set = new HashSet<>();
  Python: set = set() 或 {1, 2, 3}

函数：
  Java:   public int add(int a, int b) { return a + b; }
  Python: def add(a, b): return a + b       # 无类型/无访问修饰符

类：
  Java:   class Dog extends Animal { ... }
  Python: class Dog(Animal): ...            # 括号内是父类

  Java:   this.name = name;
  Python: self.name = name                  # self = this, 必须显式传
```

## 关键差异

| 特性 | Java | Python |
|------|------|--------|
| 编译/解释 | 编译→字节码→JVM | 解释执行 |
| 类型 | 静态强类型 | 动态强类型(鸭子类型) |
| 多继承 | 仅接口 | ✅ 支持 |
| 方法重载 | ✅ | ❌ (后定义覆盖) |
| 访问控制 | public/private/protected | 约定(_前缀=私有) |
| None/null | null | None |
| 字符串 | String(不可变) | str(不可变) |
| 布尔 | boolean(true/false) | bool(True/False) ← 大写！ |

## 常见陷阱

```python
# ① 默认参数只计算一次！
def add_item(item, lst=[]):    # ❌ 每次调用共享同一个list
    lst.append(item)
    return lst
# 修复：
def add_item(item, lst=None):
    if lst is None: lst = []
    lst.append(item)
    return lst

# ② 浅拷贝 vs 深拷贝
a = [1, 2, [3, 4]]
b = a.copy()        # 浅拷贝 → b[2] 和 a[2] 是同一个对象
b[2].append(5)      # a 也会变！
import copy
b = copy.deepcopy(a) # 深拷贝 ✅

# ③ is vs ==
a = [1, 2, 3]
b = [1, 2, 3]
a == b  # True  (值相等)
a is b  # False (不同对象)

# ④ 可变对象作为类属性
class Dog:
    tricks = []       # ❌ 类属性，所有实例共享！
    def __init__(self, name):
        self.name = name
        self.tricks = []  # ✅ 实例属性
```
