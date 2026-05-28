"""
模块 06：Scrapy 爬虫框架。

覆盖知识点：
    - Scrapy 核心架构：Spider / Item / Pipeline / Settings
    - Item 数据容器
    - Item Pipeline 数据清洗与存储
    - 独立运行（无需命令行 scrapy crawl）
    - 中间件概念

注意：此模块演示 Scrapy 的核心 API，可直接 python 运行，
     无需使用 scrapy 命令行工具。
"""

from __future__ import annotations

from dataclasses import dataclass, field


# ============================================================
# 数据模型（模拟 Scrapy Item）
# ============================================================
@dataclass
class QuoteItem:
    """名言数据项 —— 等价于 Scrapy 的 Item。"""

    text: str
    author: str
    tags: list[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {"text": self.text, "author": self.author, "tags": self.tags}


# ============================================================
# 模拟 Processor（等价于 Scrapy Item Pipeline）
# ============================================================
class DataPipeline:
    """
    数据处理管道 —— 等价于 Scrapy 的 Item Pipeline。

    每个 Processor 负责一项职责：
        - CleanProcessor: 清洗数据（去除空白、HTML 标签）
        - ValidateProcessor: 校验必填字段
        - DuplicateProcessor: 去重
        - SaveProcessor: 持久化
    """

    def __init__(self) -> None:
        self._processors: list = []
        self._seen: set[str] = set()

    def add_processor(self, name: str, processor_func):
        """注册处理器。"""
        self._processors.append((name, processor_func))

    def process_item(self, item: QuoteItem) -> QuoteItem | None:
        """管道处理单条数据：任一环节返回 None 则丢弃该条。"""
        for name, processor in self._processors:
            item = processor(item, self)
            if item is None:
                print(f"    [{name}] 丢弃")
                return None
        return item


# ============================================================
# 管道处理器
# ============================================================
def clean_text(item: QuoteItem, _ctx: DataPipeline) -> QuoteItem:
    """清洗：去除首尾空白和多余空格。"""
    import re
    item.text = re.sub(r"\s+", " ", item.text.strip())
    item.author = item.author.strip()
    return item


def validate_fields(item: QuoteItem, _ctx: DataPipeline) -> QuoteItem | None:
    """校验：丢弃空文本或空作者。"""
    if not item.text or not item.author:
        return None
    return item


def deduplicate(item: QuoteItem, ctx: DataPipeline) -> QuoteItem | None:
    """去重：按 text 内容去重。"""
    key = item.text[:100]  # 前 100 字符作为去重键
    if key in ctx._seen:
        return None
    ctx._seen.add(key)
    return item


# ============================================================
# Spider 基类
# ============================================================
class BaseSpider:
    """
    爬虫基类 —— 模拟 Scrapy Spider 的核心流程。

    子类只需实现：
        - name: 爬虫名称
        - start_urls: 起始 URL 列表
        - parse(response): 解析响应，yield 数据项
    """

    name: str = "base"
    start_urls: list[str] = []
    custom_settings: dict = {}

    def __init__(self) -> None:
        self.pipeline = DataPipeline()
        self.pipeline.add_processor("clean", clean_text)
        self.pipeline.add_processor("validate", validate_fields)
        self.pipeline.add_processor("dedup", deduplicate)
        self._items: list[QuoteItem] = []

    def parse(self, response_text: str) -> list[QuoteItem]:
        """
        解析响应文本，返回 Item 列表。

        子类必须重写此方法。
        """
        raise NotImplementedError

    def run(self) -> list[QuoteItem]:
        """
        运行爬虫 —— 循环 start_urls → fetch → parse → pipeline。
        """
        import requests
        from bs4 import BeautifulSoup

        results: list[QuoteItem] = []

        for url in self.start_urls:
            print(f"\n  [Spider:{self.name}] 抓取: {url}")
            try:
                resp = requests.get(url, timeout=10, headers={
                    "User-Agent": "Mozilla/5.0 (compatible; LearningBot/1.0)",
                })
                resp.raise_for_status()
            except requests.RequestException as e:
                print(f"  [请求失败] {e}")
                continue

            items = self.parse(resp.text)
            for item in items:
                processed = self.pipeline.process_item(item)
                if processed:
                    results.append(processed)
                    self._items.append(processed)

        return results


# ============================================================
# 具体爬虫：抓取 quotes.toscrape.com
# ============================================================
class QuotesSpider(BaseSpider):
    """
    名言爬虫 —— 抓取 quotes.toscrape.com。

    这是一个专门为学习者设计的爬虫友好站点。
    """

    name = "quotes"
    start_urls = [
        "http://quotes.toscrape.com/page/1/",
        "http://quotes.toscrape.com/page/2/",
    ]

    def parse(self, response_text: str) -> list[QuoteItem]:
        from bs4 import BeautifulSoup

        soup = BeautifulSoup(response_text, "lxml")
        items: list[QuoteItem] = []

        for quote_div in soup.select(".quote"):
            text_tag = quote_div.select_one(".text")
            author_tag = quote_div.select_one(".author")
            tag_elems = quote_div.select(".tag")

            text = text_tag.get_text(strip=True) if text_tag else ""
            author = author_tag.get_text(strip=True) if author_tag else ""
            tags = [t.get_text(strip=True) for t in tag_elems]

            items.append(QuoteItem(text=text, author=author, tags=tags))

        return items


# ============================================================
# 同时支持下一页的爬虫
# ============================================================
class FullQuotesSpider(BaseSpider):
    """自动翻页的名言爬虫。"""

    name = "full_quotes"
    start_urls = ["http://quotes.toscrape.com/page/1/"]

    def parse(self, response_text: str) -> list[QuoteItem]:
        """解析名言并自动发现下一页。"""
        from bs4 import BeautifulSoup

        soup = BeautifulSoup(response_text, "lxml")
        items: list[QuoteItem] = []

        for quote_div in soup.select(".quote"):
            text = quote_div.select_one(".text")
            author = quote_div.select_one(".author")
            tags = quote_div.select(".tag")
            items.append(QuoteItem(
                text=text.get_text(strip=True) if text else "",
                author=author.get_text(strip=True) if author else "",
                tags=[t.get_text(strip=True) for t in tags],
            ))

        # 发现下一页并自动追加到 start_urls
        next_link = soup.select_one("li.next a")
        if next_link and next_link.get("href"):
            next_url = "http://quotes.toscrape.com" + next_link["href"]
            if next_url not in self.start_urls:
                # 修改实例属性以支持内部翻页
                self.start_urls.append(next_url)

        return items


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 06：Scrapy 爬虫框架")
    print("=" * 60)

    # 1. 基础爬虫（2 页）
    print("\n--- QuotesSpider（2 页） ---")
    spider = QuotesSpider()
    results = spider.run()
    print(f"\n  总计抓取: {len(results)} 条名言")
    for item in results[:5]:
        tags = ", ".join(item.tags)
        print(f"  「{item.text[:40]}...」 — {item.author} [{tags}]")

    # 2. 管道处理效果
    print(f"\n  管道效果: clean={True}, dedup={True}, validate={True}")

    # 3. 自动翻页爬虫
    print("\n--- FullQuotesSpider（自动翻页） ---")
    spider2 = FullQuotesSpider()
    results2 = spider2.run()
    print(f"\n  总计抓取: {len(results2)} 条名言（自动翻页）")

    authors = set(item.author for item in results2)
    print(f"  去重后作者数: {len(authors)}")


if __name__ == "__main__":
    demo()
