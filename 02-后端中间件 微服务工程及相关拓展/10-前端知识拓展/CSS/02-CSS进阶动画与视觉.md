# 02 - CSS 进阶：动画与视觉

> 定位：过渡/动画/变换三件套、渐变与阴影、BFC 与层叠上下文、视觉设计模式——从"能布局"到"会做效果"

## 📚 目录

1. [过渡 transition](#1-过渡-transition)
2. [动画 animation](#2-动画-animation)
3. [变换 transform](#3-变换-transform)
4. [渐变与阴影](#4-渐变与阴影)
5. [BFC 与层叠上下文](#5-bfc-与层叠上下文)
6. [视觉设计模式](#6-视觉设计模式)

---

## 1. 过渡 transition

### 1.1 基础语法

```css
.button {
    background: #4CAF50;
    /* 属性 时长 缓动 延迟 */
    transition: background 0.3s ease, transform 0.3s ease;
    /* 简写：全部属性 */
    transition: all 0.3s ease;
}
.button:hover {
    background: #45a049;
    transform: scale(1.05);
}
```

### 1.2 缓动函数

| 函数 | 效果 |
|------|------|
| ease | 慢-快-慢（默认） |
| linear | 匀速 |
| ease-in | 加速（出场） |
| ease-out | 减速（入场） |
| ease-in-out | 对称缓动 |
| cubic-bezier() | 自定义贝塞尔 |
| steps() | 逐帧（打字机） |

```css
/* 动画性能提示：只过渡 transform/opacity（GPU 合成） */
.card { transition: transform 0.3s ease, opacity 0.3s ease; }
/* ❌ 不要过渡 width/height/top（触发重排） */
```

> 🎯 **要点**：过渡性能三原则——只动 `transform/opacity`（GPU 层）、`will-change` 提示合成层、避免布局属性动画。这是 2026 前端性能面试必答点。

---

## 2. 动画 animation

### 2.1 keyframes

```css
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(20px); }
    to   { opacity: 1; transform: translateY(0); }
}

.hero {
    animation: fadeIn 0.6s ease-out;
    /* 动画 时长 缓动 延迟 次数 方向 填充模式 */
    animation: fadeIn 0.6s ease-out 0.2s 1 normal both;
}
```

### 2.2 动画属性全解

```css
.animated {
    animation-name: fadeIn;        /* keyframes 名 */
    animation-duration: 1s;        /* 时长 */
    animation-timing-function: ease;   /* 缓动 */
    animation-delay: 0.5s;         /* 延迟 */
    animation-iteration-count: infinite;  /* 次数/无限 */
    animation-direction: alternate;      /* 正/反/交替 */
    animation-fill-mode: both;     /* 填充（开始/结束态） */
    animation-play-state: paused;  /* 播放/暂停 */
}
```

### 2.3 经典动画模式

```css
/* 呼吸灯 */
@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}

/* 骨架屏 */
@keyframes shimmer {
    0% { background-position: -200% 0; }
    100% { background-position: 200% 0; }
}
.skeleton {
    background: linear-gradient(90deg, #eee 25%, #f5f5f5 50%, #eee 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}

/* 尊重减少动画偏好（无障碍） */
@media (prefers-reduced-motion: reduce) {
    * { animation: none !important; transition: none !important; }
}
```

---

## 3. 变换 transform

### 3.1 常用变换

```css
.item {
    /* 位移 / 缩放 / 旋转 / 倾斜 */
    transform: translate(10px, 20px);
    transform: scale(1.2);
    transform: rotate(45deg);
    transform: skew(10deg);

    /* 组合（从右往左应用） */
    transform: translateY(10px) scale(1.1);

    /* 3D */
    transform: translateZ(50px) rotateX(30deg);
    transform-style: preserve-3d;    /* 3D 上下文 */
    perspective: 800px;              /* 透视（父级） */
}
```

### 3.2 变换与动画的黄金组合

```css
/* 悬浮卡片 */
.card {
    transition: transform 0.3s ease;
}
.card:hover {
    transform: translateY(-4px);    /* GPU 合成，流畅 */
}
```

> 🎯 **要点**：`transform` 不触发重排（合成器处理）——**动画性能的基石**。位移优先用 transform 而非 top/left。

---

## 4. 渐变与阴影

### 4.1 渐变

```css
/* 线性渐变 */
background: linear-gradient(90deg, #ff6b6b, #4ecdc4);
background: linear-gradient(to right, red, blue);

/* 径向渐变 */
background: radial-gradient(circle, #fff, #333);

/* 圆锥渐变（色轮） */
background: conic-gradient(red, yellow, green, blue, red);

/* 重复渐变 */
background: repeating-linear-gradient(45deg, #eee 0 10px, #ccc 10px 20px);
```

### 4.2 阴影

```css
/* 盒阴影：x y 模糊 扩散 颜色 */
box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
/* 内阴影 */
box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
/* 多层 */
box-shadow: 0 2px 4px rgba(0,0,0,0.1), 0 8px 24px rgba(0,0,0,0.1);

/* 文字阴影 */
text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);

/* filter 滤镜（与阴影不同，作用于整个元素） */
filter: blur(4px) | brightness(0.8) | grayscale(1) | drop-shadow(0 2px 4px #000);
```

---

## 5. BFC 与层叠上下文

### 5.1 BFC（块级格式化上下文）

```
BFC = 独立的布局环境（内部与外部隔离）

触发 BFC 的条件：
  float 非 none
  overflow 非 visible（hidden/auto/scroll）
  display: inline-block / flex / grid / flow-root
  position: absolute / fixed

BFC 解决三大经典问题：
  ① margin 折叠
  ② 浮动元素撑不起父容器（clearfix 的现代替代）
  ③ 文字环绕控制

⚠️ 面试必答：
"BFC 三能力——防 margin 折叠、包含浮动、
 隔离内外布局；现代触发方式
 display: flow-root 最语义化。"
```

```css
/* 解决父容器高度塌陷（overflow 触发 BFC） */
.parent { overflow: hidden; }
/* 现代推荐 */
.parent { display: flow-root; }
```

### 5.2 层叠上下文

```
层叠上下文 = 独立的 z 轴空间（内部 z-index 不越界）

创建条件：
  z-index + 定位
  opacity < 1
  transform / filter / will-change
  display: flex/grid 的子项 + z-index

⚠️ 面试必答：
"'z-index 失效'多半是层叠上下文——
 transform/opacity 会创建上下文，
 内部 z-index 无法与外部比较。"
```

---

## 6. 视觉设计模式

### 6.1 现代设计速查

```css
/* 卡片（现代 UI 标配） */
.card {
    background: #fff;
    border-radius: 12px;              /* 圆角 */
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    border: 1px solid rgba(0, 0, 0, 0.05);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.card:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 玻璃拟态（Glassmorphism） */
.glass {
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10px);      /* 背景模糊 */
    border: 1px solid rgba(255, 255, 255, 0.3);
}
```

| 模式 | 关键属性 |
|------|---------|
| 卡片悬浮 | transform + box-shadow 过渡 |
| 玻璃拟态 | backdrop-filter: blur |
| 渐变背景 | linear-gradient 组合 |
| 骨架屏 | 渐变 + animation |
| 流光按钮 | 伪元素 + 位移动画 |

> 🎯 **核心要点**：动画视觉四件套——**transition**（状态过渡，只动 transform/opacity）、**animation**（keyframes 关键帧）、**transform**（GPU 合成）、**gradient/shadow**（视觉质感）。BFC 与层叠上下文是两个"隐形机制"——布局异常和 z-index 失效的根因都在这里。

---

**返回总览**：[00-CSS总览与核心概念](00-CSS总览与核心概念.md) | **上一篇**：[01-CSS布局Flex与Grid](01-CSS布局Flex与Grid.md) | **下一篇**：[03-现代CSS与2026新特性](03-现代CSS与2026新特性.md)
