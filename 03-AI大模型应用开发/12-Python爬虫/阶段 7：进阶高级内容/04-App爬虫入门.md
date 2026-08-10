# 04 - App 爬虫入门

> 本阶段第四课：第二战场——抓包、证书、接口分析、逆向入门——"网页是地表，App 是地下——很多数据只在 App 里——App 爬虫 = 抓包 + 逆向，2026 年爬虫工程师的必修形态"

---

## 📚 目录

1. [App 爬虫的定位](#1-app-爬虫的定位)
2. [抓包原理与 mitmproxy](#2-抓包原理与-mitmproxy)
3. [HTTPS 证书配置](#3-https-证书配置)
4. [App 接口分析](#4-app-接口分析)
5. [逆向入门：frida](#5-逆向入门frida)
6. [合规边界](#6-合规边界)
7. [练习 5 题](#7-练习-5-题)
8. [本节验收](#8-本节验收)

---

## 1. App 爬虫的定位

**App 爬虫 = 从手机 App 获取数据**——为什么需要（网页爬不到的）：

```text
App 数据的独特性
├── 网页没有：独家数据只在 App（社区/电商/生活服务的高频数据）
├── 网页被墙：接口有反爬，App 端可能宽松（或相反）
├── 移动端专属：LBS 数据（位置相关——附近/实时数据）
└── 接口直取：App 的每个界面背后都是接口——"App 是接口的'壳'"
    ——"App 爬虫的核心：找到壳背后的接口——数据都在接口里"
```

**定位心智**：**"App 爬虫的第一原则：'抓包找接口'——不是逆向整个 App，是找到数据接口直接调"**——**"App 界面上你看到的每个数据，背后都是一个 HTTP 接口——抓包 = 把接口'翻译'出来——'App 逆向是手段，接口是目的'"**（阶段 5 01 篇"接口直取最优"的 App 版）；**App 爬虫的难度梯度**——"① 无签名接口（直接调）→ ② 有签名/加密接口（要逆向参数）→ ③ 强混淆 + 反调试（高难）——**'先抓包看看——80% 的 App 数据不需要深逆向'"**；**工具链**（2026）：**mitmproxy（抓包——本课主角）**、Charles（图形化抓包）、frida（逆向 Hook）、Appium/ADB（自动化操作）——"抓包是入门，逆向是进阶"。

## 2. 抓包原理与 mitmproxy

**抓包 = 中间人代理——让手机流量经过你的电脑**：

```text
mitmproxy 抓包原理
┌─────────┐   HTTPS   ┌──────────┐   HTTPS   ┌─────────┐
│  手机 App  │ ←──────→ │ mitmproxy │ ←──────→ │  服务器   │
└─────────┘   (代理)   └──────────┘   (代理)   └─────────┘
手机信任 mitmproxy 的证书 → mitmproxy 解密 HTTPS → 看到明文请求
    ——"抓包的本质：'中间人'——App 以为在和服务器说话，其实先经过了你的代理"
```

```bash
# mitmproxy 三步（Windows/macOS/Linux 通用）
# ① 电脑上启动代理
mitmproxy --listen-port 8080          # 或 mitmdump（无界面模式——脚本用）

# ② 手机设置代理：WiFi → 手动代理 → 电脑 IP:8080

# ③ 安装证书：手机浏览器访问 http://mitm.it → 下载安装 mitmproxy 证书

# ④ Python 脚本处理流量（mitmdump 插件——自动记录接口）
# mitmdump -s capture.py
```

```python
# capture.py——mitmproxy 插件：自动记录 App 接口
from mitmproxy import http

def request(flow: http.HTTPFlow):
    """每个请求经过时调用——记录接口 URL 与参数"""
    url = flow.request.pretty_url
    if "api." in url:                        # 只关心 API 接口
        print(f"[接口] {flow.request.method} {url}")
        print(f"[参数] {flow.request.query}")

def response(flow: http.HTTPFlow):
    """每个响应经过时调用——记录返回数据"""
    if "api." in flow.request.pretty_url:
        body = flow.response.text[:500]      # 看返回的 JSON 结构
        print(f"[返回] {body}")
```

**抓包心智**：**"抓包的目标：'接口清单'——URL/方法/参数/返回结构——抓到就能用 requests 复刻"**——**"mitmproxy 插件的价值：自动记录（不用一个个点界面）——'跑一遍 App 常用页面，接口清单自动生成'"**（阶段 5 01 篇"复刻请求"的 App 版——"F12 是网页的抓包，mitmproxy 是 App 的 F12"）；**抓包失败的三个检查**——"代理设置对了吗（WiFi 代理）、证书装了吗（HTTPS 拦截需要）、App 有证书校验吗（防抓包——本课第 5 节逆向绕）——**'抓包看不到流量，先查三件事'"**。

## 3. HTTPS 证书配置

**HTTPS 抓包 = 证书信任的建立**（抓包的核心技术细节）：

```text
证书配置原理
├── 正常 HTTPS：App 验证服务器的证书（CA 链）
├── 抓包 HTTPS：mitmproxy 伪造证书（自己签的）——App 默认不信任
├── 解法：把 mitmproxy 的 CA 证书装进手机"信任列表"
│    Android：设置 → 安全 → 安装证书（CA 证书——Android 7+ 需"用户证书"）
│    iOS：设置 → 描述文件 → 安装 → 证书信任设置 → 开启完全信任
└── 障碍：App 的"证书校验"（SSL Pinning）——只认特定证书——抓包失效
    ——"证书配置 = 让 App 信任你的代理——App 不让 = SSL Pinning（要逆向绕）"
```

**证书心智**：**"证书配置是抓包的第一道门槛——配不好，HTTPS 流量全是乱码（TLS 握手失败）"**——**"Android 7+ 的'用户证书'限制 + iOS 的'完全信任'开关——两个系统的证书坑要记住"**（"证书装错地方 = App 能上网但抓不到包——最常见的抓包困境"）；**SSL Pinning（证书固定）**——"App 内置特定证书指纹——代理的证书不匹配 → 拒绝连接——**'Pinning 是 App 防抓包的第一道防线——遇到它，抓包从'配置'升级到'逆向'（本课第 5 节）'"**；**抓包的替代方案**——"有些 App 允许'明文流量'（开发模式）或走 HTTP——**'先试无加密的，再攻坚 HTTPS'"**（"抓包的顺序：越简单越好"）。

## 4. App 接口分析

**抓到接口后：分析 → 复刻 → 数据落地**：

```python
# 抓到的接口示例（分析三要素）
# GET https://api.example.com/v2/feed?page=1&size=20&sign=xxxx
# ① 接口结构：/v2/feed（版本 + 资源）——page/size 分页参数
# ② 参数：page=1（分页——翻页循环）——sign=xxxx（签名——可能要逆向）
# ③ 返回：JSON（json_normalize 直接转 DataFrame——数据分析阶段 3 05 篇）

import requests

# 复刻请求（无签名接口——直接调）
def fetch_feed(page):
    resp = requests.get("https://api.example.com/v2/feed",
                        params={"page": page, "size": 20},
                        headers={"User-Agent": "App/2.3.1 (Android 14)"},   # App 的 UA！
                        timeout=(3, 15))
    resp.raise_for_status()
    return resp.json()["data"]

for page in range(1, 11):
    items = fetch_feed(page)
    if not items: break
    all_items.extend(items)
    time.sleep(1.5)                               # 限速纪律（阶段 4）
```

**接口分析心智**：**"接口复刻的三要素：UA 像 App（服务器认 App 的 UA）、参数对齐（分页/时间戳）、Header 补全（Referer/Device-Id——App 的请求头）"**——**"复刻的验证：返回数据是否和 App 一致——'一致 = 复刻成功'"**（阶段 5 02 篇"直取 vs 渲染"的 App 版）；**分页三模式**（数据分析阶段 3 05 篇通用）——"page/offset/cursor——App 常用 cursor（游标——'下一页的钥匙'）——**'先看接口参数认模式，再写循环'"**；**签名参数（sign/token）**——"sign 是接口的'防伪码'（参数+密钥哈希）——**'有 sign 的接口 = 逆向门槛——先评估值不值得绕（01 篇止损线）'"**（"很多 App 的 sign 算法可以搜到/逆向简单——但评估价值优先"）。

## 5. 逆向入门：frida

**frida = 动态注入工具——运行时 Hook App 的函数（绕 Pinning/看签名算法）**：

```text
frida 是什么
├── 往运行中的 App 注入 JS 代码（不需要重打包 App）
├── Hook 能力：拦截函数调用（看参数/改返回值/调任意函数）
├── 典型用途：绕 SSL Pinning（Hook 证书校验函数——让它永远"通过"）
│            看签名算法（Hook 签名函数——打印参数看密钥怎么生成）
└── 配套：需要 root/越狱手机 或 模拟器（frida 生态 2026 成熟）
    ——"frida = 给运行中的 App 打'调试器'——不碰代码，看它怎么算"
```

```python
# frida 绕 SSL Pinning（经典示例——Android）
# frida -U -f com.example.app -l bypass_pinning.js

# bypass_pinning.js（核心 3 行）
Java.perform(function () {
    var SSLContext = Java.use("javax.net.ssl.SSLContext");
    SSLContext.init.overload(
        "[Ljavax.net.ssl.KeyManager;", "[Ljavax.net.ssl.TrustManager;",
        "java.security.SecureRandom"
    ).implementation = function (km, tm, sr) {
        // 替换 TrustManager——让所有证书"通过验证"
        this.init(km, [trustAllManager], sr);
    };
});
```

**逆向心智**：**"逆向的定位：'解决抓包解决不了的问题'——Pinning/签名——不是所有 App 都要逆向"**——**"逆向学习的正确顺序：先抓包（80% 场景够用）→ 再 frida 绕 Pinning（20%）→ 最后才深逆向（5%——脱壳/算法还原——高难度）——'逆向是金字塔，大部分人只需要塔基'"**；**逆向的技术栈**——"frida（Hook）+ jadx（静态看代码——反编译 APK）+ objection（frida 封装——一键绕 Pinning）——**'工具链：objection 一键、frida 手工、jadx 看源码'"**；**逆向的难度现实**——"加固/混淆（脱壳）、反调试（检测 frida——App 主动退出）——**'逆向是军备竞赛的高地——评估 ROI：多数场景不值得攻坚'"**（01 篇止损线的最高级应用——"遇到强逆向的 App：换方案（网页/第三方/买数据）比攻坚划算"）。

## 6. 合规边界

**App 爬虫的合规特殊性——比网页爬虫更敏感**：

```text
App 爬虫合规三问
├── ① 授权：App 的用户协议/开发者条款是否禁止自动化访问？
│    （很多 App 条款明令禁止——抓包+接口调用违反条款）
├── ② 隐私：App 数据常含个人信息（用户主页/通讯录）——个保法红线
│    （数据分析阶段 5 方向 C 08 篇的个保法——App 数据尤其敏感）
└── ③ 逆向：绕过 Pinning/脱壳可能触犯"技术措施"保护条款
    （反不正当竞争法/著作权法的技术保护条款）
    ——"App 爬虫 = 网页爬虫的高风险版本——授权评估先行"
```

**合规心智**：**"App 爬虫的三个判断"**——**"① 这个 App 有 API/开放平台吗（有就申请——合规正道）；② 用户协议允许吗（禁止自动化 = 别碰）；③ 数据含个人信息吗（含 = 脱敏+授权+最小化）"**——"**App 数据的合规顺序：开放 API > 用户授权 > 放弃——逆向是最后的选项"**（01 篇"先谈合作再谈爬取"的 App 版——"App 厂商比网站更重视数据主权——被抓的案例更多"）；**测试与学习场景**——"自己开发的 App/测试环境/开源 App——**'学习逆向用无害目标（自己/开源），职业实践走授权通道'"**；**职业底线**——"逆向支付/账号体系等高敏功能 = 明确的职业红线——**'技术展示能力，选择展示底线'"**（01 篇红线五条在 App 场景的落地）。

## 7. 练习 5 题

1. App 爬虫的核心原则？（抓包找接口——不是逆向整个 App）
2. mitmproxy 抓包的原理？（中间人 + 证书信任）
3. 证书配置的三个检查？SSL Pinning 是什么？
4. 接口复刻的三要素？签名参数怎么处理？
5. App 爬虫合规三问？

## 8. 本节验收

**验收动作**：① 用 mitmproxy 抓自己手机上一个 App 的接口（记录 3 个接口的 URL/参数/返回）；② 写 mitmdump 插件自动记录接口清单；③ 复刻一个无签名接口并落地数据（限速纪律）；④ 写 App 爬虫合规评估（三问）——**"抓包 + 证书 + 复刻 + 合规 = App 爬虫入门"**——**练习纪律**：抓包实验用自己/无害目标——"学习的目标是掌握方法，不是攻击他人"。

> 🎯 **核心要点**：App 爬虫 = 第二战场（**数据都在接口里——App 是接口的壳**）；**mitmproxy 抓包（中间人 + 证书信任——插件自动记录接口清单）**；证书配置（**Android 7+/iOS 完全信任的坑——Pinning 是抓包升级逆向的信号**）；接口复刻三要素（**UA 像 App/参数对齐/Header 补全——有 sign 先评估值不值得**）；**frida 逆向（Hook 绕 Pinning/看签名——逆向是金字塔，大部分人只需要塔基——强逆向先评估 ROI）**；**合规三问（开放 API 优先/用户协议/个人信息——逆向是高危选项）**——"抓包找接口是目的，逆向是手段——技术展示能力，选择展示底线"。

---

**上一模块**：[03-验证码与指纹对抗.md](./03-验证码与指纹对抗.md) / **下一模块**：[05-分布式爬虫深化.md](./05-分布式爬虫深化.md)
