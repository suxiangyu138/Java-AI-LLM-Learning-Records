# Git 命令速查（Java 后端开发版）

> **定位**：Java 后端日常 Git 工作流速查
> **原则**：只记高频操作，复杂场景给解决方案

---

## 一、Git 三区模型（核心认知）

```
工作区(Working)  →  git add  →  暂存区(Staging)  →  git commit  →  仓库(Repository)
      ↑                              ↑                                  ↓
      └──────────── git checkout/restore ───────────────────────── git push → 远程(Remote)
```

---

## 二、日常工作流（每天用）

```bash
# 1. 看状态
git status

# 2. 看改动内容
git diff                    # 工作区 vs 暂存区
git diff --cached           # 暂存区 vs 仓库
git diff main..feature      # 两分支差异

# 3. 添加文件
git add src/UserService.java
git add -p                  # 交互式选择改动（推荐）

# 4. 提交
git commit -m "feat: 新增用户手机号登录"

# 5. 推送
git push
git push -u origin feature/login   # 首次推送新分支

# 6. 拉取
git pull                     # = fetch + merge
git pull --rebase            # = fetch + rebase（推荐）
```

---

## 三、分支管理

```bash
# 查看分支
git branch                   # 本地
git branch -r                # 远程
git branch -a                # 全部

# 创建分支
git checkout -b feature/payment     # 创建并切换
git switch -c feature/payment       # 新版写法

# 切换分支
git checkout main
git switch main                     # 新版写法

# 删除分支
git branch -d feature/payment       # 本地
git push origin --delete feature/payment  # 远程

# 合并分支
git merge feature/payment
git merge --no-ff feature/payment   # 保留分支历史

# 变基
git rebase main                     # 把当前分支变基到 main 上
```

---

## 四、撤销操作（急救包）

```bash
# 撤销工作区改动（未 add）
git restore src/file.java
git checkout -- src/file.java       # 旧写法

# 撤销暂存区（已 add，未 commit）
git restore --staged src/file.java
git reset HEAD src/file.java        # 旧写法

# 撤销最近一次 commit（保留改动）
git reset --soft HEAD~1

# 撤销最近一次 commit（放弃改动）
git reset --hard HEAD~1

# 撤销某次 commit（生成反向 commit）
git revert <commit-hash>

# 回到某个历史状态（临时查看）
git checkout <commit-hash>

# 强制本地匹配远程（⚠️ 本地改动丢失）
git fetch origin
git reset --hard origin/main
```

---

## 五、查看历史

```bash
# 简洁日志
git log --oneline -20

# 图形式日志
git log --graph --oneline --all --decorate

# 看具体改了什么
git log -p -2

# 看某个文件的改动历史
git log -p -- src/UserService.java

# 看谁改的每一行
git blame src/UserService.java

# 看某行最近一次修改
git log -L 100,120:src/UserService.java

# 搜索 commit message
git log --all --grep="支付"

# 搜索代码变更
git log -S "findByPhone" -- src/
```

---

## 六、暂存与恢复

```bash
# 暂存当前改动
git stash
git stash save "WIP: 支付模块未完成"

# 查看暂存列表
git stash list

# 恢复最近一次暂存
git stash pop

# 恢复但不删除 stash
git stash apply stash@{0}

# 删除暂存
git stash drop stash@{0}
git stash clear           # 清空全部
```

---

## 七、远程仓库

```bash
# 查看远程地址
git remote -v

# 添加远程
git remote add upstream https://github.com/original/repo.git

# 拉取远程分支
git fetch origin

# 删除本地已不存在的远程追踪
git remote prune origin
```

---

## 八、合并 vs 变基

| 操作 | 结果 | 适用 |
|---|---|---|
| `git merge` | 生成 merge commit，保留分支拓扑 | 公共分支、多人协作 |
| `git rebase` | 变基到目标分支，历史线性整洁 | 个人分支整理后提交 |

```bash
# 标准 rebase 工作流
git checkout feature
git rebase main
# 有冲突就解决 → git add → git rebase --continue
git checkout main
git merge feature
```

---

## 九、冲突解决

```bash
# 发生冲突时
# 1. 看哪些文件冲突
git status

# 2. 冲突文件内容：
# <<<<<<< HEAD
# 你的改动
# =======
# 别人的改动
# >>>>>>> branch-name

# 3. 手动编辑解决后
git add resolved_file.java
git merge --continue   # 或 git rebase --continue

# 4. 放弃合并/变基
git merge --abort
git rebase --abort
```

---

## 十、标签管理

```bash
# 创建标签
git tag v1.0.0
git tag -a v1.0.0 -m "发布 v1.0.0"

# 推送标签
git push origin v1.0.0
git push --tags              # 推送所有

# 删除标签
git tag -d v1.0.0
git push origin --delete v1.0.0
```

---

## 十一、常用高级操作

```bash
# 修改最后一次 commit message
git commit --amend -m "新的 message"

# 把漏掉的文件追加到上次 commit
git add forgotten_file.java
git commit --amend --no-edit

# 把一个 commit 应用到当前分支
git cherry-pick <commit-hash>

# 交互式 rebase（合并/删除/重排 commit）
git rebase -i HEAD~3
# pick   → 保留
# squash → 合并到上一个
# reword → 改 message
# drop   → 删除
```

---

## 十二、Conventional Commit 规范

```
feat: 新功能
fix: 修复 bug
docs: 文档
refactor: 重构（不改变功能）
test: 测试相关
chore: 构建/工具/依赖
style: 格式（不影响代码运行）
perf: 性能优化

示例：
feat: 新增支付回调接口
fix: 修复订单金额计算精度丢失
refactor: 提取公共校验逻辑到 BaseController
docs: 更新 README 部署说明
```

---

## 十三、.gitignore 模板（Java 项目）

```gitignore
# Maven
target/
*.jar
*.war

# IDE
.idea/
*.iml
.vscode/
.settings/
.project
.classpath

# 日志
*.log
logs/

# 环境配置（敏感）
application-prod.yml
.env
*.pem

# OS
.DS_Store
Thumbs.db

# 临时文件
*.swp
*.swo
*~
```

---

## 十四、极简总结

```
日常 = status → diff → add → commit → push
分支 = checkout -b → merge/rebase → branch -d
撤销 = restore（未add）/ reset HEAD~1（已commit）
存草稿 = stash / stash pop
查历史 = log --oneline / blame / log -S "keyword"
冲突 = 手动改 → add → merge/rebase --continue
规范 = feat/fix/docs/refactor 前缀
救命 = git reflog（找回"丢失"的 commit）
```
