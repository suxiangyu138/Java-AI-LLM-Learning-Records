# lxml 与 XPath 解析
> lxml 原生 API、XPath 提取实战、性能对比与 bs4 协同——"CSS 搞不定时，XPath 上"

## 📚 目录
1. [lxml 定位：bs4 的引擎也是独立武器](#1-lxml-定位bs4-的引擎也是独立武器)
2. [lxml 基础用法](#2-lxml-基础用法)
3. [XPath 提取实战](#3-xpath-提取实战)
4. [性能对比：什么时候用 lxml](#4-性能对比什么时候用-lxml)
5. [bs4 + lxml 协同模式](#5-bs4--lxml-协同模式)

## 1. lxml 定位：bs4 的引擎也是独立武器

| 事实 | 说明 |
|------|------|
| 版本 | lxml 5.x（2026） |
| 双重身份 | ① bs4 的高速解析后端 ② 独立的高性能解析库（XPath） |
| 核心能力 | **XPath**（bs4 没有）+ 极快解析 |
| 定位 | 大规模数据 / XPath 场景 / 生产性能 |

```text
选择矩阵（结合阶段 1 §4）：
  CSS 选择器能解决 80% 场景 → bs4 select（简单可读）
  XPath 独有能力（文本/父级/动态 class）→ lxml
  海量页面（性能敏感）→ lxml（快 5-10 倍）
```

> 🎯 学习策略：**bs4 做日常、lxml 做攻坚**——两个都会，按场景选；本文件把 lxml 路线讲透。

## 2. lxml 基础用法

```python
from lxml import etree

# ═══ 解析 HTML ═══
tree = etree.HTML(html_text)          # 容错解析（自动补全标签）
# 或 fromstring（严格模式，XML 用）
# tree = etree.fromstring(xml_text)

# ═══ 基础导航 ═══
tree.xpath("//title/text()")          # 页面标题
tree.xpath("//a/@href")               # 所有链接
tree.xpath("//div[@class='card']")    # 按 class 找元素

# ═══ 序列化（转回字符串调试）═══
etree.tostring(tree, encoding="unicode")   # 看修复后的 HTML
```

| lxml 核心对象 | 说明 |
|-------------|------|
| `etree.HTML(html)` | 容错解析 HTML（补全/修复坏标签） |
| `etree.fromstring(xml)` | 严格解析 XML |
| `Element` | 元素对象（`xpath`/`find`/`.text`/`.get()`） |
| `ElementTree` | 文档（少见直接用） |

> 💡 lxml 是 **C 实现的解析器**（libxml2 绑定）——解析速度是纯 Python 的 bs4 数倍；代价是安装有二进制依赖（pip 已提供 wheel，基本无感）。

## 3. XPath 提取实战

```python
from lxml import etree

tree = etree.HTML(html)

# ═══ 文本与属性（最常用）═══
tree.xpath('//h2[@class="title"]/text()')          # 所有标题文本
tree.xpath('//a[contains(@class,"news-title")]/@href')   # 链接（动态 class 匹配）
tree.xpath('//span[@class="date"]/text()')         # 日期

# ═══ 文本定位（CSS 做不到）═══
tree.xpath('//*[text()="热门新闻"]')               # 按文本精确找
tree.xpath('//p[contains(text(),"关键词")]')        # 文本包含

# ═══ 相对定位：从容器出发 ═══
container = tree.xpath('//ul[contains(@class,"news-list")]')[0]
container.xpath('.//a/text()')                     # 容器内所有 a 文本
container.xpath('.//a/@href')
container.xpath('.//span[@class="date"]/text()')

# ═══ 轴：上下左右 ═══
tree.xpath('//h2[@class="title"]/following-sibling::span/text()')   # 标题后的日期
tree.xpath('//a[@class="more"]/parent::div')       # 链接的父容器
tree.xpath('//div[contains(@class,"item")]/preceding-sibling::h2')  # 前面的标题

# ═══ 条件与序号 ═══
tree.xpath('//ul[@class="list"]/li[1]')            # 第一个 li（⚠️ XPath 从 1 开始）
tree.xpath('//ul[@class="list"]/li[last()]')       # 最后一个
tree.xpath('//li[position()>1]')                   # 除第一个外
tree.xpath('//a[@href and @target]')               # 多条件
```

| XPath 表达式 | 含义 |
|-------------|------|
| `//tag` | 任意位置的所有 tag |
| `//tag/@attr` | 提取属性值 |
| `//tag/text()` | 提取文本 |
| `[@class="x"]` | 属性精确 |
| `[contains(@class,"x")]` | **动态 class 模糊匹配**（CSS 难做） |
| `[text()="x"]` / `[contains(text(),"x")]` | **按文本定位**（CSS 做不到） |
| `.//` | 当前节点相对路径 |
| `following-sibling::` / `preceding-sibling::` / `parent::` | 轴 |
| `[1]` / `[last()]` | 位置（从 1 开始） |

> ⚠️ 与 Python 索引的差异：**XPath 的 `[1]` 是第一个**（Python 列表是 `[0]`）——`xpath('//li[1]')` 与 `find_all('li')[0]` 等价。混用两种语法时最容易搞混。

## 4. 性能对比：什么时候用 lxml

```text
性能实测规律（量级参考）：
  bs4 + html.parser   ：1x（最慢）
  bs4 + lxml 后端     ：3-5x
  lxml 原生 XPath     ：5-10x（C 实现）

结论：
  页面少（<1000）        → bs4 足够（可读性优先）
  页面多/生产性能敏感     → lxml 原生（速度优先）
  需要 XPath 独有能力    → lxml（别无选择）
```

| 场景 | 推荐 | 原因 |
|------|------|------|
| 学习/小爬虫 | bs4 + lxml 后端 | 可读、容错 |
| 大规模采集 | lxml 原生 | 5-10 倍速度 |
| 动态 class/文本定位 | lxml XPath | 能力需求 |
| 混合 | bs4 解析 + lxml 后端 | 简单与性能兼顾 |

> 🎯 性能决策：**"性能瓶颈出现前用 bs4（代码可读），出现后用 lxml（换引擎或换 API）"**——阶段 6 异步化后每页节省的时间 × 并发数 = 巨大收益，那时 lxml 是标配。

## 5. bs4 + lxml 协同模式

```python
# ═══ 模式一：bs4 解析 + lxml 引擎（默认组合）═══
from bs4 import BeautifulSoup
soup = BeautifulSoup(html, "lxml")      # 简单场景：bs4 API + lxml 速度

# ═══ 模式二：lxml 为主 + 需要时转 bs4 ═══
from lxml import etree
from bs4 import BeautifulSoup

tree = etree.HTML(html)
# XPath 拿容器 HTML → 转 bs4 继续处理
items_html = etree.tostring(tree.xpath('//ul[@class="list"]')[0], encoding="unicode")
soup = BeautifulSoup(items_html, "lxml")     # 片段转 bs4
for item in soup.select("li a.title"):
    print(item.text)

# ═══ 模式三：统一用 lxml（生产性能路线）═══
def parse_news_list(html_text):
    """生产标准：lxml XPath 一条龙"""
    tree = etree.HTML(html_text)
    items = tree.xpath('//ul[contains(@class,"news-list")]/li')
    results = []
    for item in items:
        title = item.xpath('.//a[contains(@class,"title")]/text()')
        url = item.xpath('.//a[contains(@class,"title")]/@href')
        date = item.xpath('.//span[contains(@class,"date")]/text()')
        if title:
            results.append({
                "title": title[0].strip(),
                "url": url[0] if url else "",
                "date": date[0].strip() if date else "",
            })
    return results
```

> 🎯 协同结论：**"XPath 定位容器，bs4 处理细节"或"lxml 一条龙"**——两种模式都生产可用；选型看团队熟悉度。XPath 的 `text()`/`@href` 提取天然返回列表，判空规则：`title[0] if title else ""`。

---

**下一模块**：[05-数据提取与清洗](05-数据提取与清洗.md) / **返回总览**：[00-阶段2静态网页爬虫总览](00-阶段2静态网页爬虫总览.md)
