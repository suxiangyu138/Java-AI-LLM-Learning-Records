"""
单元 10：异步编程（asyncio）

覆盖：async/await、Task、gather、create_task、asyncio.run、
异步上下文管理器、异步生成器、超时控制.
"""

from __future__ import annotations

import asyncio
import time
from collections.abc import AsyncGenerator, AsyncIterator


# ============================================================
# 1. async/await 基础
# ============================================================

async def fetch_data(source: str, delay: float = 0.1) -> str:
    """模拟异步 IO 操作（如 HTTP 请求、DB 查询）."""
    await asyncio.sleep(delay)
    return f"Data from {source}"


async def demo_basic() -> None:
    """基本 async/await."""
    # 串行（不推荐：总时间 = 各任务之和）
    t0 = time.perf_counter()
    r1 = await fetch_data("API-1")
    r2 = await fetch_data("API-2")
    print(f"  串行结果: [{r1}, {r2}] 耗时 {time.perf_counter() - t0:.3f}s")

    # 并发（推荐：用 gather）
    t0 = time.perf_counter()
    results = await asyncio.gather(
        fetch_data("API-1"),
        fetch_data("API-2"),
        fetch_data("API-3"),
    )
    print(f"  并发结果: {results} 耗时 {time.perf_counter() - t0:.3f}s")


# ============================================================
# 2. Task：精细化控制
# ============================================================

async def demo_tasks() -> None:
    """create_task 返回 Task 对象，可取消、获取结果."""
    task1 = asyncio.create_task(fetch_data("DB", delay=0.2))
    task2 = asyncio.create_task(fetch_data("Cache", delay=0.1))

    # 等待最快完成的
    done, pending = await asyncio.wait(
        [task1, task2],
        return_when=asyncio.FIRST_COMPLETED,
    )
    for t in done:
        print(f"  最快完成: {t.result()}")

    # 取消未完成的
    for t in pending:
        t.cancel()
        print(f"  已取消: {t.get_name()}")


# ============================================================
# 3. 超时控制
# ============================================================

async def fetch_with_timeout(delay: float, timeout: float = 0.05) -> str:
    """带超时的异步调用."""
    try:
        async with asyncio.timeout(timeout):
            return await fetch_data("slow-api", delay=delay)
    except TimeoutError:
        return f"请求超时 (>{timeout}s)"


# ============================================================
# 4. 异步上下文管理器
# ============================================================

class AsyncResource:
    """异步资源管理器：模拟数据库连接."""

    def __init__(self, name: str) -> None:
        self.name = name

    async def __aenter__(self) -> "AsyncResource":
        print(f"  连接 {self.name}...")
        await asyncio.sleep(0.05)
        return self

    async def __aexit__(self, *args: object) -> bool:
        print(f"  关闭 {self.name}...")
        await asyncio.sleep(0.02)
        return False

    async def query(self, sql: str) -> str:
        await asyncio.sleep(0.03)
        return f"[{self.name}] result for: {sql}"


async def demo_async_context() -> None:
    """异步上下文管理器用法."""
    async with AsyncResource("pg-db") as db:
        result = await db.query("SELECT 1")
        print(f"  {result}")


# ============================================================
# 5. 异步生成器（yield + async）
# ============================================================

async def paginated_fetch(total_pages: int) -> AsyncGenerator[str, None]:
    """异步生成器：按页获取数据."""
    for page in range(1, total_pages + 1):
        await asyncio.sleep(0.02)  # 模拟网络请求
        yield f"Page {page} data"


async def demo_async_generator() -> None:
    """消费异步生成器."""
    pages: list[str] = []
    async for page_data in paginated_fetch(3):
        pages.append(page_data)
    print(f"  分页结果: {pages}")


# ============================================================
# 6. asyncio.run() — 入口唯一调用方式
# ============================================================

async def main() -> None:
    """组装所有 demo."""
    print("1. 基础 await vs gather:")
    await demo_basic()

    print("\n2. Task 控制:")
    await demo_tasks()

    print("\n3. 超时控制:")
    result = await fetch_with_timeout(delay=0.2, timeout=0.05)
    print(f"  {result}")

    print("\n4. 异步上下文管理器:")
    await demo_async_context()

    print("\n5. 异步生成器:")
    await demo_async_generator()


if __name__ == "__main__":
    print("=" * 50)
    print("单元 10：异步编程 asyncio")
    print("=" * 50)
    asyncio.run(main())
