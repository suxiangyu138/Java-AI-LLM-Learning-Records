# Git 子模块与大仓管理

## 多个仓库共享代码的三种策略

当你需要在多个项目间共享代码时，有三种主流方案：

| 方案 | 一句话 | 复杂度 |
|------|--------|--------|
| **submodule** | 仓库A"引用"仓库B的某个commit | 中 |
| **subtree** | 仓库A"拷贝"仓库B的代码，但保留合并能力 | 高 |
| **monorepo** | 所有项目放在一个仓库 | 低→高 |

---

## 1. Git Submodule（子模块）

### 核心概念

子模块是一个**指向另一个仓库某个 commit 的指针**。

```text
主仓库 (main-repo)
├── src/
├── lib/
│   └── shared-utils/    ← 子模块，指向 shared-utils 仓库的 commit a1b2c3d
├── .gitmodules          ← 子模块配置
└── package.json
```

### 1.1 添加子模块

```bash
# 添加子模块
git submodule add https://github.com/team/shared-utils.git lib/shared-utils

# 发生了什么？
# 1. 克隆 shared-utils 到 lib/shared-utils/
# 2. 创建 .gitmodules 配置文件
# 3. 在 index 中记录子模块的 commit SHA

# 查看 .gitmodules
cat .gitmodules
# [submodule "lib/shared-utils"]
#     path = lib/shared-utils
#     url = https://github.com/team/shared-utils.git

# 提交子模块的引入
git add .gitmodules lib/shared-utils
git commit -m "chore: add shared-utils as submodule"
```

### 1.2 克隆含子模块的仓库

```bash
# 方式1：克隆时一并初始化子模块
git clone --recurse-submodules https://github.com/team/main-repo.git

# 方式2：先克隆，再手动初始化
git clone https://github.com/team/main-repo.git
cd main-repo
git submodule init      # 读取 .gitmodules 配置
git submodule update    # 拉取子模块代码

# 或者一条命令
git submodule update --init --recursive
```

### 1.3 更新子模块

```bash
# 拉取子模块的最新提交
cd lib/shared-utils
git fetch
git checkout main      # 或切换到你想要的分支/commit
cd ../..

# 提交子模块的"指针更新"
git add lib/shared-utils
git commit -m "chore: update shared-utils to v2.1.0"

# 或者一行命令
git submodule update --remote lib/shared-utils
```

### 1.4 工作区子模块状态

```bash
# 查看子模块状态
git submodule status
#  a1b2c3d lib/shared-utils (v2.0.0)
# +e4f5g6h lib/shared-utils (v2.0.0-1-g...)  ← + 表示有本地未提交的修改

# 递归更新所有子模块
git submodule update --init --recursive

# 对每个子模块执行命令
git submodule foreach 'git status'
git submodule foreach 'git pull origin main'
```

### 1.5 删除子模块

```bash
# 现代 Git (推荐)
git submodule deinit lib/shared-utils
git rm lib/shared-utils
# 提交删除
git commit -m "chore: remove shared-utils submodule"
```

### 1.6 子模块的坑

```text
❌ 忘记 --recurse-submodules，克隆后代码不完整
❌ 子模块改了没提交，主仓库记录了新SHA但子模块没push
❌ git pull 不会自动更新子模块（需要 git submodule update）
❌ 切换分支后子模块可能处于旧commit，需要重新 update
```

**解决方案**：配置自动更新

```bash
# git pull 时自动更新子模块
git config --global submodule.recurse true

# 显示子模块的摘要变更
git config --global status.submoduleSummary true
git config --global diff.submodule log
```

---

## 2. Git Subtree（子树合并）

### 子模块 vs 子树

| 维度 | Submodule | Subtree |
|------|----------|---------|
| 代码存储 | 子模块是"指针"，代码在独立仓库 | 子树是"拷贝"，代码在主仓库中 |
| 克隆 | 需要额外步骤 | `git clone` 直接获得完整代码 |
| 更新 | 手动 + 子仓库推送 | `git subtree pull/push` |
| 适合 | 明确边界、独立发布 | 紧密耦合、不需要独立仓库 |
| 学习曲线 | 中等 | 较高 |

### 2.1 添加子树

```bash
# 将外部仓库的代码作为子树加入
git subtree add --prefix=lib/shared-utils \
  https://github.com/team/shared-utils.git main --squash

# --squash：不保留子仓库的完整历史，只产生一个merge commit
# --prefix：子树的目录路径
```

### 2.2 更新子树

```bash
# 拉取子树的最新代码
git subtree pull --prefix=lib/shared-utils \
  https://github.com/team/shared-utils.git main --squash
```

### 2.3 向子树源仓库推送修改

```bash
# 如果你修改了子树中的代码，想推送回源仓库
git subtree push --prefix=lib/shared-utils \
  https://github.com/team/shared-utils.git main
```

---

## 3. Monorepo（单一大仓）

### 概念

将所有项目、库、服务放在一个 Git 仓库中。

```text
monorepo/
├── apps/
│   ├── web/             # 前端应用
│   ├── api/             # 后端服务
│   └── mobile/          # 移动端
├── packages/
│   ├── shared-utils/    # 共享工具库
│   ├── ui-components/   # UI 组件库
│   └── eslint-config/   # ESLint 配置
├── package.json         # workspace root
└── turbo.json           # 构建编排
```

### 优势 vs 挑战

| 优势 | 挑战 |
|------|------|
| 统一版本控制，一次 commit 跨所有项目 | 仓库体积巨大（需要浅克隆、部分检出） |
| 原子化重构（改 shared-utils → 所有 app 同步） | CI 时间长（需要增量构建） |
| 统一的 lint/test/build 配置 | 权限管理困难（谁可以改什么） |
| 团队协作更透明 | 工具链要求高 |

### 关键工具

| 工具 | 生态 | 作用 |
|------|------|------|
| **Turborepo** | JS/TS | 增量构建、并行任务、缓存 |
| **Nx** | JS/TS | 构建系统 + 依赖图 + 代码生成 |
| **Bazel** | 多语言 | Google 的构建系统，极致增量 |
| **Lerna** | JS/TS | 版本管理和发布 |
| **Rush** | JS/TS | 微软的 monorepo 管理器 |
| **pnpm workspace** | JS/TS | 包管理 + workspace 隔离 |

### 关键实践

```bash
# 部分克隆（只克隆需要的部分）
git clone --filter=blob:none https://github.com/org/monorepo.git

# Sparse Checkout（只检出部分目录）
git sparse-checkout init --cone
git sparse-checkout set apps/web packages/shared-utils

# Git LFS（管理大文件）
git lfs track "*.psd" "*.mp4" "*.zip"
```

---

## 4. 方案选择指南

```text
┌─────────────────────────────────────────────────┐
│            如何选择代码共享方案                    │
├──────────────────┬──────────────────────────────┤
│ 共享库有独立版本  │ → Submodule                   │
│ 共享库紧密耦合    │ → Subtree 或 Monorepo          │
│ 团队 < 10人      │ → Submodule 或 Monorepo        │
│ 团队 > 50人      │ → Monorepo + 专业工具          │
│ 开源项目         │ → 避免 Submodule（增加贡献门槛）│
│ 需要独立CI/CD    │ → Submodule / 多仓库           │
│ 原子化重构       │ → Monorepo                     │
└──────────────────┴──────────────────────────────┘
```

---

> 上一篇：[08-Git标签与语义化版本发布](08-Git标签与语义化版本发布.md)
> 下一篇：[10-Git高级调试与取证工具](10-Git高级调试与取证工具.md)
