# 我的博客 (My Blog)

> 个人静态博客网站，HTML5 + CSS3 + JavaScript 原生实现

## 项目概述

纯前端静态博客网站，使用原生 HTML/CSS/JavaScript 构建。包含博客首页、文章展示、图片资源等完整页面。采用响应式布局设计，展示个人技术博客内容。使用 Maven 作为构建工具管理项目。

## 技术栈

| 技术 | 说明 |
|------|------|
| HTML5 | 语义化页面结构 |
| CSS3 | 样式布局（style.css, 11904 bytes） |
| JavaScript | 页面交互逻辑（main.js, 4983 bytes） |
| Maven | 项目构建与资源管理 |

## 功能特性

- 博客首页布局（导航栏 + 文章列表 + 侧边栏）
- CSS3 动画与过渡效果
- JavaScript DOM 交互
- 图片资源管理
- Maven 构建支持

## 项目结构

```
我的博客/
├── index.html          # 主页（9154 bytes）
├── css/
│   └── style.css       # 样式表（11904 bytes）
├── js/
│   └── main.js         # 交互脚本（4983 bytes）
├── images/             # 图片资源（5 张博客截图）
├── target/             # Maven 构建输出
├── pom.xml             # Maven 配置文件
└── README.md
```

## 快速开始

```bash
# 方式一：直接用浏览器打开
start index.html

# 方式二：使用 Maven 构建
mvn clean package
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| HTML5 语义化 | header, nav, main, article, footer 标签 |
| CSS 盒模型 | margin, padding, border 布局控制 |
| CSS Flexbox / Grid | 响应式页面布局 |
| CSS 选择器 | 类选择器、ID 选择器、伪类选择器 |
| DOM 操作 | querySelector, createElement, appendChild |
| 事件处理 | click, scroll, DOMContentLoaded |
| Maven | 静态资源项目构建管理 |

## 注意事项

- 静态页面可直接浏览器打开，无需 Web 服务器
- 图片路径使用相对路径，部署时注意资源路径正确性
- 推荐使用 Live Server 等工具进行本地开发预览
