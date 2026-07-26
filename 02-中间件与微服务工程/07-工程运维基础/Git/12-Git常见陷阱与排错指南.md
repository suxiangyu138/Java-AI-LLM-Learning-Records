# Git 常见陷阱与排错指南

## 每个 Git 用户都会踩的坑

---

## 1. Detached HEAD — 游离的 HEAD

### 现象

```bash
git checkout a1b2c3d
# You are in 'detached HEAD' state.

# git branch 看不到任何分支名
git branch
# * (HEAD detached at a1b2c3d)
#   main
```

### 发生了什么？

HEAD 直接指向了一个具体的 commit，而不是一个分支。如果此时做 commit，这些 commit 没有分支引用，切走后很容易丢失。

### 怎么办？

```bash
# 情况1：刚切过来，还没做修改 → 切回去
git switch main

# 情况2：已经做了 commit → 创建分支保留它们
git switch -c my-temp-branch
# 现在你的 commit 安全了，被 my-temp-branch 引用着

# 情况3：已经做了 commit，又切到了其他分支 → reflog 找回
git reflog
# 找到 detached HEAD时的 commit hash
git switch -c recovered-branch <commit-hash>
```

---

## 2. 文件改名后 Git 认为文件被删除+新建

### 问题

```bash
mv old-name.js new-name.js
git status
# deleted: old-name.js
# Untracked files: new-name.js
# → 文件历史断了！
```

### 正确方式

```bash
# Git 能自动追踪重命名（相似度 > 50%）
git mv old-name.js new-name.js
git status
# renamed: old-name.js -> new-name.js

# 如果已经手动 mv 了，补救：
git add old-name.js new-name.js
# Git 会自动识别为 renamed

# log 自动跟踪重命名
git log --follow new-name.js
```

---

## 3. 大文件提交了怎么办？

### 问题

```bash
git push
# remote: error: File video.mp4 is 120.00 MB; exceeds GitHub's 100.00 MB limit
```

### 解决方案

**方案 A：用 Git LFS（推荐，大文件用）**

```bash
# 安装 Git LFS
git lfs install

# 追踪大文件类型
git lfs track "*.mp4"
git lfs track "*.psd"
git lfs track "*.zip"

# .gitattributes 会被自动更新
git add .gitattributes
git add video.mp4
git commit -m "add video with LFS"

# 现在可以 push 了
git push origin main
```

**方案 B：从历史中彻底删除大文件**

```bash
# 找到大文件
git rev-list --objects --all | \
  git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)' | \
  sed -n 's/^blob //p' | \
  sort -n -k 2 | \
  tail -20

# 用 filter-repo 删除（推荐，比 filter-branch 快很多）
# pip install git-filter-repo
git filter-repo --path video.mp4 --invert-paths
git push --force

# 或者用 BFG Repo-Cleaner
# java -jar bfg.jar --delete-files video.mp4 .
```

---

## 4. 在错误的仓库/分支上做了 commit

```bash
# 场景：本来应该在 feature/login 开发，结果在 main 上 commit 了

# Step 1: 把 commit 搬到正确分支
git checkout -b feature/login
# 或者如果分支已存在：
# git checkout feature/login && git cherry-pick <wrong-commit>

# Step 2: 在 main 上撤销那个 commit
git checkout main
git reset --hard HEAD~1

# 如果已经 push 了 main，用 revert（不要 force push main！）
git revert HEAD
```

---

## 5. Merge Conflict 太复杂

```bash
# 冲突太多，解决不完了
git merge --abort
# 回到合并前的清爽状态

# 换个策略，用 rebase（逐个commit解决冲突可能更容易）
git rebase main
# 每个 commit 单独解决冲突，更可控

# 或者放弃自己的改动，完全接受对方的版本
git merge feature/xxx
# 冲突后：
git checkout --theirs -- .       # 完全用对方的
git checkout --ours -- .         # 完全用自己的
git add . && git commit
```

---

## 6. .gitignore 不生效

```bash
# 问题：文件已经被 Git 追踪了，加到 .gitignore 后仍然被追踪

# 解决：从 Git 追踪中移除（但保留本地文件）
git rm --cached file-to-ignore.txt
git commit -m "chore: stop tracking file-to-ignore.txt"

# 对于整个目录
git rm --cached -r build/
git commit -m "chore: stop tracking build directory"
```

---

## 7. 换行符问题（Windows/Mac/Linux 混用）

```bash
# Windows: CRLF (\r\n)
# Mac/Linux: LF (\n)

# 配置自动转换（Windows 上推荐）
git config --global core.autocrlf true
# checkout 时 CRLF → LF，commit 时 LF → CRLF

# Mac/Linux 上推荐
git config --global core.autocrlf input
# commit 时 CRLF → LF，checkout 时不做转换

# 为仓库强制指定换行符
# .gitattributes
* text=auto
*.js text eol=lf
*.bat text eol=crlf
```

---

## 8. .git 目录损坏

```bash
# 症状
# fatal: bad object HEAD
# fatal: Unable to read current working directory

# 诊断
git fsck --full
# 检查对象完整性

# 修复引用
git fsck --lost-found
# 损坏的引用会放在 .git/lost-found/ 中

# 如果远程仓库还完整 → 重新克隆是最简单的方案
git clone <remote-url> fresh-repo
# 手动迁移未 push 的更改
```

---

## 9. git status 太慢

```bash
# 原因：工作区文件太多，索引太大

# 解决方案：
# 1. 启用文件系统缓存
git config core.untrackedCache true

# 2. 启用 FS Monitor（Git 2.37+，极大加速）
git config core.fsmonitor true

# 3. 定期 GC
git gc --aggressive --prune=now

# 4. 如果某些目录不需要跟踪，确保在 .gitignore 中
```

---

## 10. Git 命令输错了，导致混乱

```bash
# Git 会提示"是不是想输入这个？"
# 启用自动纠错
git config --global help.autocorrect 1
# 1 = 等待0.1秒后自动执行（给一点反应时间）
# immediate = 立即执行
# 0 = 只提示不执行（默认）
```

---

## 11. 常见错误信息速查

| 错误信息 | 原因 | 解决 |
|---------|------|------|
| `fatal: not a git repository` | 不在 Git 仓库目录中 | `cd` 到正确的目录 |
| `fatal: refusing to merge unrelated histories` | 两个仓库没有共同祖先 | `git merge --allow-unrelated-histories` |
| `error: failed to push some refs` | 远程有新 commit，你落后了 | `git fetch && git rebase` 后再 push |
| `fatal: You are not currently on a branch` | Detached HEAD 状态 | `git switch main` |
| `Your branch is ahead of 'origin/main' by X commits` | 本地有未 push 的 commit | `git push` |
| `CONFLICT (content): Merge conflict in X` | 合并冲突 | 手动解决 → git add → git commit |
| `Please commit your changes or stash them` | 工作区不干净不能切换分支 | `git stash` 或先 commit |

---

## 12. 应急预案

### "我搞砸了，想回到 10 分钟前的状态"

```bash
git reflog
# 找到 10 分钟前的 HEAD 位置
git reset --hard HEAD@{5}
```

### "我 force push 错了，覆盖了同事的代码"

```bash
# 立刻告诉团队！不要做更多操作
# 如果同事本地还有原来的 commit → 他们可以直接 push 回去
# 如果没有 → 检查远程仓库是否有备份/事件日志
# GitHub 上可以在 Settings → Events 中查看 push 事件
```

### "我提交了密码/密钥到公开仓库"

```bash
# 1. 立即撤销密钥！（去对应服务 revoke/rotate）
# 2. 从 Git 历史中彻底删除
git filter-repo --path .env --invert-paths
git push --force
# 3. GitHub 上联系 support 清除缓存（已克隆的人可能仍有）
# 4. 配置 pre-commit hook 防止再次发生
```

---

## 13. 心理安全指南

```text
✅ 几乎所有操作都是可逆的（有 reflog）
✅ 不确定时先用 git stash 或 git branch backup 做保险
✅ git push --force 前停一下，确认没有同事在基于你的分支开发
✅ 小步提交，频繁 push（备份到远程）
✅ 多写 commit message，少猜"这行代码是干嘛的"
✅ 遇到解决不了的冲突 → git merge --abort，休息一下再来
```

---

> 上一篇：[11-Git配置优化与安全实践](11-Git配置优化与安全实践.md)
> 🎉 恭喜学完 Git 生态全部 12 篇文档！返回 [01-Git核心技能](01-Git核心技能.md) 快速回顾，或挑任一一篇深入复习。
