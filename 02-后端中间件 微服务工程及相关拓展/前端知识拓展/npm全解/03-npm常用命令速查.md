# 03 - npm 常用命令速查

> 🎯 后端最常用的 15 条 npm 命令 — 从安装依赖到运行脚本，覆盖日常接触前端项目的全部操作

---

## 目录

1. [依赖管理命令](#1-依赖管理命令)
2. [脚本执行命令](#2-脚本执行命令)
3. [信息查看命令](#3-信息查看命令)
4. [全局与配置命令](#4-全局与配置命令)

---

## 1. 依赖管理命令

| 命令 | 说明 | Java 对标 |
|------|------|----------|
| `npm install` | 安装所有依赖（读 package.json） | `mvn dependency:resolve` |
| `npm install <pkg>` | 安装并添加到 dependencies | 添加 `<dependency>` + 下载 |
| `npm install <pkg> -D` | 安装到 devDependencies | — |
| `npm install <pkg> -g` | 全局安装 | 安装 CLI 工具 |
| `npm uninstall <pkg>` | 卸载依赖 | 删除 `<dependency>` |
| `npm update` | 更新依赖（遵守 ^/~ 范围） | — |
| `npm ci` | ⭐ 严格按 lock 安装（CI 环境使用） | `mvn clean install -o` |

```bash
# ═══ 最常用 ═══
npm install                         # 安装所有依赖
npm install axios                   # 添加生产依赖
npm install eslint -D               # 添加开发依赖

# ═══ CI 环境 ═══
npm ci                              # ⚠️ 比 npm install 更严格：
                                    #    删除 node_modules，严格按 lock 安装
                                    #    不会修改 package-lock.json

# ═══ 更新 ═══
npm outdated                        # 查看可更新的依赖
npm update lodash                   # 更新指定包
npm update                          # 更新所有（遵守版本范围）

# ═══ 卸载 ═══
npm uninstall axios                 # 卸载 + 从 package.json 移除
```

| `npm install` vs `npm ci` | npm install | npm ci |
|---------------------------|:---:|:---:|
| 按 lock 文件 | ⚠️ 可能更新 lock | ✅ 严格按 lock |
| 速度 | 慢 | ⭐ 快 |
| 使用场景 | 本地开发 | **CI/CD 环境** |
| 前提 | package.json | package.json + lock |

---

## 2. 脚本执行命令

```bash
# ═══ npm scripts 执行 ═══
npm run dev         # 启动开发服务器
npm run build       # 构建生产包
npm test            # 运行测试（等价 npm run test）
npm start           # 启动（等价 npm run start）
npm run lint        # 代码检查

# ═══ npx ═══
npx create-vite my-app    # 临时执行包命令（无需安装）
npx eslint src/ --fix     # 执行本地安装的 eslint
```

---

## 3. 信息查看命令

```bash
# ═══ 查看已安装 ═══
npm list                    # 依赖树（所有层级）
npm list --depth=0          # 仅直接依赖
npm list -g --depth=0       # 全局安装的包

# ═══ 查看包信息 ═══
npm view vue version        # 最新版本
npm view vue versions       # 所有版本
npm view axios dependencies # 查看 axios 的依赖

# ═══ 安全 ═══
npm audit                   # 安全漏洞扫描
npm audit fix               # 自动修复（可安全的更新）
npm audit fix --force       # 强制修复（可能 breaking change）
```

---

## 4. 全局与配置命令

```bash
# ═══ 全局安装（CLI 工具） ═══
npm install -g typescript
npm install -g @vue/cli

# ═══ 配置 ═══
npm config get registry                     # 查看当前 registry
npm config set registry https://registry.npmmirror.com  # 设置淘宝镜像
npm config list                             # 查看所有配置

# ═══ 缓存 ═══
npm cache clean --force        # 清理缓存

# ═══ 初始化项目 ═══
npm init                       # 交互式创建 package.json
npm init -y                    # 默认配置快速创建

# ═══ 发布 ═══
npm login
npm publish
npm version patch              # 自动更新版本号（1.0.0 → 1.0.1）
```

### npm vs yarn vs pnpm

| 命令 | npm | yarn | pnpm |
|------|-----|------|------|
| 安装依赖 | `npm install` | `yarn` | `pnpm install` |
| 添加依赖 | `npm install <pkg>` | `yarn add <pkg>` | `pnpm add <pkg>` |
| CI 安装 | `npm ci` | `yarn --frozen-lockfile` | `pnpm install --frozen-lockfile` |
| 全局安装 | `npm install -g` | `yarn global add` | `pnpm add -g` |
| 速度 | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ 最快 |
| 磁盘效率 | 低（扁平化去重） | 中 | 高（硬链接共享） |

> 🎯 **后端常用组合**：`npm install`（开发）、`npm ci`（CI）、`npm run build`（构建）、`npm audit fix`（安全）、`npm ls --depth=0`（看直接依赖）。这 5 条够用 90% 的场景。
