"""
单元 11：测试进阶（pytest）

覆盖：fixture（scope/autouse）、parametrize、conftest.py 模式、
mock/patch、异常测试、临时目录.
"""

from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest


# ============================================================
# 被测试代码（通常在其他模块，此处集中演示）
# ============================================================

class PaymentService:
    """支付服务（依赖外部 API）."""

    def __init__(self, api_key: str) -> None:
        self.api_key = api_key

    def charge(self, amount: float) -> dict[str, object]:
        """调用外部支付网关."""
        # 实际会调第三方 API，这里模拟返回
        return {"status": "success", "amount": amount, "txn_id": "txn_123"}

    def validate_card(self, card_number: str) -> bool:
        """校验卡号."""
        return len(card_number) == 16 and card_number.isdigit()


def calculate_tax(amount: float, rate: float = 0.1) -> float:
    """计算税额."""
    if amount < 0:
        raise ValueError("金额不能为负")
    return round(amount * rate, 2)


# ============================================================
# 测试代码
# ============================================================

# ---- fixture 基础 ----

@pytest.fixture
def payment_service() -> PaymentService:
    """fixture：创建 PaymentService 实例（默认 scope='function'）."""
    return PaymentService(api_key="test_key_123")


# ---- fixture scope ----

@pytest.fixture(scope="module")
def shared_resource() -> list[int]:
    """module 级别 fixture：整个模块共享，只创建一次."""
    print("\n    [setup] 创建共享资源")
    return [0]


# ---- autouse fixture ----

@pytest.fixture(autouse=True)
def _reset_counter() -> None:
    """每个测试自动执行的 fixture（清理副作用）."""
    # 可用于重置全局状态、清理缓存等
    pass


# ============================================================
# 测试类
# ============================================================

class TestPaymentService:
    """PaymentService 的单元测试."""

    def test_charge_success(self, payment_service: PaymentService) -> None:
        """fixture 注入实例."""
        result = payment_service.charge(100.0)
        assert result["status"] == "success"
        assert result["amount"] == 100.0

    def test_validate_card_valid(self, payment_service: PaymentService) -> None:
        assert payment_service.validate_card("1234567890123456") is True

    def test_validate_card_invalid(self, payment_service: PaymentService) -> None:
        assert payment_service.validate_card("1234") is False
        assert payment_service.validate_card("abcdefghijklmnop") is False


class TestCalculateTax:
    """参数化测试."""

    @pytest.mark.parametrize(
        "amount, rate, expected",
        [
            (100, 0.1, 10.0),
            (50, 0.2, 10.0),
            (0, 0.1, 0.0),
            (99.99, 0.1, 10.0),  # round 处理
        ],
        ids=["100@10%", "50@20%", "zero", "decimal"],
    )
    def test_calculate_tax(
        self, amount: float, rate: float, expected: float
    ) -> None:
        assert calculate_tax(amount, rate) == expected

    def test_negative_amount_raises(self) -> None:
        with pytest.raises(ValueError, match="金额不能为负"):
            calculate_tax(-100)

    def test_negative_amount_exact(self) -> None:
        """捕获异常后进一步断言."""
        with pytest.raises(ValueError) as exc_info:
            calculate_tax(-50)
        assert "金额不能为负" in str(exc_info.value)


# ============================================================
# Mock 测试
# ============================================================

class TestWithMock:
    """mock / patch 演示."""

    def test_mock_charge(self, payment_service: PaymentService) -> None:
        """用 Mock 替换 charge 方法."""
        mock_response = {"status": "mock_success", "amount": 999, "txn_id": "mock_001"}
        payment_service.charge = MagicMock(return_value=mock_response)  # type: ignore[method-assign]

        result = payment_service.charge(50.0)
        assert result["status"] == "mock_success"
        payment_service.charge.assert_called_once_with(50.0)  # type: ignore[union-attr]

    @patch("__main__.PaymentService.charge")
    def test_patch_decorator(self, mock_charge: MagicMock) -> None:
        """@patch 装饰器自动注入 mock."""
        mock_charge.return_value = {"status": "patched"}

        svc = PaymentService("key")
        result = svc.charge(10.0)
        assert result["status"] == "patched"
        mock_charge.assert_called_once()

    def test_mock_side_effect(self, payment_service: PaymentService) -> None:
        """side_effect：多次调用返回不同值 / 抛异常."""
        payment_service.charge = MagicMock(  # type: ignore[method-assign]
            side_effect=[
                {"status": "ok", "amount": 10},
                {"status": "ok", "amount": 20},
                Exception("Network error"),
            ]
        )
        assert payment_service.charge(10)["amount"] == 10  # type: ignore[union-attr]
        assert payment_service.charge(20)["amount"] == 20  # type: ignore[union-attr]
        with pytest.raises(Exception, match="Network error"):
            payment_service.charge(30)  # type: ignore[union-attr]


# ============================================================
# tmp_path fixture（内置）
# ============================================================

def test_write_and_read(tmp_path):
    """tmp_path：pytest 内置 fixture，提供临时目录."""
    file = tmp_path / "test.txt"
    file.write_text("Hello pytest!", encoding="utf-8")
    assert file.read_text(encoding="utf-8") == "Hello pytest!"
