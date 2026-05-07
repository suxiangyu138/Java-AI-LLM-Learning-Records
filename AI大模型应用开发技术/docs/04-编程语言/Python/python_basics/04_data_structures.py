"""
单元 04：数据结构

覆盖：list / tuple / dict / set、推导式、切片、解包、collections 模块.
"""

from __future__ import annotations

from collections import Counter, defaultdict, deque


# ============================================================
# 1. List（列表）— 可变、有序
# ============================================================
def demo_list() -> None:
    """列表的增删改查、切片、推导式."""
    nums: list[int] = [1, 2, 3]

    # 增删
    nums.append(4)          # 尾部添加 → [1, 2, 3, 4]
    nums.insert(0, 0)       # 指定位置 → [0, 1, 2, 3, 4]
    nums.extend([5, 6])     # 合并    → [0, 1, 2, 3, 4, 5, 6]
    popped: int = nums.pop()  # 弹出尾部 → 6
    nums.remove(0)           # 按值删除第一个匹配
    print(f"  nums after mutations: {nums}")  # [1, 2, 3, 4, 5]

    # 切片 [start:stop:step]
    print(f"  nums[:3]   = {nums[:3]}")    # [1, 2, 3]
    print(f"  nums[::2]  = {nums[::2]}")   # [1, 3, 5]
    print(f"  nums[::-1] = {nums[::-1]}")  # 反转 [5, 4, 3, 2, 1]

    # 推导式
    squares: list[int] = [x ** 2 for x in range(5)]  # [0, 1, 4, 9, 16]
    evens: list[int] = [x for x in range(10) if x % 2 == 0]
    print(f"  squares={squares}, evens={evens}")


# ============================================================
# 2. Tuple（元组）— 不可变、可哈希、有序
# ============================================================
def demo_tuple() -> None:
    """元组：不可变序列，可作字典 key."""
    point: tuple[float, float, float] = (1.0, 2.0, 3.0)
    x, y, z = point  # 解包
    print(f"  x={x}, y={y}, z={z}")

    # 具名元组（替代轻量 data class）
    from collections import namedtuple
    User = namedtuple("User", ["name", "age"])
    alice = User(name="Alice", age=30)
    print(f"  {alice.name} is {alice.age}")
    # alice.age = 31  # 报错！namedtuple 不可变


# ============================================================
# 3. Dict（字典）— key-value 映射，Python 3.7+ 保序
# ============================================================
def demo_dict() -> None:
    """字典常用操作."""
    user: dict[str, object] = {
        "name": "Alice",
        "age": 30,
        "roles": ["admin", "editor"],
    }

    # 安全取值
    name: str = user.get("name", "unknown")  # 推荐
    phone: str = user.get("phone", "N/A")     # key 不存在返回默认值
    print(f"  name={name}, phone={phone}")

    # 遍历
    for key, value in user.items():
        print(f"    {key} = {value}")

    # 合并字典（Python 3.9+ 用 | 运算符）
    defaults: dict[str, str] = {"theme": "dark", "lang": "zh"}
    overrides: dict[str, str] = {"lang": "en"}
    merged: dict[str, str] = defaults | overrides  # {"theme": "dark", "lang": "en"}
    print(f"  merged: {merged}")

    # 推导式
    squared_map: dict[int, int] = {x: x ** 2 for x in range(5)}
    print(f"  squared_map: {squared_map}")


# ============================================================
# 4. Set（集合）— 无序、唯一、可哈希
# ============================================================
def demo_set() -> None:
    """集合并交差运算."""
    a: set[int] = {1, 2, 3, 4}
    b: set[int] = {3, 4, 5, 6}

    print(f"  并集: {a | b}")   # {1, 2, 3, 4, 5, 6}
    print(f"  交集: {a & b}")   # {3, 4}
    print(f"  差集: {a - b}")   # {1, 2}
    print(f"  对称差: {a ^ b}") # {1, 2, 5, 6}

    # 去重
    items: list[int] = [1, 2, 2, 3, 3, 3]
    unique: list[int] = list(set(items))  # 注意：顺序可能丢失
    # 保持顺序去重（Python 3.7+）
    ordered_unique: list[int] = list(dict.fromkeys(items))
    print(f"  unique={unique}, ordered_unique={ordered_unique}")


# ============================================================
# 5. collections 实用数据结构
# ============================================================
def demo_collections() -> None:
    """Counter / defaultdict / deque."""
    # Counter：计数器
    word_counts = Counter("abracadabra")
    print(f"  most common: {word_counts.most_common(3)}")  # [('a', 5), ('b', 2), ('r', 2)]

    # defaultdict：带默认值的字典
    groups: defaultdict[str, list[str]] = defaultdict(list)
    groups["admin"].append("Alice")
    print(f"  groups: {dict(groups)}")  # {'admin': ['Alice']}

    # deque：双端队列（O(1) 头尾操作，比 list 的 pop(0) O(n) 快）
    queue: deque[int] = deque([1, 2, 3])
    queue.appendleft(0)
    queue.append(4)
    print(f"  deque: {list(queue)}")  # [0, 1, 2, 3, 4]


# ============================================================
# 6. 解包进阶
# ============================================================
def demo_unpacking() -> None:
    """星号解包."""
    first, *middle, last = range(10)
    print(f"  first={first}, middle={middle}, last={last}")

    # 合并多个可迭代对象
    combined: list[int] = [*range(3), *range(5, 8)]
    print(f"  combined: {combined}")  # [0, 1, 2, 5, 6, 7]


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 04：数据结构")
    print("=" * 50)

    demo_list()
    print()
    demo_tuple()
    print()
    demo_dict()
    print()
    demo_set()
    print()
    demo_collections()
    print()
    demo_unpacking()
