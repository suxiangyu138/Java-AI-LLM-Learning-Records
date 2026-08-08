# 05-Git 撤销与历史重写
> 撤销的第一原则：**先搞清楚你在哪里，要去哪里**——reset 三态、revert、reflog 后悔药、原子化提交与历史重写安全边界

## 📚 目录
1. [撤销命令全景图](#1-撤销命令全景图)
2. [reset vs restore vs revert vs checkout](#2-reset-vs-restore-vs-revert-vs-checkout)
3. [场景驱动的撤销指南（8 场景）](#3-场景驱动的撤销指南8-场景)
4. [reset 三种模式深度对比](#4-reset-三种模式深度对比)
5. [Reflog：救命的后悔药](#5-reflog救命的后悔药)
6. [git revert 详解](#6-git-revert-详解)
7. [撤销合并与撤销远程操作](#7-撤销合并与撤销远程操作)
8. [stash：临时保存修改](#8-stash临时保存修改)
9. [原子化提交与历史重写](#9-原子化提交与历史重写)
10. [撤销操作速查表与安全守则](#10-撤销操作速查表与安全守则)
11. [核心要点](#11-核心要点)
12. [参考来源](#12-参考来源)

## 1. 撤销命令全景图

```text
工作区 (Working)  ←→  暂存区 (Staging/Index)  ←→  本地仓库 (Repository)
     ↓ git add              ↓ git commit              ↓ git push
   文件修改               准备提交的快照              永久记录

撤销命令全景（四层区域 → 对应命令）
工作区      git restore <file>          撤销工作区修改
   │
暂存区      git restore --staged <file> 从暂存区移除
   │
本地仓库    git reset --soft HEAD~1     撤销 commit，保留暂存+工作区
            git reset --mixed HEAD~1    撤销 commit+暂存，保留工作区
            git reset --hard HEAD~1     全部撤销（危险!）
            git revert <commit>         安全反向提交（推荐远程）
   │
远程仓库    git push --force-with-lease 覆盖远程（最后手段）
```

## 2. reset vs restore vs revert vs checkout

| 命令 | 作用范围 | 安全性 | Git 版本 |
|------|---------|--------|---------|
| `git restore` | 工作区 / 暂存区 | 破坏工作区修改 | 2.23+ |
| `git reset` | 暂存区 / commit | 可能丢失 commit | 所有版本 |
| `git revert` | 公开的 commit | **最安全**，新增反向 commit | 所有版本 |
| `git checkout` | 工作区 / 分支 | 旧命令，推荐用 restore/switch | 所有版本 |

> 💡 Git 2.23+ 引入了 `restore`（管文件恢复）和 `switch`（管分支切换）来分担 `checkout` 的职责，语义更清晰。

## 3. 场景驱动的撤销指南（8 场景）

**场景 1：修改了文件，还没 git add → 撤销工作区修改**

```bash
git restore file.txt         # 撤销单个文件
git restore .                # 撤销所有文件
git checkout -- file.txt     # 旧版命令（也能用）
```

**场景 2：git add 了，还没 commit → 撤销暂存**

```bash
git restore --staged file.txt                    # 从暂存区移除，保留工作区修改
git reset HEAD file.txt                          # 旧版命令
git restore --staged --worktree file.txt         # 撤销 add + 撤销修改（一步到位）
```

**场景 3：commit 了，但有问题 → 重新提交（amend）**

```bash
git commit --amend -m "new message"              # 修改最后一条 commit message
git add forgotten-file.txt
git commit --amend --no-edit                     # 追加文件，不改 message
# ⚠️ 如果已经 push，不要 amend！否则需要 force push
```

**场景 4：commit 了，想撤销（保留修改）→ reset --soft**

```bash
git reset --soft HEAD~1        # 撤销最近 1 个 commit，修改回到暂存区
```

**场景 5：commit 了，想全部撤销 → reset --mixed（默认）**

```bash
git reset HEAD~1               # 等价于 git reset --mixed HEAD~1
# 撤销 commit + 清空暂存区，修改回到工作区
```

**场景 6：commit 了，想彻底删除 → reset --hard（危险）**

```bash
git reset --hard HEAD~1        # ⚠️ 彻底删除最近 1 个 commit 的所有修改
# 别慌，还有 reflog 可以救命（见第 5 节）
```

**场景 7：commit 已经 push 了 → revert（安全）**

```bash
git revert a1b2c3d                 # 创建一个新的反向 commit，不删除历史
git revert -m 1 <merge-commit-hash>   # revert merge commit（-m 指定保留哪一边）
git revert HEAD~3..HEAD            # 撤销多个 commit
git revert --no-commit HEAD~3..HEAD  # 不自动提交（便于合并解决冲突）
```

**场景 8：删除了文件，想恢复**

```bash
git restore file.txt                    # 从最新 commit 恢复被删文件
git restore --source=a1b2c3d file.txt   # 从指定 commit 恢复
git ls-tree --name-only a1b2c3d         # 查看某个 commit 中有什么文件
```

## 4. reset 三种模式深度对比

### 4.1 实验环境

```bash
git init reset-demo && cd reset-demo
echo "line 1" > file.txt && git add . && git commit -m "commit 1"
echo "line 2" >> file.txt && git add . && git commit -m "commit 2"
echo "line 3" >> file.txt && git add . && git commit -m "commit 3"
```

### 4.2 三态对比

| 模式 | HEAD | 暂存区 | 工作区 | 命令 |
|------|------|--------|--------|------|
| `--soft` | 回退 ✅ | **保留** ✅ | **保留** ✅ | `git reset --soft HEAD~1` |
| `--mixed` | 回退 ✅ | **清空** ❌ | **保留** ✅ | `git reset --mixed HEAD~1` |
| `--hard` | 回退 ✅ | **清空** ❌ | **清空** ❌ | `git reset --hard HEAD~1` |

```text
记忆口诀：
--soft : 只动 HEAD（最温柔）
--mixed: 动 HEAD + 暂存区（默认的）
--hard : 全动（最危险）
```

```bash
git reset --soft HEAD~3       # 用相对引用，回退 3 个 commit
git reset --soft a1b2c3d      # 用绝对哈希
git reset --hard origin/main  # 回到某个分支的状态
```

## 5. Reflog：救命的后悔药

Git 的**终极后悔药**。每当你移动 HEAD，Git 都在 reflog 中记了一笔。

```bash
git reflog
# a1b2c3d HEAD@{0}: commit: feat: add search
# e4f5g6h HEAD@{1}: reset: moving to HEAD~1    ← 这个 reset 的记录！
# i7j8k9l HEAD@{2}: commit: feat: wrong commit

git reset --hard HEAD@{1}         # 回到被 reset 干掉的状态
git reset --hard HEAD@{5}         # 回到被 rebase 干掉的状态
git reflog show feature/login     # 查看某个引用的 reflog
git reflog --date=iso             # 带时间戳
```

### Reflog vs git log

| | reflog | git log |
|------|--------|---------|
| 记录的 | **HEAD 移动历史** | commit 历史 |
| 能看到 | 被 reset/rebase 干掉的 commit | 当前可达的 commit |
| 过期 | 默认 90 天清除 | 永不过期（只要可达） |
| 作用域 | 本地 | 本地 + 远程 |

```bash
# 经典救命场景
git reflog                       # 找到 reset 之前的那一行
git reset --hard HEAD@{N}        # 回来了！
```

## 6. git revert 详解

### 6.1 revert vs reset

| 操作 | 原理 | 历史 | 远程安全 | 适用 |
|------|------|:---:|:---:|------|
| `git revert` | 创建反向 commit 抵消目标 commit | ✅ 保留 | ✅ 安全 | **生产回滚唯一选择** |
| `git reset` | 移动分支指针到历史 commit | ❌ 改写 | ❌ 禁止 | 仅本地未 push 的 commit |

### 6.2 命令全集

```bash
git revert a1b2c3d                 # 回滚单个 commit
git revert a1b2c3d..e4f5g6h        # 回滚多个 commit（左开右闭）
git revert -m 1 <merge-commit>     # 回滚 merge commit（-m 指定保留哪一边）
git revert <revert-commit>         # revert 的 revert：恢复被回滚的代码
```

> 💡 `git revert` 是生产环境唯一推荐的回滚方式。它不删除历史，只是新增一个反向操作，CI/CD 历史清晰可审计。

## 7. 撤销合并与撤销远程操作

### 7.1 撤销未推送的合并

```bash
git reset --hard HEAD~1        # 回退到 merge 前
git reset --hard ORIG_HEAD     # ORIG_HEAD 记录危险操作前的状态
```

### 7.2 撤销已推送的合并（安全方式）

```bash
# merge commit 已 push → 必须用 revert，不能用 reset！
git revert -m 1 <merge-commit-hash>
# -m 1: 保留 main 分支的内容
```

> ⚠️ 已 push 的 merge commit 绝对不能用 `git reset` 撤销——那会改写公共历史，导致其他人的仓库与你不同步。

### 7.3 撤销远程操作

```bash
# revert 是最安全的方式（推荐）
git revert a1b2c3d
git push origin main

# force push 是最后手段（需要和团队沟通！）
git reset --hard HEAD~1
git push --force-with-lease origin main
# --force-with-lease 比 --force 安全：
#   如果远程有你不知道的新 commit → 拒绝推送，防止覆盖同事的代码
```

## 8. stash：临时保存修改

不是严格的"撤销"，但 stashing 也是"暂时撤销修改"的一种方式（进阶用法见 [07](07-Git日常管理与工作区实践.md)）：

```bash
git stash push -m "WIP: half-done search feature"   # 保存并带描述
git stash list                                       # 查看列表（栈顺序）
git stash pop                        # 恢复最近一次并移除
git stash pop stash@{1}              # 恢复指定的 stash
git stash apply                      # 恢复但不移除
git stash drop stash@{0}             # 删除指定 stash
git stash clear                      # 清空所有 stash
git stash branch new-feature stash@{0}   # 从 stash 创建分支（自动 pop）
```

> ⚠️ **Stash 风险**：Stash 存储在本地 `.git/refs/stash`，**不随 push 推送**。重装系统或删除仓库将永久丢失。重要 WIP 建议推到分支上存储。

## 9. 原子化提交与历史重写

### 9.1 原子化提交

**原则**：每次提交只包含一个完整的逻辑变更；一个原子 commit = 一个独立的、可回滚的、有意义的变更。

| 变更类型 | 是否独立 commit | 理由 |
|----------|:---:|------|
| 新功能实现 | ✅ | 一个功能 = 一个 commit |
| 重构 | ✅ | 独立于功能变更 |
| Bug 修复 | ✅ | 可独立 cherry-pick 到 hotfix |
| 代码格式化 | ✅ | 独立于逻辑变更 |
| 依赖升级 | ✅ | 独立于功能变更 |
| 修 typo | ⚠️ 可合并到上一个 | `--amend` |
| "正在开发中" | ❌ | 开发完成后 squash |

```text
✅ 好的历史：
  a1b2c3d feat(user): 添加用户登录接口
  d4e5f6g feat(user): 实现JWT Token签发
  g7h8i9j feat(user): 添加登录单元测试
  k3l4m5n fix(user): 修复Token过期判断逻辑

❌ 坏的历史：
  a1b2c3d update
  d4e5f6g fix bug
  l6m7n8o tmp
  m8n9o0p finally working
```

> 🎯 **Commit Message 黄金法则**：假设半年后的你（或同事）在看 `git blame`，TA 能凭 commit message 理解"为什么有这个变更"。如果 message 里写了 "and"，说明这个提交应该拆分成两个。

### 9.2 软回退拆提交

```bash
# 场景：改了 3 个独立功能，但只有一个 commit
git reset --soft HEAD~1                 # 1. 软回退（保留所有修改在暂存区）
git reset HEAD .                        # 2. 取消全部暂存
git add src/.../UserController.java
git commit -m "feat(user): 添加用户查询接口"
git add src/.../UserService.java
git commit -m "refactor(user): 优化用户查询逻辑"
git add src/main/resources/application.yml
git commit -m "chore: 调整连接池配置"
```

### 9.3 历史重写安全边界

```text
✅ 可以重写的历史（绝对安全）：
  - 还未 push 的本地 commit
  - 个人 feature 分支（确认无人基于此工作）

❌ 绝对禁止重写的历史：
  - 已 push 到公共仓库的 commit（main/develop/release）
  - 别人可能基于你的 commit 继续开发的任何分支
```

| 操作 | 安全条件 | 命令 |
|------|----------|------|
| 修改最近 commit | 未 push | `git commit --amend` |
| 撤销最近 commit | 未 push | `git reset HEAD~1` |
| 交互式整理 commit | 未 push 的个人分支 | `git rebase -i HEAD~5` |
| 强制推送 | 仅个人分支 + `--force-with-lease` | `git push --force-with-lease` |

## 10. 撤销操作速查表与安全守则

| 想做什么 | 命令 |
|---------|------|
| 撤销工作区修改（未 add） | `git restore <file>` |
| 撤销 git add（保留修改） | `git restore --staged <file>` |
| 修改最后一次 commit message | `git commit --amend -m "..."` |
| 追加文件到最后一次 commit | `git add <file> && git commit --amend --no-edit` |
| 撤销 commit（保留修改） | `git reset --soft HEAD~1` |
| 撤销 commit+暂存（保留修改） | `git reset HEAD~1` |
| 撤销 commit+暂存+修改（彻底） | `git reset --hard HEAD~1` |
| 撤销已 push 的 commit（安全） | `git revert <commit>` |
| 找回被 reset 干掉的 commit | `git reflog` + `git reset --hard HEAD@{N}` |
| 放弃 merge | `git merge --abort` |
| 放弃 rebase | `git rebase --abort` |
| 临时保存修改 | `git stash` |
| 恢复临时保存 | `git stash pop` |

```text
安全守则：
1. 不确定时用 revert，不要 reset --hard
2. reset --hard 前先 git stash 或 git branch backup
3. push --force 前确认有没有同事基于你的分支工作
4. 善用 reflog，它是最后的保障
5. amend 只用于还没 push 的 commit
```

## 11. 核心要点

> 🎯 **核心要点**：
> - 撤销四命令分工：restore（文件）、reset（本地历史）、revert（远程安全）、stash（暂存）；
> - reset 口诀：soft 只动 HEAD、mixed 动 HEAD+暂存、hard 全动；
> - reflog 是 90 天后悔药：被 reset/rebase 干掉的东西都能找回；
> - 生产回滚唯一姿势：`git revert`（-m 1 处理 merge commit）；
> - 历史重写边界：未 push 随便改，公共分支永不改；
> - 原子化提交 + 清晰 message = 可追溯、可审计、可回滚的"黑匣子"。

## 12. 参考来源

- [Pro Git Book：重置揭秘（reset）](https://git-scm.com/book/zh/v2/Git-工具-重置揭秘)
- [Pro Git Book：reflog](https://git-scm.com/book/zh/v2/Git-工具-重写历史)
- [Git 官方文档：git-reset / git-revert / git-restore](https://git-scm.com/docs)

---

**下一模块**：[06-Git远程协作与托管平台](06-Git远程协作与托管平台.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
