"""
模块 07：组合优于继承。

覆盖知识点：
    - 组合（Composition）vs 继承
    - 依赖注入（Dependency Injection）
    - 策略模式思想
    - 何时用继承、何时用组合
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Protocol


# ============================================================
# 继承方案的问题
# ============================================================
class Bird:
    def fly(self) -> str:
        return "Flying"


class Penguin(Bird):
    """企鹅不会飞，但被迫继承了 fly() —— 典型的继承滥用。"""

    def fly(self) -> str:
        raise NotImplementedError("Penguins can't fly!")


# ============================================================
# 组合方案：将行为抽象为独立组件
# ============================================================
class FlyBehavior(Protocol):
    """飞行行为协议 —— 任何可飞行的对象。"""

    def fly(self) -> str:
        ...


class WingFly:
    """翅膀飞行。"""

    def fly(self) -> str:
        return "Flapping wings → flying"


class JetFly:
    """喷气飞行。"""

    def fly(self) -> str:
        return "Jet engine → high speed flying"


class NoFly:
    """不会飞。"""

    def fly(self) -> str:
        return "Cannot fly"


# ============================================================
# 组合后的 Animal 体系
# ============================================================
class Animal:
    """动物基类 —— 飞行能力通过组合注入，而非继承。"""

    def __init__(self, name: str, fly_behavior: FlyBehavior | None = None) -> None:
        self.name = name
        self._fly_behavior = fly_behavior

    def perform_fly(self) -> str:
        """委托给注入的飞行行为。"""
        if self._fly_behavior is None:
            return f"{self.name}: no fly behavior configured"
        return f"{self.name}: {self._fly_behavior.fly()}"

    def set_fly_behavior(self, behavior: FlyBehavior) -> None:
        """运行时动态更换飞行行为。"""
        self._fly_behavior = behavior


# ============================================================
# 实战案例：通知服务
# ============================================================
class Notifier(ABC):
    """通知方式抽象接口（策略）。"""

    @abstractmethod
    def send(self, message: str, recipient: str) -> bool:
        ...


class EmailNotifier(Notifier):
    def send(self, message: str, recipient: str) -> bool:
        print(f"[Email] 发送给 {recipient}: {message}")
        return True


class SMSNotifier(Notifier):
    def send(self, message: str, recipient: str) -> bool:
        print(f"[SMS] 发送给 {recipient}: {message}")
        return True


class WechatNotifier(Notifier):
    def send(self, message: str, recipient: str) -> bool:
        print(f"[Wechat] 发送给 {recipient}: {message}")
        return True


class AlertService:
    """
    告警服务 —— 通过组合注入通知策略。

    可以通过构造函数注入（构造时确定）或 setter 注入（运行时切换）。
    """

    def __init__(self, notifiers: list[Notifier] | None = None) -> None:
        self._notifiers: list[Notifier] = notifiers or []

    def add_notifier(self, notifier: Notifier) -> None:
        self._notifiers.append(notifier)

    def alert(self, message: str, recipients: list[str]) -> None:
        """向所有收件人通过所有注册的通知渠道发送告警。"""
        for notifier in self._notifiers:
            for recipient in recipients:
                success = notifier.send(message, recipient)
                if not success:
                    print(f"  发送失败: {notifier.__class__.__name__} → {recipient}")


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 07：组合优于继承")
    print("=" * 60)

    # 继承的坏处
    try:
        Penguin().fly()
    except NotImplementedError as e:
        print(f"继承反例: {e}")

    # 组合的好处
    eagle = Animal("Eagle", WingFly())
    fighter = Animal("FighterJet", JetFly())
    penguin = Animal("Penguin", NoFly())
    print(eagle.perform_fly())
    print(fighter.perform_fly())
    print(penguin.perform_fly())

    # 运行时动态切换行为
    eagle.set_fly_behavior(JetFly())
    print(f"Eagle 更换引擎后: {eagle.perform_fly()}")

    # 实战：告警服务
    print("\n--- 告警服务 ---")
    alert_service = AlertService()
    alert_service.add_notifier(EmailNotifier())
    alert_service.add_notifier(WechatNotifier())
    alert_service.alert("CPU 使用率 > 90%", ["admin@corp.com", "18600001111"])


if __name__ == "__main__":
    demo()
