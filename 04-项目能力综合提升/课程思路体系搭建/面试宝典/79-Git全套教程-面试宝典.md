# Git全套教程 面试宝典
> 基于黑马程序员Git全套教程课程大纲，全面覆盖Git面试高频考点

## 目录

1. [基础概念速答](#一基础概念速答12-18题)
2. [深度原理剖析](#二深度原理剖析8-12题)
3. [实战场景题](#三实战场景题6-10题)
4. [手写代码/配置文件题](#四手写代码配置文件题5-8题)
5. [系统设计题](#五系统设计题3-5题)
6. [常见坑点与最佳实践](#六常见坑点与最佳实践表格)
7. [面试回答模板](#七面试回答模板top-5)
8. [快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（12-18题）

### Q1: Git和SVN的核心区别是什么？
**A:**
| 特性 | Git（分布式） | SVN（集中式） |
|------|-------------|---------------|
| 架构 | 每个开发者本地完整仓库 | 中央仓库，本地只有工作副本 |
| 离线功能 | 完整支持，可离线提交 | 必须联网操作 |
| 单点故障 | 无单点故障 | 中央仓库宕机则无法工作 |
| 分支操作 | 轻量级，秒级创建/切换 | 重量级，慢速 |
| 安全性 | 每个克隆都是完整备份 | 中央仓库损坏则丢失历史 |
| 存储方式 | 基于快照 | 基于差异 |

### Q2: Git的三种状态和四大区域是什么？
**A:** 三种文件状态：
- **Modified（已修改）**: 修改了文件但未暂存
- **Staged（已暂存）**: 修改已添加到暂存区
- **Committed（已提交）**: 修改已保存到本地仓库

四大区域流转：
```
工作区（Working Directory）
    ↓ git add
暂存区（Staging Area / Index）
    ↓ git commit
本地仓库（Repository / .git）
    ↓ git push
远程仓库（Remote Repository）
```

### Q3: git add、git commit、git push的区别？
**A:**
- `git add`: 将工作区修改添加到暂存区（Index）
- `git commit`: 将暂存区内容提交到本地仓库，生成快照
- `git push`: 将本地仓库提交推送到远程仓库

### Q4: 如何配置Git全局用户信息？
**A:**
```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
git config --global --list # 查看配置
git config --global core.autocrlf input # Windows换行符处理
```

### Q5: 什么是Git分支？核心分支操作命令有哪些？
**A:** 分支是Git的核心概念，允许独立开发互不干扰的代码线。
```bash
git branch               # 查看分支
git branch <name>        # 创建分支
git checkout <name>      # 切换分支
git checkout -b <name>   # 创建并切换
git merge <branch>       # 合并分支
git branch -d <name>     # 删除分支
git branch -D <name>     # 强制删除分支（未合并）
```

### Q6: 如何解决Git合并冲突？
**A:** 当两个分支修改了同一文件的同一部分时产生冲突。解决流程：
1. `git status` 查看冲突文件
2. 手动编辑文件解决冲突（保留需要的代码）
3. 删除冲突标记 `<<<<<<<`、`=======`、`>>>>>>>`
4. `git add <file>` 标记为已解决
5. `git commit` 完成合并

### Q7: 什么是快进合并（Fast-Forward Merge）？
**A:** 当当前分支直接领先于目标分支，没有分叉历史时，Git将直接将当前分支指针向前移动，不产生新的合并提交。使用 `--no-ff` 可强制创建合并提交保留分支历史。

### Q8: 远程仓库操作的核心命令？
**A:**
```bash
git remote add origin <url>  # 关联远程仓库
git remote -v                # 查看远程仓库
git push origin <branch>     # 推送到远程
git pull origin <branch>     # 拉取并合并
git fetch origin             # 抓取不合并
git clone <url>              # 克隆远程仓库
```

### Q9: git pull和git fetch的区别？
**A:**
- `git fetch`: 从远程下载最新数据到本地仓库，但不会自动合并到工作区，需要手动 `git merge`
- `git pull`: 实际上是 `git fetch + git merge`，自动拉取并合并到当前分支
- 推荐策略：使用 `git fetch` 查看差异后再决定是否合并，避免意外冲突

### Q10: 如何撤销修改和回退版本？
**A:**
```bash
# 撤销工作区修改
git restore <file>
git checkout -- <file>   # 旧版本语法

# 撤销暂存区
git restore --staged <file>
git reset HEAD <file>    # 旧版本语法

# 回退到指定版本
git reset --soft HEAD~1  # 保留工作区和暂存区
git reset --mixed HEAD~1 # 保留工作区，清空暂存区（默认）
git reset --hard HEAD~1  # 全部丢弃

# revert（安全回退，创建新提交）
git revert HEAD
```

### Q11: .gitignore文件的作用和常用规则？
**A:**
```
# 忽略target目录
target/

# 忽略IDE配置文件
.idea/
*.iml
.settings/
.project

# 忽略编译产物
*.class
*.jar
*.war

# 忽略OS文件
.DS_Store
Thumbs.db

# 忽略环境配置
.env
application-local.properties
*.log

# 取反（不忽略）
!important.log
```

### Q12: Git stash的作用和用法？
**A:** 保存当前未提交的修改，切换到其他分支处理紧急任务。
```bash
git stash                # 保存修改
git stash save "message" # 带描述保存
git stash list           # 查看保存列表
git stash pop            # 恢复并删除stash
git stash apply          # 恢复但不删除
git stash drop           # 删除stash
git stash clear          # 清空所有stash
```

### Q13: 什么是Git标签？如何管理版本？
**A:**
```bash
git tag                  # 查看标签
git tag v1.0.0           # 创建轻量标签
git tag -a v1.0.0 -m "发布1.0.0版本"  # 创建附注标签
git push origin v1.0.0   # 推送标签
git push origin --tags   # 推送所有标签
git tag -d v1.0.0        # 删除本地标签
```

### Q14: 如何配置SSH免密推送？
**A:**
```bash
# 1. 生成SSH密钥
ssh-keygen -t rsa -b 4096 -C "your.email@example.com"

# 2. 查看公钥
cat ~/.ssh/id_rsa.pub

# 3. 将公钥添加到GitHub/Gitee/GitLab

# 4. 修改远程仓库地址为SSH
git remote set-url origin git@github.com:username/repo.git
```

### Q15: 如何删除远程仓库的分支？
**A:**
```bash
git push origin --delete <branch>  # 删除远程分支
git branch -d <branch>             # 删除本地分支
git push origin :<branch>          # 旧语法（推送空到远程）
```

### Q16: 什么是rebase和merge的区别？
**A:**
| 特性 | git merge | git rebase |
|------|-----------|------------|
| 历史记录 | 保留完整合并历史 | 线性化历史 |
| 冲突处理 | 一次解决 | 每步解决 |
| 安全性 | 安全，可追溯 | 重写历史，需谨慎 |
| 适用场景 | 公共分支合并 | 个人分支整理 |

---

## 二、深度原理剖析（8-12题）

### Q1: Git的对象模型（Git Object Model）原理？
**A:** Git是一个内容寻址文件系统，核心是键值对存储，包含四种对象：

- **Blob（二进制大对象）**: 存储文件内容，不包含文件名，通过SHA-1哈希寻址
- **Tree（树对象）**: 存储目录结构，包含文件名、文件模式、指向Blob或子Tree的引用
- **Commit（提交对象）**: 存储提交信息，指向Tree对象和父Commit对象
- **Tag（标签对象）**: 指向特定Commit的引用

```
Commit (sha1)
  ├── tree: (sha1)     # 目录快照
  ├── parent: (sha1)   # 父提交
  ├── author: ...
  ├── committer: ...
  └── message: ...
        |
        v
    Tree (sha1)
      ├── blob (sha1) "README.md"
      ├── blob (sha1) "main.go"
      └── tree (sha1) "src/"
              |
              v
          Tree (sha1)
            ├── blob (sha1) "app.go"
            └── blob (sha1) "utils.go"
```

### Q2: Git的引用（References）机制是什么？
**A:** 引用是指向提交对象的指针，存储在 `.git/refs/` 目录中：

- **分支引用**: `.git/refs/heads/master` 存储提交哈希
- **标签引用**: `.git/refs/tags/v1.0` 指向提交或标签对象
- **远程分支**: `.git/refs/remotes/origin/master` 跟踪远程分支
- **HEAD**: `.git/HEAD` 指向当前分支引用，如 `ref: refs/heads/main`

### Q3: git reset的三个模式（--soft/--mixed/--hard）原理？
**A:**
- **--soft**: 仅移动HEAD指针。工作区和暂存区不变。适用于修改提交信息或重新提交
- **--mixed（默认）**: 移动HEAD指针并重置暂存区。工作区不变。适用于撤销git add
- **--hard**: 移动HEAD指针、重置暂存区、重置工作区。适用于完全丢弃修改

```
初始状态: HEAD -> master -> C3 (暂存区有D, 工作区有E)

git reset --soft HEAD~1:
  HEAD -> master -> C2, 暂存区有D, 工作区有E

git reset --mixed HEAD~1:
  HEAD -> master -> C2, 暂存区空, 工作区有E

git reset --hard HEAD~1:
  HEAD -> master -> C2, 暂存区空, 工作区与C2一致
```

### Q4: git reflog的原理和应用？
**A:** `reflog`（Reference Log）记录了HEAD引用的所有变动，包括分支切换、提交、重置等操作。数据存储在 `.git/logs/HEAD` 中。
```bash
git reflog          # 查看所有操作历史
git reflog --date=iso # 查看带时间的历史
```
**应用场景**: 在使用 `git reset --hard` 丢失提交后，通过 `git reflog` 找到丢失的提交哈希，再 `git reset --hard <hash>` 恢复。

### Q5: Git合并策略有哪些？
**A:**
1. **Fast-Forward**: 直线历史，直接移动指针，无合并提交
2. **Recursive（递归）**: 默认策略，Three-Way Merge，需要时自动寻找共同祖先
3. **Octopus（八爪鱼）**: 多分支合并（3个以上分支）
4. **Ours**: 完全保留当前分支，忽略其他分支变更
5. **Subtree**: 合并子项目到子目录

### Q6: Merge冲突的原理和解决策略？
**A:** 当两个分支都修改了同一文件的相同区域时，Git无法自动决定保留哪个版本。

**冲突标记格式**:
```
<<<<<<< HEAD
当前分支的内容
=======
合并分支的内容
>>>>>>> feature-branch
```

**解决策略**:
1. **手动编辑**: 直接编辑冲突文件，保留需要的代码
2. **使用工具**: `git mergetool` 启动可视化合并工具
3. **选择保留**: `git checkout --ours/--theirs <file>` 选择保留一个版本
4. **重新合并**: `git merge --abort` 取消合并回到之前状态

### Q7: Git LFS（Large File Storage）的原理？
**A:** Git LFS用于管理大文件，避免二进制大文件污染Git仓库：
- 在仓库中存储指向大文件的文本指针（pointer file）
- 实际文件内容存储在远程LFS服务器
- 克隆时先下载指针，按需下载实际文件
- 减少仓库体积，加快clone速度

```bash
git lfs track "*.psd"           # 跟踪.psd文件
git lfs track "*.zip"           # 跟踪压缩包
git lfs ls-files                # 查看LFS文件
```

### Q8: Git Hooks的原理和常见用途？
**A:** Git Hooks是Git在特定事件发生时自动执行的脚本，存储在 `.git/hooks/` 目录中：
- **客户端Hook**: `pre-commit`（提交前检查）、`prepare-commit-msg`、`commit-msg`（提交信息检查）、`post-commit`、`pre-push`（推送前运行测试）
- **服务端Hook**: `pre-receive`、`update`、`post-receive`（触发CI/CD）

常见用途：
```bash
# .git/hooks/pre-commit
#!/bin/sh
# 运行代码格式检查
npm run lint
if [ $? -ne 0 ]; then
    echo "代码格式检查未通过，提交已阻止"
    exit 1
fi
```

### Q9: Submodule和Subtree的区别？
**A:**
| 特性 | Submodule | Subtree |
|------|-----------|---------|
| 管理方式 | 独立仓库引用 | 代码合并到主仓库 |
| 版本锁定 | 指向特定commit | 无专用指针 |
| 修改提交 | 需在子仓库和主仓库分别操作 | 直接在主仓库提交 |
| 克隆体验 | 需 `--recursive` 拉取子模块 | 一次克隆包含所有代码 |
| 适用场景 | 依赖第三方库 | 内部共享代码库 |

### Q10: git bisect如何调试问题？
**A:** `git bisect` 使用二分查找法快速定位引入Bug的提交：
```bash
git bisect start
git bisect bad           # 当前版本有Bug
git bisect good v1.0.0   # v1.0.0版本正常
# Git自动检出中间的提交
# 测试后标记 good 或 bad
git bisect good
git bisect bad
# ... 重复直到找到首个Bad提交
git bisect reset         # 结束bisect
```

---

## 三、实战场景题（6-10题）

### Q1: 如果误提交了大文件到Git仓库怎么处理？
**A:**
```bash
# 方案1：使用git rm --cached（如果刚提交）
git rm --cached largefile.zip
echo "largefile.zip" >> .gitignore
git add .gitignore
git commit -m "移除大文件并添加到gitignore"

# 方案2：使用git filter-branch（需要重写历史）
git filter-branch --tree-filter 'rm -f largefile.zip' HEAD

# 方案3：使用BFG Repo-Cleaner（推荐）
java -jar bfg.jar --delete-files largefile.zip my-repo.git
```

### Q2: 工作区开发到一半，需要切换到其他分支修改Bug，如何处理？
**A:**
```bash
# 方法1：git stash（推荐）
git stash -u             # 保存工作进度（-u包含新文件）
git checkout hotfix-branch
# 修复Bug...
git checkout feature-branch
git stash pop            # 恢复工作进度

# 方法2：git commit暂存
git add .
git commit -m "WIP: temporary commit"
git checkout hotfix-branch
# ...
git checkout feature-branch
git reset HEAD~1         # 撤销临时提交，保留修改

# 方法3：创建临时分支
git checkout -b temp-branch
git add .
git commit -m "temp"
git checkout feature
git merge temp-branch --squash
```

### Q3: 远程仓库冲突如何解决（多人协作场景）？
**A:**
```bash
# 场景：git pull时出现冲突
git pull origin main
# 提示：CONFLICT (content): Merge conflict in src/App.java

# 1. 查看冲突文件
git status

# 2. 手动解决冲突（编辑文件）
#   保留/修改需要的代码，删除冲突标记

# 3. 标记为已解决
git add src/App.java

# 4. 完成合并
git commit -m "解决合并冲突"

# 5. 推送
git push origin main
```

### Q4: 如何整理提交历史（合并多个commit）？
**A:**
```bash
# 使用rebase -i合并最近3个提交
git rebase -i HEAD~3

# 交互模式中的常用操作：
# pick    - 保留该提交
# squash  - 合并到上一个提交
# fixup   - 合并到上一个提交，丢弃提交信息
# reword  - 修改提交信息
# edit    - 修改该提交
# drop    - 删除该提交

# 示例：将commit B和C合并到A
# pick A 实现用户登录功能
# squash B 修复登录bug
# squash C 优化登录性能
```

### Q5：某次commit忘记push该怎么做？
**A:**
```bash
# 场景：本地有commit但尚未push
git log --oneline          # 确认提交存在
git push origin <branch>   # 直接推送即可

# 场景：需要修改已提交但未push的commit信息
git commit --amend -m "新的提交信息"

# 场景：需要添加遗漏的文件到上次commit
git add forgotten-file.txt
git commit --amend --no-edit  # 不修改提交信息
```

### Q6: 团队协作中分支命名规范如何制定？
**A:**
```bash
# 主分支
main / master          # 线上稳定版本

# 开发分支
develop / dev          # 日常开发集成分支

# 功能分支
feature/user-login     # 用户登录功能
feature/order-system   # 订单系统功能

# 修复分支
hotfix/1.2.1           # 紧急修复
bugfix/login-error     # Bug修复

# 发布分支
release/1.2.0          # 发布准备

# 版本标签
git tag v1.0.0         # 语义化版本号
git tag v1.2.0-rc1     # Release Candidate
```

### Q7: 如何恢复误删除的分支？
**A:**
```bash
# 场景1：知道最后commit的SHA
git branch recover-branch <commit-sha>

# 场景2：使用reflog查找
git reflog
# 输出: 1234567 HEAD@{2}: checkout: moving from deleted-branch to main
git branch recover-branch 1234567

# 场景3：从远程恢复（如果已推送）
git checkout -b deleted-branch origin/deleted-branch
```

### Q8: CI/CD中Git的最佳实践？
**A:**
```yaml
# GitHub Actions + Git流程
name: CI Pipeline
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0  # 获取完整历史用于lint检查

      - name: Check commit messages
        run: |
          # 检查提交信息格式
          git log --format=%s ${{ github.event.before }}..${{ github.sha }} \
            | while read line; do
              echo "Checking: $line"
              [[ "$line" =~ ^(feat|fix|docs|refactor|test|chore)(\(.+\))?:.*$ ]] \
                || { echo "Invalid commit message: $line"; exit 1; }
            done
```

---

## 四、手写代码/配置文件题（5-8题）

### Q1: 编写Git分支管理的完整流程脚本
**A:**
```bash
#!/bin/bash
# 团队Git分支协作流程

# 1. 初始化仓库并设置远程
git init project-name
cd project-name
git remote add origin git@github.com:team/project.git

# 2. 从远程拉取最新代码
git fetch origin
git checkout -b main origin/main

# 3. 创建开发分支
git checkout -b develop main
git push -u origin develop

# 4. 开发新功能
git checkout -b feature/user-login develop
# ... 开发代码 ...
git add .
git commit -m "feat: 实现用户登录功能"

# 5. 合并到开发分支
git checkout develop
git pull origin develop
git merge --no-ff feature/user-login -m "feat: 合并用户登录功能"
git push origin develop

# 6. 创建发布分支
git checkout -b release/1.0.0 develop
# ... 修复Bug、更新版本号 ...
git commit -m "chore: 准备发布1.0.0版本"

# 7. 合并到主分支并打标签
git checkout main
git merge --no-ff release/1.0.0 -m "release: 发布1.0.0版本"
git tag -a v1.0.0 -m "v1.0.0: 正式发布版本"
git push origin main --tags

# 8. 合并回develop分支
git checkout develop
git merge --no-ff release/1.0.0 -m "chore: 合并发布分支到开发分支"
git push origin develop

# 9. 删除已合并的分支
git branch -d feature/user-login
git branch -d release/1.0.0
git push origin --delete feature/user-login
```

### Q2: 编写.gitignore文件（Java项目标准）
**A:**
```
# Maven编译产物
target/
*.class
*.jar
*.war
*.ear

# Gradle编译产物
.gradle/
build/

# IDE配置
.idea/
*.iml
*.iws
*.ipr
.settings/
.project
.classpath
.vscode/
*.swp
*.swo

# OS文件
.DS_Store
Thumbs.db
Desktop.ini

# 日志文件
logs/
*.log

# 环境配置文件
.env
.env.local
.env.production
application-local.yml
application-dev.yml

# 临时文件
*.tmp
*.bak
*.swp

# 依赖目录（如使用前端构建）
node_modules/
vendor/

# 上传文件
uploads/
public/uploads/

# 测试报告
reports/
test-output/
coverage/

# Docker文件
.docker/

# 系统文件
*.pid
*.seed
*.pid.lock
```

### Q3: 编写成功解决merge冲突的完整过程
**A:**
```bash
# 1. 拉取远程代码时发生冲突
git pull origin main
# 输出：
# Auto-merging src/main/java/com/example/UserService.java
# CONFLICT (content): Merge conflict in UserService.java
# Automatic merge failed; fix conflicts and then commit the result.

# 2. 查看冲突状态
git status
# On branch main
# Your branch is up to date with 'origin/main'.
# You have unmerged paths.
#   (fix conflicts and run "git commit")
#   Unmerged paths:
#     both modified:   src/main/java/com/example/UserService.java

# 3. 查看冲突内容
cat src/main/java/com/example/UserService.java
# <<<<<<< HEAD
# public User getUser(Long id) {
#     return userRepository.findById(id).orElse(null);
# =======
# public User getUser(Long id) {
#     return userRepository.findById(id)
#         .orElseThrow(() -> new UserNotFoundException(id));
# >>>>>>> feature/error-handling

# 4. 手动解决冲突（保留需要的代码）
cat > src/main/java/com/example/UserService.java << 'EOF'
package com.example;

@Service
public class UserService {
    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
EOF

# 5. 标记为已解决
git add src/main/java/com/example/UserService.java

# 6. 完成合并提交
git commit -m "fix: 解决UserService.java合并冲突，使用orElseThrow方式"

# 7. 推送到远程
git push origin main
```

### Q4: 编写Git Flow工作流的完整配置
**A:**
```bash
#!/bin/bash
# Git Flow初始化配置

# 1. 安装git-flow（如果未安装）
# macOS: brew install git-flow
# Ubuntu: apt-get install git-flow
# Windows: git flow init

# 2. 初始化Git Flow
git flow init -d

# 以下为交互式初始化的默认分支配置：
#   Branch name for production releases: main
#   Branch name for "next release" development: develop
#   Feature branches prefix: feature/
#   Bugfix branches prefix: bugfix/
#   Release branches prefix: release/
#   Hotfix branches prefix: hotfix/
#   Support branches prefix: support/
#   Version tag prefix: v

# 3. 使用Git Flow开发新功能
git flow feature start user-profile
# 自动创建 feature/user-profile 分支，基于develop
# ... 开发代码 ...
git add .
git commit -m "feat: 实现用户资料功能"
git flow feature finish user-profile
# 自动合并回develop，删除feature分支

# 4. 创建发布版本
git flow release start 1.0.0
# 从develop分支创建 release/1.0.0
# ... 版本号更新、最终测试 ...
git add .
git commit -m "chore: 准备发布1.0.0"
git flow release finish 1.0.0
# 自动合并到main和develop，添加标签v1.0.0

# 5. 热修复生产问题
git flow hotfix start 1.0.1
# 从main创建 hotfix/1.0.1
# ... 修复紧急Bug ...
git add .
git commit -m "hotfix: 修复支付系统空指针异常"
git flow hotfix finish 1.0.1
# 自动合并到main和develop，添加标签v1.0.1
```

### Q5: 编写Git Hooks自动化脚本
**A:**
```bash
#!/bin/sh
# .git/hooks/pre-commit — 提交前自动检查

# 获取暂存区中的文件列表
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)
if [ -z "$STAGED_FILES" ]; then
    exit 0
fi

echo "Running pre-commit checks..."

# 1. 检查是否有调试代码
for FILE in $STAGED_FILES; do
    if [ -f "$FILE" ]; then
        # 检查console.log (JS)
        if grep -n "console\.log" "$FILE" 2>/dev/null; then
            echo "WARNING: $FILE 包含console.log"
            exit 1
        fi
        # 检查System.out.println (Java)
        if grep -n "System\.out\.println" "$FILE" 2>/dev/null; then
            echo "ERROR: $FILE 包含System.out.println，请使用日志框架"
            exit 1
        fi
        # 检查TODO标记
        if grep -n "TODO\|FIXME\|XXX" "$FILE" 2>/dev/null; then
            echo "WARNING: $FILE 包含待办事项(TODO/FIXME)"
        fi
    fi
done

# 2. 检查提交信息格式
COMMIT_MSG_FILE=$1
COMMIT_MSG=$(cat "$COMMIT_MSG_FILE")
# 规范格式: type(scope): message
# type: feat|fix|docs|style|refactor|test|chore
PATTERN="^(feat|fix|docs|style|refactor|test|chore)(\(.+\))?: .+"
if ! echo "$COMMIT_MSG" | grep -qE "$PATTERN"; then
    echo "ERROR: 提交信息格式不符合规范"
    echo "格式: type(scope): message"
    echo "示例: feat(user): 添加用户登录功能"
    exit 1
fi

# 3. 运行单元测试
# npm test || exit 1
# mvn test -q || exit 1

echo "Pre-commit checks passed!"
exit 0
```

```bash
#!/bin/sh
# .git/hooks/pre-push — 推送前运行测试
echo "Running tests before push..."
git stash -k  # 保留工作区，暂存未提交的修改
# mvn test || { git stash pop; exit 1; }
git stash pop
echo "Tests passed! Proceeding with push..."
exit 0
```

### Q6: 编写rebase操作完整流程（整理个人分支历史）
**A:**
```bash
#!/bin/bash
# 场景：feature分支基于develop开发，需要整理提交历史和合并最新代码

# 1. 在feature分支上开发了一段时间，有多个提交
git log --oneline
# abc1234 优化查询性能
# def5678 添加分页功能
# ghi9012 修复搜索Bug
# jkl3456 添加搜索功能
# mno7890 初始化项目

# 2. 合并最近的3个提交（squash合并）
git rebase -i HEAD~3
# 交互界面：
# pick    ghi9012 修复搜索Bug
# squash  def5678 添加分页功能
# squash  abc1234 优化查询性能
#
# 保存退出后，编辑合并提交信息：
# feat: 实现搜索分页功能

# 3. 变基到最新的develop分支（整理后）
# 先更新本地develop
git checkout develop
git pull origin develop
git checkout feature

# 将feature变基到develop最新提交上
git rebase develop
# 如有冲突：
# 1. 解决冲突，git add <file>
# 2. git rebase --continue
# 3. 或 git rebase --abort 取消

# 4. 变基完成，强制推送（如果已推送到远程）
git push --force-with-lease origin feature
# --force-with-lease 比 --force 更安全
# 它会检查远程分支是否被他人修改过

# 5. 检查整理后的历史
git log --oneline --graph --decorate --all
# * 1234abc (HEAD -> feature) feat: 实现搜索分页功能
# * 5678def (develop) chore: 更新依赖版本
# * 9012ghi 修复数据库连接超时
# * 3456jkl 添加用户注册功能
```

---

## 五、系统设计题（3-5题）

### Q1: 设计团队Git分支管理策略（中大型团队）
**A:** 推荐Git Flow变体方案：

```
main ──────●─────────●─────────●─────────●
            \       / \       / \       /
develop      ●─────●──●─────●──●─────●──●
              \         /  \         /
feature/       ●──●──●─   ●──●──●─
                        \         /
release/                 ●───────●
                        /         \
hotfix/               ●───────────●
```

**角色权限**:
| 角色 | 可操作分支 | 职责 |
|------|-----------|------|
| 开发者 | feature/* | 功能开发 |
| 技术负责人 | develop, release/* | 代码审查、发布 |
| 主程/架构师 | main | 审核发布，紧急修复 |
| CI/CD | 所有分支 | 自动构建、测试、部署 |

**合并规范**:
- feature分支以squash方式合并到develop
- develop到main使用 `--no-ff` 保留分支信息
- hotfix直接合并到main和develop
- 所有main提交必须有对应tag

### Q2: 设计代码审查（Code Review）流程
**A:**
```
开发者: 创建feature分支 → 开发 → 提交(小粒度)
    ↓
开发者: 创建Pull Request
    ├── PR标题: feat(user): 添加用户登录功能 #123
    ├── PR描述: 修改内容、影响范围、测试覆盖
    └── 关联Issue: Closes #42
    ↓
CI/CD: 自动检查
    ├── 代码格式检查 (ESLint/Checkstyle)
    ├── 单元测试 (Jest/Maven Test)
    ├── 代码覆盖率 (>80%)
    ├── 安全扫描 (SonarQube)
    └── 构建验证 (编译通过)
    ↓
PR分配: 至少2名Reviewer
    ├── Reviewer 1: 业务逻辑和功能正确性
    └── Reviewer 2: 代码质量和架构合理性
    ↓
Review反馈:
    ├── Approved: 通过
    ├── Changes Requested: 需修改
    └── Comment: 建议性意见
    ↓
合并策略: Squash Merge（保持main历史干净）
    ↓
自动部署到测试环境
    ↓
通知相关方
```

**Review CheckList**:
- [ ] 代码可读性（命名、注释、结构）
- [ ] 异常处理是否完整
- [ ] 是否有重复代码
- [ ] 是否有安全漏洞（SQL注入、XSS等）
- [ ] 测试是否覆盖关键路径
- [ ] API变更是否向后兼容
- [ ] 是否引入不必要的依赖

### Q3: 设计一套Git仓库管理规范（包含monorepo vs multi-repo）
**A:**
```
Monorepo方案：
project-root/
├── packages/
│   ├── service-a/        # 微服务A
│   ├── service-b/        # 微服务B
│   ├── library-common/   # 公共库
│   └── library-utils/    # 工具库
├── docs/                 # 文档
├── scripts/              # 构建脚本
└── .github/              # CI配置

管理规范：
1. COMMIT规范: Conventional Commits
   feat|fix|docs|refactor|test|chore(scope): description
   示例: feat(user-api): 添加用户列表分页查询

2. 分支保护规则:
   - main: 需要PR+2个Approval+CI通过
   - develop: 需要PR+1个Approval
   - 禁止直接push protected分支

3. 文件管理:
   - 大文件使用Git LFS
   - 配置文件使用模板 + sed替换
   - 密钥/密码使用CI Secrets
```

### Q4: 设计GitOps工作流（Git + CI/CD + K8s）
**A:**
```
GitOps架构：

                    +------------------+
                    |    Git仓库       |
                    |  (声明式配置)     |
                    +--------+---------+
                             |
              +--------------+--------------+
              |                             |
    +---------v----------+       +----------v---------+
    |   Application Repo |       |   Config Repo       |
    |   (源代码)          |       |   (K8s YAML)        |
    +---------+----------+       +----------+---------+
              |                             |
              v                             v
    +---------+----------+       +----------+---------+
    |   CI Pipeline       |       |   CD Pipeline      |
    |   (构建+测试+打包)   |       |   (自动同步)        |
    +---------+----------+       +----------+---------+
              |                             |
              v                             v
    +---------+----------+       +----------+---------+
    |   Docker Registry   |       |   Kubernetes       |
    |   (Harbor)           |       |   (声明式同步)     |
    +--------------------+       +--------------------+

流程：
1. 开发者在Application Repo提交代码
2. CI流水线构建镜像，推送到Harbor
3. CI更新Config Repo中的镜像版本号
4. CD工具（ArgoCD/Flux）检测Config Repo变更
5. 自动同步配置到Kubernetes集群
6. 返回同步状态给开发者
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点/问题 | 原因 | 解决方案 | 最佳实践 |
|-----------|------|----------|----------|
| 提交信息不规范 | 无统一格式约束 | 配置commit-msg hook校验 | 统一使用Conventional Commits格式 |
| 大文件误提交 | gitignore不完善 | 使用BFG/filter-branch清理历史 | 提交前检查，大文件使用LFS |
| merge冲突频繁 | 多人同时修改相同文件 | 增加团队沟通，拆分模块 | 使用rebase保持线性历史 |
| 丢失修改 | reset --hard误操作 | 立即用reflog恢复 | reset前确认完整stash备份 |
| 推送被拒绝 | 远程有未同步的提交 | `git pull --rebase` 再推送 | 推送前先fetch查看差异 |
| 分支管理混乱 | 缺少规范 | 采用Git Flow/GitHub Flow | 团队成员统一培训 |
| .gitignore失效 | 文件已被Git跟踪 | `git rm --cached <file>` 取消跟踪 | .gitignore在初始化时就配置 |
| SSH认证失败 | 密钥配置错误 | `ssh -T git@github.com` 测试 | 配置SSH config管理多密钥 |
| rebase丢失提交 | 强制推送覆盖历史 | `git reflog` 恢复 | 只在个人分支使用rebase |
| submodule更新不及时 | 子模块指向旧commit | `git submodule update --remote` | 定义子模块更新流程 |
| 权限遗漏 | 未配置保护分支 | 仓库设置branch protection | 禁止直接push main/develop |
| 文件权限变更 | 可执行位变更被记录 | `git config core.filemode false` | Windows开发推荐配置 |

---

## 七、面试回答模板（Top 5）

### 模板1: Git和SVN的区别
**回答框架：**
- **架构差异**: Git分布式 vs SVN集中式
- **核心优势**:
  - 离线提交，本地完整仓库
  - 分支操作轻量快速
  - 无单点故障
  - 安全可靠（每个克隆都是完整备份）
- **推荐场景**: 团队协作开发推荐Git，简单文档管理SVN可满足
- **代码对比**:
  ```bash
  # Git分支操作
  git checkout -b feature
  # SVN分支操作
  svn copy trunk branches/feature
  ```

### 模板2: git merge vs git rebase
**回答框架：**
- **merge**: 创建合并提交，保留完整历史分支结构，使用 `--no-ff` 可强制保留
- **rebase**: 线性化历史，使提交历史清晰整洁
- **黄金法则**: 公共分支用merge，个人分支用rebase
- **工作流示例**:
  ```bash
  # 合并公共分支
  git checkout main && git merge --no-ff feature
  
  # 整理个人分支
  git rebase -i HEAD~3
  git rebase main
  ```
- **注意**: 已push到远程的分支不要rebase，以免造成团队混乱

### 模板3: 解决合并冲突的完整步骤
**回答框架：**
1. 识别冲突（`git status` 查看冲突文件）
2. 手动编辑冲突文件（删除冲突标记 `<<<<<<<`, `=======`, `>>>>>>>`）
3. 标记已解决（`git add <file>`）
4. 完成合并（`git commit` 或 `git merge --continue`）
5. 推送到远程（`git push`）
- **关键**: 解决冲突时仔细阅读冲突上下文，与相关开发者沟通确保正确性

### 模板4: git reset vs git revert
**回答框架：**
- **reset**: 移动HEAD指针，可能会丢弃提交，不适合已推送的历史
  - `--soft`: 保留工作区和暂存区
  - `--mixed`: 保留工作区，重置暂存区
  - `--hard`: 全部丢弃
- **revert**: 创建新的提交来撤销修改，不改变历史，安全可靠
- **选择依据**: 本地未推送用reset，已推送用revert
- **示例**:
  ```bash
  # 本地回退
  git reset --soft HEAD~1
  
  # 已推送回退
  git revert HEAD --no-edit
  git push origin main
  ```

### 模板5: Git Flow vs GitHub Flow
**回答框架：**
- **Git Flow**: 分支丰富（main/develop/feature/release/hotfix），适合版本发布周期明确的项目
- **GitHub Flow**: 简洁（main + feature分支），每次合并自动部署，适合CI/CD成熟的项目
- **选择依据**:
  - 传统企业项目、有固定版本发布周期 → Git Flow
  - 互联网SaaS、快速迭代、持续部署 → GitHub Flow
  - 小团队快速开发 → Trunk-Based（主干开发）
- **趋势**: 现代DevOps更倾向于GitHub Flow或Trunk-Based，减少分支管理复杂度

---

## 八、快速查漏补缺Checklist

- [ ] Git三种状态（modified/staged/committed）和四大区域（工作区/暂存区/本地仓库/远程仓库）
- [ ] SVN vs Git对比（架构、离线、分支、安全）
- [ ] 全局配置（user.name、user.email、autocrlf）
- [ ] 基本操作（add、commit、push、pull、status、log）
- [ ] 分支操作（branch、checkout、merge、delete）
- [ ] 合并冲突解决流程
- [ ] 快进合并（Fast-Forward）和--no-ff
- [ ] 远程仓库操作（remote、push、pull、fetch、clone）
- [ ] git fetch vs git pull区别
- [ ] 撤销操作（restore、reset --soft/--mixed/--hard、revert）
- [ ] git stash保存和恢复工作进度
- [ ] git reflog恢复误操作
- [ ] git bisect二分查找Bug
- [ ] git hooks（pre-commit、pre-push、commit-msg）
- [ ] SSH免密配置（ssh-keygen、公钥配置）
- [ ] .gitignore编写规范
- [ ] Git LFS管理大文件
- [ ] Submodule和Subtree差异
- [ ] merge和rebase核心区别
- [ ] rebase -i交互式变基（pick/squash/fixup/reword）
- [ ] Git Flow规范（main/develop/feature/release/hotfix）
- [ ] GitHub Flow和Trunk-Based
- [ ] 代码审查（PR/MR流程和CheckList）
- [ ] 提交信息规范（Conventional Commits）
- [ ] 标签管理（git tag和版本发布）
- [ ] 分支保护规则设置
- [ ] CI/CD中Git集成实践
- [ ] Monorepo vs Multi-repo选择
- [ ] GitOps（Git + CI/CD + K8s）原理
- [ ] git rm --cached 清除已被跟踪的文件
- [ ] git describe 版本描述
- [ ] git shortlog 统计贡献
- [ ] git cherry-pick 挑选特定提交
- [ ] git archive 导出代码归档
