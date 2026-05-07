"""
模块 05：魔术方法（Dunder Methods）。

覆盖知识点：
    - __str__ / __repr__ : 字符串表示
    - __eq__ / __hash__ : 相等比较与哈希
    - __lt__ / __le__ 等：全排序
    - __len__ / __contains__ : 容器行为
    - __getitem__ / __setitem__ : 索引访问
    - __enter__ / __exit__ : 上下文管理器
    - __call__ : 可调用对象
"""

from __future__ import annotations

from functools import total_ordering


# ============================================================
# __str__ / __repr__
# ============================================================
class Point:
    """二维坐标点 —— 演示 str 与 repr 的区别。"""

    def __init__(self, x: float, y: float) -> None:
        self.x = x
        self.y = y

    def __repr__(self) -> str:
        """给开发者看的，eval(repr(obj)) 应能重建对象。"""
        return f"Point(x={self.x}, y={self.y})"

    def __str__(self) -> str:
        """给用户看的，更可读。"""
        return f"({self.x}, {self.y})"


# ============================================================
# __eq__ / __hash__ / 排序方法
# ============================================================
@total_ordering  # 只需定义 __eq__ 和 __lt__，其余自动生成
class Version:
    """语义化版本号 —— 演示相等、哈希、排序。"""

    def __init__(self, major: int, minor: int, patch: int) -> None:
        self.major = major
        self.minor = minor
        self.patch = patch

    def __repr__(self) -> str:
        return f"v{self.major}.{self.minor}.{self.patch}"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Version):
            return NotImplemented
        return (self.major, self.minor, self.patch) == (
            other.major,
            other.minor,
            other.patch,
        )

    def __lt__(self, other: Version) -> bool:
        return (self.major, self.minor, self.patch) < (
            other.major,
            other.minor,
            other.patch,
        )

    def __hash__(self) -> int:
        """自定义 __eq__ 后必须同时定义 __hash__，否则对象不可哈希。"""
        return hash((self.major, self.minor, self.patch))


# ============================================================
# 容器行为：__len__ / __contains__ / __getitem__
# ============================================================
class Playlist:
    """歌单 —— 让自定义类像 list 一样使用。"""

    def __init__(self, name: str, tracks: list[str] | None = None) -> None:
        self.name = name
        self._tracks: list[str] = tracks or []

    def __len__(self) -> int:
        return len(self._tracks)

    def __contains__(self, track: str) -> bool:
        return track in self._tracks

    def __getitem__(self, index: int) -> str:
        return self._tracks[index]

    def __setitem__(self, index: int, value: str) -> None:
        self._tracks[index] = value

    def __iter__(self):
        return iter(self._tracks)

    def __repr__(self) -> str:
        return f"Playlist({self.name!r}, {len(self)} tracks)"


# ============================================================
# 上下文管理器：__enter__ / __exit__
# ============================================================
class DatabaseConnection:
    """模拟数据库连接 —— 使用 with 语句自动管理资源。"""

    def __init__(self, dsn: str) -> None:
        self.dsn = dsn
        self._connected = False

    def __enter__(self) -> DatabaseConnection:
        """进入 with 块时调用，返回值绑定到 as 变量。"""
        print(f"[DB] 连接 {self.dsn} ...")
        self._connected = True
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> bool:
        """退出 with 块时调用（即使发生异常也会执行）。"""
        print(f"[DB] 断开 {self.dsn}")
        self._connected = False
        # 返回 False（默认）让异常继续传播；返回 True 则吞掉异常
        return False

    def query(self, sql: str) -> list[str]:
        if not self._connected:
            raise RuntimeError("数据库未连接")
        print(f"[DB] 执行: {sql}")
        return ["row1", "row2"]


# ============================================================
# __call__ : 可调用对象
# ============================================================
class ThresholdFilter:
    """阈值过滤器 —— 实例化后可像函数一样调用。"""

    def __init__(self, threshold: float) -> None:
        self.threshold = threshold

    def __call__(self, value: float) -> float:
        """调用实例时触发。"""
        return value if value >= self.threshold else 0.0


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 05：魔术方法")
    print("=" * 60)

    # __str__ vs __repr__
    p = Point(3.5, 2.1)
    print(f"str: {p}  |  repr: {p!r}")  # !r 强制使用 repr

    # __eq__ / __lt__ / __hash__
    versions = [Version(2, 1, 0), Version(1, 0, 0), Version(2, 0, 0)]
    print(f"\n排序前: {versions}")
    print(f"排序后: {sorted(versions)}")
    v1, v2 = Version(1, 0, 0), Version(1, 0, 0)
    print(f"v1 == v2: {v1 == v2}, hash一致: {hash(v1) == hash(v2)}")

    # 容器行为
    playlist = Playlist("Chill", ["Track A", "Track B", "Track C"])
    print(f"\n{playlist}, len={len(playlist)}")
    print(f"第0首: {playlist[0]}")
    print(f"包含 'Track B': {'Track B' in playlist}")
    print(f"遍历: {[t for t in playlist]}")

    # 上下文管理器
    print()
    with DatabaseConnection("postgresql://localhost/mydb") as db:
        db.query("SELECT * FROM users LIMIT 1")
    # with 块结束后自动断开连接

    # 可调用对象
    print()
    filter_10 = ThresholdFilter(10)
    print(f"filter(5)={filter_10(5)}, filter(15)={filter_10(15)}")


if __name__ == "__main__":
    demo()
