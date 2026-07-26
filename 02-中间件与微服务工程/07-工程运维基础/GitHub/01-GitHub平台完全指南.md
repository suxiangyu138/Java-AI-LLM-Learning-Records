# 01 - GitHub 平台完全指南

> 从仓库创建到高级设置，从个人使用到组织管理——GitHub 不仅仅是代码托管，更是全球最大的开发者协作平台。掌握平台能力是专业工程师的基本素养。

---

## 目录

1. [GitHub 平台概述](#1-github-平台概述)
2. [仓库全生命周期管理](#2-仓库全生命周期管理)
3. [仓库设置深度解析](#3-仓库设置深度解析)
4. [分支管理与保护规则](#4-分支管理与保护规则)
5. [GitHub 社交与发现机制](#5-github-社交与发现机制)
6. [GitHub 配置文件与个性化](#6-github-配置文件与个性化)
7. [GitHub 生态工具全景](#7-github-生态工具全景)
8. [GitHub 定价与计划选择](#8-github-定价与计划选择)
9. [常见面试题](#9-常见面试题)

---

## 1. GitHub 平台概述

### 1.1 GitHub 的定位

```
GitHub = Git 托管 + 协作平台 + DevOps 平台 + 社交网络 + AI 开发工具

┌────────────────────────────────────────────────────────┐
│                    GitHub 平台能力层                     │
├────────────────────────────────────────────────────────┤
│ AI 层      │ Copilot / Copilot Chat / Code Review AI    │
├────────────────────────────────────────────────────────┤
│ DevOps 层  │ Actions (CI/CD) / Packages / Releases       │
├────────────────────────────────────────────────────────┤
│ 协作层     │ PR / Issues / Projects / Discussions / Wiki │
├────────────────────────────────────────────────────────┤
│ 安全层     │ Code Scanning / Secret Scanning / Dependabot│
├────────────────────────────────────────────────────────┤
│ 托管层     │ Git Repositories / Codespaces / Pages       │
├────────────────────────────────────────────────────────┤
│ 社区层     │ Stars / Forks / Follow / Sponsors / Gists   │
└────────────────────────────────────────────────────────┘
```

### 1.2 GitHub vs GitLab vs Bitbucket

| 维度 | GitHub | GitLab | Bitbucket |
|------|--------|--------|-----------|
| **定位** | 全球最大开源社区 | 一体化 DevOps 平台 | Atlassian 生态集成 |
| **CI/CD** | GitHub Actions | GitLab CI（更成熟） | Bitbucket Pipelines |
| **私有仓库** | 免费（无限） | 免费（无限） | 免费（5人限制） |
| **AI 能力** | Copilot（最强） | GitLab Duo | 无 |
| **开源生态** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **自托管** | GitHub Enterprise | GitLab CE/EE | Bitbucket Data Center |
| **包管理** | GitHub Packages | GitLab Container Registry | 需第三方集成 |
| **项目管理** | Projects（看板+表格） | 内置 Issue Board | Jira 深度集成 |

> 🎯 开源项目首选 GitHub，企业自建 DevOps 选 GitLab，Jira/Confluence 生态选 Bitbucket。

---

## 2. 仓库全生命周期管理

### 2.1 创建仓库的关键决策

```bash
# ═══ 创建仓库时的必选项 ═══

# 1. 可见性：Public vs Private
#    Public  → 任何人可看、可 Fork、可提 PR（适合开源）
#    Private → 仅受邀者可见（适合企业/个人项目）

# 2. README.md
#    ✅ 建议初始化时勾选 — 仓库的第一份文档
#    包含：项目简介、安装步骤、使用示例、License、贡献指南

# 3. .gitignore
#    ✅ 必须选 — 根据语言/框架选择模板
#    GitHub 提供了 100+ 预置模板

# 4. License
#    ✅ 开源项目必须选 —
#    MIT     → 最宽松（随便用，保留版权声明）
#    Apache 2.0 → 宽松 + 专利许可
#    GPL v3  → 强 Copyleft（衍生项目也必须开源）
#    AGPL v3 → 网络服务也需开源（最严格）
```

### 2.2 仓库描述与标签

```markdown
<!-- GitHub 仓库页面的关键信息位 -->

# 仓库名称：简洁、可搜索
# 描述（Description）：一句话说明，出现在搜索列表中
# 网站（Website）：项目官网或文档链接

# Topics（标签）— ⭐ 影响 GitHub 搜索排名！
# 例：java, spring-boot, microservices, rest-api, backend

# About 配置（仓库首页右侧）：
#   ✅ Description  + Topics + Website + Releases
```

### 2.3 Fork 工作流

```
Fork 的本质：
  别人的 Repo（Upstream）
       │
       │ Fork（GitHub 服务器上复制一份）
       ▼
  你的 Repo（Origin = Fork 出来的副本）
       │
       │ Clone（下载到本地）
       ▼
  本地 Repo

同步上游更新：
  git remote add upstream <原仓库URL>
  git fetch upstream
  git checkout main
  git merge upstream/main
  git push origin main

贡献回上游：
  1. 在你的 Fork 上创建 Feature Branch
  2. 开发 + Commit + Push
  3. 在 GitHub 上发起 Pull Request → 目标为 Upstream Repo
```

---

## 3. 仓库设置深度解析

### 3.1 Settings 核心配置项

| 设置区域 | 关键配置 | 说明 |
|---------|---------|------|
| **General** | Default branch | 默认分支名（main/master） |
| | Features（Wiki/Issues/Discussions/Projects） | 按需启用/禁用 |
| | Pull Requests（合并方式） | Merge Commit / Squash / Rebase |
| | Automatically delete head branches | ✅ 强烈建议开启 |
| **Collaborators** | 添加协作者 | 直接给仓库权限 |
| **Branches** | Branch protection rules | ⭐ 核心安全机制 |
| **Actions** | Actions permissions | 控制 Workflow 权限 |
| **Secrets & Variables** | Actions / Codespaces / Dependabot | CI/CD 密钥管理 |
| **Security** | Code scanning / Secret scanning | 安全扫描配置 |
| **Integrations** | GitHub Apps / Webhooks / Deploy Keys | 外部集成 |

### 3.2 合并策略对比

| 策略 | 命令 | Git 历史 | 适用场景 |
|------|------|---------|---------|
| **Merge Commit** | `git merge --no-ff` | 保留完整分支历史 | 需要看到 Feature 分支全貌 |
| **Squash and Merge** | `git merge --squash` | 所有 commit 压为 1 个 | 保持主分支历史干净 |
| **Rebase and Merge** | `git rebase` | 线性历史，无 Merge Commit | 追求线性历史 |

```
Merge Commit:
  *   Merge (PR #42)
  |\
  | * feat: add B
  | * feat: add A
  |/
  *

Squash:
  * feat: add login page (#42)

Rebase:
  * feat: add B
  * feat: add A
```

> 💡 建议配置：个人项目用 Squash（历史干净），团队项目用 Merge Commit（可追溯），开源项目按社区约定。

### 3.3 Webhook 配置

```json
// GitHub → 第三方服务的 HTTP 回调
POST https://your-server.com/webhook
Headers:
  X-GitHub-Event: push          // 事件类型
  X-GitHub-Delivery: GUID       // 唯一ID（幂等）
  X-Hub-Signature-256: sha256=  // HMAC 签名（验证来源）

Body (push event):
{
  "ref": "refs/heads/main",
  "commits": [...],
  "repository": { "full_name": "user/repo", ... },
  "pusher": { "name": "username" }
}

// 典型用途：
// - push → 自动部署（CI/CD 触发）
// - pull_request → 通知 Slack/钉钉
// - issues → 同步到 Jira
```

---

## 4. 分支管理与保护规则

### 4.1 Branch Protection Rules

```yaml
# 分支保护规则 — 可配置的保护条件：

# ═══ 基础保护 ═══
Required approvals: 1-6          # 需要 N 个审批者 Approve
Dismiss stale approvals: true    # 新 commit 后重置审批
Require review from Code Owners: true  # CODEOWNERS 文件指定的人必须审批

# ═══ 状态检查 ═══
Require status checks: true      # CI/CD 必须通过
  - "build (ubuntu-latest)"
  - "test (ubuntu-latest)"
  - "lint"

# ═══ 分支更新策略 ═══
Require branches up to date: true  # PR 分支必须与 main 同步
Require conversation resolution: true # 所有讨论必须解决

# ═══ 推送限制 ═══
Restrict push: true              # 只有特定人员/团队可推送
Allow force pushes: false        # 禁止 force push
Allow deletions: false           # 禁止删除分支
```

### 4.2 CODEOWNERS 文件

```markdown
# .github/CODEOWNERS — 定义代码责任人（自动请求 Review）

# 全局规则
*                           @team-leads

# 目录级别
/frontend/                  @frontend-team
/backend/api/               @backend-lead @senior-dev

# 文件级别
*.java                      @java-team
/Dockerfile                 @devops-team
/.github/workflows/         @devops-team

# 精确匹配
/docs/README.md             @tech-writer

# 生效方式：
# - PR 修改了匹配的文件 → 自动添加 Owner 为 Reviewer
# - 配合 Branch Protection "Require review from Code Owners"
```

---

## 5. GitHub 社交与发现机制

### 5.1 如何让项目被更多人发现

```markdown
# ⭐ GitHub 搜索排名因素（大致权重）
1. 仓库名与搜索词匹配度
2. Description 中关键词匹配
3. Topics 标签
4. README 中的内容
5. Star 数量（影响力信号）
6. Fork 数量和近期活跃度
7. 文档质量（README/Wiki）

# ═══ 优化清单 ═══
✅ 命名清晰：spring-boot-starter-xxx（便于搜索）
✅ Description 含关键词：A Spring Boot starter for...
✅ 添加 Topics：java, spring-boot, starter
✅ README 完善：徽章 + 截图 + 快速开始
✅ 开源 License
```

### 5.2 Star 与 Fork 的意义

| 操作 | 含义 | 对仓库的影响 |
|------|------|------------|
| **Star** | 收藏/点赞 | 提高搜索排名、显示在 Stargazer 列表中 |
| **Fork** | 复制仓库到自己账号 | 便于贡献代码或独立开发 |
| **Watch** | 关注动态 | 接收 Issues/PR/Discussions 通知 |
| **Follow** | 关注用户 | 在其动态页看到其活动 |

---

## 6. GitHub 配置文件与个性化

### 6.1 Profile README

```markdown
<!-- 同用户名仓库 → README.md 自动展示在个人首页 -->

# 用户名（与 GitHub 账号同名）
# 仓库名 = 用户名

<!-- 示例：https://github.com/zhangsan -->
<!-- README.md 内容 -->
### Hi there 👋

- 🔭 I'm currently working on: Spring Cloud microservices
- 🌱 I'm learning: Rust
- 👯 I'm looking to collaborate on: Open source Java projects
- 💬 Ask me about: Java Backend, Distributed Systems
- 📫 How to reach me: zhangsan@example.com

### 🛠 Tech Stack
![Java](https://img.shields.io/badge/-Java-007396?logo=java)
![Spring](https://img.shields.io/badge/-Spring-6DB33F?logo=spring)
```

### 6.2 社区健康文件

```markdown
# .github/ 目录 — 仓库级别的配置文件
.github/
├── CODEOWNERS              # 代码所有者
├── PULL_REQUEST_TEMPLATE.md  # PR 模板
├── ISSUE_TEMPLATE/
│   ├── bug_report.md       # Bug 报告模板
│   └── feature_request.md  # 功能请求模板
├── CONTRIBUTING.md         # 贡献指南
├── CODE_OF_CONDUCT.md       # 行为准则
├── SECURITY.md             # 安全漏洞报告流程
└── FUNDING.yml             # 赞助配置
```

```markdown
<!-- PULL_REQUEST_TEMPLATE.md -->
## 变更描述
<!-- 简要描述本次变更 -->

## 关联 Issue
Closes #

## 变更类型
- [ ] Bug 修复
- [ ] 新功能
- [ ] 重构
- [ ] 文档更新

## 测试
- [ ] 单元测试通过
- [ ] 集成测试通过

## Checklist
- [ ] 代码符合项目规范
- [ ] 已添加必要的测试
- [ ] 文档已更新
```

### 6.3 Badge 徽章体系

```markdown
<!-- 在 README 中使用 Shields.io 徽章 -->

<!-- 构建状态 -->
[![Build](https://github.com/user/repo/actions/workflows/build.yml/badge.svg)](https://github.com/user/repo/actions)

<!-- 版本 -->
![Version](https://img.shields.io/maven-central/v/com.example/artifact)

<!-- 许可证 -->
![License](https://img.shields.io/github/license/user/repo)

<!-- 代码覆盖率 -->
![Coverage](https://img.shields.io/codecov/c/github/user/repo)

<!-- Java 版本 -->
![Java](https://img.shields.io/badge/Java-17%2B-blue)

<!-- Star 数 -->
![Stars](https://img.shields.io/github/stars/user/repo?style=social)
```

---

## 7. GitHub 生态工具全景

| 工具/服务 | 类型 | 用途 |
|----------|------|------|
| **GitHub Desktop** | GUI 客户端 | 图形化 Git 操作 |
| **GitHub CLI (`gh`)** | 命令行 | 终端管理 GitHub |
| **GitHub Mobile** | 移动端 | 手机查看/审批/回复 |
| **GitHub Copilot** | AI | 代码补全与生成 |
| **GitHub Codespaces** | 云 IDE | 浏览器中 VS Code |
| **GitHub Actions** | CI/CD | 自动化工作流 |
| **GitHub Pages** | 静态托管 | 项目文档/博客 |
| **GitHub Packages** | 包管理 | npm/Maven/Docker 仓库 |
| **GitHub Projects** | 项目管理 | 看板/表格视图 |
| **GitHub Discussions** | 论坛 | 社区讨论（非 Issue） |
| **GitHub Sponsors** | 赞助 | 资助开源开发者 |
| **Dependabot** | 安全 | 自动依赖更新+漏洞修复 |
| **CodeQL** | 安全 | 代码静态分析+漏洞扫描 |

---

## 8. GitHub 定价与计划选择

| 功能 | Free | Team | Enterprise |
|------|------|------|------------|
| 公开/私有仓库 | ✅ 无限 | ✅ 无限 | ✅ 无限 |
| Actions 分钟数 | 2000 分钟/月 | 3000 分钟/月 | 50000 分钟/月 |
| Codespaces | 120核时/月 | 共享额度 | 共享额度 |
| Packages 存储 | 500MB | 2GB | 50GB |
| 必需审查者 | ❌ | ✅ | ✅ |
| Code Owners | ❌ | ✅ | ✅ |
| 受保护分支 | ✅ | ✅ | ✅ |
| SAML/SCIM | ❌ | ❌ | ✅ |
| 审计日志 | ❌ | ❌ | ✅ |
| Copilot | 付费附加 | 付费附加 | 付费附加 |
| **月费** | $0 | $4/用户 | $21/用户 |

---

## 9. 常见面试题

### Q1：Fork 和 Clone 的区别？

> Fork 是在 GitHub 服务器上复制仓库（服务端），Clone 是下载到本地（本地）。Fork 用于贡献开源项目（保留关联关系），Clone 用于本地开发。

### Q2：GitHub 的分支保护规则可以做什么？

> 强制 PR 审查、要求 CI 通过、禁止 Force Push、要求 CODEOWNERS 审批、确保分支与主分支同步。详见第4节。

### Q3：GitHub Actions 和 Jenkins 的区别？

> Actions 是 GitHub 原生 CI/CD（YAML 配置、事件驱动、免费额度），Jenkins 是独立的 CI 服务器（更灵活但需自运维）。Actions 与 GitHub 深度集成，配置更简单。

### Q4：.github 目录是什么？

> 仓库级社区健康文件目录：PR 模板、Issue 模板、贡献指南、CODEOWNERS、安全策略等。可放在仓库的 `.github/` 目录或组织级的 `.github` 仓库中全局生效。
