"""
Python 爬虫 —— 企业级学习示例（共 8 个模块）。

运行方式：
    python main_scraping.py                # 依次运行所有模块
    python -m scraping_basics.01_requests_basics  # 单独运行某个模块
"""

from typing import Callable

from scraping_basics import (
    anti_crawl,
    api_scraping,
    async_scraping,
    data_storage,
    html_parsing,
    real_project,
    requests_basics,
    scrapy_spider,
)


def main() -> None:
    """按顺序运行所有爬虫教学模块。"""
    modules: list[tuple[str, Callable[[], None]]] = [
        ("HTTP 请求基础", requests_basics.demo),
        ("HTML 解析", html_parsing.demo),
        ("REST API 抓取", api_scraping.demo),
        ("异步爬虫", async_scraping.demo),
        ("数据存储", data_storage.demo),
        ("Scrapy 爬虫框架", scrapy_spider.demo),
        ("反爬虫策略", anti_crawl.demo),
        ("完整实战项目", real_project.demo),
    ]

    print("\n" + "█" * 60)
    print("  Python 爬虫企业级学习示例")
    print("█" * 60)

    for i, (title, demo_fn) in enumerate(modules, 1):
        print()
        demo_fn()

    print("\n" + "█" * 60)
    print("  全部 8 个模块运行完毕！")
    print("  建议按顺序阅读源码：scraping_basics/01_*.py → 08_*.py")
    print("█" * 60 + "\n")


if __name__ == "__main__":
    main()
