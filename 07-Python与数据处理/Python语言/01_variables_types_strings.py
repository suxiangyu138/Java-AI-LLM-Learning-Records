"""
单元 01：变量、基本类型与字符串

覆盖：变量声明、int/float/bool/None、字符串操作、f-string、类型注解。
企业规范：所有 public 函数带类型注解与 docstring。
"""

from __future__ import annotations

import math
from typing import Any


# ============================================================
# 1. 变量与基本类型
# ============================================================

def demo_variables_and_types() -> None:
    """演示变量赋值与基本数值类型."""
    # Python 是动态类型，但强烈建议写类型注解（mypy / pyright 检查）
    count: int = 42
    price: float = 19.99
    is_active: bool = True
    nothing: None = None  # None 是 Python 的 null

    # 类型转换
    count_as_float: float = float(count)
    price_as_int: int = int(price)  # 向下取整，不是四舍五入

    # 数值运算
    quotient: float = count / 5       # 真除法 → 8.4
    floor_div: int = count // 5       # 整除 → 8
    remainder: int = count % 5        # 取模 → 2
    power: int = 2 ** 10              # 幂 → 1024

    # math 模块
    rounded: float = round(price, 1)  # 内置 round → 20.0
    sqrt_val: float = math.sqrt(25)   # math.sqrt → 5.0

    print(f"count={count}, price={price}, is_active={is_active}, nothing={nothing}")
    print(f"quotient={quotient}, floor_div={floor_div}, remainder={remainder}, power={power}")
    print(f"rounded={rounded}, sqrt_val={sqrt_val}")


# ============================================================
# 2. 字符串
# ============================================================

def demo_strings() -> None:
    """演示字符串常用操作."""
    name: str = "Alice"
    # f-string（Python 3.6+，企业首选）
    greeting: str = f"Hello, {name}! You have {3 + 2} messages."
    print(greeting)  # Hello, Alice! You have 5 messages.

    # 多行字符串
    sql: str = """
        SELECT id, name
        FROM users
        WHERE active = 1
    """
    print(sql.strip())

    # 常用方法
    raw: str = "  Hello World  "
    print(f"strip: '{raw.strip()}'")          # 'Hello World'
    print(f"lower: '{raw.lower()}'")          # '  hello world  '
    print(f"upper: '{raw.upper()}'")          # '  HELLO WORLD  '
    print(f"replace: '{raw.replace('World', 'Python')}'")
    print(f"split: {raw.split()}")            # ['Hello', 'World']
    print(f"'World' in raw: {'World' in raw}")  # True


# ============================================================
# 3. 类型注解（企业级必备）
# ============================================================

def calculate_discount(
    price: float,
    discount_percent: float = 0.0,
) -> float:
    """计算折扣后价格.

    Args:
        price: 原价.
        discount_percent: 折扣百分比，范围 0-100.

    Returns:
        折扣后价格.
    """
    if not (0 <= discount_percent <= 100):
        raise ValueError(f"折扣百分比必须在 0-100 之间，实际: {discount_percent}")
    return price * (1 - discount_percent / 100)


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 01：变量、类型与字符串")
    print("=" * 50)

    demo_variables_and_types()
    print()
    demo_strings()
    print()
    print(f"calculate_discount(100, 20) = {calculate_discount(100, 20)}")
    print(f"calculate_discount(50) = {calculate_discount(50)}")  # 使用默认值
