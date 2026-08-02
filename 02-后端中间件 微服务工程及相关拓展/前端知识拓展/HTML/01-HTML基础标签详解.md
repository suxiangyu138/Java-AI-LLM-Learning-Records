# 01 - HTML 基础标签详解

> 定位：文本、列表、链接、图片、表格、表单六大基础标签族的完整语法与易错点

## 📚 目录

1. [文本与标题](#1-文本与标题)
2. [列表与链接](#2-列表与链接)
3. [图片与多媒体](#3-图片与多媒体)
4. [表格](#4-表格)
5. [表单](#5-表单)
6. [全局属性](#6-全局属性)

---

## 1. 文本与标题

### 1.1 标题与段落

```html
<h1>一级标题（每页唯一）</h1>
<h2>二级标题</h2>
<p>段落文本，浏览器自动换行。</p>
<br>           <!-- 强制换行（少用） -->
<hr>           <!-- 水平分割线 -->
```

| 规则 | 说明 |
|------|------|
| h1 唯一 | 每页一个，SEO 核心 |
| 层级连续 | 不要 h1 → h3 跳级 |
| p 内不嵌套块级 | p 里不能放 div/table |

### 1.2 文本格式化

```html
<strong>重要</strong>     <!-- 语义重要（默认加粗） -->
<em>强调</em>             <!-- 语义强调（默认斜体） -->
<mark>高亮</mark>         <!-- 标记 -->
<del>删除</del> <ins>插入</ins>
<sub>下标</sub> <sup>上标</sup>
<code>console.log()</code>
<pre>
    保留   空白
    与换行
</pre>
<blockquote>长引用</blockquote>
<q>行内引用</q>
```

> 🎯 **要点**：`<strong>/<em>` 是语义标签（机器可理解），`<b>/<i>` 仅视觉效果——语义优先。

---

## 2. 列表与链接

### 2.1 三种列表

```html
<!-- 无序列表 -->
<ul>
    <li>苹果</li>
    <li>香蕉</li>
</ul>

<!-- 有序列表 -->
<ol start="3" reversed>
    <li>第三</li>
    <li>第二</li>
</ol>

<!-- 定义列表 -->
<dl>
    <dt>HTML</dt>
    <dd>网页结构语言</dd>
</dl>
```

### 2.2 链接与锚点

```html
<!-- 外部链接 -->
<a href="https://example.com" target="_blank" rel="noopener">新窗口打开</a>

<!-- ⚠️ 安全属性：target="_blank" 必须配 rel="noopener"（防钓鱼） -->

<!-- 页面内锚点 -->
<a href="#section2">跳转到第二节</a>
<h2 id="section2">第二节</h2>

<!-- 其他协议 -->
<a href="mailto:user@example.com">发邮件</a>
<a href="tel:+8613800000000">打电话</a>
<a href="javascript:void(0)">JS 占位（避免滚动）</a>
```

| 易错点 | 修正 |
|--------|------|
| `target="_blank"` 无 rel | 加 `rel="noopener noreferrer"` |
| 锚点 id 重复 | 页面内唯一 |
| 死链 href="#" | 用 `javascript:void(0)` 或真实地址 |

---

## 3. 图片与多媒体

### 3.1 图片

```html
<img src="logo.png" alt="公司 Logo" width="200" height="100">

<!-- 响应式图片（srcset：不同分辨率/尺寸） -->
<img src="small.jpg"
     srcset="small.jpg 480w, large.jpg 1080w"
     sizes="(max-width: 600px) 480px, 1080px"
     alt="响应式示例">

<!-- 图片懒加载 -->
<img src="photo.jpg" alt="示例" loading="lazy">

<!-- 图片地图/占位 -->
<figure>
    <img src="chart.png" alt="数据图表">
    <figcaption>图 1：2026 年数据</figcaption>
</figure>
```

> 🎯 **要点**：`alt` 必填（无障碍 + SEO + 图片加载失败时兜底）；`loading="lazy"` 是性能优化标配。

### 3.2 音视频

```html
<video controls width="640">
    <source src="movie.mp4" type="video/mp4">
    <source src="movie.webm" type="video/webm">
    你的浏览器不支持视频
</video>

<audio controls>
    <source src="audio.mp3" type="audio/mpeg">
</audio>

<!-- 视频封面与预加载 -->
<video controls poster="poster.jpg" preload="metadata">
```

---

## 4. 表格

### 4.1 基础表格

```html
<table>
    <caption>员工工资表</caption>      <!-- 表格标题 -->
    <thead>                            <!-- 表头组 -->
        <tr>
            <th scope="col">姓名</th>
            <th scope="col">工资</th>
        </tr>
    </thead>
    <tbody>                            <!-- 表体组 -->
        <tr>
            <td>张三</td>
            <td>10000</td>
        </tr>
    </tbody>
    <tfoot>                            <!-- 表脚组 -->
        <tr><td>合计</td><td>20000</td></tr>
    </tfoot>
</table>
```

### 4.2 合并单元格

```html
<table>
    <tr>
        <td rowspan="2">跨两行</td>   <!-- 垂直合并 -->
        <td>单元格</td>
    </tr>
    <tr>
        <td colspan="2">跨两列</td>   <!-- 水平合并 -->
    </tr>
</table>
```

| 易错点 | 说明 |
|--------|------|
| thead/tbody 缺失 | 语义不完整（样式/滚动依赖） |
| th 无 scope | 屏幕阅读器读错 |
| 表格用于布局 | ❌ 必须用 CSS（div/grid） |

---

## 5. 表单

### 5.1 表单元素总览

```html
<form action="/submit" method="post">
    <!-- 文本框 -->
    <label for="name">姓名：</label>          <!-- ⚠️ label 必配 for -->
    <input type="text" id="name" name="name"
           required placeholder="请输入姓名" maxlength="20">

    <!-- 邮箱/密码/数字 -->
    <input type="email" name="email">          <!-- 自动校验格式 -->
    <input type="password" name="pwd">
    <input type="number" name="age" min="0" max="150">

    <!-- 单选/复选 -->
    <input type="radio" name="gender" value="m" id="m">
    <label for="m">男</label>
    <input type="checkbox" name="agree" id="agree">
    <label for="agree">同意协议</label>

    <!-- 下拉 -->
    <select name="city">
        <option value="bj">北京</option>
        <option value="sh" selected>上海</option>
    </select>

    <!-- 日期/范围/文件 -->
    <input type="date" name="birthday">
    <input type="range" name="volume" min="0" max="100">
    <input type="file" name="avatar" accept="image/*">

    <button type="submit">提交</button>
    <button type="reset">重置</button>
</form>
```

### 5.2 表单校验与安全

```html
<!-- HTML5 原生校验 -->
<input type="email" required>        <!-- 必填 + 格式 -->
<input pattern="[0-9]{11}" title="11 位手机号">   <!-- 正则 -->
<input minlength="6" maxlength="20">

<!-- ⚠️ 安全：CSRF 令牌（后端注入） -->
<input type="hidden" name="_token" value="...">
```

| 易错点 | 修正 |
|--------|------|
| label 无 for | 控件无名称（无障碍失败） |
| 校验只靠前端 | 后端必须二次校验 |
| type="text" 当邮箱 | 用 type="email" 免费校验 |
| GET 提交敏感数据 | 用 POST |

---

## 6. 全局属性

| 属性 | 作用 |
|------|------|
| `id` | 唯一标识（锚点/JS 选择） |
| `class` | 样式类（可多个） |
| `data-*` | 自定义数据（JS 读取） |
| `hidden` | 隐藏元素 |
| `title` | 悬浮提示 |
| `tabindex` | Tab 顺序（可访问性） |
| `lang` | 元素语言 |
| `dir` | 文本方向（rtl/ltr） |

```html
<!-- data-* 的典型应用 -->
<button data-user-id="10086" onclick="handleClick(this)">
    查看用户
</button>
<script>
function handleClick(btn) {
    console.log(btn.dataset.userId);  // "10086"
}
</script>
```

> 🎯 **核心要点**：六大标签族——文本（strong/em 语义优先）、列表（ul/ol/dl）、链接（rel=noopener 安全）、图片（alt 必填 + lazy）、表格（thead/tbody 语义 + scope）、表单（label 配对 + 原生校验）。全局属性 data-* 是 JS 通信的桥梁。

---

**返回总览**：[00-HTML总览与语义化](00-HTML总览与语义化.md) | **下一篇**：[02-HTML进阶多媒体与可访问性](02-HTML进阶多媒体与可访问性.md)
