"""
模块 02：HTML 解析（BeautifulSoup）。

覆盖知识点：
    - BeautifulSoup 创建与解析器选择
    - CSS 选择器 vs find/find_all
    - 提取文本、属性、链接
    - 表格与列表解析
    - 嵌套结构遍历
    - 数据清洗
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Any

from bs4 import BeautifulSoup, ResultSet, Tag


# ============================================================
# 模拟数据（真实场景替换为 requests.get 的响应）
# ============================================================
SAMPLE_HTML = """
<!DOCTYPE html>
<html lang="zh">
<head><title>示例书单</title></head>
<body>
    <div class="container">
        <h1 class="page-title">Python 编程书单</h1>
        <p class="subtitle">共 <span id="book-count">3</span> 本书</p>

        <ul class="book-list">
            <li class="book-item" data-id="001">
                <a href="/books/python-cookbook" class="book-link">
                    <h3 class="book-title">Python Cookbook</h3>
                    <span class="book-author">David Beazley</span>
                </a>
                <span class="book-price">¥89.00</span>
                <span class="book-rating" data-stars="4.8">★★★★☆</span>
                <span class="book-tags">
                    <span class="tag">进阶</span>
                    <span class="tag">实战</span>
                </span>
            </li>
            <li class="book-item" data-id="002">
                <a href="/books/fluent-python" class="book-link">
                    <h3 class="book-title">Fluent Python</h3>
                    <span class="book-author">Luciano Ramalho</span>
                </a>
                <span class="book-price">¥139.00</span>
                <span class="book-rating" data-stars="4.9">★★★★★</span>
                <span class="book-tags">
                    <span class="tag">进阶</span>
                    <span class="tag">元编程</span>
                </span>
            </li>
            <li class="book-item" data-id="003">
                <a href="/books/effective-python" class="book-link">
                    <h3 class="book-title">Effective Python</h3>
                    <span class="book-author">Brett Slatkin</span>
                </a>
                <span class="book-price">¥109.00</span>
                <span class="book-rating" data-stars="4.7">★★★★☆</span>
                <span class="book-tags">
                    <span class="tag">入门</span>
                    <span class="tag">最佳实践</span>
                </span>
            </li>
        </ul>

        <!-- 表格数据 -->
        <table class="sales-table">
            <thead>
                <tr><th>书名</th><th>月销量</th><th>库存</th></tr>
            </thead>
            <tbody>
                <tr><td>Python Cookbook</td><td>1200</td><td class="in-stock">有货</td></tr>
                <tr><td>Fluent Python</td><td>890</td><td class="in-stock">有货</td></tr>
                <tr><td>Effective Python</td><td>650</td><td class="out-of-stock">售罄</td></tr>
            </tbody>
        </table>
    </div>
</body>
</html>
"""


# ============================================================
# 数据模型
# ============================================================
@dataclass
class Book:
    """书籍数据模型。"""

    data_id: str
    title: str
    author: str
    price: float
    rating: float
    tags: list[str] = field(default_factory=list)


@dataclass
class SalesRecord:
    """销售记录。"""

    title: str
    monthly_sales: int
    stock_status: str


# ============================================================
# 解析器
# ============================================================
class BookListParser:
    """
    书单解析器 —— 从 HTML 中提取结构化数据。

    使用 CSS 选择器的场景：
        - 按 class/id 定位：.book-item, #book-count
        - 层级选择：.book-list > li
        - 属性选择：[data-id]

    使用 find/find_all 的场景：
        - 复杂条件组合
        - 需要 lambda 过滤
    """

    def __init__(self, html: str, parser: str = "lxml") -> None:
        """
        Args:
            html: HTML 字符串。
            parser: 解析器（lxml 最快，html.parser 是标准库）。
        """
        self.soup = BeautifulSoup(html, parser)

    def get_page_title(self) -> str:
        """获取页面标题（CSS 选择器）。"""
        tag = self.soup.select_one(".page-title")
        return tag.get_text(strip=True) if tag else ""

    def get_book_count(self) -> int:
        """获取书本数量（ID 选择器）。"""
        tag = self.soup.find("span", id="book-count")
        return int(tag.text) if tag else 0

    def parse_books(self) -> list[Book]:
        """
        解析书籍列表 —— CSS 选择器 + 异常容忍。

        生产环境的关键：每个字段都可能缺失，需要安全取值。
        """
        books: list[Book] = []
        items: ResultSet[Tag] = self.soup.select(".book-item")  # type: ignore

        for item in items:
            data_id = str(item.get("data-id", ""))

            title_tag = item.select_one(".book-title")
            title = title_tag.get_text(strip=True) if title_tag else ""

            author_tag = item.select_one(".book-author")
            author = author_tag.get_text(strip=True) if author_tag else ""

            price_tag = item.select_one(".book-price")
            price = 0.0
            if price_tag:
                price = float(re.sub(r"[^\d.]", "", price_tag.text))

            rating_tag = item.select_one(".book-rating")
            rating = 0.0
            if rating_tag:
                rating = float(rating_tag.get("data-stars", 0))

            tags = [t.get_text(strip=True) for t in item.select(".tag")]

            books.append(Book(data_id, title, author, price, rating, tags))

        return books

    def parse_sales_table(self) -> list[SalesRecord]:
        """解析销售表格 —— find_all 遍历。"""
        records: list[SalesRecord] = []
        table = self.soup.find("table", class_="sales-table")
        if table is None:
            return records

        rows = table.select("tbody tr")
        for row in rows:
            cols = row.find_all("td")
            if len(cols) < 3:
                continue
            title = cols[0].get_text(strip=True)
            sales = int(cols[1].get_text(strip=True))
            stock = cols[2].get_text(strip=True)
            records.append(SalesRecord(title, sales, stock))

        return records

    def get_all_links(self) -> list[dict[str, str]]:
        """提取页面所有链接。"""
        links: list[dict[str, str]] = []
        for a in self.soup.find_all("a", href=True):
            links.append({
                "text": a.get_text(strip=True),
                "href": str(a.get("href", "")),
            })
        return links

    def search_by_text(self, keyword: str) -> list[str]:
        """
        按文本内容搜索元素 —— 演示 lambda 过滤。

        Args:
            keyword: 搜索关键词。

        Returns:
            匹配元素的文本列表。
        """
        matches = self.soup.find_all(
            lambda tag: tag.name and keyword.lower() in tag.get_text().lower()
        )
        return [m.get_text(strip=True)[:80] for m in matches]


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 02：HTML 解析（BeautifulSoup）")
    print("=" * 60)

    parser = BookListParser(SAMPLE_HTML, parser="html.parser")

    # 页面信息
    print(f"\n页面标题: {parser.get_page_title()}")
    print(f"书本数量: {parser.get_book_count()}")

    # 书籍列表
    print("\n--- 书籍列表 ---")
    for book in parser.parse_books():
        tags = ", ".join(book.tags)
        print(f"  [{book.data_id}] {book.title}")
        print(f"    作者: {book.author}  |  ¥{book.price:.2f}  |  {book.rating}★  |  {tags}")

    # 销售表格
    print("\n--- 销售表格 ---")
    for rec in parser.parse_sales_table():
        print(f"  {rec.title}: 月销 {rec.monthly_sales} / {rec.stock_status}")

    # 所有链接
    print("\n--- 页面链接 ---")
    for link in parser.get_all_links():
        print(f"  {link['text']} → {link['href']}")

    # 文本搜索
    print("\n--- 搜索 'Python' ---")
    for text in parser.search_by_text("Python"):
        if len(text) < 60:
            print(f"  {text}")

    # 通用技巧：处理缺失字段
    print("\n--- 缺失字段安全取值 ---")
    tag = parser.soup.select_one(".non-existent")
    print(f"  select_one 不存在: {tag}")  # None，不抛异常
    print(f"  get_text 安全取值: '{tag.get_text(strip=True) if tag else 'N/A'}'")


if __name__ == "__main__":
    demo()
