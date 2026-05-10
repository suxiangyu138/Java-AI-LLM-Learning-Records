"""
模块 14：迭代器与可迭代对象。

覆盖知识点：
    - __iter__ / __next__ : 迭代器协议
    - 可迭代对象 vs 迭代器
    - 生成器函数（yield）
    - yield from 委托
    - 实战：分页器、数据流水线、懒加载集合
"""

from __future__ import annotations

from collections.abc import Iterator
from typing import TypeVar

T = TypeVar("T")


# ============================================================
# 实战一：自定义迭代器 —— 分页器
# ============================================================
class Paginator(Iterator[list[T]]):
    """
    分页迭代器 —— 将大数据集按固定大小分页遍历。

    Iterator[T] 只需实现 __next__。
    """

    def __init__(self, data: list[T], page_size: int = 10) -> None:
        if page_size <= 0:
            raise ValueError("page_size 必须大于 0")
        self._data = data
        self._page_size = page_size
        self._cursor = 0

    def __iter__(self) -> Paginator[T]:
        return self

    def __next__(self) -> list[T]:
        if self._cursor >= len(self._data):
            raise StopIteration
        start = self._cursor
        self._cursor += self._page_size
        return self._data[start : self._cursor]

    @property
    def total_pages(self) -> int:
        import math

        return math.ceil(len(self._data) / self._page_size)


# ============================================================
# 实战二：可迭代对象 —— 日期范围
# ============================================================
class DateRange:
    """
    日期范围 —— 可迭代对象（每次调用 iter 产生新的迭代器）。

    可迭代对象实现 __iter__；迭代器实现 __iter__ + __next__。
    """

    def __init__(self, start: str, end: str) -> None:
        from datetime import date, timedelta

        self._start = date.fromisoformat(start)
        self._end = date.fromisoformat(end)

    def __iter__(self) -> Iterator[str]:
        """每次调用返回一个新的生成器（迭代器）。"""
        current = self._start
        while current <= self._end:
            yield current.isoformat()
            current += __import__("datetime").timedelta(days=1)

    def __contains__(self, date_str: str) -> bool:
        """使得 '2025-06-15' in date_range 可用。"""
        from datetime import date

        d = date.fromisoformat(date_str)
        return self._start <= d <= self._end


# ============================================================
# 实战三：生成器流水线
# ============================================================
class DataPipeline:
    """
    数据处理流水线 —— 使用生成器链式处理大文件。

    生成器 = 惰性求值，内存中始终只有当前一条记录。
    """

    def __init__(self) -> None:
        self._steps: list = []

    def add_step(self, step):
        self._steps.append(step)
        return self

    def run(self, source):
        """按顺序应用所有处理步骤。"""
        data = source
        for step in self._steps:
            data = step(data)
        yield from data


def read_lines(path: str) -> Iterator[str]:
    """生成器：逐行读取文件（惰性）。"""
    with open(path, encoding="utf-8") as f:
        for line in f:
            yield line.strip()


def filter_non_empty(lines: Iterator[str]) -> Iterator[str]:
    """过滤空行。"""
    return (line for line in lines if line)


def parse_csv(lines: Iterator[str]) -> Iterator[dict[str, str]]:
    """解析 CSV 行。"""
    header = None
    for line in lines:
        fields = line.split(",")
        if header is None:
            header = fields
        else:
            yield dict(zip(header, fields))


# ============================================================
# 实战四：懒加载集合（LazyCollection）
# ============================================================
class LazyCollection:
    """
    懒加载集合 —— 只存数据源描述，访问时才计算。

    适合包装昂贵的数据库查询或 API 调用。
    """

    def __init__(self, fetcher):
        self._fetcher = fetcher
        self._cache: list | None = None

    def _load(self) -> list:
        if self._cache is None:
            print(f"  [懒加载] 首次访问，从数据源获取...")
            self._cache = list(self._fetcher())
        return self._cache

    def __iter__(self):
        yield from self._load()

    def __len__(self) -> int:
        return len(self._load())

    def __getitem__(self, index: int):
        return self._load()[index]

    def top(self, n: int):
        """取前 N 条（不会加载全部数据）。"""
        from itertools import islice

        return list(islice(self._fetcher(), n))


# ============================================================
# yield from 委托
# ============================================================
class TreeNode:
    """树节点 —— yield from 展平多层可迭代对象。"""

    def __init__(self, value: T, children: list[TreeNode[T]] | None = None) -> None:
        self.value = value
        self.children = children or []

    def traverse(self) -> Iterator[T]:
        """深度优先遍历。"""
        yield self.value
        for child in self.children:
            yield from child.traverse()

    def leaves(self) -> Iterator[T]:
        """只遍历叶子节点。"""
        if not self.children:
            yield self.value
        else:
            for child in self.children:
                yield from child.leaves()


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 14：迭代器与可迭代对象")
    print("=" * 60)

    # 分页器
    print("--- 分页器 ---")
    data = list(range(1, 26))  # 1..25
    for page_num, page in enumerate(Paginator(data, page_size=8), 1):
        print(f"  第 {page_num} 页: {page}")

    # 日期范围
    print("\n--- 日期范围 ---")
    week = DateRange("2025-06-09", "2025-06-15")
    print(f"包含 06-12: {'2025-06-12' in week}")
    print(f"遍历: {[d for d in week]}")

    # 生成器流水线（模拟数据流）
    print("\n--- 生成器流水线 ---")
    lines = ["name,age", "Alice,30", "Bob,25", "", "Carol,28"]
    pipeline = DataPipeline()
    pipeline.add_step(filter_non_empty)
    pipeline.add_step(parse_csv)
    for record in pipeline.run(iter(lines)):
        print(f"  {record}")

    # 懒加载集合
    print("\n--- 懒加载集合 ---")

    def expensive_query():
        return [f"row_{i}" for i in range(5)]

    lazy = LazyCollection(expensive_query)
    print(f"top(2): {lazy.top(2)}")  # 不会触发全量加载
    print(f"len: {len(lazy)}")        # 触发加载

    # yield from 树遍历
    print("\n--- 树遍历 ---")
    root = TreeNode("A", [
        TreeNode("B", [TreeNode("D"), TreeNode("E")]),
        TreeNode("C", [TreeNode("F")]),
    ])
    print(f"全部节点: {list(root.traverse())}")
    print(f"叶子节点: {list(root.leaves())}")


if __name__ == "__main__":
    demo()
