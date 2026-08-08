# BeautifulSoup 解析实战
> 解析器选择、find/find_all 搜索、select 选择器、遍历导航与提取——"从 HTML 树里摘数据"的主力 API

## 📚 目录
1. [BeautifulSoup 定位与解析器](#1-beautifulsoup-定位与解析器)
2. [基础导航：对象模型](#2-基础导航对象模型)
3. [find/find_all：搜索](#3-findfind_all搜索)
4. [select：CSS 选择器](#4-selectcss-选择器)
5. [提取内容：文本与属性](#5-提取内容文本与属性)
6. [遍历与导航](#6-遍历与导航)
7. [实战模式：容器先行](#7-实战模式容器先行)

## 1. BeautifulSoup 定位与解析器

| 事实 | 说明 |
|------|------|
| 版本 | bs4 4.12+（2026） |
| 定位 | 最流行的 HTML 解析库（容错强：烂 HTML 也能解析） |
| 解析器 | `html.parser`（内置）/ **`lxml`（推荐）** / `html5lib`（最标准） |
| 选择方式 | 对象导航 + find 搜索 + CSS select |

```python
from bs4 import BeautifulSoup

# ═══ 解析 HTML ═══
soup = BeautifulSoup(html_text, "lxml")      # 推荐：lxml 后端（快 + 容错）

# 从文件/响应解析
soup = BeautifulSoup(open("page.html", encoding="utf-8"), "lxml")
soup = BeautifulSoup(resp.text, "lxml")      # 爬虫标准姿势

# 解析器对比：
#   html.parser   ：内置零依赖，慢
#   lxml          ：快、容错强、支持 XPath（⚠️ 需 pip install lxml）
#   html5lib      ：最符合标准，最慢（极少用）
```

> 🎯 规则：**爬虫一律 `BeautifulSoup(html, "lxml")`**——lxml 后端是性能与容错的平衡点（坏标签/不闭合也能忍）。与 [04-lxml 解析](04-lxml与XPath解析.md) 的关系：bs4 用 lxml 当引擎，也可以直接用 lxml 的原生 API。

## 2. 基础导航：对象模型

```python
# BeautifulSoup 把 HTML 变成四个对象：
#   Tag        ：元素（.name / .attrs / .text）
#   NavigableString：文本节点
#   BeautifulSoup：文档整体
#   Comment    ：注释

soup = BeautifulSoup('<div class="card"><h2>标题</h2><p>内容</p></div>', "lxml")

div = soup.div                    # 第一个 div 标签（Tag 对象）
div.name                          # 'div'
div.attrs                         # {'class': ['card']}（class 是列表！）
div["class"]                      # ['card']

div.text                          # '标题内容'（全部文本拼接）
div.get_text(separator=" ")       # '标题 内容'（可指定分隔符）
```

> ⚠️ 两个坑：**① `soup.div` 只拿第一个 div**（要全部用 find_all/select）；**② `attrs['class']` 是列表**（多 class 元素）——取值用 `div.get("class", [])`。

## 3. find/find_all：搜索

```python
soup = BeautifulSoup(html, "lxml")

# ═══ 按标签 ═══
soup.find("a")                        # 第一个 a
soup.find_all("a")                    # 所有 a（列表）

# ═══ 按属性（class/id 高频）═══
soup.find_all("a", class_="news-title")     # ⚠️ class_ 带下划线！
soup.find("div", id="header")               # id 直接参数
soup.find_all("a", href=True)               # 有 href 的 a
soup.find_all("a", href="/news/")           # href 精确匹配
soup.find_all("a", href=lambda h: h and "news" in h)   # 模糊（lambda）

# ═══ 按文本 ═══
soup.find_all("p", string="固定文本")        # 文本精确
soup.find_all(string=re.compile("关键词"))   # 文本正则（配 re）

# ═══ 限定范围与数量 ═══
soup.find_all("a", limit=10)                # 最多 10 个
soup.find("div", class_="card").find_all("a")  # 容器内搜索
```

| find 族 | 返回 | 用途 |
|---------|------|------|
| `find()` | 第一个（Tag/None） | 定位单元素 |
| `find_all()` | 列表 | 批量提取（主力） |
| `find_parents/find_next_siblings` | 反向/旁路 | 复杂导航 |

> ⚠️ **`class_` 是 Python 关键字避让**（class 是保留字）——这是 bs4 新手第一坑；但 CSS `select(".title")` 里就不用管。

## 4. select：CSS 选择器

```python
soup = BeautifulSoup(html, "lxml")

# ═══ CSS 选择器（与阶段 1 §4 完全一致）═══
soup.select(".news-list li a")           # 后代组合
soup.select("#header .title")            # ID + 类
soup.select("a[href*='/news/']")         # 属性包含
soup.select("li:first-child")            # 伪类
soup.select("div.card > h2")             # 子代

# select_one：取第一个（找不到返回 None）
soup.select_one(".news-title")           # 单个元素
soup.select_one("a")["href"]             # 直接链式取属性

# ⚠️ 找不到时的行为：
#   select()    → []（空列表，安全）
#   select_one()→ None（⚠️ 再取 .text 会 AttributeError！）
```

| 选择方式 | 场景 |
|---------|------|
| `select()` | **批量提取主力**（列表项） |
| `select_one()` | 单元素（标题/日期） |
| `find_all()` | 需要按属性/文本复杂过滤时 |
| 组合 | `select_one` 容器 + `select` 内部（§7 模式） |

> 🎯 选择器选型：**80% 场景 `select`/`select_one` 够用**（与阶段 1 学的 CSS 语法无缝衔接）；属性模糊匹配/按文本找才用 `find_all` 的 kwargs/lambda。

## 5. 提取内容：文本与属性

```python
# ═══ 文本提取 ═══
title = item.select_one("h2.title")
title.text                      # 全部文本（含子元素）
title.get_text(strip=True)      # 去首尾空白（推荐）
title.string                    # ⚠️ 仅当元素只有一个文本子节点时可用！

# ═══ 属性提取 ═══
a = item.select_one("a")
a["href"]                       # ⚠️ 属性不存在会 KeyError！
a.get("href")                   # 安全：不存在返回 None（推荐）
a.get("href", "")               # 带默认值

# ═══ 图片 ═══
img = item.select_one("img")
img["src"]                      # 图片地址
img.get("data-src")             # 懒加载图片的真实地址（常见！）

# ═══ 完整提取示例 ═══
def extract_item(item):
    """从一条列表项提取结构化数据"""
    title_el = item.select_one("h2.title")
    link_el = item.select_one("a")
    date_el = item.select_one("span.date")
    return {
        "title": title_el.get_text(strip=True) if title_el else "",
        "url": link_el.get("href", "") if link_el else "",
        "date": date_el.get_text(strip=True) if date_el else "",
    }
```

> ⚠️ 空元素三连坑：**`select_one` 可能返回 None → 再取 `.text` 报 AttributeError**——生产代码每个提取都要判空（`if el`）或用 `.get()` 安全取值。列表页偶发缺字段是常态，判空是必修课。

## 6. 遍历与导航

```python
soup = BeautifulSoup(html, "lxml")

# ═══ 父子 ═══
div = soup.find("div", class_="card")
div.parent                    # 父节点
div.children                  # 直接子节点（迭代器）
list(div.children)

# ═══ 兄弟 ═══
div.next_sibling              # 下一个兄弟（可能是文本/换行！）
div.next_sibling.next_sibling # 跳空白（常见写法）
div.find_next_sibling("div")  # 下一个 div 兄弟（推荐）

# ═══ 搜索导向导航（推荐：绕开空白节点）═══
div.find_next("h2")           # 之后的第一个 h2
div.find_previous("div")      # 之前的第一个 div
div.find_parent("section")    # 向上找祖先

# ═══ 遍历全部 ═══
for tag in soup.find_all(True):       # 所有标签
    pass
```

> 💡 遍历优先级：**能 select/find 就别手动遍历**——`find_next_sibling("div")` 这类"导向导航"比 `next_sibling.next_sibling` 跳空白可靠得多；手动游走是最后手段。

## 7. 实战模式：容器先行

```python
# ═══ 标准提取模式：容器先行 + 逐项提取 ═══
# ① 找到列表容器（稳定锚点，见阶段 1 §7）
container = soup.select_one("ul.news-list")
if not container:
    print("容器未找到——站点结构变了？")
    return []

# ② 逐项提取（每项独立判空）
results = []
for item in container.select("li"):
    data = extract_item(item)          # §5 的提取函数
    if data["title"]:                  # 有效数据才保留
        results.append(data)

# ③ 返回结构化结果
return results
```

```text
容器先行的三个好处：
  ① 锚点稳定：容器 class 变动的概率 < 每个 item 的
  ② 范围隔离：不会误抓页面其他区域的同类元素
  ③ 容错自然：某个 item 缺字段不影响其他 item

与阶段 1 §4 的"选择器泛化"衔接：容器选择器写稳定锚点，
内部子选择器写相对定位——改版时先看容器，再调内部。
```

> 🎯 本阶段解析能力的验收：**拿到任意静态页面 → 3 分钟内写出容器 + 提取代码并跑通**。下一个能力是"用 XPath 解决 CSS 搞不定的场景"（[04-lxml与XPath解析](04-lxml与XPath解析.md)）。

---

**下一模块**：[04-lxml与XPath解析](04-lxml与XPath解析.md) / **返回总览**：[00-阶段2静态网页爬虫总览](00-阶段2静态网页爬虫总览.md)
