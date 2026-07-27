# 02 - npm 包管理与模块系统

> 🎯 npm 是 Node.js 的 Maven——管理依赖、运行脚本、版本控制。本章从 Java 开发者视角快速掌握 npm + package.json + CommonJS/ESM 模块系统

---

## 目录

1. [npm 核心命令](#1-npm-核心命令)
2. [package.json 详解](#2-packagejson-详解)
3. [CommonJS vs ES Modules](#3-commonjs-vs-es-modules)

---

## 1. npm 核心命令

| 命令 | Java 等价 | 说明 |
|------|---------|------|
| `npm init` | `mvn archetype:generate` | 初始化项目 |
| `npm install` | `mvn install` | 安装所有依赖 |
| `npm install <pkg>` | 添加 `<dependency>` | 安装包 |
| `npm install -D <pkg>` | `<scope>test</scope>` | 开发依赖 |
| `npm uninstall <pkg>` | 删除 `<dependency>` | 卸载 |
| `npm update` | `mvn versions:update` | 更新依赖 |
| `npm run <script>` | `mvn <goal>` | 运行脚本 |
| `npx <cmd>` | 一键运行（无需安装） | 临时执行 |

### 常用实战

```bash
# 初始化项目
npm init -y                           # 跳过问答，直接生成 package.json

# 生产依赖（运行时需要）
npm install express
# → 添加到 dependencies

# 开发依赖（只在开发时需要）
npm install -D typescript jest @types/node
# → 添加到 devDependencies

# 全局安装（命令行工具）
npm install -g nodemon typescript
# → 全局可执行命令

# 安全审计
npm audit                             # 检查已知漏洞
npm audit fix                         # 自动修复
```

## 2. package.json 详解

```json
{
  "name": "my-ai-tool",
  "version": "1.0.0",
  "description": "AI-powered CLI tool",
  "main": "dist/index.js",           // 入口文件（Java 的 main class）
  "type": "module",                   // ESM 模式（推荐）
  "scripts": {                        // ← Java 的 Maven goals
    "start": "node dist/index.js",
    "dev": "nodemon src/index.ts",
    "build": "tsc",
    "test": "jest",
    "lint": "eslint src/"
  },
  "dependencies": {                   // 生产依赖（Maven compile scope）
    "express": "^4.19.0",            // ^ = 兼容 4.x（>=4.19.0, <5.0.0）
    "zod": "~3.23.0"                 // ~ = 兼容补丁（>=3.23.0, <3.24.0）
  },
  "devDependencies": {                // 开发依赖（Maven test scope）
    "jest": "^29.0.0",
    "typescript": "^5.5.0"
  }
}
```

### 版本号语法

| 语法 | 含义 | Java 等价 |
|------|------|---------|
| `^4.19.0` | 兼容 4.x（默认） | `[4.19, 5.0)` |
| `~3.23.0` | 只兼容补丁 | `[3.23, 3.24)` |
| `4.19.0` | 精确锁定 | `[4.19.0]` |
| `>=2.0.0 <3.0.0` | 手动范围 | 同左 |

## 3. CommonJS vs ES Modules

```text
两大模块系统并存，这是 Node.js 最大的"历史包袱"

CommonJS (CJS)：Node.js 原生，使用 require/module.exports
  → 同步加载
  → 传统 Node.js 项目

ES Modules (ESM)：JavaScript 标准，使用 import/export
  → 异步加载
  → 现代项目推荐
  → 浏览器也支持
```

### 对比代码

```javascript
// ====== CommonJS ======
// math.js
function add(a, b) { return a + b; }
module.exports = { add };               // 导出

// main.js
const { add } = require('./math');      // 导入

// ====== ES Modules ======
// math.js
export function add(a, b) { return a + b; }
export const PI = 3.14;                // 命名导出
export default class Calculator {}     // 默认导出

// main.js
import Calculator, { add, PI } from './math.js';  // 导入
//                    ↑ 默认    ↑ 命名
```

### 选择建议

```text
新项目（2024+）：
  → package.json 中加 "type": "module"
  → 全用 ESM (import/export)
  → .js 文件 = ESM, .cjs 文件 = CommonJS

旧项目 / 需要兼容：
  → 不设 type（默认 CommonJS）
  → .mjs 文件 = ESM, .js 文件 = CommonJS

AI 生态现状：
  → MCP SDK：ESM only（必须 import）
  → LangChain.js：ESM
  → 结论：新项目一律用 ESM
```

## 核心要点回顾

- npm = Maven：install/add/run/update —— 每天必用
- `package.json` = `pom.xml`：script 段最重要
- `^` 兼容大版本，`~` 只兼容补丁
- 新项目用 ESM（`import/export`），设置 `"type": "module"`
- `npx` 一键执行，无需全局安装

## 参考资料

1. npm 官方文档 — docs.npmjs.com
2. Node.js Modules — nodejs.org/api/modules.html
