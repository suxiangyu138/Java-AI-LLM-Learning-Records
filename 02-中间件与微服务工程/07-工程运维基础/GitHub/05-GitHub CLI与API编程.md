# 05 - GitHub CLI 与 API 编程

> `gh` 命令行工具让你在终端完成所有 GitHub 操作；REST API 和 GraphQL API 让你以编程方式集成 GitHub 到自动化流程。从日常操作到高级集成全覆盖。

---

## 目录

1. [GitHub CLI (gh) 入门](#1-github-cli-gh-入门)
2. [gh 常用命令速查](#2-gh-常用命令速查)
3. [gh 与 Git 命令的协作](#3-gh-与-git-命令的协作)
4. [gh 扩展与自定义](#4-gh-扩展与自定义)
5. [GitHub REST API](#5-github-rest-api)
6. [GitHub GraphQL API](#6-github-graphql-api)
7. [API 认证与速率限制](#7-api-认证与速率限制)
8. [自动化脚本实战](#8-自动化脚本实战)
9. [常见面试题](#9-常见面试题)

---

## 1. GitHub CLI (gh) 入门

### 1.1 安装与认证

```bash
# ═══ 安装 ═══
# macOS
brew install gh

# Windows
winget install --id GitHub.cli
# 或从 https://cli.github.com 下载安装包

# Linux (Ubuntu)
type -p curl >/dev/null || sudo apt install curl -y
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | \
  sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
sudo apt update && sudo apt install gh

# ═══ 登录 ═══
gh auth login
# 交互式选择：GitHub.com → HTTPS → Login with web browser
# → 浏览器中确认 → 终端获得 token

# 验证
gh auth status
# ✓ Logged in to github.com as zhangsan
```

### 1.2 gh 的定位

```
┌───────────────────────────────────────────────────────┐
│                    gh vs git                           │
│                                                        │
│  git   → 负责本地 Git 操作（commit/push/pull/merge）    │
│  gh    → 负责 GitHub 平台操作（PR/Issue/Release/Action）│
│                                                        │
│  典型工作流：                                          │
│    gh repo clone user/repo   ← 克隆仓库               │
│    git checkout -b feat/x    ← git 创建分支            │
│    git commit && git push    ← git 提交+推送           │
│    gh pr create              ← gh 创建 PR              │
│    gh pr review --approve    ← gh 审批 PR              │
│    gh pr merge               ← gh 合并 PR              │
│    gh release create v1.0.0  ← gh 发布 Release         │
└───────────────────────────────────────────────────────┘
```

---

## 2. gh 常用命令速查

### 2.1 Repository 仓库操作

```bash
# 创建仓库
gh repo create my-project --public --clone
gh repo create my-project --private --description "My App"

# 克隆仓库
gh repo clone user/repo           # 自动使用 SSH/HTTPS
gh repo clone user/repo -- --depth=1  # 浅克隆

# 查看仓库
gh repo view user/repo            # 在终端展示仓库信息
gh repo view --web                # 在浏览器打开

# Fork
gh repo fork user/repo --clone    # Fork + Clone 一步到位

# 列出仓库
gh repo list                      # 列出自己的仓库
gh repo list user                 # 列出指定用户的仓库
```

### 2.2 Pull Request 操作

```bash
# 创建 PR
gh pr create
# → 交互式：选择 base 分支 → 填标题 → 填描述

gh pr create \
  --title "feat: add login API" \
  --body "Implementation of JWT-based login endpoint" \
  --base main \
  --label "enhancement,backend" \
  --assignee @me \
  --reviewer "zhangsan,lisi" \
  --milestone "v1.0.0"

# 创建 Draft PR
gh pr create --draft

# 查看 PR
gh pr list                        # 列出所有 PR
gh pr list --state open --label bug
gh pr list --assignee @me
gh pr view 42                     # 查看 #42 详情
gh pr view --web 42               # 浏览器中打开

# Review PR
gh pr review 42 --approve
gh pr review 42 --comment -b "建议优化数据库查询"
gh pr review 42 --request-changes -b "需要补充测试"

# Checkout PR 到本地
gh pr checkout 42

# 合并 PR
gh pr merge 42 --squash
gh pr merge 42 --merge
gh pr merge 42 --rebase

# 关闭 PR
gh pr close 42
```

### 2.3 Issue 操作

```bash
# 创建 Issue
gh issue create \
  --title "数据库连接池泄漏" \
  --body "在高并发场景下出现连接超时..." \
  --label "bug,high-priority" \
  --assignee @me \
  --milestone "v1.0.0"

# 列表
gh issue list --state open
gh issue list --label bug --assignee @me
gh issue list --milestone "v1.0.0"

# 查看
gh issue view 42
gh issue view 42 --comments    # 含评论
gh issue view 42 --web

# 状态
gh issue close 42
gh issue reopen 42

# 开发分支（自动创建关联分支）
gh issue develop 42 --checkout
# → 创建分支 42-issue-title 并切换
```

### 2.4 Release 操作

```bash
# 创建 Release
gh release create v1.0.0 \
  --title "v1.0.0 - 首个正式版本" \
  --notes "## 新功能 ..." \
  --target main

# 创建 Release + 附带构建产物
gh release create v1.0.0 \
  --title "Release v1.0.0" \
  --generate-notes \
  target/*.jar

# 列出/查看/下载
gh release list
gh release view v1.0.0
gh release download v1.0.0 --pattern "*.jar"
```

### 2.5 Actions 操作

```bash
# 查看 Workflow 运行
gh run list                       # 最近的运行
gh run list --workflow "Java CI"  # 按 Workflow 名过滤
gh run list --branch main --status failure

# 查看运行详情
gh run view <run-id>
gh run view --job <job-id>
gh run view --log <run-id>        # 查看日志

# 重新运行
gh run rerun <run-id>
gh run rerun <run-id> --failed    # 只重跑失败的 Job

# 手动触发 Workflow
gh workflow run "Deploy" -f environment=staging

# 查看 Workflow
gh workflow list
gh workflow view "Java CI"
```

---

## 3. gh 与 Git 命令的协作

### 3.1 日常开发脚本

```bash
#!/bin/bash
# create-pr.sh — 一条命令完成：创建分支 → push → 创建 PR

BRANCH="feat/$1"
TITLE="$2"

git checkout -b "$BRANCH"
# ... 写代码 ...
git add .
git commit -m "$TITLE"
git push -u origin "$BRANCH"
gh pr create --title "$TITLE" --base main --web
# --web 在浏览器中打开 PR 填写描述

# 使用：
# ./create-pr.sh "user-login" "feat: add user login with JWT"
```

### 3.2 常用别名

```bash
# ~/.gitconfig 中添加 git 别名，调用 gh
[alias]
    # 创建 PR
    pr = "!gh pr create"

    # 查看所有 PR
    prs = "!gh pr list"

    # Checkout 某个 PR 到本地
    prco = "!gh pr checkout"

    # 在浏览器打开当前仓库
    browse = "!gh repo view --web"

    # 查看 CI 状态
    ci = "!gh run list --limit 5"
```

---

## 4. gh 扩展与自定义

### 4.1 安装扩展

```bash
# 查看已安装的扩展
gh extension list

# 安装官方/社区扩展
gh extension install github/gh-copilot      # Copilot CLI
gh extension install dlvhdr/gh-dash         # GitHub Dashboard TUI
gh extension install yusukebe/gh-markdown-preview  # Markdown 预览
gh extension install mislav/gh-branch       # 分支管理增强

# 搜索扩展
gh extension search dashboard
```

### 4.2 创建自定义扩展

```bash
# gh 扩展本质是一个可执行的 gh-<name> 脚本
# 放在 PATH 中即可被 gh 调用

# ~/bin/gh-stale-branches（可执行文件）
#!/bin/bash
# 列出本地已合并到 main 的分支
echo "=== 已合并到 main 的分支 ==="
git branch --merged main | grep -v "main\|*"

# 使用：
gh stale-branches
```

---

## 5. GitHub REST API

### 5.1 API 基础

```bash
# REST API 基础 URL
# https://api.github.com

# ═══ 公开访问（不含认证）════
# 速率限制：60 次/小时/IP

# 获取用户信息
curl https://api.github.com/users/octocat

# 获取仓库信息
curl https://api.github.com/repos/spring-projects/spring-boot

# 获取 Issues
curl https://api.github.com/repos/user/repo/issues?state=open&labels=bug

# ═══ 认证访问 ═══
# 速率限制：5000 次/小时/用户

# 使用 Personal Access Token
curl -H "Authorization: Bearer ghp_xxxx" \
     -H "Accept: application/vnd.github+json" \
     https://api.github.com/user

# 或通过 gh 命令（自动携带 token）
gh api /user
gh api /repos/spring-projects/spring-boot
```

### 5.2 常用 API 端点

```bash
# ═══ 用户 ═══
gh api /user                                  # 当前用户信息
gh api /users/{username}                       # 指定用户
gh api /users/{username}/repos                 # 用户的仓库列表

# ═══ 仓库 ═══
gh api /repos/{owner}/{repo}                   # 仓库信息
gh api /repos/{owner}/{repo}/branches          # 分支列表
gh api /repos/{owner}/{repo}/commits           # 提交历史
gh api /repos/{owner}/{repo}/contributors      # 贡献者
gh api /repos/{owner}/{repo}/languages         # 语言统计

# ═══ Issues ═══
gh api /repos/{owner}/{repo}/issues            # Issue 列表
gh api /repos/{owner}/{repo}/issues/42         # Issue #42
gh api /repos/{owner}/{repo}/issues/42/comments # Issue 评论

# 创建 Issue（POST）
gh api /repos/{owner}/{repo}/issues \
  -f title="Bug: NPE in service" \
  -f body="详细描述..." \
  -f labels='["bug","high-priority"]'

# ═══ Pull Requests ═══
gh api /repos/{owner}/{repo}/pulls             # PR 列表
gh api /repos/{owner}/{repo}/pulls/42          # PR #42
gh api /repos/{owner}/{repo}/pulls/42/reviews  # PR 审查

# ═══ Actions ═══
gh api /repos/{owner}/{repo}/actions/runs      # Workflow 运行列表
gh api /repos/{owner}/{repo}/actions/workflows  # Workflow 列表

# ═══ 搜索 ═══
gh api /search/repositories?q=spring+boot+language:java
gh api /search/code?q=@RestController+language:java+repo:user/repo
```

### 5.3 API 响应分页

```bash
# GitHub API 分页机制
# Header: Link: <url?page=2>; rel="next", <url?page=5>; rel="last"

# gh api 自动处理分页
gh api /repos/user/repo/issues --paginate  # 获取所有页
gh api /repos/user/repo/issues --jq '.[] | {title, state}'  # 过滤字段

# 或使用 curl 的便捷方式
gh api /repos/user/repo/issues -X GET \
  -f per_page=100 -f page=1
```

---

## 6. GitHub GraphQL API

### 6.1 GraphQL vs REST 对比

```
REST — /repos/owner/repo/issues → 返回完整 Issue 对象（含不需要的字段）
        需要多个请求才能获取关联数据

GraphQL — 单次请求精确获取所需数据（减少 Over-fetching / Under-fetching）

{
  repository(owner: "octocat", name: "Hello-World") {
    issues(first: 5, states: OPEN) {
      nodes {
        title
        createdAt
        labels(first: 3) { nodes { name } }
        assignees(first: 2) { nodes { login } }
      }
    }
  }
}
```

### 6.2 实战查询

```bash
# GraphQL API 端点
# POST https://api.github.com/graphql

# 使用 gh api
gh api graphql -f query='
query {
  viewer {
    login
    repositories(first: 5, orderBy: {field: STARGAZERS, direction: DESC}) {
      nodes {
        name
        stargazerCount
        primaryLanguage { name }
      }
    }
  }
}'

# 搜索仓库
gh api graphql -f query='
query($query: String!) {
  search(query: $query, type: REPOSITORY, first: 10) {
    repositoryCount
    edges {
      node {
        ... on Repository {
          nameWithOwner
          description
          stargazerCount
        }
      }
    }
  }
}' -f query='spring boot starter language:java stars:>1000'
```

### 6.3 GitHub API Explorer

> 在浏览器中打开 https://docs.github.com/en/graphql/overview/explorer — GitHub 提供的交互式 GraphQL 查询工具，带自动补全和文档。

---

## 7. API 认证与速率限制

### 7.1 认证方式对比

| 方式 | 适用场景 | 创建位置 | 格式 |
|------|---------|---------|------|
| **Personal Access Token (Classic)** | 个人脚本/CLI | Settings → Developer settings → PAT | `ghp_xxxx` |
| **Personal Access Token (Fine-grained)** | 推荐，精确权限控制 | 同上 | `github_pat_xxxx` |
| **GitHub App Token** | 自动化服务/CI | Settings → GitHub Apps | 需 OAuth 流程 |
| **GITHUB_TOKEN** | GitHub Actions 内 | 自动提供 | `${{ secrets.GITHUB_TOKEN }}` |

### 7.2 速率限制

```bash
# 查看当前速率限制
gh api /rate_limit

# 响应示例：
{
  "resources": {
    "core": {
      "limit": 5000,         # 每小时限制
      "remaining": 4998,     # 剩余次数
      "reset": 1705312800    # 重置时间戳
    },
    "search": {
      "limit": 30,           # 搜索 API 限制更低
      "remaining": 30
    },
    "graphql": {
      "limit": 5000,
      "remaining": 5000
    }
  }
}

# ═══ 处理速率限制 ═══
# 1. 检查 remaining 值
# 2. 等待 reset 时间
# 3. 使用条件请求（If-None-Match / If-Modified-Since）减少调用
# 4. 使用 GraphQL 减少多次 REST 请求
```

---

## 8. 自动化脚本实战

### 8.1 批量关闭过期 Issue

```bash
#!/bin/bash
# close-stale-issues.sh

REPO="user/repo"
STALE_DAYS=90
STALE_DATE=$(date -d "$STALE_DAYS days ago" +%Y-%m-%d)

echo "查找 $STALE_DATE 之前创建的 Issue..."

gh issue list --repo "$REPO" \
  --state open \
  --label "question" \
  --search "created:<$STALE_DATE" \
  --json number,title \
  --jq '.[] | "\(.number) \(.title)"' |
while read -r number title; do
  echo "关闭 #$number: $title"
  gh issue close "$number" --repo "$REPO" \
    --comment "已自动关闭（$STALE_DAYS 天无活动）。如仍需要请 Reopen。"
done

echo "完成！"
```

### 8.2 统计代码贡献

```bash
#!/bin/bash
# contribution-stats.sh — 统计团队成员的 PR 和 Review 数据

REPO="user/repo"
SINCE="2024-01-01"

echo "=== PR 统计（$SINCE 至今）==="

# PR 创建数
for user in zhangsan lisi wangwu; do
  count=$(gh pr list --repo "$REPO" --state merged \
    --author "$user" --search "merged:>=$SINCE" --json number --jq 'length')
  echo "$user: $count 个 PR"
done

echo ""
echo "=== Review 统计 ==="

gh pr list --repo "$REPO" --state merged --limit 100 \
  --search "merged:>=$SINCE" --json number |
  jq -r '.[].number' | while read pr; do
    gh api "repos/$REPO/pulls/$pr/reviews" \
      --jq '.[].user.login' 2>/dev/null
  done | sort | uniq -c | sort -rn
```

### 8.3 一键创建 Milestone + Issues

```bash
#!/bin/bash
# setup-sprint.sh — 为新 Sprint 创建 Milestone 和任务 Issues

REPO="user/repo"
SPRINT="Sprint-$(date +%Y-W%V)"  # Sprint-2024-W03
DUE_DATE=$(date -d "+2 weeks" +%Y-%m-%d)

# 1. 创建 Milestone
echo "创建 Milestone: $SPRINT (截止 $DUE_DATE)"
gh api "repos/$REPO/milestones" \
  -f title="$SPRINT" \
  -f due_on="${DUE_DATE}T23:59:59Z" \
  -f description="Sprint $SPRINT 开发任务"

# 2. 批量创建 Issues
tasks=(
  "代码审查：支付模块"
  "集成测试：订单流程"
  "性能优化：用户查询接口"
  "文档更新：API 文档"
)

for task in "${tasks[@]}"; do
  echo "创建 Issue: $task"
  gh issue create --repo "$REPO" \
    --title "$task" \
    --milestone "$SPRINT" \
    --label "task"
done

echo "Sprint 创建完成！"
```

---

## 9. 常见面试题

### Q1：gh 和 git 命令的区别？

> `git` 操作本地 Git 仓库（commit、push、pull、merge），`gh` 操作 GitHub 平台功能（PR、Issue、Release、Actions）。gh 不替代 git，而是 git 的补充。详见第1节。

### Q2：GitHub REST API 和 GraphQL API 各自适用什么场景？

> REST 适合简单查询和 CRUD 操作（获取仓库信息、创建 Issue）；GraphQL 适合需要精确控制返回字段和关联查询的复杂场景（一次获取 Issue + Labels + Assignees + Comments）。详见第5-6节。

### Q3：如何通过命令行批量操作 GitHub Issues/PRs？

> 使用 `gh` 的 `--json` + `--jq` 导出结构化数据 → 管道处理 → 逐条操作。如 `gh issue list --json number --jq '.[].number' | while read n; do gh issue close $n; done`。详见第8节。
