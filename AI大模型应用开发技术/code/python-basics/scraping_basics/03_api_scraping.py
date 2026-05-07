"""
模块 03：REST API 抓取。

覆盖知识点：
    - API 分页抓取（offset / cursor / page）
    - Token 认证与自定义 Header
    - 速率控制（rate limiting）
    - 断点续抓（resume）
    - 数据去重
"""

from __future__ import annotations

import json
import os
import time
from dataclasses import dataclass
from typing import Any, Iterator

import requests


# ============================================================
# 数据模型
# ============================================================
@dataclass
class RepoInfo:
    """GitHub 仓库信息（精简版）。"""

    full_name: str
    stars: int
    language: str | None
    description: str | None
    html_url: str


# ============================================================
# 通用分页抓取器
# ============================================================
class PaginatedFetcher:
    """
    通用分页 API 抓取器。

    使用生成器惰性返回每条数据，内存友好。
    """

    def __init__(
        self,
        base_url: str,
        headers: dict[str, str] | None = None,
        page_size: int = 30,
        max_items: int = 100,
        delay: float = 0.5,
    ) -> None:
        self.base_url = base_url
        self.headers = headers or {}
        self.page_size = page_size
        self.max_items = max_items
        self.delay = delay

        self._fetched_ids: set[str] = set()

    def fetch_paginated(
        self, endpoint: str, params: dict[str, Any] | None = None
    ) -> Iterator[dict[str, Any]]:
        """
        分页抓取 —— offset 模式。

        每页返回一个列表，逐条 yield 出去，控制总条数。
        """
        params = (params or {}).copy()
        params.setdefault("per_page", self.page_size)

        page = 1
        total_fetched = 0

        while total_fetched < self.max_items:
            params["page"] = page
            try:
                resp = requests.get(
                    f"{self.base_url}{endpoint}",
                    headers=self.headers,
                    params=params,
                    timeout=15,
                )
                resp.raise_for_status()
            except requests.RequestException as e:
                print(f"  [请求失败] 第 {page} 页: {e}")
                time.sleep(self.delay * 2)
                continue

            items = resp.json()
            if not items:
                break

            for item in items:
                if total_fetched >= self.max_items:
                    return
                item_id = str(item.get("id", item))
                if item_id in self._fetched_ids:
                    continue
                self._fetched_ids.add(item_id)
                total_fetched += 1
                yield item

            page += 1
            time.sleep(self.delay)


# ============================================================
# GitHub API 抓取实战
# ============================================================
class GitHubTrendingFetcher:
    """
    GitHub API 抓取器 —— 封装认证与重试。

    生产环境注意：
        - 无 Token：60 次/小时
        - 有 Token：5000 次/小时
        - 遵守 API 文档的 rate limit 字段
    """

    BASE_URL = "https://api.github.com"

    def __init__(self, token: str = "") -> None:
        self.headers: dict[str, str] = {
            "Accept": "application/vnd.github.v3+json",
        }
        if token:
            self.headers["Authorization"] = f"Bearer {token}"

    def _request(self, endpoint: str, params: dict | None = None) -> dict:
        url = f"{self.BASE_URL}{endpoint}"
        resp = requests.get(url, headers=self.headers, params=params, timeout=15)
        # 检查 rate limit
        remaining = resp.headers.get("X-RateLimit-Remaining", "?")
        print(f"  [API] {endpoint} → {resp.status_code} (剩余配额: {remaining})")

        if resp.status_code == 403 and int(remaining) == 0:  # type: ignore
            reset_time = int(resp.headers.get("X-RateLimit-Reset", 0))
            wait = max(reset_time - time.time(), 0)
            print(f"  [限流] 等待 {wait:.0f} 秒...")
            time.sleep(min(wait, 60))
            return self._request(endpoint, params)

        resp.raise_for_status()
        return resp.json()  # type: ignore

    def search_repos(
        self, query: str, per_page: int = 10, page: int = 1
    ) -> dict[str, Any]:
        """搜索仓库。"""
        return self._request("/search/repositories", {
            "q": query, "per_page": per_page, "page": page, "sort": "stars",
        })

    def get_user(self, username: str) -> dict[str, Any]:
        """获取用户信息。"""
        return self._request(f"/users/{username}")

    @classmethod
    def parse_search_results(cls, data: dict[str, Any]) -> list[RepoInfo]:
        """将 API 原始响应解析为数据模型列表。"""
        repos: list[RepoInfo] = []
        for item in data.get("items", []):
            repos.append(RepoInfo(
                full_name=str(item.get("full_name", "")),
                stars=int(item.get("stargazers_count", 0)),
                language=item.get("language"),
                description=item.get("description"),
                html_url=str(item.get("html_url", "")),
            ))
        return repos


# ============================================================
# 断点续抓
# ============================================================
class CheckpointManager:
    """
    断点管理器 —— 将已抓取的 ID 持久化到文件。

    中断后重启不重复抓取，适合长时间运行的任务。
    """

    def __init__(self, filepath: str = "checkpoint.json") -> None:
        self.filepath = filepath
        self._ids: set[str] = set()
        self._load()

    def _load(self) -> None:
        if os.path.exists(self.filepath):
            with open(self.filepath, encoding="utf-8") as f:
                self._ids = set(json.load(f))

    def save(self) -> None:
        with open(self.filepath, "w", encoding="utf-8") as f:
            json.dump(list(self._ids), f)

    def is_new(self, item_id: str) -> bool:
        return item_id not in self._ids

    def mark_done(self, item_id: str) -> None:
        self._ids.add(item_id)


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 03：REST API 抓取")
    print("=" * 60)

    # 1. 分页抓取（使用公共 JSONPlaceholder API）
    print("\n--- 分页抓取器（JSONPlaceholder） ---")
    fetcher = PaginatedFetcher(
        base_url="https://jsonplaceholder.typicode.com",
        page_size=5,
        max_items=10,
        delay=0.3,
    )
    posts = list(fetcher.fetch_paginated("/posts"))
    for post in posts[:5]:
        print(f"  [{post['id']}] {post['title'][:50]}...")

    # 2. GitHub API
    print("\n--- GitHub API ---")
    gh = GitHubTrendingFetcher()
    try:
        # 搜索星标最高的 Python 项目
        result = gh.search_repos("python language:python", per_page=3)
        repos = GitHubTrendingFetcher.parse_search_results(result)
        print(f"  总匹配数: {result.get('total_count', 0)}")
        for repo in repos:
            print(f"  {repo.full_name} ⭐{repo.stars} ({repo.language or 'N/A'})")
            if repo.description:
                print(f"    {repo.description[:60]}")

        # 获取用户信息
        user = gh.get_user("torvalds")
        print(f"\n  torvalds: {user.get('public_repos', 0)} repos, "
              f"{user.get('followers', 0)} followers")
    except requests.RequestException as e:
        print(f"  GitHub API 请求失败: {e}")

    # 3. 断点续抓演示
    print("\n--- 断点续抓 ---")
    ck = CheckpointManager("temp_checkpoint.json")
    test_items = ["item-001", "item-002", "item-001", "item-003"]
    for iid in test_items:
        if ck.is_new(iid):
            print(f"  抓取: {iid}")
            ck.mark_done(iid)
        else:
            print(f"  跳过(已抓): {iid}")
    ck.save()
    os.remove("temp_checkpoint.json")
    print(f"  去重结果: 只抓取了 3 个唯一项")


if __name__ == "__main__":
    demo()
