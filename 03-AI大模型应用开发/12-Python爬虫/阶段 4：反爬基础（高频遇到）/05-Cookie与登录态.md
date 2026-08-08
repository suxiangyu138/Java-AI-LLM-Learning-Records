# Cookie 与登录态
> 登录流程分析、Session 保持、验证码基础、手动 Cookie 方案——"登录后才能看的数据"怎么合规地拿

## 📚 目录
1. [登录态：为什么需要](#1-登录态为什么需要)
2. [登录流程分析（F12 视角）](#2-登录流程分析f12-视角)
3. [三种登录方案对比](#3-三种登录方案对比)
4. [方案一：手动 Cookie（快速）](#4-方案一手动-cookie快速)
5. [方案二：代码登录（自动）](#5-方案二代码登录自动)
6. [方案三：验证码辅助](#6-方案三验证码辅助)
7. [登录态维护与合规](#7-登录态维护与合规)

## 1. 登录态：为什么需要

```text
哪些数据"登录后才能看"：
  ① 个人信息页（我的订单/收藏）
  ② 会员专属内容（部分文章/视频）
  ③ 高频功能（评论/关注需要身份）
  ④ 反爬升级的站点（登录才给数据）

登录态的机制（阶段 1 §6 已学）：
  登录成功 → 服务器下发会话 Cookie（session/token）
  → 后续请求带 Cookie → 服务器认出"已登录"

爬虫的三种登录策略：
  手动 Cookie / 代码登录 / 验证码辅助（本文件三节）
```

> 🎯 合规提醒（先立边界）：**"登录后才能看的数据 ≠ 登录后就能爬"**——绕过登录态获取数据可能构成"突破技术措施"（阶段 1 §8 红线）；**个人账号登录爬取公开范围数据**用于学习可接受，批量/商用必须谨慎（07 章细化）。

## 2. 登录流程分析（F12 视角）

```text
登录的完整链路（用 F12 分析任何站点）：
  ① 打开登录页 → F12 Network → 勾 Preserve log
  ② 输入账号密码 → 点击登录 → 观察请求
  ③ 找到登录接口（POST /api/login 之类）
  ④ 记录请求细节：
     · URL + 方法
     · Payload（username/password/captcha...）
     · Headers（Content-Type/Cookie/签名）
  ⑤ 看响应：成功 → Set-Cookie（会话凭证）
  ⑥ 登录后请求：验证带 Cookie 能访问受保护页
```

```python
# F12 分析结果示例（登录接口画像）
# POST https://example.com/api/login
# JSON body: {"username": "...", "password": "...", "captcha": "..."}
# 成功响应头: Set-Cookie: session_id=abc123; Path=/; HttpOnly
# 之后请求: GET /me → 带 Cookie: session_id=abc123 → 200 登录态
```

> 🎯 登录分析的产出 = **登录接口调用说明书**（URL/参数/响应/凭证）——有了它，代码登录就是"把说明书翻译成 requests"（与阶段 1 §7 的接口定位五步法同款）。

## 3. 三种登录方案对比

| 方案 | 原理 | 优点 | 缺点 | 适用 |
|------|------|------|------|------|
| **手动 Cookie** | 浏览器登录后复制 Cookie | 最快、零代码 | 易过期、需手动更新 | 快速验证/低频 |
| **代码登录** | 模拟登录接口 | 自动、可维护 | 需分析接口/处理验证码 | 生产首选 |
| **验证码辅助** | 打码平台/手动输入 | 突破验证码 | 成本/延迟 | 验证码站点 |

> 🎯 选型：**"先手动 Cookie 验证可行性，再代码登录自动化"**——两步走避免"登录接口还没分析清楚就写代码"的返工。

## 4. 方案一：手动 Cookie（快速）

```python
import requests

# ═══ 步骤：浏览器登录 → F12 Application → Cookies → 复制 Cookie 串 ═══
COOKIE_STR = "session_id=abc123; csrf_token=xyz789; _ga=GA1.2.xxx"

session = requests.Session()
session.headers.update({
    "User-Agent": "Mozilla/5.0 ...",
    "Cookie": COOKIE_STR,          # 手动带上登录态
})
# 或更规范：cookies 参数
# session.cookies.update(dict(c.split("=", 1) for c in COOKIE_STR.split("; ")))

# 访问受保护页
resp = session.get("https://example.com/me", timeout=10)
print(resp.status_code)            # 200 = 登录态生效
```

| 手动 Cookie 注意 | 说明 |
|-----------------|------|
| 过期 | 会话 Cookie 关闭浏览器/一段时间后失效 |
| 更新 | 过期后重新复制（写进配置，别硬编码） |
| 安全 | Cookie 是敏感信息（别提交到代码仓库） |
| 定位 | 快速验证/低频任务（生产用方案二） |

> ⚠️ 手动 Cookie 的两个坑：**① 会过期**（长任务跑一半失效 → 需要"Cookie 过期自动告警"）；**② 别硬编码进源码**（进环境变量/配置文件，阶段 6 工程化）。

## 5. 方案二：代码登录（自动）

```python
import requests

# ═══ 代码登录三步 ═══
session = requests.Session()
session.headers.update({
    "User-Agent": "Mozilla/5.0 ...",
    "Referer": "https://example.com/login",
})

# ① 获取登录页（拿初始 Cookie/CSRF token，常见前置）
login_page = session.get("https://example.com/login", timeout=10)
# 有些站点登录接口需要页面里的 csrf_token：
# token = re.search(r'name="csrf_token" value="([^"]+)"', login_page.text).group(1)

# ② 提交登录（按 F12 分析的结果构造）
resp = session.post(
    "https://example.com/api/login",
    json={
        "username": "your_account",
        "password": "your_password",
        # "csrf_token": token,      # 如有
    },
    timeout=10,
)
print("登录响应:", resp.status_code, resp.json() if "json" in resp.headers.get("Content-Type","") else "")

# ③ 验证登录态（访问受保护页）
me = session.get("https://example.com/me", timeout=10)
print("登录态验证:", me.status_code, "个人页" in me.text)

# 之后所有 session 请求自动带登录 Cookie
data = session.get("https://example.com/orders", timeout=10)
```

| 代码登录要点 | 说明 |
|------------|------|
| 前置请求 | 登录页拿初始 Cookie/CSRF token（常见） |
| 参数分析 | 以 F12 实测为准（用户名/密码/验证码/签名） |
| 验证 | 登录后访问受保护页确认（status 200） |
| 凭证管理 | 账号密码进配置（环境变量） |
| 失败处理 | 登录失败检查参数/验证码/风控 |

> 🎯 面试/实战点：**"代码登录 = 复刻登录接口调用 + Session 保持 + 登录态验证"**——三步缺一不可；登录失败时先看"响应内容"（错误信息在 body 里），别只看状态码。

## 6. 方案三：验证码辅助

```text
验证码的三种应对（从轻到重）：
  ① 图形验证码（4-6 位字母数字）
     · 手动方案：下载图片 → 人工识别 → 提交（学习/低频）
     · 自动方案：OCR（tesseract/ddddocr）识别（简单码可行）
  ② 滑块/点选验证码（行为验证）
     · 需模拟人类轨迹（复杂，阶段 7）
     · 高频场景用打码平台（付费 API）
  ③ 短信/邮箱验证码
     · 需接收验证码（自动化复杂，通常人工介入）
```

```python
# ═══ 图形验证码：下载 → 人工/OCR → 提交（学习版）═══
import requests

session = requests.Session()
session.get("https://example.com/login", timeout=10)

# ① 下载验证码图片（一般有独立接口）
img_resp = session.get("https://example.com/captcha", timeout=10)
with open("captcha.png", "wb") as f:
    f.write(img_resp.content)

# ② 人工识别（终端显示图片/打开查看）
captcha = input("请输入验证码: ")          # 学习场景：人工输入

# ③ 提交登录
resp = session.post("https://example.com/api/login",
                    json={"username": "x", "password": "y", "captcha": captcha},
                    timeout=10)
```

| 验证码应对 | 场景 | 复杂度 |
|-----------|------|:---:|
| 人工识别（input） | 学习/低频 | 低 |
| OCR（ddddocr） | 简单图形码 | 中 |
| 打码平台 | 生产高频 | 中（付费） |
| 滑块轨迹模拟 | 行为验证码 | 高（阶段 7） |

> ⚠️ 验证码认知：**"验证码是'人机分界'的技术实现"**——高频绕过验证码 = 对抗"技术措施"，合规风险高（阶段 1 §8 红线）；学习可以理解原理，生产要评估"该不该绕"（07 章）。

## 7. 登录态维护与合规

```text
登录态维护（长时间任务）：
  ① 会话过期检测：请求返回登录页特征 → 触发重新登录
  ② 自动重登：捕获失效 → 重新代码登录 → 继续任务
  ③ 凭证轮换：多账号分散（别单账号扛所有量）
  ④ 会话保持：定期"心跳"请求防过期（部分站点）

合规边界（本阶段明确）：
  ✅ 自己账号登录 + 访问自己的数据范围
  ✅ 登录公开内容（与未登录一致的公开数据）
  ⚠️ 批量采集他人隐私/会员专属 → 停（07 章）
  ❌ 多账号绕过限流/突破付费 → 明确禁止
```

```python
# ═══ 会话过期自动重登（工程模式）═══
def ensure_login(session, credentials):
    """检测登录态，失效则重登"""
    test = session.get("https://example.com/me", timeout=10)
    if test.status_code == 200 and "登录" not in test.text[:200]:
        return True                        # 登录态有效
    print("会话失效，重新登录...")
    return do_login(session, credentials)  # 重新走登录流程
```

> 🎯 本章小结：**"登录态 = 手动 Cookie（快）/ 代码登录（自动）/ 验证码（辅助）三选一 + 过期自动重登"**——同时记住边界：**登录 ≠ 授权爬取**。下一关是"指纹"（[06-请求指纹与风控信号](06-请求指纹与风控信号.md)）——为什么"装得很像"还是被识别。

---

**下一模块**：[06-请求指纹与风控信号](06-请求指纹与风控信号.md) / **返回总览**：[00-阶段4反爬基础总览](00-阶段4反爬基础总览.md)
