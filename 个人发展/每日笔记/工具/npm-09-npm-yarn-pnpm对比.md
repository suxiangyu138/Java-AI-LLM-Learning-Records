# npm vs Yarn vs pnpm vs Bun 全景对比

> 面向选择和面试，从安装机制、性能、磁盘效率、工程化四维深度对比。

---

## 一、快速总览

| 维度 | npm | Yarn (v1) | Yarn (v4/Berry) | pnpm | Bun |
|------|-----|-----------|-----------------|------|-----|
| 首次发布 | 2010 | 2016 | 2020 | 2017 | 2023 |
| 开发者 | npm Inc. | Facebook | — | Zoltan Kochan | Oven/Jarred Sumner |
| 安装速度 | 基准 | 快 2-3× | 快 | 快 3-5× | **快 10-25×** |
| 磁盘效率 | 低 | 低 | 中 | **极高** | 低 |
| 幽灵依赖 | 存在 | 存在 | PnP 模式无 | **不存在** | 存在 |
| 默认 lock | package-lock.json | yarn.lock | yarn.lock | pnpm-lock.yaml | bun.lockb(二进制) |
| monorepo | workspaces(基础) | workspaces | workspaces(增强) | **workspace(最成熟)** | workspaces |
| 插件机制 | 无 | 有 | **丰富（P'n'P/约束等）** | 有(Hooks) | 无 |

---

## 二、安装机制对比

### 2.1 npm（扁平化 + 幽灵依赖）

```
node_modules/
├── vue/              ← 你的依赖（在 package.json 中声明的）
├── axios/            ← 你的依赖
├── lodash/           ← 你的依赖
├── @babel/xxx/       ← 间接依赖，被提升到顶层的
├── loose-envify/     ← 间接依赖（你没声明，但能用！→ 幽灵依赖）
└── react/            ← 你的依赖
```

### 2.2 Yarn v1（类似 npm，也扁平化）

```
yarn 的改进：
- yarn.lock 格式更清晰（vs package-lock.json 的 JSON）
- Offline Cache 离线安装
- yarn install 并行下载更快
- yarn workspaces 比早期 npm 更好用
```

但与 npm 一样存在幽灵依赖问题。

### 2.3 pnpm（硬链接 + 非扁平的 node_modules）

```
实际存储：~/.pnpm-store/v3/files/（全局，所有项目共享）
                     ↓ 硬链接
node_modules/.pnpm/（真实的扁平存储）
                     ↓ 软链接
node_modules/vue  →  .pnpm/vue@3.4.0/node_modules/vue/
node_modules/axios → .pnpm/axios@1.6.0/node_modules/axios/

你只能访问 node_modules 下你自己声明过的包！
未声明的包不在 node_modules 顶层 → 幽灵依赖直接报错
```

**pnpm 为什么省磁盘？**

```
三个项目都依赖 vue@3.4.0（~20 MB）：

npm：3 份 × 20MB = 60 MB
yarn：3 份 × 20MB = 60 MB
pnpm：1 份（全局 store）+ 3 组软链接 = 20 MB + 几 KB
```

### 2.4 Yarn Berry（PnP — 更激进的无 node_modules 方案）

```
项目根目录/
├── .pnp.cjs          ← PnP 解析器文件（替代 node_modules 的依赖查找）
├── .pnp.loader.mjs
├── .yarn/
│   └── cache/        ← 压缩包的缓存（直接用 zip 文件，不展开）
│       ├── vue-3.4.0.zip
│       ├── axios-1.6.0.zip
│       └── ...
└── package.json

运行时：Node.js 通过 PnP 解析器直接从 zip 中读取文件
        不再需要 node_modules 目录
```

**PnP 的优劣**：
- ✅ 安装极快（不用解压，zip 直接可用）
- ✅ 零幽灵依赖
- ✅ 磁盘占用小
- ❌ 兼容性问题（某些工具假设 node_modules 存在）
- ❌ 调试困难（文件在 zip 里）

### 2.5 Bun（速度的极致）

```
Bun 的特点：
1. 运行时 + 包管理器 + 打包器 + 测试框架 四合一体
2. 用 Zig 编写，而非 Node.js 的 C++ + JS
3. 安装速度是 npm 的 10-25 倍（二进制 lock 文件 + 全局缓存 + 并行网络）
4. 与 Node.js 大部分兼容
5. bun.lockb 是二进制格式，人类不可读
```

---

## 三、核心命令对照表

| 操作 | npm | yarn (v1) | pnpm | bun |
|------|-----|-----------|------|-----|
| 初始化 | `npm init` | `yarn init` | `pnpm init` | `bun init` |
| 安装所有 | `npm install` | `yarn` | `pnpm install` | `bun install` |
| 安装包 | `npm i pkg` | `yarn add pkg` | `pnpm add pkg` | `bun add pkg` |
| 安装开发依赖 | `npm i pkg -D` | `yarn add pkg -D` | `pnpm add pkg -D` | `bun add pkg -D` |
| 全局安装 | `npm i -g pkg` | `yarn global add pkg`| `pnpm add -g pkg` | `bun add -g pkg` |
| 卸载 | `npm un pkg` | `yarn remove pkg` | `pnpm remove pkg` | `bun remove pkg` |
| 更新 | `npm update` | `yarn upgrade` | `pnpm update` | `bun update` |
| 运行脚本 | `npm run dev` | `yarn dev`(省略 run) | `pnpm dev`(省略 run) | `bun run dev` |
| 执行脚本 | `npx cmd` | `yarn cmd` | `pnpm exec cmd`/`pnpx cmd` | `bunx cmd` |
| CI 安装 | `npm ci` | `yarn --frozen-lockfile` | `pnpm --frozen-lockfile` | — |
| 列出依赖 | `npm ls` | `yarn list` | `pnpm list` | — |
| 审计 | `npm audit` | `yarn audit` | `pnpm audit` | — |

---

## 四、Lock 文件对比

### npm (package-lock.json)

```json
{
  "lockfileVersion": 3,
  "packages": {
    "node_modules/vue": {
      "version": "3.4.0",
      "resolved": "...",
      "integrity": "sha512-..."
    }
  }
}
```

### yarn (yarn.lock)

```
vue@^3.4.0:
  version "3.4.0"
  resolved "..."
  integrity sha512-...
  dependencies:
    "@vue/shared" "3.4.0"
```

### pnpm (pnpm-lock.yaml)

```yaml
lockfileVersion: '6.0'

dependencies:
  vue:
    specifier: ^3.4.0
    version: 3.4.0

packages:
  /vue@3.4.0:
    resolution: {integrity: sha512-...}
```

### bun (bun.lockb)

二进制格式，人类不可读（可用 `bun bun.lockb` 打印）。

---

## 五、Monorepo 支持对比

| 功能 | npm 7+ | Yarn v1 | Yarn Berry | pnpm |
|------|--------|---------|------------|------|
| workspaces 声明 | ✅ | ✅ | ✅ | ✅ (最灵活) |
| 子包依赖引用 | ✅ `-w` 参数 | ✅ | ✅ | ✅ `--filter` / `-r` |
| 子包脚本执行 | ✅ `--workspaces` | ✅ | ✅ | ✅ 丰富过滤 |
| 依赖提升控制 | ❌ | ❌ | ✅ (constraints) | ✅ `hoist: false` 默认 |
| workspace 协议 | ❌ | ❌ | ✅ `workspace:*` | ✅ `workspace:*` |

**pnpm workspace 协议**：
```json
{
  "dependencies": {
    "@my-org/shared": "workspace:*"
  }
}
```
`workspace:*` 在发布时自动替换为实际版本号。

---

## 六、选型建议

```
┌─────────────────────────────────────────────┐
│              你应该用哪个？                   │
│                                              │
│  个人项目 / 新手学习                          │
│  → npm（官方、生态最全、不用额外安装）         │
│                                              │
│  公司项目 / 团队协作                          │
│  → pnpm（磁盘省、速度快、杜绝幽灵依赖）        │
│                                              │
│  Monorepo 管理                               │
│  → pnpm workspace（最成熟、filter功能强大）    │
│                                              │
│  大型项目 / 需要离线 / 确定性                  │
│  → Yarn Berry（PnP 模式零安装）               │
│                                              │
│  追求极致速度 / 全 JavaScript 工具链           │
│  → Bun（运行+包管+打包+测试 All-in-One）       │
│                                              │
│  只想稳定不折腾                               │
│  → npm（默认不犯错）                           │
└─────────────────────────────────────────────┘
```

---

## 七、迁移指南

### 从 npm 迁移到 pnpm

```bash
# 1. 全局安装 pnpm
npm install -g pnpm

# 2. 删除旧的 node_modules 和 lock
rm -rf node_modules package-lock.json

# 3. 用 pnpm 安装
pnpm install

# 4. 如果之前有幽灵依赖 → 代码报错
# 修复：安装缺失的依赖
pnpm add <缺失的包名>
```

### 从 npm 迁移到 yarn

```bash
npm install -g yarn
rm -rf node_modules package-lock.json
yarn install
```

---

> **一句话总结**：npm 是基石和标准，pnpm 是现代最佳实践（省磁盘+防幽灵依赖），yarn 推动了生态进步（lock 文件/workspaces）,Bun 代表了未来方向（全工具链合一）。团队协作首选 pnpm。
