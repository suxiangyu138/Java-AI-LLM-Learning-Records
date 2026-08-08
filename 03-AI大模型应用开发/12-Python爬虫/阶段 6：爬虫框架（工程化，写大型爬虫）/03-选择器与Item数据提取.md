# 03 选择器与 Item 数据提取
> 数据质量第一关：Selector 提取规范 + Item 三形态统一模型 + 字段级校验

## 📚 目录
1. [Selector：框架内置解析层](#1-selector框架内置解析层)
2. [CSS 选择器与 Scrapy 扩展语法](#2-css-选择器与-scrapy-扩展语法)
3. [XPath：文本定位与复杂提取](#3-xpath文本定位与复杂提取)
4. [提取工程规范：从"能取到"到"取得好"](#4-提取工程规范从能取到到取得好)
5. [Item 三种形态与统一模型](#5-item-三种形态与统一模型)
6. [字段校验与清洗](#6-字段校验与清洗)
7. [FEEDS 输出与落盘](#7-feeds-输出与落盘)
8. [核心要点](#8-核心要点)

---

## 1. Selector：框架内置解析层

Scrapy 的 `response.css()/.xpath()` 返回 **SelectorList**（由 parsel 库实现，lxml 之上）。它与阶段 2 的 BeautifulSoup 解决同一问题，但为框架场景优化：

| 维度 | BeautifulSoup（阶段 2） | Scrapy Selector |
|------|------------------------|-----------------|
| 依赖 | bs4 + 解析器 | lxml（C 实现，快 5-10 倍） |
| 语法 | find/find_all + select | css()/xpath() + re() 三合一 |
| 提取粒度 | Tag 对象 | Selector（.get()/.getall()） |
| 容错 | 需要判空三连 | .get() 天然 None 安全 |
| 组合 | 手动嵌套 | `.css(...).xpath(...).get()` 链式 |
| 与框架集成 | 无（需自己包装） | 响应自带，无需 import |

**API 速查**：

```python
# 单值（不存在返回 None，不会抛异常）
title = response.css("h1::text").get()          # 等价 .extract_first()
# 多值
texts = response.xpath("//p/text()").getall()   # 等价 .extract()
# 属性
href = response.css("a::attr(href)").get()
# 正则提取（SelectorList 自带）
ids = response.css("div.item").re(r"id=(\d+)")
# 链式组合
name = response.xpath("//div[@class='author']").css("span::text").get()
# 子选择器（迭代）
for sel in response.css("div.quote"):
    text = sel.css("span.text::text").get()
```

> 💡 与 bs4 对照记忆：`.get()` ≈ `find(...).text`（但 None 安全）、`.getall()` ≈ `find_all(...)` 取全部文本、`::text` ≈ 手动遍历 `.strings`。Selector 的"**链式 + None 安全 + 正则内建**"让解析代码短一半。

## 2. CSS 选择器与 Scrapy 扩展语法

**标准 CSS**（阶段 2 已学）之外，Scrapy 增加三个扩展：

| 扩展语法 | 含义 | 示例 |
|---------|------|------|
| `::text` | 取**文本节点**（不是元素） | `h1::text` → "标题" |
| `::attr(name)` | 取属性值 | `a::attr(href)` → URL |
| `::attr(name)` 链式 | 组合属性与文本 | `span::attr(data-id)` |

```python
# 完整示例：提取卡片
quote = response.css("div.quote")[0]
{
    "text": quote.css("span.text::text").get(),          # 文本
    "author": quote.css("small.author::text").get(),
    "link": quote.css("span > a::attr(href)").get(),      # 属性
    "tags": quote.css("div.tags a.tag::text").getall(),   # 多值
}
```

> ⚠️ 伪类注意：Scrapy 的 CSS 支持 `:nth-child`、`::first-child` 等结构伪类，但**不支持 CSS3 的 `:contains()`**——需要"按文本筛选"用 XPath 的 `contains(text(), ...)`（§3），这是新手最常见的"CSS 取不到"原因。

## 3. XPath：文本定位与复杂提取

### 3.1 核心用法（阶段 2 XPath 基础回顾 + 框架增强）

```python
# 按文本模糊匹配（CSS 做不了的活）
item = response.xpath("//div[contains(text(), '热卖')]")
# 按属性包含
card = response.xpath("//div[contains(@class, 'product-card')]")
# 轴：兄弟/父级（兄弟价格定位）
price = response.xpath("//span[@class='name']/following-sibling::span[@class='price']/text()")
# 多条件
el = response.xpath("//a[@href and @data-id]")
# 索引（XPath 从 1 开始）
first = response.xpath("//ul/li[1]")
# 正则（XPath 1.0 无正则，框架用 re() 补）
ids = response.xpath("//div/@data-id").re(r"\d+")
```

### 3.2 文本提取三件套（工程高频）

```python
# ① 提取并去空白
text = response.xpath("string(//h1)").get()          # 取整个子树文本
text = " ".join(response.xpath("//p/text()").getall()).strip()  # 拼接段落
# ② 提取并 strip
clean = sel.css("span::text").get().strip() if sel.css("span::text").get() else ""
# ③ 防御式链式
title = response.xpath("//title/text()").get() or response.css("h1::text").get() or "UNKNOWN"
```

> 🎯 **核心要点**：工程里 80% 的提取是 `::text` + `::attr` + `.get()` + `.getall()` 四个 API；**XPath 只在 CSS 表达不了（按文本/轴/复杂逻辑）时用**。别把 XPath 当首选，把 CSS 当首选——代码可读性差一个量级。

## 4. 提取工程规范：从"能取到"到"取得好"

| 规范 | 反例 | 正例 |
|------|------|------|
| 优先容器先行 | `response.css("div.quote span.text::text")` | 先取 `div.quote` 再取子元素（迭代单元清晰） |
| 稳定锚点 | `//div[2]/p[1]`（位置易变） | 用 class/data-* 等语义锚点 |
| 必填字段兜底 | `.get()` 返回 None 直接入库 | 写默认值或标记异常（§6 校验） |
| 清洗后置 | 解析里写一堆 replace | 解析只取"原始值"，清洗统一放 Pipeline（04 章） |
| 编码假设 | 直接 `.get()` 中文乱码 | 响应编码由 Downloader 处理；乱码时 05 章处理法 |
| 空容器 | 循环取不到任何 item 无感知 | `item_scraped_count == 0` 视为异常（08 章监控） |

> 💡 **与阶段 2 的分工**：阶段 2 学的是"解析原理"（bs4 练手），本阶段学"框架内提取规范"——Selector 只是工具，**真正的增量是"提取即数据契约"**：Spider 的 yield 结构 = 数据表结构，字段名统一在 items 层定义（§5），别在 parse 里随手 dict。

## 5. Item 三种形态与统一模型

### 5.1 三种写法对比

```python
# 形态一：纯 dict（最轻，示例常用）
yield {"title": "...", "url": "..."}

# 形态二：Item + Field（经典，scrapy startproject 生成）
from scrapy import Item, Field

class QuoteItem(Item):
    text = Field()
    author = Field()
    tags = Field()

yield QuoteItem(text="...", author="...", tags=[...])

# 形态三：dataclass（现代，推荐 2026 新项目）
from dataclasses import dataclass, field

@dataclass
class QuoteItem:
    text: str
    author: str
    tags: list[str] = field(default_factory=list)

yield QuoteItem(text="...", author="...")
```

| 维度 | dict | Item+Field | dataclass |
|------|:---:|:---:|:---:|
| 写起来快 | ✅ | ❌ | ✅ |
| 字段提示/IDE | ❌ | 部分 | ✅ |
| 类型标注 | ❌ | ❌ | ✅ |
| 序列化/入库兼容 | ✅ | ✅ | ✅（需 ItemAdapter） |
| 官方推荐度（2.14+） | 可用 | 兼容 | **推荐** |

### 5.2 ItemAdapter：统一访问接口

三种形态在 Pipeline 里**不用区分**——`ItemAdapter` 统一包装：

```python
from scrapy.utils.item import ItemAdapter

class ValidationPipeline:
    def process_item(self, item, spider):
        adapter = ItemAdapter(item)
        adapter["text"] = adapter.get("text", "").strip()   # 统一读写
        if not adapter.get("url"):
            raise DropItem(f"缺少 url: {item}")
        return item
```

> ⚠️ 三种形态混用时要小心：**dict 没有字段约束**，`item.get()` 与 `item["k"]` 行为一致；**Item 访问未定义字段会抛 KeyError**；**dataclass 构造时缺失必填字段直接 TypeError**。所以形态定了就别混——新项目定 dataclass，存量 Item 就全用 Item。

## 6. 字段校验与清洗

**校验位置的选择**（与 04 章 Pipeline 分工）：Spider 里做"取得到"，Pipeline 里做"取对了"。

```python
# pipelines.py —— 字段校验（04 章会扩展成完整入库管道）
from scrapy.exceptions import DropItem
from scrapy.utils.item import ItemAdapter


class QuoteValidationPipeline:
    def process_item(self, item, spider):
        adapter = ItemAdapter(item)
        # 必填校验
        if not adapter.get("text"):
            raise DropItem(f"text 为空: {item}")
        if not adapter.get("author"):
            raise DropItem(f"author 为空: {item}")
        # 类型与清洗
        adapter["tags"] = list(set(adapter.get("tags") or []))      # 去重
        adapter["text"] = adapter["text"].replace("\xa0", " ").strip()
        # 标准化
        adapter["url"] = adapter["url"].split("#")[0]               # 去 fragment
        return item
```

**校验清单（生产标准）**：

| 类别 | 检查项 | 手段 |
|------|--------|------|
| 必填 | 主键/关键字段非空 | DropItem |
| 类型 | int/float/date 可转换 | 转换失败 DropItem 并记日志 |
| 格式 | URL 合法性、时间格式 | `urllib.parse` / `dateutil` |
| 值域 | 价格 > 0、评分 0-5 | 业务断言 |
| 重复 | 库唯一键冲突 | 入库层去重（04 章 §4） |

> 🎯 **核心要点**：校验是**数据质量的第一道闸门**——宁可 DropItem 也不把脏数据写进数据库。DropItem 的条目会计入 `item_dropped_count` 统计，是监控数据质量的硬指标（08 章）。

## 7. FEEDS 输出与落盘

调试期/小型项目可直接用 FEEDS 落盘，免写 Pipeline：

```python
# settings.py
FEEDS = {
    "data/quotes.jsonl": {"format": "jsonlines", "encoding": "utf-8", "overwrite": False},
    "data/quotes.csv": {"format": "csv", "fields": ["text", "author", "tags"], "encoding": "utf-8-sig"},
}
```

| format | 适合 | 注意 |
|--------|------|------|
| json | 结构清晰 | 中文需 `ensure_ascii=False`（dict 含中文时） |
| jsonlines | 增量追加 | **推荐**，每行一条可断点续读 |
| csv | Excel 打开 | 用 `utf-8-sig` 防中文乱码；字段顺序用 `fields` 指定 |
| xml | 对接遗留系统 | 少用 |

> ⚠️ FEEDS 适合**交付数据**，不适合**生产入库**——增量/去重/更新策略都需要 Pipeline + 数据库配合（04 章），生产环境 FEEDS 只做备份导出。

## 8. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | Selector 三合一：css()/xpath()/re()，.get()/.getall() None 安全 |
| 2 | 扩展语法 `::text`、`::attr()`；CSS 无 `:contains()`，文本筛选用 XPath |
| 3 | 提取规范：容器先行、稳定锚点、清洗放 Pipeline、必填兜底 |
| 4 | Item 三形态：dict / Item / dataclass；新项目推荐 dataclass |
| 5 | ItemAdapter 统一三种形态的读写（Pipeline 无感） |
| 6 | 校验在 Pipeline：DropItem 保证脏数据不进库，`item_dropped_count` 是质量指标 |
| 7 | FEEDS 管交付（jsonlines 推荐），生产入库走 Pipeline + 数据库 |

---

**下一模块**：[04-Item Pipeline 与数据落地](04-ItemPipeline与数据落地.md) / **返回总览**：[00-阶段6爬虫框架总览](00-阶段6爬虫框架总览.md)

## 参考来源

- [Scrapy Selectors 官方文档](https://docs.scrapy.org/en/latest/topics/selectors.html)
- [Scrapy Items 官方文档](https://docs.scrapy.org/en/latest/topics/items.html)
- [ItemAdapter 官方文档](https://docs.scrapy.org/en/latest/topics/items.html#itemadapter)
- [Scrapy FEEDS 官方文档](https://docs.scrapy.org/en/latest/topics/feed-exports.html)
- [阶段 2：静态网页爬虫（Selector 前置知识）](../阶段%202：静态网页爬虫（入门）/00-阶段2静态网页爬虫总览.md)
