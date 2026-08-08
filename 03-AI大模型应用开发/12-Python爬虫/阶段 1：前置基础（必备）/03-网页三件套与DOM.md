# 网页三件套与 DOM
> HTML 是网页的骨架、CSS 是皮肤、JavaScript 是肌肉——爬虫必须看懂"网页是什么构成的"，才能知道"数据藏在哪一层"

## 📚 目录
1. [网页的构成：HTML/CSS/JS 分工](#1-网页的构成htmlcssjs-分工)
2. [HTML：骨架与标签](#2-html骨架与标签)
3. [DOM 树：浏览器眼中的网页](#3-dom-树浏览器眼中的网页)
4. [CSS：皮肤与选择器入口](#4-css皮肤与选择器入口)
5. [JavaScript：动态内容的制造者](#5-javascript动态内容的制造者)
6. [数据藏在哪一层：静态 vs 动态](#6-数据藏在哪一层静态-vs-动态)
7. [爬虫必会的 HTML 结构常识](#7-爬虫必会的-html-结构常识)

## 1. 网页的构成：HTML/CSS/JS 分工

| 技术 | 角色 | 类比 | 爬虫意义 |
|------|------|------|---------|
| **HTML** | 结构（内容是什么） | 骨架 | **主要提取目标** |
| **CSS** | 样式（长什么样） | 皮肤 | 选择器语法的来源 |
| **JavaScript** | 行为（动态变化） | 肌肉 | 决定数据是否"可见" |

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">                  <!-- 编码声明（乱码排查第一站） -->
    <title>文章列表</title>
    <link rel="stylesheet" href="style.css"> <!-- CSS 外链 -->
</head>
<body>
    <div class="article-list">              <!-- HTML：内容结构 -->
        <article class="item">
            <h2 class="title">标题一</h2>
            <p class="summary">摘要内容...</p>
        </article>
    </div>
    <script src="app.js"></script>          <!-- JS 外链：可能后续加载数据！ -->
</body>
</html>
```

> 🎯 爬虫第一问永远是：**"数据在 HTML 里，还是 JS 加载的？"** ——在 HTML 里 → 静态爬虫（阶段 2）；JS 加载 → 找接口或动态渲染（阶段 5）。这一问决定整个技术路线。

## 2. HTML：骨架与标签

### 2.1 高频标签速查

| 标签 | 语义 | 爬虫关注点 |
|------|------|-----------|
| `<div>` | 块容器（无语义） | 分块定位（class 区分） |
| `<span>` | 行内容器 | 行内数据 |
| `<h1>-<h6>` | 标题 | 页面/区块标题 |
| `<p>` | 段落 | 正文 |
| `<a href>` | 链接 | **href 是抓取目标 URL** |
| `<img src>` | 图片 | **src 是图片地址** |
| `<ul>/<li>` | 列表 | 列表结构（导航/条目） |
| `<table>/<tr>/<td>` | 表格 | 表格数据 |
| `<input>` | 表单输入 | 表单提交参数 |
| `<form>` | 表单 | 登录/搜索的提交地址与字段 |

```html
<!-- 一条典型的"列表条目"结构（爬虫最常见的解析对象） -->
<ul class="news-list">
    <li>
        <a href="/news/1001.html" class="news-title">新闻标题一</a>
        <span class="date">2026-08-08</span>
    </li>
    <li>
        <a href="/news/1002.html" class="news-title">新闻标题二</a>
        <span class="date">2026-08-07</span>
    </li>
</ul>
<!-- 提取目标：每个 li 里的 a 文本 + href + date -->
```

### 2.2 属性（attribute）

| 属性 | 用途 | 爬虫用法 |
|------|------|---------|
| `class` | 样式类（**可重复**） | 定位的主力（.news-title） |
| `id` | 唯一标识（页面唯一） | 精确定位（#header） |
| `href` | 链接目标 | 抓取 URL |
| `src` | 资源地址（图片/脚本） | 图片下载 |
| `data-*` | 自定义数据属性 | **常藏着结构化数据！** |
| `title` | 悬停提示 | 附加文本信息 |

> 💡 **`data-*` 属性是爬虫的宝矿**：很多站点把 ID、类型、排序等结构化数据塞进 `data-xxx` 属性——解析时先扫一眼元素属性，常常比解析文本更稳。

## 3. DOM 树：浏览器眼中的网页

```text
浏览器把 HTML 解析成内存中的树结构（DOM）：
                        html
                     ┌────┴────┐
                   head       body
                  ┌──┴──┐   ┌──┴────────┐
              meta  title  div.article-list   script
                              ┌────┴────┐
                          article.item  article.item
                           ┌────┴────┐
                        h2.title   p.summary

爬虫解析 HTML 的本质 = 在这棵树里"找节点"
  BeautifulSoup / lxml 就是把 HTML 变成这种树，然后按选择器取节点
```

| 概念 | 说明 | 爬虫用法 |
|------|------|---------|
| 节点（Node） | 树的每个元素 | 选择器的返回对象 |
| 父子/兄弟 | 节点间关系 | XPath 轴定位（见 04） |
| 文本节点 | 元素里的文字 | `.text` / `.string` |
| 属性节点 | 元素的属性 | `.get("href")` |

> 🎯 认知转换：**爬虫解析 = 树遍历**。用 CSS 选择器（按特征找）+ XPath（按路径找）都能到达同一节点——04 会讲透两种语法。

## 4. CSS：皮肤与选择器入口

```css
/* CSS 的定位语法 = 选择器（爬虫最常用的定位语法） */
.news-title { color: red; }        /* 类选择器：class="news-title" */
#header { height: 60px; }          /* ID 选择器：id="header" */
div > p { margin: 0; }             /* 子选择器：div 的直接子 p */
a[href*="news"] { ... }            /* 属性选择器：href 含 news */
```

| 选择器 | 写法 | 匹配 |
|--------|------|------|
| 类 | `.title` | class 含 title |
| ID | `#header` | id="header" |
| 标签 | `div` | 所有 div |
| 后代 | `div a` | div 内的所有 a |
| 子代 | `div > a` | div 的直接子 a |
| 属性 | `a[href]` / `a[href*="news"]` | 有/含某属性的 a |
| 组合 | `.list .item a.title` | 多级定位 |

> 💡 为什么爬虫要学 CSS 选择器：**BeautifulSoup 的 `select()` 就是 CSS 选择器语法**（阶段 2 主力 API）；浏览器 F12 还能"复制 selector"直接生成（见 [07](07-浏览器开发者工具实战.md)）——CSS 选择器是解析 HTML 的通用语言。

## 5. JavaScript：动态内容的制造者

```text
静态 HTML：服务器把数据直接写进 HTML（爬虫直接拿）
动态 HTML：HTML 是空壳，JS 运行后再请求接口填充数据

判断"数据是不是 JS 加载的"（爬虫关键判断）：
  ① 查看源码（Ctrl+U）：数据在源码里？→ 静态
  ② 不在源码但页面上有 → JS 加载（接口/渲染）
  ③ F12 Network 里找 XHR 请求 → 数据接口
```

```html
<!-- 空壳页面（动态）：列表数据全靠 JS 拉取 -->
<body>
    <div id="app"></div>          <!-- 容器是空的！ -->
    <script>
        fetch('/api/news').then(r => r.json()).then(data => {
            // JS 把接口数据渲染进 #app
        });
    </script>
</body>
```

| 数据形态 | 技术路线 | 阶段 |
|---------|---------|:---:|
| 静态 HTML | requests + BeautifulSoup | 阶段 2 |
| JS 渲染（无接口） | Selenium/Playwright 渲染后解析 | 阶段 5 |
| JS 渲染（有接口） | **直接请求接口**（最优！） | 阶段 2/5 |
| 加密接口 | 逆向/渲染兜底 | 阶段 7 |

> 🎯 黄金法则：**有接口先抓接口，接口拿不到才渲染页面**——直接请求接口（JSON）比渲染页面（HTML）快 10 倍、稳 10 倍。发现数据接口的能力就是 [07-浏览器开发者工具实战](07-浏览器开发者工具实战.md) 的核心。

## 6. 数据藏在哪一层：静态 vs 动态

| 场景 | 表现 | 爬虫动作 |
|------|------|---------|
| 静态内容 | Ctrl+U 源码里能看到数据 | 直接 requests + 解析 |
| JSON 内嵌 | HTML 里有一大段 JSON（`window.__DATA__` 或 script type=application/json） | **直接正则/JSON 解析提取**（省事） |
| 接口数据 | F12 Network 里 XHR 返回 JSON | 直接请求接口（带 Cookie/参数） |
| 完全渲染 | 页面元素由 JS 动态生成 | Selenium/Playwright（阶段 5） |

```html
<!-- 常见的内嵌 JSON 形态（很多人不知道可以直接提取） -->
<script type="application/json" id="__NEXT_DATA__">
{"props":{"articles":[{"id":1001,"title":"标题一"}]}}
</script>
<!-- 爬虫：正则抠出 script 内容 → json.loads → 直接用 -->
```

> 🎯 **内嵌 JSON 是"低调的宝藏"**：Next.js/SPA 框架常把首屏数据序列化进页面脚本——发现它就能绕开复杂解析，一步到位拿结构化数据。

## 7. 爬虫必会的 HTML 结构常识

```text
爬虫读 HTML 的三个习惯：
  ① 先看整体结构（F12 Elements 折叠看层级）
  ② 找数据所在的最小节点（避免误抓）
  ③ 找"稳定锚点"（class/id 稳定的节点，比位置稳定）

识别稳定锚点：
  ✅ class 语义化（.news-title / #article-list）→ 稳定
  ❌ 无规律 class（css-1234 哈希类名）→ 不稳定（改版就崩）
  ❌ 依赖位置（第 3 个 div 的第 2 个 p）→ 脆弱
```

> 💡 工程经验：**锚点稳定性决定爬虫寿命**——站点改版 80% 是改 class/结构。选"语义化稳定节点"做锚点，配合数据校验（提取结果非空才入库），爬虫才能活过改版（[阶段 2](../阶段%202：静态网页爬虫（入门）/00-阶段2静态网页爬虫总览.md) 与[阶段 8 存储](../阶段%203：数据存储/00-阶段3数据存储总览.md) 会展开）。

---

**下一模块**：[04-选择器与定位技术](04-选择器与定位技术.md) / **返回总览**：[00-阶段1前置基础总览](00-阶段1前置基础总览.md)
