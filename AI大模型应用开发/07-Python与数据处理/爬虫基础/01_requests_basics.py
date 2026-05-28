"""
模块 01：HTTP 请求基础。

覆盖知识点：
    - requests 库的核心 API
    - Session 复用连接
    - 请求头伪装
    - 查询参数与表单数据
    - 超时与异常处理
    - 响应状态码与内容处理
"""

from __future__ import annotations

import time
from typing import Any

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


# ============================================================
# 基础 GET / POST
# ============================================================
def basic_get(url: str, params: dict[str, str] | None = None) -> dict[str, Any]:
    """
    发送 GET 请求并返回 JSON 响应。

    Args:
        url: 请求地址。
        params: URL 查询参数字典。

    Returns:
        JSON 解析后的字典。

    Raises:
        requests.RequestException: 网络错误或 HTTP 错误状态码。
    """
    resp = requests.get(url, params=params, timeout=10)
    resp.raise_for_status()  # 4xx/5xx 自动抛异常
    return dict(resp.json())  # type: ignore


def basic_post(url: str, data: dict[str, Any]) -> dict[str, Any]:
    """
    发送 POST 请求并返回 JSON 响应。

    Args:
        url: 请求地址。
        data: 请求体（JSON 格式）。

    Returns:
        响应的 JSON 字典。
    """
    resp = requests.post(url, json=data, timeout=10)
    resp.raise_for_status()
    return dict(resp.json())  # type: ignore


# ============================================================
# Session 复用（企业级核心）
# ============================================================
class HTTPClient:
    """
    企业级 HTTP 客户端 —— 封装 Session、重试、超时、请求头。

    复用 Session 的好处：
        - TCP 连接复用，减少握手开销
        - 自动处理 cookie
        - 统一配置请求头与超时
    """

    DEFAULT_HEADERS: dict[str, str] = {
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/125.0.0.0 Safari/537.36"
        ),
        "Accept": "application/json, text/html, */*",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Accept-Encoding": "gzip, deflate",
    }

    def __init__(
        self,
        base_url: str = "",
        timeout: int = 15,
        max_retries: int = 3,
        custom_headers: dict[str, str] | None = None,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

        self.session = requests.Session()
        headers = {**self.DEFAULT_HEADERS, **(custom_headers or {})}
        self.session.headers.update(headers)

        # 配置重试策略（指数退避）
        retry_strategy = Retry(
            total=max_retries,
            backoff_factor=0.5,  # 0.5s → 1s → 2s ...
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["GET", "HEAD", "OPTIONS"],
        )
        adapter = HTTPAdapter(max_retries=retry_strategy)
        self.session.mount("https://", adapter)
        self.session.mount("http://", adapter)

    def _build_url(self, path: str) -> str:
        if path.startswith("http"):
            return path
        return f"{self.base_url}{path}"

    def get(self, path: str, **kwargs: Any) -> requests.Response:
        """GET 请求。"""
        url = self._build_url(path)
        resp = self.session.get(url, timeout=kwargs.pop("timeout", self.timeout), **kwargs)
        resp.raise_for_status()
        return resp

    def post(self, path: str, **kwargs: Any) -> requests.Response:
        """POST 请求。"""
        url = self._build_url(path)
        resp = self.session.post(url, timeout=kwargs.pop("timeout", self.timeout), **kwargs)
        resp.raise_for_status()
        return resp

    def get_json(self, path: str, **kwargs: Any) -> dict[str, Any]:
        """GET 并返回 JSON。"""
        return dict(self.get(path, **kwargs).json())  # type: ignore

    def close(self) -> None:
        """关闭 Session。"""
        self.session.close()

    def __enter__(self) -> HTTPClient:
        return self

    def __exit__(self, *args: object) -> None:
        self.close()


# ============================================================
# 下载文件（流式写入）
# ============================================================
def download_file(url: str, dest_path: str, chunk_size: int = 8192) -> None:
    """
    流式下载大文件，避免内存溢出。

    Args:
        url: 文件 URL。
        dest_path: 保存路径。
        chunk_size: 每次读取的字节数。
    """
    resp = requests.get(url, stream=True, timeout=30)
    resp.raise_for_status()

    total = int(resp.headers.get("content-length", 0))
    downloaded = 0
    start = time.time()

    with open(dest_path, "wb") as f:
        for chunk in resp.iter_content(chunk_size=chunk_size):
            f.write(chunk)
            downloaded += len(chunk)

    elapsed = time.time() - start
    speed = downloaded / elapsed / 1024 if elapsed > 0 else 0
    print(f"  下载完成: {dest_path} ({downloaded:,}B / {elapsed:.1f}s / {speed:.0f}KB/s)")


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 01：HTTP 请求基础")
    print("=" * 60)

    # 使用公共测试 API：httpbin.org
    BASE = "https://httpbin.org"

    # 1. 基础 GET
    print("\n--- GET 请求 ---")
    data = basic_get(f"{BASE}/get", params={"q": "python", "page": "1"})
    print(f"  URL: {data.get('url')}")

    # 2. 基础 POST
    print("\n--- POST 请求 ---")
    result = basic_post(f"{BASE}/post", data={"name": "Alice", "role": "dev"})
    print(f"  JSON 回显: {result.get('json')}")

    # 3. 企业级 HTTPClient
    print("\n--- HTTPClient(Session) ---")
    with HTTPClient(base_url=BASE) as client:
        resp = client.get("/get", params={"source": "session"})
        print(f"  状态码: {resp.status_code}")
        print(f"  Server: {resp.headers.get('Server', 'unknown')}")

        # 复用 Session 再请求一次
        resp2 = client.get("/headers")
        data2 = resp2.json()
        ua = data2.get("headers", {}).get("User-Agent", "")
        print(f"  User-Agent: {ua[:60]}...")

    # 4. 异常处理演示
    print("\n--- 超时/异常处理 ---")
    try:
        requests.get("https://httpbin.org/delay/3", timeout=1)
    except requests.exceptions.Timeout:
        print("  [预期] 请求超时被捕获")
    except requests.exceptions.RequestException as e:
        print(f"  请求异常: {e}")

    # 5. 流式下载（小文件演示）
    print("\n--- 流式下载 ---")
    download_file("https://httpbin.org/image/png", "temp_download.png")
    import os
    os.remove("temp_download.png")


if __name__ == "__main__":
    demo()
