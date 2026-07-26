# Python 爬虫实战项目清单

## 一、核心能力

Python 爬虫完整技能栈：

- 静态页面抓取
- 动态页面渲染
- 数据解析与清洗
- 反爬对抗
- 异步并发
- 数据持久化
- 分布式爬虫

---

## 二、技术能力拆解

- 请求层：HTTP / HTTPS / User-Agent / Cookie / Session
- 解析层：BeautifulSoup / lxml / XPath / CSS Selector
- 动态层：Selenium / Playwright / JS 渲染
- 反爬层：代理 IP / 验证码识别 / 请求头伪装 / 限速
- 并发层：多线程 / 多进程 / 异步 IO / 协程
- 存储层：CSV / JSON / MySQL / MongoDB / Redis
- 框架层：Scrapy / Scrapy-Redis / 中间件 / 管道

---

## 三、必做项目（求职优先级）

### 1. 静态网页单页爬取
**技术栈**：requests + BeautifulSoup4

**实现**：
- HTTP GET 请求
- 请求头伪装
- User-Agent 设置
- HTML 解析
- 标签定位
- 文本 / 属性提取
- 数据保存到 CSV

**核心知识点**：
- requests 基础用法
- BeautifulSoup 解析
- 基础反爬

**项目场景**：
- 豆瓣电影
- 简书文章
- 新闻资讯
- 图书信息

**产出**：
- 单页数据抓取能力

**耗时**：1~2天

---

### 2. 分页批量爬取
**技术栈**：requests + BeautifulSoup4

**实现**：
- 分页 URL 规律分析
- 循环翻页
- 批量数据存储
- 异常捕获
- 进度记录
- 断点续爬

**核心知识点**：
- 分页逻辑
- 循环控制
- 异常处理

**项目场景**：
- 多页商品列表
- 多页影评
- 多页文章

**产出**：
- 批量数据采集能力

**耗时**：2天

---

### 3. XPath / CSS 选择器精准爬取
**技术栈**：requests + lxml

**实现**：
- XPath 语法
- CSS 选择器
- 节点遍历
- 多条件筛选
- 模糊匹配

**核心知识点**：
- XPath 语法
- CSS Selector
- 精准定位

**项目场景**：
- 知乎问题
- B 站视频标题
- 电商商品详情

**产出**：
- 精准数据提取能力

**耗时**：2天

---

### 4. 爬虫数据持久化
**技术栈**：requests + pymysql / sqlite3

**实现**：
- 数据库建表
- 数据插入
- 批量写入
- 字段清洗
- 数据去重
- 异常重试

**核心知识点**：
- MySQL 连接
- SQL 语句
- 数据清洗

**项目场景**：
- 结构化数据存储

**产出**：
- 数据持久化能力

**耗时**：2~3天

---

### 5. Selenium 模拟浏览器爬取
**技术栈**：Selenium + ChromeDriver

**实现**：
- JS 渲染页面
- 元素定位
- 隐式 / 显式等待
- 页面滚动
- 点击 / 输入 / 下拉框操作
- 无头浏览器配置
- 截图保存

**核心知识点**：
- Selenium WebDriver
- 元素定位策略
- 等待机制

**项目场景**：
- 淘宝
- 微博
- 小红书
- 动态加载数据

**产出**：
- 动态页面抓取能力

**耗时**：3天

---

### 6. 异步并发爬虫
**技术栈**：aiohttp + asyncio

**实现**：
- 异步 IO
- 协程
- 并发请求
- 信号量控制并发数
- 异常捕获
- 批量任务调度

**核心知识点**：
- asyncio
- 协程概念
- 并发控制

**项目场景**：
- 大规模批量数据抓取
- 多站点同时爬取

**产出**：
- 高性能爬虫能力（5~10 倍提速）

**耗时**：3~4天

---

### 7. Scrapy 框架实战
**技术栈**：Scrapy

**实现**：
- Scrapy 项目结构
- Spider 编写
- Item 定义
- Pipeline 数据处理
- 中间件配置
- 自动去重
- 请求调度
- 限速配置

**核心知识点**：
- Scrapy 架构
- 中间件
- 管道
- 调度器

**项目场景**：
- 综合性爬虫项目
- 多站点协同爬取

**产出**：
- 工程化爬虫能力

**耗时**：4~5天

---

## 四、进阶项目（提升上限）

### 8. Playwright 高性能动态爬虫
**技术栈**：Playwright

**实现**：
- 自动等待
- 网络拦截
- 异步执行
- 跨浏览器兼容
- 防检测配置

**项目场景**：
- 高反爬动态网站
- SPA 单页应用

**产出**：
- 稳定高效的动态爬虫

---

### 9. 验证码识别实战
**技术栈**：ddddocr / pytesseract

**实现**：
- 图片验证码识别
- 滑块验证码破解
- 打码平台对接
- 自动登录

**项目场景**：
- 登录页验证码
- 注册页验证码

**产出**：
- 自动化绕过验证码能力

---

### 10. 代理 IP 池搭建
**技术栈**：requests + 代理服务

**实现**：
- 代理 IP 使用
- IP 有效性检测
- 随机切换代理
- 防 IP 封禁
- 代理池管理

**项目场景**：
- 高频爬取
- 大规模数据采集

**产出**：
- 简易代理池 + IP 轮换机制

---

### 11. Cookie / Session 会话保持
**技术栈**：requests.Session

**实现**：
- 会话维持
- 登录态保持
- Cookie 复用
- 模拟登录
- 自动保存登录凭证

**项目场景**：
- 需要登录的内容
- 个人中心数据

**产出**：
- 免重复登录的持久化爬虫

---

### 12. 多线程 / 多进程爬虫
**技术栈**：threading / multiprocessing

**实现**：
- 线程池
- 进程池
- 任务队列
- 线程安全
- 锁机制

**项目场景**：
- 海量数据快速采集

**产出**：
- 多任务并行爬虫框架

---

### 13. 分布式爬虫 Scrapy-Redis
**技术栈**：Scrapy + Redis

**实现**：
- 分布式任务调度
- 共享请求队列
- 多机协同爬取
- 断点续爬
- 去重管理

**项目场景**：
- 超大规模数据采集
- 全站抓取

**产出**：
- 分布式爬虫集群方案

---

## 五、必练核心知识点

### 1. requests 基础
```python
import requests

headers = {'User-Agent': 'Mozilla/5.0'}
response = requests.get(url, headers=headers)
html = response.text
```

### 2. BeautifulSoup 解析
```python
from bs4 import BeautifulSoup

soup = BeautifulSoup(html, 'html.parser')
title = soup.find('h1').text
items = soup.find_all('div', class_='item')
```

### 3. XPath
```python
from lxml import etree

tree = etree.HTML(html)
title = tree.xpath('//h1/text()')
items = tree.xpath('//div[@class="item"]')
```

### 4. Selenium
```python
from selenium import webdriver

driver = webdriver.Chrome()
driver.get(url)
element = driver.find_element_by_id('id')
element.click()
```

### 5. 异步并发
```python
import aiohttp
import asyncio

async def fetch(url):
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            return await response.text()

asyncio.run(fetch(url))
```

---

## 六、项目架构标准

必须覆盖：

- 请求头伪装
- 异常捕获与重试
- 数据清洗与去重
- 数据持久化
- 日志记录
- 并发控制
- 反爬处理
- 代码模块化

---

## 七、简历表达（核心关键词）

- Python 爬虫全链路开发
- requests / BeautifulSoup / lxml 数据解析
- Selenium / Playwright 动态页面抓取
- 异步并发爬虫性能优化
- 代理 IP 池与反爬对抗
- Scrapy 工程化爬虫框架
- Scrapy-Redis 分布式爬虫
- MySQL / MongoDB 数据持久化

---

## 八、技术栈推荐

- 静态爬取：requests / BeautifulSoup4 / lxml
- 动态爬取：Selenium / Playwright
- 异步并发：aiohttp / asyncio
- 数据存储：MySQL / MongoDB / CSV / Redis
- 反爬对抗：代理 IP / ddddocr / fake-useragent
- 框架工程化：Scrapy / Scrapy-Redis

---

## 九、爬虫合规说明

**重要提醒**：

- 严格遵守网站 robots.txt 协议
- 仅用于学习研究
- 不可用于商业用途
- 不得恶意爬取
- 不得泄露他人隐私
- 控制请求频率，避免对服务器造成压力

---

## 十、项目成果要求

- GitHub 完整代码（结构清晰）
- README 写明：爬取目标 / 技术栈 / 反爬方案 / 数据结构 / 使用说明
- 代码规范：模块化 / 异常捕获 / 日志记录
- 至少 1 个可运行 Demo
- 必含能力：反爬处理 + 并发优化 + 数据存储
- 突出工程化与性能优化

---
