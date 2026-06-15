# HTML 详细知识点梳理

> **文档定位**：Java 后端技术参考文档 | HTML 核心知识点全景  
> **核心说明**：HTML（HyperText Markup Language，超文本标记语言）是用于创建网页结构和内容的标准标记语言，是网页开发的"骨架"，通常与 CSS（样式）、JavaScript（交互）配合使用  
> **后端重点**：看懂 HTML 结构、理解表单提交机制、掌握语义化标签

---

## 目录

- [一、HTML 基础核心概念](#一html-基础核心概念)
- [二、HTML 核心标签](#二html-核心标签)
- [三、HTML5 新增标签与特性](#三html5-新增标签与特性)
- [四、HTML 常用属性与规范](#四html-常用属性与规范)
- [五、常见问题与注意事项](#五常见问题与注意事项)

---

## 一、HTML 基础核心概念

### 1.1 什么是 HTML

HTML（HyperText Markup Language，超文本标记语言）是用于创建网页结构和内容的标准标记语言。它不是编程语言（无逻辑、不执行计算），而是通过一系列标记（标签）来定义网页中各个元素的含义和布局。

> **核心作用**：将文本、图片、视频、链接等内容组织成浏览器可识别、可渲染的网页结构。

### 1.2 HTML 的基本结构

```html
<!DOCTYPE html>  <!-- 声明文档类型，HTML5 -->
<html lang="zh-CN">  <!-- 根标签，lang 指定语言 -->
  <head>  <!-- 头部：存放页面元信息，不直接显示 -->
    <meta charset="UTF-8">  <!-- 字符编码，UTF-8 兼容所有中文 -->
    <title>页面标题</title>  <!-- 浏览器标签页标题，必填 -->
    <meta name="description" content="页面描述">  <!-- SEO 优化 -->
    <link rel="stylesheet" href="style.css">  <!-- 引入外部 CSS -->
  </head>
  <body>  <!-- 主体：所有可见内容 -->
    <h1>这是页面主标题</h1>
    <p>这是一段文本内容</p>
  </body>
</html>
```

### 1.3 HTML 标签的核心特性

| 特性 | 说明 |
|------|------|
| **标签语法** | 大多数成对出现（`<h1></h1>`），少数自闭合（`<img>`、`<input>`） |
| **标签属性** | 格式 `属性名="属性值"`，写在开始标签内，多个用空格分隔 |
| **嵌套规则** | 可以嵌套，但不能交叉嵌套 |
| **大小写** | 不区分大小写，规范建议全部小写 |

---

## 二、HTML 核心标签

### 2.1 文档结构标签

| 标签 | 说明 | 关键属性 |
|------|------|----------|
| `<!DOCTYPE html>` | 文档类型声明，必须放在最顶部 | — |
| `<html>` | 根标签 | `lang`：指定页面语言 |
| `<head>` | 头部标签，存放元信息和资源引入 | — |
| `<title>` | 页面标题，影响 SEO | — |
| `<body>` | 主体标签，所有可见内容 | — |

### 2.2 文本相关标签

#### 标题标签（h1 ~ h6）

```html
<h1>一级标题（主标题，建议一个页面只写一个）</h1>
<h2>二级标题（章节标题）</h2>
<h3>三级标题（小节标题）</h3>
```

#### 段落与换行

```html
<p>段落标签，段落之间自动换行并产生间距。</p>
<p>段落内<br/>强制换行（不产生段落间距）。</p>
```

#### 文本样式标签

| 标签 | 说明 | 语义 |
|------|------|------|
| `<strong>` | 加粗强调 | 语义强调 |
| `<em>` | 斜体强调 | 语义强调 |
| `<b>` | 单纯加粗 | 无语义 |
| `<i>` | 单纯斜体 | 无语义 |
| `<u>` | 下划线 | 避免与链接混淆 |
| `<s>` / `<del>` | 删除线 | 废弃内容 |
| `<ins>` | 插入线 | 新增内容 |

#### 特殊文本标签

```html
<span style="color: red;">行内标签，常配合 CSS 使用</span>

<pre>
  预格式化文本
  保留空格和换行（常用于展示代码）
</pre>

<blockquote>引用内容，自动缩进</blockquote>
```

### 2.3 链接标签（`<a>`）

#### 核心属性

| 属性 | 说明 | 常用值 |
|------|------|--------|
| **`href`** | 跳转目标地址（必填） | URL、相对路径、`#锚点`、`mailto:`、`tel:` |
| **`target`** | 打开方式 | `_self`（当前窗口）、`_blank`（新窗口） |
| **`rel`** | 页面关系 | `noopener noreferrer`（安全属性）、`nofollow` |

```html
<!-- 外部链接，新窗口打开 -->
<a href="https://www.baidu.com" target="_blank" rel="noopener noreferrer">百度</a>

<!-- 内部链接 -->
<a href="news.html">新闻页面</a>

<!-- 锚点链接 -->
<a href="#section1">跳转到第一部分</a>
<div id="section1">第一部分内容</div>

<!-- 邮件/电话链接 -->
<a href="mailto:123@qq.com">发送邮件</a>
<a href="tel:13800138000">拨打电话</a>
```

### 2.4 图片标签（`<img>`）

```html
<img src="img/1.jpg" alt="风景图" width="500" loading="lazy" title="美丽的风景">
```

| 属性 | 说明 |
|------|------|
| **`src`** | 图片路径（必填），支持相对路径、绝对路径、base64 |
| **`alt`** | 加载失败时的替代文本（必填，兼顾 SEO 和无障碍） |
| `width` / `height` | 宽高（建议只设一个，另一个自动按比例缩放） |
| `title` | 鼠标悬停提示 |
| `loading` | `lazy`（懒加载，优化性能）/ `eager`（默认） |

### 2.5 列表标签

#### 无序列表（`<ul>`）

```html
<ul>
  <li>列表项 1</li>
  <li>列表项 2</li>
</ul>
```

#### 有序列表（`<ol>`）

```html
<ol type="a" start="3">  <!-- type: 1/a/A/i/I -->
  <li>列表项 1</li>
  <li>列表项 2</li>
</ol>
```

#### 定义列表（`<dl>`）

```html
<dl>
  <dt>HTML</dt>
  <dd>超文本标记语言，用于创建网页结构。</dd>
  <dt>CSS</dt>
  <dd>层叠样式表，用于美化网页样式。</dd>
</dl>
```

### 2.6 表格标签（`<table>`）

| 标签 | 说明 |
|------|------|
| `<table>` | 表格容器 |
| `<thead>` | 表格头部 |
| `<tbody>` | 表格主体 |
| `<tfoot>` | 表格底部（合计、备注） |
| `<tr>` | 表格行 |
| `<th>` | 表头单元格（默认加粗居中） |
| `<td>` | 普通单元格 |

```html
<table border="1">
  <thead>
    <tr>
      <th colspan="2">学生信息表</th>  <!-- 跨 2 列 -->
    </tr>
    <tr>
      <th>姓名</th>
      <th>年龄</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>张三</td>
      <td rowspan="2">18</td>  <!-- 跨 2 行 -->
    </tr>
    <tr>
      <td>李四</td>
    </tr>
  </tbody>
</table>
```

| 合并属性 | 说明 |
|----------|------|
| `rowspan` | 跨行合并 |
| `colspan` | 跨列合并 |

### 2.7 表单标签（`<form>`）

#### `<form>` 核心属性

| 属性 | 说明 | 常用值 |
|------|------|--------|
| **`action`** | 提交目标地址 | 服务器接口 URL |
| **`method`** | 提交方式 | `GET`（查询）/ `POST`（提交） |
| `enctype` | 编码方式 | `multipart/form-data`（上传文件） |

#### 常用表单控件

| 控件 | 标签 | 核心属性 |
|------|------|----------|
| **输入框** | `<input>` | `type`、`name`、`value`、`placeholder`、`required` |
| **多行文本** | `<textarea>` | `name`、`rows`、`cols` |
| **下拉选择** | `<select>` + `<option>` | `name`、`value`、`selected` |
| **按钮** | `<button>` | `type`（`submit`/`reset`/`button`） |
| **标签关联** | `<label>` | `for` 与控件 `id` 对应 |

#### `<input>` 的 type 属性

| type | 说明 |
|------|------|
| `text` | 单行文本 |
| `password` | 密码框（内容隐藏） |
| `number` | 数字输入框 |
| `email` | 邮箱输入（自动验证格式） |
| `tel` | 电话输入（移动端唤起拨号盘） |
| `date` | 日期选择 |
| `file` | 文件上传 |
| `checkbox` | 复选框（多选，同 name） |
| `radio` | 单选框（单选，同 name） |
| `submit` | 提交按钮 |
| `reset` | 重置按钮 |
| `button` | 普通按钮（配合 JS） |

#### 完整表单示例

```html
<form action="login.php" method="post">
  <label for="username">用户名：</label>
  <input type="text" id="username" name="username" placeholder="请输入用户名" required><br/>

  <label>性别：</label>
  <input type="radio" name="gender" value="male" id="male">
  <label for="male">男</label>
  <input type="radio" name="gender" value="female" id="female">
  <label for="female">女</label><br/>

  <label for="city">城市：</label>
  <select id="city" name="city">
    <option value="">请选择</option>
    <option value="beijing">北京</option>
    <option value="shanghai" selected>上海</option>
  </select><br/>

  <label for="remark">备注：</label>
  <textarea id="remark" name="remark" rows="3" placeholder="请输入备注"></textarea><br/>

  <button type="submit">提交</button>
  <button type="reset">重置</button>
</form>
```

---

## 三、HTML5 新增标签与特性

### 3.1 新增语义化标签

| 标签 | 说明 | 使用场景 |
|------|------|----------|
| `<header>` | 页面头部 | 页面顶部、章节顶部 |
| `<nav>` | 导航栏 | 页面顶部导航、侧边导航 |
| `<main>` | 页面主体（唯一） | 页面核心内容区域 |
| `<section>` | 章节/区块 | 新闻列表、产品展示 |
| `<article>` | 独立内容 | 新闻、博客、评论 |
| `<aside>` | 侧边栏 | 辅助内容、广告 |
| `<footer>` | 页面底部 | 版权信息、联系方式 |
| `<figure>` | 媒体容器 | 图片 + 说明 |
| `<figcaption>` | 媒体说明 | 图片下方说明文字 |

### 3.2 HTML5 新增表单控件

#### 新增 input 类型

| type | 说明 |
|------|------|
| `color` | 颜色选择器 |
| `range` | 滑块控件（`min`/`max`/`step`） |
| `search` | 搜索输入框（带清除按钮） |
| `url` | URL 输入框（自动验证） |
| `time` | 时间选择 |
| `month` | 月份选择 |

#### 新增表单属性

| 属性 | 说明 |
|------|------|
| `placeholder` | 输入提示文本 |
| `required` | 必填验证 |
| `autofocus` | 自动聚焦 |
| `autocomplete` | 自动完成 `on`/`off` |
| `pattern` | 正则验证（如手机号：`^1[3-9]\d{9}$`） |

### 3.3 视频标签（`<video>`）

```html
<video controls width="600" poster="cover.jpg">
  <source src="video.mp4" type="video/mp4">
  <source src="video.webm" type="video/webm">
  您的浏览器不支持视频播放。
</video>
```

| 属性 | 说明 |
|------|------|
| `controls` | 显示控制栏 |
| `autoplay` | 自动播放 |
| `muted` | 静音 |
| `loop` | 循环播放 |
| `poster` | 封面图片 |

### 3.4 音频标签（`<audio>`）

```html
<audio controls muted autoplay loop>
  <source src="audio.mp3" type="audio/mpeg">
  <source src="audio.ogg" type="audio/ogg">
  您的浏览器不支持音频播放。
</audio>
```

---

## 四、HTML 常用属性与规范

### 4.1 全局属性

| 属性 | 说明 |
|------|------|
| `id` | 唯一标识（不可重复） |
| `class` | 类名（可重复，多个用空格分隔） |
| `style` | 行内样式 |
| `title` | 鼠标悬停提示 |
| `hidden` | 隐藏元素 |
| `lang` | 指定语言 |

### 4.2 HTML 编码规范

| 规范 | 说明 |
|------|------|
| **文档声明** | 顶部必须添加 `<!DOCTYPE html>` |
| **字符编码** | `<head>` 内必须添加 `<meta charset="UTF-8">` |
| **标签小写** | 所有标签和属性名全部使用小写 |
| **属性引号** | 属性值必须用双引号包裹 |
| **语义优先** | 优先使用语义化标签，避免过度使用 `<div>` |
| **缩进规范** | 标签嵌套使用缩进（2 或 4 个空格） |

---

## 五、常见问题与注意事项

| 问题 | 原因 | 解决方法 |
|------|------|----------|
| **中文乱码** | 未添加 `<meta charset="UTF-8">` | 确保 `<head>` 内添加正确编码 |
| **图片加载失败** | src 路径错误 | 检查图片路径，确保文件存在 |
| **表单无法提交** | 缺少 `name` 属性 | 表单控件必须设置 `name` |
| **标签嵌套错误** | 行内标签嵌套块级标签 | 块级内可嵌套行内，行内不可嵌套块级 |
