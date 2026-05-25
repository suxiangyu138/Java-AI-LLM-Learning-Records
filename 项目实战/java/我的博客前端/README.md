# 我的博客前端 (My Blog Frontend)

> 现代化博客前端项目，计划使用 React/Vue 框架构建

## 项目概述

"我的博客"的前端升级版本，计划使用现代前端框架（React 或 Vue3）重构静态博客，打造 SPA（单页面应用）博客系统。将支持组件化开发、路由管理、API 数据交互、响应式设计等现代化前端特性。

## 计划技术栈

| 层级 | 技术方案 | 说明 |
|------|----------|------|
| 框架 | React 18+ 或 Vue 3 | 主选方案，根据学习进度选择 |
| 构建工具 | Vite | 快速开发构建 |
| 语言 | TypeScript | 类型安全 |
| 样式 | Tailwind CSS / CSS Modules | 原子化 CSS |
| 路由 | React Router / Vue Router | 前端路由 |
| 状态管理 | Zustand / Pinia | 轻量状态管理 |
| HTTP 客户端 | Axios / Fetch API | 后端 API 通信 |
| UI 组件库 | Ant Design / Element Plus（可选）| 加速开发 |

## 计划功能

- **文章列表页**：分页展示、分类筛选、搜索
- **文章详情页**：Markdown 渲染、代码高亮
- **标签/分类云**：按标签浏览文章
- **关于我页面**：个人介绍、技能展示
- **响应式布局**：PC + 移动端适配
- **暗黑模式**：主题切换
- **SEO 优化**：meta 标签、语义化 HTML
- **评论系统**：集成第三方评论（Giscus 等）

## 推荐项目结构

```
我的博客前端/
├── public/                 # 静态资源
│   └── favicon.ico
├── src/
│   ├── assets/             # 图片、字体等
│   ├── components/         # 可复用组件
│   │   ├── Header/
│   │   ├── Footer/
│   │   ├── ArticleCard/
│   │   └── TagCloud/
│   ├── pages/              # 页面组件
│   │   ├── Home/
│   │   ├── Article/
│   │   ├── About/
│   │   └── Archive/
│   ├── hooks/              # 自定义 Hooks
│   ├── services/           # API 请求封装
│   ├── store/              # 状态管理
│   ├── styles/             # 全局样式
│   ├── utils/              # 工具函数
│   ├── App.tsx
│   └── main.tsx
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── README.md
```

## 开发路线图

### 第一阶段：基础搭建
- [ ] 项目脚手架初始化（Vite + TS）
- [ ] 路由配置
- [ ] 基础布局组件（Header/Footer/Sidebar）
- [ ] Mock 数据展示

### 第二阶段：核心功能
- [ ] 文章列表与详情页
- [ ] Markdown 渲染
- [ ] 标签/分类功能
- [ ] API 对接（对接后端博客服务）

### 第三阶段：体验优化
- [ ] 响应式适配
- [ ] 暗黑模式
- [ ] 加载动画与骨架屏
- [ ] SEO 优化

### 第四阶段：部署上线
- [ ] 打包优化（代码分割、懒加载）
- [ ] 部署到 Vercel / GitHub Pages

## 快速开始

```bash
# 使用 Vite 创建项目（React 示例）
npm create vite@latest my-blog-frontend -- --template react-ts

# 安装依赖
cd my-blog-frontend
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| 组件化开发 | JSX/TSX 组件拆分与复用 |
| 前端路由 | SPA 页面导航（React Router / Vue Router） |
| 状态管理 | 全局状态与组件通信 |
| API 封装 | Axios 拦截器、请求/响应处理 |
| TypeScript | 类型定义、接口设计 |
| Vite | 模块热更新、快速构建 |
| CSS 工程化 | Tailwind CSS 原子化样式 |
| 性能优化 | 懒加载、代码分割、图片优化 |

## 注意事项

- 建议从 React 或 Vue3 中二选一深入学习，而非同时学两个
- 初期可用 Mock 数据开发，后期再对接后端 API
- 使用 Git 分支管理不同阶段的功能开发
