"""
模块 05：数据存储。

覆盖知识点：
    - CSV 写入（标准库 csv）
    - JSON 行格式（逐条追加，适合大规模写入）
    - SQLite 存储（轻量数据库）
    - 统一存储接口
"""

from __future__ import annotations

import csv
import json
import sqlite3
import time
from abc import ABC, abstractmethod
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any


# ============================================================
# 数据模型
# ============================================================
@dataclass
class Article:
    """文章数据模型。"""

    title: str
    url: str
    author: str
    publish_date: str
    word_count: int = 0


# ============================================================
# 存储策略接口
# ============================================================
class DataStorage(ABC):
    """数据存储抽象接口。"""

    @abstractmethod
    def save(self, item: dict[str, Any]) -> None:
        """保存单条数据。"""

    @abstractmethod
    def save_batch(self, items: list[dict[str, Any]]) -> None:
        """批量保存。"""

    @abstractmethod
    def close(self) -> None:
        """关闭资源。"""

    def __enter__(self):
        return self

    def __exit__(self, *args: object) -> None:
        self.close()


# ============================================================
# CSV 存储
# ============================================================
class CSVStorage(DataStorage):
    """CSV 文件存储 —— 适合 Excel 用户和数据分析。"""

    def __init__(self, filepath: str, fieldnames: list[str] | None = None) -> None:
        self.filepath = Path(filepath)
        self.fieldnames = fieldnames
        self._file = open(self.filepath, "w", newline="", encoding="utf-8-sig")
        self._writer: csv.DictWriter | None = None

    def _ensure_header(self, item: dict[str, Any]) -> None:
        if self._writer is not None:
            return
        fields = self.fieldnames or list(item.keys())
        self._writer = csv.DictWriter(self._file, fieldnames=fields)
        self._writer.writeheader()

    def save(self, item: dict[str, Any]) -> None:
        self._ensure_header(item)
        if self._writer:
            self._writer.writerow(item)

    def save_batch(self, items: list[dict[str, Any]]) -> None:
        for item in items:
            self.save(item)

    def close(self) -> None:
        self._file.close()


# ============================================================
# JSON Lines 存储（逐条追加）
# ============================================================
class JSONLinesStorage(DataStorage):
    """
    JSON Lines 存储 —— 每行一个 JSON 对象。

    优势：
        - 追加写入不需要重写整个文件
        - 崩溃后已写入的数据不丢失
        - 可按行读取，适合大文件
    """

    def __init__(self, filepath: str) -> None:
        self.filepath = Path(filepath)
        self._file = open(self.filepath, "a", encoding="utf-8")
        self._count = 0

    def save(self, item: dict[str, Any]) -> None:
        self._file.write(json.dumps(item, ensure_ascii=False) + "\n")
        self._count += 1
        # 每 100 条 flush 一次
        if self._count % 100 == 0:
            self._file.flush()

    def save_batch(self, items: list[dict[str, Any]]) -> None:
        for item in items:
            self.save(item)

    def close(self) -> None:
        self._file.flush()
        self._file.close()

    @property
    def count(self) -> int:
        return self._count


# ============================================================
# SQLite 存储
# ============================================================
class SQLiteStorage(DataStorage):
    """
    SQLite 存储 —— 适合需要查询的结构化数据。

    自动建表，使用 executemany 批量插入。
    """

    def __init__(self, db_path: str, table_name: str = "articles") -> None:
        self.conn = sqlite3.connect(db_path)
        self.table = table_name
        self._inferred_schema = False

    def _ensure_table(self, item: dict[str, Any]) -> None:
        if self._inferred_schema:
            return
        # 从第一条数据推断列类型
        type_map = {str: "TEXT", int: "INTEGER", float: "REAL", bool: "INTEGER"}
        cols: list[str] = []
        for key, val in item.items():
            # 清理列名（防止 SQL 注入）
            safe_key = key.replace('"', '""')
            col_type = type_map.get(type(val), "TEXT")
            cols.append(f'"{safe_key}" {col_type}')
        ddl = f'CREATE TABLE IF NOT EXISTS "{self.table}" ({", ".join(cols)})'
        self.conn.execute(ddl)
        self.conn.commit()
        self._inferred_schema = True

    def save(self, item: dict[str, Any]) -> None:
        self._ensure_table(item)
        placeholders = ", ".join("?" for _ in item)
        cols = ", ".join(f'"{k}"' for k in item)
        self.conn.execute(
            f'INSERT INTO "{self.table}" ({cols}) VALUES ({placeholders})',
            tuple(item.values()),
        )

    def save_batch(self, items: list[dict[str, Any]]) -> None:
        if not items:
            return
        self._ensure_table(items[0])
        placeholders = ", ".join("?" for _ in items[0])
        cols = ", ".join(f'"{k}"' for k in items[0])
        rows = [tuple(item.values()) for item in items]
        self.conn.executemany(
            f'INSERT INTO "{self.table}" ({cols}) VALUES ({placeholders})',
            rows,
        )
        self.conn.commit()

    def query(self, sql: str, params: tuple = ()) -> list[dict[str, Any]]:
        """执行查询并返回字典列表。"""
        cursor = self.conn.execute(sql, params)
        columns = [col[0] for col in cursor.description]
        return [dict(zip(columns, row)) for row in cursor.fetchall()]

    def close(self) -> None:
        self.conn.commit()
        self.conn.close()


# ============================================================
# 统一导出工具
# ============================================================
def save_articles_to_all_formats(
    articles: list[Article],
    output_dir: str = "output",
) -> dict[str, str]:
    """将文章列表同时保存为 CSV、JSONL、SQLite 三种格式。"""
    output = Path(output_dir)
    output.mkdir(exist_ok=True)

    items = [asdict(a) for a in articles]

    paths: dict[str, str] = {}

    # CSV
    csv_file = str(output / "articles.csv")
    with CSVStorage(csv_file) as storage:  # type: ignore
        storage.save_batch(items)
    paths["csv"] = csv_file

    # JSONL
    jsonl_file = str(output / "articles.jsonl")
    with JSONLinesStorage(jsonl_file) as storage:  # type: ignore
        storage.save_batch(items)
    paths["jsonl"] = jsonl_file

    # SQLite
    db_file = str(output / "articles.db")
    with SQLiteStorage(db_file) as storage:  # type: ignore
        storage.save_batch(items)
    paths["sqlite"] = db_file

    return paths


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 05：数据存储")
    print("=" * 60)

    # 模拟抓取数据
    articles = [
        Article("Python 异步编程指南", "https://example.com/1", "Alice", "2025-06-01", 3500),
        Article("Django vs FastAPI", "https://example.com/2", "Bob", "2025-06-03", 4200),
        Article("Pandas 数据分析实战", "https://example.com/3", "Carol", "2025-06-05", 2800),
    ]

    # 1. 保存到多种格式
    print("\n--- 多格式存储 ---")
    paths = save_articles_to_all_formats(articles, output_dir="output")
    for fmt, path in paths.items():
        size = Path(path).stat().st_size
        print(f"  {fmt.upper():7s}: {path} ({size}B)")

    # 2. 从 SQLite 查询
    print("\n--- SQLite 查询 ---")
    storage = SQLiteStorage("output/articles.db")
    # 先插入数据
    storage.save_batch([asdict(a) for a in articles])
    results = storage.query("SELECT title, author FROM articles WHERE word_count > 3000")
    for row in results:
        print(f"  {row['title']} — {row['author']}")
    storage.close()

    # 3. 验证 JSONL 内容
    print("\n--- JSONL 内容验证 ---")
    with open("output/articles.jsonl", encoding="utf-8") as f:
        for i, line in enumerate(f, 1):
            obj = json.loads(line)
            print(f"  行 {i}: {obj['title']}")

    # 清理
    import shutil
    shutil.rmtree("output")


if __name__ == "__main__":
    demo()
