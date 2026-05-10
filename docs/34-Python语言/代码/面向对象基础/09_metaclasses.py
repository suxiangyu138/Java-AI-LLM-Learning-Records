"""
模块 09：元类（Metaclasses）。

覆盖知识点：
    - type 动态创建类
    - 自定义元类：控制类的创建过程
    - __new__ 与 __init__ 在元类中的区别
    - 元类实战：自动注册、接口校验、单例
"""

from __future__ import annotations

import inspect
from typing import Any, ClassVar


# ============================================================
# 基础：type 动态创建类
# ============================================================
# 等价于 class Dog: pass
Dog = type("Dog", (), {"bark": lambda self: "Woof!"})


# ============================================================
# 实战一：自动注册元类（插件系统）
# ============================================================
class PluginRegistryMeta(type):
    """元类：所有使用此元类的类自动注册到插件表。"""

    registry: ClassVar[dict[str, type]] = {}

    def __new__(mcs, name: str, bases: tuple[type, ...], namespace: dict[str, Any]) -> type:
        cls = super().__new__(mcs, name, bases, namespace)
        # 跳过基类本身
        if name != "BasePlugin":
            mcs.registry[name] = cls
        return cls


class BasePlugin(metaclass=PluginRegistryMeta):
    """插件基类 —— 子类自动注册。"""

    def run(self) -> str:
        raise NotImplementedError


class PDFPlugin(BasePlugin):
    def run(self) -> str:
        return "Exporting PDF..."


class CSVPlugin(BasePlugin):
    def run(self) -> str:
        return "Exporting CSV..."


class JSONPlugin(BasePlugin):
    def run(self) -> str:
        return "Exporting JSON..."


# ============================================================
# 实战二：接口校验元类
# ============================================================
class InterfaceEnforcerMeta(type):
    """
    强制子类实现标注了 @abstractmethod 的方法（在类定义时检查，而非运行时）。

    比 ABC 更严格 —— 定义类时就报错，不用等到实例化。
    """

    def __new__(mcs, name: str, bases: tuple[type, ...], namespace: dict[str, Any]) -> type:
        cls = super().__new__(mcs, name, bases, namespace)

        # 仅在子类上校验（声明 __required_methods__ 的基类本身放过）
        if "__required_methods__" not in namespace:
            for base in bases:
                required: set[str] = getattr(base, "__required_methods__", set())
                for method_name in required:
                    if method_name not in namespace:
                        raise TypeError(
                            f"类 {name} 必须实现方法 '{method_name}'"
                            f"（由 {base.__name__} 声明）"
                        )
        return cls


class RESTClient(metaclass=InterfaceEnforcerMeta):
    """REST 客户端接口 —— 子类必须实现所有标记的方法。"""

    __required_methods__ = {"get", "post", "delete"}


# 下面这个类定义会报错（取消注释验证）：
# class BrokenClient(RESTClient):
#     def get(self, url): return "ok"
#     # 缺少 post, delete


class HTTPClient(RESTClient):
    def get(self, url: str) -> str:
        return f"GET {url} → 200"

    def post(self, url: str, data: dict) -> str:
        return f"POST {url} → 201"

    def delete(self, url: str) -> str:
        return f"DELETE {url} → 204"


# ============================================================
# 实战三：单例元类
# ============================================================
class SingletonMeta(type):
    """将任意类变为单例的元类。"""

    _instances: dict[type, object] = {}

    def __call__(cls, *args: Any, **kwargs: Any) -> object:
        if cls not in cls._instances:
            cls._instances[cls] = super().__call__(*args, **kwargs)
        return cls._instances[cls]


class DatabasePool(metaclass=SingletonMeta):
    """数据库连接池 —— 全局唯一实例。"""

    def __init__(self, dsn: str = "default") -> None:
        self.dsn = dsn
        print(f"[DBPool] 初始化 {dsn}")


# ============================================================
# 实战四：属性类型校验元类
# ============================================================
class TypedMeta(type):
    """
    属性类型校验元类 —— 在类定义时收集带类型注解的属性，
    实例化时自动校验类型。
    """

    def __new__(mcs, name: str, bases: tuple[type, ...], namespace: dict[str, Any]) -> type:
        cls = super().__new__(mcs, name, bases, namespace)
        cls.__annotations_validated__ = True
        return cls

    def __call__(cls, *args: Any, **kwargs: Any) -> object:
        instance = super().__call__(*args, **kwargs)
        hints = getattr(cls, "__annotations__", {})
        for attr, expected_type in hints.items():
            if attr.startswith("_"):
                continue
            value = getattr(instance, attr, None)
            if value is not None and not isinstance(value, expected_type):
                raise TypeError(
                    f"{cls.__name__}.{attr}: 期望 {expected_type}, 收到 {type(value)}"
                )
        return instance


class TypedDTO(metaclass=TypedMeta):
    """使用类型校验元类的 DTO。"""

    name: str
    age: int


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 09：元类（Metaclasses）")
    print("=" * 60)

    # 1. type 动态创建
    d = Dog()
    print(f"type 动态创建类: {type(d).__name__} → {d.bark()}")

    # 2. 自动注册元类
    print(f"\n插件注册表: {list(PluginRegistryMeta.registry.keys())}")
    for name, cls in PluginRegistryMeta.registry.items():
        print(f"  {name}: {cls().run()}")

    # 3. 接口校验元类
    client = HTTPClient()
    print(f"\n接口校验: {client.get('/api/users')}")

    # 4. 单例元类
    print("\n--- 单例元类 ---")
    pool1 = DatabasePool("postgresql://prod")
    pool2 = DatabasePool("postgresql://staging")  # 不会重新初始化
    print(f"pool1 is pool2: {pool1 is pool2}, dsn={pool2.dsn}")

    # 5. 类型校验元类
    print("\n--- 类型校验 DTO ---")
    try:
        valid = TypedDTO()
        valid.name = "Alice"
        valid.age = 30
        print(f"valid: {valid.name}, {valid.age}")
        # valid.age = "thirty"  # 会触发 TypeError
    except TypeError as e:
        print(f"类型校验拦截: {e}")


if __name__ == "__main__":
    demo()
