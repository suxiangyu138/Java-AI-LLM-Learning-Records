"""
单元 02：控制流

覆盖：if/elif/else、match-case（Python 3.10+）、while、for、break/continue、海象运算符。
"""

from __future__ import annotations


# ============================================================
# 1. 条件判断
# ============================================================

def classify_number(num: int) -> str:
    """根据数值返回分类."""
    if num > 0:
        return "正数"
    elif num == 0:
        return "零"
    else:
        return "负数"


# Python 3.10+ match-case（类似 switch）
def describe_http_status(code: int) -> str:
    """用 match-case 描述 HTTP 状态码."""
    match code:
        case 200:
            return "OK"
        case 201:
            return "Created"
        case 301 | 302:
            return "Redirect"
        case 400:
            return "Bad Request"
        case 404:
            return "Not Found"
        case 500:
            return "Internal Server Error"
        case _:
            return f"Unknown status: {code}"


# ============================================================
# 2. 循环
# ============================================================

def demo_loops() -> None:
    """演示 while / for / break / continue."""
    # while
    i: int = 0
    while i < 3:
        print(f"while loop i={i}")
        i += 1

    # for 遍历列表
    fruits: list[str] = ["apple", "banana", "cherry"]
    for fruit in fruits:
        print(f"  fruit: {fruit}")

    # for + enumerate（需要索引时用这个，不要自己维护计数器）
    for idx, fruit in enumerate(fruits, start=1):
        print(f"  {idx}: {fruit}")

    # for + range
    for n in range(3):  # 0, 1, 2
        print(f"  range: {n}")

    # break / continue
    for n in range(10):
        if n == 3:
            continue  # 跳过 3
        if n == 6:
            break     # 退出循环
        print(f"  n={n}")  # → 0, 1, 2, 4, 5


# ============================================================
# 3. 海象运算符 :=（Python 3.8+）
# ============================================================

def demo_walrus() -> None:
    """海象运算符：在表达式内部赋值."""
    # 传统写法 vs 海象
    data: list[str] = ["hello", "world", ""]

    # 海象：在 while 条件中同时赋值和判断
    while (line := data.pop() if data else ""):
        print(f"  walrus line: '{line}'")

    # 海象在列表推导中复用计算值
    prices: list[float] = [9.99, 19.99, 29.99]
    discounted: list[float] = [
        d for p in prices if (d := p * 0.8) > 10
    ]
    print(f"  discounted > 10: {discounted}")


# ============================================================
# 4. 三元表达式 & any / all
# ============================================================

def demo_shortcuts() -> None:
    """三元表达式和内置判断函数."""
    age: int = 20
    status: str = "成年" if age >= 18 else "未成年"
    print(f"  年龄 {age} → {status}")

    scores: list[int] = [80, 90, 70, 60]
    print(f"  全部及格? {all(s >= 60 for s in scores)}")   # True
    print(f"  有满分?   {any(s == 100 for s in scores)}")  # False


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 02：控制流")
    print("=" * 50)

    print(f"classify_number(10) = {classify_number(10)}")
    print(f"classify_number(-3) = {classify_number(-3)}")
    print(f"describe_http_status(404) = {describe_http_status(404)}")
    print(f"describe_http_status(301) = {describe_http_status(301)}")
    print()
    demo_loops()
    print()
    demo_walrus()
    print()
    demo_shortcuts()
