# Web 前端基础

## 📌 课程定位
后端工程师需要懂前端基础——不是为了写页面，而是为了更好地设计API、排查问题、与前端协作。理解HTTP交互、浏览器原理、HTML/CSS/JS基础是后端开发的必修课。

## 🎯 核心章节

### 1. HTML 核心
- **HTML5 语义化标签**：`<header>`、`<nav>`、`<main>`、`<article>`、`<section>`、`<footer>`——替代无意义的div
- **表单元素**：`<form>`、`<input>`(text/radio/checkbox/file)、`<select>`、`<textarea>`
  - 属性：name(提交时的key)、required、placeholder
- **DOM 树**：浏览器将HTML解析为树状结构——JavaScript通过DOM操作页面

### 2. CSS 核心
- **选择器**：标签选择器、类选择器(.class)、ID选择器(#id)、属性选择器、后代/子代选择器
- **盒模型（⭐ 重要）**：content + padding + border + margin
  - `box-sizing: border-box`——宽高包含padding和border(更符合直觉)
- **布局**：
  - **Flexbox**（一维布局）：`display: flex` + `justify-content`(主轴) + `align-items`(交叉轴)
  - **Grid**（二维布局）：`display: grid` + `grid-template-columns`——强大但复杂
- **响应式设计**：`@media (max-width: 768px) { ... }`——不同屏幕尺寸不同样式

### 3. JavaScript 核心（后端需要懂的）
- **基本语法**：`let`/`const` vs `var`（块作用域）、箭头函数`()=>{}`、解构赋值、模板字符串
- **异步编程**（⭐ 重点）：
  - **回调函数**：最早的异步方式——回调地狱
  - **Promise**：`.then().catch()` 链式调用——状态扭转(pending→fulfilled/rejected)
  - **async/await**：写起来像同步的异步代码——`const data = await fetch(url);`
- **fetch API**：浏览器发起HTTP请求——`fetch(url, {method:'POST', body: JSON.stringify(data)})`
- **同源策略与跨域**：
  - 同源：协议+域名+端口全相同
  - CORS：服务端设置 `Access-Control-Allow-Origin`响应头
  - JSONP：古老跨域方案——利用script标签不受同源限制

### 4. 浏览器原理（后端需要懂的）
- **从输入URL到页面显示（⭐ 经典面试题）**：
  1. DNS解析：域名→IP
  2. TCP三次握手建立连接
  3. TLS握手(如果是HTTPS)
  4. 发送HTTP请求
  5. 服务器处理请求→返回HTTP响应
  6. 浏览器解析HTML→构建DOM树
  7. 解析CSS→构建CSSOM树
  8. DOM+CSSOM→Render树→布局(Layout)→绘制(Paint)→合成(Composite)
- **浏览器缓存**：
  - 强缓存：`Cache-Control: max-age=3600` 或 `Expires`——不过期直接用
  - 协商缓存：`ETag`/`Last-Modified`——向服务器确认是否过期，304 Not Modified
- **Cookie/Web Storage**：
  | Cookie | localStorage | sessionStorage |
  |--------|-------------|----------------|
  | 4KB，每次请求自动携带 | 5MB，持久存储 | 5MB，标签页关闭清除 |
  | 用于身份认证 | 用于本地数据持久化 | 用于临时状态 |

### 5. 前后端交互
- **RESTful API 设计**：资源用名词(users)、操作用HTTP方法(GET/POST/PUT/DELETE)
- **JSON**：前后端数据交换格式——`{"key": "value"}`
- **状态码的正确使用**：201(Created)、204(No Content)、400/422(参数错误)、401/403(权限)
- **常见坑**：CORS配置忘记加OPTIONS预检请求、Cookie的SameSite/Secure属性

## ✅ 学习建议
- 重点掌握HTTP交互和浏览器原理(从输入URL到页面显示)
- JS不需要深耕，但async/await和fetch要能看懂
- 用浏览器DevTools(Network标签)观察HTTP请求——后端调试利器
- MDN Web Docs 是最好的前端参考文档
