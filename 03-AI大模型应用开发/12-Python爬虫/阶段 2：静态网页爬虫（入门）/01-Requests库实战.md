# Requests 库实战
> GET/POST、params 与 headers、Session 会话、响应对象三件套——爬虫请求的"主力 API"，全部实战细节

## 📚 目录
1. [Requests 定位与版本（2026）](#1-requests-定位与版本2026)
2. [GET 请求全解](#2-get-请求全解)
3. [POST 请求全解](#3-post-请求全解)
4. [headers 与参数构造](#4-headers-与参数构造)
5. [Session：会话保持](#5-session会话保持)
6. [响应对象处理](#6-响应对象处理)
7. [Requests 工程习惯清单](#7-requests-工程习惯清单)

## 1. Requests 定位与版本（2026）

| 事实 | 说明 |
|------|------|
| 当前版本 | **2.34.2**（2026-05-14） |
| Python 要求 | 3.10+（2.33 起放弃 3.9） |
| 定位 | 最流行的 Python HTTP 客户端（"HTTP for Humans"） |
| 2026 变化 | 内联类型（2.34 起）、`Response.reason` 类型收紧、CVE-2026-25645 已修复 |
| 替代 | httpx（异步）、aiohttp（异步，阶段 6）——本阶段只学 requests |

> 🎯 与 [阶段 1 HTTP 基础](../阶段%201：前置基础（必备）/02-HTTP协议基础（爬虫视角）.md) 的衔接：requests 就是"用代码说 HTTP 对话"——每个参数对应报文的一个部分，理论已备、此处练手。

## 2. GET 请求全解

```python
import requests

# ═══ 最简 GET ═══
resp = requests.get("https://example.com", timeout=10)
print(resp.status_code, resp.url)

# ═══ 带查询参数（params 自动拼 query string）═══
resp = requests.get(
    "https://example.com/api/news",
    params={"page": 1, "size": 20, "keyword": "爬虫"},
    timeout=10,
)
# 实际请求：https://example.com/api/news?page=1&size=20&keyword=%E7%88%AC%E8%99%AB
# 中文/特殊字符自动 URL 编码（别自己拼！）

# ═══ 带请求头（UA 伪装，见阶段 1 §5）═══
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                  "AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36",
    "Referer": "https://example.com/list",
}
resp = requests.get(url, headers=headers, timeout=10)

# ═══ 重定向：默认自动跟随（可关闭）═══
resp = requests.get(url, allow_redirects=False, timeout=10)   # 看 302 原始响应
```

| GET 参数 | 说明 |
|---------|------|
| `params` | 查询参数（**自动编码**，别手拼 query string） |
| `headers` | 请求头（UA/Referer/Cookie） |
| `timeout` | **必设**（连接 + 读取超时） |
| `allow_redirects` | 是否跟随 301/302（默认 True） |
| `cookies` | 手动带 Cookie（Session 场景更优） |

> ⚠️ **params vs 手拼**：`params` 自动 URL 编码（中文/`&`/`=` 安全）；手拼 `?page=1&kw=中文` 可能编码不一致导致服务器解析错乱——**一律用 params**。

## 3. POST 请求全解

```python
import requests

# ═══ 表单提交（application/x-www-form-urlencoded）═══
resp = requests.post(
    "https://example.com/api/login",
    data={"username": "admin", "password": "secret"},
    timeout=10,
)

# ═══ JSON 提交（application/json）═══
resp = requests.post(
    "https://example.com/api/orders",
    json={"userId": 42, "items": [{"id": 1, "qty": 2}]},
    timeout=10,
)
# json= 自动序列化 + 设置 Content-Type

# ═══ 文件上传（multipart）═══
resp = requests.post(
    "https://example.com/api/upload",
    files={"file": ("photo.jpg", open("photo.jpg", "rb"), "image/jpeg")},
    data={"desc": "测试"},
    timeout=30,
)
```

| POST 参数 | Content-Type | 场景 |
|----------|:---:|------|
| `data={}` | form-urlencoded | 登录/表单 |
| `json={}` | application/json | **API 接口（主流）** |
| `files={}` | multipart/form-data | 文件上传 |
| `data="原始字符串"` | 自定义 | 特殊接口（需配合 headers） |

> ⚠️ 常见错误：**接口要 JSON 你传了 data（或反之）**——服务器 400/解析失败。判断标准：F12 Network 里看接口的 Content-Type 是 `application/json` 还是 `application/x-www-form-urlencoded`（阶段 1 §7 五步法已学）。

## 4. headers 与参数构造

```python
# ═══ headers 构造规范 ═══
HEADERS = {
    "User-Agent": ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                   "AppleWebKit/537.36 (KHTML, like Gecko) "
                   "Chrome/126.0.0.0 Safari/537.36"),
    "Accept": "application/json, text/html, */*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    "Referer": "https://example.com/",
}
# 建议：HEADERS 定义在模块级，请求时复用（别每个请求重写）

# ═══ 会话内统一设置（推荐）═══
session = requests.Session()
session.headers.update(HEADERS)          # 所有请求自动带
session.headers["Referer"] = "https://example.com/page2"   # 单次覆盖
```

| Header 技巧 | 说明 |
|------------|------|
| 模块级 HEADERS 常量 | 一处定义、处处复用 |
| Session 统一头 | 所有请求自动携带（少传参） |
| 单请求覆盖 | 特定请求临时改（如 Referer） |
| Cookie 头 | 别手写，用 Session（§5） |

> 🎯 工程习惯：**"headers 进常量、请求参数进函数"**——爬虫代码的公共头（UA/Accept）只写一次；业务参数（页码/关键词）走函数参数。

## 5. Session：会话保持

```python
import requests

# ═══ Session = 浏览器会话模拟 ═══
session = requests.Session()
session.headers.update({"User-Agent": "Mozilla/5.0 ..."})

# ① 首次访问：服务器 Set-Cookie → Session 自动保存
resp1 = session.get("https://example.com", timeout=10)

# ② 登录：POST 提交表单 → 服务器下发登录态 Cookie
resp2 = session.post(
    "https://example.com/api/login",
    json={"username": "admin", "password": "secret"},
    timeout=10,
)

# ③ 后续请求：自动带 Cookie（登录态保持）
resp3 = session.get("https://example.com/me", timeout=10)
# 若登录成功，resp3 返回的是登录后的数据

# ═══ 查看会话 Cookie ═══
print(session.cookies.get_dict())        # {'session_id': 'xxx', ...}
```

| Session 能力 | 说明 |
|-------------|------|
| Cookie 自动管理 | Set-Cookie 自动存、请求自动带 |
| 统一 headers | `session.headers.update()` 全局生效 |
| 连接复用 | 底层连接池（性能） |
| 登录态保持 | 登录后所有请求自动带会话 |

> 🎯 面试/实战点：**"Session 对象 = 模拟浏览器会话"**——登录爬虫的第一步就是 `session.post(登录接口)` 然后 `session.get(需登录页面)`。Cookie 机制理论见[阶段 1 §6](../阶段%201：前置基础（必备）/02-HTTP协议基础（爬虫视角）.md)。

## 6. 响应对象处理

```python
resp = requests.get(url, timeout=10)

# ═══ 三件套（永远先看）═══
resp.status_code            # 200/403/404/429...
resp.headers                # 响应头（Content-Type/Set-Cookie）
resp.url                    # 最终 URL（重定向后！）

# ═══ 内容获取 ═══
resp.text                   # 文本（HTML/JSON）——自动按编码解码
resp.content                # 原始字节（图片/文件/需手动解码）
resp.json()                 # JSON 直接解析（Content-Type 不对会报错）
resp.encoding               # 当前解码编码（乱码排查，见阶段 1 §5）

# ═══ 二进制保存（图片下载）═══
with open("photo.jpg", "wb") as f:
    f.write(resp.content)

# ═══ 流式下载（大文件）═══
with requests.get(url, stream=True, timeout=30) as resp:
    with open("big.zip", "wb") as f:
        for chunk in resp.iter_content(chunk_size=8192):
            f.write(chunk)
```

| 响应方法 | 场景 | 注意 |
|---------|------|------|
| `resp.text` | HTML/JSON 文本 | 编码推断可能错（乱码时用 content.decode） |
| `resp.content` | 二进制 | 图片/文件/乱码急救 |
| `resp.json()` | 接口 JSON | 先确认 Content-Type 或 try-except |
| `iter_content` | **大文件流式** | 别 read() 全量进内存 |

> ⚠️ 大文件铁律：**下载大文件用 `stream=True + iter_content`**——直接 `resp.content` 会把整个文件读进内存（视频/安装包 = OOM）。与 [阶段 1 §3](../阶段%201：前置基础（必备）/02-HTTP协议基础（爬虫视角）.md) 的"响应三件套"呼应。

## 7. Requests 工程习惯清单

```text
爬虫请求代码自查（每条都打勾）：
  □ 每个请求都有 timeout（10-30s，按场景）
  □ headers 用模块级常量 + UA 伪装
  □ 查询参数走 params（不手拼）
  □ 接口 JSON 用 json=、表单用 data=
  □ 登录态用 Session（不手动拼 Cookie）
  □ 响应先看 status_code 再处理
  □ 大文件 stream + iter_content
  □ 异常处理 + 重试（见 02）
  □ 编码问题用 content.decode 显式控制
```

> 💡 下一步：requests 能发请求了，但**"请求失败怎么办"**（异常/重试/代理）还没解决——[02-请求进阶](02-请求进阶异常重试代理.md) 是爬虫健壮性的第一课。

---

**下一模块**：[02-请求进阶异常重试代理](02-请求进阶异常重试代理.md) / **返回总览**：[00-阶段2静态网页爬虫总览](00-阶段2静态网页爬虫总览.md)
