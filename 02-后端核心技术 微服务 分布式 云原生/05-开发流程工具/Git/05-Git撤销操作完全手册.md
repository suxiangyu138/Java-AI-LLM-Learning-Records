# Git 撤销操作完全手册

## 核心原则

撤销操作的第一原则：**先搞清楚你在哪里，要去哪里**。

```text
工作区 (Working)  ←→  暂存区 (Staging/Index)  ←→  本地仓库 (Repository)
     ↓ git add              ↓ git commit              ↓ git push
   文件修改               准备提交的快照              永久记录
```

---

## 1. 撤销命令全景图

```text
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

---

## 2. reset vs restore vs revert vs checkout

先搞清楚这几个容易混淆的命令：

| 命令 | 作用范围 | 安全性 | Git 版本 |
|------|---------|--------|---------|
| `git restore` | 工作区 / 暂存区 | 破坏工作区修改 | 2.23+ |
| `git reset` | 暂存区 / commit | 可能丢失commit | 所有版本 |
| `git revert` | 公开的 commit | **最安全**，新增反向commit | 所有版本 |
| `git checkout` | 工作区 / 分支 | 旧命令，推荐用 restore/switch | 所有版本 |

> Git 2.23+ 引入了 `restore` 和 `switch` 来分担 `checkout` 的职责，语义更清晰。

---

## 3. 场景驱动的撤销指南

### 场景 1：修改了文件，还没 git add → 撤销工作区修改

```bash
# 撤销单个文件
git restore file.txt

# 撤销所有文件
git restore .

# 旧版命令（也能用）
git checkout -- file.txt
```

### 场景 2：git add 了，还没 commit → 撤销暂存

```bash
# 从暂存区移除，但保留工作区修改
git restore --staged file.txt

# 旧版命令
git reset HEAD file.txt

# 从暂存区移除，并且丢弃工作区修改
git restore --staged --worktree file.txt   # 等于撤销 add + 撤销修改
```

### 场景 3：commit 了，但有问题 → 重新提交

```bash
# 修改最后一条 commit message
git commit --amend -m "new message"

# 修改最后一条 commit 的内容（补上忘加的文件）
git add forgotten-file.txt
git commit --amend --no-edit    # 不改message，只追加内容

# ⚠️ 如果已经push，不要amend！否则需要 force push
```

### 场景 4：commit 了，想撤销（保留修改）→ reset --soft

```bash
# 撤销最近的1个commit，修改回到暂存区
git reset --soft HEAD~1

# 此时 git status 会看到刚刚commit的文件在暂存区
# 修改都在，只是"撤回"了commit动作
```

### 场景 5：commit 了，想全部撤销 → reset --mixed（默认）

```bash
# 撤销最近1个commit + 清空暂存区，修改回到工作区
git reset HEAD~1
# 等价于 git reset --mixed HEAD~1

# 此时 git status 看到文件是红色（unstaged）
```

### 场景 6：commit 了，想彻底删除 → reset --hard（危险）

```bash
# ⚠️ 彻底删除最近1个commit的所有修改
git reset --hard HEAD~1

# 这之后修改找不到了...吗？
# 别慌，还有 reflog 可以救命（见第5节）
```

### 场景 7：commit 已经 push 了 → revert（安全）

```bash
# revert 创建一个新的反向commit，不删除历史
git revert a1b2c3d

# revert 一个 merge commit 需要指定 parent
# -m 1 表示保留 main 分支的内容
git revert -m 1 <merge-commit-hash>

# revert 也可以撤销多个commit
git revert HEAD~3..HEAD
```

### 场景 8：删除了文件，想恢复

```bash
# 从最新commit中恢复被删的文件
git restore file.txt

# 从指定commit中恢复文件
git restore --source=a1b2c3d file.txt

# 查看某个commit中有什么文件
git ls-tree --name-only a1b2c3d
```

---

## 4. reset 三种模式深度对比

```bash
# 准备实验环境
git init reset-demo && cd reset-demo
echo "line 1" > file.txt && git add . && git commit -m "commit 1"
echo "line 2" >> file.txt && git add . && git commit -m "commit 2"
echo "line 3" >> file.txt && git add . && git commit -m "commit 3"
```

| 模式 | HEAD | 暂存区 | 工作区 | 命令 |
|------|------|--------|--------|------|
| `--soft` | 回退 ✅ | **保留** ✅ | **保留** ✅ | `git reset --soft HEAD~1` |
| `--mixed` | 回退 ✅ | **清空** ❌ | **保留** ✅ | `git reset --mixed HEAD~1` |
| `--hard` | 回退 ✅ | **清空** ❌ | **清空** ❌ | `git reset --hard HEAD~1` |

### 记忆口诀

```text
--soft : 只动 HEAD（最温柔）
--mixed: 动 HEAD + 暂存区（默认的）
--hard : 全动（最危险）
```

### 指定具体 commit

```bash
# 用相对引用
git reset --soft HEAD~3    # 回退3个commit

# 用绝对哈希
git reset --soft a1b2c3d

# 回到某个分支的状态
git reset --hard origin/main
```

---

## 5. Reflog：救命的后悔药

Git 的**终极后悔药**。每当你移动 HEAD，Git 都在 reflog 中记了一笔。

```bash
# 查看 reflog
git reflog
# a1b2c3d HEAD@{0}: commit: feat: add search
# e4f5g6h HEAD@{1}: reset: moving to HEAD~1    ← 这个reset的记录！
# i7j8k9l HEAD@{2}: commit: feat: wrong commit
# ...

# 回到被 reset 干掉的状态
git reset --hard HEAD@{1}

# 回到被 rebase 干掉的状态
git reset --hard HEAD@{5}

# 查看某个引用的 reflog
git reflog show feature/login
```

### Reflog vs git log

| | reflog | git log |
|------|--------|---------|
| 记录的 | **HEAD 移动历史** | commit 历史 |
| 能看到 | 被 reset/rebase 干掉的 commit | 当前可达的 commit |
| 过期 | 默认 90 天清除 | 永不过期（只要可达） |
| 作用域 | 本地 | 本地 + 远程 |

### 经典救命场景

```bash
# 场景：不小心 git reset --hard 干掉了重要commit
git reflog
# 找到 reset 之前的那一行，记住 HEAD@{N}
git reset --hard HEAD@{N}
# 回来了！

# 场景：rebase 搞砸了
git reflog
# 找到 rebase 之前的状态
git reset --hard HEAD@{N}
```

---

## 6. 撤销远程操作

```bash
# revert 是最安全的方式（推荐）
git revert a1b2c3d
git push origin main

# force push 是最后手段（需要和团队沟通！）
git reset --hard HEAD~1
git push --force-with-lease origin main
# --force-with-lease 比 --force 安全：
#   如果远程有你不知道的新commit → 拒绝推送，防止覆盖同事的代码
```

---

## 7. stash 临时保存

虽然不是严格的"撤销"，但 stashing 也是"暂时撤销修改"的一种方式：

```bash
# 保存当前修改 → 工作区变干净
git stash push -m "WIP: half-done search feature"

# 查看stash列表
git stash list
# stash@{0}: On main: WIP: half-done search feature

# 恢复最近一次stash（保留stash记录）
git stash apply

# 恢复最近一次stash（删除stash记录）
git stash pop

# 恢复指定的stash
git stash pop stash@{1}

# 丢弃某个stash
git stash drop stash@{0}

# 清空所有stash
git stash clear

# 创建一个分支来应用stash
git stash branch new-feature stash@{0}
```

---

## 8. 撤销操作速查表

| 想做什么 | 命令 |
|---------|------|
| 撤销工作区修改（未add） | `git restore <file>` |
| 撤销 git add（保留修改） | `git restore --staged <file>` |
| 修改最后一次commit message | `git commit --amend -m "..."` |
| 追加文件到最后一次commit | `git add <file> && git commit --amend --no-edit` |
| 撤销commit（保留修改） | `git reset --soft HEAD~1` |
| 撤销commit+暂存（保留修改） | `git reset HEAD~1` |
| 撤销commit+暂存+修改（彻底） | `git reset --hard HEAD~1` |
| 撤销已push的commit（安全） | `git revert <commit>` |
| 找回被reset干掉的commit | `git reflog` + `git reset --hard HEAD@{N}` |
| 放弃merge | `git merge --abort` |
| 放弃rebase | `git rebase --abort` |
| 临时保存修改 | `git stash` |
| 恢复临时保存 | `git stash pop` |

---

## 9. 安全守则

```text
1. 不确定时用 revert，不要 reset --hard
2. reset --hard 前先 git stash 或 git branch backup
3. push --force 前确认有没有同事基于你的分支工作
4. 善用 reflog，它是最后的保障
5. amend 只用于还没 push 的 commit
```

---

> 上一篇：[04-Git合并与变基完全指南](04-Git合并与变基完全指南.md)
> 下一篇：[06-Git远程协作与平台实战](06-Git远程协作与平台实战.md)
