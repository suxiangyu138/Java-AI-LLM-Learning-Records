# 10-Git 调试取证与排错
> 定位问题 vs 调查历史：bisect/blame 深潜、reflog 取证、worktree、常见陷阱清单、应急预案——"几乎所有操作都是可逆的"

## 📚 目录
1. [git bisect 深潜](#1-git-bisect-深潜)
2. [git blame：谁改了这一行](#2-git-blame谁改了这一行)
3. [git worktree：并行工作区](#3-git-worktree并行工作区)
4. [reflog 取证](#4-reflog-取证)
5. [组合实战：追查线上 Bug](#5-组合实战追查线上-bug)
6. [常见陷阱：detached HEAD / 文件改名 / 大文件](#6-常见陷阱detached-head--文件改名--大文件)
7. [常见陷阱：仓库损坏 / 错误提交 / 冲突 / 换行符](#7-常见陷阱仓库损坏--错误提交--冲突--换行符)
8. [常见错误信息速查表](#8-常见错误信息速查表)
9. [应急预案：force push 失误与密钥泄露](#9-应急预案force-push-失误与密钥泄露)
10. [工具速查表与心理安全指南](#10-工具速查表与心理安全指南)
11. [核心要点](#11-核心要点)
12. [参考来源](#12-参考来源)

## 1. git bisect 深潜

> 💡 完整入门（手动/自动/退出码）见 [07](07-Git日常管理与工作区实践.md) 第 9 节；本节为排错视角的进阶用法。

```bash
git bisect skip         # 跳过无法测试的 commit（编译不过等）
git bisect log          # 查看 bisect 轨迹
git bisect visualize    # 可视化剩余范围
git show $(git bisect view)   # 查看引入 bug 的 commit
```

| 排错要点 | 说明 |
|----------|------|
| 先找确定的好 commit | 上个 release 的 tag 是最佳起点 |
| 写可重复测试脚本 | `git bisect run` 全自动，退出码：0=good、125=跳过、其他=bad |
| 保存记录 | 找到后 `git bisect log` 留档 |
| 结束清理 | 完成后务必 `git bisect reset` |

## 2. git blame：谁改了这一行

```bash
git blame src/auth.js
# a1b2c3d (zhangsan 2025-01-10 10:30:00 +0800  12) function login(user, pass) {
# e4f5g6h (lisi    2025-01-12 14:20:00 +0800  13)   if (!user || !pass) {

git blame -L 10,50 src/auth.js                    # 只看指定范围的行
git blame -w src/auth.js                          # 忽略空白变化（谁真正改了逻辑）
git blame --date=short src/auth.js                # 更详细时间戳
git blame --since="2026-06-01" src/auth.js        # 按时间过滤
git blame -e src/auth.js                          # 显示作者邮箱
git blame --ignore-rev 8a2f8c9 src/auth.js        # 忽略某个 commit（大规模重构）

# 忽略 commit 列表写入配置文件（推荐）
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

```text
# .git-blame-ignore-revs 示例内容：
# 忽略大规模格式化的 commit
a1b2c3d4e5f6...  # prettier format all files
```

> 🎯 **blame 正确用法**："这行代码的背景是什么？哪个 PR 引入的？"——配合 bisect 定位 bug；**不是用来甩锅的**。

## 3. git worktree：并行工作区

概念：同时 checkout 多个分支到不同目录（完整场景见 [03](03-Git分支模型与团队工作流.md) 第 10 节）。

```bash
git worktree add ../project-feature feature/login   # 创建 worktree
git worktree add --detach ../project-temp           # 仅创建不切换（后台用）
git worktree list
# /Users/me/project              a1b2c3d [main]
# /Users/me/project-feature      e4f5g6h [feature/login]
git worktree remove ../project-feature
git worktree prune
```

| 使用场景 | 说明 |
|----------|------|
| 跑长任务时切换 | 当前分支跑测试/构建，切 worktree 写其他代码 |
| 快速修复线上 bug | 不用 stash 当前工作 |
| Code Review | 拉 PR 分支到 worktree 本地验证 |
| 对比两个分支 | 并排 diff |

## 4. reflog 取证

```bash
git reflog                 # HEAD 操作日志（90 天后悔药）
git reflog --date=iso      # 带时间戳
# "我搞砸了，想回到 10 分钟前的状态"
git reflog                 # 找到 10 分钟前的 HEAD 位置
git reset --hard HEAD@{5}

# detached HEAD 已做 commit 又切走 → reflog 找回
git switch -c recovered-branch <commit-hash>
```

| 取证场景 | 命令 |
|----------|------|
| 找回被 reset --hard 干掉的 commit | `git reflog` + `git reset --hard HEAD@{N}` |
| 找回被 rebase 搞砸的状态 | `git reflog` + `git reset --hard HEAD@{N}` |
| 恢复误删的分支 | `git branch recovered-branch <hash>` |
| 查看某次 reflog 条目的详情 | `git show HEAD@{2}` |

> ⚠️ Reflog 限制：默认保留 90 天（可配置），仅本地存储。克隆的新仓库无 reflog 记录。

## 5. 组合实战：追查线上 Bug

```bash
# 场景：线上 v2.0.0 正常，v2.1.0 出现性能退化
git bisect start
git bisect bad v2.1.0
git bisect good v2.0.0
git bisect run ./perf-test.sh          # Step 1: bisect 定位 → a1b2c3d first bad commit
git show a1b2c3d --stat                # Step 2: 看这个 commit 做了什么
git blame src/search.js -L 50,80       # Step 3: blame 看文件修改历史
git worktree add ../project-fix fix/search-perf   # Step 4: worktree 并行测试修复
git checkout release/2.1.x             # Step 5: cherry-pick 修复到 release 分支
git cherry-pick <fix-commit>
```

## 6. 常见陷阱：detached HEAD / 文件改名 / 大文件

### 6.1 Detached HEAD（三种情况处理）

```bash
git checkout a1b2c3d
# You are in 'detached HEAD' state.
git branch
# * (HEAD detached at a1b2c3d)
#   main

# 情况 1：刚切过来，还没做修改 → 切回去
git switch main
# 情况 2：已经做了 commit → 创建分支保留它们
git switch -c my-temp-branch
# 情况 3：已经做了 commit，又切到了其他分支 → reflog 找回
git reflog
git switch -c recovered-branch <commit-hash>
```

### 6.2 文件改名后 Git 认为文件被删除+新建

```bash
# 问题：mv old-name.js new-name.js 后显示 deleted + Untracked → 历史断了！

# 正确方式（Git 自动追踪重命名，相似度 > 50%）
git mv old-name.js new-name.js
# git status → renamed: old-name.js -> new-name.js

# 已手动 mv 的补救
git add old-name.js new-name.js   # Git 会自动识别为 renamed

# log 自动跟踪重命名
git log --follow new-name.js
```

### 6.3 大文件提交了怎么办

```bash
# 现象：remote: error: File video.mp4 is 120.00 MB; exceeds GitHub's 100.00 MB limit

# 方案 A：Git LFS（见 06 第 12 节）
# 方案 B：从历史中彻底删除
# 找到大文件
git rev-list --objects --all | \
  git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)' | \
  sed -n 's/^blob //p' | sort -n -k 2 | tail -20

# 用 filter-repo 删除（推荐，比 filter-branch 快很多）
pip install git-filter-repo
git filter-repo --path video.mp4 --invert-paths
git push --force

# 或者用 BFG Repo-Cleaner
java -jar bfg.jar --delete-files video.mp4 .
```

## 7. 常见陷阱：仓库损坏 / 错误提交 / 冲突 / 换行符

### 7.1 .git 目录损坏

```bash
# 症状：fatal: bad object HEAD / Unable to read current working directory
git fsck --full          # 诊断：检查对象完整性
git fsck --lost-found    # 修复引用：损坏的引用放在 .git/lost-found/ 中
# 如果远程仓库还完整 → 重新克隆最简单
git clone <remote-url> fresh-repo
```

### 7.2 在错误的仓库/分支上做了 commit

```bash
# 场景：本来应该在 feature/login 开发，结果在 main 上 commit 了
git checkout -b feature/login          # Step 1: 把 commit 搬到正确分支
# 或者：git checkout feature/login && git cherry-pick <wrong-commit>
git checkout main
git reset --hard HEAD~1                # Step 2: 在 main 上撤销
# 如果已经 push 了 main，用 revert（不要 force push main！）
git revert HEAD
```

### 7.3 Merge Conflict 太复杂

```bash
git merge --abort          # 冲突太多，回到合并前的清爽状态
git rebase main            # 换策略：每个 commit 单独解决冲突，更可控
git checkout --theirs -- .    # 放弃自己的改动，完全接受对方的
git checkout --ours -- .      # 完全用自己的
git add . && git commit
```

### 7.4 .gitignore 不生效

```bash
# 原因：文件已被 Git 追踪，加到 .gitignore 后仍被追踪
git rm --cached file-to-ignore.txt
git commit -m "chore: stop tracking file-to-ignore.txt"
git rm --cached -r build/
git commit -m "chore: stop tracking build directory"
```

### 7.5 换行符问题（CRLF/LF）

```bash
git config --global core.autocrlf true     # Windows：checkout 时 CRLF→LF，commit 时 LF→CRLF
git config --global core.autocrlf input    # Mac/Linux：commit 时 CRLF→LF
```

```gitattributes
# 仓库强制指定（最可靠）
*       text=auto
*.java  text eol=lf
*.bat   text eol=crlf
```

### 7.6 git status 太慢

```bash
git config core.untrackedCache true        # 1. 文件系统缓存
git config core.fsmonitor true             # 2. FS Monitor（Git 2.37+，极大加速）
git gc --aggressive --prune=now            # 3. 定期 GC
# 4. 不需要跟踪的目录确保在 .gitignore 中
```

### 7.7 命令输错自动纠错

```bash
git config --global help.autocorrect 1
# 1 = 等待 0.1 秒后自动执行；immediate = 立即执行；0 = 只提示不执行（默认）
```

## 8. 常见错误信息速查表

| 错误信息 | 原因 | 解决 |
|---------|------|------|
| `fatal: not a git repository` | 不在 Git 仓库目录中 | `cd` 到正确的目录 |
| `fatal: refusing to merge unrelated histories` | 两个仓库没有共同祖先 | `git merge --allow-unrelated-histories` |
| `error: failed to push some refs` | 远程有新 commit，你落后了 | `git fetch && git rebase` 后再 push |
| `fatal: You are not currently on a branch` | Detached HEAD 状态 | `git switch main` |
| `Your branch is ahead of 'origin/main' by X commits` | 本地有未 push 的 commit | `git push` |
| `CONFLICT (content): Merge conflict in X` | 合并冲突 | 手动解决 → git add → git commit |
| `Please commit your changes or stash them` | 工作区不干净不能切换分支 | `git stash` 或先 commit |
| `remote: error: File X is 120 MB; exceeds limit` | 提交了大文件 | LFS 或 filter-repo 清理（见 6.3） |

## 9. 应急预案：force push 失误与密钥泄露

### 9.1 force push 错覆盖同事代码

```text
① 立刻告诉团队不要做更多操作
② 同事本地有原 commit 可直接 push 回去
③ 否则检查远程仓库备份/事件日志（GitHub Settings → Events 查看 push 事件）
```

### 9.2 提交了密码/密钥到公开仓库

```text
① 立即去对应服务 revoke/rotate 密钥（最高优先级！）
② git filter-repo --path .env --invert-paths + git push --force
③ GitHub 联系 support 清除缓存（已克隆的人可能仍有）
④ 配置 pre-commit hook 防止再次发生（detect-private-key，见 08）
```

```bash
# 从历史中彻底清除敏感信息（完整工具集见 11）
git filter-repo --path .env --invert-paths                  # 删除文件
git filter-repo --replace-text <(echo "old-password==>REDACTED")   # 替换密码
git filter-repo --strip-blobs-bigger-than 10M               # 删除大文件
git push --force --all && git push --force --tags
```

## 10. 工具速查表与心理安全指南

| 命令 | 作用 | 关键参数 |
|------|------|---------|
| `git bisect` | 二分定位 bug | `run`, `skip`, `visualize` |
| `git blame` | 谁改了这一行 | `-L`, `-w`, `--ignore-revs-file` |
| `git worktree` | 并行工作区 | `add`, `list`, `remove`, `prune` |
| `git stash` | 保存半成品 | `push -u`, `branch`, `show -p` |
| `git cherry-pick` | 挑选 commit | `--no-commit`, `--continue`, `--abort` |
| `git reflog` | HEAD 操作日志 | `show`, `--date=iso` |
| `git grep` | 搜索代码库 | `-n`, `-i`, `--name-only` |
| `git fsck` | 对象完整性检查 | `--full`, `--lost-found` |
| `git filter-repo` | 历史重写清理 | `--path`, `--replace-text`, `--strip-blobs-bigger-than` |

```text
心理安全指南：
✅ 几乎所有操作都是可逆的（有 reflog）
✅ 不确定时先用 git stash 或 git branch backup 做保险
✅ git push --force 前停一下，确认没有同事在基于你的分支开发
✅ 小步提交，频繁 push（备份到远程）
✅ 多写 commit message，少猜"这行代码是干嘛的"
✅ 遇到解决不了的冲突 → git merge --abort，休息一下再来
```

## 11. 核心要点

> 🎯 **核心要点**：
> - 定位问题三件套：bisect（哪个 commit 引入）→ blame（谁改的）→ show（改了什么）；
> - 取证四件套：reflog（HEAD 历史）、git log 过滤器（时间/作者/文件）、pickaxe（-S/-G）、fsck（悬空对象）；
> - detached HEAD 三情况：没改切回、改了建分支、切走了 reflog 找回；
> - 大文件/敏感信息清理：filter-repo（推荐）> BFG > filter-branch（慢）；
> - 心理安全：**几乎所有操作都可逆**——reflog 是 90 天保险，小步提交是日常保险。

## 12. 参考来源

- [Pro Git Book：调试与排错](https://git-scm.com/book/zh/v2/Git-工具-使用-Git-调试)
- [Git 官方文档：git-bisect / git-blame / git-worktree](https://git-scm.com/docs)
- [git-filter-repo 官方文档](https://github.com/newren/git-filter-repo)
- [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/)

---

**下一模块**：[11-Git配置安全与最佳实践](11-Git配置安全与最佳实践.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
