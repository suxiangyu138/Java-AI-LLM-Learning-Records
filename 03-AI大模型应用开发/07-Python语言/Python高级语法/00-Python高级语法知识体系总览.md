# 00 - Python 高级语法 知识体系总览

> 🎯 Python 高级语法的关键不在语法本身，而在于理解"Pythonic"的设计哲学——装饰器、生成器、上下文管理器、协程，这些是 AI 框架（Transformers/LangChain/PyTorch）的底层基石

> 🎯 共 **12 篇**，从装饰器到协程、从元类到类型提示，覆盖 Python 进阶全链路

---

## 1. 知识全景

```
Python 高级语法体系（12个文件）
│
├── 🏗️ 函数进阶篇（01-03）
│   ├── 01-装饰器与闭包.md
│   ├── 02-生成器与迭代器.md
│   └── 03-上下文管理器与with.md
│
├── 🔧 面向对象篇（04-06）
│   ├── 04-元类与类装饰器.md
│   ├── 05-描述符与属性管理.md
│   └── 06-魔术方法与运算符重载.md
│
├── 🚀 并发与类型篇（07-09）
│   ├── 07-协程与asyncio深度.md
│   ├── 08-并发编程：线程与进程.md
│   └── 09-类型提示与Pydantic.md
│
├── 📋 函数式篇（10）
│   └── 10-函数式编程：map_filter_reduce.md
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | 装饰器与闭包 | @语法糖/闭包/functools | ⭐⭐⭐⭐⭐ |
| 02 | 生成器与迭代器 | yield/Generator/惰性求值 | ⭐⭐⭐⭐⭐ |
| 03 | 上下文管理器 | with/__enter__/contextmanager | ⭐⭐⭐⭐ |
| 04 | 元类 | type/__new__/metaclass | ⭐⭐⭐ |
| 05 | 描述符 | __get__/__set__/@property | ⭐⭐⭐ |
| 06 | 魔术方法 | __str__/__call__/运算符重载 | ⭐⭐⭐⭐ |
| 07 | 协程深度 | async/await/事件循环/Task | ⭐⭐⭐⭐⭐ |
| 08 | 并发编程 | Thread/Process/线程池/GIL | ⭐⭐⭐⭐ |
| 09 | 类型提示 | typing/Protocol/Pydantic | ⭐⭐⭐⭐ |
| 10 | 函数式编程 | map/filter/reduce/lambda | ⭐⭐⭐⭐ |
| 11 | 面试考点 | 闭包/装饰器/协程/GIL | ⭐⭐⭐⭐⭐ |

## 3. 学习路线

```text
🟢 基础（1h）：01-装饰器 → 02-生成器 → 03-上下文管理器
🔵 进阶（45min）：06-魔术方法 → 09-类型提示 → 10-函数式
🟣 深入（1h）：04-元类 → 05-描述符 → 07-协程 → 08-并发
🔴 冲刺（30min）：11-面试考点
```

## 4. Java → Python 速查

| Java | Python | 说明 |
|------|--------|------|
| `@Annotation` | `@decorator` | 装饰器 ≈ 更强大的注解 |
| `Iterator<T>` | `__iter__` + `yield` | 生成器 ≈ 懒加载迭代器 |
| `try-with-resources` | `with` 语句 | 上下文管理器 ≈ AutoCloseable |
| `Thread` / `ExecutorService` | `threading` / `asyncio` | 线程 vs 协程（Python 特有） |
| `Generics<T>` | `TypeVar` / `Protocol` | 类型提示 ≈ 弱版泛型 |
| `interface` | `ABC` / `Protocol` | 抽象基类 / 结构化子类型 |
