# 爬虫 (Web Scraper)

> Python 网络爬虫实战练习，从 requests 基础到 Scrapy 框架

## 项目概述

Python 爬虫技术系统性实践项目，从 HTTP 请求基础开始，逐步学习页面解析、数据提取、反爬对抗、分布式爬虫等进阶技术。涵盖静态页面爬取、动态渲染页面处理、API 数据抓取等多种场景。

## 技术栈

| 技术 | 说明 |
|------|------|
| Python | 3.10+ |
| Requests | HTTP 请求库 |
| BeautifulSoup4 / lxml | HTML/XML 解析 |
| Selenium / Playwright | 动态页面渲染 |
| Scrapy | 爬虫框架 |
| parsel / XPath / CSS Selector | 数据提取 |
| json / csv / pandas | 数据存储 |

## 学习路线

### 第一阶段：HTTP 基础
- HTTP 请求方法（GET/POST/PUT/DELETE）
- Headers、Cookies、Session
- URL 参数与表单提交
- 状态码与异常处理

### 第二阶段：页面解析
- BeautifulSoup 解析 HTML
- XPath 表达式（lxml）
- CSS Selector 选择器
- 正则表达式辅助提取

### 第三阶段：数据持久化
- 保存为 JSON / CSV 文件
- 写入 MySQL / MongoDB 数据库
- 下载图片/文件（二进制流处理）

### 第四阶段：反爬对抗
- User-Agent 轮换
- IP 代理池
- 请求频率控制（time.sleep / 随机延迟）
- Cookie 池管理
- 验证码识别基础

### 第五阶段：框架化爬虫
- Scrapy 项目结构
- Spider / Item / Pipeline
- 中间件编写
- 分布式爬虫（Scrapy-Redis）

## 项目结构

```
爬虫/
├── src/
│   ├── basic/
│   │   ├── requests_demo.py         # Requests 基础
│   │   └── bs4_demo.py              # BeautifulSoup 解析
│   ├── parser/
│   │   ├── xpath_parser.py          # XPath 提取
│   │   └── css_parser.py            # CSS Selector 提取
│   ├── dynamic/
│   │   └── selenium_demo.py         # Selenium 动态渲染
│   ├── anti_spider/
│   │   ├── proxy_pool.py            # 代理池
│   │   └── ua_rotation.py           # UA 轮换
│   └── scrapy_project/             # Scrapy 项目
├── data/                            # 抓取数据存储
├── requirements.txt
├── main.py
└── README.md
```

## 快速开始

```bash
# 安装依赖
pip install -r requirements.txt

# 运行基础爬虫示例
python main.py

# 创建 Scrapy 项目
scrapy startproject my_spider
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| requests 库 | HTTP 请求发送、参数传递、异常处理 |
| BeautifulSoup4 | DOM 树解析、元素定位、属性提取 |
| XPath | 节点选择、条件筛选、轴定位 |
| Selenium | 浏览器自动化、等待策略、JS 执行 |
| 反爬对抗 | IP 代理、UA 轮换、Cookie 管理、验证码 |
| Scrapy | 引擎架构、Pipeline 数据管道、中间件 |
| 数据存储 | JSON/CSV 文件、MySQL/MongoDB |

## 推荐练习目标

- 豆瓣电影 Top250 榜单
- 拉勾/Boss 直聘 职位信息
- 微博/知乎 热榜数据
- 京东/淘宝 商品搜索
- GitHub Trending 仓库

## 注意事项

- **遵守 robots.txt 协议**，不爬取禁爬页面
- **控制请求频率**，避免给目标服务器造成过大压力
- **遵守法律法规**，不爬取个人信息、版权内容等受保护数据
- 注意分辨静态页面和动态渲染页面，选择合适的工具
- 爬取数据仅供学习用途，勿用于商业目的
