# 17-Git应用入门
> 🎯 Git是Java后端开发的版本控制中枢与协作底座，本文从零开始构建完整的Git认知体系：分布式 vs 集中式的本质差异、三态工作流模型、企业级配置规范、提交语义化约定，以及一套完整的SpringBoot项目实战演练

## 目录

1. [Git概述：分布式版本控制系统](#1-git概述分布式版本控制系统)
   - 1.1 [什么是Git](#11-什么是git)
   - 1.2 [分布式 vs 集中式（SVN）](#12-分布式-vs-集中式svn)
   - 1.3 [Git的核心优势](#13-git的核心优势)
2. [安装与首次配置](#2-安装与首次配置)
   - 2.1 [各平台安装方式](#21-各平台安装方式)
   - 2.2 [用户身份配置](#22-用户身份配置)
   - 2.3 [编辑器与行尾配置](#23-编辑器与行尾配置)
   - 2.4 [别名配置](#24-别名配置)
   - 2.5 [验证配置](#25-验证配置)
3. [.gitignore：Java项目忽略规则](#3-gitignorejava项目忽略规则)
   - 3.1 [完整模板](#31-完整模板)
   - 3.2 [忽略规则原理](#32-忽略规则原理)
   - 3.3 [已追踪文件的忽略处理](#33-已追踪文件的忽略处理)
4. [Git的三种状态与三态工作流](#4-git的三种状态与三态工作流)
   - 4.1 [三态模型](#41-三态模型)
   - 4.2 [三态流转图](#42-三态流转图)
5. [基础工作流](#5-基础工作流)
   - 5.1 [初始化仓库](#51-初始化仓库)
   - 5.2 [克隆远程仓库](#52-克隆远程仓库)
   - 5.3 [查看状态](#53-查看状态)
   - 5.4 [添加与提交](#54-添加与提交)
   - 5.5 [查看差异](#55-查看差异)
6. [Commit最佳实践](#6-commit最佳实践)
   - 6.1 [Conventional Commits约定](#61-conventional-commits约定)
   - 6.2 [原子化提交](#62-原子化提交)
   - 6.3 [好的Commit Message怎么写](#63-好的commit-message怎么写)
7. [Git概念深入](#7-git概念深入)
   - 7.1 [SHA-1哈希](#71-sha-1哈希)
   - 7.2 [HEAD指针](#72-head指针)
   - 7.3 [分支本质是指针](#73-分支本质是指针)
8. [查看历史](#8-查看历史)
   - 8.1 [git log实用选项](#81-git-log实用选项)
   - 8.2 [搜索提交内容](#82-搜索提交内容)
9. [撤销操作](#9-撤销操作)
   - 9.1 [工作区撤销](#91-工作区撤销)
   - 9.2 [暂存区撤销](#92-暂存区撤销)
   - 9.3 [git reset三种模式对比](#93-git-reset三种模式对比)
   - 9.4 [restore vs reset vs revert](#94-restore-vs-reset-vs-revert)
10. [完整实战：SpringBoot项目初始化与版本管理](#10-完整实战springboot项目初始化与版本管理)

---

## 1. Git概述：分布式版本控制系统

### 1.1 什么是Git

Git是目前世界上最先进的**分布式版本控制系统**（DVCS），由Linus Torvalds于2005年为管理Linux内核开发而创建。它记录文件内容的每一次变化，支持任意规模的协作开发，是Java后端、微服务、DevOps生态的事实标准工具。

### 1.2 分布式 vs 集中式（SVN）

| 维度 | Git（分布式） | SVN（集中式） |
|------|-------------|-------------|
| **仓库位置** | 每个开发者本地有完整仓库 | 仅中央服务器有完整仓库 |
| **离线工作** | 完全支持，提交/日志/对比均离线 | 不支持，几乎全部操作需联网 |
| **分支成本** | O(1)，创建即指针，切换秒级 | O(n)，创建即复制整个目录，成本高 |
| **性能** | 本地操作，毫秒级响应 | 远程操作，受网络延迟影响 |
| **安全性** | 每个节点都是完整备份 | 中央服务器是单点故障 |
| **分支模型** | 轻量分支，支持任意分支策略 | 目录级分支，路径开销大 |
| **存储方式** | 内容寻址（基于SHA-1的键值存储） | 增量存储（基于文件变更列表） |

> 💡 **核心区别一句话**：SVN是一份代码多人看，Git是每人一份完整代码再同步。离线可提交、分支即指针、仓库即备份，是Git碾压SVN的三个根本优势。

### 1.3 Git的核心优势

- **极致的分支与合并能力**：分支创建和切换成本极低，合并算法（递归/正交/重定向）成熟稳定
- **数据完整性保障**：通过SHA-1哈希校验，任何文件损坏或被篡改都能被立即发现
- **暂存区设计**：工作区→暂存区→版本库的三态模型，提交前可精细控制每次提交的文件颗粒度
- **社区生态**：GitHub/GitLab/Gitee等平台构建了完整的开源协作生态

> 🎯 **定位**：Git是Java后端工程师的第二个IDE——不会Git，等于不会开发。

---

## 2. 安装与首次配置

### 2.1 各平台安装方式

| 平台 | 安装命令 / 方式 |
|------|----------------|
| **Windows** | 从 https://git-scm.com 下载安装包，安装时选择"Git Bash"和"Checkout as-is, commit Unix-style line endings" |
| **macOS** | `brew install git`（需先安装Homebrew） |
| **Ubuntu/Debian** | `sudo apt install git -y` |
| **CentOS/RHEL** | `sudo yum install git -y` |
| **验证安装** | `git --version`，输出形如 `git version 2.42.0` |

### 2.2 用户身份配置

每次提交都会记录作者信息，必须配置：

```bash
git config --global user.name "Su Xiangyu"
git config --global user.email "xiangyu.su@example.com"
```

> ⚠️ **务必与远程托管平台（GitHub/GitLab）的账号邮箱一致**，否则提交不会被关联到你的账号，贡献统计也无法正确显示。

### 2.3 编辑器与行尾配置

```bash
# 默认编辑器（git commit 打开编辑窗口时使用）
git config --global core.editor "vim"
# Windows上也可配置为 Notepad++ 或 VS Code：
# git config --global core.editor "code --wait"

# Windows行尾配置（解决 CRLF / LF 战争）
git config --global core.autocrlf input   # macOS/Linux
git config --global core.autocrlf true    # Windows（推荐）
```

> 💡 **core.autocrlf 详解**：
> - `true`：提交时 CRLF→LF，检出时 LF→CRLF（Windows推荐）
> - `input`：提交时 CRLF→LF，检出时不做转换（macOS/Linux推荐）
> - `false`：不做任何转换（不推荐，跨平台协作极易出现行尾混乱）

### 2.4 别名配置

高频命令的短别名可大幅提升日常效率：

```bash
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.cm "commit -m"
git config --global alias.lg "log --oneline --graph --all --decorate"
git config --global alias.df diff
git config --global alias.dfc "diff --cached"
git config --global alias.unstage "restore --staged"
git config --global alias.last "log -1 HEAD"
```

### 2.5 验证配置

```bash
git config --list            # 列出所有配置
git config --list --global   # 仅全局配置
git config user.name         # 查单个配置项
```

配置文件位置：
- **系统级**：`/etc/gitconfig`（`--system`）
- **全局级**：`~/.gitconfig` 或 `~/.config/git/config`（`--global`）
- **项目级**：`.git/config`（当前仓库，优先级最高）

> 💡 优先级：项目级 > 全局级 > 系统级，遵循就近覆盖原则。

---

## 3. .gitignore：Java项目忽略规则

### 3.1 完整模板

```gitignore
# ============ Maven / Gradle 构建产物 ============
target/
build/
*.class
*.jar
*.war
*.ear

# ============ IDE 配置文件 ============
.idea/
*.iml
*.iws
*.ipr
.vscode/
.settings/
.project
.classpath
*.swp
*.swo
*~

# ============ 本地配置与敏感信息 ============
application-local.yml
application-dev.yml
application-local.properties
bootstrap-local.yml
*.env
.env.local

# ============ 日志文件 ============
logs/
*.log
log/
**/logs/

# ============ 依赖缓存 ============
.m2/
gradle/
.gradle/
node_modules/

# ============ 操作系统文件 ============
.DS_Store
Thumbs.db
Desktop.ini

# ============ 临时文件 ============
*.tmp
*.temp
*.bak
*.orig
*.pid
*.seed
*.pid.lock

# ============ 系统与测试报告 ============
test-output/
*.html.report
coverage/
surefire-reports/
```

### 3.2 忽略规则原理

| 模式 | 含义 | 示例 |
|------|------|------|
| `target/` | 忽略整个目录 | 匹配任意层级的 `target/` |
| `/target/` | 仅忽略根目录下的 `target/` | 不匹配 `sub/target/` |
| `*.log` | 忽略所有 `.log` 文件 | `/var/log/app.log` 被忽略 |
| `!important.log` | 排除（不忽略）该文件 | 与上一行配合，只追踪重要日志 |
| `build/` | 忽略 build 目录及内容 | 匹配任意层级的 `build/` |
| `**/logs/` | 忽略任意深度的 logs 目录 | 匹配 `a/b/logs/` |

> 💡 `.gitignore` 匹配规则基于**文件路径模式**，`*` 匹配任意字符串（不含 `/`），`**` 匹配任意层级目录。

### 3.3 已追踪文件的忽略处理

如果文件已被Git追踪，单纯加入 `.gitignore` 不会生效。需先移除追踪：

```bash
# 从追踪中移除（保留工作区文件）
git rm --cached application-dev.yml

# 从追踪中移除（同时删除文件）
git rm application-dev.yml

# 批量移除整个目录的追踪
git rm -r --cached target/
```

> ⚠️ **重要**：`git rm --cached` 只移除Git追踪，不删除本地文件。提交此变更后，`.gitignore` 才会对新克隆生效。但**历史版本中仍然存在该文件**，如需彻底抹除需使用 `git filter-branch` 或 BFG Repo-Cleaner。

---

## 4. Git的三种状态与三态工作流

### 4.1 三态模型

Git的文件生命周期围绕三个区域流转：

| 区域 | 英文 | 说明 | 存储位置 |
|------|------|------|---------|
| **工作区** | Working Directory | 你实际编辑文件的地方，即项目目录 | 文件系统 |
| **暂存区** | Staging Area (Index) | 下次提交的快照清单，可精细控制提交内容 | `.git/index` |
| **版本库** | Repository (.git) | 存储所有提交历史的数据库 | `.git/objects/` |

三种文件状态对应三个区域：

| 文件状态 | 说明 | 所在区域 |
|----------|------|---------|
| **Modified** | 已修改但尚未加入暂存区 | 工作区 |
| **Staged** | 已加入暂存区，等待提交 | 暂存区 |
| **Committed** | 已提交到版本库，安全存储 | 版本库 |

### 4.2 三态流转图

```
        git add              git commit
  ┌────────────┐  ────────►  ┌────────────┐  ────────►  ┌────────────┐
  │  Working   │  staging    │  Staging   │  snapshot   │ Repository │
  │ Directory  │  file       │   Area     │  commit     │  (.git)    │
  └────────────┘  ◄────────  └────────────┘             └────────────┘
       │           git         ▲
       │        restore        │
       │        --staged       │ git checkout
       │                       │ (switch branch)
       ▼                       │
  (Modified)                   │
       │                       │
       └───────────────────────┘
         git restore (丢弃修改)
```

> 🎯 **三态模型的设计哲学**：暂存区是Git最巧妙的设计之一——它让你可以在一次提交中只包含部分修改，实现"原子化提交"。比如你改了A和B两个文件，但只想先提交A，就可以 `git add A` 后 `git commit`，B保留在工作区下次再提交。

---

## 5. 基础工作流

### 5.1 初始化仓库

```bash
# 创建项目目录
mkdir spring-boot-demo
cd spring-boot-demo

# 初始化 Git 仓库
git init
# 输出: Initialized empty Git repository in /path/spring-boot-demo/.git/

# 查看仓库状态
git status
# 输出: On branch master
#       No commits yet
#       nothing to commit (create/copy files and use "git add" to track)
```

### 5.2 克隆远程仓库

```bash
# 标准克隆（完整历史）
git clone https://github.com/user/spring-boot-demo.git

# 指定分支克隆
git clone -b develop https://github.com/user/spring-boot-demo.git

# 浅克隆（仅最新版本，大仓库加速）
git clone --depth 1 https://github.com/user/spring-boot-demo.git

# 指定本地目录名
git clone https://github.com/user/spring-boot-demo.git my-app
```

### 5.3 查看状态

```bash
# 详细状态
git status

# 简洁状态（单行显示）
git status -s
#  M README.md        ← 空格+M: modified, 未暂存
# A  src/main/App.java ← A: added, 已暂存
# ?? .env              ← ?? : 未追踪
```

`git status -s` 的两列含义：
- **第一列**：暂存区状态（相对于上次提交）
- **第二列**：工作区状态（相对于暂存区）

| 标记 | 含义 |
|------|------|
| `??` | 未追踪（Untracked） |
| ` M` | 已修改未暂存 |
| `M ` | 已暂存 |
| `MM` | 已暂存且又修改 |
| `A ` | 新增已暂存 |
| ` D` | 已删除未暂存 |
| `D ` | 已删除已暂存 |

### 5.4 添加与提交

```bash
# 添加单个文件到暂存区
git add README.md

# 添加所有文件（含新增/修改/删除）
git add -A
# 或简写
git add .

# 交互式添加（选择文件的部分变更）
git add -p

# 提交暂存区到版本库
git commit -m "feat: 初始化SpringBoot项目"

# 跳过暂存区，直接提交已追踪文件的修改（新增文件仍需 git add）
git commit -am "fix: 修复空指针异常"

# 修改最近一次提交（未推送时使用）
git commit --amend -m "feat: 初始化SpringBoot项目（校正描述）"
```

> ⚠️ `git commit --amend` 会生成一个新的commit hash，替换原有提交。如果已推送到远程，再次推送需 `--force-with-lease`。

### 5.5 查看差异

```bash
# 工作区 vs 暂存区（未暂存的修改）
git diff

# 暂存区 vs 版本库（已暂存但未提交的修改）
git diff --cached
# 或
git diff --staged

# 工作区 vs 版本库（所有未提交的修改）
git diff HEAD

# 两个提交之间的差异
git diff commitA..commitB

# 两个分支之间的差异
git diff main..develop

# 仅显示文件名
git diff --name-only
git diff --name-status

# 统计变更行数
git diff --stat
```

---

## 6. Commit最佳实践

### 6.1 Conventional Commits约定

[Conventional Commits](https://www.conventionalcommits.org/) 是一个轻量级约定，规定了提交消息的结构，被 Angular、ESLint、Vue 等主流项目广泛采用。

```
<type>(<scope>): <subject>

<body>

<footer>
```

**常用类型**：

| 类型 | 说明 | 版本影响 | Java后端场景 |
|------|------|---------|-------------|
| `feat` | 新功能 | 次版本+1 | 新增接口、服务、Controller |
| `fix` | Bug修复 | 修订号+1 | 修复空指针、SQL异常、业务错误 |
| `docs` | 文档变更 | 无 | README、API文档、注释 |
| `style` | 代码格式 | 无 | 缩进、分号、格式化（非语义变更） |
| `refactor` | 重构 | 无 | 代码结构优化、设计模式引入 |
| `perf` | 性能优化 | 无 | SQL优化、缓存策略、并发改进 |
| `test` | 测试 | 无 | 新增/修改单元测试、集成测试 |
| `chore` | 构建/工具 | 无 | Maven依赖、pom.xml、CI配置 |
| `ci` | CI配置 | 无 | Jenkinsfile、GitHub Actions |
| `revert` | 回滚 | 无 | 回滚之前的提交 |

**示例**：

```text
feat(user-service): 实现用户登录JWT认证

- 集成jjwt库实现Token签发与验证
- 新增LoginController、AuthService
- 添加JwtAuthenticationFilter过滤器
- 配置白名单路径免认证

Closes #1234
```

### 6.2 原子化提交

**原子提交原则**：每次提交只包含一个完整的逻辑变更。

| 反例（不建议） | 正例（推荐） |
|---------------|-------------|
| `feat: 修改了登录和订单功能` | `feat(auth): 实现JWT登录认证` |
| 一个commit里混杂了 bugfix + 新功能 + 重构 | `fix(order): 修复订单金额计算精度丢失` |
| 提交了30+文件，涉及6个不相关模块 | `refactor(user): 抽取UserValidator校验类` |

> 💡 **何时提交**：每完成一个逻辑单元（一个功能点、一个bug修复、一次重构），立即commit。不要等一天结束再提交，不要等一个模块全部开发完再提交。频繁提交等于频繁备份。

### 6.3 好的Commit Message怎么写

**坏的提交**（无信息量）：

```
git commit -m "修复bug"
git commit -m "update"
git commit -m "提交代码"
git commit -m "."
```

**好的提交**（一看就懂）：

```
git commit -m "fix(order): 修复订单金额四舍五入导致的1分钱精度丢失"
git commit -m "feat(payment): 新增支付宝支付回调处理"
git commit -m "refactor(user): 将UserService拆分为QueryService和CommandService"
```

> 🎯 **Commit Message黄金法则**：假设半年后的你（或你的同事）在看git blame，TA能凭commit message理解"为什么有这个变更"。

---

## 7. Git概念深入

### 7.1 SHA-1哈希

Git通过SHA-1哈希算法为每个对象生成一个40位的十六进制标识符：

```
commit 8a2f8c9b3d1e5f6a7b8c9d0e1f2a3b4c5d6e7f8a
Author: Su Xiangyu <xiangyu.su@example.com>
Date:   2026-07-27 14:30:00 +0800

    feat: 初始化SpringBoot项目
```

- **保证数据完整性**：文件内容任何一位变化都会导致哈希完全不同
- **内容寻址存储**：Git将对象存储在 `.git/objects/` 目录下，文件名就是SHA-1哈希
- **引用完整性**：每个commit都包含parent commit的哈希，形成不可篡改的链

```bash
# 查看对象类型
git cat-file -t 8a2f8c9
# commit

# 查看对象内容
git cat-file -p 8a2f8c9
```

### 7.2 HEAD指针

HEAD是一个指向当前分支的**符号引用**（symbolic reference），告诉Git你正在哪个分支上工作：

```bash
# HEAD指向master分支
cat .git/HEAD
# ref: refs/heads/master

# 切换分支后
git checkout develop
cat .git/HEAD
# ref: refs/heads/develop

# 分离头指针（detached HEAD）：直接指向一个commit而非分支
git checkout 8a2f8c9
cat .git/HEAD
# 8a2f8c9b3d1e5f6a7b8c9d0e1f2a3b4c5d6e7f8a
```

> ⚠️ **分离头指针风险**：当HEAD直接指向commit时，新提交不会属于任何分支。切换分支后这些提交将处于"悬空"状态，最终被Git GC清理。如果需要保留，创建分支：`git checkout -b new-branch-name`。

### 7.3 分支本质是指针

在Git中，**分支就是一个指向commit的指针**，非常轻量：

```
        o---o---o  feature/login (指向最新commit)
       /
o---o---o---o  master (指向最新commit)
    ↑
    初始commit（所有分支的祖先）
```

```bash
# 查看所有分支及其指向的commit
git branch -v

# 查看分支追踪关系
git branch -vv

# 查看commit所在的分支（包含关系）
git branch --contains 8a2f8c9
```

> 💡 **分支成本对比**：
> - **Git**：创建分支 = 创建指针 = 毫秒级，一个41字节文件
> - **SVN**：创建分支 = 拷贝目录 = 秒到分级，文件越多越慢
>
> 正因为分支成本趋近于零，Git flow、GitHub Flow等基于分支的工作流才能成为可能。

---

## 8. 查看历史

### 8.1 git log实用选项

```bash
# 标准日志
git log

# 单行简洁日志
git log --oneline

# 图形化 + 全部分支 + 装饰信息（最常用）
git log --oneline --graph --all --decorate

# 显示文件变更详情
git log --stat

# 显示具体变更内容
git log -p
git log -p -2    # 仅最近2次提交

# 按作者过滤
git log --author="Su"

# 按时间过滤
git log --since="2026-01-01"
git log --until="2026-06-30"
git log --after="2 weeks ago"

# 按提交信息过滤
git log --grep="fix(order)"

# 查看某个文件的提交历史
git log -- README.md

# 查看某个函数的变更历史
git log -L :methodName:src/main/java/UserService.java
```

**最常用的查看命令组合**：

```bash
# 终极日志（建议配置为别名）
git log --oneline --graph --all --decorate --pretty=format:'%C(yellow)%h%C(reset) %C(blue)%an%C(reset) %C(red)%ar%C(reset) %s'

# 配置为别名
git config --global alias.lg "log --oneline --graph --all --decorate"
```

### 8.2 搜索提交内容

```bash
# 搜索所有commit中新增/删除了特定字符串的提交
git log -S "JwtTokenUtil" --oneline

# 使用正则搜索commit message
git log --oneline --grep="fix.*order"

# 搜索某个字符串在历史中的所有出现
git grep "JwtTokenUtil" $(git rev-list --all)
```

> 💡 `git log -S "string"`（pickaxe选项）非常强大——它会找出所有**新增或删除**了该字符串的提交，即使commit message完全不相关。适用于"哪个提交引入了这段代码"的溯源。

---

## 9. 撤销操作

### 9.1 工作区撤销

文件已修改但**未 `git add`**：

```bash
# 丢弃工作区的全部修改（恢复到最后一次提交或暂存的状态）
git restore README.md

# 旧式命令（仍可用）
git checkout -- README.md

# 丢弃所有文件的修改
git restore .
```

### 9.2 暂存区撤销

文件已 `git add` 但**未 `git commit`**：

```bash
# 从暂存区移除，修改保留在工作区
git restore --staged README.md

# 旧式命令
git reset HEAD README.md

# 全部取消暂存
git restore --staged .
```

### 9.3 git reset三种模式对比

`git reset` 用于移动HEAD并决定如何处理工作区和暂存区：

```bash
# 语法
git reset [--soft | --mixed | --hard] [<commit>]
```

| 模式 | HEAD移动 | 暂存区 | 工作区 | 安全性 | 使用场景 |
|------|---------|--------|--------|--------|---------|
| `--soft` | 移动到指定commit | 不变 | 不变 | 最安全 | 合并多个commit为一次（commit --amend 的替代） |
| `--mixed`（默认） | 移动到指定commit | 重置为指定commit | 不变 | 安全 | 取消暂存，重新组织提交内容 |
| `--hard` | 移动到指定commit | 重置为指定commit | 重置为指定commit | 危险 | 彻底丢弃本地修改，强制同步远程 |

```bash
# --soft：保留所有修改，重新提交
git reset --soft HEAD~1
# 现在所有文件都在暂存区，可修改commit message后重新提交

# --mixed：取消暂存，修改保留在工作区
git reset HEAD~1
# 文件回到Modified状态，可重新git add选择提交

# --hard：彻底丢弃（谨慎使用！）
git reset --hard HEAD~1
# 回退一个commit，且本地修改全部丢失

# 回退到某个远程commit
git reset --hard origin/main
```

> ⚠️ **警告**：`git reset --hard` 会**永久丢弃工作区和暂存区的修改**。执行前务必确认没有未保存的内容，或先 `git stash` 备份。

### 9.4 restore vs reset vs revert

| 命令 | 作用于 | 是否修改历史 | 适用场景 |
|------|--------|------------|---------|
| `git restore <file>` | 工作区 | 否 | 丢弃未暂存的修改 |
| `git restore --staged <file>` | 暂存区 | 否 | 取消暂存 |
| `git reset --hard HEAD~1` | 分支历史 | **是**（改写） | 本地未推送的回退 |
| `git revert HEAD` | 分支历史 | 否（新commit抵消） | **已推送的提交回滚** |

> 🎯 **核心原则**：
> - **未推送到远程**：`git reset` 随意使用
> - **已推送到远程**（公共分支）：必须使用 `git revert`，通过**新增一个反向commit**来撤销，不修改历史
> - **已推送到远程**（个人分支）：谨慎使用 `git reset --hard` + `git push --force-with-lease`

---

## 10. 完整实战：SpringBoot项目初始化与版本管理

以下是一个完整的实战场景，演示从零开始用Git管理SpringBoot项目。

### 第一阶段：环境准备

```bash
# 1. 创建项目根目录
mkdir spring-boot-demo
cd spring-boot-demo

# 2. 初始化Git仓库
git init

# 3. 创建 .gitignore
cat > .gitignore << 'EOF'
target/
*.class
*.jar
*.war
.idea/
*.iml
*.log
logs/
application-local.yml
.env
.DS_Store
EOF

# 4. 创建 README
echo "# Spring Boot Demo Project" > README.md

# 5. 提交初始版本
git add .
git commit -m "chore: 初始化SpringBoot项目结构

- 初始化Git仓库
- 添加Java项目.gitignore
- 添加项目README"
```

### 第二阶段：基础开发

```bash
# 1. 创建项目结构和代码
mkdir -p src/main/java/com/demo/controller
mkdir -p src/main/java/com/demo/service
mkdir -p src/main/resources

# 2. 创建主应用类
cat > src/main/java/com/demo/DemoApplication.java << 'EOF'
package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
EOF

# 3. 创建Controller
cat > src/main/java/com/demo/controller/HelloController.java << 'EOF'
package com.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Git!";
    }
}
EOF

# 4. 添加 application.yml
cat > src/main/resources/application.yml << 'EOF'
server:
  port: 8080
spring:
  application:
    name: spring-boot-demo
EOF

# 5. 分步提交（先提交基础结构，再提交应用代码）
git add pom.xml
git commit -m "chore: 添加Maven构建配置

- 配置Spring Boot Starter Parent
- 添加spring-boot-starter-web依赖"

git add src/main/java/com/demo/DemoApplication.java
git add src/main/resources/application.yml
git commit -m "feat: 初始化SpringBoot主应用入口

- 创建DemoApplication启动类
- 配置应用端口和名称"
```

### 第三阶段：迭代提交

```bash
# 1. 新增功能
cat > src/main/java/com/demo/controller/GreetingController.java << 'EOF'
package com.demo.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GreetingController {

    @PostMapping("/greet")
    public Map<String, String> greet(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "World");
        return Map.of("message", "Hello, " + name + "!");
    }
}
EOF

# 2. 查看修改状态
git status

# 3. 分次暂存 + 提交（原子提交）
git add src/main/java/com/demo/controller/GreetingController.java
git commit -m "feat: 新增GreetingAPI

- POST /api/greet 接收name参数返回个性化问候
- 默认name为World"

# 4. 查看日志
git log --oneline --graph --all --decorate

# 5. 发现bug：修复
cat > src/main/java/com/demo/controller/HelloController.java << 'EOF'
package com.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello, Git!");
    }
}
EOF

git add src/main/java/com/demo/controller/HelloController.java
git commit -m "fix: HelloController返回格式改为JSON对象

- 原String类型改为Map封装，保持API响应格式统一"

# 6. 查看最终提交历史
git lg
```

### 第四阶段：查看成果

```bash
# 提交历史
git log --oneline
# 输出示例：
# 2d1e3a4 fix: HelloController返回格式改为JSON对象
# 8f4b9c1 feat: 新增GreetingAPI
# a1b2c3d feat: 初始化SpringBoot主应用入口
# e5f6g7h chore: 添加Maven构建配置
# 9i0j1k2 chore: 初始化SpringBoot项目结构

# 查看文件变更
git log --stat

# 查看某次提交的具体变更
git show 8f4b9c1

# 查看至今为止的变更量
git diff --stat $(git log --oneline | tail -1 | awk '{print $1}')..HEAD
```

> 🎯 **实战要点总结**：
> 1. **先写.gitignore再开始开发**，避免编译产物被追踪
> 2. **一个功能一次commit**，commit message使用Conventional Commits规范
> 3. **频繁提交、尽早提交**，每次提交都是可恢复的备份点
> 4. **提交前 git status + git diff** 确认内容无误
> 5. **合理使用分支**，main分支始终保持可发布状态

---

> 🎯 **总结**：Git应用入门的关键在于理解三态模型的流转逻辑、掌握分支即指针的轻量本质、养成Conventional Commits的提交习惯。这三个认知到位后，剩下的命令都是"查手册即可掌握"的战术细节。从一个规范的 `git init` 和 `.gitignore` 开始你的每一次项目。
