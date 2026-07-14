# package.json 完全详解

> `package.json` 是一个 Node.js 项目的"身份证 + 说明书 + 控制面板"。每一个字段都有约定和最佳实践。

---

## 一、基础字段

### 1.1 必须字段（npm 发布要求）

```json
{
  "name": "my-project",
  "version": "1.0.0"
}
```

| 字段 | 说明 | 约定 |
|------|------|------|
| `name` | 项目名称 | 小写 + 短横线连接，不能有空格/大写；发布到 npm 必须唯一；长 214 字符以内 |
| `version` | 版本号 | SemVer 格式 `主.次.补丁`，如 `1.2.3` |

### 1.2 推荐字段

```json
{
  "description": "项目的一句话描述",
  "main": "index.js",
  "keywords": ["npm", "demo", "tutorial"],
  "author": "suxiangyu <email@example.com> (https://github.com/suxiangyu138)",
  "license": "MIT",
  "repository": {
    "type": "git",
    "url": "https://github.com/suxiangyu138/my-project.git"
  },
  "homepage": "https://github.com/suxiangyu138/my-project#readme",
  "bugs": {
    "url": "https://github.com/suxiangyu138/my-project/issues"
  }
}
```

| 字段 | 作用 | 注意事项 |
|------|------|----------|
| `description` | 项目描述 | npm 搜索时显示在包名下方 |
| `main` | 包入口文件 | 别人 `require('你的包')` 时找到的入口，默认 `index.js` |
| `keywords` | 搜索关键词 | 提升 npm 搜索排名，上限 6-8 个关键词 |
| `author` | 作者信息 | 格式：`姓名 <邮箱> (网址)` |
| `license` | 许可证 | 开源项目常用 MIT / Apache-2.0 / GPL |
| `repository` | 代码仓库地址 | 格式 `{ "type": "git", "url": "..." }` |
| `homepage` | 项目主页 | 一般是 README 地址 |
| `bugs` | 问题反馈地址 | 一般是 GitHub Issues 链接 |

### 1.3 入口字段对比

```json
{
  "main": "index.js",
  "module": "dist/index.esm.js",
  "types": "dist/index.d.ts",
  "exports": {
    ".": {
      "import": "./dist/index.esm.js",
      "require": "./dist/index.cjs.js",
      "types": "./dist/index.d.ts"
    },
    "./utils": "./dist/utils.js"
  }
}
```

| 字段 | 面向 | 说明 |
|------|------|------|
| `main` | CommonJS | `require()` 入口，Node.js 传统入口 |
| `module` | ES Module | `import` 入口，打包工具 (Webpack/Rollup) 优先使用 |
| `types` | TypeScript | 类型声明入口，IDE 智能提示来源 |
| `exports` | 现代统一方案 | Node 12.7+ / 支持条件导出，**优先级最高** |

`exports` 是现代推荐写法，支持条件导出（Conditional Exports），可以按 `import` / `require` / `types` / `browser` / `node` 等条件指定不同入口。

---

## 二、依赖声明字段

### 2.1 五种依赖类型

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "vite": "^5.0.0",
    "eslint": "^8.0.0",
    "typescript": "^5.3.0"
  },
  "peerDependencies": {
    "react": ">=18.0.0"
  },
  "optionalDependencies": {
    "fsevents": "^2.3.0"
  },
  "bundledDependencies": ["my-local-package"]
}
```

| 类型 | 何时安装 | 典型包含 | 被谁安装 |
|------|----------|----------|----------|
| **dependencies** | `npm install` 始终 | Vue / React / Axios / Express | 所有使用者 |
| **devDependencies** | `npm install`（非 `--production`）| Webpack / Vite / ESLint / Jest | 项目开发者 |
| **peerDependencies** | npm 7+ 自动安装 | 插件对宿主框架的依赖 | 宿主项目 |
| **optionalDependencies** | `npm install` 始终（失败不中断）| 平台相关包如 fsevents | 所有使用者 |
| **bundledDependencies** | `npm pack` 时打包进去 | 本地模块 | 发布为压缩包时 |

### 2.2 dependencies vs devDependencies

```
生产依赖 (dependencies)             开发依赖 (devDependencies)
─────────────────────────         ─────────────────────────
  项目运行时必需的代码              仅开发/构建/测试时需要

  ✅ vue                            ✅ webpack / vite
  ✅ react                           ✅ eslint / prettier
  ✅ axios                           ✅ jest / vitest
  ✅ express                         ✅ typescript
  ✅ lodash                          ✅ sass / less / postcss
  ✅ echarts                         ✅ @types/xxx
  ✅ element-plus                    ✅ husky / lint-staged
  ✅ vue-router                      ✅ cypress / playwright
```

**判断法**：删掉这个包之后，用户还能正常使用你的产品吗？
- 能 → devDependencies
- 不能 → dependencies

### 2.3 peerDependencies 详解

```json
// 场景：你开发了一个 Vue 3 的 UI 组件库
{
  "name": "my-vue-ui",
  "peerDependencies": {
    "vue": "^3.3.0"
  },
  "devDependencies": {
    "vue": "^3.3.0"
  }
}
```

| 行为 | 说明 |
|------|------|
| npm 6 及以前 | peerDependencies **不自动安装**，仅警告 |
| npm 7+ | **自动安装** peerDependencies |
| 版本不匹配 | 控制台输出 warning |

**为什么需要 peerDependencies？**
- 避免宿主项目和插件各自安装一份 Vue/React，造成版本冲突
- 插件声明"我兼容 Vue 3.3+，请宿主项目自行安装"

---

## 三、scripts 脚本字段

### 3.1 脚本定义

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext .js,.ts,.vue",
    "test": "vitest run",
    "test:watch": "vitest",
    "format": "prettier --write .",
    "prepare": "husky install",
    "prebuild": "npm run lint",
    "postbuild": "echo 'build done!'"
  }
}
```

| 特殊前缀 | 行为 |
|----------|------|
| `pre<script>` | 在 `<script>` 执行**之前**自动运行 |
| `post<script>` | 在 `<script>` 执行**之后**自动运行 |
| `prepare` | `npm install` 之后自动运行（常用于初始化 husky）|

### 3.2 内置快捷命令

```bash
npm start      # → npm run start
npm test       # → npm run test
npm stop       # → npm run stop
npm restart    # → npm run stop && npm run start
```

**只用这四个**不需要 `run`，其他都必须 `npm run xxx`。

### 3.3 脚本执行环境

执行 `npm run xxx` 时，npm 会自动将 `node_modules/.bin` 临时加入 PATH：

```bash
# 可以直接执行本地安装的 CLI 工具，不需要写完整路径
"scripts": {
  "dev": "vite"         # ✅ 直接 vite 命令
}

# 等价于
"scripts": {
  "dev": "./node_modules/.bin/vite"  # 不推荐，冗余
}
```

### 3.4 传参与环境变量

```bash
# 传参：用 -- 分隔
npm run build -- --mode production

# 环境变量：scripts 中可直接用 process.env
"scripts": {
  "build:prod": "cross-env NODE_ENV=production vite build"
}
```

---

## 四、engines 与配置字段

### 4.1 engines（运行环境要求）

```json
{
  "engines": {
    "node": ">=18.0.0",
    "npm": ">=9.0.0"
  }
}
```

- `npm install` 时只是**警告**，不会阻止安装
- 配合 `engine-strict=true`（`.npmrc` 中设置）才能阻止安装

```bash
# .npmrc
engine-strict=true
```

### 4.2 files（发布白名单）

```json
{
  "files": [
    "dist",
    "README.md",
    "LICENSE"
  ]
}
```

- 指定 `npm publish` 时哪些文件上传到 Registry
- 等同于 `.gitignore` 的反向逻辑，不在此列表的不发布
- 默认总会上传：`package.json`、`README`、`LICENSE`、`CHANGELOG`、主入口文件
- 默认不传：`.git`、`node_modules`、`.npmrc`、`.gitignore` 等

### 4.3 其他常用配置

| 字段 | 作用 | 示例 |
|------|------|------|
| `private` | 设为 `true` 禁止 `npm publish` | 在项目中防止误发布到公共仓库 |
| `type` | 设为 `"module"` 启用 ES Module | `.js` 文件默认用 `import/export` |
| `browserslist` | 声明兼容浏览器范围 | Babel / Autoprefixer 读取 |
| `sideEffects` | 标记文件是否有副作用 | Webpack Tree Shaking 使用 |
| `workspaces` | monorepo 子包目录 | `["packages/*"]` |
| `bin` | CLI 工具注册命令 | `{ "my-cli": "./bin/cli.js" }` |

### 4.4 browserslist 配置

```json
{
  "browserslist": [
    "> 1%",
    "last 2 versions",
    "not dead",
    "not ie 11"
  ]
}
```

Babel（`@babel/preset-env`）、Autoprefixer、postcss-preset-env 等工具都会读取此字段来决定编译/兼容目标。

---

## 五、完整配置模板

```json
{
  "name": "my-vue-project",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "description": "A Vue 3 project with TypeScript",
  "main": "./dist/index.cjs.js",
  "module": "./dist/index.esm.js",
  "types": "./dist/index.d.ts",
  "exports": {
    ".": {
      "import": "./dist/index.esm.js",
      "require": "./dist/index.cjs.js",
      "types": "./dist/index.d.ts"
    }
  },
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext .vue,.ts",
    "test": "vitest run",
    "prepare": "husky install"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "vue-tsc": "^2.0.0",
    "vitest": "^1.0.0",
    "eslint": "^8.0.0",
    "prettier": "^3.0.0"
  },
  "engines": {
    "node": ">=18.0.0"
  },
  "browserslist": [
    "> 1%",
    "last 2 versions",
    "not dead"
  ],
  "repository": {
    "type": "git",
    "url": "https://github.com/user/my-project"
  },
  "keywords": ["vue", "typescript", "vite"],
  "author": "Your Name <email@example.com>",
  "license": "MIT"
}
```

---

> **重点记忆**：`dependencies` vs `devDependencies` 的区别、`exports` 的优先级最高、`scripts` 中 `pre/post` 钩子的自动执行。这三个是面试和工程实践中最高频的知识点。
