"""
模块 03：继承。

覆盖知识点：
    - 单继承
    - super() 调用父类方法
    - 多层继承
    - 多重继承与 MRO（方法解析顺序）
    - Mixin 模式
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import ClassVar


# ============================================================
# 单继承
# ============================================================
class Vehicle:
    """交通工具基类。"""

    def __init__(self, brand: str, model: str, year: int) -> None:
        self.brand = brand
        self.model = model
        self.year = year

    def description(self) -> str:
        return f"{self.year} {self.brand} {self.model}"

    def start(self) -> str:
        return "Vehicle engine starting..."


class Car(Vehicle):
    """汽车 —— 单继承自 Vehicle。"""

    WHEELS: ClassVar[int] = 4

    def __init__(self, brand: str, model: str, year: int, doors: int = 4) -> None:
        super().__init__(brand, model, year)  # 调用父类 __init__
        self.doors = doors

    def start(self) -> str:
        """重写父类方法，同时复用父类逻辑。"""
        parent_msg = super().start()
        return f"{parent_msg} → Car {self.model} is ready."

    def honk(self) -> str:
        return "Beep beep!"


class ElectricCar(Car):
    """电动车 —— 多层继承 Vehicle → Car → ElectricCar。"""

    def __init__(
        self, brand: str, model: str, year: int, doors: int = 4, battery_kwh: float = 75
    ) -> None:
        super().__init__(brand, model, year, doors)
        self.battery_kwh = battery_kwh

    def start(self) -> str:
        return f"ElectricCar {self.model}: silent start (battery: {self.battery_kwh} kWh)"


# ============================================================
# 多重继承 & Mixin
# ============================================================
class LoggingMixin:
    """日志 Mixin —— 为任何类提供日志能力。"""

    def log(self, message: str) -> None:
        print(f"[LOG {self.__class__.__name__}] {message}")


class SerializationMixin:
    """序列化 Mixin —— 为任何类提供序列化能力。"""

    def to_dict(self) -> dict[str, object]:
        result: dict[str, object] = {}
        for key, value in self.__dict__.items():
            if not key.startswith("_"):
                result[key] = value
        return result


class SmartCar(Car, LoggingMixin, SerializationMixin):
    """
    智能汽车 —— 多重继承。

    MRO 决定了属性查找顺序，可通过 SmartCar.__mro__ 查看。
    """

    def __init__(
        self, brand: str, model: str, year: int, software_version: str = "1.0"
    ) -> None:
        super().__init__(brand, model, year)
        self.software_version = software_version
        self.log(f"SmartCar {model} initialized")


# ============================================================
# 抽象基类作接口
# ============================================================
class PaymentProcessor(ABC):
    """支付处理器抽象基类 —— 定义子类必须实现的方法。"""

    @abstractmethod
    def pay(self, amount: float) -> bool:
        """执行支付，返回是否成功。"""
        ...

    @abstractmethod
    def refund(self, transaction_id: str) -> bool:
        """退款。"""
        ...


class WechatPay(PaymentProcessor):
    """微信支付实现。"""

    def pay(self, amount: float) -> bool:
        print(f"WechatPay: 支付 ¥{amount:.2f}")
        return True

    def refund(self, transaction_id: str) -> bool:
        print(f"WechatPay: 退款 {transaction_id}")
        return True


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 03：继承")
    print("=" * 60)

    # 单继承 & 方法重写
    car = Car("Toyota", "Camry", 2025)
    print(car.description())
    print(car.start())

    # 多层继承
    tesla = ElectricCar("Tesla", "Model 3", 2025, battery_kwh=82)
    print(tesla.description())
    print(tesla.start())

    # isinstance / issubclass
    print(f"\ntesla is Vehicle: {isinstance(tesla, Vehicle)}")
    print(f"ElectricCar is Car: {issubclass(ElectricCar, Car)}")

    # 多重继承 & MRO
    smart = SmartCar("NIO", "ET7", 2025, software_version="2.3.1")
    print(f"\nSmartCar MRO: {[c.__name__ for c in SmartCar.__mro__]}")
    print(f"to_dict: {smart.to_dict()}")

    # 抽象基类
    processor = WechatPay()
    processor.pay(99.90)
    processor.refund("TXN-001")
    print(f"WechatPay is PaymentProcessor: {isinstance(processor, PaymentProcessor)}")


if __name__ == "__main__":
    demo()
