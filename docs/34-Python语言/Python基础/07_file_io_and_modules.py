"""
单元 07：文件 IO 与模块系统

覆盖：open/read/write、pathlib、csv/json、import 规则、__init__.py、包结构.
"""

from __future__ import annotations

import csv
import json
import sys
from pathlib import Path


# ============================================================
# 1. pathlib（现代文件路径操作，替代 os.path）
# ============================================================

def demo_pathlib() -> None:
    """pathlib 核心操作."""
    # 路径对象
    current: Path = Path(__file__)  # 当前文件路径
    parent: Path = current.parent    # 所在目录

    print(f"  当前文件: {current.name}")
    print(f"  所在目录: {parent}")
    print(f"  后缀: {current.suffix}")
    print(f"  去掉后缀: {current.stem}")

    # 拼接路径（/ 运算符，跨平台）
    data_dir: Path = parent / "data"
    config_file: Path = data_dir / "config.json"
    print(f"  config 路径: {config_file}")

    # 创建目录
    data_dir.mkdir(exist_ok=True)
    print(f"  data/ 存在: {data_dir.exists()}")
    data_dir.rmdir()  # 清理


# ============================================================
# 2. 文件读写
# ============================================================

def demo_file_io() -> None:
    """文本文件读写."""
    filepath: Path = Path(__file__).parent / "_io_demo.txt"

    # 写入（with 自动关闭）
    lines: list[str] = ["第一行", "第二行", "第三行"]
    filepath.write_text("\n".join(lines), encoding="utf-8")

    # 读取全部
    content: str = filepath.read_text(encoding="utf-8")
    print(f"  全部内容:\n{content}")

    # 逐行读取
    print("  逐行:")
    with filepath.open("r", encoding="utf-8") as f:
        for idx, line in enumerate(f, 1):
            print(f"    L{idx}: {line.strip()}")

    filepath.unlink()  # 清理


# ============================================================
# 3. JSON 序列化
# ============================================================

def demo_json() -> None:
    """JSON 读写."""
    data: dict[str, object] = {
        "name": "Alice",
        "age": 30,
        "roles": ["admin", "editor"],
        "active": True,
    }

    # Python → JSON 字符串
    json_str: str = json.dumps(data, ensure_ascii=False, indent=2)
    print(f"  JSON:\n{json_str}")

    # JSON 字符串 → Python
    parsed: dict[str, object] = json.loads(json_str)
    print(f"  解析后: {parsed['name']}")

    # 文件读写（推荐：显式编码）
    filepath: Path = Path(__file__).parent / "_demo.json"
    filepath.write_text(json_str, encoding="utf-8")
    loaded: dict[str, object] = json.loads(filepath.read_text(encoding="utf-8"))
    print(f"  从文件加载: {loaded}")
    filepath.unlink()


# ============================================================
# 4. CSV
# ============================================================

def demo_csv() -> None:
    """CSV 读写."""
    filepath: Path = Path(__file__).parent / "_demo.csv"

    # 写入
    rows: list[dict[str, str]] = [
        {"name": "Alice", "dept": "Engineering", "salary": "150000"},
        {"name": "Bob", "dept": "Design", "salary": "120000"},
    ]
    with filepath.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["name", "dept", "salary"])
        writer.writeheader()
        writer.writerows(rows)

    # 读取
    with filepath.open("r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            print(f"  {row['name']} — {row['dept']} — ¥{row['salary']}")

    filepath.unlink()


# ============================================================
# 5. 模块导入规则速览
# ============================================================

def explain_imports() -> None:
    """解释 import 规则."""
    print("""
    Python 模块导入规则：

    1. import 搜索路径（优先级从高到低）：
       - 当前目录
       - PYTHONPATH 环境变量
       - 标准库
       - site-packages（pip 安装的包）

    2. 导入方式：
       import os                    # 导入整个模块
       from os import path          # 导入特定名称
       from os import path as p     # 别名
       from . import sibling        # 相对导入（包内使用）

    3. __init__.py：
       - 标记目录为 Python 包
       - 可在其中定义 __all__ 控制 from package import * 的行为

    4. 当前 sys.path 前 3 项：
    """)
    for p in sys.path[:3]:
        print(f"       {p}")


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 07：文件 IO 与模块系统")
    print("=" * 50)

    demo_pathlib()
    print()
    demo_file_io()
    print()
    demo_json()
    print()
    demo_csv()
    print()
    explain_imports()
