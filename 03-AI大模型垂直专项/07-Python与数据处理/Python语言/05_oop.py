"""
单元 05：面向对象编程（OOP）

覆盖：class/__init__/self、继承/多态、@property、@classmethod/@staticmethod、
__str__/__repr__、dataclass、抽象基类.
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field


# ============================================================
# 1. 基本类
# ============================================================

class User:
    """用户模型.

    Attributes:
        name: 用户名.
        age: 年龄.
        _email: 邮箱（受保护属性，约定用 _ 前缀）.
    """

    # 类属性（所有实例共享）
    species: str = "Homo sapiens"

    def __init__(self, name: str, age: int, email: str) -> None:
        """初始化实例.

        Args:
            name: 用户名.
            age: 年龄.
            email: 邮箱地址.
        """
        self.name = name       # 公开属性
        self.age = age
        self._email = email    # 约定：_ 前缀 = protected

    # ---- 属性访问器（property） ----
    @property
    def email(self) -> str:
        """只读属性：邮箱."""
        return self._email

    @property
    def is_adult(self) -> bool:
        """计算属性."""
        return self.age >= 18

    # ---- 魔法方法（dunder methods） ----
    def __str__(self) -> str:
        """用户友好的字符串表示（print / str 调用）."""
        return f"User(name={self.name}, age={self.age})"

    def __repr__(self) -> str:
        """开发者友好的字符串表示（调试 / repr 调用）."""
        return f"User(name={self.name!r}, age={self.age!r}, email={self._email!r})"

    def __eq__(self, other: object) -> bool:
        """相等性比较."""
        if not isinstance(other, User):
            return NotImplemented
        return self.name == other.name and self.age == other.age


# ============================================================
# 2. 继承与多态
# ============================================================

class Animal(ABC):
    """抽象基类."""

    @abstractmethod
    def speak(self) -> str:
        """子类必须实现."""
        ...

    def describe(self) -> str:
        """普通方法，子类可继承."""
        return f"I am a {self.__class__.__name__}"


class Dog(Animal):
    """Dog 实现 Animal."""

    def speak(self) -> str:
        return "Woof!"


class Cat(Animal):
    """Cat 实现 Animal."""

    def speak(self) -> str:
        return "Meow!"


# ============================================================
# 3. @classmethod 与 @staticmethod
# ============================================================

class DateUtils:
    """演示 classmethod / staticmethod."""

    value: int = 42

    @classmethod
    def from_string(cls, date_string: str) -> str:
        """类方法：第一个参数是类本身（cls），可访问类属性."""
        return f"{cls.value}: {date_string}"

    @staticmethod
    def is_valid_date(date_string: str) -> bool:
        """静态方法：无需类/实例上下文，纯工具函数."""
        parts = date_string.split("-")
        return len(parts) == 3 and all(p.isdigit() for p in parts)


# ============================================================
# 4. dataclass（Python 3.7+，企业首选数据结构）
# ============================================================

@dataclass
class Product:
    """商品数据类.

    自动生成 __init__, __repr__, __eq__ 等方法.
    """
    name: str
    price: float
    stock: int = 0

    @property
    def is_available(self) -> bool:
        return self.stock > 0


@dataclass(frozen=True)
class Point:
    """不可变数据类（frozen=True）."""
    x: float
    y: float

    def distance_from_origin(self) -> float:
        return (self.x ** 2 + self.y ** 2) ** 0.5


@dataclass
class Order:
    """带 field() 高级选项的数据类."""
    order_id: str
    items: list[Product] = field(default_factory=list)  # 可变默认值必须用 default_factory

    @property
    def total(self) -> float:
        return sum(item.price for item in self.items)


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 05：面向对象编程")
    print("=" * 50)

    # 基本类
    alice = User("Alice", 30, "alice@example.com")
    bob = User("Bob", 16, "bob@example.com")
    print(alice)                        # __str__
    print(repr(alice))                  # __repr__
    print(f"  email={alice.email}, is_adult={alice.is_adult}")
    print(f"  bob.is_adult={bob.is_adult}")
    print(f"  alice == bob: {alice == bob}")
    print(f"  species: {User.species}")  # 类属性

    # 继承/多态
    animals: list[Animal] = [Dog(), Cat()]
    for a in animals:
        print(f"  {a.describe()} → {a.speak()}")

    # classmethod / staticmethod
    print(f"\n  {DateUtils.from_string('2026-05-07')}")
    print(f"  is_valid: {DateUtils.is_valid_date('2026-05-07')}")

    # dataclass
    print()
    laptop = Product(name="Laptop", price=999.99, stock=10)
    print(f"  {laptop}, available={laptop.is_available}")

    p = Point(3.0, 4.0)
    print(f"  Point distance: {p.distance_from_origin()}")

    order = Order(order_id="ORD-001", items=[laptop, Product(name="Mouse", price=29.99, stock=5)])
    print(f"  Order total: ${order.total:.2f}")
