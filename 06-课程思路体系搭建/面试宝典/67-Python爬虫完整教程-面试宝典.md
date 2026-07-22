# Python爬虫完整教程 面试宝典
> 基于98集完整课程大纲，从零基础到分布式爬虫，全面覆盖 requests、数据解析、反爬处理、并发爬虫、Selenium、Scrapy及加密破解面试高频考点

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践（表格）](#六常见坑点与最佳实践表格)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

### 1.1 Web请求全过程与HTTP协议
> 💡 爬虫本质是模拟浏览器向服务器发起HTTP请求并解析响应数据。理解Web请求全过程是爬虫开发的基石。

| 阶段 | 说明 | 爬虫关注点 |
|------|------|-----------|
| DNS解析 | 域名转IP地址 | 可能被DNS劫持/污染 |
| 建立TCP连接 | 三次握手 | 连接复用（Keep-Alive） |
| 发送HTTP请求 | Request Line + Headers + Body | 请求方法、UA、Cookie、Referer |
| 服务器处理 | 业务逻辑执行 | 关注响应时间 |
| 返回HTTP响应 | Status Code + Headers + Body | 状态码、Content-Type、响应体 |
| 渲染/解析 | 浏览器渲染HTML | 直接解析原始HTML/JSON |

```python
import requests
# 模拟完整请求过程
session = requests.Session()
session.headers.update({
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept-Language': 'zh-CN,zh;q=0.9',
    'Accept-Encoding': 'gzip, deflate, br',
    'Connection': 'keep-alive'
})
resp = session.get('https://httpbin.org/get', timeout=10)
print(f"Status: {resp.status_code}, Encoding: {resp.encoding}, Content-Type: {resp.headers.get('Content-Type')}")
```

### 1.2 requests模块全场景用法
> 💡 requests是Python爬虫最核心的HTTP库，必须熟练掌握。

| 功能 | API | 说明 |
|------|-----|------|
| GET请求 | `requests.get(url, params=...)` | 查询参数自动拼接 |
| POST请求 | `requests.post(url, data=/json=...)` | 表单/JSON提交 |
| Session | `requests.Session()` | 自动管理Cookie |
| 代理 | `proxies={'http':..., 'https':...}` | 绕过IP限制 |
| 超时 | `timeout=(connect, read)` | 防止请求卡死 |
| SSL | `verify=False` | 跳过证书验证 |
| 重定向 | `allow_redirects=False` | 禁用自动重定向 |
| 流式响应 | `stream=True` | 大文件下载 |

```python
import requests

# 1. GET + 参数
r = requests.get('https://api.douban.com/v2/book/search', params={'q': 'python', 'count': 10})

# 2. POST + 表单/JSON/文件
r1 = requests.post('https://httpbin.org/post', data={'key': 'value'})
r2 = requests.post('https://httpbin.org/post', json={'key': 'value'})
r3 = requests.post('https://httpbin.org/post', files={'file': open('test.txt', 'rb')})

# 3. Session + 保持登录
s = requests.Session()
s.post('https://example.com/login', data={'username': 'admin', 'password': '123'})
profile = s.get('https://example.com/profile')  # 自动携带cookie

# 4. 代理 + 超时
proxies = {'http': 'http://127.0.0.1:7890', 'https': 'http://127.0.0.1:7890'}
r = requests.get('https://www.google.com', proxies=proxies, timeout=5)

# 5. 大文件分块下载
r = requests.get('https://example.com/bigfile.zip', stream=True)
with open('file.zip', 'wb') as f:
    for chunk in r.iter_content(chunk_size=8192):
        if chunk:
            f.write(chunk)
```

### 1.3 HTML解析库全方位对比

| 解析库 | 原理 | 性能 | 学习曲线 | 推荐场景 |
|-------|------|------|---------|---------|
| 正则表达式 | 模式匹配 | 最快 | 陡峭 | 简单文本提取 |
| BeautifulSoup4 | DOM树解析 | 较慢 | 平缓 | 新手首选，结构清晰 |
| lxml + XPath | C底层解析 | 快 | 中等 | 高性能生产环境 |
| PyQuery | jQuery风格 | 中等 | 平缓 | 前端转爬虫开发者 |
| Selector（Scrapy内置） | 封装lxml | 快 | 中等 | Scrapy项目 |

```python
html = '<html><body><div class="content"><a href="/page/1">Link 1</a><a href="/page/2">Link 2</a></div></body></html>'

# --- 正则 ---
import re
links = re.findall(r'<a href="(.*?)">(.*?)</a>', html)
# [('/page/1', 'Link 1'), ('/page/2', 'Link 2')]

# --- BeautifulSoup ---
from bs4 import BeautifulSoup
soup = BeautifulSoup(html, 'html.parser')
for a in soup.select('div.content a'):
    print(a.text, a['href'])

# --- lxml XPath ---
from lxml import etree
tree = etree.HTML(html)
for href in tree.xpath('//div[@class="content"]/a/@href'):
    print(href)

# --- PyQuery ---
from pyquery import PyQuery as pq
doc = pq(html)
for a in doc('div.content a').items():
    print(a.text(), a.attr('href'))
```

### 1.4 正则表达式核心模式速查

| 模式 | 含义 | 示例 |
|------|------|------|
| `.` | 匹配任意字符（除换行） | `p.thon` 匹配 `python` |
| `*` | 前一个字符0次或多次 | `ab*c` 匹配 `ac`, `abc`, `abbc` |
| `+` | 前一个字符1次或多次 | `ab+c` 匹配 `abc`, `abbc` |
| `?` | 前一个字符0次或1次/非贪婪 | `ab?c` 匹配 `ac`, `abc` |
| `{n,m}` | 前一个字符n到m次 | `\d{3,4}` 匹配3或4位数字 |
| `[]` | 字符集 | `[a-z0-9]` 匹配小写字母或数字 |
| `()` | 分组捕获 | `(\d{4})-(\d{2})` 提取日期 |
| `.*?` | 非贪婪任意匹配 | 最常用的懒惰匹配模式 |

```python
import re

# 提取手机号
text = "联系方式：13800138000 或 13912345678"
phones = re.findall(r'1[3-9]\d{9}', text)
# ['13800138000', '13912345678']

# 提取邮箱
text = "联系邮箱：user@example.com, admin@test.org.cn"
emails = re.findall(r'[\w.+-]+@[\w-]+\.[\w.-]+', text)
# ['user@example.com', 'admin@test.org.cn']

# 分组提取
text = "2024-01-15 商品A 99.50元"
match = re.search(r'(\d{4}-\d{2}-\d{2})\s+(\S+)\s+(\d+\.\d+)', text)
if match:
    date, product, price = match.groups()
    print(f"日期：{date}，商品：{product}，价格：{price}")
```

### 1.5 数据存储方案横向对比

| 方案 | 适用场景 | 优点 | 缺点 | 常用库 |
|------|---------|------|------|-------|
| CSV | 结构化表格数据 | 通用性强，Excel可打开 | 不支持嵌套，无编码标准 | `csv` |
| JSON | API数据/配置 | 原生Python字典转换 | 不支持索引查询 | `json` |
| MySQL | 关系型数据 | ACID事务，复杂查询 | 需要建表schema | `pymysql` |
| MongoDB | 非结构化数据 | 灵活，水平扩展好 | 不支持JOIN | `pymongo` |
| SQLite | 单机小数据量 | 零配置，文件型 | 并发写入差 | `sqlite3` |

```python
# CSV
import csv
with open('output.csv', 'w', newline='', encoding='utf-8-sig') as f:
    w = csv.DictWriter(f, fieldnames=['title', 'price', 'url'])
    w.writeheader()
    w.writerows(data)

# MySQL
import pymysql
conn = pymysql.connect(host='localhost', user='root', password='123456', db='spider')
with conn.cursor() as cur:
    cur.execute("INSERT INTO products (title, price) VALUES (%s, %s)", ('商品A', 99.9))
conn.commit()

# MongoDB
from pymongo import MongoClient
col = MongoClient()['spider']['products']
col.insert_many([{'title': '商品A', 'price': 99.9}])
```

### 1.6 常见反爬手段与分层应对方案

| 反爬层级 | 手段 | 检测方式 | 应对方案 | 实施难度 |
|---------|------|---------|---------|---------|
| 传输层 | IP频率限制 | 同一IP请求频率异常 | 代理IP池（快代理/芝麻/付费代理） | 中 |
| 请求头层 | UA检测/Referer防盗链 | 检查HTTP请求头 | 随机UA池 + 补充Referer/Origin | 低 |
| Cookie层 | Session校验/登录验证 | 缺少Cookie/Token | requests.Session + 模拟登录 | 低 |
| 行为层 | 验证码 | 极验/腾讯滑块/图形码 | 打码平台/轨迹模拟/深度模型 | 高 |
| 数据层 | 字体反爬/数据加密 | 显示正常但爬取乱码 | 字体逆向映射/JS加密分析 | 高 |
| 渲染层 | JS动态渲染/骨架屏 | 数据在XHR请求中 | 抓包分析/Selenium/Playwright | 中 |
| 风控层 | 浏览器指纹/WebDriver检测 | navigator属性/Selenium特征 | 使用Playwright stealth/undetected-chromedriver | 高 |

```python
from fake_useragent import UserAgent
import random

# 随机UA池
ua = UserAgent()
headers = {'User-Agent': ua.random}

# 代理池（支持测试有效性）
proxy_list = [
    'http://ip1:port', 'http://ip2:port', 'http://ip3:port'
]
proxies = {'http': random.choice(proxy_list), 'https': random.choice(proxy_list)}

# 带重试机制的请求
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

session = requests.Session()
retry = Retry(total=3, backoff_factor=1, status_forcelist=[500, 502, 503, 504])
adapter = HTTPAdapter(max_retries=retry)
session.mount('http://', adapter)
session.mount('https://', adapter)
```

### 1.7 Scrapy框架核心组件与工作流程

> 🎯 Scrapy是一个异步爬虫框架，核心设计理念是组件化、可扩展。

| 组件 | 作用 | 必须实现？ | 关键方法 |
|------|------|-----------|---------|
| Spider | 定义爬取逻辑和解析规则 | 是 | `start_requests()` / `parse()` |
| Item | 定义数据结构和字段 | 推荐 | `scrapy.Item` + `scrapy.Field()` |
| Item Pipeline | 数据清洗、去重、存储 | 否 | `process_item()` / `open_spider()` |
| Downloader Middleware | 请求/响应预处理 | 否 | `process_request()` / `process_response()` |
| Spider Middleware | Spider输入输出拦截 | 否 | `process_spider_input()` / `process_spider_output()` |
| Scheduler | 管理请求队列和去重 | 内置 | 支持优先级和延迟 |
| Engine | 控制数据流在组件间流动 | 内置 | 开发不需关注 |

```python
# items.py
import scrapy
class ProductItem(scrapy.Item):
    title = scrapy.Field()
    price = scrapy.Field()
    url = scrapy.Field()
    crawl_time = scrapy.Field()

# spiders/products.py
import scrapy
class ProductsSpider(scrapy.Spider):
    name = 'products'
    start_urls = ['https://example.com/products']

    def parse(self, response):
        for product in response.css('div.product'):
            item = ProductItem()
            item['title'] = product.css('h2::text').get()
            item['price'] = product.css('span.price::text').get()
            item['url'] = response.urljoin(product.css('a::attr(href)').get())
            yield item

        # 翻页
        next_page = response.css('a.next::attr(href)').get()
        if next_page:
            yield response.follow(next_page, self.parse)

# pipelines.py
class MongoPipeline:
    def open_spider(self, spider):
        self.client = MongoClient()
        self.collection = self.client['spider']['products']

    def process_item(self, item, spider):
        self.collection.insert_one(dict(item))
        return item
```

### 1.8 并发爬虫方案对比

| 方案 | 原理 | 适用场景 | 性能 | 复杂度 |
|------|------|---------|------|-------|
| 多线程 | 共享内存，IO复用 | IO密集型爬虫 | 高 | 低 |
| 多进程 | 独立内存，CPU并行 | CPU密集型 | 中 | 中 |
| 线程池/进程池 | 池化管理 | 可控并发 | 高 | 低 |
| 异步协程 | 事件循环+await | 高并发IO | 最高 | 中 |
| aiohttp | 异步HTTP | 大规模爬取 | 极高 | 中高 |

```python
# —— 协程爬虫（最高性能）——
import asyncio
import aiohttp

async def fetch(session, url):
    async with session.get(url, timeout=10) as resp:
        return await resp.text()

async def main():
    urls = [f'https://example.com/page/{i}' for i in range(50)]
    async with aiohttp.ClientSession() as session:
        tasks = [fetch(session, url) for url in urls]
        results = await asyncio.gather(*tasks, return_exceptions=True)

asyncio.run(main())

# —— 线程池（适中）——
from concurrent.futures import ThreadPoolExecutor, as_completed

def fetch_url(url):
    r = requests.get(url, timeout=5)
    return (url, len(r.text))

with ThreadPoolExecutor(max_workers=16) as pool:
    futures = {pool.submit(fetch_url, url): url for url in urls}
    for future in as_completed(futures):
        url, length = future.result()
        print(f"{url}: {length} bytes")
```

### 1.9 加密算法识别速查表

| 算法 | 特征 | 输出长度 | 常见用途 | 破解难度 |
|------|------|---------|---------|---------|
| MD5 | 32位hex（a-f0-9） | 128bit | 登录密码传输 | 可碰撞 |
| SHA1 | 40位hex | 160bit | 数据签名 | 已弃用 |
| SHA256 | 64位hex | 256bit | 签名验证 | 安全 |
| AES | 需key+iv，CBC/ECB模式 | 动态 | 接口参数加密 | 需定位密钥 |
| RSA | 2048位公钥+密文 | 动态 | 登录/支付 | 需定位私钥 |
| Base64 | 末尾`=`，含`+/`字符 | 变长 | 数据传输编码 | 非加密 |
| DES/3DES | 64位分组 | 动态 | 老旧系统 | 已不安全 |

```python
# JS逆向常用的Python解密实现
import base64
import hashlib
from Crypto.Cipher import AES

def md5_encrypt(text):
    return hashlib.md5(text.encode()).hexdigest()

def sha256_encrypt(text):
    return hashlib.sha256(text.encode()).hexdigest()

def aes_decrypt(ciphertext, key, iv):
    """AES/CBC/PKCS7解密"""
    cipher = AES.new(key.encode(), AES.MODE_CBC, iv.encode())
    decrypted = cipher.decrypt(base64.b64decode(ciphertext))
    # 去除PKCS7填充
    pad_len = decrypted[-1]
    return decrypted[:-pad_len].decode()

def base64_decode(text):
    return base64.b64decode(text).decode('utf-8')
```

---

## 二、深度原理剖析

### 2.1 Cookie与Session的爬虫处理机制

> ⚠️ Cookie是爬虫保持登录状态的核心，处理不当会导致频繁被要求登录。

```python
# 方案1：requests.Session自动管理（推荐）
session = requests.Session()
# 登录后session自动保存cookie
resp = session.post('https://example.com/login', json={'username': 'user', 'password': 'pass'})
# 后续请求自动携带cookie
resp2 = session.get('https://example.com/dashboard')

# 方案2：手动提取和注入cookie
resp = requests.get('https://example.com')
cookies = resp.cookies.get_dict()  # 提取
requests.get('https://example.com/profile', cookies=cookies)  # 注入

# 方案3：将cookie转为header形式
cookie_str = '; '.join([f'{k}={v}' for k, v in cookies.items()])
headers = {'Cookie': cookie_str}

# 方案4：从浏览器导出cookies.json导入
import json
with open('cookies.json') as f:
    cookies_dict = json.load(f)
session.cookies.update(cookies_dict)
```

### 2.2 字体反爬的破解原理与实现

> 🎯 字体反爬通过@font-face自定义字体，使浏览器正常显示但爬虫获取的HTML中是乱码。

破解步骤：
1. 从HTML/CSS中提取字体文件URL（.woff/.ttf/.eot）
2. 下载字体文件并使用`fontTools`解析
3. 建立字形轮廓坐标到字符的映射关系
4. 替换HTML中的乱码字符

```python
from fontTools.ttLib import TTFont
import re

def crack_font_anti_spider(html, font_url, char_map):
    """
    字体反爬破解
    :param html: 含乱码的HTML
    :param font_url: 字体文件URL
    :param char_map: 预建立的字符映射 {字形名: 正确字符}
    """
    # 下载字体
    import requests
    resp = requests.get(font_url)
    with open('temp.woff', 'wb') as f:
        f.write(resp.content)

    # 解析字体映射
    font = TTFont('temp.woff')
    cmap = font.getBestCmap()

    # 建立编码-字符映射
    glyph_map = {}
    for code, name in cmap.items():
        glyph = font.getGlyphSet()[name]
        # 提取轮廓坐标作为特征
        coords = list(glyph.coordinates)
        # 与预定义坐标字典匹配
        glyph_map[chr(code)] = char_map.get(tuple(coords), chr(code))

    # 替换HTML中的字体编码
    for enc_char, real_char in glyph_map.items():
        html = html.replace(enc_char, real_char)

    return html
```

### 2.3 Selenium与Playwright深度对比

| 对比维度 | Selenium | Playwright |
|---------|----------|-----------|
| 底层协议 | WebDriver（W3C标准） | Chrome DevTools Protocol |
| 启动速度 | 较慢（启动完整浏览器） | 快（支持Browser Context隔离） |
| API风格 | 同步为主（需额外安装asyncio） | 原生支持同步和异步 |
| 自动等待 | 需要显式`WebDriverWait` | 内置auto-waiting机制 |
| 网络拦截 | 能力有限 | 强大的`route()`拦截和mock |
| 并发 | 多进程/多实例 | 多Browser Context + 异步 |
| 反检测 | 容易被检测到WebDriver | stealth模式更好，但仍有特征 |
| 移动端模拟 | 有限 | 内置Device Emulation |
| 社区生态 | 成熟，资源多 | 正在快速成长 |

```python
# —— Selenium 示例 ——
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

driver = webdriver.Chrome()
driver.get('https://example.com')
# 显式等待
elem = WebDriverWait(driver, 10).until(
    EC.presence_of_element_located((By.CSS_SELECTOR, 'div.result'))
)
print(elem.text)
driver.quit()

# —— Playwright 示例 ——
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=False)
    context = browser.new_context(
        user_agent='Mozilla/5.0 ...',
        viewport={'width': 1920, 'height': 1080}
    )
    page = context.new_page()
    # 自动等待直到元素可见
    page.goto('https://example.com')
    page.fill('input[name="username"]', 'user')
    page.click('button[type="submit"]')
    # 拦截网络请求
    page.route('**/*.png', lambda route: route.abort())
    content = page.content()
    browser.close()
```

### 2.4 验证码识别与应对策略

| 验证码类型 | 识别方法 | 识别率 | 成本 | 适用场景 |
|-----------|---------|-------|------|---------|
| 数字字母验证码 | Tesseract OCR + 图像预处理 | 60-80% | 免费 | 简单验证码 |
| 中文验证码 | CNN分类模型 | 85-95% | 中（需训练） | 中文识别 |
| 滑动验证码 | 轨迹模拟 + 缺口识别 | 70-90% | 中 | 极验/腾讯滑块 |
| 点选验证码 | OCR + NLP | 60-80% | 高 | 12306等 |
| 极验3.0 | 参数逆向 + 轨迹模拟 | 80-90% | 高 | 各类网站 |
| 行为验证 | 需逆向风控参数 | 不定 | 极高 | 反爬严格场景 |

```python
# OpenCV+Tesseract识别数字验证码
import cv2
import pytesseract

def recognize_captcha(image_path):
    # 读取图像
    img = cv2.imread(image_path)
    # 灰度化
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    # 二值化
    _, binary = cv2.threshold(gray, 150, 255, cv2.THRESH_BINARY_INV)
    # 去噪
    denoised = cv2.medianBlur(binary, 3)
    # OCR识别
    text = pytesseract.image_to_string(denoised, config='--psm 7 -c tessedit_char_whitelist=0123456789')
    return text.strip()

# 使用打码平台（超级鹰/图鉴）
import requests
def captcha_by_platform(image_path):
    with open(image_path, 'rb') as f:
        resp = requests.post(
            'http://api.ttshitu.com/predict',
            data={'username': 'xxx', 'password': 'xxx', 'typeid': 1},
            files={'image': f}
        )
    return resp.json()['data']['result']
```

### 2.5 Scrapy-Redis分布式爬虫原理

> 💡 Scrapy的Scheduler默认基于内存，Scrapy-Redis将其替换为Redis实现，使多个爬虫节点共享请求队列和去重集合。

```python
# settings.py 配置
SCHEDULER = "scrapy_redis.scheduler.Scheduler"
DUPEFILTER_CLASS = "scrapy_redis.dupefilter.RFPDupeFilter"
SCHEDULER_PERSIST = True  # 爬取暂停后不丢失队列
REDIS_URL = 'redis://localhost:6379'

# 去重机制：
# 1. 每个请求生成指纹（sha1(method+url+body+headers)）
# 2. 指纹存入Redis Set（dupefilter）
# 3. 请求存入Redis List（scheduler队列）

# 分布式部署要点：
# 1. 所有节点配置相同Redis地址
# 2. 共享代理池在Redis中
# 3. 爬虫节点无状态，可任意扩缩
# 4. 数据存储指向同一数据库

# 启动多个爬虫节点（不同机器或同一机器多进程）
# scrapy crawl myspider --nolog &
# scrapy crawl myspider --nolog &
```

---

## 三、实战场景题

### 3.1 抓取网易云音乐评论（Cookie + 加密 + 翻页）

> 综合考察Cookie处理、JS逆向、数据解析和翻页能力。

```python
import requests
from Crypto.Cipher import AES
import base64, json

class NeteaseMusicSpider:
    """网易云音乐评论爬虫"""
    def __init__(self):
        self.session = requests.Session()
        # 从浏览器复制cookie
        self.session.cookies.update({'MUSIC_U': 'xxxx'})
        self.headers = {
            'User-Agent': 'Mozilla/5.0 ...',
            'Referer': 'https://music.163.com/'
        }

    @staticmethod
    def aes_encrypt(text, key):
        """AES/CBC/PKCS7加密"""
        iv = b'0102030405060708'
        # PKCS7填充
        pad = 16 - len(text) % 16
        text = text + chr(pad) * pad
        cipher = AES.new(key.encode(), AES.MODE_CBC, iv)
        return base64.b64encode(cipher.encrypt(text.encode())).decode()

    @staticmethod
    def rsa_encrypt(text):
        """RSA加密encSecKey"""
        # 固定公钥和模数（从网页JS中提取）
        pub_key = '010001'
        modulus = '00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7'
        # 实际代码使用pycryptodome的RSA加密
        return "xxx"  # 简化处理

    def encrypt_params(self, data):
        """生成加密请求参数"""
        text = json.dumps(data)
        sec_key = 'TA3YiYCfY2dDJQ6T'  # 16位随机密钥
        params = self.aes_encrypt(text, sec_key)
        enc_sec_key = self.rsa_encrypt(sec_key)
        return {'params': params, 'encSecKey': enc_sec_key}

    def get_comments(self, song_id, offset=0, limit=20):
        url = 'https://music.163.com/weapi/v1/resource/comments/R_SO_4_' + str(song_id)
        data = {'rid': f'R_SO_4_{song_id}', 'offset': offset, 'total': 'true', 'limit': limit}
        encrypted = self.encrypt_params(data)
        resp = self.session.post(url, data=encrypted, headers=self.headers)
        return resp.json()

# 使用
# spider = NeteaseMusicSpider()
# comments = spider.get_comments(186016, offset=0)
# for c in comments.get('comments', []):
#     print(c['user']['nickname'], c['content'])
```

### 3.2 抓取梨视频（防盗链 + 动态页面）

> 考察Referer防盗链处理和Selenium/Playwright基础。

```python
import requests
from lxml import etree

def crawl_pearvideo():
    """梨视频爬虫 - 防盗链处理"""
    headers = {
        'User-Agent': 'Mozilla/5.0 ...',
        'Referer': 'https://www.pearvideo.com/',  # 关键防盗链
    }
    # 获取视频列表
    list_url = 'https://www.pearvideo.com/category_8'
    resp = requests.get(list_url, headers=headers)
    tree = etree.HTML(resp.text)

    videos = []
    for item in tree.xpath('//ul[@class="listvideo-list"]/li'):
        title = item.xpath('.//div[@class="vervideo-title"]/text()')[0]
        video_id = item.xpath('.//a/@href')[0].split('_')[-1]
        # 通过API获取真实视频地址
        api_url = f'https://www.pearvideo.com/videoStatus.jsp?contId={video_id}'
        api_resp = requests.get(api_url, headers=headers)
        # 视频URL在返回JSON中（需要替换systemTime）
        json_data = api_resp.json()
        src_url = json_data['videoInfo']['videos']['srcUrl']
        system_time = json_data['systemTime']
        real_url = src_url.replace(system_time, f'cont-{video_id}')
        videos.append({'title': title, 'url': real_url})

    return videos
```

### 3.3 抓取新发地菜价（线程池 + 数据存储）

```python
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
import json, time, random

def crawl_xinfadi():
    """新发地菜价爬虫 - 线程池版本"""
    headers = {'User-Agent': 'Mozilla/5.0 ...'}
    base_url = 'http://www.xinfadi.com.cn/getPriceData.html'
    all_data = []

    def fetch_page(page):
        form_data = {'limit': 20, 'current': page}
        try:
            resp = requests.post(base_url, data=form_data, headers=headers, timeout=5)
            return resp.json().get('list', [])
        except Exception as e:
            return []

    # 先获取总页数
    first = fetch_page(1)
    total = first[0]['total'] if first else 0
    total_pages = min((total // 20) + 1, 50)  # 限制最大50页

    # 多线程爬取
    with ThreadPoolExecutor(max_workers=10) as pool:
        futures = {pool.submit(fetch_page, p): p for p in range(1, total_pages + 1)}
        for future in as_completed(futures):
            all_data.extend(future.result())
            time.sleep(random.uniform(0.5, 1.5))  # 礼貌延迟

    # 写入JSON
    with open('xinfandi_prices.json', 'w', encoding='utf-8') as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)

    print(f"共爬取 {len(all_data)} 条菜价记录")
```

### 3.4 异步协程爬取小说（aiohttp + 文件存储）

```python
import asyncio
import aiohttp
from bs4 import BeautifulSoup
import os

class AsyncNovelSpider:
    """异步协程爬虫 - 扒光一部小说"""
    def __init__(self, base_url, concurrency=5):
        self.base_url = base_url
        self.semaphore = asyncio.Semaphore(concurrency)

    async def fetch(self, session, url):
        async with self.semaphore:
            async with session.get(url, timeout=10) as resp:
                return await resp.text(encoding='utf-8')

    async def get_chapter_list(self, session):
        """获取章节列表"""
        html = await self.fetch(session, self.base_url)
        soup = BeautifulSoup(html, 'html.parser')
        chapters = []
        for a in soup.select('.chapter-list a'):
            title = a.text.strip()
            href = a['href']
            if not href.startswith('http'):
                href = self.base_url + href
            chapters.append((title, href))
        return chapters

    async def get_chapter_content(self, session, title, url):
        """获取单章内容"""
        html = await self.fetch(session, url)
        soup = BeautifulSoup(html, 'html.parser')
        content = soup.select_one('#content').text.strip()
        return title, content

    async def run(self):
        async with aiohttp.ClientSession() as session:
            chapters = await self.get_chapter_list(session)
            tasks = [self.get_chapter_content(session, t, u) for t, u in chapters]
            results = await asyncio.gather(*tasks)

        # 写入文件
        os.makedirs('novel', exist_ok=True)
        for title, content in results:
            filename = f"novel/{title.replace('/', '_')}.txt"
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"已保存：{title}")

# asyncio.run(AsyncNovelSpider('https://example-novel.com').run())
```

### 3.5 JS逆向破解网站加密参数

**字节/腾讯高频面试题：如何在浏览器中定位加密参数并还原？**

```python
# 步骤方法论
"""
1. 打开Chrome DevTools -> Network -> 找到带加密参数的XHR请求
2. 查看 Initiator（调用堆栈） -> 找到发起请求的JS代码
3. 搜索关键字（如 params, sign, token, encrypt, md5, rsa）
4. 打XHR断点/条件断点 -> 逐步调试定位加密函数
5. 在Sources面板中格式化（Pretty Print）混淆后的JS
6. 提取关键逻辑：加密算法 + 密钥 + 参数拼接规则
7. 在Python中复现或使用 node.execJS() 调用原JS
"""

# 使用 execjs 调用浏览器JS
import execjs
import requests

# 从网页中复制加密JS函数
js_code = """
function getSign(params) {
    var keys = Object.keys(params).sort();
    var str = keys.map(k => k + '=' + params[k]).join('&');
    return md5(str + 'secret_key_xxx');
}
"""
ctx = execjs.compile(js_code)

def get_data_with_sign(params):
    sign = ctx.call('getSign', params)
    params['sign'] = sign
    resp = requests.post('https://api.example.com/data', json=params)
    return resp.json()
```

---

## 四、手写代码题

### 4.1 通用爬虫基类封装（模板方法模式）

```python
import requests
from typing import Optional, Dict, List, Any

class BaseSpider:
    """爬虫基类 - 模板方法模式"""
    name = 'base_spider'
    start_urls: List[str] = []
    headers: Dict[str, str] = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }

    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update(self.headers)
        self.results: List[Dict[str, Any]] = []

    def send_request(self, url: str, method: str = 'GET', **kwargs) -> requests.Response:
        """发送HTTP请求"""
        kwargs.setdefault('timeout', 10)
        if method.upper() == 'GET':
            return self.session.get(url, **kwargs)
        elif method.upper() == 'POST':
            return self.session.post(url, **kwargs)
        else:
            raise ValueError(f"Unsupported method: {method}")

    def parse(self, response: requests.Response) -> List[Dict[str, Any]]:
        """解析响应（子类实现）"""
        raise NotImplementedError

    def save(self, data: List[Dict[str, Any]]):
        """保存数据（子类实现）"""
        raise NotImplementedError

    def run(self):
        """执行爬虫"""
        for url in self.start_urls:
            resp = self.send_request(url)
            data = self.parse(resp)
            self.results.extend(data)
        self.save(self.results)

# 使用示例
class BookSpider(BaseSpider):
    name = 'books'
    start_urls = ['https://books.toscrape.com/']

    def parse(self, response):
        from bs4 import BeautifulSoup
        soup = BeautifulSoup(response.text, 'html.parser')
        items = []
        for book in soup.select('article.product_pod'):
            items.append({
                'title': book.select_one('h3 a')['title'],
                'price': book.select_one('p.price_color').text,
                'stock': book.select_one('p.availability').text.strip()
            })
        return items

    def save(self, data):
        import json
        with open('books.json', 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
```

### 4.2 Scrapy中间件实现代理轮换

```python
# middlewares.py
import random
import base64

class RandomProxyMiddleware:
    """随机代理中间件"""
    def __init__(self, proxy_list):
        self.proxies = proxy_list or []

    @classmethod
    def from_crawler(cls, crawler):
        # 从settings获取代理列表
        return cls(proxy_list=crawler.settings.get('PROXY_LIST', []))

    def process_request(self, request, spider):
        if self.proxies:
            proxy = random.choice(self.proxies)
            request.meta['proxy'] = proxy
            # 如果代理需要认证
            if 'username' in proxy:
                auth = base64.b64encode(
                    f"{proxy['username']}:{proxy['password']}".encode()
                ).decode()
                request.headers['Proxy-Authorization'] = f'Basic {auth}'

    def process_response(self, request, response, spider):
        if response.status in [403, 407, 429]:
            # 代理不可用，移除并重试
            failed_proxy = request.meta.get('proxy')
            if failed_proxy in self.proxies:
                self.proxies.remove(failed_proxy)
            # 更换代理重试
            new_proxy = random.choice(self.proxies) if self.proxies else None
            new_request = request.copy()
            if new_proxy:
                new_request.meta['proxy'] = new_proxy
            return new_request
        return response

# settings.py 配置
# DOWNLOADER_MIDDLEWARES = {
#     'myproject.middlewares.RandomProxyMiddleware': 350,
# }
# PROXY_LIST = ['http://ip1:port', 'http://ip2:port', 'http://ip3:port']
```

### 4.3 自定义去重过滤器（Bloom Filter实现）

```python
import hashlib
import redis
from scrapy.dupefilters import BaseDupeFilter

class BloomDupeFilter(BaseDupeFilter):
    """基于Bloom Filter的Scrapy去重过滤器"""
    def __init__(self, server, key, bit_size=1 << 25, hash_count=6):
        self.server = server
        self.key = key
        self.bit_size = bit_size
        self.hash_count = hash_count

    @classmethod
    def from_settings(cls, settings):
        server = redis.Redis.from_url(settings.get('REDIS_URL', 'redis://localhost:6379'))
        return cls(server, 'bloom:dupefilter')

    def _get_positions(self, fingerprint):
        """计算bit位置"""
        positions = []
        for i in range(self.hash_count):
            seed = str(i).encode()
            hash_val = int(hashlib.md5(fingerprint + seed).hexdigest(), 16)
            positions.append(hash_val % self.bit_size)
        return positions

    def request_seen(self, request):
        fp = self.request_fingerprint(request)
        positions = self._get_positions(fp.encode())
        # 检查是否所有bit都已设置
        exists = all(self.server.getbit(self.key, pos) for pos in positions)
        if not exists:
            # 设置bit
            pipe = self.server.pipeline()
            for pos in positions:
                pipe.setbit(self.key, pos, 1)
            pipe.execute()
        return exists

    def request_fingerprint(self, request):
        """生成请求指纹"""
        fp = hashlib.sha1()
        fp.update(request.url.encode())
        fp.update(request.method.encode())
        # 添加请求体摘要
        if request.body:
            fp.update(request.body)
        return fp.hexdigest()
```

### 4.4 Selenium/Playwright自动化翻页 + 数据提取

```python
# Playwright版本
from playwright.sync_api import sync_playwright
import json

def scroll_infinite_scroll(page_url, max_items=200):
    """处理无限滚动加载的页面"""
    data = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.goto(page_url)

        while len(data) < max_items:
            # 滚动到底部
            page.evaluate('window.scrollTo(0, document.body.scrollHeight)')
            page.wait_for_timeout(2000)  # 等待加载

            # 提取数据
            items = page.evaluate('''() => {
                return Array.from(document.querySelectorAll('.item')).map(el => ({
                    title: el.querySelector('.title')?.innerText,
                    price: el.querySelector('.price')?.innerText,
                    link: el.querySelector('a')?.href
                }));
            }''')
            data.extend(items)
            data = list({v['link']: v for v in data}.values())  # 去重

            # 检测是否到底
            is_bottom = page.evaluate(
                'window.innerHeight + window.scrollY >= document.body.scrollHeight'
            )
            if is_bottom:
                break

        browser.close()

    with open('scroll_data.json', 'w', encoding='utf-8') as f:
        json.dump(data[:max_items], f, ensure_ascii=False, indent=2)
```

### 4.5 模拟登录通用框架

```python
def smart_login(login_url, username, password, extra_fields=None, captcha_func=None):
    """
    通用模拟登录
    :param login_url: 登录接口URL
    :param username: 用户名
    :param password: 密码
    :param extra_fields: 额外表单字段
    :param captcha_func: 验证码处理函数
    :return: requests.Session（已登录）
    """
    session = requests.Session()

    # 先GET获取必要的cookie和token
    pre_resp = session.get(login_url)
    # 提取CSRF Token（常见场景）
    import re
    csrf_token = re.search(r'name="_csrf" value="(.*?)"', pre_resp.text)
    csrf = csrf_token.group(1) if csrf_token else None

    # 构建表单
    form_data = {
        'username': username,
        'password': password,
    }
    if csrf:
        form_data['_csrf'] = csrf
    if extra_fields:
        form_data.update(extra_fields)

    # 处理验证码
    if captcha_func:
        captcha_url = re.search(r'<img.*src="(.*captcha.*)"', pre_resp.text)
        if captcha_url:
            captcha_resp = session.get(captcha_url.group(1))
            with open('captcha.png', 'wb') as f:
                f.write(captcha_resp.content)
            form_data['captcha'] = captcha_func('captcha.png')

    # 提交登录
    login_resp = session.post(login_url, data=form_data)

    if login_resp.ok and '登录成功' in login_resp.text:
        return session
    else:
        raise Exception(f"登录失败：{login_resp.text[:200]}")
```

---

## 五、系统设计题

### 5.1 设计一个高可用分布式爬虫系统

> 阿里/美团面试高频：千万级数据爬取的系统架构设计。

```text
+-------------------------------------------------------------+
|                      调度中心（Scrapyd）                      |
|   +----------+  +----------+  +----------+  +----------+    |
|   | 爬虫节点1 |  | 爬虫节点2 |  | 爬虫节点3 |  | 爬虫节点N |   |
|   +-----+----+  +-----+----+  +-----+----+  +-----+----+   |
|         +--------------+-----------------+                  |
|                        v                                     |
|               +------------------+                           |
|               |  Redis集群        |                          |
|               |  - 请求队列        |                         |
|               |  - 布隆过滤器去重   |                         |
|               |  - 代理池          |                         |
|               +--------+---------+                           |
|                        v                                     |
|                +---------------+                             |
|                |  数据清洗管道   |                            |
|                |  (Kafka+Flink) |                            |
|                +-------+-------+                             |
|                        v                                     |
|                +------------------+                          |
|                |  数据存储         |                          |
|                |  MySQL+MongoDB+HDFS                        |
|                +------------------+                          |
|   +------------------------------------+                    |
|   |  监控告警（Prometheus + Grafana）   |                    |
|   +------------------------------------+                    |
+-------------------------------------------------------------+
```

**核心设计要点：**
1. **请求去重**：Redis Set + Bloom Filter，避免重复爬取
2. **爬虫节点无状态**：任意节点宕机不影响整体任务
3. **代理池自动管理**：验证代理有效性，失败自动剔除
4. **数据管道解耦**：爬虫只负责采集，清洗入队后异步处理
5. **分级存储**：热数据MySQL，文档数据MongoDB，历史数据归档HDFS
6. **链路监控**：爬取成功率/速率/延迟/错误率实时告警

### 5.2 爬虫数据质量控制体系

```text
质量控制流程：
采集层 -> 校验层 -> 清洗层 -> 去重层 -> 存储层

1. 采集层控制：
   - 请求成功率监控（<95%告警）
   - 响应时间监控（超时自动重试）
   - 数据大小校验（空/过小数据丢弃）

2. 校验层控制：
   - 必填字段非空检查
   - 字段格式校验（邮箱/手机号/URL正则）
   - 类型校验（int/float/日期格式）
   - 值域校验（价格>0，日期不超当前时间）

3. 清洗层控制：
   - HTML标签去除
   - 空白字符规范化
   - 编码统一（全部转为UTF-8）
   - 特殊字符转义

4. 去重层控制：
   - URL指纹去重
   - 内容相似度去重（SimHash，海明距离<3视为重复）
   - 时间窗口去重（同内容24小时内不再抓取）

5. 事后监控：
   - 数据量趋势对比（突降说明爬虫失效）
   - 字段缺失率统计
   - 定时抽样人工审核
```

### 5.3 爬虫任务调度与优先级管理

```python
# 设计思路：基于Redis的优先级队列
import redis
import json
from enum import Enum

class TaskPriority(Enum):
    HIGH = 1     # 实时任务（如价格监控）
    NORMAL = 2   # 每日增量更新
    LOW = 3      # 全量历史数据

class SpiderTaskScheduler:
    """爬虫任务调度器"""
    def __init__(self, redis_url='redis://localhost:6379'):
        self.redis = redis.from_url(redis_url)

    def push_task(self, spider_name, params, priority=TaskPriority.NORMAL):
        task = json.dumps({
            'spider': spider_name,
            'params': params,
            'created_at': time.time()
        })
        # 使用有序集合，优先级作为score
        self.redis.zadd('spider:tasks', {task: priority.value})
        print(f"添加任务 [{spider_name}] 优先级 {priority.name}")

    def pop_task(self):
        """获取最高优先级任务"""
        tasks = self.redis.zrange('spider:tasks', 0, 0, withscores=True)
        if tasks:
            self.redis.zrem('spider:tasks', tasks[0][0])
            return json.loads(tasks[0][0])
        return None

    def task_count(self):
        return self.redis.zcard('spider:tasks')

    def clear_completed(self, spider_name):
        """清理已完成任务"""
        # 可移到已完成集合归档
        pass
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点 | 问题描述 | 解决方案 | 代码示例 |
|------|---------|---------|---------|
| requests中文乱码 | 自动检测编码错误，中文显示乱码 | `response.encoding = 'utf-8'`或`response.apparent_encoding` | `resp.encoding = resp.apparent_encoding` |
| IP被限制/封禁 | 频繁请求触发反爬，返回403/429 | 代理IP池 + 随机间隔 + 重试退避 | `time.sleep(random.uniform(1, 3))` |
| SSL证书错误 | 目标网站证书不受信任 | `verify=False` + `urllib3.disable_warnings()` | `requests.get(url, verify=False)` |
| JSON解析异常 | 接口返回的不是标准JSON | 预检查Content-Type + try/except | `if 'json' in resp.headers.get('content-type', ''):` |
| Session登录失效 | 登录后请求仍返回未登录 | 检查Cookie过期时间 + 心跳保活 | 定期重新登录或刷新token |
| 无限滚动加载不全 | Selenium滚动后数据未加载 | 显式等待特定元素出现 | `WebDriverWait(driver, 10).until(...)` |
| 字体反爬乱码 | 显示正常但提取文本为乱码 | 下载字体文件解析映射 | `fontTools.ttLib.TTFont` |
| WebDriver被检测 | 网站识别Selenium并拒绝 | 使用undetected-chromedriver或Playwright | `uc.Chrome()`或`browser.new_context()` |
| Scrapy Item字段缺失 | 部分页面字段为空导致Pipeline报错 | Item中设置default值 | `price = scrapy.Field(default='0')` |
| aiohttp连接数超限 | 并发太高导致连接池耗尽 | 限制`TCPConnector(limit=50)` | `ClientSession(connector=TCPConnector(limit=50))` |
| CSV写入Excel乱码 | 编码问题导致中文乱码 | 使用`utf-8-sig`编码 | `encoding='utf-8-sig'` |
| 内存溢出 | 爬取数据量太大积压内存 | yield逐条处理 + 分批写入 | 使用Scrapy Pipeline逐条处理 |
| 爬虫被重定向到登录页 | Cookie过期或未登录 | 维护登录状态 + 检测重定向URL | `if 'login' in response.url.lower():` |
| 同页面重复请求 | 翻页逻辑重复爬取相同URL | URL指纹去重 | 使用`response.url`做Set去重 |
| XPath/CSS路径失效 | 网站改版导致选择器不匹配 | 使用多备选路径 + 监控告警 | `soup.select_one('.price, .price-tag, [data-price]')` |

---

## 七、面试回答模板（Top 5）

### Q1: 你用Python爬虫做过哪些有挑战的项目？

> 建议按 **STAR原则**（Situation-Task-Action-Result）回答，突出技术深度。

**参考回答：**
"我主导开发过一个电商价格监控系统。项目需要采集5个电商平台（淘宝、京东、拼多多、苏宁、国美）的商品价格数据，每天约500万条记录。主要挑战：1）各平台反爬策略不同（京东的动态token、淘宝的签名算法、拼多多的风控）；2）数据异构标准化；3）每天全量更新需要在2小时内完成。解决方案：底层用了Scrapy-Redis分布式架构，8个爬虫节点并行；针对不同平台定制了中间件（代理轮换、UA池、Cookie池）；数据管道用Kafka+Flink做清洗实时入ES。最终系统爬取成功率达到98.7%，每天采集500万+商品数据。这个项目让我深入掌握了分布式爬虫架构、反爬对抗和数据处理全链路。"

### Q2: 如何处理反爬？请从轻量到重量级详细说明？

> 考查反爬对抗的体系化思维。

**参考回答：**
"我按五个层级来应对：第一层，请求伪装层——随机UA、补充Referer/Origin/Accept等浏览器默认头、使用requests.Session自动管理Cookie。第二层，IP代理层——当某IP请求频率被限制时切换到代理池，代理池需要自动验证有效性、剔除失效IP，同时控制请求间隔（Random Delay 1-3s）。第三层，行为模拟层——滑块验证码需要模拟人类拖动轨迹（加速度变化、抖动），不只是直线移动；带Cookie的Session保持和登录态维护。第四层，浏览器渲染层——遇到JS动态加载的SPA页面，使用Playwright或Selenium处理，同时通过禁用WebDriver检测、注入stealth脚本绕过浏览器指纹识别。第五层，加密逆向层——定位加密参数入口，分析加密算法（MD5/RSA/AES），提取密钥，在Python中复现或用execjs调用原JS。实际项目中，优先使用前两层（成本最低），迫不得已才上浏览器渲染和逆向破解。"

### Q3: Scrapy框架的数据流是如何运转的？

> 考查对Scrapy原理的理解深度。

**参考回答：**
"Scrapy的数据流是一个由Engine驱动的异步循环。流程如下：1）Engine从Spider获取初始Request，发给Scheduler入队；2）Scheduler按优先级出队，交给Downloader Middleware链处理（经过代理、UA等中间件）；3）Downloader执行HTTP请求得到Response，回传经过Downloader Middleware；4）Engine将Response交给Spider Middleware，最终到达Spider的parse方法；5）Spider解析出Item或新的Request，Item经过Item Pipeline（清洗、去重、存储），新Request重新进入Scheduler。整个过程是异步的，Engine不需要等待每个请求完成，可以同时调度多个请求，这是Scrapy高性能的核心。"

### Q4: 大规模爬虫如何保证数据质量？

> 考查数据工程素养。

**参考回答：**
"我采用五层质量控制：第一层校验——数据入库前校验必填字段、字段类型和值域，比如价格必须为正数、URL必须是合法格式。第二层去重——URL级别用布隆过滤器，内容级别用SimHash计算文本相似度（海明距离小于3视为重复），避免重复数据污染。第三层清洗——去除HTML标签、空白符规范化、编码统一转UTF-8。第四层异常处理——异常数据进入死信队列（Dead Letter Queue），人工排查后决定丢弃或修复重跑。第五层监控——每日数据量和趋势对比，如果某天采集量突降70%说明爬虫失效需要告警。定期抽样人工审核数据质量。这套体系保证了在日均500万+数据量下数据质量稳定在98%以上。"

### Q5: JS逆向中定位加密参数的完整思路？

> 考查调试和分析能力。

**参考回答：**
"定位加密参数一般分四步：第一步，全局搜索——在Sources面板中搜索参数名（如sign、token、params、_signature等），快速定位加密函数位置。第二步，XHR断点——给该API请求打XHR Breakpoint（在Network面板中右键请求->Break on->XHR/fetch），触发时在Call Stack中回溯调用链。第三步，条件断点——在可疑函数上打条件断点（如参数包含'encrypt'或返回值包含加密特征字符串），逐步缩小范围。第四步，代码还原——定位到加密函数后，分析其调用了哪些加密库（如crypto-js的MD5/AES/RSA），提取密钥和参数拼接规则。最后用Python重写加密逻辑，或用execjs直接调用原JS函数。平时我也会用Chrome插件Cookie-Editor和Requestly辅助调试和改包。"

---

## 八、快速查漏补缺Checklist

### HTTP与网络请求
- [ ] HTTP请求方法（GET/POST/PUT/DELETE）及使用场景
- [ ] 常见状态码含义（200/301/403/404/429/500/502）
- [ ] 关键请求头（User-Agent/Cookie/Referer/Origin/Authorization）
- [ ] requests全场景用法（params/data/json/files/stream/proxies/timeout）
- [ ] Session与Cookie自动管理机制

### 数据解析
- [ ] 正则表达式元字符与量词
- [ ] 贪婪模式 vs 惰性模式（`.*` vs `.*?`）
- [ ] 分组捕获 `()` 与 `re.findall`/`re.search`
- [ ] BeautifulSoup（find/find_all/select/tag属性）
- [ ] lxml XPath（`//`/`@`/`text()`/`contains`/`starts-with`）
- [ ] PyQuery（CSS选择器语法）
- [ ] 数据编码统一（UTF-8/utf-8-sig/GBK处理）

### 反爬对抗
- [ ] User-Agent伪装与随机UA池
- [ ] 代理IP验证与管理（HTTP/HTTPS/SOCKS5）
- [ ] Referer防盗链处理
- [ ] Cookie有效期维护与刷新
- [ ] 字体反爬破解流程
- [ ] 验证码识别方案选择

### 并发与异步
- [ ] 多线程 vs 多进程 vs 协程选型
- [ ] `ThreadPoolExecutor` 使用
- [ ] `asyncio` 事件循环基础
- [ ] `aiohttp` 异步HTTP请求
- [ ] 协程并发控制（Semaphore限流）
- [ ] 异步爬虫异常处理

### Selenium/Playwright
- [ ] 元素定位策略（CSS/XPath/文本）
- [ ] 显式等待（WebDriverWait）
- [ ] 浏览器反检测（stealth插件）
- [ ] Playwright route网络拦截
- [ ] 截图与PDF导出
- [ ] 无头模式配置

### Scrapy框架
- [ ] Spider核心方法（start_requests/parse/follow）
- [ ] Item定义与Field类型
- [ ] Item Pipeline数据存储
- [ ] Downloader Middleware编写
- [ ] Spider Middleware编写
- [ ] Settings配置（并发/延迟/重试/缓存）
- [ ] Scrapy-Redis分布式配置
- [ ] Scrapyd部署

### JS逆向基础
- [ ] MD5特征识别与Python实现
- [ ] AES-CBC/ECB模式与密钥定位
- [ ] RSA公钥提取与加密
- [ ] Base64编解码
- [ ] Webpack打包的模块还原
- [ ] Chrome DevTools断点调试
- [ ] execjs/PyExecJS调用原生JS

### 数据存储
- [ ] CSV读写（DictWriter/utf-8-sig）
- [ ] JSON序列化（ensure_ascii=False）
- [ ] MySQL连接池（pymysql/连接参数）
- [ ] MongoDB插入与索引（insert_many/create_index）
- [ ] 数据去重（Set/Bloom Filter/SimHash）
- [ ] 数据清洗流程（校验/清洗/归档）

### 工程化与部署
- [ ] Pipenv/Conda虚拟环境管理
- [ ] 配置与代码分离（settings.py/environ）
- [ ] 日志分级与存储（logging模块）
- [ ] Scrapyd远程部署
- [ ] Docker容器化爬虫
- [ ] 定时任务（Crontab/Airflow）
- [ ] 异常告警（钉钉/企业微信webhook）

---

> 🎯 **总结**：Python爬虫面试的核心竞争力在于 **体系化反爬对抗能力** + **大规模工程化经验** + **JS逆向分析深度**。建议在简历中突出1-2个有技术深度的爬虫项目，从系统架构（分布式）、反爬策略（多层应对）、数据质量（全链路控制）三个维度展开说明。代码题优先掌握：通用爬虫基类封装、异步协程爬虫、Scrapy中间件、JS解密还原。
