# UA 与 Headers 伪装
> UA 池、完整浏览器头、Referer/Origin、Cookie 一致性——"让请求看起来像浏览器"的第一课

## 📚 目录
1. [为什么 UA 是第一道校验](#1-为什么-ua-是第一道校验)
2. [UA 池：随机切换](#2-ua-池随机切换)
3. [完整浏览器头：不是只有 UA](#3-完整浏览器头不是只有-ua)
4. [Referer 与 Origin：来源校验](#4-referer-与-origin来源校验)
5. [Cookie 一致性](#5-cookie-一致性)
6. [伪装工程规范](#6-伪装工程规范)

## 1. 为什么 UA 是第一道校验

```text
服务器看 User-Agent 判断"你是谁"：
  浏览器：Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ...
  爬虫库：python-requests/2.34.0（一眼假！）

为什么 UA 是反爬第一关：
  · 零成本检测（读一个头）
  · 拦截效果显著（大量爬虫不伪装）
  · 是后续风控的基础信号

伪装后：
  服务器看到浏览器 UA → 通过第一关
  （UA 只是第一关，不是全部——本阶段后面几章是其他关）
```

> 🎯 认知：**"UA 伪装是入场券，不是通行证"**——能过第一关，但只有 UA 没有其他头配套（Accept/Referer/Cookie），照样被识别（§3 讲成套伪装）。

## 2. UA 池：随机切换

```python
import random

# ═══ UA 池（生产级：按平台/浏览器组合）═══
UA_POOL = [
    # Chrome（Windows）
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    # Edge（Windows）
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0",
    # Firefox
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) "
    "Gecko/20100101 Firefox/127.0",
    # Safari（macOS）
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
    # Chrome（macOS）
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
]

def random_ua() -> str:
    return random.choice(UA_POOL)

# ═══ 使用：每个请求随机 UA ═══
session = requests.Session()
session.headers["User-Agent"] = random_ua()     # 会话级（简单）
# 或请求级（更分散）：
# resp = requests.get(url, headers={"User-Agent": random_ua()}, timeout=10)
```

| UA 池设计 | 说明 |
|---------|------|
| 平台混合 | Windows/macOS（别全是 Windows） |
| 版本分散 | 多个浏览器版本（别全最新） |
| 随机切换 | 会话级 or 请求级（分散度取舍） |
| 真实性 | 从真实浏览器 F12 复制（别编造） |

> ⚠️ UA 池注意：**① 会话内固定 UA**（一个会话忽变 UA 反而可疑）；**② 别用"爬虫专用 UA"**（如自定义 `MyCrawler/1.0`——礼貌场景除外，阶段 1 §8 的公开数据采集用真实标识更合规）；**③ UA 要与系统匹配**（Windows UA 配 Accept-Language: zh-CN 才自然）。

## 3. 完整浏览器头：不是只有 UA

```python
# ═══ 完整浏览器头模板（Copy as cURL 的标准形态）═══
BROWSER_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                  "AppleWebKit/537.36 (KHTML, like Gecko) "
                  "Chrome/126.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,"
              "image/avif,image/webp,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    "Accept-Encoding": "gzip, deflate, br, zstd",   # requests 自动处理解压
    "Connection": "keep-alive",
    "Upgrade-Insecure-Requests": "1",
    "Sec-Fetch-Dest": "document",        # 浏览器安全头（Fetch Metadata）
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "same-origin",
    "Sec-Fetch-User": "?1",
}

# ═══ 使用：成套伪装 ═══
resp = requests.get(url, headers=BROWSER_HEADERS, timeout=10)
```

| 头 | 作用 | 缺失后果 |
|----|------|---------|
| UA | 浏览器标识 | 第一关就挂 |
| Accept | 期望内容类型 | 可能返回降级内容 |
| Accept-Language | 语言偏好 | 中文站可能返回英文/被识别 |
| **Sec-Fetch-\*** | 浏览器 Fetch Metadata | 缺失 = 非浏览器信号（2024+ 风控常用） |
| Referer | 来源页 | 防盗链站点直接拒绝 |

> 🎯 成套认知：**"Headers 是一套'衣服'，不是单件"**——只有 UA 没有 Sec-Fetch/Referer，风控看"穿得不全"照样识别。最稳的做法：**F12 Copy as cURL 复制完整头**，别手搓。

## 4. Referer 与 Origin：来源校验

```python
# ═══ Referer：来源页校验（防盗链核心）═══
# 图片/视频站常见：非本站来源拒绝
headers = {
    "Referer": "https://example.com/list",     # 从列表页访问详情
    "Origin": "https://example.com",            # 跨域请求来源
}

# 场景：
#   图片防盗链：img 请求需带来源页 Referer
#   接口校验：某些接口校验 Referer 必须来自本站
#   CSRF 防护：POST 接口校验 Origin（阶段 5 也相关）

# ═══ 获取正确 Referer 的方法 ═══
# ① F12 Network → 看真实请求的 Referer
# ② 页面 URL 即 Referer（详情页 = 列表页 URL）
referer = "https://example.com/news?page=1"
session.headers["Referer"] = referer
```

| 场景 | Referer 设置 |
|------|-------------|
| 图片/资源 | 来源页面 URL |
| 详情页 | 列表页 URL |
| API 接口 | 页面 URL 或空（看 F12） |
| 跨域请求 | Origin 一并带上 |

> ⚠️ Referer 注意：**别乱设**——有些站点校验"Referer 必须是自己域"，设成别的域反而被拒；**以 F12 实测为准**（Copy as cURL 里有什么就带什么）。

## 5. Cookie 一致性

```text
Cookie 是"会话的身份证"：
  · 无 Cookie 的"新人" → 可疑（正常人都有会话）
  · Cookie 与 UA/IP 不一致 → 可疑（会话拼接）
  · 会话异常活跃 → 风控升级

一致性要求（风控视角）：
  UA ↔ Cookie ↔ IP 尽量稳定对应
  一个会话别同时干"海量请求 + 快速换 IP"
```

```python
# ═══ 正确姿势：会话内保持一致性 ═══
session = requests.Session()
session.headers.update(BROWSER_HEADERS)       # 固定 UA
# 首次访问 → 服务器下发 Cookie → Session 自动保存
session.get("https://example.com", timeout=10)
# 后续请求自动带同一会话 Cookie（一致）
resp = session.get("https://example.com/list?page=2", timeout=10)

# ═══ 反模式：每次新建 Session 且无 Cookie ═══
# for page in range(100):
#     requests.get(url)     # ❌ 每次都是"新人"无会话 —— 风控信号
```

> 🎯 一致性铁律：**"一个会话（Session）干完一件事"**——登录、浏览、采集用同一个 Session（Cookie 连续）；别每个请求都裸请求（无 Cookie 的"幽灵访客"是明显机器信号）。

## 6. 伪装工程规范

```text
伪装自查清单：
  □ UA 从真实浏览器复制（不编造）
  □ Headers 成套（UA + Accept + Language + Sec-Fetch）
  □ Referer 以 F12 实测为准
  □ 会话内 UA/Cookie 保持一致
  □ 礼貌节奏配合（伪装 ≠ 可以猛爬，见 03）
  □ 合规前提（公开数据/尊重 robots，见 07）

伪装的目标是"像正常用户"，不是"伪装成别人"：
  · 像正常用户 → 通过校验（正确目的）
  · 伪装成特定人 → 法律风险（错误目的）
```

> 🎯 本章小结：**"伪装 = 让请求'成套且一致'地像浏览器"**——UA 池 + 完整头 + Referer + 会话一致性，第一层反爬基本通关。下一关是"频率"（[03-频率控制与限流应对](03-频率控制与限流应对.md)）——伪装得再像，爬太猛照样被限。

---

**下一模块**：[03-频率控制与限流应对](03-频率控制与限流应对.md) / **返回总览**：[00-阶段4反爬基础总览](00-阶段4反爬基础总览.md)
