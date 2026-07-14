"""
单元 09：类型系统进阶

覆盖：TypeVar/Generic、TypedDict、Literal、overload、Final、Protocol、Self.
企业级场景：泛型容器、API 响应模型、配置类型、重载签名.
"""

from __future__ import annotations

from collections.abc import Callable, Iterable, Sequence
from typing import (
    Final,
    Generic,
    Literal,
    Protocol,
    TypedDict,
    TypeVar,
    final,
    overload,
)

# ============================================================
# 1. TypeVar + Generic：泛型
# ============================================================

T = TypeVar("T")
K = TypeVar("K")
V = TypeVar("V")


class Stack(Generic[T]):
    """泛型栈：支持任意类型的类型安全栈."""

    def __init__(self) -> None:
        self._items: list[T] = []

    def push(self, item: T) -> None:
        self._items.append(item)

    def pop(self) -> T | None:
        return self._items.pop() if self._items else None

    def peek(self) -> T | None:
        return self._items[-1] if self._items else None

    def __len__(self) -> int:
        return len(self._items)


def first(items: Sequence[T]) -> T | None:
    """返回序列第一个元素，泛型保持类型.

    Sequence[T] → T | None  而非 Sequence → Any
    """
    return items[0] if items else None


# ============================================================
# 2. TypedDict：强类型字典（API 响应、JSON 模型）
# ============================================================

class UserRecord(TypedDict):
    """用户记录的精确类型.

    比普通 dict[str, str] 更安全 — mypy 会检查 key 名和值类型.
    """
    id: str
    name: str
    email: str
    age: int


class UserRecordWithOptional(TypedDict, total=False):
    """total=False 表示所有字段可选."""
    id: str
    nickname: str
    avatar_url: str


def format_user(user: UserRecord) -> str:
    """TypedDict 保证调用方只能传符合结构的字典."""
    return f"User({user['id']}): {user['name']} <{user['email']}>"


# ============================================================
# 3. Literal：字面量类型（限定可选值）
# ============================================================

# Python 3.8+ typing.Literal，Python 3.11+ 可直接用 Literal
HttpMethod = Literal["GET", "POST", "PUT", "DELETE", "PATCH"]
LogLevel = Literal["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]


def request(url: str, method: HttpMethod) -> str:
    """method 只能是指定的 5 个值之一."""
    return f"{method} {url}"


def set_log_level(level: LogLevel) -> None:
    """level 类型由 Literal 约束."""
    print(f"  日志级别设置为: {level}")


# ============================================================
# 4. @overload：函数重载签名
# ============================================================

@overload
def get_value(data: dict[str, int], key: str) -> int: ...


@overload
def get_value(data: dict[str, str], key: str) -> str: ...


def get_value(data: dict[str, int] | dict[str, str], key: str) -> int | str:
    """根据输入 dict 的值类型，返回值类型也不同."""
    return data[key]


# ============================================================
# 5. Final / @final：禁止覆盖
# ============================================================

MAX_CONNECTIONS: Final = 100  # 常量，不可重新赋值


class BaseHandler:
    """基类处理器."""

    @final
    def handle(self) -> str:
        """子类不能覆盖此方法."""
        return "base handle"


# ============================================================
# 6. Protocol：结构化子类型（静态鸭子类型）
# ============================================================

class Flyable(Protocol):
    """任何有 fly 方法的对象都满足 Flyable 协议 — 无需继承."""

    def fly(self) -> str: ...


class Bird:
    def fly(self) -> str:
        return "Bird flying"


class Airplane:
    def fly(self) -> str:
        return "Airplane flying"


def make_it_fly(obj: Flyable) -> str:
    """只要对象有 fly() 方法就能传入，编译时检查."""
    return obj.fly()


# ============================================================
# 7. Self（Python 3.11+ typing_extensions.Self）
# ============================================================

from typing import Self  # Python 3.11 内置，低版本用 typing_extensions


class Builder:
    """链式调用 Builder 模式."""

    def __init__(self) -> None:
        self._config: dict[str, object] = {}

    def set_host(self, host: str) -> Self:
        self._config["host"] = host
        return self

    def set_port(self, port: int) -> Self:
        self._config["port"] = port
        return self

    def build(self) -> dict[str, object]:
        return dict(self._config)


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 09：类型系统进阶")
    print("=" * 50)

    # 泛型栈
    int_stack: Stack[int] = Stack()
    int_stack.push(1)
    int_stack.push(2)
    print(f"Stack len={len(int_stack)}, peek={int_stack.peek()}, pop={int_stack.pop()}")

    str_stack: Stack[str] = Stack()
    str_stack.push("hello")
    print(f"first fn: {first([10, 20, 30])}")

    # TypedDict
    alice: UserRecord = {"id": "1", "name": "Alice", "email": "a@b.com", "age": 30}
    print(f"\n{format_user(alice)}")

    # Literal
    print(f"\n{request('/api/users', method='GET')}")
    set_log_level("INFO")

    # overload
    int_val = get_value({"a": 1}, "a")
    str_val = get_value({"a": "hello"}, "a")
    print(f"\n  get_value int={int_val}, str={str_val}")

    # Protocol
    for obj in [Bird(), Airplane()]:
        print(f"  {make_it_fly(obj)}")

    # Self / Builder
    config = Builder().set_host("localhost").set_port(8080).build()
    print(f"\n  Builder result: {config}")
