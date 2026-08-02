# 06 - JavaScript DOM 与浏览器 API

> 定位：DOM 操作、事件体系、存储、Fetch 网络、BOM、现代 Web API——浏览器端实战能力

## 📚 目录

1. [DOM 基础与选择](#1-dom-基础与选择)
2. [DOM 操作](#2-dom-操作)
3. [事件体系](#3-事件体系)
4. [存储与缓存](#4-存储与缓存)
5. [Fetch 与网络](#5-fetch-与网络)
6. [BOM 与现代 API](#6-bom-与现代-api)

---

## 1. DOM 基础与选择

### 1.1 DOM 树

```
DOM = 文档对象模型（HTML 的内存树形表示）
  document → html → body → div → ...

⚠️ 面试必答：
"DOM 是 HTML 的编程接口——
 浏览器解析 HTML 生成 DOM 树，
 JS 通过 DOM API 读写页面。"
```

### 1.2 元素选择

```javascript
// 现代选择（推荐）
document.querySelector('.card');            // 第一个匹配
document.querySelectorAll('.card');         // 全部（NodeList）

// 传统选择
document.getElementById('app');             // 最快
document.getElementsByClassName('card');    // 动态集合 ⚠️
document.getElementsByTagName('div');

// 关系遍历
element.parentElement
element.children            // 子元素（HTMLCollection）
element.firstElementChild
element.nextElementSibling
element.closest('.wrapper') // ⚠️ 向上查找（事件委托常用）
```

---

## 2. DOM 操作

### 2.1 创建与插入

```javascript
// 创建
const div = document.createElement('div');
div.textContent = '新内容';
div.className = 'card';

// 插入
parent.appendChild(div);          // 尾部
parent.prepend(div);              // 头部
parent.insertBefore(div, ref);    // 指定位置
div.insertAdjacentHTML('beforeend', '<span>HTML</span>');

// 删除
div.remove();                     // 现代直接删

// ⚠️ 性能：批量插入用 DocumentFragment
const fragment = document.createDocumentFragment();
for (const item of items) {
    const el = document.createElement('li');
    el.textContent = item;
    fragment.appendChild(el);     // 不触发重排
}
list.appendChild(fragment);       // 一次插入
```

### 2.2 属性与样式

```javascript
// 属性
el.id = 'main';
el.className = 'a b';
el.classList.add('active');       // ✅ 推荐
el.classList.toggle('active');
el.dataset.userId = '100';        // data-* 属性

// 内联样式（推荐 class 切换，少用 style）
el.style.color = 'red';
el.style.transform = 'translateY(10px)';   // 驼峰命名

// 计算样式
getComputedStyle(el).color;       // 最终生效样式（只读）

// 尺寸与位置
el.offsetWidth / el.offsetHeight;  // 含 padding/border
el.getBoundingClientRect();        // 视口位置
window.scrollY;                    // 滚动位置
```

---

## 3. 事件体系

### 3.1 事件绑定

```javascript
// 现代绑定
el.addEventListener('click', handler, { once: true });
// 选项：once 一次、passive 不阻止默认（滚动性能）、capture 捕获阶段

// 移除
el.removeEventListener('click', handler);

// ⚠️ 内联 onclick="..." 不推荐（耦合 + 无移除）
```

### 3.2 事件流与委托

```
事件传播三阶段：
  捕获（capture）→ 目标（target）→ 冒泡（bubble）

⚠️ 面试必答：
"事件三阶段——捕获从根到目标、
 冒泡从目标到根；
 addEventListener 默认绑定冒泡阶段
 （第三个参数 true 改为捕获）。"
```

```javascript
// 事件委托（性能优化核心）
// 原理：利用冒泡，父元素统一处理子元素事件
document.querySelector('#list').addEventListener('click', (e) => {
    const item = e.target.closest('li');   // ⚠️ 找实际点击的 li
    if (item) handleItem(item.dataset.id);
});
// 优点：动态元素无需重新绑定、事件数量从 N 降到 1

// 自定义事件
const event = new CustomEvent('user-login', { detail: { id: 1 } });
window.dispatchEvent(event);
window.addEventListener('user-login', (e) => console.log(e.detail));
```

> 🎯 **要点**：事件委托（冒泡 + closest）是列表/表格事件处理的标准答案——动态元素、性能、解耦三赢。

---

## 4. 存储与缓存

### 4.1 存储对比

| 存储 | 容量 | 持久 | 特点 |
|------|:---:|:---:|------|
| localStorage | ~5MB | ✅ 永久 | 同步、字符串 |
| sessionStorage | ~5MB | 会话 | 标签页关闭即清 |
| cookie | ~4KB | 可设 | 随请求发送（httpOnly） |
| IndexedDB | 大 | ✅ | 异步、结构化 |

```javascript
// localStorage 使用
localStorage.setItem('token', 'abc');
localStorage.getItem('token');
localStorage.removeItem('token');
localStorage.clear();

// ⚠️ 只存字符串 → 对象要 JSON 序列化
localStorage.setItem('user', JSON.stringify(user));
const user = JSON.parse(localStorage.getItem('user'));

// sessionStorage 同 API（会话级）
```

### 4.2 缓存策略

```
浏览器缓存层级：
  Memory Cache（内存）→ Disk Cache（磁盘）→ 网络

HTTP 缓存控制（服务端主导）：
  Cache-Control: max-age=3600     // 强缓存
  ETag / Last-Modified            // 协商缓存

⚠️ 面试必答：
"缓存两级——强缓存（max-age，不发请求）
 与协商缓存（ETag，304 复用）；
 前端工程用 hash 文件名破坏强缓存。"
```

---

## 5. Fetch 与网络

### 5.1 Fetch API

```javascript
// 基础 GET
const resp = await fetch('/api/users');
if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
const users = await resp.json();

// POST + JSON
const resp = await fetch('/api/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: '张三' }),
});

// 表单/文件上传（自动 multipart）
const formData = new FormData();
formData.append('file', fileInput.files[0]);
await fetch('/api/upload', { method: 'POST', body: formData });

// 超时与取消（AbortController）
const controller = new AbortController();
setTimeout(() => controller.abort(), 5000);
await fetch(url, { signal: controller.signal });

// 并发
const [a, b] = await Promise.all([
    fetch('/api/a').then(r => r.json()),
    fetch('/api/b').then(r => r.json()),
]);
```

### 5.2 跨域 CORS

```
浏览器同源策略：协议 + 域名 + 端口必须一致

跨域解决方案：
  ① CORS（服务端配置 Access-Control-Allow-Origin）✅ 标准
  ② 反向代理（开发环境 vite/webpack proxy）✅ 常见
  ③ JSONP（历史，GET only）
  ④ postMessage（iframe 通信）

⚠️ 面试必答：
"跨域是浏览器的安全策略——
 生产用 CORS、开发用代理；
 JSONP 是历史方案（只能 GET）。"
```

---

## 6. BOM 与现代 API

### 6.1 BOM 速查

```javascript
// 窗口
window.innerWidth / innerHeight;   // 视口尺寸
window.location.href;              // 当前 URL
window.location.search;            // 查询参数

// 历史与跳转
history.pushState({}, '', '/new-url');   // 不刷新改 URL（SPA 路由基础）
history.back();

// 定时器
setTimeout(fn, 1000);
setInterval(fn, 1000);
requestAnimationFrame(step);       // ⚠️ 动画专用（60fps 跟随刷新率）
```

### 6.2 现代 Web API（2026 常用）

| API | 用途 |
|-----|------|
| IntersectionObserver | 懒加载/曝光埋点（替代 scroll 监听） |
| ResizeObserver | 元素尺寸变化（替代 window.resize） |
| MutationObserver | DOM 变化监听 |
| Web Storage / IndexedDB | 存储 |
| WebSocket | 实时通信 |
| View Transitions | 页面过渡（CSS 篇详述） |
| Notification | 浏览器通知 |

```javascript
// 懒加载（IntersectionObserver 标准姿势）
const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            img.src = img.dataset.src;      // 进入视口才加载
            observer.unobserve(img);
        }
    });
}, { rootMargin: '100px' });
observer.observe(img);

// ResizeObserver：容器尺寸响应
const ro = new ResizeObserver(entries => {
    for (const entry of entries) {
        console.log(entry.contentRect.width);
    }
});
ro.observe(element);
```

> 🎯 **要点**：现代 API 替代"滚动/窗口监听 + 手算"——**IntersectionObserver**（懒加载/埋点）、**ResizeObserver**（容器响应）是性能优化的标准答案。

---

> 🎯 **核心要点**：DOM/浏览器体系 = **选择与操作**（querySelector + classList + Fragment 批量）、**事件**（三阶段 + 委托）、**存储**（localStorage + JSON）、**网络**（Fetch + AbortController + CORS）、**现代 API**（Intersection/Resize Observer）。前端实战能力的四大支柱。

---

**返回总览**：[00-JavaScript总览与核心概念](00-JavaScript总览与核心概念.md) | **上一篇**：[05-JavaScriptES6进阶特性](05-JavaScriptES6进阶特性.md) | **下一篇**：[07-JavaScriptES2025与ES2026新特性](07-JavaScriptES2025与ES2026新特性.md)
