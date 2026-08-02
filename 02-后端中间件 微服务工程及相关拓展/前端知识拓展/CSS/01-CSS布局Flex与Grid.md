# 01 - CSS 布局：Flex 与 Grid

> 定位：从文档流到现代布局双雄——Flexbox（一维布局）与 Grid（二维布局）的完整体系、定位机制、响应式布局策略

## 📚 目录

1. [文档流与布局基础](#1-文档流与布局基础)
2. [Flexbox：一维布局](#2-flexbox一维布局)
3. [Grid：二维布局](#3-grid二维布局)
4. [定位机制](#4-定位机制)
5. [响应式布局策略](#5-响应式布局策略)
6. [Flex vs Grid 选型](#6-flex-vs-grid-选型)

---

## 1. 文档流与布局基础

### 1.1 文档流

```
正常文档流：
  块级元素（div/p/h1）：独占一行，自上而下
  行内元素（span/a/img）：从左到右，一行放不下换行

改变流：
  display: inline / block / inline-block / flex / grid / none
  float（历史遗留，现代布局不用）

⚠️ 面试必答：
"布局的第一步是理解文档流——
 块级占行、行内排布；现代布局
 用 Flex/Grid 替代 float/定位硬排。"
```

### 1.2 display 速查

| display | 行为 |
|---------|------|
| block | 独占行，可设宽高 |
| inline | 行内，宽高无效 |
| inline-block | 行内但可设宽高 |
| flex | 弹性一维布局 |
| grid | 网格二维布局 |
| none | 不渲染（vs hidden 仍占位） |

---

## 2. Flexbox：一维布局

### 2.1 主轴与交叉轴

```
容器设置 display: flex：
  主轴（main axis）：默认水平（flex-direction: row）
  交叉轴（cross axis）：垂直

属性分布：
  容器属性（作用于整体排列）
  项目属性（作用于单个元素）
```

### 2.2 容器属性

```css
.container {
    display: flex;

    /* 方向 */
    flex-direction: row | column | row-reverse | column-reverse;

    /* 换行 */
    flex-wrap: nowrap | wrap;

    /* 主轴对齐 */
    justify-content:
        flex-start | flex-end | center | space-between | space-around | space-evenly;

    /* 交叉轴对齐 */
    align-items: flex-start | center | stretch | baseline;

    /* 多行交叉轴 */
    align-content: center | space-between;
}
```

### 2.3 项目属性

```css
.item {
    /* 放大比例（默认 0 不放大） */
    flex-grow: 1;
    /* 缩小比例（默认 1 可缩小） */
    flex-shrink: 0;
    /* 基础尺寸 */
    flex-basis: 200px;

    /* 简写（常用）：grow shrink basis */
    flex: 1;              /* 1 1 0%：等分剩余空间 */
    flex: 0 0 200px;      /* 固定 200px 不伸缩 */

    /* 排序与单元素对齐 */
    order: -1;            /* 数值小的在前 */
    align-self: center;   /* 覆盖容器的 align-items */
}
```

### 2.4 经典布局三连

```css
/* ① 水平垂直居中（Flex 最经典） */
.center {
    display: flex;
    justify-content: center;
    align-items: center;
}

/* ② 等分布局 */
.row {
    display: flex;
    gap: 16px;            /* ⚠️ gap 现代浏览器全支持 */
}
.row > * { flex: 1; }

/* ③ 圣杯布局（头部 + 左右 + 主体） */
.page { display: flex; flex-direction: column; min-height: 100vh; }
.content { display: flex; flex: 1; }
.sidebar { width: 200px; }
.main { flex: 1; }
```

> 🎯 **要点**：`gap`（2021 全支持）替代 margin 做间距；`flex: 1` 是"等分剩余空间"的最常用简写。Flex 解决"一维排列 + 对齐"的一切问题。

---

## 3. Grid：二维布局

### 3.1 网格定义

```css
.grid {
    display: grid;
    /* 列定义 */
    grid-template-columns: 1fr 2fr 1fr;   /* 3 列，中间 2 倍宽 */
    grid-template-columns: repeat(3, 1fr); /* 等分 3 列 */
    grid-template-columns: 200px 1fr;      /* 固定 + 自适应 */

    /* 行定义 */
    grid-template-rows: auto 1fr auto;     /* 头/主/脚 */

    /* 间距 */
    gap: 16px;

    /* 自动行高 */
    grid-auto-rows: minmax(100px, auto);
}
```

### 3.2 区域与定位

```css
/* ① 命名区域（推荐，语义清晰） */
.layout {
    display: grid;
    grid-template-columns: 200px 1fr;
    grid-template-rows: 60px 1fr 60px;
    grid-template-areas:
        "header header"
        "sidebar main"
        "footer footer";
}
.header  { grid-area: header; }
.sidebar { grid-area: sidebar; }
.main    { grid-area: main; }
.footer  { grid-area: footer; }

/* ② 行号定位 */
.item {
    grid-column: 1 / 3;    /* 从列线 1 到 3 */
    grid-row: 2 / 4;
}

/* ③ 自动填充（响应式网格核心） */
.cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
    /* ⚠️ auto-fill + minmax = 自动适应列数（免媒体查询） */
}
```

> 🎯 **要点**：`grid-template-areas` 让布局"看得见"；`repeat(auto-fill, minmax(200px, 1fr))` 是响应式卡片网格的标准答案。

---

## 4. 定位机制

```css
/* position 五种 */
.static   { position: static; }     /* 默认，文档流 */
.relative { position: relative; }   /* 相对自身偏移（不脱离流） */
.absolute { position: absolute; }   /* 相对最近定位祖先（脱离流） */
.fixed    { position: fixed; }      /* 相对视口（导航/弹窗） */
.sticky   { position: sticky; }     /* 滚动吸附（吸顶导航） */

/* z-index：仅对定位元素生效 */
.popup { position: absolute; z-index: 100; }

/* 堆叠上下文：z-index + opacity/transform 等会创建 */
```

| 易错点 | 说明 |
|--------|------|
| absolute 找错祖先 | 最近"定位"祖先（relative 常用作锚） |
| z-index 失效 | 检查是否创建堆叠上下文 |
| sticky 不生效 | 父容器高度不够/overflow 限制 |

---

## 5. 响应式布局策略

### 5.1 媒体查询

```css
/* 断点策略：移动优先（默认移动，向上增强） */
/* 默认（移动端）样式 */

@media (min-width: 768px) {
    /* 平板 */
    .cards { grid-template-columns: repeat(2, 1fr); }
}

@media (min-width: 1024px) {
    /* 桌面 */
    .cards { grid-template-columns: repeat(3, 1fr); }
}
```

### 5.2 现代响应式组合拳

```
① 视口单位 + clamp()：流体字号
② auto-fill + minmax：自适应网格（免媒体查询）
③ 容器查询：组件级响应（见 03 现代 CSS）
④ Flex + wrap：流式排列

⚠️ 面试必答：
"现代响应式 = 移动优先 + 断点增强；
 容器查询把'视口响应'升级为
 '容器响应'（组件自适应）。"
```

---

## 6. Flex vs Grid 选型

| 维度 | Flexbox | Grid |
|------|:---:|:---:|
| 维度 | 一维（单轴） | 二维（行列） |
| 内容驱动 | 内容撑开 | 轨道定义 |
| 适用 | 导航、按钮组、居中对齐 | 页面骨架、卡片网格、复杂布局 |
| 嵌套 | Flex 内可嵌 Grid | Grid 内可嵌 Flex |

```
选型口诀：
  一行/一列排列 → Flex
  行列矩阵 → Grid
  页面级骨架 → Grid（areas）
  组件内细节 → Flex

⚠️ 面试必答：
"'一维用 Flex、二维用 Grid'——
 Flex 管组件内部、Grid 管页面骨架，
 两者嵌套使用是常态。"
```

---

> 🎯 **核心要点**：布局体系 = **文档流**（基础认知）+ **Flex**（一维：justify/align/gap/flex:1）+ **Grid**（二维：areas/auto-fill+minmax）+ **定位**（relative 锚点/absolute 弹层/sticky 吸顶）+ **响应式**（移动优先 + 容器查询）。现代布局的答案：**页面骨架 Grid、组件细节 Flex**。

---

**返回总览**：[00-CSS总览与核心概念](00-CSS总览与核心概念.md) | **下一篇**：[02-CSS进阶动画与视觉](02-CSS进阶动画与视觉.md)
