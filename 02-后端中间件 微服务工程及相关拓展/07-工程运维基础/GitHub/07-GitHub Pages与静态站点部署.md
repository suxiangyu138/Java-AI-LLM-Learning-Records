# 07 - GitHub Pages 与静态站点部署

> 免费、HTTPS、自定义域名、自动构建——GitHub Pages 是开发者部署项目文档、个人博客和技术站点的最佳选择。从 Jekyll 到现代静态站点生成器全覆盖。

---

## 目录

1. [GitHub Pages 概述](#1-github-pages-概述)
2. [三种部署方式](#2-三种部署方式)
3. [Jekyll 静态站点](#3-jekyll-静态站点)
4. [自定义域名与 HTTPS](#4-自定义域名与-https)
5. [GitHub Actions 自动化部署](#5-github-actions-自动化部署)
6. [现代静态站点生成器](#6-现代静态站点生成器)
7. [常见面试题](#7-常见面试题)

---

## 1. GitHub Pages 概述

### 1.1 核心特性

```
GitHub Pages：
  ✅ 免费托管（公开仓库无限，私有仓库有流量限制）
  ✅ 自动 HTTPS（Let's Encrypt 证书）
  ✅ 自定义域名支持（含子域名和 apex 域名）
  ✅ 自动构建（推送即部署）
  ✅ 全球 CDN（Fastly）
  ✅ 支持 Jekyll 和任意静态站点生成器

限制：
  ⚠️ 仓库大小 ≤ 1GB
  ⚠️ 站点大小 ≤ 1GB
  ⚠️ 带宽 ≤ 100GB/月（软限制）
  ⚠️ 构建 ≤ 10 次/小时
  ⚠️ 纯静态（无服务端代码，无数据库）
```

### 1.2 站点 URL 规则

| 仓库类型 | 仓库名 | Pages URL |
|---------|--------|-----------|
| 个人/组织站点 | `username.github.io` | `https://username.github.io` |
| 项目站点 | `my-project` | `https://username.github.io/my-project` |

---

## 2. 三种部署方式

### 2.1 方式一：分支部署（传统）

```
Settings → Pages → Source: Deploy from a branch

选项：
  分支：main（或 gh-pages）
  目录：/ (root) 或 /docs

特点：
  - 直接推送 HTML 到指定分支即自动部署
  - 无需额外 CI 配置
  - 适合纯静态 HTML 或 Jekyll 项目
```

### 2.2 方式二：GitHub Actions 部署（⭐ 推荐）

```
Settings → Pages → Source: GitHub Actions

在 .github/workflows/ 中配置构建+部署流程
→ 构建产物通过 actions/deploy-pages Action 部署

特点：
  - 支持任意构建工具（Node.js, Python, Hugo, Next.js...）
  - 完整的 CI/CD 流程控制
  - 部署前可进行测试/检查
```

### 2.3 方式三：/docs 目录部署

```
仓库设置：
  - 在 main 分支的 /docs 目录中放置 HTML 文件
  - Settings → Pages → Source: main / /docs

适用场景：
  - 项目文档与代码在同一仓库
  - 文档生成器输出到 /docs 目录
```

---

## 3. Jekyll 静态站点

### 3.1 Jekyll 基础

> Jekyll 是 GitHub Pages 原生支持的静态站点生成器（Ruby 编写）。用 Markdown 写内容，Jekyll 生成静态 HTML。GitHub Pages 会自动检测并构建 Jekyll 站点。

```yaml
# _config.yml — Jekyll 配置文件
title: My Project Docs
description: A comprehensive documentation site
baseurl: "/my-project"      # 项目站点需设置
url: "https://username.github.io"
theme: minima                # 或 jekyll-theme-cayman 等
```

```
典型 Jekyll 项目结构：
├── _config.yml          # 站点配置
├── _layouts/            # 页面布局模板
│   ├── default.html
│   └── post.html
├── _includes/           # 可复用组件（header/footer）
├── _posts/              # 博客文章（YYYY-MM-DD-title.md）
├── _pages/              # 独立页面
├── assets/              # CSS/JS/图片
├── index.md             # 首页
└── README.md
```

### 3.2 Front Matter

```markdown
---
layout: default
title: API 参考文档
permalink: /api-reference/
nav_order: 3
---

# API 参考文档

正文内容...
```

---

## 4. 自定义域名与 HTTPS

### 4.1 配置自定义域名

```bash
# 1. 在仓库 Settings → Pages → Custom domain → 填入域名
#    www.example.com  或  docs.example.com

# 2. 配置 DNS 记录

# ═══ Apex 域名 (example.com) ═══
# DNS 添加 A 记录 → GitHub Pages IP：
A   @   185.199.108.153
A   @   185.199.109.153
A   @   185.199.110.153
A   @   185.199.111.153

# ═══ 子域名 (www.example.com) ═══
# DNS 添加 CNAME 记录：
CNAME  www   username.github.io.

# 3. 仓库中添加 CNAME 文件（或在 Settings 中填入后自动创建）
# echo "www.example.com" > CNAME

# 4. 等待 DNS 生效 + GitHub 自动签发 HTTPS 证书
# Settings → Pages → ✅ Enforce HTTPS
```

### 4.2 HTTPS 强制

```
1. Settings → Pages → ✅ Enforce HTTPS
   → 所有 HTTP 请求自动 301 重定向到 HTTPS

2. 证书自动续期
   → GitHub 使用 Let's Encrypt，自动管理

⚠️ 注意：
   - 刚配置自定义域名时，HTTPS 可能需要最多 24 小时才可用
   - 如果 DNS 配置错误，证书签发会失败
   - 不要同时使用 Cloudflare 的 Flexible SSL（会导致重定向循环）
```

---

## 5. GitHub Actions 自动化部署

### 5.1 标准部署 Workflow

```yaml
# .github/workflows/pages.yml
name: Deploy to GitHub Pages

on:
  push:
    branches: [main]
    paths:
      - 'docs/**'
      - '.github/workflows/pages.yml'

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Pages
        uses: actions/configure-pages@v4

      - name: Build with Jekyll
        uses: actions/jekyll-build-pages@v1
        with:
          source: ./docs

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

### 5.2 部署现代 SSG（如 Vue/React 文档）

```yaml
# 部署 VitePress 站点
name: Deploy VitePress

on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm' }

      - run: npm ci
      - run: npm run docs:build

      - uses: actions/upload-pages-artifact@v3
        with:
          path: docs/.vitepress/dist

      - uses: actions/deploy-pages@v4
```

---

## 6. 现代静态站点生成器

| 生成器 | 语言 | 适用场景 | 构建速度 |
|--------|------|---------|---------|
| **Jekyll** | Ruby | GitHub Pages 原生 | 慢（大站点） |
| **Hugo** | Go | 博客、文档 | ⚡ 极快 |
| **VitePress** | Vue/JS | Vue 生态文档 | 快 |
| **Docusaurus** | React/JS | 技术文档 | 中 |
| **MkDocs** | Python | 技术文档 | 快 |
| **Next.js (Static)** | React/JS | 复杂站点 | 中 |
| **Astro** | JS | 内容型站点 | 快 |

> 💡 GitHub Pages 原生支持 Jekyll。其他生成器通过 GitHub Actions 构建后部署——在 Actions 中执行构建命令 → 上传产物 → deploy-pages Action 部署。

---

## 7. 常见面试题

### Q1：GitHub Pages 如何配置自定义域名？

> Settings → Pages → Custom domain 填入域名 → DNS 添加 A 记录（Apex 域名）或 CNAME 记录（子域名）→ 等待 SSL 证书自动签发 → 勾选 Enforce HTTPS。详见第4节。

### Q2：GitHub Pages 的部署方式有哪些？

> 三种：分支直接部署（推送到 main/gh-pages）、GitHub Actions 部署（灵活构建+部署）、/docs 目录部署（项目文档）。详见第2节。

### Q3：GitHub Pages 支持后端代码吗？

> 不支持。GitHub Pages 是纯静态托管（HTML/CSS/JS），无服务端运行时。需要后端功能的可用 GitHub Actions + 其他云服务。
