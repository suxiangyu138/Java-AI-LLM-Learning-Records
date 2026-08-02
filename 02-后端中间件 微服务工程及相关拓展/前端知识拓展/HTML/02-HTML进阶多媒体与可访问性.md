# 02 - HTML 进阶：多媒体与可访问性

> 定位：Canvas/SVG、表单进阶、Web 组件、ARIA 可访问性、现代 HTML API——从"会写标签"到"写出可访问的高质量页面"

## 📚 目录

1. [Canvas 绘图](#1-canvas-绘图)
2. [SVG 矢量图](#2-svg-矢量图)
3. [表单进阶：原生表单 API](#3-表单进阶原生表单-api)
4. [Web Components](#4-web-components)
5. [ARIA 与可访问性](#5-aria-与可访问性)
6. [现代 HTML API 速查](#6-现代-html-api-速查)

---

## 1. Canvas 绘图

### 1.1 基础

```html
<canvas id="myCanvas" width="400" height="300">
    你的浏览器不支持 Canvas
</canvas>
```

```javascript
const canvas = document.getElementById('myCanvas');
const ctx = canvas.getContext('2d');

// 绘制矩形
ctx.fillStyle = '#4CAF50';
ctx.fillRect(10, 10, 100, 50);

// 绘制路径
ctx.beginPath();
ctx.moveTo(50, 50);
ctx.lineTo(150, 50);
ctx.strokeStyle = 'red';
ctx.stroke();

// 绘制圆
ctx.beginPath();
ctx.arc(200, 150, 40, 0, Math.PI * 2);
ctx.fillStyle = 'blue';
ctx.fill();

// 文本
ctx.font = '20px Arial';
ctx.fillText('Hello Canvas', 50, 200);
```

| 要点 | 说明 |
|------|------|
| 像素级绘图 | 游戏、图表、图像处理 |
| 性能 | 大量绘制用 requestAnimationFrame |
| 高分屏 | canvas.width = 物理像素 × devicePixelRatio |
| 无障碍 | Canvas 内容需提供替代文本（aria-label） |

---

## 2. SVG 矢量图

### 2.1 基础图形

```html
<svg width="200" height="200" viewBox="0 0 100 100">
    <circle cx="50" cy="50" r="40" fill="green"/>
    <rect x="10" y="10" width="30" height="30" fill="blue"/>
    <path d="M10 80 L50 80 L30 60 Z" fill="orange"/>
</svg>
```

### 2.2 Canvas vs SVG

| 维度 | Canvas | SVG |
|------|:---:|:---:|
| 本质 | 像素位图 | 矢量 DOM |
| 缩放 | 模糊 | 无损 |
| 事件 | 需自行命中检测 | 元素级事件 |
| 性能 | 大量元素更快 | 少量元素更快 |
| 适用 | 游戏/图表/图像 | 图标/Logo/数据可视化 |

> 🎯 **要点**：图标用 SVG（可缩放 + 可样式化）；高频动画用 Canvas。两者都支持 `<img>` 引入外部文件。

---

## 3. 表单进阶：原生表单 API

### 3.1 Constraint Validation（约束校验）

```javascript
const form = document.getElementById('myForm');

// 校验整个表单
form.checkValidity();          // true/false

// 单控件校验
const email = document.getElementById('email');
email.validity.valid;          // 是否通过
email.validity.typeMismatch;   // 格式错误
email.validationMessage;       // 错误消息

// 自定义校验消息
email.setCustomValidity('邮箱格式不正确');
```

### 3.2 FormData 与提交

```javascript
// 收集表单数据（含文件）
const form = document.getElementById('myForm');
form.addEventListener('submit', async (e) => {
    e.preventDefault();                        // 阻止默认提交
    const data = new FormData(form);

    // FormData → 普通对象
    const obj = Object.fromEntries(data);

    // 上传文件（multipart）
    const resp = await fetch('/api/upload', {
        method: 'POST',
        body: data,                            // ⚠️ 不要手动设 Content-Type
    });
});

// 表单重置与序列化
form.reset();                                  // 重置
```

---

## 4. Web Components

### 4.1 自定义元素

```javascript
// 定义自定义元素
class UserCard extends HTMLElement {
    connectedCallback() {                      // 挂载时
        this.innerHTML = `
            <div class="card">
                <strong>${this.getAttribute('name')}</strong>
            </div>`;
    }
}
customElements.define('user-card', UserCard);
```

```html
<!-- 使用自定义元素 -->
<user-card name="张三"></user-card>
```

### 4.2 Shadow DOM 与模板

```html
<template id="cardTemplate">
    <style>
        .card { border: 1px solid #ccc; padding: 10px; }
    </style>
    <div class="card">
        <slot name="title">默认标题</slot>
    </div>
</template>
```

```javascript
// Shadow DOM：样式隔离（外部 CSS 不影响内部）
const host = document.getElementById('host');
const shadow = host.attachShadow({ mode: 'open' });
shadow.appendChild(document.getElementById('cardTemplate').content.cloneNode(true));
```

| 组件技术 | 作用 |
|---------|------|
| Custom Elements | 自定义标签 |
| Shadow DOM | 样式与 DOM 隔离 |
| Template/Slot | 模板复用 + 插槽 |

> 🎯 **要点**：Web Components 是浏览器原生组件标准——框架无关、可复用；三大技术组合使用。Vue/React 组件与其理念相通。

---

## 5. ARIA 与可访问性

### 5.1 ARIA 是什么

```
ARIA（Accessible Rich Internet Applications）：
  为动态内容补充"语义"的规范
  让屏幕阅读器理解自定义组件

⚠️ 面试必答：
"ARIA 是给'非语义元素'补语义的规范——
 自定义组件（div 模拟按钮）需要
 role/aria-label 等让读屏软件可理解。"
```

### 5.2 常用 ARIA 属性

```html
<!-- role：角色声明 -->
<div role="button" tabindex="0" onclick="...">点击</div>

<!-- aria-label：无文本元素的可访问名称 -->
<button aria-label="关闭对话框">×</button>

<!-- aria-hidden：隐藏装饰性内容 -->
<i class="icon" aria-hidden="true"></i>

<!-- aria-live：动态区域播报（无障碍的杀手锏） -->
<div aria-live="polite" id="status">加载中...</div>

<!-- 对话框 -->
<div role="dialog" aria-modal="true" aria-labelledby="title">
    <h2 id="title">弹窗标题</h2>
</div>
```

### 5.3 可访问性检查清单

| 检查项 | 要求 |
|--------|------|
| 键盘可达 | 所有交互可用 Tab/Enter 操作 |
| 焦点可见 | :focus-visible 样式 |
| 对比度 | 文本 ≥ 4.5:1 |
| 表单标签 | 所有控件有 label |
| 图片 alt | 信息图必填 |
| prefers-reduced-motion | 尊重减少动画偏好 |
| 语义优先 | 能用原生标签不用 ARIA |

> 🎯 **要点**："能用原生标签就不用 ARIA"——`<button>` 免费获得键盘/焦点/读屏支持，`<div role="button">` 要手写全部。ARIA 是补丁不是替代。

---

## 6. 现代 HTML API 速查

| API | 用途 | 示例 |
|-----|------|------|
| `<dialog>` | 原生对话框 | `dialog.showModal()` |
| `<details>/<summary>` | 原生折叠 | 免 JS 手风琴 |
| `<popover>` | 弹出层（2024+） | `popover="auto"` |
| View Transitions | 页面过渡（2024+） | `document.startViewTransition()` |
| `<search>` | 搜索区（语义） | 与 form 区分 |
| `inert` | 禁用交互区 | 模态背后的内容 |
| `contenteditable` | 富文本编辑 | 需自行处理粘贴安全 |

```html
<!-- 原生 dialog（2026 基线广泛支持） -->
<dialog id="myDialog">
    <h2>提示</h2>
    <p>这是原生对话框</p>
    <button onclick="this.closest('dialog').close()">关闭</button>
</dialog>
<button onclick="document.getElementById('myDialog').showModal()">打开</button>
```

```html
<!-- 原生折叠（零 JS） -->
<details>
    <summary>点击展开</summary>
    <p>折叠内容——details 元素免费提供展开/收起</p>
</details>
```

> 🎯 **核心要点**：HTML 进阶四块——Canvas（像素绘图）/SVG（矢量）、原生表单 API（校验/FormData）、Web Components（自定义元素 + Shadow DOM）、ARIA（可访问性补语义）。2026 年 `<dialog>`/`<details>`/popover 等原生组件已广泛可用，**优先原生、ARIA 兜底**是写页面的现代原则。

---

**返回总览**：[00-HTML总览与语义化](00-HTML总览与语义化.md) | **上一篇**：[01-HTML基础标签详解](01-HTML基础标签详解.md)
