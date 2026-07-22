# Python爬虫教程 面试宝典
> 基于Python爬虫课程大纲全面覆盖面试高频考点，涵盖基础请求解析、JS逆向工程与Scrapy框架实战

## 目录
1. [一、基础概念速答（16题）](#一基础概念速答16题)
2. [二、深度原理剖析（10题）](#二深度原理剖析10题)
3. [三、实战场景题（8题）](#三实战场景题8题)
4. [四、手写代码题（6题）](#四手写代码题6题)
5. [五、系统设计题（4题）](#五系统设计题4题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（16题）

> 💡 **面试特点**：基础概念题通常出现在一面，重点考察对HTTP协议、解析库、反爬机制的理解。回答应简洁准确，适当举例。

### 1.1 HTTP请求中 GET 和 POST 的区别

| 对比项 | GET | POST |
|--------|-----|------|
| 参数位置 | URL查询字符串 | 请求体（body） |
| 安全性 | 参数明文暴露在URL | 相对安全（参数在body） |
| 长度限制 | 一般2KB-8KB（浏览器/服务器限制） | 理论无限制 |
| 幂等性 | 幂等（多次请求结果相同） | 非幂等 |
| 缓存 | 可被浏览器缓存 | 不缓存 |
| 爬虫应用 | 简单数据获取 | 表单提交、登录、大参数传输 |

> ⚠️ **注意**：POST 并不真正安全，HTTPS 才是保护数据的核心手段。

### 1.2 requests 库 Session 的作用

```python
import requests

# 不使用Session —— 每次请求独立，无法维持cookies
resp1 = requests.get("https://httpbin.org/cookies/set/name/value")
resp2 = requests.get("https://httpbin.org/cookies")  # 丢失cookie

# 使用Session —— 自动维持cookies、连接池
session = requests.Session()
session.headers.update({"User-Agent": "Mozilla/5.0"})
resp1 = session.get("https://httpbin.org/cookies/set/name/value")
resp2 = session.get("https://httpbin.org/cookies")  # cookie保持
```

关键点：`Session` 自动管理 **cookies**、**连接池**（复用TCP连接提升性能）、**默认headers**。

### 1.3 XPath 常用表达式

| 表达式 | 含义 | 示例 |
|--------|------|------|
| `//` | 任意位置选取 | `//div` 选取所有div |
| `/` | 直接子节点 | `//ul/li` ul下的直接li |
| `[@attr]` | 按属性过滤 | `//div[@class="main"]` |
| `text()` | 获取文本 | `//h1/text()` |
| `@attr` | 获取属性值 | `//img/@src` |
| `contains()` | 包含匹配 | `//div[contains(@class,"item")]` |
| `position()` | 位置筛选 | `//li[position()<3]` |

```python
from lxml import etree

html = "<html><body><div class='item'><span>Hello</span></div></body></html>"
tree = etree.HTML(html)
result = tree.xpath('//div[contains(@class,"item")]/span/text()')
print(result)  # ['Hello']
```

### 1.4 JSONPath 与 JSON 数据解析

```python
import json
from jsonpath import jsonpath

data = json.loads('{"store":{"book":[{"title":"A","price":10},{"title":"B","price":20}]}}')

# $.book[*].title → 提取所有title
titles = jsonpath(data, '$.store.book[*].title')
print(titles)  # ['A', 'B']
```

> 💡 **对比**：JSONPath 类似 XPath for JSON。常见库有 `jsonpath`（第三方）和 Python 原生 `json` 模块。

### 1.5 常见反爬机制与应对策略

| 反爬手段 | 检测方式 | 应对策略 |
|----------|----------|----------|
| User-Agent 检测 | 检查请求头UA | 构建UA池随机切换 |
| IP 频率限制 | 统计单位时间请求数 | IP代理池，请求间隔随机化 |
| Cookie/Session 校验 | 校验登录态或会话标识 | Session维持登录态 |
| Referer 验证 | 检查请求来源 | 添加正确Referer头 |
| 动态参数签名 | 前端加密生成参数 | JS逆向分析加密逻辑 |
| WebDriver 检测 | 检测navigator.webdriver | Selenium stealth / Playwright |
| 验证码 | 图形/滑块/点选 | OCR / 打码平台 / 模拟行为 |

### 1.6 Scrapy 架构核心组件

```
                    +-----------+
                    |  Scheduler|
                    +-----+-----+
                          |
          +---------------+---------------+
          |               |               |
    +-----v-----+   +----v----+   +------v------+
    | Downloader |-->|  Spider |-->| Item Pipeline|
    +-----+------+   +---------+   +------+------+
          |                                |
          v                                v
    Internet(请求)                     存储(DB/CSV)
```

| 组件 | 职责 |
|------|------|
| **Spider** | 定义爬取规则、解析响应、生成Item |
| **Engine** | 协调组件间数据流转 |
| **Scheduler** | 调度请求队列（去重、优先级） |
| **Downloader** | 下载页面，支持中间件 |
| **Item Pipeline** | 清洗、验证、持久化数据 |
| **Middleware** | 请求/响应钩子，处理代理、UA、重试 |

### 1.7 Scrapy 中间件的典型用途

```python
# middlewares.py —— 随机UA中间件示例
class RandomUserAgentMiddleware:
    def process_request(self, request, spider):
        ua = random.choice(spider.settings.get('USER_AGENT_LIST'))
        request.headers['User-Agent'] = ua
        return None

# settings.py 中激活
DOWNLOADER_MIDDLEWARES = {
    'myproject.middlewares.RandomUserAgentMiddleware': 543,
}
```

### 1.8 Selenium 与 Playwright 对比

| 对比维度 | Selenium | Playwright |
|----------|----------|------------|
| 浏览器支持 | Chrome/Firefox/Edge/Safari | Chromium/Firefox/WebKit |
| 速度 | 较慢 | 较快（CDP协议） |
| API 设计 | 多层封装 | 简洁，await异步 |
| 反检测 | 需额外配置stealth | 内置防检测机制 |
| 并发 | 多线程 | 原生异步支持 |
| 安装 | 需对应浏览器驱动 | `pip install playwright` 自动下载 |

```python
# Playwright 示例 —— 获取动态渲染内容
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("https://example.com")
    content = page.content()  # 包含JS渲染后的HTML
    browser.close()
```

### 1.9 数据存储选型

| 存储方式 | 适用场景 | 优缺点 |
|----------|----------|--------|
| CSV | 简单结构数据、小规模 | 读写方便，不支持嵌套 |
| JSON | 半结构化数据 | 与Python dict天然兼容 |
| MySQL (pymysql) | 关系型、事务要求 | 成熟稳定，ORM友好 |
| MongoDB (pymongo) | 非结构化、大规模 | 灵活schemaless，横向扩展 |

```python
# pymongo 写入示例
import pymongo

client = pymongo.MongoClient("mongodb://localhost:27017/")
db = client["scrapy_db"]
collection = db["articles"]
collection.insert_one({"title": "爬虫教程", "url": "https://example.com"})
```

### 1.10 asyncio + aiohttp 异步爬虫

```python
import asyncio
import aiohttp

async def fetch(session, url):
    async with session.get(url) as resp:
        return await resp.text()

async def main():
    urls = ["https://example.com"] * 10
    async with aiohttp.ClientSession() as session:
        tasks = [fetch(session, url) for url in urls]
        results = await asyncio.gather(*tasks)
        print(f"共获取 {len(results)} 个页面")

asyncio.run(main())
```

> 🎯 **性能**：异步爬虫比同步 requests 快 5-10 倍，适合 IO 密集型场景。核心是 `asyncio.gather` 并发执行任务。

### 1.11 robots.txt 与爬虫协议

- **作用**：告知爬虫哪些路径允许/禁止抓取
- **检查方式**：`https://example.com/robots.txt`
- **处理**：`from urllib.robotparser import RobotFileParser` 解析

```python
from urllib.robotparser import RobotFileParser

rp = RobotFileParser()
rp.set_url("https://www.baidu.com/robots.txt")
rp.read()
print(rp.can_fetch("*", "https://www.baidu.com/s?wd=test"))  # False
```

### 1.12 HTTP 状态码在爬虫中的处理

| 状态码 | 含义 | 爬虫处理 |
|--------|------|----------|
| 200 | 成功 | 正常解析 |
| 301/302 | 重定向 | 追踪Location头 |
| 403 | 禁止访问 | 检查UA/IP是否被封 |
| 404 | 不存在 | 跳过或记录日志 |
| 429 | 请求过多 | 等待重试，降低频率 |
| 500/502/503 | 服务端错误 | 指数退避重试 |

### 1.13 验证码识别方案对比

| 方案 | 适用类型 | 准确率 | 成本 |
|------|----------|--------|------|
| OCR（Tesseract） | 简单文字验证码 | 60-80% | 免费 |
| 深度学习（CNN/CRNN） | 复杂文字验证码 | 90%+ | 需训练数据 |
| 打码平台（打码兔/超人） | 各类验证码 | 95%+ | 按次付费 |
| 行为模拟 | 滑块验证码 | 80-90% | 需轨迹算法 |

### 1.14 Scrapy-Redis 分布式原理

```python
# settings.py 配置
SCHEDULER = "scrapy_redis.scheduler.Scheduler"
DUPEFILTER_CLASS = "scrapy_redis.dupefilter.RFPDupeFilter"
REDIS_URL = "redis://localhost:6379"

# 多个爬虫节点共享同一个Redis请求队列 → 天然去重 + 分布式调度
```

> 💡 **核心**：用 Redis 替代本地的 `Scheduler` 和 `DupeFilter`，实现多个爬虫节点共享请求队列与去重集合。

### 1.15 Cookies 管理与登录态维持

```python
# 方式1：requests Session 自动管理
session = requests.Session()
session.post("https://example.com/login", data={"user":"admin","pass":"123"})
resp = session.get("https://example.com/profile")  # 已携带cookie

# 方式2：手动设置cookies
cookies = {"sessionid": "abc123"}
resp = requests.get("https://example.com/profile", cookies=cookies)

# 方式3：从文件加载cookies
import http.cookiejar
jar = http.cookiejar.LWPCookieJar("cookies.txt")
session = requests.Session()
session.cookies = jar
```

### 1.16 爬虫框架选型对比

| 框架 | 适合场景 | 特点 |
|------|----------|------|
| Scrapy | 中大规模、结构化提取 | 组件化，中间件丰富，高性能 |
| requests+BS4 | 小规模、快速原型 | 轻量，学习成本低 |
| Selenium/Playwright | JS渲染、SPA | 真实浏览器，速度慢 |
| pyspider | 多站点管理 | WebUI管理，已停止维护 |
| feapder | 生产级爬虫 | 支持断点续爬、分布式 |

---

## 二、深度原理剖析（10题）

> 💡 **面试特点**：深度题出现在二面/三面，考察对底层机制的理解和问题排查能力。

### 2.1 JS逆向工程完整流程

**场景**：爬取某网站数据，接口返回加密的 `data` 字段。

```
Step 1: 抓包定位 → XHR断点到加密入口
Step 2: 调用栈回溯 → 找到加密函数 enc(data, key)
Step 3: 代码分析 → 识别加密算法 MD5 / RSA / AES / 自定义
Step 4: 扣代码 / 补环境 → 纯JS调用 or Python重写
Step 5: 验证结果 → 加密参数与网页一致
```

```javascript
// 浏览器端JS示例 —— AES加密 + base64
// 通过搜索 "encrypt" 或 "JSON.parse" 在Sources面板中找到
function encryptData(data, secretKey) {
    var key = CryptoJS.enc.Utf8.parse(secretKey);
    var encrypted = CryptoJS.AES.encrypt(data, key, {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
    });
    return encrypted.toString();
}
```

```python
# Python 等价实现
from Crypto.Cipher import AES
import base64

def decrypt_data(encrypted_data, secret_key):
    key = secret_key.encode('utf-8')
    cipher = AES.new(key, AES.MODE_ECB)
    decrypted = cipher.decrypt(base64.b64decode(encrypted_data))
    return decrypted.decode('utf-8').strip()
```

### 2.2 JS逆向常见的加密算法识别

| 特征 | 算法 | 识别线索 |
|------|------|----------|
| 固定长度32位hex | MD5 | `md5()` / `hex_md5()` |
| 固定长度40位hex | SHA1 | `sha1()` / `hex_sha1()` |
| 可变长度 + padding | AES / DES | `enc.Utf8.parse`、`mode.ECB/CBC` |
| 大质数运算 | RSA | `setPublicKey`、`encrypt.enc` |
| 等号结尾的字符串 | Base64 | `btoa()` / `Base64.encode()` |
| URL安全字符串 | Base64 URLSafe | 含 `-` 和 `_` |
| 固定长度64位hex | HMAC-SHA256 | `HmacSHA256` |

### 2.3 webpack 打包 JS 的逆向思路

```javascript
// webpack 模块加载器特征 —— 自执行函数 + 模块数组
!function(e) {
    var t = {};
    function n(r) {
        if (t[r]) return t[r].exports;
        var o = t[r] = {i: r, l: false, exports: {}};
        return e[r].call(o.exports, o, o.exports, n), o.l = true, o.exports
    }
    n(0)  // 入口模块
}([
    function(module, exports, n) { /* 模块0 */ },
    function(module, exports, n) { /* 模块1: 加密模块 */ },
    // ...
])
```

**逆向步骤**：
1. 找到入口调用 `n(0)`
2. 定位加密所在模块号（搜索关键字 `encrypt` / `sign`）
3. 提取该模块依赖的所有子模块
4. 在 Node.js 中构造相同的 `n` 函数加载器
5. 调用目标模块导出函数

```javascript
// Node.js 复现 —— 复制webpack的模块加载器 + 加密模块
const module_list = [
    function(m, e, n) { /* 加密模块代码 */ },
    // ...
];
const modules = {};
function load(id) {
    if (modules[id]) return modules[id].exports;
    var mod = {id, exports: {}};
    modules[id] = mod;
    module_list[id](mod, mod.exports, load);
    return mod.exports;
}
const encrypt = load(1);  // 加载加密模块
console.log(encrypt("test_data"));
```

### 2.4 反调试技术及绕过方法

| 反调试手段 | 原理 | 绕过方法 |
|------------|------|----------|
| `debugger` 无限断点 | 定时执行debugger | Chrome Deactivate breakpoints |
| 检测开发者工具 | `console.log` 调用次数 | 使用CDP协议无头浏览器 |
| 代码混淆 | 变量名/控制流扁平化 | AST解析还原 |
| 内存爆破 | 正常运行内存泄漏 | 补齐缺失环境变量 |
| 时间戳校验 | 请求时间差判定 | 同步时间戳 |

```javascript
// 反调试示例 —— 无限debugger
function anti_debug() {
    function check() {
        debugger;  // 令开发者工具卡住
    }
    setInterval(check, 100);
}
// 绕过: 在Chrome中点击 "Deactivate breakpoints"（小圆圈斜杠图标）或添加条件断点 false
```

### 2.5 Scrapy 的 Request 去重机制

```python
# scrapy/dupefilters.py 核心逻辑
from scrapy.utils.request import request_fingerprint

class RFPDupeFilter(BaseDupeFilter):
    def __init__(self):
        self.fingerprints = set()

    def request_seen(self, request):
        fp = request_fingerprint(request)  # MD5(method + url + body + headers)
        if fp in self.fingerprints:
            return True
        self.fingerprints.add(fp)
        return False
```

使用 `scrapy_redis` 时，`fingerprints` 存储在 Redis Set 中，实现跨节点去重。

### 2.6 浏览器指纹（Fingerprint）检测原理

浏览器指纹是通过收集浏览器特征信息（客户端随机化程度越低，指纹越唯一）生成的唯一标识。

| 采集维度 | 特征值 | 对抗方法 |
|----------|--------|----------|
| User-Agent | 浏览器版本/操作系统 | 随机UA池 |
| WebGL | GPU渲染信息 | 修改canvas指纹 |
| Canvas指纹 | canvas绘图差异 | 添加随机噪声 |
| AudioContext | 音频处理差异 | 固定音频参数 |
| 屏幕分辨率 | 宽高/色深 | 统一分辨率 |
| 时区/语言 | Intl.DateTimeFormat | 固定时区与语言 |
| 字体列表 | CSS font检测 | 限制字体列表 |

> 🎯 **对抗策略**：使用 Playwright 的 `browser_context` 配合固定参数，或使用指纹修改工具如 `puppeteer-extra-plugin-stealth`。

### 2.7 Scrapy 中间件执行顺序与优先级

```python
# settings.py —— 优先级数值越小越靠近引擎
DOWNLOADER_MIDDLEWARES = {
    'scrapy.downloadermiddlewares.robotstxt.RobotsTxtMiddleware': 100,
    'scrapy.downloadermiddlewares.httpproxy.HttpProxyMiddleware': 750,
    'myproject.middlewares.ProxyMiddleware': 543,  # 中间位置
    'scrapy.downloadermiddlewares.retry.RetryMiddleware': 500,
}
```

**执行顺序**：
- **请求阶段**：数字从小到大（优先级高→低）
- **响应阶段**：数字从大到小（反转）

```
请求: Engine → 100 → 543 → 500 → 750 → Downloader
响应: Engine ← 100 ← 543 ← 500 ← 750 ← Downloader
```

### 2.8 aiohttp 与 requests 性能对比原理

| 维度 | requests (同步) | aiohttp (异步) |
|------|-----------------|----------------|
| IO模型 | 阻塞式，每个请求独占线程 | 事件循环，单线程处理多请求 |
| 并发数100 | 100个线程 / 进程池 | 1个事件循环 + 100个Task |
| 内存占用 | ~1MB/线程 | ~几KB/Task |
| 上下文切换 | 操作系统内核切换 | 协程用户态切换，零开销 |

```python
# 压测对比：抓取20个URL
import time
import requests
import asyncio
import aiohttp

# 同步方式
start = time.time()
for url in urls:
    requests.get(url)
print(f"同步耗时: {time.time() - start:.2f}s")  # 约20s (假设每个1s)

# 异步方式
async def async_fetch_all():
    async with aiohttp.ClientSession() as session:
        tasks = [asyncio.create_task(fetch(session, url)) for url in urls]
        await asyncio.gather(*tasks)

start = time.time()
asyncio.run(async_fetch_all())
print(f"异步耗时: {time.time() - start:.2f}s")  # 约1-2s (并发20个)
```

### 2.9 IP代理池的设计要点

```python
import random
import redis
import requests

class ProxyPool:
    def __init__(self):
        self.redis = redis.Redis()
        self.key = "proxy_pool"

    def add_proxy(self, proxy, score=10):
        """添加代理，初始分数10"""
        self.redis.zadd(self.key, {proxy: score})

    def get_proxy(self):
        """随机获取高分代理"""
        proxies = self.redis.zrangebyscore(self.key, 7, 10)
        if not proxies:
            proxies = self.redis.zrevrange(self.key, 0, 10)
        return random.choice(proxies).decode() if proxies else None

    def decrease_score(self, proxy):
        """代理失败 → 分数减1，低于阈值则移除"""
        score = self.redis.zincrby(self.key, -1, proxy)
        if score <= 0:
            self.redis.zrem(self.key, proxy)
```

### 2.10 Selenium 如何绕过 navigator.webdriver 检测

```python
# Chrome Options 方式
from selenium.webdriver import Chrome, ChromeOptions

options = ChromeOptions()
options.add_experimental_option('excludeSwitches', ['enable-automation'])
options.add_experimental_option('useAutomationExtension', False)
options.add_argument('--disable-blink-features=AutomationControlled')

driver = Chrome(options=options)
driver.execute_cdp_cmd("Page.addScriptToEvaluateOnNewDocument", {
    "source": """
        Object.defineProperty(navigator, 'webdriver', {
            get: () => undefined
        });
        // 覆盖 plugins / languages / chrome 等检测属性
        Object.defineProperty(navigator, 'plugins', {
            get: () => [1, 2, 3, 4, 5]
        });
    """
})
```

> 💡 Playwright 默认已处理 `webdriver` 检测，是更好的选择。

---

## 三、实战场景题（8题）

> 💡 **面试特点**：场景题考察综合能力。需先明确需求边界，再给出技术选型和方案。

### 3.1 【字节跳动】如何爬取一个需要登录才能查看数据的网站？

**考点**：登录态维持、Session管理、验证码处理

**回答要点**：
1. **分析登录方式**：Cookie + Session vs JWT Token vs OAuth2.0
2. **模拟登录**：`requests.Session` + 账号密码提交
3. **验证码处理**：打码平台或深度学习识别
4. **Token 续期**：监控 Token 过期时间，自动刷新
5. **保存登录态**：将 Cookies/Token 持久化到文件或 Redis，避免重复登录

```python
import requests
import json

def login_and_crawl(username, password):
    session = requests.Session()
    # Step 1: 获取登录页面CSRF Token
    page = session.get("https://example.com/login")
    csrf = extract_csrf_token(page.text)
    # Step 2: 提交登录
    data = {"username": username, "password": password, "csrf": csrf}
    resp = session.post("https://example.com/api/login", data=data)
    if resp.status_code == 200:
        # Step 3: 保存cookies备用
        with open("cookies.json", "w") as f:
            json.dump(session.cookies.get_dict(), f)
        # Step 4: 携带登录态请求目标数据
        data_resp = session.get("https://example.com/api/protected/data")
        return data_resp.json()
    raise Exception("登录失败")
```

### 3.2 【阿里巴巴】目标网站使用JS动态渲染数据，如何爬取？

**考点**：JS渲染理解、抓包分析、渲染方案选型

**回答要点**：
1. **优先抓包（Network面板）**：查找 XHR/Fetch 请求，很多时候API直接返回JSON，无需渲染
2. **分析接口参数**：寻找加密参数（如 `sign`、`token`），进行JS逆向
3. **万不得已再渲染**：Playwright / Selenium 打开浏览器获取渲染后HTML

```python
# 推荐方案 —— 先尝试直接请求XHR接口
import requests

# 通过F12 Network找到真实数据接口
api_url = "https://example.com/api/v1/goods?page=1"
headers = {
    "User-Agent": "Mozilla/5.0",
    "X-Requested-With": "XMLHttpRequest",
    "Referer": "https://example.com/goods"
}
resp = requests.get(api_url, headers=headers)
print(resp.json())  # 直接拿到JSON数据，无需渲染
```

### 3.3 【腾讯】频控限制（429 Too Many Requests）如何解决？

**考点**：反爬对抗、代理池、请求频率控制

**回答要点**：
1. **请求间隔随机化**：不要固定 `time.sleep(1)`，使用 `random.uniform(1, 3)`
2. **IP代理池**：多IP轮询降低单个IP的请求频率
3. **请求去重**：避免重复请求浪费配额
4. **指数退避重试**

```python
import time
import random
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
def safe_request(url, proxies=None):
    time.sleep(random.uniform(1, 3))  # 随机间隔
    resp = requests.get(url, proxies=proxies, timeout=10)
    if resp.status_code == 429:
        raise Exception("Too Many Requests — 触发重试")
    return resp

proxies_list = ["http://ip1:port", "http://ip2:port"]
for url in urls:
    proxy = {"http": random.choice(proxies_list)}
    safe_request(url, proxies=proxy)
```

### 3.4 【美团】如何大规模抓取上万级别的商品数据？

**考点**：分布式爬虫、任务拆分、数据一致性

**回答要点**：
1. **任务拆分**：按品类/店铺/地域分片
2. **分布式架构**：Scrapy-Redis 或 Celery + 爬虫节点
3. **异步 IO**：aiohttp 单机并发 500+
4. **异常处理**：消息队列（失败请求重新入队）
5. **监控告警**：Prometheus + Grafana 监控爬虫状态

```python
# Scrapy-Redis 分布式爬虫配置
# settings.py
SCHEDULER = "scrapy_redis.scheduler.Scheduler"
DUPEFILTER_CLASS = "scrapy_redis.dupefilter.RFPDupeFilter"
REDIS_URL = "redis://master:6379"
CONCURRENT_REQUESTS = 32
DOWNLOAD_DELAY = 0.5
```

### 3.5 网站接口参数包含 sign 签名，如何破解？

**考点**：JS逆向、签名算法分析

**回答步骤**：
1. **抓包定位**：XHR断点 → 找到 `sign` 生成处
2. **调用栈回溯**：从构建请求的地方向函数内部追溯
3. **分析算法**：常见的签名是 `sort(params) + secretKey` 后 MD5
4. **Python 复现**

```javascript
// 浏览器中发现的签名算法（经过格式化）
function generateSign(params, timestamp) {
    var keys = Object.keys(params).sort();
    var baseStr = "";
    for (var i = 0; i < keys.length; i++) {
        baseStr += keys[i] + "=" + params[keys[i]] + "&";
    }
    baseStr += "timestamp=" + timestamp + "&key=your_secret_key";
    return CryptoJS.MD5(baseStr).toString();
}
```

```python
# Python 复现
import hashlib
import time

def generate_sign(params: dict, secret_key: str) -> str:
    timestamp = str(int(time.time() * 1000))
    items = sorted(params.items())
    base_str = "&".join(f"{k}={v}" for k, v in items)
    base_str += f"&timestamp={timestamp}&key={secret_key}"
    return hashlib.md5(base_str.encode()).hexdigest(), timestamp
```

### 3.6 如何爬取使用 WebSocket 推送数据的网站？

**考点**：WebSocket协议、长连接

```python
import asyncio
import websockets
import json

async def subscribe_data():
    async with websockets.connect("wss://example.com/ws") as ws:
        # 发送订阅消息
        await ws.send(json.dumps({"type": "subscribe", "channel": "trade"}))
        # 持续接收推送
        async for message in ws:
            data = json.loads(message)
            print(f"收到推送数据: {data['price']}")

asyncio.run(subscribe_data())
```

**回答要点**：抓包识别 `ws://` 或 `wss://` 连接 → 分析握手消息 → 保持长连接接收推送。

### 3.7 爬虫被网站封禁IP，如何排查和解决？

**排查步骤**：
1. **确认封禁**：检查状态码（403/429）、页面内容是否包含"访问过于频繁"
2. **定位原因**：单IP请求频率过高、User-Agent 不合法、Cookie 异常
3. **分层解决**：

| 层级 | 解决方案 |
|------|----------|
| 请求头 | 完善 User-Agent / Referer / Accept-Language |
| IP | 代理池轮换，住宅代理 > 数据中心代理 |
| 行为 | 添加随机间隔、模拟鼠标轨迹、随机浏览路径 |
| 指纹 | 固定浏览器指纹参数 |

### 3.8 网站使用了图片防盗链，如何下载图片？

**考点**：Referer 验证、图片懒加载

```python
def download_image(img_url, referer_url, save_path):
    headers = {
        "User-Agent": "Mozilla/5.0",
        "Referer": referer_url,          # 模拟来源页面
    }
    resp = requests.get(img_url, headers=headers, stream=True)
    if resp.status_code == 200:
        with open(save_path, "wb") as f:
            for chunk in resp.iter_content(chunk_size=8192):
                f.write(chunk)
```

> 💡 **进阶**：某些站点使用 `data:image/base64` 懒加载——需要找到真实图片地址（通常在 `data-src` 或 `src` 属性的加密值中）。

---

## 四、手写代码题（6题）

> 💡 **面试特点**：手写代码题考察编码能力和对库的熟练度。注意边界条件处理和异常处理。

### 4.1 用 requests 实现带重试机制的下载函数

```python
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

def create_session_with_retries(
    retries=3,
    backoff_factor=0.5,
    status_forcelist=(500, 502, 503, 504, 429),
):
    session = requests.Session()
    retry = Retry(
        total=retries,
        read=retries,
        connect=retries,
        backoff_factor=backoff_factor,
        status_forcelist=status_forcelist,
        allowed_methods=["GET", "POST"],
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session

# 使用
session = create_session_with_retries()
resp = session.get("https://example.com", timeout=10)
print(resp.status_code)
```

### 4.2 用 Scrapy 编写一个爬取新闻标题的 Spider

```python
import scrapy

class NewsSpider(scrapy.Spider):
    name = "news"
    start_urls = ["https://news.example.com"]

    def parse(self, response):
        """解析新闻列表页"""
        for article in response.css("div.article-item"):
            yield {
                "title": article.css("h2 a::text").get(),
                "url": article.css("h2 a::attr(href)").get(),
                "summary": article.css("p.summary::text").get(),
                "date": article.css("span.date::text").get(),
            }

        # 翻页
        next_page = response.css("a.next::attr(href)").get()
        if next_page:
            yield response.follow(next_page, self.parse)

    def parse_detail(self, response):
        """解析详情页"""
        yield {
            "url": response.url,
            "content": "".join(response.css("div.content p::text").getall()),
        }
```

### 4.3 用 XPath 从 HTML 中提取表格数据

```python
from lxml import etree

html = """
<table id="score-table">
  <tr><th>姓名</th><th>分数</th></tr>
  <tr><td>张三</td><td>95</td></tr>
  <tr><td>李四</td><td>88</td></tr>
</table>
"""

tree = etree.HTML(html)
rows = tree.xpath('//table[@id="score-table"]/tr[position()>1]')  # 跳过表头
data = []
for row in rows:
    name = row.xpath('./td[1]/text()')[0]
    score = row.xpath('./td[2]/text()')[0]
    data.append({"name": name, "score": int(score)})

print(data)  # [{'name': '张三', 'score': 95}, {'name': '李四', 'score': 88}]
```

### 4.4 用 Playwright 实现自动登录并截图

```python
from playwright.sync_api import sync_playwright

def auto_login_and_screenshot(url, username, password, screenshot_path):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            viewport={"width": 1920, "height": 1080},
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
        page = context.new_page()

        # 访问登录页
        page.goto(url)
        page.fill('input[name="username"]', username)
        page.fill('input[name="password"]', password)
        page.click('button[type="submit"]')

        # 等待登录完成
        page.wait_for_url("**/dashboard", timeout=10000)
        page.screenshot(path=screenshot_path, full_page=True)

        # 获取cookies
        cookies = context.cookies()
        browser.close()
        return cookies
```

### 4.5 实现一个简单的异步 IP 代理检测器

```python
import asyncio
import aiohttp

class ProxyChecker:
    def __init__(self, test_url="http://httpbin.org/ip", timeout=5):
        self.test_url = test_url
        self.timeout = timeout

    async def check_one(self, session, proxy):
        try:
            async with session.get(
                self.test_url,
                proxy=f"http://{proxy}",
                timeout=aiohttp.ClientTimeout(total=self.timeout)
            ) as resp:
                if resp.status == 200:
                    return proxy, True
        except Exception:
            pass
        return proxy, False

    async def check_many(self, proxy_list):
        async with aiohttp.ClientSession() as session:
            tasks = [self.check_one(session, p) for p in proxy_list]
            results = await asyncio.gather(*tasks)
        valid = [proxy for proxy, ok in results if ok]
        return valid

# 使用
async def main():
    checker = ProxyChecker()
    proxies = ["ip1:8080", "ip2:8080", "ip3:8080"]
    valid = await checker.check_many(proxies)
    print(f"可用代理: {valid}")

asyncio.run(main())
```

### 4.6 用 asyncio + aiofiles 实现异步文件存储

```python
import asyncio
import aiohttp
import aiofiles
import os
from urllib.parse import urlparse

async def download_file(session, url, save_dir):
    """异步下载文件并保存"""
    filename = os.path.basename(urlparse(url).path) or "index.html"
    save_path = os.path.join(save_dir, filename)

    async with session.get(url) as resp:
        if resp.status != 200:
            print(f"下载失败: {url} — {resp.status}")
            return

        # aiofiles 异步写入
        async with aiofiles.open(save_path, "wb") as f:
            while True:
                chunk = await resp.content.read(1024 * 64)  # 64KB
                if not chunk:
                    break
                await f.write(chunk)
        print(f"已保存: {save_path}")

async def batch_download(urls, save_dir="downloads"):
    os.makedirs(save_dir, exist_ok=True)
    async with aiohttp.ClientSession() as session:
        tasks = [download_file(session, url, save_dir) for url in urls]
        await asyncio.gather(*tasks)

# 使用
urls = ["https://example.com/file1.pdf", "https://example.com/file2.pdf"]
asyncio.run(batch_download(urls))
```

---

## 五、系统设计题（4题）

> 🎯 **面试特点**：系统设计题考察架构设计能力。使用 STAR 法则（场景-方案-行动-结果）组织回答。

### 5.1 设计一个百万级商品数据的爬虫系统

**需求**：爬取某电商平台100万+商品信息，包括标题、价格、评论数。

**架构设计**：

```
                          +-----------+
                          |   Redis   |  ← 请求队列 & 去重集合
                          +-----+-----+
                                |
          +----------+----------+----------+----------+
          |          |          |          |          |
    Spider-1   Spider-2   Spider-3  ...  Spider-N (K8s Pods)
          |          |          |          |
          +----------+----------+----------+
                                |
                     +----------v----------+
                     |    消息队列 (Kafka)    |   ← 解耦生产与消费
                     +----------+----------+
                                |
                     +----------v----------+
                     |   数据清洗 (Flink)    |   ← 去重、格式统一
                     +----------+----------+
                                |
                     +----------v----------+
                     |   MySQL (结构化)      |   ← 商品信息
                     |   MongoDB (评论)      |   ← 非结构化
                     |   ES (搜索)           |   ← 全文检索
                     +----------------------+
```

**关键设计要点**：

| 模块 | 技术选型 | 说明 |
|------|----------|------|
| 任务调度 | Scrapy-Redis | 统一请求队列，动态水平扩展 |
| 去重 | Redis Set + Bloom Filter | 亿级URL去重，降低内存90% |
| 代理 | 自建代理池（住宅+数据中心） | 按权重分配，自动剔除失效 |
| 频率控制 | 令牌桶算法 | 单IP QPS控制在合理范围 |
| 数据管道 | Kafka + Flink | 削峰填谷，实时清洗 |
| 监控 | Prometheus + Grafana | 爬取速率、失败率、延迟 |

### 5.2 设计一个通用爬虫监控告警系统

**指标收集**：

```python
# 爬虫中嵌入 Prometheus 指标
from prometheus_client import Counter, Histogram, start_http_server

# 定义指标
requests_total = Counter("scrapy_requests_total", "总请求数", ["spider", "status"])
response_duration = Histogram("scrapy_response_seconds", "响应耗时", ["spider"])
parse_errors = Counter("scrapy_parse_errors_total", "解析错误数", ["spider"])

# Spider中间件中上报
class MonitorMiddleware:
    def process_response(self, request, response, spider):
        requests_total.labels(spider=spider.name, status=response.status).inc()
        return response
```

**告警规则**：
- 5分钟内失败率 > 10% → P1告警
- 爬虫超过30分钟无数据产出 → P2告警
- IP封禁率 > 20% → 动态调整代理策略

### 5.3 设计一个 JS 逆向参数提取平台

**架构**：

```
用户输入目标URL
        ↓
 抓包代理 (mitmproxy) —— 自动捕获XHR请求
        ↓
 代码定位助手 —— 高亮可疑加密函数
        ↓
 加密算法识别 —— 模式匹配 (MD5/AES/RSA特征)
        ↓
 Python 代码 —— 自动生成Python解密代码
        ↓
 验证沙箱 —— 测试生成的解密函数是否正确
```

> 💡 工具推荐：`mitmproxy` + `ast` 解析 + 规则引擎匹配加密模式。

### 5.4 设计一个兼容多种反爬策略的动态代理调度器

```python
class DynamicProxyScheduler:
    """基于反馈的动态代理调度"""

    def __init__(self):
        self.proxies = {}  # proxy → ProxyStatus
        self.weights = {}  # proxy → weight

    def select_proxy(self):
        """加权随机选择代理"""
        total = sum(self.weights.values())
        if total == 0:
            return None
        r = random.uniform(0, total)
        cumulative = 0
        for proxy, weight in self.weights.items():
            cumulative += weight
            if r <= cumulative:
                return proxy

    def report_result(self, proxy, success, latency):
        """根据反馈调整权重"""
        status = self.proxies[proxy]
        if success:
            status.success_count += 1
            status.weight = min(10, status.weight + 0.5)
        else:
            status.fail_count += 1
            status.weight = max(0, status.weight - 2)
        # 连续失败3次 → 暂移除
        if status.fail_count >= 3:
            self.remove_proxy(proxy)

    def refresh_pool(self):
        """定期清理失效代理、补充新代理"""
        pass
```

---

## 六、常见坑点与最佳实践

> ⚠️ **核心原则**：爬虫开发中90%的问题来自**请求层面**（headers/cookies/代理），而不是解析层面。

### 6.1 常见坑点汇总

| 坑点 | 现象 | 原因 | 解决方案 |
|------|------|------|----------|
| 请求返回空数据 | 状态码200但body为空 | 反爬JS检测、参数缺失 | 补齐headers，检查请求参数 |
| XPath取不到值 | 解析结果为空列表 | HTML命名空间/大小写 | 使用 `contains()`，检查原始HTML |
| Session失效 | 登录后请求仍302到登录页 | Token过期/CSRF未更新 | 每次请求前获取新Token |
| 中文乱码 | 解析后中文为乱码 | 编码识别错误 | `resp.encoding = resp.apparent_encoding` |
| 内存溢出 | 爬取大量数据时OOM | 未分页/未控制并发数 | 使用 `CONCURRENT_REQUESTS` 限制，分页抓取 |
| 代理无效 | 请求超时或403 | 代理质量差/代理失效 | 代理池实时检测，分数淘汰机制 |
| 循环重定向 | 请求进入无限重定向 | Cookie缺失/Session校验失败 | `allow_redirects=False` 手动追踪 |
| `robots.txt` 阻止 | 爬虫遇到403 | Scrapy默认遵守robots | 设置 `ROBOTSTXT_OBEY = False` |

### 6.2 最佳实践清单

| 实践项 | 具体操作 |
|--------|----------|
| **User-Agent 管理** | 准备20-50个真实浏览器UA，每次随机选择 |
| **请求间隔** | `random.uniform(0.5, 2.0)`，避免固定间隔 |
| **异常处理** | 捕获 `requests.exceptions` 所有子类，分类处理 |
| **数据校验** | 解析后检查关键字段不为空，脏数据写入死信队列 |
| **日志记录** | 结构化日志（spider名/URL/状态码/耗时） |
| **幂等设计** | 以URL指纹为唯一键，支持断点续爬 |
| **频率控制** | 使用令牌桶算法，峰值QPS控制在阈值下 |
| **浏览器模拟** | 完善 Accept / Accept-Language / Accept-Encoding |

```python
# 完善的请求头模板
PERFECT_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                  "AppleWebKit/537.36 (KHTML, like Gecko) "
                  "Chrome/120.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    "Accept-Encoding": "gzip, deflate, br",
    "Connection": "keep-alive",
    "Upgrade-Insecure-Requests": "1",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "none",
    "Sec-Fetch-User": "?1",
}
```

---

## 七、面试回答模板（Top 5）

> 🎯 **使用方式**：面试时不需死记硬背，掌握回答结构和关键词即可。用 STAR 法则组织语言。

### 模板1：遇到反爬虫如何解决？

```
回答结构：
1. 识别反爬类型（IP限频/UA检测/签名校验/验证码）
2. 分层解决策略：
   - 表象层：完善请求头、随机UA、IP代理
   - 行为层：随机间隔、模拟浏览路径、随机延迟
   - 逻辑层：JS逆向、补环境、参数加密还原
3. 结合实例：我曾遇到XX网站采用XXX反爬手段，通过XXX方式解决，成功率从30%提升到95%
```

### 模板2：JS逆向的经验

```
回答结构：
1. 先抓包定位关键接口（XHR断点 + 调用栈回溯）
2. 分析加密逻辑（搜索 encrypt/sign/signature 关键字）
3. 使用AST工具反混淆，提取核心加密函数
4. 在Node.js中补环境运行，或Python重写
5. 验证：加密结果与浏览器一致
> 曾成功还原过XX平台基于webpack的AES+RSA混合加密
```

### 模板3：爬虫性能优化

```
回答结构：
1. IO模型：同步→异步（aiohttp + asyncio），并发量从几十提升到数千
2. 连接复用：Session / 连接池，避免TCP三次握手重复开销
3. 任务分发：Scrapy-Redis + 多节点水平扩展
4. 数据管道：Kafka异步写入，避免阻塞爬取流程
5. 实测数据：优化前单机2000条/小时，优化后单机5万条/小时
```

### 模板4：分布式爬虫设计思路

```
回答结构：
1. 使用Scrapy-Redis统一请求队列 + 去重集合（Redis Set + Bloom Filter）
2. 爬虫节点无状态，通过K8s动态扩缩容
3. 数据通过消息队列（Kafka）写入下游存储
4. 监控：Prometheus采集各节点指标（请求数/失败率/延迟）
5. 可扩展：新增爬虫只需修改Redis队列配置，无需改动节点
```

### 模板5：爬虫稳定性保障

```
回答结构：
1. 异常分类：网络异常/解析异常/反爬触发/数据异常
2. 重试机制：指数退避（1s→2s→4s→8s），最大重试3次
3. 告警：失败率 > 10% 触发P1告警，30分钟无数据触发P2
4. 容错：失败URL写入死信队列，人工或自动补爬
5. 持久化：中间结果写入Redis，支持断点续爬
```

---

## 八、快速查漏补缺Checklist

### 8.1 HTTP / 网络基础
- [ ] HTTP/HTTPS 区别与握手流程
- [ ] GET / POST / PUT / DELETE 语义
- [ ] 常见状态码含义（200/301/403/429/500）
- [ ] Cookie / Session / Token 的区别
- [ ] HTTPS 证书验证流程

### 8.2 爬虫框架
- [ ] Scrapy 组件与数据流（Engine → Scheduler → Downloader → Spider → Pipeline）
- [ ] Spider 的 parse / start_requests / closed 回调
- [ ] Item Pipeline 的 open_spider / process_item / close_spider
- [ ] Downloader Middleware 的 process_request / process_response / process_exception
- [ ] Scrapy-Redis 配置与原理
- [ ] 请求去重：指纹生成算法

### 8.3 数据解析
- [ ] XPath 常用函数（text / contains / starts-with / position）
- [ ] BeautifulSoup 的 find / find_all / select
- [ ] CSS 选择器（class / id / 属性 / 伪类）
- [ ] JSONPath 语法（`$..book[?(@.price<10)]`）
- [ ] 正则表达式提取（re.findall / re.search）

### 8.4 反爬与对抗
- [ ] UA 识别与随机UA池
- [ ] IP 代理池（HTTP/HTTPS/SOCKS5）
- [ ] 请求频率控制（随机间隔 + 令牌桶）
- [ ] Cookie / Session 维持
- [ ] WebDriver 检测绕过
- [ ] Canvas / WebGL 指纹对抗
- [ ] 验证码识别方案

### 8.5 JS 逆向
- [ ] Chrome DevTools 断点调试（XHR / 事件 / 条件断点）
- [ ] 调用栈分析与回溯
- [ ] MD5 / SHA / AES / RSA / Base64 特征识别
- [ ] webpack 模块加载器结构与复现
- [ ] AST 语法树与代码反混淆
- [ ] 反调试绕过（无限debugger / 时间校验）

### 8.6 异步与性能
- [ ] asyncio 事件循环原理
- [ ] aiohttp 基本使用
- [ ] asyncio.gather vs asyncio.wait
- [ ] 信号量控制并发数（asyncio.Semaphore）
- [ ] aiofiles 异步IO

### 8.7 数据存储
- [ ] CSV 读写（csv.DictReader / DictWriter）
- [ ] MySQL 写入去重（INSERT IGNORE / ON DUPLICATE KEY）
- [ ] MongoDB 索引与聚合
- [ ] Redis 数据类型（Set/Hash/ZSet/LRU）

### 8.8 工程化
- [ ] 日志配置（Python logging + 分级别输出）
- [ ] 配置管理（yaml / json / 环境变量）
- [ ] Docker 容器化爬虫
- [ ] 定时调度（APScheduler / Crontab / Airflow）
- [ ] 数据质量监控（字段完整性 / 一致性）

---

> 📚 **参考资源**
> - 《Python爬虫开发与项目实战》
> - Scrapy 官方文档: https://docs.scrapy.org/
> - Playwright 官方文档: https://playwright.dev/python/
> - Scrapy-Redis 文档: https://github.com/rmax/scrapy-redis
> - Chrome DevTools 调试指南: https://developer.chrome.com/docs/devtools/
