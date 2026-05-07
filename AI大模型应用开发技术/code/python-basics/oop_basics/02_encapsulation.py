"""
模块 02：封装与属性。

覆盖知识点：
    - 命名约定：_protected / __private（名称改写）
    - @property 装饰器：getter / setter / deleter
    - 只读属性
    - 数据校验在 setter 中的应用
"""

from __future__ import annotations

import re
from typing import Any


class BankAccount:
    """
    银行账户 —— 演示封装、属性装饰器与数据校验。

    企业实践中，所有外部可写的属性都应通过 property 做校验，
    避免无效数据进入系统。
    """

    def __init__(self, account_holder: str, initial_balance: float = 0.0) -> None:
        self._account_holder = account_holder
        self._balance = 0.0
        self._transactions: list[dict[str, Any]] = []

        # 通过 property setter 写入，走校验逻辑
        self.balance = initial_balance

    # ---------- balance: 带校验的读写属性 ----------
    @property
    def balance(self) -> float:
        """账户余额（只允许非负值）。"""
        return self._balance

    @balance.setter
    def balance(self, amount: float) -> None:
        if amount < 0:
            raise ValueError(f"余额不能为负数，收到: {amount}")
        self._balance = amount

    # ---------- account_holder: 只读属性 ----------
    @property
    def account_holder(self) -> str:
        """账户持有人姓名（只读，创建后不可修改）。"""
        return self._account_holder

    # ---------- 普通方法 ----------
    def deposit(self, amount: float) -> None:
        """存款。"""
        if amount <= 0:
            raise ValueError(f"存款金额必须大于零，收到: {amount}")
        self.balance += amount
        self._transactions.append({"type": "deposit", "amount": amount})

    def withdraw(self, amount: float) -> None:
        """取款。"""
        if amount <= 0:
            raise ValueError(f"取款金额必须大于零，收到: {amount}")
        if amount > self.balance:
            raise ValueError(f"余额不足: {self.balance:.2f} < {amount:.2f}")
        self.balance -= amount
        self._transactions.append({"type": "withdraw", "amount": amount})

    @property
    def last_transaction(self) -> dict[str, Any] | None:
        """最近一笔交易（只读派生属性）。"""
        return self._transactions[-1] if self._transactions else None


class User:
    """
    用户类 —— 演示 __ 名称改写的私有属性。

    双下划线前缀触发 Python 的名称改写（name mangling），
    外部无法通过原名访问，用于防止子类意外覆盖。
    """

    def __init__(self, username: str, email: str) -> None:
        self.username = username

        # __ 前缀 → Python 自动改写为 _User__email
        self.__email = ""
        self.email = email

        # __ 前缀 → _User__password_hash
        self.__password_hash: str = ""

    # ---------- email: 读写 + 格式校验 ----------
    @property
    def email(self) -> str:
        return self.__email

    @email.setter
    def email(self, value: str) -> None:
        if not re.match(r"^[\w\.-]+@[\w\.-]+\.\w+$", value):
            raise ValueError(f"无效的邮箱格式: {value}")
        self.__email = value

    # ---------- password: 只写属性 ----------
    def set_password(self, raw_password: str) -> None:
        """设置密码（真实场景应使用 bcrypt / passlib）。"""
        if len(raw_password) < 6:
            raise ValueError("密码长度不能少于 6 位")
        self.__password_hash = f"hashed({raw_password})"

    def verify_password(self, raw_password: str) -> bool:
        """验证密码。"""
        return self.__password_hash == f"hashed({raw_password})"


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 02：封装与属性")
    print("=" * 60)

    # BankAccount 演示
    account = BankAccount("Alice", 1000)
    print(f"持有人: {account.account_holder}  |  余额: ¥{account.balance:.2f}")

    account.deposit(500)
    account.withdraw(200)
    print(f"交易后余额: ¥{account.balance:.2f}")
    print(f"最近交易: {account.last_transaction}")

    try:
        account.balance = -100
    except ValueError as e:
        print(f"校验拦截: {e}")

    # User 演示
    user = User("bob_dev", "bob@example.com")
    print(f"\n用户: {user.username}  |  邮箱: {user.email}")

    user.set_password("s3cret!")
    print(f"密码验证正确: {user.verify_password('s3cret!')}")
    print(f"密码验证错误: {user.verify_password('wrong')}")

    # 名称改写演示
    # print(user.__email)          # AttributeError
    # print(user._User__email)     # 可以访问，但不应这样做

    try:
        user.email = "invalid"
    except ValueError as e:
        print(f"邮箱校验拦截: {e}")


if __name__ == "__main__":
    demo()
