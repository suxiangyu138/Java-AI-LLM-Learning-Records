# 02 - 依赖管理与 package.json

> 🎯 package.json 是 npm 项目的"身份证" — 声明依赖、定义脚本、配置元数据。后端必须能读懂这份文件

---

## 目录

1. [package.json 字段详解](#1-packagejson-字段详解)
2. [dependencies vs devDependencies](#2-dependencies-vs-devdependencies)
3. [版本号语义（SemVer）](#3-版本号语义semver)
4. [package-lock.json](#4-package-lockjson)

---

## 1. package.json 字段详解

```json
{
  "name": "my-frontend",
  "version": "1.0.0",
  "private": true,                    // 私有项目（防止意外 publish）
  "description": "Vue3 前端项目",
  "main": "src/main.js",              // 入口文件
  "scripts": {                        // ⭐ 自定义脚本（常用）
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "vitest run",
    "lint": "eslint src --fix"
  },
  "dependencies": {                   // ⭐ 生产依赖
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {                // ⭐ 开发依赖
    "vite": "^5.0.0",
    "@vitejs/plugin-vue": "^5.0.0",
    "eslint": "^8.56.0",
    "vitest": "^1.2.0"
  },
  "engines": {
    "node": ">=18.0.0"
  },
  "browserslist": [
    "> 1%",
    "last 2 versions"
  ]
}
```

| 字段 | 说明 | Java 对标 |
|------|------|----------|
| `name` | 包名（小写、无空格） | `artifactId` |
| `version` | 版本号 | `<version>` |
| `scripts` | 自定义命令 | Maven Plugins |
| `dependencies` | 生产环境依赖 | `<dependencies>` + compile scope |
| `devDependencies` | 开发环境依赖 | test scope / 构建插件 |
| `private` | 禁止发布 | — |
| `engines` | 运行环境要求 | `<java.version>` |

---

## 2. dependencies vs devDependencies

| 类型 | 何时安装 | 包含内容 | Java 对标 |
|------|----------|----------|----------|
| **dependencies** | `npm install` / `npm install --production` | Vue/React/Axios 等运行时依赖 | compile scope |
| **devDependencies** | 仅 `npm install`（开发） | Vite/ESLint/Jest 等构建工具 | test scope / 插件 |

```bash
# 安装到 dependencies
npm install axios

# 安装到 devDependencies
npm install eslint --save-dev
npm install vite -D         # -D = --save-dev

# 生产环境安装（跳过 devDependencies）
npm install --production
```

```json
// package.json 中的依赖标识
{
  "dependencies": {
    "vue": "^3.4.0"          // 生产依赖
  },
  "devDependencies": {
    "vite": "^5.0.0"         // 仅开发依赖
  }
}
```

> 💡 **后端对照**：Maven 的 `scope=compile` 类似 `dependencies`，`scope=test` 类似 `devDependencies`。但 npm 没有 `provided`/`runtime` 精确区分。

---

## 3. 版本号语义（SemVer）

```text
^3.4.0  ← 版本范围：>=3.4.0 <4.0.0（主版本不变，接受次版本和补丁更新）
~3.4.0  ← 版本范围：>=3.4.0 <3.5.0（次版本不变，只接受补丁更新）
3.4.0   ← 精确版本
*       ← 任意版本
>=3.0.0 ← 最低版本
```

| 符号 | 含义 | 风险 | 推荐场景 |
|:---:|------|:---:|------|
| `^` | 兼容主版本 | 中 | 大多数依赖 |
| `~` | 兼容次版本 | 低 | 对稳定性要求高的依赖 |
| 精确 | 锁定版本 | 无 | 关键依赖 |
| `latest` | 最新版 | ⚠️ 高 | 不推荐用于生产 |

```bash
# 安装时用什么版本标识
npm install vue          # 默认用 ^（最新版，写入 ^3.4.0）
npm install vue@3.3.0    # 精确版本
npm install vue@~3.3.0   # ~ 范围
```

> ⚠️ **Java 对标**：npm 的 `^3.4.0` 类似 Maven 的 `[3.4,4.0)` 版本范围，但没有 Maven 的 SNAPSHOT 概念。

---

## 4. package-lock.json

> package-lock.json = 锁定依赖的精确版本树。类似 Maven 的 BOM + dependencyManagement 的版本锁定效果。

```json
// package-lock.json 关键信息
{
  "name": "my-frontend",
  "lockfileVersion": 3,
  "packages": {
    "node_modules/vue": {
      "version": "3.4.15",            // ← 精确锁定版本
      "resolved": "https://registry.npmjs.org/vue/-/vue-3.4.15.tgz",
      "integrity": "sha512-xxx..."    // ← 哈希校验
    }
  }
}
```

| 特性 | package-lock.json | Maven |
|------|:---:|------|
| 锁定版本 | ✅ 精确到 3.4.15 | ✅ BOM / dependencyManagement |
| 锁定依赖树 | ✅ 递归锁 | ❌ 只锁直接依赖 |
| 哈希校验 | ✅ integrity 字段 | ⚠️ PGP 签名（不常用） |
| 提交到 Git | ✅ 必须 | ✅ pom.xml |

> 🎯 **后端必知**：`package-lock.json` 必须提交到 Git！它保证所有环境和 CI 安装的依赖完全一致，相当于确定性构建的保障。
