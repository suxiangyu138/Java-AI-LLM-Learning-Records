"""
模块 13：泛型编程。

覆盖知识点：
    - TypeVar：类型变量
    - Generic[T]：泛型类
    - 约束泛型（bound= / constrained）
    - 泛型函数
    - 实战：泛型 Repository、泛型 Result 容器
"""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass, field
from typing import Any, Generic, TypeVar


# ============================================================
# 基础：TypeVar 与 Generic
# ============================================================
T = TypeVar("T")
K = TypeVar("K")
V = TypeVar("V")

# 约束泛型：只接受 int 或 float
Number = TypeVar("Number", int, float)

# bound 泛型：接受 HasName 或其子类
class HasName:
    def __init__(self, name: str) -> None:
        self.name = name


NameT = TypeVar("NameT", bound=HasName)


# ============================================================
# 实战一：泛型 Repository（仓库模式）
# ============================================================
@dataclass
class Entity:
    """实体基类 — id 由 Repository 自动分配，所有子类字段需要有默认值。"""

    id: int = 0


class Repository(Generic[T]):
    """
    泛型仓库 —— 为任意实体类型提供 CRUD 操作。

    用法：user_repo = Repository[User]()
    """

    def __init__(self) -> None:
        self._items: dict[int, T] = {}
        self._next_id = 1

    def add(self, item: T) -> T:
        if hasattr(item, "id"):
            item.id = self._next_id
        self._items[self._next_id] = item
        self._next_id += 1
        return item

    def get(self, id_: int) -> T | None:
        return self._items.get(id_)

    def list_all(self) -> list[T]:
        return list(self._items.values())

    def remove(self, id_: int) -> bool:
        if id_ in self._items:
            del self._items[id_]
            return True
        return False

    def count(self) -> int:
        return len(self._items)

    def find(self, predicate: Callable[[T], bool]) -> list[T]:
        """根据条件查找。"""
        return [item for item in self._items.values() if predicate(item)]


# ============================================================
# 具体实体
# ============================================================
@dataclass
class User(Entity):
    name: str = ""
    email: str = ""


@dataclass
class Product(Entity):
    sku: str = ""
    price: float = 0.0


# ============================================================
# 实战二：泛型 Result 容器（避免 None 传播）
# ============================================================
@dataclass
class Result(Generic[V]):
    """
    泛型 Result 容器 —— 表示可能成功或失败的操作结果。

    Rust 风格的 Result 类型，避免 None 泛滥和 try-except 嵌套。
    """

    value: V | None = field(default=None)
    error: str | None = field(default=None)

    @classmethod
    def ok(cls, value: V) -> Result[V]:
        return cls(value=value)

    @classmethod
    def fail(cls, error: str) -> Result[V]:
        return cls(error=error)

    @property
    def is_ok(self) -> bool:
        return self.error is None

    @property
    def is_fail(self) -> bool:
        return not self.is_ok

    def unwrap(self) -> V:
        """成功返回 value，失败抛异常。"""
        if self.is_fail:
            raise ValueError(f"试图解包失败的 Result: {self.error}")
        return self.value  # type: ignore

    def unwrap_or(self, default: V) -> V:
        """成功返回 value，失败返回默认值。"""
        return self.value if self.is_ok else default  # type: ignore

    def map(self, fn: Callable[[V], T]) -> Result[T]:
        """对 value 应用函数，失败则原样返回。"""
        if self.is_fail:
            return Result.fail(self.error)  # type: ignore
        return Result.ok(fn(self.value))  # type: ignore

    def __repr__(self) -> str:
        if self.is_ok:
            return f"Ok({self.value!r})"
        return f"Err({self.error!r})"


# ============================================================
# 泛型函数
# ============================================================
def first(items: list[T]) -> T | None:
    """泛型函数：安全地获取列表第一个元素。"""
    return items[0] if items else None


def merge_dicts(a: dict[K, V], b: dict[K, V]) -> dict[K, V]:
    """泛型函数：合并两个字典，b 覆盖 a 的重复键。"""
    return {**a, **b}


def group_by(items: list[T], key: Callable[[T], K]) -> dict[K, list[T]]:
    """泛型函数：按 key 分组。"""
    result: dict[K, list[T]] = {}
    for item in items:
        k = key(item)
        result.setdefault(k, []).append(item)
    return result


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 13：泛型编程")
    print("=" * 60)

    # 泛型 Repository
    user_repo: Repository[User] = Repository()
    user_repo.add(User(name="Alice", email="alice@corp.com"))
    user_repo.add(User(name="Bob", email="bob@corp.com"))
    print(f"用户数: {user_repo.count()}")
    user = user_repo.get(1)
    print(f"ID=1: {user}")

    product_repo: Repository[Product] = Repository()
    product_repo.add(Product(sku="LT-01", price=9999))
    print(f"商品数: {product_repo.count()}")

    # 泛型查找
    matches = user_repo.find(lambda u: "alice" in u.email.lower())
    print(f"查找结果: {matches}")

    # Result 容器
    print("\n--- Result 容器 ---")

    def divide(a: float, b: float) -> Result[float]:
        if b == 0:
            return Result.fail("除数不能为零")
        return Result.ok(a / b)

    r1 = divide(10, 2)
    r2 = divide(10, 0)
    print(f"10/2 = {r1}")
    print(f"10/0 = {r2}")
    print(f"r1.is_ok={r1.is_ok}, unwrap={r1.unwrap()}")
    print(f"r2.unwrap_or(-1)={r2.unwrap_or(-1)}")

    # map 链式操作
    r3 = divide(100, 3).map(lambda x: round(x, 2))
    print(f"100/3 取两位小数: {r3}")

    # 泛型函数
    print("\n--- 泛型函数 ---")
    items = [1, 2, 3, 4, 5]
    print(f"first([]): {first([])}")
    print(f"first({items}): {first(items)}")

    dict_a = {"x": 1, "y": 2}
    dict_b = {"y": 99, "z": 3}
    print(f"merge: {merge_dicts(dict_a, dict_b)}")

    users = [
        User(name="A", email="a@x.com"),
        User(name="B", email="b@x.com"),
        User(name="C", email="a@x.com"),
    ]
    grouped = group_by(users, key=lambda u: u.email.split("@")[1])
    print(f"按邮箱域名分组: { {k: [u.name for u in v] for k, v in grouped.items()} }")


if __name__ == "__main__":
    demo()
