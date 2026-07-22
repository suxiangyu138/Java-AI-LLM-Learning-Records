# Git学习路线 面试宝典
> 基于Git学习路线课程大纲，全面覆盖Git面试高频考点

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

### Q1: Git和SVN的区别是什么？（面试高频）
**A:** 
- **架构**: Git是分布式版本控制系统，每个开发者本地有完整仓库；SVN是集中式版本控制系统，依赖中央仓库
- **网络**: Git可在离线环境工作，提交、查看历史都无需联网；SVN必须联网才能操作
- **单点故障**: Git无单点故障，每个仓库都是完整备份；SVN中央仓库宕机则团队无法工作
- **分支**: Git分支操作轻量快速（文件指针），秒级创建；SVN分支是完整目录拷贝，速度慢
- **速度**: Git大部分操作在本地执行，速度极快；SVN需频繁网络通信

### Q2: Git的四⼤工作区域和流转流程是什么？（高频）
**A:** 
四大区域：
1. **工作区（Working Directory）**: 实际存放代码的目录
2. **暂存区（Stage/Index）**: 临时保存修改，位于 `.git/index`
3. **本地仓库（Repository）**: `.git` 目录，存储所有提交历史
4. **远程仓库（Remote Repository）**: 远程服务器上的仓库

流转流程：
```
工作区 → git add → 暂存区 → git commit → 本地仓库 → git push → 远程仓库
```

### Q3: Git的必要配置有哪些？
**A:**
```bash
# 必须配置
git config --global user.name "Your Name"     # 提交者名称
git config --global user.email "your@email.com" # 提交者邮箱

# 建议配置
git config --global core.autocrlf input       # Windows换行符转换
git config --global core.quotepath false      # 中文文件名显示
git config --global init.defaultBranch main   # 默认分支名
git config --global alias.st status           # 别名简化命令

# 查看配置
git config --global --list
git config user.name
```

### Q4: 如何创建本地仓库和克隆远程仓库？
**A:**
```bash
# 本地初始化新仓库
git init my-project
cd my-project

# 克隆远程仓库
git clone https://github.com/user/repo.git
git clone git@github.com:user/repo.git  # SSH方式

# 克隆指定分支
git clone -b develop git@github.com:user/repo.git

# 克隆时重命名目录
git clone git@github.com:user/repo.git my-app
```

### Q5: Git的日常高频操作命令有哪些？
**A:**
```bash
git status              # 查看工作区状态
git add .               # 添加所有修改到暂存区
git add <file>          # 添加指定文件
git commit -m "msg"     # 提交暂存区内容
git commit -am "msg"    # 添加并提交（跳过add，仅跟踪过的文件）
git log                 # 查看提交历史
git log --oneline       # 简洁查看
git log --graph         # 图形化查看
git diff                # 查看工作区与暂存区差异
git diff --staged       # 查看暂存区与仓库差异
```

### Q6: 什么是SSH免密配置？如何配置？
**A:** 配置SSH密钥后，推送代码到远程仓库无需每次输入密码。
```bash
# 1. 生成SSH密钥对
ssh-keygen -t rsa -b 4096 -C "your@email.com"

# 2. 启动SSH代理
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_rsa

# 3. 查看公钥
cat ~/.ssh/id_rsa.pub

# 4. 复制公钥内容，添加到远程仓库（GitHub/Gitee/GitLab）
# GitHub: Settings → SSH and GPG keys → New SSH key
# Gitee: 设置 → SSH公钥

# 5. 测试连接
ssh -T git@github.com
# 成功输出: Hi username! You've successfully authenticated...
```

### Q7: Git分支的核心概念和命令？
**A:** 分支是Git的核心功能，允许并行开发互不干扰。
```bash
# 查看分支
git branch                   # 查看本地分支
git branch -r                # 查看远程分支
git branch -a                # 查看所有分支

# 创建和切换分支
git branch feature-login     # 创建分支
git checkout feature-login   # 切换分支
git checkout -b feature-login # 创建并切换

# 合并和删除
git merge feature-login      # 合并分支到当前分支
git branch -d feature-login  # 删除已合并的分支
git branch -D feature-login  # 强制删除（即使未合并）
```

### Q8: 什么是版本回退？如何操作？
**A:**
```bash
# 查看提交历史获取commit id
git log --oneline

# 回退到指定版本（本地）
git reset --soft HEAD~1       # 保留工作区和暂存区
git reset --mixed HEAD~1      # 保留工作区，清空暂存区
git reset --hard <commit-id>  # 完全回退到指定版本

# 回退已推送的提交（安全方式）
git revert <commit-id>        # 创建反做提交
git push origin main          # 推送回退

# 搜索所有历史（包括已重置的）
git reflog                    # 引用日志，可用于恢复
```

### Q9: 如何解决代码冲突？
**A:** 当两个分支修改同一文件同一位置时产生冲突。
**解决步骤**:
1. `git status` 查看冲突文件
2. 编辑冲突文件，解决冲突标记 `<<<<<<<`、`=======`、`>>>>>>>`
3. `git add <file>` 标记已解决
4. `git commit` 完成合并提交

**冲突预防**:
- 经常pull保持代码最新
- 使用 `git pull --rebase` 减少合并提交
- 模块化拆分，减少多人修改同一文件

### Q10: 什么是Gitee和GitHub？有什么区别？
**A:**
| 特性 | GitHub | Gitee（码云） |
|------|--------|---------------|
| 服务器位置 | 美国 | 中国 |
| 访问速度 | 慢（需代理） | 快 |
| 私有仓库 | 有限制（免费版） | 免费无限制 |
| 社区生态 | 全球最大，开源项目多 | 国内为主 |
| CI/CD | GitHub Actions | Gitee Go |
| 企业用户 | GitHub Enterprise | Gitee企业版 |
| 推荐场景 | 开源项目 | 国内企业开发 |

### Q11: 如何撤销暂存区的修改？
**A:**
```bash
# 撤销指定文件的暂存
git restore --staged <file>
# 或旧语法
git reset HEAD <file>

# 撤销工作区的修改（危险！会丢失修改）
git restore <file>
# 或旧语法
git checkout -- <file>

# 撤销所有暂存区
git reset HEAD .
git restore --staged .
```

### Q12: IDEA中如何集成Git操作？
**A:** IDEA集成了完整的Git图形化操作：
- **VCS → Git → Commit**: 提交代码
- **VCS → Git → Push**: 推送远程
- **VCS → Git → Pull**: 拉取更新
- **VCS → Git → Branches**: 分支管理
- **VCS → Git → Merge Changes**: 合并操作
- **VCS → Git → Show History**: 查看历史
- **VCS → Git → Uncommitted Changes**: 查看未提交修改
- **右键→Git→Compare with Branch**: 分支对比

快捷键: `Ctrl+K`(提交)、`Ctrl+Shift+K`(推送)、`Ctrl+T`(拉取)

### Q13: 什么是主分支、开发分支、热修复分支？
**A:**
- **main/master（主分支）**: 线上稳定版本，只能通过合并方式更新，需代码审查
- **develop/dev（开发分支）**: 日常集成分支，功能分支合并到此处
- **feature（功能分支）**: 从develop分出，开发完成后合并回develop
- **hotfix（热修复分支）**: 从main分出，修复紧急问题后合并回main和develop
- **release（发布分支）**: 从develop分出，准备发布版本，修复微调后合并到main

### Q14: 如何查看Git提交历史？
**A:**
```bash
git log                    # 完整历史
git log --oneline          # 单行显示
git log --graph --all      # 图形化显示所有分支
git log -p                 # 显示每次提交的diff
git log --stat             # 显示文件变更统计
git log --author="name"    # 按作者过滤
git log --since="2 days ago" # 按时间过滤
git log --grep="bug"       # 按提交信息搜索
git log --oneline -5       # 显示最近5条
```

### Q15: 团队协作开发基本流程？
**A:**
1. 从远程仓库clone或pull最新代码
2. 从develop创建功能分支 `git checkout -b feature/xxx develop`
3. 在功能分支上开发，小粒度提交
4. 开发完成：push功能分支到远程
5. 创建Pull Request/Merge Request
6. 代码审查（Code Review）
7. 通过后合并到develop分支
8. 定期从develop更新main（版本发布）

---

## 二、深度原理剖析（8-12题）

### Q1: Git的分支原理是什么？为什么Git分支操作如此轻量？
**A:** Git分支本质上是指向Commit对象的指针（一个包含40位SHA-1哈希值的文件），存储在 `.git/refs/heads/` 目录下。
- 创建分支 = 创建一个新的指针文件（40字节），开销极小
- 切换分支 = 更新HEAD指向，更新工作区文件
- 合并分支 = 创建新Commit指向两个父Commit（Three-Way Merge）
- 删除分支 = 删除指针文件，不影响提交历史

对比SVN：
- SVN分支是完整目录拷贝，包含所有文件
- Git分支仅是40字节的指针引用，所以快如闪电

### Q2: Git如何存储数据（快照 vs 差异）?
**A:** Git存储的是快照（Snapshot）而非差异（Delta）：
- **提交时**: Git为所有文件创建快照（实际是Blob对象的引用），未变化文件复用之前的引用
- **存储方式**: 每个Commit包含一个Tree对象，记录所有文件的当前状态
- **压缩**: Git会定期进行GC（垃圾回收），将松散对象打包为pack文件，进行增量压缩存储
```
提交1: file1(v1), file2(v1)
提交2: file1(v2), file2(v1)  → file2复用提交1的快照
提交3: file1(v2), file2(v2)  → file1复用提交2的快照
```

### Q3: 什么是HEAD？detached HEAD状态是什么？
**A:** HEAD是指向当前分支的引用，默认指向 `ref: refs/heads/main`。当检出到某个具体的Commit而不是分支时，进入detached HEAD状态：
```bash
# 进入detached HEAD状态
git checkout <commit-hash>

# 此时HEAD直接指向提交，而不是分支
cat .git/HEAD  # 输出: <commit-hash>（而非 ref: refs/heads/branch）

# detached HEAD状态下创建的提交可能丢失
# 解决方案：创建分支保存提交
git checkout -b new-branch
```

### Q4: git merge的三种合并算法是什么？
**A:**
1. **Fast-Forward Merge**: 当当前分支是目标分支的直接祖先时，直接移动指针，不创建合并提交
   ```bash
   git merge feature      # 默认快进
   git merge --no-ff feature  # 强制创建合并提交
   ```

2. **Three-Way Merge**: 当两个分支有分叉历史时，Git找到两个分支的共同祖先，结合两个分支的修改进行合并
   ```
   共同祖先(C1) → 当前分支(C2)
   共同祖先(C1) → 待合并分支(C3)
   C2和C3的差异合并创建新提交C4
   ```

3. **Recursive Merge**: Three-Way Merge的增强版，当存在多个共同祖先时递归合并

### Q5: git pull --rebase和git pull的区别？
**A:**
```bash
# 普通pull（fetch + merge）
git pull origin develop
# 等同于：
git fetch origin develop
git merge origin/develop

# rebase方式pull（fetch + rebase）
git pull --rebase origin develop
# 等同于：
git fetch origin develop
git rebase origin/develop
```

**区别**:
| 方式 | 结果 | 历史记录 | 适用场景 |
|------|------|----------|----------|
| `git pull` | 创建合并提交 | 保留分叉历史 | 多人协作公共分支 |
| `git pull --rebase` | 线性提交历史 | 整洁无分叉 | 个人/功能开发分支 |

### Q6: Git的.git目录结构和内容？
**A:**
```
.git/
├── HEAD               # 指向当前分支的引用
├── config             # 仓库配置（用户名、远程仓库等）
├── description        # 仓库描述
├── index              # 暂存区（staging area）
├── refs/              # 引用目录
│   ├── heads/         # 本地分支
│   │   ├── main      # 分支指针（存储提交SHA-1）
│   │   └── develop
│   ├── remotes/       # 远程分支
│   │   └── origin/
│   │       ├── main
│   │       └── HEAD
│   └── tags/          # 标签
├── objects/           # 对象存储
│   ├── 12/           # SHA-1前两位为目录名
│   │   └── abc456... # 实际对象文件
│   ├── pack/          # 打包后的对象
│   │   └── pack-*.pack
│   └── info/          # 对象信息
├── logs/              # 引用日志（reflog）
│   ├── HEAD
│   └── refs/
│       └── heads/
│           └── main
└── hooks/             # 钩子脚本
    ├── pre-commit.sample
    ├── commit-msg.sample
    └── post-update.sample
```

### Q7: CI/CD中Git的整合原理？
**A:** CI/CD通过webhook与Git仓库集成：
1. 开发者push代码到远程仓库
2. 远程仓库触发webhook到CI服务器
3. CI服务器（Jenkins/GitHub Actions/GitLab CI）开始构建
4. 构建过程涉及Git操作（checkout、merge、tag）
5. 可根据分支策略定义不同流水线：
   - feature分支: 仅运行测试
   - develop分支: 构建+测试+部署到测试环境
   - main分支: 构建+测试+安全扫描+部署到生产
   - tag: 发布版本并部署

### Q8: Cherry-pick的原理和应用场景？
**A:** `cherry-pick` 将其他分支的特定提交应用到当前分支：
```bash
# 应用单个提交
git cherry-pick <commit-hash>

# 应用多个提交
git cherry-pick <hash1> <hash2>

# 应用一段范围的提交
git cherry-pick <hash1>..<hash3>

# 仅暂存修改但不提交
git cherry-pick -n <commit-hash>
```

**应用场景**:
- hotfix修复需要同时应用到main和develop
- 从废弃分支提取有用的提交
- 选择性将某功能的部分提交移到其他版本

---

## 三、实战场景题（6-10题）

### Q1: 开发到一半需要切换到其他分支处理紧急Bug
**A:**
```bash
# 场景：当前在feature分支开发，线上出现紧急Bug
# 方案1：使用stash
git stash -u             # 保存当前工作（-u包含未跟踪文件）
git checkout -b hotfix/urgent-bug main  # 创建hotfix分支
# ... 修复Bug ...
git add . && git commit -m "hotfix: 修复紧急Bug"
git checkout feature      # 回到功能分支
git stash pop             # 恢复工作进度

# 方案2：临时提交（适合修改较大）
git add .
git commit -m "WIP: feature开发中"
git checkout main -b hotfix/urgent-bug
# ... 修复Bug并提交 ...
git checkout feature
git reset HEAD~1          # 撤销临时提交，保留修改
```

### Q2: 误执行了git reset --hard，如何恢复？
**A:**
```bash
# 场景：误操作丢失了提交
# 方案1：使用reflog（前提是提交已被记录到Git数据库）
git reflog
# 输出:
# abc1234 HEAD@{0}: reset: moving to HEAD~2
# def5678 HEAD@{1}: commit: 重要功能实现
# ghi9012 HEAD@{2}: commit: 修复Bug
git reset --hard def5678  # 恢复到丢失的提交

# 方案2：使用git fsck（reflog也过期了）
git fsck --lost-found     # 查找悬空对象
# Checking object directories: 100% (256/256), done.
# dangling commit xyz7890
git show xyz7890          # 查看提交内容
git merge xyz7890         # 或 git cherry-pick
```

### Q3: 多人协作时解决远程冲突
**A:**
```bash
# 场景：其他人在之前推送了代码，导致push被拒绝
git push origin main
# 错误: ! [rejected] main -> main (fetch first)
# error: failed to push some refs

# 正确的解决流程：
# 1. 拉取远程代码
git pull --rebase origin main
# 或 git fetch origin && git rebase origin/main

# 2. 如果有冲突，解决冲突
# 编辑冲突文件...
git add <resolved-file>
git rebase --continue

# 3. 推送代码
git push origin main
```

### Q4: 如何撤销已推送到远程的提交？
**A:**
```bash
# 方案1：使用revert（推荐，安全）
git revert <commit-hash>      # 创建反做的提交
git push origin main          # 推送

# 方案2：revert一段连续提交
git revert <oldest>..<newest>  # 按顺序反做多个提交
git push origin main

# 方案3：reset后force push（仅当团队确认）
git reset --hard <target-commit>
git push --force-with-lease origin main
# ⚠️ 注意：force push会重写远程历史，慎用！
```

### Q5: 开发完成如何创建Pull Request？
**A:**
```bash
# 1. 确保功能分支已推送
git checkout -b feature/user-login develop
# ... 开发、提交 ...
git push -u origin feature/user-login

# 2. 在GitHub/Gitee创建PR/MR
# GitHub: Pull Requests → New Pull Request
# base: develop ← compare: feature/user-login

# 3. PR模板填写
# ## 变更内容
# - 实现用户登录功能
# - 添加JWT Token认证
# - 优化密码加密存储
#
# ## 测试覆盖
# - 添加登录接口单元测试
# - 测试密码加密验证
#
# ## 关联Issue
# Closes #42

# 4. 代码审查通过后合并
# 可选择合并策略：
# - Create a merge commit（保留完整历史）
# - Squash and merge（压缩提交）
# - Rebase and merge（线性历史）
```

### Q6: 误将代码提交到错误分支如何处理？
**A:**
```bash
# 场景：在main分支上做了开发提交，应该是feature分支

# 方案1：cherry-pick（推荐）
# 1. 记录当前提交
git log --oneline -1
# abc1234 feat: 用户登录功能

# 2. 回退main分支
git reset --hard HEAD~1

# 3. 在正确分支应用提交
git checkout feature
git cherry-pick abc1234

# 方案2：使用rebase
git branch feature       # 在现有提交上创建分支
git reset --hard HEAD~1  # main回退
git checkout feature     # 转到feature分支继续开发

# 方案3：使用reset
git checkout -b feature  # 在main的提交基础上创建分支
git branch -f main HEAD~1 # 强制移动main指针回退
```

### Q7: 如何查看某行代码是谁在什么时候修改的？
**A:**
```bash
# git blame: 查看每行代码的最后修改者
git blame src/main/java/com/example/UserService.java

# 输出格式：
# abc1234 (张三 2024-01-15 14:30:00 +0800 12) public User getUser(Long id) {
# def5678 (李四 2024-01-20 09:15:00 +0800 13)     return userRepository.findById(id)
# def5678 (李四 2024-01-20 09:15:00 +0800 14)         .orElseThrow(() -> new UserNotFoundException(id));
# abc1234 (张三 2024-01-15 14:30:00 +0800 15) }

# 查看指定行范围
git blame -L 10,20 UserService.java

# 显示更详细的信息
git blame -w -C -C -C UserService.java  # 跟踪代码移动

# 查看某次提交的详细信息
git show abc1234
```

---

## 四、手写代码/配置文件题（5-8题）

### Q1: 完整的SSH配置流程脚本
**A:**
```bash
#!/bin/bash
# Git SSH免密配置脚本

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "=== Git SSH免密配置脚本 ==="

# 1. 检查是否已有SSH密钥
if [ -f ~/.ssh/id_rsa ]; then
    echo -e "${GREEN}检测到已有SSH密钥${NC}"
else
    echo "生成新的SSH密钥..."
    read -p "请输入邮箱地址: " email
    ssh-keygen -t rsa -b 4096 -C "$email" -f ~/.ssh/id_rsa -N ""
    echo -e "${GREEN}SSH密钥已生成${NC}"
fi

# 2. 启动SSH代理
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_rsa

# 3. 显示公钥
echo ""
echo "=== 你的SSH公钥如下 ==="
echo -e "${RED}请复制以下内容添加到GitHub/Gitee/GitLab${NC}"
echo "========================================"
cat ~/.ssh/id_rsa.pub
echo "========================================"

# 4. 配置SSH config（可选）
if [ ! -f ~/.ssh/config ]; then
    cat > ~/.ssh/config << 'EOF'
# GitHub
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_rsa
    IdentitiesOnly yes

# Gitee
Host gitee.com
    HostName gitee.com
    User git
    IdentityFile ~/.ssh/id_rsa
    IdentitiesOnly yes
EOF
    echo -e "${GREEN}SSH config已配置${NC}"
fi

# 5. 测试连接
echo ""
echo "测试GitHub连接..."
ssh -T git@github.com -o StrictHostKeyChecking=no
echo "测试Gitee连接..."
ssh -T git@gitee.com -o StrictHostKeyChecking=no

echo ""
echo -e "${GREEN}配置完成！${NC}"
```

### Q2: Git全局配置优化
**A:**
```bash
#!/bin/bash
# Git全局配置优化脚本

# 基本配置
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 别名配置（提高效率）
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.df diff
git config --global alias.lg "log --graph --oneline --decorate --all"
git config --global alias.last "log -1 HEAD"
git config --global alias.unstage "reset HEAD --"
git config --global alias.undo "reset --soft HEAD~1"
git config --global alias.tree "log --graph --pretty=format:'%C(yellow)%h%Creset %s %Cgreen(%an)%Creset'"

# 换行符处理
git config --global core.autocrlf input        # macOS/Linux
# git config --global core.autocrlf true       # Windows

# 中文支持
git config --global core.quotepath false        # 显示中文文件名

# 默认分支名
git config --global init.defaultBranch main

# 颜色显示
git config --global color.ui auto

# 差异显示
git config --global diff.renames copies         # 检测重命名和复制

# 合并冲突时显示原始样式
git config --global merge.conflictstyle diff3

# 拉取策略（推荐rebase）
git config --global pull.rebase true

# 查看最终配置
echo "=== Git全局配置 ==="
git config --global --list
```

### Q3: IDEA中解决Git冲突完整流程
**A:**
```text
IDEA图形化解决冲突步骤：

1. 更新代码时发现冲突
   VCS → Update Project (Ctrl+T)
   IDEA弹出冲突对话框，显示冲突文件列表

2. 双击冲突文件进入合并界面
   ├── Left: 本地版本（Your Version）
   ├── Center: 合并结果（Result）
   └── Right: 远程版本（Their Version）

3. 解决冲突的操作按钮
   ├── "<<": 接受左侧（本地）的修改
   ├── ">>": 接受右侧（远程）的修改
   ├── "X": 拒绝修改
   └── "Apply Non-Conflicting Changes": 自动应用非冲突部分

4. 手动调整合并结果
   - 直接编辑中间的Result区域
   - 注意保留双方都有意义的修改

5. 标记已解决
   - 点击 "Apply" 或 "Save changes and finish merging"
   - 冲突文件在提交对话框显示为绿色

6. 提交合并
   - Ctrl+K 打开提交对话框
   - 填写合并提交信息
   - Commit and Push

IDEA冲突解决优势：
- 可视化三路合并界面
- 显示文件结构（Structure视图）
- 接受/拒绝按钮操作简单
- 可对比左右两侧差异
```

### Q4: 基于Gitee/GitHub的Fork协作流程
**A:**
```bash
#!/bin/bash
# Fork协作流程（适用于开源项目贡献）

# 1. Fork原项目
# 在GitHub/Gitee网页上点击 Fork 按钮

# 2. 克隆自己的Fork到本地
git clone git@github.com:your-username/original-project.git
cd original-project

# 3. 添加上游仓库（原项目）
git remote add upstream git@github.com:original-author/original-project.git
git remote -v
# origin     git@github.com:your-username/original-project.git (push)
# upstream   git@github.com:original-author/original-project.git (fetch)

# 4. 同步上游最新代码
git fetch upstream
git checkout main
git merge upstream/main
git push origin main

# 5. 创建功能分支
git checkout -b feature/awesome-feature

# 6. 开发并提交
# ... 编码 ...
git add .
git commit -m "feat: 添加超棒的功能"

# 7. 推送到自己的Fork
git push -u origin feature/awesome-feature

# 8. 创建Pull Request
# 在GitHub网页上创建PR
# base: original-author:main ← compare: your-username:feature/awesome-feature

# 9. 等待项目维护者Review和合并

# 10. PR合并后，同步上游
git checkout main
git pull upstream main
git push origin main

# 11. 删除已经合并的功能分支
git branch -d feature/awesome-feature
git push origin --delete feature/awesome-feature
```

### Q5: 企业级Git提交规范配置
**A:**
```bash
#!/bin/bash
# 配置提交信息规范（使用commitlint）

# 1. 创建commit-msg hook
cat > .git/hooks/commit-msg << 'HOOK'
#!/bin/sh

# Conventional Commits 格式校验
# 格式: type(scope): description
# type: feat|fix|docs|style|refactor|perf|test|chore|ci|build

COMMIT_MSG=$(cat "$1")

# 允许Merge提交和Revert提交
if echo "$COMMIT_MSG" | grep -qE "^(Merge|Revert)"; then
    exit 0
fi

# 校验提交信息格式
PATTERN="^(feat|fix|docs|style|refactor|perf|test|chore|ci|build)(\(.+\))?: .+"

if ! echo "$COMMIT_MSG" | grep -qE "$PATTERN"; then
    echo ""
    echo "============================================"
    echo "  ERROR: 提交信息格式不正确！"
    echo "============================================"
    echo ""
    echo "正确格式: type(scope): description"
    echo ""
    echo "type 可选值:"
    echo "  feat    - 新功能"
    echo "  fix     - Bug修复"
    echo "  docs    - 文档更新"
    echo "  style   - 代码格式（不影响功能）"
    echo "  refactor- 重构"
    echo "  perf    - 性能优化"
    echo "  test    - 测试"
    echo "  chore   - 构建/工具变更"
    echo "  ci      - CI配置变更"
    echo "  build   - 构建系统变更"
    echo ""
    echo "示例: feat(user): 添加用户登录功能"
    echo "      fix(order): 修复订单金额计算错误"
    echo ""
    exit 1
fi
HOOK

chmod +x .git/hooks/commit-msg

echo "commit-msg hook已配置"

# 2. 配置分支命名规范
cat > .git/hooks/pre-commit << 'HOOK'
#!/bin/sh

# 获取当前分支名
BRANCH_NAME=$(git symbolic-ref --short HEAD 2>/dev/null)

# 分支命名规范检查（在推送时检查）
if [ "$BRANCH_NAME" != "main" ] && [ "$BRANCH_NAME" != "develop" ]; then
    # 功能分支: feature/xxx
    # 修复分支: bugfix/xxx
    # 发布分支: release/x.x.x
    # 热修复:   hotfix/x.x.x
    PATTERN="^(feature|bugfix|release|hotfix|support)/.+"
    if ! echo "$BRANCH_NAME" | grep -qE "$PATTERN"; then
        echo ""
        echo "============================================"
        echo "  WARNING: 分支名 '$BRANCH_NAME' 不符合规范"
        echo "============================================"
        echo "规范格式: feature/xxx, bugfix/xxx, release/x.x.x, hotfix/x.x.x"
        echo ""
    fi
fi

# 检查是否有大文件被暂存（大于50MB）
STAGED_FILES=$(git diff --cached --name-only)
for FILE in $STAGED_FILES; do
    if [ -f "$FILE" ]; then
        SIZE=$(stat -c%s "$FILE" 2>/dev/null || stat -f%z "$FILE" 2>/dev/null)
        if [ "$SIZE" -gt 52428800 ]; then  # 50MB
            echo "ERROR: 文件 '$FILE' 超过50MB，请使用Git LFS"
            exit 1
        fi
    fi
done
HOOK

chmod +x .git/hooks/pre-commit

echo "pre-commit hook已配置"
echo "Git提交规范配置完成！"
```

---

## 五、系统设计题（3-5题）

### Q1: 设计企业级Git分支管理规范
**A:**
```
[规范文档]

1. 分支结构
   main        - 生产分支，只接受来自release/hotfix的合并
   develop     - 开发分支，功能分支合并目标
   feature/*   - 功能分支，从develop创建，完成后合并回develop
   release/*   - 发布分支，从develop创建，测试完成后合并到main和develop
   hotfix/*    - 热修复分支，从main创建，修复后合并到main和develop

2. 命名规范
   feature/issue-142-user-login   (关联Issue编号)
   bugfix/login-null-pointer
   release/2.1.0                  (语义化版本号)
   hotfix/2.0.1

3. 提交规范（Conventional Commits）
   feat(user): 添加用户登录功能                  # 新功能
   fix(order): 修复订单金额计算错误              # Bug修复
   docs(readme): 更新安装说明                     # 文档
   refactor(service): 重构用户服务层              # 重构
   test(api): 添加用户接口测试                    # 测试
   chore(deps): 更新Spring Boot版本              # 依赖维护

4. 合并要求
   - main分支：需要PR + 2个Reviewers + CI通过
   - develop分支：需要PR + 1个Reviewer + CI通过
   - feature分支：squash合并到develop
   - 禁止直接push到main和develop

5. 版本号规范（语义化版本）
   major.minor.patch
   示例: v2.1.0
   - major: 不兼容的API变更
   - minor: 向下兼容的功能新增
   - patch: 向下兼容的问题修复
```

### Q2: 设计Git仓库安全策略
**A:**
```
1. 访问控制
   - 使用SSH密钥认证（禁止密码认证）
   - 配置团队角色和权限（Owner/Maintainer/Developer/Viewer）
   - 定期轮换访问密钥
   - 离职员工及时移除权限

2. 分支保护
   [分支保护规则 - main]
   - 需要PR才能合并
   - 至少2个Approval
   - 所有CI检查通过
   - 禁止强制推送
   - 线性历史要求
   - 对话解决（所有评论需Resolve）

3. 安全扫描（CI集成）
   - 密钥泄露检测（GitLeaks/TruffleHog）
   - 依赖漏洞扫描（Dependabot/Snyk）
   - 代码质量扫描（SonarQube）
   - 容器镜像扫描（Trivy/Clair）

4. 审计日志
   - 记录所有Push/Pull/Merge操作
   - 记录权限变更
   - 记录Webhook调用
   - 定期审计回顾

5. 数据安全
   - 仓库加密存储
   - 定期备份（异地容灾）
   - 禁止提交密钥/密码/Token
   - 使用Git Secrets检测敏感信息
```

### Q3: 设计团队Git工作流（中大型Java项目）
**A:**
```
项目结构：微服务架构（8个服务）
开发团队：12人，4个小组
迭代周期：2周

每日流程：
09:00 - 同步最新代码（git fetch origin）
09:15 - 开始开发（feature分支）
12:00 - 提交暂存，推送远程备份
14:00 - 同步develop最新代码（git pull --rebase origin develop）
17:00 - 提交当日工作（推送到远程）

迭代流程：
D1-D2: 需求评审，创建feature分支
D3-D8: 功能开发
D9-D10: 功能测试，Code Review
D11: 合并到develop，集成测试
D12: 创建release分支，回归测试
D13: 发布到生产（合并到main）

工具链：
代码仓库: GitLab（自托管）
代码审查: GitLab MR + Reviewers
CI/CD: GitLab CI
代码质量: SonarQube
沟通协作: Slack + GitLab集成
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点/问题 | 原因 | 解决方案 | 最佳实践 |
|-----------|------|----------|----------|
| git push失败 | 远程有未同步的提交 | `git pull --rebase` 再推送 | 推送前先 `git fetch` |
| 合并冲突频繁 | 多人同时修改同一模块 | 模块拆分，减少协作冲突 | 使用微服务拆分 |
| 提交信息无意义 | 缺乏规范约束 | 配置commit-msg hook | 使用Conventional Commits |
| .gitignore不生效 | 文件已被Git跟踪 | `git rm --cached <file>` | 项目初始化时配置 |
| 大文件误提交 | 未配置LFS | filter-branch清理 | 使用.gitattributes管控 |
| 丢失提交记录 | reset --hard误操作 | 立即用reflog恢复 | reset前确认备份 |
| 分支命名混乱 | 缺少规范 | 制定分支命名规范 | 自动化检查分支名 |
| SSH连接失败 | 密钥配置错误 | ssh -T测试，检查权限 | 使用SSH config |
| 合并了错误分支 | 操作失误 | `git reset --hard ORIG_HEAD` | 合并前确认分支名 |
| rebase后丢失提交 | 使用方法不当 | reflog恢复 | 仅个人分支使用rebase |
| clone速度慢 | 网络问题或仓库太大 | 使用 `--depth 1` 浅克隆 | 配置代理或国内镜像 |
| 代码被覆盖 | 强制推送 | 使用 `--force-with-lease` | 禁用force push到保护分支 |

---

## 七、面试回答模板（Top 5）

### 模板1: 版本控制的分类和Git的优势
**回答框架：**
- **分类**: 集中式（SVN/CVS）vs 分布式（Git/Mercurial）
- **Git优势**:
  1. 分布式架构，每个开发者拥有完整仓库
  2. 分支操作轻量、快速、低成本
  3. 本地操作不需要网络
  4. 数据完整性（SHA-1校验）
  5. 暂存区设计提供灵活性
- **SVN局限**: 单点故障、需联网、分支慢

### 模板2: Git工作区域的流转
**回答框架：**
- **四大区域**: 工作区（Working Directory）→ 暂存区（Stage）→ 本地仓库（Repository）→ 远程仓库（Remote）
- **命令映射**:
  - `git add`: 工作区 → 暂存区
  - `git commit`: 暂存区 → 本地仓库
  - `git push`: 本地仓库 → 远程仓库
  - `git pull`: 远程仓库 → 工作区
- **核心价值**: 暂存区让开发者可以分批精细提交

### 模板3: 解决合并冲突的步骤
**回答框架：**
1. 识别冲突（`git status` 查看冲突文件）
2. 打开冲突文件，找到 `<<<<<<<`, `=======`, `>>>>>>>` 标记
3. 与相关开发者沟通，确认保留哪个版本或结合两个版本
4. 编辑文件，删除冲突标记
5. `git add <file>` 标记为已解决
6. `git commit` 完成合并提交
7. `git push` 推送到远程

### 模板4: 场景题 - 误操作恢复策略
**回答框架：**
- **误改工作区文件**: `git restore <file>` 或 `git checkout -- <file>`
- **误添加到暂存区**: `git restore --staged <file>` 或 `git reset HEAD <file>`
- **误提交到本地**: `git reset --soft HEAD~1`（保留修改）或 `git commit --amend`
- **误提交并推送**: `git revert <commit-hash>`（安全，推荐）或 `git reset --hard + force push`
- **误删分支/丢提交**: `git reflog` 查找历史引用，再 `git reset --hard <hash>`
- **核心原则**: 本地未推送可用reset，已推送用revert，恢复靠reflog

### 模板5: Git Flow企业级应用方案
**回答框架：**
- **分支定义**: main（生产）、develop（开发）、feature（功能）、release（发布）、hotfix（热修）
- **协作流程**: 功能从develop分支创建feature分支 → 开发 → Code Review → 合并回develop → 发布创release分支 → 测试 → 合main并打tag → 同步回develop
- **价值**:
  - 主分支代码始终可部署
  - 并行开发互不干扰
  - 发布流程标准化
  - 紧急修复可快速响应
- **适用**: 版本发布周期明确、需要严格流程管控的企业项目

---

## 八、快速查漏补缺Checklist

- [ ] 集中式vs分布式版本控制核心区别
- [ ] Git四大工作区（工作区/暂存区/本地仓库/远程仓库）
- [ ] 三态流转（modified → staged → committed）
- [ ] 全局配置（user.name, user.email, core.autocrlf）
- [ ] SSH免密配置完整流程
- [ ] 基本操作：init/clone/add/commit/push/pull/status/log/diff
- [ ] 分支操作：branch/checkout/merge/delete
- [ ] 撤销操作：restore/reset/revert
- [ ] git reset三模式（--soft/--mixed/--hard）
- [ ] git reflog恢复误操作
- [ ] git stash暂存工作进度
- [ ] 合并冲突解决和预防
- [ ] merge vs rebase vs cherry-pick
- [ ] Fast-Forward Merge原理
- [ ] 远程仓库操作（remote/fetch/push/pull/clone）
- [ ] fetch vs pull区别
- [ ] IDEA Git集成操作流程
- [ ] Fork协作流程
- [ ] .gitignore编写规则
- [ ] Git提交信息规范（Conventional Commits）
- [ ] Git Flow分支策略
- [ ] 语义化版本号管理
- [ ] Tag标签管理
- [ ] Pull Request / Merge Request流程
- [ ] Code Review事项CheckList
- [ ] CI/CD与Git集成
- [ ] Git LFS大文件管理
- [ ] .git目录结构和对象模型
- [ ] git bisect二分查找
- [ ] git blame追溯代码来源
- [ ] 分支保护规则设置
- [ ] 团队Git协作规范制定
- [ ] GitHub vs Gitee对比
- [ ] 常见的Git错误和解决方案
- [ ] 面试高频题：Git vs SVN、merge vs rebase、冲突解决
