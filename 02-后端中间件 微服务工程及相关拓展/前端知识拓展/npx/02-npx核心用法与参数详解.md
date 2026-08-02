# 02 - npx 核心用法与参数详解

> 🎯 npx 的全部核心用法浓缩于此：跑本地命令、临时下载、固定版本、选项速查、参数解析陷阱。掌握本章即可应对 95% 的 npx 场景

---

## 目录

1. [基本用法：跑起来再说](#1-基本用法跑起来再说)
2. [版本控制：临时指定与固定版本](#2-版本控制临时指定与固定版本)
3. [常用选项速查](#3-常用选项速查)
4. [参数解析规则（关键陷阱）](#4-参数解析规则关键陷阱)
5. [npx vs npm run：执行命令的两种方式](#5-npx-vs-npm-run执行命令的两种方式)

---

## 1. 基本用法：跑起来再说

### 1.1 最简形态：直接跑

```bash
# 包没装？没关系，npx 会临时下载后执行
npx cowsay "hello java-backend"
```

### 1.2 跑本地已安装的工具（最常用）

```bash
# 1. 项目里装好工具（推荐 -D，工具属于开发依赖）
npm install -D prettier

# 2. npx 优先使用 node_modules 里的版本，不重复下载
npx prettier --write src/
```

> 💡 **等价关系**：`npx prettier` 与直接执行 `./node_modules/.bin/prettier` 等价，但 npx 写法更简洁，且工具没装时会自动兜底临时安装。

### 1.3 检查环境

```bash
npx --version          # npx 版本（同 npm 版本）
npx node --version     # 执行 node 二进制
```

---

## 2. 版本控制：临时指定与固定版本

| 写法 | 含义 | 场景 |
|------|------|------|
| `npx prettier` | 本地有就用本地，没有则拉最新 | 日常使用 |
| `npx prettier@3.3.3` | 精确指定版本 | 复现问题、对齐团队版本 |
| `npx prettier@^3` | 指定大版本范围 | 需要 3.x 特性 |
| `npx create-vite@latest` | 显式拉取最新版 | 脚手架（避免本地缓存旧版） |
| `npx -p typescript@5.6.2 tsc -v` | 包名与命令名不同时指定 | 临时提供任意包 |

```bash
# 精确版本：测试某个版本是否修复了问题
npx prettier@3.3.3 --version    # 3.3.3

# 包名 ≠ 命令名：--package 指定包，再执行其中的命令
npx -p typescript@5.6.2 tsc --version   # Version 5.6.2
```

> ⚠️ **脚手架必须用 @latest**：如果本地缓存了旧版 create-vite，直接 `npx create-vite` 可能用到缓存旧版。写 `npx create-vite@latest` 确保拿到最新。

---

## 3. 常用选项速查

| 选项 | 简写 | 说明 |
|------|:---:|------|
| `--package <pkg>` | `-p` | 指定要提供/安装的包（包名 ≠ 命令名时必需） |
| `--yes` | `-y` | 跳过"是否安装远程包"的确认提示 |
| `--no` | — | 拒绝安装远程包（已废弃 `--no-install` 的替代） |
| `--call <cmd>` | `-c` | 在包含包二进制的 PATH 下执行 shell 命令串 |
| `--workspace <name>` | `-w` | 在指定 workspace 中执行（npm exec 特性） |
| `--ignore-existing` | — | （已移除）强制忽略本地已安装版本 |
| `--shell <shell>` | — | （已移除）被 `--script-shell` 替代 |

```bash
# -p 指定多个包，同时提供
npx -p @nestjs/cli -p typescript nest new my-app

# -y 跳过确认（CI 等非交互环境必需）
npx -y create-vite@latest my-app

# -c 执行命令串（支持管道、重定向）
npx -c 'eslint . && prettier --write .'

# --no：只跑本地有的，远程包一律拒绝
npx --no some-tool
```

> 💡 **`-p` 的坑**：`-p` 在 **npx** 里是 `--package` 的简写；但在 **npm** 里 `-p` 是 `--parseable`（机器可读输出）的简写。两个工具对同一简写含义不同，混用会翻车。

---

## 4. 参数解析规则（关键陷阱）

### 4.1 npx：选项必须在位置参数之前

```bash
# ✅ 正确：选项在前
npx -p typescript tsc --version

# ❌ 错误：--package 会被当成 tsc 的参数吞掉
npx tsc --version -p typescript
```

### 4.2 npm exec：用 `--` 分隔

```bash
# npm exec 中 -- 之后全是命令参数
npm exec -- tap --bail test/foo.js
npm exec -p pkg@1 -- pkg --custom-flag
```

### 4.3 给被执行的命令传参

```bash
# 传参给 prettier
npx prettier --write --tab-width 2 src/

# 用 -c 避免参数歧义
npx -c 'prettier --write "$@"' sh -- src/
```

> 🎯 **记忆口诀**：npx 选项在左，命令参数在右；npm exec 用 `--` 划清界限。

---

## 5. npx vs npm run：执行命令的两种方式

| 对比项 | npx | npm run |
|--------|-----|---------|
| 执行对象 | 任意 npm 包中的二进制 | package.json 中 scripts 定义的命令 |
| 是否需要声明 | 否，即用即跑 | 是，必须先写进 package.json |
| 版本来源 | 本地 node_modules / 临时下载 | 本地 node_modules 中的二进制 |
| 典型场景 | 临时工具、脚手架、一次性任务 | 项目构建、测试、启动等固定流程 |

```json
// package.json 中声明好 scripts
{
  "scripts": {
    "lint": "eslint .",
    "format": "prettier --write ."
  }
}
```

```bash
npm run lint      # ✅ 走脚本（推荐：团队约定统一）
npx eslint .      # ✅ 临时跑（推荐：没配脚本时）
```

> 💡 **最佳实践**：项目内的固定流程（lint/format/build/test）写进 `scripts` 用 `npm run`；临时的、一次性的、工具性的用 `npx`。脚本里调工具时优先用 `npm run` 间接调用，避免版本漂移。

---

> 🎯 **核心要点**：npx 五板斧 — ① 直接跑（自动装）② 本地优先 ③ `@版本` 固定 ④ `-p` 指定包 ⑤ `-c` 跑命令串。牢记"选项在前、参数在后"，`@latest` 用于脚手架，`-y` 用于 CI。

**下一模块**：[03-npx实战场景大全](03-npx实战场景大全.md) / **返回总览**：[00-npx知识体系总览](00-npx知识体系总览.md)
