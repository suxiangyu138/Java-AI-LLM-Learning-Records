# 21-Git生态与托管平台
> 全面解析 Git 生态全景、主流托管平台深度对比、GitHub CLI 实战、CI/CD 集成、安全合规与开源项目运营，帮助团队做出正确的平台选型与生态建设决策。

## 目录
1. [Git Ecosystem Panorama](#1-git-ecosystem-panorama)
2. [GitHub Deep Dive](#2-github-deep-dive)
3. [GitLab Deep Dive](#3-gitlab-deep-dive)
4. [Platform Comprehensive Comparison](#4-platform-comprehensive-comparison)
5. [Platform Selection Decision Tree](#5-platform-selection-decision-tree)
6. [Multi-Platform Sync](#6-multi-platform-sync)
7. [GitHub CLI (gh) in Action](#7-github-cli-gh-in-action)
8. [Git LFS Cross-Platform](#8-git-lfs-cross-platform)
9. [Security & Compliance](#9-security--compliance)
10. [Open Source Project Operations](#10-open-source-project-operations)

---

## 1. Git Ecosystem Panorama

Git 生态早已超越单一的版本控制工具，形成了一个从本地开发到云端协作再到生产部署的完整链路。

### 1.1 生态全景架构

```
                    ┌─────────────────────────────────────┐
                    │         Git 生态全景架构              │
                    └─────────────────────────────────────┘

  ┌───────────────────────────────────────────────────────────┐
  │                    Local Development                       │
  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
  │  │    Git    │  │   IDE    │  │   LFS    │  │   GUI    │  │
  │  │  CLI/CMD  │  │ 集成插件  │  │ 大文件管理 │  │ 可视化工具 │  │
  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
  └───────────────────────────────────────────────────────────┘
                              │ Git Push / Pull
  ┌───────────────────────────────────────────────────────────┐
  │                   Hosting Platforms                       │
  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
  │  │  GitHub   │  │  GitLab  │  │  Gitee   │  │ Bitbucket│  │
  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
  └───────────────────────────────────────────────────────────┘
                              │ Webhooks / API
  ┌───────────────────────────────────────────────────────────┐
  │                   CI/CD & DevOps                           │
  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
  │  │  Actions  │  │ GitLab CI│  │ Jenkins  │  │ ArgoCD   │  │
  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
  └───────────────────────────────────────────────────────────┘
                              │ Deploy
  ┌───────────────────────────────────────────────────────────┐
  │               Production & Monitoring                     │
  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
  │  │ Kubernetes│ │   APM    │  │  Docker   │  │  SonarQube│  │
  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
  └───────────────────────────────────────────────────────────┘
```

### 1.2 生态核心组件

| 层级 | 组件 | 代表产品 | 核心功能 |
|------|------|----------|----------|
| **版本控制** | Git CLI | Git v2.40+ | 本地版本管理、分支、合并、变基 |
| **IDE 集成** | 编辑器插件 | IDEA Git Plugin, VS Code Git | 可视化操作、Diff 对比、冲突解决 |
| **托管平台** | 远程仓库 | GitHub, GitLab, Gitee, Bitbucket | 代码托管、协作、PR/MR |
| **CI/CD** | 持续集成 | GitHub Actions, GitLab CI, Jenkins | 自动构建、测试、部署 |
| **代码质量** | 代码扫描 | SonarQube, CodeQL, Snyk | 静态分析、安全扫描 |
| **项目管理** | 任务跟踪 | GitHub Issues, Jira, Linear | 需求管理、Bug 跟踪 |
| **制品管理** | 包管理 | GitHub Packages, Docker Registry | 构建产物存储、版本管理 |
| **文档** | 文档系统 | GitHub Wiki, GitLab Pages | 项目文档、API 文档 |
| **监控** | 可观测性 | Datadog, Grafana | 部署监控、性能追踪 |

---

## 2. GitHub Deep Dive

GitHub 是全球最大的代码托管平台，拥有超过 1 亿开发者，提供从代码托管到项目管理的完整工具链。

### 2.1 GitHub Features Matrix

| 功能模块 | 具体能力 | 免费版 | Team 版 | Enterprise 版 |
|----------|----------|--------|---------|---------------|
| **Repositories** | 私有仓库 | 无限 | 无限 | 无限 |
| **Issues** | Bug/Feature 模板 + Labels + Milestones | ✅ | ✅ | ✅ |
| **Projects** | Kanban 看板（GitHub Projects Beta） | ✅ | ✅ | ✅ |
| **Actions** | CI/CD 流水线（2000 分钟/月 免费） | ✅ | 3000 分钟 | 50000 分钟 |
| **Packages** | Docker 镜像 + 包管理 | ✅ | ✅ | ✅ |
| **Pages** | 静态网站托管 | ✅ | ✅ | ✅ |
| **Wiki** | 项目 Wiki | ✅ | ✅ | ✅ |
| **Security** | Dependabot + Code Scanning + Secret Scanning | ✅ | ✅ | ✅ |
| **Codespaces** | 云端开发环境 | 60小时/月 | 无限 | 无限 |
| **Copilot** | AI 代码补全 | ✅ (免费) | ✅ | ✅ |

### 2.2 Issues 深度使用

```yaml
# .github/ISSUE_TEMPLATE/bug_report.md
---
name: Bug Report
about: 报告 Bug 帮助改进项目
title: "[BUG] "
labels: bug
assignees: ''
---

## Bug 描述
清晰简洁地描述 Bug

## 复现步骤
1. 进入 '...'
2. 点击 '....'
3. 看到错误

## 期望行为
期望应该发生什么

## 截图（可选）

## 环境
- OS: [e.g. Windows 11]
- JDK: [e.g. 17]
- 浏览器: [e.g. Chrome 120]

## 附加信息
```

**Labels 管理策略**:

| Label 类别 | 示例 | 用途 |
|------------|------|------|
| 类型 | `bug`, `feature`, `enhancement`, `docs` | 区分 Issue 类型 |
| 优先级 | `priority: critical`, `priority: high`, `priority: low` | 排期优先级 |
| 状态 | `needs triage`, `in progress`, `blocked`, `stale` | 工作流状态 |
| 模块 | `module: auth`, `module: payment` | 所属模块 |
| 难度 | `good first issue`, `help wanted`, `difficult` | 适合的贡献者 |

**Milestones 版本管理**:

```text
Milestone: v2.0.0
Due: 2024-06-30
Description: 订单系统重构 + 支付通道升级

Issues:
  - #123 订单导出功能    [feature]
  - #124 微信支付集成    [feature]
  - #125 订单列表性能优化  [enhancement]
  - #126 导出中文乱码    [bug]

Progress: 2/4 open (50%)
```

### 2.3 GitHub Actions 实战

```yaml
# .github/workflows/java-ci.yml
name: Java CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest

    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test
        ports:
          - 3306:3306

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}

      - name: Build & Test
        run: mvn verify -B
        env:
          SPRING_PROFILES_ACTIVE: ci

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: target/surefire-reports/
```

### 2.4 GitHub Security 套件

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
      time: "09:00"
      timezone: "Asia/Shanghai"
    open-pull-requests-limit: 10
    labels:
      - "dependencies"
      - "security"

  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
```

| 安全功能 | 作用 | 配置方式 |
|----------|------|----------|
| **Dependabot alerts** | 依赖漏洞自动告警 | 自动（GitHub 检测） |
| **Dependabot updates** | 自动创建依赖更新 PR | `dependabot.yml` |
| **Code scanning** | CodeQL 静态分析 | `.github/workflows/codeql.yml` |
| **Secret scanning** | 检测代码中的令牌/密钥 | 自动（GitHub 检测） |
| **Security advisories** | 发布安全公告 | 仓库 Security 标签页 |

---

## 3. GitLab Deep Dive

GitLab 定位为"单一 DevOps 平台"，内置从代码托管到监控的完整工具链。

### 3.1 GitLab CI/CD 核心概念

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - scan
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2"

cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .m2/
    - target/

build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn compile
  artifacts:
    paths:
      - target/*.jar

test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn test
  coverage: '/Coverage: (\d+\.\d+)%/'

code_scan:
  stage: scan
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn sonar:sonar -Dsonar.projectKey=$CI_PROJECT_ID
  only:
    - main

deploy_prod:
  stage: deploy
  image: alpine:latest
  script:
    - apk add --no-cache openssh-client
    - scp target/*.jar deploy@prod-server:/app/
    - ssh deploy@prod-server "systemctl restart myapp"
  only:
    - tags
  when: manual
```

### 3.2 GitLab 权限粒度

GitLab 的权限管理比 GitHub 更细致，适合企业级管控。

| 级别 | GitHub | GitLab |
|------|--------|--------|
| 角色粒度 | 5 级 | 8 级 |
| 可见性控制 | 公开/私有两种 | 公开/内部/私有三种 |
| 分支权限 | Branch protection rules | Protected branches + Code owners |
| Group 嵌套 | ❌ | ✅（支持多层 Group） |
| 审批规则 | PR Approval | Merge Request Approval Rules（按文件/按人数） |
| Compliance | Enterprise only | CE 版本也支持 |

**GitLab Group 层级示例**:

```text
Company Group (gitlab.company.com)
  ├── DevOps Team (Subgroup)
  │   ├── CI/CD Configs (Project)
  │   └── Infrastructure (Project)
  ├── Backend Team (Subgroup)
  │   ├── Order Service (Project)
  │   ├── Payment Service (Project)
  │   └── User Service (Project)
  └── Frontend Team (Subgroup)
      ├── Web App (Project)
      └── Mobile App (Project)
```

### 3.3 GitLab vs GitHub 核心差异

| 维度 | GitHub | GitLab |
|------|--------|--------|
| 开源理念 | GitHub 本身闭源 | GitLab CE 开源，EE 闭源 |
| CI/CD | Actions（外部工具集成） | 内置 CI/CD（无需额外配置） |
| 自托管 | GitHub Enterprise（昂贵） | GitLab CE（免费自托管） |
| 部署方式 | SaaS only（除非 Enterprise） | SaaS + 自托管 |
| Container Registry | GitHub Packages | 内置 Container Registry |
| Terraform 集成 | GitHub + Terraform Cloud | 内置 Terraform State |
| 合规审计 | Enterprise only | CE 有基础审计，EE 完整审计 |
| Kubernetes | Actions + k8s deploy | 内置 K8s 集成面板 |

> 💡 **选择 GitLab 的关键考量**: 如果你需要完全自托管、细粒度权限控制、以及一体化 DevOps（CI + Registry + K8s 集成），GitLab 是最佳选择。GitLab CE 免费版的功能已经覆盖了大多数企业需求。

---

## 4. Platform Comprehensive Comparison

### 4.1 全维度对比表

| 维度 | GitHub | GitLab | Gitee | Bitbucket |
|------|--------|--------|-------|-----------|
| **免费私有仓库** | ✅ 无限 | ✅ 无限 | ✅ 无限（5人限制） | ✅ 无限（5人限制） |
| **内置 CI/CD** | ✅ Actions | ✅ 最强（原生内置） | ⚠️ Gitee Go（有限） | ✅ Pipelines（500分钟/月） |
| **自托管** | ❌（仅 Enterprise） | ✅ CE/EE 均可 | ✅ 企业版 | ❌（Data Center 版已停售） |
| **代码审查** | ✅ PR | ✅ MR（更细粒度） | ✅ PR | ✅ PR |
| **中国访问速度** | ⚠️ 慢（需代理） | ⚠️ 自托管可加速 | ✅ 极快 | ⚠️ 不稳定 |
| **开源生态** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **中国市场** | ⚠️ 受限 | ⚠️ 受限 | ✅ 合规 | ⚠️ 无优势 |
| **免费 CI 额度** | 2000 分钟/月 | 400 分钟/月 | 有限 | 500 分钟/月 |
| **项目看板** | ✅ GitHub Projects | ✅ Issue Board | ⚠️ 基础 | ✅ + Jira 集成 |
| **Wiki** | ✅ | ✅ | ✅ | ✅ |
| **Pages 站点** | ✅ GitHub Pages | ✅ GitLab Pages | ✅ Gitee Pages | ❌ |
| **包管理** | ✅ Packages | ✅ Registry | ❌ | ❌ |
| **最大文件限制** | 100 MB | 100 MB | 50 MB | 1 GB（但有限制） |
| **LFS 免费额度** | 1 GB | 5 GB | 500 MB | 1 GB |
| **API 完备性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **社区活跃度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **安全性** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |

### 4.2 功能丰富度雷达图维度

```text
             GitHub          GitLab           Gitee         Bitbucket
              ┌─┐              ┌─┐              ┌─┐              ┌─┐
   CI/CD      │5│              │5│              │2│              │3│
              └─┘              └─┘              └─┘              └─┘
              ┌─┐              ┌─┐              ┌─┐              ┌─┐
    Code      │5│              │5│              │3│              │4│
    Review    └─┘              └─┘              └─┘              └─┘
              ┌─┐              ┌─┐              ┌─┐              ┌─┐
  Performance │5│              │4│              │3│              │3│
    China     └─┘              └─┘              └─┘              └─┘
              ┌─┐              ┌─┐              ┌─┐              ┌─┐
    Pricing   │4│              │5│              │5│              │3│
              └─┘              └─┘              └─┘              └─┘
              ┌─┐              ┌─┐              ┌─┐              ┌─┐
  Integration │5│              │5│              │3│              │4│
    (Jira)    └─┘              └─┘              └─┘              └─┘
```

---

## 5. Platform Selection Decision Tree

### 5.1 决策流程

```text
              你需要代码托管平台
                     │
                     ▼
          ┌────────────────────┐
          │  项目性质是什么？    │
          └────────┬───────────┘
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
   个人开源     企业/团队   中国团队
       │           │          │
       ▼           ▼          ▼
  ┌────────┐  ┌────────┐  ┌────────┐
  │ GitHub │  │是否需要  │  │ Gitee  │
  │ 首选   │  │自托管？  │  │ 首选   │
  └────────┘  └───┬────┘  └────────┘
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
     需要完整   需要自助   使用 Atlassian
     DevOps     低成本    全家桶(Jira)
        │          │          │
        ▼          ▼          ▼
   ┌────────┐  ┌────────┐  ┌─────────┐
   │GitLab  │  │ GitLab │  │Bitbucket│
   │EE/自托 │  │ CE     │  │ + Jira  │
   └────────┘  └────────┘  └─────────┘
```

### 5.2 平台选型速查表

| 场景 | 推荐平台 | 备选方案 | 理由 |
|------|----------|----------|------|
| 🌍 开源项目 | GitHub | GitLab.com | 最大开源社区，最高可见度 |
| 🇨🇳 中国团队内部项目 | Gitee | GitLab CE 自托管 | 国内访问快，合规数据本地化 |
| 🏢 企业私有部署 | GitLab CE | GitLab EE | 免费自托管，权限粒度细 |
| 🔄 Atlassian 生态 | Bitbucket | GitHub | 与 Jira 原生集成 |
| 🚀 需要最强 CI/CD | GitLab | GitHub + Actions | GitLab CI 原生内置无需额外工具 |
| 💰 预算有限的企业 | GitLab CE | Gitee 企业版 | 免费 + 自托管 + 全功能 |
| 🔐 高安全合规要求 | GitLab EE | GitHub Enterprise | 审计日志、合规报告、SAML SSO |
| 👤 个人学习/作品集 | GitHub | Gitee | 简历加分、技术社交 |

---

## 6. Multi-Platform Sync

### 6.1 Git Remote 多平台同步

```bash
# 场景：项目需要同时托管在 GitHub 和 Gitee

# 配置多个 remote
git remote add github https://github.com/user/project.git
git remote add gitee https://gitee.com/user/project.git

# 推送到两个平台
git push github main
git push gitee main

# 或使用 pushurl 实现一键双推
git remote set-url --add --push origin https://github.com/user/project.git
git remote set-url --add --push origin https://gitee.com/user/project.git

# 现在 git push origin main 会同时推送到 GitHub 和 Gitee

# 验证配置
git remote -v
# origin  https://github.com/user/project.git (fetch)
# origin  https://gitee.com/user/project.git (push)
```

### 6.2 Mirror Push 保持同步

```bash
# 创建镜像仓库（完整克隆所有分支和标签）
git clone --mirror https://github.com/user/project.git
cd project.git
git remote set-url --push origin https://gitee.com/user/project.git
git push --mirror

# 定期同步脚本（cron 或 GitHub Actions）
# sync-to-gitee.yml
name: Sync to Gitee
on:
  push:
    branches: [main, develop]
  schedule:
    - cron: '0 */6 * * *'

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Sync to Gitee
        run: |
          git remote add gitee https://${{ secrets.GITEE_TOKEN }}@gitee.com/user/project.git
          git push --mirror gitee
```

---

## 7. GitHub CLI (gh) in Action

`gh` 是 GitHub 官方命令行工具，让开发者无需离开终端即可完成所有 GitHub 操作。

### 7.1 安装与配置

```bash
# 安装（Windows）
winget install --id GitHub.cli

# 安装（macOS）
brew install gh

# 安装（Linux）
sudo apt install gh

# 认证
gh auth login
# 选择 GitHub.com，选择 SSH/HTTPS，完成浏览器认证

# 验证
gh auth status
```

### 7.2 PR 全流程操作

```bash
# 创建 PR
gh pr create \
  --title "feat: 新增订单导出功能 #PROJ-456" \
  --body "### 变更内容\n- 新增 CSV 导出接口\n- 支持时间范围过滤" \
  --base develop \
  --assignee @me \
  --label feature,enhancement

# 创建 Draft PR
gh pr create --draft --title "WIP: 开发中" --body "单元测试待补充"

# 查看 PR 列表
gh pr list                    # 当前仓库所有 PR
gh pr list --state merged     # 已合并的 PR
gh pr list --author @me       # 自己创建的 PR
gh pr list --review-requested @me  # 待审查的 PR
gh pr list --label bug        # 按 label 过滤

# 查看 PR 详情
gh pr view 123                # PR #123 详情
gh pr view 123 --comments     # 含评论

# 切换分支到 PR
gh pr checkout 123

# 在 PR 上添加评论
gh pr comment 123 --body "已修复，请重新审查"

# Review PR
gh pr review 123 --approve                     # 批准
gh pr review 123 --request-changes --body "需要修改"  # 请求修改
gh pr review 123 --comment --body "建议优化"         # 评论

# 合并 PR
gh pr merge 123                                # 默认 merge commit
gh pr merge 123 --squash                       # Squash
gh pr merge 123 --rebase                       # Rebase
gh pr merge 123 --auto                         # 条件满足时自动合并

# 关闭 PR
gh pr close 123
gh pr reopen 123

# 查看 PR diff
gh pr diff 123
```

### 7.3 Issue 操作

```bash
# 创建 Issue
gh issue create \
  --title "Bug: 导出 CSV 中文乱码" \
  --label bug \
  --assignee @me

# 查看 Issue
gh issue list
gh issue view 456

# 关闭 Issue
gh issue close 456

# 标记 Issue
gh issue develop 456          # 创建分支来修复 Issue
gh issue label 456 bug,urgent
```

### 7.4 Release 管理

```bash
# 创建 Release
gh release create v1.2.0 \
  --title "v1.2.0 - 订单导出功能" \
  --notes "### 新增\n- 订单 CSV 导出\n- 导出限流" \
  --target main \
  target/app.jar

# 查看 Release
gh release list
gh release view v1.2.0

# 下载 Release 资源
gh release download v1.2.0 --pattern "*.jar"

# 删除 Release
gh release delete v1.2.0
```

### 7.5 Repo / 其他操作

```bash
# 仓库操作
gh repo view                        # 查看当前仓库
gh repo create my-new-project       # 创建新仓库
gh repo fork                        # Fork 当前仓库
gh repo sync                        # 同步 Fork（GitHub 新功能）
gh repo clone org/project           # 克隆仓库

# Gist 操作
gh gist create app.log
gh gist list
gh gist view 123abc

# Action 操作
gh run list
gh run view 123
gh run watch 123                    # 实时观察 Action 运行
gh run rerun 123

# 配置
gh config set editor vim
gh config set git_protocol ssh
```

> 💡 **gh 效率技巧**: 将高频命令设为 shell alias。例如 `alias gprc='gh pr create'`、`alias gprv='gh pr view --web'`、`alias gprm='gh pr merge --squash'`。

---

## 8. Git LFS Cross-Platform

Git LFS (Large File Storage) 用指针文件替换大文件，将实际文件存储到远程服务器，避免 Git 仓库膨胀。

### 8.1 LFS 工作原理

```bash
# 跟踪大文件类型
git lfs track "*.jar"
git lfs track "*.war"
git lfs track "*.zip"
git lfs track "*.dll"
git lfs track "*.so"
git lfs track "assets/**"           # 整个目录

# 查看跟踪规则
git lfs track
# Listing tracked patterns
#     *.jar (.gitattributes)
#     *.war (.gitattributes)

# .gitattributes 文件内容（提交到仓库共享）
*.jar filter=lfs diff=lfs merge=lfs -text
*.war filter=lfs diff=lfs merge=lfs -text

# 将已跟踪的文件添加到 LFS
git lfs migrate import --include="*.jar" --everything
```

### 8.2 各平台 LFS 支持对比

| 平台 | 免费额度 | 额外收费 | LFS 带宽限制 | 备注 |
|------|----------|----------|-------------|------|
| **GitHub** | 1 GB 存储 / 1 GB 带宽每月 | $5/月 50GB | 按带宽计费 | LFS 数据在 GitHub 存储 |
| **GitLab** | 5 GB 存储（SaaS 版） | $19/月起 10GB | ✓ 无带宽限制 | 自托管 CE 无限制 |
| **Gitee** | 500 MB | 企业版付费 | 有带宽限制 | 不支持 1GB 以上单文件 |
| **Bitbucket** | 1 GB | $10/月 10GB | 按套餐 | 与 Git 仓库存储共享配额 |

### 8.3 LFS 日常管理

```bash
# 查看 LFS 文件状态
git lfs ls-files

# 查看 LFS 存储使用量
git lfs env

# 拉取 LFS 文件
git lfs pull                    # 拉取所有 LFS 文件
git pull                        # 自动触发 lfs pull（如果配置了 smudge filter）

# 只拉取当前检出版本需要的 LFS 文件（加速克隆）
GIT_LFS_SKIP_SMUDGE=1 git clone https://github.com/user/project.git
cd project
git lfs pull                    # 按需拉取

# 删除本地 LFS 缓存（释放空间）
git lfs prune

# 迁移已存在的 Git 历史到 LFS
git lfs migrate import --include="*.jar" --everything
git push --force origin main
```

---

## 9. Security & Compliance

### 9.2 SSH Key 管理

```bash
# 生成 SSH Key
ssh-keygen -t ed25519 -C "zhangsan@company.com"
# 或兼容旧系统的 RSA 4096
ssh-keygen -t rsa -b 4096 -C "zhangsan@company.com"

# 添加 SSH Key 到 ssh-agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# 配置 SSH config（多账号场景）
# ~/.ssh/config
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_work

Host github-personal
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_personal

# 多账号 clone 示例
git clone git@github-personal:username/private-project.git
```

### 9.3 安全合规最佳实践

| 实践 | 说明 | 推荐平台 |
|------|------|----------|
| **2FA 强制** | 所有成员启用双因素认证 | GitHub / GitLab 均支持 |
| **SSH Key 轮换** | 每 6 个月轮换一次 | 手动或自动化 |
| **SSH Key 审查** | 定期审计仓库内所有 SSH Key | GitLab 审计日志 |
| **Commit 签名** | GPG 签名验证提交者身份 | GitHub 显示 "Verified" |
| **IP 白名单** | 限制仅公司 IP 可访问 | GitHub Enterprise / GitLab EE |
| **审计日志** | 记录所有 Git 操作 | GitLab EE 更全面 |
| **SOC2 合规** | 安全审计认证 | GitHub / GitLab Enterprise |
| **ISO 27001** | 信息安全标准 | GitHub / GitLab Enterprise |
| **SAML SSO** | 企业单点登录 | GitHub Enterprise / GitLab EE |
| **SCIM 同步** | 自动用户管理 | GitHub Enterprise / GitLab EE |
| **分支保护** | 强制 PR + Approvals | GitHub / GitLab |
| **提交签名强制** | GPG 签名强制验证 | GitLab 支持全局策略 |

### 9.4 Git Commit Signing（GPG 签名）

```bash
# 生成 GPG Key
gpg --full-generate-key
# RSA 4096, 有效期为 2 年

# 列出 GPG Key
gpg --list-secret-keys --keyid-format=long
# /home/user/.gnupg/secring.gpg
# -----------------------------------
# sec   4096R/ABCDEF1234567890 2024-01-01 [expires: 2026-01-01]
# uid                          Zhang San <zhangsan@example.com>

# 配置 Git 使用 GPG 签名
git config --global user.signingkey ABCDEF1234567890
git config --global commit.gpgsign true

# 签署提交
git commit -S -m "feat: 带签名的提交"

# 查看签名
git log --show-signature -1
```

---

## 10. Open Source Project Operations

一个成功的开源项目不仅需要好的代码，还需要完整的项目治理结构。

### 10.1 开源项目必备文件

```text
project-root/
├── README.md                 # 项目入口文档（最重要）
├── CONTRIBUTING.md           # 贡献指南
├── CODE_OF_CONDUCT.md        # 行为准则
├── LICENSE                   # 开源许可证
├── SECURITY.md               # 安全报告流程
├── CHANGELOG.md              # 版本变更日志
└── .github/
    ├── ISSUE_TEMPLATE/        # Issue 模板
    │   ├── bug_report.md
    │   └── feature_request.md
    ├── PULL_REQUEST_TEMPLATE/ # PR 模板
    │   └── pull_request_template.md
    └── CODEOWNERS            # 代码所有者
```

### 10.2 README 黄金结构

```markdown
# Project Name
> 一句话描述项目定位

## Badges（CI 状态、覆盖率、许可证、下载量）

[![CI](https://img.shields.io/github/actions/workflow/status/user/repo/ci.yml)](...)
[![Coverage](https://img.shields.io/codecov/c/github/user/repo)](...)
[![License](https://img.shields.io/github/license/user/repo)](...)
[![JDK](https://img.shields.io/badge/JDK-17+-blue)](...)

## 简介
2-3 段介绍项目解决了什么问题、核心特性是什么。

## 快速开始
```bash
# 安装
git clone https://github.com/user/repo.git
# 构建
mvn clean install
# 运行
java -jar target/app.jar
```

## 文档
- [完整文档](https://project-docs.dev)
- [API 参考](https://project-docs.dev/api)
- [常见问题](FAQ.md)

## 贡献
请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解如何贡献代码。

## 许可证
本项目采用 [MIT 许可证](LICENSE)。
```

### 10.3 CONTRIBUTING.md 核心内容

```markdown
# Contributing to Project

## 开发流程
1. Fork 项目到自己的账号
2. 基于 main 创建分支：`git checkout -b feature/xxx`
3. 提交代码：`git commit -m "feat: ..."`
4. 推送：`git push origin feature/xxx`
5. 创建 Pull Request

## 提交规范
遵循 Conventional Commits 规范：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档变化
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/依赖

示例：`feat(order): 新增 CSV 导出功能`

## 代码规范
- Java 17+，使用 Records、Pattern Matching 等新特性
- 遵循阿里巴巴 Java 开发手册
- 代码通过 Checkstyle 检查
- 单元测试覆盖率 ≥ 80%

## PR 要求
- 更新相关文档
- 添加单元测试
- 通过 CI 检查
- 描述变更内容和动机
```

### 10.4 开源许可证速查表

| 许可证 | 性质 | 使用方义务 | 适合场景 |
|--------|------|-----------|----------|
| **MIT** | 宽松 | 保留版权声明 | 大多数项目首选，商业友好 |
| **Apache 2.0** | 宽松 | 保留声明 + 注明修改 | 企业级项目 |
| **GPL 3.0** | 强 Copyleft | 衍生项目也必须开源 | 希望保护开源生态的项目 |
| **LGPL 3.0** | 弱 Copyleft | 修改库需开源，调用不受限 | 库/框架项目 |
| **AGPL 3.0** | 网络 Copyleft | 通过 Web 提供服务也需开源 | 网络服务项目 |
| **BSL 1.1** | 源码可用 | 生产环境有限制 | 商业开源项目 |

> 🎯 **Git 生态的层次化理解**: 从最底层的 `git init` 到最高层的开源社区运营，Git 生态形成了"工具 → 平台 → 流程 → 文化"的四层架构。初学阶段关注工具操作，进阶阶段选择平台和建立流程，而高级阶段则是塑造团队协作文化和开源治理模式。选择一个技术栈容易，但建立一个健康的协作生态需要持续的投入和迭代。
