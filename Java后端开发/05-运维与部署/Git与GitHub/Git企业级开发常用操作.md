Git企业级开发常用操作

--------------------------------------------------------------------------------------------------------------------------------------
一、Git基础配置（首次使用/新环境必做）
1. 配置用户信息（关联提交记录）
    git config --global user.name "你的用户名"
    git config --global user.email "你的邮箱"
2. 配置默认编辑器（可选）
    git config --global core.editor "vim"
3. 配置别名（简化常用命令，可选）
    git config --global alias.st status
    git config --global alias.co checkout
    git config --global alias.br branch
    git config --global alias.cm commit
4. 查看配置信息
    git config --list
    git config user.name

--------------------------------------------------------------------------------------------------------------------------------------
二、仓库初始化与克隆
1. 本地初始化新仓库
    mkdir project && cd project
    git init
2. 克隆远程仓库（企业最常用）

# 基础克隆
git clone <远程仓库地址>

# 克隆指定分支
git clone -b <分支名> <远程仓库地址>

# 浅克隆（只拉取最新版本，加快速度，适用于大仓库）
git clone --depth 1 <远程仓库地址>

--------------------------------------------------------------------------------------------------------------------------------------
三、分支管理（企业开发核心）
1. 查看分支

# 本地分支
git branch

# 远程分支
git branch -r

# 所有分支（本地+远程）
git branch -a
2. 创建分支

# 基于当前分支创建
git branch <分支名>

# 创建并切换到新分支
git checkout -b <分支名>

# 基于远程分支创建本地分支
git checkout -b <本地分支名> origin/<远程分支名>
3. 切换分支
    git checkout <分支名>

# 新版Git推荐
git switch <分支名>
4. 合并分支（开发完成合并到主分支）

# 先切换到目标分支（如master/main）
git checkout master

# 合并开发分支（如dev）
git merge <开发分支名>

# 解决冲突后提交
git add .
git commit -m "merge: 解决冲突，合并dev分支"
5. 删除分支

# 删除本地分支（已合并）
git branch -d <分支名>

# 强制删除本地分支（未合并）
git branch -D <分支名>

# 删除远程分支
git push origin --delete <分支名>
6. 拉取远程分支更新
    git fetch origin
    git pull origin <分支名>

--------------------------------------------------------------------------------------------------------------------------------------
四、代码提交与推送（日常开发高频）
1. 查看工作区状态
    git status
    git status -s （简洁版）
2. 添加文件到暂存区

# 添加指定文件
git add <文件名>

# 添加所有修改/新增文件
git add .

# 添加所有修改（包括删除）
git add -A
3. 提交暂存区代码

# 基础提交
git commit -m "feat: 新增用户登录功能"

# 提交时修改最后一次提交（未推远程时用）
git commit --amend

# 跳过暂存区直接提交（仅修改已追踪文件）
git commit -am "fix: 修复登录验证bug"
4. 推送代码到远程仓库

# 推送当前分支到远程
git push origin <分支名>

# 首次推送绑定分支（后续可直接git push）
git push -u origin <分支名>
5. 撤销操作（开发中纠错）

# 撤销工作区修改（未add）
git checkout -- <文件名>

# 撤销暂存区修改（已add未commit）
git reset HEAD <文件名>

# 回退到指定版本（保留工作区）
git reset --soft <commit-id>

# 强制回退到指定版本（清空工作区）
git reset --hard <commit-id>

--------------------------------------------------------------------------------------------------------------------------------------
五、版本回溯与查看（排查问题必备）
1. 查看提交记录

# 基础日志
git log

# 简洁日志（一行显示）
git log --oneline

# 显示分支合并图
git log --graph --oneline --all

# 查看指定文件的提交记录
git log <文件名>

# 查看最近n条提交
git log -n 5
2. 查看版本差异

# 工作区与暂存区差异
git diff

# 暂存区与本地仓库差异
git diff --cached

# 两个版本之间的差异
git diff <commit-id1> <commit-id2>

# 两个分支之间的差异
git diff <分支1> <分支2>
3. 回到指定版本
    git checkout <commit-id>

# 回到最新版本
git checkout <当前分支名>

--------------------------------------------------------------------------------------------------------------------------------------
六、协作开发核心操作（多人团队必用）
1. 拉取远程最新代码（避免冲突）

# 基础拉取
git pull origin <分支名>

# 拉取并重新合并（解决冲突更友好）
git pull --rebase origin <分支名>
2. 解决合并冲突

# 冲突出现后，先查看冲突文件
git status

# 编辑冲突文件（删除<<<<<<<、=======、>>>>>>>标记，保留正确代码）

# 解决后提交
git add .
git commit -m "fix: 解决合并冲突"
3. 暂存工作区（临时切换分支时用）

# 暂存当前修改
git stash

# 查看暂存列表
git stash list

# 恢复最近一次暂存
git stash apply

# 恢复并删除暂存记录
git stash pop

# 删除指定暂存
git stash drop <stash@{0}>

# 清空所有暂存
git stash clear
4. 标签管理（发布版本时用）

# 创建标签（轻量标签）
git tag v1.0.0

# 创建带注释的标签（推荐）
git tag -a v1.0.0 -m "发布v1.0.0版本"

# 推送标签到远程
git push origin v1.0.0

# 推送所有标签
git push origin --tags

# 查看标签
git tag

# 删除本地标签
git tag -d v1.0.0

# 删除远程标签
git push origin --delete tag v1.0.0

--------------------------------------------------------------------------------------------------------------------------------------
七、.gitignore配置（企业开发规范）
1. 核心忽略规则（Java项目示例）

# 编译产物
*.class
target/

# 依赖包
node_modules/

# IDE配置
.idea/
.vscode/
*.iml

# 日志文件
*.log

# 系统文件
.DS_Store
Thumbs.db

# 配置文件（敏感信息）
application-dev.yml
*.properties
2. 生效方式

# 已提交的文件需先移除追踪
git rm --cached <文件名>

# 提交.gitignore
git add .gitignore
git commit -m "docs: 添加.gitignore忽略规则"

--------------------------------------------------------------------------------------------------------------------------------------
八、企业级开发规范（避坑重点）
1. 分支规范（主流Git Flow）

# 主分支：master/main（生产环境）

# 开发分支：develop（日常开发）

# 功能分支：feature/xxx（如feature/user-login）

# 修复分支：bugfix/xxx（如bugfix/login-validation）

# 发布分支：release/1.0.0

# 紧急修复分支：hotfix/xxx
2. 提交信息规范（Conventional Commits）

# 格式：<类型>: <描述>

# 类型：feat（新功能）、fix（修复）、docs（文档）、style（格式）、refactor（重构）、test（测试）、chore（构建）

# 示例：
git commit -m "feat: 新增用户注册接口"
git commit -m "fix: 修复注册时手机号验证错误"
3. 高频避坑点

# 不要直接推主分支，先提MR/PR

# 提交前先pull远程最新代码，避免冲突

# 敏感信息（密码、密钥）不要提交到仓库

# 大文件（如日志、包、视频）用Git LFS管理

# 定期清理本地无用分支和暂存

--------------------------------------------------------------------------------------------------------------------------------------
九、远程仓库操作（企业协作）
1. 查看远程仓库信息
    git remote -v
2. 添加远程仓库
    git remote add origin <远程仓库地址>
3. 修改远程仓库地址
    git remote set-url origin <新地址>
4. 拉取远程所有分支
    git fetch --all
5. 强制推送（慎用，仅本地版本正确时用）
    git push -f origin <分支名>

--------------------------------------------------------------------------------------------------------------------------------------
总结
1. 企业级Git开发核心是**分支规范、提交规范、冲突处理**，优先遵循团队既定流程，不擅自操作主分支
2. 日常高频操作：拉取（pull）、创建分支（checkout -b）、提交（commit）、推送（push）、合并（merge）、暂存（stash）
3. 避坑关键：提交前拉最新代码、敏感信息不上库、大文件用Git LFS、冲突解决后再推送
