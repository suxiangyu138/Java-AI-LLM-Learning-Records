# Git 合并与变基完全指南

## 核心问题：merge 还是 rebase？

这是 Git 最经典的争论之一。本质上是在"**保留真实历史**"和"**整洁线性历史**"之间做选择。

---

## 1. 合并（Merge）的三种策略

### 1.1 Fast-Forward Merge（快进合并）

当目标分支没有新提交时，Git 直接移动指针：

```bash
# 场景：main 在 feat 创建后没有新提交
git checkout main
git merge feature/login
# 结果：HEAD 直接指向 feature/login 的最新 commit
# 历史线性，不创建 merge commit
```

```text
Fast-Forward 合并（main 无新提交，feature 直接快进）

main:    A ── B ── C (feature 快进，无合并提交)
feature:      └ C ┘
```

### 1.2 Three-Way Merge（三方合并）

两个分支都有新提交时，创建 merge commit：

```bash
git checkout main
git merge feature/login
# 产生一个 merge commit，有两个 parent
```

```text
非快进合并（main 与 feature 分叉，产生合并提交 M）

main:    A ──────── B ── M (merge commit, 两个 parent: B 和 C)
feature:    └ C ──┘
```

### 1.3 --no-ff Merge（强制非快进）

即使可以快进，也强制创建 merge commit：

```bash
git merge --no-ff feature/login
# 保留"这个feature分支存在过"的信息
```

| 策略 | 命令 | 历史形状 | 适用 |
|------|------|---------|------|
| Fast-Forward | `git merge <branch>` | 线性 | 个人开发 |
| Three-Way | `git merge <branch>` | 分叉后再汇聚 | 分支并行开发 |
| No Fast-Forward | `git merge --no-ff <branch>` | 保留分叉记录 | 需要追溯feature边界 |

### 1.4 Squash Merge（压缩合并）

把 feature 分支的所有 commit 压成一个：

```bash
git merge --squash feature/login
git commit -m "feat: complete login module"
# 所有feature的commits变成main上的一个commit
```

| Squash 优点 | Squash 缺点 |
|------------|------------|
| main 分支历史清爽 | 丢失中间commit的细节 |
| 一个功能一个commit | 无法追溯"这个bug是哪个commit引入的" |

---

## 2. 变基（Rebase）深度解析

### 2.1 Rebase 做了什么？

```bash
# 场景：你在 feature 分支开发，main 往前走了
git checkout feature
git rebase main
```

Rebase 的原理（三步）：
1. 找到 feature 和 main 的共同祖先
2. 把 feature 上独有的 commit **逐个 cherry-pick** 到 main 的最新位置
3. 把 feature 指针移到新位置

```text
Rebase 前:                          Rebase 后:
main:    A ── B                     main:    A ── B ── C' (feature 重放)
feature: A ── C (与 B 分叉)         feature:      └ C' ┘ (历史线性化)
```

**注意**：`C'` 是全新的 commit，hash 不同了！这就是 "改写历史" 的含义。

### 2.2 交互式 Rebase（Interactive Rebase）

Git 最强大的提交整理工具：

```bash
# 整理最近3个commit
git rebase -i HEAD~3
```

进入编辑器后：

```text
pick a1b2c3d feat: add login form
pick e4f5g6h fix: typo in login form      ← 这个应该和上一个合并
pick i7j8k9l wip: save progress            ← 这个是中间状态，不想要

# 修改为 ↓
pick a1b2c3d feat: add login form
fixup e4f5g6h fix: typo in login form     # 合并到上一个，丢弃commit message
drop  i7j8k9l wip: save progress          # 直接删除
```

### Rebase 指令速查表

| 指令 | 效果 |
|------|------|
| `pick` | 保留这个 commit（默认） |
| `reword` | 保留但修改 commit message |
| `edit` | 停下来，允许修改这个 commit 的内容 |
| `squash` | 合并到上一个 commit，保留 commit message |
| `fixup` | 合并到上一个 commit，丢弃 commit message |
| `drop` | 删除这个 commit |
| `break` | 在这里暂停（方便中途运行命令） |

### 实战：用 Rebase 整理提交历史

```bash
# 1. 查看要整理的范围
git log --oneline main..HEAD
# e4f5g6h fix: typo
# a1b2c3d wip
# i7j8k9l feat: draft login
# k1l2m3n feat: add auth utils

# 2. 启动交互式 rebase
git rebase -i main

# 3. 编辑窗口操作
pick k1l2m3n feat: add auth utils
squash i7j8k9l feat: draft login     # 挤上去
fixup a1b2c3d wip                    # 压上去且不要message
fixup e4f5g6h fix: typo              # 同上

# 4. 保存退出后，最终 main 上只新增一个干净commit
```

---

## 3. Rebase 黄金法则

> ⚠️ **永远不要 rebase 已经推送到公共仓库的 commit**

```
为什么？
1. Rebase 会产生全新的 commit hash
2. 如果别人基于你原来的 commit 开发了，他们会被坑
3. 强行 push (--force) 会覆盖别人的工作

简单判断：
  这个commit只有你在用？ → 可以rebase
  有人可能在你的commit上开发？ → 绝对不要rebase
```

```bash
# ✅ 安全：rebase 从未 push 过的本地分支
git checkout feature/draft
git rebase main

# ❌ 危险：rebase 已 push 的分支
git checkout feature/shared
git rebase main
git push --force  # 同事的本地分支会乱掉！

# ✅ 安全替代：merge
git checkout feature/shared
git merge main    # 不重写历史，多一个merge commit而已
```

---

## 4. 冲突解决实战

### 4.1 冲突是怎么发生的

```
main 分支：       file.txt 第3行 = "const PORT = 3000;"
feature 分支：    file.txt 第3行 = "const PORT = 8080;"
```

两个分支改了同一行 → Git 无法自动决定用哪个 → **冲突**

### 4.2 解决冲突的标准流程

```bash
# 1. 合并或rebase时遇到冲突
git merge feature/login
# CONFLICT (content): Merge conflict in src/config.js
# Automatic merge failed; fix conflicts and then commit the result.

# 2. 查看冲突文件
git status
# both modified: src/config.js

# 3. 打开冲突文件
# <<<<<<< HEAD
# const PORT = 3000;
# =======
# const PORT = 8080;
# >>>>>>> feature/login

# 4. 手动编辑，选择最终版本
# const PORT = 8080;  ← 选择并删除冲突标记

# 5. 标记为已解决
git add src/config.js

# 6. 继续
git merge --continue     # merge 场景
git rebase --continue    # rebase 场景

# 如果要放弃
git merge --abort        # 回到merge前
git rebase --abort       # 回到rebase前
```

### 4.3 冲突解决工具

```bash
# 配置可视化 merge tool（推荐 VS Code）
git mergetool

# 或者直接用 VS Code 的三栏合并编辑器
# 右键冲突文件 → "在合并编辑器中解决"
```

### 4.4 冲突标记含义

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
   const PORT = 8080;  ← 这是你的feature commit内容
>>>>>>> feat: add config ← 你的commit message
```

**记忆技巧**：
- Merge：`HEAD` = 你当前分支，`>>>` = 被合并的分支
- Rebase：`HEAD` = 基准分支（main），`>>>` = 你的补丁

---

## 5. Rerere：一劳永逸解决重复冲突

```bash
# 启用 rerere (Reuse Recorded Resolution)
git config --global rerere.enabled true

# 工作流程：
# 1. 第一次遇到冲突 → 手动解决 → rerere自动记录解决方案
# 2. 相同冲突再次出现 → rerere自动应用之前的解决方案！
```

这对频繁 rebase 的团队是神器——相同的冲突只需要解决一次。

---

## 6. Merge vs Rebase：终极对比

| 维度 | Merge | Rebase |
|------|-------|--------|
| **历史记录** | 真实，保留所有分叉 | 线性整洁，像是顺序开发 |
| **冲突解决** | 一次性解决所有 | 每个commit解决一次（可能多次） |
| **安全性** | 安全，不改变已有commit | 改写历史，有合作风险 |
| **追溯性** | 容易看到"这个feature什么时候合并的" | 难以辨认功能边界 |
| **回滚** | 简单 `git revert -m 1 <merge-commit>` | 需要逐个revert |
| **适用** | 公共分支、发布分支 | 私有分支、整理提交 |

### 推荐策略

```text
本地整理用 rebase，公开发布用 merge：

1. git checkout feature/xxx
2. git rebase main              ← 本地把commit整理干净
3. git checkout main
4. git merge --no-ff feature/xxx ← 合并时保留feature痕迹
```

---

## 7. Rebase 实战场景

### 场景1：你的 feature 落后 main 了

```bash
git checkout feature/login
git rebase main
# 解决冲突（如果有）→ git add → git rebase --continue
# 成功：feature 分支已经基于最新的 main
```

### 场景2：把多个"修typotypo"的commit整理干净

```bash
git rebase -i HEAD~5
# 把 fix typo、wip 之类的 commit fixup 到对应的 feat commit
```

### 场景3：Rebase 后需要 force push

```bash
git push --force-with-lease origin feature/login
# --force-with-lease 比 --force 安全：如果远程有你不知道的新commit，会拒绝推送
```

---

## 8. 核心总结

```text
┌──────────────────────────────────────────────────┐
│          Git 合并策略速查                         │
├──────────────┬───────────────────────────────────┤
│ git merge    │ 合并分支，保留真实历史             │
│ git rebase   │ 变基，让历史变线性和整洁           │
│ git rebase -i│ 交互式整理commit（神器）           │
│ git merge    │                                    │
│   --squash   │ 压缩为一个commit再合并             │
│ git merge    │                                    │
│   --abort    │ 放弃合并，回到合并前状态           │
│ git rebase   │                                    │
│   --continue │ 解决冲突后继续rebase               │
│ git rebase   │                                    │
│   --abort    │ 放弃rebase，回到rebase前状态       │
├──────────────┴───────────────────────────────────┤
│ 黄金法则：不要rebase已push的公共分支             │
│ 推荐策略：本地rebase整理 + merge --no-ff合并     │
└──────────────────────────────────────────────────┘
```

---

> 上一篇：[03-Git分支模型与团队工作流](03-Git分支模型与团队工作流.md)
> 下一篇：[05-Git撤销操作完全手册](05-Git撤销操作完全手册.md)
