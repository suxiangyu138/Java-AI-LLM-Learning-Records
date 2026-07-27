# 19-Git企业级开发常用操作
> 🎯 面向中高级Java后端工程师的Git实战手册，涵盖远程协作、分支策略、暂存管理、选择性子提交、交互式变基、安全强制推送、代码溯源、版本标记、并行工作区、高效别名、自动二分查找等企业高频场景，每个技术点均以"问题场景→解决方案→最佳实践"结构展开

## 目录

1. [远程仓库操作](#1-远程仓库操作)
   - 1.1 [remote管理：添加/查看/修改/删除](#11-remote管理添加查看修改删除)
   - 1.2 [fetch vs pull vs pull --rebase](#12-fetch-vs-pull-vs-pull---rebase)
   - 1.3 [上游分支与追踪关系](#13-上游分支与追踪关系)
2. [分支管理进阶](#2-分支管理进阶)
   - 2.1 [分支创建与切换全览](#21-分支创建与切换全览)
   - 2.2 [删除分支的多种姿势](#22-删除分支的多种姿势)
   - 2.3 [追踪分支与上游配置](#23-追踪分支与上游配置)
3. [Stash：工作区暂存](#3-stash工作区暂存)
   - 3.1 [基础用法](#31-基础用法)
   - 3.2 [带消息的暂存](#32-带消息的暂存)
   - 3.3 [暂存指定文件](#33-暂存指定文件)
   - 3.4 [从暂存创建分支](#34-从暂存创建分支)
4. [Cherry-pick：选择性提交迁移](#4-cherry-pick选择性提交迁移)
   - 4.1 [基础用法](#41-基础用法)
   - 4.2 [常见场景](#42-常见场景)
   - 4.3 [进阶选项](#43-进阶选项)
5. [Interactive Rebase：交互式变基](#5-interactive-rebase交互式变基)
   - 5.1 [rebase -i 基础](#51-rebase--i-基础)
   - 5.2 [squash/fixup：合并提交](#52-squashfixup合并提交)
   - 5.3 [reword：修改提交信息](#53-reword修改提交信息)
   - 5.4 [reorder/drop：重排与删除](#54-reorderdrop重排与删除)
   - 5.5 [安全改写历史的原则](#55-安全改写历史的原则)
6. [Force Push安全策略](#6-force-push安全策略)
   - 6.1 [--force-with-lease vs --force](#61---force-with-lease-vs---force)
   - 6.2 [何时允许force push](#62-何时允许force-push)
7. [Git Blame：代码溯源](#7-git-blame代码溯源)
   - 7.1 [基础用法](#71-基础用法)
   - 7.2 [高级选项](#72-高级选项)
8. [Git Tag：版本标记](#8-git-tag版本标记)
   - 8.1 [轻量标签 vs 附注标签](#81-轻量标签-vs-附注标签)
   - 8.2 [标签的推送与删除](#82-标签的推送与删除)
   - 8.3 [签名标签](#83-签名标签)
9. [Git Worktree：并行工作区](#9-git-worktree并行工作区)
   - 9.1 [为什么需要worktree](#91-为什么需要worktree)
   - 9.2 [基础用法](#92-基础用法)
   - 9.3 [实战场景](#93-实战场景)
10. [Aliases：高效别名](#10-aliases高效别名)
    - 10.1 [必装别名清单](#101-必装别名清单)
    - 10.2 [配置方式](#102-配置方式)
11. [Git Bisect：二分查找Bug源头](#11-git-bisect二分查找bug源头)
    - 11.1 [手动二分查找](#111-手动二分查找)
    - 11.2 [自动化二分查找](#112-自动化二分查找)
12. [完整实战：生产热修复→Cherry-pick→Tag发布](#12-完整实战生产热修复cherry-picktag发布)

---

## 1. 远程仓库操作

### 1.1 remote管理：添加/查看/修改/删除

```bash
# 查看远程仓库
git remote                    # 列出远程仓库别名
git remote -v                 # 查看URL（fetch/push地址）

# 添加远程仓库
git remote add origin https://github.com/user/project.git

# 修改远程仓库URL（仓库迁移时使用）
git remote set-url origin https://github.com/new-org/project.git

# 修改远程仓库的推送地址（分离fetch和push）
git remote set-url --push origin https://github.com/new-org/project.git

# 删除远程仓库关联
git remote remove origin

# 重命名远程仓库
git remote rename origin upstream
```

| 命令 | 场景 |
|------|------|
| `git remote add` | 本地init后首次关联远程仓库 |
| `git remote set-url` | 仓库迁移（GitHub→GitLab）、SSH→HTTPS切换 |
| `git remote remove` | 项目废弃或重新关联 |
| `git remote rename` | fork项目的上游仓库管理 |

> 💡 **多远程仓库管理**：开发中常同时关联多个远程仓库（origin + upstream），用于开源协作的fork工作流：
> ```bash
> git remote add origin git@github.com:myuser/project.git
> git remote add upstream git@github.com:original/project.git
> git fetch upstream
> git merge upstream/main
> ```

### 1.2 fetch vs pull vs pull --rebase

这是Git远程协作中最核心的概念区分：

| 操作 | 实质 | 是否会合并 | 是否产生合并提交 | 推荐场景 |
|------|------|-----------|----------------|---------|
| `git fetch` | 仅下载远程对象，不改变本地分支 | 否 | 否 | 先查看远程变更，再决定如何合并 |
| `git pull` | fetch + merge | 是 | 可能产生 | 单人分支或无需保持线性历史的场景 |
| `git pull --rebase` | fetch + rebase | 是（变基合并） | 否 | 多人协作分支，保持提交历史线性 |

```bash
# fetch：安全查看远程更新
git fetch origin
git log --oneline origin/main..main  # 查看本地领先远程的提交
git log --oneline main..origin/main  # 查看远程领先本地的提交

# pull（默认merge行为）
git pull origin develop
# 等价于：
# git fetch origin
# git merge origin/develop

# pull --rebase（推荐）
git pull --rebase origin develop
# 等价于：
# git fetch origin
# git rebase origin/develop
```

> ⚠️ **为什么不推荐直接用 `git pull`**：
> 1. `git pull` = fetch + merge，merge会产生一个额外的"merge commit"，导致历史图复杂
> 2. 多人协作时，merge commit层层嵌套，`git log --graph` 变成一团乱麻
> 3. `git pull --rebase` 将自己的提交"变基"到远程最新提交之后，历史保持线性、清晰
>
> 配置 `git pull` 默认使用rebase：
> ```bash
> git config --global pull.rebase true
> ```

### 1.3 上游分支与追踪关系

```bash
# 创建分支的同时设置上游（tracking）
git checkout -b feature/user-order origin/develop

# 为已有分支设置上游
git branch -u origin/develop

# 查看追踪关系
git branch -vv            # 详细显示本地分支追踪的远程分支
git remote show origin    # 查看所有分支的追踪关系

# 清理本地已删除的远程追踪分支
git remote prune origin
```

| 状态 | 上游分支设置 | `git push` 行为 | `git pull` 行为 |
|------|-------------|----------------|----------------|
| 已设置上游 | `git branch -u origin/feature` | 直接 `git push` | 直接 `git pull` |
| 未设置上游 | 首推时 `git push -u origin feature` | 报错，需指定远程和分支 | 报错，需指定远程和分支 |

---

## 2. 分支管理进阶

### 2.1 分支创建与切换全览

```bash
# ---- 创建 ----
git branch develop                        # 基于当前HEAD创建
git branch feature/order 8a2f8c9          # 基于指定commit创建
git branch feature/login origin/develop   # 基于远程分支创建
git checkout -b feature/pay develop       # 创建并切换（基于develop）

# ---- 切换 ----
git switch develop             # Git 2.23+ 推荐
git switch -                   # 切换到上一个分支（cd - 的类比）
git checkout develop           # 传统方式

# ---- 重命名 ----
git branch -m old-name new-name           # 重命名当前分支
git branch -m develop feature/develop     # 重命名指定分支
```

### 2.2 删除分支的多种姿势

```bash
# 删除本地已合并分支（安全）
git branch -d feature/user-login

# 强制删除本地未合并分支（危险）
git branch -D feature/user-login

# 删除远程分支（两种方式）
git push origin --delete feature/user-login
git push origin :feature/user-login        # 冒号语法（推送空到远程）

# 批量清理本地已合并到main的分支
git branch --merged main | grep -v 'main\|develop' | xargs git branch -d

# 批量清理远程已删除分支的本地追踪引用
git remote prune origin
git fetch --prune
```

### 2.3 追踪分支与上游配置

```bash
# 首次推送并建立追踪关系
git push -u origin feature/user-login
# 推送成功后，本地feature/user-login自动追踪origin/feature/user-login

# 为已有分支设置上游
git branch --set-upstream-to=origin/develop develop

# 取消上游追踪
git branch --unset-upstream develop

# 查看当前分支的远程状态
git status -sb
# ## feature/login...origin/feature/login [ahead 2, behind 3]
# ahead 2 = 本地有2个远程没有的提交
# behind 3 = 远程有3个本地没有的提交
```

> 💡 **ahead/behind的理解**：`[ahead 2, behind 3]` 意味着需要先 `git pull`（或 `git pull --rebase`）拉取远程的3个提交，然后再 `git push` 推送本地的2个提交。

---

## 3. Stash：工作区暂存

### 3.1 基础用法

场景：你正在feature分支开发到一半，突然需要切换到main分支处理紧急hotfix，但不想提交半成品代码。

```bash
# 暂存当前所有未提交的修改（包括暂存区和工作区）
git stash

# 查看暂存列表
git stash list
# stash@{0}: WIP on feature/login: 8a2f8c9 实现登录功能
# stash@{1}: WIP on develop: 3b4c5d6 优化SQL查询

# 恢复最近一次暂存（保留stash记录）
git stash apply

# 恢复并删除最近一次暂存
git stash pop

# 恢复指定暂存
git stash apply stash@{1}

# 删除指定暂存
git stash drop stash@{1}

# 清空所有暂存
git stash clear
```

### 3.2 带消息的暂存

```bash
# 暂存并添加描述信息（推荐）
git stash push -m "WIP: 完成JWT令牌生成，尚未处理验证逻辑"

# 查看时直接看到描述
git stash list
# stash@{0}: On feature/login: WIP: 完成JWT令牌生成，尚未处理验证逻辑
```

### 3.3 暂存指定文件

```bash
# 只暂存某个文件或目录
git stash push -m "仅暂存配置文件" -- src/main/resources/application.yml

# 暂存除指定文件外的所有内容
git stash push -m "暂存所有，排除application.yml" -- ":(exclude)*application.yml"
```

### 3.4 从暂存创建分支

```bash
# 从stash创建一个新分支（自动恢复stash内容并清除stash记录）
git stash branch hotfix/pay-timeout
# 等价于：
# 1. 基于创建stash时的commit创建新分支
# 2. 自动 git stash pop
```

> 💡 **适用场景**：当你把stash pop到不同分支上遇到冲突时，`git stash branch` 可以在stash所在的原分支commit上创建分支并恢复，完美避免冲突。

---

## 4. Cherry-pick：选择性提交迁移

### 4.1 基础用法

Cherry-pick允许你将某个分支上的一个或多个提交"复制"到当前分支，就像从樱桃树上挑选成熟的果子。

```bash
# 复制单个提交到当前分支
git cherry-pick 8a2f8c9

# 复制连续区间（从A到B的所有提交，不含A）
git cherry-pick A..B

# 复制连续区间（含A）
git cherry-pick A^..B

# 复制多个不连续的提交
git cherry-pick 8a2f8c9 3b4c5d6 1a2b3c4
```

### 4.2 常见场景

| 场景 | 操作 | 说明 |
|------|------|------|
| **Hotfix到Main** | hotfix分支修复bug后，cherry-pick到main | 只取修复commit，不取hotfix分支的其他commit |
| **Main到Develop** | main上的hotfix同步到develop | 确保develop也包含修复，避免回归 |
| **特定功能提前上线** | A功能在feature分支，B功能需先上线 | 只cherry-pick B功能的commit到release分支 |
| **开源补丁** | 开源项目修复了bug | cherry-pick对方仓库的commit到自己的项目 |

```bash
# 实战：hotfix同步到多分支
git checkout main
git cherry-pick -x 8a2f8c9    # -x 自动添加 "(cherry picked from commit ...)" 注释

git checkout develop
git cherry-pick -x 8a2f8c9
```

### 4.3 进阶选项

```bash
# -n（--no-commit）：只应用变更到工作区，不自动commit
# 可用于将多个cherry-pick合并为一个commit
git cherry-pick -n 8a2f8c9
git cherry-pick -n 3b4c5d6
git commit -m "feat: 合并两个功能提交"

# -e（--edit）：提交前编辑commit message
git cherry-pick -e 8a2f8c9

# 处理冲突：cherry-pick过程中出现冲突
git cherry-pick 8a2f8c9
# ...手动解决冲突...
git add .
git cherry-pick --continue    # 解决后继续
# 或
git cherry-pick --abort       # 放弃本次cherry-pick，回到之前状态
# 或
git cherry-pick --skip        # 跳过当前commit
```

> 🎯 **Cherry-pick最佳实践**：
> 1. 优先使用 `git merge` 而不是 cherry-pick，merge保留了分支拓扑和历史关系
> 2. cherry-pick应用于**跨分支线性的、单次提交级别的修复同步**
> 3. 对cherry-pick的commit使用 `-x` 参数，保留可追溯的元信息
> 4. 避免在长期存在的分支上反复cherry-pick同一批commit（会导致重复提交）

---

## 5. Interactive Rebase：交互式变基

### 5.1 rebase -i 基础

交互式变基是Git最强大也最危险的改写历史工具。它允许你对一系列提交进行**重新组织、合并、修改信息、删除**等操作。

```bash
# 交互式变基最近3个提交
git rebase -i HEAD~3

# 或基于某个分支变基
git rebase -i main
```

执行后会打开编辑器，显示如下内容：

```
pick 8a2f8c9 feat: 实现用户登录接口
pick 3b4c5d6 fix: 修复登录验证空指针
pick 1a2b3c4 fix: 再修复登录验证问题

# Commands:
# p, pick <commit> = use commit
# r, reword <commit> = use commit, but edit the commit message
# e, edit <commit> = use commit, but stop for amending
# s, squash <commit> = use commit, but meld into previous commit
# f, fixup <commit> = like "squash", but discard this commit's log message
# x, exec <command> = run command (the rest of the line) using shell
# b, break = stop here (continue rebase later with 'git rebase --continue')
# d, drop <commit> = remove commit
```

### 5.2 squash/fixup：合并提交

场景：开发过程中做了多次"小改动"提交，PR前需要整理成更清晰的提交。

```text
# 原始提交历史（杂乱）
feat: WIP 用户登录
feat: 添加登录验证
fix: 验证逻辑修正
fix: typo
chore: 删除调试日志

# 合并后（清晰）
feat: 实现用户登录功能
```

操作方式：将后4个提交改为 `s` 或 `f`：

```
pick 8a2f8c9 feat: 实现用户登录接口
s 3b4c5d6 fix: 修复登录验证空指针
f 1a2b3c4 fix: 再修复登录验证问题
f 4d5e6f7 chore: 删除调试日志
```

> 💡 **squash vs fixup**：
> - `squash`（简写 `s`）：将当前提交合并到上一个提交并**保留**commit message，可合并编辑
> - `fixup`（简写 `f`）：将当前提交合并到上一个提交并**丢弃**commit message，适合合并"typo"类commit

### 5.3 reword：修改提交信息

场景：commit message写错了或不够清晰。

```
pick 8a2f8c9 feat: 实现用户登录接口
reword 3b4c5d6 fix: 修复登录验证空指针    ← 改成 reword
pick 1a2b3c4 fix: 修复订单金额精度问题
```

保存退出后，Git会逐个打开编辑器让你修改commit message。

### 5.4 reorder/drop：重排与删除

场景：提交顺序不理想，或某个提交不应该存在。

```text
# 原始
pick a 修复订单bug     ← 应放到最后
pick b 实现登录功能    ← 应该第一个
pick c WIP登录
pick d 删除测试代码    ← 这个不要

# 调整后
pick b 实现登录功能
pick c WIP登录
pick a 修复订单bug
drop d 删除测试代码
```

### 5.5 安全改写历史的原则

> ⚠️ **黄金法则**：**绝不对已推送到公共分支的提交执行 rebase！**
>
> 改写已推送的公共历史会导致团队成员的分支混乱，产生"双胞胎提交"。

| 场景 | 能否rebase | 说明 |
|------|-----------|------|
| 本地未推送的commit | 安全 | 随便改，只有你看到 |
| 个人feature分支已推送 | 有条件安全 | 用 `--force-with-lease` 推送，前提是你确认没有别人在使用这个分支 |
| 多人协作的公共分支 | **绝对禁止** | 会导致其他人仓库混乱，pull时出现大量冲突 |

```bash
# 当rebase因冲突中断时：
git status                        # 查看冲突文件
# ...手动解决冲突...
git add .
git rebase --continue             # 继续下一个rebase操作
# 或
git rebase --skip                 # 跳过当前commit
# 或
git rebase --abort                # 放弃整个rebase，回到之前状态
```

---

## 6. Force Push安全策略

### 6.1 --force-with-lease vs --force

| 参数 | 行为 | 安全等级 | 推荐 |
|------|------|---------|------|
| `git push --force` | 无条件覆盖远程分支的HEAD | 危险 | ❌ 不推荐 |
| `git push --force-with-lease` | 检查远程分支的最新状态是否与你上次fetch一致，一致才覆盖 | 安全 | ✅ 推荐 |
| `git push --force-if-includes` | 检查远程已在本地被合并，才允许force push | 更安全 | ✅ 推荐 |

```bash
# 不安全（无条件覆盖）
git push origin feature/login --force

# 安全（有租赁检查）
git push origin feature/login --force-with-lease

# 最安全（加包含检查）
git push origin feature/login --force-if-includes
```

> 💡 **`--force-with-lease` 的原理**：它会比较你本地的 `refs/remotes/origin/feature` 和远程仓库当前的 `refs/heads/feature` 是否一致。如果其他人已经推送了新提交到远程，你的refs记录落后，force push会被拒绝，从而避免覆盖他人的工作。

### 6.2 何时允许force push

| 允许的场景 | 原因 | 操作方式 |
|-----------|------|---------|
| 个人feature分支 | 只有你一个人用，改写历史不影响他人 | `--force-with-lease` |
| GitHub PR中的分支 | PR合并前，更新PR分支 | `--force-with-lease` |
| 修复CI失败后的commit | 如修复了CI配置后amend提交 | `--force-with-lease` |

| 禁止的场景 | 原因 |
|-----------|------|
| 多人协作的公共分支（main/develop） | 覆盖他人提交 |
| review中的PR分支（别人也可能在review分支上做修改） | 导致reviewer的仓库状态混乱 |
| 带保护规则的分支 | 平台（GitHub/GitLab）会阻止 |

> 🎯 **安全准则**：如果这条分支上不止你一个人在工作，**绝不用**force push。如果只有你一个人，始终使用 `--force-with-lease` 而非 `--force`。

---

## 7. Git Blame：代码溯源

### 7.1 基础用法

`git blame` 逐行显示文件的每一行是谁在什么时间修改的，是代码评审和问题排查的利器。

```bash
# 逐行查看文件溯源
git blame src/main/java/com/demo/UserService.java

# 输出示例：
# 8a2f8c9a (Su Xiangyu 2026-07-27 14:30:00 +0800 15) public User findById(Long id) {
# 3b4c5d6b (Li Si     2026-07-26 10:00:00 +0800 16)     if (id == null) {
# 1a2b3c4c (Zhang San 2026-07-25 16:00:00 +0800 17)         throw new IllegalArgumentException("id must not be null");
```

### 7.2 高级选项

```bash
# 限定行范围（查看某几行）
git blame -L 15,25 UserService.java

# 按时间过滤（只看某段时间之后的修改）
git blame --since="2026-06-01" UserService.java

# 显示作者的邮箱
git blame -e UserService.java

# 忽略空白变更（如格式化、缩进调整）
git blame -w UserService.java

# 忽略某个commit（如大规模重构变更）
git blame --ignore-rev 8a2f8c9 UserService.java

# 将忽略commit列表写入配置文件
git config blame.ignoreRevsFile .git-blame-ignore-revs
# .git-blame-ignore-revs 文件内容：
# 8a2f8c9a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e
```

> 💡 **使用场景**：
> 1. **谁改了这个bug**：`git blame` 找到引入问题的commit，然后 `git show <commit>` 查看详情
> 2. **代码评审**：了解某行代码的作者和修改时间，判断是否需要咨询
> 3. **引入 .git-blame-ignore-revs 文件**：当代码库经过大规模格式化（如统一缩进、import排序），用此文件标记这些"噪音commit"，让 blame 结果更准确

---

## 8. Git Tag：版本标记

### 8.1 轻量标签 vs 附注标签

| 类型 | 命令 | 是否含元数据 | 是否可签名 | 推荐 |
|------|------|-------------|-----------|------|
| **轻量标签** | `git tag v1.0.0` | 否，仅一个指针 | 否 | 个人使用、临时标记 |
| **附注标签** | `git tag -a v1.0.0 -m "发布v1.0.0"` | 是（作者、日期、消息） | 是 | 正式发布、生产部署 ✅ |

```bash
# 创建轻量标签
git tag v1.0.0
git tag v1.0.0 8a2f8c9          # 在指定commit上打标签

# 创建附注标签（推荐）
git tag -a v1.0.0 -m "发布用户服务v1.0.0：实现用户注册、登录、信息查询"

# 查看标签
git tag                          # 列出所有标签
git tag -l "v1.*"                # 列出匹配的标签（通配符）
git show v1.0.0                  # 查看标签详情（仅附注标签有完整信息）

# 删除本地标签
git tag -d v1.0.0
```

### 8.2 标签的推送与删除

```bash
# 推送单个标签到远程
git push origin v1.0.0

# 推送所有未推送的标签
git push origin --tags

# 删除远程标签（两种方式）
git push origin --delete tag v1.0.0
git push origin :refs/tags/v1.0.0

# 拉取远程标签（fetch会自动拉取）
git fetch --tags
```

> ⚠️ **标签与分支的区别**：标签是**不可移动的**，一旦创建就永远指向同一个commit。分支是可移动的。不要用标签做"当前版本"的概念——用分支。标签只用于**标记历史中的里程碑**。

### 8.3 签名标签

```bash
# 使用 GPG 签名创建标签（需要先配置GPG密钥）
git tag -s v1.0.0 -m "发布v1.0.0（已签名）"

# 验证签名标签
git tag -v v1.0.0
```

> 💡 签名标签在开源项目中尤为重要——它保证了标签的创建者确实是声称的那个人，防止假冒发布。

---

## 9. Git Worktree：并行工作区

### 9.1 为什么需要worktree

```
┌─────────────────────────────────────────────────┐
│  传统方式：切换分支 = 暂存/提交 + checkout + 恢复   │
│  致命问题：频繁切换导致context switch成本极高        │
├─────────────────────────────────────────────────┤
│  Worktree方式：每个分支有独立的工作目录，无需切换     │
│  main     → /project/main     （已打开）          │
│  hotfix   → /project/hotfix   （同时开发）         │
│  feature  → /project/feature  （并行处理）         │
└─────────────────────────────────────────────────┘
```

核心问题：你正在feature分支上开发复杂功能，突然需要修复main上的紧急bug。没有worktree，你需要：
1. `git stash` 或提交半成品
2. `git checkout main`
3. 修复bug
4. 切回feature
5. `git stash pop`

有了worktree，你只需要在另一个目录同时检出main分支。

### 9.2 基础用法

```bash
# 创建worktree（在../hotfix目录检出main分支）
git worktree add ../project-hotfix main

# 创建worktree并创建新分支
git worktree add ../project-feature-pay feature/pay

# 列出所有worktree
git worktree list
# /path/main-project       8a2f8c9 [main]
# /path/project-hotfix     3b4c5d6 [main]
# /path/project-feature    1a2b3c4 [feature/pay]

# 移除worktree
git worktree remove ../project-hotfix

# 清理worktree元数据
git worktree prune
```

### 9.3 实战场景

```bash
# 场景：feature分支开发到一半，需要修复生产bug

# 1. 在主项目目录中
cd /home/user/my-project

# 2. 创建worktree用于hotfix
git worktree add /home/user/temp-hotfix main

# 3. 在新的终端窗口中
cd /home/user/temp-hotfix

# 4. 创建hotfix分支
git checkout -b hotfix/pay-timeout

# 5. 修复、提交、推送
git add .
git commit -m "fix(payment): 修复支付超时未处理"
git push origin hotfix/pay-timeout

# 6. 回到主项目，继续feature开发
cd /home/user/my-project
git status   # 工作区完全没受影响！stash都不需要

# 7. 清理worktree
cd /home/user/temp-hotfix
git checkout main   # 切换回main
cd ..
git worktree remove /home/user/temp-hotfix
```

---

## 10. Aliases：高效别名

### 10.1 必装别名清单

```bash
git config --global alias.lg "log --oneline --graph --all --decorate"
git config --global alias.ll "log --oneline --all --decorate"
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.cm "commit -m"
git config --global alias.df diff
git config --global alias.dfc "diff --cached"
git config --global alias.unstage "restore --staged"
git config --global alias.last "log -1 HEAD"
git config --global alias.aa "add -A"
git config --global alias.prom "pull --rebase origin main"
git config --global alias.prod "pull --rebase origin develop"
```

**高价值自定义别名**：

```bash
# git undo = 软回退一个commit
git config --global alias.undo "reset --soft HEAD~1"

# git wip = 保存当前工作进度（Work In Progress）
git config --global alias.wip '!git add -A && git commit -m "WIP: $(date +%Y-%m-%d_%H:%M:%S)"'

# git unwip = 最近一个WIP退回工作区
git config --global alias.unwip '!git log -1 --oneline | grep -q "WIP:" && git reset --soft HEAD~1 || echo "Last commit is not a WIP"'

# git all = 查看所有分支的最近提交
git config --global alias.all "log --oneline --graph --all --decorate --simplify-by-decoration"

# git tree = 提交树（彩色）
git config --global alias.tree "log --graph --pretty=format:'%C(yellow)%h%C(reset) %C(bold green)%an%C(reset) %C(blue)%ar%C(reset) %C(red)%d%C(reset)%n%s'"
```

### 10.2 配置方式

别名可以用三种方式管理：

```bash
# 方式1：直接命令行（推荐）
git config --global alias.lg "log --oneline --graph --all --decorate"

# 方式2：编辑配置文件
git config --global --edit
# 在 [alias] 段中添加

# 方式3：直接编辑 ~/.gitconfig
# [alias]
#     lg = log --oneline --graph --all --decorate
#     st = status
```

---

## 11. Git Bisect：二分查找Bug源头

### 11.1 手动二分查找

场景：当前版本有bug，已知两周前是好的。需要在几百个commit中找到引入bug的那个commit。

```bash
# 1. 开始二分查找
git bisect start

# 2. 标记当前版本为bad（有bug）
git bisect bad

# 3. 标记某个已知的好的commit（两周前）
git bisect good v2.0.0
# Bisecting: 47 revisions left to test after this (roughly 6 steps)
# Git自动切换到中间的commit

# 4. 测试当前代码是否符合预期
# ...运行测试、编译、检查...

# 5. 根据结果标记
git bisect good   # 如果当前版本没问题
git bisect bad    # 如果当前版本有bug

# 6. 重复步骤4-5，每次范围缩小一半
# Git会在约 log2(N) 步后找到引入bug的commit

# 7. 找到后查看
git bisect log              # 查看bisect日志
git show $(git bisect view) # 查看引入bug的commit

# 8. 结束bisect
git bisect reset
```

### 11.2 自动化二分查找

如果项目有自动化测试脚本，可以让bisect全自动运行：

```bash
# 创建测试脚本 bisect-test.sh
cat > bisect-test.sh << 'EOF'
#!/bin/bash
mvn test -Dtest=PaymentServiceTest 2>/dev/null
# 返回0表示测试通过（good），非0表示失败（bad）
EOF
chmod +x bisect-test.sh

# 让bisect自动运行测试
git bisect start HEAD v2.0.0
git bisect run ./bisect-test.sh
# 输出：
# Bisecting: 47 revisions left to test after this (roughly 6 steps)
# ...
# 8a2f8c9a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e is the first bad commit
```

> 💡 **bisect run的退出码约定**：
> - 退出码 0：标记为 good
> - 退出码 125：跳过该commit（无法编译、测试环境异常等）
> - 退出码 1-127（非0非125）：标记为 bad

> 🎯 **Bisect实战建议**：
> 1. 先找一个**确定的好commit**（比如上一个release的tag）
> 2. 写一个可重复执行的测试脚本
> 3. 使用 `git bisect run` 全自动运行
> 4. 找到后 `git bisect log` 保存记录
> 5. 完成后务必 `git bisect reset` 回到正常状态

---

## 12. 完整实战：生产热修复→Cherry-pick→Tag发布

以下是一个完整的企业级应急流程。

### 场景设定

- **main**：生产分支（版本v2.3.0）
- **develop**：开发分支（正在开发v2.4.0功能）
- **问题**：生产环境发现支付超时异常，需紧急修复

### 第一步：拉取最新代码

```bash
# 确保本地main是最新状态
git checkout main
git pull --rebase origin main

# 从main创建hotfix分支
git checkout -b hotfix/payment-timeout
```

### 第二步：修复并测试

```bash
# 修复代码
# 编辑 src/main/java/com/demo/service/PaymentService.java

# 查看修改
git diff

# 原子提交（只修复一个问题）
git add src/main/java/com/demo/service/PaymentService.java
git commit -m "fix(payment): 修复支付超时未正确处理

- 增加超时回调处理逻辑
- 超时后自动发起退款
- 添加超时日志记录

Closes #1567"
```

### 第三步：推送到远程

```bash
# 首次推送，设置追踪
git push -u origin hotfix/payment-timeout

# 提PR/MR到main分支
# ... GitHub/GitLab操作 ...
```

### 第四步：合并到main

```bash
# 本地合并到main
git checkout main
git merge --no-ff hotfix/payment-timeout -m "merge: 合并支付超时hotfix到main"

# 打生产版本标签
git tag -a v2.3.1 -m "修复支付超时问题"

# 推送到远程
git push origin main
git push origin v2.3.1
```

### 第五步：同步到develop

```bash
# 切换到develop分支
git checkout develop
git pull --rebase origin develop

# cherry-pick修复commit
git cherry-pick -x main

# 推送到远程
git push origin develop
```

### 第六步：清理

```bash
# 删除本地hotfix分支
git branch -d hotfix/payment-timeout

# 删除远程hotfix分支
git push origin --delete hotfix/payment-timeout

# 查看最终状态
git lg
# * 1a2b3c4 (tag: v2.3.1, main) merge: 合并支付超时hotfix到main
# |\
# | * 8a2f8c9 (hotfix/payment-timeout) fix(payment): 修复支付超时未正确处理
# * | 9i0j1k2 (develop) chore: develop同步hotfix
# |/
# * ...
```

### 完整命令速查表

```bash
# ============ Hotfix流程 ============
git checkout main                            # 到main分支
git pull --rebase origin main               # 拉取最新
git checkout -b hotfix/payment-timeout      # 创建hotfix分支
# ...修复代码...
git add .                                    # 暂存
git commit -m "fix(payment): 修复支付超时"   # 提交
git push -u origin hotfix/payment-timeout    # 推送

# ============ 合并到main ============
git checkout main
git merge --no-ff hotfix/payment-timeout
git tag -a v2.3.1 -m "修复支付超时问题"
git push origin main --tags

# ============ 同步到develop ============
git checkout develop
git pull --rebase origin develop
git cherry-pick -x main
git push origin develop

# ============ 清理 ============
git branch -d hotfix/payment-timeout
git push origin --delete hotfix/payment-timeout
```

> 🎯 **企业级Git操作的三个核心原则**：
> 1. **绝不直接操作main/develop**：所有变更通过PR/MR + Code Review合并
> 2. **改写历史只限个人分支**：已推送的公共分支历史永远不rebae
> 3. **每个commit是一个原子逻辑单元**：方便cherry-pick、revert、bisect

---

> 🎯 **总结**：企业级Git操作不只是记命令，而是要建立"分支即工作流、提交即文档、版本即发布"的工程思维。掌握了cherry-pick、interactive rebase、bisect、worktree这些高阶工具，你就能在复杂的多分支协作场景中游刃有余，做到"不乱、不丢、不覆盖"。
