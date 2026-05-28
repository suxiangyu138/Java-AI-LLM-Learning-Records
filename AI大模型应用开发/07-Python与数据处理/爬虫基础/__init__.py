"""
Python 爬虫学习包。

本包包含从入门到企业级实践的爬虫示例代码，涵盖：
    - HTTP 请求与响应
    - HTML 解析（BeautifulSoup）
    - REST API 抓取
    - 异步爬虫（aiohttp）
    - 数据存储
    - Scrapy 框架
    - 反爬应对策略
    - 完整实战项目
"""

import importlib

__version__ = "1.0.0"

_module_names = [
    "01_requests_basics",
    "02_html_parsing",
    "03_api_scraping",
    "04_async_scraping",
    "05_data_storage",
    "06_scrapy_spider",
    "07_anti_crawl",
    "08_real_project",
]

_module_aliases = [
    "requests_basics",
    "html_parsing",
    "api_scraping",
    "async_scraping",
    "data_storage",
    "scrapy_spider",
    "anti_crawl",
    "real_project",
]

__all__ = _module_aliases.copy()

for name, alias in zip(_module_names, _module_aliases):
    mod = importlib.import_module(f".{name}", package=__package__)
    globals()[alias] = mod
