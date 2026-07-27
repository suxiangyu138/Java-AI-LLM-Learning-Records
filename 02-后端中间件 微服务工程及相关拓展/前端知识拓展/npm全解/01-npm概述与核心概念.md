# 01 - npm 概述与核心概念

> 🎯 npm 之于 Node.js = Maven 之于 Java — 包管理器、构建工具、脚本运行器三位一体。用 Java 后端的视角来理解 npm

---

## 目录

1. [npm 是什么](#1-npm-是什么)
2. [npm vs Maven：Java 后端的对标理解](#2-npm-vs-mavenjava-后端的对标理解)
3. [Node.js 与 npm 的关系](#3-nodejs-与-npm-的关系)
4. [包管理架构](#4-包管理架构)

---

## 1. npm 是什么

npm（Node Package Manager）是 Node.js 的默认包管理器，同时也是一个命令行工具和在线仓库。

| 角色 | 说明 | Java 对标 |
|------|------|----------|
| **包管理器** | 管理项目依赖（安装/更新/卸载） | Maven 依赖管理 |
| **构建工具** | npm scripts 执行构建任务 | Maven lifecycle |
| **仓库** | npm registry 存储和分发包 | Maven Central / Nexus |

---

## 2. npm vs Maven：Java 后端的对标理解

| 概念 | npm | Maven | 说明 |
|------|-----|-------|------|
| 包描述文件 | `package.json` | `pom.xml` | 项目元数据 + 依赖声明 |
| 依赖锁文件 | `package-lock.json` | `pom.xml` + `dependencyManagement` | 锁定依赖版本 |
| 安装依赖 | `npm install` | `mvn install`（但含义不同！） | npm 安装依赖到 `node_modules/` |
| 执行构建 | `npm run build` | `mvn compile package` | 项目构建 |
| 运行测试 | `npm test` | `mvn test` | 运行测试 |
| 本地仓库 | `~/.npm/` | `~/.m2/repository/` | 缓存目录 |
| 远程仓库 | `registry.npmjs.org` | `repo1.maven.org` | 中央仓库 |
| 发布 | `npm publish` | `mvn deploy` | 发布到仓库 |
| 作用域包 | `@scope/package` | `groupId:artifactId` | 命名空间隔离 |

```bash
# Java Maven 项目
mvn clean install    # 构建+安装到本地

# Node.js npm 项目
npm install          # ⬅ 仅安装依赖（不构建！）
npm run build        # ⬅ 构建项目
```

> ⚠️ **最大差异**：`npm install` ≠ `mvn install`！前者是下载依赖，后者是构建 + 安装到本地仓库。`npm run build` 才类似 Maven 的构建。

---

## 3. Node.js 与 npm 的关系

| 组件 | 说明 | Java 对标 |
|------|------|----------|
| **Node.js** | JavaScript 运行时（基于 V8 引擎） | JVM（Java 虚拟机） |
| **npm** | Node.js 自带包管理器 | Maven（但默认安装，不像 Maven 需单独装） |
| **npx** | 临时执行 npm 包中的命令 | `mvn exec:java` |
| **nvm** | Node.js 版本管理器 | `sdkman` / `jenv` |

```bash
# 安装 Node.js → npm 自动附带
node -v      # v20.10.0
npm -v       # 10.2.0

# npx：不安装直接运行
npx create-react-app my-app    # 等价于先 npm install -g，再运行

# nvm：切换 Node 版本（类似 sdkman）
nvm install 20
nvm use 20
```

---

## 4. 包管理架构

```text
npm install lodash
    │
    ├── 1. 解析 package.json → 确定 lodash 版本
    ├── 2. 查询本地缓存 ~/.npm/
    ├── 3. 未命中 → 从 registry.npmjs.org 下载
    ├── 4. 解压到 node_modules/lodash/
    └── 5. 更新 package-lock.json

node_modules 目录（扁平化结构）：
  ├── lodash/           # 直接依赖
  ├── react/            # 直接依赖
  ├── ...               # 依赖的依赖被提升到顶层（去重）
  └── .package-lock.json

对比 Java：
  ~/.m2/repository/com/google/guava/guava/32.1.3-jre/guava-32.1.3-jre.jar
  → 按 groupId 分目录，不扁平化
```

```bash
# 项目结构对比
# Java Maven                         # Node.js npm
my-app/                              my-app/
├── pom.xml                          ├── package.json          ← 包描述
├── src/main/java/                   ├── src/
├── src/test/java/                   ├── node_modules/         ← 依赖目录
└── target/                          └── dist/                 ← 构建产物
```

> 🎯 **核心理解**：`package.json` = `pom.xml`、`npm install` = 下载依赖（非构建）、`npm run build` = 构建、`node_modules/` = 依赖存放目录（类似 Maven 的 classpath，但物理存在项目中）。
