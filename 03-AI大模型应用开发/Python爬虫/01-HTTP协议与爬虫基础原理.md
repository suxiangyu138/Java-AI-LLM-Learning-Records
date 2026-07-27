# 01 - HTTP 协议与爬虫基础原理

> 🎯 爬虫的本质是"模拟浏览器发送 HTTP 请求"。理解 HTTP 协议的结构、请求头、Cookie、状态码，是整个爬虫技术栈的基石

---

## 目录

1. [HTTP 请求响应模型](#1-http-请求响应模型)
2. [关键请求头解析](#2-关键请求头解析)
3. [Cookie 与 Session](#3-cookie-与-session)
4. [常见状态码](#4-常见状态码)
5. [爬虫的工作流程](#5-爬虫的工作流程)

---

## 1. HTTP 请求响应模型

### 1.1 请求结构

```http
POST /api/search HTTP/1.1              ← 请求行（方法 + 路径 + 协议版本）
Host: www.example.com                   ← 请求头（Headers）
User-Agent: Mozilla/5.0 ...
Content-Type: application/json
Cookie: session_id=abc123
Authorization: Bearer token_xxx
                                        ← 空行（Header 与 Body 的分隔）
{"keyword": "python", "page": 1}        ← 请求体（Body / Payload）
```

### 1.2 响应结构

```http
HTTP/1.1 200 OK                         ← 状态行（协议版本 + 状态码 + 短语）
Content-Type: text/html; charset=utf-8  ← 响应头
Set-Cookie: session_id=xyz789
Content-Length: 1234
                                        ← 空行
<!DOCTYPE html>                         ← 响应体（Body）
<html>
<head><title>Example</title></head>
<body>...</body>
</html>
```

### 1.3 请求方法

| 方法 | 用途 | 爬虫场景 |
|------|------|---------|
| `GET` | 获取资源 | 抓取页面内容 |
| `POST` | 提交数据 | 登录、搜索、翻页 |
| `PUT` | 更新资源 | 修改数据（少用） |
| `DELETE` | 删除资源 | 清理数据（少用） |
| `HEAD` | 仅获取响应头 | 检查资源是否存在 |

## 2. 关键请求头解析

| 请求头 | 含义 | 爬虫中的重要性 |
|------|------|:---:|
| **User-Agent** | 客户端标识 | ⭐⭐⭐⭐⭐ 必设！否则直接暴露是爬虫 |
| **Cookie** | 用户会话标识 | ⭐⭐⭐⭐ 登录态/翻页必须携带 |
| **Referer** | 来源页面 URL | ⭐⭐⭐ 部分网站防盗链校验 |
| **Content-Type** | 请求体格式 | ⭐⭐⭐ POST 请求正确格式 |
| **Authorization** | 认证 Token | ⭐⭐⭐ API 接口认证 |
| **Accept** | 能接收的响应类型 | ⭐⭐ 指定期望格式 |
| **Accept-Encoding** | 压缩算法 | ⭐⭐ 设置 gzip 减少流量 |
| **X-Requested-With** | AJAX 标记 | ⭐⭐ 部分网站校验异步请求 |

### 爬虫必设的 Headers

```python
# 最小防反爬配置
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                  "AppleWebKit/537.36 (KHTML, like Gecko) "
                  "Chrome/125.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    "Accept-Encoding": "gzip, deflate, br",
    "Connection": "keep-alive",
}
```

## 3. Cookie 与 Session

### 3.1 工作机制

```text
1. 客户端（爬虫） → POST /login + 用户名密码 → 服务器
2. 服务器验证 → 创建 Session → 返回 Set-Cookie: session_id=xxx
3. 爬虫保存 Cookie
4. 后续请求携带 Cookie: session_id=xxx → 服务器识别身份
```

### 3.2 Requests 中的 Cookie 管理

```python
import requests

# 方式一：手动设置
response = requests.get(
    "https://example.com/profile",
    headers={"Cookie": "session_id=abc123"}
)

# 方式二：Session 自动管理（推荐）
session = requests.Session()
session.post("https://example.com/login", data={
    "username": "user",
    "password": "pass"
})
# Session 自动保存 Cookie，后续请求自动携带
response = session.get("https://example.com/profile")
```

## 4. 常见状态码

| 状态码 | 含义 | 爬虫应对 |
|:---:|------|---------|
| **200** | 成功 | ✅ 正常解析 |
| **301/302** | 重定向 | `allow_redirects=True`（默认）|
| **304** | 未修改 | 使用缓存（条件请求） |
| **400** | 请求错误 | 检查参数格式 |
| **401** | 未认证 | 重新登录/刷新 Token |
| **403** | 禁止访问 | ⚠️ 被反爬拦截，加 Headers/换 IP |
| **404** | 未找到 | URL 错误或资源已删除 |
| **429** | 请求过多 | ⚠️ 降低频率/换 IP |
| **500** | 服务器错误 | 重试/等待恢复 |
| **503** | 服务不可用 | 重试/等待恢复 |

## 5. 爬虫的工作流程

```text
标准爬虫流水线：
┌─────────────────────────────────────────────┐
│  1. URL 管理器 → 种子 URL / URL 去重 / 优先级  │
└──────────────────┬──────────────────────────┘
                   ↓
┌──────────────────▼──────────────────────────┐
│  2. 下载器 → Requests 发送请求 → 获取响应      │
│  关键：Headers / Cookie / 代理 / 超时 / 重试  │
└──────────────────┬──────────────────────────┘
                   ↓
┌──────────────────▼──────────────────────────┐
│  3. 解析器 → HTML → 提取目标数据 + 新 URL      │
│  工具：BS4 / XPath / Regex / LLM             │
└──────────────────┬──────────────────────────┘
                   ↓
┌──────────────────▼──────────────────────────┐
│  4. 数据管道 → 清洗 / 去重 / 格式化 → 存储     │
│  存储：CSV / JSON / MySQL / MongoDB          │
└─────────────────────────────────────────────┘
```

### 最小可运行爬虫

```python
import requests
from bs4 import BeautifulSoup

# 1. 发送请求
response = requests.get(
    "https://quotes.toscrape.com",
    headers={
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }
)

# 2. 检查状态
if response.status_code != 200:
    raise Exception(f"请求失败: {response.status_code}")

# 3. 解析数据
soup = BeautifulSoup(response.text, "html.parser")
quotes = soup.select(".quote .text")

for quote in quotes:
    print(quote.text)  # → "The world as we have created it..."
```

## 核心要点回顾

- 爬虫 = 模拟 HTTP 请求 → 获取响应 → 解析提取 → 存储
- 最小反爬配置：User-Agent + Accept + Accept-Language
- `requests.Session()` 自动管理 Cookie，比手动设置更可靠
- 状态码 403 = 反爬拦截；429 = 频率限制；30x = 重定向
- 标准爬虫流水线：URL 管理 → 下载 → 解析 → 存储

## 参考资料

1. MDN HTTP 文档 — developer.mozilla.org
2. Requests 官方文档 — docs.python-requests.org
