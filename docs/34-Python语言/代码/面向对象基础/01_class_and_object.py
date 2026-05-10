"""
模块 01：类与对象基础。

覆盖知识点：
    - 类的定义与实例化
    - __init__ 构造方法
    - 实例方法、类方法、静态方法
    - 类变量与实例变量
    - 类型注解（Type Hints）
"""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import ClassVar


class Employee:
    """
    员工类 —— 演示类与对象的核心概念。

    Attributes:
        name: 员工姓名。
        position: 职位。
        salary: 薪资。
        employee_id: 自动生成的唯一 ID。
    """

    # 类变量：所有实例共享，逻辑上属于类而非某个对象
    company_name: ClassVar[str] = "Acme Corp"
    _total_count: ClassVar[int] = 0

    def __init__(self, name: str, position: str, salary: float) -> None:
        """初始化员工实例，每创建一个实例 _total_count 自增。"""
        self.name = name
        self.position = position
        self.salary = salary
        self.employee_id = str(uuid.uuid4())[:8]
        self.created_at = datetime.now()
        Employee._total_count += 1

    # ---------- 实例方法 ----------
    def give_raise(self, percent: float) -> None:
        """按百分比加薪（实例方法，第一个参数是 self）。"""
        self.salary *= 1 + percent / 100

    def summary(self) -> str:
        """返回员工信息摘要。"""
        return (
            f"[{self.employee_id}] {self.name} | {self.position} "
            f"| ¥{self.salary:,.0f}"
        )

    # ---------- 类方法 ----------
    @classmethod
    def from_dict(cls, data: dict[str, str | float]) -> Employee:
        """工厂方法：从字典创建 Employee 实例（类方法，第一个参数是 cls）。"""
        return cls(
            name=str(data["name"]),
            position=str(data.get("position", "Intern")),
            salary=float(data.get("salary", 0)),
        )

    @classmethod
    def get_total_count(cls) -> int:
        """获取已创建的员工总数。"""
        return cls._total_count

    # ---------- 静态方法 ----------
    @staticmethod
    def validate_salary(salary: float) -> bool:
        """校验薪资是否合法（静态方法：无需访问 cls 或 self）。"""
        return salary > 0


class Department:
    """部门类 —— 演示组合关系（一个部门包含多个员工）。"""

    def __init__(self, name: str) -> None:
        self.name = name
        self._members: list[Employee] = []

    def add(self, employee: Employee) -> None:
        """添加员工到部门。"""
        self._members.append(employee)

    def list_members(self) -> list[str]:
        """列出部门所有员工信息。"""
        return [e.summary() for e in self._members]


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 01：类与对象基础")
    print("=" * 60)

    # 1. 实例化
    alice = Employee("Alice Wang", "Engineer", 150_000)
    bob = Employee("Bob Li", "Manager", 200_000)
    print(alice.summary())
    print(bob.summary())

    # 2. 实例方法
    alice.give_raise(10)
    print(f"加薪后: {alice.summary()}")

    # 3. 类方法 — 工厂方法
    carol = Employee.from_dict({"name": "Carol Zhao", "salary": 120_000})
    print(carol.summary())

    # 4. 类变量 & 静态方法
    print(f"公司: {Employee.company_name}  |  员工总数: {Employee.get_total_count()}")
    print(f"薪资校验 -1: {Employee.validate_salary(-1)}, 50000: {Employee.validate_salary(50000)}")

    # 5. 组合关系
    dept = Department("Engineering")
    dept.add(alice)
    dept.add(bob)
    print(f"\n{dept.name} 部门成员:")
    for m in dept.list_members():
        print(f"  {m}")


if __name__ == "__main__":
    demo()
