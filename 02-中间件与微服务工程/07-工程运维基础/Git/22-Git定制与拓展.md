# 22-Git定制与拓展
> 🎯 Git不只是`commit/push/pull` — 通过配置定制、别名优化、Hook自动化、多仓库管理，让Git深度适配Java后端团队的协作规范与CI/CD流程

---

## 目录
1. [Git定制的核心价值](#1-git定制的核心价值)
2. [配置定制：git config深度解析](#2-配置定制git-config深度解析)
3. [别名：提效利器](#3-别名提效利器)
4. [提交模板与规范强制](#4-提交模板与规范强制)
5. [Git Hooks：自动化工作流](#5-git-hooks自动化工作流)
6. [Git Attributes：文件属性控制](#6-git-attributes文件属性控制)
7. [多仓库管理与批量操作](#7-多仓库管理与批量操作)
8. [定制实战：Java后端团队规范落地](#8-定制实战java后端团队规范落地)

---

## 1. Git定制的核心价值

```text
默认Git：手动、通用、无规范约束
定制后的Git：自动、贴合团队、规范强制执行

定制四维度：
  📐 规范落地 → 统一提交格式、统一身份、统一操作流程
  ⚡ 效率提升 → 别名简化命令、Hook自动检查
  🔗 工具集成 → 连接IDE/Maven/Jenkins/SonarQube
  🏗️ 架构适配 → 微服务多仓库统一管理
```

| 定制方式 | 作用范围 | 复杂度 | 推荐场景 |
|----------|:---:|:---:|------|
| `git config` | 个人/项目 | ⭐ | 所有开发者，入门定制 |
| 别名(Alias) | 个人/全局 | ⭐ | 简化高频命令 |
| 提交模板 | 个人/项目 | ⭐ | 团队统一提交格式 |
| Git Hooks | 项目/团队 | ⭐⭐⭐ | 自动化检查、CI触发 |
| 自定义脚本 | 团队 | ⭐⭐⭐ | 批量操作、复杂工作流 |
| .gitattributes | 项目 | ⭐⭐ | 跨平台文件处理 |

---

## 2. 配置定制：git config深度解析

### 2.1 三个配置层级

| 层级 | 命令 | 配置文件位置 | 影响范围 | 优先级 |
|------|------|-------------|----------|:---:|
| **系统** | `git config --system` | `/etc/gitconfig` | 所有用户所有仓库 | 最低 |
| **全局** | `git config --global` | `~/.gitconfig` | 当前用户所有仓库 | 中 |
| **本地** | `git config --local` | `.git/config` | 当前仓库 | **最高** |

```bash
# 团队统一配置
git config --global user.name "Zhang San"
git config --global user.email "zhangsan@company.com"
git config --global core.editor "code --wait"          # VS Code作为编辑器
git config --global core.autocrlf input                # Mac/Linux: input; Windows: true

# 特定项目使用不同身份（开源/公司项目切换）
git config --local user.email "zhangsan@gmail.com"     # 局部覆盖全局

# 推荐：按项目目录自动切换身份
# ~/.gitconfig 中添加：
# [includeIf "gitdir:~/work/"]
#   path = ~/.gitconfig-work
# [includeIf "gitdir:~/personal/"]
#   path = ~/.gitconfig-personal
```

### 2.2 Java后端推荐配置

```bash
# === 提交相关 ===
git config --global commit.template ~/.gitmessage       # 提交模板
git config --global commit.gpgsign true                 # GPG签名
git config --global pull.rebase true                    # pull时默认rebase

# === 性能相关 ===
git config --global core.preloadindex true              # 预加载索引（加速status）
git config --global core.fscache true                   # 文件系统缓存（macOS/Windows）

# === 差异与合并 ===
git config --global diff.algorithm histogram            # 更智能的diff算法
git config --global merge.conflictstyle diff3           # 冲突时显示共同祖先版本
git config --global merge.tool "code --wait"            # VS Code作为合并工具

# === 安全相关 ===
git config --global core.ignorecase false               # 大小写敏感（Mac/Windows默认不敏感！）
git config --global core.filemode false                 # 忽略文件权限变更
git config --global fetch.prune true                    # fetch时自动清理远程已删除分支
```

### 2.3 团队配置共享策略

```text
问题：如何让团队每个人的Git配置保持一致？

方案1（推荐）：在项目根目录放 .gitconfig-recommended
  → README中说明：cp .gitconfig-recommended ~/.gitconfig
  → 适合一次性初始化

方案2：在 project/.git/config 中配置本地级别
  → git config --local xxx → 只对当前项目生效
  → 缺点：clone不会复制.git/config

方案3：Shell脚本统一配置（适合大团队）
  → 提供 setup-git.sh 脚本，新人运行一次即可
```

---

## 3. 别名：提效利器

### 3.1 必装别名清单

```bash
# 添加到 ~/.gitconfig 的 [alias] 段落

[alias]
  # === 状态与日志 ===
  st = status -sb                    # 精简状态
  lg = log --graph --oneline --decorate --all -20  # 图形化日志
  ll = log --pretty=format:"%C(yellow)%h%Creset %C(cyan)%ad%Creset %s %C(green)(%an)%Creset" --date=short  # 彩色一行日志
  hist = log --follow --graph --all --format='%C(yellow)%h%Creset %C(cyan)%ad%Creset | %s%C(green)%d%Creset %C(bold blue)[%an]%Creset' --date=short

  # === 分支管理 ===
  br = branch
  co = checkout
  sw = switch
  nb = switch -c                    # 新建并切换分支
  del = branch -d
  del-force = branch -D
  upstream = branch --set-upstream-to=origin/$(git branch --show-current)

  # === 暂存与提交 ===
  ci = commit
  ca = commit -a                    # add所有跟踪文件 + commit
  amend = commit --amend --no-edit  # 追加到上一个commit（不改message）
  unstage = reset HEAD --           # 取消暂存
  uncommit = reset --soft HEAD~1    # 撤销commit但保留修改

  # === 工作区操作 ===
  wip = stash push -m "WIP"        # 快速暂存
  unwip = stash pop                # 恢复暂存
  disc = checkout -- .              # 丢弃所有工作区修改（危险！）

  # === 远程操作 ===
  sync = !git fetch --all --prune && git pull --rebase  # 全同步
  force-push = push --force-with-lease

  # === 维护 ===
  cleanup = !git branch --merged | grep -v '\\*\\|main\\|master\\|develop' | xargs -r git branch -d  # 删除已合并分支
```

### 3.2 复杂别名：Shell函数

```bash
# Git别名可以调用Shell（以 ! 开头）

# 查看某个开发者今天的提交
[alias]
  today = !git log --author=\"$(git config user.name)\" --since=midnight --oneline

# 查看每个开发者的提交统计
[alias]
  count = !git shortlog -sn --all --no-merges

# 删除远程已不存在的本地分支
[alias]
  prune-branches = !git fetch -p && git branch -vv | awk '/: gone]/{print $1}' | xargs -r git branch -D

# 回滚到指定commit并保留修改到stash
[alias]
  rollback = !sh -c 'git stash && git reset --hard $1 && git stash pop' -
```

---

## 4. 提交模板与规范强制

### 4.1 提交模板

```bash
# 创建模板文件 ~/.gitmessage
cat > ~/.gitmessage << 'EOF'
# <type>(<scope>): <subject>
# 
# <body>
# 
# <footer>
# 
# type: feat | fix | docs | style | refactor | test | chore | perf | ci
# scope: 影响范围（可选）
# subject: 简短描述（≤50字符，现在时，首字母小写）
# body: 详细描述（可选，≤72字符换行）
# footer: 关联issue（可选，Closes #123）
EOF

git config --global commit.template ~/.gitmessage
```

### 4.2 Commit Message规范强制（commit-msg Hook）

```bash
# .git/hooks/commit-msg
#!/bin/bash
# 强制Conventional Commits格式

COMMIT_MSG=$(cat "$1")
PATTERN="^(feat|fix|docs|style|refactor|test|chore|perf|ci)(\(.+\))?: .{1,50}"

if ! echo "$COMMIT_MSG" | head -1 | grep -qE "$PATTERN"; then
    echo "❌ Commit message不符合规范！"
    echo ""
    echo "格式: <type>(<scope>): <subject>"
    echo "type: feat|fix|docs|style|refactor|test|chore|perf|ci"
    echo ""
    echo "示例: feat(user): 添加用户登录功能"
    echo "      fix(order): 修复订单金额计算错误"
    exit 1
fi
```

### 4.3 团队Hook共享

```bash
# 问题：.git/hooks/ 不在版本控制中，如何共享给整个团队？

# 方案：将hooks放在项目目录，配置git查找路径
mkdir -p .githooks
cp commit-msg .githooks/
git config --local core.hooksPath .githooks

# 现在 .githooks/ 可以git add + commit，团队clone后
# 执行 git config --local core.hooksPath .githooks 即可启用
```

---

## 5. Git Hooks：自动化工作流

### 5.1 常用Hook清单

| Hook | 触发时机 | 典型用途 | 返回值影响 |
|------|----------|----------|:---:|
| `pre-commit` | `git commit` 之前 | 代码检查（checkstyle/lint）、单元测试 | 非0=阻止提交 |
| `commit-msg` | 编辑commit message后 | 规范校验 | 非0=阻止提交 |
| `post-commit` | commit完成后 | 通知、日志记录 | 不影响 |
| `pre-push` | `git push` 之前 | 完整测试、安全扫描 | 非0=阻止推送 |
| `post-receive` | 服务端接收推送后 | 触发CI/CD、自动部署 | 不影响 |

### 5.2 pre-commit：提交前自动检查

```bash
#!/bin/bash
# .git/hooks/pre-commit — Java项目提交流程

echo "🔍 运行提交前检查..."

# 1. 编译检查（只检查暂存的Java文件）
STAGED_JAVA=$(git diff --cached --name-only --diff-filter=ACM | grep '\.java$')
if [ -n "$STAGED_JAVA" ]; then
    echo "📦 编译检查..."
    mvn -q compile
    if [ $? -ne 0 ]; then
        echo "❌ 编译失败，请修复后重新提交"
        exit 1
    fi
fi

# 2. Checkstyle（仅检查暂存文件）
if [ -n "$STAGED_JAVA" ]; then
    echo "📐 Checkstyle检查..."
    mvn -q checkstyle:check
    if [ $? -ne 0 ]; then
        echo "❌ Checkstyle不通过，请修复"
        exit 1
    fi
fi

# 3. 禁止提交调试代码
if git diff --cached | grep -qE '(System\.out\.println|console\.log|debugger)'; then
    echo "❌ 检测到调试代码（System.out.println等），请移除后重新提交"
    exit 1
fi

echo "✅ 检查通过！"
```

### 5.3 post-receive：服务端自动部署

```bash
#!/bin/bash
# 服务端 .git/hooks/post-receive — 推送后自动触发

while read oldrev newrev refname; do
    BRANCH=$(echo $refname | sed 's/refs\/heads\///')
    
    if [ "$BRANCH" = "main" ]; then
        echo "🚀 检测到main分支更新，触发生产部署..."
        WORKTREE="/var/www/prod"
        git --work-tree=$WORKTREE checkout -f main
        cd $WORKTREE
        mvn clean package -DskipTests
        systemctl restart myapp
        echo "✅ 部署完成"
    elif [ "$BRANCH" = "develop" ]; then
        echo "🧪 检测到develop分支更新，触发测试环境部署..."
        # 部署到测试环境
    fi
done
```

---

## 6. Git Attributes：文件属性控制

### 6.1 .gitattributes 标准配置

```gitattributes
# .gitattributes — 放在项目根目录

# === 行尾处理（跨平台协作关键） ===
*       text=auto                # 自动检测文本/二进制
*.java  text eol=lf              # Java源码强制LF（Linux/macOS）
*.xml   text eol=lf              # XML/POM强制LF
*.yaml  text eol=lf              # YAML强制LF
*.sh    text eol=lf              # Shell脚本强制LF
*.bat   text eol=crlf            # Windows批处理强制CRLF
*.cmd   text eol=crlf

# === 二进制文件 ===
*.jar   binary                   # JAR包不diff
*.class binary                   # 编译产物不diff
*.png   binary                   # 图片不diff
*.pdf   binary

# === 忽略diff ===
*.lock  -diff                    # 锁文件不diff
pom.xml -diff                    # (可选) POM变更频繁时

# === 语言统计 ===
*.sql   linguist-language=SQL    # 标记SQL文件类型
*.vue   linguist-detectable      # Vue文件计入统计

# === 合并策略 ===
pom.xml merge=union              # POM冲突取并集（仅版本号场景）
```

### 6.2 关键配置说明

```bash
# text=auto: Git自动判断是文本还是二进制
#   文本 → checkout时转换行尾（CRLF→LF），commit时转回
#   二进制 → 不转换

# eol=lf: 文件中始终使用LF换行（Java标准）
#   Windows开发者需配合 core.autocrlf=true
#   → 工作区CRLF，Git仓库中LF

# Java项目推荐配置：
#   Windows: git config --global core.autocrlf true
#   Mac/Linux: git config --global core.autocrlf input
#   项目 .gitattributes: *.java text eol=lf
```

---

## 7. 多仓库管理与批量操作

### 7.1 Git Submodule

```bash
# 添加子模块（如公共组件库）
git submodule add https://github.com/company/common.git libs/common

# clone含子模块的项目
git clone --recurse-submodules <url>

# 更新子模块到最新
git submodule update --remote --recursive

# 批量操作所有子模块
git submodule foreach 'git checkout main && git pull'
git submodule foreach 'mvn clean install -DskipTests'
```

| 方案 | 适用场景 | 优势 | 劣势 |
|------|----------|------|------|
| **Submodule** | 独立仓库，需要精确版本锁定 | 独立性好 | 操作复杂，初学者容易出错 |
| **Monorepo** | 紧耦合项目，统一发版 | 原子提交、跨模块重构 | 仓库巨大、权限粗粒度 |
| **Git Subtree** | 需要合并外部仓库历史 | 比submodule简单 | 提交历史变长 |

### 7.2 批量仓库操作脚本

```bash
#!/bin/bash
# batch-git.sh — 对所有微服务仓库执行同一Git操作

REPOS=(
    "user-service"
    "order-service"
    "product-service"
    "gateway-service"
    "common-lib"
)

ACTION=${1:-status}

for repo in "${REPOS[@]}"; do
    echo "=== $repo ==="
    cd "$repo" || continue
    case $ACTION in
        status)  git status --short ;;
        pull)    git fetch --all --prune && git pull --rebase ;;
        branch)  git branch --all ;;
        *)       git "$@" ;;
    esac
    cd ..
    echo ""
done
```

---

## 8. 定制实战：Java后端团队规范落地

### 8.1 新成员Onboarding脚本

```bash
#!/bin/bash
# setup-git.sh — 新成员入职一键配置

echo "👤 请输入你的姓名（中文）:"
read NAME
echo "📧 请输入你的企业邮箱:"
read EMAIL

# 基础配置
git config --global user.name "$NAME"
git config --global user.email "$EMAIL"
git config --global core.autocrlf input
git config --global core.ignorecase false
git config --global pull.rebase true
git config --global fetch.prune true

# 别名
git config --global alias.st "status -sb"
git config --global alias.lg "log --graph --oneline --decorate --all -20"
git config --global alias.ci "commit"
git config --global alias.br "branch"
git config --global alias.co "checkout"

# 提交模板
cp ~/.gitmessage ~/.gitmessage.bak 2>/dev/null
cat > ~/.gitmessage << 'TEMPLATE'
# feat|fix|docs|style|refactor|test|chore|perf|ci: 简短描述
# 
# 详细说明（可选）
# 
# Closes #ISSUE_ID
TEMPLATE
git config --global commit.template ~/.gitmessage

# 全局.gitignore
cat > ~/.gitignore_global << 'IGNORE'
.idea/
*.iml
target/
*.class
.DS_Store
Thumbs.db
IGNORE
git config --global core.excludesfile ~/.gitignore_global

echo "✅ Git配置完成！"
git config --global --list
```

### 8.2 CI/CD中的Git检查

```yaml
# .github/workflows/git-check.yml
name: Git Commit Check
on: [push, pull_request]

jobs:
  commit-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
      
      - name: 检查提交信息格式
        run: |
          git log origin/main..HEAD --format='%s' | while read msg; do
            if ! echo "$msg" | grep -qE '^(feat|fix|docs|style|refactor|test|chore|perf|ci)'; then
              echo "❌ 不符合规范: $msg"
              exit 1
            fi
          done
      
      - name: 检查无大型二进制文件
        run: |
          if git diff --stat origin/main..HEAD | grep -qE '\.jar|\.war|\.zip'; then
            echo "❌ 禁止提交JAR/WAR/ZIP等二进制文件"
            exit 1
          fi
```

---

> 🎯 **Git定制不是炫技，而是将团队的规范、流程、效率要求具象化为可执行的配置和脚本** — 让规范自动执行，让新人一键上手，让微服务多仓库操作不再繁琐。
