# HTTP 协议基础（爬虫视角）
> 爬虫就是"模拟浏览器的 HTTP 对话"：请求怎么发、响应怎么读、状态码怎么判定、Cookie 怎么带上——爬虫的第一性原理

## 📚 目录
1. [爬虫的本质：一次 HTTP 对话](#1-爬虫的本质一次-http-对话)
2. [请求报文：爬虫要会"说"](#2-请求报文爬虫要会说)
3. [响应报文：爬虫要会"听"](#3-响应报文爬虫要会听)
4. [状态码：爬虫的判定字典](#4-状态码爬虫的判定字典)
5. [Headers：服务器识别你是谁](#5-headers服务器识别你是谁)
6. [Cookie 与 Session：登录态的秘密](#6-cookie-与-session登录态的秘密)
7. [HTTPS 与证书](#7-https-与证书)
8. [爬虫的 HTTP 基本功清单](#8-爬虫的-http-基本功清单)

## 1. 爬虫的本质：一次 HTTP 对话

```text
浏览器访问网页 = HTTP 对话：
  浏览器 ──GET /page HTTP/1.1──> 服务器
  浏览器 <──200 OK + HTML───── 服务器

爬虫访问网页 = 用代码代替浏览器，说同样的 HTTP 对话：
  requests.get("https://example.com")
  = 发出 GET 请求 + 接收响应

爬虫与浏览器的区别：
  浏览器：请求 + 渲染 + 执行 JS（给人看）
  爬虫：  请求 + 解析（给程序用）
```

> 🎯 一句话：**爬虫 = 用代码精确复刻浏览器的 HTTP 行为**。理解了 HTTP 对话，爬虫就不神秘——后面所有"反爬"（UA 校验、Cookie 校验、登录态）都是 HTTP 对话的附加规则。

## 2. 请求报文：爬虫要会"说"

```http
GET /api/articles?page=1 HTTP/1.1        ← 请求行：方法 + 路径 + 协议版本
Host: example.com                         ← 目标主机（HTTP/1.1 必带）
User-Agent: Mozilla/5.0 (Windows NT 10.0) ← 客户端标识
Accept: application/json                  ← 期望返回格式
Accept-Encoding: gzip, deflate            ← 支持的压缩（爬虫要注意解压！）
Cookie: session_id=abc123                 ← 会话凭证（登录态核心）
Referer: https://example.com/list         ← 来源页（部分站点校验）
Connection: keep-alive                    ← 连接复用
```

```python
# 爬虫视角的等价代码（阶段 2 会完整展开）
import requests

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "Accept": "application/json",
}
resp = requests.get(
    "https://example.com/api/articles",
    params={"page": 1},          # 拼成 query string
    headers=headers,
    timeout=10,
)
```

| 请求要素 | 爬虫注意点 |
|---------|-----------|
| 方法 | GET（取数据）/ POST（提交/取需参数的数据） |
| 路径+query | 翻页/筛选常藏在 query 里 |
| UA | **反爬第一道校验**（默认 requests 的 UA 一眼假） |
| Cookie | 登录态/会话标识（见 §6） |
| Referer | 部分站点校验来源（防盗链） |
| Accept-Encoding | 服务器可能返回 gzip——requests 自动解压，原生请求要手动 |

## 3. 响应报文：爬虫要会"听"

```http
HTTP/1.1 200 OK                            ← 状态行：版本 + 状态码 + 短语
Content-Type: text/html; charset=utf-8     ← 内容类型（HTML? JSON? 编码?）
Content-Length: 4523
Set-Cookie: session_id=abc123; Path=/      ← 服务器要求你存下这个 Cookie！
Content-Encoding: gzip                     ← body 是压缩的

<!DOCTYPE html><html>...                    ← body（HTML/JSON/图片/文件）
```

```python
# 响应对象三件套（爬虫永远先看这三个）
resp.status_code        # 状态码：200? 403? 404?
resp.headers            # 响应头：Content-Type / Set-Cookie / Location
resp.text               # 文本内容（HTML/JSON）；二进制用 resp.content

# 判定格式的规范姿势：
if "json" in resp.headers.get("Content-Type", ""):
    data = resp.json()          # 接口数据
else:
    html = resp.text            # 页面 HTML
```

> ⚠️ 新手三连坑：① **没看状态码就解析内容**（403 的"内容"是一堆乱码/提示页）；② **`resp.text` vs `resp.content` 混用**（text 自动解码、content 是原始字节）；③ **编码判错**（`resp.encoding` 不对 → 乱码，见 [05](05-数据格式与编码.md)）。

## 4. 状态码：爬虫的判定字典

| 状态码 | 含义 | 爬虫处理 |
|:---:|------|---------|
| **200** | 成功 | 正常解析 |
| 301/302 | 重定向 | requests 自动跟随；检查 `resp.url` 是否变了 |
| 304 | 未修改（缓存） | 有条件请求（If-Modified-Since）时出现 |
| **400** | 请求格式错 | 检查参数/编码 |
| **401** | 未认证 | 缺登录态/Cookie |
| **403** | 拒绝访问 | **最常见**——UA 被拦/风控/IP 被封 |
| **404** | 不存在 | 路径错/资源下线 |
| 405 | 方法不允许 | 该用 POST 用了 GET（或反之） |
| **429** | 请求太频繁 | **限流**——必须降频+等待 |
| 500 | 服务器错 | 服务器问题，重试或跳过 |
| 502/503/504 | 网关/过载 | 服务器忙，延时重试 |

```python
# 状态码处理范式
if resp.status_code == 200:
    parse(resp)
elif resp.status_code == 403:
    handle_blocked()        # 换 UA/代理（阶段 4）
elif resp.status_code == 429:
    sleep(60)               # 降频等待
else:
    log_and_skip(resp)
```

> 🎯 面试/实战要点：**403 与 429 是爬虫常态**——403 = 身份校验没过（UA/Cookie/风控），429 = 频率超限（要礼貌）。见到这两个码别慌，是信息不是事故（对应[阶段 4：反爬基础](../阶段%204：反爬基础（高频遇到）/00-阶段4反爬基础总览.md)）。

## 5. Headers：服务器识别你是谁

| Header | 服务器用它判断 | 爬虫策略 |
|--------|--------------|---------|
| **User-Agent** | 浏览器类型/版本（**第一校验点**） | 伪装成真实浏览器 UA |
| **Cookie** | 会话/登录态（第二校验点） | 登录后携带会话 Cookie |
| Referer | 来源页（防盗链/校验） | 按业务补上来源 |
| Accept-Language | 地区/语言偏好 | 中文站补 `zh-CN,zh;q=0.9` |
| X-Requested-With | 是否 AJAX 请求 | 接口场景常需 `XMLHttpRequest` |
| Origin | 跨域来源（接口校验） | 按接口要求补 |

```python
# 标准浏览器 UA 模板（2026 常用，组合版本号即可）
headers = {
    "User-Agent": ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                   "AppleWebKit/537.36 (KHTML, like Gecko) "
                   "Chrome/126.0.0.0 Safari/537.36"),
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
}
```

> ⚠️ 反爬思维：**服务器从 Headers 推断"你是不是真人浏览器"**——缺 UA、UA 是库名（python-requests/2.34.0）、Header 顺序怪、Cookie 对不上，都是降级信号。前置基础阶段先学会"伪装得像"；深层对抗（指纹等）在阶段 4。

## 6. Cookie 与 Session：登录态的秘密

```text
Cookie 生命周期（爬虫必须理解）：
  第一次访问 → 服务器 Set-Cookie → 客户端保存
  后续访问   → 请求头带 Cookie → 服务器认出"还是你"
  登录成功   → 服务器下发登录凭证 Cookie（session_id/token）

爬虫的三种会话策略：
  ① 无登录态：直接请求公开页面（最简单）
  ② 手动带 Cookie：浏览器登录后复制 Cookie 到代码（快，易过期）
  ③ 自动登录：代码模拟登录流程，自动保存 Cookie（正规，复杂）
```

```python
# requests 的 Session 对象（自动保存 Cookie，模拟浏览器会话）
import requests

session = requests.Session()                  # 会话对象：自动管理 Cookie
session.headers.update({"User-Agent": "Mozilla/5.0 ..."})

resp = session.get("https://example.com")     # 服务器 Set-Cookie 自动保存
resp2 = session.get("https://example.com/me") # 自动带上 Cookie
# 登录后：session.post("/login", data=...) → 后续请求自带登录态
```

> 🎯 核心认知：**Session = 自动管 Cookie 的浏览器模拟器**。爬虫登录态全部基于 Cookie 机制——这也是为什么 [阶段 2](../阶段%202：静态网页爬虫（入门）/00-阶段2静态网页爬虫总览.md) 的第一课就是 Session 对象。（理论细节见 [Cookie 体系](../../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/Cookie%20%26%20Session（会话技术）/00-会话技术总览.md)）

## 7. HTTPS 与证书

| 事项 | 说明 |
|------|------|
| HTTPS 是什么 | HTTP + TLS 加密（传输不被窃听/篡改） |
| 证书校验 | requests 默认验证证书（`verify=True`） |
| 自签证书站点 | **把 CA 加入信任库**（`verify=证书路径`），绝不跳过验证 |
| 证书错误 | `requests.exceptions.SSLError`——站点证书过期/不被信任 |

```python
# ✅ 自签证书的正确姿势：把 CA 加入信任库，而不是跳过验证
# ① 下载自签站的 CA 证书（ca.crt）
# ② 验证时指定信任库：
resp = requests.get(url, verify="/path/to/ca.crt", timeout=10)
# 或全局：REQUESTS_CA_BUNDLE=/path/to/ca.crt

# ❌ 反模式（仅限一次性内网排障，公网环境会沦为中间人攻击目标）
import urllib3
urllib3.disable_warnings()
resp = requests.get(url, verify=False, timeout=10)   # 跳过证书验证
```

> ⚠️ **`verify=False` 是安全红线**：等于告诉爬虫"不验证对方身份"——公网环境会暴露在中间人攻击下（流量可被窃听/篡改）。自签证书环境的正规解法是**把 CA 加入信任库**（`verify=证书路径`）；生产必须真证书 + 完整验证。

## 8. 爬虫的 HTTP 基本功清单

```text
自查清单（全部打勾再进阶段 2）：
  □ 能说清一次请求的五个要素（方法/路径/UA/Cookie/参数）
  □ 拿到响应先看 status_code 再决定怎么处理
  □ 能区分 resp.text（解码文本）与 resp.content（原始字节）
  □ 知道 403（身份）与 429（频率）的应对方向
  □ 会用 Session 自动带 Cookie
  □ 会从 F12 复制 curl 并翻译成 requests 代码（见 07）
  □ 知道 verify=False 为什么危险
```

> 💡 关联深挖：完整 HTTP 语义（状态码全表、头字段体系、缓存）见 [Web 高阶知识-HTTP 协议](../../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/Web%20高阶知识/01-HTTP协议深入.md)；本阶段只讲爬虫用得到的子集。

---

**下一模块**：[03-网页三件套与DOM](03-网页三件套与DOM.md) / **返回总览**：[00-阶段1前置基础总览](00-阶段1前置基础总览.md)
