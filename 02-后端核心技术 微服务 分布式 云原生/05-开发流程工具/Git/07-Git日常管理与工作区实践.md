# 07-Git 日常管理与工作区实践
> 工作区是 Git 的入口，也是开发者每天的战场——每日节奏、status/diff 解读、add -p 精准暂存、stash 进阶、log 高级、bisect、GC 维护

## 📚 目录
1. [Developer's Daily Git Rhythm](#1-developers-daily-git-rhythm)
2. [工作区卫生：status 输出解读](#2-工作区卫生status-输出解读)
3. [git diff 三种范围](#3-git-diff-三种范围)
4. [git add 精准暂存](#4-git-add-精准暂存)
5. [stash 进阶](#5-stash-进阶)
6. [commit 日常管理](#6-commit-日常管理)
7. [分支日常操作](#7-分支日常操作)
8. [Log Mastery：日志高级用法](#8-log-mastery日志高级用法)
9. [git bisect：二分法定位 Bug](#9-git-bisect二分法定位-bug)
10. [工作区清理与撤销](#10-工作区清理与撤销)
11. [.gitattributes 与仓库维护](#11-gitattributes-与仓库维护)
12. [完整日常工作流示例](#12-完整日常工作流示例)
13. [核心要点](#13-核心要点)
14. [参考来源](#14-参考来源)

## 1. Developer's Daily Git Rhythm

```text
Morning (09:00)   → git pull --rebase → git status → review yesterday's work
Mid-morning       → git add -p → git commit (per micro-task)
Lunch (12:00)     → git push (save morning work to remote)
Afternoon (14:00) → git pull --rebase (sync with team) → continue working
Late afternoon    → git commit → git push
Evening (17:30)   → git push final → create PR / update PR
```

| 时段 | 命令 | 目的 |
|------|------|------|
| 晨间 | `git pull --rebase` | 同步远程最新代码 |
| 开发中 | `git status` | 频繁检查工作区状态 |
| 提交前 | `git diff` / `git diff --staged` | 确认修改内容准确 |
| 微任务完成 | `git commit` | 按功能点粒度提交 |
| 午间/下班 | `git push` | 推送远程，避免本地丢失 |
| PR 前 | `git log --oneline` | 确认提交历史清晰 |
| 下班前 | `git stash list` | 检查遗留 stash |

> 🎯 **黄金法则**：提交频率 = 解决一个最小独立问题的单位。如果 commit message 里写了 "and"，说明这个提交应该拆分成两个。

## 2. 工作区卫生：status 输出解读

```bash
$ git status
On branch feature/user-login          # 当前分支
Your branch is ahead of 'origin/feature/user-login' by 2 commits.  # 领先远程 2 个
Changes not staged for commit:        # 已修改但未暂存
  modified: src/main/java/.../UserController.java
Changes to be committed:              # 已暂存等待提交
  new file:   src/main/java/.../dto/LoginRequest.java
Untracked files:                      # 未追踪文件
  .idea/workspace.xml
```

| 文件状态 | 显示位置 | 含义 |
|----------|---------|------|
| Untracked | `Untracked files` | 未追踪，需 `git add` 注册 |
| Modified (unstaged) | `Changes not staged for commit` | 已追踪有改动，未暂存 |
| Staged | `Changes to be committed` | 已暂存，将进入下次提交 |
| Unmodified | 不显示 | 与 HEAD 一致 |

```bash
git status --short
#  ?? new-file.java        ← 未跟踪
#  M modified-file.java    ← 右侧M = 工作区有修改（未暂存）
# M  staged-file.java      ← 左侧M = 已暂存
# MM both-staged.java      ← 两侧M = 暂存后又修改了
```

> ⚠️ **关注 Untracked files**：如果 `.idea/` 或 `target/` 出现在 Untracked 中，说明 `.gitignore` 配置有遗漏，立即修复。

## 3. git diff 三种范围

```bash
git diff                          # 工作区 vs 暂存区（未暂存的修改）
git diff --staged                 # 暂存区 vs HEAD（已暂存的修改）；同 --cached
git diff HEAD                     # 工作区 vs HEAD（所有未提交修改总和）
git diff --name-status            # 文件名列表（含类型 M/A/D）
git diff --stat                   # 行变化统计
git diff -w                       # 忽略空白差异
git diff <文件路径>                 # 对比特定文件
git diff --word-diff              # 单词粒度对比（长行变更/文档）

# 日常场景
git diff main..feature/login      # 对比两个分支
git diff HEAD~1 HEAD              # 检查某次提交改了什么
git diff v1.0.0 v1.1.0 -- pom.xml # 某文件在两版本间的变化
```

## 4. git add 精准暂存

```bash
git add OrderController.java          # 单个文件
git add src/main/java/.../service/    # 整个目录
git add *.java                        # 通配符
git add -u                            # 仅已跟踪文件的修改+删除（不含新增）
git add -A                            # 所有变更（新增+修改+删除）
git add -p                            # 逐块确认是否暂存 ⭐
```

### git add -p 交互选项

| 选项 | 含义 | 使用场景 |
|------|------|----------|
| `y` | 暂存当前 hunk | 该块改动属于本次提交 |
| `n` | 不暂存当前 hunk | 属于下次提交或不应提交 |
| `s` | 拆分成更小的块 | hunk 太大，内含多个独立改动 |
| `e` | 手动编辑当前 hunk | 最精细的控制，只选取部分行 |
| `q` | 退出，不再暂存 | 后续块都不需要 |
| `?` | 显示帮助 | 遗忘命令时 |

```bash
git add -p              # 确认暂存的内容
git diff --cached       # 复查即将提交的内容
git commit -m "feat: ..."
```

> 💡 `git add -p` 天然强制开发者审查每一块改动，能捕获大量低级错误，远胜无脑 `git add .`。

## 5. stash 进阶

```bash
git stash push -m "WIP: 用户注册接口，还剩参数校验未完成"   # 带描述
git stash push -p                    # 交互式选择暂存哪些
git stash push -u                    # 含未跟踪文件
git stash push -a                    # 含 .gitignore 忽略的文件
git stash push -m "仅暂存配置文件" -- src/main/resources/application.yml   # 指定文件
git stash push -m "排除配置" -- ":(exclude)*application.yml"               # 排除指定文件
git stash list                       # 查看列表（栈顺序）
git stash pop stash@{1}              # 恢复指定 stash
git stash apply                      # 恢复但不移除
git stash show -p stash@{0}          # 查看内容（不改动任何东西）
git stash show -p stash@{0} | git apply -     # 部分应用
git stash drop stash@{1}             # 删除指定
git stash clear                      # 清空所有
git stash branch feature/fix-stash-conflict stash@{0}   # 从 stash 创建分支（自动 pop）
```

| 场景 | 操作 |
|------|------|
| 临时切换分支审查问题 | `git stash` → 切走 → 回来 `pop` |
| 拉取代码时本地有未提交修改 | `git stash` + `git pull` + `git stash pop` |
| 实验性功能开发一半 | `git stash -m "experiment-X"` |
| 多个 WIP 同时进行 | 多次 `stash push -m "描述"` 区分 |
| 误 stash 后找回 | `git stash list` + `apply` |

> ⚠️ **Stash 风险**：存在本地 `.git/refs/stash`，**不随 push 推送**。重要 WIP 建议推到分支上存储。

## 6. commit 日常管理

### 6.1 修正最近一次提交

```bash
git commit --amend -m "feat: 新增用户注册接口（含参数校验）"   # 修正信息
git add forgot-file.java
git commit --amend --no-edit          # 追加文件（不改信息）
```

> ⚠️ `--amend` 红线：仅对**尚未推送**的提交使用；已 push 则需 `--force-with-lease`，**严禁在公共分支上执行**。

### 6.2 拆分提交

```bash
# 方法 1：reset --soft + 重新 add
git reset --soft HEAD~1        # 撤销提交，保留工作区和暂存区
git reset HEAD .               # 取消所有暂存
git add src/file1.java && git commit -m "feat: 实现功能A"
git add src/file2.java && git commit -m "feat: 实现功能B"

# 方法 2：交互式 rebase 拆分
git rebase -i HEAD~3           # 在要拆分的提交前标记 "edit"
git reset HEAD^                # 保留工作区，撤销该次提交
git add -p && git commit -m "part 1"
git add -p && git commit -m "part 2"
git rebase --continue
```

## 7. 分支日常操作

```bash
git branch feature/user-login                     # 基于当前 HEAD 创建
git checkout -b feature/user-login develop        # 基于指定分支创建
git switch develop                                # Git 2.23+ 新语法
git switch -c feature/payment                     # 创建并切换
git switch -                                      # 切换到上一个分支（cd - 的类比）
git branch -m old-name new-name                   # 重命名
git branch -d feature/old-feature                 # 删除（已合并）
git branch -D feature/abandoned-feature           # 强制删除（未合并）
git branch -v                                     # 含最后一次提交信息
git branch --merged                               # 已合并到当前分支的
git branch --no-merged                            # 未合并的
git branch feature/order 8a2f8c9                  # 基于指定 commit 创建
git push origin --delete feature/user-login       # 删除远程分支
git push origin :feature/user-login               # 冒号语法（推送空到远程）
```

## 8. Log Mastery：日志高级用法

### 8.1 自定义格式

```bash
git log --oneline
git log --graph --pretty=format:'%C(yellow)%h%C(reset) - %C(green)%an%C(reset) %C(blue)(%ar)%C(reset)%C(auto)%d%C(reset)%n %s' --all

# 配置为别名
git config --global alias.lg "log --graph --pretty=format:'...' --all"
```

| 占位符 | 含义 | 示例 |
|--------|------|------|
| `%h` | 简短 hash | `a1b2c3d` |
| `%an` | 作者名 | `Zhang San` |
| `%ar` | 相对时间 | `2 days ago` |
| `%s` | 提交标题 | `feat: 添加登录` |
| `%d` | 分支/标签引用 | `(HEAD -> main)` |
| `%C(color)` | 颜色设置 | `%C(red)` |

### 8.2 高级过滤

```bash
# 按时间
git log --since="2026-01-01" --until="2026-06-30"
git log --after="2 weeks ago"

# 按作者
git log --author="Zhang"

# 按文件
git log -- src/main/java/.../UserService.java      # 某文件的变更历史
git log --follow -- src/.../UserService.java       # 跟踪重命名
git log -L 1,20:UserService.java                   # 行范围历史

# 按提交信息
git log --grep="JIRA-1234"

# 按代码内容（pickaxe 搜索）⭐⭐
git log -S"findByEmail"              # 搜索出现/删除"findByEmail"的所有 commit
git log -G"password.*encrypt"        # 正则搜索代码变更
git log -S"// TODO: " --all          # 搜所有分支

# 统计与对比
git shortlog -sn --all --no-merges   # 每人提交数
git log main..feature                # feature 有而 main 没有的 commit
git log main...feature               # 两者各自独有的（对称差）
git log --left-right main...feature  # 标注每个 commit 属于哪边
```

> 💡 `git log -S "string"`（pickaxe）非常强大——找出所有**新增或删除**了该字符串的提交，即使 commit message 完全不相关。适用于"哪个提交引入了这段代码"的溯源。

## 9. git bisect：二分法定位 Bug

**工作原理**：v1.0.0 正常、v2.0.0 出 Bug、中间 200 个 commit——传统逐个回退最坏 200 次；bisect 二分查找最多 log₂(200) ≈ 8 次。

### 9.1 手动 bisect

```bash
git bisect start
git bisect bad HEAD           # 当前版本有 Bug
git bisect good v1.0.0        # 标记已知正常的版本
# Git 自动 checkout 中间版本 → 测试 → 标记
git bisect good               # 或 git bisect bad
# 重复直到：
# a1b2c3d is the first bad commit
git bisect reset              # 回到原始状态
```

### 9.2 自动 bisect（推荐）

```bash
# 测试脚本：有 bug 返回非 0（bad），正常返回 0（good）
cat > test.sh << 'EOF'
#!/bin/bash
mvn test -Dtest=PaymentBugTest
EOF
chmod +x test.sh

git bisect start HEAD v1.0.0
git bisect run ./test.sh      # Git 自动二分，无需人工干预
```

| 退出码约定 | 含义 |
|-----------|------|
| 0 | good（正常） |
| 125 | 跳过该 commit（无法编译/环境异常） |
| 1-127（非 0 非 125） | bad（有 Bug） |

```bash
git bisect skip         # 跳过无法测试的 commit
git bisect log          # 查看 bisect 轨迹
git bisect visualize    # 可视化剩余范围
```

> 💡 实战建议：先找确定的好 commit（上个 release 的 tag）→ 写可重复测试脚本 → `git bisect run` 全自动 → 找到后 `git bisect log` 保存记录 → 完成后务必 `git bisect reset`。

## 10. 工作区清理与撤销

```bash
# 撤销决策矩阵（完整版见 05）
git restore <file>                    # 没 add：恢复暂存区/HEAD 版本
git restore -s <commit> <file>        # 没 add：恢复到某 commit
git restore --staged <file>           # add 了：取消暂存
git restore .                         # 清空所有工作区修改

# git clean：清理未跟踪文件
git clean -n            # --dry-run，只显示不删除（先预览！）
git clean -f            # 删除文件
git clean -fd           # 删除文件+目录
git clean -fx           # 删除文件+被 .gitignore 忽略的文件
git clean -i            # 交互式清理
git reset --hard HEAD && git clean -fd    # 彻底重置（危险！）
```

> ⚠️ `git clean -fd` 会**永久删除**未跟踪文件！执行前务必 `git clean -n` 预览。

## 11. .gitattributes 与仓库维护

### 11.1 .gitattributes（跨平台文件处理）

```gitattributes
# .gitattributes — 放在项目根目录

# === 行尾处理（跨平台协作关键）===
*       text=auto                # 自动检测文本/二进制
*.java  text eol=lf              # Java 源码强制 LF
*.xml   text eol=lf              # XML/POM 强制 LF
*.yaml  text eol=lf
*.sh    text eol=lf
*.bat   text eol=crlf            # Windows 批处理保留 CRLF

# === 二进制文件 ===
*.jar   binary                   # 不 diff
*.class binary
*.png   binary
*.pdf   binary

# === 语言统计 ===
*.sql   linguist-language=SQL
*.vue   linguist-detectable
```

> 💡 Java 项目推荐组合：Windows `core.autocrlf true`、Mac/Linux `input`、项目 `.gitattributes: *.java text eol=lf`——三管齐下解决 CRLF/LF 战争。

### 11.2 .git 目录结构与 GC 维护

```bash
cat .git/HEAD                     # 符号引用
git symbolic-ref HEAD             # 解析引用
git rev-parse --short HEAD        # 当前 commit 短哈希
git cat-file -t a1b2c3d           # 对象类型
git cat-file -p a1b2c3d           # 对象内容
git ls-files --stage              # 暂存区文件
git count-objects -v              # 对象统计

# GC 维护
git gc
git gc --aggressive               # 更激进优化（大型仓库季度执行）
git gc --prune=now                # 立即删除所有不可达对象
git fsck                          # 完整性检查（dangling commit/blob）
git fsck --lost-found             # 找回悬空对象
git maintenance start             # Git 2.31+：后台自动维护
```

| GC 自动触发时机 | 阈值 |
|-----------------|------|
| 松散对象数 | > 7000（`git gc --auto`） |
| pack 文件数 | > 50 |
| 距上次 gc | 超 30 天 |

## 12. 完整日常工作流示例

```text
Morning (08:55)
  cd ~/workspace/my-project
  git status
  git checkout develop && git pull --rebase
  git checkout -b feature/PROJ-456-order-export

Mid-Morning (09:30-11:00)
  git add ExportService.java
  git commit -m "feat(order-service): 新增导出订单CSV核心逻辑 #PROJ-456"
  git diff --staged               # 提交前审查
  git commit --amend --no-edit    # 发现遗漏，追加修正

Noon (11:45)
  git push -u origin feature/PROJ-456-order-export

Afternoon (14:00)
  git fetch origin develop
  git rebase origin/develop       # 冲突 → 解决 → git rebase --continue
  git push

Late Afternoon (16:30)
  git log --oneline               # 审查提交历史
  git rebase -i HEAD~3            # 整理历史
  git push --force-with-lease     # rebase 后推送
  创建 PR

Next Day (Review & Merge)
  git commit -m "fix: 修复PR评审指出的空指针问题 #PROJ-456"
  git push
  # 合并后
  git checkout develop && git pull --rebase
  git branch -d feature/PROJ-456-order-export

End of Day (17:45)
  git status                      # 有未提交？
  git stash list                  # 有遗留 stash？
  git remote prune origin         # 清理远程已删分支
```

> 🎯 **核心要义**：优秀的日常管理在于建立**可重复的操作节律**——每天遵循相同节奏，将 `git pull --rebase → git add -p → git commit → git push` 变为肌肉记忆。

## 13. 核心要点

> 🎯 **核心要点**：
> - 每日节奏五节点：晨间同步 → 微任务提交 → 午间备份 → 午后 rebase → 晚间 PR；
> - status 两列标记、diff 三种范围、add -p 六选项是工作区卫生三件套；
> - stash 是"临时移走"，有 4 个进阶参数（-p/-u/-a/指定文件）与 branch 恢复法；
> - log 高阶三件套：格式化占位符、pickaxe（-S/-G）、对称差（...）；
> - bisect 自动二分（run 脚本 + 退出码 125 跳过）是定位回归的最快路径；
> - GC 阈值（7000/50/30 天）+ `git maintenance` 保持仓库健康。

## 14. 参考来源

- [Pro Git Book：Git 工具（stash/log/bisect）](https://git-scm.com/book/zh/v2/Git-工具-储藏与清理)
- [Git 官方文档：git-bisect / git-stash / git-log](https://git-scm.com/docs)
- [gitattributes 官方文档](https://git-scm.com/docs/gitattributes)

---

**下一模块**：[08-Git钩子自动化与定制](08-Git钩子自动化与定制.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
