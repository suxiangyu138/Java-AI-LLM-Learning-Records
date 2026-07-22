# Git快速入门 面试宝典
> 基于课程大纲全面覆盖面试高频考点，适合零基础入门到面试冲刺

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

### Q1: Git 是什么？有什么特点？
Git 是一个**分布式版本控制系统**，由 Linus Torvalds 为管理 Linux 内核开发而创建。核心特点包括：
- **分布式架构**：每个开发者本地都有完整仓库副本，断网也可工作
- **分支轻量**：分支本质是指向 commit 的指针，创建切换成本极低
- **数据完整性**：所有对象通过 SHA-1 哈希寻址，内容改则哈希变
- **暂存区机制**：工作区 → 暂存区 → 本地仓库，分阶段提交

### Q2: Git 与 SVN 的核心区别？
| 维度 | Git (分布式) | SVN (集中式) |
|------|-------------|-------------|
| 架构 | 每个节点有完整仓库 | 只有中央服务器有完整版本库 |
| 联网要求 | 本地操作无需联网 | 大部分操作需联网 |
| 分支 | 轻量指针，创建/合并成本低 | 目录拷贝，分支操作重 |
| 速度 | 本地操作极快 | 依赖网络，大文件慢 |
| 存储方式 | 快照存储 | 差异存储 |
| 社区生态 | 开源主导，GitHub/GitLab | 企业遗留系统 |

### Q3: Git 的三大工作区域是什么？
1. **Working Directory（工作区）**：当前编辑的文件目录，即 `git init` 所在目录（不含 `.git` 目录）
2. **Staging Area / Index（暂存区）**：位于 `.git/index`，记录下次要提交的文件快照
3. **Repository（本地仓库）**：位于 `.git/objects`，保存所有提交历史与对象

> 扩展为四区域时还包括 **Remote Repository（远程仓库）**，即托管在 GitHub/Gitee 等服务器上的仓库。

### Q4: 文件在 Git 中的四种状态？
- **Untracked（未跟踪）**：文件在工作区，未被 Git 管理
- **Unmodified（未修改）**：文件已入库，自上次提交未改动
- **Modified（已修改）**：文件已修改但未放入暂存区
- **Staged（已暂存）**：修改已放入暂存区，等待下一次提交

状态流转：Untracked → `git add` → Staged → `git commit` → Unmodified → 编辑 → Modified → `git add` → Staged

### Q5: `git fetch` 与 `git pull` 的区别？
- `git fetch`：仅从远程下载最新数据到本地仓库的远程跟踪分支（origin/main），**不合并**到当前工作分支
- `git pull`：`git fetch` + `git merge FETCH_HEAD` 的组合，远程变更直接合并到当前分支

> 建议：先 `git fetch` 查看变更，确认后再 `git merge`，避免意外合并导致冲突。

### Q6: `git merge` 与 `git rebase` 的区别？
| 维度 | git merge | git rebase |
|------|-----------|------------|
| 提交历史 | 保留分支合并记录，有 merge commit | 重写历史，线性提交 |
| 可读性 | 保留真实协作历史 | 历史清晰整洁 |
| 冲突处理 | 一次解决所有冲突 | 每次 commit 依次解决 |
| 安全性 | 不修改现有 commit | 重写 commit 哈希 |
| 适用场景 | 公共分支合并 | 个人分支整理 |

### Q7: Git 标签（Tag）的作用？
标签用于标记特定提交，通常用于版本发布（v1.0、v2.0）。
- **轻量标签（Lightweight）**：仅是指向某 commit 的指针
- **附注标签（Annotated）**：存储完整信息（作者、日期、消息），可用 GPG 签名

```bash
# 创建标签
git tag v1.0.0                          # 轻量标签
git tag -a v1.0.0 -m "Release 1.0.0"    # 附注标签

# 推送标签到远程
git push origin v1.0.0                   # 推送单个标签
git push origin --tags                   # 推送所有标签

# 删除标签
git tag -d v1.0.0                        # 删除本地
git push origin --delete v1.0.0          # 删除远程
```

### Q8: `git reset` 的三种模式？
| 模式 | 参数 | HEAD | 暂存区 | 工作区 |
|------|------|------|--------|--------|
| Soft | `--soft` | 移动 | 不变 | 不变 |
| Mixed | `--mixed`（默认） | 移动 | 重置 | 不变 |
| Hard | `--hard` | 移动 | 重置 | 重置 |

```bash
git reset --soft HEAD~1     # 撤销 commit，保留代码修改和暂存
git reset --mixed HEAD~1    # 撤销 commit 和暂存，保留代码修改
git reset --hard HEAD~1     # 彻底回到上一个版本，丢弃所有修改（慎用）
```

### Q9: `git revert` 与 `git reset` 的区别？
- `git reset`：移动 HEAD 指针，**修改提交历史**（`--hard` 会丢失代码）
- `git revert`：创建**新提交**来反向撤销指定提交，**不修改历史**，适合公共分支

```bash
git revert HEAD          # 撤销最近一次提交，生成新的撤销提交
git revert <commit-hash> # 撤销某次历史提交
```

### Q10: `git stash` 的常见用法？
将未提交的修改临时保存，使工作区恢复干净。

```bash
git stash                # 暂存当前修改
git stash list           # 查看暂存列表
git stash pop            # 恢复最近一次暂存并删除
git stash apply stash@{0} # 恢复指定暂存但不删除
git stash drop stash@{0} # 删除指定暂存
git stash -u             # 包括未跟踪文件（-u/--include-untracked）
```

### Q11: `git rebase -i` 交互式变基可以做什么？
交互式变基用于整理本地提交历史，支持以下操作：
- `pick`：使用该提交
- `reword`：修改提交信息
- `edit`：修改提交内容
- `squash`：合并到上一个提交
- `fixup`：合并但丢弃提交信息
- `drop`：删除提交

```bash
git rebase -i HEAD~3     # 整理最近3个提交
```

### Q12: `git log` 常用选项？
```bash
git log                  # 完整日志
git log --oneline        # 单行显示
git log --graph          # 分支图显示
git log -p               # 显示差异内容
git log --oneline --graph --all --decorate  # 最常用组合
```

### Q13: 什么是 `.gitignore`？
`.gitignore` 文件指定哪些文件/目录不被 Git 跟踪，规则包括：
- `*.log` — 匹配所有 .log 文件
- `/target/` — 匹配根目录下的 target 目录
- `!important.log` — 重新包含被忽略的文件
- `build/` — 匹配任意目录下的 build 目录

### Q14: 什么是 detached HEAD 状态？
HEAD 指向某个具体 commit 而非分支名时即为 detached HEAD。此时做出的修改如果创建新分支前切换分支会被丢弃。

```bash
git checkout <commit-hash>  # 进入 detached HEAD
git switch -c new-branch    # 基于当前 commit 创建分支保留修改
```

### Q15: 撤销操作的常见场景？
| 场景 | 操作 |
|------|------|
| 工作区修改未暂存 | `git checkout -- <file>` 或 `git restore <file>` |
| 已暂存未提交 | `git reset HEAD <file>` 或 `git restore --staged <file>` |
| 已提交未推送 | `git reset --soft HEAD~1` |
| 已推送至远程 | `git revert <commit>` |
| 误删分支 | `git reflog` 找到 commit 后 `git branch <name> <hash>` |

---

## 二、深度原理剖析（8-12题）

### Q1: Git 对象模型详解
Git 是一个**内容寻址文件系统**，核心有四种对象：

| 对象类型 | 存储内容 | 唯一标识 | 命令示例 |
|---------|---------|---------|---------|
| **Blob** | 文件内容（二进制） | SHA-1 哈希 | `git hash-object <file>` |
| **Tree** | 目录结构（文件名 + 权限 + blob/tree引用） | SHA-1 哈希 | `git write-tree` |
| **Commit** | 提交快照（tree + parent + author + message） | SHA-1 哈希 | `git commit-tree` |
| **Tag** | 标签引用（commit + 标签信息） | SHA-1 哈希 | `git tag -a` |

```text
Commit (40位SHA-1)
  ├── tree (目录快照)
  │   ├── blob "README.md"
  │   ├── blob "pom.xml"
  │   └── tree "src/"
  │       ├── blob "Main.java"
  │       └── blob "Utils.java"
  ├── parent (上一个 Commit)
  ├── author
  ├── committer
  └── message
```

### Q2: Git 分支的本质是什么？
分支本质上是一个**可移动的指针**，指向某个 commit 对象。
- `.git/refs/heads/main` 文件内容即为该分支最新 commit 的 SHA-1
- 创建分支 = 创建指针（几乎零开销）
- 切换分支 = 更新 HEAD 指向 + 更新工作区文件

```bash
# 查看分支指向
cat .git/refs/heads/main
# 输出: a1b2c3d4e5f6...

# 查看 HEAD 指向
cat .git/HEAD
# 输出: ref: refs/heads/main
```

### Q3: Git 的三种合并策略
| 策略 | 触发条件 | 特点 |
|------|---------|------|
| **Fast-Forward** | 目标分支是当前分支的直接后继 | 不创建合并提交，线性推进 |
| **Three-Way Merge** | 两个分支都有新的独立提交 | 创建 merge commit，保留分叉历史 |
| **Squash Merge** | 显式指定 `--squash` | 将分支所有变更压缩为一个提交 |

```bash
git merge feature                # 默认模式，尽量 fast-forward
git merge --no-ff feature        # 强制创建 merge commit
git merge --squash feature       # squash 合并
git commit -m "Squash feature"   # squash 后需手动提交
```

### Q4: Fast-Forward 合并详解
当当前分支相对于目标分支没有分叉时，Git 直接移动指针完成合并。

```text
合并前：        合并后 (Fast-Forward)：
A---B---C       A---B---C---D---E
         \           (直接推进)
          D---E
```

禁用 Fast-Forward 保留分支拓扑：
```bash
git merge --no-ff feature
# 结果：A---B---C---F (merge commit)
#              \   /
#               D-E
```

### Q5: 冲突产生的本质原因
当两个分支对**同一文件的同一行**做了不同修改，Git 无法自动决定保留哪个版本。

冲突标记示例：
```text
<<<<<<< HEAD
当前分支的代码
=======
合并进来的分支的代码
>>>>>>> feature-branch
```

解决步骤：
1. 手动编辑冲突文件，保留正确版本
2. `git add <file>` 标记为已解决
3. `git commit` 完成合并

### Q6: `git reflog` 的底层原理
Reflog 记录 HEAD 和分支引用的**所有历史变动**，包括被撤销或删除的提交，是以防数据丢失的最后一道防线。

```bash
git reflog
# 输出示例：
# a1b2c3d (HEAD -> main) HEAD@{0}: commit: fix bug
# e5f6g7h HEAD@{1}: reset: moving to HEAD~1
# i9j0k1l HEAD@{2}: commit: add feature

git reset --hard HEAD@{1}  # 恢复到 reflog 中的记录
```

> Reflog 是**本地**的，不会被推送到远程仓库，默认 90 天后清理。

### Q7: Git Hooks 钩子机制
钩子是 Git 在特定事件时触发的脚本，位于 `.git/hooks/` 目录。

| 钩子名称 | 触发时机 | 常见用途 |
|---------|---------|---------|
| `pre-commit` | `git commit` 前 | 代码格式检查、静态分析 |
| `commit-msg` | 提交信息编辑后 | 检查提交信息格式 |
| `pre-push` | `git push` 前 | 运行测试、防止推送至保护分支 |
| `post-merge` | `git merge` 后 | 自动更新依赖 |

### Q8: Git LFS 原理
Git Large File Storage 用**指针文件**替换大文件，实际大文件存储在远程 LFS 服务器。

```text
Git 仓库存储：      LFS 服务器存储：
pointer.txt         大文件.zip
version https://git-lfs.github.com/spec/v1
oid sha256:abc123...
size 104857600
```

```bash
git lfs track "*.zip"    # 跟踪 .zip 文件
git lfs track "*.psd"    # 跟踪 Photoshop 文件
```

---

## 三、实战场景题（6-10题）

### Q1: 本地提交后发现提交信息写错了，如何修改？
```bash
# 1. 最近一次提交信息
git commit --amend -m "正确的提交信息"

# 2. 更早的提交信息（交互式变基）
git rebase -i HEAD~3
# 将需要修改的提交前的 pick 改为 reword，保存后修改信息
```

### Q2: 不小心将大文件提交到了仓库，如何彻底删除？
```bash
# 1. 添加 .gitignore 防止再次提交
echo "largefile.zip" >> .gitignore
git add .gitignore && git commit -m "Add .gitignore"

# 2. 从所有历史中移除（使用 git filter-branch 或 BFG）
git filter-branch --tree-filter 'rm -f largefile.zip' HEAD
# 或使用 BFG Repo-Cleaner: java -jar bfg.jar --delete-files largefile.zip

# 3. 强制推送清理后的历史（需要协调团队）
git push origin --force

# 4. 回收空间
git reflog expire --expire=now --all
git gc --aggressive --prune=now
```

### Q3: 开发的代码应该提交到 dev 分支，却提交到了 main 分支，如何处理？
```bash
# 场景1：尚未推送
git branch dev                    # 在 main 上创建 dev 分支（包含提交）
git reset --hard HEAD~1           # main 分支回退
git checkout dev                  # 切换到 dev 继续开发

# 场景2：已推送至远程
git checkout main
git revert HEAD                   # main 上撤销提交
git checkout dev
git cherry-pick <commit-hash>     # 将提交复制到 dev
git push origin dev
```

### Q4: 合并时产生冲突，如何解决？
```bash
# 1. 查看冲突状态
git status                         # 查看冲突文件列表

# 2. 手动编辑冲突文件
# 找到冲突标记 <<<<<<< HEAD / ======= / >>>>>>> branch
# 保留需要的代码，删除冲突标记

# 3. 标记为已解决
git add <resolved-file>

# 4. 完成合并
git commit                         # 使用自动生成的合并信息

# 5. 如果冲突太复杂想中止合并
git merge --abort
```

### Q5: 某次 commit 导致了 bug，如何快速定位？
```bash
# 使用 git bisect 二分查找
git bisect start
git bisect bad                     # 当前 commit 是坏的
git bisect good v1.0               # 标记某个已知正常版本

# Git 会二分切换到中间 commit，测试后标记
git bisect good                    # 或 git bisect bad

# 找到问题 commit 后退出
git bisect reset
```

### Q6: 团队协作中，push 前发现有冲突，标准流程是什么？
```bash
# 1. 先拉取远程最新代码（优先用 rebase 保持历史整洁）
git fetch origin
git rebase origin/main

# 2. 解决 rebase 过程中的冲突
# 编辑冲突文件 → git add → git rebase --continue

# 3. 推送本地代码
git push origin feature/my-feature
```

### Q7: 误删了未推送的分支，如何恢复？
```bash
git reflog                         # 查找被删除分支最后指向的 commit
# 输出: abc1234 HEAD@{5}: branch: deleted feature-branch

git checkout -b feature-branch abc1234  # 基于 commit 重建分支
```

---

## 四、手写代码/配置文件题（5-8题）

### Q1: 完整的 Git 全局配置命令
```bash
# 配置用户信息
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 配置默认分支名
git config --global init.defaultBranch main

# 配置别名
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.lg "log --oneline --graph --all --decorate"

# 查看配置
git config --global --list
git config --global user.name
```

### Q2: 本地仓库的完整初始化流程
```bash
# 1. 创建项目目录
mkdir my-project
cd my-project

# 2. 初始化本地仓库
git init                        # 创建 .git 目录

# 3. 创建 .gitignore
cat > .gitignore << EOF
target/
*.class
*.jar
*.log
.idea/
*.iml
EOF

# 4. 添加并提交
git add .
git commit -m "Initial commit"

# 5. 关联远程仓库
git remote add origin https://github.com/user/my-project.git

# 6. 推送
git branch -M main
git push -u origin main
```

### Q3: 分支操作的标准流程
```bash
# 创建并切换到新分支
git checkout -b feature/login

# 查看分支
git branch                    # 本地分支列表
git branch -a                 # 所有分支（含远程）
git branch -r                 # 远程分支

# 在 feature 分支上开发
echo "login code" > login.java
git add login.java
git commit -m "Add login feature"

# 切回 main 合并
git checkout main
git merge feature/login

# 删除分支
git branch -d feature/login               # 本地分支
git push origin --delete feature/login    # 远程分支
```

### Q4: 远程仓库操作命令序列
```bash
# 1. 添加远程仓库
git remote add origin https://github.com/user/repo.git

# 2. 查看远程仓库
git remote -v                     # 查看所有远程仓库 URL

# 3. 推送
git push origin main              # 推送到远程 main 分支
git push -u origin main           # -u 建立跟踪关系，后续可直接 git push

# 4. 克隆
git clone https://github.com/user/repo.git

# 5. 拉取
git pull origin main              # fetch + merge

# 6. 抓取
git fetch origin                  # 下载远程数据但不合并
git log origin/main               # 查看远程分支
git merge origin/main             # 手动合并
```

### Q5: IDEA 集成 Git 操作
```text
一、配置 Git
Settings → Version Control → Git → Path to Git executable → 选择 git.exe
Settings → Version Control → GitHub/Gitee → 添加账号

二、创建本地仓库
VCS → Import into Version Control → Create Git Repository

三、提交代码
项目文件右键 → Git → Commit（或 Ctrl+K）
选择要提交的文件 → 填写 Commit Message → Commit

四、推送
VCS → Git → Push（或 Ctrl+Shift+K）

五、拉取
VCS → Git → Pull（或 Ctrl+T）

六、分支操作
右下角 Git 分支图标 → New Branch / Checkout / Merge / Rebase

七、解决冲突
更新时冲突 → 弹出冲突解决窗口 → Accept Yours / Accept Theirs / Merge
Merge 窗口：左侧本地、右侧远程、中间合并结果 → 手动编辑 → Apply
```

### Q6: `.gitignore` 常用配置
```gitignore
# Java 项目
*.class
*.jar
*.war
target/
!.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
.settings/
.project
.classpath

# OS
.DS_Store
Thumbs.db

# 构建产物
build/
dist/
out/
*.log

# 环境变量
.env
.env.local
*.env.*

# 依赖
node_modules/
vendor/
```

### Q7: 标签操作命令序列
```bash
# 创建附注标签
git tag -a v1.0.0 -m "正式发布 v1.0.0"

# 创建轻量标签
git tag v1.0.0-rc1

# 指定 commit 打标签
git tag -a v0.9.0 <commit-hash> -m "Beta 版本"

# 查看标签
git tag                       # 列表
git tag -l "v1.*"             # 通配符过滤
git show v1.0.0               # 标签详情

# 推送标签到远程
git push origin v1.0.0
git push origin --tags

# 切换到标签（detached HEAD 状态）
git checkout v1.0.0
```

---

## 五、系统设计题（3-5题）

### Q1: 如何设计一个适合 20 人前后端团队的分支策略？
采用 **Git Flow** 变体，适配前后端并行开发：

```text
main ──── v1.0 ── v1.1 ── v2.0 ──
          \         \       \
release ── R1 ── R2 ── R3
         /       /       /
develop ──── D1 ──── D2 ──── D3
          \  \     \  \     \
feature ── F1  F2  F3  F4  F5  F6
```

| 分支 | 命名规范 | 权限 | 用途 |
|------|---------|------|------|
| main | main | 受保护，仅管理员合并 | 生产发布 |
| develop | dev | 受保护 | 集成开发 |
| feature | feature/xxx | 开发者 | 功能开发 |
| release | release/x.x | 受保护 | 预发布测试 |
| hotfix | hotfix/x.x | 管理员 | 紧急修复 |

**保护规则**：main 和 dev 分支禁止直接 push，必须通过 PR/MR 合入，且需要至少 1 人 Code Review。

### Q2: 如何实现 monorepo 策略？
使用 **Git Subtree** 或 **Git Submodule** 管理多项目共用仓库。

```bash
# 方案 A：Git Subtree（推荐，历史完整保留）
git remote add sub-project https://github.com/org/sub-project.git
git subtree add --prefix=sub-project sub-project main --squash
git subtree pull --prefix=sub-project sub-project main

# 方案 B：Git Submodule（独立的仓库引用，需额外步骤）
git submodule add https://github.com/org/sub-project.git sub-project
git submodule update --init --recursive    # 克隆子模块
```

比较：
| 维度 | Subtree | Submodule |
|------|---------|-----------|
| 历史完整性 | 完整保留 | 指针引用 |
| 操作复杂度 | 简单 | 需额外命令 |
| 跨项目修改 | 方便 | 需分别在子仓库操作 |

### Q3: 如何将 CI/CD 集成到 Git 工作流？
```yaml
# .github/workflows/ci.yml 示例
name: CI Pipeline
on:
  push:
    branches: [dev, main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build with Maven
        run: mvn clean package
      - name: Run tests
        run: mvn test
      - name: Run linter
        run: mvn checkstyle:check
```

### Q4: Gerrit 代码审查工作流设计
Gerrit 通过 **patch set** 机制实现严格的代码审查：

```text
开发者 ──> 推送至 refs/for/main ──> Gerrit 创建 Change
                                          │
                                    ┌─────┴─────┐
                                    │ Code Review │
                                    │ CI 验证     │
                                    └─────┬─────┘
                                          │
                              Verified +1, Code-Review +2
                                          │
                                    提交变更（Submit）
                                          │
                                    ┌─────┴─────┐
                                    │ 合并到 main │
                                    └───────────┘
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点/问题 | 原因 | 解决方案 | 最佳实践 |
|-----------|------|----------|----------|
| `git push` 被拒绝 | 远程有本地没有的提交 | `git pull --rebase` 后重新推送 | 每次 push 前先 pull/pull --rebase |
| `git reset --hard` 丢失代码 | 强制重置工作区，未备份 | 立即用 `git reflog` 查找原 commit | 线上环境慎用 `--hard`；用 `git stash` 暂存 |
| 误提交到 main 分支 | 分支混乱 | `git cherry-pick` 到目标分支后 `git revert` | 创建一个新分支再删除错误的提交 |
| 大文件导致 .git 过大 | 不小心提交了二进制大文件 | `git filter-branch` 或 BFG 清理 | 使用 `.gitignore` + Git LFS |
| commit 信息不清晰 | 匆忙提交仅写 "fix bug" | `git commit --amend` 或 `git rebase -i` | 规范：`type(scope): subject` |
| 冲突反复出现 | 多人修改同一区域代码 | 沟通协调 + 定期 rebase 同步 | 拆模块细化，减少耦合 |
| push 到错误远程仓库 | 配置了多个 remote | `git remote set-url` 修正 | 明确 remote 命名：origin/upstream |
| `git pull` 意外合并产生冲突 | pull 自动 merge | 先用 `git fetch` 查看，再选择 merge/rebase | 养成先 fetch 再 merge 的习惯 |

---

## 七、面试回答模板（Top 5）

### 模板一：Git 和 SVN 的区别
> Git 是分布式版本控制系统，SVN 是集中式版本控制系统。主要区别有三点：
> **架构上**，Git 每个开发者本地都有完整的仓库副本，断网也能提交查看历史；SVN 必须联网连接到中央服务器才能操作。
> **分支上**，Git 分支只是一个 41 字节的指针文件，创建和切换成本极低；SVN 分支是目录拷贝，耗时且占用空间大。
> **性能上**，Git 大多数操作是本地完成，速度极快；SVN 依赖网络，大仓库慢。目前 Git 已成为业界主流。回答后可补充：Git 通过 SHA-1 哈希保证数据完整性，SVN 的提交 ID 是递增数字。

### 模板二：merge 和 rebase 的选择
> merge 和 rebase 都可以整合分支，但理念不同。
> **merge** 保留真实的合并历史，会产生一个 merge commit，反映实际协作过程，适合公共分支（如将 feature 合并到 main）。
> **rebase** 将提交"重放"到目标分支上，得到线性干净的提交历史，适合个人分支整理（如 feature 分支同步 main 的最新代码）。
> 我的原则是：公共分支用 merge --no-ff，个人分支用 rebase。一条铁则：**不要对已推送的公共分支做 rebase**，因为会重写历史导致团队混乱。

### 模板三：git reset 和 git revert 的区别
> 两者都能撤销变更，但适用场景不同。
> **git reset** 移动 HEAD 指针，会修改提交历史，`--hard` 模式会彻底删除代码。适合**本地**撤销还未推送的提交。
> **git revert** 通过创建新的反向提交来撤销指定提交，不修改历史，适合已经推送的公共分支。
> 打个比方：reset 是回到过去改写历史，revert 是保留历史记录但写一个"我对之前那个决定后悔了"的新记录。线上生产环境一律用 git revert。

### 模板四：冲突解决步骤
> 冲突是 Git 合并在遇到对同一位置的冲突修改时无法自动决策的情况。我的解决步骤是：
> 1. 执行 `git status` 或 `git diff` 查看冲突文件列表；
> 2. 打开冲突文件，搜索 `<<<<<<<`、`=======`、`>>>>>>>` 标记，手动编辑保留正确代码并删除标记；
> 3. `git add` 标记为已解决的状态；
> 4. `git commit` 提交合并结果（或 `git rebase --continue`）；
> 5. 如果情况复杂无法解决，用 `git merge --abort` 回到合并前状态重新评估。沟通也重要，冲突时我通常会和相关开发者沟通确认最终方案。

### 模板五：Git Flow 和 GitHub Flow
> **Git Flow** 分支模型包含 main、develop、feature、release、hotfix 五类分支，每个分支有明确的职责和生命周期。优点是结构清晰，适合有计划的大型发布项目；缺点是分支过多，复杂。适用场景：企业级项目，有固定版本发布周期。
> **GitHub Flow** 只有 main 分支和 feature 分支，基于 PR 的简化模型，任何修改都通过 PR 审查合并到 main 后立即部署。优点是简单灵活，适合持续部署；缺点是缺少 release 隔离，大型项目不太适用。适用场景：Web 应用，快速迭代的小团队。
> 我所在的项目使用基于 Git Flow 的变体，保留 main、dev、feature、hotfix，省去 release 分支，用 tag 管理版本。

---

## 八、快速查漏补缺 Checklist

- [ ] 能说出 Git 和 SVN 的 3 个核心区别
- [ ] 能画出 Git 四大区域的流转图（工作区 → 暂存区 → 本地仓库 → 远程仓库）
- [ ] 能默写 `git config --global user.name/email` 配置
- [ ] 能手写完整的 `git init` → `git remote add` → `git push` 流程
- [ ] 能区分 `git fetch` 和 `git pull`，并说出推荐用法
- [ ] 能区分 `git merge` 和 `git rebase` 的适用场景
- [ ] 能说出 `git reset` 三种模式对 HEAD/暂存区/工作区的影响
- [ ] 能写出 `git revert` 撤销已推送提交的命令
- [ ] 能手写解决冲突的完整步骤
- [ ] 能说出 Fast-Forward 合并的条件和特点
- [ ] 能使用 `git stash` 临时保存工作区修改
- [ ] 能使用 `git reflog` 恢复误删的提交/分支
- [ ] 能配置 `.gitignore` 排除不必要的文件
- [ ] 能说出 Git 四种对象的类型和用途
- [ ] 能使用 `git branch` 进行分支创建、切换、删除
- [ ] 能使用 `git tag` 标记版本发布
- [ ] 能配置和使用 SSH 密钥免密推送
- [ ] 能在 IDEA 中完成 Git 提交、推送、拉取、冲突解决
- [ ] 能描述 Git Flow 和 GitHub Flow 的区别
- [ ] 能写出 CI/CD 中 Git 集成的基本配置
