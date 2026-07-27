# 03 - BeautifulSoup 与 HTML 解析

> 🎯 获取 HTML 只是第一步，从中提取结构化数据才是核心。本文覆盖 BS4 + XPath + 正则三套解析方案及其各自的最佳场景

---

## 目录

1. [BeautifulSoup 核心 API](#1-beautifulsoup-核心-api)
2. [CSS 选择器实战](#2-css-选择器实战)
3. [XPath 与 lxml](#3-xpath-与-lxml)
4. [正则表达式辅助提取](#4-正则表达式辅助提取)
5. [解析策略选择](#5-解析策略选择)

---

## 1. BeautifulSoup 核心 API

### 1.1 初始化与解析器选择

```python
from bs4 import BeautifulSoup

html = "<html><body><p class='title'>Hello</p></body></html>"

# 解析器选择
soup = BeautifulSoup(html, "html.parser")     # 内置，无依赖
soup = BeautifulSoup(html, "lxml")            # 最快，需 pip install lxml
soup = BeautifulSoup(html, "html5lib")        # 最宽容，像浏览器一样解析
```

| 解析器 | 速度 | 容错性 | 依赖 |
|------|:---:|:---:|------|
| `html.parser` | ⭐⭐ | ⭐⭐ | 无（内置） |
| `lxml` | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | `pip install lxml` |
| `html5lib` | ⭐ | ⭐⭐⭐⭐⭐ | `pip install html5lib` |

> 🎯 推荐：开发用 `lxml`（速度最快），遇到不规范 HTML 用 `html5lib`

### 1.2 核心查找方法

```python
soup = BeautifulSoup(html, "lxml")

# ---- 单元素查找 ----
soup.find("div")                    # 第一个 <div>
soup.find("div", class_="content")  # class="content" 的第一个 <div>
soup.find("a", href=True)           # 包含 href 属性的第一个 <a>
soup.find(id="main")                # id="main" 的元素

# ---- 多元素查找 ----
soup.find_all("a")                  # 所有 <a> 标签
soup.find_all("li", class_="item")  # class="item" 的所有 <li>
soup.find_all("div", limit=10)      # 前 10 个 <div>

# ---- 导航 ----
tag.parent                          # 父元素
tag.children                        # 直接子元素（生成器）
tag.find_next("div")                # 下一个 <div> 兄弟
tag.find_previous("div")            # 上一个 <div> 兄弟
```

### 1.3 内容提取

```python
tag = soup.find("div", class_="content")

tag.text        # 所有文本内容（包括子元素）
tag.string      # 直接文本内容（无子元素时）
tag.get("href") # 属性值
tag["href"]     # 同上，但属性不存在会报 KeyError
tag.attrs       # 所有属性字典
```

## 2. CSS 选择器实战

### 2.1 select() 方法

```python
# 通用选择器
soup.select("div")                   # 所有 <div>
soup.select("#main")                 # id="main"
soup.select(".item")                 # class="item"
soup.select("div.content")           # <div class="content">

# 层级选择
soup.select("div > p")               # div 的直接子 p
soup.select("div p")                 # div 的所有后代 p
soup.select("div + p")               # div 后紧邻的 p

# 属性选择
soup.select("a[href]")               # 有 href 属性的 a
soup.select("a[href$='.pdf']")       # href 以 .pdf 结尾
soup.select("a[href^='https']")      # href 以 https 开头
soup.select("a[href*='example']")    # href 包含 example

# 伪类选择
soup.select("li:nth-of-type(1)")     # 第一个 <li>
soup.select("li:nth-of-type(2n)")    # 偶数 <li>
soup.select("p:not(.exclude)")       # 不含 class="exclude" 的 p
```

### 2.2 实战：提取列表数据

```python
# HTML 结构示例
# <div class="product-list">
#   <div class="product">
#     <h3 class="name">商品A</h3>
#     <span class="price">¥99</span>
#     <a class="link" href="/item/1">详情</a>
#   </div>
#   ...
# </div>

def extract_products(html: str) -> list[dict]:
    soup = BeautifulSoup(html, "lxml")
    products = []

    for item in soup.select(".product"):
        name_tag = item.select_one(".name")
        price_tag = item.select_one(".price")
        link_tag = item.select_one(".link")

        products.append({
            "name": name_tag.text.strip() if name_tag else "",
            "price": price_tag.text.strip() if price_tag else "",
            "url": link_tag.get("href") if link_tag else ""
        })
    return products
```

## 3. XPath 与 lxml

### 3.1 XPath 速查

```python
from lxml import etree

# 从 HTML 字符串解析
tree = etree.HTML(html)

# XPath 选择
tree.xpath("//div")                       # 所有 <div>
tree.xpath("//div[@class='content']")     # class="content" 的 div
tree.xpath("//a/@href")                   # 所有 <a> 的 href 属性
tree.xpath("//div[@id='main']//p")        # id="main" 内的所有 <p>
tree.xpath("//h1/text()")                 # 所有 <h1> 的文本
tree.xpath("//li[1]")                     # 第一个 <li>（XPath 索引从 1 开始！）
tree.xpath("//a[contains(@href, 'pdf')]") # href 包含 "pdf"
```

### 3.2 BS4 vs XPath 选择

```python
# 同一选择，两种写法对比

# CSS (BS4)
soup.select("div.product > h3.name")

# XPath (lxml)
tree.xpath("//div[@class='product']/h3[@class='name']")

# CSS 更简洁但表达能力有限
# XPath 更强大（轴选择、条件筛选、函数），但语法稍复杂
```

## 4. 正则表达式辅助提取

### 4.1 从 HTML 中提取

```python
import re

# 提取所有邮箱
emails = re.findall(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}', html)

# 提取所有手机号（中国）
phones = re.findall(r'1[3-9]\d{9}', html)

# 提取 JSON 数据
json_data = re.search(r'var data = ({.*?});', html).group(1)

# 提取 script 标签中的变量
pattern = r'window\.__INITIAL_STATE__\s*=\s*({.*?});'
match = re.search(pattern, html, re.DOTALL)
```

### 4.2 正则 vs 解析器

| 场景 | 推荐工具 | 原因 |
|------|---------|------|
| 提取 HTML 结构化数据 | BS4 / XPath | HTML 不是正则语言 |
| 从 JS 变量中提取 JSON | 正则 | 不是 HTML 标签 |
| 提取邮箱/手机/URL | 正则 | 模式匹配，非结构依赖 |
| 提取特定格式字符串 | 正则 | 最灵活 |

> ⚠️ **Golden Rule**：不要用正则解析 HTML 结构（`<div>.*?</div>`），用 BS4/XPath。正则只用于**提取文本模式**（邮箱、JSON 块等）

## 5. 解析策略选择

```text
你的数据在哪？
├── HTML 标签中（结构化表格/列表）
│   └── BS4 select() 或 XPath（推荐 BS4，语法更友好）
│
├── JavaScript 变量中（SPA 页面）
│   └── 正则提取 + json.loads()
│
├── JSON-LD / Schema.org
│   └── BS4 select('script[type="application/ld+json"]') + json.loads()
│
├── API 接口返回的 JSON
│   └── 直接 resp.json()，不需要 HTML 解析！
│
├── 纯文本模式（邮箱/电话/日期）
│   └── 正则表达式
│
└── 复杂嵌套 / 不规范的 HTML
    └── html5lib 解析器 + BS4
```

## 核心要点回顾

- BS4 推荐解析器：`lxml`（最快）+ `html5lib`（最宽容）
- `select()` vs `find_all()`：select 支持 CSS 选择器，更直观
- XPath 三大优势：轴选择、内置函数（contains/text()）、性能
- **不要用正则解析 HTML 结构**——正则只用于文本模式提取
- 优先查 API 接口（JSON 格式），其次才是解析 HTML

## 参考资料

1. BeautifulSoup 官方文档 — crummy.com/software/BeautifulSoup
2. lxml XPath 教程 — lxml.de
3. CSS Selector Reference — MDN
