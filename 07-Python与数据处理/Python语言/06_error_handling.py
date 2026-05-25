"""
单元 06：异常处理与上下文管理器

覆盖：try/except/else/finally、自定义异常、raise from、contextlib、with.
"""

from __future__ import annotations

import logging
from contextlib import contextmanager
from pathlib import Path
from typing import Generator

logger = logging.getLogger(__name__)


# ============================================================
# 1. 自定义异常（企业规范）
# ============================================================

class AppError(Exception):
    """应用级异常基类."""


class ValidationError(AppError):
    """数据校验异常."""


class NotFoundError(AppError):
    """资源不存在."""


# ============================================================
# 2. try/except/else/finally
# ============================================================

def divide(a: float, b: float) -> float:
    """除法，演示完整的 try/except/else/finally 结构.

    - try: 可能出错的代码
    - except: 捕获特定异常
    - else: 无异常时执行（此时 b 有效）
    - finally: 无论是否异常都执行（清理资源）
    """
    try:
        result = a / b
    except ZeroDivisionError as exc:
        logger.warning("除零错误: a=%s, b=%s", a, b)
        raise AppError(f"除数不能为零: {exc}") from exc  # raise from 保留原因链
    except TypeError as exc:
        raise AppError(f"类型错误: {exc}") from exc
    else:
        print(f"  计算成功: {a} / {b} = {result}")  # 无异常才执行
        return result
    finally:
        print(f"  [finally] 清理: a={a}, b={b}")  # 无论是否异常


# ============================================================
# 3. 上下文管理器
# ============================================================

class FileWriter:
    """类实现上下文管理器：自动关闭文件."""

    def __init__(self, filepath: Path) -> None:
        self.filepath = filepath
        self._file = None

    def __enter__(self) -> "FileWriter":
        self._file = self.filepath.open("w", encoding="utf-8")
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> bool:
        if self._file:
            self._file.close()
        # return False → 不抑制异常；return True → 抑制
        return False

    def write_line(self, text: str) -> None:
        if self._file is None:
            raise RuntimeError("File not opened")
        self._file.write(text + "\n")


@contextmanager
def temp_file(filepath: Path) -> Generator[Path, None, None]:
    """基于生成器的上下文管理器（更简洁）."""
    try:
        filepath.touch()
        yield filepath
    finally:
        filepath.unlink(missing_ok=True)  # 清理


# ============================================================
# 4. ExceptionGroup（Python 3.11+）
# ============================================================

def validate_user(name: str, age: int) -> None:
    """演示同时抛出多个异常（Python 3.11+ ExceptionGroup）."""
    errors: list[Exception] = []
    if len(name) < 2:
        errors.append(ValidationError("名字至少2个字符"))
    if age < 0:
        errors.append(ValidationError("年龄不能为负"))
    if errors:
        raise ExceptionGroup("用户校验失败", errors)


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 06：异常处理与上下文管理器")
    print("=" * 50)

    # try/except/else/finally
    print("1. 正常除法:")
    try:
        divide(10, 2)
    except AppError as e:
        print(f"  捕获: {e}")

    print("\n2. 除零:")
    try:
        divide(10, 0)
    except AppError as e:
        print(f"  捕获: {e}")

    # 上下文管理器
    print("\n3. 文件写入（with 自动关闭）:")
    output_path = Path(__file__).parent / "_demo_output.txt"
    with FileWriter(output_path) as fw:
        fw.write_line("Hello from context manager!")
    print(f"  读取: {output_path.read_text().strip()}")
    output_path.unlink()  # 清理

    # 基于生成器的上下文管理器
    print("\n4. 临时文件:")
    with temp_file(Path(__file__).parent / "_temp.txt") as tf:
        print(f"  临时文件存在: {tf.exists()}")
