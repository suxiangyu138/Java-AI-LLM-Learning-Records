"""
模块 12：枚举（Enum）。

覆盖知识点：
    - 基础 Enum
    - IntEnum / StrEnum（Python 3.11+）/ Flag
    - auto() 自动赋值
    - 枚举方法、属性
    - 枚举与数据库映射的实战
"""

from __future__ import annotations

from enum import IntEnum, StrEnum, auto, Flag, Enum


# ============================================================
# 基础枚举
# ============================================================
class OrderStatus(Enum):
    """订单状态枚举。"""

    PENDING = "pending"
    CONFIRMED = "confirmed"
    SHIPPED = "shipped"
    DELIVERED = "delivered"
    CANCELLED = "cancelled"

    @property
    def can_transition(self) -> tuple[OrderStatus, ...]:
        """返回当前状态可以转换到的目标状态列表。"""
        _transitions: dict[OrderStatus, tuple[OrderStatus, ...]] = {
            OrderStatus.PENDING: (OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED: (OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED: (OrderStatus.DELIVERED,),
            OrderStatus.DELIVERED: (),
            OrderStatus.CANCELLED: (),
        }
        return _transitions.get(self, ())

    def can_transition_to(self, target: OrderStatus) -> bool:
        """判断能否转为目标状态。"""
        return target in self.can_transition


# ============================================================
# IntEnum：可直接与整数比较
# ============================================================
class Priority(IntEnum):
    """优先级（IntEnum 可直接与整数比较，适合 DB 存储）。"""

    LOW = 1
    NORMAL = auto()  # 自动赋值为 2
    HIGH = auto()    # 自动为 3
    CRITICAL = auto()  # 4

    @property
    def label_cn(self) -> str:
        _labels = {
            Priority.LOW: "低",
            Priority.NORMAL: "普通",
            Priority.HIGH: "高",
            Priority.CRITICAL: "紧急",
        }
        return _labels[self]


# ============================================================
# StrEnum：可以直接在字符串上下文中使用（Python 3.11+）
# ============================================================
class Environment(StrEnum):
    """运行环境（StrEnum 可直接当作字符串使用）。"""

    DEV = "development"
    STAGING = "staging"
    PROD = "production"

    @property
    def is_prod(self) -> bool:
        return self == Environment.PROD

    @classmethod
    def from_url(cls, url: str) -> Environment:
        """从 URL 推断环境。"""
        if "localhost" in url or "127.0.0.1" in url:
            return cls.DEV
        if "staging" in url:
            return cls.STAGING
        return cls.PROD


# ============================================================
# Flag：位标志（可组合）
# ============================================================
class Permission(Flag):
    """权限标志 —— 支持位运算组合。"""

    NONE = 0
    READ = auto()
    WRITE = auto()
    DELETE = auto()
    ADMIN = READ | WRITE | DELETE  # 组合权限


# ============================================================
# 实战：枚举驱动的状态机
# ============================================================
class TaskState(Enum):
    """任务状态机。"""

    TODO = auto()
    IN_PROGRESS = auto()
    REVIEW = auto()
    DONE = auto()

    _transitions: dict

    def __init_subclass__(cls, **kwargs) -> None:
        """Python 3.11+ 不需要，这里手动声明允许的转换。"""
        ...


# 注册允许的转换
TaskState.TODO._transitions = {TaskState.IN_PROGRESS}
TaskState.IN_PROGRESS._transitions = {TaskState.REVIEW, TaskState.TODO}
TaskState.REVIEW._transitions = {TaskState.DONE, TaskState.IN_PROGRESS}
TaskState.DONE._transitions = set()


class Task:
    """使用枚举状态机的任务类。"""

    def __init__(self, title: str) -> None:
        self.title = title
        self.state = TaskState.TODO

    def transition_to(self, new_state: TaskState) -> bool:
        if new_state in self.state._transitions:
            print(f"  {self.title}: {self.state.name} → {new_state.name}")
            self.state = new_state
            return True
        print(f"  {self.title}: 禁止 {self.state.name} → {new_state.name}")
        return False


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 12：枚举（Enum）")
    print("=" * 60)

    # 基础枚举 + 状态转换
    print("--- 订单状态状态机 ---")
    s1 = OrderStatus.PENDING
    s2 = OrderStatus.CONFIRMED
    print(f"{s1.value} → {s2.value}: {s1.can_transition_to(s2)}")
    print(f"{s1.value} → delivered: {s1.can_transition_to(OrderStatus.DELIVERED)}")

    # IntEnum
    print(f"\n--- IntEnum ---")
    print(f"Priority.HIGH = {Priority.HIGH} (int: {int(Priority.HIGH)})")
    print(f"Priority.HIGH == 3: {Priority.HIGH == 3}")
    print(f"中文: {Priority.CRITICAL.label_cn}")

    # StrEnum
    print(f"\n--- StrEnum ---")
    env = Environment.PROD
    print(f"环境: {env} (is_prod={env.is_prod})")
    print(f"URL 推断: {Environment.from_url('https://staging.example.com')}")
    # StrEnum 可直接用于字符串比较
    print(f"env == 'production': {env == 'production'}")

    # Flag 权限
    print("\n--- Flag 权限 ---")
    user_perm = Permission.READ | Permission.WRITE
    print(f"用户权限: {user_perm}")
    print(f"有 READ: {Permission.READ in user_perm}")
    print(f"有 DELETE: {Permission.DELETE in user_perm}")

    # 状态机实战
    print("\n--- 任务状态机 ---")
    task = Task("实现登录功能")
    task.transition_to(TaskState.IN_PROGRESS)
    task.transition_to(TaskState.REVIEW)
    task.transition_to(TaskState.TODO)     # 禁止
    task.transition_to(TaskState.DONE)

    # 枚举遍历
    print(f"\n所有状态: {[s.name for s in OrderStatus]}")
    print(f"状态 → 值映射: { {s.name: s.value for s in OrderStatus} }")


if __name__ == "__main__":
    demo()
