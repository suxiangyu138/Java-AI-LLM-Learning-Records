# 03 - npx 实战场景大全

> 🎯 npx 的四大黄金场景：脚手架创建项目、工具链执行、临时任务、CI 集成。每个场景配 Java 后端对标，拿来即用

---

## 目录

1. [脚手架创建项目（最经典）](#1-脚手架创建项目最经典)
2. [前端工具链：格式化与代码检查](#2-前端工具链格式化与代码检查)
3. [临时工具与一次性任务](#3-临时工具与一次性任务)
4. [CI/CD 与后端项目中的 npx](#4-cicd-与后端项目中的-npx)

---

## 1. 脚手架创建项目（最经典）

npx 最常见的用途：**零安装创建新项目**。等价于 Java 的 `mvn archetype:generate`，但无需先全局装任何东西。

| 脚手架 | 生成项目 | Java 对标 |
|--------|----------|-----------|
| `npx create-vite@latest` | Vue / React / Svelte 应用（轻量） | Spring Initializr |
| `npx create-react-app@latest` | React 全家桶应用 | Spring Initializr |
| `npx create-next-app@latest` | Next.js 应用（SSR/全栈） | Spring Initializr + 前端 |
| `npx create-nestjs` | NestJS 后端服务（TypeScript） | Spring Boot CLI |
| `npx create-expo-app@latest` | React Native 移动端 | — |

```bash
# 最常用：Vite 创建 Vue3 项目（模板可选 vue / react / vanilla）
npx create-vite@latest my-app --template vue

# 交互式创建
npx create-vite@latest
# ✔ Project name: my-app
# ✔ Select a framework: › Vue
# ✔ Select a variant: › TypeScript

# 之后的标准流程
cd my-app
npm install
npm run dev        # 本地开发
npm run build      # 生产构建
```

> ⚠️ **记得 @latest**：npx 有本地缓存，直接 `npx create-vite` 可能用到旧版模板，显式 `@latest` 保证拿到最新版本。

> 💡 **后端视角**：微服务仓库常含前端子模块，团队上手时一条 `npx create-vite@latest` 即可初始化前端骨架，与后端 `Spring Initializr` 流程对称。

---

## 2. 前端工具链：格式化与代码检查

工程化三件套 — 格式化、Lint、类型检查，全部可以用 npx 零安装执行。

### 2.1 代码格式化：Prettier

```bash
# 方案 A：临时执行（不装进项目）
npx prettier --write src/

# 方案 B：装进项目 + npx 执行（推荐，团队统一版本）
npm install -D prettier
npx prettier --check src/          # CI 中检查格式
npx prettier --write .             # 本地一键格式化
```

### 2.2 代码检查：ESLint

```bash
# 一键初始化 ESLint 配置
npx eslint --init

# 执行检查
npx eslint src/
npx eslint --fix src/              # 自动修复
```

### 2.3 本地静态服务器

```bash
# 把任意目录变成静态站点（联调前端构建产物非常好用）
npx http-server dist/ --port 8080
# 或
npx serve -l 8080 dist/
```

> 💡 **后端视角**：前端 `dist/` 构建产物本地预览，等价于后端 `mvn package` 后直接 `java -jar` 起服务 — 都是"产物 → 服务"的一步操作。

---

## 3. 临时工具与一次性任务

| 场景 | 命令 | 说明 |
|------|------|------|
| 临时跑 TypeScript | `npx -p typescript tsc --version` | 不装 typescript 也能编译检查 |
| 测试某个包 | `npx -p foo@1.2.3 foo` | 快速验证某版本行为 |
| 查询 npm 包信息 | `npx view`（即 `npm view`） | 包版本、描述、依赖 |
| 交互式升级依赖 | `npx npm-check-updates` | 查看可升级依赖（类似 `mvn versions:display-dependency-updates`） |
| 清理 node_modules 垃圾 | `npx rimraf node_modules` | 跨平台删除（Windows 友好） |
| 压缩文件 | `npx tar` | 免装系统 tar |

```bash
# 临时使用某版本验证问题
npx -p typescript@5.6.2 tsc --version
# Version 5.6.2

# 查询依赖升级情况（对标 mvn versions:display-dependency-updates）
npx npm-check-updates
# Checking ... 12 upgrades available
```

---

## 4. CI/CD 与后端项目中的 npx

### 4.1 前端流水线标准模板

```yaml
# .gitlab-ci.yml / GitHub Actions 片段
build-frontend:
  stage: build
  script:
    - npm ci                  # 按 lockfile 精确安装（比 install 快且可复现）
    - npx prettier --check src/   # 格式检查
    - npx eslint .                # lint 检查
    - npm run build               # 构建
```

> ⚠️ **CI 铁律**：CI 中 `npm ci` 后优先用 `npm run` 或直接本地二进制，**避免 npx 临时拉取未固定版本的远程包** — 每次构建结果可能不同，且新增一次供应链信任风险。

### 4.2 后端项目的场景

```bash
# 1. 全栈仓库：构建前端子模块
cd frontend && npm ci && npx eslint . && npm run build

# 2. 检查 package-lock.json 与依赖树
npx npm ls                    # 列出依赖树
npm audit                     # 依赖漏洞审计（npm 内置，无需 npx）

# 3. 一键初始化 Node 子项目
npx create-vite@latest ui --template vue
```

### 4.3 团队约定与脚本化

```json
{
  "scripts": {
    "lint": "eslint .",
    "format": "prettier --write .",
    "check": "npm run lint && npm run format"
  }
}
```

> 💡 **后端视角最佳实践**：把 npx 命令收敛进 `package.json` 的 scripts，团队统一入口（类似 Maven 的 `mvn verify`），避免每个人各自的 npx 版本漂移。

---

> 🎯 **核心要点**：npx 四大场景 — 脚手架（`create-vite@latest`）、工具链（prettier/eslint/http-server）、临时任务（版本验证/依赖升级）、CI 集成（锁版本 + npm ci）。后端人员记住"脚手架初始化 + 工具链执行 + CI 防漂移"三件事就够。

**下一模块**：[04-npx运行原理与缓存机制](04-npx运行原理与缓存机制.md) / **返回总览**：[00-npx知识体系总览](00-npx知识体系总览.md)
