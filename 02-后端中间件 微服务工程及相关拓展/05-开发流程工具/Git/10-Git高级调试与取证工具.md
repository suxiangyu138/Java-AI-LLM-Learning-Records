# Git 高级调试与取证工具

## 定位问题 vs 调查历史

这些命令不常被提起，但关键时刻能救命——bisect 帮你找到 bug 是谁引入的，blame 告诉你这行代码是谁写的，worktree 让你同时干好几件事。

---

## 1. Git Bisect：二分法定位 Bug

### 核心思想

在已知"好的 commit"和"坏的 commit"之间，用二分法快速找到第一个引入 bug 的 commit。

```text
A ── B ── C ── D ── E ── F ── G
✅                          ❌
        ↳ 二分查找这个范围 ↲
```

### 自动二分（推荐）

```bash
# 1. 启动 bisect
git bisect start

# 2. 标记已知的坏提交（当前的）
git bisect bad HEAD

# 3. 标记已知的好提交（上一个正常版本）
git bisect good v1.0.0

# Bisecting: 剩下约 7 个待测试提交

# 4. 编写自动化测试脚本
cat > test.sh << 'EOF'
#!/bin/bash
# 如果这个 commit 有 bug → 返回非0（bad）
# 如果这个 commit 正常 → 返回0（good）
npm test -- --testPathPattern="login.test" 2>&1 | grep -q "FAILED"
EOF
chmod +x test.sh

# 5. 自动二分！
git bisect run ./test.sh

# 输出:
# a1b2c3d is the first bad commit
# commit a1b2c3d
# Author: zhangsan <zhangsan@example.com>
# Date:   Tue Jan 14 10:30:00 2025
#
#     feat: refactor login validation logic

# 6. 结束 bisect
git bisect reset
```

### 手动二分

```bash
git bisect start
git bisect bad HEAD
git bisect good v1.0.0

# Git 会自动 checkout 一个中间的 commit
# 测试这个版本...
# 如果有 bug:
git bisect bad
# 如果正常:
git bisect good

# 重复，直到找到第一个 bad commit
# a1b2c3d is the first bad commit

git bisect reset    # 回到原来的位置
```

### Bisect 使用技巧

```bash
# 跳过无法测试的 commit（编译不过等）
git bisect skip

# 查看 bisect 的轨迹
git bisect log

# 可视化剩余范围
git bisect visualize
```

---

## 2. Git Blame：追责与追溯

### 基础用法

```bash
# 查看文件的每一行是谁、什么时间、哪个 commit 改的
git blame src/auth.js

# a1b2c3d (zhangsan 2025-01-10 10:30:00 +0800  12) function login(user, pass) {
# e4f5g6h (lisi    2025-01-12 14:20:00 +0800  13)   if (!user || !pass) {
# i7j8k9l (zhangsan 2025-01-13 09:00:00 +0800  14)     throw new Error('Invalid input');
```

### 常用选项

```bash
# 只看指定范围的行
git blame -L 10,50 src/auth.js

# 忽略空白变化（谁真正改了逻辑）
git blame -w src/auth.js

# 显示更详细的时间戳
git blame --date=short src/auth.js

# 查看某一行在哪些 commit 中被修改过
git blame -L 42,42 --line-porcelain src/auth.js

# 忽略某些 commit（比如格式化提交）
git blame --ignore-revs-file .git-blame-ignore-revs src/auth.js

# .git-blame-ignore-revs 示例内容：
# # 忽略大规模格式化的 commit
# a1b2c3d4e5f6...  # prettier format all files
```

### 追责不等于甩锅

Blame 的正确用法：
- ✅ "这行代码的背景是什么？哪个 PR 引入的？"→ 理解上下文
- ✅ "这个 bug 是哪个 commit 引入的？"→ 配合 bisect
- ❌ "这是谁的 bug？"→ 不要用 blame 甩锅

---

## 3. Git Worktree：同时工作在多个分支

### 概念

Worktree 让你**同时 checkout 多个分支**到不同目录，不需要来回切换。

```text
~/project/              ← 主工作区 (main 分支)
~/project-feature/      ← worktree (feature/login 分支)
~/project-hotfix/       ← worktree (hotfix/bug-123 分支)
```

### 基础操作

```bash
# 创建一个新 worktree（同时创建分支）
git worktree add ../project-feature feature/login

# 在已有分支上创建 worktree
git worktree add ../project-hotfix hotfix/bug-123

# 不切换过去，仅创建（后台用）
git worktree add --detach ../project-temp

# 列出所有 worktree
git worktree list
# /Users/me/project              a1b2c3d [main]
# /Users/me/project-feature      e4f5g6h [feature/login]
# /Users/me/project-hotfix       i7j8k9l [hotfix/bug-123]

# 删除 worktree（先删除目录，再 prune）
rm -rf ../project-feature
git worktree prune

# 或者一条命令
git worktree remove ../project-feature
```

### 使用场景

| 场景 | 说明 |
|------|------|
| **跑长任务时切换** | 当前分支在跑测试/构建，切到 worktree 写其他代码 |
| **快速修复线上 bug** | 不用 stash 当前工作 → 开 worktree 修 hotfix |
| **Code Review** | 拉 PR 分支到 worktree → 本地运行验证 |
| **对比两个分支** | 两个 worktree 并排，diff 工具直接对比 |

---

## 4. Git Stash 进阶

### 除了基本的 push/pop

```bash
# 只 stash 指定文件
git stash push -m "WIP: only auth changes" -- src/auth.js src/login.js

# stash 包括 untracked 文件
git stash push -u -m "WIP: including new files"

# stash 包括 ignored 文件
git stash push --all -m "WIP: everything"

# 只恢复 stash，不删除记录
git stash apply stash@{0}

# 从 stash 创建分支（测试 stash 内容的好方式）
git stash branch experimental-fix stash@{0}

# 查看 stash 的内容（不改动任何东西）
git stash show -p stash@{0}

# 部分应用 stash（交互式选择）
git stash show -p stash@{0} | git apply -
```

---

## 5. Git Cherry-Pick：精选提交

### 应用场景

```text
main:     A ── B ── C ── D
                   \
feature:            E ── F ── G

# 你想把 commit F 单独拿出来，应用到 hotfix 分支
```

```bash
# 基础用法
git cherry-pick e4f5g6h

# 一次挑选多个 commit
git cherry-pick a1b2c3d e4f5g6h i7j8k9l

# 连续范围
git cherry-pick a1b2c3d..e4f5g6h    # 不包含 a1b2c3d
git cherry-pick a1b2c3d^..e4f5g6h   # 包含 a1b2c3d

# 只应用改动，不自动提交（可以检查/修改后再提交）
git cherry-pick --no-commit e4f5g6h
# 检查代码...
git add . && git commit

# 冲突时：
# 解决冲突 → git add → git cherry-pick --continue
# 放弃：git cherry-pick --abort
# 跳过：git cherry-pick --skip
```

---

## 6. 组合实战：追查线上 Bug

```bash
# 场景：线上 v2.0.0 正常，v2.1.0 出现性能退化

# Step 1: 用 bisect 定位引入性能问题的提交
git bisect start
git bisect bad v2.1.0
git bisect good v2.0.0
git bisect run ./perf-test.sh
# → a1b2c3d is the first bad commit

# Step 2: 用 show 查看这个 commit 做了什么
git show a1b2c3d --stat
# src/search.js  ← 修改了100行

# Step 3: 用 blame 查看这个文件的修改历史
git blame src/search.js -L 50,80

# Step 4: 用 worktree 并行测试修复方案
git worktree add ../project-fix fix/search-perf
# 在 ../project-fix 中开发修复

# Step 5: Cherry-pick 修复到 release 分支
git checkout release/2.1.x
git cherry-pick <fix-commit>
```

---

## 7. 工具速查

| 命令 | 作用 | 关键参数 |
|------|------|---------|
| `git bisect` | 二分定位 bug | `run`, `skip`, `visualize` |
| `git blame` | 谁改了这一行 | `-L`, `-w`, `--ignore-revs-file` |
| `git worktree` | 并行工作区 | `add`, `list`, `remove`, `prune` |
| `git stash` | 保存半成品 | `push -u`, `branch`, `show -p` |
| `git cherry-pick` | 挑选 commit | `--no-commit`, `--continue`, `--abort` |
| `git reflog` | HEAD 操作日志 | `show`, `--date=iso` |
| `git grep` | 搜索代码库 | `-n`, `-i`, `--name-only` |

---

> 上一篇：[09-Git子模块与大仓管理](09-Git子模块与大仓管理.md)
> 下一篇：[11-Git配置优化与安全实践](11-Git配置优化与安全实践.md)
