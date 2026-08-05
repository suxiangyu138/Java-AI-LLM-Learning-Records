# 18-Git日常管理
> 一套完整覆盖开发者日常 Git 操作的实战手册，从晨间同步到夜间推送，从工作区卫生到仓库维护，构建规范高效的 Day-to-Day Git 工作流。

## 目录
1. [Developer's Daily Git Rhythm](#1-developers-daily-git-rhythm)
2. [Working Directory Hygiene](#2-working-directory-hygiene)
3. [Commit Management](#3-commit-management)
4. [Branch Daily Operations](#4-branch-daily-operations)
5. [Syncing with Remote](#5-syncing-with-remote)
6. [Stash as Daily Tool](#6-stash-as-daily-tool)
7. [Log Mastery](#7-log-mastery)
8. [Undo Operations for Daily Use](#8-undo-operations-for-daily-use)
9. [.git Directory Walkthrough](#9-git-directory-walkthrough)
10. [Repository Maintenance](#10-repository-maintenance)
11. [Working with Remotes](#11-working-with-remotes)
12. [Complete Daily Workflow Example](#12-complete-daily-workflow-example)

---

## 1. Developer's Daily Git Rhythm

高效 Git 用户的一天遵循固定的节奏，将版本控制内化为肌肉记忆而非心智负担。

### 1.1 The Daily Cadence

```
Morning (09:00)   → git pull → git status → review yesterday's work
Mid-morning       → git add -p → git commit (per micro-task)
Lunch (12:00)     → git push (save morning work to remote)
Afternoon (14:00) → git pull --rebase (sync with team) → continue working
Late afternoon    → git commit → git push
Evening (17:30)   → git push final → create PR / update PR
```

### 1.2 每日操作核查清单

| 时段 | 命令 | 目的 |
|------|------|------|
| 晨间 | `git pull --rebase` | 同步远程最新代码，确保基于最新起点开发 |
| 开发中 | `git status` | 频繁检查工作区状态，避免遗漏或误提交 |
| 提交前 | `git diff` / `git diff --staged` | 确认修改内容准确无遗漏 |
| 微任务完成 | `git commit` | 按功能点粒度提交，拒绝大杂烩提交 |
| 午间/下班 | `git push` | 将代码推送到远程，避免本地数据丢失 |
| PR 前 | `git log --oneline` | 确认提交历史清晰且无 WIP 提交 |
| 下班前 | `git stash list` | 检查是否有未完成的 stash 遗留 |

> 💡 **黄金法则**: 提交频率 = 解决一个最小独立问题的单位。如果你在 commit message 里写了 "and"，说明这个提交应该拆分成两个。

---

## 2. Working Directory Hygiene

工作区（Working Directory）是开发者与 Git 交互的第一现场，保持其整洁是日常管理的基础。

### 2.1 git status 深度解读

`git status` 是每日最高频的命令，但多数人只看了"Ignore"级信息。

```bash
# 典型输出解读
$ git status
On branch feature/user-login          # 当前分支
Your branch is ahead of 'origin/feature/user-login' by 2 commits.  # 领先远程2个提交

Changes not staged for commit:        # 已修改但未暂存
  modified: src/main/java/com/xxx/controller/UserController.java

Changes to be committed:              # 已暂存等待提交
  new file:   src/main/java/com/xxx/dto/LoginRequest.java
  modified:   src/main/java/com/xxx/service/UserService.java

Untracked files:                      # 未追踪文件（新文件或不受版本控制的文件）
  .idea/workspace.xml
```

**状态矩阵解读**:

| 文件状态 | git status 显示位置 | 含义 |
|----------|---------------------|------|
| Untracked | `Untracked files` | Git 尚未追踪，需 `git add` 注册 |
| Modified (unstaged) | `Changes not staged for commit` | 已追踪文件有改动，未暂存 |
| Staged | `Changes to be committed` | 已暂存，将进入下一次提交 |
| Unmodified | 不显示 | 文件与 HEAD 一致，无变化 |

> ⚠️ **关注 Untracked files**: 如果 `.idea/` 或 `target/` 出现在 Untracked 中，说明 `.gitignore` 配置有遗漏，立即修复。

### 2.2 git diff 全面解析

```bash
# 工作区 vs 暂存区（查看尚未暂存的修改）
git diff

# 暂存区 vs 最后一次提交（查看即将提交的内容）
git diff --staged        # 同 git diff --cached

# 工作区 vs 最后一次提交（所有未提交的修改总和）
git diff HEAD

# 仅查看文件名列表（含修改类型）
git diff --name-status
# M      UserController.java
# A      LoginRequest.java
# D      OldService.java

# 统计行变化数
git diff --stat

# 忽略空白差异（处理 IDE 自动格式化导致的噪音）
git diff --ignore-space-change
git diff --ignore-all-space
```

**日常使用场景**:

```bash
# 场景 1：提交前审查
git add UserController.java
git diff --staged UserController.java  # 确认要提交的内容

# 场景 2：对比两个分支的差异
git diff main..feature/login           # feature/login 相对 main 的变化

# 场景 3：检查某次提交改了什么
git diff HEAD~1 HEAD                   # 最近一次提交的变更

# 场景 4：找出某文件在两个版本间如何变化的
git diff v1.0.0 v1.1.0 -- pom.xml
```

> 💡 **技巧**: `git diff --word-diff` 以单词粒度对比代码，特别适合阅读长行变更或文档修改。

---

## 3. Commit Management

提交管理是 Git 日常操作的"最小管理单元"，掌握提交的精雕细琢能力，是区分新手与高手的标志。

### 3.1 修正最近一次提交

```bash
# 场景：提交后发现漏了一个文件、或者提交信息有错别字

# 修正提交信息（不修改内容）
git commit --amend -m "feat: 新增用户注册接口（含参数校验）"

# 追加文件到上一个提交（不修改提交信息）
git add forgot-file.java
git commit --amend --no-edit

# 同时修改内容和信息
git add additional-change.java
git commit --amend
```

> ⚠️ **--amend 使用红线**: 仅可对**尚未推送**的提交使用。如果已 `git push`，`--amend` 会重写历史，需 `--force-with-lease` 推送，**严禁在公共分支上执行**。

### 3.2 拆分提交

场景：一个提交里包含两个不相关的改动，需要拆开。

```bash
# 方法 1：reset --soft + 重新 add
git reset --soft HEAD~1        # 撤销提交，保留工作区和暂存区内容
git reset HEAD .               # 取消所有暂存
git add src/file1.java         # 只添加第一个文件的改动
git commit -m "feat: 实现功能A"
git add src/file2.java
git commit -m "feat: 实现功能B"

# 方法 2：交互式 rebase 拆分已有提交
git rebase -i HEAD~3           # 在要拆分的提交前标记为 "edit"
# Git 暂停在指定提交，然后:
git reset HEAD^                # 保留工作区，撤销该次提交
git add -p                     # 分块添加
git commit -m "part 1"
git add -p
git commit -m "part 2"
git rebase --continue
```

### 3.3 Interactive Add (`git add -p`)

将文件的修改按"块（hunk）"粒度选择性暂存，是最精细的提交控制手段。

```bash
git add -p src/main/java/com/xxx/service/UserService.java
```

交互界面中的选项:

| 选项 | 含义 | 使用场景 |
|------|------|----------|
| `y` | 暂存当前 hunk | 该块改动属于本次提交 |
| `n` | 不暂存当前 hunk | 该块改动属于下次提交或不应提交 |
| `s` | 将当前 hunk 拆分成更小的块 | hunk 太大，内含多个独立改动 |
| `e` | 手动编辑当前 hunk | 最精细的控制，只选取部分行 |
| `q` | 退出，不再暂存 | 后续块都不需要 |
| `?` | 显示帮助 | 遗忘命令时 |

```bash
# 示例：一个文件内有日志修改 + 业务逻辑修改
# 通过 git add -p 可将日志修改暂存到一个提交，业务逻辑到另一个提交

# 配合 git restore 使用
git add -p              # 确认暂存的内容
git diff --cached       # 复查即将提交的内容
git commit -m "feat: ..."
git checkout -- .       # 丢弃剩余的未暂存修改（谨慎使用）
```

> 💡 **最佳实践**: `git add -p` 天然强制开发者审查每一块改动，能捕获大量低级错误，远胜于无脑 `git add .`。

---

## 4. Branch Daily Operations

分支是 Git 日常操作的核心载体，掌握分支的创建、切换、重命名、删除全生命周期管理。

### 4.1 本地分支操作

```bash
# 创建分支（基于当前 HEAD）
git branch feature/user-login

# 创建并切换（等同于 git branch + git checkout）
git checkout -b feature/user-login

# 基于指定分支创建
git checkout -b feature/user-login develop

# 切换到已有分支
git checkout develop        # 传统方式
git switch develop          # Git 2.23+ 新语法

# 创建并切换（switch 语法）
git switch -c feature/payment

# 重命名分支（当前分支）
git branch -m new-name

# 重命名分支（指定分支）
git branch -m old-name new-name

# 删除本地分支（已合并）
git branch -d feature/old-feature

# 强制删除本地分支（未合并）
git branch -D feature/abandoned-feature

# 查看本地分支（含最后一次提交信息）
git branch -v

# 查看已合并到当前分支的分支（准备清理）
git branch --merged

# 查看未合并到当前分支的分支
git branch --no-merged
```

### 4.2 远程分支操作

```bash
# 查看远程分支
git branch -r

# 查看所有分支（本地 + 远程）
git branch -a

# 将本地分支推送到远程并建立追踪
git push -u origin feature/user-login

# 删除远程分支
git push origin --delete feature/old-feature

# 获取远程分支信息但不同步代码
git remote update origin --prune

# 清理本地 stale tracking branches（远程已删除的追踪分支）
git remote prune origin

# 或更激进的清理方法
git branch -r --merged | grep -v main | xargs -r git push origin --delete
```

### 4.3 分支命名规范速查表

| 分支类型 | 命名模式 | 示例 | 生命周期 |
|----------|----------|------|----------|
| 主分支 | `main` / `master` | `main` | 永久 |
| 开发分支 | `develop` / `dev` | `develop` | 永久 |
| 功能分支 | `feature/<JIRA-ID>-<desc>` | `feature/PROJ-123-user-login` | 功能完成后删除 |
| 修复分支 | `fix/<desc>` | `fix/npe-in-login` | 修复合并后删除 |
| 热修复 | `hotfix/<version>-<desc>` | `hotfix/1.2.1-payment-timeout` | 上线合并后删除 |
| 发布分支 | `release/<version>` | `release/v2.0.0` | 发布后删除 |
| 个人分支 | `personal/<name>/<desc>` | `personal/zhang3/experiment` | 个人使用，随用随删 |

---

## 5. Syncing with Remote

与远程仓库同步是每日最高频的协作操作，理解 fetch vs pull 以及 merge vs rebase 的区别直接影响工作流质量。

### 5.1 fetch vs pull

```bash
# git fetch：仅下载远程数据，不改变本地工作区
git fetch origin
git fetch origin develop       # 仅获取 develop 分支
git fetch --all                # 更新所有远程追踪分支
git fetch --prune              # 获取 + 删除已不存在的远程追踪分支

# git pull = git fetch + git merge（或 rebase）
git pull origin develop
git pull --rebase origin develop  # 推荐方式
```

| 操作 | 下载数据 | 修改工作区 | 修改 HEAD | 安全性 |
|------|----------|------------|-----------|--------|
| `git fetch` | 是 | 否 | 否 | 安全，只读 |
| `git pull` (merge) | 是 | 是 | 是 | 可能产生合并提交 |
| `git pull --rebase` | 是 | 是 | 是 | 可能需解决冲突 |

### 5.2 pull --rebase vs pull --merge

```bash
# Merge 方式（默认）：创建一个 merge commit
git config --global pull.rebase false
git pull origin develop
# 结果：A---B---C---本地提交---Merge Commit---远程提交
# 产生额外的 merge commit，历史非线形

# Rebase 方式：将本地提交"移动"到远程提交之上
git config --global pull.rebase true
git pull origin develop
# 结果：A---B---远程提交---本地提交（被rebase到顶部）
# 历史线形，无额外 merge commit
```

**选择指南**:

| 场景 | 建议 | 原因 |
|------|------|------|
| 个人开发分支 | `--rebase` | 保持历史线形，合并时更清晰 |
| 公共分支（多人推送） | `--merge` | 避免 rebase 远程存在的提交导致混乱 |
| 从 main/develop 拉取更新 | `--rebase` | 让你的 feature 分支看起来始终基于最新起点 |
| 共享 feature 分支 | `--rebase`（谨慎） | 确保团队知道你在 rebase，用 `--force-with-lease` |

### 5.3 处理 diverged branches（分叉分支）

当本地和远程都有独立提交时，分支出现分叉:

```bash
# 场景：你提交了 commit A，同事推送了 commit B
# 本地: main → ... → commit A
# 远程: main → ... → commit B

# 方案一：rebase
git fetch origin
git rebase origin/main
# 结果：main → ... → commit B → commit A（线形历史）

# 方案二：merge
git fetch origin
git merge origin/main
# 结果：main → ... → commit A → commit B → Merge Commit

# 方案三：全自动处理（推荐）
git pull --rebase
```

> 💡 **最佳实践**: 通过 `git config --global pull.rebase true` + `git config --global rebase.autoStash true` 将 `git pull` 自动设为 rebase 模式，并自动 stash 未提交的修改。

---

## 6. Stash as Daily Tool

Stash 是开发者的"临时搁物架"，用于在不提交的情况下切换上下文。

### 6.1 基础操作

```bash
# 保存未提交的修改（工作区 + 暂存区）
git stash

# 保存并包含信息描述
git stash push -m "WIP: 用户注册接口，还剩参数校验未完成"

# 保存未追踪的文件
git stash push -u                   # --include-untracked
git stash push -a                   # --all（包含 .gitignore 中忽略的文件）

# 查看 stash 列表（按栈顺序排列）
git stash list
# stash@{0}: On feature/login: WIP: 用户注册接口，还剩参数校验未完成
# stash@{1}: On feature/payment: 支付回调调试

# 应用最近的 stash 并移除
git stash pop

# 应用指定的 stash 但不移除
git stash apply stash@{1}

# 移除指定的 stash
git stash drop stash@{1}

# 查看 stash 中的改动（类似 git diff）
git stash show stash@{0}
git stash show -p stash@{0}         # 详细内容

# 从 stash 创建分支（解决 stash 冲突的最佳方式）
git stash branch feature/fix-stash-conflict stash@{0}
```

### 6.2 Stash 管理策略

| 场景 | 操作 | 说明 |
|------|------|------|
| 临时切换到其他分支审查问题 | `git stash` | 保存当前 WIP，切走后回来 `pop` |
| 拉取代码时本地有未提交修改 | `git stash` + `git pull` + `git stash pop` | 或使用 `--autostash` |
| 实验性功能开发一半 | `git stash -m "experiment-X"` | 保留实验代码，后续 `apply` |
| 多个 WIP 工作同时进行 | `git stash push -m "描述1"` 多次 | 用描述区分，按栈顺序管理 |
| 误 stash 后找回 | `git stash list` + `git stash apply` | 只要没 drop，随时可取回 |

> ⚠️ **Stash 风险**: Stash 存储在本地 `.git/refs/stash`，**不随 push 推送**。重装系统或删除仓库将永久丢失。重要 WIP 建议推到分支上存储。

---

## 7. Log Mastery

日志查询是日常排查问题的核心能力，从简单查看历史到高级搜索定位。

### 7.1 Custom Log Format

```bash
# 标准美化格式（推荐 alias 使用）
git log --graph --pretty=format:'%C(yellow)%h%C(reset) - %C(green)%an%C(reset) %C(blue)(%ar)%C(reset)%C(auto)%d%C(reset)%n %s' --all

# 最简单行格式
git log --oneline

# 常用 format 占位符
git log --pretty=format:"%h - %an, %ar : %s"
```

**Format 占位符速查表**:

| 占位符 | 含义 | 示例输出 |
|--------|------|----------|
| `%h` | 短提交哈希 | `a1b2c3d` |
| `%H` | 完整提交哈希 | `a1b2c3d4e5f6...` |
| `%an` | 作者名 | `Zhang San` |
| `%ae` | 作者邮箱 | `zhangsan@example.com` |
| `%ar` | 相对时间 | `2 days ago` |
| `%ai` | ISO 格式时间 | `2024-05-15 14:30:00 +0800` |
| `%s` | 提交信息标题 | `feat: 新增用户注册接口` |
| `%b` | 提交信息正文 | `详细描述内容` |
| `%d` | ref 引用名 | `(HEAD -> feature/login, origin/dev)` |
| `%C(color)` | 颜色设置 | `%C(red)`, `%C(auto)` |
| `%n` | 换行 | `\n` |

```bash
# 创建常用 alias
git config --global alias.lg "log --graph --pretty=format:'%C(yellow)%h%C(reset) - %C(green)%an%C(reset) %C(blue)(%ar)%C(reset)%C(auto)%d%C(reset)%n %s' --all"
git config --global alias.l "log --oneline --graph --decorate"
```

### 7.2 高级过滤

```bash
# 按作者过滤
git log --author="Zhang"
git log --author="zhangsan@example.com"
git log --committer="Wang"

# 按时间过滤
git log --after="2024-05-01"
git log --before="2024-06-01"
git log --since="2 weeks ago"
git log --until="yesterday"
git log --since="2024-01-01" --until="2024-06-01"

# 按文件过滤
git log -- src/main/java/com/xxx/service/UserService.java

# 按提交信息搜索
git log --grep="BUG2024001"       # 搜索包含 BUG2024001 的提交
git log --grep="feat" --grep="user" --all-match  # 同时匹配多个关键词

# 按代码内容搜索（pickaxe）
git log -S"passwordEncoder"        # 搜索包含"passwordEncoder"字符串变更的提交
git log -S"@Transactional" --pickaxe-all  # 搜索含 @Transactional 的提交

# 按修改文件数量/行数过滤
git log --diff-filter=M            # 仅显示修改的文件（A=Add, D=Delete, M=Modify）
git log --diff-filter=A --name-only  # 仅显示新增文件名称
```

### 7.3 代码变更搜索

```bash
# -S 搜索（pickaxe）：搜索字符串的出现/消失
git log -S"oldMethodName" --oneline
git log -S"// TODO: " --all       # 搜索所有分支上的 TODO

# -G 搜索（正则）：搜索匹配正则表达式的行变更
git log -G"function\s+\w+\(.*\)" --oneline

# -L 搜索（行范围）：追踪某段代码的历史
git log -L 1,20:UserService.java  # 查看 UserService.java 第 1-20 行的变更历史

# 调试二分查找（定位引入 Bug 的提交）
git bisect start HEAD v1.0.0      # 二分查找范围
git bisect bad                    # 当前版本有 Bug
git bisect good                   # 历史版本正常
git bisect run mvn test           # 自动化查找
git bisect reset                  # 结束 bisect
```

---

## 8. Undo Operations for Daily Use

撤销操作是 Git 日常管理中最需要谨慎对待的部分，理解每种撤销的语义和影响范围至关重要。

### 8.1 Undo 命令全景矩阵

| 我要做什么 | 命令 | 影响范围 | 是否可逆 | 是否重写历史 |
|-----------|------|----------|----------|-------------|
| 撤销工作区文件修改 | `git restore <file>` | 工作区 | 部分(取决于IDE本地历史) | 否 |
| 撤销暂存（unstage） | `git restore --staged <file>` | 暂存区 | 是 | 否 |
| 撤销上一次提交但保留修改 | `git reset --soft HEAD~1` | 提交历史 | 是 | 是 |
| 撤销上一次提交并丢弃修改 | `git reset --hard HEAD~1` | 提交历史+工作区 | ⚠️ 否 | 是 |
| 撤销已推送的提交（安全方式） | `git revert <commit>` | 提交历史（新提交） | 是 | 否（新增回滚提交） |
| 恢复已删除的分支 | `git reflog` + `git branch` | 分支引用 | 是（24小时内） | 否 |

### 8.2 日常撤销场景详解

```bash
# ---- 场景 1：工作区修改有误，想恢复到 HEAD 版本 ----
git restore README.md
# 等同于 git checkout -- README.md

# ---- 场景 2：不小心 git add 了不该 add 的文件 ----
git restore --staged target/classes/UserController.class
git restore --staged src/main/resources/application-dev.yml
# 等同于 git reset HEAD -- <file>

# ---- 场景 3：提交后发现漏了文件，或文案有误 ----
git add missing-file.java
git commit --amend --no-edit

# ---- 场景 4：最近一次提交完全搞错了，想回退并重新开始 ----
git reset --soft HEAD~1
# 工作区和暂存区保留所有修改，可以重新 add 和 commit

# ---- 场景 5：最近一次提交完全搞错了，想丢弃 ----
git reset --hard HEAD~1
# ⚠️ 工作区的修改也会被丢弃！确保你不需要这些修改

# ---- 场景 6：已经推送到远程的提交有 Bug ----
git revert a1b2c3d
# Git 创建一个"反向提交"来抵消 a1b2c3d 的改动
# 这是最安全的撤销已推送提交的方式
git revert HEAD~3..HEAD          # 回滚最近 3 个提交
git revert --no-commit HEAD~3..HEAD  # 回滚但不自动提交，以便合并解决冲突

# ---- 场景 7：revert 后又需要恢复之前 revert 的代码 ----
git revert <revert-commit-id>    # 撤销 revert 本身
```

### 8.3 reflog：终极后悔药

`git reflog` 记录 HEAD 指针的每一次移动，是 Git 最强大的数据恢复工具。

```bash
$ git reflog
a1b2c3d HEAD@{0}: commit: feat: 新增支付模块
e5f6g7h HEAD@{1}: reset: moving to HEAD~1
i8j9k0l HEAD@{2}: commit: feat: 新增订单导出
m1n2o3p HEAD@{3}: commit: fix: 修复空指针异常

# 恢复被 reset --hard 丢弃的提交
git reflog
# 找到 reset 之前的提交哈希
git reset --hard e5f6g7h          # 恢复到 reflog 中的状态

# 恢复被删除的分支
git branch recovered-branch a1b2c3d   # 基于 reflog 中的哈希重建分支

# 查看某次 reflog 条目的详细信息
git show HEAD@{2}
```

> ⚠️ **Reflog 限制**: reflog 默认保留 90 天（可配置），且仅在本地存储。克隆的新仓库无 reflog 记录。

---

## 9. .git Directory Walkthrough

`.git` 目录是 Git 仓库的心脏，理解其结构能从根本上掌握 Git 工作原理。

```bash
.git/
├── HEAD                  # 指向当前分支的引用（符号引用）
├── config                # 仓库级配置（user.name, remote url 等）
├── description           # 仓库描述（主要用于 GitWeb）
├── index                 # 暂存区文件（二进制，追踪文件的 blob/SHA）
├── packed-refs           # 压缩的引用文件
├── refs/
│   ├── heads/            # 本地分支（每个文件名为分支名，内容为提交哈希）
│   │   ├── main          # → a1b2c3d...
│   │   ├── develop       # → e5f6g7h...
│   │   └── feature/login # → i8j9k0l...
│   ├── remotes/          # 远程追踪分支
│   │   └── origin/
│   │       ├── HEAD      # → refs/remotes/origin/main
│   │       ├── main      # → a1b2c3d...
│   │       └── develop   # → e5f6g7h...
│   └── tags/             # 标签（轻量标签指向提交，附注标签指向 tag 对象）
│       └── v1.0.0        # → tag 对象
├── objects/              # 对象存储（SHA-1 hash 首字母/剩余38位）
│   ├── a1/               # 哈希以 a1 开头的对象
│   │   └── b2c3d4e5f6... # 提交对象
│   ├── e5/
│   │   └── f6g7h8i9j0... # 树对象（tree）
│   ├── pack/             # 打包后的对象文件（.pack + .idx）
│   │   ├── pack-xxxxxx.pack
│   │   └── pack-xxxxxx.idx
│   └── info/             # pack 文件索引信息
├── hooks/                # Git 钩子脚本
│   ├── pre-commit.sample
│   ├── commit-msg.sample
│   ├── pre-push.sample
│   ├── pre-receive.sample
│   └── post-receive.sample
├── info/                 # 排除规则（类似 .gitignore 但本地生效）
│   └── exclude
├── logs/                 # reflog 日志
│   ├── HEAD
│   └── refs/
│       ├── heads/
│       │   └── main
│       └── remotes/
│           └── origin/
│               └── main
└── FETCH_HEAD            # 最后一次 fetch 获取的分支/引用信息
```

### 9.1 key 文件详解

| 文件 | 内容 | 作用 |
|------|------|------|
| `HEAD` | `ref: refs/heads/main` | 符号引用，指向当前检出的分支 |
| `index` | 二进制（blob SHA + 元数据） | 暂存区，记录下一次提交的内容快照 |
| `refs/heads/main` | `a1b2c3d4e5f6...` | main 分支的最新提交哈希 |
| `objects/ab/cdef...` | 对象数据（commit/tree/blob/tag） | Git 数据存储的核心 |

```bash
# 探索 .git 的实用命令
cat .git/HEAD                    # 查看当前分支指向
git symbolic-ref HEAD            # 使用 Git 命令查看 HEAD

git rev-parse HEAD               # 获取 HEAD 的完整哈希
git rev-parse --short HEAD

git cat-file -t a1b2c3d          # 查看对象类型 (commit/tree/blob/tag)
git cat-file -p a1b2c3d          # 查看对象内容

git ls-files --stage             # 查看暂存区中的所有文件
git ls-tree HEAD                 # 查看 HEAD 指向的树对象

git count-objects -v             # 查看对象存储统计
```

---

## 10. Repository Maintenance

仓库维护是 Git 日常管理的"体检与保健"环节，定期执行可保持仓库健康和高效。

### 10.1 git gc（Garbage Collection）

```bash
# 手动触发垃圾回收（Git 会自动在适当时机执行）
git gc

# 更激进的优化（推荐不定期执行）
git gc --aggressive
git gc --prune=now              # 立即删除所有不可达对象

# 查看优化效果
git count-objects -v
# count: 0         # loose objects 数量（应尽可能少）
# size: 0          # loose objects 总大小
# in-pack: 1500    # pack 文件中的对象数
# packs: 3         # pack 文件数（越少性能越好）
# prune-packable: 0
# garbage: 0
```

**gc 自动触发时机**:

| 条件 | Git 自动执行 |
|------|-------------|
| loose object 数 > 7000 | `git gc --auto` |
| pack 文件数 > 50 | `git gc --auto` |
| 距离上次 gc 超过 30 天 | `git gc --auto` |

### 10.2 git prune（清理松散对象）

```bash
# 清理不可达的松散对象
git prune

# 查看哪些对象将被清理（不带 --expire 的 dry-run）
git fsck --unreachable

# git gc 内部会自动调用 git prune
# 独立 prune 场景：gc 未自动清理时
git prune --expire=now           # 立即清理所有过期松散对象
git prune --expire=2.weeks       # 清理超过2周未引用的对象
```

### 10.3 git fsck（File System Check）

```bash
# 检查仓库完整性（类似文件系统的 fsck）
git fsck
# Checking object directories: 100% (256/256), done.
# dangling commit a1b2c3d4e5f6...  # 孤儿提交（无任何引用可达）
# dangling blob e5f6g7h8i9j0...    # 孤儿 blob

# 常用检查项
git fsck --full                   # 完整检查所有对象
git fsck --unreachable            # 列出所有不可达对象
git fsck --lost-found             # 将丢失的对象写入 .git/lost-found/

# 修复检测到的问题
git fsck --full --strict          # 严格模式，检查更多潜在问题
```

### 10.4 Maintenance 最佳实践

| 频率 | 操作 | 目的 |
|------|------|------|
| 按需 | `git gc` | 压缩对象存储，优化性能 |
| 按需 | `git gc --aggressive` | 深度优化（大型仓库季度执行） |
| 发现异常 | `git fsck --full` | 检查仓库完整性 |
| 删除分支后 | `git remote prune origin` | 清理远程追踪引用 |
| 每月 | `git count-objects -v` | 监控仓库健康状况 |

> 💡 **使用 `git maintenance`（Git 2.31+）**: `git maintenance start` 在后台自动执行常见维护任务，无需手动操心。

---

## 11. Working with Remotes

远程管理是 Git 日常协作的基础，从单远程到多远程的进阶管理。

### 11.1 管理多个 Remote

```bash
# 查看远程仓库
git remote -v
# origin  https://github.com/user/project.git (fetch)
# origin  https://github.com/user/project.git (push)

# 添加额外远程（例如 fork 的上游仓库）
git remote add upstream https://github.com/original/project.git

# 重命名远程
git remote rename origin github

# 修改远程 URL
git remote set-url origin https://new-url.com/repo.git

# 删除远程
git remote remove stale-remote

# 查看远程详细信息
git remote show origin
```

### 11.2 Fork Sync 最佳实践

```bash
# 场景：在 fork 仓库中开发，需要同步上游仓库

# 初始设置
git remote add upstream https://github.com/original/project.git

# 每日同步（在 main 分支执行）
git checkout main
git fetch upstream
git rebase upstream/main          # 用上游代码 rebase 本地的 main
git push origin main              # 推送到自己的 fork

# 在 feature 分支上同步
git checkout feature/my-feature
git rebase main                   # 基于最新的 main 变基
# 或
git merge main                    # 合并 main 到 feature

# 将上游的指定分支同步到本地
git fetch upstream feature/xxx
git checkout -b feature/xxx upstream/feature/xxx
```

### 11.3 Remote Branch Cleanup

```bash
# 清理远程已删除但在本地残留的追踪分支
git remote prune origin

# 批量删除已合并的远程分支
git branch -r --merged origin/main | grep -v 'origin/main' | grep -v 'origin/develop' | sed 's/origin\///' | xargs -I {} git push origin --delete {}

# 彻底清理本地和远程
# 本地已合并分支
git branch --merged | grep -v '\*' | grep -v 'main' | grep -v 'develop' | xargs git branch -d

# 同步远程已删除分支到本地
git fetch --prune
```

### 11.4 Remote 配置矩阵

| 场景 | 推荐 Remote 配置 | Push URL |
|------|------------------|----------|
| 个人项目 | `origin → github` | Push allowed |
| 企业内部协作 | `origin → gitlab` | Push allowed |
| Fork 开发 | `origin → fork` + `upstream → 原项目` | Fork push, upstream pull-only |
| 开源贡献 | `origin → fork` + `upstream → 原项目` | Fork push, upstream PR |
| 多平台同步 | `origin → github` + `gitee → gitee` | 两个都 push |

---

## 12. Complete Daily Workflow Example

以下是一个完整的开发者日常 Git 工作流，串联全篇文章的所有知识点。

### 12.1 Morning: 开工同步

```bash
# 08:55 - 到了工位，泡好咖啡
cd ~/workspace/my-project

# 检查状态，确认昨晚没有未完成的工作
git status

# 进入开发分支，同步远程最新代码
git checkout develop
git pull --rebase

# 开始新功能开发
git checkout -b feature/PROJ-456-order-export
```

### 12.2 Mid-Morning: 功能开发

```bash
# 09:30 - 完成 ExportService 的核心逻辑
git add src/main/java/com/xxx/service/ExportService.java
git commit -m "feat(order-service): 新增导出订单CSV核心逻辑 #PROJ-456"

# 10:15 - 完成 Controller 层
git add src/main/java/com/xxx/controller/ExportController.java
git diff --staged                    # 审查
git commit -m "feat(order-service): 新增导出接口和参数校验 #PROJ-456"

# 11:00 - 发现之前提交有遗漏的注解
git add-forgotten-annotation.java
git commit --amend --no-edit
```

### 12.3 Noon: 午间推送

```bash
# 11:45 - 推送上午的工作成果
git push -u origin feature/PROJ-456-order-export

# 确认远程分支建立成功
git branch -a
```

### 12.4 Afternoon: 同步与冲突解决

```bash
# 14:00 - 午休回来，同步团队代码
git fetch origin develop
git rebase origin/develop
# 可能遇到冲突，解决...

# 解决冲突后
git add resolved-file.java
git rebase --continue
git commit -m "fix: 解决与支付模块的合并冲突 #PROJ-456"

# 16:00 - 完成全部开发，最终推送
git push
```

### 12.5 Late Afternoon: PR 准备

```bash
# 16:30 - 清理提交历史
git log --oneline                     # 审查提交记录
# 如果发现某次提交里有调试代码...
git rebase -i HEAD~3                  # 交互式整理提交

# 最终确认
git diff develop..feature/PROJ-456-order-export  # 查看全部差异
git log --oneline develop..HEAD                   # 确认提交历史和数量

# 推送最终版本
git push --force-with-lease           # 如果 rebase 后需要强制推送

# 17:00 - 在 GitHub/GitLab 创建 PR
# 标题: feat(order-service): 新增订单CSV导出功能 #PROJ-456
# 描述: 包含导出核心逻辑、参数校验、单元测试
```

### 12.6 Code Review & Merge

```bash
# 次日 - Review 反馈需要修改
# 直接在 feature 分支上修改
git commit -m "fix: 修复PR评审指出的空指针问题 #PROJ-456"
git push

# Reviewer approve 后合并

# 合并完成后，清理分支
git checkout develop
git pull --rebase
git branch -d feature/PROJ-456-order-export
```

### 12.7 End of Day: 收尾检查

```bash
# 17:45 - 下班前检查

# 是否有未提交的修改？
git status

# 是否有遗留的 stash？
git stash list

# 是否所有分支都已推送到远程？
git branch -v | grep '\[gone\]'     # 检查已远程删除的本地分支

# 清理无用分支
git remote prune origin

# 确认本次 merge 的提交已同步
git log --oneline origin/develop -5

# 愉快下班！
```

> 🎯 **核心要义**: 优秀的 Git 日常管理不在于记住多少命令，而在于建立一套可重复的操作节律。每天遵循相同的节奏，将 `git pull --rebase → git add -p → git commit → git push --force-with-lease` 变为肌肉记忆。当 Git 操作不再需要思考时，你才能把全部精力放在真正的编码上。
