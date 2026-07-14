# CSS 详细知识点梳理

> **文档定位**：Java 后端技术参考文档 | CSS 核心知识点全景  
> **核心说明**：CSS（Cascading Style Sheets，层叠样式表）用于描述 HTML 文档的呈现样式，核心作用是分离文档结构与表现形式  
> **三大核心特性**：层叠性、继承性、优先级 —— 决定了样式的最终渲染效果

---

## 目录

- [一、CSS 基础概念](#一css-基础概念)
- [二、CSS 选择器](#二css-选择器)
- [三、CSS 核心特性](#三css-核心特性)
- [四、CSS 样式属性](#四css-样式属性)

---

## 一、CSS 基础概念

### 1.1 什么是 CSS

CSS（Cascading Style Sheets，层叠样式表）是用于描述 HTML（或 XML）文档呈现样式的语言，核心作用是分离文档结构（HTML）和表现形式（样式），让页面布局更灵活、样式更统一，降低维护成本。

### 1.2 CSS 的引入方式

| 方式 | 位置 | 优先级 | 推荐度 |
|------|------|--------|--------|
| **内联样式** | HTML 标签 `style` 属性 | 最高 | ❌ 不推荐大量使用 |
| **内部样式表** | `<head>` 内 `<style>` 标签 | 中 | ⚠️ 仅小型页面 |
| **外部样式表** | 独立 `.css` 文件，通过 `<link>` 引入 | 低 | ✅ 推荐 |

```html
<!-- 内联样式 -->
<p style="color: red; font-size: 16px;">内联样式</p>

<!-- 内部样式表 -->
<head>
  <style>
    p { color: blue; font-size: 14px; }
  </style>
</head>

<!-- 外部样式表（推荐） -->
<link rel="stylesheet" href="style.css">
```

### 1.3 CSS 基本语法

```
选择器 { 属性1: 值1; 属性2: 值2; ... }
```

| 组成部分 | 说明 | 示例 |
|----------|------|------|
| **选择器** | 指定要样式化的 HTML 元素 | `p`、`.class`、`#id` |
| **声明块** | `{}` 包裹，每个声明由属性和值组成 | `{ color: red; }` |
| **注释** | `/* 注释内容 */`，不会被浏览器渲染 | `/* 这是注释 */` |

---

## 二、CSS 选择器

选择器的作用是精准定位 HTML 元素，分为基础选择器和复合选择器。

### 2.1 基础选择器

| 选择器 | 语法 | specificity | 说明 |
|--------|------|-------------|------|
| **元素选择器** | `div {}` | 1 | 作用于所有该标签元素 |
| **类选择器** | `.box {}` | 10 | 作用于 class 为该值的所有元素，可重复 |
| **ID 选择器** | `#header {}` | 100 | 作用于唯一元素，不可重复 |
| **通配符选择器** | `* {}` | 0 | 作用于所有元素，常用于重置默认样式 |

### 2.2 复合选择器

| 选择器 | 语法 | 说明 |
|--------|------|------|
| **后代选择器** | `.box p {}` | 空格分隔，选中所有后代元素 |
| **子选择器** | `.box > p {}` | `>` 分隔，仅选中直接子元素 |
| **相邻兄弟选择器** | `p + div {}` | `+` 分隔，下一个紧邻的兄弟元素 |
| **通用兄弟选择器** | `p ~ div {}` | `~` 分隔，所有后续同级兄弟元素 |
| **交集选择器** | `p.box {}` | 无分隔，同时满足多个条件 |
| **并集选择器** | `p, div, .box {}` | `,` 分隔，统一应用样式 |

### 2.3 伪类选择器

#### 链接伪类

```css
a:link    { color: blue; }    /* 未访问 */
a:visited { color: purple; }  /* 已访问 */
a:hover   { color: red; }     /* 鼠标悬浮（最常用） */
a:active  { color: orange; }  /* 点击瞬间 */
```

> **书写顺序**：link → visited → hover → active（口诀：LVHA），否则样式会失效。

#### 结构伪类

| 伪类 | 说明 |
|------|------|
| `:first-child` | 父元素的第一个子元素 |
| `:last-child` | 父元素的最后一个子元素 |
| `:nth-child(n)` | 父元素的第 n 个子元素（支持 odd/even/公式） |
| `:first-of-type` | 同类型标签的第一个 |
| `:nth-of-type(n)` | 同类型标签的第 n 个 |

#### 状态伪类

| 伪类 | 说明 |
|------|------|
| `:focus` | 元素获得焦点时 |
| `:checked` | 表单元素被选中时 |
| `:disabled` | 元素被禁用时 |
| `:not(selector)` | 不满足条件的元素 |

### 2.4 伪元素选择器

| 伪元素 | 说明 |
|--------|------|
| `::before` | 在元素内容前插入虚拟元素（必须配合 `content`） |
| `::after` | 在元素内容后插入虚拟元素（必须配合 `content`） |
| `::first-letter` | 文本的第一个字符 |
| `::first-line` | 文本的第一行 |
| `::selection` | 用户选中的文本 |

```css
.box::before { content: "★"; color: red; }
```

### 2.5 选择器优先级

| 优先级 | 选择器 | specificity |
|--------|--------|-------------|
| 最高 | `!important` | — |
| ↓ | 内联样式 | 1000 |
| ↓ | ID 选择器 | 100 |
| ↓ | 类/伪类/属性选择器 | 10 |
| ↓ | 元素/伪元素选择器 | 1 |
| 最低 | 通配符 / 继承样式 | 0 |

---

## 三、CSS 核心特性

### 3.1 层叠性

当多个 CSS 规则作用于同一个元素，且样式不冲突时，所有规则会叠加生效；若样式冲突，按优先级决定生效的样式。

### 3.2 继承性

子元素自动继承父元素的某些 CSS 属性。

| 可继承 | 不可继承 |
|--------|----------|
| `color`、`font-size`、`font-family`、`text-align`、`line-height` | `width`、`height`、`margin`、`padding`、`border`、`background`、`position` |

```css
/* 强制继承 */
子元素 { color: inherit; }
```

---

## 四、CSS 样式属性

### 4.1 文本样式

#### 字体属性

| 属性 | 说明 | 常用值 |
|------|------|--------|
| `font-family` | 字体 | `"Microsoft YaHei", Arial, sans-serif` |
| `font-size` | 字体大小 | `16px`、`1rem`、`1em` |
| `font-weight` | 字体粗细 | `normal`(400)、`bold`(700) |
| `font-style` | 字体样式 | `normal`、`italic` |

```css
/* 复合属性（必须包含 font-size 和 font-family） */
font: italic bold 16px "Microsoft YaHei";
```

#### 文本属性

| 属性 | 说明 | 常用值 |
|------|------|--------|
| `color` | 文本颜色 | `#333`、`rgb(255,255,255)`、`rgba(0,0,0,0.5)` |
| `text-align` | 水平对齐 | `left`、`center`、`right`、`justify` |
| `text-decoration` | 文本装饰 | `none`（去掉下划线）、`underline`、`line-through` |
| `text-indent` | 首行缩进 | `2em`（缩进 2 个字符） |
| `line-height` | 行高 | `1.5`、`24px`（常用于垂直居中） |
| `text-shadow` | 文本阴影 | `2px 2px 3px rgba(0,0,0,0.3)` |
| `white-space` | 换行方式 | `normal`、`nowrap`（不换行） |

### 4.2 背景样式

| 属性 | 说明 |
|------|------|
| `background-color` | 背景颜色 |
| `background-image` | 背景图片 `url("图片路径")` |
| `background-repeat` | 重复方式：`no-repeat`、`repeat-x`、`repeat-y` |
| `background-position` | 位置：`center center` |
| `background-size` | 大小：`cover`（覆盖）、`contain`（包含） |
| `background-attachment` | 固定方式：`fixed`（视口固定） |

```css
/* 复合属性 */
background: #f0f0f0 url("bg.jpg") no-repeat center center / cover fixed;
```

### 4.3 盒子模型

CSS 中所有元素都可以看作一个"盒子"，从内到外：**内容区（content）→ 内边距（padding）→ 边框（border）→ 外边距（margin）**。

#### 两种模式

| 模式 | 总宽度计算 | 切换方式 |
|------|-----------|----------|
| **标准盒子**（W3C） | content + padding + border + margin | `box-sizing: content-box` |
| **怪异盒子**（IE） | content（含 padding + border）+ margin | `box-sizing: border-box`（推荐） |

#### 各部分属性

| 部分 | 核心属性 | 简写 |
|------|----------|------|
| **内容区** | `width`、`height` | — |
| **内边距** | `padding-top/right/bottom/left` | `padding: 上 右 下 左` |
| **边框** | `border-width/style/color` | `border: 1px solid #ccc` |
| **外边距** | `margin-top/right/bottom/left` | `margin: 上 右 下 左` |

#### 边框扩展

```css
/* 圆角 */
border-radius: 10px;

/* 阴影 */
box-shadow: 0 2px 5px rgba(0,0,0,0.2);
```

> **⚠️ margin 塌陷**：相邻块级元素的外边距会合并取最大值。解决：给父元素加 border/padding，或 `overflow: hidden`。

### 4.4 元素显示模式

| 模式 | 特点 | 常见元素 | width/height |
|------|------|----------|-------------|
| **块级** `block` | 独占一行，宽度默认 100% | `div`、`p`、`h1-h6` | 可设置 |
| **行内** `inline` | 不独占一行 | `span`、`a`、`strong` | 不可设置 |
| **行内块** `inline-block` | 不独占一行 + 可设宽高 | `img`、`input`、`button` | 可设置 |

| `display` 值 | 效果 |
|---------------|------|
| `block` | 转为块级元素 |
| `inline` | 转为行内元素 |
| `inline-block` | 转为行内块元素 |
| `none` | 隐藏元素（不占据空间） |
| `flex` | 弹性盒容器 |
| `grid` | 网格容器 |

### 4.5 浮动（Float）

浮动的核心作用是让元素脱离文档流，实现左右排列。

| 属性值 | 说明 |
|--------|------|
| `none` | 默认，不浮动 |
| `left` | 向左浮动 |
| `right` | 向右浮动 |

**清除浮动（解决父元素塌陷）**：

| 方法 | 代码 | 推荐度 |
|------|------|--------|
| 额外标签法 | `<div style="clear: both;"></div>` | ❌ |
| `overflow: hidden` | 父元素添加 `overflow: hidden` | ⭐⭐⭐ |
| 伪元素法 | `.father::after { content: ""; display: table; clear: both; }` | ⭐⭐⭐⭐⭐ |

### 4.6 定位（Position）

| 定位模式 | 脱离文档流 | 偏移参考 | 常用场景 |
|----------|-----------|----------|----------|
| `static`（默认） | ❌ | 无 | 正常文档流 |
| `relative`（相对） | ❌ | 自身原位置 | 作为绝对定位的参考容器 |
| `absolute`（绝对） | ✅ | 最近的已定位祖先 | 精确控制位置（父相子绝） |
| `fixed`（固定） | ✅ | 浏览器视口 | 固定导航栏、回到顶部 |
| `sticky`（粘性） | 混合 | 视口（滚动后固定） | 滚动固定的标题 |

```css
/* 父相子绝 */
.parent { position: relative; }
.child { position: absolute; top: 0; right: 0; }
```

> **z-index**：仅对已定位元素有效，数值越大越靠上。

### 4.7 弹性盒布局（Flex）

Flex 是 CSS3 新增的布局方式，是目前主流的布局方式。

#### Flex 容器属性

| 属性 | 说明 | 常用值 |
|------|------|--------|
| `display: flex` | 转为弹性容器 | — |
| `flex-direction` | 主轴方向 | `row`、`column` |
| `flex-wrap` | 是否换行 | `nowrap`、`wrap` |
| `justify-content` | 主轴对齐 | `center`、`space-between`、`space-around` |
| `align-items` | 交叉轴对齐 | `center`、`stretch` |
| `align-content` | 多行交叉轴对齐 | `space-between` |

#### Flex 项目属性

| 属性 | 说明 | 常用值 |
|------|------|--------|
| `flex-grow` | 放大比例 | 默认 0（不放大） |
| `flex-shrink` | 缩小比例 | 默认 1 |
| `flex` | 复合属性 | `flex: 1`（可放大缩小） |
| `align-self` | 单独交叉轴对齐 | 覆盖 `align-items` |
| `order` | 排列顺序 | 数值越小越靠前 |

### 4.8 网格布局（Grid）

Grid 是比 Flex 更强大的二维布局方式，适合复杂页面布局。

```css
.container {
  display: grid;
  grid-template-columns: 1fr 2fr 1fr;  /* 3 列，比例 1:2:1 */
  grid-template-rows: 100px auto;
  gap: 10px;  /* 行列间距 */
}
```

| 容器属性 | 说明 |
|----------|------|
| `grid-template-columns` | 列数和每列宽度（支持 `fr` 比例单位） |
| `grid-template-rows` | 行数和每行高度 |
| `gap` | 行列间距 |

### 4.9 其他常用属性

| 属性 | 说明 | 常用值 |
|------|------|--------|
| `visibility` | 可见性 | `visible`、`hidden`（仍占空间） |
| `opacity` | 透明度 | 0（完全透明）~ 1（完全不透明） |
| `cursor` | 鼠标指针样式 | `pointer`（手型）、`not-allowed`（禁止） |
| `overflow` | 溢出处理 | `hidden`、`scroll`、`auto` |

#### 过渡（transition）

```css
/* 语法：transition: 属性 时间 速度 延迟 */
transition: all 0.3s ease 0s;
```

#### 变形（transform）

| 函数 | 效果 | 示例 |
|------|------|------|
| `translate(x, y)` | 平移 | `translate(50px, 30px)` |
| `scale(x, y)` | 缩放 | `scale(1.2)`（放大 1.2 倍） |
| `rotate(deg)` | 旋转 | `rotate(45deg)`（顺时针 45°） |
