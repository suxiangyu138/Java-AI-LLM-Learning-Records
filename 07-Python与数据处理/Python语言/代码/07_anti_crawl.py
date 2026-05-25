"""
模块 07：反爬虫应对策略。

覆盖知识点：
    - User-Agent 池随机切换
    - 请求延迟（random.uniform 模拟人类）
    - 指数退避重试
    - Referer 链伪造
    - Cookie 管理
    - 代理 IP 使用
    - robots.txt 合规检查
"""

from __future__ import annotations

import random
import time
from typing import Any
from urllib.robotparser import RobotFileParser

import requests


# ============================================================
# 1. User-Agent 池
# ============================================================
USER_AGENTS = [
    # Chrome / Windows
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    # Chrome / Mac
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    # Firefox / Windows
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0",
    # Safari / Mac
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
    # Edge / Windows
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.0.0",
]


def random_ua() -> str:
    """返回一个随机的 User-Agent。"""
    return random.choice(USER_AGENTS)


# ============================================================
# 2. 延迟策略
# ============================================================
class PoliteDelay:
    """
    礼貌延迟 —— 模拟人类浏览间隔。

    uniform(1, 3) 表示每次请求间隔 1~3 秒。
    """

    def __init__(self, min_s: float = 1.0, max_s: float = 3.0) -> None:
        self.min = min_s
        self.max = max_s

    def wait(self) -> None:
        delay = random.uniform(self.min, self.max)
        time.sleep(delay)

    def __enter__(self) -> PoliteDelay:
        self.wait()
        return self

    def __exit__(self, *args: object) -> None:
        pass


# ============================================================
# 3. 重试策略（指数退避 + 抖动）
# ============================================================
class RetryWithBackoff:
    """
    指数退避重试 —— 带随机抖动防止惊群效应。

    序列: 1s → 2s → 4s → 8s → 16s (max 30s)
    抖动: ±25%
    """

    def __init__(self, max_retries: int = 3, base_delay: float = 1.0, max_delay: float = 30.0) -> None:
        self.max_retries = max_retries
        self.base_delay = base_delay
        self.max_delay = max_delay

    def execute(self, fn, *args: Any, **kwargs: Any) -> requests.Response:
        """执行请求，自动重试。"""
        last_exc: Exception | None = None

        for attempt in range(self.max_retries + 1):
            try:
                resp = fn(*args, **kwargs)
                # 仅对服务端错误重试
                if resp.status_code < 500:
                    return resp
                print(f"  [重试] HTTP {resp.status_code} (第 {attempt + 1} 次)")
            except requests.RequestException as e:
                last_exc = e
                print(f"  [重试] {e} (第 {attempt + 1} 次)")

            if attempt < self.max_retries:
                delay = min(self.base_delay * (2 ** attempt), self.max_delay)
                jitter = random.uniform(0.75, 1.25)
                time.sleep(delay * jitter)

        if last_exc:
            raise last_exc
        raise RuntimeError("重试耗尽")


# ============================================================
# 4. robots.txt 合规检查
# ============================================================
class RobotsChecker:
    """
    robots.txt 合规检查器。

    企业级爬虫必须遵守 robots.txt 规定，
    违反可能导致法律风险和 IP 被封。

    示例：robots.txt 内容
        User-agent: *
        Disallow: /admin/
        Crawl-delay: 5
    """

    def __init__(self, base_url: str, user_agent: str = "LearningBot/1.0") -> None:
        self.parser = RobotFileParser()
        self.parser.set_url(f"{base_url.rstrip('/')}/robots.txt")
        self.user_agent = user_agent
        self._loaded = False

    def _load(self) -> None:
        if not self._loaded:
            try:
                self.parser.read()
            except Exception:
                pass
            finally:
                self._loaded = True

    def is_allowed(self, url: str) -> bool:
        """检查 URL 是否允许抓取。"""
        self._load()
        return self.parser.can_fetch(self.user_agent, url)

    def crawl_delay(self) -> float | None:
        """获取 Crawl-delay 建议值（秒）。"""
        self._load()
        delay = self.parser.crawl_delay(self.user_agent)
        return float(delay) if delay else None


# ============================================================
# 5. 企业级爬虫基类（整合所有策略）
# ============================================================
class EnterpriseCrawler:
    """
    企业级爬虫基类 —— 整合 UA 池、延迟、重试、Robot 检查。

    使用示例:
        crawler = EnterpriseCrawler("https://httpbin.org")
        resp = crawler.get("/get", params={"q": "test"})
    """

    def __init__(self, base_url: str, delay: tuple[float, float] = (1, 3)) -> None:
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.delay = PoliteDelay(*delay)
        self.retry = RetryWithBackoff(max_retries=3)
        self.robots = RobotsChecker(base_url)

        # 每次请求都换一批请求头
        self._refresh_headers()

    def _refresh_headers(self) -> None:
        self.session.headers.update({
            "User-Agent": random_ua(),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9",
            "Accept-Encoding": "gzip, deflate",
            "Cache-Control": "no-cache",
        })

    def get(self, path: str, **kwargs: Any) -> requests.Response | None:
        """发送 GET 请求，整合所有反爬策略。"""
        url = path if path.startswith("http") else f"{self.base_url}{path}"

        # 检查 robots.txt
        if not self.robots.is_allowed(url):
            print(f"  [robots.txt 禁止] {url}")
            return None

        # 礼貌延迟
        self.delay.wait()

        # 随机换 UA
        self._refresh_headers()

        # 带重试的请求
        try:
            return self.retry.execute(
                self.session.get, url, timeout=15, **kwargs,
            )
        except requests.RequestException as e:
            print(f"  [最终失败] {url}: {e}")
            return None


# ============================================================
# 6. 代理示例
# ============================================================
def demo_proxy() -> None:
    """演示代理配置（需要实际代理地址才能生效）。"""
    proxies = {
        "http": "http://127.0.0.1:7890",   # 示例：本地代理
        "https": "http://127.0.0.1:7890",
    }
    try:
        resp = requests.get(
            "https://httpbin.org/ip",
            proxies=proxies,
            timeout=5,
        )
        print(f"  代理 IP: {resp.json().get('origin', 'unknown')}")
    except requests.RequestException:
        print("  [演示] 代理不可用（需要实际代理地址）")


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 07：反爬虫应对策略")
    print("=" * 60)

    # 1. UA 池
    print("\n--- User-Agent 池 ---")
    for i in range(3):
        print(f"  [{i + 1}] {random_ua()[:70]}...")

    # 2. 延迟演示
    print("\n--- 礼貌延迟 ---")
    for i in range(3):
        delay = random.uniform(1, 3)
        print(f"  请求间隔: {delay:.2f}s")
        time.sleep(0.1)  # 演示用，不真等

    # 3. Robots.txt 合规检查
    print("\n--- robots.txt 检查 ---")
    checker = RobotsChecker("https://httpbin.org")
    test_urls = [
        "https://httpbin.org/get",
        "https://httpbin.org/admin/secret",
    ]
    for url in test_urls:
        allowed = checker.is_allowed(url)
        print(f"  {'✅' if allowed else '❌'} {url}")

    # 4. 企业级爬虫实战
    print("\n--- 企业级爬虫 ---")
    crawler = EnterpriseCrawler("https://httpbin.org", delay=(0.2, 0.5))
    for i in range(3):
        resp = crawler.get("/get", params={"page": i + 1})
        if resp:
            print(f"  第 {i + 1} 次: HTTP {resp.status_code} (UA 已轮换)")

    # 5. 代理演示
    print("\n--- 代理配置 ---")
    demo_proxy()


if __name__ == "__main__":
    demo()
