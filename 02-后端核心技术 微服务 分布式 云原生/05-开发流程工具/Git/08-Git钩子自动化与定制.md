# 08-Git 钩子自动化与定制
> 钩子做轻量检查、CI 做重量检查：Hooks 全景、示例脚本、husky/pre-commit 框架、CI 集成、config 三层体系与别名定制

## 📚 目录
1. [Git Hooks 全景](#1-git-hooks-全景)
2. [示例脚本（可直接使用）](#2-示例脚本可直接使用)
3. [Hook 共享方案：core.hooksPath](#3-hook-共享方案corehookspath)
4. [Husky + lint-staged（前端）](#4-husky--lint-staged前端)
5. [pre-commit 框架（Python）](#5-pre-commit-框架python)
6. [Hook 跳过与最佳实践](#6-hook-跳过与最佳实践)
7. [CI 集成（Actions / GitLab CI / act）](#7-ci-集成actions--gitlab-ci--act)
8. [config 三层配置体系](#8-config-三层配置体系)
9. [includeIf 与身份定制](#9-includeif-与身份定制)
10. [别名定制大全](#10-别名定制大全)
11. [差异工具 / 性能优化 / 提交模板](#11-差异工具--性能优化--提交模板)
12. [团队配置共享与新人 Onboarding](#12-团队配置共享与新人-onboarding)
13. [核心要点](#13-核心要点)
14. [参考来源](#14-参考来源)

## 1. Git Hooks 全景

### 1.1 客户端钩子

| 钩子 | 触发时机 | 典型用途 | 非 0 返回值影响 |
|------|---------|---------|:---:|
| `pre-commit` | `git commit` 之前 | lint、格式化、检查密钥泄露 | 阻止提交 |
| `prepare-commit-msg` | 默认消息生成后 | 自动填充 commit 模板 | - |
| `commit-msg` | 消息写入后 | 校验 commit message 格式 | 阻止提交 |
| `post-commit` | commit 完成后 | 通知、日志 | 不影响 |
| `pre-rebase` | rebase 之前 | 阻止危险 rebase | 阻止 |
| `post-checkout` | checkout 之后 | 更新依赖 | 不影响 |
| `post-merge` | merge 之后 | 自动安装新依赖 | 不影响 |
| `pre-push` | push 之前 | 运行单元测试、安全检查 | 阻止推送 |

### 1.2 服务端钩子

| 钩子 | 触发时机 | 典型用途 |
|------|---------|---------|
| `pre-receive` | push 到达后、更新 refs 前 | 拒绝不合规 push |
| `update` | 每个 ref 更新时 | 拒绝特定分支 force push |
| `post-receive` | push 完成后 | CI 触发/部署/通知 |

### 1.3 hooks 目录与局限

```bash
ls .git/hooks/
# applypatch-msg.sample  pre-push.sample
# commit-msg.sample      pre-rebase.sample
# pre-commit.sample      ...
# Git 提供示例模板，去掉 .sample 后缀即激活
```

| 局限 | 说明 |
|------|------|
| 不可追踪 | `.git/hooks/` 不在版本控制中，每人需手动设置 |
| 跨平台差 | bash 脚本在 Windows 需要 Git Bash |
| 可绕过 | `--no-verify` 可跳过——**安全策略必须在服务端 enforce** |

## 2. 示例脚本（可直接使用）

### 2.1 pre-commit（通用版）

```bash
# .git/hooks/pre-commit
#!/bin/bash

# 禁止提交包含 console.log 的代码
if git diff --cached | grep -q "^+.*console\.log"; then
    echo "❌ 发现 console.log，请在提交前移除"
    exit 1
fi

# 检查 .env 文件是否被提交
if git diff --cached --name-only | grep -q "\.env$"; then
    echo "❌ 禁止提交 .env 文件！"
    exit 1
fi

exit 0
```

```bash
chmod +x .git/hooks/pre-commit
```

### 2.2 commit-msg（强制 Conventional Commits）

```bash
# .git/hooks/commit-msg
#!/bin/bash

COMMIT_MSG=$(cat "$1")

# 强制 Conventional Commits 格式
PATTERN="^(feat|fix|docs|style|refactor|test|chore|perf|ci)(\(.+\))?: .{1,72}$"

if ! echo "$COMMIT_MSG" | grep -qE "$PATTERN"; then
    echo "❌ Commit message 不符合规范"
    echo "格式: <type>(<scope>): <subject>"
    echo "type: feat|fix|docs|style|refactor|test|chore|perf|ci"
    exit 1
fi
```

### 2.3 pre-commit（Java 项目完整版）

```bash
#!/bin/bash
# .git/hooks/pre-commit — Java 项目提交流程

echo "🔍 运行提交前检查..."

# 1. 编译检查（只检查暂存的 Java 文件）
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

### 2.4 post-receive（服务端自动部署）

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
    fi
done
```

## 3. Hook 共享方案：core.hooksPath

`.git/hooks/` 不在版本控制中 → 用 `core.hooksPath` 指向项目内目录：

```bash
mkdir -p .githooks
cp commit-msg .githooks/
git config --local core.hooksPath .githooks
# .githooks/ 可以 git add + commit，团队 clone 后执行
# git config --local core.hooksPath .githooks 即可启用
```

## 4. Husky + lint-staged（前端）

```bash
npm install --save-dev husky lint-staged
npx husky init    # Git 2.9+，创建 .husky/ 目录并设置 prepare 脚本
```

```json
// package.json 或 .lintstagedrc.json
{
  "lint-staged": {
    "*.{js,ts,tsx}": ["eslint --fix", "prettier --write"],
    "*.{json,md,yaml}": ["prettier --write"],
    "*.py": ["black", "isort"]
  }
}
```

```javascript
// commitlint.config.js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', ['feat', 'fix', 'docs', 'style', 'refactor', 'test', 'chore', 'perf', 'ci', 'build']],
    'subject-max-length': [2, 'always', 72],
  },
};
```

```text
project/
├── .husky/
│   ├── pre-commit       # npx lint-staged
│   ├── commit-msg       # npx commitlint --edit $1
│   └── pre-push         # npm test
├── .lintstagedrc.json
├── commitlint.config.js
└── package.json         # 需含 "prepare": "husky"
```

## 5. pre-commit 框架（Python）

```bash
pip install pre-commit
```

```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.5.0
    hooks:
      - id: trailing-whitespace      # 移除行尾空格
      - id: end-of-file-fixer        # 文件末尾加空行
      - id: check-yaml               # YAML 语法检查
      - id: detect-private-key       # 检测私钥泄露
      - id: check-added-large-files  # 阻止大文件
  - repo: https://github.com/psf/black
    rev: 24.1.0
    hooks:
      - id: black                    # Python 格式化
  - repo: https://github.com/charliermarsh/ruff-pre-commit
    rev: v0.1.0
    hooks:
      - id: ruff                     # Python linting
```

```bash
pre-commit install                 # 安装 pre-commit hook
pre-commit install --hook-type commit-msg
pre-commit run --all-files         # 手动对所有文件运行
```

## 6. Hook 跳过与最佳实践

```bash
git commit --no-verify -m "emergency fix"   # 跳过 pre-commit hook
git commit -n -m "..."        # -n = --no-verify，跳过所有 hooks
git push --no-verify          # 跳过 push hooks
```

> ⚠️ 只在明确知道为什么需要跳过时使用；**CI 应作为第二道防线兜底**（hook 可跳过，CI 不可）。

```text
钩子最佳实践：
1. 轻量前置 → 重量后置：Hook 做轻量检查（格式、lint），CI 做重量检查（测试、构建）
2. 保持 Hook 快速：pre-commit 应在 5 秒内完成
3. 统一管理：用 husky/pre-commit 框架让团队共享 hook 配置
4. 不依赖 Hook 做安全：安全策略必须在服务端 enforce
5. CI 是最终防线：不要只在本地 hook 检查，CI 必须重复验证
```

## 7. CI 集成（Actions / GitLab CI / act）

### 7.1 GitHub Actions 基础 CI

```yaml
# .github/workflows/ci.yml
name: CI
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'npm' }
      - run: npm ci
      - run: npm run lint
      - run: npm test
  # 检查 commit message 格式
  commitlint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: wagoid/commitlint-github-action@v5
```

### 7.2 CI 中的提交规范检查

```yaml
# .github/workflows/git-check.yml
name: Git Commit Check
on: [push, pull_request]
jobs:
  commit-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with: { fetch-depth: 0 }
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

### 7.3 act：本地运行 GitHub Actions

```bash
winget install nektos.act      # Windows
brew install act               # Mac

act                              # 本地运行默认 workflow
act -j test                      # 运行特定 job
act pull_request                 # 运行特定事件
act -n                           # 查看 workflow 而不运行
act -P ubuntu-latest=catthehacker/ubuntu:act-latest   # 指定 runner 镜像
```

## 8. config 三层配置体系

```bash
# System (所有用户) — 最低优先级，文件: /etc/gitconfig
git config --system --list
# Global (当前用户) — 中优先级，文件: ~/.gitconfig 或 ~/.config/git/config
git config --global --list
# Local (当前仓库) — 最高优先级，文件: .git/config
git config --local --list
# 查看某个配置的来源
git config --show-origin user.name
```

| 层级 | 影响范围 | 优先级 |
|------|---------|:---:|
| 系统级 | 所有用户所有仓库 | 低 |
| 全局级 | 当前用户所有仓库 | 中 |
| 本地级 | 当前仓库 | **高** |

## 9. includeIf 与身份定制

### 9.1 按目录自动切换身份

```ini
# ~/.gitconfig
[includeIf "gitdir:~/work/"]
    path = ~/.gitconfig-work

[includeIf "gitdir:~/personal/"]
    path = ~/.gitconfig-personal
```

```ini
# ~/.gitconfig-work
[user]
    name = Work Name
    email = work@company.com
```

```ini
# ~/.gitconfig-personal
[user]
    name = Personal Name
    email = personal@gmail.com
```

> 💡 `~/work/` 下仓库自动用工作身份，`~/personal/` 下自动用个人身份——开源/公司项目切换零操作。

### 9.2 基础配置

```bash
git config --global init.defaultBranch main          # 默认分支名（Git 2.28+）
git config --local user.email "work@company.com"     # 按仓库设置不同身份
git config --global core.editor "code --wait"        # VS Code 编辑器
```

## 10. 别名定制大全

### 10.1 常用别名

```bash
git config --global alias.st "status -sb"
git config --global alias.lg "log --oneline --graph --all"
git config --global alias.ll "log --oneline --all --decorate"
git config --global alias.co checkout
git config --global alias.sw switch
git config --global alias.cm "commit -m"
git config --global alias.ca "commit --amend"
git config --global alias.can "commit --amend --no-edit"
git config --global alias.unstage "restore --staged ."
git config --global alias.discard "restore ."
git config --global alias.undo "reset --soft HEAD~1"
git config --global alias.ds "diff --staged"
git config --global alias.rl "reflog"
git config --global alias.last "log -1 HEAD"
```

### 10.2 高价值自定义别名（`!` 开头 = shell 命令）

```bash
git config --global alias.wip '!git add -A && git commit -m "WIP: $(date +%Y-%m-%d_%H:%M:%S)"'
git config --global alias.rank "shortlog -sn --no-merges"
git config --global alias.clean-branches '!git branch --merged | grep -v "\*\|main\|master" | xargs -r git branch -d'
git config --global alias.today '!git log --oneline --since="6am" --author="$(git config user.name)"'
git config --global alias.sync '!git fetch --all --prune && git pull --rebase'
git config --global alias.force-push 'push --force-with-lease'
git config --global alias.prune-branches '!git fetch -p && git branch -vv | awk "/: gone]/{print \$1}" | xargs -r git branch -D'
git config --global alias.rollback '!sh -c "git stash && git reset --hard $1 && git stash pop" -'
```

```ini
# 直接编辑 ~/.gitconfig 的 [alias] 段（推荐写法）
[alias]
  st = status -sb
  lg = log --graph --oneline --decorate --all -20
  nb = switch -c                    # 新建并切换分支
  amend = commit --amend --no-edit
  unwip = !git log -1 --oneline | grep -q "WIP:" && git reset --soft HEAD~1 || echo "Last commit is not a WIP"
  upstream = branch --set-upstream-to=origin/$(git branch --show-current)
```

## 11. 差异工具 / 性能优化 / 提交模板

### 11.1 差异与合并工具

```bash
git config --global diff.tool vscode
git config --global difftool.vscode.cmd "code --wait --diff \$LOCAL \$REMOTE"
git config --global merge.tool vscode
git config --global mergetool.vscode.cmd "code --wait \$MERGED"
git config --global diff.colorMoved zebra        # 移动的代码用不同颜色
git config --global diff.algorithm histogram     # 更易读的 diff
git config --global merge.conflictstyle diff3    # 冲突显示三栏（含共同祖先）
```

### 11.2 性能优化配置

```bash
git config --global core.fscache true            # 文件系统缓存（Windows 必开）
git config --global core.untrackedCache true     # 加速 status
git config --global core.preloadindex true       # 预加载索引
git config --global core.fsmonitor true          # FS Monitor（Git 2.37+，极大加速）
git config --global http.postBuffer 524288000    # 500MB 缓冲区
git config --global fetch.parallel 8             # 并行 fetch
git config --global fetch.prune true             # fetch 时清理远程已删分支
git config --global gc.writeCommitGraph true     # 加速 log/blame
git config --global core.multiPackIndex true     # 多包索引
git config --global pull.rebase true             # pull 默认 rebase
git config --global rebase.autoStash true        # rebase 前自动 stash
git config --global rerere.enabled true          # 记住冲突解决方案
git config --global help.autocorrect 1           # 命令输错自动纠错
```

### 11.3 提交模板

```bash
cat > ~/.gitmessage << 'EOF'
# <type>(<scope>): <subject>
# 
# <body>
# 
# <footer>
# 
# type: feat | fix | docs | style | refactor | test | chore | perf | ci
# subject: ≤50字符，现在时，首字母小写
# footer: 关联issue（Closes #123）
EOF

git config --global commit.template ~/.gitmessage
```

## 12. 团队配置共享与新人 Onboarding

### 12.1 团队配置共享三方案

| 方案 | 做法 | 适用 |
|------|------|------|
| 推荐文件 | 项目根目录放 `.gitconfig-recommended` + README 说明复制 | 一次性初始化 |
| 本地配置 | `git config --local` 只对当前项目生效 | 小团队（clone 不复制） |
| Shell 脚本 | `setup-git.sh` 统一配置 | 大团队 |

### 12.2 新人 Onboarding 脚本

```bash
#!/bin/bash
# setup-git.sh — 新成员入职一键配置

echo "👤 请输入你的姓名:"
read NAME
echo "📧 请输入你的企业邮箱:"
read EMAIL

git config --global user.name "$NAME"
git config --global user.email "$EMAIL"
git config --global core.autocrlf input
git config --global core.ignorecase false
git config --global pull.rebase true
git config --global fetch.prune true
git config --global alias.st "status -sb"
git config --global alias.lg "log --graph --oneline --decorate --all -20"

# 提交模板
cat > ~/.gitmessage << 'TEMPLATE'
# feat|fix|docs|style|refactor|test|chore|perf|ci: 简短描述
# 
# 详细说明（可选）
# 
# Closes #ISSUE_ID
TEMPLATE
git config --global commit.template ~/.gitmessage

# 全局 .gitignore
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
```

> 💡 Java 后端补充推荐：`git config --global core.ignorecase false`（大小写敏感——Mac/Windows 默认不敏感，极易踩坑）、`git config --global core.filemode false`（忽略文件权限变更）。

## 13. 核心要点

> 🎯 **核心要点**：
> - 钩子定位：**轻量前置、重量后置**——本地 hook 快检查，CI 兜底重检查；
> - 团队共享 hooks：`core.hooksPath` 指向版本化目录 `.githooks/`；
> - 三种自动化框架：手写 bash（灵活）、husky+lint-staged（前端）、pre-commit 框架（多语言）；
> - config 三层层级：local > global > system；`includeIf` 按目录切身份；
> - 别名 = 效率杠杆：高频命令缩写 + `!` shell 别名做复合操作；
> - 性能配置九件套：fscache/untrackedCache/fsmonitor/commitGraph/parallel 等。

## 14. 参考来源

- [Pro Git Book：Git 钩子](https://git-scm.com/book/zh/v2/Customizing-Git-Git-钩子)
- [Git 官方文档：git-config / githooks](https://git-scm.com/docs)
- [Husky 官方文档](https://typicode.github.io/husky/)
- [pre-commit 框架文档](https://pre-commit.com/)
- [act 项目（本地 Actions）](https://github.com/nektos/act)

---

**下一模块**：[09-Git标签子模块与Monorepo](09-Git标签子模块与Monorepo.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
