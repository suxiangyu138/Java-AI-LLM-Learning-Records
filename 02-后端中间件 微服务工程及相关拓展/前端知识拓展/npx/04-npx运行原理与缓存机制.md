# 04 - npx 运行原理与缓存机制

> 🎯 npx 看似魔法，实则五步流水线：解析 spec → 查本地 → 查全局 → 临时安装 → 加入 PATH 执行。理解底层机制才能排查"npx 为什么用了旧版本"这类问题

---

## 目录

1. [npx 执行流程五步](#1-npx-执行流程五步)
2. [缓存机制与 _npx 目录](#2-缓存机制与-_npx-目录)
3. [版本选择逻辑](#3-版本选择逻辑)
4. [npm 7+ 行为变化一览](#4-npm-7-行为变化一览)

---

## 1. npx 执行流程五步

```
npx prettier --write src/
│
├─ ① 解析 spec（prettier 是什么包、什么版本范围）
│
├─ ② 查本地：node_modules/.bin/prettier 存在？
│      ├─ ✅ 存在 → 直接执行（结束）
│      └─ ❌ 不存在 ↓
│
├─ ③ 查全局：全局安装的 prettier 存在？
│      └─ 均不存在 ↓
│
├─ ④ 提示确认（-y 跳过）→ 临时下载到 npm 缓存 → 解压到 _npx 目录
│
└─ ⑤ 把 _npx/<hash>/node_modules/.bin 加入 PATH → 执行命令
```

| 步骤 | 对应 Java 概念 |
|------|---------------|
| ① 解析 spec | 解析 Maven 坐标（groupId:artifactId:version） |
| ② 本地查找 | 查找当前 classpath / `.m2` 中的依赖 |
| ③ 全局查找 | 查找 MAVEN_HOME 全局仓库 |
| ④ 临时安装 | Maven 首次使用时从中央仓库下载 |
| ⑤ PATH 执行 | `mvn exec:java` 执行 main |

---

## 2. 缓存机制与 _npx 目录

### 2.1 临时安装到哪里

```bash
# 未安装的包被解压到 npm 缓存的 _npx 子目录
npm cache dir
# C:\Users\<user>\AppData\Local\npm-cache\_npx\   ← Windows
# /home/<user>/.npm/_npx/                          ← Linux/macOS
```

```text
_npx/
└── a1b2c3d4e5f6/                # 按 spec 哈希生成的目录
    └── node_modules/
        └── .bin/                # 本次执行期间加入 PATH
            ├── prettier
            ├── prettier.cmd     # Windows 使用 .cmd 包装器
            └── ...
```

### 2.2 缓存的保留与清理

| 行为 | 说明 |
|------|------|
| 保留 | 临时下载的包**不会自动清理**，供后续复用 |
| 复用 | 相同 spec（如 `prettier@3.3.3`）再次执行直接命中缓存，不再下载 |
| 更新 | npm 11+ 中，spec 为版本范围时会尽可能更新 npx 缓存 |
| 清理 | `npm cache clean --force` 或手动删除 `_npx` 目录 |

> ⚠️ **常见疑问**：npx 临时安装的包**不会**写入当前项目的 `node_modules`，也不会写入 `package.json` — 它对项目"零污染"，这是设计意图。

### 2.3 执行期间的环境

```bash
# 执行期间 npx 会临时修改 PATH，命令结束后恢复
# 因此以下命令中 prettier 可用，但子 shell 结束后不可见
npx -c 'prettier --version && which prettier'
```

> 💡 **后端视角**：类似 `mvn dependency:get` 拉取依赖后缓存到本地仓库 — 但 npx 的缓存是按"spec 哈希"隔离的临时安装，与项目的依赖树无关。

---

## 3. 版本选择逻辑

```
npx prettier            # 无版本 → ① 本地（任意版本）→ ② 全局 → ③ 缓存 _npx → ④ registry 最新
npx prettier@3.3.3      # 固定版本 → ① 本地匹配版本 → ② 全局匹配 → ③ 缓存匹配 → ④ registry 下载
npx create-vite@latest  # @latest → 跳过本地/全局，直接取 registry 最新
```

| 场景 | 实际执行的版本 | 原因 |
|------|----------------|------|
| 项目已装 prettier 3.1.0，执行 `npx prettier` | 3.1.0（本地） | 本地优先 |
| 未安装，执行 `npx prettier` | registry 最新版 | 无本地/全局可命中 |
| 执行 `npx prettier@3.3.3` | 3.3.3 | 固定版本精确匹配 |
| 执行 `npx create-vite@latest` | 最新（绕过缓存） | `latest` 标签强制远程 |

> ⚠️ **排查指南**："npx 用了旧版本？" — ① 检查 `node_modules/.bin` 里是否已有该工具（本地优先会赢过 @latest 以外的写法）② 检查 `_npx` 缓存（无版本范围时命中缓存）③ 检查全局安装（`npm ls -g`）。

---

## 4. npm 7+ 行为变化一览

| 旧行为（npm 6） | 新行为（npm 7+） | 说明 |
|-----------------|------------------|------|
| 静默安装远程包 | **安装前提示确认** | 可用 `-y`/`--yes` 跳过，CI 中必须 `-y` |
| `--no-install` | 已废弃 → `--no` | 拒绝安装远程包，只跑本地 |
| shell fallback（命令不存在时进入交互 shell） | **已移除** | 不再提供 |
| `--ignore-existing` | 已移除 | 无法强制忽略本地版本 |
| `--shell` | 改为 `--script-shell` | 指定脚本执行 shell |
| 独立 npx 包 | 集成进 npm 内置 | 无需 `npm install -g npx` |
| — | npm 11：npx 命令集成进 npm 缓存系统 | 缓存策略更统一 |

```bash
# npm 7+ 的典型非交互用法
npx -y create-vite@latest my-app          # CI 脚本中必须 -y
npx --no some-tool                        # 只允许本地二进制
```

> 🎯 **核心要点**：npx = 五步流水线（解析→本地→全局→临时装→PATH 执行）；临时安装落在 `_npx` 缓存目录且对项目零污染；版本选择严格遵循"本地 > 全局 > 缓存 > registry"；npm 7+ 后所有安装前都会确认，`-y` 是自动化环境的必需品。

**下一模块**：[05-npx安全实践与供应链风险](05-npx安全实践与供应链风险.md) / **返回总览**：[00-npx知识体系总览](00-npx知识体系总览.md)
