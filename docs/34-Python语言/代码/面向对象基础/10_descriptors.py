"""
模块 10：描述符（Descriptors）。

覆盖知识点：
    - 描述符协议：__get__ / __set__ / __delete__
    - 数据描述符 vs 非数据描述符
    - 查找优先级
    - 实战：类型校验字段、惰性属性、缓存属性
"""

from __future__ import annotations

import weakref
from typing import Any, Callable, Generic, TypeVar

T = TypeVar("T")


# ============================================================
# 实战一：类型校验描述符
# ============================================================
class TypedField(Generic[T]):
    """
    强类型字段描述符 —— 替代 property + 重复校验代码。

    用法：price = TypedField(float, default=0.0)
    """

    def __init__(self, field_type: type[T], default: T | None = None) -> None:
        self.field_type = field_type
        self.default = default
        self._name: str = ""  # 在 __set_name__ 中赋值

    def __set_name__(self, owner: type, name: str) -> None:
        """Python 3.6+ 自动调用，获取属性在所属类中的名称。"""
        self._name = name

    def __get__(self, instance: object, owner: type) -> T:
        if instance is None:
            return self  # 类级别访问，返回描述符本身
        return instance.__dict__.get(self._name, self.default)

    def __set__(self, instance: object, value: T) -> None:
        # 容忍合法的隐式类型转换：int → float, bool → int
        actual = value
        if self.field_type is float and isinstance(value, int):
            actual = float(value)
        elif self.field_type is int and isinstance(value, bool):
            raise TypeError(
                f"{instance.__class__.__name__}.{self._name}: "
                f"期望 int, 收到 bool（请显式转换为 int）"
            )
        if not isinstance(actual, self.field_type):
            raise TypeError(
                f"{instance.__class__.__name__}.{self._name}: "
                f"期望 {self.field_type.__name__}, 收到 {type(value).__name__}"
            )
        instance.__dict__[self._name] = actual


class PositiveNumber(TypedField[float]):
    """正数描述符 —— 在 TypedField 基础上增加范围校验。"""

    def __init__(self, default: float = 0.0) -> None:
        super().__init__(float, default)

    def __set__(self, instance: object, value: float) -> None:
        super().__set__(instance, value)
        if value < 0:
            raise ValueError(
                f"{instance.__class__.__name__}.{self._name}: 不允许负值, 收到 {value}"
            )


class Product:
    """使用描述符的商品类 —— 无需每写一个字段就定义一对 property。"""

    name = TypedField(str, default="")
    price = PositiveNumber(default=0.0)
    stock = TypedField(int, default=0)

    def __init__(self, name: str, price: float, stock: int = 0) -> None:
        self.name = name
        self.price = price
        self.stock = stock

    def __repr__(self) -> str:
        return f"Product(name={self.name!r}, price={self.price}, stock={self.stock})"


# ============================================================
# 实战二：惰性属性（Lazy Property）
# ============================================================
class LazyProperty:
    """
    惰性属性描述符 —— 首次访问时计算，之后缓存结果。

    适合：数据库查询、文件读取、复杂计算等昂贵操作。
    """

    def __init__(self, func: Callable[..., T]) -> None:
        self.func = func
        self._name = func.__name__

    def __set_name__(self, owner: type, name: str) -> None:
        self._name = name

    def __get__(self, instance: object, owner: type) -> T:
        if instance is None:
            return self
        value = self.func(instance)
        # 计算后将结果写入实例 __dict__，后续直接从 __dict__ 读取
        instance.__dict__[self._name] = value
        return value


class Report:
    """报表类 —— 演示惰性计算。"""

    def __init__(self, data: list[int]) -> None:
        self.data = data

    @LazyProperty
    def average(self) -> float:
        """首次访问才计算均值，之后缓存。"""
        print("  [计算 average...]")
        return sum(self.data) / len(self.data) if self.data else 0.0

    @LazyProperty
    def total(self) -> float:
        print("  [计算 total...]")
        return sum(self.data)


# ============================================================
# 实战三：Django 风格的 CachedProperty
# ============================================================
class CachedProperty:
    """
    带过期时间的缓存属性 —— 缓存指定秒数后自动刷新。

    比 LazyProperty 进阶：支持 TTL 过期。
    """

    def __init__(self, func: Callable[..., T] | None = None, *, ttl_seconds: float = 60) -> None:
        self.func = func
        self.ttl = ttl_seconds

    def __call__(self, func: Callable[..., T]) -> CachedProperty[T]:
        """支持 @CachedProperty(ttl_seconds=X) 带参数装饰器语法。"""
        self.func = func
        return self

    def __set_name__(self, owner: type, name: str) -> None:
        self._name = name

    def __get__(self, instance: object, owner: type) -> T:
        if instance is None:
            return self
        if self.func is None:
            raise RuntimeError("CachedProperty 未绑定函数，请使用 @CachedProperty 或 @CachedProperty(ttl_seconds=X) 装饰器")

        import time

        cache = instance.__dict__.get(f"__cache_{self._name}")
        now = time.time()
        if cache is not None:
            ts, value = cache
            if now - ts < self.ttl:
                return value

        value = self.func(instance)
        instance.__dict__[f"__cache_{self._name}"] = (now, value)
        return value


class WeatherService:
    """天气服务 —— 演示带 TTL 的缓存属性。"""

    def __init__(self, city: str) -> None:
        self.city = city

    @CachedProperty(ttl_seconds=0.5)
    def temperature(self) -> float:
        """模拟 API 调用，0.5 秒内重复访问使用缓存。"""
        import random

        print(f"  [API 调用] 获取 {self.city} 温度...")
        return round(random.uniform(20, 35), 1)


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 10：描述符（Descriptors）")
    print("=" * 60)

    # 类型校验描述符
    print("--- 类型校验描述符 ---")
    p = Product("MacBook", 14999, 10)
    print(p)
    try:
        p.price = -100
    except ValueError as e:
        print(f"校验拦截: {e}")
    try:
        p.name = 123  # type: ignore
    except TypeError as e:
        print(f"类型拦截: {e}")

    # 惰性属性
    print("\n--- 惰性属性 ---")
    r = Report([85, 92, 78, 95, 88])
    print("Report 创建完成（尚未计算）")
    print(f"average = {r.average}")  # 首次访问 → 计算
    print(f"average = {r.average}")  # 再次访问 → 直接读缓存
    print(f"total   = {r.total}")    # 首次 → 计算

    # 带 TTL 的缓存属性
    print("\n--- TTL 缓存属性 ---")
    ws = WeatherService("Shanghai")
    print(f"温度: {ws.temperature}")
    print(f"温度（缓存命中）: {ws.temperature}")
    print("等待 0.6 秒使缓存过期...")
    import time
    time.sleep(0.6)
    print(f"温度（缓存过期后刷新）: {ws.temperature}")


if __name__ == "__main__":
    demo()
