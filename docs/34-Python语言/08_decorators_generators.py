"""
单元 08：装饰器、迭代器与生成器

覆盖：装饰器原理/@语法/functools、生成器/yield、迭代器协议、async/await 入门.
"""

from __future__ import annotations

import functools
import time
from collections.abc import Callable, Generator, Iterator
from typing import Any, ParamSpec, TypeVar

P = ParamSpec("P")
R = TypeVar("R")


# ============================================================
# 1. 装饰器基础
# ============================================================

def log_call(func: Callable[P, R]) -> Callable[P, R]:
    """装饰器：记录函数调用.

    使用 functools.wraps 保留原函数的元信息（__name__、__doc__ 等）.
    """

    @functools.wraps(func)
    def wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
        print(f"  [LOG] 调用 {func.__name__}({args}, {kwargs})")
        result = func(*args, **kwargs)
        print(f"  [LOG] {func.__name__} 返回 {result}")
        return result

    return wrapper


@log_call
def add(a: int, b: int) -> int:
    """返回两个整数的和."""
    return a + b


# ============================================================
# 2. 带参数的装饰器
# ============================================================

def retry(max_attempts: int = 3, delay: float = 0.1):
    """装饰器工厂：失败自动重试.

    带参数的装饰器本质上是「返回装饰器的函数」.
    """

    def decorator(func: Callable[P, R]) -> Callable[P, R]:
        @functools.wraps(func)
        def wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
            last_exc: Exception | None = None
            for attempt in range(1, max_attempts + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:
                    last_exc = exc
                    print(f"    重试 {attempt}/{max_attempts}，错误: {exc}")
                    if attempt < max_attempts:
                        time.sleep(delay)
            raise last_exc  # type: ignore[misc]
        return wrapper
    return decorator


@retry(max_attempts=2, delay=0.05)
def unreliable_operation(value: int) -> int:
    """模拟不稳定的操作."""
    if value < 0:
        raise ValueError(f"负数: {value}")
    return value * 2


# ============================================================
# 3. 生成器（yield）
# ============================================================

def fibonacci(n: int) -> Generator[int, None, None]:
    """生成斐波那契数列前 n 项.

    生成器函数遇到 yield 暂停，下次调用 next() 时恢复.
    内存 O(1)，适合处理大数据流.
    """
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b


def demo_generators() -> None:
    """生成器的多种用法."""
    # 基本迭代
    print("  斐波那契前 10 项:")
    fib = fibonacci(10)
    print(f"    {list(fib)}")

    # 生成器推导式（用 () 而非 []）
    squares_gen = (x ** 2 for x in range(5))  # 惰性求值
    print(f"  生成器: {next(squares_gen)}")    # 0
    print(f"  生成器: {next(squares_gen)}")    # 1

    # 生成器管道：处理大文件时特别有用
    def read_lines() -> Generator[str, None, None]:
        yield "  hello"
        yield "  world"
        yield "  # comment"
        yield "  python"

    def strip_lines(lines: Iterator[str]) -> Generator[str, None, None]:
        for line in lines:
            yield line.strip()

    def filter_comments(lines: Iterator[str]) -> Generator[str, None, None]:
        for line in lines:
            if not line.startswith("#"):
                yield line

    # 管道组合
    pipeline = filter_comments(strip_lines(read_lines()))
    print(f"  管道结果: {list(pipeline)}")  # ['hello', 'world', 'python']


# ============================================================
# 4. yield from（委托给子生成器）
# ============================================================

def chain_generators() -> Generator[int, None, None]:
    """yield from：将迭代委托给另一个生成器."""
    yield from range(3)       # 等价于 for i in range(3): yield i
    yield from [10, 20, 30]


# ============================================================
# 5. 迭代器协议
# ============================================================

class CountDown:
    """实现迭代器协议的类."""

    def __init__(self, start: int) -> None:
        self._current = start

    def __iter__(self) -> "CountDown":
        return self

    def __next__(self) -> int:
        if self._current < 0:
            raise StopIteration
        value = self._current
        self._current -= 1
        return value


# ============================================================
# 6. async/await 入门
# ============================================================

async def async_greet(name: str) -> str:
    """异步函数（需 Python 3.7+ asyncio.run 运行）."""
    await asyncio.sleep(0.1)  # 模拟 IO
    return f"Hello, {name}"


async def async_main() -> None:
    """并发执行多个异步任务."""
    results = await asyncio.gather(
        async_greet("Alice"),
        async_greet("Bob"),
        async_greet("Charlie"),
    )
    print(f"  Async results: {results}")


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 08：装饰器、迭代器与生成器")
    print("=" * 50)

    # 装饰器
    print("1. 基本装饰器:")
    result = add(3, 5)  # type: ignore[call-arg]
    print(f"   3 + 5 = {result}")

    print("\n2. 带参数的装饰器（重试）:")
    print(f"   结果: {unreliable_operation(42)}")
    try:
        unreliable_operation(-1)
    except ValueError as e:
        print(f"   最终失败: {e}")

    # 生成器
    print("\n3. 生成器:")
    demo_generators()
    print(f"   yield from: {list(chain_generators())}")

    # 迭代器
    print("\n4. 迭代器协议:")
    for num in CountDown(3):
        print(f"    倒计时: {num}")

    # async
    print("\n5. async/await:")
    import asyncio
    asyncio.run(async_main())
