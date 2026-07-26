# 03-CSS核心知识点
> 🎯 后端只需了解CSS的布局模型和调试方法 — 能看懂为什么"页面乱了"，知道什么是Flex布局和响应式设计

---

## 目录
1. [CSS基础概念](#1-css基础概念)
2. [盒模型 — 一切布局的基础](#2-盒模型--一切布局的基础)
3. [Flex布局 — 现代布局标配](#3-flex布局--现代布局标配)
4. [响应式设计基础](#4-响应式设计基础)
5. [后端视角的CSS调试](#5-后端视角的css调试)

---

## 1. CSS基础概念

### 1.1 CSS的三种引入方式

```html
<!-- 1. 内联样式（优先级最高，不推荐） -->
<div style="color: red;">红色文字</div>

<!-- 2. 内部样式表 -->
<style>
    .title { font-size: 20px; }
</style>

<!-- 3. 外部样式表（推荐） -->
<link rel="stylesheet" href="styles.css">

<!-- 优先级：内联 > ID选择器 > 类选择器 > 标签选择器 -->
```

### 1.2 选择器速查

| 选择器 | 语法 | 说明 |
|--------|------|------|
| 类选择器 | `.class-name` | 最常用，多个元素共用 |
| ID选择器 | `#id-name` | 唯一标识 |
| 标签选择器 | `div`, `p`, `span` | 按HTML标签 |
| 后代选择器 | `.parent .child` | 空格分隔 |
| 属性选择器 | `[type="text"]` | 按属性值 |

### 1.3 常用属性

```css
/* 颜色 */
color: #333;              /* 文字颜色 */
background-color: #f5f5f5; /* 背景色 */

/* 文字 */
font-size: 16px;
font-weight: bold;        /* 粗体 */
text-align: center;       /* 居中 */

/* 尺寸 */
width: 300px;
height: 200px;
max-width: 1200px;        /* 最大宽度 */

/* 间距 */
margin: 10px;             /* 外边距（元素到元素） */
padding: 20px;            /* 内边距（内容到边框） */

/* 边框 */
border: 1px solid #ddd;
border-radius: 8px;       /* 圆角 */
```

---

## 2. 盒模型 — 一切布局的基础

```text
每个HTML元素都是一个"盒子"：

┌─────────────────────────────────┐
│         margin (外边距)          │
│  ┌───────────────────────────┐  │
│  │      border (边框)         │  │
│  │  ┌─────────────────────┐  │  │
│  │  │   padding (内边距)   │  │  │
│  │  │  ┌───────────────┐  │  │  │
│  │  │  │   content     │  │  │  │
│  │  │  │   (内容)      │  │  │  │
│  │  │  └───────────────┘  │  │  │
│  │  └─────────────────────┘  │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘

元素总宽度 = content + padding-left + padding-right 
           + border-left + border-right + margin-left + margin-right
```

```css
/* box-sizing: border-box 让宽度包含padding和border（推荐） */
* {
    box-sizing: border-box;  /* 大多数前端框架默认设置 */
}
```

> 💡 **后端理解**：`box-sizing: border-box` 让一个`width: 300px`的盒子总宽度就是300px（padding和border内扣），而不是300+padding+border。看到前端全局设置这个属性是正常的。

---

## 3. Flex布局 — 现代布局标配

> 90%的现代Web布局用Flex解决。后端只需看懂几个核心属性。

```css
/* 容器属性 */
.container {
    display: flex;
    flex-direction: row;        /* 水平排列（默认） */
    /* flex-direction: column;  垂直排列 */
    justify-content: center;    /* 主轴对齐：center/space-between/space-around */
    align-items: center;        /* 交叉轴对齐：center/stretch/flex-start */
    flex-wrap: wrap;            /* 换行 */
    gap: 16px;                  /* 间距 */
}

/* 子元素属性 */
.item {
    flex: 1;                    /* 等分剩余空间 */
    /* flex: 2;  占2份 */
}
```

```text
Flex布局核心记忆：
  justify-content → 水平方向对齐
  align-items     → 垂直方向对齐
  flex: 1         → 自动撑满剩余空间
```

---

## 4. 响应式设计基础

### 4.1 媒体查询

```css
/* 移动端优先 + 桌面端适配 */
.container {
    width: 100%;           /* 手机端全宽 */
}

@media (min-width: 768px) {
    .container {
        width: 750px;      /* 平板 */
        margin: 0 auto;    /* 居中 */
    }
}

@media (min-width: 1200px) {
    .container {
        width: 1170px;     /* 桌面 */
    }
}
```

### 4.2 后端开发对应

| 概念 | 后端关联 |
|------|----------|
| 响应式图片 | 后端提供多尺寸图片URL（缩略图/原图） |
| 移动端适配 | API return字段要考虑移动端少传不必要的字段 |
| 分页 | 移动端通常pageSize更小 |

---

## 5. 后端视角的CSS调试

```text
当页面"看起来不对"时，后端能做的：
1. F12 → Elements面板 → 选中元素 → 右侧Styles看CSS规则
2. 检查是否有样式被划掉（被更高优先级覆盖）
3. 检查Computed标签看最终生效的盒模型数值
4. 常见原因：z-index层级、overflow:hidden裁剪、position定位
```

> 🎯 **后端学CSS的最小集合**：理解盒模型 → 看懂Flex布局 → 知道响应式概念 → 能用DevTools查看元素样式。
