# 02 - Requests 库与网页抓取实战

> 🎯 Requests 是 Python 最流行的 HTTP 库（月下载量 2 亿+）。掌握 Session、代理、超时、重试四大能力，就能应对 80% 的静态网页抓取需求

---

## 目录

1. [Requests 核心 API](#1-requests-核心-api)
2. [Session 与持久连接](#2-session-与持久连接)
3. [代理与 IP 池](#3-代理与-ip-池)
4. [超时与重试策略](#4-超时与重试策略)
5. [实战：分页抓取](#5-实战分页抓取)

---

## 1. Requests 核心 API

### 1.1 GET 请求

```python
import requests

# 基础 GET
resp = requests.get("https://httpbin.org/get")

# 携带参数（自动 URL 编码）
resp = requests.get("https://httpbin.org/get", params={
    "page": 1,
    "size": 20,
    "keyword": "python爬虫"
})
# → https://httpbin.org/get?page=1&size=20&keyword=python%E7%88%AC%E8%99%AB

# 自定义 Headers
resp = requests.get("https://httpbin.org/get", headers={
    "User-Agent": "Mozilla/5.0 ...",
    "Referer": "https://www.google.com/"
})
```

### 1.2 POST 请求

```python
# Form 表单提交
resp = requests.post("https://httpbin.org/post", data={
    "username": "admin",
    "password": "123456"
})

# JSON 提交
resp = requests.post("https://httpbin.org/post", json={
    "name": "test",
    "value": 42
})

# 文件上传
resp = requests.post("https://httpbin.org/post", files={
    "file": open("report.pdf", "rb")
})
```

### 1.3 响应处理

```python
resp = requests.get("https://example.com")

# 状态码
print(resp.status_code)      # 200
print(resp.ok)               # True (200-399)

# 文本内容
print(resp.text)             # str (自动推断编码)
print(resp.content)          # bytes

# JSON 响应
data = resp.json()           # 直接 parse JSON

# 响应头
print(resp.headers["Content-Type"])
print(resp.cookies.get("session_id"))

# 编码处理
resp.encoding = "utf-8"      # 强制指定编码
```

## 2. Session 与持久连接

### 2.1 为什么用 Session

```python
# ❌ 不用 Session：每次都是新连接，Cookie 不共享
r1 = requests.get("https://example.com/login")
r2 = requests.get("https://example.com/profile")  # Cookie 丢失

# ✅ 用 Session：自动管理 Cookie + 连接池复用
session = requests.Session()

# 登录
session.post("https://example.com/login", data={
    "username": "user", "password": "pass"
})

# 自动携带登录后的 Cookie
profile = session.get("https://example.com/profile")

# 全局默认 Headers
session.headers.update({
    "User-Agent": "Mozilla/5.0 ...",
    "Accept-Language": "zh-CN,zh;q=0.9"
})
```

### 2.2 Session 连接池

```python
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

session = requests.Session()

# 配置连接池
adapter = HTTPAdapter(
    pool_connections=10,        # 连接池大小
    pool_maxsize=20,            # 最大连接数
    max_retries=Retry(total=3, backoff_factor=0.5)
)
session.mount("https://", adapter)
session.mount("http://", adapter)
```

## 3. 代理与 IP 池

### 3.1 代理设置

```python
# HTTP 代理
proxies = {
    "http": "http://127.0.0.1:7890",
    "https": "http://127.0.0.1:7890",
}
resp = requests.get("https://api.example.com", proxies=proxies)

# 带认证的代理
proxies = {
    "http": "http://user:pass@proxy.com:8080",
}

# SOCKS5 代理（需 pip install 'requests[socks]'）
proxies = {
    "http": "socks5://127.0.0.1:1080",
    "https": "socks5://127.0.0.1:1080",
}
```

### 3.2 轮换 IP 池

```python
import random

class ProxyPool:
    """简易 IP 代理池"""

    def __init__(self, proxies: list[str]):
        self.proxies = proxies
        self.index = 0

    def get(self) -> str:
        """轮询获取代理"""
        proxy = self.proxies[self.index % len(self.proxies)]
        self.index += 1
        return proxy

    def get_random(self) -> str:
        """随机获取代理"""
        return random.choice(self.proxies)

    def remove(self, proxy: str):
        """移除失效代理"""
        if proxy in self.proxies:
            self.proxies.remove(proxy)

# 使用
pool = ProxyPool([
    "http://proxy1:8080",
    "http://proxy2:8080",
    "http://proxy3:8080",
])

for url in urls:
    proxy = pool.get_random()
    try:
        resp = requests.get(url, proxies={"http": proxy}, timeout=5)
    except requests.RequestException:
        pool.remove(proxy)   # 失效则移除
```

## 4. 超时与重试策略

```python
# 超时设置（强烈建议永远设！）
resp = requests.get(
    "https://example.com",
    timeout=(3.05, 10)    # (连接超时, 读取超时)
)

# 自动重试封装
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

def create_retry_session(
    retries=3,
    backoff_factor=1,
    status_forcelist=[429, 500, 502, 503, 504]
) -> requests.Session:
    """创建带自动重试的 Session"""
    session = requests.Session()
    retry = Retry(
        total=retries,
        backoff_factor=backoff_factor,     # 退避: 1s → 2s → 4s
        status_forcelist=status_forcelist, # 这些状态码触发重试
        allowed_methods=["GET", "POST"]    # 允许重试的方法
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("https://", adapter)
    session.mount("http://", adapter)
    return session
```

## 5. 实战：分页抓取

```python
import requests
from bs4 import BeautifulSoup
import time

def crawl_pages(base_url: str, max_pages: int = 10):
    """通用分页爬虫"""
    session = create_retry_session()
    session.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    })

    results = []

    for page in range(1, max_pages + 1):
        print(f"正在抓取第 {page} 页...")

        try:
            resp = session.get(
                base_url,
                params={"page": page},      # 翻页参数
                timeout=10
            )

            if resp.status_code == 404:     # 最后一页
                break

            if resp.status_code == 429:     # 被限流
                wait = int(resp.headers.get("Retry-After", 60))
                print(f"限流！等待 {wait} 秒...")
                time.sleep(wait)
                continue

            if not resp.ok:
                print(f"请求失败: {resp.status_code}")
                continue

            # 解析（具体逻辑按网站实际结构）
            soup = BeautifulSoup(resp.text, "html.parser")
            items = soup.select(".item")          # ← 按实际 CSS 选择器
            results.extend(items)

            # 礼貌延迟
            time.sleep(1 + random.random() * 2)  # 1-3 秒随机间隔

        except requests.Timeout:
            print(f"第 {page} 页超时，跳过")
            continue

    return results
```

## 核心要点回顾

- `params` 参数自动 URL 编码，比手动拼接 URL 更安全
- `session.post()` + `session.get()` = 自动 Cookie 管理
- `timeout=(3, 10)` 必须设！否则请求可能无限挂起
- 重试策略：`Retry(total=3, backoff_factor=1)` 指数退避
- 代理轮换：随机选取 + 失效移除
- 爬虫礼仪：`time.sleep(1-3)` 随机间隔，避免 429

## 参考资料

1. Requests 官方文档 — docs.python-requests.org
2. urllib3 Retry 文档
