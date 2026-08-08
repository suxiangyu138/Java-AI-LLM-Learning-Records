# 04-Git 合并与变基
> merge vs rebase 是 Git 最经典的争论——核心是在"保留真实历史"和"整洁线性历史"之间做选择，外加冲突解决与 cherry-pick

## 📚 目录
1. [合并（Merge）的三种策略](#1-合并merge的三种策略)
2. [变基（Rebase）深度解析](#2-变基rebase深度解析)
3. [交互式 Rebase：整理提交历史](#3-交互式-rebase整理提交历史)
4. [Cherry-pick：精准提交移植](#4-cherry-pick精准提交移植)
5. [merge vs rebase 终极对比](#5-merge-vs-rebase-终极对比)
6. [三方合并的底层原理](#6-三方合并的底层原理)
7. [冲突解决完整指南](#7-冲突解决完整指南)
8. [高级合并选项与 rerere](#8-高级合并选项与-rerere)
9. [PR/MR 合并选项](#9-prmr-合并选项)
10. [合并策略速查](#10-合并策略速查)
11. [核心要点](#11-核心要点)
12. [参考来源](#12-参考来源)

## 1. 合并（Merge）的三种策略

### 1.1 Fast-Forward Merge（快进合并）

目标分支没有新提交时，Git 直接移动指针：

```bash
git checkout main
git merge feature/login
# 结果：HEAD 直接指向 feature/login 的最新 commit，历史线性，无 merge commit
```

### 1.2 Three-Way Merge（三方合并）

两个分支都有新提交时创建 merge commit：

```bash
git merge feature/login
# 产生一个 merge commit，有两个 parent
```

```text
非快进合并（main 与 feature 分叉，产生合并提交 M）
main:    A ──────── B ── M (merge commit, 两个 parent: B 和 C)
feature:    └ C ──┘
```

### 1.3 --no-ff Merge（强制非快进）

即使可以快进也强制创建 merge commit，保留"这个 feature 分支存在过"的信息：

```bash
git merge --no-ff feature/login
```

### 1.4 Squash Merge（压缩合并）

```bash
git merge --squash feature/login
git commit -m "feat: complete login module"
# 所有 feature 的 commits 变成 main 上的一个 commit
```

| Squash 优点 | Squash 缺点 |
|------------|------------|
| main 分支历史清爽 | 丢失中间 commit 的细节 |
| 一个功能一个 commit | 无法追溯"这个 bug 是哪个 commit 引入的" |

### 1.5 策略对比

| 策略 | 命令 | 历史形状 | 适用 |
|------|------|---------|------|
| Fast-Forward | `git merge <branch>` | 线性 | 个人开发 |
| Three-Way | `git merge <branch>` | 分叉后再汇聚 | 分支并行开发 |
| No Fast-Forward | `git merge --no-ff <branch>` | 保留分叉记录 | 需要追溯 feature 边界（企业推荐） |

> 💡 企业级建议：**禁止快进，保留合并记录**（`git merge --no-ff`）→ 历史可追溯。

## 2. 变基（Rebase）深度解析

### 2.1 原理三步

```bash
git checkout feature
git rebase main
```

1. 找到 feature 和 main 的**共同祖先**；
2. 把 feature 上独有的 commit **逐个 cherry-pick** 到 main 的最新位置；
3. 把 feature 指针移到新位置。

```text
Rebase 前:                          Rebase 后:
main:    A ── B                     main:    A ── B ── C' (feature 重放)
feature: A ── C (与 B 分叉)         feature:      └ C' ┘ (历史线性化)
```

> ⚠️ 注意：`C'` 是**全新的 commit**，hash 不同了！这就是"改写历史"的含义。

### 2.2 Rebase 黄金法则

> ⚠️ **永远不要 rebase 已经推送到公共仓库的 commit**

```text
为什么？
1. Rebase 会产生全新的 commit hash
2. 如果别人基于你原来的 commit 开发了，他们会被坑
3. 强行 push (--force) 会覆盖别人的工作

简单判断：
  这个 commit 只有你在用？ → 可以 rebase
  有人可能在你的 commit 上开发？ → 绝对不要 rebase
```

```bash
# ✅ 安全：rebase 从未 push 过的本地分支
git checkout feature/draft
git rebase main

# ❌ 危险：rebase 已 push 的分支
git checkout feature/shared
git rebase main
git push --force    # 同事的本地分支会乱掉！

# ✅ 安全替代：merge
git checkout feature/shared
git merge main      # 不重写历史，多一个 merge commit 而已
```

### 2.3 Rebase 实战场景

```bash
# 场景 1：feature 落后 main → 变基到最新
git checkout feature/login
git rebase main
# 冲突解决 → git add → git rebase --continue

# 场景 2：整理 typo commit → 交互式变基
git rebase -i HEAD~5

# 场景 3：rebase 后推送（安全版）
git push --force-with-lease origin feature/login
# --force-with-lease 比 --force 安全：
#   如果远程有你不知道的新 commit → 拒绝推送，防止覆盖同事的代码
```

## 3. 交互式 Rebase：整理提交历史

```bash
git rebase -i HEAD~3     # 整理最近 3 个 commit
# 或
git rebase -i main       # 基于某个分支变基
```

编辑器界面：

```text
pick a1b2c3d feat: add login form
pick e4f5g6h fix: typo in login form      ← 应该和上一个合并
pick i7j8k9l wip: save progress            ← 中间状态，不想要

# 修改为 ↓
pick a1b2c3d feat: add login form
fixup e4f5g6h fix: typo in login form     # 合并到上一个，丢弃 message
drop  i7j8k9l wip: save progress          # 直接删除
```

### Rebase 指令速查表

| 指令 | 效果 |
|------|------|
| `pick` | 保留这个 commit（默认） |
| `reword` | 保留但修改 commit message |
| `edit` | 停下来，允许修改这个 commit 的内容 |
| `squash` | 合并到上一个 commit，**保留** message（可合并编辑） |
| `fixup` | 合并到上一个 commit，**丢弃** message（适合 typo） |
| `drop` | 删除这个 commit |
| `reorder` | 改变 commit 顺序（拖动行） |
| `break` | 在这里暂停（方便中途运行命令） |

### 实战：整理提交历史

```bash
# 1. 查看要整理的范围
git log --oneline main..HEAD

# 2. 启动交互式 rebase
git rebase -i main

# 3. 编辑窗口操作
pick k1l2m3n feat: add auth utils
squash i7j8k9l feat: draft login     # 挤上去
fixup a1b2c3d wip                    # 压上去且不要 message
fixup e4f5g6h fix: typo              # 同上

# 4. 保存退出后，最终 main 上只新增一个干净 commit
```

## 4. Cherry-pick：精准提交移植

| 场景 | 命令 |
|------|------|
| 把 develop 上的一个 Bug 修复同步到 main | `git cherry-pick <commit>` |
| 把 hotfix 的修复合并回 develop | `git cherry-pick <commit>` |
| 只想要别人的某几个提交 | `git cherry-pick <C> <D> <E>` |

```bash
# 场景：hotfix 分支上的安全修复需要移植到 develop
git log hotfix --oneline -5          # 1. 找到需要移植的 commit
git switch develop                   # 2. 切换到目标分支
git cherry-pick a1b2c3d              # 3. 精准移植
# 冲突 → 解决后
git add .
git cherry-pick --continue           # 继续
git cherry-pick --abort              # 放弃

# 多个连续 commit（左开右闭）
git cherry-pick a1b2c3d..e4f5g6h
# 含起点
git cherry-pick a1b2c3d^..e4f5g6h
# 多个不连续 commit
git cherry-pick a1b2c3d i7j8k9l
# 高级参数
git cherry-pick -x 8a2f8c9           # 自动添加 "(cherry picked from commit ...)" 注释
git cherry-pick -n 8a2f8c9           # 只应用变更不自动提交（可合并多个为一个 commit）
git cherry-pick -e 8a2f8c9           # 提交前编辑 message
```

> ⚠️ **独有知识点**：cherry-pick 会创建**新的 commit**（SHA-1 不同），即使内容完全相同。这导致同一修改在两个分支有不同 hash，后续 merge 时 Git 可能无法识别为"已合并"。

## 5. merge vs rebase 终极对比

| 维度 | Merge | Rebase |
|------|-------|--------|
| **历史记录** | 真实，保留所有分叉 | 线性整洁，像是顺序开发 |
| **冲突解决** | 一次性解决所有 | 每个 commit 解决一次（可能多次） |
| **安全性** | 安全，不改变已有 commit | 改写历史，有合作风险 |
| **追溯性** | 容易看到"这个 feature 什么时候合并的" | 难以辨认功能边界 |
| **回滚** | 简单 `git revert -m 1 <merge-commit>` | 需要逐个 revert |
| **适用** | 公共分支、发布分支 | 私有分支、整理提交 |

```text
推荐策略（本地整理用 rebase，公开发布用 merge）：
1. git checkout feature/xxx
2. git rebase main              ← 本地把 commit 整理干净
3. git checkout main
4. git merge --no-ff feature/xxx ← 合并时保留 feature 痕迹
```

## 6. 三方合并的底层原理

```text
三方合并 (Three-Way Merge):

  Commit BASE (共同祖先)
      │
      ├──→ Commit LOCAL (当前分支，如 main 的 HEAD)
      │
      └──→ Commit REMOTE (要合并进来的分支，如 feature 的 HEAD)

Git 自动合并逻辑：
  BASE和LOCAL相同，REMOTE改了 → 用REMOTE的（对方改了）
  BASE和REMOTE相同，LOCAL改了 → 用LOCAL的（我方改了）
  BASE和LOCAL和REMOTE都不一样 → 冲突（需要人工判断）
  BASE没这个文件，只有一方有 → 用有的一方
```

> 💡 理解 merge 不是"自动合并命令"，而是"**人工判断 + 自动辅助**"。遇到冲突时，任务是理解两边代码的意图，做出正确的业务决策。

## 7. 冲突解决完整指南

### 7.1 冲突产生原因

| 原因 | 场景 | 预防 |
|------|------|------|
| 两人改同一文件的同一行 | 并行开发同一模块 | 模块职责清晰 |
| 一人删文件另一人改文件 | 重构+功能开发同时进行 | 重构前通知团队 |
| 配置文件冲突 | 两人加了不同的配置项 | 配置按模块分区 |
| 依赖版本冲突（pom.xml） | 两人升级了同一个依赖 | 依赖管理集中化 |

### 7.2 解决标准流程

```bash
# 1. 合并或 rebase 时遇到冲突
git merge feature/login
# CONFLICT (content): Merge conflict in src/config.js

# 2. 查看冲突文件
git status
# both modified: src/config.js

# 3. 编辑冲突文件（删除冲突标记，选择最终版本）

# 4. 标记为已解决
git add src/config.js

# 5. 继续
git merge --continue     # merge 场景
git rebase --continue    # rebase 场景

# 放弃
git merge --abort        # 回到 merge 前
git rebase --abort       # 回到 rebase 前
```

### 7.3 冲突标记含义

```text
# merge 场景
<<<<<<< HEAD           ← 当前分支（你所在的分支）的版本
   const PORT = 3000;
=======                ← 分隔线
   const PORT = 8080;
>>>>>>> feature/login  ← 被合并分支的版本

# rebase 场景（标记反过来！）
<<<<<<< HEAD           ← 当前是 main 的最新内容
   const PORT = 3000;
=======
   const PORT = 8080;  ← 这是你的 feature commit 内容
>>>>>>> feat: add config ← 你的 commit message
```

> 💡 **记忆技巧**：Merge：`HEAD` = 你当前分支，`>>>` = 被合并分支；Rebase：`HEAD` = 基准分支（main），`>>>` = 你的补丁。

### 7.4 冲突解决高级手段

```bash
# 选择一方完全接受
git checkout --ours OrderService.java     # 全部用当前分支的
git checkout --theirs OrderService.java   # 全部用对方分支的
git checkout --ours -- .                  # 全部文件

# 可视化合并工具
git mergetool
git config --global merge.tool "code --wait"
git config --global mergetool.prompt false

# 冲突文件按大小排序（从大到小解决）
git diff --name-only --diff-filter=U | xargs wc -l | sort -n
```

> 💡 IntelliJ IDEA 三栏可视化：VCS → Git → Resolve Conflicts → 左(ours)/中(base)/右(theirs) → 点击合并（快捷键见 [12](12-Git工具链与IDE集成.md)）。

### 7.5 策略性放弃与换策略

```bash
git merge --abort          # 冲突太多，回到合并前的清爽状态
git rebase main            # 换策略：每个 commit 单独解决冲突，更可控
git checkout --theirs -- . # 放弃自己的改动，完全接受对方的版本
```

## 8. 高级合并选项与 rerere

### 8.1 合并策略选项

```bash
git merge -s ort feature           # 默认策略（Git 2.34+）
git merge -s recursive feature     # 老默认策略

# 策略选项
git merge -X theirs feature        # 冲突时自动选对方
git merge -X ours feature          # 冲突时自动选我方
git merge -X ignore-space-change   # 忽略空白差异
```

### 8.2 Rerere：一劳永逸解决重复冲突

```bash
git config --global rerere.enabled true

# 工作流程：
# 1. 第一次遇到冲突 → 手动解决 → rerere 自动记录解决方案
# 2. 相同冲突再次出现 → rerere 自动应用之前的解决方案！
```

> 💡 对频繁 rebase 的团队是神器——相同的冲突只需要解决一次。

## 9. PR/MR 合并选项

| 选项 | 命令 | 历史效果 | 适用 |
|------|------|----------|------|
| **Merge Commit** | `git merge --no-ff` | 保留分支拓扑 + 合并节点 | ✅ 推荐，历史最清晰 |
| **Squash and Merge** | `git merge --squash` | 所有 commit 压为 1 个 | 小功能、清理杂乱提交 |
| **Rebase and Merge** | `git rebase` + `git merge --ff` | 线性历史 | 追求线性历史 |

```text
GitHub/GitLab PR 合并按钮对应：
  "Create a merge commit"  → Merge Commit（保留完整历史）
  "Squash and merge"       → Squash（压成 1 个 commit）
  "Rebase and merge"       → Rebase（线性历史）

推荐：默认用 Merge Commit（分支历史可追溯，SHA-1 不变）
例外：feature 分支 commit 杂乱时用 Squash
```

## 10. 合并策略速查

```text
┌──────────────────────────────────────────────────┐
│          Git 合并策略速查                         │
├──────────────┬───────────────────────────────────┤
│ git merge    │ 合并分支，保留真实历史             │
│ git rebase   │ 变基，让历史变线性和整洁           │
│ git rebase -i│ 交互式整理 commit（神器）          │
│ git merge    │                                    │
│   --squash   │ 压缩为一个 commit 再合并           │
│ git merge    │                                    │
│   --abort    │ 放弃合并，回到合并前状态           │
│ git rebase   │                                    │
│   --continue │ 解决冲突后继续 rebase              │
│ git rebase   │                                    │
│   --abort    │ 放弃 rebase，回到 rebase 前状态    │
├──────────────┴───────────────────────────────────┤
│ 黄金法则：不要 rebase 已 push 的公共分支          │
│ 推荐策略：本地 rebase 整理 + merge --no-ff 合并   │
└──────────────────────────────────────────────────┘
```

## 11. 核心要点

> 🎯 **核心要点**：
> - 三种合并：快进（线性）、三方（分叉汇聚）、--no-ff（强制留痕，企业推荐）；
> - Rebase = 重放提交，产生新 hash；黄金法则：公共分支永不 rebase；
> - 交互式 rebase 六指令：pick/reword/edit/squash/fixup/drop；
> - 三方合并判定：两同取一、三异冲突；冲突解决 = 意图判断而非机械操作；
> - cherry-pick 是新 commit——跨分支同步修复的唯一正确姿势；
> - rerere 记住冲突解决方案，频繁 rebase 团队必备。

## 12. 参考来源

- [Pro Git Book：合并与变基](https://git-scm.com/book/zh/v2/Git-分支-变基)
- [Git 官方文档：git-merge / git-rebase / git-cherry-pick](https://git-scm.com/docs)
- [Atlassian：Merging vs. Rebasing](https://www.atlassian.com/git/tutorials/merging-vs-rebasing)

---

**下一模块**：[05-Git撤销与历史重写](05-Git撤销与历史重写.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
