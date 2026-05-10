"""
模块 15：SOLID 原则实战。

覆盖知识点：
    - S：单一职责（Single Responsibility）
    - O：开闭原则（Open/Closed）
    - L：里氏替换（Liskov Substitution）
    - I：接口隔离（Interface Segregation）
    - D：依赖倒置（Dependency Inversion）
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Protocol


# ============================================================
# S：单一职责 —— 反例 vs 正例
# ============================================================
# ❌ 反例：一个类做了三件事（数据、格式化、持久化）
class EmployeeViolation:
    def __init__(self, name: str, salary: float) -> None:
        self.name = name
        self.salary = salary

    def to_json(self) -> str:
        import json; return json.dumps({"name": self.name, "salary": self.salary})

    def save_to_db(self) -> None:
        print(f"SAVE {self.name} TO DB")


# ✅ 正例：职责分离
@dataclass
class Employee:
    """纯数据：只有属性。"""
    name: str
    salary: float


class EmployeeSerializer:
    """序列化：只管格式化。"""

    @staticmethod
    def to_json(employee: Employee) -> str:
        import json
        return json.dumps({"name": employee.name, "salary": employee.salary})


class EmployeeRepository:
    """持久化：只管存储。"""

    def save(self, employee: Employee) -> None:
        print(f"[DB] 保存 {employee.name}")


# ============================================================
# O：开闭原则 —— 对扩展开放，对修改关闭
# ============================================================
class DiscountStrategy(ABC):
    """折扣策略 —— 新增策略无需修改现有代码。"""

    @abstractmethod
    def apply(self, price: float) -> float:
        ...


class NoDiscount(DiscountStrategy):
    def apply(self, price: float) -> float:
        return price


class PercentageDiscount(DiscountStrategy):
    def __init__(self, percent: float) -> None:
        self.percent = percent

    def apply(self, price: float) -> float:
        return price * (1 - self.percent / 100)


class ThresholdDiscount(DiscountStrategy):
    """满减折扣 —— 新增策略，无需改动任何已有代码。"""

    def __init__(self, threshold: float, discount: float) -> None:
        self.threshold = threshold
        self.discount = discount

    def apply(self, price: float) -> float:
        return price - self.discount if price >= self.threshold else price


class PriceCalculator:
    """价格计算器 —— 接受任意折扣策略，对扩展开放。"""

    def __init__(self, discount: DiscountStrategy) -> None:
        self._discount = discount

    def calculate(self, price: float) -> float:
        return self._discount.apply(price)


# ============================================================
# L：里氏替换 —— 子类必须可以替换父类
# ============================================================
class Rectangle:
    def __init__(self, width: float, height: float) -> None:
        self.width = width
        self.height = height

    def area(self) -> float:
        return self.width * self.height


class Square(Rectangle):
    """
    ✅ 正方形 is-a 矩形，且没有破坏父类契约。

    注意：这里通过属性同步 width/height 来保持正方形不变式，
    如果父类期望 width/height 独立可变，继承就会出问题。
    """

    def __init__(self, side: float) -> None:
        super().__init__(side, side)

    # 不重写 area() —— 父类的实现已经正确


# ❌ 反例：违反里氏替换
class BirdViolation:
    def fly(self) -> str:
        return "Flying"


class OstrichViolation(BirdViolation):
    """鸵鸟不会飞，但继承了 fly() —— 调用会崩溃。"""

    def fly(self) -> str:
        raise NotImplementedError("Ostriches can't fly!")


# ✅ 正例：用抽象隔离差异
class Bird(ABC):
    @abstractmethod
    def move(self) -> str:
        ...


class Sparrow(Bird):
    def move(self) -> str:
        return "Flying"


class Ostrich(Bird):
    def move(self) -> str:
        return "Running"


# ============================================================
# I：接口隔离 —— 不强迫实现不需要的方法
# ============================================================
# ❌ 反例：胖接口
class WorkerViolation(ABC):
    @abstractmethod
    def work(self) -> None: ...
    @abstractmethod
    def eat(self) -> None: ...
    @abstractmethod
    def sleep(self) -> None: ...


# ✅ 正例：拆分小接口
class Workable(Protocol):
    def work(self) -> None: ...


class Eatable(Protocol):
    def eat(self) -> None: ...


class Sleepable(Protocol):
    def sleep(self) -> None: ...


class Human:
    def work(self) -> None:
        print("Human working")

    def eat(self) -> None:
        print("Human eating")

    def sleep(self) -> None:
        print("Human sleeping")


class Robot:
    """机器人只需实现 Workable，不需要 eat/sleep。"""

    def work(self) -> None:
        print("Robot working 24/7")


# ============================================================
# D：依赖倒置 —— 依赖抽象而非具体实现
# ============================================================
class MessageSender(ABC):
    """抽象：消息发送器。"""

    @abstractmethod
    def send(self, recipient: str, message: str) -> bool:
        ...


class EmailSender(MessageSender):
    def send(self, recipient: str, message: str) -> bool:
        print(f"[Email] {recipient}: {message}")
        return True


class SMSSender(MessageSender):
    def send(self, recipient: str, message: str) -> bool:
        print(f"[SMS] {recipient}: {message}")
        return True


class NotificationService:
    """
    通知服务 —— 依赖抽象 MessageSender，而非具体 EmailSender。

    更换发送方式只需注入不同实现，NotificationService 代码零修改。
    """

    def __init__(self, sender: MessageSender) -> None:
        self._sender = sender

    def notify(self, recipient: str, message: str) -> None:
        success = self._sender.send(recipient, message)
        if not success:
            print("  发送失败，进入重试队列")


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 15：SOLID 原则实战")
    print("=" * 60)

    # S: 单一职责
    print("--- S: 单一职责 ---")
    emp = Employee("Alice", 150_000)
    print(f"序列化: {EmployeeSerializer.to_json(emp)}")
    EmployeeRepository().save(emp)

    # O: 开闭原则
    print("\n--- O: 开闭原则 ---")
    for strategy in [
        NoDiscount(),
        PercentageDiscount(20),
        ThresholdDiscount(100, 15),  # 满100减15（新增策略，零改动）
    ]:
        calc = PriceCalculator(strategy)
        print(f"  {strategy.__class__.__name__}: ¥200 → ¥{calc.calculate(200):.2f}")

    # L: 里氏替换
    print("\n--- L: 里氏替换 ---")
    square = Square(5)
    print(f"Square area: {square.area()}")
    print(f"Liskov check: {isinstance(square, Rectangle)}")

    birds: list[Bird] = [Sparrow(), Ostrich()]
    for b in birds:
        print(f"  {b.__class__.__name__}: {b.move()}")

    # I: 接口隔离
    print("\n--- I: 接口隔离 ---")
    Human().work()
    Robot().work()  # 不需要 eat/sleep

    # D: 依赖倒置
    print("\n--- D: 依赖倒置 ---")
    # 注入 Email → 换成 SMS 只需改一处
    for sender_cls in (EmailSender, SMSSender):
        service = NotificationService(sender_cls())
        service.notify("user@corp.com", f"通过 {sender_cls.__name__} 发送的消息")

    print("\n✅ 五大原则总结：")
    print("  S: 一个类只做一件事")
    print("  O: 新增功能靠扩展，不修改已有代码")
    print("  L: 子类能无缝替换父类")
    print("  I: 接口小而专，不强迫实现不需要的方法")
    print("  D: 依赖抽象接口，不依赖具体实现")


if __name__ == "__main__":
    demo()
