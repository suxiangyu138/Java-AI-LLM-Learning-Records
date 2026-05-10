"""
模块 04：多态。

覆盖知识点：
    - 鸭子类型（Duck Typing）
    - 协议（Protocol）—— 结构化子类型
    - 方法重写实现多态
    - 函数接受抽象类型，运行时传入任意子类
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from math import pi
from typing import Protocol, runtime_checkable


# ============================================================
# 方式一：继承 + 方法重写（经典多态）
# ============================================================
class Shape(ABC):
    """形状抽象基类。"""

    @abstractmethod
    def area(self) -> float:
        ...

    @abstractmethod
    def perimeter(self) -> float:
        ...

    def describe(self) -> str:
        """具体方法：子类可继承也可重写。"""
        return f"{self.__class__.__name__}: area={self.area():.2f}, perimeter={self.perimeter():.2f}"


class Circle(Shape):
    def __init__(self, radius: float) -> None:
        self.radius = radius

    def area(self) -> float:
        return pi * self.radius ** 2

    def perimeter(self) -> float:
        return 2 * pi * self.radius


class Rectangle(Shape):
    def __init__(self, width: float, height: float) -> None:
        self.width = width
        self.height = height

    def area(self) -> float:
        return self.width * self.height

    def perimeter(self) -> float:
        return 2 * (self.width + self.height)


class Triangle(Shape):
    def __init__(self, a: float, b: float, c: float) -> None:
        self.a = a
        self.b = b
        self.c = c

    def area(self) -> float:
        s = self.perimeter() / 2  # 半周长
        return (s * (s - self.a) * (s - self.b) * (s - self.c)) ** 0.5

    def perimeter(self) -> float:
        return self.a + self.b + self.c


# ============================================================
# 方式二：Protocol — 结构化子类型（无需继承）
# ============================================================
@runtime_checkable
class Renderable(Protocol):
    """任何实现了 render() 方法的对象都满足 Renderable 协议。"""

    def render(self) -> str:
        ...


class MarkdownDoc:
    """不继承任何基类，但实现了 render() → 自动满足 Renderable。"""

    def __init__(self, content: str) -> None:
        self.content = content

    def render(self) -> str:
        return f"# Markdown\n\n{self.content}"


class JSONReport:
    def __init__(self, data: dict[str, object]) -> None:
        self.data = data

    def render(self) -> str:
        import json

        return json.dumps(self.data, ensure_ascii=False, indent=2)


# ============================================================
# 统一处理多态对象
# ============================================================
def print_shape_info(shapes: list[Shape]) -> None:
    """接收 Shape 列表，运行时调用各子类的具体实现。"""
    for shape in shapes:
        print(f"  {shape.describe()}")


def export_all(renderables: list[Renderable]) -> None:
    """接收任意满足 Renderable 协议的对象列表。"""
    for r in renderables:
        print(f"\n--- {r.__class__.__name__} ---")
        print(r.render())


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 04：多态")
    print("=" * 60)

    # 经典多态
    shapes: list[Shape] = [
        Circle(5),
        Rectangle(4, 6),
        Triangle(3, 4, 5),
    ]
    print("经典多态（抽象基类）:")
    print_shape_info(shapes)

    # 鸭子类型 / Protocol
    print("\n鸭子类型（Protocol）:")
    docs: list[Renderable] = [
        MarkdownDoc("Hello **World**"),
        JSONReport({"status": "ok", "count": 42}),
    ]
    export_all(docs)

    # runtime_checkable 可以用于 isinstance
    print(f"\nMarkdownDoc is Renderable: {isinstance(MarkdownDoc(''), Renderable)}")


if __name__ == "__main__":
    demo()
