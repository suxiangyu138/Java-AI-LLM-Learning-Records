"""
模块 16：面向对象的单元测试。

覆盖知识点：
    - unittest.TestCase 基础
    - setUp / tearDown 夹具管理
    - Mock / patch 隔离外部依赖
    - 测试抽象基类及其子类
    - 参数化测试
    - AAA 模式（Arrange-Act-Assert）
"""

from __future__ import annotations

import unittest
from abc import ABC, abstractmethod
from unittest.mock import MagicMock


# ============================================================
# 被测代码
# ============================================================
class InsufficientFundsError(Exception):
    """余额不足异常。"""
    pass


class Account:
    """银行账户 —— 我们测试的目标类。"""

    def __init__(self, owner: str, balance: float = 0.0) -> None:
        self.owner = owner
        self.balance = balance

    def deposit(self, amount: float) -> float:
        """存款，返回新余额。"""
        if amount <= 0:
            raise ValueError(f"存款金额必须 > 0，收到 {amount}")
        self.balance += amount
        return self.balance

    def withdraw(self, amount: float) -> float:
        """取款，余额不足时抛出 InsufficientFundsError。"""
        if amount <= 0:
            raise ValueError(f"取款金额必须 > 0，收到 {amount}")
        if amount > self.balance:
            raise InsufficientFundsError(
                f"余额 {self.balance:.2f} 不足以取出 {amount:.2f}"
            )
        self.balance -= amount
        return self.balance


class TransferService:
    """转账服务 —— 依赖外部通知服务。"""

    def __init__(self, notifier: Notifier) -> None:
        self._notifier = notifier

    def transfer(self, from_acc: Account, to_acc: Account, amount: float) -> bool:
        """转账：从 from_acc 转账到 to_acc。"""
        try:
            from_acc.withdraw(amount)
            to_acc.deposit(amount)
            self._notifier.send(from_acc.owner, f"已向 {to_acc.owner} 转账 ¥{amount:.2f}")
            return True
        except (ValueError, InsufficientFundsError) as e:
            self._notifier.send(from_acc.owner, f"转账失败: {e}")
            return False


class Notifier(ABC):
    """通知接口（方便 mock）。"""

    @abstractmethod
    def send(self, recipient: str, message: str) -> None:
        ...


# ============================================================
# 测试代码
# ============================================================
class AccountTest(unittest.TestCase):
    """Account 类的单元测试 —— AAA 模式（Arrange → Act → Assert）。"""

    def setUp(self) -> None:
        """每个测试方法执行前调用：准备干净的初始状态。"""
        self.account = Account("Alice", 1000.0)

    def tearDown(self) -> None:
        """每个测试方法执行后调用：清理资源。"""
        self.account = None  # 实际项目中可关闭 DB 连接等

    # ---- 存款测试 ----
    def test_deposit_positive_amount_success(self) -> None:
        """存款正数金额 → 成功。"""
        new_balance = self.account.deposit(500)
        self.assertEqual(new_balance, 1500)
        self.assertEqual(self.account.balance, 1500)

    def test_deposit_zero_raises_value_error(self) -> None:
        """存款 0 → 抛 ValueError。"""
        with self.assertRaises(ValueError):
            self.account.deposit(0)

    def test_deposit_negative_raises_value_error(self) -> None:
        """存款负数 → 抛 ValueError。"""
        with self.assertRaises(ValueError):
            self.account.deposit(-100)

    # ---- 取款测试 ----
    def test_withdraw_valid_amount_success(self) -> None:
        """取款正常金额 → 成功。"""
        new_balance = self.account.withdraw(300)
        self.assertEqual(new_balance, 700)

    def test_withdraw_exceeds_balance_raises(self) -> None:
        """取款超额 → 抛 InsufficientFundsError。"""
        with self.assertRaises(InsufficientFundsError):
            self.account.withdraw(2000)

    def test_withdraw_all_balance(self) -> None:
        """取出全部余额 → 余额为 0。"""
        self.account.withdraw(1000)
        self.assertEqual(self.account.balance, 0)

    # ---- 边界条件 ----
    def test_multiple_operations(self) -> None:
        """多次操作后余额正确。"""
        self.account.deposit(200)
        self.account.withdraw(150)
        self.account.deposit(50)
        self.assertEqual(self.account.balance, 1100)


class TransferServiceTest(unittest.TestCase):
    """
    TransferService 测试 —— 使用 Mock 隔离外部 Notifier 依赖。

    企业级测试的关键：不真正发邮件/短信，只验证调用行为。
    """

    def setUp(self) -> None:
        self.mock_notifier = MagicMock(spec=Notifier)
        self.service = TransferService(self.mock_notifier)
        self.alice = Account("Alice", 5000)
        self.bob = Account("Bob", 1000)

    def test_transfer_success(self) -> None:
        """转账成功：余额变化 + 通知发送。"""
        result = self.service.transfer(self.alice, self.bob, 2000)

        self.assertTrue(result)
        self.assertEqual(self.alice.balance, 3000)
        self.assertEqual(self.bob.balance, 3000)
        # 验证 mock 的 send 被调用，且参数正确
        self.mock_notifier.send.assert_called_once()
        call_args = self.mock_notifier.send.call_args[0]
        self.assertIn("Alice", call_args[0])  # recipient
        self.assertIn("2000", call_args[1])   # message

    def test_transfer_insufficient_funds(self) -> None:
        """余额不足：转账失败，余额不变，通知发送失败消息。"""
        result = self.service.transfer(self.alice, self.bob, 10000)

        self.assertFalse(result)
        self.assertEqual(self.alice.balance, 5000)  # 不变
        self.assertEqual(self.bob.balance, 1000)    # 不变
        # 即使转账失败也发送了通知
        self.mock_notifier.send.assert_called_once()
        self.assertIn("转账失败", self.mock_notifier.send.call_args[0][1])

    def test_transfer_negative_amount(self) -> None:
        """转账负数金额 → 失败。"""
        result = self.service.transfer(self.alice, self.bob, -100)
        self.assertFalse(result)

    def test_transfer_with_manual_mock(self) -> None:
        """手动创建 Mock 并注入 —— 不依赖 @patch 装饰器，更灵活。"""
        sender = MagicMock()
        sender.send.return_value = True
        service = TransferService(sender)
        result = service.transfer(self.alice, self.bob, 500)
        self.assertTrue(result)
        sender.send.assert_called_once()


# ============================================================
# 参数化测试（subTest）
# ============================================================
class ParameterizedTests(unittest.TestCase):
    """使用 subTest 实现参数化测试。"""

    def test_deposit_boundary_values(self) -> None:
        """测试存款的各边界值。"""
        test_cases = [
            # (初始余额, 充值金额, 期望余额, 是否抛异常)
            (1000, 500, 1500, None),
            (1000, 0, 1000, ValueError),
            (1000, -1, 1000, ValueError),
            (0, 1, 1, None),
            (9999.99, 0.01, 10000, None),
        ]
        for initial, amount, expected, exc in test_cases:
            with self.subTest(initial=initial, amount=amount):
                acc = Account("Test", initial)
                if exc:
                    with self.assertRaises(exc):
                        acc.deposit(amount)
                else:
                    acc.deposit(amount)
                    self.assertEqual(acc.balance, expected)


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 16：面向对象的单元测试")
    print("=" * 60)

    print("\n运行 Account 单元测试...\n")
    # 创建测试套件并运行
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    suite.addTests(loader.loadTestsFromTestCase(AccountTest))
    suite.addTests(loader.loadTestsFromTestCase(TransferServiceTest))
    suite.addTests(loader.loadTestsFromTestCase(ParameterizedTests))

    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    print("\n" + "─" * 60)
    print(f"测试结果: {result.testsRun} 个测试运行")
    print(f"  成功: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"  失败: {len(result.failures)}")
    print(f"  错误: {len(result.errors)}")
    print(f"  全部通过: {result.wasSuccessful()}")
    print("─" * 60)

    if not result.wasSuccessful():
        print("\n失败详情:")
        for test, traceback in result.failures + result.errors:
            print(f"\n  [{test}]")
            print(f"  {traceback[:300]}...")


if __name__ == "__main__":
    demo()
