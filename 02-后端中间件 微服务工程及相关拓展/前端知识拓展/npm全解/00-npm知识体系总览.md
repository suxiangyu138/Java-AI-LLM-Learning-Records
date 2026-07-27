# 00 - npm 知识体系总览

> 🎯 npm 是 Node.js 生态的包管理器 — Java 后端在日常开发中不可避免地要接触前端项目的 npm 构建、依赖管理和脚本配置。理解 npm 是理解前端工程化的第一步

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [后端必知的学习路线](#3-后端必知的学习路线)

---

## 1. 知识全景

```
npm 精通体系（6个文件 — 聚焦 Java 后端需要知道的）
│
├── 🏗️ 基础（01-02）
│   ├── 01-npm概述与核心概念.md       # npm是什么/Node.js/package.json/与Maven对比
│   └── 02-依赖管理与package.json.md   # dependencies/dev/scripts/版本号语义
│
├── 🔧 实战（03-04）
│   ├── 03-npm常用命令速查.md          # install/run/update/uninstall/global
│   └── 04-npm脚本与工程化.md          # npm scripts/构建/前端工程化/CI集成
│
├── 📋 进阶（05）
│   └── 05-npm私服与安全.md            # Verdaccio/私有仓库/安全审计/lock文件
│
└── 📌 00-npm知识体系总览.md            # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | npm知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | npm概述与核心概念 | Node.js/npm/package.json/与 Maven 对比 | ⭐ |
| 02 | 依赖管理与 package.json | dependencies/devDependencies/scripts/版本号 | ⭐⭐ |
| 03 | npm常用命令速查 | install/run/update/global 及对标 Maven | ⭐ |
| 04 | npm脚本与前端工程化 | Scripts 自动化/构建工具链/CI 集成 | ⭐⭐ |
| 05 | npm私服与安全审计 | Verdaccio/私有仓库/安全审计/package-lock | ⭐⭐ |

---

## 3. 后端必知的学习路线

### 🟢 足够用（1-2 小时）

```
01-概述（含 Maven 对标）→ 02-package.json 解读 → 03-命令速查
产出：能看懂前端项目、执行 npm install && npm run build、理解依赖声明
```

### 🔵 深度理解（半天）

```
04-npm scripts 与工程化 → 05-私服与安全
产出：能配置前端工程化脚本、搭建私有 npm 仓库、做安全审计
```

---

> 🎯 **后端为什么需要学 npm？** 微服务项目常包含前端模块（Vue/React），CI/CD 构建需要 `npm install && npm run build`，理解 npm 让后端具备全栈协作能力。
