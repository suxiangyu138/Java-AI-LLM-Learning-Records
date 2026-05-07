"""
单元测试合集：覆盖 01-08 所有单元的核心函数.

运行方式：
    pytest python_basics/test_all.py -v
    python -m pytest python_basics/test_all.py -v
"""

from __future__ import annotations

import json
import math
from pathlib import Path

import pytest

# 将当前目录加入 sys.path，确保模块可导入
import sys
sys.path.insert(0, str(Path(__file__).parent))

from _test_targets import (
    # 01
    calculate_discount,
    # 02
    classify_number,
    describe_http_status,
    # 03
    greet,
    create_user,
    sum_all,
    build_headers,
    make_multiplier,
    # 04
    # (demo 函数无需单独测试，测推导式逻辑即可)
    # 05
    User,
    Dog,
    Cat,
    DateUtils,
    Product,
    Point,
    Order,
    # 06
    divide,
    AppError,
    ValidationError,
    # 07
    # (IO 函数大多是 demo，不单独测)
    # 08
    fibonacci,
    CountDown,
    chain_generators,
    retry,
    # 09
    Stack,
    first,
    format_user,
    request,
    get_value,
    make_it_fly,
    Bird,
    Airplane,
    Builder,
    # 10
    fetch_data,
    fetch_with_timeout,
    # 11
    calculate_tax,
    PaymentService,
    # 12
    AppConfig,
    load_dotenv_simple,
)


# ============================================================
# 单元 01 测试
# ============================================================

class TestUnit01:
    """变量、类型与字符串."""

    def test_calculate_discount_normal(self) -> None:
        assert calculate_discount(100, 20) == pytest.approx(80.0)

    def test_calculate_discount_default(self) -> None:
        assert calculate_discount(50) == pytest.approx(50.0)

    def test_calculate_discount_invalid(self) -> None:
        with pytest.raises(ValueError, match="0-100"):
            calculate_discount(100, -5)
        with pytest.raises(ValueError, match="0-100"):
            calculate_discount(100, 101)


# ============================================================
# 单元 02 测试
# ============================================================

class TestUnit02:
    """控制流."""

    def test_classify_number(self) -> None:
        assert classify_number(10) == "正数"
        assert classify_number(0) == "零"
        assert classify_number(-5) == "负数"

    def test_describe_http_status(self) -> None:
        assert describe_http_status(200) == "OK"
        assert describe_http_status(404) == "Not Found"
        assert describe_http_status(301) == "Redirect"
        assert describe_http_status(999) == "Unknown status: 999"


# ============================================================
# 单元 03 测试
# ============================================================

class TestUnit03:
    """函数."""

    def test_greet(self) -> None:
        assert greet("Alice") == "Hello, Alice!"
        assert greet("Bob", greeting="Hi") == "Hi, Bob!"

    def test_create_user(self) -> None:
        user = create_user("Alice", age=30, city="Beijing")
        assert user == {"name": "Alice", "age": 30, "city": "Beijing"}

    def test_sum_all(self) -> None:
        assert sum_all(1, 2, 3) == 6
        assert sum_all() == 0

    def test_build_headers(self) -> None:
        headers = build_headers(Authorization="Bearer x", Content_Type="json")
        assert headers == {"Authorization": "Bearer x", "Content_Type": "json"}

    def test_make_multiplier(self) -> None:
        double = make_multiplier(2)
        assert double(5) == 10
        assert double(0) == 0


# ============================================================
# 单元 04 测试
# ============================================================

class TestUnit04:
    """数据结构."""

    def test_dict_merge(self) -> None:
        a = {"x": 1, "y": 2}
        b = {"y": 3, "z": 4}
        assert a | b == {"x": 1, "y": 3, "z": 4}

    def test_set_operations(self) -> None:
        a = {1, 2, 3}
        b = {2, 3, 4}
        assert a | b == {1, 2, 3, 4}
        assert a & b == {2, 3}
        assert a - b == {1}

    def test_list_comprehension(self) -> None:
        squares = [x ** 2 for x in range(5)]
        assert squares == [0, 1, 4, 9, 16]

    def test_dict_fromkeys_dedup(self) -> None:
        items = [1, 2, 2, 3]
        assert list(dict.fromkeys(items)) == [1, 2, 3]


# ============================================================
# 单元 05 测试
# ============================================================

class TestUnit05:
    """面向对象."""

    def test_user_creation(self) -> None:
        u = User("Alice", 30, "a@b.com")
        assert u.name == "Alice"
        assert u.is_adult is True

    def test_user_not_adult(self) -> None:
        u = User("Bob", 16, "b@b.com")
        assert u.is_adult is False

    def test_user_equality(self) -> None:
        u1 = User("Alice", 30, "a@b.com")
        u2 = User("Alice", 30, "other@b.com")  # email 不参与 eq
        assert u1 == u2
        assert u1 != User("Bob", 30, "b@b.com")

    def test_user_email_readonly(self) -> None:
        u = User("Alice", 30, "a@b.com")
        with pytest.raises(AttributeError):
            u.email = "new@b.com"  # type: ignore[misc]

    def test_dog_cat_polymorphism(self) -> None:
        dog = Dog()
        cat = Cat()
        assert dog.speak() == "Woof!"
        assert cat.speak() == "Meow!"

    def test_date_utils(self) -> None:
        assert DateUtils.is_valid_date("2026-05-07") is True
        assert DateUtils.is_valid_date("bad") is False

    def test_product_dataclass(self) -> None:
        p = Product(name="Test", price=9.99, stock=5)
        assert p.is_available is True
        p2 = Product(name="Test", price=9.99, stock=0)
        assert p2.is_available is False

    def test_point_frozen(self) -> None:
        p = Point(3.0, 4.0)
        assert p.distance_from_origin() == 5.0
        with pytest.raises(Exception):
            p.x = 5.0  # type: ignore[misc]  # frozen=True

    def test_order_total(self) -> None:
        order = Order(order_id="O1", items=[
            Product(name="A", price=10.0),
            Product(name="B", price=20.0),
        ])
        assert order.total == 30.0


# ============================================================
# 单元 06 测试
# ============================================================

class TestUnit06:
    """异常处理."""

    def test_divide_success(self) -> None:
        assert divide(10, 2) == 5.0

    def test_divide_zero_raises_app_error(self) -> None:
        with pytest.raises(AppError, match="除数不能为零"):
            divide(10, 0)

    def test_validation_error_hierarchy(self) -> None:
        assert issubclass(ValidationError, AppError)
        assert issubclass(AppError, Exception)


# ============================================================
# 单元 07 测试
# ============================================================

class TestUnit07:
    """文件 IO 与模块."""

    def test_json_roundtrip(self, tmp_path: Path) -> None:
        data = {"name": "Alice", "age": 30}
        filepath = tmp_path / "test.json"
        filepath.write_text(json.dumps(data), encoding="utf-8")
        loaded = json.loads(filepath.read_text(encoding="utf-8"))
        assert loaded == data

    def test_pathlib_operations(self) -> None:
        p = Path("/a/b/c.txt")
        assert p.name == "c.txt"
        assert p.suffix == ".txt"
        assert p.stem == "c"


# ============================================================
# 单元 08 测试
# ============================================================

class TestUnit08:
    """装饰器与生成器."""

    def test_fibonacci(self) -> None:
        fib = list(fibonacci(5))
        assert fib == [0, 1, 1, 2, 3]

    def test_fibonacci_length(self) -> None:
        assert len(list(fibonacci(10))) == 10

    def test_countdown_iterator(self) -> None:
        assert list(CountDown(3)) == [3, 2, 1, 0]

    def test_countdown_empty(self) -> None:
        assert list(CountDown(-1)) == []

    def test_chain_generators(self) -> None:
        assert list(chain_generators()) == [0, 1, 2, 10, 20, 30]


# ============================================================
# 单元 09 测试
# ============================================================

class TestUnit09:
    """类型系统进阶."""

    def test_stack_generic_int(self) -> None:
        s: Stack[int] = Stack()
        s.push(1)
        s.push(2)
        assert len(s) == 2
        assert s.peek() == 2
        assert s.pop() == 2
        assert s.pop() == 1
        assert s.pop() is None

    def test_stack_generic_str(self) -> None:
        s: Stack[str] = Stack()
        s.push("hello")
        assert s.peek() == "hello"

    def test_first_sequence(self) -> None:
        assert first([10, 20]) == 10
        assert first([]) is None
        assert first(("a", "b")) == "a"

    def test_format_user_typeddict(self) -> None:
        user: UserRecord = {"id": "1", "name": "Alice", "email": "a@b.com", "age": 30}
        result = format_user(user)
        assert "Alice" in result
        assert "a@b.com" in result

    def test_request_literal(self) -> None:
        assert "GET" in request("/api", method="GET")
        assert "POST" in request("/submit", method="POST")

    def test_get_value_overload(self) -> None:
        assert get_value({"a": 1}, "a") == 1
        assert get_value({"a": "s"}, "a") == "s"

    def test_protocol_structural_typing(self) -> None:
        assert "Bird" in make_it_fly(Bird())
        assert "Airplane" in make_it_fly(Airplane())

    def test_builder_self_type(self) -> None:
        config = Builder().set_host("localhost").set_port(8080).build()
        assert config == {"host": "localhost", "port": 8080}


# ============================================================
# 单元 10 测试
# ============================================================

class TestUnit10:
    """异步编程."""

    @pytest.mark.anyio
    async def test_fetch_data(self) -> None:
        result = await fetch_data("test", delay=0.01)
        assert result == "Data from test"

    @pytest.mark.anyio
    async def test_fetch_with_timeout_success(self) -> None:
        result = await fetch_with_timeout(delay=0.01, timeout=1.0)
        assert "Data from" in result

    @pytest.mark.anyio
    async def test_fetch_with_timeout_exceeded(self) -> None:
        result = await fetch_with_timeout(delay=1.0, timeout=0.01)
        assert "超时" in result


# ============================================================
# 单元 11 测试
# ============================================================

class TestUnit11:
    """测试进阶（这里测被测试对象本身）."""

    def test_calculate_tax_parametrized(self) -> None:
        assert calculate_tax(100, 0.1) == 10.0
        assert calculate_tax(50, 0.2) == 10.0
        assert calculate_tax(0, 0.1) == 0.0

    def test_calculate_tax_raises(self) -> None:
        with pytest.raises(ValueError, match="金额不能为负"):
            calculate_tax(-100)

    def test_payment_service_charge(self) -> None:
        svc = PaymentService(api_key="test_key")
        result = svc.charge(100.0)
        assert result["status"] == "success"

    def test_payment_service_validate_card(self) -> None:
        svc = PaymentService(api_key="test_key")
        assert svc.validate_card("1234567890123456") is True
        assert svc.validate_card("1234") is False


# ============================================================
# 单元 12 测试
# ============================================================

class TestUnit12:
    """日志与配置."""

    def test_app_config_defaults(self) -> None:
        config = AppConfig()
        assert config.host == "0.0.0.0"
        assert config.port == 8000
        assert config.debug is False
        assert config.llm_model == "claude-sonnet-4-6"

    def test_app_config_to_dict(self) -> None:
        config = AppConfig()
        d = config.to_dict()
        assert d["host"] == "0.0.0.0"
        assert d["port"] == 8000
        assert "***" in d["db_url"] or "localhost" in d["db_url"]

    def test_load_dotenv_simple_missing_file(self, tmp_path: Path) -> None:
        """.env 不存在时不应报错."""
        nonexistent = tmp_path / ".env"
        load_dotenv_simple(nonexistent)  # should not raise

    def test_load_dotenv_simple_parses(self, tmp_path: Path) -> None:
        env_file = tmp_path / ".env"
        env_file.write_text('KEY1=value1\nKEY2 = "value2"\n')
        load_dotenv_simple(env_file)
        import os
        assert os.environ.get("KEY1") == "value1"
        assert os.environ.get("KEY2") == "value2"
