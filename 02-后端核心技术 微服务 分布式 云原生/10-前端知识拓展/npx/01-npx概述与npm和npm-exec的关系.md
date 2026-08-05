# 01 - npx 概述与 npm / npm exec 的关系

> 🎯 npx 是 npm 生态的"命令执行器" — npm 负责"装"，npx 负责"跑"。它随 Node.js 内置（npm 5.2+），npm 7+ 之后成为 `npm exec` 的兼容入口。用 Java 后端的视角理解：npx ≈ `mvn exec:java` + `mvn archetype:generate`

---

## 目录

1. [npx 是什么](#1-npx-是什么)
2. [npm vs npx：装与跑的分工](#2-npm-vs-npx装与跑的分工)
3. [npx vs npm exec：npm 7 之后的统一](#3-npx-vs-npm-execnpm-7-之后的统一)
4. [何时用 npx / npm / npm run](#4-何时用-npx--npm--npm-run)

---

## 1. npx 是什么

npx（Node Package Execute）是 npm 自带的 CLI 工具，用于**执行 npm 包中的命令行工具**，核心能力：包没装也能跑。

| 特点 | 说明 |
|------|------|
| **无需全局安装** | 临时工具用完即走，不污染全局环境 |
| **本地优先** | 项目里已安装的二进制直接用本地的 |
| **版本可控** | `npx prettier@3.3.3` 指定任意版本执行 |
| **内置随附** | 安装 Node.js 即自带（npm 5.2.0+，2017 年引入） |

```bash
# 查看版本（npx 与 npm 版本号一致，因为它是 npm 内置命令）
node -v    # v22.14.0
npm -v     # 10.9.2
npx -v     # 10.9.2
```

> 💡 **为什么不用全局安装？** 全局安装（`npm install -g`）会造成版本冲突与环境污染 — 不同项目需要不同版本的 prettier/eslint 时，全局只有一个。npx 让每个项目各取所需，类似 Java 项目的 Maven 依赖隔离。

---

## 2. npm vs npx：装与跑的分工

| 对比项 | npm | npx |
|--------|-----|-----|
| 定位 | 包管理器（装） | 命令执行器（跑） |
| 作用 | 安装/卸载/更新依赖，管理 package.json | 执行包里的 CLI 命令 |
| 安装结果 | 永久保留（node_modules / 全局目录） | 临时下载，用完由缓存机制处理 |
| 典型场景 | `npm install vue` | `npx create-vite my-app` |
| Java 对标 | Maven 依赖管理 | `mvn exec:java` |

```bash
# npm：把依赖正式装进项目
npm install -D prettier          # 写入 devDependencies，永久保留

# npx：不装进项目，直接执行
npx prettier --version           # 临时下载 prettier 并执行
```

**一句话：npm 建房子，npx 修屋顶** — npm 提供永久的项目依赖基础，npx 处理临时性的工具执行。

---

## 3. npx vs npm exec：npm 7 之后的统一

### 3.1 历史背景

| 版本 | 变化 |
|------|------|
| npm 5.2.0（2017） | npx 作为独立工具引入 |
| npm 7.0.0（2020） | **npx 被重写并整合进 `npm exec` 机制**，成为 npm exec 的兼容入口/包装器 |
| npm 7+ 至今 | 独立的 npx 包已废弃，无需再 `npm install -g npx` |

> 💡 **结论**：现代 npm 中 npx 就是 `npm exec` 的别名，`npm x` 是 npm exec 的简写。三个写法本质相同。

### 3.2 主要区别：参数解析

| 对比项 | npx | npm exec |
|--------|-----|----------|
| 选项与参数 | 选项必须放在位置参数**之前** | 用 `--`（双连字符）分隔 npm 选项与要执行的命令 |
| workspaces 支持 | 有限 | 完整（`--workspace`） |
| 社区使用度 | 更常见（文档、教程、脚本） | 官方推荐，更规范 |

```bash
# npx：选项放在参数后会被当作命令的参数！
$ npx foo@latest bar --package=@npmcli/foo
# 实际执行：foo bar --package=@npmcli/foo   ← --package 被吞进 foo 的参数

# npm exec：会先解析 --package 选项
$ npm exec foo@latest bar --package=@npmcli/foo
# 实际执行：foo@latest bar

# 加上 -- 后与 npx 等价（推荐写法）
$ npm exec -- foo@latest bar --package=@npmcli/foo
```

### 3.3 等价写法对照

| 场景 | npx 写法 | npm exec 写法 |
|------|----------|---------------|
| 跑本地依赖命令 | `npx prettier --write .` | `npm exec -- prettier --write .` |
| 临时安装指定包 | `npx -p typescript tsc -v` | `npm exec -p typescript -- tsc -v` |
| 固定版本 | `npx eslint@8.57.0 .` | `npm exec -- eslint@8.57.0 .` |
| shell 命令串 | `npx -c 'eslint . && tsc'` | `npm exec -c 'eslint . && tsc'` |
| 多包同时提供 | `npx -p pkg1 -p pkg2 cmd` | `npm exec -p pkg1 -p pkg2 -- cmd` |
| 简写 | `npx` | `npm x` |

---

## 4. 何时用 npx / npm / npm run

| 场景 | 推荐 | 原因 |
|------|------|------|
| 临时执行一个工具 | **npx** | 用完即走，无需安装 |
| 创建新项目 | **npx** | `npx create-vite@latest` 无需全局装脚手架 |
| 直接跑本地依赖中的 CLI | **npx** | 自动从 `node_modules/.bin` 找到二进制 |
| 安装项目长期依赖 | **npm** | `npm install` 写入 package.json |
| 管理依赖版本 | **npm** | 更新/卸载/锁定 |
| 执行项目内定义的脚本 | **npm run** | `npm run build` 读取 package.json 的 scripts |
| CI 流水线 | **npm ci + npm run** | 只执行 lockfile 里固定好的二进制，杜绝漂移 |

```bash
# 典型工程化工作流
npm install              # 装依赖
npx prettier --write .   # 格式化（本地有就用本地的）
npx eslint .             # 代码检查
npm run build            # 构建
```

> ⚠️ **常见误区**：`npx` 和 `npm run` 都带命令，但完全不同 — `npm run xxx` 只能执行 `package.json` 的 `scripts` 里定义好的脚本；`npx xxx` 执行的是包里的二进制（无论是否安装过）。

---

> 🎯 **核心要点**：现代 npm 里 npx = npm exec 的兼容入口。npx 负责"跑任意包的命令"，npm 负责"装正式依赖"，npm run 负责"跑项目自定义脚本"。三者分工明确，Java 后端看到 `npx create-xxx` 只需理解成"免安装执行脚手架"即可。

**下一模块**：[02-npx核心用法与参数详解](02-npx核心用法与参数详解.md) / **返回总览**：[00-npx知识体系总览](00-npx知识体系总览.md)
