# Git 版本控制

## 📌 课程定位
Git是现代软件开发的标配——不管你用什么语言、做什么方向，团队协作都依赖Git。理解Git的数据模型（快照+DAG）是解决一切冲突和问题的前提。

## 🎯 核心章节

### 1. Git 基本概念
- **三种状态**：Modified(工作区修改)→Staged(暂存区，git add)→Committed(本地仓库，git commit)
- **Git 数据模型**：Git是内容寻址文件系统——一切操作基于快照和引用
- **SHA-1 哈希**：每次commit产生唯一的40位哈希值，保证数据完整性

### 2. 日常操作（⭐ 必须肌肉记忆）
```bash
git init                    # 初始化仓库
git clone <url>             # 克隆仓库
git status                  # 查看状态（最常用！）
git add <file> / git add .  # 添加到暂存区
git commit -m "message"     # 提交
git log --oneline --graph   # 查看提交历史（可视化分支）
git diff                    # 工作区 vs 暂存区
git diff --staged           # 暂存区 vs 最新提交
```

### 3. 分支管理（⭐ Git 的灵魂）
- **分支本质**：指向某个commit的可移动指针——极其轻量
- **HEAD**：当前所在位置的指针
```bash
git branch <name>           # 创建分支
git checkout -b <name>      # 创建并切换（等价于 switch -c）
git merge <branch>          # 合并分支
git branch -d <name>        # 删除分支（已合并）
git branch -D <name>        # 强制删除
```
- **合并策略**：
  - **Fast-Forward**：目标分支没有新提交→直接移动指针
  - **Three-way Merge**：两个分支都有新提交→生成合并提交(merge commit)
- **合并冲突**：同一文件同一位置被不同分支修改→手动解决→git add→git commit

### 4. 撤销与回退
```bash
git reset HEAD <file>       # 从暂存区移除文件（工作区保留）
git checkout -- <file>      # 撤销工作区修改（丢弃！不可恢复！）
git reset --soft HEAD~1     # 撤销commit但保留暂存区修改
git reset --mixed HEAD~1    # 撤销commit和暂存，保留工作区修改（默认）
git reset --hard HEAD~1     # 全部撤销（危险！）
git revert <commit>         # 安全撤销——创建一个新的反向commit
```

### 5. 远程协作（⭐ 日常高频）
```bash
git remote add origin <url> # 添加远程仓库
git push origin main        # 推送本地分支到远程
git pull = git fetch + git merge  # 拉取并合并
git fetch origin            # 只拉取不合并（安全）
```

- **GitHub/GitLab 协作流程**：
  1. Fork 主仓库 → Clone 自己的Fork
  2. 创建 feature 分支 → 开发 → commit
  3. Push 到自己的Fork → 主仓库发起 Pull Request
  4. Code Review → 修改 → Merge

### 6. 进阶技巧
- **rebase vs merge**：rebase使历史线性整洁(重写历史)，merge保留真实合并记录——黄金法则：不要rebase已push的分支
- **cherry-pick**：选择特定commit应用到当前分支——`git cherry-pick <commit-hash>`
- **stash**：暂存未提交的修改——`git stash` / `git stash pop`
- **reflog**：记录了HEAD的所有移动——救命的后悔药（误操作后从这里找回commit）

### 7. .gitignore
```
node_modules/
*.log
.env
.DS_Store
target/          # Java编译输出
__pycache__/     # Python缓存
```

## ✅ 学习建议
- 先理解Git的数据模型(commit→tree→blob的DAG)，再记命令
- 建一个测试仓库，把reset/rebase/cherry-pick都试一遍
- 多用 `git log --oneline --graph --all` 可视化分支历史
- 《Pro Git》前3章是最好的入门资料
