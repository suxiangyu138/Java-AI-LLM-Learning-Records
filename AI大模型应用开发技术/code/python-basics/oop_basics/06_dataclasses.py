"""
模块 06：数据类（dataclasses）。

覆盖知识点：
    - @dataclass 装饰器：自动生成 __init__ / __repr__ / __eq__
    - field() 定制字段
    - __post_init__ 初始化后处理
    - frozen=True 不可变实例
    - 与普通类的对比
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import ClassVar


# ============================================================
# 基础 dataclass
# ============================================================
@dataclass
class GeoPoint:
    """
    地理坐标 —— 最简洁的数据类。

    自动生成 __init__、__repr__、__eq__。
    """

    latitude: float
    longitude: float

    def is_valid(self) -> bool:
        """自定义方法依然可以自由添加。"""
        return -90 <= self.latitude <= 90 and -180 <= self.longitude <= 180


# ============================================================
# field() 定制字段
# ============================================================
@dataclass
class Product:
    """
    商品 —— 演示 field() 的各种用法。

    field() 参数说明：
        default: 默认值
        default_factory: 默认值工厂函数（用于可变类型）
        repr: 是否在 __repr__ 中显示
        compare: 是否参与 __eq__ 比较
    """

    sku: str
    name: str
    price: float
    stock: int = 0  # 带默认值的字段必须放在无默认值字段之后
    tags: list[str] = field(default_factory=list)  # 可变默认值必须用 default_factory
    created_at: datetime = field(
        default_factory=datetime.now,
        repr=False,  # repr 中不显示
    )
    # 类变量不属于 dataclass 字段，不会被包含在 __init__ 中
    currency: ClassVar[str] = "CNY"

    def __post_init__(self) -> None:
        """__init__ 之后自动调用，用于额外的校验或计算。"""
        if self.price < 0:
            raise ValueError(f"价格不能为负: {self.price}")
        # 自动计算总价值（缓存）
        self._inventory_value = self.price * self.stock

    @property
    def inventory_value(self) -> float:
        """库存总价值（派生属性）。"""
        return self._inventory_value


# ============================================================
# frozen=True：不可变数据类
# ============================================================
@dataclass(frozen=True)
class ImmutableConfig:
    """
    不可变配置 —— 创建后所有字段只读。

    适合用作配置对象、DTO（数据传输对象）等。
    """

    host: str
    port: int = 8080
    debug: bool = False

    def connection_url(self) -> str:
        return f"http://{self.host}:{self.port}"


# ============================================================
# 对比：普通类 vs dataclass
# ============================================================
class ManualPoint:
    """手动实现的数据类 —— 对比 dataclass 减少的样板代码。"""

    def __init__(self, x: float, y: float) -> None:
        self.x = x
        self.y = y

    def __repr__(self) -> str:
        return f"ManualPoint(x={self.x}, y={self.y})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, ManualPoint):
            return NotImplemented
        return self.x == other.x and self.y == other.y

    def __hash__(self) -> int:
        return hash((self.x, self.y))


# dataclass 三行就等价于上面十几行
@dataclass(frozen=True)
class AutoPoint:
    x: float
    y: float


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 06：数据类（dataclasses）")
    print("=" * 60)

    # 基础用法
    p1 = GeoPoint(31.23, 121.47)
    p2 = GeoPoint(31.23, 121.47)
    print(f"p1: {p1}, valid={p1.is_valid()}")
    print(f"p1 == p2: {p1 == p2}")  # 自动生成的 __eq__
    print(f"GeoPoint(91, 0).is_valid(): {GeoPoint(91, 0).is_valid()}")

    # field() 定制
    laptop = Product(
        sku="LT-001",
        name="MacBook Pro",
        price=14999,
        stock=10,
        tags=["electronics", "laptop"],
    )
    print(f"\n{laptop}")
    print(f"库存价值: ¥{laptop.inventory_value:,.0f}")

    # frozen=True
    config = ImmutableConfig(host="0.0.0.0", port=8000)
    print(f"\n{config}")
    print(f"连接: {config.connection_url()}")
    try:
        config.port = 9090  # frozen=True，修改会抛异常
    except AttributeError as e:
        print(f"FrozenDataclass 修改拦截: {e}")

    # 等价性对比
    ap1, ap2 = AutoPoint(1, 2), AutoPoint(1, 2)
    mp1, mp2 = ManualPoint(1, 2), ManualPoint(1, 2)
    print(f"\nAutoPoint == : {ap1 == ap2}, hash=({hash(ap1)}, {hash(ap2)})")
    print(f"ManualPoint ==: {mp1 == mp2}, hash=({hash(mp1)}, {hash(mp2)})")


if __name__ == "__main__":
    demo()
