# 02-HTML核心知识点
> 🎯 后端只需30分钟掌握HTML骨架 — 看懂页面结构、表单提交、数据属性，足够和前端高效沟通

---

## 目录
1. [HTML文档结构](#1-html文档结构)
2. [常用标签速查](#2-常用标签速查)
3. [表单与输入 — 后端最需要懂的](#3-表单与输入--后端最需要懂的)
4. [HTML5 API（后端友好）](#4-html5-api后端友好)
5. [与后端协作要点](#5-与后端协作要点)

---

## 1. HTML文档结构

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>页面标题</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <!-- 页面可见内容 -->
    <div id="app">
        <h1>Hello World</h1>
    </div>
    <script src="app.js"></script>
</body>
</html>
```

| 区域 | 后端需要知道 |
|------|-------------|
| `<head>` | 字符集(`<meta charset>`)、视口(`viewport`)、SEO标签 |
| `<body>` | 所有可见内容，前端框架(React/Vue)通常只渲染到`<div id="app">` |
| `<script>` | JS加载位置影响页面渲染，`defer`/`async`属性 |

---

## 2. 常用标签速查

### 2.1 结构标签

| 标签 | 用途 | 后端关联 |
|------|------|----------|
| `<div>` | 通用容器 | 前端框架挂载点 `id="app"` |
| `<span>` | 行内容器 | 显示后端返回的文本 |
| `<a href="/users">` | 超链接 | URL路由与后端Path对应 |
| `<img src="url">` | 图片 | src指向后端静态资源/OSS |
| `<ul>/<ol>/<li>` | 列表 | 遍历后端返回的数组渲染 |

### 2.2 语义化标签（HTML5）

```html
<header>    <!-- 页头 -->
<nav>       <!-- 导航 -->
<main>      <!-- 主体内容 -->
<section>   <!-- 内容区块 -->
<article>   <!-- 独立文章 -->
<aside>     <!-- 侧边栏 -->
<footer>    <!-- 页脚 -->
```

> 💡 后端理解：这些标签对后端透明，但前端用CSS选择器定位元素时常用。看到`<section>`就知道这是一个内容区块。

---

## 3. 表单与输入 — 后端最需要懂的

### 3.1 核心表单元素

```html
<form action="/api/users" method="POST" enctype="multipart/form-data">
    <!-- 文本输入 -->
    <input type="text" name="username" placeholder="请输入用户名" required>
    
    <!-- 密码 -->
    <input type="password" name="password" minlength="6">
    
    <!-- 数字 -->
    <input type="number" name="age" min="1" max="150">
    
    <!-- 邮箱（浏览器自带格式校验） -->
    <input type="email" name="email">
    
    <!-- 单选 -->
    <input type="radio" name="gender" value="male"> 男
    <input type="radio" name="gender" value="female"> 女
    
    <!-- 多选 -->
    <input type="checkbox" name="hobbies" value="sports"> 运动
    <input type="checkbox" name="hobbies" value="music"> 音乐
    
    <!-- 下拉 -->
    <select name="city">
        <option value="beijing">北京</option>
        <option value="shanghai">上海</option>
    </select>
    
    <!-- 文件上传 -->
    <input type="file" name="avatar" accept="image/*">
    
    <!-- 提交按钮 -->
    <button type="submit">提交</button>
</form>
```

### 3.2 表单提交方式与后端接收

| 表单 enctype | Content-Type | 后端接收 |
|-------------|-------------|----------|
| 默认(不设置) | `application/x-www-form-urlencoded` | `@RequestParam` |
| `multipart/form-data` | `multipart/form-data` | `@RequestParam` + `@RequestParam MultipartFile` |
| 不用表单,用JS | `application/json` | `@RequestBody` |

```java
// 后端对应代码
@PostMapping("/api/users")
public void createUser(
    @RequestParam String username,      // ← name="username"
    @RequestParam MultipartFile avatar  // ← name="avatar"
) { ... }
```

### 3.3 HTML5 输入校验

```html
<!-- 前端校验属性（后端仍需做服务端校验！） -->
<input required>           <!-- 必填 -->
<input minlength="3">      <!-- 最小长度 -->
<input maxlength="20">     <!-- 最大长度 -->
<input pattern="[A-Za-z]+"> <!-- 正则 -->
```

> ⚠️ **后端必知**：前端校验只是用户体验优化！任何人都可以通过Postman/curl绕过前端校验直接发送恶意数据，**后端必须独立做一次校验**。

---

## 4. HTML5 API（后端友好）

### 4.1 data-* 自定义属性

```html
<!-- 前端常用data-*传递后端数据给JS -->
<div data-user-id="123" data-user-role="admin">
    张三
</div>
```

```javascript
// JS读取
const userId = element.dataset.userId; // "123"
```

> 💡 后端理解：Thymeleaf等模板引擎常把后端数据渲染到data-*属性，前端JS再读取。

### 4.2 Web Storage

```javascript
// 浏览器端存储（不发送给服务器，不同于Cookie！）
localStorage.setItem('token', 'xxx');     // 持久存储
sessionStorage.setItem('temp', 'yyy');    // 关闭标签页即清除
```

---

## 5. 与后端协作要点

| 协作点 | 后端需要知道 |
|--------|------------|
| **表单字段名** | `name` 属性 → 后端 `@RequestParam` 的名称 |
| **提交方式** | `method` 决定HTTP方法(GET/POST)，注意GET会把参数放URL |
| **编码方式** | `enctype` 决定Content-Type，影响后端如何解析 |
| **前后端渲染** | SSR(服务端渲染Thymeleaf) → `th:text`；SPA(前后端分离) → 前端框架接管 |
| **SEO** | SSR对SEO友好，SPA需额外处理(SSR/预渲染) |

---

> 🎯 **后端学HTML的最小集合**：能看懂DOM结构 + 理解表单提交与后端接收的对应关系 + 知道data-*属性是前后端数据桥梁。
