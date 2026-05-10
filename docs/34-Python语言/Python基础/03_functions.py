"""
单元 03：函数

覆盖：定义、参数类型（位置/默认/可变/关键字）、*args/**kwargs、lambda、闭包、
作用域（LEGB 规则）、类型注解 + Protocol.
"""

from __future__ import annotations

from collections.abc import Callable
from typing import Any, Protocol


# ============================================================
# 1. 函数定义与参数类型
# ============================================================

def greet(name: str, greeting: str = "Hello") -> str:
    """基本函数：必选参数 + 默认参数.

    注意：有默认值的参数必须放在无默认值参数后面.
    """
    return f"{greeting}, {name}!"


def create_user(name: str, /, *, age: int, city: str = "Unknown") -> dict[str, Any]:
    """仅位置参数 / 和仅关键字参数 *.

    - / 之前的参数只能按位置传递
    - * 之后的参数只能按关键字传递

    这种设计让 API 更清晰，避免歧义.
    """
    return {"name": name, "age": age, "city": city}


# ============================================================
# 2. *args 与 **kwargs
# ============================================================

def sum_all(*args: int) -> int:
    """*args：接收任意数量的位置参数，打包为元组."""
    return sum(args)


def build_headers(**kwargs: str) -> dict[str, str]:
    """**kwargs：接收任意数量的关键字参数，打包为字典."""
    return dict(kwargs)


def forward(*args: Any, **kwargs: Any) -> tuple[tuple[Any, ...], dict[str, Any]]:
    """同时使用 *args 和 **kwargs 来透传任意参数."""
    return args, kwargs


# ============================================================
# 3. lambda 与高阶函数
# ============================================================

def demo_lambda_and_higher_order() -> None:
    """lambda 表达式：单行匿名函数."""
    # lambda 语法：lambda 参数: 返回值
    add: Callable[[int, int], int] = lambda x, y: x + y
    print(f"  lambda add(3, 5) = {add(3, 5)}")

    # 高阶函数：sorted 的 key 参数
    users: list[dict[str, str]] = [
        {"name": "Charlie", "age": "35"},
        {"name": "Alice", "age": "30"},
        {"name": "Bob", "age": "25"},
    ]
    sorted_users = sorted(users, key=lambda u: u["age"])
    print(f"  sorted by age: {[u['name'] for u in sorted_users]}")

    # map / filter / reduce
    nums: list[int] = [1, 2, 3, 4, 5]
    squared: list[int] = list(map(lambda x: x ** 2, nums))
    evens: list[int] = list(filter(lambda x: x % 2 == 0, nums))
    print(f"  squared={squared}, evens={evens}")

    # 推导式通常比 map/filter 更可读
    squared2: list[int] = [x ** 2 for x in nums]
    evens2: list[int] = [x for x in nums if x % 2 == 0]
    print(f"  推导式: squared={squared2}, evens={evens2}")


# ============================================================
# 4. 闭包
# ============================================================

def make_multiplier(factor: int) -> Callable[[int], int]:
    """闭包：内部函数捕获外部函数的变量."""
    def multiply(x: int) -> int:
        return x * factor  # factor 被「闭合」捕获
    return multiply


# ============================================================
# 5. Protocol（结构化类型，比 ABC 更灵活）
# ============================================================

class Greeter(Protocol):
    """任何实现了 greet() 方法的对象都满足 Greeter 协议."""
    def greet(self) -> str: ...


def say_hello(obj: Greeter) -> str:
    """接受任何满足 Greeter 协议的对象，无需继承."""
    return obj.greet()


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 03：函数")
    print("=" * 50)

    print(greet("Alice"))
    print(greet("Bob", greeting="Hi"))

    # create_user 的参数规则：
    # - 第一个参数只能按位置
    # - age 必须按关键字
    user = create_user("Alice", age=30, city="Beijing")
    print(f"  create_user: {user}")

    print(f"sum_all(1, 2, 3, 4) = {sum_all(1, 2, 3, 4)}")
    print(f"headers: {build_headers(Authorization='Bearer xxx', Content_Type='application/json')}")
    print(f"forward: {forward(1, 2, key='value')}")

    print()
    demo_lambda_and_higher_order()

    double = make_multiplier(2)
    triple = make_multiplier(3)
    print(f"\n  double(5)={double(5)}, triple(5)={triple(5)}")
