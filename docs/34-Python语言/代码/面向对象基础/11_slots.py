"""
模块 11：__slots__ 内存优化。

覆盖知识点：
    - __slots__ 原理：用 tuple/descriptor 替代 __dict__
    - 内存占用对比
    - 与 property、继承的交互
    - 适用场景与限制
"""

from __future__ import annotations

import sys
from typing import ClassVar


# ============================================================
# 对比：无 slots vs 有 slots
# ============================================================
class RecordWithDict:
    """普通类 —— 每个实例带 __dict__，灵活但占内存。"""

    def __init__(self, id_: int, name: str, value: float) -> None:
        self.id = id_
        self.name = name
        self.value = value


class RecordWithSlots:
    """使用 __slots__ —— 固定属性集，省内存、更快。"""

    __slots__ = ("id", "name", "value")

    def __init__(self, id_: int, name: str, value: float) -> None:
        self.id = id_
        self.name = name
        self.value = value


# ============================================================
# 实战：ORM 风格的 Model 基类
# ============================================================
class ModelMeta(type):
    """收集 slots 定义，为 ORM Model 自动配置。"""

    def __new__(mcs, name: str, bases: tuple[type, ...], namespace: dict) -> type:
        # 收集 annotation，排除 ClassVar 和已有默认值的类变量
        from typing import ClassVar as _CV

        annotations = namespace.get("__annotations__", {})
        slots: list[str] = []
        for k, v in annotations.items():
            if k.startswith("_"):
                continue
            origin = getattr(v, "__origin__", None)
            if origin is _CV:
                continue
            if k in namespace:
                continue
            slots.append(k)
        # 继承父类的 slots
        for base in bases:
            parent_slots = getattr(base, "__slots__", ())
            slots.extend(s for s in parent_slots if s not in slots)
        namespace["__slots__"] = tuple(slots)
        return super().__new__(mcs, name, bases, namespace)


class Model(metaclass=ModelMeta):
    """ORM 基类 —— 子类只需声明类型注解，slots 自动生成。"""

    def __init__(self, **kwargs) -> None:
        for slot in self.__slots__:
            setattr(self, slot, kwargs.get(slot))

    def to_dict(self) -> dict[str, object]:
        return {slot: getattr(self, slot) for slot in self.__slots__}

    def __repr__(self) -> str:
        fields = ", ".join(f"{s}={getattr(self, s)!r}" for s in self.__slots__)
        return f"{self.__class__.__name__}({fields})"


class User(Model):
    """用户模型 —— 自动生成 __slots__，节省内存。"""

    id: int
    name: str
    email: str
    is_active: bool  # 实例字段，无默认值
    _cache: ClassVar[dict] = {}  # ClassVar 排除在 __slots__ 外


# ============================================================
# 实战：Slots + Property
# ============================================================
class Measurement:
    """
    带校验的测量值类 —— slots 可与 property 共存。

    注意：property 中存储的 _hidden 属性也必须在 __slots__ 中声明。
    """

    __slots__ = ("_value", "_unit", "_timestamp")

    def __init__(self, value: float, unit: str = "m") -> None:
        self._timestamp = __import__("time").time()
        self.value = value
        self.unit = unit

    @property
    def value(self) -> float:
        return self._value

    @value.setter
    def value(self, v: float) -> None:
        if v < 0:
            raise ValueError(f"测量值不能为负: {v}")
        self._value = v

    @property
    def unit(self) -> str:
        return self._unit

    @unit.setter
    def unit(self, u: str) -> None:
        allowed = {"m", "cm", "mm", "km"}
        if u not in allowed:
            raise ValueError(f"不支持的计量单位: {u}")
        self._unit = u

    def __repr__(self) -> str:
        return f"Measurement({self._value}{self._unit})"


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 11：__slots__ 内存优化")
    print("=" * 60)

    # 内存对比
    N = 100_000
    d1 = RecordWithDict(1, "a", 1.0)
    s1 = RecordWithSlots(1, "a", 1.0)
    dict_size = sys.getsizeof(d1) + sys.getsizeof(d1.__dict__)  # 对象本身 + __dict__
    slot_size = sys.getsizeof(s1)
    print(f"单个实例内存: __dict__={dict_size}B  |  __slots__={slot_size}B")
    print(f"节省: {(1 - slot_size / dict_size) * 100:.1f}%")
    print(f"{N:,} 个实例: __dict__≈{dict_size * N / 1024**2:.0f}MB  |  __slots__≈{slot_size * N / 1024**2:.0f}MB")

    # __slots__ 限制：不能动态添加属性
    try:
        s1.new_attr = "oops"
    except AttributeError as e:
        print(f"\n__slots__ 限制: {e}")

    # ORM Model
    print("\n--- ORM Model ---")
    user = User(id=1, name="Alice", email="alice@corp.com", is_active=True)
    print(user)
    print(f"slots: {User.__slots__}")
    print(f"to_dict: {user.to_dict()}")

    # Slots + Property
    print("\n--- Slots + Property ---")
    m = Measurement(12.5, "cm")
    print(m)
    try:
        m.value = -1
    except ValueError as e:
        print(f"Property 校验: {e}")


if __name__ == "__main__":
    demo()
