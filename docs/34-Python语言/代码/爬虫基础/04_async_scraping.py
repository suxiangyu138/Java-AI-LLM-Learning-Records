"""
模块 04：异步爬虫（aiohttp + asyncio）。

覆盖知识点：
    - asyncio 基础：协程、事件循环、Task
    - aiohttp 并发请求
    - 信号量（Semaphore）控制并发数
    - asyncio.gather 批量执行
    - 同步 vs 异步性能对比
"""

from __future__ import annotations

import asyncio
import time
from dataclasses import dataclass, field
from typing import Any

import aiohttp


# ============================================================
# 数据模型
# ============================================================
@dataclass
class FetchResult:
    """抓取结果。"""

    url: str
    status: int
    content_length: int
    elapsed_ms: float
    error: str | None = None

    @property
    def is_ok(self) -> bool:
        return self.error is None


# ============================================================
# 异步抓取器
# ============================================================
class AsyncFetcher:
    """
    异步并发抓取器。

    核心组件：
        - Semaphore: 限制同时发出的请求数（防止被 ban / 打爆带宽）
        - TCPConnector: 限制连接池大小
        - ClientTimeout: 统一超时
    """

    def __init__(
        self,
        concurrency: int = 5,
        timeout: int = 15,
        headers: dict[str, str] | None = None,
    ) -> None:
        self.concurrency = concurrency
        self.timeout = aiohttp.ClientTimeout(total=timeout)
        self.headers = headers or {
            "User-Agent": "AsyncFetcher/2.0",
            "Accept": "*/*",
        }
        self._sem = asyncio.Semaphore(concurrency)

    async def fetch_one(self, session: aiohttp.ClientSession, url: str) -> FetchResult:
        """抓取单个 URL（受 Semaphore 限制）。"""
        start = time.perf_counter()
        async with self._sem:
            try:
                async with session.get(url, timeout=self.timeout) as resp:
                    body = await resp.read()
                    elapsed = (time.perf_counter() - start) * 1000
                    return FetchResult(
                        url=url,
                        status=resp.status,
                        content_length=len(body),
                        elapsed_ms=elapsed,
                    )
            except (aiohttp.ClientError, asyncio.TimeoutError) as e:
                elapsed = (time.perf_counter() - start) * 1000
                return FetchResult(
                    url=url, status=0, content_length=0,
                    elapsed_ms=elapsed, error=str(e),
                )

    async def fetch_many(self, urls: list[str]) -> list[FetchResult]:
        """并发抓取多个 URL。"""
        connector = aiohttp.TCPConnector(limit=self.concurrency)
        async with aiohttp.ClientSession(
            headers=self.headers, connector=connector,
        ) as session:
            tasks = [self.fetch_one(session, url) for url in urls]
            results = await asyncio.gather(*tasks, return_exceptions=True)

        output: list[FetchResult] = []
        for r in results:
            if isinstance(r, FetchResult):
                output.append(r)
            else:
                output.append(FetchResult("unknown", 0, 0, 0, str(r)))
        return output


# ============================================================
# 实战：批量请求 httpbin 的多路径
# ============================================================
async def run_async_demo() -> None:
    """异步演示（在 demo() 中通过 asyncio.run 调用）。"""
    urls = [
        "https://httpbin.org/get?q=1",
        "https://httpbin.org/get?q=2",
        "https://httpbin.org/get?q=3",
        "https://httpbin.org/get?q=4",
        "https://httpbin.org/get?q=5",
        "https://httpbin.org/delay/0.2",
        "https://httpbin.org/delay/0.2",
        "https://httpbin.org/delay/0.2",
        "https://httpbin.org/headers",
        "https://httpbin.org/ip",
    ]

    fetcher = AsyncFetcher(concurrency=5)

    start = time.perf_counter()
    results = await fetcher.fetch_many(urls)
    total_elapsed = (time.perf_counter() - start) * 1000

    ok_count = sum(1 for r in results if r.is_ok)
    fail_count = len(results) - ok_count

    print(f"\n  总数: {len(results)}  |  成功: {ok_count}  |  失败: {fail_count}")
    print(f"  总耗时: {total_elapsed:.0f}ms")
    print(f"  平均单次: {total_elapsed / len(urls):.0f}ms")

    for r in results:
        status = f"HTTP {r.status}" if r.is_ok else f"ERR: {r.error}"
        print(f"  {r.url.split('/')[-1]:20s} → {status:15s} {r.elapsed_ms:6.0f}ms")


# ============================================================
# 性能对比
# ============================================================
def sync_fetch_all(urls: list[str]) -> tuple[int, float]:
    """同步方式依次请求（用于对比）。"""
    import requests

    start = time.perf_counter()
    count = 0
    for url in urls:
        try:
            resp = requests.get(url, timeout=5)
            count += 1 if resp.ok else 0
        except requests.RequestException:
            pass
    return count, (time.perf_counter() - start) * 1000


async def async_fetch_all(urls: list[str]) -> tuple[int, float]:
    """异步方式并发请求。"""
    fetcher = AsyncFetcher(concurrency=5)
    start = time.perf_counter()
    results = await fetcher.fetch_many(urls)
    ok = sum(1 for r in results if r.is_ok)
    return ok, (time.perf_counter() - start) * 1000


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 04：异步爬虫（aiohttp）")
    print("=" * 60)

    # 1. 异步并发请求
    print("\n--- 异步批量抓取 ---")
    asyncio.run(run_async_demo())

    # 2. 同步 vs 异步对比
    print("\n--- 性能对比（3 个含延迟的 URL）---")
    test_urls = [
        "https://httpbin.org/delay/0.5",
        "https://httpbin.org/delay/0.5",
        "https://httpbin.org/delay/0.5",
    ]

    print("  同步执行中...")
    sync_ok, sync_ms = sync_fetch_all(test_urls)
    print(f"  同步: {sync_ok}/{len(test_urls)} 成功, {sync_ms:.0f}ms")

    print("  异步执行中...")
    async_ok, async_ms = asyncio.run(async_fetch_all(test_urls))
    print(f"  异步: {async_ok}/{len(test_urls)} 成功, {async_ms:.0f}ms")

    if async_ms > 0:
        print(f"  异步提速: {sync_ms / async_ms:.1f}x")

    # 3. Semaphore 信号量演示
    print("\n--- Semaphore 限流验证 ---")
    sem = asyncio.Semaphore(2)

    async def limited_task(n: int) -> str:
        async with sem:
            print(f"    任务 {n} 开始")
            await asyncio.sleep(0.1)
            print(f"    任务 {n} 结束")
            return f"result-{n}"

    async def run_limited() -> None:
        tasks = [limited_task(i) for i in range(1, 7)]
        results = await asyncio.gather(*tasks)
        print(f"  全部完成: {results}")

    asyncio.run(run_limited())


if __name__ == "__main__":
    demo()
